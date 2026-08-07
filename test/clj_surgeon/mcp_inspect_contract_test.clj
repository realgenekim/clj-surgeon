(ns clj-surgeon.mcp-inspect-contract-test
  (:require
   [clj-surgeon.mcp-inspect :as inspect]
   [clj-surgeon.structural-lens :as structural-lens]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(def forms-request
  {"requests"
   [{"id" "selected"
     "operation" "forms"
     "file" "src/example.clj"
     "forms" ["alpha" "beta"]
     "expect" {"forms" 2}}]
   "expect" {"requests" 1 "files" 1}})

(defn- java-json-containers
  [value]
  (if (System/getProperty "babashka.version")
    value
    ((requiring-resolve 'clj-surgeon.java-json-containers/convert) value)))

(defn- snapshot
  [file source]
  {:file file
   :source source
   :hash (structural-lens/source-hash source)})

(deftest validates-all-four-discriminated-request-variants
  (let [request
        {"requests"
         [{"id" "forms" "operation" "forms" "file" "src/a.clj"
           "forms" ["a"] "expect" {"forms" 1}}
          {"id" "outline" "operation" "outline" "file" "src/a.clj"}
          {"id" "match" "operation" "match" "file" "src/b.cljs"
           "match" "(send! _)" "inside" "dispatch"
           "expect" {"matches" 0}}
          {"id" "xray" "operation" "xray" "file" "src/c.cljc"
           "expression" "(-> (form 'settings) initializer)"}]
         "expect" {"requests" 4 "files" 3}}
        result (inspect/validate-inspect-params request)]
    (is (:ok result))
    (is (= ["forms" "outline" "match" "xray"]
           (mapv :operation (get-in result [:params :requests]))))
    (is (= 0 (get-in result [:params :requests 2 :expect :matches])))
    (is (= {:requests 4 :files 3} (get-in result [:params :expect])))))

(deftest accepts-real-java-sdk-containers
  (is (= (inspect/validate-inspect-params forms-request)
         (inspect/validate-inspect-params
           (java-json-containers forms-request)))))

(deftest rejects-schema-and-cardinality-errors-with-exact-paths
  (doseq [[label request reason path]
          [[:top-not-map [] :expected-object []]
           [:unknown-top (assoc forms-request "extra" true)
            :unknown-fields []]
           [:missing-top (dissoc forms-request "requests")
            :missing-fields []]
           [:empty-requests (assoc forms-request "requests" [])
            :non-empty-array ["requests"]]
           [:request-not-map (assoc forms-request "requests" [42])
            :expected-object ["requests" 0]]
           [:missing-id (update-in forms-request ["requests" 0] dissoc "id")
            :missing-fields ["requests" 0]]
           [:blank-id (assoc-in forms-request ["requests" 0 "id"] " ")
            :non-blank-string ["requests" 0 "id"]]
           [:duplicate-id
            (-> forms-request
                (assoc "requests" [(get-in forms-request ["requests" 0])
                                   (assoc (get-in forms-request ["requests" 0])
                                          "file" "src/b.clj")])
                (assoc "expect" {"requests" 2 "files" 2}))
            :duplicate-id ["requests" 1 "id"]]
           [:missing-operation
            (update-in forms-request ["requests" 0] dissoc "operation")
            :missing-fields ["requests" 0]]
           [:unknown-operation
            (assoc-in forms-request ["requests" 0 "operation"] "grep")
            :unknown-operation ["requests" 0 "operation"]]
           [:illegal-forms-field-on-outline
            (-> forms-request
                (assoc-in ["requests" 0 "operation"] "outline")
                (update-in ["requests" 0] dissoc "expect"))
            :unknown-fields ["requests" 0]]
           [:illegal-match-field-on-xray
            (-> forms-request
                (assoc "requests" [{"id" "x" "operation" "xray"
                                    "file" "src/example.clj"
                                    "expression" "(form 'a)"
                                    "match" "a"}]))
            :unknown-fields ["requests" 0]]
           [:empty-forms (assoc-in forms-request ["requests" 0 "forms"] [])
            :non-empty-array ["requests" 0 "forms"]]
           [:duplicate-form
            (assoc-in forms-request ["requests" 0 "forms"] ["alpha" "alpha"])
            :duplicate-form ["requests" 0 "forms" 1]]
           [:forms-expect-mismatch
            (assoc-in forms-request ["requests" 0 "expect" "forms"] 1)
            :request-expectation-mismatch ["requests" 0 "expect" "forms"]]
           [:aggregate-request-mismatch
            (assoc-in forms-request ["expect" "requests"] 2)
            :aggregate-expectation-mismatch ["expect" "requests"]]
           [:aggregate-file-mismatch
            (assoc-in forms-request ["expect" "files"] 2)
            :aggregate-expectation-mismatch ["expect" "files"]]
           [:match-expect-negative
            {"requests" [{"id" "m" "operation" "match"
                          "file" "src/a.clj" "match" "_"
                          "expect" {"matches" -1}}]
             "expect" {"requests" 1 "files" 1}}
            :non-negative-integer ["requests" 0 "expect" "matches"]]
           [:absolute-path
            (assoc-in forms-request ["requests" 0 "file"] "/tmp/a.clj")
            :invalid-relative-source-path ["requests" 0 "file"]]
           [:parent-path
            (assoc-in forms-request ["requests" 0 "file"] "../a.clj")
            :invalid-relative-source-path ["requests" 0 "file"]]
           [:unsupported-extension
            (assoc-in forms-request ["requests" 0 "file"] "src/a.txt")
            :invalid-relative-source-path ["requests" 0 "file"]]]]
    (testing (name label)
      (let [result (inspect/validate-inspect-params request)]
        (is (false? (:ok result)))
        (is (= "inspect_clojure" (:operation result)))
        (is (= reason (:reason result)))
        (is (= path (:path result)))))))

(deftest evaluates-ordered-exact-forms-with-source-fidelity
  (let [source (str "(ns example)\n\n"
                    "(defn alpha\n"
                    "  [x]\n"
                    "  ^{:doc \"λ\"} #(inc %, x))\n\n"
                    "(def beta\n"
                    "  ;; café\n"
                    "  [:a, :b])\n")
        params (get-in (inspect/validate-inspect-params forms-request)
                       [:params])
        result (inspect/evaluate-snapshots
                 params {"src/example.clj"
                         (snapshot "src/example.clj" source)})]
    (is (:ok result))
    (is (= ["alpha" "beta"]
           (mapv :name (get-in result [:results 0 :forms]))))
    (is (= (str "(defn alpha\n"
                "  [x]\n"
                "  ^{:doc \"λ\"} #(inc %, x))")
           (get-in result [:results 0 :forms 0 :source])))
    (is (= "(def beta\n  ;; café\n  [:a, :b])"
           (get-in result [:results 0 :forms 1 :source])))
    (is (= (structural-lens/source-hash source)
           (get-in result [:file_hashes "src/example.clj"])))
    (is (= 2 (get-in result [:results 0 :form_count])))
    (is (= "none" (:next_action result)))))

(deftest evaluates-outline-match-and-xray-in-request-order
  (let [source (str "(ns example)\n"
                    "(def settings {:alpha 1 :beta 2})\n"
                    "(defn dispatch []\n"
                    "  \"textual decoy: (send! _)\"\n"
                    "  (send! :real))\n")
        raw {"requests"
             [{"id" "outline" "operation" "outline"
               "file" "src/example.clj"}
              {"id" "match" "operation" "match"
               "file" "src/example.clj" "match" "(send! _)"
               "expect" {"matches" 1}}
              {"id" "xray" "operation" "xray"
               "file" "src/example.clj"
               "expression"
               "(-> (form 'settings) initializer (expect-count 1) (analyze (fn [[m]] (count m))))"}]
             "expect" {"requests" 3 "files" 1}}
        params (get-in (inspect/validate-inspect-params raw) [:params])
        result (inspect/evaluate-snapshots
                 params {"src/example.clj"
                         (snapshot "src/example.clj" source)})]
    (is (:ok result))
    (is (= ["outline" "match" "xray"] (mapv :id (:results result))))
    (is (= 2 (get-in result [:results 0 :outline :form_count])))
    (is (= 1 (get-in result [:results 1 :match_count])))
    (is (= "(send! :real)"
           (get-in result [:results 1 :matches 0 :source])))
    (is (= "dispatch" (get-in result [:results 1 :matches 0 :inside])))
    (is (= 2 (get-in result [:results 2 :value])))
    (let [summary (inspect/concise-summary (assoc result :elapsed_ms 1.0))]
      (is (str/includes? summary
                         "outline: 5 lines · 2 forms · first settings · last dispatch"))
      (is (str/includes? summary
                         "match: 1 match · [{\"inside\":\"dispatch\",\"source\":\"(send! :real)\"}]"))
      (is (str/includes? summary "xray: value 2")))))

(deftest refuses-the-complete-batch-on-form-or-match-failure
  (let [source "(ns example)\n(defn duplicate [] 1)\n(defn duplicate [] 2)\n"
        base-snapshot {"src/example.clj"
                       (snapshot "src/example.clj" source)}]
    (doseq [[label request expected]
            [[:ambiguous
              forms-request
              "batch-form-selection-failed"]
             [:missing
              (assoc-in forms-request ["requests" 0 "forms"]
                        ["missing" "beta"])
              "batch-form-selection-failed"]
             [:match-count
              {"requests" [{"id" "m" "operation" "match"
                            "file" "src/example.clj"
                            "match" "(defn _ _ _)"
                            "expect" {"matches" 1}}]
               "expect" {"requests" 1 "files" 1}}
              "inspect-cardinality-mismatch"]]]
      (testing (name label)
        (let [params (get-in (inspect/validate-inspect-params request) [:params])
              result (inspect/evaluate-snapshots params base-snapshot)]
          (is (false? (:ok result)))
          (is (= expected (:error_type result)))
          (is (nil? (:results result))))))))

(deftest output-budget-boundaries-are-inclusive-and-fail-closed
  (doseq [[label size limit ok?]
          [[:below 9 10 true]
           [:equal 10 10 true]
           [:above 11 10 false]]]
    (testing (name label)
      (let [result (inspect/enforce-output-budget
                     [{:id "x" :operation "forms"
                       :source_character_count size
                       :payload (apply str (repeat size "x"))}]
                     {:per-request-source limit
                      :per-request-result 1000
                      :aggregate-result 1000})]
        (is (= ok? (:ok result)))
        (when-not ok?
          (is (= "inspect-output-limit" (:error_type result)))))))
  (let [one {:id "a" :operation "outline" :source_character_count 0}
        per-request-size (inspect/json-character-count one)
        encoded-size (inspect/json-character-count [one])]
    (doseq [[label limit ok?]
            [[:per-request-below (inc per-request-size) true]
             [:per-request-equal per-request-size true]
             [:per-request-above (dec per-request-size) false]]]
      (testing (name label)
        (let [result (inspect/enforce-output-budget
                       [one] {:per-request-source 1000
                              :per-request-result limit
                              :aggregate-result 1000})]
          (is (= ok? (:ok result)))
          (when-not ok?
            (is (= "inspect-output-limit" (:error_type result)))))))
    (is (:ok (inspect/enforce-output-budget
               [one] {:per-request-source 1000
                      :per-request-result 1000
                      :aggregate-result encoded-size})))
    (is (= "inspect-output-limit"
           (:error_type
             (inspect/enforce-output-budget
               [one] {:per-request-source 1000
                      :per-request-result 1000
                      :aggregate-result (dec encoded-size)}))))))

(deftest summaries-are-stable-concise-and-source-free
  (let [result {:ok true :operation "inspect_clojure"
                :request_count 2 :file_count 1
                :results [{:operation "forms" :form_count 3}
                          {:operation "match" :match_count 2}]
                :source_character_count 1234 :elapsed_ms 12.5}
        summary (inspect/concise-summary result)]
    (is (= (str "inspect_clojure\n"
                "  2 requests · 1 file · 3 forms · 2 matches\n\n"
                "✓ all requests resolved\n"
                "✓ ordered snapshot\n"
                "✓ hashes attached\n"
                "✓ terminal evidence · read_complete=true · next action none\n"
                "  1,234 source characters · 12.50 ms")
           summary))
    (is (not (.contains summary "(defn")))))
