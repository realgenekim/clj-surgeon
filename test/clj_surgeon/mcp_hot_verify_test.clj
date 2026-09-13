(ns clj-surgeon.mcp-hot-verify-test
  {:lane :integration}
  (:require
   [clj-surgeon.mcp-hot-verify :as hot-verify]
   [clj-surgeon.probe :as probe]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [nrepl.server :as nrepl-server]
   [nrepl.transport :as transport]))

(deftest passing-law
  (is (= 4 (+ 2 2))))

(defn- with-image-roots [root dirs f]
  (let [classpath (System/getProperty "java.class.path")
        loader (clojure.lang.DynamicClassLoader. (clojure.lang.RT/baseLoader))
        paths (mapv #(io/file root %) dirs)]
    (try
      (doseq [path paths] (.addURL loader (.toURL (.toURI path))))
      (System/setProperty "java.class.path"
                          (str/join java.io.File/pathSeparator (cons classpath paths)))
      (with-bindings {clojure.lang.Compiler/LOADER loader} (f))
      (finally (System/setProperty "java.class.path" classpath)))))

;; @spec BB-PROBE-003 -- Sol round-five F1: live dev/experiments was omitted.
(deftest probe-reloads-image-classpath-experiment-dependency
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                        "probe-classpath-" (make-array java.nio.file.attribute.FileAttribute 0)))
        dependency (io/file root "dev/experiments/sol_round5/dependency.clj")
        target (io/file root "test/sol_round5/probe_test.clj")
        original-classpath (System/getProperty "java.class.path")
        loader (clojure.lang.DynamicClassLoader. (clojure.lang.RT/baseLoader))]
    (try
      (io/make-parents dependency)
      (io/make-parents target)
      (doseq [path probe/identity-files]
        (io/make-parents (io/file root path))
        (io/copy (io/file path) (io/file root path)))
      (doseq [dir ["test" "dev/experiments"]]
        (.addURL loader (.toURL (.toURI (io/file root dir)))))
      (System/setProperty "java.class.path"
                          (str original-classpath java.io.File/pathSeparator
                               (io/file root "test") java.io.File/pathSeparator
                               (io/file root "dev/experiments")))
      (spit dependency "(ns sol-round5.dependency) (def value 1)")
      (spit target "(ns sol-round5.probe-test (:require [clojure.test :refer [deftest is]] [sol-round5.dependency :as d])) (deftest stale-check (is (= 1 d/value)))")
      (with-bindings {clojure.lang.Compiler/LOADER loader}
        (require 'sol-round5.probe-test :reload)
        (is (= 1 (var-get (resolve 'sol-round5.dependency/value))))
        (let [image (probe/image-identity (.getCanonicalPath root))
              request {:ns "sol-round5.probe-test" :image image}]
          (is (nil? (probe/request-problem image (probe/fingerprint (:root image)) request)))
          (spit dependency "(ns sol-round5.dependency) (def value 2)")
          (let [receipt (hot-verify/probe! image request)]
            (println :sol-round5 receipt :loaded-value (var-get (resolve 'sol-round5.dependency/value)) :disk-value 2)
            (is (= :probe-failed (:state receipt)))
            (is (= 1 (:failures receipt)))
            (is (= ["sol-round5.dependency" "sol-round5.probe-test"] (:reloaded receipt)))
            (is (= 2 (var-get (resolve 'sol-round5.dependency/value))))
            (is (= 2 (:closure-expected receipt)))
            (is (= "jar" (.getProtocol (io/resource "clojure/test.clj"))))
            (is (= 1 (:external receipt)))
            (is (= (->> (str/split (System/getProperty "java.class.path")
                          (re-pattern (java.util.regex.Pattern/quote java.io.File/pathSeparator)))
                        (map io/file)
                        (filter #(.isDirectory %))
                        (mapv #(.getCanonicalPath %)))
                   (:roots receipt))))))
      (finally
        (System/setProperty "java.class.path" original-classpath)
        (doseq [n '[sol-round5.probe-test sol-round5.dependency]]
          (when (find-ns n) (remove-ns n))
          (dosync (alter @#'clojure.core/*loaded-libs* disj n)))
        (doseq [file (reverse (file-seq root))] (io/delete-file file))))))

;; @spec BB-PROBE-003 -- refusal is preflight, including already-loaded code.
(deftest probe-refuses-dependencies-outside-image-roots
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                        "probe-unresolved-" (make-array java.nio.file.attribute.FileAttribute 0)))
        dependency (io/file root "outside/sol_unresolved/dependency.clj")
        target (io/file root "test/sol_unresolved/probe_test.clj")]
    (try
      (io/make-parents dependency)
      (io/make-parents target)
      (doseq [path probe/identity-files]
        (io/make-parents (io/file root path))
        (io/copy (io/file path) (io/file root path)))
      (spit dependency "(ns sol-unresolved.dependency) (def value 1)")
      (spit target "(ns sol-unresolved.probe-test (:require [sol-unresolved.dependency]))")
      (with-image-roots root ["test"]
        (fn []
          ;; The loader can see this file, but it is outside the image's roots.
          (.addURL ^clojure.lang.DynamicClassLoader @clojure.lang.Compiler/LOADER
                   (.toURL (.toURI (io/file root "outside"))))
          (require 'sol-unresolved.probe-test :reload)
          (let [image (probe/image-identity (.getCanonicalPath root))
                request {:ns "sol-unresolved.probe-test" :image image}]
            (doseq [case [:outside :missing]]
              (when (= :missing case) (io/delete-file dependency))
              (let [reloads (atom [])
                    original require
                    receipt (with-redefs [clojure.core/require
                                          (fn [& args]
                                            (when (some #{:reload} args) (swap! reloads conj args))
                                            (apply original args))]
                              (hot-verify/probe! image request))]
                (is (some? (find-ns 'sol-unresolved.dependency)))
                (is (= :probe-dependency-unresolved (:error-type receipt)) case)
                (is (= 'sol-unresolved.dependency (:ns receipt)))
                (is (= (when (= :outside case) (str (.toURL (.toURI dependency))))
                       (:resolved-to receipt)))
                (is (= (hot-verify/probe-source-roots) (:roots receipt)))
                (is (= [] (:reloaded receipt)))
                (is (= [] @reloads)))))))
      (finally
        (doseq [n '[sol-unresolved.probe-test sol-unresolved.dependency]]
          (when (find-ns n) (remove-ns n))
          (dosync (alter @#'clojure.core/*loaded-libs* disj n)))
        (doseq [file (reverse (file-seq root))] (io/delete-file file))))))

;; @spec BB-PROBE-003 -- Sol F1, round-four review of e3ffc6a7.
(deftest probe-reloads-prefix-list-dependency
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                        "probe-prefix-" (make-array java.nio.file.attribute.FileAttribute 0)))
        dependency (io/file root "src/foo/bar.clj")
        target (io/file root "test/demo/probe_test.clj")]
    (try
      (io/make-parents dependency)
      (io/make-parents target)
      (spit dependency "(ns foo.bar) (def value 1)")
      (spit target "(ns demo.probe-test (:require (foo [bar :as b])))")
      (with-image-roots root ["src" "test"]
        #(is (= '[foo.bar demo.probe-test]
                (hot-verify/probe-reload-order (.getCanonicalPath root) 'demo.probe-test))))
      (finally
        (doseq [file (reverse (file-seq root))] (io/delete-file file))))))

;; @spec BB-PROBE-003
(deftest probe-require-shape-matrix
  (doseq [[source expected]
          [["(ns demo (:require (foo [bar :as b])))" '[foo.bar]]
           ["(ns demo (:require (foo (bar [baz :as b]))))" '[foo.bar.baz]]
           ["(ns demo (:require [foo [bar :as b] baz]))" '[foo.bar foo.baz]]
           ["(ns demo (:require [foo.bar :as b] foo.baz))" '[foo.bar foo.baz]]
           ["(ns demo (:require [\"foo.bar\" :as b] \"foo.baz\"))" '[foo.bar foo.baz]]
           ["(ns demo (:require-macros (foo [bar :as b])) (:use foo.baz))" '[foo.bar foo.baz]]
           ["(ns demo (:require #?(:cljs [wrong.lib] :clj [foo.bar])))" '[foo.bar]]
           ["(ns demo #?(:clj (:require foo.bar) :cljs (:require wrong.lib)))" '[foo.bar]]
           ["(ns demo (:require #?@(:clj [foo.bar [foo.baz]] :cljs [wrong.lib])))" '[foo.bar foo.baz]]
           ["(ns demo #?@(:clj [(:require foo.bar) (:use foo.baz)]))" '[foo.bar foo.baz]]
           ["(ns demo (:require #?(:cljs wrong.lib :default foo.bar)))" '[foo.bar]]
           ["(ns demo (:require #?(:cljs wrong.lib) #_[ignored.lib] foo.bar :reload))" '[foo.bar]]
           ["(ns demo (:require ^:meta [foo.bar :as b]))" '[foo.bar]]
           ["(ns demo (:import java.io.File))" []]
           ["'(ns decoy) #_(ns discarded) (ns demo (:require foo.bar))" '[foo.bar]]]]
    (is (= expected (hot-verify/probe-require-libs source "fixture.cljc")) source))
  (doseq [form ["{:unknown foo}" "42" "[]" "[foo.bar :as]" "(foo {:unknown bar})" "#?@(:clj foo.bar)" "#?(:clj)" "#?(:clj foo.bar :cljs)"]]
    (let [data (try
                 (hot-verify/probe-require-libs (str "(ns demo (:require " form "))") "fixture.clj")
                 (catch Exception e (ex-data e)))]
      (is (= :probe-require-unparsed (:error-type data)) form)
      (is (= "fixture.clj" (:file data)))
      (is (string? (:form data))))))

;; @spec BB-PROBE-003 -- execute the green-that-lies failure, not only its order.
(deftest probe-observes-changed-prefix-dependency-and-refuses-unknown-before-reload
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                        "probe-stale-" (make-array java.nio.file.attribute.FileAttribute 0)))
        dependency (io/file root "src/probe_fixture/dependency.clj")
        target (io/file root "test/probe_fixture/check_test.clj")
        loader (clojure.lang.DynamicClassLoader. (clojure.lang.RT/baseLoader))
        classpath (System/getProperty "java.class.path")]
    (try
      (io/make-parents dependency)
      (io/make-parents target)
      (doseq [path probe/identity-files]
        (io/make-parents (io/file root path))
        (io/copy (io/file path) (io/file root path)))
      (.addURL loader (.toURL (.toURI (io/file root "src"))))
      (.addURL loader (.toURL (.toURI (io/file root "test"))))
      (System/setProperty "java.class.path"
                          (str/join java.io.File/pathSeparator
                                    [classpath (io/file root "src") (io/file root "test")]))
      (spit dependency "(ns probe-fixture.dependency) (def value 1)")
      (spit target "(ns probe-fixture.check-test (:require [clojure.test :refer [deftest is]] (probe-fixture [dependency :as d]))) (deftest stale-check (is (= 1 d/value)))")
      (let [image (probe/image-identity (.getCanonicalPath root))
            request {:ns "probe-fixture.check-test" :image image}]
        (with-bindings {clojure.lang.Compiler/LOADER loader}
          (require 'probe-fixture.check-test :reload)
          (is (= 1 (var-get (resolve 'probe-fixture.dependency/value))))
          (spit dependency "(ns probe-fixture.dependency) (def value 2)")
          (let [receipt (hot-verify/probe! image request)]
            (is (= :probe-failed (:state receipt)))
            (is (= [1 1 1] ((juxt :tests :assertions :failures) receipt)))
            (is (= 2 (:closure-expected receipt)))
            (is (= ["probe-fixture.dependency" "probe-fixture.check-test"] (:reloaded receipt))))
          (spit dependency "(ns probe-fixture.dependency (:require {:unknown foo}))")
          (let [reloads (atom [])
                receipt (with-redefs [clojure.core/require (fn [& args] (swap! reloads conj args))]
                          (hot-verify/probe! image request))]
            (is (= :probe-require-unparsed (:error-type receipt)))
            (is (= "{:unknown foo}" (:form receipt)))
            (is (= (.getCanonicalPath dependency) (:file receipt)))
            (is (= [] (:reloaded receipt)))
            (is (= [] @reloads)))))
      (finally
        (System/setProperty "java.class.path" classpath)
        (doseq [n '[probe-fixture.check-test probe-fixture.dependency]]
          (when (find-ns n) (remove-ns n))
          (dosync (alter @#'clojure.core/*loaded-libs* disj n)))
        (doseq [file (reverse (file-seq root))] (io/delete-file file))))))

;; @spec BB-PROBE-003
(deftest probe-receipt-retains-expected-closure-after-reload-failure
  (let [image (probe/image-identity ".")
        receipt (with-redefs [hot-verify/probe-reload-order (constantly '[foo.bar demo.probe-test])
                              clojure.core/require (fn [& _] (throw (Exception. "reload failed")))]
                  (hot-verify/probe! image {:ns "demo.probe-test" :image image}))]
    (is (= :probe-failed (:state receipt)))
    (is (= 2 (:closure-expected receipt)))
    (is (= [] (:reloaded receipt)))))

;; @spec BB-PROBE-003 -- Sol F3, round-two review of 09486a6f.
(deftest probe-authorizes-the-requested-test-target-before-reload
  (let [image (probe/image-identity ".")
        request {:ns "clj-surgeon.core" :image image}
        reloads (atom [])
        original-require require
        receipt (with-redefs [clojure.core/require
                              (fn [& args]
                                (when (some #{:reload} args)
                                  (swap! reloads conj args))
                                (apply original-require args))]
                  (hot-verify/probe! image request))]
    (is (nil? (probe/request-problem image (probe/fingerprint ".") request)))
    (is (= :probe-refused (:state receipt)))
    (is (= :probe-target-not-a-test-namespace (:error-type receipt)))
    (is (= [] (:reloaded receipt)))
    (is (= [] @reloads) "observe the reload boundary, not only receipt claims")
    (is (= 'clj-surgeon.core (:requested receipt)))
    (is (= "src/clj_surgeon/core.clj" (:source receipt)))
    (is (= ["test"] (:authorized-roots receipt)))
    (is (str/includes? (str (:error receipt))
                       "warm image executing production code on request, with no test to bound it"))
    (testing "missing test source preserves the existing refusal"
      (let [missing (hot-verify/probe! image {:ns "absent.probe-test" :image image})]
        (is (= :probe-refused (:state missing)))
        (is (= :probe-namespace-not-found (:error-type missing)))
        (is (= [] (:reloaded missing)))))
    (testing "an authorized real test reloads its production dependencies"
      (let [accepted (hot-verify/probe! image {:ns "clj-surgeon.forms-test" :image image})]
        (is (= :probe-passed (:state accepted)))
        (is (= ["clj-surgeon.fields" "clj-surgeon.forms" "clj-surgeon.forms-test"]
               (:reloaded accepted)))
        (is (= [24 94 0] ((juxt :tests :assertions :failures) accepted)))))))

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
      (is (false? (hot-verify/valid-profile? profile)))))
  ;; @spec BB-PROBE-003
  (testing "local dependency reload order and missing namespace refusal"
    (let [root (.getCanonicalPath (io/file "."))]
      (is (= '[clj-surgeon.fields clj-surgeon.forms clj-surgeon.forms-test]
             (hot-verify/probe-reload-order root 'clj-surgeon.forms-test)))
      (is (= :probe-namespace-not-found
             (try (hot-verify/probe-reload-order root 'absent.probe-test)
                  (catch Exception e (:error-type (ex-data e)))))))))

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
        (is (= :hot-verification-transport-closed (:error-type result)))))))

;; @spec MCP-OP-HOTVER-002
(deftest hot-verification-preserves-output-when-the-transport-closes-mid-read
  (with-stub-nrepl
    (fn [{:keys [transport id]}]
      (transport/send transport {:id id :out "partial output before the close"})
      (.close ^java.io.Closeable transport))
    (fn [project-root port-file]
      (let [result (hot-verify/verify! project-root
                                       (stub-profile port-file 60000))]
        (is (false? (:ok result)))
        (is (= :hot-verification-transport-closed (:error-type result))
            "a closure DURING an established read is not a connect failure")
        (is (str/includes? (str (:output result)) "partial output before")
            "the responses read before the closure are diagnostic, not discarded")))))

;; @spec MCP-OP-HOTVER-002
(deftest hot-verification-names-a-connect-failure-as-a-connect-failure
  (let [dead-port (with-open [socket (java.net.ServerSocket. 0)]
                    (.getLocalPort socket))
        project-root (System/getProperty "user.dir")
        port-file (str ".hot-verify-test-" (random-uuid) ".port")
        file (io/file project-root port-file)]
    (try
      (spit file dead-port)
      (let [result (hot-verify/verify! project-root
                                       (stub-profile port-file 60000))]
        (is (false? (:ok result)))
        (is (= :hot-verification-connection-failed (:error-type result))
            "failing to CONNECT is a different typed failure from a closure mid-read"))
      (finally
        (io/delete-file file true)))))

;; @spec MCP-OP-HOTVER-001
(deftest hot-verification-does-not-terminate-on-another-messages-done
  (with-stub-nrepl
    (fn [{:keys [transport id]}]
      (transport/send transport {:id "a-different-message" :status ["done"]})
      (transport/send
        transport
        {:id id
         :status ["done"]
         :value (pr-str {:cwd (System/getProperty "user.dir")
                         :pid 91
                         :summary {:test 1 :pass 1 :fail 0 :error 0}})}))
    (fn [project-root port-file]
      (let [result (hot-verify/verify! project-root
                                       (stub-profile port-file 60000))]
        (is (:ok result) "a foreign id's done must not end this message's read")
        (is (= 91 (:pid result)))))))

;; @spec MCP-OP-HOTVER-002
(deftest hot-verification-deadline-is-not-reset-by-non-terminal-responses
  (let [pump (atom nil)]
    (with-stub-nrepl
      (fn [{:keys [transport id]}]
        (reset! pump
                (future
                  (try
                    (dotimes [n 200]
                      (^{:temporal-purpose :spaced-stimulus} Thread/sleep 50)
                      (transport/send transport {:id id :out (str "tick " n)}))
                    (catch Exception _ nil)))))
      (fn [project-root port-file]
        (let [started (System/nanoTime)
              result (hot-verify/verify! project-root
                                         (stub-profile port-file 500))
              elapsed (/ (double (- (System/nanoTime) started)) 1000000.0)]
          (future-cancel @pump)
          (is (>= elapsed 500.0))
          (is (< elapsed 3000.0)
              (str "responses arriving every 50 ms must not reset the 500 ms "
                   "deadline; took " elapsed "ms"))
          (is (= :hot-verification-timeout (:error-type result)))
          (is (str/includes? (str (:output result)) "tick")
              "the non-terminal output read before the ceiling is diagnostic"))))))

;; @spec MCP-OP-HOTVER-001
(deftest hot-verification-treats-interrupted-as-a-failure-never-a-pass
  (let [good-value (pr-str {:cwd (System/getProperty "user.dir")
                            :pid 77
                            :summary {:test 1 :pass 1 :fail 0 :error 0}})]
    (doseq [[label send-responses]
            [["interrupted alone"
              (fn [transport id]
                (transport/send transport {:id id :status ["interrupted"]}))]
             ["a good value, then interrupted"
              (fn [transport id]
                (transport/send transport {:id id :value good-value})
                (transport/send transport {:id id :status ["interrupted"]}))]
             ["a good value and interrupted in one response"
              (fn [transport id]
                (transport/send transport {:id id
                                           :value good-value
                                           :status ["interrupted"]}))]]]
      (testing label
        (with-stub-nrepl
          (fn [{:keys [transport id]}] (send-responses transport id))
          (fn [project-root port-file]
            (let [started (System/nanoTime)
                  result (hot-verify/verify! project-root
                                             (stub-profile port-file 60000))
                  elapsed (/ (double (- (System/nanoTime) started)) 1000000.0)]
              (is (< elapsed 2000.0)
                  (str "interrupted is terminal; took " elapsed "ms"))
              (is (false? (:ok result))
                  "an interrupted evaluation never verified anything")
              (is (= :failed (:status result)))
              (is (= :hot-verification-failed (:error-type result))))))))))
