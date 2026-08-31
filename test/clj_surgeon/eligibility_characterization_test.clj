(ns clj-surgeon.eligibility-characterization-test
  "Characterization tests for prepared-request eligibility, from caller #1's
  13-case table (dev/eligibility_characterization.edn, 2026-08-31).
  Flywheel-built: fixture by the caller (judgment), case tests by oss-120b (fan-out)."
  (:require [clojure.test :refer [deftest is]]
            [clj-surgeon.mcp-prepared-request :as prep]
            [clj-surgeon.structural-lens :as lens]))

(def ^:private file "src/x.clj")
(def ^:private fhash (apply str (repeat 64 "a")))
(def ^:private form-src "(def target 1)")

(defn base-fixture
  "A minimal fully-eligible read result: project-result must attach a descriptor."
  []
  (let [form {:name "target" :form_type "def" :file file :file_hash fhash
              :platforms ["clj"] :source form-src
              :hash (lens/source-hash form-src) :line 1 :end_line 1
              :source_anchor {:file file :source_sha256 fhash :owner "target"
                              :range {:start {:line 0 :character 0}
                                      :end {:line 0 :character 14}}
                              :selection_range {:start {:line 0 :character 5}
                                                :end {:line 0 :character 11}}}}]
    {:ok true :read_complete true :next_action "none" :operation "inspect_clojure"
     :request_count 1 :file_count 1 :source_character_count (count form-src)
     :workspace_root "/private/tmp/cathedral" :file_hashes {file fhash}
     :results [{:operation "forms" :file file :file_hash fhash :form_count 1
                :source_character_count (count form-src) :forms [form]}]}))

(defn- attaches? [result] (contains? (prep/project-result result) :prepared_request))

(deftest base-is-eligible
  (is (attaches? (base-fixture)) "the verified base must receive a descriptor"))

;; ELABORATOR-FILLS: twelve single-mutation deftests, one per table case,
;; each asserting (not (attaches? mutated-base)).
(deftest bad-file-sha-refused
  (is (not (attaches? (-> (base-fixture)
                          (assoc-in [:results 0 :file_hash] "abc"))))))

(defn- nth-form [i]
  (let [src (str "(def f" i " 1)")]
    {:name (str "f" i) :form_type "def" :file file :file_hash fhash
     :platforms ["clj"] :source src
     :hash (lens/source-hash src) :line 1 :end_line 1
     :source_anchor {:file file :source_sha256 fhash :owner (str "f" i)
                     :range {:start {:line 0 :character 0}
                             :end {:line 0 :character (dec (count src))}}
                     :selection_range {:start {:line 0 :character 5}
                                       :end {:line 0 :character (+ 5 (count (str "f" i)))}}}}))

(deftest seven-forms-refused
  ;; single-axis: seven internally-valid distinct forms; ONLY :count-1-6 fails
  (let [forms (mapv nth-form (range 7))
        chars (reduce + (map (comp count :source) forms))
        b (-> (base-fixture)
              (assoc-in [:results 0 :forms] forms)
              (assoc-in [:results 0 :form_count] 7)
              (assoc-in [:results 0 :source_character_count] chars)
              (assoc :source_character_count chars))]
    (is (not (attaches? b))
        "a seventh form alone must forfeit the descriptor")))

(deftest dup-owners-refused
  ;; single-axis: counts and chars stay consistent so ONLY :owners-distinct fails
  (let [b (base-fixture)
        f (get-in b [:results 0 :forms 0])
        b2 (-> b
               (assoc-in [:results 0 :forms] [f f])
               (assoc-in [:results 0 :form_count] 2)
               (assoc-in [:results 0 :source_character_count] (* 2 (count form-src)))
               (assoc :source_character_count (* 2 (count form-src))))]
    (is (not (attaches? b2))
        "duplicate owners alone must forfeit the descriptor")))

(deftest char-mismatch-refused
  (is (not (attaches? (assoc (base-fixture) :source_character_count 999)))
      "single mutation must forfeit the descriptor: char-mismatch"))

(deftest wrong-operation-refused
  (is (not (attaches? (assoc (base-fixture) :operation "other")))
      "single mutation must forfeit the descriptor: wrong-operation"))

(deftest two-requests-refused
  (is (not (attaches? (assoc (base-fixture) :request_count 2)))
      "single mutation must forfeit the descriptor: two-requests"))

(deftest hashes-mismatch-refused
  (is (not (attaches? (assoc (base-fixture) :file_hashes {"src/x.clj" "zzz"})))
      "single mutation must forfeit the descriptor: hashes-mismatch"))

(deftest next-action-retry-refused
  (is (not (attaches? (assoc (base-fixture) :next_action "retry")))
      "single mutation must forfeit the descriptor: next-action-retry"))

(deftest ok-false-refused
  (is (not (attaches? (assoc (base-fixture) :ok false)))
      "single mutation must forfeit the descriptor: ok-false"))

(deftest forms-not-vec-refused
  (is (not (attaches? (assoc-in (base-fixture) [:results 0 :forms] "notvec")))
      "single mutation must forfeit the descriptor: forms-not-vec"))

(deftest blank-owner-refused
  (is (not (attaches? (assoc-in (base-fixture) [:results 0 :forms 0 :name] "")))
      "single mutation must forfeit the descriptor: blank-owner"))

(deftest relative-root-refused
  (is (not (attaches? (assoc (base-fixture) :workspace_root "relative/root")))
      "single mutation must forfeit the descriptor: relative-root"))


(deftest dev-explainer-agrees-with-product
  ;; binds dev/explain_eligibility.clj to product truth: agreement on the
  ;; eligible boolean for the base; drift here fails loud.
  (load-file "/tmp/cathedral/dev/explain_eligibility.clj")
  (let [explain (resolve 'explain-eligibility/explain)]
    (is (= true (:eligible? (explain (base-fixture))))
        "the dev explainer must agree the base is eligible")))
