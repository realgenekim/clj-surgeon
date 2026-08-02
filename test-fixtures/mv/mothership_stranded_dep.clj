(ns mv.mothership-stranded-dep)

;; Faithful minimized fixture from realgenekim/clj-surgeon#20.
;; The declare is essential: it proves the starting program is valid and that
;; moving walk-files above run-kondo introduces the skip-dirs failure.
(declare walk-files)

(defn run-kondo [root]
  (walk-files root))

(def skip-dirs
  #{"target" ".git"})

(defn walk-files [root]
  (remove skip-dirs root))
