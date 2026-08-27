(ns clj-surgeon.move-dependency-test
  (:require
   [clj-surgeon.analyze :as analyze]
   [clj-surgeon.mcp-process :as process-env]
   [clj-surgeon.move :as move]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [rewrite-clj.zip :as z]))

(defn- position [source form-prefix]
  (str/index-of source form-prefix))

(defn- before? [source left right]
  (< (position source left) (position source right)))

(defn- parseable? [source]
  (try
    (z/of-string source)
    true
    (catch Exception _ false)))

(defn- stranded-names [result]
  (mapv :name (:stranded result)))

(defn- cold-lints? [source filename]
  (let [command ["clj-kondo" "--lint" "-"
                 "--filename" filename
                 "--fail-level" "error"]
        result (process-env/run-bounded!
                 {:command command
                  :cwd (System/getProperty "user.dir")
                  :timeout-ms 120000
                  :stdin-text source})]
    (and (= :admitted (get-in result [:admission :status]))
         (:finished? result)
         (zero? (:exit result)))))

(deftest issue-20-plain-move-refuses-and-recommends-mv-with-deps
  (let [source (slurp "test-fixtures/mv/mothership_stranded_dep.clj")
        result (move/plan-move source {:file "test-fixtures/mv/mothership_stranded_dep.clj"
                                       :form "walk-files"
                                       :before "run-kondo"})]
    (is (= :would-strand-dependencies (:error-type result)))
    (is (= ["skip-dirs"] (stranded-names result)))
    (is (= [{:name "skip-dirs"
             :defined-at 11
             :would-be-at 14
             :required-before 8}]
           (:stranded result)))
    (is (= :preview-dependency-closure (:recommended-action result)))
    (is (str/includes? (:recommended-command result) ":op :mv-with-deps"))
    (is (str/includes? (:recommended-command result) ":form walk-files"))
    (is (str/ends-with? (:recommended-command result) ":dry-run true"))
    (is (str/includes? (:apply-command result) ":op :mv-with-deps"))
    (is (not (str/includes? (:apply-command result) ":dry-run")))
    (is (= "walk-files" (:form result)))
    (is (= "run-kondo" (:before result)))
    (is (= :up (:direction result)))
    (is (not (contains? result :result)))))

(deftest issue-20-with-deps-moves-minimum-disclosed-closure
  (let [source (slurp "test-fixtures/mv/mothership_stranded_dep.clj")
        result (move/plan-move source {:file "test-fixtures/mv/mothership_stranded_dep.clj"
                                       :form "walk-files"
                                       :before "run-kondo"
                                       :with-deps true})
        moved (:result result)]
    (is (:ok result))
    (is (= ["skip-dirs"] (get-in result [:plan :added-forms])))
    (is (= ["skip-dirs" "walk-files"] (get-in result [:plan :move-order])))
    (is (str/includes? (:apply-command result) ":op :mv-with-deps"))
    (is (not (str/includes? (:apply-command result) ":dry-run")))
    (is (= 4 (get-in result [:plan :lines-moved])))
    (is (= 64 (count (get-in result [:plan :source-hash]))))
    (is (= 64 (count (get-in result [:plan :result-hash]))))
    (is (not= (get-in result [:plan :source-hash])
              (get-in result [:plan :result-hash])))
    (is (str/includes? (get-in result [:plan :diff]) "--- "))
    (is (str/includes? (get-in result [:plan :diff]) "+++ "))
    (is (before? moved "(def skip-dirs" "(defn walk-files"))
    (is (before? moved "(defn walk-files" "(defn run-kondo"))
    (is (parseable? moved))))

(deftest writer-state-real-chain-pulls-transitive-dependencies
  (let [source (slurp "test-fixtures/mv/writer_state_chain.clj")
        result (move/plan-move source {:form "transition!"
                                       :before "dispatch!"
                                       :with-deps true})
        moved (:result result)]
    (is (:ok result))
    (is (= ["app-state" "log-event!"] (get-in result [:plan :added-forms])))
    (is (= ["app-state" "log-event!" "transition!"]
           (get-in result [:plan :move-order])))
    (is (before? moved "(def app-state" "(defn log-event!"))
    (is (before? moved "(defn log-event!" "(defn transition!"))
    (is (before? moved "(defn transition!" "(defn dispatch!"))))

(deftest satisfied-dependencies-do-not-widen-the-move
  (let [source "(ns x)\n\n(def config 1)\n\n(defn destination [] nil)\n\n(defn worker [] config)\n"
        result (move/plan-move source {:form "worker" :before "destination"
                                       :with-deps true})]
    (is (:ok result))
    (is (= [] (get-in result [:plan :added-forms])))
    (is (= ["worker"] (get-in result [:plan :move-order])))
    (is (before? (:result result) "(def config" "(defn worker"))))

(deftest shared-dependency-can-move-earlier-without-breaking-other-users
  (let [source "(ns x)\n\n(declare worker)\n\n(defn destination [] (worker))\n\n(def shared 1)\n\n(defn other-user [] shared)\n\n(defn worker [] shared)\n"
        result (move/plan-move source {:form "worker" :before "destination"
                                       :with-deps true})
        moved (:result result)]
    (is (:ok result))
    (is (= ["shared"] (get-in result [:plan :added-forms])))
    (is (before? moved "(def shared" "(defn destination"))
    (is (before? moved "(def shared" "(defn other-user"))))

(deftest existing-declare-satisfies-dependency-at-destination
  (let [source "(ns x)\n\n(declare dependency)\n\n(defn destination [] nil)\n\n(defn dependency [] :ok)\n\n(defn worker [] (dependency))\n"
        result (move/plan-move source {:form "worker" :before "destination"})]
    (is (:ok result))
    (is (= [] (get-in result [:plan :added-forms])))))

(deftest unchanged-pre-existing-forward-reference-does-not-block-safe-move
  (let [source "(ns x)\n\n(defn old-user [] (old-dep))\n\n(defn destination [] nil)\n\n(defn safe [] :safe)\n\n(defn old-dep [] :old)\n"
        result (move/plan-move source {:form "safe" :before "destination"})]
    (is (:ok result))
    (is (before? (:result result) "(defn safe" "(defn destination"))))

(deftest downward-move-that-strands-caller-refuses-with-user-diagnostic
  (let [source "(ns x)\n\n(defn callee [] :ok)\n\n(defn caller [] (callee))\n\n(defn tail [] nil)\n"
        result (move/plan-move source {:form "callee" :before "tail"})]
    (is (= :would-strand-users (:error-type result)))
    (is (= ["caller"] (mapv :name (:stranded-users result))))))

(deftest with-deps-does-not-move-callers-on-a-downward-move
  (let [source "(ns x)\n\n(defn callee [] :ok)\n\n(defn caller [] (callee))\n\n(defn tail [] nil)\n"
        result (move/plan-move source {:form "callee" :before "tail"
                                       :with-deps true})]
    (is (= :would-strand-users (:error-type result)))
    (is (nil? (:result result)))))

(deftest transitive-cycle-fails-closed
  (let [source "(ns x)\n\n(declare a)\n\n(defn destination [] (a))\n\n(declare b)\n\n(defn a [] (b))\n\n(defn b [] (a))\n"
        result (move/plan-move source {:form "a" :before "destination"
                                       :with-deps true})]
    (is (= :cyclic-move-dependencies (:error-type result)))
    (is (= #{"a" "b"} (set (:cycle result))))))

(deftest local-shadowing-and-quoted-data-are-not-dependencies
  (let [shadow-source "(ns x)\n\n(defn destination [] nil)\n\n(def config 1)\n\n(defn worker [config] config)\n"
        quote-source "(ns x)\n\n(defn destination [] nil)\n\n(def config 1)\n\n(defn worker [] 'config)\n"]
    (doseq [source [shadow-source quote-source]]
      (let [result (move/plan-move source {:form "worker" :before "destination"})]
        (is (:ok result))
        (is (= [] (get-in result [:plan :added-forms])))))))

(deftest dependency-analysis-excludes-local-and-quoted-symbols
  (doseq [source ["(ns x) (def config 1) (defn worker [config] config)"
                  "(ns x) (def config 1) (defn worker [] 'config)"
                  "(ns x) (def config 1) (defn worker [] (let [config 2] config))"
                  "(ns x) (def config 1) (defn worker [{:keys [config]}] config)"
                  "(ns x) (def config 1) (defn worker [{:person/keys [config]}] config)"
                  "(ns x) (def config 1) (>defn worker [config] [int? => int?] config)"
                  "(ns x) (def config 1) (mu/defn worker :- int? [config :- int?] config)"]]
    (let [deps (analyze/intra-ns-deps (analyze/string->zloc source))
          worker (first (filter #(= "worker" (:name %)) deps))]
      (is (= #{} (:depends-on worker))))))

(deftest destructuring-default-remains-a-real-dependency
  (let [source "(ns x)\n\n(def default-value 1)\n\n(defn worker [{:keys [value] :or {value default-value}}] value)\n"
        deps (analyze/intra-ns-deps (analyze/string->zloc source))
        worker (first (filter #(= "worker" (:name %)) deps))]
    (is (= #{"default-value"} (:depends-on worker)))))

(deftest eager-def-and-macro-dependencies-are-pulled-before-their-users
  (let [eager-source "(ns x)\n\n(declare derived)\n\n(defn destination [] derived)\n\n(def base 1)\n\n(def derived (inc base))\n"
        macro-source "(ns x)\n\n(declare worker)\n\n(defn destination [] (worker))\n\n(defmacro answer [] 42)\n\n(defn worker [] (answer))\n"]
    (doseq [[source form expected-order]
            [[eager-source "derived" ["base" "derived"]]
             [macro-source "worker" ["answer" "worker"]]]]
      (let [result (move/plan-move source {:form form
                                           :before "destination"
                                           :with-deps true})]
        (is (:ok result))
        (is (= expected-order (get-in result [:plan :move-order])))
        (is (cold-lints? (:result result) "x.clj"))))))

(deftest missing-source-and-destination-errors-remain-stable
  (let [source "(ns x)\n\n(defn present [] nil)\n"]
    (is (= {:error "Form not found: absent"
            :error-type :form-not-found
            :form "absent"}
           (move/plan-move source {:form "absent" :before "present"})))
    (is (= {:error "Destination form not found: absent"
            :error-type :destination-form-not-found
            :form "absent"}
           (move/plan-move source {:form "present" :before "absent"})))))

(deftest real-program-fixtures-and-transformed-candidates-cold-lint
  (doseq [[fixture form before]
          [["test-fixtures/mv/mothership_stranded_dep.clj"
            "walk-files" "run-kondo"]
           ["test-fixtures/mv/writer_state_chain.clj"
            "transition!" "dispatch!"]]]
    (let [source (slurp fixture)
          result (move/plan-move source {:file fixture
                                         :form form
                                         :before before
                                         :with-deps true})]
      (testing (str fixture " valid baseline")
        (is (cold-lints? source fixture)))
      (testing (str fixture " valid transformed candidate")
        (is (:ok result))
        (is (cold-lints? (:result result) fixture))))))

(deftest ambiguous-definitions-fail-closed
  (let [source "(ns x)\n\n(defn destination [] nil)\n\n(defn worker [] 1)\n\n(defn worker [] 2)\n"
        result (move/plan-move source {:form "worker" :before "destination"})]
    (is (= :ambiguous-form (:error-type result)))
    (is (= 2 (:match-count result)))))

(deftest no-op-is-stable-and-does-not-reformat
  (let [source "(ns x)\n\n;; Header\n(defn worker [] :ok)\n"
        result (move/plan-move source {:form "worker" :before "worker"})]
    (is (:ok result))
    (is (:no-op (get result :plan)))
    (is (str/includes? (:apply-command result) ":op :mv"))
    (is (= source (:result result)))))

(deftest forms-sharing-a-physical-line-fail-closed
  (let [source "(ns x)\n\n(defn destination [] nil) (defn worker [] :ok)\n"
        result (move/plan-move source {:form "worker" :before "destination"})]
    (is (= :unsupported-source-layout (:error-type result)))
    (is (= 3 (:line result)))
    (is (= ["worker" "destination"] (:forms result)))
    (is (nil? (:result result)))))

(deftest comments-and-metadata-travel-with-expanded-moves
  (let [source "(ns x)\n\n(declare worker)\n\n(defn destination [] (worker))\n\n;; Configuration\n(def ^:private config 1)\n\n;; Worker\n(defn ^:private worker [] config)\n"
        result (move/plan-move source {:form "worker" :before "destination"
                                       :with-deps true})
        moved (:result result)]
    (is (:ok result))
    (is (str/includes? moved ";; Configuration\n(def ^:private config"))
    (is (str/includes? moved ";; Worker\n(defn ^:private worker"))
    (is (before? moved ";; Configuration" ";; Worker"))
    (is (before? moved ";; Worker" "(defn destination"))))

(deftest clj-surgeon-can-plan-a-guarded-move-on-its-own-real-source
  (let [source (slurp "src/clj_surgeon/analyze.clj")
        result (move/plan-move source {:file "src/clj_surgeon/analyze.clj"
                                       :form "topological-sort"
                                       :before "symbols-in-form"})]
    (is (= :would-strand-dependencies (:error-type result)))
    (is (some #{"intra-ns-deps"} (stranded-names result)))))

(deftest file-wrapper-refusal-preserves-bytes
  (let [source (slurp "test-fixtures/mv/mothership_stranded_dep.clj")
        tmp (java.io.File/createTempFile "clj-surgeon-mv-guard" ".clj")]
    (spit tmp source)
    (try
      (let [result (move/move-form {:file (.getAbsolutePath tmp)
                                    :form "walk-files"
                                    :before "run-kondo"})]
        (is (= :would-strand-dependencies (:error-type result)))
        (is (= source (slurp tmp))))
      (finally (.delete tmp)))))

(deftest file-wrapper-dry-run-discloses-expanded-scope-without-writing
  (let [source (slurp "test-fixtures/mv/mothership_stranded_dep.clj")
        tmp (java.io.File/createTempFile "clj-surgeon-mv-dry" ".clj")]
    (spit tmp source)
    (try
      (let [result (move/move-form {:file (.getAbsolutePath tmp)
                                    :form "walk-files"
                                    :before "run-kondo"
                                    :with-deps true
                                    :dry-run true})]
        (is (:ok result))
        (is (= ["skip-dirs"] (get-in result [:plan :added-forms])))
        (is (nil? (:result result)))
        (is (= source (slurp tmp))))
      (finally (.delete tmp)))))
