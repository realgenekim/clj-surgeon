#!/usr/bin/env bb

(ns verify-edit-portfolio
  (:require
   [babashka.fs :as fs]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [rewrite-clj.zip :as z])
  (:import
   (java.math BigInteger)
   (java.security MessageDigest)))

(def required-verification
  {:exact-bytes true
   :parse-clojure true})

(defn- positive-int?
  [value]
  (and (int? value) (pos? value)))

(defn- valid-expected-counts?
  [expected]
  (and (map? expected)
       (positive-int? (:changed-files expected))
       (= 1 (count (filter positive-int?
                           ((juxt :edits :moved-forms) expected))))))

(defn- valid-target?
  [target]
  (and (string? target)
       (not (str/blank? target))
       (not (str/starts-with? target "/"))
       (not (some #{".."} (str/split target #"/")))))

(defn- valid-provenance?
  [provenance]
  (and (map? provenance)
       (keyword? (:kind provenance))
       (not (str/blank? (:source provenance)))
       (not (str/blank? (:minimized provenance)))))

(defn sha256
  [source]
  (format "%064x"
          (BigInteger. 1 (.digest (MessageDigest/getInstance "SHA-256")
                                  (.getBytes source "UTF-8")))))

(defn parseable-clojure?
  [source]
  (try
    (z/of-string source)
    true
    (catch Exception _
      false)))

(defn validate-capsule
  "Validate one capsule from data only. before-sources and after-sources map
  declared relative target paths to exact source strings."
  [capsule task-text before-sources after-sources]
  (let [targets (:targets capsule)
        target-list (if (vector? targets) targets [])
        target-set (set target-list)
        before-set (set (keys before-sources))
        after-set (set (keys after-sources))
        hashes (:hashes capsule)
        expected (:expected capsule)
        changed-target-count
        (count (filter #(not= (get before-sources %)
                              (get after-sources %))
                       target-list))
        errors
        (cond-> []
          (not (keyword? (:id capsule)))
          (conj {:error-type :invalid-capsule-id})

          (not (keyword? (:decision-boundary capsule)))
          (conj {:error-type :missing-decision-boundary})

          (str/blank? task-text)
          (conj {:error-type :missing-task-prompt})

          (re-find #"(?m)^[ \t]+```(?:clojure|clj)[ \t]*$" task-text)
          (conj {:error-type :ambiguous-indented-clojure-fence})

          (not (valid-provenance? (:provenance capsule)))
          (conj {:error-type :invalid-provenance})

          (or (not (vector? targets))
              (empty? targets)
              (not-every? valid-target? targets)
              (not= (count targets) (count target-set)))
          (conj {:error-type :invalid-targets})

          (not (valid-expected-counts? expected))
          (conj {:error-type :invalid-expected-counts})

          (and (map? expected)
               (positive-int? (:changed-files expected))
               (not= changed-target-count (:changed-files expected)))
          (conj {:error-type :changed-file-count-mismatch
                 :expected (:changed-files expected)
                 :actual changed-target-count})

          (not= required-verification
                (select-keys (:verification capsule)
                             (keys required-verification)))
          (conj {:error-type :missing-verification-policy})

          (not= target-set before-set)
          (conj {:error-type :before-target-mismatch
                 :expected target-set
                 :actual before-set})

          (not= target-set after-set)
          (conj {:error-type :after-target-mismatch
                 :expected target-set
                 :actual after-set})

          (and (= target-set before-set)
               (= target-set after-set)
               (= before-sources after-sources))
          (conj {:error-type :unchanged-capsule})

          (not= target-set (set (keys hashes)))
          (conj {:error-type :hash-target-mismatch
                 :expected target-set
                 :actual (set (keys hashes))}))]
    (let [errors
          (reduce
            (fn [current target]
              (let [before (get before-sources target)
                    after (get after-sources target)
                    declared (get hashes target)]
                (cond-> current
                  (and (string? before) (not (parseable-clojure? before)))
                  (conj {:error-type :invalid-before-clojure :target target})

                  (and (string? after) (not (parseable-clojure? after)))
                  (conj {:error-type :invalid-after-clojure :target target})

                  (and (string? before)
                       (not= (sha256 before) (:before declared)))
                  (conj {:error-type :before-hash-mismatch :target target})

                  (and (string? after)
                       (not= (sha256 after) (:after declared)))
                  (conj {:error-type :after-hash-mismatch :target target}))))
            errors
            (filter string? target-list))]
      (if (seq errors)
        {:ok false :id (:id capsule) :errors errors}
        {:ok true
         :id (:id capsule)
         :decision-boundary (:decision-boundary capsule)
         :target-count (count target-list)
         :changed-target-count changed-target-count}))))

(defn- relative-source-map
  [snapshot-root]
  (->> (fs/glob snapshot-root "**")
       (filter fs/regular-file?)
       (map (fn [path]
              [(str (fs/relativize snapshot-root path))
               (slurp (str path))]))
       (into {})))

(defn read-capsule
  [task-dir]
  (let [capsule (edn/read-string
                  (slurp (str (fs/path task-dir "capsule.edn"))))]
    {:capsule capsule
     :task-text (slurp (str (fs/path task-dir "task.txt")))
     :before (relative-source-map (fs/path task-dir "before"))
     :after (relative-source-map (fs/path task-dir "after"))}))

(defn validate-portfolio
  [entries]
  (let [ids (mapv #(get-in % [:capsule :id]) entries)
        duplicates (->> ids frequencies (keep (fn [[id n]] (when (> n 1) id))) vec)
        results (mapv #(validate-capsule (:capsule %) (:task-text %)
                                         (:before %) (:after %))
                      entries)]
    (if (or (seq duplicates) (some (complement :ok) results))
      {:ok false
       :error-type :invalid-edit-portfolio
       :duplicate-ids duplicates
       :tasks results}
      {:ok true
       :task-count (count results)
       :target-count (reduce + (map :target-count results))
       :tasks results})))

(defn load-portfolio
  [root]
  (->> (fs/list-dir root)
       (filter fs/directory?)
       (sort-by str)
       (mapv read-capsule)))

(defn- hashes-for
  [sources]
  (into {} (map (fn [[target source]] [target (sha256 source)]) sources)))

(def supplied-decision-boundaries
  #{:complete-decision-supplied
    :complete-literal-decision-supplied
    :complete-owner-deletion-supplied
    :unique-prose-change-supplied})

(defn capsule-run-metadata
  [capsule]
  {:targets (:targets capsule)
   :decision-supplied
   (contains? supplied-decision-boundaries (:decision-boundary capsule))})

(defn- read-run-metadata
  [task-dir]
  (-> task-dir fs/path read-capsule :capsule capsule-run-metadata))

(defn self-test
  []
  (let [before {"src/x.clj" "(ns x)\n(defn f [] :before)\n"}
        after {"src/x.clj" "(ns x)\n(defn f [] :after)\n"}
        valid {:id :x
               :decision-boundary :complete
               :provenance {:kind :test
                            :source "self-test"
                            :minimized "minimal valid capsule"}
               :targets ["src/x.clj"]
               :verification required-verification
               :expected {:changed-files 1 :edits 1}
               :hashes {"src/x.clj"
                        {:before (sha256 (get before "src/x.clj"))
                         :after (sha256 (get after "src/x.clj"))}}}
        task-text "Make the exact change."
        cases
        [{:label :valid
          :capsule valid :before before :after after :ok true}
         {:label :missing-id
          :capsule (dissoc valid :id) :before before :after after
          :error :invalid-capsule-id}
         {:label :missing-boundary
          :capsule (dissoc valid :decision-boundary) :before before :after after
          :error :missing-decision-boundary}
         {:label :missing-task
          :capsule valid :task-text "" :before before :after after
          :error :missing-task-prompt}
         {:label :ambiguous-indented-clojure-fence
          :capsule valid
          :task-text "Change exactly:\n  ```clojure\n  (:before)\n  ```"
          :before before :after after
          :error :ambiguous-indented-clojure-fence}
         {:label :missing-provenance
          :capsule (dissoc valid :provenance) :before before :after after
          :error :invalid-provenance}
         {:label :duplicate-target
          :capsule (assoc valid :targets ["src/x.clj" "src/x.clj"])
          :before before :after after :error :invalid-targets}
         {:label :non-collection-targets
          :capsule (assoc valid :targets 42)
          :before before :after after :error :invalid-targets}
         {:label :parent-target
          :capsule (assoc valid :targets ["../x.clj"])
          :before before :after after :error :invalid-targets}
         {:label :missing-expected-counts
          :capsule (dissoc valid :expected) :before before :after after
          :error :invalid-expected-counts}
         {:label :wrong-changed-file-count
          :capsule (assoc-in valid [:expected :changed-files] 2)
          :before before :after after :error :changed-file-count-mismatch}
         {:label :missing-before
          :capsule valid :before {} :after after :error :before-target-mismatch}
         {:label :missing-after
          :capsule valid :before before :after {} :error :after-target-mismatch}
         {:label :unexpected-before
          :capsule valid
          :before (assoc before "src/y.clj" "(ns y)\n")
          :after after :error :before-target-mismatch}
         {:label :unexpected-after
          :capsule valid :before before :after (assoc after "src/y.clj" "(ns y)")
          :error :after-target-mismatch}
         {:label :unchanged
          :capsule (assoc-in valid [:hashes "src/x.clj" :after]
                             (sha256 (get before "src/x.clj")))
          :before before :after before :error :unchanged-capsule}
         {:label :invalid-before
          :capsule (assoc-in valid [:hashes "src/x.clj" :before]
                             (sha256 "(ns x"))
          :before {"src/x.clj" "(ns x"} :after after
          :error :invalid-before-clojure}
         {:label :invalid-after
          :capsule (assoc-in valid [:hashes "src/x.clj" :after]
                             (sha256 "(ns x"))
          :before before :after {"src/x.clj" "(ns x"}
          :error :invalid-after-clojure}
         {:label :wrong-before-hash
          :capsule (assoc-in valid [:hashes "src/x.clj" :before] "bad")
          :before before :after after :error :before-hash-mismatch}
         {:label :wrong-after-hash
          :capsule (assoc-in valid [:hashes "src/x.clj" :after] "bad")
          :before before :after after :error :after-hash-mismatch}
         {:label :hash-target-mismatch
          :capsule (assoc valid :hashes {})
          :before before :after after :error :hash-target-mismatch}
         {:label :missing-verification
          :capsule (dissoc valid :verification)
          :before before :after after :error :missing-verification-policy}
         {:label :weakened-verification
          :capsule (assoc valid :verification {:exact-bytes false
                                               :parse-clojure true})
          :before before :after after :error :missing-verification-policy}]]
    (doseq [{:keys [label capsule before after ok error]
             case-task-text :task-text}
            cases]
      (let [result (validate-capsule capsule (if (nil? case-task-text)
                                               task-text
                                               case-task-text)
                                     before after)]
        (assert (= (boolean ok) (:ok result)) label)
        (when error
          (assert (some #(= error (:error-type %)) (:errors result)) label))))
    (let [entry {:capsule valid :task-text task-text
                 :before before :after after}
          duplicate (validate-portfolio [entry entry])]
      (assert (= :invalid-edit-portfolio (:error-type duplicate)))
      (assert (= [:x] (:duplicate-ids duplicate))))
    (let [before-two (assoc before "src/y.clj" "(ns y)\n(def y :before)\n")
          after-two (assoc after "src/y.clj" "(ns y)\n(def y :after)\n")
          multi (-> valid
                    (assoc :id :multi
                           :targets ["src/x.clj" "src/y.clj"]
                           :expected {:changed-files 2 :edits 2}
                           :hashes (into {}
                                         (map (fn [target]
                                                [target {:before (sha256 (get before-two target))
                                                         :after (sha256 (get after-two target))}])
                                              ["src/x.clj" "src/y.clj"]))))]
      (assert (:ok (validate-capsule multi task-text before-two after-two))))
    (assert (= {"src/x.clj" (sha256 (get before "src/x.clj"))}
               (hashes-for before)))
    (assert (= {:targets ["src/x.clj"]
                :decision-supplied false}
               (capsule-run-metadata valid)))
    (assert (:decision-supplied
              (capsule-run-metadata
                (assoc valid :decision-boundary :complete-owner-deletion-supplied))))
    (println "edit portfolio verifier self-test passed")))

(let [[command argument] *command-line-args*]
  (cond
    (= "--self-test" command)
    (self-test)

    (= "--targets" command)
    (doseq [target (:targets (read-run-metadata argument))]
      (println target))

    (= "--decision-supplied" command)
    (println (:decision-supplied (read-run-metadata argument)))

    (and command (nil? argument))
    (let [result (validate-portfolio (load-portfolio command))]
      (prn result)
      (when-not (:ok result)
        (System/exit 1)))

    :else
    (do
      (binding [*out* *err*]
        (println "Usage: bb bench/verify_edit_portfolio.clj ROOT | --self-test | --targets TASK_DIR | --decision-supplied TASK_DIR"))
      (System/exit 2))))
