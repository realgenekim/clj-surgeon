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
