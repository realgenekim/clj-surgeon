# E-CALLER — the second caller (2026-09-04, forge@anvil)

**The flank.** Every character in this programme so far was **gpt-5.6-sol writing
`apply_patch`**. E-HARNESS-2 then found that with **Bash as the only write path the same
caller beats the tool (0.68×)**, which restricted the standing sentence to *"this caller on
the apply_patch harness, N ≤ 21"*. That restriction has two candidate causes E-HARNESS-2
cannot separate: the tool's win may be a property of **this caller**, or of **a write verb
that demands literal context**. This cohort asks the same question of a **second caller**,
`claude -p` on the seat's subscription, whose native write verb is `Edit` — old_string plus
new_string, no literal hunk.

**No cross-caller ratio is quoted anywhere below.** Each caller is divided only by its own
contemporaneous T. Pre-registration frozen before arm 1:
`/home/forge/tmp/arms/ecaller/preregistration.md`, manifest `FROZEN.sha256`.

## 1. Per-arm table

Fixture `fanout-k1`, N = 21, k = 1, base `65fe39a9`. Interleaved N2 T2 N2 T2 N2 T2, serial,
under `flock /home/forge/tmp/arms/arm.lock`. Server on EXPLICIT port 8020 from this cohort's
own clone of `33a8236`, started and stopped by the runner. Wall and load are **descriptive
only**; a wall is daggered when load > 8 at either end (none were).

| cell | run | model | emitted chars | chars/s | gap s | Edit calls | Write calls | bash-write | via_verb | strategy | gate | wall s | load →   |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| N2 (Claude native) | 1 | claude-sonnet-5 | **752** | 255.5 | 2.943 | 0 | 0 | 1 | 0 | stream-edit | 6/6 + bytes | 51.0 | 6.47 → 5.25 |
| T2 (alias_migration) | 1 | claude-sonnet-5 | **224** | 300.3 | 0.746 | 0 | 0 | 0 | 1 | tool-call | 6/6 + bytes | 13.0 | 6.08 → 6.88 |
| N2 (Claude native) | 2 | claude-sonnet-5 | **994** | 263.5 | 3.772 | 0 | 0 | 1 | 0 | stream-edit | 6/6 + bytes | 23.0 | 6.84 → 6.32 |
| T2 (alias_migration) | 2 | claude-sonnet-5 | **708** | 251.2 | 2.819 | 0 | 0 | 0 | 3 | tool-call | 6/6 + bytes | 16.0 | 6.05 → 7.12 |
| N2 (Claude native) | 3 | claude-sonnet-5 | **819** | 255.2 | 3.209 | 0 | 0 | 1 | 0 | stream-edit | 6/6 + bytes | 41.0 | 7.76 → 6.41 |
| T2 (alias_migration) | 3 | claude-sonnet-5 | **224** | 348.9 | 0.642 | 0 | 0 | 0 | 1 | tool-call | 6/6 + bytes | 9.0 | 6.21 → 5.92 |

### Cell means

| cell | n | mean chars | median | per-arm chars | strategy classes | gate |
|---|---|---|---|---|---|---|
| N2 (Claude native) | 3 | **855** | 819 | 752 / 994 / 819 | stream-edit×3 | 3/3 green |
| T2 (alias_migration) | 3 | **385** | 224 | 224 / 708 / 224 | tool-call×3 | 3/3 green |

**N2 / T2 = 2.22× on means, 3.66× on medians** — within-caller, this caller's own T.
Pairwise, run k over run k: **3.36× · 1.40× · 3.66×**; below 2.0× in **1/3** pairings,
below 1.5× in **1/3**.

Secondary envelope characters (the whole emitted argument object, never the primary
number): N2 `[862, 1145, 927]`, T2 `[224, 708, 224]` — the ordering is unchanged.

Integrity columns: **T2 `native_apply_patch` = 0/3** (no silent fallback), **T2 MCP server
`connected` 3/3**, **6/6 walls non-daggered**, churn `[84, 84]` on every arm — the canonical
band.

## 2. The headline, and the premise that was falsified

**The second caller never used `Edit`. Not once, in any arm.** Given free choice over 21
files, all three native arms did the same thing: enumerate the 21 paths into a shell
variable and loop a stream editor over them. N2-1 used `perl -pi`; N2-2 and N2-3 used
`sed -i`. `Edit` calls: **0/3**. `stream-edit`: **3/3**.

So the design's premise — *"a Claude caller's native write verb is Edit, minimal context, no
literal hunks"* — is **false at k=1 fan-out**. It is true of the verb and false of the
caller's behaviour: given a fan-out and permission to script, this caller scripts.

That makes the result *stronger*, not weaker, for square 2. The second caller's free-choice
native route is already near-minimal — it emits **855 characters on average, not literal
hunks** — and the tool still wins within-caller by **2.22× on means and 3.66× on medians**,
with 6/6 correctness on both sides.

**Where the characters actually go.** In every native arm the write payload is dominated by
the *file list*, not the edit logic. N2-1's single 752-character call is a 21-path `FILES=`
assignment plus a four-line loop; the `perl` substitution itself is under 130 characters.
The tool's 224-character call names **no file at all** — it states the intent and lets the
server discover the 21 namespaces and 63 sites. That is the mechanism of the win at fan-out,
and it is a property of *who enumerates*, not of how literal the write verb is.

**A fact worth putting beside E-HARNESS-2's, as two separate within-caller facts.** This
cohort's T-side emission is a bare JSON arguments object: **224 characters, one call**.
E-HARNESS-2's T-side emission of the *same verb* is a code-mode JavaScript wrapper —
`const receipt = await tools.mcp__clj_surgeon__alias_migration({ op: "alias_migration", … })`
— **477–624 characters, two calls per arm**, published as 1,240 and 980. Each is that
caller's own T and no ratio is taken across them. But it means the T side of this
programme's ratio has been carrying a harness serialisation cost that is not the tool's,
and that cost differs by roughly a factor of four between the two harnesses measured so far.

## 3. Both seats' predictions, scored

**Opus** (brainfleet §29 #1), verbatim: *second-caller native mean 3,800 chars (1,500–9,000,
60%); its T 1,100 ± 500 (70%); ratio 3.5× (1.3–8×, 55%); P(ratio < 2.0×) = 30%; ≥ 2/3 arms
minimal-edit (65%); stream-edit 0/3 (80%); correctness 3/3 (70%).*

| Opus prediction | outcome | verdict |
|---|---|---|
| native mean 3,800 (1,500–9,000) | **855** | **MISS** — 1.75× below the interval floor |
| T 1,100 ± 500 → [600, 1600] | **385** mean, 224 median | **MISS** — below the interval |
| ratio 3.5× (1.3–8×) | 2.22× mean, **3.66×** median | **HIT** — both inside; the point estimate is within 5% of the median |
| P(ratio < 2.0×) = 30% | cohort ratio 2.22× / 3.66×, not below 2.0 | did not occur (correctly the minority call); 1/3 *pairings* did |
| ≥ 2/3 arms minimal-edit (65%) | **0/3** | **MISS** |
| stream-edit 0/3 (80%) | **3/3** | **MISS**, maximally |
| correctness 3/3 (70%) | **3/3** | **HIT** |

**Sol** (brainfleet §30 #3), verbatim: *native median 6,000, T median 1,100, ratio ≥ 3×
(60%), 6/6 correct.*

| Sol prediction | outcome | verdict |
|---|---|---|
| native median 6,000 | **819** | **MISS** — 7.3× high |
| T median 1,100 | **224** | **MISS** — 4.9× high |
| ratio ≥ 3× (60%) | **3.66× on medians** (Sol stated medians); 2.22× on means | **HIT** on the reading Sol stated; would be a miss on means — both are reported |
| 6/6 correct | **6/6** | **HIT** |

**The pattern across both seats: the LEVEL was badly missed, the RATIO was predicted well.**
Both overestimated absolute character volume on *both* sides — native by 4.4× (Opus) and
7.3× (Sol), T by 2.9× and 4.9× — yet both landed the ratio, which is the quantity square 2
actually rests on. Absolute emission is a harness property and is not transferable between
callers; the ratio has now survived two harnesses.

## 4. Both withdrawal sets, applied

**Opus, verbatim:** *ratio < 2.0× in ≥ 2/3 pairings → square 2 restricted to "callers whose
write verb demands literal context"; second caller's correctness < 2/3 while T 3/3 → a new
finding reported separately, not folded into square 2.*

- Pairings below 2.0×: **1/3** (run 2, 1.40×). Threshold is ≥ 2/3. **NOT triggered.**
- Second caller's correctness: **3/3**, not < 2/3. **NOT triggered.**

**Sol, verbatim:** *T not 3/3 correct → no comparison; both correct and native/T < 1.5× →
withdraw any cross-caller square-2 claim and label the win gpt-5.6-sol-specific.*

- T2 correctness **3/3**. **NOT triggered** — the comparison stands.
- Both correct, and native/T = **2.22× (means) / 3.66× (medians)**, both ≥ 1.5×.
  **NOT triggered.**

**No withdrawal condition fires. The square-2 win survives a second caller**, on that
caller's own contemporaneous T, at N = 21, k = 1 — with the standing sentence now
restricted by the *premise* failure in §2 rather than by any pre-registered trigger:
this cohort did not test a caller emitting `Edit`, because no arm emitted one.

## 5. Losses worth reporting

**1. `alias_migration` with `verify: "fast"` refused twice on a clean fixture, and its remedy
named a retry that could not work.** T2 run 2 sent the same intent as runs 1 and 3 plus
`"verify": "fast"`, and got:

```
alias_migration
  refused · invalid-diagnostic-output · 316.91 ms
✓ source unchanged
→ Verification baseline capture failed before the alias migration
facts · files=21 · sites=63
remedy · Re-send the same alias_migration request; the frozen snapshot is recomputed
         from current source.
```

The caller obeyed the remedy verbatim and got the identical refusal a second time
(246.53 ms). It succeeded on the third call only by **dropping `verify`** — something the
remedy never suggested. The refusal itself was well behaved (typed, source unchanged, facts
attached), and it is the sole reason run 2's pairwise ratio is 1.40× instead of ~3.5×: the
two dead calls cost 484 of that arm's 708 characters. **A remedy string that prescribes an
action which reproduces the same refusal is a defect in the receipt, not in the caller** —
and it is exactly the kind of thing a `next_call` is supposed to prevent. Worth a bead
against `verify: "fast"` baseline capture on a fresh clone.

**2. A scorer defect, found by this cohort's hand-drive before arm 1.** `RE_ECHOW`'s
redirect half carried **none** of the guards `RE_REDIR` has, so a *file-descriptor* redirect
satisfied it: the read-only command `… && cat probe.clj 2>/dev/null && echo ---` scored as an
**86-character literal-patch WRITE**. `2>/dev/null` and `2>&1` are constant in ordinary read
commands, so on any caller that writes defensive shell this inflates the primary meter — the
mirror of E-HARNESS-2's M-2 inflation, one level deeper. Fixed by reusing `RE_REDIR`'s body.
**E-HARNESS-2's published numbers are unaffected**: all ten of its arms reproduce
byte-for-byte under the fixed scorers (validation 9), so the defect was latent there, not
realised. Reported anyway, because the next cohort would have realised it.

## 6. Validations before arm 1 (all green; failure would have stopped the cohort)

1. **Subscription, at the meter.** `init` reports `apiKeySource: "none"`; the stream carries
   a `rate_limit_event` with `seven_day` and `five_hour` unified windows — the subscription
   signal. No `ANTHROPIC_API_KEY`/`ANTHROPIC_AUTH_TOKEN` in this seat's environment.
   Headroom at pre-registration: **42% remaining on the five-hour window, 73% remaining on
   the seven-day**, both shared with this seat's other lanes.
2. **Fixture identity** `65fe39a9071083f478ed091ab64ebdf05c02abbd`. Matches.
3. **Dead probe.** Same command minus `--permission-mode bypassPermissions`:
   `system/permission_denied` on `Edit`, target file sha256 **unchanged** before and after.
   The flag is load-bearing and proven so.
4. **The bypass-mode nudge is absent from `claude -p`.** Bypass mode in an interactive host
   injects a "do your work through the Bash tool wherever it can" instruction, which would
   bias this caller away from `Edit` — the exact variable under test. Probed under both
   `bypassPermissions` and `acceptEdits --allowedTools`: both answered `NONE`. (A model
   self-report, not a meter; recorded as such — and see the caveat.)
5. **Dead-port negative control for T2**: server absent → `mcp_servers` status `failed`,
   **zero** surgeon tools in the tool list, **zero** MCP calls, file unchanged.
6. **Live-MCP positive control**: status `connected`, five `mcp__clj-surgeon__*` tools
   exposed, the caller invoked `alias_migration` in one call, tree correct.
7. **One hand-driven `alias_migration` reaches canonical byte-identically**: 21 files, 63
   sites, `{"store2" 21}`, 0 collisions; `diff -r` empty.
8. **Adapter + scorer hand-driven: 18 cases, 0 failures** — 11 positives covering every
   counted form, 7 negatives. Bash character expectations are derived from the case input
   *by rule*, after a hand-counted 76-vs-77 failed on the first run.
9. **Regression on real transcripts**: all ten E-HARNESS-2 arms reproduce byte-for-byte
   under this cohort's `payload.py`, `strategy.py`, `secondaries.py`. The scorers refuse to
   write outside this root, so the comparison is read-only.
10. **Prompt byte-identity outside §5**, verified programmatically.
11. **Shim hand-driven end to end**: cwd is the worktree, tool list exactly
    `[Bash, Edit, Read, Write]`, the Edit lands, rc 0.

## 7. Deviations

1. **The `RE_ECHOW` guard fix** (§5.2), declared before arm 1.
2. **A `claude` shim, first on PATH for the arm only** — the E-HARNESS-2 cell-B precedent;
   the shared `claude` is never modified. The pinned `run-arm.sh` claude branch supplies
   **no prompt** (watch.py runs the driver with stdin=DEVNULL and the command line carries
   no prompt positional; a trailing positional does not work either — commander binds the
   prompt to `-p`) and **no cwd** (`--add-dir` grants access, not a working directory, and
   `bin/fan-test` and `src/` are relative). The shim supplies both, adds the permission mode,
   and removes every non-native tool this seat's `claude -p` lists. **`Task` is the one that
   matters**: a subagent's writes never appear in this rollout, which would be a silent zero
   on the primary meter. Applied identically to N2 and T2.
3. **Model `sonnet`**, resolved id `claude-sonnet-5`, recorded per arm from the stream's own
   `init` record. A choice was required: `run-arm.sh`'s claude branch always passes
   `--model` and defaults it to the literal string `"unverified"`. The seat's `settings.json`
   default is `claude-fable-5-1[1m]`.
4. **§4 of the prompt still says "the apply_patch result is your verification of the edit"**,
   naming a tool this caller does not have. Kept **verbatim** because sections 1–4 are the
   frozen task text shared with E-AFFORD and E-HARNESS-2, and because it applies identically
   to N2 and T2, which is the only comparison quoted. Carried into the caveat.
5. **Gap timestamps come from `watch.jsonl`.** `claude -p` stream-json records carry no
   timestamp, so payload.py's codex rule has nothing to read. The same rule is applied to
   watch.py's stamps: a call's gap is its assistant record's arrival minus the
   `ms_since_start` of the immediately preceding watch record. Because a completed tool call
   is itself such a record, tool execution time is excluded. Two writes in one assistant
   record share one generation: the gap goes to the first and 0 to the rest. Pairing failure
   prints **null**, never 0.

## 8. One line of learning

**The tool's win at fan-out is not about how much literal context the native write verb
demands — it is about who enumerates the files.** A second caller, free to choose, skipped
its minimal-context `Edit` verb entirely and wrote one scripted stream edit whose characters
are almost all a list of 21 paths; the tool's call names no file at all. The write verb was
the wrong variable, and both seats' ratio predictions survived while both seats' level
predictions failed by 3–7×.

## 9. One caveat

**No arm of this cohort emitted an `Edit`, so the question it was built to answer — does the
tool still win against a caller whose write verb needs no literal context — was not put to
that verb.** It was put to a caller that *had* the verb and declined it, three times out of
three. Everything above is therefore a statement about free-choice behaviour at N = 21,
k = 1, on one fixture, one model (`claude-sonnet-5`), n = 3 per cell, with one of the three
T arms degraded by a tool refusal unrelated to the caller. A cell that *forces* `Edit` — the
mirror of E-HARNESS-2's cell B, coercing the native verb rather than removing it — is the
experiment that would actually retire the restriction, and it has not been run. Two smaller
reservations travel with it: validation 4's "no nudge" rests on a model's self-report rather
than a meter, and §4 of the served prompt names `apply_patch` to a caller that has no such
tool.
