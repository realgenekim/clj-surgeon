(ns ^{:lane :fast} clj-surgeon.telemetry-events-test
  "Witnesses for TELEMETRY-EVENTS-001. Each pins a property the ledger is
   worthless without: one intact JSON line per call, the fields a reader
   counts by, atomicity under concurrent appends, a bounded line, and a
   ledger failure that costs the call nothing but is still reported."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-telemetry :as telemetry]
   [clj-surgeon.ns-isolation :as iso]
   [clj-surgeon.telemetry-events :as events]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is use-fixtures]])
  (:import
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute PosixFilePermissions)))

(def required-fields
  "Every field a count-first reader needs from one line."
  [:ts :seat :pid :kind :tool :ok :error_type :wall_ms :mission_id])

(defn- temp-dir
  "@spec TEST-ISO-003 -- scratch under this namespace's own subdir."
  [prefix]
  (.toFile (Files/createTempDirectory
             (.toPath (iso/namespace-tmp-dir 'clj-surgeon.telemetry-events-test))
             prefix
             (into-array FileAttribute []))))

(defn- reset-drops! [f] (reset! events/dropped 0) (f) (reset! events/dropped 0))

(use-fixtures :each reset-drops!)

(defn- lines [file] (->> (slurp file) str/split-lines (remove str/blank?)))

(deftest one-call-appends-one-valid-json-line-with-every-field
  (let [file (io/file (temp-dir "events-one-line-") "events.jsonl")]
    (events/record! file {:kind "mcp-call" :tool "inspect_clojure" :ok true
                          :error_type nil :wall_ms 12.6 :mission_id "m-7"})
    (let [written (lines file)
          parsed (json/parse-string (first written) true)]
      (is (= 1 (count written)) "one call, one line")
      (is (every? #(contains? parsed %) required-fields)
          (str "missing: " (remove #(contains? parsed %) required-fields)))
      (is (= "mcp-call" (:kind parsed)))
      (is (= "inspect_clojure" (:tool parsed)))
      (is (true? (:ok parsed)))
      (is (nil? (:error_type parsed)))
      (is (= 13 (:wall_ms parsed)) "wall is a rounded long, not a float")
      (is (= "m-7" (:mission_id parsed)))
      (is (= (events/current-pid) (:pid parsed)))
      (is (string? (:seat parsed)))
      (is (str/ends-with? (first written) "}") "the line is complete"))))

(deftest a-refusal-line-carries-its-error-type-and-a-null-mission
  (let [file (io/file (temp-dir "events-refusal-") "events.jsonl")]
    (events/record! file {:kind "mcp-call" :tool "apply_clojure_changes"
                          :ok false :error_type "verification-incomplete"
                          :wall_ms 40})
    (let [parsed (json/parse-string (first (lines file)) true)]
      (is (false? (:ok parsed)))
      (is (= "verification-incomplete" (:error_type parsed)))
      (is (nil? (:mission_id parsed)) "absent mission is null, not omitted"))))

(deftest eight-threads-fifty-lines-each-produce-four-hundred-intact-lines
  (let [file (io/file (temp-dir "events-concurrent-") "events.jsonl")
        threads (mapv (fn [t]
                        (Thread.
                          ^Runnable
                          (fn []
                            (dotimes [i 50]
                              (events/record!
                                file {:kind "mcp-call"
                                      :tool (str "tool-" t "-" i)
                                      :ok true :wall_ms i})))))
                      (range 8))]
    (run! #(.start ^Thread %) threads)
    (run! #(.join ^Thread %) threads)
    (let [written (lines file)
          parsed (map #(json/parse-string % true) written)]
      (is (= 400 (count written)) "no line lost")
      (is (= 400 (count (filter map? parsed))) "no line interleaved or split")
      (is (= 400 (count (distinct (map :tool parsed))))
          "every writer's own lines survived intact"))))

(deftest oversize-free-text-is-truncated-and-says-so
  (let [file (io/file (temp-dir "events-oversize-") "events.jsonl")
        huge (apply str (repeat 5000 "x"))]
    (events/record! file {:kind "mcp-call" :tool "apply_clojure_changes"
                          :ok false :error_type huge :wall_ms 1})
    (let [line (first (lines file))
          parsed (json/parse-string line true)]
      (is (<= (count (.getBytes line "UTF-8")) events/line-limit)
          "the line stays inside the atomic-append budget")
      (is (= events/free-text-limit (count (:error_type parsed))))
      (is (true? (:error_type_truncated parsed)) "the truncation is declared"))))

(deftest an-unwritable-ledger-costs-the-call-nothing-and-is-counted
  (let [base (temp-dir "events-unwritable-")
        locked (doto (io/file base "locked") .mkdirs)
        file (io/file locked "events.jsonl")
        good (io/file base "events.jsonl")]
    (Files/setPosixFilePermissions (.toPath locked)
                                   (PosixFilePermissions/fromString "r-xr-xr-x"))
    (try
      (is (nil? (events/record! file {:kind "mcp-call" :tool "inspect_clojure"
                                      :ok true :wall_ms 3}))
          "a failed append returns nil rather than throwing")
      (is (= 1 @events/dropped) "the drop is counted in-process")
      (events/record! file {:kind "mcp-call" :tool "inspect_clojure"
                            :ok true :wall_ms 3})
      (is (= 2 @events/dropped))
      (let [reported (events/record! good {:kind "mcp-call" :tool "inspect_clojure"
                                           :ok true :wall_ms 3})]
        (is (= 2 (:telemetry_dropped reported))
            "the next line that lands reports what was lost")
        (is (= 2 (:telemetry_dropped
                   (json/parse-string (first (lines good)) true))))
        (is (zero? @events/dropped) "reported exactly once, then cleared"))
      (finally
        (Files/setPosixFilePermissions
          (.toPath locked) (PosixFilePermissions/fromString "rwx------"))))))

(deftest a-healthy-line-omits-the-drop-counter
  (let [file (io/file (temp-dir "events-clean-") "events.jsonl")]
    (events/record! file {:kind "mcp-call" :tool "inspect_clojure" :ok true})
    (is (not (contains? (json/parse-string (first (lines file)) true)
                        :telemetry_dropped))
        "zero drops is silence, so a present counter always means trouble")))

(deftest the-ledger-file-is-private
  (let [file (io/file (temp-dir "events-perms-") "nested" "events.jsonl")]
    (events/record! file {:kind "mcp-call" :tool "inspect_clojure" :ok true})
    (is (= "rw-------"
           (PosixFilePermissions/toString
             (Files/getPosixFilePermissions (.toPath file) (into-array java.nio.file.LinkOption [])))))
    (is (= "rwx------"
           (PosixFilePermissions/toString
             (Files/getPosixFilePermissions
               (.toPath (.getParentFile file)) (into-array java.nio.file.LinkOption [])))))))

(deftest a-tool-call-writes-the-ledger-even-with-per-server-telemetry-off
  (let [file (io/file (temp-dir "events-mode-off-") "events.jsonl")]
    (with-redefs [events/default-events-file (fn [] (.getAbsolutePath file))]
      (let [state (telemetry/start! {:mode :off})]
        (telemetry/record-inspect-call!
          state {:requests [{:operation "outline" :file "a.clj"}]}
          {:ok true :request_count 1 :file_count 1} {:total_ms 9})
        (is (.isFile file)
            "a server started with telemetry off still lands in the ledger")
        (let [parsed (json/parse-string (first (lines file)) true)]
          (is (= "inspect_clojure" (:tool parsed)))
          (is (true? (:ok parsed)))
          (is (= 9 (:wall_ms parsed))))))))

(deftest the-default-path-is-the-home-dotdir
  (is (str/ends-with? (events/default-events-file) "/.clj-surgeon/events.jsonl")
      "the ledger is a HOME dotdir, not one of the launcher-chosen state roots"))

(def cost-fields
  "The optional cost fields. A caller that has them passes them through; a
   caller that does not gets nulls -- never a zero, which would read as a free
   call in a ledger whose whole purpose is telling Gene what he is paying."
  [:prompt_tokens :completion_tokens :reasoning_tokens :cost_usd
   :provider :upstream])

(deftest cost-fields-round-trip-and-are-null-when-the-caller-has-none
  (let [file (io/file (temp-dir "events-cost-") "events.jsonl")]
    (events/record! file {:kind "typist-call" :tool "arm-F" :ok true
                          :wall_ms 1234 :mission_id "onesite"
                          :prompt_tokens 806 :completion_tokens 318
                          :reasoning_tokens 256 :cost_usd 5.21E-4
                          :provider "openrouter" :upstream "Cerebras"})
    (events/record! file {:kind "mcp-call" :tool "inspect_clojure" :ok true})
    (let [[priced unpriced] (map #(json/parse-string % true) (lines file))]
      (is (= 806 (:prompt_tokens priced)))
      (is (= 318 (:completion_tokens priced)))
      (is (= 256 (:reasoning_tokens priced)))
      (is (= 5.21E-4 (:cost_usd priced))
          "the USD survives serialization at its real magnitude, not rounded to a cent")
      (is (= "openrouter" (:provider priced)))
      (is (= "Cerebras" (:upstream priced)))
      (doseq [f cost-fields]
        (is (contains? unpriced f)
            (str f " is PRESENT on a line whose caller has no such number")))
      (is (every? nil? (map unpriced cost-fields))
          "an MCP tool call has no tokens and no cost: null, never zero")
      (doseq [f required-fields]
        (is (contains? priced f) (str f " still present on a priced line"))))))
