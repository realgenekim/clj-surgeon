(ns clj-surgeon.mcp-extraction-test
  (:require
   [clj-surgeon.mcp-extraction :as extraction]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(def source
  (str "(ns sample.core\n"
       "  (:require [clojure.string :as str]))\n\n"
       ";; Preserve this helper comment.\n"
       "(defn helper [value]\n"
       "  (str/upper-case value))\n\n"
       "(defn keep-me [] :ok)\n"))

(def request
  {:file "src/sample/core.clj"
   :to "src/sample/moved.clj"
   :forms ["helper"]
   :require-policy :copy-all
   :caller-changes []
   :ignored-caller-files []
   :expect {:forms 1 :caller-edits 0 :files 2}
   :source source
   :target-ns "sample.moved"
   :workspace-sources {}})

(deftest compiles-source-and-new-target-as-one-pure-future-snapshot
  (let [result (extraction/compile-extraction request)]
    (is (:ok result))
    (is (= :structural-candidates-only
           (get-in result [:caller-proof :level])))
    (is (false? (get-in result [:caller-proof
                                :zero-callers-authoritative])))
    (is (= ["helper"] (:forms result)))
    (is (= ["src/sample/moved.clj"] (:created-files result)))
    (is (= source (get-in result [:original-sources "src/sample/core.clj"])))
    (is (= "(ns sample.core\n  (:require [clojure.string :as str]))\n\n(defn keep-me [] :ok)\n"
           (get-in result [:future-sources "src/sample/core.clj"])))
    (is (= (str "(ns sample.moved\n"
                "  (:require [clojure.string :as str]))\n\n"
                ";; Preserve this helper comment.\n"
                "(defn helper [value]\n"
                "  (str/upper-case value))\n")
           (get-in result [:future-sources "src/sample/moved.clj"])))))

(defn memory-io
  [initial fail-on-write]
  (let [state (atom initial)
        writes (atom 0)]
    {:state state
     :read-source #(get @state %)
     :exists? #(contains? @state %)
     :write-source!
     (fn [file source]
       (let [write-number (swap! writes inc)]
         (when (= fail-on-write write-number)
           (throw (ex-info "injected write failure" {:file file})))
         (swap! state assoc file source)))
     :delete-file! #(swap! state dissoc %)}))

(deftest staged-formatting-preserves-extraction-ownership-and-exact-undo
  (let [compiled (extraction/compile-extraction request)
        formatted (update (:future-sources compiled)
                          "src/sample/moved.clj"
                          #(str/replace % "\n\n;; Preserve" "\n\n\n;; Preserve"))
        prepared (extraction/with-future-sources compiled formatted)
        io (memory-io {(:file request) source} nil)
        committed (extraction/commit! prepared io)]
    (is (:ok prepared))
    (is (= (:original-sources compiled) (:original-sources prepared)))
    (is (= (:created-files compiled) (:created-files prepared)))
    (is (:ok committed))
    (is (= formatted @(:state io)))
    (is (= formatted
           (into {} (map (juxt :file :result-source))
                 (get-in committed [:receipt :files]))))
    (is (:ok (extraction/undo! (:receipt committed) io)))
    (is (= {(:file request) source} @(:state io))))
  (doseq [future-sources
          [{}
           {"src/sample/core.clj" "(ns sample.core)"}
           {"src/sample/core.clj" "(defn broken ["
            "src/sample/moved.clj" "(ns sample.moved)"}]]
    (let [result (extraction/with-future-sources
                   (extraction/compile-extraction request) future-sources)]
      (is (false? (:ok result)))
      (is (keyword? (:error-type result))))))

(deftest refuses-invalid-or-unproven-extractions-as-data
  (testing "the source and target cannot collide"
    (is (= :extraction-path-collision
           (:error-type
             (extraction/compile-extraction
               (assoc request :to (:file request)))))))
  (testing "form names must be unique"
    (is (= :invalid-extraction-forms
           (:error-type
             (extraction/compile-extraction
               (assoc request :forms ["helper" "helper"]))))))
  (testing "the exact compiled count is part of the contract"
    (is (= {:expected 2 :actual 1}
           (select-keys
             (extraction/compile-extraction
               (assoc-in request [:expect :forms] 2))
             [:expected :actual]))))
  (testing "a missing owner refuses without future files"
    (let [result (extraction/compile-extraction
                   (assoc request :forms ["absent"]))]
      (is (= :extraction-plan-refused (:error-type result)))
      (is (:source-unchanged result))
      (is (nil? (:future-files result))))))

(deftest caller-proof-levels-distinguish-completeness-from-safe-execution
  (is (= #{:semantic-complete :structural-candidates-only
           :caller-proof-unavailable}
         (set (keys extraction/caller-proof-levels))))
  (is (true? (get-in extraction/caller-proof-levels
                     [:semantic-complete :zero-callers-authoritative])))
  (is (false? (get-in extraction/caller-proof-levels
                      [:structural-candidates-only
                       :zero-callers-authoritative])))
  (is (false? (get-in extraction/caller-proof-levels
                      [:caller-proof-unavailable :scan-complete]))))

(deftest commits-and-recovers-mixed-create-update-transactions
  (let [compiled (extraction/compile-extraction request)
        successful-io (memory-io {(:file request) source} nil)
        success (extraction/commit! compiled successful-io)]
    (is (:ok success))
    (is (string? (:receipt-hash success)))
    (is (= (:receipt-hash success)
           (get-in success [:receipt :receipt-hash])))
    (is (= :structural-candidates-only
           (get-in success [:receipt :caller-proof :level])))
    (is (= (:future-sources compiled) @(:state successful-io)))
    (is (= [false true]
           (mapv #(boolean (:absent-before %))
                 (get-in success [:receipt :files]))))
    (let [undo (extraction/undo! (:receipt success) successful-io)]
      (is (:ok undo))
      (is (= {(:file request) source} @(:state successful-io)))))
  (testing "a target write failure restores the updated source and absence"
    (let [compiled (extraction/compile-extraction request)
          failing-io (memory-io {(:file request) source} 2)
          result (extraction/commit! compiled failing-io)]
      (is (= :extraction-write-failed (:error-type result)))
      (is (:rolled-back result))
      (is (= {(:file request) source} @(:state failing-io)))))
  (testing "source drift and an appeared target refuse before mutation"
    (let [compiled (extraction/compile-extraction request)
          drifted-io (memory-io {(:file request) "changed"} nil)
          occupied-io (memory-io {(:file request) source
                                  (:to request) "occupied"} nil)]
      (is (= :source-hash-mismatch
             (:error-type (extraction/commit! compiled drifted-io))))
      (is (= :target-already-exists
             (:error-type (extraction/commit! compiled occupied-io)))))))

(deftest refuses-when-a-discovered-caller-is-neither-changed-nor-ignored
  (let [caller-file "src/sample/caller.clj"
        caller-source
        "(ns sample.caller)\n(defn call-it [x] (sample.core/helper x))\n"
        result
        (extraction/compile-extraction
          (-> request
              (dissoc :public-forms :caller-changes :ignored-caller-files)
              (assoc :workspace-sources {caller-file caller-source})))]
    (is (= :extraction-decisions-required (:error-type result)))
    (is (= [caller-file] (:files result)))
    (is (= false (:mutation-attempted result)))
    (is (= false (:write-authority result)))
    (is (= [{:decision :caller-disposition
             :file caller-file
             :source-hash (get-in result [:genuine-unknowns 0 :source-hash])}]
           (:genuine-unknowns result)))
    (let [source-hash (get-in result [:genuine-unknowns 0 :source-hash])]
      (is (and (string? source-hash)
               (re-matches #"[0-9a-f]{64}" source-hash))))
    (is (= [caller-file]
           (get-in result [:completed-plan :callers-to-review])))
    (is (:source-unchanged result))))

;; @spec MCP-OP-PLAN-008
;; @spec MCP-OP-PLAN-010
(deftest derives-only-omitted-required-visibility-inside-the-frozen-plan
  (let [private-source
        (str "(ns sample.core)\n\n"
             "(defn- helper [value] (inc value))\n\n"
             "(defn keep-me [] (helper 1))\n")
        automatic
        (extraction/compile-extraction
          (-> request
              (dissoc :public-forms)
              (assoc :source private-source
                     :workspace-sources {})))
        explicit
        (extraction/compile-extraction
          (-> request
              (assoc :public-forms ["helper"]
                     :source private-source
                     :workspace-sources {})))
        explicit-empty
        (extraction/compile-extraction
          (-> request
              (assoc :public-forms []
                     :source private-source
                     :workspace-sources {})))]
    (is (:ok automatic) (pr-str automatic))
    (is (:ok explicit) (pr-str explicit))
    (is (= (select-keys automatic
                        [:future-sources :form-count :caller-edit-count])
           (select-keys explicit
                        [:future-sources :form-count :caller-edit-count])))
    (is (.contains ^String (get-in automatic
                                   [:future-sources "src/sample/moved.clj"])
                   "(defn helper "))
    (is (= :required-public-forms-missing
           (:error-type explicit-empty)))
    (is (= ["helper"] (:missing-public-forms explicit-empty)))))
