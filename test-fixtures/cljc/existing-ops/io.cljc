(ns myapp.ui.io
  "I/O utilities — shared interface, platform-specific implementations.
   Mirrors the fulcro io.cljc pattern."
  (:require
   [clojure.string :as str]
   #?(:clj  [clojure.java.io :as jio])
   #?(:cljs [goog.string :as gstr])))

;; ---- Shared ----

(defn sanitize-filename [name]
  (-> name
      str/trim
      (str/replace #"[^a-zA-Z0-9._-]" "_")))

;; ---- Platform-specific implementations ----

#?(:clj
   (defn read-file [path]
     (slurp (jio/file path))))

#?(:clj
   (defn write-file [path content]
     (spit (jio/file path) content)))

#?(:cljs
   (defn get-element [id]
     (-> js/document (.getElementById id))))

#?(:cljs
   (defn format-number [n decimals]
     (gstr/format (str "%." decimals "f") n)))
