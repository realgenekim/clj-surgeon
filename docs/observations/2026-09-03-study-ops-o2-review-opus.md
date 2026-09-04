# O2 (ls-tree one-call sufficiency) — Opus executed review of bridge/study-ops-mcp at 26e4810 (2026-09-03T23:32Z)

Verdict: **GO-WITH-FIX for merging 26e4810 via the integration branch.** ls-tree CLOSED; the class is OPEN on all eight other inspect modes. Close at merge: ls-tree results exceed `max-public-result-bytes` 32,768 unenforced (34,042 bytes measured; `enforce-result-budget` covers three verbs only). Class (round 2): `deps`/`topo`/`ls-deps`/`ls-extract` render a row COUNT and hide `next_call` on truncation; `outline` names first and last form only; `forms` never renders `:source`; `match`/`xray` drop evidence above 1,024 chars and still claim terminal evidence; a study refusal prints "All listed owners are real snapshot evidence…" while listing zero owners and never prints `:error`; the good pattern is the ls-tree refusal branch at mcp_inspect_tool.clj:857 — lift it into `concise-summary` and the generic refusal branch. Telemetry: `session.start` dedupes per process × root (a second client run on the same root is invisible; per-arm proof needs distinct worktrees) and `client_run_id` has no production caller. Witness gap: row LOSS caught, row DISAGREEMENT (order, phantom rows) not. Bound answers: default 8192 completes 30 files / 90 forms with grep, `format=names` completes the real E6 tree 25/25 in 3,065 chars (builder confirmed to the character); refusals typed. Goldens 4/4 identical; gates reproduce exactly.

## Opus verdict, verbatim

# O2 / PF-5 executed re-review — `bridge/study-ops-mcp` @ 26e4810

Independent reviewer, Anvil seat, worktree `/home/forge/tmp/sol/o2-wt` (verified at
`26e481053e2bc0ea5a0c106174a49c47f92af683`, clean, five commits on GO tip `4480e3d`).
Nothing committed, stashed, or pushed. No port bound; no process signalled. Fixtures under
`/tmp/o2-sol-fx` and JVM-created temp dirs only. Written 2026-09-03.

Every number below is measured in this checkout, in process, through
`mcp-operation/invoke!` — so the string called TEXT is literally `content[0].text` as the
callback publishes it, and the map called structuredContent is literally the third callback
argument.

---

## Gates (item 6) — all six reproduce

| gate | builder's claim | measured here |
|---|---|---|
| `suite-run bb test/run_all.clj` | 731 / 6,023 / 0 / 0 | **731 tests, 6,023 assertions, 0 failures, 0 errors** |
| `clojure -M:clj-surgeon/mcp-test` | 437 / 5,634 / 0 / 0 | **437 tests, 5,634 assertions, 0 failures, 0 errors** |
| `swipl -q -f test/mcp_operation_contract_oracle.pl` | pass | **pass**; `legacy counterexamples=[verification_failed,verification_pending]`, exit 0 |
| `mcp-intent-contract/audit-current-repository` | 0 violations | **ok=true, violations=0** |
| CLI goldens byte-identical (4 files) | 4 files | **4 files, identical blob SHAs across `4480e3d..26e4810`**; `git diff --name-only 4480e3d..26e4810 -- test-fixtures/` is empty |

Golden blobs, both revisions: `ls-tree-existing-ops.golden.txt` `aa06049`,
`ls-tree-existing-ops-edn.golden.txt` `e370c02`, `ls-tree-no-clojure-files.golden.txt`
`8e5a55a`, `ls-tree-prune-target.golden.txt` `d54a061`. Confirmed — the CLI renders through
`study/format-ls-tree-text`, which this branch does not touch.

---

## Item 1 — the class ratchet: is `content[0].text` ⊇ structuredContent, per mode?

Reproduction: `/tmp/o2-sol-fx/probe1.clj`, `probe2.clj`, `probe5.clj`, all against
`src/clj_surgeon/analyze.clj` (27 top-level forms) in this checkout.

| mode | verdict | what TEXT carries | what only structuredContent carries |
|---|---|---|---|
| `ls-tree` | **CLOSED** | header + every row + truncation state + continuation line or remedy | — |
| `deps` | **OPEN** | `request-1: deps · 27 of 27 rows` | all 27 `{name,type,line,depends_on}` rows |
| `topo` | **OPEN** | `request-1: topo · 27 of 27 rows` | the whole topological order |
| `ls-deps` | **OPEN** | `request-1: ls-deps · 1 of 1 rows` | the entire `dep_tree` |
| `ls-extract` | **OPEN** | `request-1: ls-extract · 1 of 1 rows` | the entire `closure` |
| `outline` | **OPEN** | `598 lines · 28 forms · first file->zloc · last topological-sort` | the other 26 forms and every line range |
| `forms` | **OPEN (by design)** | `reader-cond?@37-39 · 131 source characters` | `:source` — the answer when `include_source` is on |
| `match` | **OPEN (worst)** | `7 matches` and **nothing else** above the cap | all 7 match sources (3,529 chars) |
| `xray` | **OPEN** | nothing above the cap | `:value` |
| owners (refusal vocabulary) | **OPEN + live bug** | `refused · study-form-not-found` and a sentence referring to a list that is not printed | `:error` text and all 27 `available_owners` |

O2 fixed exactly one of the ten. The same defect it fixed exists on eight of the other nine,
in the same shape and from one function: `mcp_inspect.clj:1090-1136` renders a COUNT of rows
where `ls-tree-summary` now renders the rows.

Two of them are strictly worse than the pre-O2 `ls-tree` was:

**`match` / `xray` drop their evidence silently and then call the read terminal.**
`compact-json` (`mcp_inspect.clj:1090`) returns `nil` above 1024 characters, and
`concise-result-line` is wrapped in `when-let` / `when compact`, so the whole line vanishes.
Measured: `match "(defn- _ _ _)"` on `analyze.clj` → `match_count=7`, 7 matches in
structuredContent, compact JSON 3,529 chars, TEXT contains no match source at all — and the
text still says `✓ terminal evidence · read_complete=true · next action none`. A text-only
caller is told the answer is complete and receives none of it, with no truncation flag to
contradict it. The pre-O2 `ls-tree` at least said `read_complete=false`.

**The study refusal text names a list it does not print.** `diagnostic?`
(`mcp_inspect_tool.clj:1551`) requires both `failed_request` and a `:failures` entry. A study
refusal has neither, so the `available owners (n/n): …` line at `:1571` is skipped — but the
sentence at `:1579` is guarded only by `(seq available-owners)` and prints anyway. Measured,
`ls-deps form=no-such-form-xyz`: structuredContent carries `:error` ("No top-level form named
no-such-form-xyz in src/clj_surgeon/analyze.clj") and 27 `available_owners`; TEXT carries
neither, and reads:

```
inspect_clojure
  refused · study-form-not-found · 55.12 ms

  All listed owners are real snapshot evidence; ranking is non-authoritative. …

→ correct_request
```

The same refusal through `forms` (where `diagnostic?` is true) prints all 27 owners and the
hypothesis correctly — so the bug is the study/`ls-tree`-adjacent path specifically, and the
generic branch never prints `:error` for any mode.

None of this is a regression from `4480e3d`; all of it predates the branch. It is the class
the E6-Lb finding names, and O2 closed one member of it.

---

## Item 2 — the bound

Reproduction: `/tmp/o2-sol-fx/probe3.clj`. Toy files are the builder's own fixture shape
(ns + two requires + three forms).

**What completes in one call at the new default of 8192:**

| shape | files complete | first truncation |
|---|---|---|
| `format=text` + `grep` (the shape grep selects) | **30 files / 90 forms**, tree 6,334 chars, TEXT 6,448 | 40 files → 38 of 40, `read_complete=false` |
| `format=names` (the no-grep default) | **100 files**, TEXT 8,118 | 150 files → 101 of 150 |

The builder's headline (a ten-file/thirty-form tree complete in one call inside 8 KB of text)
is true and conservative by 3x. The 4096→8192 raise is load-bearing and correctly witnessed.

**The real E6 tree** (`/home/forge/tmp/arms/e6/pf3`, 25 `.clj*` files under `src`):

| call | result |
|---|---|
| `format=names`, default limit | **25 of 25, `read_complete=true`, TEXT 3,065 chars** — builder's figure confirmed to the character |
| `format=text`, default limit | 5 of 25, truncated, TEXT 6,981, `next call:` in the text |
| `format=text` + grep, ceiling 16384 | 7 of 25, truncated, `next_action=narrow_scope`, `maximum limit` remedy in the text, no replay continuation offered |

**Is the ceiling refusal typed, and does its text carry the continuation?** Two distinct
things, both behave correctly:
- Over-ceiling `limit` is a real typed refusal: `ok=false`, `error_type=invalid-study-limit`,
  and the ls-tree refusal branch (`mcp_inspect_tool.clj:857`) prints the cause *and*
  `next_action` in the text — unlike the generic branch above.
- At the ceiling with a too-large tree it is a *typed truncation*, not a refusal: `ok=true`,
  `error_type` absent, `truncated=true`, `next_action=narrow_scope`, `:remedy` present and
  rendered as `→ The receipt is already at the maximum limit; scan a subdirectory or add a
  grep pattern.` No continuation is offered, correctly, because none could advance.

---

## Item 3 — can TEXT disagree with structuredContent? Mutation witnesses

Reproduction: `/tmp/o2-sol-fx/probe4.clj` — `alter-var-root` on
`#'clj-surgeon.mcp-inspect-tool/ls-tree-payload-text`, then re-run the six O2 witnesses.

| mutation | pass | fail | caught? |
|---|---|---|---|
| baseline | 49 | 0 | — |
| M1 — drop the last 40% of rows from the text | 42 | **7** | **yes** |
| M2 — reverse the row ORDER in the text | 49 | 0 | **no** |
| M3 — add a phantom row the receipt does not contain | 49 | 0 | **no** |
| M4 — drop the payload entirely (the pre-O2 defect) | 34 | **15** | **yes** |

The ratchet catches *"the text is emptier than the receipt"* — including a clean red on the
exact field defect. It does not catch *"the text disagrees with the receipt"*: neither a
reordering nor an over-claim fails a single assertion. In the shipped code a disagreement is
structurally impossible (`ls-tree-payload-text`, `:832`, returns the already-bounded payload
verbatim), which is the right design — but nothing in the suite holds a future refactor to it.
The missing assertion is one line: the rendered rows equal the rows in the receipt, in order.

---

## Item 4 — `session.start`

Reproduction: `/tmp/o2-sol-fx/probe3.clj`, three `execute-inspect!` calls against one
telemetry state and one workspace root.

```
tool.call events=3   session.start events=1
{:event "session.start" :workspace_key "b3780e7f7434c8fc" :run_id "rev-run" :telemetry_mode "metrics"}
```

- One per workspace root **per server process** — confirmed, and correct as documented.
- **A second client run on the same root emits nothing.** The dedupe key is
  `workspace-key` alone (`mcp_telemetry.clj:150-154`), held in a process-lifetime atom. So the
  event distinguishes arms only when every arm gets its own worktree path. Two arms in the
  same root, or one client reconnecting, still leave the same bytes — which is the exact
  failure the event was built to end. The brief's requirement ("it must, for per-arm proof")
  is **not met**.
- `client_run_id` would fix it, and has **no production caller**: both entrances
  (`mcp_tool.clj:450`, `mcp_inspect_tool.clj:1421`) pass `{:workspace-root project-root}` and
  nothing else. Only tests supply it. Even supplied, it is not part of the dedupe key, so the
  second run would still be suppressed.
- `:off` suppresses — confirmed: `record-session-start!` returns `nil`, `:file` is `nil`.
- Root leaks only in `:full` mode; `workspace_key` is a recomputable SHA-256 prefix. Correct.

---

## New finding this change introduces: the ls-tree public result now exceeds the tool's own budget

Measured with the repo's own `mcp-result-byte-count` on the real E6 tree:

| call | complete public result | `max-public-result-bytes` | over? |
|---|---|---|---|
| `ls-tree src format=names` default | 6,742 | 32,768 | no |
| `ls-tree src grep=defn limit=16384` | **34,042** | 32,768 | **yes** |

The text block for that call is 16,471 characters — so before O2 the same call was roughly
17.7 KB. Rendering the payload into the text roughly DOUBLES every ls-tree result on the wire,
and at the ceiling it crosses the 32 KB budget the tool declares and enforces for other modes.
`enforce-result-budget` (`mcp_inspect_tool.clj:1626`) enforces only
`#{"prepare-change" "basis-view" "plan-extraction"}`; ls-tree falls to `:else raw-result`, so
nothing refuses, trims, or warns. It also means `limit=16384` now yields ~32.8 K characters of
content, not 16,384 — the builder's decision not to charge the envelope against `limit` is
defensible on its own, but the second copy of the payload is not envelope.

This is not a crash and not a suite failure; ls-tree was unenforced before the branch too. It
is a real, new, measurable overshoot of a declared bound, and it is the one item I would want
closed at merge time rather than filed.

---

## Verdict

The change does what it says, on the evidence it says it has. It is scoped, honestly
documented (the "what this does NOT establish" section is accurate — I checked each claim),
regresses nothing, leaves the CLI goldens untouched, and carries a witness that goes red on
the exact field defect. Its weaknesses are a bound it silently crosses, a telemetry event that
does not yet do the job it was added for, and a class it closes on one member of ten.

**GO-WITH-FIX** for merging `26e4810` via the integration branch.

### Numbered findings, each with a witness

1. **`mcp_inspect_tool.clj:1626` — ls-tree results now exceed the declared 32 KB public
   budget, unenforced.** Witness: `mcp-result-byte-count` on `ls-tree dir=src grep=defn
   limit=16384` against `/home/forge/tmp/arms/e6/pf3` = **34,042 bytes** vs
   `max-public-result-bytes` 32,768; ls-tree is not in the enforced mode set, so nothing
   refuses. *(fix before/with merge: add ls-tree to the enforced set, or bound
   `limit + envelope + payload-text` so the complete result fits 32 KB.)*

2. **`mcp_telemetry.clj:150-154` — `session.start` is once per process x root, so a second
   client run on the same root is invisible.** Witness: three `execute-inspect!` calls, one
   root, one telemetry state → `tool.call events=3, session.start events=1`. Per-arm proof
   holds only if every arm has a distinct worktree path.

3. **`mcp_tool.clj:450` and `mcp_inspect_tool.clj:1421` — `client_run_id` has no production
   caller.** Witness: `grep -rn client-run-id src/` returns only the definition
   (`mcp_telemetry.clj:134,136`) and the two entrances, both passing `{:workspace-root
   project-root}`; every `client_run_id` in the repo is a test literal. It is also absent from
   the dedupe key, so supplying it would not re-arm the event.

4. **`mcp_inspect.clj:1117-1128` — `match` and `xray` drop their evidence silently above 1024
   characters and still report the read terminal.** Witness: `match "(defn- _ _ _)"` on
   `analyze.clj` → `match_count=7`, compact JSON 3,529 chars, TEXT contains no match source,
   and TEXT says `✓ terminal evidence · read_complete=true · next action none`.

5. **`mcp_inspect_tool.clj:1571` vs `:1579` — a study refusal prints "All listed owners are
   real snapshot evidence…" while listing zero owners, and never prints `:error`.** Witness:
   `ls-deps form=no-such-form-xyz` → structuredContent has 27 `available_owners` and the cause
   string; TEXT has neither, but does have the sentence. The same refusal via `forms`
   (`diagnostic?` true) prints all 27.

6. **`mcp_inspect.clj:1130-1136` — `deps` / `topo` / `ls-deps` / `ls-extract` render a row
   COUNT, not the rows; and on truncation they carry `next_call` in structuredContent while
   the text offers only `next action raise_limit_or_narrow_scope`.** Witness: `deps
   limit=16384` → TEXT 243 chars, `request-1: deps · 27 of 27 rows`, all 27 rows in
   structuredContent only; `deps limit=200` → `next_call` present in structuredContent, absent
   from the text. This is the defect O2 fixed on ls-tree, unfixed on four modes.

7. **`mcp_inspect.clj:1108-1116` — `outline` names only the first and last form.** Witness:
   `outline` on `analyze.clj` → TEXT `598 lines · 28 forms · first file->zloc · last
   topological-sort`; the other 26 forms and every line range are structuredContent-only.

8. **`mcp_inspect.clj:1098-1104` — `forms` never renders `:source`, by design.** Witness:
   `forms ["reader-cond?"]` → structured form carries `:source`; TEXT carries
   `reader-cond?@37-39 · 131 source characters`. Deliberate ("source-free text companion") and
   the lowest-priority member of the class, but it is the same gap: a text-only client cannot
   see the form it asked for.

9. **`test/clj_surgeon/mcp_study_test.clj:1820+` — the O2 witnesses catch row LOSS but not row
   DISAGREEMENT.** Witness: mutating `ls-tree-payload-text` to reverse row order → 49 pass /
   0 fail; to add a phantom row → 49 pass / 0 fail; to drop 40% of rows → 7 failures; to drop
   the payload → 15 failures. One assertion (rendered rows equal receipt rows, in order)
   closes it.

10. **`mcp_inspect_tool.clj:857` is the good pattern and should be the class fix.** Witness:
    the ls-tree refusal branch prints `error_type`, the `:error` text, and `next_action`; the
    generic branch at `:1553` prints only `error_type` and `next_action`. Lifting the ls-tree
    shape into `concise-summary` and the generic refusal branch closes findings 4-8 in one
    place.

*Not audited (outside the nine named modes): `prepare-change`, `plan-extraction`,
`basis-view`, `verification-job`, which have their own dedicated summaries.*
