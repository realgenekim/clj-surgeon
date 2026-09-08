(ns clj-surgeon.mission-git-submodule-test
  {:lane :battery}
  (:require
   [clj-surgeon.mission-git :as g]
   [clj-surgeon.mission-git-boundary-test :as fixture]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]))

(defn refuses-hidden-gitlink [configure]
  (fixture/with-repository
    (fn [root run provenance]
      (let [head (str/trim (run ["rev-parse" "HEAD"] nil))
            file (io/file root "src/a.clj")
            source (slurp file)]
        (run ["update-index" "--add" "--cacheinfo" (str "160000," head ",vendor/sub")] nil)
        (configure root run)
        (let [tree (str/trim (run ["write-tree"] nil))
              hidden (run ["diff" "--cached" "--no-ext-diff" "--name-only" "-z" "--"] nil)
              result (g/commit! provenance (constantly true))]
          (is (= "src/a.clj\u0000" hidden) "faithful Opus field condition: default diff hides the staged gitlink")
          (is (false? (:ok result)) (pr-str result))
          (is (= :git-staged-scope (:error-type result)))
          (is (false? (:git-ref-updated result)))
          (is (= head (str/trim (run ["rev-parse" "HEAD"] nil))))
          (is (= tree (str/trim (run ["write-tree"] nil))))
          (is (= source (slurp file))))))))

(deftest repo-ignore-submodules-cannot-hide-an-unverified-staged-entry
  (refuses-hidden-gitlink
    (fn [_ run] (run ["config" "diff.ignoreSubmodules" "all"] nil))))

(deftest working-gitmodules-cannot-hide-an-unverified-staged-entry
  (refuses-hidden-gitlink
    (fn [root run]
      (spit (io/file root ".gitmodules")
        "[submodule \"vendor/sub\"]\n\tpath = vendor/sub\n\turl = ./nowhere\n\tignore = all\n")
      (is (= "all" (str/trim (run ["config" "--file" ".gitmodules" "--get" "submodule.vendor/sub.ignore"] nil)))))))
