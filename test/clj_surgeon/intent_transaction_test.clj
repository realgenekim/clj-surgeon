(ns clj-surgeon.intent-transaction-test
  (:require
   [babashka.fs :as fs]
   [babashka.process :as proc]
   [clj-surgeon.core :as core]
   [clj-surgeon.intent-transaction :as transaction]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [rewrite-clj.zip :as z]))

(defn- spec
  [intents expected]
  {:intents intents
   :expect expected})

(defn- intent
  [files from to expected-count]
  {:files files
   :from from
   :to to
   :expect-count expected-count})

(defn- valid-source?
  [source]
  (try
    (z/of-string source)
    true
    (catch Exception _ false)))

(deftest compiles-one-model-plan-into-heterogeneous-edits
  (let [sources
        {"src/a.clj"
         "(ns app.a)\n\n(defn page [x]\n  [(old-call x) :body])\n"
         "src/b.clj"
         "(ns app.b)\n\n(defn shell []\n  [:main :body])\n"}
        result
        (transaction/compile-transaction
          sources
          (spec [(intent ["src/a.clj"]
                         "(old-call x)"
                         "#(new-call %)"
                         1)
                 (intent ["src/a.clj" "src/b.clj"]
                         ":body"
                         ":body.ide-shell-page"
                         2)]
                {:intent-count 2
                 :edit-count 3
                 :changed-file-count 2}))]
    (is (= true (:ok result)))
    (is (= :change (:operation result)))
    (is (= 2 (:intent-count result)))
    (is (= 3 (:match-count result)))
    (is (= 2 (:changed-file-count result)))
    (is (= {"src/a.clj" 2 "src/b.clj" 1}
           (into {} (map (juxt :file :match-count) (:files result)))))
    (is (= "(ns app.a)\n\n(defn page [x]\n  [#(new-call %) :body.ide-shell-page])\n"
           (get-in result [:future-sources "src/a.clj"])))
    (is (= "(ns app.b)\n\n(defn shell []\n  [:main :body.ide-shell-page])\n"
           (get-in result [:future-sources "src/b.clj"])))
    (is (every? valid-source? (vals (:future-sources result))))
    (is (str/includes? (:diff result) "#(new-call %)"))
    (is (= {:whole-files-parsed true :file-count 2}
           (:validated result)))))

(deftest matches-lossless-syntax-while-ignoring-whitespace
  (testing "whitespace may vary, but literal replacement spelling is exact"
    (let [result
          (transaction/compile-transaction
            {"src/a.clj" "(ns app.a)\n(def values [(old-call   x) (old-call x)])\n"}
            (spec [(intent ["src/a.clj"]
                           "(old-call x)"
                           "(new-call\n  x)"
                           2)]
                  {:intent-count 1 :edit-count 2 :changed-file-count 1}))]
      (is (:ok result))
      (is (= "(ns app.a)\n(def values [(new-call\n  x) (new-call\n  x)])\n"
             (get-in result [:future-sources "src/a.clj"])))))

  (testing "an undeclared interior comment prevents a destructive match"
    (let [source "(ns app.a)\n(def value (old-call ; keep this reason\n             x))\n"
          sources {"src/a.clj" source}
          result
          (transaction/compile-transaction
            sources
            (spec [(intent ["src/a.clj"] "(old-call x)" "(new-call x)" 1)]
                  {:intent-count 1 :edit-count 1 :changed-file-count 1}))]
      (is (= :expect-count-mismatch (:error-type result)))
      (is (= 0 (:actual-count result)))
      (is (= {"src/a.clj" source} sources))))

  (testing "declaring the interior comment permits the replacement"
    (let [result
          (transaction/compile-transaction
            {"src/a.clj" "(ns app.a)\n(def value (old-call ; keep this reason\n             x))\n"}
            (spec [(intent ["src/a.clj"]
                           "(old-call ; keep this reason\n x)"
                           "(new-call ; replacement keeps the reason\n x)"
                           1)]
                  {:intent-count 1 :edit-count 1 :changed-file-count 1}))]
      (is (:ok result))
      (is (str/includes? (get-in result [:future-sources "src/a.clj"])
                         "; replacement keeps the reason")))))

(deftest exact-intents-do-not-cascade-or-match-textual-lookalikes
  (let [source
        (str "(ns app.a)\n"
             ";; (old-call x) is documentation, not syntax\n"
             "(def text \"(old-call x)\")\n"
             "(def value (old-call x))\n")
        result
        (transaction/compile-transaction
          {"src/a.clj" source}
          (spec [(intent ["src/a.clj"]
                         "(old-call x)"
                         "(wrapper (old-call x))"
                         1)]
                {:intent-count 1 :edit-count 1 :changed-file-count 1}))
        future (get-in result [:future-sources "src/a.clj"])]
    (is (:ok result))
    (is (= 3 (count (re-seq #"\(old-call x\)" future))))
    (is (str/includes? future ";; (old-call x) is documentation"))
    (is (str/includes? future "\"(old-call x)\""))
    (is (str/includes? future "(wrapper (old-call x))"))))

(deftest refuses-every-contract-error-as-data
  (let [sources {"src/a.clj" "(ns app.a)\n(def value (old-call x))\n"}
        valid-intent (intent ["src/a.clj"] "(old-call x)" "(new-call x)" 1)
        valid-expect {:intent-count 1 :edit-count 1 :changed-file-count 1}
        cases
        [{:label "empty intents"
          :spec (spec [] {:intent-count 0 :edit-count 0 :changed-file-count 0})
          :error :invalid-intents}
         {:label "missing source"
          :spec (spec [(intent ["src/missing.clj"] "(old-call x)" "(new-call x)" 1)]
                      valid-expect)
          :error :invalid-source}
         {:label "invalid before form"
          :spec (spec [(intent ["src/a.clj"] "(old-call" "(new-call x)" 1)]
                      valid-expect)
          :error :invalid-intent-form}
         {:label "two before forms"
          :spec (spec [(intent ["src/a.clj"] "(old-call x) extra" "(new-call x)" 1)]
                      valid-expect)
          :error :invalid-intent-form}
         {:label "detached comment before before-form"
          :spec (spec [(intent ["src/a.clj"]
                               "; explain\n(old-call x)"
                               "(new-call x)"
                               1)]
                      valid-expect)
          :error :invalid-intent-form}
         {:label "detached comment before replacement"
          :spec (spec [(intent ["src/a.clj"]
                               "(old-call x)"
                               "; explain\n(new-call x)"
                               1)]
                      valid-expect)
          :error :invalid-intent-form}
         {:label "lossless no-op"
          :spec (spec [(intent ["src/a.clj"] "(old-call x)" "(old-call   x)" 1)]
                      valid-expect)
          :error :no-op-intent}
         {:label "zero expected count"
          :spec (spec [(assoc valid-intent :expect-count 0)] valid-expect)
          :error :invalid-expect-count}
         {:label "per-intent mismatch"
          :spec (spec [(assoc valid-intent :expect-count 2)]
                      (assoc valid-expect :edit-count 2))
          :error :expect-count-mismatch}
         {:label "aggregate intent mismatch"
          :spec (spec [valid-intent] (assoc valid-expect :intent-count 2))
          :error :transaction-expectation-mismatch}
         {:label "aggregate edit mismatch"
          :spec (spec [valid-intent] (assoc valid-expect :edit-count 2))
          :error :transaction-expectation-mismatch}
         {:label "aggregate file mismatch"
          :spec (spec [valid-intent] (assoc valid-expect :changed-file-count 2))
          :error :transaction-expectation-mismatch}]]
    (doseq [{:keys [label spec error]} cases]
      (testing label
        (let [result (transaction/compile-transaction sources spec)]
          (is (= error (:error-type result)))
          (is (nil? (:future-sources result))))))))

(deftest refuses-overlapping-intents-before-producing-future-source
  (let [sources
        {"src/a.clj"
         "(ns app.a)\n(def value (outer (inner x)))\n"}
        result
        (transaction/compile-transaction
          sources
          (spec [(intent ["src/a.clj"] "(outer (inner x))" "(outer (new x))" 1)
                 (intent ["src/a.clj"] "(inner x)" "(new x)" 1)]
                {:intent-count 2 :edit-count 2 :changed-file-count 1}))]
    (is (= :overlapping-intents (:error-type result)))
    (is (= "src/a.clj" (:file result)))
    (is (= #{0 1} (set (:intent-indexes result))))
    (is (nil? (:future-sources result)))))

(deftest metadata-reader-syntax-and-token-spelling-are-exact
  (let [source
        "(ns app.a)\n(def values [^String x ^{:tag String} x #_ignored 1 1N])\n"
        result
        (transaction/compile-transaction
          {"src/a.clj" source}
          (spec [(intent ["src/a.clj"] "^String x" "^String renamed" 1)
                 (intent ["src/a.clj"] "1N" "2N" 1)]
                {:intent-count 2 :edit-count 2 :changed-file-count 1}))
        future (get-in result [:future-sources "src/a.clj"])]
    (is (:ok result))
    (is (str/includes? future "^String renamed"))
    (is (str/includes? future "^{:tag String} x"))
    (is (str/includes? future "#_ignored 1 2N"))))

(deftest change-preview-reads-each-file-once-and-omits-full-future-source
  (let [reads (atom [])
        canonical-file (.getCanonicalPath (java.io.File. "src/a.clj"))
        sources {canonical-file "(ns app.a)\n(def value [(old x) :body])\n"}
        change-spec
        (spec [(intent ["src/a.clj"] "(old x)" "(new x)" 1)
               (intent ["src/a.clj"] ":body" ":body.page" 1)]
              {:intent-count 2 :edit-count 2 :changed-file-count 1})]
    (with-redefs [clojure.core/slurp
                  (fn [file]
                    (swap! reads conj file)
                    (get sources file))]
      (let [result (transaction/plan-change {:op :change :spec change-spec})]
        (is (:ok result))
        (is (= [canonical-file] @reads))
        (is (nil? (:future-sources result)))
        (is (= 2 (:match-count result)))
        (is (= 2 (count (get-in result [:files 0 :edits]))))
        (is (every? #(not-any? (set (keys %)) [:before :after])
                    (get-in result [:files 0 :edits]))))))

  (testing "unknown CLI arguments refuse before source reads"
    (let [read? (atom false)
          result (with-redefs [clojure.core/slurp
                               (fn [_]
                                 (reset! read? true)
                                 "")]
                   (transaction/plan-change
                     {:op :change
                      :spec {:intents [] :expect {}}
                      :surprise true}))]
      (is (= :unknown-arguments (:error-type result)))
      (is (false? @read?))))

  (testing "malformed specs refuse before source reads"
    (let [read? (atom false)
          malformed
          [{:spec {:intents {} :expect {}}
            :error :invalid-intents}
           {:spec {:intents [(assoc (intent ["src/a.clj"]
                                            "(old x)" "(new x)" 1)
                                    :surprise true)]
                   :expect {:intent-count 1 :edit-count 1
                            :changed-file-count 1}}
            :error :unknown-intent-arguments}
           {:spec {:intents [(intent "src/a.clj"
                                     "(old x)" "(new x)" 1)]
                   :expect {:intent-count 1 :edit-count 1
                            :changed-file-count 1}}
            :error :invalid-files}
           {:spec {:intents [(intent ["src/a.clj"]
                                     "(old x)" "(new x)" 1)]
                   :expect {:intent-count 1 :edit-count 1
                            :changed-file-count 1}
                   :surprise true}
            :error :unknown-transaction-arguments}]]
      (with-redefs [clojure.core/slurp
                    (fn [_]
                      (reset! read? true)
                      "")]
        (doseq [{:keys [spec error]} malformed]
          (is (= error
                 (:error-type
                   (transaction/plan-change {:op :change :spec spec}))))))
      (is (false? @read?)))))

(deftest change-preview-canonicalizes-one-physical-file-across-intents
  (let [temp-dir (fs/create-temp-dir {:prefix "intent-path-alias-"})
        canonical-file (str (fs/path temp-dir "sample.clj"))
        aliased-file (str (fs/path temp-dir "." "sample.clj"))
        source "(ns sample)\n(def value [(old x) :body])\n"
        change-spec
        (spec [(intent [canonical-file] "(old x)" "(new x)" 1)
               (intent [aliased-file] ":body" ":body.page" 1)]
              {:intent-count 2 :edit-count 2 :changed-file-count 1})]
    (try
      (spit canonical-file source)
      (let [result (transaction/plan-change {:op :change :spec change-spec})]
        (is (:ok result))
        (is (= 1 (count (:files result))))
        (is (= (.getCanonicalPath (java.io.File. canonical-file))
               (get-in result [:files 0 :file])))
        (is (= 2 (get-in result [:files 0 :match-count])))
        (is (= source (slurp canonical-file))))
      (finally
        (fs/delete-tree temp-dir)))))

(deftest intent-order-does-not-change-compiled-future-files
  (let [sources {"src/a.clj" "(ns app.a)\n(def value [(old x) :body])\n"}
        first-intent (intent ["src/a.clj"] "(old x)" "(new x)" 1)
        second-intent (intent ["src/a.clj"] ":body" ":body.page" 1)
        expected {:intent-count 2 :edit-count 2 :changed-file-count 1}
        forward (transaction/compile-transaction
                  sources (spec [first-intent second-intent] expected))
        reverse (transaction/compile-transaction
                  sources (spec [second-intent first-intent] expected))]
    (is (:ok forward))
    (is (:ok reverse))
    (is (= (:future-sources forward) (:future-sources reverse)))
    (is (= (mapv :result-hash (:files forward))
           (mapv :result-hash (:files reverse))))))

(deftest intents-match-only-original-snapshots-and-never-cascade
  (let [source "(ns app.a)\n(def values [(old x) (new y)])\n"
        result
        (transaction/compile-transaction
          {"src/a.clj" source}
          (spec [(intent ["src/a.clj"] "(old x)" "(new x)" 1)
                 (intent ["src/a.clj"] "(new y)" "(final y)" 1)]
                {:intent-count 2 :edit-count 2 :changed-file-count 1}))
        future (get-in result [:future-sources "src/a.clj"])]
    (is (:ok result))
    (is (= "(ns app.a)\n(def values [(new x) (final y)])\n" future))
    (is (str/includes? future "(new x)"))
    (is (not (str/includes? future "(final x)")))))

(deftest real-program-ui-plan-compiles-six-coordinated-edits-once
  (let [shell-file "test/fixtures/intent_transaction/app_shell.clj"
        reader-file "test/fixtures/intent_transaction/source_reader.clj"
        original-shell (slurp shell-file)
        original-reader (slurp reader-file)
        change-spec
        (spec
          [(intent [shell-file reader-file]
                   ":body" ":body.ide-shell-page" 2)
           (intent [reader-file]
                   "[project-id projects artifact current-location reader-region show-all?]"
                   "[project-id projects artifact document-title current-location reader-region show-all?]"
                   1)
           (intent [reader-file]
                   "[:title \"Workbench\"]"
                   "[:title (str document-title \" — Workbench\")]"
                   1)
           (intent [reader-file]
                   "[:span.tab-label artifact]"
                   "[:span.tab-label {:title artifact} document-title]"
                   1)
           (intent [shell-file]
                   "#(str \"/assets\" %)"
                   "(partial str \"/assets\")"
                   1)]
          {:intent-count 5 :edit-count 6 :changed-file-count 2})
        result (transaction/plan-change {:op :change :spec change-spec})]
    (is (:ok result))
    (is (= 5 (:intent-count result)))
    (is (= 6 (:match-count result)))
    (is (= 2 (:changed-file-count result)))
    (is (= [2 4] (mapv :match-count (:files result))))
    (is (= {:whole-files-parsed true :file-count 2}
           (:validated result)))
    (is (str/includes? (:diff result) "(partial str \"/assets\")"))
    (is (str/includes? (:diff result)
                       "[:title (str document-title \" — Workbench\")]"))
    (is (not (str/includes? (:diff result)
                            "The text :body is not a structural tag.")))
    (is (= original-shell (slurp shell-file)))
    (is (= original-reader (slurp reader-file)))))

(defn- compiled-two-file-change
  []
  (transaction/compile-transaction
    {"src/a.clj" "(ns app.a)\n(def value (old x))\n"
     "src/b.clj" "(ns app.b)\n(def value :body)\n"}
    (spec [(intent ["src/a.clj"] "(old x)" "(new x)" 1)
           (intent ["src/b.clj"] ":body" ":body.page" 1)]
          {:intent-count 2 :edit-count 2 :changed-file-count 2})))

(defn- memory-io
  [state write-fn]
  {:read-source (fn [file] (get @state file))
   :write-source! (fn [file source]
                    (write-fn state file source))})

(deftest commit-protocol-updates-and-verifies-every-file
  (let [compiled (compiled-two-file-change)
        originals (:original-sources compiled)
        futures (:future-sources compiled)
        state (atom originals)
        writes (atom [])
        result
        (transaction/commit-compiled!
          compiled
          (memory-io state
                     (fn [state file source]
                       (swap! writes conj file)
                       (swap! state assoc file source))))]
    (is (:ok result))
    (is (= :change! (:operation result)))
    (is (:committed result))
    (is (= ["src/a.clj" "src/b.clj"] @writes))
    (is (= futures @state))
    (is (= (into {} (map (juxt :file :result-hash) (:files compiled)))
           (get-in result [:verified :read-back-hashes])))
    (is (= {:whole-files true :file-count 2}
           (select-keys (:verified result) [:whole-files :file-count])))))

(deftest commit-protocol-refuses-stale-source-before-any-write
  (let [compiled (compiled-two-file-change)
        state (atom (assoc (:original-sources compiled)
                           "src/b.clj" "(ns app.b)\n(def value :changed-elsewhere)\n"))
        writes (atom 0)
        result
        (transaction/commit-compiled!
          compiled
          (memory-io state
                     (fn [state file source]
                       (swap! writes inc)
                       (swap! state assoc file source))))]
    (is (= :source-hash-mismatch (:error-type result)))
    (is (= "src/b.clj" (:file result)))
    (is (zero? @writes))
    (is (= "(ns app.a)\n(def value (old x))\n"
           (get @state "src/a.clj")))
    (is (= "(ns app.b)\n(def value :changed-elsewhere)\n"
           (get @state "src/b.clj")))))

(deftest commit-protocol-rolls-back-a-partial-write
  (let [compiled (compiled-two-file-change)
        originals (:original-sources compiled)
        state (atom originals)
        write-count (atom 0)
        result
        (transaction/commit-compiled!
          compiled
          (memory-io state
                     (fn [state file source]
                       (swap! state assoc file source)
                       (when (= 2 (swap! write-count inc))
                         (throw (ex-info "injected second-write failure"
                                         {:error-type :injected-write-failure}))))))]
    (is (= :transaction-write-failed (:error-type result)))
    (is (= :injected-write-failure (:cause-error-type result)))
    (is (true? (:rolled-back result)))
    (is (= originals @state))
    (is (= {"src/a.clj" :restored "src/b.clj" :restored}
           (into {} (map (juxt :file :status) (:recovery result)))))))

(deftest commit-protocol-reports-recovery-required-without-clobbering-concurrent-source
  (let [compiled (compiled-two-file-change)
        originals (:original-sources compiled)
        concurrent "(ns app.b)\n(def value :concurrent-user-change)\n"
        state (atom originals)
        write-count (atom 0)
        result
        (transaction/commit-compiled!
          compiled
          (memory-io state
                     (fn [state file source]
                       (swap! state assoc file source)
                       (when (= 1 (swap! write-count inc))
                         (swap! state assoc "src/b.clj" concurrent)))))]
    (is (= :transaction-recovery-required (:error-type result)))
    (is (false? (:rolled-back result)))
    (is (= "src/b.clj" (:file result)))
    (is (= originals
           (assoc @state "src/b.clj" (get originals "src/b.clj"))))
    (is (= concurrent (get @state "src/b.clj")))
    (is (= :unexpected-source
           (:status (first (filter #(= "src/b.clj" (:file %))
                                   (:recovery result))))))))

(deftest commit-protocol-rolls-back-after-read-back-corruption
  (let [compiled (compiled-two-file-change)
        originals (:original-sources compiled)
        state (atom originals)
        corrupt-read-once? (atom true)
        b-result (get (:future-sources compiled) "src/b.clj")
        result
        (transaction/commit-compiled!
          compiled
          {:read-source
           (fn [file]
             (let [source (get @state file)]
               (if (and (= "src/b.clj" file)
                        (= b-result source)
                        (compare-and-set! corrupt-read-once? true false))
                 (str source "\n; corrupted read")
                 source)))
           :write-source! (fn [file source]
                            (swap! state assoc file source))})]
    (is (= :transaction-write-failed (:error-type result)))
    (is (= :read-back-hash-mismatch (:cause-error-type result)))
    (is (true? (:rolled-back result)))
    (is (= originals @state))))

(deftest commit-protocol-makes-rollback-failure-explicit
  (let [compiled (compiled-two-file-change)
        originals (:original-sources compiled)
        futures (:future-sources compiled)
        state (atom originals)
        forward-failed? (atom false)
        result
        (transaction/commit-compiled!
          compiled
          (memory-io
            state
            (fn [state file source]
              (cond
                (and (= "src/b.clj" file)
                     (= source (get futures file))
                     (compare-and-set! forward-failed? false true))
                (do
                  (swap! state assoc file source)
                  (throw (ex-info "injected forward failure"
                                  {:error-type :injected-write-failure})))

                (and (= "src/b.clj" file)
                     (= source (get originals file)))
                (throw (ex-info "injected rollback failure"
                                {:error-type :injected-rollback-failure}))

                :else
                (swap! state assoc file source)))))]
    (is (= :transaction-recovery-required (:error-type result)))
    (is (false? (:rolled-back result)))
    (is (= (get originals "src/a.clj") (get @state "src/a.clj")))
    (is (= (get futures "src/b.clj") (get @state "src/b.clj")))
    (is (= :restore-failed
           (:status (first (filter #(= "src/b.clj" (:file %))
                                   (:recovery result))))))))

(deftest change-help-teaches-one-plan-materialization
  (let [op-def (get core/ops-registry :change)
        help (core/format-op-help :change op-def)
        global-help (core/format-global-help core/ops-registry)]
    (is (= :change (core/resolve-op :change)))
    (is (str/includes? global-help "    change"))
    (is (str/includes? help "complete mechanical model plan"))
    (is (str/includes? help "writes nothing"))
    (is (str/includes? help "heterogeneous model plan"))
    (is (str/includes? help ":intent-count"))
    (is (str/includes? help ":changed-file-count"))))

(deftest change-cli-previews-real-files-and-refuses-with-nonzero-exit
  (let [temp-dir (fs/create-temp-dir {:prefix "intent-change-cli-"})
        file (str (fs/path temp-dir "sample.clj"))
        source "(ns sample)\n(defn page [x] [(old x) :body])\n"
        change-spec
        (spec [(intent [file] "(old x)" "#(new %)" 1)
               (intent [file] ":body" ":body.page" 1)]
              {:intent-count 2 :edit-count 2 :changed-file-count 1})]
    (try
      (spit file source)
      (let [{:keys [exit out err]}
            @(proc/process ["bb" "-m" "clj-surgeon.core"
                            ":op" ":change"
                            ":spec" (pr-str change-spec)]
                           {:out :string :err :string})
            result (edn/read-string out)]
        (is (= 0 exit))
        (is (= "" err))
        (is (:ok result))
        (is (= 2 (:match-count result)))
        (is (= 1 (:changed-file-count result)))
        (is (str/includes? (:diff result) "#(new %)"))
        (is (nil? (:future-sources result)))
        (is (= source (slurp file))))

      (let [wrong-spec
            (assoc-in change-spec [:intents 0 :expect-count] 2)
            {:keys [exit out err]}
            @(proc/process ["bb" "-m" "clj-surgeon.core"
                            ":op" ":change"
                            ":spec" (pr-str wrong-spec)]
                           {:out :string :err :string})
            result (edn/read-string out)]
        (is (= 1 exit))
        (is (= "" err))
        (is (= :expect-count-mismatch (:error-type result)))
        (is (= 0 (:intent-index result)))
        (is (= 1 (:actual-count result)))
        (is (= source (slurp file))))
      (finally
        (fs/delete-tree temp-dir)))))
