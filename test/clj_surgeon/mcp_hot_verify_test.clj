(ns ^{:lane :integration} clj-surgeon.mcp-hot-verify-test
  (:require
   [clj-surgeon.mcp-hot-verify :as hot-verify]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [nrepl.server :as nrepl-server]
   [nrepl.transport :as transport]))

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

;; --- Hot verification terminates on a terminal status, not on its ceiling ---
;; Requirements: docs/intent/hot-verification/hot-verification-specs.md

(defn- with-stub-nrepl
  "Run `body-fn` against an in-process nREPL server whose handler is `handler`,
   with the port published in a project-root port file. Returns body-fn's value."
  [handler body-fn]
  (let [server (nrepl-server/start-server :bind "127.0.0.1" :port 0
                                          :handler handler)
        project-root (System/getProperty "user.dir")
        port-file (str ".hot-verify-test-" (random-uuid) ".port")
        file (io/file project-root port-file)]
    (try
      (spit file (:port server))
      (body-fn project-root port-file)
      (finally
        (io/delete-file file true)
        (nrepl-server/stop-server server)))))

(defn- stub-profile
  [port-file timeout-ms]
  {:port-file port-file :reload [] :tests [] :timeout-ms timeout-ms})

;; @spec MCP-OP-HOTVER-001
(deftest hot-verification-returns-when-done-arrives-not-when-the-ceiling-expires
  (with-stub-nrepl
    (fn [{:keys [transport id]}]
      (transport/send
        transport
        {:id id
         :status ["done"]
         :value (pr-str {:cwd (System/getProperty "user.dir")
                         :pid 4242
                         :summary {:test 1 :pass 1 :fail 0 :error 0}})}))
    (fn [project-root port-file]
      (let [started (System/nanoTime)
            result (hot-verify/verify! project-root
                                       (stub-profile port-file 60000))
            elapsed (/ (double (- (System/nanoTime) started)) 1000000.0)]
        (is (< elapsed 2000.0)
            (str "hot verification must end at \"done\", not at its ceiling; took "
                 elapsed "ms"))
        (is (:ok result))
        (is (= :complete (:status result)))
        (is (= "application" (:jvm result)))
        (is (= 4242 (:pid result)))
        (is (= 0 (:reload-count result)))
        (is (= 0 (:law-count result)))
        (is (= {:test 1 :pass 1 :fail 0 :error 0} (:summary result)))))))

;; @spec MCP-OP-HOTVER-002
(deftest hot-verification-refuses-with-a-typed-timeout-when-no-terminal-status-arrives
  (with-stub-nrepl
    (fn [_msg] nil)
    (fn [project-root port-file]
      (let [started (System/nanoTime)
            result (hot-verify/verify! project-root
                                       (stub-profile port-file 300))
            elapsed (/ (double (- (System/nanoTime) started)) 1000000.0)]
        (is (>= elapsed 300.0))
        (is (< elapsed 5000.0))
        (is (false? (:ok result)))
        (is (= :hot-verification-timeout (:error-type result)))
        (is (re-find #"(?i)terminal status" (str (:error result))))
        (is (re-find #"300" (str (:error result))))))))

;; @spec MCP-OP-HOTVER-001
(deftest hot-verification-ends-on-an-error-status-that-is-never-followed-by-done
  (with-stub-nrepl
    (fn [{:keys [transport id]}]
      (transport/send transport
                      {:id id
                       :status ["eval-error"]
                       :ex "java.lang.RuntimeException"
                       :err "boom"}))
    (fn [project-root port-file]
      (let [started (System/nanoTime)
            result (hot-verify/verify! project-root
                                       (stub-profile port-file 60000))
            elapsed (/ (double (- (System/nanoTime) started)) 1000000.0)]
        (is (< elapsed 2000.0)
            (str "an error status is terminal; took " elapsed "ms"))
        (is (false? (:ok result)))
        (is (= :failed (:status result)))
        (is (= :hot-verification-failed (:error-type result)))
        (is (str/includes? (str (:output result)) "boom"))))))

;; @spec MCP-OP-HOTVER-002
(deftest hot-verification-refuses-when-the-transport-closes-without-any-status
  (with-stub-nrepl
    (fn [{:keys [transport]}]
      (.close ^java.io.Closeable transport))
    (fn [project-root port-file]
      (let [started (System/nanoTime)
            result (hot-verify/verify! project-root
                                       (stub-profile port-file 60000))
            elapsed (/ (double (- (System/nanoTime) started)) 1000000.0)]
        (is (< elapsed 5000.0)
            (str "a closed transport must not hang to the ceiling; took "
                 elapsed "ms"))
        (is (false? (:ok result)))
        (is (contains? #{:hot-verification-transport-closed
                         :hot-verification-connection-failed}
                       (:error-type result)))))))
