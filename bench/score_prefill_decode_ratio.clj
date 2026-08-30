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

(defn load-run [dir]
  (let [meta (json/parse-string (slurp (str (fs/path dir "meta.json"))) true)
        trials (->> (fs/glob (fs/path dir "trials") "*.timing.json")
                    (map parse-trial)
                    (sort-by :tag)
                    vec)]
    {:meta meta :trials trials}))

;; -------------------------------------------------------------------- summary

(defn summarize-condition [trials cond-key]
  (let [ts (filter #(and (= cond-key (:condition %)) (:ok %)) trials)
        walls (map :wall_ms ts)]
    {:condition cond-key
     :n (count ts)
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
      "Confounds this measurement cannot separate:"
      (str/join "\n" (map #(str "  - " %) (:confounds s)))])))

(def tsv-columns
  [:tag :condition :replicate :wall_ms :exit_code :prompt_bytes :prompt_sha256_16
   :input-tokens :cached-input-tokens :output-tokens :reasoning-output-tokens
   :decoded-tokens :turn-started-ms :message-chars :loadavg])

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
