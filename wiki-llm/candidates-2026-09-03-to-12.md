# Candidate entries, 2026-09-03 → 2026-09-12 — sublime vs unsublime patterns

Gene, 2026-09-12: *"start compiling all of the dues and don'ts to go into the wiki that we can use to
start capturing patterns of good and bad for sublime behaviors. Everything code testing to patterns
to exceptions anything that leads to making a sublime score go up and down."*

These are CANDIDATES for `sublime-vs-unsublime-patterns.md`, in the labelled form the compile asked
for (name · symptom · counterexample with receipt · why · counterpart · ratchet · meter · category).
Promotion into the page's prose form is a later pass. Nothing here repeats the 13 anti-patterns and
6 sublime patterns already on that page; near-neighbours I deliberately did not repeat are listed at
the end.

**Count, and how to prune.** 66 candidates, above the 25–50 the compile asked for. I did not pad —
every one carries a dated receipt and a distinct lesson — but if the page wants a hard 50, cut these
sixteen FIRST, in this order, because each one's lesson is either carried by a neighbour that stays
or is seat-operational rather than general: 5, 13, 24, 49 (carried by 6/17/20/37/46), 41, 45, 48, 50,
54, 63, 64, 66 (apparatus and delivery detail), 33, 57, 59, 40 (process and review detail). Cutting
them leaves 50 with every category still represented.

**The three meters, never blended** (log, 2026-09-12T01:54Z): **trunk** (what a tag is worth),
**window** (how the hours were spent), **skills' index** (the `sublime-every-day` / `tighten-the-loop`
terms). The week as the log plots it:

```
            Sat 06  Sun 07  Mon 08  Tue 09  Wed 10  Thu 11  Fri 12
 trunk        tag     tag     -       9       9       9       9
 window        -       -      7      7→6      7      8→9      9
 skills idx    -       -      -       -       -      3→4      4
```

## Sources read

Primary, in `/home/forge/src/clj-surgeon-records/docs/observations/`:

- `2026-09-03-captains-log-anvil-seat.md` (5,972 lines / 1.3 MB / 1,423 `##` entries). All 1,423
  entry headings scanned; full text read at lines 456–460, 475–495, 615–649, 801–832, 901–940,
  1240–1260, 1357–1378, 1620–1622, 1936–1940, 2051–2056, 2147–2152, 2599–2634, 2795–2801,
  2860–2875, 3116–3120, 3178–3186, 3350–3354, 3625–3640, 3890–3900, 4260–4270, 4355–4375,
  4380–4480, 4560–4600, 4702–4760, 4856–4871, 4911–4915, 4949–4953, 4979–4990, 4992–5070,
  5145–5165, 5900–5972.
- Sept 3 verdicts/reviews: `andon-find-build-files-review-opus`, `anvil-arms-apparatus-sol-review-NO-GO`,
  `anvil-arms-apparatus-round4/5/6-sol-review`, `census-redteam-GO-WITH-FIX`, `census-rereview-NO-GO`,
  `census-round15-rereview-opus`, `folddiff-lens-redteam`, `folddiff-round11-rereview-opus`,
  `gene-report-night`, `mem-003-round8-rereview-opus`, `mem-005-round3-rereview-opus-GO`,
  `memory-battery-sol-review`, `memory-battery-round2-sol-review`, `q5z-round3-rereview-NO-GO`,
  `q5z-round9-rereview-opus-GO`, `ratchets-redteam-GO-WITH-FIX`, `resume-here-anvil-seat`,
  `rf2-q5z-redteam`, `rf2-round4-rereview-opus-GO`, `study-ops-redteam-NO-GO`,
  `study-round4-rereview-opus`, `txn-journal-sol-review`, `txn-journal-round9-rereview-opus`.
- Sept 4–6 cohorts/ethnographies: `e3-p-cohort`, `e6-lb-cohort`, `e6c-routing-plate-cohort`,
  `eafford-sed-counterfactual-cohort`, `ecaller-cohort`, `eprewrite-preregistration`,
  `escalewall-preregistration`, `ereg-irregularity-cohort`, `feature-thread-replay-result`,
  `gene-report-0815z`, `gene-report-2139z`, `fast-typist-cohort-1`, `gene-report-1933z-aperture-window`,
  `gene-report-2342z-night`, `helper-extraction-fence-review-r1`/`r7`, `astra-agent-usage-result-authority`,
  `astra-forms-cohort-prereg`/`result`, `astra-fresh-caller-review`, `astra-paper-cuts-ethnography`,
  `astra-raw-cohort-control-failure`, `astra-raw-cohort-v2-result`, `astra-recovery-pilot-result`,
  `extract-E-ethnography`/`result`, `fanout-B-cohort-result`, `fanout-I-result`, `fanout-J-result`,
  `real-session-ethnography`, `sublime-tool-for-astra`, `gene-report-1950z-crank`, `wiring-test-result`,
  `astra-review-reconciled`.
- Sept 7–9: `aliasfix-r2`/`r4-sol-review`, `expectfix-r1`/`r5-sol-review`, `gene-report-dogfood-2`,
  `gene-report-lid-audit`, `gene-report-ten-decisions`, `lid-cc-sol-report`, `lid-mvr-sol-report`,
  `pair-1-preregistration`/`result`, `plan-2-preregistration`, `plan-2-cellA`/`cellB`/`cellC-result`,
  `astra-B01-report`, `battery-parallel-verdict-parity`, `gene-report-evening`/`night`,
  `row2-alias-preregistration`, `row3-require-preregistration`, `rows-sublime-4-fence`,
  `sublime-score-5-days`, `gene-report-morning`/`night` (09-09), `item1-sol-verdict-round-3`,
  `item3-sol-verdict-round-1`/`9`/`17`.
- Sept 10–12: `astra-ethnography`, `cohort-astra-review`, `cohort-proposal`/`v2`,
  `ethnography-plan-of-record`, `ethnography-program-edits`, `gene-report-evening`,
  `insert-forms-opus-redteam`, `parity-harness-sol-verdict-round-1`/`4`, `rename-alias-opus-redteam`,
  `seat-path-defaults-sol-verdict-round-1`, `skiff-install-sol-verdict-round-1`/`3-GO`,
  `astra-sublime`, `block1`/`block2`/`block3-report-astra`, `block2-opus-redteam`,
  `clj-splice-opus-redteam`, `cohort-report-v1`/`v2`, `gate-vs-prompt-preregistration`/`amendment-1`/
  `report-claude`/`report-codex`/`astra-review`, `refusals-astra-review`, `refusals-native-parity-rule`,
  `sublime-integration`, `system-plan-of-record`, `gene-report-morning`, `longctx-preregistration`/
  `report`/`report-attempt2-blocked`, `codex-arm-n-report`, `block4-report-astra`,
  `block4-delta2-report-astra`.
- The memory index `/home/forge/.claude/projects/-home-forge-src-marvin-voice-remote/memory/MEMORY.md`
  (used for ratchet ids and to avoid repeating promoted lessons).

The `2026-09-12-bbtower-block-b-*` cluster was deliberately NOT re-mined: it is the source of the
existing page.

---

## code

### 1. Identity by spelling, not by binding
Symptom: a rewrite or a preservation claim treats two occurrences of the same token as the same thing
without asking what each one resolves to.
Counterexample: `alias_migration`'s `ns-binding-namespaces`/`reusable?` "conflates namespace aliases
with referred Vars" — with `[example.unrelated :as newlib]` plus `[example.new :refer [newlib]]` it
committed `newlib/fetch-event` although the qualifier resolves through `example.unrelated`, "a silent
semantic mismigration" (`docs/observations/2026-09-07-aliasfix-r2-sol-review.md`, 2026-09-07); the
same class in `bin/preservation-brief`, where planting
`(form-edit-panel event editing cfp-scheduler-killer.views.portal/edit-form)` over a local binding
still reported `preserved_bodies=141` (`docs/observations/2026-09-09-item3-sol-verdict-round-1.md`
PB-FENCE-001, 2026-09-09).
Why unsublime: "same spelling" is not "same referent" in any language with lexical scope, `:refer` or
macro capture, and both tools were silent — no refusal, no warning — until an adversarial probe was
planted.
Sublime counterpart: resolution-aware equivalence, or a conservative refusal whenever a bare symbol
could be lexically bound, referred or macro-captured.
Ratchet: aliasfix landed `b3a8371c → 9b7e22f8`; PB-FENCE-001 held under `HOLD reason=oracle-changed`.
Meter: no meter movement recorded.
Category: code

### 2. A guard that is true by construction
Symptom: a receipt field looks like an independent safety assertion but is computed from a set defined
as exactly the rows that satisfy it.
Counterexample: `rename_alias` computes `other` as `(filterv #(= (:before_sha256 %) (:after_sha256 %)) rows)`
and then asserts `:other_forms_unchanged` as `(every? #(= (:before_sha256 %) (:after_sha256 %)) other)`
— "true by construction — including vacuously, when `other` is empty… It is `true` typed in, wearing a
computation." A wrong-offset mutation turned `(def y events/x)` into `(def y eevx)` while the receipt
still reported `other_forms_unchanged true`
(`docs/observations/2026-09-10-rename-alias-opus-redteam.md` F2, 2026-09-10).
Why unsublime: worse than a missing check — its presence in the receipt manufactures confidence about
exactly the corruption class it cannot see.
Sublime counterpart: define the unchanged set positionally (every root ordinal not in the authorised
changed set) so both the boolean and a refusal rest on an independently observable fact.
Ratchet: `insert-forms-candidate-test` (the sibling verb's fix; not yet ported to `rename_alias`).
Meter: no meter movement recorded.
Category: code

### 3. A control defined by a list is only as complete as the list
Symptom: a scanner, allowlist or type check enumerates the names it knows, and the same hand writes
the code that must not appear.
Counterexample: MEM-003 round 4 — "the scanner enumerates laundering verbs by name; round three
already said a name the scanner does not know is a hole in every scanner at once, and round four
wrote such a name into src twice"; the census's "an allowlist by type admits every subtype"; and
round 6's closing line, "a derivation over names cannot see a call that names nothing. Strings,
positions, and the dot form with a package prefix all reach the number"
(captain's log 2026-09-04 07:17Z, 13:10Z).
Why unsublime: the control's completeness is bounded by the author's vocabulary, and the defect is
always spelled in the vocabulary the author did not think of.
Sublime counterpart: move the control to the boundary — "a number comes out of a Reading in exactly
one place" — and derive the spelling set rather than typing it (round 6: 11 escape routes, 159 clock
spellings derived).
Ratchet: memory `scanner-brief-names-vs-spellings`; memory `marker-presence-audit-is-not-a-ratchet`.
Meter: no meter movement recorded.
Category: code

### 4. The argument parser that evaluates its argument
Symptom: a bound is placed on what a refusal may print while the value is read with a reader that can
execute.
Counterexample: census round 20 — closing a string bound, the builder asked what else `parse-val`
mints and found `clojure.core/read-string` with `*read-eval*` on: `:doors '[#=(println "PWNED")]'`
"printed PWNED while the op was refusing the argument. Arbitrary code from argv, in a public CLI, for
as long as `parse-val` has existed." Now `edn/read-string`, witnessed (captain's log 2026-09-04
07:13Z).
Why unsublime: twenty rounds of bounding refusal TEXT, and the sharpest hole was in how the text was
READ.
Sublime counterpart: bound the value as printed, and read the value as data.
Ratchet: the parsed ratchet with an empty, asserted allow-list (census r23, captain's log 12:00Z).
Meter: no meter movement recorded.
Category: code

### 5. Code carried as a JSON string
Symptom: encoding source as a JSON field to make a request machine-parseable reintroduces the exact
escaping defect the structured interface was meant to remove.
Counterexample: owner-forms cohort T4 "named the correct original owner in its JSON envelope, but its
form string contained literal backslash-n sequences after JSON decoding", two intended docstrings did
not parse as docstrings, the `forms-owner-mismatch` guard refused, and the predicted 4/4 measured
native 4/4, tool **3/4** (`docs/observations/2026-09-06-astra-forms-cohort-result.md`, 2026-09-06).
Why unsublime: "we reintroduced quoting into the model interface" — a wrong escape corrupts the
payload invisibly until parse time, and the first diagnosis blamed owner discovery instead.
Sublime counterpart: "Accept raw complete Clojure definitions and have the kernel map their names to
frozen planned owners. No JSON-encoded code strings, no old whitespace, no model-supplied offsets."
Ratchet: none yet — "a separate pure decoder is being prototyped."
Meter: no meter movement recorded.
Category: code

---

## testing

### 6. Defence in depth hides a missing witness
Symptom: a witness proves the outcome, so removing any one layer leaves it green.
Counterexample: q5z round 16 — "Reverting the allowlist alone left the witness green, because the
guard and the time budget caught the same attack one layer down. Three layers, one witness, and the
witness proved the OUTCOME, never any layer's own contract" (captain's log 2026-09-04 07:09Z).
Why unsublime: the day a layer is removed, nobody learns it — the suite cannot distinguish three
working controls from one.
Sublime counterpart: each layer asserts its own contract; the rule was written into the branch's
intent text.
Ratchet: the q5z intent text on the branch ("each layer asserts its own contract").
Meter: no meter movement recorded.
Category: testing

### 7. No fixture ever reaches the bound
Symptom: a witness polices a ceiling that none of its inputs ever crosses.
Counterexample: admit-gate round 4, reviewer verbatim: "the suite is green because every fixture
produces a receipt below the bound — the witness has never once been handed an input where the bound
it polices actually bites"; the lane had no fixture above the bound for two rounds (captain's log
2026-09-04 07:16Z). The same shape closed in census r15 by mutation: "ceiling removed → fails at
780/728/727/722 bytes" (captain's log 2026-09-03 22:25Z).
Why unsublime: a bound with no fixture at or above it is an untested constant with a test's
reputation.
Sublime counterpart: fixtures AT the ceiling and one above it; the witness turns red when the ceiling
is removed.
Ratchet: memory `a-bound-must-degrade-never-delete` (witnesses at 200/201/oversized).
Meter: no meter movement recorded.
Category: testing

### 8. A witness driven only through the tamper seam
Symptom: the RED input is manufactured by the test rather than produced by the production path, so
the fix closes the manufactured case and breaks the real one.
Counterexample: MEM-003 round 6 confirm, verbatim: "on an untampered tree containing a directory
`src/mydir.clj`, 0914a37 page 3 served [fixt.m11 fixt.m12] and 3cedd44 page 3 refuses
`:unconfined-manifest-row` … a false statement, two innocent files lost, pagination permanently
unfinishable"; "the suite is green at 778/6322/0 while the untampered case is broken; the new refusal
needs a test on a tree where DISCOVERY produced the row." The brief (the seat's own) "said 'RED
witness first' but let the builder pick the tamper seam" (captain's log 2026-09-03 19:22Z).
Why unsublime: a witness bound to the seam proves the seam, not the behaviour.
Sublime counterpart: RED must be an input the production path produces; every confinement witness
re-done on a DISCOVERED row.
Ratchet: memory `witness-through-the-production-path`.
Meter: no meter movement recorded.
Category: testing

### 9. A witness lane no landing gate runs
Symptom: a real failing test exists, and nothing on the path to trunk executes it.
Counterexample: `registers-one-admit-tool-in-the-full-profile` expected nine tool names and the full
profile had ten since `namespace_split` landed; "that witness lives only in the battery lane, which
no landing gate runs (battery-fresh checks distance, not results), so trunk was red for two hours
with every landing gate green" (captain's log 2026-09-08 02:54Z).
Why unsublime: "distance ≤ 30" is a freshness proxy, not a verdict — the gate was measuring how old
the receipt was, not whether the tests passed.
Sublime counterpart: a landing that adds a tool runs the registry witnesses; land runs the affected
battery namespaces when `tool.clj` changes.
Ratchet: inb-45b82d.
Meter: trunk — the 9 of 2026-09-09 08:16Z rests on "gates consumed at landing" and derived censuses,
both of which came out of this class of finding (log 2026-09-12T01:54Z).
Category: testing

### 10. A single-threaded witness for a concurrent mechanism
Symptom: hand-planted fixtures pass green while the defect they exist to catch needs concurrency to
exist at all.
Counterexample: the transaction kernel — "the previous invariant test:1744 was correct and never
fired, because no scenario had a concurrent deleter"; a 6×20 storm through the public verb found
"188 of 240 lines typing `:evidence :retained` for a file that is not there" and 40 minted orphan
sidecars, closed to 0/240 and 0 orphans by the same storm
(`docs/observations/2026-09-03-txn-journal-round9-rereview-opus.md`, 2026-09-03).
Why unsublime: a green single-threaded suite for a lock/tombstone/journal is evidence that the suite
never generated the race.
Sublime counterpart: the storm goes into the permanent suite — the reviewer's own line, "the defect
was found by a storm and is verified by a storm only in this review. This is the ratchet I'd build
first."
Ratchet: kernel round-9 storm ratchet (adoption-scoped follow-up item 8).
Meter: storm 5a2d254 → 2df05b3: 188/240 lying lines → 0; 40 orphans → 0.
Category: testing

### 11. Assertions inside a conditional that guards the failure
Symptom: a test wraps its assertions in `when (failure?)`, so an unexpected success skips the body and
the test still passes.
Counterexample: `f2-refusal-locator-names-a-line-not-a-docstring` "wraps all assertions in
`when (= false (:ok plan))`; it passes vacuously if the planner unexpectedly succeeds"
(`docs/observations/2026-09-07-aliasfix-r2-sol-review.md` finding 4, 2026-09-07) — inside a round
reporting 183 tests / 3,953 assertions / "0 failures, 0 errors".
Why unsublime: the test reports the same green whether or not the refusal it is named for ever fires.
Sublime counterpart: assert the precondition first (the plan actually refused), so a regression that
makes the code succeed turns the test red.
Ratchet: none yet.
Meter: no meter movement recorded.
Category: testing

### 12. Tests that encode the defect as a requirement
Symptom: the existing suite asserts the buggy behaviour, so fixing the bug turns tests red and the
builder is tempted to "fix" the fix.
Counterexample: q5z round 5 — "Two existing tests had encoded Sol's defect as a requirement
(fabricated peers with `spit`, asserted they got pruned) — premise corrected through the production
writer, assertions unchanged" (captain's log 2026-09-03 06:47Z).
Why unsublime: a suite can be a record of what the code does rather than of what it must do, and the
difference is invisible until an outside reviewer attacks the behaviour.
Sublime counterpart: correct the test's PREMISE through the production path and leave its assertions
alone, so the change is visibly about how the state was reached, not about what is promised.
Ratchet: ALIAS-045/047/052/054 amended in the same round.
Meter: no meter movement recorded.
Category: testing

### 13. A strict parser that counts cells and never reads them
Symptom: a validator checks record shape and calls that strict.
Counterexample: `parse-refusal-table` "validates only the number of pipe-delimited cells. It does not
validate that any of the four cells is nonblank" — `| base | src/a.clj | ns-count |  |` parsed clean;
mutating the real producer to emit a blank `why` left "the 384-map property and all five behavioural
drift controls" exiting 0 and printing PASS, "because both the JSON projection and the parsed
Markdown projection deliberately omit `why`; no assertion observes the blank cell"
(`docs/observations/2026-09-09-item3-sol-verdict-round-17.md` PB-FENCE-024, 2026-09-09).
Why unsublime: the tool's stated product claim (blank rows are reported as errors) is never exercised
by its own green suite.
Sublime counterpart: assert every required cell's non-blankness and add the blank-required-cell
mutation as a permanent drift control, as was already done for duplicate and reordered rows.
Ratchet: PB-FENCE-024 (held under `bin/`, `HOLD reason=oracle-changed`).
Meter: no meter movement recorded.
Category: testing

### 14. Adversarial SHAPES in the battery, derived from the real failure (sublime)
Symptom (of its absence): every arm scales one dimension, and the defect is in another.
Counterexample: the memory battery's adversarial arms paid off on their first run — "cli-ls-tree
peaks 386.4 MB on ONE 1.9 MiB file and 285.7 MB on ONE 300-deep 111 KB file (1,322 MB at 4g) vs a
248 MB budget — heap sized by a file's SHAPE, invisible to any tree-scale arm" (captain's log
2026-09-03 06:25Z); the ceilings that followed were derived from a cold failure ladder, not guessed:
"depth 150 (cold ladder: 440 completes, 460 lowest StackOverflow; deepest real source 22 — 6.8× above
real, 3.07× below the crash); nodes 200,000" (06:53Z).
Why sublime: the arm set is derived from the mechanism's failure modes, and every ceiling is stated
with its margin in both directions.
Anti-pattern it prevents: a scaling battery that is green at 10,000 files and dies on one file.
Ratchet: MCP-OP-MEM-005 parser admission (B3, `bridge/parser-admission` 8a55dbc).
Meter: cli-ls-tree giant 386.4 → 33.4 MB / 2,058 → 27 ms; nested 285.7 → 24.6 MB / 784 → 10 ms.
Category: testing

### 15. Replicate the failure first, and give the heavy proof its own target (sublime)
Symptom (of its absence): a fix whose green was never preceded by a red in the same scenario.
Counterexample: Gene, verbatim: *"Use TDD style; replicate OOM first."* and *"Make sure to write a
test (not a unit test) that confirms we don't OOM; don't want to slow the make run tests too much,
tho!"* The order was fixed in the brief: commit 1 = a subprocess test at `-Xmx256m` reproducing the
OOM (RED cc04af6: `Terminating due to java.lang.OutOfMemoryError`, 8-file positive control exit 0);
commit 4 = the same scope GREEN at the same `-Xmx256m`, 600 files, retained 14.19 MB, three-way
digest parity; `make memory-battery` asserted OUT of `make test` with a self-test proving its absence
from the closure (captain's log 2026-09-03 04:17–04:20Z, 06:37Z, 04:50Z).
Why sublime: the red is the same scenario under the same limit as the green, and the expensive proof
cannot silently enter the fast lane.
Anti-pattern it prevents: a "fix" for a failure nobody ever reproduced, and a slow proof that either
rots outside the gate or taxes every run.
Ratchet: memory `tdd-replicate-the-failure-first`; memory `no-oom-proof-is-a-battery`; MCP-OP-MEM-001/002.
Meter: no meter movement recorded.
Category: testing

---

## measurement

### 16. Reading the window as if it were the trunk
**DELETED 2026-09-12 by Astra's refutation (see review-astra-2026-09-12.md): the receipt does not support the entry.**

### 17. A meter bound to one transport
Symptom: an adoption counter reads zero because it can only see one of the entrances.
Counterexample: the token study concluded "Astra's own session contains ZERO Surgeon MCP calls …
across all 215 Codex rollouts, 192 carry the Surgeon tool list and 0 contain a Surgeon MCP call."
Correction 40 minutes later: "Astra DOES use Surgeon, through the CLI entrance, not MCP — his
coordinator session carries 263 `bin/mission` and 33 `typist-run` invocations (via exec) … So 'zero
Surgeon use' is wrong; 'zero MCP use' is right." A second correction the same hour: the 192
"exposed" rollouts "carry catalog TEXT (tool descriptions), not a callable tool inventory"
(captain's log 2026-09-06 07:23Z, 07:33Z).
Why unsublime: exposure is not availability and one transport is not the tool; a zero from a partial
meter is indistinguishable from a zero from the field.
Sublime counterpart: name the transport in the claim, and count every entrance the caller could have
used before saying "adoption".
Ratchet: inb-4c3d0e (caller tokens per mission in the ledger).
Meter: usage watch, flat at `mcp_tool_calls 96, ok 49, refused 47` for ~17 hours while provider
counters climbed claude 135→181 / codex 543→693 — "a contaminated denominator, not a measure of
adoption" (inb-46f90f).
Category: measurement

### 18. Zero exposure scored as a behavioural null
Symptom: an intervention that never reached the subject is reported as an intervention that did not
work.
Counterexample: the Claude gate-vs-prompt cohort's hooks H/H0 fired on 1–2 of 13 tasks per caller,
and a blind audit found none touched a task's source file — all three firings were on scratch fixture
files. "In 156 runs neither caller ever attempted shell string-surgery on a task's Clojure source.
The intervention H and H0 exist to catch was never attempted, so H − H0 measures the effect of a
message delivered only against fixture-building"
(`docs/observations/2026-09-11-gate-vs-prompt-report-claude.md`, 2026-09-11).
Why unsublime: "the model decided" and "the deployed hook did not reach this caller" are different
facts, and only one of them is in the data.
Sublime counterpart: report exposure first and separately; a near-zero exposure count voids the
effect estimate rather than explaining it.
Ratchet: "An agent cannot respond to a refusal it never receives | exposure reported first in every
hook experiment; H0 control" (`2026-09-11-gene-report-morning.md`).
Meter: window 8→9 on 09-11 includes "the pre-registered bets scored in public, mine lost" — both
Fable H bets missed; Astra 11/12 held.
Category: measurement

### 19. A confound that reaches back through a week of arms
**DELETED 2026-09-12 by Astra's refutation (see review-astra-2026-09-12.md): the receipt does not support the entry.**

### 20. A broken oracle flattens every arm into a clean null
Symptom: an instrument that fails identically on all arms produces a result that looks exactly like
"no effect".
Counterexample: "8 of the 12 frozen `:gate` commands do not run at their own `:sha` on the untouched
tree, so their runs are `unrunnable`: UNKNOWN, never accepted." Under gate v1 every one of five arms
for both callers scored exactly 25% (3/12). Re-scored with a working gate: 42–50% per caller/arm and
a measurable AC−B of "+8 pp" for one caller (`docs/observations/2026-09-11-cohort-report-v1.md`,
`…-v2.md`, 2026-09-11). The same night: "16 runs had been hiding in `unrunnable`, in the direction
that flatters the arm; now red, marked `broke_the_gate`" (captain's log 2026-09-11 01:53Z).
Why unsublime: a null from a dead instrument is the most expensive result there is — it looks like
knowledge and it ends inquiry.
Sublime counterpart: when a gate is unrunnable at a majority of sampled commits, every affected cell
is unscored; publish v1 and v2 side by side rather than substituting silently.
Ratchet: the curator's replacement gate (v2), reported beside v1.
Meter: v1 flat 25% across all arms → v2 42–50% with +8 pp AC−B on one caller.
Category: measurement

### 21. A term that means different things in the two arms
Symptom: both arms report the same endpoint and the endpoint is not the same event.
Counterexample: row 5 D1 — "candidate-complete 341 s vs native floor 317 s — the apparatus asymmetry
Astra forecast: the verb's in-call verification (the ~229 s Cell B oracle) is counted as the D
caller's work while N callers stamp before their own suite." The fix, disclosed in the freeze: run D
with the in-call profile replaced by a no-op and have the RUNNER grade both arms with the same frozen
oracle (captain's log 2026-09-08 10:43Z).
Why unsublime: the tool was charged for proof the native arm had not yet paid for; the ratio was a
definition, not a measurement.
Sublime counterpart: "'candidate-complete' must mean the same thing for a verb that proves inside the
call and a caller that proves after — proof by the runner, for both, on the primary clock."
Ratchet: the D2–D6 no-op-profile design, disclosed in the freeze.
Meter: D1 341 s vs native floor 317 s, kept as a labelled proof-inclusive diagnostic, not a verdict.
Category: measurement

### 22. A verifiable negative invites verification
Symptom: a receipt is improved by adding honest negatives, and the metric chosen to prove the
improvement moves the wrong way.
Counterexample: row-5 E2 — "X4 went 10 → 18 because it accepted every negative and then independently
RE-DERIVED them (body diff of all 25 owners, a reflective visibility check, two fresh-process loads)";
the paired inspection ratio missed its gate at 0.81 (gate ≤ 0.50) although the inspection LEVEL hit
the prediction (median 5 vs E's 9 vs D's 12). E3's follow-up found the other half: "two arms rose
against pairs already at 2–3 reads — the paired ratio is measuring arms with no floor left"
(captain's log 2026-09-08 22:44Z, 2026-09-09 00:45Z).
Why unsublime: "fewer inspections" is the wrong meter for a change whose purpose is to make a claim
checkable, and a ratio against a floor measures the floor.
Sublime counterpart: split the meter — inspections the receipt could have answered vs corroborations
of a receipt claim (kept, not penalised) — and apply the classifier retroactively to the retained
transcripts.
Ratchet: the E3 split meter; the retroactive reclassification of E2's transcripts.
Meter: E2→E3 answerable inspections 29 → 24 (paired 0.83, gate ≤ 0.5); wall median 156.6 → 141.8 s
(the row's first wall win, 0.45 of native).
Category: measurement

### 23. The pre-registered withdrawal line that fires (sublime)
Symptom (of its absence): a losing square is re-narrated as "the task was too easy".
Counterexample: three in one week. E6-Q2: "the pre-registered decision fired: SQUARE 3 WITHDRAWN for
this caller (N 6/6 in 3/3; written before the first arm so a second null could not be re-narrated as
'too easy')" (`docs/observations/2026-09-03-gene-report-night.md`). Arm G: "WITHDRAWN as
pre-registered: the gate as the write path is a 2.6–2.8× wall LOSS in its current contract"
(captain's log 2026-09-04 20:15Z). The ceremony-free pair: "the pre-registered withdrawal line fired:
ceremony was NOT the diluter (native's total did not fall; the receipt arm's rose)" (08:53Z).
Why sublime: the stop condition is written before the data exists, so a null costs one cohort instead
of a week of re-framing.
Anti-pattern it prevents: running a square until the metric that failed looks favourable.
Ratchet: the withdrawal clauses in each pre-registration; Gene's wall-clock rule in `vision.md`
("2x on wall or withdraw", captain's log 2026-09-04 16:55Z).
Meter: E6-Q2 correctness gap [0,0,0]; mandated inspect cost "3.3–6× the actions for nothing".
Category: measurement

### 24. A zero from a broken meter is not a zero
Symptom: a collector fails and its empty output is published as a measurement.
Counterexample: "CORRECTION (the previous usage line was empty: a quoting error in my extractor, not
a collector zero)" (captain's log 2026-09-03 05:50Z); then the collector itself, nine consecutive
times: "usage watch: collector TIMED OUT at 120 s (rc 124, third consecutive, load 10.9); no
figures — ledger inb-65c941" (2026-09-04 07:51Z and following). The collector is a JVM competing for
the box it is measuring; on 2026-09-05 it was DEFERRED entirely while a peer's timed controls held a
quiet window.
Why unsublime: a meter that can fail silently converts "we do not know" into "it is zero", and the
zero is the number people remember.
Sublime counterpart: print "no figures" with the exit code and the load; file the ledger item; give
the collector its own lane or defer it while someone else's clock is running.
Ratchet: inb-65c941 (warm-path collector); the quiet-window protocol (owner/start/end/purpose).
Meter: no meter movement recorded.
Category: measurement

### 25. A hand-counted table beside a fold (sublime, by losing)
Symptom: the narrative's own numbers are hand-derived from log files and nobody can re-derive them.
Counterexample: Block 3's red-team, verbatim: "`board --report` DISAGREES WITH THIS LOG and the log
is wrong — at the same 44 h window the fold reproduces the median 436 s and the 423–846 range exactly
but counts 30 distinct ship runs / 7 landed / 4.14 launches per landing where the entry 'Sublime
index from the logs' wrote 36 / 8 / 4.5 (hand-counted from log files, some of them duplicates of one
run). CORRECTION: 30 runs, 7 landed, 4.14 launches per landing … This is exactly the hand-maintained
table the plan says Block 3 retires, and the best argument in the packet for installing it"
(captain's log 2026-09-11 23:30Z).
Why sublime: the correction ran against the seat's own published reading and the reading's verdict
(4) was retained with its reason, because it rested on quantities the fold did not move.
Anti-pattern it prevents: a status table only its author can reproduce, quoted onward as fact.
Ratchet: `board --report` / `board --meters` folding from the production ledger (ship v3.11,
2026-09-12 00:48Z), with a COVERAGE line naming what remains uncovered.
Meter: skills' index 4 (Fri), "unchanged by design — the skills refuse credit for encounters nobody
registered."
Category: measurement

### 26. Re-score the retained bytes; never re-run for a detector bug (sublime)
Symptom (of its absence): an instrument defect costs the whole cohort.
Counterexample: "Detector defect found on both seats: Codex reports MCP use as `McpToolCall` items,
not `function_call` names; fixed from the retained rollout, no new probe (Astra: 'fix accounting from
retained call/result')"; the ratchet landed as "a fail-first fixture cut from the retained bound
rollout (3 structural lines) — legacy outer-name parse 0 (blindness proved on the exact bytes), new
parser 1" (captain's log 2026-09-06 09:41Z, 09:43Z). Same practice on the Codex side of
gate-vs-prompt: a classifier read Codex's rollout as JSON when "it is JavaScript with unquoted keys,
and it filed 41 of 41 real calls as 'other-tool'"; all 130 runs were "re-derived under the corrected
classifier (`rederive.sh`); no run was re-executed and no oracle or gate verdict moved"
(`docs/observations/2026-09-11-gate-vs-prompt-report-codex.md`, 2026-09-11).
Why sublime: raw evidence kept separate from every parsed projection turns an instrument bug into a
re-fold instead of a re-run, and the blindness is proved on the exact bytes that fooled it.
Anti-pattern it prevents: spending a cohort's budget to fix the cohort's reader.
Ratchet: `rederive.sh`; the detector's fail-first fixture cut from the retained rollout.
Meter: 41/41 calls misclassified → correctly classified with no verdict movement.
Category: measurement

---

## review

### 27. Seventeen rounds is a selection failure, not a fence failure
Symptom: a prototype absorbs round after round of real findings, each smaller than the last, and
nobody asks whether the claim can be certified at all.
Counterexample: item 3 reached Sol round 17 on one branch — "Seventeen holds, every one a real edge,
the last five progressively smaller (sets vs sequences, a probe's call site, a redefinition later in
the file): the tool is converging, but a prototype absorbing seventeen review rounds is a selection
failure, mine, not a fence failure." Astra, asked the design question once from the top, parked it in
two hours with six valid specimens "whose observable value or generated interface changes while the
brief reports `clear:true`, exit 0, and a positive preserved count" (captain's log 2026-09-10 02:04Z,
03:14Z).
Why unsublime: one edge found per round and one edge fixed per round is a loop that terminates only
when someone changes the question.
Sublime counterpart: "the strongest rung first, and the first question is whether the claim can be
certified at all"; a round cap named IN the brief, after which the branch parks and the question goes
back to the planner.
Ratchet: the round-cap clause added to the checker-rung memory rule (2026-09-10 02:04Z).
Meter: window 7 → 6 on Tue 09-09/10 — "review rounds started from the wrong rung, parked at caps"
(log 2026-09-12T01:54Z); "no landing since 3ea3803e at 08:16Z."
Category: review

### 28. A verdict that does not name its base
Symptom: a GO is quoted forward after the base it was earned against has moved.
Counterexample: the mayor, verbatim: "rf2-extract-rewire 965d49e IS STALE-GO. HOLD IT. Its GO was
earned against a main that no longer exists. Measured, my hands, twice: main at 46a18041 is GREEN
(run_all 709/5945/0). main + receipt-ratchets alone is GREEN (727/6051/0). main + rf2 ALONE fails
`a-compile-failure-after-apply-is-reported-with-undo` (extract_test.clj:1298) … I first suspected box
load … and disproved it by baselining clean main at the same load and getting zero." General lesson,
his: "a GO is a claim about a base. Main moved four times tonight" (captain's log 2026-09-03 22:08Z).
Why unsublime: a verdict with no base is a fact about a tree nobody has any more, and the plausible
alternative explanation (load) will be reached for first.
Sublime counterpart: every approval conditioned on an executed re-review against CURRENT main before
the merge — which is exactly how the seat's delegated approvals were written the same hour.
Ratchet: memory `go-with-fix-is-not-go`; inb-2f78f5.
Meter: no meter movement recorded.
Category: review

### 29. A scope-disciplined GO that still names what it found outside the diff
Symptom: an out-of-scope defect either blocks unrelated work or disappears into a clean pass.
Counterexample: MEM-003 round 8 returned "GO — merge 95b0881" while naming OPEN-A verbatim as
"ANDON-class and must be filed against main NOW, not queued behind this merge", reproducing the shell
injection live ("marker before: false / marker after : true <- my touch executed") and closing with
"a bare 'GO' that lets OPEN-A ride quietly is the failure mode I am naming out loud"
(`docs/observations/2026-09-03-mem-003-round8-rereview-opus.md`, 2026-09-03).
Why sublime: "should this branch merge" and "does a defect exist" are separated, so good work merges
and the finding gets its own escalation.
Anti-pattern it prevents: a pre-existing bug buried inside a passing review nobody re-reads.
Ratchet: inb-d27b79 (the Andon incident this finding triggered); memory
`andon-loop-worked-escalate-direct`.
Meter: pull-to-lift 34 minutes (20:26Z pull → 21:00Z merge a6df86ee).
Category: review

### 30. A reviewer that will not waive a gate it does not own
Symptom: the fix is right, an unrelated gate is red, and the reviewer quietly decides it does not
count.
Counterexample: aliasfix r4 — every requested defect NOT-REPRODUCED, and `~/bin/clj-kondo --lint`
reporting "0 errors, 2 warnings; exit 2" on warnings that "predate this branch and appear identically
on `origin/MCP/main`". Verdict: "If the two baseline lint warnings are explicitly waived, the
functional verdict is GO. Under the gates as written, it remains NO-GO"
(`docs/observations/2026-09-07-aliasfix-r4-sol-review.md`, 2026-09-07).
Why sublime: the waiver is named and left to its owner instead of being absorbed into the verdict.
Anti-pattern it prevents: a reviewer deciding a gate does not apply this time — functionally the same
as letting a branch turn its own gate off (memory `a-gate-a-caller-can-turn-off`).
Ratchet: memory `a-gate-a-caller-can-turn-off`.
Meter: no meter movement recorded.
Category: review

### 31. The oracle is never certified by the pass that found its bug (sublime)
Symptom (of its absence): a reviewer patches the grading tool mid-review and re-approves against its
own fix.
Counterexample: across item3 rounds 1, 9 and 17 the same clause fires — "That repair changes `bin/`,
which the ship-fix contract forbids a reviewer from patching, so it cannot be closed as
`GO-WITH-FIX` here" (round 1); "`HOLD reason=oracle-changed`; I did not patch it" (round 9); "I
therefore left no `GO-WITH-FIX` patch" (round 17)
(`docs/observations/2026-09-09-item3-sol-verdict-round-1/9/17.md`, 2026-09-09). The same contract
also requires an INDEPENDENT delta reviewer for any reviewer-authored patch touching
src/tests/fixtures/config/spec — Astra's amendment: "a reviewer-authored patch applied by the
pipeline is NOT a review of itself" (captain's log 2026-09-08 21:24Z).
Why sublime: the entity whose correctness is in question is never self-certified by the session that
questioned it.
Anti-pattern it prevents: an oracle whose soundness is untestable by anyone outside one session.
Ratchet: ship v3's `HOLD reason=oracle-changed`; `~/bin/independent-review` (a fresh Claude, not Sol;
printed `INDEPENDENT: GO` in 22 s on a real batch-4 patch with six file:line justifications).
Meter: skills' index 3→4 on Thu, from "three consecutive one-launch landings once the prewarm rule
was applied."
Category: review

### 32. The reviewer who concedes on an executed reproduction (sublime)
Symptom (of its absence): a dispute between builder and reviewer settled by authority.
Counterexample: Sol conceded twice, in writing: "My earlier depth-first rule would prefer a tiny deep
subtree and was wrong; largest first, then deepest, then lexicographic is the useful deterministic
rule" (q5z r4, captain's log 06:21Z) and "my earlier sampled-peak hard line was wrong for small-heap
G1" (kernel review, 07:08Z). The traffic runs both ways: Sol "DISPROVED the builder's impossibility
claim: a 296-char prefix-narrowing `next_call` selected 141 files / 267,900,000 bytes under the
268,435,456 ceiling" (05:07Z), and the study builder "corrected two of the reviewer's figures with
real bytes (largest atomic result 22,141 not 28,168; floor 38 not 40)" (05:40Z).
Why sublime: every disputed claim is settled by an executed reproduction on the tree, and either side
may lose.
Anti-pattern it prevents: a review round spent arguing, or a builder's correct refusal overruled.
Ratchet: memory `send-the-dispute-back-with-a-reproduction-demand`.
Meter: no meter movement recorded.
Category: review

### 33. A reviewer whose filter refuses your vocabulary
**DELETED 2026-09-12 by Astra's refutation (see review-astra-2026-09-12.md): the receipt does not support the entry.**

### 34. A refusal that names the wrong cause
Symptom: a typed refusal is well-formed, complete, and points at the wrong thing.
Counterexample: `alias_migration` with `scope.paths ["src"]` refused `alias-migration-empty-scope`
with "No namespace under scope requires acid.fanout.store" — a domain sentence — when the real
problem was that `scope.paths` are globs and `"src"` matched zero files; 4 refusals in 7 verb calls
(57%) against a ≤20% falsifier, and the caller guessed the corrected glob on its next turn all four
times (`docs/observations/2026-09-04-e3-p-cohort.md`, 2026-09-03). Census r15 names the general form:
"Three lies in one receipt: the adapter is blamed for a permission bit, the remedy invites a smaller
retry against a bound that refuses identically at any size, and the good source is dropped because
the narrowing branch was never reached" → GREEN `unreadable-source-path` with the file named
(captain's log 2026-09-03 22:25Z).
Why unsublime: a refusal that names a plausible wrong cause costs one model return per occurrence and
teaches the wrong lesson every time.
Sublime counterpart: state the discriminating fact in the same sentence as the cause ("`src` matched
0 files; did you mean `src/**`?"), and choose a refusal TYPE over a type-plus-`:cause` field — "a
caller who must read a second field to learn which remedy applies has been handed a branch dressed as
a type."
Ratchet: `bridge/q5z-alias-migration 0085db8` (bare directory treated as its subtree;
`scope-matches-nothing` its own typed refusal); census CENSUS-027..031.
Meter: after the fix "the cohort's exact request commits in ONE call, byte-identical to canonical,
6/6."
Category: refusals

### 35. A remedy that prescribes the same refusal
Symptom: the receipt's suggested retry reproduces the failure it is answering.
Counterexample: `alias_migration` with `"verify": "fast"` refused `invalid-diagnostic-output` with
remedy "Re-send the same alias_migration request; the frozen snapshot is recomputed from current
source." The caller "obeyed the remedy verbatim and got the identical refusal a second time
(246.53 ms)", succeeding only by dropping `verify` entirely — "something the remedy never suggested".
That exchange cost 484 of the arm's 708 emitted characters and dropped the pair's ratio from ~3.5× to
**1.40×** (`docs/observations/2026-09-04-ecaller-cohort.md`, 2026-09-04).
Why unsublime: "a remedy string that prescribes an action which reproduces the same refusal is a
defect in the receipt, not in the caller."
Sublime counterpart: every remedy is executed against its own fixture before it ships; if the replay
reproduces the refusal, it is not a remedy. Census r15's ratchet is the same idea from the other
side — a refusal that spells its own map fails with "published a next_call the constructor did not
build."
Ratchet: the one `continuation` constructor at all eight sites (census r15); replay-proven
`next_call` by byte-compare (q5z r3).
Meter: pair ratio 3.5× → 1.40× on that arm.
Category: refusals

### 36. One refusal ends "encouraged" adoption
Symptom: a correct refusal is the last interaction the caller has with the tool.
Counterexample: the "Surgeon encouraged" caller D1 generated a native patch, sent it once to
`admit_clojure_patch` with `verify=none`, was refused `verification-incomplete`, then "passed the
same stored patch to native `apply_patch`… There were zero ordinary batch-edit calls and zero sites
committed by Surgeon." Complete wall 87.680 s vs native-only N1 at 67.262 s — "native-only completion
after a refused admission, not mixed migration or successful tool adoption"
(`docs/observations/2026-09-06-astra-recovery-pilot-result.md`, 2026-09-06).
Why unsublime: a gate whose correct refusal ends the interaction makes the encouraged path strictly
worse than not offering the tool — slower AND unadopted.
Sublime counterpart: fit the gate to the patch workflow the caller already has, with proof authority
ready to use; the fresh-caller finding says the same thing — "adoption = verification binding, never
a weakened gate or a second editor" (memory `fresh-caller-wants-a-gate-not-a-second-editor`).
Ratchet: memory `fresh-caller-wants-a-gate-not-a-second-editor`.
Meter: adoption after refusal 0 of 1; D1 87.680 s vs N1 67.262 s.
Category: refusals

### 37. Presence or shape checked; the bound value never verified
Symptom: a gate accepts any syntactically valid value for the authority field it exists to enforce.
Counterexample: replacing a fixture's "first selected digest with `nil`", replacing
`:candidate/:inputs-manifest/:sha256` "with 64 `f` characters", and replacing every populated fixture
census with the unrelated singleton `["not/a/real-fixture"]` each still produced
`DECISION consume discharged=R1,R2,R3,R4,R5,R6,R7,R8` — "The consumer checks only
`(seq (:fixture-identities e))`" (`docs/observations/2026-09-09-item1-sol-verdict-round-3.md`
SOL-EC-007/009, 2026-09-09). The same class at the other end: arm G's `admit_clojure_patch` gate was
preflighted with E4's actual bytes and "the detector does NOT reject E4's known-wrong candidate
(ADMITTED)" (`docs/observations/2026-09-11-gate-vs-prompt-report-claude.md` §6, 2026-09-11).
Why unsublime: a structural check standing in for a value binding makes the gate's whole promise
unfalsifiable — and the gate's presence is what stops anyone from checking.
Sublime counterpart: verify the bound value (the same gate's required-input rule is the right shape:
"replacing a required input's digest with a different valid SHA-256 refused `inputs-mismatch`"), and
replay the named historical incident's actual bytes through every gate built to catch it, reporting
the result whether it passes or fails.
Ratchet: SOL-EC-007/009 (open); the E4 preflight replay as a standing positive control.
Meter: no meter movement recorded.
Category: refusals

---

## receipts

### 38. The rendered text is a strict subset of the structured receipt
Symptom: two callers receive the same response and only one of them can act on it.
Counterexample: on the empty-scope refusal, `structuredContent.next_call` was "complete and executable
(corrected globs `["src/**","test/**"]`, `expect.files 21`, `workspace_root`, the arm's own
`refer_policy`)" and T-1/T-3 executed it verbatim in 3.3 s — "but the text block rendered only the
domain sentence and dropped the remedy and the discriminating fields (`found_files 0`,
`scanned_files 0`); only T-2, reading the text, was fooled"
(`docs/observations/2026-09-04-e3-p-cohort.md`, correction appended 2026-09-03).
Why unsublime: the common case (a text-reading client) is systematically worse informed than the
JSON-parsing one, from the same bytes.
Sublime counterpart: text ⊇ structured, per verb, with a completeness witness.
Ratchet: `bridge/q5z-alias-migration 0085db8`; memory
`text-block-must-carry-the-structured-receipt`; `fable/refusal-text-shape`.
Meter: after the fix the same request "commits in ONE call, byte-identical to canonical, 6/6."
Category: receipts

### 39. A required check that could not run, rendering as OK
Symptom: an absent measurement is displayed in the same shape as a passing one.
Counterexample: the grader's receipt-bound check prints
`receipt_check=<ok|miss|unavailable|foreign|forged>`, and "`unavailable` renders N/A, never OK (the
F9/F10 lesson on a required check)" — so an 11-bundle regrade produced 0 verdict changes; the miss it
does catch reads `note: R12.receipt_bound MISS agent=231/2258/0 receipt=230/2258/0 … reason=totals_mismatch`
(captain's log 2026-09-08 17:55Z). The same principle on the meters: "the meters print independence
self-issued and protected completion unknown, which is the honest reading until the mayor's account
issues receipts" (2026-09-12 00:48Z).
Why unsublime: a required check that renders OK when it could not run converts a gap in evidence into
a claim of evidence.
Sublime counterpart: three states minimum — pass, fail, and could-not-run — with could-not-run never
rendering as pass, and unknown printed where the recorder is absent.
Ratchet: R12.receipt_bound + F14.claimed_pass_with_red_receipt; `board --meters` unknown states.
Meter: skills' index held at 4 because "daily and fleet and independence remain 0/unknown because
nothing is registered ahead of time and nothing is independently recorded."
Category: receipts

### 40. A number about neither of two populations
Symptom: a receipt publishes one count over a set that is actually two different things.
Counterexample: kernel round 9 — "`:interrupted 5` → `:interrupted-corroborated 2` /
`-uncorroborated 3` (the combined key removed: 'a number about neither of two populations is not
worth publishing')" (captain's log 2026-09-03 22:08Z).
Why unsublime: the combined number is unusable for any decision either population would drive, and
its existence suppresses the question.
Sublime counterpart: split the key; if the split cannot be computed, publish neither.
Ratchet: the kernel's resolution keys.
Meter: no meter movement recorded.
Category: receipts

### 41. The headline count printed before the section carrying the signal
Symptom: the encouraging aggregate is at the top and the only evidence of a defect is further down.
Counterexample: "the brief prints 'Mechanically preserved bodies: 141 of 141' before a long A
section. The load-order signals appear only starting at B4, and the machine summary has no overall
unresolved/fail state. A reviewer can consume the preservation headline and skip the only signal" —
of six planted defects, "silent promotion and both load-order variants were caught only in B"
(`docs/observations/2026-09-09-item3-sol-verdict-round-1.md` PB-FENCE-003, 2026-09-09).
Why unsublime: report order is part of the interface; a reader (or a machine consumer) stopping at
the top-line count reaches the opposite conclusion from the evidence.
Sublime counterpart: hoist every hard-stop signal into a top-of-report machine-readable non-clear
state before any aggregate — by round 17, "`silent-promotion` puts `9 PRIVACY CHANGES` in the top
hard-stop banner… The banner precedes section A."
Ratchet: PB-FENCE-003 (closed by round 17's banner).
Meter: no meter movement recorded.
Category: receipts

### 42. Exit 0 read as publication
Symptom: a write is treated as delivered because the command that attempted it returned zero.
Counterexample: the records-lane squeeze found five things the brief did not anticipate, among them:
"v3.7 with `RECORDS_DEST=MCP/main` published records onto the CODE ref and printed OK; a remote that
accepted and dropped the ref returned OK (exit 0 was being read as publication)". v3.8 added "bound
destination (exit 7), bound source branch (6), no `src/` in the pending delta (8), per-checkout flock
(9), rebase-and-retry, and readback before OK (10, UNVERIFIED)" (captain's log 2026-09-10 07:07Z).
The same class caught in landing: "FALSE RECEIPT caught: `land 7e462745` printed 'LANDED … as
10dc7709' but the merge was a NO-OP — Astra's tip was already an ancestor of MCP/main … so the gates
ran on an unchanged tree and trunk still lacked his changes (diff vs his tip: 5 files, −122)"
(2026-09-06 15:30Z).
Why unsublime: an exit code is a fact about a process, and delivery is a fact about a remote; the
report says the first and means the second.
Sublime counterpart: read the thing back from the destination and bind the named blob; refuse a tip
that is already an ancestor (exit 7) and a merge that changes nothing (exit 8).
Ratchet: `records-push` readback-before-OK; `land` exits 7 and 8; house rule 18 (test delivery, not
identity).
Meter: blocked records publication median 660.417 s (n=6, SD 0.046) → 0.150 s, "0 s claimed off any
ship."
Category: receipts

---

## apparatus

### 43. A fail-open scorer certifies an incomplete run
Symptom: the scoring layer of a measurement apparatus is lenient about exactly the corruptions that
would make its numbers meaningless.
Counterexample: "[score.py:42] malformed lines are silently skipped; a truncated final record scored
with rc=0, while duplicated and fully reversed rollout/watch streams also produced receipts with
`sources.agree=true`"; "[score.py:73] an empty `watch.jsonl` produced rc=0 with zero metered returns,
and `attest_ok=false` also produced rc=0 and a receipt"; "[watch.py:456] a return followed by a tool
call with no result was flushed as `no-output` … and remained a citeable receipt rather than an
incomplete-run refusal"
(`docs/observations/2026-09-03-anvil-arms-apparatus-sol-review-NO-GO.md`, 2026-09-03).
Why unsublime: an apparatus that scores anything will score noise, and the receipt it emits is
indistinguishable from a real one.
Sublime counterpart: typed refusals that DELETE the receipt and return nonzero — `incomplete-run`,
`watch-schema-unsupported`, `SCORE-ABORT malformed-watch duplicate-header`; and a discrimination
check proving good arms still score byte-identical.
Ratchet: score.py schema-version + header-uniqueness checks (closed rounds 5–6).
Meter: self-test 55 → 162 → 354 → … → 389 passed across rounds as fail-open gaps closed.
Category: apparatus

### 44. Source text is not execution
Symptom: a checker verifies a program by reading the program.
Counterexample: the self-test's round-9 static regex over its own source accepted "an exact tally line
inside an inert quoted heredoc"; a visible `FAIL case35d heredoc-probe` still produced
`anvil-arms self-test: 386 passed, 0 failed`, rc 0
(`docs/observations/2026-09-03-anvil-arms-apparatus-round6-sol-review.md`, 2026-09-03). Found again a
day later in a different lane: "the heap-config gate asserts the printed make line, while the re-exec
silently ran the 512 MB suite at 7.8 GB" (captain's log 2026-09-04 07:10Z).
Why unsublime: matching a script's bytes for the shape of a correct result proves the string exists,
not that it ran.
Sublime counterpart: a runtime ledger of what executed (`tally <id>` appending to `$WORK/tallied`,
"reads no source text at all"), reconciled against an independent recount — which then produced
`383 passed, 4 failed` against the same twelve attacks.
Ratchet: self-test.sh runtime tally ledger + case 45 (shipped 77e6237); memory
`source-text-is-not-execution`.
Meter: heredoc decoy: `386 passed, 0 failed` (false green) → `383 passed, 4 failed` (caught).
Category: apparatus

### 45. A gate whose verdict depends on ambient state it never builds
Symptom: the same commit is green on the builder's tree and red on the reviewer's.
Counterexample: fold-diff round 10, reviewer verbatim: "asserts only that the log EXISTS, while case
13 rows 13/16 need >80 rows … as found (5-line log) → ✗ row 13 (exit 0), GATE5_EXIT=1; after
`make seed-demo` (149 lines) → ✓ 22 cases, all green"
(`docs/observations/2026-09-03-gene-report-night.md` §6 item 3, 2026-09-03).
Why unsublime: an undocumented ambient precondition makes the gate's verdict a fact about the box.
Sublime counterpart: round 11 — "the suite builds its own 120-row log under `$TMP` from a committed
fixture — 25 cases green with `data/` ABSENT — and a `LOG=` override below 80 rows exits 3 with a
named PRECONDITION."
Ratchet: fold-diff round 11 (3d344432); memory `ambient-state-is-an-invisible-precondition`.
Meter: builder 22/22 green vs reviewer red at row 13 → 25 cases green with `data/` absent.
Category: apparatus

### 46. A rule set in a profile the harness's shell never reads
Symptom: an environment guarantee is verified at one entrance and assumed at all of them.
Counterexample: Gene, verbatim: *"You must use /var/tmp — tmp is tmpfs, which uses ram."* /
*"Make it impossible to make this mistake again."* The ratchet was then verified at three entrances:
"Login shell: `TMPDIR=/var/tmp/forge`, `JAVA_TOOL_OPTIONS` set. Through `suite-run`: set (the guard).
The harness's own Bash tool shell: UNSET — it is non-interactive and does not read `~/.bashrc`, so a
fixture created directly by a Bash call (or by an agent's Bash) would still default to `/tmp`."
Closed by putting the four variables in `~/.claude/settings.json` `"env"` (captain's log 2026-09-04
05:04Z, 05:06Z).
Why unsublime: "a rule set in a shell profile is set for the shells that read the profile, and the
harness's shell is not one of them."
Sublime counterpart: verify the guarantee AT EACH ENTRANCE, and install it where every tool shell and
every subagent inherits it.
Ratchet: `~/.claude/settings.json` env block; memory `anvil-tmp-is-shared-tmpfs`; the tmp-leak
ratchet landed d0b4e1ca after four rounds.
Meter: no meter movement recorded.
Category: apparatus

### 47. A registration proved in one launch mode proves nothing about another
Symptom: a capability probe is run with different arguments from the thing it certifies.
Counterexample: "Fan-out B admission: REGISTRATION PROOF FAILED under the actor's flags — with
`--ignore-user-config --ignore-rules` the repo-level `.codex/config.toml` is not loaded
(`tools_offered []`, model bound, server listening, `required=true` no abort). A D arm on the file
alone would have been a native arm with extra prose … Lesson for the seat: a registration proved in
one launch mode (my 07:43Z smoke, no ignore flags) proves nothing about another; the registration
proof is per exact argv" (captain's log 2026-09-06 09:37Z). The mirror image six days later: the
09-11 finding that repo-level config "did NOT deliver MCP" was itself an artefact of the isolation
flag — "three sacrificial launches under this worktree's production argv (no `--ignore-user-config`)
each delivered a genuine `mcp_tool_call` round trip"
(`docs/observations/2026-09-12-codex-arm-n-report.md` §8, 2026-09-12).
Why unsublime: the flag added to keep the rig clean removed the delivery path under test, in both
directions — once hiding a real capability and once manufacturing a false general fact.
Sublime counterpart: prove registration under the EXACT argv the arm will run, as an admission
check, not an arm; and when a negative finding is generalised, reproduce it under the production
argv before it enters the record.
Ratchet: the freeze manifest carrying the D argv; memory `codex-seats-need-repo-level-mcp-config`;
`2026-09-12-codex-config-delivery-repro.md`.
Meter: no meter movement recorded.
Category: apparatus

### 48. A builder that backgrounds its work and exits
Symptom: the packet closes terminal while the work it launched is still running, unowned.
Counterexample: "the packet's builder (claude-sonnet-5) launched the 26-run batch in the background,
wrote 'I'll stop polling and wait for that signal', and EXITED — the packet went terminal with 7 of
26 runs scored … and no `report.edn`, and the JVM lease was reconciled dead-holder" (captain's log
2026-09-12 03:32Z). The correction the next hour matters as much: the seat's own claim that the batch
"died at 9/26" was wrong — "My wait loop counted rows in a ledger file the runner only rewrites at
the end, so nine rows meant 'nine written so far', not 'nine survived'" (05:14Z).
Why unsublime: a terminal verdict over live work destroys the run's ownership and its evidence at the
same time.
Sublime counterpart: "the packet must refuse to close while a child it spawned is alive, or the
runner must forbid backgrounding"; every caller session runs in the packet's foreground.
Ratchet: the foreground clause added to the long-context brief (03:35Z).
Meter: no meter movement recorded — arm N attempt 2 closed `:verdict :blocked`.
Category: apparatus

### 49. A setting applied after the command line
Symptom: the envelope silently overrides the caps the thing inside it set for itself.
Counterexample: "the packet seals `_JAVA_OPTIONS=-Xmx1024m -Djava.io.tmpdir=<packet tmp>`, and
`_JAVA_OPTIONS` is applied AFTER command-line flags, so the coordinator's 512 MiB per-worker cap and
its private temp dir are silently overridden inside every packet. Astra proved it with two diagnostic
JVMs" (captain's log 2026-09-12 06:42Z).
Why unsublime: every measurement inside the envelope was taken at a heap and a temp root nobody
declared.
Sublime counterpart: drop `_JAVA_OPTIONS`, keep `JAVA_TOOL_OPTIONS`, and witness a 512 MiB child
INSIDE a packet.
Ratchet: inb-55b884 (ship v3.12).
Meter: no meter movement recorded.
Category: apparatus

### 50. A tripwire that has never run
Symptom: the absence of alarms is read as the absence of drift.
Counterexample: "`~/bin/check-prompt-plate.sh` (hourly cron, present) pointed at
`$HOME/src/clj-surgeon-main`, which does not exist on this seat, and `~/logs/prompt-plate.log` had
never been written — the prompt-plate drift check has been failing silently since install. Same class
as the doctrine it enforces (a document asserting a state nobody verified at the place it takes
effect)" (captain's log 2026-09-06 19:04Z). Its first real run, after the fix, reported the real
drift: `OK main=38e40a94` at 19:09:30Z.
Why unsublime: a silent tripwire is worse than no tripwire, because it is counted as coverage.
Sublime counterpart: the tripwire writes a line every run, pass or fail, and its log's staleness is
itself monitored — "the pulse file's staleness IS the signal."
Ratchet: the corrected `check-prompt-plate.sh` (W → records worktree, `origin/main` →
`origin/MCP/main`); house rule 7 (disabled tripwires expire).
Meter: no meter movement recorded.
Category: apparatus

---

## briefs

### 51. A brief over-constrained by its own author
Symptom: the packet stops, correctly, on limits the brief invented, and spends launches producing no
data.
Counterexample: three long-context packets, zero scored sessions. "Each stop was a literal reading of
my own brief: a probe cap I set spent on the arms that changed mode, a repository gate for the one
task whose repo is not in the fixture. The packet contract is doing what it says; the brief was
over-constrained by its author." Amendment 3: "delivery probes uncapped, task 13 oracle-only with
gate unknown, and an explicit 'once the probes attest, the sessions run; owed items record, they do
not block'" (captain's log 2026-09-12 03:54Z).
Why unsublime: a stop is cheap only if it is about the subject; a stop about the brief costs a launch
and teaches nothing.
Sublime counterpart: "name the admission conditions and let the packet spend what those conditions
cost; do not cap the evidence the packet needs to admit itself."
Ratchet: Amendment 3 to `2026-09-12-longctx-preregistration.md`.
Meter: no meter movement recorded — three attempts, 0/104 task submissions scored.
Category: briefs

### 52. A brief executed against the box instead of the envelope
Symptom: the brief asks the builder to create something the sandbox cannot contain.
Counterexample: block B attempt 4 — "`git worktree add` registers the new checkout under the SHARED
repository's `.git/worktrees`, which the envelope does not cover. So I made the base checkout myself
before launch … Four refusals now, every one a brief defect, every one before a JVM ran. The lesson
that generalizes: a packet brief must be executed in the head against the ENVELOPE, not the box.
Anything the builder must create that touches a shared store (git registration, `/var/tmp` fixtures,
`~/bin`) is Fable's to create before admission" (captain's log 2026-09-12 06:3xZ).
Why unsublime: every shared-store side effect is a refusal waiting at admission, and each one costs a
full launch to discover.
Sublime counterpart: walk the brief against the envelope before launch; pre-create anything that
touches a shared store and tell the brief it exists.
Ratchet: none yet — the rule is stated in the block-B briefs from attempt 5 on.
Meter: no meter movement recorded — four attempts refused before any JVM ran.
Category: briefs

### 53. A brief number that the mechanism cannot reach
Symptom: the brief's ceiling is set by intuition and is below the measured floor.
Counterexample: MEM-015 — "allocation 62,686,992 → 37,583,552 B (1303× → 781× source; the irreducible
node tree alone is 749×, so my brief's '≤ 30×' was physically wrong — the builder measured the floor
and set the ceiling at 980× = green × 1.25, stated in the docstring)" (captain's log 2026-09-03
06:21Z).
Why unsublime: an impossible target either produces a false pass or burns the round arguing.
Sublime counterpart: the builder measures the mechanism's floor, derives the ceiling from it with a
stated margin, and says so in the docstring; the review then attacks the margin, not the number.
Ratchet: MEM-015 with its explicit rebaseline rule (five samples, min 794.3×, environment pinned).
Meter: 1,000 files: wall 11,784 → 6,621 ms (−43.8%), allocated 25.28 → 14.95 GB (−40.9%), identical
SHA-256 over all outlines.
Category: briefs

### 54. A delivery probe that bundles the arms it is certifying
Symptom: one probe boots several interventions at once, so a green probe certifies only the
combination.
Counterexample: "The two probes intentionally combined B's text, H's exact installed hook, and AC/H's
MCP configuration. This proves that combination only; it does not prove all three isolated arms" —
and the caller's own attestation that the paragraph appeared "twice" is labelled "a warning about
unverified intervention separation, not proof of AC/H contamination… these are caller attestations
and must not be promoted to independent byte capture"
(`docs/observations/2026-09-12-longctx-report-attempt2-blocked.md`, 2026-09-12).
Why unsublime: the probe reads as "the arms are configured correctly" and certifies something
narrower, leaving open exactly the isolation question the experiment exists to answer.
Sublime counterpart: probes get the same one-variable-at-a-time discipline as the scored arms, or the
probe's result is explicitly bounded to its combination, and a caller's self-report is never promoted
to byte capture.
Ratchet: none yet — named as a blocker; 0/104 submissions scored under it.
Meter: no meter movement recorded.
Category: briefs

---

## process

### 55. Surfacing the definition is not surfacing the decision
**DELETED 2026-09-12 by Astra's refutation (see review-astra-2026-09-12.md): the receipt does not support the entry.**

### 56. Paper cuts before capability
Symptom: friction the team pays every hour is deferred behind the next feature.
Counterexample: Gene, verbatim: *"I want some of these rough edges / paper cuts fixed first before
proceeding. Slowification in action."* and *"Maybe schedule an ethnographic analysis upon conclusion
of round, and then a paper cut / perfect tool spike, and then resume work. It's imperative we build a
tool that we actually use!!!"* The cadence adopted was round → ethnography → spike → resume, with
`~/bin/ethno` built as the one-shot; the first ethnography counted "76 tool calls, all
send_message/followup_task/list_agents, zero edits" from the orchestrator (captain's log 2026-09-05
03:01Z, 03:33Z, 03:35Z). Three weeks of the same instinct: *"Writing python, babashka, is like super
ridiculous. Like using emacs, and firing up echo and cat to modify files. Not acceptable. We can do
better for Astra!!! Hahaha"*
Why sublime: the cuts are named from an observed session, not from intuition, and the next capability
round does not start until they are closed.
Anti-pattern it prevents: a tool whose own authors work around it.
Ratchet: `~/bin/ethno`, `~/bin/status`, `~/bin/events`; memory `friction-ledger-to-ratchets`.
Meter: window — "routine is the laggard in all ten half-days (mean 3.1)… it moved for the first time
today (4 → 6) because the seat stopped hand-driving and built the one-shots"
(`2026-09-08-sublime-score-5-days.md`).
Category: process

### 57. Obviously-better goes to the partner, not to the principal
Symptom: a reversible improvement under an existing ruling waits on the busiest human.
Counterexample: Gene, verbatim: *"Go on test. Don't ask me in future for things like this —
obviously better. Instead ask Astra to confirm."* → the standing rule from that moment: "obviously-
better changes get Astra's confirmation, not Gene's" (captain's log 2026-09-05 02:17Z). And when he
is busy: *"On things waiting for my approval: I'm busy. If you can approve responsibly, go for
it!!!"* — each approval then recorded with its safety condition (an executed re-review against
current main before the mayor lands it) (2026-09-03 22:13Z).
Why sublime: the principal's attention is reserved for direction changes, irreversible scope, and
device receipts; everything else has a named confirmer.
Anti-pattern it prevents: a queue of reversible decisions blocking on one person, and a seat
approving its own work with no condition.
Ratchet: memory `obviously-better-goes-to-astra`; memory `gene-delegates-approvals-when-busy`.
Meter: no meter movement recorded.
Category: process

### 58. DO NOT RUN — refusing to spend a cohort on a task the tool cannot express
Symptom: the design phase proves the verb cannot do the natural task, and the task is quietly
narrowed to fit.
Counterexample: row 3's preregistration froze the full design, oracle and falsifiers and then wrote:
"**Admission status: DO NOT RUN.** The natural task selected below is outside the schema at HEAD
`1f962abc…`: `require_change` is coupled to a nonempty `symbol_migration`, accepts one exact global
alias instead of an alias policy, and refuses every comment-bearing require clause" — and forbade the
obvious escape: "No run may conceal those facts with a bespoke native pre-edit inside D"
(`docs/observations/2026-09-08-row3-require-preregistration.md`, 2026-09-08).
Why sublime: the design becomes the finding, and the cohort's budget is not spent measuring a task
bent to fit the tool.
Anti-pattern it prevents: a D arm whose success is propped up by an unmetered native workaround.
Ratchet: the standalone whole-intent `require_change` operation named as the prerequisite gain.
Meter: no meter movement recorded — cohort never launched.
Category: process

### 59. A stop rule that cannot tell a bad answer from a broken instrument
Symptom: the model does something wrong, the apparatus halts, and real data is discarded.
Counterexample: a native control "applied the correct patch, ran 2 tests, 20 assertions green", then
"says it acted prematurely and reverts to the original file", finishing READY instead of DONE — and
because the preregistration required six VERIFIED positive controls, "the cohort stopped before raw
arms". The self-correction: "That was an overly strict interpretation: a stochastic model's wrong
result is evidence, not automatically a broken positive-control apparatus. Future fixed-size cohorts
should retain real model failures in correctness and terminal-latency results, while stopping for
actual runner/identity/proof failures"
(`docs/observations/2026-09-06-astra-raw-cohort-control-failure.md`, 2026-09-06).
Why unsublime: "the subject behaved unexpectedly" and "the measuring instrument is broken" are
different events, and conflating them throws away the datum the cohort exists to collect.
Sublime counterpart: classify the failure before the stop rule freezes — model failures are retained
data; runner/identity/proof faults halt.
Ratchet: the corrected rule, applied in the immediately following cohort
(`2026-09-06-astra-raw-cohort-v2-result.md`).
Meter: cohort voided as inconclusive; corrected rule shipped in the next design.
Category: process

---

## delivery

### 60. A pointer is not delivery
Symptom: the protocol lives in a file the agent must choose to open.
Counterexample: the blind-reader witness's Sol arm — "all 16 as-specified Sol cells were void — Sol
read 'Do not run anything' as 'open no files' and answered unknown from the prompt alone… That null
is itself a delivery lesson: a protocol living in a file the agent must choose to open is a pointer,
not delivery (CX-1 again)" (captain's log 2026-09-08 20:24Z). The cold-start pilots had found the
same thing from the other side: "p3/p4 sol Ø/W mvr FAIL 2/7 — Codex has no skill mechanism, the
pointer named an artifact Sol could not load; Sol said so and went native", and the repair was
"COLDSTART.md inlined into AGENTS.md/CLAUDE.md (delivery, not pointer)" (2026-09-08 03:20Z).
Why unsublime: a delivered pointer and a delivered protocol produce the same install receipt and
different behaviour.
Sublime counterpart: inline the bytes into the surface the caller actually reads, and measure
installed → offered → invoked → used separately (next entry).
Ratchet: COLDSTART.md inlining; CX-1 in the cold-start counterexample list.
Meter: pilot 1/4 executed the intended workflow → pilot 4/4 after "seven pilots and six one-sentence
fixes to the block plus five apparatus rounds."
Category: delivery

### 61. Installed, offered, invoked, used — four counts, not one (sublime)
Symptom: "we shipped it" is reported from install logs.
Counterexample: arm S measured the clj-surgeon skill at four stages: "installed | 26/26 … offered |
26/26 (the session's own init event listed it) … invoked | 0/26 (the caller actually called the Skill
tool) … used | 3/26 (Surgeon produced the final bytes)"
(`docs/observations/2026-09-11-gate-vs-prompt-report-claude.md` §7, 2026-09-11).
Why sublime: a 100%-installed, 100%-offered, 0%-invoked skill is a specific, actionable fact that a
single "available?" checkbox erases.
Anti-pattern it prevents: reporting adoption from delivery logs.
Ratchet: `S-SKILL-EXPOSURE` preflight (archives the init skills array and the resolved skill hash).
Meter: installed 26/26 · offered 26/26 · invoked 0/26 · used 3/26.
Category: delivery

### 62. Install, then test, on the real platform — and say what was never executed
Symptom: a portability claim is made from a box that cannot run the thing.
Counterexample: the skiff-ready install landed with the honest caveat stated in advance ("written,
not executed on a Mac"); the mayor ran the tag and "make install PASSED on Darwin arm64 … make test
FAILED with four defects, each filed with evidence: inb-7e7366 … inb-9af090 … inb-ef25cd … inb-e748f0
… Every one of these is exactly what 'written, not executed on a Mac' meant in yesterday's report,
and the install-then-test order gave us the receipt in one round trip. Not a mistake to install: the
versioned package keeps every commit and the revert is one command" (captain's log 2026-09-10
16:13Z).
Why sublime: the unverifiable half is labelled before it ships, the reversal path is named, and one
round trip converts four guesses into four filed defects with evidence.
Anti-pattern it prevents: a cross-platform claim defended from a Linux box, or a refusal to ship at
all while the only real receipt stays unobtainable.
Ratchet: the `make install` preflight naming platform/bash/java/clojure/bb/kondo/swipl/admission mode;
the four inbox items.
Meter: trunk — `stable/2026-09-10` carried the 9; the four defects moved the next tag, not the score.
Category: delivery

### 63. A staged set edited after its install
Symptom: the proof artefact for a delivery is modified in place, so the next install's baseline is
poisoned.
Counterexample: "Delivery blocked, not by the change: `/var/tmp/forge/ship-v3.7` was staged past its
last install (item 1's parked consumer work: gate-envelope.clj, gate-consume.clj, mutate.clj, five
fixtures) and v3.8 inherited the drift; a v3.7 baseline control fails the installer's proof with
byte-identical counts" (captain's log 2026-09-10 07:07Z).
Why unsublime: the installer's own proof compares against a set that no longer represents what was
installed, and the failure looks like the change.
Sublime counterpart: "a staged set is a proof artefact; leaving a parked builder's edits in it
poisons the next install's baseline — stage per version, never edit a staged set after its install."
Ratchet: the min-src composition retained at `/var/tmp/forge/squeeze/min-src/` (15/15 carried files
hash-identical to installed).
Meter: INSTALL OK v3.9 on 235 rows only after recomposing on the installed epoch.
Category: delivery

### 64. A push from a shared checkout carries someone else's commits
Symptom: a records one-shot pushes `HEAD:<trunk>` from a checkout another agent has re-pointed.
Counterexample: "INCIDENT (owner: forge-anvil / Fable): three UNREVIEWED commits reached MCP/main
without gates … Cause: the pane agent switched the SHARED seat checkout to its branch (~14:5xZ); my
usage-watch one-shot committed its log line there and pushed `HEAD:MCP/main` without checking the
branch — the push carried his commits. Detected within minutes … Remedy: three revert commits on
MCP/main (57343cb5 top; no force push)" (captain's log 2026-09-06 15:04Z). Reported, not hidden, with
blast radius named: "MCP/main for ~10 minutes; public main untouched."
Why unsublime: a tool that can push whatever the working tree happens to be on will eventually push
whatever the working tree happens to be on.
Sublime counterpart: make the failure unrepresentable — "records one-shots run in a dedicated records
worktree on branch `records/MCP-main`, refuse unless fast-forwarded to `origin/MCP/main`, refuse on
any other branch, refuse if `src/test` differ from HEAD"; servers run from their own worktrees, never
the shared checkout.
Ratchet: memory `never-push-from-the-shared-checkout`; memory
`main-checkout-is-shared-add-by-name`; the `records/MCP-main` ref split (Gene decision 1a,
2026-09-09 01:34Z).
Meter: no meter movement recorded — reverted inside the owner's scope, no Andon pulled.
Category: delivery

---

## memory

### 65. A pointer you must remember is not a pointer
**DELETED 2026-09-12 by Astra's refutation (see review-astra-2026-09-12.md): the receipt does not support the entry.**

### 66. A seat is invisible while it is dead
Symptom: an outage looks exactly like a busy seat from outside.
Counterexample: "the seat's Claude WEEKLY limit hit at ~13:03Z … Three subagents died mid-flight …
The main loop was silent too: every heartbeat and usage-watch prompt from 13:03Z to 19:03Z queued
unanswered — six hours with NO pulse refresh, NO heartbeat, and the cron prompts piling up (about 35
heartbeats, 5 usage watches)… the mayor's 10-minute watch is the only thing that could have noticed;
the pulse file's staleness IS the signal (pulse 13:02Z at 19:03Z)" (captain's log 2026-09-03 19:04Z).
Why unsublime: an absent heartbeat is a non-event, and non-events are not delivered to anybody.
Sublime counterpart: relaunch every killed lane from its committed state (commit-per-item made it
cheap), and treat pulse staleness as the monitored quantity rather than pulse content.
Ratchet: memory `claude-session-limit-kills-subagents` (with its weekly-limit addendum); the mayor's
pulse watch.
Meter: no meter movement recorded.
Category: memory

---

## Claims without receipts

Listed, not entered. Each is a claim a document makes that the documents in scope do not bind.

1. "The mayor's own ack was 8 minutes late, requiring escalation on a second channel" —
   `2026-09-03-gene-report-night.md` A.4; no SLA definition or channel receipt in scope.
2. "OpenAI's content filter refuses our own symlink/path-confinement fixtures" as a general property —
   `2026-09-03-gene-report-night.md` §10; the per-refusal text is not in any scoped doc (the
   captain's log's running tally IS a receipt for the COUNT, which is why entry 33 stands on the log).
3. Cohort I's 1.75× fan-out figure — cited as prior context by `2026-09-07-pair-1-result.md`; the
   underlying receipt is outside the read scope.
4. Astra's forecast "150 s to all four gates (range 100–240)… a machine-ready mapping might reach
   ~80 s" — `2026-09-07-plan-2-cellA-result.md`; a forecast, not a measurement.
5. "61.2% of actions apparatus", attributed to Astra at 12:56Z — quoted secondhand in
   `2026-09-08-sublime-score-5-days.md`; original not in scope.
6. "72.7% apparatus (8/11 actions)" crossing the <50% tripwire with "no new alarm filed" —
   `2026-09-11-astra-sublime.md`; the 8/11 action log is not shown.
7. "Sonnet never called Surgeon once in 84 fresh-session runs with a live server and a plate" —
   `2026-09-11-gene-report-morning.md`; the 84-run denominator and per-run detail are not shown
   (the cohort docs bind 60/60 and 0/24, not 84).
8. Program share as "a MODEL trait far more than a task trait" (sol 3%, sonnet 17%, astra 71%, opus
   90%, fable 100%) — `2026-09-10-ethnography-program-edits.md`; the census binds the shares, not the
   causal claim.
9. "Whole-program 4–8× remains unproved" is itself the honest state, but the 4–8× figure it refers to
   has no cohort behind it — captain's log 2026-09-09T00:03Z reconciliation.
10. The trunk 9 is self-issued: "independence 0 (every receipt self-built; the recorder needs the
    mayor's account)" — captain's log 2026-09-11 21:26Z. Every trunk/window number in this file
    inherits that caveat.

## Duplicates of existing wiki entries (not repeated here)

- **"arm N died at 9/26"** — already a counterexample under *Cause stated from a summary number* (#3).
  The generalisable half (a progress meter reading a file the runner rewrites only at the end) is
  folded into entry 48 instead.
- **The 50% functional floor / apparatus-share tripwire**, including Gene's *"ensure 50/50 allocation
  between perfect tool vs pushing frontier; can't lose 3 hours like this again. Suggesting bounding
  each iteration to 1 houpr"* — covered by *Apparatus share at 100% while calling the day productive*
  (#9).
- **Re-verify a GO-WITH-FIX at the tip** — sublime pattern #2. Entry 28 (stale-GO) is kept as a
  distinct case: there the FIX was fine and the BASE had moved.
- **A ceiling derived from measurement rather than typed** — the sublime half of *A constant typed by
  hand* (#5). Entry 14 keeps only the new part: the margin stated in both directions and the
  adversarial shape that produced the ladder.
- **Refusal meaning bound to an exception wrapper / English message text; evidence bound to generated
  source; identity bound to line numbers** — *Facts coupled to incidental representation* (#6). Entry
  5 (code as a JSON string) is the nearest neighbour and is kept because its receipt is a caller-side
  correctness loss, not a classification loss.
- **A refusal that names nothing** (#11). Entry 34 is kept as its complement: a refusal that names
  the wrong thing, completely and confidently.
- **A waiter the harness can kill** (#13). The seat's own variant — *"a pgrep pattern matched the
  shell that contained it — verify cmdline BEFORE kill, not after printing it"* (captain's log
  2026-09-03 06:58Z; memory `waiters-bind-to-pid-not-argv`) — is the same class and is not repeated.
- **A test green on one runtime hiding the defect on the other** (#7); **policy from single
  observations** (#4); **instance-at-a-time discovery through the most expensive entrance** (#1);
  **hand-written boilerplate briefs** (#2); **an over-broad rule executed correctly** (#8); **running
  the loop by hand instead of through its entrances** (#10); **a boundary that authenticates but does
  not authorize** (#12) — all unchanged, none repeated.
