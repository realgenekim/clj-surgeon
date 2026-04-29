(ns clj-surgeon.cljc.require-ops
  "Platform-aware require operations on a CLJC source.

   Strategy: rather than write a CLJC-aware editor, we leverage the merge/split
   pair. To add a require at platform P:
     1. Split CLJC into CLJ + CLJS sources.
     2. Insert the new require into whichever side(s) need it.
     3. Merge back into a CLJC source. The merge classifier automatically
        places the new entry in the correct shared/one-sided/divergent slot.

   This composition keeps the implementation small and proves merge/split
   are real inverses, not just for the round-trip test."
  (:require [rewrite-clj.zip :as z]
            [rewrite-clj.node :as n]
            [clj-surgeon.cljc.merge :as merge]
            [clj-surgeon.cljc.split :as split]))

;; ============================================================
;; Single-file require insertion
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

(defn- entry-node
  "Build a rewrite-clj vector node like [foo.bar :as fb] or [\"npm-pkg\" :as p].
   String `ns-sym` produces an npm-style string literal; symbols produce a
   namespace token."
  [ns-sym alias]
  (let [head (cond
               (string? ns-sym) (n/string-node ns-sym)
               (symbol? ns-sym) (n/token-node ns-sym)
               :else            (n/token-node (symbol ns-sym)))
        parts (cond-> [head]
                alias (into [(n/spaces 1)
                             (n/keyword-node :as)
                             (n/spaces 1)
                             (n/token-node (symbol alias))]))]
    (n/vector-node parts)))

(defn- existing-alias-target
  "Return the ns symbol bound to alias `a` in src, or nil if no such alias.
   Used to detect collisions before inserting a new require."
  [src a]
  (when a
    (let [ns-zl (ns-zloc src)
          req-zl (and ns-zl (require-form-zloc ns-zl))]
      (when req-zl
        (some (fn [v-zl]
                (let [s (n/sexpr (z/node v-zl))
                      idx (.indexOf (vec s) :as)]
                  (when (and (pos? idx)
                             (= (str (nth s (inc idx))) (str a)))
                    (first s))))
              (->> (z/down req-zl)
                   (iterate z/right)
                   (take-while some?)
                   (filter z/vector?)))))))

(defn- check-alias-collision! [src target-ns alias platform]
  (when alias
    (when-let [existing (existing-alias-target src alias)]
      (when (not= (str existing) (str target-ns))
        (throw (ex-info (str "Alias '" alias "' is already bound to a different "
                             "namespace on the " platform " side")
                        {:platform platform
                         :alias alias
                         :existing-ns existing
                         :requested-ns target-ns}))))))

(defn insert-into-require
  "Add a single [ns-sym :as alias] entry to the (:require ...) block of src.
   If src has no :require form, one is created. Returns updated source string."
  [src ns-sym alias]
  (let [ns-zl (ns-zloc src)
        _ (when (nil? ns-zl)
            (throw (ex-info "Cannot insert require: source has no ns form" {})))
        req-zl (require-form-zloc ns-zl)
        new-entry (entry-node ns-sym alias)]
    (if req-zl
      (-> req-zl
          (z/append-child (n/newlines 1))
          (z/append-child (n/spaces 3))
          (z/append-child new-entry)
          z/root-string)
      ;; No :require form yet — append one to the ns form.
      (-> ns-zl
          (z/append-child (n/newlines 1))
          (z/append-child (n/spaces 2))
          (z/append-child (n/list-node [(n/keyword-node :require)
                                        (n/newlines 1)
                                        (n/spaces 3)
                                        new-entry]))
          z/root-string))))

;; ============================================================
;; Public API: CLJC-level require ops
;; ============================================================

(defn add-require
  "Add a require to a CLJC source at the given platform.

   opts: {:platform :clj | :cljs | :cljc
          :ns       'foo.bar
          :as       'fb (optional)}"
  [cljc-src {:keys [platform ns as] :as _opts}]
  (when-not (#{:clj :cljs :cljc} platform)
    (throw (ex-info "platform must be :clj, :cljs, or :cljc" {:got platform})))
  (let [{clj :clj cljs :cljs} (split/split-file cljc-src)]
    (when (#{:clj :cljc} platform)  (check-alias-collision! clj  ns as :clj))
    (when (#{:cljs :cljc} platform) (check-alias-collision! cljs ns as :cljs))
    (let [clj-out  (if (#{:clj :cljc} platform)  (insert-into-require clj  ns as) clj)
          cljs-out (if (#{:cljs :cljc} platform) (insert-into-require cljs ns as) cljs)]
      (merge/merge-files clj-out cljs-out))))
