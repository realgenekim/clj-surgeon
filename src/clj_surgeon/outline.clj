(ns clj-surgeon.outline
  "Parse a Clojure file and return structured outline of all top-level forms.
   For CLJC files (and any file containing reader conditionals), forms inside
   #?(:clj ...) / #?@(:cljs [...]) are surfaced too, each tagged with the
   platforms it applies to."
  (:require
   [clj-surgeon.cljc.walk :as cwalk]
   [clj-surgeon.forms :as forms]
   [clojure.string :as str]
   [rewrite-clj.node :as n]
   [rewrite-clj.zip :as z]))

(def max-string-symbols-per-form 512)

(def ^:private string-symbol-patterns
  [[:function #"(?:^|[^A-Za-z0-9_$]|\\[nrt])function\s+([A-Za-z_$][A-Za-z0-9_$]*)\s*\("]
   [:var #"(?:^|[^A-Za-z0-9_$]|\\[nrt])var\s+([A-Za-z_$][A-Za-z0-9_$]*)\b"]
   [:let #"(?:^|[^A-Za-z0-9_$]|\\[nrt])let\s+([A-Za-z_$][A-Za-z0-9_$]*)\b"]
   [:const #"(?:^|[^A-Za-z0-9_$]|\\[nrt])const\s+([A-Za-z_$][A-Za-z0-9_$]*)\b"]
   [:assignment #"(?:^|[^A-Za-z0-9_$]|\\[nrt])([A-Za-z_$][A-Za-z0-9_$]*)\s*=\s*function\b"]
   [:property #"(?:^|[^A-Za-z0-9_$]|\\[nrt])([A-Za-z_$][A-Za-z0-9_$]*)\s*:\s*function\b"]])

(defn- newline-count-before
  [s end]
  (loop [index 0
         count 0]
    (if (>= index end)
      count
      (recur (inc index)
             (if (= \newline (.charAt s index)) (inc count) count)))))

(defn- string-token-symbols
  [token-zloc owner-line owner]
  (let [raw (z/string token-zloc)
        token-row (:row (meta (z/node token-zloc)))]
    (->> string-symbol-patterns
         (map-indexed
           (fn [pattern-index [kind pattern]]
             (let [matcher (re-matcher pattern raw)]
               (loop [matches []]
                 (if (and (<= (count matches) max-string-symbols-per-form)
                          (.find matcher))
                   (recur (conj matches
                                {:index (.start matcher 1)
                                 :pattern-index pattern-index
                                 :name (.group matcher 1)
                                 :kind kind
                                 :line (+ owner-line token-row -1
                                          (newline-count-before
                                            raw (.start matcher 1)))
                                 :owner owner}))
                   matches)))))
         (mapcat identity))))

(defn string-symbols-for-form
  "Extract bounded JS-ish declarations from string tokens in one form record.

   The record must contain the exact `:source`, its absolute `:line`, and may
   contain its `:name`. Physical newlines in raw string-token source determine
   line offsets; escaped newline characters do not."
  [{:keys [source line name]}]
  (if (and source line)
    (let [root (z/of-string source {:track-position? true})
          found (loop [zloc root
                       matches []]
                  (if (z/end? zloc)
                    matches
                    (recur (z/next zloc)
                           (if (and (#{:token :multi-line}
                                      (n/tag (z/node zloc)))
                                    (string? (n/sexpr (z/node zloc))))
                             (into matches
                                   (string-token-symbols zloc line name))
                             matches))))
          ordered (->> found
                       (sort-by (juxt :line :index :pattern-index))
                       (reduce (fn [kept match]
                                 (if (some #(= [(:name match) (:line match)]
                                               [(:name %) (:line %)])
                                           kept)
                                   kept
                                   (conj kept match)))
                               []))]
      {:symbols (mapv #(dissoc % :index :pattern-index)
                      (take max-string-symbols-per-form ordered))
       :truncated? (> (count ordered) max-string-symbols-per-form)})
    {:symbols [] :truncated? false}))

(defn- resolve-user-fields
  "Run each user-supplied extractor fn against the form zloc. Returns a
   map field-key -> value, omitting nil results.

   When an extractor throws, attach context so the user can find the
   broken field — which macro, which field key, which form line."
  [user-fields zloc type-str line]
  (into {} (for [[k f] user-fields
                 :let [v (try (f zloc)
                              (catch Exception e
                                (throw (ex-info
                                         (str ".clj-surgeon.edn: extractor for "
                                              type-str " :fields " k
                                              " threw at line " line ": "
                                              (.getMessage e))
                                         {:macro type-str
                                          :field k
                                          :line line}
                                         e))))]
                 :when (some? v)]
             [k v])))

(defn- extract-name
  "Get the name from the second child of a form. Handles metadata like ^:private.
   Walks past meta nodes to find the actual symbol name."
  [zloc]
  (loop [child (some-> zloc z/down z/right)]
    (when child
      (let [s (z/string child)
            tag (n/tag (z/node child))]
        ;; Skip metadata nodes (^:private, ^:dynamic, ^String, etc.)
        (if (= :meta tag)
          ;; Meta node wraps the actual symbol — get the last child
          (let [inner (some-> child z/down z/rightmost z/string)]
            (or inner s))
          ;; Regular symbol
          (if (or (= :token tag) (= :symbol tag))
            s
            (recur (z/right child))))))))

(defn- extract-arglist
  "Get arglist from a defn form. Descends into :meta nodes so meta-tagged
   arglists like `^String [k]` are found."
  [zloc]
  (let [type-str (some-> zloc z/down z/string)]
    (when (forms/has-arglists? type-str)
      ;; Walk children to find first vector (the arglist)
      ;; Meta nodes (^String [k]) wrap the vector — descend into them
      (loop [child (some-> zloc z/down)]
        (when child
          (let [tag (n/tag (z/node child))]
            (cond
              (= :vector tag) (z/string child)
              (= :meta tag)   (let [inner (some-> child z/down z/rightmost)]
                                (if (and inner (= :vector (n/tag (z/node inner))))
                                  (z/string inner)
                                  (recur (z/right child))))
              :else           (recur (z/right child)))))))))

(defn- extract-dispatch
  "Source spelling of a `defmethod` dispatch value: the child after the name.

   The exact spelling is returned, not a normalized value, so a caller can copy
   it verbatim into an exact `{kind, name, dispatch}` owner form."
  ;; @spec MCP-OP-DISPATCH-001
  [zloc]
  (some-> zloc z/down z/right z/right z/string))

(defn attached-comment-start
  "Look backwards from a form's start line to find attached comment lines.
   Comments must be contiguous (no blank lines between them and the form)."
  [lines form-line]
  (let [idx (dec form-line)] ;; 0-indexed
    (loop [i (dec idx), comment-start form-line]
      (if (neg? i)
        comment-start
        (let [line (str/trim (nth lines i ""))]
          (if (str/starts-with? line ";")
            (recur (dec i) (inc i)) ;; 1-indexed line number
            comment-start))))))

(defn- file-extension [file]
  (let [s (str file)
        i (.lastIndexOf s ".")]
    (when (pos? i) (subs s (inc i)))))

(defn- reader-cond-require?
  "Is this zipper node a reader-conditional (#? or #?@) inside a :require?"
  [zloc]
  (and (= :reader-macro (some-> zloc z/node n/tag))
       (#{"?" "?@"} (some-> zloc z/down z/string))))

(defn- vectors-from-reader-cond
  "Extract require vectors from a reader-conditional node.
   Handles both #?(:clj [ns :as a]) and #?@(:cljs [[ns1 :as a] [ns2 :as b]])."
  [rcond-zloc]
  (let [splicing? (= "?@" (some-> rcond-zloc z/down z/string))
        pair-list (-> rcond-zloc z/down z/right)
        children  (->> (z/down pair-list)
                       (iterate z/right)
                       (take-while some?))
        pairs     (partition 2 children)]
    (mapcat (fn [[_platform-key v-zl]]
              (cond
                ;; #?@(:clj [[ns1 :as a] [ns2 :as b]]) — splice: v-zl is vector of vectors
                splicing?
                (->> (z/down v-zl)
                     (iterate z/right)
                     (take-while some?)
                     (filter z/vector?))

                ;; #?(:clj [ns :as a]) — single vector
                (z/vector? v-zl)
                [v-zl]

                :else nil))
            pairs)))

(defn extract-ns-requires
  "Extract require entries from a (ns ...) zipper location.
   Returns a vector of strings like [\"[clojure.string :as str]\"]
   or nil if no :require block found.
   Reader-conditional-aware: also extracts from #?(:clj [...]) and
   #?@(:cljs [[...]]) within the :require block."
  [ns-zloc]
  (when ns-zloc
    (let [require-form (->> (z/down ns-zloc)
                            (iterate z/right)
                            (take-while some?)
                            (filter #(and (z/list? %)
                                          (= ":require" (some-> % z/down z/string))))
                            first)]
      (when require-form
        (let [children (->> (z/down require-form)
                            (iterate z/right)
                            (take-while some?))
              ;; Direct vector children (shared requires)
              direct   (->> children
                            (filter z/vector?))
              ;; Reader-conditional children (platform-specific requires)
              rcond    (->> children
                            (filter reader-cond-require?)
                            (mapcat vectors-from-reader-cond))]
          (mapv z/string (concat direct rcond)))))))

(defn top-level-form-records
  "Return every parsed top-level list form in source order.

   Pure: filename, source string, and an explicit project-alias map in;
   records out. The two-argument arity uses no project aliases. Records include
   exact `:source` for structural readers. Public outline output removes that
   field to preserve the compact `:ls` contract."
  ([file source]
   (top-level-form-records file source {}))
  ([file source project-aliases]
   (let [lines (str/split-lines source)
         defaults (cwalk/platforms-for-extension (file-extension file))
         walked (cwalk/top-level-forms source defaults)]
     (mapv (fn [{:keys [zloc platforms]}]
             (let [node (z/node zloc)
                   m (meta node)
                   type-str (some-> zloc z/down z/string)
                   form-spec (forms/spec-with-project-aliases
                               project-aliases type-str)
                   user-fields (:fields form-spec)
                   extracted (when user-fields
                               (resolve-user-fields user-fields zloc
                                                    type-str (:row m)))
                   ;; If user provided :fields, respect their spec; don't fall
                   ;; back to legacy extractors for fields they omitted.
                   name-val (cond
                              user-fields (:name extracted)
                              (some? form-spec) (extract-name zloc))
                   arglist (cond
                             user-fields (:arglist extracted)
                             name-val (extract-arglist zloc))
                   dispatch (when (and name-val
                                       (= :defmethod (:kind form-spec)))
                              (extract-dispatch zloc))
                   form-line (:row m)
                   comment-start (when form-line
                                   (attached-comment-start lines form-line))
                   extras (when extracted
                            (dissoc extracted :name :arglist))]
               (cond-> {:type (symbol (or type-str "?"))
                        :platforms (vec (sort platforms))
                        :source (z/string zloc)}
                 form-line (assoc :line form-line)
                 (:end-row m) (assoc :end-line (:end-row m))
                 name-val (assoc :name (if (symbol? name-val)
                                         name-val
                                         (symbol (str name-val))))
                 arglist (assoc :args arglist)
                 dispatch (assoc :dispatch dispatch)
                 (seq extras) (merge extras)
                 (and form-line comment-start (< comment-start form-line))
                 (assoc :comment-start comment-start))))
           walked))))

(defn outline-source
  "Return the existing compact outline for a filename and source string.

   Pure counterpart to `outline`. Each public form includes platform data but
   not the complete `:source` retained by `top-level-form-records`."
  ([file source]
   (outline-source file source {}))
  ([file source project-aliases]
   (outline-source file source project-aliases {}))
  ([file source project-aliases {:keys [include-string-symbols]}]
   (let [records (top-level-form-records file source project-aliases)
         zloc (z/of-string source {:track-position? true})
         ns-zloc (some-> zloc
                         (z/find-value z/next 'ns)
                         z/up)
         ns-name (some-> ns-zloc z/down z/right z/string symbol)
         requires (extract-ns-requires ns-zloc)]
     {:ns ns-name
      :file file
      :lines (count (str/split-lines source))
      :form-count (count (filter :name records))
      :forms (->> records
                  (remove #(= 'ns (:type %)))
                  (mapv (fn [record]
                          (cond-> (dissoc record :source)
                            include-string-symbols
                            (merge
                              (let [{:keys [symbols truncated?]}
                                    (string-symbols-for-form record)]
                                {:string-symbols symbols
                                 :string-symbols-truncated truncated?}))))))
      :requires (or requires [])
      :forward-refs []})))

(defn outline
  "Return the compact outline of all top-level forms in a Clojure file.

   Thin I/O wrapper over `outline-source`. For CLJC files, reader-conditional
  forms retain the exact platform sets returned by the shared walker."
  [file]
  (outline-source file (slurp file) @forms/project-aliases))
