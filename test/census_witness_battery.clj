(ns census-witness-battery
  "THE COMMITTED COMPOSITION of the relation-census review battery.

   Opus's round-seventeen item 8. The builder reported the battery as
   `{:test 13 :pass 540}` and the reviewer, assembling their own thirteen from
   the same prose description, got `{:test 13 :pass 444}` — a 96-assertion gap
   with no defect behind it, because the two sets were not the same thirteen.
   The difference was WHICH VARS ARE IN THE SET, and the set lived in a
   sentence.

   A figure quoted from a set nobody wrote down is not a receipt: it cannot be
   reproduced, so it cannot be checked, so it can be repeated onward as though
   it had been. This file is the set. It is a FIXED, ordered list of vars — no
   discovery over the namespace, no filtering by name pattern, nothing that
   depends on what happens to be loaded — and it prints the per-var
   composition, so two runs that disagree name the var they disagree about
   rather than a total.

   Run (the alias's `:main-opts` name the suite runner, so the battery is
   driven over the same classpath rather than through the alias):

     java -cp \"$(clojure -Spath -M:clj-surgeon/mcp-test)\" \\
          clojure.main -m census-witness-battery

   Exits non-zero on any MISSING var, failure or error."
  (:require
   [clojure.test :as t]
   [clj-surgeon.mcp-relation-census-test]))

(def battery
  "Every named witness this program's review rounds produced, with the round
   that produced it. ORDERED, so the printed composition is stable; a var
   added here is a deliberate edit to the battery, and a var whose name changes
   is reported MISSING rather than silently dropped from the count."
  [;; Round fifteen
   ['clj-surgeon.mcp-relation-census-test/the-cli-entrance-validates-every-field-before-it-loads-any-config :r15]
   ['clj-surgeon.mcp-relation-census-test/the-constructor-refuses-a-files-list-the-published-schema-rejects :r15]
   ;; Round sixteen
   ['clj-surgeon.mcp-relation-census-test/a-read-that-fails-after-the-fence-is-never-an-adapter-crash :r16]
   ['clj-surgeon.mcp-relation-census-test/no-refusal-publishes-the-raw-text-of-the-exception-that-produced-it :r16]
   ['clj-surgeon.mcp-relation-census-test/an-unreadable-directory-refuses-the-census-on-both-entrances :r16]
   ['clj-surgeon.mcp-relation-census-test/a-continuation-over-the-ceiling-names-the-cause-it-measured :r16]
   ['clj-surgeon.mcp-relation-census-test/a-continuation-over-the-ceiling-on-a-long-root-names-the-root :r16]
   ['clj-surgeon.mcp-relation-census-test/every-refusal-field-is-length-bounded-at-both-entrances :r16]
   ['clj-surgeon.mcp-relation-census-test/a-shape-refusal-on-a-long-root-measures-its-continuation :r16]
   ['clj-surgeon.mcp-relation-census-test/every-continuation-either-entrance-emits-fits-the-byte-ceiling :r16]
   ['clj-surgeon.mcp-relation-census-test/the-constructors-are-the-only-continuation-construction-sites :r16]
   ;; Round eighteen — the witnesses that close round seventeen's findings
   ['clj-surgeon.mcp-relation-census-test/every-refusal-shape-either-entrance-emits-is-bounded-at-the-entrance :r18]
   ['clj-surgeon.mcp-relation-census-test/every-declared-refusal-shape-carries-no-field-over-the-ceiling :r18]
   ['clj-surgeon.mcp-relation-census-test/the-overflow-remedy-names-the-heaviest-field-it-measured :r18]
   ['clj-surgeon.mcp-relation-census-test/the-two-entrances-name-the-same-cause-for-the-same-observation :r18]
   ['clj-surgeon.mcp-relation-census-test/a-refusal-whose-subject-is-the-root-names-the-root :r18]
   ;; Round nineteen — the witnesses that close round eighteen's findings.
   ;; `naming-a-source-is-not-walking-a-tree` is GONE from this list, and its
   ;; removal is the deliberate edit this file exists to make visible: round
   ;; eighteen's rule authorised a read outside the censused tree, and round
   ;; nineteen replaced the rule rather than adding a witness beside it.
   ['clj-surgeon.mcp-relation-census-test/no-census-reads-a-source-whose-real-path-leaves-the-workspace :r19]
   ['clj-surgeon.mcp-relation-census-test/every-refusal-the-launcher-itself-prints-is-bounded-at-its-exit :r19]
   ['clj-surgeon.mcp-relation-census-test/no-refusal-names-the-workspace-root-in-its-prose :r19]
   ['clj-surgeon.mcp-relation-census-test/no-refusal-SITE-renders-a-raw-workspace-root-into-prose :r19]])

(defn -main [& _]
  (let [resolved (for [[sym round] battery]
                   {:sym sym :round round :var (resolve sym)})
        missing (mapv :sym (remove :var resolved))]
    (println "MISSING:" (pr-str missing))
    (let [rows (vec (for [{:keys [sym round var]} resolved
                          :when var]
                      (let [r (binding [t/*test-out* (java.io.StringWriter.)]
                                (t/run-test-var var))]
                        {:witness (name sym) :round round
                         :pass (:pass r 0) :fail (:fail r 0)
                         :error (:error r 0)})))
          total (reduce (fn [acc {:keys [pass fail error]}]
                          (-> acc (update :pass + pass)
                              (update :fail + fail) (update :error + error)))
                        {:test (count rows) :pass 0 :fail 0 :error 0}
                        rows)]
      (println "COMPOSITION:")
      (doseq [{:keys [witness round pass fail error]} rows]
        (println (format "  %-6s %-72s pass %4d  fail %d  error %d"
                         (str round) witness pass fail error)))
      (println ":BATTERY-RESULT" (pr-str total))
      (when (or (seq missing) (pos? (:fail total)) (pos? (:error total)))
        (System/exit 1)))))
