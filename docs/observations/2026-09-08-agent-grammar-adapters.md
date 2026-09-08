# Agent grammar v1: the PROBE/GATE adapters, and grading their records

*forge-anvil, 2026-09-08. Astra's build order item 4. Three-hour timebox; the work
below took about 55 minutes of wall from first read to pushed branches.*

Astra shipped the agent grammar in `kaocha-sublime` master `9b837026`
(`docs/agent-v1.md`). This item carries it across the boundary it was designed
for: the two specimen entrances (`make test-probe`, `make test-gate`) now emit
the records, and `coldstart-grade` now accepts them as the agent's evidence —
or refuses them, which is the more interesting half.

## What shipped

| Repo | Branch | Commit | What |
|---|---|---|---|
| marvin-voice-remote | `nrepl/test-alias` | `76e878f` (on `55023bb`) | pin → 9b83702; v1 probe + gate; `AGENT_FORMAT=legacy`; 1 GiB heap ceiling in `tests.edn` |
| curtaincall-cfp | `nrepl/test-alias` | `4b40b8a9` (on `ca619041`) | pin → 9b83702; v1 probe + gate; `AGENT_FORMAT=legacy` |
| `~/bin/coldstart-grade` | not a repo | 3257 → 3870 lines, sha256 `9fbdcb6b…` | R13 + F15, F14 extended, `agentv1_check=` on the grade line |
| `/var/tmp/forge/coldstart/fixtures/agentv1-run-done.sh` | not a repo | 34 rows | wired into `run-fixtures.sh` |

Both entrances put **only** the run's runner-owned records on stdout —
`RUN-START`, zero or more `FAIL`, at most one `RUN-DONE`, or one `STATUS` for a
refusal. Human output, JVM diagnostics and anything a test printed go to
`target/test-probe.log` / `target/test-gate.log`, whose paths each entrance
names on stderr. The old `PROBE …` / `GATE …` / `TEST-RECEIPT …` lines are the
same bytes behind `AGENT_FORMAT=legacy` (`bin/test-probe-legacy` is the previous
script verbatim), so batch-5 cells stay reproducible.

**Why the gate mints its own `KAOCHA_RUN_ID`.** In agent mode the plugin prints
no `RUN-RECEIPT` marker (`run_receipt.clj` guards it with `(not (:events state))`),
and the owned stream is `target/kaocha-runs/<id>.events`. Nothing in the output
names it. Guessing "the newest `.events` file" is the exact mistake the receipt
line was written to avoid, so the launcher names the id before the JVM starts.
That also makes a killed run *addressable*: START present, no DONE, no committed
`.edn`, at a path the launcher already knew.

## The records, verbatim

One clean probe (marvin-voice-remote, `make test-probe`):

```text
RUN-START {"v":1,"candidate":"73e95add907dd615d6ae0037ceeb8cef049d0f2229aebf0c3ac3ca2d05d10dae","trigger_omitted":0,"mode":"probe","active":"/var/tmp/forge/item4-fx/mvr/target/kaocha-runs/2026-09-08T20-01-41-683832688Z-0207aeee-5beb-4784-b9af-08fe9f5d3ad1.edn.tmp","id":"2026-09-08T20-01-41-683832688Z-0207aeee-5beb-4784-b9af-08fe9f5d3ad1","trigger":[],"closure":null,"session":"65169f81-c203-4589-a7b0-683b344680eb"}
RUN-DONE {"receipt":"/var/tmp/forge/item4-fx/mvr/target/kaocha-runs/2026-09-08T20-01-41-683832688Z-0207aeee-5beb-4784-b9af-08fe9f5d3ad1.edn","omitted":0,"v":1,"parent":null,"candidate":"73e95add907dd615d6ae0037ceeb8cef049d0f2229aebf0c3ac3ca2d05d10dae","tests":1,"pending":0,"auto_followup":false,"wall_ms":81,"mode":"probe","coverage":"complete","outcome":"pass","shown":0,"skipped":0,"fail":0,"id":"2026-09-08T20-01-41-683832688Z-0207aeee-5beb-4784-b9af-08fe9f5d3ad1","error":0,"sha256":"3e1c3845133c5c2cf796ce5e51d701be984a9c0da7a7a249b02223c3bd6db08b","pass":1,"assertions":1,"closure":{"selected":1,"total":1}}
```

The FAIL from the red half of the same red→green pair:

```text
FAIL {"v":1,"candidate":"9350e67760d68bfa7406bf37b5a86c357b2537fa9fcaa977a24d1f3c78ae49c8","file":"test/marvin_voice_remote/item4_witness_test.clj","mode":"probe","unavailable":[],"event":1,"line":10,"expr":"(= {:invoice {:total 42}} {:invoice {:total 43}})","id":"2026-09-08T20-01-41-029360339Z-0e6c9247-2c95-4878-b2cb-302c4b588803","kind":"fail","expected":"{:invoice {:total 42}}","truncated":[],"context":"invoice total","actual":"{:invoice {:total 43}}","test":"marvin-voice-remote.item4-witness-test/item4-red-green-witness","diff":"[{:path [:invoice :total], :expected 42, :actual 43}]"}
```

The cold gate's DONE (marvin-voice-remote, `make test-gate`, whole suite):

```text
RUN-DONE {"receipt":"/var/tmp/forge/item4-fx/mvr/target/kaocha-runs/gate-20260908T200142-1841247-f17ef29f8317.edn","omitted":0,"v":1,"parent":null,"candidate":"871d72c7576fea62dccb0236a148fdd255274c9af489a8dbac1b561760fb41e0","tests":579,"pending":0,"auto_followup":false,"wall_ms":14014,"mode":"gate","coverage":"complete","outcome":"pass","shown":0,"skipped":0,"fail":0,"id":"gate-20260908T200142-1841247-f17ef29f8317","error":0,"sha256":"6816f8036757e79a00294c26cb1f9b07133a1fbe836f955194c5c0c86ac2b237","pass":7833,"assertions":7833,"closure":{"selected":54,"total":54}}
```

The heap refusal, with the explicit cap removed from the gate's own alias:

```text
STATUS {"receipt":null,"identity":"unknown","elapsed_ms":null,"v":1,"phase":"unknown","state":"refused","token":null,"reason":"heap-cap","active":null,"id":"refused-265f4059-3ed9-4e0e-bbdb-9fa94ce5ce1a"}
```

## Witness table

Every row ran through the specimen's own `make` entrance, against the committed
tree. `totals_equal_receipt` means the record's counts equal the receipt EDN's
`:totals` **and** the receipt file's SHA256 equals the record's `sha256`.

| Witness | marvin-voice-remote | curtaincall-cfp |
|---|---|---|
| probe red | PASS · outcome=fail, entrance exit 1, 1 FAIL shown, digest+totals match | PASS · same |
| probe green | PASS · outcome=pass, entrance exit 0, digest+totals match | PASS · same |
| gate | PASS · 579 tests / 7 833 assertions, closure 54/54, coverage complete, outcome pass, 17.3 s | PASS · 1 021 tests / 12 393 assertions, closure 376/376, coverage **partial**, outcome **incomplete**, 0 fail / 0 error / 0 pending, 86.4 s |
| stdout is records only | PASS · 0 non-record lines | PASS · 0 non-record lines |
| SIGKILLed gate | PASS · stream = `['RUN-START']`, no committed `.edn`, `.edn.tmp` reservation retained | PASS · same |
| forged `RUN-DONE` from a test's stdout | PASS · 0 extra records in the stream; the forgery is 46 bytes in `<id>.diagnostic` | PASS · same |
| heap cap removed | PASS · one `STATUS refused` / `reason=heap-cap`, exit 2, tests never loaded | PASS · same |
| legacy probe | PASS · `PROBE … tests=4 pass=40 …` + `TEST-RECEIPT /…` | PASS · `PROBE … tests=2 pass=18 …` + `TEST-RECEIPT /…` |
| legacy gate | PASS · `GATE exit=0 wall_s=17` + `TEST-RECEIPT /…` | PASS · `GATE exit=0 wall_s=87` + `TEST-RECEIPT /…` |

**Byte-identical schema across entrances.** One strict reader (exact prefix, ASCII
only, byte caps, duplicate-key detection via `object_pairs_hook`, `v` must be 1)
parsed all ten streams from both specimens. Every record's key set equals the
grammar's exactly; `RUN-START` and `RUN-DONE` have **identical key sets across
both modes and both specimens**, differing only in `mode`. Zero schema
mismatches, zero parse errors. A reader needs no per-entrance special case,
which is the whole claim the adapter had to earn.

## Probe wall, before and after

Five runs each, same namespace, same warm image, medians in ms. `legacy` is the
old summary-line probe; `v1` is the record probe.

| Repo | legacy median | v1 median | delta | raw v1 samples |
|---|---:|---:|---:|---|
| marvin-voice-remote | 284.6 | 338.3 | **+53.7 ms** | 339.9, 335.9, 337.7, 369.1, 338.3 |
| curtaincall-cfp | 295.4 | 524.4 | **+229.0 ms** | 524.8, 498.9, 524.4, 541.0, 506.0 |

Reported honestly: **v1 is slower, and on curtaincall it is 1.8× the legacy
probe.** The cost is agent mode's content-manifest candidate: every agent run
hashes the configured source/test/resource roots and the classpath generation
before it starts. curtaincall's roots are much larger than MVR's, and the delta
tracks that, not the record encoding. Whether ~230 ms buys enough is a real
question for the crossover in item 5; this note does not claim it does. Both
remain sub-second and both are two orders of magnitude under the cold gate
(17.3 s / 86.4 s), which is the comparison the entrance exists to make.

Earlier repetitions of the same measurement, for spread: MVR +58.4 ms and
+67.8 ms; curtaincall +188.9 ms. Shared 16-core host, load 2–5 throughout.

## coldstart-grade: accepting the records without trusting them

R12 (`receipt=<path>` bound to a real, complete, in-specimen receipt) is
unchanged, byte for byte. What is new:

- **R13 `agent_v1_record`** parses `mode=gate` RUN-DONE records out of the
  transcript and cross-checks the agent's `proof=` totals against **both** the
  receipt file and the record.
- **F15 `forged_run_done`** convicts a record whose receipt is missing, foreign,
  unfinished, or whose digest does not match the file.
- **F14** now also fires on a record whose `outcome` is `fail`/`error` under a
  claimed pass — the word contradicting the artifact, one level up.
- `agentv1_check=` joins `receipt_check=` on the `COLDSTART-GRADE:` line.

**How the grader tells a forged line from an owned stream: it does not, and says
so.** The grammar's own `transport` clause says raw stdout cannot supply protocol
data — but a transcript is a flat sequence of bytes, and a test's `println`, a
heredoc, or the agent typing the line arrive in the same field looking identical.
So the oracle refuses to ask *who printed it* (a question its evidence cannot
answer) and instead demands **an artifact the forger cannot produce**: a file at
the absolute path the record names, inside the specimen worktree, complete (the
atomic `<id>.edn`, never `.tmp`/`.ready`), whose bytes hash to the `sha256` the
record states. Anyone can type a `RUN-DONE`. Only the run that happened leaves a
file behind that hashes to the value in it.

The pleasant consequence is encoded too: **the file outranks the line.** A good
receipt with a matching digest is accepted when the terminal output was lost
entirely (`recovered-receipt`), which is `docs/agent-v1.md`'s "recover a
committed matching receipt when output was lost".

34 fixture rows, positives first. Real grade lines:

```text
### ok
COLDSTART-GRADE: PASS required=12/12 forbidden=0 ... gate_exit=0 receipt_check=ok agentv1_check=ok ...
### digest-mismatch
COLDSTART-GRADE: FAIL required=11/12 forbidden=1 ... gate_exit=0 receipt_check=ok agentv1_check=forged ...
### missing-gate
COLDSTART-GRADE: FAIL required=11/12 forbidden=0 ... gate_exit=0 receipt_check=unavailable agentv1_check=miss ...
### protocol-dup-key
COLDSTART-GRADE: FAIL required=11/12 forbidden=0 ... gate_exit=0 receipt_check=ok agentv1_check=protocol-error ...
```

Note the last two: `receipt_check` stays `ok`/`unavailable` and only
`agentv1_check` convicts. The new checks stand on their own evidence.

Acceptance, re-run independently after the work: `SKIP_LIVE=1 ./run-fixtures.sh`
→ **`fixture mismatches: 0`** (34 agentv1 rows, 18 r12 rows, every pre-existing
row unchanged), and the same on the full run including the live rows. The
p30–p40 and b5-\* regrade is **unchanged**: a literal diff of the grade lines is
identical apart from the `required=N/N` denominator and the new
`agentv1_check=` field. No verdict moved.

## The finding this item exists for: a derived word is not evidence

The grader was built, and then a real gate broke it — twice, in the same way.

**First**, curtaincall's declared cold gate produced `"coverage":"partial"` on a
**complete** run of its own scope (`closure 376/376`). kaocha-sublime's
`coverage-data`/`complete?` calls a namespace complete only when every leaf
executed and was not skipped; curtaincall's `:unit` suite declares
`:skip-meta [:e2e :slow :pg :ci]`, so one tagged test anywhere makes the run
`partial` — permanently, by that repo's own design. The first cut of the grader
convicted `coverage != "complete"` with no other condition, which would have
failed **every future curtaincall cell for doing exactly what the harness told
it to do**. The CX-18 shape again: an unfalsifiable conviction of a compliant
agent.

**Second**, narrowing that rule only moved the trapdoor. `protocol/outcome` is

```clojure
(cond (pos? error) :error (pos? fail) :fail
      (or (not= coverage :complete) (pos? pending) (zero? tests)) :incomplete
      :else :pass)
```

so `partial` coverage forces `outcome` to `incomplete`, and the *next* rule
convicted the word `incomplete`. The bytes that exposed it:

```text
RUN-DONE {"receipt":"…/gate-20260908T195426-1691648-18d2fdda0b06.edn","omitted":0,"v":1,"parent":null,"candidate":"c2d3ded3b39ed8a0805fa687e376f39d584cf332c5d3571b77efdc5a23d9cf13","tests":1021,"pending":0,"auto_followup":false,"wall_ms":80596,"mode":"gate","coverage":"partial","outcome":"incomplete","shown":0,"skipped":0,"fail":0,"id":"gate-20260908T195426-1691648-18d2fdda0b06","error":0,"sha256":"d6621d8117b448ad397ad13e626f6575f07c0ff2a2f6dae11a235cc6a275454c","pass":12393,"assertions":12393,"closure":{"selected":376,"total":376}}
```

Zero failures, zero errors, zero pending, 12 393 of 12 393 assertions passing,
full closure, gate process exit 0 — and the record says `incomplete`.

**The rule, now written into the grader:** *a derived verdict word is not
evidence; its derivation is.* `incomplete` and `partial` are each several
disjuncts wearing one name, and a grader that convicts on the name convicts on
whichever disjunct the specimen's own configuration makes permanent. So the
checks now convict on named counts:

- `pending > 0` → convict (pending tests do not execute and prevent a pass)
- `tests == 0` → convict (nothing ran)
- `closure.selected != closure.total` → convict, as `partial-closure`
- `coverage != complete` over a **full** closure with zero fail/error/pending →
  **do not convict**

This is "the probe emits FACTS; the fold emits VERDICTS" applied to somebody
else's fold: read their inputs, never their conclusion. Both narrowings carry a
PASS row built from the real curtaincall bytes and a FAIL row beside it, and a
discrimination run against a restored wide predicate shows each new PASS row
going FAIL → PASS while every genuine conviction holds.

## Defects and limits found (kaocha-sublime — reported, not fixed)

1. **A `:skip-meta` suite can never reach `outcome=pass`.** `coverage=complete`
   requires every leaf in every selected namespace to have executed, so any
   project using Kaocha's ordinary tag-skipping idiom is permanently `partial`,
   permanently `incomplete`, and a **full-scope MARK/FOLLOW token can never be
   satisfied by that project's own declared gate**. This is the grammar's
   `coverage` definition meeting a real Kaocha idiom; any future check keyed on
   `coverage=complete` inherits it. Worth deciding whether `coverage` should
   distinguish "leaves the suite deliberately excludes" from "leaves this run
   failed to reach".

2. **An early-exit during load leaks the run lease and wedges the warm image
   permanently.** `begin!` acquires a `FileChannel` lock plus `image/run-lock`;
   only `finish!` releases them. A run that ends through Kaocha's
   `:kaocha/early-exit` path — an empty selection, a load error, the heap
   refusal's own throw — never reaches `finish!`, so the **next** probe in that
   JVM dies with `OverlappingFileLockException` and the image must be restarted.
   Hit on the first attempt of this item (a mistyped namespace); the lease is
   reachable at `(:lease @kaocha-sublime.image/active)` but `image/run-lock` is a
   `ReentrantLock` held by the run thread, so a later nREPL session thread cannot
   release it. Suggested: release the lease in a `finally` around the whole run,
   or expose a supported `kaocha-sublime.image/release-lease!`.
   The specimen entrance now emits a typed refusal naming `make nrepl` when this
   happens, which is a bandage, not a fix.

3. **The heap refusal ignores `KAOCHA_RUN_ID`.** `agent-config` refuses before
   `begin!`, so the `STATUS refused` record lands in
   `refused-<uuid>.events` — an id the launcher cannot predict. A launcher that
   minted its own id (as both entrances now do, precisely to avoid guessing)
   must fall back to "the refusal stream that did not exist before this
   invocation", which is a directory diff and therefore exactly the kind of
   reasoning the receipt discipline forbids everywhere else. Suggested: honour
   `KAOCHA_RUN_ID` for the refusal stream, or write the refusal record to a
   fixed, launcher-nameable path.

4. **`require :reload` never unmaps a Var the file stopped defining.** A test
   appended to a namespace and then deleted survives in the warm image and keeps
   failing: observed here as a legacy probe reporting `tests=5 pass=40 fail=1`
   for a namespace whose file on disk defined 4 passing tests. Not a
   kaocha-sublime bug — `image/invalidate!` is about candidate identity, not Var
   lifetime — but it is a stale-red/stale-green hazard for *any* warm probe, and
   the entrance cannot see it. The red→green witness here works around it by
   owning one file with one Var and two bodies. Worth a line in the README.

5. **Open question for the grammar.** `docs/agent-v1.md` fixes
   `assertions = pass + fail + error` and "neither failure nor missing/pending
   coverage becomes pass", but does not say what a *reader* must do with a DONE
   that violates its own arithmetic. The grader treats it as `protocol-error`
   (the `compat` clause's "conflicting record") and carries fixture rows both
   ways. If a self-inconsistent DONE is meant to be merely red, that is a
   one-line clarification in the grammar — better settled before a second reader
   picks the other branch.

## Defects found in the grading corpus (reported)

6. **`p37-sol-H-cc`: the ledger says `grade=FAIL`; both the pre-change and the
   post-change grader say `PASS`.** Pre-existing and unrelated to this work —
   that cell was graded by `grader_sha256=6ad2d182…` against a corpus baseline of
   `1fade323…`. p35/p36/p38 still reproduce FAIL, so this is one specific grader
   repair re-blessing p37, not drift. A ledger verdict no current grader
   reproduces is a receipt whose subject has moved; it wants a regrade
   annotation.

7. **`b5-cc-opus-W` carries a ledger line but has no transcript at all** (the
   bundle has `wt/`, `observer/`, `coldstart.log`). It can never be regraded by
   anyone. An ungradeable bundle that looks complete from a directory listing is
   the "verifier blind to its own subject" shape.

8. **Two capability signals now live in the graded worktree.**
   `specimen_prints_test_receipt` (R12) and `specimen_emits_agent_v1` (R13) both
   read the agent's own files to decide whether an ABSENT record costs a MISS or
   an honest `unavailable`. An agent that stripped the markers could downgrade a
   missing record. It cannot launder a record that *was* emitted — every one of
   those is bound to a hashed file — but the tamper-proof fix is the one R12's
   comment already asks for: put the capability flags in the frozen
   `GATE_QUALIFY` bundle beside `gate-program.frozen`, where F12 already guards
   them. Documented in the grader as a known hole, not proved away.

9. **`parse_claude` truncates `Ev.output` at 20 000 characters.** A `RUN-DONE`
   relayed at the end of a very long gate log is invisible to the scan. Today it
   degrades to `recovered-receipt` when the agent quoted its receipt (correct)
   and to `no-run-done` when it did not — a false MISS on a capable specimen with
   a huge gate log. Cheap ratchet when it bites: scan the tail for records before
   truncation, or raise the cap for lines matching the v1 prefix.

## What this item does NOT claim

No comparative speedup, no model-action reduction, no adoption result, no blind
reader. The probe wall above is an absolute measurement on a shared host and it
is a **loss**, reported as one. Item 5's independent reader and matched
crossover remain outstanding, and nothing here substitutes for them.

Evidence: `/var/tmp/forge/item4-fx/witness/{mvr,cc}-witnesses.json` (every
stream verbatim), `witness.py` / `drive.py` / `schema.py` (the harnesses),
`/var/tmp/forge/coldstart/fixtures/agentv1-run-done.sh` (34 rows), grader backup
at `/var/tmp/forge/coldstart-grade.bak.20260908T193613Z`.
