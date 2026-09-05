(ns clj-surgeon.helper-extraction-test
  "RED witnesses for `helper_extraction` (selected-helper closure extraction).

  THIS NAMESPACE DOES NOT LOAD YET, AND THAT IS THE POINT. It requires
  `clj-surgeon.helper-extraction` (the pure planner) and
  `clj-surgeon.mcp-helper-extraction` (the I/O boundary and registration),
  neither of which exists. Running it today fails at load with

      Could not locate clj_surgeon/helper_extraction__init.class ...

  which is the executable statement of the gaps the EARS registry marks `[ ]`
  in docs/intent/helper-extraction/helper-extraction-specs.md. Registration
  lands in the same change as these witnesses so the intent audit never sees
  an unwitnessed active gap; they go green in the GREEN phase, not before.
  Until then the namespace is `excluded` from every JVM gate lane and is run
  by `make helper-extraction-red`, the repository's existing pattern for a
  not-yet-implemented witness.

  CONTRACT OF RECORD: docs/plans/helper-closure-extraction.md revision 3, the
  EARS registry above, and the design document's `Planner and boundary
  surfaces` section, which fixes the shapes these witnesses bind to:

    (helper/plan request sources)
      => {:ok true  :plan {:destination {:file :source}
                           :files [{:file :partition :alias :sites :edits}]
                           :transactions [{:changes [{:kind \"extraction\"} ...]}]}
                    :receipt {...counts and histograms only...}}
      => {:ok false :error_type \"helper-extraction-...\" :next_call nil ...}

  Revision 3 and Astra's 05:07Z corrections, as they bear on these witnesses:

    - `next_call` is nil on EVERY v1 refusal (010/016);
    - `expect.caller_files` is OPTIONAL (017);
    - a moved -> retained-PUBLIC reference REFUSES (019), conservatively;
    - a namespace-sensitive form in a moved body REFUSES, full stop (018);
    - partitions are moved-only / mixed / qualified-only / untouched (006),
      and a qualified-only caller GAINS a require of the destination so the
      rewritten qualified symbol has a load path of its own;
    - `scope.paths` is a write-authorization subset of the admitted roots, and
      a supported reference outside it REFUSES (021);
    - verification coverage is TYPED, never a bare count (022).

  The oracle is `clj-surgeon.helper-extraction-fixture`, which renders PRE and
  canonical POST from one description, so no assertion here is fed by the
  planner under test."
  {:lane :excluded}
  (:require
   [clj-surgeon.helper-extraction :as helper]
   [clj-surgeon.helper-extraction-fixture :as fixture]
   [clj-surgeon.mcp-helper-extraction :as mcp-helper]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

;; ---------------------------------------------------------------------------
;; harness

(defn- plan-of
  "The planner's answer for one fixture variant. The planner is pure: it is
  handed the sources it may read."
  ([variant] (plan-of variant {}))
  ([variant overrides]
   (helper/plan (fixture/request overrides) (fixture/sources variant))))

(defn- refused?
  [plan error-type]
  (and (false? (:ok plan)) (= error-type (:error_type plan))))

(defn- plan-files [plan] (get-in plan [:plan :files]))
(defn- plan-destination [plan] (get-in plan [:plan :destination]))
(defn- plan-transactions [plan] (get-in plan [:plan :transactions]))

(defn- entry-for
  [plan file]
  (some #(when (= file (:file %)) %) (plan-files plan)))

(defn- apply-edits
  [source edits]
  (reduce (fn [text {:keys [original replacement]}]
            (when-not (str/includes? text original)
              (throw (ex-info "Planned original bytes are absent from the source"
                              {:original original})))
            (str/replace-first text original replacement))
          source
          edits))

(defn- extracted-tree
  "Apply one plan to the pre-extraction corpus and return the whole tree,
  including the file the plan says it creates."
  [plan]
  (let [pre (into {} (keep (fn [{:keys [file pre]}] (when pre [file pre])))
                  (fixture/files :happy))
        rewritten (reduce (fn [tree {:keys [file edits]}]
                            (assoc tree file (apply-edits (get tree file) edits)))
                          pre
                          (plan-files plan))]
    (assoc rewritten
           (:file (plan-destination plan))
           (:source (plan-destination plan)))))

(defn- canonical
  [file]
  (some #(when (= file (:file %)) (:post %)) (fixture/files :happy)))

(defn- pre-source
  [variant file]
  (some #(when (= file (:file %)) (:pre %)) (fixture/files variant)))

;; ---------------------------------------------------------------------------
;; request and registration

;; @spec MCP-OP-HELPER-001
(deftest the-server-advertises-helper-extraction-with-a-closed-field-set
  (let [tool (mcp-helper/tool)]
    (is (= "helper_extraction" (:name tool)))
    (is (= #{"op" "workspace_root" "from" "helpers" "to" "scope"
             "verification" "expect"}
           (set (keys (get-in tool [:inputSchema :properties]))))
        "the input schema is closed: no per-file, per-owner or per-site field")
    (is (not (contains? (set (get-in tool [:inputSchema :required])) "expect"))
        "MCP-OP-HELPER-017: expect is optional")))

;; @spec MCP-OP-HELPER-002
;; @spec MCP-OP-HELPER-025
(deftest a-request-carrying-a-per-caller-table-refuses-as-an-unknown-field
  (testing "the request's size must be constant in the number of callers"
    (doseq [table [{:caller_files [{:file "src/acid/app/m01.clj" :alias "response"}]}
                   {:callers {"src/acid/app/m01.clj" {:sites 3}}}
                   {:sites [{:file "src/acid/app/x01.clj" :count 2}]}]]
      (testing (pr-str (first (keys table)))
        (let [plan (plan-of :happy table)]
          (is (refused? plan "helper-extraction-unknown-field") (pr-str plan))
          (is (= (mapv name (keys table)) (:unknown_fields plan)))
          (is (nil? (:next_call plan))))))))

;; @spec MCP-OP-HELPER-017
(deftest expect-caller-files-is-optional-and-its-absence-is-accepted
  (let [plan (plan-of :happy)]
    (is (nil? (:expect (fixture/request)))
        "the fixture's normal problem-to-done request carries no expect")
    (is (:ok plan) (pr-str plan))))

;; @spec MCP-OP-HELPER-013
;; @spec MCP-OP-HELPER-017
(deftest a-supplied-and-wrong-expect-refuses-with-both-counts-and-no-next-call
  (let [plan (plan-of :happy {:expect {:caller_files 99}})]
    (is (refused? plan "helper-extraction-expect-mismatch") (pr-str plan))
    (is (= (:caller-files fixture/canonical-counts) (:derived_caller_files plan)))
    (is (= 99 (:expected_caller_files plan)))
    (is (nil? (:next_call plan))
        "revision 3: a stateless request carries no old count; no continuation")
    (is (not (contains? plan :expect_revised))
        "expect_revised was REMOVED in revision 3")))

;; @spec MCP-OP-HELPER-013
;; @spec MCP-OP-HELPER-017
(deftest a-supplied-and-correct-expect-is-accepted
  (is (:ok (plan-of :happy {:expect {:caller_files (:caller-files
                                                    fixture/canonical-counts)}}))))

;; ---------------------------------------------------------------------------
;; owners and dependencies

;; @spec MCP-OP-HELPER-003
(deftest declare-plus-defn-of-one-selected-name-refuses-ambiguous-owner
  (let [plan (plan-of :ambiguous-owner)]
    (is (refused? plan "helper-extraction-ambiguous-owner") (pr-str plan))
    (is (= "json-response" (:helper plan)))
    (is (= #{"declare" "defn"} (set (map :kind (:owners plan))))
        "the refusal names every owner found, by kind")
    (is (every? int? (map :line (:owners plan))))
    (is (nil? (:next_call plan))
        "revision 3 removed the positional-owner continuation from v1; a
         mutually recursive selected pair, which needs `declare`, is out of
         v1 scope and lands here")))

;; @spec MCP-OP-HELPER-004
(deftest a-moved-helper-calling-a-retained-private-var-refuses
  (let [plan (plan-of :private-dependency)]
    (is (refused? plan "helper-extraction-private-dependency") (pr-str plan))
    (is (= "acid.web.http/header-date" (:var plan)))
    (is (= "see-other" (:helper plan)))
    (is (nil? (:next_call plan))
        "adding a var to the selection is the caller's decision, not the server's")))

;; @spec MCP-OP-HELPER-019
(deftest a-moved-helper-referencing-a-retained-public-var-refuses
  (let [plan (plan-of :retained-dependency-direct)]
    (is (refused? plan "helper-extraction-retained-dependency") (pr-str plan))
    (is (= "acid.web.http/strong-etag" (:var plan)))
    (is (= "with-etag" (:helper plan)))
    (is (nil? (:next_call plan)))))

;; @spec MCP-OP-HELPER-019
(deftest the-chain-fixtures-original-loads-and-the-retained-edge-still-refuses
  (testing "THE ORIGINAL IS VALID. The source requires C; C reaches a selected
            helper fully qualified with no require of the source, so the PRE
            require graph carries no edge back and the tree loads."
    (let [graph (fixture/static-require-graph :retained-dependency-chain :pre)]
      (is (contains? (get graph "acid.web.http") "acid.app.c01")
          "the source requires C, which is what makes the shape reachable")
      (is (empty? (get graph "acid.app.c01"))
          "C requires nothing: it reaches the helper fully qualified")
      (is (empty? (fixture/cyclic-namespaces graph))
          "the fixture's own description shows the original tree loads")))
  (testing "and the plan still refuses at the moved -> retained-public edge,
            which is where this shape is stopped"
    (let [plan (plan-of :retained-dependency-chain)]
      (is (refused? plan "helper-extraction-retained-dependency") (pr-str plan))
      (is (= "acid.web.http/strong-etag" (:var plan)))
      (is (nil? (:next_call plan))))))

;; @spec MCP-OP-HELPER-018
(deftest a-namespace-sensitive-form-in-a-moved-body-refuses
  (testing "(defn json-response [data] ... ::ok) moved verbatim would return
            :acid.web.response/ok instead of :acid.web.http/ok and would still
            compile. v1 refuses rather than rewriting: a rewrite of any kind
            fails this witness."
    (let [plan (plan-of :namespace-sensitive)]
      (is (false? (:ok plan))
          "no plan is an admissible answer for a namespace-sensitive body")
      (is (refused? plan "helper-extraction-namespace-sensitive-body")
          (pr-str plan))
      (is (= "json-response" (:helper plan)))
      (is (= "::ok" (:form plan)))
      (is (nil? (plan-destination plan)) "nothing is written, so nothing is rewritten")
      (is (nil? (:next_call plan))))))

;; @spec MCP-OP-HELPER-019
(deftest a-first-class-reference-is-a-dependency-and-a-site
  (testing "m03 passes a selected helper as an argument, not at head position"
    (let [plan (plan-of :happy)
          entry (entry-for plan "src/acid/app/m03.clj")]
      (is (some? entry) "the first-class caller is in the footprint")
      (is (= 2 (:sites entry)))
      (is (= (canonical "src/acid/app/m03.clj")
             (get (extracted-tree plan) "src/acid/app/m03.clj"))))))

;; @spec MCP-OP-HELPER-003
(deftest the-closure-moves-the-directed-chain-of-selected-helpers-together
  (testing "html-response calls with-etag and json-response calls
            html-response: a selected owner's peer references are rewritten to
            the destination's own symbols, and no peer is left behind"
    (let [plan (plan-of :happy)
          destination (:source (plan-destination plan))]
      (is (= (set fixture/helpers) (set (get-in plan [:plan :moved])))
          "every selected owner, and only those, are in the moved group")
      (doseq [helper-name fixture/helpers]
        (testing helper-name
          (is (str/includes? destination (str "(defn " helper-name "\n")))))
      (is (str/includes? destination "(with-etag {:status 200")
          "a moved -> moved reference is the destination's own bare symbol")
      (is (not (str/includes? destination "acid.web.http"))
          "this plan's destination requires nothing of the source, because
           every moved -> retained edge would have refused"))))

;; ---------------------------------------------------------------------------
;; discovery, partition and aliases

;; @spec MCP-OP-HELPER-006
(deftest the-caller-partition-equals-the-fixtures-canonical-partition
  (let [plan (plan-of :happy)]
    (is (:ok plan) (pr-str plan))
    (is (= fixture/canonical-receipt-partition (get-in plan [:receipt :partition]))
        "8 moved_only, 20 mixed, 1 qualified_only, 3 untouched")
    (is (= (:caller-files fixture/canonical-counts)
           (get-in plan [:receipt :caller_files]))
        "the source is counted ONCE in the footprint (MCP-OP-HELPER-015)")))

;; @spec MCP-OP-HELPER-005
(deftest every-declared-site-is-discovered-and-no-retained-site-is-touched
  (let [plan (plan-of :happy)]
    (is (= (:sites fixture/canonical-counts) (get-in plan [:receipt :sites])))
    (is (= (:retained-sites fixture/canonical-counts)
           (get-in plan [:receipt :retained_sites])))
    (doseq [{:keys [file sites]} (fixture/files :happy)
            :when (pos? sites)]
      (testing file
        (is (= sites (:sites (entry-for plan file)))
            "per-file site count equals the fixture's own description")))))

;; @spec MCP-OP-HELPER-006
(deftest a-mixed-caller-keeps-its-old-require-and-gains-exactly-one-new-one
  (let [tree (extracted-tree (plan-of :happy))]
    (doseq [{:keys [file partition]} (fixture/files :happy)
            :when (= :mixed partition)]
      (testing file
        (let [text (get tree file)]
          (is (str/includes? text "[acid.web.http :as http]")
              "the whole library require is NEVER replaced in a mixed caller")
          (is (str/includes? text "[acid.web.response :as response]"))
          (is (str/includes? text "(http/parse-json-body request)")
              "the retained use is unchanged"))))))

;; @spec MCP-OP-HELPER-006
(deftest an-untouched-caller-is-not-in-the-footprint-and-gains-no-authority
  (let [plan (plan-of :happy)
        planned (set (map :file (plan-files plan)))]
    (doseq [{:keys [file partition]} (fixture/files :happy)
            :when (= :untouched partition)]
      (testing file
        (is (not (contains? planned file))
            "requiring the source without referencing a selected owner is not
             a mutation licence")))))

;; @spec MCP-OP-HELPER-014
;; @spec MCP-OP-HELPER-006
(deftest a-fully-qualified-caller-without-a-require-gains-a-load-path
  (let [plan (plan-of :happy)
        entry (entry-for plan "src/acid/app/fq01.clj")
        tree (extracted-tree plan)
        after (get tree "src/acid/app/fq01.clj")]
    (testing "the ORIGINAL is valid but only BOOTSTRAP-loads: it requires
              nothing, so its qualified symbol resolves because something else
              in the program loaded the source first"
      (let [before (pre-source :happy "src/acid/app/fq01.clj")]
        (is (not (str/includes? before ":require"))
            "no require of the source, which is what makes it qualified-only")
        (is (str/includes? before "(acid.web.http/json-response data)"))
        (is (empty? (fixture/cyclic-namespaces
                     (fixture/static-require-graph :happy :pre)))
            "the pre tree loads")))
    (testing "discovered, and its own partition class"
      (is (some? entry) "discovered even though it never requires the source")
      (is (= "qualified_only" (:partition entry)))
      (is (nil? (:alias entry)) "no alias is chosen for a qualified-only caller"))
    (testing "after the write the caller LOADS STANDALONE: the rewritten
              qualified destination symbol has a require of its own"
      (is (= (canonical "src/acid/app/fq01.clj") after))
      (is (str/includes? after ":require"))
      (is (str/includes? after "acid.web.response")
          "never a bare qualified symbol with no require")
      (is (str/includes? after "(acid.web.response/json-response data)"))
      (is (empty? (fixture/cyclic-namespaces
                   (fixture/static-require-graph :happy :post)))
          "and the post tree loads"))))

;; @spec MCP-OP-HELPER-015
(deftest the-source-local-use-is-lowered-by-the-extraction-itself
  (let [plan (plan-of :happy)
        tree (extracted-tree plan)
        source (get tree fixture/source-file)]
    (is (= (canonical fixture/source-file) source))
    (is (str/includes? source "(response/text-response \"ok\")")
        "the retained handle-health now calls the destination")
    (is (str/includes? source "[acid.web.response :as response]")
        "one require of the destination is added to the source")
    (is (= 1 (count (filter #(= fixture/source-file (:file %)) (plan-files plan))))
        "the source is both mutation subject and caller, and appears ONCE")
    (is (not-any? #(str/includes? source (str "(defn " % "\n")) fixture/helpers)
        "the definitions are RETIRED, so a passing load proves the callers
         were rewritten rather than that the old definitions survived")))

;; @spec MCP-OP-HELPER-007
(deftest the-alias-is-the-first-policy-entry-that-collides-with-nothing
  (let [plan (plan-of :happy)
        tree (extracted-tree plan)]
    (is (= (:alias-histogram fixture/canonical-counts)
           (into (sorted-map) (get-in plan [:receipt :alias_histogram]))))
    (testing "m04 binds a local named `response`, so `resp` is chosen"
      (is (= "resp" (:alias (entry-for plan "src/acid/app/m04.clj"))))
      (is (= (canonical "src/acid/app/m04.clj") (get tree "src/acid/app/m04.clj"))))))

;; @spec MCP-OP-HELPER-007
(deftest a-caller-that-binds-every-policy-entry-refuses
  (let [plan (plan-of :alias-policy-exhausted)]
    (is (refused? plan "helper-extraction-alias-policy-exhausted") (pr-str plan))
    (is (= "src/acid/app/ae01.clj" (:file plan)))
    (is (= fixture/alias-policy (:collided_bindings plan)))
    (is (nil? (:next_call plan))
        "MCP-OP-HELPER-016: the server never invents an alias")))

;; @spec MCP-OP-HELPER-023
(deftest a-bare-symbol-referred-from-two-namespaces-refuses
  (let [plan (plan-of :ambiguous-reference)]
    (is (refused? plan "helper-extraction-ambiguous-reference") (pr-str plan))
    (is (= "src/acid/app/ar01.clj" (:file plan)))
    (is (= ["acid.web.http/json-response" "acid.web.mirror/json-response"]
           (:candidates plan)))
    (is (nil? (:next_call plan)))))

;; @spec MCP-OP-HELPER-005
(deftest a-prefix-list-libspec-refuses-as-an-unsupported-binding
  (let [plan (plan-of :unsupported-binding)]
    (is (refused? plan "helper-extraction-unsupported-binding") (pr-str plan))
    (is (= "src/acid/app/pl01.clj" (:file plan)))
    (is (str/includes? (:form plan) "acid.web"))
    (is (nil? (:next_call plan))
        "excluding the caller is not offered: retiring the definitions with an
         unrewritten caller left behind is the failure this verb prevents")))

;; @spec MCP-OP-HELPER-021
(deftest a-supported-reference-outside-the-authorized-scope-refuses
  (testing "test/ is an admitted discovery root; scope.paths (src/**) is only
            the WRITE authorization, so a caller there is found and refused"
    (let [plan (plan-of :caller-outside-scope)]
      (is (refused? plan "helper-extraction-caller-outside-scope") (pr-str plan))
      (is (= ["test/acid/app/o01_test.clj"] (:files_outside_scope plan)))
      (is (= fixture/admitted-roots (:admitted_roots plan)))
      (is (nil? (:next_call plan))
          "widening scope is the caller's decision; narrowing is never offered"))))

;; ---------------------------------------------------------------------------
;; canonical bytes

;; @spec MCP-OP-HELPER-006
;; @spec MCP-OP-HELPER-015
(deftest every-changed-file-equals-the-canonical-post-tree
  (let [plan (plan-of :happy)
        tree (extracted-tree plan)]
    (doseq [{:keys [file post partition]} (fixture/files :happy)
            :when (contains? #{:moved-only :mixed :qualified-only
                               :source :destination}
                             partition)]
      (testing file
        (is (= post (get tree file)))))))

;; @spec MCP-OP-HELPER-005
(deftest every-protected-decoy-region-survives-byte-identically
  (let [tree (extracted-tree (plan-of :happy))]
    (doseq [[file regions] (fixture/protected-regions :happy)
            {:keys [region sha256]} regions]
      (testing (str file " :: " region)
        (is (str/includes? (get tree file) region))
        (is (= sha256 (fixture/sha256 region)))))))

;; ---------------------------------------------------------------------------
;; transaction, proof and terminal states

;; @spec MCP-OP-HELPER-008
(deftest the-write-is-one-transaction-and-a-refusal-stages-nothing
  (let [plan (plan-of :happy)]
    (is (= 1 (count (plan-transactions plan)))
        "one guarded transaction through the shared kernel entrance")
    (is (= 1 (count (filter #(= "extraction" (:kind %))
                            (:changes (first (plan-transactions plan))))))
        "source retirement and destination creation are ONE typed extraction
         change, not a second overlapping caller edit"))
  (doseq [variant [:private-dependency :ambiguous-owner :unsupported-binding
                   :retained-dependency-direct]]
    (testing variant
      (let [plan (plan-of variant)]
        (is (false? (:ok plan)))
        (is (true? (:source_unchanged plan)))
        (is (true? (:target_unchanged plan)))
        (is (nil? (plan-transactions plan))
            "a refusal before staging plans no write")))))

;; @spec MCP-OP-HELPER-011
(deftest a-profile-that-cannot-run-refuses-before-anything-is-staged
  (let [plan (plan-of :happy {:verification {:profile "no-such-profile"}})]
    (is (refused? plan "helper-extraction-verification-preflight-unavailable")
        (pr-str plan))
    (is (= "no-such-profile" (:profile plan)))
    (is (true? (:source_unchanged plan)))
    (is (nil? (plan-transactions plan)) "nothing staged")
    (is (nil? (:next_call plan))
        "MCP-OP-HELPER-016: a weaker profile is never suggested")))

;; @spec MCP-OP-HELPER-011
(deftest only-synchronous-rollback-capable-profiles-are-admitted
  (is (seq (mcp-helper/admitted-profiles)))
  (is (every? (fn [profile]
                (and (:synchronous? profile) (:rollback-capable? profile)))
              (vals (mcp-helper/admitted-profiles)))
      "capability is validated BEFORE writing, not discovered afterwards"))

;; @spec MCP-OP-HELPER-020
(deftest the-four-terminal-states-are-distinct-and-rollback-failed-never-claims-unchanged
  (is (= #{:committed :verification-failed :verification-timeout :rollback-failed}
         (set (mcp-helper/terminal-states))))
  (testing "a handled failure restores every protected byte and mode"
    (doseq [state [:verification-failed :verification-timeout]]
      (testing state
        (let [receipt (mcp-helper/terminal-receipt state {})]
          (is (= (name state) (:status receipt)))
          (is (false? (:committed receipt)))
          (is (true? (:restored receipt)))
          (is (true? (:source_unchanged receipt)))
          (is (false? (:destination_created receipt))
              "the destination is removed on a handled failure")
          (is (some? (:restoration_read_back receipt)))))))
  (testing "a failed restoration is never reported as unchanged"
    (let [receipt (mcp-helper/terminal-receipt :rollback-failed {})]
      (is (= "rollback-failed" (:status receipt)))
      (is (false? (:committed receipt)))
      (is (false? (:restored receipt)))
      (is (false? (:source_unchanged receipt))
          "MCP-OP-HELPER-020: it NEVER claims unchanged")
      (is (seq (:files receipt)) "it names the files")
      (is (some? (:recovery_required receipt))
          "the kernel's recovery-required evidence is carried through")))
  (testing "only a committed receipt claims a completed proof"
    (let [receipt (mcp-helper/terminal-receipt :committed {})]
      (is (true? (:committed receipt)))
      (is (= "complete" (get-in receipt [:verification :status]))))))

;; @spec MCP-OP-HELPER-022
(deftest the-proof-is-typed-and-runs-in-a-fresh-process-never-a-bare-count
  (let [receipt (mcp-helper/terminal-receipt :committed {})
        verification (:verification receipt)]
    (testing "the executed profile names itself and its typed checks"
      (is (= "helper-proof" (:profile verification)))
      (is (= "complete" (:status verification)))
      (is (= (:caller-files fixture/canonical-counts)
             (:structural_callers verification))
          "the structural check covers the declared caller set")
      (is (pos-int? (:helper_behaviors verification))
          "behavior is run, not merely parsed")
      (is (int? (:compiled_callers verification)))
      (is (true? (:ok verification))))
    (testing "an ambiguous coverage integer is not a typed check"
      (is (not (contains? verification :covered_callers))
          "a bare covered_callers integer cannot say WHAT was covered"))
    (testing "a compile claim must be backed by compiles that happened"
      (is (or (zero? (:compiled_callers verification))
              (= (:compiled_callers verification)
                 (count (:compiled_evidence verification))))
          "claiming compiled callers without the evidence of each compile is
           the false green this witness exists to prevent"))
    (testing "and the proof cannot be manufactured by a warm namespace"
      (is (true? (:fresh_process verification))
          "stale Vars in an already-loaded source would otherwise satisfy a
           proof after the definitions were retired"))))

;; ---------------------------------------------------------------------------
;; the receipt

;; @spec MCP-OP-HELPER-009
;; @spec MCP-OP-HELPER-012
(deftest the-receipt-carries-counts-and-histograms-and-never-a-file-list
  (let [receipt (:receipt (plan-of :happy))]
    (is (= (:helpers fixture/canonical-counts) (:helpers receipt)))
    (is (= (:source-retired fixture/canonical-counts) (:source_retired receipt)))
    (is (true? (:destination_created receipt)))
    (is (= (:caller-files fixture/canonical-counts) (:caller_files receipt)))
    (is (= fixture/canonical-receipt-partition (:partition receipt)))
    (is (= (:sites fixture/canonical-counts) (:sites receipt)))
    (is (= (:retained-sites fixture/canonical-counts) (:retained_sites receipt)))
    (is (= (:alias-histogram fixture/canonical-counts)
           (into (sorted-map) (:alias_histogram receipt))))
    (is (string? (:details_path receipt))
        "per-caller detail goes to details_path, not into the receipt")
    (testing "no value anywhere in the receipt is a list of files"
      (let [values (tree-seq coll? seq receipt)
            file-ish (fn [v] (and (string? v) (re-find #"\.cljc?$" v)))]
        (is (not-any? (fn [v] (and (sequential? v) (some file-ish v))) values)
            (pr-str receipt))))
    (testing "MCP-OP-HELPER-012: closure states its roots and its grammar"
      (is (= fixture/admitted-roots (get-in receipt [:closure :roots])))
      (is (= fixture/scope-paths (get-in receipt [:closure :authorized_paths])))
      (is (= "supported-libspecs-only" (get-in receipt [:closure :grammar])))
      (is (= "not-claimed" (get-in receipt [:closure :dynamic_references]))))))

;; ---------------------------------------------------------------------------
;; the refusal matrix: each type exactly once, none of them a continuation

(def refusal-matrix
  "Every v1 refusal type, mapped to the ONE fixture that produces it. The map
  is the exactly-once proof: a duplicated key cannot exist, and a type with no
  entry fails `the-declared-refusal-set-is-complete`."
  {"helper-extraction-ambiguous-owner"       #(plan-of :ambiguous-owner)
   "helper-extraction-private-dependency"    #(plan-of :private-dependency)
   "helper-extraction-retained-dependency"   #(plan-of :retained-dependency-direct)
   "helper-extraction-namespace-sensitive-body" #(plan-of :namespace-sensitive)
   "helper-extraction-unsupported-binding"   #(plan-of :unsupported-binding)
   "helper-extraction-ambiguous-reference"   #(plan-of :ambiguous-reference)
   "helper-extraction-alias-policy-exhausted" #(plan-of :alias-policy-exhausted)
   "helper-extraction-caller-outside-scope"  #(plan-of :caller-outside-scope)
   "helper-extraction-target-exists"         #(plan-of :target-exists)
   "helper-extraction-expect-mismatch"       #(plan-of :happy {:expect {:caller_files 99}})
   "helper-extraction-verification-preflight-unavailable"
   #(plan-of :happy {:verification {:profile "no-such-profile"}})
   "helper-extraction-unknown-field"
   #(plan-of :happy {:caller_files [{:file "src/acid/app/m01.clj"}]})})

;; @spec MCP-OP-HELPER-010
(deftest every-declared-refusal-type-is-produced-by-exactly-one-fixture
  (doseq [[error-type produce] refusal-matrix]
    (testing error-type
      (let [plan (produce)]
        (is (false? (:ok plan)) (pr-str plan))
        (is (= error-type (:error_type plan)) (pr-str plan))))))

;; @spec MCP-OP-HELPER-010
(deftest the-declared-refusal-set-is-complete
  (is (= (set (keys refusal-matrix)) (set (helper/refusal-types)))
      "a refusal type the planner can emit but no witness exercises, or a
       witness for a type the planner does not declare, is a finding"))

;; @spec MCP-OP-HELPER-010
;; @spec MCP-OP-HELPER-016
(deftest no-refusal-offers-a-continuation-or-a-way-to-narrow-the-problem
  (doseq [[error-type produce] refusal-matrix]
    (testing error-type
      (let [plan (produce)]
        (is (contains? plan :next_call)
            "the field is present and explicitly null, never merely absent")
        (is (nil? (:next_call plan))
            "v1 knows no schema-valid, scope-preserving, non-identical
             continuation for any refusal")
        (is (true? (:source_unchanged plan)))
        (is (true? (:target_unchanged plan)))
        (testing "and nothing in the refusal is an escape hatch"
          (let [text (pr-str plan)]
            (is (not (str/includes? text "exclude"))
                "no caller exclusion")
            (is (not (re-find #"(?i)narrow" text))
                "no scope narrowing")
            (is (not (re-find #"(?i)fallback|weaker|degrade" text))
                "no weaker verification profile")))
        (is (some? (:decision plan))
            "each refusal names the ONE unresolved decision")))))

;; @spec MCP-OP-HELPER-024
;; @spec MCP-OP-HELPER-016
(deftest the-target-exists-refusal-never-invents-a-destination
  (let [plan (plan-of :target-exists)]
    (is (refused? plan "helper-extraction-target-exists") (pr-str plan))
    (is (= fixture/dest-lib (:lib plan)))
    (is (= fixture/dest-file (:file plan)))
    (is (nil? (:next_call plan))
        "choosing a different destination is the caller's decision")))
