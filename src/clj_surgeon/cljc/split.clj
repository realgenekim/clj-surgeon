(ns clj-surgeon.cljc.split
  "Split a single CLJC source into parallel CLJ + CLJS sources.

   Pure: takes a string, returns {:clj clj-src, :cljs cljs-src}. No I/O.

   Inverse of merge:
   - Top-level reader conditionals split per platform.
   - Inside (ns ... (:require ...)), splicing reader conditionals split into
     per-platform require entries.
   - All other forms appear in both outputs unchanged.

   v1 limitation: only :require is supported among ns sub-forms.
   Forms inside #?(:cljc ...) are not yet handled (rare)."
  (:require [rewrite-clj.zip :as z]
            [rewrite-clj.node :as n]
            [clojure.string :as str]))

;; ============================================================
;; Reader-conditional decoding
;; ============================================================

(defn- reader-cond?
  "True if the zloc is a #?(...) or #?@(...) reader macro."
  [zloc]
  (and (= :reader-macro (some-> zloc z/node n/tag))
       (#{"?" "?@"} (some-> zloc z/down z/string))))

(defn- splicing-reader-cond? [zloc]
  (and (reader-cond? zloc)
       (= "?@" (some-> zloc z/down z/string))))

(defn- decode-reader-cond
  "Return {:clj [form-strings], :cljs [form-strings], :splicing? bool}.
   For #?(:clj X :cljs Y) the strings are [\"X\"] and [\"Y\"].
   For #?@(:clj [a b] :cljs [c d]) the strings are [\"a\" \"b\"] and [\"c\" \"d\"]
   (i.e. the spliced contents)."
  [rmacro-zloc]
  (let [splicing? (splicing-reader-cond? rmacro-zloc)
        ;; The pair-list is the second child of the reader-macro (the list
        ;; following ? or ?@).
        pair-list (-> rmacro-zloc z/down z/right)
        children (->> (z/down pair-list)
                      (iterate z/right)
                      (take-while some?))
        pairs (partition 2 children)
        as-strings (fn [zl]
                     (if splicing?
                       ;; zl is a vector zloc; return strings of its elements
                       (->> (z/down zl)
                            (iterate z/right)
                            (take-while some?)
                            (mapv z/string))
                       [(z/string zl)]))
        platform-map (into {} (for [[k v] pairs]
                                [(keyword (subs (z/string k) 1)) (as-strings v)]))]
    (assoc platform-map :splicing? splicing?)))

;; ============================================================
;; ns / require splitting
;; ============================================================

(defn- ns-zloc [src]
  (let [zl (z/of-string src {:track-position? true})]
    (->> zl
         (iterate z/right)
         (take-while some?)
         (filter #(and (z/list? %)
                       (= "ns" (some-> % z/down z/string))))
         first)))

(defn- require-form-zloc [ns-zl]
  (->> (-> ns-zl z/down z/right z/right)
       (iterate z/right)
       (take-while some?)
       (filter #(and (z/list? %)
                     (= ":require" (some-> % z/down z/string))))
       first))

(defn- split-require-block
  "Walk a :require form and return {:clj [require-strings], :cljs [require-strings]}
   where each string is one require entry like \"[ns :as alias]\"."
  [require-zl]
  (loop [loc (z/down require-zl)
         clj  []
         cljs []]
    (cond
      (nil? loc) {:clj clj :cljs cljs}

      (z/vector? loc)
      (let [s (z/string loc)]
        (recur (z/right loc) (conj clj s) (conj cljs s)))

      (reader-cond? loc)
      (let [{c :clj cs :cljs} (decode-reader-cond loc)]
        (recur (z/right loc)
               (into clj (or c []))
               (into cljs (or cs []))))

      :else (recur (z/right loc) clj cljs))))

;; ============================================================
;; Top-level walk
;; ============================================================

(defn- emit-ns-form [ns-name require-strs]
  (if (seq require-strs)
    (str "(ns " ns-name "\n"
         "  (:require\n"
         (str/join "\n" (map #(str "   " %) require-strs))
         "))")
    (str "(ns " ns-name ")")))

(defn- ns-name-of [ns-zl]
  (-> ns-zl z/down z/right z/string))

(defn split-file
  "Split a CLJC source string into {:clj clj-src, :cljs cljs-src}."
  [cljc-src]
  (let [ns-zl (ns-zloc cljc-src)
        _ (when (nil? ns-zl)
            (throw (ex-info "Input must contain an ns form" {})))
        ns-nm (ns-name-of ns-zl)
        req-zl (require-form-zloc ns-zl)
        {clj-reqs :clj cljs-reqs :cljs} (if req-zl
                                          (split-require-block req-zl)
                                          {:clj [] :cljs []})
        clj-ns-text  (emit-ns-form ns-nm clj-reqs)
        cljs-ns-text (emit-ns-form ns-nm cljs-reqs)
        ;; Walk top-level forms AFTER the ns; split reader conditionals.
        top-forms (->> (z/of-string cljc-src {:track-position? true})
                       (iterate z/right)
                       (take-while some?))
        after-ns  (rest (drop-while #(not (and (z/list? %)
                                               (= "ns" (some-> % z/down z/string))))
                                    top-forms))
        clj-bodies  (atom [])
        cljs-bodies (atom [])]
    (doseq [zl after-ns]
      (cond
        (reader-cond? zl)
        (let [{c :clj cs :cljs} (decode-reader-cond zl)]
          (when (seq c) (swap! clj-bodies into c))
          (when (seq cs) (swap! cljs-bodies into cs)))

        :else
        (let [s (z/string zl)]
          (swap! clj-bodies conj s)
          (swap! cljs-bodies conj s))))
    {:clj  (str clj-ns-text
                (when (seq @clj-bodies) (str "\n\n" (str/join "\n\n" @clj-bodies)))
                "\n")
     :cljs (str cljs-ns-text
                (when (seq @cljs-bodies) (str "\n\n" (str/join "\n\n" @cljs-bodies)))
                "\n")}))
