(ns clj-surgeon.mcp-relation-census
  "relation_census: the finder for event-sourced Clojure repositories.

   It reads. It never writes. It reports, per collection write inside a
   `defmethod fold-event` arm, whether that write goes through a known identity
   door, targets a set, is dominated by a recognised guard on the written
   value's identity, is unguarded (`:raw`), or cannot be decided (`:unknown`,
   with a reason). It LOCATES review work; it does not prove idempotency and it
   is not an enforcement gate."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.census-pool :as census-pool]
   [clj-surgeon.mcp-operation :as mcp-operation]
   [clj-surgeon.mcp-paths :as mcp-paths]
   [clj-surgeon.mcp-runtime :as runtime]
   [clj-surgeon.mcp-workspace :as workspace]
   [clj-surgeon.relation-census :as census]
   [clojure.string :as str])
  (:import
   (java.nio.file FileVisitResult Files LinkOption Path SimpleFileVisitor)
   (java.nio.file.attribute BasicFileAttributes)))

(def max-scanned-files census/max-scanned-files)
(def max-source-bytes census/max-source-bytes)
(def max-receipt-bytes 4096)
(def max-listed-sites 12)
(def max-listed-files 12)
(def max-listed-unrecognised 5)

;; @spec MCP-OP-CENSUS-009
;; @spec MCP-OP-CENSUS-026
(def census-tool-description
  (str
    "Census every collection write inside `defmethod fold-event` arms and "
    "classify each one as :door (routed through a known identity door), :set "
    "(the target is a set), :guarded (a recognised guard on the written "
    "value's identity dominates the write with the right polarity), :raw (no "
    "recognised guard dominates it), or :unknown with a reason "
    "(:helper-mediated-guard, :polarity, :unsupported-container, "
    ":unresolved-target). A :raw site is the vulnerability; an :unknown site "
    "is review work this version declines to decide. Omit files to census "
    "every file in the workspace that defines arms; pass files for an exact "
    "list. doors extends the default identity doors "
    "(conj-once, cons-once, upsert-by, conj-distinct-by, cons-distinct-by). "
    "The plan phase is parallel and the answer is pool-size independent. "
    "This verb reads only; it writes nothing and it is not an enforcement "
    "gate: it locates review work and never claims to prove idempotency. It is "
    "the one clj-surgeon tool that enumerates the workspace tree, so point it "
    "at the workspace you mean."))

(def census-tool-schema
  {:type "object"
   :additionalProperties false
   :properties
   {"workspace_root" {:type "string"}
    "files" {:type "array"
             :items {:type "string"}
             :minItems 1
             :maxItems 512}
    "doors" {:type "array"
             :items {:type "string"}
             :maxItems 32}
    "pool_size" {:type "integer" :minimum 1 :maximum 64}}})

;; @spec MCP-OP-SCHEMA-001
(def census-output-schema
  {:type "object"
   :additionalProperties true
   :properties {"ok" {:type "boolean"}
                "operation" {:type "string"}
                "census_version" {:type "integer"}
                "read_complete" {:type "boolean"}
                "files" {:type "integer"}
                "arms" {:type "integer"}
                "sites" {:type "integer"}
                "outside_arms" {:type "integer"}
                "counts" {:type "object"}
                "by_file" {:type "object"}
                "raw" {:type "array"}
                "guarded" {:type "array"}
                "unknown" {:type "array"}
                "pool_size" {:type "integer"}
                "phases_elapsed_ms" {:type "object"}
                "next_action" {:type "string"}
                "next_call" {:type "object"}
                "error_type" {:type "string"}
                "elapsed_ms" {:type "number" :minimum 0}}
   :required ["ok" "operation" "elapsed_ms"]})

(def census-annotations
  {:title "Relation Census"
   :read-only true
   :destructive false
   :idempotent true
   :open-world false
   :return-direct false})

(def ^:private runtime-config runtime/tool-config)

(defn init!
  "Set the live relation_census tool configuration. Passing nil disarms it."
  [config]
  (reset! runtime-config
          (when config
            (assoc config :workspace-router (workspace/router config)))))

;; ---------------------------------------------------------------------------
;; Refusals
;; ---------------------------------------------------------------------------

(defn- refusal
  [error-type message next-call & [data]]
  (merge {:ok false
          :operation "relation-census"
          :census_version census/census-version
          :error_type (name error-type)
          :error message
          :source_unchanged true
          :read_complete false
          :next_call next-call}
         data))

;; ---------------------------------------------------------------------------
;; Parameter validation (server-side; the advertised schema is only a hint)
;; ---------------------------------------------------------------------------

(def ^:private census-fields #{:files :doors :pool_size})

;; @spec MCP-OP-CENSUS-016
(defn validate-census-params
  "Validate relation_census parameters before any filesystem work.

   The JSON schema this tool advertises is a hint to a well-behaved caller. A
   malformed call reaches the server anyway, so every bound the schema states is
   re-checked here and refused with a typed reason and an executable next_call."
  [params workspace-root]
  (let [next-call (cond-> {:tool "relation_census" :pool_size 8}
                    workspace-root (assoc :workspace_root workspace-root))
        refuse (fn [reason message data]
                 (refusal :invalid-mcp-request message next-call
                          (merge {:reason (name reason)} data)))
        unknown (vec (sort (map name (remove census-fields (keys params)))))
        files (:files params)
        doors (:doors params)
        pool-size (:pool_size params)]
    (cond
      (seq unknown)
      (refuse :unknown-fields
              (str "relation_census does not accept " (str/join ", " unknown))
              {:unknown unknown
               :accepted (vec (sort (map name census-fields)))})

      (and (some? files) (not (sequential? files)))
      (refuse :files-not-an-array "files must be a JSON array of paths" {})

      (and (some? files) (empty? files))
      (refuse :empty-file-list
              "files must name at least one path; omit files to census the tree"
              {})

      (and (some? files) (> (count files) census/max-requested-files))
      (refuse :too-many-files
              "files exceeds the maximum file count"
              {:maximum census/max-requested-files :actual (count files)})

      (and (some? files) (not (every? #(and (string? %) (not (str/blank? %))) files)))
      (refuse :file-not-a-string "every entry in files must be a non-blank string" {})

      (and (some? doors) (not (sequential? doors)))
      (refuse :doors-not-an-array "doors must be a JSON array of symbols" {})

      (and (some? doors) (> (count doors) census/max-doors))
      (refuse :too-many-doors "doors exceeds the maximum door count"
              {:maximum census/max-doors :actual (count doors)})

      :else
      (let [coerced (when (some? pool-size) (census/coerce-pool-size pool-size))]
        (cond
          (and coerced (not (:ok coerced)) (= :not-an-integer (:reason coerced)))
          (refuse :pool-size-not-an-integer
                  (str "pool_size must be an integer between 1 and "
                       census/max-pool-size)
                  {:maximum census/max-pool-size :value (:value coerced)})

          (and coerced (not (:ok coerced)))
          (refuse :pool-size-out-of-range
                  (str "pool_size must be between 1 and " census/max-pool-size)
                  {:maximum census/max-pool-size :value (:value coerced)})

          :else
          {:ok true
           :params (cond-> params
                     coerced (assoc :pool_size (:size coerced)))})))))

;; ---------------------------------------------------------------------------
;; Discovery
;; ---------------------------------------------------------------------------

(def ^:private skipped-directories
  #{".git" "node_modules" "target" ".cpcache" ".clj-kondo" ".lsp" ".shadow-cljs"
    ".calva" "out" "dist" ".idea"})

(defn- source-name?
  [^Path path]
  (boolean (re-find #"\.clj[cs]?$" (str (.getFileName path)))))

(defn- escapes-root?
  "Does this entry's real location lie outside the canonical root?"
  [^Path root ^Path path]
  (try
    (not (.startsWith (.toRealPath path (make-array LinkOption 0)) root))
    (catch Throwable _ true)))

;; @spec MCP-OP-CENSUS-018
;; @spec MCP-OP-CENSUS-027
(defn- candidate-files
  "Project-relative Clojure sources under one canonical root, bounded.

   Walks with `Files/walkFileTree` and no `FOLLOW_LINKS`, so a symlinked
   directory is never descended: `dev/checkouts/foo -> ../../foo` costs one
   counted skip instead of refusing the whole census. Skip-directories are
   pruned before they are read rather than filtered out of the result, and the
   file cap terminates the walk rather than truncating it afterwards.

   Reaching the cap is a REFUSAL, not a result: the walk stops at one candidate
   past the ceiling and reports `:exceeded?` with the count it had seen, so the
   caller is told the tree is larger than the census may read instead of being
   handed a subset dressed as a complete answer."
  [^Path root]
  (let [found (java.util.ArrayList.)
        skipped (atom 0)
        exceeded (atom false)
        visitor
        (proxy [SimpleFileVisitor] []
          (preVisitDirectory [dir _attrs]
            (let [^Path dir dir]
              (if (and (not (.equals dir root))
                       (contains? skipped-directories (str (.getFileName dir))))
                FileVisitResult/SKIP_SUBTREE
                FileVisitResult/CONTINUE)))
          (visitFile [path attrs]
            (let [^Path path path
                  ^BasicFileAttributes attrs attrs]
              (cond
                (and (.isRegularFile attrs)
                     (source-name? path)
                     (<= (.size attrs) census/max-source-bytes))
                (cond
                  (escapes-root? root path)
                  (do (swap! skipped inc)
                      FileVisitResult/CONTINUE)

                  (>= (.size found) census/max-scanned-files)
                  (do (reset! exceeded true)
                      FileVisitResult/TERMINATE)

                  :else
                  (do (.add found (str (.relativize root path)))
                      FileVisitResult/CONTINUE))

                (and (.isSymbolicLink attrs) (escapes-root? root path))
                (do (swap! skipped inc)
                    FileVisitResult/CONTINUE)

                :else FileVisitResult/CONTINUE)))
          (visitFileFailed [_path _error] FileVisitResult/CONTINUE))]
    (Files/walkFileTree root #{} Integer/MAX_VALUE visitor)
    {:files (vec (sort found))
     :skipped-outside-root @skipped
     :exceeded? @exceeded
     :observed (cond-> (.size found) @exceeded inc)}))

;; @spec MCP-OP-CENSUS-017
(defn collect-inputs
  "Read each scanned path through the project fence, retaining only arm sources.

   The census needs the text of a file only if that file defines arms, so each
   source is tested as it is read and dropped when it does not. Nothing but the
   arm-defining sources is ever held at once, and a source above
   `max-source-bytes` is refused rather than read."
  ([root relatives] (collect-inputs root relatives {}))
  ([root relatives {:keys [declared?]}]
   (reduce
     (fn [acc relative]
       (let [resolved (mcp-paths/resolve-source-path root relative)]
         (cond
           (not (:ok resolved))
           (reduced (assoc acc :refusal resolved :file relative))

           (> (Files/size ^Path (:canonical resolved)) census/max-source-bytes)
           (reduced (assoc acc
                           :oversized relative
                           :bytes (Files/size ^Path (:canonical resolved))))

           :else
           (let [source (slurp (:path resolved))
                 acc (update acc :read inc)]
             (if (census/defines-arms? source)
               (update acc :inputs conj {:file relative :source source})
               ;; Not an arm file: its text is dropped here. Only its top-level
               ;; names survive, and only when a caller's doors need checking
               ;; against them.
               (cond-> acc
                 declared?
                 (update :declared into
                         (census/source-declared-names source))))))))
     {:inputs [] :read 0 :declared #{}}
     relatives)))

;; ---------------------------------------------------------------------------
;; Receipt
;; ---------------------------------------------------------------------------

(defn- public-site
  [site]
  (into {}
        (remove (comp nil? val))
        {:file (:file site)
         :line (:line site)
         :arm (:arm site)
         :write (:write site)
         :target (:target site)
         :value (:value site)
         :identity (:identity site)
         :guard (:guard site)
         :guard_line (:guard-line site)
         :polarity (some-> (:polarity site) name)
         :reason (some-> (:reason site) name)
         :detail (:detail site)}))

(defn- receipt-bytes
  [receipt]
  (count (.getBytes ^String (json/generate-string receipt) "UTF-8")))

(defn- longest-list-key
  [receipt]
  (->> [:raw :unknown :guarded]
       (sort-by #(- (count (get receipt % []))))
       (filter #(seq (get receipt % [])))
       first))

(defn- trim-once
  "Drop the cheapest remaining evidence, or nil when nothing is left to drop.

   Unmodelled-call examples go first (their count carries the signal), then
   listed sites, and `by_file` last: it is the summary a reviewer can act on
   without the site list, but with long project paths it alone overruns the
   budget, so it must be trimmable too."
  [receipt]
  (cond
    (seq (get-in receipt [:unrecognised_calls :examples]))
    (update-in receipt [:unrecognised_calls :examples] #(vec (butlast %)))

    (longest-list-key receipt)
    (update receipt (longest-list-key receipt) #(vec (butlast %)))

    (seq (:by_file receipt))
    (update receipt :by_file #(dissoc % (last (keys %))))

    :else nil))

(def ^:private receipt-envelope-allowance
  "Bytes the receipt gains after it is bounded: the operation clock's
   `elapsed_ms` and the JSON around the workspace root."
  64)

;; @spec MCP-OP-CENSUS-022
(defn- bound-receipt
  "Trim listed evidence, then per-file counts, until the receipt fits.

   `reserved` is the size of what the adapter and the operation clock append
   after this returns; the budget must hold for the receipt that is PUBLISHED,
   not for the one that is built."
  ([receipt] (bound-receipt receipt 0))
  ([receipt reserved]
   (loop [receipt receipt]
     (if (<= (+ (receipt-bytes receipt) reserved) max-receipt-bytes)
       receipt
       (if-let [trimmed (trim-once receipt)]
         (recur (assoc trimmed :receipt_truncated true))
         receipt)))))

(defn- listed
  [sites class-key]
  (let [matching (filterv #(= class-key (:class %)) sites)]
    (mapv public-site (take max-listed-sites matching))))

;; @spec MCP-OP-CENSUS-025
(defn- next-action
  [counts unrecognised]
  (cond
    (pos? (:raw counts 0))
    "review the raw sites: each is a collection write in a fold arm with no dominating recognised guard"

    (pos? (:unknown counts 0))
    "review the unknown sites: this census version declines to decide them; the reason names why"

    (pos? (:count unrecognised 0))
    (str "no site is unguarded, but " (:count unrecognised)
         " call(s) inside arms are not modelled by this census version ("
         (str/join ", " (take 3 (map :call (:examples unrecognised))))
         "): a write behind one of them is not a site here")

    :else "none"))

;; @spec MCP-OP-CENSUS-013
(defn- build-receipt
  [{:keys [merged pool-size requested-pool phases scanned skipped-outside-root
           reserved]}]
  (let [counts (:counts merged)
        sites (:all-sites merged)
        unrecognised (census/unrecognised-summary
                       (:unrecognised merged) max-listed-unrecognised)]
    (bound-receipt
      (into {}
            (remove (comp nil? val))
            {:ok true
             :operation "relation-census"
             :census_version census/census-version
             :read_complete true
             :files (:files merged)
             :arms (:arms merged)
             :sites (:sites merged)
             :outside_arms (:outside-arms merged)
             :files_scanned scanned
             :skipped_outside_root (when (pos? (or skipped-outside-root 0))
                                     skipped-outside-root)
             :counts counts
             :by_file (into (sorted-map)
                            (take max-listed-files
                                  (map (fn [[f v]]
                                         [f (assoc (:counts v)
                                                   :arms (:arms v)
                                                   :sites (:sites v))])
                                       (:by-file merged))))
             :raw (listed sites :raw)
             :guarded (listed sites :guarded)
             :unknown (listed sites :unknown)
             :pool_size pool-size
             :pool_size_requested (when (and requested-pool
                                             (> requested-pool pool-size))
                                    requested-pool)
             :phases_elapsed_ms phases
             :unrecognised_calls unrecognised
             :next_action (next-action counts unrecognised)})
      (or reserved 0))))

;; ---------------------------------------------------------------------------
;; Execution
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-CENSUS-027
(defn- ceiling-refusal
  "Refuse a tree that holds more candidate sources than the census may read.

   The caller gets the ceiling, the count that fits, the count the walk had
   observed when it stopped (a lower bound, because the walk stops rather than
   enumerating the rest), and a next_call that narrows the scan."
  [discovered canonical]
  (refusal :too-many-candidate-files
           (str "This workspace holds more than " census/max-scanned-files
                " candidate Clojure sources (" (:observed discovered)
                " seen before the walk stopped). The census reads at most "
                census/max-scanned-files
                " and will not report a truncated tree as a complete census: "
                "name the sources, or point workspace_root at a narrower "
                "subtree.")
           {:tool "relation_census"
            :workspace_root canonical
            :files [(str "<at most " census/max-scanned-files
                         " named sources under this root>")]}
           {:maximum census/max-scanned-files
            :fits census/max-scanned-files
            :observed (:observed discovered)
            :observed_at_least true
            :files_read 0}))

;; @spec MCP-OP-CENSUS-014
(defn- door-refusal
  [invalid canonical]
  (refusal :unknown-door-symbol
           (str "Unknown identity door " (:invalid invalid) ": " (:why invalid))
           {:tool "relation_census"
            :workspace_root canonical
            :doors (vec (sort (map str census/default-doors)))}
           {:door (:invalid invalid)
            :known_doors (vec (sort (map str census/default-doors)))}))

;; @spec MCP-OP-CENSUS-023
(defn- execute-in-context!
  [{:keys [project-root]} {:keys [files doors pool_size]}]
  (let [root (mcp-paths/real-root project-root)
        canonical (.toString root)
        want-declared? (boolean (seq doors))
        t0 (System/nanoTime)
        requested (when (seq files) (mapv str files))
        discovered (when-not requested (candidate-files root))
        scanned (or requested (:files discovered))
        skipped-outside-root (:skipped-outside-root discovered 0)
        t-discovered (System/nanoTime)
        ;; The ceiling is checked BEFORE any read: a tree the census may not
        ;; finish is refused, never partially read and published as complete.
        loaded (when-not (:exceeded? discovered)
                 (collect-inputs root scanned {:declared? want-declared?}))
        t-read (System/nanoTime)]
    (cond
      (:exceeded? discovered)
      (ceiling-refusal discovered canonical)

      (:refusal loaded)
      (refusal :unreadable-source-path
               (str (:error (:refusal loaded)) " (" (:file loaded) ")")
               {:tool "relation_census"
                :workspace_root canonical
                :files [(:file loaded)]}
               {:file (:file loaded)})

      (:oversized loaded)
      (refusal :source-too-large
               (str (:oversized loaded) " is " (:bytes loaded)
                    " bytes; the census reads at most " census/max-source-bytes)
               {:tool "relation_census"
                :workspace_root canonical
                :files ["<a source under the byte cap>"]}
               {:file (:oversized loaded)
                :bytes (:bytes loaded)
                :maximum census/max-source-bytes})

      :else
      (let [candidates (:inputs loaded)]
        (if (empty? candidates)
          (refusal :no-fold-arms-found
                   (str "No file defines defmethod fold-event arms. Scanned "
                        (count scanned) " file(s).")
                   {:tool "relation_census"
                    :workspace_root canonical
                    :files (vec (take max-listed-files scanned))}
                   (cond-> {:files_scanned (count scanned)
                            :scanned (vec (take max-listed-files scanned))}
                     (pos? skipped-outside-root)
                     (assoc :skipped_outside_root skipped-outside-root)))
          ;; The door symbols themselves are checked before any census runs;
          ;; whether a door is DEFINED anywhere can only be answered once the
          ;; scan has been parsed, so that half waits for the plan's own
          ;; `:declared` rather than parsing every file a second time.
          (let [syntactic (if want-declared?
                            (census/parse-doors doors nil)
                            census/default-doors)]
            (if (map? syntactic)
              (door-refusal syntactic canonical)
              (let [requested-pool (or pool_size (census-pool/default-pool-size))
                    pool-size (census/effective-pool-size requested-pool)
                    planned (census/plan {:inputs candidates
                                          :doors syntactic
                                          :map-fn (census-pool/pooled-map pool-size)})
                    declared (when want-declared?
                               (into (:declared loaded #{}) (:declared planned)))
                    confirmed (when want-declared?
                                (census/parse-doors doors declared))]
                (cond
                  (not (:ok planned))
                  (refusal (or (:error-type planned) :census-failed)
                           (:error planned)
                           {:tool "relation_census"
                            :workspace_root canonical
                            :files [(:file planned)]}
                           {:file (:file planned)})

                  (map? confirmed)
                  (door-refusal confirmed canonical)

                  :else
                  (build-receipt
                    {:merged planned
                     :reserved (+ receipt-envelope-allowance (count canonical))
                     :pool-size pool-size
                     :requested-pool requested-pool
                     :scanned (count scanned)
                     :skipped-outside-root skipped-outside-root
                     :phases (cond-> {:read (/ (- t-read t-discovered) 1e6)
                                      :classify (get-in planned [:phases :classify])
                                      :merge (get-in planned [:phases :merge])}
                               discovered
                               (assoc :discover (/ (- t-discovered t0) 1e6)))}))))))))))

;; @spec MCP-OP-CENSUS-017
(defn- exhaustion-refusal
  "Turn a Throwable that escaped the census into a typed refusal.

   A census walks a tree it did not choose. Running out of heap or stack is a
   bounded, reportable outcome of that walk, not an adapter crash, and the
   caller needs an executable narrower call rather than a stack trace."
  [^Throwable error]
  (let [exhausted? (instance? VirtualMachineError error)]
    (refusal (if exhausted? :census-resource-exhausted :census-adapter-failure)
             (str (if exhausted?
                    "The census exhausted a runtime resource: "
                    "The census failed: ")
                  (.getName (class error))
                  (when-let [message (.getMessage error)] (str " " message)))
             {:tool "relation_census"
              :files ["<a narrower file list>"]
              :pool_size 1}
             {:exhausted exhausted?})))

(defn execute-request!
  "Route and execute one relation_census request."
  [config params]
  (let [normalized (json/parse-string (json/generate-string params) true)
        router (or (:workspace-router config) (workspace/router config))
        routed (workspace/resolve-request router normalized)]
    (if-not (:ok routed)
      (assoc routed
             :ok false
             :operation "relation-census"
             :error_type (or (:error_type routed) "invalid-workspace-root")
             :read_complete false
             :next_call {:tool "relation_census"
                         :workspace_root "<an existing absolute directory>"})
      (let [validated (validate-census-params (:params routed)
                                              (:workspace-root routed))]
        (if-not (:ok validated)
          (assoc validated :workspace_root (:workspace-root routed))
          (assoc (try
                   (execute-in-context! (:config routed) (:params validated))
                   (catch Throwable error
                     (exhaustion-refusal error)))
                 :workspace_root (:workspace-root routed)))))))

(defn- summary
  [result]
  (if (:ok result)
    (let [c (:counts result)]
      (str "relation_census\n  " (:files result) " file(s) · "
           (:arms result) " arm(s) · " (:sites result) " site(s) · "
           "raw " (:raw c 0) " · unknown " (:unknown c 0)
           " · guarded " (:guarded c 0) " · door " (:door c 0)
           " · set " (:set c 0) " · outside-arms " (:outside_arms result)
           " · pool " (:pool_size result) " · "
           (mcp-operation/format-elapsed-ms (:elapsed_ms result))
           "\nnext_action: " (:next_action result)))
    (str "relation_census refused · " (:error_type result)
         " · " (mcp-operation/format-elapsed-ms (:elapsed_ms result))
         "\n" (:error result) "\nnothing was written")))

(defn handle-relation-census
  "clojure-mcp callback handler retained as a Var for hot reload."
  [_exchange params callback]
  (mcp-operation/invoke!
    {:execute #(if-let [config @runtime-config]
                 (execute-request! config params)
                 (refusal :server-not-initialized
                          "relation_census server is not initialized"
                          {:tool "relation_census"}))
     :summarize summary
     :callback callback}))

;; @spec MCP-OP-CENSUS-015
(def relation-census-tool
  {:id :relation-census
   :name "relation_census"
   :description census-tool-description
   :schema census-tool-schema
   :output-schema census-output-schema
   :annotations census-annotations
   :structured? true
   :tool-fn #'handle-relation-census})
