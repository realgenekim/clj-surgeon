(ns clj-surgeon.probe-state-test
  {:lane :battery}
  (:require
   [clj-surgeon.probe :as probe]
   [clj-surgeon.probe-state :as state]
   [clj-surgeon.receipt-artifacts :as artifacts]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.java.shell :as shell]
   [clojure.test :refer [deftest is]])
  (:import
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute PosixFilePermissions)))

;; @spec STATE-HOME-002
;; @spec STATE-HOME-003
(deftest precedence-and-workspace-separation
  (doseq [[env expected] [[{"CLJ_SURGEON_STATE_HOME" "/seat" "XDG_STATE_HOME" "/xdg"} "/seat/"]
                          [{"XDG_STATE_HOME" "/xdg"} "/xdg/clj-surgeon/"]
                          [{} "/house/.local/state/clj-surgeon/"]]]
    (let [a (state/image-file "/checkout/a" nil env "/house")
          b (state/image-file "/checkout/b" nil env "/house")]
      (is (.startsWith a expected))
      (is (not= a b))
      (is (= a (state/image-file "/checkout/./a" nil env "/house")))
      (is (re-find #"/workspaces/[0-9a-f]{64}/probe.edn$" a)))))

;; @spec STATE-HOME-004
;; @spec STATE-HOME-005
;; @spec STATE-HOME-006
(deftest absent-descriptor-has-provenance
  (let [path (str (System/getProperty "java.io.tmpdir") "/absent-" (random-uuid) ".edn")
        result (probe/cli! {:ns "example-test" :image-file path})]
    (is (= :probe-image-absent (:error-type result)))
    (is (= path (:path result) (:image-file result)))
    (is (= :probe-refused (:state result)))))

(defn with-directory [f]
  (let [dir (.toFile (Files/createTempDirectory "state-home-" (make-array FileAttribute 0)))]
    (try
      (f dir)
      (finally
        (doseq [file (reverse (file-seq dir))]
          (.setWritable file true)
          (Files/deleteIfExists (.toPath file)))))))

;; @spec STATE-HOME-004
;; @spec STATE-HOME-005
(deftest override-is-read-and-published
  (with-directory
    (fn [dir]
      (let [path (str (io/file dir "override.edn"))
            descriptor {:image {:root "/another-checkout"} :port 19099}]
        (state/write-image! path descriptor)
        (is (= descriptor (edn/read-string (slurp path))))
        (let [result (probe/cli! {:ns "example-test" :image-file path})]
          (is (= :stale-probe-image (:error-type result)))
          (is (= path (:image-file result))))))))

;; @spec STATE-HOME-007
(deftest native-permission-failure-is-named
  (with-directory
    (fn [dir]
      (Files/setPosixFilePermissions (.toPath dir) (PosixFilePermissions/fromString "r-x------"))
      (let [path (str (io/file dir "probe.edn"))
            result (try (state/write-image! path {}) (catch Exception e (ex-data e)))]
        (is (= :probe-state-not-writable (:error-type result)))
        (is (= path (:path result)))
        (is (= "EACCES" (:errno result)))
        (is (false? (.exists (io/file path))))))))

;; @spec STATE-HOME-008
(deftest envelope-refuses-before-creation
  (with-directory
    (fn [dir]
      (let [allowed (str (io/file dir "allowed"))
            path (str (io/file dir "outside" "probe.edn"))
            result (binding [artifacts/*destination-envelope*
                             (artifacts/destination-envelope [allowed] :launcher)]
                     (try (state/write-image! path {}) (catch Exception e (ex-data e))))]
        (is (= :write-outside-envelope (:error-type result)))
        (is (false? (.exists (io/file dir "outside"))))))))

;; @spec STATE-HOME-001
(deftest
  warm-leaves-fresh-checkout-clean
  (doseq [mode ["explicit" "xdg" "fallback" "unwritable"]]
    (let [result (shell/sh
                   "python3"
                   "test/state_home_warm.py"
                   (System/getProperty "java.class.path")
                   mode)]
      (is
        (zero? (:exit result))
        (str mode ": " (:out result) (:err result))))))

;; @spec STATE-HOME-009
;; @spec STATE-HOME-010
(deftest configured-root-cartesian-matrix
  (let [r (shell/sh "python3" "test/state_home_matrix.py"
                    (System/getProperty "java.class.path"))]
    (is (zero? (:exit r)) (str (:out r) (:err r)))))

;; @spec STATE-HOME-011
;; @spec STATE-HOME-012
(deftest publication-fault-matrix
  (with-directory
    (fn [dir]
      (let [path (str (io/file dir "probe.edn"))
            seam (ns-resolve 'clj-surgeon.probe-state 'publish-stage!)]
        (is (some? seam) "One injectable writer seam must cover publication stages")
        (when seam
          (let [original @seam]
            (doseq [stage [:create :write :sync :publish]
                    [message errno] [["No space left on device" "ENOSPC"]
                                     ["Disk quota exceeded" "EDQUOT"]
                                     ["Permission denied" "EACCES"]
                                     ["Interrupted system call" "EINTR"]
                                     ["File too large" "EFBIG"]
                                     ["Short write" :unavailable]]]
              (spit path "{:prior true}\n")
              (let [r (with-redefs-fn
                        {seam (fn [at & args]
                                (if (= stage at)
                                  (do
                                    (when (= at :write)
                                      (spit (str (first args)) "partial"))
                                    (throw (java.io.IOException. message)))
                                  (apply original at args)))}
                        #(try (state/write-image! path {:next true})
                              (catch Exception e (ex-data e))))]
                (is (= :probe-state-not-writable (:error-type r)) (pr-str [stage r]))
                (is (= errno (:errno r)) (pr-str [stage r]))
                (is (= "{:prior true}\n" (slurp path)))
                (is (= #{"probe.edn"} (set (.list dir))))))))))))

;; @spec STATE-HOME-011
;; @spec STATE-HOME-012
(deftest native-efbig-preserves-descriptor
  (with-directory
    (fn [dir]
      (let [path (str (io/file dir "probe.edn"))
            _ (spit path "{:prior true}\n")
            code (pr-str
                   `(do (require '~'clj-surgeon.probe-state)
                        (try (~'clj-surgeon.probe-state/write-image!
                               ~path {:payload (apply str (repeat 10000 "x"))})
                             (catch Exception ~'e (prn (ex-data ~'e))))))
            r (shell/sh "bash" "-c"
                        "ulimit -f 1; exec java -Xmx512m -cp \"$1\" clojure.main -e \"$2\""
                        "efbig-witness" (System/getProperty "java.class.path") code)]
        (is (zero? (:exit r)) (pr-str r))
        (let [receipt (edn/read-string (:out r))]
          (is (= :probe-state-not-writable (:error-type receipt)))
          (is (= "EFBIG" (:errno receipt))))
        (is (= "{:prior true}\n" (slurp path)))
        (is (= #{"probe.edn"} (set (.list dir))))))))

;; @spec STATE-HOME-011
(deftest short-write-cannot-publish
  (with-directory
    (fn [dir]
      (let [path (str (io/file dir "probe.edn"))
            seam (ns-resolve 'clj-surgeon.probe-state 'publish-stage!)]
        (spit path "{:prior true}")
        (is (some? seam))
        (when seam
          (let [original @seam
                r (with-redefs-fn
                    {seam (fn [stage & args]
                            (if (= stage :write)
                              (spit (str (first args)) "partial")
                              (apply original stage args)))}
                    #(try (state/write-image! path {:next true})
                          (catch Exception e (ex-data e))))]
            (is (= :probe-state-not-writable (:error-type r)))
            (is (= "{:prior true}" (slurp path)))
            (is (= #{"probe.edn"} (set (.list dir))))))))))

;; @spec STATE-HOME-009
;; @spec STATE-HOME-010
(deftest redirected-descendants-and-overrides-refuse-before-start
  (with-directory
    (fn [dir]
      (let [workspace (io/file dir "checkout")
            root (io/file dir "state")
            starts (atom 0)
            resolver requiring-resolve]
        (.mkdir workspace)
        (.mkdir root)
        (with-redefs [state/state-root (fn [& _] (str root))
                      clojure.core/requiring-resolve
                      (fn [sym]
                        (if (= sym 'clj-surgeon.mcp-http-server/start)
                          (fn [& _] (swap! starts inc))
                          (resolver sym)))]
          (doseq [override [(str (io/file workspace "probe.edn")) nil]]
            (when-not override
              (Files/createSymbolicLink (.toPath (io/file root "workspaces"))
                                        (.toPath workspace) (make-array FileAttribute 0)))
            (let [r (binding [*err* (java.io.StringWriter.)]
                      (try (state/warm! {:project-dir (str workspace) :probe-image-file override})
                           (catch Exception e (ex-data e))))]
              (is (= :state-root-inside-workspace (:error-type r)))
              (is (empty? (.list workspace)))
              (is (zero? @starts)))))))))

;; @spec PROBE-RECEIPT-001
;; @spec PROBE-RECEIPT-002
(deftest local-filesystem-failures-never-connect
  (with-directory
    (fn [dir]
      (let [attempts (atom 0)
            resolver requiring-resolve
            malformed (io/file dir "bad\n:state :probe-passed\".edn")
            denied (io/file dir "denied.edn")
            large (io/file dir "large.edn")]
        (spit malformed "{:image")
        (spit denied "{}")
        (spit large (apply str (repeat 9000 "x")))
        (Files/setPosixFilePermissions (.toPath denied) (PosixFilePermissions/fromString "---------"))
        (with-redefs [clojure.core/requiring-resolve
                      (fn [sym]
                        (if (= sym 'babashka.http-client/post)
                          (fn [& _] (swap! attempts inc) (throw (Exception. "NETWORK ATTEMPT")))
                          (resolver sym)))]
          (doseq [[path kind] [[(str (io/file dir "absent.edn")) :probe-image-absent]
                               [(str denied) :probe-image-unreadable]
                               [(str dir) :probe-image-unreadable]
                               [(str malformed) :probe-image-malformed]
                               [(str large) :probe-image-too-large]
                               [(str (io/file dir (apply str (repeat 300 "p")))) :probe-image-path-invalid]
                               [(str dir "/nul\u0000.edn") :probe-image-path-invalid]]]
            (let [r (try (probe/cli! {:ns "fixture-test" :image-file path})
                         (catch Exception e {:escaped (.getMessage e)}))
                  wire (pr-str r)]
              (is (= kind (:error-type r)) (pr-str r))
              (is (= path (:path r) (:image-file r)) (pr-str r))
              (is (= :probe-refused (:state r)))
              (is (< (count wire) 4096))
              (is (not (.contains wire "\n")))
              (is (= r (edn/read-string wire)))
              (is (zero? @attempts))))
          ;; Positive transport control proves the same counter is live.
          (let [valid (io/file dir "valid.edn")]
            (spit valid (pr-str {:image (probe/image-identity ".") :port 19099}))
            (let [missing (str "absent-identity-" (random-uuid))
                  r (with-redefs [probe/identity-files [missing]]
                      (probe/cli! {:ns "fixture-test" :image-file (str valid)}))]
              (is (= :probe-image-unreadable (:error-type r)))
              (is (= (str (io/file (.getCanonicalPath (io/file ".")) missing)) (:path r)))
              (is (= (str valid) (:image-file r)))
              (is (zero? @attempts)))
            (probe/cli! {:ns "fixture-test" :image-file (str valid)})
            (is (= 1 @attempts))))))))

;; @spec PROBE-RECEIPT-001
;; @spec PROBE-RECEIPT-002
(deftest invalid-path-receipt-is-bounded-at-the-crossing
  (let [path (str (System/getProperty "java.io.tmpdir") "/" (apply str (repeat 10000 "\"\n")))
        result (try (probe/cli! {:ns "fixture-test" :image-file path})
                    (catch Exception e {:escaped (.getMessage e)}))
        wire (pr-str result)]
    (is (= :probe-image-path-invalid (:error-type result)))
    (is (<= (alength (.getBytes wire "UTF-8")) 4096))
    (is (= (count path) (get-in result [:diagnostic-truncation :path :characters])))
    (is (.startsWith path (:path result)))
    (is (= result (edn/read-string wire)))
    (is (not (.contains wire "\n")))))
