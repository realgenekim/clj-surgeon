(ns clj-surgeon.relation-census
  "Pure census of collection writes inside event-fold arms.

   The census LOCATES review work. It does not prove idempotency and it is not
   an enforcement gate: `:raw` means \"no recognised guard dominates this
   write\", `:unknown` means \"this analyzer cannot decide\", and every site
   carries the evidence a reviewer needs to judge it.

   This namespace is pure and babashka-compatible. Parallelism is injected by
   the caller as `:map-fn`; it changes elapsed time and never the answer."
  (:require
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.walk :as walk]
   [rewrite-clj.node :as node]
   [rewrite-clj.parser :as parser]))

(def census-version 1)

;; ---------------------------------------------------------------------------
;; Shared bounds: one kernel for both entrances (MCP tool and CLI op)
;; ---------------------------------------------------------------------------

(def max-pool-size
  "The largest plan-phase pool a caller may ask for."
  64)

(def max-requested-files
  "The largest explicit file list a caller may pass."
  512)

(def max-doors
  "The largest identity-door list a caller may pass."
  32)

(def max-source-bytes
  "A single source larger than this is not censused."
  (* 2 1024 1024))

(def max-scanned-files
  "Discovery stops after this many candidate sources."
  4000)

;; @spec MCP-OP-CENSUS-033
(def max-walk-entries
  "Discovery stops after visiting this many directory entries, of any name.

   `max-scanned-files` bounds what the census will READ; it does not bound what
   the walk COSTS. Only a candidate Clojure source counts toward that ceiling,
   so a tree of 60,000 images, fixtures or build artefacts holds no candidate
   at all: the ceiling never fires and the walk enumerates the whole tree to
   discover nothing. This bound counts EVERY entry the walk visits — files,
   directories, and the pruned directories it declines to descend — and is the
   same 50,000 the alias-migration walker uses, because the resource being
   protected is the same one: the number of directory entries one operation may
   touch before it owes the caller an answer."
  50000)

(def skipped-directories
  "Directories both entrances prune before reading them."
  #{".git" "node_modules" "target" ".cpcache" ".clj-kondo" ".lsp" ".shadow-cljs"
    ".calva" "out" "dist" ".idea"})

(def max-listed-files
  "The largest number of file names either entrance lists in one receipt.

   The bound belongs here, next to the other shared bounds, because a receipt
   that lists twelve of twenty names is only honest if the entrance that
   listed them also says how many it left out — and both entrances must mean
   the same twelve."
  12)

(def max-next-call-bytes
  "The largest continuation either entrance hands back with a refusal.

   A continuation is only useful if the caller can read it and run it; one
   that grows with the tree it is refusing is neither."
  512)

(def discovery-fact-keys
  "The discovery facts, in the CLI's key style mapped to the tool's.

   Two entrances publish one set of facts in two spellings; the spelling is a
   table here rather than a literal in each entrance, so neither entrance can
   invent a fact or quietly drop one."
  {:files-scanned :files_scanned
   :skipped-outside-root :skipped_outside_root
   :duplicates-collapsed :duplicates_collapsed
   :oversized-skipped :oversized_skipped
   :oversized-skipped-omitted :oversized_skipped_omitted})

;; @spec MCP-OP-CENSUS-028
;; @spec MCP-OP-CENSUS-030
;; @spec MCP-OP-CENSUS-032
(defn discovery-facts
  "What a walk observed, in the key style of the entrance publishing it.

   ONE publication rule for EVERY receipt shape. A receipt that carries the
   escaping-path count, the collapsed link chain and the oversized names when
   the census succeeds, and drops them when it refuses, hides exactly the
   evidence the caller needs to understand a refusal: `no-fold-arms-found` on a
   tree whose only sources were skipped for size looks identical to
   `no-fold-arms-found` on an empty directory.

   `style` is `:kebab` for the CLI's EDN receipt and `:snake` for the tool's
   JSON one. The facts are the same facts; only the spelling differs.

   A fact the walk did not observe is not published: a census that reached no
   source twice publishes no `duplicates_collapsed` and one that skipped
   nothing publishes no `oversized_skipped` (MCP-OP-CENSUS-028/030). The
   `files-scanned` count is published whenever the walk knows it, zero
   included — zero scanned files is a fact, not an absence."
  [{:keys [files-scanned skipped-outside-root duplicates oversized]} style]
  (let [oversized (vec oversized)
        facts (cond-> {}
                (some? files-scanned)
                (assoc :files-scanned files-scanned)

                (pos? (or skipped-outside-root 0))
                (assoc :skipped-outside-root skipped-outside-root)

                (pos? (or duplicates 0))
                (assoc :duplicates-collapsed duplicates)

                (seq oversized)
                (assoc :oversized-skipped {:count (count oversized)
                                           :files (vec (take max-listed-files
                                                             oversized))
                                           :maximum max-source-bytes}
                       ;; A list bounded in silence reads as complete.
                       :oversized-skipped-omitted
                       (max 0 (- (count oversized) max-listed-files))))]
    (if (= :snake style)
      (into {} (map (fn [[k v]] [(get discovery-fact-keys k k) v])) facts)
      facts)))

(def source-name-pattern
  "The file names both entrances treat as candidate Clojure sources."
  #"\.clj[cs]?$")

;; @spec MCP-OP-CENSUS-016
(defn coerce-pool-size
  "Pure pool-size kernel shared by the MCP tool and the CLI op.

   Accepts an integer or its decimal digits (the CLI hands every value over as
   a string). Returns {:ok true :size n} or a typed reason.

   MAGNITUDE IS DECIDED BEFORE ANY COERCION, and the ordering is the whole
   point. Sol's round-eleven item 6, blocking: this kernel asked its range
   question THROUGH `(long value)`, so `pool_size` 9223372036854775808 — a
   perfectly ordinary JSON integer, and exactly the magnitude the bound exists
   to refuse — threw `IllegalArgumentException: Value out of range for long`
   out of `execute-request!` instead of returning `pool-size-out-of-range`. A
   bound enforced by an exception is not a bound: the caller gets a stack
   trace rather than a typed reason, the maximum, and a continuation, and the
   refusal contract MCP-OP-CENSUS-014 states never runs.

   So the comparison runs on arbitrary-precision integers and `long` is
   reached only once the value is known to fit. The CLI's text spelling is
   read the same way — any number of digits, sign included — because
   `:threads 9223372036854775808` and `:threads -1` are integers that are OUT
   OF RANGE, not values that are not integers, and telling the caller the
   wrong one of those two things sends them to fix the wrong thing.

   `:value` is published as text when the magnitude does not fit a long, so
   the refusal survives the wire it is published on."
  [value]
  (let [text (str/trim (str value))
        parsed (cond
                 (integer? value) (bigint value)
                 (re-matches #"[+-]?\d+" text)
                 (try (bigint (java.math.BigInteger. text))
                      (catch Exception _ nil))
                 :else nil)
        publishable (fn [n]
                      (if (and (<= (bigint Long/MIN_VALUE) n)
                               (<= n (bigint Long/MAX_VALUE)))
                        (long n)
                        (str n)))]
    (cond
      (nil? parsed) {:ok false :reason :not-an-integer :value text}
      (or (< parsed 1) (> parsed max-pool-size))
      {:ok false :reason :out-of-range
       :value (publishable parsed)
       :maximum max-pool-size}
      :else {:ok true :size (long parsed)})))

;; @spec MCP-OP-CENSUS-016
(defn effective-pool-size
  "The pool a census may actually use: never more than the box has processors.

   The one function in this namespace that reads the runtime rather than its
   arguments; it changes elapsed time and never the answer."
  [requested]
  (max 1 (min (long requested) (.availableProcessors (Runtime/getRuntime)))))

(def default-doors
  "Identity doors: a write routed through one of these is already keyed."
  #{'conj-once 'cons-once 'upsert-by 'conj-distinct-by 'cons-distinct-by})

;; ---------------------------------------------------------------------------
;; The shared request-shape refusal table
;;
;; @spec MCP-OP-CENSUS-016
;; @spec MCP-OP-CENSUS-029
;;
;; ONE table, read by BOTH entrances. Each row names one logical field, one
;; violation of it, the PREDICATE that decides the violation, and the name
;; each entrance publishes when it refuses. A field the two entrances would
;; otherwise be free to disagree about — Sol's round-ten item 6, where the
;; tool refused `format` and `max_files` as unknown fields while the CLI
;; accepted them SILENTLY — cannot disagree here, because there is only one
;; place to say what the field is.
;;
;; What the entrances are still allowed to differ on is SPELLING, and the
;; difference is real: the tool takes JSON arrays over the wire, the CLI takes
;; one comma-separated `:doors` string and one `:file`. `normalise-request`
;; is where that difference lives and where it ends; every predicate below
;; reads the normalised request, not the caller's.
;;
;; A row whose `:cli` (or `:mcp`) entry is a map rather than a keyword is one
;; that entrance CANNOT express — it is not an oversight, and the map says
;; why. `:file` is one path, so it has no list to be empty or oversized;
;; splitting a `:doors` string yields strings, so no CLI entry can have the
;; wrong type. Those are the only such rows, and the parity witness asserts
;; each one is genuinely inexpressible rather than merely unimplemented.
;; ---------------------------------------------------------------------------

(def mcp-census-fields
  "The census fields the `relation_census` tool accepts."
  #{:files :doors :pool_size})

(def mcp-routing-fields
  "The routing field the tool accepts.

   `workspace_root` is a routing field, not a census field: it is validated
   and canonicalised later, by `workspace/resolve-request`. The shape pass
   must not treat its presence as an unknown field nor its value as anything
   to check — checking it would mean resolving it, which is exactly the
   filesystem work this pass runs before."
  #{:workspace_root})

(def cli-census-fields
  "The arguments the `:relation-census` CLI op accepts.

   The op registry's own `:args` map must name exactly these; a witness
   asserts it, so a new argument cannot be added to the CLI without this
   table learning about it and the shape pass refusing everything else."
  #{:dir :file :doors :threads})

(def cli-dispatch-fields
  "Arguments the CLI DISPATCH owns rather than the op.

   `core/run` is handed the parsed argument map whole, `:op` included, and
   `--help` is answered before dispatch but survives in the map. Neither is
   an unknown field."
  #{:op :help})

(declare parse-doors)

(defn cli-door-names
  "The door names one CLI `:doors` argument denotes. Pure.

   The limit of -1 keeps trailing empties, so `\"conj-once,\"` names two
   doors, the second blank, and is refused rather than silently trimmed to
   one."
  [value]
  (mapv str/trim (str/split (str value) #"," -1)))

(defn normalise-request
  "One census request in the shape the shared refusal table checks.

   Normalising is the ONLY place the two entrances' spellings are allowed to
   differ. Everything after it is one implementation."
  [entrance params]
  (let [unknown (fn [accepted]
                  (vec (sort (map name (remove accepted (keys params))))))]
    (case entrance
      :mcp {:entrance :mcp
            :params params
            :unknown (unknown (into mcp-census-fields mcp-routing-fields))
            ;; The PATH-valued arguments, in the order this entrance would
            ;; use them. Only their decodability is checked here; where they
            ;; point is still `workspace/resolve-request`'s question, and
            ;; answering it would mean the filesystem work this pass runs
            ;; before. `workspace_root` is the tool's only path argument:
            ;; `files` entries are project-relative names resolved through
            ;; the fence, not anchors.
            :paths (if (string? (:workspace_root params))
                     [{:argument "workspace_root"
                       :value (:workspace_root params)}]
                     [])
            ;; The tool has no `:dir`. Its anchor is `workspace_root`, a
            ;; ROUTING field this pass deliberately does not validate: checking
            ;; it would mean resolving it, which is the filesystem work this
            ;; pass runs before. So the `:dir` row is absent here, and the
            ;; table's `:mcp` column says so rather than leaving it unstated.
            :dir {:present? false}
            :doors {:present? (some? (:doors params))
                    :value (:doors params)
                    :entries (:doors params)}
            :files {:present? (some? (:files params))
                    :value (:files params)
                    :entries (:files params)}
            :pool-size {:present? (contains? params :pool_size)
                        :value (:pool_size params)}}
      :cli (let [doors (:doors params)
                 file (:file params)]
             {:entrance :cli
              :params params
              :unknown (unknown (into cli-census-fields cli-dispatch-fields))
              ;; Both of the CLI's ANCHOR arguments. `cli-anchor` prefers
              ;; `:file` when one is named, so both can decide where a census
              ;; points and both must be decodable.
              :paths (into []
                           (comp (filter (comp string? second))
                                 (map (fn [[k v]] {:argument (str k) :value v})))
                           [[:dir (:dir params)] [:file file]])
              :dir {:present? (contains? params :dir)
                    :value (:dir params)}
              :doors {:present? (some? doors)
                      :value doors
                      :entries (when (string? doors) (cli-door-names doors))}
              :files {:present? (some? file)
                      :value file
                      :entries (when (some? file) [file])}
              :pool-size {:present? (some? (:threads params))
                          :value (:threads params)}}))))

(def replacement-character
  "U+FFFD: what a decoder writes when the bytes it was handed were not text
   in the encoding it was told to use."
  "\ufffd")

(defn argv-encoding
  "The encoding the runtime used to decode this process's arguments.

   Named in every not-decodable refusal, because it is the setting that
   decides whether a given filename survives the trip into the process at
   all, and the caller cannot see it from outside."
  []
  (or (System/getProperty "sun.jnu.encoding")
      (System/getProperty "file.encoding")
      "unknown"))

;; @spec MCP-OP-CENSUS-014
;; @spec MCP-OP-CENSUS-016
(defn undecodable-path
  "The first path argument of `req` whose value carries U+FFFD, or nil.

   Sol's round-twelve item 2: a directory whose name ended in the raw byte
   `0xff` was handed to the CLI, and the census answered `no-fold-arms-found`
   with `files-scanned 0` about a path that does not exist. The raw byte is
   NOT detectable here — the JVM decodes argv before `-main` runs, so this
   process only ever holds the decoded string — and it is not recoverable
   either, because the replacement is lossy: every undecodable byte becomes
   this one character. So there is nothing to canonicalise, nothing to carry
   into a continuation, and no way to name the directory the caller meant.

   What IS detectable is the replacement character, and this is where it is
   detected. A path that LEGITIMATELY contains U+FFFD is caught by the same
   test, and that is a stated cost rather than an oversight: after decoding
   the two are the same character, and a refusal that explains itself is
   worth more than a census of the wrong directory that nothing can detect."
  [req]
  (first (filter #(and (string? (:value %))
                       (str/includes? (:value %) replacement-character))
                 (:paths req))))

(defn not-decodable-message
  "What a not-decodable refusal says it found."
  [{:keys [argument value]}]
  (str argument " did not decode as text: it carries U+FFFD, the replacement "
       "character this runtime writes for bytes that are not valid "
       (argv-encoding) " (got " (pr-str value) ")"))

(defn not-decodable-remedy
  "Why no continuation exists for a path that did not decode, and what to do.

   It states the rule's cost out loud. A caller whose directory really is
   named with a U+FFFD in it is refused by this rule too, and is owed the
   reason rather than left to guess."
  [{:keys [argument]}]
  (str "The bytes " argument " was given did not decode as " (argv-encoding)
       " text; they reached this process as U+FFFD, and that replacement is "
       "lossy — every undecodable byte becomes the same character — so the "
       "path the caller meant cannot be reconstructed, named, or carried "
       "into a continuation, and none is offered. Rename the target to a "
       "name valid in " (argv-encoding) ", or start the process with "
       "-Dsun.jnu.encoding set to the encoding the name is actually in. A "
       "path that legitimately CONTAINS U+FFFD is refused by this rule too: "
       "after decoding, a real U+FFFD and a decoding failure are the same "
       "character, and this refusal prefers a refusal it can explain to a "
       "census of the wrong directory it cannot detect."))

(def ^:private known-door-list
  (delay (str/join "," (sort (map str default-doors)))))

(defn- doors-ok?
  "True when `pred` holds for the normalised doors, or there are none to check."
  [req pred]
  (let [{:keys [present? entries]} (:doors req)]
    (or (not present?) (not (sequential? entries)) (pred entries))))

(def request-shape-rules
  "The ONE ordered refusal table both census entrances validate against.

   Order is DECODABILITY first — a path that did not survive the trip into
   the process is a request no later row can narrow (Sol's round-thirteen
   item 2) — then the CLI's anchor, then the order MCP-OP-CENSUS-016 states:
   unknown fields, then `doors`, then `files`, then `pool_size`. Both
   entrances refuse on the FIRST row that fails.

   Per row: `:predicate` is true when the request is ACCEPTABLE for that row.
   `:mcp` and `:cli` are the names the two entrances publish; a map instead
   of a keyword means that entrance cannot express the violation and says
   why. `:cli-message`, `:cli-data` and `:cli-fix` are the CLI's refusal
   payload — the CLI validator below is driven entirely from this table. The
   tool's validator keeps its own `cond` (its per-branch payloads differ)
   but takes its predicates and its published names from these same rows, so
   a rename or a loosened bound reaches both entrances at once."
  [;; @spec MCP-OP-CENSUS-014
   ;; @spec MCP-OP-CENSUS-029
   ;; DECODABILITY IS ASKED FIRST OVERALL, on both entrances, ahead of every
   ;; other shape question — unknown fields included.
   ;;
   ;; Sol's round-thirteen item 2, blocking: this row used to sit fourth, and
   ;; U+FFFD ALONE was refused correctly while U+FFFD BESIDE a `bogus` field
   ;; was refused as `unknown-fields`/`unknown-arguments` — refusals that DO
   ;; compute a continuation — and the continuation carried the corrupted
   ;; spelling back for the caller to replay.
   ;;
   ;; The rule, stated once: A CONTINUATION IS A NARROWING OF THE REQUEST, and
   ;; a request whose path did not decode HAS no faithful narrowing. The bytes
   ;; are gone, the replacement is lossy, and anything carried names a
   ;; different directory or none at all. So this is not a severity ordering;
   ;; it is a precondition. Every row after this one builds its continuation
   ;; out of a path argument, so every row after this one is asking its
   ;; question of a value that no longer denotes anything, and answering it
   ;; produces a refusal whose continuation is a silent retarget.
   ;;
   ;; This is the ONE thing the tool's shape pass checks about
   ;; `workspace_root`, and it does not break the rule the `:dir` row states.
   ;; Deciding WHERE a path points means resolving it — the filesystem work
   ;; this pass runs before. Deciding whether the caller's argument is text at
   ;; all is a string inspection that touches nothing, which is exactly why it
   ;; can be asked before everything else.
   {:field :paths
    :violation :not-decodable
    :predicate (fn [req] (nil? (undecodable-path req)))
    :mcp :workspace-root-not-decodable
    :mcp-message (fn [req] (not-decodable-message (undecodable-path req)))
    :mcp-data (fn [req]
                {:argument (:argument (undecodable-path req))
                 :encoding (argv-encoding)})
    ;; NO continuation, at either entrance. A continuation carries the path
    ;; the caller named, and the bytes of that path are gone: what could be
    ;; carried is the corrupted spelling, which names a different directory
    ;; or none at all. That is the silent retarget MCP-OP-CENSUS-014 forbids,
    ;; so the refusal offers a remedy instead.
    :mcp-fix :uncomputable
    :mcp-remedy (fn [req] (not-decodable-remedy (undecodable-path req)))
    :cli :dir-not-decodable
    :cli-message (fn [req] (not-decodable-message (undecodable-path req)))
    :cli-data (fn [req]
                {:argument (:argument (undecodable-path req))
                 :encoding (argv-encoding)})
    :cli-fix :uncomputable
    :cli-remedy (fn [req] (not-decodable-remedy (undecodable-path req)))}

   {:field :unknown-fields
    :violation :present
    :predicate (fn [req] (empty? (:unknown req)))
    :mcp :unknown-fields
    :mcp-message (fn [req]
                   (str "relation_census does not accept "
                        (str/join ", " (:unknown req))))
    ;; The accepted list is what the caller retries with, so it names every
    ;; field this tool accepts — the ROUTING field included. Sol's round-nine
    ;; item 6: advertising only the census fields tells a caller that the
    ;; workspace_root it legitimately supplied is not accepted, and
    ;; workspace_root is the field that decides which tree gets censused.
    :mcp-data (fn [req]
                {:unknown (:unknown req)
                 :accepted (vec (sort (map name (into mcp-census-fields
                                                      mcp-routing-fields))))})
    :cli :unknown-arguments
    :cli-message (fn [req]
                   (str ":op :relation-census does not accept "
                        (str/join ", " (map #(str ":" %) (:unknown req)))))
    :cli-data (fn [_req]
                {:accepted (vec (sort (map #(str ":" (name %))
                                           cli-census-fields)))})
    :cli-fix :none}

   ;; The CLI's ANCHOR, and it is checked ahead of `doors`, `files` and
   ;; `threads` — and behind decodability alone, for the same reason stated
   ;; one row up — because every refusal below
   ;; builds its continuation from this value (`cli-anchor`), so a request
   ;; whose anchor is unusable cannot produce a faithful continuation for any
   ;; later row. Sol's round-eleven item 8: `:dir [1]` returned a generic
   ;; `:invalid-arguments` from an `as-file` coercion three frames down, and
   ;; `:dir ""` was worse than untyped — it stat'ed the config ancestors,
   ;; SCANNED THE CWD, and reported success about whatever directory the
   ;; process happened to be standing in.
   {:field :dir
    :violation :type
    ;; NON-EMPTY, not non-blank. Sol's round-twelve item 1: a path is the
    ;; bytes the caller gave, and `"   "` is a legal — if peculiar — relative
    ;; path that resolves against the cwd like any other. The EMPTY string is
    ;; the one value that names nothing: POSIX gives the empty pathname no
    ;; meaning and every syscall answers it with ENOENT. Refusing whitespace
    ;; here would be a normalisation wearing a refusal's clothes.
    :predicate (fn [req]
                 (let [{:keys [present? value]} (:dir req)]
                   (or (not present?)
                       (and (string? value) (not= "" value)))))
    :mcp {:inexpressible
          (str "the tool has no :dir; its anchor is workspace_root, a ROUTING "
               "field this pass deliberately does not validate, because "
               "checking it would mean resolving it — the filesystem work "
               "this pass runs before. workspace/resolve-request owns it, "
               "and refuses it as invalid-workspace-root")}
    :cli :dir-not-a-string
    :cli-message (fn [req]
                   (str ":dir must be a non-empty path (got "
                        (pr-str (:value (:dir req))) ")"))
    :cli-data (fn [req] {:value (:value (:dir req))})
    ;; No continuation exists for this row, and that is the honest answer.
    ;; The anchor is the one thing a CLI continuation is built OUT OF, so a
    ;; request whose anchor is unusable has no narrower call to compute: the
    ;; only command that could be offered would anchor on the cwd, which is
    ;; the silent retarget MCP-OP-CENSUS-014 forbids and precisely the
    ;; accident `:dir ""` was already committing.
    :cli-fix :uncomputable
    :cli-remedy (fn [req]
                  (str ":dir " (pr-str (:value (:dir req)))
                       " names no workspace, and a continuation is built out "
                       "of the workspace the caller named, so no narrower "
                       "command can be computed: retry with :dir naming a "
                       "directory, or name one source with :file."))}

   {:field :doors
    :violation :container-type
    ;; The tool is handed a JSON array; the CLI is handed one comma-separated
    ;; string. Same question — "is this a doors LIST at all?" — asked of the
    ;; two shapes the two wires can carry.
    :predicate (fn [req]
                 (let [{:keys [present? value]} (:doors req)]
                   (or (not present?)
                       (if (= :cli (:entrance req))
                         (string? value)
                         (sequential? value)))))
    :mcp :doors-not-an-array
    :mcp-message (fn [_req] "doors must be a JSON array of symbols")
    :mcp-data (fn [_req] {})
    :cli :doors-not-a-string
    :cli-message (fn [req]
                   (str ":doors must be a comma-separated string of door "
                        "names (got " (pr-str (:value (:doors req))) ")"))
    :cli-data (fn [req] {:value (:value (:doors req))})
    :cli-fix :doors}

   {:field :doors
    :violation :too-many
    :predicate (fn [req] (doors-ok? req #(<= (count %) max-doors)))
    :mcp :too-many-doors
    :mcp-message (fn [_req] "doors exceeds the maximum door count")
    :mcp-data (fn [req] {:maximum max-doors
                         :actual (count (:value (:doors req)))})
    :cli :too-many-doors
    :cli-message (fn [req]
                   (str ":doors names "
                        (count (:entries (:doors req)))
                        " doors; the maximum is " max-doors))
    :cli-data (fn [req] {:maximum max-doors
                         :actual (count (:entries (:doors req)))})
    :cli-fix :doors}

   {:field :doors
    :violation :entry-type
    :predicate (fn [req] (doors-ok? req #(every? string? %)))
    :mcp :doors-not-strings
    ;; Refused BEFORE `files` and `pool_size` are even looked at, and before
    ;; the oversized-source branch of execute-in-context! can copy `doors`
    ;; UNCHANGED into a next_call. A non-string entry that survived to that
    ;; branch produced an unexecutable continuation: the schema rejects it,
    ;; even though this validator had not yet refused it.
    :mcp-message (fn [_req] "every entry in doors must be a JSON string")
    :mcp-data (fn [req]
                (let [doors (:value (:doors req))
                      index (first (keep-indexed
                                     (fn [i d] (when-not (string? d) i))
                                     doors))]
                  {:index index :value (nth doors index)}))
    :cli {:inexpressible
          (str "the CLI's :doors is one string split on commas, so every "
               "entry it yields is a string by construction; a CLI :doors "
               "value that is not a string is refused one row earlier, as "
               ":doors-not-a-string")}}

   {:field :doors
    :violation :vocabulary
    ;; The syntactic half of the door check: is this name a symbol at all,
    ;; and does it shadow a collection write head? Whether a door is DEFINED
    ;; can only be answered after a scan, so THAT half is not a shape rule and
    ;; is not here; it stays after discovery, on both entrances, and keeps the
    ;; discovery facts a post-scan refusal is owed.
    ;;
    ;; This half needs no scan at all. Both questions are decided against
    ;; `'#{conj cons into concat}` and `default-doors`, two compile-time sets,
    ;; and `parse-doors` is called with `declared` nil precisely so it asks
    ;; only what a pure pass can answer. Sol's round-twelve item 10: the row
    ;; used to carry `:mcp-phase :post-discovery`, so the tool's shape walk
    ;; skipped it and the CLI's applied it, and `doors=conj, file=""` refused
    ;; `unknown-door-symbol` at one entrance and `file-not-a-string` at the
    ;; other. A pure predicate answered later is not a better answer; it is
    ;; the same answer, on an entrance the other one no longer matches. The
    ;; facts it bought are facts about a walk the caller's request had already
    ;; disqualified.
    :predicate (fn [req]
                 (doors-ok? req #(or (not (every? string? %))
                                     (not (map? (parse-doors % nil))))))
    :mcp :unknown-door-symbol
    :mcp-message (fn [req]
                   (let [bad (parse-doors (:entries (:doors req)) nil)]
                     (str "Unknown identity door " (:invalid bad) ": "
                          (:why bad))))
    :mcp-data (fn [req]
                (let [bad (parse-doors (:entries (:doors req)) nil)]
                  {:door (:invalid bad)
                   :known_doors (vec (sort (map str default-doors)))}))
    :cli :unknown-door-symbol
    :cli-message (fn [req]
                   (let [bad (parse-doors (:entries (:doors req)) nil)]
                     (str "Unknown identity door " (:invalid bad) ": "
                          (:why bad))))
    :cli-data (fn [req]
                (let [bad (parse-doors (:entries (:doors req)) nil)]
                  {:door (:invalid bad)
                   :known-doors (vec (sort (map str default-doors)))}))
    :cli-fix :doors}

   {:field :files
    :violation :container-type
    :predicate (fn [req]
                 (let [{:keys [present? value]} (:files req)]
                   (or (not present?) (sequential? value))))
    :mcp :files-not-an-array
    :mcp-message (fn [_req] "files must be a JSON array of paths")
    :mcp-data (fn [_req] {})
    :cli {:inexpressible
          (str "the CLI names at most one source, with :file; there is no "
               "list whose container could have the wrong type")}}

   {:field :files
    :violation :empty
    :predicate (fn [req]
                 (let [{:keys [present? value]} (:files req)]
                   (or (not present?) (not (sequential? value)) (seq value))))
    :mcp :empty-file-list
    :mcp-message (fn [_req]
                   (str "files must name at least one path; omit files to "
                        "census the tree"))
    :mcp-data (fn [_req] {})
    :cli {:inexpressible
          (str "the CLI names at most one source, with :file; an absent "
               ":file censuses the tree and is not an empty list")}}

   {:field :files
    :violation :too-many
    :predicate (fn [req]
                 (let [{:keys [present? value]} (:files req)]
                   (or (not present?)
                       (not (sequential? value))
                       (<= (count value) max-requested-files))))
    :mcp :too-many-files
    :mcp-message (fn [_req] "files exceeds the maximum file count")
    :mcp-data (fn [req] {:maximum max-requested-files
                         :actual (count (:value (:files req)))})
    :cli {:inexpressible
          (str "the CLI names at most one source, with :file, so its file "
               "count can never exceed the maximum")}}

   {:field :files
    :violation :entry-type
    :predicate (fn [req]
                 (let [{:keys [present? entries]} (:files req)]
                   (or (not present?)
                       (not (sequential? entries))
                       (every? #(and (string? %) (not (str/blank? %)))
                               entries))))
    :mcp :file-not-a-string
    :mcp-message (fn [_req] "every entry in files must be a non-blank string")
    :mcp-data (fn [_req] {})
    :cli :file-not-a-string
    :cli-message (fn [req]
                   (str ":file must be a non-blank path (got "
                        (pr-str (:value (:files req))) ")"))
    :cli-data (fn [req] {:value (:value (:files req))})
    :cli-fix :none}

   {:field :pool-size
    :violation :not-an-integer
    ;; The wire carries JSON, so the tool accepts only a JSON integer and
    ;; refuses a string, float, boolean, null or array. The CLI hands every
    ;; argument over as a string, so its kernel reads decimal digits — the
    ;; documented spelling difference, not a looser bound: both run the same
    ;; `coerce-pool-size`, and both refuse everything that is not an integer
    ;; in range.
    :predicate (fn [req]
                 (let [{:keys [present? value]} (:pool-size req)]
                   (or (not present?)
                       (if (= :cli (:entrance req))
                         (not= :not-an-integer (:reason (coerce-pool-size value)))
                         (integer? value)))))
    :mcp :pool-size-not-an-integer
    :mcp-message (fn [_req]
                   (str "pool_size must be a JSON integer between 1 and "
                        max-pool-size))
    :mcp-data (fn [req] {:maximum max-pool-size
                         :value (:value (:pool-size req))})
    :cli :invalid-pool-size
    :cli-message (fn [req]
                   (str ":threads must be an integer between 1 and "
                        max-pool-size
                        " (got " (pr-str (:value (:pool-size req))) ")"))
    :cli-data (fn [_req] {})
    :cli-fix :threads}

   {:field :pool-size
    :violation :out-of-range
    :predicate (fn [req]
                 (let [{:keys [present? value]} (:pool-size req)]
                   (or (not present?)
                       (let [coerced (coerce-pool-size value)]
                         (or (:ok coerced)
                             (not= :out-of-range (:reason coerced)))))))
    :mcp :pool-size-out-of-range
    :mcp-message (fn [_req]
                   (str "pool_size must be between 1 and " max-pool-size))
    :mcp-data (fn [req]
                {:maximum max-pool-size
                 :value (:value (coerce-pool-size (:value (:pool-size req))))})
    ;; The CLI publishes ONE name for both pool-size violations, and has
    ;; since the op shipped; its message names the bound and the value, so
    ;; the caller is not told less. This is the documented many-to-one half
    ;; of the CLI/MCP name mapping, and the parity witness uses it
    ;; explicitly rather than papering over it.
    :cli :invalid-pool-size
    :cli-message (fn [req]
                   (str ":threads must be an integer between 1 and "
                        max-pool-size
                        " (got " (pr-str (:value (:pool-size req))) ")"))
    :cli-data (fn [_req] {})
    :cli-fix :threads}])

;; @spec MCP-OP-CENSUS-014
(def cli-refusal-types
  "Every typed refusal the `:relation-census` CLI op can return once its
   arguments have been parsed into an opts map.

   Sol's round-eleven item 2, blocking: round ten routed the SHAPE refusals
   through `cli-anchor`/`cli-continuation` and left the POST-SCAN ones
   spelling their own command, so a door that parses as a symbol but is
   defined in no scanned file — a question only the scan can answer — still
   handed back the literal `:dir .`, and replaying it censused the replay's
   cwd. A rule that lives in one branch is a rule the other branches break.

   This set is the CLI's half of the anchor contract, written down so a
   witness can ENUMERATE it: every name here is driven through the entrance
   and asserted to name the workspace the caller named and to build any
   continuation it carries through `cli-continuation`. A refusal added to the
   op without a probe fails that witness rather than shipping unexercised.

   `:duplicate-argument` is NOT here: it is raised by `parse-args`, before
   dispatch knows which op it is building, so there is no request and no
   anchor to name."
  #{:unknown-arguments
    :dir-not-a-string
    :dir-not-decodable
    :doors-not-a-string
    :too-many-doors
    :unknown-door-symbol
    :file-not-a-string
    :file-not-found
    ;; Sol's round-fourteen item 7. A separate name from `:file-not-found`
    ;; because the remedy differs: the file is THERE, and what must change is
    ;; what may read it. A `:cause` field on a shared name would also be
    ;; invisible to the enumeration witness below, which drives on the name.
    :file-not-readable
    ;; Sol's round-fifteen items 2 and 8. A FIFO, a socket, or a DIRECTORY
    ;; carrying a source name is neither missing nor unreadable: `fs/readable?`
    ;; is true of a named pipe, and `slurp` on one blocks forever with no
    ;; writer. Its own name because its remedy is its own — the path is not a
    ;; file at all — and because a name is what the enumeration witness can
    ;; see.
    :file-not-a-regular-file
    :invalid-pool-size
    :source-too-large
    :too-many-walk-entries
    :too-many-candidate-files
    :no-fold-arms-found
    :unparseable-file
    :census-worker-failure
    ;; Sol's round-fifteen NO-GO item 3. Anything that escapes the census path
    ;; used to reach the launcher's catch-all and be stamped
    ;; `:invalid-arguments`, which is in NEITHER declared set — so every
    ;; witness pinned to them was green over a whole class of answers this op
    ;; can give. These are the names the MCP entrance already publishes for the
    ;; same two events, because a throw is not a different KIND of event at the
    ;; two entrances, and declaring them here is what makes the enumeration
    ;; TOTAL rather than a subset.
    :census-adapter-failure
    :census-resource-exhausted})

;; @spec MCP-OP-CENSUS-014
(def mcp-refusal-types
  "Every typed refusal the `relation_census` MCP tool can return once its
   request has passed the ordered shape pass.

   The sibling of `cli-refusal-types`, and it exists for the reason that one
   already proved: Sol's round-fourteen item 9 found the 512-byte continuation
   ceiling enforced at ONE of the tool's eight construction sites, and no
   witness could have caught it, because the CLI's refusals were enumerated
   and the tool's were not. A rule that lives in one branch is a rule the
   other branches break; a set nobody wrote down is a set nobody can drive.

   Every name here is driven through the tool against a workspace root of at
   least 600 characters and asserted to publish either a continuation that
   fits the wire or a remedy that says what it measured — and to have built
   any continuation it carries through the ONE constructor, counted through a
   redefinition of that constructor rather than by reading the source.

   The SHAPE refusals are not here. They all publish `error_type`
   `invalid-mcp-request` and their specific name in `reason`, and the shared
   `request-shape-rules` table already enumerates them for both entrances;
   the ceiling witness drives that table too.

   `:census-failed` IS here even though the two plan failures this version can
   produce both name themselves: it is the fallback the tool would publish for
   a plan failure that does not, and a fallback nobody drives is a refusal
   shape nobody has ever seen."
  #{:invalid-workspace-root
    :unknown-door-symbol
    :unreadable-source-path
    :source-too-large
    :no-fold-arms-found
    :unparseable-file
    :census-worker-failure
    :census-failed
    :too-many-candidate-files
    :too-many-walk-entries
    :census-adapter-failure
    :census-resource-exhausted
    :server-not-initialized})

(def ^:dynamic *shape-rules*
  "An override for the shared refusal table, or nil for the real one.

   Bound ONLY by witnesses, and it exists because of Sol's round-eleven item
   3. The parity witness could prove that both entrances read the table's
   PREDICATES — widening one predicate failed both entrances — but it could
   not prove they read the table's ORDER, and they did not: moving `files`
   before `doors` in the table left the witness green while the tool still
   refused `doors` and the CLI still refused `file`. A property that can only
   be tested by editing the source and re-reading the diff is a property
   nothing enforces. This var makes the mutation injectable, so the witness
   can assert that reordering the table reorders BOTH entrances' refusals."
  nil)

(defn shape-rules
  "The refusal table in force: the injected one when a witness has bound one."
  []
  (or *shape-rules* request-shape-rules))

(defn shape-rule
  "One row of the shared refusal table, by field and violation."
  [field violation]
  (first (filter #(and (= field (:field %)) (= violation (:violation %)))
                 (shape-rules))))

(defn shape-name
  "The name `entrance` publishes for one row of the shared table, or nil when
   that entrance cannot express the violation."
  [entrance field violation]
  (let [published (get (shape-rule field violation) entrance)]
    (when (keyword? published) published)))

(defn shape-violated?
  "True when the normalised request violates one row of the shared table."
  [req field violation]
  (not ((:predicate (shape-rule field violation)) req)))

;; @spec MCP-OP-CENSUS-014
;; @spec MCP-OP-CENSUS-019
(defn cli-anchor
  "Where a CLI continuation must point: the workspace the caller named, made
   absolute WITHOUT touching the filesystem.

   Sol's round-ten item 5, blocking: every CLI shape refusal handed back the
   fixed `:dir .`, so a caller who named an absolute workspace and replayed
   the continuation from anywhere else censused THAT directory instead — a
   continuation that validates, runs, reports success, and answers about a
   tree the caller never named. `.` is not a workspace; it is whatever the
   next shell happens to be standing in.

   Absolute is computed from `user.dir`, not from `getCanonicalPath`:
   canonicalising is a `realpath`, and this pass runs before any filesystem
   call. So a relative `:dir` becomes its resolution against the cwd it was
   resolved in, and the refusal SAYS so in `:resolved-against` rather than
   leaving the caller to guess which cwd a bare path meant.

   A PATH IS THE BYTES THE CALLER GAVE, and this function edits none of them.
   Sol's round-twelve item 1, blocking: it ran the caller's string through
   `str/trim` first, so `:dir \"/root \"` — a legal POSIX directory, since a
   filename may begin and end with spaces — anchored on `/root`, a DIFFERENT
   directory that existed and held two arm-bearing sources against the named
   root's one. The continuation carried the trimmed sibling, replayed without
   refusing, and reported success about a tree the caller never named. Note
   which half was already right: `census-root` absolutizes and canonicalises,
   both byte-preserving, so the census READ the named root while the anchor
   NAMED its sibling — one entrance disagreeing with itself about which
   directory the request meant.

   Absolutizing is the only transformation left, and it is the one the caller
   asked for: prefixing a relative path with the cwd adds bytes and removes
   none. Every remaining branch is a whole-string EQUALITY, not a trim: `.`
   and `\"\"` mean the cwd, `./x` means `cwd/x`, and anything else is
   appended verbatim."
  [{:keys [dir file]}]
  (let [cwd (System/getProperty "user.dir")
        ;; The empty string is the one path that names nothing — POSIX gives
        ;; the empty pathname no meaning and every syscall answers it with
        ;; ENOENT — so it is the only value that counts as "no :file was
        ;; named". `:file \"   \"` names a (peculiar) file and anchors on it;
        ;; demoting it to the directory would be the same silent retarget one
        ;; argument over.
        named-file? (and (string? file) (not= "" file))
        given (str (if named-file? file (if (some? dir) dir ".")))
        absolute (cond
                   (str/starts-with? given "/") given
                   (or (= "." given) (= "" given)) cwd
                   (str/starts-with? given "./") (str cwd "/" (subs given 2))
                   :else (str cwd "/" given))]
    (cond-> {:kind (if named-file? :file :dir)
             :given given
             :absolute absolute}
      (not= given absolute) (assoc :resolved-against cwd))))

(def ^:private shell-safe-token
  "Characters a POSIX shell passes through unchanged in an unquoted word.

   The same set `shlex.quote` uses: word characters plus `@%+=:,./-`. Note
   what is NOT here — space, quote, `;`, `$`, backtick, `&`, `|`, `<`, `>`,
   `*`, `?`, `(`, `)`, `[`, `]`, `{`, `}`, `!`, `#`, `~`, and every control
   character, newline included."
  #"[A-Za-z0-9_@%+=:,./-]+")

;; @spec MCP-OP-CENSUS-014
(defn shell-quote
  "One argv token, safe to paste into a POSIX shell.

   Single quotes, because inside them the shell interprets NOTHING — no
   expansion, no substitution, no word splitting, not even a backslash. The
   one character a single-quoted string cannot contain is a single quote, and
   the standard escape for it is to close the quote, emit an escaped quote,
   and reopen: `'` becomes `'\\''`.

   A token made entirely of safe characters is returned unquoted, so the
   ordinary continuation still reads as a command a human would type. That is
   a legibility choice and not a safety one: the predicate is a WHITELIST, so
   a character nobody thought of is quoted by default rather than passed
   through by default."
  [token]
  (let [text (str token)]
    (if (and (seq text) (re-matches shell-safe-token text))
      text
      (str "'" (str/replace text "'" "'\\''") "'"))))

;; @spec MCP-OP-CENSUS-014
(defn render-command
  "One argv vector as a shell-safe command line."
  [argv]
  (str/join " " (map shell-quote argv)))

(defn utf8-byte-count
  "The length of `text` in UTF-8 BYTES.

   Sol's round-twelve item 3: the continuation ceiling is named in bytes,
   documented in bytes and reported in bytes, and was enforced with `count`,
   which counts UTF-16 code units. The two agree on ASCII and diverge by a
   factor of three elsewhere — 490 characters of accented path measured 890
   bytes and was emitted under a 512-byte limit."
  ^long [text]
  (alength (.getBytes (str text) "UTF-8")))

(def max-refusal-field-chars
  "The longest any ONE field of a refusal may render, in characters.

   Opus's round-sixteen item 7. Receipts are capped at 4,096 bytes and
   continuations at 512; refusals were capped at nothing, so a 10,001-character
   `files` entry yielded a 30,763-byte tool refusal and a 50,612-byte CLI one —
   the caller's own bad input, echoed back four times, at the moment they are
   least able to read it.

   Characters and not bytes, deliberately: this bound exists to keep a refusal
   READABLE, and the ceiling that exists to keep a continuation TRANSMISSIBLE
   is measured in bytes because that is what a wire carries. Two different
   promises, two different units, neither borrowed from the other."
  1024)

(def refusal-continuation-keys
  "The refusal fields a bound must never touch.

   A continuation is an EXECUTABLE promise, and MCP-OP-CENSUS-014 already
   forbids putting anything in an argument position that does not execute: a
   truncated path does not fail, it names a DIFFERENT file. These fields carry
   their own bound — `max-next-call-bytes`, enforced at the one constructor per
   entrance — so a continuation is short by construction and there is nothing
   here for a length bound to do except damage."
  #{:next-command :next-command-argv :next_call})

(defn bound-refusal-text
  "One refusal field, bounded, saying so when it had to be.

   Truncation is honest only when it is VISIBLE: a silently shortened path
   leaves the caller comparing it against the one they sent and finding a
   difference the refusal never mentioned. So the marker names the original
   length, which is also the fact that explains the refusal in the
   name-too-long case."
  [text]
  (let [text (str text)]
    (if (<= (count text) max-refusal-field-chars)
      text
      (str (subs text 0 max-refusal-field-chars)
           "… [truncated: " (count text) " characters]"))))

(defn bound-refusal
  "Every string in a refusal, bounded, except the continuation it carries.

   Applied at each entrance's LAST step rather than at the sites that build
   the strings, for the reason MCP-OP-CENSUS-014 gives about the continuation
   constructor: a bound enforced at some of a namespace's construction sites is
   not a bound, it is those sites' habit, and the habit does not travel to the
   site added next round."
  [refusal]
  (if-not (map? refusal)
    refusal
    (reduce-kv
      (fn [acc k v]
        (assoc acc k
               (if (contains? refusal-continuation-keys k)
                 v
                 (walk/postwalk
                   #(if (string? %) (bound-refusal-text %) %)
                   v))))
      (empty refusal)
      refusal)))

;; @spec MCP-OP-CENSUS-014
(defn within-next-call-bytes?
  "True when one rendered continuation fits the shared ceiling.

   THE ONE PLACE that bound is decided, for both entrances and every refusal
   shape. Every consumer of a continuation measures bytes — argv is bytes, a
   JSON request body is bytes, a terminal line is bytes — so this measures
   bytes, once, rather than leaving each call site to reach for whatever
   length function is nearest."
  [text]
  (<= (utf8-byte-count text) max-next-call-bytes))

(defn cli-command-argv
  "The argv a CLI continuation would carry, before the size bound is applied.

   Separate from the bounded builder because the remedy that REPLACES an
   over-long continuation has to measure the thing it is declining to send."
  [anchor fix]
  (when (and anchor (not= :uncomputable fix))
    (into ["clj-surgeon" ":op" ":relation-census"
           (if (= :file (:kind anchor)) ":file" ":dir")
           (str (:absolute anchor))]
          (case fix
            :doors [":doors" @known-door-list]
            :threads [":threads" "8"]
            []))))

(defn cli-next-command-argv
  "The continuation one CLI refusal hands back, as ARGV, or nil when there is
   none.

   ARGV IS THE PRIMITIVE and the rendered string is derived from it, which is
   Sol's round-eleven item 5 stated as a design: this used to build a string
   by interpolation, so `:dir \"space root\"` produced a command whose replay
   returned `:invalid-arguments`, and a root containing `;printf INJECTED`
   became command-injection syntax in a string whose entire purpose is to be
   pasted into a shell. A continuation is an EXECUTABLE PROMISE;
   MCP-OP-CENSUS-014 forbids a caption in an argument position because a
   caption is unexecutable, and an unquoted path is the same defect one turn
   later — it executes, and it executes something else.

   `fix` names the narrowing the refusal suggests: `:doors` and `:threads`
   append the argument the caller must correct, `:none` narrows nothing but
   still anchors, and `:uncomputable` says there is no continuation at all —
   that anchor was itself the thing refused.

   The `max-next-call-bytes` bound is measured on the RENDERED string, not on
   the argv, because the rendered string is what the caller reads and runs."
  [anchor fix]
  (when-let [argv (cli-command-argv anchor fix)]
    (when (within-next-call-bytes? (render-command argv)) argv)))

;; @spec MCP-OP-CENSUS-014
(defn cli-continuation-overflow-remedy
  "Why an over-long continuation was replaced by advice, with what it measured.

   A refusal that names a bound without naming the value it compared against
   leaves the caller to guess how much shorter is short enough, so this states
   the measured length beside the ceiling."
  [anchor fix]
  (let [rendered (some-> (cli-command-argv anchor fix) render-command)]
    (str "The continuation this refusal would carry renders as "
         (if rendered (utf8-byte-count rendered) 0)
         " UTF-8 bytes, over the " max-next-call-bytes
         "-byte ceiling a continuation must fit, so no narrower command can "
         "be computed: run the census from a shorter path.")))

;; @spec MCP-OP-CENSUS-014
;; @spec MCP-OP-CENSUS-019
(defn cli-continuation
  "The continuation FIELDS one CLI refusal publishes, or nil when it has none.

   THE ONE PLACE a CLI continuation is built. Sol's round-eleven item 2,
   blocking: round ten routed the SHAPE refusals through the anchor and left
   the POST-SCAN ones spelling their own command, so an undefined door
   discovered after the scan still handed back the literal `:dir . :doors …`.
   Sol replayed that from another cwd and the census answered about the client
   fixture — the same silent retarget MCP-OP-CENSUS-014 forbids, from a site
   the fix never reached. A rule that lives in one branch is a rule the other
   branches break, so every refusal site calls this.

   BOTH spellings are published. `:next-command-argv` is the vector a program
   should exec — no shell, no parsing, nothing to get wrong.
   `:next-command` is that vector RENDERED shell-safe, because the string is
   what a human or an agent actually pastes, and the two must denote the same
   call."
  [anchor fix]
  (when-let [argv (cli-next-command-argv anchor fix)]
    {:next-command (render-command argv)
     :next-command-argv argv}))

;; @spec MCP-OP-CENSUS-014
;; @spec MCP-OP-CENSUS-016
;; @spec MCP-OP-CENSUS-019
;; @spec MCP-OP-CENSUS-029
(defn validate-cli-request-shape
  "Pure request-shape validation for the CLI/babashka relation-census request.

   Returns nil when the request's shape is acceptable, and the typed refusal
   otherwise. It reads its argument, `user.dir`, and nothing else: no path is
   resolved, no directory is stat'ed, no `.clj-surgeon.edn` is looked for.

   It exists so `clj-surgeon.core/run` can refuse a malformed request BEFORE
   it loads project aliases. Sol's round-nine finding was that ordering:
   `bb … :threads not-a-number` returned `invalid-pool-size` only after the
   entrance had stat'ed the workspace, stat'ed and read its
   `.clj-surgeon.edn`, and walked the ancestor chain for more — filesystem
   work on a request the tool had already decided it would not honour.
   MCP-OP-CENSUS-016 says \"before any filesystem work\", and config
   discovery is filesystem work.

   Round ten fixed that for `:threads` and for `:threads` alone: this
   function destructured `{:keys [threads]}` and read nothing else, so
   `bb … :doors conj` against a workspace with unparseable `.clj-surgeon.edn`
   still returned the EDN error (Sol's round-ten item 4). It now validates
   the WHOLE shape, and it validates it against `request-shape-rules`, the
   same table the tool's `validate-request-shape` reads, so the two
   entrances cannot disagree about which shapes are refusable.

   It remains a SIBLING of the tool's validator rather than the same
   function, and the reason is mechanical, not stylistic: that one lives in
   `clj-surgeon.mcp-relation-census`, which requires
   `clj-surgeon.census-pool` and through it claypoole, a dependency babashka
   cannot load (`Could not locate com/climate/claypoole.bb, …`). A require of
   it from `clj-surgeon.core` would not slow the bb entrance down, it would
   delete it. The TABLE is what the two share, and it is loadable by both.

   The continuation is a full command, not a caption in an argument position
   (MCP-OP-CENSUS-014), and it names the workspace the caller named, not the
   cwd the replay happens to run in (MCP-OP-CENSUS-014, again)."
  [params]
  (let [req (normalise-request :cli params)
        anchor (cli-anchor params)]
    (reduce
      (fn [_ {:keys [predicate cli cli-message cli-data cli-fix cli-remedy]
              :as _rule}]
        (if (or (not (keyword? cli)) (predicate req))
          nil
          (reduced
            (merge
              {:ok false
               :error-type cli
               :error (cli-message req)
               :anchor anchor}
              (cli-data req)
              (or
                (cli-continuation anchor cli-fix)
                {:remedy
                 (if cli-remedy
                   (cli-remedy req)
                   (cli-continuation-overflow-remedy anchor cli-fix))})))))
      nil
      (shape-rules))))

(def ^:private write-heads '#{conj cons into concat})

(def ^:private recognised-containers
  '#{let let* letfn loop do if if-not when when-not cond condp case
     if-let when-let if-some when-some -> ->> some-> some->> as->
     fn fn* defmethod update update-in update-vals swap! swap-vals!
     assoc assoc-in merge merge-with vec vector into set hash-map
     doto binding with-meta reduce})

(def ^:private recognised-test-heads
  '#{not and or = not= == some not-any? every? contains? get get-in seq empty?
     filter remove count zero? pos? neg? nil? some? boolean str keyword name
     fn fn* first second last nth peek identity complement partial ->})

;; ---------------------------------------------------------------------------
;; Node helpers
;; ---------------------------------------------------------------------------

(defn- inner? [n] (and (some? n) (node/inner? n)))

(defn- sig-children
  [n]
  (if (inner? n)
    (vec (remove node/whitespace-or-comment? (node/children n)))
    []))

(defn- call-node?
  [n]
  (and (inner? n) (contains? #{:list :fn} (node/tag n))))

(defn- token-sexpr
  [n]
  (when (and n (= :token (node/tag n)))
    (try (node/sexpr n) (catch Throwable _ nil))))

(defn- head-symbol
  [n]
  (when (call-node? n)
    (let [s (token-sexpr (first (sig-children n)))]
      (when (symbol? s) s))))

(defn- simple-name
  [s]
  (when (symbol? s) (symbol (name s))))

(defn- head-name
  [n]
  (simple-name (head-symbol n)))

(defn- line-of
  [n]
  (:row (meta n)))

(defn- source-line
  ([n] (source-line n 100))
  ([n limit]
   (let [text (-> (node/string n) (str/replace #"\s+" " ") str/trim)]
     (if (> (count text) limit)
       (str (subs text 0 (dec limit)) "…")
       text))))

(defn- safe-value
  [n]
  (try (node/sexpr n) (catch Throwable _ ::unreadable)))

(defn- node-seq
  [n]
  (tree-seq inner? sig-children n))

(defn- keyword-lookups
  "Keywords used as a lookup head: (:k x) and (get x :k)."
  [n]
  (when n
    (into #{}
          (comp
            (filter call-node?)
            (mapcat
              (fn [c]
                (let [kids (sig-children c)
                      k (token-sexpr (first kids))]
                  (cond
                    (keyword? k) [k]
                    (= 'get (simple-name k))
                    (keep #(let [v (token-sexpr %)] (when (keyword? v) v))
                          (rest kids))
                    :else nil)))))
          (node-seq n))))

(defn- all-keywords
  [n]
  (when n
    (into #{}
          (keep (fn [c] (let [v (token-sexpr c)] (when (keyword? v) v))))
          (node-seq n))))

;; ---------------------------------------------------------------------------
;; Stack: [{:node parent :index child-index-in-sig-children} ...]
;; ---------------------------------------------------------------------------

(defn- parent-node [stack] (:node (peek stack)))
(defn- index-in-parent [stack] (:index (peek stack)))

(defn- ancestor-frames
  "Outermost-first frames {:node :parent :index} for every stack entry."
  [stack]
  (mapv (fn [d]
          (let [entry (nth stack d)]
            {:node (:node entry)
             :parent (when (pos? d) (:node (nth stack (dec d))))
             :index (when (pos? d) (:index (nth stack (dec d))))
             :depth d
             :child-index (:index entry)}))
        (range (count stack))))

;; ---------------------------------------------------------------------------
;; Threading-aware argument lists
;; ---------------------------------------------------------------------------

(defn- form-args
  "Arg nodes of one call, with the implicit `->` threaded argument prepended.

   Returns nil when the threading shape is not supported."
  [n stack]
  (let [args (vec (rest (sig-children n)))
        p (parent-node stack)
        ph (head-name p)]
    (cond
      (nil? p) args
      (= '-> ph) (let [i (index-in-parent stack)
                       kids (sig-children p)]
                   (cond
                     (= i 2) (into [(nth kids 1)] args)
                     (< i 2) args
                     :else nil))
      (contains? '#{->> some-> some->> as->} ph) nil
      :else args)))

;; ---------------------------------------------------------------------------
;; Update-form shapes
;; ---------------------------------------------------------------------------

(defn- update-shape
  "Describe `(update m k f …)`, `(update-in m path f …)`, `(swap! a f …)`.

   Returns {:kind :base :path-nodes :fn-node :value-node} or nil."
  [n stack]
  (when-let [h (head-name n)]
    (when (contains? '#{update update-in swap!} h)
      (when-let [args (form-args n stack)]
        (case h
          update (when (>= (count args) 3)
                   {:kind :update :base (nth args 0)
                    :path-nodes [(nth args 1)] :fn-node (nth args 2)
                    :value-node (last args)})
          update-in (when (>= (count args) 3)
                      (let [pv (nth args 1)]
                        (when (= :vector (node/tag pv))
                          {:kind :update-in :base (nth args 0)
                           :path-nodes (sig-children pv) :fn-node (nth args 2)
                           :value-node (last args)})))
          swap! (when (>= (count args) 2)
                  {:kind :swap! :base (nth args 0)
                   :path-nodes [] :fn-node (nth args 1)
                   :value-node (last args)}))))))

(defn- update-fn-position?
  "True when the node at `stack`'s tip is the update fn of an update form."
  [stack]
  (let [p (parent-node stack)]
    (when-let [shape (update-shape p (pop stack))]
      (let [kids (sig-children p)
            fn-index (first (keep-indexed
                              (fn [i c] (when (identical? c (:fn-node shape)) i))
                              kids))]
        (= fn-index (index-in-parent stack))))))

;; ---------------------------------------------------------------------------
;; Target resolution
;; ---------------------------------------------------------------------------

(declare resolve-target)

(defn- let-init
  "Innermost single-assignment let/loop init node for symbol `sym`."
  [sym stack]
  (->> (ancestor-frames stack)
       reverse
       (some (fn [{:keys [node]}]
               (when (contains? '#{let let* loop if-let when-let if-some when-some}
                                (head-name node))
                 (let [bv (second (sig-children node))]
                   (when (and bv (= :vector (node/tag bv)))
                     (let [pairs (partition 2 (sig-children bv))
                           hits (filter #(= sym (token-sexpr (first %))) pairs)]
                       (when (= 1 (count hits))
                         (second (first hits)))))))))))

(defn- fn-param-source
  "When `sym` is the parameter of an enclosing (fn [sym] …) that is itself the
   update fn of an outer update form, return that outer update form + stack."
  [sym stack]
  (->> (ancestor-frames stack)
       reverse
       (some (fn [{:keys [node parent depth]}]
               (when (and parent (contains? '#{fn fn*} (head-name node)))
                 (let [pv (second (sig-children node))]
                   (when (and pv (= :vector (node/tag pv))
                              (some #(= sym (token-sexpr %)) (sig-children pv)))
                     (let [outer-stack (vec (take (dec depth) stack))]
                       (when (update-fn-position? (vec (take depth stack)))
                         {:form parent :stack outer-stack})))))))))

(defn- resolve-target
  "Resolve one collection expression to {:root string :path [sexprs]}."
  [n stack depth]
  (when (and n (< depth 8))
    (let [h (head-name n)]
      (cond
        (and h (= 'get-in h))
        (let [args (form-args n stack)
              pv (second args)]
          (when (and pv (= :vector (node/tag pv)))
            (when-let [base (resolve-target (first args) stack (inc depth))]
              (update base :path into (map safe-value (sig-children pv))))))

        (and h (= 'get h))
        (let [args (form-args n stack)]
          (when (= 2 (count args))
            (when-let [base (resolve-target (first args) stack (inc depth))]
              (update base :path conj (safe-value (second args))))))

        (and (= :token (node/tag n)) (symbol? (token-sexpr n)))
        (let [sym (token-sexpr n)]
          (if-let [init (let-init sym stack)]
            (resolve-target init stack (inc depth))
            (if-let [{:keys [form stack]} (fn-param-source sym stack)]
              (when-let [shape (update-shape form stack)]
                (when-let [base (resolve-target (:base shape) stack (inc depth))]
                  (update base :path into (map safe-value (:path-nodes shape)))))
              {:root (str sym) :path []})))

        :else nil))))

(defn- update-form-target
  [form stack]
  (when-let [shape (update-shape form stack)]
    (when-let [base (resolve-target (:base shape) stack 0)]
      (update base :path into (map safe-value (:path-nodes shape))))))

(defn- target-string
  [{:keys [root path]}]
  (str root " " (pr-str (vec path))))

;; ---------------------------------------------------------------------------
;; Sites
;; ---------------------------------------------------------------------------

(defn- fnil-write?
  [n]
  (and (= 'fnil (head-name n))
       (let [kids (sig-children n)]
         (contains? write-heads (simple-name (token-sexpr (second kids)))))))

;; @spec MCP-OP-CENSUS-001
(defn- site-kind
  [n stack doors]
  (cond
    (and (call-node? n) (contains? doors (head-name n))) :door-call
    (and (call-node? n) (contains? write-heads (head-name n))) :write-call
    (and (call-node? n) (fnil-write? n) (update-fn-position? stack)) :fnil-update-fn
    (and (= :token (node/tag n))
         (let [s (simple-name (token-sexpr n))]
           (and s (or (contains? write-heads s) (contains? doors s))))
         (update-fn-position? stack))
    (if (contains? doors (simple-name (token-sexpr n))) :door-update-fn :write-update-fn)
    :else nil))

(defn- collect-sites
  [root doors]
  (letfn [(go [n stack acc]
            (let [kind (when (seq stack) (site-kind n stack doors))
                  acc (if kind (conj acc {:node n :stack stack :kind kind}) acc)]
              (if (inner? n)
                (reduce (fn [a [i c]] (go c (conj stack {:node n :index i}) a))
                        acc
                        (map-indexed vector (sig-children n)))
                acc)))]
    (go root [] [])))

(defn- written-value-node
  [{:keys [node stack kind]}]
  (case kind
    (:fnil-update-fn :write-update-fn :door-update-fn)
    (:value-node (update-shape (parent-node stack) (pop stack)))

    (:write-call :door-call)
    (let [args (vec (rest (sig-children node)))
          h (head-name node)]
      (when (seq args)
        (if (= 'cons h) (first args) (last args))))

    nil))

(defn- target-collection
  [{:keys [node stack kind]}]
  (case kind
    (:fnil-update-fn :write-update-fn :door-update-fn)
    (update-form-target (parent-node stack) (pop stack))

    (:write-call :door-call)
    (let [args (vec (rest (sig-children node)))
          h (head-name node)
          coll (cond
                 (= 'cons h) (second args)
                 (contains? #{:door-call} kind) (when (>= (count args) 2)
                                                  (nth args (- (count args) 2)))
                 :else (first args))]
      (when coll (resolve-target coll stack 0)))

    nil))

(defn- set-target?
  [{:keys [node stack kind]}]
  (let [set-node? (fn [n] (and n (= :set (node/tag n))))]
    (case kind
      :fnil-update-fn (boolean (some set-node? (drop 2 (sig-children node))))
      :write-call (boolean (some set-node? (rest (sig-children node))))
      :write-update-fn (let [shape (update-shape (parent-node stack) (pop stack))]
                         (boolean (some set-node? (:path-nodes shape))))
      false)))

;; ---------------------------------------------------------------------------
;; Guards
;; ---------------------------------------------------------------------------

(defn- guard-frame
  "Describe the dominating guard at one ancestor frame, or nil."
  [{:keys [node child-index]}]
  (let [h (head-name node)
        kids (sig-children node)]
    (case h
      (if if-not)
      (when (and (>= (count kids) 3) (contains? #{2 3} child-index))
        {:head h :test (nth kids 1) :branch (if (= 2 child-index) :then :else)
         :node node})

      (when when-not)
      (when (and (>= (count kids) 3) (>= child-index 2))
        {:head h :test (nth kids 1) :branch :then :node node})

      (if-let when-let if-some when-some)
      (let [bv (second kids)]
        (when (and bv (= :vector (node/tag bv))
                   (>= child-index 2)
                   (= 2 (count (sig-children bv))))
          {:head h :test (second (sig-children bv))
           :branch (if (= 2 child-index) :then :else) :node node}))

      cond
      (when (and (>= child-index 2) (even? child-index))
        (let [test (nth kids (dec child-index))]
          (when-not (= :else (token-sexpr test))
            {:head h :test test :branch :then :node node})))

      nil)))

(def ^:private idiom-senses
  '{some :present not-any? :absent every? :present contains? :present
    get :present get-in :present seq :present empty? :absent})

(defn- membership-idioms
  "Every recognised membership idiom occurrence inside one test node."
  [test]
  (keep
    (fn [c]
      (when (call-node? c)
        (let [h (head-name c)
              kids (sig-children c)
              args (vec (rest kids))]
          (cond
            (contains? '#{some not-any? every?} h)
            (when (= 2 (count args))
              {:node c :sense (idiom-senses h) :coll (second args) :pred (first args)})

            (= 'contains? h)
            (when (= 2 (count args))
              {:node c :sense :present :coll (first args) :pred (second args)})

            (contains? '#{get get-in} h)
            {:node c :sense :present :coll c :pred nil}

            (contains? '#{seq empty?} h)
            (let [inner (first args)]
              (when (and inner (= 'filter (head-name inner)))
                (let [iargs (vec (rest (sig-children inner)))]
                  (when (= 2 (count iargs))
                    {:node c :sense (idiom-senses h)
                     :coll (second iargs) :pred (first iargs)}))))

            (and (inner? c) (= :set (node/tag (first kids))))
            {:node c :sense :present :coll (first kids) :pred (second kids)}

            :else nil))))
    (node-seq test)))

(defn- flip [sense] (if (= :present sense) :absent :present))

(defn- negations-above
  "Count `not`/`nil?` wrappers between `idiom` and `test`."
  [test idiom]
  (letfn [(go [n]
            (cond
              (identical? n idiom) 0
              (not (inner? n)) nil
              :else (some (fn [c]
                            (when-let [d (go c)]
                              (+ d (if (contains? '#{not nil?} (head-name n)) 1 0))))
                          (sig-children n))))]
    (or (go test) 0)))

(defn- branch-sense
  [{:keys [head branch]} sense]
  (let [after-negated-head (if (contains? '#{if-not when-not} head) (flip sense) sense)]
    (if (= :else branch) (flip after-negated-head) after-negated-head)))

(defn- suspicious-helpers
  "Unrecognised heads inside a test that carry the write's identity or target."
  [test target written-kws stack]
  (into []
        (keep
          (fn [c]
            (when (call-node? c)
              (let [h (head-name c)]
                (when (and h
                           (not (contains? recognised-test-heads h))
                           (not (contains? idiom-senses h)))
                  (let [args (rest (sig-children c))
                        kws (keyword-lookups c)]
                    (when (or (seq (set/intersection kws (or written-kws #{})))
                              (some #(= target (resolve-target % stack 0)) args))
                      (str h))))))))
        (node-seq test)))

;; ---------------------------------------------------------------------------
;; Classification
;; ---------------------------------------------------------------------------

(defn- container-violation
  "The first ancestor head between the arm root and the site that this version
   does not understand."
  [stack]
  (->> (ancestor-frames stack)
       (drop 1)
       (some (fn [{:keys [node]}]
               (when (call-node? node)
                 (let [h (head-name node)]
                   (cond
                     (nil? h) nil
                     (contains? recognised-containers h) nil
                     :else (str h))))))))

;; @spec MCP-OP-CENSUS-003
;; @spec MCP-OP-CENSUS-004
;; @spec MCP-OP-CENSUS-005
;; @spec MCP-OP-CENSUS-006
;; @spec MCP-OP-CENSUS-007
;; @spec MCP-OP-CENSUS-008
;; @spec MCP-OP-CENSUS-009
(defn- classify-site
  [{:keys [node stack kind] :as site} arm]
  (let [line (line-of node)
        base {:file nil
              :line line
              :arm (:event-type arm)
              :write (source-line node)}]
    (cond
      (contains? #{:door-call :door-update-fn} kind)
      (assoc base :class :door :door (str (or (head-name node)
                                              (simple-name (token-sexpr node)))))

      (set-target? site)
      (assoc base :class :set)

      :else
      (let [target (target-collection site)
            value-node (written-value-node site)
            value-node (or (when (and value-node
                                      (= :token (node/tag value-node))
                                      (symbol? (token-sexpr value-node)))
                             (let-init (token-sexpr value-node) stack))
                           value-node)
            written-kws (all-keywords value-node)
            base (cond-> base
                   target (assoc :target (target-string target))
                   value-node (assoc :value (source-line value-node 60)))
            container (container-violation stack)
            frames (ancestor-frames stack)
            guards (keep guard-frame frames)
            evaluated
            (for [g guards
                  idiom (membership-idioms (:test g))
                  :let [sense (nth (iterate flip (:sense idiom))
                                   (negations-above (:test g) (:node idiom)))
                        effective (branch-sense g sense)
                        gt (resolve-target (:coll idiom) stack 0)
                        id-kws (keyword-lookups (:pred idiom))
                        id-kws (if (seq id-kws) id-kws (keyword-lookups (:coll idiom)))
                        shared (set/intersection (or id-kws #{}) (or written-kws #{}))]]
              {:guard g :idiom idiom :polarity effective
               :target-match (and target gt (= target gt))
               :identity-match (boolean (seq shared))
               :identity (when (seq shared) (pr-str (first (sort shared))))})
            qualifying (first (filter #(and (:target-match %) (:identity-match %)
                                            (= :absent (:polarity %)))
                                      evaluated))
            wrong-polarity (first (filter #(and (:target-match %) (:identity-match %))
                                          evaluated))
            helpers (mapcat #(suspicious-helpers (:test %) target written-kws stack)
                            guards)]
        (cond
          qualifying
          (assoc base :class :guarded
                 :guard (source-line (:node (:guard qualifying)) 80)
                 :guard-line (line-of (:node (:idiom qualifying)))
                 :identity (:identity qualifying)
                 :polarity :absent)

          container
          (assoc base :class :unknown :reason :unsupported-container
                 :detail container)

          wrong-polarity
          (assoc base :class :unknown :reason :polarity
                 :guard (source-line (:node (:guard wrong-polarity)) 80)
                 :guard-line (line-of (:node (:idiom wrong-polarity)))
                 :identity (:identity wrong-polarity)
                 :polarity (:polarity wrong-polarity))

          (seq helpers)
          (assoc base :class :unknown :reason :helper-mediated-guard
                 :detail (first helpers)
                 :guard-line (line-of (:node (first guards))))

          (nil? target)
          (assoc base :class :unknown :reason :unresolved-target)

          :else
          (assoc base :class :raw))))))

;; ---------------------------------------------------------------------------
;; Calls this version does not model
;; ---------------------------------------------------------------------------

(def ^:private analyzed-heads
  "Call heads the census reasons about. Everything else inside an arm is a call
   whose effect on the state this version cannot see."
  (set/union recognised-containers
             recognised-test-heads
             write-heads
             '#{defmethod fnil}))

;; @spec MCP-OP-CENSUS-025
(defn- unrecognised-calls
  "Calls inside one arm whose head this census version does not model.

   The census reports `raw 0` when it finds no site at all, and a write hidden
   behind an ordinary helper produces exactly that: no site, no raw, a clean
   next_action. These are the calls that could be hiding one."
  [arm-node event-type doors]
  (into []
        (keep (fn [n]
                (when-let [h (head-name n)]
                  (when-not (or (contains? analyzed-heads h)
                                (contains? doors h))
                    {:call (str h) :line (line-of n) :arm event-type}))))
        (node-seq arm-node)))

;; ---------------------------------------------------------------------------
;; File census
;; ---------------------------------------------------------------------------

(defn- arm?
  [n multi]
  (and (call-node? n)
       (= 'defmethod (head-name n))
       (= multi (simple-name (token-sexpr (second (sig-children n)))))))

(defn- arm-of
  [n]
  {:event-type (let [v (safe-value (nth (sig-children n) 2 nil))]
                 (if (= ::unreadable v) "?" (str v)))
   :line (line-of n)
   :node n})

(defn- declared-names
  [forms]
  (into #{}
        (keep (fn [n]
                (when (and (call-node? n)
                           (contains? '#{def defn defn- defmacro} (head-name n)))
                  (simple-name (token-sexpr (second (sig-children n)))))))
        forms))

(def empty-counts {:door 0 :set 0 :guarded 0 :raw 0 :unknown 0})

;; @spec MCP-OP-CENSUS-002
(defn census-file
  "Census one source string. Pure: text in, data out."
  [{:keys [file source doors multi]
    :or {doors default-doors multi 'fold-event}}]
  (let [parsed (try {:ok true :node (parser/parse-string-all source)}
                    (catch Throwable e {:ok false :message (.getMessage e)}))]
    (if-not (:ok parsed)
      {:ok false :error-type :unparseable-file :file file
       :error (str "Could not parse " file ": " (:message parsed))}
      (let [forms (sig-children (:node parsed))
            arm-nodes (filterv #(arm? % multi) forms)
            other-nodes (filterv #(not (arm? % multi)) forms)
            sites (into []
                        (mapcat
                          (fn [an]
                            (let [arm (arm-of an)
                                  body (drop 4 (sig-children an))]
                              (->> body
                                   (mapcat #(collect-sites % doors))
                                   (map #(assoc (classify-site % arm) :file file))))))
                        arm-nodes)
            outside (reduce + 0 (map #(count (collect-sites % doors)) other-nodes))
            unmodelled (into []
                             (mapcat
                               (fn [an]
                                 (map #(assoc % :file file)
                                      (unrecognised-calls
                                        an (:event-type (arm-of an)) doors))))
                             arm-nodes)]
        {:ok true
         :file file
         :arms (count arm-nodes)
         :arm-types (mapv #(:event-type (arm-of %)) arm-nodes)
         :declared (declared-names forms)
         :sites sites
         :unrecognised unmodelled
         :outside-arms outside
         :counts (merge empty-counts (frequencies (map :class sites)))}))))

(defn source-declared-names
  "Top-level `def`/`defn`/`defn-`/`defmacro` names in one source.

   A door may be defined in a helper namespace that defines no arms, so door
   validation needs the names of every scanned file, not only the censused
   ones. Returns the empty set for a source that cannot be parsed: an
   unparseable file is the census's refusal to report, not this predicate's."
  [source]
  (try
    (declared-names (sig-children (parser/parse-string-all source)))
    (catch Throwable _ #{})))

(defn defines-arms?
  "Cheap discovery predicate: does this source define arms of `multi`?"
  ([source] (defines-arms? source 'fold-event))
  ([source multi]
   (boolean (re-find (re-pattern (str "\\(defmethod\\s+(?:[\\w.$-]+/)?"
                                      (java.util.regex.Pattern/quote (str multi))
                                      "\\s"))
                     source))))

;; @spec MCP-OP-CENSUS-019
(defn parse-doors
  "Validate a caller's identity doors against the census vocabulary.

   One kernel for both entrances: the MCP tool and the CLI op. Returns the door
   set, or a map naming the offending value and why it was refused. `declared`
   is the set of names defined in the scanned files, or nil to skip that check."
  [doors declared]
  (reduce
    (fn [acc value]
      (let [sym (try (symbol (str/trim (str value))) (catch Throwable _ nil))]
        (cond
          (or (nil? sym) (str/blank? (str value)) (str/includes? (str value) " "))
          (reduced {:invalid (str value) :why "not a symbol"})

          (contains? '#{conj cons into concat} (symbol (name sym)))
          (reduced {:invalid (str value) :why "shadows a collection write head"})

          (and (some? declared)
               (not (contains? default-doors (symbol (name sym))))
               (not (contains? declared (symbol (name sym)))))
          (reduced {:invalid (str value)
                    :why "not defined in any scanned file"})

          :else (conj acc (symbol (name sym))))))
    #{}
    doors))

;; @spec MCP-OP-CENSUS-025
(defn unrecognised-summary
  "The count of unmodelled calls inside arms, with up to `limit` named examples.

   Shared by both entrances so the tool and the CLI report the same thing."
  [unrecognised limit]
  (when (seq unrecognised)
    {:count (count unrecognised)
     :examples (->> (sort-by (juxt :file :line) unrecognised)
                    (reduce (fn [{:keys [seen out] :as acc} call]
                              (cond
                                (>= (count out) limit)
                                (reduced acc)

                                (contains? seen (:call call))
                                acc

                                :else {:seen (conj seen (:call call))
                                       :out (conj out call)}))
                            {:seen #{} :out []})
                    :out
                    (mapv #(select-keys % [:call :file :line :arm])))}))

(defn merge-results
  "Merge per-file results, re-keyed by path. Order is by path, always."
  [results]
  (let [ordered (vec (sort-by :file results))]
    {:by-file (into (sorted-map)
                    (map (fn [r] [(:file r) (-> (select-keys r [:arms :outside-arms :counts])
                                                (assoc :sites (count (:sites r))))]))
                    ordered)
     :files (count ordered)
     :arms (reduce + 0 (map :arms ordered))
     :sites (reduce + 0 (map #(count (:sites %)) ordered))
     :outside-arms (reduce + 0 (map :outside-arms ordered))
     :counts (apply merge-with + empty-counts (map :counts ordered))
     :all-sites (vec (mapcat :sites ordered))
     :unrecognised (vec (mapcat :unrecognised ordered))
     :declared (reduce into #{} (map :declared ordered))}))

;; @spec MCP-OP-CENSUS-012
(defn census-input
  "One worker unit. Any throw becomes a typed per-file refusal naming the file,
   so a pool worker can never publish a partial census."
  [{:keys [doors multi]} input]
  (try
    (census-file (assoc input :doors doors :multi multi))
    (catch Throwable e
      {:ok false
       :error-type :census-worker-failure
       :file (:file input)
       :error (str "Census worker failed on " (:file input) ": " (ex-message e))})))

;; @spec MCP-OP-CENSUS-011
(defn plan
  "Census many files. `map-fn` performs the parse+classify phase; it changes
   elapsed time and never the answer."
  [{:keys [inputs doors multi map-fn]
    :or {doors default-doors multi 'fold-event map-fn map}}]
  (let [t0 (System/nanoTime)
        results (vec (map-fn #(census-input {:doors doors :multi multi} %) inputs))
        t1 (System/nanoTime)
        failed (first (remove :ok results))]
    (if failed
      (assoc failed :phases {:classify (/ (- t1 t0) 1e6)})
      (let [merged (merge-results results)
            t2 (System/nanoTime)]
        (assoc merged
               :ok true
               :census-version census-version
               :phases {:classify (/ (- t1 t0) 1e6)
                        :merge (/ (- t2 t1) 1e6)})))))
