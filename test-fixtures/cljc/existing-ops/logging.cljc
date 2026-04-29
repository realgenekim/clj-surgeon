(ns myapp.lib.logging
  "Logging helpers — mirrors real Fulcro logging.cljc pattern."
  (:require
   #?@(:clj [[clojure.pprint :refer [pprint]]])
   [clojure.string :as str]))

;; ---- Shared helpers (both platforms) ----

(defn format-level [level]
  (str/upper-case (name level)))

(defn- truncate-ns [ns-str]
  (str/replace-first (or ns-str "?") "com.example." "_"))

;; ---- CLJ-only: macros and server-side logging ----

#?(:clj
   (defmacro p
     "Pretty-print a value with delimiters."
     [v]
     `(str "\n" (with-out-str (pprint ~v)) "\n")))

#?(:clj
   (defn custom-output-fn
     "Server-side log output formatter."
     ([data] (custom-output-fn nil data))
     ([opts data]
      (let [{:keys [level msg_ ?ns-str ?line]} data]
        (format "%1.1S %20s:-%3s - %s"
                (name level)
                (truncate-ns ?ns-str)
                (or ?line "?")
                (force msg_))))))

#?(:clj
   (defn configure-logging!
     "Configure server-side logging. Uses truncate-ns and custom-output-fn."
     [config]
     {:output-fn custom-output-fn
      :level (format-level (:level config :info))}))
