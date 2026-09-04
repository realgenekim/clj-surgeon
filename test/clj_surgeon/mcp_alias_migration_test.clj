(ns clj-surgeon.mcp-alias-migration-test
  (:require
   [cheshire.core :as json]
   [clj-surgeon.alias-migration :as planner]
   [clj-surgeon.alias-migration-fixture :as fixture]
   [clj-surgeon.file-ops :as file-ops]
   [clj-surgeon.intent-transaction :as transaction]
   [clj-surgeon.mcp-alias-migration :as alias-migration]
   [clj-surgeon.mcp-change-buffer :as change-buffer]
   [clj-surgeon.mcp-paths :as mcp-paths]
   [clj-surgeon.mcp-server]
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
  ([workspace] (execute! workspace {} {}))
  ([workspace overrides] (execute! workspace overrides {}))
  ([workspace overrides config-overrides]
   (let [receipt-dir (io/file workspace "receipts")]
     (.mkdirs receipt-dir)
     (alias-migration/execute! (merge (config workspace receipt-dir)
                                      config-overrides)
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

;; @spec MCP-OP-ALIAS-034
(def ^:private nine-shape-source
  "Every spelling of one migrated var, plus a live STRING naming it.

  The string is the subject: the verb does not rewrite it — it is an assertion
  about the codebase, not a code reference — and a receipt that reports zero
  where one exists hides exactly the work the caller still owes."
  (str "(ns acid.fanout.qshapes\n  (:require\n"
       "   [acid.fanout.store :as store]))\n\n"
       "(def a 'acid.fanout.store/find-event)\n"
       "(def b '[acid.fanout.store/find-event :marker])\n"
       "(def d '{:lookup acid.fanout.store/find-event})\n"
       "(def e `acid.fanout.store/find-event)\n"
       "(def g #'acid.fanout.store/find-event)\n"
       "(def h #'store/find-event)\n"
       "(def i `store/find-event)\n"
       "(def f \"acid.fanout.store/find-event\")\n"
       "(defn one [id] (store/find-event id))\n"))

;; @spec MCP-OP-ALIAS-034
(deftest string-mentions-are-counted-in-var-mode-and-name-their-site
  ;; Round-10 review finding 5: `(if (nil? from-var) (string-mentions …) [])`
  ;; made `string_mentions` a hardcoded zero for every var migration, against a
  ;; docstring that says "a silent zero would hide real work, so the count
  ;; travels in the receipt". Every E3-P receipt's `string_mentions: 0` was
  ;; vacuous. In var mode the retired thing is the VAR — the lib survives, its
  ;; other vars with it — so the string the receipt must find is the qualified
  ;; var name, not the lib name.
  (let [workspace (workspace!)]
    (try
      (spit (io/file workspace "src/acid/fanout/qshapes.clj") nine-shape-source)
      (let [result (execute! workspace {:expect {:files 13}})]
        (is (:ok result) (pr-str result))
        (is (= 1 (:string_mentions result))
            "a live string naming the retired var was reported as zero")
        (is (= ["src/acid/fanout/qshapes.clj:12"]
               (:string_mention_sites result))
            "the receipt does not name the site the caller must go read")
        (testing "the verb still does not rewrite it"
          (is (str/includes? (slurp (io/file workspace
                                             "src/acid/fanout/qshapes.clj"))
                             "\"acid.fanout.store/find-event\"")
              "the string literal was rewritten")))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-034
(deftest the-string-needle-follows-what-the-migration-retires
  ;; The needle is a function of the migration's own subject: a lib migration
  ;; retires the LIB, a var migration retires one qualified VAR, and the string
  ;; that names the surviving lib in a var migration is not stale work.
  (let [sources [{:file "src/a.clj"
                  :source (str "(ns a)\n(def x \"acid.fanout.store\")\n")}
                 {:file "src/b.clj"
                  :source (str "(ns b)\n\n"
                               "(def y \"acid.fanout.store/find-event\")\n")}]]
    (is (= ["src/a.clj:2"]
           (planner/string-mentions "acid.fanout.store" sources))
        "the lib needle no longer finds the bare lib string")
    (is (= ["src/b.clj:3"]
           (planner/string-mentions "acid.fanout.store/find-event" sources))
        "the var needle does not find the qualified var string")
    (is (= 2 (count (planner/string-mentions "acid.fanout.store"
                                             [{:file "src/c.clj"
                                               :source (str "(ns c)\n"
                                                            "(def p \"acid.fanout.store\")\n"
                                                            "(def q \"acid.fanout.store\")\n")}])))
        "two sites in one file were collapsed to one")))

;; @spec MCP-OP-ALIAS-034
(defn- mention-source
  "One namespace whose lines 2..(inc n) are each a string literal of `needle`."
  [ns-name needle n]
  (str "(ns " ns-name ")\n"
       (str/join "\n" (repeat n (str "(def q \"" needle "\")")))
       "\n"))

;; @spec MCP-OP-ALIAS-034
(deftest string-mention-sites-are-ordered-by-file-then-numeric-line
  ;; Round-11 re-review finding 4: the sites were sorted as `file:line`
  ;; STRINGS, so `src/z.clj:10` sorted before `src/z.clj:2` and a bound of 20
  ;; over 26 mentions kept lines 2, 3 and 10-27 and dropped 4-9. This is
  ;; round-10 finding 1 — a bound applied to the wrong ranking — reintroduced
  ;; inside round 11's own fix commit, and the docstring is what makes the
  ;; ordering load-bearing: "a file is where the caller must go, a line is
  ;; where they must look."
  ;;
  ;;   string_mentions (total) = 26
  ;;   lines actually named   = (2 3 10 11 ... 27)
  ;;   lines DROPPED          = (4 5 6 7 8 9)
  (let [needle (str fixture/from-lib "/" fixture/from-var)
        sources [{:file "src/b.clj" :source (mention-source "b" needle 13)}
                 {:file "src/a.clj" :source (mention-source "a" needle 13)}]
        sites (planner/string-mentions needle sources)
        expected (concat (map #(str "src/a.clj:" %) (range 2 15))
                         (map #(str "src/b.clj:" %) (range 2 9)))]
    (is (= 26 (count sites)) (pr-str sites))
    (is (= (vec expected)
           (vec (take alias-migration/max-string-mention-sites sites)))
        "the bounded list is not the first 20 sites in (file, numeric line) order")))

;; @spec MCP-OP-ALIAS-034
(deftest the-receipt-states-how-many-mention-sites-it-shows
  ;; The caller could only infer the truncation by comparing `string_mentions`
  ;; against `(count string_mention_sites)`. A bound that does not say it
  ;; fired is the silent-truncation class this branch has now paid for three
  ;; times.
  (let [workspace (workspace!)
        needle (str fixture/from-lib "/" fixture/from-var)]
    (try
      (write-tree! workspace
                   {"src/mention_a.clj"
                    (mention-source "mention-a" needle 13)
                    "src/mention_b.clj"
                    (mention-source "mention-b" needle 13)})
      (let [result (execute! workspace)]
        (is (:ok result) (pr-str result))
        (is (= 26 (:string_mentions result))
            "the total count is not exact")
        (is (= alias-migration/max-string-mention-sites
               (:string_mention_sites_shown result))
            "the receipt does not say how many of the sites it named")
        (is (= (count (:string_mention_sites result))
               (:string_mention_sites_shown result))
            "the shown count does not match the list it describes")
        (is (= (concat (map #(str "src/mention_a.clj:" %) (range 2 15))
                       (map #(str "src/mention_b.clj:" %) (range 2 9)))
               (:string_mention_sites result))
            "the receipt names sites the caller cannot walk in order"))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-034
(deftest only-string-literals-count-as-mention-sites
  ;; Round-11 re-review finding 6: the scan was `str/includes?` of the quoted
  ;; needle, line by line, with no reader, so a COMMENT naming the retired var
  ;; and a REGEX literal spelling it both counted as string-literal sites:
  ;;
  ;;   sites = ["src/z.clj:2" "src/z.clj:3" "src/z.clj:4"]
  ;;   comment line 2 counted? => true
  ;;   regex   line 3 counted? => true
  ;;
  ;; MCP-OP-ALIAS-034 says STRING LITERAL sites and publishes the count as
  ;; exact. A go-look-here list can afford a false positive; a count published
  ;; as exact cannot be something other than what the requirement names.
  (let [needle (str fixture/from-lib "/" fixture/from-var)
        source (str "(ns z)\n"
                    "; a comment mentioning \"" needle "\"\n"
                    "(def r #\"" needle "\")\n"
                    "(def s \"" needle "\")\n")
        sites (planner/string-mentions needle
                                       [{:file "src/z.clj" :source source}])]
    (is (= ["src/z.clj:4"] sites)
        "a comment or a regex literal was counted as a string literal")
    (testing "the string literal is still found where the others are absent"
      (is (= ["src/z.clj:2"]
             (planner/string-mentions
               needle
               [{:file "src/z.clj"
                 :source (str "(ns z)\n(def s \"" needle "\")\n")}]))))))


;; @spec MCP-OP-ALIAS-057
(deftest a-directory-entry-selects-the-same-subtree-under-every-spelling
  ;; Round-10 review finding 2: the directory CHECK resolved and normalised the
  ;; entry, and the emitted glob was built from the raw text, so a directory
  ;; that IS detected yielded a pattern that can never match. `./src` — as
  ;; natural a spelling as the one ALIAS-057 exists to accept — selected zero
  ;; files and was refused `scope-matches-nothing`.
  (let [workspace (workspace!)
        root (mcp-paths/real-root (.getPath workspace))
        select (fn [entry]
                 (set (alias-migration/expand-scope root {:paths [entry]
                                                          :exclude []})))
        baseline (select "src")]
    (try
      (is (= 23 (count baseline)) "the fixture's own source root moved")
      (testing "every spelling that normalises to `src` selects what `src` does"
        (doseq [entry ["./src" "src/." "src/" "src//" "./src/" ".///src"
                       "./src/." "src/./"]]
          (is (= baseline (select entry))
              (str "the entry " (pr-str entry)
                   " selected a different set than \"src\""))))
      (testing "a doubled separator inside the entry normalises too"
        (is (= (select "src/acid") (select "src//acid"))
            "`src//acid` selected a different set than `src/acid`")
        (is (seq (select "src//acid"))
            "`src//acid` selected nothing at all"))
      (testing "the root itself still contributes no subtree pattern"
        ;; ALIAS-057 is explicit that an entry naming the root contributes no
        ;; subtree, so the whole-tree spelling stays a typed refusal rather
        ;; than a silent whole-repository migration
        (is (empty? (select "."))
            "`.` widened the scope to the whole project root")
        (is (empty? (select "./"))
            "`./` widened the scope to the whole project root"))
      (testing "the widening never narrows: a literal glob keeps its matches"
        (is (= (select "src/acid/fanout/t01.clj")
               #{"src/acid/fanout/t01.clj"})
            "an exact file entry lost its match"))
      (testing "a normalised directory entry reaches discovery end to end"
        (let [result (execute! workspace {:scope {:paths ["./src"]}
                                          :expect {:files 12}})]
          (is (not= "alias-migration-scope-matches-nothing" (:error_type result))
              (str "`./src` was refused for matching nothing: "
                   (pr-str (select "./src"))))
          (is (:ok result) (pr-str result))))
      (finally
        (delete-tree! workspace)))))


;; @spec MCP-OP-ALIAS-058
(defn- many-root-workspace!
  "A tree of 118 Clojure sources under 9 top-level source roots.

  `src` holds 100 of them, so a remedy that ranks its roots alphabetically and
  keeps the first six drops the one root that holds five sixths of the tree."
  []
  (let [workspace (temp-dir)]
    (doseq [index (range 100)]
      (let [target (io/file workspace "src" "big"
                            (format "ns_%03d.clj" index))]
        (.mkdirs (.getParentFile target))
        (spit target (format "(ns big.ns-%03d)\n" index))))
    (doseq [[root n] [["r0" 3] ["r1" 3] ["r2" 2] ["r3" 2]
                      ["r4" 2] ["r5" 2] ["r6" 2] ["r7" 2]]
            index (range n)]
      (let [target (io/file workspace root (format "n%d.clj" index))]
        (.mkdirs (.getParentFile target))
        (spit target (format "(ns %s.n%d)\n" root index))))
    workspace))

;; @spec MCP-OP-ALIAS-058
(deftest the-derived-remedy-never-drops-a-source-root-in-silence
  ;; Round-10 review: `max-suggested-scope-paths` took 6 entries from a SORTED
  ;; SET, so truncation was alphabetical rather than by size. On a nine-root
  ;; tree the remedy named r0..r5, dropped `src/**` — the root holding 100 of
  ;; the 118 sources — and no field said a root had been dropped, while the
  ;; prose still called the list "the source roots this tree actually holds".
  ;; A remedy that silently selects a sixth of the tree is the same class of
  ;; defect as the refusal it replaced.
  (let [workspace (many-root-workspace!)
        receipt-dir (io/file workspace "receipts")]
    (.mkdirs receipt-dir)
    (try
      (let [result (alias-migration/execute!
                     (config workspace receipt-dir)
                     {:op "alias_migration"
                      :workspace_root (.getPath workspace)
                      :from {:lib fixture/from-lib :var fixture/from-var}
                      :to {:lib fixture/to-lib :var fixture/to-var
                           :alias_policy fixture/alias-policy}
                      :scope {:paths ["zzz/**"]}
                      :expect {:files 1}})]
        (is (= "alias-migration-scope-matches-nothing" (:error_type result))
            (pr-str result))
        (is (= 118 (:source_files_under_root result))
            "the walk did not see the whole tree")
        (testing "the receipt states how many of the tree's roots it listed"
          (is (= 9 (:source_roots result))
              "the receipt does not say how many source roots the walk saw")
          (is (= 6 (:roots_listed result))
              "the receipt does not say how many of them the remedy lists")
          (is (str/includes? (str (:remedy result)) "9")
              "the remedy does not state the truncation")
          (is (str/includes? (str (:remedy result)) "6")
              "the remedy does not state how many roots it named"))
        (testing "the biggest root is the one a bounded list keeps"
          (is (contains? (set (:suggested_paths result)) "src/**")
              "the remedy dropped the root holding 100 of the 118 sources"))
        (testing "the next_call selects the same file set the walk saw"
          (let [next-call (:next_call result)
                replayed (alias-migration/execute!
                           (config workspace receipt-dir)
                           (json/parse-string (json/generate-string next-call)
                                              true))]
            (is (= "alias-migration-empty-scope" (:error_type replayed))
                (pr-str replayed))
            (is (= 118 (:scanned_files replayed))
                "the remedy's next_call selects fewer files than the walk saw"))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-058
(deftest a-tree-inside-the-root-bound-lists-every-root-it-has
  ;; The other side of the bound: when the tree's roots fit, every one of them
  ;; is named, nothing is appended, and the two counts agree.
  (let [workspace (workspace!)
        receipt-dir (io/file workspace "receipts")]
    (.mkdirs receipt-dir)
    (try
      (let [result (execute! workspace {:scope {:paths ["srk/**"]}
                                        :expect {:files 12}})]
        (is (= "alias-migration-scope-matches-nothing" (:error_type result)))
        (is (= (:source_roots result) (:roots_listed result))
            "a tree inside the bound reported a truncation it did not make")
        (is (= (count (:suggested_paths result)) (:roots_listed result))
            "the listed count does not match the list")
        (is (not (contains? (set (:suggested_paths result)) "**"))
            "an untruncated remedy appended the completing pattern anyway"))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-058
(defn- long-root-workspace!
  "A tree whose six top-level source roots have ordinary but long names.

  Nothing here is hostile: six directories of fifty-two characters, one source
  apiece. The rescoping call composed from them is 539 characters, past the
  planner's 512-character `next_call` ceiling, so `rescoping-call` returns nil
  and the refusal publishes no call at all."
  []
  (let [workspace (temp-dir)]
    (doseq [index (range 6)]
      (let [target (io/file
                     workspace
                     (str "a-very-long-but-entirely-ordinary-source-root-name-"
                          index)
                     "one.clj")]
        (.mkdirs (.getParentFile target))
        (spit target (format "(ns root%d.one)\n" index))))
    workspace))

;; @spec MCP-OP-ALIAS-058
(defn- scope-matches-nothing-refusal
  "One `scope-matches-nothing` refusal over `workspace`, with the receipt dir made."
  ([workspace] (scope-matches-nothing-refusal workspace {}))
  ([workspace overrides]
   (let [receipt-dir (io/file workspace "receipts")]
     (.mkdirs receipt-dir)
     (alias-migration/execute!
       (config workspace receipt-dir)
       (merge {:op "alias_migration"
               :workspace_root (.getPath workspace)
               :from {:lib fixture/from-lib :var fixture/from-var}
               :to {:lib fixture/to-lib :var fixture/to-var
                    :alias_policy fixture/alias-policy}
               :scope {:paths ["zzz/**"]}
               :expect {:files 1}}
              overrides)))))

;; @spec MCP-OP-ALIAS-058
(deftest the-remedy-never-tells-the-caller-to-resend-a-call-it-did-not-publish
  ;; Round-11 review finding 1: on six top-level roots with ordinary long
  ;; names the composed call is 539 characters, `rescoping-call` returns nil
  ;; past its 512-character bound, and the receipt published `next_call nil`
  ;; beside a remedy that opened "Resend the next_call:" — two adjacent lines
  ;; of one text block telling the caller to resend a call and that no call
  ;; exists. The listing is what a bound may shrink; the SELECTION is kept
  ;; whole by the completing `**`, so a call that does not fit is a listing
  ;; that has not been shrunk far enough — and where no listing fits at all,
  ;; the receipt must say so, name the ceiling and the size, and hand the
  ;; caller the roots to choose from.
  (let [workspace (long-root-workspace!)
        receipt-dir (io/file workspace "receipts")]
    (try
      (let [result (scope-matches-nothing-refusal workspace)]
        (is (= "alias-migration-scope-matches-nothing" (:error_type result))
            (pr-str result))
        (is (= 6 (:source_files_under_root result)))
        (is (some? (:next_call result))
            "the remedy blew the next_call bound and published no call")
        (testing "the published call is the one the remedy claims"
          (let [replayed (alias-migration/execute!
                           (config workspace receipt-dir)
                           (json/parse-string
                             (json/generate-string (:next_call result)) true))]
            (is (= "alias-migration-empty-scope" (:error_type replayed))
                (pr-str replayed))
            (is (= 6 (:scanned_files replayed))
                "the published next_call selects fewer files than the walk saw"))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-058
(deftest a-remedy-with-no-composable-call-names-the-ceiling-and-the-roots
  ;; The other side of finding 1: a request carrying enough exclusions that
  ;; even the one-pattern call `["**"]` is past the ceiling. There is then no
  ;; call to shrink to, and the receipt must NOT say "Resend the next_call".
  ;; It must state that no call was composed, name the ceiling and the size
  ;; that missed it, and give the caller the roots — with the number of
  ;; sources each holds — to spell a scope from.
  (let [workspace (long-root-workspace!)]
    (try
      (let [exclusions (mapv #(str "a-very-long-but-entirely-ordinary-source-"
                                   "root-name-" % "/one.clj")
                             (range 6))
            result (scope-matches-nothing-refusal
                     workspace
                     {:scope {:paths ["zzz/**"] :exclude exclusions}})
            remedy (str (:remedy result))]
        (is (= "alias-migration-scope-matches-nothing" (:error_type result))
            (pr-str result))
        (is (nil? (:next_call result))
            "this fixture no longer exercises the no-composable-call branch")
        (is (not (str/includes? remedy "Resend the next_call"))
            "the remedy told the caller to resend a call the receipt has not got")
        (is (str/includes? remedy
                           (str planner/max-next-call-characters))
            "the remedy does not name the next_call ceiling it missed")
        (is (number? (:next_call_characters result))
            "the receipt does not say how long the call it could not publish is")
        (is (> (long (or (:next_call_characters result) 0))
               (long planner/max-next-call-characters))
            "the reported size does not exceed the ceiling it is said to miss")
        (is (str/includes? remedy
                           (str (or (:next_call_characters result) "<absent>")))
            "the remedy does not name the size that missed the ceiling")
        (is (seq (:source_root_sizes result))
            "the receipt hands the caller no roots to choose from")
        (is (every? #(re-find #"\(\d+\)$" %) (:source_root_sizes result))
            "the roots are listed without the number of sources each holds")
        (is (<= (count (:source_root_sizes result))
                alias-migration/max-suggested-scope-paths)
            "the root listing is not bounded"))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-051
;; @spec MCP-OP-ALIAS-058
(deftest the-derived-remedy-is-a-glob-the-parser-accepts-and-the-tree-matches
  ;; Round-11 review finding 2: `suggested-scope-paths` built `<dirname>/**`
  ;; from the RAW directory name, so a top-level directory whose name holds a
  ;; glob metacharacter — legal on POSIX — made the published correction
  ;; unexecutable. `a{b` produced `"a{b/**"`, which the same round's new
  ;; ALIAS-051 parser gate then refused; `[x]`, `{a,b}` and `*` parsed and
  ;; selected the WRONG set, which is worse, because nothing refuses it.
  ;;
  ;;   dir "a{b"    suggested=["a{b/**" "src/**"]
  ;;      UNPARSEABLE "a{b/**" -> "Missing '} near index 5"
  ;;      REPLAY error_type => "alias-migration-scope-path-refused"
  ;;   dir "[x]"    suggested=["[x]/**" "src/**"]   REPLAY selects 0 of its own
  ;;   dir "{a,b}"  suggested=["{a,b}/**" ...]      REPLAY selects 0 of its own
  ;;   dir "*"      suggested=["*/**" "src/**"]     matches every depth-2 path
  (doseq [dirname ["a{b" "[x]" "{a,b}" "*"]]
    (let [workspace (temp-dir)
          receipt-dir (io/file workspace "receipts")]
      (try
        (write-tree! workspace {(str dirname "/one.clj") "(ns one)\n"
                                "src/two.clj" "(ns two)\n"})
        (.mkdirs receipt-dir)
        (let [result (scope-matches-nothing-refusal workspace)
              root (mcp-paths/real-root (.getPath workspace))
              suggested (:suggested_paths result)
              derived (first (remove #{"src/**" "**"} suggested))]
          (is (= "alias-migration-scope-matches-nothing" (:error_type result))
              (pr-str result))
          (testing (str "every derived pattern parses · " (pr-str dirname))
            (doseq [pattern suggested]
              (is (nil? (#'alias-migration/glob-parse-error pattern))
                  (str "the remedy published a pattern the verb's own parser "
                       "gate refuses: " (pr-str pattern)))))
          (testing (str "the derived pattern selects that directory · "
                        (pr-str dirname))
            (is (some? derived)
                "the remedy did not derive a pattern for the metacharacter root")
            (is (= [(str dirname "/one.clj")]
                   (alias-migration/expand-scope root {:paths [derived]
                                                       :exclude []}))
                (str "the derived pattern " (pr-str derived)
                     " does not select the directory it was derived from")))
          (testing (str "the published call replays · " (pr-str dirname))
            (let [replayed (alias-migration/execute!
                             (config workspace receipt-dir)
                             (json/parse-string
                               (json/generate-string (:next_call result)) true))]
              (is (not= "alias-migration-scope-path-refused"
                        (:error_type replayed))
                  (str "the published remedy earned a second refusal from the "
                       "verb's own parser gate: " (pr-str replayed)))
              (is (= 2 (:scanned_files replayed))
                  (str "the published remedy selects fewer files than the walk "
                       "saw: " (pr-str replayed))))))
        (finally
          (delete-tree! workspace))))))

;; @spec MCP-OP-ALIAS-059
(deftest a-caller-sized-scope-entry-cannot-grow-the-refusal-text
  ;; Round-11 re-review finding 5: `max-refusal-fact-characters` bounds the
  ;; rendered `facts ·` line, but `:error` and `:remedy` are ENVELOPE keys and
  ;; are rendered whole, and both embed `:path`, `:pattern` and `:cause`
  ;; verbatim. The glob parser echoes the pattern twice, so a 10,001-character
  ;; entry becomes a 20,031-character cause before the two sentences quote it
  ;; again:
  ;;
  ;;   error_type   => "alias-migration-scope-path-refused"
  ;;   path length  = 10001  pattern length = 10001  cause length = 20031
  ;;   error length = 30141  remedy length = 20298
  ;;   TEXT BLOCK LENGTH = 51191
  ;;
  ;; ALIAS-059 asks for a text "bounded in count and in per-field length"; the
  ;; bound read on the size of the TREE and not on the size of the CALLER'S
  ;; OWN INPUT, which is the one thing in a refusal that no ceiling upstream
  ;; of it constrains.
  (let [workspace (temp-dir)]
    (try
      (write-tree! workspace {"src/two.clj" "(ns two)\n"})
      (testing "a 10,001-character malformed entry"
        (let [entry (str (apply str (repeat 10000 "a")) "{")
              result (scope-matches-nothing-refusal
                       workspace {:scope {:paths [entry]}})
              text (mcp-tool/alias-migration-summary
                     (assoc result :elapsed_ms 1.0))]
          (is (= "alias-migration-scope-path-refused" (:error_type result))
              (pr-str (:error_type result)))
          (doseq [field [:path :pattern :cause]]
            (is (<= (count (str (get result field))) 256)
                (str "the refusal field " field " is "
                     (count (str (get result field)))
                     " characters of caller input"))
            (is (re-find #"\[elided, \d+ characters\]$"
                         (str (get result field)))
                (str "the field " field
                     " is not elided with the length it replaced")))
          (doseq [field [:path :pattern]]
            (is (str/includes? (str (get result field)) "10001")
                (str "the elided field " field
                     " does not name the caller's own 10,001 characters")))
          (is (str/includes? (str (:cause result)) "20031")
              "the elided cause does not name its own length")
          (is (<= (count (str (:error result))) 1024)
              (str "the error sentence is " (count (str (:error result)))
                   " characters"))
          (is (<= (count (str (:remedy result))) 1024)
              (str "the remedy is " (count (str (:remedy result)))
                   " characters"))
          (is (<= (count text) 4096)
              (str "the refusal text block is " (count text)
                   " characters, past the stated 4096-character ceiling"))))
      (testing "a 10,001-character entry that parses and matches nothing"
        (let [entry (apply str (repeat 10001 "a"))
              result (scope-matches-nothing-refusal
                       workspace {:scope {:paths [entry]}})
              text (mcp-tool/alias-migration-summary
                     (assoc result :elapsed_ms 1.0))]
          (is (= "alias-migration-scope-matches-nothing" (:error_type result))
              (pr-str (:error_type result)))
          (is (every? #(<= (count %) 256) (:paths result))
              "the caller's own paths ride the refusal at full length")
          (is (<= (count text) 4096)
              (str "the refusal text block is " (count text)
                   " characters, past the stated 4096-character ceiling"))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-051
(deftest a-nul-byte-in-a-scope-path-is-a-typed-refusal
  ;; Round-11 re-review finding 8: `scope-glob-patterns` skips the subtree
  ;; derivation for an entry containing a NUL, and `getPathMatcher` accepts
  ;; one, so the entry selected nothing and the caller was handed
  ;; `scope-matches-nothing` — a refusal about the TREE — for a spelling cause
  ;; they cannot see, a byte that prints as nothing at all.
  ;;
  ;;   === P3b: NUL entry through scan-scope ===
  ;;   {:ok true, :files []}      ; entry "src/ x"
  ;;   {:ok true, :files []}      ; entry " "
  ;;
  ;; No escape and no data loss, so this refuses closed either way; but the
  ;; cause a caller cannot see is the one the refusal has to name.
  (let [workspace (temp-dir)]
    (try
      (write-tree! workspace {"src/two.clj" "(ns two)\n"})
      (doseq [[label entry index] [["inside a path" (str "src/" (char 0) "x") 4]
                                   ["alone" (str (char 0)) 0]
                                   ["trailing" (str "src" (char 0)) 3]]]
        (testing label
          (let [result (scope-matches-nothing-refusal
                         workspace {:scope {:paths [entry]}})
                cause (str (:cause result))]
            (is (= "alias-migration-scope-path-refused" (:error_type result))
                (str "a NUL byte was reported as a fact about the tree: "
                     (pr-str (:error_type result))))
            (is (str/includes? cause "NUL")
                (str "the refusal does not name the byte: " (pr-str cause)))
            (is (str/includes? cause "U+0000")
                (str "the refusal does not name the byte's code point: "
                     (pr-str cause)))
            (is (str/includes? cause (str "index " index))
                (str "the refusal does not say where the byte is: "
                     (pr-str cause)))
            (is (true? (:source_unchanged result)))
            (is (nil? (:next_call result))
                "a spelling only the caller knows was given a mechanical call"))))
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


;; ---------------------------------------------------------------------------
;; the refusal TEXT block carries what the structured refusal carries

;; @spec MCP-OP-ALIAS-059
(def ^:private refusal-envelope-keys
  "Receipt keys the refusal text renders structurally rather than as facts.

  Kept in step with `mcp-tool/alias-migration-refusal-envelope-keys`: a key
  listed here is a key `assert-refusal-text!` does not require by name, so a
  key that drifts into this set drops out of the gate with it."
  #{:ok :operation :error_type :error :next_call :remedy :elapsed_ms
    :workspace_root :receipt_hash :undo_receipt :details_path
    :details_retained :details_retention})

;; @spec MCP-OP-ALIAS-059
(defn- namespace-source-path
  [namespace-name]
  (str "src/"
       (str/replace (str/replace (str namespace-name) "." "/") "-" "_")
       ".clj"))

;; @spec MCP-OP-ALIAS-059
(defn- required-clj-surgeon-namespaces
  "The clj-surgeon namespaces one source `:require`s, read with the reader."
  [namespace-name]
  (let [path (namespace-source-path namespace-name)]
    (when (.exists (io/file path))
      (let [node (->> (n/children (parser/parse-string-all (slurp path)))
                      (filter #(= :list (n/tag %)))
                      first)
            form (try (n/sexpr node) (catch Exception _ nil))]
        (->> form
             (filter seq?)
             (filter #(= :require (first %)))
             (mapcat rest)
             (map #(if (sequential? %) (first %) %))
             (filter symbol?)
             (filter #(str/starts-with? (str %) "clj-surgeon.")))))))

;; @spec MCP-OP-ALIAS-059
(defn- reachable-entrance-namespaces
  "Every clj-surgeon namespace the alias_migration entrance can refuse THROUGH.

  Closed under `:require` from the verb's own two namespaces and from
  `mcp_operation`, which `handle-alias-migration` calls directly. Derived
  rather than listed, because round twelve's enumeration derived its KINDS
  from a hand-written list of six SOURCES and missed
  `:invalid-diagnostic-output` — minted by `mcp_change_buffer`, which the verb
  requires, and forwarded verbatim by the verb itself.

  `mcp_tool` is the one namespace held out of the closure: it is the shared
  router that hosts every verb, so closing over it would reach every verb in
  the server. It keeps the prefix filter it has always had.

  Over-approximation is the SAFE direction here. A kind in the set that the
  entrance cannot actually reach costs one renderer assertion; a kind missing
  from the set is the defect this witness exists to catch."
  []
  (loop [pending '[clj-surgeon.mcp-alias-migration
                   clj-surgeon.alias-migration
                   clj-surgeon.mcp-operation]
         seen #{}]
    (if-let [candidate (first pending)]
      (if (or (contains? seen candidate)
              (= 'clj-surgeon.mcp-tool candidate))
        (recur (vec (rest pending)) seen)
        (recur (into (vec (rest pending))
                     (required-clj-surgeon-namespaces candidate))
               (conj seen candidate)))
      seen)))

;; @spec MCP-OP-ALIAS-059
(defn- node-seq
  "Every node of a parsed source, tolerant of nodes with no children."
  [node]
  (tree-seq #(try (seq (n/children %)) (catch Exception _ nil))
            #(try (n/children %) (catch Exception _ nil))
            node))

;; @spec MCP-OP-ALIAS-059
(defn- node-value
  [node]
  (try (n/sexpr node) (catch Exception _ nil)))

;; @spec MCP-OP-ALIAS-059
(defn- significant-children
  [node]
  (remove n/whitespace-or-comment? (n/children node)))

;; @spec MCP-OP-ALIAS-059
(defn- top-level-def-forms
  "Every top-level `def…` form of one source, by the name it defines."
  [text]
  (->> (n/children (parser/parse-string-all text))
       (filter #(= :list (n/tag %)))
       (keep (fn [node]
               (let [kids (significant-children node)
                     head (node-value (first kids))
                     named (when (and (symbol? head)
                                      (str/starts-with? (str head) "def"))
                             (node-value (second kids)))]
                 (when (symbol? named)
                   [(str named) (try (n/string node) (catch Exception _ ""))]))))
       (into {})))

;; @spec MCP-OP-ALIAS-059
(defn- router-entrance-slice
  "The part of the shared router the alias_migration ENTRANCE can reach.

  Round-thirteen review finding 3: `mcp_tool` was held OUT of the closed
  require graph, which is exactly the future-regression hole the requirement
  says is closed — the reviewer added a `defmulti`/`defmethod` to `mcp_tool`,
  routed it from `handle-alias-migration`, and composed the kind at runtime;
  all four enumeration witnesses stayed green while the live entrance returned
  a kind absent from the set.

  A file is the wrong unit. The right one is a code path: the top-level forms
  of `mcp_tool.clj` transitively referenced by `handle-alias-migration`. That
  is reachability BY CONSTRUCTION — in the require graph AND on a path the
  entrance can take — and it is a strict subset, so the router's other verbs'
  kinds stay out of the set on their own merits rather than by a hold-out."
  []
  (let [forms (top-level-def-forms (slurp "src/clj_surgeon/mcp_tool.clj"))]
    (loop [pending ["handle-alias-migration"] seen #{}]
      (if-let [candidate (first pending)]
        (if (or (contains? seen candidate) (not (contains? forms candidate)))
          (recur (vec (rest pending)) seen)
          (recur (into (vec (rest pending))
                       (filter forms
                               (map second
                                    (re-seq #"[^\w!?*<>=+/.-]([a-zA-Z][\w!?*<>=+.-]*)"
                                            (forms candidate)))))
                 (conj seen candidate)))
        (apply str (map forms (sort seen)))))))

;; @spec MCP-OP-ALIAS-059
(defn- head-position-keywords
  "Keywords used as functions — `(:ok checks)` — which name a FIELD, not a kind."
  [node]
  (set (for [candidate (node-seq node)
             :when (= :list (n/tag candidate))
             :let [head (node-value (first (significant-children candidate)))]
             :when (keyword? head)]
         (name head))))

;; @spec MCP-OP-ALIAS-059
(defn- literal-kind
  [node]
  (let [value (node-value node)]
    (cond (keyword? value) (name value)
          (string? value) value)))

;; @spec MCP-OP-ALIAS-059
(defn- minted-kinds-in
  "The kinds a non-literal `:error-type` value can still MINT.

  Only KEYWORD literals, and only outside function position. A keyword is a
  whole kind — `(if … :no-match :ambiguous-match)` mints two — while a string
  inside a composition is a FRAGMENT: the reviewer's `(str \"heldout-\"
  \"protocol-kind\")` mints one kind that appears nowhere in the source, and
  reading its pieces as kinds would enumerate two names that do not exist and
  miss the one that does. `keyword` is a composer for the same reason `str`
  is — `(keyword \"alias-migration-\" (name x))` mints a kind from a runtime
  name and a literal PREFIX, and reading that prefix on its own as a minted
  kind is exactly the fragment mistake this guard exists to avoid. A site
  that mints no keyword spells its kind entirely at runtime and must declare
  that it is forwarding."
  [node]
  (let [heads (head-position-keywords node)
        composed (set (for [candidate (node-seq node)
                            :when (= :list (n/tag candidate))
                            :let [head (node-value
                                         (first (significant-children candidate)))]
                            :when (contains? '#{str format name subs keyword} head)
                            piece (node-seq candidate)]
                        piece))]
    (->> (node-seq node)
         (remove composed)
         (keep (fn [candidate]
                 (let [value (node-value candidate)]
                   (cond (keyword? value) (name value)
                         (string? value) value))))
         (remove heads)
         (filter #(re-matches #"[a-z][a-z0-9-]*" %))
         set)))

;; @spec MCP-OP-ALIAS-059
(defn- error-type-value-sites
  "Every `:error-type`/`:error_type` map entry of a source, with its value node."
  [text]
  (for [top-level (significant-children (parser/parse-string-all text))
        candidate (node-seq top-level)
        :when (= :map (n/tag candidate))
        [key-node value-node] (partition 2 (significant-children candidate))
        :let [key-value (node-value key-node)]
        :when (contains? #{:error-type :error_type} key-value)]
    ;; @spec MCP-OP-ALIAS-059
    ;; the enclosing TOP-LEVEL form travels with the site: a bare symbol
    ;; forwards or mints according to what it was bound to, and that binding
    ;; lives outside the value expression
    {:value value-node
     :context top-level
     :literal (literal-kind value-node)
     :text (try (n/string value-node) (catch Exception _ "<unprintable>"))}))

;; @spec MCP-OP-ALIAS-059
(def ^:private kind-forwarding-heads
  "The only call heads a FORWARDED kind expression may use.

  Round-fifteen review finding 2. Every one of these SELECTS or RELAYS a value
  that already exists — `or`/`and`/`if`/`when` choose among candidates, the
  threading macros and `get`/`get-in` walk to one, `some`/`first`/`second`/
  `filter`/`remove`/`keep`/`seq` pick one out of a sequence of incoming
  refusals (`(some :error-type (remove :ok checks))` is the change buffer's
  own forward), and `name` reads the name of a kind it was handed. None of
  them can BUILD a name out of request data, which is exactly what `keyword`,
  `symbol`, `str`, `format` and `subs` do — and what the reviewer's planted
  `(keyword (:review_dynamic_kind params))` did under a marker that never
  looked. An allowlist and not a denylist, for the same reason the renderer's
  scalar allowlist is one: a denylist admits everything nobody thought of."
  '#{or and if when if-let when-let let
     -> ->> some-> some->>
     get get-in name
     some first second filter remove keep seq})

;; @spec MCP-OP-ALIAS-059
(def ^:private kind-minting-symbols
  "The functions that BUILD a name out of data, wherever they appear.

  Round-sixteen review finding 1: `kind-forwarding-heads` is an allowlist of
  CALL HEADS, and a call head is not where a threading macro puts its
  functions. `(some-> kind name keyword)` has exactly one list, whose head is
  `some->` — an allowed selecting head — and the minting `keyword` is a bare
  symbol in argument position that the head test never looked at. So the
  minting side is read as a set of SYMBOLS matched anywhere in the expression,
  not as a set of heads: a name-builder is a mint whether it is called
  directly, threaded into, or passed to something else."
  '#{keyword symbol str format subs})

;; @spec MCP-OP-ALIAS-059
(def ^:private keyword-selector-heads
  "Heads whose keyword ARGUMENTS are selectors rather than kind sources.

  `(some :error-type (remove :ok checks))` — the change buffer's own forward —
  passes two keywords as FUNCTIONS: each names a field to read out of an
  incoming refusal and can mint no name. Everywhere else a keyword literal is
  a value the expression can hand back as the kind, which is exactly how a
  literal table mints invisibly: `(get {:a :brand-new-kind} kind)` selects,
  and the thing it selects is a fresh keyword no `(refusal :kw` scan can see."
  '#{some first second filter remove keep seq map mapcat})

;; @spec MCP-OP-ALIAS-059
(defn- mint-evidence
  "Every reason `node` MINTS a kind rather than forwarding one, form-DEEP.

  Two shapes, both read at every depth and in every position:

  1. a symbol from `kind-minting-symbols` ANYWHERE — head, argument, or
     threaded — because a name-builder builds a name from whichever position
     it is called; and
  2. a keyword OR STRING literal that is not a lookup head and not a
     keyword-as-function argument of a `keyword-selector-heads` call, because
     such a literal is a value the expression can return as the kind. A
     literal table's entries are exactly this shape, and `get`/`get-in`
     against one was exempt while the kind it yields appeared in no source
     scan.

  Round-seventeen review finding 2: shape 2 tested `keyword?` alone. Every
  `refusal` constructor in the reachable set forwards its kind through
  `(name error-type)`, and `(name \"brand-new-kind\")` is `\"brand-new-kind\"` —
  a string kind is a whole kind, and the reviewer drove one live through the
  entrance from a marked `(get {\"a\" \"brand-new-kind\"} k)`. The blindness hid
  itself in both directions, `minted-kinds-in` having read strings all along:
  the marked site was exempt, so the enumeration read nothing from it, and it
  minted a READABLE kind, so the runtime-spelled scan did not name it either.

  Returned as reasons rather than as a boolean so a witness that fails can say
  WHICH shape it found."
  ([node] (mint-evidence node nil false false))
  ([node parent-head head? selector?]
   (let [value (node-value node)
         list? (= :list (n/tag node))
         kids (try (when (seq (n/children node)) (significant-children node))
                   (catch Exception _ nil))
         child-head (when list? (node-value (first kids)))]
     (concat
       (when (and (symbol? value) (contains? kind-minting-symbols value))
         [(str "mints with `" value "`")])
       (when (and (or (keyword? value) (string? value))
                  (not head?)
                  (not selector?)
                  (not (contains? keyword-selector-heads parent-head)))
         [(str "reads the literal kind source `" value "`")])
       (mapcat (fn [index child]
                 (mint-evidence child
                                (when list? child-head)
                                (and list? (zero? index))
                                ;; @spec MCP-OP-ALIAS-059
                                ;; a `get`/`get-in` KEY or PATH names a field
                                ;; to read out of an incoming map and can mint
                                ;; nothing — `(get-in data [:admission
                                ;; :error-type])` is mcp_cold_verify's own
                                ;; forward. The COLLECTION argument is not a
                                ;; selector: a keyword inside it is a value the
                                ;; expression can hand back as the kind, which
                                ;; is exactly how a literal table mints.
                                (or selector?
                                    (and list?
                                         (contains? '#{get get-in} child-head)
                                         (= 2 index)))))
               (range)
               (or kids []))))))

;; @spec MCP-OP-ALIAS-059
(defn- binding-values-in
  "Every `let`-style binding of `context`, by the symbol it binds.

  Round-sixteen review finding 1's third shape: a kind LITERAL bound one line
  above the site and relayed as a bare symbol. A bare symbol is the purest
  forwarding shape there is — `authority-error` in `mcp_cold_verify` is one —
  so the symbol cannot be judged on its own; it is judged on what it was bound
  to, inside the same top-level form."
  [context]
  (reduce (fn [acc [bound value-node]]
            (update acc bound (fnil conj []) value-node))
          {}
          (for [candidate (node-seq context)
                :when (= :list (n/tag candidate))
                :let [kids (significant-children candidate)
                      head (node-value (first kids))
                      binding-vector (second kids)]
                :when (contains? '#{let let* loop if-let when-let if-some
                                    when-some binding with-open with-local-vars
                                    doseq for}
                                 head)
                :when (and binding-vector (= :vector (n/tag binding-vector)))
                [name-node value-node] (partition 2 (significant-children
                                                      binding-vector))
                :let [bound (node-value name-node)]
                :when (symbol? bound)]
            [bound value-node])))

;; @spec MCP-OP-ALIAS-059
(defn- binding-mints?
  "Whether a symbol BOUND to this expression carries a minted kind.

  A bound symbol cannot be judged by `mint-evidence` alone: the change
  buffer's `checks` is bound to a vector of check maps whose own `:ok` and
  `:error-type` keys are keyword literals, and reading those as kind sources
  put the spurious kind `ok` back into the enumeration. What separates that
  from the shape the review named — a kind literal bound one line above and
  relayed as a bare symbol — is a RUNTIME SOURCE: `checks` is computed from
  incoming values, while `:planted-let-kind` and `{:a :brand-new-kind}` are
  pure literal data that can only ever hand back the name written in them.

  So a binding mints when it BUILDS a name (`keyword`, `str`, … appear in it),
  or when it carries `mint-evidence` and names no symbol at all — a literal,
  or a literal table, wearing a forward's clothes."
  [node]
  (let [symbols (set (for [candidate (node-seq node)
                           :let [value (node-value candidate)]
                           :when (symbol? value)]
                       value))]
    (boolean (or (some kind-minting-symbols symbols)
                 (and (seq (mint-evidence node)) (empty? symbols))))))

;; @spec MCP-OP-ALIAS-059
(defn- forwarded-kind-expression?
  "Whether `node` FORWARDS a kind rather than MINTING one.

  Three conditions, all required:

  1. every call in the expression uses a head from `kind-forwarding-heads`, or
     is a keyword lookup (a keyword in head position is a lookup and can mint
     no name whatever the keyword is);
  2. the expression carries no `mint-evidence` — no name-builder in ANY
     position and no keyword literal that is a value source rather than a
     selector; and
  3. the expression names at least one runtime SOURCE — a symbol outside head
     position, or such a lookup — so a bare literal composition cannot pass by
     containing no calls at all.

  Where `context` is given — the enclosing top-level form — every symbol the
  expression relays is additionally resolved through that form's `let`-style
  bindings and its bound expression checked for `mint-evidence`. The bound
  expression is NOT held to condition 1: a symbol bound to an ordinary helper
  call, `(analyzer-authority-error-type process)`, is a forward through a
  named function and not a mint, while a symbol bound to `:planted-kind` is a
  literal wearing a forward's clothes.

  This is what makes the `forwarded-refusal-kind` marker a CHECKED capability
  rather than a comment: a marker on a site that mints is now named by the
  guard exactly as an unmarked one is."
  ([node] (forwarded-kind-expression? node nil))
  ([node context]
   (let [nodes (node-seq node)
         lists (filter #(= :list (n/tag %)) nodes)
         heads (set (keep #(first (significant-children %)) lists))
         ;; a KEYWORD in head position is always a map lookup and can never
         ;; mint a name, whatever the keyword is
         lookup? (fn [candidate]
                   (keyword? (node-value (first (significant-children candidate)))))
         head-forwards? (fn [candidate]
                          (or (lookup? candidate)
                              (contains? kind-forwarding-heads
                                         (node-value
                                           (first (significant-children
                                                    candidate))))))
         source? (fn [candidate]
                   (or (and (symbol? (node-value candidate))
                            (not (contains? heads candidate)))
                       (and (= :list (n/tag candidate)) (lookup? candidate))))
         bindings (if context (binding-values-in context) {})
         relayed (set (for [candidate nodes
                            :let [value (node-value candidate)]
                            :when (and (symbol? value)
                                       (not (contains? heads candidate)))]
                        value))]
     (and (every? head-forwards? lists)
          (empty? (mint-evidence node))
          ;; EVERY binding of a relayed symbol, not the last one read: a
          ;; symbol bound twice in one top-level form forwards only if no
          ;; binding of it mints
          (every? (fn [symbol-name]
                    (not-any? binding-mints? (get bindings symbol-name)))
                  relayed)
          (boolean (some source? nodes))))))

;; @spec MCP-OP-ALIAS-059
(defn- forwarding-marked?
  "Whether the twelve lines above the site declare it forwards another's kind."
  [text site-text]
  (let [lines (vec (str/split-lines text))
        first-line (first (str/split-lines site-text))]
    (boolean
      (some (fn [index]
              (and (str/includes? (nth lines index) first-line)
                   (some #(str/includes? % "forwarded-refusal-kind")
                         (subvec lines (max 0 (- index 12)) (inc index)))))
            (range (count lines))))))

;; @spec MCP-OP-ALIAS-059
(defn- forwarding-exempt?
  "Whether a dynamic site is exempt: MARKED and mechanically FORWARDING.

  The marker alone was an unchecked capability — the guard exempted any site
  whose preceding twelve lines carried the marker text without ever
  establishing that the marked expression forwards. A comment is not a
  control."
  [text site]
  (and (forwarding-marked? text (:text site))
       (forwarded-kind-expression? (:value site) (:context site))))

;; @spec MCP-OP-ALIAS-059
(defn- reachable-entrance-source-text
  "Every source the entrance can refuse THROUGH, the router's slice included.

  Round-thirteen review finding 3: the shared router was held OUT, and a
  hold-out is not a construction. It is read as the slice
  `router-entrance-slice` builds — the top-level forms of `mcp_tool.clj`
  transitively referenced by `handle-alias-migration` — so membership is
  decided by a code path rather than by a file name, and the router's other
  verbs' kinds stay out on their own merits."
  []
  (str (apply str (map (comp slurp namespace-source-path)
                       (sort (reachable-entrance-namespaces))))
       (router-entrance-slice)))

;; @spec MCP-OP-ALIAS-059
(defn- structural-error-type-kinds
  "Kinds spelled in `:error-type`/`:error_type` VALUES, literal or minted.

  Read with the reader rather than with a regex anchored on the key: a
  non-literal value can still mint whole kinds, and a scan that requires a
  keyword immediately after the key sees none of them."
  [text]
  (into (sorted-set)
        (for [site (error-type-value-sites text)
              kind (if-let [literal (:literal site)]
                     [literal]
                     (when-not (forwarding-exempt? text site)
                       (minted-kinds-in (:value site))))
              ;; a value that spells the FIELD's own name is a key rename —
              ;; `(set/rename-keys {:error_type :error-type})` — and not a kind
              :when (and (re-matches #"[a-z][a-z0-9-]*" kind)
                         (not (contains? #{"error-type" "error_type"} kind)))]
          kind)))

;; @spec MCP-OP-ALIAS-059
(defn- runtime-spelled-kind-sites
  "`:error-type` values that mint no readable kind and declare no forwarding.

  The complement of `structural-error-type-kinds`: where that reads what a
  site can mint, this reports the sites that mint nothing a source scan can
  read — the reviewer's `(str \"heldout-\" \"protocol-kind\")` among them —
  so a kind composed at runtime fails the gate rather than disappearing from
  the enumeration.

  Round-fourteen review finding 2: this used to also exempt any site whose
  text merely CONTAINED one of its enclosing top-level def's own parameter
  names — meant to read the `refusal` constructor's own `(name error-type)`
  as a forward rather than a mint, it just as readily read
  `(keyword (name kind))` and `(keyword \"alias-migration-\" (name x))` as
  forwards too, in a router helper that MINTS a kind from a parameter rather
  than merely relaying one. There is exactly one legitimate dynamic site —
  the constructor's own — and it declares itself with the same
  `forwarded-refusal-kind` marker every other dynamic site must carry. No
  other exemption exists: a site not marked is named, whatever it mentions."
  [label text]
  (vec
    (for [site (error-type-value-sites text)
          :when (and (nil? (:literal site))
                     (empty? (minted-kinds-in (:value site)))
                     (not (forwarding-exempt? text site)))]
      (str label " · " (:text site)))))

;; @spec MCP-OP-ALIAS-059
(defn- unwrap-meta
  "The node a `^meta` or `#^meta` wrapper carries, however deep the wrapping."
  [node]
  (if (contains? #{:meta :meta*} (n/tag node))
    (recur (last (significant-children node)))
    node))

;; @spec MCP-OP-ALIAS-059
(defn- non-head-symbols
  "Every symbol of `node` outside FUNCTION position.

  `(name error-type)` mentions two symbols; only `error-type` is a value the
  expression can hand back. Reading the head as a value would make every
  constructor look as though it relayed something it does not."
  [node]
  (letfn [(walk [node head?]
            (let [value (node-value node)
                  list? (= :list (n/tag node))
                  kids (try (when (seq (n/children node))
                              (significant-children node))
                            (catch Exception _ nil))]
              (reduce into
                      (if (and (symbol? value) (not head?)) #{value} #{})
                      (map-indexed (fn [index child]
                                     (walk child (and list? (zero? index))))
                                   (or kids [])))))]
    (walk node false)))

;; @spec MCP-OP-ALIAS-059
(defn- map-parameter-locators
  "How a call site reaches each symbol a MAP destructuring parameter binds.

  `{:keys [kind message]}` at position 0 puts the kind at the `:kind` key of
  the first argument; `{k :kind}` puts it at the same place under another
  name. A map after `&` is keyword-argument destructuring, which no positional
  read can reach, so it binds nothing here and its symbols fall through to the
  unscannable branch."
  [map-node index rest?]
  (into {}
        (when-not rest?
          (mapcat
            (fn [[key-node value-node]]
              (let [key-value (node-value key-node)]
                (cond
                  (= :keys key-value)
                  (keep (fn [child]
                          (let [symbol-value (node-value (unwrap-meta child))]
                            (when (symbol? symbol-value)
                              [symbol-value
                               [:map-key index (keyword (name symbol-value))]])))
                        (significant-children value-node))

                  (and (symbol? key-value) (keyword? (node-value value-node)))
                  [[key-value [:map-key index (node-value value-node)]]]

                  :else nil)))
            (partition 2 (significant-children map-node))))))

;; @spec MCP-OP-ALIAS-059
(defn- parameter-locators
  "How a CALL SITE reaches each symbol one argument vector binds.

  Round-eighteen review finding 1: the scan asked whether the constructor's
  FIRST parameter carries the kind, and skipped the whole file when it did
  not. The question a call site actually needs answered is WHICH ARGUMENT to
  read, so the parameters are read into locators and the constructor's body
  says which locator the published kind comes from.

  A locator is `[:argument n]` — the nth argument of the call — or
  `[:map-key n k]` — the value at key `k` of the nth argument, which must be a
  map literal — or `:unreadable`, for a symbol bound through a shape no call
  site can be read through: a nested sequential destructuring, or a bare
  `& rest` the constructor takes apart itself."
  [argument-vector]
  (loop [nodes (map unwrap-meta (significant-children argument-vector))
         index 0
         rest? false
         locators {}]
    (if-let [node (first nodes)]
      (let [value (node-value node)
            tag (n/tag node)]
        (cond
          (= '& value) (recur (rest nodes) index true locators)

          (symbol? value)
          (recur (rest nodes) (inc index) rest?
                 (assoc locators value
                        (if rest? :unreadable [:argument index])))

          ;; `& [error-type extra]` continues the POSITIONS a caller fills;
          ;; a vector anywhere else destructures the contents of ONE argument,
          ;; which a call site cannot be read through.
          (= :vector tag)
          (recur (rest nodes) (inc index) rest?
                 (into locators
                       (keep-indexed
                         (fn [offset child]
                           (let [symbol-value (node-value (unwrap-meta child))]
                             (when (symbol? symbol-value)
                               [symbol-value
                                (if rest?
                                  [:argument (+ index offset)]
                                  :unreadable)])))
                         (significant-children node))))

          (= :map tag)
          (recur (rest nodes) (inc index) rest?
                 (into locators (map-parameter-locators node index rest?)))

          :else (recur (rest nodes) (inc index) rest? locators)))
      locators)))

;; @spec MCP-OP-ALIAS-059
(defn- refusal-arity-forms
  "Every arity of a `refusal` definition, as `[argument-vector body-nodes]`.

  Takes the definition's children AFTER the defined name and skips what the
  reader says is not an argument vector — a docstring, an attribute map, a
  metadata wrapper — then handles both shapes a definition takes: one vector
  for a single arity, and a list per arity for a multi-arity body. A
  `(def refusal (fn …))` is followed into the `fn`, whose own optional name is
  skipped the same way. Each arity carries its OWN body, because two arities
  of one constructor may take the kind in different positions."
  [nodes]
  (let [tail (drop-while (fn [node]
                           (let [value (node-value node)]
                             (or (string? value) (map? value) (symbol? value))))
                         (map unwrap-meta nodes))
        head (first tail)]
    (cond
      (nil? head) []

      (= :vector (n/tag head)) [[head (vec (rest tail))]]

      (and (= :list (n/tag head))
           (contains? '#{fn fn*}
                      (node-value (first (significant-children head)))))
      (refusal-arity-forms (rest (significant-children head)))

      :else (vec (for [node tail
                       :when (= :list (n/tag node))
                       :let [kids (map unwrap-meta (significant-children node))
                             child (first kids)]
                       :when (and child (= :vector (n/tag child)))]
                   [child (vec (rest kids))])))))

;; @spec MCP-OP-ALIAS-059
(defn- published-kind-values
  "Every `:error-type`/`:error_type`/`:kind` VALUE a constructor body publishes."
  [body-nodes]
  (for [body body-nodes
        candidate (node-seq body)
        :when (= :map (n/tag candidate))
        [key-node value-node] (partition 2 (significant-children candidate))
        :when (contains? #{:error-type :error_type :kind}
                         (node-value key-node))]
    value-node))

;; @spec MCP-OP-ALIAS-059
(defn- arity-kind-locator
  "Which ARGUMENT of a call to this arity carries the kind.

  The parameter that flows into the published kind is followed back to the
  position a caller fills, so second, third, destructured and rest positions
  are all found. Three determinate answers and no fourth:

  - a locator, when exactly one bound parameter reaches the published value;
  - `:constructor-literal`, when NOTHING outside the constructor reaches it —
    `mcp_workspace` and `extract_header` spell their one kind inside their own
    map, where the `:error_type \"…\"` scan already has it, and their call
    sites carry no kind to read;
  - `:unscannable`, for every other reading — no published kind at all, more
    than one parameter reaching it, a parameter bound through a shape a call
    site cannot be read through, or a symbol the reader cannot account for.
    Fail CLOSED: an unclassifiable constructor makes its sites unscannable and
    named, and can no longer switch the file's scan off."
  [argument-vector body-nodes]
  (let [locators (parameter-locators argument-vector)
        values (published-kind-values body-nodes)
        symbols (reduce into #{} (map non-head-symbols values))
        bound (distinct (keep locators symbols))
        foreign (seq (remove (set (keys locators)) symbols))]
    (cond
      (empty? values) :unscannable
      (= 1 (count bound)) (let [locator (first bound)]
                            (if (= :unreadable locator) :unscannable locator))
      (seq bound) :unscannable
      foreign :unscannable
      :else :constructor-literal)))

;; @spec MCP-OP-ALIAS-059
(defn- refusal-kind-arities
  "Where each ARITY of this source's own `refusal` takes its kind.

  ASSUMPTION, declared rather than implied: a call site is a list whose head
  is the literal symbol `refusal`, so a constructor that is ALIASED or APPLIED
  — `(def r refusal)`, `(let [r refusal] …)`, `(apply refusal …)` — is
  invisible to this scan in either direction. `rg -n \"\\(apply refusal|\\(def r\"
  src/` finds no such shape in the reachable set today, and one added later is
  a hole this guard would not see."
  [text]
  (let [root (try (parser/parse-string-all text) (catch Exception _ nil))]
    (vec
      (when root
        (for [top-level (significant-children root)
              :when (= :list (n/tag top-level))
              :let [kids (map unwrap-meta (significant-children top-level))]
              :when (and (contains? '#{defn defn- def}
                                    (node-value (first kids)))
                         (= 'refusal (node-value (second kids))))
              [argument-vector body-nodes] (refusal-arity-forms (drop 2 kids))
              :let [parameters (map unwrap-meta
                                    (significant-children argument-vector))]]
          {:fixed (count (take-while #(not= '& (node-value %)) parameters))
           :variadic? (boolean (some #(= '& (node-value %)) parameters))
           :locator (arity-kind-locator argument-vector body-nodes)})))))

;; @spec MCP-OP-ALIAS-059
(defn- call-site-kind-locator
  "The locator governing a `(refusal …)` call of `argument-count` arguments.

  A file with no `refusal` constructor at all, and a call matching two arities
  that disagree about the kind's position, are `:unscannable` — the site is
  NAMED rather than silently exempt.

  A call matching NO arity would throw an `ArityException` if it ever ran, so
  it is not evidence about anything; rather than exempt it, it falls back to
  the position every arity of the constructor AGREES on, and is unscannable
  only when they disagree. The fallback errs toward scanning a site, never
  toward skipping one."
  [arities argument-count]
  (let [matching (filter (fn [{:keys [fixed variadic?]}]
                           (if variadic?
                             (>= argument-count fixed)
                             (= argument-count fixed)))
                         arities)
        locators (or (seq (distinct (map :locator matching)))
                     (seq (distinct (map :locator arities))))]
    (if (= 1 (count locators))
      (first locators)
      :unscannable)))

;; @spec MCP-OP-ALIAS-059
(defn- kind-node-at
  "The node a locator points at among one call's arguments, or nil."
  [locator arguments]
  (when (vector? locator)
    (let [[shape index key-value] locator
          argument (get arguments index)]
      (case shape
        :argument argument
        :map-key (when (and argument (= :map (n/tag argument)))
                   (some (fn [[key-node value-node]]
                           (when (= key-value (node-value key-node))
                             value-node))
                         (partition 2 (significant-children argument))))
        nil))))

;; @spec MCP-OP-ALIAS-059
(defn- refusal-call-sites-in
  "Every `(refusal …)` CALL of one source, read with the READER.

  Round-sixteen review finding 2: these sites were found by a per-LINE regex,
  `#\"\\(refusal\\s\"`, which is wrong in both directions — a call whose kind
  begins on the next line matches nothing (the line ends after `(refusal`),
  and the word `refusal` inside a string or a comment matches everything. Both
  were reproduced on planted counterexamples. A call site is a FORM: it spans
  as many lines as it likes, and a string or a comment cannot be one BY
  CONSTRUCTION, because the reader never yields them as a list.

  Round-eighteen review findings 1 and 9: the site's KIND is no longer assumed
  to be the first argument, and the file's scan is no longer gated on the
  constructor's shape. `refusal-kind-arities` reads the constructor once,
  `call-site-kind-locator` picks the arity this call fills, and `:kind` is the
  argument that locator points at — second, third, inside a map literal, or
  after a `&`. A site whose kind the reader cannot locate carries `:kind nil`
  and is reported UNSCANNABLE by `dynamic-refusal-kind-sites-in`; only
  `:constructor-literal` — a constructor that publishes a kind no caller
  supplies — drops a site, and it drops one that carries no kind at all.

  Each site carries its kind expression, its enclosing TOP-LEVEL form (a bare
  symbol forwards or mints according to what it was bound to) and its ROW,
  read from the parser's own position metadata rather than counted by hand."
  [text]
  (let [root (try (parser/parse-string-all text) (catch Exception _ nil))
        arities (refusal-kind-arities text)]
    (when root
      (for [top-level (significant-children root)
            candidate (node-seq top-level)
            :when (= :list (n/tag candidate))
            :let [kids (significant-children candidate)]
            :when (= 'refusal (node-value (first kids)))
            :let [arguments (vec (rest kids))
                  locator (call-site-kind-locator arities (count arguments))]
            :when (not= :constructor-literal locator)]
        {:kind (kind-node-at locator arguments)
         :context top-level
         :row (:row (meta candidate))}))))

;; @spec MCP-OP-ALIAS-059
(defn- literal-refusal-kinds-in
  "Every kind a `(refusal …)` call of `text` SPELLS from literals.

  Read as forms for the same reason the guard is: the regex this replaces,
  `#\"\\(refusal :([a-z][a-z0-9-]*)\"`, is anchored on a single space, so a
  literal kind written on the line BELOW its `(refusal` was missed by the
  enumeration and by the guard that exists to catch what the enumeration
  cannot see — the one hole that hides a kind twice over.

  Round-seventeen review finding 2's second half: this read a keyword at the
  kind position and NOTHING else, so a non-literal expression that can still
  hand back a literal kind — `(get {\"planted\" \"brand-new-kind\"} k)` — was
  enumerated nowhere, while the guard exempted it as a marked forward. That is
  a kind reaching neither reader, which is exactly what the reviewer's plantI
  was. The `:error-type` shape has read its non-literal values with
  `minted-kinds-in` since round fourteen; the call-site shape now reads them
  the same way, so every site is either NAMED by
  `dynamic-refusal-kind-sites-in` — it spells its kind entirely at runtime, or
  its kind argument cannot be located at all — or ENUMERATED here, and never
  neither. A site whose kind the reader cannot locate contributes NOTHING
  here: enumerating arguments that may be messages would mint phantom kinds,
  and the site is named unscannable instead."
  [text]
  (for [site (refusal-call-sites-in text)
        :when (:kind site)
        :let [value (node-value (:kind site))]
        kind (if (keyword? value)
               [(name value)]
               (minted-kinds-in (:kind site)))
        :when (re-matches #"[a-z][a-z0-9-]*" kind)]
    kind))

;; @spec MCP-OP-ALIAS-059
(defn- dynamic-refusal-kind-sites-in
  "Every `(refusal …)` site of ONE source whose kind no source scan can read.

  Two shapes, both named the same way, because they cost the same thing — a
  live refusal kind absent from the enumeration:

  1. a kind spelled at RUNTIME. Source cannot scan it, so an enumeration
     derived from source is complete only while no reachable namespace builds
     one. The single legitimate exception is FORWARDING a kind another scanned
     source already minted, and such a site declares itself with the marker
     `forwarded-refusal-kind` in the twelve lines above it.
  2. a kind whose ARGUMENT POSITION the reader cannot locate — the file
     defines no `refusal`, the call matches no arity, or the constructor's own
     body cannot be classified. Round-eighteen review findings 1 and 9: this
     used to be a per-file ENABLE, so an unrecognised constructor made every
     site in the file exempt and the reviewer drove a live kind through one.
     It now fails CLOSED and carries no marker exemption: an unscannable site
     is named whatever the comments above it say, because a marker declares
     that a kind is forwarded, not that it cannot be found.

  Sites are read as FORMS by `refusal-call-sites-in`; only the MARKER is still
  a line window, because a marker is a comment and a comment is a line.

  Taken as TEXT rather than as a namespace so a witness can plant a site and
  drive this directly; `dynamic-refusal-kind-sites` is the same scan over the
  reachable set."
  [label text]
  (let [lines (vec (str/split-lines text))
        marked?
        (fn [row]
          (let [index (max 0 (dec (or row 1)))]
            (boolean
              (some #(str/includes? % "forwarded-refusal-kind")
                    (subvec lines
                            (max 0 (- index 12))
                            (min (count lines) (inc index)))))))]
    (vec
      (for [site (refusal-call-sites-in text)
            :let [kind (:kind site)
                  value (when kind (node-value kind))]
            ;; a kind the enumeration can READ is not spelled at runtime: a
            ;; keyword literal at the kind position is the kind itself, and a
            ;; non-literal expression contributes every literal it can still
            ;; hand back. `literal-refusal-kinds-in` reads both, so this is
            ;; the same escape `runtime-spelled-kind-sites` has always had at
            ;; the `:error-type` shape — and it is what makes every site
            ;; either NAMED here or ENUMERATED there, never neither.
            :when (or (nil? kind)
                      (and (not (and (keyword? value)
                                     (re-matches #"[a-z][a-z0-9-]*"
                                                 (name value))))
                           (empty? (minted-kinds-in kind))
                           (not (and (marked? (:row site))
                                     (forwarded-kind-expression?
                                       kind (:context site))))))]
        (str label ":" (:row site))))))

;; @spec MCP-OP-ALIAS-059
(defn- dynamic-refusal-kind-sites
  "`dynamic-refusal-kind-sites-in` over every source the entrance reaches."
  []
  (vec
    (mapcat (fn [namespace-name]
              (let [path (namespace-source-path namespace-name)]
                (dynamic-refusal-kind-sites-in path (slurp path))))
            (sort (reachable-entrance-namespaces)))))

;; @spec MCP-OP-ALIAS-059
(defn- unscannable-refusal-kind-sites
  "Every site in the reachable set whose kind no source scan can read.

  The union of two shapes: a `(refusal <non-literal>` call, and an
  `:error-type`/`:error_type` value that mints no keyword literal. The second
  is the shape the reviewer used — a `defmulti` in the shared router, routed
  from `handle-alias-migration`, spelling `(str \"heldout-\"
  \"protocol-kind\")` — and it was invisible to a guard that only read
  `(refusal` call sites, in a file that was held out besides.

  The router is read as the entrance's SLICE, the same one the enumeration
  reads, so the guard and the enumeration can never disagree about their
  subject."
  []
  (into (dynamic-refusal-kind-sites)
        cat
        (conj (mapv (fn [namespace-name]
                      (let [path (namespace-source-path namespace-name)]
                        (runtime-spelled-kind-sites path (slurp path))))
                    (sort (reachable-entrance-namespaces)))
              (runtime-spelled-kind-sites
                "src/clj_surgeon/mcp_tool.clj · entrance slice"
                (router-entrance-slice)))))


;; @spec MCP-OP-ALIAS-059
(defn- literal-refusal-kinds-in-reachable-sources
  "`literal-refusal-kinds-in` over each reachable source SEPARATELY.

  Round-eighteen review findings 1 and 9: a `(refusal …)` site's kind ARGUMENT
  is now located by reading the constructor that governs it, and a constructor
  governs ONE FILE. `reachable-entrance-source-text` CONCATENATES every
  reachable source into a single string, where eight `refusal` definitions
  with four different arities all appear to govern every site at once — a
  two-argument call matches `mcp_workspace`'s constant-kind constructor and
  `mcp_program_tool`'s kind-first one together, and would be read as
  unscannable for a disagreement that is an artifact of the concatenation and
  exists nowhere in the code.

  So the enumeration's unit is a FILE, exactly as `dynamic-refusal-kind-sites`'
  already is, and the guard and the enumeration can never disagree about their
  subject. The router's entrance SLICE is scanned as its own text for the same
  reason it is scanned at all: it is a code path, not a file, and the forms it
  carries are governed by no constructor of their own."
  []
  (into (vec (mapcat (fn [namespace-name]
                       (literal-refusal-kinds-in
                         (slurp (namespace-source-path namespace-name))))
                     (sort (reachable-entrance-namespaces))))
        (literal-refusal-kinds-in (router-entrance-slice))))

;; @spec MCP-OP-ALIAS-059
(defn- refusal-kinds-in-source
  "Every refusal kind the alias_migration ENTRANCE can emit, read from SOURCE.

  Derived rather than listed, so a refusal kind added later without a text
  witness fails this gate on the day it is written.

  The subject is the entrance, `handle-alias-migration`, and not the verb's own
  namespace: three other sources reach the same renderer through it, and an
  enumeration that reads only the verb's files is a listed set wearing a
  derivation's clothes. `mcp_workspace` refuses the route before the verb runs;
  `mcp_server` publishes the adapter's own failure after it throws; and the
  transaction kernel's error-types are passed through VERBATIM by
  `commit-refusal`, which names `(:error-type commit)` and not a constant, so
  every `:error-type` the kernel can mint is a kind this text block must
  render. Each of the three is scraped in its own spelling — the alias verb
  writes its kinds as keywords or strings, the router and adapter as string
  literals under `:error_type`, the kernel as keywords under `:error-type`.

  EVERY spelling is applied to the verb's OWN namespaces, whatever the prefix.
  Prefix-locking the alias side to `alias-migration-` cost the enumeration
  three kinds: `:unknown-verification-profile`, minted twice by
  `mcp_alias_migration.clj` through the verb's own `refusal` constructor —
  once in `execute-migration!` before any discovery, once on the undo path —
  and `mcp_operation.clj`'s `:invalid-mcp-operation-result` and
  `:invalid-mcp-elapsed-time`, thrown from the `invoke!` that
  `handle-alias-migration` calls directly. A prefix is a naming convention,
  not a boundary, and an enumeration that trusts one is narrower than its
  subject.

  `mcp_tool.clj` keeps the prefix filter, and only that: it is the shared
  router that hosts every verb, so its unprefixed kinds —
  `:receipt-publish-failed` on the extraction path,
  `:compact-relation-path-conflict` on compact-relations — belong to verbs
  `handle-alias-migration` never reaches."
  []
  (let [verb-text (reachable-entrance-source-text)
        router-text (slurp "src/clj_surgeon/mcp_tool.clj")
        entrance-text (str (slurp "src/clj_surgeon/mcp_workspace.clj")
                           (slurp "src/clj_surgeon/mcp_server.clj"))]
    (into (sorted-set "invalid-mcp-request" "server-not-initialized")
          cat
          [(map second (re-seq #"[:\"](alias-migration-[a-z-]+)[\"\s\)\}]"
                               (str verb-text router-text)))
           (literal-refusal-kinds-in-reachable-sources)
           (map second (re-seq #":error-type :([a-z][a-z0-9-]*)" verb-text))
           (map second (re-seq #":error_type \"([a-z-]+)\"" entrance-text))
           ;; @spec MCP-OP-ALIAS-059
           ;; a kind minted inside a NON-LITERAL value, which no regex anchored
           ;; on the key can read: `:error-type (if … :no-match
           ;; :ambiguous-match)` mints two, and a 125-kind set held neither
           (structural-error-type-kinds verb-text)])))

;; @spec MCP-OP-ALIAS-059
(defn- assert-refusal-text!
  "The text block a text-reading client sees carries the cause, every
  discriminating fact, the remedy, and the next_call as sendable JSON — or an
  explicit statement that there is none."
  [structured label]
  (let [text (mcp-tool/alias-migration-summary structured)]
    (is (str/includes? text (str (:error_type structured)))
        (str label " · the text block does not name the cause"))
    ;; @spec MCP-OP-ALIAS-059
    ;; EVERY non-envelope key, whatever the shape of its value. Round-thirteen
    ;; review finding 2: this filter admitted scalars and flat sequentials and
    ;; skipped everything else, so a key whose value is a NESTED MAP was never
    ;; asserted at all — `:helper_sabotage_detail {:nested "value"}` added to
    ;; the live `mcp_workspace` refusal and made to disappear in the renderer
    ;; left this witness, the dedicated live workspace witness, the live
    ;; receipts witness and the any-shape witness ALL green over 259
    ;; assertions. A witness that filters by the shape it is meant to police
    ;; is not a witness.
    (doseq [[field _] (sort-by key structured)
            :when (not (contains? refusal-envelope-keys field))]
      (is (str/includes? text (name field))
          (str label " · the text block drops the discriminating field "
               (name field))))
    (when-let [error (:error structured)]
      (is (str/includes? text error)
          (str label " · the text block drops the error sentence")))
    (when-let [remedy (:remedy structured)]
      (is (str/includes? text remedy)
          (str label " · the text block drops the remedy")))
    (if-let [call (:next_call structured)]
      (let [encoded (json/generate-string call)]
        (is (or (str/includes? text encoded)
                (and (str/includes? text "next_call")
                     (str/includes? text "structuredContent")
                     (str/includes? text (str (count encoded)))))
            (str label " · the text block drops the next_call the caller must send")))
      (is (re-find #"next_call[^\n]*none" text)
          (str label " · an absent next_call is omitted rather than stated")))
    text))

;; @spec MCP-OP-ALIAS-059
(deftest every-refusal-kind-renders-its-remedy-and-next-call-in-the-text-block
  ;; E3-P, 2026-09-03: `content[0].text` rendered ONLY the domain sentence.
  ;; structuredContent carried a complete, executable next_call — T-1 and T-3
  ;; found it and converged in 3.3 s — but T-2, reading the text, sent the same
  ;; wrong scope a second time. A refusal whose two faces disagree about what
  ;; the caller must do next is a refusal that costs a return at random.
  (testing "every refusal kind the source can emit"
    (doseq [kind (refusal-kinds-in-source)]
      (assert-refusal-text!
        {:ok false
         :operation "alias_migration"
         :error_type kind
         :error (str "one sentence stating the " kind " cause")
         :remedy (str "Resend the next_call; it corrects " kind ".")
         :found_files 0
         :scanned_files 7
         :elapsed_ms 1.25
         :source_unchanged true
         :next_call {"op" "alias_migration"
                     "scope" {"paths" ["src/**"]}
                     "expect" {"files" 21}}}
        kind)))
  (testing "a refusal with no computable next_call says so"
    (let [text (assert-refusal-text!
                 {:ok false
                  :operation "alias_migration"
                  :error_type "alias-migration-scope-too-deep"
                  :error "one path is past the depth bound"
                  :remedy "Narrow scope.paths so the walk does not reach it."
                  :depth 65
                  :max_depth 64
                  :elapsed_ms 1.0
                  :source_unchanged true
                  :next_call nil}
                 "no-next-call")]
      (is (str/includes? text "65"))
      (is (str/includes? text "Narrow scope.paths")))))

;; @spec MCP-OP-ALIAS-059
(deftest the-refusal-enumeration-covers-every-kind-the-entrance-can-emit
  ;; Round-10 review finding 4: `refusal-kinds-in-source` reads three files and
  ;; claims "every refusal kind alias_migration can emit". The alias_migration
  ;; ENTRANCE is `handle-alias-migration`, and three other sources reach the
  ;; same renderer through it: `invalid-workspace-root` from mcp_workspace,
  ;; `mcp-adapter-failure` from mcp_server, and the transaction kernel's own
  ;; error-types, passed through verbatim by `commit-refusal`. A gate that
  ;; derives its subject from a subset of its subject's sources is a listed
  ;; enumeration wearing a derivation's clothes.
  (let [kinds (refusal-kinds-in-source)]
    (testing "the workspace router's refusal is one of them"
      (is (contains? kinds "invalid-workspace-root")
          "the enumeration does not see mcp_workspace.clj"))
    (testing "the adapter's own failure is one of them"
      (is (contains? kinds "mcp-adapter-failure")
          "the enumeration does not see mcp_server.clj"))
    (testing "every kernel error-type the entrance passes through is one of them"
      (doseq [kind ["transaction-write-exception" "target-ancestor-changed"
                    "intent-compiler-failure" "invalid-transaction-receipt"
                    "future-source-transformation-failed"]]
        (is (contains? kinds kind)
            (str "the enumeration does not see the kernel's " kind))))))

;; @spec MCP-OP-ALIAS-059
(defn- refusal-constructor-kinds
  "Kind literals the entrance's own refusal constructors name.

  Scanned INDEPENDENTLY of `refusal-kinds-in-source`, over the same subject,
  and written apart on purpose: the enumeration is the thing under test, so a
  witness that asked the enumeration what the entrance holds would agree with
  itself. When the round-11 enumeration was prefix-locked to
  `alias-migration-`, `(refusal :unknown-verification-profile …)` — minted by
  the verb's own namespace, on the verb's own execution path, through the
  verb's own constructor — fell through both of its spellings.

  Two shapes, neither of which can produce a false positive: `(refusal :kind`
  is a call form, and `mcp_operation.clj` mints its two kinds as map values
  under `:error-type` and holds no other keyword in that position."
  []
  (let [verb-text (reachable-entrance-source-text)]
    (into (sorted-set)
          cat
          [(map second (re-seq #"\(refusal :([a-z][a-z0-9-]*)" verb-text))
           (map second (re-seq #":error-type :([a-z][a-z0-9-]*)"
                               verb-text))])))

;; @spec MCP-OP-ALIAS-059
(deftest the-refusal-enumeration-contains-every-kind-the-entrance-constructs
  ;; Round-11 re-review finding 3: the enumeration's alias-side regex was
  ;; prefix-locked to `alias-migration-`, and its keyword-spelled regex was
  ;; applied only to the two kernel files, so a kind minted by the verb's own
  ;; namespace under any other prefix fell through both. `execute-migration!`
  ;; refuses `:unknown-verification-profile` before any discovery, and the
  ;; undo path mints the same kind; `mcp_operation/invoke!` — which
  ;; `handle-alias-migration` calls directly — mints
  ;; `:invalid-mcp-operation-result` and `:invalid-mcp-elapsed-time`. The
  ;; live receipt renders correctly today only because `refusal-fact-line` is
  ;; generic; the gate that exists to fail "on the day a kind is written"
  ;; would not have noticed.
  (let [constructed (refusal-constructor-kinds)
        enumerated (refusal-kinds-in-source)
        missing (vec (sort (clojure.set/difference constructed enumerated)))]
    (is (empty? missing)
        (str "refusal kinds the entrance constructs that the enumeration does "
             "not carry: " (pr-str missing)))
    (testing "the kinds round 11 could not see, named"
      (doseq [kind ["unknown-verification-profile"
                    "invalid-mcp-operation-result"
                    "invalid-mcp-elapsed-time"]]
        (is (contains? enumerated kind)
            (str "the enumeration does not see " kind))))
    (testing "the kinds round 11 could see are still seen"
      (doseq [kind ["invalid-workspace-root" "mcp-adapter-failure"
                    "invalid-mcp-request" "alias-migration-empty-scope"]]
        (is (contains? enumerated kind)
            (str "the enumeration lost " kind))))))

;; @spec MCP-OP-ALIAS-059
(deftest a-truncated-fact-line-says-how-many-facts-it-dropped
  ;; Round-11 re-review finding 7: `refusal-fact-line` takes 12 facts and says
  ;; nothing about the rest.
  ;;
  ;;   facts published = 15  · facts rendered = 12
  ;;   dropped fields = (:f12 :f13 :f14)
  ;;   does the text say it truncated? => false
  ;;
  ;; No live alias_migration refusal carries twelve discriminating facts today
  ;; — the widest, scope-matches-nothing, carries ten — so the bound has never
  ;; fired, which is exactly why it must say so before it does. It is the same
  ;; silent-truncation class this branch has now paid for three times.
  (let [receipt (fn [n]
                  (into {:ok false
                         :operation "alias_migration"
                         :error_type "alias-migration-empty-scope"}
                        (map (fn [index]
                               [(keyword (format "f%02d" index)) index])
                             (range n))))
        fact-count (fn [line]
                     (count (str/split (str/replace line "facts · " "")
                                       #" · ")))]
    (is (= 16 mcp-tool/max-refusal-facts)
        (str "the stated fact bound moved; the numbers below name it and must "
             "move with it"))
    (testing "at the bound the line says nothing, because nothing was dropped"
      (let [line (mcp-tool/refusal-fact-line (receipt 16))]
        (is (= 16 (fact-count line)) line)
        (is (not (str/includes? line "more"))
            "a complete fact line claims a truncation it did not make")))
    (testing "one past the bound"
      (let [line (mcp-tool/refusal-fact-line (receipt 17))]
        (is (str/includes? line "+1 more")
            (str "the fact line dropped a fact in silence: " line))
        (is (str/includes? line "structuredContent")
            "the line does not say where the dropped facts are")))
    (testing "three past the bound"
      (is (str/includes? (mcp-tool/refusal-fact-line (receipt 19)) "+3 more")
          "the fact line does not count the facts it dropped"))))

;; @spec MCP-OP-ALIAS-059
(def ^:private text-renders-by-value
  "Refusal keys the text block renders as a VALUE rather than as a name.

  `ok` is the word `refused`, `operation` is the header, `error_type` is the
  cause after it, and `error` is the `→` sentence. Every OTHER key a refusal
  carries must appear in the text under its own name, or a client reading the
  text is told less than one reading the structure."
  #{"ok" "operation" "error_type" "error"})

;; @spec MCP-OP-ALIAS-059
(defn- refusal-receipt-keys-in-source
  "Every key an alias_migration refusal carries by construction.

  Read with the reader rather than grepped: the constructor's own base map
  merged with every facts map handed to a `(refusal …)` call in the verb's two
  namespaces. Keys are the EVEN meaningful children of each map literal, so a
  keyword VALUE is never mistaken for a key."
  []
  (let [meaningful (fn [node]
                     (remove #(contains? #{:whitespace :newline :comma :comment}
                                         (n/tag %))
                             (n/children node)))
        map-keys (fn [node]
                   (keep-indexed
                     (fn [index child]
                       (when (even? index)
                         (let [value (try (n/sexpr child) (catch Exception _ nil))]
                           (when (keyword? value) (name value)))))
                     (meaningful node)))
        forms (fn [path]
                (tree-seq n/inner? n/children
                          (parser/parse-string-all (slurp path))))
        constructor-keys
        (fn [path]
          (->> (forms path)
               (filter #(= :list (n/tag %)))
               (filter (fn [node]
                         (let [[head name-node] (meaningful node)]
                           (and (contains? #{"defn" "defn-"}
                                           (some-> head n/string))
                                (= "refusal" (some-> name-node n/string))))))
               (mapcat (fn [node]
                         (->> (tree-seq n/inner? n/children node)
                              (filter #(= :map (n/tag %)))
                              (mapcat map-keys))))))
        fact-keys
        (fn [path]
          (->> (forms path)
               (filter #(= :list (n/tag %)))
               (filter (fn [node]
                         (= "refusal" (some-> (first (meaningful node))
                                              n/string))))
               (mapcat (fn [node]
                         (->> (n/children node)
                              (filter #(= :map (n/tag %)))
                              (mapcat map-keys))))))
        sources ["src/clj_surgeon/mcp_alias_migration.clj"
                 "src/clj_surgeon/alias_migration.clj"]]
    (into (sorted-set)
          cat
          [(mapcat constructor-keys sources)
           (mapcat fact-keys sources)])))

;; @spec MCP-OP-ALIAS-059
(deftest every-refusal-key-the-verb-constructs-appears-in-its-text-block
  ;; E-PREWRITE cohort, 2026-09-04, live against alias_migration at 656a0a3c:
  ;; the `alias-migration-alias-policy-exhausted` refusal carried
  ;; `mutation_attempted false` and `write_authority false` in
  ;; structuredContent, and its text block carried neither —
  ;;
  ;;   structured keys => [… :mutation_attempted … :write_authority]
  ;;   text names mutation_attempted? => false
  ;;   text names write_authority?    => false
  ;;
  ;; — because both are listed as ENVELOPE keys and nothing renders them.
  ;; They are exactly the two fields that separate "refused before touching
  ;; anything" from "tried and rolled back", so a client reading the text
  ;; cannot tell those two states apart. `source_unchanged`,
  ;; `next_action` and `expect_files_unchanged_reason` are suppressed the same
  ;; way. This is derived from SOURCE, so a refusal key added later without a
  ;; rendering fails the gate on the day it is written.
  (let [keys-in-source (refusal-receipt-keys-in-source)]
    (is (contains? keys-in-source "mutation_attempted")
        "the scan does not see the constructor's own base map")
    (is (contains? keys-in-source "collided_bindings")
        "the scan does not see a facts map handed to the constructor")
    ;; @spec MCP-OP-ALIAS-059
    ;; Round-twelve review finding 6: every key was probed with the single
    ;; synthetic string "probe-value", and the renderer dropped non-scalar
    ;; values in silence — so this witness was blind to exactly the shape the
    ;; review's sabotage used. Each key is now probed with the shapes a
    ;; refusal's facts actually take, a nested map among them.
    (doseq [field (remove text-renders-by-value keys-in-source)
            [shape value] [["a string" "probe-value"]
                           ["a number" 7]
                           ["a boolean" false]
                           ["a vector" ["a" "b"]]
                           ["a nested map" {:nested "value"}]]]
      (let [receipt {:ok false
                     :operation "alias_migration"
                     :error_type "alias-migration-empty-scope"
                     :error "one sentence stating the cause"
                     :elapsed_ms 1.25
                     (keyword field) value}
            text (mcp-tool/alias-migration-summary receipt)]
        (is (str/includes? text field)
            (str "the text block drops the refusal key " field
                 " when its value is " shape
                 ", which structuredContent carries"))))
    (testing "the four keys the text renders as values, not as names"
      (let [text (mcp-tool/alias-migration-summary
                   {:ok false
                    :operation "alias_migration"
                    :error_type "alias-migration-empty-scope"
                    :error "one sentence stating the cause"
                    :elapsed_ms 1.25})]
        (is (str/includes? text "alias_migration") "operation")
        (is (str/includes? text "refused") "ok")
        (is (str/includes? text "alias-migration-empty-scope") "error_type")
        (is (str/includes? text "one sentence stating the cause") "error")))))

;; @spec MCP-OP-ALIAS-059
(deftest a-refusal-without-a-remedy-does-not-point-at-one
  ;; The rendered no-next_call line said "the remedy above names what only the
  ;; caller can decide" unconditionally — on a receipt carrying no `:remedy` it
  ;; points the caller at a sentence that is not there.
  (let [text (mcp-tool/alias-migration-summary
               {:ok false
                :operation "alias_migration"
                :error_type "invalid-workspace-root"
                :error "workspace_root must be absolute"
                :elapsed_ms 1.0
                :source_unchanged true})]
    (is (not (str/includes? text "the remedy above"))
        (str "a receipt with no remedy pointed at one:\n" text))
    (is (str/includes? text "next_call")
        "an absent next_call was omitted rather than stated")))

;; @spec MCP-OP-ALIAS-059
(deftest the-live-invalid-workspace-root-refusal-renders-cause-remedy-and-next-call
  ;; Driven through the real entrance, not shaped by hand: this is the receipt
  ;; the reviewer rendered, and it named a remedy it did not carry while
  ;; suppressing the one discriminating fact it had — the bad value itself,
  ;; hidden because `workspace_root` is an ENVELOPE key on every other receipt.
  (let [workspace (workspace!)]
    (try
      (mcp-tool/init! (config workspace (io/file workspace "receipts")))
      (let [captured (atom nil)]
        (mcp-tool/handle-alias-migration
          nil
          (json/parse-string
            (json/generate-string
              (request workspace {:workspace_root "relative/path"}))
            true)
          (fn [content error? structured]
            (reset! captured {:content content :error? error?
                              :result structured})))
        (let [{:keys [content result]} @captured
              text (first content)]
          (is (= "invalid-workspace-root" (:error_type result)) (pr-str result))
          (is (string? (:remedy result))
              "the live refusal carries no remedy at all")
          (is (str/includes? text (:remedy result))
              "the text block drops the remedy")
          ;; the invariant, not the phrase: the line may name a remedy only
          ;; when the receipt carries one. RED published this sentence over a
          ;; receipt with no `:remedy` at all.
          (is (or (:remedy result)
                  (not (str/includes? text "the remedy above")))
              (str "the text pointed at a remedy the receipt does not carry:\n"
                   text))
          (is (str/includes? text "relative/path")
              (str "the one discriminating fact — the bad workspace_root — is "
                   "suppressed as an envelope key:\n" text))
          ;; @spec MCP-OP-ALIAS-059
          (assert-refusal-text! (assoc result :elapsed_ms 1.0)
                                "invalid-workspace-root")))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-059
(deftest an-adapter-failure-renders-through-the-tools-own-summary
  ;; `mcp-adapter-failure` is published by the SDK wrapper, which never saw the
  ;; operation's summarizer, so its `content[0].text` was raw JSON — the one
  ;; refusal class whose two faces could not agree because one of them was not
  ;; rendered at all.
  (let [shape (resolve 'clj-surgeon.mcp-server/adapter-failure)
        _ (is (some? shape)
              (str "mcp_server shapes its adapter failure inline, so nothing "
                   "else can render or assert it"))
        failure (when shape
                  (shape "alias_migration"
                         (ex-info "the adapter could not read the arguments" {})))]
    (is (= "mcp-adapter-failure" (:error_type failure)))
    (is (string? (:remedy failure))
        "the adapter failure carries no remedy")
    (is (not (contains? failure :source_unchanged))
        (str "the adapter failure claims to know the tree's state; it cannot — "
             "the operation's own receipt was never published"))
    (is (= mcp-tool/alias-migration-summary
           (:summarize (first (filter #(= "alias_migration" (:name %))
                                      (mcp-tool/tools-for-profile :full)))))
        "the alias_migration tool carries no summarizer for the adapter to use")
    (when failure
      (let [text (mcp-tool/alias-migration-summary
                   (assoc failure :elapsed_ms 1.0))]
        (is (str/includes? text "mcp-adapter-failure"))
        (is (str/includes? text (:remedy failure)))
        (is (str/includes? text "source state requires structured receipt review")
            "an adapter failure asserted the source was unchanged")))))


;; @spec MCP-OP-ALIAS-059
(deftest a-live-refusals-text-and-structured-receipt-do-not-disagree
  (let [workspace (workspace!)]
    (try
      (doseq [[label overrides]
              [["scope-matches-nothing" {:scope {:paths ["srk/**"]}
                                         :expect {:files 12}}]
               ["empty-scope" {:scope {:paths ["src/acid/fanout/n0*.clj"]}
                               :expect {:files 0}}]
               ["expect-mismatch" {:expect {:files 99}}]
               ["mixed-var-spec" {:from {:lib fixture/from-lib :var nil}
                                  :to {:lib fixture/to-lib :var fixture/to-var
                                       :alias_policy fixture/alias-policy}}]]]
        ;; execute! is the workspace half; mcp-operation/invoke! stamps
        ;; elapsed_ms at the tool boundary, so it is supplied here rather than
        ;; driving the whole MCP callback for a rendering assertion
        (let [result (assoc (execute! workspace overrides) :elapsed_ms 1.0)]
          (is (false? (:ok result)) (str label " did not refuse"))
          (assert-refusal-text! result label)))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-051
(def ^:private malformed-scope-globs
  "Glob spellings `FileSystems/getDefault().getPathMatcher` refuses to parse.

  Every one of them is a shape a model reaches for and mis-closes; `src/{**` is
  one keystroke from `src/{clj,cljs}/**`."
  ["[" "{a,b" "**/{" "src/{**" "src/[a-" "src/**\\" "\\" "a{b"])

;; @spec MCP-OP-ALIAS-051
(def ^:private well-formed-scope-globs
  "The control arm: legal globs that must keep reaching the walk."
  ["src/**" "**" "src/*.clj" "src/{acid,other}/**"])

;; @spec MCP-OP-ALIAS-051
(deftest a-malformed-scope-glob-is-a-typed-refusal-not-an-adapter-failure
  ;; Round-10 review finding 3: `glob-matcher` calls `getPathMatcher` on caller
  ;; text with no `try`; `execute!` catches only `OutOfMemoryError`, and
  ;; `mcp-operation/invoke!` has no catch at all, so the throw reached
  ;; `mcp_server.clj` and was published as `mcp-adapter-failure` — a receipt
  ;; with no source_unchanged, no mutation_attempted, no remedy, no next_call,
  ;; and a raw-JSON text block that never passes through the summary. The whole
  ;; ALIAS-059 contract is bypassed for this class.
  (let [workspace (workspace!)]
    (try
      (doseq [pattern malformed-scope-globs]
        (let [result (try
                       (assoc (execute! workspace {:scope {:paths [pattern]}
                                                   :expect {:files 12}})
                              :elapsed_ms 1.0)
                       (catch Throwable error
                         {:threw (.getName (class error))
                          :message (.getMessage error)}))
              label (str "malformed glob " (pr-str pattern))]
          (is (nil? (:threw result))
              (str label " escaped as an untyped throw: " (pr-str result)))
          (is (= "alias-migration-scope-path-refused" (:error_type result))
              (str label " · " (pr-str result)))
          (is (not= "mcp-adapter-failure" (:error_type result)) label)
          (is (true? (:source_unchanged result)) label)
          (is (false? (:mutation_attempted result)) label)
          (is (str/includes? (str (:error result)) pattern)
              (str label " · the refusal does not name the pattern"))
          (is (and (string? (:cause result)) (seq (:cause result)))
              (str label " · the refusal does not carry the parser's message"))
          (is (str/includes? (str (:error result)) (str (:cause result)))
              (str label " · the sentence drops the parser's own message"))
          (is (string? (:remedy result)) label)
          ;; @spec MCP-OP-ALIAS-059
          ;; the text face is only renderable once the throw is a receipt
          (when-not (:threw result)
            (assert-refusal-text! result label))))
      (testing "a legal glob still reaches the walk"
        (doseq [pattern well-formed-scope-globs]
          (let [result (execute! workspace {:scope {:paths [pattern]}
                                            :expect {:files 12}})]
            (is (not= "alias-migration-scope-path-refused" (:error_type result))
                (str "the well-formed glob " (pr-str pattern)
                     " was refused as malformed")))))
      (testing "the tool entrance publishes the same typed refusal"
        (let [captured (atom nil)
              _ (mcp-tool/init! (config workspace (io/file workspace "receipts")))
              thrown (try
                       (mcp-tool/handle-alias-migration
                         nil
                         (json/parse-string
                           (json/generate-string
                             (request workspace {:scope {:paths ["src/{**"]}
                                                 :expect {:files 12}}))
                           true)
                         (fn [content error? structured]
                           (reset! captured {:content content :error? error?
                                             :result structured})))
                       nil
                       (catch Throwable error (.getName (class error))))]
          (is (nil? thrown)
              (str "the tool entrance threw " thrown
                   " instead of publishing a receipt"))
          (is (some? @captured) "no receipt was published at all")
          (is (= "alias-migration-scope-path-refused"
                 (:error_type (:result @captured)))
              (pr-str (:result @captured)))))
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
        ;; @spec MCP-OP-ALIAS-008
        ;; This assertion used to PIN the defect: it required the next_call to
        ;; carry ["store2" "store2-2"], an alias_policy the caller never sent.
        ;; An oracle that asserts the wrong invariant is a finding of its own,
        ;; corrected in the same fix.
        (is (nil? (:next_call result))
            "an exhausted policy was answered with an alias outside it"))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-008
(deftest an-exhausted-alias-policy-proposes-no-alias-the-policy-forbids
  ;; E-PREWRITE cohort, 2026-09-04, live against alias_migration at 656a0a3c
  ;; over its own fixture, whose src/acid/fanout/ns_100.clj binds all four
  ;; alias_policy entries:
  ;;
  ;;   alias_policy (given)=> ["store2" "st2" "es" "store-2"]
  ;;   collided_bindings   => ["store2" "st2" "es" "store-2"]
  ;;   next_call policy    => ["store2" "st2" "es" "store-2" "store-2-2"]
  ;;
  ;; `store-2-2` is not in the policy the caller sent. A next_call is the
  ;; caller's own request with one field corrected; it may never answer with a
  ;; value the caller's own request forbids, because the caller cannot execute
  ;; it without abandoning a constraint they stated. When the policy is
  ;; exhausted for a file the correction is not mechanical — only the caller
  ;; knows which alias they are willing to add — so there is no next_call, and
  ;; the remedy has to say the policy is exhausted and name the file.
  (let [workspace (workspace!)]
    (try
      (spit (io/file workspace "src/acid/fanout/allbound.clj")
            (str "(ns acid.fanout.allbound\n  (:require\n"
                 "   [acid.fanout.store :as st]\n"
                 "   [acid.fanout.util-a :as store2]\n"
                 "   [acid.fanout.util-b :as st2]\n"
                 "   [acid.fanout.util-c :as es]\n"
                 "   [acid.fanout.util-d :as store-2]))\n\n"
                 "(defn one [id] (st/find-event id))\n"))
      (let [result (execute! workspace {:expect {:files 13}})
            remedy (str (:remedy result))]
        (is (= "alias-migration-alias-policy-exhausted" (:error_type result))
            (pr-str result))
        (is (= "src/acid/fanout/allbound.clj" (:file result)))
        (is (= (vec fixture/alias-policy) (:collided_bindings result))
            "every policy entry was expected to be bound in that file")
        (testing "no next_call proposes a value the caller's policy forbids"
          (is (every? (set fixture/alias-policy)
                      (get-in result [:next_call "to" "alias_policy"]))
              (str "the next_call proposes an alias outside the caller's own "
                   "alias_policy: "
                   (pr-str (get-in result [:next_call "to" "alias_policy"]))))
          (is (nil? (:next_call result))
              "an exhausted policy has no mechanical correction to compose"))
        (testing "the remedy says what is exhausted, and where"
          (is (str/includes? remedy "exhausted")
              (str "the remedy does not say the policy is exhausted: " remedy))
          (is (str/includes? remedy "src/acid/fanout/allbound.clj")
              (str "the remedy does not name the file: " remedy))))
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
        ;; @spec MCP-OP-ALIAS-059
        ;; the refused path is 255 characters and every refusal field is now
        ;; bounded at 200: it is named by its prefix, with the length it
        ;; replaced, not by silent truncation
        (is (str/starts-with? deep (subs (:path result) 0 200))
            "the refusal does not name the path the walk refused")
        (is (str/includes? (:path result) (str (count deep)))
            "the elided path does not name the length it replaced")
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

;; ---------------------------------------------------------------------------
;; discovery is byte-faithful, and its completeness claim is witnessed

(defn- backslash-tree!
  "A workspace holding three owners, two of them spelled with a backslash.

  On POSIX a backslash is an ORDINARY path character: `\\` is a legal
  top-level directory name and `a\\b.clj` is a legal file name. Neither is a
  separator, and a discovery that reads one as a separator loses the owner
  under it."
  []
  (let [workspace (temp-dir)]
    (write-tree! workspace
                 {"src/a.clj" (requiring-source "a")
                  (str "\\" "/b.clj") (requiring-source "b")
                  (str "src/c" "\\" "d.clj") (requiring-source "c-d")})
    workspace))

(defn- independent-source-count
  "A second walker's count of the `.clj` files under `workspace`.

  Written with `file-seq` and `java.io.File`, sharing no code with the scope
  walk, so it cannot inherit the scope walk's own path arithmetic."
  [workspace]
  (count (filter #(and (.isFile ^java.io.File %)
                       (str/ends-with? (.getName ^java.io.File %) ".clj"))
                 (file-seq (io/file workspace)))))

;; @spec MCP-OP-ALIAS-060
(deftest a-backslash-is-a-path-character-and-never-a-separator
  ;; Round-twelve review finding 1: `relative-path` converted EVERY backslash
  ;; in a project-relative filename into a slash, so the owner under the
  ;; top-level directory whose name is exactly `\` was dropped from
  ;; `scope.paths ["**"]` — and the verb then COMMITTED the one owner it could
  ;; see under a receipt claiming complete discovery:
  ;;
  ;;   receipt => {:ok true, :committed true, :files 1, :sites 1}
  ;;   normal file migrated? => true
  ;;   backslash file still old? => true
  ;;   scan => {:ok true, :files [src/a.clj]}
  ;;
  ;; A partial migration under a complete claim breaks MCP-OP-ALIAS-004's
  ;; N-owner closure guarantee: the caller is told the fan-out is closed while
  ;; a namespace still requires the retired lib.
  (let [workspace (backslash-tree!)]
    (try
      (let [scan (alias-migration/scan-scope (.toPath workspace)
                                             {:paths ["**"] :exclude []})]
        (testing "the walk sees every owner the filesystem holds"
          (is (:ok scan) (pr-str scan))
          (is (= 3 (count (:files scan)))
              (str "the scope walk lost a file to a backslash: "
                   (pr-str (:files scan))))
          (is (= #{"src/a.clj" (str "\\" "/b.clj") (str "src/c" "\\" "d.clj")}
                 (set (:files scan)))
              (str "the walk did not report the paths byte-faithfully: "
                   (pr-str (:files scan))))))
      (testing "every owner migrates, and the receipt's count is the tree's"
        (let [captured (atom nil)
              _ (mcp-tool/init! (config workspace (io/file workspace "receipts")))
              _ (mcp-tool/handle-alias-migration
                  nil
                  (json/parse-string
                    (json/generate-string
                      (request workspace {:scope {:paths ["**"]}
                                          :expect {:files 3}}))
                    true)
                  (fn [content error? structured]
                    (reset! captured {:content content :error? error?
                                      :result structured})))
              result (:result @captured)]
          (is (:ok result) (pr-str result))
          (is (true? (:committed result)) (pr-str result))
          (is (= 3 (:files result))
              (str "the receipt claims complete discovery over "
                   (:files result) " of 3 owners"))
          (is (= (independent-source-count workspace) (:files result))
              "the receipt's file count and an independent walk disagree")
          (doseq [relative ["src/a.clj" (str "\\" "/b.clj")
                            (str "src/c" "\\" "d.clj")]]
            (let [migrated (slurp (io/file workspace relative))]
              (is (str/includes? migrated "acid.fanout.store2")
                  (str relative " was left requiring the retired lib"))
              (is (not (str/includes? migrated "acid.fanout.store :as"))
                  (str relative " still requires the retired lib"))))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-060
(deftest a-discovery-that-cannot-account-for-the-tree-refuses-and-writes-nothing
  ;; The completeness claim is only as good as its witness. The scope walk
  ;; derives one relative path string per entry; a SECOND enumeration counts
  ;; the same scope without building a string, and the two must agree before
  ;; any byte is written. Round twelve's defect was silent precisely because
  ;; nothing compared the discovery against the tree.
  (let [workspace (backslash-tree!)]
    (try
      (let [before (into {} (map (fn [relative]
                                   [relative (slurp (io/file workspace relative))])
                                 ["src/a.clj" (str "\\" "/b.clj")
                                  (str "src/c" "\\" "d.clj")]))
            counter (resolve
                      'clj-surgeon.mcp-alias-migration/independent-scope-count)]
        (is (some? counter)
            (str "discovery publishes no independent enumeration for its "
                 "completeness claim to be checked against"))
        (when counter
          (let [result (with-redefs-fn {counter (fn [& _] 99)}
                         #(execute! workspace {:scope {:paths ["**"]}
                                               :expect {:files 3}}))]
            (is (false? (:ok result)) (pr-str result))
            (is (= "alias-migration-discovery-incomplete" (:error_type result))
                (pr-str result))
            (is (= 3 (:files_considered result)) (pr-str result))
            (is (= 99 (:files_enumerated result)) (pr-str result))
            (is (true? (:source_unchanged result)) (pr-str result))
            (is (false? (:mutation_attempted result)) (pr-str result))
            (doseq [[relative source] before]
              (is (= source (slurp (io/file workspace relative)))
                  (str relative " was written under a discovery that did not "
                       "account for the tree"))))))
      (finally
        (delete-tree! workspace)))))

;; ---------------------------------------------------------------------------
;; the visible refusal is bounded in CHARACTERS, not in items

(defn- blowout-tree!
  "Seven legal top-level roots, each a digit followed by 246 quotation marks."
  []
  (let [workspace (temp-dir)]
    (write-tree! workspace
                 (into {}
                       (map (fn [index]
                              [(str index (apply str (repeat 246 \")) "/x.clj")
                               (requiring-source (str "n" index))])
                            (range 7))))
    workspace))

;; @spec MCP-OP-ALIAS-059
(deftest the-visible-refusal-is-held-to-its-stated-character-ceiling
  ;; Round-twelve review finding 2: `root-sizes` is bounded by ITEM COUNT and
  ;; embedded whole in `:remedy`, and `max-refusal-text-characters` is stated
  ;; but enforced nowhere. Seven legal roots of 246 quotation marks each:
  ;;
  ;;   error_type => alias-migration-scope-matches-nothing
  ;;   next_call => nil
  ;;   root count/listed => 7 6
  ;;   root list count/json length/max item => 6 3019 254
  ;;   error/remedy/text lengths => 353 3445 4890
  ;;   text ceiling => 4096
  ;;
  ;; A ceiling a witness asserts and the renderer does not enforce is a
  ;; number in a docstring.
  (let [workspace (blowout-tree!)]
    (try
      (let [captured (atom nil)
            _ (mcp-tool/init! (config workspace (io/file workspace "receipts")))
            _ (mcp-tool/handle-alias-migration
                nil
                (json/parse-string
                  (json/generate-string
                    (request workspace
                             {:scope {:paths ["no-such-dir/**"]
                                      :exclude (vec (repeat 6 (apply str (repeat 60 "e"))))}
                              :expect {:files 7}}))
                  true)
                (fn [content error? structured]
                  (reset! captured {:content content :error? error?
                                    :result structured})))
            result (:result @captured)
            text (first (:content @captured))]
        (is (string? text)
            (str "the entrance published no text block to bound: "
                 (pr-str (:content @captured))))
        (is (= "alias-migration-scope-matches-nothing" (:error_type result))
            (pr-str result))
        (is (nil? (:next_call result)) (pr-str (:next_call result)))
        (is (<= (count text) alias-migration/max-refusal-text-characters)
            (str "the visible refusal is " (count text)
                 " characters, past its stated ceiling of "
                 alias-migration/max-refusal-text-characters))
        (testing "the root listing is bounded in characters and says so"
          (let [ceiling (some-> (resolve
                                  'clj-surgeon.mcp-alias-migration/max-refusal-root-list-characters)
                                var-get)
                listing (alias-migration/root-sizes
                          (alias-migration/suggested-scope-paths
                            (.toPath workspace)))
                rendered (pr-str listing)]
            (is (some? ceiling)
                "the root listing states no character ceiling to be held to")
            (when ceiling
              (is (<= (count rendered) ceiling)
                  (str "the root listing renders " (count rendered)
                       " characters, past its ceiling of " ceiling))
              (is (<= (count (pr-str (alias-migration/root-sizes
                                       (alias-migration/suggested-scope-paths
                                         (.toPath workspace)))))
                      ceiling)
                  "the ceiling is not read on the rendered form"))
            (is (some #(str/includes? % "more roots") listing)
                (str "the listing dropped roots in silence: "
                     (pr-str listing))))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-059
(deftest the-text-ceiling-is-witnessed-at-the-ceiling-and-one-past-it
  ;; The bound is asserted AT the number, in both directions: a refusal whose
  ;; rendered text is exactly the ceiling is published whole and claims no
  ;; truncation; one character more is truncated with a typed marker naming
  ;; the length it replaced. A bound tested only far from its edge is a bound
  ;; nobody has tested.
  (let [ceiling alias-migration/max-refusal-text-characters
        receipt (fn [remedy]
                  {:ok false
                   :operation "alias_migration"
                   :error_type "alias-migration-empty-scope"
                   :error "one sentence stating the cause"
                   :elapsed_ms 1.25
                   :source_unchanged true
                   :remedy remedy})
        base (count (mcp-tool/alias-migration-summary (receipt "")))
        at (mcp-tool/alias-migration-summary
             (receipt (apply str (repeat (- ceiling base) \x))))
        past (mcp-tool/alias-migration-summary
               (receipt (apply str (repeat (inc (- ceiling base)) \x))))]
    (testing "exactly at the ceiling"
      (is (= ceiling (count at))
          (str "the fixture did not land on the ceiling: " (count at)))
      (is (not (str/includes? at "truncated"))
          "a refusal exactly at the ceiling claims a truncation it did not make"))
    (testing "one character past the ceiling"
      (is (<= (count past) ceiling)
          (str "the refusal text is " (count past) " characters, past "
               ceiling))
      (is (str/includes? past "truncated")
          "the refusal text was cut in silence")
      (is (str/includes? past "structuredContent")
          "the truncation marker does not say where the whole refusal is"))))

;; ---------------------------------------------------------------------------
;; the refusal enumeration derives its SOURCES, not just its kinds

(defn- kondo-stub!
  "An executable named `clj-kondo` that answers something that is not EDN."
  [workspace]
  (let [script (io/file workspace "bin" "clj-kondo")]
    (.mkdirs (.getParentFile script))
    (spit script "#!/bin/bash\necho not-edn\n")
    (.setExecutable script true)
    script))

;; @spec MCP-OP-ALIAS-059
(deftest the-enumeration-sees-every-namespace-the-entrance-can-refuse-through
  ;; Round-twelve review finding 3: `refusal-kinds-in-source` scanned SIX
  ;; FIXED FILES and called the result "every refusal kind the entrance can
  ;; emit". `mcp_change_buffer.clj:1445` mints `:invalid-diagnostic-output`,
  ;; `mcp_alias_migration.clj:2236-2240` forwards it verbatim, and no scanned
  ;; file contains it:
  ;;
  ;;   enumerated count => 36
  ;;   enumerated contains invalid-diagnostic-output? => false
  ;;   live error_type => "invalid-diagnostic-output"
  ;;   live source_unchanged => true
  ;;
  ;; A fixed list of sources is a fixed list however the kinds inside it are
  ;; derived. The subject is what the entrance can REACH, so the source set
  ;; must be derived from the require graph and not enumerated by hand.
  (let [kinds (refusal-kinds-in-source)]
    (testing "a kind forwarded from a helper namespace is in the set"
      (is (contains? kinds "invalid-diagnostic-output")
          (str "the enumeration does not reach mcp_change_buffer.clj, which "
               "the verb requires and whose refusals it forwards verbatim"))
      (is (contains? kinds "verification-unverified")
          "the enumeration does not see the helper's unverified refusal"))
    (testing "the kinds the fixed list could already see are still seen"
      (doseq [kind ["invalid-workspace-root" "mcp-adapter-failure"
                    "invalid-mcp-request" "alias-migration-empty-scope"
                    "unknown-verification-profile"
                    "invalid-mcp-operation-result"]]
        (is (contains? kinds kind)
            (str "the enumeration lost " kind))))))

;; @spec MCP-OP-ALIAS-059
(deftest a-helper-refusal-driven-live-renders-every-key-it-carries
  ;; The enumeration is derived from source; this drives the actual refusal
  ;; the review found, through the entrance, and applies the text ⊇ structured
  ;; contract to the receipt the verb really published — not to a synthetic
  ;; one built from a kind name.
  (let [workspace (workspace!)]
    (try
      (let [script (kondo-stub! workspace)
            result (execute! workspace
                             {:verify "focused"
                              :scope {:paths ["src/**"]}
                              :expect {:files 12}}
                             {:verification-profiles
                              {"focused" [(.getPath script)]}})]
        (is (false? (:ok result)) (pr-str result))
        (is (= "invalid-diagnostic-output" (:error_type result))
            (pr-str result))
        (is (true? (:source_unchanged result)) (pr-str result))
        (is (contains? (refusal-kinds-in-source) (:error_type result))
            (str "the enumeration does not carry the kind the entrance just "
                 "published: " (:error_type result)))
        (assert-refusal-text! (assoc result :elapsed_ms 1.0)
                              "live invalid-diagnostic-output"))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-059
(deftest no-reachable-namespace-spells-a-refusal-kind-dynamically
  ;; A kind built at runtime cannot be scanned out of source, so the only
  ;; way an enumeration can be complete is if no reachable namespace builds
  ;; one — except where the site is explicitly FORWARDING a kind another
  ;; scanned source minted. Every such site carries the marker
  ;; `forwarded-refusal-kind`; a new dynamic constructor anywhere reachable
  ;; carries no marker and fails here.
  (let [sites (unscannable-refusal-kind-sites)]
    (is (empty? sites)
        (str "refusal kinds spelled dynamically with no forwarding marker, "
             "which no source scan can ever enumerate: " (pr-str sites)))))

;; @spec MCP-OP-ALIAS-059
(deftest the-source-guard-exempts-only-the-forwarded-refusal-kind-marker
  ;; Round-fourteen review finding 2: `constructor-site?` treated ANY dynamic
  ;; `:error-type` expression that merely CONTAINS one of its enclosing
  ;; function's own parameters as the `refusal` constructor's legitimate
  ;; forward — far broader than the one constructor it was meant to exempt.
  ;; `(keyword (name kind))` and `(keyword "alias-migration-" (name x))` each
  ;; mention their enclosing function's sole parameter and each mints a live,
  ;; unscannable kind; the heuristic swallowed both silently. The only
  ;; legitimate exemption is the explicit `forwarded-refusal-kind` marker —
  ;; asserted here on both a marked and an unmarked instance of the same
  ;; shape, so the fix cannot be "stop scanning `:error_type` sites at all".
  (let [route-a (str "(defn- route-a-refusal\n"
                     "  [kind]\n"
                     "  {:ok false\n"
                     "   :operation \"alias_migration\"\n"
                     "   :error_type (keyword (name kind))\n"
                     "   :error \"routed refusal\"})\n")
        route-b (str "(defn- route-b-refusal\n"
                     "  [x]\n"
                     "  {:ok false\n"
                     "   :operation \"alias_migration\"\n"
                     "   :error_type (keyword \"alias-migration-\" (name x))\n"
                     "   :error \"routed refusal\"})\n")
        ;; @spec MCP-OP-ALIAS-059
        ;; round-FIFTEEN review finding 2: the marked exemplar used to be
        ;; `(keyword (name kind))` — a site that MINTS a kind and merely
        ;; CLAIMS to forward one. The marker is now checked against the
        ;; expression's shape, so that exemplar is named (asserted in
        ;; `the-forwarded-refusal-kind-marker-is-checked-and-not-merely-believed`)
        ;; and the marked exemplar here is a genuine forward: the incoming
        ;; kind relayed verbatim.
        route-a-marked (str "(defn- route-a-refusal-marked\n"
                            "  [kind]\n"
                            "  {:ok false\n"
                            "   :operation \"alias_migration\"\n"
                            "   ;; forwarded-refusal-kind: kind is this fn's own argument\n"
                            "   :error_type (name kind)\n"
                            "   :error \"routed refusal\"})\n")]
    (doseq [[label text]
            [["(keyword (name kind)) mentions the enclosing parameter" route-a]
             ["(keyword \"alias-migration-\" (name x)) mentions the enclosing parameter"
              route-b]]]
      (testing label
        (is (= 1 (count (runtime-spelled-kind-sites label text)))
            (str "a kind derived from an enclosing parameter with no "
                 "forwarded-refusal-kind marker was not named: "
                 (pr-str (runtime-spelled-kind-sites label text))))))
    (testing "the same shape marked forwarded-refusal-kind is exempt"
      (is (empty? (runtime-spelled-kind-sites "route-a-marked" route-a-marked))
          (str "a site explicitly marked forwarded-refusal-kind was still "
               "named: "
               (pr-str (runtime-spelled-kind-sites
                        "route-a-marked" route-a-marked)))))))

;; @spec MCP-OP-ALIAS-059
(deftest the-forwarded-refusal-kind-marker-is-checked-and-not-merely-believed
  ;; Round-fifteen review finding 2: with `constructor-site?` gone, the marker
  ;; became an UNCHECKED CAPABILITY — the guard exempts any dynamic site whose
  ;; preceding twelve lines contain the marker text, without ever establishing
  ;; that the marked expression FORWARDS a kind rather than MINTING one. The
  ;; reviewer planted a reachable helper spelling
  ;; `:error_type (keyword (:review_dynamic_kind params))` under the marker,
  ;; drove a live kind that no source scan could see, and all four advertised
  ;; enumeration witnesses stayed green over 20 assertions. A comment is not a
  ;; control: the marker must be coupled to a mechanically checked forwarding
  ;; SHAPE — a value selected from an incoming refusal — and must not exempt a
  ;; constructor of a new kind from data.
  (let [minting
        [["(keyword (:review_dynamic_kind params)) mints from request data"
          (str "(defn- route-minting-refusal\n"
               "  [params]\n"
               "  {:ok false\n"
               "   :operation \"alias_migration\"\n"
               "   ;; forwarded-refusal-kind: claims to forward, mints\n"
               "   :error_type (keyword (:review_dynamic_kind params))\n"
               "   :error \"routed refusal\"})\n")]
         ["(keyword (name kind)) mints a new kind from a parameter"
          (str "(defn- route-a-refusal-marked\n"
               "  [kind]\n"
               "  {:ok false\n"
               "   :operation \"alias_migration\"\n"
               "   ;; forwarded-refusal-kind: kind is this fn's own argument\n"
               "   :error_type (keyword (name kind))\n"
               "   :error \"routed refusal\"})\n")]
         ["(str \"heldout-\" (name kind)) composes a kind from a literal"
          (str "(defn- route-composed-refusal\n"
               "  [kind]\n"
               "  {:ok false\n"
               "   :operation \"alias_migration\"\n"
               "   ;; forwarded-refusal-kind: composed, not forwarded\n"
               "   :error_type (str \"heldout-\" (name kind))\n"
               "   :error \"routed refusal\"})\n")]]
        forwarding
        [["(name error-type) forwards the constructor's own argument"
          (str "(defn refusal\n"
               "  [error-type message]\n"
               "  {:ok false\n"
               "   ;; forwarded-refusal-kind: forwarded verbatim\n"
               "   :error_type (name error-type)\n"
               "   :error message})\n")]
         ["(:error-type source) selects an incoming refusal's own kind"
          (str "(defn- retire-refusal\n"
               "  [retire-source]\n"
               "  {:ok false\n"
               "   ;; forwarded-refusal-kind: travels verbatim\n"
               "   :error-type (:error-type retire-source)\n"
               "   :error \"retired\"})\n")]
         ["a bare symbol is the kind itself"
          (str "(defn- refuse!\n"
               "  ;; forwarded-refusal-kind: forwarded verbatim\n"
               "  [error-type message]\n"
               "  (throw (ex-info message {:error-type error-type})))\n")]
         ["(or (some-> (or (:error-type c) (:error_type c)) name) literal)"
          (str "(defn- commit-refusal\n"
               "  [commit]\n"
               "  {:ok false\n"
               "   ;; forwarded-refusal-kind: the kernel's own kind travels verbatim\n"
               "   :error_type (or (some-> (or (:error-type commit)\n"
               "                                (:error_type commit)) name)\n"
               "                   \"alias-migration-transaction-refused\")\n"
               "   :error \"refused\"})\n")]]]
    (doseq [[label text] minting]
      (testing (str "a marked MINTING site is still named · " label)
        (is (= 1 (count (runtime-spelled-kind-sites label text)))
            (str "a site that MINTS a kind was exempted by an unchecked "
                 "forwarded-refusal-kind marker: "
                 (pr-str (runtime-spelled-kind-sites label text))))))
    (doseq [[label text] forwarding]
      (testing (str "a marked FORWARDING site stays exempt · " label)
        (is (empty? (runtime-spelled-kind-sites label text))
            (str "a genuinely forwarded kind was named: "
                 (pr-str (runtime-spelled-kind-sites label text))))))
    (testing "a marked MINTING (refusal …) call site is still named"
      (let [text (str "(defn refusal\n"
                      "  [error-type message]\n"
                      "  {:ok false :error_type (name error-type) :error message})\n"
                      "\n"
                      "(defn- route-minting-call\n"
                      "  [params]\n"
                      "  ;; forwarded-refusal-kind: claims to forward, mints\n"
                      "  (refusal (keyword (:review_dynamic_kind params))\n"
                      "           \"routed refusal\"))\n")]
        (is (= 1 (count (dynamic-refusal-kind-sites-in "plant" text)))
            (str "a (refusal …) call that MINTS a kind was exempted by an "
                 "unchecked marker: "
                 (pr-str (dynamic-refusal-kind-sites-in "plant" text))))))
    (testing "a marked FORWARDING (refusal …) call site stays exempt"
      (let [text (str "(defn refusal\n"
                      "  [error-type message]\n"
                      "  {:ok false :error_type (name error-type) :error message})\n"
                      "\n"
                      "(defn- commit-refusal\n"
                      "  [commit]\n"
                      "  ;; forwarded-refusal-kind: the kernel's own kind travels verbatim\n"
                      "  (refusal (or (some-> (:error-type commit) name)\n"
                      "               \"alias-migration-transaction-refused\")\n"
                      "           \"refused\"))\n")]
        (is (empty? (dynamic-refusal-kind-sites-in "plant" text))
            (str "a genuinely forwarded (refusal …) kind was named: "
                 (pr-str (dynamic-refusal-kind-sites-in "plant" text))))))))

;; @spec MCP-OP-ALIAS-059
(deftest the-refusal-constructor-is-decided-by-the-reader-and-not-by-a-regex
  ;; Round-seventeen review finding 1: whether the `(refusal …)` scan runs AT
  ;; ALL for a source was decided by a TEXT regex over the constructor's
  ;; argument NAME, `#"\(defn-?\s+refusal\s*\n?\s*\[\s*(error-type|kind)\b"`.
  ;; `\s*\[` cannot cross a docstring, an attribute map, a multi-arity body or
  ;; a `(def refusal (fn …))`, so five of the reviewer's six constructor
  ;; shapes switched the guard off for EVERY site in the file, while
  ;; `refusal-call-sites-in` found the planted site in all six — the form scan
  ;; was right and the per-file enable threw its answer away.
  ;;
  ;; It is not hypothetical: `src/clj_surgeon/mcp_workspace.clj:8` carries a
  ;; docstring today, so its four `(refusal …)` sites are skipped wholesale
  ;; and the gate reaches the right answer for the wrong reason. Adding a
  ;; docstring to that constructor is the single most house-style-encouraged
  ;; edit anyone could make to `mcp_alias_migration.clj`, and it disabled the
  ;; guard for all fifteen of its sites with no test going red anywhere —
  ;; `a-gate-a-caller-can-turn-off`, reproduced end to end by the reviewer
  ;; with a live kind the enumeration does not contain.
  ;;
  ;; The constructor is now decided with the READER, and the question asked of
  ;; it is the one that matters: does its FIRST parameter reach the
  ;; `:error_type`/`:error-type` value the constructor publishes? An argument
  ;; name is a naming convention; a parameter that spells the kind is the
  ;; thing itself.
  (let [planted (str "\n(defn- route-dynamic\n"
                     "  [params]\n"
                     "  (refusal (keyword (:review_kind params))\n"
                     "           \"planted dynamic kind\"\n"
                     "           {}))\n")
        planted-row (fn [text]
                      (inc (count (take-while
                                    #(not (str/includes? % "(refusal (keyword"))
                                    (str/split-lines text)))))]
    (testing "every constructor shape that takes the kind first enables the scan"
      (doseq [[label constructor]
              [["canonical"
                (str "(defn refusal\n"
                     "  [error-type message]\n"
                     "  {:ok false :error_type (name error-type) :error message})\n")]
               ["with a docstring"
                (str "(defn refusal\n"
                     "  \"One refusal, carrying its own cause and remedy.\"\n"
                     "  [error-type message]\n"
                     "  {:ok false :error_type (name error-type) :error message})\n")]
               ["multi-arity"
                (str "(defn refusal\n"
                     "  ([error-type message]\n"
                     "   {:ok false :error_type (name error-type) :error message})\n"
                     "  ([error-type message extra]\n"
                     "   (merge {:ok false :error_type (name error-type)\n"
                     "           :error message}\n"
                     "          extra)))\n")]
               ["(def refusal (fn …))"
                (str "(def refusal\n"
                     "  (fn [error-type message]\n"
                     "    {:ok false :error_type (name error-type)\n"
                     "     :error message}))\n")]
               ["with an attribute map"
                (str "(defn refusal\n"
                     "  {:added \"1.0\"}\n"
                     "  [error-type message]\n"
                     "  {:ok false :error_type (name error-type) :error message})\n")]
               ["a first parameter named `k`"
                (str "(defn refusal\n"
                     "  [k message]\n"
                     "  {:ok false :error_type (name k) :error message})\n")]]]
        (testing label
          (let [text (str constructor planted)]
            (is (= [(str "plant:" (planted-row text))]
                   (dynamic-refusal-kind-sites-in "plant" text))
                (str "the planted dynamic site was not reported for a "
                     "constructor shaped `" label "` — one ordinary edit to "
                     "the constructor switched the whole file's scan off: "
                     (pr-str (dynamic-refusal-kind-sites-in "plant" text))))))))
    (testing "a constructor whose kind is a CONSTANT enables nothing"
      ;; `mcp_workspace`'s own shape: `[message value]`, spelling its one kind
      ;; as a literal INSIDE the constructor, where the `:error_type "…"` scan
      ;; already has it. Its call sites carry no kind at all, and naming them
      ;; would be a false positive on every one.
      (let [text (str "(defn- refusal\n"
                      "  \"One stable workspace-root refusal.\"\n"
                      "  [message value]\n"
                      "  {:ok false :error_type \"invalid-workspace-root\"\n"
                      "   :error message :workspace_root value})\n"
                      "\n(defn canonical-root\n"
                      "  [value]\n"
                      "  (refusal \"workspace_root must be absolute\" value))\n")]
        (is (empty? (dynamic-refusal-kind-sites-in "plant" text))
            (str "a constructor that takes no kind had its call sites read as "
                 "dynamic kind sites: "
                 (pr-str (dynamic-refusal-kind-sites-in "plant" text))))))))

;; @spec MCP-OP-ALIAS-059
(deftest the-refusal-kind-argument-is-located-by-reading-the-constructor
  ;; Round-eighteen review findings 1 and 9. The `(refusal …)` scan was gated
  ;; on a PER-FILE enable — `own-refusal-constructor-takes-kind?` — that asked
  ;; whether the constructor's FIRST parameter reaches the `:error_type` value
  ;; it publishes. A constructor shaped `[message error-type extra]` answers
  ;; no, so every call site in that file was skipped wholesale; the reviewer
  ;; drove `planted-runtime-kind` live through the entrance while the scan
  ;; reported no dynamic site, no unscannable site, and a still-139-member
  ;; enumeration. A guard whose subject is decided by the SHAPE of a helper is
  ;; a guard an ordinary refactor turns off — `a-gate-a-caller-can-turn-off`,
  ;; paid for a third time.
  ;;
  ;; The per-file enable is GONE. Every `(refusal …)` call in a reachable
  ;; namespace is a site, and the constructor is read for one thing only:
  ;; WHICH ARGUMENT carries the kind. The reader follows the parameter that
  ;; flows into the published `:error-type`/`:error_type`/`:kind` value back to
  ;; the position a caller fills — second, third, destructured out of a map,
  ;; arriving through a `&` rest argument, or a different position per arity.
  ;; A constructor whose body the reader cannot classify does not disable
  ;; anything: every site in that file becomes a TYPED unscannable entry, named
  ;; by the same scan, never silently exempt.
  (let [planted-row (fn [text]
                      (inc (count (take-while
                                    #(not (re-find #"^\s+\(refusal\b" %))
                                    (str/split-lines text)))))
        two-arities (str "(defn refusal\n"
                         "  ([error-type message]\n"
                         "   {:ok false :error_type (name error-type) :error message})\n"
                         "  ([message error-type extra]\n"
                         "   (merge {:ok false :error_type (name error-type)\n"
                         "           :error message}\n"
                         "          extra)))\n")]
    (testing "a dynamic kind is NAMED wherever the constructor takes it"
      (doseq [[label text]
              [["the SECOND parameter"
                (str "(defn refusal\n"
                     "  [message error-type extra]\n"
                     "  {:ok false :error message :error_type (name error-type)})\n"
                     "\n(defn- route-dynamic\n"
                     "  [params]\n"
                     "  (refusal \"planted dynamic kind\"\n"
                     "           (keyword (:review_kind params))\n"
                     "           {}))\n")]
               ["the THIRD parameter"
                (str "(defn refusal\n"
                     "  [message extra error-type]\n"
                     "  {:ok false :error message :error_type (name error-type)})\n"
                     "\n(defn- route-dynamic\n"
                     "  [params]\n"
                     "  (refusal \"planted dynamic kind\"\n"
                     "           {}\n"
                     "           (keyword (:review_kind params))))\n")]
               ["a `{:keys [kind]}` destructuring"
                (str "(defn refusal\n"
                     "  [{:keys [kind message]}]\n"
                     "  {:ok false :error message :error_type (name kind)})\n"
                     "\n(defn- route-dynamic\n"
                     "  [params]\n"
                     "  (refusal {:message \"planted dynamic kind\"\n"
                     "            :kind (keyword (:review_kind params))}))\n")]
               ["a `&` rest argument"
                (str "(defn refusal\n"
                     "  [message & [error-type extra]]\n"
                     "  {:ok false :error message :error_type (name error-type)})\n"
                     "\n(defn- route-dynamic\n"
                     "  [params]\n"
                     "  (refusal \"planted dynamic kind\"\n"
                     "           (keyword (:review_kind params))\n"
                     "           {}))\n")]
               ["the three-argument arity of a two-arity constructor"
                (str two-arities
                     "\n(defn- route-dynamic\n"
                     "  [params]\n"
                     "  (refusal \"planted dynamic kind\"\n"
                     "           (keyword (:review_kind params))\n"
                     "           {}))\n")]]]
        (testing label
          (is (= [(str "plant:" (planted-row text))]
                 (dynamic-refusal-kind-sites-in "plant" text))
              (str "a kind spelled at runtime and passed as " label
                   " was not named — the scan read the wrong argument or "
                   "skipped the file: "
                   (pr-str (dynamic-refusal-kind-sites-in "plant" text)))))))
    (testing "the enumeration reads the kind ARGUMENT and not every argument"
      (doseq [[label text expected]
              [["the two-argument arity spells its kind first"
                (str two-arities
                     "\n(defn- route-literal\n"
                     "  [_]\n"
                     "  (refusal :two-arg-kind \"plain-message\"))\n")
                #{"two-arg-kind"}]
               ["the three-argument arity spells its kind second"
                (str two-arities
                     "\n(defn- route-literal\n"
                     "  [_]\n"
                     "  (refusal \"plain-message\" :second-kind {}))\n")
                #{"second-kind"}]
               ["a second-parameter constructor"
                (str "(defn refusal\n"
                     "  [message error-type extra]\n"
                     "  {:ok false :error message :error_type (name error-type)})\n"
                     "\n(defn- route-literal\n"
                     "  [_]\n"
                     "  (refusal \"plain-message\" :planted-kind {}))\n")
                #{"planted-kind"}]]]
        (testing label
          (is (= expected (set (literal-refusal-kinds-in text)))
              (str "the enumeration did not read exactly the kind argument for "
                   label " — a message read as a kind is a phantom, a kind "
                   "read as a message is a hole: "
                   (pr-str (set (literal-refusal-kinds-in text)))))
          (is (empty? (dynamic-refusal-kind-sites-in "plant" text))
              (str "a site spelling a literal kind was named as dynamic for "
                   label ": "
                   (pr-str (dynamic-refusal-kind-sites-in "plant" text)))))))
    (testing "a constructor the reader cannot classify makes every site unscannable"
      (doseq [[label text]
              [["a bare `& args` relayed through a `let`"
                (str "(defn refusal\n"
                     "  [& args]\n"
                     "  (let [[message error-type] args]\n"
                     "    {:ok false :error message :error_type (name error-type)}))\n"
                     "\n(defn- route-plain\n"
                     "  [_]\n"
                     "  (refusal \"plain-message\" :ordinary-kind {}))\n")]
               ["no constructor in the file at all"
                (str "(defn- route-plain\n"
                     "  [_]\n"
                     "  (refusal :ordinary-kind \"plain-message\" {}))\n")]]]
        (testing label
          (is (= [(str "plant:" (planted-row text))]
                 (dynamic-refusal-kind-sites-in "plant" text))
              (str "a site whose kind argument the reader cannot locate — "
                   label " — was silently exempt instead of reported "
                   "unscannable: "
                   (pr-str (dynamic-refusal-kind-sites-in "plant" text))))
          (is (empty? (literal-refusal-kinds-in text))
              (str "an unscannable site's arguments were enumerated as kinds "
                   "for " label ": "
                   (pr-str (literal-refusal-kinds-in text)))))))
    (testing "a constructor whose kind is a CONSTANT still enables nothing"
      ;; `mcp_workspace`'s and `extract_header`'s own shape: no parameter
      ;; reaches the published `:error_type`, so the call sites carry no kind
      ;; and the `:error_type "…"` scan inside the constructor already has it.
      (let [text (str "(defn- refusal\n"
                      "  \"One stable workspace-root refusal.\"\n"
                      "  [message value]\n"
                      "  {:ok false :error_type \"invalid-workspace-root\"\n"
                      "   :error message :workspace_root value})\n"
                      "\n(defn canonical-root\n"
                      "  [value]\n"
                      "  (refusal \"workspace_root must be absolute\" value))\n")]
        (is (empty? (dynamic-refusal-kind-sites-in "plant" text))
            (str "a constructor that takes no kind had its call sites read as "
                 "dynamic kind sites: "
                 (pr-str (dynamic-refusal-kind-sites-in "plant" text))))
        (is (empty? (literal-refusal-kinds-in text))
            (str "a constructor that takes no kind had its call arguments "
                 "enumerated as kinds: "
                 (pr-str (literal-refusal-kinds-in text))))))))

;; @spec MCP-OP-ALIAS-059
(deftest the-forwarded-kind-check-is-form-deep-and-not-merely-head-shaped
  ;; Round-sixteen review finding 1: the shape check collected the expression's
  ;; LISTS and tested only each list's FIRST child against the forwarding-head
  ;; allowlist. In a threading form the minting function is a bare symbol in an
  ;; ARGUMENT position and is never a list head, so `keyword`, `symbol` and
  ;; `str` walked straight through the allowlist that exists to stop them:
  ;; `(some-> kind name keyword)` — the brief's own named attack — was exempt,
  ;; and the reviewer drove it end to end through the entrance with a live kind
  ;; (`planted-runtime-kind`) absent from the enumeration while all four
  ;; advertised witnesses stayed green. A `get`/`get-in` against a LITERAL
  ;; TABLE was exempt for the same reason: `get` selects, but the value it
  ;; selects is a keyword literal minted inside the table, and an exempt site's
  ;; minted keywords never reach the enumeration. So is a literal bound one
  ;; line above and relayed as a bare symbol.
  ;;
  ;; The check is now form-DEEP: a minting symbol ANYWHERE in the expression,
  ;; or a keyword literal in any position that is not a lookup head or a
  ;; keyword-as-function argument of a sequence selector, is a MINT.
  (testing "a minting symbol in ARGUMENT position is a mint, not a forward"
    (let [text (str "(defn refusal\n"
                    "  [error-type message]\n"
                    "  {:ok false :error_type (name error-type) :error message})\n"
                    "\n"
                    "(defn- route-threading-mint\n"
                    "  [params]\n"
                    "  ;; forwarded-refusal-kind: claims to forward, mints\n"
                    "  (refusal (some-> (:review_kind params) name keyword)\n"
                    "           \"routed refusal\"))\n")]
      (is (= 1 (count (dynamic-refusal-kind-sites-in "plant" text)))
          (str "a threading form whose minting fn is a bare symbol in argument "
               "position was exempted by the head-only shape check: "
               (pr-str (dynamic-refusal-kind-sites-in "plant" text))))))
  (testing "the reviewer's end-to-end plant is named at its :error_type shape"
    (let [text (str "(defn- validate-request\n"
                    "  [params]\n"
                    "  ;; forwarded-refusal-kind: claims to forward, mints\n"
                    "  {:ok false\n"
                    "   :error_type (some-> (:review_kind params) name keyword)\n"
                    "   :error \"planted\"})\n")]
      (is (= 1 (count (runtime-spelled-kind-sites "plant" text)))
          (str "the planted threading mint was exempted: "
               (pr-str (runtime-spelled-kind-sites "plant" text))))))
  (testing "a literal TABLE mints the kinds it holds, marker or no marker"
    (doseq [[label expression kind]
            [["get against a literal table"
              "(get {:a :brand-new-kind} kind)" "brand-new-kind"]
             ["get-in against a nested literal table"
              "(get-in {:a {:b :table-minted}} [kind :b])" "table-minted"]]]
      (testing label
        (let [text (str "(defn- route-table-refusal\n"
                        "  [kind]\n"
                        "  {:ok false\n"
                        "   ;; forwarded-refusal-kind: claims to forward, mints\n"
                        "   :error_type " expression "\n"
                        "   :error \"routed refusal\"})\n")]
          (is (contains? (structural-error-type-kinds text) kind)
              (str "a keyword minted inside a literal table under the marker "
                   "never reached the enumeration: "
                   (pr-str (structural-error-type-kinds text))))))))
  (testing "a literal bound above the site and relayed is a mint"
    (let [text (str "(defn- route-let-bound\n"
                    "  [params]\n"
                    "  (let [planted :planted-let-kind]\n"
                    "    {:ok false\n"
                    "     ;; forwarded-refusal-kind: claims to forward, mints\n"
                    "     :error_type planted\n"
                    "     :error \"routed refusal\"}))\n")]
      (is (= 1 (count (runtime-spelled-kind-sites "plant" text)))
          (str "a bare symbol bound to a keyword LITERAL one line above was "
               "read as a forward: "
               (pr-str (runtime-spelled-kind-sites "plant" text))))))
  ;; @spec MCP-OP-ALIAS-059
  ;; Round-seventeen review finding 2: `mint-evidence` read KEYWORD literals
  ;; and was blind to STRING literals, while every `refusal` constructor in
  ;; the reachable set forwards its kind through `(name error-type)` — and
  ;; `(name "brand-new-kind")` is `"brand-new-kind"`, so a string kind is a
  ;; perfectly good kind. The blindness hid the hole in BOTH directions:
  ;; `minted-kinds-in` already read strings, so at an `:error-type` value site
  ;; the marked expression stayed exempt and the enumeration read nothing from
  ;; it, while `runtime-spelled-kind-sites`' `(empty? (minted-kinds-in …))`
  ;; test was false and did not name it either. The reviewer drove it end to
  ;; end: a marked string-table `get` at `validate-request` emitted the live
  ;; kind `brand-new-kind` with `dynamic-sites` empty and the enumeration at
  ;; 139. Rows I and O of the reviewer's table, beside their keyword twins
  ;; above.
  (testing "a STRING literal is a kind source exactly as a keyword literal is"
    (doseq [[label expression kind]
            [["get against a literal STRING table"
              "(get {\"a\" \"brand-new-kind\"} kind)" "brand-new-kind"]
             ["get-in against a nested literal STRING table"
              "(get-in {\"a\" {\"b\" \"table-minted\"}} [kind \"b\"])"
              "table-minted"]]]
      (testing label
        (let [text (str "(defn- route-string-table-refusal\n"
                        "  [kind]\n"
                        "  {:ok false\n"
                        "   ;; forwarded-refusal-kind: claims to forward, mints\n"
                        "   :error_type " expression "\n"
                        "   :error \"routed refusal\"})\n")]
          (is (contains? (structural-error-type-kinds text) kind)
              (str "a STRING kind minted inside a literal table under the "
                   "marker never reached the enumeration: "
                   (pr-str (structural-error-type-kinds text))))))))
  (testing "a STRING literal bound above the site and relayed is a mint"
    (let [text (str "(defn- route-let-bound-string\n"
                    "  [params]\n"
                    "  (let [planted \"planted-str-kind\"]\n"
                    "    {:ok false\n"
                    "     ;; forwarded-refusal-kind: claims to forward, mints\n"
                    "     :error_type planted\n"
                    "     :error \"routed refusal\"}))\n")]
      (is (= 1 (count (runtime-spelled-kind-sites "plant" text)))
          (str "a bare symbol bound to a STRING literal one line above was "
               "read as a forward: "
               (pr-str (runtime-spelled-kind-sites "plant" text))))))
  (testing "a kind a marked (refusal …) site can spell reaches the enumeration"
    ;; The reviewer's plantI at its own shape. At a `(refusal …)` call the
    ;; `:error_type` scan is not the reader — `literal-refusal-kinds-in` is,
    ;; and it read a keyword at the kind position and nothing else — so a
    ;; marked expression that can hand back a LITERAL kind was enumerated
    ;; nowhere and named nowhere. Every site is now one or the other: NAMED,
    ;; because it spells its kind at runtime and no scan can read it, or
    ;; ENUMERATED, because every literal it can hand back is in the set.
    ;; Never neither, which is exactly what plantI was.
    (let [text (str "(defn refusal\n"
                    "  [error-type message]\n"
                    "  {:ok false :error_type (name error-type) :error message})\n"
                    "\n(defn- validate-request\n"
                    "  [params]\n"
                    "  ;; forwarded-refusal-kind: relays a kind another source minted\n"
                    "  (refusal (get {\"planted-runtime-kind\" \"brand-new-kind\"}\n"
                    "                (:review_kind params))\n"
                    "           \"planted string-table kind\"\n"
                    "           {}))\n")]
      (is (contains? (set (literal-refusal-kinds-in text)) "brand-new-kind")
          (str "a kind a marked (refusal …) site can hand back reached "
               "neither the guard nor the enumeration: "
               (pr-str (literal-refusal-kinds-in text))))))
  (testing "a (refusal …) site that can spell NO readable kind is still named"
    (let [text (str "(defn refusal\n"
                    "  [error-type message]\n"
                    "  {:ok false :error_type (name error-type) :error message})\n"
                    "\n(defn- validate-request\n"
                    "  [params]\n"
                    "  ;; forwarded-refusal-kind: claims to forward, mints\n"
                    "  (refusal (keyword (:review_kind params))\n"
                    "           \"planted dynamic kind\"\n"
                    "           {}))\n")]
      (is (= 1 (count (dynamic-refusal-kind-sites-in "plant" text)))
          (str "a (refusal …) call that spells its kind entirely at runtime "
               "was not named: "
               (pr-str (dynamic-refusal-kind-sites-in "plant" text))))))
  (testing "the reachable set's real forwarding shapes stay exempt"
    (doseq [[label text]
            [["(some :error-type (remove :ok checks)) — the change buffer's own"
              (str "(defn- verify-refusal\n"
                   "  [checks]\n"
                   "  {:ok false\n"
                   "   ;; forwarded-refusal-kind: the failing check's OWN kind\n"
                   "   :error-type (some :error-type (remove :ok checks))\n"
                   "   :error \"verification refused\"})\n")]
             ["a symbol bound to a HELPER CALL — mcp_cold_verify's own"
              (str "(defn- run-job!\n"
                   "  [process]\n"
                   "  (let [authority-error (analyzer-authority-error-type process)]\n"
                   "    {:ok true\n"
                   "     ;; forwarded-refusal-kind: the kind the helper mints\n"
                   "     :error-type authority-error\n"
                   "     :exit 1}))\n")]]]
      (testing label
        (is (empty? (runtime-spelled-kind-sites label text))
            (str "a genuinely forwarded kind was named: "
                 (pr-str (runtime-spelled-kind-sites label text))))))))

;; @spec MCP-OP-ALIAS-059
(deftest the-refusal-call-site-scan-reads-forms-and-not-lines
  ;; Round-sixteen review finding 2, proven both ways. `(refusal …)` sites were
  ;; discovered by a per-LINE regex, so a call whose kind begins on the NEXT
  ;; line was invisible to the guard, and the word `refusal` inside a STRING or
  ;; a COMMENT was a false positive. The enumeration's own literal scan,
  ;; `#"\(refusal :([a-z][a-z0-9-]*)"`, is anchored on a single space and has
  ;; the same newline hole — so a literal kind written on the next line would
  ;; be missed by the enumeration AND by the guard that exists to catch what
  ;; the enumeration cannot see.
  ;;
  ;; Deciding a call site by TEXT in a file the namespace is already parsing is
  ;; the defect class this requirement has now paid for twice. Both scans read
  ;; FORMS: strings and comments cannot be call sites by construction, and a
  ;; call is one form however many lines it occupies.
  (let [constructor (str "(defn refusal\n"
                         "  [error-type message]\n"
                         "  {:ok false :error_type (name error-type)"
                         " :error message})\n\n")]
    (testing "a MINTING call whose kind begins on the next line is named"
      (let [text (str constructor
                      "(defn- route-next-line\n"
                      "  [params]\n"
                      "  (refusal\n"
                      "    (keyword (:review_kind params))\n"
                      "    \"routed refusal\"))\n")]
        (is (= 1 (count (dynamic-refusal-kind-sites-in "plant" text)))
            (str "a (refusal …) call whose kind starts on the NEXT line was "
                 "invisible to a per-line scan: "
                 (pr-str (dynamic-refusal-kind-sites-in "plant" text))))))
    (testing "a LITERAL kind on the next line still reaches the enumeration"
      (let [text (str constructor
                      "(defn- route-next-line-literal\n"
                      "  [params]\n"
                      "  (refusal\n"
                      "    :planted-next-line-kind\n"
                      "    \"routed refusal\"))\n")]
        (is (contains? (set (literal-refusal-kinds-in text))
                       "planted-next-line-kind")
            (str "a literal kind written on the line below its (refusal was "
                 "missed by the enumeration's own scan: "
                 (pr-str (literal-refusal-kinds-in text))))))
    (testing "the word `refusal` inside a STRING is not a call site"
      (let [text (str constructor
                      "(defn- describe\n"
                      "  []\n"
                      "  \"a (refusal x) call names its kind first\")\n")]
        (is (empty? (dynamic-refusal-kind-sites-in "plant" text))
            (str "the word `refusal` inside a string was reported as a "
                 "dynamic call site: "
                 (pr-str (dynamic-refusal-kind-sites-in "plant" text))))))
    (testing "the word `refusal` inside a COMMENT is not a call site"
      (let [text (str constructor
                      "(defn- describe\n"
                      "  []\n"
                      "  ;; (refusal x) is how the kind is spelled\n"
                      "  nil)\n")]
        (is (empty? (dynamic-refusal-kind-sites-in "plant" text))
            (str "the word `refusal` inside a comment was reported as a "
                 "dynamic call site: "
                 (pr-str (dynamic-refusal-kind-sites-in "plant" text))))))))

;; @spec MCP-OP-ALIAS-059
(def ^:private frozen-refusal-kinds
  "The 145 kinds the entrance's enumeration holds on the MCP/main landing (139 at 51da9446 + four trunk kinds + two census kinds).

  A PIN, not a source: `refusal-kinds-in-source` stays derived, and this set
  exists so that a change to the derivation is LOUD. Round sixteen shipped a
  spurious 140th kind, `ok`, read out of `(some :error-type (remove :ok
  checks))` when a head allowlist mis-classified a forward; nothing asserted
  the count or the set, so it changed in silence and was found by a reviewer
  reading the enumeration by hand. A kind added here on purpose is one line of
  diff with a reason; a kind that appears here by accident is a failing test."
  #{
    ;; re-pinned at the MCP/main landing (2026-09-04): four kinds the trunk publishes (the admit-gate
    ;; and parser-admission landings) that the branch never saw — 139 → 143
    "expect-matched-invalid-pattern" "expect-matched-stale" "expect-matched-unreadable-source" "parser-admission-refused"
    "alias-migration-alias-policy-exhausted"
    "alias-migration-ambiguous-ownership"
    "alias-migration-discovery-incomplete" "alias-migration-empty-scope"
    "alias-migration-expect-mismatch" "alias-migration-indirect-reference"
    "alias-migration-mixed-var-spec"
    "alias-migration-receipt-detail-collision"
    "alias-migration-receipt-dir-escapes"
    "alias-migration-receipt-dir-in-control-directory"
    "alias-migration-receipt-published-elsewhere"
    "alias-migration-resource-exhausted" "alias-migration-retire-failed"
    "alias-migration-retire-path-refused"
    "alias-migration-retire-symlink-refused"
    "alias-migration-scope-matches-nothing"
    "alias-migration-scope-path-refused" "alias-migration-scope-too-deep"
    "alias-migration-scope-too-large" "alias-migration-scope-too-large-bytes"
    "alias-migration-scope-unreadable" "alias-migration-source-too-large"
    "alias-migration-target-lib-exists" "alias-migration-transaction-refused"
    "alias-migration-walk-too-large" "ambiguous-change-subject"
    "ambiguous-form" "ambiguous-match" "analyzer-authority-unverified"
    "analyzer-mission-budget-exhausted" "analyzer-mission-expired"
    "apply-failed" "atomic-write-failed" "basis-coverage-mismatch"
    "basis-edit-address-drift" "basis-edit-covers-owner"
    "basis-workspace-mismatch" "change-buffer-budget-exceeded"
    "clj-kondo-admission-unavailable" "clj-kondo-executable-unavailable"
    "cold-verification-capacity-exceeded" "cold-verification-exception"
    "diagnostic-output-truncated" "edit-requires-transform"
    "effect-capability-denied" "empty-basis-change" "exact-owner-ambiguous"
    "exact-owner-not-addressable" "exact-owner-not-found"
    "exact-owner-scope-unsupported" "exact-profile-not-project-owned"
    "expect-count-mismatch" "expect-mismatch"
    "expect-requires-literal-replacement" "file-read-failed"
    "future-source-transformation-failed" "hot-verification-connection-failed"
    "hot-verification-failed" "hot-verification-path-escape"
    "inside-not-found" "intent-compiler-failure"
    "invalid-analyzer-mission-scope" "invalid-basis-decision"
    "invalid-buffer-selection" "invalid-change-intent" "invalid-change-label"
    "invalid-change-scope" "invalid-change-subject"
    "invalid-compact-delete-result" "invalid-diagnostic-output"
    "invalid-diagnostic-snapshot" "invalid-exact-owner" "invalid-exact-owners"
    "invalid-exact-verification-profile" "invalid-expect"
    "invalid-hot-verification-port" "invalid-hot-verification-profile"
    "invalid-mcp-elapsed-time" "invalid-mcp-operation-result"
    "invalid-mcp-request" "invalid-operation-context"
    "invalid-operation-outcome" "invalid-plan" "invalid-plan-out"
    "invalid-process-deadline" "invalid-query" "invalid-replacement"
    "invalid-result-source" "invalid-source" "invalid-transaction-receipt"
    "invalid-workspace-root" "line-not-in-form" "mcp-adapter-failure"
    "missing-arguments" "missing-diagnostic-baseline" "missing-plan-out"
    "no-match" "plan-artifact-changed" "plan-artifact-repair-failed"
    "plan-overwrites-source" "plan-write-failed" "platform-context-required"
    "positional-mutation-authority-refused" "process-interrupted"
    "read-back-failed" "read-back-hash-mismatch" "read-back-invalid-source"
    "receipt-write-failed" "result-hash-mismatch"
    "semantic-evidence-incomplete" "semantic-owner-drift"
    "semantic-owner-not-found" "semantic-path-outside-project"
    "semantic-session-drift" "semantic-sites-not-addressable"
    "semantic-source-drift" "server-not-initialized" "source-hash-mismatch"
    "source-read-failed" "span-arity-mismatch" "stale-path" "stale-subform"
    "target-ancestor-changed" "transaction-recovery-required"
    "transaction-write-exception" "transaction-write-failed"
    "unchanged-basis-decision" "unknown-buffer-site"
    "unknown-or-expired-basis" "unknown-or-expired-verification-job"
    "unknown-verification-profile" "unsupported-arguments"
    "unsupported-buffer-context" "unsupported-plan-operation"
    "unsupported-plan-version" "verification-baseline-failed"
    "verification-failed" "verification-job-workspace-mismatch"
    "verification-unverified"
    ;; re-pinned at the census merge (2026-09-04): the two kinds the
    ;; relation-census verb publishes that the trunk enumeration never saw —
    ;; 143 → 145. Both are keyword literals in `relation_census.clj`
    ;; (`:census-worker-failure` at the pool boundary, `:unparseable-file` at
    ;; the per-file read), so the derivation reads them; they are pinned here
    ;; on purpose, one line of diff with a reason.
    "census-worker-failure" "unparseable-file"
    ;; re-pinned at the GHA round-one fix (2026-09-04, finding 1): 145 -> 146.
    ;; `:executable-unresolved` is the typed refusal MCP-OP-VERIFY-011 requires
    ;; of `mcp-change-buffer/expand-command` when a bare executable resolves in
    ;; no paved directory and nowhere on `PATH` -- the kind that replaced
    ;; silently returning the unresolved bare name to a caller that will exec
    ;; it. It is a keyword literal in `mcp_change_buffer.clj`, so this
    ;; source-reading derivation sees it. Note the two callers that own their
    ;; own typed kind (`mcp-admit-tool/kondo-findings`,
    ;; `mcp-formatter/format-candidates!`) TRANSLATE it, so it is minted in
    ;; source without being published at the admit entrance -- which is why
    ;; MCP-OP-ADMIT-133's entrance enumeration stays at 34 while this
    ;; source-derived one moves to 146.
    "executable-unresolved"})

;; @spec MCP-OP-ALIAS-059
(deftest the-refusal-enumeration-is-pinned-in-count-and-in-membership
  ;; The enumeration is DERIVED, and a derivation with no pin changes in
  ;; silence: round sixteen's `ok` was a live, spurious kind that no witness
  ;; could see. Both directions are asserted — a kind that appears and a kind
  ;; that vanishes are each a change to what a text-reading client is promised.
  (let [kinds (set (refusal-kinds-in-source))]
    (is (= 146 (count kinds))
        (str "the entrance's refusal enumeration changed size: "
             (count kinds) " kinds"))
    (is (empty? (clojure.set/difference kinds frozen-refusal-kinds))
        (str "a kind appeared in the enumeration that is not in the frozen "
             "set — a spurious kind, or a real one that needs pinning: "
             (pr-str (sort (clojure.set/difference kinds frozen-refusal-kinds)))))
    (is (empty? (clojure.set/difference frozen-refusal-kinds kinds))
        (str "a pinned kind vanished from the enumeration: "
             (pr-str (sort (clojure.set/difference frozen-refusal-kinds kinds)))))))

;; ---------------------------------------------------------------------------
;; every invisible or malformed code point in a scope entry is typed

;; @spec MCP-OP-ALIAS-061
(deftest every-invisible-or-malformed-code-point-in-a-scope-entry-is-typed
  ;; Round-twelve review finding 4: only U+0000 was typed. Every other
  ;; invisible spelling compiled as a glob, matched nothing, and was reported
  ;; as a fact about the TREE:
  ;;
  ;;   NUL                 => "alias-migration-scope-path-refused"
  ;;   SOH                 => "alias-migration-scope-matches-nothing"
  ;;   DEL                 => "alias-migration-scope-matches-nothing"
  ;;   C1-NEL              => "alias-migration-scope-matches-nothing"
  ;;   ZERO-WIDTH-SPACE    => "alias-migration-scope-matches-nothing"
  ;;   LONE-HIGH-SURROGATE => "alias-migration-scope-matches-nothing"
  ;;   LONE-LOW-SURROGATE  => "alias-migration-scope-matches-nothing"
  ;;
  ;; NUL is not special: it is the one member of a class. A code point the
  ;; caller cannot see in their own request is the cause a refusal most has to
  ;; name, and `scope-matches-nothing` names the wrong subject entirely — it
  ;; asserts something about the tree that the walk never established.
  ;;
  ;; Overlong UTF-8 cannot exist as a JVM string: Jackson normalises the
  ;; overlong `C0 AF` encoding of `/` to `/` before the verb sees it. What a
  ;; decoder that does NOT normalise emits is U+FFFD, so U+FFFD is the
  ;; observable trace of a malformed sequence and is refused as one.
  (let [workspace (temp-dir)]
    (try
      (write-tree! workspace {"src/two.clj" "(ns two)\n"})
      (doseq [[label code point]
              [["NUL" 0x0000 "U+0000"]
               ["SOH" 0x0001 "U+0001"]
               ["ESC" 0x001b "U+001B"]
               ["DEL" 0x007f "U+007F"]
               ["C1 NEL" 0x0085 "U+0085"]
               ["C1 APC" 0x009f "U+009F"]
               ["zero-width space" 0x200b "U+200B"]
               ["zero-width no-break space" 0xfeff "U+FEFF"]
               ["left-to-right override" 0x202e "U+202E"]
               ["lone high surrogate" 0xd800 "U+D800"]
               ["lone low surrogate" 0xdc00 "U+DC00"]
               ["replacement character" 0xfffd "U+FFFD"]]]
        (testing label
          (let [entry (str "src/" (char code) "x/**")
                result (scope-matches-nothing-refusal
                         workspace {:scope {:paths [entry]}})
                cause (str (:cause result))]
            (is (= "alias-migration-scope-path-refused" (:error_type result))
                (str label " was reported as a fact about the tree: "
                     (pr-str (:error_type result))))
            (is (str/includes? cause point)
                (str label ": the refusal does not name the code point "
                     point ": " (pr-str cause)))
            (is (str/includes? cause "index 4")
                (str label ": the refusal does not say where it is: "
                     (pr-str cause)))
            (is (true? (:source_unchanged result)) (pr-str result))
            (is (nil? (:next_call result))
                (str label ": a spelling only the caller knows was given a "
                     "mechanical call")))))
      (testing "an ordinary entry is untouched by the code-point gate"
        (let [result (scope-matches-nothing-refusal
                       workspace {:scope {:paths ["src/nope/**"]}})]
          (is (= "alias-migration-scope-matches-nothing" (:error_type result))
              (str "a printable entry was refused as a code point: "
                   (pr-str result)))))
      (testing "a printable non-ASCII entry is not a control character"
        (write-tree! workspace {"ρίζα/three.clj" "(ns three)\n"})
        (let [scan (alias-migration/scan-scope (.toPath workspace)
                                               {:paths ["ρίζα/**"]
                                                :exclude []})]
          (is (:ok scan) (pr-str scan))
          (is (= ["ρίζα/three.clj"] (:files scan)) (pr-str scan))))
      (finally
        (delete-tree! workspace)))))

;; ---------------------------------------------------------------------------
;; a mention site is what the READER reads

;; @spec MCP-OP-ALIAS-034
(deftest a-reader-discarded-string-is-not-a-mention-site
  ;; Round-twelve review finding 5: `string-literal-lines` descends every
  ;; rewrite-clj node and does not stop at `:uneval`, although the migration
  ;; walker itself stops there at alias_migration.clj:424. Over the six shapes
  ;; a string can take:
  ;;
  ;;   sites => ["src/shapes.clj:3" "src/shapes.clj:4"
  ;;             "src/shapes.clj:5" "src/shapes.clj:6"]
  ;;
  ;; line 5 is `#_ "acid.fanout.store/find-event"`, which the reader DROPS —
  ;;
  ;;   (read-string "[#_\"old.store/find-event\" :sentinel]") => [:sentinel]
  ;;
  ;; MCP-OP-ALIAS-034 says a string literal is what the READER says one is,
  ;; and publishes the count as EXACT. A discarded form is not read, so it is
  ;; not a site.
  ;;
  ;; `(comment …)` goes the OTHER way and stays a site. `comment` is an
  ;; ordinary macro: the reader READS its body and produces the string
  ;; literal; only evaluation discards it. The distinction the requirement
  ;; draws is the reader's, not the evaluator's — and it is the useful one,
  ;; because a commented-out call naming the retired var is exactly the stale
  ;; work an operator must go and look at, while a discarded form is text the
  ;; reader never built.
  (let [needle (str fixture/from-lib "/" fixture/from-var)
        source (str "(ns shapes)\n"
                    "(def s \"" needle "\")\n"
                    "(comment \"" needle "\")\n"
                    "#_ \"" needle "\"\n"
                    "(defn f \"" needle "\" [] 1)\n"
                    "(def r #\"" needle "\")\n"
                    "; " needle "\n")
        sites (planner/string-mentions needle
                                       [{:file "src/shapes.clj" :source source}])]
    (testing "the reader's own answer, on the same six shapes"
      (is (= [:sentinel] (read-string (str "[#_\"" needle "\" :sentinel]")))
          "the reader kept a discarded form, and this witness is wrong")
      (is (= ["src/shapes.clj:2" "src/shapes.clj:3" "src/shapes.clj:5"] sites)
          (str "a reader-discarded string was counted as a mention site: "
               (pr-str sites))))
    (testing "a discarded form nested inside live code is still discarded"
      (is (= []
             (planner/string-mentions
               needle
               [{:file "src/n.clj"
                 :source (str "(ns n)\n(def v [#_ \"" needle "\" :keep])\n")}]))
          "a discard inside a live vector was counted"))
    (testing "a discarded FORM containing the string is discarded whole"
      (is (= []
             (planner/string-mentions
               needle
               [{:file "src/d.clj"
                 :source (str "(ns d)\n#_ (def s \"" needle "\")\n")}]))
          "a discarded def carrying the string was counted"))
    (testing "the comment form and the docstring are still sites"
      (is (= ["src/c.clj:2"]
             (planner/string-mentions
               needle
               [{:file "src/c.clj"
                 :source (str "(ns c)\n(comment \"" needle "\")\n")}]))
          "the reader reads a comment form's body and this dropped it"))))

;; ---------------------------------------------------------------------------
;; the two faces are diffed on the CONSTRUCTED receipt, not on source text

;; @spec MCP-OP-ALIAS-059
(defn- live-refusal-scenarios
  "Refusals this verb really publishes, each driven through the entrance.

  Round-twelve review finding 6: the advertised regression witnesses keyed on
  SOURCE TEXT — `(refusal :kind` and `:error-type :kind` literals — and
  probed the renderer with the synthetic string value `\"probe-value\"`. Both
  stayed green under sabotage: a dynamically spelled kind was in neither
  scan, and a nested-map fact added to the live `alias-policy-exhausted`
  refusal was carried by structuredContent and absent from the text. A
  witness that never sees a constructed value cannot see what construction
  actually produced.

  Each scenario returns the receipt the verb published, so the text ⊇
  structured contract is applied to the real thing."
  []
  (let [scenarios (atom [])
        record! (fn [label result cleanup]
                  (swap! scenarios conj {:label label :result result})
                  (cleanup))]
    (let [workspace (workspace!)]
      (record! "scope-matches-nothing"
               (execute! workspace {:scope {:paths ["no-such-dir/**"]}})
               #(delete-tree! workspace)))
    (let [workspace (workspace!)]
      (record! "empty-scope"
               (execute! workspace {:scope {:paths ["src/acid/fanout/n0*.clj"]}
                                    :expect {:files 0}})
               #(delete-tree! workspace)))
    (let [workspace (workspace!)]
      (record! "scope-path-refused · unparseable glob"
               (execute! workspace {:scope {:paths ["src/{**"]}})
               #(delete-tree! workspace)))
    (let [workspace (workspace!)]
      (record! "scope-path-refused · invisible code point"
               (execute! workspace
                         {:scope {:paths [(str "src/" (char 1) "x/**")]}})
               #(delete-tree! workspace)))
    (let [workspace (workspace!)]
      (record! "expect-mismatch"
               (execute! workspace {:expect {:files 3}})
               #(delete-tree! workspace)))
    (let [workspace (workspace!)]
      (record! "unknown-verification-profile"
               (execute! workspace {:verify "no-such-profile"})
               #(delete-tree! workspace)))
    (let [workspace (workspace!)]
      (record! "invalid-mcp-request"
               (alias-migration/execute!
                 (config workspace (io/file workspace "receipts"))
                 (assoc (request workspace) :unknown_field 1))
               #(delete-tree! workspace)))
    (let [workspace (workspace!)]
      (record! "invalid-diagnostic-output"
               (let [script (kondo-stub! workspace)]
                 (execute! workspace
                           {:verify "focused"}
                           {:verification-profiles
                            {"focused" [(.getPath script)]}}))
               #(delete-tree! workspace)))
    (let [workspace (workspace!)]
      (record! "alias-policy-exhausted"
               (execute! workspace
                         {:to {:lib fixture/to-lib :var fixture/to-var
                               :alias_policy ["store2"]}
                          :scope {:paths ["src/**"]}
                          :expect {:files 12}})
               #(delete-tree! workspace)))
    ;; @spec MCP-OP-ALIAS-059
    ;; Round-thirteen review finding 2: the list omitted the ONE refusal the
    ;; reviewer sabotaged — the workspace router's, minted before the verb
    ;; runs. A list of "every live refusal" that leaves out an entrance is a
    ;; list, not an enumeration.
    (let [workspace (workspace!)]
      (record! "invalid-workspace-root"
               (let [captured (atom nil)]
                 (mcp-tool/init! (config workspace
                                         (io/file workspace "receipts")))
                 (mcp-tool/handle-alias-migration
                   nil
                   (json/parse-string
                     (json/generate-string
                       (request workspace {:workspace_root "relative/path"}))
                     true)
                   (fn [_content _error? structured] (reset! captured structured)))
                 @captured)
               #(delete-tree! workspace)))
    (let [workspace (backslash-tree!)]
      (record! "discovery-incomplete"
               (with-redefs-fn
                 {(resolve
                    'clj-surgeon.mcp-alias-migration/independent-scope-count)
                  (fn [& _] 99)}
                 #(execute! workspace {:scope {:paths ["**"]}
                                       :expect {:files 3}}))
               #(delete-tree! workspace)))
    @scenarios))

;; @spec MCP-OP-ALIAS-059
(deftest every-live-refusal-renders-every-key-its-receipt-carries
  ;; The two faces, diffed on ten receipts the verb actually published.
  ;; Whatever a key's VALUE turns out to be — scalar, vector, or a nested map
  ;; the renderer used to drop on the floor — the text names the key, because
  ;; a client reading the text must never be told less than one reading the
  ;; structure.
  (doseq [{:keys [label result]} (live-refusal-scenarios)]
    (testing label
      (is (false? (:ok result)) (str label " · " (pr-str result)))
      (let [receipt (cond-> result
                      (nil? (:elapsed_ms result)) (assoc :elapsed_ms 1.0))
            text (mcp-tool/alias-migration-summary receipt)
            missing (vec (sort (for [[field _] receipt
                                     :when (and (not (contains?
                                                       mcp-tool/alias-migration-refusal-envelope-keys
                                                       field))
                                                (not (str/includes?
                                                       text (name field))))]
                                 (name field))))]
        (is (empty? missing)
            (str label " · the text block drops "
                 (pr-str missing)
                 ", which structuredContent carries"))
        (is (not (str/includes? text "more in structuredContent"))
            (str label " · a live refusal is past the fact bound of "
                 mcp-tool/max-refusal-facts))
        (is (not (str/includes? text "refusal text truncated"))
            (str label " · a live refusal is past the "
                 alias-migration/max-refusal-text-characters
                 "-character text ceiling"))
        (is (<= (count text) alias-migration/max-refusal-text-characters)
            (str label " · " (count text) " characters"))))))

;; @spec MCP-OP-ALIAS-059
(deftest a-fact-of-any-shape-is-named-in-the-text-block
  ;; The sabotage the review ran, as a permanent witness. `renderable-fact?`
  ;; admitted strings, numbers, booleans and flat sequentials, and SILENTLY
  ;; DROPPED everything else — so a nested map added to a live refusal was
  ;; carried by structuredContent and absent from the text, and the
  ;; source-derived key witness never noticed because it probed every key
  ;; with the string "probe-value".
  ;;
  ;;   live sabotage key present structurally? => true
  ;;   live sabotage key named in text?        => false
  (doseq [[label value] [["a nested map" {:nested "value"}]
                         ["a map of maps" {:a {:b 1}}]
                         ["a vector of maps" [{:a 1}]]
                         ["a set" #{"a" "b"}]
                         ["a keyword" :some-keyword]
                         ["nil" nil]
                         ["a ratio" 3/4]]]
    (testing label
      (let [text (mcp-tool/alias-migration-summary
                   {:ok false
                    :operation "alias_migration"
                    :error_type "alias-migration-alias-policy-exhausted"
                    :error "one sentence stating the cause"
                    :elapsed_ms 1.25
                    :sabotage_detail value})]
        (is (str/includes? text "sabotage_detail")
            (str label " · the text block drops a fact structuredContent "
                 "carries: " text)))))
  (testing "a fact too large to render whole is elided, never dropped"
    (let [text (mcp-tool/alias-migration-summary
                 {:ok false
                  :operation "alias_migration"
                  :error_type "alias-migration-alias-policy-exhausted"
                  :error "one sentence stating the cause"
                  :elapsed_ms 1.25
                  :wide_detail (zipmap (map #(keyword (str "k" %)) (range 60))
                                       (range 60))})]
      (is (str/includes? text "wide_detail")
          "a wide fact was dropped rather than elided")
      (is (str/includes? text "…")
          "a wide fact was rendered whole past the per-fact bound"))))

;; @spec MCP-OP-ALIAS-058
;; @spec MCP-OP-ALIAS-059
(defn- root-listing-suggestion
  "A `root-sizes` input whose entries render at exactly the given lengths.

  Synthetic rather than a tree, because the subject is the ARITHMETIC of the
  listing budget and a tree can only approach its edges. Each entry is
  `<pattern> (9)`, so a pattern of `length - 4` characters renders at `length`,
  and no entry reaches `max-refusal-field-characters` and elides."
  [lengths]
  (let [patterns (vec (map-indexed
                        (fn [index length]
                          (str (char (+ (int \a) index))
                               (apply str (repeat (- length 5) \x))))
                        lengths))]
    {:ranked patterns
     :counts (into {} (map (fn [pattern] [pattern 9])) patterns)}))

;; @spec MCP-OP-ALIAS-058
;; @spec MCP-OP-ALIAS-059
(deftest the-root-listing-charges-its-own-marker-against-its-own-ceiling
  ;; Round-thirteen review finding 1: `root-sizes` budgets only the entries it
  ;; RETAINS and then appends `… [+N more roots …]` for free, so the marker is
  ;; a fifth item nobody charged and the advertised sub-bound is broken by the
  ;; very thing that announces it.
  ;;
  ;;   root-list items/rendered/ceiling => 5 528 512
  ;;   root-list marker? => true
  ;;   root-list within ceiling? => false
  ;;
  ;; Six ordinary roots rendering 116 characters each retain four entries and
  ;; render 528. A bound that stops counting one item before the end is not a
  ;; bound; it is a bound plus whatever the last item costs.
  (let [ceiling alias-migration/max-refusal-root-list-characters
        rendered-of (fn [lengths]
                      (alias-migration/root-sizes
                        (root-listing-suggestion lengths)))
        marker? (fn [listing]
                  (boolean (some #(str/includes? % "more roots") listing)))]
    (testing "the review's own six 116-character roots"
      (let [listing (rendered-of (repeat 6 116))
            rendered (pr-str listing)]
        (is (marker? listing)
            (str "six roots past the budget dropped entries in silence: "
                 (pr-str listing)))
        (is (<= (count rendered) ceiling)
            (str "the root listing renders " (count rendered)
                 " characters, past its ceiling of " ceiling
                 " — the marker is appended after the budget rather than "
                 "charged against it"))))
    (testing "at exactly the ceiling the listing is published whole"
      ;; four entries of 125, 125, 125 and 124 characters render, as a vector
      ;; of strings, at exactly 512 with no marker: every root fits, so no
      ;; root is dropped and the listing says nothing about dropping any.
      (let [listing (rendered-of [125 125 125 124])
            rendered (pr-str listing)]
        (is (= 512 (count rendered))
            (str "a listing that renders exactly the ceiling was cut to "
                 (count rendered) " characters — the budget is short by the "
                 "marker it never charged"))
        (is (= 4 (count (filter #(str/ends-with? % " (9)") listing)))
            (str "a listing that fits whole dropped roots: " (pr-str listing)))
        (is (not (marker? listing))
            (str "a listing that dropped nothing claimed it dropped roots: "
                 (pr-str listing)))))
    (testing "one character past the ceiling the listing is cut and says so"
      (let [listing (rendered-of [125 125 125 125])
            rendered (pr-str listing)]
        (is (<= (count rendered) ceiling)
            (str "the cut listing renders " (count rendered)
                 " characters, past its ceiling of " ceiling))
        (is (marker? listing)
            (str "the listing dropped roots in silence: " (pr-str listing)))
        (is (str/includes? (last listing) "+1 more roots")
            (str "the marker names the wrong number of dropped roots: "
                 (pr-str (last listing))))))))

;; @spec MCP-OP-ALIAS-059
(defn- counted-endless-seq
  "An unchunked infinite sequence that counts realisations and refuses to be
  realised past `budget`.

  Deterministic where a timeout is not: a renderer that realises the whole
  value before measuring it walks straight into the throw, and one that stops
  at its ceiling never reaches it. `lazy-seq` rather than `map` over `range`,
  because a chunked source realises thirty-two elements at a time and would
  make the count a property of the chunk size."
  [counter budget]
  (letfn [(step [index]
            (lazy-seq
              (swap! counter inc)
              (when (> index budget)
                (throw (ex-info (str "the fact renderer realised " index
                                     " elements of an endless value")
                                {:realised index :budget budget})))
              (cons index (step (inc index)))))]
    (step 0)))

;; @spec MCP-OP-ALIAS-059
(deftest the-fact-renderer-costs-bounded-work-whatever-the-value
  ;; Round-thirteen review finding 2: "every value shape is elided" bounds the
  ;; returned TEXT and not the WORK. `pr-str` realises the complete value and
  ;; the ceiling is applied to the finished string:
  ;;
  ;;   function      => chars 352 named true elided true ms 0.9
  ;;   lazy-100k     => chars 449 named true elided true ms 15.4
  ;;   nested-map-10k=> chars 449 named true elided true ms 6.4
  ;;   before infinite render
  ;;   EXIT=124
  ;;
  ;; A bound on the output is not a bound on the cost of producing it: an
  ;; infinite lazy sequence never reaches the gate, and a ten-megabyte string
  ;; is rendered whole in order to be cut to 160 characters.
  (let [ceiling mcp-tool/max-refusal-fact-characters
        bounded? (fn [line] (<= (count line) (+ ceiling 64)))]
    (testing "an endless value is never realised past the ceiling"
      (let [realised (atom 0)
            budget (* 4 ceiling)
            line (mcp-tool/refusal-fact-line
                   {:error_type "alias-migration-empty-scope"
                    :endless (counted-endless-seq realised budget)})]
        (is (string? line) "the renderer published no fact line at all")
        (is (str/includes? line "endless")
            (str "an endless fact is dropped rather than elided: " line))
        (is (bounded? line)
            (str "the fact line renders " (count line) " characters"))
        (is (<= @realised (* 2 ceiling))
            (str "the renderer realised " @realised
                 " elements to publish " ceiling " characters"))))
    ))

;; @spec MCP-OP-ALIAS-059
(deftest the-fact-renderer-survives-a-deeply-nested-value
  (let [ceiling mcp-tool/max-refusal-fact-characters
        bounded? (fn [line] (<= (count line) (+ ceiling 64)))]
    (testing "a deeply nested value does not take the renderer down"
      (let [deep (reduce (fn [acc _] {:n acc}) {:leaf 1} (range 20000))
            line (mcp-tool/refusal-fact-line
                   {:error_type "alias-migration-empty-scope" :deep deep})]
        (is (string? line) "the renderer published no fact line at all")
        (is (str/includes? line "deep")
            (str "a deep fact is dropped rather than elided: " line))
        (is (bounded? line)
            (str "the fact line renders " (count line) " characters"))))
    ))

;; @spec MCP-OP-ALIAS-059
(deftest the-fact-renderer-does-not-read-a-huge-value-whole
  (let [ceiling mcp-tool/max-refusal-fact-characters
        bounded? (fn [line] (<= (count line) (+ ceiling 64)))]
    (testing "a ten-megabyte string is not rendered whole to be cut to 160"
      ;; a coarse guard rather than a fine one, and the only shape of witness
      ;; a String admits: a renderer that reads the whole value measured 362 ms
      ;; here (635 ms cold) and one that stops at its ceiling measures a
      ;; fraction of a millisecond, so the bound sits 3.6x below the failing
      ;; side and three orders of magnitude above the passing one.
      (let [big (apply str (repeat 10000000 \a))
            started (System/nanoTime)
            line (mcp-tool/refusal-fact-line
                   {:error_type "alias-migration-empty-scope" :big big})
            elapsed-ms (/ (- (System/nanoTime) started) 1e6)]
        (is (str/includes? line "big")
            (str "a huge fact is dropped rather than elided: " line))
        (is (bounded? line)
            (str "the fact line renders " (count line) " characters"))
        (is (< elapsed-ms 100.0)
            (str "the renderer took " elapsed-ms
                 " ms to publish " ceiling " characters, so it read the whole "
                 "of a ten-megabyte value before bounding it"))))))

;; @spec MCP-OP-ALIAS-059
;; round-fourteen review finding 1: `print-method`'s own default for an
;; object it does not recognise calls that object's `toString` before a
;; single character reaches the ceiling writer, so a value nested inside no
;; collection at all can still cost unbounded work.
(deftype ^:private LoopingToStringProbe []
  Object
  (toString [_] (loop [] (recur))))

;; @spec MCP-OP-ALIAS-059
(deftype ^:private ThrowingToStringProbe []
  Object
  (toString [_] (throw (ex-info "toString exploded" {}))))

;; @spec MCP-OP-ALIAS-059
(deftest the-fact-renderer-never-invokes-an-arbitrary-toString
  ;; Round-fourteen review finding 1, reproduced at 6cbcbd48/524dd21d and
  ;; every commit through 1cc5990b: `bounded-pr-str` bounds writes made AFTER
  ;; `print-method` receives the value, but `print-method`'s default for an
  ;; object it does not recognise invokes that object's `toString` before any
  ;; character reaches `ceiling-writer` at all. A `deftype` whose `toString`
  ;; never returns therefore hangs the renderer no matter how tight the
  ;; ceiling is, and one whose `toString` throws escapes the ceiling's own
  ;; catch. The fix admits only Clojure data before printing and renders
  ;; everything else — recursively inside collections — as an identity
  ;; marker, never touching its `toString`.
  (let [ceiling mcp-tool/max-refusal-fact-characters]
    (testing "a toString that never returns does not hang the renderer"
      ;; the timeout guards the WITNESS, not the renderer: a regression fails
      ;; this assertion in two seconds instead of hanging the test process
      (let [work (future (mcp-tool/bounded-pr-str
                           (LoopingToStringProbe.) ceiling))
            result (deref work 2000 ::timed-out)]
        (is (not= ::timed-out result)
            "bounded-pr-str hung inside a toString that never returns")
        (when-not (= ::timed-out result)
          (is (str/includes? (str result) "LoopingToStringProbe")
              (str "an opaque value renders no identity at all: " result)))))
    (testing "a toString that throws does not escape the renderer"
      (let [result (try (mcp-tool/bounded-pr-str
                          (ThrowingToStringProbe.) ceiling)
                         (catch Exception e
                           (str "<threw " (.getSimpleName (class e)) ">")))]
        (is (not (str/starts-with? result "<threw"))
            (str "bounded-pr-str propagated the object's own toString "
                 "exception: " result))
        (is (str/includes? result "ThrowingToStringProbe")
            (str "an opaque value renders no identity at all: " result))))
    (testing "a Java collection wrapping such an object is opaque too"
      (let [poison (doto (java.util.ArrayList.)
                     (.add (ThrowingToStringProbe.)))
            work (future (mcp-tool/bounded-pr-str poison ceiling))
            result (deref work 2000 ::timed-out)]
        (is (not= ::timed-out result)
            "bounded-pr-str hung walking a Java collection's own toString")
        (when-not (= ::timed-out result)
          (is (str/includes? (str result) "ArrayList")
              (str "the wrapping collection renders no identity at all: "
                   result)))))
    (testing "ordinary Clojure data still renders exactly as before"
      (is (= "{:a 1, :b [2 3]}"
             (mcp-tool/bounded-pr-str {:a 1 :b [2 3]} ceiling))
          "an ordinary value's rendering changed shape")
      (is (= "#{1}" (mcp-tool/bounded-pr-str #{1} ceiling))
          "an ordinary set's rendering changed shape")
      (is (= "(1 2)" (mcp-tool/bounded-pr-str '(1 2) ceiling))
          "an ordinary list's rendering changed shape")
      (is (= "nil" (mcp-tool/bounded-pr-str nil ceiling))
          "nil's rendering changed shape")
      (is (= "[nil 1]" (mcp-tool/bounded-pr-str [nil 1] ceiling))
          "a nil element inside a vector is dropped rather than rendered"))))
;; @spec MCP-OP-ALIAS-059
;; Round-fifteen review finding 1: an allowlist BY TYPE admits every SUBTYPE.
;; `print-safe-leaf?` admitted everything satisfying `number?` — that is
;; `(instance? Number …)` — and `print-method`'s own `Number` implementation is
;; `print-simple`, which writes `(str o)`. `java.lang.Number` is not final, so a
;; proxy or an anonymous subclass carries an arbitrary `toString` straight
;; through the leaf branch the renderer treats as safe: a throwing one escapes
;; `bounded-pr-str` and a looping one hangs it, exactly the two failures the
;; round-fourteen fix closed for every OTHER class.
(defn- throwing-number
  "A `java.lang.Number` whose `toString` throws — `number?` is true of it."
  []
  (proxy [Number] []
    (toString [] (throw (ex-info "number toString exploded" {})))
    (intValue [] 0)
    (longValue [] 0)
    (floatValue [] (float 0.0))
    (doubleValue [] 0.0)))

;; @spec MCP-OP-ALIAS-059
(defn- looping-number
  "A `java.lang.Number` whose `toString` never returns."
  []
  (proxy [Number] []
    (toString [] (loop [] (recur)))
    (intValue [] 0)
    (longValue [] 0)
    (floatValue [] (float 0.0))
    (doubleValue [] 0.0)))

;; @spec MCP-OP-ALIAS-059
;; @spec MCP-OP-ALIAS-059
(defn- benign-number
  "A `java.lang.Number` whose `toString` is HARMLESS and OBSERVABLE.

  It neither throws nor loops, so neither `write-safe-leaf`'s try/catch nor
  `bounded-pr-str`'s time budget can fire on it. The only thing that keeps its
  `toString` out of the receipt is the exact-class allowlist — which is
  exactly why the witness needs it."
  []
  (proxy [Number] []
    (toString [] "SABOTAGE-VISIBLE-42")
    (intValue [] 42)
    (longValue [] 42)
    (floatValue [] (float 42.0))
    (doubleValue [] 42.0)))

;; @spec MCP-OP-ALIAS-059
(deftest the-scalar-allowlist-refuses-a-benign-subclass-of-an-admitted-class
  ;; Round-sixteen review finding 3: the exact-class allowlist shipped with NO
  ;; witness of its own. The reviewer reverted ONLY `print-safe-leaf?` to the
  ;; pre-fix `instance?`-based predicate, leaving `write-safe-leaf`'s guard and
  ;; the time budget in place, and the advertised witness stayed 22/22 GREEN
  ;; while a caller-controlled `toString` provably reached the receipt again.
  ;;
  ;; The lesson, which is the general form: DEFENCE IN DEPTH HIDES A MISSING
  ;; WITNESS. Three layers each guarantee the OUTCOME the old assertions
  ;; asserted — bounded, identity marker rendered — so removing any one of them
  ;; changes nothing those assertions can see. Each layer must therefore assert
  ;; its OWN contract, on a subject only that layer can bound. A benign
  ;; `Number` subclass is that subject: it does not throw, so the leaf guard
  ;; cannot fire; it does not loop, so the time budget cannot fire; and its
  ;; `toString` returns a marker string a reader can look for. Only the
  ;; allowlist keeps it out.
  (let [ceiling mcp-tool/max-refusal-fact-characters
        benign (benign-number)]
    (is (number? benign)
        "the witness's own subject is not a Number, so it proves nothing")
    (is (= "SABOTAGE-VISIBLE-42" (str benign))
        "the subject's toString is not observable, so the assertion is blind")
    (doseq [[label value] [["on its own" benign]
                           ["inside a map value" {:a benign}]
                           ["inside a vector" [benign]]]]
      (testing label
        (let [rendered (mcp-tool/bounded-pr-str value ceiling)]
          (is (not (str/includes? rendered "SABOTAGE-VISIBLE-42"))
              (str "a Number SUBCLASS reached print-method and its own "
                   "toString was published in the receipt · " label " · "
                   rendered))
          (is (str/includes? rendered "#object[")
              (str "a Number the allowlist does not admit rendered no "
                   "identity marker · " label " · " rendered)))))
    (testing "the exact classes the allowlist admits still render as values"
      (doseq [value [42 (int 7) (long 9) 1.5 (float 2.5) 3/4 42N 1.25M
                     "text" true :kw 'sym]]
        (is (not (str/includes? (mcp-tool/bounded-pr-str value ceiling)
                                "#object["))
            (str "an admitted scalar lost its rendering: " (pr-str value)))))))

(deftest the-fact-renderer-admits-only-the-exact-numeric-classes-it-prints
  (let [ceiling mcp-tool/max-refusal-fact-characters]
    (testing "a Number whose toString throws does not escape the renderer"
      (let [hostile (throwing-number)]
        (is (number? hostile)
            "the witness's own subject is not a Number, so it proves nothing")
        (let [result (try (mcp-tool/bounded-pr-str hostile ceiling)
                          (catch Throwable t
                            (str "<threw " (.getSimpleName (class t)) ">")))]
          (is (not (str/starts-with? result "<threw"))
              (str "bounded-pr-str propagated a Number's own toString "
                   "exception: " result))
          (is (str/includes? result "#object[")
              (str "a Number the renderer cannot print safely rendered no "
                   "identity marker at all: " result)))))
    (testing "a Number whose toString never returns does not hang the renderer"
      ;; the deref timeout guards the WITNESS; the renderer's own guard is
      ;; asserted by the value it returns inside it
      (let [hostile (looping-number)
            work (future (mcp-tool/bounded-pr-str hostile ceiling))
            result (deref work 30000 ::timed-out)]
        (is (not= ::timed-out result)
            "bounded-pr-str hung inside a Number's toString that never returns")
        (when-not (= ::timed-out result)
          (is (str/includes? (str result) "#object[")
              (str "a hostile Number rendered no identity marker: " result)))))
    (testing "a lazy sequence whose realisation never returns is bounded too"
      ;; the character ceiling cannot fire on a seq that never yields its
      ;; first element, so the renderer needs a TIME bound as well as a
      ;; character bound
      (let [hostile (lazy-seq (loop [] (recur)))
            work (future (mcp-tool/bounded-pr-str hostile ceiling))
            result (deref work 30000 ::timed-out)]
        (is (not= ::timed-out result)
            (str "bounded-pr-str hung realising a lazy sequence whose body "
                 "never returns"))))
    (testing "every numeric class the program really emits renders as before"
      (doseq [value [(long 7) (int 7) (short 7) (byte 7)
                     (double 7.5) (float 7.5)
                     (bigint 7) (biginteger 7) (bigdec "7.50") (/ 22 7)
                     "text" :kw 'sym true false nil]]
        (is (= (pr-str value) (mcp-tool/bounded-pr-str value ceiling))
            (str "the rendering of " (pr-str value) " ("
                 (if (nil? value) "nil" (.getName (class value)))
                 ") changed shape"))))))

;; @spec MCP-OP-ALIAS-059
;; Round-fifteen review finding 3: the bounded renderer's own recursion
;; replaced `print-method`'s, and two of `print-method`'s renderings were not
;; carried across — a record lost its `#namespace.Record` tag and became an
;; ordinary map, and collection metadata disappeared under `*print-meta*`
;; while SYMBOL metadata still rendered. Neither shows in the ordinary-receipt
;; corpus, so the compatibility claim held for those receipts and not
;; universally. Both previous forms are bounded — a class name is read with
;; `.getName` and never with `toString`, and a metadata map recurses through
;; the same bounded writer as any other value — so they are RESTORED rather
;; than narrowed.
(defrecord RendererCompatRecord [a b])

;; @spec MCP-OP-ALIAS-059
;; @spec MCP-OP-ALIAS-059
(deftest the-refusal-fact-line-spends-one-print-budget-for-the-whole-receipt
  ;; Round-sixteen review, recorded as a composition FACT rather than a
  ;; finding: the wall-clock budget is per `bounded-pr-str` CALL and not per
  ;; leaf, so a map of a hundred looping values costs one budget — but
  ;; `refusal-fact-line` calls it once per fact under a sixteen-fact bound, and
  ;; a refusal carrying sixteen unrenderable values cost a measured 32,008 ms.
  ;; It is bounded, and it is not reachable from caller data: every entrance
  ;; normalises its params through a JSON round trip that can carry only
  ;; scalars and collections, never a JVM object with a hostile `toString`.
  ;;
  ;; Fixed anyway, because the fix is a DEADLINE shared across the fact line
  ;; rather than a budget per fact — the receipt is the unit a caller waits on,
  ;; and sixteen budgets is a bound nobody would choose on purpose.
  (let [;; interruptible, so the sixteen abandoned daemon threads cost the rest
        ;; of the suite nothing: a lazy sequence that sleeps rather than spins
        unrenderable (fn [] (lazy-seq (do (Thread/sleep Long/MAX_VALUE) nil)))
        result (into {:ok false
                      :operation "alias_migration"
                      :error_type "alias-migration-planted"
                      :error "planted"}
                     (for [index (range 16)]
                       [(keyword (str "fact_" index)) (unrenderable)]))
        started (System/nanoTime)
        line (mcp-tool/refusal-fact-line result)
        elapsed-ms (/ (- (System/nanoTime) started) 1e6)]
    (is (some? line) "the planted refusal rendered no fact line at all")
    (is (= 16 (count (re-seq #"#object\[" line)))
        (str "every unrenderable fact should carry its own identity marker: "
             line))
    (is (< elapsed-ms 8000.0)
        (str "refusal-fact-line spent one print budget PER FACT rather than "
             "one for the receipt: " (long elapsed-ms) " ms for 16 facts"))))

(deftest the-fact-renderer-keeps-every-rendering-callers-already-saw
  (let [ceiling mcp-tool/max-refusal-fact-characters]
    (testing "a record keeps its own tag rather than becoming a map"
      (let [value (->RendererCompatRecord 1 2)]
        (is (= (pr-str value) (mcp-tool/bounded-pr-str value ceiling))
            "a record's rendering lost its #namespace.Record tag")))
    (testing "a record's fields are still rendered through the bounded writer"
      (let [value (->RendererCompatRecord 1 (LoopingToStringProbe.))
            work (future (mcp-tool/bounded-pr-str value ceiling))
            result (deref work 30000 ::timed-out)]
        (is (not= ::timed-out result)
            "restoring the record tag reopened the toString hole")
        (when-not (= ::timed-out result)
          (is (str/includes? (str result) "LoopingToStringProbe")
              (str "a record's poisonous field rendered no identity marker: "
                   result)))))
    (testing "metadata renders under *print-meta* exactly as before"
      (binding [*print-meta* true]
        (doseq [value [(with-meta {:a 1} {:receipt true})
                       (with-meta [1 2] {:receipt true})
                       (with-meta '(1 2) {:receipt true})
                       (with-meta #{1} {:receipt true})
                       (with-meta 'alpha {:receipt true})
                       (with-meta [1 2] {:tag 'Long})]]
          (is (= (pr-str value) (mcp-tool/bounded-pr-str value ceiling))
              (str "metadata on " (pr-str (class value))
                   " renders differently from pr-str")))))
    (testing "metadata is still absent by default"
      (is (= "{:a 1}"
             (mcp-tool/bounded-pr-str (with-meta {:a 1} {:receipt true})
                                      ceiling))
          "metadata leaked into a default rendering"))))

;; @spec MCP-OP-ALIAS-059
(deftest the-enumeration-reaches-the-routers-entrance-slice-and-every-spelling
  ;; Round-thirteen review finding 3, reproduced at c5e63e6. Two legs.
  ;;
  ;; The hold-out:
  ;;   enumeration reads mcp_tool? => false
  ;;
  ;; And a kind spelling the scan cannot see, which is the same defect in the
  ;; sources it DOES read — `mcp_change_buffer.clj:1062` mints two kinds from
  ;; one expression, `:error-type (if (zero? (:match-count found)) :no-match
  ;; :ambiguous-match)`, and the regex requires `:error-type` to be followed
  ;; immediately by a keyword:
  ;;   enumerated count => 125
  ;;   no-match => false · ambiguous-match => false
  (let [kinds (refusal-kinds-in-source)
        slice (router-entrance-slice)
        source (reachable-entrance-source-text)]
    (testing "a kind minted inside a non-literal expression is enumerated"
      (doseq [kind ["no-match" "ambiguous-match"]
              :let [enumerated? (contains? kinds kind)]]
        (is enumerated?
            (str "the enumeration holds " (count kinds) " kinds and not " kind
                 ", which `:error-type (if …)` mints in the reachable set"))))
    (testing "the router's entrance slice is read rather than held out"
      (is (true? (str/includes? slice "handle-alias-migration"))
          "the slice does not contain the entrance it is built from")
      (is (true? (str/includes? source "handle-alias-migration"))
          (str "`mcp_tool` is held out of the enumeration's source set, so a "
               "kind spelled on the entrance's own code path is invisible to "
               "it — the hole the reviewer walked through")))
    (testing "the slice is a code path and not the whole file"
      (is (true? (< (count slice) (count (slurp "src/clj_surgeon/mcp_tool.clj"))))
          "the slice is the whole router, so every verb's kinds come with it")
      (doseq [kind ["compact-relation-path-conflict" "receipt-publish-failed"]
              :let [enumerated? (contains? kinds kind)]]
        (is (false? enumerated?)
            (str kind " belongs to a verb this entrance never reaches"))))
    (testing "a kind composed at runtime on that path is refused"
      ;; the reviewer's own construction, verbatim
      (let [sabotage (str "(defmethod heldout-alias-refusal :alias [_]\n"
                          "  {:ok false\n"
                          "   :operation \"alias_migration\"\n"
                          "   :error_type (str \"heldout-\" \"protocol-kind\")\n"
                          "   :error \"held-out mcp_tool refusal\"})\n")
            sites (->> (error-type-value-sites sabotage)
                       (remove :literal)
                       (remove #(seq (minted-kinds-in (:value %))))
                       (remove #(forwarding-marked? sabotage (:text %))))]
        (is (= 1 (count sites))
            (str "a kind spelled entirely at runtime, with no forwarding "
                 "marker, is not reported"))))))

;; @spec MCP-OP-ALIAS-028
;; @spec MCP-OP-ALIAS-059
(defn- wide-kondo-stub!
  "An executable named `clj-kondo` answering VALID EDN larger than the
  process runner's visible byte budget.

  The shape a real `clj-kondo --config {:output {:format :edn}}` answers over
  an ordinary tree: 100 fanout namespaces produced 11,223 bytes for twenty-one
  of them and far more for all of them, against a 12,000-byte visible cap."
  [workspace bytes]
  (let [script (io/file workspace "bin" "clj-kondo")
        finding (str "{:type :unused-namespace :level :warning "
                     ":filename \"src/x.clj\" :row 2 :col 14 "
                     ":message \"namespace acid.fanout.util-b is required "
                     "but never used\"}")
        findings (apply str (repeat (max 1 (quot bytes (count finding)))
                                    finding))]
    (.mkdirs (.getParentFile script))
    (spit script (str "#!/bin/bash\ncat <<'PAYLOAD'\n"
                      "{:findings [" findings "]}\n"
                      "PAYLOAD\n"))
    (.setExecutable script true)
    script))

;; @spec MCP-OP-ALIAS-028
;; @spec MCP-OP-ALIAS-059
(deftest a-diagnostic-baseline-parses-output-larger-than-the-visible-budget
  ;; inb-76b351, the E-CALLER cohort: `alias_migration` with `verify: "fast"`
  ;; refused TWICE on a clean fixture, and its remedy prescribed re-sending
  ;; the request that had just failed:
  ;;
  ;;   alias_migration
  ;;     refused · invalid-diagnostic-output · 316.91 ms
  ;;   ✓ source unchanged
  ;;   → Verification baseline capture failed before the alias migration
  ;;   facts · files=21 · sites=63
  ;;   remedy · Re-send the same alias_migration request; the frozen snapshot
  ;;            is recomputed from current source.
  ;;
  ;; The cause is not the tree and not clj-kondo. The baseline runs
  ;; `clj-kondo … --config {:output {:format :edn}}` through the shared
  ;; process runner, whose `visible-byte-limit` is 12,000 bytes; the scope
  ;; `["src"]` selects 100 sources, the EDN document is far larger, and it
  ;; comes back CUT mid-map:
  ;;
  ;;   scoped files: 100
  ;;   ok? false  error-type :invalid-diagnostic-output
  ;;   output tail: "…:filename \"src/acid/fanout/ns_022.clj\", :col 14,
  ;;                 :end-col 32, :langs (), :message \"namespace acid.fa"
  ;;
  ;; A truncated EDN document cannot parse, so a correct analyzer answering a
  ;; correct document is reported as invalid output — deterministically, on
  ;; every retry, which is exactly what the field arm did.
  (let [workspace (workspace!)]
    (try
      (let [script (wide-kondo-stub! workspace 40000)
            baseline (change-buffer/capture-verification-baseline!
                       (.getPath workspace) "wide"
                       {"wide" [(.getPath script)]}
                       ["src/acid/fanout/ns_000.cljc"])]
        (is (true? (:ok baseline))
            (str "a valid EDN diagnostic larger than the visible byte budget "
                 "is reported as invalid output: "
                 (pr-str (select-keys baseline [:ok :error-type]))))
        (is (nil? (:error-type baseline))
            (str "the baseline typed a parse failure over a complete "
                 "document: " (pr-str (:error-type baseline)))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-028
;; @spec MCP-OP-ALIAS-059
(deftest a-baseline-refusal-never-prescribes-re-sending-the-same-request
  ;; The second half of inb-76b351: the remedy told the caller to re-send the
  ;; identical request, which reproduced the refusal, and never mentioned the
  ;; one change that worked — dropping `verify`. A remedy may prescribe a
  ;; retry of the SAME request only when the cause is transient and said to be
  ;; transient; a baseline that cannot read its analyzer's answer is not.
  (let [workspace (workspace!)]
    (try
      (let [script (kondo-stub! workspace)
            result (execute! workspace
                             {:verify "focused"}
                             {:verification-profiles
                              {"focused" [(.getPath script)]}})
            remedy (str (:remedy result))]
        (is (false? (:ok result)) (pr-str result))
        (is (not (str/includes? remedy "Re-send the same alias_migration"))
            (str "the remedy prescribes re-sending the request that just "
                 "failed, for a cause that is not transient: " remedy))
        (is (some? (:next_call result))
            "the refusal carries no executable correction at all")
        (is (nil? (:verify (:next_call result)))
            (str "the next_call re-sends the same verify value: "
                 (pr-str (:next_call result)))))
      (finally
        (delete-tree! workspace)))))

;; @spec MCP-OP-ALIAS-028
;; @spec MCP-OP-ALIAS-059
(deftest a-post-write-verification-refusal-names-the-one-change-that-works
  ;; The same rule on the other side of the write. A profile that reported a
  ;; failure reports it again on an identical re-send, so the generic
  ;; "Re-send the same alias_migration request" remedy is a retry loop —
  ;; published, in the live replay of the E-CALLER shape, BESIDE a next_call
  ;; that had already dropped `verify`. A receipt whose remedy and whose
  ;; next_call disagree about what to send is worse than either alone.
  (let [workspace (workspace!)]
    (try
      (let [script (io/file workspace "bin" "failing-check")
            _ (.mkdirs (.getParentFile script))
            _ (spit script "#!/bin/bash\necho refused-by-profile\nexit 1\n")
            _ (.setExecutable script true)
            result (execute! workspace
                             {:verify "strict"}
                             {:verification-profiles
                              {"strict" [(.getPath script)]}})
            remedy (str (:remedy result))]
        (is (false? (:ok result)) (pr-str result))
        (is (true? (:source_unchanged result))
            (str "the rollback did not restore the tree: " (pr-str result)))
        (is (not (str/includes? remedy "Re-send the same alias_migration"))
            (str "the remedy prescribes the request the profile just "
                 "refused: " remedy))
        (is (some? (:next_call result))
            "the refusal carries no executable correction at all")
        (is (nil? (:verify (:next_call result)))
            (str "the next_call carries the profile that failed: "
                 (pr-str (:next_call result)))))
      (finally
        (delete-tree! workspace)))))
