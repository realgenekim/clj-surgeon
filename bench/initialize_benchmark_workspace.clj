#!/usr/bin/env bb

(ns initialize-benchmark-workspace
  (:require
   [babashka.fs :as fs]
   [babashka.process :as proc]))

(def fixed-git-environment
  {"GIT_AUTHOR_DATE" "2000-01-01T00:00:00Z"
   "GIT_COMMITTER_DATE" "2000-01-01T00:00:00Z"})

(defn initialization-steps
  "Return the complete caller-neutral Git initialization program as data."
  []
  [{:id :init
    :argv ["git" "init" "-q"]}
   {:id :user-name
    :argv ["git" "config" "user.name" "clj-surgeon benchmark"]}
   {:id :user-email
    :argv ["git" "config" "user.email" "benchmark@invalid.example"]}
   {:id :stage
    :argv ["git" "add" "-A"]}
   {:id :commit
    :argv ["git" "commit" "-q" "--no-gpg-sign" "-m"
           "benchmark starting state"]
    :extra-env fixed-git-environment}])

(defn- run-step!
  [workspace {:keys [id argv extra-env]}]
  (let [{:keys [exit err]}
        @(proc/process argv
                       (cond-> {:dir workspace :out :string :err :string}
                         extra-env (assoc :extra-env extra-env)))]
    (when-not (zero? exit)
      (throw (ex-info "Benchmark workspace initialization failed"
                      {:error-type :workspace-initialization-failed
                       :step id
                       :exit exit
                       :stderr err})))))

(defn- git-output
  [workspace argv]
  (let [{:keys [exit out err]}
        @(proc/process argv {:dir workspace :out :string :err :string})]
    (when-not (zero? exit)
      (throw (ex-info "Benchmark workspace verification failed"
                      {:error-type :workspace-verification-failed
                       :argv argv
                       :exit exit
                       :stderr err})))
    out))

(defn initialize!
  [workspace]
  (let [workspace (str (fs/canonicalize workspace))]
    (when-not (fs/directory? workspace)
      (throw (ex-info "Benchmark workspace must be an existing directory"
                      {:error-type :invalid-workspace
                       :workspace workspace})))
    (when (fs/exists? (fs/path workspace ".git"))
      (throw (ex-info "Benchmark workspace already contains .git"
                      {:error-type :workspace-already-initialized
                       :workspace workspace})))
    (doseq [step (initialization-steps)]
      (run-step! workspace step))
    (let [commit-count (parse-long
                         (.trim (git-output workspace
                                            ["git" "rev-list" "--count" "HEAD"])))
          status (git-output workspace ["git" "status" "--porcelain"])]
      (when-not (and (= 1 commit-count) (empty? status))
        (throw (ex-info "Benchmark workspace did not initialize cleanly"
                        {:error-type :unclean-initial-workspace
                         :commit-count commit-count
                         :status status})))
      {:ok true
       :operation :initialize-benchmark-workspace
       :commit-count commit-count
       :clean true})))

(defn self-test
  []
  (let [steps (initialization-steps)]
    (assert (= [:init :user-name :user-email :stage :commit]
               (mapv :id steps)))
    (assert (= fixed-git-environment (:extra-env (last steps))))
    (assert (= ["git" "add" "-A"] (:argv (nth steps 3))))
    (assert (= "benchmark@invalid.example"
               (last (:argv (nth steps 2)))))
    (println "benchmark workspace initializer self-test passed")))

(let [[argument] *command-line-args*]
  (cond
    (= "--self-test" argument)
    (self-test)

    argument
    (prn (initialize! argument))

    :else
    (do
      (binding [*out* *err*]
        (println "Usage: bb bench/initialize_benchmark_workspace.clj WORKSPACE | --self-test"))
      (System/exit 2))))
