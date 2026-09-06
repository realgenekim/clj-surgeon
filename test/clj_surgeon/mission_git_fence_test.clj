(ns clj-surgeon.mission-git-fence-test
  {:lane :battery}
  (:require
   [clj-surgeon.mission-git :as g]
   [clj-surgeon.mission-git-boundary-test :as fixture]
   [clj-surgeon.mission-git-test :as unit]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]])
  (:import
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute)))

(deftest path-and-reference-matrix
  (doseq [s [nil 1 "" "/absolute" "../parent" "a/../b" ".git/config" "a//b" "a/"
             "a\\b" "a:b" "a\nInjected: trailer" "a\u0000b"]]
    (is (not (g/path? s))))
  (doseq [s [nil "HEAD" "refs/heads/main" "refs/heads/MCP/main" "refs/heads/a..b"
             "refs/heads/a.lock" "refs/heads/.secret" "refs/heads/a/" "refs/heads/a\nB"]]
    (is (not (g/branch? s))))
  (is (g/path? "src/foo-bar.clj"))
  (is (g/branch? "refs/heads/astra/mission-git")))

(deftest opaque-metadata-is-never-provenance
  (let [p (assoc unit/provenance :secret "SENTINEL" :intent "SENTINEL")]
    (is (:ok (g/plan p unit/observed)))
    (is (not (str/includes? (:message (g/plan p unit/observed)) "SENTINEL")))))

(deftest symlink-and-live-drift-refuse
  (fixture/with-repository
    (fn [root run p]
      (let [source (io/file root "src/a.clj")
            head (str/trim (run ["rev-parse" "HEAD"] nil))
            saved (slurp source)
            target (io/file root "target.clj")]
        (spit source (str saved "\n(def stranger 1)"))
        (is (= :git-stale-or-unsupported-files (:error-type (g/commit! p (constantly true)))))
        (spit target saved)
        (Files/delete (.toPath source))
        (Files/createSymbolicLink (.toPath source) (.toPath target) (make-array FileAttribute 0))
        (is (= :git-symlink (:error-type (g/commit! p (constantly true)))))
        (is (= head (str/trim (run ["rev-parse" "HEAD"] nil))))))))

(deftest fixture-freeze-and-hook-skip
  (fixture/with-repository
    (fn [root run p]
      (run ["branch" "-m" "main"] nil)
      (is (= :git-unsupported-head (:error-type (g/commit! p (constantly true)))))
      (run ["branch" "-m" "fixture"] nil)
      (let [hook (io/file root ".git/hooks/pre-commit")]
        (spit hook "#!/bin/sh\nexit 77\n")
        (.setExecutable hook true)
        (let [r (g/commit! p (constantly true))]
          (is (:ok r))
          (is (false? (:hooks-run r)))
          (is (= (slurp (io/file root "src/a.clj")) (run ["show" "HEAD:src/a.clj"] nil))))))))

(deftest argv-output-cap-refuses
  (fixture/with-repository
    (fn [root run _p]
      (spit (io/file root "big.txt") (apply str (repeat (+ g/max-bytes 100) "a")))
      (run ["add" "--" "big.txt"] nil)
      (is (thrown? Exception (run ["show" ":big.txt"] nil))))))
