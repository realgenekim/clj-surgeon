(ns clj-surgeon.mcp-relation-census-test
  "Wire-level witnesses for the relation_census MCP tool.

   The census subject is real bytes: `test-fixtures/relation-census/folds.clj`
   carries the verbatim task-chase, agenda-selections, upsert-by and conj-once
   arms from curtaincall-cfp-lens at commit
   963875358a37c48ab6175ea1bea22633e4fd0306."
  (:require
   [babashka.process :as proc]
   [cheshire.core :as json]
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

(def ^:private shape-rule-probes
  "For each row of the shared table: a request violating THAT row and no row
   before it, spelled for each entrance that can express it."
  {[:unknown-fields :present]
   {:mcp {:nope 1} :cli {:format :edn}}

   [:dir :type]
   {:cli {:dir ""}}

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
   specific name in `reason`; the door-vocabulary row is refused after
   discovery, so its name is the `error_type` itself."
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
  "Run a CLI continuation verbatim, from `cwd`."
  [cwd next-command]
  (apply bb-cli-in cwd (rest (str/split (str next-command) #"\s+"))))

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
        big "src/app/huge.clj"
        small "src/app/small.clj"]
    (try
      (spit-file! (io/file root big)
                  (str "(defmethod fold-event \"a\" [state event] state)\n"
                       (apply str (repeat (inc census/max-source-bytes) \;))))
      (spit-file! (io/file root small) arm-source)
      (.mkdirs (io/file empty-root "src"))
      (let [here (fn [params]
                   (census-tool/execute-request!
                     {:project-root (.getPath root)} params))
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
             [run {:workspace_root (.getCanonicalPath root) :doors [1]}]}
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
                              ":dir" (.getPath empty-root) ":threads" "0")}]

        (testing "no MCP refusal serialises a placeholder"
          (doseq [[label result] mcp-refusals]
            (let [wire (json/generate-string result)]
              (is (false? (:ok result)) (str label " did not refuse"))
              (is (not (str/includes? wire "<"))
                  (str label " carries a placeholder: " wire)))))

        (testing "no CLI refusal, on either runtime, serialises a placeholder"
          (doseq [[label result] cli-refusals]
            (let [wire (pr-str result)]
              (is (false? (:ok result)) (str label " did not refuse: " wire))
              (is (not (str/includes? wire "<"))
                  (str label " carries a placeholder: " wire)))))

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
              (is (str/includes? (str (:remedy result))
                                 (.getCanonicalPath empty-root))
                  (str label " remedy does not name the directory it scanned"))
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
      (finally (delete-tree! root) (delete-tree! empty-root)))))

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
    (doseq [value ["" "   " [1] {:a 1} 7]]
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
                       (or (not= :mcp entrance)
                           (= :shape (:mcp-phase rule :shape)))
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
