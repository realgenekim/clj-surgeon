(ns clj-surgeon.mcp-http-server-test
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-http-server :as http-server]
   [clj-surgeon.mcp-tool :as tool]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [nrepl.core :as nrepl])
  (:import
   (java.net URI)
   (java.net.http HttpClient HttpRequest HttpRequest$BodyPublishers HttpResponse$BodyHandlers)
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute)))

(defn- temp-dir
  []
  (.toFile
    (Files/createTempDirectory
      "clj-surgeon-mcp-http-test-"
      (make-array FileAttribute 0))))

(defn- delete-tree!
  [file]
  (when (.exists (io/file file))
    (doseq [child (reverse (file-seq (io/file file)))]
      (Files/deleteIfExists (.toPath child)))))

(defn- post-json
  [^HttpClient client url session-id value]
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

(defn- sse-json
  [response]
  (->> (str/split-lines (.body response))
       (some #(when (str/starts-with? % "data: ")
                (subs % 6)))
       (#(json/parse-string % true))))

(deftest accepts-only-non-browser-or-loopback-origins
  (doseq [[origin expected]
          [[nil true]
           ["http://localhost" true]
           ["http://localhost:9999" true]
           ["https://127.0.0.1:443" true]
           ["http://[::1]:8080" true]
           ["https://evil.example" false]
           ["null" false]
           ["http://localhost.evil.example" false]]]
    (testing (str origin)
      (is (= expected (http-server/allowed-origin? origin))))))

(deftest starts-on-loopback-publishes-readiness-and-stays-hot
  (let [project (temp-dir)
        ready-file (io/file project "ready.edn")
        running (http-server/start-http-server!
                  {:project-dir (.getPath project)
                   :port 0
                   :telemetry :off
                   :nrepl-port :none
                   :ready-file (.getPath ready-file)})]
    (try
      (is (= "127.0.0.1" (:host running)))
      (is (pos? (:port running)))
      (is (= (str "http://127.0.0.1:" (:port running) "/mcp")
             (:url running)))
      (is (.exists ready-file))
      (is (= (:url running) (:url (edn/read-string (slurp ready-file)))))
      (let [client (HttpClient/newHttpClient)
            request (-> (HttpRequest/newBuilder)
                        (.uri (URI/create (str "http://127.0.0.1:"
                                               (:port running) "/healthz")))
                        (.GET)
                        (.build))
            first-response (.send client request (HttpResponse$BodyHandlers/ofString))
            second-response (.send client request (HttpResponse$BodyHandlers/ofString))]
        (is (= 200 (.statusCode first-response)))
        (is (= "{\"ok\":true,\"server\":\"clj-surgeon\"}"
               (.body first-response)))
        (is (= 200 (.statusCode second-response))))
      (finally
        (http-server/stop-http-server! running)
        (delete-tree! project)))))

(deftest one-http-session-observes-an-nrepl-handler-redefinition
  (let [project (temp-dir)
        port-file (io/file project ".nrepl-port")
        source-file (io/file project "src/demo.clj")
        receipt-dir (io/file project "receipts")
        _created-source-dir (.mkdirs (.getParentFile source-file))
        _wrote-source (spit source-file "(ns demo)\n\n(defn shell []\n  [:body])\n")
        original @#'tool/handle-clj-change
        running (http-server/start-http-server!
                  {:project-dir (.getPath project)
                   :receipt-dir (.getPath receipt-dir)
                   :port 0
                   :telemetry :off
                   :nrepl-port 0
                   :port-file (.getPath port-file)})]
    (try
      (let [client (HttpClient/newHttpClient)
            initialized
            (post-json client (:url running) nil
                       {:jsonrpc "2.0"
                        :id 1
                        :method "initialize"
                        :params {:protocolVersion "2025-03-26"
                                 :capabilities {}
                                 :clientInfo {:name "hot-reload-test"
                                              :version "1"}}})
            session-id
            (-> initialized .headers (.firstValue "Mcp-Session-Id") (.orElse nil))
            _ (post-json client (:url running) session-id
                         {:jsonrpc "2.0"
                          :method "notifications/initialized"})
            before (post-json client (:url running) session-id
                              {:jsonrpc "2.0"
                               :id 2
                               :method "tools/call"
                               :params {:name "apply_clojure_changes"
                                        :arguments
                                        {:changes
                                         [{:id "body-class"
                                           :files ["src/demo.clj"]
                                           :forms ["shell"]
                                           :find ":body"
                                           :replace ":body.page"
                                           :expect {:matches 1
                                                    :each_form 1
                                                    :each_file 1}}]
                                         :expect {:changes 1
                                                  :edits 1
                                                  :files 1}}}})]
        (is (= 200 (.statusCode initialized)))
        (is (some? session-id))
        (is (true? (-> before sse-json :result :content first :text
                       (json/parse-string true) :verification_complete)))
        (is (= "(ns demo)\n\n(defn shell []\n  [:body.page])\n"
               (slurp source-file)))
        (is (= 1 (count (filter #(.isFile %) (file-seq receipt-dir)))))
        (with-open [connection (nrepl/connect :port (-> running :nrepl :port))]
          (let [client (nrepl/client connection 5000)
                code
                (str "(alter-var-root "
                     "#'clj-surgeon.mcp-tool/handle-clj-change "
                     "(constantly (fn [_ _ callback] "
                     "(callback [\"HOT_RELOAD_OK\"] false))))")
                replies (doall (nrepl/message client {:op "eval" :code code}))]
            (is (some #(contains? (set (:status %)) "done") replies))))
        (let [after (post-json client (:url running) session-id
                               {:jsonrpc "2.0"
                                :id 3
                                :method "tools/call"
                                :params {:name "apply_clojure_changes"
                                         :arguments {}}})]
          (is (= "HOT_RELOAD_OK"
                 (-> after sse-json :result :content first :text)))
          (is (false? (-> after sse-json :result :isError)))
          (is (= (:port running)
                 (-> running :jetty .getConnectors first .getLocalPort)))))
      (finally
        (alter-var-root #'tool/handle-clj-change (constantly original))
        (http-server/stop-http-server! running)
        (delete-tree! project)))))
