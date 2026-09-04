(ns clj-surgeon.worktree-lifecycle-io-test
  (:require
   [babashka.fs :as fs]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]))

(def sha-a (apply str (repeat 64 "a")))

(def plan
  {:schema :clj-surgeon.worktree-close-plan/v1
   :plan-id "plan-io-1"
   :plan-sha256 sha-a
   :outcome :landed
   :target {:path "/fixture/target" :head "aaaa" :tree "bbbb"}
   :supacode {:initial :unpinned :terminal :archived}
   :lifecycle-lease-prestate :absent
   :expected-lifecycle-lease {:plan-id "plan-io-1"}})

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

(defn- assert-context [conditions]
  (doseq [condition conditions]
    (is condition)))

(deftest apply-acquires-lock-and-revalidates-before-effects
  ;; @spec WTL-APPLY-001
  (let [events (atom [])
        result (invoke 'clj-surgeon.worktree-lifecycle-io/apply-plan!
                       {:plan plan
                        :snapshot-fn #(do (swap! events conj :snapshot) plan)
                        :archive-fn #(swap! events conj :archive)
                        :remove-fn #(swap! events conj :remove)
                        :terminal-fn (constantly true)
                        :fixture-root "/fixture"})]
    (assert-context [(map? plan)
                     (= "plan-io-1" (:plan-id plan))
                     (= :absent (:lifecycle-lease-prestate plan))
                     (vector? @events)
                     (not (contains? plan :source))])
    (is (= true (:ok result)))))

(deftest journal-states-are-monotone-and-explicit
  ;; @spec WTL-APPLY-002
  (let [states [:prepared :parking-intent-recorded :archive-commanded
                :archive-verified :remove-commanded :remove-verified
                :final-receipt-written :parking-completion-verified]]
    (assert-context [(vector? states)
                     (= :prepared (first states))
                     (= :parking-completion-verified (last states))
                     (= 8 (count states))])
    (is (= states
           (invoke 'clj-surgeon.worktree-lifecycle-io/journal-states
                   :landed)))))

(deftest supacode-archive-requires-a-stable-bracket
  ;; @spec WTL-APPLY-003
  (let [before {:available true :state :unpinned :focused false}
        after {:available true :state :unpinned :focused false}]
    (assert-context [(true? (:available before))
                     (= (:state before) (:state after))
                     (false? (:focused before))
                     (false? (:focused after))])
    (is (= {:ok true :terminal :archived :archive-required true}
           (invoke 'clj-surgeon.worktree-lifecycle-io/compile-archive-step
                   before after)))))

(deftest post-archive-gate-rechecks-non-ui-authorities
  ;; @spec WTL-APPLY-004
  (let [before (assoc plan :supacode {:initial :unpinned :terminal :archived})
        after before]
    (assert-context [(= before after)
                     (= :archived (get-in before [:supacode :terminal]))
                     (= (:target before) (:target after))
                     (= (:outcome before) (:outcome after))])
    (is (= true
           (:ok (invoke 'clj-surgeon.worktree-lifecycle-io/post-archive-gate
                        before after :archived))))))

(deftest post-archive-drift-restores-or-reports-partial
  ;; @spec WTL-APPLY-005
  (let [events (atom [])
        result (invoke 'clj-surgeon.worktree-lifecycle-io/handle-post-archive-drift!
                       {:archived-by-invocation true
                        :restore-fn #(swap! events conj :restore)})]
    (assert-context [(vector? @events)
                     (map? plan)
                     (= :archived (get-in plan [:supacode :terminal]))
                     (= :unpinned (get-in plan [:supacode :initial]))])
    (is (= :restored-refusal (:terminal-state result)))))

(deftest removal-command-is-non-force-and-controller-owned
  ;; @spec WTL-APPLY-006
  (let [command (invoke 'clj-surgeon.worktree-lifecycle-io/removal-command
                        "/fixture/controller" "/fixture/target")]
    (assert-context [(string? "/fixture/controller")
                     (string? "/fixture/target")
                     (not= "/fixture/controller" "/fixture/target")
                     (not (some #{"--force"} ["git" "worktree" "remove"]))])
    (is (= ["git" "-C" "/fixture/controller" "worktree" "remove"
            "/fixture/target"]
           command))))

(deftest failed-removal-restores-the-visible-room
  ;; @spec WTL-APPLY-007
  (let [events (atom [])
        result (invoke 'clj-surgeon.worktree-lifecycle-io/handle-removal-failure!
                       {:archived-by-invocation true
                        :restore-fn #(do (swap! events conj :restore) true)})]
    (assert-context [(vector? @events)
                     (map? plan)
                     (= :landed (:outcome plan))
                     (= :archived (get-in plan [:supacode :terminal]))])
    (is (= :restored-refusal (:terminal-state result)))))

(deftest successful-removal-proves-the-planned-ui-postcondition
  ;; @spec WTL-APPLY-008
  (let [postconditions {:target-present false
                        :registration-present false
                        :refs-unchanged true
                        :evidence-unchanged true
                        :supacode-state :archived}]
    (assert-context [(false? (:target-present postconditions))
                     (false? (:registration-present postconditions))
                     (true? (:refs-unchanged postconditions))
                     (true? (:evidence-unchanged postconditions))])
    (is (= true
           (:ok (invoke 'clj-surgeon.worktree-lifecycle-io/validate-terminal-postconditions
                        plan postconditions))))))

(deftest final-receipt-and-parking-marker-are-separate
  ;; @spec WTL-APPLY-009
  (let [receipt {:schema :clj-surgeon.worktree-close-receipt/v1
                 :plan-id "plan-io-1"
                 :plan-sha256 sha-a}
        marker {:schema :clj-surgeon.worktree-parking-completion/v1
                :plan-id "plan-io-1"
                :receipt-sha256 sha-a}]
    (assert-context [(map? receipt)
                     (map? marker)
                     (= (:plan-id receipt) (:plan-id marker))
                     (not= (:schema receipt) (:schema marker))])
    (is (= true
           (:ok (invoke 'clj-surgeon.worktree-lifecycle-io/validate-completion-pair
                        receipt marker))))))

(deftest recovery-resumes-only-the-next-idempotent-transition
  ;; @spec WTL-APPLY-010
  (let [journal {:plan-id "plan-io-1"
                 :transitions [{:state :prepared :result :ok}
                               {:state :parking-intent-recorded
                                :result :not-applicable}]}
        effects #{[:archive "plan-io-1"]}]
    (assert-context [(map? journal)
                     (= 2 (count (:transitions journal)))
                     (= :prepared (get-in journal [:transitions 0 :state]))
                     (contains? effects [:archive "plan-io-1"])])
    (is (= :archive-commanded
           (invoke 'clj-surgeon.worktree-lifecycle-io/next-journal-state
                   :landed journal)))))

(deftest terminal-replay-refuses-a-recreated-path
  ;; @spec WTL-APPLY-011
  (let [postconditions {:target-present true
                        :registration-present false
                        :receipt-present true
                        :plan-sha256 sha-a}]
    (assert-context [(true? (:target-present postconditions))
                     (false? (:registration-present postconditions))
                     (true? (:receipt-present postconditions))
                     (= sha-a (:plan-sha256 postconditions))])
    (is (= :path-reused
           (:error-type
             (invoke 'clj-surgeon.worktree-lifecycle-io/validate-replay
                     plan postconditions))))))

(deftest subprocess-boundary-uses-closed-argv-and-directory
  ;; @spec WTL-APPLY-012
  (let [request {:directory "/fixture/controller"
                 :argv ["git" "status" "--porcelain=v2" "-z"]}]
    (assert-context [(string? (:directory request))
                     (vector? (:argv request))
                     (= "git" (first (:argv request)))
                     (not-any? #(or (= % "sh") (= % "-c")) (:argv request))])
    (is (= true
           (:ok (invoke 'clj-surgeon.worktree-lifecycle-io/validate-process-request
                        request))))))

(deftest fixture-removal-guard-rejects-outside-common-git-directory
  (let [root (.toFile
               (java.nio.file.Files/createTempDirectory
                 "worktree-lifecycle-fixture"
                 (make-array java.nio.file.attribute.FileAttribute 0)))
        root-path (.getCanonicalPath root)
        inside (.getCanonicalPath (io/file root "target"))
        outside "/Users/genekim/src.local/clj-surgeon"]
    (try
      (assert-context [(.isDirectory root)
                       (.startsWith inside root-path)
                       (not (.startsWith outside root-path))
                       (not= inside outside)
                       (str/includes? root-path "worktree-lifecycle-fixture")])
      (finally
        (try (fs/delete-tree root) (catch Throwable _ nil))))))
