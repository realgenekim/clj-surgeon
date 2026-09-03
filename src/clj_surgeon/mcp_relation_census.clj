(ns clj-surgeon.mcp-relation-census
  "relation_census: the finder for event-sourced Clojure repositories.

   It reads. It never writes. It reports, per collection write inside a
   `defmethod fold-event` arm, whether that write goes through a known identity
   door, targets a set, is dominated by a recognised guard on the written
   value's identity, is unguarded (`:raw`), or cannot be decided (`:unknown`,
   with a reason). It LOCATES review work; it does not prove idempotency and it
   is not an enforcement gate."
  (:require
   [cheshire.core :as json]
   [clj-surgeon.census-discovery :as discovery]
   [clj-surgeon.census-pool :as census-pool]
   [clj-surgeon.mcp-operation :as mcp-operation]
   [clj-surgeon.mcp-paths :as mcp-paths]
   [clj-surgeon.mcp-runtime :as runtime]
   [clj-surgeon.mcp-workspace :as workspace]
   [clj-surgeon.relation-census :as census]
   [clojure.string :as str])
  (:import
   (java.nio.file Files Path)))

(def max-scanned-files census/max-scanned-files)
(def max-source-bytes census/max-source-bytes)
(def max-receipt-bytes 4096)
(def max-listed-sites 12)
(def max-listed-files census/max-listed-files)
(def max-listed-unrecognised 5)

;; @spec MCP-OP-CENSUS-009
;; @spec MCP-OP-CENSUS-026
(def census-tool-description
  (str
    "Census every collection write inside `defmethod fold-event` arms and "
    "classify each one as :door (routed through a known identity door), :set "
    "(the target is a set), :guarded (a recognised guard on the written "
    "value's identity dominates the write with the right polarity), :raw (no "
    "recognised guard dominates it), or :unknown with a reason "
    "(:helper-mediated-guard, :polarity, :unsupported-container, "
    ":unresolved-target). A :raw site is the vulnerability; an :unknown site "
    "is review work this version declines to decide. Omit files to census "
    "every file in the workspace that defines arms; pass files for an exact "
    "list. doors extends the default identity doors "
    "(conj-once, cons-once, upsert-by, conj-distinct-by, cons-distinct-by). "
    "The plan phase is parallel and the answer is pool-size independent. "
    "This verb reads only; it writes nothing and it is not an enforcement "
    "gate: it locates review work and never claims to prove idempotency. It is "
    "the one clj-surgeon tool that enumerates the workspace tree, so point it "
    "at the workspace you mean."))

(def census-tool-schema
  {:type "object"
   :additionalProperties false
   :properties
   {"workspace_root" {:type "string"}
    "files" {:type "array"
             :items {:type "string"}
             :minItems 1
             :maxItems 512}
    "doors" {:type "array"
             :items {:type "string"}
             :maxItems 32}
    "pool_size" {:type "integer" :minimum 1 :maximum 64}}})

;; @spec MCP-OP-SCHEMA-001
(def census-output-schema
  {:type "object"
   :additionalProperties true
   :properties {"ok" {:type "boolean"}
                "operation" {:type "string"}
                "census_version" {:type "integer"}
                "read_complete" {:type "boolean"}
                "files" {:type "integer"}
                "arms" {:type "integer"}
                "sites" {:type "integer"}
                "outside_arms" {:type "integer"}
                "counts" {:type "object"}
                "by_file" {:type "object"}
                "raw" {:type "array"}
                "guarded" {:type "array"}
                "unknown" {:type "array"}
                "pool_size" {:type "integer"}
                "phases_elapsed_ms" {:type "object"}
                "next_action" {:type "string"}
                "next_call" {:type "object"}
                "error_type" {:type "string"}
                "elapsed_ms" {:type "number" :minimum 0}}
   :required ["ok" "operation" "elapsed_ms"]})

(def census-annotations
  {:title "Relation Census"
   :read-only true
   :destructive false
   :idempotent true
   :open-world false
   :return-direct false})

(def ^:private runtime-config runtime/tool-config)

(defn init!
  "Set the live relation_census tool configuration. Passing nil disarms it."
  [config]
  (reset! runtime-config
          (when config
            (assoc config :workspace-router (workspace/router config)))))

;; ---------------------------------------------------------------------------
;; Refusals
;; ---------------------------------------------------------------------------

(defn- refusal
  "A typed refusal, with a continuation only when there IS one.

   `next-call` nil publishes NO `next_call` key at all. A null continuation is
   not a smaller promise than a real one, it is a field the caller must
   interpret; the refusal says what it can offer instead, in `remedy`."
  [error-type message next-call & [data]]
  (merge (cond-> {:ok false
                  :operation "relation-census"
                  :census_version census/census-version
                  :error_type (name error-type)
                  :error message
                  :source_unchanged true
                  :read_complete false}
           (some? next-call) (assoc :next_call next-call))
         data))

;; ---------------------------------------------------------------------------
;; Parameter validation (server-side; the advertised schema is only a hint)
;; ---------------------------------------------------------------------------

;; The accepted field sets live in `clj-surgeon.relation-census`
;; (`mcp-census-fields` / `mcp-routing-fields`), next to the shared refusal
;; table, because the CLI entrance must be able to read them and cannot
;; require this namespace (claypoole does not load under babashka). The
;; unknown-field row of that table reads them directly, so this namespace no
;; longer keeps aliases of its own: a second name for one set is a second
;; place for the two to drift.
;;
;; `workspace_root` is a ROUTING field, not a census field: it is validated
;; and canonicalised later, by `workspace/resolve-request`, and this pass
;; must not treat its presence as an unknown field nor its value as anything
;; to check — checking it here would mean resolving it, which is exactly the
;; filesystem work this pass runs before.

;; @spec MCP-OP-CENSUS-016
;; @spec MCP-OP-CENSUS-029
(defn validate-request-shape
  "Validate relation_census parameters, pure and before ANY filesystem work.

   This runs before `workspace/resolve-request`, not merely before the
   scanned sources are read: `workspace_root` canonicalisation is itself a
   stat, so a malformed `doors`, `files`, or `pool_size` must refuse before
   that resolver ever touches the filesystem, not only before this tool
   reads a source. Sol's round-eight finding was exactly this — `doors=[1]`
   was refused only AFTER `workspace/resolve-request` had already `stat`ed
   the workspace root.

   The JSON schema this tool advertises is a hint to a well-behaved caller. A
   malformed call reaches the server anyway, so every bound the schema states is
   re-checked here and refused with a typed reason and an executable next_call.

   The checks run in ONE fixed order — `doors`, then `files`, then
   `pool_size` — so that a malformed `doors` entry refuses before a request
   that also names an oversized `files` list or an unresolvable
   `workspace_root` ever reaches those checks or performs a single stat.

   TYPES are re-checked too, not only ranges. `coerce-pool-size` reads decimal
   digits because the CLI hands every value over as a string; the wire does not,
   so a JSON string, float, boolean, null or array for `pool_size` is refused
   here rather than quietly coerced into a pool the schema never advertised.
   Every `doors` entry must likewise be the JSON string the schema declares:
   a non-string entry is refused here, by index and value, before it can
   reach the oversized-source branch of execute-in-context!, which copies
   `doors` into its next_call unchanged and would otherwise hand back a
   continuation the tool's own schema rejects.

   The `next_call` this pass computes CARRIES the caller's `workspace_root`
   verbatim whenever the caller supplied one. No canonical value exists at
   this point — canonicalising is the filesystem work this pass runs before —
   so the caller's own string is what there is, and it is what must travel:
   a continuation may narrow WHAT is censused, it may never change WHERE.
   Sol's round-nine finding was this exact defect, introduced by round nine's
   own fix: a refusal targeting a fixture workspace handed back
   `{tool, pool_size: 8}`, and replaying that verbatim censused the SERVER's
   default root and reported success. Carrying an unvalidated value through
   is not a promise about the value; it either routes to the workspace the
   caller meant or refuses on that same value, and both are honest. Silently
   censusing a different tree is neither.

   When `workspace_root` is present but is NOT a string, there is no
   continuation to compute: the value cannot be carried into a `next_call`
   the published schema would accept, and a `next_call` without it targets a
   different tree. So this pass publishes no `next_call` at all and a
   `remedy` saying why, which is what MCP-OP-CENSUS-014 already requires of
   every refusal that can compute no narrower call."
  [params]
  (let [asked-root (:workspace_root params)
        ;; A routing field this pass deliberately does not validate — but it
        ;; must be CARRIABLE, and a non-string is not: the schema this tool
        ;; publishes declares workspace_root a string, so copying a number or
        ;; an object into the continuation would hand back a call the tool's
        ;; own schema rejects.
        carriable? (or (not (contains? params :workspace_root))
                       (string? asked-root))
        next-call (when carriable?
                    (cond-> {:tool "relation_census" :pool_size 8}
                      (string? asked-root) (assoc :workspace_root asked-root)))
        ;; `uncomputable` is a row that can offer no continuation AT ALL,
        ;; whatever `workspace_root` said — the not-decodable row is the
        ;; first: what it would carry is the corrupted spelling of a path
        ;; whose real bytes are gone, which names a different directory or
        ;; none. It gets the row's own remedy and no `next_call` key, exactly
        ;; as MCP-OP-CENSUS-014 requires of every refusal from which no
        ;; narrower call can be computed.
        refuse (fn [reason message data & [uncomputable]]
                 (refusal :invalid-mcp-request message
                          (when-not uncomputable next-call)
                          (merge {:reason (name reason)}
                                 (when uncomputable {:remedy uncomputable})
                                 (when (and (not uncomputable) (not carriable?))
                                   {:remedy
                                    (str "workspace_root must be a JSON "
                                         "string; the value this request "
                                         "supplied cannot be carried into a "
                                         "continuation, and a continuation "
                                         "without it would census a "
                                         "different tree, so none is "
                                         "offered: retry with workspace_root "
                                         "naming an existing absolute "
                                         "directory, or omit it to census "
                                         "the server's workspace.")})
                                 data)))
        ;; The shape questions AND THEIR ORDER are the shared table's, not
        ;; this function's. Sol's round-eleven item 3: this validator read the
        ;; table's PREDICATES through `shape-violated?` but kept its own
        ;; `cond` for the ORDER, so moving `files` before `doors` in the table
        ;; changed the CLI's refusal and left the tool's unmoved — and the
        ;; parity witness, which enumerates predicates, stayed green while the
        ;; two entrances disagreed. MCP-OP-CENSUS-029 states the order as a
        ;; requirement, so it is now a fact about the table: this walks the
        ;; rows in the table's order and stops at the first one violated.
        ;;
        ;; The per-branch PAYLOADS the `cond` existed to carry did not go
        ;; away; they moved into the rows as `:mcp-message` and `:mcp-data`
        ;; formatters, beside the predicate that decides them and the name
        ;; each entrance publishes. A row this entrance cannot express carries
        ;; no `:mcp` keyword and is skipped, and that is the ONLY reason a row
        ;; is skipped. Sol's round-twelve item 10 removed the other one: the
        ;; door-vocabulary row used to declare a PHASE of its own here, so
        ;; this walk passed over a row the CLI's walk applied and the two
        ;; entrances chose different first refusals for one request. A table
        ;; that both entrances read in the same order, minus the rows one of
        ;; them silently skips, is not one table.
        req (census/normalise-request :mcp params)
        violated (some (fn [rule]
                         (when (and (keyword? (:mcp rule))
                                    (not ((:predicate rule) req)))
                           rule))
                       (census/shape-rules))
        pool-size (:pool_size params)]
    (if violated
      (refuse (:mcp violated)
              ((:mcp-message violated) req)
              ((:mcp-data violated) req)
              (when (= :uncomputable (:mcp-fix violated))
                ((:mcp-remedy violated) req)))
      ;; Every reachable non-integer refuses above, so what is left is either
      ;; absent or an in-range integer. Sol's round-eleven item 9 found the
      ;; old `:else` block's second `pool-size-not-an-integer` branch had no
      ;; constructible request; walking the table deletes it rather than
      ;; leaving dead code behind a comment claiming it is reachable.
      {:ok true
       :params (cond-> params
                 (some? pool-size)
                 (assoc :pool_size (:size (census/coerce-pool-size pool-size))))})))

;; ---------------------------------------------------------------------------
;; Discovery
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-CENSUS-018
;; @spec MCP-OP-CENSUS-027
;; @spec MCP-OP-CENSUS-028
;; @spec MCP-OP-CENSUS-032
(defn- candidate-files
  "Project-relative Clojure sources under one canonical root, bounded.

   Discovery is not implemented here. It is the shared
   `clj-surgeon.census-discovery` kernel, which the CLI op calls too: root
   canonicalisation, root confinement, skip-directory pruning, the byte cap,
   and the scanned-file ceiling are ONE implementation, so the two entrances
   cannot answer the same tree differently."
  [^Path root]
  (discovery/discover root))

;; @spec MCP-OP-CENSUS-014
;; @spec MCP-OP-CENSUS-017
(defn- oversized-among
  "The named sources above the byte cap, in the order they were named.

   `collect-inputs` stops at the FIRST oversized source, which is right when
   reading: nothing after it needs to be read. It is wrong when COMPUTING a
   continuation, because a request that removes only the source the reader
   tripped on refuses again on the next one. This stats the named list and
   reads nothing, and it runs only when a refusal is already being built."
  [root relatives]
  (into []
        (filter (fn [relative]
                  (let [resolved (mcp-paths/resolve-source-path root relative)]
                    (boolean
                      (and (:ok resolved)
                           (try
                             (> (Files/size ^Path (:canonical resolved))
                                census/max-source-bytes)
                             (catch Throwable _ false)))))))
        relatives))

;; @spec MCP-OP-CENSUS-017
;; @spec MCP-OP-CENSUS-030
(defn collect-inputs
  "Read each scanned path through the project fence, retaining only arm sources.

   The census needs the text of a file only if that file defines arms, so each
   source is tested as it is read and dropped when it does not. Nothing but the
   arm-defining sources is ever held at once, and a source above
   `max-source-bytes` is refused rather than read.

   The path SET is canonicalised as it is read: a caller may name one source
   many times, by the same string or by different strings that resolve to one
   real path, and each repeat is collapsed and counted in `:duplicates` instead
   of multiplying every figure in the receipt."
  ([root relatives] (collect-inputs root relatives {}))
  ([root relatives {:keys [declared?]}]
   (reduce
     (fn [acc relative]
       (let [resolved (mcp-paths/resolve-source-path root relative)]
         (cond
           (not (:ok resolved))
           (reduced (assoc acc :refusal resolved :file relative))

           (contains? (:seen acc) (str (:canonical resolved)))
           (update acc :duplicates inc)

           (> (Files/size ^Path (:canonical resolved)) census/max-source-bytes)
           (reduced (assoc acc
                           :oversized relative
                           :bytes (Files/size ^Path (:canonical resolved))))

           :else
           (let [source (slurp (:path resolved))
                 acc (-> acc
                         (update :seen conj (str (:canonical resolved)))
                         (update :read inc))]
             (if (census/defines-arms? source)
               (update acc :inputs conj {:file relative :source source})
               ;; Not an arm file: its text is dropped here. Only its top-level
               ;; names survive, and only when a caller's doors need checking
               ;; against them.
               (cond-> acc
                 declared?
                 (update :declared into
                         (census/source-declared-names source))))))))
     {:inputs [] :read 0 :declared #{} :seen #{} :duplicates 0}
     relatives)))

;; ---------------------------------------------------------------------------
;; Receipt
;; ---------------------------------------------------------------------------

(defn- public-site
  [site]
  (into {}
        (remove (comp nil? val))
        {:file (:file site)
         :line (:line site)
         :arm (:arm site)
         :write (:write site)
         :target (:target site)
         :value (:value site)
         :identity (:identity site)
         :guard (:guard site)
         :guard_line (:guard-line site)
         :polarity (some-> (:polarity site) name)
         :reason (some-> (:reason site) name)
         :detail (:detail site)}))

(defn- receipt-bytes
  [receipt]
  (count (.getBytes ^String (json/generate-string receipt) "UTF-8")))

(defn- longest-list-key
  [receipt]
  (->> [:raw :unknown :guarded]
       (sort-by #(- (count (get receipt % []))))
       (filter #(seq (get receipt % [])))
       first))

(defn- trim-once
  "Drop the cheapest remaining evidence, or nil when nothing is left to drop.

   Unmodelled-call examples go first (their count carries the signal), then
   listed sites, and `by_file` last: it is the summary a reviewer can act on
   without the site list, but with long project paths it alone overruns the
   budget, so it must be trimmable too."
  [receipt]
  (cond
    (seq (get-in receipt [:unrecognised_calls :examples]))
    (update-in receipt [:unrecognised_calls :examples] #(vec (butlast %)))

    (longest-list-key receipt)
    (update receipt (longest-list-key receipt) #(vec (butlast %)))

    (seq (:by_file receipt))
    (update receipt :by_file #(dissoc % (last (keys %))))

    :else nil))

(def ^:private receipt-envelope-allowance
  "Bytes the receipt gains after it is bounded: the operation clock's
   `elapsed_ms` and the JSON around the workspace root."
  64)

;; @spec MCP-OP-CENSUS-022
(defn- bound-receipt
  "Trim listed evidence, then per-file counts, until the receipt fits.

   `reserved` is the size of what the adapter and the operation clock append
   after this returns; the budget must hold for the receipt that is PUBLISHED,
   not for the one that is built."
  ([receipt] (bound-receipt receipt 0))
  ([receipt reserved]
   (loop [receipt receipt]
     (if (<= (+ (receipt-bytes receipt) reserved) max-receipt-bytes)
       receipt
       (if-let [trimmed (trim-once receipt)]
         (recur (assoc trimmed :receipt_truncated true))
         receipt)))))

(defn- listed
  [sites class-key]
  (let [matching (filterv #(= class-key (:class %)) sites)]
    (mapv public-site (take max-listed-sites matching))))

;; @spec MCP-OP-CENSUS-025
(defn- next-action
  [counts unrecognised]
  (cond
    (pos? (:raw counts 0))
    "review the raw sites: each is a collection write in a fold arm with no dominating recognised guard"

    (pos? (:unknown counts 0))
    "review the unknown sites: this census version declines to decide them; the reason names why"

    (pos? (:count unrecognised 0))
    (str "no site is unguarded, but " (:count unrecognised)
         " call(s) inside arms are not modelled by this census version ("
         (str/join ", " (take 3 (map :call (:examples unrecognised))))
         "): a write behind one of them is not a site here")

    :else "none"))

;; @spec MCP-OP-CENSUS-013
;; @spec MCP-OP-CENSUS-028
;; @spec MCP-OP-CENSUS-030
(defn- build-receipt
  [{:keys [merged pool-size requested-pool phases facts oversized reserved]}]
  (let [counts (:counts merged)
        sites (:all-sites merged)
        unrecognised (census/unrecognised-summary
                       (:unrecognised merged) max-listed-unrecognised)]
    (bound-receipt
      (into {}
            (remove (comp nil? val))
            ;; The discovery facts are the SAME facts every refusal publishes,
            ;; built by the same kernel: a success is not a different receipt
            ;; shape with evidence rules of its own.
            (merge
              {:ok true
               :operation "relation-census"
               :census_version census/census-version
               ;; A census that skipped a source in scope is not complete, and
               ;; says so in the same receipt that names what it skipped.
               :read_complete (empty? oversized)
               :files (:files merged)
               :arms (:arms merged)
               :sites (:sites merged)
               :outside_arms (:outside-arms merged)
               :counts counts
               :by_file (into (sorted-map)
                              (take max-listed-files
                                    (map (fn [[f v]]
                                           [f (assoc (:counts v)
                                                     :arms (:arms v)
                                                     :sites (:sites v))])
                                         (:by-file merged))))
               :raw (listed sites :raw)
               :guarded (listed sites :guarded)
               :unknown (listed sites :unknown)
               :pool_size pool-size
               :pool_size_requested (when (and requested-pool
                                               (> requested-pool pool-size))
                                      requested-pool)
               :phases_elapsed_ms phases
               :unrecognised_calls unrecognised
               :next_action (next-action counts unrecognised)}
              facts))
      (or reserved 0))))

;; ---------------------------------------------------------------------------
;; Execution
;; ---------------------------------------------------------------------------

;; @spec MCP-OP-CENSUS-014
;; @spec MCP-OP-CENSUS-027
;; @spec MCP-OP-CENSUS-033
(defn- narrowing-continuation
  "The narrowing one bound refusal would carry, and what carrying it MEASURES.

   THE ONE PLACE both bound refusals turn a chosen subtree into a continuation.
   `:next-call` is nil in two DIFFERENT situations, and the whole point of this
   map is that the caller can tell them apart: the walk knew no fitting subtree
   (`:narrower` nil), or it knew one the wire cannot carry (`:narrower` set,
   `:bytes` over the ceiling).

   The shared BYTE predicate decides, not `count`: a JSON request body is bytes
   on the wire, and a workspace path outside ASCII costs more of them than it
   has characters (Sol's round-twelve item 3)."
  [canonical narrower]
  (let [candidate (when narrower
                    {:tool "relation_census"
                     :workspace_root (str canonical "/" narrower)})
        rendered (when candidate (json/generate-string candidate))]
    {:narrower narrower
     :candidate candidate
     :bytes (when rendered (census/utf8-byte-count rendered))
     :next-call (when (and rendered (census/within-next-call-bytes? rendered))
                  candidate)}))

;; @spec MCP-OP-CENSUS-014
(defn- narrowing-overflow-remedy
  "Why a narrowing the walk DID find was not handed back, with what it measured.

   Sol's round-thirteen item 3: both bound refusals fell through to the
   NO-SUBTREE remedy whenever the byte ceiling dropped their continuation, and
   told the caller that no subtree the walk finished was known to fit — about a
   walk that had found one and measured it at 891 bytes. A refusal that names a
   bound without naming the value it compared against leaves the caller to
   guess how much shorter is short enough; one that reports the wrong REASON
   leaves them narrowing the wrong thing. This says only what is true: the
   subtree is known, it is named, and it is the PATH to it that does not fit."
  [narrower bytes]
  (str "The walk did find a subtree that fits under the bound — " narrower
       " — but the smallest narrowing this refusal can offer renders as "
       bytes " UTF-8 bytes, over the " census/max-next-call-bytes
       "-byte ceiling a continuation must fit, so none is offered. The SUBTREE "
       "is not the problem, the length of the path to it is: narrow the "
       "request yourself by setting workspace_root to " narrower
       " under the workspace just refused — reaching it by a shorter path if "
       "you can — or name at most " census/max-requested-files
       " sources with files."))

;; @spec MCP-OP-CENSUS-027
(defn- ceiling-refusal
  "Refuse a tree that holds more candidate sources than the census may read.

   The caller gets the ceiling, the count that fits, the count the walk had
   observed when it stopped (a lower bound, because the walk stops rather than
   enumerating the rest), and a next_call COMPUTED from the walk's own
   per-directory aggregates — the largest subtree the walk finished that fits
   under the ceiling. A placeholder in an argument position is not a
   continuation; when nothing is known to fit, the caller is told so and told
   why, and gets no next_call at all."
  [discovered canonical facts]
  (let [{:keys [narrower bytes next-call]}
        (narrowing-continuation canonical
                                (discovery/narrowing-subtree discovered))]
    (refusal :too-many-candidate-files
             (str "This workspace holds more than " census/max-scanned-files
                  " candidate Clojure sources (" (:observed discovered)
                  " seen before the walk stopped). The census reads at most "
                  census/max-scanned-files
                  " and will not report a truncated tree as a complete census: "
                  (if next-call
                    (str "retry under " narrower ", which holds "
                         (get-in discovered [:subtree-counts narrower])
                         ".")
                    "name the sources, or point workspace_root at a subtree you know is smaller."))
             next-call
             (cond-> (merge {:maximum census/max-scanned-files
                             :fits census/max-scanned-files
                             :observed (:observed discovered)
                             :observed_at_least true
                             :files_read 0}
                            facts)
               ;; Two reasons there is no continuation, told apart. A
               ;; subtree the walk KNEW about and could not carry is not the
               ;; same fact as no fitting subtree at all, and reporting the
               ;; second for the first sends the caller looking for a smaller
               ;; tree when what they need is a shorter path.
               (and (not next-call) narrower)
               (assoc :remedy (narrowing-overflow-remedy narrower bytes))

               (and (not next-call) (not narrower))
               (assoc :remedy
                      (str "The walk stopped at the ceiling, so every count it "
                           "observed is a lower bound and no subtree it finished "
                           "walking is known to fit; name at most "
                           census/max-requested-files
                           " sources with files, or point workspace_root at a "
                           "directory you know is smaller."))))))

;; @spec MCP-OP-CENSUS-033
(defn- walk-entry-refusal
  "Refuse a tree that costs more directory entries than the walk may visit.

   The candidate ceiling bounds what the census READS; this one bounds what
   the walk COSTS. A tree of 60,000 non-sources holds no candidate at all, so
   the ceiling never fires and the walk enumerates every entry to discover
   nothing. The caller gets the bound, the entry count the walk had visited
   when it stopped (a lower bound, for the same reason the candidate count
   is), and a continuation computed from the walk's own per-directory
   aggregates: the largest subtree it FINISHED walking that fits under BOTH
   bounds. When nothing is known to fit, it gets a remedy and no next_call."
  [discovered canonical facts]
  (let [{:keys [narrower bytes next-call]}
        (narrowing-continuation canonical
                                (discovery/entry-narrowing-subtree discovered))]
    (refusal :too-many-walk-entries
             (str "This workspace holds more than " census/max-walk-entries
                  " filesystem entries (" (:entries-observed discovered)
                  " visited before the walk stopped). The census visits at most "
                  census/max-walk-entries
                  " entries and will not report a truncated tree as a complete "
                  "census: "
                  (if next-call
                    (str "retry under " narrower ", which holds "
                         (get-in discovered [:subtree-entries narrower])
                         " entries.")
                    "name the sources, or point workspace_root at a subtree you know is smaller."))
             next-call
             (cond-> (merge {:maximum census/max-walk-entries
                             :fits census/max-walk-entries
                             :observed (:entries-observed discovered)
                             :observed_at_least true
                             ;; The names the walk actually OBTAINED from the
                             ;; filesystem: the receipt for the promise that
                             ;; it stopped the directory rather than measured
                             ;; it. It is at most the bound.
                             :entries_yielded (:entries-yielded discovered)
                             :files_read 0}
                            facts)
               (and (not next-call) narrower)
               (assoc :remedy (narrowing-overflow-remedy narrower bytes))

               (and (not next-call) (not narrower))
               (assoc :remedy
                      (str "The walk stopped at the entry bound, so every "
                           "count it observed is a lower bound and no subtree "
                           "it finished walking is known to fit; name at most "
                           census/max-requested-files
                           " sources with files, or point workspace_root at a "
                           "directory you know is smaller."))))))

;; @spec MCP-OP-CENSUS-014
(defn- door-refusal
  [invalid canonical facts]
  (refusal :unknown-door-symbol
           (str "Unknown identity door " (:invalid invalid) ": " (:why invalid))
           {:tool "relation_census"
            :workspace_root canonical
            :doors (vec (sort (map str census/default-doors)))}
           (merge {:door (:invalid invalid)
                   :known_doors (vec (sort (map str census/default-doors)))}
                  facts)))

;; @spec MCP-OP-CENSUS-023
;; @spec MCP-OP-CENSUS-031
(defn- execute-in-context!
  [{:keys [project-root]} {:keys [files doors pool_size] :as params}]
  (let [root (mcp-paths/real-root project-root)
        canonical (.toString root)
        want-declared? (boolean (seq doors))
        t0 (System/nanoTime)
        requested (when (seq files) (mapv str files))
        discovered (when-not requested (candidate-files root))
        scanned (or requested (:files discovered))
        skipped-outside-root (:skipped-outside-root discovered 0)
        t-discovered (System/nanoTime)
        ;; Both bounds are checked BEFORE any read: a tree the census may not
        ;; finish is refused, never partially read and published as complete.
        bounded? (or (:exceeded? discovered) (:walk-exceeded? discovered))
        loaded (when-not bounded?
                 (collect-inputs root scanned {:declared? want-declared?}))
        t-read (System/nanoTime)
        ;; Two ways one real source reaches the census twice: a caller who
        ;; names it twice, and a walk that finds two paths onto it. Both are
        ;; collapsed, and the receipt reports the SUM — the caller cannot
        ;; reconcile `files` against what it asked for otherwise.
        duplicates (+ (:duplicates loaded 0) (:duplicates discovered 0))
        scanned-count (- (count scanned) (:duplicates loaded 0))
        ;; ONE fact bundle, published by EVERY shape this fn returns. A
        ;; receipt whose evidence depends on whether the census succeeded is a
        ;; receipt that hides the walk exactly when the caller must audit it.
        facts (census/discovery-facts
                {:files-scanned (cond
                                  bounded? 0
                                  (or (:refusal loaded) (:oversized loaded))
                                  (:read loaded 0)
                                  :else scanned-count)
                 :skipped-outside-root skipped-outside-root
                 :duplicates duplicates
                 :oversized (:oversized discovered)}
                :snake)]
    (cond
      (:walk-exceeded? discovered)
      (walk-entry-refusal discovered canonical facts)

      (:exceeded? discovered)
      (ceiling-refusal discovered canonical facts)

      (:refusal loaded)
      (refusal :unreadable-source-path
               (str (:error (:refusal loaded)) " (" (:file loaded) ")")
               {:tool "relation_census"
                :workspace_root canonical
                :files [(:file loaded)]}
               (merge {:file (:file loaded)} facts))

      (:oversized loaded)
      ;; The continuation is the request the caller made, MINUS the sources it
      ;; cannot read — a call the caller may replay verbatim. It is computable
      ;; because the caller named them. When removing them leaves no request,
      ;; there is no call to hand back and the refusal says so. Every other
      ;; option the caller supplied (doors, pool_size, and any field this
      ;; tool learns later) travels through UNCHANGED: this refusal narrows
      ;; `files`, it does not silently drop the rest of the request.
      (let [over (oversized-among root scanned)
            removed (set over)
            remaining (when requested
                        (vec (remove removed (distinct requested))))]
        (refusal :source-too-large
                 (str (:oversized loaded) " is " (:bytes loaded)
                      " bytes; the census reads at most " census/max-source-bytes)
                 (when (seq remaining)
                   (assoc (dissoc params :files)
                          :tool "relation_census"
                          :workspace_root canonical
                          :files remaining))
                 (cond-> (merge {:file (:oversized loaded)
                                 :bytes (:bytes loaded)
                                 :maximum census/max-source-bytes
                                 :files_removed (vec (take max-listed-files over))
                                 :files_removed_omitted
                                 (max 0 (- (count over) max-listed-files))}
                                facts)
                   (empty? remaining)
                   (assoc :remedy
                          (str "Every source this request named is larger than "
                               census/max-source-bytes
                               " bytes, so the request minus them is not a "
                               "request and no narrower call can be computed: "
                               "name a source under the byte cap with files, "
                               "or omit files to census the tree, where an "
                               "oversized source is skipped and counted "
                               "instead of refused.")))))

      :else
      (let [candidates (:inputs loaded)]
        (if (empty? candidates)
          (let [named (vec (take max-listed-files (distinct scanned)))
                ;; The caller named these files itself. Every one of them was
                ;; already scanned and none holds a fold arm, so no PROPER
                ;; subset of an already-refused, caller-named list is any more
                ;; likely to hold one: there is no narrower call to compute,
                ;; only the identical request just refused. Discovery (no
                ;; `files` in the request) is different — the tree walk found
                ;; these paths, the caller never named them, and pinning them
                ;; with `files` is a real narrower call than re-walking the
                ;; same root.
                explicit? (boolean requested)]
            (refusal :no-fold-arms-found
                     (str "No file defines defmethod fold-event arms. Scanned "
                          scanned-count " file(s).")
                     ;; An empty array is not a narrower call, and the schema
                     ;; this tool advertises does not accept one. Neither is
                     ;; the bare workspace call this refusal just answered: a
                     ;; tree whose only sources were skipped for size scans
                     ;; nothing, and handing the same root back is a call that
                     ;; refuses identically. The tool offers a call only when
                     ;; it can name the sources to look at, AND that call is
                     ;; not the one the caller just made.
                     (when (and (seq named) (not explicit?))
                       {:tool "relation_census"
                        :workspace_root canonical
                        :files named})
                     (cond-> (merge {:scanned named} facts)
                       (or (empty? named) explicit?)
                       (assoc :remedy
                              (cond
                                explicit?
                                (str "None of the file(s) this request named ("
                                     (str/join ", " named)
                                     ") defines defmethod fold-event arms, so "
                                     "the call just refused is already the "
                                     "narrowest one that can be computed: "
                                     "point files at sources that define fold "
                                     "arms, or omit files to census the tree.")

                                (empty? named)
                                (str "Nothing under " canonical
                                     " defines defmethod fold-event arms ("
                                     scanned-count " file(s) scanned), so no "
                                     "narrower call can be computed: point "
                                     "workspace_root at a directory whose "
                                     "sources define fold arms, or name the "
                                     "sources to census with files."))))))
          ;; The door symbols themselves are checked before any census runs;
          ;; whether a door is DEFINED anywhere can only be answered once the
          ;; scan has been parsed, so that half waits for the plan's own
          ;; `:declared` rather than parsing every file a second time.
          (let [syntactic (if want-declared?
                            (census/parse-doors doors nil)
                            census/default-doors)]
            (if (map? syntactic)
              (door-refusal syntactic canonical facts)
              (let [requested-pool (or pool_size (census-pool/default-pool-size))
                    pool-size (census/effective-pool-size requested-pool)
                    planned (census/plan {:inputs candidates
                                          :doors syntactic
                                          :map-fn (census-pool/pooled-map pool-size)})
                    declared (when want-declared?
                               (into (:declared loaded #{}) (:declared planned)))
                    confirmed (when want-declared?
                                (census/parse-doors doors declared))]
                (cond
                  (not (:ok planned))
                  (refusal (or (:error-type planned) :census-failed)
                           (:error planned)
                           {:tool "relation_census"
                            :workspace_root canonical
                            :files [(:file planned)]}
                           (merge {:file (:file planned)} facts))

                  (map? confirmed)
                  (door-refusal confirmed canonical facts)

                  :else
                  (build-receipt
                    {:merged planned
                     :reserved (+ receipt-envelope-allowance (count canonical))
                     :oversized (:oversized discovered)
                     :facts facts
                     :pool-size pool-size
                     :requested-pool requested-pool
                     :phases (cond-> {:read (/ (- t-read t-discovered) 1e6)
                                      :classify (get-in planned [:phases :classify])
                                      :merge (get-in planned [:phases :merge])}
                               discovered
                               (assoc :discover (/ (- t-discovered t0) 1e6)))}))))))))))

;; @spec MCP-OP-CENSUS-017
(defn- exhaustion-refusal
  "Turn a Throwable that escaped the census into a typed refusal.

   A census walks a tree it did not choose. Running out of heap or stack is a
   bounded, reportable outcome of that walk, not an adapter crash, and the
   caller needs a typed answer rather than a stack trace.

   It gets NO next_call. Every continuation this tool hands back is COMPUTED
   from the walk's own per-directory aggregates, and an exhaustion is exactly
   the case in which those aggregates were lost with the heap that held them:
   there is no narrower call to compute. The refusal says that in a `remedy`
   instead. A caption in an argument position — `files
   [\"<a narrower file list>\"]` — is not a smaller promise than a real
   continuation, it is an unexecutable one, and MCP-OP-CENSUS-017 forbids it."
  [^Throwable error]
  (let [exhausted? (instance? VirtualMachineError error)]
    (refusal (if exhausted? :census-resource-exhausted :census-adapter-failure)
             (str (if exhausted?
                    "The census exhausted a runtime resource: "
                    "The census failed: ")
                  (.getName (class error))
                  (when-let [message (.getMessage error)] (str " " message)))
             nil
             {:exhausted exhausted?
              :files_read 0
              :remedy
              (str "The census ran out of a runtime resource part-way through, "
                   "so the walk's own aggregates were lost with it and this "
                   "refusal can compute no narrower call: name at most "
                   census/max-requested-files
                   " sources with files, or point workspace_root at a "
                   "directory you know is smaller, and retry.")})))

(defn execute-request!
  "Route and execute one relation_census request."
  [config params]
  (let [normalized (json/parse-string (json/generate-string params) true)
        shaped (validate-request-shape normalized)]
    (if-not (:ok shaped)
      ;; Refused on shape alone — before workspace_root is ever resolved, so
      ;; before any filesystem work at all. MCP-OP-CENSUS-016/029.
      shaped
      (let [router (or (:workspace-router config) (workspace/router config))
            routed (workspace/resolve-request router (:params shaped))]
        (if-not (:ok routed)
          ;; No root resolved, so there is no root from which a narrower call
          ;; could be computed, and a caption describing the argument the caller
          ;; got wrong is not a call. MCP-OP-CENSUS-014: a remedy instead.
          (-> routed
              (assoc :ok false
                     :operation "relation-census"
                     :error_type (or (:error_type routed) "invalid-workspace-root")
                     :read_complete false
                     :remedy (str "The workspace root this request resolved to is "
                                  "not an existing absolute directory, so nothing "
                                  "about it can be narrowed: retry with "
                                  "workspace_root naming a directory that exists."))
              (dissoc :next_call))
          (assoc (try
                   (execute-in-context! (:config routed) (:params routed))
                   (catch Throwable error
                     (exhaustion-refusal error)))
                 :workspace_root (:workspace-root routed)))))))

(defn- summary
  [result]
  (if (:ok result)
    (let [c (:counts result)]
      (str "relation_census\n  " (:files result) " file(s) · "
           (:arms result) " arm(s) · " (:sites result) " site(s) · "
           "raw " (:raw c 0) " · unknown " (:unknown c 0)
           " · guarded " (:guarded c 0) " · door " (:door c 0)
           " · set " (:set c 0) " · outside-arms " (:outside_arms result)
           " · pool " (:pool_size result) " · "
           (mcp-operation/format-elapsed-ms (:elapsed_ms result))
           "\nnext_action: " (:next_action result)))
    (str "relation_census refused · " (:error_type result)
         " · " (mcp-operation/format-elapsed-ms (:elapsed_ms result))
         "\n" (:error result) "\nnothing was written")))

(defn handle-relation-census
  "clojure-mcp callback handler retained as a Var for hot reload."
  [_exchange params callback]
  (mcp-operation/invoke!
    {:execute #(if-let [config @runtime-config]
                 (execute-request! config params)
                 (refusal :server-not-initialized
                          "relation_census server is not initialized"
                          {:tool "relation_census"}))
     :summarize summary
     :callback callback}))

;; @spec MCP-OP-CENSUS-015
(def relation-census-tool
  {:id :relation-census
   :name "relation_census"
   :description census-tool-description
   :schema census-tool-schema
   :output-schema census-output-schema
   :annotations census-annotations
   :structured? true
   :tool-fn #'handle-relation-census})
