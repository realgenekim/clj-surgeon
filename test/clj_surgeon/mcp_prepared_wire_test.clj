(ns ^{:lane :battery} clj-surgeon.mcp-prepared-wire-test
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-prepared-confirmation :as prepared-confirmation]
   [clj-surgeon.tmp-leak-support :as tmp-leak]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing use-fixtures]])
  (:import
   (java.net URI)
   (java.net.http HttpClient HttpRequest HttpRequest$BodyPublishers HttpResponse$BodyHandlers)
   (java.util.concurrent TimeUnit)))

(def ^:private wire-timeout-ms
  "How long to wait for the CHILD LAUNCHER to become ready. This is a
   RENDEZVOUS bound, not a contract: the child is a cold `clojure -X` that
   resolves deps and loads a Clojure runtime, and round one measured one such
   launcher drive at 65 s at load 7 and ~4 min at load 12. Thirty seconds was
   an idle-box number; two-wide behind a CPU burner it timed out in BOTH
   copies (mcp_prepared_wire_test.clj:57 and :75, 2026-09-04).
   `CLJ_SURGEON_TEST_WIRE_TIMEOUT_MS` widens it further."
  (or (some-> (System/getenv "CLJ_SURGEON_TEST_WIRE_TIMEOUT_MS") parse-long)
      300000))

;; The workspace's deletion is owned by a FIXTURE, not by the tail of a
;; `finally` in each test body. Round one's leak was not that the test forgot
;; to clean up -- it cleaned up in a `finally` -- but that the cleanup was
;; SEQUENCED AFTER a call that could throw, so a teardown exception skipped it
;; and the tmp-leak ratchet failed the run a second time. A fixture cannot be
;; skipped by anything the body does, which makes the leak unrepresentable
;; rather than merely unlikely.
(def ^:private temp-roots (atom []))

(use-fixtures :each (tmp-leak/tracking-temp-dir-fixture temp-roots))
(def ^:private original-source "(ns demo)\n(def alpha :old)\n")
(def ^:private committed-source "(ns demo)\n(def alpha :new)\n")

(defn- temp-dir
  [prefix]
  (tmp-leak/track!
    temp-roots
    (.toFile
      (java.nio.file.Files/createTempDirectory
        prefix (make-array java.nio.file.attribute.FileAttribute 0)))))

(defn- delete-tree!
  [file]
  (when (.exists file)
    (doseq [child (reverse (file-seq file))]
      (.delete child))))

(defn- start-child!
  [command]
  (let [builder (doto (ProcessBuilder. ^java.util.List command)
                  (.directory (io/file (System/getProperty "user.dir"))))
        process (.start builder)]
    {:process process
     :stderr (future (slurp (.getErrorStream process)))}))

(defn- stop-child!
  "Tears the child down and returns a TYPED RECEIPT. NEVER THROWS.

   @spec TEST-ISO-RACE-001. `start-child!` slurps the child's stderr in a
   `future`; this function closes the child's streams and destroys it, and
   only then reads that future. `deref`'s default value covers a TIMEOUT, not
   an EXCEPTION -- so when the destroy wins the race against the still-running
   slurp, the future completes exceptionally and the deref RETHROWS. Round one
   saw exactly that in 2/2 concurrent pairs, and because this call is the
   FIRST step of the test's `finally`, the throw escaped before the workspace
   was deleted: one error plus one leak, from one race.

   A teardown step that can throw is not a teardown step. This one reports
   what went wrong as a typed, printed fact and returns."
  [{:keys [^Process process stderr]}]
  (let [process-refusal
        (try
          (when process
            (try (.close (.getOutputStream process)) (catch Exception _))
            (try (.close (.getInputStream process)) (catch Exception _))
            (.destroy process)
            (when-not (.waitFor process 5 TimeUnit/SECONDS)
              (.destroyForcibly process)
              (.waitFor process 5 TimeUnit/SECONDS)))
          nil
          (catch Throwable t
            {:refusal :process-teardown-failed
             :message (str (.getSimpleName (class t)) ": " (.getMessage t))}))
        stderr-result
        (when stderr
          (try
            {:stderr (deref stderr 5000 ::timed-out)}
            (catch Throwable t
              (let [cause (or (.getCause t) t)]
                {:refusal :stderr-reader-failed
                 :message (str (.getSimpleName (class cause)) ": "
                               (.getMessage cause))}))))
        receipt (merge {} process-refusal stderr-result)]
    (when-let [reason (:refusal receipt)]
      (binding [*out* *err*]
        (println (format "prepared-wire-teardown: %s -- %s"
                         (name reason) (:message receipt)))))
    receipt))

(defn- await!
  [predicate description]
  (let [deadline (+ (System/currentTimeMillis) wire-timeout-ms)]
    (loop []
      (cond
        (predicate) true
        (< (System/currentTimeMillis) deadline) (do (Thread/sleep 25) (recur))
        :else (throw (ex-info (str "Timed out waiting for " description)
                              {:timeout-ms wire-timeout-ms}))))))

(defn- write-json-line!
  [writer value]
  (.write writer (json/generate-string value))
  (.write writer "\n")
  (.flush writer))

(defn- read-json-response!
  [reader expected-id]
  (let [deadline (+ (System/currentTimeMillis) wire-timeout-ms)]
    (loop []
      (let [remaining (max 1 (- deadline (System/currentTimeMillis)))
            pending (future (.readLine reader))
            line (deref pending remaining ::timeout)]
        (when (= ::timeout line)
          (future-cancel pending)
          (throw (ex-info "Timed out waiting for stdio MCP response"
                          {:id expected-id :timeout-ms wire-timeout-ms})))
        (when-not line
          (throw (ex-info "Stdio MCP server closed stdout before responding"
                          {:id expected-id})))
        (let [response (json/parse-string line true)]
          (if (= expected-id (:id response))
            response
            (recur)))))))

(defn- tool-request
  [id tool arguments]
  {:jsonrpc "2.0"
   :id id
   :method "tools/call"
   :params {:name tool :arguments arguments}})

(defn- inspect-arguments
  []
  {:requests [{:id "forms"
               :operation "forms"
               :file "src/demo.clj"
               :forms ["alpha"]
               :expect {:forms 1}}]
   :expect {:requests 1 :files 1}})

(defn- exercise-prepared-wire!
  [transport call! source-file]
  (let [inspected (call! "inspect_clojure" (inspect-arguments))
        inspect-result (:result inspected)
        confirmation (get-in inspect-result
                             [:structuredContent :prepared_confirmation])
        digest (:descriptor_sha256 confirmation)
        fill {"arguments.edits[0].to" "(def alpha :new)"}
        compact {:confirm digest :fill fill}
        after-inspect (slurp source-file)
        preview (call! "edit_clojure" (assoc compact :preview true))
        after-preview (slurp source-file)
        committed (call! "edit_clojure" compact)
        after-commit (slurp source-file)
        replay (call! "edit_clojure" compact)
        after-replay (slurp source-file)]
    (testing (str transport " serves one prepared digest over the real wire")
      (is (false? (:isError inspect-result)))
      (is (re-matches #"[0-9a-f]{64}" digest)))
    (testing (str transport " previews through the official edit_clojure route")
      (is (= "edit_clojure-preview"
             (get-in preview [:result :structuredContent :operation])))
      (is (= original-source after-inspect after-preview)))
    (testing (str transport " commits once and rejects replay")
      (is (= true (get-in committed [:result :structuredContent :ok])))
      (is (= committed-source after-commit))
      (is (= "prepared-confirmation-consumed"
             (get-in replay [:result :structuredContent :error_type])))
      (is (= committed-source after-replay)))))

(defn- initialize-request
  []
  {:jsonrpc "2.0"
   :id 1
   :method "initialize"
   :params {:protocolVersion "2025-03-26"
            :capabilities {}
            :clientInfo {:name "prepared-wire-regression" :version "1"}}})

;; @spec MCP-OP-PREP-ACT-004
;; @spec MCP-OP-PREP-ACT-005
;; @spec MCP-OP-PREP-ACT-006
;; @spec MCP-OP-PREP-ACT-009
(deftest prepared-confirm-preview-commit-and-replay-cross-the-real-stdio-wire
  ;; Field-failure regression for SURGEON2 probe 9541fd00: the MCP SDK supplies
  ;; java.util.Map at both the request and nested fill boundaries.
  (let [project (temp-dir "prepared-wire-stdio-")
        source-file (io/file project "src/demo.clj")
        receipt-dir (io/file project "receipts")
        _created (.mkdirs (.getParentFile source-file))
        _written (spit source-file original-source)
        child (start-child!
                ["clojure" "-X:clj-surgeon/mcp-stdio"
                 ":project-dir" (pr-str (.getPath project))
                 ":receipt-dir" (pr-str (.getPath receipt-dir))
                 ":nrepl-port" ":none"
                 ":telemetry" ":off"])]
    (try
      (with-open [writer (io/writer (.getOutputStream ^Process (:process child)))
                  reader (io/reader (.getInputStream ^Process (:process child)))]
        (write-json-line! writer (initialize-request))
        (let [initialized (read-json-response! reader 1)
              next-id (atom 1)
              call! (fn [tool arguments]
                      (let [id (swap! next-id inc)]
                        (write-json-line! writer (tool-request id tool arguments))
                        (read-json-response! reader id)))]
          (is (= "clj-surgeon"
                 (get-in initialized [:result :serverInfo :name])))
          (write-json-line! writer
                            {:jsonrpc "2.0"
                             :method "notifications/initialized"})
          (exercise-prepared-wire! "stdio" call! source-file)))
      ;; Both steps run, in order, independently of the other's success:
      ;; `stop-child!` cannot throw, and the workspace's deletion is owned by
      ;; the :each fixture besides.
      (finally
        (stop-child! child)
        (delete-tree! project)))))

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
    (json/parse-string payload true)))

;; @spec MCP-OP-PREP-ACT-004
;; @spec MCP-OP-PREP-ACT-005
;; @spec MCP-OP-PREP-ACT-006
;; @spec MCP-OP-PREP-ACT-009
(deftest prepared-confirm-preview-commit-and-replay-cross-the-real-http-wire
  ;; The HTTP process is separate from the test JVM so its SDK session table,
  ;; confirmation registry, project root, and transport lifecycle are fresh.
  (let [project (temp-dir "prepared-wire-http-")
        source-file (io/file project "src/demo.clj")
        receipt-dir (io/file project "receipts")
        ready-file (io/file project "http-ready.edn")
        _created (.mkdirs (.getParentFile source-file))
        _written (spit source-file original-source)
        child (start-child!
                ["clojure" "-X:clj-surgeon/mcp"
                 ":project-dir" (pr-str (.getPath project))
                 ":receipt-dir" (pr-str (.getPath receipt-dir))
                 ":port" "0"
                 ":ready-file" (pr-str (.getPath ready-file))
                 ":nrepl-port" ":none"
                 ":telemetry" ":off"])]
    (try
      (await! #(.isFile ready-file) "Streamable HTTP readiness")
      (let [url (:url (edn/read-string (slurp ready-file)))
            client (HttpClient/newHttpClient)
            initialized (post-json client url nil (initialize-request))
            session-id (-> initialized .headers (.firstValue "Mcp-Session-Id")
                           (.orElse nil))
            next-id (atom 1)
            call! (fn [tool arguments]
                    (let [id (swap! next-id inc)
                          response (post-json client url session-id
                                              (tool-request id tool arguments))]
                      (is (= 200 (.statusCode response)))
                      (response-json response)))]
        (is (= 200 (.statusCode initialized)))
        (is (= "clj-surgeon"
               (get-in (response-json initialized)
                       [:result :serverInfo :name])))
        (is (string? session-id))
        (post-json client url session-id
                   {:jsonrpc "2.0" :method "notifications/initialized"})
        (exercise-prepared-wire! "streamable-http" call! source-file))
      ;; Both steps run, in order, independently of the other's success:
      ;; `stop-child!` cannot throw, and the workspace's deletion is owned by
      ;; the :each fixture besides.
      (finally
        (stop-child! child)
        (delete-tree! project)))))

;; @spec MCP-OP-PREP-ACT-005
(deftest sdk-json-containers-normalize-at-the-confirm-boundary
  (let [nested-fill (doto (java.util.LinkedHashMap.)
                      (.put "arguments.edits[0].to"
                            (java.util.ArrayList. ["(def alpha :new)"])))
        nested-request (doto (java.util.LinkedHashMap.)
                         (.put "confirm" (apply str (repeat 64 "a")))
                         (.put "fill" nested-fill))
        normalized (prepared-confirmation/public-keyword-map nested-request)
        valid-fill (doto (java.util.LinkedHashMap.)
                     (.put "arguments.edits[0].to" "(def alpha :new)"))
        valid-request (doto (java.util.LinkedHashMap.)
                        (.put "confirm" (apply str (repeat 64 "a")))
                        (.put "fill" valid-fill))
        validated (prepared-confirmation/validate-confirm-request valid-request)]
    (is (= {:confirm (apply str (repeat 64 "a"))
            :fill {"arguments.edits[0].to" ["(def alpha :new)"]}}
           normalized))
    (is (= true (:ok validated)))
    (is (= {"arguments.edits[0].to" "(def alpha :new)"}
           (:fill validated)))))

;; ---------------------------------------------------------------------------
;; Round one's teardown race, as a deterministic witness.
;;
;; Round one, 2/2 concurrent pairs: `java.io.IOException: Stream closed` out of
;; `stop-child!` via `FutureTask.report`. The mechanism is that `start-child!`
;; slurps the child's stderr in a `future`, and `stop-child!` closes the
;; child's streams and destroys it BEFORE `(deref stderr 5000 "")`. `deref`'s
;; default value covers a TIMEOUT, not an EXCEPTION: when the destroy wins the
;; race against the still-running slurp, the future completes exceptionally and
;; the deref rethrows. On a quiet box the slurp finishes first.
;;
;; The second-order failure is the one that cost the run: the throw escaped the
;; test's `(finally (stop-child! child) (delete-tree! project))` BEFORE
;; `delete-tree!` ran, the workspace survived, and the tmp-leak ratchet failed
;; the run a second time. One error plus one leak, from one race. A cleanup
;; step sequenced after a call that can throw is not cleanup.
;;
;; These two witnesses plant the exception directly rather than waiting for the
;; scheduler, so the MECHANISM is pinned deterministically; the race itself is
;; re-run under contention by dev/experiments/contention_witness.sh, because an
;; example test on a quiet box is a measurement of how idle the box was.
;; ---------------------------------------------------------------------------

(defn- exploding-child
  []
  (let [f (future (throw (ex-info "stderr reader died" {})))]
    (try @f (catch Throwable _ nil))
    {:process nil :stderr f}))

;; @spec TEST-ISO-RACE-001
(deftest stop-child-reports-a-stderr-reader-failure-as-a-typed-fact
  (let [receipt (stop-child! (exploding-child))]
    (is (map? receipt)
        "stop-child! must return a receipt, never throw: it runs in a finally")
    (is (= :stderr-reader-failed (:refusal receipt))
        (str "a stderr reader that died must be reported as a typed fact, got "
             (pr-str receipt)))
    (is (str/includes? (str (:message receipt)) "stderr reader died")
        "the refusal must carry what actually went wrong")))

;; @spec TEST-ISO-RACE-001
(deftest a-teardown-failure-still-deletes-the-workspace
  (let [project (temp-dir "prepared-wire-teardown-witness-")]
    (try
      :body-succeeded
      (finally
        (stop-child! (exploding-child))
        (delete-tree! project)))
    (is (not (.exists project))
        (str "the workspace " (.getPath project) " survived a teardown whose "
             "first step threw -- cleanup sequenced after a call that can "
             "throw is not cleanup"))))
