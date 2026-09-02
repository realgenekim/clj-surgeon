(ns clj-surgeon.extract-test
  (:require
   [clj-surgeon.extract :as extract]
   [clj-surgeon.file-ops :as file-ops]
   [clojure.java.io :as io]
   [clojure.java.shell :as shell]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [rewrite-clj.parser :as parser]))

(defn- create-temp-project!
  "Create a minimal project with a monolith file."
  []
  (let [root (java.io.File/createTempFile "extract-test" "")
        _ (.delete root)
        _ (.mkdirs root)
        src-dir (io/file root "src" "my")]
    (.mkdirs src-dir)
    (spit (io/file src-dir "app.clj")
          "(ns my.app
  (:require [clojure.string :as str]))

(def config {:port 3000})

(defn helper [x]
  (str/upper-case x))

;; Distillery functions
(defn distill [x]
  (helper x))

(defn refine [x]
  (distill (str x \"-refined\")))

;; Main entry
(defn -main []
  (refine \"hello\"))
")
    root))

(defn- delete-recursive! [f]
  (when (.isDirectory f)
    (doseq [child (.listFiles f)]
      (delete-recursive! child)))
  (.delete f))

(defn- dependency-minimal-source []
  (str "(ns fixture.views\n  (:require\n"
       (apply str
              (for [index (range 1 17)]
                (format "   [fixture.dep%02d :as d%02d]\n" index index)))
       "   [fixture.domain.schedule :as schedule]))\n\n"
       "(defn day-tab []\n"
       "  [d01/value d02/value d03/value d04/value d05/value d06/value])\n\n"
       "(defn agenda-page []\n  (day-tab))\n\n"
       "(defn unrelated-page [] :ok)\n"))

(defn- create-dependency-minimal-project!
  "Create the minimized production shape from clj-surgeon-to4."
  []
  (let [root (java.io.File/createTempFile "extract-dependencies" "")
        _ (.delete root)
        _ (.mkdirs root)
        src-dir (io/file root "src")
        views-file (io/file src-dir "fixture" "views.clj")]
    (spit (io/file root "deps.edn") "{:paths [\"src\"]}\n")
    (doseq [index (range 1 17)]
      (let [ns-name (format "fixture.dep%02d" index)
            file (io/file src-dir "fixture" (format "dep%02d.clj" index))]
        (.mkdirs (.getParentFile file))
        (spit file (format "(ns %s)\n\n(def value :dep-%02d)\n" ns-name index))))
    (let [schedule-file (io/file src-dir "fixture" "domain" "schedule.clj")]
      (.mkdirs (.getParentFile schedule-file))
      (spit schedule-file
            "(ns fixture.domain.schedule)\n\n(def existing :schedule)\n"))
    (.mkdirs (.getParentFile views-file))
    (spit views-file (dependency-minimal-source))
    root))

(defn- cold-require-result [root & namespaces]
  (shell/sh "clojure"
            "-Sdeps" (pr-str {:paths [(str (io/file root "src"))]})
            "-M" "-e"
            (str "(require " (str/join " " (map #(str "'" %) namespaces)) ")")))

(defn- cold-eval-result [root expression]
  (shell/sh "clojure"
            "-Sdeps" (pr-str {:paths [(str (io/file root "src"))]})
            "-M" "-e" expression))

;; ============================================================
;; Pure unit tests for file-path->ns-name
;; ============================================================

(deftest test-file-path->ns-name-standard-layout
  (testing "standard src/ layout"
    (is (= "myapp.core"
           (#'extract/file-path->ns-name "src/myapp/core.clj"))))
  (testing "nested namespaces"
    (is (= "myapp.state.distillery"
           (#'extract/file-path->ns-name "src/myapp/state/distillery.clj"))))
  (testing "underscores become hyphens"
    (is (= "my-app.my-ns"
           (#'extract/file-path->ns-name "src/my_app/my_ns.clj"))))
  (testing "absolute path with /src/"
    (is (= "my.app"
           (#'extract/file-path->ns-name "/tmp/project/src/my/app.clj"))))
  (testing "relative path starting with src/"
    (is (= "my.app"
           (#'extract/file-path->ns-name "src/my/app.clj")))))

(deftest test-source-paths-from-deps-edn
  (testing "reads this project's deps.edn correctly"
    (let [paths (#'extract/source-paths-from-deps-edn)]
      (is (sequential? paths))
      (is (some #{"src"} paths)))))

(deftest test-file-path->ns-name-no-deps-edn
  (testing "falls back to [\"src\"] when no deps.edn and no explicit paths"
    ;; Pass empty vector to simulate no source paths found;
    ;; the function should fall through to the /src/ fallback
    (is (= "myapp.core"
           (#'extract/file-path->ns-name "/tmp/no-project/src/myapp/core.clj"
                                         [])))
    ;; Relative path fallback
    (is (= "myapp.core"
           (#'extract/file-path->ns-name "src/myapp/core.clj"
                                         [])))))

(deftest test-file-path->ns-name-cljs-cljc-extensions
  (testing ".cljs extension stripped"
    (is (= "myapp.ui"
           (#'extract/file-path->ns-name "src/myapp/ui.cljs"))))
  (testing ".cljc extension stripped"
    (is (= "myapp.shared"
           (#'extract/file-path->ns-name "src/myapp/shared.cljc"))))
  (testing ".clj still works"
    (is (= "myapp.core"
           (#'extract/file-path->ns-name "src/myapp/core.clj")))))

(deftest test-file-path->ns-name-dialect-dirs
  (testing "src/clj/ layout strips dialect dir"
    (is (= "myapp.core"
           (#'extract/file-path->ns-name "src/clj/myapp/core.clj"
                                         ["src/clj"]))))
  (testing "src/cljs/ layout strips dialect dir and extension"
    (is (= "myapp.ui"
           (#'extract/file-path->ns-name "src/cljs/myapp/ui.cljs"
                                         ["src/cljs"]))))
  (testing "src/cljc/ layout strips dialect dir and extension"
    (is (= "myapp.shared"
           (#'extract/file-path->ns-name "src/cljc/myapp/shared.cljc"
                                         ["src/cljc"]))))
  (testing "Maven-style layout"
    (is (= "myapp.core"
           (#'extract/file-path->ns-name "src/main/clojure/myapp/core.clj"
                                         ["src/main/clojure"]))))
  (testing "standard src/ with explicit paths"
    (is (= "myapp.core"
           (#'extract/file-path->ns-name "src/myapp/core.clj"
                                         ["src"]))))
  (testing "longest matching prefix wins"
    (is (= "myapp.core"
           (#'extract/file-path->ns-name "src/clj/myapp/core.clj"
                                         ["src" "src/clj"])))))

;; ============================================================
;; Plan integration test with dialect-split layout
;; ============================================================

(deftest test-plan-dialect-split-layout
  (let [root (java.io.File/createTempFile "extract-dialect-test" "")
        _ (.delete root)
        _ (.mkdirs root)
        src-dir (io/file root "src" "clj" "my")]
    (.mkdirs src-dir)
    (spit (io/file root "deps.edn") "{:paths [\"src/clj\"]}")
    (spit (io/file src-dir "app.clj")
          "(ns my.app
  (:require [clojure.string :as str]))

(defn helper [x]
  (str/upper-case x))

(defn distill [x]
  (helper x))
")
    (try
      (let [source (str (.getPath root) "/src/clj/my/app.clj")
            target (str (.getPath root) "/src/clj/my/distillery.clj")
            p (extract/plan {:file source
                             :forms '[distill]
                             :to target
                             :source-paths ["src/clj"]})]
        (testing "target-ns strips dialect dir"
          (is (= "my.distillery" (:target-ns p))))
        (testing "no error"
          (is (nil? (:error p))))
        (testing "preview has correct ns"
          (is (str/includes? (:new-file-preview p) "ns my.distillery"))))
      (finally (delete-recursive! root)))))

(deftest test-execute-dialect-split-layout
  (let [root (java.io.File/createTempFile "extract-dialect-exec" "")
        _ (.delete root)
        _ (.mkdirs root)
        src-dir (io/file root "src" "cljs" "my")]
    (.mkdirs src-dir)
    (spit (io/file root "deps.edn") "{:paths [\"src/cljs\"]}")
    (spit (io/file src-dir "ui.cljs")
          "(ns my.ui
  (:require [clojure.string :as str]))

(defn render-loud [text]
  (str \"<b>\" (str/upper-case text) \"</b>\"))

(defn render-quiet [text]
  (str \"<i>\" (str/lower-case text) \"</i>\"))
")
    (try
      (let [source (str (.getPath root) "/src/cljs/my/ui.cljs")
            target (str (.getPath root) "/src/cljs/my/helpers.cljs")
            result (extract/execute! {:file source
                                      :forms '[render-loud]
                                      :to target
                                      :source-paths ["src/cljs"]})]
        (testing "new file created"
          (is (.exists (io/file target))))
        (testing "new file has correct ns (no cljs. prefix, no .cljs suffix)"
          (let [content (slurp target)]
            (is (str/includes? content "ns my.helpers"))
            (is (not (str/includes? content "cljs.my")))
            (is (not (str/includes? content "helpers.cljs")))))
        (testing "extracted form present"
          (is (str/includes? (slurp target) "defn render-loud")))
        (testing "form removed from source"
          (is (not (str/includes? (slurp source) "defn render-loud"))))
        (testing "non-extracted form remains"
          (is (str/includes? (slurp source) "defn render-quiet")))
        (testing "callerless source does not gain a new require"
          (is (not (str/includes? (slurp source) "my.helpers"))))
        (testing "target keeps the used ClojureScript dependency"
          (is (= ["clojure.string"] (get-in result [:header :requires-kept])))
          (is (str/includes? (slurp target) "clojure.string")))
        (testing "receipt reports that no source require was necessary"
          (is (false? (:source-require-added result)))))
      (finally (delete-recursive! root)))))

;; ============================================================
;; Plan tests (pure, no file mutation)
;; ============================================================

(deftest test-plan-basic
  (let [root (create-temp-project!)]
    (try
      (let [source (str (.getPath root) "/src/my/app.clj")
            target (str (.getPath root) "/src/my/distillery.clj")
            p (extract/plan {:file source
                             :forms '[distill refine]
                             :to target})]
        (testing "no error"
          (is (nil? (:error p))))
        (testing "correct form count"
          (is (= 2 (:form-count p))))
        (testing "source and target ns"
          (is (= "my.app" (:source-ns p)))
          (is (= "my.distillery" (:target-ns p))))
        (testing "preview contains new ns name"
          (is (str/includes? (:new-file-preview p) "my.distillery")))
        (testing "preview contains extracted forms"
          (is (some #(= "distill" (:name %)) (:forms (:new-file-preview p))))
          (is (some #(= "refine" (:name %)) (:forms (:new-file-preview p))))))
      (finally (delete-recursive! root)))))

(deftest test-plan-proves-quoted-var-callers-without-textual-lookalikes
  (let [root (create-temp-project!)
        test-file (io/file root "test/my/app_test.clj")]
    (try
      (.mkdirs (.getParentFile test-file))
      (spit test-file
            (str "(ns my.app-test\n"
                 "  (:require [my.app :as app]))\n"
                 "(deftest aliased-private-var (is (var? #'app/distill)))\n"
                 "(deftest qualified-private-var (is (var? #'my.app/refine)))\n"
                 "(def text \"#'my.app/refine\")\n"
                 ";; #'my.app/distill\n"
                 "(def data '(var my.app/refine))\n"))
      (let [source (str (.getPath root) "/src/my/app.clj")
            target (str (.getPath root) "/src/my/distillery.clj")
            plan (extract/plan {:file source
                                :forms '[distill refine]
                                :to target})]
        (is (nil? (:error plan)))
        (is (= 2 (count (:quoted-var-references-unrewired plan))))
        (is (= #{"my.app/distill" "my.app/refine"}
               (set (map :subject (:quoted-var-references-unrewired plan)))))
        (is (= #{:structural-var-quote}
               (set (map :reference-authority
                         (:quoted-var-references-unrewired plan))))))
      (finally (delete-recursive! root)))))

(deftest test-plan-missing-form
  (let [root (create-temp-project!)]
    (try
      (let [source (str (.getPath root) "/src/my/app.clj")
            target (str (.getPath root) "/src/my/distillery.clj")
            p (extract/plan {:file source
                             :forms '[distill nonexistent]
                             :to target})]
        (testing "error for missing form"
          (is (some? (:error p)))
          (is (str/includes? (:error p) "nonexistent"))))
      (finally (delete-recursive! root)))))

(deftest test-plan-omits-unused-requires
  (let [root (create-temp-project!)]
    (try
      (let [source (str (.getPath root) "/src/my/app.clj")
            target (str (.getPath root) "/src/my/distillery.clj")
            p (extract/plan {:file source
                             :forms '[distill]
                             :to target})]
        (testing "new file omits dependencies unused by the moved form"
          (is (empty? (:target-requires p)))
          (is (= ["clojure.string"] (get-in p [:header :requires-pruned])))
          (is (not (str/includes? (:new-file-preview p) "clojure.string")))))
      (finally (delete-recursive! root)))))

;; ============================================================
;; Execute tests
;; ============================================================

(deftest test-execute-creates-new-file
  (let [root (create-temp-project!)]
    (try
      (let [source (str (.getPath root) "/src/my/app.clj")
            target (str (.getPath root) "/src/my/distillery.clj")
            result (extract/execute! {:file source
                                      :forms '[distill refine]
                                      :to target})]
        (testing "new file created"
          (is (.exists (io/file target))))
        (testing "new file has correct ns"
          (let [content (slurp target)]
            (is (str/includes? content "ns my.distillery"))))
        (testing "new file has the forms"
          (let [content (slurp target)]
            (is (str/includes? content "(defn distill"))
            (is (str/includes? content "(defn refine"))))
        (testing "new file omits unused source requires"
          (let [content (slurp target)]
            (is (not (str/includes? content "clojure.string")))
            (is (empty? (get-in result [:header :requires-kept])))))
        (testing "summary correct"
          (is (= 2 (count (:forms (:new-file-preview result)))))))
      (finally (delete-recursive! root)))))

(deftest test-execute-removes-from-source
  (let [root (create-temp-project!)]
    (try
      (let [source (str (.getPath root) "/src/my/app.clj")
            target (str (.getPath root) "/src/my/distillery.clj")
            _ (extract/execute! {:file source
                                 :forms '[distill refine]
                                 :to target})
            source-content (slurp source)]
        (testing "extracted forms removed from source"
          (is (not (str/includes? source-content "(defn distill")))
          (is (not (str/includes? source-content "(defn refine"))))
        (testing "non-extracted forms remain"
          (is (str/includes? source-content "(defn helper"))
          (is (str/includes? source-content "(defn -main"))
          (is (str/includes? source-content "(def config")))
        (testing "source ns still intact"
          (is (str/includes? source-content "ns my.app"))))
      (finally (delete-recursive! root)))))

(deftest test-execute-does-not-consume-the-next-form-without-a-blank-line
  (let [root (create-temp-project!)
        source (io/file root "src" "my" "app.clj")
        target (io/file root "src" "my" "distillery.clj")]
    (try
      (spit source
            "(ns my.app)\n\n(defn move-me [] :moved)\n(defn event-resume-path [event]\n  (:resume-path event))\n")
      (extract/execute! {:file (.getPath source)
                         :forms '[move-me]
                         :to (.getPath target)})
      (let [rewritten (slurp source)]
        (testing "the complete rewritten namespace still parses"
          (is (some? (parser/parse-string-all rewritten))))
        (testing "the adjacent form remains byte-for-byte intact"
          (is (str/includes? rewritten
                             "(defn event-resume-path [event]\n  (:resume-path event))"))))
      (finally (delete-recursive! root)))))

(deftest test-execute-refuses-an-existing-target-without-changing-either-file
  (let [root (create-temp-project!)
        source (io/file root "src" "my" "app.clj")
        target (io/file root "src" "my" "distillery.clj")
        original-source (slurp source)
        original-target "existing target\n"]
    (try
      (spit target original-target)
      (let [result (extract/execute! {:file (.getPath source)
                                      :forms '[distill refine]
                                      :to (.getPath target)})]
        (is (= :extraction-target-exists (:error-type result)))
        (is (true? (:source-unchanged result)))
        (is (= original-source (slurp source)))
        (is (= original-target (slurp target))))
      (finally (delete-recursive! root)))))

(deftest test-execute-rolls-back-the-created-target-when-source-commit-fails
  (let [root (create-temp-project!)
        source (io/file root "src" "my" "app.clj")
        target (io/file root "src" "my" "distillery.clj")
        original-source (slurp source)
        real-atomic-write! file-ops/atomic-write!
        write-count (atom 0)]
    (try
      (let [failure
            (try
              (with-redefs [file-ops/atomic-write!
                            (fn [file content]
                              (if (= 2 (swap! write-count inc))
                                (throw (ex-info "simulated source write failure" {}))
                                (real-atomic-write! file content)))]
                (extract/execute! {:file (.getPath source)
                                   :forms '[distill refine]
                                   :to (.getPath target)}))
              nil
              (catch Exception e e))]
        (is (= :extraction-commit-failed (-> failure ex-data :error-type)))
        (is (true? (-> failure ex-data :source-restored)))
        (is (true? (-> failure ex-data :target-removed)))
        (is (= original-source (slurp source)))
        (is (not (.exists target))))
      (finally (delete-recursive! root)))))

(deftest test-extraction-receipt-undo-restores-the-original-source
  (let [root (create-temp-project!)
        source (io/file root "src" "my" "app.clj")
        target (io/file root "src" "my" "distillery.clj")
        receipt (io/file root "receipts" "extract.edn")
        original-source (slurp source)]
    (try
      (let [result (extract/execute! {:file (.getPath source)
                                      :forms '[distill refine]
                                      :to (.getPath target)
                                      :receipt-out (.getPath receipt)})]
        (is (= (.getCanonicalPath receipt) (:receipt-file result)))
        (is (.exists target))
        (is (.exists receipt))
        (let [undo (extract/undo! {:receipt (.getPath receipt)})]
          (is (true? (:ok undo)))
          (is (= original-source (slurp source)))
          (is (not (.exists target))))
        (testing "the same inverse cannot be applied twice"
          (let [second-undo (extract/undo! {:receipt (.getPath receipt)})]
            (is (= :stale-extraction-result (:error-type second-undo)))
            (is (= original-source (slurp source))))))
      (finally (delete-recursive! root)))))

(deftest test-extraction-undo-refuses-a-modified-target-before-writing
  (let [root (create-temp-project!)
        source (io/file root "src" "my" "app.clj")
        target (io/file root "src" "my" "distillery.clj")
        receipt (io/file root "extract.edn")]
    (try
      (extract/execute! {:file (.getPath source)
                         :forms '[distill refine]
                         :to (.getPath target)
                         :receipt-out (.getPath receipt)})
      (let [extracted-source (slurp source)
            changed-target (str (slurp target) "\n;; user change\n")]
        (spit target changed-target)
        (let [undo (extract/undo! {:receipt (.getPath receipt)})]
          (is (= :stale-extraction-result (:error-type undo)))
          (is (true? (:source-unchanged undo)))
          (is (= extracted-source (slurp source)))
          (is (= changed-target (slurp target)))))
      (finally (delete-recursive! root)))))

(deftest test-execute-refuses-a-source-that-changed-after-planning
  (let [root (create-temp-project!)
        source (io/file root "src" "my" "app.clj")
        target (io/file root "src" "my" "distillery.clj")
        real-plan extract/plan-raw
        concurrent-source (str (slurp source) "\n;; concurrent user change\n")]
    (try
      (let [result
            (with-redefs [extract/plan-raw
                          (fn [opts]
                            (let [planned (real-plan opts)]
                              (spit source concurrent-source)
                              planned))]
              (extract/execute! {:file (.getPath source)
                                 :forms '[distill refine]
                                 :to (.getPath target)}))]
        (is (= :stale-extraction-source (:error-type result)))
        (is (true? (:source-unchanged result)))
        (is (= concurrent-source (slurp source)))
        (is (not (.exists target))))
      (finally (delete-recursive! root)))))

(deftest test-production-shaped-adjacent-extraction-is-parseable-and-reversible
  (let [root (create-temp-project!)
        source (io/file root "src" "fixtures" "extract_adjacent.clj")
        target (io/file root "src" "fixtures" "stages.clj")
        receipt (io/file root "extract-adjacent.edn")
        fixture (slurp (io/file "test" "fixtures" "extract_adjacent.clj"))
        forms (mapv #(symbol (format "stage-%02d" %)) (range 1 16))]
    (try
      (.mkdirs (.getParentFile source))
      (spit source fixture)
      (let [result (extract/execute! {:file (.getPath source)
                                      :forms forms
                                      :to (.getPath target)
                                      :receipt-out (.getPath receipt)})
            future-source (slurp source)
            future-target (slurp target)]
        (is (= 15 (count (:forms (:new-file-preview result)))))
        (is (some? (parser/parse-string-all future-source)))
        (is (some? (parser/parse-string-all future-target)))
        (is (str/includes?
              future-source
              ";; This documented neighbor has no sacrificial blank line before it.\n(defn event-resume-path [event]\n  (:resume-path event))"))
        (is (= 15 (count (re-seq #"\(defn stage-" future-target))))
        (is (true? (:ok (extract/undo! {:receipt (.getPath receipt)}))))
        (is (= fixture (slurp source)))
        (is (not (.exists target))))
      (finally (delete-recursive! root)))))

(deftest test-execute-adds-require-to-source
  (let [root (create-temp-project!)]
    (try
      (let [source (str (.getPath root) "/src/my/app.clj")
            target (str (.getPath root) "/src/my/distillery.clj")
            _ (extract/execute! {:file source
                                 :forms '[distill refine]
                                 :to target})
            source-content (slurp source)]
        (testing "source has new require"
          (is (str/includes? source-content "my.distillery"))))
      (finally (delete-recursive! root)))))

(deftest test-execute-comment-header-moves-with-form
  (let [root (create-temp-project!)]
    (try
      (let [source (str (.getPath root) "/src/my/app.clj")
            target (str (.getPath root) "/src/my/distillery.clj")
            _ (extract/execute! {:file source
                                 :forms '[distill refine]
                                 :to target})
            target-content (slurp target)]
        (testing "comment header extracted with form"
          (is (str/includes? target-content ";; Distillery functions"))))
      (finally (delete-recursive! root)))))

(deftest test-execute-topological-order
  (let [root (create-temp-project!)]
    (try
      (let [source (str (.getPath root) "/src/my/app.clj")
            target (str (.getPath root) "/src/my/distillery.clj")
            _ (extract/execute! {:file source
                                 :forms '[distill refine]
                                 :to target})
            target-content (slurp target)]
        (testing "distill before refine (refine depends on distill)"
          (is (< (str/index-of target-content "(defn distill")
                 (str/index-of target-content "(defn refine")))))
      (finally (delete-recursive! root)))))

(deftest test-execute-creates-subdirectory
  (let [root (create-temp-project!)]
    (try
      (let [source (str (.getPath root) "/src/my/app.clj")
            target (str (.getPath root) "/src/my/app/distillery.clj")
            _ (extract/execute! {:file source
                                 :forms '[distill]
                                 :to target})]
        (testing "subdirectory created"
          (is (.exists (io/file target))))
        (testing "ns name derived from path"
          (let [content (slurp target)]
            (is (str/includes? content "ns my.app.distillery")))))
      (finally (delete-recursive! root)))))

(deftest test-production-extraction-keeps-only-used-requires-and-avoids-alias-collision
  ;; Minimized from sessionize-sched-killer, 2026-08-10, clj-surgeon-to4.
  (let [root (create-dependency-minimal-project!)
        source (io/file root "src" "fixture" "views.clj")
        target (io/file root "src" "fixture" "views" "schedule.clj")
        receipt (io/file root "dependency-minimal-receipt.edn")]
    (try
      (testing "the field fixture is valid before extraction"
        (is (zero? (:exit (cold-require-result root "fixture.views")))))
      ;; plan-raw: this assertion inspects the compiler's own working state,
      ;; which the reader-facing receipt deliberately no longer publishes
      (let [plan (extract/plan-raw {:file (.getPath source)
                                    :forms '[day-tab agenda-page]
                                    :to (.getPath target)})]
        (testing "the target header contains exactly the six used dependencies"
          (is (= (mapv #(format "fixture.dep%02d" %) (range 1 7))
                 (:target-requires plan)))
          (is (= 11 (count (:omitted-target-requires plan))))
          (is (= 6 (count (re-seq #"\[fixture\.dep" (:_new-file-content plan)))))
          (is (not (str/includes? (:_new-file-content plan)
                                  "fixture.domain.schedule"))))
        (testing "the unused colliding alias does not cause a source require"
          (is (= "schedule2" (:target-alias plan)))
          (is (empty? (:remaining-source-callers plan)))
          (is (empty? (:source-referred-forms plan)))
          (is (false? (:source-require-added plan))))
        (testing "the source is never reported as its own external caller"
          (is (not-any? #(= (.getCanonicalPath source)
                            (.getCanonicalPath (io/file %)))
                        (:callers-to-review plan))))
        (let [result (extract/execute! {:file (.getPath source)
                                        :forms '[day-tab agenda-page]
                                        :to (.getPath target)
                                        :receipt-out (.getPath receipt)})]
          (testing "both generated namespaces load"
            (is (= 2 (count (:forms (:new-file-preview result)))))
            (let [runtime (cold-require-result root
                                               "fixture.views"
                                               "fixture.views.schedule")]
              (is (zero? (:exit runtime)) (:err runtime))))
          (testing "the callerless source has no destination require"
            (is (not (str/includes? (slurp source) "fixture.views.schedule"))))
          (testing "undo restores both paths exactly"
            (is (true? (:ok (extract/undo! {:receipt (.getPath receipt)}))))
            (is (= (dependency-minimal-source) (slurp source)))
            (is (not (.exists target))))))
      (finally (delete-recursive! root)))))

;; @spec MCP-OP-EXTRACT-004
;; @spec MCP-OP-EXTRACT-006
;; SUPERSEDED CONTRACT, rf2-1: this used to assert the source got
;; `[fixture.moved :as moved :refer [moved]]`. Cohort rf1 measured that refer
;; list being rewritten by hand in 4 of 4 structural runs, because the task
;; forbade it. The extraction now qualifies the remaining call sites itself and
;; emits a bare `[ns :as alias]`. The invariant the old name protected --
;; the source still RUNS -- is unchanged and still asserted below by actually
;; requiring the namespace in a cold JVM.
(deftest test-source-caller-is-alias-qualified-and-runtime-valid
  (let [root (java.io.File/createTempFile "extract-source-caller" "")
        _ (.delete root)
        _ (.mkdirs root)
        source (io/file root "src" "fixture" "callers.clj")
        target (io/file root "src" "fixture" "moved.clj")
        receipt (io/file root "caller-receipt.edn")]
    (try
      (.mkdirs (.getParentFile source))
      (spit (io/file root "deps.edn") "{:paths [\"src\"]}\n")
      (spit source
            "(ns fixture.callers)\n\n(defn moved [] :ok)\n\n(defn caller [] (moved))\n")
      (let [result (extract/execute! {:file (.getPath source)
                                      :forms '[moved]
                                      :to (.getPath target)
                                      :receipt-out (.getPath receipt)})]
        (is (= [{:owner "caller" :vars ["moved"] :sites 1}]
               (:source-callers-rewired result)))
        (is (= ["moved"] (:source-referred-forms result)))
        (is (str/includes? (slurp source) "[fixture.moved :as moved]"))
        (is (not (str/includes? (slurp source) ":refer"))
            "the rewiring qualifies the sites, so a refer list is never emitted")
        (is (str/includes? (slurp source) "(moved/moved)")
            "the remaining caller is alias-qualified")
        (is (= [{:owner "caller" :vars ["moved"] :sites 1}]
               (:source-callers-rewired result)))
        (let [runtime (cold-eval-result
                        root
                        "(require 'fixture.callers) (assert (= :ok (fixture.callers/caller)))")]
          (is (zero? (:exit runtime)) (:err runtime)))
        (is (true? (:ok (extract/undo! {:receipt (.getPath receipt)})))))
      (finally (delete-recursive! root)))))

(deftest pure-compile-plan-separates-conservative-movement-from-minimization
  (let [source
        (str "(ns sample.core\n"
             "  (:require ;; keep this side effect\n"
             "   [alpha.side-effects]\n"
             "   [clojure.string :as str]))\n\n"
             "(defn helper [] (str/upper-case \"x\"))\n"
             "(defn moved [] (helper))\n"
             "(defn remains [] (moved))\n")
        input {:file "src/sample/core.clj"
               :source source
               :forms '[helper moved]
               :to "src/sample/extracted.clj"
               :target-ns "sample.extracted"
               :workspace-sources
               {"src/sample/consumer.clj"
                (str "(ns sample.consumer\n"
                     "  (:require [sample.core :as core]))\n"
                     "(def pinned #'core/moved)\n")}}
        minimal (extract/compile-plan (assoc input :require-policy :minimal))
        conservative (extract/compile-plan
                       (assoc input :require-policy :copy-all))
        candidates
        (extract/compile-candidates
          {:source source
           :source-file (:file input)
           :target-file (:to input)
           :form-ranges (:_form-texts conservative)
           :target-source (:_new-file-content conservative)
           :target-ns (:target-ns conservative)
           :target-alias (:target-alias conservative)
           :source-referred-forms (:_source-referred-forms conservative)})]
    (testing "minimal proof still refuses an unproved side-effect require"
      (is (false? (:ok minimal)))
      (is (= :unsupported-require-minimization (:error-type minimal)))
      (is (= :comment-bearing-require-clause (:reason minimal))))

    (testing "copy-all moves exact header syntax without dependency judgment"
      (is (= :copy-all (:require-policy conservative)))
      (is (= 2 (:copied-require-count conservative)))
      (is (= :copied-exactly (:target-requires conservative)))
      (is (str/includes? (:_new-file-content conservative)
                         "(:require ;; keep this side effect"))
      (is (str/includes? (:_new-file-content conservative)
                         "[alpha.side-effects]"))
      (is (= ["helper" "moved"] (:forms-to-extract conservative)))
      (is (= ["moved"] (:source-referred-forms conservative)))
      (is (nil? (:target-alias conservative))
          "copy-all needs no invented alias for a refer-only source edge")
      (is (= ["src/sample/consumer.clj"]
             (:callers-to-review conservative)))
      (is (= ["src/sample/consumer.clj"]
             (mapv :file (:quoted-var-references conservative)))))

    (testing "the complete future namespace pair parses from immutable data"
      (is (str/includes? (:source candidates)
                         "[sample.extracted :refer [moved]]"))
      (is (not (str/includes? (:source candidates) ":as nil")))
      (do
        (is (str/includes? (:target candidates)
                           "(defn moved [] (helper))"))
        (is (str/ends-with? (:_new-file-content conservative) ")\n"))
        (is (not (str/ends-with? (:_new-file-content conservative) "\n\n"))
            "the generated target is formatter-stable at end of file")))))

(deftest pure-compile-plan-can-promote-required-visibility-in-one-pass
  (let [source (str "(ns sample.core)\n\n"
                    "(defn- helper [value] (inc value))\n\n"
                    "(defn keep-me [] (helper 1))\n")
        plan (extract/compile-plan
               {:file "src/sample/core.clj"
                :source source
                :forms '[helper]
                :to "src/sample/moved.clj"
                :target-ns "sample.moved"
                :workspace-sources {}
                :derive-required-public-forms true})]
    (is (= ["helper"] (:required-public-forms plan)))
    (is (= ["helper"] (:public-forms plan)))
    (is (empty? (:missing-required-public-forms plan)))
    (is (str/includes? (:_new-file-content plan) "(defn helper "))))

(deftest test-unsupported-require-minimization-refuses-before-writing
  (let [root (java.io.File/createTempFile "extract-side-effect-require" "")
        _ (.delete root)
        _ (.mkdirs root)
        source (io/file root "src" "fixture" "effects.clj")
        target (io/file root "src" "fixture" "moved.clj")
        original "(ns fixture.effects (:require [fixture.side-effects]))\n\n(defn moved [] :ok)\n"]
    (try
      (.mkdirs (.getParentFile source))
      (spit (io/file root "deps.edn") "{:paths [\"src\"]}\n")
      (spit source original)
      (let [result (extract/execute! {:file (.getPath source)
                                      :forms '[moved]
                                      :to (.getPath target)})]
        (is (= :unsupported-require-minimization (:error-type result)))
        (is (= :side-effect-only-require (:reason result)))
        (is (= original (slurp source)))
        (is (not (.exists target))))
      (finally (delete-recursive! root)))))

;; @spec MCP-OP-EXTRACT-012
(deftest an-unprovable-source-header-degrades-instead-of-failing-the-extraction
  (testing "a comment-bearing require clause cannot be narrowed, and that is
            not a reason to fail an extraction that is otherwise correct"
    (let [source (str "(ns sample.core\n"
                      "  (:require ;; keep this side effect\n"
                      "   [alpha.side-effects]\n"
                      "   [clojure.string :as str]))\n\n"
                      "(defn moved [] (str/upper-case \"x\"))\n"
                      "(defn remains [] :ok)\n")
          plan (extract/compile-plan
                 {:file "src/sample/core.clj"
                  :source source
                  :forms '[moved]
                  :to "src/sample/extracted.clj"
                  :target-ns "sample.extracted"})
          candidates (extract/compile-candidates
                       {:source source
                        :source-file "src/sample/core.clj"
                        :target-file "src/sample/extracted.clj"
                        :form-ranges (:_form-texts plan)
                        :target-source (:_new-file-content plan)
                        :target-ns "sample.extracted"
                        :target-alias (get-in plan [:header :alias])
                        :source-referred-forms (:_source-referred-forms plan)
                        :moved-sources (:_moved-sources plan)
                        :remaining-callers (:remaining-source-callers plan)})]
      (is (:ok candidates)
          (str "the extraction must still complete: " (pr-str candidates)))
      (is (string? (:source candidates)))
      (is (str/includes? (:source candidates) ";; keep this side effect")
          "the unprovable header is left exactly as the caller wrote it")
      (is (str/includes? (:source candidates) "[alpha.side-effects]"))
      (is (some? (:narrowing-note candidates))
          "and the caller is told narrowing was unavailable")
      (is (nil? (:removed-requires candidates))))))

(defn- create-caller-project!
  "A project whose monolith is used by two other namespaces: one that uses only
  moved Vars (so its require is REPLACED) and one that uses both moved and
  staying Vars (so the target require is ADDED beside the existing one)."
  []
  (let [root (java.io.File/createTempFile "extract-rewire-project" "")
        _ (.delete root)
        _ (.mkdirs root)
        src (io/file root "src" "app")]
    (.mkdirs src)
    (spit (io/file root "deps.edn") "{:paths [\"src\"]}\n")
    (spit (io/file src "core.clj")
          (str "(ns app.core\n"
               "  (:require\n"
               "   [clojure.string :as str]))\n\n"
               "(defn moved-one [x] (str/upper-case x))\n\n"
               "(defn moved-two [x] (moved-one x))\n\n"
               "(defn stays [x] (moved-two x))\n\n"
               "(defn also-stays [] :ok)\n"))
    (spit (io/file src "only_moved.clj")
          (str "(ns app.only-moved\n"
               "  (:require\n"
               "   [app.core :as core]))\n\n"
               "(defn go [x] (core/moved-two x))\n"))
    (spit (io/file src "mixed.clj")
          (str "(ns app.mixed\n"
               "  (:require\n"
               "   [app.core :as core]\n"
               "   [clojure.test :refer [deftest is]]))\n\n"
               "(defn go [x] [(core/moved-one x) (core/also-stays)])\n"))
    root))

;; @spec MCP-OP-EXTRACT-001
;; @spec MCP-OP-EXTRACT-008
(deftest extraction-rewires-every-proved-caller-in-one-transaction
  (let [root (create-caller-project!)
        source (io/file root "src" "app" "core.clj")
        target (io/file root "src" "app" "moved.clj")
        only-moved (io/file root "src" "app" "only_moved.clj")
        mixed (io/file root "src" "app" "mixed.clj")
        receipt (io/file root "rewire-receipt.edn")
        before {:source (slurp source) :only-moved (slurp only-moved)
                :mixed (slurp mixed)}]
    (try
      (let [result (extract/execute! {:file (.getPath source)
                                      :forms '[moved-one moved-two]
                                      :to (.getPath target)
                                      :alias "moved"
                                      :receipt-out (.getPath receipt)})
            target-text (slurp target)]

        (testing "the target emits the caller's declared order"
          ;; @spec MCP-OP-EXTRACT-001
          (is (< (str/index-of target-text "(defn moved-one")
                 (str/index-of target-text "(defn moved-two"))))

        (testing "the source is narrowed, qualified and requires the target"
          (let [source-text (slurp source)]
            (is (str/includes? source-text "[app.moved :as moved]"))
            (is (not (str/includes? source-text ":refer")))
            (is (str/includes? source-text "(moved/moved-two x)"))
            (is (not (str/includes? source-text "[clojure.string :as str]"))
                "the require whose last use left with the moved forms is gone")))

        (testing "a caller whose only uses were moved has its require REPLACED"
          (let [text (slurp only-moved)]
            (is (str/includes? text "[app.moved :as moved]"))
            (is (not (str/includes? text "app.core")))
            (is (str/includes? text "(moved/moved-two x)"))))

        (testing "a caller that still uses the source gets the target require ADDED,
                  and its unrelated :refer entry is untouched"
          (let [text (slurp mixed)]
            (is (str/includes? text "[app.core :as core]"))
            (is (str/includes? text "[app.moved :as moved]"))
            (is (str/includes? text "[clojure.test :refer [deftest is]]"))
            (is (str/includes? text "(moved/moved-one x)"))
            (is (str/includes? text "(core/also-stays)"))))

        (testing "the receipt names every rewired file"
          (is (= 2 (count (:external-callers-rewired result))))
          (is (= #{:replaced :added}
                 (set (map :require-action (:external-callers-rewired result)))))
          (is (= [] (:callers-unresolved result)))
          (is (true? (:complete result))))

        (testing "every rewritten namespace loads in a cold JVM"
          (let [runtime (cold-require-result root "app.core" "app.moved"
                                             "app.only-moved" "app.mixed")]
            (is (zero? (:exit runtime)) (:err runtime))))

        (testing "undo restores the complete proved file set, not just two files"
          ;; @spec MCP-OP-EXTRACT-008
          (is (true? (:ok (extract/undo! {:receipt (.getPath receipt)}))))
          (is (= (:source before) (slurp source)))
          (is (= (:only-moved before) (slurp only-moved)))
          (is (= (:mixed before) (slurp mixed)))
          (is (not (.exists target)))))
      (finally (delete-recursive! root)))))

;; @spec MCP-OP-EXTRACT-009
;; @spec MCP-OP-EXTRACT-011
(deftest the-dry-run-previews-the-same-plan-the-executor-applies
  (let [root (create-caller-project!)
        source (io/file root "src" "app" "core.clj")
        target (io/file root "src" "app" "moved.clj")
        before (slurp source)]
    (try
      ;; the RECEIPT is the preview now: there is no second shape to learn
      (let [preview (extract/plan {:file (.getPath source)
                                   :forms '[moved-one moved-two]
                                   :to (.getPath target)
                                   :alias "moved"
                                   :doc "Moved out of app.core."})]
        (testing "the dry run writes nothing"
          (is (= before (slurp source)))
          (is (not (.exists target))))

        (testing "the target preview carries the header the executor will write"
          (is (= ["moved-one" "moved-two"] (mapv :name (:forms (:new-file-preview preview)))))
          (is (str/includes? (get-in preview [:new-file-preview :ns-form])
                             "\"Moved out of app.core.\""))
          (is (= ["clojure.string"] (get-in preview [:header :requires-kept]))))

        (testing "the source preview names what it will remove and qualify"
          (is (= ["clojure.string"] (get-in preview [:source-header :requires-removed])))
          (is (= [{:owner "stays" :vars ["moved-two"] :sites 1}]
                 (:source-callers-rewired preview))))

        (testing "every caller is previewed with its per-file action"
          (is (= #{[:replaced 1] [:added 1]}
                 (set (map (juxt :require-action :sites)
                           (:external-callers-rewired preview)))))))

      (testing ":public promotes exactly the named private forms"
        ;; @spec MCP-OP-EXTRACT-011
        (let [private-source (io/file root "src" "app" "private.clj")]
          (.mkdirs (.getParentFile private-source))
          (spit private-source
                (str "(ns app.private)\n\n"
                     "(defn- helper [] :ok)\n\n"
                     "(defn user [] (helper))\n"))
          (let [out (io/file root "src" "app" "promoted.clj")
                result (extract/execute! {:file (.getPath private-source)
                                          :forms '[helper]
                                          :to (.getPath out)
                                          :public '[helper]})]
            (is (nil? (:error result)) (pr-str result))
            (is (str/includes? (slurp out) "(defn helper"))
            (is (not (str/includes? (slurp out) "(defn- helper"))))
          (let [out2 (io/file root "src" "app" "promoted2.clj")
                refusal (extract/execute! {:file (.getPath private-source)
                                           :forms '[user]
                                           :to (.getPath out2)
                                           :public '[user]})]
            (is (= :invalid-public-forms (:error-type refusal))
                "a form that is not a selected PRIVATE form refuses")
            (is (not (.exists out2))))))
      (finally (delete-recursive! root)))))

;; @spec MCP-OP-EXTRACT-013
(deftest a-declared-order-that-would-need-a-declare-refuses
  (testing "honouring the caller's order means the caller can state one that
            does not compile; that refuses and names the order that works"
    (let [source (str "(ns sample.core)\n\n"
                      "(defn first-fn [] :ok)\n\n"
                      "(defn second-fn [] (first-fn))\n\n"
                      "(defn stays [] (second-fn))\n")
          refusal (extract/compile-plan
                    {:file "src/sample/core.clj"
                     :source source
                     ;; second-fn calls first-fn, so this order needs a declare
                     :forms '[second-fn first-fn]
                     :to "src/sample/extracted.clj"
                     :target-ns "sample.extracted"})]
      (is (= :forward-reference-in-declared-order (:error-type refusal)))
      (is (= [{:form "second-fn" :depends-on "first-fn"}]
             (:forward-references refusal)))
      (is (= ["first-fn" "second-fn"] (:dependency-order refusal))
          "the refusal hands back an order that satisfies the constraint")
      (is (true? (:source-unchanged refusal)))
      (is (true? (:target-unchanged refusal)))))

  (testing "the order the refusal recommends is accepted"
    (let [source (str "(ns sample.core)\n\n"
                      "(defn first-fn [] :ok)\n\n"
                      "(defn second-fn [] (first-fn))\n\n"
                      "(defn stays [] (second-fn))\n")
          plan (extract/compile-plan
                 {:file "src/sample/core.clj"
                  :source source
                  :forms '[first-fn second-fn]
                  :to "src/sample/extracted.clj"
                  :target-ns "sample.extracted"})]
      (is (nil? (:error plan)) (pr-str (select-keys plan [:error :error-type])))
      (is (= ["first-fn" "second-fn"]
             (mapv :name (:forms (:new-file-preview plan))))))))

;; @spec MCP-OP-EXTRACT-014
(deftest a-target-namespace-is-derived-from-the-workspace-not-the-server
  (testing "the workspace's own deps.edn decides its source roots"
    (let [root (java.io.File/createTempFile "extract-workspace-ns" "")
          _ (.delete root)
          _ (.mkdirs root)]
      (try
        ;; this workspace calls its source root "source", not "src"
        (spit (io/file root "deps.edn") "{:paths [\"source\"]}\n")
        (.mkdirs (io/file root "source" "app"))
        (is (= ["source"] (extract/source-paths-in-root (.getPath root))))
        (is (= "app.moved"
               (extract/workspace-target-ns
                 (.getPath root)
                 (.getPath (io/file root "source" "app" "moved.clj"))))
            "a hard-coded [src test dev] would not have matched this workspace")
        (finally (delete-recursive! root)))))

  (testing "a directory named src ABOVE the workspace cannot name the namespace"
    (let [outer (java.io.File/createTempFile "extract-outer-src" "")
          _ (.delete outer)
          _ (.mkdirs outer)
          ;; the workspace itself lives under a path containing /src/
          root (io/file outer "src" "vendored-project")]
      (try
        (.mkdirs (io/file root "src" "app"))
        (spit (io/file root "deps.edn") "{:paths [\"src\"]}\n")
        (is (= "app.moved"
               (extract/workspace-target-ns
                 (.getPath root)
                 (.getPath (io/file root "src" "app" "moved.clj"))))
            "the enclosing /src/ must not decide the name")
        (finally (delete-recursive! outer)))))

  (testing "a workspace with no deps.edn falls back to the conventional roots"
    (let [root (java.io.File/createTempFile "extract-no-deps" "")
          _ (.delete root)
          _ (.mkdirs root)]
      (try
        (is (nil? (extract/source-paths-in-root (.getPath root))))
        (is (= "app.moved"
               (extract/workspace-target-ns
                 (.getPath root)
                 (.getPath (io/file root "src" "app" "moved.clj")))))
        (finally (delete-recursive! root))))))

;; ============================================================
;; rf2 follow-up — the receipt must state what it guarantees, and a reader who
;; did not drive the run must be able to determine the next call from it.
;;
;; Field evidence, metered session: the receipt reported forms-extracted,
;; new-file-lines, callers-to-review, target-requires — and a driver who KNEW to
;; check found the header correct. A fresh model given only the receipt text
;; could not: it read `:remaining-source-callers` and `:callers-to-review 4` as
;; unfinished work it had to do by hand, when the tool had rewired every one of
;; them; it could not tell whether the change was applied or previewed; it never
;; learned the new namespace's name; and `:quoted-var-references 0` was named
;; but never explained.
;; ============================================================

(defn- rf1-fixture-plan
  "The rf1 extraction, compiled against this repository's own real bytes."
  []
  (extract/compile-plan
    {:file "src/clj_surgeon/mcp_change_buffer.clj"
     :source (slurp "src/clj_surgeon/mcp_change_buffer.clj")
     :forms '[exact-verification-visible-bytes expand-command bytes->hex
              sha256-text run-process! admission-unverified?
              compile-exact-profile classify-exact-process-outcome
              run-exact-verification!]
     :to "src/clj_surgeon/mcp_exact_verify.clj"
     :target-ns "clj-surgeon.mcp-exact-verify"
     :alias "exact-verify"
     :derive-required-public-forms true}))

(defn- rf1-fixture-receipt [applied]
  (let [plan (rf1-fixture-plan)
        source (get-in plan [:_preview :source])]
    (extract/receipt-map
      {:applied applied
       :plan plan
       :candidates {:source-rewrites (:call-sites-qualified source)
                    :removed-requires (:removed-requires source)
                    :removed-imports (:removed-imports source)}
       :would "clj-surgeon :op :extract! ..."})))

;; @spec MCP-OP-EXTRACT-015
(deftest the-receipt-states-the-header-guarantees
  (let [header (:header (rf1-fixture-receipt true))]
    (testing "every guarantee key is present"
      (is (= #{:docstring :requires-kept :requires-pruned
               :imports-kept :imports-pruned :visibility-derived
               :alias :refer}
             (set (keys header)))
          "requires are stated exactly the way imports are"))

    (testing "requires are guaranteed symmetrically with imports"
      (let [header (:header (rf1-fixture-receipt true))]
        (is (= ["clj-surgeon.mcp-process" "clojure.java.io" "clojure.string"]
               (:requires-kept header)))
        (is (= 12 (count (:requires-pruned header))))))

    (testing "and each one is correct on the rf1 fixture"
      (is (= :none (:docstring header))
          "no :doc was supplied, so the source's docstring was NOT copied")
      (is (= ["(java.nio.charset StandardCharsets)"
              "(java.security MessageDigest)"]
             (:imports-kept header)))
      (is (= ["(java.nio.file LinkOption Path Paths)" "(java.util UUID)"]
             (:imports-pruned header))
          "the four imports rf1's agents deleted by hand are named as pruned")
      (is (= ["admission-unverified?"] (:visibility-derived header))
          "the promotion rf1 silently skipped is stated, not implied")
      (is (= "exact-verify" (:alias header)))
      (is (= :none (:refer header))
          "the refer list rf1's agents rewrote in 4 of 4 runs is stated absent"))

    (testing "a caller-supplied docstring is reported as such, not as :none"
      (is (= :caller-supplied
             (:docstring (extract/header-guarantees
                           {:require-policy :minimal :doc "Mine."})))))

    (testing "copy-all does not claim :none for a docstring it copied"
      (is (= :copied-from-source
             (:docstring (extract/header-guarantees
                           {:require-policy :copy-all})))))

    (testing "a refer the extraction really did emit is named, not hidden"
      (is (= ["a" "b"]
             (:refer (extract/header-guarantees
                       {:require-policy :minimal :refer-emitted ["a" "b"]})))))

    (testing "the source header's removals are grouped and stated"
      (let [source-header (:source-header (rf1-fixture-receipt true))]
        (is (= ["clj-surgeon.mcp-process"] (:requires-removed source-header)))
        (is (= ["(java.nio.charset StandardCharsets)"
                "(java.security MessageDigest)"]
               (:imports-removed source-header)))))))

;; @spec MCP-OP-EXTRACT-016
(deftest a-cold-reader-can-determine-the-next-call-from-the-receipt
  (let [receipt (rf1-fixture-receipt true)]
    (testing "the receipt leads with what happened and to what"
      (is (= [:applied :target-ns :target-file :header :source-header
              :source-callers-rewired :external-callers-rewired
              :callers-unresolved :complete :compile]
             (take 10 (keys receipt)))
          "printed order is the reading order; array-map preserves it")
      (is (true? (:applied receipt)))
      (is (= "clj-surgeon.mcp-exact-verify" (:target-ns receipt)))
      (is (= "src/clj_surgeon/mcp_exact_verify.clj" (:target-file receipt))))

    (testing "caller fields name a state, and nothing is outstanding"
      (is (= [{:owner "diagnostic-command" :vars ["expand-command"] :sites 1}
              {:owner "run-check!"
               :vars ["admission-unverified?" "run-process!"] :sites 2}
              {:owner "run-diagnostic-check!"
               :vars ["admission-unverified?" "run-process!"] :sites 2}
              {:owner "run-verification!" :vars ["expand-command"] :sites 2}]
             (:source-callers-rewired receipt))
          "the seven internal sites are reported as REWIRED, not as work to do")
      (is (= [] (:callers-unresolved receipt)))
      (is (true? (:complete receipt))
          ":complete is true exactly when :callers-unresolved is empty")
      (is (every? #{:file :old-alias :sites :require-action}
                  (mapcat keys (:external-callers-rewired receipt)))))

    (testing "the history that misled a cold reader is demoted, not top level"
      (is (nil? (:callers-to-review receipt)))
      (is (nil? (:remaining-source-callers receipt)))
      (is (string? (get-in receipt [:history :note]))
          "history now points at the fields that replaced it")
      (is (nil? (:summary receipt))
          "and no tally block restates the vectors above it"))

    (testing "what has NOT been checked says so, and names the next call"
      (is (false? (get-in receipt [:compile :checked])))
      (is (= :not-run (get-in receipt [:compile :status])))
      (is (= ["clj-surgeon.mcp-exact-verify" "clj-surgeon.mcp-change-buffer"]
             (take 2 (get-in receipt [:compile :namespaces]))))
      (is (str/includes? (get-in receipt [:compile :command])
                         "'clj-surgeon.mcp-exact-verify")
          "the command is executable, not a description of one"))

    (testing "a named-but-unexplained count is renamed and explained"
      (is (nil? (:quoted-var-references receipt)))
      (is (= [] (:quoted-var-references-unrewired receipt)))
      (is (string? (:note receipt)))))

  (testing "a dry run says so and hands back the command that applies it"
    (let [preview (rf1-fixture-receipt false)]
      (is (false? (:applied preview)))
      (is (= [:applied :would :target-ns :target-file :header]
             (take 5 (keys preview))))
      (is (= (dissoc (rf1-fixture-receipt true) :applied)
             (dissoc preview :applied :would))
          "the dry run prints the SAME map, so there is only one to learn"))))

;; ============================================================
;; rf2 follow-up — a receipt too long to read is ignored, and an unchecked
;; apply leaves its own correctness unproven.
;; Field evidence: the :extract dry run on the rf1 fixture printed 346,519
;; bytes, of which 337,447 were `_`-prefixed executor working state — three
;; whole caller files twice over in :_caller-plans (238 KB) and the whole
;; source in :_source (78 KB) — against 8,205 bytes of signal. This is the rf1
;; ethnography's `output too long to use` deviation: rf1-g2-A-1 call 06 got a
;; dry run truncated at 23,888 tokens and ignored it.
;; ============================================================

;; @spec MCP-OP-EXTRACT-017
;; @spec MCP-OP-EXTRACT-018
(deftest the-receipt-is-bounded-and-carries-no-file-text
  (let [receipt (rf1-fixture-receipt false)
        encoded (pr-str receipt)
        strings (filter string? (tree-seq coll? seq receipt))]
    (testing "no value in a receipt is ever a source file"
      (is (empty? (filter #(> (count %) 2000) strings))
          (str "longest string was "
               (apply max 0 (map count strings)) " chars"))
      (is (not (str/includes? encoded "Proof-carrying semantic selection"))
          "the source file's own text must not appear anywhere")
      (is (not (str/includes? encoded "(defn compile-exact-profile"))
          "nor the moved forms' bodies"))

    (testing "the encoded receipt fits in one readable output"
      (is (< (count encoded) 4096)
          (str "receipt was " (count encoded) " bytes")))

    (testing "no executor working state escapes"
      (is (empty? (filter extract/private-plan-field? (keys receipt)))))

    (testing "the preview is the ns form plus form names with line ranges"
      (let [preview (:new-file-preview receipt)]
        (is (str/starts-with? (:ns-form preview) "(ns clj-surgeon.mcp-exact-verify"))
        (is (= 9 (count (:forms preview))))
        (is (= {:name "expand-command" :type "defn" :lines [12 27]}
               (second (:forms preview))))
        (is (= "defn" (:type (first (filter #(= "admission-unverified?" (:name %))
                                            (:forms preview)))))
            "the resulting kind, after promotion — not the kind it had before")
        (is (>= 20 (count (:forms preview))))))))

;; @spec MCP-OP-EXTRACT-019
(deftest apply-reports-a-checked-compile
  (let [root (create-caller-project!)
        source (io/file root "src" "app" "core.clj")
        target (io/file root "src" "app" "moved.clj")]
    (try
      (let [result (extract/execute! {:file (.getPath source)
                                      :forms '[moved-one moved-two]
                                      :to (.getPath target)
                                      :alias "moved"})
            compiled (:compile result)]
        (is (true? (:checked compiled))
            (str "the apply must compile what it wrote: " (pr-str compiled)))
        (is (true? (:ok compiled)) (pr-str compiled))
        (is (= :run (:status compiled)))
        (is (= ["app.moved" "app.core"] (take 2 (:namespaces compiled)))
            "target then source")
        (is (= #{"app.moved" "app.core" "app.only-moved" "app.mixed"}
               (set (:namespaces compiled)))
            "every touched namespace is compiled, callers included")
        (is (nil? (:undo result)) "a green compile needs no revert instruction"))
      (finally (delete-recursive! root))))

  (testing "the dry run does not compile, and says the apply will"
    (let [root (create-caller-project!)
          source (io/file root "src" "app" "core.clj")]
      (try
        (let [compiled (:compile (extract/plan
                                   {:file (.getPath source)
                                    :forms '[moved-one moved-two]
                                    :to (.getPath (io/file root "src" "app" "moved.clj"))
                                    :alias "moved"}))]
          (is (false? (:checked compiled)))
          (is (true? (:will-check compiled)))
          (is (str/includes? (:command compiled) "clojure.main -e")))
        (finally (delete-recursive! root))))))

;; @spec MCP-OP-EXTRACT-019
;; @spec MCP-OP-EXTRACT-020
(deftest a-compile-failure-after-apply-is-reported-with-undo
  (testing "a moved form that references a Var the new namespace cannot see"
    (let [root (java.io.File/createTempFile "extract-compile-fail" "")
          _ (.delete root)
          _ (.mkdirs root)
          source (io/file root "src" "app" "core.clj")
          target (io/file root "src" "app" "moved.clj")
          receipt (io/file root "r.edn")]
      (try
        (.mkdirs (.getParentFile source))
        (spit (io/file root "deps.edn") "{:paths [\"src\"]}\n")
        ;; `helper` STAYS behind, so the moved form cannot resolve it
        (spit source
              (str "(ns app.core)\n\n"
                   "(def helper 41)\n\n"
                   "(defn moved [] (inc helper))\n"))
        (let [result (extract/execute! {:file (.getPath source)
                                        :forms '[moved]
                                        :to (.getPath target)
                                        :receipt-out (.getPath receipt)})
              compiled (:compile result)]
          (is (true? (:checked compiled)) (pr-str compiled))
          (is (false? (:ok compiled))
              (str "the failure IS attributable to a changed file: "
                   (pr-str compiled)))
          (is (str/includes? (str (:output-tail compiled)) "helper")
              "the tail names the unresolved Var")

          (testing "the bytes are on disk and the receipt says how to revert"
            (is (.exists target) "a failed compile does NOT auto-revert")
            (is (= (.getPath receipt) (get-in result [:undo :receipt])))
            (is (str/includes? (get-in result [:undo :command])
                               ":op :undo-extract!"))
            (is (str/includes? (get-in result [:undo :note]) "not reverted")))

          (testing "and the named revert actually works"
            (is (true? (:ok (extract/undo! {:receipt (.getPath receipt)}))))
            (is (not (.exists target)))))
        (finally (delete-recursive! root)))))

  (testing "a classpath the runner cannot resolve is :unverified, never false"
    (is (= {:ok :unverified :reason :classpath-incomplete}
           (extract/attribute-compile-failure
             "Could not locate nrepl/core.clj on classpath" ["a.clj"])))
    (is (= :unverified
           (:ok (extract/attribute-compile-failure
                  "Syntax error at (untouched_thing.clj:12)." ["a.clj" "b.clj"])))
        "an error raised inside a file this change never touched is unverified")
    (is (= {:ok false}
           (extract/attribute-compile-failure
             "Unable to resolve symbol: helper (a.clj:5)." ["src/x/a.clj"]))
        "an error inside a file we DID change is attributable")))
