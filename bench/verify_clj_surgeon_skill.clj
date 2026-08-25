(ns verify-clj-surgeon-skill
  (:require
   [clojure.string :as str]))

(def common-route-fragments
  ["inspect_clojure"
   "edit_clojure"
   "workspace_root"
   "`edits`"
   "`programs`"
   "frozen snapshot"
   "`apply_patch`"
   "heavyweight `apply_clojure_changes`"
   "never add a tool call merely because Surgeon performed"])

(def skill-route-fragments
  ["Optimize complete verified task time"
   "references/mcp-advanced.md"
   "references/cli-fallback.md"
   "references/advanced-operations.md"])

(def max-entrypoint-lines 45)

(defn fail! [message data]
  (binding [*out* *err*]
    (prn (merge {:ok false :error message} data)))
  (System/exit 1))

(defn assert-fragments! [label text fragments]
  (doseq [fragment fragments]
    (when-not (str/includes? text fragment)
      (fail! "Routing contract fragment is missing"
             {:surface label :fragment fragment}))))

(defn assert-contract! [skill-text]
  (assert-fragments! :skill skill-text skill-route-fragments)
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
        root-mirror (slurp root-path)
        always-loaded (slurp "CLAUDE.md")]
    (assert-contract! canonical)
    (assert-fragments! :always-loaded-instructions
                       always-loaded
                       common-route-fragments)
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
