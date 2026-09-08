(ns marvin-voice-remote.director-outbox
  "Durable, acknowledged SSE delivery from the frozen Bridge 4 engine to Code
   Director. This namespace observes exactly one channel seat; it never changes
   capture, transcription, OVER policy, or any Bridge handler.

   Cloud Run is deliberately max-instances=1, and blob/save-edn! writes through
   to GCS. A finalized utterance is therefore persisted before the originating
   channel-state swap returns. SSE is only the low-latency notification layer:
   every unacknowledged event is replayed after reconnect."
  (:require
   [clojure.data.json :as json]
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
