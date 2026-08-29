(ns namespace-tolerance-replay
  "Pure offline replay of injective owner-scope tolerance laws.

   This experiment does not extend the public MCP schema or product compiler.
   It lowers retained JSON-shaped calls, then sends the result through today's
   real validator and intent compiler against frozen source bytes."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.intent-transaction :as transaction]
   [clj-surgeon.mcp-compact-location :as compact-location]
   [clj-surgeon.mcp-contract :as contract]
   [clj-surgeon.outline :as outline]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [owner-aware-symbol-migration :as migration]
   [rewrite-clj.node :as node]
   [rewrite-clj.parser :as parser]))

(def capture-schema "clj-surgeon.owner-aware-call-capture.v1")
(def default-capture-root
  "dev/experiments/namespace_tolerance_retained_corpus")
(def namespace-clause-kinds
  #{:refer-clojure :require :require-macros :use :use-macros
    :import :load :gen-class})

(defn sha256
  [value]
  (let [digest (java.security.MessageDigest/getInstance "SHA-256")]
    (.update digest (.getBytes (str value) "UTF-8"))
    (format "%064x" (java.math.BigInteger. 1 (.digest digest)))))

(defn utf8-byte-count
  [value]
  (alength (.getBytes (str value) "UTF-8")))

(defn meaningful-children
  [form-node]
  (->> (node/children form-node)
       (remove node/whitespace?)
       vec))

(defn parse-one-node
  [source]
  (when (string? source)
    (try
      (let [forms (-> (parser/parse-string-all source)
                      meaningful-children)]
        (when (and (= 1 (count forms))
                   (not (node/comment? (first forms))))
          (first forms)))
      (catch Exception _ nil))))

(defn lossless-fingerprint
  [form-node]
  (when-not (node/whitespace? form-node)
    [(node/tag form-node)
     (if (node/inner? form-node)
       (vec (keep lossless-fingerprint (node/children form-node)))
       (node/string form-node))]))

(defn descendant-nodes
  [form-node]
  (tree-seq node/inner? node/children form-node))

(defn direct-form-nodes
  [source]
  (try
    (->> (parser/parse-string-all source)
         meaningful-children
         (remove node/comment?)
         vec)
    (catch Exception _ [])))

(defn list-head
  [form-node]
  (when (= :list (node/tag form-node))
    (try
      (first (node/sexpr form-node))
      (catch Exception _ nil))))

(defn namespace-evidence
  [file source]
  (let [records (outline/top-level-form-records file source)
        walked-namespaces (filterv #(= 'ns (:type %)) records)
        direct-nodes (direct-form-nodes source)
        direct-namespaces (filterv #(= 'ns (list-head %)) direct-nodes)
        direct-namespace (first direct-namespaces)
        namespace-name
        (when (and (= 1 (count walked-namespaces))
                   (= 1 (count direct-namespaces)))
          (try
            (let [candidate (second (node/sexpr direct-namespace))]
              (when (symbol? candidate) candidate))
            (catch Exception _ nil)))]
    {:records records
     :walked-namespace-count (count walked-namespaces)
     :direct-namespace-count (count direct-namespaces)
     :namespace-node direct-namespace
     :namespace-name namespace-name
     :unique-direct-namespace?
     (and (= 1 (count walked-namespaces))
          (= 1 (count direct-namespaces))
          (symbol? namespace-name))}))

(defn exact-file-selector
  [edit]
  (let [file? (contains? edit "file")
        files? (contains? edit "files")]
    (cond
      (= file? files?) nil
      file?
      (let [file (get edit "file")]
        (when (and (string? file) (not (str/blank? file)))
          {:file file :shape :file}))
      :else
      (let [files (get edit "files")]
        (when (and (vector? files)
                   (= 1 (count files))
                   (string? (first files))
                   (not (str/blank? (first files))))
          {:file (first files) :shape :singleton-files})))))

(defn singular-file
  [edit {:keys [file shape]}]
  (if (= :singleton-files shape)
    (-> edit (dissoc "files") (assoc "file" file))
    edit))

(defn lower-law-a
  "Lower an exact namespace name misplaced in within.form.

   A named owner with the same name, an indirect/reader-conditional namespace,
   or more than one parsed namespace makes this law inapplicable."
  [sources edit]
  (let [within (get edit "within")
        selector (exact-file-selector edit)
        requested (when (= #{"form"} (set (keys within)))
                    (get within "form"))
        source (get sources (:file selector))]
    (when (and (= :file (:shape selector))
               (string? requested)
               (not (str/blank? requested))
               (string? source))
      (let [{:keys [records namespace-name unique-direct-namespace?]}
            (namespace-evidence (:file selector) source)
            competing (filterv #(= (symbol requested) (:name %)) records)]
        (when (and unique-direct-namespace?
                   (= (symbol requested) namespace-name)
                   (empty? competing))
          (assoc edit "within" {"namespace" requested}))))))

(defn namespace-clause-kind
  [source]
  (when-let [form-node (parse-one-node source)]
    (when (= :list (node/tag form-node))
      (let [head (list-head form-node)]
        (when (contains? namespace-clause-kinds head)
          head)))))

(defn fingerprint-count
  [root target]
  (let [target-fingerprint (lossless-fingerprint target)]
    (->> (descendant-nodes root)
         (filter #(= target-fingerprint (lossless-fingerprint %)))
         count)))

(defn direct-fingerprint-count
  [root target]
  (let [target-fingerprint (lossless-fingerprint target)]
    (->> (meaningful-children root)
         (filter #(= target-fingerprint (lossless-fingerprint %)))
         count)))

(defn source-fingerprint-count
  [source target]
  (try
    (fingerprint-count (parser/parse-string-all source) target)
    (catch Exception _ 0)))

(defn lower-law-b
  "Infer the unique namespace owner for matching complete namespace clauses.

   Every equal lossless fingerprint must be a direct namespace child; the
   declared count must equal that direct-child count, with no equal candidate
   nested under the namespace or anywhere else in the file.

   A singleton files array is losslessly emitted as file only inside the full
   proof. Other file-selector shapes remain untouched for the strict current
   validator to refuse."
  [sources edit]
  (when-not (contains? edit "within")
    (let [selector (exact-file-selector edit)
          file (:file selector)
          source (get sources file)
          from-node (parse-one-node (get edit "from"))
          from-kind (namespace-clause-kind (get edit "from"))
          to-kind (namespace-clause-kind (get edit "to"))
          matches (get edit "matches")]
      (when (and selector
                 (string? source)
                 from-node
                 from-kind
                 (= from-kind to-kind)
                 (integer? matches)
                 (pos? matches))
        (let [{:keys [namespace-node unique-direct-namespace?]}
              (namespace-evidence file source)
              direct-count (when unique-direct-namespace?
                             (direct-fingerprint-count namespace-node from-node))
              namespace-count (when unique-direct-namespace?
                                (fingerprint-count namespace-node from-node))
              source-count (source-fingerprint-count source from-node)]
          (when (and unique-direct-namespace?
                     (= matches direct-count)
                     (= direct-count namespace-count)
                     (= namespace-count source-count))
            (-> (singular-file edit selector)
                (assoc "within" {"namespace" true}))))))))

(defn named-owner-evidence
  [file source]
  (when-let [form-node (parse-one-node source)]
    (let [records (outline/top-level-form-records file source)]
      (when (and (= 1 (count records))
                 (= 1 (count (direct-form-nodes source))))
        (let [record (first records)]
          (when (and (:name record)
                     (= (list-head form-node) (:type record)))
            {:node form-node
             :kind (:type record)
             :name (:name record)}))))))

(defn lower-law-c
  "Optional law: infer within.form from a complete, unique named owner rewrite.

   The before and after forms must preserve owner kind and name. The before
   fingerprint must occur exactly once as a complete direct top-level form."
  [sources edit]
  (when-not (contains? edit "within")
    (let [selector (exact-file-selector edit)
          file (:file selector)
          source (get sources file)
          before (named-owner-evidence file (get edit "from"))
          after (named-owner-evidence file (get edit "to"))
          matches (get edit "matches")]
      (when (and selector
                 (string? source)
                 before
                 after
                 (= (select-keys before [:kind :name])
                    (select-keys after [:kind :name]))
                 (= 1 matches))
        (let [target (lossless-fingerprint (:node before))
              occurrences
              (->> (direct-form-nodes source)
                   (filter #(= target (lossless-fingerprint %)))
                   count)]
          (when (= 1 occurrences)
            (-> (singular-file edit selector)
                (assoc "within" {"form" (str (:name before))}))))))))

(def lowerers
  [[:law-a lower-law-a]
   [:law-b lower-law-b]
   [:law-c lower-law-c]])

(defn lower-edit
  [sources enabled-rules edit]
  (or
    (some (fn [[rule lower]]
            (when (contains? enabled-rules rule)
              (when-let [lowered (lower sources edit)]
                {:edit lowered :rule rule})))
          lowerers)
    {:edit edit :rule :unchanged}))

(defn expand-symbol-migration
  [request]
  (if (contains? request "symbol_migration")
    (migration/compile-manifest request)
    {:ok true :request request :owner-row-count 0 :declared-match-count 0}))

(defn symbol-migration-owner-match-rows
  [request]
  (mapv
    (fn [[file [owner _from matches]]]
      [file owner matches])
    (mapcat
      (fn [[file sites]]
        (map (fn [site] [file site]) sites))
      (get-in request ["symbol_migration" "files"] []))))

(defn expanded-owner-match-rows
  [request row-count]
  (mapv
    (fn [edit]
      [(get edit "file")
       (get-in edit ["within" "form"])
       (get edit "matches")])
    (take-last row-count (get request "edits" []))))

(defn lower-request
  [sources enabled-rules request]
  (let [expanded (expand-symbol-migration request)]
    (if-not (:ok expanded)
      expanded
      (let [lowered (mapv #(lower-edit sources enabled-rules %)
                          (get-in expanded [:request "edits"]))]
        (assoc expanded
               :request (assoc (:request expanded) "edits"
                               (mapv :edit lowered))
               :lowering-counts (frequencies (map :rule lowered)))))))

(defn compile-lowered
  [sources enabled-rules request]
  (let [lowered (lower-request sources enabled-rules request)]
    (if-not (:ok lowered)
      lowered
      (assoc lowered :product
             (if (some #(not (contains? % "within"))
                       (get-in lowered [:request "edits"]))
               {:ok false :error-type :historical-missing-within}
               (migration/compile-request sources (:request lowered)))))))

(defn compile-product
  [sources request]
  (let [requested-owner-match-rows
        (symbol-migration-owner-match-rows request)
        expanded (expand-symbol-migration request)]
    (if-not (:ok expanded)
      expanded
      (let [owner-match-rows
            (expanded-owner-match-rows
              (:request expanded) (:owner-row-count expanded))
            validated (contract/validate-tool-params (:request expanded))]
        (if-not (:ok validated)
          validated
          (let [spec (contract/tool-params->transaction (:params validated))
                prepared
                (compact-location/normalize-spec
                  sources spec (:compact-location-normalization validated))]
            (if (:error prepared)
              prepared
              (assoc prepared
                     :owner-row-count (:owner-row-count expanded)
                     :declared-match-count (:declared-match-count expanded)
                     :requested-owner-match-rows requested-owner-match-rows
                     :owner-match-rows owner-match-rows
                     :owner-match-rows-preserved
                     (= requested-owner-match-rows owner-match-rows)
                     :compiled
                     (transaction/compile-transaction
                       sources (:spec prepared))))))))))

(defn replay-product-one
  [sources expected-after-hashes capture]
  (let [result (compile-product sources (:request capture))
        compiled (:compiled result)]
    (merge
      (select-keys capture
                   [:run :base :capture-file :capture-bytes
                    :capture-sha256 :actual-capture-bytes
                    :actual-capture-sha256 :capture-bytes-equal
                    :capture-hash-equal :request-sha256
                    :actual-request-sha256 :request-hash-equal
                    :schema-valid])
      {:ok (and (:ok result) (:ok compiled))
       :owner-row-count (:owner-row-count result)
       :declared-match-count (:declared-match-count result)
       :owner-match-rows (:owner-match-rows result)
       :owner-match-rows-preserved (:owner-match-rows-preserved result)
       :match-count (:match-count compiled)
       :changed-file-count (:changed-file-count compiled)
       :future-hashes-equal
       (and (:ok compiled)
            (= expected-after-hashes (migration/file-hashes compiled)))
       :exact-future
       (and (:ok result)
            (:ok compiled)
            (= 51 (:match-count compiled))
            (= 9 (:changed-file-count compiled))
            (= expected-after-hashes (migration/file-hashes compiled)))
       :location-normalization (:location-normalization result)
       :error-type (or (:error-type result) (:error-type compiled))})))

(declare capture-files load-capture)

(def retained-manifest-file
  "dev/experiments/namespace_tolerance_retained_manifest.edn")

(defn canonical-data
  [value]
  (cond
    (map? value)
    (into (sorted-map)
          (map (fn [[key child]] [key (canonical-data child)]))
          value)

    (vector? value) (mapv canonical-data value)
    (sequential? value) (mapv canonical-data value)
    :else value))

(defn retained-request
  [manifest {:keys [base location-shape]}]
  (let [request (case base
                  :oracle migration/oracle-request
                  :candidate migration/candidate-manifest)
        names (:namespace-names manifest)
        namespace-count (count names)
        edit-at
        (fn [request index transform]
          (update-in request ["edits" index] transform))
        namespace-name-shape
        (fn [request]
          (reduce
            (fn [result [index namespace-name]]
              (edit-at result index
                       #(assoc % "within" {"form" namespace-name})))
            request
            (map-indexed vector names)))
        omitted-shape
        (fn [request end-index]
          (reduce
            (fn [result index]
              (edit-at result index
                       (fn [edit]
                         (-> edit
                             (assoc "files" [(get edit "file")])
                             (dissoc "file" "within")))))
            request
            (range end-index)))]
    (dissoc
      (case location-shape
        :namespace-name-in-form (namespace-name-shape request)
        :namespace-clauses-omitted (omitted-shape request namespace-count)
        :namespace-clauses-and-complete-owner-omitted
        (omitted-shape request (inc namespace-count)))
      "workspace_root")))

(defn load-retained-manifest
  []
  (edn/read-string (slurp retained-manifest-file)))

(defn retained-captures
  [manifest]
  (mapv
    (fn [{:keys [run capture-file capture-bytes capture-sha256
                 request-sha256]
          :as retained}]
      (let [capture (load-capture (io/file capture-file) run)
            request (:request capture)
            actual-request-sha256
            (sha256 (pr-str (canonical-data request)))]
        (assoc retained
               :request request
               :actual-capture-bytes (:capture-bytes capture)
               :actual-capture-sha256 (:capture-sha256 capture)
               :actual-request-sha256 actual-request-sha256
               :capture-bytes-equal
               (= capture-bytes (:capture-bytes capture))
               :capture-hash-equal
               (= capture-sha256 (:capture-sha256 capture))
               :request-hash-equal
               (= request-sha256 actual-request-sha256)
               :schema-valid (= capture-schema (:schema capture)))))
    (:runs manifest)))

(defn product-report
  []
  (let [{:keys [sources expected-after-hashes]} (migration/load-fixture)
        manifest (load-retained-manifest)
        captures (retained-captures manifest)
        runs (mapv #(replay-product-one sources expected-after-hashes %)
                   captures)
        request-hashes-equal (every? :request-hash-equal captures)
        raw-corpus-bound
        (every?
          #(and (:schema-valid %)
                (:capture-bytes-equal %)
                (:capture-hash-equal %)
                (:request-hash-equal %))
          captures)
        expected-owner-match-rows (:candidate-owner-match-rows manifest)
        owner-match-rows-exact
        (every?
          (fn [run]
            (and (:owner-match-rows-preserved run)
                 (= (if (= :candidate (:base run))
                      expected-owner-match-rows
                      [])
                    (:owner-match-rows run))))
          runs)]
    {:capture-count (count captures)
     :unique-request-count (count (set (map :request-sha256 captures)))
     :raw-corpus-bound raw-corpus-bound
     :request-hashes-equal request-hashes-equal
     :candidate-owner-match-rows expected-owner-match-rows
     :owner-match-rows-exact owner-match-rows-exact
     :exact-run-count (count (filter :exact-future runs))
     :all-eight-exact (and (= 8 (count captures))
                           raw-corpus-bound
                           request-hashes-equal
                           owner-match-rows-exact
                           (every? :exact-future runs))
     :runs runs}))

(defn exact-future?
  [product expected-after-hashes]
  (let [compiled (:compiled product)]
    (and (:ok product)
         (:ok compiled)
         (= 51 (:match-count compiled))
         (= 9 (:changed-file-count compiled))
         (= expected-after-hashes (migration/file-hashes compiled)))))

(defn capture-files
  [capture-root]
  (->> (.listFiles (io/file capture-root))
       (filter #(.isDirectory %))
       (map #(io/file % "captured-calls.json"))
       (filter #(.isFile %))
       (sort-by #(.getPath %))))

(defn load-capture
  ([capture-file]
   (load-capture capture-file (.getName (.getParentFile capture-file))))
  ([capture-file run]
   (let [raw (slurp capture-file)
         capture (json/parse-string raw)]
     {:run run
      :capture-file (.getPath capture-file)
      :capture-bytes (utf8-byte-count raw)
      :capture-sha256 (sha256 raw)
      :schema (get capture "schema")
      :request (get-in capture ["calls" 0 "params"])})))

(defn replay-one
  [sources expected-after-hashes enabled-rules capture]
  (let [result (compile-lowered sources enabled-rules (:request capture))
        product (:product result)
        compiled (:compiled product)]
    (merge
      (select-keys capture [:run :capture-sha256])
      {:schema-valid (= capture-schema (:schema capture))
       :lowering-counts (:lowering-counts result)
       :owner-row-count (:owner-row-count result)
       :declared-migration-match-count (:declared-match-count result)
       :validator-ok (true? (:ok product))
       :compiler-ok (true? (:ok compiled))
       :match-count (:match-count compiled)
       :changed-file-count (:changed-file-count compiled)
       :future-hashes-equal
       (and (:ok compiled)
            (= expected-after-hashes (migration/file-hashes compiled)))
       :exact-future (exact-future? product expected-after-hashes)
       :error-type (or (:error-type compiled) (:error-type product))
       :error-path (or (:path compiled) (:path product))})))

(defn current-refuses?
  [sources enabled-rules request]
  (let [result (compile-lowered sources enabled-rules request)]
    (not (and (get-in result [:product :ok])
              (get-in result [:product :compiled :ok])))))

(defn base-edit
  [file]
  {"file" file
   "within" {"form" "sample.app"}
   "from" "(:require [old.core :as old])"
   "to" "(:require [new.core :as new])"
   "matches" 1})

(defn falsifier-report
  []
  (let [file "src/sample/app.clj"
        source "(ns sample.app\n  (:require [old.core :as old]))\n(defn f [] 1)\n"
        sources {file source}
        request (fn [edit] {"edits" [edit]})
        law-a-edit (base-edit file)
        law-b-edit (-> law-a-edit
                       (dissoc "file" "within")
                       (assoc "files" [file]))
        law-c-edit {"files" [file]
                    "from" "(defn f [] 1)"
                    "to" "(defn f [] 2)"
                    "matches" 1}
        refusal (fn [test-sources rules edit]
                  (current-refuses? test-sources rules (request edit)))]
    {:two-law
     {:wrong-namespace
      (refusal sources #{:law-a :law-b}
               (assoc-in law-a-edit ["within" "form"] "sample.wrong"))
      :competing-owner
      (let [competing (str source "(def sample.app 1)\n")]
        (refusal {file competing} #{:law-a :law-b} law-a-edit))
      :multiple-namespaces
      (refusal {file (str source "(ns sample.other)\n")}
               #{:law-a :law-b} law-a-edit)
      :reader-conditional-namespace
      (refusal {file "#?(:clj (ns sample.app (:require [old.core :as old])))\n"}
               #{:law-a :law-b} law-a-edit)
      :non-namespace-missing-within
      (refusal sources #{:law-a :law-b}
               (assoc law-b-edit
                      "from" "(defn f [] 1)"
                      "to" "(defn f [] 2)"))
      :stale-count
      (refusal sources #{:law-a :law-b}
               (assoc law-b-edit "matches" 2))
      :mismatched-clause-kind
      (refusal sources #{:law-a :law-b}
               (assoc law-b-edit "to" "(:import java.time.Instant)"))
      :nested-only-clause
      (refusal
        {file "(ns sample.app {:probe (:require [old.core :as old])})\n"}
        #{:law-a :law-b} law-b-edit)
      :competing-identical-subtree-outside-namespace
      (refusal
        {file (str source "(def competing '(:require [old.core :as old]))\n")}
        #{:law-a :law-b} law-b-edit)
      :empty-files
      (refusal sources #{:law-a :law-b}
               (assoc law-b-edit "files" []))
      :multiple-files
      (refusal sources #{:law-a :law-b}
               (assoc law-b-edit "files" [file "src/sample/other.clj"]))
      :file-and-files
      (refusal sources #{:law-a :law-b}
               (assoc law-b-edit "file" file))}
     :law-c
     {:zero-occurrences
      (refusal {file "(ns sample.app)\n(defn other [] 1)\n"}
               #{:law-c} law-c-edit)
      :many-occurrences
      (refusal {file (str source "(defn f [] 1)\n")}
               #{:law-c} law-c-edit)
      :anonymous-owner
      (refusal sources #{:law-c}
               (assoc law-c-edit "from" "(+ 1 2)" "to" "(+ 2 3)"))
      :different-kind
      (refusal sources #{:law-c}
               (assoc law-c-edit "to" "(def f 2)"))
      :different-name
      (refusal sources #{:law-c}
               (assoc law-c-edit "to" "(defn g [] 2)"))
      :nested-only-occurrence
      (refusal {file "(ns sample.app)\n(def nested '(defn f [] 1))\n"}
               #{:law-c} law-c-edit)
      :stale-count
      (refusal sources #{:law-c}
               (assoc law-c-edit "matches" 2))}}))

(defn all-true?
  [value]
  (if (map? value)
    (every? all-true? (vals value))
    (true? value)))

(defn report
  ([] (report default-capture-root))
  ([capture-root]
   (let [{:keys [sources expected-after-hashes]} (migration/load-fixture)
         captures (mapv load-capture (capture-files capture-root))
         two-law-runs
         (mapv #(replay-one sources expected-after-hashes #{:law-a :law-b} %)
               captures)
         optional-c-runs
         (mapv #(replay-one sources expected-after-hashes
                            #{:law-a :law-b :law-c} %)
               captures)
         falsifiers (falsifier-report)
         two-law-exact (count (filter :exact-future two-law-runs))
         optional-c-exact (count (filter :exact-future optional-c-runs))
         candidate-runs (filterv #(str/ends-with? (:run %) "candidate")
                                 two-law-runs)
         candidate-migration-preserved
         (every? #(and (= 23 (:owner-row-count %))
                       (= 27 (:declared-migration-match-count %)))
                 candidate-runs)]
     {:schema :clj-surgeon.namespace-tolerance-replay/v1
      :integration-head "3e4a05e77c3abb523437e9430d73b524320e2780"
      :model-calls 0
      :mutation-actions 0
      :capture-count (count captures)
      :candidate-migration
      {:expanded-before-tolerance true
       :run-count (count candidate-runs)
       :all-preserve-23-owners-27-matches candidate-migration-preserved}
      :two-law
      {:claim :original-two-law-ceiling
       :exact-run-count two-law-exact
       :all-eight-exact (= 8 two-law-exact)
       :runs two-law-runs}
      :optional-law-c
      {:claim :separate-new-injective-law
       :exact-run-count optional-c-exact
       :all-eight-exact (= 8 optional-c-exact)
       :runs optional-c-runs}
      :falsifiers falsifiers
      :all-falsifiers-refuse (all-true? falsifiers)
      :experiment-green
      (and (= 8 (count captures))
           (= 7 two-law-exact)
           (= 8 optional-c-exact)
           candidate-migration-preserved
           (all-true? falsifiers))})))

(defn -main
  [& [capture-root]]
  (let [result (report (or capture-root default-capture-root))]
    (prn result)
    (when-not (:experiment-green result)
      (System/exit 1))))
