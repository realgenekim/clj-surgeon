(ns replay (:require [babashka.fs :as fs] [babashka.process :refer [shell]] [clojure.edn :as edn]
   [clojure.string :as str] [clojure.walk :as walk]))
(defn donor-env [env] (select-keys env ["PATH" "HOME" "JAVA_HOME" "LANG"])) (defn source-refusal [listed actual]
  (when-let [path (first (sort (remove (set listed) actual)))] (str "unlisted-source-file " path)))
(defn created-count [files before after] (count (filter (set after) (remove (set before) (distinct files)))))
(defn rebind-config [cfg scratch fixture] (walk/postwalk #(if (string? %) (str/replace % #"\{\{(scratch|fixture)\}\}"
                                    (fn [[_ k]] ({"scratch" scratch "fixture" fixture} k))) %) cfg))
(defn plan-fallback? [r] (and (= "refused" (:state r)) (string? (:error_type r)) (not (str/blank? (:error_type r)))
       (false? (:mutation_attempted r)) (false? (:committed r)))) (defn receipt-facts [r] (into (sorted-map)
        (for [path [[:state] [:verification_complete] [:counts :caller_sites] [:counts :caller_files]
                    [:counts :destinations] [:facts :footprint :created] [:facts :unexpected_paths]
                    [:unrequired_qualified_refs]] :let [k (last path) v (get-in r path (if (= k :unexpected_paths)
                                        (get-in r [:workspace_status k] ::missing) ::missing))]]
          [k (if (and (= :created k) (coll? v)) (count v) v)])))
(defn inventory [root] (for [p (fs/glob root "**" {:hidden true :follow-links false})
        :when (or (fs/sym-link? p) (fs/regular-file? p))] (str (fs/relativize root p))))
(defn inventory-facts [facts sources destinations paths]
  (let [before (map #(subs % 7) sources) profile? #(str/starts-with? % ".cpcache/")]
    (assoc facts :inventory-count (count paths) :profile-artefacts (count (filter profile? paths))
      :created (created-count destinations before paths)
      :unexpected_paths (vec (sort (remove (set (concat before destinations [".clj-surgeon.edn"]))
                             (remove profile? paths)))))))
(defn compare-facts [recorded now] (mapv (fn [[k v]] (let [n (get now k)] (str (name k) " "
  (when (= k :unexpected_paths) (str "inventory: " (:inventory-count now) ", extra: " (when (coll? n) (count n))
    ", profile-artefacts: " (:profile-artefacts now 0) " (.cpcache/) "))
                 (cond (and (= k :unexpected_paths) (coll? v) (coll? n)
    (every? #{".clj-surgeon.edn" "00SERVER-LOGS.txt" "test/cfp_scheduler_killer/view_architecture_test.clj"} n))
                       (str "expected-different recorded artefacts: " (count v) ", replay: " (count n))
                       (and (not= k :unexpected_paths) (not= ::missing v) (= v n))
                       (if (= k :created) (str "same (" v " destination files present on disk)") "same")
                       :else (str "DIFFERS " (pr-str v) " " (pr-str n)))))) (receipt-facts recorded)))
(defn comparison-exit ([lines] (comparison-exit lines 0))
  ([lines exit] (if (and (= 0 exit) (not-any? #(str/includes? % " DIFFERS ") lines)) 0 1)))
(defn verify-manifest! [fixture] (let [entries (mapv #(str/split % #"  " 2)
                      (str/split-lines (slurp (str (fs/path fixture "source-manifest.txt")))))
        sources (filter #(str/starts-with? % "source/") (map second entries))
        actual (map #(str "source/" %) (inventory (fs/path fixture "source")))]
    (when-let [error (source-refusal sources actual)] (throw (ex-info error {:exit 2})))
    (doseq [[expected path] entries] (let [digest (.digest (java.security.MessageDigest/getInstance "SHA-256")
                     (java.nio.file.Files/readAllBytes (fs/path fixture path)))
            actual (apply str (map #(format "%02x" (bit-and 255 %)) digest))]
        (when (not= expected actual) (throw (ex-info "Digest DIFFERS" {:path path}))))) sources))
(defn replay! [fixture donor scratch] (let [sources (verify-manifest! fixture)
        donor (fs/canonicalize (or donor (throw (ex-info "Supply astra checkout or CLJ_SURGEON_ASTRA" {}))))
        scratch (fs/canonicalize (or scratch (fs/path (fs/create-temp-dir) "workspace")))
        read-edn #(edn/read-string (slurp (str %)))]
    (when (or (fs/exists? scratch) (some #(fs/starts-with? scratch (fs/canonicalize %)) [fixture donor]))
      (throw (ex-info "Scratch must be fresh and outside fixture and checkout" {})))
    (doseq [path sources :let [dest (fs/path scratch (subs path (count "source/")))]]
      (fs/create-dirs (fs/parent dest)) (fs/copy (fs/path fixture path) dest {:copy-attributes true}))
    (spit (str (fs/path scratch ".clj-surgeon.edn"))
      (pr-str (rebind-config (read-edn (fs/path fixture "workspace.clj-surgeon.edn")) (str scratch) (str fixture))))
    (let [control (fs/create-temp-dir {:dir (fs/parent scratch) :prefix "replay-control-"})
          request-file (str (fs/path control "request.edn"))
          request (assoc (read-edn (fs/path fixture "D1-request.edn")) :workspace_root (str scratch))
          invoke! (fn [request] (spit request-file (pr-str request))
            (let [result (shell {:dir donor :env (donor-env (System/getenv)) :out :string :err :inherit :continue true}
                       "bb" "-cp" "src:test" "-m" "clj-surgeon.core" ":op" ":split-ns!" ":request-file" request-file)]
  (spit (str (fs/path control (if (:plan_only request) "receipt-diagnostic.edn" "receipt-commit.edn"))) (:out result))
                      (println "Invocation:" (if (:plan_only request) "plan-only" "commit") "exit" (:exit result))
                      {:receipt (edn/read-string (:out result)) :exit (:exit result)}))
          {receipt :receipt initial-exit :exit} (invoke! request) {:keys [receipt exit]} (if (plan-fallback? receipt)
                                   (invoke! (assoc request :plan_only true)) {:receipt receipt :exit initial-exit})
          lines (compare-facts (read-edn (fs/path fixture "sentinel-20261025-split-receipt.edn"))
      (inventory-facts (receipt-facts receipt) sources (map :file (:destinations request)) (inventory scratch)))]
      (println "Scratch:" (str scratch) "Control:" (str control) "Diagnostic:" (:error_type receipt) (:error receipt))
      (when-let [{:keys [exit error]} (:workspace_status receipt)]
        (when (not= 0 exit) (println "workspace-inspection: failed" exit error)))
      (when-let [details (:details_path receipt)] (println "Details:" details))
      (run! println lines) (comparison-exit lines (if (= 0 initial-exit) exit initial-exit)))))
(when (= *file* (System/getProperty "babashka.file")) (System/exit (try (replay! (fs/parent (fs/absolutize *file*))
       (or (first *command-line-args*) (System/getenv "CLJ_SURGEON_ASTRA")) (second *command-line-args*))
                 (catch clojure.lang.ExceptionInfo e (println (ex-message e)) (or (:exit (ex-data e)) 1)))))
