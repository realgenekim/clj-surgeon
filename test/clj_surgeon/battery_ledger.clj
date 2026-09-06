(ns clj-surgeon.battery-ledger
  "TEST-ISO-009a/b -- the battery RECEIPT LEDGER and its FRESHNESS TRIPWIRE.

   THE PROBLEM THIS EXISTS FOR. The battery lane is the one gate nothing else
   can stand in for: eleven namespaces of cold launcher drives, minutes-scale,
   deliberately OUT of the merge gate so the fast lanes stop queueing behind
   674 s of JVM starts. Taking it out of the merge gate is the whole point of
   the partition -- and it is also the risk, because a gate that does not run
   on every merge is a gate whose ABSENCE is silent. `make mcp-test` goes
   green either way. Nothing on the screen distinguishes `the battery passed
   last night` from `the battery has not run since Tuesday`.

   Gene's rule for this class (house rules, delivery invariant 17): a refusal
   nobody hears is indistinguishable from silent data loss. A missing nightly
   is exactly that shape -- so a stale battery must be a REFUSAL, not a
   silence.

   THE TWO HALVES:

     the LEDGER   `make test-battery` appends one line to
                  docs/observations/battery-ledger.edn -- {:sha :started
                  :wall_s :verdict :host} -- whether it passed or failed. One
                  entry per line, append-only, never rewritten: it is an event
                  log, and the only thing we can do to it is ruin it. The
                  RUNNER writes the file; the SEAT commits it, so a receipt
                  arrives in the history through a human act that can be
                  reviewed.

     the TRIPWIRE `make battery-fresh` refuses when the newest entry is older
                  than 26 h, when it failed, or when the commit it names is
                  not an ancestor of HEAD or is more than 30 counted commits behind
                  it. 26 h rather than 24 gives a nightly a two-hour margin
                  before it starts crying wolf; 30 commits bounds `the battery
                  passed, on a tree nobody would recognise`.

   WHY THE VERDICT IS PURE AND THE GIT LOOKUP IS INJECTED. Deciding freshness
   is arithmetic over an event log; finding out whether one sha leads to
   another is a `git` call, which is a CHILD PROCESS. The fast lane forbids
   those, so the decision function takes the distance as data and the CLI is
   the only place that shells out. That is also what lets the witness drive
   every refusal state without a repository shaped to produce it."
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(def ledger-path "docs/observations/battery-ledger.edn")

(def max-age-ms
  "26 h. A nightly that starts at 03:00 and takes 12 minutes must not be
   called stale at 03:00 the next day because it ran four minutes later."
  (* 26 60 60 1000))

(def max-commits-behind
  "N=30. Past this the battery's receipt describes a tree far enough from
   HEAD that `it passed` is a claim about different code."
  30)

;; --------------------------------------------------------------------------
;; the ledger itself
;; --------------------------------------------------------------------------

(defn entry-line
  "One ledger entry as the single line it is appended as."
  [{:keys [sha started wall_s verdict host]}]
  (pr-str {:sha sha :started started :wall_s wall_s :verdict verdict :host host}))

(defn parse-ledger
  "Every entry in ledger `text`, in file order. A line that does not read is
   NAMED rather than skipped: a corrupted receipt must not be able to make the
   ledger look merely shorter."
  [text]
  (->> (str/split-lines (or text ""))
       (map str/trim)
       (remove str/blank?)
       (map-indexed
         (fn [i line]
           (try
             (let [m (edn/read-string line)]
               (if (map? m) m {::unreadable line ::line (inc i)}))
             (catch Exception _ {::unreadable line ::line (inc i)}))))
       vec))

(defn append-entry!
  "Appends `entry` to the ledger at `path`, creating it if absent. Append-only
   by construction: it never reads what is already there."
  [path entry]
  (io/make-parents (io/file path))
  (spit path (str (entry-line entry) "\n") :append true)
  entry)

;; --------------------------------------------------------------------------
;; the tripwire
;; --------------------------------------------------------------------------

(defn- ms->hours [ms] (/ (double ms) 3600000.0))

(defn- verdict-kw
  "The verdict as a keyword, whether the ledger line carried `:pass` (edn read
   it as a keyword) or the CLI handed us the string \"pass\". Written out
   because `(keyword (str :pass))` is `:\":pass\"` -- which compares unequal to
   `:pass` and made the tripwire refuse EVERY receipt as failed. The witness
   `a-fresh-receipt-on-this-tree-passes` is the pin on that."
  [v]
  (if (keyword? v) v (keyword (str v))))

(def remedy
  (str "REMEDY: run the battery and commit its receipt --\n"
       "  flock /home/forge/tmp/suite.lock make test-battery\n"
       "  git add " ledger-path " && git commit\n"
       "The battery is the only gate that drives the eleven cold-launcher "
       "namespaces; `make mcp-test` cannot stand in for it."))

(defn freshness
  "The tripwire's verdict over `entries`, as data.

   `now-ms`             the instant to measure age from
   `commits-behind-fn`  sha -> a distance audit map (or legacy integer), or nil
                        when the sha is not an ancestor of HEAD at all

   Returns {:ok true :entry e :age-hours h} or {:ok false :reason kw
   :message s :remedy s}. Every refusal names its subject and its number: a
   tripwire that says only `stale` makes the reader go and find out how stale,
   and a reader who has to investigate is a reader who learns to ignore it."
  [entries now-ms commits-behind-fn]
  (let [bad (filter ::unreadable entries)
        newest (last (remove ::unreadable entries))]
    (cond
      (seq bad)
      {:ok false :reason :unreadable-entry
       :message (format "%d unreadable line(s) in %s (first at line %s): %s"
                        (count bad) ledger-path (::line (first bad))
                        (pr-str (::unreadable (first bad))))
       :remedy remedy}

      (nil? newest)
      {:ok false :reason :no-entries
       :message (str "no battery receipt in " ledger-path
                     " -- the battery lane has never recorded a run here, so "
                     "nothing on this tree distinguishes `it passed` from `it "
                     "was never run`")
       :remedy remedy}

      :else
      (let [{:keys [sha started verdict wall_s]} newest
            started-ms (try (.toEpochMilli (java.time.Instant/parse started))
                            (catch Exception _ nil))
            age (when started-ms (- now-ms started-ms))
            distance (commits-behind-fn sha)
            audit (if (map? distance) distance
                    {:commits-behind distance :raw-commits-behind distance
                     :ignored-archive-commits 0})
            behind (:commits-behind audit)]
        (merge audit
          (cond
            (nil? started-ms)
            {:ok false :reason :unreadable-timestamp
             :message (format "newest receipt names sha %s but its :started %s is not an instant"
                              sha (pr-str started))
             :remedy remedy}

            (not= :pass (verdict-kw verdict))
            {:ok false :reason :last-run-failed
             :message (format (str "the newest battery receipt FAILED: sha %s, started %s, "
                                   "wall %ss, verdict %s. A failing gate is not a fresh gate.")
                              sha started wall_s (pr-str verdict))
             :remedy remedy}

            (> age max-age-ms)
            {:ok false :reason :stale
             :message (format (str "the newest battery receipt is %.1f h old (sha %s, started %s); "
                                   "the tripwire refuses past %.0f h")
                              (ms->hours age) sha started (ms->hours max-age-ms))
             :remedy remedy}

            (nil? behind)
            {:ok false :reason :not-an-ancestor
             :message (format (str "the newest battery receipt names sha %s, which is NOT an "
                                   "ancestor of HEAD -- it passed on a tree this one does not "
                                   "descend from, so it says nothing about the code here")
                              sha)
             :remedy remedy}

            (> behind max-commits-behind)
            {:ok false :reason :too-far-behind
             :message (format (str "the newest battery receipt names sha %s, %d commits behind "
                                   "HEAD; the tripwire refuses past %d")
                              sha behind max-commits-behind)
             :remedy remedy}

            :else
            {:ok true :entry newest :age-hours (ms->hours age) :commits-behind behind}))))))

;; --------------------------------------------------------------------------
;; the CLI (bb) -- the ONLY place that shells out
;; --------------------------------------------------------------------------

(defn- sh
  [& args]
  (let [p (.. (ProcessBuilder. ^java.util.List (vec args))
              (redirectErrorStream true)
              start)
        out (slurp (.getInputStream p))]
    {:exit (.waitFor p) :out out}))

(def archival-paths
  "Closed output-journal set, audited in TEST-ISO-009b. Never a docs glob."
  #{"docs/observations/2026-09-03-captains-log-anvil-seat.md"
    "docs/observations/2026-09-05-captains-log-astra-four-hour-comparison.md"
    "docs/observations/2026-09-06-live-astra-typist-commentary.md"})

;; @spec TEST-ISO-009b -- malformed or non-content changes cannot exempt a commit.
(defn archive-only-diff?
  "Strict NUL-delimited --raw -z --no-renames diff; false on missing evidence."
  [raw]
  (let [parts (str/split (or raw "") #"\u0000" -1)
        records (butlast parts)]
    (boolean
      (and (= "" (last parts)) (seq records) (even? (count records))
           (every? (fn [[header path]]
                     (and (re-matches #":100644 100644 [0-9a-f]{40} [0-9a-f]{40} M" header)
                          (contains? archival-paths path)))
                   (partition 2 records))))))

(defn- archive-only-commit? [line]
  (let [[commit & parents] (str/split line #" ")]
    (and (seq parents)
         (every? (fn [parent]
                   (let [{:keys [exit out]}
                         (sh "git" "diff" "--raw" "-z" "--no-abbrev" "--no-renames"
                             "--no-ext-diff" "--no-textconv" "--ignore-submodules=none"
                             parent commit "--")]
                     (and (zero? exit) (archive-only-diff? out))))
                 parents))))

(defn- commits-behind-head
  "All-DAG distance, excluding only proven archival content commits.
   Histories over 1000 commits conservatively use raw distance."
  [sha]
  (when (and sha (re-matches #"[0-9a-f]{7,40}" (str sha)))
    (when (zero? (:exit (sh "git" "merge-base" "--is-ancestor" (str sha) "HEAD")))
      (let [range (str sha "..HEAD")
            {count-exit :exit count-out :out} (sh "git" "rev-list" "--count" range)
            raw (when (zero? count-exit) (parse-long (str/trim count-out)))]
        (when raw
          (let [{:keys [exit out]} (when (<= raw 1000)
                                    (sh "git" "rev-list" "--parents" range))
                lines (when (and (= 0 exit) (not (str/blank? out))) (str/split-lines out))
                ignored (if (and (<= raw 1000) (= raw (count lines)))
                          (count (filter archive-only-commit? lines)) 0)]
            {:commits-behind (- raw ignored) :raw-commits-behind raw
             :ignored-archive-commits ignored}))))))

(defn -main
  [& args]
  (let [[cmd & more] args
        opts (apply hash-map more)]
    (case cmd
      "append"
      (let [entry {:sha (get opts "--sha")
                   :started (get opts "--started")
                   :wall_s (parse-long (str (get opts "--wall-s" "0")))
                   :verdict (keyword (str (get opts "--verdict" "unknown")))
                   :host (get opts "--host" (str/trim (:out (sh "hostname"))))}]
        (append-entry! ledger-path entry)
        (println "battery-ledger: appended" (entry-line entry)))

      "check"
      (let [entries (parse-ledger (when (.exists (io/file ledger-path))
                                    (slurp ledger-path)))
            r (freshness entries (System/currentTimeMillis) commits-behind-head)]
        (when (some? (:raw-commits-behind r))
          (println "battery-fresh: distance"
                   (pr-str (select-keys r [:commits-behind :raw-commits-behind
                                          :ignored-archive-commits]))))
        (if (:ok r)
          (do (println (format (str "battery-fresh: OK -- newest receipt sha %s, started %s, "
                                    "wall %ss, %.1f h old, %d commit(s) behind HEAD")
                               (:sha (:entry r)) (:started (:entry r))
                               (:wall_s (:entry r)) (:age-hours r) (:commits-behind r)))
              (System/exit 0))
          (do (println (str "battery-fresh: REFUSED (" (name (:reason r)) ") -- " (:message r)))
              (println (:remedy r))
              (System/exit 1))))

      (do (println "usage: battery_ledger.clj append --sha S --started T --wall-s N --verdict pass|fail")
          (println "       battery_ledger.clj check")
          (System/exit 2)))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
