(ns clj-surgeon.worktree-lifecycle
  "Pure compiler for evidence-preserving, single-target worktree closure."
  (:require
   [clojure.set :as set]
   [clojure.string :as str])
  (:import
   (java.io ByteArrayOutputStream)
   (java.nio ByteBuffer)
   (java.nio.charset CharacterCodingException CodingErrorAction StandardCharsets)
   (java.security MessageDigest)
   (java.time Instant)
   (java.util UUID)))

(def snapshot-schema :clj-surgeon.worktree-lifecycle-snapshot/v1)
(def close-request-schema :clj-surgeon.worktree-close-request/v1)
(def prune-request-schema
  :clj-surgeon.worktree-registration-prune-request/v1)
(def handoff-schema :clj-surgeon.worktree-handoff/v1)
(def plan-schema :clj-surgeon.worktree-close-plan/v1)
(def prune-plan-schema :clj-surgeon.worktree-registration-prune-plan/v1)
(def receipt-schema :clj-surgeon.worktree-close-receipt/v1)
(def prune-receipt-schema
  :clj-surgeon.worktree-registration-prune-receipt/v1)
(def negative-seal-schema :clj-surgeon.negative-experiment-seal/v1)

(def ^:private snapshot-fields
  #{:schema :captured-at :repository :controller-worktree :git-worktrees
    :supacode :remotes :ancestry :handoffs :lifecycle-leases})
(def ^:private request-fields
  #{:schema :target :outcome :handoff :evidence})
(def ^:private prune-request-fields #{:schema :target :preservation})
(def ^:private plan-fields
  #{:schema :plan-id :plan-sha256 :repository :controller :captured-at
    :target :outcome :outcome-evidence :handoff :lifecycle-lease-prestate
    :expected-lifecycle-lease :supacode})
(def ^:private prune-plan-fields
  #{:schema :plan-id :plan-sha256 :operation :repository :controller
    :captured-at :target :preservation :handoff :lifecycle-lease-prestate
    :expected-lifecycle-lease :supacode})
(def ^:private repository-fields
  #{:root :common-git-dir :primary-worktree :object-format})
(def ^:private controller-fields #{:commit :tree :clean :artifacts})
(def ^:private controller-artifact-fields
  #{"Makefile"
    "src/clj_surgeon/worktree_lifecycle.clj"
    "src/clj_surgeon/worktree_lifecycle_io.clj"})
(def ^:private nearest-parent-fields #{:path :device :inode})
(def ^:private prune-postcondition-fields
  #{:target-present :registration-present :branch-unchanged
    :preservation-unchanged :supacode-state})
(def ^:private outcomes #{:landed :negative-experiment :parked})
(def ^:private handoff-modes #{:agent :legacy})
(def ^:private forbidden-record-keys
  #{:source :source-body :prompt :transcript :token :tokens :credential
    :credentials :remote-url :environment :env})

(defn sha256
  "Return the lower-case SHA-256 of UTF-8 text."
  [value]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256")
                        (.getBytes (str value) StandardCharsets/UTF_8))]
    (apply str (map #(format "%02x" (bit-and % 0xff)) digest))))

(defn- canonical-value [value]
  (cond
    (map? value)
    (into (sorted-map-by #(compare (pr-str %1) (pr-str %2)))
          (map (fn [[key nested]] [key (canonical-value nested)]))
          value)

    (vector? value) (mapv canonical-value value)
    (list? value) (apply list (map canonical-value value))
    (set? value)
    (throw (ex-info "Public sets are not canonical plan data"
                    {:error-type :non-canonical-public-set}))
    (sequential? value) (mapv canonical-value value)
    :else value))

(defn canonical-edn
  "Render recursively ordered EDN with one trailing newline."
  [value]
  (str (pr-str (canonical-value value)) "\n"))

(defn- refuse [error-type & [details]]
  (merge {:ok false :error-type error-type} details))

(defn- exact-fields? [value allowed]
  (and (map? value) (set/subset? (set (keys value)) allowed)))

(defn- hex? [length value]
  (and (string? value)
       (= length (count value))
       (boolean (re-matches #"[0-9a-f]+" value))))

(defn- oid-length [object-format]
  (case object-format :sha1 40 :sha256 64 nil))

(defn- exact-oid? [object-format value]
  (some-> (oid-length object-format) (hex? value)))

(defn- strict-utf8 [bytes]
  (try
    (-> (.newDecoder StandardCharsets/UTF_8)
        (.onMalformedInput CodingErrorAction/REPORT)
        (.onUnmappableCharacter CodingErrorAction/REPORT)
        (.decode (ByteBuffer/wrap bytes))
        str)
    (catch CharacterCodingException _ nil)))

(defn- hex-digit [character]
  (Character/digit ^char character 16))

(defn- percent-decode [encoded]
  (when (string? encoded)
    (let [output (ByteArrayOutputStream.)]
      (loop [index 0]
        (if (= index (count encoded))
          (strict-utf8 (.toByteArray output))
          (let [character (.charAt encoded index)]
            (if (= character \%)
              (if (< (+ index 2) (count encoded))
                (let [high (hex-digit (.charAt encoded (inc index)))
                      low (hex-digit (.charAt encoded (+ index 2)))]
                  (when (and (not (neg? high)) (not (neg? low)))
                    (.write output (+ (* high 16) low))
                    (recur (+ index 3))))
                nil)
              (let [bytes (.getBytes (str character) StandardCharsets/UTF_8)]
                (.write output bytes 0 (alength bytes))
                (recur (inc index))))))))))

(defn- absolute-clean-path? [path]
  (and (string? path)
       (str/starts-with? path "/")
       (not (str/includes? path "\u0000"))
       (not-any? #{".." "."} (str/split path #"/"))))

(defn- row-path [row]
  (or (:path-lexical row) (:path row)))

(defn- valid-prune-repository? [repository]
  (and (= repository-fields (set (keys repository)))
       (every? absolute-clean-path?
               (map repository [:root :common-git-dir :primary-worktree]))
       (contains? #{:sha1 :sha256} (:object-format repository))))

(defn- valid-prune-controller? [object-format controller]
  (and (= controller-fields (set (keys controller)))
       (exact-oid? object-format (:commit controller))
       (exact-oid? object-format (:tree controller))
       (true? (:clean controller))
       (map? (:artifacts controller))
       (= controller-artifact-fields (set (keys (:artifacts controller))))
       (every? #(hex? 64 %) (vals (:artifacts controller)))))

(defn decode-supacode-id
  "Strictly decode one Supacode worktree ID. Existing paths must be canonical."
  [encoded existing]
  (let [decoded (percent-decode encoded)]
    (cond
      (nil? decoded) (refuse :invalid-supacode-id)
      (re-find #"%[0-9A-Fa-f]{2}" decoded) (refuse :double-encoded-supacode-id)
      (not (absolute-clean-path? decoded)) (refuse :invalid-worktree-path)
      (and existing
           (try
             (not= decoded (.getCanonicalPath (java.io.File. decoded)))
             (catch Exception _ true)))
      (refuse :non-canonical-worktree-path)
      :else {:ok true :path decoded :existing (boolean existing)})))

(defn validate-git-worktree
  "Validate the closed Git facts used by classification and planning."
  [object-format worktree]
  (let [present-fields #{:path :head :tree :branch :detached :locked
                         :lock-reason :prunable :status :removal-preflight}
        absent-fields #{:path-lexical :path-state :path-real
                        :nearest-existing-parent :head :tree :branch :detached
                        :locked :lock-reason :prunable :status
                        :removal-preflight}
        fields (set (keys worktree))
        absent-row? (= fields absent-fields)
        preflight (:removal-preflight worktree)]
    (cond
      (not (contains? #{present-fields absent-fields} fields))
      (refuse :invalid-git-worktree-fields)
      (not (absolute-clean-path? (row-path worktree)))
      (refuse :invalid-worktree-path)
      (and absent-row?
           (not (and (= :absent (:path-state worktree))
                     (nil? (:path-real worktree))
                     (map? (:nearest-existing-parent worktree))
                     (= nearest-parent-fields
                        (set (keys (:nearest-existing-parent worktree))))
                     (absolute-clean-path?
                       (get-in worktree [:nearest-existing-parent :path]))
                     (every? #(or (nil? %) (integer? %))
                             (map #(get-in worktree
                                           [:nearest-existing-parent %])
                                  [:device :inode]))
                     (= :not-applicable (:status worktree))
                     (= :not-applicable (:removal-preflight worktree)))))
      (refuse :invalid-absent-path-authority)
      (not (exact-oid? object-format (:head worktree))) (refuse :invalid-git-head)
      (not (exact-oid? object-format (:tree worktree)))
      (refuse :invalid-git-tree)
      (not (contains? #{:clean :dirty :unknown :not-applicable}
                      (:status worktree)))
      (refuse :invalid-worktree-status)
      (and (not= :not-applicable preflight)
           (not (and (map? preflight)
                     (boolean? (:eligible preflight))
                     (contains? #{:none :present :unknown}
                                (:submodules preflight))
                     (vector? (:reasons preflight)))))
      (refuse :invalid-removal-preflight)
      :else {:ok true :worktree worktree})))

(defn validate-snapshot
  "Validate the closed snapshot shape and repository object format."
  ;; @spec WTL-INV-001 WTL-INV-002 WTL-INV-003
  [snapshot]
  (let [repository (:repository snapshot)
        object-format (:object-format repository)
        worktrees (:git-worktrees snapshot)]
    (cond
      (not (exact-fields? snapshot snapshot-fields)) (refuse :invalid-snapshot-fields)
      (not= snapshot-schema (:schema snapshot)) (refuse :invalid-snapshot-schema)
      (not (contains? #{:sha1 :sha256} object-format))
      (refuse :invalid-object-format)
      (not (and (map? repository)
                (every? absolute-clean-path?
                        (map repository
                             [:root :common-git-dir :primary-worktree]))))
      (refuse :invalid-repository-identity)
      (not (absolute-clean-path? (:controller-worktree snapshot)))
      (refuse :invalid-controller-worktree)
      (not (vector? worktrees)) (refuse :invalid-git-worktree-list)
      :else
      (if-let [invalid (some #(when-not (:ok (validate-git-worktree object-format %)) %)
                             worktrees)]
        (refuse :invalid-git-worktree {:worktree invalid})
        {:ok true :snapshot snapshot}))))

(defn- git-row [snapshot path]
  (some #(when (= path (row-path %)) %) (:git-worktrees snapshot)))

(defn- supacode-row [snapshot path]
  (some #(when (= path (row-path %)) %)
        (get-in snapshot [:supacode :worktrees])))

(defn- plan-current? [snapshot path plan]
  (let [row (git-row snapshot path)]
    (and (map? plan)
         (= path (get-in plan [:target :path]))
         (= (:head row) (get-in plan [:target :head]))
         (= (:tree row) (get-in plan [:target :tree]))
         (= :absent (:lifecycle-lease-prestate plan))
         (nil? (get-in snapshot [:lifecycle-leases path])))))

(defn classify-target
  "Return the one closed classification for a union identity."
  ;; @spec WTL-INV-004 WTL-INV-005 WTL-INV-006
  [snapshot path plan]
  (let [git (git-row snapshot path)
        supacode (supacode-row snapshot path)
        missing-path? (or (= :absent (:path-state git))
                          (some #{:missing-path}
                                (get-in git [:removal-preflight :reasons])))
        active-reason
        (cond
          (= path (get-in snapshot [:repository :primary-worktree])) :primary-worktree
          (= path (:controller-worktree snapshot)) :controller-worktree
          (:focused supacode) :focused
          (= :pinned (:status supacode)) :pinned
          (= true (:live supacode)) :live
          (= :unknown (:live supacode)) :live-state-unknown
          (= :main (:status supacode)) :contradictory-main
          (:locked git) :agent-locked
          (get-in snapshot [:lifecycle-leases path]) :lifecycle-leased)
        dirty-reason
        (cond
          (and git (not missing-path?) (not= :clean (:status git)))
          :worktree-not-clean
          (and git (not missing-path?)
               (not (get-in git [:removal-preflight :eligible]))) :not-removable
          (and git (not missing-path?)
               (not= :none (get-in git [:removal-preflight :submodules])))
          :submodule-state)
        missing-reason
        (cond
          (and supacode (nil? git)) :supacode-only
          (and git missing-path?) :missing-checkout
          (and git (:prunable git)) :broken-git-registration)]
    (cond
      active-reason {:path path :classification :active :reasons [active-reason]}
      dirty-reason {:path path :classification :dirty-blocked :reasons [dirty-reason]}
      missing-reason {:path path :classification :missing-prunable
                      :reasons [missing-reason]}
      (nil? git) {:path path :classification :missing-prunable
                  :reasons [:missing-git-registration]}
      (plan-current? snapshot path plan)
      {:path path :classification :clean-safe :reasons []}
      :else {:path path :classification :needs-seal :reasons [:no-current-plan]})))

(defn compile-audit
  "Compile one deterministic row for every Git or Supacode identity."
  ;; @spec WTL-INV-007
  [snapshot]
  (if-not (:ok (validate-snapshot snapshot))
    (validate-snapshot snapshot)
    (let [paths (->> (concat (map :path (:git-worktrees snapshot))
                             (map :path (get-in snapshot [:supacode :worktrees])))
                     distinct
                     sort)]
      {:ok true
       :schema :clj-surgeon.worktree-lifecycle-audit/v1
       :worktrees (mapv #(classify-target snapshot % nil) paths)})))

(defn validate-close-request
  "Validate a one-target, one-outcome close request without inferring policy."
  [request]
  (cond
    (not (exact-fields? request request-fields)) (refuse :invalid-close-request-fields)
    (not= close-request-schema (:schema request)) (refuse :invalid-close-request-schema)
    (not (absolute-clean-path? (:target request))) (refuse :invalid-worktree-path)
    (not (contains? outcomes (:outcome request))) (refuse :invalid-close-outcome)
    (not (contains? handoff-modes (:handoff request))) (refuse :invalid-handoff-mode)
    (not (map? (:evidence request))) (refuse :invalid-outcome-evidence)
    :else {:ok true :request request :handoff-mode (:handoff request)}))

(defn validate-prune-request
  "Validate the separate one-target stale-registration request."
  ;; @spec WTL-PRUNE-001
  [request]
  (cond
    (not= prune-request-fields (set (keys request)))
    (refuse :invalid-prune-request-fields)
    (not= prune-request-schema (:schema request))
    (refuse :invalid-prune-request-schema)
    (not (absolute-clean-path? (:target request)))
    (refuse :invalid-worktree-path)
    (not (map? (:preservation request)))
    (refuse :invalid-preservation-proof)
    :else {:ok true :request request}))

(def ^:private preservation-fields
  #{:kind :local-ref :remote :remote-url-sha256 :ref :object
    :peeled-object})

(defn validate-preservation-proof
  "Validate one exact remote preservation proof against the snapshot."
  ;; @spec WTL-PRUNE-003
  [snapshot target proof]
  (let [object-format (get-in snapshot [:repository :object-format])
        endpoint (or (:peeled-object proof) (:object proof))
        matching (filterv #(and (= (:remote proof) (:remote %))
                                (= (:remote-url-sha256 proof)
                                   (:remote-url-sha256 %))
                                (= (:ref proof) (:ref %))
                                (= (:object proof) (:object %))
                                (= (:peeled-object proof) (:peeled-object %)))
                          (get-in snapshot [:remotes :rows]))]
    (cond
      (not= preservation-fields (set (keys proof)))
      (refuse :invalid-preservation-fields)
      (not (contains? #{:branch-tip-on-remote :commit-on-remote}
                      (:kind proof)))
      (refuse :invalid-preservation-kind)
      (or (:detached target) (nil? (:branch target)))
      (refuse :detached-registration)
      (not= (:branch target) (:local-ref proof))
      (refuse :local-ref-mismatch)
      (not (and (string? (:remote proof))
                (not (str/blank? (:remote proof)))
                (string? (:ref proof))
                (str/starts-with? (:ref proof) "refs/")
                (hex? 64 (:remote-url-sha256 proof))
                (exact-oid? object-format (:object proof))
                (or (nil? (:peeled-object proof))
                    (exact-oid? object-format (:peeled-object proof)))))
      (refuse :invalid-preservation-identity)
      (not= 1 (count matching)) (refuse :ambiguous-remote-preservation)
      (and (= :branch-tip-on-remote (:kind proof))
           (or (:peeled-object proof)
               (not= (:head target) endpoint)))
      (refuse :remote-tip-mismatch)
      (and (= :commit-on-remote (:kind proof))
           (not (contains? (:ancestry snapshot) [(:head target) endpoint])))
      (refuse :remote-ancestry-missing)
      :else {:ok true :proof proof :endpoint endpoint})))

(defn compile-handoff
  "Compile the create-only owner handoff record while the agent lock is present."
  ;; @spec WTL-HAND-001 WTL-HAND-002
  [snapshot request owner nonce]
  (let [target (git-row snapshot (:target request))]
    (cond
      (not (:ok (validate-close-request request))) (validate-close-request request)
      (not (:locked target)) (refuse :active-owner-lock-required)
      (str/blank? owner) (refuse :handoff-owner-required)
      (str/blank? nonce) (refuse :handoff-nonce-required)
      :else
      {:schema handoff-schema
       :target (:path target)
       :head (:head target)
       :tree (:tree target)
       :owner owner
       :request-sha256 (sha256 (canonical-edn request))
       :nonce nonce})))

(defn handoff-unlock-command [handoff]
  ["git" "worktree" "unlock" (:target handoff)])

(defn expected-lifecycle-lease [plan]
  {:schema :clj-surgeon.worktree-lifecycle-lease/v1
   :plan-id (:plan-id plan)
   :plan-sha256 (:plan-sha256 plan)
   :target (or (get-in plan [:target :path-lexical])
               (get-in plan [:target :path]))
   :handoff-nonce (get-in plan [:handoff :nonce])})

(defn validate-lease-prestate [plan existing]
  ;; @spec WTL-HAND-003 WTL-HAND-004 WTL-HAND-005
  (cond
    (not= :absent (:lifecycle-lease-prestate plan))
    (refuse :invalid-lifecycle-lease-prestate)
    existing (refuse :lifecycle-lease-exists)
    :else {:ok true :expected (expected-lifecycle-lease plan)}))

(defn- remote-row-matches? [snapshot evidence]
  (some #(every? (fn [key] (= (get % key) (get evidence key)))
                 [:remote :remote-url-sha256 :ref :object :peeled-object])
        (get-in snapshot [:remotes :rows])))

(declare validate-raw-evidence)

(defn validate-negative-seal [seal]
  (let [allowed #{:schema :experiment :allowed-terminal-paths :raw-evidence}
        experiment (:experiment seal)
        paths (:allowed-terminal-paths seal)]
    (cond
      (not (exact-fields? seal allowed)) (refuse :invalid-negative-seal-fields)
      (not= negative-seal-schema (:schema seal)) (refuse :invalid-negative-seal-schema)
      (not (and (map? experiment)
                (string? (:commit experiment))
                (string? (:tree experiment))))
      (refuse :invalid-negative-experiment-identity)
      (not (and (vector? paths)
                (seq paths)
                (= (count paths) (count (distinct paths)))
                (every? #(and (string? %)
                              (not (str/starts-with? % "/"))
                              (not (str/includes? % "..")))
                        paths)))
      (refuse :invalid-negative-terminal-paths)
      :else (validate-raw-evidence (:raw-evidence seal)))))

(defn validate-raw-evidence [raw]
  (case (:kind raw)
    :none
    (if (= #{:kind} (set (keys raw)))
      {:ok true :raw-evidence raw}
      (refuse :invalid-no-raw-evidence))

    :archive
    (let [allowed #{:kind :receipt-ref :receipt-path :archive-locator
                    :archive-sha256}]
      (if (and (exact-fields? raw allowed)
               (str/starts-with? (:receipt-ref raw "") "refs/")
               (string? (:receipt-path raw))
               (not (str/starts-with? (:receipt-path raw) "/"))
               (not (str/includes? (:receipt-path raw) ".."))
               (string? (:archive-locator raw))
               (not (str/includes? (:archive-locator raw) ".."))
               (hex? 64 (:archive-sha256 raw)))
        {:ok true :raw-evidence raw}
        (refuse :invalid-raw-evidence-archive)))

    (refuse :invalid-raw-evidence-disposition)))

(defn validate-parking-revision [issue planned observed]
  (if (and (= (:revision issue) (:revision-before observed))
           (= planned observed)
           (= (inc (:revision-before observed)) (:revision-after observed)))
    {:ok true :revision (:revision-after observed)}
    (refuse :parked-issue-revision-drift)))

(defn- valid-future-expiry? [now expiry]
  (try
    (.isAfter (Instant/parse expiry) (Instant/parse now))
    (catch Exception _ false)))

(defn validate-outcome
  "Validate the declared terminal outcome against the captured target."
  ;; @spec WTL-SEAL-001 WTL-SEAL-002 WTL-SEAL-003 WTL-SEAL-004
  ;; @spec WTL-SEAL-005 WTL-SEAL-006
  [snapshot target request]
  (let [evidence (:evidence request)]
    (case (:outcome request)
      :landed
      (if (and (remote-row-matches? snapshot evidence)
               (contains? (:ancestry snapshot)
                          [(:head target) (:object evidence)]))
        {:ok true :outcome :landed :evidence evidence}
        (refuse :landed-evidence-not-proved))

      :negative-experiment
      (let [seal (:seal evidence)
            seal-result (validate-negative-seal seal)
            breadcrumb (:breadcrumb evidence)]
        (if (and (:ok seal-result)
                 (remote-row-matches? snapshot breadcrumb)
                 (:reachable evidence)
                 (= (:terminal-paths evidence) (:allowed-terminal-paths seal))
                 (= (:head target) (:object breadcrumb)))
          {:ok true :outcome :negative-experiment :evidence evidence}
          (refuse :negative-experiment-evidence-not-proved)))

      :parked
      (let [issue (:issue evidence)
            upstream (:upstream evidence)
            next-action (:next-action evidence)]
        (if (and (= :clean (:status target))
                 (string? (:branch target))
                 (= (:head target) (:object upstream))
                 (remote-row-matches? snapshot upstream)
                 (= :open (:status issue))
                 (not (str/blank? (:owner issue)))
                 (string? next-action)
                 (not (str/blank? next-action))
                 (not (str/includes? next-action "\n"))
                 (<= (count (.getBytes next-action StandardCharsets/UTF_8)) 512)
                 (valid-future-expiry? (:now evidence) (:expiry evidence)))
          {:ok true :outcome :parked :evidence evidence}
          (refuse :parked-evidence-not-proved)))

      (refuse :invalid-close-outcome))))

(defn- recursive-keys [value]
  (cond
    (map? value) (into (set (keys value)) (mapcat recursive-keys (vals value)))
    (sequential? value) (into #{} (mapcat recursive-keys value))
    :else #{}))

(defn- privacy-safe? [value]
  (empty? (set/intersection forbidden-record-keys (recursive-keys value))))

(defn- supacode-plan-state [snapshot path]
  (if-let [row (supacode-row snapshot path)]
    {:id (:id row) :initial (:status row) :terminal :archived}
    {:id nil :initial :absent :terminal :absent}))

(defn- target-fingerprint [snapshot target]
  {:path (:path target)
   :head (:head target)
   :tree (:tree target)
   :branch (:branch target)
   :detached (:detached target)
   :status (:status target)
   :locked (:locked target)
   :removal-preflight (:removal-preflight target)
   :supacode (supacode-plan-state snapshot (:path target))})

(defn- seal-plan [plan]
  (let [plan-sha (sha256 (canonical-edn (dissoc plan :plan-sha256)))]
    (assoc plan :plan-sha256 plan-sha)))

(defn compile-prune-plan
  "Compile one exact outcome-free stale-registration plan."
  ;; @spec WTL-PRUNE-004 WTL-PRUNE-005
  [snapshot request controller-identity plan-id]
  (let [snapshot-result (validate-snapshot snapshot)
        request-result (validate-prune-request request)
        target-rows (filterv #(= (:target request) (row-path %))
                             (:git-worktrees snapshot))
        target (first target-rows)
        supacode-rows (filterv #(= (:target request) (row-path %))
                               (get-in snapshot [:supacode :worktrees]))
        proof-result (when target
                       (validate-preservation-proof
                         snapshot target (:preservation request)))
        classification (when target
                         (classify-target snapshot (:target request) nil))]
    (cond
      (not (:ok snapshot-result)) snapshot-result
      (not (:ok request-result)) request-result
      (nil? target) (refuse :target-not-registered)
      (not= 1 (count target-rows)) (refuse :ambiguous-target-registration)
      (not= :absent (:path-state target)) (refuse :target-path-not-absent)
      (:detached target) (refuse :detached-registration)
      (:locked target) (refuse :target-locked)
      (not= :missing-prunable (:classification classification))
      (refuse :target-not-prune-eligible)
      (seq (get-in snapshot [:handoffs (:target request)]))
      (refuse :handoff-exists)
      (seq (get-in snapshot [:lifecycle-leases (:target request)]))
      (refuse :lifecycle-lease-exists)
      (= (:target request) (:controller-worktree snapshot))
      (refuse :controller-target-collision)
      (not= true (get-in snapshot [:supacode :available]))
      (refuse :supacode-unavailable)
      (> (count supacode-rows) 1) (refuse :ambiguous-supacode-identity)
      (and (= 1 (count supacode-rows))
           (let [row (first supacode-rows)]
             (or (:focused row)
                 (= :pinned (:status row))
                 (= :main (:status row))
                 (not= false (:live row)))))
      (refuse :active-supacode-identity)
      (not (:ok proof-result)) proof-result
      (not (and (string? plan-id) (not (str/blank? plan-id))))
      (refuse :plan-id-required)
      (not (valid-prune-repository? (:repository snapshot)))
      (refuse :invalid-prune-repository)
      (not (valid-prune-controller?
             (get-in snapshot [:repository :object-format])
             controller-identity))
      (refuse :invalid-prune-controller)
      (not (privacy-safe? controller-identity))
      (refuse :private-plan-data-refused)
      :else
      (let [supacode-row (first supacode-rows)
            supacode-state (if supacode-row
                             {:id (:id supacode-row)
                              :initial (:status supacode-row)
                              :terminal :archived}
                             {:id nil :initial :absent :terminal :absent})
            base {:schema prune-plan-schema
                  :plan-id plan-id
                  :operation :prune-missing-registration
                  :repository (:repository snapshot)
                  :controller controller-identity
                  :captured-at (:captured-at snapshot)
                  :target target
                  :preservation (:proof proof-result)
                  :handoff :not-applicable
                  :lifecycle-lease-prestate :absent
                  :expected-lifecycle-lease
                  {:schema :clj-surgeon.worktree-lifecycle-lease/v1
                   :plan-id plan-id
                   :target (:target request)
                   :handoff-nonce nil}
                  :supacode supacode-state}
            plan (seal-plan base)]
        {:ok true :plan plan :plan-sha256 (:plan-sha256 plan)}))))

(defn validate-prune-plan
  "Validate the distinct prune plan without weakening close-plan fields."
  ;; @spec WTL-PRUNE-005 WTL-PRUNE-010
  [plan]
  (let [expected (sha256 (canonical-edn (dissoc plan :plan-sha256)))
        lease (:expected-lifecycle-lease plan)
        target (:target plan)
        proof (:preservation plan)
        object-format (get-in plan [:repository :object-format])
        supacode (:supacode plan)]
    (cond
      (not= prune-plan-fields (set (keys plan)))
      (refuse :invalid-prune-plan-fields)
      (not= prune-plan-schema (:schema plan))
      (refuse :invalid-prune-plan-schema)
      (not= :prune-missing-registration (:operation plan))
      (refuse :invalid-operation-kind)
      (not= :not-applicable (:handoff plan))
      (refuse :invalid-prune-handoff)
      (not= :absent (:lifecycle-lease-prestate plan))
      (refuse :invalid-lifecycle-lease-prestate)
      (not (valid-prune-repository? (:repository plan)))
      (refuse :invalid-prune-repository)
      (not (valid-prune-controller? object-format (:controller plan)))
      (refuse :invalid-prune-controller)
      (not (:ok (validate-git-worktree object-format target)))
      (refuse :invalid-prune-target)
      (not (and (= :absent (:path-state target))
                (= :not-applicable (:status target))
                (= :not-applicable (:removal-preflight target))
                (not (:detached target))
                (not (:locked target))
                (string? (:branch target))))
      (refuse :invalid-prune-target)
      (not= preservation-fields (set (keys proof)))
      (refuse :invalid-preservation-fields)
      (not= (:branch target) (:local-ref proof))
      (refuse :local-ref-mismatch)
      (not (contains? #{:branch-tip-on-remote :commit-on-remote}
                      (:kind proof)))
      (refuse :invalid-preservation-kind)
      (not (and (string? (:remote proof))
                (not (str/blank? (:remote proof)))
                (string? (:ref proof))
                (str/starts-with? (:ref proof) "refs/")
                (hex? 64 (:remote-url-sha256 proof))
                (exact-oid? object-format (:object proof))
                (or (nil? (:peeled-object proof))
                    (exact-oid? object-format (:peeled-object proof)))))
      (refuse :invalid-preservation-identity)
      (and (= :branch-tip-on-remote (:kind proof))
           (or (:peeled-object proof)
               (not= (:head target) (:object proof))))
      (refuse :remote-tip-mismatch)
      (not= #{:schema :plan-id :target :handoff-nonce}
            (set (keys lease)))
      (refuse :invalid-expected-lifecycle-lease)
      (not= :clj-surgeon.worktree-lifecycle-lease/v1 (:schema lease))
      (refuse :invalid-expected-lifecycle-lease)
      (not= (get-in plan [:target :path-lexical]) (:target lease))
      (refuse :invalid-expected-lifecycle-lease)
      (not= (:plan-id plan) (:plan-id lease))
      (refuse :invalid-expected-lifecycle-lease)
      (some? (:handoff-nonce lease))
      (refuse :invalid-expected-lifecycle-lease)
      (not= #{:id :initial :terminal} (set (keys supacode)))
      (refuse :invalid-prune-supacode-state)
      (not (or (= {:id nil :initial :absent :terminal :absent} supacode)
               (and (string? (:id supacode))
                    (= :archived (:terminal supacode))
                    (contains? #{:unpinned :archived} (:initial supacode)))))
      (refuse :invalid-prune-supacode-state)
      (not (hex? 64 (:plan-sha256 plan))) (refuse :invalid-plan-hash)
      (not= expected (:plan-sha256 plan)) (refuse :plan-hash-mismatch)
      (not (privacy-safe? plan)) (refuse :private-plan-data-refused)
      :else {:ok true :plan plan})))

(defn compile-plan
  "Compile one exact reviewed close plan; perform no external mutation."
  ;; @spec WTL-PLAN-001 WTL-PLAN-002 WTL-PLAN-003 WTL-PLAN-004
  [snapshot request controller-identity plan-id]
  (let [snapshot-result (validate-snapshot snapshot)
        request-result (validate-close-request request)
        target (git-row snapshot (:target request))
        outcome-result (when target (validate-outcome snapshot target request))
        handoff (get-in snapshot [:handoffs (:target request)])]
    (cond
      (not (:ok snapshot-result)) snapshot-result
      (not (:ok request-result)) request-result
      (nil? target) (refuse :target-not-registered)
      (contains? #{:active :dirty-blocked :missing-prunable}
                 (:classification (classify-target snapshot (:target request) nil)))
      (refuse :target-not-plan-eligible)
      (and (= :agent (:handoff request)) (nil? handoff))
      (refuse :handoff-required)
      (get-in snapshot [:lifecycle-leases (:target request)])
      (refuse :lifecycle-lease-exists)
      (not (:ok outcome-result)) outcome-result
      (not (and (string? plan-id) (not (str/blank? plan-id))))
      (refuse :plan-id-required)
      (false? (:clean controller-identity))
      (refuse :controller-worktree-not-clean)
      (not (privacy-safe? controller-identity))
      (refuse :private-plan-data-refused)
      :else
      (let [base {:schema plan-schema
                  :plan-id plan-id
                  :repository (:repository snapshot)
                  :controller controller-identity
                  :captured-at (:captured-at snapshot)
                  :target (target-fingerprint snapshot target)
                  :outcome (:outcome request)
                  :outcome-evidence (:evidence outcome-result)
                  :handoff (if (= :legacy (:handoff request))
                             {:mode :legacy}
                             handoff)
                  :lifecycle-lease-prestate :absent
                  :expected-lifecycle-lease
                  {:schema :clj-surgeon.worktree-lifecycle-lease/v1
                   :plan-id plan-id
                   :target (:target request)
                   :handoff-nonce (:nonce handoff)}
                  :supacode (supacode-plan-state snapshot (:target request))}
            plan (seal-plan base)]
        {:ok true :plan plan :plan-sha256 (:plan-sha256 plan)}))))

(defn validate-plan [plan]
  (let [expected (sha256 (canonical-edn (dissoc plan :plan-sha256)))
        expected-lease-template
        {:schema :clj-surgeon.worktree-lifecycle-lease/v1
         :plan-id (:plan-id plan)
         :target (get-in plan [:target :path])
         :handoff-nonce (get-in plan [:handoff :nonce])}]
    (cond
      (not (exact-fields? plan plan-fields)) (refuse :invalid-plan-fields)
      (not= plan-schema (:schema plan)) (refuse :invalid-plan-schema)
      (not (hex? 64 (:plan-sha256 plan))) (refuse :invalid-plan-hash)
      (not= expected (:plan-sha256 plan)) (refuse :plan-hash-mismatch)
      (not= expected-lease-template (:expected-lifecycle-lease plan))
      (refuse :invalid-expected-lifecycle-lease)
      (not (privacy-safe? plan)) (refuse :private-plan-data-refused)
      :else {:ok true :plan plan})))

(defn validate-apply-request [request]
  ;; @spec WTL-PLAN-005 WTL-PLAN-006
  (if (and (= #{:plan :apply} (set (keys request)))
           (absolute-clean-path? (:plan request))
           (= "1" (:apply request)))
    {:ok true :request request}
    (refuse :invalid-apply-request)))

(defn validate-terminal-replay [postconditions]
  (cond
    (:target-present postconditions) (refuse :path-reused)
    (:registration-present postconditions) (refuse :registration-still-present)
    :else {:ok true :terminal true}))

(defn new-plan-id [] (str (UUID/randomUUID)))

(defn compile-receipt [plan postconditions]
  (if-not (:ok (validate-plan plan))
    (validate-plan plan)
    {:ok true
     :receipt {:schema receipt-schema
               :plan-id (:plan-id plan)
               :plan-sha256 (:plan-sha256 plan)
               :outcome (:outcome plan)
               :target (:target plan)
               :postconditions postconditions
               :branch-retained true
               :next-action :none}}))

(defn compile-prune-receipt
  "Compile one immutable, meaning-free stale-registration receipt."
  ;; @spec WTL-PRUNE-007 WTL-PRUNE-008
  [plan effect-observed postconditions]
  (let [validation (validate-prune-plan plan)]
    (cond
      (not (:ok validation)) validation
      (not= prune-postcondition-fields (set (keys postconditions)))
      (refuse :invalid-prune-postconditions)
      (not (privacy-safe? postconditions))
      (refuse :private-receipt-data-refused)
      (not (contains? #{:controller :controller-or-external}
                      effect-observed))
      (refuse :invalid-effect-observed)
      (:target-present postconditions) (refuse :path-reused)
      (:registration-present postconditions)
      (refuse :registration-still-present)
      (not (:branch-unchanged postconditions)) (refuse :branch-drift)
      (not (:preservation-unchanged postconditions))
      (refuse :preservation-drift)
      (not= (get-in plan [:supacode :terminal])
            (:supacode-state postconditions))
      (refuse :supacode-terminal-state-mismatch)
      :else
      {:ok true
       :receipt {:schema prune-receipt-schema
                 :plan-id (:plan-id plan)
                 :plan-sha256 (:plan-sha256 plan)
                 :operation (:operation plan)
                 :effect :registration-pruned
                 :effect-observed effect-observed
                 :before-registration (:target plan)
                 :preservation (:preservation plan)
                 :postconditions postconditions
                 :branch-retained true
                 :next-action :none}})))
