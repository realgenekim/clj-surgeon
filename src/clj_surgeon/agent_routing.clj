(ns clj-surgeon.agent-routing
  "Install one canonical clj-surgeon routing block into agent instructions."
  (:require
   [clj-surgeon.file-ops :as file-ops]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.math BigInteger)
   (java.security MessageDigest)))

;; @spec MCP-OP-RELAY-004
(def managed-begin "<!-- BEGIN CLJ-SURGEON ROUTING v:1 -->")
(def managed-end "<!-- END CLJ-SURGEON ROUTING v:1 -->")

;; The doctrine-agreement ratchet. A plate change that agents must READ is not
;; landed because a file changed; it is landed when the text is present, byte
;; for byte, in every managed block that takes effect. These lines are checked
;; both in the canonical source (so a generator that drops one cannot install)
;; and in every installed target (so a seat whose block was hand-edited or left
;; on an older plate is a LOUD refusal, not a silent disagreement).
;;
;; Every entry must be a SINGLE line of the plate; a wrapped paragraph is not a
;; byte-exact needle. Update this vector and the plate in the same commit.
;; @spec ROUTING-FANOUT-001
;; @spec ROUTING-SPLIT-001
(def required-sections
  ["**SUSPENDED 2026-09-08 (pair-1: tool 0.68×/0.59× native at 3 and 21 sites, equal acceptance; retest only as a whole-intent redesign)**"
   "Capability example only; no automatic fan-out route."
   "### Namespace split -- EXACT witnessed contract only"
   "Only the frozen Cell C source shape AND manifest qualify (experimental)."
   "`promotion_policy=promote-required`, `source_retirement=delete`,"
   "`roots=[src,test]` and a named cold `verification.profile`."
   "141 named owners / 20 absent destinations / 87 static sites / five caller files."
   "`state=committed`, `verification_complete=true`, `proof_pending=[]`,"
   "every required profile check present with `:exit 0`;"
   "`papercuts` must pass when the profile carries that oracle."
   "Escape: plan-only first when the mapping is uncertain (`plan_only=true` in X)."
   "A safely correctable typed refusal: repair once from `next_call`, then native."
   "Never re-run a committed split; finish pending proof or follow guarded recovery."
   "wall loss vs the registered controls suspends this route."
   "Non-claims: dynamic references, open-ended decomposition, other source grammars"
   "*Derived from doctrine commit 3c2eb6a9 on clj-surgeon astra/namespace-split (2026-09-08).*"
   ;; The strictly-better rule and its kill switch. A plate that routes a class
   ;; without stating WHEN the route is withdrawn is an unbounded default, so the
   ;; rule line and the retirement line are pinned together.
   "**Strictly better, or native.** Route automatically only when the task matches a witnessed contract and the complete receipt path is available; otherwise use native. On one clear refusal repair once, then native fallback with a receipt. Meter complete verified wall, first-attempt success, fallback and unknown telemetry; retire a route when evidence no longer clears its native control."
   "**Kill switch.** A correctness failure SUSPENDS a routed class immediately. A wall"])

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
