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
                 {:error (str "Unknown op: " op
                              ". Valid ops: :ls, :mv, :declares, :deps, :topo, :closure, :rename-ns, :fix-declares")})]
    (pp/pprint result)))

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
