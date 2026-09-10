(ns clj-surgeon.insert-forms
  (:require
   [clj-surgeon.file-ops :as file-ops]
   [clj-surgeon.insert-forms-plan :as p]
   [clj-surgeon.intent-transaction :as transaction]
   [clj-surgeon.receipt-artifacts :as artifacts]
   [clj-surgeon.txn-journal :as journal]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.io ByteArrayOutputStream)
   (java.nio.file Files LinkOption)
   (java.util UUID)))

(defn plan [source request] (p/plan source request))
(defn read-request [text] (p/read-request text))
(def nofollow (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))

(defn read-bounded [file kind]
  (let [limit (p/limits kind)]
    (when (> (Files/size (.toPath (io/file file))) limit)
      (p/refuse! :limit-exceeded [kind] "File exceeds input bound."))
    (with-open [in (io/input-stream file) out (ByteArrayOutputStream.)]
      (let [buf (byte-array 8192)]
        (loop [total 0]
          (let [n (.read in buf)]
            (when (pos? n)
              (when (> (+ total n) limit) (p/refuse! :limit-exceeded [kind] "File grew beyond input bound."))
              (.write out buf 0 n)
              (recur (+ total n))))))
      (p/decode (.toByteArray out) kind))))

;; @spec INSERT-FORMS-015
;; INTENT: INSERT-FORMS-015
(defn target! [request]
  (when (some #(str/includes? % (str (char 0))) [(:workspace_root request) (:file request)])
    (p/refuse! :invalid-path [:file] "NUL is not a filesystem path character."))
  (let [root (.toPath (io/file (:workspace_root request)))
        file (.toPath (io/file (:file request)))
        invalid #(p/refuse! :invalid-path [:file] "Target must be a regular, singly-linked file inside a canonical root.")]
    (when-not (and (.isAbsolute root) (= (str root) (str (.normalize root)))
                   (Files/isDirectory root nofollow) (not (.isAbsolute file))
                   (not-any? #{".." "." ""} (str/split (:file request) #"/"))) (invalid))
    (when-not (str/ends-with? (:file request) ".clj")
      (p/refuse! :unsupported-source [:file] "Only .clj files are supported."))
    (doseq [path (take-while some? (iterate #(.getParent ^java.nio.file.Path %) root))]
      (when (Files/isSymbolicLink path) (invalid)))
    (when-not (= (str root) (str (.toRealPath root (make-array LinkOption 0)))) (invalid))
    (let [target (.resolve root file)]
      (doseq [path (take-while #(and % (not= % root)) (iterate #(.getParent ^java.nio.file.Path %) target))]
        (when (Files/isSymbolicLink path) (invalid)))
      (when-not (and (.startsWith target root) (Files/isRegularFile target nofollow)
                     (= 1 (Files/getAttribute target "unix:nlink" nofollow))) (invalid))
      (.toFile target))))

(defn failed [state attempted unchanged kind message detail]
  (merge {:state state :committed (when-not (= state "recovery-required") false)
          :mutation_attempted attempted :source_unchanged unchanged :ok false
          :operation "insert_forms" :error-type kind :error (p/diagnostic message)
          :next_action (if (= state "recovery-required") "recover" "retry-after-repair")}
         detail))

(defn compiled [file source candidate offset inserted]
  {:ok true :original-sources {file source} :future-sources {file candidate}
   :intent-count 1 :match-count 1 :changed-file-count 1
   :files [{:file file :source-hash (p/sha source) :result-hash (p/sha candidate) :match-count 1
            :edits [{:raw true :offset offset :before "" :after inserted :intent-index 0}]}]})

(defn durable-detail! [request result compiled]
  (let [path (artifacts/target "insert-forms" (:workspace_root request) (str (UUID/randomUUID) ".edn"))
        detail (merge (:detail result) {:receipt (:receipt result)
                                        :transaction_receipt (transaction/build-receipt compiled)})
        text (str (pr-str detail) "\n")]
    (io/make-parents path)
    (file-ops/atomic-write! path text)
    (when-not (= (p/sha text) (journal/sha256-file path))
      (throw (java.io.IOException. "Durable receipt read-back differs.")))
    {:receipt_details_path path :receipt_hash (p/sha text)}))

;; @spec INSERT-FORMS-014
;; INTENT: INSERT-FORMS-014
(defn commit-plan! [request file source result hooks]
  (let [attempted (atom false) hook-fired (atom false) detail (atom {})
        stage (atom nil) seed (atom nil) identity (journal/path-identity file)
        {:keys [candidate offset inserted]} result
        compiled (compiled (str file) source candidate offset inserted)
        hook! (fn [k] (when-let [f (get hooks k)] (f)))
        read-source (fn [path]
                      (when (and @attempted (compare-and-set! hook-fired false true)) (hook! :read-back))
                      (read-bounded path :candidate))]
    (try
      (reset! detail (durable-detail! request result compiled))
      (hook! :stage)
      (let [f (io/file (str (:receipt_details_path @detail) ".candidate"))]
        (reset! seed f)
        (spit f candidate :encoding "UTF-8")
        (reset! stage (file-ops/prepare-publish! file f)))
      (when-not (= (p/sha candidate) (journal/sha256-file @stage))
        (throw (java.io.IOException. "Staged candidate differs.")))
      (hook! :before-recheck)
      (when-not (and (= identity (journal/path-identity (target! request)))
                     (= (p/sha source) (journal/sha256-file file)))
        (p/refuse! :source-changed-before-commit [:guard] "Source identity or bytes changed before commit."
                   {:expected (p/sha source) :actual (journal/sha256-file file)}))
      (let [outcome
            (transaction/commit-compiled!
              compiled
              {:read-source read-source
               :write-source!
               (fn [path text]
                 (if @attempted
                   (do
                     ;; Recovery is permitted only while this transaction's exact candidate remains.
                     (when-not (and (= :regular (:kind (journal/path-identity path)))
                                    (= (p/sha candidate) (journal/sha256-file path)))
                       (throw (java.io.IOException. "Recovery target changed.")))
                     (file-ops/atomic-write! path text))
                   (do
                     (when-not (and (= identity (journal/path-identity (target! request)))
                                    (= (p/sha source) (journal/sha256-file file)))
                       (p/refuse! :source-changed-before-commit [:guard] "Final source recheck differs."))
                     (reset! attempted true)
                     (file-ops/publish-prepared! path @stage)
                     (hook! :external-after-write))))})]
        (cond
          (:ok outcome)
          (merge {:state "committed" :committed true :mutation_attempted true :source_unchanged false :ok true}
                 (:receipt result) @detail
                 {:write_verified true :read_back_hashes {(:file request) (p/sha candidate)}})
          (not @attempted)
          (merge (p/refusal (ex-info (or (:error outcome) "Source changed before publication.")
                              {:error-type :source-changed-before-commit :at [:guard]
                               :expected (p/sha source) :actual (journal/sha256-file file)})) @detail)
          (:rolled-back outcome)
          (failed "rolled-back" true true :io-error (:error outcome) @detail)
          :else (failed "recovery-required" true nil :commit-outcome-unknown (:error outcome) @detail)))
      (catch Exception e
        (cond
          @attempted (failed "recovery-required" true nil :commit-outcome-unknown (.getMessage e) @detail)
          (= :source-changed-before-commit (:error-type (ex-data e))) (merge (p/refusal e) @detail)
          :else (failed "failed" false true :io-error (.getMessage e) @detail)))
      (finally
        (when @stage (Files/deleteIfExists (.toPath ^java.io.File @stage)))
        (when @seed (Files/deleteIfExists (.toPath ^java.io.File @seed)))))))

(defn bounded-summary [result]
  (if (> (alength (p/bytes (pr-str result))) 3900)
    (dissoc result :inserted_form_ranges)
    result))

(defn execute!
  ([request] (execute! request {}))
  ([request hooks]
   (let [start (System/nanoTime)
         completed (atom nil)
         result (try
                  (p/validate! request)
                  (let [file (target! request)
                        locks (journal/transactions-dir (:workspace_root request))]
                    (file-ops/with-publish-lock*
                      locks
                      (fn []
                        (target! request)
                        (let [source (read-bounded file :source)
                              result (plan source request)]
                          (reset! completed (if (:ok result) (commit-plan! request file source result hooks) result))))))
                  (catch Exception e
                    (cond
                      (:mutation_attempted @completed)
                      (failed "recovery-required" true nil :commit-outcome-unknown (.getMessage e)
                              (select-keys @completed [:receipt_details_path :receipt_hash]))
                      (#{:invalid-request :invalid-path :unsupported-source :limit-exceeded}
                       (:error-type (ex-data e)))
                      (p/refusal e)
                      :else (failed "failed" false true :io-error (.getMessage e) {}))))]
     (bounded-summary (assoc result :elapsed_ms (/ (- (System/nanoTime) start) 1e6))))))

(def leading [:state :committed :mutation_attempted :source_unchanged])
(defn receipt-text [receipt]
  (str "{" (str/join ", " (for [k (concat leading (sort (remove (set leading) (keys receipt))))]
                            (str (pr-str k) " " (pr-str (get receipt k))))) "}\n"))

;; @spec INSERT-FORMS-018
;; INTENT: INSERT-FORMS-018
(defn cli! [opts]
  (let [start (System/nanoTime)]
    (try
      (p/closed! opts [:op :request-file] [] [] :invalid-request)
      (when-not (string? (:request-file opts)) (p/refuse! :invalid-request [:request-file] "Request file required."))
      (let [request (read-request (read-bounded (:request-file opts) :request))]
        (if (:error-type request) (assoc request :elapsed_ms (/ (- (System/nanoTime) start) 1e6)) (execute! request)))
      (catch Exception e
        (assoc (if (:error-type (ex-data e)) (p/refusal e)
                   (failed "failed" false true :io-error (.getMessage e) {}))
               :elapsed_ms (/ (- (System/nanoTime) start) 1e6))))))
