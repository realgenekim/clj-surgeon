(ns bench.fixtures.read-portfolio.match-decoys)

(declare send!)

(def textual-decoy "Another textual decoy: (send! :string)")

(defn publish-alpha
  []
  ;; Textual decoy: (send! :comment)
  (send! :alpha))

(defn publish-beta
  []
  (send! {:kind :beta}))
