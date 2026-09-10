(ns clj-surgeon.insert-forms-support
  (:require
   [clj-surgeon.insert-forms :as insert]
   [clj-surgeon.insert-forms-oracle :as oracle]
   [clj-surgeon.receipt-artifacts :as artifacts]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.test :refer [is]])
  (:import
   (java.nio.file Files)
   (java.security MessageDigest)))

(defn sha [s]
  (apply str (map #(format "%02x" (bit-and 255 %))
                  (.digest (MessageDigest/getInstance "SHA-256")
                           (.getBytes ^String s "UTF-8")))))
(def source "(defn a [] 1)\n(defn ab [] 2)\n")
(defn request [s]
  {:version 1 :workspace_root "/fixture" :file "src/example.clj"
   :guard {:sha256 (sha s)}
   :anchor {:scope "top-level" :owner {:kind "defn" :name "a"}
            :expect 1 :position "after"}
   :payload {:text "(defn b [] 3)" :forms 1}})
(defn body [kind name boundary]
  {:scope "body" :owner {:kind kind :name name} :expect 1 :boundary boundary})
(defn accepted [s req expected]
  (let [result (insert/plan s req)]
    (is (= true (:ok result)) (pr-str result))
    (is (= expected (:candidate result)))
    (when (:ok result)
      (is (= true (:ok (oracle/verify s (:candidate result) req (:receipt result))))))
    result))
(defn refused [s req kind]
  (let [result (insert/plan s req)]
    (is (= kind (:error-type result)) (pr-str result))
    (is (= {:state "refused" :committed false :mutation_attempted false
            :source_unchanged true}
           (select-keys result [:state :committed :mutation_attempted :source_unchanged])))
    (is (= s (or (:candidate result) s)))
    result))
(defn with-file [s f]
  (let [dir (.toFile (Files/createTempDirectory
                       (.toPath (io/file (System/getProperty "java.io.tmpdir")))
                       "insert-" (make-array java.nio.file.attribute.FileAttribute 0)))
        workspace (io/file dir "workspace")
        file (io/file workspace "src/example.clj")]
    (try
      (.mkdirs (.getParentFile file))
      (spit file s)
      (binding [artifacts/*artifact-root* (str (io/file dir "artifacts"))]
        (with-redefs-fn {(ns-resolve 'clj-surgeon.file-ops 'publish-monitors) (atom {})}
          #(f workspace file (assoc (request s) :workspace_root (.getCanonicalPath workspace)))))
      (finally
        (doseq [p (reverse (file-seq dir))] (io/delete-file p true))))))

(defn fixtures []
  (edn/read-string (slurp (io/resource "clj_surgeon/insert_forms_fixtures.edn"))))
