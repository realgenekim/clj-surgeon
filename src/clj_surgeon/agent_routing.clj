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

(defn- marker-scan
  "Every marker of EVERY version, with its span and its declared version.
   Scanning only the current version's literal is what let a superseded block
   survive next to a fresh one: the old bytes were invisible to the check."
  [pattern source]
  (let [matcher (re-matcher pattern source)]
    (loop [found []]
      (if (.find matcher)
        (recur (conj found {:start (.start matcher)
                            :end (.end matcher)
                            :version (parse-long (.group matcher 1))}))
        found))))

(defn- marker-state
  "Exactly ONE well-formed BEGIN/END pair, at ONE version, across ALL versions.
   Anything else is refused with a diagnosis and the file is left alone: a
   second block, a crossed pair, or a version-mismatched pair means a human
   edited a managed region and the installer cannot know which rule governs."
  [source]
  (let [begins (marker-scan any-begin-pattern source)
        ends (marker-scan any-end-pattern source)
        refuse (fn [diagnosis]
                 {:ok false
                  :error-type :invalid-managed-routing
                  :diagnosis diagnosis
                  :begin-count (count begins)
                  :end-count (count ends)
                  :begin-versions (mapv :version begins)
                  :end-versions (mapv :version ends)})]
    (cond
      (and (empty? begins) (empty? ends))
      {:ok true :state :absent}

      (or (not= 1 (count begins)) (not= 1 (count ends)))
      (refuse (str "expected exactly one CLJ-SURGEON ROUTING marker pair across all "
                   "versions; found " (count begins) " BEGIN "
                   (pr-str (mapv :version begins)) " and " (count ends) " END "
                   (pr-str (mapv :version ends))
                   ". Delete every routing block by hand until at most one "
                   "remains, then re-run the installer."))

      (>= (:start (first begins)) (:start (first ends)))
      (refuse (str "the END marker (v:" (:version (first ends))
                   ") appears before the BEGIN marker (v:"
                   (:version (first begins)) "); the managed region is inverted."))

      (not= (:version (first begins)) (:version (first ends)))
      (refuse (str "BEGIN is v:" (:version (first begins)) " but END is v:"
                   (:version (first ends))
                   "; a routing block must open and close at the same version. "
                   "Neither marker can be trusted to bound the managed region."))

      :else
      (let [version (:version (first begins))
            span {:begin (:start (first begins)) :end (:end (first ends))}]
        (if (= managed-version version)
          (merge {:ok true :state :present} span)
          (merge {:ok true :state :stale :stale-version version} span))))))

(defn- routing-state
  "One pair or nothing. A lone older-version pair is reported stale and is
   replaced IN PLACE, so no bytes of the superseded rule survive."
  [source]
  (marker-state source))

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
