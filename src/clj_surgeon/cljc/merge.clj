(ns clj-surgeon.cljc.merge
  "Merge parallel CLJ + CLJS source (same ns) into a single CLJC source.

   Pure: takes two strings, returns a string. No I/O.

   v1 scope:
   - Requires (shared / one-sided / divergent-alias / npm string requires)
   - Post-ns body forms: identical-by-string → shared;
     differing-by-string → wrapped in #?(:clj ... :cljs ...) at the form level
   - ns sub-forms beyond :require (i.e. :import, :refer-clojure, :gen-class, etc.)
     and ns metadata/docstrings are NOT yet supported and will throw.

   Symbol-collision rules (override the default reader-conditional split with a
   project-supplied rewrite — e.g. RPC-style rename-and-tag) live in a sibling
   namespace and consume the same body-pairing data structure."
  (:require [rewrite-clj.zip :as z]
            [rewrite-clj.node :as n]
            [clojure.string :as str]))

;; ============================================================
;; ns / require parsing
;; ============================================================

(defn parse-ns-form
  "Return the (ns ...) zloc, or nil if absent. Public for analyze."
  [src]
  (let [zloc (z/of-string src {:track-position? true})]
    (->> zloc
         (iterate z/right)
         (take-while some?)
         (filter #(and (z/list? %)
                       (= "ns" (some-> % z/down z/string))))
         first)))

(defn- ns-children
  "Direct child zlocs of the ns form, after the ns symbol and ns name."
  [ns-zloc]
  (->> (-> ns-zloc z/down z/right z/right)
       (iterate z/right)
       (take-while some?)))

(defn- assert-supported-ns!
  "v1 limitations:
   - No ns docstring (string child) — would be silently dropped.
   - No ns metadata map (map child).
   - Among list children, only :require is supported."
  [ns-zloc which]
  (doseq [child (ns-children ns-zloc)]
    (let [tag (some-> child z/node n/tag)]
      (cond
        ;; rewrite-clj exposes string literals as :token nodes whose sexpr
        ;; is a String. Detect ns docstrings this way.
        (and (= :token tag) (string? (n/sexpr (z/node child))))
        (throw (ex-info "ns docstring not supported in v1 (would be lost)"
                        {:platform which}))

        (#{:multi-line :string} tag)
        (throw (ex-info "ns docstring not supported in v1 (would be lost)"
                        {:platform which}))

        (= :map tag)
        (throw (ex-info "ns attr-map not supported in v1 (would be lost)"
                        {:platform which}))

        (z/list? child)
        (let [head (some-> child z/down z/string)]
          (when-not (= ":require" head)
            (throw (ex-info (str "ns sub-form '" head "' not supported in v1")
                            {:platform which :sub-form head}))))))))

(defn require-vector-nodes
  "Return the rewrite-clj nodes for each [ns :as alias ...] vector inside :require."
  [ns-zloc]
  (when ns-zloc
    (let [require-form (->> (ns-children ns-zloc)
                            (filter #(and (z/list? %)
                                          (= ":require" (some-> % z/down z/string))))
                            first)]
      (when require-form
        (->> (z/down require-form)
             (iterate z/right)
             (take-while some?)
             (filter z/vector?)
             (mapv z/node))))))

(defn vec-node->info
  "{:ns sym-or-string :alias str-or-nil :node node :sexpr sexpr}.
   `:ns` is a symbol for [foo.bar :as fb] or a string for [\"npm-pkg\" :refer [X]].
   Public for use by clj-surgeon.cljc.analyze."
  [v-node]
  (let [sexpr  (n/sexpr v-node)
        ns-key (first sexpr)
        as-i   (.indexOf (vec sexpr) :as)
        alias  (when (pos? as-i) (str (nth sexpr (inc as-i))))]
    {:ns ns-key :alias alias :node v-node :sexpr sexpr}))

;; ============================================================
;; Classification of requires
;; ============================================================

(defn classify-requires
  "Classify two require-info vectors into shared / one-sided / divergent
   (alias and ns) groups. Public for use by clj-surgeon.cljc.analyze."
  [clj-vecs cljs-vecs]
  (let [by-ns-clj     (into {} (map (juxt :ns identity) clj-vecs))
        by-ns-cljs    (into {} (map (juxt :ns identity) cljs-vecs))
        by-alias-clj  (into {} (keep (fn [r] (when (:alias r) [(:alias r) r])) clj-vecs))
        by-alias-cljs (into {} (keep (fn [r] (when (:alias r) [(:alias r) r])) cljs-vecs))
        all-aliases   (set (concat (keys by-alias-clj) (keys by-alias-cljs)))
        ;; Alias appears on both sides but bound to different namespaces.
        divergent-alias-set
        (set (for [a all-aliases
                   :let [c (get by-alias-clj a)
                         s (get by-alias-cljs a)]
                   :when (and c s (not= (:ns c) (:ns s)))]
               a))
        alias-divergent? (fn [r] (contains? divergent-alias-set (:alias r)))
        clj-vecs'  (remove alias-divergent? clj-vecs)
        cljs-vecs' (remove alias-divergent? cljs-vecs)
        ;; Same ns key on both sides but the entry vectors aren't structurally equal
        ;; — typically diverging :refer / :refer-macros / :include-macros / :rename.
        ns-divergent-set
        (set (for [r clj-vecs'
                   :let [m (get by-ns-cljs (:ns r))]
                   :when (and m (not= (:sexpr r) (:sexpr m)))]
               (:ns r)))
        ns-divergent? (fn [r] (contains? ns-divergent-set (:ns r)))
        shared     (filter (fn [r]
                             (when-let [m (get by-ns-cljs (:ns r))]
                               (= (:sexpr r) (:sexpr m))))
                           clj-vecs')
        clj-only   (remove (fn [r] (or (ns-divergent? r)
                                       (contains? by-ns-cljs (:ns r))))
                           clj-vecs')
        cljs-only  (remove (fn [r] (or (ns-divergent? r)
                                       (contains? by-ns-clj (:ns r))))
                           cljs-vecs')
        alias-divergent (for [a (sort divergent-alias-set)]
                          {:clj  (get by-alias-clj  a)
                           :cljs (get by-alias-cljs a)})
        ns-divergent    (for [k (sort-by str ns-divergent-set)]
                          {:clj  (get by-ns-clj k)
                           :cljs (get by-ns-cljs k)})]
    {:shared    (vec shared)
     :clj-only  (vec clj-only)
     :cljs-only (vec cljs-only)
     :divergent (vec (concat alias-divergent ns-divergent))}))

;; ============================================================
;; Body-form parsing
;; ============================================================

(defn- top-level-zlocs [src]
  (let [zloc (z/of-string src {:track-position? true})]
    (->> zloc
         (iterate z/right)
         (take-while some?))))

(defn- post-ns-form-strings
  "All top-level form strings AFTER the ns form."
  [src]
  (let [forms (top-level-zlocs src)
        after-ns (drop-while #(not (and (z/list? %)
                                        (= "ns" (some-> % z/down z/string))))
                             forms)]
    (when (seq after-ns)
      (mapv z/string (rest after-ns)))))

;; ============================================================
;; Emit
;; ============================================================

(defn- emit-vec [v-info]
  (n/string (:node v-info)))

(defn- ns-key-string
  "String key used to sort require entries within a platform branch.
   Symbols and strings (npm) are both supported; npm strings sort
   lexicographically against symbol-names."
  [v-info]
  (let [k (:ns v-info)]
    (if (string? k) k (str k))))

(defn- emit-platform-splice
  [plat-clj plat-cljs]
  (let [indent "   "
        clj-vecs  (sort-by ns-key-string plat-clj)
        cljs-vecs (sort-by ns-key-string plat-cljs)
        clj-strs  (map emit-vec clj-vecs)
        cljs-strs (map emit-vec cljs-vecs)]
    (cond
      (and (seq plat-clj) (seq plat-cljs))
      (str indent "#?@(:clj  [" (str/join " " clj-strs) "]\n"
           indent "    :cljs [" (str/join "\n              " cljs-strs) "])")

      (seq plat-clj)
      (str indent "#?@(:clj [" (str/join " " clj-strs) "])")

      (seq plat-cljs)
      (str indent "#?@(:cljs [" (str/join " " cljs-strs) "])")

      :else nil)))

(defn- emit-require-block
  [{:keys [shared clj-only cljs-only divergent]}]
  (let [indent "   "
        shared-sorted (sort-by ns-key-string shared)
        shared-lines (map (fn [v] (str indent (emit-vec v))) shared-sorted)
        plat-clj  (concat (map :clj divergent) clj-only)
        plat-cljs (concat (map :cljs divergent) cljs-only)
        platform-block (emit-platform-splice plat-clj plat-cljs)
        body-lines (cond-> (vec shared-lines)
                     platform-block (conj platform-block))]
    (when (seq body-lines)
      (str "(:require\n"
           (str/join "\n" body-lines)
           ")"))))

(defn- emit-body-form
  "Pair of (clj-form-string, cljs-form-string).
   Equal → return the form unchanged.
   Different → return a #?(:clj ... :cljs ...) reader conditional."
  [clj-s cljs-s]
  (if (= clj-s cljs-s)
    clj-s
    (str "#?(:clj  " clj-s "\n"
         "   :cljs " cljs-s ")")))

(defn- emit-strict-body-split
  "Fallback when body-form counts don't align: dump each side's entire body
   into a single splicing reader conditional. The output is mechanically
   correct — semantically equivalent to the two original files — but the LLM
   may want to refactor to per-form `#?(:clj X :cljs Y)` afterwards."
  [clj-bodies cljs-bodies]
  (let [clj-block  (str/join "\n\n             " clj-bodies)
        cljs-block (str/join "\n\n             " cljs-bodies)]
    (cond
      (and (seq clj-bodies) (seq cljs-bodies))
      [(str "#?@(:clj  [" clj-block "]\n"
            "    :cljs [" cljs-block "])")]

      (seq clj-bodies)
      [(str "#?@(:clj [" clj-block "])")]

      (seq cljs-bodies)
      [(str "#?@(:cljs [" cljs-block "])")]

      :else [])))

(defn- emit-bodies [clj-bodies cljs-bodies]
  (if (= (count clj-bodies) (count cljs-bodies))
    (mapv emit-body-form clj-bodies cljs-bodies)
    (emit-strict-body-split clj-bodies cljs-bodies)))

;; ============================================================
;; Public API
;; ============================================================

(defn merge-files
  "Take CLJ source string and CLJS source string (same ns), return CLJC source string."
  [clj-src cljs-src]
  (let [clj-ns   (parse-ns-form clj-src)
        cljs-ns  (parse-ns-form cljs-src)
        _ (when (or (nil? clj-ns) (nil? cljs-ns))
            (throw (ex-info "Both inputs must contain an ns form" {})))
        _ (assert-supported-ns! clj-ns :clj)
        _ (assert-supported-ns! cljs-ns :cljs)
        ns-name (-> clj-ns z/down z/right z/string)
        ns-name-cljs (-> cljs-ns z/down z/right z/string)
        _ (when-not (= ns-name ns-name-cljs)
            (throw (ex-info "ns names differ" {:clj ns-name :cljs ns-name-cljs})))
        clj-reqs  (mapv vec-node->info (require-vector-nodes clj-ns))
        cljs-reqs (mapv vec-node->info (require-vector-nodes cljs-ns))
        classed   (classify-requires clj-reqs cljs-reqs)
        require-block (emit-require-block classed)
        bodies (emit-bodies (or (post-ns-form-strings clj-src) [])
                            (or (post-ns-form-strings cljs-src) []))
        ns-line (if require-block
                  (str "(ns " ns-name "\n  " require-block ")")
                  (str "(ns " ns-name ")"))
        body-text (when (seq bodies)
                    (str "\n\n" (str/join "\n\n" bodies)))]
    (str ns-line body-text "\n")))
