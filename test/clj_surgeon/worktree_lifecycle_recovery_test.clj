(ns clj-surgeon.worktree-lifecycle-recovery-test
  (:require
   [cheshire.core :as json]
   [clj-surgeon.worktree-lifecycle :as lifecycle]
   [clj-surgeon.worktree-lifecycle-io :as lifecycle-io]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is]]))

(def ^:private plan
  {:schema :clj-surgeon.worktree-close-plan/v1
   :plan-id "recovery-plan"
   :plan-sha256 (apply str (repeat 64 "a"))
   :target {:path "/fixture/target"}
   :handoff {:nonce "handoff"}})

(defn- journal-through [state]
  (let [states (lifecycle-io/journal-states :landed)
        end (inc (.indexOf states state))]
    {:schema :clj-surgeon.worktree-lifecycle-journal/v1
     :plan-id (:plan-id plan)
     :plan-sha256 (:plan-sha256 plan)
     :transitions (mapv #(hash-map :state % :result :ok)
                        (subvec states 0 end))}))

(defn- private-call [name & arguments]
  (apply (ns-resolve 'clj-surgeon.worktree-lifecycle-io name) arguments))

(deftest matching-lease-before-prepared-journal-is-recoverable
  (let [lease (lifecycle/expected-lifecycle-lease plan)]
    (is (= (:plan-id plan) (:plan-id lease)))
    (is (true? (private-call 'matching-lease? plan lease)))
    (is (false? (private-call 'matching-lease?
                              plan (assoc lease :plan-id "foreign"))))))

(deftest recovery-refuses-a-journal-owned-by-another-plan
  (let [journal (assoc (journal-through :prepared) :plan-id "foreign")]
    (is (= :prepared (get-in journal [:transitions 0 :state])))
    (is (= :invalid-lifecycle-journal
           (try
             (private-call 'validate-journal! journal plan)
             nil
             (catch clojure.lang.ExceptionInfo error
               (:error-type (ex-data error))))))))

(deftest recovery-after-parking-append-advances-to-archive-command
  (let [journal (journal-through :parking-intent-recorded)]
    (is (= :archive-commanded
           (lifecycle-io/next-journal-state :parked journal)))))

(deftest recovery-after-archive-command-advances-to-verification
  (let [journal (journal-through :archive-commanded)]
    (is (= :archive-verified
           (lifecycle-io/next-journal-state :landed journal)))))

(deftest recovery-after-lease-release-observes-removal
  (let [journal (journal-through :remove-commanded)]
    (is (= :remove-verified
           (lifecycle-io/next-journal-state :landed journal)))))

(deftest recovery-after-removal-finalizes-the-receipt
  (let [journal (journal-through :remove-verified)]
    (is (= :final-receipt-written
           (lifecycle-io/next-journal-state :negative-experiment journal)))))

(deftest recovery-after-receipt-finalizes-parking-once
  (is (= :parking-completion-verified
         (lifecycle-io/next-journal-state
           :parked (journal-through :final-receipt-written))))
  (is (nil? (lifecycle-io/next-journal-state
              :parked (journal-through :parking-completion-verified)))))

(defn- make-apply-fixture
  ([] (make-apply-fixture :landed))
  ([outcome]
   (let [root (.toFile
                (java.nio.file.Files/createTempDirectory
                  "worktree-lifecycle-apply"
                  (make-array java.nio.file.attribute.FileAttribute 0)))
         root-path (.getCanonicalPath root)
         target (doto (io/file root "target") .mkdirs)
         target-path (.getCanonicalPath target)
         common (doto (io/file root "common.git") .mkdirs)
         issue-store (doto (io/file root ".beads") .mkdirs)
         controller {:commit (apply str (repeat 40 "c"))
                     :tree (apply str (repeat 40 "d"))
                     :clean true
                     :artifacts {}}
         remote {:remote "origin"
                 :remote-url-sha256 (apply str (repeat 64 "e"))
                 :ref "refs/heads/main"
                 :object (apply str (repeat 40 "b"))
                 :peeled-object nil}
         row {:path target-path
              :head (apply str (repeat 40 "a"))
              :tree (apply str (repeat 40 "f"))
              :branch "refs/heads/experiment"
              :detached false
              :locked false
              :lock-reason nil
              :prunable nil
              :status :clean
              :removal-preflight {:eligible true :submodules :none :reasons []}}
         repository {:root root-path
                     :common-git-dir (.getCanonicalPath common)
                     :primary-worktree (str root-path "/primary")
                     :object-format :sha1}
         base {:schema lifecycle/snapshot-schema
               :captured-at "2026-08-31T00:00:00Z"
               :repository repository
               :controller-worktree (str root-path "/controller")
               :git-worktrees [row]
               :supacode {:available true :worktrees []}
               :remotes {:available true :rows [remote]}
               :ancestry #{[(:head row) (:object remote)]}
               :handoffs {}
               :lifecycle-leases {}}
         parked-evidence
         {:upstream (assoc remote :object (:head row))
          :issue {:store (.getCanonicalPath issue-store)
                  :project "fixture"
                  :id "fixture-parked"
                  :revision 7
                  :status :open
                  :owner "surgeon2"}
          :next-action "Resume the fixture."
          :expiry "2099-09-30T00:00:00Z"
          :now "2026-08-31T00:00:00Z"}
         request {:schema lifecycle/close-request-schema
                  :target target-path
                  :outcome outcome
                  :handoff :legacy
                  :evidence (if (= :parked outcome) parked-evidence remote)}
         base (if (= :parked outcome)
                (assoc-in base [:remotes :rows] [(:upstream parked-evidence)])
                base)
         compiled (lifecycle/compile-plan base request controller "crash-plan")
         plan (:plan compiled)
         plan-write (lifecycle-io/write-plan! (.getCanonicalPath common) plan)
         lease-file (io/file common "clj-surgeon/worktree-lifecycle/v1/leases"
                             (str (lifecycle/sha256 target-path) ".edn"))
         remove-count (atom 0)
         snapshot
         (fn []
           (assoc base
                  :git-worktrees (if (.exists target) [row] [])
                  :lifecycle-leases
                  (if (.isFile lease-file)
                    {target-path (lifecycle-io/read-record lease-file)} {})))]
     {:root root
      :target target
      :controller controller
      :plan plan
      :plan-file (:plan-file plan-write)
      :snapshot snapshot
      :remove-count remove-count})))

(deftest apply-entry-recovers-every-journal-crash-window-exactly-once
  (doseq [crash-state (lifecycle-io/journal-states :landed)]
    (let [{:keys [target controller plan-file snapshot remove-count]}
          (make-apply-fixture)
          advance-var (ns-resolve 'clj-surgeon.worktree-lifecycle-io
                                  'advance-journal!)
          capture-var (ns-resolve 'clj-surgeon.worktree-lifecycle-io
                                  'capture-inventory)
          controller-var (ns-resolve 'clj-surgeon.worktree-lifecycle-io
                                     'controller-identity)
          run-var (ns-resolve 'clj-surgeon.worktree-lifecycle-io 'run-captured)
          original-advance @advance-var
          crashed (atom false)
          fake-run
          (fn [_ argv]
            (if (and (= "git" (first argv))
                     (= "worktree" (nth argv 3 nil))
                     (= "remove" (nth argv 4 nil)))
              (do
                (swap! remove-count inc)
                (java.nio.file.Files/delete (.toPath target))
                {:exit 0 :out "" :err "" :argv argv})
              {:exit 0 :out "" :err "" :argv argv}))
          crash-advance
          (fn [file plan state result]
            (if (and (= crash-state state) (compare-and-set! crashed false true))
              (if (= :prepared state)
                (throw (ex-info "fixture crash" {:state state}))
                (let [journal (original-advance file plan state result)]
                  (throw (ex-info "fixture crash" {:state state
                                                   :journal journal}))))
              (original-advance file plan state result)))]
      (with-redefs-fn
        {capture-var (fn [& _] (snapshot))
         controller-var (constantly controller)
         run-var fake-run
         advance-var crash-advance}
        #(is (thrown? clojure.lang.ExceptionInfo
                      (lifecycle-io/apply-plan-file! plan-file))))
      (with-redefs-fn
        {capture-var (fn [& _] (snapshot))
         controller-var (constantly controller)
         run-var fake-run}
        #(is (true? (:ok (lifecycle-io/apply-plan-file! plan-file)))))
      (is (= 1 @remove-count)))))

(deftest parked-apply-recovers-after-completion-effect-before-journal
  (let [{:keys [target controller plan-file snapshot remove-count]}
        (make-apply-fixture :parked)
        capture-var (ns-resolve 'clj-surgeon.worktree-lifecycle-io
                                'capture-inventory)
        controller-var (ns-resolve 'clj-surgeon.worktree-lifecycle-io
                                   'controller-identity)
        run-var (ns-resolve 'clj-surgeon.worktree-lifecycle-io 'run-captured)
        parking-intent
        (lifecycle/canonical-edn
          (private-call 'parking-record
                        (lifecycle-io/read-record plan-file)))
        issue-row (atom {:id "fixture-parked"
                         :status "open"
                         :assignee "surgeon2"
                         :revision 7
                         :notes parking-intent})
        append-count (atom 0)
        crash-after-completion (atom true)
        fake-run
        (fn [_ argv]
          (cond
            (and (= "git" (first argv))
                 (= "worktree" (nth argv 3 nil))
                 (= "remove" (nth argv 4 nil)))
            (do
              (swap! remove-count inc)
              (java.nio.file.Files/delete (.toPath target))
              {:exit 0 :out "" :err "" :argv argv})

            (and (= "bd" (first argv)) (= "show" (nth argv 3 nil)))
            {:exit 0 :out (json/generate-string [@issue-row]) :err "" :argv argv}

            (and (= "bd" (first argv)) (= "update" (nth argv 3 nil)))
            (let [record (nth argv 6)]
              (swap! append-count inc)
              (swap! issue-row
                     (fn [row]
                       (-> row
                           (update :revision inc)
                           (update :notes str (when (seq (:notes row)) "\n")
                                   record))))
              (when (and (= 1 @append-count)
                         (compare-and-set! crash-after-completion true false))
                (throw (ex-info "fixture crash after append"
                                {:append-count @append-count})))
              {:exit 0 :out "{}" :err "" :argv argv})

            :else
            {:exit 0 :out "" :err "" :argv argv}))]
    (with-redefs-fn
      {capture-var (fn [& _] (snapshot))
       controller-var (constantly controller)
       run-var fake-run}
      #(is (thrown? clojure.lang.ExceptionInfo
                    (lifecycle-io/apply-plan-file! plan-file))))
    (with-redefs-fn
      {capture-var (fn [& _] (snapshot))
       controller-var (constantly controller)
       run-var fake-run}
      #(is (true? (:ok (lifecycle-io/apply-plan-file! plan-file)))))
    (is (= 1 @append-count))
    (is (= 1 @remove-count))))
