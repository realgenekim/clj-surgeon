#!/usr/bin/env bb
;; Print a bin directory on $PATH suitable for installing user scripts.
;; Preference: $HOME/.local/bin, then $HOME/bin, then any $HOME-rooted entry
;; in $PATH. Prints nothing and exits 1 if no candidate is found.

(require '[clojure.string :as str])

(let [home (System/getenv "HOME")
      entries (->> (str/split (or (System/getenv "PATH") "") #":")
                   (remove str/blank?))
      in-path? (set entries)
      preferred [(str home "/.local/bin") (str home "/bin")]
      home-prefix (str home "/")
      pick (or (first (filter in-path? preferred))
               (first (filter #(str/starts-with? % home-prefix) entries)))]
  (if pick
    (println pick)
    (System/exit 1)))
