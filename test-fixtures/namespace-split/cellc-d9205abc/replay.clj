(ns replay
  (:require
   [babashka.fs :as fs]
   [babashka.process :refer [shell]]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [clojure.walk :as walk]))
(defn rebind-request [request root] (assoc request :workspace_root root))
(defn rebind-config [config scratch fixture]
  (walk/postwalk #(if (string? %) (str/replace % #"\{\{(scratch|fixture)\}\}"
                                    (fn [[_ k]] ({"scratch" scratch "fixture" fixture} k))) %) config))
(defn plan-fallback? [r]
  (and (= "refused" (:state r)) (string? (:error_type r)) (not (str/blank? (:error_type r)))
       (false? (:mutation_attempted r)) (false? (:committed r))))
(defn receipt-facts [receipt]
  (into (sorted-map)
        (for [path [[:state] [:verification_complete] [:counts :caller_sites] [:counts :caller_files]
                    [:counts :destinations] [:facts :footprint :created] [:facts :unexpected_paths]
                    [:unrequired_qualified_refs]]]
          [(last path) (let [v (get-in receipt path
                                 (case (last path)
                                   :created (get-in receipt [:facts :destinations] ::missing)
                                   :unexpected_paths (get-in receipt [:workspace_status :unexpected_paths] ::missing)
                                   ::missing))]
                         (if (and (= :created (last path)) (coll? v)) (count v) v))])))
(defn compare-facts [recorded now]
  (mapv (fn [[k v]]
          (str (name k) " "
               (cond
                 (and (= k :unexpected_paths) (coll? v) (or (nil? (get now k)) (coll? (get now k))))
                 (str "expected-different recorded artefacts: " (count v) ", replay: " (count (get now k)))
                 (and (not= ::missing v) (= v (get now k))) "same"
                 :else (str "DIFFERS " (pr-str v) " " (pr-str (get now k))))))
        (receipt-facts recorded)))
(defn comparison-exit [lines] (if (some #(str/includes? % " DIFFERS ") lines) 1 0))
(defn replay! [fixture donor scratch]
  (shell {:dir fixture} "sha256sum" "-c" "source-manifest.txt")
  (let [donor (fs/canonicalize (or donor (throw (ex-info "Supply astra checkout or CLJ_SURGEON_ASTRA" {}))))
        scratch (fs/canonicalize (or scratch (fs/path (fs/create-temp-dir) "workspace")))
        request-file (str (fs/path scratch "request.edn"))
        read-edn #(edn/read-string (slurp (str %)))]
    (when (or (fs/exists? scratch) (some #(fs/starts-with? scratch (fs/canonicalize %)) [fixture donor]))
      (throw (ex-info "Scratch must be fresh and outside fixture and checkout" {})))
    (fs/copy-tree (fs/path fixture "source") scratch)
    (spit (str (fs/path scratch ".clj-surgeon.edn"))
          (pr-str (rebind-config (read-edn (fs/path fixture "workspace.clj-surgeon.edn"))
                                 (str scratch) (str fixture))))
    (let [request (rebind-request (read-edn (fs/path fixture "D1-request.edn")) (str scratch))
          invoke! (fn [request]
                    (spit request-file (pr-str request))
                    (let [result (shell {:dir donor :out :string :continue true}
                                   "bb" "-cp" "src:test" "-m" "clj-surgeon.core"
                                   ":op" ":split-ns!" ":request-file" request-file)]
                      (spit (str (fs/path scratch "replay-receipt.edn")) (:out result))
                      (println "Invocation:" (if (:plan_only request) "plan-only" "commit") "exit" (:exit result))
                      (edn/read-string (:out result))))
          receipt (invoke! request)
          receipt (if (plan-fallback? receipt)
                    (do (println "Commit refused:" (:error_type receipt) (:error receipt) "; plan-only fallback")
                        (invoke! (assoc request :plan_only true)))
                    receipt)
          lines (compare-facts (read-edn (fs/path fixture "sentinel-20261025-split-receipt.edn"))
                               (receipt-facts receipt))]
      (println "Scratch:" (str scratch) "Diagnostic:" (:error_type receipt) (:error receipt))
      (when-let [details (:details_path receipt)]
        (prn (:verification (read-edn details))))
      (run! println lines)
      (comparison-exit lines))))
(when (= *file* (System/getProperty "babashka.file"))
  (System/exit
    (replay! (fs/parent (fs/absolutize *file*))
             (or (first *command-line-args*) (System/getenv "CLJ_SURGEON_ASTRA"))
             (second *command-line-args*))))
