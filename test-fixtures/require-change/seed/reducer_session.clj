(ns marvin-voice-remote.reducer-session
  "S5a — THE SESSION LOOP. The reducer becomes AUTHORITATIVE for one live
   surface (ARCHITECTURE.md §7 S5a, D11: `/voice-lab` first, iOS second,
   `/bridge4` last).

   S4 ran the reducer in SHADOW: real events went in, decisions came out, and
   nothing was ever executed. This namespace is the same reducer with the wire
   attached at both ends:

     browser --(marvin.voice/1 envelopes)--> POST /api/reducer-session/events
                                              |
                                       protocol/explain  (fail closed)
                                       protocol/fact->event
                                       core/step         (THE decision)
                                              |
     browser <--(effect batches, SSE)------ GET /api/reducer-session/effects

   Three properties are the whole design, and each one is a rule the old
   engines broke:

   1. NO DECISION LIVES HERE. This namespace validates, routes, journals and
      transports. Every product question — is that speech, has the utterance
      ended, does this reply speak now, may the mic reopen — is answered by
      `core/step` under `policy/current`. Grep this file for a threshold and
      you will not find one; the only numbers are transport cadences, named
      as such.

   2. EVERY EFFECT IS EXECUTED BY THE CLIENT. v1 executes nothing server-side
      (D8's durable outbox + runners is Gate G1 work, and G1 ruled 'no new
      database now' on 2026-07-25). The outbox here is an in-memory ring with
      a monotonic cursor, which is exactly enough to survive a reconnect and
      honest about not surviving a restart — a reconnecting client that has
      fallen off the back of the ring is TOLD it lost effects (D5: never
      silently resume).

   3. THE JOURNAL KEEPS RUNNING. Every accepted event is also recorded into
      `reducer.shadow`'s journal, so the /reducer-lab/shadow console gains,
      for the first time, a trace that carries REAL reducer-catalog events and
      REAL `:cmd/*` commands. That closes two S4 coverage gaps by supply
      rather than by argument: `speak-comparison`'s 'the replayed session
      never seeded' gap (no server seam produces a command — this one does)
      and the identity gaps (`:requires-identity`), because the ids come off
      the reducer's own state instead of being inferred.

   WHAT IS DELIBERATELY NOT HERE: no state in the DOM, no policy on the wire,
   no server-side audio. The page is a MediaPort (D4) — its negative space is
   documented at `channel/voice-lab-reducer-page-html`."
  (:require [clojure.data.json :as json]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [marvin-voice-remote.reducer.core :as core]
            [marvin-voice-remote.reducer.policy :as policy]
            [marvin-voice-remote.reducer.protocol :as protocol]
            [marvin-voice-remote.reducer.shadow :as shadow]
            [marvin-voice-remote.sse-registry :as sse]
            [org.httpkit.server :as http]
            [taoensso.timbre :as log])
  (:import [java.util.concurrent.atomic AtomicLong]))
