(ns round6.bb-witnesses
  (:require
   [babashka.fs :as fs]
   [clj-surgeon.lane-manifest-test :as lanes]
   [clj-surgeon.receipt-artifacts :as artifacts]
   [clojure.test :as t]))

(t/deftest bb-inode-accounting-and-unavailable-attributes
  (let [root (fs/create-temp-dir {:prefix "round6-bb-links-"})
        a (fs/path root "a") b (fs/path root "b")
        target (fs/path a "LOCK") peer (fs/path b "LOCK.broken.1")]
    (try
      (fs/create-dirs a)
      (fs/create-dirs b)
      (spit (str target) "owner")
      (java.nio.file.Files/createLink peer target)
      (binding [artifacts/*destination-envelope*
                (artifacts/destination-envelope [(str a) (str b)] :launcher)]
        (t/is (= (str target) (artifacts/admit-target! (str target)))))
      (binding [artifacts/*destination-envelope*
                (artifacts/destination-envelope [(str a)] :launcher)]
        (t/is (= :hard-link
                 (:reason (try (artifacts/admit-target! (str target)) nil
                               (catch clojure.lang.ExceptionInfo e (ex-data e)))))))
      (binding [artifacts/*destination-envelope*
                (artifacts/destination-envelope [(str root)] :launcher)]
        (let [stat (ns-resolve 'clj-surgeon.receipt-artifacts 'inode-attributes)]
          (t/is (some? stat))
          (when stat
            (with-redefs-fn {stat (fn [_] (throw (UnsupportedOperationException. "no unix view")))}
              #(let [r (try (artifacts/admit-target! (str target)) nil
                            (catch clojure.lang.ExceptionInfo e (ex-data e)))]
                 (t/is (= :write-outside-envelope (:error-type r)))
                 (t/is (= :unavailable (:link-evidence r))))))))
      (finally (fs/delete-tree root)))))

(binding [t/*report-counters* (ref t/*initial-report-counters*)]
  (t/test-vars [#'bb-inode-accounting-and-unavailable-attributes
                #'lanes/runtime-steering-fields-cannot-outvote-control-receipts
                #'lanes/runtime-receipts-must-stay-in-retained-evidence-roots])
  (let [r @t/*report-counters*]
    (t/do-report (assoc r :type :summary))
    (spit "docs/observations/2026-09-12-data-not-code/round6/bb-witnesses.edn" (pr-str r))
    (System/exit (if (zero? (+ (:fail r) (:error r))) 0 1))))
