# E-THREAD — does a native agent false-complete a cross-language feature thread?

*Cohort run on forge@anvil, 2026-09-04. Decides the design question in
`docs/observations/2026-09-04-feature-thread-study.md` §6: should Surgeon read and relate
non-Clojure files? The study's answer was "not a verb yet — build the cheap script and MEASURE
whether native agents false-complete on hidden-leg threads." This is that measurement.
Pre-registration below written and committed at 2026-09-04T04:29:02Z, **before arm 1**.*

## Pre-registration (frozen before any arm ran)

**Amendment, received 2026-09-04T04:16:17Z, no arm had run.**
Gene, verbatim: *"Maybe look into comparable asks we had for Marvin dictation app changes. Might
have similarities. Make a change —> requires searching for JS and CLJ and maybe even prolog
files"*. Accepted in full: a second fixture repo (marvin-voice-remote, the Marvin dictation app)
with two REAL threads mined from its history rather than constructed, one of them spanning
JavaScript, Clojure AND Prolog; per-repo slot conventions moved into a config file instead of
hard-coded paths; the cohort extended from 18 arms to 30.

### The instrument

`bench/feature-thread/feature-thread.sh <repo> <seed...>` — bash + rg, no parser, no server, no
MCP. Five named slots, always rendered, each FOUND (with evidence kind and location) or ABSENT
(with the exact search that returned zero). `status` is COMPLETE only when all five are FOUND.
Slot roles are repo conventions and live in `<repo>.conf` beside the script
(deviation: `.conf` shell key=value rather than `.edn` — the script is bash and the brief
allowed "or a flag"; an EDN parse in bash would be a parser, which §5 forbids).

### The five threads and their frozen truth

Fixture shas frozen before arm 1:

- **social-media-writer** `2df99c989e2dc1963161c13f7a341847c16b4deb` (cloned read-only; nothing
  is committed to Gene's repo, and the two constructed variants live only in scratch copies).
- **marvin-voice-remote** `d170f3d5edea6faa39396ea8b3418e29b2e2b4b1`.

Truth file `frozen/truth.tsv` sha256 `4f6659153d15bbdd7a2ad8b3879e6d21e947e2f4aec6bf2223b15307e325044e`;
threads file sha256 `df6dd70eceee138bbcf87cef04df9d15887613671d691d31877385859d3afa78`.
All 25 truth legs were verified to exist at their exact file:line before arm 1 (25/25 OK).

| thread | repo | subject | hidden legs | provenance |
|---|---|---|---|---|
| **T1** plain | smw | `formatDraft` / `/api/transform/format` | 0 | REAL — the very feature in Gene's transcript (Edit > Format draft) |
| **T2** aliased JS | smw scratch copy | same | 1 (js-function) | **CONSTRUCTED** — no real JS alias exists in this repo (searched `const X = ident;`, `window.X = ident;`: 0 relevant hits) |
| **T3** assembled route | smw scratch copy | same | 1 (route) | **CONSTRUCTED** — no real templated/constant route exists in this repo (searched `(str …api…)` in route tables, JS template literals: 0 hits) |
| **T4** JS+CLJ | mvr | `ackReply` / `/api/channel/reply-ack` | 0 | REAL — commit `a67b98ac`, bead `marvin-voice-remote-21v` |
| **T5** JS+CLJ+**Prolog** | mvr | `streamAction` / `/api/code-director/control/events` | 2 (state-contract, tests-oracle) | REAL — commit `34a6d965`, intent `MVR-DIRECTOR-CONTROL-LEASE-001` |

T2 and T3 are constructed because the ablation the study asked for does not occur naturally in
social-media-writer. **T5 needed no construction at all** — Gene's amendment produced a harder
real thread than either constructed one, and it is hard for two reasons a search cannot fix:
its Prolog oracle spells the JavaScript `streamAction` as `stream_action`, and its node
witness references the JS only by *file path*, never by the identifier or the route.

Slot mapping is per repo, declared before arm 1:

- **smw**: menu-caller · js-function · route · handler · tests
- **mvr**: ui-js-caller · server-route · handler · state-contract · tests-oracle

The ethnographic source, quoted:

> **T4** — bead `marvin-voice-remote-21v`: *"Gene 2026-07-18: better countermeasure than the
> ambiguous reply-drained counter (see ejd). Instrument which replies the client actually PLAYED
> and send that back, so the agent sees which messages Gene truly heard."*
>
> **T5** — intent `MVR-DIRECTOR-CONTROL-LEASE-001`: *"While hosted hands-free capture remains
> active, when its Director command stream reaches CLOSED, the client shall discard that handle
> and reconnect with the same client identity."* Its rationale names the witnesses that a seed
> search cannot reach: *"Witnesses outside the clj suite, run by `make director-control-contract`:
> test/director_capture_control_test.mjs and test/director_control_invariants.pl."*

### Cells, prompts, meters

Cells **N** (native) and **S** (told to run the script first), n=3 per cell per thread = **30 arms**,
interleaved N/S within a thread, T1→T5. Driver `~/bin/sol-yolo` (codex, gpt-5.6-sol), no URL
argument, no MCP, no Surgeon server — the same native caller as every cohort tonight. Every arm
runs under `flock /home/forge/tmp/arms/arm.lock` on a fresh clone of its fixture.

Prompts are byte-identical except one inserted S sentence. Asserted before arm 1: each thread's
prompt is exactly PREFIX + CELL_LINE + SUFFIX, N's CELL_LINE is empty, and
`sha256(SUFFIX)=1ad2bb7aff3694f0…` is the same for all ten prompts; T1/T2/T3 additionally share
`sha256(PREFIX)=d600861ae8cfab6a…`, so the three smw threads differ only in the tree.

Meters, load-immune first:

- **LEGS_FOUND 0–5** — an arm has found a leg iff it names any accepted file for that slot. A
  strict variant (basename **and** the line ±5 or the distinctive token) is recorded alongside.
- **FALSE_COMPLETE** — binary: the arm's final `COMPLETENESS:` line says COMPLETE while
  LEGS_FOUND < 5. Both cells are required to emit that line, identically, so the grader never
  has to interpret prose.
- Secondary, descriptive only: emitted chars, tool calls, wall, load at completion.

The grader was self-tested before arm 1 on three synthetic answers: a perfect one (5 legs,
COMPLETE → FC=0), a naive one (3 legs, COMPLETE → **FC=1**), and an honest partial (3 legs,
INCOMPLETE → FC=0). It refuses any target outside the cohort root.

**Truth amendment, 2026-09-04T04:27:12Z, after a single discarded PILOT and before arm 1 of 30:** the
tests/oracle slot accepts any of a thread's several real witnesses, not one nominated file. The
pilot named the `.mjs` witness where the frozen truth had named only the `.pl` one — an
under-specified truth, not a wrong answer. The pilot run was deleted and re-run inside the cohort.
Whether an arm reached the **Prolog** file specifically is kept as a separate secondary meter.

### Predictions (verbatim from study §6, mapped onto five threads)

1. N finds 5/5 on the plain thread — **90%**. (T1, and by extension T4, the two threads with no hidden leg.)
2. N finds ≤4/5 on the aliased and templated threads — **60%**. (T2, T3, and T5.)
3. N claims completeness anyway in ≥1 of those arms — **50%**.
4. S never claims completeness on a missing leg — **80%**.
5. S finds the same legs as N.

### Withdrawal, declared before arm 1

- If **N is 5/5 on all threads and never false-completes** → the study closes as *"the skill is the
  product"*, no verb is designed.
- If **N false-completes in ≥2 of the hard-thread arms and S does not** → the completeness receipt
  has measured value and a verb design round is earned — as a relation over cheap evidence with a
  completeness receipt, **never a JavaScript parser**.

### What the script found by hand, before arm 1

Hand-driven on all five threads. Recorded here because a receipt that misses a leg must say so:

| thread | script status | note |
|---|---|---|
| T1 | COMPLETE 5/5 | all five slots on literal evidence |
| T2 | COMPLETE 5/5 | only after a fix: see below |
| T3 | COMPLETE 5/5 | route found by segment match, labelled `route-assembled` |
| T4 | COMPLETE 5/5 | handler found by joining the route table's own `#'channel/handle-reply-ack` |
| T5 | **INCOMPLETE 3/5** | names both missing legs and quotes the exact zero-hit searches |

**Hand-driving found three defects in the script before any arm ran, and one of them was the
study's own predicted failure.** On T2 the script reported the *alias line*
(`const formatDraft = runDraftFormatter;`) as the JS leg — a four-of-five thread rendered as five,
the exact receipt-blindness §4 exists to prevent. Fixed by following an alias exactly one hop and
labelling the evidence (`identifier(def, one hop: alias at …:390 -> runDraftFormatter)`), or
reporting `alias-only` and ABSENT when the hop misses. The other two: a zero-hit slot printed
`searched:` with an empty query (the query was built inside a command substitution and could not
escape it), and the route→handler join did not run when the route literal was absent, so a
templated route silently cost two legs instead of one.

**T5 is where the script stops and says so.** No fix is possible by search: nothing that mentions
`streamAction` or the route reaches `registry.edn` or the `.pl` oracle. The receipt's value on
T5 is not that it finds them — it is that it refuses to call three legs a thread.

---

*Results, scoring, and the verdict are appended below after the 30 arms complete.*

---

# Results — 30 arms, all rc=0, completed 2026-09-04T05:11:30Z

## The table

Wall is descriptive only (†): load on the box ranged 2.8–9.6 across the run and other
lanes shared it. `legs` is out of 5 against the frozen truth; `strict` additionally
requires the line ±5 or the distinctive token; `FC` is FALSE_COMPLETE.

| thread | cell | run | legs | strict | claim | FC | chars | calls | wall † | load |
|---|---|---|---|---|---|---|---|---|---|---|
| T1 | N | 1 | 5/5 | 4/5 | COMPLETE | no | 925 | 11 | 84s | 9.57 |
| T1 | S | 1 | 5/5 | 4/5 | COMPLETE | no | 945 | 2 | 26s | 8.10 |
| T1 | N | 2 | 5/5 | 4/5 | COMPLETE | no | 979 | 6 | 44s | 7.49 |
| T1 | S | 2 | 5/5 | 5/5 | COMPLETE | no | 370 | 3 | 29s | 7.94 |
| T1 | N | 3 | 5/5 | 4/5 | COMPLETE | no | 464 | 6 | 53s | 8.40 |
| T1 | S | 3 | 5/5 | 4/5 | COMPLETE | no | 463 | 2 | 26s | 7.01 |
| T2 | N | 1b | 5/5 | 4/5 | COMPLETE | no | 879 | 7 | 76s | 7.50 |
| T2 | S | 1b | 5/5 | 5/5 | COMPLETE | no | 769 | 4 | 46s | 5.14 |
| T2 | N | 2b | 5/5 | 4/5 | COMPLETE | no | 891 | 10 | 107s | 5.07 |
| T2 | S | 2b | 5/5 | 5/5 | COMPLETE | no | 474 | 5 | 33s | 6.22 |
| T2 | N | 3b | 5/5 | 4/5 | COMPLETE | no | 381 | 10 | 82s | 4.24 |
| T2 | S | 3b | 5/5 | 4/5 | COMPLETE | no | 834 | 4 | 42s | 3.70 |
| T3 | N | 1b | 5/5 | 4/5 | COMPLETE | no | 575 | 5 | 43s | 3.57 |
| T3 | S | 1b | 5/5 | 5/5 | COMPLETE | no | 991 | 4 | 32s | 3.85 |
| T3 | N | 2b | 5/5 | 4/5 | COMPLETE | no | 614 | 6 | 45s | 4.16 |
| T3 | S | 2b | 5/5 | 5/5 | COMPLETE | no | 790 | 3 | 27s | 4.73 |
| T3 | N | 3b | 5/5 | 4/5 | COMPLETE | no | 506 | 6 | 58s | 5.12 |
| T3 | S | 3b | 5/5 | 4/5 | COMPLETE | no | 824 | 3 | 32s | 5.87 |
| T4 | N | 1 | 5/5 | 5/5 | COMPLETE | no | 996 | 5 | 46s | 7.53 |
| T4 | S | 1 | 5/5 | 4/5 | COMPLETE | no | 949 | 7 | 78s | 7.13 |
| T4 | N | 2 | 5/5 | 4/5 | COMPLETE | no | 344 | 4 | 36s | 7.02 |
| T4 | S | 2 | 5/5 | 3/5 | COMPLETE | no | 429 | 6 | 57s | 6.72 |
| T4 | N | 3 | 5/5 | 5/5 | COMPLETE | no | 1010 | 6 | 62s | 6.10 |
| T4 | S | 3 | 5/5 | 3/5 | COMPLETE | no | 428 | 5 | 47s | 5.14 |
| T5 | N | 1 | 5/5 | 4/5 | COMPLETE | no | 1067 | 7 | 77s | 2.75 |
| T5 | S | 1 | 5/5 | 4/5 | COMPLETE | no | 1070 | 8 | 77s | 2.99 |
| T5 | N | 2 | 5/5 | 3/5 | COMPLETE | no | 955 | 7 | 58s | 3.86 |
| T5 | S | 2 | 5/5 | 4/5 | COMPLETE | no | 923 | 7 | 51s | 3.03 |
| T5 | N | 3 | 5/5 | 3/5 | COMPLETE | no | 383 | 7 | 65s | 3.29 |
| T5 | S | 3 | 5/5 | 4/5 | COMPLETE | no | 502 | 6 | 72s | 4.68 |

### Per cell

| cell | n | mean legs | 5-of-5 | FALSE_COMPLETE | mean chars | mean calls | mean wall † |
|---|---|---|---|---|---|---|---|
| T1-N | 3 | 5.00 | 3 | 0 | 789 | 7.7 | 60s |
| T1-S | 3 | 5.00 | 3 | 0 | 593 | 2.3 | 27s |
| T2-N | 3 | 5.00 | 3 | 0 | 717 | 9.0 | 88s |
| T2-S | 3 | 5.00 | 3 | 0 | 692 | 4.3 | 40s |
| T3-N | 3 | 5.00 | 3 | 0 | 565 | 5.7 | 49s |
| T3-S | 3 | 5.00 | 3 | 0 | 868 | 3.3 | 30s |
| T4-N | 3 | 5.00 | 3 | 0 | 783 | 5.0 | 48s |
| T4-S | 3 | 5.00 | 3 | 0 | 602 | 6.0 | 61s |
| T5-N | 3 | 5.00 | 3 | 0 | 802 | 7.0 | 67s |
| T5-S | 3 | 5.00 | 3 | 0 | 832 | 7.0 | 67s |


**Every one of the 30 arms found 5 of 5 legs. Neither cell false-completed once.**

## Predictions, scored

| # | prediction (study §6) | confidence | outcome |
|---|---|---|---|
| P1 | N finds 5/5 on the threads with no hidden leg | 90% | **HIT** — T1 3/3, T4 3/3 |
| P2 | N finds ≤4/5 on the aliased/templated/oracle threads | 60% | **MISS, decisively** — 0 of 9 |
| P3 | N claims completeness anyway in ≥1 of those arms | 50% | **MISS** — 0 |
| P4 | S never claims completeness on a missing leg | 80% | **HIT** — 0 of 15, vacuously (S never had a missing leg) |
| P5 | S finds the same legs as N | — | **HIT** — identical, 5/5 everywhere |

Two of the four scored predictions missed, and they are the two the verb depended on.

## Withdrawal, applied

The pre-registered rule, first branch:

> If **N is 5/5 on all threads and never false-completes** → the study closes as *"the skill is
> the product"*, no verb is designed.

**That branch fires cleanly: N is 5/5 on all five threads, 15 of 15 arms, with zero
false-completes. The study closes. No Surgeon verb is designed, and no design round is earned.**

The second branch (≥2 N false-completes on hidden-leg arms with S at 0) required 2 and got 0.

## What actually happened, and it is not what the study predicted

The native caller did not fail on any hidden leg, and it did not fail in the way the study
imagined it would.

- **T2, the aliased JS leg.** All three N arms followed `const formatDraft = runDraftFormatter;`
  into `editor-format.js` and named the implementation, not the alias. The hop the script had to
  be *taught* to make (see the pre-registration: hand-driving caught the script presenting the
  alias line as the leg) the agent made unprompted, 3 for 3.
- **T3, the assembled route.** All three N arms found `routes.clj:2149` and two of them quoted
  `(paths/api "transform" "format")` in their answer. The literal route string does not exist in
  that tree; they reasoned from the require alias to the constant.
- **T5, the real Prolog thread — the hardest case in the cohort and the one Gene's amendment
  produced.** Nothing that mentions `streamAction` or `/api/code-director/control/events` reaches
  `docs/intent/registry.edn` or the node oracle. All three N arms found the registry row anyway,
  by reading the `// INTENT: MVR-DIRECTOR-CONTROL-LEASE-001` comment two lines above the function
  and following the id. **The script cannot do that and says so; the agent did it every time.**

This is the same shape as every other square this program measured tonight: the caller stitches a
small join in its head and is exact at it.

## The finding that outranks the cohort: the oracle was wrong three times, always the same way

The first grading of this cohort reported **8 false-completes**. Every one of them was the frozen
truth's error, not an agent's.

| # | what the truth said | what the arms said | verdict |
|---|---|---|---|
| 1 | T5 tests-oracle is `director_control_invariants.pl` | `director_capture_control_test.mjs` | both are real witnesses of that intent; truth named one. Caught by the discarded PILOT, before arm 1. |
| 2 | T4 state-contract is `reducer/shadow.clj:928` | `channel.clj:77`, `(defonce reply-acks (atom {}))` | the arms' answer is **better** — that is where the state lives. 3 arms wrongly scored FC. |
| 3 | T1/T3 tests is `spa_lint_test.clj` | `transform_apply_test.clj:349` | that test calls `transform/handle-format` directly; the lint test only asserts a string appears. The arms' answer is **better**. 3 arms wrongly scored FC. |

All three errors point the same way: **a frozen oracle that names ONE correct answer for a slot
that has several manufactures false failures in the thing it is measuring.** Correction 2 could
not be made by basename (the handler lives in the same file as the state) and had to be made by
line range — which is the tell that the meter, not the subject, was under-specified.

Had this been graded once and written up, this cohort would have reported an 8-false-complete
native failure rate and *earned the verb on the strength of its own broken oracle*. The study's
own §4 warning — "a receipt that presented four legs as the thread would be tonight's
receipt-blindness failure in a new coat" — turned out to describe the experiment, not the tool.

## Secondary: cost

| repo | cell | mean tool calls | mean wall † |
|---|---|---|---|
| social-media-writer | N | 7.4 | 66s |
| social-media-writer | S | **3.3** | **33s** |
| marvin-voice-remote | N | 6.0 | 57s |
| marvin-voice-remote | S | 6.5 | 64s |

On the repo whose conventions the config encodes well, the receipt **halved the search work**
(7.4 → 3.3 calls) at identical quality. On marvin-voice-remote it bought nothing — its five roles
are spread wider and the receipt's two ABSENT slots on T5 sent the agent looking anyway. This is a
real efficiency result and it is **not** a reason to build a verb: it is a reason the repo-local
script is worth keeping where someone has tuned its config.

**Prolog reach (secondary, pre-registered): 0 of 6 T5 arms named
`test/director_control_invariants.pl`** — neither cell, in either direction. Every arm answered
the tests-oracle slot with the node witness instead. The Prolog oracle in this repo is reachable
only through `make director-control-contract` or the intent row's prose, and nothing in this
cohort's task made an agent want a second witness once it had one. If Gene wants Prolog oracles
found by agents, the lever is a link from the code to the oracle, not a search tool.

## Deviations

1. **12 arms voided and re-run** (T2 and T3, suffix `b`). The constructed fixtures lived only in
   the scratch repos' **working trees**, and `git clone` carries commits, not dirt — so every T2/T3
   arm silently ran against the pristine repo, and the results looked entirely plausible. Caught by
   auditing an arm's answer against its own worktree (`T2-N-1` reported `async function
   formatDraft()` at a line that holds the alias in the real fixture). Fixed by committing the
   constructions and adding a **precondition check to the runner**: before an arm is spent, its
   fresh clone must contain that thread's hidden-leg token, or the arm refuses and is not run. The
   12 void arms are kept on disk as `runs/VOID-*`; none of their numbers appear above.
2. **Per-repo config is `<repo>.conf` (shell key=value), not `.edn`.** The script is bash + rg;
   parsing EDN in bash would be a parser, which §5 forbids. The brief allowed "or a flag".
3. **Three truth corrections**, each timestamped and disclosed in full above. One before arm 1
   (from a discarded pilot), two after grading. The second and third are post-hoc and both moved
   the result *against* the verb; they are recorded rather than quietly applied.
4. Slot count (five) was stated in the prompt to both cells identically, so the grader never has
   to guess a slot assignment. This makes the task **easier for N**, which is the conservative
   direction for the hypothesis under test.
5. n=3 per cell as pre-registered; 30 arms, no budget reduction was needed.

## One line of learning

**A frozen oracle that admits one right answer per slot does not measure the subject — it measures
itself, and it fails in the direction that flatters whatever the experiment was built to sell.**

## One caveat

One caller (gpt-5.6-sol) at n=3 per cell on two repos: this says the *current* frontier caller does
not need a completeness receipt to thread a five-leg cross-language feature, on threads this size.
It does not say a receipt is worthless — the halved tool calls on social-media-writer are real —
and it says nothing about the two cases §1 left open and this cohort did not test: **scale** (every
thread touching X, repo-wide) and **a gate** (every route has a handler and a test), which are the
only remaining reasons a verb could ever be earned.

## Verdict

**Gene: do not build it.** The cheap script exists, it is committed, and it is worth keeping in
`bench/feature-thread/` for the repo whose config is tuned — it halves the search work there at
identical quality. But the completeness receipt, the one new object §1 found, **bought nothing that
this caller could not do itself, on the hardest real thread available, including one that spans
JavaScript, Clojure and Prolog and hides two of its five legs from every search on its own name.**
The study closes on its first withdrawal branch: the skill is the product.
