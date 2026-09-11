# Curation record — programs-to-contracts cohort task draw

Independent curator. I did not build clj-surgeon, I did not read the census's
tool-support or outcome columns while classifying, and no arm has run. This
file is the complete audit trail: what was frozen, what the seed was, why every
row is in or out, and every judgment call I made, including the ones that cut
against the tool.

---

## 1. The list used, and its digest

**The FULL census hit list exists on disk.** It is not aggregates-only, so the
protocol's fallback does not apply and this is **not** a failure-enriched
challenge pilot label.

`/var/tmp/forge/ethno/seqhits_intent.json` — **6,124 program-call rows**, each
carrying session path, UTC timestamp, model, agent, mechanism categories,
targets, the full command text, verification, reader-error and "oops" flags.
The miner's 60-row table (`hitrows.md`) is a breakage-ranked *sample* of this
file; `rows.py` shows the derivation. I used the 6,124-row parent, not the
sample, so Astra §3's failure-enrichment objection does not attach to this draw.

Frozen before any drawing, in `curation/frozen-list.sha256` (verified `OK`
against `/var/tmp/forge/ethno/` at the time of writing):

```
c3bfb084361ee5d4cc9f915367a9a48f4cdd3319dea00ccfe97ea165b61fec1d  seqhits_intent.json   <- the list used
a8734b63a0c4f2e0d51087b4bb99e3d9cb64ef78935d0dc77e7b95aef343907a  seqhits.json
d3a61562333b0ba83365bd6e5fa7f40c2e3078d2375e14e31673c534f56dbd71  hitrows.md
5a4a08d6bbd79c725d55415ab21bea989b6d3d7adc5bbea6ebf976a7da10af91  classified.json
366ad1f55c02c74f14acc8c3722b6ba83171a69d238e6cd5a077810c87cfdd20  raw_hits.jsonl
fac908763532bcd08fa1e919d0e8210753448013f02cce1a641fe3de6d0c1cb3  hits2.jsonl
bd2c84b4236c13491c5705460c5d00a5bd1479b0300f32aa567c9fd62df194e7  broke.json
```

The miner's own three broad families sum to 4,424 / 6,124 = **72.2%**, which
reproduces Astra's corrected figure exactly. **That 72.2% is not this sample's
coverage claim.** My families are much stricter than the miner's regexes (§2),
and the frame is narrower still after recoverability. This draw makes **no
census-prevalence claim**.

## 2. How intent was reconstructed (blind to tool support and outcomes)

I re-derived every row from the raw command text with
`curation/extract.py`, ignoring the miner's `intent` label. The pipeline parses
each command into a structured **edit operation** — an anchor (`old`) and a
result (`new`) — from three mechanisms: python `str.replace` / `re.sub` /
slice-insertion (read with `ast`, not regex), and `sed -i s///`. Rows whose
command contains no parseable file mutation are `x-noop`; rows with an
operation that is not a strict T/R/I are `x-other`.

Strict definitions used, which are *narrower* than the miner's:

- **T** — an existing `deftest` in a test file has its body (or its immediate
  marker) edited. The miner's `test_body_edit` also swept in markdown writes and
  whole-file creation; those are `x-other` here.
- **R** — a require alias or an alias use changes in a Clojure file.
- **I** — a `defn`/`def`/`deftest` is INSERTED, anchored on an existing named
  form, with nothing removed.

Result: 122 T, 34 R, 31 I candidates out of 6,124.

## 3. Eligibility: pre-edit bytes must be provably recoverable

`curation/resolve.py` resolves each candidate's repository (from an absolute
path in the command, else the session's own recorded `cwd`; deleted worktrees
fall back to the canonical repository that owns their object store) and then
**proves recoverability positively**: it walks commits reachable on any ref,
dated at or before the session timestamp, and requires that the blob for that
path actually **contains the anchor text the historical edit consumed**. A row
is eligible only when such a commit exists. That is why `:sha` in every
`meta.edn` is a real pre-edit state and not a guess.

`curation/eligibility.tsv` — one line per census row, 6,124 lines, with reason:

| count | status | reason |
|---:|---|---|
| 4,875 | ineligible | `out-of-family:x-noop` — no parseable file mutation in the command |
| 1,058 | ineligible | `out-of-family:x-other` — a parseable edit, but not strict T/R/I |
| 100 | ineligible | `pre-edit-bytes-not-recoverable` — no reachable commit at/before the timestamp holds the anchor |
| **51** | **eligible** | **`ok`** |
| 11 | ineligible | `lab-fixture-repo-not-original` — a throwaway `/var/tmp` or `/home/forge/tmp` fixture, not an original repository |
| 11 | ineligible | `dup-episode-in-session` — a later edit/repair of the same file+family in the same session; the earliest is retained |
| 9 | ineligible | `edit-already-present-in-pre-edit` — the "result" is already in the pre-edit bytes, so the reconstruction is bogus |
| 4 | ineligible | `out-of-family:I?` — an insertion whose anchor is not a named form |
| 2 | ineligible | `no-clj-path-resolved` |
| 1 | ineligible | `repo-unresolvable` |
| 1 | ineligible | `insertion-introduces-no-new-named-form` — a "replace" that is really a deletion |
| 1 | ineligible | `anchor-too-short-ambiguous` — anchor under 12 characters |

Eligible by family: **T 39, R 7, I 5**, over 33 / 7 / 4 distinct sessions.

**Nothing was excluded for being hard, unsupported by any tool, or likely to
fail.** No exclusion reason mentions a capability. The verb builder chose
nothing here.

## 4. The seed and the draw

```
seed = sha256(curation/eligibility.tsv)
     = 68f22a5b94cb380c51bd6b8d3e3374d66d068376dbe18f94bacd318f20235005   (curation/seed.txt)
rank = sha256(seed || row_id),  ascending
```

Four per family, at most one task per original session. Families are filled in
ascending order of **distinct eligible sessions** (I=4, R=7, T=33), scarcest
first. That tie-break is a feasibility requirement, not a preference: family I
has exactly four distinct sessions, so a family-ordered greedy draw starves it
(it did, at three). The rule is deterministic and blind to task content,
repository and outcome. Ranked reserves are in `curation/reserves.tsv`
(35 T, 3 R, 1 I).

Digests: `eligibility.tsv 68f22a5b…`, `selected.tsv 4e9eb2ee…`,
`reserves.tsv aa0e84c3…`, task set `curation/tasks-frozen.sha256`
(`e877278944270ca314225d92fad3719dec2179d7cee5f27eb4e01f5d73fced8a`).

## 5. The twelve selected

Every one is the ORIGINAL repository at the ORIGINAL pre-edit commit. Nothing
was transplanted into a fixture; nothing here is a synthetic analogue.

| id | family | repo / file | pre-edit sha | intent in one line |
|---|---|---|---|---|
| T-01 | T | clj-surgeon `test/clj_surgeon/outline_memory_test.clj` | `a8452157b` | Mark `outline-of-one-file-allocates-within-its-ceiling` with the `;; @spec MCP-OP-MEM-015` line directly above it |
| T-02 | T | clj-surgeon `test/clj_surgeon/mcp_study_test.clj` | `8210e5c4b` | Stop pinning the literal `reader-cond?@37-39`; assert the row names the form's own `:line`/`:end_line` |
| T-03 | T | clj-surgeon `test/clj_surgeon/mcp_alias_migration_test.clj` | `827a751ad` | Add two new typed refusal kinds to the frozen set and make the count assertion agree (147 → 149) |
| T-04 | T | clj-surgeon `test/clj_surgeon/lane_manifest_test.clj` | `0659d8b6c` | Move the corpus total pin from 1370 to 1373 and record the reason at the pin |
| R-01 | R | clj-surgeon `test/clj_surgeon/core_discovery_test.clj` | `077f5a64d` | Add `clojure.edn` to the requires under the codebase's usual alias, alphabetical order preserved |
| R-02 | R | clj-surgeon `test/clj_surgeon/recovery_test.clj` | `2b3177dfa` | Add `clj-surgeon.measured` to the requires under its short name, in alphabetical position |
| R-03 | R | clj-surgeon `test/clj_surgeon/mcp_workspace_test.clj` | `e6b9c7e9a` | Require `clojure.string` as `str` and route the one longhand `clojure.string/includes?` call through the alias |
| R-04 | R | clj-surgeon `src/clj_surgeon/namespace_split.clj` | `a9da43441` | Add `cheshire.core` to the requires under the codebase's JSON alias, alphabetical order preserved |
| I-01 | I | clj-surgeon `src/clj_surgeon/mission_cli.clj` | `71dd6eff8` | Insert a `stale?` pre-stage drift check plus its private file reader, immediately above `apply!` |
| I-02 | I | clj-surgeon `src/clj_surgeon/mission_cli.clj` | `57c9bb42f` | Insert a private helper reporting the on-disk size of every file a refusal names, immediately above `ledger-of` |
| I-03 | I | clj-surgeon `test/clj_surgeon/memory/heap.clj` | `cc04af6ac` | Insert a stop-the-world retention checkpoint and its private peak state, between `to-mb` and `measure` |
| I-04 | I | clj-surgeon `src/clj_surgeon/mission_display.clj` | `2bbcc530d` | Insert a `decision-view` that lifts a buried error type and drops a wrong-verb example, immediately above `show-result` |

Twelve distinct origin sessions. Origin callers in the provenance:
`claude-opus-5` ×8, `gpt-6-astra` ×3, `claude-sonnet-5` ×1 — provenance only;
the cohort's own callers are set by the arms, not by these rows.

## 6. Oracles — all twelve proven three ways

Each `oracle.sh <worktree>` exits 0/1 with a reason and uses **both hashers**:

- **Semantics** — `clj-kondo --lint <file> --config '{:analysis true :output
  {:format :edn}}'`, parsed by `curation/kondo_edn.py`. Requires, aliases,
  defined vars and *resolved* var usages come from the analyzer's own name
  resolution; error-level findings fail on their own.
- **Preservation** — `curation/formhash.py`, a hand-written bracket-matching
  tokenizer with no Clojure reader in it. Hashes every top-level form's raw
  bytes, **the raw bytes of the gap before each form**, and each form's
  **ordinal position**. A candidate that does not bracket-match is RED, never
  unknown.

`curation/prove.sh` recorded, in each task's `oracle-proof.log`: GREEN on
`expected-output.clj`, RED on `wrong-output.clj`, RED on the untouched
`pre-edit.clj`. **All twelve proven.** Each log carries the oracle's own
sha256 and the UTC run time.

Beyond the three required runs I probed the oracles for over-fitting and for
Astra's named blind spots (`curation/variants/`):

| probe | expectation | result |
|---|---|---|
| T-01 with the historical two-blank-line spelling | accept | exit 0 ✔ |
| I-03 with a different settle duration and docstring | accept | exit 0 ✔ |
| T-02 written with `format` instead of `str` | accept | exit 0 ✔ |
| R-02 with two `deftest` names swapped between bodies (a multiset would miss this) | reject | exit 1 ✔ |
| I-04 correct, plus a stray `;;` comment above an unrelated `def` | reject | exit 1 ✔ |

`wrong-output.clj` is parseable in all twelve cases. Seven are unmet intent
(T-01, T-02, T-03, R-02, R-03, I-01, I-03) and five are parseable collateral
damage (T-04, R-01, R-04, I-02, I-04); I-01's is specifically a **location** error —
both functions correct, compiling, at the end of the file — which every
"was the new thing added?" check passes and only the ordinal check catches.

## 7. Every judgment call

1. **Full list, not the 60-row table.** The full hit list exists, so the
   failure-enriched label does not apply. Both are frozen anyway.

2. **Strict re-classification, not the miner's labels.** My T/R/I are much
   narrower than `test_body_edit` / `change_require_alias` /
   `insert_or_edit_defn`, which is why 4,424 miner-family rows become 187
   candidates. A looser reading would have produced a larger, dirtier frame.

3. **Three pipeline corrections, all made before any arm ran, all disclosed.**
   - The lab-fixture screen originally tested the *worktree* path; a worktree
     under `/home/forge/src/` whose object store lives in
     `/var/tmp/forge/row2/seed-repo` slipped through. Fixed to test the git
     common dir. This changed the seed and therefore the draw.
   - Two classifier defects: an "insertion" that was really a deletion, and an
     edit whose result was already present in the pre-edit bytes. Fixed with
     two screens applied **uniformly to every candidate**, not to the drawn
     rows only (`insertion-introduces-no-new-named-form`,
     `edit-already-present-in-pre-edit`).
   - A `sed` parser that mangled multi-expression commands. Before adopting
     the fix I checked whether it moved any verdict: the **eligible row set was
     byte-identical** before and after; only 11 already-ineligible rows changed
     their reason. The seed changed, so the draw changed.
   **Because I saw draws before those corrections, here is the disclosure that
   neutralises the shopping hazard:** the pre-correction draws are preserved at
   `curation/.selected-predraw2.tsv` and `curation/.elig-before.tsv`, and the
   earlier draws contained rows I did **not** carry forward — among them
   `d0051e6c30a5729e` (R, `namespace_split.clj`) and `2f27c0e451a62006` (I,
   `mission_display.clj`) which survived into the final draw, and
   `9af09bc9a550d182`, `678880f9b3bd7ebe`, `dc38a17d807fefbe` which did not, in
   different combinations. I never selected on content. **After the third
   correction the pipeline was frozen; nothing was re-run.**

4. **Task scope = the smallest coherent intent containing the reconstructed
   operation.** Several historical shell commands bundled unrelated edits
   (T-03 also changed a filename filter; R-04 also changed comment encoding and
   two receipt shapes). Those are out of scope and the oracles require them to
   be ABSENT. Where the coherent intent needs a second form (T-03's frozen set
   beside its count), it is included, because the alternative leaves the file
   self-contradictory.

5. **I-02 was narrowed, and it is not a byte replay.** The episode inserted
   two functions; the first calls `mission/configured-profiles`, which **does
   not exist at `:sha`** — it arrived in `c627e1d8`, the same commit, in a
   different file. A single-file task cannot ask for a call to a function that
   is not there without guaranteeing a broken namespace in every arm, so the
   task is the second function at the same anchor. Declared in
   `I-02/oracle-notes.md` too. Astra: never silently call an analogue a replay.

6. **T-01's family fidelity.** It edits an existing `deftest` in a test file,
   but annotates rather than mutates the body. Kept (the draw is the draw),
   flagged in its notes, and the oracle scores it as a gap change against that
   deftest owner.

7. **Family R as drawn is mostly require ADDITION, not alias rename.** Three of
   four (R-01, R-02, R-04) add a require with no use site in scope; only R-03
   changes a require *and* the use that depends on it. That is what the census
   frame contains — pure renames are rare. Anyone reading "R" as "rename" will
   misread these results. The capability manifest should be checked against
   this shape before the run.

8. **All twelve landed in clj-surgeon.** Not a choice: `clj-surgeon` worktrees
   dominate the census (roughly five sixths of family rows), the seeded draw
   reflects that, and the scarcity-first fill did not steer it — the reserves
   include `curtaincall-cfp`, `marvin-voice-remote` and `kaocha-sublime` rows.
   **Repository diversity in this pilot is nil**, so nothing here separates a
   result from a property of one codebase's conventions. Say so in the write-up.

9. **Gates need a baseline; several of these files pin derived numbers.**
   `:gate` in every `meta.edn` is the repository's own narrowest command. But
   T-03's 149 and T-04's 1373 are claims about files the task does not touch,
   so a correctly completed task can still leave the namespace red at `:sha`.
   Every `meta.edn` therefore carries `:gate-baseline-required true` and a
   `:gate-note`: **run the gate on the untouched tree at `:sha` first and score
   "no new failures against that baseline", never absolute green.** I did not
   run any suite — that is the apparatus's job, and it is the one open item
   that must be closed before scoring. `I-03`'s file has no test namespace at
   all; its narrowest honest gate is that the namespace loads, with the memory
   battery named as the broader one.

10. **The apparatus did not exist yet.** Neither
    `apparatus/oracle-lib.sh` nor `apparatus/task-schema.md` was present, so the
    oracles are the protocol's self-contained fallback, with shared helpers in
    `curation/oracle-lib.sh` (kondo EDN + bracket hasher). One line at the top
    of each `oracle.sh` (`ORACLE_LIB=`) repoints them if the apparatus later
    publishes an equivalent.

11. **`expected-output.clj` is the correct implementation of the stated intent,
    not a transcript of the historical keystrokes.** T-02's historical edit also
    left a tautological `(is (= [a b] [a b]))`; I did not reproduce an assertion
    that cannot fail. Each file was produced by editing a copy of the pre-edit
    bytes and then reviewed; alternates are recorded per task in
    `oracle-notes.md`, and three of them are probed above.

12. **What I did not do.** I did not commit or push anything, did not create a
    git worktree, did not run any repository test suite, did not touch
    `/home/forge/src/clj-surgeon` except with read-only git commands, and wrote
    only under `tasks/` and `curation/`.

## 8. Files

```
curation/frozen-list.sha256   the census list and its digest, frozen first
curation/seed.txt             the published seed
curation/eligibility.tsv      6,124 rows, one reason each
curation/selected.tsv         the twelve
curation/reserves.tsv         39 ranked reserves
curation/tasks-frozen.sha256  digest of every task artifact
curation/extract.py           command -> structured edit operation (blind)
curation/resolve.py           repository + pre-edit commit, proven by anchor
curation/eligibility.py       dedupe + the eligibility ledger
curation/draw.py              the seeded draw
curation/gen_tasks.py         task.md / meta.edn / oracle.sh
curation/gen_notes.py         oracle-notes.md
curation/formhash.py          preservation hasher (bracket matching)
curation/kondo_edn.py         semantic hasher (clj-kondo analysis EDN)
curation/oracle-lib.sh        shared oracle helpers
curation/prove.sh             the three-way proof harness
curation/variants/            over-fitting and collateral-damage probes
tasks/<ID>/                   task.md meta.edn expected-output.clj
                              wrong-output.clj oracle.sh oracle-proof.log
                              oracle-notes.md pre-edit.clj reconstruction.json
```

---

## 9. Correction: the gate commands (added after the coordinator's baseline run)

**I got the gates wrong, and the coordinator's baseline caught it.** I read
`deps.edn` and `test/clj_surgeon/mcp_test_runner.clj` at **HEAD** and named that
entrance in all twelve `meta.edn` files, without checking that it existed at
each task's own `:sha`. It did not. Eight of twelve were unrunnable: `:clj-surgeon/test-deps`
and the runner's `--ns` flag postdate the September 2–4 commits (at those shas
`-main` is `[& _]` over a fixed namespace list), and `clj-surgeon.mission-test`
/ `clj-surgeon.mission-display-test` carry no lane declaration at their shas, so
the lane runner correctly refuses them with exit 96. This is the
"source text is not execution" failure in its purest form: I verified an
entrance by reading a file from the wrong snapshot.

`meta.edn` and every other frozen task file are **unchanged** — the cohort is
running and reads them (all 84 still verify against `tasks-frozen.sha256`). The
correction lives beside them in **`tasks/<id>/gate-v2.edn`** (command + one-line
`:why` + what it supersedes) and **`tasks/<id>/gate-baseline-v2.edn`** (the
failing set at `:sha`). Every one was proven by running it on the untouched tree
in a fresh detached worktree at that commit, one JVM at a time, `-J-Xmx1g`,
under `/var/tmp/forge`; all worktrees were removed afterwards
(`curation/gate_v2.sh`, logs in `curation/gate-v2-<id>.log`).

**All twelve are now runnable** (a parseable `Ran N tests` line and exit 0 or 1):

| task | old exit | new command | new exit | baseline failing |
|---|---:|---|---:|---:|
| T-01 | 1 | `-M:clj-surgeon/mcp-test` (suite) | 1 | 3 of 381 |
| T-02 | 1 | `-M:clj-surgeon/mcp-test` (suite) | 0 | 0 of 493 |
| T-03 | 145 | unchanged, `--ns mcp-alias-migration-test` | 1 | 28 of 137 |
| T-04 | 0 | unchanged, `--ns lane-manifest-test` | 0 | 0 of 25 |
| R-01 | 1 | `bb test/run_all.clj` | 0 | 0 of 806 |
| R-02 | 1 | `bb test/run_all.clj` | 0 | 0 of 654 |
| R-03 | 0 | unchanged, `--ns mcp-workspace-test` | 0 | 0 of 5 |
| R-04 | 0 | unchanged, `--ns namespace-split-test` | 0 | 0 of 35 |
| I-01 | 96 | `-M:clj-surgeon/test-deps -e (run-tests mission-test)` | 0 | 0 of 7 |
| I-02 | 96 | `-M:clj-surgeon/test-deps -e (run-tests mission-test)` | 0 | 0 of 17 |
| I-03 | 1 | `-M:clj-surgeon/memory-test` | 0 | 0 of 2 |
| I-04 | 96 | `-M:clj-surgeon/test-deps -e (run-tests mission-display-test)` | 0 | 0 of 7 |

Three things the scorer must carry forward:

1. **The subset rule is not optional for T-01 and T-03.** T-01's `:sha` is a
   commit literally titled *"red: outline read-path allocation and parse-count
   witnesses"* — `outline-of-one-file-allocates-within-its-ceiling`, the very
   test T-01 asks a caller to annotate, is **already failing** at baseline.
   T-03 is red in 28 of 137. Score both as *no new failing test names*, never
   as absolute green.
2. **Exit codes are normalised by the printed summary, not by magnitude.** The
   repository's runners `System/exit` with their own `(fail + error)` COUNT,
   which for T-03 is 145 and collides with the signal range. Every `gate-v2`
   command therefore wraps the repository command in a `sh -c` that reads
   clojure.test's own summary: clean → 0, failures → 1, no summary at all
   (crash, refusal, kill) → the raw exit code. **Nothing about what runs
   changes**; only how the run reports itself.
3. **Two of these gates are suites, not namespaces.** T-01 and T-02 run 381 and
   493 tests because their commits offered no per-namespace entrance at all,
   and R-01/R-02 run the 806/654-test babashka suite because their namespaces
   live in `test/run_all.clj` rather than the JVM runner. Their baselines are
   correspondingly broad, so a NEW failure anywhere in those suites will be
   attributed to the task. That is a real sensitivity difference between tasks,
   and it belongs in the write-up beside the results.
