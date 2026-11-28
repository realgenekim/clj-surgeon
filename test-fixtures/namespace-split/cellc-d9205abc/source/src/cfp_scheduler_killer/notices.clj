(ns cfp-scheduler-killer.notices
  "The server's short-term memory of what it needs to tell ONE person.

   A rejected write is not history — nobody will ever want to re-fold \"Gene
   typed a star value we didn't like\" into the world. So notices deliberately do
   NOT go in the event log: they live in an atom, keyed by [event-id person-id],
   and a restart forgets them. That is correct; a refusal is a conversation, not
   a fact about the conference.

   Why the SERVER holds it at all, rather than the browser: the board is
   Datastar-driven and the house rule is that the server owns every piece of
   state the UI displays (global CLAUDE.md, NEVER #4). The client never parses a
   response, never branches on one, and never runs a timer to fade a message
   away. It POSTs; the server decides what the message says, who sees it, and —
   this is the part a `setTimeout` gets wrong — WHEN it goes away:

     - the person's next SUCCESSFUL action on that event clears it, because the
       thing they were told about is now done;
     - or they dismiss it, which is a POST like any other.

   One notice per person per event. A second refusal replaces the first, because
   two stacked complaints about the same control is noise, and the newest one is
   always the one they just caused."
  (:require [taoensso.timbre :as log]))

(defonce ^{:doc "{[event-id person-id] -> {:kind :message :detail :at ...}}"}
  notices (atom {}))

(def kinds
  "`:error` — we refused a write. `:ok` — we did something you asked for out of
   band (a Slack test post) and you deserve to hear how it went."
  #{:error :ok})

(defn set-notice!
  "Remember one thing to say to one person about one event. Returns the notice."
  [event-id person-id notice]
  (when (and event-id person-id)
    (let [n (merge {:kind :error} notice)]
      (swap! notices assoc [event-id person-id] n)
      (log/info :notice-set :event-id event-id :person-id person-id
                :kind (:kind n) :message (:message n))
      n)))

(defn notice-for
  [event-id person-id]
  (when (and event-id person-id)
    (get @notices [event-id person-id])))

(defn clear-notice!
  [event-id person-id]
  (when (and event-id person-id)
    (swap! notices dissoc [event-id person-id]))
  nil)

(defn clear-all!
  "Tests and the REPL. Never called by a handler."
  []
  (reset! notices {})
  nil)
