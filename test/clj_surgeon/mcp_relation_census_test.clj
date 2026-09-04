(ns clj-surgeon.mcp-relation-census-test
  "Wire-level witnesses for the relation_census MCP tool.

   The census subject is real bytes: `test-fixtures/relation-census/folds.clj`
   carries the verbatim task-chase, agenda-selections, upsert-by and conj-once
   arms from curtaincall-cfp-lens at commit
   963875358a37c48ab6175ea1bea22633e4fd0306."
  (:require
   [babashka.process :as proc]
   [cheshire.core :as json]
   [clj-surgeon.census-discovery :as census-discovery]
   [clj-surgeon.core :as core]
   [clj-surgeon.forms :as forms]
   [clj-surgeon.mcp-paths :as mcp-paths]
   [clj-surgeon.mcp-relation-census :as census-tool]
   [clj-surgeon.mcp-workspace :as workspace]
   [clj-surgeon.relation-census :as census]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]])
  (:import
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute)))

(def repo-root (.getCanonicalPath (io/file ".")))
(def fixture "test-fixtures/relation-census/folds.clj")
(def second-fixture "test-fixtures/relation-census/inventory_folds.clj")
(def helpers "test-fixtures/relation-census/helpers_only.clj")

(defn- temp-dir
  []
  (.toFile (Files/createTempDirectory
             "clj-surgeon-census-test"
             (make-array FileAttribute 0))))

(defn- delete-tree!
  [file]
  (when (.exists file)
    (doseq [child (reverse (file-seq file))]
      (.delete child))))

(defn- run
  [params]
  (census-tool/execute-request! {:project-root repo-root} params))

(defn- fixture-receipt
  ([] (fixture-receipt {}))
  ([extra] (run (merge {:files [fixture]} extra))))

;; @spec MCP-OP-CENSUS-009
;; @spec MCP-OP-CENSUS-013
(deftest censuses-the-real-bytes-fixture-through-the-tool
  (let [result (fixture-receipt)]
    (is (true? (:ok result)))
    (is (= "relation-census" (:operation result)))
    (is (true? (:read_complete result)))
    (is (= 1 (:census_version result)))
    (is (= 1 (:files result)))
    (is (= 9 (:arms result)))
    (is (= 7 (:sites result)))
    (is (= 3 (:outside_arms result)))
    (is (= {:door 2 :set 1 :guarded 1 :raw 1 :unknown 2} (:counts result)))

    (testing "the receipt lists the raw site with its evidence"
      (let [raw (first (:raw result))]
        (is (= 1 (count (:raw result))))
        (is (= "event.speaker-announced" (:arm raw)))
        (is (= fixture (:file raw)))
        (is (= 125 (:line raw)))
        (is (= "(fnil conj [])" (:write raw)))
        (is (= "state [:events slug :settings :announced-speakers]" (:target raw)))
        (is (nil? (:reason raw)))))

    (testing "the receipt lists the guarded site with its guard line"
      (let [guarded (first (:guarded result))]
        (is (= "task.chase-recorded" (:arm guarded)))
        (is (= 78 (:guard_line guarded)))
        (is (= ":chase-id" (:identity guarded)))
        (is (= "absent" (:polarity guarded)))
        (is (str/starts-with? (:guard guarded) "(if (and (get-in state [:tasks k])"))))

    (testing "the receipt lists every unknown site with its reason"
      (is (= #{"helper-mediated-guard" "polarity"}
             (set (map :reason (:unknown result)))))
      (is (= "reminder-already-logged?"
             (:detail (first (filter #(= "helper-mediated-guard" (:reason %))
                                     (:unknown result)))))))

    (testing "per-file counts, phase timings and next_action"
      (is (= {:door 2 :set 1 :guarded 1 :raw 1 :unknown 2 :arms 9 :sites 7}
             (get (:by_file result) fixture)))
      (is (= #{:read :classify :merge}
             (set (keys (:phases_elapsed_ms result))))
          "an explicit file list discovers nothing, so no discover phase is claimed")
      (is (every? number? (vals (:phases_elapsed_ms result))))
      (is (every? #(not (neg? %)) (vals (:phases_elapsed_ms result))))
      (is (str/starts-with? (:next_action result) "review the raw sites")))

    (testing "the published receipt is bounded and carries only bounded excerpts"
      (let [bytes (count (.getBytes (json/generate-string result) "UTF-8"))]
        (is (<= bytes 4096) (str "receipt was " bytes " bytes")))
      (is (not (str/includes? (json/generate-string result) "defmethod fold-event"))))))

;; @spec MCP-OP-CENSUS-010
;; @spec MCP-OP-CENSUS-011
(deftest pool-size-one-and-pool-size-n-agree-byte-for-byte
  (let [strip #(-> % (dissoc :elapsed_ms :phases_elapsed_ms :pool_size))
        all [fixture second-fixture helpers]
        serial (run {:files all :pool_size 1})
        parallel (run {:files all :pool_size 8})
        reversed (run {:files (vec (reverse all)) :pool_size 8})]
    (is (= 1 (:pool_size serial)))
    (is (= 8 (:pool_size parallel)))
    (is (= (json/generate-string (strip serial))
           (json/generate-string (strip parallel)))
        "parallelism changes elapsed time, never the answer")
    (is (= (json/generate-string (strip serial))
           (json/generate-string (strip reversed)))
        "the merge re-keys by path, so request order cannot reorder the census")
    (is (= 2 (:files serial))
        "both arm-defining fixtures went through the pool; the helper did not")
    (is (= [fixture second-fixture] (vec (keys (:by_file serial))))
        "the merge orders by path regardless of the order the pool finished in")))

;; @spec MCP-OP-CENSUS-014
(deftest refuses-with-a-typed-reason-and-an-executable-next-call
  (testing "a workspace root that is not an existing absolute directory"
    (let [result (run {:workspace_root "relative/nope"})]
      (is (false? (:ok result)))
      (is (= "invalid-workspace-root" (:error_type result)))))

  (testing "no file defines fold arms, and the scan is named explicitly"
    ;; The caller named `helpers` itself; it was scanned and has no fold arms,
    ;; so replaying the identical `files` list is not a narrower call. No
    ;; `next_call`, and a remedy naming the file(s) scanned instead.
    (let [result (run {:files [helpers]})]
      (is (false? (:ok result)))
      (is (= "no-fold-arms-found" (:error_type result)))
      (is (= [helpers] (:scanned result)))
      (is (= 1 (:files_scanned result)))
      (is (nil? (:counts result)))
      (is (not (contains? result :next_call))
          "the caller's own file list, already refused, is not a continuation")
      (is (string? (:remedy result)))
      (is (str/includes? (:remedy result) helpers))))

  (testing "an unknown door symbol is named alongside the known doors"
    (let [result (run {:files [fixture] :doors ["conj-once" "made-up-door"]})]
      (is (false? (:ok result)))
      (is (= "unknown-door-symbol" (:error_type result)))
      (is (= "made-up-door" (:door result)))
      (is (str/includes? (:error result) "not defined in any scanned file"))
      (is (contains? (set (:known_doors result)) "upsert-by"))
      (is (some? (get-in result [:next_call :doors]))))
    (let [result (run {:files [fixture] :doors ["conj"]})]
      (is (false? (:ok result)))
      (is (str/includes? (:error result) "shadows a collection write head"))))

  (testing "a door defined in a scanned file is accepted"
    (let [result (run {:files [fixture] :doors ["upsert-by" "submission-speaker-identity"]})]
      (is (true? (:ok result)))))

  (testing "an unparseable file refuses and names itself"
    (let [root (temp-dir)
          broken (io/file root "src/app/folds.clj")]
      (try
        (.mkdirs (.getParentFile broken))
        (spit broken "(defmethod fold-event \"a\" [state event] (conj")
        (let [result (census-tool/execute-request!
                       {:project-root (.getPath root)} {})]
          (is (false? (:ok result)))
          (is (= "unparseable-file" (:error_type result)))
          (is (= "src/app/folds.clj" (:file result)))
          (is (nil? (:counts result))))
        (finally (delete-tree! root)))))

  (testing "a path outside the project refuses before any read"
    (let [result (run {:files ["../etc/passwd.clj"]})]
      (is (false? (:ok result)))
      (is (= "unreadable-source-path" (:error_type result))))))

;; @spec MCP-OP-CENSUS-015
(deftest the-tool-is-read-only-and-says-what-it-is-not
  (is (true? (:read-only census-tool/census-annotations)))
  (is (false? (:destructive census-tool/census-annotations)))
  (is (str/includes? census-tool/census-tool-description "not an enforcement gate"))
  (is (str/includes? census-tool/census-tool-description
                     "never claims to prove idempotency"))
  (is (= "relation_census" (:name census-tool/relation-census-tool)))
  (is (= #'census-tool/handle-relation-census
         (:tool-fn census-tool/relation-census-tool)))
  (is (= {:type "number" :minimum 0}
         (get-in census-tool/census-output-schema [:properties "elapsed_ms"])))
  (is (false? (:additionalProperties census-tool/census-tool-schema))))

;; @spec MCP-OP-CENSUS-010
(deftest discovery-finds-the-fixture-without-an-explicit-file-list
  (let [result (run {})]
    (is (true? (:ok result)))
    (is (pos? (:files_scanned result)))
    (is (contains? (set (keys (:by_file result))) fixture))
    (is (pos? (get-in result [:counts :raw])))))

;; @spec MCP-OP-CENSUS-001
(deftest the-default-doors-are-the-documented-set
  (is (= #{"conj-once" "cons-once" "upsert-by" "conj-distinct-by" "cons-distinct-by"}
         (set (map str census/default-doors)))))

;; @spec MCP-OP-CENSUS-016
(deftest bounds-pool-size-and-file-count-server-side
  (testing "pool_size 0 refuses typed instead of failing inside the adapter"
    (let [result (run {:files [fixture] :pool_size 0})]
      (is (false? (:ok result)))
      (is (= "invalid-mcp-request" (:error_type result)))
      (is (= "pool-size-out-of-range" (:reason result)))
      (is (nil? (:counts result)))
      (is (= "relation_census" (get-in result [:next_call :tool])))
      (is (= 8 (get-in result [:next_call :pool_size])))))

  (testing "pool_size 4096 refuses instead of starting 4096 platform threads"
    (let [result (run {:files [fixture] :pool_size 4096})]
      (is (false? (:ok result)))
      (is (= "invalid-mcp-request" (:error_type result)))
      (is (= "pool-size-out-of-range" (:reason result)))
      (is (= 64 (:maximum result)))))

  (testing "a non-integer pool_size refuses typed"
    (let [result (run {:files [fixture] :pool_size "many"})]
      (is (false? (:ok result)))
      (is (= "pool-size-not-an-integer" (:reason result)))))

  (testing "a file list beyond the advertised maximum refuses before any read"
    (let [result (run {:files (vec (repeat 513 fixture))})]
      (is (false? (:ok result)))
      (is (= "invalid-mcp-request" (:error_type result)))
      (is (= "too-many-files" (:reason result)))
      (is (= 512 (:maximum result)))
      (is (= 513 (:actual result)))))

  (testing "an empty file list refuses rather than silently censusing the tree"
    (let [result (run {:files []})]
      (is (false? (:ok result)))
      (is (= "empty-file-list" (:reason result)))))

  (testing "an unknown field refuses"
    (let [result (run {:files [fixture] :threads 8})]
      (is (false? (:ok result)))
      (is (= "unknown-fields" (:reason result)))
      (is (= ["threads"] (:unknown result)))))

  (testing "the accepted list names every field the tool accepts"
    ;; Sol's round-nine item 6. The accepted list is what a caller retries
    ;; with, so a field missing from it reads as a field the tool rejects —
    ;; and workspace_root is not only accepted, it is the field that decides
    ;; which tree gets censused. The authority is the published schema, not
    ;; a hand-kept list, so the assertion compares against the schema's own
    ;; properties.
    (let [result (run {:files [fixture] :threads 8})]
      (is (= (vec (sort (keys (:properties census-tool/census-tool-schema))))
             (:accepted result))
          (str "the refusal advertises " (pr-str (:accepted result))
               " but the tool accepts "
               (pr-str (vec (sort (keys (:properties
                                          census-tool/census-tool-schema)))))))
      (is (some #{"workspace_root"} (:accepted result))
          "the accepted list omits the routing field the caller may supply")))

  (testing "the effective pool is capped at the box and the receipt says so"
    (let [result (run {:files [fixture] :pool_size 64})
          effective (min 64 (.availableProcessors (Runtime/getRuntime)))]
      (is (true? (:ok result)))
      (is (= effective (:pool_size result)))
      (is (<= (:pool_size result) (.availableProcessors (Runtime/getRuntime))))
      (if (< effective 64)
        (is (= 64 (:pool_size_requested result)))
        (is (nil? (:pool_size_requested result)))))))

;; @spec MCP-OP-CENSUS-029
(deftest doors-accepts-only-the-json-strings-the-schema-advertises
  (testing "the advertised schema is the one being enforced"
    (is (= {:type "array" :items {:type "string"} :maxItems 32}
           (get-in census-tool/census-tool-schema [:properties "doors"]))))

  (doseq [value [1 1.5 true nil {} []]]
    (testing (str "doors entry " (pr-str value) " is not a JSON string")
      (let [result (run {:files [fixture] :doors [value]})]
        (is (false? (:ok result))
            (str "the wire accepted a doors entry " (pr-str value)
                 " though the advertised schema requires a string"))
        (is (= "invalid-mcp-request" (:error_type result)))
        (is (= "doors-not-strings" (:reason result)))
        (is (= 0 (:index result)))
        (is (= value (:value result)))
        (is (nil? (:counts result)))
        (is (= "relation_census" (get-in result [:next_call :tool])))
        (is (not (contains? (:next_call result) :doors))
            (str "the offending doors list must not be copied into the "
                 "continuation: " (pr-str (:next_call result)))))))

  (testing "a later offending entry is named by its own index"
    (let [result (run {:files [fixture] :doors ["upsert-by" 2]})]
      (is (= "doors-not-strings" (:reason result)))
      (is (= 1 (:index result)))
      (is (= 2 (:value result)))))

  (testing "a JSON string door still censuses"
    (let [result (run {:files [fixture] :doors ["upsert-by"]})]
      (is (true? (:ok result))))))

;; @spec MCP-OP-CENSUS-029
(deftest pool-size-accepts-only-the-json-integer-the-schema-advertises
  (testing "the advertised schema is the one being enforced"
    (is (= {:type "integer" :minimum 1 :maximum 64}
           (get-in census-tool/census-tool-schema [:properties "pool_size"]))))

  (doseq [value ["8" 8.0 true nil []]]
    (testing (str "pool_size " (pr-str value) " is not a JSON integer")
      (let [result (run {:files [fixture] :pool_size value})]
        (is (false? (:ok result))
            (str "the wire accepted pool_size " (pr-str value)
                 " though the advertised schema requires an integer"))
        (is (= "invalid-mcp-request" (:error_type result)))
        (is (= "pool-size-not-an-integer" (:reason result)))
        (is (= census/max-pool-size (:maximum result)))
        (is (nil? (:counts result)))
        (is (= "relation_census" (get-in result [:next_call :tool]))))))

  (testing "a JSON integer inside the advertised range still censuses"
    (let [result (run {:files [fixture] :pool_size 2})]
      (is (true? (:ok result)))
      (is (= (min 2 (.availableProcessors (Runtime/getRuntime)))
             (:pool_size result))))))

;; @spec MCP-OP-CENSUS-017
(deftest reads-are-bounded-and-exhaustion-is-a-typed-refusal
  (testing "an oversized requested file refuses typed instead of being slurped"
    (let [root (temp-dir)
          big (io/file root "src/app/huge.clj")]
      (try
        (.mkdirs (.getParentFile big))
        (spit big (str "(defmethod fold-event \"a\" [state event] state)\n"
                       (apply str (repeat (inc census/max-source-bytes) \;))))
        (let [result (census-tool/execute-request!
                       {:project-root (.getPath root)}
                       {:files ["src/app/huge.clj"]})]
          (is (false? (:ok result)))
          (is (= "source-too-large" (:error_type result)))
          (is (= "src/app/huge.clj" (:file result)))
          (is (= census/max-source-bytes (:maximum result)))
          (is (nil? (:counts result)))
          ;; This assertion used to require a next_call, which the refusal
          ;; satisfied with `files ["<a source under the byte cap>"]` — a
          ;; caption in an argument position. The only source this request
          ;; named is the oversized one, so the request minus it is not a
          ;; request: MCP-OP-CENSUS-014 requires no next_call and a remedy.
          (is (not (contains? result :next_call))
              "the refusal still hands back a call it cannot compute")
          (is (string? (:remedy result)))
          (is (= ["src/app/huge.clj"] (:files_removed result))))
        (finally (delete-tree! root)))))

  (testing "a scanned source that defines no arms is read and then dropped"
    (let [root (mcp-paths/real-root repo-root)
          collected (census-tool/collect-inputs root [helpers fixture] {})]
      (is (= 2 (:read collected)) "both files were read")
      (is (= [fixture] (mapv :file (:inputs collected)))
          "only the arm-defining source is retained")
      (is (every? string? (map :source (:inputs collected))))))

  (testing "an out-of-memory inside the census is a typed refusal, not a crash"
    (with-redefs [census-tool/collect-inputs
                  (fn [& _] (throw (OutOfMemoryError. "Java heap space")))]
      (let [result (run {:files [fixture]})]
        (is (false? (:ok result)))
        (is (= "census-resource-exhausted" (:error_type result)))
        (is (nil? (:counts result)))
        ;; This assertion used to require a next_call, which the refusal
        ;; satisfied with `files ["<a narrower file list>"]` — a caption in an
        ;; argument position. After an exhaustion the walk's aggregates are
        ;; gone with the heap that held them, so there is no narrower call to
        ;; compute: MCP-OP-CENSUS-017 now requires a remedy and no next_call,
        ;; and `an-exhausted-census-offers-no-placeholder-continuation` is the
        ;; witness for it.
        (is (not (contains? result :next_call)))
        (is (string? (:remedy result))))))

  (testing "any other Throwable is typed too"
    (with-redefs [census-tool/collect-inputs
                  (fn [& _] (throw (AssertionError. "boom")))]
      (let [result (run {:files [fixture]})]
        (is (false? (:ok result)))
        (is (= "census-adapter-failure" (:error_type result)))))))

(def ^:private arm-source
  "(defmethod fold-event \"e\" [state event]\n  (update state :xs conj (:x event)))\n")

(defn- spit-file!
  [file text]
  (.mkdirs (.getParentFile ^java.io.File file))
  (spit file text))

(defn- deny-reads!
  "Make `file` unreadable to every user, returning it."
  ^java.io.File [^java.io.File file]
  (.setReadable file false false)
  file)

(defn- allow-reads!
  "Restore owner-readable permissions on `file`, whatever the test did to it."
  [^java.io.File file]
  (when (.exists file)
    (.setReadable file true true)))

(defn- deny-traversal!
  "chmod 000 a DIRECTORY: neither listable nor traversable, for every user.

   `deny-reads!` clears only the read bit, which leaves a directory
   traversable — a file inside it still opens, so it does not reproduce the
   denied-PARENT shape at all."
  ^java.io.File [^java.io.File dir]
  (.setExecutable dir false false)
  (.setReadable dir false false)
  dir)

(defn- allow-traversal!
  "Restore owner read+execute on a directory, whatever the test did to it."
  [^java.io.File dir]
  (when (.exists dir)
    (.setExecutable dir true true)
    (.setReadable dir true true)))

;; @spec MCP-OP-CENSUS-018
(deftest discovery-prunes-skipped-directories-and-skips-escaping-paths
  (let [parent (temp-dir)
        root (io/file parent "project")
        outside (io/file parent "outside")]
    (try
      (spit-file! (io/file root "src/app/folds.clj") arm-source)
      ;; arms inside a skipped directory: the walk must never descend here
      (spit-file! (io/file root ".git/hooks/folds.clj") arm-source)
      ;; the shape that made the tool unusable: dev/checkouts/foo -> ../../outside
      (spit-file! (io/file outside "src/other/folds.clj") arm-source)
      (.mkdirs (io/file root "dev/checkouts"))
      (Files/createSymbolicLink (.toPath (io/file root "dev/checkouts/foo"))
                                (.toPath outside)
                                (make-array FileAttribute 0))
      (let [result (census-tool/execute-request!
                     {:project-root (.getPath root)} {})]
        (is (true? (:ok result)) (str "census refused: " (:error result)))
        (is (= 1 (:files result)))
        (is (= ["src/app/folds.clj"] (vec (keys (:by_file result))))
            "the .git arms were never visited and the symlink was never followed")
        (is (= 1 (:files_scanned result)))
        (is (= 1 (:skipped_outside_root result))
            "the escaping path is counted, not fatal")
        (is (= 1 (get-in result [:counts :raw]))))
      (finally (delete-tree! parent)))))

(def ^:private filler-source
  "(ns app.filler)\n(def x 1)\n")

(defn- build-candidate-tree!
  "A workspace holding exactly `n` candidate Clojure sources, one with arms."
  [root n]
  (spit-file! (io/file root "src/app/folds.clj") arm-source)
  (doseq [i (range (dec n))]
    (spit-file! (io/file root (format "src/filler/d%02d/f%04d.clj" (quot i 100) i))
                filler-source)))

;; @spec MCP-OP-CENSUS-027
(deftest discovery-refuses-at-the-scanned-file-ceiling-instead-of-truncating
  (let [root (temp-dir)]
    (try
      (build-candidate-tree! root census/max-scanned-files)
      (testing "exactly the ceiling is censused and completion is claimed"
        (let [result (census-tool/execute-request!
                       {:project-root (.getPath root)} {})]
          (is (true? (:ok result))
              (str "the census refused AT the ceiling: " (:error result)))
          (is (true? (:read_complete result)))
          (is (= census/max-scanned-files (:files_scanned result)))
          (is (= 1 (:files result)))))

      (testing "one candidate past the ceiling refuses typed before any read"
        (spit-file! (io/file root "src/filler/one-too-many.clj") filler-source)
        (let [result (census-tool/execute-request!
                       {:project-root (.getPath root)} {})]
          (is (false? (:ok result))
              "a tree over the ceiling was censused as if it were complete")
          (is (= "too-many-candidate-files" (:error_type result)))
          (is (false? (:read_complete result)))
          (is (= 0 (:files_read result)))
          (is (= census/max-scanned-files (:maximum result)))
          (is (= census/max-scanned-files (:fits result)))
          (is (= (inc census/max-scanned-files) (:observed result)))
          (is (true? (:observed_at_least result)))
          (is (nil? (:counts result)))
          (is (nil? (:by_file result)))
          (is (= "relation_census" (get-in result [:next_call :tool])))
          ;; This assertion used to require a non-empty `files`, which the
          ;; refusal satisfied with the literal string
          ;; "<at most 4000 named sources under this root>" — a caption in an
          ;; argument position. The promise is now a narrowed root the caller
          ;; can replay, so the caption must be gone.
          (is (nil? (get-in result [:next_call :files]))
              "the continuation still carries a placeholder file list")
          (is (str/starts-with? (str (get-in result [:next_call :workspace_root]))
                                (str (.getCanonicalPath root) "/"))
              "the continuation hands back the root that was just refused")))
      (finally (delete-tree! root)))))

(defn- padded-source
  "An arm-defining source of exactly `bytes` bytes, padded with a comment."
  [bytes]
  (str arm-source
       (apply str (repeat (- bytes (count arm-source) 1) \;))
       "\n"))

;; @spec MCP-OP-CENSUS-028
(deftest a-source-over-the-byte-cap-is-named-and-completion-is-not-claimed
  (let [root (temp-dir)]
    (try
      (spit-file! (io/file root "src/app/folds.clj") arm-source)
      (spit-file! (io/file root "src/app/at_cap.clj")
                  (padded-source census/max-source-bytes))

      (testing "a source of exactly max-source-bytes is read and censused"
        (let [result (census-tool/execute-request!
                       {:project-root (.getPath root)} {})]
          (is (true? (:ok result)) (str "refused: " (:error result)))
          (is (true? (:read_complete result)))
          (is (nil? (:oversized_skipped result)))
          (is (nil? (:oversized_skipped_omitted result))
              "a census that skipped nothing has nothing to omit")
          (is (= 2 (:files result)))
          (is (contains? (set (keys (:by_file result))) "src/app/at_cap.clj")
              "the source at the cap was not censused")))

      (testing "one byte over the cap is named and completion is withheld"
        (spit-file! (io/file root "src/app/over_cap.clj")
                    (padded-source (inc census/max-source-bytes)))
        (let [result (census-tool/execute-request!
                       {:project-root (.getPath root)} {})]
          (is (true? (:ok result)) (str "refused: " (:error result)))
          (is (false? (:read_complete result))
              "a census that silently dropped a source claimed completion")
          (is (= 1 (get-in result [:oversized_skipped :count])))
          (is (= ["src/app/over_cap.clj"]
                 (get-in result [:oversized_skipped :files])))
          (is (= 0 (:oversized_skipped_omitted result))
              "a complete list must SAY it is complete, not leave it inferred")
          (is (= 2 (:files result)) "the skipped source was censused anyway")))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-CENSUS-030
(deftest a-source-named-many-times-is-read-and-counted-once
  (testing "512 copies of one path are one file, not 512"
    (let [result (run {:files (vec (repeat census/max-requested-files fixture))})]
      (is (true? (:ok result)) (str "refused: " (:error result)))
      (is (= 1 (:files result))
          "a repeated path multiplied every count in the receipt")
      (is (= 9 (:arms result)))
      (is (= 7 (:sites result)))
      (is (= {:door 2 :set 1 :guarded 1 :raw 1 :unknown 2} (:counts result)))
      (is (= (dec census/max-requested-files) (:duplicates_collapsed result)))
      (is (= 1 (:files_scanned result)))
      (is (= [fixture] (vec (keys (:by_file result)))))))

  (testing "two strings that canonicalise to one real path collapse too"
    (let [root (temp-dir)]
      (try
        (spit-file! (io/file root "src/app/folds.clj") arm-source)
        (Files/createSymbolicLink (.toPath (io/file root "src/alias"))
                                  (.toPath (io/file "app"))
                                  (make-array FileAttribute 0))
        (let [result (census-tool/execute-request!
                       {:project-root (.getPath root)}
                       {:files ["src/app/folds.clj" "src/alias/folds.clj"]})]
          (is (true? (:ok result)) (str "refused: " (:error result)))
          (is (= 1 (:files result))
              "one real file was censused twice under two names")
          (is (= 1 (:duplicates_collapsed result)))
          (is (= 1 (get-in result [:counts :raw]))))
        (finally (delete-tree! root)))))

  (testing "a census with no repeated path says nothing about duplicates"
    (let [result (run {:files [fixture second-fixture]})]
      (is (= 2 (:files result)))
      (is (nil? (:duplicates_collapsed result))))))

;; @spec MCP-OP-CENSUS-021
(deftest the-cli-plan-pool-is-the-bounded-pool-on-the-jvm
  (testing "a threads request above one runs on census_pool, not core pmap"
    (let [{:keys [map-fn pool-size]} (core/census-plan-pool 4)
          threads (atom #{})]
      (is (= 4 pool-size))
      (is (not= map map-fn) "the CLI selected the serial map for a pooled run")
      (map-fn (fn [x] (swap! threads conj (Thread/currentThread)) x) (range 40))
      (is (<= (count @threads) 4))
      (is (> (count @threads) 1) "nothing actually ran on the pool")))

  (testing "no request means no pool"
    (let [{:keys [map-fn pool-size]} (core/census-plan-pool nil)]
      (is (= 1 pool-size))
      (is (= map map-fn))))

  (testing "a request above the box is capped to the pool that ran"
    (let [{:keys [pool-size]} (core/census-plan-pool 64)]
      (is (= (census/effective-pool-size 64) pool-size))
      (is (<= pool-size (.availableProcessors (Runtime/getRuntime)))))))

(defn- padded-path
  "A project-relative source path of exactly `width` characters."
  [width i]
  (let [head "src/"
        tail (format "/f%03d.clj" i)
        fill (- width (count head) (count tail))]
    (str head (apply str (repeat fill \a)) tail)))

;; @spec MCP-OP-CENSUS-022
(deftest the-published-receipt-holds-its-byte-budget-with-long-paths
  (let [root (temp-dir)]
    (try
      (doseq [i (range 14)]
        (let [path (padded-path 231 i)]
          (is (= 231 (count path)))
          (spit-file! (io/file root path) arm-source)))
      (let [result (census-tool/execute-request!
                     {:project-root (.getPath root)} {})
            bytes (count (.getBytes (json/generate-string result) "UTF-8"))]
        (is (true? (:ok result)) (str "census refused: " (:error result)))
        (is (= 14 (:files result)))
        (is (<= bytes 4096)
            (str "the published receipt was " bytes " bytes"))
        (is (true? (:receipt_truncated result))
            "a trimmed receipt must say it was trimmed")
        (is (pos? (get-in result [:counts :raw]))
            "trimming evidence must not touch the counts"))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-CENSUS-023
(deftest the-phases-are-the-phases-that-actually-ran
  (testing "without doors nothing is parsed twice"
    (let [calls (atom 0)
          original census/census-file]
      (with-redefs [census/census-file (fn [m] (swap! calls inc) (original m))]
        (let [result (run {:files [fixture second-fixture]})]
          (is (true? (:ok result)))
          (is (= 2 @calls)
              "every arm file was parsed and classified more than once")))))

  (testing "a census that discovered nothing reports no discover phase"
    (let [result (run {:files [fixture]})]
      (is (= #{:read :classify :merge}
             (set (keys (:phases_elapsed_ms result)))))
      (is (nil? (get-in result [:phases_elapsed_ms :parse]))
          "the parse phase was a second serial census, not a parse")))

  (testing "a census that walked the tree reports what the walk cost"
    (let [result (run {})]
      (is (= #{:discover :read :classify :merge}
             (set (keys (:phases_elapsed_ms result)))))
      (is (every? #(and (number? %) (not (neg? %)))
                  (vals (:phases_elapsed_ms result))))))

  (testing "with doors, door confirmation still refuses an undefined door"
    (let [result (run {:files [fixture] :doors ["made-up-door"]})]
      (is (false? (:ok result)))
      (is (= "unknown-door-symbol" (:error_type result))))))

;; @spec MCP-OP-CENSUS-025
(deftest a-write-behind-an-unmodelled-helper-is-not-a-clean-bill
  (let [root (temp-dir)]
    (try
      (spit-file!
        (io/file root "src/app/folds.clj")
        (str "(ns app.folds)\n"
             "(defn- record-event [state x]\n"
             "  (update state :xs conj x))\n"
             "(defmethod fold-event \"e\" [state event]\n"
             "  (record-event state (:x event)))\n"))
      (let [result (census-tool/execute-request!
                     {:project-root (.getPath root)} {})]
        (is (true? (:ok result)))
        (is (= 0 (get-in result [:counts :raw]))
            "the write is behind a helper, so the census finds no site")
        (is (= 0 (:sites result)))
        (testing "the receipt says which calls it could not see through"
          (is (= 1 (get-in result [:unrecognised_calls :count])))
          (is (= ["record-event"]
                 (mapv :call (get-in result [:unrecognised_calls :examples]))))
          (is (= "src/app/folds.clj"
                 (:file (first (get-in result [:unrecognised_calls :examples])))))
          (is (= "e" (:arm (first (get-in result [:unrecognised_calls :examples]))))))
        (testing "next_action refuses to read as a clean bill of health"
          (is (not= "none" (:next_action result)))
          (is (str/includes? (:next_action result) "record-event"))
          (is (str/includes? (:next_action result) "not modelled"))))
      (finally (delete-tree! root))))

  (testing "modelled vocabulary is not reported as unmodelled"
    (let [root (temp-dir)]
      (try
        (spit-file!
          (io/file root "src/app/folds.clj")
          (str "(ns app.folds)\n"
               "(defmethod fold-event \"e\" [state event]\n"
               "  (if (get-in state [:seen (:id event)])\n"
               "    state\n"
               "    (update-in state [:xs] (fnil conj []) (:x event))))\n"))
        (let [result (census-tool/execute-request!
                       {:project-root (.getPath root)} {})]
          (is (true? (:ok result)))
          (is (nil? (:unrecognised_calls result))
              "if, get-in, update-in and fnil are all modelled"))
        (finally (delete-tree! root)))))

  (testing "a raw site still outranks unmodelled calls in next_action"
    (let [result (run {:files [fixture]})]
      (is (str/starts-with? (:next_action result) "review the raw sites")))))

;; @spec MCP-OP-CENSUS-024
(deftest a-door-defined-in-a-file-with-no-arms-is-accepted
  (testing "the branch's own fixture shape: the door lives in a helpers ns"
    (let [result (run {:files [fixture helpers]
                       :doors ["conj-once" "record-window"]})]
      (is (true? (:ok result))
          (str "refused a door defined in a scanned helper file: "
               (:error result)))
      (is (= 1 (:files result)) "the helper file still defines no arms")))

  (testing "discovery collects declarations from every scanned file too"
    (let [root (temp-dir)]
      (try
        (spit-file! (io/file root "src/app/folds.clj") arm-source)
        (spit-file! (io/file root "src/app/helpers.clj")
                    "(ns app.helpers)\n(defn keep-once [coll x] (conj coll x))\n")
        (let [result (census-tool/execute-request!
                       {:project-root (.getPath root)} {:doors ["keep-once"]})]
          (is (true? (:ok result)) (str "refused: " (:error result))))
        (finally (delete-tree! root)))))

  (testing "a door defined nowhere is still refused"
    (let [result (run {:files [fixture helpers] :doors ["nowhere-door"]})]
      (is (false? (:ok result)))
      (is (= "unknown-door-symbol" (:error_type result)))
      (is (= "nowhere-door" (:door result))))))

(def ^:private design-doc
  "docs/intent/relation-census/relation-census-design.md")

;; @spec MCP-OP-CENSUS-031
(deftest the-low-level-design-names-the-phases-the-implementation-runs
  (let [design (slurp design-doc)
        mentions? (fn [needle] (boolean (str/includes? design needle)))
        walked (run {})
        named (run {:files [fixture]})
        phases (set (concat (keys (:phases_elapsed_ms walked))
                            (keys (:phases_elapsed_ms named))))]
    (is (= #{:discover :read :classify :merge} phases)
        "the implementation's phase set changed; the design must follow it")

    (testing "every phase a receipt publishes is named in the design"
      (is (= #{} (into #{} (remove #(mentions? (str "`" (name %) "`"))) phases))
          "the design does not name these phases the census publishes"))

    (testing "the design promises no phase the census does not run"
      (is (false? (mentions? "`parse`"))
          "the design still promises a parse phase")
      (is (false? (mentions? ":parse"))
          "the design still lists a :parse timing"))

    (testing "the design names one discovery kernel, not two walks"
      (is (false? (mentions? "`fs/walk-file-tree` in the CLI"))
          "the design still describes a second walk the CLI no longer has")
      (is (true? (mentions? "census-discovery"))
          "the design never names the kernel both entrances call")
      (is (true? (mentions? "canonical"))
          "the design never says the root is canonicalised before the walk")
      (is (true? (mentions? "duplicates_collapsed"))
          "the design never says two paths onto one source collapse")
      (is (true? (mentions? "oversized_skipped_omitted"))
          "the design never says a truncated skip list names its omission"))

    (testing "the design states which pool each entrance actually uses"
      (is (false? (mentions? "only the MCP route carries the claypoole pool"))
          "the design still says the CLI has no pool")
      (is (true? (mentions? "census_pool"))
          "the design never names the pool the JVM CLI runs on")
      (is (true? (mentions? "pool_size_requested"))
          "the design never says the receipt reports the pool it asked for"))))

;; @spec MCP-OP-CENSUS-026
(deftest the-published-surfaces-name-the-fifth-tool
  (let [text (fn [path] (str/replace (slurp path) #"\s+" " "))
        readme (text "README.md")
        claude (text "CLAUDE.md")
        skill (text "skills/clj-surgeon/references/mcp-advanced.md")
        mirror (text ".claude/skills/clj-surgeon/references/mcp-advanced.md")
        contract (text "docs/intent/mcp-operation-contract/mcp-operation-contract-specs.md")]
    (testing "the tool count is five"
      (is (str/includes? readme "registers exactly five clj-surgeon tools"))
      (is (not (str/includes? readme "exactly four clj-surgeon tools")))
      (is (str/includes? contract "today's five tools"))
      (is (not (str/includes? contract "today's four tools"))))

    (testing "every published surface names the census"
      (doseq [[label body] [["README.md" readme]
                            ["CLAUDE.md" claude]
                            ["skill reference" skill]
                            ["skill mirror" mirror]]]
        (is (str/includes? body "relation_census")
            (str label " does not name relation_census"))))

    (testing "the surfaces say what the census is not, and that it enumerates"
      (is (str/includes? claude "not an enforcement gate"))
      (is (str/includes? readme "enumerates"))
      (is (str/includes? skill "enumerates the workspace tree"))
      (is (str/includes? census-tool/census-tool-description
                         "enumerates the workspace tree")
          "the tool's own description hides that it walks a tree"))))

;; ---------------------------------------------------------------------------
;; Entrance parity: the tool and BOTH CLIs discover through one kernel
;;
;; Every witness below runs three entrances over one fixture — the MCP tool,
;; the JVM CLI (`core/run-relation-census`, the op the JVM launcher dispatches
;; to), and the babashka CLI as a real subprocess — and asserts the same
;; discovery figures from all three. A discovery rule that lives in only one
;; entrance is exactly the class of defect these exist to refuse.
;; ---------------------------------------------------------------------------

(defn- bb-cli
  "Run the babashka CLI as a subprocess and read its EDN receipt."
  [& args]
  (let [{:keys [out err exit]}
        (apply proc/shell {:out :string :err :string :continue true}
               "bb" "-cp" (str repo-root "/src") "-m" "clj-surgeon.core" args)]
    (try
      (assoc (edn/read-string out) ::exit exit)
      (catch Exception _
        {::exit exit ::out out ::err err}))))

(defn- census-entrances
  "The census of one directory through all three entrances."
  [dir]
  {:mcp (census-tool/execute-request! {:project-root dir} {})
   :jvm-cli (core/run-relation-census {:dir dir})
   :bb-cli (bb-cli ":op" "relation-census" ":dir" dir)})

;; @spec MCP-OP-CENSUS-032
(deftest a-symlinked-workspace-root-is-canonicalised-by-every-entrance
  (let [parent (temp-dir)
        real (io/file parent "real")
        link (io/file parent "link")]
    (try
      (spit-file! (io/file real "src/app/folds.clj") arm-source)
      (Files/createSymbolicLink (.toPath link) (.toPath (io/file "real"))
                                (make-array FileAttribute 0))
      (let [{:keys [mcp jvm-cli bb-cli]} (census-entrances (.getPath link))]
        (testing "the tool canonicalises the root and censuses the one arm file"
          (is (true? (:ok mcp)) (str "refused: " (:error mcp)))
          (is (= 1 (:files mcp)))
          (is (= 1 (:files_scanned mcp))))

        (testing "the JVM CLI canonicalises the same root"
          (is (true? (:ok jvm-cli))
              (str "the JVM CLI walked a symlinked root as a file: "
                   (:error-type jvm-cli) " " (:error jvm-cli)))
          (is (= 1 (:files jvm-cli))))

        (testing "the babashka CLI canonicalises the same root"
          (is (true? (:ok bb-cli))
              (str "the babashka CLI walked a symlinked root as a file: "
                   (:error-type bb-cli) " " (:error bb-cli)))
          (is (= 1 (:files bb-cli)))))
      (finally (delete-tree! parent)))))

;; ---------------------------------------------------------------------------
;; Sol's round-eight finding: `doors=[1]` was refused only AFTER
;; `workspace/resolve-request` had already `stat`ed the workspace root
;; (`newfstatat` observed by isolated tracing, before the typed refusal).
;; These witnesses instrument the filesystem primitives each entrance's own
;; entrance code touches — the routing resolver's stat/realpath and the
;; source reader — and assert ZERO calls to either before a shape violation
;; refuses. The bb CLI runs as a real subprocess (`babashka.process`), so its
;; syscalls cannot be counted in-process; it is asserted for behavioural
;; parity only, alongside the JVM CLI witness that runs the identical op
;; function `core/run-relation-census` in-process and IS instrumented.
;;
;; READ THE NEXT TWO DEFTESTS BEFORE TRUSTING THIS ONE. Both counters below
;; are wrapped around functions the OP BODY calls. Neither can see what the
;; ENTRANCE does before it dispatches, and at the time this was written the
;; entrance was loading project aliases — stat, read, ancestor walk — on
;; requests it was about to refuse. This witness was green throughout. What
;; it proves is that `run-relation-census` touches nothing; the entrance is
;; proved by `the-cli-entrance-validates-the-request-shape-before-it-loads-
;; any-config` and `the-babashka-entrance-refuses-before-it-reads-the-
;; workspace-config`, and the bb block below remains a parity check, not a
;; meter.
;; ---------------------------------------------------------------------------

(defn- counting
  "Wrap `f` to count calls into `counter`, still calling the real `f`."
  [counter f]
  (fn [& args]
    (swap! counter inc)
    (apply f args)))

;; @spec MCP-OP-CENSUS-016
;; @spec MCP-OP-CENSUS-029
(deftest a-malformed-doors-entry-refuses-before-any-filesystem-call
  (testing "the MCP tool touches neither the workspace router's stat/realpath nor the source reader"
    (let [canonical-calls (atom 0)
          read-calls (atom 0)]
      (with-redefs [workspace/canonical-root
                    (counting canonical-calls workspace/canonical-root)
                    census-tool/collect-inputs
                    (counting read-calls census-tool/collect-inputs)]
        (let [result (census-tool/execute-request! {:project-root repo-root}
                                                    {:doors [1]})]
          (is (false? (:ok result)))
          (is (= "doors-not-strings" (:reason result)))
          (is (= 0 @canonical-calls)
              "the workspace router stat'ed/realpath'd the root before doors was validated")
          (is (= 0 @read-calls)
              "the source reader ran before doors was validated")))))

  (testing "the JVM CLI op touches neither its routing resolver nor its source reader"
    ;; :threads is the CLI's pool_size; there is no CLI-shaped equivalent of a
    ;; non-string `doors` entry (every CLI arg arrives as a string), so this
    ;; exercises the same MCP-OP-CENSUS-016 family — a type/bound violation
    ;; that must refuse before `core/census-root`/`core/census-sources` run.
    (let [canonical-calls (atom 0)
          read-calls (atom 0)]
      (with-redefs [core/census-root (counting canonical-calls core/census-root)
                    core/census-sources (counting read-calls core/census-sources)]
        (let [result (core/run-relation-census {:dir repo-root :threads "not-a-number"})]
          (is (false? (:ok result)))
          (is (= :invalid-pool-size (:error-type result)))
          (is (= 0 @canonical-calls)
              "the CLI's routing resolver ran before threads was validated")
          (is (= 0 @read-calls)
              "the CLI's source reader ran before threads was validated")))))

  (testing "the babashka CLI subprocess refuses the same shape violation (behavioural parity)"
    (let [result (bb-cli ":op" "relation-census" ":dir" repo-root
                         ":threads" "not-a-number")]
      (is (false? (:ok result)))
      (is (= :invalid-pool-size (:error-type result))))))

;; @spec MCP-OP-CENSUS-029
(deftest a-malformed-doors-entry-beats-too-many-files-and-an-invalid-workspace-root
  (testing "doors beats too-many-files"
    (let [result (run {:files (vec (repeat 513 fixture)) :doors [1]})]
      (is (false? (:ok result)))
      (is (= "invalid-mcp-request" (:error_type result)))
      (is (= "doors-not-strings" (:reason result))
          (str "too-many-files won instead: " (pr-str result)))))

  (testing "doors beats an unresolvable workspace_root"
    (let [result (run {:workspace_root "relative/nope" :doors [1]})]
      (is (false? (:ok result)))
      (is (= "invalid-mcp-request" (:error_type result)))
      (is (= "doors-not-strings" (:reason result))
          (str "invalid-workspace-root won instead: " (pr-str result))))))

;; ---------------------------------------------------------------------------
;; Sol's round-nine finding, and the blind spot in the witness above.
;;
;; The MCP entrance refused `doors=[1]` having made zero filesystem syscalls.
;; The babashka entrance did not: `bb … :threads not-a-number` returned
;; `invalid-pool-size` only AFTER it had `stat`ed the workspace, `stat`ed and
;; READ its `.clj-surgeon.edn`, and walked the ancestor chain looking for more.
;; That work is not in `run-relation-census` at all — it is in `core/run`, the
;; top-level dispatch, which loads project aliases BEFORE it dispatches to any
;; op.
;;
;; The witness above could never have seen it: it wraps `core/census-root` and
;; `core/census-sources`, functions the OP BODY calls, and the defect is one
;; frame above them, in the ENTRANCE. A witness blind to its own subject
;; returns a green that means nothing. These two count at the first filesystem
;; touch on the entrance itself.
;;
;; The bb CLI is a real subprocess, so an in-process counter cannot reach it.
;; Its meter here is the filesystem: a workspace whose `.clj-surgeon.edn` is
;; unparseable EDN, which `forms/read-config` THROWS on. A run that loads
;; config before it validates the request shape cannot return the shape
;; refusal — it returns the config error instead. That is a behavioural meter,
;; deterministic and portable, where an strace shim would be neither. (The
;; syscall count was taken by hand at both shas and reported with the round:
;; 3 filesystem syscalls naming the workspace before the refusal at 459f46e,
;; 0 after.)
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-CENSUS-016
;; @spec MCP-OP-CENSUS-019
;; @spec MCP-OP-CENSUS-029
(deftest the-cli-entrance-validates-the-request-shape-before-it-loads-any-config
  (testing "core/run — the dispatch babashka runs — touches nothing before refusing"
    (let [config-loads (atom 0)
          root-calls (atom 0)
          read-calls (atom 0)]
      (with-redefs [forms/init-from-file!
                    (counting config-loads forms/init-from-file!)
                    core/census-root (counting root-calls core/census-root)
                    core/census-sources (counting read-calls core/census-sources)]
        (let [result (binding [*out* (java.io.StringWriter.)]
                       (core/run {:op :relation-census
                                  :dir repo-root
                                  :threads "not-a-number"}))]
          (is (false? (:ok result)))
          (is (= :invalid-pool-size (:error-type result)))
          (is (= 0 @config-loads)
              (str "the CLI entrance resolved and read .clj-surgeon.edn "
                   "before it validated the request shape"))
          (is (= 0 @root-calls)
              "the CLI's routing resolver ran before threads was validated")
          (is (= 0 @read-calls)
              "the CLI's source reader ran before threads was validated")))))

  (testing "a valid request still loads the config it needs"
    ;; The ordering fix must not turn config loading OFF; it moves it after
    ;; the pure pass. A witness that only proves absence would pass a broken
    ;; entrance that never loads aliases at all.
    (let [config-loads (atom 0)]
      (with-redefs [forms/init-from-file!
                    (counting config-loads forms/init-from-file!)]
        (binding [*out* (java.io.StringWriter.)]
          (core/run {:op :relation-census :file (str repo-root "/" fixture)}))
        (is (= 1 @config-loads)
            "the entrance stopped loading project aliases for a valid request")))))

;; @spec MCP-OP-CENSUS-016
;; @spec MCP-OP-CENSUS-019
;; @spec MCP-OP-CENSUS-029
(deftest the-babashka-entrance-refuses-before-it-reads-the-workspace-config
  (let [root (temp-dir)]
    (try
      (spit-file! (io/file root "src/app/folds.clj") arm-source)
      ;; The trap: unparseable EDN. Reaching it at all is the defect.
      (spit (io/file root ".clj-surgeon.edn") "{:aliases {\"x\" }\n")

      (testing "a malformed :threads refuses on shape, not on the config it never read"
        (let [result (bb-cli ":op" "relation-census" ":dir" (.getPath root)
                             ":threads" "not-a-number")]
          (is (false? (:ok result))
              (str "the bb entrance did not refuse: " (pr-str result)))
          (is (= :invalid-pool-size (:error-type result))
              (str "the bb entrance loaded the workspace config before it "
                   "validated the request shape: " (pr-str result)))))

      (testing "a well-shaped request still reaches that config, and still trips it"
        ;; The same trap, proving the meter is live: a request whose shape is
        ;; valid DOES load the config, so the assertion above is about order,
        ;; not about the config having become unreachable.
        (let [result (bb-cli ":op" "relation-census" ":dir" (.getPath root))]
          (is (= :invalid-arguments (:error-type result))
              (str "the trap never fired, so the refusal above proves nothing: "
                   (pr-str result)))))
      (finally (delete-tree! root)))))

;; ---------------------------------------------------------------------------
;; Sol's round-ten review, item 4 (blocking): the CLI shape validator is
;; incomplete.
;;
;; Round ten made the CLI ENTRANCE validate its request shape before it loads
;; project aliases — for ONE field. `validate-cli-request-shape` destructured
;; `{:keys [threads]}` and read nothing else, so every other malformed shape
;; still travelled through `run-op`'s config load first: `bb … :doors conj`
;; against a workspace whose `.clj-surgeon.edn` is unparseable EDN returned
;; the EDN error, not the door refusal, having `stat`ed that workspace twice
;; and opened its config once (hand-run strace at 48c64ac: 3 filesystem
;; syscalls naming the workspace before the refusal).
;;
;; Sol also found `:format edn` and `:max-files 1` — arguments the
;; `:relation-census` op does not accept and that the MCP tool refuses as
;; unknown fields — accepted SILENTLY at the CLI. A silently ignored argument
;; is worse than a refused one: the caller believes a bound was applied that
;; never existed.
;;
;; The rule is the WHOLE shape, not one field of it. Every refusal shape the
;; pure pass can own must refuse with the entrance's own counter still at
;; zero.
;; ---------------------------------------------------------------------------

(def ^:private thirty-three-doors
  (str/join "," (map #(str "door-" %) (range (inc census/max-doors)))))

(def ^:private cli-shape-refusals
  "One malformed CLI request per refusal shape the pure pass owns.

   `:opts` is the request as an in-process opts map; `:args` is the same
   request as the argument strings babashka is handed. Both spellings must
   produce the same typed refusal, and both must produce it before the
   entrance touches the filesystem."
  [{:label :unknown-argument-format
    :opts {:format :edn} :args [":format" "edn"]
    :error-type :unknown-arguments}
   {:label :unknown-argument-max-files
    :opts {:max-files 1} :args [":max-files" "1"]
    :error-type :unknown-arguments}
   {:label :doors-not-a-string
    :opts {:doors [1]} :args [":doors" "[1]"]
    :error-type :doors-not-a-string}
   {:label :too-many-doors
    :opts {:doors thirty-three-doors} :args [":doors" thirty-three-doors]
    :error-type :too-many-doors}
   {:label :unknown-door-symbol
    :opts {:doors "conj"} :args [":doors" "conj"]
    :error-type :unknown-door-symbol}
   {:label :file-not-a-string
    :opts {:file ""} :args [":file" ""]
    :error-type :file-not-a-string}
   {:label :threads-not-an-integer
    :opts {:threads "not-a-number"} :args [":threads" "not-a-number"]
    :error-type :invalid-pool-size}
   {:label :threads-out-of-range
    :opts {:threads "0"} :args [":threads" "0"]
    :error-type :invalid-pool-size}])

;; @spec MCP-OP-CENSUS-016
;; @spec MCP-OP-CENSUS-019
;; @spec MCP-OP-CENSUS-029
(deftest the-cli-entrance-validates-every-field-before-it-loads-any-config
  (doseq [{:keys [label opts error-type]} cli-shape-refusals]
    (testing (str "the CLI entrance refuses " label " before it reads a byte")
      (let [config-loads (atom 0)
            root-calls (atom 0)
            read-calls (atom 0)]
        (with-redefs [forms/init-from-file!
                      (counting config-loads forms/init-from-file!)
                      core/census-root (counting root-calls core/census-root)
                      core/census-sources (counting read-calls core/census-sources)]
          (let [result (binding [*out* (java.io.StringWriter.)]
                         (core/run (merge {:op :relation-census :dir repo-root}
                                          opts)))]
            (is (false? (:ok result))
                (str label " was accepted: " (pr-str result)))
            (is (= error-type (:error-type result))
                (str label " refused as " (pr-str (:error-type result))
                     ": " (pr-str result)))
            (is (= 0 @config-loads)
                (str label ": the CLI entrance resolved and read "
                     ".clj-surgeon.edn before it validated the request shape"))
            (is (= 0 @root-calls)
                (str label ": the CLI's routing resolver ran before the "
                     "shape was validated"))
            (is (= 0 @read-calls)
                (str label ": the CLI's source reader ran before the shape "
                     "was validated"))))))))

;; @spec MCP-OP-CENSUS-016
;; @spec MCP-OP-CENSUS-019
;; @spec MCP-OP-CENSUS-029
(deftest the-babashka-entrance-refuses-every-malformed-field-before-the-config
  ;; The bb CLI is a real subprocess, so no in-process counter can reach it.
  ;; Its meter is the filesystem itself: a workspace whose `.clj-surgeon.edn`
  ;; is unparseable EDN, which `forms/read-config` THROWS on. An entrance
  ;; that loads config before it validates shape cannot return the shape
  ;; refusal — it returns the config error instead. This is Sol's exact
  ;; scenario, widened from `:threads` to every field.
  (let [root (temp-dir)]
    (try
      (spit-file! (io/file root "src/app/folds.clj") arm-source)
      (spit (io/file root ".clj-surgeon.edn") "{:aliases {\"x\" }\n")
      (doseq [{:keys [label args error-type]} cli-shape-refusals]
        (testing (str "bb refuses " label " on shape, not on the config it never read")
          (let [result (apply bb-cli ":op" "relation-census"
                              ":dir" (.getPath root) args)]
            (is (false? (:ok result))
                (str label " did not refuse: " (pr-str result)))
            (is (= error-type (:error-type result))
                (str label ": the bb entrance loaded the workspace config "
                     "before it validated the request shape: "
                     (pr-str result))))))

      (testing "a well-shaped request still reaches that config, and still trips it"
        ;; The meter's liveness check: a valid request DOES load the config,
        ;; so every assertion above is about ORDER, not about the config
        ;; having quietly become unreachable.
        (let [result (bb-cli ":op" "relation-census" ":dir" (.getPath root))]
          (is (= :invalid-arguments (:error-type result))
              (str "the trap never fired, so the refusals above prove "
                   "nothing: " (pr-str result)))))
      (finally (delete-tree! root)))))

;; ---------------------------------------------------------------------------
;; Sol's round-ten review, item 6: validator parity.
;;
;; "MCP refuses string `doors`/`files`, string `pool_size`, `max_files`, and
;; `format`; the CLI shape pass accepts or ignores all except invalid
;; `threads`." The defect that finding names is not any one missing check —
;; it is that there was no place where the two entrances had to agree, so
;; drift was free and silent.
;;
;; `census/request-shape-rules` is that place. This witness enumerates EVERY
;; row of it and drives BOTH entrances with a request that violates that row
;; and nothing before it, then asserts each entrance published the name its
;; own column of the table declares. Where the two names differ they differ
;; only by the mapping the table itself records, and the witness reads that
;; mapping rather than hard-coding either name:
;;
;;   unknown-fields        -> :unknown-arguments   (the CLI's word is argument)
;;   doors-not-an-array    -> :doors-not-a-string  (array on the wire, one
;;                                                  comma-separated string at
;;                                                  the CLI)
;;   pool-size-not-an-integer \
;;   pool-size-out-of-range   / -> :invalid-pool-size  (many-to-one: the CLI
;;                                                  has published one name
;;                                                  since the op shipped, and
;;                                                  its message names both the
;;                                                  bound and the value)
;;
;; A row the CLI cannot express carries `:inexpressible` saying why, and the
;; witness asserts the row is genuinely closed rather than merely
;; unimplemented.
;; ---------------------------------------------------------------------------

(def ^:private undecodable-probe-path
  "A path argument carrying U+FFFD — what a raw 0xff byte in a filename looks
   like once the runtime has decoded argv and the original byte is gone."
  "/tmp/clj-surgeon-census-not-decodable/root\ufffd")

(def ^:private shape-rule-probes
  "For each row of the shared table: a request violating THAT row and no row
   before it, spelled for each entrance that can express it."
  {[:unknown-fields :present]
   {:mcp {:nope 1} :cli {:format :edn}}

   [:dir :type]
   {:cli {:dir ""}}

   [:paths :not-decodable]
   {:mcp {:workspace_root undecodable-probe-path}
    :cli {:dir undecodable-probe-path}}

   [:doors :container-type]
   {:mcp {:doors "conj-once"} :cli {:doors [1]}}

   [:doors :too-many]
   {:mcp {:doors (vec (repeat (inc census/max-doors) "conj-once"))}
    :cli {:doors thirty-three-doors}}

   [:doors :entry-type]
   {:mcp {:doors [1]}}

   [:doors :vocabulary]
   {:mcp {:files [fixture] :doors ["conj"]} :cli {:doors "conj"}}

   [:files :container-type]
   {:mcp {:files fixture}}

   [:files :empty]
   {:mcp {:files []}}

   [:files :too-many]
   {:mcp {:files (vec (repeat (inc census/max-requested-files) fixture))}}

   [:files :entry-type]
   {:mcp {:files [""]} :cli {:file ""}}

   [:pool-size :not-an-integer]
   {:mcp {:files [fixture] :pool_size "8"} :cli {:threads "not-a-number"}}

   [:pool-size :out-of-range]
   {:mcp {:files [fixture] :pool_size 0} :cli {:threads "0"}}})

(defn- published-mcp-name
  "The name the tool published for a refusal, whichever key carries it.

   Every shape refusal publishes `error_type` \"invalid-mcp-request\" and the
   specific name in `reason`. A refusal computed AFTER discovery — the
   door-DEFINEDNESS check is the one that reaches this helper — publishes its
   name as the `error_type` itself."
  [result]
  (or (:reason result) (:error_type result)))

;; @spec MCP-OP-CENSUS-016
;; @spec MCP-OP-CENSUS-029
(deftest one-refusal-table-governs-both-census-entrances
  (testing "every row of the shared table has a probe, and every probe a row"
    (is (= (set (map (juxt :field :violation) census/request-shape-rules))
           (set (keys shape-rule-probes)))
        "the table and the probes have drifted apart"))

  (doseq [rule census/request-shape-rules]
    (let [key [(:field rule) (:violation rule)]
          probe (get shape-rule-probes key)]

      (if (keyword? (:mcp rule))
        (testing (str "the tool refuses " key " as the table says")
          (let [result (run (:mcp probe))]
            (is (false? (:ok result))
                (str key " was accepted by the tool: " (pr-str result)))
            (is (= (name (:mcp rule)) (published-mcp-name result))
                (str key ": the tool published "
                     (pr-str (published-mcp-name result))
                     ", the table says " (pr-str (:mcp rule))))))

        (testing (str key " is closed at the tool, not merely unimplemented")
          (is (string? (:inexpressible (:mcp rule)))
              (str key ": the MCP column is neither a published name nor a "
                   "stated reason the violation cannot arise"))
          (is (not (str/blank? (:inexpressible (:mcp rule)))))
          (is (nil? (census/shape-name :mcp (:field rule) (:violation rule)))
              (str key ": the table both closes the row and names it"))
          (is (nil? (:mcp probe))
              (str key ": the row says the tool cannot express this "
                   "violation, and the probes contain an MCP spelling of it"))))

      (if (keyword? (:cli rule))
        (testing (str "the CLI refuses " key " as the table says")
          (let [result (binding [*out* (java.io.StringWriter.)]
                         (core/run (merge {:op :relation-census :dir repo-root}
                                          (:cli probe))))]
            (is (false? (:ok result))
                (str key " was accepted by the CLI: " (pr-str result)))
            (is (= (:cli rule) (:error-type result))
                (str key ": the CLI published " (pr-str (:error-type result))
                     ", the table says " (pr-str (:cli rule))))))

        (testing (str key " is closed at the CLI, not merely unimplemented")
          (is (string? (:inexpressible (:cli rule)))
              (str key ": the CLI column is neither a published name nor a "
                   "stated reason the violation cannot arise"))
          (is (not (str/blank? (:inexpressible (:cli rule)))))
          (is (nil? (census/shape-name :cli (:field rule) (:violation rule)))
              (str key ": the table both closes the row and names it"))
          (is (nil? (:cli probe))
              (str key ": the row says the CLI cannot express this "
                   "violation, and the probes contain a CLI spelling of it"))))))

  (testing "the many-to-one pool-size mapping is deliberate, not an accident"
    (is (= :invalid-pool-size
           (census/shape-name :cli :pool-size :not-an-integer)
           (census/shape-name :cli :pool-size :out-of-range))
        "the CLI's two pool-size rows no longer share one published name")
    (is (not= (census/shape-name :mcp :pool-size :not-an-integer)
              (census/shape-name :mcp :pool-size :out-of-range))
        "the tool stopped distinguishing the two pool-size violations"))

  (testing "the CLI op registry accepts exactly the fields the table names"
    ;; Without this, a new CLI argument can be added to the op and the shape
    ;; pass will refuse it as unknown — or, worse, the accepted set can be
    ;; widened here and the op will never read the argument.
    (is (= census/cli-census-fields
           (set (keys (:args (get core/ops-registry :relation-census)))))
        "the op registry and the shared table disagree about the op's arguments"))

  (testing "both entrances refuse on the FIRST row a request violates"
    (let [tool (run {:doors [1] :pool_size "8"})
          cli (binding [*out* (java.io.StringWriter.)]
                (core/run {:op :relation-census :dir repo-root
                           :doors [1] :threads "not-a-number"}))]
      (is (= "doors-not-strings" (published-mcp-name tool))
           "pool_size won the order at the tool")
      (is (= :doors-not-a-string (:error-type cli))
          (str ":threads won the order at the CLI: " (pr-str cli))))))

;; ---------------------------------------------------------------------------
;; Sol's round-ten review, item 5 (blocking): CLI continuations retarget
;; across cwd.
;;
;; Every CLI shape refusal handed back the fixed string
;; `"clj-surgeon :op :relation-census :dir . :threads 8"`. Sol refused a
;; request naming an absolute workspace, replayed the returned continuation
;; from `/tmp/census11-sol-fx/client-cwd`, and the census answered about the
;; CLIENT fixture — a continuation that validates, runs, reports success, and
;; describes a tree the caller never named.
;;
;; This is the CLI's half of the round-nine finding the tool has already
;; closed (MCP-OP-CENSUS-014: a continuation may narrow WHAT is censused; it
;; may never change WHERE). `.` is not a workspace. It is whatever directory
;; the next shell happens to be standing in, which is precisely the thing a
;; continuation exists to stop the caller from having to remember.
;;
;; The witness replays from a DIFFERENT cwd on purpose, and that cwd holds
;; two arm-bearing sources against the workspace's one, so a retargeted
;; census is visible as a count and not only as a path.
;; ---------------------------------------------------------------------------

(defn- bb-cli-in
  "Run the babashka CLI as a subprocess from a named working directory."
  [cwd & args]
  (let [{:keys [out err exit]}
        (apply proc/shell {:out :string :err :string :continue true
                           :dir (str cwd)}
               "bb" "-cp" (str repo-root "/src") "-m" "clj-surgeon.core" args)]
    (try
      (assoc (edn/read-string out) ::exit exit)
      (catch Exception _
        {::exit exit ::out out ::err err}))))

(defn- replay-next-command
  "Run a CLI continuation verbatim, from `cwd`.

   Prefers the refusal's own argv vector when the caller has one — that is
   the spelling a PROGRAM replays, with no shell and nothing to re-parse.
   The rendered string's own safety is proved separately, by replaying it
   through a real `bash -c` (`a-cli-continuation-is-shell-safe`)."
  ([cwd next-command]
   (apply bb-cli-in cwd (rest (str/split (str next-command) #"\s+"))))
  ([cwd _next-command argv]
   (apply bb-cli-in cwd (rest argv))))

;; @spec MCP-OP-CENSUS-014
;; @spec MCP-OP-CENSUS-019
(deftest a-cli-continuation-censuses-the-workspace-the-caller-named
  (let [parent (temp-dir)
        workspace (io/file parent "workspace")
        client (io/file parent "client-cwd")]
    (try
      (spit-file! (io/file workspace "src/app/folds.clj") arm-source)
      (spit-file! (io/file client "src/app/one.clj") arm-source)
      (spit-file! (io/file client "src/app/two.clj") arm-source)
      (let [named (.getCanonicalPath workspace)
            elsewhere (.getCanonicalPath client)]

        (testing "the replay cwd is a workspace of its own, so a retarget shows"
          (let [here (bb-cli-in elsewhere ":op" "relation-census" ":dir" ".")]
            (is (true? (:ok here)))
            (is (= 2 (:files here))
                "the client cwd no longer differs from the named workspace")))

        (testing "an absolute :dir travels into the continuation verbatim"
          (let [refusal (bb-cli-in elsewhere ":op" "relation-census"
                                   ":dir" named ":threads" "not-a-number")]
            (is (= :invalid-pool-size (:error-type refusal))
                (str "the request was not refused on shape: " (pr-str refusal)))
            (is (= {:kind :dir :given named :absolute named} (:anchor refusal))
                (str "the refusal does not name the workspace it was given: "
                     (pr-str (:anchor refusal))))
            (is (str/includes? (str (:next-command refusal)) named)
                (str "the continuation retargets the census: "
                     (pr-str (:next-command refusal))))

            (testing "and replaying it from another cwd censuses that workspace"
              (let [replay (replay-next-command elsewhere
                                                (:next-command refusal))]
                (is (true? (:ok replay))
                    (str "the continuation refused: " (pr-str replay)))
                (is (= 1 (:files replay))
                    (str "the replay censused " (:files replay)
                         " arm-bearing file(s) from " elsewhere
                         "; the workspace the caller named holds 1 and the "
                         "replay cwd holds 2"))
                (is (= 1 (:files-scanned replay))
                    (str "the replay scanned " (:files-scanned replay)
                         " file(s); the workspace the caller named holds 1"))))))

        (testing "a relative :dir is resolved against the cwd it was given in, and says so"
          (let [refusal (bb-cli-in named ":op" "relation-census"
                                   ":dir" "." ":threads" "not-a-number")]
            (is (= :invalid-pool-size (:error-type refusal)))
            (is (= {:kind :dir :given "." :absolute named
                    :resolved-against named}
                   (:anchor refusal))
                (str "a relative :dir left the caller to guess which cwd it "
                     "meant: " (pr-str (:anchor refusal))))
            (is (str/includes? (str (:next-command refusal)) named)
                (str "the continuation still carries a relative anchor: "
                     (pr-str (:next-command refusal))))

            (testing "so the replay still hits the same tree from elsewhere"
              (let [replay (replay-next-command elsewhere
                                                (:next-command refusal))]
                (is (true? (:ok replay))
                    (str "the continuation refused: " (pr-str replay)))
                (is (= 1 (:files replay))
                    (str "the replay censused " (:files replay)
                         " arm-bearing file(s); the relative :dir named a "
                         "workspace holding 1"))))))

        (testing "every CLI shape refusal carries the workspace, not a bare dot"
          ;; The anchor is not a property of the :threads refusal; it is a
          ;; property of every refusal the pure pass can return.
          (doseq [{:keys [label args]} cli-shape-refusals]
            (let [refusal (apply bb-cli-in elsewhere ":op" "relation-census"
                                 ":dir" named args)]
              (is (= named (:absolute (:anchor refusal)))
                  (str label " lost the workspace the caller named: "
                       (pr-str (:anchor refusal))))
              (when-let [command (:next-command refusal)]
                (is (not (str/includes? command " :dir . "))
                    (str label " hands back a continuation anchored on the "
                         "replay's cwd: " command))
                (is (str/includes? command named)
                    (str label " continuation does not name the workspace: "
                         command))))))

        (testing "a workspace too long to carry leaves a remedy, not a truncated call"
          ;; MCP-OP-CENSUS-014's other half, at the CLI: a continuation that
          ;; does not fit the shared bound is not shortened into a call
          ;; naming a different directory — there is no such call, so the
          ;; refusal says so. Without this the branch ships unexercised, and
          ;; an unexercised refusal branch is where the last four rounds'
          ;; findings have lived.
          (let [long-dir (str "/" (apply str (repeat census/max-next-call-bytes
                                                     "d")))
                refusal (core/run-relation-census
                          {:dir long-dir :threads "not-a-number"})]
            (is (= :invalid-pool-size (:error-type refusal)))
            (is (= long-dir (:absolute (:anchor refusal)))
                "the refusal stopped naming the workspace it was given")
            (is (not (contains? refusal :next-command))
                (str "a continuation over the " census/max-next-call-bytes
                     "-byte bound was handed back anyway: "
                     (pr-str (:next-command refusal))))
            (is (string? (:remedy refusal)))
            (is (not (str/includes? (str (:remedy refusal)) "<"))
                "the remedy carries a placeholder")))

        (testing "the JVM CLI op agrees with the bb subprocess"
          (let [refusal (core/run-relation-census
                          {:dir named :doors "conj"})]
            (is (= :unknown-door-symbol (:error-type refusal)))
            (is (= named (:absolute (:anchor refusal))))
            (is (str/includes? (str (:next-command refusal)) named)
                (str "the JVM CLI hands back a bare dot: "
                     (pr-str (:next-command refusal)))))))
      (finally (delete-tree! parent)))))

;; ---------------------------------------------------------------------------
;; Sol's round-nine finding, item 4: a refusal targeting
;; /tmp/census10-fx/workspace handed back
;; `next_call={tool:"relation_census",pool_size:8}`. Replaying it verbatim
;; produced no refusal — it censused the SERVER's default root and scanned 370
;; files. The continuation validates against the published schema and is still
;; not a narrowing of the caller's request: a continuation may narrow WHAT is
;; censused; it may never change WHERE.
;;
;; That defect was introduced by round nine's own fix. Making the shape pass
;; run before routing left it with no canonical root to publish, and it chose
;; to publish nothing rather than the caller's unvalidated string. The choice
;; is wrong: the caller's own value, carried verbatim, either routes to the
;; workspace the caller meant or refuses on that same value — both honest.
;; Silently censusing a different tree is neither.
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-CENSUS-014
(deftest a-continuation-censuses-the-workspace-the-caller-named
  (let [root (temp-dir)]
    (try
      (spit-file! (io/file root "src/app/folds.clj") arm-source)
      (let [target (.getCanonicalPath root)
            ;; The server's default root is this REPO. A continuation that
            ;; drops workspace_root retargets the census from a one-file
            ;; fixture onto the whole repository, and succeeds while doing it.
            refusal (census-tool/execute-request!
                      {:project-root repo-root}
                      {:workspace_root target :doors [1]})
            next-call (:next_call refusal)
            replayed (census-tool/execute-request!
                       {:project-root repo-root}
                       (dissoc next-call :tool))]

        (testing "the request is refused on shape, before any routing"
          (is (false? (:ok refusal)))
          (is (= "doors-not-strings" (:reason refusal))))

        (testing "the continuation carries the caller's workspace_root verbatim"
          (is (= target (:workspace_root next-call))
              (str "the continuation changed the target: " (pr-str next-call))))

        (testing "replayed unmodified, it censuses the same workspace"
          (is (true? (:ok replayed))
              (str "the replay refused: " (:error replayed)))
          (is (= target (:workspace_root replayed))
              (str "the replay censused another tree: "
                   (pr-str (:workspace_root replayed))))
          (is (= 1 (:files replayed))
              (str "the replay scanned " (:files replayed)
                   " file(s); the workspace the caller named holds 1"))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-CENSUS-014
(deftest a-workspace-root-that-cannot-be-carried-leaves-no-continuation
  ;; The invariant is absolute, so it decides this case too: a non-string
  ;; workspace_root cannot be carried into a next_call the published schema
  ;; would accept, and a next_call WITHOUT it targets a different tree. So
  ;; there is no call to compute, and MCP-OP-CENSUS-014 already says what to
  ;; do then — no `next_call` key at all, and a remedy saying why.
  (let [refusal (run {:workspace_root 42 :doors [1]})]
    (is (false? (:ok refusal)))
    (is (= "doors-not-strings" (:reason refusal))
        "a routing field's type beat the shape order it is not part of")
    (is (not (contains? refusal :next_call))
        (str "the continuation silently retargets the census: "
             (pr-str (:next_call refusal))))
    (is (string? (:remedy refusal)))))

;; @spec MCP-OP-CENSUS-018
;; @spec MCP-OP-CENSUS-032
(deftest an-escaping-file-symlink-is-a-counted-skip-at-every-entrance
  (let [parent (temp-dir)
        root (io/file parent "project")
        outside (io/file parent "outside")]
    (try
      (spit-file! (io/file root "src/app/folds.clj") arm-source)
      (spit-file! (io/file outside "secrets.clj")
                  (str arm-source
                       "(defmethod fold-event \"escaped\" [state event]\n"
                       "  (update state :ys conj (:y event)))\n"))
      (Files/createSymbolicLink (.toPath (io/file root "src/escape.clj"))
                                (.toPath (io/file "../../outside/secrets.clj"))
                                (make-array FileAttribute 0))
      (let [{:keys [mcp jvm-cli bb-cli]} (census-entrances (.getPath root))]
        (testing "the tool reads the one in-root source and counts the escape"
          (is (true? (:ok mcp)) (str "refused: " (:error mcp)))
          (is (= 1 (:files mcp)))
          (is (= 1 (:files_scanned mcp)))
          (is (= 1 (:skipped_outside_root mcp))))

        (testing "the JVM CLI reads no file whose real path escapes the root"
          (is (true? (:ok jvm-cli)) (str "refused: " (:error jvm-cli)))
          (is (= 1 (:files jvm-cli))
              "the CLI followed a symlink out of the workspace root")
          (is (= 1 (:skipped-outside-root jvm-cli))
              "the CLI never counted the path it refused to read"))

        (testing "the babashka CLI answers identically"
          (is (true? (:ok bb-cli)) (str "refused: " (:error bb-cli)))
          (is (= 1 (:files bb-cli))
              "the CLI followed a symlink out of the workspace root")
          (is (= 1 (:skipped-outside-root bb-cli))))

        (testing "the arm counts agree across the three entrances"
          (is (= (:arms mcp) (:arms jvm-cli) (:arms bb-cli)))
          (is (= (:counts mcp) (:counts jvm-cli) (:counts bb-cli)))))
      (finally (delete-tree! parent)))))

;; @spec MCP-OP-CENSUS-030
;; @spec MCP-OP-CENSUS-032
(deftest a-link-chain-to-one-real-source-is-censused-once-at-every-entrance
  (let [root (temp-dir)]
    (try
      (spit-file! (io/file root "src/real/folds.clj") arm-source)
      (Files/createSymbolicLink (.toPath (io/file root "src/link2.clj"))
                                (.toPath (io/file "real/folds.clj"))
                                (make-array FileAttribute 0))
      (Files/createSymbolicLink (.toPath (io/file root "src/link1.clj"))
                                (.toPath (io/file "link2.clj"))
                                (make-array FileAttribute 0))
      (let [{:keys [mcp jvm-cli bb-cli]} (census-entrances (.getPath root))]
        (testing "the tool reads the real source once and says what collapsed"
          (is (true? (:ok mcp)) (str "refused: " (:error mcp)))
          (is (= 1 (:files mcp)))
          (is (= ["src/real/folds.clj"] (vec (keys (:by_file mcp)))))
          (is (= 1 (:arms mcp)))
          (is (= 2 (:duplicates_collapsed mcp))
              "the two links onto one real path were never counted"))

        (testing "the JVM CLI collapses the same chain onto the same path"
          (is (true? (:ok jvm-cli)) (str "refused: " (:error jvm-cli)))
          (is (= 1 (:files jvm-cli))
              "the CLI censused one real file three times")
          (is (= 1 (:arms jvm-cli)))
          (is (= 2 (:duplicates-collapsed jvm-cli))))

        (testing "the babashka CLI answers identically"
          (is (true? (:ok bb-cli)) (str "refused: " (:error bb-cli)))
          (is (= 1 (:files bb-cli))
              "the CLI censused one real file three times")
          (is (= 1 (:arms bb-cli)))
          (is (= 2 (:duplicates-collapsed bb-cli))))

        (testing "the site counts agree across the three entrances"
          (is (= (:counts mcp) (:counts jvm-cli) (:counts bb-cli)))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-CENSUS-032
(deftest every-entrance-publishes-the-count-of-files-it-scanned
  (let [dir (.getPath (io/file repo-root "test-fixtures/relation-census"))
        {:keys [mcp jvm-cli bb-cli]} (census-entrances dir)
        scanned (count (filter #(re-find census/source-name-pattern (.getName ^java.io.File %))
                               (.listFiles (io/file dir))))]
    (testing "the tool substantiates the scan it claims to have completed"
      (is (true? (:ok mcp)) (str "refused: " (:error mcp)))
      (is (= scanned (:files_scanned mcp))))

    (testing "the JVM CLI publishes the same count from the same walk"
      (is (true? (:ok jvm-cli)) (str "refused: " (:error jvm-cli)))
      (is (= scanned (:files-scanned jvm-cli))
          "the CLI discovery counted the scan and the receipt dropped it"))

    (testing "the babashka CLI publishes the same count"
      (is (true? (:ok bb-cli)) (str "refused: " (:error bb-cli)))
      (is (= scanned (:files-scanned bb-cli))
          "the CLI discovery counted the scan and the receipt dropped it"))

    (testing "the arm figures agree, so the counts describe one scan"
      (is (= (:files mcp) (:files jvm-cli) (:files bb-cli)))
      (is (= (:arms mcp) (:arms jvm-cli) (:arms bb-cli)))
      (is (= (:counts mcp) (:counts jvm-cli) (:counts bb-cli))))))

;; @spec MCP-OP-CENSUS-028
(deftest an-oversized-list-that-was-truncated-says-how-many-it-left-out
  (let [root (temp-dir)
        listed census/max-listed-files
        total (inc listed)]
    (try
      (spit-file! (io/file root "src/app/folds.clj") arm-source)
      (doseq [i (range total)]
        (spit-file! (io/file root (format "src/app/over%02d.clj" i))
                    (padded-source (inc census/max-source-bytes))))
      (let [{:keys [mcp jvm-cli bb-cli]} (census-entrances (.getPath root))]
        (testing "the tool lists the bound and names the omission"
          (is (true? (:ok mcp)) (str "refused: " (:error mcp)))
          (is (false? (:read_complete mcp)))
          (is (= total (get-in mcp [:oversized_skipped :count])))
          (is (= listed (count (get-in mcp [:oversized_skipped :files]))))
          (is (= (- total listed) (:oversized_skipped_omitted mcp))
              "the receipt listed 12 of 13 names and said nothing about the 13th"))

        (testing "the JVM CLI lists the SAME bound and names the omission"
          (is (true? (:ok jvm-cli)) (str "refused: " (:error jvm-cli)))
          (is (false? (:read-complete jvm-cli)))
          (is (= total (get-in jvm-cli [:oversized-skipped :count])))
          (is (= listed (count (get-in jvm-cli [:oversized-skipped :files])))
              "the CLI hard-coded a listing bound of its own")
          (is (= (- total listed) (:oversized-skipped-omitted jvm-cli))))

        (testing "the babashka CLI answers identically"
          (is (true? (:ok bb-cli)) (str "refused: " (:error bb-cli)))
          (is (= total (get-in bb-cli [:oversized-skipped :count])))
          (is (= listed (count (get-in bb-cli [:oversized-skipped :files]))))
          (is (= (- total listed) (:oversized-skipped-omitted bb-cli))))

        (testing "the three entrances name the same files in the same order"
          (is (= (get-in mcp [:oversized_skipped :files])
                 (get-in jvm-cli [:oversized-skipped :files])
                 (get-in bb-cli [:oversized-skipped :files])))))
      (finally (delete-tree! root)))))

(defn- build-split-candidate-tree!
  "A workspace whose candidates are split across two subtrees: `src/a` holds
   the arm file and fits under the ceiling on its own; `src/b` pushes the
   total past it, so the walk terminates inside `src/b` and only `src/a` has
   an EXACT observed count."
  [root a-count b-count]
  (spit-file! (io/file root "src/a/folds.clj") arm-source)
  (doseq [i (range (dec a-count))]
    (spit-file! (io/file root (format "src/a/f%04d.clj" i)) filler-source))
  (doseq [i (range b-count)]
    (spit-file! (io/file root (format "src/b/f%04d.clj" i)) filler-source)))

;; @spec MCP-OP-CENSUS-027
(deftest the-ceiling-continuation-is-a-narrowing-that-actually-replays
  (let [root (temp-dir)
        fits (- census/max-scanned-files 1500)]
    (try
      (build-split-candidate-tree! root fits (- (inc census/max-scanned-files) fits))
      (let [{:keys [mcp jvm-cli bb-cli]} (census-entrances (.getPath root))
            next-call (:next_call mcp)
            narrowed (:workspace_root next-call)]
        (testing "the tool refuses the over-ceiling tree"
          (is (false? (:ok mcp)))
          (is (= "too-many-candidate-files" (:error_type mcp)))
          (is (= (inc census/max-scanned-files) (:observed mcp)))
          (is (true? (:observed_at_least mcp))))

        (testing "the continuation narrows the root instead of repeating it"
          (is (some? next-call) "the ceiling refusal carried no continuation")
          (is (not= (:workspace_root mcp) narrowed)
              "the continuation hands back the root that was just refused")
          (is (str/ends-with? (str narrowed) "/src/a")
              (str "the largest fully-walked subtree that fits is src/a, got "
                   narrowed))
          (is (not-any? #(str/includes? (str %) "<")
                        (vals (dissoc next-call :tool)))
              "the continuation still carries a placeholder, not a call")
          (is (<= (count (json/generate-string next-call))
                  census/max-next-call-bytes)
              "the continuation is not bounded"))

        (testing "replaying the continuation verbatim completes a census"
          (let [replay (census-tool/execute-request!
                         {:project-root narrowed}
                         (dissoc next-call :tool :workspace_root))]
            (is (true? (:ok replay))
                (str "the continuation the refusal handed back refused: "
                     (:error_type replay) " " (:error replay)))
            (is (= 1 (:files replay)))
            (is (= fits (:files_scanned replay)))))

        (testing "the JVM CLI hands back a narrowing that replays too"
          (is (= :too-many-candidate-files (:error-type jvm-cli)))
          (is (str/includes? (str (:next-command jvm-cli)) "/src/a")
              (str "no narrower subtree in the CLI continuation: "
                   (:next-command jvm-cli)))
          (let [replay (core/run-relation-census
                         {:dir (str (io/file root "src/a"))})]
            (is (true? (:ok replay)) (str "refused: " (:error replay)))
            (is (= fits (:files-scanned replay)))))

        (testing "the babashka CLI hands back the same narrowing"
          (is (= :too-many-candidate-files (:error-type bb-cli)))
          (is (str/includes? (str (:next-command bb-cli)) "/src/a")
              (str "no narrower subtree in the CLI continuation: "
                   (:next-command bb-cli)))
          (is (= (:next-command jvm-cli) (:next-command bb-cli)))))
      (finally (delete-tree! root)))))


;; ---------------------------------------------------------------------------
;; Sol's round-thirteen review, item 3: the two MCP bound refusals told the
;; caller something FALSE.
;;
;; Both compute a narrowing from the walk's own per-directory aggregates, and
;; both then measure the rendered JSON against the shared byte ceiling and drop
;; it when it does not fit. That much is right. What was wrong is what they say
;; afterwards: the remedy is the one written for the case where NO fully-walked
;; subtree fits at all — "no subtree it finished walking is known to fit" —
;; and Sol got it from a walk that had found one and measured it at 891 bytes.
;;
;; A refusal that names a bound without naming the value it compared against
;; leaves the caller to guess how much shorter is short enough; a refusal that
;; reports the wrong REASON leaves them narrowing the wrong thing entirely. The
;; CLI's own overflow remedy already states its measurement (round twelve); the
;; tool's two bound refusals now do the same, and they distinguish the two
;; cases rather than collapsing both into the more alarming one.
;; ---------------------------------------------------------------------------

(defn- long-named-workspace
  "A workspace whose canonical path alone nearly fills the continuation
   ceiling: three 150-character components under a temp directory.

   Sol's counterexample is a real deep tree, not a synthetic string: the
   narrowing the walk computes is `<root>/src/<dir>`, and once `<root>` is
   long enough that rendering costs more than `max-next-call-bytes`, the
   refusal knows a subtree it cannot carry."
  [parent]
  (let [segment (apply str (repeat 150 \d))]
    (io/file parent segment segment segment)))

(defn- narrowing-of
  "The subtree the production kernel would offer for `root`, and what carrying
   it would cost in UTF-8 bytes on the wire."
  [root chooser]
  (let [discover (requiring-resolve 'clj-surgeon.census-discovery/discover)
        choose (requiring-resolve chooser)
        discovered (discover (.toPath (io/file (.getCanonicalPath root))))
        narrower (choose discovered)]
    {:narrower narrower
     :bytes (when narrower
              (census/utf8-byte-count
                (json/generate-string
                  {:tool "relation_census"
                   :workspace_root (str (.getCanonicalPath root) "/" narrower)})))}))

;; @spec MCP-OP-CENSUS-014
;; @spec MCP-OP-CENSUS-027
;; @spec MCP-OP-CENSUS-033
(deftest a-narrowing-too-long-to-carry-says-what-it-measured
  (let [parent (temp-dir)
        root (long-named-workspace parent)]
    (try
      (spit-file! (io/file root "src/a/folds.clj") arm-source)
      (spit-file! (io/file root "src/b/one.clj") arm-source)
      (spit-file! (io/file root "src/b/two.clj") arm-source)

      (testing "the candidate ceiling knew a narrowing and could not carry it"
        (with-redefs [census/max-scanned-files 2]
          (let [{:keys [narrower bytes]}
                (narrowing-of root 'clj-surgeon.census-discovery/narrowing-subtree)
                result (census-tool/execute-request!
                         {:project-root (.getPath root)} {})
                remedy (str (:remedy result))]
            (is (some? narrower)
                "the fixture no longer produces a narrowing to refuse")
            (is (> bytes census/max-next-call-bytes)
                (str "the fixture narrowing fits after all: " bytes " bytes"))
            (is (= "too-many-candidate-files" (:error_type result)))
            (is (not (contains? result :next_call))
                (str "an over-long continuation was handed back: "
                     (pr-str (:next_call result))))
            (is (str/includes? remedy (str bytes))
                (str "the remedy does not state the " bytes
                     " bytes it measured: " (pr-str remedy)))
            ;; `-byte` on purpose: `max-requested-files` happens to be 512
            ;; too, so a bare "512" is already in the wording this witness
            ;; exists to replace.
            (is (str/includes? remedy (str census/max-next-call-bytes "-byte"))
                (str "the remedy does not state the ceiling it compared "
                     "against: " (pr-str remedy)))
            (is (str/includes? remedy (str narrower))
                (str "the remedy does not name the narrowing it knew about: "
                     (pr-str remedy)))
            (is (not (str/includes? remedy "known to fit"))
                (str "the remedy says no subtree was known to fit, and one "
                     "was: " (pr-str remedy))))))

      (testing "the entry bound says the same true thing"
        (with-redefs [census/max-walk-entries 4]
          (let [{:keys [narrower bytes]}
                (narrowing-of root
                              'clj-surgeon.census-discovery/entry-narrowing-subtree)
                result (census-tool/execute-request!
                         {:project-root (.getPath root)} {})
                remedy (str (:remedy result))]
            (is (some? narrower)
                "the fixture no longer produces an entry narrowing to refuse")
            (is (> bytes census/max-next-call-bytes)
                (str "the fixture narrowing fits after all: " bytes " bytes"))
            (is (= "too-many-walk-entries" (:error_type result)))
            (is (not (contains? result :next_call))
                (str "an over-long continuation was handed back: "
                     (pr-str (:next_call result))))
            (is (str/includes? remedy (str bytes))
                (str "the remedy does not state the " bytes
                     " bytes it measured: " (pr-str remedy)))
            ;; `-byte` on purpose: `max-requested-files` happens to be 512
            ;; too, so a bare "512" is already in the wording this witness
            ;; exists to replace.
            (is (str/includes? remedy (str census/max-next-call-bytes "-byte"))
                (str "the remedy does not state the ceiling it compared "
                     "against: " (pr-str remedy)))
            (is (str/includes? remedy (str narrower))
                (str "the remedy does not name the narrowing it knew about: "
                     (pr-str remedy)))
            (is (not (str/includes? remedy "known to fit"))
                (str "the remedy says no subtree was known to fit, and one "
                     "was: " (pr-str remedy))))))

      (testing "when nothing fits, the remedy still says THAT, not the other"
        ;; The branch the over-long wording was stealing. A tree whose only
        ;; fully-walked subtree holds no candidate source has no narrowing at
        ;; all, and its remedy must go on saying so.
        (let [bare (temp-dir)]
          (try
            (spit-file! (io/file bare "src/a/one.clj") arm-source)
            (spit-file! (io/file bare "src/a/two.clj") arm-source)
            (spit-file! (io/file bare "src/a/three.clj") arm-source)
            (with-redefs [census/max-scanned-files 2]
              (let [result (census-tool/execute-request!
                             {:project-root (.getPath bare)} {})
                    remedy (str (:remedy result))]
                (is (= "too-many-candidate-files" (:error_type result)))
                (is (not (contains? result :next_call)))
                (is (str/includes? remedy "known to fit")
                    (str "the no-narrowing remedy lost its own wording: "
                         (pr-str remedy)))
                (is (not (str/includes? remedy "ceiling a continuation must fit"))
                    (str "a tree with no narrowing at all is being told its "
                         "narrowing was too long: " (pr-str remedy)))))
            (finally (delete-tree! bare)))))
      (finally (delete-tree! parent)))))

;; @spec MCP-OP-CENSUS-027
(deftest the-narrowing-rule-is-largest-then-deepest-then-lexicographic
  (let [choose (requiring-resolve 'clj-surgeon.census-discovery/narrowing-subtree)]
    (is (some? choose) "the discovery kernel exposes no narrowing rule")
    (when choose
      (testing "the largest subtree that fits under the ceiling wins"
        (is (= "src/b"
               (choose {:subtree-counts {"" 4001 "src" 4001 "src/a" 10 "src/b" 40}
                        :partial-dirs #{"" "src"}}))))

      (testing "a tie is broken by depth, then by name"
        (is (= "src/a/deep"
               (choose {:subtree-counts {"" 4001 "src" 4001
                                         "src/a" 40 "src/a/deep" 40 "src/b" 40}
                        :partial-dirs #{"" "src"}})))
        (is (= "src/a"
               (choose {:subtree-counts {"" 4001 "src" 4001 "src/b" 40 "src/a" 40}
                        :partial-dirs #{"" "src"}}))))

      (testing "a subtree the walk did not finish is never offered"
        (is (nil? (choose {:subtree-counts {"" 4001 "src" 4001 "src/b" 4000}
                           :partial-dirs #{"" "src" "src/b"}}))
            "a partially walked count is a lower bound, not a fit"))

      (testing "a subtree over the ceiling never wins"
        (is (nil? (choose {:subtree-counts {"" 4001 "src" 4001
                                            "src/a" 4001}
                           :partial-dirs #{"" "src"}})))))))

;; ---------------------------------------------------------------------------
;; Refusal shapes publish the SAME discovery facts a success publishes
;;
;; A receipt whose evidence disappears exactly when the census refuses is a
;; receipt the caller cannot audit: the escaping link, the collapsed link
;; chain and the oversized source are precisely what the caller must be told
;; about when the census found nothing to report.
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-CENSUS-032
(deftest a-refusal-counts-the-escaping-link-at-every-entrance
  (let [parent (temp-dir)
        root (io/file parent "project")
        outside (io/file parent "outside")]
    (try
      (spit-file! (io/file root "src/app/plain.clj") filler-source)
      (spit-file! (io/file outside "secrets.clj") filler-source)
      (Files/createSymbolicLink (.toPath (io/file root "src/escape.clj"))
                                (.toPath (io/file "../../outside/secrets.clj"))
                                (make-array FileAttribute 0))
      (let [{:keys [mcp jvm-cli bb-cli]} (census-entrances (.getPath root))]
        (testing "the tool refuses with no arms and still says what it skipped"
          (is (false? (:ok mcp)))
          (is (= "no-fold-arms-found" (:error_type mcp)))
          (is (= 1 (:files_scanned mcp)))
          (is (= 1 (:skipped_outside_root mcp))
              "the refusal dropped the escaping-path count the success publishes"))

        (testing "the JVM CLI publishes the same figures in the same refusal"
          (is (= :no-fold-arms-found (:error-type jvm-cli)))
          (is (= 1 (:files-scanned jvm-cli))
              "the CLI refusal never said how many files it scanned")
          (is (= 1 (:skipped-outside-root jvm-cli))
              "the CLI refusal dropped the escaping-path count"))

        (testing "the babashka CLI answers identically"
          (is (= :no-fold-arms-found (:error-type bb-cli)))
          (is (= 1 (:files-scanned bb-cli)))
          (is (= 1 (:skipped-outside-root bb-cli)))))
      (finally (delete-tree! parent)))))

;; @spec MCP-OP-CENSUS-030
(deftest a-refusal-reports-the-collapsed-link-chain-at-every-entrance
  (let [root (temp-dir)]
    (try
      (spit-file! (io/file root "src/real/plain.clj") filler-source)
      (Files/createSymbolicLink (.toPath (io/file root "src/link2.clj"))
                                (.toPath (io/file "real/plain.clj"))
                                (make-array FileAttribute 0))
      (Files/createSymbolicLink (.toPath (io/file root "src/link1.clj"))
                                (.toPath (io/file "link2.clj"))
                                (make-array FileAttribute 0))
      (let [{:keys [mcp jvm-cli bb-cli]} (census-entrances (.getPath root))]
        (testing "the tool refuses with no arms and still says what collapsed"
          (is (= "no-fold-arms-found" (:error_type mcp)))
          (is (= 1 (:files_scanned mcp)))
          (is (= 2 (:duplicates_collapsed mcp))
              "the refusal dropped the collapse count the success publishes"))

        (testing "the JVM CLI publishes the same collapse in the same refusal"
          (is (= :no-fold-arms-found (:error-type jvm-cli)))
          (is (= 1 (:files-scanned jvm-cli)))
          (is (= 2 (:duplicates-collapsed jvm-cli))))

        (testing "the babashka CLI answers identically"
          (is (= :no-fold-arms-found (:error-type bb-cli)))
          (is (= 1 (:files-scanned bb-cli)))
          (is (= 2 (:duplicates-collapsed bb-cli)))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-CENSUS-028
(deftest a-refusal-names-the-oversized-sources-at-every-entrance
  (let [root (temp-dir)
        listed census/max-listed-files
        total (inc listed)]
    (try
      (doseq [i (range total)]
        (spit-file! (io/file root (format "src/app/over%02d.clj" i))
                    (padded-source (inc census/max-source-bytes))))
      (let [{:keys [mcp jvm-cli bb-cli]} (census-entrances (.getPath root))]
        (testing "the tool names what it did not look at, even with no arms"
          (is (= "no-fold-arms-found" (:error_type mcp)))
          (is (= 0 (:files_scanned mcp)))
          (is (= total (get-in mcp [:oversized_skipped :count]))
              "the refusal published none of the oversized evidence")
          (is (= listed (count (get-in mcp [:oversized_skipped :files]))))
          (is (= (- total listed) (:oversized_skipped_omitted mcp))))

        (testing "the JVM CLI names the same sources in the same refusal"
          (is (= :no-fold-arms-found (:error-type jvm-cli)))
          (is (= 0 (:files-scanned jvm-cli)))
          (is (= total (get-in jvm-cli [:oversized-skipped :count])))
          (is (= listed (count (get-in jvm-cli [:oversized-skipped :files]))))
          (is (= (- total listed) (:oversized-skipped-omitted jvm-cli))))

        (testing "the babashka CLI answers identically"
          (is (= :no-fold-arms-found (:error-type bb-cli)))
          (is (= 0 (:files-scanned bb-cli)))
          (is (= total (get-in bb-cli [:oversized-skipped :count])))
          (is (= listed (count (get-in bb-cli [:oversized-skipped :files]))))
          (is (= (- total listed) (:oversized-skipped-omitted bb-cli))))

        (testing "the three entrances name the same files in the same order"
          (is (= (get-in mcp [:oversized_skipped :files])
                 (get-in jvm-cli [:oversized-skipped :files])
                 (get-in bb-cli [:oversized-skipped :files])))))
      (finally (delete-tree! root)))))

;; ---------------------------------------------------------------------------
;; The walk is bounded by the entries it VISITS, not only by the candidates
;; it collects: a tree of 60,000 non-sources holds no candidate at all, so the
;; scanned-file ceiling never fires and the walk reads the whole directory
;; tree to discover nothing.
;; ---------------------------------------------------------------------------

(defn- build-entry-heavy-tree!
  "One arm file plus `n` non-source entries spread over 1,000-entry directories."
  [root n]
  (spit-file! (io/file root "src/a/folds.clj") arm-source)
  (doseq [d (range (quot n 1000))]
    (let [dir (io/file root (format "src/junk/d%02d" d))]
      (.mkdirs dir)
      (doseq [i (range 1000)]
        (.createNewFile (io/file dir (format "f%04d.txt" i)))))))

;; @spec MCP-OP-CENSUS-033
(deftest the-walk-is-bounded-by-the-entries-it-visits-at-every-entrance
  (let [bound 50000
        shared (some-> (requiring-resolve 'clj-surgeon.relation-census/max-walk-entries)
                       deref)
        root (temp-dir)]
    (is (= bound shared)
        "the entry bound is not a shared bound both entrances read")
    (try
      (build-entry-heavy-tree! root 60000)
      (let [t0 (System/nanoTime)
            {:keys [mcp jvm-cli bb-cli]} (census-entrances (.getPath root))
            elapsed-ms (/ (- (System/nanoTime) t0) 1e6)]
        (testing "the tool refuses the entry-heavy tree instead of walking it"
          (is (false? (:ok mcp))
              "60,000 non-sources were walked because none of them counted")
          (is (= "too-many-walk-entries" (:error_type mcp)))
          (is (= bound (:maximum mcp)))
          (is (= (inc bound) (:observed mcp))
              "the walk did not stop at one entry past the bound")
          (is (true? (:observed_at_least mcp)))
          (is (= 0 (:files_read mcp)))
          (is (false? (:read_complete mcp)))
          (is (nil? (:counts mcp))))

        (testing "the refusal narrows to the arm-bearing subtree, not the junk"
          (let [narrowed (get-in mcp [:next_call :workspace_root])
                arms-dir (str (.getCanonicalPath root) "/src/a")]
            (is (some? narrowed)
                (str "the walk finished a subtree that holds arms and offered "
                     "no continuation; remedy: " (pr-str (:remedy mcp))))
            (when narrowed
              (is (not= (:workspace_root mcp) narrowed)
                  "the continuation hands back the root it just refused")
              (is (not-any? #(str/includes? (str %) "<")
                            (vals (dissoc (:next_call mcp) :tool)))
                  "the continuation carries a placeholder, not a call")
              (is (= arms-dir narrowed)
                  "the continuation offers a subtree that holds no source")
              (let [replay (census-tool/execute-request!
                             {:project-root narrowed}
                             (dissoc (:next_call mcp) :tool :workspace_root))]
                (is (true? (:ok replay))
                    (str "the narrowing the refusal handed back refuses too: "
                         (:error_type replay) " " (:error replay)))
                (is (= 1 (:arms replay))
                    "the narrowing replayed onto a subtree with no fold arms")))))

        (testing "the JVM CLI refuses the same tree with the same figures"
          (is (= :too-many-walk-entries (:error-type jvm-cli)))
          (is (= bound (:maximum jvm-cli)))
          (is (= (inc bound) (:observed jvm-cli)))
          (is (true? (:observed-at-least jvm-cli)))
          (is (or (string? (:next-command jvm-cli)) (string? (:remedy jvm-cli)))))

        (testing "the babashka CLI answers identically"
          (is (= :too-many-walk-entries (:error-type bb-cli)))
          (is (= bound (:maximum bb-cli)))
          (is (= (inc bound) (:observed bb-cli)))
          (is (= (:next-command jvm-cli) (:next-command bb-cli))))

        (testing "both CLI entrances narrow to the same replayable subtree"
          (let [command (:next-command jvm-cli)
                arms-dir (str (.getCanonicalPath root) "/src/a")]
            (is (string? command)
                (str "the JVM CLI offered no continuation; remedy: "
                     (pr-str (:remedy jvm-cli))))
            (when command
              (is (= arms-dir (second (re-find #":dir (.+)$" command)))
                  "the CLI continuation offers a subtree that holds no source")
              ;; `bb-cli` names the receipt map in this scope, so the replay
              ;; goes back through `census-entrances`, which runs both CLIs.
              (let [replay (census-entrances arms-dir)]
                (is (= 1 (:arms (:jvm-cli replay)))
                    "the JVM CLI narrowing replays onto a subtree with no arms")
                (is (= 1 (:arms (:bb-cli replay)))
                    "the babashka narrowing replays onto a subtree with no arms")))))

        (testing "all three answered in bounded time"
          (is (< elapsed-ms 120000)
              (str "the three entrances took " (long elapsed-ms) "ms"))))
      (finally (delete-tree! root)))))

(defn- build-flat-directory!
  "`n` entries in ONE directory: `n` - 1 non-sources and one arm source whose
   name sorts LAST.

   The source sorts last on purpose. A walk that materialises the complete
   directory listing, sorts it, and only then charges the entry bound spends
   the whole directory before it can refuse it; a walk that streams the
   listing charges as the filesystem yields, and the only way to prove which
   one is running is a source that a truncated front-of-the-sort never
   reaches."
  [root n]
  (.mkdirs ^java.io.File root)
  (doseq [i (range (dec n))]
    (.createNewFile (io/file root (format "f%06d.txt" i))))
  (spit (io/file root "zzz_folds.clj") arm-source))

;; @spec MCP-OP-CENSUS-033
(deftest the-walk-streams-a-directory-instead-of-materialising-it
  (let [bound census/max-walk-entries
        root (temp-dir)]
    (try
      (build-flat-directory! root bound)
      (testing "a flat directory of exactly the bound is censused, source last"
        (let [{:keys [mcp jvm-cli bb-cli]} (census-entrances (.getPath root))]
          (is (true? (:ok mcp))
              (str "the walk refused a tree that is exactly at the bound: "
                   (:error_type mcp) " " (:error mcp)))
          (is (= 1 (:arms mcp)))
          (is (true? (:ok jvm-cli)) (str "refused: " (:error jvm-cli)))
          (is (= 1 (:arms jvm-cli)))
          (is (true? (:ok bb-cli)) (str "refused: " (:error bb-cli)))
          (is (= 1 (:arms bb-cli)))))

      ;; 1,000 entries PAST the bound, not one: the walk must stop at
      ;; `bound` + 1 names, so a listing that materialised the directory
      ;; before charging is caught by the 999 names it had no business
      ;; obtaining.
      (doseq [i (range 1000)]
        (.createNewFile (io/file root (format "z%06d.txt" i))))
      (testing "past the bound the walk refuses without realising the rest"
        (let [{:keys [mcp jvm-cli bb-cli]} (census-entrances (.getPath root))]
          (is (= "too-many-walk-entries" (:error_type mcp)))
          (is (= (inc bound) (:observed mcp)))
          (is (some? (:entries_yielded mcp))
              "the refusal publishes no count of the names it obtained")
          (is (<= (:entries_yielded mcp 0) (inc bound))
              (str "the walk obtained " (:entries_yielded mcp)
                   " names from a directory it stopped at " (inc bound)))
          (is (= :too-many-walk-entries (:error-type jvm-cli)))
          (is (some? (:entries-yielded jvm-cli))
              "the JVM CLI refusal publishes no count of the names it obtained")
          (is (<= (:entries-yielded jvm-cli 0) (inc bound))
              (str "the JVM CLI walk obtained " (:entries-yielded jvm-cli)
                   " names"))
          (is (= :too-many-walk-entries (:error-type bb-cli)))
          (is (some? (:entries-yielded bb-cli))
              "the babashka refusal publishes no count of the names it obtained")
          (is (<= (:entries-yielded bb-cli 0) (inc bound))
              (str "the babashka walk obtained " (:entries-yielded bb-cli)
                   " names"))))
      (finally (delete-tree! root)))))

;; @spec MCP-OP-CENSUS-017
(deftest an-exhausted-census-offers-no-placeholder-continuation
  (with-redefs [census-tool/collect-inputs
                (fn [& _] (throw (OutOfMemoryError. "Java heap space")))]
    (let [result (run {:files [fixture]})
          wire (json/generate-string result)]
      (is (false? (:ok result)))
      (is (= "census-resource-exhausted" (:error_type result)))
      (is (not (contains? result :next_call))
          "the exhaustion refusal still hands back a call it cannot compute")
      (is (string? (:remedy result))
          "no continuation and no remedy: the caller is told nothing")
      (is (not (str/includes? wire "<"))
          (str "the exhaustion receipt carries a placeholder: " wire)))))

;; ---------------------------------------------------------------------------
;; A minimal JSON-Schema-subset checker, covering exactly the shapes
;; census-tool-schema uses: object/array/string/integer/number/boolean,
;; properties, additionalProperties, items, minItems/maxItems,
;; minimum/maximum. This is not a general validator; it exists to answer one
;; question a hand read cannot answer reliably at scale — does an emitted
;; `next_call`, MINUS the `:tool` routing key the schema itself never
;; declares, validate against the tool's own published input schema. A
;; continuation that fails this is not a smaller promise than a call, it is
;; one the advertised contract itself would reject.
;; ---------------------------------------------------------------------------

(defn- same-target
  "A `workspace_root` reduced to what two calls must agree on.

   A refusal computed AFTER routing publishes the canonical root; one
   computed BEFORE routing has no canonical value and publishes the caller's
   own string. Those name one target, so the comparison is by real path when
   the path exists and by the string as given when it does not."
  [value]
  (when (string? value)
    (let [file (io/file value)]
      (if (.exists file) (.getCanonicalPath file) value))))

(defn- schema-violations
  [schema value]
  (let [t (:type schema)]
    (cond
      (nil? schema) []

      (= "integer" t)
      (cond
        (not (integer? value)) [(str (pr-str value) " is not a JSON integer")]
        (and (:minimum schema) (< value (:minimum schema)))
        [(str value " is below minimum " (:minimum schema))]
        (and (:maximum schema) (> value (:maximum schema)))
        [(str value " is above maximum " (:maximum schema))]
        :else [])

      (= "number" t)
      (if (number? value) [] [(str (pr-str value) " is not a JSON number")])

      (= "boolean" t)
      (if (boolean? value) [] [(str (pr-str value) " is not a JSON boolean")])

      (= "string" t)
      (if (string? value) [] [(str (pr-str value) " is not a JSON string")])

      (= "array" t)
      (if (and (sequential? value) (not (map? value)))
        (concat
          (when (and (:minItems schema) (< (count value) (:minItems schema)))
            [(str "array has " (count value) " item(s), fewer than minItems "
                  (:minItems schema))])
          (when (and (:maxItems schema) (> (count value) (:maxItems schema)))
            [(str "array has " (count value) " item(s), more than maxItems "
                  (:maxItems schema))])
          (mapcat #(schema-violations (:items schema) %) value))
        [(str (pr-str value) " is not a JSON array")])

      (= "object" t)
      (if (map? value)
        (let [props (:properties schema)
              entries (map (fn [[k v]] [(if (keyword? k) (name k) (str k)) v])
                           value)
              unknown (remove #(contains? props (first %)) entries)]
          (concat
            (when (false? (:additionalProperties schema))
              (map (fn [[k _]] (str "unknown property " k)) unknown))
            (mapcat (fn [[k v]]
                      (when (contains? props k)
                        (schema-violations (get props k) v)))
                    entries)))
        [(str (pr-str value) " is not a JSON object")])

      :else [])))

(defn- schema-conformant?
  [schema value]
  (empty? (schema-violations schema value)))

;; ---------------------------------------------------------------------------
;; MCP-OP-CENSUS-014, stated globally: a caption in an argument position is
;; not a smaller promise than a call, it is an unexecutable one. Every refusal
;; shape either carries a continuation a caller may replay verbatim, or no
;; continuation at all and a remedy saying why none could be computed.
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-CENSUS-014
;; @spec MCP-OP-CENSUS-027
(deftest no-refusal-anywhere-puts-a-caption-in-an-argument-position
  (let [root (temp-dir)
        empty-root (temp-dir)
        ;; Sol's round-fourteen item 7 shape needs a tree of its OWN: a
        ;; chmod-000 entry inside `root` would refuse the rows above that
        ;; WALK it, rather than the refusal each of those exists to probe.
        denied-root (temp-dir)
        big "src/app/huge.clj"
        small "src/app/small.clj"
        arm "src/app/folds.clj"
        denied "src/app/denied.clj"]
    (try
      (spit-file! (io/file root big)
                  (str "(defmethod fold-event \"a\" [state event] state)\n"
                       (apply str (repeat (inc census/max-source-bytes) \;))))
      (spit-file! (io/file root small) arm-source)
      (.mkdirs (io/file empty-root "src"))
      (spit-file! (io/file denied-root arm) arm-source)
      (spit-file! (io/file denied-root denied) arm-source)
      (deny-reads! (io/file denied-root denied))
      (let [here (fn [params]
                   (census-tool/execute-request!
                     {:project-root (.getPath root)} params))
            denied-here (fn [params]
                          (census-tool/execute-request!
                            {:project-root (.getPath denied-root)} params))
            empty-here (fn [params]
                         (census-tool/execute-request!
                           {:project-root (.getPath empty-root)} params))
            ;; label -> [entrance request]. The battery keeps the REQUEST, not
            ;; only the answer: a continuation can only be judged against the
            ;; call it claims to narrow, and Sol's round-nine item 4 — a
            ;; next_call that silently retargeted the census to the server's
            ;; default root — is invisible to a battery that has forgotten
            ;; what was asked.
            mcp-calls
            {:invalid-workspace [run {:workspace_root "relative/nope"}]
             :unknown-field [run {:files [fixture] :nope 1}]
             :pool-size [run {:files [fixture] :pool_size 0}]
             :unknown-door [run {:files [fixture] :doors ["made-up-door"]}]
             :source-too-large-only [here {:files [big]}]
             :source-too-large-mixed [here {:files [big small]}]
             :source-too-large-mixed-with-options
             [here {:files [big small] :doors ["upsert-by"] :pool_size 1}]
             :doors-not-a-string [run {:files [fixture] :doors [1]}]
             ;; Sol's round-seven finding: a malformed doors item passed
             ;; server validation and was copied unchanged into the
             ;; oversized-file next_call, because that branch is reached
             ;; before parse-doors ever runs. This exact shape must now be
             ;; refused before any filesystem work, with no doors leak.
             :source-too-large-mixed-with-bad-door
             [here {:files [big small] :doors [1] :pool_size 1}]
             ;; Sol's round-nine shape: a pre-routing refusal of a request
             ;; that named a workspace of its own.
             :doors-not-a-string-in-a-named-workspace
             [run {:workspace_root (.getCanonicalPath root) :doors [1]}]
             :source-too-large-in-a-named-workspace
             [run {:workspace_root (.getCanonicalPath root)
                   :files [big small]}]
             :no-arms-scanned [run {:files [helpers]}]
             :no-arms-empty [empty-here {}]
             ;; Sol's round-ten item 8: the remedy branch this battery had
             ;; never held. A workspace_root that is not a string cannot be
             ;; carried into a next_call the published schema would accept,
             ;; and a next_call without it targets a different tree — so the
             ;; refusal offers no continuation and a remedy instead. A remedy
             ;; is only worth publishing if following it WORKS, so the
             ;; battery holds the remedied request beside it and replays what
             ;; that one hands back.
             :non-string-workspace-root [run {:workspace_root 42 :doors [1]}]
             :non-string-workspace-root-remedied
             [run {:workspace_root (.getCanonicalPath root) :doors [1]}]
             ;; Sol's round-thirteen item 2, blocking: U+FFFD ALONE refused
             ;; correctly and carried no continuation; U+FFFD BESIDE an
             ;; unknown field did not. The unknown-field row sat first in the
             ;; shared table, so this request was refused as `unknown-fields`
             ;; — a refusal that computes a continuation — and that
             ;; continuation carried the corrupt path. A continuation is a
             ;; NARROWING of the request; a request whose path did not decode
             ;; has no faithful narrowing, so the decodability row is asked
             ;; FIRST OVERALL rather than fourth.
             :not-decodable-with-unknown-field
             [run {:workspace_root undecodable-probe-path :bogus 1}]
             ;; Sol's round-fourteen item 7: a source that EXISTS and cannot
             ;; be read. It belongs in this battery and not only in its own
             ;; witness, because the continuation it computes has to satisfy
             ;; the SAME two global properties every other continuation does
             ;; — it validates against the published schema, and it names the
             ;; workspace the request named — and neither is asserted by a
             ;; witness that only reads the refusal it expected.
             :unreadable-denied-mixed
             [denied-here {:files [arm denied]
                           :doors ["upsert-by"]
                           :pool_size 1}]
             :unreadable-denied-only [denied-here {:files [denied]}]}
            mcp-refusals (into {}
                               (map (fn [[label [entrance params]]]
                                      [label (entrance params)]))
                               mcp-calls)
            cli-refusals
            {:jvm-source-too-large (core/run-relation-census
                                     {:file (str (.getPath root) "/" big)})
             :jvm-no-arms (core/run-relation-census
                            {:dir (.getPath empty-root)})
             :jvm-unknown-door (core/run-relation-census
                                 {:dir (.getPath root) :doors "made-up-door"})
             :jvm-pool (core/run-relation-census
                         {:dir (.getPath empty-root) :threads 0})
             :bb-source-too-large (bb-cli ":op" "relation-census"
                                          ":file" (str (.getPath root) "/" big))
             :bb-no-arms (bb-cli ":op" "relation-census"
                                 ":dir" (.getPath empty-root))
             :bb-unknown-door (bb-cli ":op" "relation-census"
                                      ":dir" (.getPath root)
                                      ":doors" "made-up-door")
             :bb-pool (bb-cli ":op" "relation-census"
                              ":dir" (.getPath empty-root) ":threads" "0")
             ;; The CLI half of Sol's round-thirteen item 2: an unknown
             ;; ARGUMENT beside a `:dir` that did not decode.
             :jvm-not-decodable-with-unknown-argument
             (core/run-relation-census {:dir undecodable-probe-path
                                        :format :edn})}]

        ;; Round nineteen, item 4. `census/workspace-root-token` is `<workspace_
        ;; root>`, which is angle-bracketed on purpose — no relative path can
        ;; be mistaken for it — and it is now the root's one name in PROSE at
        ;; both entrances. It is the OPPOSITE of the caption this witness
        ;; refuses: a caption stands where an ARGUMENT belongs and cannot be
        ;; run, while the token stands in a sentence beside an `:anchor`,
        ;; `:dir` or `workspace_root` that carries the real path. So it is
        ;; removed before the placeholder question is asked, and asserted for
        ;; separately by `no-refusal-names-the-workspace-root-in-its-prose`.
        (let [without-token #(str/replace (str %) census/workspace-root-token "")]

          (testing "no MCP refusal serialises a placeholder"
            (doseq [[label result] mcp-refusals]
              (let [wire (json/generate-string result)]
                (is (false? (:ok result)) (str label " did not refuse"))
                (is (not (str/includes? (without-token wire) "<"))
                    (str label " carries a placeholder: " wire)))))

          (testing "no CLI refusal, on either runtime, serialises a placeholder"
            (doseq [[label result] cli-refusals]
              (let [wire (pr-str result)]
                (is (false? (:ok result)) (str label " did not refuse: " wire))
                (is (not (str/includes? (without-token wire) "<"))
                    (str label " carries a placeholder: " wire))))))

        (testing "the oversized refusal is the same request minus the file"
          (let [mixed (:source-too-large-mixed mcp-refusals)]
            (is (= "source-too-large" (:error_type mixed)))
            (is (= [small] (get-in mixed [:next_call :files]))
                "the continuation is not the request minus the oversized file")
            (is (= [big] (:files_removed mixed)))
            (is (= 0 (:files_removed_omitted mixed)))))

        (testing "the oversized replay carries every other option through unchanged"
          ;; The original request named files, doors AND pool_size. The
          ;; refusal must not be the request minus the file AND minus the
          ;; rest of the caller's options — it is the original request with
          ;; only the oversized path removed.
          (let [with-options (:source-too-large-mixed-with-options mcp-refusals)
                next-call (:next_call with-options)]
            (is (= "source-too-large" (:error_type with-options)))
            (is (= [small] (:files next-call)))
            (is (= ["upsert-by"] (:doors next-call))
                "doors was dropped from the continuation")
            (is (= 1 (:pool_size next-call))
                "pool_size was dropped from the continuation")
            (is (= {:tool "relation_census"
                    :workspace_root (.getCanonicalPath root)
                    :files [small]
                    :doors ["upsert-by"]
                    :pool_size 1}
                   next-call)
                "the continuation is not the original request minus only the oversized file")))

        (testing "every named source oversized leaves no request to make"
          (let [only (:source-too-large-only mcp-refusals)]
            (is (= "source-too-large" (:error_type only)))
            (is (not (contains? only :next_call))
                "the refusal hands back a call it cannot compute")
            (is (string? (:remedy only)))
            (is (= [big] (:files_removed only)))))

        (testing "an invalid workspace root gets a remedy, not a caption"
          (let [result (:invalid-workspace mcp-refusals)]
            (is (= "invalid-workspace-root" (:error_type result)))
            (is (not (contains? result :next_call)))
            (is (string? (:remedy result)))))

        (testing "an arm-less tree never hands back the call it just refused"
          (let [result (:no-arms-empty mcp-refusals)]
            (is (= "no-fold-arms-found" (:error_type result)))
            (is (not (contains? result :next_call))
                "the continuation is the workspace call that just refused")
            (is (string? (:remedy result)))))

        (testing "an explicitly named, arm-less file list is not its own continuation"
          (let [result (:no-arms-scanned mcp-refusals)]
            (is (= "no-fold-arms-found" (:error_type result)))
            (is (not (contains? result :next_call))
                "the caller's own file list, already refused, is not a continuation")
            (is (string? (:remedy result)))
            (is (str/includes? (:remedy result) helpers))))

        (testing "both CLI runtimes answer the arm-less tree with a remedy"
          (doseq [label [:jvm-no-arms :bb-no-arms]]
            (let [result (label cli-refusals)]
              (is (= :no-fold-arms-found (:error-type result)))
              (is (not (contains? result :next-command))
                  (str label " hands back a call it cannot compute"))
              (is (string? (:remedy result)))
              ;; Round nineteen, item 4: the remedy names the tree it
              ;; scanned by the root's ONE name, and the absolute path it
              ;; scanned is in `:dir` and `:anchor` — the fields a reader
              ;; checks their request against and a replay reads. Naming it
              ;; both ways in one receipt is the defect Sol's round-eighteen
              ;; item 4 recorded, one refusal over.
              (is (str/includes? (str (:remedy result))
                                 census/workspace-root-token)
                  (str label " remedy does not name the tree it scanned"))
              (is (= (.getCanonicalPath empty-root) (:dir result))
                  (str label " lost the absolute tree it scanned: "
                       (pr-str (:dir result))))
              (is (= (.getCanonicalPath empty-root)
                     (:absolute (:anchor result)))
                  (str label " lost the workspace the caller named: "
                       (pr-str (:anchor result))))
              (is (str/includes? (str (:remedy result)) "0 file(s) scanned")
                  (str label " remedy does not say what it scanned")))))

        (testing "both CLI runtimes answer an oversized :file with a remedy"
          (doseq [label [:jvm-source-too-large :bb-source-too-large]]
            (let [result (label cli-refusals)]
              (is (= :source-too-large (:error-type result)))
              (is (not (contains? result :next-command))
                  (str label " hands back a call it cannot compute"))
              (is (string? (:remedy result))))))

        (testing "a malformed doors item refuses before any filesystem work"
          (let [only (:doors-not-a-string mcp-refusals)
                mixed (:source-too-large-mixed-with-bad-door mcp-refusals)]
            (is (= "doors-not-strings" (:reason only)))
            (is (= 0 (:index only)))
            (is (= 1 (:value only)))
            (is (= "doors-not-strings" (:reason mixed))
                "the oversized-file check ran before doors was validated")
            (is (not (contains? (:next_call mixed) :doors))
                "a malformed door reached the oversized-file continuation")))

        (testing "a workspace_root that is not a string offers a remedy, not a continuation"
          (let [result (:non-string-workspace-root mcp-refusals)]
            (is (= "invalid-mcp-request" (:error_type result)))
            (is (= "doors-not-strings" (:reason result))
                "a routing field's type beat the shape order it is not part of")
            (is (not (contains? result :next_call))
                (str "the continuation silently retargets the census: "
                     (pr-str (:next_call result))))
            (is (string? (:remedy result)))
            (is (str/includes? (:remedy result) "workspace_root")
                "the remedy does not name the field the caller must fix")
            (is (str/includes? (:remedy result) "absolute")
                "the remedy does not say what a carriable value looks like")))

        (testing "following that remedy yields a continuation that replays"
          ;; The remedy says: retry with workspace_root naming an existing
          ;; absolute directory. Doing exactly that must produce a refusal
          ;; whose continuation targets the SAME root and replays without
          ;; refusing — otherwise the remedy is advice that leads to another
          ;; dead end, which is the failure mode MCP-OP-CENSUS-014 exists to
          ;; prevent.
          (let [remedied (:non-string-workspace-root-remedied mcp-refusals)
                next-call (:next_call remedied)
                asked (.getCanonicalPath root)]
            (is (= "doors-not-strings" (:reason remedied)))
            (is (some? next-call)
                "the remedied request still hands back no continuation")
            (is (= (same-target asked) (same-target (:workspace_root next-call)))
                (str "the remedied continuation retargets the census: "
                     (pr-str next-call)))
            (let [replayed (run (dissoc next-call :tool))]
              (is (true? (:ok replayed))
                  (str "the remedied continuation refused: "
                       (:error_type replayed) " " (:error replayed)))
              (is (= (same-target asked) (same-target (:workspace_root replayed)))
                  (str "the replay censused another tree: "
                       (pr-str (:workspace_root replayed))))
              (is (= 1 (:files replayed))
                  (str "the replay scanned " (:files replayed)
                       " arm-bearing file(s); the remedied workspace holds 1")))))

        (testing "a corrupt path outranks every other shape question, both entrances"
          ;; The refusal a corrupt path earns is the one refusal that can
          ;; offer no continuation of ANY kind, so it has to be reached
          ;; FIRST: every row after it builds its continuation out of a path
          ;; whose bytes are gone.
          (let [tool (:not-decodable-with-unknown-field mcp-refusals)
                cli (:jvm-not-decodable-with-unknown-argument cli-refusals)]
            (is (= "workspace-root-not-decodable"
                   (or (:reason tool) (:error_type tool)))
                (str "an unknown field won the order over a path that did "
                     "not decode: " (pr-str tool)))
            (is (not (contains? tool :next_call))
                (str "the continuation carries the corrupt path: "
                     (pr-str (:next_call tool))))
            (is (string? (:remedy tool)))
            (is (= "workspace_root" (:argument tool)))
            (is (= :dir-not-decodable (:error-type cli))
                (str "an unknown argument won the order over a path that did "
                     "not decode: " (pr-str cli)))
            (is (not (contains? cli :next-command))
                (str "the continuation carries the corrupt path: "
                     (pr-str (:next-command cli))))
            (is (not (contains? cli :next-command-argv)))
            (is (string? (:remedy cli)))
            (is (= ":dir" (:argument cli)))))

        (testing "every next_call this battery emits validates against the published schema"
          ;; The published schema, not a hand read, is the authority on
          ;; whether a continuation is executable. Sol's round-seven finding
          ;; was exactly this: doors=[1] passed server validation and was
          ;; copied into a next_call the schema rejects (items must be
          ;; strings). Any failure here is the same class of bug.
          (doseq [[label result] mcp-refusals]
            (when-let [next-call (:next_call result)]
              (let [violations (schema-violations census-tool/census-tool-schema
                                                   (dissoc next-call :tool))]
                (is (empty? violations)
                    (str label " next_call is not executable through the "
                         "published input schema: " violations " — "
                         (pr-str next-call)))))))

        (testing "every next_call targets the workspace its request named"
          ;; Executable is not enough. Sol's round-nine item 4: a refusal
          ;; targeting a fixture workspace handed back
          ;; {tool, pool_size: 8} — schema-valid, and it replays against the
          ;; SERVER's default root, censusing a tree the caller never named
          ;; and reporting success. A continuation may narrow WHAT is
          ;; censused; it may never change WHERE.
          (doseq [[label [_ params]] mcp-calls]
            (when-let [asked (:workspace_root params)]
              (when-let [next-call (:next_call (get mcp-refusals label))]
                (is (= (same-target asked)
                       (same-target (:workspace_root next-call)))
                    (str label " continuation retargets the census: the "
                         "request named " (pr-str asked) ", the continuation "
                         (pr-str (:workspace_root next-call)) " — "
                         (pr-str next-call))))))))
      (finally
        (allow-reads! (io/file denied-root denied))
        (delete-tree! root)
        (delete-tree! empty-root)
        (delete-tree! denied-root)))))

;; @spec MCP-OP-CENSUS-033
(deftest the-entry-narrowing-fits-under-both-bounds
  (let [choose (requiring-resolve
                 'clj-surgeon.census-discovery/entry-narrowing-subtree)]
    (is (some? choose) "the discovery kernel exposes no entry-narrowing rule")
    (when choose
      (testing "the subtree with the most entries that still fits wins"
        (is (= "src/b"
               (choose {:subtree-entries {"" 50001 "src" 50001
                                          "src/a" 1000 "src/b" 4000}
                        :subtree-counts {"src/a" 1 "src/b" 1}
                        :partial-dirs #{"" "src"}}))))

      (testing "a subtree that would trip the CANDIDATE ceiling is not offered"
        (is (= "src/a"
               (choose {:subtree-entries {"" 50001 "src" 50001
                                          "src/a" 1000 "src/b" 4000}
                        :subtree-counts {"src/a" 1
                                         "src/b" (inc census/max-scanned-files)}
                        :partial-dirs #{"" "src"}}))
            "the narrowing replays straight into the other refusal"))

      (testing "a subtree the walk did not finish is never offered"
        (is (nil? (choose {:subtree-entries {"" 50001 "src" 50001
                                             "src/b" 50000}
                           :subtree-counts {"src/b" 1}
                           :partial-dirs #{"" "src" "src/b"}}))))

      (testing "a subtree over the entry bound never wins"
        (is (nil? (choose {:subtree-entries {"" 50001 "src" 50001
                                             "src/a" (inc census/max-walk-entries)}
                           :subtree-counts {"src/a" 1}
                           :partial-dirs #{"" "src"}}))))

      ;; The entry bound fires on trees of JUNK: that is what it is for. So
      ;; the biggest fully-walked subtree is very often the junk itself, and
      ;; offering it hands the caller a call that replays into
      ;; `no-fold-arms-found` on a workspace that has arms.
      (testing "a bigger subtree with no candidate source loses to a small one"
        (is (= "src/a"
               (choose {:subtree-entries {"" 50001 "src" 50001
                                          "src/a" 3 "src/junk" 49997}
                        :subtree-counts {"src/a" 1}
                        :partial-dirs #{"" "src"}}))
            "the narrowing offers a subtree the replay can find nothing in"))

      (testing "no fully-walked subtree holds a source: no narrowing at all"
        (is (nil? (choose {:subtree-entries {"" 50001 "src" 50001
                                             "src/junk" 49997}
                           :subtree-counts {}
                           :partial-dirs #{"" "src"}}))
            "a subtree with nothing to census is not a continuation"))

      (testing "equal entry counts are broken by the most candidate sources"
        (is (= "src/b"
               (choose {:subtree-entries {"" 50001 "src" 50001
                                          "src/a" 100 "src/b" 100}
                        :subtree-counts {"src/a" 1 "src/b" 4}
                        :partial-dirs #{"" "src"}})))))))

;; ---------------------------------------------------------------------------
;; Sol's round-eleven review, item 6 (blocking): oversized MCP integers escape
;; validation.
;;
;; An actual `execute-request!` carrying `pool_size` 9223372036854775808 threw
;; `IllegalArgumentException: Value out of range for long` instead of
;; returning `pool-size-out-of-range`. The kernel asked its RANGE question
;; through a coercion — `(long value)` — so the one value the range check
;; exists to refuse is the one value the coercion cannot survive. A bound
;; enforced by an exception is not a bound: the caller gets a stack trace
;; instead of a typed reason, a bound, and a continuation, and the tool's own
;; refusal contract (MCP-OP-CENSUS-014) never runs.
;;
;; The JSON wire can carry an integer of any magnitude. A caller who sends
;; 2^63 is asking a question this kernel must ANSWER, and the answer is
;; "between 1 and 64, and that is not".
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-CENSUS-016
;; @spec MCP-OP-CENSUS-019
;; @spec MCP-OP-CENSUS-029
(deftest an-integer-field-is-range-checked-before-it-is-coerced
  (testing "the shared kernel decides magnitude without coercing to long"
    (doseq [[value reason]
            [[9223372036854775808N :out-of-range]
             [(biginteger "9223372036854775808") :out-of-range]
             [(- 9223372036854775808N) :out-of-range]
             ["9223372036854775808" :out-of-range]
             ["-1" :out-of-range]
             [-1 :out-of-range]
             [0 :out-of-range]
             [(inc census/max-pool-size) :out-of-range]
             ["1e3" :not-an-integer]
             ["1.5" :not-an-integer]
             [1.5 :not-an-integer]
             ["not-a-number" :not-an-integer]]]
      (let [outcome (try (census/coerce-pool-size value)
                         (catch Throwable e
                           {::threw (str (.getName (class e)) ": "
                                         (ex-message e))}))]
        (is (nil? (::threw outcome))
            (str "coerce-pool-size " (pr-str value) " threw instead of "
                 "refusing: " (::threw outcome)))
        (is (= reason (:reason outcome))
            (str "coerce-pool-size " (pr-str value) " answered "
                 (pr-str (:reason outcome)) ", not " (pr-str reason))))))

  (testing "no pool_size escapes execute-request! as an exception"
    (doseq [[value reason]
            [[9223372036854775808N "pool-size-out-of-range"]
             [(biginteger "9223372036854775808") "pool-size-out-of-range"]
             [-1 "pool-size-out-of-range"]
             [0 "pool-size-out-of-range"]
             [(inc census/max-pool-size) "pool-size-out-of-range"]
             [1e3 "pool-size-not-an-integer"]
             [1.5 "pool-size-not-an-integer"]
             ["8" "pool-size-not-an-integer"]]]
      (let [result (try (run {:files [fixture] :pool_size value})
                        (catch Throwable e
                          {::threw (str (.getName (class e)) ": "
                                        (ex-message e))}))]
        (is (nil? (::threw result))
            (str "pool_size " (pr-str value)
                 " escaped execute-request! as an exception: "
                 (::threw result)))
        (is (false? (:ok result))
            (str "pool_size " (pr-str value) " was accepted: "
                 (pr-str result)))
        (is (= reason (:reason result))
            (str "pool_size " (pr-str value) " refused as "
                 (pr-str (:reason result)) ", not " (pr-str reason)))
        ;; The refusal is a receipt, so it must survive the wire it is
        ;; published on: a value too large for a long is still a JSON number.
        (is (string? (try (json/generate-string result)
                          (catch Throwable e (str "THREW " (ex-message e)))))
            "the refusal cannot be serialised")
        (is (not (str/includes? (str (json/generate-string result)) "THREW"))))))

  (testing "the CLI's :threads refuses the same magnitudes, typed"
    (doseq [value ["9223372036854775808" "-9223372036854775808" "-1" "0"
                   "65" "1e3" "1.5" "not-a-number"]]
      (let [result (try (binding [*out* (java.io.StringWriter.)]
                          (core/run {:op :relation-census :dir repo-root
                                     :threads value}))
                        (catch Throwable e
                          {::threw (str (.getName (class e)) ": "
                                        (ex-message e))}))]
        (is (nil? (::threw result))
            (str ":threads " (pr-str value) " escaped the CLI entrance as an "
                 "exception: " (::threw result)))
        (is (= :invalid-pool-size (:error-type result))
            (str ":threads " (pr-str value) " refused as "
                 (pr-str (:error-type result)) ": " (pr-str result)))))))

;; ---------------------------------------------------------------------------
;; Sol's round-eleven review, item 7: duplicate `:file` flags are silently
;; collapsed.
;;
;; `clj-surgeon :op :relation-census :file one.clj :file two.clj` censused
;; `two.clj` alone and reported `:ok true` with `files-scanned 1`. The caller
;; asked about two sources and was told, in a receipt that claims completeness,
;; about one — with no refusal, no warning, and no count they could reconcile
;; against what they asked for.
;;
;; This is the same failure MCP-OP-CENSUS-019 already names for an argument
;; the op does not accept: an argument that is silently DROPPED tells the
;; caller a bound was applied that never existed. A repeated flag is one
;; argument dropped, and the last-one-wins rule that drops it is invisible in
;; the receipt. The CLI grammar has no repeated flags, so a repeat is a
;; malformed request, not a list.
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-CENSUS-019
(deftest a-repeated-cli-flag-is-refused-and-never-silently-collapsed
  (testing "the parser refuses a repeated flag, naming it"
    (doseq [[args flag]
            [[[":op" "relation-census" ":file" "a.clj" ":file" "b.clj"] ":file"]
             [[":op" "relation-census" ":dir" "a" ":dir" "b"] ":dir"]
             [[":op" "relation-census" ":dir" "a" ":doors" "x" ":doors" "y"]
              ":doors"]
             [[":op" "relation-census" ":dir" "a" ":threads" "2" ":threads" "4"]
              ":threads"]
             [[":op" "a" ":op" "b"] ":op"]]]
      (let [outcome (try {::opts (core/parse-args args)}
                         (catch Exception e
                           {::data (ex-data e) ::message (ex-message e)}))]
        (is (nil? (::opts outcome))
            (str (pr-str args) " was parsed rather than refused: "
                 (pr-str (::opts outcome))))
        (is (= :duplicate-argument (:error-type (::data outcome)))
            (str (pr-str args) " refused as "
                 (pr-str (:error-type (::data outcome)))))
        (is (= flag (:argument (::data outcome)))
            (str (pr-str args) " did not name the repeated flag: "
                 (pr-str (::data outcome))))
        (is (str/includes? (str (::message outcome)) flag)
            (str "the message does not name " flag ": "
                 (pr-str (::message outcome)))))))

  (testing "a request with no repeat still parses"
    (is (= {:op "relation-census" :dir "a" :file "b.clj" :threads "2"}
           (core/parse-args [":op" "relation-census" ":dir" "a"
                             ":file" "b.clj" ":threads" "2"]))))

  (testing "the babashka entrance refuses the repeat instead of censusing one file"
    (let [parent (temp-dir)
          one (io/file parent "src/app/one.clj")
          two (io/file parent "src/app/two.clj")]
      (try
        (spit-file! one arm-source)
        (spit-file! two arm-source)
        (let [result (bb-cli-in parent ":op" "relation-census"
                                ":file" (.getPath one)
                                ":file" (.getPath two))]
          (is (not (true? (:ok result)))
              (str "the repeat was censused as a complete request: "
                   (pr-str result)))
          (is (= :duplicate-argument (:error-type result))
              (str "the bb entrance refused as "
                   (pr-str (:error-type result)) ": " (pr-str result)))
          (is (= ":file" (:argument result))
              (str "the bb refusal does not name the repeated flag: "
                   (pr-str result))))
        (finally (delete-tree! parent))))))

;; ---------------------------------------------------------------------------
;; Sol's round-eleven review, item 8: `:dir` is outside shape validation.
;;
;; `:dir [1]` returned a GENERIC `:invalid-arguments` carrying
;; "No implementation of method: :as-file of protocol: Coercions found for
;; class: clojure.lang.PersistentVector" — a coercion accident from three
;; frames down, not a typed refusal naming the field. And `:dir ""` was worse
;; than untyped: it stat'ed the config ancestors, SCANNED THE CWD, and
;; SUCCEEDED, answering about whatever directory the process happened to be
;; standing in rather than the workspace the caller (failed to) name.
;;
;; `:dir` is the CLI's anchor. Every other refusal's continuation is built
;; from it, so a request whose anchor is unusable cannot produce a faithful
;; continuation for any later row — which is why it joins the shared table
;; ahead of `doors`, `files` and `threads` rather than beside them.
;;
;; The meter is strace, not an in-process counter: the config ancestor walk
;; happens inside the bb subprocess, where no `with-redefs` can see it. The
;; witness carries its own liveness check — a well-shaped request from the
;; same cwd DOES name `.clj-surgeon.edn` in the trace — so a green here is
;; about ORDER and not about the trace having gone blind.
;; ---------------------------------------------------------------------------

(defn- strace-file-log
  "Run the bb CLI under strace from `cwd`; return the file-syscall trace text."
  [cwd & args]
  (let [log (io/file cwd "strace.log")]
    (apply proc/shell {:out :string :err :string :continue true :dir (str cwd)}
           "strace" "-f" "-e" "trace=file" "-o" (.getPath log)
           "bb" "-cp" (str repo-root "/src") "-m" "clj-surgeon.core" args)
    (if (.exists log) (slurp log) "")))

;; @spec MCP-OP-CENSUS-016
;; @spec MCP-OP-CENSUS-019
(deftest the-cli-anchor-is-validated-before-any-filesystem-touch
  (testing "the shared table owns :dir, and the CLI publishes its name"
    (is (some? (census/shape-rule :dir :type))
        ":dir is not a row of the shared refusal table")
    (is (= :dir-not-a-string (census/shape-name :cli :dir :type))
        "the table does not publish a CLI name for a malformed :dir"))

  (testing "a malformed :dir refuses typed, in process, before any scan"
    ;; `"   "` is NOT in this list, and its absence is the point of Sol's
    ;; round-twelve item 1: whitespace is a legal relative path, so the row
    ;; refuses the EMPTY string and non-strings, and nothing else.
    ;; `a-cli-anchor-carries-the-path-bytes-the-caller-gave` holds the other
    ;; side of that line.
    (doseq [value ["" [1] {:a 1} 7]]
      (let [root-calls (atom 0)
            read-calls (atom 0)
            config-loads (atom 0)
            result (with-redefs [forms/init-from-file!
                                 (counting config-loads forms/init-from-file!)
                                 core/census-root
                                 (counting root-calls core/census-root)
                                 core/census-sources
                                 (counting read-calls core/census-sources)]
                     (binding [*out* (java.io.StringWriter.)]
                       (core/run {:op :relation-census :dir value})))]
        (is (false? (:ok result))
            (str ":dir " (pr-str value) " was accepted: " (pr-str result)))
        (is (= :dir-not-a-string (:error-type result))
            (str ":dir " (pr-str value) " refused as "
                 (pr-str (:error-type result)) ": " (pr-str result)))
        (is (= 0 @config-loads @root-calls @read-calls)
            (str ":dir " (pr-str value) " reached the filesystem: "
                 @config-loads " config load(s), " @root-calls
                 " root resolution(s), " @read-calls " scan(s)")))))

  (testing "the babashka entrance names no config ancestor for a malformed :dir"
    (let [cwd (temp-dir)]
      (try
        (spit-file! (io/file cwd "src/app/folds.clj") arm-source)
        (spit (io/file cwd ".clj-surgeon.edn") "{:aliases {}}")
        (testing "liveness: a well-shaped request DOES read that config"
          (let [trace (strace-file-log cwd ":op" "relation-census" ":dir" ".")]
            (is (str/includes? trace ".clj-surgeon.edn")
                "the strace meter never saw the config read it exists to see")))
        (doseq [value ["" "[1]"]]
          (let [trace (strace-file-log cwd ":op" "relation-census" ":dir" value)]
            (is (not (str/includes? trace ".clj-surgeon.edn"))
                (str ":dir " (pr-str value)
                     " walked the cwd's config ancestors before it was "
                     "refused: "
                     (pr-str (vec (take 4 (filter #(str/includes?
                                                     % ".clj-surgeon.edn")
                                                  (str/split-lines trace)))))))))
        (finally (delete-tree! cwd)))))

  (testing "the bb entrance refuses rather than censusing the cwd"
    (let [cwd (temp-dir)]
      (try
        (spit-file! (io/file cwd "src/app/one.clj") arm-source)
        (spit-file! (io/file cwd "src/app/two.clj") arm-source)
        (doseq [value ["" "[1]"]]
          (let [result (bb-cli-in cwd ":op" "relation-census" ":dir" value)]
            (is (not (true? (:ok result)))
                (str ":dir " (pr-str value) " censused the cwd: "
                     (pr-str result)))
            (is (= :dir-not-a-string (:error-type result))
                (str ":dir " (pr-str value) " refused as "
                     (pr-str (:error-type result)) ": " (pr-str result)))))
        (finally (delete-tree! cwd)))))

  (testing "a well-shaped :dir is still accepted"
    (let [cwd (temp-dir)]
      (try
        (spit-file! (io/file cwd "src/app/one.clj") arm-source)
        (let [result (bb-cli-in cwd ":op" "relation-census" ":dir" ".")]
          (is (true? (:ok result)) (str "a valid :dir refused: " (pr-str result)))
          (is (= 1 (:files result))))
        (finally (delete-tree! cwd))))))

;; ---------------------------------------------------------------------------
;; Sol's round-eleven review, item 3 (partial): the parity witness proves the
;; entrances read the table's PREDICATES, not the table's ORDER.
;;
;; Widening one predicate failed the live parity witness at both entrances
;; (1/58/3), which proves enumeration. But MOVING `files` before `doors` in
;; the table left it green (1/58/0) while the tool still refused `doors` and
;; the CLI still refused `file` — the tool's `cond` carries an ordering of its
;; own, and nothing notices when the two disagree.
;;
;; MCP-OP-CENSUS-029 states the order as a REQUIREMENT, so the order has to be
;; a fact about the table rather than a coincidence between the table and a
;; hand-written `cond`. A property that can only be tested by editing the
;; source and re-reading the diff is a property nothing enforces, so the
;; mutation is made injectable (`census/*shape-rules*`) and asserted here.
;; ---------------------------------------------------------------------------

(defn- reordered-shape-rules
  "The shared table with its FIELD groups in `field-order`.

   Rows keep their relative order inside a group, so this is a REORDERING of
   the same rows and nothing else — Sol's mutation, made injectable."
  [field-order]
  (let [rank (into {} (map-indexed (fn [i f] [f i]) field-order))]
    (vec (sort-by #(get rank (:field %) (count field-order))
                  census/request-shape-rules))))

(defn- expected-first-refusal
  "The name `entrance` must publish, read from the table in force.

   Computed from the table rather than hard-coded, so a permutation the
   witness did not anticipate still has an expected answer."
  [entrance params]
  (let [req (census/normalise-request entrance params)]
    (some (fn [rule]
            (when (and (keyword? (get rule entrance))
                       (not ((:predicate rule) req)))
              (get rule entrance)))
          (census/shape-rules))))

;; @spec MCP-OP-CENSUS-016
;; @spec MCP-OP-CENSUS-029
(deftest the-refusal-order-is-the-tables-order-at-both-entrances
  (let [mcp-probe {:doors "conj-once" :files [] :pool_size "8"}
        cli-probe {:op :relation-census :dir repo-root
                   :doors [1] :file "" :threads "not-a-number"}
        cli-refusal #(binding [*out* (java.io.StringWriter.)]
                       (core/run cli-probe))
        files-first (reordered-shape-rules
                      [:unknown-fields :dir :files :doors :pool-size])]

    (testing "the mutation is a reordering of the same rows, nothing else"
      (is (= (set census/request-shape-rules) (set files-first))
          "the reordering added or dropped a row")
      (is (not= census/request-shape-rules files-first)
          "the reordering did not reorder anything"))

    (testing "with the table as written, doors wins at both entrances"
      (is (= "doors-not-an-array" (published-mcp-name (run mcp-probe))))
      (is (= :doors-not-a-string (:error-type (cli-refusal)))))

    (testing "moving files ahead of doors moves BOTH entrances' refusal"
      (binding [census/*shape-rules* files-first]
        (is (= "empty-file-list" (published-mcp-name (run mcp-probe)))
            (str "the tool keeps an ordering of its own, independent of the "
                 "table: it still published "
                 (pr-str (published-mcp-name (run mcp-probe)))))
        (is (= :file-not-a-string (:error-type (cli-refusal)))
            (str "the CLI keeps an ordering of its own, independent of the "
                 "table: it still published "
                 (pr-str (:error-type (cli-refusal)))))))

    (testing "every field-group permutation moves both entrances together"
      (doseq [order [[:unknown-fields :dir :pool-size :doors :files]
                     [:unknown-fields :dir :files :pool-size :doors]
                     [:unknown-fields :dir :pool-size :files :doors]
                     [:unknown-fields :dir :doors :files :pool-size]]]
        (binding [census/*shape-rules* (reordered-shape-rules order)]
          (let [want-mcp (expected-first-refusal :mcp mcp-probe)
                want-cli (expected-first-refusal :cli cli-probe)]
            (is (= (name want-mcp) (published-mcp-name (run mcp-probe)))
                (str "order " (pr-str order) ": the table's first violated row "
                     "is " (pr-str want-mcp) ", the tool published "
                     (pr-str (published-mcp-name (run mcp-probe)))))
            (is (= want-cli (:error-type (cli-refusal)))
                (str "order " (pr-str order) ": the table's first violated row "
                     "is " (pr-str want-cli) ", the CLI published "
                     (pr-str (:error-type (cli-refusal)))))))))

    (testing "the real table is restored outside the binding"
      (is (= "doors-not-an-array" (published-mcp-name (run mcp-probe))))
      (is (= :doors-not-a-string (:error-type (cli-refusal)))))))

;; ---------------------------------------------------------------------------
;; Sol's round-eleven review, item 2 (blocking): the anchor fix reached the
;; SHAPE refusals and stopped there.
;;
;; Round ten routed every refusal from the pure pass through `cli-anchor` and
;; `cli-next-command`, and Sol confirmed it. But the refusals computed AFTER
;; the scan still spelled their own command, and one of them —
;; `unknown-door-symbol`, raised when a door parses as a symbol but is
;; DEFINED in no scanned file, which can only be known after the scan — still
;; handed back the literal `:dir . :doors …`. Sol refused a request naming an
;; absolute workspace, replayed the continuation from the client fixture, and
;; the census answered about `client.clj`: a continuation that validates,
;; runs, reports success, and describes a tree the caller never named. The
;; same silent retarget MCP-OP-CENSUS-014 forbids, from a site the fix never
;; reached.
;;
;; A rule that lives in one branch is a rule the other branches break. So
;; this witness does not check the branch Sol found; it ENUMERATES every
;; typed refusal the op can emit, drives each one through the entrance, and
;; asserts three things of all of them: the refusal names the workspace the
;; caller named; any continuation it carries was BUILT BY
;; `census/cli-next-command` (counted, not grepped); and that continuation's
;; anchor argument is an absolute path and never `.`. The enumeration is
;; pinned to `census/cli-refusal-types`, so a refusal added to the op without
;; a probe here fails this witness rather than shipping unexercised.
;; ---------------------------------------------------------------------------

(def ^:private malformed-arm-source
  "A source that DEFINES an arm and cannot be parsed: the census worker's
   per-file failure, which is otherwise unreachable from a well-formed tree."
  "(defmethod fold-event :x [state event] (update state :xs conj")

(defn- anchor-argument
  "The value in the `:dir`/`:file` argument position of a CLI continuation."
  [command]
  (let [tokens (vec (str/split (str command) #"\s+"))
        index (first (keep-indexed (fn [i t] (when (#{":dir" ":file"} t) i))
                                   tokens))]
    (when index (get tokens (inc index)))))

;; ---------------------------------------------------------------------------
;; The CLI enumeration MACHINERY, extracted so more than one witness can drive
;; it. `every-cli-refusal-anchors-its-continuation-on-the-named-workspace` asks
;; whether each refusal names the workspace the caller named;
;; `every-continuation-either-entrance-emits-fits-the-byte-ceiling` asks the
;; same drives whether what they hand back fits the wire. Both are pinned to
;; `census/cli-refusal-types`, so a refusal added to the op without a probe
;; fails BOTH rather than shipping unexercised in either.
;; ---------------------------------------------------------------------------

(defn- cli-refusal-fixture!
  "Build, under `parent`, every tree the CLI enumeration drives need."
  [^java.io.File parent]
  (let [trees {:workspace (io/file parent "workspace")
               :empty-ws (io/file parent "empty")
               :broken (io/file parent "broken")
               ;; A chmod-000 source lives in a tree of its OWN. Every other
               ;; drive walks `workspace`, and an unreadable entry inside it
               ;; would refuse those walks `unreadable-source-path` instead of
               ;; the refusal each one exists to probe.
               :denied-file (io/file parent "denied/src/a/denied.clj")
               ;; Sol's round-fifteen item 7: a DIRECTORY carrying a source
               ;; name. It lives outside `workspace` for the same reason the
               ;; denied source does — inside it the walk would simply descend
               ;; it — and it is driven by `:file`, which is the shape that
               ;; reached `slurp` and threw "(Is a directory)". The FIFO of the
               ;; same class is driven by its own witness, under a deadline: a
               ;; drive that blocks belongs nowhere near a shared fixture.
               :dir-named-clj (io/file parent "dirfile/src/a/thing.clj")
               ;; Sol's round-eighteen item 2: a link UNDER a workspace whose
               ;; real path leaves it. Its own tree for the same reason the
               ;; denied source has one — inside `workspace` the walk would
               ;; count it `skipped-outside-root` and every other drive's
               ;; figures would change.
               :escaping (io/file parent "escaping")}]
    (spit-file! (io/file (:workspace trees) "src/a/one.clj") arm-source)
    (spit-file! (io/file (:workspace trees) "src/b/two.clj") arm-source)
    (spit-file! (io/file (:workspace trees) "src/b/three.clj") arm-source)
    ;; Not a source, and not discovered by the walk either: the extension rule
    ;; is what both entrances read it through.
    (spit-file! (io/file (:workspace trees) "src/a/notes.txt") "(ns a.notes)")
    (.mkdirs (io/file (:empty-ws trees) "src"))
    (spit-file! (io/file (:broken trees) "src/app/broken.clj")
                malformed-arm-source)
    (spit-file! (:denied-file trees) arm-source)
    (deny-reads! (:denied-file trees))
    (.mkdirs ^java.io.File (:dir-named-clj trees))
    (spit-file! (io/file (:escaping trees) "src/a/real.clj") arm-source)
    (spit-file! (io/file parent "beyond/target.clj") arm-source)
    (Files/createSymbolicLink
      (.toPath (io/file (:escaping trees) "src/a/link.clj"))
      (.toPath (io/file "../../../beyond/target.clj"))
      (make-array FileAttribute 0))
    trees))

(defn- cli-refusal-drives
  "One drive per refusal `census/cli-refusal-types` declares the op can emit."
  [{:keys [workspace empty-ws broken denied-file dir-named-clj escaping]}]
  (let [named #(.getCanonicalPath ^java.io.File %)]
    (concat
      ;; Every row of the shared table the CLI can express, driven
      ;; from the table itself rather than a hand-kept list.
      (for [rule census/request-shape-rules
            :when (keyword? (:cli rule))
            :let [probe (get shape-rule-probes
                             [(:field rule) (:violation rule)])]]
        {:label [(:field rule) (:violation rule)]
         :error-type (:cli rule)
         :root workspace
         ;; The :dir row's own probe REPLACES the anchor with the
         ;; blank value it is refusing, so its refusal names what a
         ;; blank :dir would have meant — the cwd — which is the
         ;; point of that refusal rather than an exception to this
         ;; rule.
         ;; Two rows REPLACE the anchor with the value they are
         ;; refusing, so their refusals name what that value would
         ;; have meant — the cwd for a blank :dir, the undecodable
         ;; string itself for a path that did not survive argv. That
         ;; is the point of those refusals, not an exception to this
         ;; rule.
         :expect-anchor (case (:field rule)
                          :dir (System/getProperty "user.dir")
                          :paths undecodable-probe-path
                          nil)
         :opts (merge {:dir (named workspace)} (:cli probe))})
      [{:label :unknown-door-symbol-after-the-scan
        :error-type :unknown-door-symbol
        :root workspace
        :opts {:dir (named workspace) :doors "no-such-door"}}
       {:label :source-too-large
        :error-type :source-too-large
        :root workspace
        ;; This drive names a :file, so the workspace it named IS
        ;; that file — `cli-anchor` prefers an explicit :file over
        ;; :dir, because that is the narrower thing the caller named.
        :expect-anchor (str (named workspace) "/src/a/one.clj")
        :opts {:file (str (named workspace) "/src/a/one.clj")}
        :around (fn [f]
                  (with-redefs [census/max-source-bytes 4] (f)))}
       ;; Sol's round-thirteen item 7: the missing-`:file` refusal,
       ;; enumerated so it cannot ship unexercised. It names a `:file`,
       ;; so the workspace it named IS that file — even though the file
       ;; is not there, which is the whole point: the anchor is what
       ;; the CALLER named, not what the filesystem confirms.
       {:label :file-not-found
        :error-type :file-not-found
        :root workspace
        :expect-anchor (str (named workspace) "/src/a/missing.clj")
        :opts {:file (str (named workspace) "/src/a/missing.clj")}}
       ;; Sol's round-fourteen item 7: the denied-`:file` refusal,
       ;; enumerated so IT cannot ship unexercised either. Same shape
       ;; as the missing one — the anchor is the file the caller
       ;; named — and a separate row because it is a separate name.
       {:label :file-not-readable
        :error-type :file-not-readable
        :root workspace
        :expect-anchor (.getCanonicalPath denied-file)
        :opts {:file (.getCanonicalPath denied-file)}}
       ;; Sol's round-fifteen item 7: a path that is not a regular file,
       ;; enumerated so it cannot ship unexercised either.
       {:label :file-not-a-regular-file
        :error-type :file-not-a-regular-file
        :root workspace
        :expect-anchor (.getCanonicalPath ^java.io.File dir-named-clj)
        :opts {:file (.getCanonicalPath ^java.io.File dir-named-clj)}}
       ;; Sol's round-eighteen item 2: a NAMED source whose real path leaves
       ;; the workspace the request named, enumerated so it cannot ship
       ;; unexercised. The anchor is the file the caller named, like every
       ;; other `:file` row.
       {:label :file-outside-workspace
        :error-type :file-outside-workspace
        :root escaping
        :expect-anchor (str (named escaping) "/src/a/link.clj")
        :opts {:dir (named escaping)
               :file (str (named escaping) "/src/a/link.clj")}}
       ;; Sol's round-eighteen item 3: a NAMED path that is not a Clojure
       ;; source, enumerated so it cannot ship unexercised. The tool has always
       ;; refused this lexically; the CLI read it.
       {:label :file-not-a-source-path
        :error-type :file-not-a-source-path
        :root workspace
        :expect-anchor (str (named workspace) "/src/a/notes.txt")
        :opts {:dir (named workspace)
               :file (str (named workspace) "/src/a/notes.txt")}}
       {:label :no-fold-arms-found
        :error-type :no-fold-arms-found
        :root empty-ws
        :opts {:dir (named empty-ws)}}
       {:label :unparseable-file
        :error-type :unparseable-file
        :root broken
        :opts {:dir (named broken)}}
       {:label :census-worker-failure
        :error-type :census-worker-failure
        :root workspace
        :opts {:dir (named workspace)}
        :around (fn [f]
                  (with-redefs [census/census-file
                                (fn [& _]
                                  (throw (ex-info "boom" {})))]
                    (f)))}
       ;; Sol's round-fifteen NO-GO item 3: the two names the op gives to a
       ;; throw that escapes its own branches, driven by injecting one. Without
       ;; a drive here the enumeration would be a subset of what the op emits,
       ;; which is the property these witnesses exist to deny.
       {:label :census-adapter-failure
        :error-type :census-adapter-failure
        :root workspace
        :opts {:dir (named workspace)}
        :around (fn [f]
                  (with-redefs [census-discovery/discover
                                (fn [& _]
                                  (throw (ex-info "injected" {})))]
                    (f)))}
       {:label :census-resource-exhausted
        :error-type :census-resource-exhausted
        :root workspace
        :opts {:dir (named workspace)}
        :around (fn [f]
                  (with-redefs [census-discovery/discover
                                (fn [& _] (throw (OutOfMemoryError. "injected")))]
                    (f)))}
       {:label :too-many-candidate-files
        :error-type :too-many-candidate-files
        :root workspace
        :opts {:dir (named workspace)}
        :around (fn [f]
                  (with-redefs [census/max-scanned-files 2] (f)))}
       {:label :too-many-walk-entries
        :error-type :too-many-walk-entries
        :root workspace
        :opts {:dir (named workspace)}
        :around (fn [f]
                  (with-redefs [census/max-walk-entries 3] (f)))}])))

(defn- run-cli-drives
  "Run each drive, counting the continuation-constructor calls it made."
  [drives]
  (doall
    (for [{:keys [label error-type root opts around expect-anchor]}
          drives]
      (let [built (atom 0)
            thunk (fn []
                    (with-redefs [census/cli-continuation
                                  (counting
                                    built census/cli-continuation)]
                      (binding [*out* (java.io.StringWriter.)]
                        (core/run (assoc opts
                                         :op :relation-census)))))
            result ((or around (fn [f] (f))) thunk)]
        {:label label :error-type error-type :root root
         :expect-anchor (or expect-anchor
                            (.getCanonicalPath ^java.io.File root))
         :built @built :result result}))))

;; @spec MCP-OP-CENSUS-014
;; @spec MCP-OP-CENSUS-019
;; @spec MCP-OP-CENSUS-027
;; @spec MCP-OP-CENSUS-033
(deftest every-cli-refusal-anchors-its-continuation-on-the-named-workspace
  (let [parent (temp-dir)
        trees (cli-refusal-fixture! parent)]
    (try
      (let [results (run-cli-drives (cli-refusal-drives trees))]

        (testing "the probes cover every refusal the op declares it can emit"
          (is (= census/cli-refusal-types
                 (set (map (comp :error-type :result) results)))
              (str "declared: " (pr-str census/cli-refusal-types)
                   "; driven: "
                   (pr-str (set (map (comp :error-type :result) results))))))

        (doseq [{:keys [label error-type expect-anchor built result]} results]
          (testing (str label " refuses as declared")
            (is (false? (:ok result))
                (str label " was accepted: " (pr-str result)))
            (is (= error-type (:error-type result))
                (str label " refused as " (pr-str (:error-type result)))))

          (testing (str label " names the workspace the caller named")
            (is (= expect-anchor (:absolute (:anchor result)))
                (str label " lost the workspace: "
                     (pr-str (:anchor result)))))

          (testing (str label " offers exactly one of a continuation and a remedy")
            (is (not= (contains? result :next-command)
                      (contains? result :remedy))
                (str label " offers "
                     (pr-str (select-keys result [:next-command :remedy])))))

          (when-let [command (:next-command result)]
            (testing (str label " builds its continuation through the anchor")
              (is (pos? built)
                  (str label " spelled its own continuation instead of "
                       "calling cli-continuation: " command))
              (is (some? (anchor-argument command))
                  (str label " continuation names no anchor at all: "
                       command))
              (is (not= "." (anchor-argument command))
                  (str label " hands back a bare dot, which names whatever "
                       "directory the next shell is standing in: " command))
              (is (str/starts-with? (str (anchor-argument command)) "/")
                  (str label " continuation anchor is not absolute: "
                       command))))))
      (finally
        (allow-reads! (:denied-file trees))
        (delete-tree! parent)))))

;; @spec MCP-OP-CENSUS-014
(deftest a-post-scan-door-refusal-censuses-the-workspace-the-caller-named
  ;; Sol's exact round-eleven scenario, replayed: refuse from the CLIENT
  ;; fixture while naming the workspace, then replay the continuation from
  ;; that same client fixture. The client holds two arm-bearing sources and
  ;; the workspace holds one, so a retarget shows as a count and not only as
  ;; a path.
  (let [parent (temp-dir)
        workspace (io/file parent "workspace")
        client (io/file parent "client-cwd")]
    (try
      (spit-file! (io/file workspace "src/app/folds.clj") arm-source)
      (spit-file! (io/file client "src/app/one.clj") arm-source)
      (spit-file! (io/file client "src/app/two.clj") arm-source)
      (let [named (.getCanonicalPath workspace)
            elsewhere (.getCanonicalPath client)
            refusal (bb-cli-in elsewhere ":op" "relation-census"
                               ":dir" named ":doors" "no-such-door")]
        (is (= :unknown-door-symbol (:error-type refusal))
            (str "the post-scan door refusal did not fire: "
                 (pr-str refusal)))
        (is (= named (:absolute (:anchor refusal)))
            (str "the refusal does not name the workspace it was given: "
                 (pr-str (:anchor refusal))))
        (is (str/includes? (str (:next-command refusal)) named)
            (str "the continuation retargets the census: "
                 (pr-str (:next-command refusal))))
        (let [replay (replay-next-command elsewhere (:next-command refusal))]
          (is (true? (:ok replay))
              (str "the continuation refused: " (pr-str replay)))
          (is (= 1 (:files replay))
              (str "the replay censused " (:files replay)
                   " arm-bearing file(s) from " elsewhere
                   "; the workspace the caller named holds 1 and the replay "
                   "cwd holds 2"))))
      (finally (delete-tree! parent)))))

;; ---------------------------------------------------------------------------
;; Sol's round-eleven review, item 5 (blocking, security boundary): generated
;; CLI commands are not shell-safe.
;;
;; `:dir "space root"` produced
;;   clj-surgeon :op :relation-census :dir /tmp/census12-sol-fx/space root :threads 8
;; whose replay returned `:invalid-arguments` — the workspace split into two
;; arguments and the pair count went odd. And a root containing
;; `;printf INJECTED` was interpolated VERBATIM, producing command-injection
;; syntax in a string whose entire purpose is to be pasted into a shell.
;;
;; A continuation is an EXECUTABLE PROMISE. MCP-OP-CENSUS-014 already forbids
;; a caption in an argument position because a caption is unexecutable; an
;; unquoted path is the same defect one turn later — it executes, and it
;; executes something else. The path is not attacker-supplied in the ordinary
;; case, but it IS caller-supplied, and a refusal handed to an agent to replay
;; is exactly where a caller-supplied string becomes a command.
;;
;; The witness replays the RENDERED STRING through a real `bash -c`, not
;; through an argv vector, because the string is what a human or an agent
;; pastes. The shim on PATH records the argv bash actually produced, so the
;; assertion is not "it seemed to work" but "bash split this into exactly the
;; tokens the continuation meant" — and a canary file in the replay's own cwd
;; proves nothing else ran.
;; ---------------------------------------------------------------------------

(def ^:private injected-roots
  "Six workspace names, each hostile to a different shell metacharacter."
  [{:label :space :name "space root"}
   {:label :single-quote :name "quote'root"}
   {:label :double-quote :name "double\"root"}
   {:label :semicolon :name "semi;touch canary"}
   {:label :command-substitution :name "dollar$(touch canary)"}
   {:label :newline :name "new\nline"}])

(defn- shell-shim!
  "A `clj-surgeon` on PATH that records the argv bash handed it, NUL-separated,
   then runs the real CLI."
  [dir]
  (let [bin (io/file dir "bin")
        shim (io/file bin "clj-surgeon")]
    (.mkdirs bin)
    (spit shim
          (str "#!/usr/bin/env bash\n"
               ": > \"$CENSUS_ARGV_LOG\"\n"
               "for a in \"$@\"; do printf '%s\\0' \"$a\" >> "
               "\"$CENSUS_ARGV_LOG\"; done\n"
               "exec bb -cp " repo-root "/src -m clj-surgeon.core \"$@\"\n"))
    (.setExecutable shim true)
    (.getPath bin)))

;; @spec MCP-OP-CENSUS-014
;; @spec MCP-OP-CENSUS-019
(deftest a-cli-continuation-is-shell-safe
  (let [parent (temp-dir)
        replay-cwd (io/file parent "replay-cwd")
        canary (io/file replay-cwd "canary")]
    (try
      (.mkdirs replay-cwd)
      (let [bin (shell-shim! parent)]
        (doseq [[index {:keys [label name]}] (map-indexed vector injected-roots)]
          (let [root (io/file parent name)
                source (str "f" index ".clj")
                argv-log (io/file parent (str "argv-" index ".log"))]
            (spit-file! (io/file root "src/app" source) arm-source)
            (let [real (.getCanonicalPath root)
                  refusal (core/run-relation-census
                            {:dir real :threads "not-a-number"})
                  command (:next-command refusal)
                  argv (:next-command-argv refusal)]

              (testing (str label ": the refusal anchors on the root it was given")
                (is (= :invalid-pool-size (:error-type refusal)))
                (is (= real (:absolute (:anchor refusal)))))

              (testing (str label ": the continuation carries an argv vector")
                (is (vector? argv)
                    (str label ": no argv vector was published: "
                         (pr-str (select-keys refusal
                                              [:next-command
                                               :next-command-argv]))))
                (is (some #{real} argv)
                    (str label ": the argv vector does not carry the root: "
                         (pr-str argv))))

              (let [{:keys [out exit]}
                    (proc/shell
                      {:out :string :err :string :continue true
                       :dir (.getPath replay-cwd)
                       :extra-env {"PATH" (str bin ":" (System/getenv "PATH"))
                                   "CENSUS_ARGV_LOG" (.getPath argv-log)}}
                      "bash" "-c" (str command))
                    produced (when (.exists argv-log)
                               (vec (remove str/blank?
                                            (str/split (slurp argv-log)
                                                       #"\u0000"))))
                    replay (try (edn/read-string out) (catch Exception _ nil))]

                (testing (str label ": bash splits the rendered string as meant")
                  (is (zero? exit)
                      (str label ": the rendered continuation did not run "
                           "(exit " exit "): " (pr-str command)))
                  (is (= (vec (rest argv)) produced)
                      (str label ": bash produced " (pr-str produced)
                           " from " (pr-str command)
                           "; the continuation meant "
                           (pr-str (vec (rest argv))))))

                (testing (str label ": the replay censuses the root by real path")
                  (is (true? (:ok replay))
                      (str label ": the replay refused: " (pr-str replay)))
                  (is (= 1 (:files replay))
                      (str label ": the replay censused "
                           (pr-str (:files replay)) " file(s)"))
                  (is (contains? (:by-file replay) (str "src/app/" source))
                      (str label ": the replay censused the WRONG root — "
                           "expected src/app/" source ", got "
                           (pr-str (keys (:by-file replay)))))))))))

      (testing "nothing but the census ran: the canary was never created"
        (is (not (.exists canary))
            (str "an injected root executed a second command: "
                 (.getPath canary) " exists")))
      (finally (delete-tree! parent)))))

;; ---------------------------------------------------------------------------
;; Sol's round-twelve review, item 1 (blocking): trailing whitespace silently
;; retargets.
;;
;; `cli-anchor` ran the caller's path through `str/trim`, so `:dir "/root "`
;; produced an anchor whose `:absolute` was `/root` — a DIFFERENT directory,
;; which on Sol's fixture existed and held two arm-bearing sources against the
;; named root's one. The continuation carried the trimmed sibling, replayed
;; without refusing, and reported success about a tree the caller never named:
;; the silent retarget MCP-OP-CENSUS-014 forbids, arriving this time through a
;; normalisation nobody asked for rather than through a bare dot.
;;
;; A PATH IS THE BYTES THE CALLER GAVE. POSIX filenames may begin and end with
;; spaces, and no layer between the caller and the filesystem is entitled to
;; edit them: `census-root` never did (it absolutizes and canonicalises, both
;; byte-preserving), so the census READ the right tree while the anchor NAMED
;; the wrong one — the two halves of one entrance disagreeing about which
;; directory the request meant.
;;
;; The one string that names nothing is the EMPTY one: POSIX gives the empty
;; pathname no meaning and every syscall answers it with ENOENT. So the
;; `:dir` row refuses `""` and nothing else, and `"   "` — three spaces — is
;; a legal, if peculiar, relative path that resolves against the cwd like any
;; other.
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-CENSUS-014
(deftest a-cli-anchor-carries-the-path-bytes-the-caller-gave
  (let [parent (temp-dir)
        trimmed (io/file parent "root")
        spaced (io/file parent "root ")]
    (try
      (spit-file! (io/file trimmed "src/app/one.clj") arm-source)
      (spit-file! (io/file trimmed "src/app/two.clj") arm-source)
      (spit-file! (io/file spaced "src/app/only.clj") arm-source)
      (let [named (str (.getCanonicalPath parent) "/root ")
            sibling (.getCanonicalPath trimmed)]

        (testing "the fixture is Sol's: the trimmed sibling holds a file more"
          (is (.isDirectory (io/file named))
              "the trailing-space root was not created")
          (is (= 1 (:files (core/run-relation-census {:dir named}))))
          (is (= 2 (:files (core/run-relation-census {:dir sibling})))))

        (testing "the anchor names the path the caller named, byte for byte"
          (let [refusal (core/run-relation-census
                          {:dir named :threads "not-a-number"})
                anchor (:anchor refusal)]
            (is (= :invalid-pool-size (:error-type refusal))
                (str "the request was not refused on shape: " (pr-str refusal)))
            (is (= named (:given anchor))
                "the anchor lost the caller's own string")
            (is (= named (:absolute anchor))
                (str "the anchor trimmed the caller's path to a DIFFERENT "
                     "directory: " (pr-str (:absolute anchor))))
            (is (not (contains? anchor :resolved-against))
                (str "an already-absolute path was reported as resolved "
                     "against the cwd, which only a normalisation could "
                     "cause: " (pr-str anchor)))

            (testing "and the continuation replays against THAT root"
              (let [argv (:next-command-argv refusal)
                    replay (replay-next-command sibling nil argv)]
                (is (= named (nth argv 4))
                    (str "the continuation carries a different directory: "
                         (pr-str argv)))
                (is (true? (:ok replay))
                    (str "the replay refused: " (pr-str replay)))
                (is (= 1 (:files replay))
                    (str "the replay censused " (:files replay)
                         " arm-bearing file(s); the named root holds 1 and "
                         "the trimmed sibling holds 2"))
                (is (contains? (:by-file replay) "src/app/only.clj")
                    (str "the replay censused the WRONG root: "
                         (pr-str (keys (:by-file replay)))))))))

        (testing "only the EMPTY path names nothing; whitespace is a path"
          (is (= :dir-not-a-string
                 (:error-type (census/validate-cli-request-shape {:dir ""})))
              "the empty pathname was accepted")
          (is (nil? (census/validate-cli-request-shape {:dir "   "}))
              (str "a whitespace-only :dir was refused; it is a legal "
                   "relative path and must resolve like any other"))
          (let [anchor (census/cli-anchor {:dir "   "})]
            (is (= "   " (:given anchor)))
            (is (= (str (System/getProperty "user.dir") "/   ")
                   (:absolute anchor))
                (str "a whitespace-only relative path did not resolve "
                     "against the cwd: " (pr-str anchor))))
          (let [anchor (census/cli-anchor {:file "   "})]
            (is (= :file (:kind anchor))
                (str ":file \"   \" was treated as no file at all, and the "
                     "anchor silently fell back to the directory: "
                     (pr-str anchor))))))
      (finally
        (delete-tree! spaced)
        (delete-tree! parent)))))

;; ---------------------------------------------------------------------------
;; Sol's round-twelve review, item 2 (blocking): a non-decodable path silently
;; retargets.
;;
;; Sol created a directory whose name ended in the raw byte `0xff` and handed
;; it to the CLI as `:dir`. The JVM decodes argv before `-main` runs, so what
;; the op received was `…root�` — the replacement character — and the
;; rendered and vector argv agreed with each other about that corrupted path.
;; The census then answered `no-fold-arms-found` with `files-scanned 0` about
;; a directory that DOES NOT EXIST, while the directory the caller actually
;; named held one arm-bearing source. A confident count about a tree nobody
;; asked for, from a request whose target was already lost.
;;
;; The raw byte cannot be recovered: by the time any code in this process can
;; look, the decoder has already replaced it, and the replacement is lossy —
;; every undecodable byte becomes the SAME character. So there is nothing to
;; carry into a continuation and nothing to canonicalise. What is detectable
;; is the replacement character itself, and the honest answer is a typed
;; refusal that names the argument and the encoding, offers NO continuation
;; (the correct bytes cannot be rendered), and says what to do instead.
;;
;; The rule refuses a legitimately-named path containing U+FFFD too. That is
;; a stated cost, not an oversight: after decoding, a real U+FFFD and a
;; decoding failure are the same character, and a false refusal that explains
;; itself is worth more than a census of the wrong directory that cannot be
;; detected at all.
;; ---------------------------------------------------------------------------

(defn- raw-byte-fixture-script!
  "A bash script — written as BYTES — that builds a `<prefix>0xff` workspace
   and censuses it through the babashka CLI.

   The byte cannot travel through a Clojure string: this process only ever
   holds the DECODED argument, so the only way to put a non-decodable name in
   front of a JVM from inside one is to hand the bytes to `execve` through a
   file bash reads as bytes."
  [^java.io.File script prefix arm-file]
  (let [line (fn [^java.io.OutputStream out before after]
               (.write out (.getBytes ^String before "UTF-8"))
               (.write out (int 0xff))
               (.write out (.getBytes ^String after "UTF-8")))]
    (with-open [out (java.io.FileOutputStream. script)]
      (.write out (.getBytes (str "#!/bin/bash\ncd '" repo-root "'\n") "UTF-8"))
      (line out (str "mkdir -p '" prefix) "/src/app'\n")
      (line out (str "cp '" arm-file "' '" prefix) "/src/app/only.clj'\n")
      (line out (str "exec bb -cp '" repo-root "/src' -m clj-surgeon.core "
                     ":op relation-census :dir '" prefix)
            "'\n")))
  (.setExecutable script true))

;; @spec MCP-OP-CENSUS-014
;; @spec MCP-OP-CENSUS-016
(deftest a-path-that-did-not-decode-is-refused-and-never-censused
  (let [parent (temp-dir)
        arm-file (io/file parent "arm.clj")
        script (io/file parent "census.sh")
        undecodable (str (.getCanonicalPath parent) "/root\ufffd")]
    (try
      (spit-file! arm-file arm-source)
      (raw-byte-fixture-script! script (str (.getCanonicalPath parent) "/root")
                                (.getPath arm-file))

      (testing "the raw 0xff root reaches the CLI as U+FFFD and is refused"
        (let [{:keys [out exit]} (proc/shell {:out :string :err :string
                                              :continue true}
                                             "bash" (.getPath script))
              result (try (edn/read-string out) (catch Exception _ {::out out}))]
          ;; A refusal exits 1, like every other CLI refusal; what this
          ;; assertion is for is that the script RAN and produced a receipt
          ;; at all, rather than dying in the shell before bb started.
          (is (map? result)
              (str "the fixture script produced no receipt (exit " exit "): "
                   (pr-str out)))
          (is (false? (:ok result))
              (str "a census of a path that did not decode reported success: "
                   (pr-str result)))
          (is (= :dir-not-decodable (:error-type result))
              (str "the undecodable :dir was not refused; it answered "
                   (pr-str (:error-type result)) " about a directory that "
                   "does not exist: " (pr-str result)))
          (is (not (contains? result :next-command))
              (str "a continuation was offered for a path whose correct "
                   "bytes cannot be rendered: "
                   (pr-str (:next-command result))))
          (is (not (contains? result :next-command-argv)))
          (is (string? (:remedy result))
              "the refusal offers neither a continuation nor a remedy")
          (is (str/includes? (str (:remedy result)) "U+FFFD")
              (str "the remedy does not say what was detected: "
                   (pr-str (:remedy result))))
          (is (= (System/getProperty "sun.jnu.encoding") (:encoding result))
              (str "the refusal does not name the encoding that decoded the "
                   "argument: " (pr-str result)))
          (is (= ":dir" (:argument result))
              (str "the refusal does not name the argument: "
                   (pr-str result)))))

      (testing "the same rule holds in process, for every path argument"
        (let [from-dir (core/run-relation-census {:dir undecodable})
              from-file (census/validate-cli-request-shape
                          {:file (str undecodable "/src/app/only.clj")})]
          (is (= :dir-not-decodable (:error-type from-dir)))
          (is (= :dir-not-decodable (:error-type from-file))
              (str ":file was not checked for decodability: "
                   (pr-str from-file)))
          (is (= ":file" (:argument from-file))
              (str "the refusal blamed the wrong argument: "
                   (pr-str from-file)))))

      (testing "the tool refuses an undecodable workspace_root the same way"
        (let [result (run {:workspace_root undecodable})]
          (is (false? (:ok result)))
          (is (= "workspace-root-not-decodable"
                 (or (:reason result) (:error_type result)))
              (str "the tool routed on a path that did not decode: "
                   (pr-str result)))
          (is (not (contains? result :next_call))
              (str "a continuation was offered for a path whose correct "
                   "bytes cannot be rendered: " (pr-str (:next_call result))))
          (is (string? (:remedy result)))
          (is (= "workspace_root" (:argument result)))
          (is (= (System/getProperty "sun.jnu.encoding") (:encoding result)))))

      (testing "the remedy states the cost of the rule it is enforcing"
        (let [remedy (:remedy (core/run-relation-census {:dir undecodable}))]
          (is (str/includes? remedy "legitimately")
              (str "the remedy does not admit that a path which really "
                   "contains U+FFFD is refused too: " (pr-str remedy)))))
      (finally
        ;; Java cannot name the 0xff directory to delete it — encoding the
        ;; decoded U+FFFD back out yields three different bytes — so the
        ;; cleanup goes through the shell that created it.
        (proc/shell {:continue true} "rm" "-rf" (.getCanonicalPath parent))))))

;; ---------------------------------------------------------------------------
;; Sol's round-thirteen review, item 2, blocking: the decodability row was
;; FOURTH in the shared table, behind unknown-fields, `:dir`-type and nothing
;; else. So U+FFFD ALONE was refused as `dir-not-decodable` with no
;; continuation — the round-twelve fix, working — and U+FFFD BESIDE a `bogus`
;; field was refused as `unknown-arguments`/`unknown-fields`, a refusal that
;; DOES compute a continuation, and the continuation carried the corrupt path
;; straight back to the caller to replay.
;;
;; The invariant, stated once: a continuation is a NARROWING of the request it
;; answers, and a request whose path did not decode HAS no faithful narrowing
;; — the bytes are gone, the replacement is lossy, and anything carried names
;; a different directory or none. So the question "did this path survive the
;; trip into the process at all?" is asked FIRST OVERALL, ahead of every other
;; shape question, on both entrances. Not because it is the most severe
;; violation, but because every row after it computes a continuation out of a
;; value that no longer denotes anything.
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-CENSUS-014
;; @spec MCP-OP-CENSUS-016
;; @spec MCP-OP-CENSUS-029
(deftest a-path-that-did-not-decode-outranks-every-other-shape-question
  (let [corrupt (str (System/getProperty "java.io.tmpdir")
                     "/clj-surgeon-census-corrupt/root\ufffd")]

    (testing "the shared table asks decodability first, and nothing before it"
      (is (= [:paths :not-decodable]
             ((juxt :field :violation) (first census/request-shape-rules)))
          (str "a shape row was placed ahead of the decodability check, so "
               "its refusal computes a continuation out of a corrupt path: "
               (pr-str (mapv (juxt :field :violation)
                             census/request-shape-rules)))))

    (testing "an unknown ARGUMENT does not outrank a corrupt :dir"
      (let [result (binding [*out* (java.io.StringWriter.)]
                     (core/run {:op :relation-census
                                :dir corrupt
                                :format :edn}))]
        (is (= :dir-not-decodable (:error-type result))
            (str "the unknown argument won the order: " (pr-str result)))
        (is (not (contains? result :next-command))
            (str "the refusal handed back a continuation carrying the "
                 "corrupt path: " (pr-str (:next-command result))))
        (is (not (contains? result :next-command-argv)))
        (is (string? (:remedy result))
            "the refusal offers neither a continuation nor a remedy")
        (is (= ":dir" (:argument result)))))

    (testing "an unknown ARGUMENT does not outrank a corrupt :file either"
      (let [result (census/validate-cli-request-shape
                     {:file (str corrupt "/only.clj") :format :edn})]
        (is (= :dir-not-decodable (:error-type result))
            (str "the unknown argument won the order: " (pr-str result)))
        (is (= ":file" (:argument result)))
        (is (not (contains? result :next-command)))
        (is (string? (:remedy result)))))

    (testing "an unknown FIELD does not outrank a corrupt workspace_root"
      (let [result (run {:workspace_root corrupt :bogus 1})]
        (is (false? (:ok result)))
        (is (= "workspace-root-not-decodable"
               (or (:reason result) (:error_type result)))
            (str "the unknown field won the order: " (pr-str result)))
        (is (not (contains? result :next_call))
            (str "the refusal handed back a continuation carrying the "
                 "corrupt path: " (pr-str (:next_call result))))
        (is (string? (:remedy result)))
        (is (= "workspace_root" (:argument result)))))

    (testing "a corrupt path beats EVERY other row the entrance can express"
      ;; One request per remaining row, each carrying the corrupt path too.
      ;; The ordering fix is only worth anything if it holds against the whole
      ;; table rather than against the one row Sol happened to combine it with.
      (doseq [[label extra] [[:unknown-field {:bogus 1}]
                             [:doors-container {:doors "conj-once"}]
                             [:doors-entry {:doors [1]}]
                             [:doors-vocabulary {:doors ["conj"]}]
                             [:files-container {:files fixture}]
                             [:files-empty {:files []}]
                             [:files-entry {:files [""]}]
                             [:pool-size-type {:pool_size "8"}]
                             [:pool-size-range {:pool_size 0}]]]
        (let [result (run (merge {:workspace_root corrupt} extra))]
          (is (= "workspace-root-not-decodable"
                 (or (:reason result) (:error_type result)))
              (str label " won the order over a path that did not decode: "
                   (pr-str result)))
          (is (not (contains? result :next_call))
              (str label " left a continuation carrying the corrupt path: "
                   (pr-str (:next_call result)))))))

    (testing "the CLI agrees, row for row"
      (doseq [[label extra] [[:unknown-argument {:format :edn}]
                             [:dir-type {:dir ""}]
                             [:doors-container {:doors [1]}]
                             [:doors-vocabulary {:doors "conj"}]
                             [:file-entry {:file ""}]
                             [:pool-size {:threads "not-a-number"}]]]
        ;; `:dir ""` and `:file ""` REPLACE the corrupt argument they would
        ;; otherwise sit beside, so those two rows carry the corrupt path in
        ;; the OTHER path argument.
        (let [base (if (contains? extra :dir)
                     {:file (str corrupt "/only.clj")}
                     {:dir corrupt})
              result (census/validate-cli-request-shape (merge base extra))]
          (is (= :dir-not-decodable (:error-type result))
              (str label " won the order over a path that did not decode: "
                   (pr-str result)))
          (is (not (contains? result :next-command))
              (str label " left a continuation carrying the corrupt path: "
                   (pr-str (:next-command result)))))))))

;; ---------------------------------------------------------------------------
;; Sol's round-thirteen review, items 7 and 8, both blocking: a source that is
;; NOT THERE was the one request shape neither entrance answered honestly.
;;
;; At the CLI it was not typed at all. `:file /tmp/does-not-exist.clj` reached
;; `census-sources`, which stats the named path with `fs/size` before anything
;; has asked whether it exists, and the `java.nio.file.NoSuchFileException` it
;; throws surfaced through the launcher as a bare `:invalid-arguments` whose
;; entire payload was the path — no type a caller can branch on, no anchor, no
;; remedy. Existence is a filesystem question, so it cannot live in the pure
;; shape pass; it belongs at the ENTRANCE, before the scan is forced, which is
;; where every other filesystem refusal this op makes already lives.
;;
;; At the tool it was typed and the CONTINUATION was wrong, which is worse: a
;; refusal whose next_call replays into the identical refusal is not a
;; narrowing, it is a loop with a receipt. Measured before the fix, against a
;; request naming one good source and one missing one:
;;
;;   error_type "unreadable-source-path"
;;   next_call  {… :files ["src/does_not_exist.clj"]}
;;
;; — the continuation is the request narrowed to ONLY the entry that caused
;; the refusal. The rule the oversized-source branch already follows is the
;; rule here: the continuation is the same request WITHOUT the entries that
;; could not be read, every other option carried through unchanged, and when
;; removing them leaves no request there is no call to hand back and the
;; refusal says so.
;; ---------------------------------------------------------------------------

(defn- refusal-or-throw
  "Run `thunk`, reporting a thrown exception AS a receipt.

   The defect under test is an entrance that throws instead of refusing, and a
   witness that lets the throw escape reports a stack trace rather than the
   shape of the answer."
  [thunk]
  (try (thunk)
       (catch Throwable t
         {:ok false :threw (.getName (class t)) :error (.getMessage t)})))

;; @spec MCP-OP-CENSUS-014
;; @spec MCP-OP-CENSUS-016
;; @spec MCP-OP-CENSUS-019
(deftest a-source-that-is-not-there-is-a-typed-refusal-on-both-entrances
  (let [root (temp-dir)
        arm "src/app/folds.clj"
        missing "src/app/missing.clj"
        gone "src/app/gone.clj"
        absolute "/tmp/census15-fx/nope.clj"]
    (try
      (spit-file! (io/file root arm) arm-source)
      (let [named (.getCanonicalPath root)
            missing-path (str named "/" missing)
            here (fn [params]
                   (census-tool/execute-request! {:project-root named} params))]

        (testing "the JVM CLI types the missing :file instead of throwing"
          (let [result (refusal-or-throw
                         #(core/run-relation-census {:file missing-path}))]
            (is (nil? (:threw result))
                (str "the entrance threw instead of refusing: "
                     (pr-str result)))
            (is (false? (:ok result)))
            (is (= :file-not-found (:error-type result))
                (str "the missing source is not a typed refusal: "
                     (pr-str result)))
            (is (= missing-path (:file result))
                (str "the refusal does not name the path it could not find: "
                     (pr-str result)))
            (is (= missing-path (:absolute (:anchor result)))
                (str "the refusal does not name the workspace the caller "
                     "named: " (pr-str (:anchor result))))
            (is (string? (:remedy result))
                "the refusal offers neither a continuation nor a remedy")
            (is (not (contains? result :next-command))
                (str "a continuation was offered for a file that is the whole "
                     "request: " (pr-str (:next-command result))))
            (is (not (contains? result :next-command-argv)))))

        (testing "the babashka CLI answers identically"
          (let [result (bb-cli ":op" "relation-census" ":file" missing-path)]
            (is (= :file-not-found (:error-type result))
                (str "the bb entrance answered " (pr-str result)))
            (is (= missing-path (:file result)))
            (is (string? (:remedy result)))
            (is (not (contains? result :next-command)))))

        (testing "the tool's continuation is the request MINUS the missing entry"
          (let [result (here {:files [arm missing]
                              :doors ["upsert-by"]
                              :pool_size 1})]
            (is (false? (:ok result)))
            (is (= "unreadable-source-path" (:error_type result)))
            (is (= {:tool "relation_census"
                    :workspace_root named
                    :files [arm]
                    :doors ["upsert-by"]
                    :pool_size 1}
                   (:next_call result))
                (str "the continuation is not the request minus the entries "
                     "it could not read: " (pr-str (:next_call result))))))

        (testing "an out-of-fence entry is removed the same way"
          (let [result (here {:files [absolute arm]})]
            (is (= "unreadable-source-path" (:error_type result)))
            (is (= [arm] (get-in result [:next_call :files]))
                (str "the continuation kept the entry the fence refused: "
                     (pr-str (:next_call result))))))

        (testing "every named source missing leaves no request to make"
          (let [result (here {:files [missing gone]})]
            (is (= "unreadable-source-path" (:error_type result)))
            (is (not (contains? result :next_call))
                (str "the refusal hands back a call that replays into itself: "
                     (pr-str (:next_call result))))
            (is (string? (:remedy result))
                "the refusal offers neither a continuation nor a remedy")))

        (testing "a single missing source is never its own continuation"
          (let [result (here {:files [missing]})]
            (is (= "unreadable-source-path" (:error_type result)))
            (is (not= [missing] (get-in result [:next_call :files]))
                (str "the rejected request came back verbatim: "
                     (pr-str (:next_call result))))
            (is (not (contains? result :next_call))))))
      (finally (delete-tree! root)))))

;; ---------------------------------------------------------------------------
;; Sol's round-fourteen review, item 7, blocking: a source the PROCESS CANNOT
;; READ was not the same thing as a source that is not there.
;;
;; `mcp-paths/resolve-source-path` tested that the resolved path was inside the
;; fence and was a REGULAR FILE, and stopped there. A chmod-000 file passes
;; both: it exists, it is regular, its real path is under the root. So the
;; resolver returned `:ok true`, `collect-inputs` reached `slurp`, and the
;; `java.io.FileNotFoundException (Permission denied)` escaped the census
;; entirely and was caught three frames up by `exhaustion-refusal`. Measured
;; before the fix, against `files ["src/app/folds.clj" "src/app/denied.clj"]`:
;;
;;   error_type "census-adapter-failure"
;;   exhausted  false
;;   remedy     "The census ran out of a runtime resource part-way through…"
;;   next_call  (absent)
;;
;; Three separate lies in one receipt. The type says the ADAPTER broke, when
;; the adapter did exactly what it should and one named file is unreadable.
;; The remedy says a runtime resource ran out, which invites the caller to
;; retry smaller against a permission bit that will refuse identically at any
;; size. And the good source the caller also named is dropped: the continuation
;; that the missing-file case computes — the request minus the entries that
;; cannot be read — was never computed at all, because the branch that computes
;; it was never reached.
;;
;; A path the process cannot read and a path that is not there are the SAME
;; fact to a continuation: a name the next call must not carry. The resolver is
;; where that is decided for every entry, so the readability question belongs
;; there, beside the regularity question, and not in a `try` around `slurp`.
;;
;; At the CLI the same file reached `census-sources` and threw untyped, exactly
;; as the missing `:file` did in round thirteen. It gets its own type rather
;; than reusing `:file-not-found` with a cause field: the two refusals have
;; DIFFERENT remedies — "name a source that exists" versus "the source is
;; there, fix what may read it" — and a caller that must read a second field
;; to learn which remedy applies has been handed a branch, not a type. The
;; enumeration witness drives on the type name, so a distinct name is also what
;; makes this refusal impossible to ship unexercised.
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-CENSUS-014
;; @spec MCP-OP-CENSUS-016
;; @spec MCP-OP-CENSUS-019
(deftest a-source-the-process-cannot-read-is-a-typed-refusal-on-both-entrances
  (let [root (temp-dir)
        arm "src/app/folds.clj"
        denied "src/app/denied.clj"
        denied-too "src/app/denied_two.clj"
        denied-file (io/file root denied)
        denied-too-file (io/file root denied-too)]
    (try
      (spit-file! (io/file root arm) arm-source)
      (spit-file! denied-file arm-source)
      (spit-file! denied-too-file arm-source)
      (deny-reads! denied-file)
      (deny-reads! denied-too-file)
      (let [named (.getCanonicalPath root)
            denied-path (str named "/" denied)
            here (fn [params]
                   (census-tool/execute-request! {:project-root named} params))]

        (testing "the fixture is genuinely unreadable to this process"
          ;; The liveness check for the whole witness. A process running as
          ;; root reads a chmod-000 file, so without this assertion every
          ;; probe below would pass by censusing a file it was supposed to be
          ;; refused, and the witness would be green and blind.
          (is (false? (.canRead denied-file))
              "this process can still read the chmod-000 fixture")
          (is (false? (.canRead denied-too-file))
              "this process can still read the chmod-000 fixture"))

        (testing "the tool types the denied source instead of blaming the adapter"
          (let [result (here {:files [arm denied]
                              :doors ["upsert-by"]
                              :pool_size 1})]
            (is (false? (:ok result)))
            (is (not= "census-adapter-failure" (:error_type result))
                (str "a permission bit was reported as an adapter crash: "
                     (pr-str (select-keys result
                                          [:error_type :error :exhausted
                                           :remedy]))))
            (is (= "unreadable-source-path" (:error_type result))
                (str "the denied source is not the typed refusal the missing "
                     "one earns: " (pr-str result)))
            (is (not (contains? result :exhausted))
                (str "the refusal claims a resource-exhaustion fact about a "
                     "permission bit: " (pr-str (:exhausted result))))
            (is (= denied (:file result))
                (str "the refusal does not name the source it could not read: "
                     (pr-str result)))))

        (testing "the continuation is the request MINUS the denied entry"
          (let [result (here {:files [arm denied]
                              :doors ["upsert-by"]
                              :pool_size 1})]
            (is (= {:tool "relation_census"
                    :workspace_root named
                    :files [arm]
                    :doors ["upsert-by"]
                    :pool_size 1}
                   (:next_call result))
                (str "the continuation is not the request minus the entries "
                     "it could not read: " (pr-str (:next_call result))))
            (is (= [denied] (:files_removed result))
                (str "the refusal does not name what it removed: "
                     (pr-str (:files_removed result))))
            (is (= 0 (:files_removed_omitted result)))))

        (testing "every named source denied leaves no request to make"
          (let [result (here {:files [denied denied-too]})]
            (is (= "unreadable-source-path" (:error_type result))
                (str "the all-denied list is not the typed refusal the "
                     "all-missing one earns: " (pr-str result)))
            (is (not (contains? result :next_call))
                (str "the refusal hands back a call that replays into itself: "
                     (pr-str (:next_call result))))
            (is (string? (:remedy result))
                "the refusal offers neither a continuation nor a remedy")
            (is (not (str/includes? (str (:remedy result)) "ran out"))
                (str "the remedy invites a smaller retry against a permission "
                     "bit: " (pr-str (:remedy result))))
            (is (= [denied denied-too] (:files_removed result))
                (str "the refusal does not name both denied sources: "
                     (pr-str (:files_removed result))))))

        (testing "the JVM CLI types the denied :file instead of throwing"
          (let [result (refusal-or-throw
                         #(core/run-relation-census {:file denied-path}))]
            (is (nil? (:threw result))
                (str "the entrance threw instead of refusing: "
                     (pr-str result)))
            (is (false? (:ok result)))
            (is (= :file-not-readable (:error-type result))
                (str "the denied source is not a typed refusal: "
                     (pr-str result)))
            (is (= denied-path (:file result))
                (str "the refusal does not name the path it could not read: "
                     (pr-str result)))
            (is (= denied-path (:absolute (:anchor result)))
                (str "the refusal does not name the workspace the caller "
                     "named: " (pr-str (:anchor result))))
            (is (string? (:remedy result))
                "the refusal offers neither a continuation nor a remedy")
            (is (not (contains? result :next-command))
                (str "a continuation was offered for a file that is the whole "
                     "request: " (pr-str (:next-command result))))
            (is (not (contains? result :next-command-argv)))))

        (testing "the babashka CLI answers identically"
          (let [result (bb-cli ":op" "relation-census" ":file" denied-path)]
            (is (= :file-not-readable (:error-type result))
                (str "the bb entrance answered " (pr-str result)))
            (is (= denied-path (:file result)))
            (is (string? (:remedy result)))
            (is (not (contains? result :next-command))))))
      ;; Sol's round-fifteen item 8: this witness drives `:file` and a
      ;; babashka `:file` subprocess only, which is why it was green while the
      ;; `:dir` WALK still read whatever it was handed. The `:dir` drives, and
      ;; the two shapes this one never names — a FIFO and a directory carrying
      ;; a source name — live in
      ;; `every-path-the-census-reads-passes-one-fence-before-any-open` and
      ;; `a-named-pipe-is-refused-before-any-open-on-both-entrances`.
      (finally
        (allow-reads! denied-file)
        (allow-reads! denied-too-file)
        (delete-tree! root)))))
;; ---------------------------------------------------------------------------
;; Sol's round-fifteen review, items 1/2/5/7 (blocking): round fourteen fenced
;; the CLI's single-`:file` branch and left the `:dir` WALK — the ordinary
;; invocation — reading whatever the discovery kernel handed it.
;;
;;   $ bb -m clj-surgeon.core :op :relation-census :dir <tree with a chmod-000 source>
;;   {:error "…/denied.clj (Permission denied)", :error-type :invalid-arguments}
;;
;; Untyped, no anchor, no remedy, no continuation — and `:invalid-arguments` is
;; in neither `cli-refusal-types` nor `mcp-refusal-types`, so both enumeration
;; witnesses are blind to it. The tool, given the SAME tree, answers
;; `unreadable-source-path` naming the member and saying what to do.
;;
;; Two more shapes reach the same bare `(slurp p)`:
;;
;;   - a FIFO named `*.clj`. `fs/readable?` is true of a named pipe and `slurp`
;;     BLOCKS on it with no writer: 30 seconds, zero bytes, EXIT 124, no
;;     diagnostic. One named pipe anywhere under `:dir` wedges the census. The
;;     tool refuses the same path in milliseconds, before any open.
;;   - a readable file under a chmod-000 PARENT. `fs/exists?` is false, because
;;     it cannot stat through the parent, so the entrance answers
;;     `:file-not-found` — "name a source that exists" — about a file that is
;;     right there. This is the `file-not-found`/`file-not-readable` confusion
;;     commit 1038893 was written to end, reproduced by moving the permission
;;     bit one directory up.
;;
;; So the rule is not "the `:file` branch asks the readability question"; it is
;; that EVERY path this op reads — the `:file` the caller named and every
;; member the walk discovered alike — passes through ONE fence before any open,
;; and that fence asks the same four questions the tool's resolver asks:
;; existence, regular-file (FOLLOWED), readable, and — when the answer is "not
;; there" — whether an ancestor directory is what this process may not read.
;; ---------------------------------------------------------------------------

(defn- bounded
  "Run `thunk` on a DAEMON thread under a deadline; `::timeout` when it blocks.

   The defect under test is an entrance that blocks forever on a named pipe,
   and a witness that simply calls it would hang the suite rather than fail it.
   The thread is a daemon precisely so the FAILING run still exits: a blocked
   `slurp` on a FIFO does not answer an interrupt, so the thread cannot be
   reclaimed and must instead be made unable to hold the JVM open."
  [ms thunk]
  (let [answer (promise)]
    (doto (Thread.
            ^Runnable
            (fn []
              (deliver answer
                       (try (thunk)
                            (catch Throwable t
                              {:ok false
                               :threw (.getName (class t))
                               :error (.getMessage t)})))))
      (.setDaemon true)
      (.start))
    (deref answer ms ::timeout)))

(defn- bb-cli-bounded
  "The babashka CLI as a subprocess under a deadline, killed if it blocks.

   `bb-cli` blocks on `proc/shell`; a FIFO drive through it never returns. This
   starts the process, waits `ms`, and force-destroys the process IT started."
  [ms & args]
  (let [started (apply proc/process {:out :string :err :string}
                       "bb" "-cp" (str repo-root "/src")
                       "-m" "clj-surgeon.core" args)
        ^Process handle (:proc started)]
    (if (.waitFor handle ms java.util.concurrent.TimeUnit/MILLISECONDS)
      (let [{:keys [out exit]} @started]
        (try (assoc (edn/read-string out) ::exit exit)
             (catch Exception _ {::exit exit ::out out})))
      (do (.destroyForcibly handle)
          (.waitFor handle 5 java.util.concurrent.TimeUnit/SECONDS)
          ::timeout))))

(defn- mkfifo!
  "A named pipe at `path`, or nil when this box has no mkfifo."
  [path]
  (let [{:keys [exit]} (proc/shell {:out :string :err :string :continue true}
                                   "mkfifo" (str path))]
    (when (zero? exit) (str path))))

;; @spec MCP-OP-CENSUS-014
;; @spec MCP-OP-CENSUS-016
;; @spec MCP-OP-CENSUS-019
(deftest every-path-the-census-reads-passes-one-fence-before-any-open
  (let [parent (temp-dir)
        arm "src/app/folds.clj"
        denied "src/app/denied.clj"
        dirfile "src/app/dirfile.clj"
        inner "src/app/locked/inner.clj"
        denied-tree (io/file parent "denied-tree")
        plain (io/file parent "plain")
        denied-file (io/file denied-tree denied)
        locked-dir (io/file plain "src/app/locked")
        inner-file (io/file plain inner)]
    (try
      (spit-file! (io/file denied-tree arm) arm-source)
      (spit-file! denied-file arm-source)
      (spit-file! (io/file plain arm) arm-source)
      (spit-file! inner-file arm-source)
      (.mkdirs (io/file plain dirfile))
      (deny-reads! denied-file)
      (deny-traversal! locked-dir)
      (let [denied-root (.getCanonicalPath denied-tree)
            plain-root (.getCanonicalPath plain)
            declared (into census/cli-refusal-types census/mcp-refusal-types)]

        (testing "the fixtures are genuinely refused to this process"
          ;; The liveness check for the whole witness: a process running as
          ;; root reads a chmod-000 file and lists a chmod-000 directory, and
          ;; every probe below would then pass by censusing what it was
          ;; supposed to refuse.
          (is (false? (.canRead denied-file))
              "this process can still read the chmod-000 fixture")
          (is (false? (.canRead locked-dir))
              "this process can still read the chmod-000 parent"))

        (testing "the :dir walk types a chmod-000 member instead of throwing"
          (let [result (refusal-or-throw
                         #(core/run-relation-census {:dir denied-root}))]
            (is (nil? (:threw result))
                (str "the walk threw instead of refusing: " (pr-str result)))
            (is (false? (:ok result)))
            (is (not= :invalid-arguments (:error-type result))
                (str "a permission bit escaped the op untyped: "
                     (pr-str result)))
            (is (contains? declared (:error-type result))
                (str "the walk refused " (pr-str (:error-type result))
                     ", which no declared refusal set contains"))
            (is (= :file-not-readable (:error-type result))
                (str "the denied member is not the typed refusal the denied "
                     ":file earns: " (pr-str result)))
            (is (= denied (:file result))
                (str "the refusal does not name the member it could not "
                     "read: " (pr-str result)))
            (is (= denied-root (:absolute (:anchor result)))
                (str "the refusal lost the workspace the caller named: "
                     (pr-str (:anchor result))))
            (is (string? (:remedy result))
                "the refusal offers neither a continuation nor a remedy")))

        (testing "the babashka :dir walk answers identically"
          (let [result (bb-cli ":op" "relation-census" ":dir" denied-root)]
            (is (= :file-not-readable (:error-type result))
                (str "the bb entrance answered " (pr-str result)))
            (is (= denied (:file result)))
            (is (string? (:remedy result)))))

        (testing "the tool, given the same tree, answers the same way"
          (let [result (census-tool/execute-request!
                         {:project-root denied-root} {})]
            (is (= "unreadable-source-path" (:error_type result))
                (str "the tool answered " (pr-str result)))
            (is (= denied (:file result)))))

        (testing "a directory named *.clj is refused, not opened"
          (let [result (refusal-or-throw
                         #(core/run-relation-census
                            {:file (str plain-root "/" dirfile)}))]
            (is (nil? (:threw result))
                (str "the entrance threw instead of refusing: "
                     (pr-str result)))
            (is (not= :invalid-arguments (:error-type result))
                (str "a directory named *.clj escaped the op untyped: "
                     (pr-str result)))
            (is (= :file-not-a-regular-file (:error-type result))
                (str "a directory named *.clj is not typed as one: "
                     (pr-str result)))
            (is (= (str plain-root "/" dirfile) (:file result)))
            (is (string? (:remedy result)))))

        (testing "a readable file under a denied PARENT names the parent"
          (let [result (refusal-or-throw
                         #(core/run-relation-census
                            {:file (str plain-root "/" inner)}))]
            (is (nil? (:threw result)))
            (is (not= :file-not-found (:error-type result))
                (str "a file that IS there was refused as missing: "
                     (pr-str result)))
            (is (= :file-not-readable (:error-type result))
                (str "the denied parent is not typed as unreadable: "
                     (pr-str result)))
            (is (= :parent-denied (:cause result))
                (str "the refusal does not say WHY it cannot be read: "
                     (pr-str result)))
            (is (str/includes? (str (:error result))
                               (.getCanonicalPath locked-dir))
                (str "the refusal names the path but not the parent that is "
                     "what must change: " (pr-str (:error result))))
            (is (str/includes? (str (:remedy result))
                               (.getCanonicalPath locked-dir))
                (str "the remedy does not name the parent: "
                     (pr-str (:remedy result))))))

        (testing "the tool names the denied parent too, not just the path"
          ;; Sol's round-fifteen item 9, the tool half. It TYPES the refusal
          ;; correctly and then says nothing: `AccessDeniedException.getMessage`
          ;; IS the path, and it was passed through unlabelled, so the error
          ;; read "/abs/src/app/locked/inner.clj (src/app/locked/inner.clj)" —
          ;; the path twice, once absolutely, and not one word about what may
          ;; not be read or why. Every other refusal this resolver publishes
          ;; names paths project-relative; leaking the absolute one is the
          ;; signature of a message nobody wrote.
          (let [result (census-tool/execute-request!
                         {:project-root plain-root} {:files [inner]})]
            (is (= "unreadable-source-path" (:error_type result))
                (str "the tool answered " (pr-str result)))
            (is (not (str/includes? (str (:error result)) plain-root))
                (str "the error is the raw exception path, not a message: "
                     (pr-str (:error result))))
            (is (str/includes? (str (:error result)) "src/app/locked")
                (str "the error does not name the parent: "
                     (pr-str (:error result))))
            (is (str/includes? (str (:error result)) "directory")
                (str "the error does not say a DIRECTORY is what may not be "
                     "read: " (pr-str (:error result))))))

        (testing "the tool refuses the same shapes through files"
          ;; Sol's round-fifteen item 8: the round-fourteen witness drove the
          ;; CLI through `:file` and a babashka `:file` subprocess and the tool
          ;; through one denied entry, which is why it was green over all of
          ;; this. The three shapes are driven at BOTH entrances or the class
          ;; recurs at whichever one was left out.
          (let [here (fn [params]
                       (census-tool/execute-request!
                         {:project-root plain-root} params))]
            (doseq [[label bad] [[:a-directory-named-clj dirfile]
                                 [:a-file-under-a-denied-parent inner]]]
              (let [result (here {:files [arm bad]
                                  :doors ["upsert-by"]
                                  :pool_size 1})]
                (is (= "unreadable-source-path" (:error_type result))
                    (str label " answered " (pr-str result)))
                (is (= bad (:file result))
                    (str label " does not name the entry: " (pr-str result)))
                (is (= [bad] (:files_removed result))
                    (str label " does not name what it removed: "
                         (pr-str (:files_removed result))))
                (is (= {:tool "relation_census"
                        :workspace_root plain-root
                        :files [arm]
                        :doors ["upsert-by"]
                        :pool_size 1}
                       (:next_call result))
                    (str label " lost the readable remainder: "
                         (pr-str (:next_call result))))))))

        (testing "a directory named *.clj cannot arrive from the walk at all"
          ;; The one shape of the three that the WALK cannot produce, asserted
          ;; rather than assumed: `census-discovery` asks whether an entry is a
          ;; real directory BEFORE it asks whether the name looks like a
          ;; source, so a directory called `*.clj` is descended and never
          ;; becomes a candidate. It reaches the census only when a caller
          ;; NAMES it, which is the drive above.
          (is (not (contains? (set (:files (census-discovery/discover
                                             plain-root)))
                              dirfile))
              (str "the walk yielded a directory as a candidate source: "
                   (pr-str (:files (census-discovery/discover plain-root)))))))
      (finally
        (allow-reads! denied-file)
        (allow-traversal! locked-dir)
        (delete-tree! parent)))))

;; @spec MCP-OP-CENSUS-014
;; @spec MCP-OP-CENSUS-019
(deftest a-named-pipe-is-refused-before-any-open-on-both-entrances
  (let [parent (temp-dir)
        arm "src/app/folds.clj"
        pipe "src/app/pipe.clj"
        tree (io/file parent "fifo-tree")
        pipe-path (str (.getCanonicalPath tree) "/" pipe)]
    (try
      (spit-file! (io/file tree arm) arm-source)
      (.mkdirs (.getParentFile (io/file pipe-path)))
      (if-not (mkfifo! pipe-path)
        (is false "this box has no mkfifo, so the FIFO witness cannot run")
        (let [root (.getCanonicalPath tree)
              declared (into census/cli-refusal-types census/mcp-refusal-types)]

          (testing "the fixture is a named pipe that fs/readable? admits"
            (is (true? (Files/isReadable (.toPath (io/file pipe-path))))
                "the fixture is not readable, so it does not reproduce")
            (is (false? (Files/isRegularFile (.toPath (io/file pipe-path))
                                             (make-array java.nio.file.LinkOption 0)))
                "the fixture is a regular file, so it does not reproduce"))

          ;; Warm the fence path so the timing assertion measures the fence
          ;; and not class loading.
          (core/run-relation-census {:file (str root "/does-not-exist.clj")})

          (testing "a :file FIFO is refused, not opened"
            (let [started (System/nanoTime)
                  result (bounded 5000
                                  #(core/run-relation-census
                                     {:file pipe-path}))
                  elapsed (/ (- (System/nanoTime) started) 1e6)]
              (is (not= ::timeout result)
                  (str "the entrance blocked on a named pipe for 5 s: "
                       pipe-path))
              (when (not= ::timeout result)
                (is (contains? declared (:error-type result))
                    (str "the FIFO refused " (pr-str (:error-type result))
                         ", which no declared refusal set contains"))
                (is (= :file-not-a-regular-file (:error-type result))
                    (str "a named pipe is not typed as a non-regular file: "
                         (pr-str result)))
                (is (< elapsed 100.0)
                    (str "the refusal took " elapsed
                         " ms, which is an open and not a stat")))))

          (testing "a :dir walk over a FIFO is refused, not wedged"
            (let [result (bounded 5000
                                  #(core/run-relation-census {:dir root}))]
              (is (not= ::timeout result)
                  (str "one named pipe wedged the whole :dir census: " root))
              (when (not= ::timeout result)
                (is (= :file-not-a-regular-file (:error-type result))
                    (str "the walk answered " (pr-str result)))
                (is (= pipe (:file result))
                    (str "the refusal does not name the member: "
                         (pr-str result))))))

          (testing "the babashka :dir walk is refused, not wedged"
            (let [result (bb-cli-bounded 30000 ":op" "relation-census"
                                         ":dir" root)]
              (is (not= ::timeout result)
                  "the babashka entrance blocked on a named pipe for 30 s")
              (when (not= ::timeout result)
                (is (= :file-not-a-regular-file (:error-type result))
                    (str "the bb entrance answered " (pr-str result))))))

          (testing "the tool refuses the same three shapes through files"
            (let [here (fn [params]
                         (census-tool/execute-request!
                           {:project-root root} params))]
              (doseq [[label params] [[:fifo {:files [arm pipe]}]
                                      [:denied-absent {:files [arm "src/app/nope.clj"]}]]]
                (let [result (bounded 5000 #(here params))]
                  (is (not= ::timeout result)
                      (str label " blocked the tool"))
                  (when (not= ::timeout result)
                    (is (= "unreadable-source-path" (:error_type result))
                        (str label " answered " (pr-str result)))
                    (is (= {:tool "relation_census"
                            :workspace_root root
                            :files [arm]}
                           (:next_call result))
                        (str label " lost the readable remainder: "
                             (pr-str (:next_call result)))))))))))
      (finally
        (delete-tree! parent)))))

;; ---------------------------------------------------------------------------
;; Sol's round-fifteen item 6, second half, and the NO-GO list's item 3:
;; `:error-type :invalid-arguments` is in NEITHER `cli-refusal-types` NOR
;; `mcp-refusal-types`, so both enumeration witnesses are blind to every
;; refusal that reaches the launcher's catch-all at the bottom of `-main`.
;;
;; That is the general form of the `:dir` defect rather than the defect
;; itself: while ANY throw from the census path can leave the op untyped, the
;; declared sets describe a subset of what the op can emit, every witness
;; pinned to them is green over the rest, and the next round's escape has the
;; same signature — a bare `{:error "<a message>", :error-type
;; :invalid-arguments}` with no anchor, no remedy and no continuation.
;;
;; So the enumeration is made TOTAL: the op catches what escapes it, names it
;; from the declared set, and this witness drives that catch site directly
;; with an exception the op has never heard of. `:census-adapter-failure` and
;; `:census-resource-exhausted` are the names the MCP entrance already
;; publishes for exactly these two cases, which is the point — a throw is not
;; a different KIND of event at the two entrances.
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-CENSUS-014
;; @spec MCP-OP-CENSUS-017
(deftest nothing-thrown-on-the-census-path-escapes-the-op-untyped
  (let [root (temp-dir)]
    (try
      (spit-file! (io/file root "src/app/folds.clj") arm-source)
      (let [named (.getCanonicalPath root)]
        (doseq [[label thrower expected]
                [[:an-exception-the-op-has-never-heard-of
                  #(throw (ex-info "injected" {:sol :round-fifteen}))
                  :census-adapter-failure]
                 [:a-runtime-resource-exhaustion
                  #(throw (OutOfMemoryError. "injected"))
                  :census-resource-exhausted]]]
          (testing (str label " is refused as a declared type")
            (let [result (refusal-or-throw
                           #(with-redefs [census-discovery/discover
                                          (fn [& _] (thrower))]
                              (core/run-relation-census {:dir named})))]
              (is (nil? (:threw result))
                  (str label " escaped the op as a throw: " (pr-str result)))
              (is (not= :invalid-arguments (:error-type result))
                  (str label " reached the launcher's catch-all: "
                       (pr-str result)))
              (is (contains? census/cli-refusal-types (:error-type result))
                  (str label " refused " (pr-str (:error-type result))
                       ", which `cli-refusal-types` does not declare, so no "
                       "enumeration witness can see it"))
              (is (= expected (:error-type result))
                  (str label " refused " (pr-str (:error-type result))))
              (is (= named (:absolute (:anchor result)))
                  (str label " lost the workspace the caller named: "
                       (pr-str (:anchor result))))
              (is (string? (:remedy result))
                  (str label " offers neither a continuation nor a remedy: "
                       (pr-str result)))
              (is (not (contains? result :next-command))
                  (str label " handed back a continuation computed from a "
                       "walk whose aggregates were lost with it: "
                       (pr-str (:next-command result)))))))

        (testing "the dispatch entrance answers the same way"
          (let [result (refusal-or-throw
                         #(with-redefs [census-discovery/discover
                                        (fn [& _]
                                          (throw (ex-info "injected" {})))]
                            (binding [*out* (java.io.StringWriter.)]
                              (core/run {:op :relation-census :dir named}))))]
            (is (nil? (:threw result))
                (str "the dispatch entrance threw: " (pr-str result)))
            (is (= :census-adapter-failure (:error-type result))
                (str "the dispatch entrance answered " (pr-str result))))))
      (finally
        (delete-tree! root)))))

;; ---------------------------------------------------------------------------
;; Sol's round-fifteen item 5 and NO-GO item 4: `isReadable` is a CHECK, and
;; the read happens somewhere else.
;;
;; `mcp-paths/resolve-source-path` answers "may this process read it" and
;; `collect-inputs` performs the `slurp`, with nothing between them and no
;; typed catch around it. Sol's storm — twenty thousand in-process requests
;; naming one file whose mode a second thread flipped:
;;
;;   RACE RESULTS: {"unreadable-source-path" 10085, :OK 5308,
;;                  "census-adapter-failure" 4607}
;;
;; and the payload of one of those 4,607 is round fourteen's REJECTED receipt,
;; verbatim: `census-adapter-failure`, `exhausted false`, and a remedy telling
;; the caller a runtime resource ran out and to retry smaller — against a
;; permission bit that refuses identically at any size.
;;
;; It is not only a permission race. The same window opens for an ordinary
;; editor's atomic save — `spit` to `.tmp`, `renameTo`, with no `chmod`
;; anywhere — which Sol measured at 38 in 20,000.
;;
;; Moving the CHECK earlier does not remove the need for a typed catch at the
;; READ: the class narrowed, it did not close. A read that fails after the
;; fence is the same fact to a continuation as one the fence refused — a name
;; the next call must not carry — so it answers alike, and the entry that
;; tripped the reader is removed from the narrowing whether the fence saw it or
;; not.
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-CENSUS-014
;; @spec MCP-OP-CENSUS-017
(deftest a-read-that-fails-after-the-fence-is-never-an-adapter-crash
  (let [root (temp-dir)
        arm "src/app/folds.clj"
        race "src/app/race.clj"
        racing (io/file root race)
        ;; Sol drove 20,000. The mode-flip window reproduces on roughly a
        ;; quarter of runs, so 2,000 is several hundred hits and costs a few
        ;; seconds. The assertion is one-sided — ZERO adapter failures — so a
        ;; green run is never a lucky one, and the liveness assertion below
        ;; refuses to pass a run in which the racer never won.
        storm 2000]
    (try
      (spit-file! (io/file root arm) arm-source)
      (spit-file! racing arm-source)
      (let [named (.getCanonicalPath root)
            here (fn [params]
                   (census-tool/execute-request! {:project-root named} params))
            answer (fn [] (let [r (here {:files [race arm]
                                         :doors ["upsert-by"]
                                         :pool_size 1})]
                            (if (:ok r) :ok (:error_type r))))]

        (testing "a mode flipped between the check and the read is not an adapter crash"
          (let [stop (atom false)
                flipper (doto (Thread.
                                ^Runnable
                                (fn []
                                  (while (not @stop)
                                    (.setReadable racing false false)
                                    (.setReadable racing true true))))
                          (.setDaemon true)
                          (.start))
                counts (try (frequencies (repeatedly storm answer))
                            (finally (reset! stop true)
                                     (.join flipper 5000)
                                     (.setReadable racing true true)))]
            (is (pos? (- storm (get counts :ok 0)))
                (str "the flipper never won a race, so this run proves "
                     "nothing: " (pr-str counts)))
            (is (zero? (get counts "census-adapter-failure" 0))
                (str "a permission bit was reported as an adapter crash "
                     (get counts "census-adapter-failure" 0) " times in "
                     storm ": " (pr-str counts)))
            (is (zero? (get counts "census-resource-exhausted" 0))
                (str "a permission bit was reported as a resource "
                     "exhaustion: " (pr-str counts)))))

        (testing "an atomic rename between the check and the read is not one either"
          (let [stop (atom false)
                tmp (io/file root "src/app/race.clj.tmp")
                saver (doto (Thread.
                              ^Runnable
                              (fn []
                                (while (not @stop)
                                  (spit tmp arm-source)
                                  (.renameTo tmp racing))))
                        (.setDaemon true)
                        (.start))
                counts (try (frequencies (repeatedly storm answer))
                            (finally (reset! stop true)
                                     (.join saver 5000)
                                     (spit racing arm-source)))]
            (is (zero? (get counts "census-adapter-failure" 0))
                (str "an ordinary editor's atomic save was reported as an "
                     "adapter crash " (get counts "census-adapter-failure" 0)
                     " times in " storm ": " (pr-str counts)))))

        (testing "the entry that tripped the READER is narrowed away exactly as one the fence refused"
          ;; The race above proves the CLASS is gone; this proves what the
          ;; answer says, deterministically, with no race at all: the reader
          ;; fails on one named entry and the refusal must remove THAT entry
          ;; from the continuation. It cannot fall back on re-resolving the
          ;; list through the fence, because the fence says every entry is
          ;; fine — which would hand back the identical request, a loop with a
          ;; receipt.
          (let [original slurp
                result (with-redefs
                         [slurp (fn [& args]
                                  (if (str/includes? (str (first args))
                                                     "race.clj")
                                    (throw (java.io.FileNotFoundException.
                                             "injected (Permission denied)"))
                                    (apply original args)))]
                         (here {:files [race arm]
                                :doors ["upsert-by"]
                                :pool_size 1}))]
            (is (= "unreadable-source-path" (:error_type result))
                (str "a read that failed after the fence answered "
                     (pr-str (select-keys result [:error_type :error :exhausted
                                                  :remedy]))))
            (is (not (contains? result :exhausted))
                (str "the refusal claims a resource-exhaustion fact about a "
                     "read: " (pr-str (:exhausted result))))
            (is (= race (:file result))
                (str "the refusal does not name the entry the reader tripped "
                     "on: " (pr-str result)))
            (is (= [race] (:files_removed result))
                (str "the refusal does not name what it removed: "
                     (pr-str (:files_removed result))))
            (is (= {:tool "relation_census"
                    :workspace_root named
                    :files [arm]
                    :doors ["upsert-by"]
                    :pool_size 1}
                   (:next_call result))
                (str "the continuation is not the request minus the entry "
                     "that could not be read: " (pr-str (:next_call result))))
            (is (not (str/includes? (str (:remedy result)) "ran out"))
                (str "the remedy invites a smaller retry against a read that "
                     "failed: " (pr-str (:remedy result))))))

        ;; -------------------------------------------------------------------
        ;; Opus's round-sixteen NO-GO items 1 and 3, blocking. The round that
        ;; closed this class closed it at ONE entrance. `collect-inputs` took
        ;; the typed catch; `core/census-sources` kept its bare `(slurp p)`,
        ;; and the identical storm through the CLI answered
        ;;
        ;;   {:file-not-readable 16523, :census-adapter-failure 1623, :OK 1854}
        ;;
        ;; — 1,623 in 20,000 carrying round fourteen's rejected receipt, and
        ;; carrying it to a request that named ONE file with a remedy telling
        ;; it to point `:dir` at a smaller directory. The witness above is
        ;; green over all of it because it drives `execute-request!` only,
        ;; which is the same blindness as the round-fourteen witness that drove
        ;; only `:file`. A rule proved at one entrance while the EARS text
        ;; states it for both is what authorises the next round to move on.
        ;; -------------------------------------------------------------------
        (testing "the CLI's read after the fence is not an adapter crash either"
          (let [named-file (.getCanonicalPath racing)
                stop (atom false)
                flipper (doto (Thread.
                                ^Runnable
                                (fn []
                                  (while (not @stop)
                                    (.setReadable racing false false)
                                    (.setReadable racing true true))))
                          (.setDaemon true)
                          (.start))
                cli-answer (fn []
                             (let [r (refusal-or-throw
                                       #(core/run-relation-census
                                          {:file named-file}))]
                               (cond
                                 (:threw r) (:threw r)
                                 (:ok r) :ok
                                 :else (:error-type r))))
                counts (try (frequencies (repeatedly storm cli-answer))
                            (finally (reset! stop true)
                                     (.join flipper 5000)
                                     (.setReadable racing true true)))]
            (is (pos? (- storm (get counts :ok 0)))
                (str "the flipper never won a race at the CLI, so this run "
                     "proves nothing: " (pr-str counts)))
            (is (zero? (get counts :census-adapter-failure 0))
                (str "the CLI reported a permission bit as an adapter crash "
                     (get counts :census-adapter-failure 0) " times in "
                     storm ": " (pr-str counts)))
            (is (zero? (get counts :census-resource-exhausted 0))
                (str "the CLI reported a permission bit as a resource "
                     "exhaustion: " (pr-str counts)))
            (is (nil? (some #(when (string? %) %) (keys counts)))
                (str "the CLI threw instead of refusing: " (pr-str counts)))))

        (testing "the CLI names the actual cause, not a resource exhaustion"
          ;; Deterministic, no race: the reader fails on the one file the
          ;; request named. The refusal must answer as the fence answers a
          ;; path it refuses — `:file-not-readable`, naming the source the
          ;; caller named — and its remedy must be about a read that failed,
          ;; never the walk's lost aggregates and never "point :dir at a
          ;; directory you know is smaller" handed to a one-file request.
          (let [named-file (.getCanonicalPath racing)
                original slurp
                result (refusal-or-throw
                         #(with-redefs
                            [slurp (fn [& args]
                                     (if (str/includes? (str (first args))
                                                        "race.clj")
                                       (throw (java.io.FileNotFoundException.
                                                "injected (Permission denied)"))
                                       (apply original args)))]
                            (core/run-relation-census {:file named-file})))]
            (is (nil? (:threw result))
                (str "the CLI threw instead of refusing: " (pr-str result)))
            (is (= :file-not-readable (:error-type result))
                (str "a read that failed after the CLI's fence answered "
                     (pr-str (select-keys result [:error-type :error :exhausted
                                                  :remedy]))))
            (is (= :read-failed-after-fence (:cause result))
                (str "the refusal does not say WHY the read failed: "
                     (pr-str result)))
            (is (= named-file (:file result))
                (str "the refusal does not name the source the caller named: "
                     (pr-str (:file result))))
            (is (not (contains? result :exhausted))
                (str "the refusal claims a resource-exhaustion fact about a "
                     "read: " (pr-str (:exhausted result))))
            (is (not (str/includes? (str (:remedy result))
                                    "directory you know is smaller"))
                (str "a one-file request is told to point :dir somewhere "
                     "smaller: " (pr-str (:remedy result))))
            (is (str/includes? (str (:remedy result)) named-file)
                (str "the remedy does not name the source that could not be "
                     "read: " (pr-str (:remedy result))))
            (is (contains? (into census/cli-refusal-types
                                 census/mcp-refusal-types)
                           (:error-type result))
                (str "the CLI refused " (pr-str (:error-type result))
                     ", which no declared refusal set contains"))))

        (testing "a walk member whose read fails answers with the walk's remedy"
          ;; The same fact reached by the OTHER provenance: the path came from
          ;; the `:dir` walk, so the remedy is the walk's — remove or repair it
          ;; — and the file is named project-relative, exactly as a member the
          ;; fence refuses is named.
          (let [named (.getCanonicalPath root)
                original slurp
                result (refusal-or-throw
                         #(with-redefs
                            [slurp (fn [& args]
                                     (if (str/includes? (str (first args))
                                                        "race.clj")
                                       (throw (java.io.FileNotFoundException.
                                                "injected (Permission denied)"))
                                       (apply original args)))]
                            (core/run-relation-census {:dir named})))]
            (is (nil? (:threw result))
                (str "the walk threw instead of refusing: " (pr-str result)))
            (is (= :file-not-readable (:error-type result))
                (str "a walk member whose read failed answered "
                     (pr-str (select-keys result [:error-type :error
                                                  :remedy]))))
            (is (= :read-failed-after-fence (:cause result))
                (str "the refusal does not say WHY the read failed: "
                     (pr-str result)))
            (is (= race (:file result))
                (str "the refusal does not name the member project-relative: "
                     (pr-str (:file result))))
            (is (str/includes? (str (:remedy result))
                               "came from the workspace walk")
                (str "the walk's refusal does not say where the path came "
                     "from: " (pr-str (:remedy result)))))))
      (finally
        (delete-tree! root)))))

;; ---------------------------------------------------------------------------
;; Opus's round-sixteen NO-GO item 2, blocking. Round sixteen closed the
;; raw-exception-text leak for `AccessDeniedException` by matching its class
;; NAME, and wrote the rule into the spec GLOBALLY — "a refusal that names a
;; path shall not publish the raw text of the exception that produced it as its
;; explanation" — while its sibling `FileSystemException` still walked into the
;; generic catch and published the server's absolute root:
;;
;;   error_type = "unreadable-source-path"
;;   error = "/tmp/census17-sol-fx/workspace/src/app/loopa.clj: Too many levels
;;            of symbolic links or unable to access attributes of symbolic link
;;            (src/app/loopa.clj)"
;;
;; A symlink loop is a shape that occurs in real repositories, and a name too
;; long is another; both throw `FileSystemException` and neither is an
;; `AccessDeniedException`. The predicate was one class too narrow.
;;
;; The same drive is also where the two entrances were caught disagreeing about
;; WHICH FACT they had observed. `fs/exists?` follows links, so the CLI calls a
;; loop and an over-long name what they are — a path that does not resolve —
;; while the tool called them "unreadable" and printed the exception. A `cause`
;; both entrances publish from one vocabulary is what makes that comparable at
;; all: the entrance-specific NAMES differ by MCP-OP-CENSUS-014's own design,
;; and a witness that can only compare names cannot see this class.
;; ---------------------------------------------------------------------------

(defn- symlink!
  [^java.io.File from ^java.io.File to]
  (Files/createSymbolicLink (.toPath from) (.toPath to)
                            (make-array FileAttribute 0)))

;; @spec MCP-OP-CENSUS-014
;; @spec MCP-OP-CENSUS-019
(deftest no-refusal-publishes-the-raw-text-of-the-exception-that-produced-it
  (let [root (temp-dir)
        arm "src/app/folds.clj"
        loopa "src/app/loopa.clj"
        loopb "src/app/loopb.clj"
        ;; A single path component far over any filesystem's name limit. The
        ;; kernel answers ENAMETOOLONG, which the JDK raises as a
        ;; `FileSystemException` that is not an `AccessDeniedException` — the
        ;; second member of the class the round-sixteen predicate missed.
        too-long (str "src/app/" (apply str (repeat 10001 \a)) ".clj")]
    (try
      (spit-file! (io/file root arm) arm-source)
      (symlink! (io/file root loopa) (io/file root loopb))
      (symlink! (io/file root loopb) (io/file root loopa))
      (let [named (.getCanonicalPath root)
            real-root (mcp-paths/real-root named)]

        (testing "the resolver publishes no absolute path for any hostile shape"
          (doseq [[label relative] [[:a-symlink-loop loopa]
                                    [:a-name-too-long too-long]]]
            (let [refusal (mcp-paths/resolve-source-path real-root relative)]
              (is (false? (:ok refusal))
                  (str label " resolved: " (pr-str refusal)))
              (is (not (str/includes? (str (:error refusal)) named))
                  (str label " published the server's absolute root: "
                       (pr-str (subs (str (:error refusal)) 0
                                     (min 200 (count (str (:error refusal))))))))
              (is (not (re-find #"(?:^|[\s(])/\S" (str (:error refusal))))
                  (str label " published an absolute path: "
                       (pr-str (subs (str (:error refusal)) 0
                                     (min 200 (count (str (:error refusal))))))))
              (is (some? (:cause refusal))
                  (str label " published no typed cause: " (pr-str refusal))))))

        (testing "a symlink loop through the tool names the path relative"
          (let [result (census-tool/execute-request!
                         {:project-root named} {:files [loopa]})]
            (is (= "unreadable-source-path" (:error_type result))
                (str "the tool answered " (pr-str (:error_type result))))
            (is (not (str/includes? (str (:error result)) named))
                (str "the tool published the server's absolute root: "
                     (pr-str (:error result))))
            (is (str/includes? (str (:error result)) loopa)
                (str "the tool does not name the path it refused: "
                     (pr-str (:error result))))
            (is (= "not-found" (:cause result))
                (str "the tool published no shared cause for a path that does "
                     "not resolve: " (pr-str (:cause result))))))

        (testing "the CLI answers the same FACT about the same loop"
          (let [result (refusal-or-throw
                         #(core/run-relation-census
                            {:file (str named "/" loopa)}))
                tool (census-tool/execute-request!
                       {:project-root named} {:files [loopa]})]
            (is (nil? (:threw result))
                (str "the CLI threw instead of refusing: " (pr-str result)))
            (is (= :file-not-found (:error-type result))
                (str "the CLI answered " (pr-str (:error-type result))))
            (is (= :not-found (:cause result))
                (str "the CLI published no typed cause: " (pr-str result)))
            (is (= (some-> (:cause result) name) (:cause tool))
                (str "the two entrances disagree about what they observed: "
                     "CLI " (pr-str (:cause result)) " vs tool "
                     (pr-str (:cause tool)))))))
      (finally
        (delete-tree! root)))))

;; ---------------------------------------------------------------------------
;; Opus's round-sixteen NO-GO item 4: the contract disagrees with itself one
;; level up. One unreadable FILE refuses the entire census; one unreadable
;; DIRECTORY, which can hide a thousand files, was swallowed with `:continue`
;; and no counter, and the receipt still said `read_complete true`:
;;
;;   tree: src/app/ok.clj (an arm) + src/app/hidden/ (chmod 000, holding an arm)
;;   CLI  :dir  -> ok, sites 1, "review the raw sites…"
;;   MCP  walk  -> ok = true, files_scanned = 1, files = 1, arms = 1
;;
;; A census is a COMPLETENESS claim, which is the whole reason a single
;; unreadable member refuses it; a subtree the walk could not enter makes that
;; claim false in exactly the same way and by a larger amount. So it is decided
;; the way the file rule is decided — refuse, typed, counted, naming the
;; directory project-relative — and both entrances decide it alike.
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-CENSUS-014
;; @spec MCP-OP-CENSUS-018
;; @spec MCP-OP-CENSUS-019
(deftest an-unreadable-directory-refuses-the-census-on-both-entrances
  (let [root (temp-dir)
        arm "src/app/ok.clj"
        hidden-dir (io/file root "src/app/hidden")
        hidden "src/app/hidden"]
    (try
      (spit-file! (io/file root arm) arm-source)
      (spit-file! (io/file root "src/app/hidden/inner.clj") arm-source)
      (deny-traversal! hidden-dir)
      (let [named (.getCanonicalPath root)
            declared (into census/cli-refusal-types census/mcp-refusal-types)]

        (testing "the fixture is genuinely unlistable to this process"
          (is (false? (.canRead hidden-dir))
              "this process can still list the chmod-000 directory"))

        (testing "the walk itself reports the subtree it could not enter"
          (let [discovered (census-discovery/discover named)]
            (is (= [hidden] (:unreadable-directories discovered))
                (str "the walk swallowed a directory it could not enter: "
                     (pr-str (select-keys discovered
                                          [:files :unreadable-directories]))))))

        (testing "the CLI :dir walk refuses instead of claiming completeness"
          (let [result (refusal-or-throw
                         #(core/run-relation-census {:dir named}))]
            (is (nil? (:threw result))
                (str "the walk threw instead of refusing: " (pr-str result)))
            (is (false? (:ok result))
                (str "an unreadable subtree was censused as complete: "
                     (pr-str (select-keys result [:ok :sites :files-scanned
                                                  :read-complete]))))
            (is (= :file-not-readable (:error-type result))
                (str "the walk refused " (pr-str (:error-type result))))
            (is (contains? declared (:error-type result))
                (str "the walk refused " (pr-str (:error-type result))
                     ", which no declared refusal set contains"))
            (is (= :directory-denied (:cause result))
                (str "the refusal does not say WHY: " (pr-str result)))
            (is (= hidden (:directory result))
                (str "the refusal does not name the directory it could not "
                     "enter: " (pr-str (:directory result))))
            (is (str/includes? (str (:remedy result)) hidden)
                (str "the remedy does not name the directory: "
                     (pr-str (:remedy result))))
            (is (not (contains? result :next-command))
                (str "a path from the walk is not a request to narrow: "
                     (pr-str (:next-command result))))))

        (testing "the tool's walk refuses the same tree the same way"
          (let [result (census-tool/execute-request! {:project-root named} {})]
            (is (false? (:ok result))
                (str "an unreadable subtree was censused as complete: "
                     (pr-str (select-keys result [:ok :files :arms
                                                  :read_complete]))))
            (is (= "unreadable-source-path" (:error_type result))
                (str "the tool answered " (pr-str (:error_type result))))
            (is (= "directory-denied" (:cause result))
                (str "the tool published no shared cause: "
                     (pr-str (:cause result))))
            (is (= hidden (:directory result))
                (str "the tool does not name the directory: "
                     (pr-str (:directory result))))
            (is (str/includes? (str (:remedy result)) hidden)
                (str "the remedy does not name the directory: "
                     (pr-str (:remedy result))))
            (is (not (contains? result :next_call))
                (str "a path from the walk is not a request to narrow: "
                     (pr-str (:next_call result))))))

        (testing "the two entrances agree about what they observed"
          (let [cli (refusal-or-throw
                      #(core/run-relation-census {:dir named}))
                tool (census-tool/execute-request! {:project-root named} {})]
            (is (= (some-> (:cause cli) name) (:cause tool))
                (str "CLI " (pr-str (:cause cli)) " vs tool "
                     (pr-str (:cause tool))))
            (is (= (:directory cli) (:directory tool))
                (str "CLI " (pr-str (:directory cli)) " vs tool "
                     (pr-str (:directory tool)))))))
      (finally
        (allow-traversal! hidden-dir)
        (delete-tree! root)))))

;; ---------------------------------------------------------------------------
;; Sol's round-fifteen item 10 and NO-GO item 6: the ONE constructor refuses an
;; EMPTY `files` because "the published schema declares minItems 1" — and the
;; same schema declares the items are STRINGS, which it never asked.
;;
;;   ctor empty files     -> {:candidate nil, :bytes nil, :next-call nil}
;;   ctor nil-entry files -> {:next-call {… :files [nil] …}, :bytes 91}
;;   ctor non-string      -> {:next-call {… :files [42]  …}, :bytes 89}
;;   ctor files "x"       -> {:next-call {… :files "x"   …}, :bytes 88}
;;
;; The `:census-failed` fallback hands it `{:files [(:file planned)]}`, and a
;; plan failure that names no `:file` — the documented reason that fallback
;; exists — makes that `[nil]`. Driven from a short root, where the byte
;; ceiling cannot mask it:
;;
;;   census-failed next_call: {"workspace_root":"…","files":[null],"tool":"relation_census"}
;;   has remedy? false
;;
;; Replaying that continuation refuses `invalid-mcp-request / file-not-a-string`
;; — a call the tool's own schema rejects, which is the same unexecutable
;; promise a caption in an argument position is. And the 600-character ratchet
;; is structurally BLIND to it: at 600 characters the ceiling drops the
;; continuation before any schema question is asked, so the root length that
;; makes that witness work is what hides this one.
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-CENSUS-014
(deftest the-constructor-refuses-a-files-list-the-published-schema-rejects
  (testing "the constructor's own contract, direct"
    (doseq [[label candidate]
            [[:empty-list {:workspace_root "/x" :files []}]
             [:nil-entry {:workspace_root "/x" :files [nil]}]
             [:non-string-entry {:workspace_root "/x" :files [42]}]
             [:blank-entry {:workspace_root "/x" :files [""]}]
             [:not-a-list-at-all {:workspace_root "/x" :files "x"}]]]
      (let [{:keys [next-call bytes candidate]}
            (census-tool/continuation candidate)]
        (is (nil? next-call)
            (str label " was admitted as a continuation: " (pr-str next-call)))
        (is (nil? candidate)
            (str label " was admitted as a candidate: " (pr-str candidate)))
        (is (nil? bytes)
            (str label " reports a measured byte length, which says it was "
                 "refused for LENGTH when it was refused for shape: "
                 (pr-str bytes))))))

  ;; -------------------------------------------------------------------------
  ;; Opus's round-sixteen NO-GO item 6. The constructor asks the schema's
  ;; `items` rule and not its `maxItems 512`, and the item rule it asks is
  ;; "a non-blank string" rather than the rule this tool's own ENTRANCE
  ;; applies. Measured at 42df064:
  ;;
  ;;   513-entries  next-call? false bytes=5591  (dropped for LENGTH)
  ;;   nul-byte     next-call? TRUE  bytes=76
  ;;
  ;; 513 entries is refused only because 5,591 bytes exceeds the 512-byte
  ;; ceiling — the SAME masking by the ceiling that hid the `items` half until
  ;; round sixteen — and a NUL-byte entry is published into a continuation that
  ;; replaying returns "Expected a project-relative .clj, .cljs, .cljc, or .edn
  ;; path without parent traversal". A continuation the tool's own entrance
  ;; refuses is the unexecutable promise MCP-OP-CENSUS-014 forbids, whether the
  ;; caption is a word or a control character.
  ;; -------------------------------------------------------------------------
  (testing "the constructor applies the WHOLE published files rule"
    (doseq [[label entries]
            [[:a-nul-byte-entry [(str "src/a" (char 0) ".clj")]]
             [:an-absolute-entry ["/src/a.clj"]]
             [:a-parent-traversal-entry ["../src/a.clj"]]
             [:an-unsupported-extension ["src/a.txt"]]
             [:an-empty-segment ["src//a.clj"]]
             [:five-hundred-and-thirteen
              (mapv #(str "s" % ".clj") (range 513))]]]
      (let [{:keys [next-call bytes candidate]}
            (census-tool/continuation {:workspace_root "/x" :files entries})]
        (is (nil? next-call)
            (str label " was admitted as a continuation: " (pr-str next-call)))
        (is (nil? candidate)
            (str label " was admitted as a candidate: "
                 (pr-str (take 3 (:files candidate)))))
        (is (nil? bytes)
            (str label " reports a measured byte length, which says it was "
                 "refused for LENGTH when the schema refuses it for SHAPE: "
                 (pr-str bytes))))))

  (testing "the count rule is enforced AT the ceiling, not near it"
    ;; 512 is what the schema declares, so 512 passes the SHAPE question and
    ;; whatever happens next is the byte ceiling's business. 513 does not, and
    ;; must not be masked by the byte ceiling the way it was for sixteen
    ;; rounds.
    (let [at (census-tool/continuation
               {:workspace_root "/x"
                :files (mapv #(str "s" % ".clj") (range 512))})
          over (census-tool/continuation
                 {:workspace_root "/x"
                  :files (mapv #(str "s" % ".clj") (range 513))})]
      (is (some? (:candidate at))
          "512 entries — exactly what the schema declares — was refused for shape")
      (is (some? (:bytes at))
          "512 entries carries no measured byte length, so it was refused for shape")
      (is (nil? (:candidate over))
          "513 entries was admitted for shape and left to the byte ceiling")))

  (testing "nothing the constructor admits is refused by this tool's entrance"
    ;; The class, stated as the property rather than as a list of shapes: a
    ;; continuation is a call the caller replays into THIS tool, so every entry
    ;; it carries must pass the entrance's own path rule.
    (doseq [[label entries]
            [[:ordinary ["src/app/folds.clj"]]
             [:mixed ["src/app/a.cljc" "src/app/b.edn"]]
             [:nul [(str "src/a" (char 0) ".clj")]]
             [:absolute ["/src/a.clj"]]]]
      (when-let [candidate (:candidate (census-tool/continuation
                                         {:workspace_root "/x"
                                          :files entries}))]
        (doseq [entry (:files candidate)]
          (is (mcp-paths/relative-source-path? entry)
              (str label " published an entry this tool's entrance refuses: "
                   (pr-str entry)))))))

  (testing "a well-formed files list still travels"
    (let [{:keys [next-call bytes]}
          (census-tool/continuation {:workspace_root "/x"
                                     :files ["src/a.clj"]})]
      (is (= {:tool "relation_census" :workspace_root "/x"
              :files ["src/a.clj"]}
             next-call)
          (str "a valid narrowing was refused: " (pr-str next-call)))
      (is (pos? bytes))))

  (testing "the :census-failed fallback publishes no continuation at all"
    (let [root (temp-dir)
          arm "src/app/folds.clj"]
      (try
        (spit-file! (io/file root arm) arm-source)
        (let [named (.getCanonicalPath root)
              result (with-redefs [census/plan
                                   (fn [& _]
                                     {:ok false :error "injected plan failure"})]
                       (census-tool/execute-request!
                         {:project-root named}
                         {:files [arm] :doors ["upsert-by"] :pool_size 1}))]
          (is (= "census-failed" (:error_type result))
              (str "the probe did not reach the fallback: " (pr-str result)))
          (is (not (contains? result :next_call))
              (str "the fallback published a continuation the tool's own "
                   "schema rejects: " (json/generate-string (:next_call result))))
          (is (string? (:remedy result))
              (str "the fallback offers neither a continuation nor a remedy: "
                   (pr-str result)))
          (is (not (str/includes? (str (:remedy result)) "null"))
              (str "the remedy is built out of the measurement of a "
                   "continuation that was never measurable: "
                   (pr-str (:remedy result))))
          (is (not (and (contains? result :file) (nil? (:file result))))
              (str "the refusal publishes a null :file rather than omitting "
                   "the key: " (pr-str (select-keys result [:file])))))
        (finally
          (delete-tree! root))))))

;; ---------------------------------------------------------------------------
;; Opus's round-sixteen NO-GO item 5: `continuation-overflow-remedy` asserts a
;; cause it never measured. Five hundred named files plus one denied, on a
;; TWENTY-FOUR character root, returned
;;
;;   "The narrowest continuation this refusal can compute renders as 9592 UTF-8
;;    bytes, over the 512-byte ceiling … The REQUEST is not the problem, the
;;    length of the workspace path in it is: retry with workspace_root reaching
;;    the same tree by a shorter path"
;;
;; The workspace path was 24 of those 9,592 bytes. A caller who follows that
;; remedy shortens the one thing that is not the problem and gets the identical
;; refusal — which is the loop-with-a-receipt this requirement forbids a
;; continuation, arriving in a remedy instead. A refusal that names a bound must
;; name the value it compared against; a refusal that names a CAUSE must have
;; measured it.
;; ---------------------------------------------------------------------------

(defn- long-relative-path
  "A project-relative source path of roughly `chars` characters.

   Built out of nested directory segments because a single component over 255
   bytes is ENAMETOOLONG on every filesystem this runs on — the entry cannot
   exist, and a continuation is only over the ceiling because of an entry that
   CAN."
  [chars]
  (let [segment (apply str (repeat 24 \d))
        depth (max 1 (quot chars 25))]
    (str (str/join "/" (repeat depth segment)) "/folds.clj")))

;; @spec MCP-OP-CENSUS-014
(deftest a-continuation-over-the-ceiling-names-the-cause-it-measured
  (let [root (temp-dir)
        arm "src/app/folds.clj"
        denied "src/app/denied.clj"
        denied-file (io/file root denied)
        many (mapv #(format "src/app/f%03d.clj" %) (range 500))
        long-path (long-relative-path 600)]
    (try
      (spit-file! (io/file root arm) arm-source)
      (spit-file! denied-file arm-source)
      (spit-file! (io/file root long-path) arm-source)
      (doseq [name many] (spit-file! (io/file root name) arm-source))
      (deny-reads! denied-file)
      (let [named (.getCanonicalPath root)
            here (fn [params]
                   (census-tool/execute-request! {:project-root named} params))]

        (testing "the fixtures are the ones that make each cause dominant"
          (is (false? (.canRead denied-file))
              "this process can still read the chmod-000 fixture")
          (is (< (census/utf8-byte-count named) 200)
              (str "the workspace root is not short, so the entry-count drive "
                   "cannot distinguish the causes: " (count named))))

        (testing "many short entries name the ENTRY COUNT, not the root"
          (let [result (here {:files (conj many denied)})
                remedy (str (:remedy result))]
            (is (not (contains? result :next_call))
                (str "the candidate fitted, so this drive proves nothing: "
                     (pr-str (:next_call result))))
            (is (string? (:remedy result))
                (str "the refusal offers neither a continuation nor a "
                     "remedy: " (pr-str result)))
            (is (not (str/includes? remedy
                                    "reaching the same tree by a shorter path"))
                (str "the remedy blames a workspace path it never measured: "
                     (pr-str remedy)))
            (is (str/includes? remedy "500")
                (str "the remedy does not name the number of sources it "
                     "measured: " (pr-str remedy)))
            (is (re-find #"\b\d{4,}\b" remedy)
                (str "the remedy does not name the byte length it measured: "
                     (pr-str remedy)))))

        (testing "one very long entry names the ENTRY LENGTH, not the count"
          (let [result (here {:files [long-path denied]})
                remedy (str (:remedy result))]
            (is (not (contains? result :next_call))
                (str "the candidate fitted, so this drive proves nothing: "
                     (pr-str (:next_call result))))
            (is (not (str/includes? remedy
                                    "reaching the same tree by a shorter path"))
                (str "the remedy blames a workspace path it never measured: "
                     (pr-str remedy)))
            (is (str/includes? remedy "longest")
                (str "the remedy does not name the entry whose length it "
                     "measured: " (pr-str remedy)))
            (is (str/includes? remedy
                               (str (census/utf8-byte-count long-path)))
                (str "the remedy does not name the byte length of that entry: "
                     (pr-str remedy))))))
      (finally
        (allow-reads! denied-file)
        (delete-tree! root)))))

;; @spec MCP-OP-CENSUS-014
(deftest a-continuation-over-the-ceiling-on-a-long-root-names-the-root
  (let [parent (temp-dir)
        ;; A root long enough that IT is what does not fit, which is the case
        ;; the round-fifteen wording was written for and the only case in which
        ;; it was true.
        long-root (io/file parent (str/join "/" (repeat 24 (apply str (repeat 24 \r)))))
        arm "src/app/folds.clj"
        denied "src/app/denied.clj"
        denied-file (io/file long-root denied)]
    (try
      (spit-file! (io/file long-root arm) arm-source)
      (spit-file! denied-file arm-source)
      (deny-reads! denied-file)
      (let [named (.getCanonicalPath long-root)
            result (census-tool/execute-request!
                     {:project-root named} {:files [arm denied]})
            remedy (str (:remedy result))]
        (is (> (census/utf8-byte-count named) census/max-next-call-bytes)
            (str "the root is not long enough to dominate: " (count named)))
        (is (not (contains? result :next_call))
            (str "the candidate fitted, so this drive proves nothing: "
                 (pr-str (:next_call result))))
        (is (str/includes? remedy "reaching the same tree by a shorter path")
            (str "the remedy does not name the workspace path, which IS what "
                 "it measured here: " (pr-str remedy)))
        (is (str/includes? remedy (str (census/utf8-byte-count named)))
            (str "the remedy does not name the byte length of the root it "
                 "measured: " (pr-str remedy))))
      (finally
        (allow-reads! denied-file)
        (delete-tree! parent)))))

;; ---------------------------------------------------------------------------
;; Opus's round-sixteen NO-GO item 7: receipts are capped at 4,096 bytes,
;; continuations at 512, and refusals at nothing. A 10,001-character `files`
;; entry yielded a ~30 KB tool refusal — `error`, `file` and `files_removed`
;; each echoing the whole name, and `error` again the raw exception text — and
;; a 50,612-byte CLI refusal. The same drive is the second place the two
;; entrances were caught calling one input two different things.
;;
;; A refusal is the answer a caller reads when something has already gone
;; wrong; it is the last place that should be able to hand back thirty
;; kilobytes of the caller's own bad input. Truncation is only honest when it
;; SAYS it truncated and says how much there was, so the caller is never left
;; comparing a silently shortened path against the one they sent.
;; ---------------------------------------------------------------------------

(defn- all-strings
  "Every string anywhere in a refusal, keys and values alike."
  [x]
  (cond
    (string? x) [x]
    (map? x) (mapcat all-strings (concat (keys x) (vals x)))
    (coll? x) (mapcat all-strings x)
    :else []))

;; @spec MCP-OP-CENSUS-014
(deftest every-refusal-field-is-length-bounded-at-both-entrances
  (let [root (temp-dir)
        arm "src/app/folds.clj"
        long-name (str "src/app/" (apply str (repeat 10001 \a)) ".clj")
        ;; The policy this round establishes, owned by the witness rather than
        ;; read back from the code it checks: no single refusal field renders
        ;; more than 1,024 characters, plus the marker that says so.
        max-field 1024
        ceiling (+ max-field 64)
        ;; A path of an EXACT length, built out of 24-character segments
        ;; because one component over 255 bytes is ENAMETOOLONG and would be
        ;; refused for a different reason than the one under test.
        exact-path (fn [n]
                     (let [segment (apply str (repeat 24 \d))
                           body (subs (apply str (repeat (inc (quot n 25))
                                                         (str segment "/")))
                                      0 (- n 4))]
                       (str body ".clj")))]
    (try
      (spit-file! (io/file root arm) arm-source)
      (let [named (.getCanonicalPath root)
            tool (census-tool/execute-request!
                   {:project-root named} {:files [long-name]})
            cli (refusal-or-throw
                  #(core/run-relation-census
                     {:file (str named "/" long-name)}))
            file-field (fn [path]
                         (:file (census-tool/execute-request!
                                  {:project-root named} {:files [path]})))]

        (testing "the bound is enforced AT the ceiling, not near it"
          (let [at (exact-path max-field)
                over (exact-path (inc max-field))]
            (is (= max-field (count at)))
            (is (= (inc max-field) (count over)))
            (is (= at (file-field at))
                (str "a field exactly at the ceiling was truncated: "
                     (pr-str (subs (str (file-field at)) 0 40))))
            (is (not= over (file-field over))
                "a field one character over the ceiling was published whole")
            (is (str/includes? (str (file-field over)) (str (inc max-field)))
                (str "the truncation does not say how long the original was: "
                     (pr-str (str/join (take-last 60 (str (file-field over))))))))) 

        (testing "no field of the tool's refusal is unbounded"
          (is (false? (:ok tool))
              (str "the drive did not refuse: " (pr-str (:ok tool))))
          (doseq [text (all-strings tool)]
            (is (<= (count text) ceiling)
                (str "a refusal field renders " (count text)
                     " characters: " (pr-str (str/join (take 80 text))))))
          (is (< (count (json/generate-string tool)) 8192)
              (str "the whole refusal renders "
                   (count (json/generate-string tool))
                   " bytes, against a 4096-byte receipt cap"))
          (is (some #(str/includes? % "truncated") (all-strings tool))
              "the tool shortened a field without saying so")
          (is (some #(str/includes? % (str (count long-name)))
                    (all-strings tool))
              (str "the tool does not say how long the original " (count long-name)
                   "-character entry was: "
                   (pr-str (map #(str/join (take-last 40 %))
                                (all-strings tool))))))

        (testing "no field of the CLI's refusal is unbounded"
          (is (false? (:ok cli))
              (str "the drive did not refuse: " (pr-str cli)))
          (doseq [text (all-strings cli)]
            (is (<= (count text) ceiling)
                (str "a refusal field renders " (count text)
                     " characters: " (pr-str (str/join (take 80 text))))))
          (is (< (count (pr-str cli)) 8192)
              (str "the whole refusal renders " (count (pr-str cli))
                   " bytes, against a 4096-byte receipt cap"))
          (is (some #(str/includes? % "truncated") (all-strings cli))
              "the CLI shortened a field without saying so"))

        (testing "the two entrances call this one input the same thing"
          (is (= :not-found (:cause cli))
              (str "the CLI answered " (pr-str (:cause cli))))
          (is (= "not-found" (:cause tool))
              (str "the tool answered " (pr-str (:cause tool))))
          (is (= (some-> (:cause cli) name) (:cause tool))
              (str "CLI " (pr-str (:cause cli)) " vs tool "
                   (pr-str (:cause tool)))))

        (testing "a continuation is never truncated"
          ;; The one EXEMPTION from the bound, proved rather than asserted. A
          ;; continuation is an executable promise: a truncated path in an
          ;; argument position does not fail, it names a DIFFERENT file, which
          ;; MCP-OP-CENSUS-014 already forbids in the sentence about captions.
          ;; It is safe to exempt precisely because it carries its OWN ceiling
          ;; — `max-next-call-bytes`, a quarter of the field bound — so nothing
          ;; a continuation holds can reach the length this bound acts on.
          (doseq [[label params] [[:one-entry-missing
                                   {:files [arm "src/app/missing.clj"]}]
                                  [:one-entry-far-too-long
                                   {:files [arm long-name]}]
                                  [:an-unknown-door
                                   {:files [arm] :doors ["nope"]}]]]
            (let [result (census-tool/execute-request!
                           {:project-root named} params)]
              (doseq [text (all-strings (select-keys result [:next_call]))]
                (is (not (str/includes? text "truncated"))
                    (str label " published a TRUNCATED continuation, which "
                         "names a different file rather than failing: "
                         (pr-str (:next_call result))))
                (is (<= (count text) max-field)
                    (str label " carried a continuation field longer than the "
                         "refusal bound, so the exemption is not safe: "
                         (count text))))
              (when-let [call (:next_call result)]
                (is (<= (census/utf8-byte-count (json/generate-string call))
                        census/max-next-call-bytes)
                    (str label " published a continuation over its OWN "
                         "ceiling: "
                         (census/utf8-byte-count
                           (json/generate-string call))))))))

        (testing "the exemption itself, pinned where it can be falsified"
          ;; The drives above prove the exemption is HARMLESS — no continuation
          ;; production can build reaches the field bound, because its own
          ;; ceiling is a quarter of it. They cannot prove the exemption is
          ;; still THERE: deleting it would leave every one of them green.
          ;; This asks `bound-refusal` its contract directly, with a
          ;; continuation deliberately over the field bound, so that removing
          ;; the exemption fails here instead of failing in the field, on the
          ;; day something starts building longer calls.
          (let [call {:tool "relation_census"
                      :workspace_root (apply str (repeat 2000 \r))
                      :files ["src/a.clj"]}
                argv ["clj-surgeon" ":op" ":relation-census" ":dir"
                      (apply str (repeat 2000 \r))]
                bounded (census/bound-refusal
                          {:ok false
                           :error (apply str (repeat 2000 \e))
                           :next_call call
                           :next-command-argv argv})]
            (is (= call (:next_call bounded))
                "the bound truncated a continuation, which names a different file")
            (is (= argv (:next-command-argv bounded))
                "the bound truncated a CLI continuation's argv tokens")
            (is (str/includes? (str (:error bounded)) "truncated")
                (str "an ordinary field was NOT bounded, so this drive proves "
                     "nothing about the exemption: "
                     (count (str (:error bounded)))))))

        (testing "the CLI's continuation is never truncated either"
          (let [result (refusal-or-throw
                         #(core/run-relation-census {:dir named :doors "nope"}))]
            (doseq [text (all-strings (select-keys result [:next-command
                                                           :next-command-argv]))]
              (is (not (str/includes? text "truncated"))
                  (str "the CLI published a TRUNCATED continuation: "
                       (pr-str (select-keys result [:next-command
                                                    :next-command-argv])))))))

        (testing "an ordinary refusal is not truncated at all"
          ;; The near-miss: a bound that fires on everything is not a bound,
          ;; it is a censor. Nothing about a normal-length refusal changes.
          (let [ordinary (census-tool/execute-request!
                           {:project-root named}
                           {:files ["src/app/missing.clj"]})]
            (is (false? (:ok ordinary)))
            (is (not-any? #(str/includes? % "truncated")
                          (all-strings ordinary))
                (str "an ordinary refusal was truncated: "
                     (pr-str ordinary))))))
      (finally
        (delete-tree! root)))))

;; ---------------------------------------------------------------------------
;; Sol's round-twelve review, item 3: the byte ceiling counted Java characters.
;;
;; `max-next-call-bytes` is named in bytes, documented in bytes, and reported
;; in bytes by every remedy that mentions it — and it was enforced with
;; `count`, which counts UTF-16 code units. On an ASCII path the two agree,
;; which is why the bound looked correct for eleven rounds. On a path with
;; accented characters they do not: Sol emitted a continuation of 490
;; characters and 890 UTF-8 bytes under a 512-BYTE limit, and the same
;; arithmetic guards the tool's JSON continuations.
;;
;; The bound exists because a continuation is a thing a caller reads, pastes
;; and execs, and every one of those consumers measures bytes: argv is bytes,
;; a JSON body is bytes, a terminal line is bytes. So the measurement is
;; bytes, at one shared predicate both entrances call, and the remedy that
;; replaces an over-long continuation SAYS what it measured — a refusal that
;; names a bound without naming the value it compared against leaves the
;; caller to guess how much shorter is short enough.
;; ---------------------------------------------------------------------------

(def ^:private wide-continuation-path
  "A path whose CLI continuation is 490 characters and 890 UTF-8 bytes.

   `é` is one Java character and two UTF-8 bytes, so this is Sol's exact
   counterexample: comfortably inside a 512-character bound and far outside a
   512-byte one."
  (let [segment (apply str (repeat 100 "é"))]
    (str "/" segment "/" segment "/" segment "/" segment "/"
         (apply str (repeat 34 "a")))))

;; @spec MCP-OP-CENSUS-014
(deftest the-continuation-ceiling-is-measured-in-bytes
  (testing "the fixture is Sol's: 490 characters, 890 bytes"
    (let [command (census/render-command
                    ["clj-surgeon" ":op" ":relation-census"
                     ":dir" wide-continuation-path ":threads" "8"])]
      (is (= 490 (count command))
          (str "the fixture drifted: " (count command) " characters"))
      (is (= 890 (alength (.getBytes ^String command "UTF-8")))
          (str "the fixture drifted: "
               (alength (.getBytes ^String command "UTF-8")) " bytes"))
      (is (<= (count command) census/max-next-call-bytes)
          "the fixture no longer fits the CHARACTER bound it must fit")
      (is (> (alength (.getBytes ^String command "UTF-8"))
             census/max-next-call-bytes)
          "the fixture no longer breaks the BYTE bound it must break")))

  (testing "one shared predicate answers the bound, in bytes"
    ;; Resolved rather than referred, so this namespace still LOADS while the
    ;; predicate does not exist and the rest of the witness can report on the
    ;; behaviour instead of dying at compile time.
    (let [within? (some-> (resolve 'clj-surgeon.relation-census/within-next-call-bytes?)
                          var-get)]
      (is (some? within?)
          (str "there is no ONE shared predicate for the continuation bound, "
               "so each call site measures it in whatever units it happens "
               "to reach for"))
      (when within?
        (is (true? (within? (apply str (repeat census/max-next-call-bytes "a"))))
            "a continuation exactly at the bound was refused")
        (is (false? (within? (apply str (repeat (inc census/max-next-call-bytes)
                                                "a"))))
            "a continuation one byte over the bound was allowed")
        (is (false? (within? (apply str (repeat (quot census/max-next-call-bytes 2)
                                                "éé"))))
            (str "a continuation of " census/max-next-call-bytes
                 " characters and " (* 2 census/max-next-call-bytes)
                 " bytes was measured as fitting a "
                 census/max-next-call-bytes "-byte bound")))))

  (testing "a continuation over the BYTE bound is refused, not emitted"
    (let [refusal (core/run-relation-census
                    {:dir wide-continuation-path :threads "not-a-number"})]
      (is (= :invalid-pool-size (:error-type refusal)))
      (is (= wide-continuation-path (:absolute (:anchor refusal)))
          "the refusal stopped naming the workspace it was given")
      (is (not (contains? refusal :next-command))
          (str "a continuation of 890 UTF-8 bytes was handed back under a "
               census/max-next-call-bytes "-byte bound: "
               (pr-str (:next-command refusal))))
      (is (not (contains? refusal :next-command-argv)))
      (is (string? (:remedy refusal))
          "the refusal offers neither a continuation nor a remedy")
      (is (str/includes? (str (:remedy refusal)) "890")
          (str "the remedy does not state the byte length it measured: "
               (pr-str (:remedy refusal))))
      (is (str/includes? (str (:remedy refusal))
                         (str census/max-next-call-bytes))
          (str "the remedy does not state the bound: "
               (pr-str (:remedy refusal))))))

  (testing "the post-scan refusals measure the same way"
    ;; `:doors` reaches its refusal after the scan and builds its
    ;; continuation at a different site; a bound enforced in one branch is a
    ;; bound the other branches break.
    (let [refusal (core/run-relation-census
                    {:dir wide-continuation-path :doors "conj"})]
      (is (= :unknown-door-symbol (:error-type refusal)))
      (is (not (contains? refusal :next-command))
          (str "the door refusal emitted an over-long continuation: "
               (pr-str (:next-command refusal))))
      (is (str/includes? (str (:remedy refusal)) "bytes")
          (str "the door remedy does not talk about bytes: "
               (pr-str (:remedy refusal))))))

  (testing "an ASCII continuation of the same length still fits"
    (let [ascii (str "/" (apply str (repeat 430 "a")))
          refusal (core/run-relation-census
                    {:dir ascii :threads "not-a-number"})]
      (is (= :invalid-pool-size (:error-type refusal)))
      (is (contains? refusal :next-command)
          (str "a continuation well inside the byte bound was refused: "
               (pr-str refusal))))))

;; ---------------------------------------------------------------------------
;; Sol's round-twelve review, item 10: the entrances still divide the door
;; vocabulary between two phases.
;;
;; The shared table gave the `[:doors :vocabulary]` row an `:mcp-phase` of
;; `:post-discovery`, so the tool's shape walk SKIPPED it and the CLI's did
;; not. Sol's `doors=conj, file="", threads=bad` therefore refused
;; `unknown-door-symbol` at the CLI and `empty-file-list` at the tool: one
;; request, two first refusals, from a table whose entire purpose is that the
;; two entrances cannot disagree about which shape is refused first.
;;
;; The divergence was defended on the grounds that the tool's refusal could
;; then carry the discovery facts. But this row does not need a scan. It asks
;; the SYNTACTIC half of the door question — is this name a symbol, and does
;; it shadow a collection write head — and both halves are decided against
;; `'#{conj cons into concat}` and `default-doors`, two compile-time sets. A
;; pure predicate answered after a walk is not a better answer; it is the
;; same answer, later, on an entrance the other one no longer matches.
;;
;; The half that genuinely needs the scan — is this door DEFINED in any file
;; the census read — is not this row and does not move. It stays after
;; discovery, on both entrances, and keeps its facts.
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-CENSUS-016
;; @spec MCP-OP-CENSUS-029
(deftest the-door-vocabulary-is-decided-in-one-phase-at-both-entrances
  (testing "the vocabulary this row checks is compile-time, not discovered"
    (is (set? census/default-doors)
        "the door vocabulary is not a static set")
    (is (map? (census/parse-doors ["conj"] nil))
        (str "the syntactic door check cannot answer without a scan after "
             "all, and the phase split was load-bearing"))
    (is (map? (census/parse-doors ["not a symbol"] nil))
        "the not-a-symbol half needs a scan"))

  (testing "the row runs in the shape pass on BOTH entrances"
    (is (nil? (:mcp-phase (census/shape-rule :doors :vocabulary)))
        (str "the door-vocabulary row still declares a phase of its own on "
             "the tool, so the tool's shape walk skips a row the CLI's "
             "applies"))
    (let [split (mapv (juxt :field :violation)
                      (filter :mcp-phase census/request-shape-rules))]
      (is (empty? split)
          (str "these rows still split the two entrances across phases: "
               (pr-str split)))))

  (testing "one malformed request, one first refusal"
    (let [cli (binding [*out* (java.io.StringWriter.)]
                (core/run {:op :relation-census :dir repo-root
                           :doors "conj" :file "" :threads "bad"}))
          blank-file (run {:doors ["conj"] :files [""] :pool_size "bad"})
          empty-list (run {:doors ["conj"] :files [] :pool_size "bad"})]
      (is (= :unknown-door-symbol (:error-type cli))
          (str "the CLI moved: " (pr-str (:error-type cli))))
      (is (= "unknown-door-symbol" (published-mcp-name blank-file))
          (str "the tool refused files before doors: "
               (pr-str (published-mcp-name blank-file))))
      (is (= "unknown-door-symbol" (published-mcp-name empty-list))
          (str "the tool refused files before doors: "
               (pr-str (published-mcp-name empty-list))))))

  (testing "the tool's shape refusal still says what it found"
    (let [result (run {:files [fixture] :doors ["conj"]})]
      (is (false? (:ok result)))
      (is (str/includes? (:error result) "shadows a collection write head")
          (str "the refusal lost its reason: " (pr-str (:error result))))
      (is (= "conj" (:door result))
          (str "the refusal does not name the door: " (pr-str result)))
      (is (contains? (set (:known_doors result)) "upsert-by")
          (str "the refusal does not name the known doors: " (pr-str result)))
      (is (not (contains? result :files_scanned))
          (str "a refusal computed before any walk published a scan count: "
               (pr-str result)))))

  (testing "definedness still needs the scan, and still gets its facts"
    (let [result (run {:files [fixture] :doors ["made-up-door"]})]
      (is (= "unknown-door-symbol" (:error_type result))
          (str "the post-scan door refusal moved too: " (pr-str result)))
      (is (str/includes? (:error result) "not defined in any scanned file"))
      (is (= 1 (:files_scanned result))
          (str "the post-scan refusal lost its discovery facts: "
               (pr-str result))))))

;; ---------------------------------------------------------------------------
;; Sol's round-fourteen review, item 9, blocking: the 512-byte continuation
;; ceiling was enforced at ONE of the tool's construction sites.
;;
;; `narrowing-continuation` measures what it builds, and both bound refusals
;; go through it. Seven other sites — the shape pass, the door refusal, the
;; unreadable and oversized narrowings, the arm-less file list, the plan
;; failure, and the uninitialised-server refusal — each spelled a map into
;; `refusal`'s `next-call` argument directly, and nothing measured any of
;; them. Measured before the fix, an unknown-field refusal on a 600-character
;; `workspace_root`:
;;
;;   next_call  {"tool":"relation_census","pool_size":8,"workspace_root":"aaa…"}
;;   rendered   661 UTF-8 bytes, against a 512-byte ceiling
;;
;; A ceiling that lives in one branch is not a ceiling, it is that branch's
;; habit. The bound exists because a continuation is a thing a caller reads,
;; pastes and execs, and it has to hold wherever the continuation was built —
;; so there is ONE constructor, every site goes through it, and a candidate
;; that cannot fit becomes a remedy naming the bytes it measured.
;; ---------------------------------------------------------------------------

(def ^:private long-root
  "A workspace root of 600 ASCII characters, Sol's exact probe.

   Every character is one UTF-8 byte, so this is not the multibyte question
   round twelve answered — it is the plainer one the tool never asked: a
   continuation nobody measured."
  (str "/" (apply str (repeat 599 \a))))

;; @spec MCP-OP-CENSUS-014
;; @spec MCP-OP-CENSUS-016
(deftest a-shape-refusal-on-a-long-root-measures-its-continuation
  (testing "the probe is Sol's: a 600-character root"
    (is (= 600 (count long-root))))

  (let [result (run {:workspace_root long-root :bogus 1})]
    (is (false? (:ok result)))
    (is (= "unknown-fields" (:reason result))
        (str "the probe did not reach the unknown-field row: " (pr-str result)))

    (testing "the continuation it emits fits the ceiling it publishes"
      (when-let [next-call (:next_call result)]
        (let [bytes (census/utf8-byte-count (json/generate-string next-call))]
          (is (<= bytes census/max-next-call-bytes)
              (str "the refusal emitted a " bytes
                   "-byte continuation under a " census/max-next-call-bytes
                   "-byte ceiling: " (json/generate-string next-call))))))

    (testing "a continuation that cannot fit is replaced by a remedy naming the bytes"
      (is (not (contains? result :next_call))
          (str "a 600-character root cannot be carried inside "
               census/max-next-call-bytes " bytes: "
               (pr-str (:next_call result))))
      (is (string? (:remedy result))
          "the refusal offers neither a continuation nor a remedy")
      (is (str/includes? (str (:remedy result))
                         (str census/max-next-call-bytes))
          (str "the remedy does not name the ceiling it compared against: "
               (pr-str (:remedy result))))
      (is (re-find #"\b66\d\b" (str (:remedy result)))
          (str "the remedy does not name the value it measured: "
               (pr-str (:remedy result)))))))

;; ---------------------------------------------------------------------------
;; THE RATCHET for Sol's round-fourteen item 9.
;;
;; The defect was not that one site got the arithmetic wrong; it was that
;; seven sites never did the arithmetic at all, and nothing in the suite could
;; tell. Two witnesses close that class rather than the instance.
;;
;; The first drives EVERY refusal kind both entrances can express — the shared
;; shape table for the pre-filesystem rows, `census/mcp-refusal-types` and
;; `census/cli-refusal-types` for the rest — against a workspace root of at
;; least 600 characters, and asserts of each answer that whatever continuation
;; it carries fits the 512-byte wire ceiling, and that a refusal which carries
;; none says what it measured instead. 600 characters is chosen so that the
;; NAIVE continuation is over the ceiling for every one of them: a witness
;; driven from a short root is green whether the ceiling is enforced or not.
;;
;; The second is structural, and it is the one that makes a site added NEXT
;; round fail here. It neuters the constructor — one function, redefined to
;; return no continuation at all — and asserts that no refusal from either
;; entrance then publishes a `next_call` or a `next-command`. A site that
;; spells its own map is invisible to a grep for a spelling it does not use
;; and invisible to a byte assertion on a short path; it is not invisible to
;; a constructor that has stopped constructing.
;; ---------------------------------------------------------------------------

(defn- deep-parent!
  "[top deep] — a temp tree whose deepest directory's canonical path is at
   least `n` characters. `top` is what the caller deletes."
  [n]
  (let [top (temp-dir)
        segment (apply str (repeat 200 \d))]
    (loop [dir top]
      (if (>= (count (.getCanonicalPath ^java.io.File dir)) n)
        [top dir]
        (let [deeper (io/file dir segment)]
          (.mkdirs deeper)
          (recur deeper))))))

(def ^:private long-root-chars
  "Long enough that the NAIVE continuation of every refusal is over the
   512-byte ceiling, so a witness driven from here cannot be green by
   accident."
  600)

(defn- mcp-refusal-drives
  "One drive per refusal `census/mcp-refusal-types` says the tool can return.

   Each drive is a thunk so the witness can run it twice — once counting the
   constructor's calls, once with the constructor neutered."
  [{:keys [arms bare broken]}]
  (let [named #(.getCanonicalPath ^java.io.File %)
        into-tree (fn [tree params]
                    (census-tool/execute-request!
                      {:project-root (named tree)} params))
        in-arms #(into-tree arms %)]
    [{:label :invalid-workspace-root
      :error-type :invalid-workspace-root
      :drive #(run {:workspace_root (str (named arms) "/does-not-exist")})}

     {:label :unknown-door-symbol
      :error-type :unknown-door-symbol
      :drive #(in-arms {:doors ["no-such-door"]})}

     {:label :unreadable-source-path
      :error-type :unreadable-source-path
      :drive #(in-arms {:files ["src/a/one.clj" "src/a/missing.clj"]})}

     {:label :source-too-large
      :error-type :source-too-large
      ;; `small.clj` is under the redefined cap and `one.clj` is over it, so
      ;; the request minus the oversized entry is still a request — the branch
      ;; that COMPUTES a continuation, which is the branch under test.
      :drive #(with-redefs [census/max-source-bytes 8]
                (in-arms {:files ["src/a/one.clj" "src/small.clj"]}))}

     {:label :no-fold-arms-found
      :error-type :no-fold-arms-found
      ;; Discovered, not named: the branch that computes a continuation.
      :drive #(into-tree bare {})}

     {:label :unparseable-file
      :error-type :unparseable-file
      :drive #(into-tree broken {})}

     {:label :census-worker-failure
      :error-type :census-worker-failure
      :drive #(with-redefs [census/census-file
                            (fn [& _] (throw (ex-info "boom" {})))]
                (in-arms {}))}

     {:label :census-failed
      :error-type :census-failed
      ;; A plan failure that names no type of its own: the fallback.
      :drive #(with-redefs [census/plan (fn [& _] {:ok false :error "boom"})]
                (in-arms {}))}

     {:label :too-many-candidate-files
      :error-type :too-many-candidate-files
      :drive #(with-redefs [census/max-scanned-files 1] (in-arms {}))}

     {:label :too-many-walk-entries
      :error-type :too-many-walk-entries
      :drive #(with-redefs [census/max-walk-entries 2] (in-arms {}))}

     {:label :census-adapter-failure
      :error-type :census-adapter-failure
      :drive #(with-redefs [census/discovery-facts
                            (fn [& _] (throw (ex-info "boom" {})))]
                (in-arms {}))}

     {:label :census-resource-exhausted
      :error-type :census-resource-exhausted
      :drive #(with-redefs [census/discovery-facts
                            (fn [& _] (throw (StackOverflowError.)))]
                (in-arms {}))}

     ;; Raised by the HANDLER, before a request exists: no workspace to make
     ;; long and no params to narrow. It is here anyway, because it builds a
     ;; continuation, and every site that builds one is in scope.
     {:label :server-not-initialized
      :error-type :server-not-initialized
      :drive (fn []
               (let [config @#'census-tool/runtime-config
                     saved @config
                     captured (atom nil)]
                 (try
                   (reset! config nil)
                   (census-tool/handle-relation-census
                     nil {} (fn [_ _ result] (reset! captured result)))
                   @captured
                   (finally (reset! config saved)))))}]))

(defn- next-call-bytes
  "The rendered size of an MCP continuation, or nil when there is none."
  [result]
  (when-let [next-call (:next_call result)]
    (census/utf8-byte-count (json/generate-string next-call))))

;; @spec MCP-OP-CENSUS-014
;; @spec MCP-OP-CENSUS-016
(deftest every-continuation-either-entrance-emits-fits-the-byte-ceiling
  (let [[arms-top arms] (deep-parent! long-root-chars)
        [bare-top bare] (deep-parent! long-root-chars)
        [broken-top broken] (deep-parent! long-root-chars)
        parent (temp-dir)]
    (try
      (spit-file! (io/file arms "src/a/one.clj") arm-source)
      (spit-file! (io/file arms "src/b/two.clj") arm-source)
      (spit-file! (io/file arms "src/small.clj") "()")
      (spit-file! (io/file bare "src/app/helpers.clj") "(ns app.helpers)")
      (spit-file! (io/file broken "src/app/broken.clj") malformed-arm-source)

      (testing "the probe roots really are long enough to break the ceiling"
        (doseq [tree [arms bare broken]]
          (is (<= long-root-chars (count (.getCanonicalPath ^java.io.File tree)))
              "a probe root is short enough to pass whether the ceiling holds or not")))

      (let [drives (mcp-refusal-drives {:arms arms :bare bare :broken broken})
            results
            (doall
              (for [{:keys [label error-type drive]} drives]
                (let [built (atom 0)
                      result (with-redefs
                               [census-tool/continuation
                                (counting built census-tool/continuation)]
                               (drive))]
                  {:label label :error-type error-type
                   :built @built :result result})))]

        (testing "the probes cover every refusal the tool declares it can return"
          (is (= census/mcp-refusal-types
                 (set (map (comp keyword :error_type :result) results)))
              (str "declared: " (pr-str census/mcp-refusal-types)
                   "; driven: "
                   (pr-str (set (map (comp keyword :error_type :result)
                                     results))))))

        (doseq [{:keys [label error-type built result]} results]
          (testing (str label " refuses as declared")
            (is (false? (:ok result))
                (str label " was accepted: " (pr-str result)))
            (is (= (name error-type) (:error_type result))
                (str label " refused as " (pr-str (:error_type result)))))

          (testing (str label " offers exactly one of a continuation and a remedy")
            (is (not= (contains? result :next_call)
                      (contains? result :remedy))
                (str label " offers "
                     (pr-str (select-keys result [:next_call :remedy])))))

          (when (contains? result :next_call)
            (testing (str label " emits a continuation that fits the wire")
              (let [bytes (next-call-bytes result)]
                (is (<= bytes census/max-next-call-bytes)
                    (str label " emitted a " bytes "-byte continuation under a "
                         census/max-next-call-bytes "-byte ceiling: "
                         (json/generate-string (:next_call result)))))
              (is (pos? built)
                  (str label " spelled its own continuation instead of "
                       "calling the constructor: "
                       (pr-str (:next_call result))))))))

      ;; The shared shape table, driven at the tool from a long root: every
      ;; row computes a continuation of its own, which is exactly how the
      ;; 600-byte one Sol measured got out.
      (let [long-ws (.getCanonicalPath ^java.io.File arms)]
        (doseq [rule census/request-shape-rules
                :when (keyword? (:mcp rule))
                :let [key [(:field rule) (:violation rule)]
                      probe (:mcp (get shape-rule-probes key))]
                :when probe]
          (let [built (atom 0)
                result (with-redefs
                         [census-tool/continuation
                          (counting built census-tool/continuation)]
                         (run (merge {:workspace_root long-ws} probe)))]
            (testing (str key " refuses from a 600-character root")
              (is (false? (:ok result))
                  (str key " was accepted: " (pr-str result))))
            (when (contains? result :next_call)
              (testing (str key " emits a continuation that fits the wire")
                (let [bytes (next-call-bytes result)]
                  (is (<= bytes census/max-next-call-bytes)
                      (str key " emitted a " bytes "-byte continuation: "
                           (json/generate-string (:next_call result)))))
                (is (pos? built)
                    (str key " spelled its own continuation instead of "
                         "calling the constructor")))))))

      ;; The CLI half, driven from the same machinery the anchor witness uses.
      (let [trees (cli-refusal-fixture! parent)
            results (run-cli-drives (cli-refusal-drives trees))]
        (testing "the CLI probes cover every refusal the op declares it can emit"
          (is (= census/cli-refusal-types
                 (set (map (comp :error-type :result) results)))))
        (doseq [{:keys [label built result]} results]
          (when-let [command (:next-command result)]
            (testing (str label " emits a command that fits the wire")
              (let [bytes (census/utf8-byte-count command)]
                (is (<= bytes census/max-next-call-bytes)
                    (str label " emitted a " bytes "-byte command under a "
                         census/max-next-call-bytes "-byte ceiling: "
                         command)))
              (is (pos? built)
                  (str label " spelled its own continuation instead of "
                       "calling cli-continuation: " command))))))

      (finally
        (delete-tree! parent)
        (doseq [top [arms-top bare-top broken-top]] (delete-tree! top))))))

;; @spec MCP-OP-CENSUS-014
(deftest the-constructors-are-the-only-continuation-construction-sites
  ;; Grep-free and spelling-free. Neuter the one constructor each entrance
  ;; owns; if anything still publishes a continuation, it built that
  ;; continuation somewhere else, whatever it named the local it built it in.
  (let [[arms-top arms] (deep-parent! long-root-chars)
        [bare-top bare] (deep-parent! long-root-chars)
        [broken-top broken] (deep-parent! long-root-chars)
        parent (temp-dir)]
    (try
      (spit-file! (io/file arms "src/a/one.clj") arm-source)
      (spit-file! (io/file arms "src/b/two.clj") arm-source)
      (spit-file! (io/file arms "src/small.clj") "()")
      (spit-file! (io/file bare "src/app/helpers.clj") "(ns app.helpers)")
      (spit-file! (io/file broken "src/app/broken.clj") malformed-arm-source)

      (testing "with the tool's constructor neutered, no tool refusal carries a next_call"
        (let [drives (mcp-refusal-drives {:arms arms :bare bare :broken broken})]
          (with-redefs [census-tool/continuation
                        (fn [_] {:candidate nil :bytes 0 :next-call nil})]
            (doseq [{:keys [label drive]} drives]
              (let [result (drive)]
                (is (not (contains? result :next_call))
                    (str label " published a next_call the constructor did "
                         "not build: " (pr-str (:next_call result))))))
            (doseq [rule census/request-shape-rules
                    :when (keyword? (:mcp rule))
                    :let [key [(:field rule) (:violation rule)]
                          probe (:mcp (get shape-rule-probes key))]
                    :when probe]
              (let [result (run (merge {:workspace_root
                                        (.getCanonicalPath ^java.io.File arms)}
                                       probe))]
                (is (not (contains? result :next_call))
                    (str key " published a next_call the constructor did "
                         "not build: " (pr-str (:next_call result)))))))))

      (testing "with the op's constructor neutered, no CLI refusal carries a next-command"
        (let [trees (cli-refusal-fixture! parent)]
          (doseq [{:keys [label opts around]} (cli-refusal-drives trees)]
            (let [thunk (fn []
                          (with-redefs [census/cli-continuation (fn [& _] nil)]
                            (binding [*out* (java.io.StringWriter.)]
                              (core/run (assoc opts :op :relation-census)))))
                  result ((or around (fn [f] (f))) thunk)]
              (is (not (contains? result :next-command))
                  (str label " published a next-command cli-continuation did "
                       "not build: " (pr-str (:next-command result))))
              (is (not (contains? result :next-command-argv))
                  (str label " published a next-command-argv "
                       "cli-continuation did not build: "
                       (pr-str (:next-command-argv result))))))))

      (finally
        (delete-tree! parent)
        (doseq [top [arms-top bare-top broken-top]] (delete-tree! top))))))

;; ---------------------------------------------------------------------------
;; Opus's round-seventeen NO-GO item 1, blocking. Round sixteen's item 7 landed
;; the sentence "NO FIELD of any refusal either entrance emits shall be
;; unbounded", and applied the bound at the tool's `refusal` CONSTRUCTOR —
;; which is precisely the placement the same sentence forbids. Two refusals the
;; MCP entrance emits are not built by that constructor, and the workspace
;; router's is one of them: a 10,001-character `workspace_root` came back as a
;; 10,540-byte refusal carrying a 10,007-character field and no truncation
;; marker, hours after the rule shipped.
;;
;; Rounds fifteen and sixteen were "one entrance proved, both claimed". This is
;; "one refusal SHAPE proved, every shape claimed": the round-sixteen witness
;; drove exactly one MCP shape, `{:files [long-name]}`, which goes through the
;; constructor, and never drove `workspace_root` at all.
;;
;; So the witness below is TOTAL by construction on two derived axes, and it is
;; the derivation that makes it a ratchet rather than a longer list:
;;
;;   * every REQUEST FIELD either entrance declares — the MCP set read out of
;;     `census-tool-schema`'s own `:properties`, the CLI set read out of the
;;     shared `census/request-shape-rules` table — each driven with a
;;     10,001-character value. A field added to the schema without a hostile
;;     drive fails the equality assertion before it can ship unbounded.
;;   * every REFUSAL SHAPE either entrance declares — the round-sixteen
;;     enumerations `census/mcp-refusal-types` and `census/cli-refusal-types`,
;;     driven through the machinery that already pins them TOTAL — each
;;     asserted to carry no field over the ceiling.
;; ---------------------------------------------------------------------------

(def ^:private hostile-field-chars
  "The reviewer's drive length: over the 1,024-character field bound by an
   order of magnitude, so nothing here can be green by being short."
  10001)

(def ^:private hostile-string
  (apply str (repeat hostile-field-chars \a)))

(def ^:private refusal-field-ceiling
  "The field bound plus the marker that says it fired. Owned by the witness
   rather than read back from the code it checks."
  (+ 1024 64))

(defn- declared-mcp-request-fields
  "Every request field the tool's OWN published schema declares, plus the
   unknown-field row the schema expresses as `additionalProperties false`.

   Derived, never listed: a property added to `census-tool-schema` appears
   here on the next run and fails the drive-table equality below."
  []
  (conj (set (keys (:properties census-tool/census-tool-schema)))
        ::unknown-field))

(defn- declared-cli-request-fields
  "Every request field the SHARED shape table names. Derived for the same
   reason, out of the table both entrances already validate against."
  []
  (set (map :field census/request-shape-rules)))

(defn- echoed-hostile-input?
  "True when a refusal carried the caller's hostile input back out at all.

   A refusal that never echoes the input cannot be asked for a truncation
   marker; one that does must carry the marker, because a silently shortened
   field leaves the caller comparing it against what they sent."
  [strings]
  (boolean (some #(str/includes? % (subs hostile-string 0 200)) strings)))

(defn- assert-bounded!
  [label result]
  (let [strings (all-strings result)]
    (is (false? (:ok result))
        (str label " was accepted: " (pr-str (select-keys result [:ok]))))
    (doseq [text strings]
      (is (<= (count text) refusal-field-ceiling)
          (str label " published a " (count text) "-character field under a "
               refusal-field-ceiling "-character ceiling: "
               (pr-str (str/join (take 80 text))))))
    (is (< (count (json/generate-string result)) 8192)
        (str label " renders " (count (json/generate-string result))
             " bytes, against a 4096-byte receipt cap"))
    (when (echoed-hostile-input? strings)
      (is (some #(str/includes? % "truncated") strings)
          (str label " echoed the caller's input back and shortened it "
               "without saying so")))))

;; @spec MCP-OP-CENSUS-014
(deftest every-refusal-shape-either-entrance-emits-is-bounded-at-the-entrance
  (let [parent (temp-dir)
        ws (io/file parent "ws")]
    (try
      (spit-file! (io/file ws "src/a/one.clj") arm-source)
      (let [named (.getCanonicalPath ws)
            mcp-drives
            ;; `workspace_root` is driven ABSOLUTE, so it reaches the
            ;; workspace ROUTER rather than stopping at the shape pass: the
            ;; router's refusal is the one the constructor-level bound never
            ;; saw.
            {"workspace_root" {:workspace_root (str "/nope/" hostile-string)}
             "files" {:files [hostile-string]}
             "doors" {:doors [hostile-string]}
             ;; Not a string field, and driven anyway so the enumeration has
             ;; no exceptions: a set with a carve-out is a set nobody can
             ;; check.
             "pool_size" {:files ["src/a/one.clj"] :pool_size 0}
             ::unknown-field {(keyword hostile-string) 1}}
            cli-drives
            {:dir {:dir hostile-string}
             :files {:file hostile-string}
             :doors {:dir named :doors hostile-string}
             :paths {:dir (str undecodable-probe-path hostile-string)}
             :pool-size {:dir named :threads hostile-string}
             :unknown-fields {:dir named (keyword hostile-string) 1}}]

        (testing "the hostile drives cover every request field the tool declares"
          (is (= (declared-mcp-request-fields) (set (keys mcp-drives)))
              (str "declared: " (pr-str (declared-mcp-request-fields))
                   "; driven: " (pr-str (set (keys mcp-drives))))))

        (testing "the hostile drives cover every request field the CLI declares"
          (is (= (declared-cli-request-fields) (set (keys cli-drives)))
              (str "declared: " (pr-str (declared-cli-request-fields))
                   "; driven: " (pr-str (set (keys cli-drives))))))

        (doseq [[field params] (sort-by (comp str key) mcp-drives)]
          (testing (str "the tool bounds a hostile " field)
            (assert-bounded!
              (str "tool/" field)
              (refusal-or-throw
                #(census-tool/execute-request! {:project-root named} params)))))

        (doseq [[field opts] (sort-by (comp str key) cli-drives)]
          (testing (str "the CLI bounds a hostile " field)
            (assert-bounded!
              (str "cli/" field)
              (refusal-or-throw
                #(binding [*out* (java.io.StringWriter.)]
                   (core/run (assoc opts :op :relation-census)))))))

        ;; The reviewer's exact receipt, pinned on its own so a regression
        ;; names the shape rather than a field count.
        (testing "the router's own refusal is bounded, which it was not"
          (let [result (census-tool/execute-request!
                         {:project-root named}
                         {:workspace_root (str "/nope/" hostile-string)})]
            (is (= "invalid-workspace-root" (:error_type result)))
            (is (<= (count (json/generate-string result)) 8192)
                (str "the router refusal renders "
                     (count (json/generate-string result))
                     " bytes; the reviewer measured 10540"))
            (is (>= 1088 (reduce max 0 (map count (all-strings result))))
                (str "the router refusal's longest field is "
                     (reduce max 0 (map count (all-strings result)))
                     " characters; the reviewer measured 10007")))))
      (finally
        (delete-tree! parent)))))

;; @spec MCP-OP-CENSUS-014
(deftest every-declared-refusal-shape-carries-no-field-over-the-ceiling
  ;; The second axis: not the fields going IN but the shapes coming OUT. The
  ;; two enumerations are the ones round sixteen built and pinned TOTAL; this
  ;; asks each of them the length question the round-seventeen router refusal
  ;; answered wrong.
  (let [[arms-top arms] (deep-parent! long-root-chars)
        [bare-top bare] (deep-parent! long-root-chars)
        [broken-top broken] (deep-parent! long-root-chars)
        parent (temp-dir)]
    (try
      (spit-file! (io/file arms "src/a/one.clj") arm-source)
      (spit-file! (io/file arms "src/b/two.clj") arm-source)
      (spit-file! (io/file arms "src/small.clj") "()")
      (spit-file! (io/file bare "src/app/helpers.clj") "(ns app.helpers)")
      (spit-file! (io/file broken "src/app/broken.clj") malformed-arm-source)

      (let [results (for [{:keys [label drive]}
                          (mcp-refusal-drives
                            {:arms arms :bare bare :broken broken})]
                      {:label label :result (refusal-or-throw drive)})]
        (testing "the drives still cover every refusal the tool declares"
          (is (= census/mcp-refusal-types
                 (set (map (comp keyword :error_type :result) results)))))
        (doseq [{:keys [label result]} results]
          (testing (str label " carries no field over the ceiling")
            (assert-bounded! (str "tool-shape/" label) result))))

      (let [trees (cli-refusal-fixture! parent)
            results (run-cli-drives (cli-refusal-drives trees))]
        (testing "the drives still cover every refusal the CLI declares"
          (is (= census/cli-refusal-types
                 (set (map (comp :error-type :result) results)))))
        (doseq [{:keys [label result]} results]
          (testing (str label " carries no field over the ceiling")
            (assert-bounded! (str "cli-shape/" label) result))))

      (finally
        (delete-tree! parent)
        (doseq [top [arms-top bare-top broken-top]] (delete-tree! top))))))

;; ---------------------------------------------------------------------------
;; Opus's round-seventeen NO-GO item 2, blocking. `overflow-measurement` was
;; written for round sixteen's item 5 — "a remedy blaming the workspace path on
;; a twenty-four-character root" — and weighs exactly TWO of the candidate's
;; parts, `workspace_root` and `files`. The candidate carries more than two:
;; the `:refusal loaded` branch builds it as `(assoc (dissoc params :files) …)`
;; so EVERY caller-supplied option rides through, `doors` included.
;;
;; With a 19-character root and 600-odd bytes of `doors`, `(>= root-bytes
;; entry-bytes)` wins and the remedy says "workspace_root alone measures 19 of
;; those bytes — retry with workspace_root reaching the same tree by a shorter
;; path". Nineteen bytes of 723: 2.6% named as the cause. A caller who follows
;; it shortens the one thing that is not the problem and receives the identical
;; refusal — the loop-with-a-receipt MCP-OP-CENSUS-014 forbids a continuation,
;; arriving in a remedy instead. That is round-sixteen item 5's own rejected
;; receipt, from the function written to replace it, one round later.
;;
;; Why the round-sixteen witness was green over it: its three fixtures — 500
;; short entries, one 609-byte entry, a 647-byte root — vary only the two
;; dimensions the fix weighs. A witness whose fixtures ARE the fix's own two
;; dimensions cannot see a third.
;;
;; So the rule this round states is not "weigh `doors` too". It is that the
;; cause is DERIVED by walking the candidate's ACTUAL fields and their measured
;; byte weights and naming the heaviest, and that a named field list is
;; FORBIDDEN. The witness proves the walk rather than the list: it puts the
;; bulk in a field whose name appears nowhere in the source, and asserts the
;; remedy names that field.
;; ---------------------------------------------------------------------------

(defn- overflow-remedy-for
  "The remedy the tool would publish for a candidate too large to send."
  [candidate]
  (let [{:keys [overflow]} (census-tool/continuation candidate)]
    (is (some? overflow)
        (str "the candidate FITTED, so this drive measures nothing: "
             (census/utf8-byte-count (json/generate-string candidate))
             " bytes"))
    {:overflow overflow
     :remedy (#'census-tool/continuation-overflow-remedy overflow)}))

(defn- bulk-value
  "A vector of entries weighing about `n` bytes in rendered JSON."
  [n]
  (mapv (fn [i] (str "d" i (apply str (repeat (quot n 4) \x)))) (range 4)))

;; @spec MCP-OP-CENSUS-014
(deftest the-overflow-remedy-names-the-heaviest-field-it-measured
  (let [parent (temp-dir)
        ws (io/file parent "ws")]
    (try
      (spit-file! (io/file ws "src/app/arm.clj") arm-source)
      (let [named (.getCanonicalPath ws)]

        (testing "the reviewer's receipt: a doors-heavy continuation on a short root"
          ;; Driven through `execute-request!`, the way the reviewer reached
          ;; it, so this is the field behaviour and not a unit of the private
          ;; measurement.
          (doseq [doors [(mapv (fn [i] (str "d" i (apply str (repeat 150 \x))))
                               (range 4))
                         (mapv (fn [i] (str "d" i (apply str (repeat 80 \x))))
                               (range 8))
                         (mapv (fn [i] (str "d" i (apply str (repeat 300 \x))))
                               (range 2))]]
            (let [result (census-tool/execute-request!
                           {:project-root named}
                           {:files ["src/app/arm.clj" "src/app/missing.clj"]
                            :doors doors})
                  remedy (str (:remedy result))]
              (is (false? (:ok result)))
              (is (not (str/includes? remedy "the length of the workspace path"))
                  (str "the remedy blames the workspace path for a "
                       "continuation whose bulk is doors: " (pr-str remedy)))
              (is (str/includes? remedy "doors")
                  (str "the remedy does not name the field it measured: "
                       (pr-str remedy))))))

        (testing "the cause is WALKED, not listed: a field named nowhere in the source"
          ;; The falsifier for "derived". `synthetic_option` appears in no
          ;; namespace this witness checks; a measurement built from a named
          ;; field list cannot name it, whatever that list contains.
          (let [{:keys [overflow remedy]}
                (overflow-remedy-for {:workspace_root "/ws"
                                      :files ["src/a.clj"]
                                      :synthetic_option (bulk-value 700)})]
            (is (str/includes? remedy "synthetic_option")
                (str "the remedy does not name the synthetic field that "
                     "carries the bulk: " (pr-str remedy)))
            (is (not (str/includes? remedy "the length of the workspace path"))
                (str "the remedy blames the workspace path: " (pr-str remedy)))
            (is (>= (* 2 (:measured overflow)) (:bytes overflow))
                (str "the named cause accounts for " (:measured overflow)
                     " of " (:bytes overflow)
                     " bytes — a minority named as the cause is the defect"))))

        (testing "the heaviest field wins wherever the bulk is put"
          ;; Three DIFFERENT synthetic names, so nothing can be green by
          ;; having learned one of them.
          (doseq [field [:doors :pool_hint :some_future_option]]
            (let [{:keys [remedy]}
                  (overflow-remedy-for (assoc {:workspace_root "/ws"
                                               :files ["src/a.clj"]}
                                              field (bulk-value 700)))]
              (is (str/includes? remedy (name field))
                  (str "the bulk was in " field " and the remedy named "
                       "something else: " (pr-str remedy))))))

        (testing "the round-sixteen fixtures still answer as round sixteen fixed them"
          (let [long-root (str "/" (apply str (repeat 647 \r)))
                {:keys [overflow remedy]}
                (overflow-remedy-for {:workspace_root long-root
                                      :files ["src/a.clj"]})]
            (is (= :workspace-root-length (:cause overflow))
                (str "a 647-byte root measured as " (pr-str overflow)))
            (is (str/includes? remedy "the length of the workspace path")))

          (let [{:keys [overflow remedy]}
                (overflow-remedy-for
                  {:workspace_root "/ws"
                   :files (mapv #(str "src/a" % ".clj") (range 500))})]
            (is (= :entry-count (:cause overflow))
                (str "500 short entries measured as " (pr-str overflow)))
            (is (str/includes? remedy "the NUMBER of sources")))

          (let [{:keys [overflow remedy]}
                (overflow-remedy-for
                  {:workspace_root "/ws"
                   :files [(str "src/" (apply str (repeat 609 \e)) ".clj")]})]
            (is (= :entry-length (:cause overflow))
                (str "one 609-byte entry measured as " (pr-str overflow)))
            (is (str/includes? remedy "the length of a source path"))))

        (testing "whatever it names, it named the HEAVIEST field of the candidate"
          ;; The invariant, computed here from the candidate rather than read
          ;; back from the code under test: the field the remedy names is the
          ;; one carrying the most bytes on the wire, so no caller is ever
          ;; pointed at a minority.
          (doseq [candidate [{:workspace_root "/ws" :files ["src/a.clj"]
                              :doors (bulk-value 700)}
                             {:workspace_root (str "/" (apply str (repeat 647 \r)))
                              :files ["src/a.clj"]}
                             {:workspace_root "/ws"
                              :files (mapv #(str "src/a" % ".clj") (range 500))}
                             {:workspace_root "/ws" :files ["src/a.clj"]
                              :some_future_option (bulk-value 700)}]]
            (let [{:keys [overflow]} (overflow-remedy-for candidate)
                  weights (into {} (for [[k v] (dissoc candidate :tool)]
                                     [(name k)
                                      (census/utf8-byte-count
                                        (json/generate-string {k v}))]))
                  heaviest (key (apply max-key val weights))]
              (is (= heaviest (:field overflow))
                  (str "the remedy named " (pr-str (:field overflow))
                       " while the heaviest field is " (pr-str heaviest)
                       ": " (pr-str weights)))))))
      (finally
        (delete-tree! parent)))))

;; ---------------------------------------------------------------------------
;; Opus's round-seventeen NO-GO item 3. `core/denied-ancestor`'s docstring says
;; "the nearest EXISTING ancestor DIRECTORY"; the code never asks
;; `fs/directory?`. A mode-644 regular file is readable and NOT executable, so
;; it satisfies `(when-not (and readable? executable?))` — and an ordinary
;; source file in a path prefix, `src/app/afile.clj/x.clj`, is reported as a
;; denied DIRECTORY.
;;
;; Three defects in one. The refusal states a falsehood: that file is readable,
;; and it is not a directory. The remedy cannot be followed: it is already
;; readable, and making it more readable changes nothing. And the two entrances
;; publish DIFFERENT CAUSES for the same observation — the tool says
;; `not-found` (ENOTDIR did not resolve), the CLI says `parent-denied` — which
;; is precisely what the shared `mcp-paths/source-refusal-causes` vocabulary
;; was added in round sixteen to make impossible, and what the round-sixteen
;; witness asserts for the symlink loop and the long name but not for this
;; shape.
;;
;; So the witness is the reviewer's whole ten-shape enumeration rather than an
;; eleventh case, and it is TOTAL against the vocabulary: every cause
;; `source-refusal-causes` declares for a NAMED source is covered by a row, so
;; a cause added to that set without a parity row fails here.
;; ---------------------------------------------------------------------------

;; ---------------------------------------------------------------------------
;; ROUND NINETEEN, item 3 — Sol's round-eighteen item 3.
;;
;; Round eighteen's brief claimed "a ten-shape parity enumeration, all
;; agreeing." The reviewer ran it and found EIGHT agreeing and two declared
;; divergences that were not refusals at all but successful CLI reads:
;;
;;   escape          expected outside-project           cli nil  agree false
;;   wrong-extension expected not-a-relative-source-path cli nil agree false
;;
;; Two defects in one sentence. The first is that the claim was false: a
;; witness whose green depends on a hand-written exemption table cannot be
;; quoted as "all agreeing", because the table is where the disagreement went.
;; The second is that the exemptions were the divergence Sol's item 2 calls
;; blocking, plus its lexical sibling.
;;
;; Both are closed rather than re-declared. `escape` agrees because round
;; nineteen's containment fence refuses it (item 2); `wrong-extension` agrees
;; because a source the CLI is NAMED must carry a source extension, which is
;; the rule the CLI's own walk has always applied to every member it discovers
;; (`census/source-name-pattern`) and which the tool applies lexically. The
;; exemption table is GONE, and the witness now PRINTS the enumeration it
;; compared and asserts the disagreeing set is empty — so "all agreeing" is a
;; computed fact with a printed derivation rather than a sentence in a brief.
;; ---------------------------------------------------------------------------

(defn- source-parity-rows!
  "One tree per shape, built under `parent`, with the relative path to drive."
  [^java.io.File parent]
  (let [ws (io/file parent "ws")
        outside (io/file parent "outside")
        long-name (str "src/app/"
                       (str/join "/" (repeat 12 (apply str (repeat 90 \n))))
                       ".clj")]
    (spit-file! (io/file ws "src/app/arm.clj") arm-source)
    (spit-file! (io/file ws "src/app/afile.clj") "(ns app.afile)")
    (spit-file! (io/file ws "src/app/denied.clj") arm-source)
    (deny-reads! (io/file ws "src/app/denied.clj"))
    (spit-file! (io/file ws "src/app/locked/inner.clj") arm-source)
    (deny-traversal! (io/file ws "src/app/locked"))
    (.mkdirs (io/file ws "src/app/dirnamed.clj"))
    (spit-file! (io/file outside "escape.clj") arm-source)
    (spit-file! (io/file ws "src/app/notes.txt") "(ns app.notes)")
    (java.nio.file.Files/createSymbolicLink
      (.toPath (io/file ws "src/app/loopa.clj"))
      (.toPath (io/file "loopb.clj")) (make-array FileAttribute 0))
    (java.nio.file.Files/createSymbolicLink
      (.toPath (io/file ws "src/app/loopb.clj"))
      (.toPath (io/file "loopa.clj")) (make-array FileAttribute 0))
    (java.nio.file.Files/createSymbolicLink
      (.toPath (io/file ws "src/app/escape.clj"))
      (.toPath (io/file "../../../outside/escape.clj"))
      (make-array FileAttribute 0))
    {:ws ws
     :fifo (mkfifo! (io/file ws "src/app/pipe.clj"))
     :rows [{:shape :missing :path "src/app/missing.clj" :cause :not-found}
            {:shape :denied-file :path "src/app/denied.clj"
             :cause :permission-denied}
            {:shape :denied-parent :path "src/app/locked/inner.clj"
             :cause :parent-denied}
            {:shape :dir-named-clj :path "src/app/dirnamed.clj"
             :cause :not-a-regular-file}
            {:shape :fifo :path "src/app/pipe.clj"
             :cause :not-a-regular-file}
            {:shape :symlink-loop :path "src/app/loopa.clj" :cause :not-found}
            {:shape :name-too-long :path long-name :cause :not-found}
            ;; The finding. A readable regular file in a path PREFIX.
            {:shape :enotdir-component :path "src/app/afile.clj/x.clj"
             :cause :not-found}
            {:shape :escape :path "src/app/escape.clj"
             :cause :outside-project}
            {:shape :wrong-extension :path "src/app/notes.txt"
             :cause :not-a-relative-source-path}]}))

;; @spec MCP-OP-CENSUS-014
;; @spec MCP-OP-CENSUS-019
(deftest the-two-entrances-name-the-same-cause-for-the-same-observation
  (let [parent (temp-dir)]
    (try
      (let [{:keys [ws rows]} (source-parity-rows! parent)
            named (.getCanonicalPath ^java.io.File ws)
            observed
            (doall
              (for [{:keys [shape path cause]} rows]
                (let [tool (census-tool/execute-request!
                             {:project-root named} {:files [path]})
                      cli (refusal-or-throw
                            #(core/run-relation-census
                               {:file (str named "/" path)}))]
                  {:shape shape :expected cause
                   :tool (:cause tool) :cli (:cause cli)})))]

        (testing "every named-source cause the shared vocabulary declares has a row"
          ;; Derived totality, with three declared exclusions and the reason
          ;; for each. `:directory-denied` is the WALK's cause and has its own
          ;; witness; `:read-failed-after-fence` is raised BETWEEN the fence
          ;; and the open, so no path can provoke it; `:unresolvable-source-path`
          ;; is the `:else` of the resolver's exception analysis and needs a
          ;; throw that is not the filesystem answering. Every other member is
          ;; a row above, so a cause added to the vocabulary without a parity
          ;; row fails here.
          (is (= (disj mcp-paths/source-refusal-causes
                       :directory-denied :read-failed-after-fence
                       :unresolvable-source-path)
                 (set (map :cause rows)))
              (str "vocabulary: " (pr-str mcp-paths/source-refusal-causes)
                   "; rows: " (pr-str (set (map :cause rows))))))

        ;; The enumeration is PRINTED, one line per shape, with the agreement
        ;; each row computed. Sol's round-eighteen item 3: the previous brief
        ;; claimed all ten agreed while two rows were successful CLI reads
        ;; parked in an exemption table. A claim about a set is checkable only
        ;; when the set and its verdicts are on the page.
        (println "PARITY-ENUMERATION:")
        (doseq [{:keys [shape expected tool cli]} observed]
          (println (format "  %-20s expected %-28s tool %-28s cli %-28s agree %s"
                           (name shape) (name expected)
                           (pr-str tool) (pr-str cli)
                           (= (name expected) tool (some-> cli name)))))

        (testing "the enumeration this witness printed is the one it compared"
          (is (= (set (map :shape rows)) (set (map :shape observed)))
              (str "printed: " (pr-str (set (map :shape observed)))
                   "; declared: " (pr-str (set (map :shape rows)))))
          (is (= 10 (count observed))
              (str "the enumeration is " (count observed)
                   " shapes, not the ten this witness reports")))

        (testing "EVERY shape in the printed enumeration agrees"
          ;; No exemption table. A divergence is a defect, and the set of them
          ;; is asserted empty rather than listed somewhere a reader has to
          ;; find before they can discount the claim.
          (let [disagreeing (into (sorted-set)
                                  (comp (remove (fn [{:keys [expected tool cli]}]
                                                  (= (name expected) tool
                                                     (some-> cli name))))
                                        (map :shape))
                                  observed)]
            (is (= #{} disagreeing)
                (str "these shapes do not get one cause from both entrances: "
                     (pr-str disagreeing) " — full enumeration: "
                     (pr-str (vec observed))))))

        (doseq [{:keys [shape expected tool cli]} observed]
          (testing (str shape " is named from the shared vocabulary")
            (is (contains? mcp-paths/source-refusal-causes
                           (some-> tool keyword))
                (str shape ": the tool published " (pr-str tool))))

          (testing (str shape " gets ONE cause from both entrances")
            (is (contains? mcp-paths/source-refusal-causes cli)
                (str shape ": the CLI published " (pr-str cli)))
            (is (= (name expected) tool)
                (str shape ": the tool published " (pr-str tool)))
            (is (= expected cli)
                (str shape ": the CLI published " (pr-str cli)))
            (is (= (some-> cli name) tool)
                (str shape ": CLI " (pr-str cli) " vs tool "
                     (pr-str tool)))))

        (testing "the ENOTDIR remedy does not tell the caller to chmod a source file"
          (let [cli (refusal-or-throw
                      #(core/run-relation-census
                         {:file (str named "/src/app/afile.clj/x.clj")}))]
            (is (not (str/includes? (str (:remedy cli)) "afile.clj readable"))
                (str "the CLI asks the caller to make a readable regular file "
                     "readable: " (pr-str (:remedy cli)))))))
      (finally
        (allow-traversal! (io/file parent "ws/src/app/locked"))
        (allow-reads! (io/file parent "ws/src/app/denied.clj"))
        (delete-tree! parent)))))

;; ---------------------------------------------------------------------------
;; Opus's round-seventeen items 4 and 5, one class with two doors: a refusal
;; whose SUBJECT is the workspace root itself has no name for it, so one
;; entrance prints the server's absolute path and both entrances print nothing
;; at all.
;;
;; Item 4, `mcp_paths/unreadable-ancestor`: `(if (str/blank? shown) (.toString
;; dir) shown)` — when the unreadable ancestor IS the root, `relativize` yields
;; `""` and the fallback publishes the absolute path, from the namespace whose
;; own docstring forbids exactly that.
;;
;; Item 5, `core.clj` and `mcp_relation_census.clj` alike: `census-discovery`
;; records `rel-dir` for the failing directory, which for the root is `""`, and
;; both entrances interpolate it into three sentences — "the directory  may not
;; be read", "make  readable under …". A receipt that names no subject is the
;; class House-rule 20 exists for, and the two entrances agreeing on nothing is
;; not parity.
;;
;; One rule, one spelling, one place: `census/workspace-root-token`. The root is
;; NAMED, never empty and never absolute, wherever a workspace-relative path
;; would be blank.
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-CENSUS-014
;; @spec MCP-OP-CENSUS-018
(deftest a-refusal-whose-subject-is-the-root-names-the-root
  (let [parent (temp-dir)
        ws (io/file parent "ws")]
    (try
      (spit-file! (io/file ws "src/app/arm.clj") arm-source)
      (let [named (.getCanonicalPath ws)]
        (deny-traversal! ws)

        (testing "item 4: the unreadable ancestor IS the root, at the resolver"
          (let [refusal (mcp-paths/resolve-source-path
                          (mcp-paths/real-root (.getParentFile ws))
                          (str (.getName ws) "/src/app/arm.clj"))]
            ;; Driven from the PARENT so the root is reachable and the
            ;; unreadable directory is inside it; the tool drive below is the
            ;; shape the reviewer measured.
            (is (false? (:ok refusal))
                (str "the resolver accepted a path under a chmod-000 "
                     "directory: " (pr-str refusal)))))

        (testing "item 4: the unreadable ancestor IS the root, at the tool"
          (let [result (census-tool/execute-request!
                         {:project-root named} {:files ["src/app/arm.clj"]})]
            (is (false? (:ok result)))
            (is (not (str/includes? (str (:error result)) named))
                (str "the refusal published the server's absolute root: "
                     (pr-str (:error result))))
            (is (str/includes? (str (:error result))
                               census/workspace-root-token)
                (str "the refusal does not NAME the root it is about: "
                     (pr-str (:error result))))))

        (testing "item 5: a root the walk cannot enter names its subject"
          (doseq [[entrance result directory error remedy]
                  [(let [r (census-tool/execute-request!
                             {:project-root named} {})]
                     [:tool r (:directory r) (:error r) (:remedy r)])
                   (let [r (refusal-or-throw
                             #(core/run-relation-census {:dir named}))]
                     [:cli r (:directory r) (:error r) (:remedy r)])]]
            (is (false? (:ok result))
                (str entrance " accepted a tree it could not enter: "
                     (pr-str (:ok result))))
            (is (not (str/blank? (str directory)))
                (str entrance " published :directory " (pr-str directory)
                     " — a receipt that names no subject"))
            (is (= census/workspace-root-token directory)
                (str entrance " published :directory " (pr-str directory)))
            (is (not= (str directory) named)
                (str entrance " published the server's absolute root as the "
                     "directory: " (pr-str directory)))
            (doseq [[field text] [[:error error] [:remedy remedy]]]
              (is (str/includes? (str text) census/workspace-root-token)
                  (str entrance " " field " does not name its subject: "
                       (pr-str text)))
              (is (not (re-find #"\s{2,}" (str text)))
                  (str entrance " " field " interpolated an empty name: "
                       (pr-str text)))))))
      (finally
        (allow-traversal! ws)
        (delete-tree! parent)))))

;; ---------------------------------------------------------------------------
;; ROUND NINETEEN, item 2 — Sol's round-eighteen BLOCKING finding, and the
;; reversal of the rule round eighteen wrote here.
;;
;; Round seventeen found the CLI's `:file` reading through a symlink its own
;; `:dir` walk counts as `skipped-outside-root`, and round eighteen DECLARED
;; the divergence rather than closing it: "NAMING IS NOT WALKING — the CLI's
;; `:file` has no project root, so it reads what the operator typed, exactly as
;; `cat` would." Reproduced at 3b7904a, the four cells that rule produced:
;;
;;   MCP-WALK      {:ok true,  :skipped 1, :files 1, :read-secret? false}
;;   MCP-NAMED     {:ok false, :cause "outside-project",  :read-secret? false}
;;   CLI-WALK      {:ok true,  :skipped 1, :files 1, :read-secret? false}
;;   CLI-NAMED     {:ok true,              :files 1, :read-secret? TRUE}
;;
;; The reviewer's ruling, and it stands: "The EARS text does contain the
;; implemented rule, but that rule AUTHORISES THE FORBIDDEN READ." A census
;; that publishes the bytes of a file outside the tree it is censusing is not
;; saved by having said in advance that it would. A "no path escapes" claim
;; that is false at one entrance is false.
;;
;; THE RULE, round nineteen, replacing round eighteen's:
;;
;;   NO CENSUS READS A SOURCE WHOSE REAL PATH LEAVES THE WORKSPACE, at either
;;   entrance, however the source was reached. `toRealPath` resolves every link
;;   in the path — a chain, an absolute target, a sibling workspace — and the
;;   answer is compared against the workspace the request named.
;;
;;   * The WORKSPACE is the tree the request named: `:dir` when the request
;;     gives one, and the `:file` itself when it names only a file, because a
;;     request that names one source is a census over exactly that source. At
;;     the tool it is the declared `workspace_root`, which is what it always
;;     was.
;;   * A source the WALK discovered that leaves the workspace is COUNTED
;;     (`skipped_outside_root`) and never read: the walk never named it, so an
;;     incidental link is not a refusal of the whole census. Unchanged.
;;   * A source the request NAMED that leaves the workspace is REFUSED, typed,
;;     naming the link as the request spelled it and NEVER the target — at the
;;     tool `unreadable-source-path` / `outside-project`, at the CLI
;;     `:file-outside-workspace` / `:outside-project`, one shared cause.
;;   * A link whose real path stays INSIDE the workspace is FOLLOWED by both
;;     the walk and `:file`, which is the half round eighteen was right about:
;;     a source linked into `src/` in an ordinary repository is an ordinary
;;     source, and refusing it would buy nothing.
;;
;; What survives from round eighteen is the OTHER end, round fifteen's:
;; `census-root` canonicalises a `:dir` that names a symlink, because a named
;; ROOT is resolved and then becomes the fence. Naming still resolves; what it
;; no longer does is exempt.
;; ---------------------------------------------------------------------------

(def ^:private escape-secret
  "A marker that can only appear in a receipt by being READ.

   The outside arms write into a distinctly named key, so the assertion is
   evidence rather than an inference from a file count."
  "SECRET-OUTSIDE")

(defn- escape-fixture!
  "One workspace whose `src/app` holds four links: three that escape it by
   different routes, and one that does not."
  [^java.io.File parent]
  (let [ws (io/file parent "ws")
        sibling (io/file parent "ws2")
        outside (io/file parent "outside")
        link! (fn [from to]
                (Files/createSymbolicLink
                  (.toPath ^java.io.File from) (.toPath ^java.io.File to)
                  (make-array FileAttribute 0)))
        secret-source (str/replace arm-source ":xs" (str ":" escape-secret))]
    (spit-file! (io/file ws "src/app/arm.clj") arm-source)
    (spit-file! (io/file ws "lib/inside.clj")
                (str/replace arm-source ":xs" ":INSIDE-TARGET"))
    (spit-file! (io/file outside "secret.clj") secret-source)
    (spit-file! (io/file sibling "src/app/sibling.clj") secret-source)
    ;; 1. an ABSOLUTE target outside the workspace — the `/etc/passwd` shape,
    ;;    written to a file this test owns so the assertion can be about bytes.
    (link! (io/file ws "src/app/absolute.clj") (io/file outside "secret.clj"))
    ;; 2. a link into a SIBLING workspace: the neighbouring repository, which
    ;;    is the escape an operator is most likely to have by accident.
    (link! (io/file ws "src/app/sibling.clj")
           (io/file "../../../ws2/src/app/sibling.clj"))
    ;; 3. a CHAIN: the first hop lands inside the workspace and the second
    ;;    leaves it, so a containment test that stops at one hop passes.
    (link! (io/file ws "src/app/hop.clj") (io/file "../../../outside/secret.clj"))
    (link! (io/file ws "src/app/chain.clj") (io/file "hop.clj"))
    ;; 4. the control: a link that stays inside, which BOTH must follow.
    (link! (io/file ws "src/app/inside.clj") (io/file "../../lib/inside.clj"))
    {:ws ws
     :escapes [{:shape :absolute-target :path "src/app/absolute.clj"}
               {:shape :sibling-workspace :path "src/app/sibling.clj"}
               {:shape :link-chain :path "src/app/chain.clj"}]}))

;; @spec MCP-OP-CENSUS-018
(deftest no-census-reads-a-source-whose-real-path-leaves-the-workspace
  (let [parent (temp-dir)]
    (try
      (let [{:keys [ws escapes]} (escape-fixture! parent)
            named (.getCanonicalPath ^java.io.File ws)
            target-secret (.getCanonicalPath (io/file parent "outside/secret.clj"))]

        (testing "the WALK counts an escape and reads none of it, both entrances"
          (let [tool (census-tool/execute-request! {:project-root named} {})
                cli (core/run-relation-census {:dir named})]
            (is (true? (:ok tool)))
            (is (true? (:ok cli)))
            (is (<= 3 (:skipped_outside_root tool 0))
                (str "the tool's walk did not count all three escapes: "
                     (pr-str (select-keys tool [:skipped_outside_root]))))
            (is (<= 3 (:skipped-outside-root cli 0))
                (str "the CLI's walk did not count all three escapes: "
                     (pr-str (select-keys cli [:skipped-outside-root]))))
            (is (not (str/includes? (json/generate-string tool) escape-secret))
                "the tool's walk READ a file outside the workspace root")
            (is (not (str/includes? (pr-str cli) escape-secret))
                "the CLI's walk READ a file outside the tree it was pointed at")))

        (doseq [{:keys [shape path]} escapes]
          (let [absolute (str named "/" path)
                tool (census-tool/execute-request!
                       {:project-root named} {:files [path]})
                cli-in-workspace (refusal-or-throw
                                   #(core/run-relation-census
                                      {:dir named :file absolute}))
                cli-alone (refusal-or-throw
                            #(core/run-relation-census {:file absolute}))
                bb (bb-cli ":op" "relation-census" ":dir" named
                           ":file" absolute)]

            (testing (str shape ": the tool refuses the named escape")
              (is (false? (:ok tool))
                  (str shape ": the tool read a source whose real path leaves "
                       "its root: " (pr-str (:ok tool))))
              (is (= "outside-project" (:cause tool))
                  (str shape ": the tool refused as " (pr-str (:cause tool))))
              (is (not (str/includes? (json/generate-string tool)
                                      escape-secret))
                  (str shape ": the tool published the content it refused")))

            (doseq [[entrance result]
                    [[:cli-with-dir cli-in-workspace]
                     [:cli-file-alone cli-alone]
                     [:bb-launcher bb]]]
              (testing (str shape " " entrance ": the named escape is refused")
                (is (false? (:ok result))
                    (str shape " " entrance
                         ": read a source whose real path leaves the "
                         "workspace: " (pr-str (select-keys result [:ok]))))
                (is (= :file-outside-workspace (:error-type result))
                    (str shape " " entrance ": refused as "
                         (pr-str (:error-type result))))
                (is (= :outside-project (or (:cause result)
                                            (some-> (:cause result) keyword)))
                    (str shape " " entrance ": published cause "
                         (pr-str (:cause result))))
                (is (not (str/includes? (pr-str result) escape-secret))
                    (str shape " " entrance
                         ": published the content it refused to read"))
                (is (not (str/includes? (pr-str result) target-secret))
                    (str shape " " entrance
                         ": named the TARGET of the link, which is a fact "
                         "about the box and not about the request: "
                         (pr-str (select-keys result [:error :remedy]))))
                (is (str/includes? (pr-str result) path)
                    (str shape " " entrance
                         ": did not name the link the request spelled: "
                         (pr-str (select-keys result [:file :error]))))
                (is (string? (:remedy result))
                    (str shape " " entrance ": offers no remedy"))))

            (testing (str shape ": the tool names the link relative to the workspace")
              (is (= path (:file tool))
                  (str shape ": the tool named " (pr-str (:file tool))))
              (is (= [path] (:files_removed tool))
                  (str shape ": the tool narrowed to "
                       (pr-str (:files_removed tool))))
              ;; `workspace_root` is the request's own IDENTIFYING target and
              ;; carries the absolute root by contract — the same exception the
              ;; CLI's `:anchor` and every continuation carry, and the reason
              ;; the round-nineteen item-4 witness scopes its claim to PROSE.
              ;; Every other field is checked here.
              (is (not (str/includes?
                         (json/generate-string (dissoc tool :workspace_root))
                         named))
                  (str shape ": the tool published its absolute root outside "
                       "workspace_root: "
                       (pr-str (dissoc tool :workspace_root)))))))

        (testing "a link that stays INSIDE is followed by the walk and by :file"
          (let [inside (str named "/src/app/inside.clj")
                walk (core/run-relation-census {:dir named})
                tool-walk (census-tool/execute-request!
                            {:project-root named} {})
                cli (core/run-relation-census {:dir named :file inside})
                tool (census-tool/execute-request!
                       {:project-root named} {:files ["src/app/inside.clj"]})]
            (is (true? (:ok cli))
                (str "the CLI refused a link whose real path is inside the "
                     "workspace: " (pr-str (select-keys cli [:ok :error-type]))))
            (is (str/includes? (pr-str cli) "INSIDE-TARGET")
                "the CLI did not follow an inside link the request named")
            (is (true? (:ok tool))
                (str "the tool refused a link whose real path is inside its "
                     "root: " (pr-str (select-keys tool [:ok :error_type]))))
            (is (pos? (:duplicates-collapsed walk 0))
                (str "the CLI walk did not follow the inside link either — "
                     "a link that resolves inside is collapsed as a duplicate, "
                     "not skipped: "
                     (pr-str (select-keys walk [:duplicates-collapsed
                                                :skipped-outside-root]))))
            (is (pos? (:duplicates_collapsed tool-walk 0))
                (str "the tool walk did not follow the inside link: "
                     (pr-str (select-keys tool-walk
                                          [:duplicates_collapsed
                                           :skipped_outside_root]))))))

        (testing "a NAMED root is still resolved, which is the same rule (round fifteen)"
          (let [link (io/file parent "link-to-ws")]
            (Files/createSymbolicLink
              (.toPath link) (.toPath (io/file named))
              (make-array FileAttribute 0))
            (let [cli (core/run-relation-census {:dir (str link)})]
              (is (true? (:ok cli)))
              (is (pos? (:files-scanned cli 0))
                  (str "the walk visited a link instead of the tree it names: "
                       (pr-str (select-keys cli [:files-scanned]))))))))
      (finally
        (delete-tree! parent)))))

;; ---------------------------------------------------------------------------
;; ROUND NINETEEN, item 1 — Sol's round-eighteen BLOCKING finding.
;;
;; Round seventeen bounded the census op's exits and round eighteen bounded
;; both entrances' last steps, and the brief then declared the LAUNCHER's own
;; refusals out of scope because they "belong to no op". The reviewer's ruling,
;; and it stands: "'Belongs to no op' is not a valid bound exemption. It can
;; explain why these names do not belong in `cli-refusal-types`, but it cannot
;; exempt the public CLI entrance from the global CENSUS-014 promise that no
;; refusal field is unbounded."
;;
;; Reproduced at 3b7904a through the REAL launcher, not a fn call:
;;
;;   duplicate EXIT=1 BYTES=20228 MAX_A_RUN=10001 MARKERS=0 :duplicate-argument
;;   invalid   EXIT=1 BYTES=10064 MAX_A_RUN=10001 MARKERS=0 :invalid-arguments
;;
;; `parse-args` throws BEFORE dispatch, so neither `run-relation-census`'s last
;; step nor `run`'s shape exit ever sees the value; `-main`'s catch-all printed
;; `(merge (ex-data e) {:error (.getMessage e)})` verbatim.
;;
;; THE RULE: the bound is a property of the EXIT, not of the op. Every refusal
;; the CLI can print — op or launcher — leaves through one bounded exit, and it
;; is the same `census/bound-refusal` the op's exit uses, for the reason that
;; function's own docstring gives: a bound enforced at some of the sites is not
;; a bound, it is those sites' habit.
;;
;; And the launcher's names are DECLARED, in `census/launcher-refusal-types`,
;; so the enumeration that makes the bound total covers them: an unenumerated
;; refusal is one no witness can drive, which is how this one shipped.
;; ---------------------------------------------------------------------------

(def ^:private hostile-argument-length
  "The length the reviewer drove, so the receipt above is the drive below."
  10001)

(defn- hostile-argument
  []
  (apply str (repeat hostile-argument-length \a)))

(defn- raw-launcher
  "Run one REAL launcher as a subprocess and return its raw bytes.

   Raw, and not `edn/read-string`, because the finding is about the BYTES the
   launcher publishes: a reader that parses the map back has already thrown
   away the question of how big the thing on stdout was."
  [runtime args]
  (let [launcher (case runtime
                   :bb ["bb" "-cp" (str repo-root "/src")
                        "-m" "clj-surgeon.core"]
                   :jvm ["java" "-cp" (System/getProperty "java.class.path")
                         "clojure.main" "-m" "clj-surgeon.core"])
        {:keys [out err exit]}
        (apply proc/shell {:out :string :err :string :continue true}
               (concat launcher args))]
    {:out out :err err :exit exit
     :parsed (try (edn/read-string out) (catch Exception _ nil))}))

(defn- launcher-drives
  "One drive per name `census/launcher-refusal-types` declares.

   Both are raised by `parse-args`, before dispatch knows which op it is
   building — which is exactly why they escaped every bound the op grew."
  []
  (let [big (hostile-argument)]
    [{:label :duplicate-argument
      :error-type :duplicate-argument
      :args [":op" ":relation-census" ":doors" big ":doors" big]}
     {:label :invalid-arguments
      :error-type :invalid-arguments
      :args [":op" ":relation-census" ":doors" (str "[1" big "]")]}
     ;; Opus's round-nineteen item 1, blocking. Every drive above builds its
     ;; hostile argument as a STRING, and `core/parse-val` mints a KEYWORD
     ;; from any CLI value beginning with `:` and reads any value beginning
     ;; with `[`. So the caller controls a leaf the round-nineteen bound never
     ;; looked at, and the same declared name printed 20,287 bytes with a
     ;; 10,001-character run and no marker. Two more drives, same name, one
     ;; character of difference in what the caller typed.
     {:label :duplicate-argument-keyword
      :error-type :duplicate-argument
      :args [":op" ":relation-census"
             ":doors" (str ":" big) ":doors" (str ":" big)]}
     {:label :duplicate-argument-symbol
      :error-type :duplicate-argument
      :args [":op" ":relation-census"
             ":doors" (str "[" big "]") ":doors" (str "[" big "]")]}
     ;; Both dispatch refusals for an op nobody defines: `run-op`'s, which the
     ;; launcher reaches for an ordinary invocation, and `-main`'s, which it
     ;; reaches under `--help`. Two sites, one name, and only one of them was
     ;; bounded when this set was first written.
     {:label :unknown-operation
      :error-type :unknown-operation
      :args [":op" big]}
     {:label :unknown-operation-under-help
      :error-type :unknown-operation
      :args [":op" big ":help" "true"]}]))

(defn- printed-leaf-lengths
  "The RENDERED length of every leaf a parsed refusal carries.

   `pr-str`, and every leaf rather than every string, because Opus's
   round-nineteen item 1 is exactly the gap between those two measures: what
   the caller reads is printed output, and a 10,001-character keyword is
   10,002 characters on their terminal however `string?` answers about it. A
   bound that asks `string?` is a bound on the type the author happened to
   picture."
  [parsed]
  (->> (tree-seq coll? seq parsed)
       (remove coll?)
       (map #(count (pr-str %)))))

;; @spec MCP-OP-CENSUS-014
(deftest every-refusal-the-launcher-itself-prints-is-bounded-at-its-exit
  (let [drives (launcher-drives)
        marker-slack 64
        ceiling (+ census/max-refusal-field-chars marker-slack)]

    (testing "the drives cover every refusal the launcher declares it can print"
      (is (= census/launcher-refusal-types
             (set (map :error-type drives)))
          (str "declared: " (pr-str census/launcher-refusal-types)
               "; driven: " (pr-str (set (map :error-type drives))))))

    (doseq [runtime [:jvm :bb]
            {:keys [label error-type args]} drives]
      (let [{:keys [out exit parsed]} (raw-launcher runtime args)]
        (testing (str runtime " " label " refuses as the declared type")
          (is (= 1 exit)
              (str runtime " " label " exited " exit ": " (pr-str out)))
          (is (map? parsed)
              (str runtime " " label " printed no readable refusal: "
                   (pr-str (subs (str out) 0 (min 400 (count (str out)))))))
          (is (= error-type (:error-type parsed))
              (str runtime " " label " refused "
                   (pr-str (:error-type parsed)))))

        (testing (str runtime " " label " is bounded at the launcher's exit")
          (let [longest (reduce max 0 (printed-leaf-lengths parsed))]
            (is (<= longest ceiling)
                (str runtime " " label " published a " longest
                     "-character field, over the " ceiling
                     "-character ceiling — the launcher's refusal is "
                     "unbounded")))
          (is (str/includes? (str out) "[truncated:")
              (str runtime " " label
                   " truncated nothing and said nothing: the caller's own "
                   "10,001-character argument came back whole"))
          (is (not (re-find (re-pattern (str "a{" hostile-argument-length "}"))
                            (str out)))
              (str runtime " " label
                   " echoed the whole hostile argument back"))
          (is (< (alength (.getBytes (str out) "UTF-8")) 8192)
              (str runtime " " label " published "
                   (alength (.getBytes (str out) "UTF-8"))
                   " bytes")))))))

;; ---------------------------------------------------------------------------
;; ROUND TWENTY, item 1 — Opus's round-nineteen BLOCKING finding.
;;
;; Round nineteen gave the launcher ONE bounded exit and called the bound
;; total. It was not. `census/bound-refusal` postwalked STRINGS only, and
;; `core/parse-val` mints a KEYWORD out of any CLI value beginning with `:`
;; and READS any value beginning with `[` or `{`, so the caller controls a
;; non-string leaf that rides straight through the bound. Measured by the
;; reviewer at the real launchers, and reproduced at this branch's tip:
;;
;;   jvm-dup-keyword EXIT=1 BYTES=20287 MAX_A_RUN=10001 MARKERS=0 :duplicate-argument
;;   bb-dup-keyword  EXIT=1 BYTES=20226 MAX_A_RUN=10001 MARKERS=0 :duplicate-argument
;;   jvm-dup-symbol  EXIT=1 BYTES=20289 MAX_A_RUN=10001 MARKERS=0 :duplicate-argument
;;   jvm-kw-vector   EXIT=1 BYTES=11667 MAX_A_RUN=10001 MARKERS=1 :doors-not-a-string
;;   jvm-map         EXIT=1 BYTES=11672 MAX_A_RUN=10001 MARKERS=1 :doors-not-a-string
;;
;; The last two are the OP's own entrance exit, not the launcher's, so this is
;; one root cause reaching two exits: a bound applied to one type inside a
;; value is not a bound on the value.
;;
;; The round-nineteen witness was blind twice over — every drive built its
;; hostile argument as a string, and the assertion filtered the parsed tree
;; with `string?` before measuring — which is the round-eighteen lesson one
;; frame over: an enumeration that describes a subset of what an entrance
;; emits is green over the rest.
;;
;; THE RULE: THE BOUND IS OVER THE VALUE AS PRINTED, not over one type inside
;; it. A keyword, a symbol, a vector of them and a nested map are bounded
;; exactly as a string is, at BOTH real launchers and at both exits, and this
;; witness drives all four through both as subprocesses.
;; ---------------------------------------------------------------------------

(defn- printed-value-drives
  "One drive per non-string shape `core/parse-val` can mint from CLI text.

   Two exits, deliberately: `:duplicate-argument` is the LAUNCHER's own
   refusal, raised by `parse-args` before dispatch; `:doors-not-a-string` is
   the OP's entrance exit. One root cause reaches both, so one witness drives
   both."
  []
  (let [big (hostile-argument)]
    [{:label :keyword-at-the-launchers-exit
      :error-type :duplicate-argument
      :args [":op" ":relation-census"
             ":doors" (str ":" big) ":doors" (str ":" big)]}
     {:label :symbol-at-the-launchers-exit
      :error-type :duplicate-argument
      :args [":op" ":relation-census"
             ":doors" (str "[" big "]") ":doors" (str "[" big "]")]}
     {:label :keyword-vector-at-the-ops-exit
      :error-type :doors-not-a-string
      :args [":op" ":relation-census" ":dir" "." ":doors" (str "[:" big "]")]}
     {:label :nested-map-at-the-ops-exit
      :error-type :doors-not-a-string
      :args [":op" ":relation-census" ":dir" "." ":doors" (str "{:k :" big "}")]}]))

;; @spec MCP-OP-CENSUS-014
(deftest no-refusal-either-real-launcher-prints-carries-an-unbounded-printed-value
  (let [drives (printed-value-drives)
        marker-slack 64
        ;; AT the ceiling, never at a constant: the assertion moves when the
        ;; declared bound moves, which is what makes it a witness for the rule
        ;; rather than for today's number.
        ceiling (+ census/max-refusal-field-chars marker-slack)]

    (testing "every driven name is DECLARED at the exit it leaves through"
      (doseq [{:keys [label error-type]} drives]
        (is (contains? (into census/launcher-refusal-types
                             census/cli-refusal-types)
                       error-type)
            (str label " drives " (pr-str error-type)
                 ", which neither declared refusal set contains, so no "
                 "enumeration witness could see it"))))

    (doseq [runtime [:jvm :bb]
            {:keys [label error-type args]} drives]
      (let [{:keys [out exit parsed]} (raw-launcher runtime args)]
        (testing (str runtime " " label " refuses as the declared type")
          (is (= 1 exit)
              (str runtime " " label " exited " exit ": " (pr-str out)))
          (is (map? parsed)
              (str runtime " " label " printed no readable refusal: "
                   (pr-str (subs (str out) 0 (min 400 (count (str out)))))))
          (is (= error-type (:error-type parsed))
              (str runtime " " label " refused "
                   (pr-str (:error-type parsed)))))

        (testing (str runtime " " label " is bounded AS PRINTED")
          (let [longest (reduce max 0 (printed-leaf-lengths parsed))]
            (is (<= longest ceiling)
                (str runtime " " label " published a leaf that RENDERS as "
                     longest " characters, over the " ceiling
                     "-character ceiling — the bound is enforced on one type "
                     "inside the value rather than on the value")))
          (is (str/includes? (str out) "[truncated:")
              (str runtime " " label
                   " truncated nothing and said nothing: the caller's own "
                   "10,001-character argument came back whole"))
          (is (not (re-find (re-pattern (str "a{" hostile-argument-length "}"))
                            (str out)))
              (str runtime " " label
                   " echoed the whole hostile argument back"))
          (is (< (alength (.getBytes (str out) "UTF-8")) 8192)
              (str runtime " " label " published "
                   (alength (.getBytes (str out) "UTF-8")) " bytes")))))))

;; ---------------------------------------------------------------------------
;; ROUND NINETEEN, item 4 — Sol's round-eighteen item 4.
;;
;; Round seventeen's items 4 and 5 gave the workspace root ONE name and
;; repaired three sites. The round-eighteen brief then claimed "no absolute
;; root". The reviewer drove it and found the claim false of the CLI's remedy:
;;
;;   :directory "<workspace_root>"
;;   :error     "the directory <workspace_root> may not be read or traversed…"
;;   :remedy    "<workspace_root> came from the workspace walk … make
;;               <workspace_root> readable under /tmp/census18-fx/sol/denied,
;;               remove it, or name the sources to census with :file."
;;
;; Two names for one subject in one sentence, one of them the server's absolute
;; path. The repair had reached the three sites the previous finding named and
;; stopped there: a rule applied at the sites a reviewer happened to list is
;; that list's habit, not a rule.
;;
;; THE RULE: a refusal's PROSE never names the workspace root absolutely. The
;; root has one name, `census/workspace-root-token`, and prose uses it. The
;; absolute root survives only in the fields that must be REPLAYABLE or
;; IDENTIFYING — the CLI's `:anchor` and `:dir`, the tool's `workspace_root`,
;; and every continuation — which are enumerated below rather than discovered,
;; and which MCP-OP-CENSUS-014 has always required to carry it.
;;
;; A path the CALLER named is not the root and is not covered: a refusal about
;; `:file /ws/src/a/missing.clj` must echo the caller's own bytes, which is why
;; the assertion is about the root NAMED AS ITSELF — the root not followed by a
;; path separator — and not about any string that contains it.
;;
;; Two witnesses, because one of them cannot see the defect the other exists
;; for: the drive proves no refusal WE DRIVE names the root, and the source
;; scan proves no site RENDERS one, including the branches no fixture reaches.
;; ---------------------------------------------------------------------------

(def ^:private root-carrying-fields
  "The refusal fields that carry the workspace root ABSOLUTELY, by contract.

   ENUMERATED, so that adding a field which leaks the root is a deliberate edit
   to this set and not an accident nobody sees. `:anchor` and `:dir` name the
   workspace the caller named and are what a reader checks their request
   against; `workspace_root` is the tool's half of the same; every continuation
   is EXECUTABLE and a relative path in an argument position runs somewhere
   else."
  #{:anchor :dir :workspace_root :next-command :next-command-argv :next_call})

(defn- names-the-root-itself
  "Every prose string in `refusal` that names `root` AS ITSELF.

   The root followed by a separator is a path UNDER the root — the caller's own
   `:file`, a project-relative name made absolute — and naming one of those is
   what a refusal is for. The root standing alone is the defect: it is a fact
   about the box published where the token belongs."
  [refusal root]
  (let [pattern (re-pattern (str (java.util.regex.Pattern/quote (str root))
                                 "(?!/)"))]
    (->> (dissoc refusal :anchor :dir :workspace_root
                 :next-command :next-command-argv :next_call)
         (tree-seq coll? seq)
         (filter string?)
         (filter #(re-find pattern %))
         vec)))

;; @spec MCP-OP-CENSUS-014
;; @spec MCP-OP-CENSUS-018
(deftest no-refusal-names-the-workspace-root-in-its-prose
  (let [parent (temp-dir)
        trees (cli-refusal-fixture! parent)
        denied-root (io/file parent "denied-root")
        mcp-parent (temp-dir)
        arms (io/file mcp-parent "arms")
        bare (io/file mcp-parent "bare")
        broken (io/file mcp-parent "broken")]
    (try
      (spit-file! (io/file denied-root "src/a/one.clj") arm-source)
      (spit-file! (io/file arms "src/a/one.clj") arm-source)
      (spit-file! (io/file arms "src/b/two.clj") arm-source)
      (spit-file! (io/file arms "src/small.clj") "()")
      (.mkdirs (io/file bare "src"))
      (spit-file! (io/file broken "src/app/broken.clj") malformed-arm-source)

      (testing "the CLI enumeration: no declared refusal names its root"
        (doseq [{:keys [label root result]}
                (run-cli-drives (cli-refusal-drives trees))]
          (let [named (.getCanonicalPath ^java.io.File root)
                leaks (names-the-root-itself result named)]
            (is (= [] leaks)
                (str label " named its workspace root absolutely in prose: "
                     (pr-str leaks))))))

      (testing "the MCP enumeration: no declared refusal names its root"
        (doseq [{:keys [label drive]}
                (mcp-refusal-drives {:arms arms :bare bare :broken broken})]
          (let [result (drive)
                leaks (names-the-root-itself result (.getCanonicalPath arms))]
            (is (= [] leaks)
                (str label " named its workspace root absolutely in prose: "
                     (pr-str leaks))))))

      (testing "the reviewer's own drive: a root the walk may not enter"
        ;; Sol's round-eighteen item 4, exactly: `:dir <chmod 000 directory>`.
        ;; The subject of this refusal IS the root, which is the shape that has
        ;; no workspace-relative name and therefore the shape that reaches for
        ;; the absolute one.
        (let [named (.getCanonicalPath denied-root)]
          (deny-traversal! denied-root)
          (try
            (let [cli (refusal-or-throw
                        #(core/run-relation-census {:dir named}))
                  tool (census-tool/execute-request! {:project-root named} {})]
              (doseq [[entrance result] [[:cli cli] [:tool tool]]]
                (is (false? (:ok result))
                    (str entrance " accepted a root it may not enter"))
                (is (= [] (names-the-root-itself result named))
                    (str entrance " named its workspace root absolutely: "
                         (pr-str (names-the-root-itself result named))))
                (is (str/includes? (pr-str (:remedy result))
                                   census/workspace-root-token)
                    (str entrance " remedy does not use the root's one name: "
                         (pr-str (:remedy result))))
                ;; Routing the absolute root out exposed the sentence
                ;; underneath it: a remedy telling the caller to make the root
                ;; readable UNDER the root is not followable, and the tool has
                ;; been saying its own version of that since round seventeen.
                (is (not (re-find #"<workspace_root>[^.]*under <workspace_root>"
                                  (str (:remedy result))))
                    (str entrance " tells the caller to make a directory "
                         "readable under itself: "
                         (pr-str (:remedy result))))))
            (finally (allow-traversal! denied-root)))))

      (testing "no arm-bearing tree is refused with its root in the prose"
        ;; `no-fold-arms-found` is the shape whose remedy names what it
        ;; SCANNED, and it named it absolutely.
        (let [named (.getCanonicalPath ^java.io.File (:empty-ws trees))
              cli (refusal-or-throw #(core/run-relation-census {:dir named}))]
          (is (= :no-fold-arms-found (:error-type cli)))
          (is (= [] (names-the-root-itself cli named))
              (str "the empty-tree remedy named its root absolutely: "
                   (pr-str (names-the-root-itself cli named))))))
      (finally
        (allow-traversal! denied-root)
        (delete-tree! parent)
        (delete-tree! mcp-parent)))))

;; @spec MCP-OP-CENSUS-018
(deftest no-refusal-SITE-renders-a-raw-workspace-root-into-prose
  ;; The ratchet. The drive above can only see the branches a fixture reaches;
  ;; this one reads the sources and refuses the SHAPE — a root binding adjacent
  ;; to a prose string literal — so a remedy added next round with the old
  ;; wording fails here even if nothing drives it. A literal with no whitespace
  ;; is a path JOIN (`(str root "/" name)`), which is how both entrances build
  ;; the absolute paths their anchors and continuations are required to carry;
  ;; a literal with whitespace is a SENTENCE.
  (let [renders
        (for [source ["src/clj_surgeon/core.clj"
                      "src/clj_surgeon/mcp_relation_census.clj"
                      "src/clj_surgeon/mcp_paths.clj"]
              :let [text (slurp (io/file repo-root source))]
              ;; `(?<![\w-])root` and not `\broot`: `census-root`,
              ;; `real-root` and `workspace-root` are NAMES, and a `defn`
              ;; followed by its docstring is not a render.
              pattern [#"(?:\(census-root dir\)|(?<![\w-])root)\s+\"[^\"]*\s[^\"]*\""
                       #"\"[^\"]*\s[^\"]*\"\s+(?:\(census-root dir\)|(?<![\w-])root)\b"]
              match (re-seq pattern text)]
          [source match])]
    (is (= [] (vec renders))
        (str "these sites render a raw workspace root into a prose string; "
             "the root has one name, `census/workspace-root-token`: "
             (pr-str (vec renders))))))
