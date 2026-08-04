(ns clj-surgeon.structural-lens
  "Exact, fail-closed structural search and replacement below a named form."
  (:require
   [clj-surgeon.file-ops :as file-ops]
   [clj-surgeon.forms :as forms]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.pprint :as pprint]
   [clojure.string :as str]
   [rewrite-clj.zip :as z])
  (:import
   (java.security MessageDigest)))

(def plan-version 1)
(def tool-version "0.1.0")

(defn source-hash [source]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256")
                        (.getBytes source "UTF-8"))]
    (apply str (map #(format "%02x" (bit-and 0xff %)) digest))))

(defn- one-complete-form [value error-type label]
  (when (nil? value)
    (throw (ex-info (str label " is required") {:error-type error-type})))
  (let [source (if (string? value) value (pr-str value))]
    (try
      (let [root (z/of-string source {:track-position? true})
            forms (->> (iterate z/right root) (take-while some?) vec)]
        (when-not (= 1 (count forms))
          (throw (ex-info (str label " must contain exactly one complete form")
                          {:error-type error-type})))
        {:sexpr (z/sexpr (first forms))
         :source (z/string (first forms))})
      (catch clojure.lang.ExceptionInfo e
        (if (= error-type (:error-type (ex-data e)))
          (throw e)
          (throw (ex-info (str "Invalid " (str/lower-case label) ": " (.getMessage e))
                          {:error-type error-type}))))
      (catch Exception e
        (throw (ex-info (str "Invalid " (str/lower-case label) ": " (.getMessage e))
                        {:error-type error-type}))))))

(defn- wildcard-match? [pattern candidate]
  (cond
    (= '_ pattern) true
    (and (map? pattern) (map? candidate))
    (and (= (set (keys pattern)) (set (keys candidate)))
         (every? (fn [[k v]] (wildcard-match? v (get candidate k))) pattern))
    (and (sequential? pattern) (sequential? candidate))
    (and (= (count pattern) (count candidate))
         (every? true? (map wildcard-match? pattern candidate)))
    (and (set? pattern) (set? candidate)) (= pattern candidate)
    :else (= pattern candidate)))

(defn- zipper-locations [zloc]
  (->> (iterate z/next zloc) (take-while #(and % (not (z/end? %))))))

(defn- top-level-locations [zloc]
  (->> (iterate z/right zloc) (take-while some?)))

(defn- defining-form-name [zloc]
  (when (z/list? zloc)
    (let [head (some-> zloc z/down z/sexpr str)]
      (when (forms/defining-form? head)
        (some-> zloc z/down z/right z/sexpr str)))))

(defn- inside-range [zloc inside]
  (some (fn [candidate]
          (when (= (str inside) (defining-form-name candidate))
            (let [{:keys [row end-row]} (meta (z/node candidate))]
              {:row row :end-row end-row})))
        (top-level-locations zloc)))

(defn- within-range? [{:keys [row end-row]} zloc]
  (let [{node-row :row node-end-row :end-row} (meta (z/node zloc))]
    (and node-row node-end-row (<= row node-row) (<= node-end-row end-row))))

(defn- enclosing-form-name [top-levels candidate]
  (some (fn [top-level]
          (let [{:keys [row end-row]} (meta (z/node top-level))]
            (when (within-range? {:row row :end-row end-row} candidate)
              (defining-form-name top-level))))
        top-levels))

(defn- node-head [zloc]
  (when-let [child (z/down zloc)]
    (try (z/sexpr child) (catch Exception _ nil))))

(defn- semantic-path [zloc inside]
  (loop [current zloc child nil path '()]
    (if-not current
      (vec path)
      (let [head (node-head current)
            descriptor
            (cond
              (z/list? current)
              (if (and inside (= (str inside) (defining-form-name current)))
                {:form (symbol (str inside))}
                (when head {:call head}))
              (z/vector? current)
              (if (keyword? head)
                {:vector-tag head}
                (let [left (some-> child z/left)
                      binding (when left (try (z/sexpr left) (catch Exception _ nil)))]
                  (if (symbol? binding) {:binding binding} {:vector true})))
              (z/map? current)
              (let [left (some-> child z/left)
                    key (when left (try (z/sexpr left) (catch Exception _ nil)))]
                (if (keyword? key) {:attr key} {:map true}))
              :else nil)]
        (recur (z/up current) current (cond-> path descriptor (conj descriptor)))))))

(defn find-subforms [source {:keys [inside match]}]
  (try
    (let [{pattern :sexpr match-source :source}
          (one-complete-form match :invalid-match "Match")
          root (z/of-string source {:track-position? true})
          top-levels (vec (top-level-locations root))
          range (when inside (inside-range root inside))]
      (if (and inside (nil? range))
        {:error (str "Enclosing form not found: " inside)
         :error-type :inside-not-found
         :inside (str inside) :match match-source :match-count 0 :matches []
         :source-hash (source-hash source)}
        (let [matches (->> (zipper-locations root)
                           (map-indexed vector)
                           (keep (fn [[index candidate]]
                                   (when (and (or (nil? range) (within-range? range candidate))
                                              (try (wildcard-match? pattern (z/sexpr candidate))
                                                   (catch Exception _ false)))
                                     (let [{:keys [row end-row]} (meta (z/node candidate))
                                           owner (enclosing-form-name top-levels candidate)]
                                       (cond-> {:path (semantic-path candidate inside)
                                                :address {:preorder index}
                                                :line row :end-line end-row
                                                :source (z/string candidate)}
                                         owner (assoc :inside owner))))))
                           vec)]
          {:inside (when inside (str inside)) :match match-source
           :match-count (count matches) :matches matches
           :source-hash (source-hash source)})))
    (catch Exception e
      {:error (.getMessage e)
       :error-type (or (:error-type (ex-data e)) :invalid-source)
       :match-count 0 :matches [] :source-hash (source-hash source)})))

(defn find-file [{:keys [file] :as opts}]
  (if-not file
    {:error ":file is required" :error-type :missing-arguments}
    (assoc (find-subforms (slurp file) opts) :file file)))

(defn- prefixed-lines [prefix source]
  (str/join "\n" (map #(str prefix %) (str/split source #"\n" -1))))

(defn- edit-diff [file {:keys [line before after]}]
  (let [before-count (count (str/split before #"\n" -1))
        after-count (count (str/split after #"\n" -1))
        absolute? (.isAbsolute (io/file file))
        before-file (if absolute? file (str "a/" file))
        after-file (if absolute? file (str "b/" file))]
    (str "--- " before-file "\n+++ " after-file "\n"
         "@@ -" line "," before-count " +" line "," after-count " @@\n"
         (prefixed-lines "-" before) "\n" (prefixed-lines "+" after) "\n")))

(defn- replacement-node [source]
  (z/node (z/of-string source)))

(defn apply-plan [source {:keys [source-hash edits result-hash] :as plan}]
  (cond
    (not= plan-version (:plan-version plan))
    {:error (str "Unsupported plan version: " (pr-str (:plan-version plan)))
     :error-type :unsupported-plan-version :supported-plan-version plan-version}
    (not= :replace-subform (:operation plan))
    {:error (str "Unsupported plan operation: " (pr-str (:operation plan)))
     :error-type :unsupported-plan-operation}
    (not= source-hash (clj-surgeon.structural-lens/source-hash source))
    {:error "Source hash does not match plan" :error-type :source-hash-mismatch}
    (not= 1 (count edits))
    {:error (str "Expected exactly one planned edit, found " (count edits))
     :error-type :invalid-plan}
    :else
    (try
      (let [{:keys [path address before after]} (first edits)
            root (z/of-string source {:track-position? true})
            target (nth (zipper-locations root) (:preorder address) nil)]
        (cond
          (nil? target) {:error (str "Planned path no longer exists: " path)
                         :error-type :stale-path}
          (not= before (z/string target))
          {:error "Source at planned path does not match edit" :error-type :stale-subform}
          :else
          (let [{replacement-source :source}
                (one-complete-form after :invalid-result-source "Replacement result")
                updated (-> target (z/replace (replacement-node replacement-source)) z/root-string)
                ;; Validate the whole future file, not only the replacement subtree.
                _ (z/of-string updated {:track-position? true})
                actual-result-hash (clj-surgeon.structural-lens/source-hash updated)]
            (if (and result-hash (not= result-hash actual-result-hash))
              {:error "Result hash does not match plan" :error-type :result-hash-mismatch}
              {:ok true :source updated :result-hash actual-result-hash}))))
      (catch Exception e
        {:error (.getMessage e)
         :error-type (or (:error-type (ex-data e)) :invalid-result-source)}))))

(defn plan-replacement [source {:keys [inside match with file]}]
  (try
    (let [{replacement-source :source}
          (one-complete-form with :invalid-replacement "Replacement")
          found (find-subforms source {:inside inside :match match})
          match-count (:match-count found)]
      (cond
        (:error found) found
        (not= 1 match-count)
        (assoc found :error (str "Expected exactly one match, found " match-count)
                     :error-type (if (zero? match-count) :no-match :ambiguous-match))
        :else
        (let [matched (first (:matches found))
              selector {:inside (:inside found) :match (:match found)
                        :expected-match-count 1}
              edit {:path (:path matched) :address (:address matched)
                    :line (:line matched) :before (:source matched)
                    :after replacement-source}
              provisional {:plan-version plan-version :operation :replace-subform
                           :file file :selector selector
                           :source-hash (:source-hash found) :match-count 1
                           :edits [edit]
                           :diff (edit-diff (or file "source.clj") edit)}
              applied (apply-plan source provisional)]
          (if (:error applied)
            applied
            (let [result-hash (:result-hash applied)]
              (assoc provisional
                     :result-hash result-hash
                     :provenance {:tool "clj-surgeon" :tool-version tool-version
                                  :operation :replace-subform :selector selector
                                  :source-hash (:source-hash found)
                                  :result-hash result-hash}))))))
    (catch Exception e
      {:error (.getMessage e)
       :error-type (or (:error-type (ex-data e)) :invalid-replacement)})))

(defn plan-file-replacement [{:keys [file plan-out] :as opts}]
  (if-not file
    {:error ":file is required" :error-type :missing-arguments}
    (let [plan (plan-replacement (slurp file) opts)]
      (if (or (:error plan) (nil? plan-out))
        plan
        (try
          (with-open [writer (io/writer plan-out)]
            (binding [*out* writer] (pprint/pprint plan)))
          (assoc plan :plan-out plan-out)
          (catch Exception e
            {:error (str "Could not write plan: " (.getMessage e))
             :error-type :plan-write-failed :plan-out plan-out}))))))

(defn- read-plan [plan]
  (cond (map? plan) plan
        (string? plan) (edn/read-string (slurp plan))
        :else nil))

(defn verified-apply-receipt
  "Build a machine-readable receipt from a plan and the exact bytes read back
   after its atomic write. Pure: plan and source string in; data out."
  [plan read-back-source]
  (let [read-back-hash (source-hash read-back-source)
        expected-result-hash (:result-hash plan)]
    (if (not= expected-result-hash read-back-hash)
      {:error "Read-back hash does not match the planned result"
       :error-type :read-back-hash-mismatch
       :file (:file plan)
       :expected-result-hash expected-result-hash
       :read-back-hash read-back-hash}
      (try
        (z/of-string read-back-source {:track-position? true})
        {:ok true
         :operation :replace-subform!
         :file (:file plan)
         :source-hash (:source-hash plan)
         :result-hash expected-result-hash
         :applied-edit (first (:edits plan))
         :verified {:whole-file-parsed true
                    :atomic-write true
                    :read-back-hash read-back-hash}}
        (catch Exception e
          {:error (str "Written result could not be reparsed: " (.getMessage e))
           :error-type :read-back-invalid-source
           :file (:file plan)
           :read-back-hash read-back-hash})))))

(defn execute-plan! [{:keys [plan]}]
  (try
    (if-let [plan (read-plan plan)]
      (if-let [file (:file plan)]
        (let [source (slurp file)
              result (apply-plan source plan)]
          (if (:error result)
            result
            (if-let [write-error
                     (try
                       (file-ops/atomic-write! file (:source result))
                       nil
                       (catch Exception e
                         {:error (str "Atomic replacement failed; target was not replaced: "
                                      (.getMessage e))
                          :error-type :atomic-write-failed :file file}))]
              write-error
              (try
                (verified-apply-receipt plan (slurp file))
                (catch Exception e
                  {:error (str "Could not read back the replaced target: "
                               (.getMessage e))
                   :error-type :read-back-failed :file file})))))
        {:error "Plan does not contain :file" :error-type :invalid-plan})
      {:error ":plan must be an EDN plan map or file path" :error-type :invalid-plan})
    (catch Exception e
      {:error (.getMessage e) :error-type :apply-failed})))
