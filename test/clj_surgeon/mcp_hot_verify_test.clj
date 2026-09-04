(ns ^{:lane :integration} clj-surgeon.mcp-hot-verify-test
  (:require
   [clj-surgeon.mcp-hot-verify :as hot-verify]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]]
   [nrepl.server :as nrepl-server]))

(deftest passing-law
  (is (= 4 (+ 2 2))))

(deftest closed-profile-runs-focused-laws-in-the-configured-jvm
  (let [server (nrepl-server/start-server :bind "127.0.0.1" :port 0)
        project-root (System/getProperty "user.dir")
        port-file (str ".hot-verify-test-" (random-uuid) ".port")
        file (io/file project-root port-file)]
    (try
      (spit file (:port server))
      (let [result (hot-verify/verify!
                     project-root
                     {:port-file port-file
                      :reload []
                      :tests ["clj-surgeon.mcp-hot-verify-test/passing-law"]
                      :timeout-ms 5000})]
        (is (:ok result))
        (is (= :complete (:status result)))
        (is (= "application" (:jvm result)))
        (is (= 1 (:law-count result)))
        (is (= 1 (get-in result [:summary :test])))
        (is (zero? (get-in result [:summary :fail])))
        (is (pos-int? (:pid result))))
      (finally
        (io/delete-file file true)
        (nrepl-server/stop-server server)))))

(deftest hot-verification-refuses-invalid-profile-and-reports-law-failure
  (is (= :invalid-hot-verification-profile
         (:error-type (hot-verify/verify!
                        (System/getProperty "user.dir")
                        {:port-file "../escape" :reload [] :tests []}))))
  (let [fixture-ns (create-ns 'clj-surgeon.hot-failing-fixture)
        failing (intern fixture-ns 'fails nil)
        _ (alter-meta! failing assoc :test
                       (fn [] (is (= :expected :actual))))
        server (nrepl-server/start-server :bind "127.0.0.1" :port 0)
        project-root (System/getProperty "user.dir")
        port-file (str ".hot-verify-test-" (random-uuid) ".port")
        file (io/file project-root port-file)]
    (try
      (spit file (:port server))
      (let [result (hot-verify/verify!
                     project-root
                     {:port-file port-file
                      :reload []
                      :tests ["clj-surgeon.hot-failing-fixture/fails"]
                      :timeout-ms 5000})]
        (is (false? (:ok result)))
        (is (= :failed (:status result)))
        (is (= :hot-verification-failed (:error-type result)))
        (is (= 1 (get-in result [:summary :fail]))))
      (finally
        (io/delete-file file true)
        (nrepl-server/stop-server server)
        (remove-ns 'clj-surgeon.hot-failing-fixture)))))

(deftest profile-validation-is-closed-and-bounded
  (is (hot-verify/valid-profile?
        {:port-file ".nrepl-port"
         :reload ["app.core" "app.routes"]
         :tests ["app.core-test/render-law"]
         :timeout-ms 10000}))
  (doseq [profile
          [{:port-file ".nrepl-port" :reload [] :tests [] :code "(+ 1 2)"}
           {:port-file "/tmp/port" :reload [] :tests []}
           {:port-file ".nrepl-port" :reload ["bad name"] :tests []}
           {:port-file ".nrepl-port" :reload [] :tests ["missing-slash"]}
           {:port-file ".nrepl-port" :reload [] :tests [] :timeout-ms 1}]]
    (testing (pr-str profile)
      (is (false? (hot-verify/valid-profile? profile))))))
