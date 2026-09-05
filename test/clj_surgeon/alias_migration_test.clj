(ns clj-surgeon.alias-migration-test
  (:require
   [clj-surgeon.alias-migration :as alias-migration]
   [clj-surgeon.alias-migration-fixture :as fixture]
   [clojure.set]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [rewrite-clj.node :as n]
   [rewrite-clj.parser :as parser]))

(def corpus (fixture/corpus))

(defn- request
  ([] (request {}))
  ([overrides]
   (merge {:workspace-root "/workspace"
           :from (get-in corpus [:manifest :from])
           :to (get-in corpus [:manifest :to])
           :scope {:paths ["src/**"]}
           :expect {:files 12}}
          overrides)))

(defn- sources
  "The planner is scope-blind: scope expansion is the MCP layer's job, so a
  src-scoped request is given exactly the src files."
  ([] (sources (into {} (filter #(str/starts-with? (key %) "src/")) (:pre corpus))))
  ([file-map] (mapv (fn [[file source]] {:file file :source source})
                    (sort file-map))))

(defn- plan-corpus
  ([] (plan-corpus (request) (sources)))
  ([request sources] (alias-migration/plan request sources)))

(defn- apply-edits
  [source edits]
  (reduce (fn [text {:keys [original replacement]}]
            (when-not (str/includes? text original)
              (throw (ex-info "Planned original bytes are absent from the source"
                              {:original original})))
            (str/replace-first text original replacement))
          source
          edits))

(defn- migrated-tree
  "Apply one plan to the pre-migration corpus and return the whole tree."
  [plan]
  (reduce (fn [tree {:keys [file edits]}]
            (assoc tree file (apply-edits (get tree file) edits)))
          (:pre corpus)
          (:files plan)))

;; ---------------------------------------------------------------------------
;; the six acceptance predicates, asserted on the planner's own output

;; @spec MCP-OP-ALIAS-004
(deftest acceptance-1-the-changed-file-set-equals-the-manifest-targets-exactly
  (let [plan (plan-corpus)]
    (is (:ok plan) (pr-str plan))
    (is (= (get-in corpus [:manifest :targets])
           (into (sorted-set) (map :file) (:files plan)))
        "no extra file and no missing file")))

;; @spec MCP-OP-ALIAS-009
;; @spec MCP-OP-ALIAS-010
(deftest acceptance-2-every-changed-file-equals-the-canonical-form-tree
  (let [plan (plan-corpus)
        tree (migrated-tree plan)]
    (doseq [file (get-in corpus [:manifest :targets])]
      (testing file
        (is (= (get (:post corpus) file) (get tree file)))))))

;; @spec MCP-OP-ALIAS-011
(deftest acceptance-3-every-protected-decoy-region-survives-byte-identically
  (let [tree (migrated-tree (plan-corpus))]
    (doseq [[file regions] (get-in corpus [:manifest :protected])
            {:keys [region sha256]} regions]
      (testing (str file " :: " region)
        (is (str/includes? (get tree file) region))
        (is (= sha256 (fixture/sha256 region)))))))

;; @spec MCP-OP-ALIAS-009
(deftest acceptance-4-every-migrated-file-binds-the-alias-it-now-uses
  (let [plan (plan-corpus)
        tree (migrated-tree plan)]
    (doseq [{:keys [file alias]} (:files plan)]
      (testing file
        (is (str/includes? (get tree file)
                           (str "[" fixture/to-lib " :as " alias "]"))
            "the new require binds the alias the sites now use")
        (is (str/includes? (get tree file) (str alias "/" fixture/to-var)))))))

;; @spec MCP-OP-ALIAS-005
(deftest acceptance-5-every-declared-site-is-rewritten-with-the-new-var-name
  (let [plan (plan-corpus)
        tree (migrated-tree plan)]
    (doseq [{:keys [file sites alias]} (:files plan)]
      (testing file
        (is (= (get-in corpus [:manifest :files file :sites]) sites))
        (is (= sites (count (re-seq (re-pattern (str "\\Q" alias "/" fixture/to-var "\\E"))
                                    (get tree file)))))))))

;; @spec MCP-OP-ALIAS-009
(defn- live-qualifiers
  "Namespace qualifiers of every token that is live code.

  Tokens inside an #_ discard or inside a metadata value are excluded, because
  the contract keeps those bytes exactly as they were."
  [source]
  (letfn [(walk [node]
            (cond
              (= :uneval (n/tag node)) []
              (= :meta (n/tag node))
              (mapcat walk (rest (filter #(not (contains? #{:whitespace :newline :comma}
                                                          (n/tag %)))
                                         (n/children node))))
              (= :token (n/tag node))
              (let [value (try (n/sexpr node) (catch Exception _ nil))]
                (if (and (symbol? value) (namespace value)) [(namespace value)] []))
              (n/inner? node) (mapcat walk (n/children node))
              :else []))]
    (set (walk (parser/parse-string-all source)))))

(deftest acceptance-6-no-residue-of-the-old-lib-or-var-remains
  (let [tree (migrated-tree (plan-corpus))
        retired #{"acid.fanout.store" "store" "st" "s"}]
    (doseq [file (get-in corpus [:manifest :targets])]
      (testing file
        (let [source (get tree file)]
          (is (not (str/includes? source (str "[" fixture/from-lib " ")))
              "no require of the retired lib survives")
          (is (not (str/includes? source (str "\n   " fixture/from-lib "\n")))
              "no bare require of the retired lib survives")
          (is (empty? (clojure.set/intersection retired (live-qualifiers source)))
              "no live code is still qualified by the retired lib or its aliases"))))))

;; ---------------------------------------------------------------------------
;; discovery, alias policy, and the histogram

;; @spec MCP-OP-ALIAS-003
;; @spec MCP-OP-ALIAS-019
(deftest totals-are-the-only-aggregate-the-planner-publishes
  (let [{:keys [totals]} (plan-corpus)
        manifest (:manifest corpus)]
    (is (= 12 (:files totals)))
    (is (= (:sites manifest) (:sites totals)))
    (is (= (:alias-histogram manifest) (into (sorted-map) (:alias-histogram totals))))
    (is (= (:collisions-resolved manifest) (:collisions-resolved totals)))))

;; @spec MCP-OP-ALIAS-007
(deftest per-file-alias-is-the-first-policy-entry-bound-to-nothing-in-that-file
  (let [plan (plan-corpus)
        by-file (into {} (map (juxt :file identity)) (:files plan))]
    (doseq [[file {:keys [alias collided]}] (get-in corpus [:manifest :files])]
      (testing file
        (is (= alias (:alias (get by-file file))))
        (is (= collided (vec (:collided (get by-file file)))))))))

;; @spec MCP-OP-ALIAS-005
(deftest every-legal-spelling-of-the-var-is-discovered
  (let [by-file (into {} (map (juxt :file identity)) (:files (plan-corpus)))]
    (testing "a per-file :as alias"
      (is (= 3 (:sites (get by-file "src/acid/fanout/t03.clj")))))
    (testing "a fully qualified use with no alias"
      (is (= 3 (:sites (get by-file "src/acid/fanout/t04.clj")))))
    (testing "a bare referred name"
      (is (= 3 (:sites (get by-file "src/acid/fanout/t06.clj")))))
    (testing "two aliases of the same lib in one file"
      (is (= 3 (:sites (get by-file "src/acid/fanout/t12.clj")))))))

;; @spec MCP-OP-ALIAS-011
(deftest decoys-are-not-sites
  (let [by-file (into {} (map (juxt :file identity)) (:files (plan-corpus)))
        tree (migrated-tree (plan-corpus))]
    (testing "a local named for the var, a string, a docstring and a comment"
      (is (= 3 (:sites (get by-file "src/acid/fanout/t01.clj"))))
      (is (str/includes? (get tree "src/acid/fanout/t01.clj")
                         "(let [find-event (store2/fetch-event id)]")))
    (testing "the reader conditional's selected branch is the only one rewritten"
      (is (str/includes? (get tree "src/acid/fanout/t02.clj")
                         "#?(:clj (store2/fetch-event id) :cljs (js-lookup id))")))
    (testing "an #_ discard and a non-selected reader-conditional branch"
      (is (str/includes? (get tree "src/acid/fanout/t02.clj")
                         "#_(store/find-event :disabled)"))
      (is (str/includes? (get tree "src/acid/fanout/t02.clj")
                         ":cljs (js-lookup id)")))
    (testing "a metadata value carrying the token"
      (is (str/includes? (get tree "src/acid/fanout/t08.clj")
                         "^{:doc \"store/find-event\"}")))
    (testing "a different namespace exporting the same var name"
      (is (str/includes? (get tree "src/acid/fanout/t07.clj")
                         "(mirror/find-event id)")))))

;; @spec MCP-OP-ALIAS-011
(deftest bystander-namespaces-are-never-planned
  (let [files (into #{} (map :file) (:files (plan-corpus)))]
    (doseq [file ["src/acid/fanout/n01.clj" "src/acid/fanout/n02.clj"
                  "src/acid/fanout/n03.clj" "src/acid/fanout/store.clj"
                  "src/acid/fanout/mirror.clj"]]
      (is (not (contains? files file)) file))))

;; ---------------------------------------------------------------------------
;; require mode

;; @spec MCP-OP-ALIAS-009
(deftest a-surviving-use-of-the-old-lib-adds-the-require-instead-of-replacing-it
  (let [source (str "(ns demo\n  (:require\n   [acid.fanout.store :as store]))\n\n"
                    "(defn one [id] (store/find-event id))\n\n"
                    "(defn two [id] (store/other-var id))\n")
        plan (alias-migration/plan (request {:expect {:files 1}})
                                   [{:file "src/demo.clj" :source source}])
        entry (first (:files plan))
        migrated (apply-edits source (:edits entry))]
    (is (:ok plan))
    (is (= :add (:require-mode entry)))
    (is (str/includes? migrated "[acid.fanout.store :as store]"))
    (is (str/includes? migrated "[acid.fanout.store2 :as store2]"))
    (is (str/includes? migrated "(store/other-var id)"))
    (is (str/includes? migrated "(store2/fetch-event id)"))))

;; @spec MCP-OP-ALIAS-011
(deftest a-site-in-a-non-selected-branch-keeps-the-old-require
  (let [source (str "(ns demo\n  (:require\n   [acid.fanout.store :as store]))\n\n"
                    "(defn one [id]\n"
                    "  #?(:clj (store/find-event id) :cljs (store/find-event 0)))\n")
        plan (alias-migration/plan (request {:expect {:files 1}})
                                   [{:file "src/demo.clj" :source source}])
        entry (first (:files plan))
        migrated (apply-edits source (:edits entry))]
    (is (:ok plan))
    (is (= :add (:require-mode entry)))
    (is (str/includes? migrated ":clj (store2/fetch-event id)"))
    (is (str/includes? migrated ":cljs (store/find-event 0)"))
    (is (str/includes? migrated "[acid.fanout.store :as store]"))))

;; ---------------------------------------------------------------------------
;; typed refusals

;; @spec MCP-OP-ALIAS-012
;; @spec MCP-OP-ALIAS-015
(deftest expect-mismatch-refuses-closed-and-names-found-versus-expected
  (let [plan (plan-corpus (request {:expect {:files 80}}) (sources))]
    (is (false? (:ok plan)))
    (is (= "alias-migration-expect-mismatch" (:error_type plan)))
    (is (= 12 (:found_files plan)))
    (is (= 80 (:expected_files plan)))
    (is (true? (:source_unchanged plan)))
    (is (= 12 (get-in plan [:next_call "expect" "files"]))
        "the next_call is executable: it carries the found count")))

;; @spec MCP-OP-ALIAS-006
;; @spec MCP-OP-ALIAS-015
(deftest an-empty-scope-refuses-closed-and-states-the-count
  (let [plan (plan-corpus (request {:expect {:files 3}})
                          (sources (select-keys (:pre corpus)
                                                ["src/acid/fanout/n01.clj"
                                                 "src/acid/fanout/n02.clj"
                                                 "src/acid/fanout/n03.clj"])))]
    (is (false? (:ok plan)))
    (is (= "alias-migration-empty-scope" (:error_type plan)))
    (is (= 0 (:found_files plan)))
    (is (= 3 (:scanned_files plan)))
    (is (seq (get-in plan [:next_call "scope" "paths"])))))

;; @spec MCP-OP-ALIAS-013
;; @spec MCP-OP-ALIAS-015
(deftest a-prefix-list-libspec-refuses-closed-and-names-the-file-and-form
  (let [source (str "(ns demo\n  (:require\n   (acid.fanout [store :as store])))\n\n"
                    "(defn one [id] (store/find-event id))\n")
        plan (alias-migration/plan (request {:expect {:files 1}})
                                   [{:file "src/demo.clj" :source source}])]
    (is (false? (:ok plan)))
    (is (= "alias-migration-indirect-reference" (:error_type plan)))
    (is (= "src/demo.clj" (:file plan)))
    (is (str/includes? (:form plan) "acid.fanout"))
    (is (= ["src/demo.clj"] (get-in plan [:next_call "scope" "exclude"])))))

;; @spec MCP-OP-ALIAS-013
(deftest a-runtime-require-refuses-closed-and-names-the-file
  (let [source (str "(ns demo\n  (:require\n   [acid.fanout.store :as store]))\n\n"
                    "(defn boot [] (require 'acid.fanout.store))\n\n"
                    "(defn one [id] (store/find-event id))\n")
        plan (alias-migration/plan (request {:expect {:files 1}})
                                   [{:file "src/demo.clj" :source source}])]
    (is (false? (:ok plan)))
    (is (= "alias-migration-indirect-reference" (:error_type plan)))
    (is (= "src/demo.clj" (:file plan)))))

;; @spec MCP-OP-ALIAS-013
(deftest a-quoted-occurrence-refuses-closed-and-names-the-form
  (let [source (str "(ns demo\n  (:require\n   [acid.fanout.store :as store]))\n\n"
                    "(defn one [id] [(store/find-event id) 'store/find-event])\n")
        plan (alias-migration/plan (request {:expect {:files 1}})
                                   [{:file "src/demo.clj" :source source}])]
    (is (false? (:ok plan)))
    (is (= "alias-migration-indirect-reference" (:error_type plan)))
    (is (= "quoted-reference" (:reason plan)))
    (is (str/includes? (:form plan) "'store/find-event"))))

;; @spec MCP-OP-ALIAS-013
(deftest a-site-in-a-non-selected-cljc-branch-refuses-closed
  (let [source (str "(ns demo\n  (:require\n   [acid.fanout.store :as store]))\n\n"
                    "(defn one [id]\n"
                    "  #?(:clj (store/find-event id) :cljs (store/find-event 0)))\n")
        plan (alias-migration/plan (request {:expect {:files 1}})
                                   [{:file "src/demo.cljc" :source source}])]
    (is (false? (:ok plan)))
    (is (= "alias-migration-indirect-reference" (:error_type plan)))
    (is (= "unselected-reader-conditional-branch" (:reason plan)))))

;; @spec MCP-OP-ALIAS-014
;; @spec MCP-OP-ALIAS-015
(deftest ambiguous-ownership-refuses-closed-and-names-both-candidates
  (let [source (str "(ns demo\n  (:require\n"
                    "   [acid.fanout.store :refer [find-event]]\n"
                    "   [acid.fanout.mirror :refer [find-event]]))\n\n"
                    "(defn one [id] (find-event id))\n")
        plan (alias-migration/plan (request {:expect {:files 1}})
                                   [{:file "src/demo.clj" :source source}])]
    (is (false? (:ok plan)))
    (is (= "alias-migration-ambiguous-ownership" (:error_type plan)))
    (is (= ["acid.fanout.store/find-event" "acid.fanout.mirror/find-event"]
           (:candidates plan)))
    (is (= ["src/demo.clj"] (get-in plan [:next_call "scope" "exclude"])))))

;; @spec MCP-OP-ALIAS-008
;; @spec MCP-OP-ALIAS-015
(deftest an-exhausted-alias-policy-refuses-closed-and-names-the-collisions
  (let [source (str "(ns demo\n  (:require\n"
                    "   [acid.fanout.store :as store]\n"
                    "   [a.b :as store2]\n"
                    "   [a.c :as st2]\n"
                    "   [a.d :as es]\n"
                    "   [a.e :as store-2]))\n\n"
                    "(defn one [id] (store/find-event id))\n")
        plan (alias-migration/plan (request {:expect {:files 1}})
                                   [{:file "src/demo.clj" :source source}])]
    (is (false? (:ok plan)))
    (is (= "alias-migration-alias-policy-exhausted" (:error_type plan)))
    (is (= "src/demo.clj" (:file plan)))
    (is (= ["store2" "st2" "es" "store-2"] (:collided_bindings plan)))
    ;; @spec MCP-OP-ALIAS-008
    ;; This assertion used to PIN the defect: it required the next_call to
    ;; append `store-2-2`, an alias_policy entry the caller never sent. A
    ;; next_call is the caller's request with one field corrected and may not
    ;; answer with a value the caller's own request forbids.
    (is (nil? (:next_call plan))
        "an exhausted policy was answered with an alias outside it")
    (is (str/includes? (str (:remedy plan)) "exhausted")
        "the remedy does not say the policy is exhausted")
    (is (str/includes? (str (:remedy plan)) "src/demo.clj")
        "the remedy does not name the file")))

;; @spec MCP-OP-ALIAS-007
;; @spec MCP-OP-ALIAS-007
(deftest local-bindings-of-every-declared-shape-do-not-block-an-alias
  ;; A qualified symbol's namespace part resolves through the ns alias map, so
  ;; no lexical binding can shadow it. Every shape below must still get store2.
  (doseq [[label body] [["let" "(defn f [id] (let [store2 1] [store2 (store/find-event id)]))"]
                        ["loop" "(defn f [id] (loop [store2 1] [store2 (store/find-event id)]))"]
                        ["doseq" "(defn f [ids] (doseq [store2 ids] (store/find-event store2)))"]
                        ["for" "(defn f [ids] (for [store2 ids] (store/find-event store2)))"]
                        ["binding" "(defn f [id] (binding [store2 1] (store/find-event id)))"]
                        ["with-open" "(defn f [id] (with-open [store2 (io)] (store/find-event id)))"]
                        ["letfn" "(defn f [id] (letfn [(store2 [x] x)] (store/find-event id)))"]
                        ["as->" "(defn f [id] (as-> id store2 (store/find-event store2)))"]
                        ["catch" "(defn f [id] (try (store/find-event id) (catch Exception store2 nil)))"]
                        ["params" "(defn f [store2 id] [store2 (store/find-event id)])"]
                        ["destructuring" "(defn f [{:keys [store2]} id] [store2 (store/find-event id)])"]
                        ["top-level def" "(def store2 1)\n(defn f [id] (store/find-event id))"]]]
    (testing label
      (let [source (str "(ns demo\n  (:require\n   [acid.fanout.store :as store]))\n\n"
                        body "\n")
            plan (alias-migration/plan (request {:expect {:files 1}})
                                       [{:file "src/demo.clj" :source source}])]
        (is (:ok plan) (pr-str plan))
        (is (= "store2" (:alias (first (:files plan))))
            "a local of that name is not a collision")
        (is (= [] (vec (:collided (first (:files plan))))))))))

;; @spec MCP-OP-ALIAS-007
(deftest a-local-named-like-a-policy-entry-is-not-a-collision
  ;; `store2/fetch-event` is a QUALIFIED symbol: its namespace part is resolved
  ;; through the file's alias map, not through lexical scope. A local named
  ;; store2 therefore cannot shadow it, and must not push the policy along.
  (let [source (str "(ns demo\n  (:require\n   [acid.fanout.store :as store]))\n\n"
                    "(defn one\n"
                    "  [id]\n"
                    "  (let [store2 1]\n"
                    "    [store2 (store/find-event id)]))\n")
        plan (alias-migration/plan (request {:expect {:files 1}})
                                   [{:file "src/demo.clj" :source source}])
        entry (first (:files plan))
        migrated (apply-edits source (:edits entry))]
    (is (:ok plan) (pr-str plan))
    (is (= "store2" (:alias entry)) "the first policy entry is free")
    (is (= [] (vec (:collided entry))))
    (is (= 0 (get-in plan [:totals :collisions-resolved])))
    (testing "the local keeps its name and the qualified site resolves past it"
      (is (str/includes? migrated "[acid.fanout.store2 :as store2]"))
      (is (str/includes? migrated "(let [store2 1]"))
      (is (str/includes? migrated "[store2 (store2/fetch-event id)]")))))

;; @spec MCP-OP-ALIAS-007
(deftest only-an-ns-level-alias-or-referred-name-is-a-collision
  (let [ns-form (fn [& requires]
                  (str "(ns demo\n  (:require\n"
                       (str/join "\n" (map #(str "   " %) requires))
                       "))\n\n(defn one [id] (store/find-event id))\n"))
        alias-of (fn [source]
                   (let [plan (alias-migration/plan (request {:expect {:files 1}})
                                                    [{:file "src/demo.clj"
                                                      :source source}])]
                     (is (:ok plan) (pr-str plan))
                     [(:alias (first (:files plan)))
                      (vec (:collided (first (:files plan))))]))]
    (testing "an :as alias collides"
      (is (= ["st2" ["store2"]]
             (alias-of (ns-form "[acid.fanout.store :as store]" "[a.b :as store2]")))))
    (testing "an :as-alias alias collides"
      (is (= ["st2" ["store2"]]
             (alias-of (ns-form "[acid.fanout.store :as store]"
                                "[a.b :as-alias store2]")))))
    (testing "a referred name collides"
      (is (= ["st2" ["store2"]]
             (alias-of (ns-form "[acid.fanout.store :as store]"
                                "[a.b :refer [store2]]")))))
    (testing "the file's own namespace name does not collide"
      (is (= ["store2" []]
             (alias-of (str "(ns store2\n  (:require\n"
                            "   [acid.fanout.store :as store]))\n\n"
                            "(defn one [id] (store/find-event id))\n")))))))

;; ---------------------------------------------------------------------------
;; every position a qualified symbol can occupy
;;
;; Reduced from the real anchor: curtaincall-cfp at d9afe8e9,
;; src/cfp_scheduler_killer/replay.clj:119-128 and sinks.clj:693.

(defn- lib-request
  [overrides]
  (merge {:workspace-root "/workspace"
          :from {:lib fixture/from-lib :var nil}
          :to {:lib fixture/to-lib :var nil
               :alias-policy ["store2"] :refer-policy "preserve-refer"}
          :scope {:paths ["src/**"]}
          :expect {:files 1}}
         overrides))

(defn- migrate-one
  [body]
  (let [source (str "(ns demo\n  (:require\n   [acid.fanout.store :as store]))\n\n"
                    body)
        plan (alias-migration/plan (lib-request {}) [{:file "src/demo.clj"
                                                      :source source}])]
    (if-not (:ok plan)
      plan
      (assoc plan :migrated (apply-edits source (:edits (first (:files plan))))
                  :entry (first (:files plan))))))

;; @spec MCP-OP-ALIAS-029
(deftest a-var-quoted-reference-is-migrated
  (testing "the #' reader form, which rewrite-clj represents as a var node"
    (let [result (migrate-one
                   "(def installed (alter-var-root #'store/*default-sinks-fn* identity))\n")]
      (is (:ok result) (pr-str result))
      (is (= 1 (:sites (:entry result))))
      (is (str/includes? (:migrated result) "#'store2/*default-sinks-fn*"))))
  (testing "the (var ...) special form"
    (let [result (migrate-one "(def clock (var store/*clock*))\n")]
      (is (:ok result))
      (is (str/includes? (:migrated result) "(var store2/*clock*)"))))
  (testing "earmuffed names carry through both spellings"
    (let [result (migrate-one
                   "(defn f [] [#'store/*clock* store/*clock* (var store/*clock*)])\n")]
      (is (= 3 (:sites (:entry result))))
      (is (not (str/includes? (:migrated result) "store/*clock*"))))))

;; @spec MCP-OP-ALIAS-030
(deftest a-var-binding-vectors-left-hand-side-is-a-site-not-a-local
  ;; This is the exact shape that broke the anchor at replay.clj:128. `binding`
  ;; rebinds Vars, so its left-hand side is a reference through the alias map,
  ;; not a local binding form.
  (testing "binding, reduced from replay.clj:128"
    (let [result (migrate-one
                   (str "(defn replay!\n"
                        "  \"Born ON the simulated timeline: creation facts are stamped\n"
                        "   (via store/*clock*) an hour before the CFP opens.\"\n"
                        "  [sim-birth]\n"
                        "  (binding [store/*clock* sim-birth]\n"
                        "    (store/other-var sim-birth)))\n"))]
      (is (:ok result) (pr-str result))
      (is (= 2 (:sites (:entry result))))
      (is (str/includes? (:migrated result) "(binding [store2/*clock* sim-birth]"))
      (is (str/includes? (:migrated result) "(store2/other-var sim-birth)"))
      (testing "the docstring one line above keeps its bytes"
        (is (str/includes? (:migrated result)
                           "(via store/*clock*) an hour before the CFP opens.")))))
  (testing "with-redefs, the same shape, hundreds of times in the anchor's tests"
    (let [result (migrate-one
                   "(defn t [] (with-redefs [store/snapshot (constantly 1)] (store/go)))\n")]
      (is (= 2 (:sites (:entry result))))
      (is (str/includes? (:migrated result)
                         "(with-redefs [store2/snapshot (constantly 1)]"))))
  (testing "a let vector's left-hand side is still a local and still untouched"
    (let [result (migrate-one
                   "(defn f [x] (let [store2 1 y (store/go x)] [store2 y]))\n")]
      (is (= 1 (:sites (:entry result))))
      (is (str/includes? (:migrated result) "(let [store2 1 y (store2/go x)]")))))

;; @spec MCP-OP-ALIAS-031
(deftest a-syntax-quoted-reference-is-migrated-and-a-plain-quote-refuses
  (testing "the reader resolves an alias inside a syntax quote, so it migrates"
    (let [result (migrate-one
                   (str "(defmacro as-of [selection & body]\n"
                        "  `(binding [store/*as-of-state* ~selection]\n"
                        "     (store/run ~@body)))\n"))]
      (is (:ok result) (pr-str result))
      (is (= 2 (:sites (:entry result))))
      (is (str/includes? (:migrated result) "(binding [store2/*as-of-state* ~selection]"))
      (is (str/includes? (:migrated result) "(store2/run ~@body)"))))
  (testing "a plain quote is a literal symbol nothing resolves, so it refuses"
    (let [result (migrate-one "(defn f [] ['store/go (store/go)])\n")]
      (is (false? (:ok result)))
      (is (= "alias-migration-indirect-reference" (:error_type result)))
      (is (= "quoted-reference" (:reason result))))))

;; @spec MCP-OP-ALIAS-032
(deftest a-namespaced-keyword-through-the-alias-is-refused-typed
  ;; Rewriting ::store/k changes the KEYWORD'S VALUE — a keyword's namespace is
  ;; part of its identity and may be persisted, dispatched on, or compared
  ;; elsewhere. Leaving it breaks the read once the alias is gone. Neither is
  ;; bookkeeping, so the verb refuses rather than guessing.
  (testing "an auto-resolved keyword refuses and names the form"
    (let [result (migrate-one "(defn f [] [::store/k (store/go)])\n")]
      (is (false? (:ok result)))
      (is (= "alias-migration-indirect-reference" (:error_type result)))
      (is (= "auto-resolved-keyword" (:reason result)))
      (is (= "::store/k" (:form result)))
      (is (= "src/demo.clj" (:file result)))
      (is (= ["src/demo.clj"] (get-in result [:next_call "scope" "exclude"])))))
  (testing "a single-colon keyword is not alias-resolved and never moves"
    (let [result (migrate-one "(defn f [] [:store/k (store/go)])\n")]
      (is (:ok result) (pr-str result))
      (is (= 1 (:sites (:entry result))))
      (is (str/includes? (:migrated result) ":store/k"))
      (is (str/includes? (:migrated result) "(store2/go)"))))
  (testing "a keyword through an UNRELATED alias is untouched"
    (let [result (migrate-one "(defn f [] [::other/k (store/go)])\n")]
      (is (:ok result))
      (is (str/includes? (:migrated result) "::other/k")))))

;; @spec MCP-OP-ALIAS-033
(deftest a-qualified-symbol-inside-a-metadata-value-is-migrated
  (let [result (migrate-one
                 (str "(def ^{:validator store/valid? :doc \"see store/valid?\"} guarded 1)\n"))]
    (is (:ok result) (pr-str result))
    (is (= 1 (:sites (:entry result))))
    (is (str/includes? (:migrated result) ":validator store2/valid?")
        "metadata values are evaluated code")
    (is (str/includes? (:migrated result) ":doc \"see store/valid?\"")
        "a string inside metadata is still a string")))

;; @spec MCP-OP-ALIAS-030
(deftest a-qualified-symbol-in-binding-position-is-migrated
  ;; Real bytes: curtaincall-cfp d9afe8e9 src/cfp_scheduler_killer/replay.clj:128
  ;; and src/cli/judge_sandbox.clj:118. The symbol is NAMED as a Var, never
  ;; invoked, so a discovery pass that only looks at operator position sees
  ;; nothing and the alias is retired out from under it.
  (let [result (migrate-one
                 (str "(defn replay! [sim-birth at offset]\n"
                      "  (binding [store/*clock* sim-birth]\n"
                      "    (store/other-var sim-birth))\n"
                      "  (binding [store/*clock* (.plusSeconds at (* offset 24 60 60))]\n"
                      "    :done))\n"))]
    (is (:ok result) (pr-str result))
    (is (= 3 (:sites (:entry result))) "two binding names plus one ordinary call")
    (is (= 2 (count (re-seq (re-pattern (java.util.regex.Pattern/quote
                                          "binding [store2/*clock*"))
                            (:migrated result))))
        "both binding-vector names moved")
    (is (not (str/includes? (:migrated result) "store/*clock*")))))

;; @spec MCP-OP-ALIAS-030
(deftest with-redefs-over-several-vars-migrates-every-name
  ;; Real bytes: curtaincall-cfp d9afe8e9
  ;; test/cfp_scheduler_killer/db_correct_test.clj:599-610 — seven Var names in
  ;; one binding vector, several with values that reference the lib again.
  (let [result (migrate-one
                 (str "(defn t [state-atom appended]\n"
                      "  (with-redefs [store/postgres? (constantly true)\n"
                      "                store/refresh-if-changed! (constantly nil)\n"
                      "                store/snapshot #(deref state-atom)\n"
                      "                store/person-by-id #(get-in @state-atom [:people %])\n"
                      "                store/append-all!\n"
                      "                (fn [facts]\n"
                      "                  (let [canonical (mapv #(store/canonicalize %) facts)]\n"
                      "                    (swap! state-atom #(reduce store/fold-one % canonical))\n"
                      "                    canonical))\n"
                      "                store/load! #(deref state-atom)]\n"
                      "    (store/other-var 1)))\n"))]
    (is (:ok result) (pr-str result))
    (is (= 9 (:sites (:entry result)))
        "six rebound Var names, two references inside the values, one call")
    (doseq [name ["postgres?" "refresh-if-changed!" "snapshot" "person-by-id"
                  "append-all!" "load!" "canonicalize" "fold-one" "other-var"]]
      (is (str/includes? (:migrated result) (str "store2/" name)) name))
    (is (not (re-find #"[^2]store/" (:migrated result)))
        "no name is left behind under the retired alias")))

;; @spec MCP-OP-ALIAS-035
(deftest a-quoted-fully-qualified-symbol-in-data-position-is-migrated
  ;; Real bytes: curtaincall-cfp d9afe8e9
  ;; src/cfp_scheduler_killer/sched_import.clj:127. That namespace deliberately
  ;; does NOT require the lib — it resolves it at runtime — so it fails lazily
  ;; at call time with no compile error: the tree loads and breaks in
  ;; production. A fully qualified name is unambiguous, so it migrates.
  (testing "in a file that never requires the lib"
    (let [source (str "(ns demo\n  (:require\n   [clojure.string :as str]))\n\n"
                      "(defn import! [slug]\n"
                      "  (let [state @(var-get (requiring-resolve"
                      " 'acid.fanout.store/state))]\n"
                      "    [slug (str/trim (str state))]))\n")
          plan (alias-migration/plan (lib-request {}) [{:file "src/demo.clj"
                                                        :source source}])
          entry (first (:files plan))
          migrated (apply-edits source (:edits entry))]
      (is (:ok plan) (pr-str plan))
      (is (= 1 (:sites entry)))
      (is (= :qualified-only (:require-mode entry))
          "no require to rewrite; only the fully qualified spelling moves")
      (is (str/includes? migrated "'acid.fanout.store2/state"))
      (is (str/includes? migrated "[clojure.string :as str]")
          "the file's own requires are untouched")))
  (testing "an ALIAS-qualified quote stays a refusal: nothing resolves it"
    (let [result (migrate-one "(defn f [] (requiring-resolve 'store/state))\n")]
      (is (false? (:ok result)))
      (is (= "quoted-reference" (:reason result)))))
  (testing "a prefix-sharing sibling's fully qualified name is untouched"
    (let [source (str "(ns demo)\n\n"
                      "(defn f [] (requiring-resolve 'acid.fanout.store-pg/write!))\n")
          plan (alias-migration/plan (lib-request {}) [{:file "src/demo.clj"
                                                        :source source}])]
      (is (false? (:ok plan)))
      (is (= "alias-migration-empty-scope" (:error_type plan))
          "store-pg is a different namespace, so nothing in this file is a site"))))

;; @spec MCP-OP-ALIAS-035
(deftest a-quoted-fully-qualified-symbol-keeps-its-full-qualification
  ;; PF-4, the hand-drive the pre-registration insists on
  ;; (/home/forge/tmp/arms/e3/pf4-cfp-shapes, 2026-09-03). The three shapes of
  ;; the real cfp anchor, migrated in one call. SHAPE B came back
  ;; ALIAS-qualified:
  ;;
  ;;   before  {:lookup 'acid.fanout.store/find-event}
  ;;   after   {:lookup 'store2/fetch-event}
  ;;
  ;; measured in that same worktree —
  ;;   resolve inside the defining namespace : #'acid.fanout.store2/fetch-event
  ;;   requiring-resolve of the VALUE from outside it :
  ;;     ERR Could not locate store2.bb, store2.clj or store2.cljc on classpath
  ;;
  ;; A quoted symbol's whole purpose is to be resolved somewhere else, and
  ;; MCP-OP-ALIAS-035 says this shape rewrites to `'to.lib/x` for exactly that
  ;; reason. The verb's own contract also makes `'alias/x` a typed refusal — a
  ;; literal nothing resolves — so producing one is producing a shape the same
  ;; verb refuses to read.
  (let [source (str "(ns acid.fanout.shapes\n"
                    "  (:require [acid.fanout.store :as store]))\n\n"
                    ";; SHAPE A — a qualified symbol in BINDING-VECTOR position"
                    " (a value, not a call).\n"
                    "(defn bind-position\n"
                    "  [id]\n"
                    "  (let [lookup store/find-event]\n"
                    "    (lookup id)))\n\n"
                    ";; SHAPE B — a QUOTED fully-qualified symbol in DATA"
                    " position.\n"
                    "(def handlers\n"
                    "  {:lookup 'acid.fanout.store/find-event})\n\n"
                    ";; an ordinary call site, for contrast\n"
                    "(defn call-position\n"
                    "  [id]\n"
                    "  (store/find-event id))\n")
        plan (alias-migration/plan
               {:workspace-root "/workspace"
                :from {:lib fixture/from-lib :var fixture/from-var}
                :to {:lib fixture/to-lib :var fixture/to-var
                     :alias-policy ["store2" "st2" "es"]}
                :scope {:paths ["src/**"]}
                :expect {:files 1}}
               [{:file "src/acid/fanout/shapes.clj" :source source}])
        entry (first (:files plan))
        migrated (apply-edits source (:edits entry))]
    (is (:ok plan) (pr-str plan))
    (is (= 3 (:sites entry)) "the arm's own receipt: 1 files · 3 sites")
    (testing "SHAPE B keeps a spelling that resolves from anywhere"
      (is (str/includes? migrated "'acid.fanout.store2/fetch-event")
          "the quoted fully-qualified symbol was narrowed to an alias")
      (is (not (str/includes? migrated "'store2/fetch-event"))
          "the verb produced the very shape it refuses to read"))
    (testing "the other two shapes stay alias-qualified"
      (is (str/includes? migrated "(let [lookup store2/fetch-event]"))
      (is (str/includes? migrated "(store2/fetch-event id)")))
    (testing "and the require moved once"
      (is (str/includes? migrated "[acid.fanout.store2 :as store2]")))))

;; @spec MCP-OP-ALIAS-035
(deftest a-quoted-fully-qualified-symbol-keeps-full-qualification-in-lib-mode
  ;; The same shape in a file that DOES require the lib. The existing witness
  ;; covered only the file that does not, where the chosen alias happens to BE
  ;; to.lib, so the defect could not show.
  (let [result (migrate-one
                 (str "(def handlers\n"
                      "  {:lookup 'acid.fanout.store/find-event})\n"))]
    (is (:ok result) (pr-str result))
    (is (= 1 (:sites (:entry result))))
    (is (str/includes? (:migrated result) "'acid.fanout.store2/find-event")
        "the quoted fully-qualified symbol was narrowed to an alias")))

;; @spec MCP-OP-ALIAS-062
;; Faithful minimization of the 2026-09-04 HTTP mixed-bare incident: the
;; original program evaluated [:selected :other], then lost other-event.
(deftest selected-var-migration-preserves-unrelated-referred-imports
  (doseq [policy [nil "preserve-refer" "alias-qualify"]
          [label libspec body expected-old]
          [["mixed bare" "[example.old :refer [find-event other-event]]"
            "[(find-event) (other-event)]" '[example.old :refer [other-event]]]
           ["unused unrelated import" "[example.old :refer [find-event other-event]]"
            "[(find-event)]" '[example.old :refer [other-event]]]
           ["qualified other" "[example.old :as old :refer [find-event]]"
            "[(find-event) (old/other-event)]" '[example.old :as old :refer []]]
           ["qualified selected" "[example.old :as old :refer [find-event other-event]]"
            "[(old/find-event) (other-event)]" '[example.old :as old :refer [other-event]]]
           ["refer all" "[example.old :refer :all]"
            "[(find-event) (other-event)]" '[example.old :refer :all]]]]
    (testing (str label " / " policy)
      (let [source (str "(ns example.client (:require " libspec "))\n"
                        "(defn result [] " body ")\n")
            req {:workspace-root "/workspace"
                 :from {:lib "example.old" :var "find-event"}
                 :to (cond-> {:lib "example.new" :var "fetch-event"
                              :alias-policy ["newlib"]}
                       policy (assoc :refer-policy policy))
                 :scope {:paths ["src/**"]} :expect {:files 1}}
            plan (alias-migration/plan req [{:file "src/example/client.clj" :source source}])
            entry (first (:files plan))
            migrated (apply-edits source (:edits entry))
            ns-form (n/sexpr (first (n/children (parser/parse-string-all migrated))))
            imports (set (rest (nth ns-form 2)))]
        (is (= true (:ok plan)) (pr-str plan))
        (is (= :add (:require-mode entry)))
        (is (contains? imports expected-old) migrated)
        (is (contains? imports '[example.new :as newlib]) migrated)
        (is (str/includes? migrated "(newlib/fetch-event)"))
        (is (not (str/includes? migrated "(find-event)")))
        (when (str/includes? body "(other-event)")
          (is (str/includes? migrated "(other-event)")))))))

;; @spec MCP-OP-ALIAS-062
(deftest selected-refer-removal-preserves-comments-order-and-shadowed-forms
  (doseq [refer-text ["find-event ; selected comment\n other-event third-event"
                      "other-event find-event ; selected comment\n third-event"
                      "other-event third-event ; selected comment\n find-event"]]
    (let [protected "(defn local [find-event] (find-event))"
          source (str "(ns example.client (:require [example.old :as old :refer ["
                      refer-text "]]))\n(defn result [] [(find-event) (other-event)])\n"
                      protected "\n")
          req {:workspace-root "/workspace"
               :from {:lib "example.old" :var "find-event"}
               :to {:lib "example.new" :var "fetch-event" :alias-policy ["newlib"]}
               :scope {:paths ["src/**"]} :expect {:files 1}}
          plan (alias-migration/plan req [{:file "src/example/client.clj" :source source}])
          migrated (apply-edits source (:edits (first (:files plan))))
          ns-form (n/sexpr (first (n/children (parser/parse-string-all migrated))))]
      (is (= true (:ok plan)) (pr-str plan))
      (is (= '[example.old :as old :refer [other-event third-event]]
             (first (rest (nth ns-form 2)))))
      (is (str/includes? migrated "; selected comment\n"))
      (is (str/includes? migrated protected)))))

;; @spec MCP-OP-ALIAS-062
(deftest selected-refer-repair-respects-reader-branches-and-refusals
  (let [req {:workspace-root "/workspace"
             :from {:lib "example.old" :var "find-event"}
             :to {:lib "example.new" :var "fetch-event" :alias-policy ["newlib"]}
             :scope {:paths ["src/**"]} :expect {:files 1}}
        source (str "(ns example.client (:require [example.old :refer [find-event other-event]]))\n"
                    "(defn result [] #?(:clj [(find-event) (other-event)] :cljs [(find-event)]))\n")
        kept (alias-migration/plan req [{:file "src/example/client.clj" :source source}])
        migrated (apply-edits source (:edits (first (:files kept))))
        refused (alias-migration/plan req [{:file "src/example/client.cljc" :source source}])
        ambiguous-source (str "(ns example.client (:require [example.old :refer [find-event other-event]]"
                              " [example.competitor :refer [find-event]]))\n(defn result [] (find-event))")
        ambiguous (alias-migration/plan req [{:file "src/example/client.clj" :source ambiguous-source}])]
    (is (= true (:ok kept)))
    (is (str/includes? migrated "[example.old :refer [find-event other-event]]"))
    (is (str/includes? migrated ":clj [(newlib/fetch-event) (other-event)]"))
    (is (str/includes? migrated ":cljs [(find-event)]"))
    (is (= "alias-migration-indirect-reference" (:error_type refused)))
    (is (= true (:source_unchanged refused)))
    (is (nil? (:files refused)))
    (is (= "alias-migration-ambiguous-ownership" (:error_type ambiguous)))
    (is (= true (:source_unchanged ambiguous)))
    (is (nil? (:files ambiguous)))))

;; @spec MCP-OP-ALIAS-062
(deftest selected-refer-repair-retains-positive-controls-and-noop
  (doseq [policy [nil "preserve-refer" "alias-qualify"]
          [libspec body expected-old]
          [["[example.old :refer [find-event]]" "[(find-event)]" nil]
           ["[example.old :as old]" "[(old/find-event) (old/other-event)]"
            '[example.old :as old]]]]
    (let [req {:workspace-root "/workspace"
               :from {:lib "example.old" :var "find-event"}
               :to (cond-> {:lib "example.new" :var "fetch-event" :alias-policy ["newlib"]}
                     policy (assoc :refer-policy policy))
               :scope {:paths ["src/**"]} :expect {:files 1}}
          source (str "(ns example.client (:require " libspec "))\n(defn result [] " body ")")
          plan (alias-migration/plan req [{:file "src/example/client.clj" :source source}])
          entry (first (:files plan))
          migrated (apply-edits source (:edits entry))
          imports (set (rest (nth (n/sexpr (first (n/children (parser/parse-string-all migrated)))) 2)))]
      (is (= true (:ok plan)))
      (is (= (if expected-old :add :replace) (:require-mode entry)))
      (is (= (cond-> #{'[example.new :as newlib]} expected-old (conj expected-old)) imports))
      (is (str/includes? migrated "(newlib/fetch-event)"))
      (let [noop (alias-migration/plan req [{:file "src/example/client.clj" :source migrated}])]
        (is (= false (:ok noop)))
        (is (= true (:source_unchanged noop)))
        (is (nil? (:files noop)))))))

;; @spec MCP-OP-ALIAS-062
;; Independent review's valid metadata-bearing refer entries, 2026-09-04.
(deftest selected-refer-repair-understands-metadata-symbol-identity
  (doseq [policy ["preserve-refer" "alias-qualify"]
          [refs selected-call keep-metadata?]
          [["find-event ^:private other-event" "(find-event)" true]
           ["^:private find-event other-event" "(old/find-event)" false]
           ["^:private find-event other-event" "(find-event)" false]]]
    (let [source (str "(ns example.client (:require [example.old :as old :refer [" refs "]]))\n"
                      "(defn result [] [" selected-call " (other-event)])")
          req {:workspace-root "/workspace"
               :from {:lib "example.old" :var "find-event"}
               :to {:lib "example.new" :var "fetch-event" :alias-policy ["newlib"] :refer-policy policy}
               :scope {:paths ["src/**"]} :expect {:files 1}}
          plan (alias-migration/plan req [{:file "src/example/client.clj" :source source}])
          migrated (apply-edits source (:edits (first (:files plan))))
          forms (parser/parse-string-all migrated)
          imports (set (rest (nth (n/sexpr (first (n/children forms))) 2)))]
      (is (= true (:ok plan)) (pr-str plan))
      (is (contains? imports '[example.old :as old :refer [other-event]]) migrated)
      (is (str/includes? migrated "(newlib/fetch-event)"))
      (is (str/includes? migrated "(other-event)"))
      (is (= keep-metadata? (str/includes? migrated "^:private other-event"))))))

;; @spec MCP-OP-ALIAS-062
(deftest selected-renamed-refer-refuses-without-authorizing-a-different-migration
  (doseq [policy ["preserve-refer" "alias-qualify"]
          body ["[(old/find-event) (selected) (other-event)]" "[(selected) (other-event)]"]]
    (let [source (str "(ns example.client (:require [example.old :as old :refer [find-event other-event]"
                      " :rename {find-event selected}]))\n(defn result [] " body ")")
          req {:workspace-root "/workspace"
               :from {:lib "example.old" :var "find-event"}
               :to {:lib "example.new" :var "fetch-event" :alias-policy ["newlib"] :refer-policy policy}
               :scope {:paths ["src/**"]} :expect {:files 1}}
          plan (alias-migration/plan req [{:file "src/example/client.clj" :source source}])]
      (is (= false (:ok plan)))
      (is (= "alias-migration-indirect-reference" (:error_type plan)))
      (is (= "unsupported-selected-renamed-refer" (:reason plan)))
      (is (= true (:source_unchanged plan)))
      (is (= "src/example/client.clj" (:file plan)))
      (is (nil? (:files plan)))
      (is (nil? (:next_call plan)))
      (is (str/includes? (or (:remedy plan) "") "renamed binding")))))

;; @spec MCP-OP-ALIAS-062
(deftest unrelated-renamed-refer-remains-at-the-old-library
  (let [source (str "(ns example.client (:require [example.old :refer [find-event other-event]"
                    " :rename {other-event other}]))\n(defn result [] [(find-event) (other)])")
        req {:workspace-root "/workspace"
             :from {:lib "example.old" :var "find-event"}
             :to {:lib "example.new" :var "fetch-event" :alias-policy ["newlib"]}
             :scope {:paths ["src/**"]} :expect {:files 1}}
        plan (alias-migration/plan req [{:file "src/example/client.clj" :source source}])
        migrated (apply-edits source (:edits (first (:files plan))))
        imports (set (rest (nth (n/sexpr (first (n/children (parser/parse-string-all migrated)))) 2)))]
    (is (= true (:ok plan)))
    (is (contains? imports '[example.old :refer [other-event] :rename {other-event other}]))
    (is (str/includes? migrated "[(newlib/fetch-event) (other)]"))))

;; @spec MCP-OP-ALIAS-063
(deftest refer-repair-preserves-discarded-entries-and-metadata-containers
  (doseq [policy ["preserve-refer" "alias-qualify"]
          [refer-value protected]
          [["[#_find-event find-event other-event]" "#_find-event"]
           ["^:private [find-event other-event]" ":refer ^:private ["]
           ["^{:private true} [#_find-event ^:private find-event other-event]"
            ":refer ^{:private true} [#_find-event"]]]
    (let [source (str "(ns example.client (:require [example.old :refer " refer-value "]))\n"
                      "(defn result [] [(find-event) (other-event)])")
          req {:workspace-root "/workspace"
               :from {:lib "example.old" :var "find-event"}
               :to {:lib "example.new" :var "fetch-event" :alias-policy ["newlib"] :refer-policy policy}
               :scope {:paths ["src/**"]} :expect {:files 1}}
          plan (alias-migration/plan req [{:file "src/example/client.clj" :source source}])
          migrated (apply-edits source (:edits (first (:files plan))))]
      (is (= true (:ok plan)) (pr-str plan))
      (is (str/includes? migrated protected) migrated)
      (is (str/includes? migrated "[(newlib/fetch-event) (other-event)]"))
      (is (= '[example.old :refer [other-event]]
             (first (rest (nth (n/sexpr (first (n/children (parser/parse-string-all migrated)))) 2))))))))

;; @spec MCP-OP-ALIAS-063
(deftest renamed-selected-refusal-sees-through-map-metadata
  (doseq [policy ["preserve-refer" "alias-qualify"]]
    (let [source (str "(ns example.client (:require [example.old :as old :refer [find-event other-event]"
                      " :rename ^:private {find-event selected}]))\n"
                      "(defn result [] [(old/find-event) (selected) (other-event)])")
          req {:workspace-root "/workspace"
               :from {:lib "example.old" :var "find-event"}
               :to {:lib "example.new" :var "fetch-event" :alias-policy ["newlib"] :refer-policy policy}
               :scope {:paths ["src/**"]} :expect {:files 1}}
          plan (alias-migration/plan req [{:file "src/example/client.clj" :source source}])]
      (is (= false (:ok plan)))
      (is (= "alias-migration-indirect-reference" (:error_type plan)))
      (is (= "unsupported-selected-renamed-refer" (:reason plan)))
      (is (= true (:source_unchanged plan)))
      (is (nil? (:files plan)))
      (is (nil? (:next_call plan))))))

;; @spec MCP-OP-ALIAS-064
(deftest discarded-libspec-option-values-are-not-live-import-authority
  (doseq [policy ["preserve-refer" "alias-qualify"]
          [libspec body refuse?]
          [["[example.old :refer #_[decoy] [find-event other-event]]"
            "[(find-event) (other-event)]" false]
           ["[example.old :as old :refer [find-event other-event] :rename #_{} {find-event selected}]"
            "[(old/find-event) (selected) (other-event)]" true]]]
    (let [source (str "(ns example.client (:require " libspec "))\n(defn result [] " body ")")
          req {:workspace-root "/workspace"
               :from {:lib "example.old" :var "find-event"}
               :to {:lib "example.new" :var "fetch-event" :alias-policy ["newlib"] :refer-policy policy}
               :scope {:paths ["src/**"]} :expect {:files 1}}
          plan (alias-migration/plan req [{:file "src/example/client.clj" :source source}])]
      (if refuse?
        (do
          (is (= false (:ok plan)))
          (is (= "alias-migration-indirect-reference" (:error_type plan)))
          (is (= "unsupported-selected-renamed-refer" (:reason plan)))
          (is (= true (:source_unchanged plan)))
          (is (nil? (:files plan)))
          (is (nil? (:next_call plan))))
        (let [migrated (apply-edits source (:edits (first (:files plan))))]
          (is (= true (:ok plan)) (pr-str plan))
          (is (str/includes? migrated ":refer #_[decoy] ["))
          (is (str/includes? migrated "[(newlib/fetch-event) (other-event)]"))
          (is (= '[example.old :refer [other-event]]
                 (first (rest (nth (n/sexpr (first (n/children (parser/parse-string-all migrated)))) 2))))))))))
(def binding-scope-cases
  [{:id "discard-before-binding" :source "(ns shadow.client (:require [shadow.old :refer [find-event other-event]]))\n(defn result [] [(find-event) (let [#_find-event x 1] (find-event)) (other-event)])\n(result)\n" :expected [:selected :selected :other] :refuse? false}
   {:id "discard-before-initializer" :source "(ns shadow.client (:require [shadow.old :refer [find-event other-event]]))\n(defn result [] [(find-event) (let [x #_find-event 1] (find-event)) (other-event)])\n(result)\n" :expected [:selected :selected :other] :refuse? false}
   {:id "discard-between-binding-pairs" :source "(ns shadow.client (:require [shadow.old :refer [find-event other-event]]))\n(defn result [] [(find-event) (let [x 1 #_find-event y 2] (find-event)) (other-event)])\n(result)\n" :expected [:selected :selected :other] :refuse? false}
   {:id "nested-discard-does-not-shadow" :source "(ns shadow.client (:require [shadow.old :refer [find-event other-event]]))\n(defn result [] [(find-event) (let [x 1] [(let [#_find-event y 2] (find-event)) (find-event)]) (other-event)])\n(result)\n" :expected [:selected [:selected :selected] :other] :refuse? false}
   {:id "binding-metadata-is-not-binding" :source "(ns shadow.client (:require [shadow.old :refer [find-event other-event]]))\n(defn result [] [(find-event) (let [^{:probe find-event} x 1] (find-event)) (other-event)])\n(result)\n" :expected [:selected :selected :other] :refuse? false}
   {:id "metadata-selected-local-control" :source "(ns shadow.client (:require [shadow.old :refer [find-event other-event]]))\n(defn result [] [(find-event) (let [^:private find-event (fn [] :local)] (find-event)) (other-event)])\n(result)\n" :expected [:selected :local :other] :refuse? false}
   {:id "destructured-selected-local-control" :source "(ns shadow.client (:require [shadow.old :refer [find-event other-event]]))\n(defn result [] [(find-event) (let [{:keys [find-event]} {:find-event (fn [] :local)}] (find-event)) (other-event)])\n(result)\n" :expected [:selected :local :other] :refuse? false}
   {:id "destructuring-default-live-call" :source "(ns shadow.client (:require [shadow.old :refer [find-event other-event]]))\n(defn result [] [(find-event) (let [{:keys [x] :or {x (find-event)}} {}] [x (find-event)]) (other-event)])\n(result)\n" :expected [:selected [:selected :selected] :other] :refuse? true}
   {:id "fn-argument-discard" :source "(ns shadow.client (:require [shadow.old :refer [find-event other-event]]))\n(defn result [] [(find-event) ((fn [#_find-event x] (find-event)) 1) (other-event)])\n(result)\n" :expected [:selected :selected :other] :refuse? false}
   {:id "fn-selected-argument-control" :source "(ns shadow.client (:require [shadow.old :refer [find-event other-event]]))\n(defn result [] [(find-event) ((fn [find-event] (find-event)) (fn [] :local)) (other-event)])\n(result)\n" :expected [:selected :local :other] :refuse? false}
   {:id "letfn-discard-before-local" :source "(ns shadow.client (:require [shadow.old :refer [find-event other-event]]))\n(defn result [] [(find-event) (letfn [#_find-event (local [] (find-event))] (local)) (other-event)])\n(result)\n" :expected [:selected :selected :other] :refuse? false}
   {:id "letfn-selected-local-control" :source "(ns shadow.client (:require [shadow.old :refer [find-event other-event]]))\n(defn result [] [(find-event) (letfn [(find-event [] :local)] (find-event)) (other-event)])\n(result)\n" :expected [:selected :local :other] :refuse? false}
   {:id "sequential-selected-initializer" :source "(ns shadow.client (:require [shadow.old :refer [find-event other-event]]))\n(defn result [] [(find-event) (let [find-event (find-event)] find-event) (other-event)])\n(result)\n" :expected [:selected :selected :other] :refuse? false}])
(def binding-scope-request
  {:workspace-root "/workspace"
   :from {:lib "shadow.old" :var "find-event"}
   :to {:lib "shadow.new" :var "fetch-event" :alias-policy ["newlib"]}
   :scope {:paths ["src/**"]} :expect {:files 1}})
(defn binding-scope-candidate [source plan]
  (reduce (fn [text {:keys [original replacement]}]
            (str/replace-first text original replacement))
          source (:edits (first (:files plan)))))
(defn binding-scope-behavior [source retired?]
  ((requiring-resolve 'sci.core/eval-string) source
   {:namespaces {'shadow.old (cond-> {'other-event (fn [] :other)}
                               (not retired?) (assoc 'find-event (fn [] :selected)))
                 'shadow.new {'fetch-event (fn [] :selected)}}}))
(deftest original-independent-fixtures-are-valid
  (doseq [{:keys [id source expected]} binding-scope-cases]
    (testing id (is (= expected (binding-scope-behavior source false))))))
(deftest binding-scope-plans-preserve-behavior-or-refuse-honestly
  (doseq [{:keys [id source expected refuse?]} binding-scope-cases]
    (testing id
      (let [plan (alias-migration/plan binding-scope-request [{:file "src/shadow/client.clj" :source source}])]
        (if refuse?
          (do (is (= false (:ok plan)))
              (is (= "alias-migration-indirect-reference" (:error_type plan)))
              (is (= "unsupported-binding-scope" (:reason plan)))
              (is (= true (:source_unchanged plan)))
              (is (= false (:mutation_attempted plan)))
              (is (empty? (:files plan)))
              (is (nil? (:next_call plan))))
          (do (is (= true (:ok plan)) (pr-str plan))
              (let [after (binding-scope-candidate source plan)]
                (is (= expected (binding-scope-behavior after false)))
                (is (= expected (binding-scope-behavior after true)))
                (when (str/includes? source "#_find-event")
                  (is (str/includes? after "#_find-event")))
                (when (str/includes? source "^{:probe find-event}")
                  (is (str/includes? after "^{:probe find-event}"))))))))))
(deftest unsupported-binding-scopes-refuse-without-changing-authority
  ;; @spec MCP-OP-ALIAS-066
  (doseq [body ["(if-let [find-event nil] (find-event) (find-event))"
                "(if-some [find-event nil] (find-event) (find-event))"
                "(as-> (find-event) find-event find-event)"
                "(for [x [1] :let [find-event (fn [] :local)]] (find-event))"
                "((fn ([find-event] (find-event)) ([] (find-event))))"
                "(letfn [(local [find-event] (find-event)) (other [] (find-event))] (other))"]]
    (let [source (str "(ns shadow.client (:require [shadow.old :refer [find-event other-event]]))\n(defn result [] [(find-event) " body "])")
          plan (alias-migration/plan binding-scope-request [{:file "src/shadow/client.clj" :source source}])]
      (is (= false (:ok plan)) body)
      (is (= "unsupported-binding-scope" (:reason plan)) body)
      (is (= true (:source_unchanged plan)))
      (is (empty? (:files plan)))
      (is (nil? (:next_call plan))))))
(deftest qualified-destructuring-defaults-refuse-without-false-success
  ;; @spec MCP-OP-ALIAS-066
  (let [source "(ns shadow.client (:require [shadow.old :as old]))\n(defn result [] [(old/find-event) (let [{:keys [x] :or {x (old/find-event)}} {}] x)])\n(result)"
        plan (alias-migration/plan binding-scope-request [{:file "src/shadow/client.clj" :source source}])]
    (is (= [:selected :selected] (binding-scope-behavior source false)))
    (is (= false (:ok plan)))
    (is (= "unsupported-binding-scope" (:reason plan)))
    (is (nil? (:next_call plan)))
    (is (empty? (:files plan)))))
(deftest metadata-named-recursive-function-refuses-without-rebinding
  ;; @spec MCP-OP-ALIAS-066
  (let [source "(ns shadow.client (:require [shadow.old :refer [find-event other-event]]))\n(defn result [] [(find-event) ((fn ^:private find-event [n] (if (zero? n) :local (find-event (dec n)))) 1)])\n(result)"
        plan (alias-migration/plan binding-scope-request [{:file "src/shadow/client.clj" :source source}])]
    (is (= [:selected :local] (binding-scope-behavior source false)))
    (is (= false (:ok plan)))
    (is (= "unsupported-binding-scope" (:reason plan)))
    (is (nil? (:next_call plan)))
    (is (empty? (:files plan)))))
(deftest exact-independent-binding-boundary-witnesses
  ;; @spec MCP-OP-ALIAS-066
  ;; Exact independent review originals; preserve unrelated calls in the witness.
  (doseq [[id source expected] [["qualified-only-fn-default" "(ns shadow.client (:require [shadow.old :as old]))\n(defn result [] [(old/find-event) ((fn [{:keys [x] :or {x (old/find-event)}}] x) {})])\n(result)\n" [:selected :selected]] ["metadata-named-recursive-fn" "(ns shadow.client (:require [shadow.old :refer [find-event other-event]]))\n(defn result [] [(find-event) ((fn ^:private find-event [n] (if (zero? n) :local (find-event (dec n)))) 1) (other-event)])\n(result)\n" [:selected :local :other]]]]
    (testing id
      (let [plan (alias-migration/plan binding-scope-request [{:file "src/shadow/client.clj" :source source}])]
        (is (= expected (binding-scope-behavior source false)))
        (is (= false (:ok plan)))
        (is (= "unsupported-binding-scope" (:reason plan)))
        (is (= true (:source_unchanged plan)))
        (is (= false (:mutation_attempted plan)))
        (is (nil? (:next_call plan)))
        (is (empty? (:files plan)))))))
(deftest metadata-parameter-vector-preserves-local-authority-or-refuses
  (let [source "(ns shadow.client (:require [shadow.old :refer [find-event other-event]]))\n(defn result [] [(find-event) ((fn ^:private [find-event] (find-event)) (fn [] :local)) (other-event)])\n(result)\n"
        plan (alias-migration/plan binding-scope-request [{:file "src/shadow/client.clj" :source source}])]
    (is (= [:selected :local :other] (binding-scope-behavior source false)))
    (is (= false (:ok plan)))
    (is (= "unsupported-binding-scope" (:reason plan)))
    (is (empty? (:files plan)))
    (is (nil? (:next_call plan)))))
(deftest lib-only-default-refusal-keeps-no-executable-remedy
  (let [source "(ns shadow.client)\n(defn result [] (let [{:keys [x] :or {x (shadow.old/find-event)}} {}] x))\n(result)"
        req (-> binding-scope-request (assoc-in [:from :var] nil) (assoc-in [:to :var] nil))
        plan (alias-migration/plan req [{:file "src/shadow/client.clj" :source source}])]
    (is (= :selected (binding-scope-behavior source false)))
    (is (= "unsupported-binding-scope" (:reason plan)))
    (is (= false (:ok plan)))
    (is (= true (:source_unchanged plan)))
    (is (empty? (:files plan)))
    (is (nil? (:next_call plan)))
    (is (not (str/includes? (:remedy plan "") "exclude")))))
(deftest lib-only-default-bystander-is-not-migration-authority
  (let [req (-> binding-scope-request (assoc-in [:from :var] nil) (assoc-in [:to :var] nil))
        client "(ns shadow.client (:require [shadow.old :as old]))\n(defn result [] (old/find-event))"
        bystander "(ns shadow.bystander)\n(defn result [] (let [{:keys [x] :or {x :untouched}} {}] x))\n(result)"
        plan (alias-migration/plan req [{:file "src/shadow/client.clj" :source client}
                                        {:file "src/shadow/bystander.clj" :source bystander}])]
    (is (= :untouched (binding-scope-behavior bystander false)))
    (is (= true (:ok plan)))
    (is (= ["src/shadow/client.clj"] (mapv :file (:files plan))))))
