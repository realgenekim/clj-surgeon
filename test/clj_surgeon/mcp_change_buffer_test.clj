(ns clj-surgeon.mcp-change-buffer-test
  (:require
   [clj-surgeon.intent-transaction :as transaction]
   [clj-surgeon.mcp-change-buffer :as change-buffer]
   [clj-surgeon.mcp-formatter :as formatter]
   [clj-surgeon.mcp-process :as process-env]
   [clj-surgeon.quoted-var-refs :as quoted-var-refs]
   [clj-surgeon.structural-lens :as structural-lens]
   [clj-surgeon.tmp-leak-support :as tmp-leak]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing use-fixtures]]))

;; RATCHET (2026-09-04, inb-9483a4): `temp-project` below is called by ~27
;; deftests and none deleted its directory -- 56 of the historical 82,210
;; leaked Anvil /tmp entries were `clj-surgeon-change-buffer-*` from this
;; namespace. Track every root it creates and sweep them after each test.
(def ^:private temp-roots (atom []))
(use-fixtures :each (tmp-leak/tracking-temp-dir-fixture temp-roots))

(def core-source
  (str "(ns sample.core)\n"
       "(defn target [x] (inc x))\n"
       "(defn caller [x]\n"
       "  ;; target here is only a comment\n"
       "  (target x))\n"))

(def test-source
  (str "(ns sample.core-test)\n"
       "(deftest target-test\n"
       "  (is (= 2 (target 1))))\n"))

(defn- temp-project
  []
  (let [root (tmp-leak/track!
               temp-roots
               (.toFile (java.nio.file.Files/createTempDirectory
                          "clj-surgeon-change-buffer-"
                          (make-array java.nio.file.attribute.FileAttribute 0))))
        source (io/file root "src/sample/core.clj")
        test-file (io/file root "test/sample/core_test.clj")]
    (.mkdirs (.getParentFile source))
    (.mkdirs (.getParentFile test-file))
    (spit source core-source)
    (spit test-file test-source)
    {:root root :source source :test-file test-file}))

(defn- semantic-result
  [{:keys [source test-file]}]
  (let [session "lsp-test-session"
        source-path (.getCanonicalPath source)
        test-path (.getCanonicalPath test-file)
        source-hash (structural-lens/source-hash (slurp source))
        test-hash (structural-lens/source-hash (slurp test-file))]
    {:ok true
     :version 2
     :lsp_session session
     :definition {:lsp_session session
                  :file "src/sample/core.clj"
                  :file_path source-path
                  :source_sha256 source-hash
                  :owner "target"
                  :range {:start {:line 1 :character 6}
                          :end {:line 1 :character 12}}
                  :line 2 :character 7 :name "target"}
     :references
     [{:lsp_session session
       :file "src/sample/core.clj"
       :file_path source-path
       :source_sha256 source-hash
       :owner "target"
       :owner_details {:name "target" :start_line 2}
       :range {:start {:line 1 :character 6}
               :end {:line 1 :character 12}}
       :line 2 :character 7}
      {:lsp_session session
       :file "src/sample/core.clj"
       :file_path source-path
       :source_sha256 source-hash
       :owner "caller"
       :owner_details {:name "caller" :start_line 3}
       :range {:start {:line 4 :character 3}
               :end {:line 4 :character 9}}
       :line 5 :character 4}
      {:lsp_session session
       :file "test/sample/core_test.clj"
       :file_path test-path
       :source_sha256 test-hash
       :owner "target-test"
       :owner_details {:name "target-test" :start_line 2}
       :range {:start {:line 2 :character 12}
               :end {:line 2 :character 18}}
       :line 3 :character 13}]}))

(defn- version-3-semantic-result
  [{:keys [source]} {:keys [reference-line provider-owner owner-status]
                     :or {reference-line 4 owner-status "unresolved"}}]
  (let [session "lsp-v3-test-session"
        source-path (.getCanonicalPath source)
        source-hash (structural-lens/source-hash (slurp source))]
    {:ok true
     :version 3
     :lsp_session session
     :definition {:lsp_session session
                  :file "src/sample/core.clj"
                  :file_path source-path
                  :source_sha256 source-hash
                  :owner_status "found"
                  :owner "target"
                  :range {:start {:line 1 :character 6}
                          :end {:line 1 :character 12}}
                  :line 2 :character 7 :name "target"}
     :references
     [{:lsp_session session
       :file "src/sample/core.clj"
       :file_path source-path
       :source_sha256 source-hash
       :owner_status owner-status
       :owner provider-owner
       :owner_details (when provider-owner
                        {:name provider-owner :start_line (dec reference-line)})
       :range {:start {:line (dec reference-line) :character 3}
               :end {:line (dec reference-line) :character 9}}
       :line reference-line :character 4}]}))

(defn- prepare!
  [project]
  (change-buffer/prepare-change!
    {:project-root (:root project)
     :semantic-resolver (fn [_] (semantic-result project))}
    {:subject "sample.core/target"
     :intent "Rename target to compute-target without touching its test call yet"
     :verify "fast"}))

(deftest addresses-the-smallest-complete-form-at-a-semantic-position
  (let [selected (transaction/addressed-form-at core-source
                                                {:line 5 :character 4})]
    (is (= "(target x)" (:before selected)))
    (is (vector? (:path selected)))
    (is (nat-int? (get-in selected [:address :preorder])))))

(deftest prepare-captures-each-file-once-and-emits-an-executable-next-call
  (change-buffer/clear-bases!)
  (let [project (temp-project)
        reads (atom [])
        result (change-buffer/prepare-change!
                 {:project-root (:root project)
                  :semantic-resolver (fn [_] (semantic-result project))
                  :read-source (fn [file]
                                 (swap! reads conj file)
                                 (slurp file))}
                 {:subject "sample.core/target"
                  :intent "Rename the Var and selected calls"
                  :verify "fast"})]
    (is (:ok result))
    (is (= 2 (:file-count result)))
    (is (= 3 (:site-count result)))
    (is (= 1 (count (filter #(= :definition (:role %))
                            (:decision-sites result)))))
    (is (= 2 (count @reads)))
    (is (= #{{:site "change/s01" :replace nil}
             {:site "change/s02" :replace nil}
             {:site "change/s03" :replace nil}}
           (set (get-in result [:next_call :decisions]))))
    (is (some #(and (= "caller" (:form %))
                    (.contains ^String (:source %) "only a comment")
                    (.startsWith ^String (:source %) "(defn caller"))
              (:decision-sites result)))
    (is (some #(and (= "target-test" (:form %))
                    (.startsWith ^String (:source %) "(deftest target-test"))
              (:decision-sites result)))))

(deftest basis-request-shape-refuses-mixed-routes-and-unknown-decision-fields
  (is (= {:ok true}
         (change-buffer/validate-basis-request
           {:basis "cb-1"
            :decisions [{:site "s1" :keep true}]
            :verify "fast"})))
  (let [mixed (change-buffer/validate-basis-request
                {:basis "cb-1" :decisions [] :changes []})
        nested (change-buffer/validate-basis-request
                 {:basis "cb-1"
                  :decisions [{:site "s1" :keep true :unexpected true}]})
        nested-edit (change-buffer/validate-basis-request
                      {:basis "cb-1"
                       :decisions
                       [{:site "s1"
                         :edit {:find "(inc x)"
                                :replace "(dec x)"
                                :unexpected true}}]})]
    (is (= :invalid-mcp-request (:error-type mixed)))
    (is (= ["changes"] (:unknown-fields mixed)))
    (is (:source-unchanged mixed))
    (is (= :invalid-mcp-request (:error-type nested)))
    (is (= ["decisions[0].unexpected"] (:unknown-fields nested)))
    (is (:source-unchanged nested))
    (is (= :invalid-mcp-request (:error-type nested-edit)))
    (is (= ["decisions[0].edit.unexpected"]
           (:unknown-fields nested-edit)))
    (is (:source-unchanged nested-edit))))

(deftest apply-uses-the-retained-basis-once-and-preserves-kept-sites
  (change-buffer/clear-bases!)
  (let [project (temp-project)
        prepared (prepare! project)
        sites-by-form (into {} (map (juxt :form :id) (:decision-sites prepared)))
        decisions [{:site (sites-by-form "target")
                    :replace "(defn compute-target [x] (inc x))"}
                   {:site (sites-by-form "caller")
                    :replace (str "(defn caller [x]\n"
                                  "  ;; target here is only a comment\n"
                                  "  (compute-target x))")}
                   {:site (sites-by-form "target-test") :keep true}]
        verified (atom 0)
        formatted (atom 0)
        result (change-buffer/apply-basis!
                 {:project-root (:root project)
                  :receipt-dir (io/file (:root project) "receipts")
                  :prepare-compiled!
                  (fn [_ compiled]
                    (swap! formatted inc)
                    (assoc compiled :format
                           {:status :complete
                            :file-count 2
                            :changed-file-count 0}))
                  :verify! (fn [_ profile _ files]
                             (swap! verified inc)
                             {:ok true :profile profile :files files})}
                 {:basis (:basis prepared)
                  :decisions decisions
                  :verify "fast"})]
    (is (:ok result))
    (is (= 1 @verified))
    (is (= 1 @formatted))
    (is (= :complete (get-in result [:format :status])))
    (is (.exists (io/file (:receipt-file result))))
    (is (= (str "(ns sample.core)\n"
                "(defn compute-target [x] (inc x))\n"
                "(defn caller [x]\n"
                "  ;; target here is only a comment\n"
                "  (compute-target x))\n")
           (slurp (:source project))))
    (is (= test-source (slurp (:test-file project))))))

(deftest prepared-basis-compilation-is-pure-deterministic-and-budget-closed
  (let [source (str "(ns sample.core)\n"
                    "(defn target [x] (inc x))\n"
                    "(defn caller [x] (target x))\n")
        captured {:ok true
                  :sources {"src/sample/core.clj" source}
                  :relative {"src/sample/core.clj" "src/sample/core.clj"}
                  :source-character-count (count source)}
        locations
        [{:file_path "src/sample/core.clj"
          :role :reference
          :owner "caller"
          :subject "sample.core/target"
          :range {:start {:line 2 :character 17}
                  :end {:line 2 :character 23}}}
         {:file_path "src/sample/core.clj"
          :role :definition
          :owner "target"
          :subject "sample.core/target"
          :range {:start {:line 1 :character 6}
                  :end {:line 1 :character 12}}}]
        input {:project-root "/workspace"
               :bundle {:lsp-session "lsp-fixed"
                        :quoted-var-proof {:reference-count 0}}
               :subjects ["sample.core/target"]
               :intent "Rename target"
               :verify "fast"
               :scope "surface"
               :label "rename"
               :surface-locations locations
               :reference-count 1
               :captured captured
               :basis-id "cb-fixed"
               :created-ms 1000}
        first-result (change-buffer/compile-prepared-basis input)
        second-result (change-buffer/compile-prepared-basis
                        (update input :surface-locations reverse))]
    (change-buffer/clear-bases!)
    (is (= first-result second-result)
        "semantic input order cannot change structural site order")
    (is (:ok first-result))
    (is (= "cb-fixed" (get-in first-result [:basis :id])))
    (is (= 1000 (get-in first-result [:basis :created-ms])))
    (is (= ["rename/s01" "rename/s02"]
           (get-in first-result [:response :decision-site-ids])))
    (is (= ["target" "caller"]
           (mapv :form (get-in first-result [:response :decision-sites]))))
    (is (= [{:site "rename/s01" :replace nil}
            {:site "rename/s02" :replace nil}]
           (get-in first-result [:response :next_call :decisions])))
    (is (zero? (change-buffer/retained-basis-count))
        "pure compilation cannot publish into the global basis store")

    (testing "definition scope changes the viewport, not the complete surface"
      (let [result (change-buffer/compile-prepared-basis
                     (assoc input :scope "definition"))]
        (is (= 2 (get-in result [:response :surface-site-count])))
        (is (= 1 (get-in result [:response :site-count])))
        (is (= ["rename/s01"]
               (get-in result [:response :decision-site-ids])))))

    (testing "each closed budget refuses as data"
      (doseq [limits [{:sites 1
                       :visible-characters 1000
                       :snapshot-characters 1000}
                      {:sites 10
                       :visible-characters 1
                       :snapshot-characters 1000}
                      {:sites 10
                       :visible-characters 1000
                       :snapshot-characters 1}]]
        (let [result (change-buffer/compile-prepared-basis
                       (assoc input :limits limits))]
          (is (false? (:ok result)))
          (is (= :change-buffer-budget-exceeded (:error-type result)))
          (is (= limits (:limits result)))
          (is (true? (:source-unchanged result))))))))

(deftest definition-scope-returns-the-complete-surface-and-one-decision-viewport
  (change-buffer/clear-bases!)
  (let [project (temp-project)
        reads (atom [])
        prepared
        (change-buffer/prepare-change!
          {:project-root (:root project)
           :semantic-resolver (fn [_] (semantic-result project))
           :read-source (fn [file]
                          (swap! reads conj file)
                          (slurp file))}
          {:subject "sample.core/target"
           :intent "Change only the target implementation"
           :label "rename"
           :scope "definition"
           :verify "fast"})]
    (is (:ok prepared))
    (is (= "definition" (:scope prepared)))
    (is (= 1 (:site-count prepared)))
    (is (= 3 (:surface-site-count prepared)))
    (is (= 2 (:reference-count prepared)))
    (is (= 3 (:surface-location-count prepared)))
    (is (= 2 (:file-count prepared)))
    (is (= 2 (count @reads))
        "the full proof reads each distinct file once")
    (is (= ["rename/s01" "rename/s02" "rename/s03"]
           (mapv :id (:surface prepared))))
    (is (= [:definition :reference :reference]
           (mapv :role (:surface prepared))))
    (is (= ["target" "caller" "target-test"]
           (mapv :form (:surface prepared))))
    (is (every? #(not (contains? % :source)) (:surface prepared))
        "the complete quickfix-style surface contains no source bodies")
    (is (= ["rename/s01"] (:decision-site-ids prepared)))
    (is (= ["target"] (mapv :form (:decision-sites prepared))))
    (is (= ["(defn target [x] (inc x))"]
           (mapv :source (:decision-sites prepared))))
    (is (= [{:site "rename/s01" :replace nil}]
           (get-in prepared [:next_call :decisions])))))

(deftest prepare-change-unions-quoted-var-callers-with-honest-authority
  (change-buffer/clear-bases!)
  (let [project (temp-project)
        quoted-source
        (str "(ns sample.core-test\n"
             "  (:require [sample.core :as sut]))\n"
             "(deftest aliased-private-var\n"
             "  (is (var? #'sut/target)))\n"
             "(deftest fully-qualified-private-var\n"
             "  (is (var? (var sample.core/target))))\n"
             "(def text \"#'sample.core/target\")\n"
             ";; #'sample.core/target\n"
             "(def quoted-data '(var sample.core/target))\n")
        _ (spit (:test-file project) quoted-source)
        semantic (update (semantic-result project) :references subvec 0 2)
        result
        (change-buffer/prepare-change!
          {:project-root (:root project)
           :semantic-resolver (constantly semantic)
           :structural-reference-resolver
           #(quoted-var-refs/scan-workspace (:root project) % slurp)}
          {:subject "sample.core/target"
           :intent "Prove every ordinary and Var-quoted caller"
           :label "quoted"
           :scope "surface"
           :verify "fast"})]
    (is (:ok result))
    (is (= 2 (get-in result [:quoted_var_proof :reference-count])))
    (is (= ["target" "caller" "aliased-private-var"
            "fully-qualified-private-var"]
           (mapv :form (:surface result))))
    (is (= [:language-server+exact-source
            :language-server+exact-source
            :structural-var-quote
            :structural-var-quote]
           (mapv :authority (:surface result))))
    (is (not-any? #{"text" "quoted-data"}
                  (map :form (:surface result))))))

(deftest retained-structural-buffer-selection-is-pure-ordered-and-fail-closed
  (let [basis {:id "cb-test"
               :subjects ["sample.core/target"]
               :surface-sites
               [{:id "rename/s01"
                 :role :definition
                 :relative-file "src/sample/core.clj"
                 :owner "target"
                 :line 2
                 :end-line 2
                 :subjects #{"sample.core/target"}
                 :owner-authority :language-server+exact-source
                 :before "(defn target [x] (inc x))"}
                {:id "rename/s02"
                 :role :reference
                 :relative-file "src/sample/core.clj"
                 :owner "caller"
                 :line 3
                 :end-line 5
                 :subjects #{"sample.core/target"}
                 :owner-authority :language-server+exact-source
                 :before "(defn caller [x] (target x))"}]}
        selected (change-buffer/select-basis-buffers
                   basis ["rename/s02" "rename/s01"] "form")]
    (is (:ok selected))
    (is (= ["rename/s02" "rename/s01"]
           (mapv :id (:buffers selected))))
    (is (= ["caller" "target"] (mapv :form (:buffers selected))))
    (is (= ["(defn caller [x] (target x))"
            "(defn target [x] (inc x))"]
           (mapv :source (:buffers selected))))
    (is (= 2 (:buffer-count selected)))
    (is (= :invalid-buffer-selection
           (:error-type
             (change-buffer/select-basis-buffers basis [] "form"))))
    (is (= :invalid-buffer-selection
           (:error-type
             (change-buffer/select-basis-buffers
               basis ["rename/s01" "rename/s01"] "form"))))
    (is (= :unknown-buffer-site
           (:error-type
             (change-buffer/select-basis-buffers
               basis ["rename/s99"] "form"))))
    (is (= :unsupported-buffer-context
           (:error-type
             (change-buffer/select-basis-buffers
               basis ["rename/s01"] "parent"))))))

(deftest retained-structural-buffers-use-the-frozen-basis-without-a-disk-reread
  (change-buffer/clear-bases!)
  (let [project (temp-project)
        prepared (change-buffer/prepare-change!
                   {:project-root (:root project)
                    :semantic-resolver (fn [_] (semantic-result project))}
                   {:subject "sample.core/target"
                    :intent "Inspect one retained caller"
                    :label "rename"
                    :scope "definition"})]
    (spit (:source project) "(ns sample.core)\n;; changed after preparation\n")
    (let [opened (change-buffer/open-basis-sites!
                   (:root project) (:basis prepared)
                   ["rename/s02"] "form")
          wrong-workspace (temp-project)]
      (is (:ok opened))
      (is (= 0 (:file-read-count opened 0)))
      (is (= ["caller"] (mapv :form (:buffers opened))))
      (is (= ["(defn caller [x]\n  ;; target here is only a comment\n  (target x))"]
             (mapv :source (:buffers opened))))
      (is (= :basis-workspace-mismatch
             (:error-type
               (change-buffer/open-basis-sites!
                 (:root wrong-workspace) (:basis prepared)
                 ["rename/s02"] "form")))))))

(deftest retained-basis-apply-refuses-a-different-workspace-before-write
  (change-buffer/clear-bases!)
  (let [prepared-project (temp-project)
        other-project (temp-project)
        prepared (prepare! prepared-project)
        receipt-dir (io/file (:root other-project) "receipts")
        result (change-buffer/apply-basis!
                 {:project-root (:root other-project)
                  :receipt-dir receipt-dir
                  :verify! (fn [& _]
                             (throw (ex-info "verification must not run" {})))}
                 {:basis (:basis prepared)
                  :decisions []
                  :verify "fast"})]
    (is (= :basis-workspace-mismatch (:error-type result)))
    (is (:source-unchanged result))
    (is (= core-source (slurp (:source prepared-project))))
    (is (= core-source (slurp (:source other-project))))
    (is (not (.exists receipt-dir)))
    (is (= 1 (change-buffer/retained-basis-count))
        "a wrong-root request cannot consume the prepared basis")))

(deftest prepare-change-refuses-an-unknown-scope-without-a-basis
  (change-buffer/clear-bases!)
  (let [project (temp-project)
        result
        (change-buffer/prepare-change!
          {:project-root (:root project)
           :semantic-resolver (fn [_]
                                (throw (ex-info "must not resolve" {})))}
          {:subject "sample.core/target"
           :intent "Do something"
           :scope "callers-only"})]
    (is (= :invalid-change-scope (:error-type result)))
    (is (:source-unchanged result))
    (is (empty? @change-buffer/basis-store))))

(deftest compact-basis-edits-compile-without-an-owner-sized-replacement
  (change-buffer/clear-bases!)
  (let [project (temp-project)
        prepared (prepare! project)
        sites-by-form (into {} (map (juxt :form identity)
                                    (:decision-sites prepared)))
        target-site (sites-by-form "target")
        decisions [{:site (:id target-site)
                    :edit {:find "(inc x)" :replace "(dec x)"}}
                   {:site (:id (sites-by-form "caller")) :keep true}
                   {:site (:id (sites-by-form "target-test")) :keep true}]
        result (change-buffer/apply-basis!
                 {:project-root (:root project)
                  :receipt-dir (io/file (:root project) "receipts")
                  :verify! (fn [_ profile _ files]
                             {:ok true :profile profile :files files})}
                 {:basis (:basis prepared)
                  :decisions decisions
                  :verify "fast"})]
    (is (:ok result))
    (testing "compact decisions carry intent instead of complete owner source" (is (= #{:site :edit} (set (keys (first decisions))))) (is (= {:find "(inc x)" :replace "(dec x)"} (:edit (first decisions)))) (is (not (contains? (first decisions) :replace))))
    (is (= (str "(ns sample.core)\n"
                "(defn target [x] (dec x))\n"
                "(defn caller [x]\n"
                "  ;; target here is only a comment\n"
                "  (target x))\n")
           (slurp (:source project))))))

(deftest compact-basis-delete-owns-comments-delimiters-and-refusals
  (testing "delete removes one exact child and its contiguous leading comment"
    (change-buffer/clear-bases!)
    (let [project (temp-project)
          prepared (prepare! project)
          sites-by-form (into {} (map (juxt :form identity)
                                      (:decision-sites prepared)))
          result (change-buffer/apply-basis!
                   {:project-root (:root project)
                    :receipt-dir (io/file (:root project) "receipts")
                    :verify! (fn [_ profile _ files]
                               {:ok true :profile profile :files files})}
                   {:basis (:basis prepared)
                    :decisions
                    [{:site (:id (sites-by-form "target")) :keep true}
                     {:site (:id (sites-by-form "caller"))
                      :edit {:find "(target x)" :delete true}}
                     {:site (:id (sites-by-form "target-test")) :keep true}]
                    :verify "fast"})]
      (is (:ok result))
      (is (= (str "(ns sample.core)\n"
                  "(defn target [x] (inc x))\n"
                  "(defn caller [x])\n")
             (slurp (:source project))))))
  (doseq [[label edit expected]
          [["ambiguous" {:find "x" :delete true} :ambiguous-match]
           ["owner" {:find "(defn target [x] (inc x))" :delete true}
            :basis-edit-covers-owner]]]
    (testing (str label " delete refuses before write")
      (change-buffer/clear-bases!)
      (let [project (temp-project)
            prepared (prepare! project)
            sites-by-form (into {} (map (juxt :form identity)
                                        (:decision-sites prepared)))
            result (change-buffer/apply-basis!
                     {:project-root (:root project)
                      :verify! (fn [& _] {:ok true})}
                     {:basis (:basis prepared)
                      :decisions
                      [{:site (:id (sites-by-form "target")) :edit edit}
                       {:site (:id (sites-by-form "caller")) :keep true}
                       {:site (:id (sites-by-form "target-test")) :keep true}]
                      :verify "fast"})]
        (is (= expected (:error-type result)))
        (is (:source-unchanged result))
        (is (= core-source (slurp (:source project))))))))

(deftest whole-site-delete-compiles-with-neighboring-edits-and-durable-undo
  (change-buffer/clear-bases!)
  (let [project (temp-project)
        prepared (prepare! project)
        sites-by-form (into {} (map (juxt :form identity)
                                    (:decision-sites prepared)))
        result (change-buffer/apply-basis!
                 {:project-root (:root project)
                  :receipt-dir (io/file (:root project) "receipts")
                  :verify! (fn [_ profile _ files]
                             {:ok true :profile profile :files files})}
                 {:basis (:basis prepared)
                  :decisions
                  [{:site (:id (sites-by-form "target")) :delete true}
                   {:site (:id (sites-by-form "caller"))
                    :edit {:find "(target x)" :replace "(fallback x)"}}
                   {:site (:id (sites-by-form "target-test")) :delete true}]
                  :verify "fast"})
        receipt (when (:ok result)
                  (read-string (slurp (:receipt-file result))))
        inverse (when receipt
                  (transaction/compile-inverse
                    receipt
                    {(.getCanonicalPath (:source project))
                     (slurp (:source project))
                     (.getCanonicalPath (:test-file project))
                     (slurp (:test-file project))}))]
    (is (:ok result))
    (is (= (str "(ns sample.core)\n"
                "(defn caller [x]\n"
                "  ;; target here is only a comment\n"
                "  (fallback x))\n")
           (slurp (:source project))))
    (is (= "(ns sample.core-test)\n" (slurp (:test-file project))))
    (is (:ok inverse))
    (is (= core-source
           (get (:future-sources inverse)
                (.getCanonicalPath (:source project)))))
    (is (= test-source
           (get (:future-sources inverse)
                (.getCanonicalPath (:test-file project)))))))

(deftest stale-source-refuses-before-any-write
  (change-buffer/clear-bases!)
  (let [project (temp-project)
        prepared (prepare! project)
        first-site (first (:decision-sites prepared))
        original-test (slurp (:test-file project))]
    (spit (:source project) (str core-source "; concurrent\n"))
    (let [result (change-buffer/apply-basis!
                   {:project-root (:root project)
                    :receipt-dir (io/file (:root project) "receipts")
                    :verify! (fn [& _] {:ok true})}
                   {:basis (:basis prepared)
                    :decisions (mapv (fn [{:keys [id source]}]
                                       (if (= id (:id first-site))
                                         {:site id :replace
                                          (str/replace-first source "target" "stale-target")}
                                         {:site id :keep true}))
                                     (:decision-sites prepared))})]
      (is (= :source-hash-mismatch (:error-type result)))
      (is (= original-test (slurp (:test-file project)))))))

(deftest failed-verification-rolls-back-every-addressed-edit
  (change-buffer/clear-bases!)
  (let [project (temp-project)
        prepared (prepare! project)
        decisions (mapv (fn [{:keys [id form]}]
                          (if (= form "caller")
                            {:site id
                             :replace (str "(defn caller [x]\n"
                                           "  ;; target here is only a comment\n"
                                           "  (compute-target x))")}
                            {:site id :keep true}))
                        (:decision-sites prepared))
        result (change-buffer/apply-basis!
                 {:project-root (:root project)
                  :receipt-dir (io/file (:root project) "receipts")
                  :verify! (fn [& _] {:ok false :profile "fast" :exit 1})}
                 {:basis (:basis prepared) :decisions decisions})]
    (is (= :verification-failed (:error-type result)))
    (is (:rolled-back result))
    (is (= core-source (slurp (:source project))))
    (is (= test-source (slurp (:test-file project))))))

(deftest exact-profile-compilation-is-project-owned-and-snapshot-bound
  ;; @spec MCP-OP-VERIFY-001
  ;; @spec MCP-OP-VERIFY-002
  ;; @spec MCP-OP-VERIFY-003
  ;; @spec MCP-OP-VERIFY-004
  ;; @spec MCP-OP-VERIFY-005
  (let [profile {:acceptance :exact-exit
                 :timeout-ms 120000
                 :commands [["clj-kondo" "--lint" "src/app.clj"
                             "--fail-level" "error"]]}
        compiled (change-buffer/compile-exact-profile
                   "exact" {"exact" profile} :project)
        reordered (change-buffer/compile-exact-profile
                    "exact"
                    {"exact" (array-map
                               :commands (:commands profile)
                               :timeout-ms 120000
                               :acceptance :exact-exit)}
                    :project)]
    (is (:ok compiled) (pr-str compiled))
    (is (= :exact-exit (:acceptance compiled)))
    (is (= :project (:profile-source compiled)))
    (is (= 64 (count (:profile-sha256 compiled))))
    (is (= (:profile-sha256 compiled) (:profile-sha256 reordered)))
    ;; The resolution RULE, not a host path: compile-exact-profile must hand
    ;; the raw command through the project's own executable resolver
    ;; (expand-command), and that resolver must land on an absolute,
    ;; existing, executable file actually named "clj-kondo" -- wherever this
    ;; host happens to keep it (Homebrew on macOS, /usr/local/bin on Linux,
    ;; etc). The expectation is computed from the resolver itself, never
    ;; typed, so this assertion is host-independent.
    (let [resolved-argv (:argv compiled)
          resolved-clj-kondo (first resolved-argv)
          resolved-file (io/file resolved-clj-kondo)]
      (is (= (change-buffer/expand-command (first (:commands profile)) [])
             resolved-argv)
          "compiled argv must equal this host's own resolver output for the same command")
      (is (= ["--lint" "src/app.clj" "--fail-level" "error"]
             (rest resolved-argv)))
      (is (.isAbsolute resolved-file)
          (str "resolved clj-kondo must be an absolute path, not a bare name: "
               resolved-clj-kondo))
      (is (and (.isFile resolved-file) (.canExecute resolved-file))
          (str "resolved clj-kondo must exist and be executable: " resolved-clj-kondo))
      (is (= "clj-kondo" (.getName resolved-file))))
    (is (= :exact-profile-not-project-owned
           (:error-type
             (change-buffer/compile-exact-profile
               "exact" {"exact" profile} :process))))))

(deftest expand-command-resolves-through-runtime-path-after-paved-directories
  ;; @spec MCP-OP-VERIFY-011
  ;; GHA round one (docs/observations/2026-09-04-gha-round1.md, finding 1): the
  ;; runner's clj-kondo lives on $PATH via setup-clojure's tool cache, in none
  ;; of the five hardcoded directories, and the seat only ever passed because
  ;; its own clj-kondo happens to sit in /usr/local/bin. Reproduce the runner
  ;; shape directly by binding *executable-path* to a directory that is NOT
  ;; one of the five paved ones.
  (testing "an executable absent from every paved directory resolves via PATH"
    ;; A fictitious name, not "clj-kondo": this seat's own clj-kondo already
    ;; lives in a paved directory (/usr/local/bin), which would find the
    ;; SEAT's binary there and mask the PATH-resolution branch under test --
    ;; exactly the class of ambient-precondition failure this fix exists for.
    (let [path-dir (tmp-leak/track!
                     temp-roots
                     (.toFile (java.nio.file.Files/createTempDirectory
                                "clj-surgeon-change-buffer-path-"
                                (make-array java.nio.file.attribute.FileAttribute 0))))
          executable-name "clj-surgeon-fake-tool-9483a4"
          fake-tool (io/file path-dir executable-name)]
      (spit fake-tool "#!/bin/sh\nexit 0\n")
      (.setExecutable fake-tool true)
      (binding [process-env/*executable-path* (.getPath path-dir)]
        (let [resolved (change-buffer/expand-command [executable-name "--lint"] [])]
          (is (= (.getCanonicalPath fake-tool)
                 (.getCanonicalPath (io/file (first resolved)))))
          (is (.isAbsolute (io/file (first resolved)))
              (str "resolved executable must be an absolute path, not a bare name: "
                   (first resolved)))
          (is (= ["--lint"] (rest resolved)))))))

  (testing "nothing resolves in either the paved directories or PATH: typed refusal"
    (let [empty-path-dir (tmp-leak/track!
                           temp-roots
                           (.toFile (java.nio.file.Files/createTempDirectory
                                      "clj-surgeon-change-buffer-empty-path-"
                                      (make-array java.nio.file.attribute.FileAttribute 0))))]
      (binding [process-env/*executable-path* (.getPath empty-path-dir)]
        (try
          (change-buffer/expand-command
            ["clj-kondo-nonexistent-binary-9483a4" "--lint"] [])
          (is false "expand-command must throw a typed refusal when nothing resolves")
          (catch clojure.lang.ExceptionInfo e
            (let [data (ex-data e)]
              (is (= :executable-unresolved (:error-type data)))
              (is (= "clj-kondo-nonexistent-binary-9483a4"
                     (:requested-executable data)))
              (is (some #(str/includes? % "/usr/local/bin")
                        (:searched-directories data))
                  "must name the paved directories searched")
              (is (some #{(.getPath empty-path-dir)} (:searched-directories data))
                  "must name the PATH directories searched"))))))))

(deftest the-resolver-refusal-is-translated-by-callers-that-own-a-typed-kind
  ;; @spec MCP-OP-VERIFY-011
  ;; The blast radius of the typed refusal, witnessed at the boundary rather
  ;; than at the throw. `expand-command` now REFUSES where it used to hand
  ;; back a bare name, and two callers return refusal MAPS as their whole
  ;; contract. Letting the throw escape them replaced an enumerated kind with
  ;; an unenumerated one: the full mcp-test suite went 4 failures / 2 errors,
  ;; including MCP-OP-ADMIT-133's "the entrance published kinds the
  ;; enumeration has never heard of: #{:executable-unresolved}".
  ;;
  ;; This assertion lives HERE, and not in `clj-surgeon.mcp-formatter-test`,
  ;; because that namespace is an orphan: no runner and no Make target loads
  ;; it, so a witness placed there would gate nothing. Reported, not fixed
  ;; here -- the lane manifest is the right home for that repair.
  (testing "the formatter translates it into its own formatter-failed kind"
    (let [empty-path-dir (tmp-leak/track!
                           temp-roots
                           (.toFile (java.nio.file.Files/createTempDirectory
                                      "clj-surgeon-formatter-empty-path-"
                                      (make-array java.nio.file.attribute.FileAttribute 0))))]
      (binding [process-env/*executable-path* (.getPath empty-path-dir)]
        (let [result (formatter/format-candidates!
                       (.getPath empty-path-dir)
                       ["clj-surgeon-absent-formatter-9483a4" "{files}"]
                       {"a.clj" "(ns a)"})]
          (is (false? (:ok result))
              "an unresolvable formatter is a refusal, never a thrown ex-info")
          (is (= :formatter-failed (:error-type result))
              "the kind stays the one this entry point already publishes")
          (is (true? (:source-unchanged result)))
          (is (str/includes? (str (:error result))
                             "clj-surgeon-absent-formatter-9483a4")
              "the refusal names the executable it could not resolve")
          (is (str/includes? (str (:remedy result)) (.getPath empty-path-dir))
              "and the remedy names the directories searched"))))))

(deftest exact-process-evidence-is-complete-bounded-and-honest
  ;; @spec MCP-OP-VERIFY-003
  ;; @spec MCP-OP-VERIFY-005
  ;; @spec MCP-OP-VERIFY-006
  ;; @spec MCP-OP-VERIFY-007
  ;; @spec MCP-OP-VERIFY-009
  ;; @spec MCP-OP-VERIFY-010
  (let [root (.getCanonicalPath (io/file "."))
        visible-limit 12000
        output (apply str (repeat (+ visible-limit 1000) "x"))
        pass-profile {:profile "exact"
                      :profile-source :project
                      :profile-sha256 (apply str (repeat 64 "a"))
                      :acceptance :exact-exit
                      :timeout-ms 120000
                      :argv ["/usr/bin/printf" "%s" output]}
        pass (change-buffer/run-exact-verification! root pass-profile)
        failed (change-buffer/run-exact-verification!
                 root (assoc pass-profile :argv ["/usr/bin/false"]))
        timeout (change-buffer/run-exact-verification!
                  root (assoc pass-profile
                              :timeout-ms 1
                              :argv ["/bin/sleep" "1"]))
        launch (change-buffer/run-exact-verification!
                 root (assoc pass-profile
                             :argv ["/definitely/missing-verifier"]))]
    (is (:ok pass) (pr-str pass))
    (is (= root (:cwd pass)))
    (is (= (:argv pass-profile) (:argv pass)))
    (is (= (count output) (:output-bytes pass)))
    (is (= 64 (count (:output-sha256 pass))))
    (is (:output-truncated pass))
    (is (not (contains? pass :diagnostics))
        "passing warning bodies stay out of model context")
    (is (= :verification-failed (:error-type failed)))
    (is (= :ordinary-nonzero (:process-outcome failed)))
    (is (= :verification-unverified (:error-type timeout)))
    (is (= :timeout (:process-outcome timeout)))
    (is (= :verification-unverified (:error-type launch)))
    (is (= :launch-failure (:process-outcome launch)))
    (is (= :crash-or-signal-style-exit
           (:process-outcome
             (change-buffer/classify-exact-process-outcome
               {:finished? true :exit 137}))))
    (is (not (contains?
               (change-buffer/classify-exact-process-outcome
                 {:finished? true :exit 137})
               :signal)))))

(deftest exact-verification-admission-timeout-is-unverified
  ;; @spec MCP-OP-ANALYZER-002
  ;; @spec MCP-OP-ANALYZER-004
  (with-redefs [process-env/run-bounded!
                (fn [_]
                  {:finished? true
                   :exit 75
                   :elapsed_ms 10.0
                   :output "clj-kondo-admission-timeout"
                   :output-bytes 27
                   :output-sha256 (apply str (repeat 64 "a"))
                   :output-truncated false
                   :admission {:status :admission-timeout
                               :error-type :clj-kondo-admission-timeout}})]
    (let [result (change-buffer/run-exact-verification!
                   (.getCanonicalPath (io/file "."))
                   {:profile "exact"
                    :profile-source :project
                    :profile-sha256 (apply str (repeat 64 "a"))
                    :acceptance :exact-exit
                    :timeout-ms 1000
                    :argv ["clj-kondo" "--lint" "must-not-launch"]})]
      (is (false? (:ok result)))
      (is (= :verification-unverified (:error-type result)))
      (is (= :admission-timeout (:process-outcome result)))
      (is (= :admission-timeout (get-in result [:admission :status])))
      (is (= :clj-kondo-admission-timeout
             (get-in result [:admission :error-type])))
      (is (str/includes? (:diagnostics result)
                         "clj-kondo-admission-timeout")))))

(deftest basis-coverage-and-provider-ambiguity-refuse-as-data
  (change-buffer/clear-bases!)
  (let [project (temp-project)
        prepared (prepare! project)
        incomplete (change-buffer/apply-basis!
                     {:project-root (:root project)}
                     {:basis (:basis prepared) :decisions []})
        ambiguous (change-buffer/prepare-change!
                    {:project-root (:root project)
                     :semantic-resolver (fn [_]
                                          {:ok false
                                           :error-type :semantic-provider-refusal
                                           :error "found 2"})}
                    {:subject "sample.core/target" :intent "Change it"})]
    (is (= :basis-coverage-mismatch (:error-type incomplete)))
    (is (:source-unchanged incomplete))
    (is (= :semantic-provider-refusal (:error-type ambiguous)))
    (is (:source-unchanged ambiguous))))

(deftest semantic-proof-contract-is-fail-closed
  (let [project (temp-project)
        complete (semantic-result project)
        cases [[:legacy (dissoc complete :version) :semantic-evidence-incomplete]
               [:missing-session (dissoc complete :lsp_session)
                :semantic-evidence-incomplete]
               [:mixed-sessions (assoc-in complete [:references 1 :lsp_session] "lsp-other")
                :semantic-session-drift]
               [:missing-hash (update complete :definition dissoc :source_sha256)
                :semantic-evidence-incomplete]
               [:missing-owner (update complete :definition dissoc :owner)
                :semantic-evidence-incomplete]
               [:missing-reference-owner
                (update-in complete [:references 1] dissoc :owner)
                :semantic-evidence-incomplete]
               [:missing-range (update complete :definition dissoc :range)
                :semantic-evidence-incomplete]
               [:v3-unresolved-definition
                (-> complete
                    (assoc :version 3)
                    (assoc-in [:definition :owner_status] "unresolved")
                    (assoc-in [:definition :owner] nil))
                :semantic-evidence-incomplete]]]
    (doseq [[label evidence expected] cases]
      (let [actual (change-buffer/validate-semantic-evidence evidence)]
        (is (= expected (:error-type actual)) (name label))
        (is (:source-unchanged actual) (name label))))))

(deftest version-3-owner-fallback-resolves-a-real-project-defining-macro
  ;; Derived from the field caller where clojure-lsp found a reference inside
  ;; (>defn post-card ...) but returned no enclosing document symbol.
  (change-buffer/clear-bases!)
  (let [project (temp-project)
        source (str "(ns sample.core)\n"
                    "(defn target [x] (inc x))\n"
                    "(>defn post-card [x]\n"
                    "  (target x))\n")
        _written (spit (:source project) source)
        result (change-buffer/prepare-change!
                 {:project-root (:root project)
                  :semantic-resolver
                  (constantly (version-3-semantic-result project {}))}
                 {:subject "sample.core/target"
                  :intent "Update the target and its project-macro caller"})]
    (is (:ok result))
    (is (= 2 (:site-count result)))
    (is (some #(and (= "post-card" (:form %))
                    (= :exact-source (:authority %))
                    (.startsWith ^String (:source %) "(>defn post-card"))
              (:decision-sites result)))
    (is (some #(and (= "target" (:form %))
                    (= :language-server+exact-source
                       (:authority %)))
              (:decision-sites result)))))

(deftest version-3-owner-fallback-refuses-owner-drift-and-ownerless-source
  (doseq [[label source options expected]
          [[:provider-drift
            (str "(ns sample.core)\n"
                 "(defn target [x] (inc x))\n"
                 "(>defn post-card [x]\n"
                 "  (target x))\n")
            {:provider-owner "wrong-owner" :owner-status "found"}
            :semantic-owner-drift]
           [:no-structural-owner
            (str "(ns sample.core)\n"
                 "(defn target [x] (inc x))\n"
                 "(target 1)\n")
            {:reference-line 3}
            :semantic-owner-not-found]]]
    (testing (name label)
      (change-buffer/clear-bases!)
      (let [project (temp-project)
            _written (spit (:source project) source)
            result (change-buffer/prepare-change!
                     {:project-root (:root project)
                      :semantic-resolver
                      (constantly
                        (version-3-semantic-result project options))}
                     {:subject "sample.core/target" :intent "Change it"})
            store (var-get
                    (ns-resolve 'clj-surgeon.mcp-change-buffer 'basis-store))]
        (is (= expected (:error-type result)))
        (is (:source-unchanged result))
        (is (nil? (:basis result)))
        (is (empty? @store))))))

(deftest semantic-source-drift-publishes-no-basis
  (change-buffer/clear-bases!)
  (let [project (temp-project)
        stale (assoc-in (semantic-result project)
                        [:definition :source_sha256]
                        (apply str (repeat 64 "b")))
        result (change-buffer/prepare-change!
                 {:project-root (:root project)
                  :semantic-resolver (constantly stale)}
                 {:subject "sample.core/target" :intent "Change it"})
        store (var-get (ns-resolve 'clj-surgeon.mcp-change-buffer 'basis-store))]
    (is (= :semantic-source-drift (:error-type result)))
    (is (= "src/sample/core.clj" (:file result)))
    (is (= (get-in stale [:definition :source_sha256]) (:provider-hash result)))
    (is (re-matches #"[0-9a-f]{64}" (:actual-hash result)))
    (is (:source-unchanged result))
    (is (nil? (:basis result)))
    (is (empty? @store))))

(deftest related-subjects-compile-to-one-deduplicated-proof-basis
  (change-buffer/clear-bases!)
  (let [project (temp-project)
        subjects ["sample.core/target" "sample.core/caller"]
        calls (atom [])
        result (change-buffer/prepare-change!
                 {:project-root (:root project)
                  :semantic-resolver (fn [subject]
                                       (swap! calls conj subject)
                                       (semantic-result project))}
                 {:subjects subjects
                  :intent "Rename the target and update its related caller"})]
    (is (:ok result))
    (is (= subjects @calls))
    (is (= subjects (:subjects result)))
    (is (nil? (:subject result)))
    (is (= "lsp-test-session" (:lsp_session result)))
    (is (= 3 (:site-count result)) "shared semantic locations are deduplicated")
    (is (every? #(= subjects (:subjects %)) (:decision-sites result)))
    (is (= 3 (count (get-in result [:next_call :decisions]))))))

(deftest related-subjects-refuse-across-lsp-sessions-without-a-basis
  (change-buffer/clear-bases!)
  (let [project (temp-project)
        change-session (fn [semantic session]
                         (-> semantic
                             (assoc :lsp_session session)
                             (assoc-in [:definition :lsp_session] session)
                             (update :references
                                     #(mapv (fn [reference]
                                              (assoc reference :lsp_session session))
                                            %))))
        result (change-buffer/prepare-change!
                 {:project-root (:root project)
                  :semantic-resolver
                  (fn [subject]
                    (cond-> (semantic-result project)
                      (= subject "sample.core/caller")
                      (change-session "lsp-other-session")))}
                 {:subjects ["sample.core/target" "sample.core/caller"]
                  :intent "Change both"})
        store (var-get (ns-resolve 'clj-surgeon.mcp-change-buffer 'basis-store))]
    (is (= :semantic-session-drift (:error-type result)))
    (is (= #{"lsp-test-session" "lsp-other-session"} (:lsp-sessions result)))
    (is (:source-unchanged result))
    (is (nil? (:basis result)))
    (is (empty? @store))))

(deftest budgets-and-decision-holes-refuse-without-changing-source
  (is (= (* 32 1024) change-buffer/max-visible-characters)
      "the measured field decision fits one bounded surface")
  (change-buffer/clear-bases!)
  (let [project (temp-project)
        budgeted (with-redefs [change-buffer/max-visible-characters 5]
                   (prepare! project))]
    (is (= :change-buffer-budget-exceeded (:error-type budgeted)))
    (is (= 5 (get-in budgeted [:limits :visible-characters])))
    (is (= core-source (slurp (:source project))))
    (let [prepared (prepare! project)
          sites (:decision-sites prepared)
          invalid-decisions
          (mapv (fn [{:keys [id]}]
                  (if (= id (:id (first sites)))
                    {:site id :keep true :replace "(identity :wrong)"}
                    {:site id :keep true}))
                sites)
          invalid (change-buffer/apply-basis!
                    {:project-root (:root project)}
                    {:basis (:basis prepared) :decisions invalid-decisions})
          conflicting-delete-decisions
          (mapv (fn [{:keys [id]}]
                  (if (= id (:id (first sites)))
                    {:site id :keep true :delete true}
                    {:site id :keep true}))
                sites)
          conflicting-delete
          (change-buffer/apply-basis!
            {:project-root (:root project)}
            {:basis (:basis prepared)
             :decisions conflicting-delete-decisions})
          unchanged-decisions
          (mapv (fn [{:keys [id source]}]
                  (if (= id (:id (first sites)))
                    {:site id :replace source}
                    {:site id :keep true}))
                sites)
          unchanged (change-buffer/apply-basis!
                      {:project-root (:root project)}
                      {:basis (:basis prepared) :decisions unchanged-decisions})]
      (is (= :invalid-basis-decision (:error-type invalid)))
      (is (:source-unchanged invalid))
      (is (= :invalid-basis-decision (:error-type conflicting-delete)))
      (is (:source-unchanged conflicting-delete))
      (is (= :unchanged-basis-decision (:error-type unchanged)))
      (is (:source-unchanged unchanged))
      (is (= core-source (slurp (:source project)))))))

(deftest exact-file-owner-preparation-needs-no-semantic-index
  (let [project (temp-project)
        file (io/file (:root project) "bench/unindexed_script.clj")
        source (str "(ns bench.unindexed-script)\n"
                    "(def numeric-fields [:wall-ms :input-tokens])\n")
        reads (atom [])
        semantic-calls (atom 0)]
    (try
      (.mkdirs (.getParentFile file))
      (spit file source)
      (let [prepared
            (change-buffer/prepare-change!
              {:project-root (:root project)
               :read-source (fn [path]
                              (swap! reads conj path)
                              (slurp path))
               :semantic-resolver (fn [_]
                                    (swap! semantic-calls inc)
                                    (throw (ex-info "must not run" {})))}
              {:file "bench/unindexed_script.clj"
               :form "numeric-fields"
               :intent "Add one metric"
               :label "metrics"})]
        (is (:ok prepared))
        (is (= 1 (count @reads)))
        (is (zero? @semantic-calls))
        (is (= "bench/unindexed_script.clj" (:file prepared)))
        (is (= "numeric-fields" (:form prepared)))
        (is (= :exact-source (:authority prepared)))
        (is (= 1 (:site-count prepared)))
        (is (= 0 (:reference-count prepared)))
        (is (= ["metrics/s01"] (:decision-site-ids prepared)))
        (is (= "numeric-fields" (get-in prepared [:decision-sites 0 :form])))
        (is (= :exact-source (get-in prepared [:decision-sites 0 :authority])))
        (is (= (:basis prepared) (get-in prepared [:next_call :basis]))))
      (finally
        (change-buffer/clear-bases!)))))

(deftest exact-file-owner-preparation-is-confined-unique-and-definition-only
  (let [project (temp-project)
        file (io/file (:root project) "bench/unindexed_script.clj")]
    (try
      (.mkdirs (.getParentFile file))
      (doseq [[label source request error-type]
              [[:missing
                "(ns bench.unindexed-script)\n(def present 1)\n"
                {:file "bench/unindexed_script.clj" :form "missing"
                 :intent "Change one form"}
                :exact-owner-not-found]
               [:ambiguous
                "(ns bench.unindexed-script)\n(def duplicate 1)\n(def duplicate 2)\n"
                {:file "bench/unindexed_script.clj" :form "duplicate"
                 :intent "Change one form"}
                :exact-owner-ambiguous]
               [:surface
                "(ns bench.unindexed-script)\n(def present 1)\n"
                {:file "bench/unindexed_script.clj" :form "present"
                 :scope "surface" :intent "Change one form"}
                :exact-owner-scope-unsupported]]]
        (testing (name label)
          (spit file source)
          (let [before (change-buffer/retained-basis-count)
                result (change-buffer/prepare-change!
                         {:project-root (:root project)} request)]
            (is (= error-type (:error-type result)))
            (is (:source-unchanged result))
            (is (= before (change-buffer/retained-basis-count))))))
      (let [outside (change-buffer/prepare-change!
                      {:project-root (:root project)}
                      {:file "../outside.clj" :form "x"
                       :intent "Change one form"})]
        (is (= :invalid-relative-source-path (:error-type outside)))
        (is (:source-unchanged outside)))
      (finally
        (change-buffer/clear-bases!)))))

(deftest exact-owner-batch-reads-each-file-once-and-compiles-one-basis
  (let [project (temp-project)
        first-file (io/file (:root project) "src/sample/first.clj")
        second-file (io/file (:root project) "src/sample/second.clj")
        reads (atom [])
        semantic-calls (atom 0)]
    (try
      (.mkdirs (.getParentFile first-file))
      (spit first-file
            "(ns sample.first)\n(defn alpha [] :a)\n(defn beta [] :b)\n")
      (spit second-file
            "(ns sample.second)\n(defn gamma [] :g)\n")
      (let [prepared
            (change-buffer/prepare-change!
              {:project-root (:root project)
               :read-source (fn [path]
                              (swap! reads conj path)
                              (slurp path))
               :semantic-resolver (fn [_]
                                    (swap! semantic-calls inc)
                                    (throw (ex-info "must not run" {})))}
              {:owners [{:file "src/sample/first.clj" :form "alpha"}
                        {:file "src/sample/first.clj" :form "beta"}
                        {:file "src/sample/second.clj" :form "gamma"}]
               :intent "Delete three obsolete owners"
               :label "delete"})]
        (is (:ok prepared))
        (is (zero? @semantic-calls))
        (is (= 2 (count @reads)))
        (is (= 2 (count (distinct @reads))))
        (is (= 2 (:file-count prepared)))
        (is (= 3 (:site-count prepared)))
        (is (= ["delete/s01" "delete/s02" "delete/s03"]
               (:decision-site-ids prepared)))
        (is (= ["alpha" "beta" "gamma"]
               (mapv :form (:decision-sites prepared))))
        (is (every? #(= :exact-source (:authority %)) (:surface prepared)))
        (is (= 1 (change-buffer/retained-basis-count))
            "provisional single-owner bases never escape"))
      (finally
        (change-buffer/clear-bases!)))))
