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
(def managed-version
  "The current routing-block intent version. Bump when the rendered rule changes
   meaning, so an installed older block is refused as stale rather than ignored."
  2)

(def managed-begin (str "<!-- BEGIN CLJ-SURGEON ROUTING v:" managed-version " -->"))
(def managed-end (str "<!-- END CLJ-SURGEON ROUTING v:" managed-version " -->"))

(def ^:private any-begin-pattern #"<!-- BEGIN CLJ-SURGEON ROUTING v:(\d+) -->")
(def ^:private any-end-pattern #"<!-- END CLJ-SURGEON ROUTING v:(\d+) -->")

(defn- stale-marker-state
  "Locate a single routing block written at a version other than the current one.
   A stale block is real drift: the seat is reading a superseded rule."
  [source]
  (let [begins (vec (re-seq any-begin-pattern source))
        ends (vec (re-seq any-end-pattern source))]
    (when (and (= 1 (count begins)) (= 1 (count ends)))
      (let [begin-text (first (first begins))
            end-text (first (first ends))
            version (second (first begins))
            begin (str/index-of source begin-text)
            end (str/index-of source end-text)]
        (when (and begin end (< begin end))
          {:ok true
           :state :stale
           :stale-version (parse-long version)
           :begin begin
           :end (+ end (count end-text))})))))

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

(defn- routing-state
  "Current-version markers win. A lone older-version block is reported stale."
  [source]
  (let [state (marker-state source)]
    (if (and (:ok state) (= :absent (:state state)))
      (or (stale-marker-state source) state)
      state)))

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
    (let [state (routing-state source)]
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
            (cond-> {:ok true
                     :previous-state (cond
                                       (= :stale (:state state)) :stale
                                       changed :replaced
                                       :else :current)
                     :changed changed
                     :source updated}
              (:stale-version state) (assoc :stale-version
                                            (:stale-version state)))))))))

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
        targets (mapv #(prepare-target % block) target-paths)]
    (if-let [failure (first (remove :ok targets))]
      (assoc failure
             :ok false
             :operation :install-agent-routing
             :target (:path failure))
      {:ok true
       :operation :install-agent-routing
       :block block
       :block-hash (sha256 block)
       :targets targets})))

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
         :targets (mapv #(select-keys % [:path :previous-state :changed
                                         :stale-version])
                        (:targets prepared))}))))

(defn check-routing!
  "Check that every target contains the exact canonical block. Does not write."
  [block-file target-paths]
  (let [prepared (prepare-install block-file target-paths)]
    (cond
      (not (:ok prepared)) prepared
      (every? (complement :changed) (:targets prepared))
      {:ok true
       :operation :check-agent-routing
       :block-hash (:block-hash prepared)
       :target-count (count (:targets prepared))}
      :else
      (let [drifted (filterv :changed (:targets prepared))]
        {:ok false
         :operation :check-agent-routing
         :error-type (if (some #(= :stale (:previous-state %)) drifted)
                       :agent-routing-stale-version
                       :agent-routing-drift)
         :expected-version managed-version
         :targets (mapv #(select-keys % [:path :previous-state :changed
                                         :stale-version])
                        drifted)}))))

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
