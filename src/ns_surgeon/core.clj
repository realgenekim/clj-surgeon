(ns ns-surgeon.core
  "ns-surgeon: structural operations on Clojure namespaces.

   A babashka CLI tool. Returns EDN.

   Usage:
     bb -m ns-surgeon.core :op :outline :file src/my/ns.clj
     bb -m ns-surgeon.core :op :mv :file src/my/ns.clj :form my-fn :before other-fn
     bb -m ns-surgeon.core :op :mv :file src/my/ns.clj :form my-fn :before other-fn :dry-run true"
  (:require [ns-surgeon.outline :as outline]
            [ns-surgeon.forward-refs :as fwd]
            [ns-surgeon.move :as move]
            [clojure.pprint :as pp]))

(defn run-outline [{:keys [file]}]
  (let [result (outline/outline file)
        ns-name (:ns result)
        forward-refs (when ns-name
                       (fwd/detect-forward-refs file ns-name))]
    (assoc result :forward-refs (or forward-refs []))))

(defn run-mv [{:keys [file form before dry-run] :as opts}]
  (move/move-form opts))

(defn run [{:keys [op] :as opts}]
  (let [result (case op
                 :outline (run-outline opts)
                 :mv (run-mv opts)
                 {:error (str "Unknown op: " op
                              ". Valid ops: :outline, :mv")})]
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
