(ns clj-surgeon.agent-routing
  "Install one canonical clj-surgeon routing block into agent instructions."
  (:require
   [clj-surgeon.file-ops :as file-ops]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.math BigInteger)
   (java.security MessageDigest)))

;; @spec MCP-OP-RELAY-004
(def managed-begin "<!-- BEGIN CLJ-SURGEON ROUTING v:1 -->")
(def managed-end "<!-- END CLJ-SURGEON ROUTING v:1 -->")

;; @spec ROUTING-PARITY-001
(defn registry-sections
  "Derive mandatory passages in declared intent order; invalid registries refuse."
  [source]
  (try
    (let [ids (mapv second (re-seq #"(?m)^- \[x\] \*\*(ROUTING-[A-Z]+-[0-9]+)\*\*:" source))
          blocks (re-seq #"(?ms)^```edn routing-requirements\n(.*?)^```$" source)]
      (when-not (and (seq ids) (= (count ids) (count (distinct ids)))
                  (= 1 (count blocks)))
        (throw (ex-info "Expected declared intents and one routing registry" {})))
      (let [reader (java.io.PushbackReader. (java.io.StringReader. (second (first blocks))))
            registry (edn/read {:eof nil} reader)]
        (when-not (and (map? registry) (= (set ids) (set (keys registry)))
                    (every? (fn [needles]
                              (and (vector? needles) (seq needles)
                                   (every? #(and (string? %) (not (str/blank? %))) needles)))
                            (vals registry))
                    (= ::eof (edn/read {:eof ::eof} reader)))
          (throw (ex-info "Each declared intent requires nonempty plate passages" {})))
        (vec (mapcat registry ids))))
    (catch Exception cause
      (throw (ex-info "Invalid routing intent registry"
               {:error-type :invalid-routing-registry} cause)))))

;; This is a repository installer: anchor its intent authority to its source,
;; including when Make invokes it from outside the checkout. Missing docs fail closed.
;; @spec ROUTING-FANOUT-001
;; @spec ROUTING-SPLIT-001
;; @spec ROUTING-PARITY-001
(def required-sections
  (registry-sections
    (slurp (io/file (-> (io/resource "clj_surgeon/agent_routing.clj")
                      io/file .getParentFile .getParentFile .getParentFile)
             "docs/intent/agent-routing/agent-routing-specs.md"))))

(defn missing-sections
  "Required plate sections absent from `source`, in declaration order."
  [source]
  (vec (remove #(str/includes? source %) required-sections)))

(defn- indexes-of [source needle]
  (loop [from 0
         indexes []]
    (if-let [index (str/index-of source needle from)]
      (recur (+ index (count needle)) (conj indexes index))
      indexes)))

(defn- marker-state [source]
  (let [begins (indexes-of source managed-begin)
        ends (indexes-of source managed-end)]
    (cond
      (and (empty? begins) (empty? ends))
      {:ok true :state :absent}

      (and (= 1 (count begins))
           (= 1 (count ends))
           (< (first begins) (first ends)))
      {:ok true
       :state :present
       :begin (first begins)
       :end (+ (first ends) (count managed-end))}

      :else
      {:ok false
       :error-type :invalid-managed-routing
       :begin-count (count begins)
       :end-count (count ends)})))

(defn- valid-canonical-block? [block]
  (let [state (marker-state block)]
    (and (:ok state)
         (= :present (:state state))
         (zero? (:begin state))
         (= (count (str/trimr block)) (:end state)))))

(defn- append-block [source block]
  (cond
    (empty? source) block
    (str/ends-with? source "\n\n") (str source block)
    (str/ends-with? source "\n") (str source "\n" block)
    :else (str source "\n\n" block)))

(defn upsert-routing-block
  "Return a source update or a fail-closed marker error. Does not write."
  [source block]
  (if-not (valid-canonical-block? block)
    {:ok false
     :error-type :invalid-canonical-routing
     :source source}
    (let [state (marker-state source)]
      (if-not (:ok state)
        (assoc state :source source)
        (if (= :absent (:state state))
          {:ok true
           :previous-state :absent
           :changed true
           :source (append-block source block)}
          (let [suffix-start (if (and (< (:end state) (count source))
                                      (= \newline (.charAt source (:end state))))
                               (inc (:end state))
                               (:end state))
                updated (str (subs source 0 (:begin state))
                             block
                             (subs source suffix-start))
                changed (not= source updated)]
            {:ok true
             :previous-state (if changed :replaced :current)
             :changed changed
             :source updated}))))))

(defn- sha256 [source]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256")
                        (.getBytes source "UTF-8"))]
    (format "%064x" (BigInteger. 1 digest))))

;; @spec ROUTING-PARITY-001
(defn validate-routing-block
  "Validate canonical bytes without reading any installed target."
  [block]
  (let [missing (missing-sections block)
        lines (count (str/split-lines block))
        bytes (alength (.getBytes ^String block "UTF-8"))]
    (cond
      (not (valid-canonical-block? block))
      {:ok false :error-type :invalid-canonical-routing}

      (seq missing)
      {:ok false :error-type :missing-required-routing-section :missing missing}

      (or (> lines 188) (> bytes 10738))
      {:ok false :error-type :routing-budget-exceeded :lines lines :bytes bytes}

      :else
      {:ok true :block-hash (sha256 block) :lines lines :bytes bytes
       :doctrine-commit (second (re-find #"Derived from doctrine commit ([0-9a-f]{8}) " block))})))

(defn- read-target [path]
  (let [file (io/file path)]
    (if (.exists file)
      (slurp file)
      "")))

(defn- prepare-target [path block]
  (let [source (read-target path)
        result (upsert-routing-block source block)
        {:keys [state begin end]} (marker-state source)
        installed (when (= :present state)
                    (let [end (if (and (< end (count source))
                                       (= \newline (.charAt source end)))
                                (inc end) end)]
                      (subs source begin end)))]
    (assoc result :path path
           :missing (missing-sections (or installed ""))
           :block-hash (when installed (sha256 installed)))))

(defn- prepare-install [block-file target-paths]
  (let [block (slurp block-file)
        validation (validate-routing-block block)]
    (if-not (:ok validation)
      (assoc validation :operation :install-agent-routing
             :scope :canonical :block-file block-file)
      (let [targets (mapv #(prepare-target % block) target-paths)]
        (if-let [failure (first (remove :ok targets))]
          (assoc failure
                 :ok false
                 :operation :install-agent-routing
                 :target (:path failure))
          (assoc validation :operation :install-agent-routing
                 :block block :targets targets))))))

(defn install-routing!
  "Install the canonical block after every target passes preflight."
  [block-file target-paths]
  (let [prepared (prepare-install block-file target-paths)]
    (if-not (:ok prepared)
      prepared
      (do
        (doseq [{:keys [path source changed]} (:targets prepared)
                :when changed]
          (.mkdirs (.getParentFile (.getAbsoluteFile (io/file path))))
          (file-ops/atomic-write! path source))
        {:ok true
         :operation :install-agent-routing
         :block-hash (:block-hash prepared)
         :target-count (count (:targets prepared))
         :changed-count (count (filter :changed (:targets prepared)))
         :targets (mapv #(select-keys % [:path :previous-state :changed])
                        (:targets prepared))}))))

;; @spec ROUTING-PARITY-001
(defn check-routing!
  "Check that every target contains the exact canonical block. Does not write."
  [block-file target-paths]
  (let [prepared (prepare-install block-file target-paths)
        drifted-target (when (:ok prepared)
                         (first (filter #(seq (:missing %)) (:targets prepared))))]
    (cond
      (empty? target-paths)
      {:ok false :operation :check-agent-routing :error-type :missing-routing-targets}

      (not (:ok prepared)) prepared

      drifted-target
      {:ok false
       :operation :check-agent-routing
       :error-type :missing-required-routing-section
       :scope :installed
       :target (:path drifted-target)
       :missing (:missing drifted-target)}

      (every? (complement :changed) (:targets prepared))
      {:ok true
       :operation :check-agent-routing
       :block-hash (:block-hash prepared)
       :doctrine-commit (:doctrine-commit prepared)
       :scope :installed
       :targets (mapv #(select-keys % [:path :block-hash]) (:targets prepared))
       :target-count (count (:targets prepared))}
      :else
      {:ok false
       :operation :check-agent-routing
       :error-type :agent-routing-drift
       :targets (mapv #(select-keys % [:path :previous-state :changed])
                      (filter :changed (:targets prepared)))})))

(defn -main [operation block-file & target-paths]
  (let [result (case operation
                 "install" (install-routing! block-file target-paths)
                 "check" (if (= ["--plate-only"] (vec target-paths))
                           (assoc (validate-routing-block (slurp block-file))
                                  :operation :check-agent-routing
                                  :scope :plate-only :target-count 0)
                           (check-routing! block-file target-paths))
                 {:ok false
                  :error-type :unknown-operation
                  :operation operation})]
    (prn result)
    (when-not (:ok result)
      (System/exit 2))))
