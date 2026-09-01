(ns clj-surgeon.worktree-lifecycle-io
  "Bounded Git, Supacode, plan, journal, and single-target apply adapter."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.worktree-lifecycle :as lifecycle]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str])
  (:import
   (java.io File)
   (java.nio ByteBuffer)
   (java.nio.channels FileChannel)
   (java.nio.charset StandardCharsets)
   (java.nio.file CopyOption Files OpenOption StandardCopyOption StandardOpenOption)
   (java.nio.file.attribute FileAttribute PosixFilePermissions)
   (java.security MessageDigest)
   (java.time Instant)
   (java.util UUID)))

(def ^:private lifecycle-directory "clj-surgeon/worktree-lifecycle/v1")
(def ^:private negative-seal-begin
  "<!-- BEGIN CLJ-SURGEON NEGATIVE-EXPERIMENT SEAL -->")
(def ^:private negative-seal-end
  "<!-- END CLJ-SURGEON NEGATIVE-EXPERIMENT SEAL -->")
(def ^:private transition-order
  [:prepared :parking-intent-recorded :archive-commanded :archive-verified
   :remove-commanded :remove-verified :final-receipt-written
   :parking-completion-verified])

(defn validate-process-request [{:keys [directory argv]}]
  (if (and (string? directory)
           (.isAbsolute (io/file directory))
           (vector? argv)
           (seq argv)
           (every? string? argv)
           (not-any? #(or (= % "sh") (= % "bash") (= % "-c")) argv))
    {:ok true}
    {:ok false :error-type :invalid-process-request}))

(defn run-captured
  "Run closed argv in an explicit directory and retain byte-exact stdout text."
  [directory argv]
  (let [validation (validate-process-request {:directory directory :argv argv})]
    (when-not (:ok validation)
      (throw (ex-info "Invalid subprocess request" validation)))
    (let [process (-> (ProcessBuilder. (into-array String argv))
                      (.directory (io/file directory))
                      (.redirectErrorStream false)
                      .start)
          out (slurp (.getInputStream process))
          err (slurp (.getErrorStream process))
          exit (.waitFor process)]
      {:argv argv :directory directory :exit exit :out out :err err})))

(defn- require-zero [{:keys [exit] :as result} error-type]
  (if (zero? exit)
    result
    (throw (ex-info "Lifecycle subprocess failed"
                    {:error-type error-type
                     :exit exit
                     :argv (:argv result)
                     :stderr (:err result)}))))

(defn- canonical-file [path]
  (.getCanonicalFile (io/file path)))

(defn- canonical-path [path]
  (.getPath (canonical-file path)))

(defn- lines [text]
  (remove str/blank? (str/split-lines (or text ""))))

(defn- split-nul [text]
  (str/split (or text "") #"\u0000" -1))

(defn parse-git-worktrees
  "Parse NUL-delimited `git worktree list --porcelain -z` output."
  [output]
  (->> (split-nul output)
       (partition-by str/blank?)
       (remove #(str/blank? (first %)))
       (mapv
         (fn [fields]
           (reduce
             (fn [row field]
               (let [[name value] (str/split field #" " 2)]
                 (case name
                   "worktree" (assoc row :path value)
                   "HEAD" (assoc row :head value)
                   "branch" (assoc row :branch value :detached false)
                   "detached" (assoc row :branch nil :detached true)
                   "locked" (assoc row :locked true :lock-reason value)
                   "prunable" (assoc row :prunable value)
                   row)))
             {:branch nil :detached false :locked false :lock-reason nil
              :prunable nil}
             fields)))))

(defn parse-supacode-list
  "Parse tab-separated encoded-ID/status rows from Supacode."
  [output]
  (mapv
    (fn [line]
      (let [[encoded status] (str/split line #"\t" 2)
            decoded (lifecycle/decode-supacode-id encoded
                                                  (.exists (io/file
                                                             (or (some-> encoded
                                                                         (lifecycle/decode-supacode-id
                                                                           false)
                                                                         :path)
                                                                 "/__invalid__"))))]
        (when-not (:ok decoded)
          (throw (ex-info "Invalid Supacode worktree identity" decoded)))
        {:id encoded
         :path (:path decoded)
         :status (keyword status)
         :focused false}))
    (lines output)))

(defn- parse-focused-id [output]
  (some-> (first (lines output)) (str/split #"\t" 2) first))

(defn- sha256-file [file]
  (lifecycle/sha256 (slurp file)))

(defn- remote-url-digest [root remote]
  (let [result (require-zero (run-captured root ["git" "remote" "get-url" remote])
                             :remote-url-unavailable)]
    (lifecycle/sha256 (str/trim (:out result)))))

(defn- parse-ls-remote [remote url-sha output]
  (let [pairs (map #(str/split % #"\s+" 2) (lines output))
        peeled (into {} (for [[object ref] pairs
                              :when (str/ends-with? ref "^{}")]
                          [(subs ref 0 (- (count ref) 3)) object]))]
    (->> pairs
         (remove #(str/ends-with? (second %) "^{}"))
         (mapv (fn [[object ref]]
                 {:remote remote
                  :remote-url-sha256 url-sha
                  :ref ref
                  :object object
                  :peeled-object (get peeled ref)})))))

(defn- status-for [path]
  (let [result (run-captured path
                             ["git" "status" "--porcelain=v2" "-z"
                              "--untracked-files=all"])]
    (if (zero? (:exit result))
      (if (str/blank? (:out result)) :clean :dirty)
      :unknown)))

(defn- tree-for [path]
  (let [result (run-captured path ["git" "rev-parse" "HEAD^{tree}"])]
    (when (zero? (:exit result)) (str/trim (:out result)))))

(defn- submodule-state [path]
  (cond
    (.isFile (io/file path ".gitmodules")) :present
    (.exists (io/file path)) :none
    :else :unknown))

(defn- enrich-git-row [row]
  (let [path (:path row)
        exists (.isDirectory (io/file path))
        status (if exists (status-for path) :unknown)
        submodules (submodule-state path)
        eligible (and exists
                      (= :clean status)
                      (nil? (:prunable row))
                      (= :none submodules))]
    (assoc row
           :path (if exists (canonical-path path) path)
           :tree (when exists (tree-for path))
           :status status
           :removal-preflight
           {:eligible (boolean eligible)
            :submodules submodules
            :reasons (cond-> []
                       (not exists) (conj :missing-path)
                       (not= :clean status) (conj :worktree-not-clean)
                       (:prunable row) (conj :prunable-registration)
                       (not= :none submodules) (conj :submodule-state))})))

(defn- capture-supacode [root]
  (try
    (let [first-list (require-zero
                       (run-captured root ["supacode" "worktree" "list"
                                           "--with-status" "--timeout" "10"])
                       :supacode-list-unavailable)
          focused-result (require-zero
                           (run-captured root ["supacode" "worktree" "list"
                                               "--focused" "--with-status"
                                               "--timeout" "10"])
                           :supacode-focus-unavailable)
          second-list (require-zero
                        (run-captured root ["supacode" "worktree" "list"
                                            "--with-status" "--timeout" "10"])
                        :supacode-list-unavailable)]
      (if (not= (:out first-list) (:out second-list))
        {:available false :error-type :unstable-supacode-bracket :worktrees []}
        (let [focused-id (parse-focused-id (:out focused-result))]
          {:available true
           :worktrees (mapv #(assoc % :focused (= focused-id (:id %)))
                            (parse-supacode-list (:out first-list)))})))
    (catch Throwable error
      {:available false
       :error-type (or (:error-type (ex-data error)) :supacode-unavailable)
       :worktrees []})))

(defn- capture-remotes [root]
  (try
    (let [remote-result (require-zero (run-captured root ["git" "remote"])
                                      :git-remote-unavailable)
          remotes (vec (lines (:out remote-result)))]
      {:available true
       :rows
       (vec
         (mapcat
           (fn [remote]
             (let [url-sha (remote-url-digest root remote)
                   result (require-zero
                            (run-captured root ["git" "ls-remote" remote])
                            :git-remote-advertisement-unavailable)]
               (parse-ls-remote remote url-sha (:out result))))
           remotes))})
    (catch Throwable error
      {:available false
       :error-type (or (:error-type (ex-data error)) :remote-state-unavailable)
       :rows []})))

(defn- common-record-root [common-git-dir]
  (io/file common-git-dir lifecycle-directory))

(defn- record-dir [common-git-dir name]
  (io/file (common-record-root common-git-dir) name))

(defn- target-key [path]
  (lifecycle/sha256 path))

(defn read-record [path]
  (edn/read-string (slurp (io/file path))))

(defn- load-record-map [common-git-dir name]
  (let [directory (record-dir common-git-dir name)]
    (if (.isDirectory directory)
      (into {}
            (for [file (or (.listFiles directory) [])
                  :when (.isFile file)
                  :let [record (try (read-record file) (catch Throwable _ nil))]
                  :when (map? record)]
              [(or (:target record) (get-in record [:target :path])) record]))
      {})))

(defn capture-inventory
  "Capture the closed read-only Git, Supacode, remote, handoff, and lease snapshot."
  ;; @spec WTL-INV-001 WTL-INV-003 WTL-INV-007
  ([] (capture-inventory (System/getProperty "user.dir")))
  ([controller-root]
   (let [root-result (require-zero
                       (run-captured controller-root
                                     ["git" "rev-parse" "--show-toplevel"])
                       :repository-root-unavailable)
         root (canonical-path (str/trim (:out root-result)))
         common-result (require-zero
                         (run-captured root ["git" "rev-parse" "--git-common-dir"])
                         :common-git-directory-unavailable)
         common-raw (str/trim (:out common-result))
         common (canonical-path (if (.isAbsolute (io/file common-raw))
                                  common-raw
                                  (io/file root common-raw)))
         format-result (require-zero
                         (run-captured root ["git" "rev-parse"
                                             "--show-object-format"])
                         :object-format-unavailable)
         object-format (keyword (str/trim (:out format-result)))
         list-result (require-zero
                       (run-captured root ["git" "worktree" "list"
                                           "--porcelain" "-z"])
                       :git-worktree-list-unavailable)
         git-rows (mapv enrich-git-row (parse-git-worktrees (:out list-result)))
         supacode (capture-supacode root)
         snapshot
         {:schema lifecycle/snapshot-schema
          :captured-at (str (Instant/now))
          :repository {:root root
                       :common-git-dir common
                       :primary-worktree (:path (first git-rows))
                       :object-format object-format}
          :controller-worktree (canonical-path controller-root)
          :git-worktrees git-rows
          :supacode supacode
          :remotes (capture-remotes root)
          :ancestry #{}
          :handoffs (load-record-map common "handoffs")
          :lifecycle-leases (load-record-map common "leases")}]
     snapshot)))

(defn- owner-permissions! [^File file]
  (try
    (Files/setPosixFilePermissions (.toPath file)
                                   (PosixFilePermissions/fromString "rw-------"))
    (catch UnsupportedOperationException _
      (.setReadable file true true)
      (.setWritable file true true)
      (.setExecutable file false false)))
  file)

(defn- fsync-directory! [^File directory]
  (try
    (with-open [channel (FileChannel/open (.toPath directory)
                                          (into-array OpenOption
                                                      [StandardOpenOption/READ]))]
      (.force channel true))
    (catch Throwable _ nil)))

(defn- atomic-write!
  [path content create-only]
  (let [target (.getAbsoluteFile (io/file path))
        parent (.getParentFile target)
        _ (Files/createDirectories (.toPath parent)
                                   (make-array FileAttribute 0))
        tmp (io/file parent (str "." (.getName target) "." (UUID/randomUUID)
                                 ".tmp"))
        bytes (.getBytes ^String content StandardCharsets/UTF_8)]
    (if (and create-only (.exists target))
      (if (= content (slurp target))
        target
        (throw (ex-info "Create-only record already exists with different bytes"
                        {:error-type :record-already-exists
                         :path (.getPath target)})))
      (try
        (with-open [channel (FileChannel/open
                              (.toPath tmp)
                              (into-array OpenOption
                                          [StandardOpenOption/CREATE_NEW
                                           StandardOpenOption/WRITE]))]
          (loop [buffer (ByteBuffer/wrap bytes)]
            (when (.hasRemaining buffer)
              (.write channel buffer)
              (recur buffer)))
          (.force channel true))
        (owner-permissions! tmp)
        (Files/move (.toPath tmp) (.toPath target)
                    (into-array CopyOption
                                (cond-> [StandardCopyOption/ATOMIC_MOVE]
                                  (not create-only)
                                  (conj StandardCopyOption/REPLACE_EXISTING))))
        (owner-permissions! target)
        (fsync-directory! parent)
        target
        (finally
          (when (.exists tmp) (.delete tmp)))))))

(defn- write-edn! [path value create-only]
  (atomic-write! path (lifecycle/canonical-edn value) create-only))

(defn write-handoff! [common-git-dir handoff]
  ;; @spec WTL-HAND-001 WTL-HAND-002
  (let [path (io/file (record-dir common-git-dir "handoffs")
                      (str (target-key (:target handoff)) ".edn"))]
    (write-edn! path handoff true)
    {:ok true :handoff-file (.getCanonicalPath path) :handoff handoff}))

(defn write-plan! [common-git-dir plan]
  (let [validation (lifecycle/validate-plan plan)]
    (when-not (:ok validation)
      (throw (ex-info "Invalid close plan" validation)))
    (let [path (io/file (record-dir common-git-dir "plans")
                        (str (:plan-id plan) ".edn"))]
      (write-edn! path plan true)
      {:ok true :plan-file (.getCanonicalPath path) :plan plan})))

(defn journal-states [_outcome]
  transition-order)

(defn next-journal-state [_outcome journal]
  (let [last-state (get-in journal [:transitions (dec (count (:transitions journal)))
                                    :state])
        position (.indexOf transition-order last-state)]
    (when (< position (dec (count transition-order)))
      (nth transition-order (inc position)))))

(defn advance-journal! [journal-file plan state result]
  (let [journal (if (.isFile (io/file journal-file))
                  (read-record journal-file)
                  {:schema :clj-surgeon.worktree-lifecycle-journal/v1
                   :plan-id (:plan-id plan)
                   :plan-sha256 (:plan-sha256 plan)
                   :transitions []})
        expected (if (empty? (:transitions journal))
                   :prepared
                   (next-journal-state (:outcome plan) journal))]
    (when-not (= expected state)
      (throw (ex-info "Non-monotone lifecycle journal transition"
                      {:error-type :invalid-journal-transition
                       :expected expected
                       :actual state})))
    (let [updated (update journal :transitions conj {:state state :result result})]
      (write-edn! journal-file updated false)
      updated)))

(defn compile-archive-step [before after]
  (cond
    (or (not (:available before)) (not (:available after)))
    {:ok false :error-type :supacode-unavailable}
    (not= (select-keys before [:state :focused])
          (select-keys after [:state :focused]))
    {:ok false :error-type :unstable-supacode-bracket}
    (contains? #{:absent :archived} (:state before))
    {:ok true :terminal (:state before) :archive-required false}
    :else {:ok true :terminal :archived :archive-required true}))

(defn post-archive-gate [planned observed ui-state]
  (let [without-ui #(-> %
                        (dissoc :supacode)
                        (update :target dissoc :supacode))
        planned-non-ui (without-ui planned)
        observed-non-ui (without-ui observed)
        expected-ui (get-in planned [:supacode :terminal])]
    (if (and (= planned-non-ui observed-non-ui)
             (= expected-ui ui-state))
      {:ok true}
      {:ok false :error-type :post-archive-authority-drift})))

(defn handle-post-archive-drift! [{:keys [archived-by-invocation restore-fn]}]
  (if-not archived-by-invocation
    {:ok false :terminal-state :refused :error-type :post-archive-authority-drift}
    (try
      (restore-fn)
      {:ok false :terminal-state :restored-refusal
       :error-type :post-archive-authority-drift}
      (catch Throwable error
        {:ok false :terminal-state :partial
         :error-type :supacode-restore-failed
         :class (.getName (class error))}))))

(defn removal-command [controller target]
  ["git" "-C" controller "worktree" "remove" target])

(defn handle-removal-failure! [{:keys [archived-by-invocation restore-fn]}]
  (if-not archived-by-invocation
    {:ok false :terminal-state :refused :error-type :git-worktree-remove-failed}
    (try
      (if (false? (restore-fn))
        {:ok false :terminal-state :partial :error-type :supacode-restore-failed}
        {:ok false :terminal-state :restored-refusal
         :error-type :git-worktree-remove-failed})
      (catch Throwable error
        {:ok false :terminal-state :partial
         :error-type :supacode-restore-failed
         :class (.getName (class error))}))))

(defn- recover-removal-failure!
  [lease-path plan archived-by-invocation restore-fn]
  (let [lease-restored
        (try
          (write-edn! lease-path (lifecycle/expected-lifecycle-lease plan) true)
          true
          (catch Throwable _ false))
        ui-result (handle-removal-failure!
                    {:archived-by-invocation archived-by-invocation
                     :restore-fn restore-fn})]
    (if (and lease-restored
             (contains? #{:refused :restored-refusal} (:terminal-state ui-result)))
      ui-result
      {:ok false
       :terminal-state :partial
       :error-type :removal-recovery-incomplete
       :lease-restored lease-restored
       :ui-terminal-state (:terminal-state ui-result)
       :ui-error-type (:error-type ui-result)})))

(defn validate-terminal-postconditions [plan postconditions]
  (cond
    (:target-present postconditions) {:ok false :error-type :path-reused}
    (:registration-present postconditions)
    {:ok false :error-type :registration-still-present}
    (not (:refs-unchanged postconditions)) {:ok false :error-type :ref-drift}
    (not (:evidence-unchanged postconditions))
    {:ok false :error-type :evidence-drift}
    (not= (get-in plan [:supacode :terminal]) (:supacode-state postconditions))
    {:ok false :error-type :supacode-terminal-state-mismatch}
    :else {:ok true}))

(defn validate-completion-pair [receipt marker]
  (if (and (= (:plan-id receipt) (:plan-id marker))
           (= :clj-surgeon.worktree-close-receipt/v1 (:schema receipt))
           (= :clj-surgeon.worktree-parking-completion/v1 (:schema marker))
           (string? (:receipt-sha256 marker)))
    {:ok true}
    {:ok false :error-type :invalid-parking-completion-pair}))

(defn- exact-completion-pair? [receipt marker]
  (and (:ok (validate-completion-pair receipt marker))
       (= (:plan-sha256 receipt) (:plan-sha256 marker))
       (= (lifecycle/sha256 (lifecycle/canonical-edn receipt))
          (:receipt-sha256 marker))))

(defn validate-replay [_plan postconditions]
  (lifecycle/validate-terminal-replay postconditions))

(defn- inside-root? [root path]
  (let [root-path (.toPath (canonical-file root))
        target-path (.toPath (canonical-file path))]
    (and (not= root-path target-path) (.startsWith target-path root-path))))

(defn apply-plan!
  "Apply one exact plan through injected or real adapters.

  Tests must pass `fixture-root`; the public file-based entrance builds the
  real adapters and performs the same journal transitions."
  ;; @spec WTL-APPLY-001 WTL-APPLY-002 WTL-APPLY-003 WTL-APPLY-004
  ;; @spec WTL-APPLY-005 WTL-APPLY-006 WTL-APPLY-007 WTL-APPLY-008
  ;; @spec WTL-APPLY-009 WTL-APPLY-010 WTL-APPLY-011 WTL-APPLY-012
  [{:keys [plan snapshot-fn archive-fn remove-fn terminal-fn fixture-root]}]
  (when (and fixture-root
             (not (inside-root? fixture-root (get-in plan [:target :path]))))
    (throw (ex-info "Fixture target escaped its owned root"
                    {:error-type :fixture-target-outside-root})))
  (let [snapshot-fn (or snapshot-fn (constantly plan))
        archive-fn (or archive-fn (constantly true))
        remove-fn (or remove-fn (constantly true))
        terminal-fn (or terminal-fn (constantly true))
        before (snapshot-fn)]
    (if (not= (dissoc plan :plan-sha256) (dissoc before :plan-sha256))
      {:ok false :error-type :pre-apply-authority-drift}
      (do
        (archive-fn)
        (let [after (snapshot-fn)]
          (if (not= (dissoc plan :plan-sha256) (dissoc after :plan-sha256))
            (handle-post-archive-drift!
              {:archived-by-invocation true :restore-fn (constantly true)})
            (do
              (remove-fn)
              (if (terminal-fn)
                {:ok true
                 :terminal-state :complete
                 :journal (mapv (fn [state]
                                  {:state state
                                   :result (if (and (not= :parked (:outcome plan))
                                                    (contains?
                                                      #{:parking-intent-recorded
                                                        :parking-completion-verified}
                                                      state))
                                             :not-applicable
                                             :ok)})
                                transition-order)}
                {:ok false :terminal-state :partial
                 :error-type :terminal-postcondition-failed}))))))))

(defn- current-target-view [snapshot path]
  (when-let [row (some #(when (= path (:path %)) %)
                       (:git-worktrees snapshot))]
    (let [supacode (some #(when (= path (:path %)) %)
                         (get-in snapshot [:supacode :worktrees]))]
      {:path (:path row)
       :head (:head row)
       :tree (:tree row)
       :branch (:branch row)
       :detached (:detached row)
       :status (:status row)
       :locked (:locked row)
       :removal-preflight (:removal-preflight row)
       :supacode (if supacode
                   {:id (:id supacode)
                    :initial (:status supacode)
                    :terminal :archived}
                   {:id nil :initial :absent :terminal :absent})})))

(defn- state-index [state]
  (.indexOf transition-order state))

(defn- journal-state [journal-file]
  (when (.isFile (io/file journal-file))
    (get-in (read-record journal-file)
            [:transitions (dec (count (:transitions
                                        (read-record journal-file)))) :state])))

(defn- transition-reached? [journal-file state]
  (let [current (journal-state journal-file)]
    (and current (<= (state-index state) (state-index current)))))

(defn- ensure-transition! [journal-file plan state result]
  (if (transition-reached? journal-file state)
    (read-record journal-file)
    (advance-journal! journal-file plan state result)))

(defn- run-transition! [journal-file plan state result-fn]
  (if (transition-reached? journal-file state)
    (read-record journal-file)
    (advance-journal! journal-file plan state (result-fn))))

(defn- supacode-status [root id]
  (if-not id
    {:available true :state :absent :focused false}
    (let [result (run-captured root ["supacode" "worktree" "status"
                                     "--worktree" id "--timeout" "10"])]
      (if-not (zero? (:exit result))
        {:available false :state :unknown :focused nil}
        (let [values (into {}
                           (for [line (lines (:out result))
                                 :let [[key value] (str/split line #"=" 2)]]
                             [(keyword key) value]))]
          {:available true
           :state (if (= "true" (:archived values))
                    :archived
                    (keyword (:status values)))
           :focused (= "true" (:focused values))})))))

(defn- archive-supacode! [root id]
  (when id
    (require-zero
      (run-captured root ["supacode" "worktree" "archive"
                          "--worktree" id "--background" "--timeout" "30"])
      :supacode-archive-failed))
  true)

(defn- restore-supacode! [root id]
  (when id
    (require-zero
      (run-captured root ["supacode" "worktree" "unarchive"
                          "--worktree" id "--background" "--timeout" "30"])
      :supacode-restore-failed))
  true)

(defn- parking-record [plan]
  {:schema :clj-surgeon.worktree-parking-intent/v1
   :plan-sha256 (:plan-sha256 plan)
   :target (select-keys (:target plan) [:path :head :tree])
   :upstream (get-in plan [:outcome-evidence :upstream])
   :owner (get-in plan [:outcome-evidence :issue :owner])
   :issue-revision (get-in plan [:outcome-evidence :issue :revision])
   :next-action (get-in plan [:outcome-evidence :next-action])
   :expiry (get-in plan [:outcome-evidence :expiry])})

(defn- read-issue-row [plan]
  (let [issue (get-in plan [:outcome-evidence :issue])
        directory (some-> (:store issue) io/file .getParent)
        result (require-zero
                 (run-captured directory ["bd" "--directory" directory
                                          "show" (:id issue) "--json"])
                 :parked-issue-unavailable)
        parsed (json/parse-string (:out result) true)]
    (if (sequential? parsed) (first parsed) parsed)))

(defn- contains-record? [row record]
  (str/includes? (or (:notes row) "")
                 (str/trim (lifecycle/canonical-edn record))))

(defn- ends-with-record? [row record]
  (str/ends-with? (str/trim (or (:notes row) ""))
                  (str/trim (lifecycle/canonical-edn record))))

(defn- issue-revision [row]
  (or (:revision row) (:updated_at row)))

(defn- journal-issue-revision [plan]
  (let [common (get-in plan [:repository :common-git-dir])
        path (io/file (record-dir common "journals")
                      (str (:plan-id plan) ".edn"))]
    (when (.isFile path)
      (some (fn [{:keys [state result]}]
              (when (and (contains? #{:parking-completion-verified
                                      :parking-intent-recorded}
                                    state)
                         (map? result))
                (:revision-after result)))
            (reverse (:transitions (read-record path)))))))

(defn- ensure-parking-intent! [plan]
  (if-not (= :parked (:outcome plan))
    :not-applicable
    (let [issue (get-in plan [:outcome-evidence :issue])
          directory (some-> (:store issue) io/file .getParent)
          issue-id (:id issue)
          record (lifecycle/canonical-edn (parking-record plan))
          row (read-issue-row plan)]
      (cond
        (ends-with-record? row (parking-record plan))
        {:result :already-recorded
         :revision-before (get-in plan [:outcome-evidence :issue :revision])
         :revision-after (issue-revision row)}

        (contains-record? row (parking-record plan))
        (throw (ex-info "Parked issue changed after lifecycle intent"
                        {:error-type :parked-issue-revision-drift}))

        :else
        (let [expected (get-in plan [:outcome-evidence :issue :revision])]
          (when-not (= expected (issue-revision row))
            (throw (ex-info "Parked issue revision drifted before append"
                            {:error-type :parked-issue-revision-drift})))
          (require-zero
            (run-captured directory ["bd" "--directory" directory
                                     "update" issue-id "--append-notes" record
                                     "--json"])
            :parking-intent-append-failed)
          {:result :recorded
           :revision-before expected
           :revision-after (issue-revision (read-issue-row plan))})))))

(declare parking-completion-record)

(defn- issue-current? [plan]
  (if-not (= :parked (:outcome plan))
    true
    (let [issue (get-in plan [:outcome-evidence :issue])
          row (try (read-issue-row plan) (catch Throwable _ nil))
          common (get-in plan [:repository :common-git-dir])
          receipt-path (io/file (record-dir common "receipts")
                                (str (:plan-id plan) ".edn"))
          journal-revision (journal-issue-revision plan)
          current-revision (some-> row issue-revision)
          completion-current
          (and (.isFile receipt-path)
               (ends-with-record? row (parking-completion-record
                                        plan (read-record receipt-path))))
          revision-current
          (if completion-current
            (or (= journal-revision current-revision)
                (and (integer? journal-revision)
                     (= (inc journal-revision) current-revision)))
            (= (or journal-revision (:revision issue)) current-revision))]
      (and row
           (try
             (.isAfter (Instant/parse (get-in plan [:outcome-evidence :expiry]))
                       (Instant/now))
             (catch Throwable _ false))
           (= "open" (:status row))
           (= (:id issue) (:id row))
           (= (:owner issue) (or (:assignee row) (:owner row)))
           revision-current
           (or (ends-with-record? row (parking-record plan))
               completion-current)))))

(defn- remote-evidence-current? [snapshot evidence]
  (some #(every? (fn [key] (= (get % key) (get evidence key)))
                 [:remote :remote-url-sha256 :ref :object :peeled-object])
        (get-in snapshot [:remotes :rows])))

(defn- ancestry-current? [root ancestor descendant]
  (zero? (:exit (run-captured root ["git" "merge-base" "--is-ancestor"
                                    ancestor descendant]))))

(defn- terminal-paths-current? [root plan]
  (every?
    (fn [path]
      (zero? (:exit (run-captured root ["git" "cat-file" "-e"
                                        (str (get-in plan
                                                     [:outcome-evidence
                                                      :breadcrumb :object])
                                             ":" path)]))))
    (get-in plan [:outcome-evidence :terminal-paths])))

(defn- sha256-bytes [bytes]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256") bytes)]
    (apply str (map #(format "%02x" (bit-and % 0xff)) digest))))

(defn- one-seal [document]
  (let [begin (.indexOf document negative-seal-begin)
        second-begin (when (not= -1 begin)
                       (.indexOf document negative-seal-begin
                                 (+ begin (count negative-seal-begin))))
        end (when (not= -1 begin)
              (.indexOf document negative-seal-end
                        (+ begin (count negative-seal-begin))))
        second-end (when (and end (not= -1 end))
                     (.indexOf document negative-seal-end
                               (+ end (count negative-seal-end))))]
    (when (and (not= -1 begin) (= -1 second-begin)
               end (not= -1 end) (= -1 second-end))
      (try
        (edn/read-string
          (str/trim (subs document (+ begin (count negative-seal-begin)) end)))
        (catch Throwable _ nil)))))

(defn- durable-archive-current? [target-path raw receipt]
  (case (:kind raw)
    :none true
    :archive
    (let [locator (str/replace (:archive-locator raw) #"^file://" "")
          archive (io/file locator)
          target (canonical-file target-path)]
      (and (.isAbsolute archive)
           (.isFile archive)
           (not (str/starts-with? (.getCanonicalPath archive) "/tmp/"))
           (not (str/starts-with? (.getCanonicalPath archive) "/private/tmp/"))
           (not (.startsWith (.toPath (.getCanonicalFile archive))
                             (.toPath target)))
           (= (:archive-sha256 raw)
              (sha256-bytes (Files/readAllBytes (.toPath archive))))
           (str/includes? receipt (:archive-locator raw))
           (str/includes? receipt (:archive-sha256 raw))))
    false))

(defn- validate-negative-authority! [root target evidence]
  (let [breadcrumb (:breadcrumb evidence)
        object (:object breadcrumb)
        document-result (require-zero
                          (run-captured root ["git" "show"
                                              (str object ":" (:path breadcrumb))])
                          :negative-breadcrumb-unavailable)
        document (:out document-result)
        seal (one-seal document)
        experiment (get seal :experiment)
        tree-result (run-captured root ["git" "rev-parse"
                                        (str (:commit experiment) "^{tree}")])
        experiment-reachable
        (ancestry-current? root (:commit experiment) object)
        target-published (ancestry-current? root (:head target) object)
        diff-result (run-captured root ["git" "diff" "--name-only" "-z"
                                        (:commit experiment) (:head target)])
        changed (set (remove str/blank? (split-nul (:out diff-result))))
        allowed (set (:allowed-terminal-paths seal))
        raw (:raw-evidence seal)
        receipt-result (when (= :archive (:kind raw))
                         (run-captured root
                                       ["git" "show"
                                        (str (:receipt-ref raw) ":"
                                             (:receipt-path raw))]))]
    (when-not (and (= (:seal evidence) seal)
                   (:ok (lifecycle/validate-negative-seal seal))
                   (= (:blob-sha256 breadcrumb)
                      (lifecycle/sha256 document))
                   (zero? (:exit tree-result))
                   (= (:tree experiment) (str/trim (:out tree-result)))
                   experiment-reachable
                   target-published
                   (zero? (:exit diff-result))
                   (set/subset? changed allowed)
                   (= (set (:terminal-paths evidence)) allowed)
                   (or (= :none (:kind raw))
                       (and receipt-result
                            (zero? (:exit receipt-result))
                            (durable-archive-current?
                              (:path target) raw (:out receipt-result)))))
      (throw (ex-info "Negative-experiment authority is not durable"
                      {:error-type :negative-experiment-evidence-not-proved})))
    true))

(defn- outcome-authority-current? [plan snapshot root]
  (let [evidence (:outcome-evidence plan)]
    (case (:outcome plan)
      :landed
      (and (remote-evidence-current? snapshot evidence)
           (ancestry-current? root (get-in plan [:target :head])
                              (:object evidence)))

      :negative-experiment
      (and (:ok (lifecycle/validate-negative-seal (:seal evidence)))
           (remote-evidence-current? snapshot (:breadcrumb evidence))
           (= (get-in plan [:target :head])
              (get-in evidence [:breadcrumb :object]))
           (= (:terminal-paths evidence)
              (get-in evidence [:seal :allowed-terminal-paths]))
           (terminal-paths-current? root plan)
           (try
             (validate-negative-authority! root (:target plan) evidence)
             true
             (catch Throwable _ false)))

      :parked
      (and (remote-evidence-current? snapshot (:upstream evidence))
           (= (get-in plan [:target :head])
              (get-in evidence [:upstream :object]))
           (issue-current? plan))

      false)))

(defn- lease-file [common plan]
  (io/file (record-dir common "leases")
           (str (target-key (get-in plan [:target :path])) ".edn")))

(defn- journal-file [common plan]
  (io/file (record-dir common "journals") (str (:plan-id plan) ".edn")))

(defn- receipt-file [common plan]
  (io/file (record-dir common "receipts") (str (:plan-id plan) ".edn")))

(defn- persisted-plan-file [common plan]
  (io/file (record-dir common "plans") (str (:plan-id plan) ".edn")))

(defn- unconsumed-plans-for-target [common plan]
  (let [directory (record-dir common "plans")]
    (vec
      (for [file (or (.listFiles directory) [])
            :when (.isFile file)
            :let [candidate (try (read-record file) (catch Throwable _ nil))]
            :when (and (map? candidate)
                       (= (get-in plan [:target :path])
                          (get-in candidate [:target :path]))
                       (not (.isFile (receipt-file common candidate))))]
        candidate))))

(defn- completion-file [common plan]
  (io/file (record-dir common "completions") (str (:plan-id plan) ".edn")))

(defn- lock-file [common plan]
  (io/file (record-dir common "locks")
           (str (target-key (get-in plan [:target :path])) ".lock")))

(defn- acquire-file-lock [file]
  (Files/createDirectories (.toPath (.getParentFile file))
                           (make-array FileAttribute 0))
  (let [channel (FileChannel/open (.toPath file)
                                  (into-array OpenOption
                                              [StandardOpenOption/CREATE
                                               StandardOpenOption/WRITE]))
        lock (try (.tryLock channel) (catch Throwable _ nil))]
    (if lock
      {:channel channel :lock lock}
      (do (.close channel)
          (throw (ex-info "Lifecycle target is locked by another controller"
                          {:error-type :lifecycle-target-locked}))))))

(defn- release-file-lock! [{:keys [^FileChannel channel lock]}]
  (when lock (.release lock))
  (when channel (.close channel)))

(declare controller-identity)

(defn- validate-current-authorities!
  [plan snapshot controller-root lease-state target-required]
  (let [target-path (get-in plan [:target :path])
        target (current-target-view snapshot target-path)
        expected-lease (lifecycle/expected-lifecycle-lease plan)
        observed-lease (get-in snapshot [:lifecycle-leases target-path])
        expected-handoff (:handoff plan)
        observed-handoff (if (= {:mode :legacy} expected-handoff)
                           {:mode :legacy}
                           (get-in snapshot [:handoffs target-path]))
        current-controller (controller-identity controller-root)]
    (when-not (= (:repository plan) (:repository snapshot))
      (throw (ex-info "Repository identity drifted"
                      {:error-type :repository-authority-drift})))
    (when-not (and (:clean current-controller)
                   (= (:controller plan) current-controller))
      (throw (ex-info "Controller identity or cleanliness drifted"
                      {:error-type :controller-authority-drift})))
    (when-not (= expected-handoff observed-handoff)
      (throw (ex-info "Owner handoff drifted"
                      {:error-type :handoff-authority-drift})))
    (when-not (case lease-state
                :absent (nil? observed-lease)
                :matching (= expected-lease observed-lease)
                :released (nil? observed-lease)
                false)
      (throw (ex-info "Lifecycle lease authority drifted"
                      {:error-type :lifecycle-lease-drift})))
    (when (and target-required
               (not= (dissoc (:target plan) :supacode)
                     (some-> target (dissoc :supacode))))
      (throw (ex-info "Target-specific authority drifted"
                      {:error-type :pre-apply-authority-drift})))
    (when-not (outcome-authority-current? plan snapshot
                                          (get-in plan [:repository :root]))
      (throw (ex-info "Outcome authority drifted"
                      {:error-type :outcome-authority-drift})))
    target))

(defn- terminal-postconditions [plan root]
  (let [target (get-in plan [:target :path])
        id (get-in plan [:supacode :id])
        snapshot (capture-inventory root)
        ui (supacode-status root id)
        outcome-current (outcome-authority-current? plan snapshot root)]
    {:target-present (.exists (io/file target))
     :registration-present (boolean (current-target-view snapshot target))
     :refs-unchanged outcome-current
     :evidence-unchanged outcome-current
     :supacode-state (:state ui)}))

(defn- validate-journal! [journal plan]
  (let [states (mapv :state (:transitions journal))
        expected-prefix (subvec transition-order 0 (count states))]
    (when-not (and (= :clj-surgeon.worktree-lifecycle-journal/v1
                      (:schema journal))
                   (= (:plan-id plan) (:plan-id journal))
                   (= (:plan-sha256 plan) (:plan-sha256 journal))
                   (= expected-prefix states))
      (throw (ex-info "Lifecycle journal does not belong to this plan"
                      {:error-type :invalid-lifecycle-journal})))
    journal))

(defn- matching-lease? [plan lease]
  (= (lifecycle/expected-lifecycle-lease plan) lease))

(defn- delete-matching-lease! [lease-path plan]
  (let [file (io/file lease-path)
        expected (lifecycle/expected-lifecycle-lease plan)]
    (when-not (and (.isFile file) (= expected (read-record file)))
      (throw (ex-info "Lifecycle lease changed before release"
                      {:error-type :lifecycle-lease-drift})))
    (Files/delete (.toPath file))
    (fsync-directory! (.getParentFile file))
    (when (.exists file)
      (throw (ex-info "Lifecycle lease release was not durable"
                      {:error-type :lifecycle-lease-release-failed})))
    true))

(defn- parking-completion-record [plan receipt]
  {:schema :clj-surgeon.worktree-parking-completion-record/v1
   :plan-id (:plan-id plan)
   :plan-sha256 (:plan-sha256 plan)
   :receipt-sha256 (lifecycle/sha256 (lifecycle/canonical-edn receipt))})

(defn- ensure-parking-completion! [common plan receipt]
  (if-not (= :parked (:outcome plan))
    :not-applicable
    (let [issue (get-in plan [:outcome-evidence :issue])
          directory (some-> (:store issue) io/file .getParent)
          issue-id (:id issue)
          completion (parking-completion-record plan receipt)
          completion-bytes (lifecycle/canonical-edn completion)
          before (read-issue-row plan)
          revision-before (issue-revision before)
          journal-revision (journal-issue-revision plan)
          completion-current (ends-with-record? before completion)]
      (cond
        completion-current
        (when-not (= (inc journal-revision) revision-before)
          (throw (ex-info "Parked issue revision drifted after completion"
                          {:error-type :parked-issue-revision-drift})))

        (contains-record? before completion)
        (throw (ex-info "Parking completion was superseded"
                        {:error-type :parked-issue-revision-drift}))

        (= journal-revision revision-before)
        (require-zero
          (run-captured directory ["bd" "--directory" directory
                                   "update" issue-id "--append-notes"
                                   completion-bytes "--json"])
          :parking-completion-append-failed)

        :else
        (throw (ex-info "Parked issue revision drifted before completion"
                        {:error-type :parked-issue-revision-drift})))
      (let [after (read-issue-row plan)]
        (when-not (and (= (inc journal-revision) (issue-revision after))
                       (contains-record? after (parking-record plan))
                       (ends-with-record? after completion))
          (throw (ex-info "Parking completion record was not retained"
                          {:error-type :parking-completion-unverified}))))
      (let [after (read-issue-row plan)
            marker {:schema :clj-surgeon.worktree-parking-completion/v1
                    :plan-id (:plan-id plan)
                    :plan-sha256 (:plan-sha256 plan)
                    :receipt-sha256 (:receipt-sha256 completion)}
            marker-file (completion-file common plan)]
        (write-edn! marker-file marker true)
        (when-not (exact-completion-pair? receipt marker)
          (throw (ex-info "Parking completion marker does not match receipt"
                          {:error-type :invalid-parking-completion-pair})))
        {:result :completed
         :revision-before journal-revision
         :revision-after (issue-revision after)
         :marker marker
         :marker-file (.getCanonicalPath marker-file)}))))

(defn- parking-completion-current? [plan receipt]
  (if-not (= :parked (:outcome plan))
    true
    (try
      (let [row (read-issue-row plan)]
        (and (contains-record? row (parking-record plan))
             (ends-with-record? row (parking-completion-record plan receipt))
             (= (journal-issue-revision plan) (issue-revision row))))
      (catch Throwable _ false))))

(defn apply-plan-file!
  "Apply one reviewed plan through the real adapters.

  This entrance is implemented but is not exercised against a real worktree by
  the MVP build. The first invocation remains a separate Gene-gated trial."
  [plan-path]
  (let [plan (read-record plan-path)
        validation (lifecycle/validate-plan plan)]
    (when-not (:ok validation)
      (throw (ex-info "Reviewed plan is invalid" validation)))
    (let [common (get-in plan [:repository :common-git-dir])
          root (get-in plan [:repository :root])
          controller (System/getProperty "user.dir")
          target (get-in plan [:target :path])
          lock-path (lock-file common plan)
          handle (acquire-file-lock lock-path)]
      (try
        (when-not (= (canonical-path plan-path)
                     (canonical-path (persisted-plan-file common plan)))
          (throw (ex-info "Apply requires the persisted reviewed plan"
                          {:error-type :unreviewed-plan-path})))
        (let [unconsumed (mapv :plan-sha256
                               (unconsumed-plans-for-target common plan))
              terminal? (.isFile (receipt-file common plan))]
          (when-not (if terminal?
                      (empty? unconsumed)
                      (= [(:plan-sha256 plan)] unconsumed))
            (throw (ex-info "Target has multiple or different unconsumed plans"
                            {:error-type :ambiguous-unconsumed-plan}))))
        (when (= (canonical-path controller) (canonical-path target))
          (throw (ex-info "Controller cannot remove its own worktree"
                          {:error-type :controller-target-collision})))
        (let [lease-path (lease-file common plan)
              journal-path (journal-file common plan)
              receipt-path (receipt-file common plan)
              marker-path (completion-file common plan)
              journal (when (.isFile journal-path)
                        (validate-journal! (read-record journal-path) plan))
              state (some-> journal :transitions last :state)
              initial-snapshot (capture-inventory controller)
              initial-lease (get-in initial-snapshot [:lifecycle-leases target])
              current-controller (controller-identity controller)]
          (when-not (and (:clean current-controller)
                         (= (:controller plan) current-controller))
            (throw (ex-info "Controller identity or cleanliness drifted"
                            {:error-type :controller-authority-drift})))
          (when (and (.isFile receipt-path) (nil? journal))
            (throw (ex-info "Terminal receipt has no owning journal"
                            {:error-type :orphan-terminal-receipt})))
          (when (and initial-lease (not (matching-lease? plan initial-lease)))
            (throw (ex-info "A foreign lifecycle lease owns this target"
                            {:error-type :lifecycle-lease-exists})))
          (when (and state
                     (<= (state-index :remove-verified) (state-index state))
                     initial-lease)
            (throw (ex-info "A post-removal journal retained its lease"
                            {:error-type :lifecycle-lease-drift})))
          (if (and state
                   (< (state-index state) (state-index :remove-commanded)))
            (when-not initial-lease
              (throw (ex-info "Recovery lost its lifecycle lease"
                              {:error-type :lifecycle-lease-missing})))
            (when (and (nil? state) initial-lease)
              ;; A crash may occur after the create-only lease and before the
              ;; first prepared journal write. Exact matching bytes recover it.
              (when-not (matching-lease? plan initial-lease)
                (throw (ex-info "Lifecycle lease does not match the plan"
                                {:error-type :lifecycle-lease-drift})))))

          (when-not state
            (validate-current-authorities! plan initial-snapshot controller
                                           (if initial-lease :matching :absent)
                                           true)
            (when-not initial-lease
              (write-edn! lease-path (lifecycle/expected-lifecycle-lease plan)
                          true))
            (ensure-transition! journal-path plan :prepared :ok))

          (when-not (transition-reached? journal-path :remove-commanded)
            (let [snapshot (capture-inventory controller)]
              (validate-current-authorities! plan snapshot controller :matching
                                             true))
            (run-transition! journal-path plan :parking-intent-recorded
                             #(ensure-parking-intent! plan))
            (let [supacode-id (get-in plan [:supacode :id])
                  planned-initial (get-in plan [:supacode :initial])
                  planned-terminal (get-in plan [:supacode :terminal])
                  before-ui (supacode-status root supacode-id)
                  archive-required (not (contains? #{:absent :archived}
                                                   planned-initial))]
              (when-not (:available before-ui)
                (throw (ex-info "Supacode state is unavailable"
                                {:error-type :supacode-unavailable})))
              (ensure-transition! journal-path plan :archive-commanded
                                  (if archive-required :commanded
                                      :not-applicable))
              (when (and archive-required
                         (not= planned-terminal (:state before-ui)))
                (archive-supacode! root supacode-id))
              (let [archived-ui (supacode-status root supacode-id)]
                (when-not (and (:available archived-ui)
                               (= planned-terminal (:state archived-ui))
                               (not (:focused archived-ui)))
                  (throw (ex-info "Supacode archive postcondition failed"
                                  {:error-type :supacode-archive-unverified})))
                (ensure-transition! journal-path plan :archive-verified :ok))
              (try
                (let [after (capture-inventory controller)]
                  (validate-current-authorities! plan after controller :matching
                                                 true))
                (catch Throwable error
                  (let [failure (handle-post-archive-drift!
                                  {:archived-by-invocation archive-required
                                   :restore-fn #(restore-supacode! root
                                                                   supacode-id)})]
                    (throw (ex-info "Post-archive safety gate refused"
                                    (merge failure (ex-data error)))))))))

          (when-not (transition-reached? journal-path :remove-verified)
            (ensure-transition! journal-path plan :remove-commanded :commanded)
            (let [before-remove (capture-inventory controller)
                  target-before-remove (current-target-view before-remove target)
                  lease-before-remove (get-in before-remove
                                              [:lifecycle-leases target])
                  supacode-id (get-in plan [:supacode :id])
                  remove-ui (supacode-status root supacode-id)
                  archived-by-operation
                  (not (contains? #{:absent :archived}
                                  (get-in plan [:supacode :initial])))]
              (when-not (and (:available remove-ui)
                             (= (get-in plan [:supacode :terminal])
                                (:state remove-ui))
                             (not (:focused remove-ui)))
                (throw (ex-info "Supacode drifted before Git removal"
                                {:error-type :pre-remove-supacode-drift})))
              (if target-before-remove
                (do
                  (validate-current-authorities!
                    plan before-remove controller
                    (if lease-before-remove :matching :released) true)
                  (when lease-before-remove
                    (delete-matching-lease! lease-path plan))
                  (let [removal (run-captured controller
                                              (removal-command controller target))]
                    (when-not (zero? (:exit removal))
                      (let [failure (recover-removal-failure!
                                      lease-path plan archived-by-operation
                                      #(restore-supacode! root supacode-id))]
                        (throw (ex-info "Git worktree removal failed" failure))))))
                (when lease-before-remove
                  (throw (ex-info "Removed target retained a lifecycle lease"
                                  {:error-type :lifecycle-lease-drift}))))
              (let [after-remove (capture-inventory controller)]
                (when (or (.exists (io/file target))
                          (current-target-view after-remove target))
                  (throw (ex-info "Git worktree removal was not verified"
                                  {:error-type :git-worktree-remove-unverified})))
                (when (get-in after-remove [:lifecycle-leases target])
                  (throw (ex-info "Removed target retained a lifecycle lease"
                                  {:error-type :lifecycle-lease-drift})))
                (validate-current-authorities! plan after-remove controller
                                               :released false)
                (ensure-transition! journal-path plan :remove-verified :ok))))

          (let [postconditions (terminal-postconditions plan root)
                terminal (validate-terminal-postconditions plan postconditions)]
            (when-not (:ok terminal)
              (throw (ex-info "Terminal postconditions failed" terminal)))
            (let [compiled (:receipt
                             (lifecycle/compile-receipt plan postconditions))
                  receipt (if (.isFile receipt-path)
                            (let [existing (read-record receipt-path)]
                              (when-not (= compiled existing)
                                (throw (ex-info "Terminal receipt drifted"
                                                {:error-type
                                                 :terminal-receipt-drift})))
                              existing)
                            (do (write-edn! receipt-path compiled true)
                                compiled))]
              (ensure-transition! journal-path plan :final-receipt-written :ok)
              (run-transition! journal-path plan :parking-completion-verified
                               #(ensure-parking-completion! common plan receipt))
              (when (= :parked (:outcome plan))
                (let [marker (when (.isFile marker-path)
                               (read-record marker-path))]
                  (when-not (and (exact-completion-pair? receipt marker)
                                 (parking-completion-current? plan receipt))
                    (throw (ex-info "Parking completion marker is invalid"
                                    {:error-type
                                     :invalid-parking-completion-pair})))))
              {:ok true
               :replayed (boolean state)
               :terminal-state :complete
               :receipt-file (.getCanonicalPath receipt-path)
               :receipt receipt})))
        (finally
          (release-file-lock! handle))))))

(defn- controller-identity [root]
  (let [head (-> (require-zero (run-captured root ["git" "rev-parse" "HEAD"])
                               :controller-head-unavailable)
                 :out str/trim)
        tree (-> (require-zero (run-captured root ["git" "rev-parse" "HEAD^{tree}"])
                               :controller-tree-unavailable)
                 :out str/trim)
        status (require-zero
                 (run-captured root ["git" "status" "--porcelain=v2" "-z"
                                     "--untracked-files=all"])
                 :controller-status-unavailable)
        artifacts (into (sorted-map)
                        (for [path ["Makefile"
                                    "src/clj_surgeon/worktree_lifecycle.clj"
                                    "src/clj_surgeon/worktree_lifecycle_io.clj"]
                              :let [file (io/file root path)]
                              :when (.isFile file)]
                          [path (sha256-file file)]))]
    {:commit head
     :tree tree
     :clean (str/blank? (:out status))
     :artifacts artifacts}))

(defn- add-ancestry [snapshot request]
  (if (= :landed (:outcome request))
    (let [root (get-in snapshot [:repository :root])
          target (some #(when (= (:target request) (:path %)) %)
                       (:git-worktrees snapshot))
          destination (get-in request [:evidence :object])
          result (when (and target destination)
                   (run-captured root ["git" "merge-base" "--is-ancestor"
                                       (:head target) destination]))]
      (if (and result (zero? (:exit result)))
        (update snapshot :ancestry conj [(:head target) destination])
        snapshot))
    snapshot))

(defn dry-run-plan! [request-file]
  ;; @spec WTL-PLAN-001 WTL-PLAN-002 WTL-PLAN-003 WTL-PLAN-004
  (let [request (read-record request-file)
        snapshot (add-ancestry (capture-inventory) request)
        root (get-in snapshot [:repository :root])
        target (some #(when (= (:target request) (:path %)) %)
                     (:git-worktrees snapshot))
        _ (when (= :negative-experiment (:outcome request))
            (validate-negative-authority! root target (:evidence request)))
        plan-result (lifecycle/compile-plan snapshot request
                                            (controller-identity root)
                                            (lifecycle/new-plan-id))]
    (when-not (:ok plan-result)
      (throw (ex-info "Close plan refused" plan-result)))
    (write-plan! (get-in snapshot [:repository :common-git-dir])
                 (:plan plan-result))))

(defn handoff-worktree! [request-file]
  (let [request (read-record request-file)
        snapshot (capture-inventory)
        target (some #(when (= (:target request) (:path %)) %)
                     (:git-worktrees snapshot))
        owner (some-> (:lock-reason target)
                      (str/split #"\s+")
                      first
                      (str/replace #"^owner=" ""))
        handoff (lifecycle/compile-handoff snapshot request owner
                                           (str (UUID/randomUUID)))]
    (when-not (= lifecycle/handoff-schema (:schema handoff))
      (throw (ex-info "Handoff refused" handoff)))
    (merge (write-handoff! (get-in snapshot [:repository :common-git-dir])
                           handoff)
           {:unlock-command (lifecycle/handoff-unlock-command handoff)})))

(defn- print-result! [result]
  (print (lifecycle/canonical-edn result))
  (flush)
  result)

(defn command-result [arguments]
  ;; @spec WTL-CLI-001 WTL-CLI-002 WTL-CLI-003
  (let [[command value & extra] arguments]
    (when (seq extra)
      (throw (ex-info "Unexpected lifecycle arguments"
                      {:error-type :invalid-command-arguments})))
    (case command
      "audit" (let [snapshot (capture-inventory)
                    result (lifecycle/compile-audit snapshot)]
                (if (str/blank? value)
                  result
                  (do (write-edn! value result false)
                      (assoc result :output (canonical-path value)))))
      "handoff" (handoff-worktree! value)
      "plan" (dry-run-plan! value)
      "apply" (apply-plan-file! value)
      (throw (ex-info "Unknown lifecycle command"
                      {:error-type :unknown-lifecycle-command})))))

(defn -main [& arguments]
  (try
    (print-result! (command-result arguments))
    (catch clojure.lang.ExceptionInfo error
      (print-result! (merge {:ok false} (ex-data error)))
      (System/exit 2))
    (catch Throwable error
      (print-result! {:ok false
                      :error-type :lifecycle-execution-failed
                      :class (.getName (class error))})
      (System/exit 5))))
