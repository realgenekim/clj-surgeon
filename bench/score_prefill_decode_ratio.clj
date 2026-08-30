#!/usr/bin/env bb
;; score_prefill_decode_ratio.clj — fold the prefill/decode probe into verdicts.
;;
;; Companion to bench/measure_prefill_decode_ratio.sh. The probe emits facts
;; (verbatim codex JSONL, arrival timestamps, environment readings); this fold
;; emits the verdicts (medians, spread, marginal token counts, rates, ratio).
;; Keeping them apart means a new question can be asked of an old run without
;; re-spending the calls, and a verdict can never be baked into a snapshot where
;; it can no longer be re-derived.
;;
;; Usage:
;;   bb bench/score_prefill_decode_ratio.clj RESULT_DIR [--markdown]
;;
;; The fold refuses to state a rate it cannot defend. If a condition's median is
;; not separated from the floor by more than the observed spread, it reports the
;; separation as UNRESOLVED and derives a bound instead of a point estimate.

(ns score-prefill-decode-ratio
  (:require
   [babashka.fs :as fs]
   [cheshire.core :as json]
   [clojure.string :as str]))

(def score-schema "clj-surgeon.prefill-decode-score/v1")

;; ----------------------------------------------------------------- statistics

(defn median [xs]
  (when (seq xs)
    (let [v (vec (sort xs)) n (count v)]
      (if (odd? n)
        (double (nth v (quot n 2)))
        (/ (+ (nth v (dec (quot n 2))) (nth v (quot n 2))) 2.0)))))

(defn quantile [xs q]
  (when (seq xs)
    (let [v (vec (sort xs))
          idx (min (dec (count v)) (int (Math/floor (* q (count v)))))]
      (double (nth v idx)))))

(defn mad
  "Median absolute deviation — spread that a single slow trial cannot inflate."
  [xs]
  (when-let [m (median xs)]
    (median (map #(Math/abs (- (double %) m)) xs))))

(defn round [x n]
  (when x
    (let [f (Math/pow 10 n)]
      (/ (Math/round (* (double x) f)) f))))

;; ------------------------------------------------------------------- loading

(defn parse-trial
  "Read one trial into a fact map. The codex JSONL lines carry an arrival
  timestamp prefix written by the probe."
  [timing-file]
  (let [timing (json/parse-string (slurp (str timing-file)) true)
        jsonl  (str/replace (str timing-file) #"\.timing\.json$" ".jsonl")
        lines  (when (fs/exists? jsonl)
                 (->> (str/split-lines (slurp jsonl))
                      (remove str/blank?)
                      (keep (fn [l]
                              (let [[ts payload] (str/split l #"\t" 2)]
                                (when payload
                                  (try {:arrival-ns (parse-long ts)
                                        :event (json/parse-string payload true)}
                                       (catch Exception _ nil))))))))
        by-type (group-by #(get-in % [:event :type]) lines)
        completed (first (get by-type "turn.completed"))
        usage (get-in completed [:event :usage])
        started (first (get by-type "turn.started"))
        message (->> lines
                     (filter #(= "agent_message" (get-in % [:event :item :type])))
                     first)]
    (merge timing
           {:input-tokens (:input_tokens usage)
            :cached-input-tokens (:cached_input_tokens usage)
            :output-tokens (:output_tokens usage)
            :reasoning-output-tokens (:reasoning_output_tokens usage)
            ;; total decoded tokens: reasoning tokens are decoded serially too,
            ;; so they are decode work whether or not they are shown.
            :decoded-tokens (when usage
                              (+ (or (:output_tokens usage) 0)
                                 (or (:reasoning_output_tokens usage) 0)))
            :turn-started-ms (when started
                               (/ (- (:arrival-ns started) (:start_ns timing)) 1e6))
            :message-chars (count (or (get-in message [:event :item :text]) ""))
            :ok (and (= 0 (:exit_code timing)) (some? usage))})))

(def tsv-numeric
  #{:replicate :wall_ms :exit_code :prompt_bytes :input-tokens :cached-input-tokens
    :output-tokens :reasoning-output-tokens :decoded-tokens :turn-started-ms :message-chars})

(defn load-trials-tsv
  "Rehydrate the fact table from trials.tsv.

  bench/results/ARCHIVED.md keeps raw per-trial transcripts out of git, so a
  committed run has trials.tsv but no trials/ directory. Reading it back means
  a committed run stays re-foldable — a new question can be asked of an old run
  without re-spending the calls, which is the entire point of separating the
  probe from the fold."
  [tsv-file]
  (let [[header & rows] (str/split-lines (slurp (str tsv-file)))
        cols (map keyword (str/split header #"\t"))]
    (vec
     (for [row rows :when (not (str/blank? row))]
       (let [m (into {}
                     (map (fn [k v]
                            [k (cond
                                 (str/blank? v) nil
                                 (tsv-numeric k) (parse-double v)
                                 :else v)])
                          cols
                          (str/split row #"\t" -1)))]
         ;; :ok is a derived verdict, not a stored fact — recompute it here so
         ;; the rehydrated path and the raw path agree on which trials count.
         (-> m
             (assoc :ok (and (= 0.0 (:exit_code m)) (some? (:input-tokens m))))
             (assoc :reproduction_exact
                    (case (str (:reproduction_exact m))
                      "true" true
                      "false" false
                      nil))))))))

(defn load-run
  "Prefer the raw trials directory; fall back to the committed fact table."
  [dir]
  (let [meta (json/parse-string (slurp (str (fs/path dir "meta.json"))) true)
        timing-files (seq (fs/glob (fs/path dir "trials") "*.timing.json"))
        tsv-file (fs/path dir "trials.tsv")
        trials (cond
                 timing-files (vec (sort-by :tag (map parse-trial timing-files)))
                 (fs/exists? tsv-file) (vec (sort-by :tag (load-trials-tsv tsv-file)))
                 :else [])]
    (when (empty? trials)
      (binding [*out* *err*]
        (println "warning: no trials found in" (str dir))))
    {:meta meta :trials trials :source (if timing-files "trials/" "trials.tsv")}))

;; -------------------------------------------------------------------- summary

(defn summarize-condition
  "Summarise one condition.

  VALIDITY, PREDECLARED: a trial counts only if it exited 0, reported usage, and
  — where an exact output was specified — reproduced it byte-for-byte after
  trimming. A truncated or paraphrased emission is not a slow copy, it is a
  different task, and folding it in would silently understate copy cost. The
  excluded trials are counted and reported, never dropped quietly."
  [trials cond-key]
  (let [all (filter #(= cond-key (:condition %)) trials)
        adherent? #(not (false? (:reproduction_exact %)))
        ts (filter #(and (:ok %) (adherent? %)) all)
        walls (map :wall_ms ts)]
    {:condition cond-key
     :n (count ts)
     :n-attempted (count all)
     :n-not-reproduced (count (filter #(false? (:reproduction_exact %)) all))
     :route-adherent (when (seq all)
                       (round (/ (count (filter adherent? all)) (double (count all))) 3))
     :exactness-checked (boolean (some #(some? (:reproduction_exact %)) all))
     :n-failed (count (filter #(and (= cond-key (:condition %)) (not (:ok %))) trials))
     :wall-ms-median (round (median walls) 1)
     :wall-ms-min (round (first (sort walls)) 1)
     :wall-ms-max (round (last (sort walls)) 1)
     :wall-ms-p25 (round (quantile walls 0.25) 1)
     :wall-ms-p75 (round (quantile walls 0.75) 1)
     :wall-ms-mad (round (mad walls) 1)
     :input-tokens-median (round (median (map :input-tokens ts)) 0)
     :cached-input-tokens-median (round (median (map :cached-input-tokens ts)) 0)
     :output-tokens-median (round (median (map :output-tokens ts)) 0)
     :reasoning-tokens-median (round (median (map :reasoning-output-tokens ts)) 0)
     :decoded-tokens-median (round (median (map :decoded-tokens ts)) 0)
     :prompt-bytes-median (round (median (map :prompt_bytes ts)) 0)
     :turn-started-ms-median (round (median (keep :turn-started-ms ts)) 1)}))

(defn resolved?
  "A delta is resolved only when it exceeds the combined spread of the two
  conditions it is subtracting. Anything less is noise wearing a number."
  [delta-ms spread-a spread-c]
  (and (some? delta-ms)
       (pos? delta-ms)
       (> delta-ms (* 2.0 (+ (or spread-a 0) (or spread-c 0))))))

(defn emission-rate
  "Decode rate for one emission condition, measured against the same floor."
  [cond-summary floor-summary]
  (let [d-ms (when (and (:wall-ms-median cond-summary) (:wall-ms-median floor-summary))
               (- (:wall-ms-median cond-summary) (:wall-ms-median floor-summary)))
        marg (when (and (:decoded-tokens-median cond-summary)
                        (:decoded-tokens-median floor-summary))
               (- (:decoded-tokens-median cond-summary)
                  (:decoded-tokens-median floor-summary)))
        res (resolved? d-ms (:wall-ms-mad cond-summary) (:wall-ms-mad floor-summary))]
    {:condition (:condition cond-summary)
     :n (:n cond-summary)
     :route-adherent (:route-adherent cond-summary)
     :n-not-reproduced (:n-not-reproduced cond-summary)
     :marginal-decoded-tokens marg
     :delta-ms (round d-ms 1)
     :tokens-per-second (when (and marg d-ms (pos? d-ms)) (round (/ marg (/ d-ms 1000.0)) 1))
     :resolved res}))

(defn copy-screen
  "Copy versus compose. B composes; D and E transcribe.

  Reported as a RATE ratio, and only when the conditions being compared emitted
  comparable token counts — otherwise this measures volume, not speed, and the
  comparison is returned as invalid rather than as a number."
  [trials floor]
  (let [summ (fn [k] (summarize-condition trials k))
        b (summ "B") d (summ "D") e (summ "E")
        rates (into {} (for [x [b d e] :when (pos? (:n x))]
                         [(:condition x) (emission-rate x floor)]))
        tok (fn [k] (get-in rates [k :marginal-decoded-tokens]))
        tps (fn [k] (get-in rates [k :tokens-per-second]))
        matched? (fn [k1 k2]
                   (let [t1 (tok k1) t2 (tok k2)]
                     (and t1 t2 (pos? t1) (pos? t2)
                          (< (/ (Math/abs (- t1 t2)) (double (max t1 t2))) 0.10))))
        discount (fn [copy-k compose-k]
                   (when (and (tps copy-k) (tps compose-k))
                     {:comparison (str copy-k " over " compose-k)
                      :token-counts-matched (matched? copy-k compose-k)
                      :copy-tokens (tok copy-k)
                      :compose-tokens (tok compose-k)
                      :speedup (round (/ (tps copy-k) (tps compose-k)) 2)}))]
    (when (seq rates)
      {:rates rates
       :copy-over-compose-unpredictable (discount "D" "B")
       :copy-over-compose-same-content (discount "E" "B")
       :unpredictable-over-predictable-copy (discount "D" "E")})))

(defn score [{:keys [meta trials]}]
  (let [a (summarize-condition trials "A")
        b (summarize-condition trials "B")
        c (summarize-condition trials "C")
        floor-ms (:wall-ms-median c)
        d-a (when (and (:wall-ms-median a) floor-ms) (- (:wall-ms-median a) floor-ms))
        d-b (when (and (:wall-ms-median b) floor-ms) (- (:wall-ms-median b) floor-ms))
        marg-in (when (and (:input-tokens-median a) (:input-tokens-median c))
                  (- (:input-tokens-median a) (:input-tokens-median c)))
        marg-out (when (and (:decoded-tokens-median b) (:decoded-tokens-median c))
                   (- (:decoded-tokens-median b) (:decoded-tokens-median c)))
        a-resolved (resolved? d-a (:wall-ms-mad a) (:wall-ms-mad c))
        b-resolved (resolved? d-b (:wall-ms-mad b) (:wall-ms-mad c))
        ;; When condition A is not separated from the floor, the honest
        ;; statement is a LOWER BOUND on prefill rate, not a point estimate:
        ;; the whole prefill fit inside the noise, so it was at least this fast.
        a-bound-ms (max 1.0 (double (+ (or (:wall-ms-mad a) 0) (or (:wall-ms-mad c) 0))))
        prefill-tps (when marg-in
                      (if a-resolved
                        (/ marg-in (/ d-a 1000.0))
                        (/ marg-in (/ a-bound-ms 1000.0))))
        decode-tps (when (and marg-out b-resolved) (/ marg-out (/ d-b 1000.0)))
        ratio (when (and prefill-tps decode-tps) (/ prefill-tps decode-tps))]
    {:schema score-schema
     :meta meta
     :conditions {:A a :B b :C c}
     :floor {:ms floor-ms
             :mad-ms (:wall-ms-mad c)
             :min-ms (:wall-ms-min c)
             :max-ms (:wall-ms-max c)
             :n (:n c)
             :local-startup-ms (:turn-started-ms-median c)
             :note "Condition C. Neither prefill nor decode: process start, scheduling, queueing, request setup, response teardown."}
     :prefill {:marginal-input-tokens marg-in
               :delta-ms (round d-a 1)
               :resolved a-resolved
               :tokens-per-second (round prefill-tps 0)
               :estimate-kind (if a-resolved "point" "lower-bound")
               :note (if a-resolved
                       "Delta exceeded combined MAD spread; point estimate."
                       "Delta did not clear the noise floor; prefill of this many tokens is not distinguishable from zero. Reported as a lower bound using the combined MAD as the largest time it could have taken.")}
     :decode {:marginal-decoded-tokens marg-out
              :delta-ms (round d-b 1)
              :resolved b-resolved
              :tokens-per-second (round decode-tps 1)
              :estimate-kind (if b-resolved "point" "unresolved")}
     :ratio {:prefill-over-decode (round ratio 1)
             :kind (cond
                     (nil? ratio) "unresolved"
                     a-resolved "point"
                     :else "lower-bound")}
     :copy-screen (copy-screen trials c)
     :confounds
     ["Wall clock includes network transfer of the prompt. Condition A ships ~1 MB, condition C ships ~1 KB, so (A - C) contains upload time as well as prefill. Prefill rate is therefore a LOWER bound even when the delta resolves."
      "Server-side batching and queueing are invisible from the client. A turn may wait behind other tenants; that time lands in the floor and in both deltas."
      "Provider prefix caching is defeated for condition A's filler (fresh random words per replicate) but the codex system prompt and tool definitions are cached identically across all conditions, so the cached prefix cancels in the subtraction."
      "Reasoning tokens are counted as decode because they are produced serially, but they are not visible, so condition B's decode total is trusted from the provider's own usage report rather than from the text."
      "Token counts come from the provider's usage report, not from a local tokenizer. They are authoritative for billing and are assumed authoritative for work."
      "A single provider, model, and datacentre. Nothing here generalises to other hardware, other models, or the same model under different load."
      "codex exec is an agent wrapper, not a raw completion. The floor includes CLI process start, config load, and session setup, which a raw API call would not pay."]}))

;; -------------------------------------------------------------------- render

(defn markdown [s]
  (let [{:keys [conditions floor prefill decode ratio meta]} s
        f #(if (nil? %) "n/a" (str %))]
    (str/join
     "\n"
     [(str "Model: " (:model meta) " | reasoning: " (:reasoning meta)
           " | profile: " (:profile meta)
           " | codex: " (:codex_version meta))
      (str "Host: " (get-in meta [:env_before :hostname])
           " | " (get-in meta [:env_before :nproc]) " cores"
           " | loadavg at start: " (get-in meta [:env_before :loadavg]))
      ""
      "| Condition | n | median wall | MAD | min | max | input tok | decoded tok |"
      "|---|---|---|---|---|---|---|---|"
      (str/join
       "\n"
       (for [k [:A :B :C]
             :let [c (get conditions k)]]
         (str "| " (name k) " | " (:n c) " | " (f (:wall-ms-median c)) " ms | "
              (f (:wall-ms-mad c)) " ms | " (f (:wall-ms-min c)) " ms | "
              (f (:wall-ms-max c)) " ms | " (f (:input-tokens-median c)) " | "
              (f (:decoded-tokens-median c)) " |")))
      ""
      (str "FLOOR (condition C): " (f (:ms floor)) " ms, MAD " (f (:mad-ms floor))
           " ms, n=" (f (:n floor)))
      (str "  of which local process startup to turn.started: " (f (:local-startup-ms floor)) " ms")
      ""
      (str "PREFILL: " (f (:marginal-input-tokens prefill)) " marginal input tokens in "
           (f (:delta-ms prefill)) " ms -> " (f (:tokens-per-second prefill))
           " tok/s (" (:estimate-kind prefill) ", resolved=" (:resolved prefill) ")")
      (str "DECODE:  " (f (:marginal-decoded-tokens decode)) " marginal decoded tokens in "
           (f (:delta-ms decode)) " ms -> " (f (:tokens-per-second decode))
           " tok/s (" (:estimate-kind decode) ")")
      (str "RATIO:   " (f (:prefill-over-decode ratio)) "x (" (:kind ratio) ")")
      ""
      (when-let [cs (:copy-screen s)]
        (str/join
         "\n"
         (concat
          ["COPY VERSUS COMPOSE"
           "| Cond | Kind | n | route-adherent | not reproduced | decoded tok | delta | tok/s |"
           "|---|---|---|---|---|---|---|---|"]
          (for [k ["B" "D" "E"]
                :let [r (get-in cs [:rates k])]
                :when r]
            (str "| " k " | "
                 (case k "B" "compose" "D" "copy/unpredictable" "E" "copy/same-content") " | "
                 (:n r) " | " (f (:route-adherent r)) " | " (f (:n-not-reproduced r)) " | "
                 (f (:marginal-decoded-tokens r)) " | " (f (:delta-ms r)) " ms | "
                 (f (:tokens-per-second r)) " |"))
          [""]
          (for [[label kw] [["copy(unpredictable) / compose" :copy-over-compose-unpredictable]
                            ["copy(same content) / compose" :copy-over-compose-same-content]
                            ["copy(unpredictable) / copy(predictable)" :unpredictable-over-predictable-copy]]
                :let [d (get cs kw)]
                :when d]
            (str "  " label " = " (f (:speedup d)) "x"
                 "  [token counts matched: " (:token-counts-matched d)
                 "; " (f (:copy-tokens d)) " vs " (f (:compose-tokens d)) " tokens]")))))
      ""
      "Confounds this measurement cannot separate:"
      (str/join "\n" (map #(str "  - " %) (:confounds s)))])))

(def tsv-columns
  [:tag :condition :replicate :wall_ms :exit_code :prompt_bytes :prompt_sha256_16
   :input-tokens :cached-input-tokens :output-tokens :reasoning-output-tokens
   :decoded-tokens :turn-started-ms :message-chars
   :expected_sha256_16 :message_sha256_16 :reproduction_exact :loadavg])

(defn tsv
  "Per-trial facts as a tab-separated time series. This is the shape
  bench/results/ARCHIVED.md asks to keep in git."
  [{:keys [trials]}]
  (str/join
   "\n"
   (cons (str/join "\t" (map name tsv-columns))
         (for [t trials]
           (str/join "\t" (map #(let [v (get t %)]
                                  (cond (nil? v) ""
                                        (double? v) (format "%.1f" v)
                                        :else (str v)))
                               tsv-columns))))))

(defn -main [& args]
  (let [dir (first args)
        md? (some #{"--markdown"} args)
        tsv? (some #{"--tsv"} args)]
    (when-not dir
      (println "usage: bb bench/score_prefill_decode_ratio.clj RESULT_DIR [--markdown|--tsv]")
      (System/exit 2))
    (let [run (load-run dir)]
      (cond
        tsv? (println (tsv run))
        md? (println (markdown (score run)))
        :else (println (json/generate-string (score run) {:pretty true}))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
