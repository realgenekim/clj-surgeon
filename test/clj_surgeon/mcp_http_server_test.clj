(ns clj-surgeon.mcp-http-server-test
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-http-server :as http-server]
   [clj-surgeon.mcp-inspect-tool :as inspect-tool]
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

(deftest http-protocol-exposes-two-tools-and-structured-read-evidence
  (let [project (temp-dir)
        source-file (io/file project "src/demo.clj")
        _created (.mkdirs (.getParentFile source-file))
        _written (spit source-file "(ns demo)\n(def answer 42)\n")
        running (http-server/start-http-server!
                  {:project-dir (.getPath project)
                   :port 0
                   :telemetry :off
                   :nrepl-port :none})]
    (try
      (let [client (HttpClient/newHttpClient)
            initialized
            (post-json client (:url running) nil
                       {:jsonrpc "2.0" :id 1 :method "initialize"
                        :params {:protocolVersion "2025-03-26"
                                 :capabilities {}
                                 :clientInfo {:name "inspect-protocol-test"
                                              :version "1"}}})
            session-id
            (-> initialized .headers (.firstValue "Mcp-Session-Id")
                (.orElse nil))
            _notification
            (post-json client (:url running) session-id
                       {:jsonrpc "2.0" :method "notifications/initialized"})
            listed (post-json client (:url running) session-id
                              {:jsonrpc "2.0" :id 2
                               :method "tools/list" :params {}})
            called (post-json client (:url running) session-id
                              {:jsonrpc "2.0" :id 3
                               :method "tools/call"
                               :params
                               {:name "inspect_clojure"
                                :arguments
                                {:requests
                                 [{:id "answer" :operation "forms"
                                   :file "src/demo.clj" :forms ["answer"]
                                   :expect {:forms 1}}]
                                 :expect {:requests 1 :files 1}}}})
            tools (get-in (sse-json listed) [:result :tools])
            result (:result (sse-json called))]
        (is (= 200 (.statusCode initialized)))
        (is (some? session-id))
        (is (= ["inspect_clojure" "apply_clojure_changes"]
               (mapv :name tools)))
        (is (= true (get-in tools [0 :annotations :readOnlyHint])))
        (is (= false (get-in tools [0 :annotations :destructiveHint])))
        (is (= true (get-in tools [0 :annotations :idempotentHint])))
        (is (= false (get-in tools [0 :annotations :openWorldHint])))
        (is (= false (get-in tools [0 :inputSchema :additionalProperties])))
        (is (= false (get-in tools [1 :inputSchema :additionalProperties])))
        (is (= #{:basis :decisions :verify :changes :expect}
               (set (keys (get-in tools [1 :inputSchema :properties])))))
        (is (= false (:isError result)))
        (is (str/starts-with? (get-in result [:content 0 :text])
                              "inspect_clojure\n"))
        (is (not (str/includes? (get-in result [:content 0 :text])
                                "(def answer")))
        (is (= "(def answer 42)"
               (get-in result [:structuredContent :results 0 :forms 0 :source])))
        (is (= true (get-in result [:structuredContent :read_complete]))))
      (finally
        (http-server/stop-http-server! running)
        (delete-tree! project)))))

(deftest one-http-session-prepares-decides-and-applies-one-proof-carrying-change
  (let [project (temp-dir)
        source-file (io/file project "src/demo.clj")
        receipt-dir (io/file project "receipts")
        _created (.mkdirs (.getParentFile source-file))
        _written (spit source-file "(ns demo)\n(defn shell []\n  [:body])\n")
        running (http-server/start-http-server!
                  {:project-dir (.getPath project)
                   :receipt-dir (.getPath receipt-dir)
                   :port 0
                   :telemetry :off
                   :nrepl-port :none
                   :semantic-resolver
                   (fn [_]
                     {:ok true
                      :definition {:file_path (.getCanonicalPath source-file)
                                   :line 2 :character 7 :name "shell"}
                      :references []})
                   :verify! (fn [_ profile _ files]
                              {:ok true :profile profile :files files})})]
    (try
      (let [client (HttpClient/newHttpClient)
            initialized
            (post-json client (:url running) nil
                       {:jsonrpc "2.0" :id 1 :method "initialize"
                        :params {:protocolVersion "2025-03-26"
                                 :capabilities {}
                                 :clientInfo {:name "change-buffer-protocol-test"
                                              :version "1"}}})
            session-id (-> initialized .headers (.firstValue "Mcp-Session-Id")
                           (.orElse nil))
            _notification
            (post-json client (:url running) session-id
                       {:jsonrpc "2.0" :method "notifications/initialized"})
            prepared
            (post-json client (:url running) session-id
                       {:jsonrpc "2.0" :id 2 :method "tools/call"
                        :params {:name "inspect_clojure"
                                 :arguments
                                 {:mode "prepare-change"
                                  :subject "demo/shell"
                                  :intent "Change the shell body class"}}})
            prepare-result (:result (sse-json prepared))
            evidence (:structuredContent prepare-result)
            site (first (:sites evidence))
            applied
            (post-json client (:url running) session-id
                       {:jsonrpc "2.0" :id 3 :method "tools/call"
                        :params {:name "apply_clojure_changes"
                                 :arguments
                                 {:basis (:basis evidence)
                                  :decisions
                                  [{:site (:id site)
                                    :replace "(defn shell []\n  [:body.page])"}]
                                  :verify "fast"}}})
            apply-result (:result (sse-json applied))]
        (is (= 200 (.statusCode initialized)))
        (is (false? (:isError prepare-result)))
        (is (str/starts-with? (get-in prepare-result [:content 0 :text])
                              "inspect_clojure prepare-change"))
        (is (= "(defn shell []\n  [:body])" (:source site)))
        (is (= "definition" (:role site)))
        (is (false? (:isError apply-result)))
        (is (= 1 (get-in apply-result [:structuredContent :match-count])))
        (is (= "fast" (get-in apply-result [:structuredContent :verification :profile])))
        (is (str/starts-with? (get-in apply-result [:content 0 :text]) "Applied 1"))
        (is (= "(ns demo)\n(defn shell []\n  [:body.page])\n"
               (slurp source-file)))
        (is (= 1 (count (filter #(.isFile %) (file-seq receipt-dir))))))
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
        original-inspect @#'inspect-tool/handle-inspect
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
        (is (true? (-> before sse-json :result :structuredContent
                       :verification_complete)))
        (is (.startsWith ^String (-> before sse-json :result :content first :text)
                         "Applied "))
        (is (= "(ns demo)\n\n(defn shell []\n  [:body.page])\n"
               (slurp source-file)))
        (is (= 1 (count (filter #(.isFile %) (file-seq receipt-dir)))))
        (with-open [connection (nrepl/connect :port (-> running :nrepl :port))]
          (let [client (nrepl/client connection 5000)
                code
                (str "(do "
                     "(alter-var-root "
                     "#'clj-surgeon.mcp-tool/handle-clj-change "
                     "(constantly (fn [_ _ callback] "
                     "(callback [\"HOT_RELOAD_OK\"] false "
                     "{:ok true :operation \"apply_clojure_changes\" "
                     ":hot true})))) "
                     "(alter-var-root "
                     "#'clj-surgeon.mcp-inspect-tool/handle-inspect "
                     "(constantly (fn [_ _ callback] "
                     "(callback [\"HOT_INSPECT_OK\"] false "
                     "{:ok true :operation \"inspect_clojure\" "
                     ":read_complete true :hot true})))))")
                replies (doall (nrepl/message client {:op "eval" :code code}))]
            (is (some #(contains? (set (:status %)) "done") replies))
            (is (not-any? :err replies))))
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
                 (-> running :jetty .getConnectors first .getLocalPort))))
        (let [after-inspect
              (post-json client (:url running) session-id
                         {:jsonrpc "2.0" :id 4 :method "tools/call"
                          :params {:name "inspect_clojure" :arguments {}}})]
          (is (= "HOT_INSPECT_OK"
                 (-> after-inspect sse-json :result :content first :text)))
          (is (= true
                 (-> after-inspect sse-json :result :structuredContent :hot)))
          (is (false? (-> after-inspect sse-json :result :isError)))))
      (finally
        (alter-var-root #'tool/handle-clj-change (constantly original))
        (alter-var-root #'inspect-tool/handle-inspect
                        (constantly original-inspect))
        (http-server/stop-http-server! running)
        (delete-tree! project)))))
