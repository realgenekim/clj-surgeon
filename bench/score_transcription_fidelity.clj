#!/usr/bin/env bb
;; score_transcription_fidelity.clj — fold the fidelity probe into verdicts.
;;
;; Companion to bench/measure_transcription_fidelity.clj. The probe records what
;; was asked and what came back; this fold decides what counts as a miss and what
;; KIND of miss it was.
;;
;; THE CLASSIFICATION THAT MATTERS
;;   GARBAGE  the answer names no candidate in the block. The server refuses it.
;;            Safe. Loud. Recoverable.
;;   VALID-OTHER  the answer names a DIFFERENT REAL candidate. The server accepts
;;            it and mutates the wrong subject, returning ok=true. This is the
;;            silent wrong-subject failure, arriving through the mechanism
;;            adopted to prevent it.
;;
;;   Note the asymmetry the fold makes visible: an ORDINAL has almost no garbage
;;   failure mode. Nearly every way to get a number wrong yields another valid
;;   number, so ordinal errors land almost entirely in the dangerous bucket. A
;;   mistyped identifier usually lands in the safe one. Raw accuracy alone
;;   therefore cannot rank the two encodings.
;;
;; Usage: bb bench/score_transcription_fidelity.clj RESULT_DIR [--markdown]

(ns score-transcription-fidelity
  (:require
   [cheshire.core :as json]
   [clojure.string :as str]))

(def score-schema "clj-surgeon.transcription-fidelity-score/v1")

(defn round [x n]
  (when x (let [f (Math/pow 10 n)] (/ (Math/round (* (double x) f)) f))))

(defn load-run [dir]
  {:meta (json/parse-string (slurp (str dir "/meta.json")) true)
   :nearest (json/parse-string (slurp (str dir "/nearest.json")))
   :facts (->> (str/split-lines (slurp (str dir "/facts.jsonl")))
               (remove str/blank?)
               (mapv #(json/parse-string % true)))})

(defn adherent?
  "PREDECLARED: a trial counts only if it exited 0 and returned exactly the
  expected number of answer lines. A trial that answered a different number of
  questions failed the FORMAT, not the transcription, and folding it into the
  accuracy rate would flatter whichever arm is worse at following instructions."
  [f]
  (and (= 0 (:exit f)) (= (:n-expected f) (:n-got f))))

(defn classify
  "One answer, judged. `items` is the candidate block this replicate saw."
  [arm expected got items]
  (let [item-set (set items)
        ordinal-arm? (= "S-ord" arm)
        valid-range? (fn [s] (when-let [n (parse-long (str/replace (str s) #"[^0-9]" ""))]
                               (<= 1 n (count items))))]
    (cond
      (= expected got) :exact
      ;; names a different REAL candidate -> executes against the wrong subject
      (or (and (not ordinal-arm?) (contains? item-set got))
          (and ordinal-arm? (valid-range? got)))
      :valid-other
      :else :garbage)))

(defn rule-of-three
  "With zero observed failures in n trials, the 95% upper confidence bound on the
  true failure rate is about 3/n. A clean sweep is evidence of a LOW rate, never
  of a zero rate, and a design that needs a wrong-subject rate below this bound
  is not supported by this run no matter how clean the table looks."
  [n errors]
  (when (and n (pos? n) (zero? errors))
    (round (/ 3.0 n) 5)))

(defn score-arm [facts nearest arm]
  (let [all (filter #(= arm (:arm %)) facts)
        good (filter adherent? all)
        answers (for [f good
                      [exp got] (map vector (:expected f) (:got f))]
                  (let [k (classify arm exp got (:items f))]
                    {:tag (:tag f) :arm arm :expected exp :got got :kind k
                     ;; distance to the nearest confusable sibling, for the
                     ;; error-rate-versus-discriminating-characters question
                     :nn (get nearest exp)}))
        n (count answers)
        by-kind (frequencies (map :kind answers))
        misses (remove #(= :exact (:kind %)) answers)]
    {:arm arm
     :trials-attempted (count all)
     :trials-adherent (count good)
     :route-adherent (when (seq all) (round (/ (count good) (double (count all))) 3))
     :answers n
     :exact (get by-kind :exact 0)
     :exact-rate (when (pos? n) (round (/ (get by-kind :exact 0) (double n)) 4))
     :valid-other (get by-kind :valid-other 0)
     :garbage (get by-kind :garbage 0)
     :dangerous-rate (when (pos? n) (round (/ (get by-kind :valid-other 0) (double n)) 4))
     :error-rate-95pct-upper-bound (rule-of-three n (count (remove #(= :exact (:kind %)) answers)))
     :misses (vec misses)
     ;; error rate bucketed by how many characters separate the target from its
     ;; nearest sibling. If confusability is the mechanism, errors concentrate
     ;; in the low buckets.
     :by-nearest-distance
     (into (sorted-map)
           (for [[d as] (group-by :nn (filter :nn answers))]
             [d {:n (count as)
                 :errors (count (remove #(= :exact (:kind %)) as))
                 :error-rate (round (/ (count (remove #(= :exact (:kind %)) as))
                                       (double (count as))) 4)}]))}))

(defn score [{:keys [meta facts nearest]}]
  (let [arms ["F" "S-echo" "S-ord"]
        per-arm (into {} (for [a arms
                               :let [s (score-arm facts nearest a)]
                               :when (pos? (:trials-attempted s))]
                           [a s]))]
    {:schema score-schema
     :meta (dissoc meta :corpus)
     :corpus-size (:corpus-size meta)
     :arms per-arm
     :design-question
     (let [e (get per-arm "S-echo") o (get per-arm "S-ord")]
       (when (and e o)
         {:note "Same selection task, same block, differing only in how the chosen subject is encoded on the way out."
          :echo-exact-rate (:exact-rate e)
          :ordinal-exact-rate (:exact-rate o)
          :echo-dangerous-rate (:dangerous-rate e)
          :ordinal-dangerous-rate (:dangerous-rate o)
          :verdict
          (cond
            (and (= 0 (:valid-other e)) (= 0 (:valid-other o))
                 (= (:exact-rate e) (:exact-rate o)))
            "Indistinguishable on this corpus: both encodings were exact and neither produced a wrong-subject."
            (< (:dangerous-rate e 0) (:dangerous-rate o 0))
            "Echo produced fewer silent wrong-subject answers than ordinals."
            (> (:dangerous-rate e 0) (:dangerous-rate o 0))
            "Ordinals produced fewer silent wrong-subject answers than echo."
            :else "Equal dangerous rates; rank on exactness and on failure KIND, not on the headline number.")}))
     :confounds
     ["A 24-candidate block. Confusability grows with block size; this does not measure a 500-candidate catalogue."
      "One model at one reasoning effort. A cheaper model or a longer context could transcribe worse."
      "The selection arms use descriptions written by the same author as the corpus, so selection difficulty is controlled but not realistic."
      "An ordinal has almost no garbage failure mode: nearly every wrong number is still a valid number. Ordinal errors therefore land in the dangerous bucket by construction, and raw exactness cannot be compared across encodings without this classification."
      "Exactness here is byte equality after trimming. A downstream resolver that normalises case or separators would mask errors this test counts, and would introduce wrong-subject risk this test does not measure."]}))

(defn markdown [s]
  (let [f #(if (nil? %) "n/a" (str %))
        pct #(if (nil? %) "n/a" (str (round (* 100 %) 2) "%"))]
    (str/join
     "\n"
     (concat
      [(str "Corpus: " (:corpus-size s) " adversarial identifiers | model: "
            (get-in s [:meta :model]) " | host: " (get-in s [:meta :hostname])
            " | loadavg: " (str/trim (str (get-in s [:meta :loadavg]))))
       ""
       "| Arm | trials | route-adherent | answers | exact | exact rate | VALID-OTHER (dangerous) | garbage (safe) |"
       "|---|---|---|---|---|---|---|---|"]
      (for [[k a] (:arms s)]
        (str "| " k " | " (:trials-adherent a) "/" (:trials-attempted a) " | "
             (f (:route-adherent a)) " | " (:answers a) " | " (:exact a) " | "
             (pct (:exact-rate a)) " | " (:valid-other a) " | " (:garbage a) " |"))
      [""]
      (for [[k a] (:arms s)
            :when (:error-rate-95pct-upper-bound a)]
        (str "  " k ": 0 errors in " (:answers a)
             " -> true error rate is only bounded BELOW "
             (round (* 100 (:error-rate-95pct-upper-bound a)) 2)
             "% (95% upper bound, rule of three). Zero observed is not zero."))
      [""
       "Error rate by characters separating the target from its nearest sibling:"]
      (for [[k a] (:arms s)
            :when (seq (:by-nearest-distance a))]
        (str "  " k ": "
             (str/join "  " (for [[d v] (:by-nearest-distance a)]
                              (str "d=" d " " (:errors v) "/" (:n v))))))
      [""]
      (if-let [ms (seq (mapcat :misses (vals (:arms s))))]
        (concat ["FAILURE CASES, verbatim:"]
                (for [m ms]
                  (str "  [" (:arm m) " " (:tag m) "] " (name (:kind m))
                       "\n    expected: " (pr-str (:expected m))
                       "\n    got:      " (pr-str (:got m)))))
        ["FAILURE CASES: none. Every answer in every arm was byte-exact."])
      [""
       (str "DESIGN QUESTION: " (get-in s [:design-question :verdict]))
       ""
       "Confounds:"]
      (map #(str "  - " %) (:confounds s))))))

(defn -main [& args]
  (let [dir (first args)
        md? (some #{"--markdown"} args)]
    (when-not dir
      (println "usage: bb bench/score_transcription_fidelity.clj RESULT_DIR [--markdown]")
      (System/exit 2))
    (let [s (score (load-run dir))]
      (if md? (println (markdown s)) (println (json/generate-string s {:pretty true}))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
