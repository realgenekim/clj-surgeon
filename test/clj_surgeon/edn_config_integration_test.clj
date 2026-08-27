(ns clj-surgeon.edn-config-integration-test
  "End-to-end integration tests for .clj-surgeon.edn support.

   Creates synthetic codebases in temp directories with various
   .clj-surgeon.edn configurations, then runs the actual outline
   function against them to verify that custom macros are recognized
   and fields are extracted correctly.

   Tests are organized by scenario:
   1. Metabase-style macros (defendpoint, defenterprise, defsetting)
   2. Simple kind-only config (no field extractors)
   3. SCI fn-based extractors
   4. Multiple repos with different configs (closest-wins)
   5. No config — baseline behavior
   6. Subprocess CLI tests — shells out to a fresh `bb` process

   WHY TWO KINDS OF INTEGRATION TESTS?

   Scenarios 1-5 call outline/outline directly from within the test
   suite. This tests the Clojure API but runs inside babashka's own
   SCI, so when .clj-surgeon.edn contains (fn [z] ...) forms, our
   sci/eval-form creates nested SCI (see forms.clj comment block for
   the full story). We fixed the nesting issue with bare symbol
   namespace keys, but it was fragile and surprising.

   Scenario 6 shells out to `bb -m clj-surgeon.core` as a separate
   process — no nesting, just a single babashka running clj-surgeon
   the same way a user would. If the in-process tests ever break due
   to a babashka SCI update, the subprocess tests will still pass
   (and tell us the tool actually works — the nesting is our test
   infrastructure's problem, not the user's)."
  (:require
   [babashka.process :as proc]
   [clj-surgeon.forms :as forms]
   [clj-surgeon.outline :as outline]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing use-fixtures]]))

;; ============================================================
;; Helpers
;; ============================================================

(defn- mk-tmp-dir [name]
  (let [d (java.nio.file.Files/createTempDirectory
            name (into-array java.nio.file.attribute.FileAttribute []))]
    (.toFile d)))

(defn- spit-at [dir & path-content-pairs]
  (doseq [[rel-path content] (partition 2 path-content-pairs)]
    (let [f (io/file dir rel-path)]
      (io/make-parents f)
      (spit f content)
      f)))

(defn- rm-rf [^java.io.File f]
  (when (.isDirectory f)
    (doseq [child (.listFiles f)]
      (rm-rf child)))
  (.delete f))

;; Reset project-aliases after each test to avoid cross-contamination
(use-fixtures :each
  (fn [test-fn]
    (try (test-fn)
         (finally (reset! forms/project-aliases {})))))

;; ============================================================
;; Scenario 1: Metabase-style macros with field extractors
;; ============================================================

(def metabase-config
  "{:aliases
 {\"defsetting\"
  {:fields {:name ->defn-name}}

  \"defenterprise\"
  {:fields {:name         ->defn-name
            :arglist      ->defn-arg-list}}

  \"defendpoint\"
  {:fields {:route (fn [z]
                     [(-> z z/down z/right z/sexpr)
                      (-> z z/down z/right z/right z/sexpr)])}}}}")

(def metabase-source
  "(ns metabase.api.geojson
  (:require [api.macros]))

(defsetting application-name
  (deferred-tru \"Replace the word.\")
  :encryption :no
  :visibility :public
  :default    \"Metabase\")

(defenterprise enable-custom-viz?
  \"Should we enable custom visualizations?\"
  metabase-enterprise.custom-viz-plugin.settings
  []
  false)

(api.macros/defendpoint :get \"/:key\"
  \"Fetch a custom GeoJSON file.\"
  [{k :key} :- [:map [:key ms/NonBlankString]]]
  (println \"endpoint body\"))

(defn regular-fn [x]
  (inc x))")

(deftest test-metabase-macros-end-to-end
  (let [dir (mk-tmp-dir "mb-e2e-")]
    (try
      (spit-at dir
               ".clj-surgeon.edn" metabase-config
               "src/metabase/api/geojson.clj" metabase-source)
      (let [src-path (str dir "/src/metabase/api/geojson.clj")]
        (forms/init-from-file! src-path)
        (let [result (outline/outline src-path)
              by-name (into {} (map (juxt :name identity)) (:forms result))
              by-type (group-by :type (:forms result))]

          (testing "defsetting: name extracted"
            (let [form (by-name 'application-name)]
              (is (some? form) "defsetting should be a named form")
              (is (= 'defsetting (:type form)))))

          (testing "defenterprise: name + arglist extracted"
            (let [form (by-name 'enable-custom-viz?)]
              (is (some? form) "defenterprise should be a named form")
              (is (= 'defenterprise (:type form)))
              (is (= "[]" (:args form)) "arglist should be extracted")))

          (testing "defendpoint: route extracted via SCI fn"
            (let [form (first (get by-type 'api.macros/defendpoint))]
              (is (some? form) "defendpoint should appear in outline")
              (is (= [:get "/:key"] (:route form))
                  "SCI fn should extract [method url] tuple")))

          (testing "regular defn still works alongside custom macros"
            (let [form (by-name 'regular-fn)]
              (is (some? form))
              (is (= "[x]" (:args form)))))

          (testing "form-count includes named forms (defendpoint has no :name)"
            (is (= 3 (:form-count result))
                "defsetting + defenterprise + defn = 3; defendpoint has :route not :name"))))
      (finally (rm-rf dir)))))

;; ============================================================
;; Scenario 2: Simple kind-only config (no field extractors)
;; ============================================================

(def simple-config
  "{:aliases {\"defendpoint\"   :defn
            \"defenterprise\" :defn
            \"defsetting\"    :def}}")

(def simple-source
  "(ns myapp.routes)

(defendpoint handle-root [request]
  {:status 200 :body \"ok\"})

(defsetting max-retries 3)

(defn helper [x] (inc x))")

(deftest test-simple-kind-only-config
  (let [dir (mk-tmp-dir "simple-e2e-")]
    (try
      (spit-at dir
               ".clj-surgeon.edn" simple-config
               "src/myapp/routes.clj" simple-source)
      (let [src-path (str dir "/src/myapp/routes.clj")]
        (forms/init-from-file! src-path)
        (let [result (outline/outline src-path)
              by-name (into {} (map (juxt :name identity)) (:forms result))]

          (testing "defendpoint classified as :defn — has name + arglist"
            (let [form (by-name 'handle-root)]
              (is (some? form))
              (is (= 'defendpoint (:type form)))
              (is (= "[request]" (:args form))
                  "defendpoint mapped to :defn gets legacy arglist extraction")))

          (testing "defsetting classified as :def — has name, no arglist"
            (let [form (by-name 'max-retries)]
              (is (some? form))
              (is (= 'defsetting (:type form)))
              (is (nil? (:args form))
                  "defsetting mapped to :def should not extract arglists")))

          (testing "regular defn still works"
            (is (some? (by-name 'helper))))))
      (finally (rm-rf dir)))))

;; ============================================================
;; Scenario 3: Multiple repos — closest config wins
;; ============================================================

(deftest test-multi-repo-closest-config-wins
  (let [dir (mk-tmp-dir "multi-repo-")]
    (try
      ;; Repo A: in dir/repo-a/, has its own .clj-surgeon.edn
      (spit-at dir
               "repo-a/.clj-surgeon.edn"
               "{:aliases {\"defwidget\" :defn}}"
               "repo-a/src/widgets.clj"
               "(ns widgets)\n(defwidget button [props] [:button props])")

      ;; Repo B: in dir/repo-b/, has different .clj-surgeon.edn
      (spit-at dir
               "repo-b/.clj-surgeon.edn"
               "{:aliases {\"defhandler\" :defn}}"
               "repo-b/src/handlers.clj"
               "(ns handlers)\n(defhandler on-click [event] (println event))")

      (testing "repo-a picks up defwidget"
        (let [path-a (str dir "/repo-a/src/widgets.clj")]
          (forms/init-from-file! path-a)
          (let [result (outline/outline path-a)]
            (is (= 'button (:name (first (:forms result)))))
            (is (= "[props]" (:args (first (:forms result))))))))

      (testing "repo-b picks up defhandler, not defwidget"
        (let [path-b (str dir "/repo-b/src/handlers.clj")]
          (forms/init-from-file! path-b)
          (let [result (outline/outline path-b)]
            (is (= 'on-click (:name (first (:forms result)))))
            (is (= "[event]" (:args (first (:forms result)))))
            ;; defwidget should NOT be classified here
            (is (nil? (forms/classify "defwidget"))
                "repo-b config doesn't know about defwidget"))))

      (finally (rm-rf dir)))))

;; ============================================================
;; Scenario 4: No config — baseline behavior
;; ============================================================

(deftest test-no-config-baseline
  (let [dir (mk-tmp-dir "no-config-")]
    (try
      ;; No .clj-surgeon.edn anywhere
      (spit-at dir
               "src/app.clj"
               "(ns app)\n(defendpoint handle [req] req)\n(defn greet [x] (str \"hi \" x))")
      (let [src-path (str dir "/src/app.clj")]
        (forms/init-from-file! src-path)
        (let [result (outline/outline src-path)
              by-name (into {} (map (juxt :name identity)) (:forms result))]

          (testing "defendpoint without config is not a defining form"
            (is (nil? (by-name 'handle))
                "without config, defendpoint is not recognized"))

          (testing "regular defn still works"
            (let [form (by-name 'greet)]
              (is (some? form))
              (is (= "[x]" (:args form)))))))
      (finally (rm-rf dir)))))

;; ============================================================
;; Scenario 5: SCI fn extractors with rewrite-clj
;; ============================================================

(def sci-fn-config
  "{:aliases
 {\"defroute\"
  {:fields {:name    ->defn-name
            :method  ->first-keyword
            :path    ->first-string
            :arglist ->defn-arg-list}}}}")

(def sci-fn-source
  "(ns myapp.api)

(defroute list-users :get \"/users\"
  [request]
  (db/list-users))

(defroute create-user :post \"/users\"
  [request]
  (db/create-user (:body request)))")

(deftest test-sci-stdlib-extractors
  (let [dir (mk-tmp-dir "sci-stdlib-")]
    (try
      (spit-at dir
               ".clj-surgeon.edn" sci-fn-config
               "src/myapp/api.clj" sci-fn-source)
      (let [src-path (str dir "/src/myapp/api.clj")]
        (forms/init-from-file! src-path)
        (let [result (outline/outline src-path)
              forms (:forms result)
              first-form (first forms)]

          (testing "name extracted"
            (is (= 'list-users (:name first-form))))

          (testing "method keyword extracted"
            (is (= :get (:method first-form))))

          (testing "path string extracted"
            (is (= "/users" (:path first-form))))

          (testing "arglist extracted"
            (is (= "[request]" (:args first-form))))

          (testing "second form also works"
            (let [second-form (second forms)]
              (is (= 'create-user (:name second-form)))
              (is (= :post (:method second-form)))
              (is (= "/users" (:path second-form)))))))
      (finally (rm-rf dir)))))

;; ============================================================
;; Scenario 6: SCI fn returning a number (defmigration :version)
;; ============================================================

(def migration-config
  "{:aliases
 {\"defmigration\"
  {:fields {:name ->defn-name
            :version (fn [z] (-> z z/down z/right z/right z/sexpr))}}}}")

(def migration-source
  "(ns app.migrations)

(defmigration add-users-table 1
  (create-table :users [:id :serial]))

(defmigration add-posts-table 2
  (create-table :posts [:id :serial]))

(defmigration add-index-on-email 3
  (create-index :users :email))")

(deftest test-sci-fn-extracts-number
  (let [dir (mk-tmp-dir "migration-")]
    (try
      (spit-at dir
               ".clj-surgeon.edn" migration-config
               "src/migrations.clj" migration-source)
      (let [src-path (str dir "/src/migrations.clj")]
        (forms/init-from-file! src-path)
        (let [result (outline/outline src-path)
              forms (:forms result)]
          (testing "all 3 migrations found"
            (is (= 3 (count forms))))
          (testing "version numbers extracted as integers"
            (is (= 1 (:version (first forms))))
            (is (= 2 (:version (second forms))))
            (is (= 3 (:version (nth forms 2)))))
          (testing "names extracted"
            (is (= '[add-users-table add-posts-table add-index-on-email]
                   (mapv :name forms))))))
      (finally (rm-rf dir)))))

;; ============================================================
;; Scenario 7: Kind-only mapping gets legacy arglist extraction
;; ============================================================

(def kind-only-config
  "{:aliases {\"defcommand\" :defn}}")

(def kind-only-source
  "(ns app.commands)

(defcommand seed-db [opts]
  (println \"Seeding...\"))

(defcommand run-migrations [opts]
  (println \"Running...\"))

(defn helper [x] x)")

(deftest test-kind-only-gets-legacy-extraction
  (let [dir (mk-tmp-dir "kind-only-")]
    (try
      (spit-at dir
               ".clj-surgeon.edn" kind-only-config
               "src/commands.clj" kind-only-source)
      (let [src-path (str dir "/src/commands.clj")]
        (forms/init-from-file! src-path)
        (let [result (outline/outline src-path)
              by-name (into {} (map (juxt :name identity)) (:forms result))]
          (testing "defcommand mapped to :defn gets name + arglist"
            (let [form (by-name 'seed-db)]
              (is (some? form))
              (is (= "[opts]" (:args form)))))
          (testing "regular defn coexists"
            (is (some? (by-name 'helper))))))
      (finally (rm-rf dir)))))

;; ============================================================
;; Scenario 8: Auto-detection (tier 4) coexists with config (tier 3)
;; ============================================================

(def coexist-config
  "{:aliases {\"defsetting\" {:fields {:name ->defn-name}}}}")

(def coexist-source
  "(ns app.mixed
  (:require [malli.util :as mu]
            [guardrails.core :refer [>defn]]))

(defsetting app-name
  (deferred-tru \"The name\")
  :default \"MyApp\")

(mu/defn schema-fn :- :string
  [x :- :int]
  (str x))

(>defn validated [x]
  [int? => string?]
  (str x))

(defn plain [x] (inc x))")

(deftest test-auto-detection-coexists-with-config
  (let [dir (mk-tmp-dir "coexist-")]
    (try
      (spit-at dir
               ".clj-surgeon.edn" coexist-config
               "src/mixed.clj" coexist-source)
      (let [src-path (str dir "/src/mixed.clj")]
        (forms/init-from-file! src-path)
        (let [result (outline/outline src-path)
              by-name (into {} (map (juxt :name identity)) (:forms result))]
          (testing "tier 3: defsetting from config"
            (is (some? (by-name 'app-name))))
          (testing "tier 4: mu/defn auto-detected despite config loaded"
            (let [form (by-name 'schema-fn)]
              (is (some? form))
              (is (= 'mu/defn (:type form)))))
          (testing "tier 2: >defn explicit alias still works"
            (let [form (by-name 'validated)]
              (is (some? form))
              (is (= '>defn (:type form)))))
          (testing "tier 1: regular defn always works"
            (is (some? (by-name 'plain))))
          (testing "all 4 named forms counted"
            (is (= 4 (:form-count result))))))
      (finally (rm-rf dir)))))

;; ============================================================
;; Scenario 9: Subprocess CLI test — what the user actually sees
;;
;; Shells out to `bb -m clj-surgeon.core` as a separate process.
;; No nested SCI — tests the real CLI end-to-end.
;; ============================================================

(def ^:private project-src
  "Absolute path to this project's src/ dir, for subprocess classpath."
  (let [this-file (io/file "test/clj_surgeon/edn_config_integration_test.clj")]
    (str (.getAbsolutePath (io/file (.getParentFile (.getParentFile (.getParentFile this-file))) "src")))))

(def cli-config
  "{:aliases
 {\"defsetting\"
  {:fields {:name ->defn-name}}

  \"defendpoint\"
  {:fields {:route (fn [z]
                     [(-> z z/down z/right z/sexpr)
                      (-> z z/down z/right z/right z/sexpr)])}}}}")

(def cli-source
  "(defsetting app-name
  (deferred-tru \"The name\")
  :default \"MyApp\")

(defendpoint :get \"/users\"
  \"List all users\"
  [request]
  (db/list-users))

(defn helper [x]
  (str \"hello \" x))")

(deftest test-cli-subprocess-with-edn-config
  (let [dir (mk-tmp-dir "cli-e2e-")]
    (try
      (spit-at dir
               ".clj-surgeon.edn" cli-config
               "src/myapp/api.clj" cli-source)
      (let [src-path (str dir "/src/myapp/api.clj")
            result (proc/shell {:out :string :err :string :dir (str dir)}
                               "bb" "-cp" project-src "-m" "clj-surgeon.core"
                               ":op" ":ls" ":file" src-path)
            output (:out result)]

        (testing "CLI exits successfully"
          (is (zero? (:exit result))
              (str "CLI failed: " (:err result))))

        (testing "defsetting name extracted in CLI output"
          (is (str/includes? output "app-name")
              "defsetting should show :name app-name"))

        (testing "defendpoint route extracted via SCI fn in CLI output"
          (is (str/includes? output ":get")
              "defendpoint should show :get in route")
          (is (str/includes? output "\"/users\"")
              "defendpoint should show \"/users\" in route"))

        (testing "regular defn still appears with arglist"
          (is (str/includes? output "helper"))
          (is (str/includes? output "[x]")))

        (testing "the CLI config is loaded from the subprocess CWD"
          (is (str/includes? output "defsetting"))))
      (finally (rm-rf dir)))))

(deftest test-cli-subprocess-without-config
  (let [dir (mk-tmp-dir "cli-noconfig-")]
    (try
      ;; No .clj-surgeon.edn — defendpoint should NOT be recognized
      (spit-at dir
               "src/app.clj"
               "(defendpoint handle [req] req)\n(defn greet [x] (str \"hi \" x))")
      (let [src-path (str dir "/src/app.clj")
            result (proc/shell {:out :string :err :string :dir (str dir)}
                               "bb" "-cp" project-src "-m" "clj-surgeon.core"
                               ":op" ":ls" ":file" src-path)
            output (:out result)]

        (testing "CLI exits successfully"
          (is (zero? (:exit result))))

        (testing "defn greet is recognized"
          (is (str/includes? output "greet"))
          (is (str/includes? output "[x]")))

        (testing "defendpoint without config has no :name"
          (is (not (str/includes? output "handle"))
              "without config, defendpoint name should not be extracted")))
      (finally (rm-rf dir)))))
