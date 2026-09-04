(ns clj-surgeon.mcp-change-buffer
  "Proof-carrying semantic selection followed by one addressed transaction."
  (:require
   [clj-surgeon.diagnostic-delta :as diagnostic-delta]
   [clj-surgeon.file-ops :as file-ops]
   [clj-surgeon.intent-transaction :as transaction]
   [clj-surgeon.mcp-cold-verify :as cold-verify]
   [clj-surgeon.mcp-hot-verify :as hot-verify]
   [clj-surgeon.mcp-paths :as mcp-paths]
   [clj-surgeon.mcp-process :as process-env]
   [clj-surgeon.mcp-workspace :as workspace]
   [clj-surgeon.outline :as outline]
   [clj-surgeon.structural-lens :as structural-lens]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [rewrite-clj.zip :as z])
  (:import
   (java.nio.charset StandardCharsets)
   (java.nio.file LinkOption Path Paths)
   (java.security MessageDigest)
   (java.util UUID)))

(def max-bases 32)
(def basis-ttl-ms (* 60 60 1000))
(def max-sites 24)
(def max-visible-characters (* 32 1024))
(def exact-verification-visible-bytes 12000)

;; @spec MCP-OP-ALIAS-028
(def diagnostic-capture-bytes
  "How much of a DIAGNOSTIC answer the baseline reads.

  `exact-verification-visible-bytes` bounds human-readable process evidence,
  where a prefix is still evidence. A diagnostic answer is a DOCUMENT, and
  half of one parses no better than none of it: the E-CALLER cohort's
  `verify: \"fast\"` call scoped 100 sources, clj-kondo answered EDN far past
  12,000 bytes, the runner cut it mid-map, and a correct analyzer answering a
  correct document was typed `invalid-diagnostic-output` — deterministically,
  on every retry, which is what the arm did before dropping `verify`.

  Four megabytes is the same order as `max-snapshot-characters`, which is the
  other place this verb holds a whole document in memory."
  (* 4 1024 1024))
(def max-snapshot-characters (* 4 1024 1024))
(defonce basis-store (atom {}))

(defn clear-bases!
  []
  (reset! basis-store {}))

(defn discard-basis!
  "Forget one unpublished or abandoned basis. Never changes project source."
  [basis-id]
  (swap! basis-store dissoc basis-id)
  nil)

(defn retained-basis-count
  "Return the number of live retained bases. Intended for readiness and tests."
  []
  (count @basis-store))

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

(defn select-basis-buffers
  "Select immutable named-form views from one retained basis. Pure data in/out."
  [basis site-ids context]
  (let [all-sites (or (:surface-sites basis) (:sites basis))
        by-id (into {} (map (juxt :id identity) all-sites))
        invalid-ids? (not (and (vector? site-ids)
                               (seq site-ids)
                               (every? string? site-ids)
                               (= (count site-ids) (count (distinct site-ids)))))
        missing (when-not invalid-ids?
                  (filterv #(not (contains? by-id %)) site-ids))]
    (cond
      invalid-ids?
      {:ok false
       :error-type :invalid-buffer-selection
       :error "open must contain unique retained site IDs"
       :source-unchanged true}

      (not= "form" context)
      {:ok false
       :error-type :unsupported-buffer-context
       :error "The first structural-buffer version supports context=form"
       :context context
       :source-unchanged true
       :remedy "Retry with context=form."}

      (seq missing)
      {:ok false
       :error-type :unknown-buffer-site
       :error "One or more requested site IDs are not in the retained basis"
       :sites missing
       :source-unchanged true}

      :else
      (let [subjects (:subjects basis)
            buffers
            (mapv
              (fn [site-id]
                (let [site (get by-id site-id)]
                  {:id site-id
                   :role (:role site)
                   :file (:relative-file site)
                   :form (:owner site)
                   :line (:line site)
                   :end-line (:end-line site)
                   :subjects (filterv (:subjects site) subjects)
                   :authority (:owner-authority site)
                   :context "form"
                   :source (:before site)}))
              site-ids)]
        {:ok true
         :operation "inspect_clojure"
         :mode "basis-view"
         :basis (:id basis)
         :context "form"
         :buffer-count (count buffers)
         :source-character-count (reduce + 0 (map (comp count :source) buffers))
         :buffers buffers
         :read_complete true
         :source-unchanged true
         :next-action "decide-or-open-more"}))))

(defn open-basis-sites!
  "Open retained structural buffers without reading project files again."
  [project-root basis-id site-ids context]
  (prune-bases!)
  (if-let [basis (get @basis-store basis-id)]
    (if (not= (.toString (mcp-paths/real-root project-root))
              (.toString (mcp-paths/real-root (:workspace-root basis))))
      {:ok false
       :operation "inspect_clojure"
       :mode "basis-view"
       :error-type :basis-workspace-mismatch
       :error "The retained basis belongs to a different workspace"
       :basis-workspace-root (:workspace-root basis)
       :request-workspace-root (.toString (mcp-paths/real-root project-root))
       :source-unchanged true
       :remedy "Copy workspace_root from the prepared result and retry once."}
      ;; @spec MCP-OP-STUDY-054 — the internal KEBAB key does NOT travel into
      ;; a published receipt. `json-key` normalizes `-` to `_` on the way to
      ;; the wire, so a receipt carrying `:workspace-root` beside the
      ;; `:workspace_root` the inspect tool attaches published a JSON object
      ;; with the member `workspace_root` TWICE — one of the two values lost
      ;; to whichever rule the decoder happens to apply, on every ordinary
      ;; `basis-view` call. Found by the round-twelve collision gate, which
      ;; refused this receipt at the boundary; nothing reads this key.
      (select-basis-buffers basis site-ids context))
    {:ok false
     :operation "inspect_clojure"
     :mode "basis-view"
     :error-type :unknown-or-expired-basis
     :error "The prepared change basis is unknown or expired"
     :source-unchanged true
     :remedy "Call inspect_clojure prepare-change again."}))

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

(defn validate-semantic-evidence
  [{:keys [version lsp_session definition references]}]
  (let [locations (cons (assoc definition :role :definition)
                        (map #(assoc % :role :reference) references))
        complete-range? (fn [range]
                          (every? #(and (integer? %)
                                        (not (neg? %)))
                                  [(get-in range [:start :line])
                                   (get-in range [:start :character])
                                   (get-in range [:end :line])
                                   (get-in range [:end :character])]))
        owner-status (fn [location]
                       (some-> (:owner_status location) name))
        named-owner? (fn [location]
                       (and (string? (:owner location))
                            (seq (:owner location))))
        complete-owner? (fn [location]
                          (cond
                            (= 2 version)
                            (named-owner? location)

                            (= :definition (:role location))
                            (and (= "found" (owner-status location))
                                 (named-owner? location))

                            :else
                            (or (and (= "found" (owner-status location))
                                     (named-owner? location))
                                (and (= "unresolved" (owner-status location))
                                     (nil? (:owner location))))))
        complete-location? (fn [location]
                             (and (string? (:lsp_session location))
                                  (seq (:lsp_session location))
                                  (string? (:file location))
                                  (seq (:file location))
                                  (string? (:file_path location))
                                  (string? (:source_sha256 location))
                                  (re-matches #"[0-9a-f]{64}" (:source_sha256 location))
                                  (complete-owner? location)
                                  (complete-range? (:range location))))
        sessions (set (keep :lsp_session locations))]
    (cond
      (not (contains? #{2 3} version))
      {:ok false
       :error-type :semantic-evidence-incomplete
       :error "cclsp semantic evidence must use version 2 or 3"
       :source-unchanged true}

      (not (and (string? lsp_session) (seq lsp_session)))
      {:ok false
       :error-type :semantic-evidence-incomplete
       :error "cclsp semantic evidence is missing lsp_session"
       :source-unchanged true}

      (not= #{lsp_session} sessions)
      {:ok false
       :error-type :semantic-session-drift
       :error "cclsp semantic locations do not share one LSP session"
       :lsp-session lsp_session
       :location-sessions sessions
       :source-unchanged true}

      (not-every? complete-location? locations)
      {:ok false
       :error-type :semantic-evidence-incomplete
       :error "Every cclsp location must include session, file, path, hash, owner status, and range"
       :lsp-session lsp_session
       :source-unchanged true}

      :else
      {:ok true :lsp-session lsp_session :locations locations})))

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

(defn- requested-subjects
  [{:keys [subject subjects]}]
  (cond
    (and (string? subject) (nil? subjects)) [subject]
    (and (nil? subject) (vector? subjects) (seq subjects)) subjects
    :else nil))

(defn- resolve-semantic-bundle
  [semantic-resolver structural-reference-resolver subjects]
  (let [resolved
        (reduce
          (fn [results subject]
            (let [semantic (semantic-resolver subject)]
              (if-not (:ok semantic)
                (reduced (assoc semantic :source-unchanged true :subject subject))
                (let [evidence (validate-semantic-evidence semantic)]
                  (if-not (:ok evidence)
                    (reduced (assoc evidence :subject subject))
                    (conj results {:subject subject
                                   :semantic semantic
                                   :evidence evidence}))))))
          []
          subjects)]
    (if (map? resolved)
      resolved
      (let [sessions (set (map (comp :lsp-session :evidence) resolved))]
        (if-not (= 1 (count sessions))
          {:ok false
           :error-type :semantic-session-drift
           :error "All requested Vars must resolve in one LSP session"
           :lsp-sessions sessions
           :source-unchanged true}
          (let [lsp-session (first sessions)
                supplemental
                (if structural-reference-resolver
                  (structural-reference-resolver subjects)
                  {:ok true :locations [] :reference-count 0})]
            (if-not (:ok supplemental)
              (assoc supplemental :source-unchanged true)
              {:ok true
               :lsp-session lsp-session
               :quoted-var-proof
               (select-keys supplemental
                            [:scanned-file-count
                             :candidate-file-count
                             :reference-count])
               :resolved
               (mapv
                 (fn [{:keys [subject] :as result}]
                   (update-in result [:semantic :references]
                              into
                              (for [location (:locations supplemental)
                                    :when (= subject (:subject location))]
                                (-> location
                                    (assoc :lsp_session lsp-session
                                           :owner_status "unresolved"
                                           :owner nil)
                                    (dissoc :source :subject :role)))))
                 resolved)})))))))

(defn- capture-files
  [project-root locations read-source]
  (let [root (mcp-paths/real-root project-root)]
    (reduce
      (fn [captured {:keys [file file_path source_sha256]}]
        (let [relative (relative-file root file_path)]
          (if-not relative
            (reduced {:ok false
                      :error-type :semantic-path-outside-project
                      :error "cclsp returned a source path outside the configured project"
                      :path file_path
                      :source-unchanged true})
            (if-not (= relative file)
              (reduced {:ok false
                        :error-type :semantic-evidence-incomplete
                        :error "cclsp relative and absolute source paths disagree"
                        :file file
                        :actual-file relative
                        :source-unchanged true})
              (let [resolved (mcp-paths/resolve-source-path root relative)]
                (if-not (:ok resolved)
                  (reduced resolved)
                  (let [absolute (:path resolved)]
                    (if (contains? (:sources captured) absolute)
                      (if (= source_sha256 (get-in captured [:provider-hashes absolute]))
                        captured
                        (reduced {:ok false
                                  :error-type :semantic-evidence-incomplete
                                  :error "cclsp returned conflicting hashes for one file"
                                  :file relative
                                  :source-unchanged true}))
                      (try
                        (let [source (read-source absolute)
                              actual-hash (structural-lens/source-hash source)]
                          (if-not (= source_sha256 actual-hash)
                            (reduced {:ok false
                                      :error-type :semantic-source-drift
                                      :error "cclsp semantic evidence does not describe the captured source"
                                      :file relative
                                      :provider-hash source_sha256
                                      :actual-hash actual-hash
                                      :source-unchanged true
                                      :remedy "Retry inspect_clojure prepare-change after the language server catches up."})
                            (-> captured
                                (assoc-in [:relative absolute] relative)
                                (assoc-in [:sources absolute] source)
                                (assoc-in [:provider-hashes absolute] source_sha256)
                                (update :source-character-count + (count source)))))
                        (catch Exception error
                          (reduced {:ok false
                                    :error-type :source-read-failed
                                    :error (.getMessage error)
                                    :file relative
                                    :source-unchanged true})))))))))))
      {:ok true :root root :sources {} :relative {} :provider-hashes {}
       :source-character-count 0}
      locations)))

(defn- structural-owner-at
  [file source location]
  (let [target-line (or (:line location)
                        (some-> (get-in location [:range :start :line]) inc))
        owners (->> (outline/top-level-form-records file source)
                    (filter (fn [{owner-line :line
                                  owner-end-line :end-line
                                  :keys [name]}]
                              (and name target-line owner-line owner-end-line
                                   (<= owner-line target-line owner-end-line)))))]
    (when (= 1 (count owners))
      (let [owner (first owners)
            selected (transaction/addressed-form-at
                       source {:line (:line owner) :character 1})]
        (when selected
          {:name (str (:name owner))
           :selected selected})))))

(defn- build-sites
  [locations {:keys [sources relative]} label]
  (let [built
        (reduce
          (fn [sites {:keys [file_path role owner subject] :as location}]
            (let [canonical (canonical-file file_path)
                  absolute (or (when (contains? sources file_path) file_path)
                               (some #(when (= canonical %) %) (keys sources)))
                  source (get sources absolute)
                  structural (when source
                               (structural-owner-at
                                 (get relative absolute) source location))
                  structural-owner (:name structural)]
              (cond
                (nil? structural)
                (reduced
                  {:ok false
                   :error-type :semantic-owner-not-found
                   :error "No named structural form contains the semantic location"
                   :file (get relative absolute)
                   :subject subject
                   :role role
                   :range (:range location)
                   :provider-form owner
                   :source-unchanged true})

                (and owner (not= owner structural-owner))
                (reduced
                  {:ok false
                   :error-type :semantic-owner-drift
                   :error "The language-server form disagrees with the exact-source form"
                   :file (get relative absolute)
                   :subject subject
                   :role role
                   :range (:range location)
                   :provider-form owner
                   :actual-form structural-owner
                   :source-unchanged true})

                :else
                (conj sites
                      (merge (:selected structural)
                             {:file absolute
                              :relative-file (get relative absolute)
                              :role role
                              :owner structural-owner
                              :owner-authority
                              (or (:reference-authority location)
                                  (if owner
                                    :language-server+exact-source
                                    :exact-source))
                              :subjects #{subject}})))))
          []
          locations)]
    (if (map? built)
      built
      {:ok true
       :sites
       (->> built
            (reduce (fn [dedup site]
                      (let [key [(:file site) (:path site)]]
                        (if (contains? dedup key)
                          (update-in dedup [key :subjects] set/union (:subjects site))
                          (assoc dedup key site))))
                    (array-map))
            vals
            (sort-by (juxt #(if (= :definition (:role %)) 0 1)
                           :relative-file
                           :line
                           (comp :preorder :address)))
            (map-indexed (fn [index site]
                           (assoc site :id
                                  (str label "/s" (format "%02d" (inc index))))))
            vec)})))

(defn- prepare-exact-owner!
  [{:keys [project-root read-source]}
   {:keys [file form intent verify scope label subject subjects]}]
  (let [label (or label "change")
        scope (or scope "definition")]
    (cond
      (or subject subjects)
      {:ok false
       :error-type :ambiguous-change-subject
       :error "Use either subject/subjects or file/form, not both"
       :source-unchanged true}

      (not (and (string? file) (seq file)
                (string? form) (seq form)))
      {:ok false
       :error-type :invalid-exact-owner
       :error "Exact-source preparation requires one file and one form"
       :source-unchanged true}

      (not (and (string? intent) (seq intent)))
      {:ok false
       :error-type :invalid-change-intent
       :error "prepare-change requires a concise non-empty intent"
       :source-unchanged true}

      (not= "definition" scope)
      {:ok false
       :error-type :exact-owner-scope-unsupported
       :error "Exact-source preparation supports only definition scope because it does not claim a reference surface"
       :scope scope
       :source-unchanged true}

      (not (and (string? label)
                (re-matches #"[a-z][a-z0-9-]{0,39}" label)))
      {:ok false
       :error-type :invalid-change-label
       :error "prepare-change label must start with a lowercase letter and contain only lowercase letters, digits, or hyphens"
       :label label
       :source-unchanged true}

      :else
      (let [root (mcp-paths/real-root project-root)
            resolved (mcp-paths/resolve-source-path root file)]
        (if-not (:ok resolved)
          (-> resolved
              (set/rename-keys {:error_type :error-type
                                :source_unchanged :source-unchanged})
              (update :error-type keyword))
          (try
            (let [absolute (:path resolved)
                  source ((or read-source slurp) absolute)
                  candidates
                  (->> (outline/top-level-form-records file source)
                       (filter #(= form (some-> (:name %) str)))
                       vec)]
              (cond
                (empty? candidates)
                {:ok false
                 :error-type :exact-owner-not-found
                 :error "No exact named form exists in the requested file"
                 :file file
                 :form form
                 :source-unchanged true}

                (> (count candidates) 1)
                {:ok false
                 :error-type :exact-owner-ambiguous
                 :error "More than one exact named form exists in the requested file"
                 :file file
                 :form form
                 :candidates (mapv #(select-keys % [:line :end-line :type])
                                   candidates)
                 :source-unchanged true}

                :else
                (let [record (first candidates)
                      selected (transaction/addressed-form-at
                                 source {:line (:line record) :character 1})]
                  (if-not selected
                    {:ok false
                     :error-type :exact-owner-not-addressable
                     :error "The exact named form has no stable structural address"
                     :file file
                     :form form
                     :source-unchanged true}
                    (let [site-id (str label "/s01")
                          site (merge selected
                                      {:id site-id
                                       :file absolute
                                       :relative-file file
                                       :role :definition
                                       :owner form
                                       :owner-authority :exact-source
                                       :subjects #{}})
                          visible-characters (count (:before site))
                          snapshot-characters (count source)]
                      (if (or (> visible-characters max-visible-characters)
                              (> snapshot-characters max-snapshot-characters))
                        {:ok false
                         :error-type :change-buffer-budget-exceeded
                         :error "The exact owner exceeds the closed change-buffer budget"
                         :site-count 1
                         :visible-characters visible-characters
                         :snapshot-characters snapshot-characters
                         :limits {:sites max-sites
                                  :visible-characters max-visible-characters
                                  :snapshot-characters max-snapshot-characters}
                         :source-unchanged true
                         :remedy "Use typed inspect requests for a smaller decision."}
                        (let [basis-id (str "cb-" (UUID/randomUUID))
                              compact-site {:id site-id
                                            :file file
                                            :role :definition
                                            :form form
                                            :line (:line site)
                                            :end-line (:end-line site)
                                            :authority :exact-source
                                            :subjects []}
                              decision-site (assoc compact-site
                                                   :authority :exact-source
                                                   :source (:before site))
                              next-call {:workspace_root (str root)
                                         :basis basis-id
                                         :decisions [{:site site-id :replace nil}]
                                         :verify (or verify "fast")}
                              basis {:id basis-id
                                     :created-ms (now-ms)
                                     :workspace-root (str root)
                                     :lsp-session nil
                                     :subjects []
                                     :intent intent
                                     :label label
                                     :scope "definition"
                                     :verify (or verify "fast")
                                     :surface-sites [site]
                                     :sites [site]
                                     :sources {absolute source}
                                     :source-hashes
                                     {absolute (structural-lens/source-hash source)}}]
                          (prune-bases!)
                          (swap! basis-store assoc basis-id basis)
                          {:ok true
                           :operation "inspect_clojure"
                           :mode "prepare-change"
                           :basis basis-id
                           :authority :exact-source
                           :file file
                           :form form
                           :intent intent
                           :label label
                           :scope "definition"
                           :reference-count 0
                           :surface-location-count 1
                           :surface-site-count 1
                           :site-count 1
                           :file-count 1
                           :visible-character-count visible-characters
                           :surface [compact-site]
                           :decision-site-ids [site-id]
                           :decision-sites [decision-site]
                           :decision-rule "Set the decision to keep, one complete named-form replacement, whole-site delete, or one compact edit; then call apply_clojure_changes once."
                           :next_call next-call
                           :read_complete true
                           :source-unchanged true})))))))
            (catch Exception error
              {:ok false
               :error-type :source-read-failed
               :error (.getMessage error)
               :file file
               :source-unchanged true})))))))

(defn- prepare-exact-owners!
  [{:keys [project-root read-source] :as config}
   {:keys [owners intent verify scope label subject subjects file form] :as request}]
  (let [label (or label "change")
        scope (or scope "definition")
        valid-owner? (fn [owner]
                       (and (map? owner)
                            (string? (:file owner)) (seq (:file owner))
                            (string? (:form owner)) (seq (:form owner))))]
    (cond
      (or subject subjects file form)
      {:ok false
       :error-type :ambiguous-change-subject
       :error "Use owners by itself instead of subject/subjects or file/form"
       :source-unchanged true}

      (not (and (vector? owners)
                (seq owners)
                (every? valid-owner? owners)
                (= (count owners)
                   (count (distinct (map (juxt :file :form) owners))))))
      {:ok false
       :error-type :invalid-exact-owners
       :error "Exact-source batch preparation requires a unique non-empty owners array of file/form objects"
       :source-unchanged true}

      (not (and (string? intent) (seq intent)))
      {:ok false
       :error-type :invalid-change-intent
       :error "prepare-change requires a concise non-empty intent"
       :source-unchanged true}

      (not= "definition" scope)
      {:ok false
       :error-type :exact-owner-scope-unsupported
       :error "Exact-source preparation supports only definition scope because it does not claim a reference surface"
       :scope scope
       :source-unchanged true}

      (not (and (string? label)
                (re-matches #"[a-z][a-z0-9-]{0,39}" label)))
      {:ok false
       :error-type :invalid-change-label
       :error "prepare-change label must start with a lowercase letter and contain only lowercase letters, digits, or hyphens"
       :label label
       :source-unchanged true}

      :else
      (let [source-cache (atom {})
            read-once (fn [path]
                        (if-let [cached (get @source-cache path)]
                          cached
                          (let [source ((or read-source slurp) path)]
                            (swap! source-cache assoc path source)
                            source)))
            provisional
            (loop [remaining owners
                   results []]
              (if-let [{:keys [file form]} (first remaining)]
                (let [result
                      (prepare-exact-owner!
                        (assoc config :read-source read-once)
                        (-> request
                            (dissoc :owners)
                            (assoc :file file :form form)))]
                  (if (:ok result)
                    (recur (next remaining) (conj results result))
                    {:failure result :results results}))
                {:results results}))
            provisional-results (:results provisional)
            provisional-basis-ids (mapv :basis provisional-results)
            discard-provisional! #(swap! basis-store
                                         (fn [store]
                                           (apply dissoc store provisional-basis-ids)))]
        (if-let [failure (:failure provisional)]
          (do
            (discard-provisional!)
            failure)
          (let [provisional-bases (mapv #(get @basis-store %) provisional-basis-ids)
                reindex (fn [index site]
                          (assoc site :id (format "%s/s%02d" label (inc index))))
                sites (mapv reindex (range)
                            (mapcat :sites provisional-bases))
                sources (apply merge (map :sources provisional-bases))
                visible-characters (reduce + 0 (map (comp count :before) sites))
                snapshot-characters (reduce + 0 (map count (vals sources)))
                over-budget? (or (> (count sites) max-sites)
                                 (> visible-characters max-visible-characters)
                                 (> snapshot-characters max-snapshot-characters))]
            (discard-provisional!)
            (if over-budget?
              {:ok false
               :error-type :change-buffer-budget-exceeded
               :error "The exact owner batch exceeds the closed change-buffer budget"
               :site-count (count sites)
               :visible-characters visible-characters
               :snapshot-characters snapshot-characters
               :limits {:sites max-sites
                        :visible-characters max-visible-characters
                        :snapshot-characters max-snapshot-characters}
               :source-unchanged true
               :remedy "Split the architectural decision into a smaller exact owner batch."}
              (let [root (mcp-paths/real-root project-root)
                    basis-id (str "cb-" (UUID/randomUUID))
                    compact-site (fn [site]
                                   {:id (:id site)
                                    :file (:relative-file site)
                                    :role :definition
                                    :form (:owner site)
                                    :line (:line site)
                                    :end-line (:end-line site)
                                    :authority :exact-source
                                    :subjects []})
                    public-surface (mapv compact-site sites)
                    public-decisions
                    (mapv #(assoc (compact-site %) :source (:before %)) sites)
                    decision-site-ids (mapv :id sites)
                    next-call {:workspace_root (str root)
                               :basis basis-id
                               :decisions (mapv (fn [id]
                                                  {:site id :replace nil})
                                                decision-site-ids)
                               :verify (or verify "fast")}
                    basis {:id basis-id
                           :created-ms (now-ms)
                           :workspace-root (str root)
                           :lsp-session nil
                           :subjects []
                           :intent intent
                           :label label
                           :scope "definition"
                           :verify (or verify "fast")
                           :surface-sites sites
                           :sites sites
                           :sources sources
                           :source-hashes
                           (update-vals sources structural-lens/source-hash)}]
                (prune-bases!)
                (swap! basis-store assoc basis-id basis)
                {:ok true
                 :operation "inspect_clojure"
                 :mode "prepare-change"
                 :basis basis-id
                 :authority :exact-source
                 :owners owners
                 :intent intent
                 :label label
                 :scope "definition"
                 :reference-count 0
                 :surface-location-count (count sites)
                 :surface-site-count (count sites)
                 :site-count (count sites)
                 :file-count (count sources)
                 :visible-character-count visible-characters
                 :surface public-surface
                 :decision-site-ids decision-site-ids
                 :decision-sites public-decisions
                 :decision-rule "Set every decision to keep, one complete named-form replacement, whole-site delete, or one compact edit; then call apply_clojure_changes once."
                 :next_call next-call
                 :read_complete true
                 :source-unchanged true}))))))))

(defn compile-prepared-basis
  "Purely compile one retained semantic basis and its bounded public response
  from captured source and semantic data. Basis identity and time are inputs."
  [{:keys [project-root bundle subjects intent verify scope label
           surface-locations reference-count captured basis-id created-ms
           limits]
    :or {verify "fast"
         scope "surface"
         label "change"
         limits {:sites max-sites
                 :visible-characters max-visible-characters
                 :snapshot-characters max-snapshot-characters}}}]
  (let [built (build-sites surface-locations captured label)]
    (if-not (:ok built)
      built
      (let [surface-sites (:sites built)
            sites (if (= "definition" scope)
                    (filterv #(= :definition (:role %)) surface-sites)
                    surface-sites)
            visible-characters (reduce + 0 (map (comp count :before) sites))
            over-budget? (or (> (count sites) (:sites limits))
                             (> visible-characters (:visible-characters limits))
                             (> (:source-character-count captured)
                                (:snapshot-characters limits)))]
        (cond
          (empty? sites)
          {:ok false
           :error-type :semantic-sites-not-addressable
           :error "No complete named forms contained the requested semantic locations"
           :source-unchanged true}

          over-budget?
          {:ok false
           :error-type :change-buffer-budget-exceeded
           :error "The decision surface exceeds the closed change-buffer budget"
           :site-count (count sites)
           :surface-site-count (count surface-sites)
           :visible-characters visible-characters
           :snapshot-characters (:source-character-count captured)
           :limits limits
           :source-unchanged true
           :remedy "Use scope=definition, narrow the subjects, or use typed inspect requests for a smaller decision."}

          :else
          (let [compact-site
                (fn [site]
                  (-> site
                      (select-keys [:id :relative-file :role :owner
                                    :line :end-line :subjects
                                    :owner-authority])
                      (update :subjects
                              (fn [site-subjects]
                                (filterv site-subjects subjects)))
                      (set/rename-keys {:relative-file :file
                                        :owner :form
                                        :owner-authority :authority})))
                decision-site
                (fn [site]
                  (-> (compact-site site)
                      (assoc :authority (:owner-authority site)
                             :source (:before site))))
                public-surface (mapv compact-site surface-sites)
                public-decisions (mapv decision-site sites)
                decision-site-ids (mapv :id sites)
                next-call
                {:workspace_root project-root
                 :basis basis-id
                 :decisions (mapv (fn [id]
                                    {:site id :replace nil})
                                  decision-site-ids)
                 :verify verify}
                basis {:id basis-id
                       :created-ms created-ms
                       :workspace-root project-root
                       :lsp-session (:lsp-session bundle)
                       :subjects subjects
                       :intent intent
                       :label label
                       :scope scope
                       :verify verify
                       :surface-sites surface-sites
                       :sites sites
                       :sources (:sources captured)
                       :source-hashes
                       (update-vals (:sources captured)
                                    structural-lens/source-hash)}]
            {:ok true
             :basis basis
             :response
             {:ok true
              :operation "inspect_clojure"
              :mode "prepare-change"
              :basis basis-id
              :lsp_session (:lsp-session bundle)
              :subject (when (= 1 (count subjects)) (first subjects))
              :subjects subjects
              :intent intent
              :label label
              :scope scope
              :reference-count reference-count
              :quoted_var_proof (:quoted-var-proof bundle)
              :surface-location-count (count surface-locations)
              :surface-site-count (count surface-sites)
              :site-count (count sites)
              :file-count (count (:sources captured))
              :visible-character-count visible-characters
              :surface public-surface
              :decision-site-ids decision-site-ids
              :decision-sites public-decisions
              :decision-rule "Set every decision to keep, one complete named-form replacement, whole-site delete, or one compact edit containing find and replace/delete; then call apply_clojure_changes once."
              :next_call next-call
              :read_complete true
              :source-unchanged true}}))))))

(defn prepare-change!
  [{:keys [project-root semantic-resolver read-source] :as config}
   {:keys [intent verify scope label] :as request}]
  (if (or (contains? request :owners)
          (contains? request :file)
          (contains? request :form))
    (if (contains? request :owners)
      (prepare-exact-owners! config request)
      (prepare-exact-owner! config request))
    (let [subjects (requested-subjects request)
          scope (or scope "surface")
          label (or label "change")]
      (cond
        (not (and (vector? subjects)
                  (every? #(and (string? %) (str/includes? % "/")) subjects)
                  (= (count subjects) (count (distinct subjects)))))
        {:ok false
         :error-type :invalid-change-subject
         :error "prepare-change requires one subject or a unique non-empty subjects array of namespace/name Vars"
         :source-unchanged true}

        (not (and (string? intent) (seq intent)))
        {:ok false
         :error-type :invalid-change-intent
         :error "prepare-change requires a concise non-empty intent"
         :source-unchanged true}

        (not (#{"definition" "surface"} scope))
        {:ok false
         :error-type :invalid-change-scope
         :error "prepare-change scope must be definition or surface"
         :scope scope
         :source-unchanged true}

        (not (and (string? label)
                  (re-matches #"[a-z][a-z0-9-]{0,39}" label)))
        {:ok false
         :error-type :invalid-change-label
         :error "prepare-change label must start with a lowercase letter and contain only lowercase letters, digits, or hyphens"
         :label label
         :source-unchanged true}

        :else
        (let [bundle (resolve-semantic-bundle
                       semantic-resolver
                       (:structural-reference-resolver config)
                       subjects)]
          (if-not (:ok bundle)
            bundle
            (let [surface-locations
                  (vec
                    (mapcat (fn [{:keys [subject semantic]}]
                              (map #(assoc % :subject subject)
                                   (semantic-locations semantic)))
                            (:resolved bundle)))
                  reference-count
                  (count (filter #(= :reference (:role %)) surface-locations))
                  captured
                  (capture-files project-root surface-locations
                                 (or read-source slurp))]
              (if-not (:ok captured)
                captured
                (let [basis-id (str "cb-" (UUID/randomUUID))
                      compiled
                      (compile-prepared-basis
                        {:project-root project-root
                         :bundle bundle
                         :subjects subjects
                         :intent intent
                         :verify (or verify "fast")
                         :scope scope
                         :label label
                         :surface-locations surface-locations
                         :reference-count reference-count
                         :captured captured
                         :basis-id basis-id
                         :created-ms (now-ms)})]
                  (if-not (:ok compiled)
                    compiled
                    (do
                      (prune-bases!)
                      (swap! basis-store assoc basis-id (:basis compiled))
                      (:response compiled))))))))))))

(defn- validate-decisions
  [basis decisions]
  (let [expected (set (map :id (:sites basis)))
        sites-by-id (into {} (map (juxt :id identity) (:sites basis)))
        actual (set (map :site decisions))
        duplicate? (not= (count decisions) (count actual))
        valid-edit?
        (fn [edit]
          (let [replace? (and (string? (:replace edit))
                              (seq (:replace edit)))
                delete? (= true (:delete edit))]
            (and (map? edit)
                 (every? #{:find :replace :delete} (keys edit))
                 (string? (:find edit))
                 (seq (:find edit))
                 (not= (boolean replace?) delete?))))
        invalid
        (filterv
          (fn [decision]
            (let [keep? (= true (:keep decision))
                  replace? (and (string? (:replace decision))
                                (seq (:replace decision)))
                  edit? (valid-edit? (:edit decision))
                  delete? (= true (:delete decision))]
              (not= 1 (count (filter true?
                                     [keep? (boolean replace?) edit? delete?])))))
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
       :error "Each decision must contain exactly one keep, owner replacement, whole-site delete, or compact edit"
       :sites (mapv :site invalid)}

      (seq unchanged)
      {:ok false :error-type :unchanged-basis-decision
       :error "A replacement must differ from the prepared source"
       :sites (mapv :site unchanged)}

      :else {:ok true})))

(defn- line-offsets
  [source]
  (loop [from 0
         offsets [0]]
    (let [newline (.indexOf ^String source "\n" (int from))]
      (if (neg? newline)
        offsets
        (recur (inc newline) (conj offsets (inc newline)))))))

(defn delete-subform-source
  [source find-source]
  (let [found (structural-lens/find-subforms source {:match find-source})]
    (cond
      (:error found)
      found

      (not= 1 (:match-count found))
      {:ok false
       :error-type (if (zero? (:match-count found)) :no-match :ambiguous-match)
       :error (str "Expected exactly one compact delete match, found "
                   (:match-count found))
       :match-count (:match-count found)}

      :else
      (let [{match-line :line match-source :source} (first (:matches found))
            root (z/of-string source {:track-position? true})
            target
            (loop [loc root]
              (cond
                (z/end? loc) nil
                (and (= match-line (:row (meta (z/node loc))))
                     (= match-source (z/string loc))) loc
                :else (recur (z/next loc))))]
        (cond
          (nil? target)
          {:ok false :error-type :basis-edit-address-drift
           :error "The compact delete match could not be re-addressed"}

          (nil? (z/up target))
          {:ok false :error-type :basis-edit-covers-owner
           :error "A compact delete cannot remove the retained owner itself"}

          :else
          (let [{:keys [row col end-row end-col]} (meta (z/node target))
                offsets (line-offsets source)
                lines (str/split source #"\n" -1)
                target-start (+ (nth offsets (dec row)) (dec col))
                target-end (+ (nth offsets (dec end-row)) (dec end-col))
                line-start (nth offsets (dec row))
                line-oriented? (str/blank? (subs source line-start target-start))
                first-comment-line
                (when line-oriented?
                  (loop [line-index (- row 2)
                         first-index nil]
                    (if (and (>= line-index 0)
                             (re-matches #"\s*;+.*" (nth lines line-index)))
                      (recur (dec line-index) line-index)
                      first-index)))
                raw-start
                (if line-oriented?
                  (nth offsets (or first-comment-line (dec row)))
                  (loop [at target-start]
                    (if (and (pos? at)
                             (contains? #{\space \tab}
                                        (.charAt ^String source (long (dec at)))))
                      (recur (dec at))
                      at)))
                delete-start
                (let [default-start
                      (if (and line-oriented?
                               (pos? raw-start)
                               (= \newline
                                  (.charAt ^String source
                                           (long (dec raw-start)))))
                        (dec raw-start)
                        raw-start)]
                  (if-let [previous
                           (and line-oriented?
                                (nil? (z/right target))
                                (z/left target))]
                    (let [{previous-end-row :end-row
                           previous-end-col :end-col} (meta (z/node previous))
                          previous-end (+ (nth offsets (dec previous-end-row))
                                          (dec previous-end-col))
                          interstitial (subs source previous-end target-start)]
                      (if (or first-comment-line
                              (not (str/includes? interstitial ";")))
                        previous-end
                        default-start))
                    default-start))
                updated
                (str (subs source 0 delete-start)
                     (let [line-end (or (str/index-of source "\n"
                                                      (long target-end))
                                        (count source))
                           trailing-source (subs source target-end line-end)]
                       (subs source
                             (if (and line-oriented?
                                      (re-matches #"\s*;+.*" trailing-source))
                               line-end
                               target-end))))]
            (try
              (when (str/blank? updated)
                (throw (ex-info "A compact delete cannot empty the retained owner"
                                {:error-type :basis-edit-covers-owner})))
              (z/of-string updated {:track-position? true})
              {:ok true :source updated}
              (catch Exception error
                {:ok false
                 :error-type (or (:error-type (ex-data error))
                                 :invalid-compact-delete-result)
                 :error (.getMessage error)}))))))))

(defn- materialize-compact-edit
  [site {:keys [find replace delete]}]
  (let [result
        (if delete
          (delete-subform-source (:before site) find)
          (let [plan (structural-lens/plan-replacement
                       (:before site)
                       {:match find :with replace :file (:file site)})]
            (if (:error plan)
              plan
              (structural-lens/apply-plan (:before site) plan))))]
    (cond
      (not (:ok result))
      (assoc result :site (:id site))

      (= (:before site) (:source result))
      {:ok false :error-type :unchanged-basis-decision
       :error "A compact edit must change its retained owner"
       :site (:id site)}

      :else
      {:ok true
       :decision {:site (:id site) :replace (:source result)}})))

(defn- materialize-decisions
  [basis decisions]
  (let [sites-by-id (into {} (map (juxt :id identity) (:sites basis)))]
    (loop [remaining decisions
           materialized []]
      (if-let [decision (first remaining)]
        (if-let [edit (:edit decision)]
          (let [result (materialize-compact-edit
                         (get sites-by-id (:site decision)) edit)]
            (if (:ok result)
              (recur (next remaining) (conj materialized (:decision result)))
              result))
          (recur (next remaining) (conj materialized decision)))
        {:ok true :decisions materialized}))))

(defn validate-basis-request
  "Refuse fields outside the closed basis route before any retained state is read."
  [request]
  (let [allowed-request-keys #{:basis :decisions :verify}
        allowed-decision-keys #{:site :keep :replace :delete :edit}
        allowed-edit-keys #{:find :replace :delete}
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
                   (concat
                     (for [field (sort (remove allowed-decision-keys
                                               (keys decision)))]
                       (str "decisions[" index "]." (name field)))
                     (when (map? (:edit decision))
                       (for [field (sort (remove allowed-edit-keys
                                                 (keys (:edit decision))))]
                         (str "decisions[" index "].edit." (name field))))))))
             (mapcat identity)
             vec)
        unknown-fields (into request-fields decision-fields)]
    (if (seq unknown-fields)
      {:ok false
       :error-type :invalid-mcp-request
       :error "Basis requests contain unknown or mixed-route fields"
       :unknown-fields unknown-fields
       :source-unchanged true
       :remedy "Copy next_call and set exactly one keep, replace, delete, or compact edit per site."}
      {:ok true})))

(defn expand-command
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

(defn- bytes->hex
  [bytes]
  (apply str (map #(format "%02x" (bit-and 0xff %)) bytes)))

(defn- sha256-text
  [text]
  (-> (doto (MessageDigest/getInstance "SHA-256")
        (.update (.getBytes ^String text StandardCharsets/UTF_8)))
      .digest
      bytes->hex))

(defn run-process!
  "Run one bounded command and return its evidence.

  `visible-byte-limit` bounds how much of the child's output this JVM READS
  back. It defaults to the receipt's publication budget because most callers
  publish what they read; a caller that PARSES the output rather than
  publishing it must pass its own ceiling, because a cap sized for a receipt
  is a cap on the truth a detector gets to see. See MCP-OP-ADMIT-122."
  ([project-root command]
   (run-process! project-root command 120000))
  ([project-root command timeout-ms]
   (run-process! project-root command timeout-ms
                 exact-verification-visible-bytes))
  ([project-root command timeout-ms visible-byte-limit]
   (let [started (System/nanoTime)]
     (try
       (process-env/run-bounded!
         {:command command
          :cwd project-root
          :timeout-ms timeout-ms
          :merge-error? true
          :visible-byte-limit visible-byte-limit})
       (catch Exception error
         {:finished? false
          :launch-error true
          :exit nil
          :elapsed_ms (/ (double (- (System/nanoTime) started)) 1000000.0)
          :output (or (.getMessage error) (.getName (class error)))
          :output-bytes 0
          :output-sha256 (sha256-text "")
          :output-truncated false
          :admission-error (ex-data error)})))))

(defn- admission-unverified?
  [{:keys [admission admission-error]}]
  (let [status (or (:status admission)
                   (get-in admission-error [:admission :status]))
        error-type (or (:error-type admission)
                       (:error-type admission-error))]
    (or (#{:delegated :admission-timeout :pressure-deferred} status)
        (#{:clj-kondo-admission-unavailable
           :clj-kondo-executable-unavailable
           :clj-kondo-admission-timeout
           :clj-kondo-pressure-deferred
           :process-interrupted}
         error-type))))

(defn compile-exact-profile
  "Compile one project-owned exact profile into an immutable execution value."
  [profile profiles profile-source]
  ;; @spec MCP-OP-VERIFY-001
  ;; @spec MCP-OP-VERIFY-002
  ;; @spec MCP-OP-VERIFY-003
  ;; @spec MCP-OP-VERIFY-004
  ;; @spec MCP-OP-VERIFY-005
  (let [definition (get profiles profile)
        command (first (:commands definition))
        valid-command? (and (vector? command)
                            (seq command)
                            (every? #(and (string? %) (not (str/blank? %))) command))]
    (cond
      (not= "exact" profile)
      {:ok false :error-type :unknown-verification-profile
       :source-unchanged true}

      (not= :project profile-source)
      {:ok false :error-type :exact-profile-not-project-owned
       :source-unchanged true}

      (not (and (map? definition)
                (= #{:acceptance :timeout-ms :commands}
                   (set (keys definition)))
                (= :exact-exit (:acceptance definition))
                (integer? (:timeout-ms definition))
                (<= 1 (:timeout-ms definition) 120000)
                (= 1 (count (:commands definition)))
                valid-command?
                (not-any? #{"{files}"} command)))
      {:ok false :error-type :invalid-exact-verification-profile
       :source-unchanged true}

      :else
      {:ok true
       :profile profile
       :profile-source profile-source
       :profile-sha256 (sha256-text (pr-str (into (sorted-map) definition)))
       :acceptance :exact-exit
       :timeout-ms (:timeout-ms definition)
       :argv (expand-command command [])})))

(defn classify-exact-process-outcome
  [{:keys [finished? launch-error exit admission] :as process}]
  (cond
    (admission-unverified? process)
    {:process-outcome (if (= :admission-timeout (:status admission))
                        :admission-timeout
                        :admission-unavailable)}
    launch-error {:process-outcome :launch-failure}
    (not finished?) {:process-outcome :timeout}
    (zero? exit) {:process-outcome :pass}
    (>= exit 128) {:process-outcome :crash-or-signal-style-exit}
    :else {:process-outcome :ordinary-nonzero}))

(defn run-exact-verification!
  "Execute one compiled exact profile and return terminal bounded evidence."
  [project-root compiled-profile]
  ;; @spec MCP-OP-VERIFY-003
  ;; @spec MCP-OP-VERIFY-005
  ;; @spec MCP-OP-VERIFY-006
  ;; @spec MCP-OP-VERIFY-007
  ;; @spec MCP-OP-VERIFY-009
  ;; @spec MCP-OP-VERIFY-010
  ;; @spec MCP-OP-ANALYZER-004
  (let [cwd (.getCanonicalPath (io/file project-root))
        process (run-process! cwd (:argv compiled-profile)
                              (:timeout-ms compiled-profile))
        outcome (:process-outcome (classify-exact-process-outcome process))
        evidence (merge
                   (select-keys compiled-profile
                                [:profile :profile-source :profile-sha256
                                 :acceptance :timeout-ms :argv])
                   {:cwd cwd
                    :process-outcome outcome
                    :exit (:exit process)
                    :elapsed_ms (:elapsed_ms process)
                    :output-bytes (:output-bytes process)
                    :output-sha256 (:output-sha256 process)
                    :output-truncated (:output-truncated process)}
                   (select-keys process [:admission :admission-error]))]
    (case outcome
      :pass (assoc evidence :ok true)
      :ordinary-nonzero
      (assoc evidence :ok false
             :error-type :verification-failed
             :diagnostics (:output process))
      (assoc evidence :ok false
             :error-type :verification-unverified
             :diagnostics (:output process)))))

(defn- run-check!
  [project-root command]
  (let [{:keys [finished? exit elapsed_ms output] :as process}
        (run-process! project-root command)]
    (if (admission-unverified? process)
      {:ok false
       :command (first command)
       :exit exit
       :elapsed_ms elapsed_ms
       :error-type :verification-unverified
       :output output
       :admission (:admission process)
       :admission-error (:admission-error process)}
      (let [ok (and finished? (zero? exit))]
        (cond-> {:ok ok
                 :command (first command)
                 :exit exit
                 :elapsed_ms elapsed_ms}
          (not ok) (assoc :output output))))))

(def diagnostic-output-config
  "{:output {:format :edn}}")

(defn- diagnostic-command?
  [command]
  (= "clj-kondo" (.getName (io/file (first command)))))

(defn- diagnostic-command
  [command files]
  (into (expand-command command files)
        ["--cache" "false" "--config" diagnostic-output-config]))

(defn- run-diagnostic-check!
  [project-root command files]
  ;; @spec MCP-OP-ALIAS-028
  ;; read the answer WHOLE: a diagnostic answer is a document, and a document
  ;; cut at the human-readable evidence budget cannot parse
  (let [{:keys [finished? exit elapsed_ms output output-bytes output-truncated]
         :as process}
        (run-process! project-root (diagnostic-command command files)
                      120000 diagnostic-capture-bytes)]
    (cond
      (admission-unverified? process)
      {:ok false
       :command (first command)
       :exit exit
       :elapsed_ms elapsed_ms
       :error-type :verification-unverified
       :output output
       :admission (:admission process)
       :admission-error (:admission-error process)}

      ;; @spec MCP-OP-ALIAS-028
      ;; an answer past even THIS budget is a truncation and is named as one.
      ;; Reporting it as invalid output blames the analyzer for a bound this
      ;; verb imposed, and leaves the caller nothing to change.
      output-truncated
      {:ok false
       :command (first command)
       :exit exit
       :elapsed_ms elapsed_ms
       :error-type :diagnostic-output-truncated
       :output-bytes output-bytes
       :diagnostic_byte_budget diagnostic-capture-bytes
       :output output}

      :else
      (let [parsed (when finished?
                     (try
                       (edn/read-string output)
                       (catch Exception _ nil)))
            ok (and finished? (map? parsed) (vector? (:findings parsed)))]
        (cond-> {:ok ok
                 :command (first command)
                 :exit exit
                 :elapsed_ms elapsed_ms}
          ok (assoc :diagnostics parsed)
          (not ok) (assoc :output output
                          :error-type :invalid-diagnostic-output))))))

(defn capture-verification-baseline!
  "Capture cache-independent diagnostic snapshots before a transaction writes."
  [project-root profile profiles files]
  (if-let [profile-spec (get profiles profile)]
    (let [commands (if (map? profile-spec)
                     (or (:commands profile-spec) [])
                     [profile-spec])
          checks (->> commands
                      (filter diagnostic-command?)
                      (mapv #(run-diagnostic-check! project-root % files)))]
      {:ok (every? :ok checks)
       :profile profile
       :checks checks
       ;; @spec MCP-OP-ALIAS-059
       ;; forwarded-refusal-kind: the failing check's OWN kind travels verbatim
       ;; — :invalid-diagnostic-output and :verification-unverified, both minted
       ;; and scanned in this file — rather than being renamed to a constant
       :error-type (some :error-type (remove :ok checks))
       :elapsed_ms (reduce + 0.0 (map :elapsed_ms checks))})
    {:ok false
     :profile profile
     :error-type :unknown-verification-profile
     :error (str "Unknown closed verification profile: " profile)}))

(defn- diagnostic-delta-check
  [baseline future]
  (if-not (:ok future)
    future
    (let [delta (diagnostic-delta/diagnostic-delta
                  (:diagnostics baseline)
                  (:diagnostics future))]
      (merge (select-keys future [:command :exit :elapsed_ms])
             {:ok (:ok delta)
              :diagnostic-delta
              (-> delta
                  (update :introduced #(vec (take 20 %)))
                  (update :removed #(vec (take 20 %)))
                  (update :blocking-introduced #(vec (take 20 %))))}))))

(defn run-verification!
  "Run one closed verification profile for the changed files. When a baseline
  is supplied, cache-independent clj-kondo findings are compared as a delta."
  ([project-root profile profiles files]
   (run-verification! project-root profile profiles files nil))
  ([project-root profile profiles files baseline]
   (if-let [profile-spec (get profiles profile)]
     (let [commands (if (map? profile-spec)
                      (or (:commands profile-spec) [])
                      [profile-spec])
           checks (loop [remaining commands
                         completed []
                         diagnostic-index 0]
                    (if-let [command (first remaining)]
                      (let [diagnostic? (and baseline
                                             (diagnostic-command? command))
                            future (if diagnostic?
                                     (run-diagnostic-check! project-root command files)
                                     (run-check! project-root
                                                 (expand-command command files)))
                            check (if diagnostic?
                                    (if-let [baseline-check
                                             (nth (:checks baseline)
                                                  diagnostic-index nil)]
                                      (diagnostic-delta-check baseline-check future)
                                      {:ok false
                                       :command (first command)
                                       :error-type :missing-diagnostic-baseline})
                                    future)
                            completed (conj completed check)]
                        (if (:ok check)
                          (recur (next remaining)
                                 completed
                                 (if diagnostic?
                                   (inc diagnostic-index)
                                   diagnostic-index))
                          completed))
                      completed))
           command-ok? (and (= (count commands) (count checks))
                            (every? :ok checks))
           hot (when (and command-ok? (map? profile-spec) (:hot profile-spec))
                 (hot-verify/verify! project-root (:hot profile-spec)))
           hot-ok? (or (nil? hot) (:ok hot))
           cold (when (and command-ok? hot-ok? (map? profile-spec)
                           (:cold profile-spec))
                  (cold-verify/launch!
                    project-root profile
                    (update (:cold profile-spec) :command
                            #(expand-command % files))))]
       {:ok (and command-ok? hot-ok? (or (nil? cold) (:ok cold)))
        :profile profile
        :checks checks
        ;; @spec MCP-OP-ALIAS-059
        ;; forwarded-refusal-kind: the failing check's own kind, minted and
        ;; scanned in this file, travels verbatim
        :error-type (some :error-type (remove :ok checks))
        :hot-verification hot
        :cold-verification cold
        :verification_complete (not= :running (:status cold))
        :elapsed_ms (+ (reduce + 0.0 (map :elapsed_ms checks))
                       (double (or (:elapsed_ms hot) 0.0)))})
     {:ok false
      :profile profile
      :error-type :unknown-verification-profile
      :error (str "Unknown closed verification profile: " profile)})))

(defn reload-after-rollback!
  "Reload original namespaces in the configured application JVM after source
   rollback. Focused laws are omitted because the original suite already owned
   those bytes before the attempted transaction."
  [project-root profile profiles]
  (when-let [hot (get-in profiles [profile :hot])]
    (hot-verify/verify! project-root (assoc hot :tests []))))

(defn- publish-receipt!
  [receipt-dir receipt]
  (let [directory (io/file receipt-dir)
        _ (.mkdirs directory)
        path (str (io/file directory (str (UUID/randomUUID) ".edn")))]
    (file-ops/atomic-write! path (pr-str receipt))
    path))

(defn apply-basis!
  [{:keys [project-root receipt-dir verification-profiles verify! read-source write-source!
           prepare-compiled!]}
   {:keys [basis decisions verify]}]
  (prune-bases!)
  (if-let [prepared (get @basis-store basis)]
    (if (and (:workspace-root prepared)
             (not= (.toString (mcp-paths/real-root project-root))
                   (.toString (mcp-paths/real-root (:workspace-root prepared)))))
      {:ok false
       :error-type :basis-workspace-mismatch
       :error "The prepared basis belongs to a different workspace"
       :basis-workspace-root (:workspace-root prepared)
       :request-workspace-root (.toString (mcp-paths/real-root project-root))
       :source-unchanged true
       :remedy "Copy workspace_root from the returned next_call and retry once."}
      (let [validation (validate-decisions prepared decisions)
            materialized (when (:ok validation)
                           (materialize-decisions prepared decisions))
            outcome (or materialized validation)
            materialized-decisions (:decisions materialized)]
        (if-not (:ok outcome)
          (assoc outcome :source-unchanged true)
          (let [decisions-by-site (into {} (map (juxt :site identity) materialized-decisions))
                edits (->> (:sites prepared)
                           (keep (fn [site]
                                   (let [{:keys [replace delete]}
                                         (get decisions-by-site (:id site))]
                                     (cond
                                       replace
                                       (-> site
                                           (assoc :after replace)
                                           (dissoc :relative-file :role :owner))

                                       delete
                                       (-> site
                                           (assoc :delete true)
                                           (dissoc :relative-file :role :owner))

                                       :else nil))))
                           vec)]
            (if (empty? edits)
              {:ok false :error-type :empty-basis-change
               :error "Every prepared site was kept; no transaction was applied"
               :source-unchanged true}
              (let [compiled (transaction/compile-addressed-transaction
                               (:sources prepared) edits)
                    compiled (if (and (:ok compiled) prepare-compiled!)
                               (prepare-compiled! project-root compiled)
                               compiled)]
                (if-not (:ok compiled)
                  (assoc compiled :source-unchanged true)
                  (let [io-opts (cond-> {}
                                  read-source (assoc :read-source read-source)
                                  write-source! (assoc :write-source! write-source!))
                        profile (or verify (:verify prepared))
                        baseline (when profile
                                   (if verify!
                                     nil
                                     (capture-verification-baseline!
                                       project-root profile verification-profiles
                                       (mapv :file (:files compiled)))))
                        baseline-refusal? (and baseline (not (:ok baseline)))
                        committed (when-not baseline-refusal?
                                    (if (seq io-opts)
                                      (transaction/commit-compiled! compiled io-opts)
                                      (transaction/commit-compiled! compiled)))]
                    (if baseline-refusal?
                      {:ok false
                       :error-type :verification-baseline-failed
                       :error "Verification baseline capture failed before the addressed transaction"
                       :verification baseline
                       :source-unchanged true}
                      (if-not (:ok committed)
                        committed
                        (let [receipt (transaction/build-receipt compiled)
                              files (mapv :file (:files compiled))
                              verification (if verify!
                                             (verify! project-root profile
                                                      verification-profiles files)
                                             (run-verification!
                                               project-root profile
                                               verification-profiles files baseline))]
                          (if-not (:ok verification)
                            (let [inverse (transaction/compile-inverse
                                            receipt (:future-sources compiled))
                                  rollback (if (:ok inverse)
                                             (if (seq io-opts)
                                               (transaction/commit-compiled! inverse io-opts)
                                               (transaction/commit-compiled! inverse))
                                             inverse)
                                  hot-rollback (when (:ok rollback)
                                                 (reload-after-rollback!
                                                   project-root profile
                                                   verification-profiles))]
                              {:ok false
                               :error-type :verification-failed
                               :error "Verification failed; the addressed transaction was rolled back"
                               :verification verification
                               :rolled-back (boolean (:ok rollback))
                               :rollback rollback
                               :hot-rollback hot-rollback})
                            (let [receipt-file (publish-receipt!
                                                 (or receipt-dir
                                                     (workspace/receipt-dir project-root))
                                                 receipt)
                                  cold (:cold-verification verification)
                                  _ (cold-verify/attach-undo-from-verification!
                                      project-root verification receipt-file
                                      (:receipt-hash receipt))
                                  verification-complete? (not= :running (:status cold))]
                              (swap! basis-store dissoc basis)
                              (merge committed
                                     {:ok true
                                      :operation "apply-basis"
                                      :basis basis
                                      :match-count (:match-count compiled)
                                      :receipt-file receipt-file
                                      :receipt-hash (:receipt-hash receipt)
                                      :verification_complete verification-complete?
                                      :read_back_hashes (get-in committed
                                                                [:verified :read-back-hashes])
                                      :next_action (if verification-complete?
                                                     "none"
                                                     "inspect_verification_job")
                                      :verification verification}
                                     (when (and cold (not verification-complete?))
                                       {:next_call (:next_call cold)})
                                     (when (:format compiled)
                                       {:format (:format compiled)})))))))))))))))
    {:ok false :error-type :unknown-or-expired-basis
     :error "The prepared change basis is unknown or expired"
     :source-unchanged true
     :remedy "Call inspect_clojure prepare-change again, then submit its returned next_call."}))
