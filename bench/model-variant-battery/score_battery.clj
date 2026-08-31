#!/usr/bin/env bb

(ns score-battery
  (:require
   [babashka.fs :as fs]
   [cheshire.core :as json]
   [clojure.edn :as edn]
   [clojure.string :as str]))

(def eof (Object.))

(defn median [xs]
  (when (seq xs)
    (let [values (vec (sort xs))
          n (count values)
          middle (quot n 2)]
      (double
        (if (odd? n)
          (nth values middle)
          (/ (+ (nth values (dec middle)) (nth values middle)) 2.0))))))

(defn mad [xs]
  (when-let [center (median xs)]
    (median (map #(Math/abs (- (double %) center)) xs))))

(defn rounded [value digits]
  (when (some? value)
    (let [factor (Math/pow 10 digits)]
      (/ (Math/round (* (double value) factor)) factor))))

(defn sha256 [text]
  (let [digest (java.security.MessageDigest/getInstance "SHA-256")]
    (.update digest (.getBytes (str text) "UTF-8"))
    (format "%064x" (java.math.BigInteger. 1 (.digest digest)))))

(defn parse-jsonl [file]
  (->> (str/split-lines (slurp (str file)))
       (remove str/blank?)
       (keep (fn [line]
               (let [[arrival payload] (str/split line #"\t" 2)]
                 (when payload
                   (try
                     {:arrival-ns (parse-long arrival)
                      :event (json/parse-string payload true)}
                     (catch Exception _ nil))))))))

(defn sequence-oracle [message]
  (let [normalized (str/trim (or message ""))
        allowed? (boolean (re-matches #"[0-9]+(?:[\s,]+[0-9]+)*[\s,]*" normalized))
        values (when allowed? (mapv parse-long (re-seq #"[0-9]+" normalized)))]
    {:pass (= (vec (range 1 201)) values)
     :normalized-count (count values)
     :last-integer (last values)}))

(defn control-oracle [message]
  {:pass (= "ok" (-> (or message "") str/trim str/lower-case))})

(defn parse-trial [timing-file]
  (let [timing (json/parse-string (slurp (str timing-file)) true)
        jsonl (str/replace (str timing-file) #"\.timing\.json$" ".jsonl")
        stderr-file (str/replace (str timing-file) #"\.timing\.json$" ".stderr")
        entries (parse-jsonl jsonl)
        events (map :event entries)
        completed (some #(when (= "turn.completed" (:type %)) %) events)
        started (some #(when (= "turn.started" (get-in % [:event :type])) %) entries)
        messages (keep #(when (= "agent_message" (get-in % [:item :type]))
                          (get-in % [:item :text]))
                       events)
        message (last messages)
        usage (:usage completed)
        condition (:condition timing)
        oracle (case condition
                 "B" (sequence-oracle message)
                 "C" (control-oracle message)
                 {:pass false})
        stderr (if (fs/exists? stderr-file) (slurp stderr-file) "")
        rerouted? (boolean (some #(or (= "model.rerouted" (:type %))
                                      (= "model/rerouted" (:type %)))
                                 events))
        meter? (boolean (re-find #"(?i)usage limit|rate limit|quota" (str stderr "\n" message)))
        decoded (when usage
                  (+ (or (:output_tokens usage) 0)
                     (or (:reasoning_output_tokens usage) 0)))
        ok? (and (zero? (:exit_code timing))
                 (some? usage)
                 (:pass oracle)
                 (not rerouted?))]
    (merge timing
           {:message-sha256 (when message (sha256 message))
            :message-chars (count (or message ""))
            :input-tokens (:input_tokens usage)
            :cached-input-tokens (:cached_input_tokens usage)
            :output-tokens (:output_tokens usage)
            :reasoning-output-tokens (:reasoning_output_tokens usage)
            :decoded-tokens decoded
            :turn-started-ms (when started
                               (/ (- (:arrival-ns started) (:start_ns timing)) 1e6))
            :oracle oracle
            :rerouted rerouted?
            :meter-fact meter?
            :valid-for-rate ok?})))

(defn condition-summary [trials condition]
  (let [all (filter #(= condition (:condition %)) trials)
        valid (filter :valid-for-rate all)
        walls (map :wall_ms valid)
        decoded (map :decoded-tokens valid)]
    {:condition condition
     :n-valid (count valid)
     :n-total (count all)
     :oracle-passes (count (filter #(get-in % [:oracle :pass]) all))
     :meter-facts (count (filter :meter-fact all))
     :wall-ms-median (rounded (median walls) 1)
     :wall-ms-min (rounded (first (sort walls)) 1)
     :wall-ms-max (rounded (last (sort walls)) 1)
     :wall-ms-mad (rounded (mad walls) 1)
     :decoded-tokens-median (rounded (median decoded) 1)
     :turn-started-ms-median (rounded (median (keep :turn-started-ms valid)) 1)}))

(defn decode-score [decode-dir]
  (let [meta (json/parse-string (slurp (str (fs/path decode-dir "meta.json"))) true)
        timing-files (sort (fs/glob (fs/path decode-dir "trials") "*.timing.json"))
        trials (mapv parse-trial timing-files)
        b (condition-summary trials "B")
        c (condition-summary trials "C")
        b-wall (:wall-ms-median b)
        c-wall (:wall-ms-median c)
        wall-delta (when (and b-wall c-wall) (- b-wall c-wall))
        token-delta (when (and (:decoded-tokens-median b) (:decoded-tokens-median c))
                      (- (:decoded-tokens-median b) (:decoded-tokens-median c)))
        resolved? (boolean
                    (and wall-delta
                         (pos? wall-delta)
                         (> wall-delta
                            (* 2.0 (+ (or (:wall-ms-mad b) 0)
                                      (or (:wall-ms-mad c) 0))))))
        e2e-rate (when (and b-wall (pos? b-wall) (:decoded-tokens-median b))
                   (/ (:decoded-tokens-median b) (/ b-wall 1000.0)))
        subtracted-rate (when (and wall-delta (pos? wall-delta) token-delta)
                          (/ token-delta (/ wall-delta 1000.0)))]
    {:meta meta
     :conditions {:B b :C c}
     :e2e-tokens-per-second (rounded e2e-rate 1)
     :bootstrap-subtracted
     {:wall-delta-ms (rounded wall-delta 1)
      :decoded-token-delta (rounded token-delta 1)
      :tokens-per-second (rounded subtracted-rate 1)
      :resolved resolved?
      :method "median(B decoded - C decoded) / median(B wall - C wall)"}
     :trials trials}))

(defn read-one-form [text]
  (when (string? text)
    (try
      (binding [*read-eval* false]
        (with-open [reader (java.io.PushbackReader. (java.io.StringReader. text))]
          (let [form (read reader false eof)
                trailing (read reader false eof)]
            (when (and (not (identical? eof form))
                       (identical? eof trailing))
              form))))
      (catch Exception _ nil))))

(defn top-level-owner [form]
  (when (and (seq? form)
             (#{'def 'defn 'defmacro 'defonce} (first form)))
    (let [candidate (second form)]
      (cond
        (symbol? candidate) candidate
        (and (map? candidate) (symbol? (nth form 2 nil))) (nth form 2)
        :else nil))))

(defn receipt-timing-ms [receipt]
  (or (:timing_ms receipt)
      (into {}
            (map (fn [[key seconds]] [key (* 1000.0 seconds)]))
            (:timing_s receipt))))

(defn path-string [path]
  (if (keyword? path)
    (subs (str path) 1)
    (str path)))

(defn fill-trial [case receipt-file]
  (let [receipt (json/parse-string (slurp (str receipt-file)) true)
        replacement (:replacement receipt)
        actual (read-one-form replacement)
        expected (read-one-form (:expected case))
        requested (symbol (get-in receipt [:intent :owner]))
        owner (top-level-owner actual)
        intended-file (get-in receipt [:intent :file])
        paths (or (seq (map (comp path-string :path) (:action_file_delta receipt)))
                  (seq (map path-string (keys (get-in receipt [:guarded_edit :read_back_hashes]))))
                  [])
        wrong-path? (boolean (some #(not= intended-file %) paths))
        wrong-subject? (or (and actual (not= requested owner)) wrong-path?)
        schema-fumble? (and (nil? replacement)
                            (boolean (re-find #"(?i)Spark garbage" (or (:detail receipt) ""))))
        parse-fumble? (and (string? replacement) (nil? actual))
        outcome (or (:outcome receipt)
                    (if (:committed receipt) "applied" "refused"))
        exact? (and actual
                    (= requested owner)
                    (= expected actual)
                    (not wrong-subject?))]
    {:id (name (:id case))
     :owner (str requested)
     :outcome outcome
     :exact (boolean exact?)
     :one-shot (= "applied" outcome)
     :wrong-subject (boolean wrong-subject?)
     :schema-fumble (boolean schema-fumble?)
     :parse-fumble (boolean parse-fumble?)
     :replacement-sha256 (when replacement (sha256 replacement))
     :timing-ms (receipt-timing-ms receipt)
     :provider-cost-usd (:provider_cost_usd receipt)
     :usage (:usage receipt)
     :action-paths (vec paths)}))

(defn fill-score [root model-dir]
  (let [manifest (edn/read-string (slurp (str (fs/path root "fill-cases.edn"))))
        cases-by-owner (into {} (map (juxt (comp str :owner) identity)) (:cases manifest))
        receipt-files (sort (fs/glob (fs/path model-dir "fill" "receipts") "*.json"))
        trials (mapv (fn [receipt-file]
                       (let [receipt (json/parse-string (slurp (str receipt-file)) true)
                             owner (get-in receipt [:intent :owner])]
                         (fill-trial (or (get cases-by-owner owner)
                                         (throw (ex-info "Receipt owner absent from manifest"
                                                         {:owner owner :receipt (str receipt-file)})))
                                     receipt-file)))
                     receipt-files)
        model-walls (keep #(or (get-in % [:timing-ms :spark])
                               (get-in % [:timing-ms :elaborate])) trials)
        read-walls (keep #(or (get-in % [:timing-ms :source])
                              (get-in % [:timing-ms :read])) trials)
        apply-walls (keep #(get-in % [:timing-ms :apply]) trials)
        total-walls (keep #(get-in % [:timing-ms :total]) trials)
        costs (keep :provider-cost-usd trials)]
    {:n-total (count trials)
     :exact (count (filter :exact trials))
     :one-shot (count (filter :one-shot trials))
     :wrong-subject (count (filter :wrong-subject trials))
     :schema-fumbles (count (filter :schema-fumble trials))
     :parse-fumbles (count (filter :parse-fumble trials))
     :read-wall-ms-median (rounded (median read-walls) 1)
     :model-wall-ms-median (rounded (median model-walls) 1)
     :apply-wall-ms-median (rounded (median apply-walls) 1)
     :total-wall-ms-median (rounded (median total-walls) 1)
     :provider-cost-usd-total (when (seq costs) (reduce + costs))
     :prompt-tokens-total (reduce + 0 (keep #(get-in % [:usage :prompt_tokens]) trials))
     :completion-tokens-total (reduce + 0 (keep #(get-in % [:usage :completion_tokens]) trials))
     :total-tokens (reduce + 0 (keep #(get-in % [:usage :total_tokens]) trials))
     :trials trials}))

(defn score-fills-only [root model-dir]
  (let [receipt-file (first (sort (fs/glob (fs/path model-dir "fill" "receipts") "*.json")))
        receipt (json/parse-string (slurp (str receipt-file)) true)]
    {:schema "clj-surgeon.model-variant-battery-score/v1"
     :model (or (:model_requested receipt) (str (fs/file-name model-dir)))
     :fill (fill-score root model-dir)}))

(defn score-model [root model-dir]
  (let [decode (decode-score (fs/path model-dir "decode" "raw"))]
    {:schema "clj-surgeon.model-variant-battery-score/v1"
     :model (get-in decode [:meta :model])
     :decode decode
     :fill (fill-score root model-dir)}))

(defn markdown [score]
  (let [b (get-in score [:decode :conditions :B])
        c (get-in score [:decode :conditions :C])
        sub (get-in score [:decode :bootstrap-subtracted])
        fill (:fill score)]
    (str/join
      "\n"
      [(str "# " (:model score))
       ""
       "| Decode condition | Oracle valid | Median wall | MAD | Median decoded |"
       "|---|---:|---:|---:|---:|"
       (str "| B: 1..200 | " (:n-valid b) "/" (:n-total b) " | "
            (or (:wall-ms-median b) "n/a") " ms | " (or (:wall-ms-mad b) "n/a")
            " ms | " (or (:decoded-tokens-median b) "n/a") " |")
       (str "| C: ok | " (:n-valid c) "/" (:n-total c) " | "
            (or (:wall-ms-median c) "n/a") " ms | " (or (:wall-ms-mad c) "n/a")
            " ms | " (or (:decoded-tokens-median c) "n/a") " |")
       ""
       (str "E2E decode rate: " (or (get-in score [:decode :e2e-tokens-per-second]) "n/a") " tok/s")
       (str "Bootstrap-subtracted decode rate: " (or (:tokens-per-second sub) "n/a")
            " tok/s (resolved=" (:resolved sub) ")")
       ""
       "| Fill | Exact | One-shot | Wrong subject | Schema fumbles | Parse fumbles | Median model wall | Median bang wall |"
       "|---|---:|---:|---:|---:|---:|---:|---:|"
       (str "| result | " (:exact fill) "/" (:n-total fill) " | " (:one-shot fill)
            "/" (:n-total fill) " | " (:wrong-subject fill) " | " (:schema-fumbles fill)
            " | " (:parse-fumbles fill) " | " (:model-wall-ms-median fill) " ms | "
            (:total-wall-ms-median fill) " ms |")
       ""
       "Per-bang timing and normalized oracle decisions are in `score.json`."])))

(defn -main [& args]
  (let [[model-dir option output-file] args]
    (when-not model-dir
      (binding [*out* *err*]
        (println "usage: score_battery.clj MODEL_RESULT_DIR [--markdown|--fills-only] [OUTPUT]"))
      (System/exit 2))
    (let [model-path (fs/absolutize model-dir)
          root (-> model-path fs/parent fs/parent)
          score (if (= "--fills-only" option)
                  (score-fills-only root model-path)
                  (score-model root model-path))
          rendered (if (= "--markdown" option)
                     (markdown score)
                     (json/generate-string score {:pretty true}))]
      (if output-file
        (spit output-file (str rendered "\n"))
        (println rendered)))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
