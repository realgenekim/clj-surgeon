(ns clj-surgeon.mcp-relation-census-test
  "Wire-level witnesses for the relation_census MCP tool.

   The census subject is real bytes: `test-fixtures/relation-census/folds.clj`
   carries the verbatim task-chase, agenda-selections, upsert-by and conj-once
   arms from curtaincall-cfp-lens at commit
   963875358a37c48ab6175ea1bea22633e4fd0306."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.core :as core]
   [clj-surgeon.mcp-paths :as mcp-paths]
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

    (testing "the published receipt is bounded and carries no file text"
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
          (is (= "relation_census" (get-in result [:next_call :tool]))))
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
        (is (= "relation_census" (get-in result [:next_call :tool]))))))

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
