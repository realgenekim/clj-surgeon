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
   [clojure.java.io :as io]
   [clojure.string :as str]))

(def max-scanned-files 4000)
(def max-source-bytes (* 2 1024 1024))
(def max-receipt-bytes 4096)
(def max-listed-sites 12)
(def max-listed-files 12)

;; @spec MCP-OP-CENSUS-009
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
    "gate: it locates review work and never claims to prove idempotency."))

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
;; Discovery
;; ---------------------------------------------------------------------------

(def ^:private skipped-directories
  #{".git" "node_modules" "target" ".cpcache" ".clj-kondo" ".lsp" ".shadow-cljs"
    ".calva" "out" "dist" ".idea"})

(defn- candidate-files
  "Project-relative Clojure sources under one canonical root, bounded."
  [root]
  (let [root-file (io/file (str root))
        prefix (inc (count (.getPath root-file)))]
    (->> (file-seq root-file)
         (remove (fn [f]
                   (some skipped-directories
                         (str/split (subs (.getPath f)
                                          (min prefix (count (.getPath f))))
                                    #"/"))))
         (filter #(.isFile ^java.io.File %))
         (filter #(re-find #"\.clj[cs]?$" (.getName ^java.io.File %)))
         (filter #(<= (.length ^java.io.File %) max-source-bytes))
         (map #(subs (.getPath ^java.io.File %) prefix))
         sort
         (take max-scanned-files)
         vec)))

(defn- load-inputs
  "Resolve and read each requested path through the existing project fence."
  [root relatives]
  (reduce
    (fn [acc relative]
      (let [resolved (mcp-paths/resolve-source-path root relative)]
        (if-not (:ok resolved)
          (reduced {:refusal resolved :file relative})
          (update acc :inputs conj {:file relative
                                    :source (slurp (:path resolved))}))))
    {:inputs []}
    relatives))

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

(defn- bound-receipt
  "Trim listed evidence until the published receipt fits its byte budget."
  [receipt]
  (loop [receipt receipt]
    (if (or (<= (receipt-bytes receipt) max-receipt-bytes)
            (nil? (longest-list-key receipt)))
      receipt
      (let [k (longest-list-key receipt)]
        (recur (-> receipt
                   (update k #(vec (butlast %)))
                   (assoc :receipt_truncated true)))))))

(defn- listed
  [sites class-key]
  (let [matching (filterv #(= class-key (:class %)) sites)]
    (mapv public-site (take max-listed-sites matching))))

(defn- next-action
  [counts]
  (cond
    (pos? (:raw counts 0))
    "review the raw sites: each is a collection write in a fold arm with no dominating recognised guard"

    (pos? (:unknown counts 0))
    "review the unknown sites: this census version declines to decide them; the reason names why"

    :else "none"))

;; @spec MCP-OP-CENSUS-013
(defn- build-receipt
  [{:keys [merged pool-size phases scanned]}]
  (let [counts (:counts merged)
        sites (:all-sites merged)]
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
             :counts counts
             :by_file (into {}
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
             :phases_elapsed_ms phases
             :next_action (next-action counts)}))))

;; ---------------------------------------------------------------------------
;; Execution
;; ---------------------------------------------------------------------------

(defn- parse-doors
  [doors declared]
  (reduce
    (fn [acc value]
      (let [sym (try (symbol (str/trim (str value))) (catch Throwable _ nil))]
        (cond
          (or (nil? sym) (str/blank? (str value)) (str/includes? (str value) " "))
          (reduced {:invalid (str value) :why "not a symbol"})

          (contains? '#{conj cons into concat} (symbol (name sym)))
          (reduced {:invalid (str value) :why "shadows a collection write head"})

          (and (some? declared)
               (not (contains? census/default-doors (symbol (name sym))))
               (not (contains? declared (symbol (name sym)))))
          (reduced {:invalid (str value)
                    :why "not defined in any scanned file"})

          :else (conj acc (symbol (name sym))))))
    #{}
    doors))

;; @spec MCP-OP-CENSUS-014
(defn- execute-in-context!
  [{:keys [project-root]} {:keys [files doors pool_size]}]
  (let [root (mcp-paths/real-root project-root)
        canonical (.toString root)
        t0 (System/nanoTime)
        requested (when (seq files) (mapv str files))
        scanned (or requested (candidate-files root))
        loaded (load-inputs root scanned)]
    (if-let [path-refusal (:refusal loaded)]
      (refusal :unreadable-source-path
               (str (:error path-refusal) " (" (:file loaded) ")")
               {:tool "relation_census"
                :workspace_root canonical
                :files [(:file loaded)]}
               {:file (:file loaded)})
      (let [candidates (filterv #(census/defines-arms? (:source %))
                                (:inputs loaded))
            t1 (System/nanoTime)]
        (if (empty? candidates)
          (refusal :no-fold-arms-found
                   (str "No file defines defmethod fold-event arms. Scanned "
                        (count scanned) " file(s).")
                   {:tool "relation_census"
                    :workspace_root canonical
                    :files (vec (take max-listed-files scanned))}
                   {:files_scanned (count scanned)
                    :scanned (vec (take max-listed-files scanned))})
          (let [declared (reduce into #{}
                                 (map #(:declared (census/census-file
                                                    (select-keys % [:file :source])))
                                      candidates))
                resolved-doors (if (seq doors)
                                 (parse-doors doors declared)
                                 census/default-doors)]
            (if (map? resolved-doors)
              (refusal :unknown-door-symbol
                       (str "Unknown identity door " (:invalid resolved-doors)
                            ": " (:why resolved-doors))
                       {:tool "relation_census"
                        :workspace_root canonical
                        :doors (vec (sort (map str census/default-doors)))}
                       {:door (:invalid resolved-doors)
                        :known_doors (vec (sort (map str census/default-doors)))})
              (let [pool-size (or pool_size (census-pool/default-pool-size))
                    t2 (System/nanoTime)
                    planned (census/plan {:inputs candidates
                                          :doors resolved-doors
                                          :map-fn (census-pool/pooled-map pool-size)})]
                (if-not (:ok planned)
                  (refusal (or (:error-type planned) :census-failed)
                           (:error planned)
                           {:tool "relation_census"
                            :workspace_root canonical
                            :files [(:file planned)]}
                           {:file (:file planned)})
                  (build-receipt
                    {:merged planned
                     :pool-size pool-size
                     :scanned (count scanned)
                     :phases (-> {:discover (/ (- t1 t0) 1e6)
                                  :parse (/ (- t2 t1) 1e6)}
                                 (assoc :classify (get-in planned [:phases :classify])
                                        :merge (get-in planned [:phases :merge])))}))))))))))

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
      (assoc (execute-in-context! (:config routed) (:params routed))
             :workspace_root (:workspace-root routed)))))

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
