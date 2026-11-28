(ns cfp-scheduler-killer.exports
  "Open-data exports — the ai.engineer seam.

   swyx doesn't publish from his program tool: ai.engineer/schedule is a custom
   Next.js site fed by open data exports (sessions.json · speakers.json ·
   llms.txt · calendar.ics). That is why requirement 9 (embeddable gallery) was
   struck from the brief — they own publishing, and they should.

   So we emit those files in THEIR shape, byte-compatible, and our tool drops
   into the pipeline they already have with zero migration. We replace
   Sessionboard exactly at its seam and leave the part they do better alone.

   ONE RULE GOVERNS EVERYTHING HERE: an export describes the PUBLISHED PROGRAM.
   A session appears only if it is Accepted AND the speaker has been informed.
   A talk the committee accepted this morning but hasn't told anyone about is
   not part of the program yet — publishing it would tell the speaker by
   accident, through a JSON file, which is the worst possible way to hear.

   And private fields NEVER leave the building. `notes-to-committee` is absent
   from every export and every API response, authenticated or not."
  (:require [clojure.data.json :as json]
            [clojure.string :as str]
            [taoensso.timbre :as log]
            [cfp-scheduler-killer.store :as store]
            [cfp-scheduler-killer.submissions :as submissions])
  (:import (java.time LocalDate ZoneId)
           (java.time.format DateTimeFormatter)))

(def ical-domain "cfp-scheduler-killer.local")

;; --- The publication gate ---------------------------------------------------

(defn published?
  "Accepted AND communicated. Both halves, always."
  [submission]
  (and (= "Accepted" (:status submission))
       (some? (:notified-at submission))))

(defn published-sessions
  [event-id]
  (->> (store/submissions-for-event event-id)
       (filter published?)
       (sort-by :created-at)
       vec))

(defn publishable-sessions
  "The PUBLIC gate: published-sessions minus any session in a KNOWN scheduling
   conflict (Gene's ruling, 2026-08-09). A partial state — room TBD, time
   undecided — publishes fine (doctrine #6); a conflict is not undecided, it
   is known wrong, and the agenda is the artifact attendees plan their day
   around, so conflicted sessions are withheld until resolved. Never silent:
   the organizer's schedule page counts what is held. The organizer-side
   pools (schedule/schedulable, the authed API) deliberately still see
   everything — you cannot fix a conflict you cannot see.
   requiring-resolve because schedule requires exports: the conflict set is
   resolved at runtime to avoid a load cycle."
  [event]
  (let [conflicted (into #{} ((requiring-resolve
                               'cfp-scheduler-killer.schedule/conflicted-submission-ids)
                              event))
        sessions (published-sessions (:id event))
        public (filterv #(not (contains? conflicted (:id %))) sessions)]
    (when (< (count public) (count sessions))
      (log/debug :withholding-conflicted-sessions
                 :event-id (:id event) :held (- (count sessions) (count public))))
    public))

(defn public-answers
  "Answers with every `:private true` field removed, per the submission's own
   snapshot. Driven by the field defs so a newly-added private field is excluded
   the day it is added, with no export code to remember to change."
  [submission]
  (let [private-ids (into #{}
                          (comp (filter :private)
                                (map #(keyword (name (:id %)))))
                          (:form-snapshot submission))]
    (apply dissoc (:answers submission) private-ids)))

;; --- Schedule version -------------------------------------------------------

(defn schedule-version
  "Monotonic version for this event: the number of stored events that touch it.

   Any change — a rating, an edit, an acceptance — bumps it, so a consumer can
   cache on this number and know a bump means 'something moved'. It is derived,
   never stored, so it cannot drift from reality (ai.engineer's own is 4945,
   i.e. republished constantly)."
  [event-id]
  (count (store/log-for-event event-id)))

;; --- Formatting helpers -----------------------------------------------------

(def ^:private date-fmt (DateTimeFormatter/ofPattern "MMM d, yyyy"))
(def ^:private day-month-fmt (DateTimeFormatter/ofPattern "MMM d"))

(defn date-range-string
  [event]
  (let [s (:starts-on event) e (:ends-on event)]
    (cond
      (and s e (= s e)) (.format ^LocalDate s date-fmt)
      (and s e (= (.getYear ^LocalDate s) (.getYear ^LocalDate e)))
      (str (.format ^LocalDate s day-month-fmt) "–"
           (.format ^LocalDate e date-fmt))
      (and s e) (str (.format ^LocalDate s date-fmt) " – " (.format ^LocalDate e date-fmt))
      s (.format ^LocalDate s date-fmt)
      :else "")))

(defn- session-type
  "ai.engineer's vocabulary is session|workshop|keynote|sponsor. Our seed form
   asks for a FORMAT (Experience Report / SME talk / Panel), which is a
   different question, so everything maps to \"session\" until an event's form
   actually distinguishes workshops and keynotes. Guessing would put wrong
   values into someone else's published site."
  [answers]
  (let [fmt (str/lower-case (str (:session-format answers)))]
    (cond
      (str/includes? fmt "workshop") "workshop"
      (str/includes? fmt "keynote") "keynote"
      :else "session")))

(defn- speaker-names [submission]
  (mapv #(str (:name %)) (:speakers submission)))

;; --- Stable identity --------------------------------------------------------
;;
;; The whole reason this API exists. Ask three ITRev systems how many talks Jason
;; Cox has given and you get 6, 9 and 12 (docs/research/post-conference-corpus-
;; survey.md) — all live, none wrong, because every join in the estate is a fuzzy
;; match on a display-name string. So: an id on EVERY entity in EVERY payload,
;; and the id on BOTH sides of every join (a session carries speakerIds, a
;; speaker carries sessionIds). A consumer should never have to match "Jason Cox"
;; against "Jason Cox " to build a graph.

(defn speaker-ids
  "The person ids behind a submission's speakers. Person id, not submission id:
   the same human across two conferences is the same id, which is exactly the
   fact the corpus survey found missing."
  [submission]
  (mapv #(str (:person-id %)) (:speakers submission)))

(defn room-name
  [event-id room-id]
  (when room-id
    (:name (first (filter #(= room-id (:id %)) (store/rooms-for-event event-id))))))

(defn event-day-strings
  "Every day of the event as an ISO string. Duplicated from
   cfp-scheduler-killer.schedule for the same reason `minutes->display` is —
   schedule depends on THIS namespace for the publication gate."
  [event]
  (let [s (:starts-on event) e (or (:ends-on event) (:starts-on event))]
    (if (and s e (not (.isAfter ^LocalDate s ^LocalDate e)))
      (loop [d s acc []]
        (if (.isAfter ^LocalDate d ^LocalDate e)
          acc
          (recur (.plusDays ^LocalDate d 1) (conj acc (str d)))))
      [])))

;; --- sessions.json ----------------------------------------------------------

(defn- minutes->display
  "9:00am. Duplicated from cfp-scheduler-killer.schedule rather than required,
   because schedule depends on THIS namespace for the publication gate and a
   cycle would be a worse trade than eight lines of formatting."
  [m]
  (when m
    (let [h (int (/ m 60)) mm (int (mod m 60))
          ampm (if (< h 12) "am" "pm")
          h12 (cond (zero? h) 12 (> h 12) (- h 12) :else h)]
      (str h12 ":" (format "%02d" mm) ampm))))

(defn- day-index-label
  "\"Day 1 — Oct 14\", the shape ai.engineer publishes."
  [event day-str]
  (when-let [d (try (LocalDate/parse (str day-str)) (catch Exception _ nil))]
    (let [start (:starts-on event)
          idx (when start (inc (.between java.time.temporal.ChronoUnit/DAYS start d)))]
      (str (when (and idx (pos? idx)) (str "Day " idx " — "))
           (.format d (DateTimeFormatter/ofPattern "MMM d"))))))

(defn placement-fields
  "day/time/room for a session — EMPTY until it is actually placed. An empty
   string is honest; an invented time would propagate into someone else's
   published site."
  [event submission]
  (let [slot (store/slot-for (:id submission))
        room (room-name (:id event) (:room-id slot))]
    (if-not slot
      {"day" "" "time" "" "room" ""}
      {"day" (or (day-index-label event (:day slot)) "")
       "time" (str (minutes->display (:start slot))
                   (when (:end slot) (str "-" (minutes->display (:end slot)))))
       "room" (or room "")})))

(defn session-entry
  "One session in ai.engineer's shape — plus the two keys ai.engineer's own feed
   does not have: `id` and `speakerIds`.

   ai.engineer joins sessions to speakers on the display-name string, which is
   why we emit the names too and in the same place; extra keys are additive, so
   a consumer built against their feed keeps working byte-for-byte. But a
   consumer built against OURS never has to fuzzy-match a human being."
  [event submission]
  (let [answers (public-answers submission)]
    (merge
     (placement-fields event submission)
     {"id" (:id submission)
      "speakerIds" (speaker-ids submission)
      "title" (str (:talk-title answers))
      "description" (str (:abstract answers))
      "type" (session-type answers)
      ;; We have no track field (single form, per swyx's Q&A); industry is the
      ;; closest real signal an organizer has already collected.
      "track" (str (or (:industry answers) ""))
      "status" "confirmed"
      "speakers" (speaker-names submission)})))

(defn sessions-json-data
  [event]
  (let [sessions (publishable-sessions event)]
    {"conference" (str (:name event))
     "dates" (date-range-string event)
     "location" (str (or (:location event) ""))
     "website" (str (or (:website-url event) ""))
     "scheduleVersion" (schedule-version (:id event))
     "totalSessions" (count sessions)
     "sessions" (mapv #(session-entry event %) sessions)}))

;; --- speakers.json ----------------------------------------------------------

(defn speaker-entry
  "The speaker as PUBLISHED: what they submitted, overlaid with anything they
   have since maintained in their profile. The profile wins because it is the
   version they keep current.

   `id` is the PERSON id — stable across conferences and across a name change —
   and `sessionIds` closes the join from this side, so a consumer can walk
   speaker -> sessions without ever comparing two title strings."
  [submission]
  (let [sp (first (:speakers submission))
        profile (:profile (store/person-by-id (:person-id sp)))
        pick (fn [& vs] (first (remove str/blank? (map str (remove nil? vs)))))]
    {"id" (str (:person-id sp))
     "sessionIds" [(:id submission)]
     "name" (str (:name sp))
     "tagline" (or (pick (:tagline profile) (:title sp)) "")
     "org" (str (or (:org sp) ""))
     "bio" (or (pick (:bio profile) (:bio sp)) "")
     "headshot" (or (pick (:headshot-url profile) (:headshot-url sp)) "")
     "links" (vec (remove nil?
                          [(when-let [u (pick (:linkedin-url profile) (:linkedin-url sp))]
                             {"label" "LinkedIn" "url" u})
                           (when-let [u (pick (:website-url profile))]
                             {"label" "Website" "url" u})
                           (when-let [u (pick (:sessionize-url sp))]
                             {"label" "Sessionize" "url" u})]))
     "sessions" [(str (:talk-title (public-answers submission)))]}))

(defn aggregate-speakers
  "One entry per PERSON, even when they have two talks — merged on the person id
   rather than on the name, which is the whole point. Insertion-ordered so the
   payload is stable between calls."
  [sessions]
  (let [order (vec (distinct (map #(:person-id (first (:speakers %))) sessions)))
        by-person (reduce (fn [acc s]
                            (let [pid (:person-id (first (:speakers s)))]
                              (if-let [existing (get acc pid)]
                                (assoc acc pid
                                       (-> existing
                                           (update "sessions" conj
                                                   (str (:talk-title (public-answers s))))
                                           (update "sessionIds" conj (:id s))))
                                (assoc acc pid (speaker-entry s)))))
                          {}
                          sessions)]
    ;; A map with more than eight keys is unordered; the order vector is how the
    ;; payload stays byte-stable between two calls with the same data.
    (mapv by-person order)))

(defn speakers-json-data
  [event]
  (let [speakers (aggregate-speakers (publishable-sessions event))]
    {"conference" (str (:name event))
     "totalSpeakers" (count speakers)
     "speakers" speakers}))

;; --- calendar.ics -----------------------------------------------------------

(defn ics-escape
  "RFC 5545 §3.3.11: backslash, semicolon, comma and newline are special."
  [s]
  (-> (str s)
      (str/replace "\\" "\\\\")
      (str/replace ";" "\\;")
      (str/replace "," "\\,")
      (str/replace #"\r?\n" "\\\\n")))

(defn fold-line
  "RFC 5545 §3.1: lines wrap at 75 octets, continuations start with a space."
  [line]
  (if (<= (count line) 75)
    line
    (str/join "\r\n "
              (loop [s line acc []]
                (if (<= (count s) 74)
                  (conj acc s)
                  (recur (subs s 74) (conj acc (subs s 0 74))))))))

(defn- ics-date [^LocalDate d]
  (when d (.format d (DateTimeFormatter/ofPattern "yyyyMMdd"))))

(defn- ics-stamp []
  (.format (.atZone (java.time.Instant/now) (ZoneId/of "UTC"))
           (DateTimeFormatter/ofPattern "yyyyMMdd'T'HHmmss'Z'")))

(defn ics-uid
  "STABLE for the life of the submission. An amendment must update the existing
   calendar entry, never create a second one — the duplicate-invite problem is
   the single most-hated behaviour of every conference tool we have used."
  [submission]
  (str (:id submission) "@" ical-domain))

(defn ics-sequence
  "RFC 5545 SEQUENCE: bumps every time this session changes, so a client knows
   the invite is an amendment. Derived from the number of stored events that
   touched this submission."
  [submission]
  (count (filter (fn [e]
                   (= (:id submission) (get-in e [:payload :submission-id])))
                 (store/log-for-event (:event-id submission)))))

(defn- ics-local-datetime
  "yyyyMMddTHHmmss — a FLOATING local time. The event's zone is published in the
   calendar header; floating times mean an attendee's phone shows the program in
   the room's clock, which is what a printed agenda means."
  [^LocalDate d minutes]
  (when (and d minutes)
    (str (.format d (DateTimeFormatter/ofPattern "yyyyMMdd"))
         "T" (format "%02d%02d00" (int (/ minutes 60)) (int (mod minutes 60))))))

(defn vevent
  [event submission]
  (let [answers (public-answers submission)
        slot (store/slot-for (:id submission))
        slot-day (when slot (try (LocalDate/parse (str (:day slot))) (catch Exception _ nil)))
        room (room-name (:id event) (:room-id slot))
        start (:starts-on event)
        end (:ends-on event)
        ;; DTEND is EXCLUSIVE for all-day events, so the last day needs +1.
        end+1 (some-> (or end start) (.plusDays 1))
        ;; A PLACED session gets its real time; an unplaced one falls back to
        ;; all-day across the event, so the invite still exists and still has a
        ;; stable UID to amend later.
        placed? (and slot-day (:start slot))]
    (->> [(str "BEGIN:VEVENT")
          (str "UID:" (ics-uid submission))
          (str "DTSTAMP:" (ics-stamp))
          (str "SEQUENCE:" (ics-sequence submission))
          (if placed?
            (str "DTSTART:" (ics-local-datetime slot-day (:start slot)))
            (when start (str "DTSTART;VALUE=DATE:" (ics-date start))))
          (if placed?
            (str "DTEND:" (ics-local-datetime slot-day (or (:end slot)
                                                           (+ 30 (:start slot)))))
            (when end+1 (str "DTEND;VALUE=DATE:" (ics-date end+1))))
          (str "SUMMARY:" (ics-escape (:talk-title answers)))
          (str "DESCRIPTION:" (ics-escape
                               (str (:abstract answers)
                                    "\n\nSpeaker: " (str/join ", " (speaker-names submission)))))
          ;; Room when we know it, event location otherwise — the late-room
          ;; assignment lands in the invite as an amendment, not a new event.
          (when-let [loc (or (when room (str room (when (:location event)
                                                    (str ", " (:location event)))))
                             (:location event))]
            (str "LOCATION:" (ics-escape loc)))
          (str "STATUS:CONFIRMED")
          (str "END:VEVENT")]
         (remove nil?)
         (map fold-line))))

(defn submission-ics
  "A single-VEVENT calendar for ONE session — what gets attached to an
   acceptance letter. Deliberately built from the SAME `vevent` as the public
   feed, so the UID and SEQUENCE an attendee receives by email are byte-identical
   to the ones in the subscribed feed. That is what makes a later room
   assignment amend the invite instead of creating a second one."
  [event submission]
  (let [lines (concat ["BEGIN:VCALENDAR"
                       "VERSION:2.0"
                       (str "PRODID:-//cfp-scheduler-killer//" (ics-escape (:name event)) "//EN")
                       "CALSCALE:GREGORIAN"
                       "METHOD:PUBLISH"]
                      (vevent event submission)
                      ["END:VCALENDAR"])]
    (str (str/join "\r\n" lines) "\r\n")))

(defn calendar-ics
  [event]
  (let [lines (concat ["BEGIN:VCALENDAR"
                       "VERSION:2.0"
                       (str "PRODID:-//cfp-scheduler-killer//" (ics-escape (:name event)) "//EN")
                       "CALSCALE:GREGORIAN"
                       "METHOD:PUBLISH"
                       (str "X-WR-CALNAME:" (ics-escape (:name event)))]
                      (mapcat #(vevent event %) (publishable-sessions event))
                      ["END:VCALENDAR"])]
    ;; CRLF throughout, and a trailing CRLF — some clients are strict.
    (str (str/join "\r\n" lines) "\r\n")))

;; --- llms.txt ---------------------------------------------------------------

(defn llms-txt
  "A markdown index for machines that read the web. Points at the structured
   exports rather than trying to be them."
  [event base-url]
  (let [sessions (publishable-sessions event)
        e (fn [& parts] (apply str parts))]
    (str/join
     "\n"
     (remove nil?
             [(e "# " (:name event))
              ""
              (when-let [d (not-empty (date-range-string event))] (e d))
              (when (:location event) (e (:location event)))
              (when (:website-url event) (e (:website-url event)))
              ""
              (e "> Program data for " (:name event)
                 ". " (count sessions) " confirmed session"
                 (when (not= 1 (count sessions)) "s") ".")
              ""
              "## Data"
              ""
              (e "- [sessions.json](" base-url "/events/" (:slug event) "/exports/sessions.json)"
                 ": every confirmed session — title, description, type, track, speakers.")
              (e "- [speakers.json](" base-url "/events/" (:slug event) "/exports/speakers.json)"
                 ": confirmed speakers — bio, tagline, org, headshot, links.")
              (e "- [calendar.ics](" base-url "/events/" (:slug event) "/exports/calendar.ics)"
                 ": the program as a calendar feed with stable UIDs.")
              ""
              "## API"
              ""
              (e "- [API reference](" base-url "/api/v1/events/" (:slug event) "/docs)"
                 ": every endpoint, with a curl line for each.")
              (e "- [event](" base-url "/api/v1/events/" (:slug event) ")"
                 ": ids, dates, timezone, CFP state, and links to everything below.")
              (e "- [sessions](" base-url "/api/v1/events/" (:slug event) "/sessions)"
                 ": the program with `id` and `speakerIds` on every row.")
              (e "- [speakers](" base-url "/api/v1/events/" (:slug event) "/speakers)"
                 ": one row per PERSON, with `id` and `sessionIds`.")
              (e "- [schedule](" base-url "/api/v1/events/" (:slug event) "/schedule)"
                 ": days, rooms and placements, joined by id.")
              ""
              "## Call for speakers"
              ""
              (e "- [Submit a talk](" base-url "/cfp/" (:slug event) ")")
              ""
              "## Notes"
              ""
              "- Only sessions that are accepted AND whose speakers have been"
              "  informed appear here. Nothing is published before the speaker knows."
              "- Day, time and room are empty until the schedule is built; they will"
              "  populate in place, and `scheduleVersion` will bump."
              "- Every entity carries a stable `id`, and every join carries the id on"
              "  BOTH sides (`speakerIds` on a session, `sessionIds` on a speaker)."
              "  Never match on a name or a title — the ids do not change."
              (e "- scheduleVersion: " (schedule-version (:id event)))
              ""]))))

;; --- JSON ------------------------------------------------------------------

(defn ->json [data] (json/write-str data :escape-slash false))

(comment
  (store/load!)
  (let [e (store/get-event-by-slug "enterprise-ai-summit-charlotte")]
    (sessions-json-data e)
    (print (calendar-ics e))))

;; --- Webhook registration ---------------------------------------------------
;;
;; Registered webhooks are stored as events like everything else, so "who added
;; this integration, and when" is answerable from the Log.

(defn register-webhook!
  "Add a webhook to an event. `types` is a vector of event-type strings; empty
   means every event."
  [event url types actor]
  (let [url (str/trim (str url))]
    (when (re-matches #"^https?://[^\s/$.?#][^\s]*$" url)
      (let [id (store/new-id)]
        (store/append! {:type "sink.registered" :actor actor :event-id (:id event)
                        :payload {:id id
                                  :event-id (:id event)
                                  :url url
                                  :types (vec (remove str/blank? types))
                                  :created-at (store/now-iso)}})
        id))))

(defn remove-webhook!
  [event sink-id actor]
  (store/append! {:type "sink.removed" :actor actor :event-id (:id event)
                  :payload {:id sink-id :event-id (:id event)
                            :at (store/now-iso)}})
  sink-id)

(defn webhooks-for
  [event]
  (vec (vals (get-in event [:settings :webhooks]))))

;; --- REST API shapes --------------------------------------------------------
;;
;; The API is richer than the static exports (ids, statuses, full answers) but
;; obeys the SAME private-field rule. Authentication buys you unpublished rows;
;; it never buys you the committee's private notes. `notes-to-committee` was
;; promised to the speaker as PC-only, and a token doesn't change who promised.

(defn api-placement
  "Where a session sits on the grid — ids AND display strings, because a
   consumer building a table wants \"9:00am\", and a consumer building a
   calendar wants the integer. Absent placement is `placed: false` with nulls,
   never an invented time (doctrine #6: partial states are first-class)."
  [event submission]
  (let [slot (store/slot-for (:id submission))]
    (if-not slot
      {"placed" false "day" nil "dayLabel" nil
       "startMinute" nil "endMinute" nil "start" nil "end" nil
       "roomId" nil "room" nil}
      {"placed" true
       "day" (str (:day slot))
       "dayLabel" (or (day-index-label event (:day slot)) "")
       "startMinute" (:start slot)
       "endMinute" (:end slot)
       "start" (minutes->display (:start slot))
       "end" (minutes->display (:end slot))
       "roomId" (:room-id slot)
       "room" (or (room-name (:id event) (:room-id slot)) nil)})))

(defn api-session
  "One submission as the API returns it — every entity carrying its id.

   `status` and `notified` are here on purpose: the seven-valued status and the
   separate notified flag ARE the model (doctrine #3), and a scraper that can
   see them never has to infer 'is this real yet' from whether a row appeared.
   Public callers only ever reach published rows, so what they see is always
   Accepted + notified; a token is what shows you the rest of the funnel."
  ([submission] (api-session (store/get-event-by-id (:event-id submission)) submission))
  ([event submission]
   (let [answers (public-answers submission)]
     {"id" (:id submission)
      "eventId" (:event-id submission)
      "title" (str (:talk-title answers))
      "status" (:status submission)
      "published" (published? submission)
      "notified" (some? (:notified-at submission))
      "notifiedAt" (some-> (:notified-at submission) str)
      "source" (:source submission)
      "createdAt" (some-> (:created-at submission) str)
      "type" (session-type answers)
      "track" (str (or (:industry answers) ""))
      ;; public-answers, ALWAYS — this is the authenticated path too.
      "answers" (into {} (map (fn [[k v]] [(name k) v])) answers)
      "speakerIds" (speaker-ids submission)
      "schedule" (when event (api-placement event submission))
      "speakers" (mapv (fn [sp]
                         {"id" (str (:person-id sp))
                          "name" (str (:name sp))
                          "org" (str (or (:org sp) ""))
                          "title" (str (or (:title sp) ""))})
                       (:speakers submission))})))

(defn api-sessions
  "`status` nil => the program (the default a public caller gets).
   `:all` => every submission, any status (token holders only).
   A status STRING => just that status (token holders only).

   The nil default differs by caller for one reason: an unauthenticated caller
   gets `publishable-sessions` — the same conflict-withholding gate as
   sessions.json — because /api/v1 with no token IS a public surface. An
   authenticated caller gets `published-sessions`, conflicts included, because
   you cannot fix a conflict you cannot see (commit 7508ff9)."
  ([event status] (api-sessions event status false))
  ([event status authed?]
   (let [rows (cond
                (= :all status) (vec (store/submissions-for-event (:id event)))
                (string? status) (filterv #(= status (:status %))
                                          (store/submissions-for-event (:id event)))
                authed? (published-sessions (:id event))
                :else (publishable-sessions event))]
     {"event" {"id" (:id event) "slug" (:slug event) "name" (:name event)}
      "scheduleVersion" (schedule-version (:id event))
      "total" (count rows)
      ;; Everything is returned in one response at our scale; saying so beats a
      ;; consumer writing pagination that will never fire.
      "pagination" {"paginated" false
                    "note" "All results are returned in a single response."}
      "sessions" (mapv #(api-session event %) rows)})))

(defn api-speakers
  "One row per PERSON. `all?` (token holders only) widens from the published
   program to everyone who has submitted."
  ([event] (api-speakers event false))
  ([event all?]
   (let [rows (if all?
                (vec (store/submissions-for-event (:id event)))
                (publishable-sessions event))
         speakers (aggregate-speakers rows)]
     {"event" {"id" (:id event) "slug" (:slug event) "name" (:name event)}
      "total" (count speakers)
      "pagination" {"paginated" false
                    "note" "All results are returned in a single response."}
      "speakers" speakers})))

(defn api-speaker
  "One speaker by PERSON id, with every session of theirs the caller may see.
   nil when this person has nothing visible here — the handler turns that into a
   404, so an unpublished speaker is indistinguishable from an unknown one."
  [event person-id all?]
  (let [pid (str person-id)
        rows (filterv (fn [s] (some #(= pid (str (:person-id %))) (:speakers s)))
                      (if all?
                        (vec (store/submissions-for-event (:id event)))
                        (publishable-sessions event)))]
    (when (seq rows)
      {"event" {"id" (:id event) "slug" (:slug event) "name" (:name event)}
       "speaker" (first (aggregate-speakers rows))})))

(defn api-rooms
  [event]
  {"event" {"id" (:id event) "slug" (:slug event) "name" (:name event)}
   "total" (count (store/rooms-for-event (:id event)))
   "rooms" (mapv (fn [r] {"id" (:id r) "name" (str (:name r)) "order" (:order r)})
                 (store/rooms-for-event (:id event)))})

(defn api-schedule
  "The grid, joined by id on every edge: an item names its `sessionId` and its
   `roomId`, and the rooms are listed once at the top. Blocks (breaks, keynote
   holds, lunch) are first-class items, not gaps to be inferred.

   Public callers see the publishable program; a token additionally shows
   placements of sessions that are not published yet — which is the whole
   difference between 'the agenda' and 'the working grid'."
  [event authed?]
  (let [event-id (:id event)
        subs (into {} (map (juxt :id identity)) (store/submissions-for-event event-id))
        visible (into #{} (map :id) (if authed?
                                      (published-sessions event-id)
                                      (publishable-sessions event)))
        slots (filterv #(contains? visible (:submission-id %))
                       (store/slots-for-event event-id))
        placed-ids (into #{} (map :submission-id) slots)
        blocks (store/blocks-for-event event-id)
        item (fn [slot]
               (let [sub (get subs (:submission-id slot))]
                 {"kind" "session"
                  "sessionId" (:submission-id slot)
                  "title" (str (:talk-title (public-answers sub)))
                  "day" (str (:day slot))
                  "startMinute" (:start slot)
                  "endMinute" (:end slot)
                  "start" (minutes->display (:start slot))
                  "end" (minutes->display (:end slot))
                  "roomId" (:room-id slot)
                  "room" (room-name event-id (:room-id slot))
                  "speakerIds" (speaker-ids sub)
                  "speakers" (speaker-names sub)}))
        block-item (fn [b]
                     {"kind" "block"
                      "blockId" (:id b)
                      "title" (str (:label b))
                      "day" (str (:day b))
                      "startMinute" (:start b)
                      "endMinute" (:end b)
                      "start" (minutes->display (:start b))
                      "end" (minutes->display (:end b))
                      "roomId" (:room-id b)
                      "room" (room-name event-id (:room-id b))})
        days (or (seq (event-day-strings event))
                 (distinct (map #(str (:day %)) slots)))]
    {"event" {"id" (:id event) "slug" (:slug event) "name" (:name event)}
     "scheduleVersion" (schedule-version event-id)
     "timezone" (:tz event)
     "locked" (boolean (get-in event [:settings :schedule-lock :locked?]))
     "rooms" (get (api-rooms event) "rooms")
     "days" (mapv (fn [d]
                    {"date" d
                     "label" (or (day-index-label event d) d)
                     "items" (->> (concat (map item (filter #(= d (str (:day %))) slots))
                                          (map block-item (filter #(= d (str (:day %))) blocks)))
                                  (sort-by (juxt #(or (get % "startMinute") 0)
                                                 #(str (get % "title"))))
                                  vec)})
                  days)
     ;; Named, not omitted: "which accepted talks still have no home" is a
     ;; question the grid should answer out loud.
     "unscheduled" (mapv (fn [id] {"sessionId" id
                                   "title" (str (:talk-title (public-answers (get subs id))))})
                         (remove placed-ids visible))}))

(defn api-changes
  "An incremental change feed — the answer to 'do I have to re-download the
   whole program to find out what moved'. `seq` is monotonic and matches
   `scheduleVersion`, so `?since=<last seq you saw>` is a complete sync
   protocol in one query parameter.

   IDS ONLY. No payload bodies, no actor emails, no answers — a change feed says
   WHAT changed and WHEN, and the caller re-reads the entity it names. That also
   means this endpoint can never become a second, unreviewed way for a private
   field to leave the building."
  [event since]
  (let [log (store/log-for-event (:id event))
        since (max 0 (long (or since 0)))
        rows (->> log
                  (map-indexed (fn [i e] [(inc i) e]))
                  (drop since))]
    {"event" {"id" (:id event) "slug" (:slug event) "name" (:name event)}
     "scheduleVersion" (count log)
     "since" since
     "total" (count rows)
     "changes" (mapv (fn [[n e]]
                       (let [p (:payload e)]
                         {"seq" n
                          "type" (:type e)
                          "at" (str (:at e))
                          "id" (:id p)
                          "submissionId" (:submission-id p)
                          "personId" (:person-id p)
                          "roomId" (:room-id p)}))
                     rows)}))

(defn api-event
  "The discovery document: ids, dates, timezone, CFP state, counts, and a link
   to every other endpoint. One GET and a consumer knows the whole surface —
   which is precisely what fifteen years of scraping Sessionize did not have."
  [event base authed?]
  (let [slug (:slug event)
        api (str base "/api/v1/events/" slug)
        ex (fn [f] (str base "/events/" slug "/exports/" f))]
    {"id" (:id event)
     "slug" slug
     "name" (str (:name event))
     "startsOn" (some-> (:starts-on event) str)
     "endsOn" (some-> (:ends-on event) str)
     "dates" (date-range-string event)
     "timezone" (:tz event)
     "location" (str (or (:location event) ""))
     "website" (str (or (:website-url event) ""))
     "scheduleVersion" (schedule-version (:id event))
     "cfp" {"state" (name (submissions/cfp-state event))
            "url" (str base "/cfp/" slug)
            "opensAt" (some-> (:cfp-opens-at event) str)
            "closesAt" (some-> (:cfp-closes-at event) str)}
     "counts" (cond-> {"publishedSessions" (count (publishable-sessions event))
                       "publishedSpeakers" (count (aggregate-speakers
                                                   (publishable-sessions event)))
                       "rooms" (count (store/rooms-for-event (:id event)))}
                ;; How many proposals an event received is the organizer's
                ;; business until the organizer says otherwise.
                authed? (assoc "submissions"
                               (count (store/submissions-for-event (:id event)))))
     "links" {"self" api
              "docs" (str api "/docs")
              "sessions" (str api "/sessions")
              "submissions" (str api "/submissions")
              "speakers" (str api "/speakers")
              "schedule" (str api "/schedule")
              "rooms" (str api "/rooms")
              "changes" (str api "/changes")
              "sessionsJson" (ex "sessions.json")
              "speakersJson" (ex "speakers.json")
              "calendarIcs" (ex "calendar.ics")
              "llmsTxt" (str base "/events/" slug "/llms.txt")
              "agenda" (str base "/agenda/" slug)}}))

(def api-endpoints
  "The endpoint table, as DATA — one source of truth for the service index, the
   docs page and the tests. A route that is not in this vector is a route the
   docs page cannot forget to document, because the docs page is generated from
   it."
  [{:method "GET" :path "/api/v1"
    :auth :public
    :summary "This index: what the service is and where the endpoints are."}
   {:method "GET" :path "/api/v1/events/{slug}"
    :auth :public
    :summary "Event detail — ids, dates, timezone, CFP state, and a link to every other endpoint."
    :notes "With a token, `counts.submissions` is included."}
   {:method "GET" :path "/api/v1/events/{slug}/docs"
    :auth :public
    :summary "This page."}
   {:method "GET" :path "/api/v1/events/{slug}/sessions"
    :auth :public
    :summary "The published program. Every row carries `id` and `speakerIds`."
    :params [["status" "all, or one of Accepted / Accept Queue / Pending / Decline Queue / Declined / Withdrawn / Draft. Requires a token."]]}
   {:method "GET" :path "/api/v1/events/{slug}/submissions"
    :auth :token
    :summary "Every submission in every state, with `status`, `notified` and `notifiedAt`."
    :params [["status" "Filter to one status."]]}
   {:method "GET" :path "/api/v1/events/{slug}/submissions/{id}"
    :auth :public
    :summary "One submission in full. Unpublished rows require a token."}
   {:method "GET" :path "/api/v1/events/{slug}/speakers"
    :auth :public
    :summary "One row per PERSON, with `id` and `sessionIds`."
    :params [["status" "all — every submitter, not just the published ones. Requires a token."]]}
   {:method "GET" :path "/api/v1/events/{slug}/speakers/{personId}"
    :auth :public
    :summary "One speaker and all of their visible sessions."}
   {:method "GET" :path "/api/v1/events/{slug}/schedule"
    :auth :public
    :summary "Days, rooms, placements and blocks — joined by `sessionId` and `roomId`, plus what is still unscheduled."}
   {:method "GET" :path "/api/v1/events/{slug}/rooms"
    :auth :public
    :summary "The rooms, with stable ids."}
   {:method "GET" :path "/api/v1/events/{slug}/changes"
    :auth :token
    :summary "Incremental change feed — ids only, monotonic `seq`."
    :params [["since" "The last `seq` you processed. Default 0."]]}
   {:method "GET" :path "/events/{slug}/exports/sessions.json"
    :auth :public :summary "ai.engineer-shaped sessions feed (plus `id` and `speakerIds`)."}
   {:method "GET" :path "/events/{slug}/exports/speakers.json"
    :auth :public :summary "ai.engineer-shaped speakers feed (plus `id` and `sessionIds`)."}
   {:method "GET" :path "/events/{slug}/exports/calendar.ics"
    :auth :public :summary "The program as a calendar feed. UIDs are stable — a late room assignment amends the invite."}
   {:method "GET" :path "/events/{slug}/llms.txt"
    :auth :public :summary "A markdown index of everything above, for an agent."}])

(defn api-index
  "GET /api/v1 — the root. A scraper's first request should teach it the whole
   surface, including the parts it is not allowed to read yet."
  [base]
  {"service" "cfp-scheduler-killer"
   "apiVersion" "v1"
   "description" (str "Read API for conference programs. Every entity carries a stable id, "
                      "and every join carries the id on both sides. Never match on a name.")
   "auth" {"scheme" "Bearer token, or ?token="
           "header" "Authorization: Bearer <token>"
           "note" (str "A token widens what you can read (unpublished rows, statuses, the "
                       "change feed). It never unlocks private fields: answers marked private "
                       "on the event's form — notes to the committee — are absent from every "
                       "response, authenticated or not.")}
   "conventions" {"ids" "Opaque, stable, and never reused. Person ids are stable across events."
                  "pagination" "None. Everything is returned in one response at conference scale."
                  "caching" "Every response carries an ETag; send If-None-Match and get a 304."
                  "versioning" (str "`scheduleVersion` bumps on any change to the event. "
                                    "Poll it, or use /changes?since= for what moved.")
                  "publication" (str "Public responses describe the PUBLISHED program: accepted "
                                     "AND the speaker has been informed, minus anything in a known "
                                     "scheduling conflict.")}
   "endpoints" (mapv (fn [{:keys [method path auth summary]}]
                       {"method" method "path" path
                        "auth" (name auth) "summary" summary})
                     api-endpoints)})

;; --- API keys ---------------------------------------------------------------
;;
;; One token per event was fine until two things needed access: revoking the
;; Zapier integration meant revoking the conference chair's curl one-liner too,
;; so nobody revoked anything. Named keys make revocation a small act instead of
;; an outage, and the label is what turns "some key leaked" into "THAT key
;; leaked". The original [:settings :api-token] keeps working — an event created
;; before this shipped must not lose its integrations.

(def ^:private key-bytes
  "24 random bytes -> 32 base64url characters. Long enough that guessing is not
   a threat model; short enough to paste into a curl line by hand."
  24)

(defn new-key-material
  "A fresh secret from SecureRandom. `random-uuid` (what the legacy api-token
   used) is NOT a secret generator — java.util.UUID is not guaranteed to be
   cryptographically random beyond its version-4 bits."
  []
  (let [buf (byte-array key-bytes)]
    (.nextBytes (java.security.SecureRandom.) buf)
    (.encodeToString (.withoutPadding (java.util.Base64/getUrlEncoder)) buf)))

(defn key-prefix
  "What the LIST shows: enough to recognise a key you are holding, never enough
   to use one you are not. The full value is shown exactly once, at creation."
  [k]
  (let [k (str k)]
    (str (subs k 0 (min 8 (count k))) "…")))

(defn api-keys-for
  "Live (non-revoked) keys for an event, oldest first."
  [event]
  (->> (vals (get-in event [:settings :api-keys]))
       (sort-by :created-at)
       vec))

(defn create-api-key!
  "Mint a named key. Returns the key map INCLUDING :key — the only moment the
   caller ever sees the material, because the settings page shows it once and
   then only ever shows the prefix."
  [event label actor]
  (let [row {:id (store/new-id)
             :event-id (:id event)
             :label (or (not-empty (str/trim (str label))) "Unnamed key")
             :key (new-key-material)
             :created-by actor
             :created-at (store/now-iso)}]
    (store/append! {:type "api-key.created" :actor actor :event-id (:id event)
                    :payload row})
    ;; Deliberately NOT logged with the material — an access log is the one
    ;; place a secret must never land.
    (log/info :api-key-created :event-id (:id event) :label (:label row)
              :key-id (:id row))
    row))

(defn revoke-api-key!
  [event key-id actor]
  (when-let [row (get-in event [:settings :api-keys key-id])]
    (store/append! {:type "api-key.revoked" :actor actor :event-id (:id event)
                    :payload {:id key-id :event-id (:id event)
                              :label (:label row)
                              :at (store/now-iso)}})
    (log/info :api-key-revoked :event-id (:id event) :key-id key-id
              :label (:label row))
    key-id))

(defn- same-secret?
  "Constant-time compare. A token check that short-circuits on the first wrong
   byte is a token check that can be walked one byte at a time."
  [expected given]
  (java.security.MessageDigest/isEqual
   (.getBytes (str expected) "UTF-8")
   (.getBytes (str given) "UTF-8")))

(defn valid-token?
  "The legacy per-event token, or any live named key. Both are secrets for the
   same event and buy exactly the same thing."
  [event token]
  (let [token (str token)]
    (and (not (str/blank? token))
         (boolean
          (or (when-let [legacy (get-in event [:settings :api-token])]
                (and (not (str/blank? (str legacy)))
                     (same-secret? legacy token)))
              (some (fn [k]
                      (and (not (str/blank? (str (:key k))))
                           (same-secret? (:key k) token)))
                    (vals (get-in event [:settings :api-keys]))))))))

(defn bearer-token
  "Accept either `Authorization: Bearer <t>` or `?token=<t>`."
  [req]
  (or (some-> (get-in req [:headers "authorization"])
              (->> (re-matches #"(?i)bearer\s+(.+)"))
              second
              str/trim)
      (not-empty (str (get-in req [:params :token])))))
