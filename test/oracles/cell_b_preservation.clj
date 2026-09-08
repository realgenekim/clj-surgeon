(ns oracles.cell-b-preservation
  "Independent Cell B structural preservation oracle; no Surgeon compiler dependencies."
  (:refer-clojure :exclude [run!])
  (:require
   [clojure.java.io :as io]
   [clojure.java.shell :as shell]
   [clojure.set :as set]
   [rewrite-clj.node :as n]
   [rewrite-clj.parser :as parser]))

(def base "92a7ca14fd904e4d962df38614b9af3a7ef41a11")
(def source-file "src/cfp_scheduler_killer/exports.clj")
(def dest-file "src/cfp_scheduler_killer/exports/calendar.clj")
(def source-lib 'cfp-scheduler-killer.exports)
(def dest-lib 'cfp-scheduler-killer.exports.calendar)
(def moved '#{calendar-prodid ical-domain ics-escape fold-line ics-date ics-stamp ics-uid
              ics-sequence ics-local-datetime absolute-link vevent snapshot-published?
              last-publication-snapshot ics-sequence-at cancellation-vevent cancellation-for
              hold-the-date-uid hold-the-date-sequence hold-the-date-vevent cfp-deadline-uid
              cfp-deadline-sequence cfp-deadline-ics submission-ics calendar-ics-for calendar-ics})
(def retained '#{communicated? content-publishable? date-range-string public-answers
                 publishable-sessions published? room-name speaker-names})
(def promoted '#{communicated? content-publishable? speaker-names})
(def caller-files ["src/cfp_scheduler_killer/agent/commands.clj"
                   "src/cfp_scheduler_killer/handlers/exports.clj"
                   "src/cfp_scheduler_killer/handlers/public_cfp.clj"
                   "src/cfp_scheduler_killer/handlers/public_widgets.clj"
                   "src/cfp_scheduler_killer/inform.clj"
                   "src/cfp_scheduler_killer/session_invites.clj"
                   "test/cfp_scheduler_killer/comms_test.clj"
                   "test/cfp_scheduler_killer/exports_test.clj"
                   "test/cfp_scheduler_killer/schedule_test.clj"])
(defn value [node] (try (n/sexpr node) (catch Exception _ nil)))
(defn nodes [source] (n/children (parser/parse-string-all source)))
(defn head [node] (when (= :list (n/tag node)) (first (value node))))
(defn named [source]
  (into {} (for [node (nodes source) :when (#{'def 'defn 'defn-} (head node))]
             [(second (value node)) node])))
(defn aliases [source]
  (let [ns-form (some #(when (= 'ns (head %)) (value %)) (nodes source))]
    (into {} (for [clause (drop 2 ns-form) :when (and (sequential? clause) (= :require (first clause)))
                   entry (rest clause) :when (vector? entry)
                   :let [alias (:as (apply hash-map (rest entry)))] :when alias]
               [(str alias) (first entry)]))))
(defn tokens [node symbol-normalizer]
  (vec (mapcat (fn walk [node]
                 (cond
                   (n/whitespace? node) []
                   (n/inner? node) (concat [[(n/tag node) :open]] (mapcat walk (n/children node)) [[(n/tag node) :close]])
                   (and (= :token (n/tag node)) (symbol? (value node))) [(symbol-normalizer (value node))]
                   :else [(n/string node)])) [node])))
(defn canonical-symbol [aliases old? sym]
  (let [lib (or (get aliases (namespace sym)) (some-> (namespace sym) symbol))
        local (symbol (name sym))]
    (if (and (namespace sym) (moved local) (= (if old? source-lib dest-lib) lib))
      (symbol "CALENDAR" (name sym)) sym)))
(defn comment-tokens [source remove?]
  (letfn [(prune [node]
            (if (and remove? (= '(print (calendar-ics e)) (value node)))
              nil
              (if (n/inner? node) (n/replace-children node (keep prune (n/children node))) node)))]
    (mapv #(tokens (prune %) identity) (filter #(= 'comment (head %)) (nodes source)))))

;; @spec NS-SPLIT-043
;; INTENT: NS-SPLIT-043
(defn check-sources
  "Independent literal policy; old/new maps hold only the fixed authorized footprint."
  [old current]
  (let [before (named (get old source-file))
        after (named (get current source-file ""))
        destination (named (get current dest-file ""))
        dest-aliases (aliases (get current dest-file ""))
        normal-dest (fn [sym]
                      (let [lib (or (get dest-aliases (namespace sym)) (some-> (namespace sym) symbol))
                            local (symbol (name sym))]
                        (if (or (and (= source-lib lib) (retained local))
                                (and (= dest-lib lib) (moved local))) local sym)))
        moved-drift (for [name moved :when (not= (some-> (get before name) (tokens identity))
                                             (some-> (get destination name) (tokens normal-dest)))] name)
        retained-drift (for [[name node] before :when (not (moved name))
                             :let [normalize #(if (and (promoted name) (= 'defn- %)) 'defn %)]
                             :when (not= (tokens node normalize) (some-> (get after name) (tokens identity)))] name)
        caller-drift (for [file caller-files
                           :let [old-ns (aliases (get old file "")) new-ns (aliases (get current file ""))
                                 body (fn [src normalize] (mapv #(tokens % normalize) (remove #(= 'ns (head %)) (nodes src))))]
                           :when (not= (body (get old file "") #(canonical-symbol old-ns true %))
                                       (body (get current file "") #(canonical-symbol new-ns false %)))] file)
        src-aliases (aliases (get current source-file ""))]
    {:inventory (= moved (set (keys destination)))
     :retained-inventory (= (set/difference (set (keys before)) moved) (set (keys after)))
     :retained-qualified (not-any? #(and (= :token (n/tag %)) (retained (value %)))
                                   (mapcat #(tree-seq n/inner? n/children %) (vals destination)))
     :moved-bodies (empty? moved-drift) :moved-drift (vec moved-drift)
     :retained-bodies (empty? retained-drift) :retained-drift (vec retained-drift)
     :mixed-callers (empty? caller-drift) :caller-drift (vec caller-drift)
     :comment-policy (= (comment-tokens (get old source-file) true)
                        (comment-tokens (get current source-file "") false))
     :acyclic-direction (not-any? #{dest-lib} (vals src-aliases))}))

(def check-keys [:inventory :retained-inventory :retained-qualified :moved-bodies :retained-bodies :mixed-callers :comment-policy :acyclic-direction])
(defn capture [root]
  {:old (into {} (for [file (cons source-file caller-files)
                       :let [p (shell/sh "git" "-C" root "show" (str base ":" file))]]
                   (do (when-not (zero? (:exit p)) (throw (ex-info "baseline unavailable" {:file file}))) [file (:out p)])))
   :current (into {} (for [file (concat [source-file dest-file] caller-files)
                           :let [f (io/file root file)]] [file (if (.exists f) (slurp f) "")]))})
(defn run! [root]
  (let [{:keys [old current]} (capture root)
        result (check-sources old current)]
    (doseq [k check-keys] (println (if (get result k) "PASS" "FAIL") "B07 preservation" (name k)))
    (prn (select-keys result [:moved-drift :retained-drift :caller-drift]))
    (println "B07 PAPERCUTS:" (count (remove #(get result %) check-keys)))
    (every? #(get result %) check-keys)))

(when (= *file* (System/getProperty "babashka.file"))
  (System/exit (if (run! (or (first *command-line-args*) ".")) 0 1)))
