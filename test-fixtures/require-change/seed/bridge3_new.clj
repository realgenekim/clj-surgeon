(ns marvin-voice-remote.bridge3-new
  "/bridge3-new — S5c Phase C1: the ONE new page (D1), built as a DATASTAR
   GAME-ENGINE surface over the reducer session loop.

   Plan v2.2/v2.3/v2.4/v2.5 in one paragraph: the intermediate rung is PTT on
   the reducer, at bridge3's scope, where it has a real comparand and a real
   adoption population; it ships the NEW UI and the storyboard-A status
   vocabulary from birth; and it is built as a Datastar page, not as a
   hand-rolled successor to the legacy pages' client-rendered style.

   THE ARCHITECTURE IN ONE PICTURE — two channels, one brain, zero client
   decisions:

     tap ──command envelope──▶ POST /api/reducer-session/events
                                        │
                                  core/step (THE decision)
                                        │
       ┌────────────────────────────────┴────────────────────────────────┐
       │                                                                 │
     EFFECTS (open-mic, close-mic, speak, cue)              STATE (what is on screen)
       │  SSE /api/reducer-session/effects                    │  SSE /sse/bridge3-new
       ▼                                                      ▼
     the MediaPort executes them                      the SERVER renders HTML and
     (browser-native ops only)                        MORPHS it into the page

   Four properties are load-bearing, and each one is a rule a legacy page broke:

   1. THE SERVER RENDERS EVERYTHING VISIBLE. Status line, turn line, timer
      origin, conversation, player dock, button LABEL and button COMMAND are all
      computed here from `core/status-view` / `core/turn-view` / the session
      view, and pushed as HTML. The client stores nothing a server decision
      reads and writes no status text. The one thing the page hands the client
      is a `data-cmd` attribute saying WHICH command this tap sends — which is
      how tap-to-talk toggles without the client knowing what a bracket is.

   2. R-STATIC (Gene, 2026-07-27). This file emits HTML STRUCTURE ONLY: no
      inline <script>, no <style>, no on*= handler attributes. Every byte of CSS
      and JS lives under `resources/public/`, cache-busted, hashed into the
      asset-manifest golden, and `check-pages` fails the build if an inline
      program ever appears here (`r-static-pages`).

   3. NOTHING HERE DECIDES. Grep this file for a product threshold and you will
      not find one; the only conditionals are layout and rendering. Which
      command the button sends is a pure function of reducer state
      (`next-tap-command`); what the line says is `core/status-view` verbatim.

   4. IT TOUCHES NO EXISTING SURFACE. Additive routes, its own page-state atom,
      its own SSE endpoint. The reducer session loop it subscribes to is the one
      `/voice-lab` already uses; deleting this namespace would change no shipped
      behavior on any other page."
  (:require
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [datastar-kit.ds :as ds]
   [hiccup2.core :as h]
   [marvin-voice-remote.friction-ui :as friction-ui]
   [marvin-voice-remote.reducer-session :as rs]
   [marvin-voice-remote.reducer.core :as core]
   [marvin-voice-remote.sse-registry :as sse]
   [org.httpkit.server :as http]
   [taoensso.timbre :as log]))
