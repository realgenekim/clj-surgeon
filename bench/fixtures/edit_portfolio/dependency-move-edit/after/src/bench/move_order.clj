(ns bench.move-order)

;; The declaration makes this starting program valid. Moving walk-files alone
;; above run-kondo would strand its dependency on skip-dirs.
(declare walk-files)

(def skip-dirs
  #{"target" ".git"})

(defn walk-files [root]
  (remove skip-dirs root))

(defn run-kondo [root]
  (walk-files root))
