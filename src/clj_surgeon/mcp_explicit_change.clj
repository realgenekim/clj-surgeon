(ns clj-surgeon.mcp-explicit-change
  (:require
   [clj-surgeon.intent-transaction :as transaction]
   [clj-surgeon.mcp-change-buffer :as change-buffer]
   [clj-surgeon.mcp-exact-verify :as exact-verify]
   [clj-surgeon.mcp-cold-verify :as cold-verify]
   [clj-surgeon.mcp-compact-location :as compact-location]
   [clj-surgeon.mcp-compact-relations :as compact-relations]
   [clj-surgeon.mcp-contract :as contract]
   [clj-surgeon.mcp-program-tool :as program-tool]
   [clojure.java.io :as io]))

(defn- compiled-addressed-edits
  [compiled]
  (mapcat :edits (:files compiled)))

(defn- resolve-spec-from-path-map [spec path-map]
  (try
    {:ok true
     :spec
     (update spec :changes
             (fn [changes]
               (mapv (fn [change]
                       (update change :in
                               (fn [paths]
                                 (mapv (fn [path]
                                         (or (get path-map path)
                                             (throw
                                               (ex-info
                                                 "Final relation path widened beyond the captured universe"
                                                 {:path path}))))
                                       paths))))
                     changes)))}
    (catch clojure.lang.ExceptionInfo error
      {:error (.getMessage error)
       :error-type :compact-relation-path-conflict
       :failed-stage :path-resolution
       :path (:path (ex-data error))
       :mutation-attempted false
       :write-authority false
       :source-unchanged true
       :next-action "correct_request"})))

(defn- merge-programs-into-compiled
  [compiled programs]
  (let [sources
        (reduce
          (fn [current {:keys [file]}]
            (if (contains? current file)
              current
              (assoc current file (slurp file))))
          (:original-sources compiled)
          programs)
        program-result (program-tool/compile-programs sources programs)]
    (if-not (:ok program-result)
      program-result
      (let [raw-edits (concat (compiled-addressed-edits compiled)
                              (compiled-addressed-edits
                                (:compiled program-result)))
            edits (mapv (fn [index edit]
                          (-> edit
                              (assoc :id (str "hybrid/" (inc index)))
                              (dissoc :intent-index)))
                        (range) raw-edits)
            combined (transaction/compile-addressed-transaction
                       sources edits)]
        (if-not (:ok combined)
          (assoc combined :ok false :source-unchanged true)
          (assoc combined
                 :program-count (:program-count program-result)
                 :program-edit-count (:edit-count program-result)
                 :program-changed-characters
                 (:changed-characters program-result)))))))

(defn- prepare-relation-spec
  [root sources relation-plan path-map]
  (let [raw-sources
        (into {}
              (map (fn [[raw canonical]] [raw (get sources canonical)]))
              path-map)
        frozen (compact-relations/compile-frozen raw-sources relation-plan)]
    (if-not (:ok frozen)
      frozen
      (let [validated (contract/validate-tool-params (:request frozen))]
        (if-not (:ok validated)
          validated
          (let [resolved
                (resolve-spec-from-path-map
                  (contract/tool-params->transaction (:params validated))
                  path-map)]
            (if-not (:ok resolved)
              resolved
              (let [canonical-files
                    (mapv (fn [raw]
                            (str (.relativize
                                   root
                                   (.toPath (io/file (get path-map raw))))))
                          (:relation-files relation-plan))
                    prepared
                    (compact-location/normalize-spec
                      sources (:spec resolved)
                      (:compact-location-normalization validated))]
                (cond-> prepared
                  (not (:error prepared))
                  (assoc :compact-relation-normalization
                         (assoc (:relation-normalization frozen)
                                :files canonical-files)))))))))))

(defn execute-explicit-change!
  ;; @spec OP-ALG-MCP-001
  ;; @spec MCP-OP-EDIT-030
  [config root resolved receipt verify compact-location-plan relation-plan
   compact-effect-identity? public-operation]
  (let [exact-profile (when (= "exact" verify)
                        (exact-verify/compile-exact-profile
                          verify (:verification-profiles config)
                          (:verification-profile-source config)))
        files (->> (get-in resolved [:spec :changes])
                   (mapcat :in)
                   distinct
                   vec)
        project-root (.toString root)
        baseline (when (and verify (nil? exact-profile))
                   (cond
                     (:capture-verification-baseline! config)
                     ((:capture-verification-baseline! config)
                      project-root verify (:verification-profiles config) files)

                     (nil? (:verify! config))
                     (change-buffer/capture-verification-baseline!
                       project-root verify (:verification-profiles config) files)))
        baseline-refusal? (or (and exact-profile (not (:ok exact-profile)))
                              (and baseline (not (:ok baseline))))]
    (if baseline-refusal?
      {:error (if exact-profile
                "Exact project verification profile is unavailable"
                "Verification baseline capture failed before the direct transaction")
       :error-type (or (:error-type exact-profile)
                       (:error-type baseline)
                       :verification-baseline-failed)
       :verification (or exact-profile baseline)
       :source-unchanged true}
      (let [base-prepare! (:prepare-compiled! config)
            programs (:programs resolved)
            relation-evidence (atom nil)
            base-prepare-compiled!
            (cond
              (seq programs)
              (fn [project-root compiled]
                (let [with-programs
                      (merge-programs-into-compiled compiled programs)]
                  (if (and (:ok with-programs) base-prepare!)
                    (base-prepare! project-root with-programs)
                    with-programs)))

              :else base-prepare!)
            prepare-compiled!
            (if compact-effect-identity?
              (fn [project-root compiled]
                (let [prepared (if base-prepare-compiled!
                                 (base-prepare-compiled! project-root compiled)
                                 compiled)]
                  (if (:error prepared)
                    prepared
                    (assoc prepared
                           :canonical-effect-identity
                           (transaction/canonical-effect-identity
                             project-root prepared)))))
              base-prepare-compiled!)
            relation-prepare
            (when relation-plan
              (fn [sources _spec]
                (let [prepared
                      (prepare-relation-spec
                        root sources relation-plan
                        (:relation-path-map resolved))]
                  (when-let [evidence (:compact-relation-normalization prepared)]
                    (reset! relation-evidence evidence))
                  prepared)))
            result (transaction/execute-mcp-change!
                     (cond-> {:spec (:spec resolved)
                              :receipt-out receipt
                              :write-refusal-context
                              {:operation public-operation
                               :project-root project-root}}
                       prepare-compiled!
                       (assoc :prepare-compiled!
                              #(prepare-compiled! project-root %))

                       relation-prepare
                       (assoc :prepare-spec relation-prepare)

                       (and compact-location-plan (nil? relation-prepare))
                       (assoc :prepare-spec
                              #(compact-location/normalize-spec %1 %2 compact-location-plan))))
            result (cond-> result
                     @relation-evidence
                     (assoc :compact-relation-normalization @relation-evidence))]
        (if (or (:error result) (nil? verify))
          result
          (let [verification (cond
                               exact-profile
                               (exact-verify/run-exact-verification!
                                 project-root exact-profile)

                               (:verify! config)
                               ((:verify! config) project-root verify
                                                  (:verification-profiles config) files)

                               :else
                               (change-buffer/run-verification!
                                 project-root verify
                                 (:verification-profiles config) files baseline))]
            (if (:ok verification)
              (do
                (cold-verify/attach-undo-from-verification!
                  project-root verification (:receipt-file result) (:receipt-hash result))
                (assoc result :verification verification))
              (let [rollback (transaction/execute-undo!
                               {:receipt (:receipt-file result)})
                    rolled-back? (boolean (:ok rollback))
                    hot-rollback (when rolled-back?
                                   (change-buffer/reload-after-rollback!
                                     project-root verify
                                     (:verification-profiles config)))]
                (when rolled-back?
                  (.delete (io/file (:receipt-file result))))
                {:error (if (= :verification-unverified
                               (:error-type verification))
                          "Verification authority was unverified; the direct transaction was rolled back"
                          "Verification failed; the direct transaction was rolled back")
                 :error-type (or (:error-type verification)
                                 :verification-failed)
                 :verification verification
                 :rolled-back rolled-back?
                 :hot-rollback hot-rollback
                 :recovery rollback
                 :source-unchanged rolled-back?}))))))))
