(ns clj-surgeon.mission-git-ledger
  "Saved owner_forms receipt to Git. No caller-supplied proof authority."
  (:require
   [clj-surgeon.mission :as mission]
   [clj-surgeon.mission-git :as git]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.nio.channels FileChannel)
   (java.nio.file Files LinkOption OpenOption StandardCopyOption StandardOpenOption)))

(def artifact-cap 16777216)
(defn fail! [reason] (throw (ex-info "Saved mission Git admission refused" {:error-type reason})))
(defn require! [condition reason] (when-not condition (fail! reason)))
(defn nonblank? [s] (and (string? s) (<= 1 (count s) 8192) (not (str/blank? s))))

(defn commands? [commands]
  (and (vector? commands) (<= 1 (count commands) 32)
       (every? #(and (vector? %) (<= 1 (count %) 64) (every? nonblank? %)) commands)))

(defn proof-valid? [authority proof]
  (and (commands? (:commands authority)) (nonblank? (:id authority))
       (nonblank? (:evidence authority)) (true? (:ok proof))
       (= (:id authority) (:id proof)) (= (:evidence authority) (:evidence proof))
       (vector? (:results proof)) (= (count (:commands authority)) (count (:results proof)))
       (every? #(and (true? (:finished? %)) (= 0 (:exit %))
                  (git/hash? (:output-sha256 %))) (:results proof))))

(defn normalize
  "Pure extraction. Receipt and proof are saved execution records, never request data."
  [m inverse ledger-hash root]
  (try
    (require! (and (= :verified (:state m)) (= "owner_forms" (:verb m))
                   (= root (:root m) (get-in m [:plan :typist :root]))
                   (true? (get-in m [:plan :ok])) (git/hash? ledger-hash)) :git-ledger-not-verified)
    (let [terminal (:receipt m) authority (get-in m [:plan :typist])
          candidates (:candidates terminal)
          _ (require! (and (true? (:ok terminal)) (true? (:committed terminal))
                        (true? (:verification-complete terminal)) (= :typist (:executor terminal))
                        (true? (get-in terminal [:verified :whole-files]))
                        (true? (get-in terminal [:verified :read-back]))
                        (vector? candidates) (<= 1 (count candidates) 5)) :git-ledger-incomplete-receipt)
          winners (filter #(and (true? (:compiled %)) (true? (get-in % [:proof :ok]))) candidates)
          _ (require! (= 1 (count winners)) :git-ledger-ambiguous-proof)
          proof (:proof (first winners))
          _ (require! (and (true? (:proof-inputs-unchanged proof))
                        (proof-valid? (:gate authority) (:gate proof))
                        (proof-valid? (:acceptance authority) (:acceptance proof))
                        (not= (get-in authority [:gate :commands]) (get-in authority [:acceptance :commands]))
                        (not= (get-in authority [:gate :id]) (get-in authority [:acceptance :id]))
                        (not= (get-in authority [:gate :evidence]) (get-in authority [:acceptance :evidence])))
                      :git-ledger-proof-incomplete)
          inverse-hash (mission/sha256 (pr-str (dissoc inverse :receipt-hash)))
          _ (require! (and (= :compiled-extraction (:operation inverse))
                        (= inverse-hash (:receipt-hash inverse)
                           (get-in m [:undo :receipt_hash]) (:receipt_hash terminal))
                        (= (get-in m [:undo :receipt]) (:undo_receipt terminal))
                        (empty? (:created-directories inverse))) :git-ledger-invalid-inverse)
          entries (:files inverse)
          _ (require! (and (vector? entries) (<= 1 (count entries) 64)
                        (= (count entries) (count (set (map :file entries))))) :git-ledger-invalid-files)
          sources (get-in m [:plan :sources])
          _ (require! (and (map? sources) (= (:snapshot m) (mission/snapshot sources))) :git-ledger-invalid-snapshot)
          absolute (:absolute authority)
          owner-files (set (map :file (get-in authority [:basis :owners])))
          normalized
          (into (sorted-map)
                (for [{:keys [file original-source result-source source-hash result-hash absent-before]} entries]
                  (let [rel (some (fn [[rel abs]] (when (= abs file) rel)) absolute)]
                    (require! (and (git/path? rel) (contains? owner-files rel)
                                   (= file (str (io/file root rel)))
                                   (not absent-before) (string? original-source) (string? result-source)
                                   (<= (count original-source) git/max-bytes) (<= (count result-source) git/max-bytes)
                                   (= original-source (get sources file) (get-in authority [:frozen-files rel]))
                                   (= source-hash (mission/sha256 original-source))
                                   (= result-hash (mission/sha256 result-source)
                                      (get-in terminal [:verified :read-back-hashes file]))
                                   (not= original-source result-source)) :git-ledger-file-mismatch)
                    [rel {:before-sha256 source-hash :after-sha256 result-hash}])))
          provenance {:id (:id m) :state :verified :workspace-root root
                      :ledger-sha256 ledger-hash :receipt-sha256 inverse-hash
                      :gate {:ok true :sha256 (mission/sha256 (pr-str [(:gate authority) (:gate proof)]))}
                      :acceptance {:ok true :sha256 (mission/sha256 (pr-str [(:acceptance authority) (:acceptance proof)]))}
                      :files normalized}]
      (require! (and (= (count entries) (:changed-file-count terminal)
                        (get-in terminal [:verified :file-count]))
                     (= (set (map :file entries)) (set (keys (get-in terminal [:verified :read-back-hashes])))))
                :git-ledger-file-count)
      (require! (git/valid-provenance? provenance) :git-ledger-invalid-provenance)
      {:ok true :provenance provenance})
    (catch StackOverflowError _ (git/refuse :git-ledger-depth))
    (catch Exception e (git/refuse (or (:error-type (ex-data e)) :git-ledger-invalid)))))

(defn artifact [path]
  (let [file (.toPath (io/file path))]
    (require! (and (.isAbsolute file) (= path (.getCanonicalPath (io/file path)))
                   (Files/isRegularFile file (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))
                   (<= (Files/size file) artifact-cap)) :git-ledger-artifact-unavailable)
    (with-open [stream (Files/newInputStream file (into-array OpenOption [LinkOption/NOFOLLOW_LINKS]))]
      (let [bytes (.readNBytes stream (inc artifact-cap))]
        (require! (<= (alength bytes) artifact-cap) :git-ledger-artifact-limit)
        {:hash (git/digest bytes) :text (str (.decode (doto (.newDecoder java.nio.charset.StandardCharsets/UTF_8) (.onMalformedInput java.nio.charset.CodingErrorAction/REPORT) (.onUnmappableCharacter java.nio.charset.CodingErrorAction/REPORT)) (java.nio.ByteBuffer/wrap bytes)))}))))

(defn publication-path [{:keys [workspace state-home id]}]
  (str (io/file (mission/missions-dir (mission/workspace-state-dir workspace state-home))
                (str id ".git-publication.edn"))))

(defn with-publication-lock [{:keys [workspace id] :as opts} f]
  (if-not (and (string? workspace) (seq workspace) (string? id)
               (re-matches #"M-[0-9]{1,12}" id))
    (git/refuse :mission-publication-invalid-options)
    (let [channel (try
                    (let [file (io/file (str (publication-path opts) ".lock"))]
                      (io/make-parents file)
                      (FileChannel/open (.toPath file)
                        (into-array OpenOption [StandardOpenOption/CREATE StandardOpenOption/WRITE LinkOption/NOFOLLOW_LINKS])))
                    (catch Exception _ nil))]
      (if-not channel
        (git/refuse :mission-publication-lock-unavailable)
        (let [locked (try (some? (.tryLock channel)) (catch Exception _ false))]
          (try (if locked (f) (git/refuse :mission-publication-lock-busy))
               (finally (try (.close channel) (catch Exception _ nil)))))))))

(defn marker-valid? [marker opts ledger]
  (and (map? marker)
       (every? #{:version :id :workspace-root :ledger-sha256 :status :commit :tree :parent :possible-commit} (keys marker))
       (= 1 (:version marker)) (= (:id opts) (:id marker))
       (= (.getCanonicalPath (io/file (:workspace opts))) (:workspace-root marker))
       (git/hash? (:ledger-sha256 marker))
       (contains? #{:pending :published :uncertain} (:status marker))
       (or (= (:ledger-sha256 marker) (:hash ledger))
           (= marker (get-in ledger [:saved :git-publication])))
       (every? (fn [k] (or (not (contains? marker k)) (git/oid? (get marker k))))
               [:commit :tree :parent :possible-commit])
       (or (not= :published (:status marker))
           (every? #(git/oid? (get marker %)) [:commit :tree :parent]))))

(defn publication-status [{:keys [workspace state-home id] :as opts}]
  (let [file (.toPath (io/file (publication-path opts)))
        exists? (Files/exists file (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))
        ledger (try (let [a (artifact (str (mission/mission-file
                                             (mission/workspace-state-dir workspace state-home) id)))]
                      (assoc a :saved (edn/read-string (:text a))))
                    (catch Throwable _ nil))]
    (when (or exists? (contains? (:saved ledger) :git-publication))
      (try
        (let [marker (if exists?
                       (do (require! (<= (Files/size file) 4096) :git-publication-marker-limit)
                           (edn/read-string (:text (artifact (str file)))))
                       (get-in ledger [:saved :git-publication]))]
          (if (marker-valid? marker opts ledger)
            marker
            {:status :invalid :error-type :git-publication-marker-invalid}))
        (catch Throwable _ {:status :invalid :error-type :git-publication-marker-invalid})))))

(defn force-directory! [path]
  (with-open [channel (FileChannel/open path (into-array OpenOption [StandardOpenOption/READ]))]
    (.force channel true)))

(defn write-publication! [opts marker]
  (let [target (.toPath (io/file (publication-path opts)))
        parent (.getParent target)
        bytes (.getBytes (str (pr-str marker) "\n") "UTF-8")]
    (require! (<= (alength bytes) 4096) :git-publication-marker-limit)
    (let [temporary (Files/createTempFile parent ".publication-" ".edn"
                      (make-array java.nio.file.attribute.FileAttribute 0))]
      (try
        (with-open [channel (FileChannel/open temporary
                              (into-array OpenOption [StandardOpenOption/WRITE LinkOption/NOFOLLOW_LINKS]))]
          (let [buffer (java.nio.ByteBuffer/wrap bytes)]
            (while (.hasRemaining buffer) (.write channel buffer)))
          (.force channel true))
        (Files/move temporary target
          (into-array java.nio.file.CopyOption [StandardCopyOption/ATOMIC_MOVE StandardCopyOption/REPLACE_EXISTING]))
        (force-directory! parent)
        (require! (= marker (edn/read-string (:text (artifact (str target))))) :git-publication-marker-write-failed)
        marker
        (finally (Files/deleteIfExists temporary))))))

(defn clear-publication! [opts]
  (let [path (.toPath (io/file (publication-path opts)))]
    (Files/deleteIfExists path)
    (force-directory! (.getParent path))))

(def proven-pre-ref-refusals
  #{:git-invalid-provenance :git-wrong-root :git-unsupported-head :git-staged-scope
    :git-stale-or-unsupported-files :git-observation-drift :git-invalid-commit-object
    :git-stale-ledger :git-lock-busy :git-identity-unavailable})

(defn record-publication-result! [opts saved intent result]
  (if (and (false? (:git-ref-updated result)) (contains? proven-pre-ref-refusals (:error-type result)))
    (try (clear-publication! opts) result
         (catch Exception _ (assoc result :metadata-recorded false :publication-status :pending)))
    (let [published? (true? (:git-ref-updated result))
          result (if published? result (assoc result :ok false :git-ref-updated :unknown))
          marker (merge intent {:status (if published? :published :uncertain)}
                        (select-keys result [:commit :tree :parent :possible-commit]))]
      (try
        (write-publication! opts marker)
        (mission/write-mission! (mission/workspace-state-dir (:workspace opts) (:state-home opts))
          (assoc saved :git-publication marker :next-action nil))
        (let [state-dir (mission/workspace-state-dir (:workspace opts) (:state-home opts))
              saved-again (edn/read-string (:text (artifact (str (mission/mission-file state-dir (:id opts))))))]
          (require! (= marker (:git-publication saved-again)) :git-publication-ledger-write-failed))
        (require! (= marker (publication-status opts)) :git-publication-marker-write-failed)
        (assoc result :metadata-recorded true :git-publication marker)
        (catch Throwable _
          (assoc result :ok false :metadata-recorded false
                        :error-type :git-publication-metadata-failed
                        :decision "Git outcome is preserved in this response. Inspect Git and the publication sidecar before any recovery; source undo remains blocked."))))))

(defn undo-publication-refusal [opts]
  (when-let [publication (publication-status opts)]
    (mission/refusal "undo-after-git-publication"
      "Source undo is blocked because Git publication succeeded or requires recovery. Git will not be undone automatically; inspect the branch and publication records."
      (cond-> {:id (:id opts) :publication-status (:status publication)
               :git-publication publication :mutation_attempted false
               :source-mutation-attempted false
               :decision "Inspect Git and the saved publication record before recovery."}
        (:commit publication) (assoc :published-commit (:commit publication))
        (:possible-commit publication) (assoc :possible-commit (:possible-commit publication))))))

(defn- commit-under-lock!
  "Public mission commit handler. Exact saved proof only; index must already match.
   commit-tree skips Git hooks/signing; no source write, staging, or push."
  [{:keys [id workspace state-home] :as opts}]
  (try
    (require! (and (map? opts) (every? #{:id :workspace :state-home} (keys opts))
                   (string? id) (re-matches #"M-[0-9]{1,12}" id) (string? workspace)
                   (or (nil? state-home) (string? state-home))) :git-ledger-invalid-options)
    (let [root (.getCanonicalPath (io/file workspace))
          state-dir (mission/workspace-state-dir root state-home)
          ledger-file (str (mission/mission-file state-dir id))
          ledger (artifact ledger-file)
          saved (edn/read-string (:text ledger))
          _ (require! (= id (:id saved)) :git-ledger-wrong-id)
          inverse-file (get-in saved [:undo :receipt])
          _ (require! (and (string? inverse-file)
                        (= inverse-file (str (io/file (get-in saved [:receipt :artifacts]) "undo.edn"))))
                      :git-ledger-invalid-inverse-path)
          inverse-bytes (artifact inverse-file)
          inverse (edn/read-string (:text inverse-bytes))
          result (normalize saved inverse (:hash ledger) root)]
      (if-not (:ok result) result
        (let [intent {:version 1 :id id :workspace-root root :ledger-sha256 (:hash ledger) :status :pending}
              _ (write-publication! opts intent)
              outcome (try (git/commit! (:provenance result)
                             #(and (= (:hash ledger) (:hash (artifact ledger-file)))
                                   (= (:hash inverse-bytes) (:hash (artifact inverse-file)))
                                   (= intent (publication-status opts))))
                           (catch Throwable _ {:ok false :git-ref-updated :unknown
                                               :error-type :git-publication-boundary-uncertain}))]
          (record-publication-result! opts saved intent outcome))))
    (catch StackOverflowError _ (git/refuse :git-ledger-depth))
    (catch Exception e (git/refuse (or (:error-type (ex-data e)) :git-ledger-invalid)))))

(defn commit!
  "Publication is serialized with undo and preceded by a forced recovery intent."
  [{:keys [id workspace state-home] :as opts}]
  (if-not (and (map? opts) (every? #{:id :workspace :state-home} (keys opts))
               (string? id) (re-matches #"M-[0-9]{1,12}" id) (string? workspace)
               (or (nil? state-home) (string? state-home)))
    (git/refuse :git-ledger-invalid-options)
    (with-publication-lock opts
      (fn []
        (if-let [publication (publication-status opts)]
          (assoc (git/refuse :git-publication-recovery-required) :git-publication publication
                 :decision "Inspect the existing publication and Git before retrying; no automatic ref undo.")
          (commit-under-lock! opts))))))
