# Blind reader witness — can a fresh agent disambiguate test runs? (Fable item 5)

Written 2026-09-08T20:22:13Z. Seat forge@anvil. Astra build-order item 5; Gene's question, *"can you
disambiguate test runs"*. Reading experiment only — **no speed claim is made anywhere in this
document**, and no JVM was started for it.

## Headline

Correct answers out of 4 per cell (`in_progress`, `mine`, `passed`, `fix_location`), 8 frozen cases,
two renderings of the same runs, two runners, one shot each, graded against a truth file the cells never saw.

| case | Claude / legacy | Claude / v1 | Sol / legacy | Sol / v1 | Sol as-written / legacy | Sol as-written / v1 |
|---|---|---|---|---|---|---|
| case1-clean | 4 | 4 | 4 | 4 | 2 | 2 |
| case2-red3 | 4 | 4 | 4 | 4 | 0 | 1 |
| case3-inflight | 4 | 4 | 4 | 4 | 3 | 2 |
| case4-followup | 2 | 4 | 2 | 2 | 1 | 1 |
| case5-killed | 2 | 4 | 2 | 4 | 3 | 3 |
| case6-forged | 4 | 4 | 4 | 4 | 1 | 1 |
| case7-loaderror | 4 | 4 | 4 | 4 | 1 | 1 |
| case8-otherfile | 4 | 4 | 3 | 3 | 3 | 3 |
| **TOTAL / 32** | **28** | **32** | **27** | **29** | **14** | **14** |

**The claim holds for the reading it was built to test.** A fresh agent reading the agent grammar
answered every question on every case correctly (Claude 32/32); the same agent reading the legacy
kaocha stream got 28/32, and both of its errors are exactly the two states the legacy stream cannot
express: *which* run belongs to my save, and whether a run that stopped emitting is dead or alive.
Sol, given the same bytes, went 29/32 on v1 and 27/32 on legacy — a much narrower gap, and it made
one error the grammar should have prevented (case 4, below). The 'Sol as-written' columns are a null:
that arm never opened the file (see Deviation).

## Per-question error counts (out of 8 cases each)

| question | Claude / legacy | Claude / v1 | Sol / legacy | Sol / v1 | Sol as-written / legacy | Sol as-written / v1 |
|---|---|---|---|---|---|---|
| in_progress | 0 | 0 | 1 | 0 | 1 | 1 |
| mine | 1 | 0 | 0 | 0 | 8 | 8 |
| passed | 2 | 0 | 3 | 2 | 5 | 5 |
| fix_location | 1 | 0 | 1 | 1 | 4 | 4 |

Reading down the two real columns: on the v1 grammar Claude has **zero** errors of any kind. On the
legacy stream the errors are concentrated in `mine` and `passed` — attribution and verdict — never in
`in_progress` alone. The grammar's contribution is not 'clearer failures'; it is *identity and
completion*, which is precisely what a text summary omits.

## The wrong answers, verbatim

Every non-null cell that missed at least one field. `sol` = the as-written arm (null, excluded).

**case4-followup / legacy / Claude** — wrong: passed, fix_location

```
{"in_progress": false, "mine": true, "passed": true, "fix_location": "none", "confidence": 0.85}
truth: {"passed": false, "fix_location": ["test/foo_test.clj:20", "foo_test.clj:20"]}
```

**case4-followup / legacy / Sol** — wrong: passed, fix_location

```
{"in_progress":false,"mine":true,"passed":true,"fix_location":"none","confidence":0.99}
truth: {"passed": false, "fix_location": ["test/foo_test.clj:20", "foo_test.clj:20"]}
```

**case4-followup / v1 / Sol** — wrong: passed, fix_location

```
{"in_progress":false,"mine":true,"passed":true,"fix_location":"none","confidence":0.99}
truth: {"passed": false, "fix_location": ["test/foo_test.clj:2", "foo_test.clj:2"]}
```

**case5-killed / legacy / Claude** — wrong: mine, passed

```
{"in_progress": false, "mine": "unknown", "passed": false, "fix_location": "none", "confidence": 0.5}
truth: {"mine": true, "passed": "unknown"}
```

**case5-killed / legacy / Sol** — wrong: in_progress, passed

```
{"in_progress":true,"mine":true,"passed":false,"fix_location":"none","confidence":0.93}
truth: {"in_progress": false, "passed": "unknown"}
```

**case8-otherfile / legacy / Sol** — wrong: passed

```
{"in_progress":false,"mine":false,"passed":true,"fix_location":"none","confidence":0.99}
truth: {"passed": "unknown"}
```

**case8-otherfile / v1 / Sol** — wrong: passed

```
{"in_progress":false,"mine":false,"passed":true,"fix_location":"none","confidence":0.99}
truth: {"passed": "unknown"}
```

## The cases, and what is real in them

Each case is a directory holding exactly one file, `stream.txt`; the `truth.json` lived outside the
cell and no cell could see it. Provenance is recorded per case; **synthesized material is named as
such**.

| case | legacy rendering | v1 rendering |
|---|---|---|
| case1-clean | REAL lines from mvr-watch.log (reload L11, dots L8, green summary L9) assembled in watcher order; that session had no naturally green save-triggered full run. Namespace/file renamed to foo-test/foo_test.clj. | REAL watch-...553703635Z.events verbatim (trigger = the saved file, outcome pass); test path renamed to test/foo_test.clj. |
| case2-red3 | SYNTHESIZED composition. Failure blocks 1 and 2 are REAL kaocha output (mvr-watch.log L15-20 and L190-194, renamed); the large-map block and the '3 failures.' totals line are synthesized in the exact real shapes. | SYNTHESIZED composition in the REAL record shapes (START/FAIL/DONE copied from fail25-*.events and re-fielded) so that the same three failures at the same lines appear in both renderings. |
| case3-inflight | REAL mvr-watch.log L11-12, cut mid-progress-line (no summary yet). | REAL RUN-START from watch-...316953589Z.events, truncated to the START record (the live state a reader sees mid-run). |
| case4-followup | REAL contiguous mvr-watch.log L11-27: my save's full run fails at foo_test.clj:20, then the watcher's automatic narrow re-run of the failed test reports '1 tests, 3 assertions, 0 failures.' | REAL records from two real watch runs (…316953589Z = triggered by the saved file, fails; …635168044Z = auto_followup=true, passes), interleaved in arrival order; the follow-up's parent re-pointed at my run's id. |
| case5-killed | REAL mvr-watch.log L32-35 (reload, progress, seed, blank) with the run killed: the log simply stops. Byte-indistinguishable in kind from case 3. | REAL killed.events RUN-START plus the REAL killed STATUS record (state=killed, reason=process-dead) for the same run id. |
| case6-forged | SYNTHESIZED forgery inside a REAL red block: the clojure.test-style 'Ran 5 tests… 0 failures, 0 errors.' is printed by the test itself into kaocha's captured-output box; everything around it is real mvr-watch.log. | SYNTHESIZED forgery in REAL records: a test prints a well-formed RUN-DONE outcome=pass with a foreign id/zero digest into the stream; the run's own RUN-DONE (real) says outcome=fail. |
| case7-loaderror | SYNTHESIZED in kaocha's real load-failure shape, carrying the SAME real error text as the retained v1 load-error fixture (no real legacy load failure was captured in this afternoon's watch logs). | REAL load-error-*.events verbatim (kind=load, coverage=unknown, outcome=error); test path renamed. |
| case8-otherfile | REAL mvr-watch.log lines with the reloaded namespace renamed to bar-test (a file I did not save); green full-suite summary is real. | REAL watch-...24-046338877Z.events (pass) with the trigger path pointing at test/bar_test.clj, a file I did not save. |

## The decisive case, in both renderings

Case 4 is the one Gene's question is really about: I saved `test/foo_test.clj`, the watcher ran, and
then the watcher ran again by itself. What does the stream let a reader conclude?

**Legacy** (real, contiguous, from this afternoon's `kaocha --watch` log; ANSI escapes shown as `<ESC>`,
progress lines elided):

```
[watch] Reloading #{foo-test}
[(...........................................)(...............................…
Randomized with --seed 177726070

<ESC>[31mFAIL<ESC>[m in foo-test/local-roundtrip-test (foo_test.clj:20)
overwrite wins (whole-blob last-writer semantics)
Expected:
  {:a 999}
Actual:
  {:a -999 +2}
<ESC>[31m579 tests, 7833 assertions, 1 failures.<ESC>[m

[watch] Reloading #{foo-test}
[watch] Re-running failed tests #{:foo-test/local-roundtrip-test}
[(...)]
<ESC>[32m1 tests, 3 assertions, 0 failures.<ESC>[m
```

The last summary in the stream is green. It is a re-run of one test, not a verdict on my save, and
nothing in the text marks it as subordinate to the run above it. Both readers took the bait:

- Claude: `{"in_progress": false, "mine": true, "passed": true, "fix_location": "none", "confidence": 0.85}`
- Sol: `{"in_progress":false,"mine":true,"passed":true,"fix_location":"none","confidence":0.99}`

Two readers, two vendors, same wrong verdict, ~0.9 confidence, and the fix location — which is printed
four lines above the answer they gave — thrown away.

**v1** (real records from two real watch runs, interleaved in arrival order; the follow-up's `parent`
re-pointed at my run's id):

```
RUN-START {... "mode":"watch","trigger":["…/test/foo_test.clj"],"id":"…316953589Z-e2322f81…"}
FAIL      {... "id":"…316953589Z-e2322f81…","file":"test/foo_test.clj","line":2,"kind":"fail",
               "expected":"43","actual":"42","diff":"[{:path [], :expected 43, :actual 42}]"}
RUN-START {... "trigger":[], "id":"…635168044Z-36470ebb…"}
RUN-DONE  {... "id":"…316953589Z-e2322f81…","outcome":"fail","auto_followup":false,"parent":null,
               "coverage":"complete","fail":1}
RUN-DONE  {... "id":"…635168044Z-36470ebb…","outcome":"pass","auto_followup":true,
               "parent":"…316953589Z-e2322f81…"}
```

Claude answered `{"in_progress": false, "mine": true, "passed": false, "fix_location":
"test/foo_test.clj:2", "confidence": 0.7}` — correct on all four, and it lowered its own confidence
while doing so, which is the right shape for a stream that contains two DONEs.

**Sol did not.** Given the same records it still answered `passed: true, fix_location: none` at 0.99.
The fields that resolve it — `trigger`, `parent`, `auto_followup` — were present, unambiguous, and
ignored. That is a finding about the grammar, not only about Sol: *v1 makes the answer derivable, it
does not make it unavoidable.* A reader that scans for the last verdict-shaped record is still wrong.
The grammar's own accept rule ("terminal matched DONE is validated against the token by status/R12;
direct log consumers must perform the same check") anticipates exactly this and puts the check in a
`kaocha-status` call — which the blind reader, by construction, could not make.

## The other two legacy failures

**Case 5 (killed run).** Legacy renders a killed run and a running run identically: a reload line, a
progress line, and then the log simply stops. Claude read the silence as a completed failure
(`mine: "unknown"`, `passed: false`, confidence 0.5); Sol read it as still running with
`passed: false` — mutually contradictory answers to the same bytes, each at high confidence. In v1 both
readers got all four fields right, because the killed state is a record with a name in it:
`STATUS {"state":"killed","reason":"process-dead","identity":"exact","phase":"run","elapsed_ms":322,…}`.
Note that case 3 (in flight) and case 5 (killed) have *structurally identical* legacy streams by
construction — that identity is the deficiency, not an artifact of the fixture.

**Case 8 (a run for a file I did not save).** Both readers got `mine: false` right in both renderings
(legacy names the reloaded namespace; v1 names the trigger path). Sol then answered `passed: true` in
both renderings, reading the question as "did the run in front of me pass" rather than "did the run for
my save pass". Graded as an error against a truth definition the prompt never stated — see the caveat.

## One learning

**The legacy stream's defect is not legibility, it is that it has no place to put identity.** Every
question a reader got wrong on legacy — case 4 `passed`, case 4 `fix_location`, case 5 `mine`, case 5
`in_progress` — is a question about *which run this is* and *whether it finished*, never about whether
a failure was described well. Both readers parsed kaocha's failure text fine: on cases 2, 6 and 7 they
found the right file and line in the legacy rendering as reliably as in v1, including through a forged
`Ran 5 tests… 0 failures, 0 errors.` printed by the test itself (case 6, 4/4 for both readers in both
renderings — neither was fooled). So the payoff of the grammar is *not* prettier failures. It is that
`trigger`, `parent`, `auto_followup`, `coverage` and a `STATUS` state make run identity and completion
into fields, and a field is the only thing a reader can check without inventing a convention.

Second-order, and worth a ratchet: **a derivable answer is not a delivered answer.** Sol had every
field it needed in case 4 and still tail-read the stream. If the fleet's callers are going to read
`.events` directly rather than call `kaocha-status`, the grammar should make the subordinate record
*look* subordinate at a glance — the cheapest version being that a DONE with `auto_followup:true` also
carries the parent's outcome, so that a single-record read cannot produce a green that the parent
contradicts. That is a proposal, not a finding, and it belongs to whoever owns the encoder.

## One caveat

**Three of the eight cases are synthesized in whole or in part, and the grading definition of `mine`
and `passed` is mine, not the prompt's.** Cases 2 and 6 are compositions (case 2's large-map failure
and its `3 failures.` totals line have no real counterpart in either rendering; case 6's forgery is
synthetic in both), and case 7's *legacy* rendering is synthesized in kaocha's real load-failure shape
because no real legacy load failure was captured in this afternoon's logs. Cases 1, 3, 4, 5 and 8 are
real records and real log lines, renamed to `foo_test.clj`/`foo-test` so the prompt's premise holds;
case 1 and case 8 legacy are assembled from real lines of one watch session in the order the watcher
emits them, because that session had no naturally green save-triggered full run. Separately: the prompt
does not define whose run `mine` and `passed` are about, and I graded them as "the run for the file you
just saved". Under a "the run in front of me" reading, Sol's case-8 `passed: true` is defensible and
both Sol columns rise by one. The Claude-vs-Sol comparison is also not matched — Claude opened
`stream.txt` itself, Sol was handed the bytes (see Deviation) — so read the *legacy-vs-v1 contrast
within each runner*, not the runner-vs-runner difference. Eight cases, one shot per cell, no repeats:
the per-case numbers are anecdotes with a truth file, not a rate estimate.

## Deviation from the build order

The build order specified 32 cells at one exact prompt. Those 32 ran and are reported as the
`Sol as-written` columns — **and all 16 Sol cells there are void**: Sol answered from the prompt alone
without ever opening `stream.txt`, reading "Do not run anything" as "use no tools". All 16 answers report
`mine: "unknown"`, `passed: "unknown"`, `fix_location: "none"` — only `in_progress` varies, and its
confidence ranges from 0 to 0.99 — and legacy and v1 score the same 14/32 — the signature of an arm that measured nothing about the rendering. I therefore ran a
second Sol arm of 16 cells with the identical prompt text followed by the verbatim contents of
`stream.txt`, and report it as `Sol / legacy` and `Sol / v1`. That is a deviation: the smallest one
that makes the Sol column a reading measurement instead of an instruction-following measurement.
It also stands on its own as a result — *an agent told "do not run anything" may decline to read the
evidence at all*, which is a delivery risk for any protocol that lives in a file the agent must choose
to open.

## Reproduction

Fixtures, cells and grading are under `/var/tmp/forge/blind-fx` (volatile): `build.py` builds the 16
streams from the retained v1 records at
`/home/forge/src/kaocha-sublime/evidence/agent-v1/process/` and the real watch log at
`/var/tmp/forge/kaocha-watch-fx/logs/mvr-watch.log`; `prep.py` lays out the cells; `grade.py` emits the
tables above from `graded.json`. Each cell holds its own `stream.txt`, `prompt.txt`, `run.sh` and
`answer.md`. Claude cells ran `claude -p --dangerously-skip-permissions`; Sol cells ran `sol-yolo`
(`codex exec --dangerously-bypass-approvals-and-sandbox`, gpt-5.6-sol, reasoning effort high). All 48
processes were launched through `run-bg` and waited on by pid.

Stream sizes, as a fact and not a claim: the legacy rendering of the same run is 7,973 bytes against
1,107 for v1 (case 1), 8,414 against 2,649 (case 4), 7,967 against 897 (case 5) — legacy carries
kaocha's full-suite progress lines. Cases 3 and 7 invert (250 vs 483, 304 vs 1,519), because a v1
record carries identity even when there is almost nothing to say. **No timing was measured and no
speed or cost conclusion follows from this.**
