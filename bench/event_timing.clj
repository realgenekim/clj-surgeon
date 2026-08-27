#!/usr/bin/env bb

(ns event-timing
  (:require
   [babashka.fs :as fs]
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.io BufferedOutputStream ByteArrayInputStream ByteArrayOutputStream StringWriter)
   (java.nio.charset StandardCharsets)
   (java.nio.file Files Paths)))

(def schema-version 1)

(defn- fail! [message data]
  (throw (ex-info message data)))

(defn- write-clock! [writer sequence-number monotonic-ns utc-ms byte-count]
  (.write writer
          (str sequence-number "\t" monotonic-ns "\t" utc-ms "\t"
               byte-count "\n"))
  (.flush writer))

(defn tap-stream!
  "Forward bytes unchanged while recording when each complete line is observed.
  clock-fn returns [monotonic-ns utc-ms] and is injectable for tests."
  [input output clock-writer clock-fn]
  (let [buffer (byte-array 8192)]
    (loop [sequence-number 0
           pending-byte-count 0]
      (let [read-count (.read input buffer)]
        (if (neg? read-count)
          (do
            (.flush output)
            (when (pos? pending-byte-count)
              (let [[monotonic-ns utc-ms] (clock-fn)]
                (write-clock! clock-writer (inc sequence-number)
                              monotonic-ns utc-ms pending-byte-count)))
            {:events (+ sequence-number (if (pos? pending-byte-count) 1 0))})
          (let [[next-sequence next-pending]
                (loop [index 0
                       segment-start 0
                       sequence-number sequence-number
                       pending-byte-count pending-byte-count]
                  (if (= index read-count)
                    (let [remaining (- read-count segment-start)]
                      (when (pos? remaining)
                        (.write output buffer segment-start remaining))
                      [sequence-number (+ pending-byte-count remaining)])
                    (if (= 10 (bit-and 0xff (aget buffer index)))
                      (let [segment-length (inc (- index segment-start))
                            line-byte-count (+ pending-byte-count segment-length)]
                        (.write output buffer segment-start segment-length)
                        (.flush output)
                        (let [[monotonic-ns utc-ms] (clock-fn)
                              next-sequence (inc sequence-number)]
                          (write-clock! clock-writer next-sequence monotonic-ns
                                        utc-ms line-byte-count)
                          (recur (inc index) (inc index) next-sequence 0)))
                      (recur (inc index) segment-start sequence-number
                             pending-byte-count))))]
            (recur next-sequence next-pending)))))))

(defn- read-record-bytes [path]
  (let [bytes (Files/readAllBytes (Paths/get path (make-array String 0)))
        length (alength bytes)]
    (loop [start 0
           index 0
           records []]
      (cond
        (= index length)
        (if (< start length)
          (conj records (java.util.Arrays/copyOfRange bytes start length))
          records)

        (= 10 (bit-and 0xff (aget bytes index)))
        (recur (inc index) (inc index)
               (conj records
                     (java.util.Arrays/copyOfRange bytes start (inc index))))

        :else
        (recur start (inc index) records)))))

(defn- record-json-string [record-bytes]
  (let [length (alength record-bytes)
        without-lf (if (and (pos? length)
                            (= 10 (bit-and 0xff (aget record-bytes (dec length)))))
                     (dec length)
                     length)
        without-cr (if (and (pos? without-lf)
                            (= 13 (bit-and 0xff
                                           (aget record-bytes (dec without-lf)))))
                     (dec without-lf)
                     without-lf)]
    (String. record-bytes 0 without-cr StandardCharsets/UTF_8)))

(defn- parse-long! [value field line]
  (try
    (Long/parseLong value)
    (catch Exception _
      (fail! "Invalid event clock integer"
             {:field field :value value :clock-line line}))))

(defn- read-clocks [path]
  (with-open [reader (io/reader path)]
    (mapv (fn [line]
            (let [parts (str/split line #"\t" -1)]
              (when-not (= 4 (count parts))
                (fail! "Invalid event clock row" {:clock-line line}))
              (let [[sequence-number monotonic-ns utc-ms byte-count] parts]
                {:sequence (parse-long! sequence-number :sequence line)
                 :observer-monotonic-ns
                 (parse-long! monotonic-ns :observer-monotonic-ns line)
                 :observer-utc-ms (parse-long! utc-ms :observer-utc-ms line)
                 :line-byte-count (parse-long! byte-count :line-byte-count line)})))
          (line-seq reader))))

(defn- event-kind [event]
  (let [event-type (:type event)
        item-type (some-> (get-in event [:item :type])
                          (str/replace "_" "-"))]
    (keyword
      (case event-type
        "thread.started" "thread-started"
        "turn.started" "turn-started"
        "turn.completed" "turn-completed"
        "item.started" (str (or item-type "unknown-item") "-started")
        "item.completed" (str (or item-type "unknown-item") "-completed")
        "item.updated" (str (or item-type "unknown-item") "-updated")
        "unknown-event"))))

(defn- server-elapsed-ms [event]
  (or (get-in event [:item :result :structured_content :elapsed_ms])
      (get-in event [:item :result :structuredContent :elapsed_ms])
      (get-in event [:item :result :elapsed_ms])))

(defn- allowlisted-observation [clock event]
  (let [item (:item event)]
    (cond-> (assoc clock
                   :event-kind (event-kind event)
                   :event-type (:type event))
      (:id item) (assoc :item-id (:id item))
      (:type item) (assoc :item-type (:type item))
      (:status item) (assoc :item-status (:status item))
      (:server item) (assoc :server (:server item))
      (:tool item) (assoc :tool (:tool item))
      (contains? item :exit_code) (assoc :exit-code (:exit_code item))
      (number? (server-elapsed-ms event))
      (assoc :server-authoritative-elapsed-ms (server-elapsed-ms event)))))

(def phase-by-transition
  {[:turn-started :agent-message-completed] :initial-decision-output
   [:turn-started :mcp-tool-call-started] :initial-tool-call-materialization
   [:agent-message-completed :mcp-tool-call-started]
   :mutation-call-materialization
   [:mcp-tool-call-completed :agent-message-completed]
   :mutation-receipt-interpretation
   [:agent-message-completed :command-execution-started]
   :verification-call-materialization
   [:command-execution-completed :agent-message-completed]
   :verification-result-interpretation
   [:agent-message-completed :turn-completed] :turn-finalization})

(defn- transitions [observations]
  (mapv (fn [left right]
          (let [from (:event-kind left)
                to (:event-kind right)]
            {:from-sequence (:sequence left)
             :to-sequence (:sequence right)
             :from from
             :to to
             :phase (get phase-by-transition [from to] :unclassified)
             :observer-elapsed-ms
             (/ (- (:observer-monotonic-ns right)
                   (:observer-monotonic-ns left))
                1000000.0)}))
        observations
        (rest observations)))

(defn- completed-span [started completed]
  (let [span {:item-id (:item-id completed)
              :item-type (:item-type completed)
              :server (:server completed)
              :tool (:tool completed)
              :start-sequence (:sequence started)
              :complete-sequence (:sequence completed)
              :observer-elapsed-ms
              (/ (- (:observer-monotonic-ns completed)
                    (:observer-monotonic-ns started))
                 1000000.0)}
        server-time (:server-authoritative-elapsed-ms completed)]
    (if (number? server-time)
      (assoc span :server-authoritative-elapsed-ms server-time)
      span)))

(defn- item-spans [observations]
  (let [{:keys [open spans]}
        (reduce
          (fn [{:keys [open spans] :as state} observation]
            (let [item-id (:item-id observation)
                  event-kind (:event-kind observation)]
              (cond
                (and item-id (str/ends-with? (name event-kind) "-started"))
                (assoc state :open (assoc open item-id observation))

                (and item-id (str/ends-with? (name event-kind) "-completed")
                     (contains? open item-id))
                {:open (dissoc open item-id)
                 :spans (conj spans
                              (completed-span (get open item-id) observation))}

                :else state)))
          {:open {} :spans []}
          observations)]
    {:complete spans
     :incomplete (mapv (fn [[item-id observation]]
                         {:item-id item-id
                          :item-type (:item-type observation)
                          :start-sequence (:sequence observation)})
                       (sort-by key open))}))

(defn summarize-data
  ([records clocks]
   (summarize-data records clocks nil))
  ([records clocks {:keys [process-start-utc-ms process-end-utc-ms]}]
   (when-not (= (count records) (count clocks))
     (fail! "Event and clock counts differ"
            {:event-count (count records) :clock-count (count clocks)}))
   (let [observations
         (mapv (fn [index record-bytes clock]
                 (let [expected-sequence (inc index)]
                   (when-not (= expected-sequence (:sequence clock))
                     (fail! "Event clock sequence is not contiguous"
                            {:expected expected-sequence
                             :actual (:sequence clock)}))
                   (when-not (= (alength record-bytes) (:line-byte-count clock))
                     (fail! "Event and clock byte counts differ"
                            {:sequence expected-sequence
                             :event-bytes (alength record-bytes)
                             :clock-bytes (:line-byte-count clock)}))
                   (let [event (try
                                 (json/parse-string
                                   (record-json-string record-bytes) true)
                                 (catch Exception exception
                                   (fail! "Invalid JSON event"
                                          {:sequence expected-sequence
                                           :cause (.getMessage exception)})))]
                     (allowlisted-observation clock event))))
               (range)
               records
               clocks)
         first-observation (first observations)
         last-observation (last observations)]
     (cond->
       {:schema :clj-surgeon.benchmark-event-timing/v1
        :schema-version schema-version
        :clock :observer-received-at-harness
        :event-count (count observations)
        :observer-window-ms
        (when (and first-observation last-observation)
          (/ (- (:observer-monotonic-ns last-observation)
                (:observer-monotonic-ns first-observation))
             1000000.0))
        :observations observations
        :transitions (transitions observations)
        :item-spans (item-spans observations)}
       (and process-start-utc-ms process-end-utc-ms)
       (assoc :process-wall-ms (- process-end-utc-ms process-start-utc-ms))

       (and process-start-utc-ms first-observation)
       (assoc :process-start-to-first-event-ms
              (- (:observer-utc-ms first-observation) process-start-utc-ms))

       (and process-end-utc-ms last-observation)
       (assoc :last-event-to-process-end-ms
              (- process-end-utc-ms (:observer-utc-ms last-observation)))))))

(defn summarize-files [events-path clock-path process-boundaries]
  (summarize-data (read-record-bytes events-path) (read-clocks clock-path)
                  process-boundaries))

(defn- check! [truthy message]
  (when-not truthy
    (fail! (str "Self-test failed: " message) {})))

(defn- throws? [f]
  (try
    (f)
    false
    (catch Exception _ true)))

(defn- clock-source [pairs]
  (let [remaining (atom pairs)]
    (fn []
      (let [pair (first @remaining)]
        (swap! remaining rest)
        pair))))

(defn self-test! []
  (let [raw (str
              "{\"type\":\"turn.started\",\"secret\":\"NEVER-RETAIN\"}\n"
              "{\"type\":\"item.completed\",\"item\":{\"id\":\"a\","
              "\"type\":\"agent_message\",\"text\":\"PRIVATE-TEXT\"}}\n"
              "{\"type\":\"item.started\",\"item\":{\"id\":\"m\","
              "\"type\":\"mcp_tool_call\",\"server\":\"clj-surgeon\","
              "\"tool\":\"apply_clojure_changes\",\"arguments\":{\"source\":"
              "\"PRIVATE-SOURCE\"}}}\n"
              "{\"type\":\"item.started\",\"item\":{\"id\":\"c\","
              "\"type\":\"command_execution\",\"command\":\"PRIVATE-COMMAND\"}}\n"
              "{\"type\":\"item.completed\",\"item\":{\"id\":\"m\","
              "\"type\":\"mcp_tool_call\",\"status\":\"completed\","
              "\"server\":\"clj-surgeon\",\"tool\":\"apply_clojure_changes\","
              "\"result\":{\"structured_content\":{\"elapsed_ms\":2500.0,"
              "\"source\":\"PRIVATE-RESULT\"}}}}\n"
              "{\"type\":\"turn.completed\"}")
        input (ByteArrayInputStream. (.getBytes raw StandardCharsets/UTF_8))
        output (ByteArrayOutputStream.)
        clock-writer (StringWriter.)
        clocks [[1000000000 1000] [2000000000 2000] [3000000000 3000]
                [3500000000 3500] [6000000000 6000] [7000000000 7000]]]
    (tap-stream! input output clock-writer (clock-source clocks))
    (check! (= raw (.toString output "UTF-8"))
            "tap must preserve raw bytes")
    (let [records (let [path (fs/create-temp-file {:prefix "event-timing-"
                                                   :suffix ".jsonl"})]
                    (try
                      (spit (str path) raw)
                      (read-record-bytes (str path))
                      (finally (fs/delete-if-exists path))))
          clock-rows (mapv (fn [line]
                             (let [[sequence monotonic utc bytes]
                                   (str/split line #"\t")]
                               {:sequence (parse-long! sequence :sequence line)
                                :observer-monotonic-ns
                                (parse-long! monotonic :monotonic line)
                                :observer-utc-ms (parse-long! utc :utc line)
                                :line-byte-count (parse-long! bytes :bytes line)}))
                           (str/split-lines (str clock-writer)))
          summary (summarize-data records clock-rows)
          printed (pr-str summary)]
      (check! (= 6 (:event-count summary)) "all events must be summarized")
      (check! (= 2500.0
                 (get-in summary [:item-spans :complete 0
                                  :server-authoritative-elapsed-ms]))
              "server clock must remain distinct")
      (check! (= [{:item-id "c" :item-type "command_execution"
                   :start-sequence 4}]
                 (get-in summary [:item-spans :incomplete]))
              "incomplete interleaved item must remain visible")
      (doseq [secret ["NEVER-RETAIN" "PRIVATE-TEXT" "PRIVATE-SOURCE"
                      "PRIVATE-COMMAND" "PRIVATE-RESULT"]]
        (check! (not (str/includes? printed secret))
                (str "summary leaked " secret)))
      (check! (throws? #(summarize-data records (pop clock-rows)))
              "count mismatch must fail")
      (let [bad-records [(byte-array (.getBytes "not json\n"
                                                StandardCharsets/UTF_8))]
            bad-clocks [{:sequence 1 :observer-monotonic-ns 1
                         :observer-utc-ms 1 :line-byte-count 9}]]
        (check! (throws? #(summarize-data bad-records bad-clocks))
                "invalid JSON must fail only during summary")))
    (println "benchmark event timing self-test passed")))

(defn usage! []
  (binding [*out* *err*]
    (println "Usage: bb bench/event_timing.clj tap CLOCK.tsv")
    (println (str "       bb bench/event_timing.clj summarize EVENTS.jsonl "
                  "CLOCK.tsv [PROCESS_START_UTC_MS PROCESS_END_UTC_MS]"))
    (println "       bb bench/event_timing.clj --self-test"))
  (System/exit 2))

(defn -main [& args]
  (case (first args)
    "tap"
    (let [[_ clock-path & extra] args]
      (when (or (nil? clock-path) (seq extra)) (usage!))
      (with-open [clock-writer (io/writer clock-path)]
        (tap-stream! System/in (BufferedOutputStream. System/out) clock-writer
                     (fn [] [(System/nanoTime) (System/currentTimeMillis)]))))

    "summarize"
    (let [[_ events-path clock-path process-start process-end & extra] args]
      (when (or (nil? events-path)
                (nil? clock-path)
                (seq extra)
                (not= (nil? process-start) (nil? process-end)))
        (usage!))
      (prn (summarize-files
             events-path clock-path
             (when process-start
               {:process-start-utc-ms
                (parse-long! process-start :process-start-utc-ms process-start)
                :process-end-utc-ms
                (parse-long! process-end :process-end-utc-ms process-end)}))))

    "--self-test" (self-test!)
    (usage!)))

(try
  (apply -main *command-line-args*)
  (catch Exception exception
    (binding [*out* *err*]
      (println "benchmark event timing failed:" (.getMessage exception))
      (when-let [data (ex-data exception)]
        (prn data)))
    (System/exit 1)))
