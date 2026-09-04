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
   [clj-surgeon.measured :as measured]
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
                "next_action" {:type "string"}
                "next_call" {:type "object"}
                "error_type" {:type "string"}
                ;; @spec MCP-OP-TIME-004
                ;; @spec MCP-OP-TIME-005
                ;; The request clock AND the per-phase clocks live in the
                ;; measured partition, like every other public MCP result's.
                ;; This tool landed while the partition was landing on a
                ;; branch, so its schema was written in the old wire: a
                ;; top-level `elapsed_ms` the finalizer no longer produces and
                ;; a top-level `phases_elapsed_ms` the partition relocates,
                ;; with no `measured` block, which every other canonical
                ;; output schema requires.
                "measured" mcp-operation/measured-output-schema}
   :required ["ok" "operation" "measured"]})

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
  ;; Opus's round-seventeen item 1, blocking. Round sixteen bounded HERE, at
  ;; "the one place every refusal this tool emits is assembled" — and that
  ;; claim was false the day it shipped: the workspace router's refusal is
  ;; `assoc`ed onto the router's own map and never passes through this
  ;; function, so a 10,001-character `workspace_root` came back as a
  ;; 10,540-byte refusal with a 10,007-character field and no marker.
  ;;
  ;; A constructor is a SITE. `bound-refusal`'s own docstring says the bound
  ;; belongs at each entrance's LAST step "rather than at the sites that build
  ;; the strings, for the reason MCP-OP-CENSUS-014 gives about the continuation
  ;; constructor: a bound enforced at some of a namespace's construction sites
  ;; is not a bound, it is those sites' habit, and the habit does not travel to
  ;; the site added next round." So the bound is GONE from here and lives at
  ;; `entrance-bounded`, which every exit from this entrance passes through.
  (merge (cond-> {:ok false
                  :operation "relation-census"
                  :census_version census/census-version
                  :error_type (name error-type)
                  :error message
                  :source_unchanged true
                  :read_complete false}
           (some? next-call) (assoc :next_call next-call))
         data))

;; @spec MCP-OP-CENSUS-014
(defn- entrance-bounded
  "THE ONE PLACE this entrance's refusals are length-bounded.

   The sibling of `run-relation-census`'s last step at the CLI, and the same
   rule: a refusal is what a caller reads when something has already gone
   wrong, and it is the last place that should be able to hand back ten
   kilobytes of the caller's own bad input at the moment they are least able
   to read it. A RECEIPT is untouched — it carries its own 4,096-byte cap and
   its own trimming rules — so this fires on `(false? (:ok result))` alone.

   Applied at the exits rather than at `refusal`, because `refusal` is a
   construction SITE and two of this entrance's refusals are not built by it:
   the workspace router's, which is `assoc`ed onto the router's own map, and
   the shape pass's, which comes back from the shared validator."
  [result]
  (if (false? (:ok result))
    (census/bound-refusal result)
    result))

;; @spec MCP-OP-CENSUS-014
(defn- candidate-field-weights
  "Every field of a continuation candidate, with the UTF-8 bytes it costs on
   the wire.

   WALKED, never listed. `overflow-measurement` below is the reason: it used to
   weigh a NAMED PAIR of the candidate's parts, and the candidate is not a
   fixed pair — the `:refusal loaded` branch builds it out of the caller's own
   request, so every option the caller sent rides through it and any option
   added to the schema next round rides through it too. A named field list is
   a list of the parts whoever wrote it happened to know about, and the caller
   is the one who pays when the bulk is in the part it does not name.

   `:tool` is excluded, and it is the only exclusion: it is STAMPED by the
   constructor, it is 26 constant bytes, and a caller cannot shorten it, so
   naming it as a cause could never be advice."
  [candidate]
  (into {}
        (map (fn [[k v]]
               [k (census/utf8-byte-count (json/generate-string {k v}))]))
        (dissoc candidate :tool)))

;; @spec MCP-OP-CENSUS-014
(defn- overflow-measurement
  "WHICH part of an over-long continuation is the length that does not fit.

   Opus's round-sixteen item 5 and round-seventeen item 2. The remedy this
   feeds used to state one cause unconditionally — \"the length of the
   workspace path in it is\" — about a 24-character root carrying 500 named
   sources. Round sixteen MEASURED the cause and measured two parts of it;
   round seventeen found the third, `doors`, and the same false sentence back:
   \"workspace_root alone measures 19 of those bytes\" on a 723-byte
   continuation, 2.6% named as the cause. A caller who follows a remedy like
   that shortens the one thing that is not the problem and receives the
   identical refusal: the loop-with-a-receipt MCP-OP-CENSUS-014 forbids a
   continuation, arriving in a remedy instead.

   So the cause is DERIVED. `candidate-field-weights` walks whatever fields the
   candidate actually has, and the HEAVIEST of them is what the caller must
   change. Ties break by field name so that two runs over one candidate name
   the same field. When the heaviest is `files`, one very long entry and five
   hundred short ones are still different problems with different remedies, and
   that distinction survives.

   Returns `{:bytes :cause :measured :entries :field}` where `:field` is the
   name walked out of the candidate and `:measured` is the byte figure the
   named cause accounts for — for the two named causes, the figure round
   sixteen's witnesses already assert, which is the raw length of the thing the
   caller would shorten rather than its rendered pair."
  [candidate bytes]
  (let [weights (candidate-field-weights candidate)
        entries (when (sequential? (:files candidate)) (vec (:files candidate)))
        entry-sizes (mapv #(census/utf8-byte-count (str %)) entries)
        entry-bytes (reduce + 0 entry-sizes)
        longest (reduce max 0 entry-sizes)
        [heaviest heaviest-bytes]
        (first (sort-by (juxt (comp - val) (comp str key)) weights))]
    (merge {:bytes bytes :entries (count entries)}
           (when heaviest {:field (name heaviest)})
           (cond
             ;; A candidate with no variable part at all. Unreachable through
             ;; either entrance today, and it gets a cause-free sentence rather
             ;; than a guess: naming nothing is honest, naming a minority is
             ;; the defect this function exists to prevent.
             (nil? heaviest)
             {:cause :indeterminate :measured 0}

             (= :workspace_root heaviest)
             {:cause :workspace-root-length
              :measured (census/utf8-byte-count
                          (str (:workspace_root candidate)))}

             (= :files heaviest)
             ;; One entry carrying more than half of what the list weighs is
             ;; the entry, not the count: telling that caller to name fewer
             ;; sources is the same unmeasured advice in a different sentence.
             (if (> (* 2 longest) entry-bytes)
               {:cause :entry-length :measured longest}
               {:cause :entry-count :measured entry-bytes})

             :else
             {:cause :field-length :measured heaviest-bytes})))) 

;; @spec MCP-OP-CENSUS-014
(defn continuation
  "THE ONE PLACE this tool turns a candidate request into a `next_call`.

   Sol's round-fourteen item 9, blocking. `narrowing-continuation` measured
   what it built and both bound refusals went through it; the other SEVEN
   sites — the shape pass, the door refusal, the unreadable and oversized
   narrowings, the arm-less file list, the plan failure and the
   uninitialised-server refusal — each spelled a map straight into `refusal`'s
   `next-call` argument, and nothing measured any of them. An unknown-field
   refusal on a 600-character `workspace_root` emitted 660 UTF-8 bytes under a
   512-byte ceiling. A bound enforced at one of eight construction sites is
   not a bound, it is that site's habit, and the habit does not travel to the
   site added next round.

   So every site hands its candidate HERE and takes back what this returns.
   What this enforces, on all of them alike:

   - `:tool` is STAMPED, never spelled by a site. A continuation naming
     another tool is not a narrowing of a census request, and a typo in an
     argument position is exactly the class MCP-OP-CENSUS-014 forbids.
   - an empty `:files` is not a narrowing — the published schema declares
     `minItems 1`, and a call the tool's own schema rejects is unexecutable
     whatever it measures.
   - the ceiling is measured on the RENDERED JSON in UTF-8 BYTES, because
     that is what crosses the wire (round twelve's finding, now enforced
     everywhere rather than at the two sites that happened to call it).

   `nil` in means there is nothing to offer and gets nothing back. A
   candidate that cannot fit comes back with `:next-call` nil and `:bytes`
   set, so the refusal can say what it measured instead of going silent — the
   two situations are DIFFERENT and a caller must be able to tell them apart."
  [candidate]
  (let [stamped (when (map? candidate)
                  (assoc candidate :tool "relation_census"))
        ;; Sol's round-fifteen item 10. The empty-`files` question was asked
        ;; because the published schema declares `minItems 1`; the SAME schema
        ;; declares the items are strings, and that half was never asked — so
        ;; `[nil]`, `[42]` and even `"x"` travelled, and the `:census-failed`
        ;; fallback published `files [null]` from a plan failure that named no
        ;; source. A call the tool's own schema rejects is the same
        ;; unexecutable promise a caption in an argument position is, and
        ;; MCP-OP-CENSUS-014 forbids both by the same sentence.
        ;; Opus's round-sixteen item 6. Half the rule is not the rule. The
        ;; schema this cites declares `maxItems 512` as well as `minItems 1`,
        ;; and 513 entries was refused only because it rendered over the byte
        ;; ceiling — the SAME masking that hid the `items` half for sixteen
        ;; rounds, and unreachable by accident rather than by construction.
        ;;
        ;; The item rule asked is the one the ENTRANCE applies, not a weaker
        ;; paraphrase of it: a continuation is a call the caller replays into
        ;; THIS tool, so an entry that `relative-source-path?` refuses is an
        ;; unexecutable promise whatever JSON Schema thinks of it. A NUL byte
        ;; is a string to JSON and a refusal here, which is why it travelled.
        publishable-files?
        (fn [files]
          (and (sequential? files)
               (seq files)
               (<= (count files)
                   (get-in census-tool-schema
                           [:properties "files" :maxItems]))
               (every? mcp-paths/relative-source-path? files)))
        faithful (when (and stamped
                            (or (not (contains? stamped :files))
                                (publishable-files? (:files stamped))))
                   stamped)
        rendered (when faithful (json/generate-string faithful))
        bytes (when rendered (census/utf8-byte-count rendered))
        fits? (boolean (and rendered
                            (census/within-next-call-bytes? rendered)))]
    {:candidate faithful
     :bytes bytes
     ;; Opus's round-sixteen item 5. A candidate dropped for LENGTH carries
     ;; WHAT was measured and WHICH part of it dominates, so the remedy can
     ;; state a cause it observed instead of the one this code happened to be
     ;; written for. `nil` when the candidate fitted or was dropped for shape:
     ;; those are different facts and a caller must be able to tell them apart.
     :overflow (when (and bytes (not fits?))
                 (overflow-measurement faithful bytes))
     :next-call (when fits? faithful)}))

;; @spec MCP-OP-CENSUS-014
(defn- continuation-overflow-remedy
  "Why a continuation this refusal COULD compute was not handed back.

   The sibling of `narrowing-overflow-remedy`, for the refusals whose
   continuation is built out of the caller's own request rather than out of
   the walk's aggregates. A refusal that names a bound without naming the
   value it compared against leaves the caller to guess how much shorter is
   short enough — and a refusal that names a CAUSE it never measured is worse
   than silence, because the caller shortens the wrong thing and receives the
   identical refusal (Opus's round-sixteen item 5).

   Every sentence below is a figure `overflow-measurement` actually observed."
  [{:keys [bytes cause measured entries field]}]
  (str "The narrowest continuation this refusal can compute renders as "
       bytes " UTF-8 bytes, over the " census/max-next-call-bytes
       "-byte ceiling a continuation must fit, so none is offered. "
       (case cause
         :workspace-root-length
         (str "The REQUEST is not the problem, the length of the workspace "
              "path in it is: workspace_root alone measures " measured
              " of those bytes — retry with workspace_root reaching the same "
              "tree by a shorter path, and fix what this refusal named.")

         :entry-length
         (str "The workspace path is not the problem, the length of a source "
              "path in it is: the longest of the " entries
              " sources this call would name measures " measured
              " of those bytes — name shorter sources with files, and fix "
              "what this refusal named.")

         :field-length
         (str "The workspace path is not the problem, the length of the "
              field " in it is: " field " alone measures " measured
              " of those bytes — retry with a shorter " field ", and fix what "
              "this refusal named.")

         :indeterminate
         (str "No one part of the call accounts for the length, so this "
              "refusal names none: narrow the request itself and retry, and "
              "fix what this refusal named.")

         (str "The workspace path is not the problem, the NUMBER of sources "
              "in it is: the " entries " sources this call would name measure "
              measured " of those bytes together — name fewer sources with "
              "files, and fix what this refusal named."))))

;; @spec MCP-OP-CENSUS-014
(defn- continuation-refused-remedy
  "Why a refusal published no continuation, when the constructor refused one.

   The constructor returns a MEASURED byte length beside a candidate it dropped
   for LENGTH, and no byte length at all beside one it dropped for SHAPE. The
   two are different facts and the caller must be able to tell them apart: one
   says retry from a shorter path, the other says there was no call to make.
   Reading the first wording over the second prints the measurement of
   something that was never measurable."
  [overflow]
  (if (some? overflow)
    (continuation-overflow-remedy overflow)
    (str "The narrowest continuation this refusal can compute is not a call "
         "the schema this tool publishes would accept — it names no source "
         "this tool could carry — so none is offered: name the sources to "
         "census with files, or point workspace_root at a smaller tree, and "
         "fix what this refusal named.")))

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
        ;; Through the ONE constructor, like every other site. This is the
        ;; site Sol measured at 660 bytes: the caller's own `workspace_root`
        ;; travels through verbatim, and a long one made the continuation
        ;; longer than the wire rule allows without anything noticing.
        computed (when carriable?
                   (continuation
                     (cond-> {:pool_size 8}
                       (string? asked-root) (assoc :workspace_root asked-root))))
        next-call (:next-call computed)
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
                                 ;; The THIRD reason there is no continuation,
                                 ;; kept apart from the other two: the value
                                 ;; was carriable and the call it produced is
                                 ;; over the wire ceiling. It gets the bytes
                                 ;; it measured, not silence.
                                 (when (and (not uncomputable)
                                            carriable?
                                            (nil? next-call))
                                   {:remedy (continuation-refused-remedy
                                              (:overflow computed))})
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

;; @spec MCP-OP-CENSUS-014
(defn- unreadable-among
  "The named sources that cannot be read through the fence, in the order named.

   `collect-inputs` stops at the FIRST unreadable path, which is right when
   reading: nothing after it can be trusted either. It is wrong when COMPUTING
   a continuation, because a request that removes only the entry the reader
   tripped on refuses again on the next one. This resolves the named list and
   reads nothing, and it runs only when a refusal is already being built.

   The sibling of `oversized-among`, and deliberately its twin: an entry the
   fence refuses and an entry too large to read are the same thing to a
   continuation — a name the next call must not carry."
  [root relatives]
  (into []
        (remove (fn [relative]
                  (:ok (mcp-paths/resolve-source-path root relative))))
        relatives))

;; @spec MCP-OP-CENSUS-014
;; @spec MCP-OP-CENSUS-017
(defn- read-failure-refusal
  "A read that failed AFTER the fence admitted the path, as a fence refusal.

   The two are the SAME fact to a continuation — a name the next call must not
   carry — so they answer alike, which is what makes the check-then-read window
   invisible to the caller instead of visible as a different error type.

   The exception's own message is NOT published: `FileNotFoundException`
   renders as \"<absolute path> (Permission denied)\", and every other refusal
   this namespace publishes names paths project-relative."
  [^Throwable error]
  {:ok false
   :error_type :source-not-readable
   ;; The same cause name the CLI's `census-read-refusal` publishes, from the
   ;; one vocabulary in `mcp-paths/source-refusal-causes`: the two entrances
   ;; name their refusals differently by design, so the CAUSE is what a witness
   ;; can compare across them.
   :cause (name :read-failed-after-fence)
   :error (str "Source file passed the fence and then could not be read; its "
               "mode or its existence changed under the census ("
               (.getName (class error)) ")")})

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
         (if-not (:ok resolved)
           (reduced (assoc acc :refusal resolved :file relative))
           (if (contains? (:seen acc) (str (:canonical resolved)))
             (update acc :duplicates inc)
             ;; Sol's round-fifteen item 5, and the residual round fourteen
             ;; left behind. The fence has answered; between that answer and
             ;; this read the filesystem may change, and it does: a mode
             ;; flipped by another process turned 398 of 2,000 requests into
             ;; `census-adapter-failure` with `exhausted false` and a
             ;; resource-exhaustion remedy, and an ordinary editor's atomic
             ;; save — `spit` to `.tmp`, `renameTo` — opens the identical
             ;; window with no `chmod` anywhere.
             ;;
             ;; Moving the CHECK earlier narrowed that class; only a typed
             ;; catch at the READ closes it. `IOException` and not `Throwable`:
             ;; an exhaustion is a different fact and keeps its own answer at
             ;; the catch-all above.
             (let [read (try
                          (let [size (Files/size ^Path (:canonical resolved))]
                            (if (> size census/max-source-bytes)
                              {:oversized size}
                              {:source (slurp (:path resolved))}))
                          (catch java.io.IOException error {:unreadable error}))]
               (cond
                 (:unreadable read)
                 (reduced (assoc acc
                                 :refusal (read-failure-refusal
                                            (:unreadable read))
                                 :file relative))

                 (:oversized read)
                 (reduced (assoc acc
                                 :oversized relative
                                 :bytes (:oversized read)))

                 :else
                 (let [source (:source read)
                       acc (-> acc
                               (update :seen conj (str (:canonical resolved)))
                               (update :read inc))]
                   (if (census/defines-arms? source)
                     (update acc :inputs conj {:file relative :source source})
                     ;; Not an arm file: its text is dropped here. Only its
                     ;; top-level names survive, and only when a caller's doors
                     ;; need checking against them.
                     (cond-> acc
                       declared?
                       (update :declared into
                               (census/source-declared-names source)))))))))))
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
      (merge
        (into {}
              (remove (comp nil? val))
              ;; The discovery facts are the SAME facts every refusal
              ;; publishes, built by the same kernel: a success is not a
              ;; different receipt shape with evidence rules of its own.
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
                 :unrecognised_calls unrecognised
                 :next_action (next-action counts unrecognised)}
                facts))
        ;; @spec MCP-OP-TIME-004
        ;; @spec MCP-OP-CENSUS-013
        ;; The phase clocks are published INSIDE the measured block, at the
        ;; level the request clock joins them at, rather than beside it as a
        ;; top-level field. The intent is unchanged — every phase that ran is
        ;; still named with its own figure — only the PARTITION moves, and it
        ;; moves because a clock-derived field outside the block reaches the
        ;; hashed parity subject by a second route the projection is blind to.
        ;; Built HERE rather than left to the finalizer's relocation so that
        ;; `bound-receipt` measures the bytes the wire actually carries.
        (measured/measured {:phases_elapsed_ms phases}))
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
  (merge {:narrower narrower}
         (continuation (when narrower
                         {:workspace_root (str canonical "/" narrower)}))))

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
  (let [known (vec (sort (map str census/default-doors)))
        {:keys [next-call overflow]}
        (continuation {:workspace_root canonical :doors known})]
    (refusal :unknown-door-symbol
             (str "Unknown identity door " (:invalid invalid) ": "
                  (:why invalid))
             next-call
             (cond-> (merge {:door (:invalid invalid) :known_doors known}
                            facts)
               (nil? next-call)
               (assoc :remedy (continuation-refused-remedy overflow))))))

;; @spec MCP-OP-CENSUS-023
;; @spec MCP-OP-CENSUS-031
(defn- execute-in-context!
  [{:keys [project-root]} {:keys [files doors pool_size] :as params}]
  (let [root (mcp-paths/real-root project-root)
        canonical (.toString root)
        want-declared? (boolean (seq doors))
        ;; @spec MCP-OP-TIME-004
        ;; @spec MCP-OP-TIME-005
        ;; Phase ticks through `measured`, so every phase figure is a TAGGED
        ;; READING carrying its own provenance instead of a bare number this
        ;; adapter could publish anywhere. Raw `System/nanoTime` here is what
        ;; the invariant witness named when the two lanes met.
        t0 (measured/start)
        requested (when (seq files) (mapv str files))
        discovered (when-not requested (candidate-files root))
        scanned (or requested (:files discovered))
        skipped-outside-root (:skipped-outside-root discovered 0)
        discover-ms (measured/elapsed-ms t0)
        t-discovered (measured/start)
        ;; Both bounds are checked BEFORE any read: a tree the census may not
        ;; finish is refused, never partially read and published as complete.
        ;; A subtree the walk could not ENTER bounds the census exactly as
        ;; the two ceilings do: a tree this census may not finish is refused
        ;; before anything is read, never partially read and published as
        ;; complete (Opus's round-sixteen item 4).
        bounded? (boolean (or (:exceeded? discovered)
                              (:walk-exceeded? discovered)
                              (seq (:unreadable-directories discovered))))
        loaded (when-not bounded?
                 (collect-inputs root scanned {:declared? want-declared?}))
        read-ms (measured/elapsed-ms t-discovered)
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

      ;; @spec MCP-OP-CENSUS-018
      ;; A subtree the walk could not ENTER, decided after both bounds and
      ;; before anything is read. Opus's round-sixteen item 4: it was swallowed
      ;; with no counter and this tool answered `ok true, files 1, arms 1,
      ;; read_complete true` about a tree it had not finished walking, while
      ;; one unreadable FILE refused the whole census. The CLI answers the same
      ;; tree with the same cause and the same directory name.
      (seq (:unreadable-directories discovered))
      ;; Opus's round-seventeen item 5. `census-discovery` records the
      ;; walk-relative path, which for the ROOT is `""`, and three sentences
      ;; below interpolate it: "the directory  may not be read", "make
      ;; readable under the workspace root". The root gets its name, from the
      ;; one function the CLI calls for the same sentences.
      (let [directory (census/shown-directory
                        (first (:unreadable-directories discovered)))]
        (refusal :unreadable-source-path
                 (str "the directory " directory " may not be read or "
                      "traversed by this process, so this census cannot claim "
                      "to have read the tree")
                 nil
                 (merge
                   {:cause (name :directory-denied)
                    :directory directory
                    :remedy (str directory " came from the workspace walk, "
                                 "not from the request, so there is no "
                                 "request to narrow and no narrower call can "
                                 "be computed: "
                                 (census/directory-repair-phrase directory)
                                 ", remove it, or "
                                 "name the sources to census with files. A "
                                 "census is a completeness claim, and a "
                                 "subtree this process may not enter cannot "
                                 "be counted as read.")}
                   facts)))

      (:refusal loaded)
      ;; The continuation is the request the caller made, MINUS the entries
      ;; that cannot be read — exactly as the oversized branch below, and for
      ;; the same reason. Sol's round-thirteen item 8: this branch handed back
      ;; `files [<the entry that failed>]`, which is not a narrowing of
      ;; anything. It drops the sources the caller asked about, drops every
      ;; other option the request carried, and replays into the IDENTICAL
      ;; refusal — a loop with a receipt. When removing the unreadable entries
      ;; leaves no request, there is no call to hand back and the refusal says
      ;; so. When the caller named no `files` at all, the paths came from the
      ;; walk rather than the request, so there is no request to narrow either.
      (let [tripped (:file loaded)
            ;; The entries the FENCE refuses, PLUS the one the reader tripped
            ;; on. Sol's round-fifteen item 5: when the read is what failed,
            ;; re-resolving the list through the fence answers "every entry is
            ;; fine" and the narrowing computed from it is the identical
            ;; request — a loop with a receipt, which is exactly what this
            ;; branch's continuation exists to prevent.
            unreadable (when requested
                         (vec (distinct
                                (concat (unreadable-among root scanned)
                                        (when (some #{tripped} requested)
                                          [tripped])))))
            removed (set unreadable)
            remaining (when requested
                        (vec (remove removed (distinct requested))))
            {:keys [next-call overflow]}
            (continuation (when (seq remaining)
                            (assoc (dissoc params :files)
                                   :workspace_root canonical
                                   :files remaining)))]
        (refusal :unreadable-source-path
                 (str (:error (:refusal loaded)) " (" (:file loaded) ")")
                 next-call
                 (cond-> (merge {:file (:file loaded)} facts)
                   ;; The shared cause, carried out to the caller. Without it
                   ;; the tool publishes one name — `unreadable-source-path` —
                   ;; for missing, denied, irregular and unresolvable alike,
                   ;; and nothing an entrance-crossing witness can compare.
                   (:cause (:refusal loaded))
                   (assoc :cause (:cause (:refusal loaded)))

                   (and (seq remaining) (nil? next-call))
                   (assoc :remedy (continuation-refused-remedy overflow))

                   (seq unreadable)
                   (merge {:files_removed (vec (take max-listed-files
                                                     unreadable))
                           :files_removed_omitted
                           (max 0 (- (count unreadable) max-listed-files))})

                   (and requested (empty? remaining))
                   (assoc :remedy
                          (str "Every source this request named is unreadable "
                               "through the project fence — missing, outside "
                               "it, or there but not readable by this process "
                               "— so the request minus them is not a request "
                               "and no narrower call can be computed: name a "
                               "source that exists and is readable under the "
                               "workspace root with files, or omit files to "
                               "census the tree."))

                   (not requested)
                   (assoc :remedy
                          (str "This path came from the workspace walk, not "
                               "from the request, so there is no request to "
                               "narrow and no narrower call can be computed: "
                               "remove or repair " (:file loaded)
                               " under " canonical
                               ", or name the sources to census with files.")))))

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
                        (vec (remove removed (distinct requested))))
            {:keys [next-call] over-overflow :overflow}
            (continuation (when (seq remaining)
                            (assoc (dissoc params :files)
                                   :workspace_root canonical
                                   :files remaining)))]
        (refusal :source-too-large
                 (str (:oversized loaded) " is " (:bytes loaded)
                      " bytes; the census reads at most " census/max-source-bytes)
                 next-call
                 (cond-> (merge {:file (:oversized loaded)
                                 :bytes (:bytes loaded)
                                 :maximum census/max-source-bytes
                                 :files_removed (vec (take max-listed-files over))
                                 :files_removed_omitted
                                 (max 0 (- (count over) max-listed-files))}
                                facts)
                   (and (seq remaining) (nil? next-call))
                   (assoc :remedy (continuation-refused-remedy over-overflow))

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
                explicit? (boolean requested)
                {:keys [next-call overflow]}
                (continuation (when (and (seq named) (not explicit?))
                                {:workspace_root canonical :files named}))]
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
                     next-call
                     (cond-> (merge {:scanned named} facts)
                       (and (seq named) (not explicit?) (nil? next-call))
                       (assoc :remedy (continuation-refused-remedy overflow))

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
                                ;; Opus's round-nineteen item 4. The CLI twin
                                ;; (core.clj) was repaired in round nineteen
                                ;; and this one was not, so the tool published
                                ;; its absolute root in prose while the rule
                                ;; said neither entrance may. The root has ONE
                                ;; name.
                                (str "Nothing under "
                                     census/workspace-root-token
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
                  ;; Sol's round-fifteen item 10: this handed the constructor
                  ;; `{:files [(:file planned)]}` unconditionally, and a plan
                  ;; failure that names no `:file` — the documented reason this
                  ;; fallback exists at all — made that `[nil]`. There is
                  ;; nothing to narrow to when nothing was named, so nothing is
                  ;; offered, and the null `:file` is omitted rather than
                  ;; published as a fact about the failure.
                  (let [{:keys [next-call overflow]}
                        (continuation (when (:file planned)
                                        {:workspace_root canonical
                                         :files [(:file planned)]}))]
                    (refusal (or (:error-type planned) :census-failed)
                             (:error planned)
                             next-call
                             (cond-> facts
                               (:file planned)
                               (assoc :file (:file planned))

                               (nil? next-call)
                               (assoc :remedy
                                      (continuation-refused-remedy
                                        overflow)))))

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
                     :phases (cond-> {:read read-ms
                                      :classify (get-in planned [:phases :classify])
                                      :merge (get-in planned [:phases :merge])}
                               discovered
                               (assoc :discover discover-ms))}))))))))))

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

(defn- execute-request!*
  "Route and execute one relation_census request, before the entrance bound.

   Separate from `execute-request!` so that the bound is applied ONCE, at the
   exit, on every branch below alike — including the two that do not go
   through `refusal`."
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

;; @spec MCP-OP-CENSUS-014
(defn execute-request!
  "Route and execute one relation_census request, bounded at the exit."
  [config params]
  (entrance-bounded (execute-request!* config params)))

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
           ;; @spec MCP-OP-TIME-005
           ;; The request clock lives in the measured partition now; reading
           ;; it as a TOP-LEVEL field got `nil` from the finalizer's own
           ;; output and `format-elapsed-ms` refused it, typed, on a receipt
           ;; that was otherwise complete. `mcp-operation/elapsed-ms` is the
           ;; reader every other tool's summary already uses.
           (mcp-operation/format-elapsed-ms (mcp-operation/elapsed-ms result))
           "\nnext_action: " (:next_action result)))
    (str "relation_census refused · " (:error_type result)
         " · " (mcp-operation/format-elapsed-ms (mcp-operation/elapsed-ms result))
         "\n" (:error result) "\nnothing was written")))

(defn handle-relation-census
  "clojure-mcp callback handler retained as a Var for hot reload."
  [_exchange params callback]
  (mcp-operation/invoke!
    ;; The handler's own exit, bounded by the SAME function, so the
    ;; uninitialised-server refusal — which is raised here, before
    ;; `execute-request!*` is ever called — is not the next shape outside the
    ;; bound.
    {:execute #(entrance-bounded
                  (if-let [config @runtime-config]
                    (execute-request!* config params)
                    (refusal :server-not-initialized
                             "relation_census server is not initialized"
                             (:next-call (continuation {})))))
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
