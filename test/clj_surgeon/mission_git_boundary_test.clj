(ns clj-surgeon.mission-git-boundary-test
  (:require
   [clj-surgeon.mission-git :as g]
   [clj-surgeon.mission-git-test :as unit]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]])
  (:import
   (java.nio.file FileVisitOption Files Paths)
   (java.nio.file.attribute FileAttribute)))

(defn with-repository [f]
  (let [root (str (Files/createTempDirectory (Paths/get "/var/tmp/forge" (make-array String 0))
                    "mission-git-" (make-array FileAttribute 0)))
        run (partial g/run-git! root)
        ;; Representative source shape from the mission-forms fixture: preserve
        ;; the protected leading comment and unrelated adjacent definition.
        before "; protected\n(defn field [x] (get x :value))\n(def untouched 3)\n"
        after "; protected\n(defn finding-field [x] (get x :value))\n(def untouched 3)\n"
        file (io/file root "src/a.clj")]
    (try
      (run ["init" "-q" "-b" "fixture"] nil)
      (run ["config" "user.name" "Mission fixture"] nil)
      (run ["config" "user.email" "mission-fixture@example.invalid"] nil)
      (.mkdirs (.getParentFile file))
      (spit file before)
      (run ["add" "--" "src/a.clj"] nil)
      (run ["commit" "-qm" "fixture preimage"] nil)
      (spit file after)
      (run ["add" "--" "src/a.clj"] nil)
      (f root run (assoc unit/provenance :workspace-root root
                         :files {"src/a.clj" {:before-sha256 (g/digest (.getBytes before "UTF-8"))
                                              :after-sha256 (g/digest (.getBytes after "UTF-8"))}}))
      (finally
        (with-open [paths (Files/walk (.toPath (io/file root)) (make-array FileVisitOption 0))]
          (doseq [path (sort-by #(.getNameCount %) > (iterator-seq (.iterator paths)))]
            (Files/delete path)))))))

(deftest actual-explicit-stage-commit
  (with-repository
    (fn [root run p]
      (let [source-before (slurp (io/file root "src/a.clj"))
            parent (str/trim (run ["rev-parse" "HEAD"] nil))
            r (g/commit! p (constantly true))]
        (is (:ok r) (pr-str r))
        (is (= (:commit r) (str/trim (run ["rev-parse" "HEAD"] nil))))
        (is (= parent (:parent r)))
        (is (= source-before (slurp (io/file root "src/a.clj"))))
        (is (= "src/a.clj\n" (run ["diff-tree" "--no-commit-id" "--name-only" "-r" "HEAD"] nil)))
        (is (str/includes? (run ["show" "-s" "--format=%B" "HEAD"] nil) "Receipt-SHA256:"))
        (is (= "" (run ["diff" "--cached" "--name-only"] nil)))
        (is (false? (:source-mutation-attempted r)))
        (is (false? (:hooks-run r)))))))

(deftest staged-stranger-and-stale-ledger-refuse
  (with-repository
    (fn [root run p]
      (let [head (str/trim (run ["rev-parse" "HEAD"] nil))]
        (is (= :git-stale-ledger (:error-type (g/commit! p (constantly false)))))
        (spit (io/file root "stranger.txt") "keep me")
        (run ["add" "--" "stranger.txt"] nil)
        (is (= :git-staged-scope (:error-type (g/commit! p (constantly true)))))
        (is (= head (str/trim (run ["rev-parse" "HEAD"] nil))))))))

(deftest pure-command-failures-are-typed
  (doseq [phase ["commit-tree" "update-ref"]]
    (let [r (g/execute! unit/provenance (constantly unit/observed)
                        (fn [argv _]
                          (if (= phase (first argv))
                            (throw (ex-info "private process output" {}))
                            unit/oid)))]
      (is (= (if (= phase "update-ref") :git-ref-update-uncertain :git-boundary-failed) (:error-type r)))
      (is (= (if (= phase "update-ref") :unknown false) (:git-ref-updated r)))
      (is (not (str/includes? (pr-str r) "private process output"))))))

(deftest creation-cannot-replace-verified-preimage
  (with-repository
    (fn [root _run p]
      (Files/delete (.toPath (io/file root "src/a.clj")))
      (is (= :git-unsupported-file (:error-type (g/commit! p (constantly true))))))))
