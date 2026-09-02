(ns clj-surgeon.relation-census-test
  "Pure witnesses for the relation census.

   The fixture is real bytes: every arm but three is copied verbatim from
   curtaincall-cfp-lens `src/cfp_scheduler_killer/folds.clj` at commit
   963875358a37c48ab6175ea1bea22633e4fd0306. Provenance is recorded in the
   fixture's own docstring."
  (:require
   [clj-surgeon.relation-census :as census]
   [clojure.test :refer [deftest is testing]]))

(def fixture-path "test-fixtures/relation-census/folds.clj")

(defn- fixture-census
  []
  (census/census-file {:file fixture-path :source (slurp fixture-path)}))

(defn- site-by-arm
  [result arm]
  (first (filter #(= arm (:arm %)) (:sites result))))

;; @spec MCP-OP-CENSUS-001
;; @spec MCP-OP-CENSUS-003
;; @spec MCP-OP-CENSUS-004
;; @spec MCP-OP-CENSUS-005
;; @spec MCP-OP-CENSUS-006
;; @spec MCP-OP-CENSUS-007
;; @spec MCP-OP-CENSUS-008
;; @spec MCP-OP-CENSUS-009
(deftest classifies-the-real-bytes-fold-arms-with-evidence
  (let [result (fixture-census)]
    (is (:ok result))
    (is (= 9 (:arms result)))
    (is (= 7 (count (:sites result))))
    (is (= {:door 2 :set 1 :guarded 1 :raw 1 :unknown 2} (:counts result))
        "the fixture has exactly one :raw site")

    (testing "the real task-chase arm is :guarded by the not-any? on :chase-id"
      (let [site (site-by-arm result "task.chase-recorded")]
        (is (= :guarded (:class site)))
        (is (= "(fnil conj [])" (:write site)))
        (is (= "state [:tasks k :chases]" (:target site))
            "the target resolves through update-in, fn, -> and update")
        (is (= ":chase-id" (:identity site)))
        (is (= :absent (:polarity site)))
        (is (= 78 (:guard-line site)) "the not-any? line, three above the write")
        (is (= 89 (:line site)))))

    (testing "the real agenda-selections arm is :set"
      (let [site (site-by-arm result "agenda.session-starred")]
        (is (= :set (:class site)))
        (is (= "(fnil conj #{})" (:write site)))))

    (testing "the real upsert-by arm is :door"
      (let [site (site-by-arm result "submission.speaker-added")]
        (is (= :door (:class site)))
        (is (= "upsert-by" (:door site)))))

    (testing "the real conj-once arm is :door"
      (let [site (site-by-arm result "speaker.blackout-window")]
        (is (= :door (:class site)))
        (is (= "conj-once" (:door site)))))

    (testing "the pre-fix announced-speaker shape is the only :raw site"
      (let [raw (filterv #(= :raw (:class %)) (:sites result))]
        (is (= 1 (count raw)))
        (is (= "event.speaker-announced" (:arm (first raw))))
        (is (= "state [:events slug :settings :announced-speakers]"
               (:target (first raw))))
        (is (nil? (:reason (first raw)))
            ":raw is a positive finding and carries no uncertainty reason")))

    (testing "a helper-mediated guard is :unknown, and names the helper"
      (let [site (site-by-arm result "speaker.reminder-logged")]
        (is (= :unknown (:class site)))
        (is (= :helper-mediated-guard (:reason site)))
        (is (= "reminder-already-logged?" (:detail site)))))

    (testing "a guard with the wrong polarity is :unknown, never :guarded or :raw"
      (let [site (site-by-arm result "task.chase-replayed")]
        (is (= :unknown (:class site)))
        (is (= :polarity (:reason site)))
        (is (= :present (:polarity site)))
        (is (= "state [:tasks k :chases]" (:target site))
            "target and identity matched; only the sense was wrong")
        (is (= ":chase-id" (:identity site)))))

    (testing "every reported site carries its write location and source"
      (doseq [site (:sites result)]
        (is (string? (:arm site)))
        (is (pos-int? (:line site)))
        (is (seq (:write site)))
        (is (= fixture-path (:file site)))))))

;; @spec MCP-OP-CENSUS-002
(deftest writes-outside-every-arm-are-counted-and-never-classified
  (let [result (fixture-census)]
    (is (= 3 (:outside-arms result))
        "conj-once, cons-once and upsert-by each write once, outside every arm")
    (is (empty? (filter #(contains? #{"conj-once" "cons-once" "upsert-by"}
                                    (:arm %))
                        (:sites result))))))

;; @spec MCP-OP-CENSUS-001
(deftest a-non-write-update-fn-contributes-no-site
  (let [result (fixture-census)]
    (is (nil? (site-by-arm result "agenda.session-unstarred"))
        "(fnil disj #{}) is not a collection write")))

;; @spec MCP-OP-CENSUS-011
(deftest the-plan-phase-answer-does-not-depend-on-the-mapper
  (let [inputs [{:file fixture-path :source (slurp fixture-path)}
                {:file "test-fixtures/relation-census/helpers_only.clj"
                 :source (slurp "test-fixtures/relation-census/helpers_only.clj")}]
        serial (census/plan {:inputs inputs :map-fn map})
        parallel (census/plan {:inputs inputs :map-fn pmap})
        reversed (census/plan {:inputs (vec (reverse inputs)) :map-fn pmap})]
    (is (= (dissoc serial :phases) (dissoc parallel :phases)))
    (is (= (dissoc serial :phases) (dissoc reversed :phases))
        "merge re-keys by path, so input order cannot reorder the census")
    (is (= 1 (:census-version serial)))))

;; @spec MCP-OP-CENSUS-012
(deftest a-worker-failure-names-its-file-and-publishes-no-counts
  (testing "a throwing worker becomes a typed per-file refusal, not a crash"
    (with-redefs [census/census-file (fn [_] (throw (ex-info "worker exploded" {})))]
      (let [result (census/census-input {} {:file "poison.clj" :source ""})]
        (is (false? (:ok result)))
        (is (= :census-worker-failure (:error-type result)))
        (is (= "poison.clj" (:file result)))
        (is (re-find #"poison\.clj" (:error result))))))
  (testing "one failed file aborts the whole plan and publishes no counts"
    (let [result (census/plan
                   {:inputs [{:file "ok.clj"
                              :source "(defmethod fold-event \"a\" [s e] (conj s e))"}
                             {:file "broken.clj"
                              :source "(defmethod fold-event \"b\" [s e] (conj"}]})]
      (is (false? (:ok result)))
      (is (= "broken.clj" (:file result)))
      (is (nil? (:counts result)))
      (is (nil? (:by-file result))))))

;; @spec MCP-OP-CENSUS-014
(deftest an-unparseable-file-refuses-and-names-itself
  (let [result (census/census-file
                 {:file "broken.clj"
                  :source "(defmethod fold-event \"x\" [state e] (conj"})]
    (is (false? (:ok result)))
    (is (= :unparseable-file (:error-type result)))
    (is (= "broken.clj" (:file result)))
    (is (re-find #"broken\.clj" (:error result)))))

;; @spec MCP-OP-CENSUS-014
(deftest discovery-recognises-only-files-that-define-arms
  (is (census/defines-arms? (slurp fixture-path)))
  (is (not (census/defines-arms?
             (slurp "test-fixtures/relation-census/helpers_only.clj"))))
  (is (census/defines-arms? "(defmethod folds/fold-event \"a\" [s e] s)")))

;; @spec MCP-OP-CENSUS-003
(deftest a-caller-supplied-door-changes-the-classification
  (let [source (str "(defmethod fold-event \"x\" [state {:keys [payload]}]\n"
                    "  (update state :rows\n"
                    "          (fn [rows] (my-append rows (:id payload)))))\n")
        without (census/census-file {:file "f.clj" :source source})
        with (census/census-file {:file "f.clj" :source source
                                  :doors (conj census/default-doors 'my-append)})]
    (is (zero? (count (:sites without)))
        "an undeclared door is not a collection write and is not a site")
    (is (= 1 (count (:sites with))))
    (is (= :door (:class (first (:sites with)))))
    (is (= "my-append" (:door (first (:sites with)))))))
