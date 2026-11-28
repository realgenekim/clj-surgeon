(ns cfp-scheduler-killer.people
  "Person identity — email-keyed, event-independent, shared by every slice.

   The same human is a committee member on one event and a speaker on another;
   they get ONE record, found by lowercased email. Extracted here so the
   committee slice and the submission slice cannot drift into two different
   notions of who someone is.

   People are created by appending a `person.created` event; the callers own
   that append because a person is almost always created as part of a larger
   batch (a membership, a submission)."
  (:require [clojure.string :as str]
            [cfp-scheduler-killer.store :as store]))

(def email-pattern #"^[^@\s]+@[^@\s]+\.[^@\s]+$")

(defn normalize-email
  "Lowercase + trim. Email is an IDENTITY key; it must not be defeated by
   capitalisation."
  [email]
  (some-> email str/trim str/lower-case not-empty))

(defn by-email
  "The person with this email, or nil."
  [email]
  (store/person-by-email (normalize-email email)))

(defn by-id [person-id] (store/person-by-id person-id))

(defn new-person
  "A person RECORD ready to be appended as a `person.created` payload.
   Pure — it does not write."
  [email name*]
  {:id (store/new-id)
   :email (normalize-email email)
   :name name*
   :profile {}
   :created-at (store/now-iso)})

(defn find-or-new
  "Returns [person created?]. An EXISTING person is returned untouched — we
   never rename someone because a third party typed their name differently."
  [email name*]
  (if-let [found (by-email email)]
    [found false]
    [(new-person email name*) true]))
