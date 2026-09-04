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
