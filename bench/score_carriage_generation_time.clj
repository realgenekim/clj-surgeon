#!/usr/bin/env bb
;; score_carriage_generation_time.clj — fold X5-T into the two ratios that decide.
;;
;; Companion to bench/measure_carriage_generation_time.clj.
;;
;; The whole point is to report TWO ratios side by side and never collapse them:
;;   token count ratio    (EDN/JSON)  -- what a static tokenizer sees
;;   generation time ratio (EDN/JSON) -- what the model actually costs
;; A token-count kill is not a generation-time kill. If these disagree, emission
;; has been priced by a proxy and that is the finding.
;;
;; Usage: bb bench/score_carriage_generation_time.clj RESULT_DIR [--markdown]

(ns score-carriage-generation-time
  (:require [cheshire.core :as json] [clojure.string :as str]))

(def score-schema "clj-surgeon.carriage-generation-time-score/v1")

(defn round [x n] (when x (let [f (Math/pow 10 n)] (/ (Math/round (* (double x) f)) f))))

(defn median [xs]
  (when (seq xs)
    (let [v (vec (sort xs)) n (count v)]
      (if (odd? n) (double (nth v (quot n 2)))
          (/ (+ (nth v (dec (quot n 2))) (nth v (quot n 2))) 2.0)))))

(defn mad [xs]
  (when-let [m (median xs)] (median (map #(Math/abs (- (double %) m)) xs))))

(defn load-run [dir]
  {:meta (json/parse-string (slurp (str dir "/meta.json")) true)
   :facts (->> (str/split-lines (slurp (str dir "/facts.jsonl")))
               (remove str/blank?) (mapv #(json/parse-string % true)))})

(defn arm-summary [facts arm]
  (let [all (filter #(= arm (:arm %)) facts)
        ;; Timing samples require a valid turn. Payload equality is recorded but
        ;; does NOT gate the sample: a corrupted payload still took the time it took.
        ts (filter #(and (:ok %) (:parse-ok %)) all)
        walls (map :wall-ms ts)]
    {:arm arm :n (count ts) :n-attempted (count all)
     :parse-failures (count (remove :parse-ok all))
     :payload-exact (count (filter :payload-exact ts))
     :wall-median (round (median walls) 1)
     :wall-mad (round (mad walls) 1)
     :wall-min (round (first (sort walls)) 1)
     :wall-max (round (last (sort walls)) 1)
     :decoded-tokens-median (round (median (keep :decoded-tokens ts)) 1)
     :output-tokens-median (round (median (keep :output-tokens ts)) 1)
     :reasoning-tokens-median (round (median (keep :reasoning-tokens ts)) 1)
     :output-bytes-median (round (median (map :output-bytes ts)) 1)}))

(defn score [{:keys [meta facts]}]
  (let [c (arm-summary facts "C") j (arm-summary facts "J") e (arm-summary facts "E")
        floor (:wall-median c)
        decode-ms (fn [a] (when (and (:wall-median a) floor) (- (:wall-median a) floor)))
        ;; marginal over the floor, so the fixed per-turn cost is not smeared into
        ;; whichever arm happens to be longer
        marg-tok (fn [a] (when (and (:decoded-tokens-median a) (:decoded-tokens-median c))
                           (- (:decoded-tokens-median a) (:decoded-tokens-median c))))
        ms-per-tok (fn [a] (let [d (decode-ms a) t (marg-tok a)]
                             (when (and d t (pos? t)) (round (/ d t) 4))))
        resolved? (fn [a b]
                    (let [da (decode-ms a) db (decode-ms b)]
                      (and da db (> (Math/abs (- da db))
                                    (* 2.0 (+ (or (:wall-mad a) 0) (or (:wall-mad b) 0)))))))
        tok-ratio (when (and (marg-tok e) (marg-tok j) (pos? (marg-tok j)))
                    (round (/ (marg-tok e) (marg-tok j)) 4))
        time-ratio (when (and (decode-ms e) (decode-ms j) (pos? (decode-ms j)))
                     (round (/ (decode-ms e) (decode-ms j)) 4))
        rate-ratio (when (and (ms-per-tok e) (ms-per-tok j) (pos? (ms-per-tok j)))
                     (round (/ (ms-per-tok e) (ms-per-tok j)) 4))]
    {:schema score-schema
     :meta meta
     :arms {:C c :J j :E e}
     :floor {:ms floor :mad (:wall-mad c) :n (:n c)}
     :per-arm
     (into {} (for [[k a] [["J" j] ["E" e]]]
                [k {:marginal-decoded-tokens (marg-tok a)
                    :decode-ms (round (decode-ms a) 1)
                    :ms-per-token (ms-per-tok a)
                    :tokens-per-second (when (ms-per-tok a) (round (/ 1000.0 (ms-per-tok a)) 1))
                    :output-bytes-median (:output-bytes-median a)}]))
     :ratios
     {:token-count-edn-over-json tok-ratio
      :generation-time-edn-over-json time-ratio
      :ms-per-token-edn-over-json rate-ratio
      :byte-edn-over-json (when (and (:output-bytes-median e) (pos? (:output-bytes-median j)))
                            (round (/ (:output-bytes-median e) (:output-bytes-median j)) 4))
      :difference-resolved (resolved? j e)
      :note "If generation-time ratio tracks token-count ratio, counting tokens was a sound proxy for cost. If they diverge, it was not."}
     :verdict
     (cond
       (not (resolved? j e))
       (str "NOT RESOLVED AT THIS n. The J/E decode-time difference ("
            (round (Math/abs (- (or (decode-ms e) 0) (or (decode-ms j) 0))) 1)
            " ms) does not exceed the combined spread ("
            (round (* 2.0 (+ (or (:wall-mad j) 0) (or (:wall-mad e) 0))) 1)
            " ms). Report as parity NOT ESTABLISHED rather than as parity, and note that "
            "token count ratio is " tok-ratio " while time ratio is " time-ratio ".")
       (and time-ratio tok-ratio (< time-ratio 1.0) (> tok-ratio 1.0))
       "DIVERGENCE: EDN costs MORE tokens but LESS time. Token count was not a sound proxy for emission cost, and a token-count kill does not carry."
       (and time-ratio tok-ratio (>= time-ratio 1.0) (> tok-ratio 1.0))
       "AGREEMENT: EDN costs more tokens AND more time. The token-count kill stands on generation time as well."
       :else "Mixed; read the ratios directly.")
     :confounds
     ["Both arms carry byte-identical Clojure; only the carriage differs. That is the intended isolation, but it also means this measures ONE payload, not a corpus. Content-dependent effects are not sampled."
      "Time is wall clock from process launch, so it includes network and the fixed per-turn floor. The floor is subtracted using a contemporaneous C arm, but server-side queueing is invisible and lands in every arm."
      "Token counts come from the provider usage report, which is the same source the decision uses for billing."
      "Reasoning tokens are included in decoded tokens because they are produced serially and cost time, but they are not part of the carriage and add variance unrelated to format."
      "One model, one reasoning effort, one hour. A different model could tokenize EDN quite differently."
      "This does not reproduce the corpus-wide token screen; it tests whether TIME tracks COUNT on a representative write. A divergence here invalidates the proxy, it does not by itself re-price the corpus."]}))

(defn markdown [s]
  (let [f #(if (nil? %) "n/a" (str %))]
    (str/join
     "\n"
     (concat
      [(str "Model: " (get-in s [:meta :model]) " | host: " (get-in s [:meta :hostname])
            " | loadavg: " (get-in s [:meta :loadavg-start])
            " | payload: " (get-in s [:meta :payload-lines]) " lines / "
            (get-in s [:meta :payload-bytes]) " bytes of real Clojure")
       ""
       "| Arm | n | parse fails | payload exact | median wall | MAD | decoded tok | out bytes |"
       "|---|---|---|---|---|---|---|---|"]
      (for [k [:C :J :E] :let [a (get-in s [:arms k])]]
        (str "| " (name k) " | " (:n a) " | " (:parse-failures a) " | " (:payload-exact a)
             " | " (f (:wall-median a)) " ms | " (f (:wall-mad a)) " ms | "
             (f (:decoded-tokens-median a)) " | " (f (:output-bytes-median a)) " |"))
      [""
       (str "FLOOR: " (f (get-in s [:floor :ms])) " ms (MAD " (f (get-in s [:floor :mad]))
            ", n=" (f (get-in s [:floor :n])) ")")
       ""]
      (for [k ["J" "E"] :let [a (get-in s [:per-arm k])]]
        (str "  " k ": " (f (:marginal-decoded-tokens a)) " marginal tokens in "
             (f (:decode-ms a)) " ms -> " (f (:ms-per-token a)) " ms/token ("
             (f (:tokens-per-second a)) " tok/s)"))
      [""
       "THE TWO RATIOS (EDN over JSON) — never collapse these:"
       (str "  token count      : " (f (get-in s [:ratios :token-count-edn-over-json])))
       (str "  generation time  : " (f (get-in s [:ratios :generation-time-edn-over-json])))
       (str "  ms per token     : " (f (get-in s [:ratios :ms-per-token-edn-over-json])))
       (str "  output bytes     : " (f (get-in s [:ratios :byte-edn-over-json])))
       (str "  difference resolved above noise: " (f (get-in s [:ratios :difference-resolved])))
       ""
       (str "VERDICT: " (:verdict s))
       ""
       "Confounds:"]
      (map #(str "  - " %) (:confounds s))))))

(defn -main [& args]
  (let [dir (first args) md? (some #{"--markdown"} args)]
    (when-not dir
      (println "usage: bb bench/score_carriage_generation_time.clj RESULT_DIR [--markdown]")
      (System/exit 2))
    (let [s (score (load-run dir))]
      (if md? (println (markdown s)) (println (json/generate-string s {:pretty true}))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
