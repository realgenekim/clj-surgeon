(ns clj-surgeon.mission-git-ledger
  "Saved owner_forms receipt to Git. No caller-supplied proof authority."
  (:require
   [clj-surgeon.mission :as mission]
   [clj-surgeon.mission-git :as git]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.nio.file Files LinkOption OpenOption)))

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

(defn commit!
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
        (git/commit! (:provenance result)
                     #(and (= (:hash ledger) (:hash (artifact ledger-file)))
                           (= (:hash inverse-bytes) (:hash (artifact inverse-file)))))))
    (catch StackOverflowError _ (git/refuse :git-ledger-depth))
    (catch Exception e (git/refuse (or (:error-type (ex-data e)) :git-ledger-invalid)))))
