(ns clj-surgeon.mcp-substantiation-w1-witness-test
  "Binding public-wire witness for prepared confirmation consumption telemetry."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-http-server :as http-server]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]])
  (:import
   (java.net URI)
   (java.net.http HttpClient HttpRequest HttpRequest$BodyPublishers HttpResponse$BodyHandlers)
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute)))

(defn- temp-directory [prefix]
  (.toFile
    (Files/createTempDirectory prefix (make-array FileAttribute 0))))

(defn- delete-tree! [file]
  (when (.exists (io/file file))
    (doseq [child (reverse (file-seq (io/file file)))]
      (Files/deleteIfExists (.toPath child)))))

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
    (is (= 200 (.statusCode response)))
    (json/parse-string payload true)))

(defn- tool-call [id name arguments]
  {:jsonrpc "2.0"
   :id id
   :method "tools/call"
   :params {:name name :arguments arguments}})

(defn- initialize! [client url]
  (let [initialized
        (post-json client url nil
                   {:jsonrpc "2.0" :id 1 :method "initialize"
                    :params {:protocolVersion "2025-03-26"
                             :capabilities {}
                             :clientInfo {:name "substantiation-w1-witness"
                                          :version "1"}}})
        session-id (-> initialized .headers (.firstValue "Mcp-Session-Id")
                       (.orElse nil))]
    (is (string? session-id))
    (post-json client url session-id
               {:jsonrpc "2.0" :method "notifications/initialized"})
    session-id))

(defn- ledger-events [directory]
  (if-not (.exists (io/file directory))
    []
    (->> (file-seq (io/file directory))
         (filter #(.isFile %))
         (mapcat #(str/split-lines (slurp %)))
         (remove str/blank?)
         (mapv #(json/parse-string % true)))))

(defn- feature-count [events feature-id stage]
  (count
    (for [event events
          feature (:features event)
          :when (and (= feature-id (:feature_id feature))
                     (= stage (:stage feature)))]
      feature)))

(deftest official-confirm-commit-counts-one-prepared-consumption
  ;; Frozen cross-feature witness required by the telemetry independent review.
  ;; It deliberately uses the real SDK HTTP JSON boundary, never direct Clojure
  ;; maps or raw-parameter feature recognition.
  (let [root (temp-directory "substantiation-w1-witness-")
        source-file (io/file root "src/demo.clj")
        receipt-dir (io/file root "receipts")
        ledger-dir (io/file root "substantiation")
        running (atom nil)]
    (try
      (.mkdirs (.getParentFile source-file))
      (spit source-file "(ns demo)\n(def alpha :old)\n")
      (reset! running
              (http-server/start-http-server!
                {:project-dir (.getCanonicalPath root)
                 :receipt-dir (.getCanonicalPath receipt-dir)
                 :substantiation-dir (.getCanonicalPath ledger-dir)
                 :telemetry :off
                 :nrepl-port :none
                 :port 0}))
      (let [client (HttpClient/newHttpClient)
            session-id (initialize! client (:url @running))
            inspected
            (-> (post-json
                  client (:url @running) session-id
                  (tool-call
                    2 "inspect_clojure"
                    {:requests [{:id "forms"
                                 :operation "forms"
                                 :file "src/demo.clj"
                                 :forms ["alpha"]
                                 :expect {:forms 1}}]
                     :expect {:requests 1 :files 1}}))
                response-json)
            digest (get-in inspected
                           [:result :structuredContent
                            :prepared_confirmation :descriptor_sha256])
            compact {:confirm digest
                     :fill {"arguments.edits[0].to" "(def alpha :new)"}}
            committed
            (-> (post-json client (:url @running) session-id
                           (tool-call 3 "edit_clojure" compact))
                response-json)
            replay
            (-> (post-json client (:url @running) session-id
                           (tool-call 4 "edit_clojure" compact))
                response-json)
            events (ledger-events ledger-dir)]
        (is (re-matches #"[0-9a-f]{64}" digest))
        (is (= true (get-in committed [:result :structuredContent :ok])))
        (is (= true (get-in committed [:result :structuredContent :committed])))
        (is (= "(ns demo)\n(def alpha :new)\n" (slurp source-file)))
        (is (= "prepared-confirmation-consumed"
               (get-in replay [:result :structuredContent :error_type])))
        (is (= 1 (feature-count events "prepared-request" "emitted")))
        (is (= 1 (feature-count events "prepared-request" "consumed")))
        (is (= 1 (feature-count events "prepared-request" "committed"))))
      (finally
        (when @running
          (http-server/stop-http-server! @running))
        (delete-tree! root)))))
