(ns marvin-voice-remote.tts
  "ElevenLabs Text-to-Speech integration. Converts Marvin's text
   responses to spoken audio for playback on Gene's phone."
  (:require
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [hato.client :as hc]
   [taoensso.timbre :as log])
  (:import
   (java.io ByteArrayOutputStream)))

(def ^:private api-url "https://api.elevenlabs.io/v1/text-to-speech")

;; Available voices — Gene's curated picks from ElevenLabs voice library
(def voices
  [{:id "mZ8K1MPRiT5wDQaasg3i" :name "Alexander" :default? true}
   {:id "sIivXWc5MTlPIP3kJXhg" :name "Hayes"}
   {:id "pEE4xYROBDy1ZB3jJbql" :name "Matt"}
   {:id "Yg7C1g7suzNt5TisIqkZ" :name "Jude"}
   {:id "2UMI2FME0FFUFMlUoRER" :name "Hugh"}
   {:id "mZ2wXweMuluxNdZUBBh7" :name "Tom Clark"}])

;; Selected voice — mutable, changed via UI dropdown
(defonce selected-voice-id (atom "mZ8K1MPRiT5wDQaasg3i"))

(defn current-voice-id []
  (or (System/getenv "ELEVENLABS_VOICE_ID")
      @selected-voice-id))

(defn- load-api-key []
  (or (System/getenv "ELEVENLABS_API_KEY")
      (let [paths ["secrets/eleven-labs.txt"
                   "secrets/elevenlabs.txt"
                   (str (System/getProperty "user.home") "/src.local/secrets/elevenlabs.txt")]]
        (some (fn [p]
                (let [f (io/file p)]
                  (when (.exists f)
                    (clojure.string/trim (slurp f)))))
              paths))))

(defn- parse-content-length
  "Pure: an upstream `content-length` header value as a long, or nil.

   NIL MEANS 'NOT DECLARED', NEVER ZERO — a chunked or streamed response
   legitimately sends no length, and coercing that absence to 0 would turn every
   honest streamed artifact into a truncation report. Anything unparseable is
   also nil, for the same reason: an unreadable claim is not a claim."
  [v]
  (let [s (cond
            (string? v)     v
            (sequential? v) (first v)
            (some? v)       (str v))]
    (when (and (string? s) (re-matches #"\d+" (clojure.string/trim s)))
      (parse-long (clojure.string/trim s)))))

;; INTENT: TTS-VERIFIED-COMPLETE-i37
(defn verify-complete
  "PURE, PUBLIC: is a promoted TTS artifact whole? nil when it is acceptable,
   else a TYPED failure map.

   P10 — 'a TTS artifact is verified complete before it counts as played' — had
   no implementation, and that is the i37 truncation scar: HTTP 200 can carry a
   TRUNCATED mp3 that ends halfway through the sentence and raises NO playback
   error at all. The media element simply fires `ended` early and the reducer
   records `playback-finished` for a reply Gene only half heard. A silence that
   looks like a success is the one failure mode this system is not allowed to
   have (D6: failure is visible), so the artifact is checked against what the
   upstream SAID it was sending before it may be cached or served.

   `declared` nil is ACCEPTED, deliberately: a streamed response declares no
   length, and refusing every undeclared artifact would break the honest case in
   order to catch the dishonest one. What is refused is a declared length the
   bytes DISAGREE with — the upstream said 40 000 and delivered 12 000, which is
   exactly and only the truncation."
  [declared actual]
  (when (and (some? declared) (some? actual)
             (not= (long declared) (long actual)))
    {:error/kind :playback-truncated
     :declared   (long declared)
     :actual     (long actual)}))

(defn synthesize-speech
  "Convert text to speech using ElevenLabs API.
   Returns {:audio-bytes <byte-array> :content-type \"audio/mpeg\"
            :declared-length <long|nil> :actual-length <long>} or throws.

   `:declared-length` is the upstream response's `content-length` header, parsed
   — or nil when the upstream sent none. It is reported and never enforced HERE:
   this namespace observes what arrived, and `verify-complete` (plus the serving
   handler) decides what an incomplete artifact means. See P10 / the i37
   truncation scar.

   Options:
     :voice-id    — ElevenLabs voice ID (default: selected voice from dropdown)
     :speed       — playback speed multiplier (default: 1.0)
     :stability   — voice stability 0.0-1.0 (default: 0.5)
     :similarity  — voice similarity boost 0.0-1.0 (default: 0.75)

   LOUDNESS (investigated 2026-07-03, iOS ducking bug): the TTS convert
   endpoint has NO output-volume/loudness-normalization parameter —
   voice_settings is stability/similarity_boost/style/use_speaker_boost/speed
   only (use_speaker_boost = similarity, NOT volume; the volume knob exists
   only in the voice-DESIGN API). A server-side boost would mean re-encoding
   every clip and re-synthesizing nothing cached — so the loudness lever is
   CLIENT-side: the ?ttsgain GainNode in resources/public/js/bridge-player.js."
  [text & {:keys [voice-id speed stability similarity]
           :or {voice-id nil
                speed 1.0
                stability 0.5
                similarity 0.75}}]
  (let [api-key (load-api-key)
        voice-id (or voice-id (current-voice-id))]
    (when-not api-key
      (throw (ex-info "No ElevenLabs API key found" {})))
    (log/info :tts-generating :chars (count text) :voice voice-id :speed speed)
    (let [started (System/nanoTime)
          resp (hc/post (str api-url "/" voice-id)
                        {:headers {"xi-api-key" api-key
                                   "Content-Type" "application/json"
                                   "Accept" "audio/mpeg"}
                         :body (json/write-str
                                 {:text text
                                  :model_id "eleven_turbo_v2_5"
                                  :voice_settings {:stability stability
                                                   :similarity_boost similarity
                                                   :speed speed}})
                         :as :byte-array
                         :throw-exceptions false})
          elapsed-ms (long (/ (- (System/nanoTime) started) 1000000))
          request-id (or (get-in resp [:headers "request-id"])
                         (get-in resp [:headers "x-request-id"]))
          declared (parse-content-length (get-in resp [:headers "content-length"]))
          actual (count (:body resp))]
      (if (= 200 (:status resp))
        (do
          (log/info :tts-generated :bytes actual :voice voice-id
                    :elapsed-ms elapsed-ms :upstream-request-id request-id
                    :declared-length declared)
          {:audio-bytes (:body resp)
           :content-type "audio/mpeg"
           :declared-length declared
           :actual-length actual})
        (do
          (let [body (String. (:body resp) "UTF-8")]
            (log/error :tts-error :status (:status resp) :voice voice-id
                       :elapsed-ms elapsed-ms :upstream-request-id request-id
                       :body (subs body 0 (min 512 (count body)))))
          (throw (ex-info "ElevenLabs TTS failed"
                          {:status (:status resp)
                           :voice-id voice-id
                           :elapsed-ms elapsed-ms
                           :upstream-request-id request-id})))))))

(comment
  (synthesize-speech "Hello Gene, this is Marvin speaking through ElevenLabs."))
