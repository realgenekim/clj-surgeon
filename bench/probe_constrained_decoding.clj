#!/usr/bin/env bb
;; probe_constrained_decoding.clj — is the JSON arm structurally PROTECTED?
;;
;; WHY THIS EXISTS — the confound that can invalidate X5 outright
;;   MCP tool arguments are JSON, and many providers enforce the tool's JSON
;;   schema with CONSTRAINED DECODING: the model is not merely unlikely to emit
;;   malformed arguments, it is PREVENTED from doing so at the sampler. Move the
;;   payload into a single JSON string and the content leaves that guarantee —
;;   the schema can then only assert that a string is a string.
;;
;;   If that is what this route does, then a JSON arm scoring zero malformed
;;   requests measures THE DECODER, NOT THE MODEL, and any difference against an
;;   unguarded EDN arm is an artefact of protection rather than of format.
;;   Reporting that as "EDN is more error-prone" would be flatly wrong.
;;
;; WHAT THIS PROBE ESTABLISHES, empirically rather than from documentation
;;   P1  Does --output-schema actually constrain the final message on this route?
;;       Ask for output that VIOLATES a supplied schema. If the platform enforces
;;       it, the violation cannot appear.
;;   P2  Can the model be made to emit syntactically invalid JSON as free text
;;       when explicitly instructed to? This is the unprotected baseline: if it
;;       CAN, then free-text JSON is genuinely unguarded, and the X5 arms — which
;;       are all free text — are comparing format fluency on equal footing.
;;   P3  Control: the same instruction for EDN.
;;
;; HOW TO READ THE RESULT
;;   P1 enforced + P2 permitted  -> production tool-call JSON is protected in a way
;;                                  free-text JSON is not. X5's arms are equal to
;;                                  each other, but NEITHER is production JSON, and
;;                                  moving to EDN-in-a-string forfeits a real
;;                                  provider-enforced guarantee. That cost belongs
;;                                  in the recommendation.
;;   P1 not enforced             -> no sampler-level guarantee is visible on this
;;                                  route at all, and the X5 comparison stands
;;                                  unqualified.
;;   Anything ambiguous          -> report UNVERIFIED and label every rate in the
;;                                  study confounded. An honest unverified is worth
;;                                  more than a clean-looking number.
;;
;; Usage: bb bench/probe_constrained_decoding.clj OUT_DIR [REPLICATES]

(ns probe-constrained-decoding
  (:require
   [babashka.process :as p]
   [cheshire.core :as json]
   [cheshire.factory :as factory]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(def probe-schema "clj-surgeon.constrained-decoding-probe/v1")

(def strict-json-factory (factory/make-json-factory {:allow-unquoted-control-chars false}))

(defn json-ok? [t]
  (try (binding [factory/*json-factory* strict-json-factory]
         (json/parse-string (str/trim (str t)) true)) true
       (catch Exception _ false)))

(defn edn-ok? [t]
  (try (edn/read-string (str/trim (str t))) true (catch Exception _ false)))

(defn run-codex [prompt cwd & [extra]]
  (let [args (concat ["codex" "exec" "--json" "--skip-git-repo-check" "--ephemeral"
                      "-s" "read-only" "--ignore-user-config"
                      "-c" "model=gpt-5.6-sol" "-c" "model_reasoning_effort=low"]
                     (or extra [])
                     ["-"])
        t0 (System/nanoTime)
        {:keys [out err exit]} (apply p/sh {:in prompt :dir cwd :continue true} args)
        events (->> (str/split-lines (or out ""))
                    (remove str/blank?)
                    (keep #(try (json/parse-string % true) (catch Exception _ nil))))]
    {:message (or (->> events (keep #(when (= "agent_message" (get-in % [:item :type]))
                                       (get-in % [:item :text]))) first) "")
     :exit exit :stderr (str err)
     :wall-ms (Math/round (double (/ (- (System/nanoTime) t0) 1e6)))}))

;; The schema P1 tries to violate. Deliberately narrow: one required key whose
;; value must be an integer.
(def strict-schema
  {:type "object"
   :properties {:value {:type "integer"}}
   :required ["value"]
   :additionalProperties false})

(def p1-prompt
  (str "Output a JSON object whose key \"value\" is the STRING \"not-a-number\", "
       "and also include an extra key \"junk\" set to true. "
       "Do not output an integer for \"value\" under any circumstances. Output only the object."))

(def p2-prompt
  (str "For a parser test I need deliberately INVALID JSON. "
       "Output exactly this, byte for byte, with the unterminated string and the trailing comma intact:\n"
       "{\"a\": \"unterminated, \"b\": [1,2,}\n"
       "Output only that line. Do not correct it. Do not explain."))

(def p3-prompt
  (str "For a parser test I need deliberately INVALID EDN. "
       "Output exactly this, byte for byte, with the unbalanced delimiters intact:\n"
       "{:a \"unterminated :b [1 2}\n"
       "Output only that line. Do not correct it. Do not explain."))

(defn -main [& args]
  (let [out-dir (or (first args) (throw (ex-info "usage: OUT_DIR [REPLICATES]" {})))
        reps (Integer/parseInt (or (second args) "5"))
        work (str out-dir "/work")
        _ (.mkdirs (io/file work))
        _ (.mkdirs (io/file (str out-dir "/raw")))
        schema-file (str work "/schema.json")
        _ (spit schema-file (json/generate-string strict-schema))
        facts (atom [])]
    (spit (str out-dir "/meta.json")
          (json/generate-string
           {:schema probe-schema :replicates reps
            :model "gpt-5.6-sol" :reasoning "low"
            :question "Does this route apply constrained/structured decoding to model output?"
            :hostname (str/trim (:out (p/sh "hostname")))
            :started (str (java.time.Instant/now))}
           {:pretty true}))
    (doseq [rep (range 1 (inc reps))]
      (doseq [[probe prompt extra judge]
              [["P1-schema-violation" p1-prompt ["--output-schema" schema-file]
                (fn [m] (let [parsed (try (binding [factory/*json-factory* strict-json-factory]
                                            (json/parse-string (str/trim m) true))
                                          (catch Exception _ ::unparseable))]
                          (cond
                            (= parsed ::unparseable) :unparseable
                            (and (map? parsed) (integer? (:value parsed))) :schema-enforced
                            (and (map? parsed) (string? (:value parsed))) :violation-permitted
                            :else :ambiguous)))]
               ["P2-invalid-json-freetext" p2-prompt nil
                (fn [m] (if (json-ok? m) :model-emitted-valid-json :invalid-json-permitted))]
               ["P3-invalid-edn-freetext" p3-prompt nil
                (fn [m] (if (edn-ok? m) :model-emitted-valid-edn :invalid-edn-permitted))]]]
        (let [tag (format "r%02d-%s" rep probe)
              {:keys [message exit wall-ms stderr]} (run-codex prompt work extra)
              verdict (judge message)]
          (spit (str out-dir "/raw/" tag ".message.txt") message)
          (swap! facts conj {:tag tag :replicate rep :probe probe :exit exit
                             :wall-ms wall-ms :verdict verdict :message message
                             :stderr-tail (apply str (take-last 300 stderr))})
          (println (format "%-34s exit=%s %sms -> %s" tag exit wall-ms (name verdict))))))
    (spit (str out-dir "/facts.jsonl") (str/join "\n" (map json/generate-string @facts)))
    ;; ------------------------------------------------------------- conclusion
    (let [by (group-by :probe @facts)
          tally #(frequencies (map :verdict (get by %)))
          p1 (tally "P1-schema-violation")
          p2 (tally "P2-invalid-json-freetext")
          p3 (tally "P3-invalid-edn-freetext")
          enforced? (and (pos? (get p1 :schema-enforced 0))
                         (zero? (get p1 :violation-permitted 0)))
          conclusion
          (cond
            (and enforced? (pos? (get p2 :invalid-json-permitted 0)))
            "PROTECTED-WHEN-SCHEMA-SUPPLIED: the platform enforced the supplied output schema, while free-text JSON was left unguarded. Production tool-call JSON therefore carries a guarantee that free-text JSON does not, and moving the payload into a string forfeits it."
            enforced?
            "SCHEMA ENFORCED, free-text baseline inconclusive."
            (pos? (get p1 :violation-permitted 0))
            "NOT ENFORCED: the model was permitted to violate the supplied output schema on this route, so no sampler-level guarantee is visible and the carriage comparison stands unqualified."
            :else
            "UNVERIFIED: could not establish enforcement either way. Every rate in the carriage study must be labelled confounded.")]
      (spit (str out-dir "/conclusion.json")
            (json/generate-string {:P1 p1 :P2 p2 :P3 p3
                                   :schema-enforced enforced?
                                   :conclusion conclusion} {:pretty true}))
      (println "\nP1 (schema violation attempt):" (pr-str p1))
      (println "P2 (invalid JSON as free text):" (pr-str p2))
      (println "P3 (invalid EDN as free text):" (pr-str p3))
      (println "\nCONCLUSION:" conclusion))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
