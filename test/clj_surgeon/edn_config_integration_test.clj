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
   5. No config — baseline behavior"
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [clojure.java.io :as io]
            [clj-surgeon.outline :as outline]
            [clj-surgeon.forms :as forms]
            [clojure.string :as str]))

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
