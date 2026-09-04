(ns clj-surgeon.timing-sample
  "How a wall-clock gate on a SHARED build host reads a clock, and what it must
  put in the receipt when it does.

  PURE enough to load under babashka, because the gate that uses it is a bb
  script and a rule the gate re-implements is not a rule.

  The scar, 2026-09-04: two of `make memory-red`'s six assertions read a wall
  clock ONCE on a sixteen-core box shared with other JVM lanes and compared
  that single sample against a fixed 50 ms bound. A reviewer measured 52 ms and
  refused; the builder measured 13 ms on the same commit and passed. Both
  readings were honest. Neither run recorded what else the machine was doing,
  so the disagreement could not be settled from the receipts at all.

  The threshold is NOT relaxed — a gate that went red on somebody else's run is
  not one to soften in the same round. The MEASUREMENT is repeated instead, and
  the assertion is made against the LOWER ENVELOPE: scheduler noise only ever
  adds time, so the smallest of several readings is the closest to the cost
  being measured, and a genuine regression (every rep slow) still reads
  differently from contention (one rep slow).

  WHAT THIS DOES AND DOES NOT CLAIM. Minimum-of-N estimates UNCONTENDED cost.
  It is deliberately silent about tail latency and about reliability: it says
  the work CAN be done inside the bound on this host, never that it always is.
  A gate built on it may not be cited as a tail-latency guarantee.

  @spec MCP-OP-MEM-021"
  (:require
   [clojure.string :as str]))

(def minimum-probes
  "The fewest probes a wall-clock cell may assert on.

  Three, not two: with two readings a single scheduling stall still decides
  half the evidence, and there is no majority to read a genuine regression
  against."
  3)

(defn best
  "The fastest reading of `k` across `probes` — the lower envelope.

  A TYPED REFUSAL, not a convention: a caller that hands over fewer than
  `minimum-probes`, or a probe that reported no reading at all, gets an error
  rather than a number. This is the rung that makes 'assert on one sample'
  unrepresentable rather than merely discouraged."
  [probes k]
  (when (< (count probes) minimum-probes)
    (throw (ex-info "A wall-clock gate asserts on the best of several probes"
                    {:error-type :insufficient-timing-probes
                     :minimum minimum-probes
                     :observed (count probes)
                     :reading k})))
  (let [readings (mapv #(get % k) probes)]
    (when-not (every? number? readings)
      (throw (ex-info "A timing probe reported no reading"
                      {:error-type :missing-timing-reading
                       :reading k
                       :readings readings})))
    ;; NOT `(map long ...)`. `long` truncates toward zero, so a 49.9 ms
    ;; reading was asserted as 49 against a `< 50` bound — the permissive
    ;; direction, on the exact figure the gate exists to hold (round-three
    ;; review §6). The reading is compared as it was measured.
    (apply min readings)))

(declare host)

(defn detail
  "The receipt one wall-clock cell publishes: the asserted reading, every
  reading behind it, AND the host those readings were taken on.

  Publishing only the minimum would hide exactly the difference the rule exists
  to preserve — one slow rep is contention, every rep slow is a regression.

  `:host` is a FIELD of this map, not a line printed once at the top of a run.
  The round-three review found the load receipt separable from the numbers it
  explains (§6): `detail`'s map is the thing anyone quotes — the round-three
  record itself quotes `{:best-scan-ms 13, :scan-ms [14 13 13]}` — and the
  host line lived only in captured stdout, so the very disagreement MEM-021
  exists to settle (reviewer 52 ms, builder 13 ms) still could not be settled
  from a quoted receipt. A number that travels without the load that explains
  it is a number two honest people can read opposite ways."
  [probes asserted-key & other-keys]
  (into {(keyword (str "best-" (name asserted-key))) (best probes asserted-key)
         :host (host)}
        (map (fn [k] [k (mapv #(get % k) probes)]))
        (cons asserted-key other-keys)))

(defn host
  "The host's own state, which is part of every wall-clock reading taken on it.

  Not `slurp`: babashka reads a procfs file of declared length zero as an
  IOException, which is how this line first reported `unavailable` on a host
  that had the number all along."
  []
  {:cores (.availableProcessors (Runtime/getRuntime))
   :load (str/trim
           (or (try
                 (String.
                   (java.nio.file.Files/readAllBytes
                     (java.nio.file.Paths/get
                       "/proc/loadavg" (into-array String []))))
                 (catch Exception _ nil))
               "unavailable"))})

(defn host-line
  "The one-line host receipt a gate prints before its verdict lines."
  []
  (let [{:keys [cores load]} (host)]
    (format "host — %d cores, load %s" cores load)))
