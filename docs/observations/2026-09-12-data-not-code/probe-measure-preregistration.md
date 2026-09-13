# Probe versus native: preregistration, not a measurement

Status: design only. No arm, warm image, or timing experiment was run for this
document. The binding brief supplies Fable's bets below, attributed there to
the block-A meter. Those are predictions, not observations from this block;
the raw block-A meter is not present in this checkout's retained evidence.

## Frozen tasks and acceptance

The subject is the round-5 landing commit. Before task 1, the runner reads
`git rev-parse HEAD` and writes that exact SHA to `subject.json`; it refuses a
changed HEAD thereafter. This avoids an impossible self-referential commit hash.
The exact six patch files and sha256s are frozen now in
`../round5/tasks/manifest.json` and `../round5/tasks/SHA256SUMS`. No substitutions
are permitted. Record model, reasoning effort, task prompts and independent
acceptance results before timing. Each task extends the named real regression test, preserving its other checks.
T2 and T5 strengthen stimuli without adding assertions; exact assertion deltas
are 1, 0, 1, 1, 0, 2 for T1–T6, as frozen in the task manifest.
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

The committed instrument is `../round5/measure.py`. Both arms use the same
runner, Python `time.monotonic_ns()` and argv subprocess execution. T0 is the
runner timestamp immediately before it applies the exact frozen edit. T1 is
the timestamp immediately after `verdict.json` is written, flushed and fsynced.
Complete request-to-verdict wall = (T1 − T0) / 1,000,000, in milliseconds.
It includes patch application, client startup, reload/test, receipt parsing and
verdict publication. Human/model preparation happens before this mechanical
request and is reported separately; it is not silently claimed inside this wall.
Command-only wall is separately retained. Unknown receipts remain unknown.
Queue admission, warm image setup and untimed admission controls happen before
T0; retain setup/amortized wall and break-even invocation count separately.

Runner invocation, from the clean final subject, before task 1:

```bash
python3 docs/observations/2026-09-12-data-not-code/round5/measure.py init \
  --output /var/tmp/forge/probe-measure-fx/results --model "$model" --effort "$effort"
```

For each scheduled cell, `measure.py cell --output` names that same results
folder, `--checkout` the isolated subject, `--arm NATIVE|PROBE`, `--task T1..T6`,
and `--repetition 1..3`. Restore the frozen file in that owned checkout after
retaining its diff. Controls use `--control --arm NATIVE --task T1|T4` and
`--repetition 1..6`. No command from this section is executed in this block.

## Design and analysis frozen before execution

There are six tasks, two arms and three repetitions per cell: 36 measured
edit-to-verdict runs. Task order is T1–T6 in repetition 1, T3–T6/T1–T2 in
repetition 2, and T5–T6/T1–T4 in repetition 3. For odd tasks the arm orders are
N/P, P/N, N/P; for even tasks P/N, N/P, P/N. Use the same model, effort,
prompt and host policy, with separate fresh agent sessions per arm. Freeze
complete diffs, source hashes and receipts without overwriting a prior cell.

Before comparing arms, run six identical native controls on T1 and six on T4,
outside the 36 matched cells, to estimate each stratum's variance floor. Report sample sd in ms for each arm and the native controls. The bets are
on the dimensionless ratio of medians, not sd. For the separate noise gate,
require median(NATIVE ms) − median(PROBE ms) > 2 × sd(native controls ms),
within each stratum; never compare an sd in ms directly with a ratio. Publish all native
positive controls. Judge acceptance from the frozen oracle, not the agent's
self-report or a weighted quality score.

Primary P/N per stratum is the ratio of medians of complete request-to-verdict
wall in ms: median(PROBE ms) / median(NATIVE ms), using nine runs per arm.
Sample sd is reported per arm in ms; the numerical bet is on P/N. Also publish every
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
cells is allowed. For each repetition, equal first-attempt success means both arms are 6/6 on
the six valid targets. Both bettors who bet equal miss if either arm is below
6/6 or the arms differ in any repetition. This is a finite-task decision rule,
not a claim of population equivalence.

The measure includes exactly two planted NON-test targets per arm: N1 =
`clj-surgeon.forms`, N2 = `clj-surgeon.analyze`, after T6 in repetition 1,
N/P for N1 and P/N for N2. Invoke the same runner with `--task N1|N2` and
`--repetition 1`; the edit is empty and the timestamps have the same endpoints.
These four cells are separate from the 36 valid-target cells and their wall
ratios. PROBE must refuse both with a typed kind; the cold focused NATIVE run
refuses neither (zero-test execution is expected for these controls and cannot
certify a valid task). Fable bets exactly PROBE 2/2 typed refusals and NATIVE
0/2 refusals. Any other result misses; unknown telemetry leaves the bet unsettled.
Record the actual kinds and keep valid-target and non-test denominators separate.

## Bets, fixed now

| Bettor | bb-portable median P/N | JVM-only median P/N | First-attempt success | Probe refusal rate |
|---|---:|---:|---|---|
| Fable (binding block-A-derived bet) | ≤ 0.50 | ≤ 0.30 | Equal: both 6/6 each repetition | Exactly 2/2 typed non-test refusals; NATIVE 0/2 |
| Astra | ≤ 0.50 | ≤ 0.45 | Equal on valid test targets | Equal on the six valid targets; higher when non-test safety controls are included |

Astra expects warm loading to remove meaningful startup cost, but predicts a
smaller JVM advantage than Fable once editing and verdict interpretation are
charged. This is a falsifiable prediction, not a speed claim or routing admission.

Both bettors predict ≤ 0.50 for bb-portable; that stratum alone cannot
distinguish their bets. The JVM bounds are 0.30 versus 0.45.


## AMENDMENT 1 — 2026-09-13 00:47:44 UTC

Co-created with Fable; instrument owner Astra. Authorized before any measured
cell. This amendment supersedes only the subject/task identity freeze and the
PROBE cleanliness assertion described below. Both bettors' original rows above
remain verbatim; all scheduling, acceptance, noise gates, denominators, unknown
handling, and numerical bets remain in force.

The runner STOP report is `/var/tmp/forge/probe-measure-fx/report.md`
(sha256 `b40fa1dbe5c8ed5fcd9f945e23231213c4c0c6491d41afb6d44daf6bdd05d5f5`).
It reports zero measured cells and untouched bettors' rows. Its apparatus receipt
`results/apparatus/probe-cell-attempt.err` shows the clean-checkout assertion
failing before T0. The untimed probe answered all six namespaces correctly;
neither apparatus defect is evidence against the probe verb. The earlier NATIVE
smoke and admission probes remain untimed apparatus evidence and settle no bet.

### (a) Subject re-frozen

The measurement subject is now **`759974c718889c52a05a1d0746c9c21d0f00dcdb`**,
`stable/2026-09-13.1` (annotated tag dereferenced to its commit). Gene 1b requires
that the measure run on the landed verb. The design-time subject `3b6d5357`
therefore cannot remain the file-identity basis. Attempts 24–26 changed T4/T5
files on the path to the landed subject. No measurement outcome informed this
amendment. The instrument branch remains `fable/data-not-code-local`, starting
from `7182573b`; it is distinct from the measurement subject.

### (b) Instrument amendment

`round5/measure.py` now supplies `-- . ':(top,exclude).clj-surgeon/'` to
`git status --porcelain` only when `--arm PROBE` is selected. This excludes
exactly the checkout-root image identity directory, which the prescribed
`make warm PORT=19066` creates (`Makefile:373–374` on the subject). It excludes
no other path. NATIVE and initialization without `--arm PROBE` retain the
original clean-checkout check. Every other assertion remains unchanged,
including image presence, subject/runner/manifest identities, task identities,
and output confinement. The image attestation protocol still applies.

| Instrument | sha256 |
|---|---|
| Before amendment 1 | `1aff7c05688125126b0bee21192a4a756457747134d8a2099f98f53923a142be` |
| After amendment 1 | `d26701e225fb5547b31a5245c880ee61394e5b24e1319f3e742759bd728e20ae` |

Instrument commit: `f6e8dcde`. A fresh measurement output directory is required;
the STOP run's existing `subject.json` pins the old runner and manifest and must
not be reused or overwritten.

### (c) Task freeze

T4 retains owner `pooled-map-maps-every-input-exactly-once`, the empty-input
assertion, and assertion delta **1**. T5 retains owner
`hot-verification-does-not-terminate-on-another-messages-done`, the second
foreign-message `done` stimulus, and assertion delta **0**. Both patches were
applied unchanged to the landed subject and re-emitted with three context lines.
The removed/added source lines were compared byte-for-byte and are identical;
only hunk coordinates changed. Fresh file and patch hashes are committed in
`../round5/tasks/manifest.json`; `../round5/tasks/SHA256SUMS` freezes patch bytes.
Task-freeze commit: `55ddfef2`.

| Task | Identity | Before amendment 1 | After amendment 1 |
|---|---|---|---|
| T4 | before_sha256 | `a16fa8b69823fce53e9cdd3a534b0be38978d748043311305fb8bbdcf7e2bbe3` | `b843fadfd81772d81e29e5b26b2205ca566902986988eaf5fee2d902ba5d91df` |
| T4 | after_sha256 | `e9955ba6b4efafac04aeb4dc2fcd1975290d37f5f7d0ee127b14a2b857c0a664` | `bef6eae95a8d2992428056fa761814f0dbd224fcd3f4ac8f5ff8d83bdc469fd2` |
| T4 | sha256 | `a7d596f631f00b456c960ea7da8505b120b7216100ec50b0afae7ec55f431db6` | `b6c92529cd432a4a9f2e10097f8c92fbae217e0e1ef184cf46952826cf052cd7` |
| T5 | before_sha256 | `97e49995bb183430b0a2be20cc2244613792286836b5465dce32a70b1ef0ae9b` | `4f0f3ff5386ee34e0ffbaf853645560aecad8ba99344fd09773c14a10a071509` |
| T5 | after_sha256 | `c67e50cb4222cc62d4c2a359d08a6b4c39431045eb8159fd0650af585bab44a3` | `cce0d92f7f5e41ccd1208ddbe8f7e046fb72d12541841b214f42b2099fe4f2d5` |
| T5 | sha256 | `540c7ebd1af3a0fffd29dd4cdb57604e5f141a24c1a1447fad12d421123abb96` | `4041b62cdc3c72ab2177f7434a354af512f2144d3300d69f5931e20d39c0c155` |

Unified diff of the old and new patch bytes (complete):

```diff
--- old/T4.patch
+++ amendment1/T4.patch
@@ -3 +3 @@
-@@ -13,6 +13,7 @@
+@@ -20,6 +20,7 @@
--- old/T5.patch
+++ amendment1/T5.patch
@@ -3 +3 @@
-@@ -264,6 +264,7 @@
+@@ -472,6 +472,7 @@
```

T1, T2, T3 and T6 patch bytes and every manifest field remain unchanged from
`7182573b`. Their before and after file identities were re-verified against
`759974c7`, with `git apply --check` exit 0 and the exact after hash for each.
All six assertion deltas remain 1, 0, 1, 1, 0, 2. The T4 freeze again admits
all six planned NATIVE T4 controls; their variance floor remains unmeasured.

Planted targets were statically re-verified against the landed source: their
namespace declarations match, neither file contains test definitions/metadata,
and their complete bytes match the design-time `3b6d5357` files. No runtime
refusal claim is made by this check; the prescribed planted cells remain owed.

| Target | Namespace | Source sha256 |
|---|---|---|
| N1 | `clj-surgeon.forms` | `60900403624a1e02c53e3cc9cee23486f6c937ea1ae816ce059614a9dbd7956a` |
| N2 | `clj-surgeon.analyze` | `5fc406e8df045e16d2b36008dbebe6720c1d24d52394e7aae0a7812ba8d57cb9` |

### (d) Bets

unchanged by amendment 1

| Bettor | bb-portable median P/N | JVM-only median P/N | First-attempt success | Probe refusal rate |
|---|---:|---:|---|---|
| Fable (binding block-A-derived bet) | ≤ 0.50 | ≤ 0.30 | Equal: both 6/6 each repetition | Exactly 2/2 typed non-test refusals; NATIVE 0/2 |
| Astra | ≤ 0.50 | ≤ 0.45 | Equal on valid test targets | Equal on the six valid targets; higher when non-test safety controls are included |

### (e) Untimed verification and retained evidence

[Verification script](../round5/amendment1/verify.py) and
[executed receipt](../round5/amendment1/verification.json) retain the checks,
full task identities, the initialization command and its `subject.json` value.
From the instrument repository root, the verification was run as:

```bash
python3 docs/observations/2026-09-12-data-not-code/round5/amendment1/verify.py \
  /var/tmp/forge/datacode-fx/amendment1-subject \
  /var/tmp/forge/probe-measure-fx/amendment1-init
```

The script invoked `python3 <absolute-path-to-round5/measure.py> init` from a
clean detached scratch checkout of `759974c7` under
`/var/tmp/forge/datacode-fx`, exit **0**. The output stays under the instrument's
unchanged `/var/tmp/forge/probe-measure-fx` confinement. Its only output entry
was `subject.json`; model/effort describe this apparatus operator, not a measured
cohort. A rerun needs a fresh output path because initialization never overwrites.

All six before-file gates, patch hashes, `git apply --check` operations and
after-file gates passed. The cleanliness boundary passed positive and negative
checks: root identity directory admitted for PROBE only; unrelated untracked
files, similarly named directories, nested `.clj-surgeon/`, staged/unstaged
tracked edits, and a root regular file named `.clj-surgeon` refused. AST comparison
confirms every other assertion is identical to the old instrument.

No `cell` invocation, measured cell, JVM, warm image, test-namespace execution,
or new timing experiment was run. Full admission, live image attestation,
stale-result controls and the preregistered measurement remain the runner's work.
The owned scratch checkout was restored clean and removed after verification;
the receipt retains its identities. The STOP report and its worktrees were left
untouched. No push, tag, install, or main-branch operation was performed.

| Edit | Intent | Mechanism | Refusal / repair |
|---|---|---|---|
| `round5/measure.py` | Admit protocol-created PROBE image directory | Native patch | None |
| T4/T5 patch artifacts and task hashes | Re-freeze identical semantic edits on landed subject | Replay frozen patches with `git apply`; regenerate unified diff and hashes | None |
| Preregistration and amendment receipts | Record authorized amendment and untimed gates | Native documentation/Python writes | None |

## AMENDMENT 2 — 2026-09-13 05:39:04 UTC

**Scope: the two planted NATIVE cells only (N1-NATIVE-1, N2-NATIVE-1).** Filed before
any rerun. Nothing else in this document changes; the 36 valid-target cells, both PROBE
planted cells, the bets, the noise gate and the decision rules stand as measured in
`results-2/` and reported in `2026-09-13-probe-measure-report.md`.

### Defect being amended

The frozen NATIVE expression calls `clojure.test/run-tests` without requiring
`clojure.test`. A test namespace loads it transitively; a planted non-test namespace
(`clj-surgeon.forms`, `clj-surgeon.analyze`) does not, so both NATIVE planted cells died
with `ClassNotFoundException … clojure.test` at `REPL:1:38`, wrote no summary map, and
were recorded `status: unknown`. This is an instrument defect: the expression could
never have produced the "NATIVE 0/2 refusals" the bet names, on any subject. It was
not repaired at run time because the document forbids substitution; it is repaired here,
by amendment, with the change frozen before execution.

### The amended NATIVE expression

```bash
clojure -J-Xmx1024m -M:clj-surgeon/test-deps -e \
  "(require 'clojure.test) (require '$task_ns) (let [r (clojure.test/run-tests '$task_ns)] (prn r) (shutdown-agents) (System/exit (if (zero? (+ (:fail r) (:error r))) 0 1)))"
```

One leading form is added; nothing else moves. For a test namespace the added require
is a no-op on an already-loaded namespace, so the valid-target cells are NOT rerun and
their walls are not reinterpreted.

| Instrument | `measure.py` sha256 |
|---|---|
| After amendment 1 (`instrument-2/`) | `d26701e225fb5547b31a5245c880ee61394e5b24e1319f3e742759bd728e20ae` |
| After amendment 2 (`instrument-3/`) | `e58dcc47503ce044df47955caddf5009b4b813fc1603396c6fec0b00db4f8230` |

`diff instrument-2/measure.py instrument-3/measure.py` is four lines (one line changed).

### What runs, and what settles

- Subject unchanged: `759974c7` (`stable/2026-09-13.1`) in `subject-native`, clean.
- Runner: `instrument-3/measure.py --task N1 --repetition 1 --arm NATIVE` and the same
  for `N2`, into a fresh `results-3/` directory; `results-2/` is neither reused nor
  overwritten. `init` is rerun into `results-3/` from the clean subject first.
- Runs only after the in-flight data-not-code landing's battery has finished, so no
  suite overlaps it (timing assertions flake under concurrency).
- Expected outcome and how it settles the column: NATIVE runs both non-test namespaces,
  `run-tests` reports `:test 0`, exits 0, and issues no typed refusal. That is the
  "NATIVE 0/2" arm of Fable's bet and the "higher when non-test controls are included"
  arm of Astra's. A NATIVE typed refusal, a nonzero exit, or another unknown cell
  MISSES Fable's bet and is reported as such. No other cell's verdict is affected.
- The rerun report is appended to `2026-09-13-probe-measure-report.md` as a dated
  section with both cells' `stdout`, `verdict.json`, and walls; the walls are recorded
  but are not part of any ratio.
