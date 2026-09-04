;; A DOCUMENTED LIMITATION, not build output: `target` is pruned by NAME
;; wherever it appears, so a source namespace that happens to live under a
;; directory called `target` is invisible to :ls-tree. The frozen golden
;; beside this file records that, so the day the rule becomes path-anchored
;; the golden changes and someone reads this note.
(ns app.target.bar)

(defn bar-fn [] :ok)
