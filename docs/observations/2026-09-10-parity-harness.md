# Byte-parity harness — same inputs, same outputs, or the candidate stops

**2026-09-10 · forge@anvil · branch `fable/parity-harness` in `/home/forge/src/clj-surgeon-parity`
(created from tag `stable/2026-09-10`, HEAD proved = `59d8bc0cad8886a028c24e8ffd52e4e5d82185c1`).
Eight commits, authored `forge-anvil`. Nothing pushed.
Sol's fence review returned NO-GO twice — on `d1343a87` and again on `4b594926`. All four
findings are fixed, each with a permanent witness, and every verdict below was re-measured under
the fences that resulted.**

## The verdict

**The public candidate `fable/public-candidate` changes exactly one observable thing, it changes
it on purpose, and with that one input pinned it is byte-identical to stable on all three
retained specimens.**

Verified at **three** tips, because the branch kept advancing while this ran — `6f74db0e`,
`9405a6eb`, and finally **`d7e9b32ecfed052c168ce1ef1b3595fcedb4bf3b`** under the fenced
comparator. All three give the same result, so the verdict is not a snapshot artefact — but it
is still a claim about a named tip, and the tip is `d7e9b32e`.

| run | specimen | verdict | what differed |
|---|---|---|---|
| stable vs stable | split · alias · fanout | **PARITY · PARITY · PARITY** | nothing outside the declared list |
| stable vs candidate, **as shipped** | split | **DIVERGENCE** | `[:undo_command 4]` — the undo artifact moved from `/var/tmp/forge/…` to `/home/forge/.local/state/clj-surgeon/artifacts/…` |
| stable vs candidate, **as shipped** | alias | **DIVERGENCE** | `[:telemetry :ledger]` and the same path inside the rendered text block |
| stable vs candidate, **as shipped** | fanout | **PARITY** | — |
| stable vs candidate, **artifact root pinned** | split · alias · fanout | **PARITY · PARITY · PARITY** | nothing |
| *(re-run at tips `9405a6eb` and `d7e9b32e`, the latter under the fenced comparator)* | | **identical outcome** | same one divergence as shipped; full parity pinned |

Every divergence is one deliberate change: `6f74db0e` replaces the hardcoded
`(def *artifact-root* "/var/tmp/forge")` with a per-user derived root
(`CLJ_SURGEON_ARTIFACT_ROOT` → `$XDG_STATE_HOME` → `$HOME/.local/state`) so a public build is
not writing into a fixed shared directory. Setting `CLJ_SURGEON_ARTIFACT_ROOT=/var/tmp/forge`
makes the two builds agree on that input, and then **599 + 352 + 3,789 file hashes, every
receipt field, both exit codes and both refusal texts match byte for byte.**

That is the useful shape of the answer: not "it passed", but *the only thing that moved is the
thing we moved, and here is the run that isolates it.*

## Specimens

| specimen | operation | fixture base | request | files hashed | wall per side |
|---|---|---|---|---|---|
| **split** | `namespace_split` (exact Cell C contract, 20 destinations, `promote-required` / `delete` / roots `[src test]` / profile `split-unit`) | `curtaincall-cfp` **d9205abc** | `/var/tmp/forge/plan2/cellC/D8-request.edn`, verbatim, only `workspace_root` rebound | 599 | **41–45 s** (includes the cold `split-unit` profile: kaocha + oracle) |
| **alias** | `alias_migration` (row-2 entrance, mjson centralisation) | row-2 entrance seed **31ff5b9d** | RECONSTRUCTED — see below | 352 | **4 s** |
| **fanout** | `apply_clojure_changes` (pair-1 task A, 3 sites) | `curtaincall-cfp` **92a7ca14** | `clj-surgeon-records/…/2026-09-07-pair-1/A-T1/request-4.json`, verbatim | 3,789 | **0–3 s** (in-call 226–263 ms) |

All six retained split request files (`M3..M8`, `D1..D8`) are byte-identical once `workspace_root`
is removed, so which one the harness uses is not a choice.

Neither build is ever the installed launcher. Each is exported to its own prefix and driven as
`bb --classpath <build>/src -m clj-surgeon.core`. Proof that this is the same executor
`make prepare-cli-package` ships: the exported tree of `59d8bc0c` hashes to
`291939f5f762fcb8…`, which is the name of the package already sitting in
`~/.local/share/clj-surgeon/versions/59d8bc0c…/cli-291939f5…`.

`alias_migration` and `apply_clojure_changes` have **no CLI op** (`:op :alias-migration` answers
`{:error "Unknown op"}`), so for those the harness starts one MCP server per build on a private
loopback port — never 7888/7890/7894/7895, never 8300–8339.

## What is compared

Four things, per specimen, per build:

1. **the resulting tree bytes** — a sha256 manifest of every file in the fixture;
2. **the receipt** — the published object and the receipt file on disk, field by field, including
   the rendered human-readable text block;
3. **the exit code**;
4. **the refusal / stderr text**.

## The normalisation list

`bin/parity/volatile-fields.edn` — **14 fields, 3 tree-excludes, every one carrying its physical
reason and a machine-checked reference to the observation that produced it.** Anything not on it
must be byte-identical, and a rule that cannot name its observation refuses the run.

The list was not written from imagination. **The stable-vs-stable control was run FIRST and it
failed**: eleven fields differed between two runs of the identical executor. Each was then
declared with a reason or kept. Later runs added four more (the MCP verbs' telemetry row id and
wall clock, and the elapsed time and receipt path as *rendered* into the text block).

Then the list was **trimmed**. The first draft declared `:started_at`, `:finished_at`,
`:timestamp`, `:run_id`, `:ms`, `:commit`, `:commit_sha` and `:head` with the evidence line
"differs stable-vs-stable" — a line I had written without checking. None of those fields exists
in any of the three receipts. They were removed and the comparator was replayed over all sixteen
stored specimen runs; every verdict was unchanged. The replay put exactly one back with real
evidence: `:duration_ms`, the wall clock of a single check inside a verification profile.

Two entries are worth reading even if nothing else is:

- **`:receipt_hash` is NOT a parity signal.** It hashes the published receipt document, which
  contains `:receipt_id` (a fresh UUID) and an absolute receipt path — volatile by construction.
  Anyone treating a matching `receipt_hash` as proof that two runs agreed is reading noise. The
  signals that ARE stable — `:map_hash`, `:snapshot_hash`, `:candidate_hash`, every count, every
  promotion, every check name/command/exit/status — are deliberately absent from the list and
  agreed byte for byte.
- **`00SERVER-LOGS.txt` is excluded** because `bin/kaocha unit`, a command in the `split-unit`
  profile, appends to the application's own runtime log. `git diff --numstat` reported 2515/0 and
  2517/0 on the two sides — additions only, zero deletions, so the tracked bytes are untouched.

## Red first — three planted controls, and one real defect they caught

| control | what was planted | expected | got |
|---|---|---|---|
| **RED0/GREEN** stable vs stable | nothing | identical | **PARITY** on all three specimens |
| **RED1** stable vs planted CLI receipt field | **one byte** in a published receipt string (`…committed snapshot` → `…committed snapshoT`) | stop, name the field | **DIVERGENCE 1** — `[:verification_complete_definition]: A="…snapshot" B="…snapshoT"` |
| **RED2** stable vs reordered `:require` | two adjacent require lines swapped — behaviour-neutral | still identical | **PARITY** — the harness measures behaviour, not source bytes |
| **RED3** stable vs planted MCP receipt field | one byte in `alias_migration`'s `:details_retention` | stop, name the field | **DIVERGENCE 2** — `[:structured :details_retention]: "best-effort" vs "best-effortX"`, plus its second face in the rendered text block |

**RED3 is why red-first exists.** Its first attempt reported **PARITY** — for a build whose
planted defect had never executed. The previous run's `kill` reached the `clojure` wrapper shell
but not its java grandchild, so two servers *from the stable build* survived on 7951/7952 and
answered both sides of a stable-vs-planted comparison. The tell was in the log: `/healthz`
answered in **1 s** where a cold start takes **9–13 s**.

A health check proves something is listening. It does not prove it is *your build*. The harness
now (a) refuses a port that is already listening, loudly, saying why; (b) after the server
answers, proves a java process exists whose **working directory is this build** and whose argv
names this port, and records that pid; (c) kills the java process, waits for the port to go
quiet, and prints `LEAKED` if it does not — so a leak is reported by the run that caused it
instead of being inherited by the next one. Every MCP line in every run since reads
`cwd proved = <build>`.

## The fence review, and what it cost

Sol reviewed `d1343a87` and returned **NO-GO** with two controls that produced a false `PARITY`.
Both were real, and both are the same disease this harness exists to treat — a verifier that
could not see its own subject.

**PARITY-FENCE-001 (HIGH) — the declaration could be widened silently.** The comparator consumed
every rule without checking it carried anything. Sol added
`{:key :unobserved_guard :match :key :to "<UNOBSERVED>"}` with no `:reason` and no `:evidence`,
and the comparator honoured it and hid a real difference. **A normalisation is permission for the
candidate to change a field unnoticed, so it now has to be paid for:**

- the declaration is validated **before anything is compared**. Every rule — field rules and tree
  exclusions alike — needs a non-empty `:reason` and an `:evidence` map naming
  `{:run, :specimen, :path}`. A rule missing either **refuses the whole run and names the rule**.
  `REFUSED` is a third verdict, exit 2, and is never reported as parity;
- `:evidence` must resolve against `bin/parity/observed-volatility.edn`, which is **generated, not
  written**. `bin/parity/observe-volatility.clj` compares the two captured sides of a
  stable-vs-stable run with **no normalisation at all**, and the fixture trees with **no
  exclusions**, and emits every path that differed between two runs of one build (51 observations).
  A rule may exist because a run showed the field differing, and for no other reason.

Running that generator immediately paid for the exclusions Sol flagged as unevidenced — and
**killed three of the six**. `.cpcache/**`, `.clj-surgeon/**` and `**/.nrepl-port` never differ;
they are gone, and those files are now hashed like every other file. `.git/**`,
`.clj-surgeon.edn` and `00SERVER-LOGS.txt` kept theirs, each naming the exact tree path the
observation run saw diverge.

**PARITY-FENCE-002 (CRITICAL) — a receipt missing on one side was skipped, not compared.** The
on-disk comparison was guarded by `(and exists-A exists-B)`, and because `.clj-surgeon/**` was
also excluded from the tree manifest, nothing else saw the missing artifact. **Presence is now
part of parity, for every compared artifact:**

- captured on one side, missing on the other → **DIVERGENCE** naming it;
- missing on **both** → PARITY only where the specimen declares `:expects-receipt false` in
  `bin/parity/specimens.edn`. A specimen with no entry **refuses**: absence can only be accepted
  where it was predicted;
- a published `receipt_path` naming a file nobody captured → reported;
- an artifact the harness always writes, missing on both sides, is a **capture failure** and
  refuses rather than passing.

`bin/parity/self-test` carries **ten witnesses, 10/10 passing**, including Sol's two controls
verbatim: inject the undocumented rule, and move build B's `receipt.edn` aside. Each fails first
if its fence is removed.

Sol's other five requested checks passed unchanged: `:map_hash`/`:snapshot_hash`/`:candidate_hash`
are on no normalisation rule and a one-character plant in `:candidate_hash` returns
`DIVERGENCE 1`; the occupied-port guard refuses and records `server-unavailable`; build isolation
holds (`/proc` cwd, private port in argv, no reference to the installed launcher or 7906); the
port policy refuses 7888/7890/7894/7895 and all of 8300–8339.

### Round 2 — the fence found two more, and they were the same disease one level down

**PARITY-FENCE-003 (CRITICAL) — provenance was not bound to a run.** Round 1 proved only that a
rule's `{:run :specimen :path}` triple *occurred* in the checked-in evidence file. Sol added a row
for `RUN-THAT-NEVER-HAPPENED`, cited it from a new `:candidate_hash` rule, planted a differing
candidate hash — and got `PARITY`. The widening alone suppressed a real content-hash difference.

The lesson sits one level below round 1's: **a checked-in row asserting its own provenance is an
assertion, not evidence.** Round 1 made the declaration answerable to an observation; round 2
makes the observation answerable to bytes.

- `observe-volatility.clj` now **retains what it compared** — the captured receipt objects and the
  unexcluded tree manifests — under `bin/parity/evidence/<run>/<specimen>/`, with a `capture.edn`
  naming the stable build sha, the generator's own source digest, a sha256 per retained file, and
  a digest over that set (776 KB checked in).
- `observed-volatility.edn` carries the stable build, the generator's source digest, and a digest
  over its own rows.
- `compare.clj`, before honouring any rule: recomputes every file digest, the capture digest and
  the evidence file's own digest; checks that the generator in this tree is the one that produced
  the file; and **re-derives the difference from the retained bytes**, requiring the cited path to
  actually appear in it. A rule citing a run with no retained capture **refuses, naming the run**.

Sol's forgery now refuses *even when the forger repairs the file digest* — because there are no
bytes behind `RUN-THAT-NEVER-HAPPENED`. That is the witness.

**PARITY-FENCE-004 (HIGH) — `:expects-receipt false` was one-way and could lie.** It was consulted
only when the receipt was absent on both sides, so a specimen declaring `false` while both sides
published receipts had them silently compared. The declaration is now a **two-way contract**:
expected present and absent → DIVERGENCE; expected **absent and present** → DIVERGENCE naming the
declaration; undeclared → refuses in both directions. The `alias` and `fanout` declarations were
re-verified against a real run — neither operation publishes a top-level `receipt_path`, so
`:expects-receipt false` is correct for both, and `split` publishes one.

That finding also exposed a circular check: `parity-run` wrote `receipt-path.txt` only when the
named file existed, so its published-path witness could never see a path that named nothing. It
now records the published path **whenever one is published**.

**`bin/parity/self-test`: 15 witnesses, 15 passing**, including all four of Sol's controls
verbatim, plus a tampered retained capture and a hand-edited evidence file.

**Every verdict in this report was re-measured under the round-2 fences** — stable-vs-stable
PARITY on split, alias and fanout; planted CLI receipt byte DIVERGENCE naming
`[:verification_complete_definition]`; planted MCP receipt byte DIVERGENCE naming
`[:structured :details_retention]`; behaviour-neutral require reorder still PARITY; the candidate
with its artifact root pinned still PARITY on all three.

## A second, unplanned result: the specimens reproduce across days and builds

The fan-out specimen run today on `59d8bc0c` returned
`canonical_effect_identity.sha256 = 32b1b83cf16347842f735976056051921addd65879046e54fb5b6b5a7913de3e`
and three `read_back_hashes` — **all four byte-identical to the retained
`2026-09-07-pair-1/A-T1/response-4.json`**, produced three days earlier by trunk `2d1a3c98` on a
different filesystem path.

The alias specimen, whose request had to be reconstructed, returned **9 files · 21 sites ·
3 collisions · `{"mjson" 9}` · `string_mentions 0`** — the retained row-2 entrance figures
exactly, which is independent evidence that the reconstruction is faithful.

So the retained records are usable as a **golden reference**, not merely as an A/B fixture: a
future candidate can be checked against 2026-09-07's bytes without re-running stable at all.

## Walls and footprint

| run | wall |
|---|---|
| split specimen, one side (incl. cold `split-unit` profile) | 40–45 s |
| alias specimen, one side | 4 s (in-call 3.8–3.9 s) |
| fanout specimen, one side | < 1–3 s (in-call 226–263 ms) |
| MCP server cold start, per build | 9–13 s |
| **whole three-specimen, two-build run** | **2 min 30 s – 3 min 33 s** |
| whole one-specimen split run | 1 min 27 s |

**Peak disk footprint: 2,268 MB**, measured on a full three-specimen candidate run. Live fixtures
at exit: 4 KB. The two built source prefixes are reused across specimens and runs and cost
354 MB together; captured evidence for all sixteen specimen runs is 7.7 MB.

That number used to be 16 GB. The fixtures are the whole footprint — one `curtaincall-cfp` copy
at `92a7ca14` is 1.9 GB and a three-specimen two-build run makes six — while the evidence the
comparator actually reads is kilobytes. Each fixture is now deleted the moment its manifest,
receipt, stdout/stderr and exit code are captured; at most one is live at a time; each run prints
its own footprint. `PARITY_KEEP_FIXTURES=1` keeps them for the case that needs the bytes by hand.
15.6 GB was reclaimed under `/var/tmp/forge/parity` and nothing outside it was touched.

## What could not be retrieved

- **The alias migration's wire payload was never persisted.** `bin/alias-migrate` composes the
  request in-process; only its effects are on disk. What survives is the verbatim invocation in
  the cell transcripts — `make alias-migrate FROM=clojure.data.json/write-str
  TO=marvin-voice-remote.json/write ALIASES=json,mjson,m-json,json-policy SCOPE=src
  EXPECT_FILES=9` — and the `request = {…}` literal in the entrance script. The harness sends the
  request those two compose. **It is RECONSTRUCTED, and the reconstruction is corroborated** by
  reproducing the retained 9/21/3/mjson-9 receipt exactly.
- **The alias fixture seed `31ff5b9d` exists in exactly one place: `/var/tmp/forge/row2/seed-repo`.**
  It is not in `marvin-voice-remote` and it is not on `nrepl/test-alias`. A `/var/tmp` directory is
  the sole holder of a retained specimen base — that is a retention hole, not a fact about this run.
- **The fan-out specimen's native precondition edits did not survive** (the helper namespace and
  the three `:as ident` requires; the run worktrees are deleted). The frozen request itself is
  verbatim, and it commits on the bare fixture, so the specimen is faithful to the recorded call
  even though the surrounding hand edits are gone.
- **`/home/forge/src/clj-surgeon-pubcand` was never opened.** It did not exist when this run
  started; the candidate was read as `git archive 6f74db0e` from the shared object store, and the
  only thing asked of that worktree was its log.
- **The candidate branch moved under the run.** Its tip was `6f74db0e` when the first comparison
  was made and `9405a6eb` twenty minutes later. That is not a defect, but it is the reason every
  verdict here names a commit rather than a branch: a parity claim about "the candidate" with no
  sha in it expires silently.

## Files

| what | where |
|---|---|
| harness | `/home/forge/src/clj-surgeon-parity/bin/parity-run` |
| comparator | `…/bin/parity/compare.clj` (replays over stored snapshots; no re-execution) |
| normalisation declaration | `…/bin/parity/volatile-fields.edn` |
| generated evidence for every rule | `…/bin/parity/observed-volatility.edn` (+ its generator `observe-volatility.clj`) |
| the retained bytes that evidence was derived from | `…/bin/parity/evidence/OBS2-provenance/<specimen>/{A,B}/` + `capture.edn` (776 KB) |
| per-specimen presence expectations | `…/bin/parity/specimens.edn` |
| witnesses for all four fences (15/15) | `…/bin/parity/self-test` |
| MCP client | `…/bin/parity/mcp-call.py` |
| run evidence (16 specimen runs) | `/var/tmp/forge/parity/runs/<run-id>/<specimen>/{A,B}/` |
| recovered specimen provenance | `/var/tmp/forge/plan2/cellC/recovered/specimens.md` |

Reproduce the verdict:

```
cd /home/forge/src/clj-surgeon-parity
CLJ_SURGEON_ARTIFACT_ROOT=/var/tmp/forge ./bin/parity-run \
  sha:stable/2026-09-10 sha:fable/public-candidate --specimens split,alias,fanout
./bin/parity/self-test   # the two fences, ten witnesses
```
