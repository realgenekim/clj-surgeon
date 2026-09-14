(ns clj-surgeon.test-census
  "Shared source derivation and guarded, canonical census writer."
  (:require
   [clojure.edn :as edn]
   [clojure.string :as str]))

(def census-file "test/clj_surgeon/deftest_census.edn")

;; INTENT: REGNS-011
;; @spec REGNS-011
(defn deftest-names [ns-sym source]
  (into (sorted-set)
        (map (fn [[_ nm]] (symbol (str ns-sym) nm)))
        (re-seq #"(?m)^\(deftest\s+([^\s()\[\]{}]+)" (or source ""))))

(defn derived-census [namespace-sources]
  (into (sorted-set) (mapcat (fn [[n source]] (deftest-names n source))) namespace-sources))

(defn write-census-ledger!
  "Writes `census` to `census-ledger-path`, one fully qualified deftest per line,
   sorted. Called by the additions-only guard; each entrance retains its own
   authorization and transaction writer."
  [path census write!]
  (write! path
    (str ";; Deftest census -- DERIVED, regenerated, never hand-edited.\n"
         ";; One FULLY QUALIFIED deftest per line: two branches adding tests\n"
         ";; touch two different lines, and a deleted test is a named line\n"
         ";; that disappears rather than a number that stays plausible.\n"
         ";; Regenerate: see clj-surgeon.lane-manifest-test/census-ledger-path.\n"
         "#{"
         (str/join "\n  " (sort census))
         "}\n")))

(defn census-ledger-diff
  "Named differences between the tree's deftests and the checked-in ledger:
   `:added` are declared in a lane but absent from the ledger, `:removed` are in
   the ledger and no longer declared anywhere. A RENAME appears as one of each,
   which is the whole reason the ledger holds names."
  [derived ledger]
  (let [added (vec (sort (remove ledger derived)))
        removed (vec (sort (remove derived ledger)))]
    (when (or (seq added) (seq removed))
      {:added added :removed removed})))

(defn regenerate-census!
  "Regenerates additions only. A removed name refuses before any write."
  ([path derived] (regenerate-census! path derived slurp spit))
  ([path derived read! write!]
   (let [ledger (edn/read-string (read! path))
         _ (when-not (set? ledger)
             (throw (ex-info "census-regenerate-refused: ledger must be a set" {})))
         {:keys [added removed]} (census-ledger-diff derived ledger)]
     (println (str "census-regenerate: +" (count added) "/-" (count removed)))
     (if (seq removed)
       (do (println "census-regenerate-refused: removed names" (pr-str removed))
           {:ok false :removed removed})
       (do (when (seq added) (write-census-ledger! path derived write!))
           {:ok true :added (or added [])})))))
