(ns clj-surgeon.worktree-lifecycle-prune-test
  (:require
   [clojure.java.io :as io]
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
  {:commit oid-b :tree oid-b :artifacts {:core sha-a :io sha-a} :clean true})

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

(defn- delete-tree! [path]
  (let [root (.toPath (io/file path))]
    (when (.exists (io/file path))
      (with-open [paths (java.nio.file.Files/walk root
                                                  (make-array
                                                    java.nio.file.FileVisitOption
                                                    0))]
        (doseq [entry (reverse (vec (iterator-seq (.iterator paths))))]
          (java.nio.file.Files/delete entry))))))

(defn- with-missing-worktree-fixture [f]
  (let [base (.toFile (java.nio.file.Files/createTempDirectory
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
      (delete-tree! target)
      (f {:repo (.getCanonicalPath repo)
          :target (.getCanonicalPath target)
          :peer (.getCanonicalPath peer)})
      (finally
        (delete-tree! base)))))

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

(deftest no-follow-path-authority-distinguishes-absence-and-links
  ;; @spec WTL-PRUNE-002 WTL-PRUNE-006
  (let [base (.toFile (java.nio.file.Files/createTempDirectory
                        "worktree-path-proof"
                        (make-array java.nio.file.attribute.FileAttribute 0)))
        target (io/file base "missing")
        link (io/file base "dangling")]
    (try
      (java.nio.file.Files/createSymbolicLink
        (.toPath link) (.toPath (io/file base "nowhere"))
        (make-array java.nio.file.attribute.FileAttribute 0))
      (is (= [:absent :present]
             (mapv :path-state
                   [(invoke 'clj-surgeon.worktree-lifecycle-io/path-authority
                            (.getPath target))
                    (invoke 'clj-surgeon.worktree-lifecycle-io/path-authority
                            (.getPath link))])))
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
                :target-registration-present false
                :peer-registration-present true}
               (f repo target peer)))))
    (is false "run-prune-compatibility-matrix is missing")))
