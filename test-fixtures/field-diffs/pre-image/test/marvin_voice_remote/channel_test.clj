(ns marvin-voice-remote.channel-test
  "Tests for the channel relay (marvin-channel connector transport edge).
   Pure queue ops + bearer auth + the ring handlers (claim-once drain, auth, validation)."
  (:require
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing use-fixtures]]
   [marvin-voice-remote.channel :as channel]
   [marvin-voice-remote.groq :as groq]
   [marvin-voice-remote.reducer-session :as reducer-session]
   [marvin-voice-remote.reducer.protocol :as protocol]
   [marvin-voice-remote.sse-registry :as sse]
   [marvin-voice-remote.tts :as tts]))

(use-fixtures :each (fn [t]
                      (reset! channel/channel-state {:messages {} :replies {}})
                      (reset! channel/metrics {:auth-fails 0 :seats {}})
                      (reset! channel/reply-acks {})
                      (reset! channel/dictations [])
                      (t)))

(defn- json-body-req [m]
  {:body (io/input-stream (.getBytes (json/write-str m) "UTF-8"))})

(defn- read-json [resp] (json/read-str (:body resp) :key-fn keyword))

(deftest isolated-upload-probe
  (with-redefs [channel/token-ok? (constantly true)]
    (let [payload (.getBytes "native-upload-probe" "UTF-8")
          before @channel/channel-state
          response (channel/handle-voice-lab-upload-probe
                     {:body (java.io.ByteArrayInputStream. payload)})
          receipt (read-json response)]
      (is (= 200 (:status response)))
      (is (true? (:ok receipt)))
      (is (= (alength payload) (:bytes receipt)))
      (is (= 64 (count (:sha256 receipt))))
      (is (number? (:server-read-ms receipt)))
      (is (= before @channel/channel-state) "probe never mutates conversation/queue state")))
  (testing "probe remains bearer-token gated"
    (with-redefs [channel/token-ok? (constantly false)]
      (is (= 401 (:status (channel/handle-voice-lab-upload-probe
                            {:body (java.io.ByteArrayInputStream. (byte-array 1))})))))))

;; --- pure queue ops ---
(deftest enqueue-and-drain-pure
  (testing "enqueue appends per kind+seat"
    (let [s (-> {:messages {} :replies {}}
                (channel/enqueue :messages "skiff" {:text "a"})
                (channel/enqueue :messages "skiff" {:text "b"})
                (channel/enqueue :messages "bridge" {:text "c"}))]
      (is (= 2 (count (get-in s [:messages "skiff"]))))
      (is (= 1 (count (get-in s [:messages "bridge"]))) "seats are isolated")))
  (testing "drain returns items + leaves [] behind"
    (let [s (channel/enqueue {:messages {} :replies {}} :messages "skiff" {:text "a"})
          [s' items] (channel/drain s :messages "skiff")]
      (is (= [{:text "a"}] items))
      (is (= [] (get-in s' [:messages "skiff"]))))))

;; --- bearer auth (pure) ---
(deftest bearer-matches
  (is (channel/bearer-matches? {:headers {"authorization" "Bearer s3cret"}} "s3cret"))
  (is (not (channel/bearer-matches? {:headers {"authorization" "Bearer nope"}} "s3cret")))
  (is (not (channel/bearer-matches? {:headers {}} "s3cret")))
  (is (not (channel/bearer-matches? {:headers {"authorization" "Bearer x"}} nil))))

;; --- handlers: happy path (auth stubbed open) ---
(deftest message-roundtrip
  (with-redefs [channel/token-ok? (constantly true)]
    (testing "POST message enqueues, GET drains once (claim-once)"
      (is (:ok (read-json (channel/handle-post-message (json-body-req {:seat "skiff" :from "gene" :text "hi"})))))
      (let [drained (read-json (channel/handle-get-messages {:params {:seat "skiff"}}))]
        (is (= 1 (count (:messages drained))))
        (is (= "hi" (-> drained :messages first :text)))
        (is (= "gene" (-> drained :messages first :from))))
      (testing "second GET is empty — message was claimed"
        (is (= [] (:messages (read-json (channel/handle-get-messages {:params {:seat "skiff"}})))))))))

(deftest reply-roundtrip
  (with-redefs [channel/token-ok? (constantly true)]
    (is (:ok (read-json (channel/handle-post-reply (json-body-req {:seat "skiff" :text "done" :reply_to "1"})))))
    (let [r (read-json (channel/handle-get-replies {:params {:seat "skiff"}}))]
      (is (= "done" (-> r :replies first :text)))
      (is (= "1" (-> r :replies first :reply_to)))
      (is (string? (-> r :replies first :reply_id)) "every reply gets a stable id"))))

(deftest reply-id-reuses-sender-id
  (with-redefs [channel/token-ok? (constantly true)]
    (channel/handle-post-reply (json-body-req {:seat "skiff" :text "done" :id "upstream-42"}))
    (is (= "upstream-42"
           (-> (channel/handle-get-replies {:params {:seat "skiff"}})
               read-json :replies first :reply_id)))))

(deftest reply-playback-acks-are-append-only-and-exposed
  (with-redefs [channel/token-ok? (constantly true)]
    (doseq [ack [{:reply_id "r-1" :event "started" :mode "tts-audio" :ts "2026-07-18T01:00:00Z"}
                 {:reply_id "r-1" :event "finished" :mode "tts-audio" :ts "2026-07-18T01:00:05Z"}]]
      (is (:ok (read-json (channel/handle-reply-ack (json-body-req ack))))))
    (let [events (get @channel/reply-acks "r-1")
          state (get (channel/reply-acks-snapshot @channel/reply-acks) "r-1")]
      (is (= 2 (count events)) "ACK writes only append")
      (is (:started? state))
      (is (:finished? state))
      (is (= "2026-07-18T01:00:00Z" (:started-ts state)))
      (is (= "2026-07-18T01:00:05Z" (:finished-ts state)))))
  (testing "browser session auth works without exposing CHANNEL_TOKEN to JS"
    (with-redefs [channel/token-ok? (constantly false)
                  marvin-voice-remote.auth/authenticated? (constantly true)]
      (is (= 200 (:status (channel/handle-reply-ack
                            (json-body-req {:reply_id "r-browser" :event "started" :mode "tts-audio"})))))))
  (testing "invalid events and unauthenticated calls are rejected"
    (with-redefs [channel/token-ok? (constantly true)]
      (is (= 400 (:status (channel/handle-reply-ack
                            (json-body-req {:reply_id "r-1" :event "drained" :mode "tts-audio"})))))
      (is (= 400 (:status (channel/handle-reply-ack
                            (json-body-req {:reply_id "r-1" :event "started" :mode "unknown"}))))))
    (with-redefs [channel/token-ok? (constantly false)
                  marvin-voice-remote.auth/authenticated? (constantly false)]
      (is (= 401 (:status (channel/handle-reply-ack
                            (json-body-req {:reply_id "r-1" :event "started" :mode "tts-audio"}))))))))

(deftest reply-playback-acks-are-bounded-and-validated
  (let [ack (fn [n] {:event "started" :mode "tts-audio" :ts (str n) :received-ts (format "%04d" n)})
        per-reply (reduce (fn [acks n] (channel/append-reply-ack acks "r" (ack n))) {} (range 21))
        across-replies (reduce (fn [acks n]
                                 (channel/append-reply-ack acks (str "r-" n) (ack n)))
                               {} (range 2001))]
    (is (= 20 (count (get per-reply "r"))) "live projection caps replay events")
    (is (= "1" (:ts (first (get per-reply "r")))) "oldest overflow event is evicted")
    (is (= 2000 (count across-replies)) "live projection caps reply IDs")
    (is (nil? (get across-replies "r-0")))
    (is (some? (get across-replies "r-2000"))))
  (with-redefs [channel/token-ok? (constantly true)]
    (is (= 400 (:status (channel/handle-reply-ack
                          (json-body-req {:reply_id "" :event "started" :mode "tts-audio"})))))
    (is (= 400 (:status (channel/handle-reply-ack
                          (json-body-req {:reply_id (apply str (repeat 257 "x"))
                                          :event "started"
                                          :mode "tts-audio"})))))))

(deftest reply-playback-client-acks-only-observed-start-and-normal-finish
  (let [player (slurp "resources/public/js/bridge-player.js")]
    (is (re-find #"ackReply\('started', 'tts-audio'\)" player)
        "HTML audio playing emits the start receipt")
    (is (re-find #"ackReply\('finished', 'tts-audio'\)" player)
        "HTML audio ended emits the finish receipt")
    (is (re-find #"u\.onstart = function \(\) \{ started = true; ackReply\('started', 'speech-synthesis'\); \}" player)
        "phone-voice fallback identifies its own start")
    (is (re-find #"u\.onerror = function \(\) \{ done\(false\); \}" player)
        "fallback errors cannot masquerade as normal completion")
    (is (not (re-find #"audio\.play\(\).*ackReply" player))
        "calling play is not treated as evidence that playback started")))

(deftest seat-isolation-over-handlers
  (with-redefs [channel/token-ok? (constantly true)]
    (channel/handle-post-message (json-body-req {:seat "skiff" :text "for-skiff"}))
    (channel/handle-post-message (json-body-req {:seat "bridge" :text "for-bridge"}))
    (is (= "for-bridge" (-> (channel/handle-get-messages {:params {:seat "bridge"}}) read-json :messages first :text)))
    (is (= 1 (count (:messages (read-json (channel/handle-get-messages {:params {:seat "skiff"}}))))) "skiff still has its own")))

;; --- handlers: validation + auth ---
(deftest validation-and-auth
  (with-redefs [channel/token-ok? (constantly true)]
    (testing "missing seat/text → 400"
      (is (= 400 (:status (channel/handle-post-message (json-body-req {:seat "skiff"})))))
      (is (= 400 (:status (channel/handle-post-reply (json-body-req {:text "x"})))))
      (is (= 400 (:status (channel/handle-get-messages {:params {}}))))))
  (testing "no auth → 401, and nothing leaks"
    (with-redefs [channel/token-ok? (constantly false)]
      (is (= 401 (:status (channel/handle-post-message (json-body-req {:seat "skiff" :text "x"})))))
      (is (= 401 (:status (channel/handle-get-messages {:params {:seat "skiff"}})))))))

;; --- #32 full-loop regression fixture: phone → bridge → reply → phone ---
;; Locks the exact round-trip the relay must always honor (it broke once today). If any
;; step's claim-once / routing / threading regresses, this fails.
(deftest full-loop-regression
  (with-redefs [channel/token-ok? (constantly true)]
    ;; 1. phone posts a dictated message to seat=bridge
    (is (:ok (read-json (channel/handle-post-message
                          (json-body-req {:seat "bridge" :from "gene" :id "m1"
                                          :text "what is on my calendar tomorrow?"})))))
    ;; 2. the bridge connector drains it EXACTLY once
    (let [d (read-json (channel/handle-get-messages {:params {:seat "bridge"}}))]
      (is (= 1 (count (:messages d))))
      (is (= "what is on my calendar tomorrow?" (-> d :messages first :text)))
      (is (= "m1" (-> d :messages first :id))))
    (is (= [] (:messages (read-json (channel/handle-get-messages {:params {:seat "bridge"}}))))
        "claim-once: a redelivery finds nothing")
    ;; 3. the bridge replies; the phone drains the reply EXACTLY once
    (is (:ok (read-json (channel/handle-post-reply
                          (json-body-req {:seat "bridge" :text "You have 3 meetings." :reply_to "m1"})))))
    (let [r (read-json (channel/handle-get-replies {:params {:seat "bridge"}}))]
      (is (= 1 (count (:replies r))))
      (is (= "You have 3 meetings." (-> r :replies first :text)))
      (is (= "m1" (-> r :replies first :reply_to))))
    (is (= [] (:replies (read-json (channel/handle-get-replies {:params {:seat "bridge"}})))))))

;; --- dictate handler: server-side error CATEGORIES feed the client retry queue ---
;; A silent/too-short capture (blank transcript) and a Groq 4xx are PERMANENT (400 → client marks
;; failed, stops). A Groq 5xx/timeout is RETRYABLE (500 → client keeps retrying).
(deftest dictate-handler-error-categories
  (let [mk-req (fn [id]
                 (let [tmp (java.io.File/createTempFile "dtest" ".webm")]
                   (spit tmp "xxxxxxxxxxxxxxxx")
                   {:tmp tmp :req {:params {:audio {:tempfile tmp :filename "d.webm"} :dedup_id id}}}))]
    (reset! @#'channel/bridge-dictate-seen {:order [] :m {}})
    (reset! channel/channel-state {:messages {} :replies {}})
    (testing "blank transcript -> 400 PERMANENT, and NO empty 'you' turn is enqueued"
      (with-redefs [groq/transcribe-audio (constantly {:transcript "   "})]
        (let [{:keys [tmp req]} (mk-req "B1")
              resp (channel/handle-bridge-dictate req)]
          (is (= 400 (:status resp)))
          (is (empty? (get-in @channel/channel-state [:messages "bridge"])) "blank capture never enqueues")
          (.delete tmp))))
    (testing "Groq 4xx -> 400 PERMANENT (client will mark the record failed and stop retrying)"
      (with-redefs [groq/transcribe-audio (fn [_] (throw (ex-info "bad audio" {:status 415 :body "unsupported"})))]
        (let [{:keys [tmp req]} (mk-req "B2")]
          (is (= 400 (:status (channel/handle-bridge-dictate req))))
          (.delete tmp))))
    (testing "Groq 5xx -> 500 RETRYABLE (client keeps the record pending and retries)"
      (with-redefs [groq/transcribe-audio (fn [_] (throw (ex-info "groq down" {:status 503 :body "upstream"})))]
        (let [{:keys [tmp req]} (mk-req "B3")]
          (is (= 500 (:status (channel/handle-bridge-dictate req))))
          (.delete tmp))))
    (testing "A REFUSAL WE MADE OURSELVES -> 400 PERMANENT (Gene, 2026-07-28)"
      ;; `groq/transcribe-audio` rejects a sub-kilobyte file BEFORE Groq is
      ;; called, so that ex-info carries no `:status` — and the old rule scored
      ;; anything without one as a SERVER fault. A 500 is exactly what tells a
      ;; retrying uploader to send the same bytes again, so an empty capture came
      ;; back three times to be refused three times. No number of retries will
      ;; make 300 bytes into a recording.
      (with-redefs [groq/transcribe-audio
                    (fn [_] (throw (ex-info "No audio captured (recording too short)"
                                            {:path "/tmp/x" :size 300 :client-error? true})))]
        (let [{:keys [tmp req]} (mk-req "B4")
              resp (channel/handle-bridge-dictate req)]
          (is (= 400 (:status resp)))
          (is (nil? (:groq-status (json/read-str (:body resp) :key-fn keyword)))
              "and it does NOT put a Groq status on a request Groq never saw")
          (.delete tmp))))))

(deftest the-dictate-status-is-a-retry-instruction-not-a-taxonomy
  "Every uploader on this app reads the status that way: 4xx marks the record
   permanently failed and stops, 5xx keeps it pending and tries again. So the
   pure rule is worth pinning on its own, away from the handler that uses it."
  (testing "the server was the problem — try again"
    (is (= 500 (channel/dictate-client-status nil nil)))
    (is (= 500 (channel/dictate-client-status 503 nil)))
    (is (= 500 (channel/dictate-client-status 500 false))))
  (testing "the request was the problem — do not"
    (is (= 400 (channel/dictate-client-status 400 nil)))
    (is (= 400 (channel/dictate-client-status 415 nil)))
    (is (= 400 (channel/dictate-client-status 499 nil))))
  (testing "…including a refusal made before the upstream was ever called"
    (is (= 400 (channel/dictate-client-status nil true))
        "no status at all, but the bytes are still the problem")))

;; --- bridge-dictate idempotency (offline persist-and-retry dedup) ---
;; A retry of a capture the server ALREADY processed (response lost in a dead zone) carries
;; the SAME dedup_id; the server must return the cached transcript and NOT re-enqueue.
(deftest dictate-remember-pure
  (testing "dictate-remember is a bounded most-recent map of dedup_id -> transcript"
    (let [seen (reduce (fn [acc i] (#'channel/dictate-remember acc (str "id" i) (str "t" i)))
                       {:order [] :m {}}
                       (range 70))]
      (is (= 64 (count (:order seen))) "bounded to the most-recent ~64")
      (is (nil? (get-in seen [:m "id0"])) "oldest evicted")
      (is (= "t69" (get-in seen [:m "id69"])) "newest retained"))))

(deftest dictate-dedup-short-circuits
  (testing "a dedup_id already processed returns the cached transcript, no audio needed, no double-enqueue"
    (reset! @#'channel/bridge-dictate-seen {:order ["RID"] :m {"RID" "the cached transcript"}})
    (let [resp (channel/handle-bridge-dictate {:params {:dedup_id "RID"}})
          body (read-json resp)]
      (is (= 200 (:status resp)))
      (is (true? (:ok body)))
      (is (true? (:dedup body)))
      (is (= "the cached transcript" (:transcript body)))
      ;; idempotent: the duplicate did NOT enqueue a second 'you' turn
      (is (empty? (get-in @channel/channel-state [:messages "bridge"])) "no re-enqueue on a dup"))
    (reset! @#'channel/bridge-dictate-seen {:order [] :m {}}))
  (testing "an UNSEEN dedup_id with no audio still 400s (dedup check doesn't mask missing audio)"
    (reset! @#'channel/bridge-dictate-seen {:order [] :m {}})
    (let [resp (channel/handle-bridge-dictate {:params {:dedup_id "NEW"}})]
      (is (= 400 (:status resp))))))

;; --- DICTATION LOG (the durable diagnosability surface): pure ops + write points ---
(deftest dictation-append-cap-and-order
  (testing "appends in order, newest LAST, capped to the most-recent 200"
    (let [ds (reduce (fn [acc i] (channel/dictation-append acc {:id (str "d" i) :ts (str i)}))
                     []
                     (range 250))]
      (is (= 200 (count ds)) "bounded to the 200-cap")
      (is (= "d50" (:id (first ds))) "oldest-kept is the 51st (50 evicted)")
      (is (= "d249" (:id (last ds))) "newest is last (append order preserved)"))))

(deftest dictation-mark-answered-idempotency
  (testing "stamps the MOST-RECENT unanswered record only, and only when :answered-ts is nil"
    (let [ds  [{:id "a" :answered-ts nil} {:id "b" :answered-ts nil} {:id "c" :answered-ts "T0"}]
          ds1 (channel/dictation-mark-answered ds "T1")]
      (is (= "T1" (:answered-ts (nth ds1 1))) "the most-recent UNANSWERED (b) gets stamped")
      (is (nil? (:answered-ts (nth ds1 0))) "an earlier unanswered (a) is left for its own reply")
      (is (= "T0" (:answered-ts (nth ds1 2))) "an already-answered record is NEVER overwritten")
      (testing "a second stamp moves to the next-most-recent unanswered (a)"
        (let [ds2 (channel/dictation-mark-answered ds1 "T2")]
          (is (= "T2" (:answered-ts (nth ds2 0))))
          (is (= "T1" (:answered-ts (nth ds2 1))) "b's stamp is preserved")))))
  (testing "no unanswered record -> unchanged"
    (let [ds [{:id "a" :answered-ts "T0"}]]
      (is (= ds (channel/dictation-mark-answered ds "T9"))))))

(deftest dictation-append-failure-dedups-retry-spam
  (testing "a 5xx retry-loop logs the failure ONCE per id (no cap-evicting spam)"
    (let [r {:id "x" :transcribed? false :error "groq down" :answered-ts nil :ts "t"}
          ds (-> [] (channel/dictation-append-failure r)
                 (channel/dictation-append-failure r)
                 (channel/dictation-append-failure r))]
      (is (= 1 (count ds)) "consecutive identical failures for the same id collapse to one")))
  (testing "a SUCCESS after a failure supersedes it in the latest-by-id projection"
    (let [ds (-> [] (channel/dictation-append-failure {:id "x" :transcribed? false :error "e" :answered-ts nil})
                 (channel/dictation-append {:id "x" :transcribed? true :transcript "hi" :enqueued? true :answered-ts nil}))
          latest (channel/dictations-latest-by-id ds)]
      (is (= 1 (count latest)) "one row per id")
      (is (true? (:transcribed? (first latest))) "the success row wins"))))

(deftest dictate-failure-appends-a-server-record
  ;; THE GAP THIS CLOSES: a failed dictation used to leave NO server record. Now the 400/500
  ;; paths append a {:transcribed? false :error ...} record to the server dictation log.
  (let [mk-req (fn [id]
                 (let [tmp (java.io.File/createTempFile "dtest" ".webm")]
                   (spit tmp "xxxxxxxxxxxxxxxx")
                   {:tmp tmp :req {:params {:audio {:tempfile tmp :filename "d.webm"} :dedup_id id}}}))]
    (reset! @#'channel/bridge-dictate-seen {:order [] :m {}})
    (reset! channel/dictations [])
    (testing "blank transcript (400) appends a failure record"
      (with-redefs [groq/transcribe-audio (constantly {:transcript "  "})]
        (let [{:keys [tmp req]} (mk-req "F1")]
          (channel/handle-bridge-dictate req)
          (let [rec (last @channel/dictations)]
            (is (= "F1" (:id rec)))
            (is (false? (:transcribed? rec)))
            (is (re-find #"no speech" (:error rec)) "captures WHY it failed"))
          (.delete tmp))))
    (testing "Groq 5xx (500) appends a failure record too"
      (reset! channel/dictations [])
      (with-redefs [groq/transcribe-audio (fn [_] (throw (ex-info "groq down" {:status 503 :body "x"})))]
        (let [{:keys [tmp req]} (mk-req "F2")]
          (channel/handle-bridge-dictate req)
          (is (false? (:transcribed? (last @channel/dictations))))
          (is (= "F2" (:id (last @channel/dictations))))
          (.delete tmp))))
    (testing "a SUCCESS appends a transcribed+enqueued record"
      (reset! channel/dictations [])
      (with-redefs [groq/transcribe-audio (constantly {:transcript "hello world"})]
        (let [{:keys [tmp req]} (mk-req "S1")]
          (channel/handle-bridge-dictate req)
          (let [rec (last @channel/dictations)]
            (is (true? (:transcribed? rec)))
            (is (true? (:enqueued? rec)))
            (is (= "hello world" (:transcript rec))))
          (.delete tmp))))
    (reset! channel/dictations [])))

;; --- #30 observability: scrubbed stats reflect traffic + live backlog, no content ---
(deftest stats-snapshot-pure
  (let [m  {:auth-fails 2 :seats {"bridge" {:msg-in 3 :reply-in 1}}}
        st {:messages {"bridge" [{:text "pending"}]} :replies {}}
        snap (channel/stats-snapshot m st)]
    (is (= 2 (:auth-fails snap)))
    (is (= 3 (get-in snap [:seats "bridge" :msg-in])))
    (is (= 1 (get-in snap [:seats "bridge" :backlog-messages])) "live backlog from state")
    (is (= 0 (get-in snap [:seats "bridge" :backlog-replies])))
    (is (not (contains? (get-in snap [:seats "bridge"]) :text)) "no message content leaks")))

(deftest stats-handlers-bump-counters
  (with-redefs [channel/token-ok? (constantly true)]
    (channel/handle-post-message (json-body-req {:seat "bridge" :from "gene" :text "hi"}))
    (channel/handle-get-messages {:params {:seat "bridge"}})
    (channel/handle-post-reply (json-body-req {:seat "bridge" :text "ok"}))
    (let [snap (channel/stats-snapshot @channel/metrics @channel/channel-state)]
      (is (= 1 (get-in snap [:seats "bridge" :msg-in])))
      (is (= 1 (get-in snap [:seats "bridge" :msg-drained])))
      (is (= 1 (get-in snap [:seats "bridge" :reply-in])))))
  (testing "401s increment auth-fails"
    (with-redefs [channel/token-ok? (constantly false)]
      (channel/handle-post-message (json-body-req {:seat "bridge" :text "x"}))
      (is (= 1 (:auth-fails (channel/stats-snapshot @channel/metrics @channel/channel-state)))))))

;; ============================================================================
;; /bridge3 — SEAT-AWARE + MULTIPLAYER (purely additive).
;; ============================================================================

(deftest bridge3-valid-seat
  (is (#'channel/valid-seat? "bridge"))
  (is (#'channel/valid-seat? "marvin-dev"))
  (is (#'channel/valid-seat? "seat_2"))
  (is (not (#'channel/valid-seat? "bad seat")) "spaces rejected")
  (is (not (#'channel/valid-seat? "a/b")) "slashes rejected (URL/env-var safe)")
  (is (not (#'channel/valid-seat? "")) "empty rejected")
  (is (not (#'channel/valid-seat? nil))))

(deftest audio-duration-telemetry-is-bounded
  (is (= 12345 (#'channel/audio-duration-ms-param
                 {:params {:audio_duration_ms "12345"}})))
  (is (= 0 (#'channel/audio-duration-ms-param
             {:params {:audio_duration_ms "0"}})))
  (is (nil? (#'channel/audio-duration-ms-param
              {:params {:audio_duration_ms "-1"}})))
  (is (nil? (#'channel/audio-duration-ms-param
              {:params {:audio_duration_ms "86400001"}})))
  (is (nil? (#'channel/audio-duration-ms-param
              {:params {:audio_duration_ms "not-a-number"}}))))

(deftest bridge3-sessions-pure
  (testing "create is idempotent by seat; rename + delete are surgical"
    (let [s0 []
          s1 (channel/b3-sessions-create s0 "marvin-dev" "Marvin Dev")
          s2 (channel/b3-sessions-create s1 "marvin-dev" "dupe")
          s3 (channel/b3-sessions-create s2 "skiff" "Skiff")
          s4 (channel/b3-sessions-rename s3 "marvin-dev" "Renamed")
          s5 (channel/b3-sessions-delete s4 "skiff")]
      (is (= 1 (count s1)))
      (is (= 1 (count s2)) "create on an existing seat is a no-op")
      (is (= 2 (count s3)))
      (is (= "Renamed" (:name (first (filter #(= "marvin-dev" (:seat %)) s4)))))
      (is (= ["marvin-dev"] (map :seat s5)) "delete drops only the named seat"))))

(deftest bridge3-reply-additions-pure
  (testing "an enqueue yields exactly the new reply; a drain yields nothing"
    (is (= [["a" {:ts "2"}]]
           (channel/reply-additions {:replies {"a" [{:ts "1"}]}}
                                    {:replies {"a" [{:ts "1"} {:ts "2"}]}})))
    (is (empty? (channel/reply-additions {:replies {"a" [{:ts "1"}]}}
                                         {:replies {"a" []}}))
        "a claim-once drain (->[]) reports no additions — never re-ingests")
    (is (empty? (channel/reply-additions {:replies {"a" [{:ts "1"}]}}
                                         {:replies {"a" [{:ts "1"}]}}))
        "a lingering, undrained reply is reported only ONCE (not on later swaps)")))

(deftest bridge3-say-is-seat-aware
  (with-redefs [channel/token-ok? (constantly true)]
    (reset! channel/bridge3-convos {})
    (reset! channel/channel-state {:messages {} :replies {}})
    (testing "POST say enqueues to messages[seat] AND appends a 'you' turn to that seat's convo"
      (is (:ok (read-json (channel/handle-bridge3-say
                            {:params {:seat "s1"}
                             :body (io/input-stream (.getBytes (json/write-str {:text "hello s1"})))}))))
      (is (= "hello s1" (-> (get-in @channel/channel-state [:messages "s1"]) first :text)))
      (is (= [{:role "you" :text "hello s1"}]
             (mapv #(select-keys % [:role :text]) (get @channel/bridge3-convos "s1"))))
      (is (nil? (get @channel/bridge3-convos "other")) "seats are isolated"))
    (testing "invalid seat is rejected (URL/env-var safety)"
      (is (= 400 (:status (channel/handle-bridge3-say
                            {:params {:seat "bad seat"}
                             :body (io/input-stream (.getBytes (json/write-str {:text "x"})))})))))))

(deftest bridge3-dictate-is-seat-aware
  (reset! channel/bridge3-convos {})
  (reset! channel/channel-state {:messages {} :replies {}})
  (reset! @#'channel/bridge3-dictate-seen {:order [] :m {}})
  (with-redefs [groq/transcribe-audio (constantly {:transcript "dictated to seat42"})]
    (let [tmp (java.io.File/createTempFile "b3test" ".webm")]
      (spit tmp "xxxxxxxxxxxxxxxx")
      (let [resp (read-json (channel/handle-bridge3-dictate
                              {:params {:audio {:tempfile tmp :filename "d.webm"}
                                        :seat "seat42" :dedup_id "D1"
                                        :audio_duration_ms "12345"}}))]
        (is (:ok resp))
        (is (= "dictated to seat42" (:transcript resp)))
        (is (= "dictated to seat42" (-> (get-in @channel/channel-state [:messages "seat42"]) first :text)))
        (is (= 12345 (-> (get-in @channel/channel-state [:messages "seat42"])
                         first :audio-duration-ms)))
        (is (= "you" (:role (first (get @channel/bridge3-convos "seat42"))))))
      (testing "retry of the SAME dedup_id returns the cached transcript (idempotent)"
        (let [resp2 (read-json (channel/handle-bridge3-dictate
                                 {:params {:audio {:tempfile tmp :filename "d.webm"}
                                           :seat "seat42" :dedup_id "D1"}}))]
          (is (:dedup resp2))
          (is (= 1 (count (get-in @channel/channel-state [:messages "seat42"]))) "no double-enqueue")))
      (.delete tmp))))

(deftest bridge3-multiplayer-reply-broadcast-via-watch
  ;; THE HEADLINE: a connector reply (POST /api/channel/replies) is INGESTED into the
  ;; per-seat bridge3 conversation by the defonce watch — with ZERO edits to handle-post-reply.
  (with-redefs [channel/token-ok? (constantly true)]
    (reset! channel/bridge3-convos {})
    (reset! channel/channel-state {:messages {} :replies {}})
    (channel/handle-post-reply (json-body-req {:seat "mp-seat" :text "answer for all tabs"}))
    (let [convo (get @channel/bridge3-convos "mp-seat")]
      (is (= 1 (count convo)) "the reply became a bridge turn for the seat")
      (is (= "bridge" (:role (first convo))))
      (is (= "answer for all tabs" (:text (first convo)))))
    (testing "the claim-once queue is ALSO still populated (/bridge v1's path untouched)"
      (is (= "answer for all tabs"
             (-> (read-json (channel/handle-get-replies {:params {:seat "mp-seat"}})) :replies first :text))))))

(deftest bridge3-convo-fragment-carries-dedup-ts
  (let [html (#'channel/bridge3-convo-fragment
               [{:role "you" :text "hi" :ts "2026-06-29T00:00:00Z"}
                {:role "bridge" :text "yo" :ts "2026-06-29T00:00:01Z"}])]
    (is (re-find #"data-role=\"bridge\"" html) "bridge turns are scannable for client dedup")
    (is (re-find #"data-ts=\"2026-06-29T00:00:01Z\"" html) "turn carries its server :ts (the dedup key)")
    (is (re-find #"class=\"msg bridge\"" html))))

(deftest code-director-latest-transcription-is-exact-and-authenticated
  (let [convos {"code-director-sse"
                [{:role "you" :text "older" :ts "2026-08-09T23:00:00Z"}
                 {:role "bridge" :text "agent reply" :ts "2026-08-09T23:00:01Z"}
                 {:role "you" :text "Newest exact transcription" :ts "2026-08-09T23:00:02Z"}]}
        latest (channel/latest-transcription convos "code-director-sse")]
    (is (= "marvin.latest-transcription.v1" (:schema latest)))
    (is (= true (:available latest)))
    (is (= "Newest exact transcription" (:text latest)))
    (is (= "2026-08-09T23:00:02Z" (:observed-at latest)))
    (is (= false (:available (channel/latest-transcription {} "code-director-sse"))))
    (with-redefs [channel/token-ok? (constantly false)]
      (is (= 401 (:status (channel/handle-code-director-latest-transcription {})))))
    (with-redefs [channel/token-ok? (constantly true)
                  channel/bridge3-convos (atom convos)]
      (is (= 200 (:status (channel/handle-code-director-latest-transcription {})))))))

(deftest bridge3-page-is-seat-aware-and-multiplayer
  (let [html (#'channel/bridge3-page-html "marvin-dev")]
    (testing "seat-aware: the page carries the seat + seat-parameterized endpoints"
      ;; seat now rides the BP boot object; say/convo/scanBridge live in THE shared player (6iu).
      (is (re-find #"var BP=\{seat:\"marvin-dev\"" html) "the seat is baked into the page (BP boot)")
      (is (re-find #"var SEAT=BP\.seat" html) "SEAT derives from BP")
      (is (re-find #"/sse/bridge3\?seat=marvin-dev" html) "SSE stream is per-seat")
      (is (re-find #"/api/bridge3/dictate\?seat=" html) "dictation carries the seat")
      (is (re-find #"__MARVIN_BRIDGE__=\{protocol:3,surface:'bridge3',submitPath:'/api/bridge3/dictate',dedupParam:'dedup_id',nativePtt:true\}" html) "bridge3 declares the versioned native PTT capture contract")
      (let [player (slurp "resources/public/js/bridge-player.js")]
        (is (re-find #"/api/bridge3/say" player) "typed send lives in the shared player")
        (is (re-find #"/api/bridge3/convo" player) "convo poll lives in the shared player")))
    (testing "multiplayer: replies come from scanning the broadcast #convo, NOT the claim-once drain"
      (is (re-find #"function scanBridge\(\)" (slurp "resources/public/js/bridge-player.js")) "scanBridge speaks NEW bridge turns")
      (is (re-find #"played" (slurp "resources/public/js/bridge-player.js")) "dedup-by-ts set so a tab never double-plays (in the shared player)")
      (is (not (re-find #"/api/bridge/replies" html))
          "bridge3 NEVER uses /bridge v1's claim-once reply path"))
    (testing "carried-over machinery (re-implemented seat-aware)"
      (is (re-find #"bridge3-dictations" html) "IndexedDB persist-and-retry, namespaced per tool")
      (is (re-find #"function getMic\(\)" html) "warm-mic")
      (is (re-find #"__MARVIN_WEB_MIC_STARTING__=true;prime\(\);unlock\(\)" html)
          "TestFlight PTT declares web-mic intent before shared audio unlock")
      (is (re-find #"navigator\.audioSession\)navigator\.audioSession\.type='playback'" html)
          "TestFlight PTT restores the loud playback route after capture")
      (is (re-find #"function releaseMic\(\)[\s\S]*__marvinWebMicReleased:true" html)
          "PTT proves its tracks were released before native Pocket takeover")
      (is (re-find #"function tickRec\(\)" html) "derived recording timer")
      (is (re-find #"seat:SEAT" html) "each queued recording remembers its seat"))
    (testing "PTT, hands-free, and config are one-tap surfaces"
      (is (re-find #"<b aria-current=page>PTT</b>" html) "marks push-to-talk active")
      (is (re-find #"href=\"/bridge4\?seat=marvin-dev\">HF</a>" html)
          "switches to hands-free without losing the seat")
      (is (re-find #"href=\"/bridge5\">Config</a>" html) "opens the channel/config picker")
      (is (re-find #"Talk to Bridge \(v3\)</h1>.*<nav id=surface-nav" html)
          "renders the mode control below the title")
      (is (re-find #"min-height:48px" html) "gives every mode a phone-sized touch target"))))

(deftest bridge3-sessions-page-shows-launch-command
  (reset! channel/bridge3-sessions [{:seat "bridge" :name "Bridge" :created-ts "2026-06-29T00:00:00Z"}])
  (let [page (#'channel/bridge3-sessions-page-html)]
    (testing "create/rename/delete controls"
      (is (re-find #"name=action value=create" page) "create form")
      (is (re-find #"name=action value=rename" page) "rename form")
      (is (re-find #"name=action value=delete" page) "delete form"))
    (testing "the one-line launch command (sources secrets from env file; SEAT overridden)"
      (is (re-find #"SEAT=bridge" page))
      (is (re-find #"dangerously-load-development-channels server:marvin-channel" page))
      (is (re-find #"marvin-channel.env" page) "secrets sourced from the env file, never embedded")
      (is (re-find #"cpLaunch" page) "browser-native copy of the launch command"))
    (testing "health is surfaced per seat"
      (is (re-find #"backlog" page)))))

(deftest bridge3-session-health-derivation
  (let [m  {:seats {"bridge" {:last-reply-ts (str (java.time.Instant/now))
                              :reply-in 5 :msg-in 3}}}
        cs {:messages {"bridge" [{:text "q"}]} :replies {"bridge" []}}
        h  (channel/session-health m cs "bridge")]
    (is (:live? h) "recent reply -> live")
    (is (= 1 (:backlog-messages h)))
    (is (= 0 (:backlog-replies h)))
    (let [stale (channel/session-health {:seats {"old" {:last-reply-ts "2020-01-01T00:00:00Z"}}}
                                        {:messages {} :replies {}} "old")]
      (is (not (:live? stale)) "activity older than 10 min -> not live"))))

(deftest bridge3-sessions-mutate-redirects
  (reset! channel/bridge3-sessions [])
  (let [resp (channel/handle-bridge3-sessions-mutate {:params {:action "create" :seat "newseat" :name "New"}})]
    (is (= 303 (:status resp)) "POST-redirect-GET so a reload shows the truth")
    (is (= "/bridge3/sessions" (get-in resp [:headers "Location"])))
    (is (= "newseat" (:seat (first @channel/bridge3-sessions))))
    (is (= "New" (:name (first @channel/bridge3-sessions)))))
  ;; delete is TWO-STEP now (6gg: server-rendered confirm, no confirm() modal):
  ;; 'delete' arms the confirmation; 'confirm-delete' actually removes.
  (channel/handle-bridge3-sessions-mutate {:params {:action "delete" :seat "newseat"}})
  (is (= "newseat" (:seat (first @channel/bridge3-sessions))) "step 1 only ARMS the confirm")
  (channel/handle-bridge3-sessions-mutate {:params {:action "confirm-delete" :seat "newseat"}})
  (is (empty? @channel/bridge3-sessions) "confirm-delete removes it"))

;; ====================================================================================
;; /voice-lab — hands-free VAD prototype (purely additive).
;; ====================================================================================

(deftest voice-lab-page-ships-vad-loop-and-tunable-params
  (let [html (#'channel/voice-lab-page-html)]
    (testing "one-tap iOS unlock: getUserMedia + AudioContext + audio unlock in the gesture"
      (is (re-find #"startBtn.onclick" html))
      (is (re-find #"getUserMedia" html))
      (is (re-find #"AudioContext\|\|window.webkitAudioContext" html))
      (is (re-find #"data:audio/wav;base64" html) "silent-wav audio unlock"))
    (testing "VAD: energy(RMS) analyser + min-speech start + silence end + blip floor"
      (is (re-find #"createAnalyser" html))
      (is (re-find #"getFloatTimeDomainData" html))
      (is (re-find #"P.minspeech" html) "speech-start needs sustained voice")
      (is (re-find #"P.silence" html) "speech-end on sustained quiet")
      (is (re-find #"recDur<P.floor" html) "blip guard discards too-short utterances")
      (is (re-find #"MediaRecorder" html) "fresh segment on the warm stream"))
    (testing "URL-tunable thresholds with documented defaults, shown on screen"
      (is (re-find #"num\('energy',0.015\)" html))
      (is (re-find #"num\('silence',900\)" html))
      (is (re-find #"num\('minspeech',300\)" html))
      (is (re-find #"num\('floor',400\)" html))
      (is (re-find #"qp.get\('seat'\)\|\|'bridge'" html) "seat default")
      (is (re-find #"ACTIVE: energy=" html) "active values rendered on the page"))
    (testing "live feedback: mic-level meter + state readout"
      (is (re-find #"meterfill" html))
      (is (re-find #"function setState" html))
      (is (re-find #"Listening" html))
      (is (re-find #"recording" html))
      (is (re-find #"Reading reply" html)))
    (testing "barge-in: pause the reply when real speech starts over it"
      (is (re-find #"if\(playing\)\{try\{audio.pause" html)))
    (testing "send + reply reuse the seat mailbox + read-only TTS"
      (is (re-find #"/api/voice-lab/dictate\?seat=" html))
      (is (re-find #"/api/voice-lab/replies\?seat=" html))
      (is (re-find #"/api/bridge/tts\?text=" html)))
    (testing "iOS: release mic + audio session on pagehide"
      (is (re-find #"pagehide" html)))))

(deftest voice-lab-dictate-is-seat-aware
  (with-redefs [groq/transcribe-audio (fn [_] {:transcript "hello lab"})]
    (let [tmp (java.io.File/createTempFile "vl-test" ".webm")
          req {:params {:seat "lab" :audio {:tempfile tmp :filename "d.webm"}}}
          resp (channel/handle-voice-lab-dictate req)
          body (read-json resp)]
      (is (= 200 (:status resp)))
      (is (= "hello lab" (:transcript body)))
      (is (= 1 (count (get-in @channel/channel-state [:messages "lab"]))) "enqueued to the seat")
      (is (empty? (get-in @channel/channel-state [:messages "bridge"])) "did NOT touch the bridge seat")
      (.delete tmp))))

(deftest voice-lab-dictate-rejects-empty-and-bad-seat
  (testing "empty transcript -> 400 permanent (no enqueue, client marks failed)"
    (with-redefs [groq/transcribe-audio (fn [_] {:transcript "  "})]
      (let [tmp (java.io.File/createTempFile "vl-test" ".webm")
            resp (channel/handle-voice-lab-dictate {:params {:seat "lab" :audio {:tempfile tmp :filename "d.webm"}}})]
        (is (= 400 (:status resp)))
        (is (empty? (get-in @channel/channel-state [:messages "lab"])))
        (.delete tmp))))
  (testing "junk seat -> 400"
    (is (= 400 (:status (channel/handle-voice-lab-dictate {:params {:seat "../etc" :audio {:tempfile "x"}}}))))))

(deftest voice-lab-replies-claim-once-and-seat-aware
  (swap! channel/channel-state channel/enqueue :replies "lab" {:text "from claude" :ts "t"})
  (let [r1 (read-json (channel/handle-voice-lab-replies {:params {:seat "lab"}}))
        r2 (read-json (channel/handle-voice-lab-replies {:params {:seat "lab"}}))]
    (is (= 1 (count (:replies r1))) "drains the seat's replies")
    (is (= "from claude" (:text (first (:replies r1)))))
    (is (empty? (:replies r2)) "claim-once: a second drain is empty"))
  (is (= 400 (:status (channel/handle-voice-lab-replies {:params {:seat "bad/seat"}})))))

(deftest bridge3-untouched-by-voice-lab
  ;; THE SACRED INVARIANT: voice-lab is purely additive. bridge3 may not reference any
  ;; /voice-lab endpoint or gain a voice-lab dependency. (Byte-for-byte: the generated
  ;; page is unchanged — verified out-of-band by rendering before/after, modulo the ?v=
  ;; cache-bust timestamp.) The bridge2 half of this guard went with bridge2 on
  ;; 2026-07-26 (deleted: zero traffic in 7 days).
  (let [b3 (#'channel/bridge3-page-html "bridge")]
    (is (not (re-find #"voice-lab" b3)) "bridge3 has NO voice-lab reference")
    (is (re-find #"Talk to Bridge \(v3\)" b3) "bridge3 identity intact")))

;; ====================================================================================
;; /voice-lab KEYWORD mode — start-on-speech, end-on-keyword-plus-pause (purely additive).
;; ====================================================================================

(deftest voice-lab-keyword-finalize-last-word-sentinel
  (testing "end-word as the LAST word => ended, sentinel stripped"
    (is (= {:ended true :text "tell me the time"}
           (channel/voice-lab-keyword-finalize "tell me the time over" "over")))
    (is (= {:ended true :text "send the message"}
           (channel/voice-lab-keyword-finalize "send the message OVER" "over")) "case-insensitive")
    (is (= {:ended true :text "what time is it"}
           (channel/voice-lab-keyword-finalize "what time is it, over." "over")) "trailing punctuation stripped both sides"))
  (testing "end-word MID-sentence (not the last word) => keep going, transcript unchanged"
    (is (= {:ended false :text "stand over there"}
           (channel/voice-lab-keyword-finalize "stand over there" "over")))
    (is (= {:ended false :text "the overton window matters"}
           (channel/voice-lab-keyword-finalize "the overton window matters" "over")) "'Overton' is never the last word here"))
  (testing "'Overton' as the embedded word but a REAL end-word still finalizes correctly"
    (is (= {:ended true :text "the overton window"}
           (channel/voice-lab-keyword-finalize "the overton window over" "over"))))
  (testing "degenerate inputs don't finalize an empty send"
    (is (= false (:ended (channel/voice-lab-keyword-finalize "over" "over"))) "lone sentinel => nothing to send")
    (is (= false (:ended (channel/voice-lab-keyword-finalize "" "over"))) "blank transcript")
    (is (= false (:ended (channel/voice-lab-keyword-finalize "hello there" ""))) "blank end-word never matches"))
  (testing "alternate end-words are honored (over/out/done/send)"
    (is (= {:ended true :text "roger that"} (channel/voice-lab-keyword-finalize "roger that out" "out")))
    (is (= {:ended false :text "we are out of time"} (channel/voice-lab-keyword-finalize "we are out of time" "out")))))

(deftest voice-lab-keyword-check-finalizes-and-enqueues
  (testing "last word == end-word => enqueue the STRIPPED text to the seat"
    (with-redefs [groq/transcribe-audio (fn [_] {:transcript "what is the weather over"})]
      (let [tmp (java.io.File/createTempFile "vlk-test" ".webm")
            resp (channel/handle-voice-lab-keyword-check
                   {:params {:seat "lab" :end "over" :audio {:tempfile tmp :filename "k.webm"}}})
            body (read-json resp)]
        (is (= 200 (:status resp)))
        (is (true? (:ended body)))
        (is (= "what is the weather" (:transcript body)) "sentinel stripped before send")
        (is (= 1 (count (get-in @channel/channel-state [:messages "lab"]))) "enqueued to the seat")
        (is (= "what is the weather" (:text (first (get-in @channel/channel-state [:messages "lab"])))))
        (.delete tmp)))))

(deftest voice-lab-keyword-check-keeps-going-without-trailing-sentinel
  (testing "end-word mid-sentence WITHOUT being the last word => NOT ended, NOTHING enqueued"
    (with-redefs [groq/transcribe-audio (fn [_] {:transcript "stand over there"})]
      (let [tmp (java.io.File/createTempFile "vlk-test" ".webm")
            resp (channel/handle-voice-lab-keyword-check
                   {:params {:seat "lab" :end "over" :audio {:tempfile tmp :filename "k.webm"}}})
            body (read-json resp)]
        (is (= 200 (:status resp)))
        (is (false? (:ended body)))
        (is (= "stand over there" (:transcript body)) "raw transcript returned, unmodified")
        (is (empty? (get-in @channel/channel-state [:messages "lab"])) "did NOT send mid-thought")
        (.delete tmp)))))

(deftest voice-lab-keyword-check-blank-and-bad-seat
  (testing "blank STT (road-noise pause) => ended=false, no enqueue, NOT an error"
    (with-redefs [groq/transcribe-audio (fn [_] {:transcript "   "})]
      (let [tmp (java.io.File/createTempFile "vlk-test" ".webm")
            resp (channel/handle-voice-lab-keyword-check
                   {:params {:seat "lab" :end "over" :audio {:tempfile tmp :filename "k.webm"}}})
            body (read-json resp)]
        (is (= 200 (:status resp)))
        (is (false? (:ended body)))
        (is (empty? (get-in @channel/channel-state [:messages "lab"])))
        (.delete tmp))))
  (testing "junk seat => 400"
    (is (= 400 (:status (channel/handle-voice-lab-keyword-check {:params {:seat "../etc" :audio {:tempfile "x"}}}))))))

(deftest voice-lab-page-ships-keyword-mode-wiring
  (let [html (#'channel/voice-lab-page-html)]
    (testing "keyword is the DEFAULT mode; silence/vad reach the old behavior"
      (is (re-find #"qp.get\('mode'\)\|\|'keyword'" html) "default mode = keyword")
      (is (re-find #"mreq==='silence'\|\|mreq==='vad'" html) "silence|vad aliases reach old behavior")
      (is (re-find #"var KW=" html)))
    (testing "keyword params with documented defaults"
      (is (re-find #"qp.get\('end'\)\|\|'over'" html) "end-word default")
      (is (re-find #"num\('endpause',1500\)" html) "endpause default"))
    (testing "BOTH mode loops ship (additive — silence VAD intact)"
      (is (re-find #"function loopKw" html))
      (is (re-find #"function loopSilence" html))
      (is (re-find #"if\(KW\)\{loopKw" html) "loop dispatches on mode"))
    (testing "keyword mechanic: record through pauses, transcribe-on-pause, last-word check"
      (is (re-find #"P.endpause" html) "pause threshold drives the check")
      (is (re-find #"/api/voice-lab/keyword-check\?seat=" html) "pause-check endpoint")
      (is (re-find #"&end=" html) "end-word passed to the server check")
      (is (re-find #"mr.start\(250\)" html) "continuous recorder accumulates through pauses"))
    (testing "state readout + the 'say over to send' UX hint"
      (is (re-find #"Recording \(say" html))
      (is (re-find #"Checking" html))
      (is (re-find #"MODE: " html) "active mode shown on screen"))))

;; ====================================================================================
;; /bridge4 — bridge3's seat-aware MULTIPLAYER backend/UI + the /voice-lab HANDS-FREE
;; input engine instead of push-to-talk (PURELY ADDITIVE; reuses bridge3 endpoints/state,
;; adds only /api/bridge4/keyword-check). bridge3/voice-lab stay sacred.
;; ====================================================================================

(deftest bridge4-page-is-hands-free-multiplayer-and-seat-aware
  (let [html (#'channel/bridge4-page-html "marvin-dev")]
    (testing "identity + seat-aware multiplayer wiring REUSES bridge3's endpoints"
      (is (re-find #"Talk to Bridge \(v4\)" html) "bridge4 identity")
      (is (re-find #"/sse/bridge3\?seat=marvin-dev" html) "reuses bridge3 per-seat SSE")
      ;; convo poll / typed-say / scanBridge moved into THE shared player (6iu refactor):
      ;; the page boots it; the endpoints live once in resources/public/js/bridge-player.js.
      (is (re-find #"/js/bridge-player\.js" html) "loads THE shared player")
      (is (re-find #"BridgePlayerBoot\(\)" html) "boots the shared player")
      (let [player (slurp "resources/public/js/bridge-player.js")]
        (is (re-find #"/api/bridge3/convo" player) "shared player polls bridge3 convo")
        (is (re-find #"/api/bridge3/say" player) "shared player sends typed text via bridge3 say")
        (is (re-find #"function scanBridge" player) "multiplayer broadcast reply audio (not claim-once)")
        ;; TTS graceful degradation (2026-07-07, the ElevenLabs quota outage): never silent
        (is (re-find #"function speakFallback" player) "phone-voice fallback when TTS is down")
        (is (re-find #"function startFallback\(generation, reason\)" player) "one generation owns either primary TTS or phone fallback")
        (is (re-find #"err\.name === 'AbortError'" player)
            "interrupted playback is distinguished from a TTS outage")
        (is (re-find #"if \(aborted\) \{[\s\S]*play-abort-release[\s\S]*notifyNativePlayback\(false\)" player) "AbortError releases native playback without starting the phone voice")
        (is (re-find #"audio\.removeAttribute\('src'\)" player)
            "fallback severs the primary source before speaking")
        (is (re-find #"function ttsBanner" player) "loud voice-down banner (Gene: obvious in the UI)")
        (is (re-find #"playing = true;\n    notifyNativePlayback\(true\);\n    setStatus" player)
            "fallback holds the playing mutex and directly tells native playback started")
        (is (re-find #"recordingActive\(\) \|\| userPaused\)[^\n]*stopFallbackSpeech" player)
            "watchdog cancels phone voice when recording starts — it can't be dictated into a message"))
      (is (re-find #"href=\"/bridge3-new\?seat=marvin-dev\">PTT</a>" html)
          "switches to push-to-talk (the REDUCER page — Gene's C1 adoption ratchet, 2026-07-27) without losing the seat")
      (is (re-find #"<b aria-current=page>HF</b>" html) "marks hands-free active")
      (is (re-find #"href=\"/bridge5\">Config</a>" html)
          "links the canonical channel/config picker")
      (is (re-find #"Talk to Bridge \(v4\) — hands-free</h1>.*<nav id=surface-nav" html)
          "renders the mode control below the title")
      (is (not (re-find #"/api/bridge/replies" html)) "bridge4 NEVER uses /bridge v1's claim-once reply path")
      (is (not (re-find #"/api/voice-lab/replies" html)) "bridge4 does not poll voice-lab claim-once replies"))
    (testing "INPUT is the hands-free engine (VAD + keyword), NOT push-to-talk"
      (is (re-find #"function loopKw" html))
      (is (re-find #"function loopSilence" html))
      ;; 2026-07-07: energy detection is EVENT-PUMPED by the audio clock (ScriptProcessorNode
      ;; onaudioprocess — still MAIN-thread, but iOS keeps delivering audio events while the mic
      ;; session is live), not a setTimeout loop — iOS throttles JS timers ~20x when the screen
      ;; is off (proven in prod: frames +3/3s), which caused the recording-start lag + deafness.
      (is (re-find #"createScriptProcessor" html) "audio-clock-pumped energy detection")
      (is (re-find #"onaudioprocess" html) "detection driven by audio events, not timers")
      (is (re-find #"function computeAndTick" html) "the VAD tick, driven by onaudioprocess")
      (is (re-find #"function startEngine" html) "mic -> ScriptProcessor -> muted gain -> destination")
      (is (re-find #"muteNode.gain.value=0" html) "the ScriptProcessor sink is muted (no mic echo)")
      (is (re-find #"function startKeepAlive" html) "slow keep-alive resumes a suspended context")
      (is (re-find #"function startHandsFree\(\)[\s\S]*if\(nativeHost\(\)\)[\s\S]*postNativeHf\('hf.start'\)" html)
          "TestFlight HF selects the native CaptureEngine without opening a web mic")
      (is (re-find #"function sendUtterance\(blob\)\{if\(window\.__MARVIN_NATIVE_CAPTURE__\)" html)
          "a web recorder finishing during takeover cannot submit a duplicate turn")
      (is (re-find #"function nativeHost\(\)" html)
          "host capability selects native versus browser execution")
      (is (re-find #"capturePolicyVersion:2,nativeHf:true" html)
          "native HF policy support is versioned and declared")
      (is (re-find #"version:2,mode:mode" html)
          "native receives the same selected keyword/silence policy as the web executor")
      (is (re-find #"endWord:P.end,cancelWord:P.cancel,endPauseMs:P.endpause" html)
          "explicit over/cancel semantics cross the versioned bridge")
      (is (not (re-find #"over.*optional" html))
          "native UI never weakens the explicit OVER contract")
      (is (re-find #"id=rectimer role=status" html)
          "native recording duration has a dedicated visible status")
      (is (re-find #"recordingElapsedMs" html)
          "native duration is anchored to Swift's audio-clock receipt")
      (is (re-find #"TF .*native .*web " html)
          "field UI identifies TestFlight, native, and hosted builds")
      (is (re-find #"uploadingCount" html)
          "upload work is visible separately from the capture phase")
      (is (not (re-find #"setTimeout\(loop,50\)" html)) "NO throttleable setTimeout VAD loop (the throttle-deafness regression)")
      (is (not (re-find #"getFloatTimeDomainData" html)) "no analyser-poll RMS (replaced by the event pump)")
      ;; 2026-07-07 CONTINUOUS CAPTURE (Gene: "still 5s to actually start recording... I started
      ;; counting at 1"): the recorder idle-cycles from hands-free-on; speech onset only flips
      ;; the flag — zero spin-up lag, 0-3s pre-roll carries the previously-clipped opening words.
      (is (re-find #"function startIdleCycle" html) "recorder runs continuously (idle cycling)")
      (is (re-find #"function pumpRecorder" html) "the pump is the single owner of recorder liveness")
      (is (re-find #"function recordingActive\(\)\{return nativeHost\(\)\?" html)
          "playback mutex observes native recording state in TestFlight and the web flag in browsers")
      (is (not (re-find #"function startRecKw\(now\)\{[^}]*new MediaRecorder" html))
          "onset does NOT create a recorder (that spin-up was the start lag)")
      ;; runaway guard, ported from voice-lab (was MISSING here — the Groq-bill fuse; the lower
      ;; calibration cap raises false-trigger odds, so bridge4 needs the cut too)
      (is (re-find #"num\('quietclose',20000\)" html))
      (is (re-find #"num\('maxutter',120000\)" html))
      (is (re-find #"function kwRunaway" html) "runaway cut flushes substantive audio, never drops")
      (is (re-find #"function lockScreen" html) "Wake Lock for hands-free")
      (is (re-find #"wakeLock.request\('screen'\)" html))
      (is (not (re-find #"id=rec>" html)) "no push-to-talk Tap-to-talk button")
      (is (not (re-find #"bridge3-dictations" html)) "no push-to-talk IndexedDB queue"))
    (testing "keyword is the DEFAULT; silence reachable; tunables read from URL"
      (is (re-find #"qp.get\('mode'\)\|\|'keyword'" html) "default mode = keyword")
      (is (re-find #"qp.get\('end'\)\|\|'over'" html) "end-word default = over")
      (is (re-find #"num\('endpause',1500\)" html))
      (is (re-find #"num\('energy',0.015\)" html))
      (is (re-find #"num\('minspeech',300\)" html))
      (is (re-find #"num\('floor',400\)" html))
      (is (re-find #"num\('silence',900\)" html)))
    (testing "the input posts to bridge3 dictate (silence) + bridge4 keyword-check (keyword)"
      (is (re-find #"/api/bridge3/dictate\?seat='\+encodeURIComponent\(P.seat\)" html) "silence-mode utterance -> bridge3 seat dictate")
      (is (re-find #"/api/bridge4/keyword-check\?seat='\+encodeURIComponent\(P.seat\)" html) "keyword check -> bridge4 sibling")
      (is (re-find #"__MARVIN_BRIDGE__=\{protocol:2,surface:'bridge4',build:.*capturePolicyVersion:2,nativeHf:true,nativeReplyOfferVersion:1,nativeBargeInVersion:1\}" html)
          "bridge4 declares native HF, background reply offers, and hosted-policy-owned native barge-in")
      (is (re-find #"barge:false" html)
          "server-resolved native barge-in remains off by default")
      (is (re-find #"duplex:\{version:1,enabled:P\.barge,bargeHoldMs:P\.bargehold\}" html)
          "the hosted CaptureEngine policy owns the native barge-in gate and timing")
      (is (re-find #"&end='\+encodeURIComponent\(P.end\)" html) "end-word passed to the keyword check"))
    (testing "each finalized browser capture carries its active audio duration"
      (is (re-find #"fd\.append\('audio_duration_ms',String\(Math\.max\(0,Math\.round\(now-recStartAt\)\)\)\)" html)
          "rolling keyword checks report the whole utterance so the accepted check is exact")
      (is (re-find #"fd\.append\('audio_duration_ms',String\(Math\.max\(0,Math\.round\(recDur\)\)\)\)" html)
          "explicit OVER and silence finalization report their measured duration"))
    (testing "lightweight crash telemetry IS wired for bridge4's maiden real-world runs"
      ;; Reuses the proven /api/voice-lab/clientlog writer + /diag readback ring, tagged
      ;; page:'bridge4'. Helper + global handlers + boot + heartbeat only (engine untouched).
      ;; The full per-step DIAG instrumentation stays voice-lab-only. Strip with the voice-lab
      ;; diag once bridge4 is proven on hardware (bd: remove-DIAG-telemetry).
      ;; telemetry moved into THE shared player (clog + clientlog writer live there);
      ;; the page tags itself via the BP boot object and keeps its hands-free heartbeat.
      (is (re-find #"clog\(" html) "page still emits beacons via the player's clog")
      (let [player (slurp "resources/public/js/bridge-player.js")]
        (is (re-find #"/api/voice-lab/clientlog" player) "shared player carries the clientlog writer")
        (is (re-find #"function clog" player) "clog helper lives in the shared player"))
      (is (re-find #"page:'bridge4'" html) "BP boot tags beacons page:bridge4")
      (is (re-find #"k:'hb'" html) "hands-free heartbeat beacon (frozen f = freeze signature)"))))

(deftest code-director-desk-mode-is-explicit-and-bridge4-remains-hands-free
  (let [director-body (:body (channel/handle-code-director-page
                               {:params {:seat "code-director-sse" :mode "desk"}}))
        running-body (:body (channel/handle-code-director-page
                              {:params {:seat "code-director-sse" :mode "running"}}))
        bridge4-body (:body (channel/handle-bridge4-page
                              {:params {:seat "bridge4-regression"}}))]
    (is (.contains ^String director-body "window.__MARVIN_CODE_DIRECTOR__=true"))
    (is (.contains ^String director-body "Start desk hands-free"))
    (is (not (.contains ^String director-body "id=deskarm")))
    (is (.contains ^String director-body "now<window.__MARVIN_DESK_INPUT_UNTIL__"))
    (is (.contains ^String (slurp "resources/public/js/code-director-desk.js")
                   "Sent to Director — awaiting delivery receipt"))
    (is (.contains ^String (slurp "resources/public/js/code-director-desk.js")
                   "Delivered to Director"))
    (is (.contains ^String (slurp "resources/public/js/code-director-desk.js")
                   "played.add(receipt.playbackKey)"))
    (is (.contains ^String (slurp "resources/public/js/code-director-desk.js")
                   "beep(3, 1175)"))
    (is (.contains ^String (slurp "resources/public/js/code-director-desk.js")
                   "Director receipt pending after 8s"))
    (is (.contains ^String (slurp "resources/public/js/code-director-desk.js")
                   "calEnabled = false"))
    (is (not (.contains ^String (slurp "resources/public/js/code-director-desk.js")
                        "P.minspeech"))
        "Desk input-noise handling must not make speech onset less sensitive than proven Running mode")
    (is (.contains ^String (slurp "resources/public/js/code-director-desk.js")
                   "director-startup-click-fenced"))
    (is (.contains ^String (slurp "resources/public/js/code-director-desk.js")
                   "director-audio-recovery"))
    (is (.contains ^String (slurp "resources/public/js/code-director-desk.js")
                   "pagehide\", cancelHealthRecovery")
        "automatic mic recovery timer must be canceled when the page lifecycle ends")
    (is (.contains ^String director-body "id=overutterance"))
    (is (.contains ^String director-body "id=cancelutterance"))
    (is (.contains ^String (slurp "resources/public/js/code-director-desk.js")
                   "applyUtteranceAction(\"over\")"))
    (is (.contains ^String (slurp "resources/public/js/code-director-desk.js")
                   "applyUtteranceAction(\"clear\")"))
    (is (.contains ^String (slurp "resources/public/js/code-director-desk.js")
                   "event.code === \"KeyM\" || event.code === \"KeyT\""))
    (is (.contains ^String (slurp "resources/public/js/code-director-desk.js")
                   "applyUtteranceAction(action)"))
    (is (.contains ^String (slurp "resources/public/js/code-director-desk.js")
                   "startHandsFree()"))
    (is (.contains ^String (slurp "resources/public/js/code-director-desk.js")
                   "Microphone stopped — press Alt-T or tap Start hands-free"))
    (is (.contains ^String running-body "<strong>Running mode</strong>"))
    (is (not (.contains ^String running-body "window.__MARVIN_CODE_DIRECTOR__=true")))
    (is (not (.contains ^String running-body "/js/code-director-desk.js")))
    (is (.contains ^String running-body "keyToggleId:'start'"))
    (is (not (.contains ^String bridge4-body "window.__MARVIN_CODE_DIRECTOR__=true")))
    (is (not (.contains ^String bridge4-body "/js/code-director-desk.js")))
    (is (not (.contains ^String bridge4-body "id=deskarm")))
    (is (not (.contains ^String bridge4-body "Code Director Interface")))
    (is (.contains ^String bridge4-body "Talk to Bridge (v4)"))
    (is (.contains ^String bridge4-body "keyToggleId:'start'"))))

(deftest bridge4-keyword-check-finalizes-and-broadcasts
  (reset! channel/bridge3-convos {})
  (testing "last word == end-word => enqueue STRIPPED text + append a 'you' turn to bridge3 convo"
    (with-redefs [groq/transcribe-audio (fn [_] {:transcript "what is the weather over"})]
      (let [tmp (java.io.File/createTempFile "b4k-test" ".webm")
            resp (channel/handle-bridge4-keyword-check
                   {:params {:seat "b4seat" :end "over" :audio_duration_ms "67890"
                             :audio {:tempfile tmp :filename "k.webm"}}})
            body (read-json resp)]
        (is (= 200 (:status resp)))
        (is (true? (:ended body)))
        (is (= "what is the weather" (:transcript body)) "sentinel stripped before send")
        (is (= 1 (count (get-in @channel/channel-state [:messages "b4seat"]))) "enqueued to the seat mailbox")
        (is (= "what is the weather" (:text (first (get-in @channel/channel-state [:messages "b4seat"])))))
        (is (= 67890 (:audio-duration-ms
                       (first (get-in @channel/channel-state [:messages "b4seat"])))))
        (let [convo (get @channel/bridge3-convos "b4seat")]
          (is (= 1 (count convo)) "a 'you' turn appended to the bridge3 conversation (so it broadcasts)")
          (is (= "you" (:role (first convo))))
          (is (= "what is the weather" (:text (first convo)))))
        (.delete tmp)))))

(deftest bridge4-keyword-check-deduplicates-a-lost-final-response
  (reset! channel/bridge3-convos {})
  (reset! channel/bridge4-keyword-seen {:order [] :m {}})
  (let [calls (atom 0)
        tmp   (java.io.File/createTempFile "b4k-dedup" ".wav")
        req   {:params {:seat "b4dedupseat"
                        :end "over"
                        :dedup_id "logical-utterance-42"
                        :audio {:tempfile tmp :filename "capture.wav"}}}]
    (with-redefs [groq/transcribe-audio (fn [_]
                                          (swap! calls inc)
                                          {:transcript "send exactly once over"})]
      (let [first-body (read-json (channel/handle-bridge4-keyword-check req))
            retry-body (read-json (channel/handle-bridge4-keyword-check req))]
        (is (true? (:ended first-body)))
        (is (true? (:ended retry-body)))
        (is (true? (:dedup retry-body)) "same logical ID returns the final receipt")
        (is (= 1 @calls) "lost-response retry does not transcribe twice")
        (is (= 1 (count (get-in @channel/channel-state [:messages "b4dedupseat"])))
            "lost-response retry does not enqueue twice")
        (is (= 1 (count (get @channel/bridge3-convos "b4dedupseat")))
            "lost-response retry does not duplicate the visible turn")))
    (.delete tmp)))

(deftest bridge4-keyword-check-keeps-going-mid-thought-and-reuses-pure-fn
  (reset! channel/bridge3-convos {})
  (testing "end-word NOT the last word => NOT ended, NOTHING enqueued, no convo turn"
    (with-redefs [groq/transcribe-audio (fn [_] {:transcript "stand over there"})]
      (let [tmp (java.io.File/createTempFile "b4k-test" ".webm")
            resp (channel/handle-bridge4-keyword-check
                   {:params {:seat "b4seat" :end "over" :audio {:tempfile tmp :filename "k.webm"}}})
            body (read-json resp)]
        (is (= 200 (:status resp)))
        (is (false? (:ended body)))
        (is (= "stand over there" (:transcript body)) "raw transcript returned, unmodified")
        (is (empty? (get-in @channel/channel-state [:messages "b4seat"])) "did NOT send mid-thought")
        (is (nil? (get @channel/bridge3-convos "b4seat")) "no premature 'you' turn")
        (.delete tmp))))
  (testing "the handler REUSES the pure voice-lab-keyword-finalize (same last-word rule)"
    (is (= {:ended true :text "roger that"} (channel/voice-lab-keyword-finalize "roger that out" "out")))
    (is (= false (:ended (channel/voice-lab-keyword-finalize "over" "over"))) "lone sentinel never sends")))

(deftest bridge4-keyword-check-blank-and-bad-seat
  (testing "blank STT (road-noise pause) => ended=false, no enqueue, NOT an error"
    (with-redefs [groq/transcribe-audio (fn [_] {:transcript "   "})]
      (let [tmp (java.io.File/createTempFile "b4k-test" ".webm")
            resp (channel/handle-bridge4-keyword-check
                   {:params {:seat "b4seat" :end "over" :audio {:tempfile tmp :filename "k.webm"}}})
            body (read-json resp)]
        (is (= 200 (:status resp)))
        (is (false? (:ended body)))
        (is (empty? (get-in @channel/channel-state [:messages "b4seat"])))
        (.delete tmp))))
  (testing "junk seat => 400"
    (is (= 400 (:status (channel/handle-bridge4-keyword-check {:params {:seat "../etc" :audio {:tempfile "x"}}}))))))

(deftest bridge3-and-voice-lab-untouched-by-bridge4
  ;; THE SACRED INVARIANT: bridge4 is purely additive. Neither bridge3 nor voice-lab may
  ;; reference any /bridge4 endpoint or gain a bridge4 dependency. (Byte-for-byte equality of
  ;; the rendered pages before/after the bridge4 commit was verified out-of-band by
  ;; rendering + normalized diff, modulo the ?v= cache-bust + the wall-clock 'ago' timestamp.)
  ;; The bridge2 assertions went with bridge2 on 2026-07-26 (zero traffic in 7 days).
  (let [b3 (#'channel/bridge3-page-html "bridge")
        vl (#'channel/voice-lab-page-html)]
    (is (= 1 (count (re-seq #"/bridge4\?seat=" b3)))
        "bridge3 references bridge4 exactly once: the explicit HF navigation link")
    (is (not (re-find #"bridge4" vl)) "voice-lab has NO bridge4 reference")
    (is (re-find #"Talk to Bridge \(v3\)" b3) "bridge3 identity intact")
    (is (re-find #"Voice Lab \(hands-free\)" vl) "voice-lab identity intact")))

(deftest bridge4-barge-in-threshold-is-capped
  ;; ir5-A (Gene, field-confirmed twice): the while-playing threshold was P.energy*2, which after
  ;; ambient calibration (cap 0.05) = 0.10 = the meter's MAXIMUM — an interrupting human voice can
  ;; never cross it, so barge-in went deaf. The page must use a CAPPED named fn for the playing
  ;; threshold, and the cap must sit at a human-reachable 0.06.
  (let [html (#'channel/bridge4-page-html "bridge")]
    (is (re-find #"function bargeTh\(\)\{return Math\.min\(P\.energy\*2,0\.06\);\}" html)
        "capped barge-in threshold fn present")
    (is (re-find #"function onsetReady\(e,now\).*if\(e>bargeTh\(\)\)" html)
        "the shared onset gate used by both VAD loops caps barge-in while playing")
    (is (not (re-find #"bridge4[\s\S]{0,40000}playing\?P\.energy\*2" (subs html (.indexOf html "loopKw"))))
        "no uncapped *2 threshold remains in bridge4's loops")))

;; ---------------------------------------------------------------------------
;; S5c delta-6 — THE REDUCER INGRESS AT THE REPLY SEAM
;; ---------------------------------------------------------------------------
;;
;; The end-to-end claim of Gene's 2026-07-27 field bug, tested at the seam it
;; broke at: a connector reply enqueued for a seat becomes a `:fact/reply-arrived`
;; in that seat's LIVE reducer session — with no page, no poll and no client in
;; the loop. `/voice-lab` polled `/api/bridge3/replies` and posted the fact from
;; the browser; `/bridge3-new` has no poll by design, so the reply had no way in.

(deftest a-connector-reply-enters-the-reducer-session-server-side
  (let [seat "chtest"]
    (try
      (reducer-session/reset-session! seat)
      ;; the surface is live and the user has expressed intent (P8's seed window
      ;; is closed), which is the state Gene's field turn was actually in
      (let [sid (:session/id (reducer-session/session-for seat))
            env (fn [kind payload]
                  (merge {:protocol "marvin.voice/1" :kind kind :session/id sid
                          :client/id "web:t" :event/id (str (gensym "e"))
                          :causation/id "boot" :occurred-at-ms 1 :sequence 1}
                         payload))]
        (reducer-session/apply-events!
          seat [(protocol/fact->event (env :command/ptt-start {:client/id "web:t"}))])
        (reducer-session/apply-events!
          seat [(protocol/fact->event (env :command/ptt-stop {}))])
        ;; THE SEAM: exactly what `handle-post-reply` does with a connector reply
        (swap! channel/channel-state channel/enqueue :replies seat
               {:seat seat :text "sixteen" :ts "2026-07-27T00:00:00Z" :reply_id "RID-1"})
        (let [state (:state (get @reducer-session/sessions seat))]
          (is (contains? (:seen-reply-ids state) "RID-1")
              "the watch submitted the reply as a reducer fact")
          (is (some #(= "RID-1" (:reply/id %))
                    (cons (:speaking state) (:reply-queue state)))
              "and it is speaking or queued to speak — the reducer's decision, not the watch's"))
        (testing "the multiplayer bridge3 turn still lands too (the seam stays additive)"
          (is (some #(= "RID-1" (:reply_id %)) (get @channel/bridge3-convos seat))))
        (testing "a second enqueue of the same reply id ingests once"
          (let [n (count (filter #(= "RID-1" (:reply/id %))
                                 (:convo (reducer-session/view
                                           (get @reducer-session/sessions seat)))))]
            (swap! channel/channel-state channel/enqueue :replies seat
                   {:seat seat :text "sixteen" :ts "2026-07-27T00:00:01Z" :reply_id "RID-1"})
            (is (= n (count (filter #(= "RID-1" (:reply/id %))
                                    (:convo (reducer-session/view
                                              (get @reducer-session/sessions seat))))))))))
      (finally (reducer-session/reset-session! seat)))))

(deftest the-beacon-name-is-read-from-both-envelope-shapes
  "THE `:k nil` BUG (Gene, 2026-07-27 field logs): /bridge3-new's entry wraps its
   ports' rows ({page, build, seat, row:{event}}) instead of posting the legacy
   FLAT `:k` report, so the extractor read no name, dropped the beacon before the
   tap, and logged `:k nil` — a field-debugging blackout."
  (testing "the legacy flat shape is unchanged"
    (is (= "bridge-boot" (channel/beacon-name {:k "bridge-boot"})))
    (is (= "hf-command" (channel/beacon-name {:k "native-hf" :event "hf-command"}))
        "native-hf still unwraps to its bounded causal name")
    (is (= "native-shell-failure" (channel/beacon-name {:k "native-shell-failure"
                                                        :event "bridge-message-42"}))
        "native-shell-failure is deliberately NOT unwrapped: its :event is an open set"))
  (testing "and the wrapped /bridge3-new shape now has a name"
    (is (= "session-degraded"
           (channel/beacon-name {:page "bridge3-new" :build "abc"
                                 :row {:event "session-degraded" :reason "timeout"}}))))
  (testing "nothing is renamed, prefixed or invented"
    (is (nil? (channel/beacon-name {:page "bridge3-new" :row {}})))
    (is (nil? (channel/beacon-name {}))))
  (testing "and the row's fields are lifted so identities and live values are read"
    (let [p (channel/beacon-payload {:page "bridge3-new" :build "abc"
                                     :row {:event "effect-terminal" :replyId "R9" :kind "speak"}})]
      (is (= "R9" (:replyId p)))
      (is (= "effect-terminal" (:event p)))
      (is (= "abc" (:build p)) "the envelope's own fields survive"))
    (is (= {:k "flat"} (channel/beacon-payload {:k "flat"})) "a flat beacon is returned unchanged")))

;; ---------------------------------------------------------------------------
;; B59 — THE THIN CAPTURE / STT PORT
;;
;; `/api/bridge3-new/capture` is a PORT: bytes in, RAW transcript out. Every
;; product ruling that `/api/bridge3/dictate` still makes on the way past — the
;; noise filter, the OVER sign-off, the partials stash, the strip, the post —
;; belongs to `reducer/core`'s `handle-transcript` now, and two authorities for
;; one decision is what these tests forbid.
;; ---------------------------------------------------------------------------

(defn- capture-req [id transcript-fn]
  (let [tmp (java.io.File/createTempFile "captest" ".webm")]
    (spit tmp "xxxxxxxxxxxxxxxx")
    {:tmp tmp
     :req {:params (cond-> {:audio {:tempfile tmp :filename "d.webm"} :seat "capseat"}
                     id (assoc :dedup_id id))}
     :stt transcript-fn}))

;; INTENT-TEST: THIN-CAPTURE-PORT-B59
(deftest the-capture-port-returns-the-raw-transcript-and-decides-nothing
  (reset! @#'channel/bridge3-capture-seen {:order [] :m {}})
  (reset! channel/channel-state {:messages {} :replies {}})
  (reset! channel/bridge3-convos {})
  (testing "the trailing protocol word comes back VERBATIM — the reducer strips it"
    (with-redefs [groq/transcribe-audio (constantly {:transcript "how long is the run. Over."})]
      (let [{:keys [tmp req]} (capture-req "C1" nil)
            resp (channel/handle-bridge3-capture req)
            body (read-json resp)]
        (is (= 200 (:status resp)))
        (is (true? (:ok body)))
        (is (= "how long is the run. Over." (:transcript body))
            "no strip-over: the sign-off is the reducer's to rule on")
        (.delete tmp))))
  (testing "and a Whisper silence hallucination is NOT filtered here either"
    ;; The legacy dictate handler drops 'you' / 'Thank you.' itself. The reducer
    ;; now owns that (B32, `silence-hallucination?`) — and it owns it WITH the
    ;; window's measured energy, which this handler does not have and must not
    ;; guess at. A port that filtered would be filtering blind.
    (with-redefs [groq/transcribe-audio (constantly {:transcript "Thank you."})]
      (let [{:keys [tmp req]} (capture-req "C2" nil)
            body (read-json (channel/handle-bridge3-capture req))]
        (is (= "Thank you." (:transcript body)))
        (.delete tmp))))
  (testing "NOTHING is posted: no message enqueued, no conversation turn appended"
    (is (empty? (get-in @channel/channel-state [:messages "capseat"])))
    (is (empty? (get @channel/bridge3-convos "capseat"))))
  (reset! @#'channel/bridge3-capture-seen {:order [] :m {}}))

(deftest the-capture-port-answers-a-retry-with-the-same-logical-utterance
  "A retry of any upload/STT step carries the SAME dedup_id and must come back as
   the SAME raw transcript — that is what stops one spoken turn becoming two
   downstream. It also must not re-transcribe: the bytes already cost a Groq
   call, and a second call could return different words for the same utterance."
  (reset! @#'channel/bridge3-capture-seen {:order [] :m {}})
  (let [calls (atom 0)]
    (with-redefs [groq/transcribe-audio (fn [_] (swap! calls inc) {:transcript "the same words"})]
      (let [{:keys [tmp req]} (capture-req "DUP" nil)]
        (is (= "the same words" (:transcript (read-json (channel/handle-bridge3-capture req)))))
        (let [body (read-json (channel/handle-bridge3-capture req))]
          (is (true? (:dedup body)))
          (is (= "the same words" (:transcript body))))
        (is (= 1 @calls) "the duplicate never reached STT")
        (.delete tmp))))
  (reset! @#'channel/bridge3-capture-seen {:order [] :m {}}))

(deftest the-capture-port-fails-the-way-the-retry-ladder-reads
  (reset! @#'channel/bridge3-capture-seen {:order [] :m {}})
  (testing "blank STT -> 400 PERMANENT, with the body the ladder already knows"
    (with-redefs [groq/transcribe-audio (constantly {:transcript "   "})]
      (let [{:keys [tmp req]} (capture-req "E1" nil)
            resp (channel/handle-bridge3-capture req)]
        (is (= 400 (:status resp)))
        (is (= "no speech detected (audio too short or silent)" (:error (read-json resp))))
        (.delete tmp))))
  (testing "a Groq 4xx -> 400 PERMANENT and forwards the upstream detail"
    ;; The JS port relays `groq-status` / `groq-body` as `:upstream/status` /
    ;; `:upstream/detail` on `:fact/upload-failed`, so these exact keys are wire
    ;; contract, not a log line.
    (with-redefs [groq/transcribe-audio (fn [_] (throw (ex-info "bad audio" {:status 415 :body "unsupported"})))]
      (let [{:keys [tmp req]} (capture-req "E2" nil)
            resp (channel/handle-bridge3-capture req)
            body (read-json resp)]
        (is (= 400 (:status resp)))
        (is (= 415 (:groq-status body)))
        (is (= "unsupported" (:groq-body body)))
        (.delete tmp))))
  (testing "a Groq 5xx -> 500 RETRYABLE"
    (with-redefs [groq/transcribe-audio (fn [_] (throw (ex-info "groq down" {:status 503 :body "upstream"})))]
      (let [{:keys [tmp req]} (capture-req "E3" nil)]
        (is (= 500 (:status (channel/handle-bridge3-capture req))))
        (.delete tmp))))
  (testing "a refusal this server made itself -> 400 PERMANENT, no invented Groq status"
    (with-redefs [groq/transcribe-audio
                  (fn [_] (throw (ex-info "No audio captured (recording too short)"
                                          {:size 300 :client-error? true})))]
      (let [{:keys [tmp req]} (capture-req "E4" nil)
            resp (channel/handle-bridge3-capture req)]
        (is (= 400 (:status resp)))
        (is (nil? (:groq-status (read-json resp))))
        (.delete tmp))))
  (testing "a junk seat is refused outright"
    (is (= 400 (:status (channel/handle-bridge3-capture {:params {:seat "not a seat!"}})))))
  (testing "and no audio is still 400"
    (is (= 400 (:status (channel/handle-bridge3-capture {:params {:seat "capseat"}})))))
  (reset! @#'channel/bridge3-capture-seen {:order [] :m {}}))

;; ---------------------------------------------------------------------------
;; B89 / P10 — A PROMOTED TTS ARTIFACT IS VERIFIED COMPLETE BEFORE IT IS SERVED
;; ---------------------------------------------------------------------------

(deftest verify-complete-is-pure-and-says-nil-when-the-artifact-is-honest
  (testing "a declared length the bytes match is complete"
    (is (nil? (tts/verify-complete 1024 1024))))
  (testing "an UNDECLARED length is complete — a streamed response sends none,
            and nil means 'not declared', never zero"
    (is (nil? (tts/verify-complete nil 1024)))
    (is (nil? (tts/verify-complete nil 0))))
  (testing "a declared length the bytes fall short of is TYPED, not a boolean"
    (is (= {:error/kind :playback-truncated :declared 40000 :actual 12000}
           (tts/verify-complete 40000 12000))))
  (testing "…and so is a body LONGER than declared — either way the artifact is
            not the one the upstream said it was sending"
    (is (= :playback-truncated (:error/kind (tts/verify-complete 100 200))))))

;; INTENT-TEST: TTS-VERIFIED-COMPLETE-i37
(deftest a-truncated-tts-artifact-is-refused-and-never-cached
  "THE i37 TRUNCATION SCAR, closed. HTTP 200 can carry an mp3 that stops halfway
   through the sentence and raises NO playback error: the media element fires
   `ended` early and the session records `playback-finished` for a reply Gene
   only half heard. P10 had no implementation. It does now, and the refusal is a
   TYPED 502 — `playback-truncated` must never be able to become
   `playback-finished`."
  (reset! @#'channel/bridge-tts-cache {:order [] :m {}})
  (testing "declared 40000, delivered 12 -> 502, typed, with both numbers"
    (with-redefs [tts/synthesize-speech
                  (fn [_text & _opts]
                    {:audio-bytes (byte-array 12) :content-type "audio/mpeg"
                     :declared-length 40000})]
      (let [resp (channel/handle-bridge-tts {:params {:text "half a sentence" :reply_id "r-1"}})
            body (read-json resp)]
        (is (= 502 (:status resp)) "never 200 — a 200 is indistinguishable from success")
        (is (= "playback-truncated" (:error body)))
        (is (= 40000 (:declared body)))
        (is (= 12 (:actual body))))))
  (testing "and NOTHING was cached — one bad synthesis must not become permanent"
    (is (empty? (:m @@#'channel/bridge-tts-cache)))
    (is (empty? (:order @@#'channel/bridge-tts-cache))))
  (testing "the honest case still serves 200 with the right Content-Length"
    (with-redefs [tts/synthesize-speech
                  (fn [_text & _opts]
                    {:audio-bytes (byte-array 2048) :content-type "audio/mpeg"
                     :declared-length 2048})]
      (let [resp (channel/handle-bridge-tts {:params {:text "a whole sentence" :reply_id "r-2"}})]
        (is (= 200 (:status resp)))
        (is (= "2048" (get-in resp [:headers "Content-Length"])))
        (is (= 2048 (alength ^bytes (:body resp)))))))
  (testing "an UNDECLARED length still serves 200 — a streamed artifact is honest"
    (with-redefs [tts/synthesize-speech
                  (fn [_text & _opts]
                    {:audio-bytes (byte-array 999) :content-type "audio/mpeg"
                     :declared-length nil})]
      (let [resp (channel/handle-bridge-tts {:params {:text "streamed reply" :reply_id "r-3"}})]
        (is (= 200 (:status resp)))
        (is (= "999" (get-in resp [:headers "Content-Length"]))))))
  (testing "and the two honest artifacts ARE cached"
    (is (= 2 (count (:m @@#'channel/bridge-tts-cache)))))
  (reset! @#'channel/bridge-tts-cache {:order [] :m {}}))

;; ---------------------------------------------------------------------------
;; SSE STREAM IDENTITY + RECONNECT CONTRACT (bd marvin-voice-remote-gyb)
;;
;; Cloud Run runs this service at maxScale=1 / concurrency=300, so 300 held
;; requests is the ceiling for the WHOLE service and an SSE stream is a request
;; that never ends. The server side of the fix (supersession + a 240-270s
;; rotation deadline) is only reachable if the PAGES hold up their end: carry a
;; per-page-load client id, and reconnect after a clean server-side close.
;; ---------------------------------------------------------------------------

(defn- with-clean-sse
  "Run `f` with an empty SSE census on both sides.
   Deliberately NOT a second `use-fixtures :each` call: clojure.test stores each
   fixture list in ns metadata and a second registration REPLACES the first, which
   would silently disarm the state reset every other test in this file depends on."
  [f]
  (sse/clear!)
  (try (f) (finally (sse/clear!))))

(defn- stream-client-id
  "The client id off a page's data-star-init stream URL, or nil."
  [html]
  (second (re-find #"client=([^'&]+)'" html)))

(deftest bridge3-and-bridge4-state-streams-carry-a-client-id-and-always-retry
  ;; TWO separate failures are pinned here, and each one alone re-wedges the service.
  ;; WITHOUT the client id the server cannot tell a reconnect from a second tab, so
  ;; it may not close the stream this one replaces — abandoned streams accumulate to
  ;; 300 and every request 429s, /api/health included. WITHOUT retry:'always' the
  ;; cure is worse than the disease: Datastar's default retry:'auto' re-opens only an
  ;; ERRORED request, and the server's rotation closes the stream CLEANLY, so both
  ;; surfaces would go permanently deaf ~4 minutes after load with no visible error.
  (with-clean-sse
    (fn []
      (doseq [[label html] [["bridge3" (#'channel/bridge3-page-html "marvin-dev")]
                            ["bridge4" (#'channel/bridge4-page-html "marvin-dev")]]]
        (testing label
          (is (re-find #"/sse/bridge3\?seat=marvin-dev&(amp;)?client=p-[A-Za-z0-9]+" html)
              "the stream URL names the seat AND this page load")
          (is (re-find #"retry:'always'" html)
              "a cleanly-closed (rotated) stream must still be re-opened")
          (is (re-find #"retryInterval:3000" html)
              "the post-recycle re-open delay is explicit, and caps re-attempts while 429ing"))))))

(deftest two-renders-of-the-same-page-mint-different-client-ids
  ;; Gene keeps several desktop tabs open on one seat; they must COEXIST. The id is
  ;; per PAGE LOAD, never per seat — a seat-shaped id would make every new tab
  ;; supersede the previous one, each eviction would trigger that tab's reconnect,
  ;; and the mutual-eviction storm would burn slots faster than the leak it fixes.
  (with-clean-sse
    (fn []
      (let [a (stream-client-id (#'channel/bridge3-page-html "marvin-dev"))
            b (stream-client-id (#'channel/bridge3-page-html "marvin-dev"))
            c (stream-client-id (#'channel/bridge4-page-html "marvin-dev"))]
        (is (every? some? [a b c]) "every render carries an id")
        (is (= 3 (count (distinct [a b c]))) "no two page loads share an id")
        (is (every? #(some? (sse/client-id-of {:params {:client %}})) [a b c])
            "and every minted id survives the server's sanitizer — an id the server
             rejects is the same as no id at all")))))

(deftest the-voice-lab-reducer-surface-puts-its-client-id-on-the-effect-stream-url
  ;; The reducer lab already mints CID for its event envelopes; the EFFECT STREAM is
  ;; the long-lived request, so that is where the id has to ride for the server to
  ;; supersede this page's own reconnects. Plain `&` — this is a JS string literal
  ;; inside a <script> block, not an HTML attribute value.
  (with-clean-sse
    (fn []
      (let [html (#'channel/voice-lab-reducer-page-html)]
        (is (re-find #"var CID='web:'" html) "the page still mints a per-load client id")
        (is (re-find #"\Q&client='+encodeURIComponent(CID)\E" html)
            "the effect stream URL carries it")
        (is (re-find #"/api/reducer-session/effects\?seat='\+encodeURIComponent\(SEAT\)\+'&client='" html)
            "and it sits on the effects URL itself, right after the seat")
        (is (not (re-find #"&amp;client=" html))
            "NOT entity-escaped: an &amp; inside a JS string would be sent literally")))))

(deftest the-frozen-voice-lab-legacy-page-is-untouched
  ;; /voice-lab is the pinned receipt window — the artifact later surfaces are
  ;; compared against. It gets no client id, no retry option, no rotation exposure.
  (with-clean-sse
    (fn []
      (let [html (#'channel/voice-lab-page-html)]
        (is (not (re-find #"client=" html)) "no client id anywhere on the frozen page")
        (is (re-find #"Voice Lab \(hands-free\)" html) "and it is still the page we think it is")))))
