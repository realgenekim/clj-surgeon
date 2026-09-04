# E-HARNESS-2 — square 2's win does NOT survive a change of write path: strip `apply_patch` and native writes the whole 21-file migration in 757 characters, BELOW the tool

*forge@anvil, 2026-09-04T02:31Z. Cohort E-HARNESS-2 is the ONE cohort carrying BOTH
harness flanks, per `docs/observations/2026-09-03-brainfleet-hills.md` §31: Opus's
E-HARNESS-MIN (§29 §3, the `bin/edit` flank) and Sol's E-HARNESS (§30 §3, the Bash-only
flank). Ten arm-runs, serial, interleaved `P B M T B M P B M T`, N = 21, k = 1,
characters primary, **no wall claim**. Pre-registration frozen before arm 1:
`/home/forge/tmp/arms/eharness2/preregistration.md`, sha256
`027553822eb4c31e269e1496718c7a37804aa6aa93d659f99bea54bb993585f0`, receipt
`receipts/prereg.sha256`, written with **zero arm directories on disk**. Instruments frozen
in `FROZEN.sha256`; the two post-hoc scorer corrections are in `receipts/POST-ARM.sha256`.
Fixtures and comparator read read-only from the E-REG root, which was not modified.*

*Context, not a design input: E-CEILING80 landed before arm 1 — at N = 80 native wrote a
generator 3/3 and emitted fewer characters than the tool in 2/3 pairs, bounding square 2 to
N ≲ 40. No number below is adjusted for it.*

---

## 1. The headline

| | |
|---|---|
| **P** (stock native, `apply_patch` present) | **8,184** chars mean — **7.37×** the tool |
| **M** (stock native **plus** a `bin/edit` verb, named with no preference) | **6,192** chars mean — **5.58×** the tool; **0 of 3 arms used `bin/edit`** |
| **B** (native with `apply_patch` **removed**, Bash the only write path) | **757** chars mean — **0.68×** the tool. **B is BELOW T.** |
| **T** (`alias_migration`) | **1,110** chars mean |

All ten arms are byte-identical to the canonical tree and pass the correctness gate.

**Sol's withdrawal condition fires.** B is 3/3 correct and B/T = 0.68× < 1.5×, so *"the
harness-general square-2 sentence is withdrawn and the win is restricted to the
`apply_patch` harness in this document."*

**Opus's second withdrawal condition also fires,** and its stated *reason* is refuted by
the cell beside it — see §5.

## 2. Per-arm table (run order = the pre-registered interleave)

| cell | run | emitted chars | chars/s | gap s | patch | bin/edit | bash-write | via_verb | strategy | gate | wall s | load start → end |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| P (apply_patch) | 1 | **8,783** | 147.7 | 59.458 | 1 | 0 | 0 | 0 | literal-patch | 6/6 + bytes | 113.0 | 4.56 → 4.95 |
| B (Bash-only) | 1 | **652** | 159.0 | 4.101 | 0 | 0 | 1 | 0 | stream-edit | 6/6 + bytes | 56.0 | 5.2 → 5.49 |
| M (bin/edit) | 1 | **817** | 172.5 | 4.736 | 1 | 0 | 0 | 0 | programmatic-generation | 6/6 + bytes ‡ | 71.0 | 5.49 → 5.23 |
| T (alias_migration) | 1 | **1,240** | 175.7 | 7.056 | 0 | 0 | 0 | 2 | tool-call | 6/6 + bytes ‡ | 45.0† | 5.23 → 10.83 |
| B (Bash-only) | 2 | **852** | 167.5 | 5.086 | 0 | 0 | 1 | 0 | stream-edit | 6/6 + bytes | 78.0† | 10.83 → 10.32 |
| M (bin/edit) | 2 | **8,977** | 141.5 | 63.450 | 1 | 0 | 0 | 0 | literal-patch | 6/6 + bytes ‡ | 139.0† | 10.32 → 6.69 |
| P (apply_patch) | 2 | **7,584** | 147.1 | 51.548 | 1 | 0 | 0 | 0 | literal-patch | 6/6 + bytes | 130.0 | 6.69 → 6.02 |
| B (Bash-only) | 3 | **767** | 157.9 | 4.857 | 0 | 0 | 1 | 0 | stream-edit | 6/6 + bytes | 66.0 | 6.02 → 7.27 |
| M (bin/edit) | 3 | **8,783** | 147.1 | 59.718 | 1 | 0 | 0 | 0 | literal-patch | 6/6 + bytes ‡ | 121.0 | 7.27 → 7.27 |
| T (alias_migration) | 2 | **980** | 161.6 | 6.066 | 0 | 0 | 0 | 2 | tool-call | 6/6 + bytes ‡ | 46.0 | 7.27 → 6.41 |

† wall daggered: load at one or both ends exceeded 8. 7 of 10 walls are undaggered.
**No wall claim is made in this cohort**; wall is recorded unconditionally and descriptively.
‡ 6/6 reached on a copy of the worktree with apparatus-owned extras removed — see
deviation 3. `diff -r worktree/src canonical-21/src` is byte-identical on **all ten** arms
with nothing removed.

### Cell means

| cell | n | mean chars | median | per-arm chars | strategy classes | gate |
|---|---|---|---|---|---|---|
| P (apply_patch) | 2 | **8,184** | 8,183 | 8,783 / 7,584 | literal-patch ×2 | 2/2 green |
| B (Bash-only) | 3 | **757** | 767 | 652 / 852 / 767 | stream-edit ×3 | 3/3 green |
| M (bin/edit) | 3 | **6,192** | 8,783 | 817 / 8,977 / 8,783 | literal-patch ×2, programmatic-generation ×1 | 3/3 green |
| T (alias_migration) | 2 | **1,110** | 1,110 | 1,240 / 980 | tool-call ×2 | 2/2 green |

**Ratios vs the contemporaneous T:** P/T **7.37×** · M/T **5.58×** · B/T **0.68×**.

Secondaries: `bin/edit` invocations **0/3** in cell M · hook denials **0/0/0** in cell B (no
B arm even attempted `apply_patch`) · `native_apply_patch` **0/2** on T arms, so neither T
arm silently fell back after the mid-programme `sol-yolo` swap · `via_verb` 2 on both T arms.

For scale: E-AFFORD's contemporaneous cells were N-weak **6,979**, N-salient **5,505**,
T **966**, ratio 7.22×. P here (8,184) replicates the N-weak cell; T here (1,110)
replicates E-AFFORD's T within 15%.

## 3. What each cell actually did

**P — the replication.** Both arms hand-transcribed a full `apply_patch` body naming all 21
files (8,783 and 7,584 chars, one patch call each). E-REG and E-AFFORD's finding holds:
given `apply_patch`, this caller types the whole tree out.

**B — one perl one-liner, three times out of three.** Every B arm read the tree with `rg`,
then wrote all 21 files in a **single** in-place `perl` invocation carrying a self-check.
B-3's write, in full, is 767 characters:

```
mapfile -t files < <(rg -l --glob '*.clj' … '\[acid\.fanout\.store[[:space:]]+:as[[:space:]]+store\]' src)
if [ "${#files[@]}" -ne 21 ]; then printf 'refusing edit: expected 21 files, got %s\n' "${#files[@]}" >&2; exit 1; fi
perl -pi -e '$req += s/acid\.fanout\.store(\s+:as\s+)store\b/acid.fanout.store2${1}store2/g;
             $use += s/\bstore\/find-event\b/store2\/fetch-event/g;
             END { die "unexpected replacement counts\n" unless $req == 21 && $use == 63; }' "${files[@]}"
```

Note what that is: a **guarded** stream edit. It refuses unless the file count is exactly 21
and dies unless the substitution counts are exactly 21 and 63 — the same expectation the
tool's `expect: {files: 21}` argument carries. All three B arms wrote such a guard
unprompted. The prompt never asked for one.

**M — the verb was named and nobody touched it.** 0 of 3 arms invoked `bin/edit`; 3 of 3
routed the write through `apply_patch` anyway. M-2 and M-3 hand-typed the full literal patch
(8,977 and 8,783 chars). M-1 generated the patch body with `awk` and fed it to `apply_patch`
(817 chars) — a third outcome neither withdrawal clause names exactly: it declined the verb
**and** declined to hand-type.

**T — unchanged.** One `alias_migration` call per arm, 21 files · 63 sites · 0 collisions,
plus a `bin/fan-test` confirmation; 980–1,240 chars.

## 4. Both seats' predictions, scored

### Opus (E-HARNESS-MIN, §29 §3)

| prediction | outcome |
|---|---|
| P(≥2/3 M arms use `bin/edit` for the majority of write chars) = **40%** | **0/3.** The 60% non-adoption branch realised. |
| conditional on non-adoption: native mean **6,500 ± 3,500** | M mean **6,192** — **inside the band** |
| conditional on non-adoption: ratio holds **4–8×** | M/T **5.58×** — **inside** |
| "0 stream-edit again: **85%**" | **0 stream-edit in the 5 arms that HAD `apply_patch`** (P and M) — correct where the affordance existed. **3 stream-edit arms across the cohort** — wrong as stated over all ten, because cell B is precisely the cell without the affordance. Both readings reported; the honest score is *right conditional on the write verb, and the condition is the experiment.* |
| "if it does not adopt, the routing is habit and **square 2 is safe against every harness**" | **REFUTED by cell B in the same cohort.** See §5. |

### Sol (E-HARNESS, §30 §3)

| prediction | actual | verdict |
|---|---|---|
| P mean **6,800**, split 2 literal / 1 generated | **8,184**, 2 literal / 0 generated (cell is n=2 by §31's design) | +20% high, direction right |
| B mean **2,200** (1,800 / 2,100 / 2,700) | **757** (652 / 852 / 767) | **2.9× too high** — B was far more compact than predicted |
| B **3/3 direct-script or stream-write**, 0 patch calls | 3/3 stream-edit, 0 patch calls | **exact** |
| T **1,000** | **1,110** | within 11% |
| P/T **6.8×** | **7.37×** | close |
| B/T **2.2×** | **0.68×** | **wrong** |
| all 9 (here 10) correct | 10/10 gate green | **exact** |
| **65%** that B/T stays above 1.5× | 0.68× | **the 65% branch lost** |
| **25%** that B erases the gap | B did more than erase it — B went **below** T | **the 25% branch won** |

Sol's structural call was right and his magnitude was wrong in the direction that matters:
he predicted the compact native representation and then under-estimated how compact.

## 5. Withdrawal conditions, applied

**Sol's, verbatim:** *"if B is 3/3 correct and B/T mean is below 1.5×, withdraw the
harness-general square-2 sentence and restrict the win to the current apply_patch harness;
if at least 2/3 B arms fail the common correctness gate, the cohort does not answer
efficiency and no character comparison is claimed; if B/T is at least 3× with B 3/3 correct,
close the harness flank for this caller."*

- B is **3/3 correct** and B/T = **0.68×**, below 1.5× → **clause 1 FIRES.**
  **The harness-general square-2 sentence is WITHDRAWN. The measured emission win is
  restricted to the `apply_patch` harness.**
- 0/3 B arms failed the gate → clause 2 does not fire; the character comparison stands.
- B/T is not ≥ 3× → clause 3 does not fire; the Bash flank is **not** closed.

**Opus's, verbatim:** *"If ≥2 of 3 arms use `bin/edit` and the ratio falls below 2.0×,
square 2's headline is restricted in the same document to 'harnesses whose write verb
demands literal context', and the 7.2× is annotated in place with the minimal-edit number
beside it. If ≥2 of 3 arms decline `bin/edit` and hand-type a patch anyway, the harness
flank is closed — habit, not affordance, not capability — and no further write-path cohort
runs."*

- 0/3 used `bin/edit` → clause 1 does not fire.
- 3/3 declined `bin/edit`; **2/3 (M-2, M-3) hand-typed a full literal patch anyway**
  → **clause 2 FIRES. The minimal-edit flank is CLOSED.** Offering this caller a
  minimal-context write verb, named beside `apply_patch` with no preference, does not move
  it. That is habit, not affordance and not capability.

**The two conditions do not conflict on ACTION — and they do conflict on INFERENCE, which
is the finding of the night.** Opus's clause carries the reasoning *"if it does not adopt,
the routing is habit and square 2 is safe against every harness, which is a stronger result
than the ratio itself."* **Cell B falsifies that inference inside the same cohort.** The
routing is habit only *while a patch verb is present*. Take the patch verb away and this
caller does not fall back on habit at all: it writes a 757-character guarded `perl` one-liner
and lands the identical tree. So:

> **Offering a better write verb does not move this caller. REMOVING the patch verb moves it
> completely — and past the tool.** Adoption and capability are different questions, and
> only the second one was ever load-bearing for square 2.

Consequently the E-HARNESS-2 doc annotates E-AFFORD's 7.22× and E-REG's 5.7–7.2× **in place**:
those figures are properties of *a caller holding `apply_patch`*, not of the task.

## 6. The cell-B apparatus finding — a result about the harness in its own right

Removing `apply_patch` took **two** removals, and discovering the second is a finding.

1. **The tool.** `gpt-5.6-sol` runs in codex **code mode**: the model emits a JS block per
   call and reaches its tools as `tools.apply_patch(…)`, `tools.exec_command({cmd:…})`. The
   model catalog carries `apply_patch_tool_type = "freeform"` for that model; cell B runs
   with `-c model_catalog_json=<root>/catalog-noap.json`, the stock catalog with that one
   field set to `null` for that one model. **Dead probe:** an `Object.keys(tools)` probe
   lists `apply_patch` on the stock catalog (`scratch/probe-ctl.log`) and does not list it
   on this one (`scratch/probe-noap.log`, 0 occurrences of the token in the entire log).
2. **The exec interception, which the tool removal does NOT close.** codex *separately*
   recognises a bare `apply_patch <<'EOF' …` inside `exec_command` and applies it internally
   with no shell. **Measured with the tool already absent:** `probe.txt` went `OLD → NEW`
   and the tool returned codex's own `Success. Updated the following files: M probe.txt`.
   A `PreToolUse` hook that denies any `tool_input` carrying the token `apply_patch` closes
   it.
3. **And the hook does not run without a CLI flag.** With the hook configured in
   `CODEX_HOME/config.toml` but **without** `--dangerously-bypass-hook-trust`, the hook
   **never fired** — no hook log was written at all — and the write landed
   (`scratch/probe-shell3.log`). *A hook config file is not a control.* Both the flag and
   the catalog override are injected by a cell-B `codex` shim placed first on `PATH`, so the
   shared `~/bin/sol-yolo` was never modified.
4. **Positive control, which matters as much as the denial.** In the full end-to-end cell-B
   path: `Object.keys(tools).filter(k => k.includes("patch"))` → `[]`; the `apply_patch`
   heredoc → blocked with the hook's reason text; `sed -i 's/OLD/NEW/'` → **allowed**, file
   written. A hook that blocked every write would have made cell B a broken harness scoring
   a null.

Receipt: `receipts/dead-probe.txt`. In the live arms the hook denied **nothing** — 0/0/0 —
so cell B's number is not inflated by a refused attempt. No B arm even tried.

**The general lesson, which is not about codex:** *removing a capability from a model's tool
list does not remove it from the agent.* A harness can expose the same capability through a
second, unlisted path, and the only way to know is to try the capability and watch the
filesystem. A tool list is a claim about the interface, not about the reachable behaviour.

## 7. Validations before arm 1

| # | validation | receipt | result |
|---|---|---|---|
| 1 | fixture byte-identical to E-AFFORD's | `receipts/fixture-identity.txt` | base `65fe39a9` = all nine E-AFFORD arms' `base.sha` |
| 2 | cell B's `apply_patch` provably absent, + positive control, + indirect route shut, + a Bash write still allowed | `receipts/dead-probe.txt` | 5 probes, all as expected (§6) |
| 3 | `bin/edit` hand-driven, 3 spellings + 3 negatives | `receipts/bin-edit-handdrive.txt` | 3 spellings + **4** negatives, all correct |
| 4 | classifier's four classes hand-driven | `receipts/classifier-handdrive.txt` | 25 cases, 0 failures (later extended — deviation 2) |
| 5 | scorer reproduces E-AFFORD's nine payload counts and labels | `receipts/scorer-replication.txt` | 9/9 exact, and again after every classifier correction |
| 6 | dead-port negative control for T, against the **swapped** `~/bin/sol-yolo` | `negative-control/` | **rc 1, zero returns, no report file, 0 tracked files changed**, 2 s, on spare port 7971 |
| 7 | one hand-driven `alias_migration` reaches canonical byte-identically | `receipts/handdrive-response.txt`, `receipts/handdrive-bytes.txt` | 21 files · 63 sites · 0 collisions · 706.72 ms; `diff -r` rc 0 |
| 8 | shared-prompt-prefix sha equal across all four prompts | `FROZEN.sha256` | `cec760bb…` ×4 |

Prompt shas: **P** `9ab5267a…` and **T** `6062621c…` are byte-identical to E-AFFORD's
`EAFFORD-Nd.md` and `EAFFORD-T.md`. **B** `1eebe977…`, **M** `3f680207…` differ from them
in §5 only.

Ports: T arms ran this cohort's own clone of build `33a8236` on **explicit** port 7970;
`COHORT_PORTS="7970 7971 7972"`. 7888, 7894, 7895, 7906–7910 and 7941–7967 were never named
and never contacted. The server was started and stopped by this apparatus; the hand-driven
validation server (also 7970) was stopped by its recorded pid and the port confirmed free.

## 8. Deviations

**1 — the cell-B apparatus finding (§6).** The brief said "find how the codex driver exposes
`apply_patch` and remove it." Removing it required a model-catalog override **plus** a
`PreToolUse` deny hook **plus** a CLI flag, because two of those three were invisible until
probed. Recorded as a finding, not merely a workaround.

**2 — the character scorer was wrong in BOTH directions, and both were found by
hand-auditing every write call.** The pre-registered definition never changed; the
implementation was corrected twice, all ten arms were re-scored under the final version, and
E-AFFORD's nine counts were re-reproduced after each correction.

- **2a, a silent ZERO (arm B-2).** B-2's tree was byte-identical to the canonical with a
  16,878-byte diff, and the scorer reported **0 emitted write chars**. Two causes stacked:
  the flag was spelled `perl -0pi -e` (a *cluster*), while v1 matched only `-i`, `-pi` and
  `-p -i`; and `perl` sat immediately after a literal `\n` **escape** inside a JSON string,
  so `n` and `p` were adjacent word characters and `\bperl\b` had no word boundary at all.
  Fix: decode `\n`/`\t` escapes before matching shell text, and replace flag-spelling
  matching with a **bounded token scan** over the flags that follow `sed`/`perl`/`awk`.
- **2b, a hang.** The intermediate regex `\b(sed|perl|awk)\b[^\n|;]{0,300}?(…)` backtracked
  catastrophically on a long normalised command and hung the scorer past 120 s. The token
  scan replaced it.
- **2c, a silent INFLATION (arm M-2).** `rg -l … | sort | tee /tmp/acid-store-files` was
  counted as a 359-char write. It is not: the bytes landing in that file are `rg`'s
  *output*, not the agent's *emission*, and the target is a scratch file list. Rule
  corrected — a redirect or `tee` counts only when the **content is emitted in the call**
  (a heredoc body, an `echo`/`printf`/`cat` literal, or a program carrying the string it
  writes). M-2 went 9,336 → 8,977; M's mean went 6,312 → 6,192; M/T 5.69× → 5.58×.
  **No verdict changed.**

  Final hand-drive: **22 cases, 0 failures**, including all three B field spellings, the M-1
  generated-patch case and the M-2 `tee` case as an explicit negative
  (`receipts/classifier-handdrive-v3.txt`). Superseded v1 kept at
  `receipts/strategy-v1-superseded.py`.

**3 — the correctness gate counts apparatus-owned files.** `rescore-FAN`'s CHECK 1 counts
*every* changed path, and two of them are not agent work: `bin/edit`, which this apparatus
installs into every M arm, and `.codex/config.toml`, which the **swapped** `~/bin/sol-yolo`
now writes into every T arm. All five affected arms failed CHECK 1 on that alone while
passing CHECKS 2–6 (form-equality 21/21, protected regions 106/106, residue/alias clean,
100 namespaces load, FAN-TEST 21 tests / 147 assertions / 0 failures). Re-running the same
gate on a **copy** with only those two paths removed — the arm's own evidence never mutated
— gives **6/6 on all five** (`gate2-apparatus-extras.sh`, `<arm>/gate2.log`). Marked ‡ in the
table. `diff -r worktree/src canonical-21/src` is byte-identical on all ten arms with
nothing removed.

**4 — cell M's fixture carries one extra file.** `bin/edit` is installed **untracked**, so
`base.sha`, `diff.patch` and the churn band are the same instruments P and B get, and it
lives outside `src/`. It is nevertheless a difference between M's tree and P's, and it is
the one CHECK 1 caught.

**5 — `~/bin/sol-yolo` was swapped between programme start and arm 1** (E-CEILING80 found it
wrote `required = true` only when a repo `.codex/config.toml` existed to neutralise, so a T
arm whose server died could silently fall back to native). Declared in the pre-registration
as a pre-arm-1 note, and validation 6 is the dead-port negative control re-run against the
swapped wrapper (sha `0babf8be…`): **rc 1, zero returns, no report file.** Both T arms
report `native_apply_patch = 0`, so no silent fallback occurred.

## 9. Second-caller feasibility (side task, no arms)

**Feasible, on the subscription, with two changes.** The exact command tried, in a scratch
directory holding a two-line `probe.clj`:

```
echo "In <dir>/probe.clj rename the var old-name to new-name using your Edit tool. Change nothing else. Then stop." \
  | claude -p --model sonnet --output-format stream-json --verbose --add-dir <dir>
```

It exited **rc 0**. Its first lines: an `init` record listing the tool set — which contains
**`Edit`**, `Write`, `Read`, `Bash` — then a `rate_limit_event` carrying
`{"rateLimitType":"seven_day","utilization":0.26, "unifiedWindows":{"five_hour":{"utilization":0.55}}}`,
which is the **subscription** rate-limit signal; no `ANTHROPIC_API_KEY` or
`ANTHROPIC_AUTH_TOKEN` is set in this seat's environment and the credential is
`~/.claude/.credentials.json`, so **no API-key path is involved** (had one been the only
route, this task's instruction was to refuse). The run then emitted an `assistant` record
with a `tool_use` block for `Read`, a `user` record with its `tool_result`, a second
`tool_use` for `Edit` — and then
`{"type":"system","subtype":"permission_denied","tool_name":"Edit", …}`: the edit did **not**
land and `probe.clj` was unchanged. **Change 1: the arm must pass a permission flag**
(`--permission-mode bypassPermissions` or equivalent) or every write is refused. **Change 2:
a rollout adapter.** `run-arm.sh` already has a `claude` driver branch and `watch.py` already
normalises `claude -p` stream-json (`type:"assistant"` → `message.content[].tool_use`) and
writes it to `<arm>/rollout.jsonl` under `--capture-stdout`, so the pinned meters work
unmodified — but **this cohort's character scorer does not**: `payload.py` keys on codex's
`payload.type == "custom_tool_call"` with a **string** `input`, whereas `claude -p` emits
`tool_use.input` as an **object**. That is roughly fifteen lines (`_args_to_text` over
`tool_use.input`, exactly as `watch.py` already does it), and it must be hand-driven on a
real `claude -p` rollout before any arm — the two silent-scorer failures in §8 are the
argument for that. No full arm was run.

## 10. One line of learning

**A capability you removed from the tool list is not a capability you removed from the
agent — and an affordance you added to the tool list is not an affordance the agent will
use.** Both halves were measured tonight in adjacent cells: `apply_patch` survived its own
deletion through an unlisted exec path until a hook shut it, and `bin/edit` was declined 3/3
after being named beside `apply_patch` with no preference.

## 11. One caveat

**Cell B is a coerced harness, and its 757-character mean is not a claim about any caller
anyone actually runs.** Zero patch calls in B is true by construction; every real caller of
this model ships `apply_patch` or `Edit`. What B licenses is exactly one negative: the
sentence *"the tool emits fewer write characters than native"* cannot be stated
harness-generally, because there exists a harness — reachable by deleting one field in a
model catalog — where the same caller, on the same fixture, to the same byte-identical tree,
emits **a third** of what the tool does. What B does **not** license is the converse product
claim: three arms at n = 3, on one task family, at one N, with a guarded one-liner that
happens to fit this migration's shape, is not evidence that a stream edit generalises to
fan-out tasks where a regex cannot express the alias policy. E-REG's k > 1 cells are where
that would be tested, and this cohort did not test them.
