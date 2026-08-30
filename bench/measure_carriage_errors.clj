#!/usr/bin/env bb
;; measure_carriage_errors.clj — X5. Does an EDN carriage break less often than JSON?
;;
;; WHY THIS EXISTS
;;   Gene, 2026-08-29: "From JSON to EDN. Wow. Seems like total no brainer!! (Might
;;   be less training data, but I can't imagine LLMs screwing up EDN. I've never
;;   seen that in the wild!)"
;;
;;   The BYTE case for EDN is weak and would not justify a migration: 331 -> 297
;;   bytes on a real edit, about 4-5% of write traffic, far under the ~1.3 KB that
;;   one turn is worth. THE CASE IS ROBUSTNESS. Every malformed request costs a
;;   whole retry turn — roughly 620 output tokens and ~11 s at the working floor —
;;   and Appendix A priced a turn at ~222 output tokens. So a carriage that is even
;;   slightly less error-prone wins on TURNS, not bytes, and a two-point drop in
;;   malformed rate beats the entire byte prize.
;;
;; THE MECHANISM UNDER TEST (confirm or refute, do not merely report a rate)
;;   Models write Clojure maps constantly, and EDN treats commas as whitespace, so
;;   a leaked JSON-ism should degrade GRACEFULLY in EDN and FATALLY in JSON.
;;   A second, sharper asymmetry: EDN strings may contain LITERAL NEWLINES; JSON
;;   strings may not. Multi-line code is the common case, so this is where a JSON
;;   carriage is structurally exposed.
;;
;; THE THREE ARMS — and why three, not two
;;   J          the edit as JSON tool arguments (today's shape)
;;   E-wrapped  the edit as one EDN value inside a single JSON string argument
;;              (the deployable shape the coordinator specified)
;;   E-raw      the edit as bare EDN (no JSON envelope)
;;
;;   E-wrapped still pays JSON escaping for the one string it lives in, so on its
;;   own it cannot say whether any observed difference came from EDN or from
;;   having fewer JSON string fields. E-raw is the upper bound on what EDN could
;;   give if the transport allowed it, and E-wrapped minus E-raw is exactly how
;;   much of that the JSON wrapper hands back.
;;
;; PREDECLARED, before any data was collected
;;   PREDICTION      EDN malformed rate <= JSON malformed rate.
;;   KILL CRITERION  EDN malformed > 2x JSON kills the idea outright.
;;   VALIDITY        A trial is ROUTE-ADHERENT if it produced non-empty output that
;;                   attempts the requested carriage. Non-adherent trials are
;;                   reported separately, never folded into the malformed rate.
;;   MALFORMED       Anything the server could not parse, INCLUDING a code-fence
;;                   wrapper. A fence is not a stylistic quibble; a parser rejects
;;                   it. Classified separately so it can be discounted by anyone
;;                   who disagrees.
;;   CONTENT-EXACT   Parsing is necessary, not sufficient. A carriage that parses
;;                   but corrupts the payload is worse than one that fails loudly,
;;                   so payload equality is recorded independently of parse success.
;;   Losses stay in the chart. A null result parks a migration we would otherwise pay for.
;;
;; USAGE
;;   bb bench/measure_carriage_errors.clj OUT_DIR [REPLICATES]

(ns measure-carriage-errors
  (:require
   [babashka.process :as p]
   [cheshire.core :as json]
   [cheshire.factory :as factory]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(def probe-schema "clj-surgeon.carriage-error-probe/v1")

;; ------------------------------------------------------------------- fixtures
;; Adversarial ON PURPOSE. Gene's claim is that he has never seen an LLM produce
;; broken EDN in the wild; the only useful test is one run where it would break if
;; it were going to. Each fixture isolates a hazard that stresses one carriage,
;; the other, or both.
(def tasks
  [{:name "embedded-double-quotes"
    :hazard "string containing escaped double quotes — both carriages must escape"
    :file "src/app/msg.clj" :owner "render-warning"
    :code "(defn render-warning [n]\n  (str \"he said \\\"no\\\" \" n \" times\"))"}

   {:name "backslashes-and-regex"
    :hazard "backslashes in a regex and a Windows path — double-escaping trap"
    :file "src/app/parse.clj" :owner "token-re"
    :code "(def token-re #\"\\d+\\s*[A-Z]\\\\w\")\n(def win-path \"C:\\\\tmp\\\\out.txt\")"}

   {:name "literal-newlines-multiline"
    :hazard "multi-line code — EDN strings may contain literal newlines, JSON strings may not"
    :file "src/app/core.clj" :owner "start"
    :code "(defn start\n  [opts]\n  (let [p (:port opts)]\n    (server/run p)\n    (log/info \"started\" p)))"}

   {:name "reader-conditional-cljc"
    :hazard "reader conditional inside the payload"
    :file "src/app/plat.cljc" :owner "now-ms"
    :code "(defn now-ms []\n  #?(:clj (System/currentTimeMillis)\n     :cljs (.getTime (js/Date.))))"}

   {:name "namespaced-keywords"
    :hazard "namespaced and auto-resolved keywords"
    :file "src/app/spec.clj" :owner "review-spec"
    :code "(s/def ::review (s/keys :req [:review/id :review/body]\n                        :opt [::draft? :app.review/score]))"}

   {:name "deep-nesting"
    :hazard "deeply nested collections — delimiter balancing under depth"
    :file "src/app/cfg.clj" :owner "defaults"
    :code "(def defaults\n  {:a {:b {:c [{:d #{1 2 3}} {:e {:f [[:g] [:h {:i :j}]]}}]}}\n   :k [[[:l]]]})"}

   {:name "json-ism-inside-payload"
    :hazard "THE MECHANISM TEST: a JSON blob inside the code, commas and colons everywhere"
    :file "src/app/fixture.clj" :owner "sample-body"
    :code "(def sample-body\n  \"{\\\"a\\\": 1, \\\"b\\\": [2, 3], \\\"c\\\": {\\\"d\\\": null}}\")"}

   {:name "char-literals-and-metadata"
    :hazard "character literals and metadata — EDN reader edge cases"
    :file "src/app/text.clj" :owner "split-lines*"
    :code "(defn ^{:doc \"splits\"} split-lines* [s]\n  #_(legacy s)\n  (str/split s (re-pattern (str \\newline))))"}

   {:name "unicode-and-escapes"
    :hazard "unicode and escape sequences"
    :file "src/app/i18n.clj" :owner "labels"
    :code "(def labels\n  {:ok \"\\u2713 done\" :warn \"caf\\u00e9 \\u2014 retry\" :tab \"a\\tb\"})"}

   {:name "quotes-newlines-backslash-combined"
    :hazard "every hazard at once — the worst realistic case"
    :file "src/app/report.clj" :owner "render-row"
    :code "(defn render-row [{:keys [name path]}]\n  (str \"| \" name \" | \\\"\" path \"\\\" |\\n\"\n       \"regex: \" #\"\\\\S+\" \" |\"))"}])

;; -------------------------------------------------------------------- prompts
(def preamble
  "You are producing ONE edit request for a code-editing tool. Output the request and nothing else: no explanation, no preamble, no markdown code fences.")

(defn task-spec [{:keys [file owner code]}]
  (str "The edit request must carry exactly these four values:\n"
       "  file  = " (pr-str file) "\n"
       "  owner = " (pr-str owner) "\n"
       "  action = \"replace\"\n"
       "  new   = the following Clojure source, reproduced EXACTLY, character for character:\n"
       "-----BEGIN SOURCE-----\n" code "\n-----END SOURCE-----"))

(defn prompt-J [t]
  (str preamble "\n\n"
       "Emit the request as a single JSON object with the keys \"file\", \"owner\", \"action\", \"new\".\n"
       "It must be valid JSON that a strict parser accepts.\n\n"
       (task-spec t)))

(defn prompt-E-wrapped [t]
  (str preamble "\n\n"
       "Emit the request as a single JSON object with exactly one key, \"request\".\n"
       "The value of \"request\" is a STRING containing an EDN map with the keys "
       ":file, :owner, :action, :new.\n"
       "The outer JSON must be valid JSON, and the string it carries must be valid EDN.\n\n"
       (task-spec t)))

(defn prompt-E-raw [t]
  (str preamble "\n\n"
       "Emit the request as a single EDN map with the keys :file, :owner, :action, :new.\n"
       "It must be valid EDN that clojure.edn/read-string accepts. Do not wrap it in JSON.\n\n"
       (task-spec t)))

;; ------------------------------------------------------------------- parsing
(defn fence? [s] (boolean (re-find #"(?m)^\s*```" (str s))))

(defn strip-fence
  "Used ONLY to report what the payload would have been had the fence not been
  there. The trial is still counted malformed; this exists so the fence failure
  mode can be told apart from a genuine syntax failure."
  [s]
  (-> (str s)
      (str/replace #"(?s)\A\s*```[a-zA-Z]*\s*" "")
      (str/replace #"(?s)\s*```\s*\z" "")
      str/trim))

;; STRICT JSON, deliberately.
;; Cheshire's default factory ACCEPTS a literal newline inside a JSON string,
;; which strict JSON forbids. Leaving that on would have handed the JSON arm a
;; free pass on precisely the hazard where EDN is structurally better — EDN
;; strings may contain literal newlines and JSON strings may not — and would have
;; biased the whole experiment toward "no difference". Every JSON parse below
;; runs through a factory with that leniency disabled.
(def strict-json-factory
  (factory/make-json-factory {:allow-unquoted-control-chars false}))

(defn parse-json-strict [t]
  (binding [factory/*json-factory* strict-json-factory]
    (json/parse-string t true)))

(defn parse-arm
  "Returns {:parsed ... :error ...}. Never throws."
  [arm text]
  (let [t (str/trim (str text))]
    (try
      (case arm
        "J" {:parsed (parse-json-strict t)}
        "E-raw" {:parsed (edn/read-string t)}
        "E-wrapped"
        (let [outer (parse-json-strict t)]
          (if-let [r (:request outer)]
            {:parsed (edn/read-string r) :outer outer}
            {:error "outer JSON parsed but had no \"request\" key"})))
      (catch Exception e {:error (str (.getSimpleName (class e)) ": " (.getMessage e))}))))

(defn field
  "Pull one field out of whatever shape came back, keyword or string keyed."
  [parsed k]
  (when (map? parsed)
    (or (get parsed (keyword k)) (get parsed k))))

(defn payload-new [parsed] (field parsed "new"))

(defn semantic-diff
  "WELL-FORMED BUT WRONG: it parsed cleanly and means something other than what
  was asked. This is a different and worse class than MALFORMED — a malformed
  request is refused loudly and costs a retry turn, whereas this one is ACCEPTED
  and executes against the wrong thing. The two are scored separately and must
  never be collapsed into a single error rate."
  [parsed t]
  (when (map? parsed)
    (into {} (remove nil?
      [(when (not= (field parsed "file") (:file t))   [:file {:want (:file t) :got (field parsed "file")}])
       (when (not= (field parsed "owner") (:owner t)) [:owner {:want (:owner t) :got (field parsed "owner")}])
       (when (not= (field parsed "action") "replace") [:action {:want "replace" :got (field parsed "action")}])
       (when (not= (field parsed "new") (:code t))    [:new {:want (:code t) :got (field parsed "new")}])]))))

;; -------------------------------------------------------------------- codex
(def codex-base
  ["codex" "exec" "--json" "--skip-git-repo-check" "--ephemeral"
   "-s" "read-only" "--ignore-user-config"
   "-c" "model=gpt-5.6-sol" "-c" "model_reasoning_effort=low" "-"])

(defn run-codex [prompt cwd]
  (let [t0 (System/nanoTime)
        {:keys [out exit]} (apply p/sh {:in prompt :dir cwd :continue true} codex-base)
        wall (/ (- (System/nanoTime) t0) 1e6)
        events (->> (str/split-lines (or out ""))
                    (remove str/blank?)
                    (keep #(try (json/parse-string % true) (catch Exception _ nil))))]
    {:message (or (->> events
                       (keep #(when (= "agent_message" (get-in % [:item :type]))
                                (get-in % [:item :text])))
                       first) "")
     :usage (->> events (keep :usage) last)
     :exit exit
     :wall-ms (Math/round (double wall))}))

;; ---------------------------------------------------------------------- main
(defn loadavg []
  (try (str/trim (:out (p/sh "sh" "-c" "cut -d' ' -f1-3 /proc/loadavg")))
       (catch Exception _ "unknown")))

(defn -main [& args]
  (let [out-dir (or (first args) (throw (ex-info "usage: OUT_DIR [REPLICATES]" {})))
        reps (Integer/parseInt (or (second args) "3"))
        work (str out-dir "/work")
        _ (.mkdirs (io/file work))
        _ (.mkdirs (io/file (str out-dir "/raw")))
        facts (atom [])]
    (spit (str out-dir "/meta.json")
          (json/generate-string
           {:schema probe-schema :replicates reps
            :tasks (mapv #(dissoc % :code) tasks)
            :n-tasks (count tasks)
            :arms ["J" "E-wrapped" "E-raw"]
            :prediction "EDN malformed rate <= JSON malformed rate"
            :kill-criterion "EDN malformed > 2x JSON kills the idea"
            :model "gpt-5.6-sol" :reasoning "low"
            :hostname (str/trim (:out (p/sh "hostname")))
            :loadavg-start (loadavg)
            :started (str (java.time.Instant/now))}
           {:pretty true}))
    (doseq [rep (range 1 (inc reps))
            t tasks]
      ;; Counterbalanced: arm order rotates with the replicate so no arm always
      ;; runs first and inherits whatever the server was doing a moment earlier.
      (let [arms (case (mod rep 3)
                   0 [["J" prompt-J] ["E-wrapped" prompt-E-wrapped] ["E-raw" prompt-E-raw]]
                   1 [["E-wrapped" prompt-E-wrapped] ["E-raw" prompt-E-raw] ["J" prompt-J]]
                   [["E-raw" prompt-E-raw] ["J" prompt-J] ["E-wrapped" prompt-E-wrapped]])]
        (doseq [[arm pf] arms]
          (let [tag (format "r%02d-%s-%s" rep (:name t) arm)
                prompt (pf t)
                {:keys [message usage exit wall-ms]} (run-codex prompt work)
                fenced (fence? message)
                {:keys [parsed error]} (parse-arm arm message)
                ;; If a fence was the only problem, say so precisely rather than
                ;; reporting a syntax error that is not there.
                refried (when (and error fenced) (parse-arm arm (strip-fence message)))
                got-new (payload-new parsed)
                sem (semantic-diff parsed t)
                content-exact (= got-new (:code t))]
            (spit (str out-dir "/raw/" tag ".prompt.txt") prompt)
            (spit (str out-dir "/raw/" tag ".message.txt") message)
            (swap! facts conj
                   {:tag tag :replicate rep :task (:name t) :hazard (:hazard t)
                    :arm arm :exit exit :wall-ms wall-ms :usage usage
                    :output-bytes (count (.getBytes (str message) "UTF-8"))
                    :route-adherent (boolean (seq (str/trim (str message))))
                    :fenced fenced
                    :parse-ok (nil? error)
                    :parse-error error
                    :parse-ok-after-fence-strip (when refried (nil? (:error refried)))
                    :content-exact content-exact
                    :semantically-exact (and (nil? error) (empty? sem))
                    :wrong-fields (vec (keys sem))
                    :semantic-diff sem
                    :expected-new (:code t)
                    :got-new got-new})
            (println (format "%-52s exit=%s %sms parse=%s exact=%s%s"
                             tag exit wall-ms (if error "FAIL" "ok")
                             content-exact (if fenced " FENCED" "")))))))

    ;; ------------------------------------------------------- retry cost probe
    ;; Converts a malformed RATE into the number that actually decides: what one
    ;; recovery turn costs. Only runs if something was malformed.
    (let [bad (filter #(not (:parse-ok %)) @facts)]
      (println (format "\nmalformed: %d of %d" (count bad) (count @facts)))
      (when (seq bad)
        (println "measuring retry cost on each malformed request...")
        (doseq [f (take 12 bad)]
          (let [orig (slurp (str out-dir "/raw/" (:tag f) ".message.txt"))
                retry-prompt
                (str "Your previous output could not be parsed. The parser reported:\n"
                     (:parse-error f) "\n\nYour previous output was:\n" orig
                     "\n\nEmit the corrected request only. No explanation, no code fences.")
                {:keys [message usage wall-ms]} (run-codex retry-prompt work)
                {:keys [error]} (parse-arm (:arm f) message)]
            (swap! facts conj
                   (assoc f :tag (str (:tag f) "-RETRY") :retry-of (:tag f)
                          :is-retry true :wall-ms wall-ms :usage usage
                          :parse-ok (nil? error) :parse-error error
                          :output-bytes (count (.getBytes (str message) "UTF-8"))))
            (println (format "  retry %-46s %sms parse=%s"
                             (:tag f) wall-ms (if error "FAIL" "ok")))))))

    (spit (str out-dir "/facts.jsonl") (str/join "\n" (map json/generate-string @facts)))
    (spit (str out-dir "/env_after.json")
          (json/generate-string {:loadavg-end (loadavg) :ended (str (java.time.Instant/now))}))
    (println "\nprobe complete. fold with:")
    (println "  bb bench/score_carriage_errors.clj" out-dir "--markdown")))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
