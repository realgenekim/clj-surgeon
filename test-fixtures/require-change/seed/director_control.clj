(ns marvin-voice-remote.director-control
  "Authenticated Director commands for the uniquely active Marvin capture client."
  (:require
   [clojure.data.json :as json]
   [clojure.string :as str]
   [marvin-voice-remote.sse-registry :as sse]
   [org.httpkit.server :as http]
   [taoensso.timbre :as log])
  (:import
   (java.time Instant)))
