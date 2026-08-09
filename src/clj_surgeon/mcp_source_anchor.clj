(ns clj-surgeon.mcp-source-anchor
  (:require
   [clj-surgeon.outline :as outline]
   [clj-surgeon.structural-lens :as structural-lens]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [rewrite-clj.node :as n]
   [rewrite-clj.zip :as z]))

(defn- owner-name-node
  [form-node owner]
  (when-let [header-node (->> (n/children form-node)
                              (remove n/whitespace-or-comment?)
                              second)]
    (->> (tree-seq n/inner? n/children header-node)
         (filter n/symbol-node?)
         (filter #(= (name owner) (n/string %)))
         first)))

(defn- owner-selection-range
  [source owner form-line form-end-line]
  (let [root-node (z/node (z/of-string* source {:track-position? true}))
        form-node (->> (tree-seq n/inner? n/children root-node)
                       (filter n/inner?)
                       (filter #(let [{:keys [row end-row]} (meta %)]
                                  (and (= form-line row)
                                       (= form-end-line end-row))))
                       (filter #(owner-name-node % owner))
                       first)
        owner-node (some-> form-node (owner-name-node owner))]
    (when owner-node
      (let [{:keys [row col end-row end-col]} (meta owner-node)]
        (when (every? some? [row col end-row end-col])
          {:start {:line (dec row) :character (dec col)}
           :end {:line (dec end-row) :character (dec end-col)}})))))

(defn- refusal
  [error-type message data]
  (merge {:ok false
          :error-type error-type
          :error message
          :source-unchanged true}
         data))

(defn- subject-parts
  [subject]
  (when (and (string? subject)
             (str/includes? subject "/"))
    (let [separator (.lastIndexOf ^String subject "/")
          namespace-name (subs subject 0 separator)
          var-name (subs subject (inc separator))]
      (when (and (seq namespace-name) (seq var-name))
        {:namespace (symbol namespace-name)
         :owner (symbol var-name)}))))

(def ^:private default-source-roots ["" "src" "test" "dev"])

(defn- read-build-config
  [project-root filename]
  (let [file (io/file project-root filename)]
    (when (.isFile file)
      (try
        (edn/read-string (slurp file))
        (catch Exception _ nil)))))

(defn- configured-source-roots
  [config]
  (when (map? config)
    (concat
      (:paths config)
      (mapcat (fn [alias]
                (concat (:extra-paths alias) (:replace-paths alias)))
              (vals (:aliases config))))))

(defn- safe-relative-root
  [root]
  (when (string? root)
    (let [normalized (-> root
                         (str/replace #"\\\\" "/")
                         (str/replace #"^\\./" "")
                         (str/replace #"/+$" ""))]
      (when (and (not (.isAbsolute (io/file normalized)))
                 (not-any? #{".."} (str/split normalized #"/")))
        (if (= normalized ".") "" normalized)))))

(defn workspace-source-roots
  "Return deterministic confined source roots declared by one workspace."
  [project-root]
  (->> [(read-build-config project-root "deps.edn")
        (read-build-config project-root "bb.edn")]
       (mapcat configured-source-roots)
       (concat default-source-roots)
       (keep safe-relative-root)
       distinct
       vec))

(defn candidate-relative-files
  "Return deterministic in-workspace namespace-convention candidates.

  Source roots come from deps.edn and bb.edn, plus conservative defaults.
  Parent traversal and absolute roots are excluded; sibling modules require an
  explicit workspace coordinate rather than weakening path confinement."
  [project-root subject]
  (when-let [{:keys [namespace]} (subject-parts subject)]
    (let [namespace-path (-> (str namespace)
                             (str/replace "-" "_")
                             (str/replace "." "/"))
          roots (workspace-source-roots project-root)]
      (vec
        (distinct
          (for [root roots
                extension [".clj" ".cljc" ".cljs"]]
            (str (when (seq root) (str root "/"))
                 namespace-path extension)))))))

(defn build-form-source-anchor
  "Build one zero-based LSP anchor from an already selected named form.

  This is the single exact-source authority used by ordinary forms reads and
  semantic prepare. The form record must contain name, line, and end-line from
  the same source bytes."
  [relative-file source {:keys [name line end-line]}]
  (let [owner (some-> name str)
        lines (str/split source #"\n" -1)
        start-text (when line (nth lines (dec line) nil))
        end-text (when end-line (nth lines (dec end-line) nil))
        form-character (count (or (some->> start-text (re-find #"^\s*")) ""))
        selection-range (when (and owner start-text end-text)
                          (owner-selection-range source owner line end-line))]
    (cond
      (not (and owner (seq owner) line end-line start-text end-text))
      (refusal
        :semantic-candidate-range-invalid
        "The exact owner range is outside the candidate source"
        {:file relative-file :owner owner :line line :end-line end-line})

      (nil? selection-range)
      (refusal
        :semantic-candidate-selection-missing
        "The exact owner token could not be selected from the candidate source"
        {:file relative-file :owner owner})

      :else
      {:ok true
       :source-anchor
       {:file relative-file
        :source_sha256 (structural-lens/source-hash source)
        :owner owner
        :range
        {:start {:line (dec line)
                 :character form-character}
         :end {:line (dec end-line)
               :character (count end-text)}}
        :selection_range selection-range}})))

(defn build-source-anchor
  "Build one zero-based LSP owner anchor from exact source bytes.

  Pure: the candidate relative path, source, and explicit project aliases are
  inputs. The candidate must declare the subject namespace and contain exactly
  one named top-level owner for the subject Var."
  [subject relative-file source project-aliases]
  (if-let [{:keys [namespace owner]} (subject-parts subject)]
    (try
      (let [outlined (outline/outline-source relative-file source project-aliases)
            owners (filterv #(= owner (:name %)) (:forms outlined))]
        (cond
          (not= namespace (:ns outlined))
          (refusal
            :semantic-candidate-namespace-mismatch
            "The semantic candidate file does not declare the requested namespace"
            {:subject subject
             :file relative-file
             :expected-namespace namespace
             :actual-namespace (:ns outlined)})

          (not= 1 (count owners))
          (refusal
            :semantic-candidate-owner-mismatch
            "The semantic candidate file must contain exactly one requested named owner"
            {:subject subject
             :file relative-file
             :owner owner
             :owner-count (count owners)})

          :else
          (assoc (build-form-source-anchor relative-file source (first owners))
                 :subject subject)))
      (catch Exception error
        (refusal
          :semantic-candidate-parse-failed
          "The semantic candidate file could not be parsed"
          {:subject subject :file relative-file :cause (.getMessage error)})))
    (refusal
      :invalid-change-subject
      "Expected one fully qualified namespace/name Var"
      {:subject subject})))
