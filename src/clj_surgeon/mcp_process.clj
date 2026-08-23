(ns clj-surgeon.mcp-process
  "Shared process environment for repository-owned formatter and verification commands."
  (:require
   [clojure.string :as str]))

(defn effective-path
  "Prepend the local agent tool directories while preserving the caller PATH."
  ([current]
   (effective-path (System/getProperty "user.home") current))
  ([user-home current]
   (->> (concat [(str user-home "/bin")
                 (str user-home "/.local/bin")
                 "/opt/homebrew/opt/node@20/bin"
                 "/opt/homebrew/bin"
                 "/usr/local/bin"
                 "/usr/bin"
                 "/bin"]
                (str/split (or current "")
                           (re-pattern
                             (java.util.regex.Pattern/quote
                               java.io.File/pathSeparator))))
        (remove str/blank?)
        distinct
        (str/join java.io.File/pathSeparator))))

(defn configure-environment!
  "Give one ProcessBuilder environment the same paved local tool entrance."
  [^java.util.Map environment]
  (.put environment "PATH" (effective-path (.getOrDefault environment "PATH" "")))
  environment)
