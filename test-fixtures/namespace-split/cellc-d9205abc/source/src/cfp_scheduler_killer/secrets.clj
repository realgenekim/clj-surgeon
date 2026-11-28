(ns cfp-scheduler-killer.secrets
  "Secrets loader — local files in dev, gcp-secrets REST API on Cloud Run.
   Mirrors vizier.secrets / publisher.secrets — THE house pattern.

   HOUSE RULE (Gene, 2026-07-08): secrets are NEVER delivered via env vars —
   they are LOADED AT RUNTIME: gitignored secrets/ files (600) in dev;
   Secret Manager via gcp-secrets on Cloud Run (metadata-server auth — zero
   env vars, zero mounts, the SA just needs roles/secretmanager.secretAccessor).
   DB credentials go one better: IAM auth = no secret exists at all (db.clj)."
  (:require
   [clojure.edn :as edn]
   [clojure.string :as str]
   ;; static require (NOT requiring-resolve) so thin-jar AOT bundles it —
   ;; vizier lesson: otherwise missing from the Cloud Run classpath
   [gcp-secrets.main :refer [get-secret!]]
   [taoensso.timbre :as log]))

(def ^:private gcp-project (or (System/getenv "GCP_PROJECT") "does2020"))

(defn- use-local-keys? []
  (let [v (System/getenv "USE_LOCAL_KEYFILES")]
    (or (and (nil? (System/getenv "K_SERVICE"))       ;; Cloud Run Service
             (nil? (System/getenv "CLOUD_RUN_JOB")))  ;; Cloud Run Job
        (= v "true")
        (= v "1"))))

(defn- with-retry
  "gcp-secrets occasionally hits a transient broken pipe — retry with
   linear backoff (vizier lesson)."
  [attempts f]
  (loop [n 1]
    (let [r (try {:ok (f)} (catch Throwable e {:err e}))]
      (cond
        (contains? r :ok) (:ok r)
        (< n attempts) (do (log/warn :secrets/retry :attempt n
                                     :msg (.getMessage ^Throwable (:err r)))
                           (Thread/sleep (* 300 n))
                           (recur (inc n)))
        :else (throw (:err r))))))

(defn load-secret
  "Fetch one secret: local file in dev, Secret Manager on Cloud Run.
   Throws if unavailable. :parse :raw (default, trimmed string) or :edn."
  [secret-name local-path & {:keys [parse] :or {parse :raw}}]
  (let [content (if (use-local-keys?)
                  (do (log/debug :secrets/load-local :name secret-name :path local-path)
                      (slurp local-path))
                  (do (log/info :secrets/load-gcp :name secret-name :project gcp-project)
                      (with-retry 4 #(get-secret! secret-name gcp-project))))]
    (case parse
      :edn (if (string? content) (edn/read-string content) content)
      :raw (if (string? content) (str/trim content) (str content)))))

(defn load-secret-or-nil
  "Like load-secret but nil + a warning when unavailable — so namespaces
   load (and tests run) on a machine without this secret."
  [secret-name local-path & opts]
  (try
    (apply load-secret secret-name local-path opts)
    (catch Throwable e
      (log/warn :secrets/unavailable :name secret-name :msg (.getMessage e))
      nil)))
