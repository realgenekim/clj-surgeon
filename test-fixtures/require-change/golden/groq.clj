(ns marvin-voice-remote.groq
  "Groq Whisper STT integration. Transcribes audio files via
   Groq's OpenAI-compatible transcriptions API."
  (:require [hato.client :as hc]
            [clojure.data.json :as json]
            [marvin-voice-remote.json :as mjson]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [taoensso.timbre :as log]))
