(ns clj-surgeon.read-hook
  "Pure decisions for the read-side routing hook (`bin/rg-clj`).

  The hook is installed as `rg`, first on an agent's PATH, so its argument
  vector is ripgrep's argument vector. Everything decidable about an
  invocation is decided here, as data in and data out: argument parsing,
  whether the invocation can be served exactly, how the read path's file set
  is compared against ripgrep's own, how the substituted argument vector is
  built, and what one route record says. The shim does environment, HTTP,
  subprocess and logging, and nothing else.

  Specifications: docs/intent/read-hook/read-hook-specs.md."
  (:require
   [clojure.string :as str]))

;; ---------------------------------------------------------------------------
;; the flag vocabulary
;;
;; This is an ALLOWLIST, and that is the whole safety argument. A flag the hook
;; does not know might consume the next argument, in which case a path is not a
;; path; it might restrict the directory walk, in which case an explicit file
;; list is not the same search; or it might reorder output, in which case the
;; substitution is detectable. So an unknown flag ends the analysis rather than
;; being guessed at, and the invocation goes to the real ripgrep untouched.

(def long-boolean-flags
  "Long flags that take no value and survive the substitution unchanged."
  #{"--line-number" "--no-line-number" "--ignore-case" "--case-sensitive"
    "--smart-case" "--word-regexp" "--fixed-strings" "--files-with-matches"
    "--files-without-match" "--count" "--count-matches" "--invert-match"
    "--only-matching" "--with-filename" "--no-filename" "--no-heading"
    "--heading" "--hidden" "--no-ignore" "--no-ignore-vcs" "--multiline"
    "--multiline-dotall" "--text" "--vimgrep" "--column" "--null"
    "--line-buffered" "--no-messages" "--trim" "--crlf" "--stop-on-nonmatch"})

(def long-valued-flags
  "Long flags that take one value and survive the substitution unchanged."
  #{"--after-context" "--before-context" "--context" "--regexp" "--glob"
    "--iglob" "--type" "--type-not" "--max-count" "--max-columns" "--replace"
    "--color" "--colors" "--threads" "--context-separator"
    "--field-context-separator" "--field-match-separator"})

(def short-boolean-flags
  "Short flags that take no value."
  (set "nNisSwFlcvoHIaUpP"))

(def short-valued-flags
  "Short flags that take one value, attached or as the next argument."
  (set "ABCegtTmMrj"))

(def walk-filter-long-flags
  "Long flags that change WHICH FILES ripgrep walks. They must be replayed on
  the `--files` probe that produces ripgrep's own candidate set, or the two
  sets are not answers to the same question."
  #{"--glob" "--iglob" "--type" "--type-not" "--hidden" "--no-ignore"
    "--no-ignore-vcs"})

(def walk-filter-short-flags
  "Short flags that change which files ripgrep walks."
  (set "gtT"))

(def pattern-bearing-flags
  "Flags that supply the pattern, so that every positional is a path."
  #{"--regexp" "-e"})

(def clojure-extensions
  #{".clj" ".cljc" ".cljs"})

(defn clojure-source?
  [path]
  (boolean (some #(str/ends-with? path %) clojure-extensions)))

;; ---------------------------------------------------------------------------
;; argument parsing

(defn- long-flag-name
  [token]
  (let [i (str/index-of token "=")]
    (if i [(subs token 0 i) (subs token (inc i))] [token nil])))

(defn- walk-filter?
  "Does this short-flag cluster contain a flag that changes the walk?"
  [token]
  (boolean (some walk-filter-short-flags (subs token 1))))

(defn- short-cluster
  "Analyse one short-flag cluster (`-n`, `-niC`, `-C5`).

  Returns `{:ok? true :takes-next? bool :pattern-supplied? bool}`, or
  `{:ok? false}` for any character the allowlist does not name."
  [token]
  (loop [i 1]
    (if (>= i (count token))
      {:ok? true :takes-next? false :pattern-supplied? false
       :with-filename? (str/includes? (subs token 1) "H")
       :no-filename? (str/includes? (subs token 1) "I")}
      (let [c (.charAt ^String token i)]
        (cond
          (contains? short-boolean-flags c) (recur (inc i))

          (contains? short-valued-flags c)
          {:ok? true
           :takes-next? (= (inc i) (count token))
           :pattern-supplied? (= c \e)
           :with-filename? (str/includes? (subs token 1 i) "H")
           :no-filename? (str/includes? (subs token 1 i) "I")}

          :else {:ok? false})))))

;; @spec MCP-OP-READ-HOOK-002
(defn parse-argv
  "Split ripgrep's argument vector into flags and path arguments.

  Returns `{:ok? true :flags [...] :paths [...] :pattern ...}` or
  `{:ok? false :reason <keyword> :flags [...] :paths [...]}`. The `:flags`
  vector holds every token that is not a path argument, in the order it was
  given, including the pattern, so a fallback can be reconstructed by
  concatenation and a route record can report what was asked."
  [argv]
  (let [argv (vec argv)
        n (count argv)]
    (loop [i 0
           flags []
           positionals []
           pattern-supplied? false
           end-of-flags? false
           with-filename? false
           no-filename? false
           walk-flags []]
      (if (>= i n)
        (let [[pattern paths] (if (or pattern-supplied? (empty? positionals))
                                [nil (vec positionals)]
                                [(first positionals) (vec (rest positionals))])]
          {:ok? true
           :reason :servable
           :flags (if pattern (conj flags pattern) flags)
           :pattern pattern
           :paths paths
           :with-filename? with-filename?
           :no-filename? no-filename?
           :walk-flags walk-flags})
        (let [token (nth argv i)
              refuse {:ok? false :reason :unsupported-flag
                      :flags flags :paths (vec positionals)}]
          (cond
            end-of-flags?
            (recur (inc i) flags (conj positionals token) pattern-supplied? true
                   with-filename? no-filename? walk-flags)

            (= "--" token)
            (recur (inc i) (conj flags token) positionals pattern-supplied? true
                   with-filename? no-filename? walk-flags)

            (str/starts-with? token "--")
            (let [[name inline] (long-flag-name token)]
              (cond
                (contains? long-boolean-flags name)
                (if inline
                  refuse
                  (recur (inc i) (conj flags token) positionals
                         pattern-supplied? false
                         (or with-filename? (= name "--with-filename"))
                         (or no-filename? (= name "--no-filename"))
                         (cond-> walk-flags
                           (contains? walk-filter-long-flags name)
                           (conj token))))

                (contains? long-valued-flags name)
                (let [supplied? (or pattern-supplied?
                                    (contains? pattern-bearing-flags name))]
                  (cond
                    inline (recur (inc i) (conj flags token) positionals
                                  supplied? false with-filename? no-filename?
                                  (cond-> walk-flags
                                    (contains? walk-filter-long-flags name)
                                    (conj token)))
                    (>= (inc i) n) refuse
                    :else (recur (+ i 2)
                                 (conj flags token (nth argv (inc i)))
                                 positionals supplied? false
                                 with-filename? no-filename?
                                 (cond-> walk-flags
                                   (contains? walk-filter-long-flags name)
                                   (conj token (nth argv (inc i)))))))

                :else refuse))

            (and (str/starts-with? token "-") (> (count token) 1))
            (let [{:keys [ok? takes-next?] :as cluster} (short-cluster token)
                  supplied? (or pattern-supplied? (:pattern-supplied? cluster))]
              (cond
                (not ok?) refuse
                (not takes-next?) (recur (inc i) (conj flags token) positionals
                                         supplied? false
                                         (or with-filename? (:with-filename? cluster))
                                         (or no-filename? (:no-filename? cluster))
                                         (cond-> walk-flags
                                           (walk-filter? token) (conj token)))
                (>= (inc i) n) refuse
                :else (recur (+ i 2) (conj flags token (nth argv (inc i)))
                             positionals supplied? false
                             (or with-filename? (:with-filename? cluster))
                             (or no-filename? (:no-filename? cluster))
                             (cond-> walk-flags
                               (walk-filter? token)
                               (conj token (nth argv (inc i)))))))

            :else
            (recur (inc i) flags (conj positionals token)
                   pattern-supplied? false with-filename? no-filename?
                   walk-flags)))))))

;; ---------------------------------------------------------------------------
;; servability

(defn suppress-filename?
  "Did the caller ask ripgrep NOT to print a filename prefix? Read from the
  parse rather than by scanning tokens: a pattern is a token too, and a pattern
  of `-I` is a legal pattern."
  [parse]
  (true? (:no-filename? parse)))

(defn force-filename?
  [parse]
  (true? (:with-filename? parse)))

;; @spec MCP-OP-READ-HOOK-001
;; @spec MCP-OP-READ-HOOK-002
(defn servable
  "Decide whether one invocation can be served exactly.

  `path-kinds` maps each path argument to `:dir`, `:file` or `:missing`.
  `candidates` is ripgrep's own candidate set for this invocation — every file
  it would open — as ripgrep prints them.

  Returns `{:servable? true}` or `{:servable? false :reason <keyword>}`. The
  reason is written verbatim into the route record, so it is the cohort's
  account of why a call was not routed."
  [{:keys [parse path-kinds candidates]}]
  (let [{:keys [ok? reason paths]} parse]
    (cond
      (not ok?) {:servable? false :reason reason}
      (empty? paths) {:servable? false :reason :no-path-argument}
      (not (every? #(= :dir (get path-kinds %)) paths))
      {:servable? false :reason :path-argument-not-a-directory}
      (empty? candidates) {:servable? false :reason :no-candidate-files}
      (not (every? clojure-source? candidates))
      {:servable? false :reason :non-clojure-candidate}
      :else {:servable? true :reason :servable})))

;; ---------------------------------------------------------------------------
;; the read path's answer

(defn- join-path
  [prefix relative]
  (cond
    (str/blank? prefix) relative
    (str/ends-with? prefix "/") (str prefix relative)
    :else (str prefix "/" relative)))

;; @spec MCP-OP-READ-HOOK-007
(defn receipt-files
  "The files one `ls-tree` receipt names, expressed the way ripgrep prints
  them under `path-argument`, or a refusal.

  A receipt that is not `ok`, that did not read the whole tree, or that was
  truncated names FEWER files than the tree holds. Serving from it would drop
  matches silently, which is the one failure this hook must never produce."
  [receipt path-argument]
  (cond
    (nil? receipt) {:ok? false :reason :read-path-unreachable}
    (not (true? (:ok receipt))) {:ok? false :reason :read-path-refused}
    (true? (:truncated receipt)) {:ok? false :reason :read-path-truncated}
    (false? (:read_complete receipt)) {:ok? false :reason :read-path-truncated}
    :else
    (let [rows (:files receipt)]
      (if (empty? rows)
        {:ok? false :reason :read-path-empty}
        {:ok? true
         :files (mapv #(join-path path-argument (:file %)) rows)}))))

;; @spec MCP-OP-READ-HOOK-007
(defn reconcile
  "Compare the read path's file set with ripgrep's own candidate set.

  Equality is the whole contract. A read path that omits a file would drop
  matches; a read path that names a file ripgrep would not open would invent
  them. Either way the hook refuses to serve and the disagreement is recorded,
  because a hook that silently prefers one of the two answers is a hook whose
  correctness nobody can audit."
  [read-path-files candidates]
  (if (= (set read-path-files) (set candidates))
    {:ok? true
     :files (let [order (into {} (map-indexed (fn [i p] [p i]) candidates))]
              (vec (sort-by order read-path-files)))}
    {:ok? false :reason :discovery-mismatch}))

;; ---------------------------------------------------------------------------
;; the substituted argument vector

;; @spec MCP-OP-READ-HOOK-001
;; @spec MCP-OP-READ-HOOK-006
(defn substitute-argv
  "Ripgrep's argument vector with the path arguments replaced by an explicit,
  canonically ordered file list.

  `--sort path` is added because ripgrep's directory walk is not deterministic
  between runs; without it there is no single byte string to be identical to.
  `--with-filename` is added unless the caller suppressed it, because ripgrep
  omits the filename prefix when it is given exactly one file and the caller
  gave it a directory."
  [{:keys [parse files]}]
  (let [{:keys [flags]} parse]
    (vec (concat ["--sort" "path"]
                 (when-not (or (suppress-filename? parse)
                               (force-filename? parse))
                   ["--with-filename"])
                 flags
                 ;; A second `--` would be a positional named "--", i.e. a file
                 ;; ripgrep would try to open. If the caller already ended
                 ;; option parsing, the pattern is already past it and nothing
                 ;; more is needed.
                 (when-not (some #{"--"} flags) ["--"])
                 files))))

;; ---------------------------------------------------------------------------
;; the route record

;; @spec MCP-OP-READ-HOOK-003
(defn route-record
  "One route record. This log is the cohort's routed-percentage meter, so the
  fields are the ones a meter needs and no others: what was asked, whether it
  was routed, why not when it was not, and what it cost."
  [{:keys [parse served? reason files ms bytes exit cwd pid]}]
  {:ts (str (java.time.Instant/now))
   :pid pid
   :cwd cwd
   :paths (vec (:paths parse))
   :flags (vec (:flags parse))
   :served_by (if served? "surgeon" "fallback")
   :reason (name (or reason :servable))
   :files (int (or files 0))
   :ms (int (or ms 0))
   :bytes (int (or bytes 0))
   :exit (int (or exit 0))})

;; ---------------------------------------------------------------------------
;; resolving the real ripgrep

;; @spec MCP-OP-READ-HOOK-005
(defn real-ripgrep
  "The first `rg` on `path-entries` that is not this hook.

  The hook IS `rg` on that PATH, so the obvious implementation is an infinite
  exec loop. `self-paths` is the set of canonical paths that are this hook —
  the script, and every symlink the caller may have reached it through — and
  `canonical` resolves one candidate to its canonical path."
  [{:keys [path-entries self-paths canonical exists?]}]
  (some (fn [entry]
          (let [candidate (str entry "/rg")]
            (when (and (exists? candidate)
                       (not (contains? self-paths (canonical candidate))))
              candidate)))
        path-entries))
