(ns marvin-voice-remote.director-control
  "Authenticated Director commands for the uniquely active Marvin capture client."
  (:require
   [marvin-voice-remote.json :as mjson]
   [clojure.string :as str]
   [marvin-voice-remote.sse-registry :as sse]
   [org.httpkit.server :as http]
   [taoensso.timbre :as log])
  (:import
   (java.time Instant)))

(def ^:private endpoint "/api/code-director/control/events")
(def ^:private command-cap 100)
(defonce ^:private clients (atom {}))
(defonce ^:private commands (atom {}))
(def ^:private client-lease-ms 360000)
(defonce ^:private client-registered-at (atom {}))
(defn- now-ms [] (System/currentTimeMillis))
(defn live-client-ids [client-map registered-at at-ms]
  (into #{}
        (keep (fn [[client-id _ch]]
                (let [registered-ms (get registered-at client-id)]
                  (when (and (number? registered-ms)
                             (< (- at-ms registered-ms) client-lease-ms))
                    client-id))))
        client-map))
(defonce ^:private write-lock (Object.))

(defn- now-iso [] (str (Instant/now)))

(defn- json-response [status body]
  {:status status
   :headers {"Content-Type" "application/json; charset=utf-8"
             "Cache-Control" "no-store"}
   :body (mjson/write body)})

(defn- parse-json-body [request]
  (mjson/parse (slurp (:body request))))

(defn- send! [ch payload]
  (locking write-lock
    (try
      (http/send! ch payload false)
      (catch Exception _ false))))

(defn- sse-frame [event-name value]
  (str "id: " (:id value) "\n"
       "event: " event-name "\n"
       "data: " (mjson/write value) "\n\n"))

(defn- client-id-of [request]
  (some-> (or (get-in request [:query-params "client"])
              (get-in request [:params "client"]))
          str
          str/trim))

(defn- reap! [client-id ch]
  (when (= ch (get @clients client-id))
    (swap! clients dissoc client-id)
    (swap! client-registered-at dissoc client-id))
  (sse/deregister! ch))

(defn- register! [client-id ch request]
  (let [old (get @clients client-id)]
    (swap! clients assoc client-id ch)
    (swap! client-registered-at assoc client-id (now-ms))
    (when old
      (when-not (= old ch)
        (try (http/close old) (catch Exception _)))))
  (sse/register! {:ch ch
                  :endpoint endpoint
                  :seat "code-director-capture"
                  :client-id client-id
                  :remote-addr (:remote-addr request)
                  :close! #(http/close ch)}))

(defn- bounded-assoc [state id command]
  (let [next-state (assoc state id command)]
    (if (<= (count next-state) command-cap)
      next-state
      (->> next-state
           vals
           (sort-by :created-at)
           (take-last command-cap)
           (map (juxt :id identity))
           (into {})))))

(defn dispatch-command
  "Pure command decision. Refuses ambiguity; duplicate IDs are idempotent."
  [state active-client-ids {:keys [id action]} now]
  (cond
    (contains? state id)
    [state (get state id)]

    (not (re-matches #"[0-9a-fA-F-]{36}" (str id)))
    [state {:error :invalid-id}]

    (not (#{"toggle-mute" "mute" "unmute" "clear" "over"} (str action)))
    [state {:error :invalid-action}]

    (not= 1 (count active-client-ids))
    [state {:error :capture-client-count
            :active-client-count (count active-client-ids)}]

    :else
    (let [command {:id (str id)
                   :action (str action)
                   :target-client (first active-client-ids)
                   :status "dispatched"
                   :created-at now}]
      [(bounded-assoc state (str id) command) command])))

(defn- prune-stale! [at-ms]
  (let [active (live-client-ids @clients @client-registered-at at-ms)]
    (doseq [[client-id ch] @clients
            :when (not (contains? active client-id))]
      (reap! client-id ch))
    active))

(defn handle-command [request]
  (try
    (let [body (parse-json-body request)
          active-clients (prune-stale! (now-ms))
          [next-state result] (dispatch-command @commands active-clients body (now-iso))]
      (cond
        (:error result)
        (json-response (if (= :capture-client-count (:error result)) 409 422)
                       (assoc result :ok false))

        (= "acknowledged" (:status result))
        (json-response 200 (assoc result :ok true :duplicate true))

        :else
        (let [fresh? (not (contains? @commands (:id result)))
              ch (get @clients (:target-client result))]
          (reset! commands next-state)
          (if (and ch (or (not fresh?) (send! ch (sse-frame "capture-command" result))))
            (json-response (if fresh? 202 200)
                           (assoc result :ok true :duplicate (not fresh?)))
            (do
              (swap! commands update (:id result) assoc
                     :status "failed" :error "capture client disconnected")
              (json-response 503 {:ok false
                                  :id (:id result)
                                  :error "capture client disconnected"}))))))
    (catch Exception error
      (log/warn error :director-control-command-failed)
      (json-response 400 {:ok false :error "invalid capture command"}))))

(defn- valid-ack-state? [action state]
  (or (= "failed" state)
      (case action
        "toggle-mute" (#{"muted" "unmuted"} state)
        "mute" (= "muted" state)
        "unmute" (= "unmuted" state)
        "clear" (= "cleared" state)
        "over" (= "over" state)
        false)))

(defn handle-ack [request]
  (try
    (let [{:keys [id client state message]} (parse-json-body request)
          command (get @commands (str id))]
      (cond
        (nil? command)
        (json-response 404 {:ok false :error "capture command not found"})

        (not= (str client) (:target-client command))
        (json-response 409 {:ok false :error "capture acknowledgement client mismatch"})

        (not (valid-ack-state? (:action command) (str state)))
        (json-response 422 {:ok false :error "capture acknowledgement state does not match command action"})

        :else
        (let [receipt (assoc command
                             :status "acknowledged"
                             :state (str state)
                             :message (subs (str message) 0 (min 500 (count (str message))))
                             :acknowledged-at (now-iso))]
          (swap! commands assoc (str id) receipt)
          (log/info :director-control-ack :id id :client client :state state)
          (json-response 200 (assoc receipt :ok true)))))
    (catch Exception error
      (log/warn error :director-control-ack-failed)
      (json-response 400 {:ok false :error "invalid capture acknowledgement"}))))

(defn handle-status [request]
  (if-let [command (get @commands (str (get-in request [:path-params :id])))]
    (json-response 200 (assoc command :ok true))
    (json-response 404 {:ok false :error "capture command not found"})))

(defn handle-state [_request]
  (let [active-clients (prune-stale! (now-ms))]
    (json-response 200 {:ok true
                        :active-clients (vec (sort active-clients))
                        :active-client-count (count active-clients)
                        :client-lease-ms client-lease-ms
                        :recent-commands (->> @commands vals (sort-by :created-at) (take-last 10) vec)})))

(defn handle-events [request]
  (let [client-id (client-id-of request)]
    (if-not (re-matches #"capture-[0-9a-fA-F-]{12,64}" (str client-id))
      (json-response 422 {:ok false :error "invalid capture client id"})
      (http/as-channel
        request
        {:on-open
         (fn [ch]
           (http/send! ch {:status 200
                           :headers {"Content-Type" "text/event-stream"
                                     "Cache-Control" "no-cache"
                                     "Connection" "keep-alive"
                                     "X-Accel-Buffering" "no"}}
                       false)
           (register! client-id ch request)
           (send! ch (str "event: hello\ndata: "
                          (mjson/write {:client client-id}) "\n\n"))
           (future
             (loop []
               (Thread/sleep 15000)
               (when (= ch (get @clients client-id))
                 (let [registered-at (get @client-registered-at client-id 0)]
                   (if (and (< (- (now-ms) registered-at) client-lease-ms)
                            (send! ch ": heartbeat\n\n"))
                     (recur)
                     (reap! client-id ch)))))))
         :on-close (fn [ch _status] (reap! client-id ch))}))))
