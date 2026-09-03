(ns clj-surgeon.scope-stream-test
  "Witnesses for the bounded streaming scope reader.

   Every ceiling witness has Sol's shape: exactly at the limit succeeds, and one
   unit past it refuses BEFORE the effect the limit bounds - which for this
   reader means before the planner callback ever sees the file."
  (:require
   [clj-surgeon.memory-battery :as battery]
   [clj-surgeon.scope-stream :as scope]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]])
  (:import
   (java.lang.ref WeakReference)
   (java.nio.file Files LinkOption Path Paths)))

(defn- temp-root
  [label]
  (let [dir (io/file (or (System/getenv "CLJ_SURGEON_MEMORY_TMP") "/home/forge/tmp")
                     (str "clj-surgeon-scope-" label "-" (System/currentTimeMillis)
                          "-" (long (rand 1000000))))]
    (.mkdirs dir)
    (.getCanonicalPath dir)))

(defn- delete-tree!
  [root]
  (let [file (io/file root)]
    (when (.exists file)
      (doseq [child (reverse (file-seq file))]
        (Files/deleteIfExists (.toPath child))))))

(defn- write-file!
  [root relative content]
  (let [target (io/file root relative)]
    (.mkdirs (.getParentFile target))
    (spit target content)
    target))

(defn- padded
  "Source of exactly `bytes` UTF-8 bytes."
  [bytes]
  (let [head "(ns padded)\n"
        body (apply str (repeat (- bytes (count head)) \x))]
    (str head body)))

(defn- seen-paths
  "A planner that records what it was handed and keeps nothing else."
  []
  (let [seen (atom [])]
    [seen (fn [entry] (swap! seen conj (:relative entry)) :planned)]))

;; ------------------------------------------------- MCP-OP-MEM-020 retention

;; @spec MCP-OP-MEM-020
(deftest the-reader-drops-each-source-when-its-callback-returns
  (testing "a weak reference to every source the planner saw is cleared after
            the walk: the reader holds no source it has finished with"
    (let [root (temp-root "retention")]
      (try
        (dotimes [i 6]
          (write-file! root (format "src/n%02d.clj" i) (padded 20000)))
        (let [refs (atom [])
              receipt (scope/stream-scope!
                        root
                        (fn [entry]
                          (swap! refs conj (WeakReference. (:source entry)))
                          nil)
                        {})]
          ;; The collection happens BEFORE the receipt is read, and the receipt
          ;; is read afterwards, so the local stays a GC root across it. Assert
          ;; on the receipt first and a leak hiding inside the receipt becomes
          ;; invisible: the JVM is free to collect a local after its last use,
          ;; and this witness passed a deliberate whole-scope leak until the
          ;; order was fixed.
          (System/gc)
          (Thread/sleep 100)
          (System/gc)
          (Thread/sleep 100)
          (is (every? #(nil? (.get ^WeakReference %)) @refs)
              "every source the planner saw has been collected")
          (is (:ok receipt))
          (is (= 6 (get-in receipt [:work :files-read])))
          (is (= 0 (get-in receipt [:work :receipt-records]))
              "the default receipt carries counters, never records"))
        (finally (delete-tree! root))))))

;; @spec MCP-OP-MEM-020
(deftest the-summary-receipt-does-not-grow-with-the-file-count
  (let [small (temp-root "summary-small")
        large (temp-root "summary-large")]
    (try
      (dotimes [i 4] (write-file! small (format "src/n%02d.clj" i) (padded 2000)))
      (dotimes [i 80] (write-file! large (format "src/n%02d.clj" i) (padded 2000)))
      (let [a (scope/stream-scope! small (constantly nil) {})
            b (scope/stream-scope! large (constantly nil) {})]
        (is (= (set (keys a)) (set (keys b))))
        (is (= 4 (get-in a [:work :files-read])))
        (is (= 80 (get-in b [:work :files-read])))
        (is (> 64 (- (count (pr-str b)) (count (pr-str a))))
            "twenty times the files adds only counter digits to the receipt"))
      (finally (delete-tree! small) (delete-tree! large)))))

;; ---------------------------------------------- MCP-OP-MEM-020 walk entries

;; @spec MCP-OP-MEM-020
(deftest the-walk-entry-ceiling-admits-exactly-its-limit-and-refuses-the-next
  (let [root (temp-root "walk-entries")]
    (try
      (dotimes [i 5] (write-file! root (format "src/n%02d.clj" i) (padded 200)))
      ;; a non-matching file and a pruned directory still cost walk entries
      (write-file! root "src/README.txt" "not a source file")
      (let [probe (scope/stream-scope! root (constantly nil) {})
            entries (get-in probe [:work :walk-entries])
            at-limit (scope/stream-scope! root (constantly nil)
                                          {:max-walk-entries entries})
            [seen plan] (seen-paths)
            past (scope/stream-scope! root plan {:max-walk-entries (dec entries)})]
        (is (= 8 entries)
            "every visited entry is counted: the root, src, five sources and one
             non-matching file, so an include glob cannot conceal the walk")
        (is (:ok at-limit) "exactly the observed entry count is admitted")
        (is (false? (:ok past)))
        (is (= :scope-walk-entries-exceeded (:error-type past)))
        (is (= (dec entries) (:max-entries past)))
        (is (some? (:next_call past)))
        (is (empty? @seen) "the planner never ran"))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-MEM-020
(deftest the-depth-bound-refuses-the-deep-path-instead-of-dropping-it
  (let [root (temp-root "depth")]
    (try
      (write-file! root "a/b/c/d/deep.clj" (padded 200))
      (write-file! root "shallow.clj" (padded 200))
      (let [at-limit (scope/stream-scope! root (constantly nil) {:max-depth 5})
            past (scope/stream-scope! root (constantly nil) {:max-depth 4})]
        (is (:ok at-limit))
        (is (= 2 (get-in at-limit [:work :files-read]))
            "at exactly the bound both files are read")
        (is (false? (:ok past)))
        (is (= :scope-too-deep (:error-type past)))
        (is (str/includes? (:path past) "deep.clj")
            "the refusal names the path, so no file silently leaves the count")
        (is (= 4 (:max-depth past))))
      (finally (delete-tree! root)))))

;; ------------------------------------------------- MCP-OP-MEM-020 byte caps

;; @spec MCP-OP-MEM-020
(deftest the-per-file-byte-ceiling-admits-exactly-its-limit-and-refuses-the-next
  (let [root (temp-root "file-bytes")]
    (try
      (write-file! root "src/big.clj" (padded 4096))
      (let [[seen-at plan-at] (seen-paths)
            at-limit (scope/stream-scope! root plan-at {:max-file-bytes 4096})
            [seen-past plan-past] (seen-paths)
            past (scope/stream-scope! root plan-past {:max-file-bytes 4095})]
        (is (:ok at-limit))
        (is (= ["src/big.clj"] @seen-at))
        (is (= 4096 (get-in at-limit [:work :largest-file-bytes])))
        (is (false? (:ok past)))
        (is (= :scope-source-too-large (:error-type past)))
        (is (= 4095 (:max-bytes past)))
        (is (empty? @seen-past)
            "the planner never saw the file the ceiling refused"))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-MEM-020
(deftest the-aggregate-byte-ceiling-admits-exactly-its-limit-and-refuses-the-next
  (let [root (temp-root "aggregate")]
    (try
      (dotimes [i 4] (write-file! root (format "src/n%02d.clj" i) (padded 1000)))
      (let [[seen-at plan-at] (seen-paths)
            at-limit (scope/stream-scope! root plan-at {:max-aggregate-bytes 4000})
            [seen-past plan-past] (seen-paths)
            past (scope/stream-scope! root plan-past {:max-aggregate-bytes 3999})]
        (is (:ok at-limit))
        (is (= 4000 (get-in at-limit [:work :source-bytes]))
            "the aggregate is counted from bytes actually read")
        (is (= 4 (count @seen-at)))
        (is (false? (:ok past)))
        (is (= :scope-aggregate-bytes-exceeded (:error-type past)))
        (is (= 3999 (:max-bytes past)))
        (is (= 3 (count @seen-past))
            "the first three files were planned; the fourth was refused before
             the planner saw it"))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-MEM-020
(deftest a-file-whose-parse-would-not-fit-the-work-budget-is-refused-before-parsing
  (let [root (temp-root "work-budget")]
    (try
      (write-file! root "src/dense.clj" (padded 10000))
      (let [[seen plan] (seen-paths)
            refusal (scope/stream-scope! root plan {:work-budget-bytes 100000
                                                    :parse-factor 56})]
        (is (false? (:ok refusal)))
        (is (= :scope-work-budget-exceeded (:error-type refusal)))
        (is (= 56 (:parse-factor refusal)))
        (is (empty? @seen) "one admitted file must fit the budget on its own"))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-MEM-020
(deftest the-accountant-reports-its-peak-reservation
  (let [root (temp-root "reserved")]
    (try
      (write-file! root "src/a.clj" (padded 1000))
      (write-file! root "src/b.clj" (padded 3000))
      (let [receipt (scope/stream-scope! root (constantly nil) {:parse-factor 56})
            path-list (get-in receipt [:reserved :path-list-bytes])]
        (is (:ok receipt))
        (is (pos? path-list))
        (is (= (+ path-list (* 3000 56))
               (get-in receipt [:reserved :heap-reserved-peak-bytes]))
            "reserved peak is the retained discovered-path list PLUS the largest
             admitted file times the measured parse factor, which is what a
             battery reads instead of UNMEASURED")
        (is (= 4000 (get-in receipt [:reserved :aggregate-bytes]))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-MEM-020
(deftest the-reservation-accounts-the-retained-discovered-path-list
  (testing "Sol's finding: the accountant charged only the largest parser
            reservation, while collect-entries retains and sorts EVERY matching
            path for the whole walk. With many small files that list is the
            larger of the two, and it was invisible."
    (let [root (temp-root "path-list")]
      (try
        (dotimes [i 200]
          (write-file! root (format "src/deeply/nested/name%03d.clj" i) (padded 100)))
        (let [receipt (scope/stream-scope! root (constantly nil) {:parse-factor 56})
              path-list (get-in receipt [:reserved :path-list-bytes])
              reserved (get-in receipt [:reserved :heap-reserved-peak-bytes])]
          (is (:ok receipt))
          (is (= 200 (get-in receipt [:work :files-read])))
          (is (> path-list (* 100 56))
              "with 200 small files the retained path list is the bigger term")
          (is (= (+ path-list (* 100 56)) reserved)
              "and the reservation now says so")
          (is (= 200 (get-in receipt [:reserved :discovered-files]))))
        (finally (delete-tree! root))))))

;; @spec MCP-OP-MEM-020
(deftest a-path-list-that-does-not-fit-the-work-budget-refuses-before-any-read
  (testing "accounting a cost the walk cannot pay is only half a fix: the
            discovered path list is admitted against the same work budget, and
            refused before the first source is read"
    (let [root (temp-root "path-list-budget")]
      (try
        (dotimes [i 50] (write-file! root (format "src/n%03d.clj" i) (padded 100)))
        (let [[seen plan] (seen-paths)
              probe (scope/stream-scope! root (constantly nil) {})
              path-list (get-in probe [:reserved :path-list-bytes])
              at-limit (scope/stream-scope! root (constantly nil)
                                            {:work-budget-bytes (+ path-list (* 100 56))
                                             :parse-factor 56})
              past (scope/stream-scope! root plan {:work-budget-bytes (dec path-list)
                                                   :parse-factor 56})]
          (is (:ok at-limit) "exactly the path list plus one file's parse is admitted")
          (is (false? (:ok past)))
          (is (= :scope-work-budget-exceeded (:error-type past)))
          (is (= :discovered-path-list (:reserved-for past))
              "the refusal names WHICH reservation did not fit")
          (is (= path-list (:path-list-bytes past)))
          (is (empty? @seen) "no source was read"))
        (finally (delete-tree! root))))))

;; @spec MCP-OP-MEM-001
;; @spec MCP-OP-MEM-011
(deftest the-battery-reads-this-receipts-reservation-block
  (testing "the shape binding. The battery's reserved-peak reader and the
            kernel's receipt are written in different namespaces by different
            builds; if they disagree the battery reports UNMEASURED for ever and
            nothing fails. This witness fails instead."
    (let [root (temp-root "battery-shape")]
      (try
        (write-file! root "src/a.clj" (padded 3000))
        (let [receipt (scope/stream-scope! root (constantly nil) {:parse-factor 56})
              observed (battery/reserved-peak-mb receipt)
              reserved (:reserved receipt)
              path-list (:path-list-bytes reserved)]
          (is (:ok receipt))
          (is (some? observed)
              "the battery must find an attributable reserved peak in this receipt")
          (is (pos? path-list)
              "the retained discovered-path list is charged; if that accounting
               were removed this is the assertion that notices")
          (is (= (+ path-list (* 3000 56)) (:heap-reserved-peak-bytes reserved))
              "the accountant's arithmetic, asserted at BYTE granularity - the
               old form asserted (* 3000 56) alone and passed only because
               both numbers round to the same tenth of a MiB")
          (is (= (battery/bytes->mb (+ path-list (* 3000 56))) observed)
              "and the battery reads that number, not the sampled peak"))
        (finally (delete-tree! root))))))

;; @spec MCP-OP-MEM-020
(deftest the-walk-refuses-a-matching-symbolic-link-and-never-follows-it
  (let [root (temp-root "symlink")
        outside (temp-root "symlink-outside")]
    (try
      (write-file! root "src/real.clj" (padded 200))
      (write-file! outside "secret.clj" (padded 200))
      (Files/createSymbolicLink
        (.toPath (io/file root "src" "link.clj"))
        (.toPath (io/file outside "secret.clj"))
        (make-array java.nio.file.attribute.FileAttribute 0))
      (let [[seen plan] (seen-paths)
            result (scope/stream-scope! root plan {})]
        (is (false? (:ok result)))
        (is (= :scope-symlink-refused (:error-type result)))
        (is (empty? @seen)))
      (finally (delete-tree! root) (delete-tree! outside)))))

;; @spec MCP-OP-MEM-020
(deftest skip-directories-are-pruned-rather-than-walked
  (let [root (temp-root "skip-dirs")]
    (try
      (write-file! root "src/keep.clj" (padded 200))
      (dotimes [i 20] (write-file! root (format "target/gen%02d.clj" i) (padded 200)))
      (let [[seen plan] (seen-paths)
            receipt (scope/stream-scope! root plan {})]
        (is (:ok receipt))
        (is (= ["src/keep.clj"] @seen))
        (is (> 6 (get-in receipt [:work :walk-entries]))
            "a pruned subtree costs one entry, not one per generated file"))
      (finally (delete-tree! root)))))

;; ------------------------------------------ MCP-OP-MEM-001 bounded receipt

;; @spec MCP-OP-MEM-001
(deftest the-receipt-record-ceiling-admits-exactly-its-limit-and-refuses-the-next
  (testing "a receipt is refused rather than truncated, so a small-constant
            per-file record cannot pass a memory gate by being small"
    (let [root (temp-root "receipt-records")]
      (try
        (dotimes [i 6] (write-file! root (format "src/n%02d.clj" i) (padded 200)))
        (let [collect (fn [entry _] {:path (:relative entry)})
              at-limit (scope/stream-scope! root (constantly nil)
                                            {:collect collect :max-receipt-records 6})
              past (scope/stream-scope! root (constantly nil)
                                        {:collect collect :max-receipt-records 5})]
          (is (:ok at-limit))
          (is (= 6 (count (:records at-limit))))
          (is (= 6 (get-in at-limit [:work :receipt-records])))
          (is (false? (:ok past)))
          (is (= :scope-receipt-too-large (:error-type past)))
          (is (= 5 (:max-records past)))
          (is (false? (:complete past))
              "an over-budget receipt is incomplete and says so"))
        (finally (delete-tree! root))))))

;; @spec MCP-OP-MEM-001
(deftest the-receipt-byte-ceiling-admits-exactly-its-limit-and-refuses-the-next
  (let [root (temp-root "receipt-bytes")]
    (try
      (dotimes [i 4] (write-file! root (format "src/n%02d.clj" i) (padded 200)))
      (let [collect (fn [entry _] {:path (:relative entry)})
            probe (scope/stream-scope! root (constantly nil) {:collect collect})
            exact (get-in probe [:work :receipt-bytes])
            at-limit (scope/stream-scope! root (constantly nil)
                                          {:collect collect :max-receipt-bytes exact})
            past (scope/stream-scope! root (constantly nil)
                                      {:collect collect :max-receipt-bytes (dec exact)})]
        (is (pos? exact))
        (is (:ok at-limit))
        (is (= exact (get-in at-limit [:work :receipt-bytes])))
        (is (false? (:ok past)))
        (is (= :scope-receipt-too-large (:error-type past)))
        (is (= (dec exact) (:max-bytes past))))
      (finally (delete-tree! root)))))
