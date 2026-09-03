# The scale-slope experiment (`sl1`) — pre-registered spec

*bridge seat, design only; nothing installed, no repo modified. Read with the big-aha log (fleet
rounds one and two), tech-tree `q5z`, CLAUDE.md "the math".*

## Answer to Gene's steer, first

**This is the last square with a 5–10x shape, and the honest prior is 1.5x.** Wall is the sum of
model returns. Native does unbounded *edit sites* in ONE `apply_patch` return (measured: 21 owners,
11 files, one cell), so fan-out in sites or files can never buy 5x. Only two terms can grow: files
the agent must read and cannot infer, and repair returns once one-shot generation of a big patch
stops being reliable. A tool at one call per N wins 5x only if those grow to ~5x native's fixed
cost — an empirical question with a monotone answer, costing **14 arm-runs, one evening.** Run it
once. A flat slope is not a 15% disappointment; it closes the square and everything goes to the
gate.

## How the design separates the three N's

| N | native cost | tool cost | held how |
|---|---|---|---|
| edit sites | ~0 (batched in one patch) | ~0 | fixed at 3/file in the slope family; varied alone in control C |
| files touched | ~0 (same patch) | ~0 | equals N in the slope family |
| **files that must be READ, contents not inferable** | the question | 0 (tool discovers) | equals N by construction |

## Transformation: **(a)**, var rename with per-file alias re-qualification

> `acid.fanout.store` is retired: its var `find-event` moves to `acid.fanout.store2` as
> `fetch-event`. In every namespace requiring it, rewrite the require and every qualified use.
> **Alias policy:** prefer `store2`; if bound in that file, `st2`; then `es`; then `store-2`.
> Never shadow a binding already in that file.

**Against the crux.** (b) is rejected: what to pass at each caller is a *judgment* from that
caller's scope, not a mechanical closure — the tool cannot compute it, q5z would refuse, and no
byte-exact oracle exists; it confounds *unread* with *undecidable*. (c) is rejected as primary: the
edit lives inside the `ns` form, so sites ≡ files and N's #1 and #3 cannot be separated, and it
re-runs the shape `require_change` already won on (Sol's "already-measured sweet spot"). (a) alone
makes the per-file decision a function of *that file only* (reads = N), leaves sites/file free
(site-count separable), keeps the closure mechanical (the tool can own it), and is byte-exactly
script-derivable.

## Repo: synthetic for the slope, real for the anchor

**Synthetic is more honest to N=80, and I measured why.** The best real fan-out on hand,
`curtaincall-cfp` at `d9afe8e9`, has 190 src files and 74 using `store/` — but **68 of 68 alias it
identically (`:as store`) and zero internal requires use `:refer`.** Its correct answer is one
`sed`, N_read collapses to 1, and it gives one N point. No real repo gives a controlled slope.

**Generator `gen-fanout.clj --n N --seed 7`** (pure, deterministic, committed with the cohort):

- **Repo size constant at 100 namespaces for every N**; only the *target* count varies, so the
  slope measures fan-out and never repo size.
- Each namespace 40–120 lines, 3–8 `defn`s from a 30-shape body bank, 2–6 requires with pooled
  aliases in varying clause order; a third with docstrings, a fifth with a comment inside
  `(:require …)`, a tenth with a reader conditional, a twentieth with a `#_` discard; 2/4-space
  indentation alternating. Not trivially regular by construction.
- Targets use the fan-out ns under a per-file alias from `{store, st, s, db, repo, k}`, exactly
  **3 sites each** (control C overrides).
- **Decoys in every target:** a `let` local named `find-event`, the string `"find-event"`, the
  token in a docstring, and a *second* required namespace that also exports `find-event`. A regex
  answer fails predicate 3.
- **Nested N:** the N=5 target set ⊂ 10 ⊂ 20 ⊂ 40 ⊂ 80, one seed — the slope is the same files
  growing, not five unrelated tasks.
- Emits per N: `repo-N/` (pre), `canonical-N/` (post, by applying the policy at generation time —
  the oracle is derived, never hand-written), `manifest-N.edn` (targets, alias per file, site
  coordinates, sha256 of every protected region), and a suite asserting one behaviour per site.
  Committed to `~/acid/fanout`, tagged `fanout-N`; **the runner reads the tag's sha into each
  attestation** — no hand-typed base.

**Real anchor:** cfp at `d9afe8e9137b3f99b892c14fc9ae863b2059c726`, cloned to `~/acid/cfp` (the
bridge checkout is never modified), same transformation on `cfp-scheduler-killer.store`, N=68.
Deliberately the *adversarial* anchor: the regular case where `sed` is right and native should win.
Tool wins synthetic N=80 and loses cfp N=68 → "the win requires irregularity", not a product claim.

## Points, arms, budget — **14 arm-runs**

| run | N | shape | arms | runs |
|---|---|---|---|---|
| sl1-a | 5, 10, 20, 40, 80 | synthetic, 3 sites/file | N native, T tool | 10 |
| sl1-c | files 5, **48 sites/file** (240 sites, 5 reads) | synthetic site control | N, T | 2 |
| sl1-r | 68 | cfp real, uniform alias | N, T | 2 |

n=1 per point: the readout is the **slope**, identifiable at n=1 (Opus, round two). Levels at a
single N are not claimable — the floor is 172 s. Two runs of headroom for one re-run.

## What the tool needs — **q5z must be built first**

The shipped verbs are disqualified in their own words. `symbol_migration` takes `files` as
`[file, [[owner, from, matches] …]]` and documents: *"Owners, old symbols, and positive counts are
authority, not discovery."* Its payload is **O(N), agent-computed, with exact per-site match
counts 1..128** — it removes no read and *adds* a counting obligation; `require_change` is bound to
it by schema. With today's verbs the tool arm is guaranteed to lose at N=80 and the cohort measures
nothing. rf1 already showed the shape: 11 structural mutation attempts, 1 commit, 9%.

**Minimal q5z** — one op, payload constant in N:

```json
{"op":"alias_migration","workspace_root":"…",
 "from":{"lib":"acid.fanout.store","var":"find-event"},
 "to":{"lib":"acid.fanout.store2","var":"fetch-event",
       "alias_policy":["store2","st2","es","store-2"]},
 "scope":{"paths":["src/**"]}, "expect":{"files":80}}
```

Tool-side discovery of every requiring namespace and site; per-file alias chosen against that
file's own bindings; locals, strings, docstrings, comments, metadata and reader-conditional
branches untouched; atomic; **one receipt of length O(1) in N** (files, sites, alias histogram,
collisions, kondo delta, focused-test result) — an N-line receipt re-imports the cost we are
deleting. Refuses closed with an executable `next_call` on `expect` mismatch, indirect or
macro-mediated references, ambiguous ownership. Nothing else is in scope.

## Acceptance — `rescore-FAN.sh <worktree> <N>`, mechanical, canonical generated

1. **File set** equals the manifest's target set exactly, no extras.
2. **Form equality (THE GATE):** every changed file parses and its form tree equals the canonical's
   modulo whitespace, comments, metadata and `#_` discards present and in place.
3. **Protected regions:** sha256 of every decoy region equals the manifest's. The sed-catcher.
4. **Load:** one process requires all 100 namespaces, zero errors.
5. **Behaviour:** the generated suite at base count, base failure set empty.
6. **Residue:** `rg -c 'store/find-event|acid\.fanout\.store\b'` over `src/` is 0, and no
   introduced alias shadows an existing binding (checked against the manifest).

Reported, not gating: byte-exactness vs `canonical-N/` (churn), returns, wall, tokens, tool calls,
typed refusals, native `apply_patch` calls landing functional bytes.

## Pre-registered predictions (returns to done)

| N | native, Sol | native, Opus | tool (both) | ratio Sol | ratio Opus |
|---|---|---|---|---|---|
| 5 | 8 | 8 | 6 | 0.75 | 0.75 |
| 10 | 9 | 10 | 6 | 0.67 | 0.60 |
| 20 | 10 | 13 | 6 | 0.60 | 0.46 |
| 40 | 11 | 19 | 7 | 0.64 | 0.37 |
| 80 | 13 | 30 | 7 | 0.54 | **0.23** |

Sol: `rg` answers the reads in one or two returns at every N, native grows logarithmically, the
tool holds a flat ~1.6x that is never big game. Opus: past ~N=20 the one-shot patch stops being
reliable, misses and mis-aliases become repair returns, native turns superlinear — 4.3x at N=80.
Wall native: Sol 250→430 s across N=5→80, Opus 250→950 s; tool flat 190–240 s, q5z tool time 2–8 s
(not the term). **Control C:** both predict native ≈ its own N=5 point ± floor. **Anchor R:** both
predict native ≤ 300 s (it is a `sed`) and tool ≥ native.

## Falsifiers, and the two verdicts

**Ends the structural-editing program** (any one): native wall at N=80 within **1.3x** of N=5
(flat over a 16x fan-out); tool/native ratio not monotone decreasing (Spearman ρ ≥ 0 vs N); ratio
≥ 0.85 at every point; or q5z falling back to `apply_patch` on functional bytes at any point, or
refusing >20% of calls — an incomplete closure hands N back as repair and is rf1 again. Then the
fan-out square closes like the per-form square and all effort goes to the gate and `:ls-tree`.

**Falsifies the mechanism, not the square:** if control C rises with sites as A rises with files,
the driver is patch size, which the tool pays too; re-cut the design before any claim.

**Build q5z as the flagship if:** ratio ≤ **0.35** at N=80, monotone on ≥4 of 5 points, wall ratio
≤ **0.50** at N ≥ 40, zero native fallback, acceptance green in both arms at every N. That is ≥2x
wall at the top of the measured range with a slope projecting past 3x at N≈150 — the only path to
big game left on the board.

**Standing caveat:** this is a slope in a synthetic corpus. cfp is the one real point and is
deliberately the case native should win; a synthetic-only win is a finding about irregular fan-out,
not a product claim.

# The prompts (generated per N by `mkprompt-FAN.sh <N> <arm>`; identical except block §5)

Installed as `~/prompts/FAN-native-<N>.md` and `~/prompts/FAN-tool-<N>.md`. The two files are
byte-identical outside §5. The runner hashes both and records the hashes in each attestation.

## Shared body (both arms, every N)

```
You are working in a throwaway git worktree of a Clojure project. Do the task below and stop.

1. THE TASK

The namespace acid.fanout.store is being retired. Its var find-event has moved to
acid.fanout.store2 and been renamed fetch-event. Nothing else in the tree changes.

In EVERY namespace under src/ that requires acid.fanout.store:
  - replace that require with acid.fanout.store2,
  - rewrite every qualified use of the old var to the new namespace and the new var name,
  - choose the alias for the new require by this policy, applied to THAT FILE ALONE:
        prefer  store2
        if store2 is already bound in that file, use  st2
        if st2 is already bound in that file, use     es
        if es is already bound in that file, use      store-2
    Never introduce an alias that shadows an alias or a referred name already bound in
    that file. Different files will legitimately end up with different aliases.

There are exactly <N> such namespaces. src/ contains 100 namespaces in total; the other
(100 - <N>) must not change.

2. WHAT MUST NOT CHANGE

A local binding named find-event, the string literal "find-event", the token find-event
inside a docstring or a comment, and any use of find-event that resolves to a DIFFERENT
required namespace are all unrelated to this change and must be left byte-for-byte alone.
Comments, metadata, reader-conditional branches and #_ discards stay where they are.

3. DONE MEANS

  - every namespace under src/ loads,
  - bin/fan-test is green at its base count with an empty failure set,
  - no occurrence of acid.fanout.store or of the old var name remains in src/.

4. RITUAL

This worktree is throwaway and has no reviewer and no beads workflow: do not run bd, do not
run git status or git diff, do not re-read a file you just patched, do not hand-run
clojure -M -e syntax probes; the apply_patch result is your verification of the edit and
bin/fan-test performs the load check. Every extra command costs a full model turn.

Four specific things this environment will tempt you into, all of which are waste here:
  (i)   run the suite ONCE, with a single blocking wait; do not poll it,
  (ii)  target/ and .cpcache are generated; never clean them, never inspect them,
  (iii) there is no skill or playbook installed for this task; do not search the filesystem
        for one, and never run a find or rg rooted above this worktree,
  (iv)  report your total tool-call count on the last line as  TOOLCALLS: <n>.
```

## §5, native arm (`FAN-native-<N>.md`)

```
5. TOOLING

You have your ordinary native tools only: shell, rg, sed, and apply_patch. There is no
structural editing server available. Use whatever route you judge fastest and safe,
including a scripted edit if you believe it is correct for this tree.
```

## §5, tool arm (`FAN-tool-<N>.md`)

```
5. TOOLING

A clj-surgeon MCP server is configured. It exposes alias_migration, one call that takes the
whole intent — the old lib and var, the new lib and var, the alias policy in order, and the
number of files you expect to be affected — discovers every requiring namespace and every
qualified site itself, chooses each file's alias against that file's own bindings, and
returns one receipt: files changed, sites rewritten, the alias histogram, collisions
resolved, the kondo delta and the focused-test result.

Make that one call. Its receipt is your verification of the rewrite; do not re-read the
files it reports as changed. If it refuses, it returns an executable next_call — send that.
You still have your native tools; use them if the tool cannot complete the task.
```

## Anchor variant (`FAN-R-<arm>.md`, cfp d9afe8e9, N=68)

Same file with these substitutions, nothing else:

```
  acid.fanout.store   ->  cfp-scheduler-killer.store
  acid.fanout.store2  ->  cfp-scheduler-killer.event-store
  find-event          ->  fetch-event    (renamed from store/find-event)
  store2 / st2 / es / store-2   ->   event-store / estore / es / event-store-2
  "exactly <N> such namespaces ... 100 namespaces in total"
      ->  "68 such namespaces; src/ contains 190 files, and the rest must not change"
  bin/fan-test        ->  bin/kaocha
```

## Control variant (`FAN-C-<arm>.md`, 5 files, 48 sites each)

Same file with `<N>` = 5 and one added line in §1:

```
These five namespaces use the var heavily — expect tens of call sites in each.
```
