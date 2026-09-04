(ns clj-surgeon.measured
  "The MEASURED channel: where a reading a clock produced is allowed to live.

  PURE. No I/O, no heavy requires — this namespace loads under babashka so the
  encoders and the battery can share ONE definition of the partition.

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
  @spec MCP-OP-MEM-011"
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

(def reading-key
  "The key of a TAGGED CLOCK READING: `{reading-key 12.5}`.

  Provenance that travels with the value. A reading is relocated into the
  measured partition wherever it is found, under whatever key it was published
  under — including a key nobody declared."
  ::reading)

(def started-key
  "The key of an opaque START TICK, as `start` returns it.

  Opaque on purpose: `(- (raw-nanos) (start))` does not typecheck, so the only
  way to turn a start tick into a duration is `elapsed-ms` / `elapsed-nanos`,
  and those return tagged readings."
  ::started)

(defn raw-nanos
  "The monotonic clock, untagged. Allow-listed call sites only."
  []
  (System/nanoTime))

(defn raw-ms
  "The wall clock in epoch milliseconds, untagged. Allow-listed call sites only."
  []
  (System/currentTimeMillis))

(defn start
  "An opaque start tick for `elapsed-ms` / `elapsed-nanos`."
  []
  {started-key (raw-nanos)})

(defn- start-nanos
  [started]
  (cond
    (and (map? started) (contains? started started-key)) (long (get started started-key))
    (number? started) (long started)
    :else (throw (ex-info "A measured interval needs a start tick"
                          {:error-type :invalid-measured-start
                           :started-type (some-> started class .getName)}))))

(defn reading
  "Tag `n` as a number a clock produced."
  [n]
  {reading-key n})

(defn reading?
  "Is `x` a tagged clock reading?

  Deliberately NOT `(contains? x reading-key)`. This predicate runs over every
  value of every published result, and a result legitimately holds SORTED maps
  keyed by file-name strings (the formatter's staged sources, for one). A
  keyword lookup in a `PersistentTreeMap` of strings goes through `compareTo`
  and throws `ClassCastException` — so the safe test is the shape a reading
  actually has: exactly one entry, and that entry's key is the tag."
  [x]
  (and (map? x)
       (== 1 (count x))
       (= reading-key (key (first x)))))

(defn value
  "The bare number inside a reading; `x` unchanged when it is not one.

  THE ONE LAUNDERING VERB. Every call site in `src/` is named in the invariant
  witness's allow-list with the reason it needs a bare number."
  [x]
  (if (reading? x) (get x reading-key) x))

(defn wall-clock-ms
  "The epoch wall clock in milliseconds, as a TAGGED reading.

  The receipt counterpart of `raw-ms`. A stamp saying WHEN something happened
  is still a number a clock produced, so it belongs in the measured partition
  exactly like a duration does — and a receipt field that wants one calls this
  rather than earning an allow-list entry for a raw read."
  []
  (reading (raw-ms)))

(defn elapsed-nanos
  "Nanoseconds since `started`, as a tagged reading."
  [started]
  (reading (- (raw-nanos) (start-nanos started))))

(defn elapsed-ms
  "Milliseconds since `started`, as a tagged reading."
  [started]
  (reading (/ (double (- (raw-nanos) (start-nanos started))) 1000000.0)))

(defn unwrap-readings
  "`x` with every tagged reading, at any depth, replaced by its bare number.

  What a measured BLOCK holds: once a value is inside the partition its
  provenance is stated by the block it lives in, and the wire carries a plain
  JSON number rather than a nested object."
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

(defn field
  "Read one measured field from `x`'s measured block."
  [x k]
  (get-in x [measured-key k]))

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
      (if found (update base measured-key merge found) base))

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
                          (= k measured-key) (walk v (conj path k) true)
                          (and (not in-measured?)
                               (contains? measured-field-names k)
                               (not (reading? v)))
                          [(conj path k)]
                          :else (walk v (conj path k) in-measured?)))
                      node)

              (or (vector? node) (seq? node) (set? node))
              (mapcat #(walk %1 (conj path %2) in-measured?) node (range))

              :else nil))]
    (walk x [] false)))

(defn first-unpartitioned-measured-path
  "The first path `unpartitioned-measured-paths` would report, or nil.

  The boundary's refusal predicate: short-circuits on the first offender."
  [x]
  (first (unpartitioned-measured-paths x)))
