(ns clj-surgeon.rename-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-surgeon.rename :as rename]
            [clojure.java.io :as io]
            [clojure.string :as str]))

;; ============================================================
;; Pure function tests (no I/O)
;; ============================================================

(deftest test-rename-source-ns-declaration
  (testing "renames ns declaration"
    (let [source "(ns ns-surgeon.core\n  (:require [clojure.string :as str]))\n\n(defn hello [] :hi)\n"
          result (rename/rename-source source "ns-surgeon" "clj-surgeon")]
      (is (str/includes? result "clj-surgeon.core"))
      (is (not (str/includes? result "ns-surgeon")))))

  (testing "renames nested namespace"
    (let [source "(ns ns-surgeon.views.distillery)\n"
          result (rename/rename-source source "ns-surgeon" "clj-surgeon")]
      (is (str/includes? result "clj-surgeon.views.distillery"))))

  (testing "doesn't rename unrelated namespaces"
    (let [source "(ns my-app.core\n  (:require [clojure.string :as str]))\n"
          result (rename/rename-source source "ns-surgeon" "clj-surgeon")]
      (is (= source result)))))

(deftest test-rename-source-requires
  (testing "renames require entries"
    (let [source "(ns my-app.core\n  (:require [ns-surgeon.outline :as outline]\n            [ns-surgeon.move :as move]\n            [clojure.string :as str]))\n"
          result (rename/rename-source source "ns-surgeon" "clj-surgeon")]
      (is (str/includes? result "clj-surgeon.outline"))
      (is (str/includes? result "clj-surgeon.move"))
      (is (str/includes? result "clojure.string"))  ;; untouched
      (is (str/includes? result ":as outline"))      ;; alias preserved
      (is (str/includes? result ":as move"))))       ;; alias preserved

  (testing "renames both ns and requires in same file"
    (let [source "(ns ns-surgeon.core\n  (:require [ns-surgeon.outline :as outline]))\n"
          result (rename/rename-source source "ns-surgeon" "clj-surgeon")]
      (is (str/includes? result "clj-surgeon.core"))
      (is (str/includes? result "clj-surgeon.outline")))))

(deftest test-rename-source-preserves-body
  (testing "function bodies unchanged"
    (let [source "(ns ns-surgeon.core)\n\n(defn hello [x]\n  (str \"ns-surgeon says: \" x))\n"
          result (rename/rename-source source "ns-surgeon" "clj-surgeon")]
      ;; ns renamed
      (is (str/includes? result "clj-surgeon.core"))
      ;; string literal preserved (not renamed!)
      (is (str/includes? result "\"ns-surgeon says: \"")))))

(deftest test-rename-source-no-ns-form
  (testing "files without ns form pass through unchanged"
    (let [source "(def x 1)\n(defn y [] x)\n"
          result (rename/rename-source source "ns-surgeon" "clj-surgeon")]
      (is (= source result)))))

(deftest test-analyze-file
  (testing "detects ns rename and require renames"
    (let [tmp (java.io.File/createTempFile "rename-test" ".clj")]
      (spit tmp "(ns ns-surgeon.core\n  (:require [ns-surgeon.outline :as outline]\n            [clojure.string :as str]))\n\n(defn hello [] :hi)\n")
      (try
        (let [result (rename/analyze-file (.getPath tmp) "ns-surgeon" "clj-surgeon")]
          (is (some? result))
          (is (= "ns-surgeon.core" (-> result :ns-rename :old)))
          (is (= "clj-surgeon.core" (-> result :ns-rename :new)))
          (is (= 1 (count (:require-renames result))))
          (is (= "ns-surgeon.outline" (-> result :require-renames first :old)))
          (is (= "clj-surgeon.outline" (-> result :require-renames first :new))))
        (finally (.delete tmp))))))

;; ============================================================
;; Integration test: full plan + execute on temp project
;; ============================================================

(defn- create-temp-project!
  "Create a minimal project in a temp dir for testing."
  []
  (let [root (java.io.File/createTempFile "ns-surgeon-project" "")
        _ (.delete root)
        _ (.mkdirs root)
        src-dir (io/file root "src" "ns_surgeon")
        test-dir (io/file root "test" "ns_surgeon")]
    (.mkdirs src-dir)
    (.mkdirs test-dir)
    ;; Write source files
    (spit (io/file src-dir "core.clj")
          "(ns ns-surgeon.core\n  (:require [ns-surgeon.outline :as outline]\n            [ns-surgeon.move :as move]\n            [clojure.pprint :as pp]))\n\n(defn run [opts]\n  (pp/pprint (outline/outline (:file opts))))\n")
    (spit (io/file src-dir "outline.clj")
          "(ns ns-surgeon.outline\n  (:require [clojure.string :as str]))\n\n(defn outline [file]\n  {:file file :forms []})\n")
    (spit (io/file src-dir "move.clj")
          "(ns ns-surgeon.move)\n\n(defn move-form [opts] {:ok true})\n")
    ;; Write test file
    (spit (io/file test-dir "outline_test.clj")
          "(ns ns-surgeon.outline-test\n  (:require [clojure.test :refer [deftest is]]\n            [ns-surgeon.outline :as outline]))\n\n(deftest test-outline\n  (is (= {:file \"x\" :forms []} (outline/outline \"x\"))))\n")
    ;; Write non-clj file
    (spit (io/file root "Makefile")
          "test:\n\tbb -m ns-surgeon.core\n\ninstall:\n\t@echo ns-surgeon installed\n")
    root))

(defn- delete-recursive! [f]
  (when (.isDirectory f)
    (doseq [child (.listFiles f)]
      (delete-recursive! child)))
  (.delete f))

(deftest test-plan-dry-run
  (let [root (create-temp-project!)]
    (try
      (let [result (rename/plan {:from "ns-surgeon" :to "clj-surgeon" :root (.getPath root)})]
        (testing "summary counts"
          (is (= 4 (-> result :summary :files-to-update)))  ;; core, outline, move, test
          (is (= 4 (-> result :summary :files-to-move)))    ;; same 4 files
          (is (pos? (-> result :summary :non-clj-to-review)))) ;; Makefile

        (testing "file analyses correct"
          (let [analyses (:file-analyses result)
                core-analysis (first (filter #(str/ends-with? (:file %) "core.clj") analyses))]
            (is (= "ns-surgeon.core" (-> core-analysis :ns-rename :old)))
            (is (= "clj-surgeon.core" (-> core-analysis :ns-rename :new)))
            (is (= 2 (count (:require-renames core-analysis)))))) ;; outline + move

        (testing "file moves computed"
          (let [moves (:file-moves result)]
            (is (every? #(str/includes? (:from %) "ns_surgeon") moves))
            (is (every? #(str/includes? (:to %) "clj_surgeon") moves))))

        (testing "non-clj files flagged"
          (is (some #(str/ends-with? % "Makefile") (:non-clj-files result)))))
      (finally (delete-recursive! root)))))

(deftest test-execute-full-rename
  (let [root (create-temp-project!)]
    (try
      (let [result (rename/execute! {:from "ns-surgeon" :to "clj-surgeon" :root (.getPath root)})]
        (testing "actions were taken"
          (is (pos? (-> result :summary :actions-taken))))

        (testing "new files exist"
          (is (.exists (io/file root "src" "clj_surgeon" "core.clj")))
          (is (.exists (io/file root "src" "clj_surgeon" "outline.clj")))
          (is (.exists (io/file root "src" "clj_surgeon" "move.clj")))
          (is (.exists (io/file root "test" "clj_surgeon" "outline_test.clj"))))

        (testing "source correctly renamed"
          (let [core-src (slurp (io/file root "src" "clj_surgeon" "core.clj"))]
            (is (str/includes? core-src "clj-surgeon.core"))
            (is (str/includes? core-src "clj-surgeon.outline"))
            (is (str/includes? core-src "clj-surgeon.move"))
            (is (not (str/includes? core-src "ns-surgeon")))))

        (testing "test file correctly renamed"
          (let [test-src (slurp (io/file root "test" "clj_surgeon" "outline_test.clj"))]
            (is (str/includes? test-src "clj-surgeon.outline-test"))
            (is (str/includes? test-src "clj-surgeon.outline"))
            (is (not (str/includes? test-src "ns-surgeon")))))

        (testing "aliases preserved"
          (let [core-src (slurp (io/file root "src" "clj_surgeon" "core.clj"))]
            (is (str/includes? core-src ":as outline"))
            (is (str/includes? core-src ":as move"))))

        (testing "non-clj files flagged for review"
          (is (pos? (-> result :summary :non-clj-to-review)))))
      (finally (delete-recursive! root)))))
