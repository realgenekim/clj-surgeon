(ns clj-surgeon.cljc.analyze
  "High-level CLJC analysis for LLM consumption.

   Produces a normalized EDN map describing a CLJ+CLJS pair (or a single CLJC
   file). The map covers require classification, body-form pairing, and
   reader-conditional surface area — everything an LLM needs to decide which
   surgical operation to apply next.

   Pure: takes string(s), returns data."
  (:require [rewrite-clj.zip :as z]
            [rewrite-clj.node :as n]
            [clj-surgeon.cljc.merge :as merge]
            [clj-surgeon.cljc.split :as split]
            [clj-surgeon.cljc.walk  :as cwalk]))

(defn- require-infos [src]
  (let [ns-zl (merge/parse-ns-form src)]
    (when ns-zl
      (mapv merge/vec-node->info (or (merge/require-vector-nodes ns-zl) [])))))

(defn- summarize-vec-info [v]
  (cond-> {:ns (let [k (:ns v)] (if (string? k) k (str k)))}
    (:alias v) (assoc :as (:alias v))))

(defn- top-level-form-summary
  "Compact summary of a top-level form: type + name + line + platforms."
  [{:keys [zloc platforms]}]
  (let [head (some-> zloc z/down z/string)
        nm   (let [second (some-> zloc z/down z/right)]
               (when second
                 (if (= :meta (some-> second z/node n/tag))
                   (some-> second z/down z/rightmost z/string)
                   (z/string second))))
        m    (meta (z/node zloc))]
    (cond-> {:type head
             :platforms (vec (sort platforms))}
      nm (assoc :name nm)
      (:row m) (assoc :line (:row m)))))

(defn- analyze-pair*
  "Internal: takes already-parsed CLJ source string + CLJS source string."
  [clj-src cljs-src]
  (let [clj-reqs   (require-infos clj-src)
        cljs-reqs  (require-infos cljs-src)
        classified (merge/classify-requires clj-reqs cljs-reqs)
        ;; Body forms surfaced through the cljc walker so reader conditionals
        ;; embedded INSIDE a CLJC source (after split) come through with
        ;; per-platform tagging too.
        clj-forms  (cwalk/top-level-forms clj-src  #{:clj})
        cljs-forms (cwalk/top-level-forms cljs-src #{:cljs})]
    {:requires
     {:shared    (mapv summarize-vec-info (:shared classified))
      :clj-only  (mapv summarize-vec-info (:clj-only classified))
      :cljs-only (mapv summarize-vec-info (:cljs-only classified))
      :divergent (mapv (fn [{c :clj cs :cljs}]
                         {:clj  (summarize-vec-info c)
                          :cljs (summarize-vec-info cs)})
                       (:divergent classified))}
     :forms-clj  (mapv top-level-form-summary
                       (remove (fn [{:keys [zloc]}]
                                 (= "ns" (some-> zloc z/down z/string)))
                               clj-forms))
     :forms-cljs (mapv top-level-form-summary
                       (remove (fn [{:keys [zloc]}]
                                 (= "ns" (some-> zloc z/down z/string)))
                               cljs-forms))}))

(defn analyze-pair
  "Analyze a parallel CLJ + CLJS pair (same ns). Returns a map with
   classified requires and per-platform top-level form summaries.
   Suitable for LLM consumption to plan a merge or surgical edit."
  [clj-src cljs-src]
  (analyze-pair* clj-src cljs-src))

(defn analyze-cljc
  "Analyze a single CLJC source by splitting and reusing analyze-pair."
  [cljc-src]
  (let [{:keys [clj cljs]} (split/split-file cljc-src)]
    (assoc (analyze-pair* clj cljs)
           :input :cljc)))
