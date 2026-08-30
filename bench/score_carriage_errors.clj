#!/usr/bin/env bb
;; score_carriage_errors.clj — fold the X5 carriage probe into verdicts.
;;
;; Companion to bench/measure_carriage_errors.clj. The probe records what was
;; asked, what came back, and whether it parsed. This fold decides what the rates
;; mean, classifies the malformations by KIND, and converts a rate into turns.
;;
;; THE CONVERSION THAT DECIDES
;;   A malformed request costs a whole retry turn. Appendix A priced the floor at
;;   ~3.9 s and a turn at ~222 output tokens, so turns dominate bytes by two to
;;   three orders of magnitude. This fold therefore reports the byte and token
;;   deltas AND the expected turn cost per hundred requests, because only the
;;   second one can justify a migration.
;;
;; Usage: bb bench/score_carriage_errors.clj RESULT_DIR [--markdown]

(ns score-carriage-errors
  (:require
   [cheshire.core :as json]
   [clojure.string :as str]))

(def score-schema "clj-surgeon.carriage-error-score/v1")

(defn round [x n]
  (when x (let [f (Math/pow 10 n)] (/ (Math/round (* (double x) f)) f))))

(defn median [xs]
  (when (seq xs)
    (let [v (vec (sort xs)) n (count v)]
      (if (odd? n) (double (nth v (quot n 2)))
          (/ (+ (nth v (dec (quot n 2))) (nth v (quot n 2))) 2.0)))))

(defn rule-of-three
  "Zero failures in n trials bounds the true rate below ~3/n at 95%. A clean
  sweep is evidence of a LOW rate, never of a zero rate."
  [n errors]
  (when (and n (pos? n) (zero? errors)) (round (/ 3.0 n) 5)))

(defn load-run [dir]
  {:meta (json/parse-string (slurp (str dir "/meta.json")) true)
   :facts (->> (str/split-lines (slurp (str dir "/facts.jsonl")))
               (remove str/blank?)
               (mapv #(json/parse-string % true)))})

;; ------------------------------------------------------------- classification
(defn malformation-kind
  "What KIND of breakage. The mechanism claim is that a leaked JSON-ism degrades
  gracefully in EDN and fatally in JSON, so the kind matters more than the count."
  [{:keys [fenced parse-error parse-ok-after-fence-strip]}]
  (let [e (str/lower-case (str parse-error))]
    (cond
      (and fenced (true? parse-ok-after-fence-strip)) :code-fence-only
      fenced :code-fence-plus-syntax
      (re-find #"unexpected end|eof|end-of-file|unexpected eof" e) :truncated
      (re-find #"escape" e) :bad-escape
      (re-find #"unterminated|unbalanced|eof while reading string" e) :unterminated-string
      (re-find #"unexpected character|unrecognized token|illegal" e) :unexpected-character
      (re-find #"no \"request\" key" e) :missing-required-key
      (re-find #"map literal must contain an even" e) :odd-map-entries
      (str/blank? e) :none
      :else :other)))

(defn out-tokens [f]
  (let [u (:usage f)]
    (when u (+ (or (:output_tokens u) 0) (or (:reasoning_output_tokens u) 0)))))

;; -------------------------------------------------------------------- scoring
(defn score-arm [facts arm]
  (let [all (remove :is-retry (filter #(= arm (:arm %)) facts))
        adherent (filter :route-adherent all)
        n (count adherent)
        bad (remove :parse-ok adherent)
        n-bad (count bad)
        fence-only (filter #(= :code-fence-only (malformation-kind %)) bad)
        content-checked (filter :parse-ok adherent)
        n-content-bad (count (remove :content-exact content-checked))
        retries (filter #(and (= arm (:arm %)) (:is-retry %)) facts)]
    {:arm arm
     :trials (count all)
     :route-adherent-n n
     :route-adherent (when (seq all) (round (/ n (double (count all))) 3))
     :malformed n-bad
     :malformed-rate (when (pos? n) (round (/ n-bad (double n)) 4))
     :malformed-rate-95pct-upper-bound (rule-of-three n n-bad)
     ;; Reported separately so a reader who considers a fence a harness artefact
     ;; rather than a carriage failure can subtract it and re-rank.
     :malformed-excluding-fence-only (- n-bad (count fence-only))
     :malformed-rate-excluding-fence-only
     (when (pos? n) (round (/ (- n-bad (count fence-only)) (double n)) 4))
     :kinds (frequencies (map malformation-kind bad))
     :parsed-but-wrong-payload n-content-bad
     :content-exact-rate (when (seq content-checked)
                           (round (/ (count (filter :content-exact content-checked))
                                     (double (count content-checked))) 4))
     :median-output-bytes (round (median (map :output-bytes adherent)) 0)
     :median-output-tokens (round (median (keep out-tokens adherent)) 0)
     :median-wall-ms (round (median (map :wall-ms adherent)) 0)
     :retry {:n (count retries)
             :median-wall-ms (round (median (map :wall-ms retries)) 0)
             :median-output-tokens (round (median (keep out-tokens retries)) 0)
             :recovered (count (filter :parse-ok retries))}
     :failures (vec (for [f bad]
                      {:tag (:tag f) :task (:task f) :hazard (:hazard f)
                       :kind (malformation-kind f) :error (:parse-error f)
                       :fenced (:fenced f)}))
     :payload-corruptions (vec (for [f (remove :content-exact content-checked)]
                                 {:tag (:tag f) :task (:task f)
                                  :expected (:expected-new f) :got (:got-new f)}))}))

(defn score [{:keys [meta facts]}]
  (let [arms (:arms meta)
        per (into {} (for [a arms] [a (score-arm facts a)]))
        j (get per "J")
        jr (:malformed-rate j)
        ;; One retry turn is the unit that matters. Prefer a measured retry cost;
        ;; fall back to the working-floor figure only when nothing was malformed.
        retry-wall (or (get-in j [:retry :median-wall-ms])
                       (->> arms (keep #(get-in per [% :retry :median-wall-ms])) first))
        turn-cost-per-100 (fn [rate] (when rate (round (* 100 rate) 2)))]
    {:schema score-schema
     :meta (dissoc meta :tasks)
     :prediction (:prediction meta)
     :kill-criterion (:kill-criterion meta)
     :arms per
     :comparison
     (into {}
           (for [a arms :when (not= a "J")
                 :let [r (:malformed-rate (get per a))]]
             [a {:malformed-rate r
                 :json-malformed-rate jr
                 :ratio-to-json (when (and r jr (pos? jr)) (round (/ r jr) 3))
                 :prediction-held (when (and r jr) (<= r jr))
                 :kill-criterion-triggered (when (and r jr) (> r (* 2 jr)))
                 :extra-retry-turns-per-100-requests
                 (when (and r jr) (round (* 100 (- r jr)) 2))
                 :median-output-bytes-delta-vs-json
                 (when (and (:median-output-bytes (get per a)) (:median-output-bytes j))
                   (round (- (:median-output-bytes (get per a)) (:median-output-bytes j)) 0))
                 :median-output-tokens-delta-vs-json
                 (when (and (:median-output-tokens (get per a)) (:median-output-tokens j))
                   (round (- (:median-output-tokens (get per a)) (:median-output-tokens j)) 0))}]))
     :retry-turn-cost {:median-wall-ms retry-wall
                       :note "Cost of ONE recovery turn. Multiply by the malformed rate to get the real price of a carriage."}
     :turns-per-100-requests (into {} (for [a arms] [a (turn-cost-per-100 (:malformed-rate (get per a)))]))
     :verdict
     (let [rates (into {} (for [a arms] [a (:malformed-rate (get per a))]))
           all-zero (every? #(= 0.0 (double (or % 1))) (vals rates))
           ns- (into {} (for [a arms] [a (:route-adherent-n (get per a))]))]
       (cond
         all-zero
         (str "NO DIFFERENCE DETECTABLE AT THIS n. Every arm produced zero malformed requests ("
              (str/join ", " (for [a arms] (str a " n=" (get ns- a))))
              "). The true rates are bounded only below the rule-of-three values in each arm; "
              "this run cannot distinguish them and does not justify a migration on robustness grounds.")
         :else
         (str "Malformed rates: "
              (str/join ", " (for [a arms] (str a "=" (get rates a))))
              ". Rank on kind as well as rate; see :kinds per arm.")))
     :confounds
     ["The model was asked to EMIT a request as text, not to call a tool. A real tool call is constrained by a schema the harness cannot reproduce here, so absolute rates are not the production rates; the COMPARISON between carriages is what transfers."
      "E-wrapped still pays JSON escaping for the one string it occupies, so it is not a clean EDN arm. That is why E-raw exists: E-wrapped minus E-raw is how much the JSON envelope gives back."
      "Ten fixtures chosen to be adversarial. They over-represent hazards relative to real write traffic, which inflates absolute malformed rates in every arm and is intended to."
      "One model at one reasoning effort. A weaker model is exactly where a carriage difference would appear, and this run does not test one."
      "A code fence is counted as malformed because a parser rejects it, but it is reported separately since it is arguably a prompt-format failure rather than a carriage failure."
      "Rates near zero cannot be separated at this n. Rule-of-three bounds are given so the reader can see what the run can and cannot support."]}))

;; --------------------------------------------------------------------- render
(defn markdown [s]
  (let [f #(if (nil? %) "n/a" (str %))
        pct #(if (nil? %) "n/a" (str (round (* 100 %) 2) "%"))
        arms (get-in s [:meta :arms])]
    (str/join
     "\n"
     (concat
      [(str "Model: " (get-in s [:meta :model])
            " | host: " (get-in s [:meta :hostname])
            " | loadavg at start: " (get-in s [:meta :loadavg-start])
            " | fixtures: " (get-in s [:meta :n-tasks])
            " | replicates: " (get-in s [:meta :replicates]))
       (str "PREDECLARED prediction: " (:prediction s))
       (str "PREDECLARED kill criterion: " (:kill-criterion s))
       ""
       "| Arm | trials | route-adherent | malformed | rate | excl. fence-only | payload wrong | median out bytes | median out tokens |"
       "|---|---|---|---|---|---|---|---|---|"]
      (for [a arms :let [x (get-in s [:arms a])]]
        (str "| " a " | " (:trials x) " | " (f (:route-adherent x)) " | "
             (:malformed x) " | " (pct (:malformed-rate x)) " | "
             (pct (:malformed-rate-excluding-fence-only x)) " | "
             (:parsed-but-wrong-payload x) " | "
             (f (:median-output-bytes x)) " | " (f (:median-output-tokens x)) " |"))
      [""]
      (for [a arms :let [x (get-in s [:arms a])] :when (:malformed-rate-95pct-upper-bound x)]
        (str "  " a ": 0 malformed in " (:route-adherent-n x)
             " -> true rate bounded below "
             (round (* 100 (:malformed-rate-95pct-upper-bound x)) 2)
             "% (95%, rule of three). Zero observed is not zero."))
      [""
       "MALFORMATION KINDS (the mechanism, not just the rate):"]
      (for [a arms :let [x (get-in s [:arms a])]]
        (str "  " a ": " (if (seq (:kinds x)) (pr-str (:kinds x)) "none")))
      [""
       "COMPARISON VS JSON:"]
      (for [a arms :when (not= a "J") :let [c (get-in s [:comparison a])]]
        (str "  " a ": rate " (pct (:malformed-rate c))
             " vs JSON " (pct (:json-malformed-rate c))
             " | ratio " (f (:ratio-to-json c))
             " | prediction held: " (f (:prediction-held c))
             " | KILL triggered: " (f (:kill-criterion-triggered c))
             "\n      extra retry turns per 100 requests: " (f (:extra-retry-turns-per-100-requests c))
             " | bytes delta " (f (:median-output-bytes-delta-vs-json c))
             " | tokens delta " (f (:median-output-tokens-delta-vs-json c))))
      [""]
      (if-let [fs (seq (mapcat #(:failures (get-in s [:arms %])) arms))]
        (concat ["FAILURES, verbatim:"]
                (for [x fs]
                  (str "  [" (:tag x) "] kind=" (name (:kind x))
                       "\n    hazard: " (:hazard x)
                       "\n    error:  " (:error x))))
        ["FAILURES: none in any arm."])
      [""]
      (if-let [cs (seq (mapcat #(:payload-corruptions (get-in s [:arms %])) arms))]
        (concat ["PARSED BUT PAYLOAD WRONG (worse than a parse failure — this one executes):"]
                (for [x cs]
                  (str "  [" (:tag x) "] task=" (:task x)
                       "\n    expected: " (pr-str (:expected x))
                       "\n    got:      " (pr-str (:got x)))))
        ["PARSED BUT PAYLOAD WRONG: none."])
      [""
       (str "RETRY TURN COST: " (f (get-in s [:retry-turn-cost :median-wall-ms])) " ms median")
       (str "TURNS PER 100 REQUESTS: " (pr-str (:turns-per-100-requests s)))
       ""
       (str "VERDICT: " (:verdict s))
       ""
       "Confounds:"]
      (map #(str "  - " %) (:confounds s))))))

(defn -main [& args]
  (let [dir (first args) md? (some #{"--markdown"} args)]
    (when-not dir
      (println "usage: bb bench/score_carriage_errors.clj RESULT_DIR [--markdown]")
      (System/exit 2))
    (let [s (score (load-run dir))]
      (if md? (println (markdown s)) (println (json/generate-string s {:pretty true}))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
