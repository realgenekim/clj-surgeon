(ns clj-surgeon.namespace-split
  "Pure single-snapshot namespace partition compiler. Reference identities come
  from clj-kondo; this namespace neither evaluates Clojure nor writes files."
  (:require [clj-surgeon.extract :as extract]
            [clj-surgeon.outline :as outline]
            [clj-surgeon.quoted-var-refs :as quoted-vars]
            [clj-surgeon.structural-lens :as lens]
            [clojure.set :as set]
            [clojure.string :as str]
            [rewrite-clj.node :as n]
            [rewrite-clj.parser :as parser]
            [rewrite-clj.zip :as z]))

(defn snapshot-hash [sources]
  (lens/source-hash (pr-str (into (sorted-map) sources))))

(defn- meaningful? [node]
  (not (or (n/whitespace? node) (n/comment? node) (= :uneval (n/tag node)))))

(defn- sexpr [node] (try (n/sexpr node) (catch Exception _ nil)))

(defn- offsets [source]
  (vec (cons 0 (map inc (keep-indexed #(when (= %2 \newline) %1) source)))))

(defn- span [starts row col end-row end-col]
  [(+ (nth starts (dec row)) (dec col))
   (+ (nth starts (dec end-row)) (dec end-col))])

(defn- node-span [starts node]
  (let [{:keys [row col end-row end-col]} (meta node)]
    (span starts row col end-row end-col)))

(defn- splice [source edits]
  (reduce (fn [s {:keys [start end text]}]
            (str (subs s 0 start) text (subs s end)))
          source (sort-by :start > (distinct edits))))

(defn- parse-file [file source]
  (let [root (parser/parse-string-all source)
        starts (offsets source)
        ns-node (first (filter #(= 'ns (first (sexpr %)))
                               (filter #(= :list (n/tag %)) (n/children root))))
        ns-value (sexpr ns-node)
        forms (:forms (outline/outline-source file source))
        definitions (into {} (for [f forms :when (and (:name f) (not= 'declare (:type f)))]
                               [[(:line f) (str (:name f))] f]))
        definition-for (fn [node] (when (= :list (n/tag node))
                                    (get definitions [(:row (meta node)) (str (second (sexpr node)))])))
        pieces (loop [nodes (n/children root) pending "" result []]
                 (if-let [node (first nodes)]
                   (let [[start end] (node-span starts node)
                         f (definition-for node)]
                     (cond
                       (not (meaningful? node))
                       (recur (rest nodes) (str pending (n/string node)) result)
                       f (recur (rest nodes) ""
                                (conj result (assoc f :name (str (:name f)) :node node
                                                   :start start :end end :prefix pending)))
                       :else (recur (rest nodes) "" result)))
                   (cond-> result (seq result) (update (dec (count result)) assoc :suffix pending))))]
    {:file file :source source :root root :starts starts :ns-node ns-node
     :lib (str (second ns-value)) :ns-value ns-value :owners pieces
     :unsupported (vec (for [node (n/children root)
                            :when (and (meaningful? node) (not (identical? node ns-node))
                                       (not (definition-for node))
                                       (not (and (= :list (n/tag node)) (= 'declare (first (sexpr node))))))]
                        {:type :unowned-top-level :file file :row (:row (meta node))}))}))

(defn- libspecs [parsed]
  (vec (mapcat rest (filter #(and (sequential? %) (= :require (first %)))
                            (drop 2 (:ns-value parsed))))))
(defn- lib-of [entry] (str (if (sequential? entry) (first entry) entry)))
(defn- options [entry] (when (sequential? entry) (apply hash-map (rest entry))))
(defn- aliases [entries]
  (into {} (keep (fn [entry] (when-let [a (or (:as (options entry)) (:as-alias (options entry)))]
                             [(str a) (lib-of entry)]))) entries))

(defn- lexical-required-libs [owners entries]
  ;; Preserve existing libspecs used by alias-qualified keywords or quoted Vars
  ;; that the analyzer does not emit as var-usages. This supplements header
  ;; retention only; it never manufactures a Var rewrite or a promotion.
  (let [bound (aliases entries)]
    (set (for [owner owners node (tree-seq n/inner? n/children (:node owner))
               :when (= :token (n/tag node))
               :let [value (sexpr node) text (n/string node)
                     qualifier (if (symbol? value) (namespace value)
                                   (when (str/starts-with? text "::")
                                     (first (str/split (subs text 2) #"/"))))
                     lib (get bound qualifier)]
               :when lib] lib))))

(defn- owner-at [parsed row col]
  (when row
    (let [offset (+ (nth (:starts parsed) (dec row)) (dec (or col 1)))]
      (first (filter #(<= (:start %) offset (dec (:end %))) (:owners parsed))))))

(defn- site [parsed usage]
  (let [[start end] (span (:starts parsed)
                          (or (:name-row usage) (:row usage))
                          (or (:name-col usage) (:col usage))
                          (or (:name-end-row usage) (:end-row usage))
                          (or (:name-end-col usage) (:end-col usage)))]
    {:file (:file parsed) :row (or (:name-row usage) (:row usage))
     :col (or (:name-col usage) (:col usage))
     :start start :end end :token (subs (:source parsed) start end)
     :owner (:name (owner-at parsed (or (:name-row usage) (:row usage)) (or (:name-col usage) (:col usage))))
     :var (str (:name usage)) :to-lib (str (:to usage))}))

(defn- located? [usage]
  (every? pos-int? [(or (:name-row usage) (:row usage))
                   (or (:name-col usage) (:col usage))
                   (or (:name-end-row usage) (:end-row usage))
                   (or (:name-end-col usage) (:end-col usage))]))

(defn- quoted-sites [parsed source-lib names]
  (for [reference (quoted-vars/references-in-source-for-subjects
                    (:file parsed) (:source parsed) (mapv #(str source-lib "/" %) (sort names)))
        :let [text (:source reference)
              token (last (filter #(and (= :token (n/tag %)) (symbol? (sexpr %)))
                                  (tree-seq n/inner? n/children (parser/parse-string-all text))))
              [start end] (node-span (offsets text) token)
              base (+ (nth (:starts parsed) (dec (:line reference))) (dec (:character reference)))]]
    {:file (:file parsed) :row (:line reference) :col (+ (:character reference) start)
     :start (+ base start) :end (+ base end) :token (n/string token)
     :owner (:name (owner-at parsed (:line reference) (:character reference)))
     :var (last (str/split (:subject reference) #"/")) :to-lib source-lib :var_quote true}))

(defn- unresolved-qualified-sites [parsed source-lib covered]
  (let [qualifiers (conj (set (for [[alias lib] (aliases (libspecs parsed)) :when (= lib source-lib)] alias)) source-lib)]
    (for [top (n/children (:root parsed)) :when (not (identical? top (:ns-node parsed)))
          node (tree-seq #(and (n/inner? %) (not= :uneval (n/tag %))) n/children top)
          :when (= :token (n/tag node))
          :let [value (sexpr node) text (n/string node)
                qualifier (if (symbol? value) (namespace value)
                              (when (str/starts-with? text "::") (first (str/split (subs text 2) #"/"))))]
          :when (qualifiers qualifier)
          :let [[start end] (node-span (:starts parsed) node)]
          :when (not (covered [(:file parsed) start end]))]
      {:file (:file parsed) :row (:row (meta node)) :col (:col (meta node))
       :token text :reason :unresolved-qualified-reference})))

(defn- sccs [edges]
  (let [adj (reduce (fn [m [a b]] (update m a (fnil conj #{}) b)) {} edges)
        nodes (set (mapcat identity edges))
        reachable (fn [start]
                    (loop [todo [start] seen #{}]
                      (if-let [x (peek todo)]
                        (if (seen x) (recur (pop todo) seen)
                            (recur (into (pop todo) (get adj x)) (conj seen x))) seen)))
        reaches (into {} (map (juxt identity reachable)) nodes)]
    (loop [remaining nodes result []]
      (if-let [x (first (sort remaining))]
        (let [group (set (filter #(and ((get reaches x) %) ((get reaches %) x)) remaining))]
          (recur (set/difference remaining group)
                 (cond-> result (or (> (count group) 1) (contains? (get adj x) x))
                   (conj (vec (sort group))))))
        (vec (sort result))))))

(defn- promote-source [source]
  (let [loc (z/of-string source) head (z/down loc)]
    (if (= "defn-" (z/string head))
      (extract/publicize-defn-source source)
      ;; Own only :private metadata; preserve every other metadata entry.
      (loop [loc loc]
        (if (z/end? loc) (z/root-string loc)
            (if (and (= :meta (z/tag loc))
                     (= ":private" (some-> loc z/down z/string)))
              (z/root-string (z/replace loc (z/node (-> loc z/down z/right))))
              (if (and (= :map (z/tag loc))
                       (= :meta (some-> loc z/up z/tag))
                       (contains? (sexpr (z/node loc)) :private))
                (z/root-string (z/replace loc (dissoc (sexpr (z/node loc)) :private)))
                (recur (z/next loc)))))))))

(defn- header [parsed lib entries used-classes]
  (let [clauses (for [clause (drop 2 (:ns-value parsed))
                     :when (and (sequential? clause) (not= :require (first clause)))
                     :let [clause (if (= :import (first clause))
                                    (cons :import
                                          (keep (fn [entry]
                                                  (if (sequential? entry)
                                                    (let [classes (filter #(contains? used-classes (str (first entry) "." %)) (rest entry))]
                                                      (when (seq classes) (cons (first entry) classes)))
                                                    (when (contains? used-classes (str entry)) entry)))
                                                (rest clause))) clause)]
                     :when (or (not= :import (first clause)) (seq (rest clause)))] clause)]
    (str "(ns " lib
         (apply str (for [value (drop 2 (:ns-value parsed)) :when (or (string? value) (map? value))]
                      (str "\n  " (pr-str value))))
         (when (seq entries) (str "\n  (:require\n    " (str/join "\n    " (map pr-str entries)) ")"))
         (apply str (map #(str "\n  " (pr-str %)) clauses)) ")")))

(defn- caller-header [parsed source-lib added]
  (let [ns-node (:ns-node parsed)
        clause? #(and (= :list (n/tag %)) (= :require (first (sexpr %))))
        clauses (filter clause? (n/children ns-node))
        append-nodes (fn [children]
                       (into (vec children) (mapcat #(vector (n/newline-node "\n") (n/spaces 4)
                                                           (parser/parse-string (pr-str %)))) added))
        changed (if (seq clauses)
                  (let [first-clause (first clauses)]
                    (n/replace-children ns-node
                      (mapv (fn [child]
                              (if (clause? child)
                                (let [kids (remove #(and (meaningful? %)
                                                         (= source-lib (lib-of (sexpr %)))) (n/children child))]
                                  (n/replace-children child (if (identical? child first-clause) (append-nodes kids) (vec kids))))
                                child)) (n/children ns-node))))
                  (n/replace-children ns-node
                    (into (vec (n/children ns-node))
                          [(n/newline-node "\n") (n/spaces 2)
                           (parser/parse-string (str "(:require " (str/join " " (map pr-str added)) ")"))])))]
    (n/string changed)))

(defn- allocate [entries needed destinations]
  (reduce (fn [{:keys [bound] :as state} lib]
            (let [policy (:alias_policy (get destinations lib))
                  alias (first (filter #(or (not (contains? bound %)) (= lib (get bound %))) policy))]
              (if alias
                (-> state (assoc-in [:chosen lib] alias) (assoc-in [:bound alias] lib))
                (update state :blockers conj {:type :alias-policy-exhausted :lib lib :policy policy}))))
          {:bound (aliases entries) :chosen {} :blockers []} (sort needed)))

;; @spec NS-SPLIT-001
;; @spec NS-SPLIT-002
;; @spec NS-SPLIT-003
;; @spec NS-SPLIT-004
;; @spec NS-SPLIT-005
;; @spec NS-SPLIT-006
;; @spec NS-SPLIT-007
;; @spec NS-SPLIT-008
(defn compile-split
  "Compile request + {:sources {relative-file bytes}, :analysis kondo-facts,
  :source-paths configured-roots} into ONE future file set and its projection."
  [request {:keys [sources analysis source-paths]}]
  (let [source-file (get-in request [:source :file]) source-lib (get-in request [:source :lib])
        parsed (into {} (map (fn [[f s]] [f (parse-file f s)])) sources)
        original (get parsed source-file)
        owners (:owners original)
        dests (:destinations request)
        dest-by-lib (into {} (map (juxt :lib identity)) dests)
        assignments (group-by first (for [d dests name (:forms d)] [name (:lib d)]))
        mapping (into {} (map (fn [[name xs]] [name (second (first xs))])) assignments)
        known (set (map :name owners))
        duplicates (vec (sort (concat (keep (fn [[name xs]] (when (> (count xs) 1) name)) assignments)
                                     (keep (fn [[name xs]] (when (> (count xs) 1) name)) (group-by :name owners)))))
        unmapped (vec (sort (set/difference known (set (keys mapping)))))
        usages (for [u (:var-usages analysis) :let [p (get parsed (:filename u))] :when (and p (located? u))] (site p u))
        moved-sites (->> (concat (filter #(and (= source-lib (:to-lib %)) (known (:var %))) usages)
                                  (mapcat #(quoted-sites % source-lib known) (vals parsed)))
                         (reduce (fn [m s] (assoc m [(:file s) (:start s) (:end s)] s)) (sorted-map)) vals vec)
        load-callers (for [[file p] parsed :when (and (not= source-file file)
                                                     (some #(= source-lib (lib-of %)) (libspecs p)))] file)
        sites-by-file (merge (zipmap load-callers (repeat [])) (group-by :file moved-sites))
        edges (mapv (fn [s] (assoc (select-keys s [:file :row :col :owner :var :token :var_quote])
                                  :from (if (= source-file (:file s)) (get mapping (:owner s)) (:lib (get parsed (:file s))))
                                  :to (get mapping (:var s)))) moved-sites)
        private-names (into #{} (comp (filter #(and (= source-lib (str (:ns %))) (:private %))) (map #(str (:name %)))) (:var-definitions analysis))
        promotions (vec (for [name (sort private-names)
                              :let [callers (filterv #(and (= name (:var %)) (not (:var_quote %)) (not= (:from %) (:to %))) edges)]
                              :when (seq callers)]
                          {:form name :lib (get mapping name) :reason "referenced across destination boundary" :callers callers}))
        authorized (if (= "promote-required" (:promotion_policy request))
                     (set (map :form promotions)) (set (:promotion_policy request)))
        missing-promotions (remove authorized (map :form promotions))
        snapshot (snapshot-hash sources)
        blocks (vec (concat
                     (when-not original [{:type :missing-source :file source-file}])
                     (when-not (= source-lib (:lib original)) [{:type :source-lib-mismatch :expected source-lib :actual (:lib original)}])
                     (:unsupported original)
                     (for [o owners :when (not (#{'def 'defn 'defn-} (:type o)))]
                       {:type :unsupported-owner :form (:name o) :owner_type (str (:type o))})
                     (when-not (and (str/ends-with? source-file ".clj") (every? #(str/ends-with? (:file %) ".clj") dests))
                       [{:type :unsupported-dialect :supported ".clj"}])
                     (for [name duplicates] {:type :duplicate-owner :form name})
                     (for [name unmapped] {:type :unmapped-owner :form name})
                     (for [name (sort (set/difference (set (keys mapping)) known))] {:type :unknown-owner :form name})
                     (for [name missing-promotions] {:type :undecided-promotion :form name})
                     (when (and (:snapshot_hash request) (not= snapshot (:snapshot_hash request))) [{:type :snapshot-drift :expected (:snapshot_hash request) :actual snapshot}])
                     (for [d dests :let [derived (extract/file-path->ns-name (:file d) source-paths (:workspace_root request))]
                           :when (not= derived (:lib d))] {:type :destination-lib-path-mismatch :lib (:lib d) :file (:file d) :path-lib derived})
                     (for [d dests :when (contains? sources (:file d))] {:type :destination-exists :file (:file d)})
                     (when-not (= (count dests) (count dest-by-lib) (count (set (map :file dests)))) [{:type :duplicate-destination}])
                     (when-not (#{"delete" "retain-empty"} (:source_retirement request)) [{:type :invalid-source-retirement}])) )
        forward (vec (for [e edges :when (and (= source-file (:file e)) (= (:from e) (:to e)))
                           :let [target (first (filter #(= (:var e) (:name %)) owners))]
                           :when (< (+ (nth (:starts original) (dec (:row e))) (dec (:col e))) (:start target))]
                       (select-keys e [:from :owner :var :row :col])))
        owner-usages (group-by :owner (filter #(= source-file (:file %)) usages))
        unknowns (vec (concat
                       (mapcat #(unresolved-qualified-sites % source-lib
                                                            (set (map (juxt :file :start :end) moved-sites))) (vals parsed))
                       (for [u (:var-usages analysis)
                             :when (and (= source-lib (str (:to u))) (not (located? u)))]
                         {:file (:filename u) :var (str (:name u)) :reason :unlocated-source-reference})
                       (for [s usages :when (and (= source-file (:file s)) (= "clj-kondo/unknown-namespace" (:to-lib s)))]
                         (assoc (select-keys s [:file :row :col :token]) :reason :unresolved-reference))
                       (for [owner owners node (tree-seq n/inner? n/children (:node owner))
                             :when (or (= :syntax-quote (n/tag node))
                                       (str/starts-with? (n/string node) "#::")
                                       (and (= :token (n/tag node)) (str/starts-with? (n/string node) "::")
                                            (not (str/includes? (n/string node) "/"))))]
                         {:file source-file :row (:row (meta node)) :reason :namespace-sensitive-source})))
        builds (mapv
                (fn [d]
                  (let [selected (filterv #(= (:lib d) (get mapping (:name %))) owners)
                        selected-names (set (map :name selected))
                        local-sites (filterv #(selected-names (:owner %)) (get sites-by-file source-file))
                        used-libs (into (lexical-required-libs selected (libspecs original))
                                        (map :to-lib (mapcat #(get owner-usages (:name %)) selected)))
                        entries (filterv #(and (not= source-lib (lib-of %))
                                               (or (used-libs (lib-of %))
                                                   (empty? (options %))
                                                   (= :all (:refer (options %))))) (libspecs original))
                        needed (disj (set (keep #(get mapping (:var %)) local-sites)) (:lib d))
                        allocation (allocate entries needed dest-by-lib)
                        chosen (:chosen allocation)
                        entries (into entries (for [lib (sort needed) :let [a (get chosen lib)] :when a] [(symbol lib) :as (symbol a)]))
                        edits (for [s local-sites :let [target (get mapping (:var s))
                                                       text (if (= (:lib d) target) (:var s) (str (get chosen target) "/" (:var s)))]]
                                (assoc s :text text))
                        bodies (for [owner selected
                                     :let [body (splice (subs (:source original) (:start owner) (:end owner))
                                                        (for [e edits :when (= (:owner e) (:name owner))]
                                                          (-> e (update :start - (:start owner)) (update :end - (:start owner)))))
                                           body (if (contains? (set (map :form promotions)) (:name owner)) (promote-source body) body)]]
                                 (str (:prefix owner) body (:suffix owner)))
                        classes (set (for [u (:java-class-usages analysis)
                                           :when (and (= source-file (:filename u))
                                                      (selected-names (:name (owner-at original (:row u) (:col u)))))]
                                       (str (:class u))))
                        declares (sort (set (map :var (filter #(= (:lib d) (:from %)) forward))))
                        ns-source (header original (:lib d) entries classes)]
                    {:file (:file d) :lib (:lib d) :entries entries :classes (vec (sort classes))
                     :blockers (:blockers allocation)
                     :source (str ns-source "\n\n" (when (seq declares) (str "(declare " (str/join " " declares) ")\n"))
                                  (str/join "\n" bodies) "\n")})) dests)
        caller-builds (vec
                       (for [[file ss] (sort-by key sites-by-file) :when (not= file source-file)
                             :let [p (get parsed file)
                                   entries (filterv #(not= source-lib (lib-of %)) (libspecs p))
                                   ;; An explicit load-only dependency still loads the
                                   ;; complete partition after its source is retired.
                                   needed (if (seq ss) (set (map #(get mapping (:var %)) ss)) (set (keys dest-by-lib)))
                                   allocation (allocate entries needed dest-by-lib)
                                   chosen (:chosen allocation)
                                   added (vec (for [lib (sort needed) :let [a (get chosen lib)] :when a] [(symbol lib) :as (symbol a)]))
                                   entries (into entries added)
                                   [ns-start ns-end] (node-span (:starts p) (:ns-node p))
                                   edits (cons {:start ns-start :end ns-end :text (caller-header p source-lib added)}
                                               (for [s ss] (assoc s :text (str (get chosen (get mapping (:var s))) "/" (:var s)))))] ]
                         {:file file :lib (:lib p) :entries entries :sites (count ss) :blockers (:blockers allocation)
                          :source (splice (:source p) edits)}))
        all-builds (concat builds caller-builds)
        graph-edges (vec (sort (set (concat
                                    (for [b all-builds entry (:entries b)] [(:lib b) (lib-of entry)])
                                    (for [[file p] parsed :when (and (not= file source-file) (not (contains? sites-by-file file)))
                                          entry (libspecs p)] [(:lib p) (lib-of entry)])))))
        cycles (sccs graph-edges)
        rules (vec (for [edge (get-in request [:constraints :forbidden_edges]) :when ((set graph-edges) edge)]
                     {:type :architecture-violation :edge edge}))
        blockers (into blocks (concat (mapcat :blockers all-builds)
                                      (for [b caller-builds :when (not (str/ends-with? (:file b) ".clj"))]
                                        {:type :unsupported-caller-dialect :file (:file b)})
                                      (when (seq cycles) [{:type :cycle :sccs cycles}]) rules
                                      (when (seq unknowns) [{:type :unsupported-analysis :unknowns unknowns}])))
        futures (into (sorted-map source-file (when (= "retain-empty" (:source_retirement request)) (str "(ns " source-lib ")\n")))
                      (map (juxt :file :source)) all-builds)
        counts {:destinations (count dests) :forms (count owners) :caller_files (count caller-builds)
                :caller_sites (reduce + 0 (map :sites caller-builds)) :files (count futures)}
        projection {:snapshot_hash snapshot :map_hash (lens/source-hash (pr-str dests))
                    :destination_libs (vec (sort (map :lib dests))) :counts counts
                    :edges edges :projected_ns_graph {:edges graph-edges :acyclic (empty? cycles) :unknown_count (count unknowns)}
                    :promotions promotions :forward_reference_groups forward
                    :external_requires (mapv #(select-keys % [:lib :entries :classes]) builds)
                    :caller_inventory (mapv #(select-keys % [:file :lib :sites]) caller-builds)
                    :unmapped_owners unmapped :duplicate_owners duplicates :cycle_sccs cycles
                    :rule_violations rules :unknowns unknowns :blockers blockers
                    :coverage {:roots (:roots request) :reference_authority "clj-kondo captured snapshot plus structural quoted-Var supplement"
                               :dynamic_references "not claimed"}}]
    {:ok (empty? blockers) :operation :compiled-extraction :blockers blockers :projection projection
     :form-count (count owners) :caller-edit-count (:caller_sites counts)
     :original-sources (select-keys sources (cons source-file (map :file caller-builds)))
     :guard-sources sources :future-sources futures :created-files (mapv :file dests)
     :deleted-files (if (= "delete" (:source_retirement request)) [source-file] []) :created-directories []}))

;; @spec NS-SPLIT-011
(defn analysis-projection [compiled] (:projection compiled))

;; @spec NS-SPLIT-012
(defn receipt [compiled checks]
  (let [p (:projection compiled)]
    {:ok (:ok compiled) :operation "namespace_split"
     :state (if (:ok compiled) "planned" "refused") :committed false :mutation_attempted false
     :source_retired false :counts (:counts p) :destination_libs (:destination_libs p)
     :snapshot_hash (:snapshot_hash p) :map_hash (:map_hash p)
     :promotions (mapv #(-> % (dissoc :callers) (assoc :reference_count (count (:callers %)))) (:promotions p))
     :graph (:projected_ns_graph p) :coverage (:coverage p) :blockers (:blockers compiled)
     :checks checks :verification_complete false :next_call nil}))
