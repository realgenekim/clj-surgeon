(ns marvin-voice-remote.codex-app-server
  "Small, REPL-first client for the Codex app-server JSONL protocol.

  This namespace intentionally owns no web state. A client is one local Codex
  process plus its wire transcript; callers decide how conversations are
  presented. All public lifecycle functions are safe to call from an nREPL."
  (:require
   [clojure.data.json :as json]
   [marvin-voice-remote.json :as mjson]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.io BufferedReader BufferedWriter Closeable)
   (java.time Instant)
   (java.util.concurrent TimeUnit)))

(def default-timeout-ms 30000)
(def turn-timeout-ms 600000)

(defonce ^:private current-client (atom nil))

(defn now [] (str (Instant/now)))

(defn- json-read [line]
  (mjson/parse line))

(defn- append-jsonl! [{:keys [receipt-path receipt-lock]} record]
  (when receipt-path
    (locking receipt-lock
      (spit receipt-path (str (json/write-str record) "\n") :append true))))

(defn- receipt! [client direction payload]
  (append-jsonl! client {:at (now) :direction direction :payload payload}))

(defn- write-message! [{:keys [^BufferedWriter stdin write-lock] :as client} message]
  (when-not (.isAlive ^Process (:process client))
    (throw (ex-info "Codex app-server is not alive" {:message message})))
  (locking write-lock
    (receipt! client "client->server" message)
    (.write stdin (json/write-str message))
    (.newLine stdin)
    (.flush stdin)))

(defn- response? [message]
  (and (contains? message :id) (not (contains? message :method))))

(defn- dispatch! [{:keys [pending events event-monitor] :as client} message]
  (receipt! client "server->client" message)
  (if (response? message)
    (when-let [response-promise (get @pending (:id message))]
      (swap! pending dissoc (:id message))
      (deliver response-promise message))
    (do
      (swap! events conj message)
      (locking event-monitor (.notifyAll ^Object event-monitor)))))

(defn- record-stderr! [client line]
  (receipt! client "server-stderr" {:line line}))

(defn- fail-pending! [{:keys [pending]} cause]
  (doseq [[_ response-promise] (swap! pending (constantly {}))]
    (deliver response-promise cause)))

(defn- process-closed! [{:keys [events event-monitor] :as client}]
  (let [event {:method "client/process-closed"
               :params {:at (now)}}]
    (receipt! client "client-event" event)
    (swap! events conj event)
    (locking event-monitor (.notifyAll ^Object event-monitor))))

(defn- read-lines! [^BufferedReader reader consume on-close]
  (future
    (try
      (doseq [line (line-seq reader)]
        (consume line))
      (finally (on-close)))))

(defn- default-receipt-path []
  (let [directory (io/file "run-logs" "codex-cos")
        filename (str (str/replace (now) #"[:.]" "-") ".jsonl")]
    (.mkdirs directory)
    (.getAbsolutePath (io/file directory filename))))

(defn start!
  "Start one Codex app-server process. Options:
   :cwd          process working directory (default current directory)
   :codex        executable (default `codex`)
   :receipt-path JSONL path, or false to disable receipts.

   The returned client is deliberately an ordinary map so it is easy to
   inspect at the REPL. Call initialize! before any other protocol request."
  ([] (start! {}))
  ([{:keys [cwd codex receipt-path]
     :or {cwd (System/getProperty "user.dir") codex "codex"}}]
   (let [builder (doto (ProcessBuilder. ^java.util.List [codex "app-server" "--stdio"])
                   (.directory (io/file cwd)))
         process (.start builder)
         client {:process process
                 :stdin (io/writer (.getOutputStream process))
                 :pending (atom {})
                 :events (atom [])
                 :next-id (atom 0)
                 :write-lock (Object.)
                 :event-monitor (Object.)
                 :receipt-lock (Object.)
                 :receipt-path (when-not (false? receipt-path)
                                 (or receipt-path (default-receipt-path)))
                 :started-at (now)
                 :cwd (.getAbsolutePath (io/file cwd))}
         stdout (io/reader (.getInputStream process))
         stderr (io/reader (.getErrorStream process))]
     (assoc client
            :stdout-reader
            (read-lines! stdout
                         #(try (dispatch! client (json-read %))
                               (catch Exception e
                                 (record-stderr! client (str "invalid stdout JSON: " % " · " (.getMessage e)))))
                         #(do
                            (fail-pending! client (ex-info "Codex app-server stdout closed" {}))
                            (process-closed! client)))
            :stderr-reader
            (read-lines! stderr #(record-stderr! client %) (constantly nil))))))

(defn request!
  "Send one JSON-RPC request and wait for its correlated response."
  ([client method params] (request! client method params default-timeout-ms))
  ([client method params timeout-ms]
   (let [id (swap! (:next-id client) inc)
         response-promise (promise)]
     (swap! (:pending client) assoc id response-promise)
     (try
       (write-message! client {:method method :id id :params params})
       (catch Exception e
         (swap! (:pending client) dissoc id)
         (throw e)))
     (let [response (deref response-promise timeout-ms ::timeout)]
       (when (= ::timeout response)
         (swap! (:pending client) dissoc id)
         (throw (ex-info "Timed out waiting for Codex app-server response"
                         {:method method :id id :timeout-ms timeout-ms})))
       (when (instance? Throwable response) (throw response))
       (when-let [error (:error response)]
         (throw (ex-info "Codex app-server returned an error"
                         {:method method :id id :error error})))
       (:result response)))))

(defn notify! [client method params]
  (write-message! client (cond-> {:method method} params (assoc :params params))))

(defn initialize!
  "Perform the mandatory initialize/initialized handshake."
  [client]
  (let [result (request! client "initialize"
                         {:clientInfo {:name "marvin_codex_cos_lab"
                                       :title "Marvin Codex COS Laboratory"
                                       :version "0.1.0"}
                          :capabilities {:experimentalApi true}})]
    (notify! client "initialized" nil)
    result))

(defn thread-start-params
  "Build the version-pinned thread/start parameters without sending them."
  [client {:keys [cwd ephemeral approval-policy sandbox]
           :or {cwd (:cwd client)
                ephemeral true
                approval-policy "never"
                sandbox "read-only"}}]
  {:cwd (.getAbsolutePath (io/file cwd))
   :ephemeral ephemeral
   :approvalPolicy approval-policy
   :sandbox sandbox})

(defn start-thread!
  "Start a Codex thread rooted at cwd. Defaults to read-only and ephemeral."
  ([client] (start-thread! client {}))
  ([client options]
   (-> (request! client "thread/start" (thread-start-params client options))
       :thread)))

(defn resume-thread! [client thread-id]
  (-> (request! client "thread/resume" {:threadId thread-id}) :thread))

(defn start-turn!
  "Submit one text turn and return the initial turn object."
  [client thread-id text]
  (-> (request! client "turn/start"
                {:threadId thread-id
                 :input [{:type "text" :text text}]})
      :turn))

(defn terminal-turn-event?
  ([event] (= "turn/completed" (:method event)))
  ([turn-id event]
   (and (terminal-turn-event? event)
        (= turn-id (get-in event [:params :turn :id])))))

(defn await-event!
  "Wait for a future or already-buffered event matching predicate."
  ([client predicate] (await-event! client predicate turn-timeout-ms))
  ([{:keys [events event-monitor]} predicate timeout-ms]
   (let [deadline (+ (System/currentTimeMillis) timeout-ms)]
     (loop [cursor 0]
       (let [[state value]
             (locking event-monitor
               (let [snapshot @events
                     match (some #(when (predicate %) %)
                                 (subvec snapshot (min cursor (count snapshot))))]
                 (if match
                   [:found match]
                   (let [remaining (- deadline (System/currentTimeMillis))]
                     (when-not (pos? remaining)
                       (throw (ex-info "Timed out waiting for Codex app-server event"
                                       {:timeout-ms timeout-ms
                                        :event-count (count snapshot)})))
                     (.wait ^Object event-monitor remaining)
                     [:retry (count snapshot)]))))]
         (if (= :found state) value (recur value)))))))

(defn agent-text
  "Return streamed agent-message text for one turn from captured events."
  [client turn-id]
  (->> @(:events client)
       (filter #(and (= "item/agentMessage/delta" (:method %))
                     (= turn-id (get-in % [:params :turnId]))))
       (map #(get-in % [:params :delta]))
       (remove nil?)
       (apply str)))

(defn turn!
  "Submit one text turn, wait for turn/completed, and return a compact receipt."
  ([client thread-id text] (turn! client thread-id text turn-timeout-ms))
  ([client thread-id text timeout-ms]
   (let [started-ns (System/nanoTime)
         turn (start-turn! client thread-id text)
         completed (await-event! client #(terminal-turn-event? (:id turn) %) timeout-ms)]
     {:thread-id thread-id
      :turn-id (:id turn)
      :status (get-in completed [:params :turn :status])
      :elapsed-ms (long (/ (- (System/nanoTime) started-ns) 1000000))
      :text (agent-text client (:id turn))
      :completed completed})))

(defn status [client]
  {:alive? (.isAlive ^Process (:process client))
   :pid (.pid ^Process (:process client))
   :started-at (:started-at client)
   :cwd (:cwd client)
   :receipt-path (:receipt-path client)
   :pending (count @(:pending client))
   :events (count @(:events client))})

(defn stop! [client]
  (when client
    (try (.close ^Closeable (:stdin client)) (catch Exception _))
    (let [process ^Process (:process client)]
      (.destroy process)
      (when-not (.waitFor process 3 TimeUnit/SECONDS)
        (.destroyForcibly process)
        (.waitFor process 3 TimeUnit/SECONDS)))
    (when (= client @current-client) (reset! current-client nil))
    {:alive? (.isAlive ^Process (:process client))
     :exit-code (try (.exitValue ^Process (:process client)) (catch Exception _ nil))
     :receipt-path (:receipt-path client)}))

(defn connect!
  "REPL convenience: stop the previous laboratory client, start a fresh one,
  and complete initialization. Returns the new client."
  ([] (connect! {}))
  ([options]
   (when-let [old @current-client] (stop! old))
   (let [client (start! options)]
     (try
       (initialize! client)
       (reset! current-client client)
       client
       (catch Exception e
         (stop! client)
         (throw e))))))

(defn client [] @current-client)

(defn disconnect! []
  (when-let [active @current-client]
    (stop! active)))
