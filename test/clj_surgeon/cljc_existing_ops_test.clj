(ns clj-surgeon.cljc-existing-ops-test
  "Comprehensive tests for existing clj-surgeon operations on .cljc files.
   These test the AUGMENTED read-only ops (outline, deps, topo, dep-tree,
   extraction-closure) and document the limitations of write ops (mv, extract)
   on platform-gated forms.

   Fixtures modeled on real Fulcro RAD patterns from a production project:
   46 .cljc files, divergent dom/dom-server aliases, mutation splits,
   cross-platform dependency chains, spliced requires.

   Supported platforms: :clj, :cljs, :cljc (reader conditionals).
   ClojureDart (:cljd) is not yet handled — other conditional platforms
   will need to be added to cljc/walk.clj's all-platforms-by-ext map."
  (:require [clojure.test :refer [deftest is testing]]
            [clj-surgeon.outline :as outline]
            [clj-surgeon.analyze :as analyze]
            [clj-surgeon.move :as move]
            [clj-surgeon.extract :as extract]
            [rewrite-clj.zip :as z]
            [clojure.string :as str]
            [clojure.java.io :as io]))

;; ============================================================
;; Helpers
;; ============================================================

(defn- with-temp-cljc-file
  "Write source to a temp .cljc file, call (f path), clean up."
  [source f]
  (let [tmp (java.io.File/createTempFile "cljc-test" ".cljc")]
    (spit tmp source)
    (try
      (f (.getAbsolutePath tmp))
      (finally
        (.delete tmp)))))

(defn- with-temp-clj-file
  "Write source to a temp .clj file, call (f path), clean up."
  [source f]
  (let [tmp (java.io.File/createTempFile "cljc-test" ".clj")]
    (spit tmp source)
    (try
      (f (.getAbsolutePath tmp))
      (finally
        (.delete tmp)))))

(defn- outline-cljc [fixture-name]
  (outline/outline (str "test-fixtures/cljc/existing-ops/" fixture-name)))

(defn- forms-by-name [result]
  (into {} (map (juxt :name identity)) (:forms result)))

(defn- deps-by-name [deps-list]
  (into {} (map (juxt :name identity)) deps-list))

;; ============================================================
;; :ls / outline tests — forms inside #?() are now visible
;; ============================================================

(deftest test-ls-logging-cljc
  (testing "logging.cljc: #?@(:clj ...) splice in ns + CLJ-gated defn/defmacro.
            All 5 named forms should be visible (was 2/5 on main)."
    (let [result (outline-cljc "logging.cljc")
          by-name (forms-by-name result)]
      (is (= 'myapp.lib.logging (:ns result)))
      (is (= 5 (:form-count result)))
      ;; Shared forms
      (is (= [:clj :cljs] (:platforms (by-name 'format-level))))
      (is (= [:clj :cljs] (:platforms (by-name 'truncate-ns))))
      ;; CLJ-gated forms
      (is (= [:clj] (:platforms (by-name 'p))))
      (is (= 'defmacro (:type (by-name 'p))))
      (is (= [:clj] (:platforms (by-name 'custom-output-fn))))
      (is (= [:clj] (:platforms (by-name 'configure-logging!)))))))

(deftest test-ls-dashboard-cljc
  (testing "dashboard.cljc: divergent dom/dom-server alias + #?@(:cljs ...) splice
            requires + mixed CLJ/CLJS body forms. 5 named forms visible."
    (let [result (outline-cljc "dashboard.cljc")
          by-name (forms-by-name result)]
      (is (= 5 (:form-count result)))
      (is (= [:clj :cljs] (:platforms (by-name 'format-title))))
      (is (= [:clj :cljs] (:platforms (by-name 'render-header))))
      (is (= [:cljs] (:platforms (by-name 'render-modal))))
      (is (= [:cljs] (:platforms (by-name 'render-dashboard))))
      (is (= [:clj] (:platforms (by-name 'render-page)))))))

(deftest test-ls-mutations-cljc
  (testing "mutations.cljc: mutation split #?(:cljs (m/defmutation ...) :clj (do ...)).
            The :cljs branch yields m/defmutation (no :name — not in def-types).
            The :clj branch yields (do ...) (no :name either).
            Shared helpers + CLJ-only def are named."
    (let [result (outline-cljc "mutations.cljc")
          by-name (forms-by-name result)
          all-forms (:forms result)]
      ;; Named forms
      (is (= 3 (:form-count result)))
      (is (some? (by-name 'validate-post-id)))
      (is (some? (by-name 'format-result)))
      (is (some? (by-name 'resolvers)))
      ;; Platform tags on named forms
      (is (= [:clj :cljs] (:platforms (by-name 'validate-post-id))))
      (is (= [:clj] (:platforms (by-name 'resolvers))))
      ;; The mutation split forms exist but are unnamed
      (is (some #(= 'm/defmutation (:type %)) all-forms)
          "m/defmutation should be surfaced from :cljs branch")
      (is (some #(= 'do (:type %)) all-forms)
          "(do ...) should be surfaced from :clj branch"))))

(deftest test-ls-posts-cljc
  (testing "posts.cljc: declare + shared dep chain + CLJ resolvers + CLJS transforms.
            All 9 named forms visible (was 5/9 on main)."
    (let [result (outline-cljc "posts.cljc")
          by-name (forms-by-name result)]
      (is (= 9 (:form-count result)))
      ;; Shared forms
      (is (= [:clj :cljs] (:platforms (by-name 'normalize-title))))
      (is (= [:clj :cljs] (:platforms (by-name 'format-post))))
      (is (= [:clj :cljs] (:platforms (by-name 'add-slug))))
      ;; CLJ-only resolvers
      (is (= [:clj] (:platforms (by-name 'resolve-posts))))
      (is (= [:clj] (:platforms (by-name 'resolve-post-detail))))
      ;; CLJS-only transforms
      (is (= [:cljs] (:platforms (by-name 'client-format-post))))
      (is (= [:cljs] (:platforms (by-name 'post-summary))))
      ;; Forward ref detection depends on clj-kondo classpath availability
      ;; In test context it may be empty; just verify the key exists
      (is (contains? result :forward-refs)))))

(deftest test-ls-io-cljc
  (testing "io.cljc: 1 shared + 2 CLJ + 2 CLJS (was 1/5 on main)."
    (let [result (outline-cljc "io.cljc")
          by-name (forms-by-name result)]
      (is (= 5 (:form-count result)))
      (is (= [:clj :cljs] (:platforms (by-name 'sanitize-filename))))
      (is (= [:clj] (:platforms (by-name 'read-file))))
      (is (= [:clj] (:platforms (by-name 'write-file))))
      (is (= [:cljs] (:platforms (by-name 'get-element))))
      (is (= [:cljs] (:platforms (by-name 'format-number)))))))

(deftest test-ls-spliced-require-ns-detected
  (testing "NS name is detected even with #?@() splice in the :require block."
    (let [result (outline-cljc "logging.cljc")]
      (is (= 'myapp.lib.logging (:ns result))))))

(deftest test-ls-clj-file-unchanged
  (testing "Regression: a plain .clj file still works. All forms get [:clj]."
    (with-temp-clj-file "(ns my.plain)\n\n(defn foo [x] (inc x))\n(defn bar [x] (dec x))\n"
      (fn [path]
        (let [result (outline/outline path)
              by-name (forms-by-name result)]
          (is (= 'my.plain (:ns result)))
          (is (= 2 (:form-count result)))
          (is (= [:clj] (:platforms (by-name 'foo))))
          (is (= [:clj] (:platforms (by-name 'bar)))))))))

(deftest test-ls-platforms-design-decision
  (testing "Design decision: shared forms in .cljc get [:clj :cljs] — one entry,
            NOT separate entries per platform."
    (let [result (outline-cljc "io.cljc")
          sanitize-forms (filter #(= 'sanitize-filename (:name %)) (:forms result))]
      (is (= 1 (count sanitize-forms))
          "Shared form appears exactly once, not duplicated per platform")
      (is (= [:clj :cljs] (:platforms (first sanitize-forms)))))))

;; ============================================================
;; :deps / intra-ns-deps — cross-boundary dependencies visible
;; ============================================================

(deftest test-deps-logging-cross-boundary
  (testing "CLJ-gated forms depend on shared helpers across platform boundary."
    (let [zloc (analyze/file->zloc "test-fixtures/cljc/existing-ops/logging.cljc")
          deps (analyze/intra-ns-deps zloc)
          by-name (deps-by-name deps)]
      ;; configure-logging! (CLJ) depends on custom-output-fn (CLJ) + format-level (shared)
      (is (= #{"custom-output-fn" "format-level"}
             (:depends-on (by-name "configure-logging!"))))
      ;; custom-output-fn (CLJ) depends on truncate-ns (shared)
      (is (= #{"truncate-ns"}
             (:depends-on (by-name "custom-output-fn"))))
      ;; format-level and truncate-ns are leaves
      (is (empty? (:depends-on (by-name "format-level"))))
      (is (empty? (:depends-on (by-name "truncate-ns")))))))

(deftest test-deps-dashboard-cljs-depends-on-shared
  (testing "CLJS-gated forms depend on shared helpers."
    (let [zloc (analyze/file->zloc "test-fixtures/cljc/existing-ops/dashboard.cljc")
          deps (analyze/intra-ns-deps zloc)
          by-name (deps-by-name deps)]
      ;; render-dashboard (CLJS) depends on render-header (shared) + render-modal (CLJS)
      (is (contains? (:depends-on (by-name "render-dashboard")) "render-header"))
      (is (contains? (:depends-on (by-name "render-dashboard")) "render-modal"))
      ;; render-modal (CLJS) depends on render-header (shared)
      (is (contains? (:depends-on (by-name "render-modal")) "render-header"))
      ;; render-page (CLJ) depends on render-header (shared)
      (is (contains? (:depends-on (by-name "render-page")) "render-header")))))

(deftest test-deps-posts-cross-boundary
  (testing "Both CLJ resolvers and CLJS transforms depend on shared format-post."
    (let [zloc (analyze/file->zloc "test-fixtures/cljc/existing-ops/posts.cljc")
          deps (analyze/intra-ns-deps zloc)
          by-name (deps-by-name deps)]
      ;; CLJ resolver depends on shared
      (is (contains? (:depends-on (by-name "resolve-posts")) "format-post"))
      (is (contains? (:depends-on (by-name "resolve-post-detail")) "format-post"))
      ;; CLJS transform depends on shared
      (is (contains? (:depends-on (by-name "client-format-post")) "format-post"))
      ;; post-summary depends on shared helpers
      (is (contains? (:depends-on (by-name "post-summary")) "normalize-title"))
      (is (contains? (:depends-on (by-name "post-summary")) "enrich-post")))))

(deftest test-deps-no-platform-annotation
  (testing "Design decision: dep edges have NO :platforms key. Keep it simple."
    (let [zloc (analyze/file->zloc "test-fixtures/cljc/existing-ops/io.cljc")
          deps (analyze/intra-ns-deps zloc)]
      (doseq [d deps]
        (is (not (contains? d :platforms))
            (str "dep entry for " (:name d) " should not have :platforms key"))))))

(deftest test-deps-io-independent-forms
  (testing "All forms in io.cljc are independent — no intra-ns dependencies."
    (let [zloc (analyze/file->zloc "test-fixtures/cljc/existing-ops/io.cljc")
          deps (analyze/intra-ns-deps zloc)]
      (doseq [d deps]
        (is (empty? (:depends-on d))
            (str (:name d) " should have no intra-ns deps"))))))

;; ============================================================
;; :topo — unified sort including platform-gated forms
;; ============================================================

(deftest test-topo-logging-unified-sort
  (testing "Design decision: ONE unified topo sort, not per-platform.
            truncate-ns before custom-output-fn before configure-logging!"
    (let [zloc (analyze/file->zloc "test-fixtures/cljc/existing-ops/logging.cljc")
          {:keys [sorted cycles]} (analyze/topological-sort zloc)]
      ;; All 5 forms present
      (is (= 5 (count sorted)))
      (is (contains? (set sorted) "configure-logging!"))
      (is (contains? (set sorted) "custom-output-fn"))
      (is (contains? (set sorted) "p"))
      ;; Ordering constraint: truncate-ns before custom-output-fn
      (is (< (.indexOf sorted "truncate-ns")
             (.indexOf sorted "custom-output-fn")))
      ;; Ordering constraint: custom-output-fn and format-level before configure-logging!
      (is (< (.indexOf sorted "custom-output-fn")
             (.indexOf sorted "configure-logging!")))
      (is (< (.indexOf sorted "format-level")
             (.indexOf sorted "configure-logging!")))
      ;; No cycles
      (is (empty? cycles)))))

(deftest test-topo-posts-cross-boundary
  (testing "Topo sort orders platform-gated forms after their shared dependencies."
    (let [zloc (analyze/file->zloc "test-fixtures/cljc/existing-ops/posts.cljc")
          {:keys [sorted cycles]} (analyze/topological-sort zloc)]
      (is (= 8 (count sorted)))  ;; 8 named defn/defn- (declare excluded from deps)
      ;; Shared ordering: add-slug before enrich-post before format-post
      (is (< (.indexOf sorted "add-slug") (.indexOf sorted "enrich-post")))
      (is (< (.indexOf sorted "enrich-post") (.indexOf sorted "format-post")))
      ;; CLJ resolvers after format-post
      (is (< (.indexOf sorted "format-post") (.indexOf sorted "resolve-posts")))
      ;; CLJS transforms after their deps
      (is (< (.indexOf sorted "format-post") (.indexOf sorted "client-format-post")))
      (is (empty? cycles)))))

(deftest test-topo-dashboard-order
  (testing "format-title before render-header before all render-* forms."
    (let [zloc (analyze/file->zloc "test-fixtures/cljc/existing-ops/dashboard.cljc")
          {:keys [sorted]} (analyze/topological-sort zloc)]
      (is (< (.indexOf sorted "format-title") (.indexOf sorted "render-header")))
      (is (< (.indexOf sorted "render-header") (.indexOf sorted "render-modal")))
      (is (< (.indexOf sorted "render-header") (.indexOf sorted "render-dashboard")))
      (is (< (.indexOf sorted "render-header") (.indexOf sorted "render-page"))))))

;; ============================================================
;; :ls-deps — transitive dep tree across platform boundaries
;; ============================================================

(deftest test-dep-tree-configure-logging
  (testing "Dep tree from configure-logging! traverses CLJ→CLJ→shared chain."
    (let [zloc (analyze/file->zloc "test-fixtures/cljc/existing-ops/logging.cljc")
          deps (analyze/intra-ns-deps zloc)
          tree (analyze/dep-tree deps "configure-logging!")]
      (is (some? tree))
      (is (= "configure-logging!" (:name tree)))
      (is (not (:leaf? tree)))
      ;; Has deps: custom-output-fn and format-level
      (is (= 2 (count (:deps tree))))
      ;; Transitive closure includes all 4 forms
      (is (= #{"configure-logging!" "custom-output-fn" "format-level" "truncate-ns"}
             (analyze/flatten-dep-tree tree))))))

(deftest test-dep-tree-resolve-posts
  (testing "Dep tree from CLJ resolver reaches all shared helpers."
    (let [zloc (analyze/file->zloc "test-fixtures/cljc/existing-ops/posts.cljc")
          deps (analyze/intra-ns-deps zloc)
          tree (analyze/dep-tree deps "resolve-posts")]
      (is (some? tree))
      (is (= #{"resolve-posts" "format-post" "normalize-title" "enrich-post" "add-slug"}
             (analyze/flatten-dep-tree tree))))))

;; ============================================================
;; :ls-extract — extraction closure across platform boundaries
;; ============================================================

(deftest test-closure-configure-logging
  (testing "Extracting configure-logging! pulls in its private helpers."
    (let [zloc (analyze/file->zloc "test-fixtures/cljc/existing-ops/logging.cljc")
          closure (analyze/extraction-closure zloc "configure-logging!")]
      (is (some? closure))
      (is (= "configure-logging!" (:target closure)))
      ;; Should include the target plus private helpers
      (let [form-names (set (map :name (:forms closure)))]
        (is (contains? form-names "configure-logging!"))
        (is (contains? form-names "custom-output-fn"))
        (is (contains? form-names "truncate-ns"))))))

(deftest test-closure-resolve-posts
  (testing "Extracting CLJ resolver — at minimum includes itself.
            The closure algorithm only pulls PRIVATE helpers that are exclusively
            used by the closure. format-post is public (used by others), so it
            stays outside the closure."
    (let [zloc (analyze/file->zloc "test-fixtures/cljc/existing-ops/posts.cljc")
          closure (analyze/extraction-closure zloc "resolve-posts")]
      (is (some? closure))
      (let [form-names (set (map :name (:forms closure)))]
        (is (contains? form-names "resolve-posts"))))))

;; ============================================================
;; :mv — design decision tests
;; ============================================================

(deftest test-mv-shared-form-works
  (testing "Moving a shared (top-level list) form in a .cljc file works."
    (with-temp-cljc-file (slurp "test-fixtures/cljc/existing-ops/io.cljc")
      (fn [path]
        ;; sanitize-filename is a shared top-level form — should be findable and movable
        ;; This is a smoke test that mv doesn't crash on .cljc files
        (let [result (move/move-form {:file path :form "sanitize-filename"
                                      :before "sanitize-filename" :dry-run true})]
          ;; dry-run on same position — the important thing is it FINDS the form
          (is (not (:error result))))))))

(deftest test-mv-platform-gated-form-returns-error
  (testing "Design decision: moving a platform-gated form returns an error because
            find-form can't see inside #?() blocks. This documents the current
            limitation — not a bug, a deferred feature."
    (with-temp-cljc-file (slurp "test-fixtures/cljc/existing-ops/io.cljc")
      (fn [path]
        (let [result (move/move-form {:file path :form "read-file"
                                      :before "sanitize-filename" :dry-run true})]
          (is (:error result))
          (is (str/includes? (:error result) "Form not found")))))))

(deftest test-mv-past-reader-conditional
  (testing "Moving a shared form past a #?() block works (line-based surgery)."
    (with-temp-cljc-file (slurp "test-fixtures/cljc/existing-ops/posts.cljc")
      (fn [path]
        ;; Move add-slug before normalize-title — both are shared top-level forms
        ;; with #?() blocks between them in the file
        (let [result (move/move-form {:file path :form "add-slug"
                                      :before "normalize-title" :dry-run true})]
          (is (:ok result))
          (is (= :up (-> result :plan :direction))))))))

;; ============================================================
;; :extract — shared forms extractable, callers scan includes .cljc
;; ============================================================

(defn- create-temp-cljc-project! []
  (let [root (java.io.File/createTempFile "extract-cljc-test" "")
        _ (.delete root)
        _ (.mkdirs root)
        src-dir (io/file root "src" "my")]
    (.mkdirs src-dir)
    (spit (io/file src-dir "app.cljc")
          (str "(ns my.app\n"
               "  (:require [clojure.string :as str]))\n\n"
               "(defn helper [x]\n  (str/upper-case x))\n\n"
               "(defn distill [x]\n  (helper x))\n\n"
               "(defn refine [x]\n  (distill (str x \"-refined\")))\n"))
    (spit (io/file src-dir "caller.cljc")
          (str "(ns my.caller\n"
               "  (:require [my.app :as app]))\n\n"
               "(defn do-thing []\n  (app/distill \"test\"))\n"))
    root))

(defn- delete-recursive! [f]
  (when (.isDirectory f)
    (doseq [child (.listFiles f)]
      (delete-recursive! child)))
  (.delete f))

(deftest test-extract-shared-forms-from-cljc
  (testing "Extracting shared (non-platform-gated) forms from a .cljc file works."
    (let [root (create-temp-cljc-project!)]
      (try
        (let [source-file (str (.getPath root) "/src/my/app.cljc")
              target-file (str (.getPath root) "/src/my/distillery.cljc")
              result (extract/plan {:file source-file
                                    :forms ['distill 'refine]
                                    :to target-file
                                    :source-paths ["src"]})]
          (is (not (:error result)))
          (is (= 2 (:form-count result)))
          (is (= "my.distillery" (:target-ns result))))
        (finally
          (delete-recursive! root))))))

(deftest test-extract-callers-scan-includes-cljc
  (testing "The callers-to-review scan includes .cljc files (Bug #2 fix)."
    (let [root (create-temp-cljc-project!)]
      (try
        (let [source-file (str (.getPath root) "/src/my/app.cljc")
              target-file (str (.getPath root) "/src/my/distillery.cljc")
              result (extract/plan {:file source-file
                                    :forms ['distill]
                                    :to target-file
                                    :source-paths ["src"]})]
          (is (some #(str/ends-with? % "caller.cljc")
                    (:callers-to-review result))
              ".cljc files should appear in callers-to-review"))
        (finally
          (delete-recursive! root))))))

;; ============================================================
;; :declares — works on .cljc with shared + gated consumers
;; ============================================================

(deftest test-declares-in-cljc
  (testing "Declare detection works on .cljc file. The (declare enrich-post)
            in posts.cljc is detected, and the defn enrich-post is visible."
    (with-temp-cljc-file (slurp "test-fixtures/cljc/existing-ops/posts.cljc")
      (fn [path]
        (let [result (outline/outline path)
              declares (->> (:forms result)
                            (filter #(= 'declare (:type %))))]
          ;; declare exists
          (is (= 1 (count declares)))
          (is (= 'enrich-post (:name (first declares))))
          ;; The defn enrich-post is also visible (not hidden in #?())
          (let [defns (->> (:forms result)
                           (filter #(and (= 'enrich-post (:name %))
                                         (not= 'declare (:type %)))))]
            (is (= 1 (count defns)))))))))

;; ============================================================
;; parse-ns-aliases — shared requires found; platform requires
;; need Bug #3 fix
;; ============================================================

(deftest test-parse-ns-aliases-shared-require
  (testing "Shared requires (direct vectors in :require) are found in .cljc."
    (let [zloc (analyze/file->zloc "test-fixtures/cljc/existing-ops/io.cljc")
          ;; Find the ns form
          ns-zloc (loop [z zloc]
                    (when z
                      (if (and (z/list? z)
                               (= "ns" (some-> z z/down z/string)))
                        z
                        (recur (z/right z)))))
          aliases (analyze/parse-ns-aliases ns-zloc)]
      (is (some? aliases))
      (is (= "clojure.string" (get aliases "str"))))))

(deftest test-parse-ns-aliases-platform-require
  (testing "Platform-specific requires inside #?() are found after Bug #3 fix.
            io.cljc has #?(:clj [clojure.java.io :as jio]) and
            #?(:cljs [goog.string :as gstr])."
    (let [zloc (analyze/file->zloc "test-fixtures/cljc/existing-ops/io.cljc")
          ns-zloc (loop [z zloc]
                    (when z
                      (if (and (z/list? z)
                               (= "ns" (some-> z z/down z/string)))
                        z
                        (recur (z/right z)))))
          aliases (analyze/parse-ns-aliases ns-zloc)]
      ;; These should be found after Bug #3 fix
      (is (= "clojure.java.io" (get aliases "jio"))
          "CLJ-gated require alias should be found")
      (is (= "goog.string" (get aliases "gstr"))
          "CLJS-gated require alias should be found"))))
