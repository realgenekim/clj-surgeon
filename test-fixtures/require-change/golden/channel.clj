(ns marvin-voice-remote.channel
  "Channel relay for the marvin-channel connector (a custom claude/channel MCP server).
   This is the REAL transport edge of the channel-connector PoC — it replaces the
   file mailbox so the skiff poller and the phone can rendezvous through Cloud Run
   (no inbound to the laptop; both sides connect OUTBOUND).

   ADDITIVE: nothing here touches the existing /voice -> STT -> Slack -> Marvin -> TTS
   path. Two per-seat queues, both DRAINED on GET (claim-once):

     POST /api/channel/messages   phone  -> Claude   {seat, from, text, id?}
     GET  /api/channel/messages?seat=X   -> drain     {:messages [...]}
     POST /api/channel/replies    Claude -> phone     {seat, text, reply_to?, files?}
     GET  /api/channel/replies?seat=X    -> drain      {:replies [...]}

   Auth: bearer token CHANNEL_TOKEN. If CHANNEL_TOKEN is unset, the endpoints are
   open ONLY when ENV=dev (local). See channel-connector/SPEC.md."
  (:require
   [clojure.data.json :as json]
   [marvin-voice-remote.json :as mjson]
   [clojure.java.io :as io]
   [datastar-kit.ds :as ds]
   [marvin-voice-remote.auth :as auth]
   [marvin-voice-remote.blob :as blob]
   ;; DURABLE CAPTURE ARCHIVE (bd marvin-voice-remote-0ar). Every call below is
   ;; total, never-throwing and off the hot path — a broken archive must never
   ;; break dictation. It observes and persists; it never decides.
   [marvin-voice-remote.capture-archive :as capture-archive]
   [marvin-voice-remote.friction-ui :as friction-ui]
   ;; S5c DELTA-6 REDUCER INGRESS. One call, from one additive watch at the reply
   ;; seam (`::reducer-reply-ingest` below): an arriving connector reply becomes a
   ;; server-authored `:fact/reply-arrived` in the seat's live reducer session.
   ;; The dependency runs surface -> session loop, never the other way: nothing in
   ;; `reducer-session` knows this file exists.
   [marvin-voice-remote.reducer-session :as reducer-session]
   ;; S4 SHADOW TAPS (ARCHITECTURE.md §7 S4). `shadow/record!` is total,
   ;; never-throwing and returns nil, ALWAYS — so every call below is additive by
   ;; construction: no response, status, or decision in this file can depend on
   ;; it. It observes; it never decides. Failures are counted, not logged blind
   ;; (S13), and shown on /reducer-lab/shadow.
   [marvin-voice-remote.reducer.shadow :as shadow]
   ;; THE SSE CENSUS (bd marvin-voice-remote-gyb). Cloud Run holds this service at
   ;; maxScale=1 / concurrency=300, so 300 held requests is the ceiling for the
   ;; WHOLE service and an SSE stream is a request that never ends. The registry
   ;; supplies the two disciplines an abandoned stream cannot escape —
   ;; per-page-load supersession and a bounded rotation deadline — plus the
   ;; off-thread push agent that keeps a slow socket off an http-kit worker.
   [marvin-voice-remote.sse-registry :as sse]
   [org.httpkit.server :as http]
   [taoensso.timbre :as log]))
