(ns clj-surgeon.mcp-telemetry-coverage-test
  "Server-side telemetry coverage: every public MCP tool call is recorded once,
   at the dispatch boundary, under the name the caller actually invoked.

   Earned 2026-09-05 (the Astra program). An hour with an attested
   `alias_migration` call reported ZERO tool calls, because `alias_migration`
   emitted no telemetry at all and the compact-edit path emitted its events
   under the internal name `apply_clojure_changes`. A telemetry surface that
   each tool opts into is a surface every new tool silently opts out of."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.mcp-runtime :as runtime]
   [clj-surgeon.mcp-server :as mcp-server]
   [clj-surgeon.mcp-telemetry :as telemetry]
   [clj-surgeon.mcp-tool :as mcp-tool]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]])
  (:import
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute)
   (java.util UUID)))

(def public-tool-names
  "The public MCP catalog, written as literals so this witness cannot be
   satisfied by a registry that quietly shrank. Independence rule: never derive
   the expectation from the code under test."
  #{"inspect_clojure"
    "apply_clojure_changes"
    "edit_clojure"
    "transform_clojure"
    "relation_census"
    "alias_migration"
    "admit_clojure_patch"
    "feature_thread"})

(def ^:private probe-arguments
  "One minimal request per public tool. A typed refusal is the expected outcome:
   this witness measures whether the SERVER recorded the call, never whether the
   call succeeded."
  {"inspect_clojure" {"requests" []}
   "apply_clojure_changes" {"changes" []}
   "edit_clojure" {"edits" []}
   "transform_clojure" {"programs" []}
   "relation_census" {}
   "alias_migration" {}
   "admit_clojure_patch" {}
   "feature_thread" {}})

(defn- temp-dir
  [prefix]
  (.toFile (Files/createTempDirectory prefix (make-array FileAttribute 0))))

(defn- delete-tree!
  [file]
  (when (.exists (io/file file))
    (doseq [child (reverse (file-seq (io/file file)))]
      (Files/deleteIfExists (.toPath child)))))

(defn- events
  [state]
  (mapv #(json/parse-string % true)
        (remove str/blank? (str/split-lines (slurp (:file state))))))

(defn- drive-every-public-tool!
  "Drive each public tool once through the real dispatch boundary and return the
   telemetry events the server wrote. Throws from a tool are caught HERE, not
   swallowed by the boundary: a call that throws is still one call."
  [state]
  (doseq [tool (mcp-server/public-tool-registry)]
    (let [dispatch (mcp-server/dispatch-tool-fn tool)]
      (try
        (dispatch nil (get probe-arguments (:name tool) {})
                  (fn [_content _error? _structured] nil))
        (catch Throwable _ nil))))
  (events state))

;; @spec MCP-OP-TELCOV-001
;; @spec MCP-OP-TELCOV-002
;; @spec MCP-OP-TELCOV-003
(deftest every-public-tool-call-is-recorded-once-under-its-public-name
  (let [workspace (temp-dir "clj-surgeon-telcov-workspace-")
        telemetry-dir (temp-dir "clj-surgeon-telcov-events-")
        previous-config @runtime/tool-config]
    (try
      (.mkdirs (io/file workspace "src"))
      (spit (io/file workspace "src" "probe.clj")
            "(ns probe)\n\n(defn answer [] 42)\n")
      (let [state (telemetry/start!
                    {:mode :metrics
                     :directory (.getPath telemetry-dir)
                     :session-id (str "telcov-" (UUID/randomUUID))})]
        (mcp-tool/init! {:project-root (.getPath workspace)
                         :receipt-dir (.getPath (io/file workspace "receipts"))
                         :tool-profile :full
                         :telemetry state})
        (let [recorded (filter #(= "tool.dispatch" (:event %))
                               (drive-every-public-tool! state))
              by-tool (group-by :tool recorded)]

          ;; The catalog-vs-telemetry set equality. A tool added later without
          ;; an event fails HERE, in a count, with the missing name.
          (is (= public-tool-names (set (keys by-tool)))
              (str "telemetry names differ from the public catalog; missing "
                   (pr-str (sort (remove (set (keys by-tool))
                                         public-tool-names)))
                   ", unexpected "
                   (pr-str (sort (remove public-tool-names
                                         (keys by-tool))))))

          ;; Exactly one event per call, including refused ones.
          (is (= (count public-tool-names) (count recorded)))
          (doseq [tool-name (sort public-tool-names)]
            (is (= 1 (count (get by-tool tool-name)))
                (str tool-name " must record exactly one dispatch event")))

          ;; Request identity is per call, never shared.
          (is (= (count recorded)
                 (count (distinct (map :request_id recorded)))))

          (doseq [event recorded]
            (is (string? (:session_id event)))
            (is (string? (:request_id event)))
            (is (contains? #{"ok" "refused" "error"} (:outcome event)))
            (is (integer? (:started_ns event)))
            (is (integer? (:finished_ns event)))
            (is (<= (:started_ns event) (:finished_ns event)))
            (is (number? (:wall_ms event)))
            (is (integer? (:bytes_in event)))
            (is (integer? (:bytes_out event)))
            (when (= "refused" (:outcome event))
              (is (string? (:refusal_kind event))
                  (str (:tool event)
                       " refused without naming a typed refusal kind"))))))
      (finally
        ;; The registry is process-wide: leaving a deleted fixture installed
        ;; would let a later test recreate it inside the run's isolated temp
        ;; root and trip the temp-leak ratchet.
        (telemetry/install! nil)
        (mcp-tool/init! previous-config)
        (delete-tree! workspace)
        (delete-tree! telemetry-dir)))))

;; @spec MCP-OP-TELCOV-002
(deftest compact-edit-is-recorded-under-edit-clojure-not-apply
  ;; Both public entrances share one handler. Before 2026-09-05 every
  ;; edit_clojure transaction appeared in the record as apply_clojure_changes,
  ;; so the compact-edit path was uncountable and the apply path was inflated.
  (let [workspace (temp-dir "clj-surgeon-telcov-edit-workspace-")
        telemetry-dir (temp-dir "clj-surgeon-telcov-edit-events-")
        previous-config @runtime/tool-config]
    (try
      (.mkdirs (io/file workspace "src"))
      (spit (io/file workspace "src" "probe.clj")
            "(ns probe)\n\n(defn answer [] 42)\n")
      (let [state (telemetry/start!
                    {:mode :metrics
                     :directory (.getPath telemetry-dir)
                     :session-id (str "telcov-edit-" (UUID/randomUUID))})]
        (mcp-tool/init! {:project-root (.getPath workspace)
                         :receipt-dir (.getPath (io/file workspace "receipts"))
                         :tool-profile :full
                         :telemetry state})
        (let [edit-tool (first (filter #(= "edit_clojure" (:name %))
                                       (mcp-server/public-tool-registry)))]
          ((mcp-server/dispatch-tool-fn edit-tool)
           nil
           {"edits" [{"file" "src/probe.clj" "from" "42" "to" "43"}]}
           (fn [_ _ _] nil)))
        (let [recorded (events state)
              names (set (map :tool (remove #(= "server.start" (:event %))
                                            recorded)))]
          (is (contains? names "edit_clojure"))
          (is (not (contains? names "apply_clojure_changes"))
              (str "an edit_clojure call was recorded under an internal name: "
                   (pr-str (sort names))))))
      (finally
        ;; The registry is process-wide: leaving a deleted fixture installed
        ;; would let a later test recreate it inside the run's isolated temp
        ;; root and trip the temp-leak ratchet.
        (telemetry/install! nil)
        (mcp-tool/init! previous-config)
        (delete-tree! workspace)
        (delete-tree! telemetry-dir)))))

;; @spec MCP-OP-TELCOV-004
(deftest an-unstructured-public-tool-refuses-instead-of-bypassing-telemetry
  ;; clojure-mcp's own factory builds a non-structured tool, which never passes
  ;; dispatch-tool-fn. That hole is invisible to any witness that drives the
  ;; boundary, so it must be impossible to register rather than merely untested.
  (let [refusal (try
                  (@#'mcp-server/create-async-tool
                   {:name "silent_tool"
                    :description "d"
                    :schema {}
                    :tool-fn (fn [_ _ callback] (callback [] false {:ok true}))})
                  nil
                  (catch clojure.lang.ExceptionInfo error error))]
    (is (some? refusal) "an unstructured public tool must not build")
    (is (= :unstructured-public-tool (:error-type (ex-data refusal))))
    (is (= "silent_tool" (:tool (ex-data refusal))))
    (is (str/includes? (:remedy (ex-data refusal)) "dispatch-tool-fn"))))
