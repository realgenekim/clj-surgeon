(ns clj-surgeon.rename-alias-scope-test
  {:lane :fast}
  (:require
   [clj-surgeon.insert-forms-support :as h]
   [clj-surgeon.rename-alias :as sut]
   [clj-surgeon.rename-alias-test :as r]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]))

(def source (str r/header "events/x\n"))
(defn repository-run [selected skipped prepare]
  (h/with-file selected
    (fn [dir target _]
      (let [other (io/file dir "src/skipped.clj")
            sources (cond-> {r/file selected} skipped (assoc "src/skipped.clj" skipped))]
        (when skipped (spit other skipped))
        (when prepare (prepare dir))
        (let [req (assoc (r/request sources 1) :workspace_root (.getCanonicalPath dir)
                         :scope {:repository true :expect_files (count sources)})
              result (sut/execute! req)]
          (is (= "committed" (:state result)) (pr-str result))
          (is (= (-> selected (str/replace ":as events" ":as ev") (str/replace "events/" "ev/")) (slurp target)))
          (when skipped (is (= skipped (slurp other)))))))))

;; @spec RENAME-ALIAS-016
;; INTENT-TEST: RENAME-ALIAS-016
(deftest repository-ignores-non-clojure-symlinks
  (repository-run source nil
                  (fn [dir]
                    (let [real (io/file dir "README.txt") link (io/file dir "README-link.txt")]
                      (spit real "ordinary repository furniture")
                      (java.nio.file.Files/createSymbolicLink (.toPath link) (.toPath real)
                        (make-array java.nio.file.attribute.FileAttribute 0))))))

;; @spec RENAME-ALIAS-016
;; INTENT-TEST: RENAME-ALIAS-016
(deftest comment-ancestry-exempts-mutations-but-keeps-references
  (doseq [head ["comment" "clojure.core/comment"]]
    (repository-run (str r/header "(" head " (do (require '[y :as z]) (events/x)))\n")
                    "(ns skipped)\n(comment (require '[other :as q]))\n" nil)))

;; @spec RENAME-ALIAS-016
;; INTENT-TEST: RENAME-ALIAS-016
(deftest skipped-namespace-mutations-do-not-refuse
  (repository-run source "(ns skipped)\n(require '[other :as q])\n" nil))

;; @spec RENAME-ALIAS-016
;; INTENT-TEST: RENAME-ALIAS-016
(deftest skipped-duplicate-bindings-do-not-refuse
  (repository-run source "(ns skipped (:require [q.r :as u] [q.r :as v]))\n" nil))
