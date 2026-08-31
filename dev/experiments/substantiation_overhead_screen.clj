(ns substantiation-overhead-screen
  "Zero-model pre-install overhead screen for substantiation telemetry."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-http-server :as http-server]
   [clj-surgeon.mcp-substantiation :as substantiation]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.net URI)
   (java.net.http HttpClient HttpRequest HttpRequest$BodyPublishers HttpResponse$BodyHandlers)
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute)))

(def candidate-commit "4e2cf27b2226997508356ac5ecbdeaed18d8132c")
(def candidate-tree "4c7265c660ce6e698e0dbff45d4e697fe05994e2")
(def samples-per-live-arm 100)
(def samples-per-live-block 50)
(def warmup-calls 10)

(def demo-source
  "(ns demo)\n(def alpha :old)\n(def beta :old)\n")

(def fidelity-source
  (str "(ns dogfood.fidelity)\n\n"
       "(def configs\n"
       "  [^{:owner \"a\"} {:name :a, :timeout 100}\n"
       "   {:name :b, :timeout 100, :enabled? true}])\n"))

(defn- fail! [message data]
  (throw (ex-info message data)))

(defn- temp-directory [prefix]
  (.toFile
   (Files/createTempDirectory
    prefix (make-array FileAttribute 0))))

(defn- delete-tree! [file]
  (when (.exists (io/file file))
    (doseq [child (reverse (file-seq (io/file file)))]
      (Files/deleteIfExists (.toPath child)))))

(defn- percentile [values p]
  (let [ordered (vec (sort values))
        index (max 0 (dec (long (Math/ceil (* p (count ordered))))))]
    (nth ordered index)))

(defn- time-call [f]
  (let [started (System/nanoTime)
        value (f)
        finished (System/nanoTime)]
    {:value value
     :elapsed-ms (/ (- (double finished) (double started)) 1000000.0)}))

(defn- private-function [symbol]
  (or (some-> (ns-resolve 'clj-surgeon.mcp-substantiation symbol) deref)
      (fail! "Missing substantiation projector" {:symbol symbol})))

(defn- pure-screen []
  (let [request-shape (private-function 'request-shape)
        result-shape (private-function 'result-shape)
        state {:secret (.getBytes "pure-screen-secret" "UTF-8")}
        request {:requests [{:file "src/demo.clj" :forms ["alpha"]}]
                 :expect {:requests 1 :files 1}}
        result {:ok true
                :operation "inspect_clojure"
                :read_complete true
                :results [{:operation "forms"
                           :file "src/demo.clj"
                           :forms [{:name "alpha"}]}]
                :elapsed_ms 1.0}
        samples
        (mapv
         (fn [index]
           (:elapsed-ms
            (time-call
             (if (< index 5000)
               #(request-shape state request)
               #(result-shape state result)))))
         (range 10000))]
    {:samples samples
     :p95-ms (percentile samples 0.95)}))

(defn- append-screen []
  (let [directory (temp-directory "clj-surgeon-substantiation-append-")
        state (substantiation/start! {:directory directory
                                      :session-id "append-screen"})]
    (try
      (let [samples
            (vec
             (mapcat
              (fn [_]
                (let [started
                      (time-call
                       #(substantiation/begin-call!
                         state nil "inspect_clojure"
                         {:requests [{:file "src/demo.clj"
                                      :forms ["alpha"]}]}))
                      context (:value started)
                      finished
                      (time-call
                       #(substantiation/complete-call!
                         state context
                         {:ok true
                          :operation "inspect_clojure"
                          :read_complete true
                          :results []
                          :elapsed_ms 1.0}))]
                  [(:elapsed-ms started) (:elapsed-ms finished)]))
              (range 500)))
            lines (str/split-lines (slurp (:file state)))
            event-bytes (mapv #(alength (.getBytes ^String % "UTF-8")) lines)
            ledger-bytes (.length (io/file (:file state)))]
        {:samples samples
         :p50-ms (percentile samples 0.50)
         :p95-ms (percentile samples 0.95)
         :max-ms (apply max samples)
         :event-max-bytes (apply max event-bytes)
         :ledger-bytes ledger-bytes
         :ledger-bytes-per-completed-call (/ (double ledger-bytes) 500.0)
         :ledger-sha256 (substantiation/sha256 (slurp (:file state)))})
      (finally
        (substantiation/stop! state)
        (delete-tree! directory)))))

(defn- post-json [^HttpClient client url session-id value]
  (let [builder (-> (HttpRequest/newBuilder)
                    (.uri (URI/create url))
                    (.header "Content-Type" "application/json")
                    (.header "Accept" "application/json, text/event-stream"))
        _ (when session-id (.header builder "Mcp-Session-Id" session-id))
        request (-> builder
                    (.POST (HttpRequest$BodyPublishers/ofString
                            (json/generate-string value)))
                    (.build))]
    (.send client request (HttpResponse$BodyHandlers/ofString))))

(defn- response-json [response]
  (let [body (.body response)
        payload (if (str/starts-with? body "{")
                  body
                  (->> (str/split-lines body)
                       (some #(when (str/starts-with? % "data: ")
                                (subs % 6)))))]
    (when-not (= 200 (.statusCode response))
      (fail! "Loopback MCP request failed"
             {:status (.statusCode response) :body body}))
    (json/parse-string payload true)))

(defn- tool-call [id name arguments]
  {:jsonrpc "2.0"
   :id id
   :method "tools/call"
   :params {:name name :arguments arguments}})

(defn- create-fixture! [root]
  (let [demo (io/file root "src/demo.clj")
        fidelity (io/file root "src/dogfood/fidelity.clj")]
    (.mkdirs (.getParentFile demo))
    (.mkdirs (.getParentFile fidelity))
    (spit demo demo-source)
    (spit fidelity fidelity-source)
    {:demo demo :fidelity fidelity}))

(defn- parity-requests [root]
  (let [workspace (.getCanonicalPath (io/file root))]
    [[:eligible-prepared-read
      "inspect_clojure"
      {:requests [{:id "alpha" :operation "forms"
                   :file "src/demo.clj" :forms ["alpha"]
                   :expect {:forms 1}}]
       :expect {:requests 1 :files 1}}]
     [:operation-less-read
      "inspect_clojure"
      {:requests [{:file "src/demo.clj" :forms ["alpha"]
                   :expect {:forms 1}}]
       :expect {:requests 1 :files 1}}]
     [:mixed-id-refusal
      "inspect_clojure"
      {:requests [{:id "alpha" :operation "forms"
                   :file "src/demo.clj" :forms ["alpha"]}
                  {:operation "forms"
                   :file "src/demo.clj" :forms ["beta"]}]
       :expect {:requests 2 :files 1}}]
     [:complete-write-refusal
      "transform_clojure"
      {:workspace_root workspace
       :file "src/dogfood/fidelity.clj"
       :expression "(-> (form 'configs) (match :timeout) right (transform #(+ % 50)))"
       :expect {:matches 3 :max_changed_characters 9}}]
     [:ordinary-write-success
      "transform_clojure"
      {:workspace_root workspace
       :file "src/dogfood/fidelity.clj"
       :expression "(-> (form 'configs) (match :timeout) right (transform #(+ % 50)))"
       :expect {:matches 2 :max_changed_characters 6}
       :commit true}]
     [:transform-preview
      "transform_clojure"
      {:workspace_root workspace
       :file "src/dogfood/fidelity.clj"
       :expression "(-> (form 'configs) (match :timeout) right (transform #(+ % 50)))"
       :expect {:matches 2 :max_changed_characters 6}}]]))

(def dynamic-result-keys
  #{:elapsed_ms :inspection_elapsed_ms :receipt_hash :receipt-hash
    :result_character_count})

(defn- normalize-string [value root]
  (-> value
      (str/replace root "<WORKSPACE>")
      (str/replace #"[0-9]+(?:\.[0-9]+)? ms" "<ELAPSED>")))

(defn- normalize-public [value root]
  (cond
    (map? value)
    (into (sorted-map)
          (keep (fn [[key child]]
                  (when-not (contains? dynamic-result-keys key)
                    [(if (or (string? key) (keyword? key))
                       (normalize-string (str key) root)
                       key)
                     (if (= key :descriptor_sha256)
                       (if (and (string? child)
                                (re-matches #"[0-9a-f]{64}" child))
                         "<SESSION-BOUND-DIGEST>"
                         (fail! "Invalid prepared confirmation digest"
                                {:value child}))
                       (normalize-public child root))])))
          value)

    (vector? value) (mapv #(normalize-public % root) value)
    (string? value) (normalize-string value root)
    :else value))

(defn- initialize! [client url]
  (let [initialized
        (post-json client url nil
                   {:jsonrpc "2.0" :id 1 :method "initialize"
                    :params {:protocolVersion "2025-03-26"
                             :capabilities {}
                             :clientInfo {:name "src/private.clj"
                                          :version "private prose"}}})
        session-id (-> initialized .headers (.firstValue "Mcp-Session-Id")
                       (.orElse nil))]
    (when-not session-id
      (fail! "MCP initialize returned no session" {}))
    (post-json client url session-id
               {:jsonrpc "2.0" :method "notifications/initialized"})
    session-id))

(defn- timed-read! [client url session-id id]
  (time-call
   #(-> (post-json
         client url session-id
         (tool-call id "inspect_clojure"
                    {:requests [{:file "src/demo.clj" :forms ["alpha"]
                                 :expect {:forms 1}}]
                     :expect {:requests 1 :files 1}}))
        response-json)))

(defn- run-live-arm! [arm block samples]
  (let [root (temp-directory (str "clj-surgeon-substantiation-live-"
                                  (name arm) "-" block "-"))
        _ (create-fixture! root)
        substantiation-dir (io/file root "substantiation")
        running
        (http-server/start-http-server!
         (cond-> {:project-dir (.getCanonicalPath root)
                  :port 0
                  :telemetry :off
                  :nrepl-port :none}
           (= arm :on) (assoc :substantiation-dir substantiation-dir)))
        client (HttpClient/newHttpClient)]
    (try
      (let [session-id (initialize! client (:url running))
            _ (dotimes [index warmup-calls]
                (timed-read! client (:url running) session-id (+ 10 index)))
            timings
            (mapv (fn [index]
                    (:elapsed-ms
                     (timed-read! client (:url running) session-id
                                  (+ 100 index))))
                  (range samples))
            parity
            (when (zero? block)
              (into {}
                    (map-indexed
                     (fn [index [label tool arguments]]
                       (when (= label :ordinary-write-success)
                         (spit (io/file root "src/dogfood/fidelity.clj")
                               fidelity-source))
                       (let [response
                             (-> (post-json client (:url running) session-id
                                            (tool-call (+ 1000 index)
                                                       tool arguments))
                                 response-json)]
                         [label {:raw response
                                 :normalized
                                 (normalize-public response
                                                   (.getCanonicalPath root))}]))
                     (parity-requests root))))
            ledger
            (when (= arm :on)
              {:directory (.getCanonicalPath substantiation-dir)
               :files (mapv #(.getCanonicalPath %)
                            (filter #(.isFile %) (file-seq substantiation-dir)))})]
        {:arm arm :block block :timings timings :parity parity :ledger ledger})
      (finally
        (http-server/stop-http-server! running)
        (delete-tree! root)))))

(defn- live-screen []
  (let [runs [(run-live-arm! :off 0 samples-per-live-block)
              (run-live-arm! :on 0 samples-per-live-block)
              (run-live-arm! :on 1 samples-per-live-block)
              (run-live-arm! :off 1 samples-per-live-block)]
        by-arm (group-by :arm runs)
        off-samples (vec (mapcat :timings (get by-arm :off)))
        on-samples (vec (mapcat :timings (get by-arm :on)))
        parity-by-arm
        (into {} (map (fn [[arm rows]]
                        [arm (:parity (first (filter :parity rows)))])
                      by-arm))
        labels (set (keys (:off parity-by-arm)))
        parity-results
        (into (sorted-map)
              (map (fn [label]
                     [label (= (get-in parity-by-arm [:off label :normalized])
                               (get-in parity-by-arm [:on label :normalized]))]))
              labels)]
    {:runs runs
     :off-samples off-samples
     :on-samples on-samples
     :off-p50-ms (percentile off-samples 0.50)
     :on-p50-ms (percentile on-samples 0.50)
     :off-p95-ms (percentile off-samples 0.95)
     :on-p95-ms (percentile on-samples 0.95)
     :p50-delta-ms (- (percentile on-samples 0.50)
                      (percentile off-samples 0.50))
     :p95-delta-ms (- (percentile on-samples 0.95)
                      (percentile off-samples 0.95))
     :parity parity-by-arm
     :parity-results parity-results
     :semantic-parity (every? true? (vals parity-results))}))

(defn- write-tsv! [file header rows]
  (spit file
        (str (str/join "\t" header) "\n"
             (str/join "\n" (map #(str/join "\t" %) rows)) "\n")))

(defn- edn-string [value]
  (binding [*print-namespace-maps* false]
    (str (pr-str value) "\n")))

(defn- assert-result-root! [path]
  (let [target (io/file path)]
    (when (or (.isFile target)
              (and (.exists target) (seq (.listFiles target))))
      (fail! "Result directory must be absent or empty" {:path path}))
    (.mkdirs target)
    target))

(defn -main [& [result-path]]
  (when-not result-path
    (fail! "Usage: substantiation-overhead-screen RESULT_DIR" {}))
  (let [result-dir (assert-result-root! result-path)
        pure (pure-screen)
        append (append-screen)
        live (live-screen)
        measurement
        {:event-max-bytes (:event-max-bytes append)
         :projection-samples (count (:samples pure))
         :projection-p95-ms (:p95-ms pure)
         :append-samples (count (:samples append))
         :append-p50-ms (:p50-ms append)
         :append-p95-ms (:p95-ms append)
         :append-max-ms (:max-ms append)
         :live-samples-per-arm (count (:on-samples live))
         :live-p50-delta-ms (:p50-delta-ms live)
         :live-p95-delta-ms (:p95-delta-ms live)
         :semantic-parity (:semantic-parity live)
         :model-calls 0
         :network-calls 0}
        verdict (substantiation/measurement-verdict measurement)
        receipt
        {:schema "clj-surgeon.substantiation-overhead-screen.v1"
         :candidate-commit candidate-commit
         :candidate-tree candidate-tree
         :clock :client-loopback-roundtrip
         :external-network-calls 0
         :loopback-http-requests (+ 8
                                    (* 4 warmup-calls)
                                    (* 2 samples-per-live-arm)
                                    12)
         :loopback-tool-calls (+ (* 4 warmup-calls)
                                 (* 2 samples-per-live-arm)
                                 12)
         :pure (dissoc pure :samples)
         :append (dissoc append :samples)
         :live (dissoc live :runs :off-samples :on-samples)
         :measurement measurement
         :verdict verdict}]
    (write-tsv! (io/file result-dir "pure-samples.tsv")
                ["index" "elapsed_ms"]
                (map-indexed vector (:samples pure)))
    (write-tsv! (io/file result-dir "append-samples.tsv")
                ["index" "elapsed_ms"]
                (map-indexed vector (:samples append)))
    (write-tsv! (io/file result-dir "live-samples.tsv")
                ["arm" "block" "position" "elapsed_ms"]
                (mapcat (fn [{:keys [arm block timings]}]
                          (map-indexed #(vector (name arm) block %1 %2) timings))
                        (:runs live)))
    (spit (io/file result-dir "parity.edn")
          (edn-string (:parity live)))
    (spit (io/file result-dir "receipt.edn") (edn-string receipt))
    (print (edn-string receipt))
    (when-not (:ok verdict)
      (System/exit 1))))
