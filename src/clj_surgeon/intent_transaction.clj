(ns clj-surgeon.intent-transaction
  (:require
   [clj-surgeon.file-ops :as file-ops]
   [clj-surgeon.structural-lens :as structural-lens]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [rewrite-clj.node :as node]
   [rewrite-clj.parser :as parser]
   [rewrite-clj.zip :as z])
  (:import
   (java.nio.file CopyOption Files StandardCopyOption)))

(def transaction-version 1)
(def receipt-version 1)

(def ^:private supported-extensions [".clj" ".cljs" ".cljc"])
(def ^:private spec-keys #{:intents :expect})
(def ^:private intent-keys #{:files :from :to :expect-count})
(def ^:private expectation-keys
  #{:intent-count :edit-count :changed-file-count})

(defn- refuse!
  [error-type message & [data]]
  (throw (ex-info message (merge {:error-type error-type} data))))

(defn- lossless-node-fingerprint
  "Return a syntax fingerprint that ignores whitespace but retains comments,
   metadata, reader macros, token spelling, and tree position."
  [form-node]
  (when-not (node/whitespace? form-node)
    [(node/tag form-node)
     (if (node/inner? form-node)
       (vec (keep lossless-node-fingerprint (node/children form-node)))
       (node/string form-node))]))

(defn- parse-one-form
  [source label]
  (when-not (string? source)
    (refuse! :invalid-intent-form
             (str label " must be a source string")
             {:field label :actual source}))
  (try
    (let [root (parser/parse-string-all source)
          forms (->> (node/children root)
                     (remove node/whitespace?)
                     vec)]
      (when-not (and (= 1 (count forms))
                     (not (node/comment? (first forms))))
        (refuse! :invalid-intent-form
                 (str label
                      " must contain exactly one complete form with no detached comments")
                 {:field label :form-count (count forms)}))
      (let [form-node (first forms)]
        {:node form-node
         :source (node/string form-node)
         :fingerprint (lossless-node-fingerprint form-node)}))
    (catch clojure.lang.ExceptionInfo e
      (if (:error-type (ex-data e))
        (throw e)
        (refuse! :invalid-intent-form
                 (str "Invalid " label ": " (.getMessage e))
                 {:field label})))
    (catch Exception e
      (refuse! :invalid-intent-form
               (str "Invalid " label ": " (.getMessage e))
               {:field label}))))

(defn- validate-complete-source!
  [file source error-type]
  (when-not (string? source)
    (refuse! error-type
             (str "Source is missing for " file)
             {:file file}))
  (try
    (parser/parse-string-all source)
    source
    (catch Exception e
      (refuse! error-type
               (str "Invalid source in " file ": " (.getMessage e))
               {:file file}))))

(defn- zipper-locations
  [zloc]
  (take-while (complement z/end?) (iterate z/next zloc)))

(defn- node-size
  [form-node]
  (if (node/whitespace-or-comment? form-node)
    0
    (inc (if (node/inner? form-node)
           (reduce + 0 (map node-size (node/children form-node)))
           0))))

(defn- sibling-index
  [zloc]
  (loop [current zloc
         index 0]
    (if-let [left (z/left current)]
      (recur left (inc index))
      index)))

(defn- location-path
  [zloc]
  (loop [current zloc
         path ()]
    (let [path (conj path (sibling-index current))]
      (if-let [parent (z/up current)]
        (recur parent path)
        (vec path)))))

(defn- supported-file?
  [file]
  (and (string? file)
       (some #(str/ends-with? file %) supported-extensions)))

(defn- normalized-path
  [file]
  (str (.normalize (java.nio.file.Paths/get file (make-array String 0)))))

(defn- validate-files!
  [files intent-index]
  (when-not (and (vector? files) (seq files))
    (refuse! :invalid-files
             "Intent :files must be a non-empty vector"
             {:intent-index intent-index :files files}))
  (doseq [file files]
    (when-not (supported-file? file)
      (refuse! :unsupported-file
               (str "Unsupported Clojure source file: " (pr-str file))
               {:intent-index intent-index :file file})))
  (let [normalized (mapv normalized-path files)]
    (when-not (= (count normalized) (count (distinct normalized)))
      (refuse! :duplicate-file
               "Intent contains duplicate or aliased file paths"
               {:intent-index intent-index :files files})))
  files)

(defn- validate-expect-count!
  [expected intent-index]
  (when-not (and (integer? expected) (pos? expected))
    (refuse! :invalid-expect-count
             "Intent :expect-count must be a positive integer"
             {:intent-index intent-index :expected-count expected}))
  expected)

(defn- validate-intent!
  [intent intent-index]
  (when-not (map? intent)
    (refuse! :invalid-intents
             "Every intent must be a map"
             {:intent-index intent-index :actual intent}))
  (let [unknown (vec (sort (remove intent-keys (keys intent))))]
    (when (seq unknown)
      (refuse! :unknown-intent-arguments
               (str "Unknown intent arguments: " (str/join ", " unknown))
               {:intent-index intent-index :unknown unknown})))
  (let [files (validate-files! (:files intent) intent-index)
        from (parse-one-form (:from intent) ":from")
        to (parse-one-form (:to intent) ":to")
        expected-count (validate-expect-count! (:expect-count intent)
                                               intent-index)]
    (when (= (:fingerprint from) (:fingerprint to))
      (refuse! :no-op-intent
               "Intent :from and :to are losslessly equal"
               {:intent-index intent-index}))
    {:intent-index intent-index
     :files files
     :from from
     :to to
     :expect-count expected-count}))

(defn- validate-aggregate-expectation!
  [expectation]
  (when-not (map? expectation)
    (refuse! :invalid-transaction-expectation
             "Spec :expect must be a map"
             {:expected-fields expectation-keys}))
  (let [unknown (vec (sort (remove expectation-keys (keys expectation))))
        missing (vec (sort (remove #(contains? expectation %) expectation-keys)))]
    (when (seq unknown)
      (refuse! :invalid-transaction-expectation
               (str "Unknown transaction expectation fields: "
                    (str/join ", " unknown))
               {:unknown unknown}))
    (when (seq missing)
      (refuse! :invalid-transaction-expectation
               (str "Missing transaction expectation fields: "
                    (str/join ", " missing))
               {:missing missing})))
  (doseq [[field value] expectation]
    (when-not (and (integer? value) (pos? value))
      (refuse! :invalid-transaction-expectation
               (str field " must be a positive integer")
               {:field field :actual value})))
  expectation)

(defn- validate-spec!
  [spec]
  (when-not (map? spec)
    (refuse! :invalid-transaction-spec "Transaction spec must be a map"))
  (let [unknown (vec (sort (remove spec-keys (keys spec))))]
    (when (seq unknown)
      (refuse! :unknown-transaction-arguments
               (str "Unknown transaction arguments: "
                    (str/join ", " unknown))
               {:unknown unknown})))
  (when-not (and (vector? (:intents spec)) (seq (:intents spec)))
    (refuse! :invalid-intents "Spec :intents must be a non-empty vector"))
  {:intents (mapv validate-intent! (:intents spec) (range))
   :expect (validate-aggregate-expectation! (:expect spec))})

(defn- ordered-scoped-files
  [intents]
  (reduce (fn [result file]
            (if (some #{file} result) result (conj result file)))
          []
          (mapcat :files intents)))

(defn- matching-edits
  [source file {:keys [intent-index from to]}]
  (let [root (z/of-string source {:track-position? true})]
    (->> (zipper-locations root)
         (map-indexed vector)
         (keep (fn [[preorder candidate]]
                 (let [candidate-node (z/node candidate)]
                   (when (= (:fingerprint from)
                            (lossless-node-fingerprint candidate-node))
                     (let [{:keys [row end-row]} (meta candidate-node)
                           size (node-size candidate-node)]
                       {:intent-index intent-index
                        :file file
                        :address {:preorder preorder}
                        :path (location-path candidate)
                        :end-preorder (+ preorder size -1)
                        :line row
                        :end-line end-row
                        :before (z/string candidate)
                        :after (:source to)})))))
         vec)))

(defn- compile-intent-edits
  [sources intent]
  (let [by-file (mapv (fn [file]
                        [file (matching-edits (get sources file) file intent)])
                      (:files intent))
        edits (vec (mapcat second by-file))
        actual-count (count edits)]
    (when-not (= (:expect-count intent) actual-count)
      (refuse! :expect-count-mismatch
               (str "Intent " (:intent-index intent) " expected "
                    (:expect-count intent) " matches, found " actual-count)
               {:intent-index (:intent-index intent)
                :expected-count (:expect-count intent)
                :actual-count actual-count
                :per-file-counts (into {} (map (fn [[file matches]]
                                                 [file (count matches)]))
                                       by-file)}))
    {:intent-index (:intent-index intent)
     :files (:files intent)
     :from (get-in intent [:from :source])
     :to (get-in intent [:to :source])
     :expected-count (:expect-count intent)
     :match-count actual-count
     :per-file-counts (into {} (map (fn [[file matches]]
                                      [file (count matches)]))
                            by-file)
     :edits edits}))

(defn- overlap?
  [left right]
  (<= (:address-preorder right) (:end-preorder left)))

(defn- assert-disjoint-edits!
  [file edits]
  (let [ordered (->> edits
                     (map #(assoc % :address-preorder
                                  (get-in % [:address :preorder])))
                     (sort-by :address-preorder)
                     vec)]
    (doseq [[left right] (partition 2 1 ordered)]
      (when (overlap? left right)
        (refuse! :overlapping-intents
                 (str "Intents overlap in " file)
                 {:file file
                  :intent-indexes [(:intent-index left)
                                   (:intent-index right)]
                  :edits [(dissoc left :address-preorder)
                          (dissoc right :address-preorder)]})))
    (mapv #(dissoc % :address-preorder) ordered)))

(defn- replacement-node
  [source]
  (:node (parse-one-form source ":to")))

(defn- move-right
  [zloc n]
  (nth (iterate z/right zloc) n nil))

(defn- location-at-path
  [source path]
  (when (and (vector? path) (seq path) (every? nat-int? path))
    (let [first-form (z/of-string source {:track-position? true})
          forms-root (z/up first-form)
          [root-index & child-indexes] path]
      (when (and forms-root (zero? root-index))
        (reduce (fn [parent index]
                  (some-> parent z/down (move-right index)))
                forms-root
                child-indexes)))))

(defn- replace-at-address
  [source {:keys [address path before after]}]
  (let [root (z/of-string source {:track-position? true})
        target (if (some? path)
                 (location-at-path source path)
                 (nth (zipper-locations root) (:preorder address) nil))]
    (when-not target
      (refuse! :stale-path
               (str "Planned address no longer exists: " (:preorder address))))
    (when-not (= before (z/string target))
      (refuse! :stale-subform
               "Source at planned address does not match edit"))
    (-> target
        (z/replace (replacement-node after))
        z/root-string)))

(defn- apply-edits
  [source edits]
  (reduce replace-at-address
          source
          (sort-by #(or (get-in % [:address :preorder]) 0) > edits)))

(defn- prefixed-lines
  [prefix source]
  (->> (str/split source #"\n" -1)
       (map #(str prefix %))
       (str/join "\n")))

(defn- edit-diff
  [{:keys [line before after intent-index]}]
  (str "@@ intent " intent-index " line " line " @@\n"
       (prefixed-lines "-" before) "\n"
       (prefixed-lines "+" after) "\n"))

(defn- file-diff
  [file edits]
  (str "--- a/" file "\n"
       "+++ b/" file "\n"
       (apply str (map edit-diff edits))))

(defn- compile-file
  [file source edits]
  (let [ordered-edits (assert-disjoint-edits! file edits)
        result-source (if (seq ordered-edits)
                        (apply-edits source ordered-edits)
                        source)]
    (validate-complete-source! file result-source :invalid-result-source)
    {:file file
     :match-count (count ordered-edits)
     :source-hash (structural-lens/source-hash source)
     :result-hash (structural-lens/source-hash result-source)
     :edits ordered-edits
     :diff (when (seq ordered-edits) (file-diff file ordered-edits))
     :result-source result-source}))

(defn- compile-transaction*
  [sources spec]
  (let [{:keys [intents expect]} (validate-spec! spec)
        files (ordered-scoped-files intents)
        _ (doseq [file files]
            (validate-complete-source! file (get sources file) :invalid-source))
        compiled-intents (mapv #(compile-intent-edits sources %) intents)
        edits-by-file (group-by :file (mapcat :edits compiled-intents))
        compiled-files (mapv #(compile-file % (get sources %) (get edits-by-file % []))
                             files)
        actual {:intent-count (count compiled-intents)
                :edit-count (reduce + (map :match-count compiled-intents))
                :changed-file-count (count (filter (comp pos? :match-count)
                                                   compiled-files))}]
    (when-not (= expect actual)
      (refuse! :transaction-expectation-mismatch
               "Compiled transaction does not match aggregate expectations"
               {:expected expect :actual actual}))
    {:ok true
     :operation :change
     :transaction-version transaction-version
     :intent-count (:intent-count actual)
     :match-count (:edit-count actual)
     :changed-file-count (:changed-file-count actual)
     :intents (mapv #(dissoc % :edits) compiled-intents)
     :files (mapv #(dissoc % :result-source :diff) compiled-files)
     :diff (apply str (keep :diff compiled-files))
     :original-sources (select-keys sources files)
     :future-sources (into {} (map (juxt :file :result-source) compiled-files))
     :validated {:whole-files-parsed true
                 :file-count (count compiled-files)}}))

(defn compile-transaction
  "Compile explicit exact structural intents against an in-memory file map.
   Returns a complete future state or one structured refusal. Performs no I/O."
  [sources spec]
  (try
    (when-not (map? sources)
      (refuse! :invalid-sources "Sources must be a map of file path to source"))
    (compile-transaction* sources spec)
    (catch clojure.lang.ExceptionInfo e
      (merge {:error (.getMessage e)} (ex-data e)))
    (catch Exception e
      {:error (.getMessage e)
       :error-type :intent-compiler-failure})))

(defn- spec-files
  [spec]
  (->> (:intents spec)
       (mapcat :files)
       distinct
       vec))

(defn- canonical-file
  [file]
  (try
    (.getCanonicalPath (java.io.File. file))
    (catch Exception e
      (refuse! :invalid-files
               (str "Cannot canonicalize source path " (pr-str file)
                    ": " (.getMessage e))
               {:file file}))))

(defn- canonicalize-spec
  [spec]
  (update spec :intents
          (fn [intents]
            (when intents
              (mapv #(update % :files
                             (fn [files]
                               (when files (mapv canonical-file files))))
                    intents)))))

(defn- read-sources
  [files]
  (reduce (fn [sources file]
            (try
              (assoc sources file (slurp file))
              (catch Exception e
                (refuse! :invalid-source
                         (str "Cannot read source " file ": " (.getMessage e))
                         {:file file}))))
          {}
          files))

(defn- public-plan
  [compiled]
  (-> compiled
      (dissoc :original-sources :future-sources)
      (update :files
              (fn [files]
                (mapv #(update % :edits
                               (fn [edits]
                                 (mapv (fn [edit]
                                         (select-keys edit
                                                      [:intent-index :address
                                                       :line :end-line]))
                                       edits)))
                      files)))))

(defn plan-change
  "Read the explicit files in :spec and compile one non-mutating transaction
   plan. The public result retains concrete edits and hashes but omits complete
   future-file source."
  [{:keys [spec] :as opts}]
  (try
    (let [unknown (vec (sort (remove #{:op :spec} (keys opts))))]
      (when (seq unknown)
        (refuse! :unknown-arguments
                 (str "Unknown :change arguments: " (str/join ", " unknown))
                 {:unknown unknown})))
    (when-not (map? spec)
      (refuse! :invalid-transaction-spec ":spec must be an EDN map"))
    ;; Reject malformed proposal data before touching the filesystem. Compile
    ;; validates again after canonicalization so aliased paths cannot evade the
    ;; exact same contract.
    (validate-spec! spec)
    (let [canonical-spec (canonicalize-spec spec)
          sources (read-sources (spec-files canonical-spec))]
      (public-plan (compile-transaction sources canonical-spec)))
    (catch clojure.lang.ExceptionInfo e
      (merge {:error (.getMessage e)} (ex-data e)))
    (catch Exception e
      {:error (.getMessage e)
       :error-type :intent-compiler-failure})))

(defn- changed-file-plans
  [compiled]
  (filterv (comp pos? :match-count) (:files compiled)))

(defn- read-source!
  [read-source file]
  (try
    (let [source (read-source file)]
      (when-not (string? source)
        (refuse! :source-read-failed
                 (str "Source reader did not return text for " file)
                 {:file file}))
      source)
    (catch clojure.lang.ExceptionInfo e
      (throw e))
    (catch Exception e
      (refuse! :source-read-failed
               (str "Cannot read source " file ": " (.getMessage e))
               {:file file}))))

(defn- assert-file-hash!
  [read-source file expected error-type]
  (let [source (read-source! read-source file)
        actual (structural-lens/source-hash source)]
    (when-not (= expected actual)
      (refuse! error-type
               (str "Source hash mismatch for " file)
               {:file file :expected-hash expected :actual-hash actual}))
    source))

(defn- recovery-result
  [read-source write-source! originals
   {:keys [file source-hash result-hash]}]
  (try
    (let [current (read-source! read-source file)
          current-hash (structural-lens/source-hash current)]
      (cond
        (= source-hash current-hash)
        {:file file :status :original :source-hash current-hash}

        (= result-hash current-hash)
        (try
          (write-source! file (get originals file))
          (let [restored (read-source! read-source file)
                restored-hash (structural-lens/source-hash restored)]
            (if (= source-hash restored-hash)
              {:file file :status :restored :source-hash restored-hash}
              {:file file :status :restore-hash-mismatch
               :expected-hash source-hash :actual-hash restored-hash}))
          (catch Exception e
            {:file file :status :restore-failed :error (.getMessage e)}))

        :else
        {:file file :status :unexpected-source
         :original-hash source-hash
         :result-hash result-hash
         :actual-hash current-hash}))
    (catch Exception e
      {:file file :status :recovery-read-failed :error (.getMessage e)})))

(defn- recover-transaction!
  [read-source write-source! originals file-plans]
  (mapv #(recovery-result read-source write-source! originals %)
        (reverse file-plans)))

(defn- recovered?
  [recovery]
  (every? #(#{:original :restored} (:status %)) recovery))

(defn- execute-writes!
  [read-source write-source! futures file-plans]
  (doseq [{:keys [file source-hash result-hash]} file-plans]
    ;; Recheck immediately before each replacement. If a later file goes stale
    ;; after an earlier write, the caller enters the same recovery protocol.
    (assert-file-hash! read-source file source-hash :source-hash-mismatch)
    (write-source! file (get futures file))
    (assert-file-hash! read-source file result-hash :read-back-hash-mismatch)))

(defn- verified-hashes
  [read-source file-plans]
  (into {}
        (map (fn [{:keys [file result-hash]}]
               (assert-file-hash! read-source file result-hash
                                  :read-back-hash-mismatch)
               [file result-hash]))
        file-plans))

(defn commit-compiled!
  "Commit a successfully compiled transaction through injected source I/O.
   Ordinary handled failures restore files that still equal either the original
   or transaction result. Unexpected bytes are never overwritten."
  ([compiled]
   (commit-compiled! compiled
                     {:read-source slurp
                      :write-source! file-ops/atomic-write!}))
  ([compiled {:keys [read-source write-source!]}]
   (try
     (when-not (and (:ok compiled)
                    (map? (:original-sources compiled))
                    (map? (:future-sources compiled))
                    (ifn? read-source)
                    (ifn? write-source!))
       (refuse! :invalid-compiled-transaction
                "Commit requires one complete compiled transaction and source I/O"))
     (let [file-plans (changed-file-plans compiled)
           originals (:original-sources compiled)
           futures (:future-sources compiled)]
       ;; The all-file preflight is outside the recovery block because it has
       ;; not written anything.
       (doseq [{:keys [file source-hash]} file-plans]
         (assert-file-hash! read-source file source-hash
                            :source-hash-mismatch))
       (try
         (execute-writes! read-source write-source! futures file-plans)
         (let [hashes (verified-hashes read-source file-plans)]
           {:ok true
            :operation :change!
            :transaction-version transaction-version
            :committed true
            :changed-file-count (count file-plans)
            :verified {:whole-files true
                       :file-count (count file-plans)
                       :read-back-hashes hashes}})
         (catch Exception cause
           (let [recovery (recover-transaction!
                            read-source write-source! originals file-plans)
                 rolled-back? (recovered? recovery)
                 cause-data (ex-data cause)]
             (merge
               {:error (if rolled-back?
                         "Transaction write failed; all files restored"
                         "Transaction write failed; manual recovery required")
                :error-type (if rolled-back?
                              :transaction-write-failed
                              :transaction-recovery-required)
                :cause-error (.getMessage cause)
                :cause-error-type (or (:error-type cause-data)
                                      :transaction-write-exception)
                :rolled-back rolled-back?
                :recovery recovery}
               (select-keys cause-data
                            [:file :expected-hash :actual-hash]))))))
     (catch clojure.lang.ExceptionInfo e
       (merge {:error (.getMessage e)} (ex-data e)))
     (catch Exception e
       {:error (.getMessage e)
        :error-type :transaction-write-exception}))))

(defn- reverse-edit
  [{:keys [intent-index address path line end-line before after]}]
  {:intent-index intent-index
   :address address
   :path path
   :line line
   :end-line end-line
   :before after
   :after before})

(defn- receipt-hash
  [receipt]
  (structural-lens/source-hash (pr-str (dissoc receipt :receipt-hash))))

(defn build-receipt
  "Build the durable forward evidence and concrete inverse edits for one
   compiled transaction. Full original and future files are intentionally not
   embedded."
  [compiled]
  (let [files (->> (changed-file-plans compiled)
                   (mapv (fn [{:keys [file source-hash result-hash edits]}]
                           {:file file
                            :source-hash source-hash
                            :result-hash result-hash
                            :inverse-edits (mapv reverse-edit edits)})))
        receipt {:receipt-version receipt-version
                 :transaction-version transaction-version
                 :operation :change!
                 :intent-count (:intent-count compiled)
                 :match-count (:match-count compiled)
                 :changed-file-count (:changed-file-count compiled)
                 :files files
                 :intents (:intents compiled)
                 :diff (:diff compiled)
                 :inverse {:operation :undo-change!
                           :guarded-file-count (count files)}}]
    (assoc receipt :receipt-hash (receipt-hash receipt))))

(defn- invalid-receipt!
  [message & [data]]
  (refuse! :invalid-transaction-receipt message data))

(defn- validate-receipt!
  [receipt]
  (when-not (map? receipt)
    (invalid-receipt! "Transaction receipt must be an EDN map"))
  (when-not (= receipt-version (:receipt-version receipt))
    (invalid-receipt!
      (str "Unsupported receipt version: " (pr-str (:receipt-version receipt)))
      {:supported-receipt-version receipt-version}))
  (when-not (= transaction-version (:transaction-version receipt))
    (invalid-receipt!
      (str "Unsupported transaction version: "
           (pr-str (:transaction-version receipt)))
      {:supported-transaction-version transaction-version}))
  (when-not (= :change! (:operation receipt))
    (invalid-receipt! "Receipt operation must be :change!"))
  (when-not (and (vector? (:files receipt)) (seq (:files receipt)))
    (invalid-receipt! "Receipt :files must be a non-empty vector"))
  (when-not (= (:receipt-hash receipt) (receipt-hash receipt))
    (invalid-receipt! "Receipt hash does not match its contents"
                      {:expected-hash (:receipt-hash receipt)
                       :actual-hash (receipt-hash receipt)}))
  (let [files (mapv :file (:files receipt))]
    (when-not (and (every? string? files)
                   (= (count files) (count (distinct files))))
      (invalid-receipt! "Receipt file paths must be distinct strings")))
  (when-not (= (:changed-file-count receipt) (count (:files receipt)))
    (invalid-receipt! "Receipt changed-file count does not match its files"))
  (when-not (= (:intent-count receipt) (count (:intents receipt)))
    (invalid-receipt! "Receipt intent count does not match its intents"))
  (when-not (= (:match-count receipt)
               (reduce + 0 (map #(count (:inverse-edits %))
                                (:files receipt))))
    (invalid-receipt! "Receipt match count does not match its inverse edits"))
  (when-not (= {:operation :undo-change!
                :guarded-file-count (count (:files receipt))}
               (:inverse receipt))
    (invalid-receipt! "Receipt inverse summary does not match its files"))
  (doseq [{:keys [file source-hash result-hash inverse-edits]} (:files receipt)]
    (when-not (and (string? source-hash)
                   (string? result-hash)
                   (vector? inverse-edits)
                   (seq inverse-edits))
      (invalid-receipt! "Receipt file entry is incomplete" {:file file}))
    (doseq [{:keys [path before after]} inverse-edits]
      (when-not (and (vector? path) (seq path) (every? nat-int? path)
                     (string? before) (string? after))
        (invalid-receipt! "Receipt inverse edit is incomplete" {:file file}))))
  receipt)

(defn compile-inverse
  "Compile a receipt's concrete reverse edits against current in-memory source.
   Every current file must match its forward result hash, and every reconstructed
   file must match its original hash."
  [receipt sources]
  (try
    (validate-receipt! receipt)
    (when-not (map? sources)
      (invalid-receipt! "Inverse sources must be a file-to-source map"))
    (let [compiled-files
          (mapv
            (fn [{:keys [file source-hash result-hash inverse-edits]}]
              (let [current (get sources file)]
                (validate-complete-source! file current :invalid-source)
                (let [actual-hash (structural-lens/source-hash current)]
                  (when-not (= result-hash actual-hash)
                    (refuse! :result-hash-mismatch
                             (str "Current source does not match transaction result: "
                                  file)
                             {:file file :expected-hash result-hash
                              :actual-hash actual-hash})))
                (let [restored
                      (try
                        (apply-edits current inverse-edits)
                        (catch clojure.lang.ExceptionInfo e
                          (invalid-receipt!
                            (str "Inverse edit does not match its recorded path: "
                                 file)
                            {:file file
                             :cause-error-type (:error-type (ex-data e))})))
                      _ (validate-complete-source!
                          file restored :invalid-result-source)
                      restored-hash (structural-lens/source-hash restored)]
                  (when-not (= source-hash restored-hash)
                    (invalid-receipt!
                      (str "Inverse result hash does not match original: " file)
                      {:file file :expected-hash source-hash
                       :actual-hash restored-hash}))
                  {:file file
                   :match-count (count inverse-edits)
                   :source-hash result-hash
                   :result-hash source-hash
                   :edits inverse-edits
                   :result-source restored})))
            (:files receipt))
          files (mapv :file compiled-files)]
      {:ok true
       :operation :undo-change!
       :transaction-version transaction-version
       :intent-count (:intent-count receipt)
       :match-count (:match-count receipt)
       :changed-file-count (count files)
       :files (mapv #(dissoc % :result-source) compiled-files)
       :original-sources (select-keys sources files)
       :future-sources (into {} (map (juxt :file :result-source) compiled-files))
       :validated {:whole-files-parsed true :file-count (count files)}})
    (catch clojure.lang.ExceptionInfo e
      (merge {:error (.getMessage e)} (ex-data e)))
    (catch Exception e
      {:error (.getMessage e) :error-type :invalid-transaction-receipt})))

(defn- valid-edn-path?
  [path]
  (and (string? path) (str/ends-with? path ".edn")))

(defn- canonical-receipt-path
  [path]
  (when-not (valid-edn-path? path)
    (refuse! :invalid-receipt-path
             ":receipt-out and :receipt must name an .edn file"
             {:path path}))
  (canonical-file path))

(defn- assert-receipt-does-not-alias-source!
  [receipt-path spec]
  (when (some #{receipt-path} (spec-files spec))
    (refuse! :invalid-receipt-path
             "Receipt path must not alias a source file"
             {:path receipt-path})))

(defn- receipt-source
  [receipt]
  (str (pr-str receipt) "\n"))

(defn- stage-receipt!
  [receipt-path receipt]
  (let [target (io/file receipt-path)
        parent (.getParentFile (.getAbsoluteFile target))]
    (when-not (and parent (.exists parent) (.isDirectory parent))
      (refuse! :invalid-receipt-path
               "Receipt parent directory does not exist"
               {:path receipt-path}))
    (let [staged (java.io.File/createTempFile
                   ".clj-surgeon-receipt-" ".edn" parent)]
      (try
        (spit staged (receipt-source receipt))
        (validate-receipt! (edn/read-string (slurp staged)))
        staged
        (catch Exception e
          (.delete staged)
          (throw e))))))

(defn- publish-staged-receipt!
  [staged receipt-path]
  (Files/move (.toPath staged)
              (.toPath (io/file receipt-path))
              (into-array CopyOption
                          [StandardCopyOption/ATOMIC_MOVE
                           StandardCopyOption/REPLACE_EXISTING])))

(defn- compile-change-spec
  [spec]
  (validate-spec! spec)
  (let [canonical-spec (canonicalize-spec spec)
        sources (read-sources (spec-files canonical-spec))]
    {:spec canonical-spec
     :compiled (compile-transaction sources canonical-spec)}))

(defn execute-change!
  "Compile, commit, verify, and publish one durable inverse receipt."
  [{:keys [spec receipt-out] :as opts}]
  (try
    (let [unknown (vec (sort (remove #{:op :spec :receipt-out} (keys opts))))]
      (when (seq unknown)
        (refuse! :unknown-arguments
                 (str "Unknown :change! arguments: " (str/join ", " unknown))
                 {:unknown unknown})))
    (when-not (map? spec)
      (refuse! :invalid-transaction-spec ":spec must be an EDN map"))
    (let [receipt-path (canonical-receipt-path receipt-out)
          {:keys [spec compiled]} (compile-change-spec spec)]
      (assert-receipt-does-not-alias-source! receipt-path spec)
      (if (:error compiled)
        compiled
        (let [receipt (build-receipt compiled)
              staged (stage-receipt! receipt-path receipt)]
          (try
            (let [commit (commit-compiled! compiled)]
              (if (:error commit)
                commit
                (try
                  (publish-staged-receipt! staged receipt-path)
                  (let [published (edn/read-string (slurp receipt-path))]
                    (validate-receipt! published)
                    (merge commit
                           {:receipt-file receipt-path
                            :receipt-hash (:receipt-hash receipt)
                            :intent-count (:intent-count compiled)
                            :match-count (:match-count compiled)
                            :inverse (:inverse receipt)}))
                  (catch Exception publish-error
                    (let [inverse (compile-inverse
                                    receipt (:future-sources compiled))
                          rollback (if (:ok inverse)
                                     (commit-compiled! inverse)
                                     inverse)]
                      {:error (if (:ok rollback)
                                "Receipt publication failed; all files restored"
                                "Receipt publication failed; manual recovery required")
                       :error-type (if (:ok rollback)
                                     :receipt-write-failed
                                     :transaction-recovery-required)
                       :cause-error (.getMessage publish-error)
                       :rolled-back (boolean (:ok rollback))
                       :recovery rollback})))))
            (finally
              (when (.exists staged) (.delete staged)))))))
    (catch clojure.lang.ExceptionInfo e
      (merge {:error (.getMessage e)} (ex-data e)))
    (catch Exception e
      {:error (.getMessage e) :error-type :transaction-write-exception})))

(defn execute-undo!
  "Apply the hash-fenced inverse from a durable :change! receipt."
  [{:keys [receipt] :as opts}]
  (try
    (let [unknown (vec (sort (remove #{:op :receipt} (keys opts))))]
      (when (seq unknown)
        (refuse! :unknown-arguments
                 (str "Unknown :undo-change! arguments: "
                      (str/join ", " unknown))
                 {:unknown unknown})))
    (let [receipt-path (canonical-receipt-path receipt)
          saved (try
                  (edn/read-string (slurp receipt-path))
                  (catch Exception e
                    (invalid-receipt!
                      (str "Cannot read transaction receipt: " (.getMessage e))
                      {:receipt receipt-path})))
          _ (validate-receipt! saved)
          files (mapv :file (:files saved))
          sources (read-sources files)
          inverse (compile-inverse saved sources)]
      (if (:error inverse)
        inverse
        (let [commit (commit-compiled! inverse)]
          (if (:error commit)
            commit
            (-> commit
                (assoc :operation :undo-change!
                       :receipt-file receipt-path
                       :receipt-hash (:receipt-hash saved)
                       :restored-original-hashes
                       (into {} (map (juxt :file :result-hash)
                                     (:files inverse)))))))))
    (catch clojure.lang.ExceptionInfo e
      (merge {:error (.getMessage e)} (ex-data e)))
    (catch Exception e
      {:error (.getMessage e) :error-type :invalid-transaction-receipt})))
