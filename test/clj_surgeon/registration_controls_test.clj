(ns clj-surgeon.registration-controls-test
  "Sol round 3: actual failing controls cross every saved-control acceptance path."
  {:lane :battery}
  (:require
   [clj-surgeon.test-registration :as reg]
   [clj-surgeon.tmp-leak-support :as tmp]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is use-fixtures]]))

(def temp-roots (atom []))
(use-fixtures :each (tmp/tracking-temp-dir-fixture temp-roots))

(defn fixture! []
  (let [root (tmp/track! temp-roots
               (java.nio.file.Files/createTempDirectory "regns-controls-"
                 (into-array java.nio.file.attribute.FileAttribute [])))
        root (str root)
        runner (str reg/control-root "/portability_runner.clj")
        files {"deps.edn" (pr-str {:paths ["src" "test"]
                                   :deps {'org.clojure/clojure {:mvn/version "1.12.1"}
                                          'babashka/fs {:mvn/version "0.5.30"}
                                          'babashka/process {:mvn/version "0.6.23"}}
                                   :aliases {:clj-surgeon/test-deps {}}})
               "bb.edn" "{:paths [\"src\" \"test\"]}"
               "test/clj_surgeon/sol_stale_test.clj"
               "(ns clj-surgeon.sol-stale-test {:lane :battery} (:require [clojure.test :refer [deftest is]]))\n(deftest sol-plant (is false))\n"
               reg/manifest-file
               "(ns clj-surgeon.lane-manifest)\n(def manifest {})\n(def portability-runtimes '{})\n(def bb-ineligibilities {})\n(def excluded {})\n(defn lane-of [n] (get manifest n))\n"
               reg/witness-file
               "(ns clj-surgeon.lane-manifest-test)\n(deftest every-manifest-entry-exists-on-disk (is (= 0 (count runtimes))))\n(def round-one-jvm-namespaces '#{})\n(def adopted-since-round-one '#{})\n"
               reg/census-file "#{}\n"
               runner (slurp (io/file runner))
               "test/clj_surgeon/tmp_leak_support.clj" (slurp "test/clj_surgeon/tmp_leak_support.clj")
               "src/clj_surgeon/path_classification.clj" (slurp "src/clj_surgeon/path_classification.clj")}]
    (doseq [[p s] files]
      (.mkdirs (.getParentFile (io/file root p)))
      (spit (io/file root p) s))
    [root files]))

(defn pass-row [n runtime]
  {:namespace n :runtime runtime :status :passed :exit 0
   :result {:test 1 :pass 1 :fail 0 :error 0}})

;; INTENT-TEST: REGNS-010
;; @spec REGNS-010
(deftest registration-stale-control-path-matrix
  (let [[root files] (fixture!)
        n 'clj-surgeon.sol-stale-test
        request {:namespace n :lane :battery :runtime :jvm}
        executed (atom nil)
        portable {:jvm (pass-row n :jvm) :bb (pass-row n :bb)}
        ineligible {:reasons #{:sci-host-interop} :detail "Sol forged bb limitation"}
        branches [[:portable-jvm :jvm nil portable]
                  [:portable-bb :bb nil portable]
                  [:bb-ineligible :jvm ineligible
                   (assoc portable :bb (assoc (pass-row n :bb) :status :test-failed :exit 1
                                         :result {:test 1 :pass 0 :fail 1 :error 0}))]
                  [:bb-load-incompatible :jvm nil
                   {:bb-load {:namespace n :runtime :bb :mode "load"
                              :status :load-failed :exit 1 :message "unsupported class"}}]]]
    ;; Sol's EXACT two-file plant, before the cross product.
    (doseq [[k row] portable]
      (let [f (io/file root ((reg/control-paths n) k))]
        (.mkdirs (.getParentFile f))
        (spit f (pr-str (assoc row :source-sha256 "stale")))))
    (let [r (reg/register! root request)]
      (is (= :register-control-stale (:error-type r)) (pr-str r))
      (is (= 1 (get-in r [:controls :jvm :result :fail])))
      (reset! executed (:controls r))
      (is (seq (get-in r [:controls :jvm :command]))))
    (doseq [field [:command :subject :exit :status :result :wall-ms :pid :start-ticks]]
      (is (seq (reg/stale-controls {:jvm (dissoc (:jvm @executed) field)} @executed))
          (str "missing provenance " field)))
    (doseq [field [:root :source-sha256]]
      (is (seq (reg/stale-controls {:jvm (update (:jvm @executed) :subject dissoc field)} @executed))
          (str "missing subject " field)))
    (doseq [[branch runtime registration rows] branches
            path-key (keys (reg/control-paths n))
            defect [:stale :wrong-hash :missing-provenance]]
      (doseq [p (vals (reg/control-paths n))] (io/delete-file (io/file root p) true))
      (is (reg/controls-valid? n runtime registration rows) (str branch " was an acceptance branch"))
      (doseq [[k row] (assoc rows path-key (or (rows path-key)
                                               (if (= :bb-load path-key)
                                                 {:namespace n :runtime :bb :mode "load" :status :loaded :exit 0}
                                                 (pass-row n path-key))))]
        (spit (io/file root ((reg/control-paths n) k))
              (pr-str (let [row (merge (@executed k) row)]
                        (case (when (= k path-key) defect)
                          :stale (assoc-in row [:subject :source-sha256] "stale")
                          :wrong-hash (assoc-in row [:subject :source-sha256] (apply str (repeat 64 "0")))
                          :missing-provenance (dissoc row :pid)
                          row)))))
      (let [r (reg/register! root (cond-> (assoc request :runtime runtime) registration (assoc :bb-ineligible registration)))]
        (println "STALE-CONTROL-MATRIX" branch path-key defect (:error-type r)
                 (get-in r [:controls :jvm :result]))
        (is (= :register-control-stale (:error-type r)) (pr-str r))
        (is (= :rolled-back (:state r)))
        (is (= 1 (get-in r [:controls :jvm :result :fail])))
        (is (every? reg/control-provenance? (vals (:controls r))))
        (is (= files (into {} (map (fn [[p _]] [p (slurp (io/file root p))])) files)))))))

;; INTENT-TEST: REGNS-011
;; @spec REGNS-011
(deftest census-writer-census
  (let [run (requiring-resolve 'clojure.java.shell/sh)
        result (run "rg" "-l" "-F" "test/clj_surgeon/deftest_census.edn" "src" "test" "Makefile")
        references (set (str/split-lines (:out result)))
        ;; Every path-bearing owner is classified, including fixture literals.
        allowed #{"src/clj_surgeon/test_census.clj"
                  "test/clj_surgeon/lane_manifest_test.clj"
                  "test/clj_surgeon/registration_controls_test.clj"}
        writer-result (run "rg" "-l" "^\\(defn-? (write-census-ledger!|regenerate-census!)" "src" "test")
        writers (set (str/split-lines (:out writer-result)))
        entrance (slurp "src/clj_surgeon/test_registration.clj")
        gate (slurp "test/clj_surgeon/lane_manifest_test.clj")]
    (println "CENSUS-WRITER-CENSUS references" (pr-str (sort references)) "writers" (pr-str writers))
    (is (= 0 (:exit result)))
    (is (= allowed references))
    (is (= #{"src/clj_surgeon/test_census.clj"} writers))
    (is (str/includes? entrance "census/regenerate-census!"))
    (is (str/includes? gate "census/regenerate-census!"))
    (is (str/includes? entrance "census/derived-census"))
    (is (str/includes? gate "census/derived-census"))
    (is (not (str/includes? entrance "(sort additions)")))
    (is (not (str/includes? gate "(defn- write-census-ledger!")))))
