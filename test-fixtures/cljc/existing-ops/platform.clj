(ns myapp.lib.platform
  "CLJ side of a dialect-split pair."
  (:require
   [clojure.string :as str]
   [clojure.java.io :as io]))

(defn timestamp []
  (System/currentTimeMillis))

(defn temp-dir []
  (str (System/getProperty "java.io.tmpdir")))

(defn hostname []
  (str/trim (.getHostName (java.net.InetAddress/getLocalHost))))
