(ns prove-installed-write-refusal-001
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str])
  (:import
   (java.net URI)
   (java.net.http HttpClient HttpRequest HttpRequest$BodyPublishers HttpResponse$BodyHandlers)
   (java.security MessageDigest)))

(def expected-success-sha256
  "b3fb1f3f997e57ba4d05d8791a62bd181b0ad634d0e25adfe4684369586b7f92")

(def forbidden-authority-keys
  #{:next_call :prepared_request :retry_template :replacement_text
    :selected_candidate :write_authority})

(defn- assert!
  [truth message data]
  (when-not truth
    (throw (ex-info message data))))

(defn- sha256-bytes
  [^bytes value]
  (let [digest (MessageDigest/getInstance "SHA-256")]
    (.update digest value)
    (format "%064x" (BigInteger. 1 (.digest digest)))))

(defn- sha256-text
  [value]
  (sha256-bytes (.getBytes (str value) "UTF-8")))

(defn- sha256-file
  [file]
  (sha256-bytes (java.nio.file.Files/readAllBytes (.toPath (io/file file)))))

(defn- utf8-bytes
  [value]
  (count (.getBytes (str value) "UTF-8")))

(defn- post-json
  [^HttpClient client url session-id value]
  (let [request-json (json/generate-string value)
        builder (-> (HttpRequest/newBuilder)
                    (.uri (URI/create url))
                    (.header "Content-Type" "application/json")
                    (.header "Accept" "application/json, text/event-stream"))
        _ (when session-id (.header builder "Mcp-Session-Id" session-id))
        request (-> builder
                    (.POST (HttpRequest$BodyPublishers/ofString request-json))
                    (.build))
        response (.send client request (HttpResponse$BodyHandlers/ofString))]
    {:request-json request-json
     :response response
     :response-body (.body response)}))

(defn- response-json
  [{:keys [response response-body]}]
  (let [payload (if (str/starts-with? response-body "{")
                  response-body
                  (->> (str/split-lines response-body)
                       (some #(when (str/starts-with? % "data: ")
                                (subs % 6)))))]
    (assert! (some? payload) "MCP response contained no JSON payload"
             {:status (.statusCode response) :body response-body})
    (json/parse-string payload true)))

(defn- structured-content
  [parsed]
  (or (get-in parsed [:result :structuredContent])
      (get-in parsed [:result :structured_content])))

(defn- owner-name
  [index]
  (format "owner-%03d" index))

(defn- source-text
  [namespace-name owner-count]
  (str "(ns " namespace-name ")\n\n"
       (str/join "\n" (map #(format "(defn %s [] :old)" (owner-name %))
                           (range owner-count)))
       "\n"))

(defn- prepare-workspace!
  [root relative-file namespace-name owner-count]
  (let [source-file (io/file root relative-file)
        source (source-text namespace-name owner-count)]
    (io/make-parents source-file)
    (spit source-file source)
    {:file source-file
     :source source
     :sha256 (sha256-text source)}))

(defn- edit-arguments
  [workspace relative-file owner-count expected-count id]
  {"workspace_root" (.getCanonicalPath (io/file workspace))
   "changes"
   [{"id" id
     "files" [relative-file]
     "forms" (mapv owner-name (range owner-count))
     "find" ":old"
     "replace" ":new"
     "expect" {"matches" expected-count}}]})

(defn- tool-call
  [id arguments]
  {:jsonrpc "2.0"
   :id id
   :method "tools/call"
   :params {:name "edit_clojure" :arguments arguments}})

(defn- retain-call!
  [result-root label exchange parsed]
  (spit (io/file result-root (str label ".request.json"))
        (:request-json exchange))
  (spit (io/file result-root (str label ".response.txt"))
        (:response-body exchange))
  (spit (io/file result-root (str label ".parsed.json"))
        (json/generate-string parsed {:pretty true})))

(defn- recursive-key-set
  [value]
  (cond
    (map? value) (into (set (keys value)) (mapcat recursive-key-set (vals value)))
    (sequential? value) (into #{} (mapcat recursive-key-set value))
    :else #{}))

(defn- assert-inert!
  [result]
  (let [keys-found (recursive-key-set result)
        continuation (get-in result [:write_refusal_evidence :candidate_continuation])]
    (assert! (empty? (disj (set/intersection forbidden-authority-keys keys-found)
                           :write_authority))
             "Refusal exposed forbidden retry or mutation authority"
             {:keys-found keys-found})
    (assert! (false? (get-in result [:write_refusal_evidence :authority]))
             "Refusal evidence authority was not false" {})
    (assert! (false? (get-in result [:write_refusal_evidence :write_authority]))
             "Refusal evidence write authority was not false" {})
    (when continuation
      (assert! (false? (:executable continuation))
               "Continuation was executable" {:continuation continuation})
      (assert! (false? (:authority continuation))
               "Continuation carried authority" {:continuation continuation})
      (assert! (false? (:write_authority continuation))
               "Continuation carried write authority" {:continuation continuation}))))

(defn- normalize-success
  [result]
  (array-map
    :workspace_root "$WORKSPACE"
    :committed (:committed result)
    :changes (:changes result)
    :verification_complete (:verification_complete result)
    :edits (:edits result)
    :read_back_hashes (:read_back_hashes result)
    :next_action (:next_action result)
    :files (:files result)
    :ok (:ok result)
    :operation (:operation result)))

(defn- process-pid
  [pid]
  (let [handle (java.lang.ProcessHandle/of (parse-long pid))]
    (when (.isPresent handle) (.pid (.get handle)))))

(defn -main
  [& [mcp-url pid result-root workspace-root]]
  (assert! (every? some? [mcp-url pid result-root workspace-root])
           "usage: prove_installed_write_refusal_001.clj MCP_URL PID RESULT_ROOT WORKSPACE_ROOT"
           {})
  (let [result-root (.getCanonicalFile (io/file result-root))
        workspace-root (.getCanonicalFile (io/file workspace-root))
        _ (.mkdirs result-root)
        _ (.mkdirs workspace-root)
        median-root (io/file workspace-root "median")
        boundary-root (io/file workspace-root "boundary")
        success-root (io/file workspace-root "success")
        median (prepare-workspace! median-root "src/median.clj" "fixture.median" 27)
        boundary (prepare-workspace! boundary-root "src/boundary.clj" "fixture.boundary" 129)
        success (prepare-workspace! success-root "src/success.clj" "fixture.success" 1)
        pid-before (process-pid pid)
        client (HttpClient/newHttpClient)
        initialize-request
        {:jsonrpc "2.0" :id 1 :method "initialize"
         :params {:protocolVersion "2025-03-26"
                  :capabilities {}
                  :clientInfo {:name "installed-write-refusal-proof" :version "1"}}}
        initialized (post-json client mcp-url nil initialize-request)
        session-id (-> initialized :response .headers
                       (.firstValue "Mcp-Session-Id") (.orElse nil))
        _ (assert! (some? session-id) "MCP initialize returned no session id" {})
        _ (post-json client mcp-url session-id
                     {:jsonrpc "2.0" :method "notifications/initialized"})
        median-exchange
        (post-json client mcp-url session-id
                   (tool-call 10 (edit-arguments median-root "src/median.clj" 27 28
                                                 "installed-median-refusal")))
        median-parsed (response-json median-exchange)
        median-result (structured-content median-parsed)
        boundary-exchange
        (post-json client mcp-url session-id
                   (tool-call 11 (edit-arguments boundary-root "src/boundary.clj" 129 130
                                                 "installed-boundary-refusal")))
        boundary-parsed (response-json boundary-exchange)
        boundary-result (structured-content boundary-parsed)
        success-exchange
        (post-json client mcp-url session-id
                   (tool-call 12 (edit-arguments success-root "src/success.clj" 1 1
                                                 "ordinary-success")))
        success-parsed (response-json success-exchange)
        success-result (structured-content success-parsed)
        normalized-success (normalize-success success-result)
        normalized-json (json/generate-string normalized-success)
        normalized-sha (sha256-text normalized-json)
        pid-after (process-pid pid)]
    (retain-call! result-root "10-median-refusal" median-exchange median-parsed)
    (retain-call! result-root "11-boundary-refusal" boundary-exchange boundary-parsed)
    (retain-call! result-root "12-ordinary-success" success-exchange success-parsed)
    (spit (io/file result-root "20-ordinary-success.normalized.json") normalized-json)
    (assert! (= pid-before (parse-long pid) pid-after)
             "Shared server PID changed during proof"
             {:expected pid :before pid-before :after pid-after})
    (doseq [[label result before]
            [["median" median-result median]
             ["boundary" boundary-result boundary]]]
      (assert! (= false (:ok result)) "Refusal unexpectedly succeeded" {:label label})
      (assert! (= "expect-count-mismatch" (:error_type result))
               "Wrong refusal type" {:label label :result result})
      (assert! (= true (:source_unchanged result))
               "Refusal did not prove unchanged source" {:label label})
      (assert! (= (:source before) (slurp (:file before)))
               "Refusal changed source bytes" {:label label})
      (assert-inert! result))
    (let [evidence (:write_refusal_evidence median-result)]
      (assert! (= [27 27 0 false]
                  [(:available_count evidence) (:returned_count evidence)
                   (:omitted_count evidence) (:truncated evidence)])
               "Median evidence was incomplete" {:evidence evidence})
      (assert! (= 27 (count (:items evidence)))
               "Median evidence did not return all 27 rows" {:evidence evidence})
      (assert! (nil? (:candidate_continuation evidence))
               "Complete evidence returned an unnecessary continuation" {:evidence evidence}))
    (let [evidence (:write_refusal_evidence boundary-result)
          continuation (:candidate_continuation evidence)]
      (assert! (= [129 128 1 true]
                  [(:available_count evidence) (:returned_count evidence)
                   (:omitted_count evidence) (:truncated evidence)])
               "Boundary evidence did not retain the exact bounded inventory"
               {:evidence evidence})
      (assert! (= 128 (count (:items evidence)))
               "Boundary evidence did not return 128 rows" {:evidence evidence})
      (assert! (= [128 1]
                  [(:next_offset continuation) (:remaining_count continuation)])
               "Boundary continuation did not identify the one omitted row"
               {:continuation continuation})
      (assert! (<= (utf8-bytes (json/generate-string boundary-result)) 32768)
               "Structured boundary result exceeded the framing budget"
               {:bytes (utf8-bytes (json/generate-string boundary-result))}))
    (assert! (= true (:ok success-result)) "Ordinary success failed" {:result success-result})
    (assert! (= true (:committed success-result)) "Ordinary success did not commit" {})
    (assert! (= true (:verification_complete success-result))
             "Ordinary success did not verify" {})
    (assert! (not (contains? success-result :write_refusal_evidence))
             "Success path leaked refusal evidence" {:result success-result})
    (assert! (= expected-success-sha256 normalized-sha)
             "Ordinary success changed modulo named dynamic exclusions"
             {:expected expected-success-sha256 :actual normalized-sha
              :normalized normalized-success})
    (assert! (= "1a6fd57dbbe2675354c4371031cc45c2c4f81e839c621e9bd4ec2c9dd96aac09"
                (sha256-file (:file success)))
             "Ordinary success produced unexpected source bytes" {})
    (let [report
          {:schema :clj-surgeon.write-refusal-001-installed-proof/v1
           :ok true
           :model-calls 0
           :mcp-url mcp-url
           :server-pid-before pid-before
           :server-pid-after pid-after
           :proofs
           {:median-complete
            {:available_count 27 :returned_count 27 :omitted_count 0
             :truncated false
             :structured_result_bytes
             (utf8-bytes (json/generate-string median-result))
             :response_sha256 (sha256-text (:response-body median-exchange))
             :source_before_sha256 (:sha256 median)
             :source_after_sha256 (sha256-file (:file median))}
            :boundary-bounded
            {:available_count 129 :returned_count 128 :omitted_count 1
             :truncated true
             :structured_result_bytes
             (utf8-bytes (json/generate-string boundary-result))
             :response_sha256 (sha256-text (:response-body boundary-exchange))
             :source_before_sha256 (:sha256 boundary)
             :source_after_sha256 (sha256-file (:file boundary))}
            :ordinary-success
            {:normalized_sha256 normalized-sha
             :expected_control_normalized_sha256 expected-success-sha256
             :dynamic_exclusions
             ["workspace_root" "receipt_hash" "undo_receipt" "timing/receipt metadata"]
             :response_sha256 (sha256-text (:response-body success-exchange))
             :source_before_sha256 (:sha256 success)
             :source_after_sha256 (sha256-file (:file success))}}}]
      (spit (io/file result-root "report.edn") (str (pr-str report) "\n"))
      (spit (io/file result-root "report.json")
            (str (json/generate-string report {:pretty true}) "\n"))
      (println (pr-str report)))))
