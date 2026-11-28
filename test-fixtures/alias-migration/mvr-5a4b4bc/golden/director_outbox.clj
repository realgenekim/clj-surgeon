(ns marvin-voice-remote.director-outbox
  "Durable, acknowledged SSE delivery from the frozen Bridge 4 engine to Code
   Director. This namespace observes exactly one channel seat; it never changes
   capture, transcription, OVER policy, or any Bridge handler.

   Cloud Run is deliberately max-instances=1, and blob/save-edn! writes through
   to GCS. A finalized utterance is therefore persisted before the originating
   channel-state swap returns. SSE is only the low-latency notification layer:
   every unacknowledged event is replayed after reconnect."
  (:require
   [marvin-voice-remote.json :as mjson]
   [clojure.string :as str]
   [marvin-voice-remote.app-route :as app-route]
   [marvin-voice-remote.blob :as blob]
   [marvin-voice-remote.channel :as channel]
   [marvin-voice-remote.sse-registry :as sse]
   [org.httpkit.server :as http]
   [taoensso.timbre :as log])
  (:import
   (java.time Instant)
   (java.util UUID)))

(def ^:private state-file "code-director-outbox.edn")
(def ^:private endpoint "/api/code-director/events")
(def ^:private ack-history-cap 200)
(def ^:private initial-state {:version 1 :next-seq 1 :events []})

(defn- now-iso [] (str (Instant/now)))

(defn enqueue-event
  "Pure transition: append one pending delivery with monotonically increasing
   sequence and stable UUID. Acknowledged history is bounded; pending work is
   never discarded."
  [state message id created-at]
  (let [state (merge initial-state state)
        seq-no (:next-seq state)
        event (cond-> {:id id
                       :seq seq-no
                       :seat app-route/director-seat
                       :from (str (or (:from message) "gene"))
                       :text (str (:text message))
                       :source-ts (:ts message)
                       :created-at created-at
                       :status :pending}
                (some? (:audio-duration-ms message))
                (assoc :audio-duration-ms (:audio-duration-ms message)))
        events (conj (vec (:events state)) event)
        pending (filterv #(= :pending (:status %)) events)
        acked (->> events
                   (remove #(= :pending (:status %)))
                   (take-last ack-history-cap)
                   vec)]
    [(assoc state :next-seq (inc seq-no) :events (into acked pending)) event]))

(defn acknowledge-event
  "Pure transition. Returns [new-state event], or [state nil] for an unknown id.
   Repeated acknowledgements are idempotent and return the existing event."
  [state id receipt now]
  (if-let [event (some #(when (= id (:id %)) %) (:events state))]
    (if (= :acknowledged (:status event))
      [state event]
      (let [updated (merge event
                           {:status :acknowledged :acknowledged-at now}
                           (select-keys receipt [:outcome :target :message]))]
        [(update state :events
                 (fn [events]
                   (mapv #(if (= id (:id %)) updated %) events)))
         updated]))
    [state nil]))

(defn pending-events [state]
  (->> (:events state)
       (filter #(= :pending (:status %)))
       (sort-by :seq)
       vec))

(defonce ^:private outbox
  (atom (try
          (let [loaded (blob/load-edn state-file)]
            (if (and (map? loaded) (= 1 (:version loaded)))
              (merge initial-state loaded)
              initial-state))
          (catch Exception error
            (log/warn error :director-outbox-restore-failed)
            initial-state))))

(defonce ^:private subscribers (atom #{}))
(defonce ^:private state-lock (Object.))
(defonce ^:private write-lock (Object.))

(defn- persist! [state]
  (blob/save-edn! state-file state)
  state)

(defn- sse-frame [event-name event]
  (str "id: " (:id event) "\n"
       "event: " event-name "\n"
       "data: " (mjson/write event) "\n\n"))

(defn- send! [ch payload]
  (locking write-lock
    (try (http/send! ch payload false)
         (catch Exception _ false))))

(defn- reap! [ch]
  (swap! subscribers disj ch)
  (sse/deregister! ch))

(defn- broadcast! [event]
  (let [frame (sse-frame "director-prompt" event)]
    (sse/push-off-thread!
      (fn []
        (doseq [ch @subscribers]
          (when-not (send! ch frame)
            (reap! ch)))))))

(defn- append-message! [message]
  (when (and (map? message) (not (str/blank? (str (:text message)))))
    (let [event (locking state-lock
                  (let [[next-state event]
                        (enqueue-event @outbox message (str (UUID/randomUUID)) (now-iso))]
                    (persist! next-state)
                    (reset! outbox next-state)
                    event))]
      (log/info :director-outbox-enqueued :id (:id event) :seq (:seq event)
                :chars (count (:text event))
                :audio-duration-ms (:audio-duration-ms event))
      (broadcast! event)
      event)))

(defn newly-appended-director-messages
  "Pure observer projection. Only append growth on the one fixed seat counts;
   drains, reply changes, and every other seat—including bridge-cos—produce []."
  [old-state new-state]
  (let [old (vec (get-in old-state [:messages app-route/director-seat] []))
        new (vec (get-in new-state [:messages app-route/director-seat] []))]
    (if (and (> (count new) (count old))
             (= old (subvec new 0 (count old))))
      (subvec new (count old))
      [])))

(defonce ^:private channel-watch-installed
  (do
    (add-watch channel/channel-state ::director-outbox
               (fn [_ _ old-state new-state]
                 (doseq [message (newly-appended-director-messages old-state new-state)]
                   (try (append-message! message)
                        (catch Exception error
                          (log/error error :director-outbox-enqueue-failed))))))
    true))

(defn- parse-json-body [request]
  (mjson/parse (slurp (:body request))))

(defn- json-response [status body]
  {:status status
   :headers {"Content-Type" "application/json; charset=utf-8"
             "Cache-Control" "no-store"}
   :body (mjson/write body)})

(defn handle-state [_request]
  (let [state @outbox]
    (json-response 200 {:ok true
                        :seat app-route/director-seat
                        :pending (count (pending-events state))
                        :next_seq (:next-seq state)})))

(defn- acknowledgement-text [{:keys [outcome target]}]
  (str (case (name outcome)
         "submitted" "Submitted in Director"
         "prepared" "Prepared in Director"
         "muted" "Muted · dictation retained but not delivered")
       (when (seq (str target)) (str " · " target))))

(defn- remove-source-message [messages event]
  (let [removed? (volatile! false)]
    (reduce (fn [acc message]
              (if (and (not @removed?)
                       (= (:text message) (:text event))
                       (= (:ts message) (:source-ts event)))
                (do (vreset! removed? true) acc)
                (conj acc message)))
            []
            messages)))

(defn handle-ack [request]
  (try
    (let [{:keys [id outcome target message] :as receipt} (parse-json-body request)]
      (cond
        (not (re-matches #"[0-9a-fA-F-]{36}" (str id)))
        (json-response 422 {:ok false :error "invalid delivery id"})

        (not (#{"muted" "prepared" "submitted"} (str outcome)))
        (json-response 422 {:ok false :error "outcome must be muted, prepared, or submitted"})

        :else
        (let [{:keys [event fresh?]}
              (locking state-lock
                (let [before @outbox
                      prior (some #(when (= (str id) (:id %)) %) (:events before))
                      [next-state event]
                      (acknowledge-event before (str id)
                                         {:outcome (keyword outcome)
                                          :target (str target)
                                          :message (str message)}
                                         (now-iso))
                      fresh? (and event (not= :acknowledged (:status prior)))]
                  (when fresh?
                    (persist! next-state)
                    (reset! outbox next-state))
                  {:event event :fresh? fresh?}))]
          (if-not event
            (json-response 404 {:ok false :error "delivery not found"})
            (do
              ;; Bound the old mailbox queue only after durable acknowledgement.
              (when fresh?
                (swap! channel/channel-state update-in [:messages app-route/director-seat]
                       #(remove-source-message (vec (or % [])) event))
                ;; Reuse Bridge's proven reply fan-out/TTS as the phone receipt.
                (swap! channel/channel-state channel/enqueue :replies app-route/director-seat
                       {:id (:id event) :text (acknowledgement-text event) :ts (now-iso)}))
              (json-response 200 {:ok true :id (:id event)
                                  :status "acknowledged" :duplicate (not fresh?)}))))))
    (catch Exception error
      (log/warn error :director-outbox-ack-failed)
      (json-response 400 {:ok false :error "invalid acknowledgement"}))))

(defn handle-events [request]
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
       (sse/register! {:ch ch
                       :endpoint endpoint
                       :seat app-route/director-seat
                       :client-id (sse/client-id-of request)
                       :remote-addr (:remote-addr request)
                       :close! #(http/close ch)})
       (swap! subscribers conj ch)
       (send! ch (str "event: hello\ndata: "
                      (mjson/write {:seat app-route/director-seat
                                       :pending (count (pending-events @outbox))})
                      "\n\n"))
       (doseq [event (pending-events @outbox)]
         (send! ch (sse-frame "director-prompt" event)))
       (future
         (loop []
           (Thread/sleep 15000)
           (when (contains? @subscribers ch)
             (if (sse/rotate-due? ch)
               (do (send! ch ": recycle\n\n")
                   (swap! subscribers disj ch)
                   (sse/close! ch :rotation))
               (if (send! ch ": heartbeat\n\n")
                 (recur)
                 (reap! ch)))))))
     :on-close (fn [ch _status] (reap! ch))}))
