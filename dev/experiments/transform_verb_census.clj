(ns transform-verb-census
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [rewrite-clj.node :as node]
   [rewrite-clj.parser :as parser])
  (:import
   (java.nio.charset StandardCharsets)
   (java.security MessageDigest)
   (java.time Instant)))

(def since (Instant/parse "2026-08-22T00:00:00Z"))
(def until (Instant/parse "2026-08-30T02:09:33.141926Z"))
(def max-verbs 3)
(def escape-op "replace-subform-freeform")

(defn utf8-bytes [s]
  (alength (.getBytes (str s) StandardCharsets/UTF_8)))

(defn sha256 [s]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256")
                        (.getBytes (str s) StandardCharsets/UTF_8))]
    (apply str (map #(format "%02x" (bit-and % 0xff)) digest))))

(defn canonicalize [x]
  (cond
    (map? x) (into (sorted-map) (map (fn [[k v]] [(name k) (canonicalize v)])) x)
    (sequential? x) (mapv canonicalize x)
    :else x))

(defn canonical-json [x]
  (json/generate-string (canonicalize x)))

(defn program-json [ops]
  (canonical-json {:ops ops}))

(defn program-bytes [ops]
  (utf8-bytes (program-json ops)))

(defn scalar-tag [x]
  (cond
    (symbol? x) "symbol"
    (keyword? x) "keyword"
    (string? x) "string"
    (number? x) "number"
    (boolean? x) "boolean"
    (nil? x) "nil"
    (char? x) "character"
    (instance? java.util.regex.Pattern x) "regex"
    :else "scalar"))

(defn normalize-reader-symbol [x]
  (if (symbol? x)
    (let [spelling (str x)]
      (if-let [[_ argument] (re-matches #"p([0-9]+)__[0-9]+#" spelling)]
        (symbol (str "p" argument "__reader-fn#"))
        x))
    x))

(declare form->tree tree->form)

(defn form->tree [form]
  (cond
    (seq? form) {:tag "list" :children (mapv form->tree form)}
    (vector? form) {:tag "vector" :children (mapv form->tree form)}
    (map? form) {:tag "map"
                 :children (mapv (fn [[k v]]
                                   {:tag "entry" :children [(form->tree k) (form->tree v)]})
                                 form)}
    (set? form) {:tag "set" :children (mapv form->tree (sort-by pr-str form))}
    :else (let [normalized (normalize-reader-symbol form)]
            {:tag (scalar-tag normalized) :value normalized :source (pr-str normalized)})))

(defn tree->form [{:keys [tag children value]}]
  (case tag
    "list" (apply list (map tree->form children))
    "vector" (mapv tree->form children)
    "map" (into (array-map)
                (map (fn [{entry-children :children}]
                       (mapv tree->form entry-children)) children))
    "set" (set (map tree->form children))
    "entry" (mapv tree->form children)
    value))

(defn tree-source [tree]
  (pr-str (tree->form tree)))

(def ignorable-tags #{:whitespace :newline :comma})

(defn contains-comment? [root]
  (boolean
    (some #(= :comment (node/tag %))
          (tree-seq node/inner? node/children root))))

(defn parse-tree [source]
  (try
    (let [parsed (parser/parse-string-all source)
          comments? (contains-comment? parsed)
          form (node/sexpr parsed)]
      {:ok true :comments? comments? :tree (form->tree form)})
    (catch Throwable t
      {:ok false :error (.getName (class t))})))

(defn inner? [tree]
  (contains? tree :children))

(defn scalar? [tree]
  (not (inner? tree)))

(defn get-at [tree path]
  (reduce (fn [current index] (get-in current [:children index])) tree path))

(defn update-at [tree path f]
  (if (empty? path)
    (f tree)
    (let [index (first path)]
      (update-in tree [:children index] #(update-at % (subvec (vec path) 1) f)))))

(defn parse-literal-tree [source]
  (let [parsed (parse-tree source)]
    (when (:ok parsed) (:tree parsed))))

(defn symbol-tree [s]
  {:tag "symbol" :value (symbol s) :source s})

(defn replace-symbols [tree from to]
  (cond
    (and (= "symbol" (:tag tree)) (= from (str (:value tree)))) (symbol-tree to)
    (inner? tree) (update tree :children #(mapv (fn [child] (replace-symbols child from to)) %))
    :else tree))

(defn replace-string-value [tree from to]
  (assoc tree :value (str/replace (:value tree) from to)
         :source (pr-str (str/replace (:value tree) from to))))

(defn nested->threaded [tree style]
  (loop [current tree
         steps []]
    (let [children (:children current)]
      (if (and (= "list" (:tag current)) (<= 2 (count children)))
        (let [previous-index (if (= style "first") 1 (dec (count children)))
              previous (nth children previous-index)
              step-children (vec (concat (subvec children 0 previous-index)
                                         (subvec children (inc previous-index))))
              step (if (= 1 (count step-children))
                     (first step-children)
                     {:tag "list" :children step-children})]
          (recur previous (conj steps step)))
        (when (seq steps)
          {:tag "list"
           :children (vec (concat [(symbol-tree (if (= style "first") "->" "->>")) current]
                                  (reverse steps)))})))))

(defn threaded->nested [tree style]
  (let [children (:children tree)
        expected (if (= style "first") "->" "->>")]
    (when (and (= "list" (:tag tree))
               (<= 3 (count children))
               (= "symbol" (:tag (first children)))
               (= expected (str (:value (first children)))))
      (reduce
        (fn [acc step]
          (if (= "list" (:tag step))
            (let [step-children (:children step)]
              {:tag "list"
               :children (if (= style "first")
                           (vec (concat [(first step-children) acc] (rest step-children)))
                           (vec (concat step-children [acc])))})
            {:tag "list" :children [(if (= "symbol" (:tag step)) step step) acc]}))
        (second children)
        (drop 2 children)))))

(defn apply-op [tree op]
  (let [path (vec (:at op))]
    (case (:op op)
      "replace-value"
      (update-at tree path (constantly (parse-literal-tree (:value op))))

      "rename-symbol"
      (update-at tree path #(replace-symbols % (:from op) (:to op)))

      "replace-string"
      (update-at tree path #(replace-string-value % (:from op) (:to op)))

      "wrap-form"
      (update-at tree path
                 (fn [old]
                   {:tag (:tag op)
                    :children (vec (concat (map parse-literal-tree (:prefix op))
                                           [old]
                                           (map parse-literal-tree (:suffix op))))}))

      "unwrap-form"
      (update-at tree path #(nth (:children %) (:child op)))

      "insert-child"
      (update-at tree path #(update % :children
                                    (fn [children]
                                      (vec (concat (subvec children 0 (:index op))
                                                   [(parse-literal-tree (:value op))]
                                                   (subvec children (:index op)))))))

      "remove-child"
      (update-at tree path #(update % :children
                                    (fn [children]
                                      (vec (concat (subvec children 0 (:index op))
                                                   (subvec children (inc (:index op))))))))

      "reorder"
      (update-at tree path #(assoc % :children (mapv (fn [index] (nth (:children %) index))
                                                     (:order op))))

      "thread"
      (update-at tree path #(if (= "forward" (:direction op))
                              (nested->threaded % (:style op))
                              (threaded->nested % (:style op))))

      "extract-binding"
      (update-at tree path
                 (fn [old]
                   (let [replacement (symbol-tree (:name op))
                         body (reduce (fn [current occurrence]
                                        (update-at current (vec occurrence) (constantly replacement)))
                                      old
                                      (:occurrences op))]
                     {:tag "list"
                      :children [(symbol-tree "let")
                                 {:tag "vector"
                                  :children [(symbol-tree (:name op))
                                             (parse-literal-tree (:expression op))]}
                                 body]})))

      "change-arglist"
      (update-at tree path (constantly (parse-literal-tree (:value op))))

      "replace-subform-freeform"
      (update-at tree path (constantly (parse-literal-tree (:value op)))))))

(defn apply-program [tree ops]
  (reduce apply-op tree ops))

(defn first-tree-difference
  ([a b] (first-tree-difference a b []))
  ([a b path]
   (cond
     (= a b) nil
     (not= (:tag a) (:tag b)) {:path path :left-tag (:tag a) :right-tag (:tag b)}
     (and (inner? a) (inner? b))
     (or (when (not= (count (:children a)) (count (:children b)))
           {:path path :left-count (count (:children a)) :right-count (count (:children b))})
         (some identity
               (map-indexed (fn [index [left right]]
                              (first-tree-difference left right (conj path index)))
                            (map vector (:children a) (:children b)))))
     :else {:path path :tag (:tag a)
            :left-sha (sha256 (pr-str (:value a)))
            :right-sha (sha256 (pr-str (:value b)))})))

(defn leaf-differences [a b]
  (cond
    (= a b) []
    (and (scalar? a) (scalar? b)) [[a b]]
    (and (inner? a) (inner? b)
         (= (:tag a) (:tag b))
         (= (count (:children a)) (count (:children b))))
    (mapcat leaf-differences (:children a) (:children b))
    :else nil))

(defn rename-candidate [a b path]
  (let [diffs (leaf-differences a b)]
    (when (and (seq diffs)
               (every? (fn [[x y]] (and (= "symbol" (:tag x)) (= "symbol" (:tag y)))) diffs))
      (let [pairs (set (map (fn [[x y]] [(str (:value x)) (str (:value y))]) diffs))]
        (when (= 1 (count pairs))
          (let [[from to] (first pairs)
                op {:op "rename-symbol" :at path :from from :to to}]
            (when (= b (apply-op a (assoc op :at []))) op)))))))

(defn common-prefix-length [a b]
  (loop [index 0]
    (if (and (< index (count a)) (< index (count b)) (= (nth a index) (nth b index)))
      (recur (inc index))
      index)))

(defn common-suffix-length [a b prefix]
  (loop [length 0]
    (if (and (< (+ prefix length) (count a))
             (< (+ prefix length) (count b))
             (= (nth a (- (dec (count a)) length))
                (nth b (- (dec (count b)) length))))
      (recur (inc length))
      length)))

(defn string-candidate [a b path]
  (when (and (= "string" (:tag a)) (= "string" (:tag b)))
    (let [old (:value a)
          new (:value b)
          prefix (common-prefix-length old new)
          suffix (common-suffix-length old new prefix)
          from (subs old prefix (- (count old) suffix))
          to (subs new prefix (- (count new) suffix))]
      (when (and (seq from) (= new (str/replace old from to)))
        {:op "replace-string" :at path :from from :to to}))))

(defn wrap-candidates [a b path]
  (when (inner? b)
    (keep-indexed
      (fn [index child]
        (when (= a child)
          {:op "wrap-form" :at path :tag (:tag b)
           :prefix (mapv tree-source (subvec (:children b) 0 index))
           :suffix (mapv tree-source (subvec (:children b) (inc index)))}))
      (:children b))))

(defn unwrap-candidates [a b path]
  (when (inner? a)
    (keep-indexed (fn [index child]
                    (when (= child b) {:op "unwrap-form" :at path :child index}))
                  (:children a))))

(defn insertion-candidates [a b path]
  (when (and (inner? a) (inner? b) (= (:tag a) (:tag b))
             (= (inc (count (:children a))) (count (:children b))))
    (keep
      (fn [index]
        (let [children (:children b)
              without (vec (concat (subvec children 0 index) (subvec children (inc index))))]
          (when (= (:children a) without)
            {:op "insert-child" :at path :index index :value (tree-source (nth children index))})))
      (range (count (:children b))))))

(defn removal-candidates [a b path]
  (when (and (inner? a) (inner? b) (= (:tag a) (:tag b))
             (= (dec (count (:children a))) (count (:children b))))
    (keep
      (fn [index]
        (let [children (:children a)
              without (vec (concat (subvec children 0 index) (subvec children (inc index))))]
          (when (= (:children b) without)
            {:op "remove-child" :at path :index index})))
      (range (count (:children a))))))

(defn permutation [old target]
  (loop [remaining (set (range (count old)))
         wanted target
         order []]
    (if (empty? wanted)
      order
      (when-let [index (first (filter #(= (nth old %) (first wanted)) (sort remaining)))]
        (recur (disj remaining index) (rest wanted) (conj order index))))))

(defn reorder-candidate [a b path]
  (when (and (inner? a) (inner? b) (= (:tag a) (:tag b))
             (= (count (:children a)) (count (:children b))))
    (when-let [order (permutation (:children a) (:children b))]
      (when (not= order (vec (range (count order))))
        {:op "reorder" :at path :order order}))))

(defn thread-candidates [a b path]
  (for [direction ["forward" "reverse"]
        style ["first" "last"]
        :let [op {:op "thread" :at path :direction direction :style style}]
        :when (= b (apply-op a (assoc op :at [])))]
    op))

(defn paths-matching [tree predicate]
  (letfn [(walk [current path]
            (concat (when (predicate current) [path])
                    (when (inner? current)
                      (mapcat (fn [index child] (walk child (conj path index)))
                              (range) (:children current)))))]
    (walk tree [])))

(defn extract-candidate [a b path]
  (when (and (= "list" (:tag b)) (= 3 (count (:children b))))
    (let [[head bindings body] (:children b)
          binding-children (:children bindings)]
      (when (and (= "symbol" (:tag head)) (= "let" (str (:value head)))
                 (= "vector" (:tag bindings)) (= 2 (count binding-children))
                 (= "symbol" (:tag (first binding-children))))
        (let [name (str (:value (first binding-children)))
              expression (second binding-children)
              occurrences (vec (paths-matching body #(and (= "symbol" (:tag %))
                                                          (= name (str (:value %))))))
              restored (reduce (fn [current occurrence]
                                 (update-at current occurrence (constantly expression)))
                               body occurrences)]
          (when (and (seq occurrences) (= a restored))
            {:op "extract-binding" :at path :name name
             :expression (tree-source expression) :occurrences occurrences}))))))

(defn ancestor-at [root path]
  (when (seq path) (get-at root (pop (vec path)))))

(defn arglist-path? [root path]
  (let [parent (ancestor-at root path)
        head (first (:children parent))]
    (and (= "list" (:tag parent))
         (= "symbol" (:tag head))
         (contains? #{"fn" "defn" "defn-"} (str (:value head))))))

(defn one-op-candidates [a b path root]
  (remove
    nil?
    (concat
      [(rename-candidate a b path)
       (string-candidate a b path)
       (when (and (scalar? a) (scalar? b))
         {:op "replace-value" :at path :value (tree-source b)})
       (reorder-candidate a b path)
       (extract-candidate a b path)
       (when (and (= "vector" (:tag a)) (= "vector" (:tag b)) (arglist-path? root path))
         {:op "change-arglist" :at path :value (tree-source b)})]
      (wrap-candidates a b path)
      (unwrap-candidates a b path)
      (insertion-candidates a b path)
      (removal-candidates a b path)
      (thread-candidates a b path))))

(defn better-plan [plans]
  (first
    (sort-by (fn [ops] [(count ops) (program-bytes ops) (program-json ops)])
             (remove nil? plans))))

(declare best-closed-plan)

(defn same-width-plan [a b path root]
  (when (and (inner? a) (inner? b) (= (:tag a) (:tag b))
             (= (count (:children a)) (count (:children b))))
    (loop [index 0
           ops []]
      (if (= index (count (:children a)))
        ops
        (let [left (nth (:children a) index)
              right (nth (:children b) index)
              child-plan (best-closed-plan left right (conj path index) root)]
          (when (and child-plan (<= (+ (count ops) (count child-plan)) max-verbs))
            (recur (inc index) (into ops child-plan))))))))

(defn one-insert-composed [a b path root]
  (when (and (inner? a) (inner? b) (= (:tag a) (:tag b))
             (= (inc (count (:children a))) (count (:children b))))
    (keep
      (fn [index]
        (let [target-children (:children b)
              target-without (assoc b :children
                                    (vec (concat (subvec target-children 0 index)
                                                 (subvec target-children (inc index)))))
              base (best-closed-plan a target-without path root)
              op {:op "insert-child" :at path :index index
                  :value (tree-source (nth target-children index))}]
          (when (and base (< (count base) max-verbs)) (conj (vec base) op))))
      (range (count (:children b))))))

(defn one-remove-composed [a b path root]
  (when (and (inner? a) (inner? b) (= (:tag a) (:tag b))
             (= (dec (count (:children a))) (count (:children b))))
    (keep
      (fn [index]
        (let [source-children (:children a)
              source-without (assoc a :children
                                    (vec (concat (subvec source-children 0 index)
                                                 (subvec source-children (inc index)))))
              op {:op "remove-child" :at path :index index}
              rest-plan (best-closed-plan source-without b path root)]
          (when (and rest-plan (< (count rest-plan) max-verbs))
            (into [op] rest-plan))))
      (range (count (:children a))))))

(defn best-closed-plan [a b path root]
  (if (= a b)
    []
    (let [one-ops (map vector (one-op-candidates a b path root))
          candidates (concat one-ops
                             [(same-width-plan a b path root)]
                             (one-insert-composed a b path root)
                             (one-remove-composed a b path root))
          verified (filter (fn [ops]
                             (and (<= (count ops) max-verbs)
                                  (= b (apply-program a (mapv #(assoc % :at
                                                                      (vec (drop (count path) (:at %))))
                                                              ops)))))
                           candidates)]
      ;; Verification above is local to this subtree; paths are rebased for replay.
      (better-plan verified))))

(defn rebase-plan [ops prefix]
  (mapv #(update % :at (fn [path] (vec (concat prefix path)))) ops))

(defn classify-pair [from-source to-source]
  (let [from-parsed (parse-tree from-source)
        to-parsed (parse-tree to-source)
        to-bytes (utf8-bytes (json/generate-string to-source))]
    (if (and (:ok from-parsed) (:ok to-parsed)
             (not (:comments? from-parsed)) (not (:comments? to-parsed)))
      (let [from (:tree from-parsed)
            to (:tree to-parsed)
            local-plan (best-closed-plan from to [] from)
            plan (or local-plan [{:op escape-op :at [] :value to-source}])
            replay (apply-program from plan)
            escaped? (some #(= escape-op (:op %)) plan)]
        (assert (= to replay) {:type :replay-failed :program (program-json plan)
                               :difference (first-tree-difference to replay)})
        {:parse-ok true :comments false :ops plan :verb-count (count plan)
         :escaped escaped? :program-bytes (program-bytes plan) :to-bytes to-bytes})
      (let [plan [{:op escape-op :at [] :value to-source}]]
        {:parse-ok (and (:ok from-parsed) (:ok to-parsed))
         :comments (or (:comments? from-parsed) (:comments? to-parsed))
         :ops plan :verb-count 1 :escaped true
         :program-bytes (program-bytes plan) :to-bytes to-bytes}))))

(defn timestamp-in-window? [event]
  (try
    (let [instant (Instant/parse (get event "timestamp"))]
      (and (not (.isBefore instant since)) (not (.isAfter instant until))))
    (catch Throwable _ false)))

(defn telemetry-files [root]
  (sort-by #(.getName %) (filter #(.isFile %) (file-seq (io/file root)))))

(defn file-has-window-call? [file]
  (with-open [reader (io/reader file)]
    (boolean
      (some (fn [line]
              (try
                (let [event (json/parse-string line)]
                  (and (= "tool.call" (get event "event"))
                       (timestamp-in-window? event)))
                (catch Throwable _ false)))
            (line-seq reader)))))

(defn telemetry-inventory [root]
  (->> (telemetry-files root)
       (filter file-has-window-call?)
       (map (fn [file]
              (let [content (slurp file)]
                {:bytes (utf8-bytes content) :sha256 (sha256 content)})))
       (sort-by :sha256)
       vec))

(defn load-calls [root]
  (->> (telemetry-files root)
       (mapcat (fn [file]
                 (with-open [reader (io/reader file)]
                   (doall
                     (keep (fn [line]
                             (try
                               (let [event (json/parse-string line)]
                                 (when (and (= "tool.call" (get event "event"))
                                            (timestamp-in-window? event))
                                   event))
                               (catch Throwable _ nil)))
                           (line-seq reader))))))
       (sort-by #(get % "timestamp"))))

(defn extract-pairs [writes]
  (vec
    (mapcat
      (fn [write-index event]
        (let [request (get event "request")
              edits (get request "edits")
              changes (get request "changes")]
          (concat
            (keep (fn [item]
                    (when (and (string? (get item "from")) (string? (get item "to")))
                      {:family "from_to" :write write-index
                       :from (get item "from") :to (get item "to")}))
                  (when (sequential? edits) edits))
            (keep (fn [item]
                    (when (and (string? (get item "find")) (string? (get item "replace")))
                      {:family "find_replace" :write write-index
                       :from (get item "find") :to (get item "replace")}))
                  (when (sequential? changes) changes)))))
      (range) writes)))

(defn percentage [n d]
  (if (zero? d) 0.0 (double (/ (* 100.0 n) d))))

(defn quantiles [xs]
  (let [values (vec (sort xs))
        n (count values)
        at (fn [p] (when (pos? n) (nth values (max 0 (dec (int (Math/ceil (* p n))))))))]
    {:n n :min (first values) :p25 (at 0.25) :median (at 0.50) :p75 (at 0.75)
     :p90 (at 0.90) :max (last values)}))

(defn summarize-group [rows]
  (let [pairs (count rows)
        escaped (count (filter :escaped rows))
        closed-rows (remove :escaped rows)
        escape-rows (filter :escaped rows)
        to-bytes (reduce + (map :to-bytes rows))
        program-bytes (reduce + (map :program-bytes rows))]
    {:pairs pairs
     :semantic-identical (count (filter #(zero? (:verb-count %)) rows))
     :closed-lte-1 (count (filter #(and (not (:escaped %)) (<= (:verb-count %) 1)) rows))
     :closed-lte-2 (count (filter #(and (not (:escaped %)) (<= (:verb-count %) 2)) rows))
     :closed-lte-3 (count (filter #(and (not (:escaped %)) (<= (:verb-count %) 3)) rows))
     :closed-lte-1-percent (percentage (count (filter #(and (not (:escaped %)) (<= (:verb-count %) 1)) rows)) pairs)
     :closed-lte-2-percent (percentage (count (filter #(and (not (:escaped %)) (<= (:verb-count %) 2)) rows)) pairs)
     :closed-lte-3-percent (percentage (count (filter #(and (not (:escaped %)) (<= (:verb-count %) 3)) rows)) pairs)
     :escape-pairs escaped
     :escape-share-percent (percentage escaped pairs)
     :escape-to-bytes (reduce + (map :to-bytes escape-rows))
     :escape-program-bytes (reduce + (map :program-bytes escape-rows))
     :closed-to-bytes (reduce + (map :to-bytes closed-rows))
     :closed-program-bytes (reduce + (map :program-bytes closed-rows))
     :to-bytes to-bytes :program-bytes program-bytes
     :net-bytes (- to-bytes program-bytes)
     :estimated-net-tokens (- (long (Math/ceil (/ to-bytes 4.0)))
                              (long (Math/ceil (/ program-bytes 4.0))))
     :verb-count-quantiles (quantiles (map :verb-count rows))
     :program-byte-quantiles (quantiles (map :program-bytes rows))}))

(defn write-summary [rows writes-count]
  (let [by-write (group-by :write rows)
        eligible (count by-write)
        closed-at (fn [n]
                    (count (filter (fn [[_ pairs]]
                                     (every? #(and (not (:escaped %)) (<= (:verb-count %) n)) pairs))
                                   by-write)))
        escape-writes (count (filter (fn [[_ pairs]] (some :escaped pairs)) by-write))]
    {:writes writes-count :eligible-writes eligible :zero-pair-writes (- writes-count eligible)
     :closed-lte-1 (closed-at 1) :closed-lte-2 (closed-at 2) :closed-lte-3 (closed-at 3)
     :closed-lte-1-percent (percentage (closed-at 1) eligible)
     :closed-lte-2-percent (percentage (closed-at 2) eligible)
     :closed-lte-3-percent (percentage (closed-at 3) eligible)
     :escape-writes escape-writes
     :escape-share-of-eligible-percent (percentage escape-writes eligible)}))

(defn verb-frequency [rows]
  (let [operation-frequencies (frequencies (mapcat #(map :op (:ops %)) rows))
        pair-uses (frequencies (mapcat #(set (map :op (:ops %))) rows))
        ordered (->> operation-frequencies
                     (map (fn [[verb operation-count]]
                            {:verb verb :operations operation-count :pairs (get pair-uses verb)
                             :pair-share-percent (percentage (get pair-uses verb) (count rows))}))
                     (sort-by (juxt (comp - :operations) :verb))
                     vec)]
    (loop [remaining ordered
           selected #{}
           result []]
      (if-let [entry (first remaining)]
        (let [selected-now (conj selected (:verb entry))
              covered (count (filter (fn [row]
                                       (some selected-now (map :op (:ops row)))) rows))]
          (recur (rest remaining) selected-now
                 (conj result (assoc entry
                                     :cumulative-pairs-covered covered
                                     :cumulative-pair-coverage-percent (percentage covered (count rows))))))
        result))))

(defn run-self-tests []
  (let [cases [["(+ 1)" "(+ 2)"]
               ["(let [x 1] (+ x x))" "(let [y 1] (+ y y))"]
               ["(str \"alpha-old\")" "(str \"alpha-new\")"]
               ["(+ 1 2)" "(when ready (+ 1 2))"]
               ["(when ready (+ 1 2))" "(+ 1 2)"]
               ["(f a)" "(f a b)"]
               ["(f a b)" "(f a)"]
               ["(f a b)" "(f b a)"]
               ["(f (g x))" "(-> x g f)"]
               ["(f (+ a b) (+ a b))" "(let [x (+ a b)] (f x x))"]
               ["(defn f [x y z] x)" "(defn f [a b c] x)"]]
        results (mapv (fn [[from to]] (classify-pair from to)) cases)]
    (assert (every? #(and (:parse-ok %) (not (:escaped %))) results)
            (mapv #(select-keys % [:parse-ok :escaped :verb-count]) results))
    (assert (every? #(<= (:verb-count %) max-verbs) results))
    {:cases (count cases)
     :verbs (sort (set (mapcat #(map :op (:ops %)) results)))}))

(defn main [telemetry-root output]
  (let [self-tests (run-self-tests)
        calls (load-calls telemetry-root)
        tool-counts (frequencies (map #(get % "tool") calls))
        writes (vec (filter #(= "apply_clojure_changes" (get % "tool")) calls))
        canonical-request-bytes (reduce + (map #(utf8-bytes (canonical-json (dissoc (get % "request") "workspace_root"))) writes))
        pairs (extract-pairs writes)
        classified (mapv (fn [pair-index pair]
                           (merge (select-keys pair [:family :write])
                                  {:pair pair-index}
                                  (classify-pair (:from pair) (:to pair))))
                         (range) pairs)
        grouped (group-by :family classified)
        receipt {:schema "transform-verb-expressibility-census.v1"
                 :window {:since (str since) :until (str until) :bounds "inclusive"}
                 :method {:model-calls 0 :parser "rewrite-clj 1.2.50"
                          :search-depth max-verbs
                          :comments "escape-hatch"
                          :program-encoding "compact sorted-key JSON; full ops envelope"
                          :token-estimate "ceil(bytes/4) per payload"
                          :self-tests self-tests
                          :script-sha256 (sha256 (slurp *file*))}
                 :telemetry-inventory {:files (telemetry-inventory telemetry-root)}
                 :population {:tool-calls (count calls) :tool-counts tool-counts
                              :writes (count writes) :eligible-pairs (count pairs)
                              :pair-families (frequencies (map :family pairs))}
                 :canonical-requests {:bytes canonical-request-bytes
                                      :workspace-root-rule "removed"}
                 :pairs (summarize-group classified)
                 :writes (write-summary classified (count writes))
                 :families (into (sorted-map)
                                 (map (fn [[family rows]] [family (summarize-group rows)]) grouped))
                 :parsing {:parse-failures (count (remove :parse-ok classified))
                           :comment-bearing-pairs (count (filter :comments classified))}
                 :verbs (verb-frequency classified)
                 :sensitivity (into (sorted-map)
                                    (for [extra [0 16 32]]
                                      (let [to-bytes (reduce + (map :to-bytes classified))
                                            program-bytes (+ (reduce + (map :program-bytes classified))
                                                             (* extra (count classified)))]
                                        [(str "+" extra "B")
                                         {:program-bytes program-bytes
                                          :net-bytes (- to-bytes program-bytes)
                                          :estimated-net-tokens (- (long (Math/ceil (/ to-bytes 4.0)))
                                                                   (long (Math/ceil (/ program-bytes 4.0))))}])))}
        invariants {:authority-counts (= [1437 {"apply_clojure_changes" 195
                                                "inspect_clojure" 1242} 195 332]
                                         [(count calls) tool-counts (count writes) (count pairs)])
                    :canonical-request-bytes (= 630138 canonical-request-bytes)
                    :pair-families (= {"find_replace" 125 "from_to" 207}
                                      (frequencies (map :family pairs)))
                    :all-replayed true
                    :no-program-over-depth (every? #(<= (:verb-count %) max-verbs) classified)}
        final-receipt (assoc receipt :invariants invariants
                             :all-invariants-true (every? true? (vals invariants)))
        payload (canonical-json final-receipt)
        with-hash (assoc final-receipt :receipt-payload-sha256 (sha256 payload))]
    (assert (:all-invariants-true with-hash) invariants)
    (spit output (str (json/generate-string (canonicalize with-hash) {:pretty true}) "\n"))
    (println (canonical-json {:status "ok" :receipt (:receipt-payload-sha256 with-hash)
                              :pairs (count pairs) :writes (count writes)
                              :escape-pairs (get-in with-hash [:pairs :escape-pairs])}))))

(when (= 2 (count *command-line-args*))
  (apply main *command-line-args*))
