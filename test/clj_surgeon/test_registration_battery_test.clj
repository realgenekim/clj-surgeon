(ns clj-surgeon.test-registration-battery-test
  "The complete Round 3 gate matrix; fast retains one representative mask."
  {:lane :battery}
  (:require
   [clj-surgeon.lane-manifest :as lm]
   [clj-surgeon.lane-manifest-test]
   [clj-surgeon.test-registration :as reg]
   [clj-surgeon.tmp-leak-support :as tmp-leak]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is use-fixtures]]))

;; Nested ordinary fixtures reset this same tracker; the verbatim matrix
;; restores its outer roots after each nested test-vars call.
(def ^:private temp-roots @(ns-resolve 'clj-surgeon.lane-manifest-test 'temp-roots))
(defn- gate-var [n] (ns-resolve 'clj-surgeon.lane-manifest-test n))

(defn- first-enrollment-fixture [f]
  ;; The gate specimen needs a complete read-only inventory even while Make
  ;; is executing this namespace's first controls, before publishing them.
  ;; These fixture rows never reach register! or any retained control path.
  (let [n 'clj-surgeon.test-registration-battery-test
        inventory-path (str reg/control-root "/portability-controls.edn")]
    (if (contains? (edn/read-string (slurp inventory-path)) n)
      (f)
      (let [root (str (tmp-leak/track! temp-roots
                        (java.nio.file.Files/createTempDirectory "regns-enrollment-"
                          (into-array java.nio.file.attribute.FileAttribute []))))
            rows (into {} (for [runtime [:jvm :bb]]
                            [runtime {:namespace n :runtime runtime :status :passed :exit 0
                                      :result {:test 1 :pass 1 :fail 0 :error 0}}]))
            paths (into {} (for [[runtime row] rows]
                             (let [p (str (io/file root (str (name runtime) ".edn")))]
                               (spit p (pr-str row)) [runtime p])))
            original-snapshot reg/snapshot
            projected (atom (original-snapshot "." n))]
        (with-redefs-fn
          {(ns-resolve 'clj-surgeon.test-registration 'read-source) (fn [_ p] (@projected p))
           (ns-resolve 'clj-surgeon.test-registration 'write-tracked!)
           (fn [_ _ p text] (swap! projected assoc p text))}
          #((ns-resolve 'clj-surgeon.test-registration 'project-controls!)
            "." (atom {}) n {:namespace n :lane :battery :runtime :jvm} rows))
        (let [projection (select-keys @projected
                           [inventory-path (str reg/control-root "/portability-census.md")])]
          (with-redefs [reg/snapshot (fn [root subject] (merge (original-snapshot root subject) projection))
                        lm/namespace-runtime-controls (assoc lm/namespace-runtime-controls n paths)]
            (f)))))))

(use-fixtures :each (tmp-leak/tracking-temp-dir-fixture temp-roots) first-enrollment-fixture)

;; INTENT-TEST: REGNS-006
;; @spec REGNS-006
(deftest registration-first-contact-gate-matrix
  (let [pass-row (fn [n runtime] {:namespace n :runtime runtime :status :passed :exit 0
                                  :result {:test 1 :pass 1 :fail 0 :error 0}})
        n 'clj-surgeon.first-contact-fixture-test
        before (assoc (reg/snapshot "." n)
                      "test/clj_surgeon/first_contact_fixture_test.clj"
                      "(ns clj-surgeon.first-contact-fixture-test {:lane :battery})\n(deftest works (is true))\n")
        request {:namespace n :lane :battery :runtime :jvm}
        planned (reg/plan before request)
        _ (is (:ok planned) (pr-str planned))
        registered (merge before (:candidate planned))
        model-base (ns-resolve 'clj-surgeon.test-registration 'model-base)
        base (model-base registered)
        paths (reg/control-paths n)
        receipt-root (str (tmp-leak/track! temp-roots
                            (java.nio.file.Files/createTempDirectory "regns-gate-"
                              (into-array java.nio.file.attribute.FileAttribute []))))
        control-files (into {} (for [[k _] paths]
                                 (let [p (str (io/file receipt-root (str (name k) ".edn")))]
                                   (spit p (pr-str (pass-row n k))) [k p])))
        ;; Fixture evidence is only input to the read-only gate, never register!.
        projected (atom registered)
        _ (with-redefs-fn
            {(ns-resolve 'clj-surgeon.test-registration 'read-source) (fn [_ p] (@projected p))
             (ns-resolve 'clj-surgeon.test-registration 'write-tracked!)
             (fn [_ _ p text] (swap! projected assoc p text))}
            #((ns-resolve 'clj-surgeon.test-registration 'project-controls!)
              "." (atom {}) n request {:jvm (pass-row n :jvm) :bb (pass-row n :bb)}))
        registered (reduce-kv (fn [s k p] (assoc s p (pr-str (pass-row n k)))) @projected paths)
        disk (into {} (map (fn [[p info]] [(:namespace info) (assoc info :file p)])) (:disk base))
        ;; These are the ordinary inventory gates. The rest of the namespace
        ;; witnesses fixtures/pure laws, not the repository's registration state.
        gate-names '#{every-manifest-entry-exists-on-disk
                      runtime-portability-controls-cover-every-assignment
                      generated-portability-census-agrees-with-all-inventories
                      every-test-namespace-on-disk-is-accounted-for
                      every-manifest-namespace-declares-its-lane-in-its-own-ns-form
                      the-corpus-only-ever-grows-and-the-arithmetic-is-shown
                      the-partition-matches-round-ones-measurement}
        gate-vars (filter #(contains? gate-names (:name (meta %)))
                    (vals (ns-interns 'clj-surgeon.lane-manifest-test)))
        original-slurp (memoize slurp)
        original-read @(ns-resolve 'clj-surgeon.test-registration 'read-source)
        count-var (gate-var 'every-manifest-entry-exists-on-disk)
        old-meta (meta count-var)]
    (is (= 7 (count gate-vars)))
    (doseq [mask (range 32)]
      (let [missing? #(bit-test mask (dec %))
            snapshot (cond-> (reduce-kv (fn [s k p] (assoc s p (pr-str (pass-row n k)))) registered control-files)
                       (missing? 2) (update "test/clj_surgeon/first_contact_fixture_test.clj"
                                      str/replace "{:lane :battery}" "{}")
                       (missing? 5) (assoc (paths :jvm) (pr-str {:status :missing})
                                      (control-files :jvm) (pr-str {:status :missing})))
            runtimes (cond-> (assoc lm/namespace-runtimes n :jvm) (missing? 1) (dissoc n))
            manifest (cond-> (:manifest base) (missing? 1) (dissoc n))
            adoptions (cond-> (:adoptions base) (missing? 4) (disj n))
            scanned (cond-> disk (missing? 2) (assoc-in [n :lane] nil))
            m (cond-> (reg/model registered request)
                (missing? 1) (assoc :manifest-lane nil :registered-runtime nil)
                (missing? 2) (assoc :lane nil)
                (missing? 3) (update :pin dec)
                (missing? 4) (assoc :adopted? false)
                true (assoc :controls-valid? (not (missing? 5))))
            diagnostic (reg/checklist m)
            failures (atom [])
            ;; Only the fixture's pin literal differs; assertion order/body are
            ;; read from the actual gate source, not restated by this witness.
            source ((ns-resolve 'clj-surgeon.test-registration 'replace-pin)
                    (registered reg/witness-file) (:pin m))
            gate-form (->> ((ns-resolve 'clj-surgeon.test-registration 'forms) source)
                           (map (requiring-resolve 'rewrite-clj.zip/sexpr))
                           (filter #(and (seq? %) (= 'deftest (first %))
                                         (= 'every-manifest-entry-exists-on-disk (second %)))) first)
            test-fn (binding [*ns* (the-ns 'clj-surgeon.lane-manifest-test)]
                      (eval (list* 'fn [] (nnext gate-form))))]
        (try
          (alter-meta! count-var assoc :test test-fn)
          (with-redefs-fn
            {#'lm/namespace-runtimes runtimes #'lm/manifest manifest
             #'lm/namespace-runtime-controls (assoc lm/namespace-runtime-controls n control-files)
             (gate-var 'on-disk) (delay scanned)
             (gate-var 'adopted-since-round-one) adoptions
             #'reg/repository-checklist (fn [_] (if (:ok diagnostic) [] [diagnostic]))
             (ns-resolve 'clj-surgeon.test-registration 'read-source)
             (fn [root p] (or (snapshot p) (original-read root p)))
             #'clojure.core/slurp
             (fn [p & args] (or (snapshot (str p)) (apply original-slurp p args)))
             #'clojure.test/report
             (fn [r] (when (#{:fail :error} (:type r))
                       (swap! failures conj (assoc r :gate (:name (meta (first clojure.test/*testing-vars*)))))))}
            #(binding [clojure.test/*report-counters* (ref clojure.test/*initial-report-counters*)]
               (doseq [v gate-vars :while (empty? @failures)]
                 ;; Nested ordinary fixtures reset their tracker. Retain this
                 ;; witness's outer root so its own fixture can sweep it.
                 (let [outer-roots @temp-roots]
                   (try (clojure.test/test-vars [v])
                        (finally (swap! temp-roots into outer-roots)))))))
          (finally (reset-meta! count-var old-meta)))
        (let [first-failure (first @failures)
              msg (str (:message first-failure))]
          (println "FIRST-CONTACT-MATRIX" mask (:gate first-failure) (:type first-failure))
          (if (zero? mask)
            (is (nil? first-failure) (pr-str first-failure))
            (do (is (= :fail (:type first-failure)) (pr-str first-failure))
                (doseq [part ["Registration checklist" "lane/runtime" "ns metadata" "runtime count"
                              "adoption" "census/control" (reg/invocation request)]]
                  (is (str/includes? msg part) (str "mask " mask " first=" first-failure))))))))))
