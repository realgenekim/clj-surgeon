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

(def ^:private begin-prefix "<!-- BEGIN CLJ-SURGEON ROUTING")
(def ^:private end-prefix "<!-- END CLJ-SURGEON ROUTING")

;; A marker is WELL FORMED only if the prefix is followed by exactly
;; " v:<positive integer> -->". `v:x`, `v:`, `v:2a`, `v:0` and a missing `v:`
;; are all malformed -- and malformed is a REFUSAL, never an absence.
(def ^:private well-formed-begin #"<!-- BEGIN CLJ-SURGEON ROUTING v:([1-9]\d*) -->")
(def ^:private well-formed-end #"<!-- END CLJ-SURGEON ROUTING v:([1-9]\d*) -->")

(defn- line-bounds
  "[start end) of the line containing `idx`."
  [^String source idx]
  (let [nl (.indexOf source "\n" (int idx))
        end (if (neg? nl) (count source) nl)
        prev (.lastIndexOf source "\n" (int idx))
        start (if (neg? prev) 0 (inc prev))]
    [start end]))

(defn- marker-scan
  "Every marker of EVERY version, with its span and its declared version --
   found by PREFIX, not by a well-formed pattern. Scanning only well-formed
   markers is what let `v:x` read as `:absent`: a line a human clearly meant as
   a managed marker was invisible to the check, so the installer appended a
   second block beside a region it could not bound. A prefix hit whose version
   is not a well-formed positive integer is returned as `:malformed`, and every
   caller refuses on it."
  [^java.util.regex.Pattern pattern ^String prefix ^String source]
  (loop [from 0
         found []]
    (let [idx (.indexOf source prefix (int from))]
      (if (neg? idx)
        found
        (let [[line-start line-end] (line-bounds source idx)
              matcher (doto (re-matcher pattern source)
                        (.region idx line-end))
              hit (if (.lookingAt matcher)
                    {:start idx
                     :end (.end matcher)
                     :version (parse-long (.group matcher 1))}
                    {:start idx
                     :malformed true
                     :line (str/trim (subs source line-start line-end))})]
          (recur (long (inc idx)) (conj found hit)))))))

(defn- marker-state
  "Exactly ONE well-formed BEGIN/END pair, at ONE version, across ALL versions.
   Anything else is refused with a diagnosis and the file is left alone: a
   malformed version, a second block, a crossed pair, or a version-mismatched
   pair means a human edited a managed region and the installer cannot know
   which rule governs."
  [source]
  (let [begins (marker-scan well-formed-begin begin-prefix source)
        ends (marker-scan well-formed-end end-prefix source)
        malformed (filterv :malformed (concat begins ends))
        refuse (fn [diagnosis]
                 (cond-> {:ok false
                          :error-type :invalid-managed-routing
                          :diagnosis diagnosis
                          :begin-count (count begins)
                          :end-count (count ends)
                          :begin-versions (mapv #(or (:version %) :malformed) begins)
                          :end-versions (mapv #(or (:version %) :malformed) ends)}
                   (seq malformed)
                   (assoc :malformed-markers (mapv :line malformed))))]
    (cond
      (seq malformed)
      (refuse (str "a CLJ-SURGEON ROUTING marker declares a version that is not a "
                   "positive integer: " (pr-str (mapv :line malformed))
                   ". A marker line the installer cannot version cannot be trusted "
                   "to bound a managed region, so the file is left untouched. Fix "
                   "the version by hand (or delete the block), then re-run the "
                   "installer."))

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

(def ^:private scratch-root
  "The ONLY root a --scratch install may write under. Tests need a real
   filesystem target; they do not need the ability to name one anywhere."
  "/var/tmp/forge")

(defn- canonical
  "The path a write would actually land on: symlinks resolved where they exist,
   `..` and `.` normalized everywhere. Comparing the STRING a caller typed is
   not confinement -- `$HOME/.codex/../../etc/passwd` is a different string and
   the same file."
  [path]
  (.getCanonicalPath (io/file (str path))))

(defn- allowed-global-targets []
  (let [home (System/getProperty "user.home")]
    #{(canonical (io/file home ".codex" "AGENTS.md"))
      (canonical (io/file home ".claude" "CLAUDE.md"))}))

(defn confine-targets
  "Fail closed on any target that is not one of the two global instruction
   files -- or, under --scratch, not under /var/tmp/forge.

   Sol fence r4, finding 5: the default Make invocation names only the two
   global files, but both the Make overrides and this CLI accepted ARBITRARY
   destinations, and a scratch installation outside those paths succeeded. A
   target list is authority, and authority a caller supplies is not authority
   the tool has checked."
  [target-paths scratch?]
  (let [allowed (allowed-global-targets)
        scratch-prefix (str (canonical scratch-root) java.io.File/separator)
        refused (for [path target-paths
                      :let [real (canonical path)]
                      :when (if scratch?
                              (not (str/starts-with? real scratch-prefix))
                              (not (contains? allowed real)))]
                  {:path path :canonical-path real})]
    (if (seq refused)
      {:ok false
       :error-type :agent-routing-target-refused
       :scratch scratch?
       :refused (vec refused)
       :allowed (if scratch?
                  [(str scratch-prefix "...")]
                  (vec (sort allowed)))
       :diagnosis (str "refused " (count refused) " target path(s): "
                       (pr-str (mapv :canonical-path refused))
                       (if scratch?
                         (str ". Under --scratch every target must resolve under "
                              scratch-root ".")
                         (str ". Without --scratch the only permitted targets are "
                              (pr-str (vec (sort allowed)))
                              "; pass --scratch to write under " scratch-root
                              " instead.")))}
      {:ok true :targets (vec target-paths)})))

(defn -main [operation block-file & args]
  (let [scratch? (boolean (some #{"--scratch"} args))
        target-paths (remove #{"--scratch"} args)
        confined (confine-targets target-paths scratch?)
        result (cond
                 (not (:ok confined))
                 (assoc confined :operation (case operation
                                              "install" :install-agent-routing
                                              "check" :check-agent-routing
                                              operation))

                 :else
                 (case operation
                   "install" (install-routing! block-file target-paths)
                   "check" (check-routing! block-file target-paths)
                   {:ok false
                    :error-type :unknown-operation
                    :operation operation}))]
    (prn result)
    (when-not (:ok result)
      (System/exit 2))))
