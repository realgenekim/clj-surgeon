(ns clj-surgeon.mission-typist-executor
  "Flagged owner-forms executor. Frozen plan authority, staged proof, guarded commit."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.file-ops :as file-ops]
   [clj-surgeon.mcp-change-buffer :as buffer]
   [clj-surgeon.mcp-extraction :as extraction]
   [clj-surgeon.mcp-formatter :as formatter]
   [clj-surgeon.mcp-process :as process]
   [clj-surgeon.mission :as mission]
   [clj-surgeon.mission-candidate-race :as race]
   [clj-surgeon.mission-events :as mission-events]
   [clj-surgeon.mission-forms :as forms]
   [clj-surgeon.mission-plain-forms :as plain-forms]
   [clj-surgeon.mission-typist :as typist]
   [clj-surgeon.mission-usage :as usage]
   [clj-surgeon.outline :as outline]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [rewrite-clj.node :as node]
   [rewrite-clj.parser :as parser])
  (:import
   (java.nio.file Files LinkOption)
   (java.nio.file.attribute FileAttribute)))

(defn refuse [reason]
  {:ok false :committed false :error-type reason :mutation-attempted false})

(defn reject! [reason] (throw (ex-info "Typist boundary refused" {:error-type reason})))

(defn relative-file? [path]
  (and (string? path) (<= 1 (count path) 1024)
       (not (str/starts-with? path "/")) (not (re-find #"[\\:\u0000]" path))
       (every? #(not (contains? #{"" "." ".."} %)) (str/split path #"/"))))

(defn confined-file [root rel]
  (when-not (relative-file? rel) (reject! :typist-invalid-path))
  (let [root-path (.toPath (.getCanonicalFile (io/file root)))
        target (.resolve root-path rel)]
    (loop [path target]
      (when-not (= path root-path)
        (when (or (nil? path) (Files/isSymbolicLink path)) (reject! :typist-symlink))
        (recur (.getParent path))))
    (when-not (and (.startsWith target root-path)
                   (Files/isRegularFile target (into-array LinkOption [LinkOption/NOFOLLOW_LINKS])))
      (reject! :typist-missing-file))
    (str target)))

(defn read-bounded
  ([file] (read-bounded file 262144))
  ([file limit]
   (with-open [in (io/input-stream file)]
     (let [bytes (.readNBytes in (inc limit))]
       (when (> (alength bytes) limit) (reject! :typist-file-budget))
       (let [text (String. bytes java.nio.charset.StandardCharsets/UTF_8)]
         (when-not (java.util.Arrays/equals bytes (.getBytes text java.nio.charset.StandardCharsets/UTF_8))
           (reject! :typist-invalid-utf8))
         text)))))

(defn plain-source! [source]
  (when (> (count (take 2049 (filter #{\( \[ \{} source))) 2048)
    (reject! :typist-parser-budget))
  (let [tree (parser/parse-string-all source)]
    (when (some #(contains? #{:eval :reader-macro} (node/tag %))
                (tree-seq node/inner? node/children tree))
      (reject! :typist-unsupported-reader-syntax))))

(defn owner-span [sources {:keys [file owner new-owner]}]
  (when-not (and (typist/relative-source? file) (typist/nonblank? owner)
                 (or (nil? new-owner) (typist/nonblank? new-owner)))
    (reject! :typist-invalid-owner))
  (let [source (get sources file)
        _ (when-not source (reject! :typist-missing-owner-source))
        matches (filter #(= owner (str (:name %))) (outline/top-level-form-records file source))]
    (when-not (= 1 (count matches)) (reject! :typist-ambiguous-owner))
    (let [form (:source (first matches))
          _ (forms/definition form)
          start (.indexOf ^String source ^String form)]
      (when (or (neg? start) (not= -1 (.indexOf ^String source ^String form (inc start))))
        (reject! :typist-ambiguous-owner-span))
      (cond-> {:file file :owner owner :start start :end (+ start (count form))}
        new-owner (assoc :new-owner new-owner)))))

(defn valid-commands? [commands]
  (and (vector? commands) (<= 1 (count commands) 8)
       (every? #(and (vector? %) (<= 1 (count %) 32)
                     (every? typist/nonblank? %)) commands)))

(defn proof-authority [profiles id]
  (let [profile (get profiles id)]
    (when-not (and (typist/nonblank? id) (valid-commands? (:commands profile))
                   (typist/nonblank? (:evidence profile))
                   (number? (:measured-ms profile)) (<= 0 (:measured-ms profile) 4999))
      (reject! :typist-invalid-proof-profile))
    {:id id :commands (:commands profile) :measured-ms (:measured-ms profile)
     :evidence (:evidence profile)}))

(defn transport-authority []
  (let [resource (io/resource "clj_surgeon/mission_typist_executor.clj")]
    (when-not (= "file" (some-> resource .getProtocol))
      (reject! :typist-transport-installation-unavailable))
    (let [tool-root (-> (io/file (.toURI resource)) .getParentFile .getParentFile .getParentFile)
          script (io/file tool-root "bin" "typist_transport.py")
          source (read-bounded script)]
      {:source source :sha256 (mission/sha256 source) :origin (.getCanonicalPath script)
       :interpreter "/usr/bin/python3"})))

(defn plan
  "Freeze explicit source/proof files and owner definitions. No provider or writes."
  [request profiles]
  (try
    (let [root (some-> (:workspace_root request) io/file .getCanonicalPath)
          selected (:owners request)
          proof-files (:proof-files request)
          _ (when-not (and root (vector? selected) (<= 1 (count selected) 128)
                           (vector? proof-files) (<= (count proof-files) 64))
              (reject! :typist-incomplete-file-set))
          files (vec (distinct (concat (map :file selected) proof-files)))
          _ (when (> (count files) 64) (reject! :typist-file-budget))
          absolute (into (sorted-map) (map #(vector % (confined-file root %))) files)
          sources (into (sorted-map) (map (fn [[rel abs]] [rel (read-bounded abs)])) absolute)
          _ (when (> (reduce + (map count (vals sources))) 4194304) (reject! :typist-source-budget))
          _ (doseq [file (distinct (map :file selected))] (plain-source! (get sources file)))
          owners (mapv #(owner-span sources %) selected)
          _ (when-not (= (count owners) (count (set (map (juxt :file :owner) owners))))
              (reject! :typist-duplicate-owner))
          gate (proof-authority profiles (get-in request [:verification :profile]))
          acceptance (proof-authority profiles (:acceptance_profile request))
          _ (when (= (:commands gate) (:commands acceptance))
              (reject! :typist-identical-proof-commands))
          target-sources (select-keys sources (map :file owners))
          facts (merge (:typist request)
                       {:enabled? true :discovery-complete? true :intent (:intent request)
                        :owners owners :sources target-sources :gate gate :acceptance acceptance
                        :commit {:atomic? true :rollback? true}})
          dossier (typist/dossier facts)]
      (if-not (:ok dossier)
        dossier
        {:ok true :sources (into (sorted-map) (map (fn [[rel abs]] [abs (get sources rel)])) absolute)
         :typist {:root root :absolute absolute :frozen-files sources
                  :transport (transport-authority)
                  :modes (into {} (map (fn [[rel abs]]
                                         [rel (set (map str (Files/getPosixFilePermissions (.toPath (io/file abs))
                                                              (make-array LinkOption 0))))])) absolute)
                  :basis {:sources target-sources :owners owners :budget (:budget facts)}
                  :dossier dossier :route (get-in dossier [:dossier :route])
                  :gate gate :acceptance acceptance}}))
    (catch StackOverflowError _ (refuse :typist-parser-depth))
    (catch Exception e (refuse (or (:error-type (ex-data e)) :typist-plan-invalid)))))

(defn format-replacements! [root replacements]
  (let [files (into (sorted-map)
                    (map-indexed (fn [i r] [(str "owner-" i ".clj") (:form r)])) replacements)
        result (formatter/format-candidates! root formatter/default-command files)]
    (if-not (:ok result)
      (refuse :typist-formatter-failed)
      {:ok true :format (select-keys result [:status :elapsed_ms :file-count :changed-file-count])
       :replacements (mapv (fn [i r]
                             (assoc r :form (get-in result [:future-sources (str "owner-" i ".clj")])))
                       (range) replacements)})))

(defn compile-formatted!
  "Validate before invoking a formatter; then validate its owned-fragment output."
  [authority replacements]
  (let [initial (forms/compile-forms (:basis authority) replacements)]
    (if-not (:ok initial)
      initial
      (let [formatted (format-replacements! (:root authority) replacements)]
        (if (:ok formatted)
          (let [compiled (forms/compile-forms (:basis authority) (:replacements formatted))]
            (if (:ok compiled)
              (assoc compiled :form-count (count replacements) :format (:format formatted))
              compiled))
          formatted)))))

(defn unchanged? [authority]
  (try
    (every? (fn [[rel source]]
              (let [file (confined-file (:root authority) rel)]
                (and (= file (get-in authority [:absolute rel]))
                     (= source (read-bounded file))
                     (= (get-in authority [:modes rel])
                        (set (map str (Files/getPosixFilePermissions (.toPath (io/file file))
                                        (make-array LinkOption 0))))))))
            (:frozen-files authority))
    (catch Exception _ false)))

(defn parse-candidate [content]
  (try
    (when-not (and (string? content) (<= (count content) 262144))
      (reject! :typist-output-budget))
    ;; JSON only: never read or evaluate arbitrary Clojure to decode the envelope.
    (json/parse-string-strict content true)
    (catch StackOverflowError _ nil)
    (catch Exception _ nil)))

(defn compile-candidate! [authority candidate]
  (if-not (:usable candidate)
    (refuse :typist-candidate-unusable)
    (case (get-in authority [:route :candidate-format] :owner-forms)
      :clojure-forms
      (let [decoded (plain-forms/compile-response (:basis authority) (:content candidate))]
        (if (:ok decoded)
          (compile-formatted! authority (:replacements decoded))
          decoded))
      :owner-forms
      (if-let [replacements (parse-candidate (:content candidate))]
        (compile-formatted! authority replacements)
        (refuse :typist-candidate-unusable))
      (refuse :typist-candidate-format))))

(defn request-one! [authority index processes]
  (let [route (case (get-in authority [:route :provider :id])
                :openrouter "openrouter-cerebras" :groq "groq" nil)
        _ (when-not route (reject! :typist-executor-provider-unavailable))
        transport (:transport authority)
        _ (when-not (and (= "/usr/bin/python3" (:interpreter transport))
                         (= (:sha256 transport) (mission/sha256 (:source transport))))
            (reject! :typist-transport-identity-mismatch))
        script (java.io.File/createTempFile "typist-client-" ".py")
        _ (spit script (:source transport))
        generation (get-in authority [:route :generation])
        config (cond-> {:route route :prompt (get-in authority [:dossier :prompt])
                        :candidates 1 :max_tokens (get generation :max-tokens 8192) :timeout_s 30}
                 (:fallback generation)
                 (assoc :fallback {:provider (name (get-in generation [:fallback :provider]))
                                   :max_tokens (get-in generation [:fallback :max-tokens])}))
        result (try (process/run-bounded! {:command [(:interpreter transport) "-I" (str script)]
                                           :cwd (:root authority)
                                           :stdin-text (json/generate-string config)
                                           :timeout-ms 35000 :visible-byte-limit 1258291
                                           :on-start (fn [pid]
                                                       (try
                                                         (let [handle (java.lang.ProcessHandle/of pid)]
                                                           (when (.isPresent handle)
                                                             (swap! processes assoc index (.get handle))))
                                                         (catch Exception _
                                                           (swap! processes assoc index ::untracked))))})
                    (finally (.delete script)))]
    (when-not (and (:finished? result) (contains? #{0 2} (:exit result))
                   (not (:out-truncated result)) (:termination-confirmed result))
      (reject! :typist-transport-failed))
    (let [response (json/parse-string (:out result) true)
          candidates (:candidates response)]
      (when-not (and (vector? candidates) (= 1 (count candidates)))
        (reject! :typist-transport-invalid-response))
      (mission-events/record-provider-fallback! (first candidates))
      (assoc (first candidates) :transport-wall-ms (:elapsed_ms result)))))

(defn request-candidates! [authority]
  (let [processes (atom {})
        context mission-events/*context*
        handle (race/start! (get-in authority [:route :k])
                 (fn [index]
                   (binding [mission-events/*context* context]
                     (request-one! authority index processes))))]
    (assoc handle :transport-processes processes)))

(defn candidate-sequence [handle]
  (if (vector? handle)
    handle
    (letfn [(step [] (lazy-seq (when-let [candidate (race/next! handle)]
                                 (cons candidate (step)))))]
      (step))))

(defn close-candidates! [handle artifacts]
  (let [result (if (vector? handle)
                 {:terminated? true :completed handle :cancelled []}
                 (let [closed (race/close! handle)
                       alive (vec (keep (fn [[index process]]
                                          (when (or (= ::untracked process) (.isAlive ^java.lang.ProcessHandle process)) index))
                                        @(:transport-processes handle)))]
                   (assoc closed :live-processes alive
                          :terminated? (and (:terminated? closed) (empty? alive)))))
        result (assoc result :cancelled-usage (when (seq (:cancelled result)) :unknown))]
    (file-ops/atomic-write! (str (io/file artifacts "transport-close.edn")) (pr-str result))
    result))

(defn make-artifacts! [config]
  (let [parent (io/file (or (:receipt-dir config)
                            (str (System/getProperty "user.home") "/.local/state/clj-surgeon/typist")))]
    (.mkdirs parent)
    (str (Files/createTempDirectory (.toPath parent) "mission-" (make-array FileAttribute 0)))))

(defn delete-tree! [root]
  (with-open [paths (Files/walk (.toPath (io/file root)) (make-array java.nio.file.FileVisitOption 0))]
    (doseq [path (reverse (vec (iterator-seq (.iterator paths))))]
      (Files/deleteIfExists path))))

(defn materialize! [dir sources modes]
  (doseq [[rel source] sources]
    (when-not (relative-file? rel) (reject! :typist-invalid-path))
    (let [file (io/file dir rel)]
      (.mkdirs (.getParentFile file))
      (spit file source)
      (when-let [permissions (get modes rel)]
        (Files/setPosixFilePermissions (.toPath file)
          (set (map #(java.nio.file.attribute.PosixFilePermission/valueOf %) permissions)))))))

(defn run-proof! [dir authority]
  (let [results (mapv #(buffer/run-process! dir % 5000 4096) (:commands authority))]
    {:ok (every? #(and (:finished? %) (zero? (or (:exit %) -1))) results)
     :id (:id authority) :evidence (:evidence authority)
     :results (mapv #(select-keys % [:finished? :exit :elapsed_ms :output-sha256
                                     :output-bytes :output-truncated]) results)}))

(defn verify-candidate! [authority compiled]
  (let [dir (str (Files/createTempDirectory "typist-proof-" (make-array FileAttribute 0)))
        expected (merge (:frozen-files authority) (:future-sources compiled))]
    (try
      (materialize! dir expected (:modes authority))
      (let [gate (run-proof! dir (:gate authority))
            acceptance (when (:ok gate) (run-proof! dir (:acceptance authority)))
            unchanged (every? (fn [[rel source]]
                                (let [file (confined-file dir rel)]
                                  (and (= source (read-bounded file))
                                       (= (get-in authority [:modes rel])
                                          (set (map str (Files/getPosixFilePermissions
                                                          (.toPath (io/file file))
                                                          (make-array LinkOption 0)))))))) expected)]
        {:ok (boolean (and (:ok gate) (:ok acceptance) unchanged))
         :gate gate :acceptance acceptance :proof-inputs-unchanged unchanged})
      (finally (delete-tree! dir)))))

(defn commit-candidate! [authority compiled artifacts config]
  (if-not (unchanged? authority)
    (refuse :typist-stale-plan)
    (let [absolute (fn [sources]
                     (into (sorted-map) (map (fn [[rel source]]
                                               [(get-in authority [:absolute rel]) source])) sources))
          compiled (assoc compiled :original-sources (absolute (:original-sources compiled))
                          :future-sources (absolute (:future-sources compiled))
                          :caller-edit-count 0
                          :created-files [] :created-directories [])
          raw-inverse (assoc (extraction/build-receipt compiled)
                             :file-modes (into {} (map (fn [[rel modes]]
                                                         [(get-in authority [:absolute rel]) modes]))
                                               (select-keys (:modes authority) (keys (:sources (:basis authority))))))
          inverse (assoc raw-inverse :receipt-hash
                         (mission/sha256 (pr-str (dissoc raw-inverse :receipt-hash))))
          receipt-file (str (io/file artifacts "undo.edn"))]
      ;; Durable inverse exists before the first live write; a crashed ledger apply
      ;; retains its :applied state and this artifact for explicit recovery.
      (file-ops/atomic-write! receipt-file (pr-str inverse))
      (when-let [persist! (:persist-recovery! config)]
        (persist! {:receipt receipt-file :receipt_hash (:receipt-hash inverse) :artifacts artifacts}))
      (let [result (extraction/commit! compiled)]
        (assoc (dissoc result :receipt) :undo_receipt receipt-file :receipt_hash (:receipt-hash inverse)
               :mutation-attempted true)))))

(defn candidate-refusal! [artifacts index compiled]
  (when-not (:ok compiled)
    (let [path (str (io/file artifacts (str "candidate-" index "-diagnostic.edn")))
          diagnostic (select-keys compiled [:error-type :error :condition :lost :moved :next_call])
          bounded (if (<= (count (pr-str diagnostic)) 4096)
                    diagnostic
                    {:error-type (:error-type compiled) :truncated true})]
      (file-ops/atomic-write! path (pr-str compiled))
      {:refusal bounded :diagnostics-file path})))

(defn execute!
  "Apply uses saved plan authority only. Tests replace transport, not proof/commit."
  [_request config]
  (let [started (System/nanoTime)
        authority (get-in config [:plan :typist])
        closed-snapshot (atom nil)]
    (try
      (when-not (and authority (unchanged? authority)) (reject! :typist-stale-plan))
      (let [artifacts (make-artifacts! config)
            _ (file-ops/atomic-write! (str (io/file artifacts "authority.edn")) (pr-str authority))
            handle (request-candidates! authority)
            closed? (atom false)]
        (try
          (loop [pending (candidate-sequence handle) ordinal 0 receipts []]
            (if-let [candidate (first pending)]
              (let [index (or (:index candidate) ordinal)
                    _ (file-ops/atomic-write! (str (io/file artifacts (str "candidate-" index ".edn")))
                        (pr-str candidate))
                    compiled (compile-candidate! authority candidate)
                    proof (when (:ok compiled)
                            (mission-events/observe-phase! "verify"
                              #(verify-candidate! authority compiled)))
                    receipt (merge {:index index :compiled (:ok compiled) :proof proof
                                    :error-type (:error-type compiled)}
                                   (candidate-refusal! artifacts index compiled))
                    receipts (conj receipts receipt)]
                (file-ops/atomic-write! (str (io/file artifacts "candidates.edn")) (pr-str receipts))
                (if (:ok proof)
                  (let [closed (close-candidates! handle artifacts)
                        _ (reset! closed-snapshot closed)]
                    (when-not (:terminated? closed) (reject! :typist-transport-cleanup-incomplete))
                    (reset! closed? true)
                    (let [committed (mission-events/observe-phase! "commit"
                                      #(commit-candidate! authority compiled artifacts config))]
                      (assoc committed :executor :typist :route (:route authority) :candidates receipts
                             :artifacts artifacts :usage (usage/summarize closed) :transport (dissoc closed :completed)
                             :verification-complete (true? (:committed committed))
                             :elapsed_ms (/ (double (- (System/nanoTime) started)) 1000000.0))))
                  (recur (rest pending) (inc ordinal) receipts)))
              (let [closed (close-candidates! handle artifacts)
                    _ (reset! closed-snapshot closed)]
                (reset! closed? (:terminated? closed))
                (assoc (refuse (if (:terminated? closed) :typist-all-candidates-rejected
                                 :typist-transport-cleanup-incomplete))
                       :executor :typist :route (:route authority) :candidates receipts
                       :usage (usage/summarize closed) :transport (dissoc closed :completed) :artifacts artifacts))))
          (finally (when-not @closed?
                     (reset! closed-snapshot (close-candidates! handle artifacts))))))
      (catch Exception e
        (assoc (refuse (or (:error-type (ex-data e)) :typist-execution-failed))
               :usage (usage/summarize @closed-snapshot))))))

(defn undo!
  ([receipt-file] (undo! receipt-file nil))
  ([receipt-file expected-hash]
   (try
     (let [receipt (edn/read-string (read-bounded receipt-file 16777216))
           computed (mission/sha256 (pr-str (dissoc receipt :receipt-hash)))]
       (cond
         (not (and (= computed (:receipt-hash receipt))
                   (or (nil? expected-hash) (= expected-hash computed))))
         (refuse :typist-invalid-undo-hash)
         (not (every? (fn [[file modes]]
                        (= modes (set (map str (Files/getPosixFilePermissions
                                                 (.toPath (io/file file))
                                                 (make-array LinkOption 0))))))
                      (:file-modes receipt)))
         (refuse :typist-stale-undo-mode)
         :else (extraction/undo! receipt)))
     (catch Exception _ (refuse :typist-invalid-undo)))))
