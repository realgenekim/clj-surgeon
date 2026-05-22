(ns clj-surgeon.ls-tree-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-surgeon.outline :as outline]
            [clj-surgeon.core :as core]
            [clojure.string :as str]
            [rewrite-clj.zip :as z]
            [babashka.fs :as fs]))

;; ============================================================
;; Pure tests: extract-ns-requires (zloc in, data out)
;; ============================================================

(defn- ns-zloc-from-string
  "Parse source and return the (ns ...) zipper location. No I/O."
  [source]
  (let [zloc (z/of-string source)]
    (some-> zloc (z/find-value z/next 'ns) z/up)))

(deftest test-extract-ns-requires-basic
  (let [ns-z (ns-zloc-from-string
              "(ns my.app
  (:require [clojure.string :as str]
            [clojure.set :as set]))
(defn foo [] :ok)")]
    (is (= ["[clojure.string :as str]"
            "[clojure.set :as set]"]
           (outline/extract-ns-requires ns-z)))))

(deftest test-extract-ns-requires-with-refer
  (let [ns-z (ns-zloc-from-string
              "(ns my.app
  (:require [clojure.test :refer [deftest is testing]]))
(defn foo [] :ok)")]
    (is (= ["[clojure.test :refer [deftest is testing]]"]
           (outline/extract-ns-requires ns-z)))))

(deftest test-extract-ns-requires-cljc-reader-conditional
  (let [ns-z (ns-zloc-from-string
              "(ns my.app
  (:require [clojure.string :as str]
            #?(:clj [clojure.java.io :as io])
            #?(:cljs [goog.string :as gstr])))
(defn foo [] :ok)")]
    (testing "extracts both direct and reader-conditional requires"
      (is (= ["[clojure.string :as str]"
              "[clojure.java.io :as io]"
              "[goog.string :as gstr]"]
             (outline/extract-ns-requires ns-z))))))

(deftest test-extract-ns-requires-cljc-splicing
  (let [ns-z (ns-zloc-from-string
              "(ns my.app
  (:require [clojure.string :as str]
            #?@(:clj [[clojure.java.io :as io]
                       [clojure.edn :as edn]])))
(defn foo [] :ok)")]
    (testing "extracts splicing reader-conditional requires"
      (is (= ["[clojure.string :as str]"
              "[clojure.java.io :as io]"
              "[clojure.edn :as edn]"]
             (outline/extract-ns-requires ns-z))))))

(deftest test-extract-ns-requires-no-require-block
  (let [ns-z (ns-zloc-from-string "(ns my.empty)")]
    (is (nil? (outline/extract-ns-requires ns-z)))))

(deftest test-extract-ns-requires-nil-input
  (is (nil? (outline/extract-ns-requires nil))))

(deftest test-extract-ns-requires-no-ns-form
  (let [ns-z (ns-zloc-from-string "(def x 1)")]
    (is (nil? (outline/extract-ns-requires ns-z)))))

;; ============================================================
;; Pure tests: source-paths-from-config (data in, data out)
;; ============================================================

(deftest test-source-paths-deps-edn
  (testing "reads :paths from deps.edn"
    (is (= ["src"] (core/source-paths-from-config "deps.edn" {:paths ["src"]})))
    (is (= ["src" "test"] (core/source-paths-from-config "deps.edn" {:paths ["src" "test"]}))))
  (testing "defaults to [\"src\"] when :paths missing"
    (is (= ["src"] (core/source-paths-from-config "deps.edn" {})))
    (is (= ["src"] (core/source-paths-from-config "deps.edn" {:deps {}})))))

(deftest test-source-paths-bb-edn
  (is (= ["src" "scripts"] (core/source-paths-from-config "bb.edn" {:paths ["src" "scripts"]})))
  (is (= ["src"] (core/source-paths-from-config "bb.edn" {}))))

(deftest test-source-paths-project-clj
  (is (= ["src/clj"]
         (core/source-paths-from-config "project.clj"
                                        '(defproject my-app "1.0" :source-paths ["src/clj"]))))
  (testing "defaults to [\"src\"] when :source-paths missing"
    (is (= ["src"]
           (core/source-paths-from-config "project.clj"
                                          '(defproject my-app "1.0" :dependencies []))))))

(deftest test-source-paths-unknown-file
  (is (= ["src"] (core/source-paths-from-config "build.gradle" {}))))

;; ============================================================
;; Pure tests: filter-projects-by-hits (data in, data out)
;; ============================================================

(deftest test-filter-by-hits-build-file-match
  (testing "when deps.edn matches, all source files are included"
    (let [projects [{:name "email-fetch"
                     :root "/r/email-fetch"
                     :files ["/r/email-fetch/src/a.clj"
                             "/r/email-fetch/src/b.clj"]}]
          hits #{"/r/email-fetch/deps.edn"}]
      (is (= projects (core/filter-projects-by-hits projects hits))))))

(deftest test-filter-by-hits-source-file-match
  (testing "when only source files match, only those are included"
    (let [projects [{:name "app"
                     :root "/r/app"
                     :files ["/r/app/src/email.clj"
                             "/r/app/src/util.clj"
                             "/r/app/src/core.clj"]}]
          hits #{"/r/app/src/email.clj"}
          result (core/filter-projects-by-hits projects hits)]
      (is (= 1 (count result)))
      (is (= ["/r/app/src/email.clj"] (:files (first result)))))))

(deftest test-filter-by-hits-no-match
  (testing "projects with no matches are excluded"
    (let [projects [{:name "unrelated"
                     :root "/r/unrelated"
                     :files ["/r/unrelated/src/core.clj"]}]
          hits #{"/r/other/src/foo.clj"}]
      (is (empty? (core/filter-projects-by-hits projects hits))))))

(deftest test-filter-by-hits-mixed
  (testing "mix of build-file match and source-file match across projects"
    (let [projects [{:name "email-lib"
                     :root "/r/email-lib"
                     :files ["/r/email-lib/src/a.clj" "/r/email-lib/src/b.clj"]}
                    {:name "app"
                     :root "/r/app"
                     :files ["/r/app/src/handler.clj" "/r/app/src/mailer.clj"]}
                    {:name "unrelated"
                     :root "/r/unrelated"
                     :files ["/r/unrelated/src/core.clj"]}]
          hits #{"/r/email-lib/deps.edn" "/r/app/src/mailer.clj"}
          result (core/filter-projects-by-hits projects hits)]
      (is (= 2 (count result)))
      ;; email-lib: build file matched → all files included
      (is (= ["/r/email-lib/src/a.clj" "/r/email-lib/src/b.clj"]
             (:files (first result))))
      ;; app: only source match → just mailer.clj
      (is (= ["/r/app/src/mailer.clj"]
             (:files (second result)))))))

;; ============================================================
;; Pure tests: format-file-text (data in, string out)
;; ============================================================

(deftest test-format-file-text-basic
  (let [result (core/format-file-text
                {:ns 'my.app
                 :lines 50
                 :form-count 2
                 :requires ["[clojure.string :as str]"]
                 :forms [{:type 'defn :name 'greet :args "[name]" :line 5 :end-line 10}
                         {:type 'def :name 'version :line 12 :end-line 12}]}
                "src/my/app.clj")]
    (is (str/includes? result "src/my/app.clj  50 lines, 2 forms"))
    (is (str/includes? result "ns: my.app"))
    (is (str/includes? result "requires: [clojure.string :as str]"))
    (is (str/includes? result "5-10: defn greet [name]"))
    (is (str/includes? result "12: def version"))))

(deftest test-format-file-text-no-requires
  (let [result (core/format-file-text
                {:ns 'my.bare :lines 5 :form-count 1
                 :requires []
                 :forms [{:type 'def :name 'x :line 3 :end-line 3}]}
                "bare.clj")]
    (is (not (str/includes? result "requires:")))
    (is (str/includes? result "3: def x"))))

(deftest test-format-file-text-error
  (let [result (core/format-file-text
                {:lines 0 :form-count 0 :error "Unexpected EOF"}
                "broken.clj")]
    (is (str/includes? result "⚠ Unexpected EOF"))))

(deftest test-format-file-text-single-line-form
  (testing "form where line == end-line shows just the line number, not a range"
    (let [result (core/format-file-text
                  {:ns 'x :lines 5 :form-count 1 :requires []
                   :forms [{:type 'def :name 'x :line 3 :end-line 3}]}
                  "x.clj")]
      (is (str/includes? result "3: def x"))
      (is (not (str/includes? result "3-3:"))))))

;; ============================================================
;; Pure tests: format-ls-tree-text (data in, string out)
;; ============================================================

(deftest test-format-ls-tree-text-single-project
  (let [projects [{:name "myapp"
                   :root "/tmp/myapp"
                   :outlines [["/tmp/myapp/src/core.clj"
                               {:ns 'myapp.core :lines 20 :form-count 2
                                :requires ["[clojure.string :as str]"]
                                :forms [{:type 'defn :name 'greet :args "[x]" :line 3 :end-line 8}
                                        {:type 'def :name 'version :line 10 :end-line 10}]}]]}]
        result (core/format-ls-tree-text projects "/tmp")]
    (is (str/includes? result "greet"))
    (is (str/includes? result "version"))
    (is (str/includes? result "total: 1 files, 2 forms"))))

(deftest test-format-ls-tree-text-multi-project-headers
  (let [projects [{:name "alpha"
                   :root "/r/alpha"
                   :outlines [["/r/alpha/src/a.clj"
                               {:ns 'alpha.core :lines 10 :form-count 1
                                :requires [] :forms [{:type 'defn :name 'a-fn :line 3 :end-line 5}]}]]}
                  {:name "beta"
                   :root "/r/beta"
                   :outlines [["/r/beta/src/b.clj"
                               {:ns 'beta.core :lines 15 :form-count 2
                                :requires [] :forms [{:type 'defn :name 'b-fn :line 3 :end-line 5}
                                                     {:type 'def :name 'b-val :line 7 :end-line 7}]}]]}]
        result (core/format-ls-tree-text projects "/r")]
    (is (str/includes? result "── alpha (1 files, 1 forms)"))
    (is (str/includes? result "── beta (1 files, 2 forms)"))
    (is (str/includes? result "total: 2 files, 3 forms"))))

;; ============================================================
;; Pure tests: format-ls-tree-edn (data in, data out)
;; ============================================================

(deftest test-format-ls-tree-edn
  (let [projects [{:name "proj"
                   :root "/tmp/proj"
                   :outlines [["/tmp/proj/src/core.clj"
                               {:ns 'proj.core :lines 20 :form-count 1
                                :requires ["[clojure.string :as str]"]
                                :forms [{:type 'defn :name 'f :args "[x]" :line 3 :end-line 5}]
                                :forward-refs []}]]}]
        result (core/format-ls-tree-edn projects "/tmp")]
    (is (vector? result))
    (is (= 1 (count result)))
    (let [entry (first result)]
      (is (= 'proj.core (:ns entry)))
      (is (str/includes? (:file entry) "core.clj"))
      (is (= 1 (:form-count entry)))
      (is (nil? (:forward-refs entry)) "forward-refs should be stripped"))))

;; ============================================================
;; Integration smoke tests (minimal I/O — verify end-to-end wiring)
;; ============================================================

(defn- create-temp-project!
  [parent-dir project-name deps-edn-content source-files]
  (let [project-dir (str parent-dir "/" project-name)
        src-dir (str project-dir "/src")]
    (fs/create-dirs src-dir)
    (spit (str project-dir "/deps.edn") deps-edn-content)
    (doseq [[rel-path content] source-files]
      (let [full-path (str src-dir "/" rel-path)]
        (fs/create-dirs (fs/parent full-path))
        (spit full-path content)))
    project-dir))

(deftest test-integration-single-project
  (let [tmp-dir (str (fs/create-temp-dir {:prefix "ls-tree-test"}))]
    (try
      (create-temp-project! tmp-dir "myapp"
                            "{:paths [\"src\"]}"
                            {"myapp/core.clj"
                             "(ns myapp.core
  (:require [clojure.string :as str]))

(defn greet [name]
  (str \"Hello \" name))

(def version \"1.0\")"})
      (let [result (core/run-ls-tree {:dir tmp-dir})]
        (is (string? result))
        (is (str/includes? result "greet"))
        (is (str/includes? result "[clojure.string :as str]"))
        (is (str/includes? result "total: 1 files, 2 forms")))
      (finally
        (fs/delete-tree tmp-dir)))))

(deftest test-integration-no-deps-edn-fallback
  (let [tmp-dir (str (fs/create-temp-dir {:prefix "ls-tree-test"}))]
    (try
      (spit (str tmp-dir "/loose.clj") "(ns loose)\n(defn loose-fn [] :loose)")
      (let [result (core/run-ls-tree {:dir tmp-dir})]
        (is (str/includes? result "loose-fn")))
      (finally
        (fs/delete-tree tmp-dir)))))

(deftest test-integration-no-deps-edn-grep-fallback
  (testing "grep fast-path finds loose .clj files without a build file"
    (let [tmp-dir (str (fs/create-temp-dir {:prefix "ls-tree-grep-loose"}))]
      (try
        (spit (str tmp-dir "/loose.clj")
              "(ns loose)\n(defn postgres-query [] :query)")
        (let [result (core/run-ls-tree {:dir tmp-dir :grep "postgres"})]
          (is (some? result) "loose file matching grep should be found")
          (is (str/includes? result "postgres-query")))
        (finally
          (fs/delete-tree tmp-dir))))))

(deftest test-integration-parse-error-non-fatal
  (let [tmp-dir (str (fs/create-temp-dir {:prefix "ls-tree-test"}))]
    (try
      (create-temp-project! tmp-dir "mixed"
                            "{:paths [\"src\"]}"
                            {"good.clj" "(ns good)\n(defn g [] :ok)"
                             "bad.clj" "(defn unclosed ["})
      (let [result (core/run-ls-tree {:dir tmp-dir})]
        (is (str/includes? result "g"))
        (is (or (str/includes? result "⚠")
                (str/includes? result "bad.clj"))))
      (finally
        (fs/delete-tree tmp-dir)))))
