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
