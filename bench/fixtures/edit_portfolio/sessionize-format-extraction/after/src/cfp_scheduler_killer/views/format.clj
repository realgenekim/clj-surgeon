(ns cfp-scheduler-killer.views.format
  "Hiccup views — everything is server-rendered.

   House rules that shape this file (global CLAUDE.md, the Datastar NEVERs):
     - No client-side DOM mutation, no setTimeout UI, no client validation.
     - The server decides what to show; the browser just displays it.
   This slice uses plain form POST + 303 redirect. SSE arrives with the board."
  (:require
   [cfp-scheduler-killer.events :as events]
   [clojure.string :as str])
  (:import
   (java.time LocalDate ZoneId)
   (java.time.format DateTimeFormatter)))

(defn ->instant
  "Coerce whatever a timestamp arrived as — ISO string, Instant, sql Timestamp —
   into an Instant. nil for anything unparseable, so a bad value renders as a
   dash rather than throwing on a page."
  [x]
  (cond
    (nil? x) nil
    (instance? java.time.Instant x) x
    (instance? java.sql.Timestamp x) (.toInstant ^java.sql.Timestamp x)
    (string? x) (try (java.time.Instant/parse x) (catch Exception _ nil))
    :else nil))

(defn ->local-date
  "Coerce a java.sql.Date / LocalDate / nil into a LocalDate."
  [d]
  (cond
    (nil? d) nil
    (instance? LocalDate d) d
    (instance? java.sql.Date d) (.toLocalDate ^java.sql.Date d)
    :else nil))

(defn cfp-public-url
  "The public CFP address we show organizers. `host` comes from the request."
  [host slug]
  (str host "/cfp/" slug))

(def ^:private date-fmt (DateTimeFormatter/ofPattern "MMM d, yyyy"))

(def ^:private datetime-fmt (DateTimeFormatter/ofPattern "MMM d, yyyy h:mm a"))

(def ^:private iso-date-fmt (DateTimeFormatter/ofPattern "yyyy-MM-dd"))

(defn not-blank [s] (when-not (str/blank? s) s))

(defn relative-when
  "\"just now\" / \"5m ago\" / \"2h ago\" / \"3d ago\", or nil once it stops being
   useful. Rendered on the SERVER at request time — a JS clock would be a second
   source of truth ticking against the page."
  [inst now]
  (when (and inst now)
    (let [secs (max 0 (.between java.time.temporal.ChronoUnit/SECONDS inst now))]
      (cond
        (< secs 60) "just now"
        (< secs 3600) (str (quot secs 60) "m ago")
        (< secs 86400) (str (quot secs 3600) "h ago")
        (< secs (* 7 86400)) (str (quot secs 86400) "d ago")
        :else nil))))

(def ^:private when-fmt (DateTimeFormatter/ofPattern "MMM d, h:mm a"))

(defn fmt-date [d]
  (some-> (->local-date d) (.format date-fmt)))

(defn fmt-instant
  "Render a timestamptz in the event's own time zone."
  [ts tz]
  (when ts
    (let [inst (cond
                 (instance? java.sql.Timestamp ts) (.toInstant ^java.sql.Timestamp ts)
                 (instance? java.time.Instant ts) ts
                 :else nil)]
      (when inst
        (str (.format (.atZone ^java.time.Instant inst
                               (ZoneId/of (if (events/valid-timezone? tz) tz "UTC")))
                      datetime-fmt))))))

(defn fmt-close-date
  "The stored close INSTANT as the yyyy-MM-dd an <input type=date> wants, read
   back in the event's own zone. The instant is 23:59:59 local, so the date it
   round-trips to is the date the organizer originally picked."
  [ts tz]
  (when-let [inst (->instant ts)]
    (.format (.toLocalDate (.atZone ^java.time.Instant inst
                                    (ZoneId/of (if (events/valid-timezone? tz) tz "UTC"))))
             iso-date-fmt)))

(defn fmt-when
  "The one timestamp format for the whole app: \"Aug 9, 7:15 AM\" in the event's
   own zone, with a relative hint while it is still recent."
  ([x tz] (fmt-when x tz (java.time.Instant/now)))
  ([x tz now]
   (when-let [inst (->instant x)]
     (let [zone (ZoneId/of (if (events/valid-timezone? tz) tz "UTC"))
           absolute (.format (.atZone inst zone) when-fmt)]
       (if-let [rel (relative-when inst now)]
         (str absolute " · " rel)
         absolute)))))

(defn fmt-date-range
  "\"Oct 14–15, 2026\"-ish. Honest about missing halves."
  [starts ends]
  (let [s (fmt-date starts) e (fmt-date ends)]
    (cond
      (and s e (= s e)) s
      (and s e) (str s " – " e)
      s (str s " – ?")
      e (str "? – " e)
      :else nil)))

(defn fmt-cfp-window [event]
  (let [{:keys [cfp-opens-at cfp-closes-at tz]} event
        o (fmt-instant cfp-opens-at tz)
        c (fmt-instant cfp-closes-at tz)]
    (cond
      (and o c) (str o " → " c)
      o (str "opens " o)
      c (str "closes " c)
      :else nil)))
