(ns clj-surgeon.measured-channel-test
  "A MEASURED field may never live inside a value another requirement hashes.

  Three ratified rows meet on one field. `MCP-OP-MEM-005` requires the scan's
  own cost, `scan_ms`, in the ls-tree receipt UNCONDITIONALLY. `MCP-OP-MEM-011`
  hashes an operation's whole result and compares it to an unbounded reference
  (the battery's `reference-mismatch` line). `MCP-OP-MEM-003` requires two scans
  of an unchanged tree to be byte-identical. `scan_ms` is a wall-clock reading,
  so as long as it sits inside the hashed value the second and third rows can
  never hold while the first does.

  Measured on this branch before the fix, two back-to-back EDN scans of one
  unchanged tree:

      equal? false
      A receipt: {:receipt {:resources {:scan_ms 12.266, :bytes_scanned 928}}}
      B receipt: {:receipt {:resources {:scan_ms 3.405, :bytes_scanned 928}}}
      records equal (receipt dropped)? true

  That is the battery's twelve `reference-mismatch` FAIL lines on `cli-ls-tree`
  (`nondeterministic:4` — four output hashes over five reps of ONE operation on
  ONE unchanged corpus) reproduced in milliseconds, without a battery run.

  The ruling this file witnesses: keep the meter, move the MEASURED fields off
  the hashed channel. The hashed channel carries the deterministic result and
  the deterministic resource facts — `bytes_scanned` stays in it — and the
  measured fields ride beside it, clearly labelled.

  @spec MCP-OP-MEM-005
  @spec MCP-OP-MEM-011"
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [clj-surgeon.core :as core]
   [clj-surgeon.measured :as measured]))

;; ============================================================
;; The subject
;; ============================================================

(defn- battery-parity-subject
  "Exactly what `clj-surgeon.memory-battery-runner/hash-result` digests.

  The battery is deliberately unreachable from `make test` and `make mcp-test`
  (`clj-surgeon.memory-battery-test` asserts that), so this witness cannot call
  the runner. It names the runner's subject instead, and
  `clj-surgeon.memory-battery-test/the-battery-hashes-the-hashed-channel`
  binds this definition to the runner's actual hashing site by reading its
  source. A witness whose notion of the subject drifts from the battery's
  proves nothing about the battery.

  The projection is the product's own, not this witness's copy of it."
  [result]
  (measured/hashed-channel result))

(defn- measured-scan-ms
  "The scan's own cost as the receipt publishes it."
  [edn-result]
  (get-in (last edn-result) [:receipt :resources :measured :scan_ms]))

;; ============================================================
;; Fixtures
;; ============================================================

(defn- scratch-tree!
  "A small ordinary project: enough source that the admission scan really
   charges something, few enough files that two scans cost milliseconds."
  []
  (let [dir (java.nio.file.Files/createTempDirectory
              "measured-channel" (into-array java.nio.file.attribute.FileAttribute []))
        root (.toFile dir)
        src (io/file root "src" "fixture")]
    (.mkdirs src)
    (spit (io/file root "deps.edn") "{:paths [\"src\"]}\n")
    (doseq [i (range 12)]
      (spit (io/file src (str "m" i ".clj"))
            (str "(ns fixture.m" i "\n"
                 "  (:require [clojure.string :as str]))\n\n"
                 "(defn f" i " [x] (str/upper-case (str x)))\n"
                 "(defn g" i " [x y] (+ x y (* " i " 2)))\n")))
    (.getPath root)))

(defn- scratch-tree-with-a-refusal!
  "The same, plus one file the parser-admission ceiling refuses on depth. The
   TEXT encoding prints its `resources` line only inside the refusal block, so
   this is the only shape in which text can carry a measured field at all."
  []
  (let [root (scratch-tree!)
        tower (str "(ns fixture.tower)\n(def t "
                   (apply str (repeat 700 "@"))
                   "x)\n")]
    (spit (io/file root "src" "fixture" "tower.clj") tower)
    root))

(defn- delete-tree! [root]
  (doseq [^java.io.File f (reverse (file-seq (io/file root)))]
    (.delete f)))

;; ============================================================
;; The requirement
;; ============================================================

;; @spec MCP-OP-MEM-005
;; @spec MCP-OP-MEM-011
(deftest two-scans-of-an-unchanged-tree-agree-on-the-hashed-channel
  (testing "a wall-clock reading may not live inside a value another row hashes"
    (let [root (scratch-tree!)]
      (try
        (let [a (core/run-ls-tree {:dir root :format :edn})
              b (core/run-ls-tree {:dir root :format :edn})
              pa (pr-str (battery-parity-subject a))
              pb (pr-str (battery-parity-subject b))]

          ;; 1. Deterministic red: no MEASURED field may appear in the channel
          ;;    the battery hashes. This assertion does not depend on two scans
          ;;    happening to take different amounts of time.
          (is (not (str/includes? pa "scan_ms"))
              (str "the battery hashes a wall-clock reading; receipt was "
                   (pr-str (:receipt (last a)))))

          ;; 2. The battery's own line, reproduced: one operation, one unchanged
          ;;    corpus, two reps, one hash.
          ;; `true?` rather than a bare `=` so a failure prints its named
          ;; receipts instead of dumping two whole scans into the report.
          (is (true? (= pa pb))
              (str "two scans of an unchanged tree hash differently; "
                   "A receipt " (pr-str (:receipt (last a)))
                   " B receipt " (pr-str (:receipt (last b)))))

          ;; 3. And the meter is NOT dark. MEM-005 exists because an unreported
          ;;    cost is one nobody notices regressing; deleting the field would
          ;;    satisfy 1 and 2 and lose the requirement.
          (is (number? (measured-scan-ms a))
              "the receipt publishes no measured scan cost at all")
          (is (pos? (or (measured-scan-ms a) 0))
              "the scan really ran and the clock really measured it")
          (is (= (get-in (last a) [:receipt :resources :bytes_scanned])
                 (get-in (last b) [:receipt :resources :bytes_scanned]))
              "the deterministic denominator is not deterministic")
          (is (str/includes? pa "bytes_scanned")
              "bytes_scanned is a deterministic resource fact and belongs IN
               the hashed channel; only the measured fields ride outside it"))
        (finally (delete-tree! root))))))

;; @spec MCP-OP-MEM-011
(deftest the-hashed-channel-drops-the-measured-fields-and-nothing-else
  (testing "the fix is a partition, not a smaller hash"
    (let [root (scratch-tree!)]
      (try
        (let [a (core/run-ls-tree {:dir root :format :edn})
              projected (battery-parity-subject a)]
          (is (= (count a) (count projected))
              "the projection dropped whole records, not measured fields")
          (is (true? (= (pr-str (vec (butlast a))) (pr-str (vec (butlast projected)))))
              "the projection changed a record; only the receipt may differ")
          (is (str/includes? (pr-str (last a)) "scan_ms")
              "the unprojected result no longer carries the meter at all"))
        (finally (delete-tree! root))))))

;; @spec MCP-OP-MEM-005
;; @spec MCP-OP-MEM-011
(deftest the-text-encoding-keeps-its-measured-field-on-a-labelled-line
  (testing "text carries resources only inside the refusal block, so that is
            the only shape in which its measured field can move"
    (let [root (scratch-tree-with-a-refusal!)]
      (try
        (let [projects ((requiring-resolve 'clj-surgeon.core/outline-all-files)
                        ((requiring-resolve 'clj-surgeon.core/discover-projects) root))
              text ((requiring-resolve 'clj-surgeon.core/format-ls-tree-text)
                    projects root)]
          (is (str/includes? text "parser_admission_refused")
              "the fixture did not actually produce a refusal, so this witness
               would pass without measuring anything")
          (is (str/includes? text "scan_ms")
              "the text receipt stopped charging the scan")
          (is (some #(str/starts-with? % "── measured (not hashed):")
                    (str/split-lines text))
              (str "the text receipt's measured field is not on a labelled "
                   "line; lines were "
                   (pr-str (filter #(str/includes? % "scan_ms")
                                   (str/split-lines text))))))
        (finally (delete-tree! root))))))
