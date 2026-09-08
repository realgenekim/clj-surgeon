(ns clj-surgeon.require-change-io
  "Explicit-file capture and shared failure-atomic publication/proof/inverse."
  (:require
   [clj-surgeon.receipt-artifacts :as artifacts]
   [clj-surgeon.file-ops :as file-ops]
   [clj-surgeon.mcp-extraction :as kernel]
   [clj-surgeon.mcp-paths :as paths]
   [clj-surgeon.require-change :as change]
   [clj-surgeon.synchronous-verification :as proof]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.walk :as walk])
  (:import
   (java.nio.file Files LinkOption)
   (java.util UUID)))

(defn- fail! [type message evidence] (change/refuse! type message evidence))

;; @spec REQUIRE-CHANGE-010
(defn capture! [root files]
  (let [paths (mapv
                (fn [{:keys [file]}]
                  (when-not (and (paths/relative-source-path? file) (str/ends-with? file ".clj"))
                    (fail! :invalid-path "Source must be a relative .clj path" {:file file}))
                  (let [p (.resolve root file)]
                    (when-not (and (= p (.toRealPath p (make-array LinkOption 0)))
                                   (Files/isRegularFile p (into-array LinkOption [LinkOption/NOFOLLOW_LINKS])))
                      (fail! :unconfined-source "Source path traverses a symlink or is not a regular file" {:file file}))
                    (when (> (Files/size p) change/max-file-bytes)
                      (fail! :file-byte-bound "Source exceeds per-file byte bound" {:file file :max_file_bytes change/max-file-bytes}))
                    [file p])) files)]
    (when (> (reduce + 0 (map #(Files/size (second %)) paths)) change/max-bytes)
      (fail! :byte-bound "Explicit source set exceeds total byte bound" {:max_bytes change/max-bytes}))
    (let [captured-bytes (atom 0)]
      (into (sorted-map)
        (map (fn [[file p]]
               (let [bytes (with-open [stream (Files/newInputStream p (make-array java.nio.file.OpenOption 0))]
                             (.readNBytes stream (inc change/max-file-bytes)))]
                 (when (> (alength bytes) change/max-file-bytes)
                   (fail! :file-byte-bound "Source grew beyond byte bound during capture" {:file file}))
                 (when (> (swap! captured-bytes + (alength bytes)) change/max-bytes)
                   (fail! :byte-bound "Source set grew beyond total byte bound during capture" {:max_bytes change/max-bytes}))
                 (let [source (String. bytes java.nio.charset.StandardCharsets/UTF_8)]
                   (when-not (java.util.Arrays/equals bytes (.getBytes source java.nio.charset.StandardCharsets/UTF_8))
                     (fail! :invalid-utf8 "Source must contain valid UTF-8 bytes" {:file file}))
                   [file source])))) paths))))

(defn- read-config [root]
  (let [file (io/file (str root) ".clj-surgeon.edn")]
    (when (.isFile file)
      (when (> (.length file) (* 1024 1024)) (fail! :config-byte-bound "Verification configuration exceeds byte bound" {}))
      (edn/read-string (slurp file)))))

(defn- anchored-profiles [root profiles]
  (into {} (for [[name spec] profiles]
             [name (if-let [capability (proof/profile-capability spec)]
                     (cond-> {:commands (mapv (fn [argv]
                                                (let [exe (first argv)]
                                                  (if (and (str/includes? exe "/") (not (.isAbsolute (io/file exe))))
                                                    (assoc argv 0 (str (.resolve root exe))) argv))) (:commands capability))}
                       (:timeout-ms capability) (assoc :timeout-ms (:timeout-ms capability)))
                     spec)])))

(defn- save! [dir name data]
  (.mkdirs (io/file dir))
  (let [path (str (io/file dir name))] (file-ops/atomic-write! path (pr-str data)) path))

(defn- absolute-compiled [root compiled]
  (let [absolute #(into (sorted-map) (map (fn [[f s]] [(str (.resolve root f)) s])) %)]
    (-> compiled (update :original-sources absolute) (update :future-sources absolute) (update :guard-sources absolute))))

(defn- check-receipt [profile result]
  {:name (str/join " " (:command result)) :profile profile :exit (:exit result)
   :duration_ms (:elapsed_ms result) :status (if (and (:finished? result) (= 0 (:exit result))) "passed" "failed")})

(defn- base-receipt [compiled]
  {:ok true :operation "require_change" :state "planned" :committed false
   :mutation_attempted false :verification_complete false :source_unchanged true
   :counts (:counts compiled) :expected (:expected compiled) :counts_match true
   :files_touched (vec (keys (:future-sources compiled)))
   :decisions (:decisions compiled)
   :alias_histogram (frequencies (map :alias (:decisions compiled)))
   :collisions_resolved (count (filter (comp seq :collisions) (:decisions compiled)))
   :protected_bytes true :symbol_edits 0 :checks [] :proof_pending [] :next_call nil})

(defn- current? [root request expected]
  (try (= expected (capture! root (:files request))) (catch Exception _ false)))

;; @spec REQUIRE-CHANGE-006
;; @spec REQUIRE-CHANGE-011
;; @spec REQUIRE-CHANGE-012
(defn publish! [root request compiled profile capability receipt-dir]
  (when-not (change/validate-candidate request (:guard-sources compiled) (:future-sources compiled))
    (fail! :protected-byte-change "Candidate differs from the authorized require-only line splice" {}))
  (let [base (base-receipt compiled)
        candidate (absolute-compiled root compiled)
        committed (kernel/commit! candidate)]
    (if-not (:ok committed)
      (merge base {:ok false :state (cond (:rolled-back committed) "rolled-back" (:recovery committed) "recovery-required" :else "refused")
                   :error (:error committed) :error_type (name (:error-type committed))
                   :mutation_attempted (boolean (:recovery committed))
                   :source_unchanged (boolean (or (:rolled-back committed) (not (:recovery committed))))
                   :restored (boolean (:rolled-back committed)) :recovery (:recovery committed)})
      (let [id (str (UUID/randomUUID))
            inverse-path (atom nil)
            rollback (fn [error-type message checks]
                       (let [undo (kernel/undo! (:receipt committed))]
                         (assoc base :ok false :state (if (:ok undo) "rolled-back" "recovery-required")
                                :error_type error-type :error message :mutation_attempted true
                                :source_unchanged (boolean (:ok undo)) :restored (boolean (:ok undo))
                                :checks checks :undo_receipt @inverse-path :recovery undo)))]
        (try
          (reset! inverse-path (save! receipt-dir (str id "-undo.edn") (:receipt committed)))
          (let [verification (proof/run-proof! (str root) profile capability)
                checks (mapv #(check-receipt profile %) (:process_evidence verification))
                expected (merge (:guard-sources compiled) (:future-sources compiled))
                current (current? root request expected)
                checks (conj checks {:name "verified-snapshot-guard" :exit (if current 0 1) :status (if current "passed" "failed")})
                details (save! receipt-dir (str id "-details.edn")
                               {:decisions (:decisions compiled) :verification verification :read_back (:verified committed)
                                :counts (:counts compiled) :expected (:expected compiled)})
                complete (and (:ok verification) current
                              (= (count (:commands capability)) (count (:process_evidence verification)))
                              (every? #(= "passed" (:status %)) checks))]
            (if complete
              (assoc (merge base (artifacts/workspace-evidence (str root) (keys (:future-sources compiled)))) :state "committed" :committed true :mutation_attempted true :source_unchanged (empty? (:future-sources compiled))
                     :verification_complete true :checks checks :undo_receipt @inverse-path :details_path details
                     :receipt_hash (:receipt-hash committed)
                     :undo_command ["clj-surgeon" ":op" ":undo-extract!" ":receipt" @inverse-path])
              (assoc (rollback (if current "verification-failed" "snapshot-drift")
                               (if current "Required verification did not pass completely" "The verified source snapshot changed") checks)
                     :details_path details)))
          (catch Throwable error
            (let [result (rollback "publication-failed" (.getMessage error) [])]
              ;; If receipt persistence itself failed, keep the in-memory inverse
              ;; in recovery evidence rather than falsely claiming durable undo.
              (cond-> result (and (nil? @inverse-path) (not (:restored result)))
                (assoc :emergency_undo (:receipt committed))))))))))

;; @spec REQUIRE-CHANGE-012
(defn- bound-receipt! [receipt-dir result]
  (if (<= (count (.getBytes (pr-str result) "UTF-8")) 4096) result
    (let [detail (save! receipt-dir (str (UUID/randomUUID) "-complete-receipt.edn") result)]
      (-> result
          (select-keys [:ok :operation :state :committed :mutation_attempted
                        :source_unchanged :restored :verification_complete :counts
                        :expected :counts_match :protected_bytes :symbol_edits
                        :workspace_status :workspace_clean_except :details_path
                        :elapsed_ms :undo_receipt :receipt_hash :proof_pending :error_type :collisions_resolved])
          (cond-> (<= (count (pr-str (:alias_histogram result))) 512)
            (assoc :alias_histogram (:alias_histogram result)))
          (cond-> (:error result)
            (assoc :error (subs (:error result) 0 (min 256 (count (:error result))))))
          (assoc :receipt_details_path detail :details_elided true
                 :next_call nil)))))

;; @spec REQUIRE-CHANGE-010
;; @spec REQUIRE-CHANGE-013
(defn execute!
  ([request] (execute! {} request))
  ([config request]
   (let [started (System/nanoTime)
         receipt-dir (artifacts/directory "require-change" (:workspace_root (walk/keywordize-keys request)))
         result
         (try
           (let [r (walk/keywordize-keys request)
                 errors (change/validate-request r)]
             (when (seq errors) (fail! :invalid-request "Invalid standalone require_change request" {:blockers errors}))
             (let [root (paths/real-root (:workspace_root r))
                   profiles (anchored-profiles root (or (:verification-profiles config) (:verification-profiles (read-config root))))
                   profile (get-in r [:verification :profile])
                   preflight (when-not (:plan_only r) (proof/verification-preflight profiles profile true))]
               (when preflight (fail! :verification-unavailable "A configured synchronous verification profile is required" {:preflight preflight}))
               (let [sources (capture! root (:files r))
                     compiled (change/compile-change r sources)]
                 (cond
                   (not (:ok compiled)) compiled
                   (:plan_only r) (assoc (base-receipt compiled) :read_complete true)
                   :else (publish! root r compiled profile (proof/profile-capability (get profiles profile)) receipt-dir)))))
           (catch Throwable error
             {:ok false :operation "require_change" :state "refused" :committed false
              :mutation_attempted false :source_unchanged true :verification_complete false
              :error_type (name (or (:error-type (ex-data error)) :require-change-failed))
              :error (.getMessage error) :evidence (dissoc (ex-data error) :error-type)
              :next_call nil}))
         result (assoc result :elapsed_ms (/ (double (- (System/nanoTime) started)) 1000000.0))]
     ;; Persistence failure must not turn a completed mutation into a prewrite refusal.
     (try (bound-receipt! receipt-dir result)
          (catch Throwable error (assoc result :receipt_bound_error (.getMessage error)))))))

;; @spec REQUIRE-CHANGE-001
(defn cli! [opts]
  (when (System/getProperty "babashka.version")
    (when-let [tmpdir (System/getenv "TMPDIR")] (System/setProperty "java.io.tmpdir" tmpdir)))
  (let [request (or (:request opts) (when-let [file (:request-file opts)] (edn/read-string (slurp file))))
        request (cond-> request (:plan-only opts) (assoc :plan_only true))]
    (execute! request)))
