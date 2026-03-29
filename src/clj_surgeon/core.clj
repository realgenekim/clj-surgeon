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
  (let [zloc (analyze/file->zloc file)
        deps (analyze/intra-ns-deps zloc)
        declares (filter #(= "declare" (:type %)) deps)
        topo (analyze/topological-sort zloc)
        truly-cyclic (set (:cycles topo))]
    {:file file
     :declares
     (mapv (fn [d]
             {:name (:name d)
              :line (:line d)
              :needed? (contains? truly-cyclic (:name d))})
           declares)
     :summary {:total (count declares)
               :removable (count (remove #(contains? truly-cyclic (:name %)) declares))
               :needed (count (filter #(contains? truly-cyclic (:name %)) declares))}}))

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

(defn run [{:keys [op] :as opts}]
  (let [result (case op
                 :ls (run-outline opts)
                 :outline (run-outline opts)
                 :mv (run-mv opts)
                 :declares (run-declares opts)
                 :deps (run-deps opts)
                 :topo (run-topo opts)
                 :closure (run-closure opts)
                 :rename-ns (rename/plan opts)
                 :rename-ns! (rename/execute! opts)
                 {:error (str "Unknown op: " op
                              ". Valid ops: :outline, :mv, :declares, :deps, :topo, :closure, :rename-ns, :rename-ns!")})]
    (pp/pprint result)))

(defn- parse-val [s]
  (cond
    (= s "true") true
    (= s "false") false
    (.startsWith s ":") (keyword (subs s 1))
    :else s))

(defn- parse-args [args]
  (->> args
       (partition 2)
       (map (fn [[k v]] [(keyword (subs k 1)) (parse-val v)]))
       (into {})))

(defn -main [& args]
  (run (parse-args args)))
