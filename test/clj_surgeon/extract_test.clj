(ns clj-surgeon.extract-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-surgeon.extract :as extract]
            [clojure.string :as str]
            [clojure.java.io :as io]))

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
        (testing "source has require for new ns"
          (is (str/includes? (slurp source) "my.helpers"))))
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
          (is (str/includes? (:new-file-preview p) "defn distill"))
          (is (str/includes? (:new-file-preview p) "defn refine"))))
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

(deftest test-plan-preserves-requires
  (let [root (create-temp-project!)]
    (try
      (let [source (str (.getPath root) "/src/my/app.clj")
            target (str (.getPath root) "/src/my/distillery.clj")
            p (extract/plan {:file source
                             :forms '[distill]
                             :to target})]
        (testing "new file has source's requires (over-include)"
          (is (str/includes? (:new-file-preview p) "clojure.string"))))
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
        (testing "new file has requires from source"
          (let [content (slurp target)]
            (is (str/includes? content "clojure.string"))))
        (testing "summary correct"
          (is (= 2 (-> result :summary :forms-extracted)))))
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
            result (extract/execute! {:file source
                                      :forms '[distill]
                                      :to target})]
        (testing "subdirectory created"
          (is (.exists (io/file target))))
        (testing "ns name derived from path"
          (let [content (slurp target)]
            (is (str/includes? content "ns my.app.distillery")))))
      (finally (delete-recursive! root)))))
