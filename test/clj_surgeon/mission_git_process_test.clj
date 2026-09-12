(ns clj-surgeon.mission-git-process-test
  {:lane :battery}
  (:require
   [clj-surgeon.mission-git-boundary-test :as fixture]
   [clj-surgeon.mission-git-identity-fixture :as identity]
   [clj-surgeon.mission-git-process :as process]
   [clj-surgeon.spawn-ledger :as spawn]
   [clj-surgeon.tmp-leak-support :refer [with-temp-dir]]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]))

(deftest nonreading-child-cannot-block-stdin-deadline
  (with-temp-dir [root "mission-process-"]
    (let [before (spawn/snapshot)
          started (System/nanoTime)
          r (try (process/run-process! (str root)
                   ["python3" "-c" "import time; time.sleep(30)"]
                   (apply str (repeat 2097152 "x")) 150)
                 (catch Exception e (ex-data e)))
          elapsed (/ (- (System/nanoTime) started) 1e6)
          launches (spawn/recorded-between before (spawn/snapshot))]
      (is (= :git-timeout (:error-type r)))
      (is (< elapsed 3000) (str "elapsed=" elapsed))
      (is (= 1 (count launches)) "one actual spawn, even when stdin times out")
      (is (= "python3 -c import time; time.sleep(30)" (:command (first launches))))
      (is (pos-int? (:pid (first launches)))))))

(deftest identity-is-explicit-preflight
  (fixture/with-repository
    (fn [_root run p]
      (run ["config" "user.name" ""] nil)
      (run ["config" "user.email" ""] nil)
      (let [head (str/trim (run ["rev-parse" "HEAD"] nil))
            r (identity/commit p {})]
        (is (= :git-identity-unavailable (:error-type r)))
        (is (str/includes? (:decision r) "user.name and user.email"))
        (is (= head (str/trim (run ["rev-parse" "HEAD"] nil))))))))
