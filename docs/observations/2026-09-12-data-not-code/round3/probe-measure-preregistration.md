# Probe versus native: preregistration, not a measurement

Status: design only. No arm, warm image, or timing experiment was run for this
document. The binding brief supplies Fable's bets below, attributed there to
the block-A meter. Those are predictions, not observations from this block;
the raw block-A meter is not present in this checkout's retained evidence.

## Frozen tasks and acceptance

The subject is the final data-not-code branch commit. Before any timing, freeze
that SHA, runner hash, model and reasoning effort, six exact patches, their
hashes, task prompts and independent acceptance results. Each task adds a real
regression assertion to the named existing test, preserving its other checks.
Both arms apply the same patch in isolated copies of that subject. No synthetic
sleep, padded source, reduced namespace, skipped test, or timing-driven task
replacement is permitted.

| Task | Test namespace / owner | Edit to freeze | Stratum |
|---|---|---|---|
| T1 | `clj-surgeon.forms-test/test-classify-namespace-qualified-auto-detection` | Add the namespaced `schema.core/defn-` classification regression. | bb-portable |
| T2 | `clj-surgeon.analyze-test/test-qualified-symbols` | Extend the real nested call fixture with another call through `db`; retain the exact distinct prefix set. | bb-portable |
| T3 | `clj-surgeon.edit-dsl-test/builders-preserve-clojure-data-without-evaluation` | Add a quoted, namespaced symbol as structural data and assert exact compiled query data. | bb-portable |
| T4 | `clj-surgeon.census-pool-test/pooled-map-maps-every-input-exactly-once` | Add the empty-input assertion to the actual pool mapper. | JVM-only |
| T5 | `clj-surgeon.mcp-hot-verify-test/hot-verification-does-not-terminate-on-another-messages-done` | Add a second foreign-message `done` response before the matching response; preserve the PID and successful-verdict assertions. | JVM-only |
| T6 | `clj-surgeon.mcp-formatter-test/default-formatter-prefers-path-then-checkout-then-npx` | Add a PATH-installed formatter whose path contains a space and assert it remains one argv element. | JVM-only |

JVM-only names are the manifest's declared JVM subjects; bb-portable names are
declared portable. Attest these classifications from the frozen subject before
running. A changed classification or an invalid task stops admission; do not
replace a task after seeing walls. Acceptance independently requires the exact
patch, expected assertion additions, full namespace execution, zero failures
and errors, and identical test/assertion counts between arms. Also conduct an
untimed stale-result negative control by deliberately falsifying one assertion
in an owned copy: both commands must report failure, then pass after restoration.

## Arms and exact commands

Run on this box under an exclusive suite lease. All scratch files are on disk
under `/var/tmp/forge/probe-measure-fx`. Use one active arm at a time. Never use
the shared installed tool as an unattested proxy for this checkout.

In each owned checkout, these shell setup commands define the exact entrances:

```bash
export TMPDIR=/var/tmp/forge/probe-measure-fx
export JAVA_TOOL_OPTIONS='-Xmx1024m -Djava.io.tmpdir=/var/tmp/forge/probe-measure-fx'
mkdir -p "$TMPDIR"
repo_root="$PWD"
clj-surgeon() {
  bb -Djava.io.tmpdir="$TMPDIR" -cp "$repo_root/src:$repo_root/libs/clj-splice/src" \
    -m clj-surgeon.core "$@"
}
```

NATIVE is the agent's own cold, complete focused JVM run after applying its
patch. `task_ns` is exactly the namespace in the table, without `/owner`:

```bash
clojure -J-Xmx1024m -M:clj-surgeon/test-deps -e \
  "(require '$task_ns) (let [r (clojure.test/run-tests '$task_ns)] (prn r) (shutdown-agents) (System/exit (if (zero? (+ (:fail r) (:error r))) 0 1)))"
```

PROBE is the same full namespace after the identical patch, through the local
client into the persistent warm image:

```bash
clj-surgeon :probe :ns "$task_ns"
```

Prepare that image before the measured task on an owned, available port. The
7906-class description denotes the persistent image architecture, not the port:
this client explicitly admits ports above 9000, so use 19066, never a reserved
shared port. In a separate owned terminal in the PROBE checkout:

```bash
make warm PORT=19066
```

Record `.clj-surgeon/probe.edn`, the live PID, generation, root, fingerprint,
HEAD, dependency/classpath identity, and startup wall. Before editing, run one
untimed probe for each target namespace to populate the image; retain those
receipts as setup costs. The warm process stays alive through that checkout's
assigned cells. Restore test-source fixtures between cells. The edited files
are test sources, not the probe's identity files; changing an identity file
requires a newly attested image and invalidates the affected cell.

Capture command wall with a monotonic-clock process wrapper using argv, exit
status, stdout and stderr. Do not confuse `:elapsed_ms` inside a probe receipt
with complete caller wall. The observer records T0 (task delivered), edit start,
patch completion, command start, process end and verdict understood. Primary
wall is T0 through the accepted verdict; it includes model returns, editing,
client startup, reload, execution, interpretation and any refusal/recovery.
Command-only wall and model-return counts are separate secondary measures.
Queue admission and image preparation happen before T0 and are reported
separately, including amortized wall and break-even invocation count.

## Design and analysis frozen before execution

There are six tasks, two arms and three repetitions per cell: 36 measured
edit-to-verdict runs. Task order is T1–T6 in repetition 1, T3–T6/T1–T2 in
repetition 2, and T5–T6/T1–T4 in repetition 3. For odd tasks the arm orders are
N/P, P/N, N/P; for even tasks P/N, N/P, P/N. Use the same model, effort,
prompt and host policy, with separate fresh agent sessions per arm. Freeze
complete diffs, source hashes and receipts without overwriting a prior cell.

Before comparing arms, run six identical native controls on T1 and six on T4,
outside the 36 matched cells, to estimate each stratum's variance floor. A wall
claim must clear twice that control standard deviation. Publish all native
positive controls. Judge acceptance from the frozen oracle, not the agent's
self-report or a weighted quality score.

Primary ratio per stratum is median(PROBE complete walls) divided by
median(NATIVE complete walls), using its nine runs per arm. Also publish every
task's three paired ratios and every raw wall. The 50% floor is met only if
both stratum ratios are at most 0.50, clear the variance floor, and preserve
acceptance and first-attempt success. A ratio above 0.50 falsifies that claim;
one stale green or missed assertion falsifies correctness regardless of wall.
Fable's stricter JVM prediction is evaluated separately at 0.30.

First-attempt success means the first prescribed command produces the correct
full-namespace verdict, with no repair. Refusal rate is refused first calls
divided by all first calls, counted by typed kind; retain missing telemetry as
unknown. A retry stays in its original complete wall and is never relabelled a
first-attempt success. Missing/terminated/unattested runs remain named unknown
cells; no replacement, imputation, outlier deletion or claim from incomplete
cells is allowed. Report whether first-attempt success differs, even when n=3
cannot establish population equivalence.

A separate untimed non-test-target control calls
`clj-surgeon :probe :ns clj-surgeon.core`; it must refuse before reload. Report
this safety control separately from the valid-target refusal rate, where
equal zero is compatible with Fable's greater-than-or-equal prediction.

## Bets, fixed now

| Bettor | bb-portable median P/N | JVM-only median P/N | First-attempt success | Probe refusal rate |
|---|---:|---:|---|---|
| Fable (binding block-A-derived bet) | ≤ 0.50 | ≤ 0.30 | Equal to native | ≥ native |
| Astra | ≤ 0.50 | ≤ 0.45 | Equal on valid test targets | Equal on the six valid targets; higher when non-test safety controls are included |

Astra expects warm loading to remove meaningful startup cost, but predicts a
smaller JVM advantage than Fable once editing and verdict interpretation are
charged. This is a falsifiable prediction, not a speed claim or routing admission.
