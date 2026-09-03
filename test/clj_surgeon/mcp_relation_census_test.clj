(ns clj-surgeon.mcp-relation-census-test
  "Wire-level witnesses for the relation_census MCP tool.

   The census subject is real bytes: `test-fixtures/relation-census/folds.clj`
   carries the verbatim task-chase, agenda-selections, upsert-by and conj-once
   arms from curtaincall-cfp-lens at commit
   963875358a37c48ab6175ea1bea22633e4fd0306."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-relation-census :as census-tool]
   [clj-surgeon.relation-census :as census]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]])
  (:import
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute)))

(def repo-root (.getCanonicalPath (io/file ".")))
(def fixture "test-fixtures/relation-census/folds.clj")
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
      (is (= #{:discover :parse :classify :merge}
             (set (keys (:phases_elapsed_ms result)))))
      (is (every? number? (vals (:phases_elapsed_ms result))))
      (is (every? #(not (neg? %)) (vals (:phases_elapsed_ms result))))
      (is (str/starts-with? (:next_action result) "review the raw sites")))

    (testing "the published receipt is bounded and carries no file text"
      (let [bytes (count (.getBytes (json/generate-string result) "UTF-8"))]
        (is (<= bytes 4096) (str "receipt was " bytes " bytes")))
      (is (not (str/includes? (json/generate-string result) "defmethod fold-event"))))))

;; @spec MCP-OP-CENSUS-010
;; @spec MCP-OP-CENSUS-011
(deftest pool-size-one-and-pool-size-n-agree-byte-for-byte
  (let [strip #(-> % (dissoc :elapsed_ms :phases_elapsed_ms :pool_size))
        serial (run {:files [fixture helpers] :pool_size 1})
        parallel (run {:files [fixture helpers] :pool_size 8})
        reversed (run {:files [helpers fixture] :pool_size 8})]
    (is (= 1 (:pool_size serial)))
    (is (= 8 (:pool_size parallel)))
    (is (= (json/generate-string (strip serial))
           (json/generate-string (strip parallel)))
        "parallelism changes elapsed time, never the answer")
    (is (= (json/generate-string (strip serial))
           (json/generate-string (strip reversed)))
        "the merge re-keys by path, so request order cannot reorder the census")
    (is (= 1 (:files serial)) "only files that define arms are censused")))

;; @spec MCP-OP-CENSUS-014
(deftest refuses-with-a-typed-reason-and-an-executable-next-call
  (testing "a workspace root that is not an existing absolute directory"
    (let [result (run {:workspace_root "relative/nope"})]
      (is (false? (:ok result)))
      (is (= "invalid-workspace-root" (:error_type result)))))

  (testing "no file defines fold arms, and the scan is named"
    (let [result (run {:files [helpers]})]
      (is (false? (:ok result)))
      (is (= "no-fold-arms-found" (:error_type result)))
      (is (= [helpers] (:scanned result)))
      (is (= 1 (:files_scanned result)))
      (is (nil? (:counts result)))
      (is (= "relation_census" (get-in result [:next_call :tool])))
      (is (= [helpers] (get-in result [:next_call :files])))))

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

  (testing "the effective pool is capped at the box and the receipt says so"
    (let [result (run {:files [fixture] :pool_size 64})
          effective (min 64 (.availableProcessors (Runtime/getRuntime)))]
      (is (true? (:ok result)))
      (is (= effective (:pool_size result)))
      (is (<= (:pool_size result) (.availableProcessors (Runtime/getRuntime))))
      (if (< effective 64)
        (is (= 64 (:pool_size_requested result)))
        (is (nil? (:pool_size_requested result)))))))
