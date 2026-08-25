#!/usr/bin/env bb

(ns verify-counterfactual-replay
  (:require
   [babashka.fs :as fs]
   [babashka.process :as proc]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [rewrite-clj.zip :as z])
  (:import
   (java.math BigInteger)
   (java.security MessageDigest)))

(def schema-version 1)
(def cases-root "bench/counterfactual-replay/cases")

(defn- sha256
  [source]
  (format "%064x"
          (BigInteger. 1 (.digest (MessageDigest/getInstance "SHA-256")
                                  (.getBytes source "UTF-8")))))

(defn- safe-target?
  [target]
  (and (string? target)
       (not (str/blank? target))
       (not (str/starts-with? target "/"))
       (not (some #{".."} (str/split target #"/")))))

(defn- full-sha?
  [value]
  (and (string? value) (boolean (re-matches #"[0-9a-f]{40}" value))))

(defn- full-sha256?
  [value]
  (and (string? value) (boolean (re-matches #"[0-9a-f]{64}" value))))

(defn- nonnegative-int?
  [value]
  (and (int? value) (not (neg? value))))

(defn- git
  [repo-root & arguments]
  (let [result (apply proc/shell
                      {:dir repo-root :out :string :err :string}
                      "git"
                      arguments)]
    (:out result)))

(defn- git-line
  [repo-root & arguments]
  (str/trim (apply git repo-root arguments)))

(defn- parseable-clojure?
  [source]
  (try
    (z/of-string source)
    true
    (catch Exception _
      false)))

(defn- clojure-target?
  [target]
  (boolean (re-find #"\.(?:clj|cljs|cljc)$" target)))

(defn- shape-errors
  [case-name capsule task]
  (let [repository (:repository capsule)
        targets (:targets capsule)
        target-list (if (vector? targets) targets [])
        ignored-paths (:ignored-paths capsule)
        hashes (:hashes capsule)
        verification (:verification capsule)
        expected (:expected capsule)
        hidden-values (keep repository [:parent :child :parent-tree :child-tree])]
    (cond-> []
      (not= schema-version (:schema capsule))
      (conj {:error-type :invalid-schema :actual (:schema capsule)})

      (not= case-name (some-> (:id capsule) name))
      (conj {:error-type :case-id-directory-mismatch
             :directory case-name :id (:id capsule)})

      (not (keyword? (:stratum capsule)))
      (conj {:error-type :invalid-stratum})

      (not= "clj-surgeon" (:name repository))
      (conj {:error-type :invalid-repository-name})

      (not-every? full-sha?
                  (keep repository [:parent :child :parent-tree :child-tree]))
      (conj {:error-type :invalid-git-identities})

      (or (not (vector? targets))
          (empty? target-list)
          (not-every? safe-target? target-list)
          (not= (count target-list) (count (set target-list))))
      (conj {:error-type :invalid-targets})

      (not= (set target-list) (set (keys hashes)))
      (conj {:error-type :hash-target-mismatch})

      (or (not (vector? ignored-paths))
          (not-every? safe-target? ignored-paths)
          (not-every? #(str/ends-with? % "/") ignored-paths))
      (conj {:error-type :invalid-ignored-paths})

      (not-every? full-sha256?
                  (mapcat (juxt :before :after) (vals hashes)))
      (conj {:error-type :invalid-target-hashes})

      (or (not (vector? (get-in capsule [:verification :commands])))
          (empty? (get-in capsule [:verification :commands]))
          (not-every? #(and (string? %) (not (str/blank? %)))
                      (get-in capsule [:verification :commands])))
      (conj {:error-type :invalid-verification-commands})

      (not= {:exact-changed-paths true
             :exact-target-bytes-secondary true
             :parse-clojure true}
            (select-keys verification
                         [:exact-changed-paths
                          :exact-target-bytes-secondary
                          :parse-clojure]))
      (conj {:error-type :invalid-verification-policy})

      (or (not (map? expected))
          (not-every? nonnegative-int?
                      ((juxt :changed-files :insertions :deletions) expected))
          (not= (count target-list) (:changed-files expected)))
      (conj {:error-type :invalid-expected-counts})

      (str/blank? task)
      (conj {:error-type :missing-task-prompt})

      (some #(str/includes? task %) hidden-values)
      (conj {:error-type :task-leaks-hidden-git-identity})

      (not= "task.md" (:task-file capsule))
      (conj {:error-type :invalid-task-file})

      (or (not= :historical-commit (get-in capsule [:provenance :kind]))
          (str/blank? (get-in capsule [:provenance :commit-subject]))
          (empty? (get-in capsule [:provenance :intent-sources])))
      (conj {:error-type :invalid-provenance}))))

(defn- repository-errors
  [repo-root capsule task]
  (let [{:keys [parent child parent-tree child-tree]} (:repository capsule)
        targets (:targets capsule)
        actual-parent (git-line repo-root "rev-parse" (str child "^"))
        actual-parent-tree (git-line repo-root "rev-parse" (str parent "^{tree}"))
        actual-child-tree (git-line repo-root "rev-parse" (str child "^{tree}"))
        actual-subject (git-line repo-root "show" "-s" "--format=%s" child)
        changed-paths (->> (str/split-lines
                             (git repo-root "diff" "--name-only" parent child "--"))
                           (remove str/blank?)
                           vec)
        numstat-lines (remove str/blank?
                              (str/split-lines
                                (git repo-root "diff" "--numstat" parent child "--")))
        numstat (map #(str/split % #"\t") numstat-lines)
        binary? (some #(or (= "-" (first %)) (= "-" (second %))) numstat)
        insertions (when-not binary? (reduce + (map #(parse-long (first %)) numstat)))
        deletions (when-not binary? (reduce + (map #(parse-long (second %)) numstat)))
        expected (:expected capsule)
        base-errors
        (cond-> []
          (not= parent actual-parent)
          (conj {:error-type :parent-mismatch
                 :expected parent :actual actual-parent})

          (not= parent-tree actual-parent-tree)
          (conj {:error-type :parent-tree-mismatch})

          (not= child-tree actual-child-tree)
          (conj {:error-type :child-tree-mismatch})

          (not= actual-subject (get-in capsule [:provenance :commit-subject]))
          (conj {:error-type :commit-subject-mismatch})

          (not= targets changed-paths)
          (conj {:error-type :changed-paths-mismatch
                 :expected targets :actual changed-paths})

          binary?
          (conj {:error-type :binary-target-unsupported})

          (and (not binary?) (not= insertions (:insertions expected)))
          (conj {:error-type :insertion-count-mismatch
                 :expected (:insertions expected) :actual insertions})

          (and (not binary?) (not= deletions (:deletions expected)))
          (conj {:error-type :deletion-count-mismatch
                 :expected (:deletions expected) :actual deletions}))]
    (reduce
      (fn [errors target]
        (let [before (git repo-root "show" (str parent ":" target))
              after (git repo-root "show" (str child ":" target))
              declared (get-in capsule [:hashes target])]
          (cond-> errors
            (not= (sha256 before) (:before declared))
            (conj {:error-type :before-hash-mismatch :target target})

            (not= (sha256 after) (:after declared))
            (conj {:error-type :after-hash-mismatch :target target})

            (and (clojure-target? target) (not (parseable-clojure? before)))
            (conj {:error-type :invalid-parent-clojure :target target})

            (and (clojure-target? target) (not (parseable-clojure? after)))
            (conj {:error-type :invalid-child-clojure :target target}))))
      base-errors
      targets)))

(defn- read-case
  [case-dir]
  (let [capsule-path (fs/path case-dir "capsule.edn")
        capsule (edn/read-string (slurp (str capsule-path)))
        task-path (fs/path case-dir (:task-file capsule))]
    {:case-name (str (fs/file-name case-dir))
     :capsule capsule
     :task (if (fs/regular-file? task-path) (slurp (str task-path)) "")}))

(defn- validate-case
  [repo-root {:keys [case-name capsule task]}]
  (let [errors (into (shape-errors case-name capsule task)
                     (repository-errors repo-root capsule task))]
    (if (seq errors)
      {:ok false :id (:id capsule) :stratum (:stratum capsule) :errors errors}
      {:ok true
       :id (:id capsule)
       :stratum (:stratum capsule)
       :parent (get-in capsule [:repository :parent])
       :child (get-in capsule [:repository :child])
       :target-count (count (:targets capsule))
       :verification-commands (get-in capsule [:verification :commands])})))

(defn- verify-all
  [repo-root]
  (let [entries (->> (fs/list-dir (fs/path repo-root cases-root))
                     (filter fs/directory?)
                     (sort-by str)
                     (mapv read-case))
        results (mapv #(validate-case repo-root %) entries)
        ids (mapv :id results)
        duplicate-ids (->> ids frequencies (keep (fn [[id n]] (when (> n 1) id))) vec)]
    (if (or (seq duplicate-ids) (some (complement :ok) results))
      {:schema "clj-surgeon.counterfactual-replay-verification/v1"
       :ok false
       :error-type :invalid-counterfactual-replay-portfolio
       :duplicate-ids duplicate-ids
       :cases results}
      {:schema "clj-surgeon.counterfactual-replay-verification/v1"
       :ok true
       :case-count (count results)
       :target-count (reduce + (map :target-count results))
       :cases results})))

(defn- self-test
  []
  (assert (safe-target? "src/x.clj"))
  (assert (not (safe-target? "../x.clj")))
  (assert (not (safe-target? "/tmp/x.clj")))
  (assert (full-sha? (apply str (repeat 40 "a"))))
  (assert (not (full-sha? "abc")))
  (assert (full-sha256? (apply str (repeat 64 "b"))))
  (assert (= (sha256 "abc")
             "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"))
  (assert (parseable-clojure? "(ns x)\n(def x 1)\n"))
  (assert (not (parseable-clojure? "(ns x")))
  (println "self-test: PASS (9 checks)"))

(defn -main
  [& arguments]
  (let [repo-root (str (fs/absolutize "."))]
    (if (= ["--self-test"] arguments)
      (self-test)
      (let [result (verify-all repo-root)]
        (prn result)
        (when-not (:ok result)
          (System/exit 1))))))

(apply -main *command-line-args*)
