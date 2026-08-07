(ns clj-surgeon.mcp-change-buffer
  "Proof-carrying semantic selection followed by one addressed transaction."
  (:require
   [clj-surgeon.file-ops :as file-ops]
   [clj-surgeon.intent-transaction :as transaction]
   [clj-surgeon.mcp-paths :as mcp-paths]
   [clj-surgeon.structural-lens :as structural-lens]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str])
  (:import
   (java.nio.file LinkOption Path Paths)
   (java.util UUID)
   (java.util.concurrent TimeUnit)))

(def max-bases 32)
(def basis-ttl-ms (* 60 60 1000))
(def max-sites 24)
(def max-visible-characters 12000)
(def max-snapshot-characters (* 4 1024 1024))
(defonce basis-store (atom {}))

(defn clear-bases!
  []
  (reset! basis-store {}))

(defn- now-ms [] (System/currentTimeMillis))

(defn- prune-bases!
  []
  (let [cutoff (- (now-ms) basis-ttl-ms)]
    (swap! basis-store
           (fn [current]
             (->> current
                  (filter (fn [[_ basis]] (> (:created-ms basis) cutoff)))
                  (sort-by (comp :created-ms val) >)
                  (take max-bases)
                  (into {}))))))

(defn- relative-file
  [^Path root file]
  (try
    (let [candidate (.toRealPath (Paths/get (str file) (make-array String 0))
                                 (make-array LinkOption 0))]
      (when (.startsWith candidate root)
        (str/replace (str (.relativize root candidate)) "\\" "/")))
    (catch Exception _ nil)))

(defn- canonical-file
  [file]
  (try
    (str (.toRealPath (Paths/get (str file) (make-array String 0))
                      (make-array LinkOption 0)))
    (catch Exception _ nil)))

(defn- semantic-locations
  [{:keys [definition references]}]
  (:locations
    (reduce
      (fn [{:keys [seen] :as result} location]
        (let [key [(:file_path location) (:line location) (:character location)]]
          (if (or (some nil? key) (contains? seen key))
            result
            (-> result
                (update :seen conj key)
                (update :locations conj location)))))
      {:seen #{} :locations []}
      (cons (assoc definition :role :definition)
            (map #(assoc % :role :reference) references)))))

(defn- capture-files
  [project-root locations read-source]
  (let [root (mcp-paths/real-root project-root)]
    (reduce
      (fn [captured {:keys [file_path]}]
        (let [relative (relative-file root file_path)]
          (if-not relative
            (reduced {:ok false
                      :error-type :semantic-path-outside-project
                      :error "cclsp returned a source path outside the configured project"
                      :path file_path
                      :source-unchanged true})
            (let [resolved (mcp-paths/resolve-source-path root relative)]
              (if-not (:ok resolved)
                (reduced resolved)
                (let [absolute (:path resolved)]
                  (if (contains? (:sources captured) absolute)
                    captured
                    (try
                      (let [source (read-source absolute)]
                        (-> captured
                            (assoc-in [:relative absolute] relative)
                            (assoc-in [:sources absolute] source)
                            (update :source-character-count + (count source))))
                      (catch Exception error
                        (reduced {:ok false
                                  :error-type :source-read-failed
                                  :error (.getMessage error)
                                  :file relative
                                  :source-unchanged true}))))))))))
      {:ok true :root root :sources {} :relative {} :source-character-count 0}
      locations)))

(defn- build-sites
  [locations {:keys [sources relative]}]
  (->> locations
       (keep
         (fn [{:keys [file_path line character role owner] :as location}]
           (let [canonical (canonical-file file_path)
                 absolute (some #(when (= canonical %) %) (keys sources))
                 owner-line (when (= :reference role) (:start_line owner))
                 selected (when absolute
                            (transaction/addressed-form-at
                              (get sources absolute)
                              {:line (or owner-line line)
                               :character (if owner-line 1 character)}))]
             (when selected
               (merge selected
                      {:file absolute
                       :relative-file (get relative absolute)
                       :role role
                       :owner (or (:name owner)
                                  (:name location))})))))
       (reduce (fn [dedup site]
                 (let [key [(:file site) (:path site)]]
                   (if (contains? dedup key)
                     dedup
                     (assoc dedup key site))))
               (array-map))
       vals
       (sort-by (juxt :relative-file :line (comp :preorder :address)))
       (map-indexed (fn [index site]
                      (assoc site :id (str "s" (inc index)))))
       vec))

(defn prepare-change!
  [{:keys [project-root semantic-resolver read-source]} {:keys [subject intent verify]}]
  (cond
    (not (and (string? subject) (str/includes? subject "/")))
    {:ok false :error-type :invalid-change-subject
     :error "prepare-change requires subject as namespace/name"
     :source-unchanged true}

    (not (and (string? intent) (seq intent)))
    {:ok false :error-type :invalid-change-intent
     :error "prepare-change requires a concise non-empty intent"
     :source-unchanged true}

    :else
    (let [semantic (semantic-resolver subject)]
      (if-not (:ok semantic)
        (assoc semantic :source-unchanged true)
        (let [locations (semantic-locations semantic)
              captured (capture-files project-root locations (or read-source slurp))]
          (if-not (:ok captured)
            captured
            (let [sites (build-sites locations captured)
                  visible-characters (reduce + 0 (map (comp count :before) sites))
                  over-budget? (or (> (count sites) max-sites)
                                   (> visible-characters max-visible-characters)
                                   (> (:source-character-count captured)
                                      max-snapshot-characters))]
              (cond
                (empty? sites)
                {:ok false
                 :error-type :semantic-sites-not-addressable
                 :error "No complete structural forms contained the cclsp locations"
                 :source-unchanged true}

                over-budget?
                {:ok false
                 :error-type :change-buffer-budget-exceeded
                 :error "The semantic surface exceeds the closed change-buffer budget"
                 :site-count (count sites)
                 :visible-characters visible-characters
                 :snapshot-characters (:source-character-count captured)
                 :limits {:sites max-sites
                          :visible-characters max-visible-characters
                          :snapshot-characters max-snapshot-characters}
                 :source-unchanged true
                 :remedy "Narrow the subject or use typed inspect requests for a manual decision."}

                :else
                (let [basis-id (str "cb-" (UUID/randomUUID))
                      public-sites
                      (mapv #(-> %
                                 (select-keys [:id :relative-file :role :owner :line
                                               :end-line :before])
                                 (set/rename-keys {:relative-file :file
                                                   :before :source}))
                            sites)
                      next-call
                      {:basis basis-id
                       :decisions (mapv (fn [{:keys [id]}]
                                          {:site id :replace nil})
                                        sites)
                       :verify (or verify "fast")}
                      basis {:id basis-id
                             :created-ms (now-ms)
                             :subject subject
                             :intent intent
                             :verify (or verify "fast")
                             :sites sites
                             :sources (:sources captured)
                             :source-hashes (update-vals (:sources captured)
                                                         structural-lens/source-hash)}]
                  (prune-bases!)
                  (swap! basis-store assoc basis-id basis)
                  {:ok true
                   :operation "inspect_clojure"
                   :mode "prepare-change"
                   :basis basis-id
                   :subject subject
                   :intent intent
                   :site-count (count sites)
                   :file-count (count (:sources captured))
                   :visible-character-count visible-characters
                   :sites public-sites
                   :decision-rule "Replace every null with either {\"keep\":true} or {\"replace\":\"ONE FORM\"}; then call apply_clojure_changes once."
                   :next_call next-call
                   :read_complete true
                   :source-unchanged true})))))))))

(defn- validate-decisions
  [basis decisions]
  (let [expected (set (map :id (:sites basis)))
        sites-by-id (into {} (map (juxt :id identity) (:sites basis)))
        actual (set (map :site decisions))
        duplicate? (not= (count decisions) (count actual))
        invalid (filterv (fn [decision]
                           (let [keep? (= true (:keep decision))
                                 replace? (and (string? (:replace decision))
                                               (seq (:replace decision)))]
                             (= keep? (boolean replace?))))
                         decisions)
        unchanged (filterv (fn [{:keys [site replace]}]
                             (and (string? replace)
                                  (= replace (:before (get sites-by-id site)))))
                           decisions)]
    (cond
      (or duplicate? (not= expected actual))
      {:ok false :error-type :basis-coverage-mismatch
       :error "Decisions must cover every prepared site exactly once"
       :expected-sites (vec (sort expected))
       :actual-sites (vec (sort actual))}

      (seq invalid)
      {:ok false :error-type :invalid-basis-decision
       :error "Each decision must contain keep true or one replacement form"
       :sites (mapv :site invalid)}

      (seq unchanged)
      {:ok false :error-type :unchanged-basis-decision
       :error "A replacement must differ from the prepared source"
       :sites (mapv :site unchanged)}

      :else {:ok true})))

(defn validate-basis-request
  "Refuse fields outside the closed basis route before any retained state is read."
  [request]
  (let [allowed-request-keys #{:basis :decisions :verify}
        allowed-decision-keys #{:site :keep :replace}
        request-fields (->> (keys request)
                            (remove allowed-request-keys)
                            (map name)
                            sort
                            vec)
        decision-fields
        (->> (:decisions request)
             (map-indexed
               (fn [index decision]
                 (when (map? decision)
                   (for [field (sort (remove allowed-decision-keys
                                             (keys decision)))]
                     (str "decisions[" index "]." (name field))))))
             (mapcat identity)
             vec)
        unknown-fields (into request-fields decision-fields)]
    (if (seq unknown-fields)
      {:ok false
       :error-type :invalid-mcp-request
       :error "Basis requests contain unknown or mixed-route fields"
       :unknown-fields unknown-fields
       :source-unchanged true
       :remedy "Copy the returned next_call and change only each decision's keep or replace value."}
      {:ok true})))

(defn- expand-command
  [command files]
  (let [expanded (vec (mapcat #(if (= "{files}" %) files [%]) command))
        executable (first expanded)
        search-paths ["/opt/homebrew/opt/node@20/bin"
                      "/opt/homebrew/bin"
                      "/usr/local/bin"
                      "/usr/bin"
                      "/bin"]
        resolved (when-not (str/includes? executable "/")
                   (some (fn [directory]
                           (let [candidate (io/file directory executable)]
                             (when (and (.isFile candidate) (.canExecute candidate))
                               (.getPath candidate))))
                         search-paths))]
    (assoc expanded 0 (or resolved executable))))

(defn- run-check!
  [project-root command]
  (let [started (System/nanoTime)
        output-file (java.io.File/createTempFile "clj-surgeon-verify-" ".log")
        builder (-> (ProcessBuilder. ^java.util.List command)
                    (.directory (io/file project-root))
                    (.redirectErrorStream true)
                    (.redirectOutput output-file))
        environment (.environment builder)
        _ (.put environment "PATH"
                (str "/opt/homebrew/opt/node@20/bin:/opt/homebrew/bin:"
                     "/usr/local/bin:/usr/bin:/bin:"
                     (get environment "PATH" "")))
        process (.start builder)
        finished? (.waitFor process 120 TimeUnit/SECONDS)
        _ (when-not finished? (.destroyForcibly process))
        raw-output (slurp output-file)
        output (subs raw-output 0 (min 12000 (count raw-output)))
        _ (.delete output-file)
        exit (when finished? (.exitValue process))
        ok (and finished? (zero? exit))]
    (cond-> {:ok ok
             :command (first command)
             :exit exit
             :elapsed_ms (/ (double (- (System/nanoTime) started)) 1000000.0)}
      (not ok) (assoc :output output))))

(defn- default-verify!
  [project-root profile profiles files]
  (if-let [profile-spec (get profiles profile)]
    (let [commands (if (map? profile-spec)
                     (:commands profile-spec)
                     [profile-spec])
          checks (loop [remaining commands
                        completed []]
                   (if-let [command (first remaining)]
                     (let [check (run-check! project-root
                                             (expand-command command files))
                           completed (conj completed check)]
                       (if (:ok check)
                         (recur (next remaining) completed)
                         completed))
                     completed))]
      {:ok (and (= (count commands) (count checks))
                (every? :ok checks))
       :profile profile
       :checks checks
       :elapsed_ms (reduce + 0.0 (map :elapsed_ms checks))})
    {:ok false :profile profile
     :error-type :unknown-verification-profile
     :error (str "Unknown closed verification profile: " profile)}))

(defn- publish-receipt!
  [receipt-dir receipt]
  (let [directory (io/file receipt-dir)
        _ (.mkdirs directory)
        path (str (io/file directory (str (UUID/randomUUID) ".edn")))]
    (file-ops/atomic-write! path (pr-str receipt))
    path))

(defn apply-basis!
  [{:keys [project-root receipt-dir verification-profiles verify! read-source write-source!]}
   {:keys [basis decisions verify]}]
  (prune-bases!)
  (if-let [prepared (get @basis-store basis)]
    (let [validation (validate-decisions prepared decisions)]
      (if-not (:ok validation)
        (assoc validation :source-unchanged true)
        (let [decisions-by-site (into {} (map (juxt :site identity) decisions))
              edits (->> (:sites prepared)
                         (keep (fn [site]
                                 (when-let [replacement (:replace (get decisions-by-site (:id site)))]
                                   (-> site
                                       (assoc :after replacement)
                                       (dissoc :relative-file :role :owner)))))
                         vec)]
          (if (empty? edits)
            {:ok false :error-type :empty-basis-change
             :error "Every prepared site was kept; no transaction was applied"
             :source-unchanged true}
            (let [compiled (transaction/compile-addressed-transaction
                             (:sources prepared) edits)]
              (if-not (:ok compiled)
                (assoc compiled :source-unchanged true)
                (let [io-opts (cond-> {}
                                read-source (assoc :read-source read-source)
                                write-source! (assoc :write-source! write-source!))
                      committed (if (seq io-opts)
                                  (transaction/commit-compiled! compiled io-opts)
                                  (transaction/commit-compiled! compiled))]
                  (if-not (:ok committed)
                    committed
                    (let [receipt (transaction/build-receipt compiled)
                          profile (or verify (:verify prepared))
                          verification ((or verify! default-verify!)
                                        project-root profile verification-profiles
                                        (mapv :file (:files compiled)))]
                      (if-not (:ok verification)
                        (let [inverse (transaction/compile-inverse
                                        receipt (:future-sources compiled))
                              rollback (if (:ok inverse)
                                         (if (seq io-opts)
                                           (transaction/commit-compiled! inverse io-opts)
                                           (transaction/commit-compiled! inverse))
                                         inverse)]
                          {:ok false
                           :error-type :verification-failed
                           :error "Verification failed; the addressed transaction was rolled back"
                           :verification verification
                           :rolled-back (boolean (:ok rollback))
                           :rollback rollback})
                        (let [receipt-file (publish-receipt!
                                             (or receipt-dir
                                                 (str (io/file project-root
                                                               ".clj-surgeon-receipts")))
                                             receipt)]
                          (swap! basis-store dissoc basis)
                          (merge committed
                                 {:ok true
                                  :operation "apply-basis"
                                  :basis basis
                                  :match-count (:match-count compiled)
                                  :receipt-file receipt-file
                                  :receipt-hash (:receipt-hash receipt)
                                  :verification_complete true
                                  :read_back_hashes (get-in committed
                                                            [:verified :read-back-hashes])
                                  :next_action "none"
                                  :verification verification}))))))))))))
    {:ok false :error-type :unknown-or-expired-basis
     :error "The prepared change basis is unknown or expired"
     :source-unchanged true
     :remedy "Call inspect_clojure prepare-change again, then submit its returned next_call."}))
