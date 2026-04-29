(ns clj-surgeon.core
  "ns-surgeon: structural operations on Clojure namespaces.

   A babashka CLI tool. Returns EDN.

   Usage:
     bb -m ns-surgeon.core :op :outline :file src/my/ns.clj
     bb -m ns-surgeon.core :op :mv :file src/my/ns.clj :form my-fn :before other-fn
     bb -m ns-surgeon.core :op :mv :file src/my/ns.clj :form my-fn :before other-fn :dry-run true"
  (:require [clj-surgeon.outline :as outline]
            [clj-surgeon.forward-refs :as fwd]
            [clj-surgeon.move :as move]
            [clj-surgeon.analyze :as analyze]
            [clj-surgeon.rename :as rename]
            [clj-surgeon.fix-declares :as fix-declares]
            [clj-surgeon.extract :as extract]
            [clj-surgeon.cljc.merge :as cljc-merge]
            [clj-surgeon.cljc.split :as cljc-split]
            [clj-surgeon.cljc.require-ops :as cljc-req]
            [clj-surgeon.cljc.analyze :as cljc-analyze]
            [clojure.pprint :as pp]))

(defn run-outline [{:keys [file]}]
  (let [result (outline/outline file)
        ns-name (:ns result)
        forward-refs (when ns-name
                       (fwd/detect-forward-refs file ns-name))]
    (assoc result :forward-refs (or forward-refs []))))

(defn run-mv [{:keys [file form before dry-run] :as opts}]
  (move/move-form opts))

(defn run-declares [{:keys [file]}]
  (let [;; Get declares from the OUTLINE (not deps — deps excludes declares)
        ol (outline/outline file)
        declares (->> (:forms ol)
                      (filter #(= 'declare (:type %))))
        ;; Use topo sort to find genuine cycles
        zloc (analyze/file->zloc file)
        topo (analyze/topological-sort zloc)
        truly-cyclic (set (:cycles topo))
        ;; Also check forward-refs to see which declares are still needed
        fwd (when (:ns ol)
              (set (map #(str (:name %))
                        (fwd/detect-forward-refs file (:ns ol)))))]
    {:file file
     :declares
     (mapv (fn [d]
             (let [name-str (str (:name d))
                   has-forward-ref? (contains? fwd name-str)
                   in-cycle? (contains? truly-cyclic name-str)]
               {:name name-str
                :line (:line d)
                :needed? (or in-cycle? has-forward-ref?)}))
           declares)
     :summary {:total (count declares)
               :removable (count (remove #(or (contains? truly-cyclic (str (:name %)))
                                              (contains? fwd (str (:name %))))
                                         declares))
               :needed (count (filter #(or (contains? truly-cyclic (str (:name %)))
                                           (contains? fwd (str (:name %))))
                                      declares))}}))

(defn run-deps [{:keys [file form]}]
  (let [zloc (analyze/file->zloc file)
        deps (analyze/intra-ns-deps zloc)]
    (if form
      (first (filter #(= form (:name %)) deps))
      deps)))

(defn run-topo [{:keys [file]}]
  (let [zloc (analyze/file->zloc file)]
    (analyze/topological-sort zloc)))

(defn run-closure [{:keys [file form]}]
  (let [zloc (analyze/file->zloc file)]
    (analyze/extraction-closure zloc form)))

(defn run-ls-deps [{:keys [file form]}]
  (let [zloc (analyze/file->zloc file)
        deps (analyze/intra-ns-deps zloc)]
    (analyze/dep-tree deps form)))

;; ============================================================
;; CLJC operations: merge, split, add-require
;; ============================================================

(defn run-cljc-merge
  "Merge parallel CLJ + CLJS files (same ns) into a single CLJC source.
   :clj  / :cljs — input file paths (required)
   :out — optional output path; omitted prints to stdout."
  [{:keys [clj cljs out] :as _opts}]
  (let [cljc-src (cljc-merge/merge-files (slurp clj) (slurp cljs))]
    (if out
      (do (spit out cljc-src)
          {:wrote out :bytes (count cljc-src)})
      cljc-src)))

(defn run-cljc-split
  "Split a CLJC file into parallel CLJ + CLJS sources.
   :file     — input CLJC path (required)
   :clj-out  — optional output CLJ path
   :cljs-out — optional output CLJS path
   When out paths are omitted, returns both contents in a map."
  [{:keys [file clj-out cljs-out] :as _opts}]
  (let [{:keys [clj cljs] :as result} (cljc-split/split-file (slurp file))]
    (cond-> result
      clj-out  (do (spit clj-out clj)   (assoc :wrote-clj clj-out))
      cljs-out (do (spit cljs-out cljs) (assoc :wrote-cljs cljs-out)))))

(defn run-cljc-add-require
  "Add a require to a CLJC file at the given platform.
   :file     — input CLJC path (required)
   :platform — :clj | :cljs | :cljc (required)
   :ns       — namespace symbol to require (required)
   :as       — optional alias
   :out      — optional output path; omitted prints to stdout."
  [{:keys [file platform ns as out] :as _opts}]
  (let [updated (cljc-req/add-require (slurp file)
                                      {:platform platform
                                       :ns ns
                                       :as as})]
    (if out
      (do (spit out updated)
          {:wrote out :bytes (count updated)})
      updated)))

(defn run [{:keys [op] :as opts}]
  (let [result (case op
                 :ls (run-outline opts)
                 :outline (run-outline opts)
                 :mv (run-mv opts)
                 :declares (run-declares opts)
                 :deps (run-deps opts)
                 :topo (run-topo opts)
                 :ls-extract (run-closure opts)
                 :ls-deps (run-ls-deps opts)
                 :rename-ns (rename/plan opts)
                 :rename-ns! (rename/execute! opts)
                 :fix-declares (fix-declares/plan (:file opts))
                 :fix-declares! (fix-declares/execute! (:file opts))
                 :extract (extract/plan opts)
                 :extract! (extract/execute! opts)
                 :cljc-merge (run-cljc-merge opts)
                 :cljc-split (run-cljc-split opts)
                 :cljc-add-require (run-cljc-add-require opts)
                 :cljc-analyze (cond
                                 (:file opts) (cljc-analyze/analyze-cljc (slurp (:file opts)))
                                 (and (:clj opts) (:cljs opts))
                                 (cljc-analyze/analyze-pair (slurp (:clj opts))
                                                            (slurp (:cljs opts)))
                                 :else {:error "supply :file or :clj + :cljs"})
                 {:error (str "Unknown op: " op
                              ". Valid ops: :ls, :mv, :declares, :deps, :topo, :closure, :rename-ns, :fix-declares, :cljc-merge, :cljc-split, :cljc-add-require, :cljc-analyze")})]
    (if (string? result)
      (println result)
      (pp/pprint result))))

(defn- parse-val [s]
  (cond
    (= s "true") true
    (= s "false") false
    (.startsWith s ":") (keyword (subs s 1))
    (.startsWith s "[") (read-string s)  ;; parse EDN vectors like '[foo bar]
    (.startsWith s "{") (read-string s)  ;; parse EDN maps
    :else s))

(defn- parse-args [args]
  (->> args
       (partition 2)
       (map (fn [[k v]] [(keyword (subs k 1)) (parse-val v)]))
       (into {})))

(defn -main [& args]
  (run (parse-args args)))
