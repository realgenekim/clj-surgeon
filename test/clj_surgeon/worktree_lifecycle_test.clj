(ns clj-surgeon.worktree-lifecycle-test
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]))

(def head-a (apply str (repeat 40 "a")))
(def head-b (apply str (repeat 40 "b")))
(def tree-a (apply str (repeat 40 "c")))
(def tree-b (apply str (repeat 40 "d")))
(def sha-a (apply str (repeat 64 "a")))

(def target-row
  {:path "/repo-wt"
   :head head-a
   :tree tree-a
   :branch "refs/heads/experiment"
   :detached false
   :locked false
   :lock-reason nil
   :prunable nil
   :status :clean
   :removal-preflight {:eligible true :submodules :none :reasons []}})

(def supacode-row
  {:id "%2Frepo-wt"
   :path "/repo-wt"
   :status :unpinned
   :focused false})

(def base-snapshot
  {:schema :clj-surgeon.worktree-lifecycle-snapshot/v1
   :captured-at "2026-08-31T12:00:00Z"
   :repository {:root "/repo"
                :common-git-dir "/repo/.git"
                :primary-worktree "/repo"
                :object-format :sha1}
   :controller-worktree "/controller"
   :git-worktrees [target-row]
   :supacode {:available true :worktrees [supacode-row]}
   :remotes {:available true
             :rows [{:remote "origin"
                     :remote-url-sha256 sha-a
                     :ref "refs/heads/main"
                     :object head-b
                     :peeled-object nil}
                    {:remote "origin"
                     :remote-url-sha256 sha-a
                     :ref "refs/heads/experiment"
                     :object head-a
                     :peeled-object nil}]}
   :ancestry #{[head-a head-b]}
   :handoffs {"/repo-wt" {:schema :clj-surgeon.worktree-handoff/v1
                          :target "/repo-wt"
                          :head head-a
                          :tree tree-a
                          :owner "surgeon2"
                          :request-sha256 sha-a
                          :nonce "handoff-1"}}
   :lifecycle-leases {}})

(def landed-request
  {:schema :clj-surgeon.worktree-close-request/v1
   :target "/repo-wt"
   :outcome :landed
   :handoff :agent
   :evidence {:remote "origin"
              :remote-url-sha256 sha-a
              :ref "refs/heads/main"
              :object head-b
              :peeled-object nil}})

(def negative-request
  {:schema :clj-surgeon.worktree-close-request/v1
   :target "/repo-wt"
   :outcome :negative-experiment
   :handoff :agent
   :evidence
   {:breadcrumb {:remote "origin"
                 :remote-url-sha256 sha-a
                 :ref "refs/heads/experiment"
                 :object head-a
                 :path "docs/observations/negative.md"
                 :blob-sha256 sha-a}
    :seal {:schema :clj-surgeon.negative-experiment-seal/v1
           :experiment {:commit head-a :tree tree-a}
           :allowed-terminal-paths ["docs/observations/negative.md"]
           :raw-evidence {:kind :none}}
    :reachable true
    :terminal-paths ["docs/observations/negative.md"]}})

(def parked-request
  {:schema :clj-surgeon.worktree-close-request/v1
   :target "/repo-wt"
   :outcome :parked
   :handoff :agent
   :evidence
   {:upstream {:remote "origin"
               :remote-url-sha256 sha-a
               :ref "refs/heads/experiment"
               :object head-a}
    :issue {:store "/repo/.beads"
            :project "clj-surgeon"
            :id "clj-surgeon-wtl"
            :revision 7
            :status :open
            :owner "surgeon2"}
    :next-action "Resume the worktree lifecycle red matrix."
    :expiry "2026-09-30T00:00:00Z"
    :now "2026-08-31T12:00:00Z"}})

(def controller-identity
  {:commit head-b :tree tree-b :artifacts {:core sha-a :io sha-a}})

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

(deftest closed-snapshot-is-admitted
  ;; @spec WTL-INV-001
  (assert-context [(map? base-snapshot)
                   (= :clj-surgeon.worktree-lifecycle-snapshot/v1
                      (:schema base-snapshot))
                   (= :sha1 (get-in base-snapshot [:repository :object-format]))
                   (= 1 (count (:git-worktrees base-snapshot)))
                   (= 1 (count (get-in base-snapshot [:supacode :worktrees])))
                   (= 2 (count (get-in base-snapshot [:remotes :rows])))
                   (contains? base-snapshot :controller-worktree)])
  (is (= true
         (:ok (invoke 'clj-surgeon.worktree-lifecycle/validate-snapshot
                      base-snapshot)))))

(deftest supacode-identities-are-strictly-decoded
  ;; @spec WTL-INV-002
  (assert-context [(str/starts-with? (:id supacode-row) "%2F")
                   (= "/repo-wt" (:path supacode-row))
                   (str/starts-with? (:path supacode-row) "/")
                   (not (str/includes? (:path supacode-row) ".."))
                   (not (str/includes? (:id supacode-row) "%252F"))])
  (is (= {:ok true :path "/repo-wt" :existing false}
         (invoke 'clj-surgeon.worktree-lifecycle/decode-supacode-id
                 "%2Frepo-wt" false))))

(deftest git-worktree-facts-remain-closed
  ;; @spec WTL-INV-003
  (assert-context [(string? (:head target-row))
                   (= 40 (count (:head target-row)))
                   (string? (:tree target-row))
                   (= :clean (:status target-row))
                   (true? (get-in target-row [:removal-preflight :eligible]))])
  (is (= true
         (:ok (invoke 'clj-surgeon.worktree-lifecycle/validate-git-worktree
                      :sha1 target-row)))))

(deftest classification-precedence-is-closed
  ;; @spec WTL-INV-004
  (assert-context [(vector? (:git-worktrees base-snapshot))
                   (vector? (get-in base-snapshot [:supacode :worktrees]))
                   (nil? (:prunable target-row))
                   (false? (:locked target-row))
                   (= :unpinned (:status supacode-row))])
  (is (= :needs-seal
         (:classification
           (invoke 'clj-surgeon.worktree-lifecycle/classify-target
                   base-snapshot "/repo-wt" nil)))))

(deftest protected-worktrees-are-active
  ;; @spec WTL-INV-005
  (assert-context [(= "/repo" (get-in base-snapshot [:repository :primary-worktree]))
                   (= "/controller" (:controller-worktree base-snapshot))
                   (false? (:focused supacode-row))
                   (= :unpinned (:status supacode-row))
                   (empty? (:lifecycle-leases base-snapshot))])
  (is (= :active
         (:classification
           (invoke 'clj-surgeon.worktree-lifecycle/classify-target
                   (assoc-in base-snapshot [:repository :primary-worktree]
                             "/repo-wt")
                   "/repo-wt" nil)))))

(deftest supacode-only-identities-are-audit-only
  ;; @spec WTL-INV-006
  (let [snapshot (assoc base-snapshot :git-worktrees [])]
    (assert-context [(empty? (:git-worktrees snapshot))
                     (= 1 (count (get-in snapshot [:supacode :worktrees])))
                     (true? (get-in snapshot [:supacode :available]))
                     (= "/repo-wt" (:path supacode-row))
                     (nil? (some #(when (= "/repo-wt" (:path %)) %)
                                 (:git-worktrees snapshot)))])
    (is (= :missing-prunable
           (:classification
             (invoke 'clj-surgeon.worktree-lifecycle/classify-target
                     snapshot "/repo-wt" nil))))))

(deftest audit-compilation-preserves-every-union-row
  ;; @spec WTL-INV-007
  (assert-context [(map? base-snapshot)
                   (seq (:git-worktrees base-snapshot))
                   (seq (get-in base-snapshot [:supacode :worktrees]))
                   (true? (get-in base-snapshot [:remotes :available]))
                   (= "/repo-wt" (:path target-row))])
  (is (= ["/repo-wt"]
         (mapv :path
               (:worktrees
                 (invoke 'clj-surgeon.worktree-lifecycle/compile-audit
                         base-snapshot))))))

(deftest handoff-record-binds-the-active-owner
  ;; @spec WTL-HAND-001
  (let [locked (assoc target-row :locked true
                      :lock-reason "owner=surgeon2 purpose=lifecycle")]
    (assert-context [(true? (:locked locked))
                     (string? (:lock-reason locked))
                     (= head-a (:head locked))
                     (= tree-a (:tree locked))
                     (= "surgeon2" (get-in base-snapshot
                                           [:handoffs "/repo-wt" :owner]))])
    (is (= :clj-surgeon.worktree-handoff/v1
           (:schema
             (invoke 'clj-surgeon.worktree-lifecycle/compile-handoff
                     (assoc base-snapshot :git-worktrees [locked])
                     landed-request "surgeon2" "handoff-1"))))))

(deftest handoff-never-releases-the-owner-lock
  ;; @spec WTL-HAND-002
  (let [handoff (get-in base-snapshot [:handoffs "/repo-wt"])]
    (assert-context [(map? handoff)
                     (= head-a (:head handoff))
                     (= tree-a (:tree handoff))
                     (= "surgeon2" (:owner handoff))
                     (= "/repo-wt" (:target handoff))])
    (is (= ["git" "worktree" "unlock" "/repo-wt"]
           (invoke 'clj-surgeon.worktree-lifecycle/handoff-unlock-command
                   handoff)))))

(deftest lifecycle-lease-is-derived-and-plan-bound
  ;; @spec WTL-HAND-003 WTL-HAND-004
  (let [plan {:schema :clj-surgeon.worktree-close-plan/v1
              :plan-id "plan-1"
              :plan-sha256 sha-a
              :target {:path "/repo-wt" :head head-a :tree tree-a}
              :handoff (get-in base-snapshot [:handoffs "/repo-wt"])}]
    (assert-context [(map? plan)
                     (= "plan-1" (:plan-id plan))
                     (= sha-a (:plan-sha256 plan))
                     (= "/repo-wt" (get-in plan [:target :path]))])
    (is (= "plan-1"
           (:plan-id
             (invoke 'clj-surgeon.worktree-lifecycle/expected-lifecycle-lease
                     plan))))
    (is (= true
           (:ok
             (invoke 'clj-surgeon.worktree-lifecycle/validate-lease-prestate
                     plan nil))))))

(deftest legacy-handoff-is-explicit-and-single-target
  ;; @spec WTL-HAND-005
  (let [request (assoc landed-request :handoff :legacy)]
    (assert-context [(= :legacy (:handoff request))
                     (= "/repo-wt" (:target request))
                     (= :landed (:outcome request))
                     (map? (:evidence request))
                     (not (vector? (:target request)))])
    (is (= :legacy
           (:handoff-mode
             (invoke 'clj-surgeon.worktree-lifecycle/validate-close-request
                     request))))))

(deftest landed-seal-requires-advertised-ancestry
  ;; @spec WTL-SEAL-001
  (assert-context [(= head-a (:head target-row))
                   (contains? (:ancestry base-snapshot) [head-a head-b])
                   (= "origin" (get-in landed-request [:evidence :remote]))
                   (= "refs/heads/main" (get-in landed-request [:evidence :ref]))
                   (= head-b (get-in landed-request [:evidence :object]))])
  (is (= true
         (:ok (invoke 'clj-surgeon.worktree-lifecycle/validate-outcome
                      base-snapshot target-row landed-request)))))

(deftest negative-seal-grammar-is-closed
  ;; @spec WTL-SEAL-002
  (let [seal (get-in negative-request [:evidence :seal])]
    (assert-context [(map? seal)
                     (= :clj-surgeon.negative-experiment-seal/v1 (:schema seal))
                     (= 1 (count (:allowed-terminal-paths seal)))
                     (= :none (get-in seal [:raw-evidence :kind]))
                     (str/starts-with? (first (:allowed-terminal-paths seal))
                                       "docs/observations/")])
    (is (= true
           (:ok (invoke 'clj-surgeon.worktree-lifecycle/validate-negative-seal
                        seal))))))

(deftest negative-seal-binds-exact-history
  ;; @spec WTL-SEAL-003
  (let [evidence (:evidence negative-request)]
    (assert-context [(true? (:reachable evidence))
                     (= head-a (get-in evidence [:seal :experiment :commit]))
                     (= tree-a (get-in evidence [:seal :experiment :tree]))
                     (= (:terminal-paths evidence)
                        (get-in evidence [:seal :allowed-terminal-paths]))
                     (= head-a (get-in evidence [:breadcrumb :object]))])
    (is (= true
           (:ok (invoke 'clj-surgeon.worktree-lifecycle/validate-outcome
                        base-snapshot target-row negative-request))))))

(deftest negative-seal-retains-raw-evidence-disposition
  ;; @spec WTL-SEAL-004
  (let [archive {:kind :archive
                 :receipt-ref "refs/heads/receipts"
                 :receipt-path "docs/observations/archive.edn"
                 :archive-locator "sha256/fixture.tar.gz"
                 :archive-sha256 sha-a}]
    (assert-context [(= :archive (:kind archive))
                     (str/starts-with? (:receipt-ref archive) "refs/")
                     (not (str/starts-with? (:receipt-path archive) "/"))
                     (= 64 (count (:archive-sha256 archive)))
                     (not (str/includes? (:archive-locator archive) ".."))])
    (is (= true
           (:ok (invoke 'clj-surgeon.worktree-lifecycle/validate-raw-evidence
                        archive))))))

(deftest parked-seal-binds-upstream-and-owner
  ;; @spec WTL-SEAL-005
  (let [evidence (:evidence parked-request)]
    (assert-context [(= :open (get-in evidence [:issue :status]))
                     (= "surgeon2" (get-in evidence [:issue :owner]))
                     (= head-a (get-in evidence [:upstream :object]))
                     (string? (:next-action evidence))
                     (< (count (.getBytes ^String (:next-action evidence) "UTF-8"))
                        513)])
    (is (= true
           (:ok (invoke 'clj-surgeon.worktree-lifecycle/validate-outcome
                        base-snapshot target-row parked-request))))))

(deftest parked-revision-accepts-only-identical-controller-append
  ;; @spec WTL-SEAL-006
  (let [issue (get-in parked-request [:evidence :issue])
        append {:plan-sha256 sha-a :revision-before 7 :revision-after 8}]
    (assert-context [(= 7 (:revision issue))
                     (= 7 (:revision-before append))
                     (= 8 (:revision-after append))
                     (= sha-a (:plan-sha256 append))
                     (= 1 (- (:revision-after append) (:revision-before append)))])
    (is (= true
           (:ok (invoke 'clj-surgeon.worktree-lifecycle/validate-parking-revision
                        issue append append))))))

(deftest missing-outcome-remains-needs-seal
  ;; @spec WTL-SEAL-007
  (assert-context [(= :clean (:status target-row))
                   (false? (:locked target-row))
                   (nil? (:prunable target-row))
                   (true? (get-in target-row [:removal-preflight :eligible]))
                   (empty? (:lifecycle-leases base-snapshot))])
  (is (= :needs-seal
         (:classification
           (invoke 'clj-surgeon.worktree-lifecycle/classify-target
                   base-snapshot "/repo-wt" nil)))))

(deftest dry-run-request-and-fingerprint-are-closed
  ;; @spec WTL-PLAN-001 WTL-PLAN-002
  (assert-context [(= :clj-surgeon.worktree-close-request/v1
                      (:schema landed-request))
                   (= "/repo-wt" (:target landed-request))
                   (= :landed (:outcome landed-request))
                   (= :agent (:handoff landed-request))])
  (let [plan (invoke 'clj-surgeon.worktree-lifecycle/compile-plan
                     base-snapshot landed-request controller-identity "plan-1")]
    (is (= true (:ok plan)))
    (is (= :absent (get-in plan [:plan :lifecycle-lease-prestate])))))

(deftest canonical-plan-bytes-are-private-and-self-hash-free
  ;; @spec WTL-PLAN-003 WTL-PLAN-004
  (let [value {:z 1 :a {:b 2} :ordered [3 2 1]}]
    (assert-context [(map? value)
                     (vector? (:ordered value))
                     (= [3 2 1] (:ordered value))
                     (not (contains? value :source))])
    (is (= "{:a {:b 2}, :ordered [3 2 1], :z 1}\n"
           (invoke 'clj-surgeon.worktree-lifecycle/canonical-edn value)))
    (is (= "fef86d69f410bf8c3331479b1c5b73a1d169be8e21c82ebf6b2d2f5960be1710"
           (invoke 'clj-surgeon.worktree-lifecycle/sha256
                   (apply str (repeat 64 "placeholder")))))))

(deftest apply-input-and-replay-authority-are-plan-bound
  ;; @spec WTL-PLAN-005 WTL-PLAN-006
  (let [request {:plan "/repo/.git/clj-surgeon/worktree-lifecycle/v1/plans/p.edn"
                 :apply "1"}]
    (assert-context [(string? (:plan request))
                     (= "1" (:apply request))
                     (str/starts-with? (:plan request) "/")
                     (not (contains? request :outcome))])
    (is (= true
           (:ok (invoke 'clj-surgeon.worktree-lifecycle/validate-apply-request
                        request))))
    (is (= :path-reused
           (:error-type
             (invoke 'clj-surgeon.worktree-lifecycle/validate-terminal-replay
                     {:target-present true :registration-present false}))))))
