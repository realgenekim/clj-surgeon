(ns marvin-voice-remote.capture-archive
  "APPEND-ONLY durable archive of every voice capture the server touches.

   WHY (bd marvin-voice-remote-0ar, Gene mid-run 2026-07-26 21:05Z): a
   multi-minute hands-free dictation was lost. The server had ALREADY received
   the audio and ALREADY transcribed it — the rolling keyword checks uploaded a
   658KB blob and got 622 chars back — and then threw BOTH away (/tmp file
   deleted in a `finally`, transcript discarded because the check said
   `:continued`). When the page rebooted on an LTE flap, the in-progress
   MediaRecorder died with it and the recording was unrecoverable.

   THE FIX: never throw away what we already have. Every upload writes, verbatim
   and append-only:

     gs://<GCS_BUCKET>/captures/<seat>/<yyyy-MM-dd>/<request-id>.webm   raw bytes
     gs://<GCS_BUCKET>/captures/<seat>/<yyyy-MM-dd>/<request-id>.edn    metadata

   HOUSE DOCTRINE (\"store data in its most ORIGINAL form; transform on the way
   OUT, never on the way in\"): the .webm is the bytes the browser sent, byte for
   byte — no transcode, no trim, no validation-reject. The .edn records the RAW
   STT transcript (not the stripped/assembled text the handler posted) plus
   minimal metadata. Nothing is ever mutated: `request-id` is unique PER UPLOAD
   (the rolling keyword checks of one growing utterance are separate captures,
   grouped by `:dedup-id`), so no write can ever overwrite an earlier one.

   HOT PATH COST: zero. `archive-audio!` reads the already-on-disk temp file into
   memory synchronously (sub-millisecond; it must, because the handler deletes
   that file in its `finally`) and does the upload on a `future`. Every failure
   is logged loudly and swallowed — A BROKEN ARCHIVE MUST NEVER BREAK DICTATION.

   BACKENDS: GCS when GCS_BUCKET is set (Cloud Run). A local directory when
   `mvr.capture.dir` / MVR_CAPTURE_DIR is set (tests, and a dev laptop that wants
   real recovery). Neither set = a total no-op, which is the local-dev default.

   RETENTION: none yet. Bucket lifecycle is an ops decision — see bd 0ar."
  (:require [marvin-voice-remote.json :as mjson]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [marvin-voice-remote.blob :as blob]
            [taoensso.timbre :as log])
  (:import [java.time Instant ZoneOffset]
           [java.time.format DateTimeFormatter]))
