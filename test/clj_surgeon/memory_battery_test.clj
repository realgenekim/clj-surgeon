(ns clj-surgeon.memory-battery-test
  "Millisecond-scale witness for MCP-OP-MEM-001 and MCP-OP-MEM-011.

  This namespace runs in the fast suite (`bb test/run_all.clj`). It never
  launches a JVM, never generates a tree, and never measures a heap. It feeds
  hand-written synthetic numbers to the battery's pure verdict function and
  asserts that the published pass lines are applied exactly, and it asserts
  that the battery target exists in the Makefile and is NOT reachable from the
  ordinary test gates.

  Expected values below are hand-written literals. They are never derived from
  the code under test."
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [clj-surgeon.memory-battery :as battery]))

;; ------------------------------------------------------------------
;; Cell fixtures — a battery observation is a flat vector of cells.
;; ------------------------------------------------------------------

(defn- cell
  "Build one synthetic measurement cell. Only the fields a pass line reads
  are varied; the rest are inert reporting fields."
  [op n phase peak after-gc & {:keys [start oom? result-hash reference-hash
                                      reserved-peak held]
                               :or {start 40.0
                                    oom? false
                                    result-hash "h"
                                    reference-hash "h"
                                    reserved-peak 100.0
                                    held 1.0}}]
  {:op op
   :n n
   :phase phase
   :rep 1
   :wall-ms 1000
   :files n
   :bytes (* n 4000)
   :heap-start-mb start
   :heap-used-peak-mb peak
   :heap-result-retained-mb held
   :heap-after-gc-mb after-gc
   ;; result-exclusive retention: what disappeared when the result was dropped
   :heap-held-after-release-mb (- (+ start held) after-gc)
   ;; persistent growth: what the call left behind for good
   :heap-after-release-start-mb (- after-gc start)
   :heap-reserved-peak-mb reserved-peak
   :oom? oom?
   :result-hash result-hash
   :reference-hash reference-hash})

(defn- clean-cells
  "A battery that satisfies every pass line: flat peak and flat retention."
  []
  [(cell :ls-tree 100 :fresh 90.0 45.0)
   (cell :ls-tree 100 :warm 88.0 44.0)
   (cell :ls-tree 1000 :fresh 120.0 50.0)
   (cell :ls-tree 1000 :warm 118.0 49.0)
   (cell :ls-tree 10000 :fresh 130.0 52.0)
   (cell :ls-tree 10000 :warm 128.0 51.0)])

(defn- lines-of
  [result]
  (set (map :line (:failures result))))

;; ------------------------------------------------------------------
;; The budget itself
;; ------------------------------------------------------------------

;; @spec MCP-OP-MEM-001
(deftest peak-budget-is-the-tighter-of-start-headroom-and-the-xmx-fraction
  (testing "headroom binds when the JVM starts small"
    (is (= 264.0 (battery/peak-budget-mb 40.0 512))))
  (testing "the Xmx fraction binds when the JVM starts large"
    (is (= 409.6 (battery/peak-budget-mb 300.0 512))))
  (testing "the published constants are Sol's set, verbatim, in one place"
    (is (= {:reserved-peak-mb        192
            :peak-headroom-mb        224
            :peak-xmx-percent        80
            :scale-peak-slack-mb     32
            :scale-retained-slack-mb 8
            :scale-held-slack-mb     2.0
            :scale-small-n           1000
            :scale-large-n           10000}
           battery/pass-lines))))

;; @spec MCP-OP-MEM-001
(deftest exit-codes-are-the-published-contract
  (is (= 0 (:pass battery/exit-codes)))
  (is (= 1 (:fail battery/exit-codes)))
  (is (= 2 (:refusal battery/exit-codes)))
  (is (= 3 (:tool-failure battery/exit-codes)))
  (is (= 4 (:incomplete battery/exit-codes))
      "INCOMPLETE is a third terminal state with its own nonzero release-gate exit"))

;; @spec MCP-OP-MEM-011
(deftest an-all-green-but-unmeasured-run-is-incomplete-never-a-pass
  (testing "every measured line held, but a line was never observed"
    (let [cells (mapv #(assoc % :heap-reserved-peak-mb nil) (clean-cells))
          result (battery/verdict {:xmx-mb 512 :cells cells})]
      (is (= [] (:failures result)))
      (is (false? (:complete? result)))
      (is (= :incomplete (:status result)))
      (is (false? (:pass? result))
          "an unobserved line is never a pass")
      (is (= 4 (:exit result))
          "INCOMPLETE blocks the release gate with its own exit code")))
  (testing "a failure outranks an unmeasured line"
    (let [cells (conj (mapv #(assoc % :heap-reserved-peak-mb nil) (clean-cells))
                      (cell :ls-tree 10000 :fresh 400.0 60.0 :oom? true))
          result (battery/verdict {:xmx-mb 512 :cells cells})]
      (is (= :fail (:status result)))
      (is (= 1 (:exit result)))))
  (testing "the table names the terminal state"
    (let [cells (mapv #(assoc % :heap-reserved-peak-mb nil) (clean-cells))]
      (is (str/includes? (battery/render-table {:xmx-mb 512 :cells cells})
                         "verdict: INCOMPLETE   exit 4")))))

;; ------------------------------------------------------------------
;; The five pass lines
;; ------------------------------------------------------------------

;; @spec MCP-OP-MEM-001
;; @spec MCP-OP-MEM-011
(deftest a-flat-battery-passes-every-line
  (let [result (battery/verdict {:xmx-mb 512 :cells (clean-cells)})]
    (is (true? (:pass? result)))
    (is (= :pass (:status result)))
    (is (true? (:complete? result)))
    (is (= [] (:failures result)))
    (is (= 0 (:exit result)))))

;; @spec MCP-OP-MEM-001
;; @spec MCP-OP-MEM-011
(deftest any-oom-fails-the-battery
  (let [cells (conj (clean-cells) (cell :ls-tree 10000 :fresh 400.0 60.0 :oom? true))
        result (battery/verdict {:xmx-mb 512 :cells cells})]
    (is (false? (:pass? result)))
    (is (contains? (lines-of result) :oom))
    (is (= 1 (:exit result)))))

;; The two peak lines are TREND signals, not gates. peak_mb is a 5 ms sampled,
;; process-wide used-heap peak containing garbage, and G1 moves it with -Xmx and
;; collector scheduling: Sol re-ran an IDENTICAL cell (cli-ls-tree, N=1,000,
;; fresh) and it fell 274.8 -> 246.5 MB, a 28.3 MB swing that crossed the verdict
;; line in both directions. A gate that flips on a rerun of the same work is a
;; flaky gate; the numbers still belong in the receipt as a regression signal.
(defn- trend-lines-of
  [result]
  (set (map :line (:trends result))))

;; @spec MCP-OP-MEM-001
(deftest peak-above-start-plus-headroom-is-a-trend-not-a-gate
  (testing "264.0 is the budget at start 40 and Xmx 512; 264.1 crosses it"
    (let [cells [(cell :ls-tree 1000 :warm 264.1 50.0)]
          result (battery/verdict {:xmx-mb 512 :cells cells})]
      (is (= [] (:failures result))
          "a sampled process-wide peak never fails the battery on its own")
      (is (contains? (trend-lines-of result) :peak-over-budget))
      (is (= 264.0 (:limit (first (:trends result)))))))
  (testing "exactly at the budget reports nothing"
    (let [cells [(cell :ls-tree 1000 :warm 264.0 50.0)]
          result (battery/verdict {:xmx-mb 512 :cells cells})]
      (is (not (contains? (trend-lines-of result) :peak-over-budget))))))

;; @spec MCP-OP-MEM-001
(deftest peak-above-the-xmx-fraction-is-a-trend-too
  (testing "start 300 leaves 524 MB of headroom but 0.80 x 512 = 409.6 binds"
    (let [cells [(cell :ls-tree 1000 :warm 500.0 50.0 :start 300.0)]
          result (battery/verdict {:xmx-mb 512 :cells cells})]
      (is (= [] (:failures result)))
      (is (contains? (trend-lines-of result) :peak-over-budget)))))

;; @spec MCP-OP-MEM-001
(deftest peak-that-scales-with-n-is-reported-as-a-trend
  (testing "10k peak more than 32 MB above the 1k peak is a trend, not a failure"
    (let [cells [(cell :ls-tree 1000 :warm 120.0 50.0)
                 (cell :ls-tree 10000 :warm 152.0 50.0)]
          result (battery/verdict {:xmx-mb 512 :cells cells})]
      (is (true? (:pass? result)))
      (is (= #{} (trend-lines-of result))))
    (let [cells [(cell :ls-tree 1000 :warm 120.0 50.0)
                 (cell :ls-tree 10000 :warm 152.1 50.0)]
          result (battery/verdict {:xmx-mb 512 :cells cells})]
      (is (= [] (:failures result)))
      (is (true? (:pass? result))
          "a trend line does not change the terminal state")
      (is (contains? (trend-lines-of result) :peak-scales-with-n))))
  (testing "the worst rep at each N is still the one compared"
    (let [cells [(cell :ls-tree 1000 :fresh 120.0 50.0)
                 (cell :ls-tree 1000 :warm 100.0 50.0)
                 (cell :ls-tree 10000 :fresh 100.0 50.0)
                 (cell :ls-tree 10000 :warm 160.0 50.0)]
          result (battery/verdict {:xmx-mb 512 :cells cells})]
      (is (contains? (trend-lines-of result) :peak-scales-with-n)))))

;; @spec MCP-OP-MEM-001
;; @spec MCP-OP-MEM-011
(deftest the-hard-lines-stay-hard-while-the-peak-lines-do-not
  (testing "OOM, parity, reserved peak, held and persistent growth all fail"
    (doseq [[label cells]
            [[:oom [(cell :ls-tree 1000 :warm 120.0 50.0 :oom? true)]]
             [:parity [(cell :ls-tree 1000 :warm 120.0 50.0
                             :result-hash "bounded" :reference-hash "unbounded")]]
             [:reserved [(cell :ls-tree 1000 :warm 120.0 50.0 :reserved-peak 192.1)]]
             [:held [(cell :ls-tree 1000 :warm 120.0 50.0 :held 1.0)
                     (cell :ls-tree 10000 :warm 120.0 50.0 :held 3.1)]]
             [:growth [(cell :ls-tree 1000 :warm 120.0 45.0 :start 40.0)
                       (cell :ls-tree 10000 :warm 120.0 45.0 :start 20.0)]]]]
      (testing (str label)
        (let [result (battery/verdict {:xmx-mb 512 :cells cells})]
          (is (= :fail (:status result)))
          (is (= 1 (:exit result)))))))
  (testing "a peak far over every budget, alone, is not a failure"
    (let [cells [(cell :ls-tree 1000 :warm 500.0 50.0)
                 (cell :ls-tree 10000 :warm 500.0 50.0)]
          result (battery/verdict {:xmx-mb 512 :cells cells})]
      (is (= [] (:failures result)))
      (is (= :pass (:status result)))
      (is (= 0 (:exit result)))))
  (testing "the table marks trend cells and prints the trend lines"
    (let [cells [(cell :ls-tree 1000 :warm 500.0 50.0)
                 (cell :ls-tree 10000 :warm 500.0 50.0)]
          table (battery/render-table {:xmx-mb 512 :cells cells})]
      (is (str/includes? table "TREND peak-over-budget"))
      (is (str/includes? table "trend"))
      (is (not (str/includes? table "FAIL"))))))

;; @spec MCP-OP-MEM-001
(deftest retention-that-scales-with-n-fails
  (testing "10k after-GC retention may exceed 1k retention by at most 8 MB"
    (let [cells [(cell :ls-tree 1000 :warm 120.0 50.0)
                 (cell :ls-tree 10000 :warm 120.0 58.0)]
          result (battery/verdict {:xmx-mb 512 :cells cells})]
      (is (true? (:pass? result))))
    (let [cells [(cell :ls-tree 1000 :warm 120.0 50.0)
                 (cell :ls-tree 10000 :warm 120.0 58.1)]
          result (battery/verdict {:xmx-mb 512 :cells cells})]
      (is (false? (:pass? result)))
      (is (contains? (lines-of result) :retained-scales-with-n)))))

;; The numbers below are the ones the battery actually measured on this branch
;; (docs/observations/2026-09-03-memory-battery-baseline.md): the full-match
;; rename arm held 1.0 MiB at N=1,000 and 9.8 MiB at N=10,000. Nothing gated
;; that, so a result whose retained size grew ~10x with the repository passed.
;; @spec MCP-OP-MEM-011
(deftest held-heap-that-scales-with-n-fails
  (testing "the measured full-match arm: 1.0 MiB at 1,000 files, 9.8 at 10,000"
    (let [cells [(cell :rename-ns-plan-full-match 1000 :warm 195.7 24.1 :held 1.0)
                 (cell :rename-ns-plan-full-match 10000 :warm 202.7 24.1 :held 9.8)]
          result (battery/verdict {:xmx-mb 512 :cells cells})]
      (is (false? (:pass? result)))
      (is (contains? (lines-of result) :held-scales-with-n))
      (is (= {:op :rename-ns-plan-full-match
              :profile :default
              :line :held-scales-with-n
              :observed 9.8
              :limit 3.0
              :small-n-observed 1.0
              :slack-mb 2.0}
             (first (filter #(= :held-scales-with-n (:line %)) (:failures result)))))))

  (testing "the measured narrow arm is flat and passes"
    (let [cells [(cell :rename-ns-plan-narrow 1000 :warm 194.7 24.1 :held 0.1)
                 (cell :rename-ns-plan-narrow 10000 :warm 196.9 24.1 :held 0.1)]
          result (battery/verdict {:xmx-mb 512 :cells cells})]
      (is (true? (:pass? result)))))

  (testing "the slack is 2.0 MiB exactly"
    (let [at-line [(cell :ls-tree 1000 :warm 120.0 50.0 :held 1.0)
                   (cell :ls-tree 10000 :warm 120.0 50.0 :held 3.0)]
          over [(cell :ls-tree 1000 :warm 120.0 50.0 :held 1.0)
                (cell :ls-tree 10000 :warm 120.0 50.0 :held 3.1)]]
      (is (not (contains? (lines-of (battery/verdict {:xmx-mb 512 :cells at-line}))
                          :held-scales-with-n)))
      (is (contains? (lines-of (battery/verdict {:xmx-mb 512 :cells over}))
                     :held-scales-with-n))))

  (testing "a battery with no held measurement reports UNMEASURED, not a pass"
    (let [cells (mapv #(dissoc % :heap-result-retained-mb)
                      [(cell :ls-tree 1000 :warm 120.0 50.0)
                       (cell :ls-tree 10000 :warm 120.0 50.0)])
          result (battery/verdict {:xmx-mb 512 :cells cells})]
      (is (= [] (:failures result)))
      (is (contains? (set (map :line (:unmeasured result))) :held-scales-with-n))
      (is (= :incomplete (:status result))))))

;; @spec MCP-OP-MEM-001
;; @spec MCP-OP-MEM-011
(deftest the-leak-line-gates-persistent-growth-not-the-absolute-after-gc-heap
  (testing "identical absolute after-GC heap, different persistent growth"
    ;; Both cells end at 45.0 MB of used heap, so the old absolute comparison saw
    ;; no difference at all. The 10,000-file call nevertheless left 25 MB behind
    ;; where the 1,000-file call left 5.
    (let [cells [(cell :ls-tree 1000 :warm 120.0 45.0 :start 40.0)
                 (cell :ls-tree 10000 :warm 120.0 45.0 :start 20.0)]
          result (battery/verdict {:xmx-mb 512 :cells cells})]
      (is (false? (:pass? result)))
      (is (contains? (lines-of result) :retained-scales-with-n))
      (is (= {:op :ls-tree :profile :default :line :retained-scales-with-n
              :observed 25.0 :limit 13.0 :small-n-observed 5.0 :slack-mb 8}
             (first (filter #(= :retained-scales-with-n (:line %))
                            (:failures result)))))))

  (testing "flat persistent growth passes even as the absolute heap moves"
    (let [cells [(cell :ls-tree 1000 :warm 120.0 45.0 :start 40.0)
                 (cell :ls-tree 10000 :warm 120.0 65.0 :start 60.0)]
          result (battery/verdict {:xmx-mb 512 :cells cells})]
      (is (true? (:pass? result)))))

  (testing "the table shows result-exclusive retention and persistent growth"
    (let [table (battery/render-table {:xmx-mb 512 :cells (clean-cells)})]
      (is (str/includes? table "excl_mb"))
      (is (str/includes? table "grow_mb")))))

;; @spec MCP-OP-MEM-001
;; @spec MCP-OP-MEM-011
(deftest a-result-that-differs-from-the-unbounded-reference-fails
  (let [cells [(cell :ls-tree 1000 :warm 120.0 50.0
                     :result-hash "bounded" :reference-hash "unbounded")]
        result (battery/verdict {:xmx-mb 512 :cells cells})]
    (is (false? (:pass? result)))
    (is (contains? (lines-of result) :reference-mismatch))))

;; @spec MCP-OP-MEM-011
(deftest a-missing-unbounded-reference-is-unmeasured-not-passed
  (testing "output parity cannot pass when there is nothing to compare against"
    (let [cells [(cell :ls-tree 1000 :warm 120.0 50.0
                       :result-hash "bounded" :reference-hash nil)
                 (cell :ls-tree 10000 :warm 120.0 50.0
                       :result-hash "bounded" :reference-hash nil)]
          result (battery/verdict {:xmx-mb 512 :cells cells})]
      (is (= [] (:failures result)))
      (is (false? (:complete? result)))
      (is (contains? (set (map :line (:unmeasured result))) :reference-mismatch)))))

;; @spec MCP-OP-MEM-001
(deftest scale-lines-with-no-large-n-cells-are-unmeasured-not-passed
  (testing "stopping at 1,000 files reports an unmeasured scale line, not a pass"
    (let [cells [(cell :ls-tree 100 :warm 90.0 45.0)
                 (cell :ls-tree 1000 :warm 120.0 50.0)]
          result (battery/verdict {:xmx-mb 512 :cells cells})]
      (is (= [] (:failures result)))
      (is (= #{{:op :ls-tree :line :peak-scales-with-n}
               {:op :ls-tree :line :held-scales-with-n}
               {:op :ls-tree :line :retained-scales-with-n}}
             (set (map #(select-keys % [:op :line]) (:unmeasured result)))))
      (is (false? (:complete? result))))))

;; @spec MCP-OP-MEM-001
(deftest the-reserved-peak-line-is-unmeasured-without-an-admission-accountant
  (testing "a cell that reports no attributable reserved peak does not pass the line"
    (let [cells [(cell :ls-tree 1000 :warm 120.0 50.0 :reserved-peak nil)
                 (cell :ls-tree 10000 :warm 120.0 50.0 :reserved-peak nil)]
          result (battery/verdict {:xmx-mb 512 :cells cells})]
      (is (= [] (:failures result)))
      (is (contains? (set (map :line (:unmeasured result)))
                     :reserved-peak-over-budget))
      (is (false? (:complete? result)))))
  (testing "a reported reserved peak above 192 MiB fails"
    (let [cells [(cell :ls-tree 1000 :warm 120.0 50.0 :reserved-peak 192.1)]
          result (battery/verdict {:xmx-mb 512 :cells cells})]
      (is (false? (:pass? result)))
      (is (contains? (lines-of result) :reserved-peak-over-budget))))
  (testing "exactly 192 MiB passes"
    (let [cells [(cell :ls-tree 1000 :warm 120.0 50.0 :reserved-peak 192.0)]
          result (battery/verdict {:xmx-mb 512 :cells cells})]
      (is (not (contains? (lines-of result) :reserved-peak-over-budget))))))

;; @spec MCP-OP-MEM-001
(deftest each-op-is-judged-independently
  (let [cells (concat (clean-cells)
                      [(cell :workspace-sources 1000 :warm 120.0 50.0)
                       (cell :workspace-sources 10000 :warm 400.0 200.0)])
        result (battery/verdict {:xmx-mb 512 :cells cells})]
    (is (false? (:pass? result)))
    (is (= #{:workspace-sources}
           (set (map :op (:failures result)))))))

;; ------------------------------------------------------------------
;; Adversarial corpus arms are measured beside the default trees, never
;; averaged into them
;; ------------------------------------------------------------------

;; @spec MCP-OP-MEM-011
(deftest the-adversarial-arms-are-separate-corpora-not-extra-cells
  (testing "the arms are the cheap shapes, at one size each"
    (is (= [[:cljc 100] [:giant 1] [:nested 1]] battery/extra-corpus-arms)))

  (testing "default scales come first, then the adversarial arms"
    (is (= [[:default 100] [:default 1000] [:default 10000]
            [:cljc 100] [:giant 1] [:nested 1]]
           (battery/corpus-arms [1000 100 10000]))))

  (testing "the corpus root directory names"
    (is (= "10000" (battery/tree-dir-name :default 10000)))
    (is (= "cljc-100" (battery/tree-dir-name :cljc 100)))
    (is (= "giant-1" (battery/tree-dir-name :giant 1))))

  (testing "a cross-N line never compares one corpus against another"
    ;; A giant-profile cell at N=1 and a default cell at N=10,000 are two
    ;; different corpora; held heap growing between them says nothing.
    (let [cells [(assoc (cell :ls-tree 1000 :warm 120.0 50.0 :held 1.0)
                        :profile :default)
                 (assoc (cell :ls-tree 10000 :warm 120.0 50.0 :held 1.0)
                        :profile :default)
                 (assoc (cell :ls-tree 1 :warm 120.0 50.0 :held 90.0)
                        :profile :giant)]
          result (battery/verdict {:xmx-mb 512 :cells cells})]
      (is (= :pass (:status result)))))

  (testing "and an adversarial arm never becomes the small-N or large-N term"
    (let [cells [(assoc (cell :ls-tree 1000 :warm 120.0 50.0 :held 1.0)
                        :profile :default)
                 (assoc (cell :ls-tree 10000 :warm 120.0 50.0 :held 1.0)
                        :profile :default)
                 ;; same op, same N labels, other corpus, wild growth
                 (assoc (cell :ls-tree 1000 :warm 120.0 50.0 :held 1.0)
                        :profile :cljc)
                 (assoc (cell :ls-tree 10000 :warm 120.0 50.0 :held 99.0)
                        :profile :cljc)]
          result (battery/verdict {:xmx-mb 512 :cells cells})]
      (is (= :pass (:status result)))))

  (testing "a cell with no :profile key is the default corpus"
    (let [cells [(cell :ls-tree 1000 :warm 120.0 50.0 :held 1.0)
                 (cell :ls-tree 10000 :warm 120.0 50.0 :held 9.8)]
          result (battery/verdict {:xmx-mb 512 :cells cells})]
      (is (contains? (lines-of result) :held-scales-with-n))))

  (testing "the table names each cell's corpus"
    (let [table (battery/render-table
                  {:xmx-mb 512
                   :cells [(assoc (cell :ls-tree 1 :warm 120.0 50.0)
                                  :profile :giant)]})]
      (is (str/includes? table "prof"))
      (is (str/includes? table "giant")))))

;; @spec MCP-OP-MEM-011
(deftest every-reported-line-names-the-corpus-it-came-from
  ;; The giant and nested arms both sit at N=1. Without the corpus in the
  ;; identity, their receipt lines are indistinguishable — two different
  ;; findings printed as if they were the same one.
  ;; start 24.0 is the JVM's real measured baseline, so the budget is 248.0 and
  ;; both of these measured peaks cross it.
  (let [cells [(assoc (cell :ls-tree 1 :fresh 333.9 50.0 :start 24.0)
                      :profile :giant)
               (assoc (cell :ls-tree 1 :fresh 259.0 50.0 :start 24.0)
                      :profile :nested)]
        result (battery/verdict {:xmx-mb 512 :cells cells})
        trends (:trends result)]
    (is (= 2 (count trends)))
    (is (= #{:giant :nested} (set (map :profile trends)))
        "each trend line names the corpus that produced it")
    (is (= {:op :ls-tree :profile :giant :n 1 :phase :fresh :rep 1}
           (select-keys (first (filter #(= :giant (:profile %)) trends))
                        [:op :profile :n :phase :rep]))))
  (testing "a default-corpus cell still says so"
    (let [cells [(cell :ls-tree 1000 :warm 500.0 50.0)]
          result (battery/verdict {:xmx-mb 512 :cells cells})]
      (is (= :default (:profile (first (:trends result))))))))

;; @spec MCP-OP-MEM-011
(deftest the-generator-and-the-battery-agree-on-every-arm
  ;; Two files name the corpus arms — the bb generator and the pure battery ns.
  ;; They are loaded here and compared, so they cannot drift apart silently.
  (let [gen-ns (do (load-file "bench/memory_battery/generate_tree.clj")
                   (find-ns 'generate-tree))
        gen-arms @(ns-resolve gen-ns 'profile-arms)
        gen-dir-name @(ns-resolve gen-ns 'tree-dir-name)
        gen-profiles @(ns-resolve gen-ns 'profiles)]
    (is (= battery/extra-corpus-arms gen-arms)
        "the generator builds exactly the arms the battery measures")
    (doseq [[profile n] (battery/corpus-arms [100 1000 10000])]
      (testing (str profile "-" n)
        (is (= (battery/tree-dir-name profile n) (gen-dir-name profile n))
            "both sides must look in the same directory")
        (is (contains? gen-profiles profile)
            "every measured arm is a profile the generator knows how to build")))))

;; ------------------------------------------------------------------
;; The unbounded reference must be attested to the thing it measured
;; ------------------------------------------------------------------

(defn- attestation
  [& {:as overrides}]
  (merge {:ops [:ls-tree :workspace-sources]
          :ops-digest "opsdigest"
          :src-digest "srcdigest"
          :generator-digest "gendigest"
          :corpus-digests {1000 "c1k" 10000 "c10k"}
          :jvm "21.0.8"}
         overrides))

;; @spec MCP-OP-MEM-011
(deftest a-reference-not-attested-to-this-corpus-and-code-is-refused
  (testing "a reference stamped with the same code, corpus and JVM is fresh"
    (is (nil? (battery/reference-staleness
                (attestation)
                {:attestation (attestation) :hashes {}}))))

  (testing "a reference stamped with a DIFFERENT corpus digest is stale"
    (let [stale (battery/reference-staleness
                  (attestation)
                  {:attestation (attestation :corpus-digests {1000 "c1k" 10000 "OTHER"})
                   :hashes {}})]
      (is (= :stale-reference (:reason stale)))
      (is (= [:corpus-digests] (:fields stale)))))

  (testing "the other bound fields are checked too"
    (doseq [[field value] [[:src-digest "other"]
                           [:generator-digest "other"]
                           [:ops-digest "other"]
                           [:ops [:ls-tree]]
                           [:jvm "17.0.1"]]]
      (testing (str field)
        (is (= [field]
               (:fields (battery/reference-staleness
                          (attestation)
                          {:attestation (attestation field value) :hashes {}})))))))

  (testing "a missing reference is typed, not silently absent"
    (is (= :no-reference (:reason (battery/reference-staleness (attestation) nil)))))

  (testing "a reference file carrying no attestation at all is never trusted"
    (is (= :unattested-reference
           (:reason (battery/reference-staleness
                      (attestation)
                      {:ls-tree {1000 "hash"}})))))

  (testing "an attestation that could not be computed fails closed"
    (is (= :attestation-unavailable
           (:reason (battery/reference-staleness
                      (attestation :src-digest :unavailable)
                      {:attestation (attestation :src-digest :unavailable)
                       :hashes {}}))))))

;; ------------------------------------------------------------------
;; Attestation names WHAT the reference measured; anchoring binds it to
;; its OWN bytes, so a hand-written reference with correct-looking
;; attestation fields but forged hashes cannot pass. Sol's probe:
;; "A hand-written reference with current attestation fields and
;; :hashes {:forged "arbitrary"} passed memory-battery-attest."
;; ------------------------------------------------------------------

;; @spec MCP-OP-MEM-011
(deftest reference-canonicalization-is-order-independent-and-value-sensitive
  (testing "key insertion order never changes the canonical bytes"
    (is (= (battery/canonical-reference-str {:b 2 :a 1 :hashes {:z 1 :y 2}})
           (battery/canonical-reference-str {:a 1 :hashes {:y 2 :z 1} :b 2}))))

  (testing "nested maps under vectors and maps are canonicalized too"
    (is (= (battery/canonical-reference-str {:xs [{:b 1 :a 2}]})
           (battery/canonical-reference-str {:xs [{:a 2 :b 1}]}))))

  (testing "an actual value change produces different bytes"
    (is (not= (battery/canonical-reference-str {:hashes {:ls-tree "h1"}})
              (battery/canonical-reference-str {:hashes {:ls-tree "h2"}})))))

;; @spec MCP-OP-MEM-011
(deftest a-reference-not-anchored-to-its-own-bytes-is-refused
  (testing "a missing sidecar is refused, typed :reference-unanchored"
    (let [issue (battery/reference-anchor-mismatch nil "computed-hash")]
      (is (= :reference-unanchored (:reason issue)))
      (is (= :missing (get-in issue [:detail :sidecar])))))

  (testing "a sidecar that does not match the reference's own bytes is refused —
            this is Sol's forged-reference probe: correct attestation, forged :hashes"
    (let [issue (battery/reference-anchor-mismatch "sidecar-says-abc" "computed-says-xyz")]
      (is (= :reference-unanchored (:reason issue)))
      (is (= :mismatch (get-in issue [:detail :sidecar])))
      (is (= "computed-says-xyz" (get-in issue [:detail :expected])))
      (is (= "sidecar-says-abc" (get-in issue [:detail :found])))))

  (testing "a sidecar matching the reference's own bytes is fresh"
    (is (nil? (battery/reference-anchor-mismatch "same-hash" "same-hash")))))

;; @spec MCP-OP-MEM-011
(deftest a-reference-naming-the-wrong-ops-catalogue-is-refused
  (testing "hashes for exactly the ops catalogue is fresh"
    (is (nil? (battery/reference-ops-mismatch
                {:hashes {:ls-tree {} :workspace-sources {}}}
                [:ls-tree :workspace-sources]))))

  (testing "an extra key the ops catalogue never named — Sol's :forged \"arbitrary\" —
            is refused, typed :reference-ops-mismatch"
    (let [issue (battery/reference-ops-mismatch
                  {:hashes {:ls-tree {} :forged "arbitrary"}}
                  [:ls-tree :workspace-sources])]
      (is (= :reference-ops-mismatch (:reason issue)))
      (is (= [:forged] (get-in issue [:detail :extra])))
      (is (= [:workspace-sources] (get-in issue [:detail :missing])))))

  (testing "a missing op is refused the same way"
    (let [issue (battery/reference-ops-mismatch
                  {:hashes {:ls-tree {}}}
                  [:ls-tree :workspace-sources])]
      (is (= [:workspace-sources] (get-in issue [:detail :missing])))
      (is (= [] (get-in issue [:detail :extra]))))))

;; ------------------------------------------------------------------
;; The battery is a gate, and it is not in the fast gates
;; ------------------------------------------------------------------

(defn- makefile-text []
  (slurp (io/file "Makefile")))

;; @spec MCP-OP-MEM-001
;; @spec MCP-OP-MEM-011
(deftest makefile-target-parsing-is-exact
  (let [targets (battery/parse-makefile-targets
                  (str "a: b c\n"
                       "\t@echo one\n"
                       "\t$(MAKE) --no-print-directory d\n"
                       "\n"
                       "VAR := not-a-target\n"
                       "OTHER ?= also-not-a-target\n"
                       ".PHONY: a b\n"
                       "b:\n"
                       "\t@echo two\n"))]
    (is (= #{"a" "b"} (set (keys targets))))
    (is (= ["b" "c"] (:prerequisites (get targets "a"))))
    (is (= ["@echo one" "$(MAKE) --no-print-directory d"]
           (:recipe (get targets "a"))))
    (is (= #{"a" "b" "c" "d"} (battery/target-closure targets "a")))))

;; @spec MCP-OP-MEM-001
;; @spec MCP-OP-MEM-011
(deftest the-battery-target-exists-and-carries-its-intent-id
  (let [targets (battery/parse-makefile-targets (makefile-text))]
    (is (contains? targets "memory-battery")
        "make memory-battery is the merge gate for MCP-OP-MEM-001")
    (is (str/includes? (str/join "\n" (:recipe (get targets "memory-battery")))
                       "@spec MCP-OP-MEM-011")
        "the battery target names the release-gate intent it witnesses")))

;; @spec MCP-OP-MEM-001
;; @spec MCP-OP-MEM-011
(deftest the-battery-is-not-reachable-from-the-ordinary-test-gates
  (let [targets (battery/parse-makefile-targets (makefile-text))]
    (doseq [gate ["test" "test-fast" "mcp-test" "runtests"]]
      (testing gate
        (is (contains? targets gate))
        (is (not (contains? (battery/target-closure targets gate) "memory-battery"))
            (str "make " gate
                 " must not launch the memory battery: it is a minutes-scale"
                 " measurement, deliberately kept out of the fast gates"))))))

;; ------------------------------------------------------------------
;; A fresh MEMBAT_ROOT must never auto-launch the 4g reference JVM as a
;; side effect of `make memory-battery` — Sol's finding 8.
;; ------------------------------------------------------------------

;; @spec MCP-OP-MEM-011
(deftest membat-reference-defaults-to-require-not-auto
  (is (str/includes? (makefile-text) "MEMBAT_REFERENCE ?= require")
      "MEMBAT_REFERENCE must default to require, not auto, so a stale/missing"))

;; @spec MCP-OP-MEM-011
(deftest a-stale-reference-does-not-silently-rebuild-under-the-default
  (let [targets (battery/parse-makefile-targets (makefile-text))
        recipe (str/join "\n" (:recipe (get targets "memory-battery")))]
    (testing "the recipe is gated on MEMBAT_REFERENCE, not an unconditional ||"
      (is (str/includes? recipe "MEMBAT_REFERENCE")
          "the memory-battery recipe must consult MEMBAT_REFERENCE before rebuilding"))
    (testing "a stale/missing reference under the default refuses with a typed reason"
      (is (str/includes? recipe "membat-reference-required")
          "the refusal must be typed and greppable, not a bare shell failure"))
    (testing "the old unconditional auto-rebuild fallback is gone"
      (is (not (re-find #"memory-battery-attest\s*\\\s*\n\s*\|\|\s*\$\(MAKE\)\s*--no-print-directory\s*memory-battery-reference\s*$"
                        recipe))
          "attest failure must not unconditionally fall through to the 4g reference build"))))
