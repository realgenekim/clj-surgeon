;; @spec MCP-OP-ADMIT-138
;;
;; The `transaction-recovery-required` proof, as a battery target.
;;
;; This is the one enumerated refusal kind no single-threaded fixture can
;; produce, because it exists to report exactly the case a single thread
;; cannot create: a THIRD PARTY changed a file between the transaction's write
;; and its rollback, so the recovery refuses to overwrite bytes it did not
;; write. The fixture widens that window rather than racing for it -- src/b is
;; unwritable, so the failing write happens only after every file in src/a has
;; landed, and a watcher thread clobbers the first of them for the whole
;; duration of those writes.
;;
;; It lived in `clj-surgeon.admit-patch-test` through round four. It does not
;; belong there. It is a TIMING bound with a busy-spinning thread, and a
;; timing bound is a battery target, not a fast merge gate -- and it was
;; load-bearing for that suite's `:once` set-equality assertion, so a flake
;; would have reported "the enumeration claims kinds no fixture drives" and
;; taken the whole enumeration proof down for an unrelated reason.
;;
;; Deliberately NOT wired into `make test`, `make test-fast` or
;; `make mcp-test`. `make admit-transaction-recovery-battery` is its only
;; entry point.

(require '[clj-surgeon.mcp-admit-tool :as admit]
         '[clj-surgeon.admit-patch-test :as admit-test]
         '[clojure.java.io :as io]
         '[clojure.java.shell :as shell])

(import '(java.nio.file Files)
        '(java.nio.file.attribute FileAttribute))

(def arms [8 32 64])
(def attempts-per-arm 3)

(defn- temp-dir
  []
  (.toFile (Files/createTempDirectory
             "clj-surgeon-admit-recovery-battery"
             (make-array FileAttribute 0))))

(defn- delete-tree!
  [file]
  (when (.exists file)
    (doseq [child (reverse (file-seq file))]
      (.delete child))))

(defn- slurp-safe
  [file]
  (try (slurp file) (catch Exception _ nil)))

(defn- attempt
  "One widened race at `n` files. Returns the receipt."
  [n]
  (let [root (temp-dir)
        _ (@#'admit-test/write-sources!
            root (@#'admit-test/transaction-fixture-sources n))
        first-file (io/file root "src/a/f000.clj")
        original (slurp-safe first-file)
        stop? (atom false)
        watcher (Thread.
                  (fn []
                    (while (not @stop?)
                      (let [current (slurp-safe first-file)]
                        (when (and current (not= current original)
                                   (not= current ";; CLOBBERED\n"))
                          (try (spit first-file ";; CLOBBERED\n")
                               (catch Exception _ nil)))))))]
    (try
      (shell/sh "chmod" "555" (.getPath (io/file root "src/b")))
      (.start watcher)
      (let [receipt (admit/execute-request!
                      (@#'admit-test/stub-config root)
                      {:patch (@#'admit-test/transaction-fixture-patch n)
                       :mode "commit" :verify "focused"})]
        (reset! stop? true)
        (.join watcher 5000)
        receipt)
      (finally
        (reset! stop? true)
        (.join watcher 5000)
        (shell/sh "chmod" "755" (.getPath (io/file root "src/b")))
        (delete-tree! root)))))

;; @spec MCP-OP-ADMIT-138
(def observed-kinds
  "Every `:error-type` this run actually published, recorded as it happens.

  The exemption in the fast suite pointed at a SUBSTRING of this file: it
  checked that the script MENTIONS `transaction-recovery-required`, which a
  comment satisfies. What the exemption needs is a record of EXECUTION, so
  the battery writes one."
  (atom #{}))

;; @spec MCP-OP-ADMIT-138
(defn- write-receipt!
  [passed]
  (let [file (io/file "target/admit-transaction-recovery-battery-receipt.edn")]
    (io/make-parents file)
    (spit file
          (pr-str {:target "make admit-transaction-recovery-battery"
                   :script "test/admit_transaction_recovery_battery.clj"
                   :at (str (java.time.Instant/now))
                   :arms arms
                   :arms-passed passed
                   :kinds-published @observed-kinds}))
    (println (str "battery receipt · " (.getPath file) " · kinds "
                  (pr-str @observed-kinds)))))

(defn- run-arm
  [n]
  (loop [tries 1 last-kind nil]
    (let [started (System/nanoTime)
          receipt (attempt n)
          wall-ms (quot (- (System/nanoTime) started) 1000000)
          kind (:error-type receipt)
          ;; @spec MCP-OP-ADMIT-138
          _ (when kind (swap! observed-kinds conj kind))
          enumerated? (contains? admit/admit-refusal-kinds kind)
          hit? (and (false? (:ok receipt))
                    (= :transaction-recovery-required kind)
                    (not (true? (:source-unchanged receipt))))]
      (cond
        hit?
        (do (println (format (str "PASS n=%d attempts=%d kind=%s "
                                  "source-unchanged=%s enumerated=%s wall-ms=%d")
                             n tries (pr-str kind)
                             (pr-str (:source-unchanged receipt))
                             enumerated? wall-ms))
            true)

        (< tries attempts-per-arm)
        (recur (inc tries) kind)

        :else
        (do (println (format (str "FAIL n=%d attempts=%d kind=%s "
                                  "source-unchanged=%s enumerated=%s wall-ms=%d")
                             n tries (pr-str (or kind last-kind))
                             (pr-str (:source-unchanged receipt))
                             enumerated? wall-ms))
            false)))))

(let [results (mapv run-arm arms)
      passed (count (filter true? results))]
  (println (format "admit-transaction-recovery-battery: %d/%d arms passed"
                   passed (count arms)))
  ;; @spec MCP-OP-ADMIT-138
  (write-receipt! passed)
  (System/exit (if (= passed (count arms)) 0 1)))
