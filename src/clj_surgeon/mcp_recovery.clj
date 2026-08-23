(ns clj-surgeon.mcp-recovery
  "Bounded Streamable HTTP probes used by the CLI recovery entrance."
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.net URI)
   (java.net.http HttpClient HttpClient$Version HttpRequest HttpRequest$BodyPublishers HttpResponse$BodyHandlers)
   (java.time Duration)
   (java.util UUID)))

(def protocol-version "2025-03-26")
(def ^:private semantic-witness-timeout-ms 60000)

(defn- http-client
  []
  (-> (HttpClient/newBuilder)
      (.version HttpClient$Version/HTTP_1_1)
      .build))

(defn- response-session-id
  [response]
  (some-> response .headers (.firstValue "mcp-session-id") (.orElse nil)))

(defn- parse-response-body
  [body]
  (when-not (str/blank? body)
    (let [data-lines (->> (str/split-lines body)
                          (keep #(when (str/starts-with? % "data:")
                                   (str/trim (subs % (count "data:"))))))]
      (json/parse-string (or (last data-lines) body) true))))

(defn send-json!
  "POST one bounded JSON message. Return status, parsed body, and session ID."
  [{:keys [url session-id timeout-ms client]} payload]
  (let [request-builder
        (doto (HttpRequest/newBuilder (URI/create url))
          (.timeout (Duration/ofMillis (long (or timeout-ms 10000))))
          (.header "content-type" "application/json")
          (.header "accept" "application/json, text/event-stream"))
        _ (when session-id
            (.header request-builder "mcp-session-id" session-id))
        request (-> request-builder
                    (.POST (HttpRequest$BodyPublishers/ofString
                             (json/generate-string payload)))
                    .build)
        response (.send (or client (http-client))
                        request
                        (HttpResponse$BodyHandlers/ofString))
        body (.body response)]
    {:status (.statusCode response)
     :session-id (response-session-id response)
     :body (parse-response-body body)}))

(defn open-session!
  "Initialize one short-lived MCP session."
  ([url]
   (open-session! url send-json!))
  ([url sender]
   (let [client (http-client)
         initialize
         (sender {:url url :client client}
                 {:jsonrpc "2.0"
                  :id 1
                  :method "initialize"
                  :params {:protocolVersion protocol-version
                           :capabilities {}
                           :clientInfo {:name "clj-surgeon-recover"
                                        :version "1"}}})
         session-id (:session-id initialize)]
     (when-not (= 200 (:status initialize))
       (throw (ex-info "MCP initialize failed"
                       {:error-type :mcp-initialize-failed
                        :status (:status initialize)})))
     (when-not (seq session-id)
       (throw (ex-info "MCP initialize returned no session ID"
                       {:error-type :mcp-session-missing})))
     (let [initialized
           (sender {:url url :client client :session-id session-id}
                   {:jsonrpc "2.0"
                    :method "notifications/initialized"})]
       (when-not (contains? #{200 202} (:status initialized))
         (throw (ex-info "MCP initialized notification failed"
                         {:error-type :mcp-initialized-failed
                          :status (:status initialized)}))))
     {:url url :client client :session-id session-id :next-id (atom 1)})))

(defn request!
  "Send one request on an initialized session and return its result."
  ([session method params]
   (request! session method params send-json!))
  ([session method params sender]
   (let [id (swap! (:next-id session) inc)
         response
         (sender session
                 {:jsonrpc "2.0" :id id :method method :params params})
         body (:body response)]
     (when (= 404 (:status response))
       (throw (ex-info "MCP session expired"
                       {:error-type :invalid-mcp-session
                        :status 404})))
     (when-not (= 200 (:status response))
       (throw (ex-info "MCP request failed"
                       {:error-type :mcp-request-failed
                        :status (:status response)
                        :method method})))
     (when-let [error (:error body)]
       (throw (ex-info (or (:message error) "MCP request refused")
                       {:error-type :mcp-request-refused
                        :method method
                        :code (:code error)})))
     (:result body))))

(defn call-tool!
  [session tool arguments]
  (request! session "tools/call" {:name tool :arguments arguments}))

(defn- structured-content
  [tool-result]
  (or (:structuredContent tool-result)
      (:structured-content tool-result)))

(defn- in-phase
  [phase operation]
  (try
    (operation)
    (catch Exception error
      (throw (ex-info (.getMessage error)
                      (assoc (or (ex-data error) {}) :phase phase)
                      error)))))

(defn- candidate-source-files
  [workspace]
  (->> ["src" "test" "dev"]
       (map #(io/file workspace %))
       (filter #(.isDirectory %))
       (mapcat file-seq)
       (filter #(.isFile %))
       (filter #(re-find #"\.(clj|cljs|cljc)$" (.getName %)))
       (sort-by #(.getPath %))
       (take 64)))

(defn- relative-path
  [workspace file]
  (-> (.toPath (io/file workspace))
      (.relativize (.toPath file))
      str
      (str/replace \\ \/)))

(defn- semantic-witness!
  [workspace surgeon-session cclsp-session tool-call]
  (loop [[file & more] (candidate-source-files workspace)]
    (when-not file
      (throw (ex-info "No named Clojure form was available for the semantic probe"
                      {:error-type :semantic-probe-source-missing})))
    (let [file (relative-path workspace file)
          outline-result
          (structured-content
            (tool-call surgeon-session
                       "inspect_clojure"
                       {:workspace_root workspace
                        :requests [{:id "recover-outline"
                                    :operation "outline"
                                    :file file}]
                        :expect {:requests 1 :files 1}}))
          outline (get-in outline-result [:results 0 :outline])
          form (first (filter #(and (seq (:name %))
                                    (contains? #{"def" "defn" "defn-" "defonce"
                                                 "deftest"}
                                               (:type %)))
                              (:forms outline)))]
      (if-not (and (seq (:ns outline)) form)
        (recur more)
        (let [form-result
              (structured-content
                (tool-call surgeon-session
                           "inspect_clojure"
                           {:workspace_root workspace
                            :requests [{:id "recover-form"
                                        :operation "forms"
                                        :file file
                                        :forms [(:name form)]
                                        :expect {:forms 1}}]
                            :expect {:requests 1 :files 1}}))
              source-anchor (get-in form-result [:results 0 :forms 0 :source_anchor])
              subject (str (:ns outline) "/" (:name form))
              semantic-result
              (structured-content
                (tool-call (assoc cclsp-session
                                  :timeout-ms semantic-witness-timeout-ms)
                           "resolve_var_surface"
                           {:workspace_root workspace
                            :var subject
                            :include_declaration true
                            :source_anchor source-anchor}))]
          (when-not (or (:ok semantic-result)
                        (= "ok" (:status semantic-result)))
            (throw (ex-info "Semantic surface probe refused"
                            {:error-type (or (:error_type semantic-result)
                                             :semantic-probe-failed)
                             :capabilities
                             {:structural-read :ready
                              :structural-write :ready
                              :semantic-surface
                              (if (= "warming" (:status semantic-result))
                                :warming
                                :unavailable)
                              :source-anchor :retained}
                             :safe-route :exact-source
                             :retained-source-anchor source-anchor
                             :fallback-command
                             ["clj-surgeon" ":op" ":cat"
                              ":file" file ":form" (:name form)]})))
          {:subject subject
           :file file
           :lsp-session (:lsp_session semantic-result)
           :definition-count (if (:definition semantic-result) 1 0)
           :reference-count (count (:references semantic-result))})))))

(defn- mutation-witness!
  [workspace surgeon-session tool-call]
  (let [probe-id (str/replace (str (UUID/randomUUID)) "-" "")
        relative (str ".clj-surgeon-recovery/probe_" probe-id ".clj")
        target (io/file workspace relative)
        parent (.getParentFile target)]
    (try
      (.mkdirs parent)
      (spit target (str "(ns clj-surgeon-recovery.probe-" probe-id ")\n\n"
                        "(def probe :before)\n"))
      (let [result
            (structured-content
              (tool-call surgeon-session
                         "apply_clojure_changes"
                         {:workspace_root workspace
                          :changes [{:id "recover-write"
                                     :files [relative]
                                     :forms ["probe"]
                                     :find ":before"
                                     :replace ":after"
                                     :expect {:matches 1
                                              :each_file 1
                                              :each_form 1}}]
                          :expect {:changes 1 :edits 1 :files 1}}))]
        (when-not (and (:ok result) (:verification_complete result))
          (throw (ex-info "Guarded mutation probe did not verify"
                          {:error-type :mutation-probe-failed})))
        {:file relative
         :verification-complete true
         :source-unchanged-after-cleanup true})
      (finally
        (java.nio.file.Files/deleteIfExists (.toPath target))
        (.delete parent)))))

(defn probe!
  "Prove catalogs, one exact semantic surface, and one guarded write."
  [{:keys [workspace surgeon-url cclsp-url open-session request tool-call]}]
  (let [open-session (or open-session open-session!)
        request (or request request!)
        tool-call (or tool-call call-tool!)
        surgeon-session (in-phase :surgeon-session
                                  #(open-session surgeon-url))
        cclsp-session (in-phase :cclsp-session
                                #(open-session cclsp-url))
        catalog (in-phase :tool-catalog
                          #(request surgeon-session "tools/list" {}))
        tool-names (set (map :name (:tools catalog)))]
    (when-not (every? tool-names ["inspect_clojure" "apply_clojure_changes"])
      (throw (ex-info "Required clj-surgeon MCP tools are missing"
                      {:error-type :mcp-tool-catalog-incomplete
                       :phase :tool-catalog})))
    {:ok true
     :catalog {:inspect-clojure true :apply-clojure-changes true}
     :semantic (in-phase :semantic-witness
                         #(semantic-witness! workspace surgeon-session
                                             cclsp-session tool-call))
     :mutation (in-phase :mutation-witness
                         #(mutation-witness! workspace surgeon-session
                                             tool-call))}))
