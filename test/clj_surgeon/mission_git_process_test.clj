(ns clj-surgeon.mission-git-process-test
  {:lane :battery}
  (:require
   [clj-surgeon.mission-git :as g]
   [clj-surgeon.mission-git-boundary-test :as fixture]
   [clj-surgeon.mission-git-process :as process]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]))

(deftest nonreading-child-cannot-block-stdin-deadline
  (let [started (System/nanoTime)
        r (try (process/run-process! "/var/tmp/forge"
                             ["python3" "-c" "import time; time.sleep(30)"]
                             (apply str (repeat 2097152 "x")) 150)
               (catch Exception e (ex-data e)))
        elapsed (/ (- (System/nanoTime) started) 1e6)]
    (is (= :git-timeout (:error-type r)))
    (is (< elapsed 3000) (str "elapsed=" elapsed))))

(deftest identity-is-explicit-preflight
  (fixture/with-repository
    (fn [_root run p]
      (run ["config" "user.name" ""] nil)
      (run ["config" "user.email" ""] nil)
      (let [head (str/trim (run ["rev-parse" "HEAD"] nil))
            r (g/commit! p (constantly true))]
        (is (= :git-identity-unavailable (:error-type r)))
        (is (str/includes? (:decision r) "user.name and user.email"))
        (is (= head (str/trim (run ["rev-parse" "HEAD"] nil))))))))
