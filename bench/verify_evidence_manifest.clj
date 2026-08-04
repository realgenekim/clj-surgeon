#!/usr/bin/env bb

(ns verify-evidence-manifest
  (:require
   [babashka.fs :as fs]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(def default-manifest
  "bench/results/2026-08-04-xray-maximality-raw/manifest.edn")

(def sha256-pattern #"[0-9a-f]{64}")

(defn safe-relative-path?
  [path]
  (and (string? path)
       (not (str/blank? path))
       (not (str/starts-with? path "/"))
       (not (str/starts-with? path "~"))
       (not (str/includes? path "\\"))
       (not-any? #{"" "." ".."} (str/split path #"/"))))

(defn validate-manifest
  "Validate manifest data against filesystem facts without performing I/O."
  [manifest facts discovered-archives]
  (let [artifacts (:artifacts manifest)
        exclusions (:excluded-roots manifest)
        paths (mapv :path artifacts)
        path-frequencies (frequencies paths)
        exclusion-frequencies (frequencies exclusions)
        manifest-archives (set (filter #(str/ends-with? % ".tar.gz") paths))]
    (vec
      (concat
        (when-not (map? manifest)
          [{:error-type :invalid-manifest :message "Manifest must be an EDN map."}])
        (when-not (and (vector? artifacts) (seq artifacts))
          [{:error-type :invalid-artifacts :message ":artifacts must be a non-empty vector."}])
        (when-not (and (vector? exclusions) (every? #(and (string? %) (not (str/blank? %))) exclusions))
          [{:error-type :invalid-exclusions
            :message ":excluded-roots must be a vector of non-blank strings."}])
        (for [[index artifact] (map-indexed vector (or artifacts []))
              :when (not (map? artifact))]
          {:error-type :invalid-artifact :index index :artifact artifact})
        (for [[index {:keys [path]}] (map-indexed vector (filter map? (or artifacts [])))
              :when (not (safe-relative-path? path))]
          {:error-type :unsafe-path :index index :path path})
        (for [[path count] path-frequencies :when (> count 1)]
          {:error-type :duplicate-path :path path :count count})
        (for [[root count] exclusion-frequencies :when (> count 1)]
          {:error-type :duplicate-exclusion :root root :count count})
        (for [[index {:keys [path sha256]}] (map-indexed vector (filter map? (or artifacts [])))
              :when (not (and (string? sha256) (re-matches sha256-pattern sha256)))]
          {:error-type :invalid-sha256 :index index :path path :sha256 sha256})
        (for [{:keys [path]} (filter map? (or artifacts []))
              root (or exclusions [])
              :when (and (string? path) (string? root) (str/includes? path root))]
          {:error-type :excluded-root-included :path path :excluded-root root})
        (for [{:keys [path]} (filter map? (or artifacts []))
              :let [{:keys [exists?]} (get facts path)]
              :when (not exists?)]
          {:error-type :missing-artifact :path path})
        (for [{:keys [path]} (filter map? (or artifacts []))
              :let [{:keys [exists? regular-file?]} (get facts path)]
              :when (and exists? (not regular-file?))]
          {:error-type :not-regular-file :path path})
        (for [{:keys [path]} (filter map? (or artifacts []))
              :let [{:keys [exists? inside-root?]} (get facts path)]
              :when (and exists? (not inside-root?))]
          {:error-type :path-escapes-repository :path path})
        (for [{:keys [path sha256]} (filter map? (or artifacts []))
              :let [actual (:sha256 (get facts path))]
              :when (and actual (not= sha256 actual))]
          {:error-type :sha256-mismatch :path path :expected sha256 :actual actual})
        (for [path (sort (remove manifest-archives discovered-archives))]
          {:error-type :unmanifested-archive :path path})))))

(defn sha256-file
  [path]
  (let [digest (java.security.MessageDigest/getInstance "SHA-256")]
    (with-open [input (io/input-stream path)]
      (let [buffer (byte-array 65536)]
        (loop []
          (let [read-count (.read input buffer)]
            (when (pos? read-count)
              (.update digest buffer 0 read-count)
              (recur))))))
    (format "%064x" (java.math.BigInteger. 1 (.digest digest)))))

(defn filesystem-facts
  [repo-root artifacts]
  (into {}
        (for [{:keys [path]} artifacts
              :when (safe-relative-path? path)
              :let [file (fs/path repo-root path)
                    exists? (fs/exists? file)
                    regular-file? (and exists? (fs/regular-file? file))
                    real-path (when exists?
                                (.toRealPath file (make-array java.nio.file.LinkOption 0)))
                    inside-root? (and exists?
                                      (.startsWith real-path
                                                   (.toRealPath (fs/path repo-root)
                                                                (make-array java.nio.file.LinkOption 0))))]]
          [path {:exists? exists?
                 :regular-file? regular-file?
                 :inside-root? inside-root?
                 :sha256 (when (and regular-file? inside-root?)
                           (sha256-file (str file)))}])))

(defn discovered-archives
  [repo-root artifacts]
  (let [directories (->> artifacts
                         (keep :path)
                         (filter safe-relative-path?)
                         (map #(fs/parent (fs/path repo-root %)))
                         set)]
    (set
      (for [directory directories
            path (fs/glob directory "*.tar.gz")]
        (str (fs/relativize repo-root path))))))

(defn run-self-test!
  []
  (let [good {:artifacts [{:path "bench/results/a.tar.gz"
                           :sha256 (apply str (repeat 64 "a"))}]
              :excluded-roots ["corrupt-run"]}
        facts {"bench/results/a.tar.gz"
               {:exists? true :regular-file? true :inside-root? true
                :sha256 (apply str (repeat 64 "a"))}}]
    (assert (empty? (validate-manifest good facts #{"bench/results/a.tar.gz"})))
    (assert (= #{:unsafe-path :missing-artifact :unmanifested-archive}
               (->> (validate-manifest
                      (assoc-in good [:artifacts 0 :path] "../escape.tar.gz")
                      {}
                      #{"bench/results/unlisted.tar.gz"})
                    (map :error-type)
                    set)))
    (assert (= [:sha256-mismatch]
               (mapv :error-type
                     (validate-manifest good
                                        (assoc-in facts ["bench/results/a.tar.gz" :sha256]
                                                  (apply str (repeat 64 "b")))
                                        #{"bench/results/a.tar.gz"}))))
    (assert (= #{:duplicate-path :excluded-root-included}
               (->> (validate-manifest
                      {:artifacts [(first (:artifacts good)) (first (:artifacts good))]
                       :excluded-roots ["a.tar.gz"]}
                      facts
                      #{"bench/results/a.tar.gz"})
                    (map :error-type)
                    set)))
    (println "evidence manifest self-test passed")))

(defn -main
  [& args]
  (if (= ["--self-test"] args)
    (run-self-test!)
    (let [manifest-path (or (first args) default-manifest)
          repo-root (-> *file* fs/path fs/parent fs/parent fs/canonicalize)
          manifest (edn/read-string (slurp manifest-path))
          artifacts (if (vector? (:artifacts manifest)) (:artifacts manifest) [])
          errors (validate-manifest manifest
                                    (filesystem-facts repo-root artifacts)
                                    (discovered-archives repo-root artifacts))]
      (if (seq errors)
        (do
          (prn {:status :error :manifest manifest-path :errors errors})
          (System/exit 1))
        (prn {:status :ok
              :manifest manifest-path
              :artifact-count (count artifacts)
              :excluded-root-count (count (:excluded-roots manifest))})))))

(apply -main *command-line-args*)
