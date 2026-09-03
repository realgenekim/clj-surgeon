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
   a string). Returns {:ok true :size n} or a typed reason."
  [value]
  (let [text (str/trim (str value))
        parsed (cond
                 (integer? value) (long value)
                 (re-matches #"\d{1,9}" text) (parse-long text)
                 :else nil)]
    (cond
      (nil? parsed) {:ok false :reason :not-an-integer :value text}
      (or (< parsed 1) (> parsed max-pool-size))
      {:ok false :reason :out-of-range :value parsed :maximum max-pool-size}
      :else {:ok true :size parsed})))

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
              :doors {:present? (some? doors)
                      :value doors
                      :entries (when (string? doors) (cli-door-names doors))}
              :files {:present? (some? file)
                      :value file
                      :entries (when (some? file) [file])}
              :pool-size {:present? (some? (:threads params))
                          :value (:threads params)}}))))

(def ^:private known-door-list
  (delay (str/join "," (sort (map str default-doors)))))

(defn- doors-ok?
  "True when `pred` holds for the normalised doors, or there are none to check."
  [req pred]
  (let [{:keys [present? entries]} (:doors req)]
    (or (not present?) (not (sequential? entries)) (pred entries))))

(def request-shape-rules
  "The ONE ordered refusal table both census entrances validate against.

   Order is the order MCP-OP-CENSUS-016 states — unknown fields, then
   `doors`, then `files`, then `pool_size` — and both entrances refuse on
   the FIRST row that fails.

   Per row: `:predicate` is true when the request is ACCEPTABLE for that row.
   `:mcp` and `:cli` are the names the two entrances publish; a map instead
   of a keyword means that entrance cannot express the violation and says
   why. `:cli-message`, `:cli-data` and `:cli-fix` are the CLI's refusal
   payload — the CLI validator below is driven entirely from this table. The
   tool's validator keeps its own `cond` (its per-branch payloads differ)
   but takes its predicates and its published names from these same rows, so
   a rename or a loosened bound reaches both entrances at once."
  [{:field :unknown-fields
    :violation :present
    :predicate (fn [req] (empty? (:unknown req)))
    :mcp :unknown-fields
    :cli :unknown-arguments
    :cli-message (fn [req]
                   (str ":op :relation-census does not accept "
                        (str/join ", " (map #(str ":" %) (:unknown req)))))
    :cli-data (fn [_req]
                {:accepted (vec (sort (map #(str ":" (name %))
                                           cli-census-fields)))})
    :cli-fix :none}

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
    :cli {:inexpressible
          (str "the CLI's :doors is one string split on commas, so every "
               "entry it yields is a string by construction; a CLI :doors "
               "value that is not a string is refused one row earlier, as "
               ":doors-not-a-string")}}

   {:field :doors
    :violation :vocabulary
    ;; The syntactic half of the door check: is this name a symbol at all,
    ;; and does it shadow a collection write head? Whether a door is DEFINED
    ;; can only be answered after a scan, so that half is not a shape rule
    ;; and is not here. The tool applies THIS predicate after discovery, so
    ;; its refusal can carry the discovery facts; the CLI applies it in the
    ;; shape pass, because the CLI entrance's next filesystem act is a config
    ;; read it has not yet earned. Same predicate, same published name, same
    ;; answer — different moment, for a stated reason.
    :predicate (fn [req]
                 (doors-ok? req #(or (not (every? string? %))
                                     (not (map? (parse-doors % nil))))))
    :mcp :unknown-door-symbol
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
    :cli {:inexpressible
          (str "the CLI names at most one source, with :file; there is no "
               "list whose container could have the wrong type")}}

   {:field :files
    :violation :empty
    :predicate (fn [req]
                 (let [{:keys [present? value]} (:files req)]
                   (or (not present?) (not (sequential? value)) (seq value))))
    :mcp :empty-file-list
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

(defn shape-rule
  "One row of the shared refusal table, by field and violation."
  [field violation]
  (first (filter #(and (= field (:field %)) (= violation (:violation %)))
                 request-shape-rules)))

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
   leaving the caller to guess which cwd a bare path meant."
  [{:keys [dir file]}]
  (let [cwd (System/getProperty "user.dir")
        named-file? (and (string? file) (not (str/blank? file)))
        given (str (if named-file? file (if (some? dir) dir ".")))
        trimmed (str/trim given)
        absolute (cond
                   (str/starts-with? trimmed "/") trimmed
                   (or (= "." trimmed) (str/blank? trimmed)) cwd
                   (str/starts-with? trimmed "./") (str cwd "/" (subs trimmed 2))
                   :else (str cwd "/" trimmed))]
    (cond-> {:kind (if named-file? :file :dir)
             :given given
             :absolute absolute}
      (not= given absolute) (assoc :resolved-against cwd))))

(defn- cli-next-command
  "The continuation one CLI shape refusal hands back, or nil when it does not
   fit the shared continuation bound."
  [anchor fix]
  (let [command (str "clj-surgeon :op :relation-census "
                     (if (= :file (:kind anchor)) ":file " ":dir ")
                     (:absolute anchor)
                     (case fix
                       :doors (str " :doors " @known-door-list)
                       :threads " :threads 8"
                       ""))]
    (when (<= (count command) max-next-call-bytes) command)))

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
      (fn [_ {:keys [predicate cli cli-message cli-data cli-fix] :as _rule}]
        (if (or (not (keyword? cli)) (predicate req))
          nil
          (reduced
            (merge
              {:ok false
               :error-type cli
               :error (cli-message req)
               :anchor anchor}
              (cli-data req)
              (if-let [command (cli-next-command anchor cli-fix)]
                {:next-command command}
                {:remedy
                 (str "The workspace this request names is too long to carry "
                      "into a continuation under " max-next-call-bytes
                      " bytes, so no narrower command can be computed: run "
                      "the census from a shorter path.")})))))
      nil
      request-shape-rules)))

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
