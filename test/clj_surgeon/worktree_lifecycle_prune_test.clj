(ns clj-surgeon.worktree-lifecycle-prune-test
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]))

(def oid-a (apply str (repeat 40 "a")))
(def oid-b (apply str (repeat 40 "b")))
(def sha-a (apply str (repeat 64 "a")))

(def missing-row
  {:path-lexical "/repo-stale"
   :path-state :absent
   :path-real nil
   :nearest-existing-parent {:path "/" :device 1 :inode 2}
   :head oid-a
   :tree oid-b
   :branch "refs/heads/stale"
   :detached false
   :locked false
   :lock-reason nil
   :prunable "gitdir file points to non-existent location"
   :status :not-applicable
   :removal-preflight :not-applicable})

(def preservation
  {:kind :branch-tip-on-remote
   :local-ref "refs/heads/stale"
   :remote "origin"
   :remote-url-sha256 sha-a
   :ref "refs/heads/stale"
   :object oid-a
   :peeled-object nil})

(def prune-request
  {:schema :clj-surgeon.worktree-registration-prune-request/v1
   :target "/repo-stale"
   :preservation preservation})

(def controller-identity
  {:commit oid-b
   :tree oid-b
   :artifacts {"Makefile" sha-a
               "src/clj_surgeon/worktree_lifecycle.clj" sha-a
               "src/clj_surgeon/worktree_lifecycle_io.clj" sha-a}
   :clean true})

(def prune-snapshot
  {:schema :clj-surgeon.worktree-lifecycle-snapshot/v1
   :captured-at "2026-09-01T12:00:00Z"
   :repository {:root "/repo"
                :common-git-dir "/repo/.git"
                :primary-worktree "/repo"
                :object-format :sha1}
   :controller-worktree "/controller"
   :git-worktrees [missing-row]
   :supacode {:available true :worktrees []}
   :remotes {:available true
             :rows [{:remote "origin"
                     :remote-url-sha256 sha-a
                     :ref "refs/heads/stale"
                     :object oid-a
                     :peeled-object nil}]}
   :ancestry #{}
   :handoffs {}
   :lifecycle-leases {}})

(defn- public-var [symbol]
  (try
    (requiring-resolve symbol)
    (catch Throwable _ nil)))

(defn- invoke [symbol & args]
  (if-let [f (public-var symbol)]
    (try
      (apply f args)
      (catch clojure.lang.ExceptionInfo error
        (merge {:ok false} (ex-data error)))
      (catch Throwable error
        {:ok false :error-type :unexpected-throw :class (class error)}))
    ::missing))

(defn- rehash-plan [plan]
  (let [canonical (public-var 'clj-surgeon.worktree-lifecycle/canonical-edn)
        sha256 (public-var 'clj-surgeon.worktree-lifecycle/sha256)]
    (assoc plan :plan-sha256
           (sha256 (canonical (dissoc plan :plan-sha256))))))

(defn- run-command! [dir & args]
  (let [builder (doto (ProcessBuilder. ^java.util.List (mapv str args))
                  (.directory (io/file dir))
                  (.redirectErrorStream true))
        process (.start builder)
        output (slurp (.getInputStream process))
        exit (.waitFor process)
        result {:exit exit :out output}]
    (when-not (zero? exit)
      (throw (ex-info "fixture command failed" (assoc result :args args))))
    result))

(defn- command-output [dir & args]
  (str/trim (:out (apply run-command! dir args))))

(defn- delete-tree! [path]
  (let [root (.toPath (io/file path))]
    (when (.exists (io/file path))
      (with-open [paths (java.nio.file.Files/walk root
                                                  (make-array
                                                    java.nio.file.FileVisitOption
                                                    0))]
        (doseq [entry (reverse (vec (iterator-seq (.iterator paths))))]
          (java.nio.file.Files/delete entry))))))

(defn- with-missing-worktree-fixture
  ([f] (with-missing-worktree-fixture :missing f))
  ([path-case f]
   (let [base (.toFile (java.nio.file.Files/createTempDirectory
                         (.toPath (io/file "/private/tmp"))
                         "worktree-prune-test"
                         (make-array java.nio.file.attribute.FileAttribute 0)))
         repo (io/file base "repo")
         target (io/file base "stale")
         peer (io/file base "peer")]
     (try
       (.mkdir repo)
       (run-command! repo "git" "init" "-q")
       (run-command! repo "git" "config" "user.name" "Fixture")
       (run-command! repo "git" "config" "user.email" "fixture@example.invalid")
       (spit (io/file repo "seed.txt") "seed\n")
       (run-command! repo "git" "add" "seed.txt")
       (run-command! repo "git" "commit" "-qm" "seed")
       (run-command! repo "git" "branch" "stale")
       (run-command! repo "git" "branch" "peer")
       (run-command! repo "git" "worktree" "add" "-q" (.getPath target) "stale")
       (run-command! repo "git" "worktree" "add" "-q" (.getPath peer) "peer")
       (when (= :locked path-case)
         (run-command! repo "git" "worktree" "lock" "--reason"
                       "compatibility witness" (.getPath target)))
       (delete-tree! target)
       (case path-case
         :missing nil
         :file (spit target "recreated file\n")
         :directory (.mkdir target)
         :dangling-symlink
         (java.nio.file.Files/createSymbolicLink
           (.toPath target) (.toPath (io/file base "absent-link-target"))
           (make-array java.nio.file.attribute.FileAttribute 0))
         :locked nil)
       (f {:repo (.getCanonicalPath repo)
           :target (.getCanonicalPath target)
           :peer (.getCanonicalPath peer)})
       (finally
         (delete-tree! base))))))

(deftest prune-request-is-a-separate-closed-schema
  ;; @spec WTL-PRUNE-001
  (is (= {:ok true :request prune-request}
         (invoke 'clj-surgeon.worktree-lifecycle/validate-prune-request
                 prune-request))))

(deftest absent-registration-row-is-closed-snapshot-data
  ;; @spec WTL-PRUNE-002
  (is (= true
         (:ok (invoke 'clj-surgeon.worktree-lifecycle/validate-snapshot
                      prune-snapshot)))))

(deftest absent-registration-row-requires-the-derived-tree
  ;; @spec WTL-PRUNE-002 WTL-PRUNE-005
  (doseq [bad-tree [nil "abc"]]
    (let [snapshot (assoc prune-snapshot :git-worktrees
                          [(assoc missing-row :tree bad-tree)])]
      (is (= :invalid-git-worktree
             (:error-type
               (invoke 'clj-surgeon.worktree-lifecycle/validate-snapshot
                       snapshot)))))))

(deftest missing-registration-has-narrow-prune-eligibility
  ;; @spec WTL-INV-008 WTL-PRUNE-001 WTL-PRUNE-004
  (is (= :missing-prunable
         (:classification
           (invoke 'clj-surgeon.worktree-lifecycle/classify-target
                   prune-snapshot "/repo-stale" nil)))))

(deftest duplicate-registration-identity-refuses-planning
  ;; @spec WTL-PRUNE-001 WTL-PRUNE-004
  (let [snapshot (assoc prune-snapshot :git-worktrees
                        [missing-row missing-row])]
    (is (= :ambiguous-target-registration
           (:error-type
             (invoke 'clj-surgeon.worktree-lifecycle/compile-prune-plan
                     snapshot prune-request controller-identity
                     "prune-duplicate"))))))

(deftest preservation-proof-is-exact-and-remote-bound
  ;; @spec WTL-PRUNE-003
  (is (= true
         (:ok (invoke 'clj-surgeon.worktree-lifecycle/validate-preservation-proof
                      prune-snapshot missing-row preservation)))))

(deftest prune-plan-is-operation-bound-and-outcome-free
  ;; @spec WTL-PRUNE-004 WTL-PRUNE-005
  (let [result (invoke 'clj-surgeon.worktree-lifecycle/compile-prune-plan
                       prune-snapshot prune-request controller-identity
                       "prune-1")]
    (is (= {:ok true
            :operation :prune-missing-registration
            :outcome-present false
            :handoff :not-applicable
            :lease-prestate :absent}
           {:ok (:ok result)
            :operation (get-in result [:plan :operation])
            :outcome-present (contains? (:plan result) :outcome)
            :handoff (get-in result [:plan :handoff])
            :lease-prestate (get-in result [:plan :lifecycle-lease-prestate])}))))

(deftest prune-plan-validation-is-distinct-from-close
  ;; @spec WTL-PRUNE-005 WTL-PRUNE-010
  (let [compiled (invoke 'clj-surgeon.worktree-lifecycle/compile-prune-plan
                         prune-snapshot prune-request controller-identity
                         "prune-2")]
    (is (= true
           (:ok (invoke 'clj-surgeon.worktree-lifecycle/validate-prune-plan
                        (:plan compiled)))))))

(deftest prune-plan-runtime-lease-binds-the-plan-hash-and-target
  ;; @spec WTL-PRUNE-005 WTL-PRUNE-006 WTL-PRUNE-010
  (let [compiled (invoke 'clj-surgeon.worktree-lifecycle/compile-prune-plan
                         prune-snapshot prune-request controller-identity
                         "prune-lease")
        plan (:plan compiled)
        lease (invoke 'clj-surgeon.worktree-lifecycle/expected-lifecycle-lease
                      plan)]
    (is (= {:plan-id (:plan-id plan)
            :plan-sha256 (:plan-sha256 plan)
            :target "/repo-stale"}
           (select-keys lease [:plan-id :plan-sha256 :target])))))

(deftest rehashed-prune-plan-drift-still-refuses
  ;; @spec WTL-PRUNE-005 WTL-PRUNE-010
  (let [compiled (invoke 'clj-surgeon.worktree-lifecycle/compile-prune-plan
                         prune-snapshot prune-request controller-identity
                         "prune-rehashed-drift")
        forged (-> (:plan compiled)
                   (assoc-in [:target :path-state] :unknown)
                   rehash-plan)]
    (is (= :invalid-prune-target
           (:error-type
             (invoke 'clj-surgeon.worktree-lifecycle/validate-prune-plan
                     forged))))))

(deftest rehashed-prune-plan-refuses-open-controller-data
  ;; @spec WTL-PRUNE-005 WTL-PRUNE-010
  (let [compiled (invoke 'clj-surgeon.worktree-lifecycle/compile-prune-plan
                         prune-snapshot prune-request controller-identity
                         "prune-open-controller")
        forged (-> (:plan compiled)
                   (assoc-in [:controller :unexpected] true)
                   rehash-plan)]
    (is (= :invalid-prune-controller
           (:error-type
             (invoke 'clj-surgeon.worktree-lifecycle/validate-prune-plan
                     forged))))))

(deftest prune-compiler-refuses-open-controller-data
  ;; @spec WTL-PRUNE-005
  (is (= :invalid-prune-controller
         (:error-type
           (invoke 'clj-surgeon.worktree-lifecycle/compile-prune-plan
                   prune-snapshot prune-request
                   (assoc controller-identity :unexpected true)
                   "prune-open-controller-at-compile")))))

(deftest rehashed-prune-plan-refuses-open-repository-and-parent-data
  ;; @spec WTL-PRUNE-002 WTL-PRUNE-005 WTL-PRUNE-010
  (let [compiled (invoke 'clj-surgeon.worktree-lifecycle/compile-prune-plan
                         prune-snapshot prune-request controller-identity
                         "prune-open-nested")
        repository-forged (-> (:plan compiled)
                              (assoc-in [:repository :unexpected] true)
                              rehash-plan)
        parent-forged (-> (:plan compiled)
                          (assoc-in [:target :nearest-existing-parent
                                     :unexpected] true)
                          rehash-plan)]
    (is (= :invalid-prune-repository
           (:error-type
             (invoke 'clj-surgeon.worktree-lifecycle/validate-prune-plan
                     repository-forged))))
    (is (= :invalid-prune-target
           (:error-type
             (invoke 'clj-surgeon.worktree-lifecycle/validate-prune-plan
                     parent-forged))))))

(deftest rehashed-prune-plan-refuses-untyped-capture-time
  ;; @spec WTL-PRUNE-005 WTL-PRUNE-010
  (let [compiled (invoke 'clj-surgeon.worktree-lifecycle/compile-prune-plan
                         prune-snapshot prune-request controller-identity
                         "prune-bad-capture-time")
        forged (-> (:plan compiled) (assoc :captured-at 42) rehash-plan)]
    (is (= :invalid-prune-captured-at
           (:error-type
             (invoke 'clj-surgeon.worktree-lifecycle/validate-prune-plan
                     forged))))))

(deftest prune-receipt-never-claims-an-experiment-outcome
  ;; @spec WTL-PRUNE-007 WTL-PRUNE-008
  (let [compiled (invoke 'clj-surgeon.worktree-lifecycle/compile-prune-plan
                         prune-snapshot prune-request controller-identity
                         "prune-3")
        receipt (invoke 'clj-surgeon.worktree-lifecycle/compile-prune-receipt
                        (:plan compiled) :controller
                        {:target-present false
                         :registration-present false
                         :branch-unchanged true
                         :preservation-unchanged true
                         :supacode-state :absent})]
    (is (= {:ok true :effect :registration-pruned :outcome-present false}
           {:ok (:ok receipt)
            :effect (get-in receipt [:receipt :effect])
            :outcome-present (contains? (:receipt receipt) :outcome)}))))

(deftest prune-receipt-refuses-open-private-postconditions
  ;; @spec WTL-PRUNE-007 WTL-PRUNE-008
  (let [compiled (invoke 'clj-surgeon.worktree-lifecycle/compile-prune-plan
                         prune-snapshot prune-request controller-identity
                         "prune-private-receipt")
        receipt (invoke 'clj-surgeon.worktree-lifecycle/compile-prune-receipt
                        (:plan compiled) :controller
                        {:target-present false
                         :registration-present false
                         :branch-unchanged true
                         :preservation-unchanged true
                         :supacode-state :absent
                         :source "(secret source)"
                         :outcome :landed})]
    (is (= :invalid-prune-postconditions (:error-type receipt)))
    (is (not (str/includes? (pr-str receipt) "secret source")))))

(deftest prune-receipt-requires-exact-boolean-terminal-evidence
  ;; @spec WTL-PRUNE-007 WTL-PRUNE-008
  (let [compiled (invoke 'clj-surgeon.worktree-lifecycle/compile-prune-plan
                         prune-snapshot prune-request controller-identity
                         "prune-untyped-receipt")
        receipt (invoke 'clj-surgeon.worktree-lifecycle/compile-prune-receipt
                        (:plan compiled) :controller
                        {:target-present nil
                         :registration-present nil
                         :branch-unchanged "yes"
                         :preservation-unchanged 1
                         :supacode-state :absent})]
    (is (= :invalid-prune-postconditions (:error-type receipt)))))

(deftest no-follow-path-authority-distinguishes-absence-and-links
  ;; @spec WTL-PRUNE-002 WTL-PRUNE-006
  (let [base (.toFile (java.nio.file.Files/createTempDirectory
                        (.toPath (io/file "/private/tmp"))
                        "worktree-path-proof"
                        (make-array java.nio.file.attribute.FileAttribute 0)))
        target (io/file base "missing")
        link (io/file base "dangling")]
    (try
      (java.nio.file.Files/createSymbolicLink
        (.toPath link) (.toPath (io/file base "nowhere"))
        (make-array java.nio.file.attribute.FileAttribute 0))
      (let [missing (invoke 'clj-surgeon.worktree-lifecycle-io/path-authority
                            (.getPath target))
            dangling (invoke 'clj-surgeon.worktree-lifecycle-io/path-authority
                             (.getPath link))]
        (is (= :absent (:path-state missing)))
        (is (= {:path-state :unknown :error-type :symlink-component}
               (select-keys dangling [:path-state :error-type]))))
      (finally
        (delete-tree! base)))))

(deftest prune-command-and-journal-are-closed
  ;; @spec WTL-PRUNE-006 WTL-PRUNE-010
  (is (= {:argv ["git" "worktree" "remove" "/repo-stale"]
          :parking-results [:not-applicable :not-applicable]}
         {:argv (invoke
                  'clj-surgeon.worktree-lifecycle-io/prune-removal-command
                  "/repo-stale")
          :parking-results
          (invoke 'clj-surgeon.worktree-lifecycle-io/prune-parking-results)})))

(deftest real-git-missing-registration-matrix-preserves-peer
  ;; @spec WTL-PRUNE-006 WTL-PRUNE-008
  (if-let [f (public-var
               'clj-surgeon.worktree-lifecycle-io/run-prune-compatibility-matrix)]
    (with-missing-worktree-fixture
      (fn [{:keys [repo target peer]}]
        (is (= {:ok true
                :expectation :success
                :exit 0
                :target-registration-present false
                :target-path-authority-unchanged true
                :peer-registration-present true}
               (f repo target peer)))))
    (is false "run-prune-compatibility-matrix is missing")))

(deftest real-git-prune-refuses-recreated-and-locked-targets
  ;; @spec WTL-PRUNE-006 WTL-PRUNE-008
  (if-let [f (public-var
               'clj-surgeon.worktree-lifecycle-io/run-prune-compatibility-matrix)]
    (doseq [path-case [:file :directory :dangling-symlink :locked]]
      (with-missing-worktree-fixture
        path-case
        (fn [{:keys [repo target peer]}]
          (let [result (f repo target peer :refusal)]
            (is (= true (:ok result)) (str path-case " must refuse"))
            (is (= :refusal (:expectation result)))
            (is (= true (:target-registration-present result)))
            (is (= true (:target-path-authority-unchanged result)))
            (is (= true (:peer-registration-present result)))))))
    (is false "run-prune-compatibility-matrix is missing")))

(defn- with-real-prune-apply-fixture [plan-id f]
  (with-missing-worktree-fixture
    (fn [{:keys [repo target peer]}]
      (let [head (command-output repo "git" "rev-parse" "refs/heads/stale")
            tree (command-output repo "git" "rev-parse" "refs/heads/stale^{tree}")
            peer-head (command-output repo "git" "rev-parse" "refs/heads/peer")
            peer-tree (command-output repo "git" "rev-parse" "refs/heads/peer^{tree}")
            common (.getCanonicalPath (io/file repo ".git"))
            target-row (assoc missing-row
                              :path-lexical target
                              :nearest-existing-parent
                              {:path (.getCanonicalPath (.getParentFile (io/file target)))
                               :device nil
                               :inode nil}
                              :head head
                              :tree tree)
            peer-row {:path peer
                      :head peer-head
                      :tree peer-tree
                      :branch "refs/heads/peer"
                      :detached false
                      :locked false
                      :lock-reason nil
                      :prunable nil
                      :status :clean
                      :removal-preflight
                      {:eligible true :submodules :none :reasons []}}
            proof (assoc preservation :object head)
            snapshot-base
            (assoc prune-snapshot
                   :repository {:root repo
                                :common-git-dir common
                                :primary-worktree repo
                                :object-format :sha1}
                   :controller-worktree repo
                   :git-worktrees [target-row peer-row]
                   :remotes {:available true :rows [proof]})
            request (assoc prune-request :target target :preservation proof)
            compiled (invoke 'clj-surgeon.worktree-lifecycle/compile-prune-plan
                             snapshot-base request controller-identity
                             plan-id)
            plan (:plan compiled)
            plan-write (invoke 'clj-surgeon.worktree-lifecycle-io/write-plan!
                               common plan)
            plan-file (:plan-file plan-write)
            actual-lease-file
            (io/file common "clj-surgeon/worktree-lifecycle/v1/leases"
                     (str (invoke 'clj-surgeon.worktree-lifecycle/sha256 target)
                          ".edn"))
            capture-var (ns-resolve 'clj-surgeon.worktree-lifecycle-io
                                    'capture-inventory)
            controller-var (ns-resolve 'clj-surgeon.worktree-lifecycle-io
                                       'controller-identity)
            run-var (ns-resolve 'clj-surgeon.worktree-lifecycle-io
                                'run-captured)
            advance-var (ns-resolve 'clj-surgeon.worktree-lifecycle-io
                                    'advance-journal!)
            original-run @run-var
            original-advance @advance-var
            registration-present?
            (fn []
              (str/includes?
                (:out (original-run repo
                                    ["git" "worktree" "list"
                                     "--porcelain" "-z"]))
                target))
            snapshot
            (fn []
              (assoc snapshot-base
                     :git-worktrees
                     (cond-> [peer-row] (registration-present?)
                             (conj target-row))
                     :lifecycle-leases
                     (if (.isFile actual-lease-file)
                       {target (invoke 'clj-surgeon.worktree-lifecycle-io/read-record
                                       actual-lease-file)}
                       {})))
            redirect-git (fn [_ argv] (original-run repo argv))
            apply!
            (fn [extra-redefs]
              (with-redefs-fn
                (merge {capture-var (fn [& _] (snapshot))
                        controller-var (constantly controller-identity)
                        run-var redirect-git}
                       extra-redefs)
                #(invoke 'clj-surgeon.worktree-lifecycle-io/apply-plan-file!
                         plan-file)))]
        (when-not (and (:ok compiled) (:ok plan-write))
          (throw (ex-info "real prune fixture did not compile"
                          {:compiled compiled :plan-write plan-write})))
        (f {:apply! apply!
            :advance-var advance-var
            :original-advance original-advance
            :registration-present? registration-present?
            :lease-file actual-lease-file})))))

(deftest temp-owned-apply-accepts-its-own-lease-and-prunes-once
  ;; @spec WTL-PRUNE-005 WTL-PRUNE-006 WTL-PRUNE-007 WTL-PRUNE-010
  (with-real-prune-apply-fixture
    "temp-owned-real-prune"
    (fn [{:keys [apply! registration-present? lease-file]}]
      (let [result (apply! {})]
        (is (= true (:ok result)) (pr-str result))
        (is (= :registration-pruned (get-in result [:receipt :effect])))
        (is (false? (registration-present?)))
        (is (false? (.exists lease-file)))))))

(deftest temp-owned-prune-apply-recovers-every-journal-window-once
  ;; @spec WTL-PRUNE-007 WTL-PRUNE-008 WTL-PRUNE-010
  (doseq [crash-state
          (invoke 'clj-surgeon.worktree-lifecycle-io/journal-states nil)]
    (with-real-prune-apply-fixture
      (str "temp-owned-prune-crash-" (name crash-state))
      (fn [{:keys [apply! advance-var original-advance
                   registration-present? lease-file]}]
        (let [crashed (atom false)
              crash-advance
              (fn [file plan state result]
                (let [journal (original-advance file plan state result)]
                  (if (and (= crash-state state)
                           (compare-and-set! crashed false true))
                    (throw (ex-info "fixture crash"
                                    {:state state :journal journal}))
                    journal)))
              first-result (apply! {advance-var crash-advance})
              replay-result (apply! {})]
          (is (= false (:ok first-result))
              (str crash-state " must interrupt the first pass"))
          (is (= true (:ok replay-result))
              (str crash-state " must recover: " (pr-str replay-result)))
          (is (false? (registration-present?)))
          (is (false? (.exists lease-file))))))))
