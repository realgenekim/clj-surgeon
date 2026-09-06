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
(def required-sections
  ["## Fan-out route (experimental default, 2026-09-06)"
   "1. Discover owners FIRST: `rg -l`, then one `inspect_clojure` `match` batch,"
   "2. THEN patch helper and `require`/alias natively with `apply_patch`. A helper"
   "3. ONE `apply_clojure_changes` call, edits"
   "4. Clear argument error: repair once from the refusal. Route unavailable,"
   "**Evidence and boundary.** Cohort I measured the INFORMED BATCHED EDIT route"
   "*Derived from doctrine commit 7a682b9e on clj-surgeon MCP/main, whose receipts"
   ;; The strictly-better rule and its kill switch. A plate that routes a class
   ;; without stating WHEN the route is withdrawn is an unbounded default, so the
   ;; rule line and the retirement line are pinned together.
   "**Strictly better, or native.** Route automatically only when the task matches a witnessed contract and the complete receipt path is available; otherwise use native. On one clear refusal repair once, then native fallback with a receipt. Meter complete verified wall, first-attempt success, fallback and unknown telemetry; retire a route when evidence no longer clears its native control."
   "**Kill switch.** Stop routing a class and re-run a preregistered native pair when"])

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

(defn- read-target [path]
  (let [file (io/file path)]
    (if (.exists file)
      (slurp file)
      "")))

(defn- prepare-target [path block]
  (let [result (upsert-routing-block (read-target path) block)]
    (assoc result :path path)))

(defn- prepare-install [block-file target-paths]
  (let [block (slurp block-file)
        missing (missing-sections block)
        targets (mapv #(prepare-target % block) target-paths)]
    (if (seq missing)
      {:ok false
       :operation :install-agent-routing
       :error-type :missing-required-routing-section
       :scope :canonical
       :block-file block-file
       :missing missing}
      (if-let [failure (first (remove :ok targets))]
        (assoc failure
               :ok false
               :operation :install-agent-routing
               :target (:path failure))
        {:ok true
         :operation :install-agent-routing
         :block block
         :block-hash (sha256 block)
         :targets targets}))))

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

(defn check-routing!
  "Check that every target contains the exact canonical block. Does not write."
  [block-file target-paths]
  (let [prepared (prepare-install block-file target-paths)
        drifted-target (when (:ok prepared)
                         (first (keep (fn [{:keys [path]}]
                                        (let [missing (missing-sections
                                                        (read-target path))]
                                          (when (seq missing)
                                            {:path path :missing missing})))
                                      (:targets prepared))))]
    (cond
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
                 "check" (check-routing! block-file target-paths)
                 {:ok false
                  :error-type :unknown-operation
                  :operation operation})]
    (prn result)
    (when-not (:ok result)
      (System/exit 2))))
