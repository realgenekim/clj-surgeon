(ns cfp-scheduler-killer.sessionize-import
  "Import a speaker's details from their public Sessionize profile.

   This is the defection feature: a speaker who already keeps a Sessionize
   profile pastes its URL and their name, tagline, bio, headshot and links land
   in our form. Switching costs are what keep incumbents alive — so we read
   theirs and ask for nothing twice.

   Strictly READ-ONLY: no login, no API key, only the same public HTML a browser
   would get, and it writes NOTHING to the database (no events_log row — an
   import is not a mutation, it just fills a form the speaker has not submitted
   yet).

   PARSING is separated from FETCHING on purpose: `parse-profile` takes an HTML
   string, so the tests run against a saved fixture and never touch the network."
  (:require [clojure.string :as str]
            [taoensso.timbre :as log])
  (:import (org.jsoup Jsoup)
           (org.jsoup.nodes Document Element)
           (java.time Duration)))

;; --- URL validation ---------------------------------------------------------

(def profile-url-pattern
  "https://sessionize.com/<handle>/ — handle is their public profile slug.
   We accept http/https, an optional www., and a missing trailing slash."
  #"(?i)^https?://(?:www\.)?sessionize\.com/([A-Za-z0-9][A-Za-z0-9._~-]*)/?$")

(def bare-handle-pattern
  "Just the profile slug on its own — what a speaker types when they know
   their handle but not the URL shape (Gene, 2026-08-09: accept either)."
  #"^[A-Za-z0-9][A-Za-z0-9._~-]*$")

(defn normalize-profile-url
  "Canonicalise a pasted Sessionize profile URL — or a bare handle like
   \"realgenekim\" — to https://sessionize.com/<handle>/ (trailing slash
   added). Returns nil when the input is neither."
  [url]
  (when-let [u (some-> url str/trim not-empty)]
    (if-let [[_ handle] (re-matches profile-url-pattern u)]
      (str "https://sessionize.com/" handle "/")
      (when (re-matches bare-handle-pattern u)
        (str "https://sessionize.com/" u "/")))))

;; --- Parsing ----------------------------------------------------------------

(defn- text-of
  "First match's normalised text, or nil when absent/blank."
  [^Document doc ^String selector]
  (when-let [^Element el (.selectFirst doc selector)]
    (not-empty (str/trim (.text el)))))

(defn- attr-of
  [^Document doc ^String selector ^String attr]
  (when-let [^Element el (.selectFirst doc selector)]
    (not-empty (str/trim (.attr el attr)))))

(defn- bio-text
  "The bio, with <br> and paragraph breaks preserved as newlines. Jsoup's
   .text() flattens everything to one line, which would destroy a multi-paragraph
   bio — so we turn the breaks into markers first."
  [^Document doc]
  (when-let [^Element el (.selectFirst doc "div.c-s-speaker-info__bio")]
    (let [clone (.clone el)]
      (doseq [^Element br (.select clone "br")]
        (.after br "\n"))
      (doseq [^Element p (.select clone "p")]
        (.after p "\n\n"))
      (-> (.wholeText clone)
          (str/replace #"[ \t]+" " ")
          (str/replace #"\n{3,}" "\n\n")
          (str/replace #"[ \t]*\n[ \t]*" "\n")
          str/trim
          not-empty))))

(defn- links
  "The profile's link list as {:label url}. Labels are Sessionize's own
   (Twitter handle, \"LinkedIn\", \"Blog\", \"Company\", …).

   Sessionize renders the same list in several responsive blocks, so the raw
   selection contains each link 2–3 times — we dedupe by URL, keeping order."
  [^Document doc]
  (->> (.select doc "ul.c-s-links a.c-s-links__link")
       (keep (fn [^Element a]
               (let [href (not-empty (str/trim (.attr a "href")))
                     label (or (some-> (.selectFirst a "span.o-label") .text str/trim not-empty)
                               (not-empty (str/trim (.text a))))]
                 (when href {:label label :url href}))))
       (reduce (fn [{:keys [seen out] :as acc} {:keys [url] :as l}]
                 (if (contains? seen url)
                   acc
                   {:seen (conj seen url) :out (conj out l)}))
               {:seen #{} :out []})
       :out))

(defn- find-link
  "The first link whose URL host contains `needle`."
  [links needle]
  (some (fn [{:keys [url]}]
          (when (str/includes? (str/lower-case url) needle) url))
        links))

(defn parse-profile
  "Parse Sessionize speaker-profile HTML into a speaker map.

   Returns {:name :tagline :bio :headshot-url :location :linkedin-url
            :twitter-url :website-url :links} — every value nilable. Returns nil
   when the document has no speaker name, which is how we detect 'that URL
   wasn't a speaker profile' without trusting the status code."
  [html]
  (when-let [html (not-empty (str html))]
    (let [doc (Jsoup/parse html)
          ls (links doc)
          name* (or (text-of doc "h1.c-s-speaker-info__name")
                    ;; og:title is "<Name>'s Speaker Profile"
                    (some-> (attr-of doc "meta[property=og:title]" "content")
                            (str/replace #"(?i)'s speaker profile.*$" "")
                            str/trim
                            not-empty))
          linkedin (find-link ls "linkedin.com")
          twitter (or (find-link ls "twitter.com") (find-link ls "x.com"))]
      (when name*
        {:name name*
         :tagline (text-of doc "p.c-s-speaker-info__tagline")
         :bio (or (bio-text doc)
                  (attr-of doc "meta[name=description]" "content"))
         :headshot-url (or (attr-of doc "figure.c-s-speaker-info__avatar img" "src")
                           (attr-of doc "meta[property=og:image]" "content"))
         :location (text-of doc "p.c-s-speaker-info__location")
         :linkedin-url linkedin
         :twitter-url twitter
         ;; "website" = the first link that is neither a social nor sessionize
         :website-url (some (fn [{:keys [url]}]
                              (let [u (str/lower-case url)]
                                (when-not (or (str/includes? u "linkedin.com")
                                              (str/includes? u "twitter.com")
                                              (str/includes? u "x.com")
                                              (str/includes? u "sessionize.com"))
                                  url)))
                            ls)
         :links ls}))))

;; --- Fetching ---------------------------------------------------------------

(def ^:private user-agent
  "Polite and honest: a real contact address beats a spoofed browser string."
  "cfp-scheduler-killer/0.1 (speaker-profile import; +https://github.com/realgenekim)")

(def ^:private fetch-timeout-ms
  "12s, not 5s, and the difference is the whole feature.

   Measured 2026-08-09 against sessionize.com: the FIRST fetch after boot took
   ~5.5s (JVM TLS/DNS cold start); every one after it took 0.7–1.5s. At the old
   5s timeout that means the import failed exactly once — on its first use, which
   is the only time a given speaker ever tries it. They get 'we couldn't reach
   Sessionize', shrug, and type their bio by hand, and the killer feature has
   killed itself in front of the one person it was for."
  12000)

(defn- attempt-fetch [url]
  (-> (Jsoup/connect url)
      (.userAgent user-agent)
      (.timeout (int fetch-timeout-ms))
      (.followRedirects true)
      (.ignoreHttpErrors false)
      (.get)
      (.html)))

(defn fetch-profile-html
  "GET the profile HTML with a polite UA, retrying ONCE.

   The retry is specifically for the cold-start case above: a warm connection
   answers in about a second, so a second attempt costs a real speaker almost
   nothing and converts the one failure that actually happens into a success.
   Returns the HTML string, or nil on any network/HTTP failure (logged)."
  [url]
  (letfn [(try-once [attempt]
            (try
              (attempt-fetch url)
              (catch Exception e
                (log/warn :sessionize-fetch-failed :url url :attempt attempt
                          :msg (.getMessage e))
                nil)))]
    (or (try-once 1)
        (try-once 2))))

(defn import-profile
  "The whole move: validate → fetch → parse.

   Returns {:ok true :url canonical :speaker {...}} or
           {:ok false :error <keyword> :message <human string>}.
   Never throws, and never writes anything."
  [url]
  (if-let [canonical (normalize-profile-url url)]
    (if-let [html (fetch-profile-html canonical)]
      (if-let [speaker (parse-profile html)]
        (do (log/info :sessionize-import-ok :url canonical :name (:name speaker))
            {:ok true :url canonical :speaker speaker})
        (do (log/warn :sessionize-import-unparseable :url canonical)
            {:ok false :error :unparseable
             :message (str "We couldn't read that profile — " canonical
                           " didn't look like a speaker page. You can fill the fields in by hand.")}))
      {:ok false :error :unreachable
       :message (str "We couldn't reach " canonical
                     " just now. You can fill the fields in by hand.")})
    {:ok false :error :bad-url
     :message "That doesn't look like a Sessionize profile — paste your profile URL (https://sessionize.com/yourname/) or just your username."}))

(comment
  (normalize-profile-url "https://sessionize.com/tessak22")
  ;=> "https://sessionize.com/tessak22/"
  (parse-profile (slurp "test/fixtures/sessionize-tessak22.html"))
  (import-profile "https://sessionize.com/tessak22/"))
