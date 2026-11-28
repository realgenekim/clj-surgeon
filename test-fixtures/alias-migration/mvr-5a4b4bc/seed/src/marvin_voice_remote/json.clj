(ns marvin-voice-remote.json
  "One place for the app's JSON parsing policy. Every inbound JSON body is
   parsed with keyword keys; changing the parser or its options happens here."
  (:require [clojure.data.json :as json]))

(defn parse
  "Parse a JSON string into Clojure data with keyword keys."
  [s]
  (json/read-str s :key-fn keyword))

(defn write
  "Serialize x as JSON. Options pass through to clojure.data.json/write-str."
  [x & opts]
  (apply json/write-str x opts))
