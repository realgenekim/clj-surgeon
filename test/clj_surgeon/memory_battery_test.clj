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
                                      reserved-peak]
                               :or {start 40.0
                                    oom? false
                                    result-hash "h"
                                    reference-hash "h"
                                    reserved-peak 100.0}}]
  {:op op
   :n n
   :phase phase
   :rep 1
   :wall-ms 1000
   :files n
   :bytes (* n 4000)
   :heap-start-mb start
   :heap-used-peak-mb peak
   :heap-after-gc-mb after-gc
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
            :scale-small-n           1000
            :scale-large-n           10000}
           battery/pass-lines))))

;; @spec MCP-OP-MEM-001
(deftest exit-codes-are-the-published-contract
  (is (= 0 (:pass battery/exit-codes)))
  (is (= 1 (:fail battery/exit-codes)))
  (is (= 2 (:refusal battery/exit-codes)))
  (is (= 3 (:tool-failure battery/exit-codes))))

;; ------------------------------------------------------------------
;; The five pass lines
;; ------------------------------------------------------------------

;; @spec MCP-OP-MEM-001
;; @spec MCP-OP-MEM-011
(deftest a-flat-battery-passes-every-line
  (let [result (battery/verdict {:xmx-mb 512 :cells (clean-cells)})]
    (is (true? (:pass? result)))
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

;; @spec MCP-OP-MEM-001
(deftest peak-above-start-plus-headroom-fails
  (testing "264.0 is the budget at start 40 and Xmx 512; 264.1 breaks it"
    (let [cells [(cell :ls-tree 1000 :warm 264.1 50.0)]
          result (battery/verdict {:xmx-mb 512 :cells cells})]
      (is (false? (:pass? result)))
      (is (contains? (lines-of result) :peak-over-budget))))
  (testing "exactly at the budget passes"
    (let [cells [(cell :ls-tree 1000 :warm 264.0 50.0)]
          result (battery/verdict {:xmx-mb 512 :cells cells})]
      (is (not (contains? (lines-of result) :peak-over-budget))))))

;; @spec MCP-OP-MEM-001
(deftest peak-above-the-xmx-fraction-fails-even-with-headroom-to-spare
  (testing "start 300 leaves 524 MB of headroom but 0.80 x 512 = 409.6 binds"
    (let [cells [(cell :ls-tree 1000 :warm 500.0 50.0 :start 300.0)]
          result (battery/verdict {:xmx-mb 512 :cells cells})]
      (is (false? (:pass? result)))
      (is (contains? (lines-of result) :peak-over-budget)))))

;; @spec MCP-OP-MEM-001
(deftest peak-that-scales-with-n-fails
  (testing "10k peak may exceed the 1k peak by at most 32 MB"
    (let [cells [(cell :ls-tree 1000 :warm 120.0 50.0)
                 (cell :ls-tree 10000 :warm 152.0 50.0)]
          result (battery/verdict {:xmx-mb 512 :cells cells})]
      (is (true? (:pass? result))))
    (let [cells [(cell :ls-tree 1000 :warm 120.0 50.0)
                 (cell :ls-tree 10000 :warm 152.1 50.0)]
          result (battery/verdict {:xmx-mb 512 :cells cells})]
      (is (false? (:pass? result)))
      (is (contains? (lines-of result) :peak-scales-with-n))))
  (testing "the worst rep at each N is the one compared"
    (let [cells [(cell :ls-tree 1000 :fresh 120.0 50.0)
                 (cell :ls-tree 1000 :warm 100.0 50.0)
                 (cell :ls-tree 10000 :fresh 100.0 50.0)
                 (cell :ls-tree 10000 :warm 160.0 50.0)]
          result (battery/verdict {:xmx-mb 512 :cells cells})]
      (is (contains? (lines-of result) :peak-scales-with-n)))))

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
