(ns operation-algebra-commit-parity
  (:require
   [clj-surgeon.intent-transaction :as transaction]
   [clojure.edn :as edn]
   [clojure.java.io :as io])
  (:import
   (java.nio.charset StandardCharsets)
   (java.nio.file Files)
   (java.security MessageDigest)))

(def candidate-commit
  "b05b3a03afb7e40020192444777a5a1c20b91a69")

(def pre-cutover-commit
  "91b2190")

(def original-source
  "(ns app)\n(defn title [] (old-title))\n")

(def future-source
  "(ns app)\n(defn title [] (new-title))\n")

(def scenarios
  [:success :compile-refusal :stale-source :write-failure
   :receipt-publication-failure])

(defn sha256
  [value]
  (let [digest (MessageDigest/getInstance "SHA-256")]
    (.update digest (.getBytes (str value) StandardCharsets/UTF_8))
    (format "%064x" (BigInteger. 1 (.digest digest)))))

(defn canonical-data
  [value]
  (cond
    (map? value)
    [:map
     (->> value
          (map (fn [[key item]]
                 [(canonical-data key) (canonical-data item)]))
          (sort-by pr-str)
          vec)]

    (vector? value) [:vector (mapv canonical-data value)]
    (set? value) [:set (->> value (map canonical-data) (sort-by pr-str) vec)]
    (sequential? value) [:sequence (mapv canonical-data value)]
    :else [:scalar value]))

(defn data-hash
  [value]
  (sha256 (pr-str (canonical-data value))))

(defn temp-directory
  [prefix]
  (.toFile
    (Files/createTempDirectory
      prefix
      (make-array java.nio.file.attribute.FileAttribute 0))))

(defn delete-tree!
  [root]
  (when (and root (.exists (io/file root)))
    (doseq [file (reverse (file-seq (io/file root)))]
      (io/delete-file file true))))

(defn file-change-spec
  [source-path scenario]
  {:intents [{:files [source-path]
              :from (if (= :compile-refusal scenario)
                      "(missing-title)"
                      "(old-title)")
              :to "(new-title)"
              :expect-count 1}]
   :expect {:intent-count 1
            :edit-count 1
            :changed-file-count 1}})

(defn private-var
  [name]
  (or (ns-resolve 'clj-surgeon.intent-transaction name)
      (throw (ex-info (str "Missing transaction Var " name) {:var name}))))

(defn transaction-version
  []
  @(private-var 'transaction-version))

(defn successful-commit-result
  [compiled]
  {:ok true
   :operation :change!
   :transaction-version (transaction-version)
   :committed true
   :changed-file-count (:changed-file-count compiled)
   :verified {:whole-files true
              :file-count (:changed-file-count compiled)
              :read-back-hashes
              (into {}
                    (map (fn [[file source]] [file (sha256 source)]))
                    (:future-sources compiled))}})

(defn stale-source-result
  [compiled]
  (let [file (first (keys (:original-sources compiled)))]
    {:error "Source hash changed before transaction commit"
     :error-type :source-hash-mismatch
     :file file
     :expected-hash (sha256 (get-in compiled [:original-sources file]))
     :actual-hash (sha256 "externally changed")}))

(defn write-failure-result
  [compiled]
  {:error "Transaction write failed; all files restored"
   :error-type :transaction-write-failed
   :cause-error "forced write failure"
   :cause-error-type :transaction-write-exception
   :rolled-back true
   :recovery {:ok true
              :restored (vec (sort (keys (:original-sources compiled))))}})

(defn canonical-outcome-var
  []
  (try
    (require 'clj-surgeon.operation-algebra)
    (ns-resolve 'clj-surgeon.operation-algebra 'observe-change-terminal)
    (catch Exception _ nil)))

(defn fake-commit
  [scenario calls compiled]
  (swap! calls update :fake-commit inc)
  (case scenario
    :stale-source (stale-source-result compiled)
    :write-failure (write-failure-result compiled)
    (successful-commit-result compiled)))

(defn run-fake-scenario
  [entrance scenario workspace]
  (let [source-file (io/file workspace "app.clj")
        receipt-file (io/file workspace "receipt.edn")
        source-path (.getCanonicalPath source-file)
        receipt-path (.getCanonicalPath receipt-file)
        stage-var (private-var 'stage-receipt!)
        publish-var (private-var 'publish-staged-receipt!)
        receipt-source-var (private-var 'receipt-source)
        observe-var (canonical-outcome-var)
        execute-var (or (ns-resolve 'clj-surgeon.intent-transaction
                                    (if (= :mcp entrance)
                                      'execute-mcp-change!
                                      'execute-change!))
                        (throw (ex-info "Requested entrance is unavailable"
                                        {:entrance entrance})))
        original-slurp clojure.core/slurp
        original-observe (when observe-var @observe-var)
        staged-receipt (atom nil)
        canonical-outcome (atom nil)
        calls (atom {:stage 0 :fake-commit 0 :publish 0
                     :authoritative-commit 0 :live-receipt 0})]
    (.mkdirs (io/file workspace))
    (spit source-file original-source)
    (when (.exists receipt-file) (.delete receipt-file))
    (let [base-bindings
          {#'transaction/commit-compiled!
           (fn [compiled]
             (fake-commit scenario calls compiled))

           stage-var
           (fn [_receipt-path receipt]
             (swap! calls update :stage inc)
             (reset! staged-receipt receipt)
             (io/file workspace "fake-staged.edn"))

           publish-var
           (fn [_staged _receipt-path]
             (swap! calls update :publish inc)
             (when (= :receipt-publication-failure scenario)
               (throw (ex-info "forced receipt publication failure"
                               {:error-type :forced-publication-failure}))))

           #'clojure.core/slurp
           (fn [path & opts]
             (if (and (= (str path) receipt-path) @staged-receipt)
               (@receipt-source-var @staged-receipt)
               (apply original-slurp path opts)))}
          bindings
          (if observe-var
            (assoc base-bindings
                   observe-var
                   (fn [observation legacy-result]
                     (let [result (original-observe observation legacy-result)]
                       (reset! canonical-outcome
                               (:outcome
                                 ((ns-resolve 'clj-surgeon.operation-algebra
                                              'classify-change-terminal)
                                  (assoc observation
                                         :legacy-result legacy-result))))
                       result)))
            base-bindings)
          result (with-redefs-fn
                   bindings
                   #(@execute-var
                      {:spec (file-change-spec source-path scenario)
                       :receipt-out receipt-path}))
          receipt-text (when-let [receipt @staged-receipt]
                         (@receipt-source-var receipt))]
      {:entrance entrance
       :scenario scenario
       :legacy-result result
       :legacy-result-hash (data-hash result)
       :canonical-outcome @canonical-outcome
       :canonical-outcome-hash
       (when @canonical-outcome (data-hash @canonical-outcome))
       :receipt-source receipt-text
       :receipt-source-sha256 (when receipt-text (sha256 receipt-text))
       :receipt-data @staged-receipt
       :receipt-data-hash
       (when @staged-receipt (data-hash @staged-receipt))
       :source-after (slurp source-file)
       :receipt-exists (.exists receipt-file)
       :calls @calls})))

(defn run-fake-snapshot
  [entrance workspace]
  {:entrance entrance
   :scenarios
   (into {}
         (map (fn [scenario]
                [scenario (run-fake-scenario entrance scenario workspace)]))
         scenarios)})

(defn run-live-success
  [workspace]
  (let [source-file (io/file workspace "app.clj")
        receipt-file (io/file workspace "receipt.edn")
        source-path (.getCanonicalPath source-file)
        receipt-path (.getCanonicalPath receipt-file)
        stage-var (private-var 'stage-receipt!)
        publish-var (private-var 'publish-staged-receipt!)
        original-commit transaction/commit-compiled!
        original-stage @stage-var
        original-publish @publish-var
        calls (atom {:stage 0 :authoritative-commit 0 :publish 0
                     :live-receipt 0})]
    (.mkdirs (io/file workspace))
    (spit source-file original-source)
    (when (.exists receipt-file) (.delete receipt-file))
    (let [result
          (with-redefs-fn
            {#'transaction/commit-compiled!
             (fn [& args]
               (when (= 1 (count args))
                 (swap! calls update :authoritative-commit inc))
               (apply original-commit args))
             stage-var
             (fn [& args]
               (swap! calls update :stage inc)
               (apply original-stage args))
             publish-var
             (fn [& args]
               (swap! calls update :publish inc)
               (let [value (apply original-publish args)]
                 (swap! calls update :live-receipt inc)
                 value))}
            #(transaction/execute-change!
               {:spec (file-change-spec source-path :success)
                :receipt-out receipt-path}))
          receipt-text (when (.exists receipt-file) (slurp receipt-file))]
      {:legacy-result result
       :legacy-result-hash (data-hash result)
       :source-after (slurp source-file)
       :receipt-source receipt-text
       :receipt-source-sha256 (when receipt-text (sha256 receipt-text))
       :receipt-data (when receipt-text (edn/read-string receipt-text))
       :receipt-data-hash
       (when receipt-text (data-hash (edn/read-string receipt-text)))
       :calls @calls})))

(defn accept-receipt
  [receipt-file source-path]
  (let [receipt (edn/read-string (slurp receipt-file))
        validate! @(private-var 'validate-receipt!)
        compile-inverse @(private-var 'compile-inverse)]
    (validate! receipt)
    (let [inverse (compile-inverse receipt {source-path future-source})]
      {:accepted (true? (:ok inverse))
       :receipt-hash (:receipt-hash receipt)
       :inverse-facts
       (select-keys inverse
                    [:ok :operation :intent-count :match-count
                     :changed-file-count :original-sources
                     :future-sources])
       :inverse-facts-hash
       (data-hash
         (select-keys inverse
                      [:ok :operation :intent-count :match-count
                       :changed-file-count :original-sources
                       :future-sources]))})))

(defn run-process!
  [directory argv]
  (let [builder (doto (ProcessBuilder. ^java.util.List (mapv str argv))
                  (.directory (io/file directory)))
        process (.start builder)
        stdout (future (slurp (.getInputStream process)))
        stderr (future (slurp (.getErrorStream process)))
        exit (.waitFor process)
        out @stdout
        err @stderr]
    (when-not (zero? exit)
      (throw (ex-info "Compatibility worker failed"
                      {:directory (str directory)
                       :argv (mapv str argv)
                       :exit exit
                       :stdout out
                       :stderr err})))
    (edn/read-string out)))

(defn run-command!
  [directory argv]
  (let [builder (doto (ProcessBuilder. ^java.util.List (mapv str argv))
                  (.directory (io/file directory))
                  (.inheritIO))
        process (.start builder)
        exit (.waitFor process)]
    (when-not (zero? exit)
      (throw (ex-info "Compatibility setup command failed"
                      {:directory (str directory)
                       :argv (mapv str argv)
                       :exit exit})))))

(defn materialize-commit!
  [repository commit destination]
  (.mkdirs (io/file destination))
  (let [archive (io/file (.getParentFile (io/file destination))
                         (str (sha256 commit) ".tar"))]
    (run-command! repository
                  ["git" "archive" "--format=tar" "-o"
                   (.getPath archive) commit])
    (run-command! destination
                  ["tar" "-xf" (.getPath archive) "-C"
                   (.getPath (io/file destination))])
    (.delete archive)))

(defn exact-projection-parity
  [pre candidate-cli candidate-mcp]
  (into {}
        (map
          (fn [scenario]
            (let [pre-case (get-in pre [:scenarios scenario])
                  cli-case (get-in candidate-cli [:scenarios scenario])
                  mcp-case (get-in candidate-mcp [:scenarios scenario])]
              [scenario
               {:legacy-cli-equal
                (= (:legacy-result pre-case) (:legacy-result cli-case))
                :legacy-mcp-equal
                (= (:legacy-result pre-case) (:legacy-result mcp-case))
                :candidate-domain-equal
                (= (:canonical-outcome cli-case)
                   (:canonical-outcome mcp-case))
                :receipt-source-equal
                (= (:receipt-source pre-case)
                   (:receipt-source cli-case)
                   (:receipt-source mcp-case))
                :source-unchanged
                (= original-source
                   (:source-after pre-case)
                   (:source-after cli-case)
                   (:source-after mcp-case))
                :no-live-effects
                (every?
                  (fn [case]
                    (and (zero? (get-in case [:calls :authoritative-commit]))
                         (zero? (get-in case [:calls :live-receipt]))
                         (false? (:receipt-exists case))))
                  [pre-case cli-case mcp-case])}])))
        scenarios))

(defn all-case-gates-pass?
  [parity]
  (every? true? (mapcat vals (vals parity))))

(defn scenario-evidence
  [pre candidate-cli candidate-mcp]
  (into {}
        (map
          (fn [scenario]
            (let [pre-case (get-in pre [:scenarios scenario])
                  cli-case (get-in candidate-cli [:scenarios scenario])
                  mcp-case (get-in candidate-mcp [:scenarios scenario])]
              [scenario
               {:pre-legacy-result-hash (:legacy-result-hash pre-case)
                :candidate-cli-legacy-result-hash
                (:legacy-result-hash cli-case)
                :candidate-mcp-legacy-result-hash
                (:legacy-result-hash mcp-case)
                :candidate-cli-domain-hash
                (:canonical-outcome-hash cli-case)
                :candidate-mcp-domain-hash
                (:canonical-outcome-hash mcp-case)
                :receipt-source-sha256 (:receipt-source-sha256 pre-case)
                :pre-calls (:calls pre-case)
                :candidate-cli-calls (:calls cli-case)
                :candidate-mcp-calls (:calls mcp-case)}])))
        scenarios))

(defn report
  [repository script]
  (let [root (temp-directory "clj-surgeon-commit-parity-")
        pre-directory (io/file root "pre")
        candidate-directory (io/file root "candidate")
        workspace (io/file root "workspace")]
    (try
      (materialize-commit! repository pre-cutover-commit pre-directory)
      (materialize-commit! repository candidate-commit candidate-directory)
      (let [worker-base ["bb" "-cp" "src:test" script "--worker"]
            pre (run-process! pre-directory
                              (conj worker-base "snapshot" "cli"
                                    (.getPath workspace)))
            candidate-cli
            (run-process! candidate-directory
                          (conj worker-base "snapshot" "cli"
                                (.getPath workspace)))
            candidate-mcp
            (run-process! candidate-directory
                          (conj worker-base "snapshot" "mcp"
                                (.getPath workspace)))
            parity (exact-projection-parity pre candidate-cli candidate-mcp)
            pre-receipt (get-in pre [:scenarios :success :receipt-source])
            candidate-receipt
            (get-in candidate-cli [:scenarios :success :receipt-source])
            pre-receipt-file (io/file root "pre-receipt.edn")
            candidate-receipt-file (io/file root "candidate-receipt.edn")
            source-path (.getCanonicalPath (io/file workspace "app.clj"))
            _ (spit pre-receipt-file pre-receipt)
            _ (spit candidate-receipt-file candidate-receipt)
            pre-accepts-candidate
            (run-process!
              pre-directory
              (conj worker-base "accept"
                    (.getPath candidate-receipt-file) source-path))
            candidate-accepts-pre
            (run-process!
              candidate-directory
              (conj worker-base "accept"
                    (.getPath pre-receipt-file) source-path))
            live
            (run-process!
              candidate-directory
              (conj worker-base "live-success" (.getPath workspace)))
            exact-receipt?
            (= pre-receipt candidate-receipt (:receipt-source live))
            live-projection?
            (= (get-in pre [:scenarios :success :legacy-result])
               (:legacy-result live))
            shadow-safe?
            (and (= 1 (get-in live [:calls :authoritative-commit]))
                 (= 1 (get-in live [:calls :live-receipt]))
                 (every?
                   (fn [snapshot]
                     (every?
                       (fn [case]
                         (zero? (get-in case [:calls :authoritative-commit])))
                       (vals (:scenarios snapshot))))
                   [pre candidate-cli candidate-mcp]))
            result
            {:candidate-commit candidate-commit
             :pre-cutover-commit pre-cutover-commit
             :model-calls 0
             :analyzer-launches 0
             :authoritative-live-commits
             (get-in live [:calls :authoritative-commit])
             :live-receipts (get-in live [:calls :live-receipt])
             :parity parity
             :scenario-evidence
             (scenario-evidence pre candidate-cli candidate-mcp)
             :all-case-gates-pass (all-case-gates-pass? parity)
             :exact-receipt-source exact-receipt?
             :receipt-source-sha256 (sha256 pre-receipt)
             :receipt-data-hash
             (get-in pre [:scenarios :success :receipt-data-hash])
             :pre-accepts-candidate-receipt
             (:accepted pre-accepts-candidate)
             :candidate-accepts-pre-receipt
             (:accepted candidate-accepts-pre)
             :cross-version-inverse-equal
             (= (:inverse-facts pre-accepts-candidate)
                (:inverse-facts candidate-accepts-pre))
             :pre-cross-version-inverse-hash
             (:inverse-facts-hash pre-accepts-candidate)
             :candidate-cross-version-inverse-hash
             (:inverse-facts-hash candidate-accepts-pre)
             :candidate-live-projection-exact live-projection?
             :candidate-live-result-hash (:legacy-result-hash live)
             :candidate-live-source-exact (= future-source (:source-after live))
             :shadow-safe shadow-safe?}]
        (let [result
              (assoc result
                     :all-correct
                     (every? true?
                             [(:all-case-gates-pass result)
                              (:exact-receipt-source result)
                              (:pre-accepts-candidate-receipt result)
                              (:candidate-accepts-pre-receipt result)
                              (:cross-version-inverse-equal result)
                              (:candidate-live-projection-exact result)
                              (:candidate-live-source-exact result)
                              (:shadow-safe result)]))]
          (assoc result :report-hash (data-hash result))))
      (finally
        (delete-tree! root)))))

(defn worker-main
  [[operation & args]]
  (case operation
    "snapshot"
    (let [[entrance workspace] args]
      (prn (run-fake-snapshot (keyword entrance) workspace)))

    "accept"
    (let [[receipt-file source-path] args]
      (prn (accept-receipt receipt-file source-path)))

    "live-success"
    (let [[workspace] args]
      (prn (run-live-success workspace)))

    (throw (ex-info "Unknown compatibility worker operation"
                    {:operation operation}))))

(defn main
  [args]
  (if (= "--worker" (first args))
    (worker-main (rest args))
    (let [repository (or (first args) (System/getProperty "user.dir"))
          script (.getCanonicalPath (io/file *file*))]
      (prn (report repository script)))))

(when (= *file* (System/getProperty "babashka.file"))
  (main *command-line-args*))
