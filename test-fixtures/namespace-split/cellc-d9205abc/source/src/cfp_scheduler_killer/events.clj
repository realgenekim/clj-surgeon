(ns cfp-scheduler-killer.events
  "Event creation — the first vertical slice, on the append-only store.

   Creating an event is not one write: it is the whole 'zero-to-open-CFP in ten
   minutes' move. One append batch records
     event.created → committee.created ('Program Committee', scope {:all true})
                   → form.installed    (the EAIS Charlotte seed field-defs)
   so a brand-new event already has a committee to invite people to and a form
   that can accept submissions.

   There is no separate audit log any more: THE LOG IS THE STORE
   (cfp-scheduler-killer.store). What used to be an events_log row is now the
   authoritative fact, and the tables you read are folded from it.

   Validation is 100% server-side (malli + a uniqueness check against the folded
   state). No client-side validation, no client-side slug JS — see CLAUDE.md."
  (:require [clojure.string :as str]
            [malli.core :as m]
            [malli.error :as me]
            [taoensso.timbre :as log]
            [closed-record.core :as cr]
            [com.fulcrologic.guardrails.core :refer [>defn => ?]]
            [cfp-scheduler-killer.seed :as seed]
            [cfp-scheduler-killer.store :as store])
  (:import (java.text Normalizer Normalizer$Form)
           (java.time Instant LocalDate LocalDateTime ZoneId)
           (java.time.format DateTimeFormatter)))

;; --- Time zones -------------------------------------------------------------

(def common-timezones
  "The short list an organizer actually picks from. Sessionize warns that the
   zone is painful to change later, so we show the common ones and default to
   America/New_York (ITRev's home zone)."
  ["America/New_York"
   "America/Chicago"
   "America/Denver"
   "America/Los_Angeles"
   "America/Phoenix"
   "America/Anchorage"
   "Pacific/Honolulu"
   "America/Toronto"
   "America/Sao_Paulo"
   "Europe/London"
   "Europe/Dublin"
   "Europe/Paris"
   "Europe/Berlin"
   "Europe/Amsterdam"
   "Europe/Stockholm"
   "Europe/Madrid"
   "Europe/Zurich"
   "Europe/Warsaw"
   "Europe/Athens"
   "UTC"])

(def default-timezone "America/New_York")

(defn valid-timezone? [tz]
  (boolean (and (string? tz) (contains? (ZoneId/getAvailableZoneIds) tz))))

;; --- Slugs ------------------------------------------------------------------

(def slug-pattern #"^[a-z0-9-]+$")
(def ^:private max-slug-length 60)

(>defn slugify
       "Derive a URL slug from free text: strip accents, lower-case, collapse
   everything that is not [a-z0-9] into single hyphens, trim hyphens, cap
   length. Returns \"\" when nothing usable survives.

   Server-side only — the create form never runs slug JS in the browser."
       [s]
       [(? string?) => string?]
       (-> (or s "")
           (Normalizer/normalize Normalizer$Form/NFD)
           (str/replace #"\p{InCombiningDiacriticalMarks}+" "")
           (str/lower-case)
           (str/replace #"[^a-z0-9]+" "-")
           (str/replace #"^-+" "")
           (str/replace #"-+$" "")
           (as-> s' (if (> (count s') max-slug-length)
                      (-> (subs s' 0 max-slug-length)
                          (str/replace #"-+$" ""))
                      s'))))

(defn random-suffix
  "A short lowercase base-36 suffix, for demo/test slugs."
  ([] (random-suffix 6))
  ([n] (subs (str (Long/toString (Math/abs (.nextLong (java.security.SecureRandom.))) 36)
                  "000000")
             0 n)))

;; --- Parsing (form strings → typed values) ----------------------------------

(defn blank->nil [s]
  (when-not (str/blank? s) (str/trim s)))

(defn parse-date
  "Parse an <input type=date> value (yyyy-MM-dd). nil on blank; ::invalid on junk."
  [s]
  (when-let [s (blank->nil s)]
    (try (LocalDate/parse s) (catch Exception _ ::invalid))))

(defn parse-local-datetime
  "Parse an <input type=datetime-local> value (yyyy-MM-ddTHH:mm[:ss]).
   nil on blank; ::invalid on junk."
  [s]
  (when-let [s (blank->nil s)]
    (try (LocalDateTime/parse s)
         (catch Exception _
           (try (LocalDateTime/parse s (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm"))
                (catch Exception _ ::invalid))))))

(defn local-datetime->instant
  "Interpret a wall-clock LocalDateTime in the event's zone and return the
   absolute Instant. The zone is part of the event; the log stores instants."
  [^LocalDateTime ldt tz]
  (when (instance? LocalDateTime ldt)
    (.toInstant (.atZone ldt (ZoneId/of (if (valid-timezone? tz) tz "UTC"))))))

(defn ->display-date
  "Coerce whatever a date arrived as — LocalDate, java.sql.Date, or the
   yyyy-MM-dd string a browser input posts — into a LocalDate. nil for anything
   unusable, so a half-typed date reads as 'no dates yet' instead of throwing."
  [d]
  (cond
    (nil? d) nil
    (instance? LocalDate d) d
    (instance? java.sql.Date d) (.toLocalDate ^java.sql.Date d)
    (string? d) (let [p (parse-date d)] (when (instance? LocalDate p) p))
    :else nil))

(defn parse-close-date
  "When the call for speakers closes, from whichever spelling arrived.

   The form now asks for a DATE, not a timestamp: a call closes at the END OF A
   DAY in the event's own zone, which is what an organizer means when they say
   \"closes November 20\". `cfp-closes-on` is that date. `cfp-closes-at` is the
   old datetime-local spelling — still accepted (the e2e drive and any existing
   integration send it) and reduced to its date, so there is exactly one meaning
   however it arrives.

   nil = no close date = the call stays open. ::invalid = junk, so validation
   can report it instead of it silently becoming 'open forever'."
  [params]
  (let [on (blank->nil (:cfp-closes-on params))
        at (blank->nil (:cfp-closes-at params))]
    (cond
      on (let [d (parse-date on)]
           (if (instance? LocalDate d) (.atTime ^LocalDate d 23 59 59) ::invalid))
      at (let [dt (parse-local-datetime at)]
           (if (instance? LocalDateTime dt)
             (.atTime (.toLocalDate ^LocalDateTime dt) 23 59 59)
             ;; a bare yyyy-MM-dd in the old field is a close date too
             (let [d (parse-date at)]
               (if (instance? LocalDate d) (.atTime ^LocalDate d 23 59 59) ::invalid))))
      :else nil)))

(defn derive-slug
  "The public address we derive when nobody types one:
   `<name>-<city>-<year>` — \"Enterprise AI Summit\" + \"Charlotte, NC\" +
   Oct 2026 becomes `enterprise-ai-summit-charlotte-2026`.

   City is the first comma-segment of the location, because \"Charlotte, NC\"
   and \"Charlotte, North Carolina\" are the same place and must not produce two
   addresses. Either part is simply left out when it isn't known yet, so a name
   alone still yields a usable address while the organizer is mid-type.

   Derivation is a CONVENIENCE, never a correction: it is used only when the
   slug field is blank, and a collision is refused rather than suffixed. Slugs
   are forever — they are permalinks and they seed the ICS UIDs."
  [draft]
  ;; The NAME is load-bearing: with no name there is no slug at all. Deriving
  ;; from the city and the year alone produced /cfp/2026 on an empty form — an
  ;; address that names nothing, offered to an organizer who has typed nothing.
  (when-let [nm (blank->nil (slugify (blank->nil (:name draft))))]
    (let [city (some-> (blank->nil (:location draft))
                       (str/split #",")
                       first
                       slugify
                       blank->nil)
          year (some-> (->display-date (:starts-on draft)) .getYear str)]
      (blank->nil (str/join "-" (remove nil? [nm city year]))))))

(defn parse-form
  "Turn the raw string params of the create form into typed values.
   Unparseable values become ::invalid so validation can report them."
  [params]
  (let [name*    (blank->nil (:name params))
        slug-in  (blank->nil (:slug params))
        tz       (or (blank->nil (:tz params)) default-timezone)]
    {:name          name*
     ;; Slug is derived SERVER-SIDE when the organizer leaves it blank.
     :slug          (or slug-in (derive-slug params))
     :slug-derived? (nil? slug-in)
     :tz            tz
     :starts-on     (parse-date (:starts-on params))
     :ends-on       (parse-date (:ends-on params))
     ;; The call opens WHEN THE EVENT IS CREATED — or not at all until the
     ;; organizer says so. There is no "opens at" field any more, and a posted
     ;; cfp-opens-at is deliberately ignored rather than honoured: "your CFP is
     ;; live the moment you press the button" is only true if nothing can
     ;; quietly schedule it for later. `apply-create-defaults` stamps it.
     :cfp-opens-at  nil
     :cfp-closes-at (parse-close-date params)
     ;; One sentence-shaped radio on the form: opens right away (the default) or
     ;; stays closed until the organizer opens it by hand.
     :cfp-open?     (not= "closed" (blank->nil (:cfp-state params)))
     :support-email (blank->nil (:support-email params))
     :location      (blank->nil (:location params))
     :website-url   (blank->nil (:website-url params))}))

(defn apply-create-defaults
  "Stamp the two things the slim create form stopped asking for.

   **When the call opens.** Always the moment of creation — a minute in the
   PAST, not `now`, because \"it opens the moment you create it\" has to still
   be true on the very next request, and an opens-at of exactly now is a coin
   flip against the clock.

   **Whether it is open at all.** \"Stays closed for now\" is recorded as a call
   that closes the same instant it opens: it was never open, the log says so
   without a new flag, and `submissions/cfp-state` reads that pair back as
   :not-open-yet. The optional close date is ignored in that case — an organizer
   who is not opening the call has not chosen when it shuts.

   The support email defaults to whoever pressed the button. Only genuinely
   blank values are defaulted, so the full-field create path (bin/e2e_drive.py
   §2, the route tests) behaves exactly as it did."
  ([draft creator-email] (apply-create-defaults draft creator-email nil))
  ([draft creator-email now]
   (let [tz      (if (valid-timezone? (:tz draft)) (:tz draft) default-timezone)
         now     (or now (.minusMinutes (LocalDateTime/now (ZoneId/of tz)) 1))
         creator (blank->nil creator-email)
         open?   (get draft :cfp-open? true)]
     (cond-> (assoc draft :cfp-opens-at now)
       (not open?) (assoc :cfp-closes-at now)

       (and creator (nil? (:support-email draft)))
       (assoc :support-email creator)))))

;; --- Display name — the ONE way an event is spelled out ----------------------
;;
;; "Name — Location · Dates" is how an event appears on the create-page marquee,
;; on the public CFP page and on the agenda. One function so those three can
;; never drift, and so the create form can stop asking anyone to type the dates
;; into the name (see `trim-suggestion`, which offers to take them back out).

(def ^:private md-fmt (DateTimeFormatter/ofPattern "MMM d"))
(def ^:private mdy-fmt (DateTimeFormatter/ofPattern "MMM d, yyyy"))

(defn display-dates
  "\"Oct 7–8, 2026\" · \"Oct 30 – Nov 1, 2026\" · \"Oct 7, 2026\".

   The tight en-dash on a same-month range is deliberate — it reads as one span
   of days. A range that crosses a month gets spaces so the two month names
   don't collide. The year is said once unless the range crosses one."
  [starts ends]
  (let [s (->display-date starts)
        e (->display-date ends)]
    (cond
      (and s e (.isEqual ^LocalDate s ^LocalDate e))
      (.format ^LocalDate s mdy-fmt)

      (and s e
           (= (.getYear ^LocalDate s) (.getYear ^LocalDate e))
           (= (.getMonthValue ^LocalDate s) (.getMonthValue ^LocalDate e)))
      (str (.format ^LocalDate s md-fmt) "–" (.getDayOfMonth ^LocalDate e)
           ", " (.getYear ^LocalDate s))

      (and s e (= (.getYear ^LocalDate s) (.getYear ^LocalDate e)))
      (str (.format ^LocalDate s md-fmt) " – " (.format ^LocalDate e md-fmt)
           ", " (.getYear ^LocalDate s))

      (and s e)
      (str (.format ^LocalDate s mdy-fmt) " – " (.format ^LocalDate e mdy-fmt))

      s (.format ^LocalDate s mdy-fmt)
      e (.format ^LocalDate e mdy-fmt)
      :else nil)))

(defn display-name
  "How this event is spelled out everywhere: \"Name — Location · Dates\".

   Takes anything carrying :name/:location/:starts-on/:ends-on — a folded event
   record, or the raw strings the create page is mid-typing. nil when there is
   no name yet, which is exactly the marquee's ghost state."
  [event]
  (let [nm    (blank->nil (:name event))
        loc   (blank->nil (:location event))
        dates (display-dates (:starts-on event) (:ends-on event))
        tail  (str/join " · " (remove nil? [loc dates]))]
    (when nm
      (if (str/blank? tail) nm (str nm " — " tail)))))

;; --- Trim assist ------------------------------------------------------------
;;
;; Fifteen years of CFP archaeology says organizers type "Enterprise AI Summit
;; (Oct 2026)" into the name field, because the incumbent tools never showed
;; them what the name would look like next to the dates. The marquee shows it,
;; so we can offer to take the duplication back out — ONCE, quietly, and only
;; when the detection is unambiguous. A guess that fires on a legitimate name
;; ("MayDay Conference") is worse than no assist at all, so these three patterns
;; are all there is: a parenthetical containing a year, a trailing month-name +
;; digits, or the typed location repeated inside the name.

(def ^:private parenthetical-date-pattern #"\s*\([^)]*\d{4}[^)]*\)\s*$")

(def ^:private month-suffix-pattern
  #"(?i)[\s,–—-]+(?:jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\.?\s*\d[\d\s,–—/-]*$")

(defn- tidy
  "Collapse runs of whitespace and shave dangling separators off both ends."
  [s]
  (-> (or s "")
      (str/replace #"\s+" " ")
      (str/replace #"^[\s,–—-]+" "")
      (str/replace #"[\s,–—-]+$" "")
      str/trim))

(defn trim-suggestion
  "Does this name repeat what the marquee already adds? Returns
   {:trimmed <name without it> :why \"dates\"|\"location\"}, or nil.

   nil is the common answer and the safe one: nothing ambiguous is ever offered."
  [nm location]
  (let [nm  (blank->nil nm)
        loc (blank->nil location)
        candidate
        (when nm
          (cond
            (re-find parenthetical-date-pattern nm)
            {:why "dates" :trimmed (tidy (str/replace nm parenthetical-date-pattern ""))}

            (re-find month-suffix-pattern nm)
            {:why "dates" :trimmed (tidy (str/replace nm month-suffix-pattern ""))}

            (and loc (str/includes? (str/lower-case nm) (str/lower-case loc)))
            {:why "location"
             :trimmed (tidy (str/replace nm
                                         (re-pattern (str "(?i)" (java.util.regex.Pattern/quote loc)))
                                         ""))}

            :else nil))]
    (when (and candidate
               (>= (count (:trimmed candidate)) 3)
               (not= (:trimmed candidate) nm))
      candidate)))

;; --- Validation -------------------------------------------------------------

(def url-pattern
  "http(s) only, host must have a dot, no whitespace. Deliberately loose — this
   is a link an organizer pastes, not something we parse."
  #"^https?://[^\s/$.?#][^\s]*\.[^\s]*$")

(def EventDraft
  "Shape checks. Cross-field rules (ordering, uniqueness) live in
   `validation-errors` because malli humanize can't key them to one field."
  [:map {:closed false}
   [:name [:string {:min 1 :max 200
                    :error/message "Event name is required."}]]
   [:slug [:and
           [:string {:min 1 :error/message "A CFP address is required (we derive one from the name if you leave it blank)."}]
           [:re {:error/message "Use lowercase letters, numbers and hyphens only."} slug-pattern]]]
   [:tz [:fn {:error/message "Pick a valid time zone."} valid-timezone?]]
   ;; The optional three. `blank->nil` already turned "" into nil upstream, so
   ;; {:optional true} + :maybe means "leave it out" — never "any junk allowed".
   [:support-email {:optional true}
    [:maybe [:re {:error/message "That doesn't look like an email address."}
             #"^[^@\s]+@[^@\s]+\.[^@\s]+$"]]]
   [:location {:optional true}
    [:maybe [:string {:max 200
                      :error/message "Keep the location under 200 characters."}]]]
   [:website-url {:optional true}
    [:maybe [:re {:error/message "Enter a full URL starting with http:// or https://"}
             url-pattern]]]])

(defn slug-taken?
  "True when an event already owns this slug (checked against folded state)."
  [slug]
  (boolean (and slug (store/get-event-by-slug slug))))

(defn slug-owner-display
  "The display name of the event that owns `slug`, nil when the address is
   free. The availability check and the refusal message both go through this,
   so the live green/red line and the 422 can never name different owners."
  [slug]
  (when-not (str/blank? slug)
    (some-> (store/get-event-by-slug slug) display-name)))

(defn validation-errors
  "Return {field [messages]} for a parsed draft, or nil when it is valid.
   Everything here runs on the server; the form is re-rendered with these."
  [draft]
  (let [;; ::invalid markers can't go through malli — report them first.
        junk (into {}
                   (keep (fn [[k label]]
                           (when (= ::invalid (get draft k))
                             [k [(str label " isn't a valid date.")]]))
                         {:starts-on     "Start date"
                          :ends-on       "End date"
                          :cfp-opens-at  "CFP open"
                          :cfp-closes-at "CFP close"}))
        clean (reduce (fn [m k] (cond-> m (= ::invalid (get m k)) (assoc k nil)))
                      draft
                      [:starts-on :ends-on :cfp-opens-at :cfp-closes-at])
        shape (when-not (m/validate EventDraft clean)
                (me/humanize (m/explain EventDraft clean)))
        {:keys [slug starts-on ends-on]} clean
        cross (cond-> {}
                (and starts-on ends-on (.isAfter ^LocalDate starts-on ^LocalDate ends-on))
                (assoc :ends-on ["The event can't end before it starts."])

                ;; There is deliberately NO "must close after it opens" rule any
                ;; more. The call now opens at creation, so the only thing that
                ;; rule could reject is a close date in the past — which is a
                ;; legitimate thing to say (it means "closed", and is exactly
                ;; how 'stays closed for now' and the Close-the-call action are
                ;; recorded). See apply-create-defaults.

                ;; A taken address is REFUSED, never quietly suffixed with -2.
                ;; The slug is a permalink and the seed of every ICS UID; an app
                ;; that silently renames what you asked for has already lied to
                ;; the calendar invites. Naming the event that holds it is the
                ;; difference between a refusal and a riddle.
                (and slug (re-matches slug-pattern slug) (slug-taken? slug))
                (assoc :slug [(str "That URL is taken by "
                                   (or (display-name (store/get-event-by-slug slug)) slug)
                                   ". Pick a different URL in More options.")]))
        all (merge-with into (or shape {}) junk cross)]
    (when (seq all) all)))

;; --- Row projections --------------------------------------------------------

(def ^:private event-keys
  "Canonical keys every event closed-record carries. Views depend on these —
   a missing key is a thrown exception, not a silent nil."
  [:id :slug :name :starts-on :ends-on :tz :cfp-opens-at :cfp-closes-at
   :support-email :location :website-url :cfp-intro :archived-at :settings
   :created-at])

(defn row->event
  "Project a folded event map into a closed-record with canonical keys."
  [row]
  (when row
    (cr/closed-record
     (reduce (fn [m k] (if (contains? m k) m (assoc m k nil)))
             (select-keys row event-keys)
             event-keys))))

;; --- Mutations --------------------------------------------------------------

(>defn create-event!
       "Create an event, its Program Committee and its seed form as ONE append batch
   of three events. Returns the event closed-record.

   `draft` is the output of `parse-form` (already validated). Throws ex-info
   {:type :duplicate-slug} if the slug was claimed between validation and here."
       ([draft] [map? => map?] (create-event! draft "organizer"))
       ([draft actor]
        [map? string? => map?]
        (let [{:keys [name slug tz starts-on ends-on cfp-opens-at cfp-closes-at
                      support-email location website-url]} draft]
          (when (slug-taken? slug)
            (log/warn :event-create-duplicate-slug :slug slug)
            (throw (ex-info (str "Slug already taken: " slug)
                            {:type :duplicate-slug :slug slug})))
          (let [event-id (store/new-id)
                created-at (store/now-iso)
           ;; The payload is the ROW: complete, self-describing, replayable.
                event-payload {:id event-id
                               :slug slug
                               :name name
                               :starts-on (store/->iso-date (when (instance? LocalDate starts-on) starts-on))
                               :ends-on (store/->iso-date (when (instance? LocalDate ends-on) ends-on))
                               :tz tz
                               :cfp-opens-at (store/->iso-instant (local-datetime->instant cfp-opens-at tz))
                               :cfp-closes-at (store/->iso-instant (local-datetime->instant cfp-closes-at tz))
                               :support-email support-email
                               :location location
                               :website-url website-url
                               ;; The API token is generated PER EVENT at creation —
                               ;; never a shared constant, and never in the seed
                               ;; defaults, where every event would inherit one token.
                               :settings (assoc seed/default-event-settings
                                                :api-token (store/new-id))
                               :created-at created-at}
                committee-payload {:id (store/new-id)
                                   :event-id event-id
                                   :name (:name seed/default-committee)
                                   :scope (:scope seed/default-committee)
                                   :coverage-target (:coverage-target seed/default-committee)
                                   :created-at created-at}
                form-payload {:id (store/new-id)
                              :event-id event-id
                              :template "eais-charlotte"
                              :fields seed/eais-charlotte-form
                              :created-at created-at}]
            (store/append-all!
             [{:type "event.created" :actor actor :payload event-payload}
              {:type "committee.created" :actor actor :payload committee-payload}
              {:type "form.installed" :actor actor :payload form-payload}])
            (log/info :event-created :slug slug :event-id event-id)
            (row->event (store/get-event-by-slug slug))))))

(def editable-detail-columns
  "Whitelist for `update-event-details!`. A whitelist, not (dissoc draft :id),
   so a stray key can never reach the update."
  #{:name :starts-on :ends-on :tz :location :website-url :support-email
    :cfp-opens-at :cfp-closes-at :cfp-intro})

(>defn update-event-details!
       "Patch a few optional event attributes by appending an 'event.updated' event.

   Only keys PRESENT in `attrs` are written, so this never blanks a field the
   caller didn't mention. The event records the before AND after values — the
   projection is derived, the log is the story of how it got there."
       ([event-id attrs] [string? map? => (? map?)] (update-event-details! event-id attrs "organizer"))
       ([event-id attrs actor]
        [string? map? string? => (? map?)]
        (let [iso-safe (fn [m]
                    ;; The folded row carries LocalDate/Instant values; the log
                    ;; is JSON. Temporals ride as ISO strings and the fold's
                    ;; parse-event-row turns them back (this crashed on the
                    ;; first date edit, 2026-08-09).
                         (into {}
                               (map (fn [[k v]]
                                      [k (cond
                                           (instance? LocalDate v) (str v)
                                           (instance? LocalDateTime v) (str v)
                                           (instance? Instant v) (store/->iso-instant v)
                                           :else v)]))
                               m))
              changes (select-keys attrs editable-detail-columns)]
          (when (seq changes)
            (let [before (store/get-event-by-id event-id)]
              (when-not before
                (throw (ex-info (str "No such event: " event-id)
                                {:type :no-such-event :event-id event-id})))
              (store/append!
               {:type "event.updated"
                :actor actor
                :payload {:id event-id
                          :slug (:slug before)
                          :changed (mapv name (keys changes))
                          :before (iso-safe (select-keys before (keys changes)))
                          :changes (iso-safe changes)}})
              (log/info :event-updated :event-id event-id :fields (mapv name (keys changes)))
              (row->event (store/get-event-by-slug (:slug before))))))))

;; --- The call for speakers is opened and closed by HAND ---------------------
;;
;; Same doctrine as the Notified flag: a state change a speaker can feel is a
;; deliberate act by a named person, recorded once, not a clock quietly doing it
;; in the background. Both of these are ordinary `event.updated` appends, so the
;; event log tells the story of when the call was open and who said so.

(defn open-cfp!
  "Open the call, now. If a close date has already passed — including the
   same-instant stamp that records \"stays closed for now\" — it is cleared,
   because otherwise the door would slam again on the very next request."
  [event actor]
  (let [now (Instant/now)
        closes (:cfp-closes-at event)]
    (update-event-details!
     (:id event)
     (cond-> {:cfp-opens-at (store/->iso-instant now)}
       (and closes (not (.isAfter ^Instant closes now)))
       (assoc :cfp-closes-at nil))
     actor)))

(defn close-cfp!
  "Close the call, now. Reopening is `open-cfp!` — there is no undo button that
   pretends the closed window never happened."
  [event actor]
  (update-event-details! (:id event)
                         {:cfp-closes-at (store/->iso-instant (Instant/now))}
                         actor))

(defn set-cfp-close-date!
  "Settings: change or clear the DATE the call shuts (end of that day, in the
   event's own zone). A blank date clears it — the call stays open."
  [event date-str actor]
  (let [parsed (parse-close-date {:cfp-closes-on date-str})]
    (when (= ::invalid parsed)
      (throw (ex-info (str "Not a date: " date-str)
                      {:type :invalid-close-date :value date-str})))
    (update-event-details!
     (:id event)
     {:cfp-closes-at (store/->iso-instant (local-datetime->instant parsed (:tz event)))}
     actor)))

(>defn archive-event!
       "Archive is a FACT, never a deletion (Gene, 2026-08-10): the event and
   its whole history stay in the log; listings stop leading with it and the
   working-event derivation skips it. Reversible with unarchive-event!."
       [event actor]
       [map? string? => any?]
       (store/append!
        {:type "event.archived"
         :actor actor
         :payload {:id (:id event) :slug (:slug event)
                   :archived-at (store/->iso-instant (Instant/now))}})
       (log/info :event-archived :slug (:slug event) :actor actor))

(>defn unarchive-event!
       "The undo half of archive-event! — also just an appended fact."
       [event actor]
       [map? string? => any?]
       (store/append!
        {:type "event.unarchived"
         :actor actor
         :payload {:id (:id event) :slug (:slug event)}})
       (log/info :event-unarchived :slug (:slug event) :actor actor))

(defn create-demo-event!
  "Create the judge-facing demo event. Same code path as a real create — the
   demo must never be a special case that can rot.

   NOTE: fake submissions land with the submissions slice; this creates the
   event, the committee and the seed form only."
  []
  (let [today (LocalDate/now)]
    (create-event!
     {:name "Demo Conference"
      :slug (str "demo-" (random-suffix 6))
      :tz default-timezone
      :starts-on (.plusMonths today 3)
      :ends-on (.plusDays (.plusMonths today 3) 1)
      ;; Open NOW, not at 9am today: a judge who presses this at 8:30 must not
      ;; land on a CFP that says "not open yet". Same doctrine as a real create.
      :cfp-opens-at (.minusMinutes (LocalDateTime/now (ZoneId/of default-timezone)) 1)
      :cfp-closes-at (.atTime (.plusMonths today 2) 23 59)
      :support-email "demo@example.com"
      :location "Charlotte, NC"}
     "demo-button")))

;; --- Queries (folded state, no I/O) -----------------------------------------

(defn list-events
  "All events, newest first."
  []
  (->> (store/all-events)
       (sort-by :created-at)
       reverse
       (mapv row->event)))

(defn events-for-person
  "The events THIS person reviews for, newest first — the tenancy boundary
   made visible (Gene, 2026-08-09: Gene's world and swyx's world must not see
   each other). Same membership fact the auth gate enforces; this is the
   listing side of it."
  [person-id]
  (let [snap (store/snapshot)
        committee-ids (into #{}
                            (comp (filter #(= person-id (:person-id %)))
                                  (map :committee-id))
                            (vals (:memberships snap)))
        event-ids (into #{}
                        (comp (filter #(contains? committee-ids (:id %)))
                              (map :event-id))
                        (vals (:committees snap)))]
    (filterv #(contains? event-ids (:id %)) (list-events))))

;; --- The working event -------------------------------------------------------
;; The sidebar is always exactly ONE event's spine — never a list — so pages
;; with no event in the URL need to know WHOSE spine to show. Rule (ratified
;; 2026-08-09, docs/design/nav-elements.md): the last event this person
;; visited; cold start falls back to the next upcoming event, else the most
;; recent one. Process-lifetime memory is fine — the fallback covers restarts.

(defonce working-events
  ;; person-id -> event-id, written on every signed-in GET of an event page.
  (atom {}))

(>defn remember-working-event!
       "Mark `event-id` as the event `person-id` is working in."
       [person-id event-id]
       [some? some? => map?]
       (swap! working-events assoc person-id event-id))

(defn working-event
  "The one event whose spine renders when the URL names none. `all-evts` is
   the person's visible events (tenancy already applied); ARCHIVED events
   never win — an archived room is not where anyone is working. Returns one
   event or nil — by design never a list (the rail is O(1) in event count)."
  [person-id all-evts]
  (let [evts (vec (remove :archived-at all-evts))]
    (when (seq evts)
      (or (when-let [id (when person-id (get @working-events person-id))]
            (first (filter #(= id (:id %)) evts)))
          ;; ISO dates compare correctly as strings, and :starts-on arrives as
          ;; either a LocalDate or its ISO string depending on the store.
          (let [today (str (java.time.LocalDate/now))
                dated (filter :starts-on evts)
                upcoming (->> dated
                              (filter #(<= 0 (compare (str (:starts-on %)) today)))
                              (sort-by (comp str :starts-on)))]
            (or (first upcoming)
                (last (sort-by (comp str :starts-on) dated))
                (first evts)))))))

(defn event-by-slug [slug]
  (row->event (store/get-event-by-slug slug)))

(defn event-by-id [event-id]
  (row->event (store/get-event-by-id event-id)))

(defn committees-for-event [event-id]
  (vec (store/committees-for-event event-id)))

(defn form-for-event [event-id]
  (last (store/forms-for-event event-id)))

(defn log-for-event [event-id]
  (store/log-for-event event-id))

(comment
  (store/load!)
  (slugify "Enterprise AI Summit — Charlotte 2026!")
  ;=> "enterprise-ai-summit-charlotte-2026"
  (validation-errors (parse-form {:name "" :tz "America/New_York"}))
  (create-demo-event!)
  (list-events))
