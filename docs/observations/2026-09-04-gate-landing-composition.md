# Gate landing — `bridge/admit-gate` aa82b42 onto the trunk `MCP/main` a730a54

Branch: **`MCP/gate-landing-2026-09-04`**. Composed 2026-09-04 by forge@anvil, as the integrator.
Nothing is pushed to `MCP/main` or `main` from here; the coordinator lands this branch after review.

This exists to satisfy condition **(b)** of the round-1 adversarial review
(`docs/observations/admit-gate-fix-round1-review-opus.md`, GO-WITH-FIX):

> (b) that witness is run and green **at the new tip on current `MCP/main`**, not on this
> branch's base — a GO is a claim about a base, and `MCP/main` has moved.

## Base proof, before any edit

```
git -C /home/forge/src/clj-surgeon fetch origin MCP/main bridge/admit-gate
git rev-parse origin/MCP/main          -> a730a547af46e5b1dd1897712b353179827e5104
git rev-parse origin/bridge/admit-gate -> aa82b4272f61cd36389a6dc43a3322bd53907530
git rev-parse --short=7 origin/bridge/admit-gate -> aa82b42          (the named tip)

~/bin/worktree-add /home/forge/src/clj-surgeon /home/forge/src/clj-surgeon-gateland \
    MCP/gate-landing-2026-09-04 origin/MCP/main
git -C /home/forge/src/clj-surgeon-gateland rev-parse HEAD
                                       -> a730a547af46e5b1dd1897712b353179827e5104   (equal)
```

Merge base of the two: `e5c4f46b326121611f5728dd4f84b8d56ebe722d`; the trunk carries **883**
commits since it. The gate branch forked at `17125fe`, *before* the six-lane integration
(receipt-ratchets/q5z, memory-battery, read-path, parser-admission, anvil-arms, txn-journal) and
before the andon hardening — which is why the eight conflicts exist at all.

## The branch

| sha | what |
|---|---|
| `0b6a494` | `merge --no-ff origin/bridge/admit-gate` — 8 conflicts, all union |
| `215acf8` | composition fix: the gate branch's own catalog witness had not met `alias_migration` |
| `1bb0c9d` | composition fix: register the admit lane in the trunk's drift ratchet; six tools moves two hot-reload counts |
| `fc2efb6` | this record |

**Pushed tip: `fc2efb6` (`MCP/gate-landing-2026-09-04`); last code commit `1bb0c9d`.** Every gate
figure below was measured at `1bb0c9d`; `fc2efb6` adds only this document. The tip was re-checked
after pushing and corrected here rather than by amending a pushed commit.

## Merge table — every conflict and its resolution

Not one conflict is a disagreement about behaviour. Every one is a **list both lanes appended
to**, so every resolution is a **union**, and each is proven by a before/after diff of the list
rather than by inspection.

| # | file | shape of the conflict | resolution | proof |
|---|---|---|---|---|
| 1 | `Makefile` | one `.PHONY` line; trunk added `repository-hygiene*`, `memory-*`, `anvil-arms-self-test`, `txn-kernel-warning-check`; gate added `admit-analyzer-memory-self-test` | trunk's line + `admit-analyzer-memory-self-test` after `clj-kondo-admission-path-self-test`; recipe bodies auto-merged | target lists: HEAD 96 / gate 86 / **composed 97**; `comm -23` "lost from HEAD" and "lost from GATE" both **empty**; added = `admit-analyzer-memory-self-test`. Same diff over the `.PHONY` names alone: both empty. |
| 2 | `src/clj_surgeon/mcp_tool.clj` (×2) | `:require` line, and the `:full` vector in `tools-for-profile` | both requires; both tools, `alias-migration-tool` then `admit-tool/admit-clojure-patch-tool` | `make mcp-smoke` returns the six-name catalog (below) |
| 3 | `src/clj_surgeon/mcp_server.clj` | `outcome-classes-by-tool` map, one entry each | both entries; `alias_migration` keeps its `;; @spec MCP-OP-ALIAS-001` marker | `mcp_operation_registry_test` `canonical-registry-and-independent-witness-catalog-match-exactly` green |
| 4 | `src/clj_surgeon/mcp_intent_contract.clj` | trunk **derives** `spec-files` from `spec-doc-paths` (a scan); gate re-lists a literal 7-entry vector that names `docs/intent/mcp-operation-contract/admit-clojure-patch-design.md` | **keep the trunk's derived scan.** Make the gate's document discoverable by it: `git mv admit-clojure-patch-design.md admit-clojure-patch-specs.md` (the scan's pattern is `.+-specs\.md`), and fix the one link to it in `mcp-operation-contract-specs.md` | audit finds it: `docs/intent/mcp-operation-contract/admit-clojure-patch-specs.md` in `spec-doc-paths`, `:ok true :violations []` |
| 5 | `test/clj_surgeon/mcp_test_runner.clj` (×2) | `:require` block and the `run-tests` argument list | union of both, in both places | namespace sets: HEAD 43 / gate 37 / **composed 44**; `comm -23` against each parent **empty** in both directions; added = `clj-surgeon.admit-patch-test`. The `:require` set and the `run-tests` set are identical to each other. |
| 6 | `test/mcp_stdio_smoke.clj` (×2) | the exact public tool catalog, asserted twice | six names in registration order; the assertion message "exactly five structural tools" → "six" | `make mcp-smoke` (below) |
| 7 | `test/clj_surgeon/mcp_server_test.clj` (×3) | `tools-for-profile :full` list, the `@spec MCP-OP-ALIAS-001` marker, the `make-tools` list | union; marker kept; `exposes-exactly-five-typed-tools` → `exposes-exactly-six-typed-tools` with `(= 6 (count tools))` | mcp-test suite green |
| 8 | `test/clj_surgeon/mcp_http_server_test.clj` (×3) | three tool-name lists in the live-reload session witness | union in all three | mcp-test suite green (after the two `:tool-count` integers in the same deftest were corrected — see `1bb0c9d`) |

Resolution 4 is the one with a judgement in it. Re-listing the gate's spec document in a literal
vector would have compiled and passed, and would have **undone the registry ratchet that made the
six-lane integration possible** — its whole point is that a new lane adds a FILE and touches no
shared line, so two lanes can never conflict on the registry again. Renaming the document to the
scanned suffix keeps the gate's specs audited and leaves the shared line untouched.

## The six holds

### (1) Every gate witness green at the composed tip

`clj-surgeon.admit-patch-test` alone, through the review's own runner
(`/tmp/gateland-fx/run-admit-test.sh`, a copy of `/tmp/gate-review-fx/run-admit-test.sh`):

```
~/bin/suite-run /tmp/gateland-fx/run-admit-test.sh /home/forge/src/clj-surgeon-gateland
Ran 114 tests containing 2122 assertions.
0 failures, 0 errors.                                              EXIT=0
```

114/2122/0 — the branch's own figure, reproduced on the trunk.

**R1–R4 (the review's blocking finding F1), at the composed tip.** Probe
`/tmp/gateland-fx/probe/probe_partial.clj`, fake runners `/tmp/gateland-fx/bin/runner-wrong-ns`,
`runner-zero-tests`, `runner-good-but-exit3`, `fake-test-runner-crash`, driven through
`execute-request!` with shipped defaults:

```
cd /home/forge/src/clj-surgeon-gateland && clojure -Sdeps '{:deps {nrepl/nrepl {:mvn/version "1.3.1"}}}' \
  -M /tmp/gateland-fx/probe/probe_partial.clj
```

| case | status | `detectors_not_run` | text names it | "clean bill" sentence | commit w/ `allow_partial` |
|---|---|---|---|---|---|
| R1 report names the wrong namespaces | `:partial` | `[{:detector "focused-tests", :reason :report-namespaces-do-not-match}]` | yes | yes | **refused** `:verification-incomplete` |
| R2 report says zero tests | `:partial` | `[{... :reason :no-test-evidence}]` | yes | yes | **refused** |
| R3 clean report, runner exits 3 | `:partial` | `[{... :reason :runner-exit-nonzero}]` | yes | yes | **refused** |
| R4 runner exits 3 writing nothing | `:unverified` | `[{... :reason :verification-runner-failed}]` | yes | yes | **refused** |

At the reviewed tip `7985b98` all four published `detectors_not_run []` with a text block naming
no detector. Verbatim text line now emitted on every one of them:

```
 | detectors that did not run: focused-tests (runner-exit-nonzero)
 | this receipt reports what was inspected; with a detector silent it is not a clean bill of health
```

**W1–W4 (the review's Finding 2, the `allow_partial` waiver), at the composed tip.** Probe
`/tmp/gateland-fx/probe/probe_waiver.clj`; `WROTE` is a re-read of `src/app/core.clj` off disk,
not a receipt field:

```
W1 commit allow_partial verify=focused dead-analyzer ok=false committed=false WROTE=false status=:unverified  reasons=[:clj-kondo-unavailable :no-focused-test-profile]
W2 commit allow_partial verify=none                  ok=false committed=false WROTE=false status=:unverified  reasons=[:verification-not-requested]
W3 commit             verify=none                    ok=false committed=false WROTE=false status=:unverified  reasons=[:verification-not-requested]
W4 commit allow_partial verify=focused live-analyzer ok=true  committed=true  WROTE=true  status=:partial     reasons=[:no-focused-test-profile]
```

W1 and W2 were `ok=true committed=true WROTE=true` at both `7985b98` and its base `17125fe`; they
are now refused. W4 — analyzer ran, profile genuinely absent, caller opted in — still writes, which
is the reviewer's own recommended predicate `(and allow-partial? profile-absent? (true? (:ran lint)))`.

### (2) The trunk's lanes intact

The strongest evidence is that the merge does not touch them. Every lane's production file is
**byte-identical to `a730a54`**:

```
git diff --quiet a730a54 HEAD -- <file>
UNCHANGED  src/clj_surgeon/core.clj                  (andon hardening, ls-tree)
UNCHANGED  src/clj_surgeon/parse_admission.clj       (parser-admission)
UNCHANGED  src/clj_surgeon/memory_battery_runner.clj (memory-battery)
UNCHANGED  src/clj_surgeon/txn_journal.clj           (txn-journal)
UNCHANGED  src/clj_surgeon/mcp_operation.clj         (receipt ratchets / measured partition)
UNCHANGED  test/run_all.clj                          (the bb suite's namespace list)
```

`git diff --stat a730a54 HEAD -- src/` touches ten files: three new (`form_identity.clj`,
`mcp_admit_tool.clj`, `patch_apply.clj`, `workspace_lock.clj`), the two conflict resolutions
(`mcp_server.clj` +4/-, `mcp_tool.clj` +7/-), and three the gate branch legitimately extends
(`mcp_change_buffer.clj` 12, `mcp_http_server.clj` 6, `mcp_process.clj` 59,
`mcp_write_refusal.clj` 55).

Lane witnesses, all green in the composed runs below: `clj-surgeon.parser-admission-test` and
`clj-surgeon.memory-battery-test` (bb suite); `clj-surgeon.txn-journal-test`,
`clj-surgeon.mcp-intent-contract-test`, `clj-surgeon.mcp-semantic-client-test`,
`clj-surgeon.mcp-inspect-contract-test`, `clj-surgeon.scope-stream-test`,
`clj-surgeon.outline-memory-test` (mcp-test suite — these carry the receipt-ratchet
text ⊇ structured assertions).

**The andon hardening, stated honestly.** `find-build-files` keeps `-H` / `-print0`
(`core.clj:262`, `:305-310`) and `ls-tree-root-refusal` is intact (`core.clj:700`) — both in a
file the merge does not touch. The "no `System/exit` outside `-main`" property is **not** literally
true on this trunk and never was (the third-landing record says the same: "base 12 hits"): `src`
has 13 `System/exit` sites, ten of them inside a `-main`, three inside `core.clj/run-ls-tree`
carrying an explicit `NOTE (inb-eca3b1)` that the library exit is owed a separate fix. What is
proven here is that **the merge introduces none**:

```
diff <(per-file System/exit counts at a730a54) <(per-file System/exit counts at HEAD)
IDENTICAL System/exit SITE COUNTS (no new site introduced by the merge)
```

### (3) The intent registry

One top-level derivation, not a list: `audit-current-repository` binds
`(let [spec-files (map #(io/file root %) (spec-doc-paths root))] ...)`, the trunk's form, kept.

```
clojure -M -e "(clj-surgeon.mcp-intent-contract/audit-current-repository \".\")"
:ok true :violations [] :spec-count 350 :duplicate-ids []
```

`spec-doc-paths "."` returns **18** documents (17 at `a730a54`, +
`docs/intent/mcp-operation-contract/admit-clojure-patch-specs.md`).

**Ids added: 94**, `MCP-OP-ADMIT-001 … MCP-OP-ADMIT-130` (94, not 130 — the scheme leaves gaps at
each group boundary: 006-009, 015-019, 025-029, 035-039, 045-049, 056-059, 072-079). Every one is
`:implemented` with both an implementation and a test witness; the audit reports zero violations,
so no `@spec` tag anywhere in `src` or `test` is unregistered and no registered id is unwitnessed.
`MCP-OP-ADMIT-121..130` — round 1's analyzer-truncation and `detectors_not_run` rungs and round
2's F1 / waiver / admission-type / `Throwable`-and-heap rungs — are all present:

```
{MCP-OP-ADMIT-121 :implemented, MCP-OP-ADMIT-122 :implemented, MCP-OP-ADMIT-123 :implemented,
 MCP-OP-ADMIT-124 :implemented, MCP-OP-ADMIT-125 :implemented, MCP-OP-ADMIT-126 :implemented,
 MCP-OP-ADMIT-127 :implemented, MCP-OP-ADMIT-128 :implemented, MCP-OP-ADMIT-129 :implemented,
 MCP-OP-ADMIT-130 :implemented}
```

They sit alongside the trunk's ids (`MCP-OP-ALIAS-*`, `MCP-OP-MEM-*`, `MCP-OP-POS-AUTH-*`,
`MCP-OP-SHELL-ARGV-*`, `MCP-OP-TIME-*`, `MCP-OP-EXIT-001`, …), 350 in all, no duplicate id.

The registration is not free and the trunk's ratchet made sure of it — see (Failure 2) below.

### (4) `mcp_test_runner` and the stdio smoke — UNION, diffed

```
require-block namespaces   HEAD 43   gate 37   composed 44
comm -23 HEAD composed  ->  (empty)   nothing dropped from the trunk
comm -23 gate composed  ->  (empty)   nothing dropped from the gate branch
comm -13 HEAD composed  ->  clj-surgeon.admit-patch-test
require set == run-tests set (the -main list), exactly
```

Stdio smoke, end to end against a live `-X:clj-surgeon/mcp-stdio` child:

```
make mcp-smoke
{:ok true, :operation :mcp-stdio-smoke, :server "clj-surgeon",
 :tools ["inspect_clojure" "apply_clojure_changes" "edit_clojure" "transform_clojure"
         "alias_migration" "admit_clojure_patch"],
 :response-count 3, :wall-ms 7332.207117}                          EXIT=0
```

### (5) `Makefile` — UNION, diffed

```
target lists     HEAD 96   gate 86   composed 97
lost from HEAD  -> (empty)
lost from GATE  -> (empty)
added vs HEAD   -> admit-analyzer-memory-self-test
.PHONY names: the same two diffs, both empty
```

So the gate's `admit-analyzer-memory-self-test` is present and every trunk target — including
`memory-battery`, `memory-battery-generate`, `memory-battery-reference`,
`memory-battery-self-test`, `memory-red`, `memory-red-kernel`, `anvil-arms-self-test`,
`txn-kernel-warning-check`, `repository-hygiene`, `repository-hygiene-self-test` — survives.

*Cosmetic, pre-existing, not fixed here:* the `help` text still says "four-tool discovery" for
`mcp-smoke`. It was already stale at `a730a54` (five tools) and is not this composition's to change.

### (6) `make admit-analyzer-memory-self-test` at `-Xmx512m`

```
clojure -J-Xms64m -J-Xmx512m -Sdeps '{:deps {nrepl/nrepl {:mvn/version "1.3.1"}}}' -M test/admit_analyzer_memory_selftest.clj
PASS n=100   findings=600   analyzer-bytes=83606   ran=true introduced=300   heap-start-MiB=24 heap-peak-MiB=35 budget-MiB=409 max-heap-MiB=512 wall-ms=55
PASS n=1000  findings=6000  analyzer-bytes=847706  ran=true introduced=3000  heap-start-MiB=23 heap-peak-MiB=79 budget-MiB=409 max-heap-MiB=512 wall-ms=174
PASS n=10000 findings=60000 analyzer-bytes=8596706 ran=true introduced=30000 heap-start-MiB=23 heap-peak-MiB=81 budget-MiB=409 max-heap-MiB=512 wall-ms=1134
admit-analyzer-memory-self-test: 3/3 arms passed at -Xmx512m       EXIT=0
```

This is the receipt the review asked for in place of the 16 MiB argument, on the composed tree at
the heap the server actually runs with (`MCP_JAVA_OPTS ?= -J-Xms64m -J-Xmx512m`).

## Failures found by the composition, and their fixes (RED → GREEN, none skipped)

### Failure 1 — the gate branch's own catalog witness had not met `alias_migration` (`215acf8`)

Attributed to **`bridge/admit-gate`**: the assertion is that branch's, added with the tool, and it
names a five-tool catalog because the branch forked before `alias_migration` was registered.

RED at `0b6a494`:

```
FAIL in (registers-one-admit-tool-in-the-full-profile) (admit_patch_test.clj:278)
expected: (= ["inspect_clojure" "apply_clojure_changes" "edit_clojure" "transform_clojure" "admit_clojure_patch"] names)
  actual: (not (= [...] ["inspect_clojure" "apply_clojure_changes" "edit_clojure" "transform_clojure" "alias_migration" "admit_clojure_patch"]))
Ran 114 tests containing 2122 assertions.
1 failures, 0 errors.
```

The gate's actual intent, MCP-OP-ADMIT-001 ("ONE admit tool in the full profile"), is the assertion
one line above and was never in doubt. The stale neighbour was **widened, not deleted**, so the
list stays a catalog witness. GREEN: `114 / 2122 / 0 failures, 0 errors`.

### Failure 2 — the admit lane was not registered in the trunk's drift ratchet (`1bb0c9d`)

Attributed to **the composition**: the trunk's `mcp_intent_contract_test` asserts the derived scan
against a witness-side registry (`expected-spec-docs`, `lanes-added-since-derivation`) precisely so
that a new leaf cannot enter silently. It fired exactly as designed.

RED at `215acf8`, `~/bin/suite-run clojure -M:clj-surgeon/mcp-test` — `Ran 711 tests containing
8429 assertions. 4 failures, 1 errors.`:

```
FAIL in (the-derived-spec-doc-set-matches-the-expected-set-exactly) (mcp_intent_contract_test.clj:222)
drift in docs/intent is visible here, not silent
  ... "docs/intent/mcp-operation-contract/admit-clojure-patch-specs.md" ...
FAIL in (the-derived-audit-covers-exactly-the-registered-lane-intents) (mcp_intent_contract_test.clj:233)
deriving the list changed WHICH FILES are scanned, not WHICH INTENTS are audited
added: ("MCP-OP-ADMIT-001" ... "MCP-OP-ADMIT-130") removed: ()
```

Fixed by registering the one path in each of the two witness vectors — 94 ids enter the audit.

### Failure 3 — six tools moves two hot-reload counts (`1bb0c9d`, same commit)

Attributed to **the composition**. The merge unioned the tool-*name* sets in
`one-http-session-observes-live-tool-add-replace-and-remove` but not the two integers in the same
deftest:

```
FAIL in (one-http-session-observes-live-tool-add-replace-and-remove) (mcp_http_server_test.clj:363)
expected :tool-count 6 ... actual :tool-count 7
FAIL in (one-http-session-observes-live-tool-add-replace-and-remove) (mcp_http_server_test.clj:388)
expected :tool-count 5 ... actual :tool-count 6
```

Base catalog 6 → add-one 7, restore 6. GREEN with the rest of the suite.

### Not a failure of either parent — one ERROR from a full `/tmp`

```
ERROR in (a-walk-above-the-entry-ceiling-refuses-before-the-filtered-set-is-built) (UnixFileSystem.java:-2)
  actual: java.io.IOException: No space left on device
    at clj_surgeon.mcp_alias_migration_test/bulk-non-source-files! (mcp_alias_migration_test.clj:949)
```

`/tmp` on Anvil is a **16 GiB tmpfs shared by every seat** and reached 100% during that run (the
integrator's own fixture copy contributed 387 MB and was cut to 9.4 MB). Not reproduced once space
was available; the re-run is green. Operational note for anyone else composing here: keep fixture
copies out of `/tmp`, or copy only `bin/` and `probe/`.

## Gates — ran-lines, verbatim, at the code tip `1bb0c9d`

All JVM suites through `~/bin/suite-run`; `clojure -M:clj-surgeon/mcp-test` invoked directly,
never `make mcp-test`. No server started on any port; `make mcp-smoke` speaks to a stdio child
with `:nrepl-port :none`.

| gate | command | ran-line | exit |
|---|---|---|---|
| babashka suite | `~/bin/suite-run bb test/run_all.clj` | `Ran 814 tests containing 6724 assertions.` / `0 failures, 0 errors.` | 0 |
| MCP JVM suite | `~/bin/suite-run clojure -M:clj-surgeon/mcp-test` | `Ran 711 tests containing 8436 assertions.` / `0 failures, 0 errors.` | 0 |
| admit gate alone | `~/bin/suite-run /tmp/gateland-fx/run-admit-test.sh <worktree>` | `Ran 114 tests containing 2122 assertions.` / `0 failures, 0 errors.` | 0 |
| Prolog oracle | `make mcp-operation-oracle` | `mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]` | 0 |
| intent audit | `clojure -M -e "(clj-surgeon.mcp-intent-contract/audit-current-repository \".\")"` | `:ok true :violations [] :spec-count 350` | 0 |
| kernel warnings | `make txn-kernel-warning-check` | `kernel warning check: 2 namespace(s), 0 warning(s)` | 0 |
| battery self-test | `make memory-battery-self-test` | `generate_tree self-test: ok` / `Ran 30 tests containing 164 assertions.` / `0 failures, 0 errors.` | 0 |
| parser-admission green witness | `make memory-red PARSER_RED_EXPECT=green` | `memory-red: 6/6 assertions held (expect=green)` | 0 |
| kernel OOM witness | `make memory-red-kernel` | `Ran 4 tests containing 25 assertions.` / `0 failures, 0 errors.` (`heap-used-peak-mb 253.3` at `xmx-mb 256.0`) | 0 |
| admit analyzer memory | `make admit-analyzer-memory-self-test` | `admit-analyzer-memory-self-test: 3/3 arms passed at -Xmx512m` | 0 |
| anvil arms | `bash bench/anvil-arms/self-test.sh` | `anvil-arms self-test: 389 passed, 0 failed` | 0 |
| stdio smoke | `make mcp-smoke` | `{:ok true, :operation :mcp-stdio-smoke, ... :tools [six names] ...}` | 0 |

### Two expectations in the brief that did not hold — both in the good direction

* **The 5 pre-existing `agent_routing_test.clj:106` failures are gone.** The brief said to report
  the set unchanged; the composed bb suite is `814 / 6724 / 0 failures, 0 errors`, and
  `clj-surgeon.agent-routing-test` did run (line 6 of the log). They were fixed on the trunk
  between the review's base and `a730a54`. `test/run_all.clj` is byte-identical to `a730a54`, so
  the namespace set is the trunk's.
* **The pre-existing `mcp_change_buffer_test.clj:686` failure is gone** (the hard-coded
  `/opt/homebrew/bin/clj-kondo` path). The composed mcp-test suite is `711 / 8436 / 0`. Same
  reason. Neither suite now runs non-green, which removes the review's own stated masking risk:
  "the mcp-test suite already runs non-green, so '1 failure' is the pass condition."

### One flake, named, with both runs quoted

`make memory-red PARSER_RED_EXPECT=green` failed once, then passed:

```
run 1 (load 5.6, mcp-test suite concurrent)
FAIL   giant 128m: admission scan under 50 ms         {:wall-ms 110, :scan-ms 52}
memory-red: 5/6 assertions held (expect=green) — FAIL

run 2 (load 4.6, box quiet)
PASS   giant 128m: admission scan under 50 ms         {:wall-ms 100, :scan-ms 14}
memory-red: 6/6 assertions held (expect=green)
```

This is the **already-filed** parser-admission wall-clock flake `inb-5f98d7`
(`docs/observations/2026-09-03-captains-log-anvil-seat.md`, 02:19Z: "the memory-red 50 ms wall
assertion flaked 3/4 at load 8–14 on byte-identical inputs: a gate that cannot tell a regression
from a busy box"). Attribution here is airtight rather than inferred:
`src/clj_surgeon/parse_admission.clj` is **byte-identical to `a730a54`** on this branch, so the
composition cannot have moved that number. The gate's owner still owes it reps-and-best, the way
MEM-003 round 2 fixed its own.

## What is NOT claimed

* This is the composition and its evidence. **It is not a fresh-clone gate pass** — every figure
  above was measured in the worktree `/home/forge/src/clj-surgeon-gateland`, which is clean but is
  not a clone. Per the ambient-state lesson, whoever lands this should run the gates once on a
  fresh clone of `1bb0c9d` before merging.
* The full `make memory-battery` was not run: it is not in this landing's gate list, it needs an
  explicit `MEMBAT_ROOT` and the exclusive `suite.lock`, and nothing on this branch touches
  `memory_battery_runner.clj`. `memory-battery-self-test` (which IS wired into `make test`) is green.
* Conditions (a) and (b) of the review are satisfied here. **Condition (c) — filing Finding 2 and
  Finding 3 as beads before the merge — is the coordinator's, not done on this branch.** Note that
  round 2 of the gate branch appears to have *closed* Finding 2's authorisation hole in code (W1
  and W2 now refuse), so the bead should record it as fixed-with-a-witness rather than open.
