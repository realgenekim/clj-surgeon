(ns clj-surgeon.move
  "Dependency-aware movement of named top-level forms within one source file."
  (:require
   [clj-surgeon.analyze :as analyze]
   [clj-surgeon.file-ops :as file-ops]
   [clj-surgeon.forms :as forms]
   [clj-surgeon.structural-lens :as structural-lens]
   [clojure.set :as set]
   [clojure.string :as str]
   [rewrite-clj.node :as n]
   [rewrite-clj.zip :as z]))

(defn- form-name-of [zloc]
  (when (z/list? zloc)
    (when-let [name-zloc (some-> zloc z/down z/right)]
      (if (= :meta (n/tag (z/node name-zloc)))
        (some-> name-zloc z/down z/rightmost z/string)
        (z/string name-zloc)))))

(defn- top-level-locations [zloc]
  (->> (iterate z/right zloc) (take-while some?)))

(defn- comment-start [lines form-line]
  (loop [i (- form-line 2)]
    (cond
      (neg? i) 0
      (str/starts-with? (str/trim (nth lines i "")) ";") (recur (dec i))
      :else (inc i))))

(defn- form-entry [lines ordinal zloc]
  (when (z/list? zloc)
    (let [type (some-> zloc z/down z/string)
          node-meta (meta (z/node zloc))
          line (:row node-meta)
          sexpr (try (z/sexpr zloc) (catch Exception _ nil))]
      {:ordinal ordinal
       :type type
       :name (form-name-of zloc)
       :line line
       :end-line (:end-row node-meta)
       :start-index (comment-start lines line)
       :end-index (:end-row node-meta)
       :declared-names (when (= "declare" type)
                         (mapv str (rest sexpr)))})))

(defn- source-model [source]
  (let [zloc (z/of-string source {:track-position? true})
        lines (vec (str/split-lines source))
        entries (->> (top-level-locations zloc)
                     (map-indexed #(form-entry lines %1 %2))
                     (remove nil?)
                     vec)
        definitions (->> entries
                         (filter #(and (:name %)
                                       (not= "declare" (:type %))
                                       (forms/defining-form? (:type %))))
                         vec)
        by-name (group-by :name definitions)
        declarations (reduce (fn [result {:keys [ordinal line declared-names]}]
                               (reduce (fn [m declared]
                                         (update m declared (fnil conj [])
                                                 {:ordinal ordinal :line line}))
                                       result declared-names))
                             {} (filter :declared-names entries))]
    {:zloc zloc
     :lines lines
     :entries entries
     :definitions definitions
     :by-name by-name
     :declarations declarations}))

(defn- unique-entry [model form-name role]
  (let [matches (get-in model [:by-name (str form-name)] [])]
    (cond
      (empty? matches)
      {:error (str (if (= role :destination) "Destination form" "Form")
                   " not found: " form-name)
       :error-type (if (= role :destination)
                     :destination-form-not-found
                     :form-not-found)
       :form (str form-name)}

      (> (count matches) 1)
      {:error (str "Ambiguous form: " form-name)
       :error-type :ambiguous-form
       :form (str form-name)
       :match-count (count matches)
       :matches (mapv #(select-keys % [:type :line :end-line]) matches)}

      :else (first matches))))

(defn- dependency-map [model]
  (into {} (map (juxt :name :depends-on)
                (analyze/intra-ns-deps (:zloc model)))))

(defn- first-declaration-before [model dependency ordinal]
  (some #(when (< (:ordinal %) ordinal) %)
        (sort-by :ordinal (get-in model [:declarations dependency]))))

(defn- violations [model]
  (let [deps (dependency-map model)
        unique-defs (into {} (keep (fn [[name entries]]
                                     (when (= 1 (count entries))
                                       [name (first entries)]))
                                   (:by-name model)))]
    (->> deps
         (mapcat (fn [[user dependencies]]
                   (let [user-entry (get unique-defs user)]
                     (for [dependency dependencies
                           :let [dep-entry (get unique-defs dependency)]
                           :when (and user-entry dep-entry
                                      (not (< (:ordinal dep-entry)
                                              (:ordinal user-entry)))
                                      (nil? (first-declaration-before
                                              model dependency
                                              (:ordinal user-entry))))]
                       {:from user
                        :dependency dependency
                        :used-at (:line user-entry)
                        :defined-at (:line dep-entry)}))))
         (sort-by (juxt :used-at :defined-at :from :dependency))
         vec)))

(defn- violation-id [{:keys [from dependency]}]
  [from dependency])

(defn- new-violations [source-model candidate-model]
  (let [before (set (map violation-id (violations source-model)))]
    (->> (violations candidate-model)
         (remove #(contains? before (violation-id %)))
         vec)))

(defn- dependency-satisfied-at? [model dependency destination-ordinal]
  (let [definitions (get-in model [:by-name dependency])]
    (or (and (= 1 (count definitions))
             (< (:ordinal (first definitions)) destination-ordinal))
        (some #(< (:ordinal %) destination-ordinal)
              (get-in model [:declarations dependency])))))

(defn- dependency-closure [model requested destination]
  (let [deps (dependency-map model)
        destination-ordinal (:ordinal destination)]
    (loop [queue [requested]
           selected #{requested}]
      (if (empty? queue)
        {:selected selected}
        (let [current (first queue)
              candidates (->> (get deps current #{})
                              (remove selected)
                              (remove #(dependency-satisfied-at?
                                         model % destination-ordinal))
                              sort
                              vec)
              ambiguous (some #(when (not= 1 (count (get-in model [:by-name %]))) %)
                              candidates)]
          (if ambiguous
            {:error (str "Cannot resolve dependency definition: " ambiguous)
             :error-type :ambiguous-dependency
             :dependency ambiguous
             :match-count (count (get-in model [:by-name ambiguous]))}
            (recur (into (vec (rest queue)) candidates)
                   (into selected candidates))))))))

(defn- stable-topological-order [model selected]
  (let [deps (dependency-map model)
        order-key (into {} (map (juxt :name :ordinal) (:definitions model)))
        selected-deps (into {} (for [name selected]
                                 [name (set/intersection selected
                                                         (get deps name #{}))]))
        reverse-deps (reduce-kv
                       (fn [result user dependencies]
                         (reduce #(update %1 %2 (fnil conj #{}) user)
                                 result dependencies))
                       {} selected-deps)
        initial (sort-by order-key
                         (for [[name dependencies] selected-deps
                               :when (empty? dependencies)]
                           name))]
    (loop [ready (vec initial)
           remaining-deps selected-deps
           result []]
      (if (empty? ready)
        (if (= (count result) (count selected))
          {:order result}
          {:error "Move dependency closure contains a cycle"
           :error-type :cyclic-move-dependencies
           :cycle (->> (keys remaining-deps)
                       (remove (set result))
                       (sort-by order-key)
                       vec)})
        (let [current (first ready)
              dependents (get reverse-deps current #{})
              next-deps (reduce #(update %1 %2 disj current)
                                remaining-deps dependents)
              newly-ready (->> dependents
                               (filter #(and (empty? (get next-deps %))
                                             (not (some #{%} ready))
                                             (not (some #{%} result))))
                               (sort-by order-key))]
          (recur (into (vec (rest ready)) newly-ready)
                 next-deps
                 (conj result current)))))))

(defn- common-prefix-count [left right]
  (count (take-while true? (map = left right))))

(defn- common-suffix-count [left right prefix-count]
  (let [max-count (- (min (count left) (count right)) prefix-count)]
    (count (take-while true?
                       (map = (take max-count (reverse left))
                            (take max-count (reverse right)))))))

(defn- unified-diff [file before after]
  (let [left (vec (str/split-lines before))
        right (vec (str/split-lines after))
        prefix (common-prefix-count left right)
        suffix (common-suffix-count left right prefix)
        left-end (- (count left) suffix)
        right-end (- (count right) suffix)
        removed (subvec left prefix left-end)
        added (subvec right prefix right-end)]
    (str "--- " file "\n"
         "+++ " file "\n"
         "@@ -" (inc prefix) "," (count removed)
         " +" (inc prefix) "," (count added) " @@\n"
         (when (seq removed)
           (str (str/join "\n" (map #(str "-" %) removed)) "\n"))
         (when (seq added)
           (str (str/join "\n" (map #(str "+" %) added)) "\n")))))

(defn- build-candidate [source model move-order destination]
  (let [entries (mapv #(first (get-in model [:by-name %])) move-order)
        blocks (into {} (map (fn [{:keys [name start-index end-index]}]
                               [name (subvec (:lines model)
                                             start-index end-index)])
                             entries))
        ranges (sort-by :start-index > entries)
        remaining (reduce (fn [lines {:keys [start-index end-index]}]
                            (into (subvec lines 0 start-index)
                                  (subvec lines end-index)))
                          (:lines model) ranges)
        destination-start (:start-index destination)
        removed-before (reduce + (for [{:keys [start-index end-index]} entries
                                       :when (< start-index destination-start)]
                                   (- end-index start-index)))
        insert-at (- destination-start removed-before)
        moved-lines (vec (mapcat (fn [name]
                                   (concat (get blocks name) [""]))
                                 move-order))
        moved-lines (if (seq moved-lines) (pop moved-lines) moved-lines)
        candidate-lines (vec (concat (subvec remaining 0 insert-at)
                                     (when (and (pos? insert-at)
                                                (not (str/blank?
                                                       (nth remaining
                                                            (dec insert-at) ""))))
                                       [""])
                                     moved-lines
                                     [""]
                                     (subvec remaining insert-at)))
        candidate (str/join "\n" candidate-lines)]
    (if (str/ends-with? source "\n") (str candidate "\n") candidate)))

(defn- overlapping-layout-error [model move-order destination]
  (let [moving (mapv #(first (get-in model [:by-name %])) move-order)
        relevant (conj moving destination)
        overlap (first
                  (for [entry relevant
                        other (:entries model)
                        :when (and (not= (:ordinal entry) (:ordinal other))
                                   (< (max (:start-index entry) (:start-index other))
                                      (min (:end-index entry) (:end-index other))))]
                    [entry other]))]
    (when overlap
      (let [[entry other] overlap]
        {:error (str "Cannot safely move forms that share source lines: "
                     (or (:name entry) (:type entry)) ", "
                     (or (:name other) (:type other)))
         :error-type :unsupported-source-layout
         :line (max (:line entry) (:line other))
         :forms (mapv #(or (:name %) (:type %)) overlap)}))))

(defn- move-command [opts with-deps?]
  (str "clj-surgeon :op " (if with-deps? ":mv-with-deps" ":mv")
       " :file " (or (:file opts) "source.clj")
       " :form " (:form opts)
       " :before " (:before opts)))

(defn- dependency-error [opts source-model destination outgoing]
  (let [stranded (->> outgoing
                      (map (fn [{:keys [dependency defined-at]}]
                             (let [source-entry (first (get-in source-model
                                                               [:by-name dependency]))]
                               {:name dependency
                                :defined-at (:line source-entry)
                                :would-be-at defined-at
                                :required-before (:line destination)})))
                      (sort-by (juxt :defined-at :name))
                      vec)
        file (or (:file opts) "source.clj")
        apply-command (move-command opts true)]
    {:error (str "Moving " (:form opts) " before " (:before opts)
                 " would strand dependencies")
     :error-type :would-strand-dependencies
     :operation :mv
     :file file
     :form (str (:form opts))
     :before (str (:before opts))
     :direction :up
     :stranded stranded
     :recommended-action :preview-dependency-closure
     :recommended-command (str apply-command " :dry-run true")
     :apply-command apply-command
     :remedies {:with-deps "Move only the dependencies required at the destination"
                :manual (str "Move " (str/join ", " (map :name stranded))
                             " first, then retry")}}))

(defn- user-error [opts incoming]
  {:error (str "Moving " (:form opts) " before " (:before opts)
               " would strand existing users")
   :error-type :would-strand-users
   :operation :mv
   :form (str (:form opts))
   :before (str (:before opts))
   :stranded-users (->> incoming
                        (map (fn [{:keys [from used-at]}]
                               {:name from :used-at used-at}))
                        (sort-by (juxt :used-at :name))
                        vec)
   :remedies {:manual "Choose a destination that remains above these users"}})

(defn plan-move
  "Pure planner for an exact or dependency-expanded within-file move.
   Source text and normalized options in; plan/refusal data and candidate text
   out. No filesystem effects."
  [source {:keys [form before with-deps] :as opts}]
  (try
    (let [model (source-model source)
          source-entry (unique-entry model form :source)
          destination (unique-entry model before :destination)]
      (cond
        (:error source-entry) source-entry
        (:error destination) destination

        (= (str form) (str before))
        {:ok true
         :result source
         :apply-command (move-command opts (boolean with-deps))
         :plan {:operation :mv
                :form (str form)
                :requested-forms [(str form)]
                :added-forms []
                :move-order [(str form)]
                :lines-moved 0
                :to-before (str before)
                :before (str before)
                :from-line (:line source-entry)
                :to-line (:line destination)
                :direction :none
                :with-deps (boolean with-deps)
                :no-op true
                :source-hash (structural-lens/source-hash source)
                :result-hash (structural-lens/source-hash source)
                :diff ""}}

        :else
        (let [direction (if (< (:ordinal source-entry) (:ordinal destination))
                          :down :up)
              closure (if (and with-deps (= :up direction))
                        (dependency-closure model (str form) destination)
                        {:selected #{(str form)}})]
          (if (:error closure)
            closure
            (let [ordered (stable-topological-order model (:selected closure))]
              (if (:error ordered)
                ordered
                (let [move-order (:order ordered)
                      layout-error (overlapping-layout-error model move-order destination)
                      moved-line-count (reduce +
                                               (for [name move-order
                                                     :let [entry (first (get-in model [:by-name name]))]]
                                                 (- (:end-index entry)
                                                    (:start-index entry))))
                      candidate (when-not layout-error
                                  (build-candidate source model move-order destination))]
                  (if layout-error
                    layout-error
                    (let [candidate-model (source-model candidate)
                          introduced (new-violations model candidate-model)
                          outgoing (filter #(contains? (set move-order) (:from %))
                                           introduced)
                          incoming (filter #(contains? (set move-order) (:dependency %))
                                           introduced)]
                      (cond
                        (seq outgoing) (dependency-error opts model destination outgoing)
                        (seq incoming) (user-error opts incoming)
                        :else
                        (let [added (vec (remove #{(str form)} move-order))]
                          {:ok true
                           :result candidate
                           :apply-command (move-command opts (boolean with-deps))
                           :plan {:operation :mv
                                  :form (str form)
                                  :requested-forms [(str form)]
                                  :added-forms added
                                  :move-order move-order
                                  :lines-moved moved-line-count
                                  :to-before (str before)
                                  :before (str before)
                                  :from-line (:line source-entry)
                                  :to-line (:line destination)
                                  :direction direction
                                  :with-deps (boolean with-deps)
                                  :source-hash (structural-lens/source-hash source)
                                  :result-hash (structural-lens/source-hash candidate)
                                  :diff (unified-diff (or (:file opts) "source.clj")
                                                      source candidate)}})))))))))))
    (catch Exception e
      {:error (str "Could not plan move: " (.getMessage e))
       :error-type :invalid-source})))

(defn move-form
  "Plan and optionally execute a dependency-aware move within one file."
  [{:keys [file dry-run] :as opts}]
  (let [source (slurp file)
        result (plan-move source opts)]
    (cond
      (:error result) result
      dry-run (dissoc result :result)
      :else
      (try
        (let [candidate (:result result)
              plan (:plan result)]
          (when-not (:no-op plan)
            (file-ops/atomic-write! file candidate))
          {:ok true
           :file file
           :form (:form plan)
           :moved-from (:from-line plan)
           :moved-to (:to-line plan)
           :lines-moved (:lines-moved plan)
           :plan plan})
        (catch Exception e
          {:error (str "Atomic replacement failed; target was not replaced: "
                       (.getMessage e))
           :error-type :atomic-write-failed
           :file file})))))
