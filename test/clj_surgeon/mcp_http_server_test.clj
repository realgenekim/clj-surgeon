(ns clj-surgeon.mcp-http-server-test
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-http-server :as http-server]
   [clj-surgeon.mcp-inspect-tool :as inspect-tool]
   [clj-surgeon.mcp-server :as mcp-server]
   [clj-surgeon.mcp-tool :as tool]
   [clj-surgeon.structural-lens :as structural-lens]
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
  (let [body (.body response)
        payload (if (str/starts-with? body "{")
                  body
                  (->> (str/split-lines body)
                       (some #(when (str/starts-with? % "data: ")
                                (subs % 6)))))]
    (json/parse-string payload true)))

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

(deftest project-verification-profiles-are-closed-data
  ;; @spec MCP-OP-VERIFY-001
  ;; @spec MCP-OP-VERIFY-002
  (let [profiles {"fast" {:commands [["clj-kondo" "--lint" "{files}"]
                                     ["npx" "formatter" "{files}"]]
                          :hot {:port-file ".nrepl-port"
                                :reload ["app.core"]
                                :tests ["app.core-test/law"]
                                :timeout-ms 5000}
                          :cold {:command ["make" "test"]
                                 :timeout-ms 600000}}
                  "full" ["make" "test"]}]
    (is (= {:profiles profiles :source :project}
           (http-server/resolve-verification-profiles
             nil {:verification-profiles profiles})))
    (is (= :process
           (:source (http-server/resolve-verification-profiles
                      profiles {:verification-profiles {"other" ["false"]}}))))
    (let [built-in (http-server/resolve-verification-profiles nil {})]
      (is (= :built-in (:source built-in)))
      (is (= 1200000
             (get-in built-in [:profiles "full" :cold :timeout-ms]))
          "the built-in full gate must exceed this repository's measured ten-minute suite"))
    (doseq [invalid [{:verification-profiles {}}
                     {:verification-profiles {:fast ["make" "test"]}}
                     {:verification-profiles {"fast" "make test"}}
                     {:verification-profiles {"fast" {:commands []}}}
                     {:verification-profiles
                      {"fast" {:hot {:port-file ".nrepl-port"
                                     :reload [] :tests []
                                     :code "(+ 1 2)"}}}}
                     {:verification-profiles
                      {"fast" {:cold {:command "make test"
                                      :timeout-ms 600000}}}}
                     {:verification-profiles
                      {"fast" {:cold {:command ["make" "test"]
                                      :timeout-ms 600000
                                      :code "(+ 1 2)"}}}}
                     {:verification-profiles {"fast" [["make" 42]]}}]]
      (let [error (try
                    (http-server/resolve-verification-profiles nil invalid)
                    nil
                    (catch clojure.lang.ExceptionInfo error error))]
        (is (= :invalid-project-verification-profiles
               (:error-type (ex-data error))))))))

(deftest exact-verification-profile-is-project-owned-closed-data
  ;; @spec MCP-OP-VERIFY-001
  ;; @spec MCP-OP-VERIFY-002
  (let [profile {:acceptance :exact-exit
                 :timeout-ms 120000
                 :commands [["clj-kondo" "--lint" "src/app.clj"
                             "--fail-level" "error"]]}
        resolved (http-server/resolve-verification-profiles
                   nil {:verification-profiles {"exact" profile}})]
    (is (= :project (:source resolved)))
    (is (= profile (get-in resolved [:profiles "exact"])))
    (doseq [invalid [(dissoc profile :timeout-ms)
                     (assoc profile :timeout-ms 0)
                     (assoc profile :timeout-ms 120001)
                     (assoc profile :acceptance :diagnostic-delta)
                     (assoc profile :commands [["true"] ["true"]])
                     (assoc profile :commands [["clj-kondo" "{files}"]])
                     (assoc profile :hot {:port-file ".nrepl-port"})]]
      (let [error (try
                    (http-server/resolve-verification-profiles
                      nil {:verification-profiles {"exact" invalid}})
                    nil
                    (catch clojure.lang.ExceptionInfo error error))]
        (is (= :invalid-project-verification-profiles
               (:error-type (ex-data error)))
            (pr-str invalid))))))

(deftest project-formatter-is-closed-data-with-one-safe-default
  (is (= http-server/default-formatter
         (http-server/formatter-from-config {})))
  (is (= ["format" "--write" "{files}"]
         (http-server/formatter-from-config
           {:formatter ["format" "--write" "{files}"]})))
  (doseq [invalid [{:formatter []}
                   {:formatter "format {files}"}
                   {:formatter ["format" 42 "{files}"]}
                   {:formatter ["format" "--write"]}]]
    (let [error (try
                  (http-server/formatter-from-config invalid)
                  nil
                  (catch clojure.lang.ExceptionInfo error error))]
      (is (= :invalid-project-formatter
             (:error-type (ex-data error)))))))

(deftest workspace-verification-contexts-are-lazy-reloadable-and-isolated
  (let [root-a (temp-dir)
        root-b (temp-dir)
        config-a (io/file root-a ".clj-surgeon.edn")
        config-b (io/file root-b ".clj-surgeon.edn")]
    (try
      (spit config-a
            "{:verification-profiles {\"fast\" [\"verify-a\"]}}\n")
      (spit config-b
            "{:verification-profiles {\"fast\" [\"verify-b\"]}}\n")
      (let [context-a (http-server/workspace-context nil (.getPath root-a))
            context-b (http-server/workspace-context nil (.getPath root-b))
            profiles-a (:verification-profiles-fn context-a)
            profiles-b (:verification-profiles-fn context-b)]
        (is (= {"fast" ["verify-a"]} (profiles-a)))
        (is (= {"fast" ["verify-b"]} (profiles-b)))
        (spit config-a
              "{:verification-profiles {\"fast\" [\"verify-a-new\"]}}\n")
        (is (= {"fast" ["verify-a-new"]} (profiles-a)))
        (is (= {"fast" ["verify-b"]} (profiles-b))
            "changing one root cannot affect another cached context"))
      (finally
        (delete-tree! root-a)
        (delete-tree! root-b)))))

(deftest starts-on-loopback-publishes-readiness-and-stays-hot
  (let [project (temp-dir)
        ready-file (io/file project "ready.edn")
        _ (spit (io/file project ".clj-surgeon.edn")
                "{:verification-profiles {\"fast\" [\"true\"]}}")
        running (http-server/start-http-server!
                  {:project-dir (.getPath project)
                   :port 0
                   :telemetry :off
                   :nrepl-port :none
                   :ready-file (.getPath ready-file)})]
    (try
      (is (= "127.0.0.1" (:host running)))
      (is (pos? (:port running)))
      (is (= :project (:verification-profile-source running)))
      (is (= (str "http://127.0.0.1:" (:port running) "/mcp")
             (:url running)))
      (is (.exists ready-file))
      (is (= :project
             (:verification-profile-source
               (edn/read-string (slurp ready-file)))))
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
        (is (= {:ok true
                :server "clj-surgeon"
                :tool_runtime "ready"
                :tool_registry "ready"}
               (json/parse-string (.body first-response) true)))
        (is (= 200 (.statusCode second-response))))
      (finally
        (http-server/stop-http-server! running)
        (delete-tree! project)))))

(deftest http-protocol-exposes-four-tools-and-structured-read-evidence
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
        (is (= true
               (get-in (sse-json initialized)
                       [:result :capabilities :tools :listChanged])))
        (is (= ["inspect_clojure" "apply_clojure_changes" "edit_clojure"
                "transform_clojure"]
               (mapv :name tools)))
        (is (= true (get-in tools [0 :annotations :readOnlyHint])))
        (is (= false (get-in tools [0 :annotations :destructiveHint])))
        (is (= true (get-in tools [0 :annotations :idempotentHint])))
        (is (= false (get-in tools [0 :annotations :openWorldHint])))
        (is (= false (get-in tools [0 :inputSchema :additionalProperties])))
        (is (= false (get-in tools [1 :inputSchema :additionalProperties])))
        (is (= #{:basis :decisions :verify :changes :expect :edits :programs
                 :delete_owners :create_files :extraction :workspace_root
                 :symbol_migration :require_change}
               (set (keys (get-in tools [1 :inputSchema :properties])))))
        (is (str/includes?
              (get-in tools [1 :inputSchema :properties :verify :description])
              "cold job"))
        (is (str/includes?
              (get-in tools [1 :inputSchema :properties :verify :description])
              "hot laws roll back"))
        (is (= "^verify/.+"
               (get-in tools [0 :inputSchema :properties
                              :verification_job :pattern])))
        (is (= false (:isError result)))
        (is (str/starts-with? (get-in result [:content 0 :text])
                              "inspect_clojure\n"))
        ;; @spec MCP-OP-STUDY-041
        ;; O2 round 2: the wire text carries the rows the receipt carries.
        (is (str/includes? (get-in result [:content 0 :text])
                           "(def answer 42)"))
        (is (= "(def answer 42)"
               (get-in result [:structuredContent :results 0 :forms 0 :source])))
        (is (= true (get-in result [:structuredContent :read_complete]))))
      (finally
        (http-server/stop-http-server! running)
        (delete-tree! project)))))

(deftest one-http-session-observes-live-tool-add-replace-and-remove
  (let [project (temp-dir)
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
                                 :clientInfo {:name "live-contract-test"
                                              :version "1"}}})
            session-id
            (-> initialized .headers (.firstValue "Mcp-Session-Id")
                (.orElse nil))
            _initialized
            (post-json client (:url running) session-id
                       {:jsonrpc "2.0" :method "notifications/initialized"})
            original-tools (tool/all-tools)
            inspect-tool (first original-tools)
            changed-inspect (assoc inspect-tool
                                   :description "HOT_SCHEMA_DESCRIPTION")
            temporary-tool (assoc inspect-tool
                                  :name "temporary_probe"
                                  :description "Temporary live registry probe"
                                  :outcome-classes #{:read-success})
            added
            (with-redefs [tool/all-tools
                          (fn [] (into [changed-inspect]
                                       (concat (rest original-tools)
                                               [temporary-tool])))]
              (mcp-server/sync-tools!))
            listed-with-addition
            (post-json client (:url running) session-id
                       {:jsonrpc "2.0" :id 2 :method "tools/list" :params {}})
            restored
            (with-redefs [tool/all-tools (constantly original-tools)]
              (mcp-server/sync-tools!))
            listed-restored
            (post-json client (:url running) session-id
                       {:jsonrpc "2.0" :id 3 :method "tools/list" :params {}})
            added-tools (get-in (sse-json listed-with-addition)
                                [:result :tools])
            added-by-name (into {} (map (juxt :name identity)) added-tools)
            restored-tools (get-in (sse-json listed-restored)
                                   [:result :tools])
            restored-by-name
            (into {} (map (juxt :name identity)) restored-tools)]
        (is (= true
               (get-in (sse-json initialized)
                       [:result :capabilities :tools :listChanged])))
        (is (= {:ok true
                :status :synchronized
                :removed []
                :upserted ["inspect_clojure" "temporary_probe"]
                :tool-count 5
                :server-restart-required false
                :agent-session-restart :client-dependent}
               (select-keys
                 added
                 [:ok :status :removed :upserted :tool-count
                  :server-restart-required :agent-session-restart])))
        (is (re-matches #"[0-9a-f]{8}" (:before-contract-hash added)))
        (is (re-matches #"[0-9a-f]{8}" (:after-contract-hash added)))
        (is (not= (:before-contract-hash added)
                  (:after-contract-hash added)))
        (is (= #{"inspect_clojure"
                 "apply_clojure_changes"
                 "edit_clojure"
                 "transform_clojure"
                 "temporary_probe"}
               (set (map :name added-tools))))
        (is (= "HOT_SCHEMA_DESCRIPTION"
               (get-in added-by-name ["inspect_clojure" :description])))
        (is (= {:ok true
                :status :synchronized
                :removed ["temporary_probe"]
                :upserted ["inspect_clojure"]
                :tool-count 4
                :server-restart-required false
                :agent-session-restart :client-dependent}
               (select-keys
                 restored
                 [:ok :status :removed :upserted :tool-count
                  :server-restart-required :agent-session-restart])))
        (is (= (:after-contract-hash added)
               (:before-contract-hash restored)))
        (is (= (:before-contract-hash added)
               (:after-contract-hash restored)))
        (is (= #{"inspect_clojure" "apply_clojure_changes" "edit_clojure"
                 "transform_clojure"}
               (set (map :name restored-tools))))
        (is (= inspect-tool/tool-description
               (get-in restored-by-name ["inspect_clojure" :description]))))
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
                     (let [session "lsp-http-test-session"]
                       {:ok true
                        :version 2
                        :lsp_session session
                        :definition {:lsp_session session
                                     :file "src/demo.clj"
                                     :file_path (.getCanonicalPath source-file)
                                     :source_sha256 (structural-lens/source-hash (slurp source-file))
                                     :owner "shell"
                                     :range {:start {:line 1 :character 6}
                                             :end {:line 1 :character 11}}
                                     :line 2 :character 7
                                     :name "shell"}
                        :references []}))
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
            site (first (:decision-sites evidence))
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
                              "inspect_clojure · prepare-change"))
        (is (= "(defn shell []\n  [:body])" (:source site)))
        (is (= "definition" (:role site)))
        (is (false? (:isError apply-result)))
        (is (= 1 (get-in apply-result [:structuredContent :match-count])))
        (is (= "fast" (get-in apply-result [:structuredContent :verification :profile])))
        (is (str/starts-with? (get-in apply-result [:content 0 :text]) "apply_clojure_changes\n"))
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
        original @#'tool/handle-apply-clojure-changes
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
                         "apply_clojure_changes\n"))
        (is (= "(ns demo)\n\n(defn shell []\n  [:body.page])\n"
               (slurp source-file)))
        (is (= 1 (count (filter #(.isFile %) (file-seq receipt-dir)))))
        (with-open [connection (nrepl/connect :port (-> running :nrepl :port))]
          ;; The full suite can keep the shared JVM busy for longer than five
          ;; seconds. Wait for the terminal nREPL reply so that a timed-out eval
          ;; cannot apply these Var changes after the test has begun cleanup.
          (let [client (nrepl/client connection 30000)
                code
                (str "(do "
                     "(alter-var-root "
                     "#'clj-surgeon.mcp-tool/handle-apply-clojure-changes "
                     "(constantly (fn [_ _ callback] "
                     "(callback [\"HOT_RELOAD_OK\"] false "
                     "{:ok true :operation \"apply_clojure_changes\" "
                     ":elapsed_ms 0.0 :hot true})))) "
                     "(alter-var-root "
                     "#'clj-surgeon.mcp-inspect-tool/handle-inspect "
                     "(constantly (fn [_ _ callback] "
                     "(callback [\"HOT_INSPECT_OK\"] false "
                     "{:ok true :operation \"inspect_clojure\" "
                     ":elapsed_ms 0.0 :read_complete true :hot true})))))")
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
        ;; Stop the executor before restoring process-global Vars. Otherwise a
        ;; late nREPL eval can overwrite the restored roots and contaminate the
        ;; tests that follow this one.
        (http-server/stop-http-server! running)
        (alter-var-root #'tool/handle-apply-clojure-changes
                        (constantly original))
        (alter-var-root #'inspect-tool/handle-inspect
                        (constantly original-inspect))
        (delete-tree! project)))))
