(ns ^{:lane :fast} clj-surgeon.telemetry-events-test
  "Witnesses for TELEMETRY-EVENTS-001. Each pins a property the ledger is
   worthless without: one intact JSON line per call, the fields a reader
   counts by, atomicity under concurrent appends, a bounded line, and a
   ledger failure that costs the call nothing but is still reported."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-telemetry :as telemetry]
   [clj-surgeon.ns-isolation :as iso]
   [clj-surgeon.telemetry-events :as events]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing use-fixtures]])
  (:import
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute PosixFilePermissions)))

(def required-fields
  "Every field a count-first reader needs from one line."
  [:ts :seat :pid :kind :tool :ok :error_type :wall_ms :mission_id])

(defn- temp-dir
  "@spec TEST-ISO-003 -- scratch under this namespace's own subdir."
  [prefix]
  (.toFile (Files/createTempDirectory
             (.toPath (iso/namespace-tmp-dir 'clj-surgeon.telemetry-events-test))
             prefix
             (into-array FileAttribute []))))

(defn- reset-drops! [f] (reset! events/dropped 0) (f) (reset! events/dropped 0))

(defn- delete-tree!
  [^java.io.File f]
  (when (.isDirectory f) (run! delete-tree! (.listFiles f)))
  (.delete f))

(defn- clean-namespace-tmp!
  "@spec TEST-ISO-003 -- this namespace owns `nsiso-clj-surgeon.telemetry-
   events-test` under the run's temp root, and owning it includes REMOVING it.
   Sol's fence run (2026-09-06) ended with `temp-leak: 1 entries` naming
   exactly this directory: the isolation fold attributed the leak correctly,
   and the namespace still had no cleanup. `:once` and a `finally`, so a
   failing assertion inside a test cannot leave the tree behind either.

   The permission fixtures below deliberately leave a directory unwritable, so
   the tree is re-opened to `rwx------` on the way down -- a cleanup that can
   be defeated by the test it is cleaning up after is not a cleanup."
  [f]
  (try (f)
       (finally
         (let [root (iso/namespace-tmp-dir 'clj-surgeon.telemetry-events-test)]
           (try
             (doseq [^java.io.File d (file-seq root)
                     :when (.isDirectory d)]
               (try (Files/setPosixFilePermissions
                      (.toPath d) (PosixFilePermissions/fromString "rwx------"))
                    (catch Exception _ nil)))
             (delete-tree! root)
             (catch Exception _ nil))))))

(use-fixtures :once clean-namespace-tmp!)
(use-fixtures :each reset-drops!)

(defn- lines [file] (->> (slurp file) str/split-lines (remove str/blank?)))

(deftest one-call-appends-one-valid-json-line-with-every-field
  (let [file (io/file (temp-dir "events-one-line-") "events.jsonl")]
    (events/record! file {:kind "mcp-call" :tool "inspect_clojure" :ok true
                          :error_type nil :wall_ms 12.6 :mission_id "m-7"})
    (let [written (lines file)
          parsed (json/parse-string (first written) true)]
      (is (= 1 (count written)) "one call, one line")
      (is (every? #(contains? parsed %) required-fields)
          (str "missing: " (remove #(contains? parsed %) required-fields)))
      (is (= "mcp-call" (:kind parsed)))
      (is (= "inspect_clojure" (:tool parsed)))
      (is (true? (:ok parsed)))
      (is (nil? (:error_type parsed)))
      (is (= 13 (:wall_ms parsed)) "wall is a rounded long, not a float")
      (is (= (events/safe-mission-id "m-7") (:mission_id parsed))
          "the id is projected by policy, not copied")
      (is (str/starts-with? (:mission_id parsed) "sha256:")
          "m-7 is not the minted M-<digits> form, so it is hashed")
      (is (= (events/current-pid) (:pid parsed)))
      (is (string? (:seat parsed)))
      (is (str/ends-with? (first written) "}") "the line is complete"))))

(deftest a-refusal-line-carries-its-error-type-and-a-null-mission
  (let [file (io/file (temp-dir "events-refusal-") "events.jsonl")]
    (events/record! file {:kind "mcp-call" :tool "apply_clojure_changes"
                          :ok false :error_type "verification-incomplete"
                          :wall_ms 40})
    (let [parsed (json/parse-string (first (lines file)) true)]
      (is (false? (:ok parsed)))
      (is (= "verification-incomplete" (:error_type parsed)))
      (is (nil? (:mission_id parsed)) "absent mission is null, not omitted"))))

(deftest eight-threads-fifty-lines-each-produce-four-hundred-intact-lines
  (let [file (io/file (temp-dir "events-concurrent-") "events.jsonl")
        threads (mapv (fn [t]
                        (Thread.
                          ^Runnable
                          (fn []
                            (dotimes [i 50]
                              (events/record!
                                file {:kind "mcp-call"
                                      :tool (str "tool-" t "-" i)
                                      :ok true :wall_ms i})))))
                      (range 8))]
    (run! #(.start ^Thread %) threads)
    (run! #(.join ^Thread %) threads)
    (let [written (lines file)
          parsed (map #(json/parse-string % true) written)]
      (is (= 400 (count written)) "no line lost")
      (is (= 400 (count (filter map? parsed))) "no line interleaved or split")
      (is (= 400 (count (distinct (map :tool parsed))))
          "every writer's own lines survived intact"))))

(deftest oversize-free-text-is-truncated-and-says-so
  (let [file (io/file (temp-dir "events-oversize-") "events.jsonl")
        huge (apply str (repeat 5000 "x"))]
    (events/record! file {:kind "mcp-call" :tool "apply_clojure_changes"
                          :ok false :error_type huge :wall_ms 1})
    (let [line (first (lines file))
          parsed (json/parse-string line true)]
      (is (<= (count (.getBytes line "UTF-8")) events/line-limit)
          "the line stays inside the atomic-append budget")
      (is (= events/free-text-limit (count (:error_type parsed))))
      (is (true? (:error_type_truncated parsed)) "the truncation is declared"))))

(deftest an-unwritable-ledger-costs-the-call-nothing-and-is-counted
  (let [base (temp-dir "events-unwritable-")
        locked (doto (io/file base "locked") .mkdirs)
        file (io/file locked "events.jsonl")
        good (io/file base "events.jsonl")]
    (Files/setPosixFilePermissions (.toPath locked)
                                   (PosixFilePermissions/fromString "r-xr-xr-x"))
    (try
      (is (nil? (events/record! file {:kind "mcp-call" :tool "inspect_clojure"
                                      :ok true :wall_ms 3}))
          "a failed append returns nil rather than throwing")
      (is (= 1 @events/dropped) "the drop is counted in-process")
      (events/record! file {:kind "mcp-call" :tool "inspect_clojure"
                            :ok true :wall_ms 3})
      (is (= 2 @events/dropped))
      (let [reported (events/record! good {:kind "mcp-call" :tool "inspect_clojure"
                                           :ok true :wall_ms 3})]
        (is (= 2 (:telemetry_dropped reported))
            "the next line that lands reports what was lost")
        (is (= 2 (:telemetry_dropped
                   (json/parse-string (first (lines good)) true))))
        (is (zero? @events/dropped) "reported exactly once, then cleared"))
      (finally
        (Files/setPosixFilePermissions
          (.toPath locked) (PosixFilePermissions/fromString "rwx------"))))))

(deftest a-healthy-line-omits-the-drop-counter
  (let [file (io/file (temp-dir "events-clean-") "events.jsonl")]
    (events/record! file {:kind "mcp-call" :tool "inspect_clojure" :ok true})
    (is (not (contains? (json/parse-string (first (lines file)) true)
                        :telemetry_dropped))
        "zero drops is silence, so a present counter always means trouble")))

(deftest the-ledger-file-is-private
  (let [file (io/file (temp-dir "events-perms-") "nested" "events.jsonl")]
    (events/record! file {:kind "mcp-call" :tool "inspect_clojure" :ok true})
    (is (= "rw-------"
           (PosixFilePermissions/toString
             (Files/getPosixFilePermissions (.toPath file) (into-array java.nio.file.LinkOption [])))))
    (is (= "rwx------"
           (PosixFilePermissions/toString
             (Files/getPosixFilePermissions
               (.toPath (.getParentFile file)) (into-array java.nio.file.LinkOption [])))))))

(deftest a-tool-call-writes-the-ledger-even-with-per-server-telemetry-off
  (let [file (io/file (temp-dir "events-mode-off-") "events.jsonl")]
    ;; Sol fence r4: the WRITER resolves `events-file` (override, else default),
    ;; so a test that pins where the writer lands redefines THAT -- redefining
    ;; `default-events-file` is silently bypassed whenever the harness exports
    ;; CLJ_SURGEON_EVENTS_FILE, which `~/bin/suite-run` does.
    (with-redefs [events/events-file (fn [] (.getAbsolutePath file))]
      (let [state (telemetry/start! {:mode :off})]
        (telemetry/record-inspect-call!
          state {:requests [{:operation "outline" :file "a.clj"}]}
          {:ok true :request_count 1 :file_count 1} {:total_ms 9})
        (is (.isFile file)
            "a server started with telemetry off still lands in the ledger")
        (let [parsed (json/parse-string (first (lines file)) true)]
          (is (= "inspect_clojure" (:tool parsed)))
          (is (true? (:ok parsed)))
          (is (= 9 (:wall_ms parsed))))))))

;; ---------------------------------------------------------------------------
;; SOL FENCE r1 (2026-09-06) -- the three ledger HOLDs, each with its witness.

(deftest a-key-shaped-mission-id-is-hashed-and-never-persisted-raw
  ;; Sol put `gsk_LEDGER_CANARY|FILE-CONTENT-CANARY` through the caller's
  ;; mission_id and read both canaries back out of the "content-free" ledger.
  ;; A field copied from a caller is content, and content is what this ledger
  ;; promises not to hold.
  (let [file (io/file (temp-dir "events-mission-id-") "events.jsonl")
        canary "gsk_LEDGER_CANARY|FILE-CONTENT-CANARY"]
    (events/record! file {:kind "mcp-call" :tool "inspect_clojure" :ok true
                          :mission_id canary})
    (let [raw (slurp file)
          parsed (json/parse-string (first (lines file)) true)]
      (is (not (str/includes? raw "gsk_LEDGER_CANARY"))
          "the raw key-shaped id never reaches the file")
      (is (not (str/includes? raw "FILE-CONTENT-CANARY"))
          "nor does the content smuggled beside it")
      (is (str/starts-with? (:mission_id parsed) "sha256:")
          (str "an id that is not a name is persisted as a digest, got "
               (pr-str (:mission_id parsed))))
      (is (= 23 (count (:mission_id parsed))) "sha256: + 16 hex")
      (is (= (:mission_id parsed)
             (:mission_id (events/line-map {:mission_id canary})))
          "the digest is stable, so a mission's lines still group")))
  (testing "a well-formed name is kept raw -- the ledger stays readable"
    (is (= "M-17" (events/safe-mission-id "M-17"))
        "the mission ledger's own minted form is the ONLY raw shape")
    (is (str/starts-with? (events/safe-mission-id "real-2j") "sha256:")
        "a free-form runner mission name is a digest, not a word")
    (is (nil? (events/safe-mission-id nil)) "absent is a fact, not a digest"))
  (testing "the wide identifier shape would have admitted a live credential"
    (is (str/starts-with? (events/safe-mission-id "gsk_ABCdef123") "sha256:")
        "gsk_ABCdef123 matches ^[A-Za-z0-9._-]{1,64}$ and is still a key")))

(deftest an-extra-scalar-field-passes-through-and-a-nested-value-does-not
  ;; THE PASS-THROUGH RULE (Astra, 2026-09-06). The mission boundary emits its
  ;; own dimensions -- mission_state, mission_verb, executor, candidate_count
  ;; -- and a writer that must be edited before a caller may count a new
  ;; dimension is a writer callers stop using. The rule is about SHAPES, not
  ;; names: a scalar is a value someone chose to report, a collection is a
  ;; structure someone forgot to summarize.
  (let [file (io/file (temp-dir "events-extras-") "events.jsonl")]
    (events/record! file {:kind "mission-call" :tool "mission_apply" :ok true
                          :mission_state "applied" :mission_verb "apply"
                          :executor "sol" :candidate_count 3 :model "gpt-5.6-sol"
                          :route "openrouter" :replayed false
                          :dossier {:files ["a.clj"] :body "..."}
                          :candidates [1 2 3]
                          :leaked "Authorization: Bearer sk-live.ABC-"})
    (let [parsed (json/parse-string (first (lines file)) true)]
      (is (= "applied" (:mission_state parsed)))
      (is (= "apply" (:mission_verb parsed)))
      (is (= "sol" (:executor parsed)))
      (is (= 3 (:candidate_count parsed)) "a number survives as a number")
      (is (= "gpt-5.6-sol" (:model parsed)))
      (is (= "openrouter" (:route parsed)))
      (is (false? (:replayed parsed)) "a boolean survives as a boolean")
      (is (not (contains? parsed :dossier)) "a map is dropped, never rendered")
      (is (not (contains? parsed :candidates)) "so is a vector")
      (is (= "Authorization: <redacted>" (:leaked parsed))
          "an extra takes the scrubber like every other string field")))
  (testing "an extra is bounded by the same byte ceiling"
    (let [huge (apply str (repeat 10000 "e"))
          line (events/line-map {:kind "mission-call" :some_extra huge})]
      (is (= events/free-text-limit
             (count (.getBytes ^String (get line "some_extra") "UTF-8"))))))
  (testing "an extra can never shadow a named field"
    (is (true? (:ok (events/line-map {:ok true :kind "k"}))))))

(deftest an-extra-field-name-can-neither-shadow-nor-smuggle
  ;; SOL FENCE R2. `merge` de-duplicates by KEY; JSON de-duplicates by NAME.
  ;; The string key "ok" and the keyword :ok are different keys and one JSON
  ;; name, so both were written and every parser kept the caller's. And an
  ;; extra's NAME was copied to the file verbatim, with the scrubber pointed
  ;; only at values -- `gsk_FIELDNAMECANARY` reached the ledger as a key.
  (testing "every spelling that would collide with a named field is rejected"
    (doseq [k [:Ok "ok" :OK "OK" :ts :Mission-Id "wall_ms" "telemetry_dropped"
               "over_limit" "dropped_fields" "error_type_truncated"]]
      (is (nil? (events/normalize-extra-name k))
          (str (pr-str k) " normalizes onto a field the writer emits"))))
  (testing "a colliding spelling never reaches the line, and is counted"
    (let [line (events/line-map {:ok true :kind "k" :tool "t"
                                 "ok" "<shadow>" "tool" "shadow-tool"
                                 :Ok "<shadow>"})
          parsed (json/parse-string (events/render-line line) true)]
      (is (true? (:ok parsed)) "the writer's ok survives serialization")
      (is (= "t" (:tool parsed)) "and so does the writer's tool")
      (is (= 3 (:dropped_fields parsed)) "all three collisions are counted")
      (is (not (str/includes? (events/render-line line) "shadow"))
          "no shadow value is anywhere in the rendered bytes")))
  (testing "a key-shaped field NAME is dropped, never redacted, never written"
    (let [line (events/line-map {:kind "k" (keyword "gsk_FIELDNAMECANARY") 7
                                 "sk-or-v1_NAMECANARY" 9})
          rendered (events/render-line line)]
      (is (not (str/includes? rendered "gsk_")))
      (is (not (str/includes? rendered "sk-or-")))
      (is (not (str/includes? (str/lower-case rendered) "namecanary")))
      (is (= 2 (:dropped_fields (json/parse-string rendered true))))))
  (testing "a name that misses the shape is dropped and counted"
    (let [long-name (apply str (repeat 200 "n"))
          line (events/line-map {:kind "k" (keyword long-name) 1
                                 (keyword "has.a.dot") 2 "" 3
                                 (keyword "_leading") 4 (keyword "9leading") 5})]
      (is (not (contains? line long-name)) "a 200-char name is dropped")
      (is (= 5 (:dropped_fields line)) "each rejected name is counted once")))
  (testing "a valid extra still survives, normalized"
    (let [line (events/line-map {:kind "k" :ok true :Mission-State "applied"
                                 :o-k "not-ok" :candidate_count 3})
          parsed (json/parse-string (events/render-line line) true)]
      (is (= "applied" (:mission_state parsed)) "case and hyphens fold")
      (is (= "not-ok" (:o_k parsed)) ":o-k is o_k and shadows nothing")
      (is (true? (:ok parsed)) "and ok is still the writer's boolean")
      (is (= 3 (:candidate_count parsed)))
      (is (not (contains? parsed :dropped_fields))
          "nothing was dropped, so the count is absent")))
  (testing "two keys that normalize to one name do not race"
    (let [line (events/line-map {:kind "k" :some-extra "a" "some_extra" "b"})]
      (is (= 1 (:dropped_fields line)) "the second claim is dropped")
      (is (contains? #{"a" "b"} (get line "some_extra"))))))

(deftest every-string-field-is-scrubbed-of-key-shaped-values
  (let [file (io/file (temp-dir "events-scrub-") "events.jsonl")]
    (events/record! file {:kind "mcp-call" :tool "apply_clojure_changes"
                          :ok false
                          :error_type "upstream said: Authorization: Bearer sk-live.ABC-"
                          :provider "router gsk_DEADBEEF"
                          :upstream "sk-or-v1_abcDEF-09"})
    (let [raw (slurp file)
          parsed (json/parse-string (first (lines file)) true)]
      (is (not (str/includes? raw "gsk_DEADBEEF")))
      (is (not (str/includes? raw "sk-or-v1_abcDEF-09")))
      (is (not (str/includes? raw "Bearer sk-live.ABC-")))
      (is (str/includes? (:error_type parsed) "<redacted>")
          "the redaction is visible, so a reader knows something was removed")
      (is (= "router <redacted>" (:provider parsed)))
      (is (= "<redacted>" (:upstream parsed))))))

(deftest no-input-can-push-a-line-past-the-atomic-append-budget
  ;; `huge-line-bytes=5185 limit=4096` -- the old fallback rebuilt the line
  ;; without bounding `seat`, which comes from the environment and was never
  ;; truncated at all.
  (let [file (io/file (temp-dir "events-ceiling-") "events.jsonl")
        huge (apply str (repeat 10000 "x"))]
    (events/record! file {:kind huge :tool huge :ok false :error_type huge
                          :mission_id huge :provider huge :upstream huge
                          :wall_ms 1})
    (let [line (first (lines file))]
      (is (< (count (.getBytes line "UTF-8")) events/line-limit)
          (str "line is " (count (.getBytes line "UTF-8")) " bytes"))
      (is (map? (json/parse-string line true)) "and it is still one valid line")))
  (testing "a 10 KB seat -- an environment value, not a tool argument"
    (let [line (events/render-line
                 (events/line-map {:kind "mcp-call" :tool "inspect_clojure"
                                   :ok true :wall_ms 1
                                   :seat (apply str (repeat 10000 "s"))}))]
      (is (< (count (.getBytes line "UTF-8")) events/line-limit)
          (str "line is " (count (.getBytes line "UTF-8")) " bytes"))))
  (testing "the floor is reachable and is itself under the ceiling"
    (let [line (events/render-line
                 {:ts "2026-09-06T00:00:00Z" :pid 1 :ok true :wall_ms 1
                  :seat (apply str (repeat 100000 "s"))})]
      (is (< (count (.getBytes line "UTF-8")) events/line-limit))
      (is (true? (:over_limit (json/parse-string line true)))
          "a shrunken line always says it was shrunk"))))

(deftest truncation-is-by-utf8-bytes-and-never-splits-a-codepoint
  (let [emoji (apply str (repeat 5000 "🙂")) ; U+1F642, 4 UTF-8 bytes
        [cut truncated?] (events/truncate emoji)]
    (is (true? truncated?))
    (is (<= (count (.getBytes cut "UTF-8")) events/free-text-limit)
        (str "cut is " (count (.getBytes cut "UTF-8")) " BYTES -- a 1024-CHAR "
             "bound would have been 4096 bytes here"))
    (is (= cut (String. (.getBytes cut "UTF-8") "UTF-8"))
        "the cut string survives a UTF-8 round trip: no split codepoint")
    (is (not (str/includes? cut "�")) "no replacement character")
    (is (even? (count cut)) "surrogate pairs are kept whole")
    (is (= 256 (count (re-seq #"🙂" cut)))
        "1024 bytes / 4 bytes per codepoint"))
  (testing "an ASCII field is unaffected by the change of unit"
    (is (= [(apply str (repeat events/free-text-limit "x")) true]
           (events/truncate (apply str (repeat 5000 "x")))))))

(deftest an-existing-parent-that-is-not-0700-is-tightened-before-the-write
  ;; `existing-dir-mode=755` -- chmod only ran when `.mkdirs` returned true,
  ;; so a directory this process did not create stayed world-readable forever.
  (let [parent (doto (io/file (temp-dir "events-existing-parent-") "ledger") .mkdirs)
        file (io/file parent "events.jsonl")
        opts (into-array java.nio.file.LinkOption [])]
    (Files/setPosixFilePermissions (.toPath parent)
                                   (PosixFilePermissions/fromString "rwxr-xr-x"))
    (is (= "rwxr-xr-x"
           (PosixFilePermissions/toString
             (Files/getPosixFilePermissions (.toPath parent) opts)))
        "precondition: the parent exists and is 0755")
    (events/record! file {:kind "mcp-call" :tool "inspect_clojure" :ok true})
    (is (= "rwx------"
           (PosixFilePermissions/toString
             (Files/getPosixFilePermissions (.toPath parent) opts)))
        "the append tightened the parent it found")
    (is (= "rw-------"
           (PosixFilePermissions/toString
             (Files/getPosixFilePermissions (.toPath file) opts))))
    (is (= 1 (count (lines file))) "and the line still landed")))

(deftest the-default-path-is-the-home-dotdir
  (is (str/ends-with? (events/default-events-file) "/.clj-surgeon/events.jsonl")
      "the ledger is a HOME dotdir, not one of the launcher-chosen state roots")
  (is (not (str/blank? (events/default-events-file)))))

(deftest the-env-override-wins-for-the-writer
  ;; Sol fence r4: the DEFAULT and the OVERRIDE are two facts. `default-events-file`
  ;; states the default and must stay true under `~/bin/suite-run`, which exports
  ;; CLJ_SURGEON_EVENTS_FILE; `events-file` is what a writer resolves, and the
  ;; override has to win THERE or the isolation variable does nothing.
  (let [file (io/file (temp-dir "events-override-") "elsewhere.jsonl")]
    (with-redefs [events/events-file-override (fn [] (.getAbsolutePath file))]
      (is (= (.getAbsolutePath file) (events/events-file))
          "the writer resolves the override, not the home dotdir")
      (is (not= (events/default-events-file) (events/events-file)))
      (events/record! {:kind "call" :tool "inspect_clojure" :ok true})
      (is (.isFile file) "record! with no explicit path appended to the override")
      (is (= "inspect_clojure" (:tool (json/parse-string (first (lines file)) true)))))
    (with-redefs [events/events-file-override (fn [] "   ")]
      (is (= (events/default-events-file) (events/events-file))
          "a blank override is not an override"))
    (with-redefs [events/events-file-override (fn [] nil)]
      (is (= (events/default-events-file) (events/events-file))))))

(def cost-fields
  "The optional cost fields. A caller that has them passes them through; a
   caller that does not gets nulls -- never a zero, which would read as a free
   call in a ledger whose whole purpose is telling Gene what he is paying."
  [:prompt_tokens :completion_tokens :reasoning_tokens :cost_usd
   :provider :upstream])

(deftest cost-fields-round-trip-and-are-null-when-the-caller-has-none
  (let [file (io/file (temp-dir "events-cost-") "events.jsonl")]
    (events/record! file {:kind "typist-call" :tool "arm-F" :ok true
                          :wall_ms 1234 :mission_id "onesite"
                          :prompt_tokens 806 :completion_tokens 318
                          :reasoning_tokens 256 :cost_usd 5.21E-4
                          :provider "openrouter" :upstream "Cerebras"})
    (events/record! file {:kind "mcp-call" :tool "inspect_clojure" :ok true})
    (let [[priced unpriced] (map #(json/parse-string % true) (lines file))]
      (is (= 806 (:prompt_tokens priced)))
      (is (= 318 (:completion_tokens priced)))
      (is (= 256 (:reasoning_tokens priced)))
      (is (= 5.21E-4 (:cost_usd priced))
          "the USD survives serialization at its real magnitude, not rounded to a cent")
      (is (= "openrouter" (:provider priced)))
      (is (= "Cerebras" (:upstream priced)))
      (doseq [f cost-fields]
        (is (contains? unpriced f)
            (str f " is PRESENT on a line whose caller has no such number")))
      (is (every? nil? (map unpriced cost-fields))
          "an MCP tool call has no tokens and no cost: null, never zero")
      (doseq [f required-fields]
        (is (contains? priced f) (str f " still present on a priced line"))))))
