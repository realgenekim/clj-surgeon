(ns verify-clj-surgeon-skill
  (:require
   [clojure.string :as str]))

(def required-fragments
  ["inspect_clojure"
   "edit_clojure"
   "workspace_root"
   "`edits`"
   "`programs`"
   "within"
   "from"
   "to"
   "matches"
   "frozen snapshot"
   "native patching"
   "heavyweight `apply_clojure_changes`"
   "Verification policy must not depend on editor choice"
   "references/mcp-advanced.md"
   "references/cli-fallback.md"
   "references/advanced-operations.md"])

(def max-entrypoint-lines 90)

(defn fail! [message data]
  (binding [*out* *err*]
    (prn (merge {:ok false :error message} data)))
  (System/exit 1))

(defn assert-contract! [skill-text]
  (doseq [fragment required-fragments]
    (when-not (str/includes? skill-text fragment)
      (fail! "Skill contract fragment is missing" {:fragment fragment})))
  (let [line-count (count (str/split-lines skill-text))]
    (when (> line-count max-entrypoint-lines)
      (fail! "Skill entrypoint exceeds its line budget"
             {:actual line-count :maximum max-entrypoint-lines}))))

(defn normalized-root-mirror [text]
  (str/replace text "skills/clj-surgeon/references/" "references/"))

(defn -main [& _]
  (let [canonical-path "skills/clj-surgeon/SKILL.md"
        root-path "skill.md"
        canonical (slurp canonical-path)
        root-mirror (slurp root-path)]
    (assert-contract! canonical)
    (when-not (= canonical (normalized-root-mirror root-mirror))
      (fail! "Root skill mirror drifted from the canonical skill"
             {:canonical canonical-path :mirror root-path}))
    (doseq [reference ["skills/clj-surgeon/references/mcp-advanced.md"
                       "skills/clj-surgeon/references/cli-fallback.md"
                       "skills/clj-surgeon/references/advanced-operations.md"]]
      (when-not (.isFile (java.io.File. reference))
        (fail! "Skill reference is missing" {:reference reference})))
    (prn {:ok true
          :operation :verify-clj-surgeon-skill
          :entrypoint-lines (count (str/split-lines canonical))
          :max-entrypoint-lines max-entrypoint-lines})))

(apply -main *command-line-args*)
