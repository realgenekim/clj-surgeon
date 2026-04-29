(ns clj-surgeon.cljc.walk
  "Reader-conditional-aware top-level walker.

   Pre-existing ops (outline, intra-ns-deps, mv, extract, fix-declares) walk
   top-level via `z/right` and only consider `(z/list? zloc)`. Forms wrapped in
   `#?(:clj ...)` / `#?@(:clj [...])` are reader-macro nodes (not lists) and
   are silently skipped — meaning a CLJC file's CLJ-only `(defn ...)` is
   invisible to the rest of the toolchain.

   This namespace provides one helper that yields every top-level *form* in a
   CLJ/CLJS/CLJC source paired with the set of platforms it lives under.

   Pure: takes a string + the all-platforms set, returns data."
  (:require [rewrite-clj.zip :as z]
            [rewrite-clj.node :as n]))

(def ^:private all-platforms-by-ext
  {"clj"  #{:clj}
   "cljs" #{:cljs}
   "cljc" #{:clj :cljs}})

(defn platforms-for-extension
  "Map a file extension (without dot) to the default platform set for forms
   that sit OUTSIDE any reader conditional in that file."
  [ext]
  (or (get all-platforms-by-ext ext) #{:clj :cljs}))

(defn- reader-cond? [zloc]
  (and (= :reader-macro (some-> zloc z/node n/tag))
       (#{"?" "?@"} (some-> zloc z/down z/string))))

(defn- splicing? [zloc]
  (= "?@" (some-> zloc z/down z/string)))

(defn- platform-pairs
  "Return [[platform-keyword value-zloc], ...] for a reader-conditional zloc.
   E.g. #?(:clj X :cljs Y) → [[:clj X-zloc] [:cljs Y-zloc]]."
  [rmacro-zloc]
  (let [pair-list (-> rmacro-zloc z/down z/right)
        children (->> (z/down pair-list)
                      (iterate z/right)
                      (take-while some?))]
    (->> children
         (partition 2)
         (mapv (fn [[k v]] [(keyword (subs (z/string k) 1)) v])))))

(defn- yield-from-value
  "Given a value-zloc that follows :clj/:cljs/etc., yield the inner top-level
   list forms tagged with the given platform set."
  [v-zloc platforms splicing?]
  (cond
    splicing?
    (->> (z/down v-zloc)
         (iterate z/right)
         (take-while some?)
         (filter z/list?)
         (mapv (fn [z] {:zloc z :platforms platforms})))

    (z/list? v-zloc)
    [{:zloc v-zloc :platforms platforms}]

    :else
    []))

(defn top-level-forms
  "Walk every top-level form in `src`, descending into reader conditionals.
   Returns vector of {:zloc <list-zloc>, :platforms <set>}.

   `default-platforms` is the platform set assigned to forms that sit OUTSIDE
   any reader conditional. Use `(platforms-for-extension ext)` to derive it
   from a file extension, or pass an explicit set."
  [src default-platforms]
  (let [zl (z/of-string src {:track-position? true})]
    (loop [z zl out []]
      (cond
        (nil? z) out

        (reader-cond? z)
        (let [pairs (platform-pairs z)
              splicing (splicing? z)
              new-forms (mapcat (fn [[plat v]]
                                  (yield-from-value v #{plat} splicing))
                                pairs)]
          (recur (z/right z) (into out new-forms)))

        (z/list? z)
        (recur (z/right z) (conj out {:zloc z :platforms default-platforms}))

        :else
        (recur (z/right z) out)))))
