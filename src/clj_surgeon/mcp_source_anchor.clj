(ns clj-surgeon.mcp-source-anchor
  (:require
   [clj-surgeon.outline :as outline]
   [clj-surgeon.structural-lens :as structural-lens]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]))

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
          (let [{:keys [line end-line]} (first owners)
                lines (str/split source #"\n" -1)
                start-text (nth lines (dec line) nil)
                end-text (nth lines (dec end-line) nil)]
            (if-not (and start-text end-text)
              (refusal
                :semantic-candidate-range-invalid
                "The exact owner range is outside the candidate source"
                {:subject subject :file relative-file :line line :end-line end-line})
              {:ok true
               :subject subject
               :source-anchor
               {:file relative-file
                :source_sha256 (structural-lens/source-hash source)
                :owner (name owner)
                :range
                {:start {:line (dec line)
                         :character (count (or (re-find #"^\s*" start-text) ""))}
                 :end {:line (dec end-line)
                       :character (count end-text)}}}}))))
      (catch Exception error
        (refusal
          :semantic-candidate-parse-failed
          "The semantic candidate file could not be parsed"
          {:subject subject :file relative-file :cause (.getMessage error)})))
    (refusal
      :invalid-change-subject
      "Expected one fully qualified namespace/name Var"
      {:subject subject})))
