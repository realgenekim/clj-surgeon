#!/usr/bin/env bb
;; measure_transcription_fidelity.clj — does the model copy an identifier EXACTLY?
;;
;; WHY THIS EXISTS
;;   The "identity-echo" design has the server pre-compose candidate calls and the
;;   model accept one by echoing the candidate's subject verbatim. Every byte of
;;   that safety argument rests on an assumption nobody had tested: that a model
;;   asked to copy an identifier out of its own input reproduces it exactly.
;;
;;   If transcription is even slightly lossy the design's central claim INVERTS.
;;   A garbled subject is a WRONG SUBJECT, produced by the very mechanism adopted
;;   to prevent wrong subjects.
;;
;;   Gene, 2026-08-29: "Don't let logic or interpretation deter you / them from
;;   firing off a cheap test and experiment to prove or disprove."
;;
;; THE CRITICAL DISTINCTION, and the reason this test is worth running
;;   A wrong reproduction is not one failure mode but two, with opposite safety
;;   properties:
;;     GARBAGE                  -> safe. No such subject exists; the server refuses.
;;     VALID OTHER IDENTIFIER   -> the silent wrong-subject failure. It executes.
;;   The fold classifies every miss into one of these. A low error rate made
;;   entirely of valid-other-identifiers is WORSE than a higher rate of garbage.
;;
;; THE ARMS
;;   F        reproduce all N identifiers in order   -> raw transcription fidelity
;;   S-echo   select by description, answer with the full identifier
;;   S-ord    select by description, answer with the item number
;;   S-echo vs S-ord is the design question: same selection task, same difficulty,
;;   differing ONLY in how the chosen subject is encoded on the way out.
;;
;; PREDECLARED VALIDITY
;;   * A trial is ROUTE-ADHERENT if it exits 0 and returns the expected NUMBER of
;;     answer lines. A trial that answers a different number of questions is
;;     reported separately and excluded from the accuracy rate; it is a
;;     format failure, not a transcription failure, and conflating them would
;;     flatter whichever arm is worse at following the format.
;;   * Comparison is byte-exact after trimming surrounding whitespace. Nothing
;;     else is normalised. Case, underscores, and trailing punctuation all count.
;;   * Losses stay in the chart. A high error rate is the valuable outcome here.
;;
;; USAGE
;;   bb bench/measure_transcription_fidelity.clj OUT_DIR [REPLICATES]

(ns measure-transcription-fidelity
  (:require
   [babashka.process :as p]
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(def probe-schema "clj-surgeon.transcription-fidelity-probe/v1")

;; ---------------------------------------------------------------- the corpus
;; Adversarial in the ways the real corpus is adversarial. Easy names would
;; measure nothing: the whole question is what happens when the discriminating
;; character is deep in the string or is a single underscore.
(def corpus
  [;; group 1 — near-identical file names behind the same function name
   {:id "src/sample/views/review.clj#render-review"                :desc "renders a single review"                      :group "near-identical-file"}
   {:id "src/sample/views/reviews.clj#render-review"               :desc "renders the review index page"                :group "near-identical-file"}
   {:id "src/sample/views/review_updates.clj#render-review-row"    :desc "renders one row of the review updates table"  :group "near-identical-file"}
   ;; group 2 — the classic Clojure underscore-file / hyphen-name trap
   {:id "src/sample/db/review_queries.clj#fetch-review"            :desc "fetches a review from the database"           :group "underscore-hyphen"}
   {:id "src/sample/db/review_queries.clj#fetch_review"            :desc "the legacy database fetch kept for the old API" :group "underscore-hyphen"}
   {:id "src/sample/db/review-queries.clj#fetch-review"            :desc "fetches a review through the deprecated path"  :group "underscore-hyphen"}
   ;; group 3 — trailing punctuation only
   {:id "src/sample/domain/validate.clj#valid-review?"             :desc "tests whether a review is valid"              :group "trailing-punctuation"}
   {:id "src/sample/domain/validate.clj#valid-review"              :desc "returns the validity record for a review"     :group "trailing-punctuation"}
   {:id "src/sample/domain/validate.clj#valid-review!"             :desc "asserts review validity and throws on failure" :group "trailing-punctuation"}
   {:id "src/sample/domain/transform.clj#normalize*"               :desc "the unmemoised normalisation primitive"       :group "trailing-punctuation"}
   {:id "src/sample/domain/transform.clj#normalize"                :desc "normalises a value for storage"               :group "trailing-punctuation"}
   ;; group 4 — case-only difference
   {:id "src/sample/util/HTTPClient.clj#send-request"              :desc "sends a request through the upper-case client" :group "case-only"}
   {:id "src/sample/util/HttpClient.clj#send-request"              :desc "sends a request through the mixed-case client" :group "case-only"}
   ;; group 5 — long shared prefix, discriminator deep in the string
   {:id "src/sample/reporting/aggregation/quarterly_summary_builder.clj#build-quarterly-summary"    :desc "builds one quarterly summary"            :group "deep-discriminator"}
   {:id "src/sample/reporting/aggregation/quarterly_summary_builder.clj#build-quarterly-summaries"  :desc "builds every quarterly summary at once"  :group "deep-discriminator"}
   {:id "src/sample/reporting/aggregation/quarterly_summary_builders.clj#build-quarterly-summary"   :desc "builds a quarterly summary using the pluralised builder namespace" :group "deep-discriminator"}
   ;; group 6 — digit confusables
   {:id "src/sample/migrations/v2_add_index.clj#migrate-v2"        :desc "the migration that adds a single index"       :group "digit-confusable"}
   {:id "src/sample/migrations/v21_add_index.clj#migrate-v21"      :desc "the twenty-first migration"                   :group "digit-confusable"}
   {:id "src/sample/migrations/v2_add_indexes.clj#migrate-v2"      :desc "the migration that adds several indexes"      :group "digit-confusable"}
   ;; group 7 — far-apart controls
   {:id "src/sample/core.clj#start-system"                         :desc "starts the system"                            :group "control"}
   {:id "src/sample/config.clj#load-config"                        :desc "loads configuration"                          :group "control"}
   {:id "src/sample/http/routes.clj#app-routes"                    :desc "defines the application routes"               :group "control"}
   {:id "src/sample/cache/lru.clj#evict-oldest"                    :desc "evicts the oldest cache entry"                :group "control"}
   {:id "src/sample/mail/sender.clj#deliver-message"               :desc "delivers an outbound message"                 :group "control"}])

;; ------------------------------------------------------------------ distance
(defn edit-distance [a b]
  (let [m (count a) n (count b)]
    (loop [i 1 prev (vec (range (inc n)))]
      (if (> i m)
        (peek prev)
        (recur (inc i)
               (loop [j 1 row [i]]
                 (if (> j n)
                   row
                   (recur (inc j)
                          (conj row (min (inc (nth row (dec j)))
                                         (inc (nth prev j))
                                         (+ (nth prev (dec j))
                                            (if (= (nth a (dec i)) (nth b (dec j))) 0 1))))))))))))

(defn nearest-neighbour-distance
  "How many characters separate this identifier from its closest sibling. The
  error rate is reported against this, because confusability is the mechanism
  under test."
  [id all]
  (->> all (remove #(= % id)) (map #(edit-distance id %)) (reduce min)))

;; ------------------------------------------------------------------- prompts
(defn numbered-block [items]
  (str/join "\n"
            (map-indexed (fn [i {:keys [id desc]}]
                           (format "%2d. %s  -- %s" (inc i) id desc))
                         items)))

(def no-chatter
  "Output only the answer. No commentary, no preamble, no numbering beyond what is asked for, no code fences, no tool use.")

(defn prompt-F [items]
  (str "Below is a numbered list of code identifiers.\n\n"
       (numbered-block items)
       "\n\nReproduce EVERY identifier above exactly as written, one per line, in the same order. "
       "Do not include the numbers and do not include the descriptions. " no-chatter))

(defn prompt-S-echo [items targets]
  (str "Below is a numbered list of code identifiers with descriptions.\n\n"
       (numbered-block items)
       "\n\nFor each question below, output ONLY the full identifier of the matching candidate, "
       "exactly as written above, one per line, in question order. " no-chatter
       "\n\nQuestions:\n"
       (str/join "\n" (map-indexed (fn [i t] (format "%d. Which candidate %s?" (inc i) (:desc t))) targets))))

(defn prompt-S-ord [items targets]
  (str "Below is a numbered list of code identifiers with descriptions.\n\n"
       (numbered-block items)
       "\n\nFor each question below, output ONLY the item NUMBER of the matching candidate, "
       "one number per line, in question order. " no-chatter
       "\n\nQuestions:\n"
       (str/join "\n" (map-indexed (fn [i t] (format "%d. Which candidate %s?" (inc i) (:desc t))) targets))))

;; -------------------------------------------------------------------- codex
(def codex-args
  ["codex" "exec" "--json" "--skip-git-repo-check" "--ephemeral"
   "-s" "read-only" "--ignore-user-config"
   "-c" "model=gpt-5.6-sol" "-c" "model_reasoning_effort=low" "-"])

(defn run-codex
  "Returns {:message ... :usage ... :exit ... :wall-ms ...}. Raw, unjudged."
  [prompt cwd]
  (let [t0 (System/nanoTime)
        {:keys [out exit]} (apply p/sh {:in prompt :dir cwd :continue true} codex-args)
        wall (/ (- (System/nanoTime) t0) 1e6)
        events (->> (str/split-lines (or out ""))
                    (remove str/blank?)
                    (keep #(try (json/parse-string % true) (catch Exception _ nil))))
        msg (->> events (keep #(when (= "agent_message" (get-in % [:item :type]))
                                 (get-in % [:item :text]))) first)]
    {:message (or msg "")
     :usage (->> events (keep :usage) last)
     :exit exit
     :wall-ms (Math/round (double wall))}))

;; --------------------------------------------------------------------- main
(defn answer-lines [s]
  (->> (str/split-lines (str/trim (or s "")))
       (map str/trim)
       (remove str/blank?)
       vec))

(defn slurp-loadavg []
  (try (:out (p/sh "sh" "-c" "cut -d' ' -f1-3 /proc/loadavg")) (catch Exception _ "unknown")))

(defn -main [& args]
  (let [out-dir (or (first args) (throw (ex-info "usage: OUT_DIR [REPLICATES]" {})))
        reps (Integer/parseInt (or (second args) "9"))
        work (str out-dir "/work")
        _ (.mkdirs (io/file work))
        _ (.mkdirs (io/file (str out-dir "/raw")))
        ids (mapv :id corpus)
        facts (atom [])]
    (spit (str out-dir "/meta.json")
          (json/generate-string
           {:schema probe-schema
            :replicates reps
            :corpus corpus
            :corpus-size (count corpus)
            :model "gpt-5.6-sol" :reasoning "low"
            :codex-args (str/join " " codex-args)
            :hostname (str/trim (:out (p/sh "hostname")))
            :loadavg (str/trim (slurp-loadavg))
            :started (str (java.time.Instant/now))}
           {:pretty true}))
    (doseq [rep (range 1 (inc reps))]
      ;; A fresh shuffle per replicate: a fixed order would let one lucky or
      ;; unlucky layout stand in for the whole result.
      (let [items (shuffle corpus)
            ;; six targets per replicate, biased toward the adversarial groups
            targets (vec (concat (take 4 (shuffle (remove #(= "control" (:group %)) items)))
                                 (take 2 (shuffle (filter #(= "control" (:group %)) items)))))]
        (doseq [[arm prompt expected]
                [["F" (prompt-F items) (mapv :id items)]
                 ["S-echo" (prompt-S-echo items targets) (mapv :id targets)]
                 ["S-ord" (prompt-S-ord items targets)
                  (mapv #(str (inc (.indexOf ^java.util.List items %))) targets)]]]
          (let [tag (format "r%02d-%s" rep arm)
                {:keys [message usage exit wall-ms]} (run-codex prompt work)
                got (answer-lines message)]
            (spit (str out-dir "/raw/" tag ".prompt.txt") prompt)
            (spit (str out-dir "/raw/" tag ".message.txt") message)
            (swap! facts conj
                   {:tag tag :replicate rep :arm arm
                    :exit exit :wall-ms wall-ms
                    :usage usage
                    :expected expected
                    :got got
                    :n-expected (count expected)
                    :n-got (count got)
                    ;; the ordered candidate list this replicate actually saw,
                    ;; so a miss can be classified against the real block
                    :items (mapv :id items)
                    :targets (mapv :id targets)})
            (println (format "%-12s exit=%s wall=%sms expected=%d got=%d"
                             tag exit wall-ms (count expected) (count got)))))))
    (spit (str out-dir "/facts.jsonl")
          (str/join "\n" (map json/generate-string @facts)))
    (spit (str out-dir "/nearest.json")
          (json/generate-string
           (into {} (for [id ids] [id (nearest-neighbour-distance id ids)]))
           {:pretty true}))
    (println "\nprobe complete. fold with:")
    (println "  bb bench/score_transcription_fidelity.clj" out-dir)))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
