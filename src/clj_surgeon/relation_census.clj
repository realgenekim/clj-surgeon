(ns clj-surgeon.relation-census
  "Pure census of collection writes inside event-fold arms.

   The census LOCATES review work. It does not prove idempotency and it is not
   an enforcement gate: `:raw` means \"no recognised guard dominates this
   write\", `:unknown` means \"this analyzer cannot decide\", and every site
   carries the evidence a reviewer needs to judge it.

   This namespace is pure and babashka-compatible. Parallelism is injected by
   the caller as `:map-fn`; it changes elapsed time and never the answer."
  (:require
   [clojure.set :as set]
   [clojure.string :as str]
   [rewrite-clj.node :as node]
   [rewrite-clj.parser :as parser]))

(def census-version 1)

;; ---------------------------------------------------------------------------
;; Shared bounds: one kernel for both entrances (MCP tool and CLI op)
;; ---------------------------------------------------------------------------

(def max-pool-size
  "The largest plan-phase pool a caller may ask for."
  64)

(def max-requested-files
  "The largest explicit file list a caller may pass."
  512)

(def max-doors
  "The largest identity-door list a caller may pass."
  32)

(def max-source-bytes
  "A single source larger than this is not censused."
  (* 2 1024 1024))

(def max-scanned-files
  "Discovery stops after this many candidate sources."
  4000)

(def skipped-directories
  "Directories both entrances prune before reading them."
  #{".git" "node_modules" "target" ".cpcache" ".clj-kondo" ".lsp" ".shadow-cljs"
    ".calva" "out" "dist" ".idea"})

(def max-listed-files
  "The largest number of file names either entrance lists in one receipt.

   The bound belongs here, next to the other shared bounds, because a receipt
   that lists twelve of twenty names is only honest if the entrance that
   listed them also says how many it left out — and both entrances must mean
   the same twelve."
  12)

(def source-name-pattern
  "The file names both entrances treat as candidate Clojure sources."
  #"\.clj[cs]?$")

;; @spec MCP-OP-CENSUS-016
(defn coerce-pool-size
  "Pure pool-size kernel shared by the MCP tool and the CLI op.

   Accepts an integer or its decimal digits (the CLI hands every value over as
   a string). Returns {:ok true :size n} or a typed reason."
  [value]
  (let [text (str/trim (str value))
        parsed (cond
                 (integer? value) (long value)
                 (re-matches #"\d{1,9}" text) (parse-long text)
                 :else nil)]
    (cond
      (nil? parsed) {:ok false :reason :not-an-integer :value text}
      (or (< parsed 1) (> parsed max-pool-size))
      {:ok false :reason :out-of-range :value parsed :maximum max-pool-size}
      :else {:ok true :size parsed})))

;; @spec MCP-OP-CENSUS-016
(defn effective-pool-size
  "The pool a census may actually use: never more than the box has processors.

   The one function in this namespace that reads the runtime rather than its
   arguments; it changes elapsed time and never the answer."
  [requested]
  (max 1 (min (long requested) (.availableProcessors (Runtime/getRuntime)))))

(def default-doors
  "Identity doors: a write routed through one of these is already keyed."
  #{'conj-once 'cons-once 'upsert-by 'conj-distinct-by 'cons-distinct-by})

(def ^:private write-heads '#{conj cons into concat})

(def ^:private recognised-containers
  '#{let let* letfn loop do if if-not when when-not cond condp case
     if-let when-let if-some when-some -> ->> some-> some->> as->
     fn fn* defmethod update update-in update-vals swap! swap-vals!
     assoc assoc-in merge merge-with vec vector into set hash-map
     doto binding with-meta reduce})

(def ^:private recognised-test-heads
  '#{not and or = not= == some not-any? every? contains? get get-in seq empty?
     filter remove count zero? pos? neg? nil? some? boolean str keyword name
     fn fn* first second last nth peek identity complement partial ->})

;; ---------------------------------------------------------------------------
;; Node helpers
;; ---------------------------------------------------------------------------

(defn- inner? [n] (and (some? n) (node/inner? n)))

(defn- sig-children
  [n]
  (if (inner? n)
    (vec (remove node/whitespace-or-comment? (node/children n)))
    []))

(defn- call-node?
  [n]
  (and (inner? n) (contains? #{:list :fn} (node/tag n))))

(defn- token-sexpr
  [n]
  (when (and n (= :token (node/tag n)))
    (try (node/sexpr n) (catch Throwable _ nil))))

(defn- head-symbol
  [n]
  (when (call-node? n)
    (let [s (token-sexpr (first (sig-children n)))]
      (when (symbol? s) s))))

(defn- simple-name
  [s]
  (when (symbol? s) (symbol (name s))))

(defn- head-name
  [n]
  (simple-name (head-symbol n)))

(defn- line-of
  [n]
  (:row (meta n)))

(defn- source-line
  ([n] (source-line n 100))
  ([n limit]
   (let [text (-> (node/string n) (str/replace #"\s+" " ") str/trim)]
     (if (> (count text) limit)
       (str (subs text 0 (dec limit)) "…")
       text))))

(defn- safe-value
  [n]
  (try (node/sexpr n) (catch Throwable _ ::unreadable)))

(defn- node-seq
  [n]
  (tree-seq inner? sig-children n))

(defn- keyword-lookups
  "Keywords used as a lookup head: (:k x) and (get x :k)."
  [n]
  (when n
    (into #{}
          (comp
            (filter call-node?)
            (mapcat
              (fn [c]
                (let [kids (sig-children c)
                      k (token-sexpr (first kids))]
                  (cond
                    (keyword? k) [k]
                    (= 'get (simple-name k))
                    (keep #(let [v (token-sexpr %)] (when (keyword? v) v))
                          (rest kids))
                    :else nil)))))
          (node-seq n))))

(defn- all-keywords
  [n]
  (when n
    (into #{}
          (keep (fn [c] (let [v (token-sexpr c)] (when (keyword? v) v))))
          (node-seq n))))

;; ---------------------------------------------------------------------------
;; Stack: [{:node parent :index child-index-in-sig-children} ...]
;; ---------------------------------------------------------------------------

(defn- parent-node [stack] (:node (peek stack)))
(defn- index-in-parent [stack] (:index (peek stack)))

(defn- ancestor-frames
  "Outermost-first frames {:node :parent :index} for every stack entry."
  [stack]
  (mapv (fn [d]
          (let [entry (nth stack d)]
            {:node (:node entry)
             :parent (when (pos? d) (:node (nth stack (dec d))))
             :index (when (pos? d) (:index (nth stack (dec d))))
             :depth d
             :child-index (:index entry)}))
        (range (count stack))))

;; ---------------------------------------------------------------------------
;; Threading-aware argument lists
;; ---------------------------------------------------------------------------

(defn- form-args
  "Arg nodes of one call, with the implicit `->` threaded argument prepended.

   Returns nil when the threading shape is not supported."
  [n stack]
  (let [args (vec (rest (sig-children n)))
        p (parent-node stack)
        ph (head-name p)]
    (cond
      (nil? p) args
      (= '-> ph) (let [i (index-in-parent stack)
                       kids (sig-children p)]
                   (cond
                     (= i 2) (into [(nth kids 1)] args)
                     (< i 2) args
                     :else nil))
      (contains? '#{->> some-> some->> as->} ph) nil
      :else args)))

;; ---------------------------------------------------------------------------
;; Update-form shapes
;; ---------------------------------------------------------------------------

(defn- update-shape
  "Describe `(update m k f …)`, `(update-in m path f …)`, `(swap! a f …)`.

   Returns {:kind :base :path-nodes :fn-node :value-node} or nil."
  [n stack]
  (when-let [h (head-name n)]
    (when (contains? '#{update update-in swap!} h)
      (when-let [args (form-args n stack)]
        (case h
          update (when (>= (count args) 3)
                   {:kind :update :base (nth args 0)
                    :path-nodes [(nth args 1)] :fn-node (nth args 2)
                    :value-node (last args)})
          update-in (when (>= (count args) 3)
                      (let [pv (nth args 1)]
                        (when (= :vector (node/tag pv))
                          {:kind :update-in :base (nth args 0)
                           :path-nodes (sig-children pv) :fn-node (nth args 2)
                           :value-node (last args)})))
          swap! (when (>= (count args) 2)
                  {:kind :swap! :base (nth args 0)
                   :path-nodes [] :fn-node (nth args 1)
                   :value-node (last args)}))))))

(defn- update-fn-position?
  "True when the node at `stack`'s tip is the update fn of an update form."
  [stack]
  (let [p (parent-node stack)]
    (when-let [shape (update-shape p (pop stack))]
      (let [kids (sig-children p)
            fn-index (first (keep-indexed
                              (fn [i c] (when (identical? c (:fn-node shape)) i))
                              kids))]
        (= fn-index (index-in-parent stack))))))

;; ---------------------------------------------------------------------------
;; Target resolution
;; ---------------------------------------------------------------------------

(declare resolve-target)

(defn- let-init
  "Innermost single-assignment let/loop init node for symbol `sym`."
  [sym stack]
  (->> (ancestor-frames stack)
       reverse
       (some (fn [{:keys [node]}]
               (when (contains? '#{let let* loop if-let when-let if-some when-some}
                                (head-name node))
                 (let [bv (second (sig-children node))]
                   (when (and bv (= :vector (node/tag bv)))
                     (let [pairs (partition 2 (sig-children bv))
                           hits (filter #(= sym (token-sexpr (first %))) pairs)]
                       (when (= 1 (count hits))
                         (second (first hits)))))))))))

(defn- fn-param-source
  "When `sym` is the parameter of an enclosing (fn [sym] …) that is itself the
   update fn of an outer update form, return that outer update form + stack."
  [sym stack]
  (->> (ancestor-frames stack)
       reverse
       (some (fn [{:keys [node parent depth]}]
               (when (and parent (contains? '#{fn fn*} (head-name node)))
                 (let [pv (second (sig-children node))]
                   (when (and pv (= :vector (node/tag pv))
                              (some #(= sym (token-sexpr %)) (sig-children pv)))
                     (let [outer-stack (vec (take (dec depth) stack))]
                       (when (update-fn-position? (vec (take depth stack)))
                         {:form parent :stack outer-stack})))))))))

(defn- resolve-target
  "Resolve one collection expression to {:root string :path [sexprs]}."
  [n stack depth]
  (when (and n (< depth 8))
    (let [h (head-name n)]
      (cond
        (and h (= 'get-in h))
        (let [args (form-args n stack)
              pv (second args)]
          (when (and pv (= :vector (node/tag pv)))
            (when-let [base (resolve-target (first args) stack (inc depth))]
              (update base :path into (map safe-value (sig-children pv))))))

        (and h (= 'get h))
        (let [args (form-args n stack)]
          (when (= 2 (count args))
            (when-let [base (resolve-target (first args) stack (inc depth))]
              (update base :path conj (safe-value (second args))))))

        (and (= :token (node/tag n)) (symbol? (token-sexpr n)))
        (let [sym (token-sexpr n)]
          (if-let [init (let-init sym stack)]
            (resolve-target init stack (inc depth))
            (if-let [{:keys [form stack]} (fn-param-source sym stack)]
              (when-let [shape (update-shape form stack)]
                (when-let [base (resolve-target (:base shape) stack (inc depth))]
                  (update base :path into (map safe-value (:path-nodes shape)))))
              {:root (str sym) :path []})))

        :else nil))))

(defn- update-form-target
  [form stack]
  (when-let [shape (update-shape form stack)]
    (when-let [base (resolve-target (:base shape) stack 0)]
      (update base :path into (map safe-value (:path-nodes shape))))))

(defn- target-string
  [{:keys [root path]}]
  (str root " " (pr-str (vec path))))

;; ---------------------------------------------------------------------------
;; Sites
;; ---------------------------------------------------------------------------

(defn- fnil-write?
  [n]
  (and (= 'fnil (head-name n))
       (let [kids (sig-children n)]
         (contains? write-heads (simple-name (token-sexpr (second kids)))))))

;; @spec MCP-OP-CENSUS-001
(defn- site-kind
  [n stack doors]
  (cond
    (and (call-node? n) (contains? doors (head-name n))) :door-call
    (and (call-node? n) (contains? write-heads (head-name n))) :write-call
    (and (call-node? n) (fnil-write? n) (update-fn-position? stack)) :fnil-update-fn
    (and (= :token (node/tag n))
         (let [s (simple-name (token-sexpr n))]
           (and s (or (contains? write-heads s) (contains? doors s))))
         (update-fn-position? stack))
    (if (contains? doors (simple-name (token-sexpr n))) :door-update-fn :write-update-fn)
    :else nil))

(defn- collect-sites
  [root doors]
  (letfn [(go [n stack acc]
            (let [kind (when (seq stack) (site-kind n stack doors))
                  acc (if kind (conj acc {:node n :stack stack :kind kind}) acc)]
              (if (inner? n)
                (reduce (fn [a [i c]] (go c (conj stack {:node n :index i}) a))
                        acc
                        (map-indexed vector (sig-children n)))
                acc)))]
    (go root [] [])))

(defn- written-value-node
  [{:keys [node stack kind]}]
  (case kind
    (:fnil-update-fn :write-update-fn :door-update-fn)
    (:value-node (update-shape (parent-node stack) (pop stack)))

    (:write-call :door-call)
    (let [args (vec (rest (sig-children node)))
          h (head-name node)]
      (when (seq args)
        (if (= 'cons h) (first args) (last args))))

    nil))

(defn- target-collection
  [{:keys [node stack kind]}]
  (case kind
    (:fnil-update-fn :write-update-fn :door-update-fn)
    (update-form-target (parent-node stack) (pop stack))

    (:write-call :door-call)
    (let [args (vec (rest (sig-children node)))
          h (head-name node)
          coll (cond
                 (= 'cons h) (second args)
                 (contains? #{:door-call} kind) (when (>= (count args) 2)
                                                  (nth args (- (count args) 2)))
                 :else (first args))]
      (when coll (resolve-target coll stack 0)))

    nil))

(defn- set-target?
  [{:keys [node stack kind]}]
  (let [set-node? (fn [n] (and n (= :set (node/tag n))))]
    (case kind
      :fnil-update-fn (boolean (some set-node? (drop 2 (sig-children node))))
      :write-call (boolean (some set-node? (rest (sig-children node))))
      :write-update-fn (let [shape (update-shape (parent-node stack) (pop stack))]
                         (boolean (some set-node? (:path-nodes shape))))
      false)))

;; ---------------------------------------------------------------------------
;; Guards
;; ---------------------------------------------------------------------------

(defn- guard-frame
  "Describe the dominating guard at one ancestor frame, or nil."
  [{:keys [node child-index]}]
  (let [h (head-name node)
        kids (sig-children node)]
    (case h
      (if if-not)
      (when (and (>= (count kids) 3) (contains? #{2 3} child-index))
        {:head h :test (nth kids 1) :branch (if (= 2 child-index) :then :else)
         :node node})

      (when when-not)
      (when (and (>= (count kids) 3) (>= child-index 2))
        {:head h :test (nth kids 1) :branch :then :node node})

      (if-let when-let if-some when-some)
      (let [bv (second kids)]
        (when (and bv (= :vector (node/tag bv))
                   (>= child-index 2)
                   (= 2 (count (sig-children bv))))
          {:head h :test (second (sig-children bv))
           :branch (if (= 2 child-index) :then :else) :node node}))

      cond
      (when (and (>= child-index 2) (even? child-index))
        (let [test (nth kids (dec child-index))]
          (when-not (= :else (token-sexpr test))
            {:head h :test test :branch :then :node node})))

      nil)))

(def ^:private idiom-senses
  '{some :present not-any? :absent every? :present contains? :present
    get :present get-in :present seq :present empty? :absent})

(defn- membership-idioms
  "Every recognised membership idiom occurrence inside one test node."
  [test]
  (keep
    (fn [c]
      (when (call-node? c)
        (let [h (head-name c)
              kids (sig-children c)
              args (vec (rest kids))]
          (cond
            (contains? '#{some not-any? every?} h)
            (when (= 2 (count args))
              {:node c :sense (idiom-senses h) :coll (second args) :pred (first args)})

            (= 'contains? h)
            (when (= 2 (count args))
              {:node c :sense :present :coll (first args) :pred (second args)})

            (contains? '#{get get-in} h)
            {:node c :sense :present :coll c :pred nil}

            (contains? '#{seq empty?} h)
            (let [inner (first args)]
              (when (and inner (= 'filter (head-name inner)))
                (let [iargs (vec (rest (sig-children inner)))]
                  (when (= 2 (count iargs))
                    {:node c :sense (idiom-senses h)
                     :coll (second iargs) :pred (first iargs)}))))

            (and (inner? c) (= :set (node/tag (first kids))))
            {:node c :sense :present :coll (first kids) :pred (second kids)}

            :else nil))))
    (node-seq test)))

(defn- flip [sense] (if (= :present sense) :absent :present))

(defn- negations-above
  "Count `not`/`nil?` wrappers between `idiom` and `test`."
  [test idiom]
  (letfn [(go [n]
            (cond
              (identical? n idiom) 0
              (not (inner? n)) nil
              :else (some (fn [c]
                            (when-let [d (go c)]
                              (+ d (if (contains? '#{not nil?} (head-name n)) 1 0))))
                          (sig-children n))))]
    (or (go test) 0)))

(defn- branch-sense
  [{:keys [head branch]} sense]
  (let [after-negated-head (if (contains? '#{if-not when-not} head) (flip sense) sense)]
    (if (= :else branch) (flip after-negated-head) after-negated-head)))

(defn- suspicious-helpers
  "Unrecognised heads inside a test that carry the write's identity or target."
  [test target written-kws stack]
  (into []
        (keep
          (fn [c]
            (when (call-node? c)
              (let [h (head-name c)]
                (when (and h
                           (not (contains? recognised-test-heads h))
                           (not (contains? idiom-senses h)))
                  (let [args (rest (sig-children c))
                        kws (keyword-lookups c)]
                    (when (or (seq (set/intersection kws (or written-kws #{})))
                              (some #(= target (resolve-target % stack 0)) args))
                      (str h))))))))
        (node-seq test)))

;; ---------------------------------------------------------------------------
;; Classification
;; ---------------------------------------------------------------------------

(defn- container-violation
  "The first ancestor head between the arm root and the site that this version
   does not understand."
  [stack]
  (->> (ancestor-frames stack)
       (drop 1)
       (some (fn [{:keys [node]}]
               (when (call-node? node)
                 (let [h (head-name node)]
                   (cond
                     (nil? h) nil
                     (contains? recognised-containers h) nil
                     :else (str h))))))))

;; @spec MCP-OP-CENSUS-003
;; @spec MCP-OP-CENSUS-004
;; @spec MCP-OP-CENSUS-005
;; @spec MCP-OP-CENSUS-006
;; @spec MCP-OP-CENSUS-007
;; @spec MCP-OP-CENSUS-008
;; @spec MCP-OP-CENSUS-009
(defn- classify-site
  [{:keys [node stack kind] :as site} arm]
  (let [line (line-of node)
        base {:file nil
              :line line
              :arm (:event-type arm)
              :write (source-line node)}]
    (cond
      (contains? #{:door-call :door-update-fn} kind)
      (assoc base :class :door :door (str (or (head-name node)
                                              (simple-name (token-sexpr node)))))

      (set-target? site)
      (assoc base :class :set)

      :else
      (let [target (target-collection site)
            value-node (written-value-node site)
            value-node (or (when (and value-node
                                      (= :token (node/tag value-node))
                                      (symbol? (token-sexpr value-node)))
                             (let-init (token-sexpr value-node) stack))
                           value-node)
            written-kws (all-keywords value-node)
            base (cond-> base
                   target (assoc :target (target-string target))
                   value-node (assoc :value (source-line value-node 60)))
            container (container-violation stack)
            frames (ancestor-frames stack)
            guards (keep guard-frame frames)
            evaluated
            (for [g guards
                  idiom (membership-idioms (:test g))
                  :let [sense (nth (iterate flip (:sense idiom))
                                   (negations-above (:test g) (:node idiom)))
                        effective (branch-sense g sense)
                        gt (resolve-target (:coll idiom) stack 0)
                        id-kws (keyword-lookups (:pred idiom))
                        id-kws (if (seq id-kws) id-kws (keyword-lookups (:coll idiom)))
                        shared (set/intersection (or id-kws #{}) (or written-kws #{}))]]
              {:guard g :idiom idiom :polarity effective
               :target-match (and target gt (= target gt))
               :identity-match (boolean (seq shared))
               :identity (when (seq shared) (pr-str (first (sort shared))))})
            qualifying (first (filter #(and (:target-match %) (:identity-match %)
                                            (= :absent (:polarity %)))
                                      evaluated))
            wrong-polarity (first (filter #(and (:target-match %) (:identity-match %))
                                          evaluated))
            helpers (mapcat #(suspicious-helpers (:test %) target written-kws stack)
                            guards)]
        (cond
          qualifying
          (assoc base :class :guarded
                 :guard (source-line (:node (:guard qualifying)) 80)
                 :guard-line (line-of (:node (:idiom qualifying)))
                 :identity (:identity qualifying)
                 :polarity :absent)

          container
          (assoc base :class :unknown :reason :unsupported-container
                 :detail container)

          wrong-polarity
          (assoc base :class :unknown :reason :polarity
                 :guard (source-line (:node (:guard wrong-polarity)) 80)
                 :guard-line (line-of (:node (:idiom wrong-polarity)))
                 :identity (:identity wrong-polarity)
                 :polarity (:polarity wrong-polarity))

          (seq helpers)
          (assoc base :class :unknown :reason :helper-mediated-guard
                 :detail (first helpers)
                 :guard-line (line-of (:node (first guards))))

          (nil? target)
          (assoc base :class :unknown :reason :unresolved-target)

          :else
          (assoc base :class :raw))))))

;; ---------------------------------------------------------------------------
;; Calls this version does not model
;; ---------------------------------------------------------------------------

(def ^:private analyzed-heads
  "Call heads the census reasons about. Everything else inside an arm is a call
   whose effect on the state this version cannot see."
  (set/union recognised-containers
             recognised-test-heads
             write-heads
             '#{defmethod fnil}))

;; @spec MCP-OP-CENSUS-025
(defn- unrecognised-calls
  "Calls inside one arm whose head this census version does not model.

   The census reports `raw 0` when it finds no site at all, and a write hidden
   behind an ordinary helper produces exactly that: no site, no raw, a clean
   next_action. These are the calls that could be hiding one."
  [arm-node event-type doors]
  (into []
        (keep (fn [n]
                (when-let [h (head-name n)]
                  (when-not (or (contains? analyzed-heads h)
                                (contains? doors h))
                    {:call (str h) :line (line-of n) :arm event-type}))))
        (node-seq arm-node)))

;; ---------------------------------------------------------------------------
;; File census
;; ---------------------------------------------------------------------------

(defn- arm?
  [n multi]
  (and (call-node? n)
       (= 'defmethod (head-name n))
       (= multi (simple-name (token-sexpr (second (sig-children n)))))))

(defn- arm-of
  [n]
  {:event-type (let [v (safe-value (nth (sig-children n) 2 nil))]
                 (if (= ::unreadable v) "?" (str v)))
   :line (line-of n)
   :node n})

(defn- declared-names
  [forms]
  (into #{}
        (keep (fn [n]
                (when (and (call-node? n)
                           (contains? '#{def defn defn- defmacro} (head-name n)))
                  (simple-name (token-sexpr (second (sig-children n)))))))
        forms))

(def empty-counts {:door 0 :set 0 :guarded 0 :raw 0 :unknown 0})

;; @spec MCP-OP-CENSUS-002
(defn census-file
  "Census one source string. Pure: text in, data out."
  [{:keys [file source doors multi]
    :or {doors default-doors multi 'fold-event}}]
  (let [parsed (try {:ok true :node (parser/parse-string-all source)}
                    (catch Throwable e {:ok false :message (.getMessage e)}))]
    (if-not (:ok parsed)
      {:ok false :error-type :unparseable-file :file file
       :error (str "Could not parse " file ": " (:message parsed))}
      (let [forms (sig-children (:node parsed))
            arm-nodes (filterv #(arm? % multi) forms)
            other-nodes (filterv #(not (arm? % multi)) forms)
            sites (into []
                        (mapcat
                          (fn [an]
                            (let [arm (arm-of an)
                                  body (drop 4 (sig-children an))]
                              (->> body
                                   (mapcat #(collect-sites % doors))
                                   (map #(assoc (classify-site % arm) :file file))))))
                        arm-nodes)
            outside (reduce + 0 (map #(count (collect-sites % doors)) other-nodes))
            unmodelled (into []
                             (mapcat
                               (fn [an]
                                 (map #(assoc % :file file)
                                      (unrecognised-calls
                                        an (:event-type (arm-of an)) doors))))
                             arm-nodes)]
        {:ok true
         :file file
         :arms (count arm-nodes)
         :arm-types (mapv #(:event-type (arm-of %)) arm-nodes)
         :declared (declared-names forms)
         :sites sites
         :unrecognised unmodelled
         :outside-arms outside
         :counts (merge empty-counts (frequencies (map :class sites)))}))))

(defn source-declared-names
  "Top-level `def`/`defn`/`defn-`/`defmacro` names in one source.

   A door may be defined in a helper namespace that defines no arms, so door
   validation needs the names of every scanned file, not only the censused
   ones. Returns the empty set for a source that cannot be parsed: an
   unparseable file is the census's refusal to report, not this predicate's."
  [source]
  (try
    (declared-names (sig-children (parser/parse-string-all source)))
    (catch Throwable _ #{})))

(defn defines-arms?
  "Cheap discovery predicate: does this source define arms of `multi`?"
  ([source] (defines-arms? source 'fold-event))
  ([source multi]
   (boolean (re-find (re-pattern (str "\\(defmethod\\s+(?:[\\w.$-]+/)?"
                                      (java.util.regex.Pattern/quote (str multi))
                                      "\\s"))
                     source))))

;; @spec MCP-OP-CENSUS-019
(defn parse-doors
  "Validate a caller's identity doors against the census vocabulary.

   One kernel for both entrances: the MCP tool and the CLI op. Returns the door
   set, or a map naming the offending value and why it was refused. `declared`
   is the set of names defined in the scanned files, or nil to skip that check."
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
               (not (contains? default-doors (symbol (name sym))))
               (not (contains? declared (symbol (name sym)))))
          (reduced {:invalid (str value)
                    :why "not defined in any scanned file"})

          :else (conj acc (symbol (name sym))))))
    #{}
    doors))

;; @spec MCP-OP-CENSUS-025
(defn unrecognised-summary
  "The count of unmodelled calls inside arms, with up to `limit` named examples.

   Shared by both entrances so the tool and the CLI report the same thing."
  [unrecognised limit]
  (when (seq unrecognised)
    {:count (count unrecognised)
     :examples (->> (sort-by (juxt :file :line) unrecognised)
                    (reduce (fn [{:keys [seen out] :as acc} call]
                              (cond
                                (>= (count out) limit)
                                (reduced acc)

                                (contains? seen (:call call))
                                acc

                                :else {:seen (conj seen (:call call))
                                       :out (conj out call)}))
                            {:seen #{} :out []})
                    :out
                    (mapv #(select-keys % [:call :file :line :arm])))}))

(defn merge-results
  "Merge per-file results, re-keyed by path. Order is by path, always."
  [results]
  (let [ordered (vec (sort-by :file results))]
    {:by-file (into (sorted-map)
                    (map (fn [r] [(:file r) (-> (select-keys r [:arms :outside-arms :counts])
                                                (assoc :sites (count (:sites r))))]))
                    ordered)
     :files (count ordered)
     :arms (reduce + 0 (map :arms ordered))
     :sites (reduce + 0 (map #(count (:sites %)) ordered))
     :outside-arms (reduce + 0 (map :outside-arms ordered))
     :counts (apply merge-with + empty-counts (map :counts ordered))
     :all-sites (vec (mapcat :sites ordered))
     :unrecognised (vec (mapcat :unrecognised ordered))
     :declared (reduce into #{} (map :declared ordered))}))

;; @spec MCP-OP-CENSUS-012
(defn census-input
  "One worker unit. Any throw becomes a typed per-file refusal naming the file,
   so a pool worker can never publish a partial census."
  [{:keys [doors multi]} input]
  (try
    (census-file (assoc input :doors doors :multi multi))
    (catch Throwable e
      {:ok false
       :error-type :census-worker-failure
       :file (:file input)
       :error (str "Census worker failed on " (:file input) ": " (ex-message e))})))

;; @spec MCP-OP-CENSUS-011
(defn plan
  "Census many files. `map-fn` performs the parse+classify phase; it changes
   elapsed time and never the answer."
  [{:keys [inputs doors multi map-fn]
    :or {doors default-doors multi 'fold-event map-fn map}}]
  (let [t0 (System/nanoTime)
        results (vec (map-fn #(census-input {:doors doors :multi multi} %) inputs))
        t1 (System/nanoTime)
        failed (first (remove :ok results))]
    (if failed
      (assoc failed :phases {:classify (/ (- t1 t0) 1e6)})
      (let [merged (merge-results results)
            t2 (System/nanoTime)]
        (assoc merged
               :ok true
               :census-version census-version
               :phases {:classify (/ (- t1 t0) 1e6)
                        :merge (/ (- t2 t1) 1e6)})))))
