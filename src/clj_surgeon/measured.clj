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

(defn measured
  "A measured block: `m` is a map of fields a clock produced."
  [m]
  {measured-key m})

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
  "Add measured `fields` to `x`'s measured block, keeping what is already there."
  [x fields]
  (update x measured-key merge fields))

(defn field
  "Read one measured field from `x`'s measured block."
  [x k]
  (get-in x [measured-key k]))

(defn partition-measured
  "Relocate every `measured-field-names` entry in `x` into the `:measured`
  block at its OWN level, at any depth.

  This is the publication boundary's half of the invariant. Code inside an
  operation may compute and pass a clock reading in whatever shape suits it;
  what it may not do is PUBLISH one outside the partition. Applying this at the
  single boundary every public result already passes through makes that
  impossible by construction rather than by everybody remembering.

  STRUCTURE-SHARING, like `hashed-channel`: a sub-value carrying no measured
  field comes back `identical?`, so partitioning a large result allocates the
  changed spine and nothing else. A `:measured` block is never descended into —
  its contents are already measured, and relocating them again would nest one
  partition inside another."
  [x]
  (cond
    (map? x)
    (let [found (reduce-kv (fn [acc k v]
                             (if (contains? measured-field-names k)
                               (assoc acc k v)
                               acc))
                           nil x)
          base (reduce-kv (fn [acc k v]
                            (cond
                              (contains? measured-field-names k) (dissoc acc k)
                              (= k measured-key) acc
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
  "Every path in `x` at which a `measured-field-names` entry sits OUTSIDE a
  `:measured` block — empty when the invariant holds.

  The witness's subject, and a diagnostic a reader can run on any result."
  [x]
  (letfn [(walk [node path]
            (cond
              (map? node)
              (mapcat (fn [[k v]]
                        (cond
                          (= k measured-key) nil
                          (contains? measured-field-names k) [(conj path k)]
                          :else (walk v (conj path k))))
                      node)

              (or (vector? node) (seq? node) (set? node))
              (mapcat #(walk %1 (conj path %2)) node (range))

              :else nil))]
    (vec (walk x []))))
