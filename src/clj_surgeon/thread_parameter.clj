(ns clj-surgeon.thread-parameter
  "Pure, Babashka-safe planner for threading one new parameter through every
  direct caller of a Var. The planner reads frozen source strings and returns
  whole-form edits; it performs no I/O."
  (:require
   [clj-surgeon.alias-migration :as alias-migration]
   [clojure.string :as str]
   [rewrite-clj.node :as n]
   [rewrite-clj.parser :as parser]))

;; ---------------------------------------------------------------------------
;; Source structure

(defn- meaningful?
  [node]
  (not (contains? #{:whitespace :newline :comma :comment :uneval} (n/tag node))))

(defn- children
  [node]
  (if (n/inner? node) (vec (n/children node)) []))

(defn- meaningful-children
  [node]
  (filterv meaningful? (children node)))

(defn- token-symbol
  [node]
  (when (= :token (n/tag node))
    (let [value (try (n/sexpr node) (catch Exception _ ::unreadable))]
      (when (symbol? value) value))))

(defn- token-text
  [node]
  (when (= :token (n/tag node)) (n/string node)))

(defn- unmeta-node
  [node]
  (if (= :meta (n/tag node))
    (recur (last (meaningful-children node)))
    node))

(defn- simple-name
  [node]
  (when-let [value (token-symbol (unmeta-node node))]
    (when-not (namespace value) (name value))))

(defn- head-name
  [node]
  (when (= :list (n/tag node))
    (some-> (first (meaningful-children node)) token-symbol name)))

(defn- top-level-forms
  [root]
  (filterv meaningful? (children root)))

(defn- ns-node
  [root]
  (first (filter #(= "ns" (head-name %)) (top-level-forms root))))

(defn- clause?
  [node keyword-text]
  (and (= :list (n/tag node))
       (= keyword-text (some-> (first (meaningful-children node)) token-text))))

(defn- require-clause
  [ns-form]
  (first (filter #(clause? % ":require") (meaningful-children ns-form))))

(defn- refer-name
  [node]
  (simple-name node))

(defn- libspec-facts
  [node]
  (case (n/tag node)
    :token
    (when-let [lib (token-symbol node)]
      {:lib (name lib) :aliases [] :referred #{} :refer-all? false})

    :vector
    (let [parts (filterv meaningful? (children node))
          lib (some-> (first parts) token-symbol name)]
      (when lib
        (loop [remaining (rest parts) aliases [] referred #{} refer-all? false]
          (if-let [option (first remaining)]
            (let [option-text (token-text option)
                  value (second remaining)]
              (cond
                (contains? #{":as" ":as-alias"} option-text)
                (recur (drop 2 remaining)
                       (cond-> aliases
                         (some-> value token-symbol) (conj (name (token-symbol value))))
                       referred refer-all?)

                (= ":refer" option-text)
                (if (= ":all" (token-text value))
                  (recur (drop 2 remaining) aliases referred true)
                  (recur (drop 2 remaining) aliases
                         (if (= :vector (n/tag (unmeta-node value)))
                           (into referred (keep refer-name)
                                 (meaningful-children (unmeta-node value)))
                           referred)
                         refer-all?))

                :else
                (recur (rest remaining) aliases referred refer-all?)))
            {:lib lib :aliases aliases :referred referred
             :refer-all? refer-all?}))))
    nil))

(defn- namespace-facts
  [root owner-ns target-var]
  (let [ns-form (ns-node root)
        direct (if-let [clause (some-> ns-form require-clause)]
                 (filterv map? (map libspec-facts
                                    (rest (meaningful-children clause))))
                 [])
        target (first (filter #(= owner-ns (:lib %)) direct))
        bare-providers (filterv #(and (not= owner-ns (:lib %))
                                   (or (:refer-all? %)
                                       (contains? (:referred %) target-var)))
                                direct)
        declared (alias-migration/ns-declared-name root)]
    {:declared declared
     :qualifiers (into #{owner-ns} (:aliases target))
     :bare? (or (= owner-ns declared)
                (:refer-all? target)
                (contains? (:referred target #{}) target-var))
     ;; Reuse the repository's ns-binding authority. A competing referred
     ;; spelling makes a bare target ambiguous and therefore fail-closed.
     :bound (alias-migration/ns-bound-names direct)
     :ambiguous-bare? (seq bare-providers)}))

(def ^:private definition-heads
  #{"def" "defn" "defn-" "defmacro" "defonce" "declare"})

(defn- top-level-lines
  [root]
  (loop [remaining (children root) line 1 result []]
    (if-let [node (first remaining)]
      (let [text (n/string node)]
        (recur (rest remaining)
               (+ line (count (filter #(= \newline %) text)))
               (cond-> result (meaningful? node) (conj {:node node :line line}))))
      result)))

(defn- owner-facts
  [root target-var]
  (into []
        (keep (fn [{:keys [node line]}]
                (let [head (head-name node)
                      name (some-> (nth (meaningful-children node) 1 nil)
                                   simple-name)]
                  (when (and (contains? definition-heads head)
                             (= target-var name))
                    {:node node :line line :kind head :name name}))))
        (top-level-lines root)))

(defn- arity-list?
  [node]
  (and (= :list (n/tag node))
       (= :vector (some-> (first (meaningful-children node)) unmeta-node n/tag))))

(defn- parameter-shape
  [owner]
  (let [parts (meaningful-children (:node owner))
        after-name (drop 2 parts)
        direct (first (filter #(= :vector (n/tag (unmeta-node %))) after-name))
        arities (filterv arity-list? after-name)]
    {:parameter-node (some-> direct unmeta-node)
     :multi-arity? (seq arities)}))

(defn- replace-child
  [parent old-node new-node]
  (n/replace-children parent
                      (mapv #(if (identical? % old-node) new-node %) (children parent))))

(defn- append-before-trailing-trivia
  [parent appended-node]
  (let [kids (children parent)
        last-meaningful (last (keep-indexed #(when (meaningful? %2) %1) kids))
        insertion (if (some? last-meaningful) (inc last-meaningful) 0)]
    (n/replace-children
      parent
      (vec (concat (subvec kids 0 insertion)
                   (when (some? last-meaningful) [(n/spaces 1)])
                   [appended-node]
                   (subvec kids insertion))))))

(defn- add-owner-parameter
  [owner parameter-node parameter-name]
  (replace-child (:node owner) parameter-node
                 (append-before-trailing-trivia
                   parameter-node (n/token-node (symbol parameter-name)))))

;; ---------------------------------------------------------------------------
;; Reference discovery through alias-migration's lexical binding walker

(def ^:private marker-symbol
  'clj-surgeon.thread-parameter.marker/TARGET-71c6d2)

(defn- target-decision
  [node {:keys [qualifiers target-var]} live-bare]
  (if-let [value (token-symbol node)]
    (let [qualifier (namespace value)
          var-name (name value)]
      (if (and (= target-var var-name)
               (if qualifier
                 (contains? qualifiers qualifier)
                 (contains? live-bare var-name)))
        {:rewrite (str marker-symbol)}
        {}))
    {}))

(defn- marker?
  [node]
  (= marker-symbol (token-symbol node)))

(defn- merge-walk-results
  [results]
  {:nodes (mapv :node results)
   :calls (reduce + 0 (map :calls results))
   :target? (boolean (some :target? results))})

(declare thread-pair)

(defn- thread-children
  [original marked default-node ignored-indexes]
  (let [original-kids (children original)
        marked-kids (children marked)]
    (merge-walk-results
      (mapv (fn [index original-child marked-child]
              (if (contains? ignored-indexes index)
                {:node original-child :calls 0 :target? false}
                (thread-pair original-child marked-child default-node)))
            (range (count original-kids)) original-kids marked-kids))))

(defn- thread-pair
  ([original marked default-node]
   (thread-pair original marked default-node #{}))
  ([original marked default-node ignored-indexes]
   (cond
     (marker? marked)
     {:node original :calls 0 :target? true}

     (and (= :list (n/tag marked))
          (let [head (first (meaningful-children marked))]
            (and head (marker? head))))
     (let [marked-kids (children marked)
           head-index (first (keep-indexed #(when (and (meaningful? %2)
                                                       (marker? %2)) %1)
                                           marked-kids))
           threaded (thread-children original marked default-node #{head-index})
           rebuilt (n/replace-children original (:nodes threaded))]
       {:node (append-before-trailing-trivia rebuilt default-node)
        :calls (inc (:calls threaded))
        :target? (:target? threaded)})

     (n/inner? marked)
     (let [threaded (thread-children original marked default-node ignored-indexes)]
       {:node (n/replace-children original (:nodes threaded))
        :calls (:calls threaded)
        :target? (:target? threaded)})

     :else
     {:node original :calls 0 :target? false})))

(defn- owner-name-index
  [node]
  (let [meaningful-indexes (keep-indexed #(when (meaningful? %2) %1) (children node))]
    (second meaningful-indexes)))

(defn- analyze-form
  [form {:keys [file owner-form? default-node] :as context}]
  (let [live-bare (if (:bare? context) #{(:target-var context)} #{})
        walked (alias-migration/rewrite-forms
                 form
                 {:decide target-decision
                  :qualifiers (:qualifiers context)
                  :target-var (:target-var context)
                  :platform (if (str/ends-with? file ".cljs") ":cljs" ":clj")}
                 live-bare)
        threaded (thread-pair form (:node walked) default-node
                              (if owner-form? #{(owner-name-index form)} #{}))]
    {:node (:node threaded)
     :calls (:calls threaded)
     :indirect (or (first (:indirect walked))
                   (when (:target? threaded) {:reason :first-class-use}))}))

;; ---------------------------------------------------------------------------
;; Scope and result shapes

(defn- glob-pattern
  [glob]
  (let [escaped (str/replace glob #"[.+^$(){}\[\]|\\]" "\\\\$0")
        expanded (str/replace escaped #"\*\*|\*"
                              (fn [match] (if (= "**" match) ".*" "[^/]*")))]
    (re-pattern (str "^" expanded "$"))))

(defn- in-scope?
  [paths file]
  (or (empty? paths)
      (boolean (some #(re-matches (glob-pattern %) file) paths))))

(defn- refusal
  [suffix message evidence]
  (merge {:ok false
          :operation "thread_parameter"
          :error_type (str "thread-parameter-" suffix)
          :error message
          :source_unchanged true
          :mutation_attempted false
          :next_call nil}
         evidence))

(defn- ambiguous-owner
  [file target-var owners]
  (refusal "ambiguous-owner"
           (str "Expected exactly one defn owner for " target-var " in " file ".")
           {:file file :var target-var
            :owners (mapv #(select-keys % [:kind :line]) owners)}))

(defn- analyze-file
  [{:keys [file source]} {:keys [owner-file owner-ns target-var
                                 owner-with-parameter default-node]}]
  (let [root (parser/parse-string-all source)
        facts (namespace-facts root owner-ns target-var)
        owner-file? (= file owner-file)
        forms (remove #(identical? % (ns-node root)) (top-level-forms root))]
    (loop [remaining forms edits [] calls 0]
      (if-let [form (first remaining)]
        (let [owner-form? (and owner-file?
                               (contains? #{"defn" "defn-"} (head-name form))
                               (= target-var
                                  (some-> (nth (meaningful-children form) 1 nil)
                                          simple-name)))
              input-form (if owner-form? owner-with-parameter form)
              result (analyze-form input-form
                                   (assoc facts
                                          :file file
                                          :target-var target-var
                                          :owner-form? owner-form?
                                          :default-node default-node))]
          (cond
            (:ambiguous-bare? facts)
            {:refusal (ambiguous-owner file target-var [])}

            (:indirect result)
            {:refusal
             (refusal "indirect-reference"
                      (str file " uses " target-var
                           " outside direct call position; v1 never guesses.")
                      {:file file
                       :var target-var
                       :reason (name (:reason (:indirect result)))
                       :form (n/string form)})}

            :else
            (let [changed? (or owner-form? (pos? (:calls result)))]
              (recur (rest remaining)
                     (cond-> edits
                       changed? (conj {:original (n/string form)
                                       :replacement (n/string (:node result))}))
                     (+ calls (:calls result))))))
        {:file file :edits edits :calls calls}))))

;; @spec PROTOTYPE-THREAD-PARAMETER-001
(defn plan
  "Return a complete pure rewrite plan or one typed refusal.

  Input:
    {:request {:from {:file relative-path :var string}
               :param {:name string :default source :position :last}
               :scope {:paths [glob]}}
     :sources [{:file relative-path :source source-text}]}

  For compatibility with other pure planners, the function also accepts the
  request map and sources vector as two arguments."
  ([input]
   (plan (:request input) (:sources input)))
  ([request sources]
   (let [request (or (:request request) request)
         owner-file (get-in request [:from :file])
         target-var (get-in request [:from :var])
         parameter-name (get-in request [:param :name])
         default-source (get-in request [:param :default])
         scope-paths (vec (get-in request [:scope :paths]))
         owner-entries (filterv #(= owner-file (:file %)) sources)]
     (if (not= 1 (count owner-entries))
       (ambiguous-owner owner-file target-var [])
       (let [owner-entry (first owner-entries)
             root (parser/parse-string-all (:source owner-entry))
             owner-ns (alias-migration/ns-declared-name root)
             owners (owner-facts root target-var)]
         (if (or (nil? owner-ns)
                 (not= 1 (count owners))
                 (not (contains? #{"defn" "defn-"} (:kind (first owners)))))
           (ambiguous-owner owner-file target-var owners)
           (let [owner (first owners)
                 {:keys [parameter-node multi-arity?]} (parameter-shape owner)]
             (if (or multi-arity? (nil? parameter-node))
               (refusal "multi-arity-unsupported"
                        (str owner-ns "/" target-var
                             " is not a supported single-arity defn.")
                        {:file owner-file :var target-var})
               (let [default-node (parser/parse-string default-source)
                     owner-with-parameter
                     (add-owner-parameter owner parameter-node parameter-name)
                     context {:owner-file owner-file
                              :owner owner
                              :owner-ns owner-ns
                              :target-var target-var
                              :owner-with-parameter owner-with-parameter
                              :default-node default-node}
                     analyses (reduce
                                (fn [state entry]
                                  (if (:refusal state)
                                    state
                                    (let [result (analyze-file entry context)]
                                      (if (:refusal result)
                                        {:refusal (:refusal result)}
                                        (update state :files conj result)))))
                                {:files []}
                                sources)]
                 (if (:refusal analyses)
                   (:refusal analyses)
                   (let [outside (->> (:files analyses)
                                      (filter #(seq (:edits %)))
                                      (remove #(in-scope? scope-paths (:file %)))
                                      (map :file)
                                      sort vec)]
                     (if (seq outside)
                       (refusal "caller-outside-scope"
                                "One or more required edits are outside scope.paths."
                                {:files_outside_scope outside})
                       (let [changed (filterv #(seq (:edits %)) (:files analyses))
                             caller-files (count (filter #(and (not= owner-file (:file %))
                                                            (pos? (:calls %)))
                                                         changed))
                             call-sites (reduce + 0 (map :calls changed))
                             edit-count (reduce + 0 (map #(count (:edits %)) changed))]
                         {:ok true
                          :plan {:files (mapv #(select-keys % [:file :edits]) changed)}
                          :receipt {:owner-files 1
                                    :caller-files caller-files
                                    :call-sites call-sites
                                    :changed-files (count changed)
                                    :edits edit-count}})))))))))))))
