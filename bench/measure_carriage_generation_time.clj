#!/usr/bin/env bb
;; measure_carriage_generation_time.clj — X5-T. Time per token, JSON vs EDN carriage.
;;
;; WHY THIS EXISTS — a token-count kill is not a generation-time kill
;;   A zero-model screen killed the EDN carriage on token COUNT: writes 4.931%
;;   fewer bytes but 1.389% MORE tokens, reads 2.306% more bytes and 12.253% more
;;   tokens, same sign under cl100k. The registered token<=byte kill fired.
;;
;;   Gene's objection, verbatim: "I wonder if EDN/CLJ are activated at the same
;;   time, so maybe it washes out?"
;;
;;   The objection is sound and the screen cannot answer it. That screen counted
;;   tokens with a STATIC TOKENIZER in isolation. It measured token COUNT. It did
;;   not measure GENERATION TIME with a model whose context is already saturated
;;   with Clojure — and the payload IS Clojure. If an EDN carriage sits in a
;;   sharper, lower-entropy distribution while the model is already emitting
;;   Clojure, time per token could fall even as the count rises.
;;
;; THE DECISIVE COMPARISON, reported explicitly either way
;;   token count ratio    (EDN/JSON)  -- previously measured ~1.014 writes, ~1.12 reads
;;   generation time ratio (EDN/JSON) -- UNKNOWN; this probe measures it
;;
;;   If time tracks count, the kill stands and EDN is dead on evidence.
;;   If time is at parity or better while count is worse, the screen killed a live
;;   idea, and emission has been priced by a proxy TWICE OVER — first bytes
;;   standing in for tokens, now tokens standing in for time. That second finding
;;   would matter more than the EDN question.
;;
;; RELATION TO THE COPY/COMPOSE RESULT ALREADY IN HAND
;;   Appendix B measured copy/compose at 0.96x with predictability making no
;;   difference (D/E = 1.00), which argues against a coherence discount. But it was
;;   measured on INTEGER SEQUENCES, not Clojure, so it does not settle the
;;   domain-coherence mechanism Gene describes. These arms carry REAL CLOJURE in
;;   both, which is exactly the case Appendix B did not cover.
;;
;; DESIGN
;;   J  emit a JSON object carrying the Clojure payload in "new"
;;   E  emit a bare EDN map carrying the IDENTICAL Clojure payload in :new
;;   C  floor (Appendix A protocol: three conditions, because two cannot separate
;;      the fixed per-turn cost from the emission cost)
;;
;;   time-per-token = (wall - floor) / decoded tokens, per arm, from the provider
;;   usage report. Identical content in both arms; only the carriage differs.
;;
;; PREDECLARED
;;   VALIDITY  a trial counts if it exited 0, reported usage, and parsed in its own
;;             carriage. Payload equality is recorded but does NOT gate the timing
;;             sample, since a corrupted payload still took the time it took; it is
;;             reported separately so a reader can exclude it.
;;   Losses stay in the chart. Parity is a result, not a failure.
;;
;; Usage: bb bench/measure_carriage_generation_time.clj OUT_DIR [REPLICATES]

(ns measure-carriage-generation-time
  (:require
   [babashka.process :as p]
   [cheshire.core :as json]
   [cheshire.factory :as factory]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(def probe-schema "clj-surgeon.carriage-generation-time-probe/v1")

(def strict-json-factory (factory/make-json-factory {:allow-unquoted-control-chars false}))

;; Real Clojure, not a synthetic filler. Strings, regexes, nested maps, threading,
;; destructuring, namespaced keywords — the content a write request actually carries.
(def payload
  (str/join
   "\n"
   ["(ns app.report.render"
    "  (:require [clojure.string :as str]"
    "            [app.db :as db]"
    "            [app.fmt :as fmt]))"
    ""
    "(def ^:private row-re #\"^\\\\s*\\\\|(.+)\\\\|\\\\s*$\")"
    ""
    "(def defaults"
    "  {:page {:size 50 :offset 0}"
    "   :sort [[:created-at :desc] [:id :asc]]"
    "   :filters {:status #{:open :pending} :owner nil}"
    "   :render {:escape? true :max-width 120}})"
    ""
    "(defn- escape-cell [s]"
    "  (-> (str s)"
    "      (str/replace \"\\\\\" \"\\\\\\\\\")"
    "      (str/replace \"|\" \"\\\\|\")"
    "      (str/replace #\"\\\\s+\" \" \")))"
    ""
    "(defn render-row"
    "  [{:keys [id title owner status created-at] :as row} opts]"
    "  (let [{:keys [escape? max-width]} (merge (:render defaults) opts)"
    "        cell (if escape? escape-cell str)]"
    "    (str \"| \" (cell id)"
    "         \" | \" (cell (fmt/truncate title max-width))"
    "         \" | \" (cell (or owner \"unassigned\"))"
    "         \" | \" (name status)"
    "         \" | \" (fmt/iso created-at) \" |\")))"
    ""
    "(defn render-table"
    "  [rows opts]"
    "  (let [header [\"id\" \"title\" \"owner\" \"status\" \"created\"]"
    "        sep (str/join \" | \" (repeat (count header) \"---\"))]"
    "    (->> rows"
    "         (map #(render-row % opts))"
    "         (cons (str \"| \" sep \" |\"))"
    "         (cons (str \"| \" (str/join \" | \" header) \" |\"))"
    "         (str/join \"\\n\"))))"
    ""
    "(defn fetch-and-render"
    "  [conn {:keys [status limit] :or {limit 50}}]"
    "  (let [rows (db/query conn {:select [:*]"
    "                             :from [:reports]"
    "                             :where [:in :status (or status [:open])]"
    "                             :limit limit})]"
    "    (if (seq rows)"
    "      (render-table rows {:escape? true})"
    "      \"| no rows |\")))"]))

(def preamble
  "You are producing ONE edit request for a code-editing tool. Output the request and nothing else: no explanation, no preamble, no markdown code fences.")

(defn task-spec []
  (str "The edit request must carry exactly these values:\n"
       "  file  = \"src/app/report/render.clj\"\n"
       "  owner = \"render-table\"\n"
       "  action = \"replace\"\n"
       "  new   = the following Clojure source, reproduced EXACTLY, character for character:\n"
       "-----BEGIN SOURCE-----\n" payload "\n-----END SOURCE-----"))

(def prompt-J
  (str preamble "\n\nEmit the request as a single JSON object with the keys "
       "\"file\", \"owner\", \"action\", \"new\". It must be valid JSON that a strict parser accepts.\n\n"
       (task-spec)))

(def prompt-E
  (str preamble "\n\nEmit the request as a single EDN map with the keys "
       ":file, :owner, :action, :new. It must be valid EDN that clojure.edn/read-string accepts. "
       "Do not wrap it in JSON.\n\n"
       (task-spec)))

(def prompt-C "Do not use any tools. Reply with exactly one word: ok")

(defn parse-arm [arm t]
  (try (case arm
         "J" {:parsed (binding [factory/*json-factory* strict-json-factory]
                        (json/parse-string (str/trim t) true))}
         "E" {:parsed (edn/read-string (str/trim t))}
         "C" {:parsed :floor})
       (catch Exception e {:error (str (.getSimpleName (class e)) ": " (.getMessage e))})))

(defn run-codex [prompt cwd]
  (let [t0 (System/nanoTime)
        {:keys [out exit]} (apply p/sh {:in prompt :dir cwd :continue true}
                                  ["codex" "exec" "--json" "--skip-git-repo-check" "--ephemeral"
                                   "-s" "read-only" "--ignore-user-config"
                                   "-c" "model=gpt-5.6-sol" "-c" "model_reasoning_effort=low" "-"])
        events (->> (str/split-lines (or out "")) (remove str/blank?)
                    (keep #(try (json/parse-string % true) (catch Exception _ nil))))]
    {:message (or (->> events (keep #(when (= "agent_message" (get-in % [:item :type]))
                                       (get-in % [:item :text]))) first) "")
     :usage (->> events (keep :usage) last)
     :exit exit
     :wall-ms (Math/round (double (/ (- (System/nanoTime) t0) 1e6)))}))

(defn -main [& args]
  (let [out-dir (or (first args) (throw (ex-info "usage: OUT_DIR [REPLICATES]" {})))
        reps (Integer/parseInt (or (second args) "9"))
        work (str out-dir "/work")
        _ (.mkdirs (io/file work))
        _ (.mkdirs (io/file (str out-dir "/raw")))
        facts (atom [])]
    (spit (str out-dir "/meta.json")
          (json/generate-string
           {:schema probe-schema :replicates reps :arms ["C" "J" "E"]
            :payload-bytes (count (.getBytes payload "UTF-8"))
            :payload-lines (count (str/split-lines payload))
            :model "gpt-5.6-sol" :reasoning "low"
            :hostname (str/trim (:out (p/sh "hostname")))
            :loadavg-start (str/trim (:out (p/sh "sh" "-c" "cut -d' ' -f1-3 /proc/loadavg")))
            :started (str (java.time.Instant/now))} {:pretty true}))
    (doseq [rep (range 1 (inc reps))]
      ;; counterbalanced arm order so neither carriage always follows the other
      (doseq [[arm prompt] (if (even? rep)
                             [["C" prompt-C] ["E" prompt-E] ["J" prompt-J]]
                             [["C" prompt-C] ["J" prompt-J] ["E" prompt-E]])]
        (let [tag (format "r%02d-%s" rep arm)
              {:keys [message usage exit wall-ms]} (run-codex prompt work)
              {:keys [parsed error]} (parse-arm arm message)
              got (when (map? parsed) (or (:new parsed) (get parsed "new")))]
          (spit (str out-dir "/raw/" tag ".message.txt") message)
          (swap! facts conj
                 {:tag tag :replicate rep :arm arm :exit exit :wall-ms wall-ms
                  :output-tokens (:output_tokens usage)
                  :reasoning-tokens (:reasoning_output_tokens usage)
                  :decoded-tokens (when usage (+ (or (:output_tokens usage) 0)
                                                 (or (:reasoning_output_tokens usage) 0)))
                  :input-tokens (:input_tokens usage)
                  :output-bytes (count (.getBytes (str message) "UTF-8"))
                  :parse-ok (nil? error) :parse-error error
                  :payload-exact (= got payload)
                  :ok (and (= 0 exit) (some? usage))})
          (println (format "%-10s exit=%s %sms tokens=%s parse=%s exact=%s"
                           tag exit wall-ms (:decoded-tokens (last @facts))
                           (if error "FAIL" "ok") (= got payload))))))
    (spit (str out-dir "/facts.jsonl") (str/join "\n" (map json/generate-string @facts)))
    (println "\nprobe complete. fold with:")
    (println "  bb bench/score_carriage_generation_time.clj" out-dir "--markdown")))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
