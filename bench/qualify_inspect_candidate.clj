(ns qualify-inspect-candidate
  ;; @spec OP-ALG-PERF-001
  (:require
   [babashka.process :as process]
   [cheshire.core :as json]
   [cli-mcp-transport-differential :as differential]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.net URI)
   (java.net.http HttpClient HttpRequest HttpRequest$BodyPublishers HttpResponse$BodyHandlers)
   (java.security MessageDigest)))

(def cli-spec
  {:reads [{:file "bench/summarize_clean_codex.clj"
            :forms ['numeric-fields 'boolean-fields 'summarize-group
                    'markdown 'self-test]}
           {:file "bench/rescore_clean_codex.clj"
            :forms ['rescore-row 'emit-table]}]
   :expect {:file-count 2 :form-count 7}})

(def mcp-arguments
  {:requests [{:id "summary-forms"
               :operation "forms"
               :file "bench/summarize_clean_codex.clj"
               :forms ["numeric-fields" "boolean-fields" "summarize-group"
                       "markdown" "self-test"]
               :expect {:forms 5}}
              {:id "rescore-forms"
               :operation "forms"
               :file "bench/rescore_clean_codex.clj"
               :forms ["rescore-row" "emit-table"]
               :expect {:forms 2}}]
   :expect {:requests 2 :files 2}})

(defn- assert!
  [truth message data]
  (when-not truth
    (throw (ex-info message data))))

(defn- sha256-file
  [path]
  (let [digest (MessageDigest/getInstance "SHA-256")]
    (with-open [input (io/input-stream path)]
      (let [buffer (byte-array 65536)]
        (loop []
          (let [read-count (.read input buffer)]
            (when (pos? read-count)
              (.update digest buffer 0 read-count)
              (recur))))))
    (format "%064x" (BigInteger. 1 (.digest digest)))))

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

(defn- response-json
  [response]
  (let [body (.body response)
        payload (if (str/starts-with? body "{")
                  body
                  (->> (str/split-lines body)
                       (some #(when (str/starts-with? % "data: ")
                                (subs % 6)))))]
    (assert! (some? payload) "MCP response contained no JSON payload"
             {:status (.statusCode response) :body body})
    (json/parse-string payload true)))

(defn- normalize-form
  [fallback-file form]
  {:file (or (:file form) fallback-file)
   :name (str (:name form))
   :line (:line form)
   :end-line (or (:end-line form) (:end_line form))
   :source (:source form)})

(defn- cli-facts
  [outcome]
  (mapv (fn [[file form]] (normalize-form file form))
        (mapcat (fn [{:keys [file forms]}]
                  (map (fn [form] [file form]) forms))
                (:files outcome))))

(defn- mcp-facts
  [outcome]
  (mapv (fn [[file form]] (normalize-form file form))
        (mapcat (fn [{:keys [file forms]}]
                  (map (fn [form] [file form]) forms))
                (:results outcome))))

(defn- test-counts
  [output]
  (when-let [[_ tests assertions]
             (re-find #"Ran ([0-9]+) tests containing ([0-9]+) assertions\."
                      output)]
    {:tests (parse-long tests) :assertions (parse-long assertions)}))

(defn- run-differential-tests
  [candidate-root]
  (let [source-root (str (io/file candidate-root "source"))
        classpath (str (io/file source-root "src") ":"
                       (io/file source-root "dev/experiments"))
        test-file (str (io/file source-root
                                "dev/experiments/cli_mcp_transport_differential_test.clj"))
        result @(process/process ["bb" "-cp" classpath test-file]
                                 {:dir source-root :out :string :err :string})
        counts (test-counts (str (:out result) (:err result)))]
    (assert! (zero? (:exit result)) "CLI/MCP differential test failed"
             (select-keys result [:exit :out :err]))
    (assert! (= {:tests 6 :assertions 37} counts)
             "CLI/MCP differential test count changed" {:counts counts})
    (assoc counts
           :exit (:exit result)
           :stdout-sha256 (differential/sha256 (:out result))
           :stderr-sha256 (differential/sha256 (:err result)))))

(defn- differential-evidence
  []
  (let [report (differential/report)
        strata (:strata report)]
    (assert! (:all-correct report) "Differential report was not correct" report)
    (assert! (:all-semantic-parity report)
             "Differential report lacked semantic parity" report)
    {:oracle-candidate-commit (:candidate-commit report)
     :canonical-report-sha256 (differential/data-hash report)
     :read-facts-sha256
     (get-in (nth strata 0) [:equivalence :cli-facts-hash])
     :compiled-intent-sha256
     (get-in (nth strata 1) [:cli :compiled-intent-hash])
     :selector-facts-sha256
     (get-in (nth strata 2) [:equivalence :cli-facts-hash])
     :selector-retry-facts-sha256
     (differential/data-hash
       (differential/normalized-read-forms
         (get-in (nth strata 2) [:cli :retry-outcome])))}))

(defn -main
  [& [candidate-root workspace mcp-url server-pid result-root]]
  (assert! (every? some? [candidate-root workspace mcp-url server-pid result-root])
           "usage: qualify_inspect_candidate.clj CANDIDATE WORKSPACE MCP_URL SERVER_PID RESULT"
           {})
  (let [candidate-root (.getCanonicalPath (io/file candidate-root))
        workspace (.getCanonicalPath (io/file workspace))
        result-root (.getCanonicalPath (io/file result-root))
        receipt-file (io/file candidate-root "candidate-receipt.edn")
        receipt (edn/read-string (slurp receipt-file))
        cli (io/file candidate-root (:cli-wrapper receipt))
        archive (io/file candidate-root (:archive receipt))
        request-text (str (pr-str cli-spec) "\n")
        cli-result @(process/process [(str cli) ":op" ":cat" ":spec-file" "-"]
                                     {:dir workspace :in request-text
                                      :out :string :err :string})
        _ (spit (io/file result-root "cli-request.edn") request-text)
        _ (spit (io/file result-root "cli-stdout.edn") (:out cli-result))
        _ (spit (io/file result-root "cli-stderr.txt") (:err cli-result))
        cli-outcome (when (zero? (:exit cli-result))
                      (edn/read-string (:out cli-result)))
        client (HttpClient/newHttpClient)
        initialize-request
        {:jsonrpc "2.0" :id 1 :method "initialize"
         :params {:protocolVersion "2025-03-26"
                  :capabilities {}
                  :clientInfo {:name "candidate-qualification" :version "1"}}}
        initialized (post-json client mcp-url nil initialize-request)
        session-id (-> initialized .headers (.firstValue "Mcp-Session-Id")
                       (.orElse nil))
        _ (post-json client mcp-url session-id
                     {:jsonrpc "2.0" :method "notifications/initialized"})
        listed-response (post-json client mcp-url session-id
                                   {:jsonrpc "2.0" :id 2
                                    :method "tools/list" :params {}})
        listed (response-json listed-response)
        call-request {:jsonrpc "2.0" :id 3 :method "tools/call"
                      :params {:name "inspect_clojure"
                               :arguments mcp-arguments}}
        called-response (post-json client mcp-url session-id call-request)
        called-body (.body called-response)
        _ (spit (io/file result-root "mcp-call-request.json")
                (str (json/generate-string call-request {:pretty true}) "\n"))
        _ (spit (io/file result-root "mcp-call-response.txt") called-body)
        called (response-json called-response)
        structured (or (get-in called [:result :structuredContent])
                       (get-in called [:result :structured_content]))
        actual-cli-facts (when cli-outcome (cli-facts cli-outcome))
        actual-mcp-facts (mcp-facts structured)
        report {:schema :clj-surgeon.inspect-candidate-qualification/v1
                :qualified true
                :model-calls 0
                :analyzer-launches 0
                :identity
                {:source-commit (:source-commit receipt)
                 :source-tree (:source-tree receipt)
                 :archive-sha256 (:archive-sha256 receipt)
                 :archive-sha256-observed (sha256-file archive)
                 :cli-wrapper-sha256 (:cli-wrapper-sha256 receipt)
                 :cli-wrapper-sha256-observed (sha256-file cli)
                 :candidate-receipt-sha256 (sha256-file receipt-file)
                 :mcp-launch-pid (parse-long server-pid)}
                :actual-read-parity
                {:cli-exit (:exit cli-result)
                 :cli-request-sha256 (differential/sha256 request-text)
                 :cli-result-sha256 (differential/data-hash cli-outcome)
                 :mcp-request-sha256 (differential/data-hash mcp-arguments)
                 :mcp-result-sha256 (differential/data-hash structured)
                 :mcp-read-complete (:read_complete structured)
                 :tool-catalog (mapv :name (get-in listed [:result :tools]))
                 :semantic-facts-equal (= actual-cli-facts actual-mcp-facts)
                 :cli-facts-sha256 (differential/data-hash actual-cli-facts)
                 :mcp-facts-sha256 (differential/data-hash actual-mcp-facts)
                 :form-count (count actual-cli-facts)}
                :differential-tests (run-differential-tests candidate-root)
                :differential (differential-evidence)}]
    (assert! (= :benchmark-candidate (:artifact receipt))
             "Candidate receipt has wrong artifact" receipt)
    (assert! (= (:archive-sha256 receipt) (sha256-file archive))
             "Candidate archive hash changed" (:identity report))
    (assert! (= (:cli-wrapper-sha256 receipt) (sha256-file cli))
             "Candidate CLI wrapper hash changed" (:identity report))
    (assert! (zero? (:exit cli-result)) "Candidate CLI read failed"
             (select-keys cli-result [:exit :out :err]))
    (assert! (= 200 (.statusCode initialized))
             "MCP initialize failed" {:status (.statusCode initialized)})
    (assert! (some? session-id) "MCP initialize returned no session" {})
    (assert! (= 200 (.statusCode called-response))
             "MCP tool call failed" {:status (.statusCode called-response)})
    (assert! (= true (:read_complete structured))
             "MCP read was not terminal" structured)
    (assert! (= 7 (count actual-cli-facts))
             "Actual CLI read returned wrong form count" {:facts actual-cli-facts})
    (assert! (= actual-cli-facts actual-mcp-facts)
             "Actual CLI and MCP reads differed"
             {:cli actual-cli-facts :mcp actual-mcp-facts})
    (spit (io/file result-root "qualification.edn")
          (str (pr-str report) "\n"))
    (prn report)))

(apply -main *command-line-args*)
