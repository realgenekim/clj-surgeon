(ns mv.writer-state-chain)

;; Faithful minimized fixture from the writer.state transition! dependency
;; cascade documented in docs/observations/2026-03-28-first-real-use.md.
(declare transition!)

(defn dispatch! [event]
  (transition! event))

(def app-state
  (atom {}))

(defn log-event! [event]
  (swap! app-state assoc :last-event event))

(defn transition! [event]
  (log-event! event))
