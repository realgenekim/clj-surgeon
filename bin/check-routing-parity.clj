#!/usr/bin/env bb
;; Routing-table parity guard.
;;
;; The decision table is COPIED BY HAND into every prompt surface. Nothing
;; generates it, so nothing but this check stops one copy from drifting into a
;; different rule than the seat next to it. It asserts:
;;   1. every rendering's table is BYTE-IDENTICAL to the canonical section's;
;;   2. the managed plate's pointer heading really exists in the canonical file;
;;   3. every document the plate cites exists on disk.
;; Run: bb bin/check-routing-parity.clj   (exit 0 green, exit 2 on drift)

(require '[clojure.string :as str]
         '[babashka.fs :as fs])

(def canonical "skills/clj-surgeon/SKILL.md")

(def renderings
  ["skill.md"
   "skills/safe-refactor/SKILL.md"
   "CLAUDE.md"
   "AGENTS.md"
   "docs/observations/2026-09-06-routing-prompt-surfaces.md"])

(def plate "resources/clj-surgeon-agent-routing.md")

(def pointer-heading "## Edit routing (policy revision 1, 2026-09-06)")

(defn table
  "The contiguous run of table rows beginning at the decision table's header."
  [path]
  (let [lines (str/split-lines (slurp path))
        start (first (keep-indexed #(when (= "| Situation | Route |" %2) %1) lines))]
    (when start
      (str/join "\n" (take-while #(str/starts-with? % "|") (drop start lines))))))

(defn -main []
  (let [expected (table canonical)
        failures (atom [])
        fail! (fn [m] (swap! failures conj m))]
    (when-not expected
      (fail! {:check :canonical-table-present :path canonical
              :problem "no `| Situation | Route |` table found in the canonical section"}))
    (doseq [path renderings]
      (let [actual (table path)]
        (cond
          (nil? actual)
          (fail! {:check :table-present :path path
                  :problem "no routing decision table found"})
          (not= expected actual)
          (fail! {:check :table-parity :path path
                  :problem "routing table differs from the canonical section"
                  :canonical-rows (count (str/split-lines expected))
                  :rendered-rows (count (str/split-lines actual))
                  :first-difference
                  (first (remove nil?
                                 (map (fn [i e a] (when (not= e a) {:row i :canonical e :rendered a}))
                                      (range)
                                      (concat (str/split-lines expected) (repeat nil))
                                      (concat (str/split-lines actual) (repeat nil)))))}))))
    (let [plate-text (slurp plate)
          canonical-text (slurp canonical)]
      (when-not (str/includes? canonical-text pointer-heading)
        (fail! {:check :plate-pointer-resolves :path canonical
                :problem (str "the plate points at heading " (pr-str pointer-heading)
                              " but the canonical file does not contain it")}))
      (when-not (str/includes? plate-text "Edit routing (policy revision 1, 2026-09-06)")
        (fail! {:check :plate-names-the-heading :path plate
                :problem "the plate no longer names the canonical section heading"}))
      (doseq [cited (re-seq #"docs/observations/[A-Za-z0-9._-]+\.md" plate-text)]
        (when-not (fs/exists? cited)
          (fail! {:check :plate-citation-resolves :path plate
                  :problem (str "the plate cites " cited ", which does not exist")}))))
    (if (seq @failures)
      (do (prn {:ok false :operation :check-routing-parity
                :canonical canonical :failures @failures})
          (System/exit 2))
      (prn {:ok true :operation :check-routing-parity
            :canonical canonical
            :table-rows (count (str/split-lines expected))
            :renderings-checked (count renderings)
            :plate plate
            :pointer-heading pointer-heading}))))

(-main)
