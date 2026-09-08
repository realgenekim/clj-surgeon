(ns marvin-voice-remote.codex-app-server
  "Small, REPL-first client for the Codex app-server JSONL protocol.

  This namespace intentionally owns no web state. A client is one local Codex
  process plus its wire transcript; callers decide how conversations are
  presented. All public lifecycle functions are safe to call from an nREPL."
  (:require
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.io BufferedReader BufferedWriter Closeable)
   (java.time Instant)
   (java.util.concurrent TimeUnit)))
