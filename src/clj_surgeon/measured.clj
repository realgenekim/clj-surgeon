(ns clj-surgeon.measured
  "The MEASURED channel: where a reading a clock produced is allowed to live.

  No heavy requires — this namespace loads under babashka so the encoders and
  the battery can share ONE definition of the partition. The one piece of I/O it
  performs, `file-modified-ms`, is there for the same reason every other raw
  clock read is: a file's modification stamp is a number a clock produced, and
  this is the one file allowed to read one raw.

  The rule, in one sentence: **a measured field may never live inside a value
  another requirement hashes.**

  Three ratified rows collided on one field and no arrangement satisfied all
  three as written:

  - `MCP-OP-MEM-005` requires the scan's own cost, `scan_ms`, in the ls-tree
    receipt UNCONDITIONALLY — a meter wired to the rare refusal branch is one
    nobody ever sees move.
  - `MCP-OP-MEM-011` hashes an operation's whole result and compares it against
    an unbounded reference (the battery's `reference-mismatch` line).
  - `MCP-OP-MEM-003` requires two scans of an unchanged tree to be
    byte-identical, and a complete result to carry no ceiling receipt.

  Measured, two back-to-back EDN scans of one unchanged tree, before this
  namespace existed:

      A receipt {:resources {:scan_ms 44.081, :bytes_scanned 111183}}
      B receipt {:resources {:scan_ms 23.054, :bytes_scanned 111183}}
      records equal (receipt dropped)? true

  The ruling: keep the meter, and partition the result instead. A result has
  two channels.

  - The HASHED channel carries the deterministic result and the deterministic
    resource facts. `bytes_scanned` is a count of bytes and stays IN it: a
    denominator that moved would be a real regression and must be caught.
  - The MEASURED channel carries every field a clock produced. It is published
    — that is the whole point of MEM-005 — but it is published BESIDE the
    hashed channel, under the well-known key `:measured`, and every
    determinism, parity, and byte-identity row takes the hashed channel as its
    subject.

  One key, one rule, applied anywhere in a result: whatever is under
  `:measured` is measured. A new timing field is added there and no
  determinism row has to be renegotiated again.

  @spec MCP-OP-MEM-003
  @spec MCP-OP-MEM-005
  @spec MCP-OP-MEM-011
  @spec MCP-OP-TIME-006
  @spec MCP-OP-TIME-007"
  (:require
   [clojure.string :as str]))

(def measured-key
  "The one key that marks a MEASURED block, anywhere in any result."
  :measured)

(def text-measured-prefix
  "The line prefix a TEXT encoding uses for its measured fields.

  Text has no keys, so the partition has to be visible in the bytes. A reader
  sees what it costs; a byte-identity witness drops these lines by prefix."
  "── measured (not hashed): ")

;; ============================================================
;; PROVENANCE: the tag rides the VALUE, not the name
;; ============================================================
;;
;; The first repair of this invariant bound it to a NAME VOCABULARY — a set of
;; field names a projector relocated. Sol's round-two review broke it in one
;; move (2026-09-04 §1): bind an existing clock read once, publish the same
;; number under the declared `:elapsed_ms` AND an undeclared
;; `:verification_wall_ms`, and the second name sails into the parity hash
;; while every witness stays green, because no clock-read COUNT changed and the
;; new name was in nobody's vocabulary.
;;
;; A vocabulary can only ever describe the names somebody already thought of.
;; So the tag is attached where the CLOCK IS READ: `elapsed-ms` and
;; `elapsed-nanos` return a READING — the number wrapped in a one-key map that
;; says a clock produced it — and the publication boundary relocates every
;; reading it finds, whatever key it is sitting under. An undeclared measured
;; field cannot be CONSTRUCTED from a sanctioned clock read, and the source
;; scan in `clj-surgeon.measured-invariant-test` makes the unsanctioned reads
;; (raw `System/nanoTime`, `System/currentTimeMillis`, `Instant/now`,
;; `.getTime`) unavailable outside this namespace except at named, reasoned
;; allow-list entries that are all on the `:control` channel.
;;
;; `value` is the one verb that strips a tag, and its call sites in `src/` are
;; allow-listed by the same witness: laundering a reading back into a bare
;; number is legitimate (a sum, a comparison, a telemetry row) and must be a
;; deliberate, greppable act rather than a side effect.
;;
;; And "one verb" is now a fact about the NAMESPACE's public surface as well as
;; about the type. The round-four review closed the type and walked through the
;; namespace (2026-09-04 §1): `unwrap-readings` was public, stripped tags at any
;; depth, and called `value` internally so no caller of it ever matched the
;; pattern; `field` handed a number back out of a published block. Both are
;; private now, and `clj-surgeon.measured-invariant-test` PROBES this namespace
;; by reflection — every callable public var, at every declared arity, called
;; with a tagged reading — so a new public verb that yields a reading's number
;; fails a witness that never had to know its name. That is the shape the
;; round-three review asked for: a name the scanner does not know is a hole in
;; every scanner at once, so the scanner stops depending on names.

(defprotocol Launderable
  "The ONE door out of an opaque measured value.

  Implemented by `Reading` and `Tick` and called by exactly one verb, `value`.
  It is a protocol rather than a public field because a `deftype` field that is
  not `^:unsynchronized-mutable` is a public final Java field, and `(.-n r)`
  would be a second door nobody named."
  (-launder [x] "The bare number this opaque value carries."))

;; A `deftype`, not a map, and the reason is a reproduced defect.
;;
;; Round three tagged a reading as a one-key map, `{::reading 12.5}`, and
;; documented `value` as "THE ONE LAUNDERING VERB". The round-three review
;; opened it in three tokens (2026-09-04 §1a):
;;
;;     :elapsed_ms (:clj-surgeon.measured/reading (measured/elapsed-ms started))
;;     :verification_wall_ms (:clj-surgeon.measured/reading
;;                             (measured/elapsed-ms started))
;;
;;     {:undeclared-field 1.788624, :hashed-field 1.788624, :unpartitioned []}
;;     Ran 9 tests containing 21 assertions. 0 failures, 0 errors.
;;
;; No raw clock read, no `value` call, no allow-list entry, no changed count —
;; and an undeclared clock-derived field in the parity hash with every witness
;; green. A keyword lookup is not a laundering verb anybody has to name, so
;; while the reading was a map the "one door" claim was prose.
;;
;; The repair is a rung-5 one: make the bad state UNREPRESENTABLE. The reading
;; is an opaque type implementing no map interface, no `ILookup`, no `IDeref`,
;; no `seq`, and holding its number in a PRIVATE mutable field. `(:foo r)` and
;; `(get r k)` return nil, `(count r)` throws, and the only expression that
;; yields the number is `measured/value` — whose call sites in `src/` are named
;; one by one in the invariant witness's allow-list.
;;
;; ACCEPTED RESIDUAL, declared rather than chased (round-four review §3):
;; `setAccessible` reflection with a COMPUTED field name —
;; `(.getDeclaredField (class r) (str "launder" "able"))` — reads the number,
;; and no textual scan can see it. The literal spelling IS in the escape-hatch
;; pattern, so a plain plant is caught; the computed one is not, and a JVM
;; without a security manager cannot prevent reflection at all. The reviewer's
;; ruling, which this comment exists to record: "Textual scanning cannot close
;; that, and a deliberate attacker is not the threat model here — record it, do
;; not chase it."

;; SECOND ACCEPTED RESIDUAL, declared rather than chased (round-five review
;; §4): VALUE EQUALITY is an oracle, and now that `hashCode` is a constant it
;; is a total one. A reading is usable as a map key and in a set, so both `=`
;; and set membership answer the question "is the hidden number exactly x?",
;; and ordinary code recovers the number by bisection using no measured verb at
;; all -- only the public `measured/reading` constructor. The reviewer measured
;; it:
;;
;;     --- binary-search laundering via = (ordinary code) ---
;;     recovered by bisection: 12345.678 in 67 steps
;;
;; It is declared at the same tier as the `setAccessible` residual above, in
;; the reviewer's own reasoning about that one: "nobody writes that by
;; accident, which is exactly the round-four reasoning about `setAccessible`,
;; and it is why I do not block on it." The alternative -- identity-only
;; equality -- would break `(= (reading 1.5) (reading 1.5))`, which the type's
;; own witnesses depend on and which is what makes a reading comparable in a
;; test at all. So: the file no longer claims `value`/`-launder` are the whole
;; surface. `=` consults the number too, deliberately, and this paragraph is
;; the record of that decision.

(deftype Reading [^:unsynchronized-mutable launderable]
  Launderable
  (-launder [_] launderable)
  Object
  (toString [_] "#clj-surgeon.measured/reading")
  (equals [_ other]
    (and (instance? Reading other) (= launderable (-launder other))))
  ;; A CONSTANT, not `(hash launderable)`. The round-four review's §3: every
  ;; other accessor withholds the number, but a hash derived from it is a
  ;; clock-varying integer a caller can publish — `:some_field (hash r)` —
  ;; through no verb any scan matches. The hashCode/equals contract only
  ;; requires equal objects to hash equally, and these types are compared,
  ;; never bucketed, so a constant satisfies it and carries no clock bits. The
  ;; keyword's hash is deterministic across runs; an identity hash would not be.
  (hashCode [_] (hash ::reading)))

(deftype Tick [^:unsynchronized-mutable launderable]
  Launderable
  (-launder [_] launderable)
  Object
  (toString [_] "#clj-surgeon.measured/tick")
  (equals [_ other]
    (and (instance? Tick other) (= launderable (-launder other))))
  ;; Constant, for the reason `Reading`'s hashCode states.
  (hashCode [_] (hash ::tick)))

;; DETERMINISTIC printing, deliberately without the number. The default
;; `print-method` for a `deftype` writes `#object[... 0x1a2b3c ...]` — an
;; IDENTITY HASH, which would make any byte-identity or parity subject that
;; ever saw a reading non-reproducible for a reason having nothing to do with
;; the clock. And the number is withheld because a printed reading is a leak
;; being caught, not a value being published.
(defmethod print-method Reading [_ ^java.io.Writer w]
  (.write w "#clj-surgeon.measured/reading"))

(defmethod print-method Tick [_ ^java.io.Writer w]
  (.write w "#clj-surgeon.measured/tick"))

(defn raw-nanos
  "The monotonic clock, untagged. Allow-listed call sites only."
  []
  (System/nanoTime))

(defn raw-ms
  "The wall clock in epoch milliseconds, untagged. Allow-listed call sites only."
  []
  (System/currentTimeMillis))

(defn reading
  "Tag `n` as a number a clock produced."
  [n]
  (->Reading n))

(defn reading?
  "Is `x` a tagged clock reading?

  A type test, so it is exact and it is cheap — this predicate runs over every
  value of every published result. It also cannot throw: the previous
  shape-based test had to reason about `PersistentTreeMap`s keyed by strings
  (the formatter's staged sources) because a keyword lookup in one goes through
  `compareTo`; a type test has no such hazard."
  [x]
  (instance? Reading x))

(defn start
  "An opaque start tick for `elapsed-ms` / `elapsed-nanos`.

  Opaque on purpose: `(- (raw-nanos) (start))` does not typecheck, so the only
  way to turn a start tick into a duration is `elapsed-ms` / `elapsed-nanos`,
  and those return tagged readings."
  []
  (->Tick (raw-nanos)))

(defn value
  "The bare number inside a reading; `x` unchanged when it is not one.

  THE ONE LAUNDERING VERB, and now that is a fact about the type rather than a
  promise in a docstring. Every call site in `src/` is named in the invariant
  witness's allow-list with the reason it needs a bare number."
  [x]
  (if (reading? x) (-launder x) x))

(defn- start-nanos
  [started]
  (cond
    (instance? Tick started) (long (-launder started))
    (number? started) (long started)
    :else (throw (ex-info "A measured interval needs a start tick"
                          {:error-type :invalid-measured-start
                           :started-type (some-> started class .getName)}))))

(defn wall-clock-ms
  "The epoch wall clock in milliseconds, as a TAGGED reading.

  The receipt counterpart of `raw-ms`. A stamp saying WHEN something happened
  is still a number a clock produced, so it belongs in the measured partition
  exactly like a duration does — and a receipt field that wants one calls this
  rather than earning an allow-list entry for a raw read."
  []
  (reading (raw-ms)))

(defn file-modified-ms
  "The modification stamp of `file` in epoch milliseconds, as a TAGGED reading;
  nil when the file is not there.

  `(.lastModified f)` returns 0 for an absent file, which reads as the epoch
  rather than as \"no answer\", so the absent case is nil and the caller decides.

  This verb exists because of the round-four review's second blocking finding
  (2026-09-04 §2): `mcp_admit_tool` published `(.lastModified report)` as
  `:report_written_at` inside the hashed parity subject, two lines above a wall
  clock the same scan had just routed, and two runs of one unchanged operation
  hashed differently. A file mtime is not \"now\", so `wall-clock-ms` is the
  wrong verb for it; what it shares with `wall-clock-ms` is the only thing that
  matters at the boundary — the TAG.

  @spec MCP-OP-TIME-007"
  [^java.io.File file]
  (let [ms (.lastModified file)]
    (when (pos? ms) (reading ms))))

(defn elapsed-nanos
  "Nanoseconds since `started`, as a tagged reading."
  [started]
  (reading (- (raw-nanos) (start-nanos started))))

(defn elapsed-ms
  "Milliseconds since `started`, as a tagged reading."
  [started]
  (reading (/ (double (- (raw-nanos) (start-nanos started))) 1000000.0)))

(defn- unwrap-readings
  "`x` with every tagged reading, at any depth, replaced by its bare number.

  What a measured BLOCK holds: once a value is inside the partition its
  provenance is stated by the block it lives in, and the wire carries a plain
  JSON number rather than a nested object.

  PRIVATE, and that is the round-four review's blocking finding (§1). While
  this was public it was a second laundering door at arbitrary depth that
  called `value` internally, so no CALLER of it matched the escape-hatch
  pattern and the reviewer walked an undeclared clock field into the parity
  hash with every witness green. It is now reachable only from the three verbs
  that BUILD or PARTITION a block — `measured`, `attach`, `partition-measured`
  — which is the boundary the whole namespace exists to be.

  FOREIGN COLLECTIONS ARE LEFT ALONE, deliberately (round-five review §4): a
  caller's `ArrayList` or `TreeMap` cannot honestly be replaced by a vector or
  a Clojure map, and guessing at a replacement is how a `ClassCastException`
  reached the publication boundary in round three. `unpartitioned-measured-paths`
  DOES walk them, so a reading inside one is a typed
  `:unpartitioned-measured-field` refusal naming its path rather than a value
  that quietly reaches the wire.

  @spec MCP-OP-TIME-006"
  [x]
  (cond
    (reading? x) (value x)
    (map? x) (reduce-kv (fn [acc k v]
                          (let [v' (unwrap-readings v)]
                            (if (identical? v' v) acc (assoc acc k v'))))
                        x x)
    (vector? x) (reduce-kv (fn [acc i v]
                             (let [v' (unwrap-readings v)]
                               (if (identical? v' v) acc (assoc acc i v'))))
                           x x)
    (set? x) (let [ys (map unwrap-readings x)]
               (if (every? true? (map identical? ys x)) x (set ys)))
    (seq? x) (map unwrap-readings x)
    :else x))

(defn measured
  "A measured block: `m` is a map of fields a clock produced.

  Readings in `m` are unwrapped — inside the block the provenance is the block."
  [m]
  {measured-key (unwrap-readings m)})

(defn hashed-channel
  "Project `x` onto its hashed channel — `x` with every `:measured` block
  removed, at any depth.

  STRUCTURE-SHARING BY CONSTRUCTION. A sub-value that carries no measured
  field is returned IDENTICALLY, so projecting a 10,000-file result allocates
  one new vector spine and one new receipt map, not a second copy of the
  result. This matters: the battery hashes the projection while the result is
  still referenced and the heap sampler is running, and a projection that
  copied the result would move the very numbers the battery exists to measure."
  [x]
  (cond
    (map? x)
    (reduce-kv (fn [acc k v]
                 (if (= k measured-key)
                   (dissoc acc k)
                   (let [v' (hashed-channel v)]
                     (if (identical? v' v) acc (assoc acc k v')))))
               x x)

    (vector? x)
    (reduce-kv (fn [acc i v]
                 (let [v' (hashed-channel v)]
                   (if (identical? v' v) acc (assoc acc i v'))))
               x x)

    (set? x)
    (let [ys (map hashed-channel x)]
      (if (every? true? (map identical? ys x)) x (set ys)))

    (seq? x)
    (map hashed-channel x)

    :else x))

(defn strip-measured-lines
  "The TEXT encoding's hashed channel: `text` with every measured line removed.

  The text counterpart of `hashed-channel`, and the reason it exists in
  product code rather than as a regex in whichever witness needed it first."
  [text]
  (->> (str/split-lines text)
       (remove #(str/starts-with? % text-measured-prefix))
       (str/join "\n")))

;; ============================================================
;; The invariant: a measured field enters a receipt ONLY through the partition
;; ============================================================

(def measured-field-names
  "Every field name in this repository whose value a CLOCK produced.

  The partition was a site fix before it was an invariant. `hashed-channel`
  removed a block whose key was literally `:measured`, and the shared MCP
  operation finalizer attached its wall-clock reading as a top-level
  `:elapsed_ms` — so measured data reached the hash subject by a SECOND route
  and the projection was blind to it (Sol review, 2026-09-04, §1).

  This set is the invariant's vocabulary. `partition-measured` relocates every
  one of these, at any depth, into the `:measured` block at its own level, and
  `clj-surgeon.measured-invariant-test` scans `src/` for clock reads and fails
  when a new clock site publishes a name this set does not carry. A name is in
  here because a clock produced it — never because it is merely a number."
  #{:elapsed_ms :elapsed-ms :job_elapsed_ms :inspection_elapsed_ms
    :scan_ms :window-ns :max-ns :wall-ms})

(defn attach
  "Add measured `fields` to `x`'s measured block, keeping what is already there.

  Readings in `fields` are unwrapped: the block states the provenance."
  [x fields]
  (update x measured-key merge (unwrap-readings fields)))

(defn partition-measured
  "Relocate every measured value in `x` into the `:measured` block at its OWN
  level, at any depth.

  A value is measured when EITHER holds:

  - it is a TAGGED READING (`reading?`) — provenance carried by the value, so
    the key it sits under is irrelevant and an undeclared name cannot smuggle
    it past; or
  - its key is in `measured-field-names` — the name vocabulary, kept for the
    fields a caller or an adapter constructs from data rather than from a
    sanctioned clock read.

  This is the publication boundary's half of the invariant. Code inside an
  operation may compute and pass a clock reading in whatever shape suits it;
  what it may not do is PUBLISH one outside the partition. Applying this at the
  single boundary every public result already passes through makes that
  impossible by construction rather than by everybody remembering.

  STRUCTURE-SHARING, like `hashed-channel`: a sub-value carrying no measured
  field comes back `identical?`, so partitioning a large result allocates the
  changed spine and nothing else. A `:measured` block is never relocated into
  another one — its contents are already measured — but readings inside it ARE
  unwrapped, so the block always holds bare numbers."
  [x]
  (cond
    (reading? x) x

    (map? x)
    (let [found (reduce-kv (fn [acc k v]
                             (if (and (not= k measured-key)
                                      (or (reading? v)
                                          (contains? measured-field-names k)))
                               (assoc acc k (unwrap-readings v))
                               acc))
                           nil x)
          base (reduce-kv (fn [acc k v]
                            (cond
                              (= k measured-key)
                              (let [v' (unwrap-readings v)]
                                (if (identical? v' v) acc (assoc acc k v')))

                              (or (reading? v) (contains? measured-field-names k))
                              (dissoc acc k)

                              :else
                              (let [v' (partition-measured v)]
                                (if (identical? v' v) acc (assoc acc k v')))))
                          x x)]
      (cond
        ;; A reading used as a KEY has no name to be relocated under, so
        ;; there is nothing honest to do with it here. Leave `x` exactly as it
        ;; was: `unpartitioned-measured-paths` reports the key position and the
        ;; boundary raises the typed `:unpartitioned-measured-field` refusal.
        ;; Silently carrying it would put a clock-derived value into the hash
        ;; subject as a MAP KEY, which is where the round-three review found it
        ;; surviving (§5b).
        (some reading? (keys x)) x

        (nil? found) base

        :else
        ;; `assoc`ing the keyword `:measured` into a SORTED map keyed by
        ;; strings — the formatter's staged sources are exactly that shape —
        ;; throws `ClassCastException` from `compareTo`, which the round-three
        ;; review reproduced (§5b): a raw JVM exception at the publication
        ;; boundary instead of a receipt or a refusal. There is no honest place
        ;; to put the block, so the map is returned untouched and the boundary
        ;; refuses it by path, typed, like every other unpartitionable reading.
        (try
          (update base measured-key merge found)
          (catch ClassCastException _ x))))

    (vector? x)
    (reduce-kv (fn [acc i v]
                 (let [v' (partition-measured v)]
                   (if (identical? v' v) acc (assoc acc i v'))))
               x x)

    (set? x)
    (let [ys (map partition-measured x)]
      (if (every? true? (map identical? ys x)) x (set ys)))

    (seq? x)
    (map partition-measured x)

    :else x))

(defn unpartitioned-measured-paths
  "Every path in `x` at which a measured value sits where a public result may
  not carry it — empty when the invariant holds.

  Two families, and both are defects of the same class:

  - a `measured-field-names` key OUTSIDE a `:measured` block (the name route
    the first repair covered);
  - a TAGGED READING anywhere at all, including inside a `:measured` block —
    outside the block it is measured data beside the hash subject, and inside
    it, it is a wrapper that would reach the wire as a nested JSON object
    instead of a number.

  LAZY, so the boundary's refusal can short-circuit on the first offender
  rather than walking a ten-thousand-record result to build a vector nobody
  reads."
  [x]
  (letfn [(walk [node path in-measured?]
            (cond
              (reading? node) [path]

              (map? node)
              (mapcat (fn [[k v]]
                        (cond
                          ;; A reading used as a KEY. The walk has to look at
                          ;; keys as well as values: `partition-measured` can
                          ;; only relocate a reading that sits UNDER a name, so
                          ;; a reading standing where a name should be reached
                          ;; the hashed channel untouched until this line
                          ;; (round-three review §5b).
                          (reading? k) [(conj path ::reading-as-key)]
                          (= k measured-key) (walk v (conj path k) true)
                          (and (not in-measured?)
                               (contains? measured-field-names k)
                               (not (reading? v)))
                          [(conj path k)]
                          :else (walk v (conj path k) in-measured?)))
                      node)

              (or (vector? node) (seq? node) (set? node))
              (mapcat #(walk %1 (conj path %2) in-measured?) node (range))

              ;; A FOREIGN collection. These two clauses come AFTER the Clojure
              ;; ones on purpose: a Clojure map IS a `java.util.Map` and a
              ;; vector IS a `java.util.Collection`, so the fast paths above
              ;; must claim them first.
              ;;
              ;; Round-five review §4: all three walkers walked Clojure
              ;; collections only, so a reading inside an `ArrayList` was
              ;; neither unwrapped, relocated, nor diagnosed — it reached the
              ;; published result untouched with `unpartitioned []`, and became
              ;; visible only when cheshire refused to encode it. The DIAGNOSTIC
              ;; is the walker that has to see it, because seeing it is what
              ;; turns the loud-but-late encoder failure into the same typed
              ;; `:unpartitioned-measured-field` refusal the other placements
              ;; get.
              ;;
              ;; The two REWRITING walkers deliberately do not rebuild a
              ;; foreign collection: there is no honest way to know that a
              ;; caller's `ArrayList`, `LinkedHashMap` or sorted `TreeMap` may
              ;; be replaced by a vector or a Clojure map, and guessing is how
              ;; the sorted-map `ClassCastException` reached the boundary in
              ;; round three. So a reading in a Java collection is REFUSED
              ;; rather than relocated, and the refusal names its path.
              (instance? java.util.Map node)
              (mapcat (fn [^java.util.Map$Entry entry]
                        (let [k (.getKey entry)
                              v (.getValue entry)]
                          (cond
                            (reading? k) [(conj path ::reading-as-key)]
                            (= k measured-key) (walk v (conj path k) true)
                            (and (not in-measured?)
                                 (contains? measured-field-names k)
                                 (not (reading? v)))
                            [(conj path k)]
                            :else (walk v (conj path k) in-measured?))))
                      (.entrySet ^java.util.Map node))

              (instance? java.util.Collection node)
              (mapcat #(walk %1 (conj path %2) in-measured?)
                      (seq node) (range))

              ;; A JAVA ARRAY. Round-six review §5: an `Object[]` is neither a
              ;; `java.util.Map` nor a `java.util.Collection`, so the two
              ;; clauses above walked past it and the diagnostic returned `[]`
              ;; for a reading sitting inside one. The number did not reach the
              ;; wire -- cheshire refuses a `Reading` -- but it failed as an
              ;; encoder stack trace instead of as the typed
              ;; `:unpartitioned-measured-field` refusal that names the PATH,
              ;; and naming the path is the entire value of the diagnostic.
              ;; `(seq node)` is the array's element sequence and does not
              ;; rebuild it; like the two clauses above this DIAGNOSES, and the
              ;; rewriting walkers still refuse rather than guess at a foreign
              ;; container's identity.
              (and (some? node) (.isArray (class node)))
              (mapcat #(walk %1 (conj path %2) in-measured?)
                      (seq node) (range))

              ;; An ITERATOR is REFUSED OUTRIGHT, not walked. Round-six review
              ;; §5 named it alongside the array and it is not the same case:
              ;; an iterator cannot be inspected without CONSUMING it, so a
              ;; walker that diagnosed one would hand the boundary a verdict
              ;; about a value it had just destroyed, and whatever encoded the
              ;; result afterwards would serialise an exhausted iterator. The
              ;; honest answer is to report the path itself as unpartitioned and
              ;; let the boundary raise its typed refusal -- an iterator has no
              ;; business in a published MCP result under any circumstances, so
              ;; refusing it costs nothing real and guessing would cost the
              ;; value.
              (instance? java.util.Iterator node)
              [path]

              :else nil))]
    (walk x [] false)))

(defn first-unpartitioned-measured-path
  "The first path `unpartitioned-measured-paths` would report, or nil.

  The boundary's refusal predicate: short-circuits on the first offender."
  [x]
  (first (unpartitioned-measured-paths x)))
