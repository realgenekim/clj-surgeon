(ns clj-surgeon.mcp-alias-migration-test
  (:require
   [cheshire.core :as json]
   [clj-surgeon.alias-migration-fixture :as fixture]
   [clj-surgeon.file-ops :as file-ops]
   [clj-surgeon.intent-transaction :as transaction]
   [clj-surgeon.mcp-alias-migration :as alias-migration]
   [clj-surgeon.mcp-tool :as mcp-tool]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.java.shell :as shell]
   [clojure.set]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [rewrite-clj.node :as n]
   [rewrite-clj.parser :as parser])
  (:import
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute)))

(def corpus (fixture/corpus))

(defn- temp-dir
  []
  (.toFile (Files/createTempDirectory "clj-surgeon-alias-migration"
                                      (make-array FileAttribute 0))))

(defn- delete-tree!
  [file]
  (when (.exists (io/file file))
    (doseq [child (reverse (file-seq (io/file file)))]
      (Files/deleteIfExists (.toPath child)))))

(defn- live-qualifiers
  "Namespace qualifiers of every token that is live code, excluding #_ discards."
  [source]
  (let [walk (fn walk [node]
               (cond
                 (= :uneval (n/tag node)) []
                 (= :token (n/tag node))
                 (let [value (try (n/sexpr node) (catch Exception _ nil))]
                   (if (and (symbol? value) (namespace value)) [(namespace value)] []))
                 (n/inner? node) (mapcat walk (n/children node))
                 :else []))]
    (set (walk (parser/parse-string-all source)))))

(defn- write-tree!
  [workspace tree]
  (doseq [[relative source] tree]
    (let [target (io/file workspace relative)]
      (.mkdirs (.getParentFile target))
      (spit target source))))

(defn- workspace!
  []
  (let [workspace (temp-dir)]
    (write-tree! workspace (:pre corpus))
    (spit (io/file workspace "fan_test.clj") (fixture/behaviour-suite))
    workspace))

(defn- requiring-source
  "One minimal namespace that requires from.lib and calls from.var once."
  [namespace-name]
  (str "(ns " namespace-name "\n  (:require\n   ["
       fixture/from-lib " :as store]))\n\n"
       "(defn one [id] (store/" fixture/from-var " id))\n"))

(defn- request
  ([workspace] (request workspace {}))
  ([workspace overrides]
   (merge {:op "alias_migration"
           :workspace_root (.getPath workspace)
           :from {:lib fixture/from-lib :var fixture/from-var}
           :to {:lib fixture/to-lib :var fixture/to-var
                :alias_policy fixture/alias-policy}
           :scope {:paths ["src/**"]}
           :expect {:files 12}}
          overrides)))

(defn- config
  [workspace receipt-dir]
  {:project-root (.getPath workspace)
   :receipt-dir (.getPath receipt-dir)})

(defn- execute!
  ([workspace] (execute! workspace {}))
  ([workspace overrides]
   (let [receipt-dir (io/file workspace "receipts")]
     (.mkdirs receipt-dir)
     (alias-migration/execute! (config workspace receipt-dir)
                               (request workspace overrides)))))

(defn- owned-detail!
  "One detail document written by the production writer, stamped `stamp`."
  [workspace stamp]
  (let [relative (alias-migration/write-details! (.toPath workspace) {:files []})
        file (io/file workspace relative)]
    (.setLastModified file stamp)
    file))

(defn- babashka-available?
  []
  (try
    (zero? (:exit (shell/sh "bb" "--version")))
    (catch Exception _ false)))

(defn- run-babashka
  [workspace & args]
  (apply shell/sh (concat ["bb"] args [:dir (.getPath workspace)])))

;; ---------------------------------------------------------------------------
;; the committed path

;; @spec MCP-OP-ALIAS-001
;; @spec MCP-OP-ALIAS-016
;; @spec MCP-OP-ALIAS-019
;; @spec MCP-OP-ALIAS-020
(deftest one-call-migrates-the-whole-fan-out-and-returns-one-constant-receipt
  (let [workspace (workspace!)]
    (try
      (let [captured (atom nil)
            _ (mcp-tool/init! (config workspace (io/file workspace "receipts")))
            _ (mcp-tool/handle-alias-migration
                nil
                (json/parse-string (json/generate-string (request workspace)) true)
                (fn [content error? structured]
                  (reset! captured {:content content :error? error?
                                    :result structured})))
            {:keys [content error? result]} @captured]
        (testing "the receipt is one committed O(1) object"
          (is (false? error?) (pr-str result))
          (is (:ok result) (pr-str result))
          (is (= "alias_migration" (:operation result)))
          (is (true? (:committed result)))
          (is (= 12 (:files result)))
          (is (= 36 (:sites result)))
          (is (= 3 (:refer_sites result))
              "t06 refers the migrated var by name")
          (is (nil? (:lib_renamed result))
              "a var migration never moves the defining namespace")
          (is (= {"store2" 10 "st2" 1 "es" 1} (:alias_histogram result)))
          (is (= 3 (:collisions_resolved result))
              "t05 collides on one ns alias, t10 on two; locals never collide")
          (is (= {:status "not-requested"} (:kondo_delta result))
              "verification is opt-in, exactly as it is for the other write tools")
          (is (= {:status "not-requested"} (:focused_test result)))
;; @spec MCP-OP-ALIAS-034
          (is (= 0 (:string_mentions result))
              "the synthetic corpus names the old lib in no string literal")
          (is (string? (:details_path result)))
          (is (number? (:elapsed_ms result))))

        (testing "the receipt is constant in N: it carries no per-file list"
          (let [encoded (json/generate-string result)]
            (doseq [file (get-in corpus [:manifest :targets])]
              (is (not (str/includes? encoded file))
                  (str "the receipt names " file)))
            (is (not (str/includes? encoded "\"edits\":[")))
            (is (< (count encoded) 1200)
                "the whole receipt stays well under a kilobyte at N=12")))

        (testing "the visible summary is also constant in N"
          (is (= 1 (count content)))
          (is (str/includes? (first content) "12 files · 36 sites")))

        (testing "every changed file equals the canonical post-migration source"
          (doseq [[relative expected] (:post corpus)]
            (is (= expected (slurp (io/file workspace relative))) relative)))

        (testing "per-file detail lives behind details_path, not in the receipt"
          (let [details (edn/read-string
                          (slurp (io/file workspace (:details_path result))))]
            (is (= 12 (count (:files details))))
            (is (= #{:file :alias :collided :sites :refer-sites :require-mode}
                   (set (keys (first (:files details))))))))

        (testing "the transaction published an inverse receipt that restores the tree"
          (let [undo (transaction/execute-undo! {:receipt (:undo_receipt result)})]
            (is (:ok undo) (pr-str undo))
            (doseq [[relative expected] (:pre corpus)]
              (is (= expected (slurp (io/file workspace relative))) relative)))))
      (finally
        (mcp-tool/init! nil)
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-005
;; @spec MCP-OP-ALIAS-010
(deftest the-migrated-tree-loads-and-its-behaviour-suite-is-green
  (if-not (babashka-available?)
    (is true "babashka is unavailable; the load and behaviour gates are skipped")
    (let [workspace (workspace!)]
      (try
        (let [before (run-babashka workspace "--classpath" "src" "-e"
                                   "(require 'acid.fanout.t01) :ok")]
          (is (zero? (:exit before)) (:err before)))
        (let [result (execute! workspace)]
          (is (:ok result) (pr-str result))
          (testing "acceptance predicate 4: every namespace loads"
            (let [loaded (run-babashka
                           workspace "--classpath" "src" "-e"
                           (str "(do "
                                (str/join " "
                                          (map #(str "(require '" % ")")
                                               (map :ns fixture/file-specs)))
                                " :ok)"))]
              (is (zero? (:exit loaded)) (:err loaded))))
          (testing "acceptance predicate 5: the behaviour suite is green at base count"
            (let [suite (run-babashka workspace "--classpath" "src:."
                                      "-e"
                                      (str "(load-file \"fan_test.clj\")"
                                           " (acid.fanout.fan-test/-main)"))]
              (is (zero? (:exit suite)) (:err suite))
              (let [summary (edn/read-string (str/trim (:out suite)))]
                (is (= 21 (:base-count summary)))
                (is (= [] (:failures summary)))))))
        (finally
          (delete-tree! workspace))))))

;; ---------------------------------------------------------------------------
;; atomicity

;; @spec MCP-OP-ALIAS-017
;; @spec MCP-OP-ALIAS-018
(deftest a-failure-in-the-eleventh-file-leaves-the-first-ten-unchanged
  (let [workspace (workspace!)
        receipt-dir (io/file workspace "receipts")]
    (.mkdirs receipt-dir)
    (try
      (let [{:keys [ok plan root paths] :as planned}
            (alias-migration/plan!
              (.getPath workspace)
              (:request (alias-migration/validate-request (request workspace))))
            _ (is ok (pr-str planned))
            ordered (mapv :file (:files plan))
            eleventh (nth ordered 10)
            first-ten (subvec ordered 0 10)
            _ (is (= 12 (count ordered)))
            ;; the eleventh file's ns form drifts after the frozen read, so
            ;; the exact bytes that change compiled against are gone
            _ (spit (io/file workspace eleventh)
                    (str/replace (slurp (io/file workspace eleventh))
                                 "[acid.fanout.store :as store]"
                                 "[acid.fanout.store :as store]\n   [clojure.set :as set]"))
            drifted (slurp (io/file workspace eleventh))
            _ (is (not= (get (:pre corpus) eleventh) drifted)
                  "the drift injection actually changed the eleventh file")
            commit (alias-migration/commit!
                     (config workspace receipt-dir)
                     (.toString root)
                     (alias-migration/plan->spec plan paths)
                     (mapv #(get paths %) ordered))]
        (testing "the transaction refuses at compile time, before any write"
          (is (:error commit) (pr-str commit))
          (is (= :expect-count-mismatch (:error-type commit)) (pr-str commit))
          (is (true? (:source-unchanged commit)) (pr-str commit)))
        (testing "not one of the first ten files was written"
          (doseq [file first-ten]
            (is (= (get (:pre corpus) file) (slurp (io/file workspace file)))
                file)))
        (testing "the drifted file keeps exactly the bytes it drifted to"
          (is (= drifted (slurp (io/file workspace eleventh)))))
        (testing "the twelfth file is untouched too"
          (let [twelfth (nth ordered 11)]
            (is (= (get (:pre corpus) twelfth)
                   (slurp (io/file workspace twelfth))))))
        (testing "the public refusal is typed and fail-closed"
          (let [refusal (alias-migration/commit-refusal plan commit false)]
            (is (false? (:ok refusal)))
            (is (= "alias_migration" (:operation refusal)))
            (is (true? (:source_unchanged refusal)))
            (is (false? (:write_authority refusal))))))
      (finally
        (delete-tree! workspace)))))

;; ---------------------------------------------------------------------------
;; typed refusals through the public entrance

;; @spec MCP-OP-ALIAS-012
;; @spec MCP-OP-ALIAS-015
;; @spec MCP-OP-ALIAS-040
(deftest expect-mismatch-refuses-closed-with-an-executable-next-call
  (let [workspace (workspace!)]
    (try
      (let [captured (atom nil)
            _ (mcp-tool/init! (config workspace (io/file workspace "receipts")))
            _ (mcp-tool/handle-alias-migration
                nil
                (json/parse-string
                  (json/generate-string (request workspace {:expect {:files 80}}))
                  true)
                (fn [content error? structured]
                  (reset! captured {:content content :error? error?
                                    :result structured})))
            {:keys [content error? result]} @captured]
        (is (true? error?))
        (is (false? (:ok result)))
        (is (= "alias-migration-expect-mismatch" (:error_type result)))
        (is (= 12 (:found_files result)))
        (is (= 80 (:expected_files result)))
        (is (true? (:source_unchanged result)))
        (is (false? (:write_authority result)))
        (is (number? (:elapsed_ms result)))
        (is (str/includes? (first content) "source unchanged"))
        (testing "no byte was written by the refused call"
          (doseq [[relative expected] (:pre corpus)]
            (is (= expected (slurp (io/file workspace relative))) relative)))
        (testing "the next_call is a complete executable alias_migration request"
          (let [next-call (:next_call result)]
            (is (= "alias_migration" (get next-call "op")))
            (is (= 12 (get-in next-call ["expect" "files"]))
                "the next_call declares exactly what discovery found")
            (is (= (:found_files result) (get-in next-call ["expect" "files"]))
                "an over-declared expectation self-corrects in one return")
            (is (= (.getPath workspace) (get next-call "workspace_root")))
            (let [replayed (atom nil)]
              ;; sent back verbatim over the same MCP entrance
              (mcp-tool/handle-alias-migration
                nil
                (json/parse-string (json/generate-string next-call) true)
                (fn [_content _error? structured] (reset! replayed structured)))
              (is (:ok @replayed) (pr-str @replayed))
              (is (= 12 (:files @replayed)))
              (doseq [[relative expected] (:post corpus)]
                (is (= expected (slurp (io/file workspace relative))) relative))))))
      (finally
        (mcp-tool/init! nil)
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-006
(deftest an-empty-scope-refuses-closed-and-states-the-scanned-count
  (let [workspace (workspace!)]
    (try
      (let [result (execute! workspace {:scope {:paths ["src/acid/fanout/n0*.clj"]}
                                        :expect {:files 0}})]
        (is (false? (:ok result)))
        (is (= "alias-migration-empty-scope" (:error_type result)))
        (is (= 0 (:found_files result)))
        (is (= 3 (:scanned_files result)))
        (is (true? (:source_unchanged result))))
      (finally
        (delete-tree! workspace)))))


;; @spec MCP-OP-ALIAS-057
(deftest a-bare-directory-in-scope-paths-is-that-directorys-subtree
  ;; The exact scope EVERY tool arm of the E3-P cohort sent on its FIRST call
  ;; (/home/forge/tmp/arms/e3/e3-P-T-1/rollout.jsonl, 2026-09-03T22:42:20.546Z):
  ;;   scope: { paths: ["src"] }
  ;; It selected nothing, because scope.paths are globs and the glob `src`
  ;; matches only a path whose whole spelling is `src`. Four refusals in four of
  ;; four arms, one model return each, on a rung whose pass line is one call.
  ;; A scope path that IS an existing directory is that directory's subtree:
  ;; the obvious reading, and the only one under which the caller's spelling is
  ;; not a trap.
  (let [workspace (workspace!)]
    (try
      (testing "a directory selects exactly what its explicit glob selects"
        (is (= (alias-migration/expand-scope (.toPath (.getCanonicalFile workspace))
                                            {:paths ["src/**"] :exclude []})
               (alias-migration/expand-scope (.toPath (.getCanonicalFile workspace))
                                             {:paths ["src"] :exclude []}))))
      (testing "the cohort's exact first request commits in ONE call"
        (let [result (execute! workspace {:scope {:paths ["src"]}
                                          :expect {:files 12}})]
          (is (true? (:ok result)) (pr-str result))
          (is (true? (:committed result)))
          (is (= 12 (:files result)))))
      (finally
        (delete-tree! workspace)))))


;; @spec MCP-OP-ALIAS-058
(deftest a-scope-that-matches-no-file-names-the-spelling-not-the-domain
  ;; `alias-migration-empty-scope` announced a DOMAIN cause — "No namespace
  ;; under scope requires acid.fanout.store" — for a SPELLING cause, and a
  ;; refusal that cannot tell "your glob matched nothing" from "nothing in this
  ;; tree requires that lib" teaches the wrong lesson. The two states are now
  ;; two refusals: this one fires only when the scope matched no file at all.
  (let [workspace (workspace!)
        receipt-dir (io/file workspace "receipts")]
    (.mkdirs receipt-dir)
    (try
      (let [result (execute! workspace {:scope {:paths ["srk/**"]}
                                        :expect {:files 12}})]
        (is (false? (:ok result)))
        (is (= "alias-migration-scope-matches-nothing" (:error_type result))
            "a glob that matched nothing was reported as a fact about the tree")
        (is (= ["srk/**"] (:paths result))
            "the refusal does not name the paths as the caller gave them")
        (is (= 0 (:files_matched result)))
        ;; the proxy "the word `requires` is absent" is too crude: the refusal
        ;; must not ASSERT the domain cause, and saying that the domain fact is
        ;; NOT KNOWN is the honest thing to publish, not a violation
        (is (not (str/includes? (str (:error result))
                                (str "No namespace under scope requires "
                                     fixture/from-lib)))
            "a spelling cause was announced as the domain cause")
        (is (str/includes? (str (:error result)) "matched 0 files")
            "the refusal does not state what the caller's own paths matched")
        (is (str/includes? (str (:error result)) "is not known")
            "the refusal leaves the unread domain fact looking settled")
        (is (string? (:remedy result)))
        (is (true? (:source_unchanged result)))
        (testing "the corrected spelling is executable, not merely described"
          (let [next-call (:next_call result)]
            (is (some? next-call) "the refusal carried no executable next_call")
            (is (contains? (set (get-in next-call ["scope" "paths"])) "src/**")
                "the remedy does not name the tree's own source root")
            (let [replayed (alias-migration/execute!
                             (config workspace receipt-dir)
                             (json/parse-string (json/generate-string next-call)
                                                true))]
              (is (not= "alias-migration-scope-matches-nothing"
                        (:error_type replayed))
                  "the corrected spelling still matched nothing")
              (is (or (:ok replayed)
                      (= "alias-migration-expect-mismatch" (:error_type replayed)))
                  (pr-str replayed))
              (when-not (:ok replayed)
                (is (pos? (long (:found_files replayed))))
                (let [committed (alias-migration/execute!
                                  (config workspace receipt-dir)
                                  (json/parse-string
                                    (json/generate-string (:next_call replayed))
                                    true))]
                  (is (:ok committed) (pr-str committed))))))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-006
(deftest the-domain-refusal-fires-only-when-the-scope-matched-files
  ;; The counterpart of ALIAS-058: three files matched, none of them requires
  ;; from.lib. That IS a fact about the tree, and it keeps its own name.
  (let [workspace (workspace!)]
    (try
      (let [result (execute! workspace {:scope {:paths ["src/acid/fanout/n0*.clj"]}
                                        :expect {:files 0}})]
        (is (false? (:ok result)))
        (is (= "alias-migration-empty-scope" (:error_type result)))
        (is (= 3 (:scanned_files result))
            "the domain refusal fired over a scope that matched no file"))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-013
(deftest an-indirect-reference-refuses-closed-and-names-the-file
  (let [workspace (workspace!)]
    (try
      (spit (io/file workspace "src/acid/fanout/t13.clj")
            (str "(ns acid.fanout.t13\n  (:require\n"
                 "   [acid.fanout.store :as store]))\n\n"
                 "(defn boot [] (require 'acid.fanout.store))\n\n"
                 "(defn one [id] (store/find-event id))\n"))
      (let [result (execute! workspace {:expect {:files 13}})]
        (is (false? (:ok result)))
        (is (= "alias-migration-indirect-reference" (:error_type result)))
        (is (= "src/acid/fanout/t13.clj" (:file result)))
        (is (= ["src/acid/fanout/t13.clj"]
               (get-in result [:next_call "scope" "exclude"])))
        (is (= (get (:pre corpus) "src/acid/fanout/t01.clj")
               (slurp (io/file workspace "src/acid/fanout/t01.clj")))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-014
(deftest ambiguous-ownership-refuses-closed-and-names-both-candidate-vars
  (let [workspace (workspace!)]
    (try
      (spit (io/file workspace "src/acid/fanout/t13.clj")
            (str "(ns acid.fanout.t13\n  (:require\n"
                 "   [acid.fanout.store :refer [find-event]]\n"
                 "   [acid.fanout.mirror :refer [find-event]]))\n\n"
                 "(defn one [id] (find-event id))\n"))
      (let [result (execute! workspace {:expect {:files 13}})]
        (is (false? (:ok result)))
        (is (= "alias-migration-ambiguous-ownership" (:error_type result)))
        (is (= ["acid.fanout.store/find-event" "acid.fanout.mirror/find-event"]
               (:candidates result))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-008
(deftest an-exhausted-alias-policy-refuses-closed-and-names-the-collisions
  (let [workspace (workspace!)]
    (try
      (let [result (execute! workspace
                             {:to {:lib fixture/to-lib :var fixture/to-var
                                   :alias_policy ["store2"]}})]
        (is (false? (:ok result)))
        (is (= "alias-migration-alias-policy-exhausted" (:error_type result)))
        (is (= ["store2"] (:collided_bindings result)))
        (is (= ["store2" "store2-2"] (get-in result [:next_call "to" "alias_policy"]))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-002
(deftest the-request-is-closed-and-refuses-unknown-or-malformed-fields
  (let [workspace (workspace!)]
    (try
      (testing "an unknown field"
        (let [result (execute! workspace {:files ["src/acid/fanout/t01.clj"]})]
          (is (false? (:ok result)))
          (is (= "invalid-mcp-request" (:error_type result)))
          (is (= ["files"] (:path result)))))
      (testing "a missing alias policy"
        (let [result (execute! workspace {:to {:lib fixture/to-lib
                                               :var fixture/to-var}})]
          (is (false? (:ok result)))
          (is (= ["to" "alias_policy"] (:path result)))))
      (testing "a non-integer expectation"
        (let [result (execute! workspace {:expect {:files "twelve"}})]
          (is (false? (:ok result)))
          (is (= ["expect" "files"] (:path result)))))
      (finally
        (delete-tree! workspace)))))

;; ---------------------------------------------------------------------------
;; verification participates in the transaction's rollback authority

;; @spec MCP-OP-ALIAS-019
(deftest a-configured-profile-reports-a-kondo-delta-and-a-focused-test-result
  (let [workspace (workspace!)
        receipt-dir (io/file workspace "receipts")]
    (.mkdirs receipt-dir)
    (try
      (let [result (alias-migration/execute!
                     (assoc (config workspace receipt-dir)
                            :verification-profiles {"fast" {:commands []}})
                     (request workspace {:verify "fast"}))]
        (is (:ok result) (pr-str result))
        (is (= {:status "not-configured"} (:kondo_delta result))
            "an empty command list runs no diagnostics")
        (is (= "pass" (get-in result [:focused_test :status])))
        (is (= "fast" (get-in result [:focused_test :profile]))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-017
(deftest a-failing-profile-rolls-the-whole-migration-back
  (let [workspace (workspace!)
        receipt-dir (io/file workspace "receipts")]
    (.mkdirs receipt-dir)
    (try
      (let [result (alias-migration/execute!
                     (assoc (config workspace receipt-dir)
                            :verification-profiles
                            {"fast" {:commands [["false"]]}})
                     (request workspace {:verify "fast"}))]
        (is (false? (:ok result)) (pr-str result))
        (is (true? (:source_unchanged result)))
        (doseq [[relative expected] (:pre corpus)]
          (is (= expected (slurp (io/file workspace relative))) relative)))
      (finally
        (delete-tree! workspace)))))

;; ---------------------------------------------------------------------------
;; scope expansion stays inside the workspace

;; @spec MCP-OP-ALIAS-004
(deftest scope-expansion-is-confined-and-skips-build-output
  (let [workspace (workspace!)]
    (try
      (.mkdirs (io/file workspace "target" "classes" "acid" "fanout"))
      (spit (io/file workspace "target/classes/acid/fanout/t99.clj")
            (str "(ns acid.fanout.t99\n  (:require\n"
                 "   [acid.fanout.store :as store]))\n\n"
                 "(defn one [id] (store/find-event id))\n"))
      (let [relatives (alias-migration/expand-scope
                        (.toPath (.getCanonicalFile workspace))
                        {:paths ["**/*.clj"] :exclude []})]
        (is (not-any? #(str/starts-with? % "target/") relatives))
        (is (every? #(or (str/starts-with? % "src/") (str/starts-with? % "test/"))
                    relatives))
        (is (contains? (set relatives) "src/acid/fanout/t01.clj"))
        (is (contains? (set relatives) "test/acid/fanout/store_test.clj")))
      (finally
        (delete-tree! workspace)))))

(defn- symlink!
  [link target]
  (Files/createSymbolicLink (.toPath (io/file link)) (.toPath (io/file target))
                            (make-array FileAttribute 0)))

;; @spec MCP-OP-ALIAS-037
(deftest a-symlink-cycle-inside-the-root-terminates-scope-expansion
  (let [workspace (workspace!)
        link (io/file workspace "src/acid/fanout/loop")]
    (try
      (symlink! link (io/file workspace "src"))
      (let [expanded (deref (future
                              (alias-migration/expand-scope
                                (.toPath (.getCanonicalFile workspace))
                                {:paths ["**/*.clj"] :exclude []}))
                            60000 ::timed-out)]
        (is (not= ::timed-out expanded)
            "scope expansion did not terminate over a symlink cycle")
        (is (not-any? #(str/includes? % "/loop/") expanded)
            "the walk descended a symlinked directory")
        (testing "the cycle contributes nothing to the scope"
          (let [_ (Files/deleteIfExists (.toPath link))
                without-link (alias-migration/expand-scope
                               (.toPath (.getCanonicalFile workspace))
                               {:paths ["**/*.clj"] :exclude []})]
            (is (= without-link expanded)))))
      (finally
        (Files/deleteIfExists (.toPath link))
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-037
(deftest a-symlinked-directory-out-of-the-root-is-never-entered
  (let [workspace (workspace!)
        outside (temp-dir)
        link (io/file workspace "src/vendor")]
    (try
      (spit (io/file outside "escape.clj")
            "(ns escape\n  (:require\n   [acid.fanout.store :as store]))\n")
      (symlink! link outside)
      (let [expanded (alias-migration/expand-scope
                       (.toPath (.getCanonicalFile workspace))
                       {:paths ["**/*.clj"] :exclude []})]
        (is (not-any? #(str/starts-with? % "src/vendor/") expanded)
            "the walk entered a directory symlinked out of the project root")
        (is (not-any? #(str/ends-with? % "escape.clj") expanded)
            "a file outside the project root reached the scope"))
      (finally
        (Files/deleteIfExists (.toPath link))
        (delete-tree! workspace)
        (delete-tree! outside)))))

(defn- bare-workspace!
  "One empty workspace holding only the files a test writes into it."
  []
  (temp-dir))

(defn- cap-request
  [workspace overrides]
  (merge {:op "alias_migration"
          :workspace_root (.getPath workspace)
          :from {:lib fixture/from-lib :var fixture/from-var}
          :to {:lib fixture/to-lib :var fixture/to-var
               :alias_policy fixture/alias-policy}
          :scope {:paths ["src/**"]}
          :expect {:files 1}}
         overrides))

;; @spec MCP-OP-ALIAS-038
(deftest a-scope-above-the-file-ceiling-refuses-before-any-source-is-read
  (let [workspace (bare-workspace!)
        receipt-dir (io/file workspace "receipts")]
    (.mkdirs receipt-dir)
    (try
      (let [directory (io/file workspace "src" "wide")]
        (.mkdirs directory)
        (dotimes [index 3000]
          (spit (io/file directory (str "f" index ".clj"))
                (str "(ns wide.f" index ")\n"))))
      (let [result (alias-migration/execute! (config workspace receipt-dir)
                                             (cap-request workspace {}))]
        (is (false? (:ok result)) (pr-str result))
        (is (= "alias-migration-scope-too-large" (:error_type result)))
        (is (= 3000 (:scanned_files result)))
        (is (= alias-migration/max-scope-files (:max_files result)))
        (is (= 1 (:expected_files result))
            "the refusal names expect.files so the caller can narrow scope")
        (is (true? (:source_unchanged result)))
        (is (false? (:mutation_attempted result))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-039
;; @spec MCP-OP-ALIAS-051
(deftest a-source-above-the-byte-ceiling-refuses-before-it-is-slurped
  (let [workspace (workspace!)
        receipt-dir (io/file workspace "receipts")
        oversized (io/file workspace "src/acid/fanout/huge.clj")]
    (.mkdirs receipt-dir)
    (try
      ;; the oversized file does require from.lib — but nothing READ it, so the
      ;; tool cannot know that, and a count corrected on a guess is worse than
      ;; one left alone. expect.files stands; the declaration that is now one
      ;; too high is corrected by the mismatch refusal's own next_call, one
      ;; return later. That return is the honest price of not guessing.
      (spit oversized
            (str (requiring-source "acid.fanout.huge")
                 ";; "
                 (String. (char-array (inc alias-migration/max-source-bytes)
                                      \x))
                 "\n"))
      (is (> (.length oversized) alias-migration/max-source-bytes))
      (let [result (alias-migration/execute!
                     (config workspace receipt-dir)
                     (request workspace {:expect {:files 13}}))]
        (is (false? (:ok result)) (pr-str result))
        (is (= "alias-migration-source-too-large" (:error_type result)))
        (is (= "src/acid/fanout/huge.clj" (:path result)))
        (is (= alias-migration/max-source-bytes (:max_bytes result)))
        (is (true? (:source_unchanged result)))
        (testing "no byte was written by the refused call"
          (doseq [[relative expected] (:pre corpus)]
            (is (= expected (slurp (io/file workspace relative))) relative)))
        (testing "the refusal carries an executable next_call that excludes it"
          (let [next-call (:next_call result)]
            (is (= "alias_migration" (get next-call "op")))
            (is (= ["src/acid/fanout/huge.clj"]
                   (get-in next-call ["scope" "exclude"])))
            (is (= 13 (get-in next-call ["expect" "files"]))
                "expect.files was decremented for a file nobody read")
            (is (string? (:expect_files_unchanged_reason result))
                "the refusal does not say why expect.files is unchanged")
            (testing "and the replay converges in one more return"
              (let [replayed (alias-migration/execute!
                               (config workspace receipt-dir)
                               (json/parse-string (json/generate-string next-call)
                                                  true))]
                (is (= "alias-migration-expect-mismatch" (:error_type replayed))
                    (pr-str replayed))
                (is (= 12 (:found_files replayed)))
                (let [corrected (:next_call replayed)]
                  (is (= ["src/acid/fanout/huge.clj"]
                         (get-in corrected ["scope" "exclude"]))
                      "the mismatch refusal dropped the exclusion it was handed")
                  (is (= 12 (get-in corrected ["expect" "files"])))
                  (let [committed (alias-migration/execute!
                                    (config workspace receipt-dir)
                                    (json/parse-string
                                      (json/generate-string corrected) true))]
                    (is (:ok committed) (pr-str committed))
                    (is (= 12 (:files committed)))
                    (doseq [[relative expected] (:post corpus)]
                      (is (= expected (slurp (io/file workspace relative)))
                          relative)))))))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-051
(deftest one-file-symlinked-out-of-the-root-refuses-with-a-replayable-next-call
  (let [workspace (workspace!)
        outside (temp-dir)
        receipt-dir (io/file workspace "receipts")
        escape (io/file workspace "src/acid/fanout/escape.clj")]
    (.mkdirs receipt-dir)
    (try
      (spit (io/file outside "escape.clj") (requiring-source "acid.fanout.escape"))
      (symlink! escape (io/file outside "escape.clj"))
      (let [result (alias-migration/execute!
                     (config workspace receipt-dir)
                     (request workspace {:expect {:files 13}}))]
        (is (false? (:ok result)) (pr-str result))
        (is (= "alias-migration-scope-path-refused" (:error_type result)))
        (is (= "src/acid/fanout/escape.clj" (:path result)))
        (is (true? (:source_unchanged result)))
        (testing "one refused file no longer refuses the whole scope with no remedy"
          (let [next-call (:next_call result)]
            (is (some? next-call) "the refusal carried no executable next_call")
            (is (= ["src/acid/fanout/escape.clj"]
                   (get-in next-call ["scope" "exclude"])))
            ;; the escaping file was refused before it was opened, so whether
            ;; it required from.lib is not knowable and expect.files stands
            (is (= 13 (get-in next-call ["expect" "files"])))
            (is (string? (:expect_files_unchanged_reason result)))
            (let [replayed (alias-migration/execute!
                             (config workspace receipt-dir)
                             (json/parse-string (json/generate-string next-call)
                                                true))]
              (is (= "alias-migration-expect-mismatch" (:error_type replayed))
                  (pr-str replayed))
              (is (= 12 (:found_files replayed)))
              (let [corrected (:next_call replayed)]
                (is (= ["src/acid/fanout/escape.clj"]
                       (get-in corrected ["scope" "exclude"])))
                (let [committed (alias-migration/execute!
                                  (config workspace receipt-dir)
                                  (json/parse-string
                                    (json/generate-string corrected) true))]
                  (is (:ok committed) (pr-str committed))
                  (is (= 12 (:files committed)))
                  (doseq [[relative expected] (:post corpus)]
                    (is (= expected (slurp (io/file workspace relative)))
                        relative))))))))
      (finally
        (Files/deleteIfExists (.toPath escape))
        (delete-tree! workspace)
        (delete-tree! outside)))))

(defn- deep-relative-path
  "One project-relative path of exactly `segments` segments ending in a source."
  [segments]
  (str/join "/" (concat ["src"]
                        (map #(str "d" %) (range 1 (dec segments)))
                        ["deep.clj"])))

;; @spec MCP-OP-ALIAS-048
(deftest a-path-deeper-than-the-bound-refuses-instead-of-vanishing-from-scope
  (let [workspace (bare-workspace!)
        receipt-dir (io/file workspace "receipts")
        deep (deep-relative-path 65)
        shallow "src/shallow.clj"
        requiring requiring-source]
    (.mkdirs receipt-dir)
    (try
      (is (= 65 (count (str/split deep #"/"))))
      (write-tree! workspace {deep (requiring "deep.one")
                              shallow (requiring "shallow.one")})
      (let [result (alias-migration/execute!
                     (config workspace receipt-dir)
                     (cap-request workspace {:expect {:files 2}}))]
        (is (false? (:ok result)) (pr-str result))
        (is (= "alias-migration-scope-too-deep" (:error_type result))
            "a file past the depth bound was truncated out of scope instead")
        (is (= deep (:path result)))
        (is (= 65 (:depth result)))
        (is (= alias-migration/max-scope-depth (:max_depth result)))
        (is (true? (:source_unchanged result)))
        (is (false? (:mutation_attempted result))))
      (testing "the truncation the bound used to launder is impossible"
        ;; the over-declare idiom re-sends found_files; had the deep file been
        ;; dropped, the caller would have committed a migration that left
        ;; deep.one requiring the retired lib
        (is (= (slurp (io/file workspace deep)) (requiring "deep.one"))))
      (finally
        (delete-tree! workspace)))))

(defn- bulk-non-source-files!
  "`count` empty non-source files in one directory, created without content."
  [^java.io.File directory count]
  (.mkdirs directory)
  (dotimes [index count]
    (.createNewFile (io/file directory (str "n" index ".txt")))))

;; @spec MCP-OP-ALIAS-050
(deftest a-walk-above-the-entry-ceiling-refuses-before-the-filtered-set-is-built
  (let [workspace (bare-workspace!)
        receipt-dir (io/file workspace "receipts")
        entries 60000]
    (.mkdirs receipt-dir)
    (try
      (bulk-non-source-files! (io/file workspace "src" "bulk") entries)
      (write-tree! workspace {"src/open.clj" (requiring-source "open.one")})
      (testing "the file ceiling cannot see this: the filtered set is one file"
        (is (> entries alias-migration/max-walk-entries))
        (is (< 1 alias-migration/max-scope-files)))
      (let [result (alias-migration/execute! (config workspace receipt-dir)
                                             (cap-request workspace {}))]
        (is (false? (:ok result)) (pr-str result))
        (is (= "alias-migration-walk-too-large" (:error_type result))
            "the raw walk was unbounded; only the filtered set was bounded")
        (is (= (inc alias-migration/max-walk-entries) (:visited_entries result))
            "the walk stops on the first entry past the ceiling")
        (is (= alias-migration/max-walk-entries (:max_entries result)))
        (is (true? (:source_unchanged result)))
        (is (false? (:mutation_attempted result))))
      (finally
        (delete-tree! workspace)))))

(defn- chmod!
  [mode ^java.io.File target]
  (shell/sh "chmod" mode (.getPath target)))

;; @spec MCP-OP-ALIAS-049
(deftest an-unreadable-subtree-refuses-instead-of-silently-shrinking-the-scope
  (let [workspace (bare-workspace!)
        receipt-dir (io/file workspace "receipts")
        locked (io/file workspace "src" "locked")]
    (.mkdirs receipt-dir)
    (try
      (write-tree! workspace {"src/open.clj" (requiring-source "open.one")
                              "src/locked/hidden.clj" (requiring-source "locked.one")})
      (chmod! "000" locked)
      (if (seq (.listFiles locked))
        ;; a privileged process cannot be denied, so it cannot witness denial
        (is true "this process reads a 000 directory; denial is unobservable here")
        (let [result (alias-migration/execute!
                       (config workspace receipt-dir)
                       (cap-request workspace {:expect {:files 2}}))]
          (is (false? (:ok result)) (pr-str result))
          (is (= "alias-migration-scope-unreadable" (:error_type result))
              "an unreadable subtree was dropped from scope in silence")
          (is (= ["src/locked"] (:unreadable_paths result)))
          (is (= 1 (:unreadable_count result)))
          (is (true? (:source_unchanged result)))
          (is (false? (:mutation_attempted result)))))
      (finally
        (chmod! "755" locked)
        (delete-tree! workspace)))))

(defn- sparse-source!
  "One .clj file that REPORTS `bytes` in size without occupying them.

  `RandomAccessFile.setLength` extends the file with a hole: the directory entry
  records the full size while no data block is allocated. A bound that is
  checked from recorded sizes before the first slurp is therefore witnessed at
  full scale on a 512 MiB heap, which is exactly the property under test."
  [^java.io.File target bytes]
  (.mkdirs (.getParentFile target))
  (with-open [handle (java.io.RandomAccessFile. target "rw")]
    (.setLength handle (long bytes)))
  target)

;; @spec MCP-OP-ALIAS-046
(deftest an-aggregate-scope-above-the-byte-ceiling-refuses-before-any-source-is-read
  (let [workspace (bare-workspace!)
        receipt-dir (io/file workspace "receipts")
        per-file 1900000
        files 450]
    (.mkdirs receipt-dir)
    (try
      (dotimes [index files]
        (sparse-source! (io/file workspace "src" "wide" (str "f" index ".clj"))
                        per-file))
      (testing "every file is under BOTH existing ceilings"
        (is (< files alias-migration/max-scope-files))
        (is (< per-file alias-migration/max-source-bytes))
        (is (> (* (long files) per-file) alias-migration/max-scope-bytes)
            "the product of two legal ceilings exceeds any heap the server has"))
      (let [result (alias-migration/execute! (config workspace receipt-dir)
                                             (cap-request workspace {}))]
        (is (false? (:ok result)) (pr-str result))
        (is (= "alias-migration-scope-too-large-bytes" (:error_type result)))
        (is (= (* (long files) per-file) (:scope_bytes result)))
        (is (= alias-migration/max-scope-bytes (:max_bytes result)))
        (is (= files (:scanned_files result)))
        (is (true? (:source_unchanged result)))
        (is (false? (:mutation_attempted result))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-047
(deftest heap-exhaustion-is-published-as-a-typed-refusal-not-an-untyped-throw
  (let [workspace (workspace!)
        receipt-dir (io/file workspace "receipts")]
    (.mkdirs receipt-dir)
    (try
      (testing "the direct dispatch"
        (with-redefs [alias-migration/plan!
                      (fn [& _] (throw (OutOfMemoryError. "Java heap space")))]
          (let [result (alias-migration/execute! (config workspace receipt-dir)
                                                 (request workspace))]
            (is (false? (:ok result)) (pr-str result))
            (is (= "alias-migration-resource-exhausted" (:error_type result)))
            (is (true? (:source_unchanged result))
                "the heap was exhausted before the transaction began")
            (is (false? (:mutation_attempted result))))))
      (testing "and the MCP tool entrance, which has no try of its own"
        (mcp-tool/init! (config workspace receipt-dir))
        (let [captured (atom nil)]
          (with-redefs [alias-migration/plan!
                        (fn [& _] (throw (OutOfMemoryError. "Java heap space")))]
            (mcp-tool/handle-alias-migration
              nil
              (json/parse-string (json/generate-string (request workspace)) true)
              (fn [content error? structured]
                (reset! captured {:content content :error? error?
                                  :result structured}))))
          (is (some? @captured) "the handler threw instead of publishing")
          (is (true? (:error? @captured)))
          (is (= "alias-migration-resource-exhausted"
                 (:error_type (:result @captured))))))
      (finally
        (mcp-tool/init! nil)
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-047
(deftest the-heap-guard-marks-the-transaction-kernel-not-the-call-that-precedes-it
  ;; `commit!` resolves the retire source, refuses an unknown profile, and
  ;; captures a verification baseline BEFORE the kernel is entered. A marker
  ;; set at the call to `commit!` therefore claims a mutation was attempted for
  ;; heap exhaustion in any of those, and tells the caller its tree may have
  ;; been written when nothing was.
  (let [workspace (workspace!)
        receipt-dir (io/file workspace "receipts")]
    (.mkdirs receipt-dir)
    (try
      (testing "an OutOfMemoryError entering commit!, before any write"
        (with-redefs [alias-migration/commit!
                      (fn [& _] (throw (OutOfMemoryError. "Java heap space")))]
          (let [result (alias-migration/execute! (config workspace receipt-dir)
                                                 (request workspace))]
            (is (false? (:ok result)) (pr-str result))
            (is (= "alias-migration-resource-exhausted" (:error_type result)))
            (is (true? (:source_unchanged result))
                "the heap was exhausted before the kernel wrote a byte")
            (is (false? (:mutation_attempted result)))
            (is (= "correct_request" (:next_action result)))
            (testing "and the tree agrees with the refusal"
              (doseq [[relative expected] (:pre corpus)]
                (is (= expected (slurp (io/file workspace relative))) relative))))))
      (testing "an OutOfMemoryError after the kernel wrote still says so"
        (with-redefs [alias-migration/write-details!
                      (fn [& _] (throw (OutOfMemoryError. "Java heap space")))]
          (let [result (alias-migration/execute! (config workspace receipt-dir)
                                                 (request workspace))]
            (is (false? (:ok result)) (pr-str result))
            (is (= "alias-migration-resource-exhausted" (:error_type result)))
            (is (false? (:source_unchanged result)))
            (is (true? (:mutation_attempted result)))
            (is (= "review_receipt" (:next_action result)))
            (testing "and the tree agrees with that refusal too"
              (doseq [[relative expected] (:post corpus)]
                (is (= expected (slurp (io/file workspace relative)))
                    relative))))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-050
(deftest the-walk-stops-at-the-ceiling-when-the-entries-past-it-are-unreadable
  ;; Every visitor callback but one already terminates over-bound.
  ;; `visitFileFailed` counted the entry and continued, so a tree whose entries
  ;; are unreadable walked straight through the ceiling that exists to stop it.
  (let [outside (temp-dir)
        receipt-dir (io/file outside "receipts")
        workspace (bare-workspace!)
        source (io/file workspace "src")
        locked (mapv (fn [index]
                       (let [directory (io/file source (str "locked" index))]
                         (.mkdirs directory)
                         (spit (io/file directory "hidden.clj")
                               (requiring-source (str "locked" index ".one")))
                         directory))
                     (range 30))]
    (.mkdirs receipt-dir)
    (try
      (doseq [directory locked] (chmod! "000" directory))
      (if (some #(seq (.listFiles ^java.io.File %)) locked)
        ;; a privileged process cannot be denied, so it cannot witness denial
        (is true "this process reads a 000 directory; denial is unobservable here")
        (with-redefs [alias-migration/max-walk-entries 5]
          ;; the walk sees the root, then src, then thirty unreadable entries:
          ;; the entry that crosses the ceiling is a read failure by
          ;; construction, which is the callback under test
          (let [result (alias-migration/execute!
                         {:project-root (.getPath workspace)
                          :receipt-dir (.getPath receipt-dir)}
                         (cap-request workspace {}))]
            (is (false? (:ok result)) (pr-str result))
            (is (= "alias-migration-walk-too-large" (:error_type result)))
            (is (= 6 (:visited_entries result))
                "the walk counted unreadable entries past the ceiling instead
                 of stopping on the first one")
            (is (= 5 (:max_entries result))))))
      (finally
        (doseq [directory locked] (chmod! "755" directory))
        (delete-tree! workspace)
        (delete-tree! outside)))))

;; @spec MCP-OP-ALIAS-055
(deftest the-file-ceiling-refusal-narrows-to-the-largest-fitting-subtree
  ;; A constant-size next_call DOES exist for the aggregate ceilings: it is not
  ;; a list of exclusions but one narrowing prefix, derived from the walk's own
  ;; per-directory aggregates.
  (let [workspace (workspace!)
        receipt-dir (io/file workspace "receipts")
        wide (io/file workspace "src" "wide")]
    (.mkdirs receipt-dir)
    (try
      (.mkdirs wide)
      (dotimes [index 2500]
        (spit (io/file wide (str "f" index ".clj")) (str "(ns wide.f" index ")\n")))
      (let [result (alias-migration/execute! (config workspace receipt-dir)
                                             (request workspace))]
        (is (false? (:ok result)) (pr-str result))
        (is (= "alias-migration-scope-too-large" (:error_type result)))
        (let [next-call (:next_call result)
              rendered (json/generate-string next-call)]
          (is (some? next-call)
              "the aggregate ceiling refused with no executable remedy")
          (when next-call
            (is (<= (count rendered) 512)
                (str "next_call is " (count rendered) " characters"))
            (is (= ["src/acid/fanout/**"] (get-in next-call ["scope" "paths"]))
                "the narrowing prefix is not the largest subtree that fits")
            (is (<= (:would_select_files result) alias-migration/max-scope-files))
            (is (pos? (:would_select_files result)))
            (testing "and the replay commits"
              (let [replayed (alias-migration/execute!
                               (config workspace receipt-dir)
                               (json/parse-string rendered true))]
                (is (:ok replayed) (pr-str replayed))
                (is (= 12 (:files replayed)))
                (doseq [[relative expected] (:post corpus)]
                  (is (= expected (slurp (io/file workspace relative)))
                      relative)))))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-055
(deftest the-byte-ceiling-refusal-narrows-to-the-largest-fitting-subtree
  (let [workspace (workspace!)
        receipt-dir (io/file workspace "receipts")
        per-file 1900000
        files 450]
    (.mkdirs receipt-dir)
    (try
      (dotimes [index files]
        (sparse-source! (io/file workspace "src" "wide" (str "f" index ".clj"))
                        per-file))
      (let [result (alias-migration/execute! (config workspace receipt-dir)
                                             (request workspace))]
        (is (false? (:ok result)) (pr-str result))
        (is (= "alias-migration-scope-too-large-bytes" (:error_type result)))
        (let [next-call (:next_call result)
              rendered (json/generate-string next-call)]
          (is (some? next-call)
              "the aggregate byte ceiling refused with no executable remedy")
          (when next-call
            (is (<= (count rendered) 512)
                (str "next_call is " (count rendered) " characters"))
            (is (= ["src/acid/fanout/**"] (get-in next-call ["scope" "paths"])))
            (is (<= (:would_select_bytes result) alias-migration/max-scope-bytes))
            (is (pos? (:would_select_bytes result)))
            (testing "and the replay commits"
              (let [replayed (alias-migration/execute!
                               (config workspace receipt-dir)
                               (json/parse-string rendered true))]
                (is (:ok replayed) (pr-str replayed))
                (is (= 12 (:files replayed))))))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-051
(deftest an-unread-exclusion-leaves-expect-files-alone-and-says-why
  ;; The two filesystem-boundary refusals name a file the tool never opened, so
  ;; whether it required from.lib is not knowable. Decrementing expect.files for
  ;; it asserts knowledge the tool does not have, and two such exclusions walk
  ;; the count away from the truth.
  (let [workspace (workspace!)
        receipt-dir (io/file workspace "receipts")
        filler (String. (char-array (inc alias-migration/max-source-bytes) \x))]
    (.mkdirs receipt-dir)
    (try
      (doseq [name ["huge1" "huge2"]]
        (spit (io/file workspace "src" "acid" "fanout" (str name ".clj"))
              (str "(ns acid.fanout." name ")\n;; " filler "\n")))
      (loop [call (request workspace) round 0]
        (let [result (alias-migration/execute! (config workspace receipt-dir) call)]
          (cond
            (= 2 round)
            (do (is (:ok result) (pr-str result))
                (is (= 12 (:files result))
                    "the migration that the two exclusions were supposed to
                     leave intact did not commit"))

            :else
            (do
              (is (= "alias-migration-source-too-large" (:error_type result))
                  (pr-str result))
              (is (= 12 (get-in (:next_call result) ["expect" "files"]))
                  "expect.files was decremented for a file nobody read")
              (is (string? (:expect_files_unchanged_reason result))
                  "the refusal does not say why expect.files is unchanged")
              (recur (json/parse-string
                       (json/generate-string (:next_call result)) true)
                     (inc round))))))
      (finally
        (delete-tree! workspace)))))

;; ---------------------------------------------------------------------------
;; the lib-only migration (the curtaincall-cfp anchor's shape)

(def lib-manifest (get-in corpus [:manifest :lib]))

(defn- lib-request
  ([workspace] (lib-request workspace {}))
  ([workspace overrides]
   (merge {:op "alias_migration"
           :workspace_root (.getPath workspace)
           :from {:lib fixture/from-lib :var nil}
           :to {:lib fixture/lib-to-lib :var nil
                :alias_policy fixture/lib-alias-policy}
           :scope {:paths ["src/**" "test/**"]}
           :expect {:files 14}}
          overrides)))

(defn- execute-lib!
  ([workspace] (execute-lib! workspace {}))
  ([workspace overrides]
   (let [receipt-dir (io/file workspace "receipts")]
     (.mkdirs receipt-dir)
     (alias-migration/execute! (config workspace receipt-dir)
                               (lib-request workspace overrides)))))

;; @spec MCP-OP-ALIAS-021
;; @spec MCP-OP-ALIAS-022
;; @spec MCP-OP-ALIAS-026
;; @spec MCP-OP-ALIAS-044
(deftest a-lib-only-migration-rewrites-every-var-and-renames-the-namespace
  (let [workspace (workspace!)]
    (try
      (let [result (execute-lib! workspace)]
        (testing "one O(1) receipt covers fourteen namespaces and the rename"
          (is (:ok result) (pr-str result))
          (is (= 14 (:files result)))
          (is (= 43 (:sites result)))
          (is (= 3 (:refer_sites result)) "t06's three bare referred uses")
          (is (= {"event-store" 13 "estore" 1} (:alias_histogram result)))
          (is (= 1 (:collisions_resolved result)))
          (is (= {:from "acid.fanout.store"
                  :to "acid.fanout.event-store"
                  :file "src/acid/fanout/store.clj"
                  :new_file "src/acid/fanout/event_store.clj"
                  :retired_to (str ".clj-surgeon/alias-migration/retired/"
                                   "src/acid/fanout/store.clj")}
                 (:lib_renamed result)))
          (testing "retired_to names a project-relative path, not a server path"
            (is (not (str/starts-with? (:retired_to (:lib_renamed result)) "/")))
            (is (.exists (io/file workspace
                                  (:retired_to (:lib_renamed result)))))))

        (testing "the receipt is still constant in N"
          (let [encoded (json/generate-string result)]
            (doseq [file (:targets lib-manifest)]
              (is (not (str/includes? encoded file)) (str "receipt names " file)))
            (is (< (count encoded) 1500))))

        (testing "the defining namespace moved and was renamed"
          (is (not (.exists (io/file workspace (:defining-file lib-manifest)))))
          (let [renamed (slurp (io/file workspace (:renamed-file lib-manifest)))]
            (is (str/starts-with? renamed "(ns acid.fanout.event-store)"))
            (is (str/includes? renamed "(defn find-event [id] {:old id})"))
            (is (str/includes? renamed "(defn other-var [id] {:other-old id})"))))

        (testing "the superseded file is retired, not destroyed"
          (is (= (get (:pre corpus) (:defining-file lib-manifest))
                 (slurp (io/file workspace
                                 (:retired_to (:lib_renamed result)))))))

        (testing "every var of the old lib moved, under every spelling"
          (is (str/includes? (slurp (io/file workspace "src/acid/fanout/t03.clj"))
                             "(event-store/find-event (first ids))")
              "an aliased use")
          (is (str/includes? (slurp (io/file workspace "src/acid/fanout/t04.clj"))
                             "(event-store/find-event id)")
              "a fully qualified use")
          (is (str/includes? (slurp (io/file workspace "src/acid/fanout/t12.clj"))
                             "[(event-store/find-event id) (event-store/find-event id)]")
              "both aliases of one lib in one file")
          (let [test-ns (slurp (io/file workspace "test/acid/fanout/store_test.clj"))]
            (is (str/includes? test-ns "(estore/find-event id)"))
            (is (str/includes? test-ns "(estore/other-var id)")
                "a second var of the same lib moves too")))

        (testing "preserve-refer, the default, keeps referred names and re-points them"
          (let [t06 (slurp (io/file workspace "src/acid/fanout/t06.clj"))]
            (is (str/includes? t06 "[acid.fanout.event-store :refer [find-event]]"))
            (is (str/includes? t06 "(find-event id)"))
            (is (not (str/includes? t06 ":as")) "no unused alias is introduced")))

        (testing "the decoys still hold"
          (is (str/includes? (slurp (io/file workspace "src/acid/fanout/t02.clj"))
                             "#_(store/find-event :disabled)"))
          (is (str/includes? (slurp (io/file workspace "src/acid/fanout/t12.clj"))
                             "(comment \"store/find-event stays here\")"))
          (doseq [[file regions] (get-in corpus [:manifest :protected])
                  {:keys [region]} regions
                  :when (.exists (io/file workspace file))]
            (is (str/includes? (slurp (io/file workspace file)) region)
                (str file " :: " region))))

        (testing "no live code still names the retired lib"
          (doseq [file (:targets lib-manifest)]
            (let [source (slurp (io/file workspace file))]
              (is (not (str/includes? source "[acid.fanout.store "))
                  (str file " still requires the retired lib"))
              (is (empty? (clojure.set/intersection
                            #{"acid.fanout.store" "store" "st" "s"}
                            (live-qualifiers source)))
                  (str file " still has live code qualified by the retired lib")))))

        (when (babashka-available?)
          (testing "the migrated tree loads"
            (let [loaded (run-babashka
                           workspace "--classpath" "src:test" "-e"
                           (str "(do "
                                (str/join " " (map #(str "(require '" % ")")
                                                   (concat (map :ns fixture/file-specs)
                                                           ["acid.fanout.store-test"
                                                            "acid.fanout.event-store"])))
                                " :ok)"))]
              (is (zero? (:exit loaded)) (:err loaded))))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-025
(deftest prefix-sharing-namespaces-are-not-touched
  (let [workspace (workspace!)]
    (try
      (let [result (execute-lib! workspace)]
        (is (:ok result) (pr-str result))
        (testing "the sibling namespaces are byte-identical and still in place"
          (doseq [sibling (:untouched-siblings lib-manifest)]
            (is (.exists (io/file workspace sibling)) sibling)
            (is (= (get (:pre corpus) sibling) (slurp (io/file workspace sibling)))
                sibling)))
        (testing "no sibling was renamed into the new lib's namespace"
          (is (not (.exists (io/file workspace "src/acid/fanout/event_store_pg.clj"))))
          (is (not (.exists (io/file workspace "src/acid/fanout/event_store_checkpoint.clj"))))
          (is (not (.exists (io/file workspace "test/acid/fanout/event_store_test.clj")))))
        (testing "a prefix-sharing ns NAME survives a rename that a sed would corrupt"
          (is (str/starts-with? (slurp (io/file workspace "test/acid/fanout/store_test.clj"))
                                "(ns acid.fanout.store-test")))
        (testing "a prefix-sharing USE survives untouched"
          (let [t07 (slurp (io/file workspace "src/acid/fanout/t07.clj"))]
            (is (str/includes? t07 "[acid.fanout.store-pg :as store-pg]"))
            (is (str/includes? t07 "(store-pg/write! id)"))))
        (testing "the siblings are not counted as migrated files"
          (is (= 14 (:files result)))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-024
(deftest a-mixed-var-spec-is-refused
  (let [workspace (workspace!)]
    (try
      (testing "a var on the from side only"
        (let [result (execute-lib! workspace
                                   {:from {:lib fixture/from-lib :var "find-event"}})]
          (is (false? (:ok result)))
          (is (= "alias-migration-mixed-var-spec" (:error_type result)))
          (is (= "find-event" (:from_var result)))
          (is (nil? (:to_var result)))
          (is (true? (:source_unchanged result)))
          (is (nil? (get-in result [:next_call "from" "var"]))
              "the next_call drops both vars, making it a lib-only migration")
          (is (nil? (get-in result [:next_call "to" "var"])))))
      (testing "a var on the to side only"
        (let [result (execute! workspace
                               {:from {:lib fixture/from-lib :var nil}})]
          (is (false? (:ok result)))
          (is (= "alias-migration-mixed-var-spec" (:error_type result)))))
      (testing "no byte was written"
        (doseq [[relative expected] (:pre corpus)]
          (is (= expected (slurp (io/file workspace relative))) relative)))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-023
(deftest target-lib-already-defined-is-refused
  (testing "the target lib is already defined while the old one still is"
    (let [workspace (workspace!)]
      (try
        (let [result (execute-lib! workspace
                                   {:to {:lib "acid.fanout.store2" :var nil
                                         :alias_policy fixture/lib-alias-policy}})]
          (is (false? (:ok result)) (pr-str result))
          (is (= "alias-migration-target-lib-exists" (:error_type result)))
          (is (= "acid.fanout.store" (:from_lib result)))
          (is (= "acid.fanout.store2" (:to_lib result)))
          (is (= "src/acid/fanout/store2.clj" (:file result)))
          (is (= "src/acid/fanout/store.clj" (:defining_file result)))
          (is (true? (:source_unchanged result)))
          (doseq [[relative expected] (:pre corpus)]
            (is (= expected (slurp (io/file workspace relative))) relative)))
        (finally
          (delete-tree! workspace)))))
  (testing "the destination path is occupied by a file that declares no ns"
    (let [workspace (workspace!)]
      (try
        (spit (io/file workspace "src/acid/fanout/event_store.clj")
              ";; a squatter with no ns form\n(def squatter true)\n")
        (let [result (execute-lib! workspace)]
          (is (false? (:ok result)) (pr-str result))
          (is (= "alias-migration-target-lib-exists" (:error_type result)))
          (is (= "src/acid/fanout/event_store.clj" (:file result)))
          (is (true? (:source_unchanged result)))
          (is (= (get (:pre corpus) "src/acid/fanout/store.clj")
                 (slurp (io/file workspace "src/acid/fanout/store.clj")))))
        (finally
          (delete-tree! workspace))))))

;; @spec MCP-OP-ALIAS-021
(deftest the-alias-qualify-refer-policy-rewrites-referred-names-instead
  (let [workspace (workspace!)]
    (try
      (let [result (execute-lib! workspace
                                 {:to {:lib fixture/lib-to-lib :var nil
                                       :alias_policy fixture/lib-alias-policy
                                       :refer_policy "alias-qualify"}})]
        (is (:ok result) (pr-str result))
        (is (= 46 (:sites result)) "the three bare uses become alias-qualified sites")
        (is (= 3 (:refer_sites result)))
        (let [t06 (slurp (io/file workspace "src/acid/fanout/t06.clj"))]
          (is (str/includes? t06 "[acid.fanout.event-store :as event-store]"))
          (is (not (str/includes? t06 ":refer")))
          (is (str/includes? t06 "(event-store/find-event id)"))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-022
(deftest a-failed-lib-migration-restores-the-defining-file
  (let [workspace (workspace!)
        receipt-dir (io/file workspace "receipts")]
    (.mkdirs receipt-dir)
    (try
      (let [result (alias-migration/execute!
                     (assoc (config workspace receipt-dir)
                            :verification-profiles {"fast" {:commands [["false"]]}})
                     (lib-request workspace {:verify "fast"}))]
        (is (false? (:ok result)) (pr-str result))
        (is (true? (:source_unchanged result)))
        (testing "the defining file is back where it started"
          (is (.exists (io/file workspace "src/acid/fanout/store.clj")))
          (is (not (.exists (io/file workspace "src/acid/fanout/event_store.clj")))))
        (testing "every other file is byte-identical to its pre-migration source"
          (doseq [[relative expected] (:pre corpus)]
            (is (= expected (slurp (io/file workspace relative))) relative))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-045
(deftest per-run-detail-files-are-retained-to-a-documented-bound
  (let [workspace (workspace!)
        receipt-dir (io/file workspace "receipts")
        details (io/file workspace ".clj-surgeon" "alias-migration")]
    (.mkdirs receipt-dir)
    (.mkdirs details)
    (try
      ;; thirty older runs, each with a distinct and strictly older timestamp.
      ;; They are written by the production writer, because a document is only
      ;; retention's to delete when this writer can prove it wrote it: the name
      ;; prefix says a file COULD be ours and is not proof of ownership.
      (let [old (mapv (fn [index]
                        (owned-detail! workspace
                                       (- (System/currentTimeMillis)
                                          (* 1000 (- 60 index)))))
                      (range 30))
            result (execute! workspace)
            manifest-name @(ns-resolve 'clj-surgeon.mcp-alias-migration
                                       'detail-manifest-name)
            remaining (->> (.listFiles details)
                           (filter #(str/ends-with? (.getName %) ".edn"))
                           (remove #(= manifest-name (.getName %)))
                           (mapv #(.getName %)))]
        (is (:ok result) (pr-str result))
        (is (= alias-migration/max-detail-files (count remaining))
            (str "detail files retained: " (sort remaining)))
        (testing "the run's own detail file is the one that survives"
          (is (contains? (set remaining)
                         (.getName (io/file (:details_path result)))))
          (is (= (:file (first (:files (edn/read-string
                                         (slurp (io/file workspace
                                                         (:details_path result)))))))
                 "src/acid/fanout/t01.clj")))
        (testing "the oldest runs are the ones dropped"
          (is (not (contains? (set remaining) (.getName ^java.io.File (first old)))))
          (is (contains? (set remaining) (.getName ^java.io.File (last old))))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-052
(deftest detail-retention-is-published-as-best-effort-because-peers-are-pruned
  (let [workspace (workspace!)
        receipt-dir (io/file workspace "receipts")
        details (io/file workspace ".clj-surgeon" "alias-migration")
        ;; twenty peers, each holding a details_path its own receipt published
        ;; a moment ago and its own caller may not have read yet. They are real
        ;; runs of this writer, recorded in the manifest every run shares, so a
        ;; peer's document stays within retention's reach even now that
        ;; retention deletes nothing it cannot prove it wrote.
        peers (mapv (fn [index]
                      (owned-detail! workspace
                                     (- (System/currentTimeMillis)
                                        (* 1000 (- 60 index)))))
                    (range 20))]
    (.mkdirs receipt-dir)
    (try
      (let [result (execute! workspace)]
        (is (:ok result) (pr-str result))
        (is (= "best-effort" (:details_retention result))
            "the receipt claims a durability the directory does not have")
        (is (= alias-migration/max-detail-files (:details_retained result)))
        (testing "the word is earned: this run pruned a peer's published path"
          (is (some #(not (.exists ^java.io.File %)) peers)
              "no peer was pruned, so the claim would be untestable here"))
        (testing "and the run's own detail document is still readable"
          (is (.exists (io/file workspace (:details_path result))))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-045
(deftest detail-pruning-never-deletes-a-document-this-writer-does-not-own
  ;; `.clj-surgeon/alias-migration/` is a legal receipt directory: it is inside
  ;; the workspace, it is pruned from every scope walk, and nothing in the
  ;; request forbids it. When the detail writer claims every `.edn` in its
  ;; directory, a run configured that way prunes its OWN undo receipt while
  ;; publishing ok=true committed=true — a receipt naming an inverse that no
  ;; longer exists, which is the worst shape a write tool can return.
  (let [workspace (workspace!)
        details (.getCanonicalFile (io/file workspace ".clj-surgeon"
                                            "alias-migration"))]
    (.mkdirs details)
    (try
      ;; twenty documents this writer did not write, stamped newer than the run
      ;; about to happen. That is the concurrent-peer shape `details-retention`
      ;; already documents: twenty peers publishing between this run's receipt
      ;; and this run's prune push the receipt past the retention window.
      (let [later (+ (System/currentTimeMillis) 600000)]
        (dotimes [index 20]
          (let [peer (io/file details (str "peer-" index ".edn"))]
            (spit peer "{:version 1 :files []}")
            (.setLastModified peer (+ later index)))))
      (let [result (alias-migration/execute! (config workspace details)
                                             (request workspace))]
        (is (:ok result) (pr-str result))
        (is (true? (:committed result)))
        (let [receipt (io/file (:undo_receipt result))]
          (is (.exists receipt)
              (str "the committed run pruned its own undo receipt: "
                   (:undo_receipt result)))
          (when (.exists receipt)
            (is (= (:receipt_hash result)
                   (:receipt-hash (edn/read-string (slurp receipt))))
                "the surviving file is not the receipt this run published"))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-054
(deftest receipt-and-detail-namespaces-are-proved-disjoint-by-a-typed-guard
  (let [guard (ns-resolve 'clj-surgeon.mcp-alias-migration
                          'receipt-detail-collision?)
        detail-name? (ns-resolve 'clj-surgeon.mcp-alias-migration
                                 'detail-document-name?)
        workspace (temp-dir)
        details (io/file workspace ".clj-surgeon" "alias-migration")
        receipts (io/file workspace "receipts")]
    (try
      (is (some? detail-name?)
          "the detail writer owns no name pattern, so it cannot tell its own
           documents from anything else in the directory")
      (is (some? guard)
          "no typed guard refuses receipt/detail co-location")
      (when (and guard detail-name?)
        (is (true? (detail-name? "detail-0dd1.edn")))
        (is (false? (detail-name? "0dd1.edn")))
        (testing "a receipt name inside the detail namespace is refused"
          (is (true? (guard (.getPath workspace) (.getPath details)
                            "detail-0dd1.edn"))))
        (testing "and the names the two actually use are disjoint"
          (is (false? (guard (.getPath workspace) (.getPath details)
                             (str (java.util.UUID/randomUUID) ".edn"))))
          (is (false? (guard (.getPath workspace) (.getPath receipts)
                             "detail-0dd1.edn")))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-054
(deftest detail-pruning-deletes-only-documents-it-can-prove-it-wrote
  ;; A name prefix says a file COULD be ours; it is not consent to delete it.
  ;; A caller keeping twenty `detail-*.edn` files of its own in this directory
  ;; watched retention take one of them, because prefix matching was standing
  ;; in for ownership. Ownership is now proved twice — the manifest this writer
  ;; keeps, and a marker inside the document — and neither is a guess about
  ;; somebody else's file.
  (let [workspace (workspace!)
        receipt-dir (io/file workspace "receipts")
        details (io/file workspace ".clj-surgeon" "alias-migration")
        base (- (System/currentTimeMillis) 600000)]
    (.mkdirs receipt-dir)
    (.mkdirs details)
    (try
      (let [callers (mapv (fn [index]
                            (let [file (io/file details
                                                (str "detail-caller-" index ".edn"))]
                              (spit file (str "{:version 1 :caller " index "}"))
                              (.setLastModified file (+ base index))
                              file))
                          (range 20))
            ;; twenty documents this writer really did write, so the run below
            ;; reaches the bound and has one of its OWN to prune
            mine (mapv (fn [index] (owned-detail! workspace (+ base 100000 index)))
                       (range 20))
            result (alias-migration/execute! (config workspace receipt-dir)
                                             (request workspace))
            gone (remove (fn [^java.io.File file] (.exists file)) callers)]
        (is (:ok result) (pr-str result))
        (testing "every one of the caller's twenty files is still there"
          (is (= 20 (count callers)))
          (is (empty? gone)
              (str "retention deleted files it never wrote: "
                   (mapv (fn [^java.io.File file] (.getName file)) gone))))
        (testing "and its own twenty-first is what it pruned instead"
          (is (= 19 (count (filter (fn [^java.io.File file] (.exists file)) mine))))
          (is (.exists (io/file workspace (:details_path result))))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-054
(deftest a-recorded-document-whose-marker-is-gone-is-no-longer-this-writers-to-delete
  ;; The manifest alone is not proof either. A file the caller replaced under a
  ;; name this writer once used is the caller's file now, so retention skips it
  ;; and takes the next document it can still prove it wrote.
  (let [workspace (workspace!)
        details (io/file workspace ".clj-surgeon" "alias-migration")
        base (- (System/currentTimeMillis) 600000)]
    (try
      (let [mine (mapv (fn [index] (owned-detail! workspace (+ base (* 1000 index))))
                       (range 20))
            stripped (first mine)
            next-oldest (second mine)]
        ;; the caller overwrites the oldest candidate; the manifest still names
        ;; it, but the bytes on disk are no longer the document we wrote
        (spit stripped "{:version 1 :files [] :owner \"someone-else\"}")
        (.setLastModified stripped base)
        (owned-detail! workspace (+ base 100000))
        (is (.exists stripped)
            "a manifest entry whose file lost its marker was deleted anyway")
        (owned-detail! workspace (+ base 200000))
        (is (.exists stripped)
            "the unmarked file was taken on the next pass instead")
        (is (not (.exists next-oldest))
            "retention skipped the unmarked file but pruned nothing in its place")
        (testing "and the manifest itself is never a pruning candidate"
          (let [manifest-name (ns-resolve 'clj-surgeon.mcp-alias-migration
                                          'detail-manifest-name)]
            (is (some? manifest-name)
                "the writer keeps no manifest, so it cannot name what it owns")
            (when manifest-name
              (is (.exists (io/file details @manifest-name)))))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-054
(deftest the-receipt-detail-collision-refuses-before-it-creates-the-directory
  ;; A refusal that first mkdirs the very directory it is refusing to write in
  ;; has mutated the tree it reports untouched.
  (let [workspace (workspace!)
        details (io/file workspace ".clj-surgeon" "alias-migration")]
    (try
      (is (not (.exists details)) "the fixture already created the directory")
      (with-redefs [alias-migration/new-receipt-name (fn [] "detail-0dd1.edn")]
        (let [result (alias-migration/execute! (config workspace details)
                                               (request workspace))]
          (is (false? (:ok result)) (pr-str result))
          (is (= "alias-migration-receipt-detail-collision" (:error_type result)))
          (is (true? (:source_unchanged result)))
          (is (not (.exists details))
              "the refusal created the directory it refused to write in")))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-054
(deftest the-receipt-detail-collision-sees-through-a-symlinked-twin
  ;; Textual absolute-path equality is not directory identity: a symlink and
  ;; the directory it points at are one directory, and a guard written on
  ;; strings misses exactly the aliasing it exists to catch.
  (let [workspace (workspace!)
        details (io/file workspace ".clj-surgeon" "alias-migration")
        twin (io/file workspace "receipts-link")
        guard (ns-resolve 'clj-surgeon.mcp-alias-migration
                          'receipt-detail-collision?)]
    (.mkdirs details)
    (Files/createSymbolicLink (.toPath twin) (.toPath details)
                              (make-array FileAttribute 0))
    (try
      (testing "the predicate"
        (is (true? (guard (.getPath workspace) (.getPath twin)
                          "detail-0dd1.edn"))))
      (testing "and the verb that stands on it"
        (with-redefs [alias-migration/new-receipt-name (fn [] "detail-0dd1.edn")]
          (let [result (alias-migration/execute! (config workspace twin)
                                                 (request workspace))]
            (is (false? (:ok result)) (pr-str result))
            (is (= "alias-migration-receipt-detail-collision"
                   (:error_type result)))
            (is (true? (:source_unchanged result))))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-054
(deftest the-receipt-detail-collision-normalises-a-remainder-that-does-not-exist
  ;; The guard resolves the nearest EXISTING ancestor and appends the rest. The
  ;; rest is caller text: `missing/../.clj-surgeon/alias-migration` IS the
  ;; detail directory, and appended without normalisation it compares unequal
  ;; to it — so the receipt is published in the directory the guard exists to
  ;; keep it out of, under a name the detail writer's retention owns.
  (let [workspace (workspace!)
        details (io/file workspace ".clj-surgeon" "alias-migration")
        sneaky (io/file workspace "missing" ".." ".clj-surgeon" "alias-migration")]
    (try
      (testing "the predicate"
        (is (true? (alias-migration/receipt-detail-collision?
                     (.getPath workspace) (.getPath sneaky) "detail-0dd1.edn"))))
      (testing "and the verb that stands on it"
        (with-redefs [alias-migration/new-receipt-name (fn [] "detail-0dd1.edn")]
          (let [result (alias-migration/execute! (config workspace sneaky)
                                                 (request workspace))]
            (is (false? (:ok result)) (pr-str result))
            (is (= "alias-migration-receipt-detail-collision"
                   (:error_type result)))
            (is (true? (:source_unchanged result)))
            (is (not (.exists details))
                "the refusal created the directory it refused to write in"))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-054
(deftest a-receipt-directory-that-climbs-past-its-nearest-existing-ancestor-refuses
  ;; A remainder that still holds `..` after normalisation names a directory
  ;; above the ancestor whose identity was proved, so the guard cannot say what
  ;; directory it is. An undecidable identity is a typed refusal, never a pass.
  (let [workspace (workspace!)
        outside-name (str "elsewhere-" (java.util.UUID/randomUUID))
        outside (io/file (.getParentFile workspace) outside-name)
        escaping (io/file workspace "missing" ".." ".." outside-name)]
    (try
      (let [result (alias-migration/execute! (config workspace escaping)
                                             (request workspace))]
        (is (false? (:ok result)) (pr-str result))
        (is (= "alias-migration-receipt-dir-escapes" (:error_type result)))
        (is (true? (:source_unchanged result)))
        (is (not (.exists outside))
            "the refusal created the directory it refused to write in"))
      (finally
        (delete-tree! outside)
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-054
(deftest the-receipt-detail-collision-is-re-proved-after-the-directory-exists
  ;; The pre-create guard decides on a path that does not exist yet, and a
  ;; path's identity is not settled until it does: a symlink installed between
  ;; the check and the mkdir makes the created receipt directory the detail
  ;; directory, and the run publishes ok=true with its undo receipt sitting in
  ;; the one place its own retention may delete it. Identity has to be re-proved
  ;; on the directory that was CREATED, not on the one that was checked.
  (let [workspace (workspace!)
        details (io/file workspace ".clj-surgeon" "alias-migration")
        receipts (io/file workspace "receipts")
        guard alias-migration/receipt-detail-collision?]
    (.mkdirs details)
    (try
      (with-redefs [alias-migration/new-receipt-name (fn [] "detail-0dd1.edn")
                    alias-migration/receipt-detail-collision?
                    (fn [project-root receipt-dir receipt-name]
                      (let [answer (guard project-root receipt-dir receipt-name)]
                        ;; the race is won here: the checked path becomes a
                        ;; symlink to the detail directory after the answer
                        (when-not (.exists receipts)
                          (Files/createSymbolicLink (.toPath receipts)
                                                    (.toPath details)
                                                    (make-array FileAttribute 0)))
                        answer))]
        (let [result (alias-migration/execute! (config workspace receipts)
                                               (request workspace))]
          (is (false? (:ok result)) (pr-str result))
          (is (= "alias-migration-receipt-detail-collision"
                 (:error_type result)))
          (is (= "post-create" (:phase result)))
          (is (true? (:source_unchanged result)))
          (is (empty? (seq (.listFiles details)))
              (str "a receipt was published inside the detail directory: "
                   (mapv (fn [^java.io.File file] (.getName file))
                         (or (seq (.listFiles details)) []))))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-054
(deftest a-recorded-document-whose-run-id-no-longer-names-it-is-left-alone
  ;; Defence in depth against ACCIDENTAL replacement, not against forgery. A
  ;; document whose bytes were swapped for another run's document still carries
  ;; the writer's marker and is still named in the manifest, but the `:run-id`
  ;; inside it no longer names the file it sits in — so it is not the document
  ;; this writer recorded, and retention leaves it alone. A deliberate forger
  ;; rewrites the run-id along with the marker; this stops the copy, not the liar.
  (let [workspace (workspace!)
        base (- (System/currentTimeMillis) 600000)]
    (try
      (let [mine (mapv (fn [index] (owned-detail! workspace (+ base (* 1000 index))))
                       (range 20))
            swapped (first mine)
            next-oldest (second mine)]
        (spit swapped (slurp (last mine)))
        (.setLastModified swapped base)
        (owned-detail! workspace (+ base 100000))
        (is (.exists swapped)
            "a document carrying another run's id was deleted as ours")
        (owned-detail! workspace (+ base 200000))
        (is (.exists swapped)
            "the swapped document was taken on the next pass instead")
        (is (not (.exists next-oldest))
            "retention skipped the swapped document but pruned nothing in its place"))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-047
(deftest the-heap-guard-marks-the-transactions-write-boundary-not-its-entrance
  ;; Entering `execute-mcp-change!` is not a mutation. Spec validation, the
  ;; frozen read, compilation, receipt staging and the whole-file hash preflight
  ;; all run inside the kernel BEFORE a source byte is written, so a marker set
  ;; at the call to it tells the caller its tree may have been written when the
  ;; kernel refused with every file byte-identical and no receipt published.
  (let [workspace (workspace!)
        receipt-dir (io/file workspace "receipts")]
    (.mkdirs receipt-dir)
    (try
      (with-redefs [transaction/execute-mcp-change!
                    (fn [& _] (throw (OutOfMemoryError. "Java heap space")))]
        (let [result (alias-migration/execute! (config workspace receipt-dir)
                                               (request workspace))]
          (is (false? (:ok result)) (pr-str result))
          (is (= "alias-migration-resource-exhausted" (:error_type result)))
          (is (true? (:source_unchanged result))
              "the kernel was entered but never reached its write boundary")
          (is (false? (:mutation_attempted result)))
          (is (= "correct_request" (:next_action result)))
          (testing "and the tree agrees with the refusal"
            (doseq [[relative expected] (:pre corpus)]
              (is (= expected (slurp (io/file workspace relative))) relative)))
          (testing "and no receipt was published for a call that never wrote"
            (is (empty? (filter (fn [^java.io.File file]
                                  (str/ends-with? (.getName file) ".edn"))
                                (.listFiles receipt-dir)))))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-043
(deftest a-rolled-back-retire-failure-deletes-its-undo-receipt
  (let [workspace (workspace!)
        receipt-dir (io/file workspace "receipts")
        ;; the retire destination's parent directory is occupied by a file, so
        ;; the move throws after the kernel has already committed
        blocker (io/file workspace ".clj-surgeon" "alias-migration" "retired"
                         "src" "acid" "fanout")]
    (.mkdirs receipt-dir)
    (.mkdirs (.getParentFile blocker))
    (spit blocker "not a directory\n")
    (try
      (let [result (alias-migration/execute! (config workspace receipt-dir)
                                             (lib-request workspace))
            receipts (filter #(str/ends-with? (.getName %) ".edn")
                             (.listFiles receipt-dir))]
        (is (false? (:ok result)) (pr-str result))
        (is (= "alias-migration-retire-failed" (:error_type result)))
        (is (true? (:source_unchanged result)))

        (testing "the rolled-back transaction leaves no undo receipt behind"
          (is (= [] (mapv #(.getName %) receipts))))

        (testing "every file is byte-identical to its pre-migration source"
          (doseq [[relative expected] (:pre corpus)]
            (is (= expected (slurp (io/file workspace relative))) relative))
          (is (not (.exists (io/file workspace "src/acid/fanout/event_store.clj"))))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-042
(deftest the-receipt-and-its-summary-report-the-kernel-s-own-committed-flag
  (let [workspace (workspace!)]
    (try
      (let [validated (alias-migration/validate-request (request workspace))
            planned (alias-migration/plan! (.getPath workspace) (:request validated))
            plan (:plan planned)]
        (is (:ok planned) (pr-str planned))

        (testing "a committed transaction is reported as committed"
          (let [receipt (assoc (alias-migration/receipt
                                 plan {:committed true} "details.edn")
                               :elapsed_ms 1)]
            (is (true? (:ok receipt)))
            (is (true? (:committed receipt)))
            (is (= "none" (:next_action receipt)))
            (is (str/includes? (mcp-tool/alias-migration-summary receipt)
                               "\u2713 atomic commit complete"))))

        (testing "a transaction the kernel did not commit claims nothing"
          (let [receipt (assoc (alias-migration/receipt
                                 plan {:committed false} "details.edn")
                               :elapsed_ms 1)]
            (is (false? (:ok receipt)))
            (is (false? (:committed receipt)))
            (is (not= "none" (:next_action receipt)))
            (is (not (str/includes? (mcp-tool/alias-migration-summary receipt)
                                    "\u2713"))
                "the summary printed a check mark for an uncommitted transaction"))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-042
(deftest a-kernel-result-that-did-not-commit-refuses-instead-of-publishing-a-receipt
  (let [workspace (workspace!)
        receipt-dir (io/file workspace "receipts")]
    (.mkdirs receipt-dir)
    (try
      (with-redefs [transaction/execute-mcp-change!
                    (fn [_opts]
                      {:ok true
                       :committed false
                       :receipt-file (.getPath (io/file receipt-dir "none.edn"))
                       :receipt-hash "0"})]
        (let [result (execute! workspace)]
          (is (false? (:ok result)) (pr-str result))
          (is (not (true? (:committed result))))
          (is (true? (:source_unchanged result)))
          (is (not (str/includes?
                     (mcp-tool/alias-migration-summary
                       (assoc result :elapsed_ms 1))
                     "atomic commit complete")))))
      (testing "no byte was written"
        (doseq [[relative expected] (:pre corpus)]
          (is (= expected (slurp (io/file workspace relative))) relative)))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-041
(deftest a-symlinked-defining-file-refuses-instead-of-retiring-the-link
  (let [workspace (workspace!)
        receipt-dir (io/file workspace "receipts")
        defining (io/file workspace "src/acid/fanout/store.clj")
        real (io/file workspace "vendor/store.clj")
        original (get (:pre corpus) "src/acid/fanout/store.clj")]
    (.mkdirs receipt-dir)
    (.mkdirs (.getParentFile real))
    (try
      ;; the definition lives outside scope and the scoped path is a link to it,
      ;; so the edits address the real file while the retire would move the link
      (Files/move (.toPath defining) (.toPath real)
                  (make-array java.nio.file.CopyOption 0))
      (symlink! defining real)
      (let [result (alias-migration/execute! (config workspace receipt-dir)
                                             (lib-request workspace))]
        (is (false? (:ok result)) (pr-str result))
        (is (= "alias-migration-retire-symlink-refused" (:error_type result)))
        (is (true? (:source_unchanged result)))

        (testing "the link and its target are both exactly as they were"
          (is (Files/isSymbolicLink (.toPath defining)))
          (is (= original (slurp real))))

        (testing "no stray file remains"
          (is (not (.exists (io/file workspace "src/acid/fanout/event_store.clj"))))
          (is (not (.exists (io/file workspace ".clj-surgeon" "alias-migration"
                                     "retired"))))))
      (finally
        (Files/deleteIfExists (.toPath defining))
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-028
(deftest verification-is-opt-in-and-an-unknown-profile-refuses-before-writing
  (let [workspace (workspace!)
        receipt-dir (io/file workspace "receipts")]
    (.mkdirs receipt-dir)
    (try
      (testing "a workspace with a configured profile is NOT verified unless asked"
        (let [runs (atom 0)
              result (alias-migration/execute!
                       (assoc (config workspace receipt-dir)
                              :verification-profiles
                              {"fast" {:commands [["false"]]}
                               "full" {:commands [["false"]]}})
                       (request workspace))]
          (is (:ok result) (pr-str result))
          (is (= {:status "not-requested"} (:focused_test result)))
          (is (zero? @runs))))
      (finally
        (delete-tree! workspace)))
    (let [workspace (workspace!)
          receipt-dir (io/file workspace "receipts")]
      (.mkdirs receipt-dir)
      (try
        (testing "asking for a profile the workspace does not configure refuses"
          (let [result (alias-migration/execute!
                         (assoc (config workspace receipt-dir)
                                :verification-profiles {"fast" {:commands []}})
                         (request workspace {:verify "nonexistent"}))]
            (is (false? (:ok result)) (pr-str result))
            (is (= "unknown-verification-profile" (:error_type result)))
            (is (true? (:source_unchanged result)))
            (doseq [[relative expected] (:pre corpus)]
              (is (= expected (slurp (io/file workspace relative))) relative))))
        (finally
          (delete-tree! workspace))))))

;; ---------------------------------------------------------------------------
;; round six: the receipt directory's own identity, after the guards answer

;; @spec MCP-OP-ALIAS-056
(deftest a-receipt-directory-swapped-after-the-second-check-never-reports-ok
  ;; The post-create guard proves the identity of the directory that now
  ;; exists — and then the write happens through a NAME, which the kernel
  ;; resolves afresh on every open. A symlink installed between the second
  ;; answer and the first byte redirects the receipt into the detail directory,
  ;; and the run reported ok=true with its undo receipt sitting in the one
  ;; place its own retention may delete. The OS leaves that window open; what
  ;; must never happen is a SUCCESS reported over it.
  (let [workspace (workspace!)
        details (io/file workspace ".clj-surgeon" "alias-migration")
        receipts (io/file workspace "receipts")
        guard alias-migration/receipt-detail-collision?
        calls (atom 0)]
    (.mkdirs details)
    (try
      (with-redefs [alias-migration/receipt-detail-collision?
                    (fn [project-root receipt-dir receipt-name]
                      (let [answer (guard project-root receipt-dir receipt-name)]
                        (when (= 2 (swap! calls inc))
                          ;; the window: the directory whose identity was just
                          ;; proved becomes a link to the detail directory
                          (.delete receipts)
                          (Files/createSymbolicLink (.toPath receipts)
                                                    (.toPath details)
                                                    (make-array FileAttribute 0)))
                        answer))]
        (let [result (alias-migration/execute! (config workspace receipts)
                                               (request workspace))
              ;; a receipt, not a detail document: the detail writer's own
              ;; files legitimately live here
              published (vec (filter (fn [^java.io.File file]
                                       (and (str/ends-with? (.getName file) ".edn")
                                            (not (str/starts-with? (.getName file)
                                                                   "detail-"))))
                                     (or (seq (.listFiles details)) [])))]
          (is (= 2 @calls) "the identity guard was not asked on both sides of creation")
          (when (:ok result)
            (is (not= (.getCanonicalPath details)
                      (.getCanonicalPath (.getParentFile
                                           (io/file (:undo_receipt result)))))
                "ok=true with the undo receipt canonically inside the detail directory"))
          (is (empty? published)
              (str "an undo receipt was left in the detail directory: "
                   (mapv (fn [^java.io.File file] (.getName file)) published)))
          (when-not (:ok result)
            (is (= "alias-migration-receipt-published-elsewhere"
                   (:error_type result))
                (pr-str result))
            (is (= "post-write" (:phase result)))
            ;; @spec MCP-OP-ALIAS-056
            ;; `source_unchanged` is the ROLLBACK's answer, not a constant.
            ;; The assertion this replaced read `(is (true? (:source_unchanged
            ;; result)))` and could never fire, because `commit-refusal`
            ;; synthesised the value it asserted from the absence of a
            ;; `:committed` key. The two fields are pinned to each other here,
            ;; and the failing-undo witness below is the one that moves them.
            (is (true? (:rolled_back result))
                "the redirect was rolled back, so the caller is told so")
            (is (= (:rolled_back result) (:source_unchanged result))
                "source_unchanged disagreed with the rollback that produced it")
            (is (zero? (:files_still_migrated result)))
            (testing "and the tree agrees with the refusal"
              (doseq [[relative expected] (:pre corpus)]
                (is (= expected (slurp (io/file workspace relative))) relative))))))
      (finally
        (delete-tree! workspace)))))

;; ---------------------------------------------------------------------------
;; round seven: what a post-write refusal owes the caller about the tree

(defn- post-write-redirect!
  "Run one migration whose receipt directory becomes a link to the detail
  directory AFTER the post-create guard has answered — the round-six race —
  with `undo` standing in for the transaction kernel's rollback."
  [workspace details receipts undo]
  (let [guard alias-migration/receipt-detail-collision?
        real-undo transaction/execute-undo!
        calls (atom 0)]
    (with-redefs [alias-migration/receipt-detail-collision?
                  (fn [project-root receipt-dir receipt-name]
                    (let [answer (guard project-root receipt-dir receipt-name)]
                      (when (= 2 (swap! calls inc))
                        (.delete ^java.io.File receipts)
                        (Files/createSymbolicLink (.toPath ^java.io.File receipts)
                                                  (.toPath ^java.io.File details)
                                                  (make-array FileAttribute 0)))
                      answer))
                  transaction/execute-undo! (or undo real-undo)]
      (alias-migration/execute! (config workspace receipts)
                                (request workspace)))))

(defn- still-migrated
  "The `:pre` files whose bytes on disk are no longer their pre-migration bytes."
  [workspace]
  (into (sorted-set)
        (keep (fn [[relative expected]]
                (when-not (= expected (slurp (io/file workspace relative)))
                  relative)))
        (:pre corpus)))

;; @spec MCP-OP-ALIAS-056
(deftest a-post-write-refusal-whose-rollback-failed-reports-a-migrated-tree
  ;; The refusal that detects a redirected receipt rolls the transaction back
  ;; — and when that rollback FAILS the verb used to publish
  ;; `source_unchanged: true` and the sentence "the alias migration was rolled
  ;; back" over twelve files that were still migrated, with the orphan receipt
  ;; named nowhere. `commit-refusal` computed the field as
  ;; `(not (:committed commit))` over a map that carries no `:committed` key,
  ;; so it was a constant no failing rollback could move.
  (let [workspace (workspace!)
        details (io/file workspace ".clj-surgeon" "alias-migration")
        receipts (io/file workspace "receipts")]
    (.mkdirs details)
    (try
      (let [result (post-write-redirect!
                     workspace details receipts
                     (fn [_] {:ok false :error "injected rollback failure"}))
            migrated (still-migrated workspace)]
        (is (false? (:ok result)) (pr-str result))
        (is (= "alias-migration-receipt-published-elsewhere" (:error_type result))
            (pr-str result))
        (is (= "post-write" (:phase result)))
        (is (false? (:rolled_back result))
            "the rollback failed and the caller was not told")
        (is (false? (:source_unchanged result))
            "twelve files are still migrated and source_unchanged said true")
        (is (= 12 (:files_still_migrated result)))
        (is (= (count migrated) (:files_still_migrated result))
            (str "the published count disagrees with the tree: " (vec migrated)))
        (is (string? (:receipt_file result))
            "the orphan receipt was left behind and never named")
        (is (.exists (io/file (str (:receipt_file result))))
            (str "receipt_file does not name a file that exists: "
                 (:receipt_file result)))
        (is (str/includes? (str (:error result)) "rollback FAILED")
            (str "the prose still claims a rollback that failed: "
                 (:error result))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-056
(deftest a-post-write-refusal-whose-rollback-succeeded-reports-a-restored-tree
  (let [workspace (workspace!)
        details (io/file workspace ".clj-surgeon" "alias-migration")
        receipts (io/file workspace "receipts")]
    (.mkdirs details)
    (try
      (let [result (post-write-redirect! workspace details receipts nil)]
        (is (false? (:ok result)) (pr-str result))
        (is (= "post-write" (:phase result)))
        (is (true? (:rolled_back result)))
        (is (true? (:source_unchanged result)))
        (is (= 0 (:files_still_migrated result)))
        (is (empty? (still-migrated workspace)))
        (is (not (str/includes? (str (:error result)) "FAILED"))
            (pr-str (:error result))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-056
(deftest a-receipt-resolved-onto-a-name-without-edn-is-refused-not-reported-ok
  ;; `canonical-receipt-path` resolves the receipt NAME before staging, so a
  ;; link already sitting on the destination name is followed rather than
  ;; replaced by the atomic rename. When the link points at a non-`.edn` name
  ;; INSIDE the proved directory, the parent comparison agrees and the verb
  ;; reported ok over a receipt `execute-undo!` refuses to read — an
  ;; unrecoverable transaction published as a success.
  (let [workspace (workspace!)
        receipts (io/file workspace "receipts")
        victim (io/file receipts "victim.txt")]
    (.mkdirs receipts)
    (spit victim "not a receipt\n")
    (symlink! (io/file receipts "pinned.edn") victim)
    (try
      (let [result (with-redefs [alias-migration/new-receipt-name
                                 (fn [] "pinned.edn")]
                     (alias-migration/execute! (config workspace receipts)
                                               (request workspace)))]
        (is (false? (:ok result))
            (str "a receipt published under a name execute-undo! refuses was "
                 "reported ok: " (pr-str result)))
        (is (= "alias-migration-receipt-published-elsewhere" (:error_type result))
            (pr-str result))
        (is (= "post-write" (:phase result)))
        (is (false? (:rolled_back result))
            "the undo cannot read a receipt without .edn, so it did not roll back")
        (is (false? (:source_unchanged result)))
        (is (= 12 (:files_still_migrated result)))
        (is (str/ends-with? (str (:receipt_file result)) "victim.txt")
            (pr-str (:receipt_file result))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-056
(deftest a-control-directory-outside-the-workspace-root-is-still-refused
  ;; Containment answers only about the workspace root, so a LINKED git
  ;; worktree — whose real control directory is `<main>/.git/worktrees/<name>`,
  ;; outside the root — published a receipt into git's per-worktree ref
  ;; storage with ok=true. A control segment is refused wherever it appears,
  ;; and every control-directory refusal names the directory it found.
  (let [linked (workspace!)
        rooted (workspace!)
        main (temp-dir)
        gitdir (io/file main ".git" "worktrees" "wt" "refs" "heads")]
    (.mkdirs gitdir)
    (try
      (let [outside (alias-migration/execute!
                      (config linked gitdir) (request linked))
            inside (alias-migration/execute!
                     {:project-root (.getPath rooted)
                      :receipt-dir ".git/refs/heads"}
                     (request rooted))]
        (testing "the linked worktree's real control directory"
          (is (false? (:ok outside)) (pr-str outside))
          (is (= "alias-migration-receipt-dir-in-control-directory"
                 (:error_type outside))
              (pr-str outside))
          (is (= ".git" (:control_directory outside))
              "the control directory the guard found is not on the wire")
          (is (empty? (filter (fn [^java.io.File file]
                                (str/ends-with? (.getName file) ".edn"))
                              (or (seq (.listFiles gitdir)) [])))
              "a receipt was published into git's per-worktree ref storage"))
        (testing "and the in-root form names it too"
          (is (false? (:ok inside)) (pr-str inside))
          (is (= "alias-migration-receipt-dir-in-control-directory"
                 (:error_type inside))
              (pr-str inside))
          (is (= ".git" (:control_directory inside)) (pr-str inside)))
        (doseq [workspace [linked rooted]]
          (doseq [[relative expected] (:pre corpus)]
            (is (= expected (slurp (io/file workspace relative))) relative))))
      (finally
        (delete-tree! linked)
        (delete-tree! rooted)
        (delete-tree! main)))))


;; @spec MCP-OP-ALIAS-056
(deftest two-concurrent-receipt-directory-creations-record-disjoint-sets
  ;; `createDirectories` answers for a whole chain and cannot say which links
  ;; it made. Two calls racing the same missing chain both recorded all of it
  ;; as "created by me", so one caller's cleanup deleted directories the peer
  ;; was still counting as its own. A call may record only what its OWN create
  ;; brought into being.
  (let [create! (ns-resolve 'clj-surgeon.mcp-alias-migration
                            'create-receipt-directory!)
        base (temp-dir)]
    (try
      (is (some? create!) "the receipt directory creator is gone")
      (when create!
        (dotimes [round 40]
          (let [deep (reduce (fn [^java.io.File acc index]
                               (io/file acc (str "d" index)))
                             (io/file base (str "round-" round))
                             (range 40))
                barrier (java.util.concurrent.CyclicBarrier. 2)
                left (future (.await barrier) (vec (create! deep)))
                right (future (.await barrier) (vec (create! deep)))
                a @left
                b @right]
            (is (zero? (count (clojure.set/intersection (set a) (set b))))
                (str "round " round ": "
                     (count (clojure.set/intersection (set a) (set b)))
                     " directories were recorded by BOTH calls as their own"))
            (doseq [^java.nio.file.Path path a]
              (.delete (.toFile path)))
            (is (zero? (count (remove (fn [^java.nio.file.Path path]
                                        (.exists (.toFile path)))
                                      b)))
                (str "round " round ": one caller's cleanup removed "
                     (count (remove (fn [^java.nio.file.Path path]
                                      (.exists (.toFile path)))
                                    b))
                     " directories the peer recorded")))))
      (finally
        (delete-tree! base)))))

;; @spec MCP-OP-ALIAS-056
(deftest a-receipt-directory-inside-a-control-directory-refuses-before-any-write
  ;; `.git` is not this verb's tree. A receipt published into `.git/refs/heads`
  ;; is a file git reads as a ref, and the workspace's own tooling breaks:
  ;; `git show-ref` exits 128 on a bad ref. The refusal is structural and comes
  ;; before any write, because the damage is done by the first published byte.
  (let [workspace (workspace!)
        path (.getPath workspace)
        heads (io/file workspace ".git" "refs" "heads")]
    (try
      (shell/sh "git" "init" "--quiet" "-b" "main" :dir path)
      (shell/sh "git" "-c" "user.email=witness@example.invalid"
                "-c" "user.name=witness" "commit" "--allow-empty"
                "-m" "root" :dir path)
      (is (zero? (:exit (shell/sh "git" "show-ref" :dir path)))
          "the fixture repository has no ref to break")
      (let [result (alias-migration/execute! (config workspace heads)
                                             (request workspace))]
        (is (false? (:ok result)) (pr-str result))
        (is (= "alias-migration-receipt-dir-in-control-directory"
               (:error_type result)))
        (is (true? (:source_unchanged result)))
        (is (empty? (filter (fn [^java.io.File file]
                              (str/ends-with? (.getName file) ".edn"))
                            (or (seq (.listFiles heads)) [])))
            "a receipt was published into git's ref namespace")
        (is (zero? (:exit (shell/sh "git" "show-ref" :dir path)))
            "the run left git unable to read its own refs"))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-056
(deftest a-relative-receipt-directory-that-escapes-through-a-link-refuses
  ;; A relative receipt directory is written against the workspace and reads as
  ;; a place inside it. `toRealPath` resolves a symlink component before any
  ;; lexical normalisation runs, so a link that points out of the tree carries
  ;; the receipts somewhere the caller never named, silently.
  (let [workspace (workspace!)
        outside (temp-dir)
        link-name (str "outside-link-" (java.util.UUID/randomUUID))
        link (io/file workspace link-name)
        relative (str link-name "/receipts")
        ;; the old resolution read a relative path against this process's
        ;; working directory, so a red run creates it there; it is cleaned up
        ;; whichever way the run goes
        cwd-junk (io/file (System/getProperty "user.dir") link-name)]
    (try
      (Files/createSymbolicLink (.toPath link) (.toPath outside)
                                (make-array FileAttribute 0))
      (let [result (alias-migration/execute!
                     {:project-root (.getPath workspace)
                      :receipt-dir relative}
                     (request workspace))]
        (is (false? (:ok result)) (pr-str result))
        (is (= "alias-migration-receipt-dir-escapes" (:error_type result)))
        (is (true? (:source_unchanged result)))
        (is (not (.exists (io/file outside "receipts")))
            "the refusal created the directory it refused to write in"))
      (finally
        (delete-tree! cwd-junk)
        (delete-tree! workspace)
        (delete-tree! outside)))))

;; @spec MCP-OP-ALIAS-056
(deftest an-absolute-receipt-directory-outside-the-workspace-stays-legal
  ;; The default receipt directory lives under the user's state root, outside
  ;; every workspace. A restriction that kept receipts inside the tree would
  ;; refuse the configuration the server ships with.
  (let [workspace (workspace!)
        outside (temp-dir)
        receipts (io/file outside "receipts")]
    (.mkdirs receipts)
    (try
      (let [result (alias-migration/execute! (config workspace receipts)
                                             (request workspace))]
        (is (:ok result) (pr-str result))
        (is (true? (:committed result)))
        (is (= (.getCanonicalPath receipts)
               (.getCanonicalPath (.getParentFile (io/file (:undo_receipt result)))))
            "the receipt was not published in the directory the caller named"))
      (finally
        (delete-tree! workspace)
        (delete-tree! outside)))))

;; ---------------------------------------------------------------------------
;; round nine: the two secondary fields a post-write refusal also owes

;; @spec MCP-OP-ALIAS-056
(deftest a-post-write-refusal-says-a-mutation-was-attempted
  ;; `mutation_attempted` is the one field whose entire purpose is "did we
  ;; touch the tree", and the shared `refusal` helper hardcoded it to `false`
  ;; for every refusal this verb publishes. A post-write refusal is published
  ;; over twelve files the transaction kernel wrote, and the same verb already
  ;; answers `true` for the identical tree state on its heap-exhaustion path
  ;; (ALIAS-047), from the kernel's own write boundary. One verb, one answer:
  ;; a caller that gates its retry on `mutation_attempted` was told to retry
  ;; over a mid-migration workspace.
  (let [workspace (workspace!)
        details (io/file workspace ".clj-surgeon" "alias-migration")
        receipts (io/file workspace "receipts")]
    (.mkdirs details)
    (try
      (testing "a rollback that failed leaves the tree migrated"
        (let [result (post-write-redirect!
                       workspace details receipts
                       (fn [_] {:ok false :error "injected rollback failure"}))]
          (is (false? (:ok result)) (pr-str result))
          (is (= "post-write" (:phase result)))
          (is (= 12 (count (still-migrated workspace)))
              "the scenario did not leave a migrated tree")
          (is (true? (:mutation_attempted result))
              "twelve files were written and mutation_attempted said false")))
      (finally
        (delete-tree! workspace)))
    (let [workspace (workspace!)
          details (io/file workspace ".clj-surgeon" "alias-migration")
          receipts (io/file workspace "receipts")]
      (.mkdirs details)
      (try
        (testing "and a rollback that SUCCEEDED still attempted the mutation"
          ;; the kernel wrote every file and then put it back. The tree is
          ;; unchanged and `source_unchanged` says so; the question
          ;; `mutation_attempted` answers is a different one, and the honest
          ;; answer is that the write boundary was crossed.
          (let [result (post-write-redirect! workspace details receipts nil)]
            (is (false? (:ok result)) (pr-str result))
            (is (true? (:source_unchanged result)))
            (is (empty? (still-migrated workspace)))
            (is (true? (:mutation_attempted result))
                "the kernel wrote every file before the rollback restored it")))
        (finally
          (delete-tree! workspace))))))

;; @spec MCP-OP-ALIAS-056
(deftest a-pre-write-refusal-still-says-no-mutation-was-attempted
  ;; The other half of the same claim: `mutation_attempted` is READ from the
  ;; kernel's write boundary, so making it honest for a post-write refusal must
  ;; not turn it into a constant `true`. A refusal decided on paths alone, with
  ;; no byte written, answers `false` and sends the caller back to correct its
  ;; request.
  (let [workspace (workspace!)
        heads (io/file workspace ".git" "refs" "heads")]
    (.mkdirs heads)
    (try
      (let [result (alias-migration/execute! (config workspace heads)
                                             (request workspace))]
        (is (false? (:ok result)) (pr-str result))
        (is (= "alias-migration-receipt-dir-in-control-directory"
               (:error_type result)))
        (is (true? (:source_unchanged result)))
        (is (false? (:mutation_attempted result)))
        (is (= "correct_request" (:next_action result)))
        (is (empty? (still-migrated workspace))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-056
(deftest a-mid-migration-refusal-sends-the-caller-to-the-receipt-not-a-retry
  ;; The refusal's own `remedy` reads "The tree is MID-MIGRATION: undo it by
  ;; hand from the receipt named in receipt_file" while `next_action` told an
  ;; automated caller to re-send a corrected request. Re-sending an alias
  ;; migration over twelve already-migrated files is precisely the wrong next
  ;; action, and `review_receipt` is this verb's own value for that state —
  ;; already published by the heap guard (ALIAS-047) and by a committed
  ;; transaction whose receipt did not land.
  (let [workspace (workspace!)
        details (io/file workspace ".clj-surgeon" "alias-migration")
        receipts (io/file workspace "receipts")]
    (.mkdirs details)
    (try
      (let [result (post-write-redirect!
                     workspace details receipts
                     (fn [_] {:ok false :error "injected rollback failure"}))]
        (is (false? (:ok result)) (pr-str result))
        (is (= 12 (count (still-migrated workspace))))
        (is (str/includes? (str (:remedy result)) "MID-MIGRATION")
            (pr-str (:remedy result)))
        (is (= "review_receipt" (:next_action result))
            "the structured field told the caller to retry over a migrated tree"))
      (finally
        (delete-tree! workspace)))
    (let [workspace (workspace!)
          details (io/file workspace ".clj-surgeon" "alias-migration")
          receipts (io/file workspace "receipts")]
      (.mkdirs details)
      (try
        (testing "and a rollback that RESTORED the tree does not point at a
                  receipt it deleted"
          ;; `next_action` follows `source_unchanged`, not
          ;; `mutation_attempted`. A successful rollback deletes the orphan
          ;; receipt — sending the caller to review a file that no longer
          ;; exists is a false pointer, and the tree it holds is safe to send
          ;; another request over.
          (let [result (post-write-redirect! workspace details receipts nil)]
            (is (false? (:ok result)) (pr-str result))
            (is (empty? (still-migrated workspace)))
            (is (true? (:source_unchanged result)))
            (is (true? (:mutation_attempted result)))
            (is (= "correct_request" (:next_action result)))))
        (finally
          (delete-tree! workspace))))))

;; @spec MCP-OP-ALIAS-056
(deftest a-partial-rollback-counts-the-files-it-left-migrated
  ;; `files_still_migrated` was `(if rolled-back? 0 (count files))` — the
  ;; PLAN's file count, never a measurement. `commit-compiled!` answers per
  ;; file when its writes fail, in `:recovery`, and `rollback-report` threw
  ;; that answer away, so a rollback that left six of twelve files migrated
  ;; published twelve. The number is the one a human uses to decide what to
  ;; undo by hand.
  ;;
  ;; The `:recovery` map is about the UNDO transaction, not the migration.
  ;; Recovery restores each file to the state that transaction READ, which for
  ;; an undo is the MIGRATED content — so `:restored` and `:original` mean
  ;; STILL MIGRATED, and only `:restore-failed` (the undo rewrote the file and
  ;; recovery could not put it back) leaves pre-migration bytes on disk. The
  ;; injection below matches the tree to the map it returns.
  (let [workspace (workspace!)
        details (io/file workspace ".clj-surgeon" "alias-migration")
        receipts (io/file workspace "receipts")]
    (.mkdirs details)
    (try
      (let [result (post-write-redirect!
                     workspace details receipts
                     (fn [_]
                       (let [changed (vec (still-migrated workspace))
                             undone (vec (take 6 changed))
                             kept (vec (drop 6 changed))]
                         (doseq [relative undone]
                           (spit (io/file workspace relative)
                                 (get (:pre corpus) relative)))
                         {:error (str "Transaction write failed; manual "
                                      "recovery required")
                          :error-type :transaction-recovery-required
                          :rolled-back false
                          :recovery
                          (into (mapv (fn [file]
                                        {:file file :status :restore-failed})
                                      undone)
                                (mapv (fn [file]
                                        {:file file :status :restored})
                                      kept))})))
            migrated (still-migrated workspace)]
        (is (false? (:ok result)) (pr-str result))
        (is (= "post-write" (:phase result)))
        (is (= 6 (count migrated))
            (str "the injection did not leave six migrated files: "
                 (vec migrated)))
        (is (= 6 (:files_still_migrated result))
            "the published count is the plan's file count, not a measurement")
        (is (= (count migrated) (:files_still_migrated result))
            (str "the published count disagrees with the tree: " (vec migrated)))
        (is (= 6 (:files_restored result))
            "the rollback's own per-file answers were discarded")
        (is (false? (:rolled_back result)))
        (is (false? (:source_unchanged result)))
        (is (str/includes? (str (:error result)) "6 files remain migrated")
            (str "the prose counts the plan, not the tree: " (:error result))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-056
(deftest a-rollback-with-no-per-file-answer-still-over-states-the-migration
  ;; The safe direction, pinned. A rollback that fails without a `:recovery`
  ;; map has told this verb nothing per file, and an unmeasured file is
  ;; counted as still migrated: over-stating the work a human has left to do
  ;; costs a wasted look, under-stating it loses a file.
  (let [workspace (workspace!)
        details (io/file workspace ".clj-surgeon" "alias-migration")
        receipts (io/file workspace "receipts")]
    (.mkdirs details)
    (try
      (let [result (post-write-redirect!
                     workspace details receipts
                     (fn [_] {:ok false :error "injected rollback failure"}))]
        (is (= 12 (:files_still_migrated result)))
        (is (= 0 (:files_restored result)))
        (is (= 12 (count (still-migrated workspace)))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-056
(deftest the-kernels-rolled-back-key-is-the-undos-answer-not-the-migrations
  ;; A REAL undo, failed at its seventh write, with no field of it faked.
  ;;
  ;; `commit-compiled!` mints `{:error "Transaction write failed; all files
  ;; restored" :rolled-back true}` — and no `:ok` — when the transaction IT was
  ;; running got put back. The transaction it is running here is the UNDO, and
  ;; recovery restores each file to the state that transaction READ, which is
  ;; the MIGRATED content. So `:rolled-back true` on an `execute-undo!` result
  ;; means the undo was reversed and the alias migration is STILL IN PLACE.
  ;;
  ;; Reading it as the migration's own answer — publishing `rolled_back true`,
  ;; `files_still_migrated 0`, `source_unchanged true` — would report a
  ;; restored tree over twelve migrated files, which is the single false green
  ;; this whole requirement family exists to forbid. `rolled-back?` therefore
  ;; reads the undo's `:ok`, and the tree below is the authority on why.
  (let [workspace (workspace!)
        receipts (io/file workspace "receipts")]
    (.mkdirs receipts)
    (try
      (let [committed (alias-migration/execute! (config workspace receipts)
                                                (request workspace))
            _ (is (:ok committed) (pr-str committed))
            _ (is (= 12 (count (still-migrated workspace)))
                  "the fixture migration did not change twelve files")
            real-write file-ops/atomic-write!
            writes (atom 0)
            rollback (with-redefs [file-ops/atomic-write!
                                   (fn [file text]
                                     (when (= 7 (swap! writes inc))
                                       (throw (java.io.IOException.
                                                "injected undo write failure")))
                                     (real-write file text))]
                       (transaction/execute-undo!
                         {:receipt (:undo_receipt committed)}))]
        (testing "the kernel mints the shape the reviewer read as a rollback"
          (is (nil? (:ok rollback)) (pr-str rollback))
          (is (true? (:rolled-back rollback)) (pr-str rollback))
          (is (str/includes? (str (:error rollback)) "all files restored")
              (pr-str (:error rollback))))
        (testing "and the tree it describes is the MIGRATED one"
          (is (= 12 (count (still-migrated workspace)))
              (str "recovery restored the undo's own reads — the migrated "
                   "content — so every file is still migrated; "
                   ":rolled-back true is the undo's answer, not the "
                   "migration's"))
          (doseq [[relative expected] (:post corpus)]
            (when (contains? (:pre corpus) relative)
              (is (= expected (slurp (io/file workspace relative))) relative)))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-056
(deftest a-refusal-never-reads-the-undos-rolled-back-key-as-the-migrations
  ;; The verb-level pin for the shape proved above. An undo that comes back
  ;; `{:rolled-back true, :recovery all-restored}` with no `:ok` has left every
  ;; file MIGRATED, and the refusal must say so. This is the assertion that
  ;; makes `(or (:ok rollback) (:rolled-back rollback))` — the reading that
  ;; looks like a one-line fix for a false RED — impossible to land: it would
  ;; publish `rolled_back true`, `files_still_migrated 0` and
  ;; `source_unchanged true` over twelve migrated files.
  (let [workspace (workspace!)
        details (io/file workspace ".clj-surgeon" "alias-migration")
        receipts (io/file workspace "receipts")]
    (.mkdirs details)
    (try
      (let [result (post-write-redirect!
                     workspace details receipts
                     (fn [_]
                       {:error "Transaction write failed; all files restored"
                        :error-type :transaction-write-failed
                        :rolled-back true
                        :recovery (mapv (fn [file]
                                          {:file file :status :restored})
                                        (still-migrated workspace))}))
            migrated (still-migrated workspace)]
        (is (false? (:ok result)) (pr-str result))
        (is (= 12 (count migrated))
            "the undo left every file migrated and the scenario says otherwise")
        (is (false? (:rolled_back result))
            "the undo's own rollback was published as the migration's")
        (is (false? (:source_unchanged result))
            "twelve files are migrated and source_unchanged said true")
        (is (= 12 (:files_still_migrated result)))
        (is (= 0 (:files_restored result)))
        (is (= (count migrated) (:files_still_migrated result)))
        (is (true? (:mutation_attempted result)))
        (is (= "review_receipt" (:next_action result)))
        (is (str/includes? (str (:error result)) "rollback FAILED")
            (pr-str (:error result))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-056
(deftest a-control-directory-outside-the-workspace-is-not-called-the-workspaces
  ;; The guard that made this refusal reachable is precisely the one for a
  ;; control directory that is NOT the workspace's: a linked worktree's real
  ;; `.git` lives under the MAIN repository, outside the root. Calling it "the
  ;; workspace's .git" sends the reader to look in the wrong tree for a
  ;; directory the refusal has already named in `control_directory`.
  (let [workspace (workspace!)
        main (temp-dir)
        gitdir (io/file main ".git" "worktrees" "wt" "refs" "heads")]
    (.mkdirs gitdir)
    (try
      (let [result (alias-migration/execute! (config workspace gitdir)
                                             (request workspace))
            message (str (:error result))]
        (is (false? (:ok result)) (pr-str result))
        (is (= "alias-migration-receipt-dir-in-control-directory"
               (:error_type result)))
        (is (= ".git" (:control_directory result)))
        (is (not (str/includes? message "workspace's"))
            (str "the refusal calls another repository's control directory "
                 "the workspace's: " message))
        (is (str/includes? message ".git")
            (str "the refusal no longer names the directory it found: "
                 message)))
      (finally
        (delete-tree! workspace)
        (delete-tree! main)))))
