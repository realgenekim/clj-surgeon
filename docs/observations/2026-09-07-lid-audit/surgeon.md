# LID / HLD integration audit — clj-surgeon, origin/MCP/main

**Scope.** Read-only audit of `/home/forge/src/clj-surgeon-records` at the trunk tip.
No edits, no commits, no checkouts, no pushes, no servers, no battery, no MCP tool calls.

| field | value |
|---|---|
| checkout | `/home/forge/src/clj-surgeon-records` |
| branch | `records/MCP-main` |
| HEAD | `9c6aeb7ac54a182de8352f4705917d7354077250` (= `origin/MCP/main`, verified equal) |
| HEAD subject | `records: LID fixes (Sol pass) reports; Astra reviews launched on the Astra model` (forge-anvil, Mon Sep 7 16:45:26 2026 +0000) |
| commits touching `src/` since 2026-08-15 | **577** |
| author identities in that window | forge-anvil 398 · Gene Kim 80 · Codex Astra 29 · Astra 24 · forge-bridge 15 · surgeon1 12 · surgeon2 10 · sol 3 · mayor 2 · builder 2 · surgeon@anvil 1 · builder2 1 |
| audit date | 2026-09-07 |

---

## 1. Gate result — the integration evidence

Both gates were run ONCE on the current trunk tip.

| gate | command | result |
|---|---|---|
| MCP operation contract oracle (Prolog, rung d) | `make mcp-operation-oracle` | **PASS**, exit 0. `mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]` |
| Fast lane, incl. the bidirectional intent contract | `clojure -M:clj-surgeon/test-fast` | **PASS**, exit 0. `Ran 573 tests containing 5595 assertions. 0 failures, 0 errors. 0 preconditions skipped. 0 preconditions failed.` 50 namespaces, including `clj-surgeon.mcp-intent-contract-test`. Trailer: `test-isolation: 0 violations across 50 namespace(s) (TEST-ISO-002/003/004/005/007/010)` |

Logs: `/var/tmp/forge/lid-audit/oracle.log`, `/var/tmp/forge/lid-audit/test-fast.log`.

### What the green actually certifies

The gate is `clj-surgeon.mcp-intent-contract/audit-current-repository`
(`src/clj_surgeon/mcp_intent_contract.clj`), asserted by
`repository-operation-intent-contract-is-coherent` in the fast lane. It:

1. **Derives** the audited registry by scanning `docs/intent/<leaf>/<name>-specs.md`
   (`spec-doc-paths`) — no hand-kept vector, so lanes cannot conflict on a shared line.
   An `excluded-spec-docs` entry naming a missing file throws `:orphan-spec-doc-listing`;
   an empty scan throws `:no-spec-docs-found`.
2. Parses rows with `#"(?m)^- \[([ xD])\] \*\*(MCP-OP-[A-Z0-9-]+)\*\*:"` →
   `:active-gap` / `:implemented` / `:deferred`.
3. Harvests witnesses with `#"@spec\s+(MCP-OP-[A-Z0-9-]+)"` from
   **`src/**.clj{,c,s}` plus `Makefile`** (implementation) and
   **`test/**.clj{,c,s}` plus `test/**.pl`** (test).
4. Fails on: `:missing-test-witness` (active-gap), `:missing-implementation-witness` +
   `:missing-test-witness` (implemented), `:unknown-intent-witness` (either direction).

**So: every `MCP-OP-*` intent row on trunk is bidirectionally linked, by construction.**
That is real, and it is the reason the 577-commit backlog does not need commit-by-commit
re-derivation for the covered surface. **Anything below is what that green does NOT cover.**

Two leaves are *self-declared* holes, each with a named reason in `excluded-spec-docs`
(honest, but still holes): `embedded-elaborator` (19 `MCP-OP-ELAB` active gaps, red ns not in
tree) and `substantiation-telemetry` (19 `MCP-OP-SUBST` rows marked `[x]` to record advance
ratification, with neither implementation nor test witnesses).

---

## 2. Counts

### 2.1 Spec rows

| | rows |
|---|---|
| total spec rows across `docs/intent/**/*-specs.md` (unique ids) | **721** |
| rows the contract regex can see (`MCP-OP-*`) | **556** |
| rows the contract regex **cannot see** | **165 (23%)** |

The invisible 165, by file:

| spec file | rows | id prefix |
|---|---|---|
| `docs/intent/worktree-lifecycle/worktree-lifecycle-specs.md` | 53 | `WTL-APPLY/PRUNE/SEAL/INV/PLAN/HAND/CLI` |
| `docs/intent/performance-regression-sentinel/performance-regression-sentinel-specs.md` | 50 | `PERF-SENT-*` |
| `docs/intent/operation-algebra/operation-algebra-specs.md` | 39 | `OP-ALG-*` |
| `docs/intent/test-isolation/test-isolation-specs.md` | 19 | `TEST-ISO-*`, `TEST-ISO-RACE-*` |
| `docs/intent/2026-08-29-ratification/measurement-evidence-specs.md` | 4 | `MEASURE-WALL/EVID` |

### 2.2 Source marker coverage

| | files |
|---|---|
| `src/**/*.clj{,c,s}` | **101** |
| carrying any `@spec <ID>` marker | **62** |
| carrying **no** marker at all | **39** |
| `src` files changed since 2026-08-15 (still present) | **91** |
| of those, changed **and** carrying no marker | **29** |
| (`MCP-OP-` markers only: 57 marked / 44 unmarked / 34 changed-and-unmarked) |  |

### 2.3 Witness-side facts

- **633** distinct ids appear as `@spec` witnesses across `src`, `test`, `Makefile`, `bench`, `bin`.
- **17 ids have their ONLY implementation witness in the `Makefile`**, not in any source file:
  `MCP-OP-ADMIT-130`, `MCP-OP-ADMIT-150`, `MCP-OP-ALIAS-036`, `MCP-OP-ALIAS-053`,
  `MCP-OP-ANALYZER-008`, `MCP-OP-ORACLE-001`, `MCP-OP-TMPHYG-001/002/003/004/006/007/008/010/011/012/013`.
- **1 genuinely dangling witness**: `TELEMETRY-EVENTS-001`, asserted as an intent id in the
  namespace docstring of `src/clj_surgeon/telemetry_events.clj` and in `Makefile`
  (`study-agent-events`), with **no spec row anywhere in `docs/intent/`**. It is invisible to
  the gate purely because the harvest regex is `MCP-OP`-scoped. (The other two "dangling" hits,
  `PERF-SENT-` and `TEST-ISO-`, are regex artifacts of run-together id lists like
  `TEST-ISO-002/003/004`.)

---

## 3. What the gates do NOT cover — four structural holes

### H1. The contract is `MCP-OP`-only; 165 rows and ~350 markers live outside it (**highest severity**)

Both regexes in `mcp_intent_contract.clj` hardcode `MCP-OP-`. Five whole intent leaves —
`worktree-lifecycle`, `performance-regression-sentinel`, `operation-algebra`,
`test-isolation`, `2026-08-29-ratification` — use other prefixes, so for those 165 rows:

- a **missing** implementation or test witness is invisible (no `missing-*-witness` can fire);
- a **typo'd, renamed, or deleted** id in a `@spec WTL-…` / `@spec OP-ALG-…` marker is invisible
  (no `unknown-intent-witness` can fire).

This is exactly intent evaporation with a green suite — the failure mode the LID skill exists
to prevent. It is also the cheapest fix in this report: broaden both patterns to
`#"^- \[([ xD])\] \*\*([A-Z][A-Z0-9-]+)\*\*:"` and `#"@spec\s+([A-Z][A-Z0-9-]+)"`, expect a
first run to go loudly red, and triage the fallout. **Do it behind a linked intent of its own**
(draft: `MCP-OP-TRACE-005`, below) so the narrowing cannot silently come back.

### H2. The only `PERF-SENT` traceability gate is orphaned — never run by any suite

`test/performance_regression_sentinel_intent_test.sh` is a correct, real bidirectional check
(spec rows → `@spec` witnesses in three named bench/test files, with `comm -23`/`comm -13` for
both missing and unknown). It is invoked only by `make performance-regression-sentinel-test`,
and **that target is referenced nowhere except its own definition, the `.PHONY` line, and the
`help` text.** It is not a dependency of `test`, `test-full`, `test-fast`, `mcp-test`, or
`landing-gate`.

> LID skill, verbatim: *"A gate you have to remember to run is not a gate — an oracle only
> invoked by hand is a diary entry."* Same class as the 2026-08-21 sessionize-sched-killer
> finding. Wire it into `test-full` at minimum.

`worktree-lifecycle-test` / `worktree-lifecycle-recovery-test` are likewise not dependencies of
`test` or `test-full`; the WTL *behavioural* tests are picked up by the ordinary lane runner
(`test/clj_surgeon/worktree_lifecycle*_test.clj`), but there is **no traceability check for the
53 `WTL-*` rows at all**, in any lane.

### H3. `OP-ALG` (39 rows) and `TEST-ISO` (19 rows) have no traceability gate either

`OP-ALG-*` markers exist in `test/clj_surgeon/operation_algebra_test.clj`,
`mcp_contract_test.clj`, `intent_transaction_test.clj` and `bench/*` — but nothing links the 39
spec rows to them. For `TEST-ISO`, `mcp_test_runner.clj` prints a *behavioural* verdict
(`test-isolation: 0 violations …`) naming six ids — `TEST-ISO-002/003/004/005/007/010` — which
is a strong rung-e style check for those six, and no coverage statement at all for the other 13.

### H4. The `Makefile` counts as an implementation source

`read-sources root ["Makefile"] ["Makefile"]` means a `# @spec MCP-OP-…` comment line in a make
recipe satisfies `missing-implementation-witness`. For the 13 `TMPHYG` / `ADMIT-130` / `ADMIT-150`
ids this is defensible — the promise really is "this target runs this thing" — but it is a
loophole a future lane can use to green a spec row with a comment. Worth an explicit rule in
the leaf docs: a Makefile-only witness is legitimate **only** when the intent's observable
outcome is the target's execution, and the target must itself be reachable from `test` or
`test-full` (which `MCP-OP-ORACLE-001` and the TMPHYG set are, and H2's sentinel is not).

---

## 4. The ranked gap list — load-bearing promises with no linked witness

29 changed-and-unmarked source files, grouped by topic. The dominant cluster is **the mission
ledger subsystem: 16 namespaces, ~4,100 LOC, zero linked intents, and every one of them in the
LID-REQUIRED classes** (delivery, idempotent effects, allocation, identity, recovery,
authorization). It is a public entrance: `bin/mission-read.clj` (babashka), `mission_cli` six
verbs, git publication.

Ranking is by blast radius = (public reachability × mutation authority × fan-in × churn).

| # | file | LOC | commits since 08-15 | class | why it needs an intent |
|---|---|---|---|---|---|
| 1 | `mission_git_ledger.clj` | 270 | 2 | delivery + idempotent effect | "Saved owner_forms receipt to Git. No caller-supplied proof authority." A receipt written to git twice, or written from caller-supplied proof, is unrecoverable and silent. |
| 2 | `mission_typist_executor.clj` | 440 | 13 | mutation authority | "Frozen plan authority, staged proof, guarded commit." Three separable promises behind one flag; a refactor can narrow any of them with every test green. |
| 3 | `mission_cli.clj` | 715 | 25 | authorization / dispatch | The public entrance, six verbs, highest churn in the repo's unmarked set, three seats editing it. |
| 4 | `mission.clj` | 1068 | 14 | identity + recovery | The durable ledger object; required by 16 src namespaces. Ledger identity is precisely the "positional ordinal inside an immutable identity tuple" failure class. |
| 5 | `mission_candidate_race.clj` | 87 | 1 | allocation / concurrency | "Owned, bounded candidate requests; completion order never implies quality." That sentence IS an EARS row waiting to be written; it is also the single most plausible thing a later optimiser breaks. |
| 6 | `mission_git.clj` | 161 | 4 | delivery | "Explicit-stage Git publication of a verified mission. No source writes." |
| 7 | `telemetry_events.clj` | 394 | 11 | append-only ledger | Box-wide JSONL ledger; **already names `TELEMETRY-EVENTS-001`, an id with no registry row** (see §2.3). Fastest win in the table: create the row, or retire the id. |
| 8 | `mission_events.clj` | 170 | 4 | delivery | "Function-emitted completion events for the public mission boundaries" — an event that stops being emitted is invisible by definition. |
| 9 | `workspace_onboarding.clj` | 546 | 13 | installation authority | Installs a project-local Codex entrance into a repo. Three seats (Gene Kim, forge-bridge, forge-anvil) have edited it; scope creep here writes into other people's repos. |
| 10 | `spawn_ledger.clj` | 63 | 1 | append-only ledger | Docstring asserts `TEST-ISO-002` in **prose only** — no `@spec` marker, and `TEST-ISO` is outside the gate anyway (H1). Fan-in 6. |
| 11 | `mission_git_process.clj` | 51 | 3 | shell safety + deadline | "One bounded subprocess including stdin delivery; argv only. No shell expansion. One monotonic deadline." The repo already has a `shell-argv-safety` leaf (7 rows, all `[x]`) — this file re-implements the same promise **unlinked**. Link it to the existing leaf rather than writing new rows. |
| 12 | `recovery.clj` | 89 | 1 | recovery | "One bounded reset button for the shared structural tool stack." A reset button whose bound erodes is the classic silent-widening defect. |
| 13 | `mcp_recovery.clj` | 279 | 1 | recovery | Bounded Streamable HTTP probes behind the CLI recovery entrance; boundedness is the promise. |
| 14 | `mission_forms_source.clj` | 393 | 4 | data preservation | "Comment-preserving source lowering." Comment loss is invisible to a compile and to most tests. |
| 15 | `mcp_formatter.clj` | 83 | 1 | ordering / effect | "Format staged candidate sources **before** a transaction writes live files" — an ordering promise, exactly the kind a refactor reorders. |

Runners-up (real but lower blast radius): `diagnostic_delta.clj` (scoring class — verifier
diagnostic comparison across a transaction), `mcp_semantic_client.clj` (restart-safe client
lifecycle), `mission_candidate.clj` (frozen-snapshot anchoring), `mission_fallback.clj`
("never a native edit or proof of adoption" — an anti-claim worth pinning), `mission_display.clj`,
`mission_plain_forms.clj` ("never evaluates or writes" — an eval fence, and eval fences are a
standing security-review class on this seat), `mission_usage.clj`, `edit_dsl.clj`,
`extract_header.clj`, `quoted_var_refs.clj`, `syntax_var_refs.clj`, `forward_refs.clj`,
`cljc/require_ops.clj`.

Note `mission_plain_forms.clj` — "never evaluates" is a *fence*, and the strongest ratchet for a
fence is rung e (typed refusal), not a comment. It is listed as a runner-up only because it is
already a pure decoder; if it grows an eval path it jumps to #1.

### Drafts — EARS rows and witness shapes

Proposed new leaf `docs/intent/mission-ledger/mission-ledger-specs.md`, ids `MCP-OP-MISSION-0NN`
(the `MCP-OP-` prefix keeps them inside the existing gate on day one; do NOT invent a
`MISSION-` prefix until H1 is fixed, or the new rows are born invisible).

| draft id | EARS row | misreadings to write down first | witness shape (rung) |
|---|---|---|---|
| `MCP-OP-MISSION-001` | While a mission has a saved `owner_forms` receipt, when git publication is requested a second time for the same receipt, the ledger shall publish exactly one commit and return the existing commit's identity. | "Publish again and let git dedupe." · "Treat a caller-supplied sha as proof the receipt is already published." | **c** — state-space over {absent, staged, committed, committed-then-amended} × {first call, retry, concurrent call}; assert commit count is 1 and the returned id is byte-equal across calls. Hand-written expected ids (independence). |
| `MCP-OP-MISSION-002` | While a typist plan is frozen, when the executor commits, the ledger shall commit only the file set named in the frozen plan. | "Commit the working tree." · "Re-derive the file set from the diff at commit time." | **e** then **b** — typed refusal `:plan-file-set-mismatch` in the executor; example test drives one out-of-plan file present in the tree and asserts the refusal, not a filtered commit. |
| `MCP-OP-MISSION-003` | While two candidate requests are in flight for one mission, when the first completes, the ledger shall select by the recorded quality relation and never by completion order. | "First done wins." · "Cancel the slower request and treat its absence as a quality signal." | **c** — exhaustive over 2 and 3 candidates × all completion orders × equal/unequal quality; the failure message names the exact interleaving. |
| `MCP-OP-MISSION-004` | When a mission boundary function completes, the ledger shall emit exactly one completion event naming that boundary. | "Emit on entry too, and let consumers pair them." · "Suppress the event when the boundary returned a refusal." | **b + c** — capture the event sink; assert one event per boundary across success, refusal, and exception paths. |
| `MCP-OP-MISSION-005` | While a mission subprocess is running, when the monotonic deadline elapses, the process shall be terminated and the result reported as a named timeout refusal. | "Return partial stdout as success." · "Measure the deadline with wall-clock time." | **b** — a fixture child that outlives the deadline; assert the named refusal and that no partial capture is returned. Link to the existing `shell-argv-safety` leaf rather than duplicating its argv rows. |
| `MCP-OP-MISSION-006` | While lowering owner-keyed candidate source, when a replacement is applied, the lowering shall preserve every comment outside the replaced span. | "Preserve comments the reader printed back." · "Round-trip through the reader and accept whatever comes out." | **c** — generative over comment positions (leading, trailing, inline, between forms, in the replaced span itself) with hand-written expected output. |
| `MCP-OP-TRACE-005` | While the intent contract audits the repository, when any spec document row uses an id prefix other than `MCP-OP-`, the audit shall include that row and its witnesses. | "Add the other prefixes to a second list." · "Skip rows whose leaf has its own gate." | **b + e** — fixture leaf with a `WTL-`-shaped row and a dangling `@spec WTL-…` witness; assert both `missing-*-witness` and `unknown-intent-witness` fire. This one is the ratchet that keeps H1 from returning. |
| `MCP-OP-TRACE-006` | While a spec document declares a traceability gate script, when the ordinary suite runs, that script shall execute. | "Run it in CI only." · "A `.PHONY` listing is wiring." | **b** — assert `test-full`'s recipe reaches `performance-regression-sentinel-test`; keep it as a Makefile-text assertion only if the target is also proven reachable, per H4. |

---

## 5. HLD — `docs/high-level-design.md` (1,194 lines)

Structure: Problem · Approach · Target Users · Goals · Non-Goals · Tenets · System Design
(8 subsections) · Key Design Decisions (12) · Success Metrics · References.

### 5.1 Intent leaves the HLD does not mention

Verified by keyword grep, not by leaf-name matching (the HLD names designs, not directories).
**15 of 27 leaves, carrying 300+ spec rows, have no HLD presence:**

| leaf | spec rows | HLD keyword hits |
|---|---|---|
| `alias-migration` | 67 | 0 (`alias migration`, `alias_migration`) |
| `feature-thread` | 52 | 0 (`feature thread`) |
| `relation-census` | 35 | 0 (`census`) |
| `helper-extraction` | 25 | 0 (`helper`) |
| `embedded-elaborator` | 19 | 0 |
| `substantiation-telemetry` | 19 | 0 (`telemetry` appears once, unrelated) |
| `test-isolation` | 19 | 0 |
| `prepared-request-actions` | 18 | mentioned as "prepared", thin |
| `temp-dir-hygiene` | 13 | 0 (`tmpdir`, `temp directory`) |
| `insertion-boundary-and-gap` | 10 | 0 |
| `sibling-pair-edit` | 10 | 2 (`sibling`) — thin |
| `write-refusal-completeness` | 8 | 0 |
| `shell-argv-safety` | 7 | 1 (`argv`) — thin |
| `read-request-normalization` | 5 | present via `normaliz` |
| `hot-verification` | 2 | 0 |

The three biggest — `alias-migration` (67 rows, and one of the two *automatically routed* classes
in this seat's doctrine), `feature-thread` (52), `relation-census` (35) — are the flagship public
operations. **The HLD does not describe the ops the fleet is told to route to.** That is the
single largest HLD omission.

### 5.2 Source areas whose promises the HLD omits

1. **The entire mission ledger subsystem** — 16 namespaces, ~4,100 LOC, a babashka read path, a
   six-verb CLI, git publication, a typist executor with commit authority. Absent.
2. **A live naming collision, and it is dangerous.** The HLD uses "mission" 28 times, always for
   the *read* mission (§"Compress a coherent read mission without guessing": "Let a caller state
   one bounded read mission"). `src/clj_surgeon/mission.clj` calls itself "The MISSION LEDGER: a
   durable, plain-EDN object for one bounded intent." Two unrelated subsystems, one word, and the
   HLD documents only the one that has no `mission*.clj` file. A reader who greps the HLD for
   "mission" gets a confident wrong answer. Rename one, or disambiguate in the HLD ("read mission"
   vs "mission ledger") in the same pass that adds §5.2.1.
3. **The two ledgers** — `telemetry_events.clj` (box-wide append-only JSONL, appended as a side
   effect by public MCP fns) and `spawn_ledger.clj` (append-only record of every child process).
   These are the observability substrate the fleet's measurement claims rest on; the HLD's
   Success Metrics section does not say where the numbers come from.
4. **`workspace_onboarding.clj`** — installs a project-local Codex entrance into a caller's repo.
   Write authority into third-party repositories, undocumented at design level.
5. **Recovery** — `recovery.clj` ("one bounded reset button for the shared structural tool stack")
   and `mcp_recovery.clj`. The HLD covers admission and verification but not reset.
6. **`performance-regression-sentinel`** — this one IS in the HLD (§"Detect regressions with an
   adaptive paired sentinel", §"Spend counterbalance only on regression suspicion"), so its
   50-row leaf is design-covered; it is only the *gate wiring* that fails (H2). Recorded here so
   the two findings are not conflated.

### 5.3 Opportunistic HLD additions, ranked

1. A "Publish a verified mission ledger" System Design section (covers gaps 1, 2, 3 above) —
   and disambiguate "mission" while writing it.
2. A Key Design Decision for each routed public op: alias migration, feature thread, relation
   census, helper extraction. Four short sections close 179 spec rows of design silence.
3. One paragraph in Success Metrics naming the telemetry and spawn ledgers as the evidence source.
4. A line in §"Gate durable intent in the ordinary suite" stating the contract's prefix scope —
   which, once H1 is fixed, becomes "every id prefix", and until then is the honest statement of
   what the green means.

---

## 6. Verdict

**Integrated, for the 556 `MCP-OP-*` rows: yes, by construction, and the evidence is the two
green gates above.** The derived-registry design (a new lane adds a *file*, not a line) is why
577 commits from 12 identities did not produce a traceability conflict, and it is the strongest
thing in this repo's LID practice.

**Not integrated, in descending order:**

1. **H1** — 165 spec rows and ~350 markers are outside the contract's regexes. Cheapest fix,
   highest value, needs its own intent (`MCP-OP-TRACE-005`) so the narrowing cannot return.
2. **The mission ledger subsystem** — 16 namespaces of delivery/effect/allocation/recovery code
   with zero linked intents, on the public entrance, at the highest churn in the repo.
3. **H2** — the `PERF-SENT` traceability gate exists, is correct, and is never executed.
4. **HLD** — 15 leaves and the whole mission subsystem undocumented, plus an active "mission"
   naming collision.

None of this contradicts the gates; all of it is *outside* them. That distinction is the whole
finding: **the green proves what it can see, and it can see 77% of the rows and 61% of the
source files.**

---

## 7. Raw commands

```bash
cd /home/forge/src/clj-surgeon-records
export LC_ALL=C

# provenance
git rev-parse --abbrev-ref HEAD; git log -1 --format='%H %an %ad %s'
git log --oneline -1 origin/MCP/main
git log --since=2026-08-15 --oneline -- src | wc -l
git log --since=2026-08-15 --format='%an' -- src | sort | uniq -c | sort -rn

# gates (run once, on the tip)
make mcp-operation-oracle                     # -> exit 0
clojure -M:clj-surgeon/test-fast              # -> 573 tests / 5595 assertions / 0 fail / exit 0

# marker coverage
find src -name '*.clj*' | sort > src-all.txt
rg -l '@spec\s+[A-Z]' src --glob '*.clj*' | sort > src-marked-any.txt
comm -23 src-all.txt src-marked-any.txt > src-unmarked-any.txt
git log --since=2026-08-15 --name-only --format='' -- src \
  | grep -E '^src/.*\.clj' | sort -u > src-changed.txt
comm -12 src-changed.txt src-unmarked-any.txt          # -> 29 files

# spec-row prefixes and the invisible leaves
rg -o '^- \[[ xD]\] \*\*([A-Z][A-Z0-9-]+)\*\*' -r '$1' docs/intent --glob '*-specs.md' \
  | sed 's/.*://' | sed -E 's/-[0-9]+[a-z]?$//' | sort | uniq -c | sort -rn
for f in $(find docs/intent -name '*-specs.md'); do
  n=$(grep -cE '^- \[[ xD]\] \*\*MCP-OP-' $f)
  t=$(grep -cE '^- \[[ xD]\] \*\*[A-Z]' $f)
  [ "$n" != "$t" ] && echo "$f mcp-op=$n total=$t"
done

# witness side: Makefile-only, and dangling ids
rg -o '@spec\s+(MCP-OP-[A-Z0-9-]+)' -r '$1' Makefile | sort -u > mk-witness.txt
rg -o '@spec\s+(MCP-OP-[A-Z0-9-]+)' -r '$1' src --glob '*.clj*' | sed 's/.*://' | sort -u > src-witness.txt
comm -23 mk-witness.txt src-witness.txt                 # -> 17 ids
rg -o '@spec\s+([A-Z][A-Z0-9-]+)' -r '$1' src test Makefile bench bin | sed 's/^[^:]*://' | sort -u > all-witness.txt
rg -o '^- \[[ xD]\] \*\*([A-Z][A-Z0-9-]+)\*\*' -r '$1' docs/intent --glob '*-specs.md' | sed 's/^[^:]*://' | sort -u > all-specrows.txt
comm -23 all-witness.txt all-specrows.txt               # -> TELEMETRY-EVENTS-001 (+2 regex artifacts)

# gate wiring
grep -n 'performance-regression-sentinel-test' Makefile   # only .PHONY, help, own definition
sed -n '1130,1145p' Makefile                              # test / test-full recipes

# HLD coverage
grep -ciE 'alias migration|feature.thread|census|helper extraction|tmpdir' docs/high-level-design.md
grep -niE 'mission ledger|typist|publication' docs/high-level-design.md
```

Artifacts: `/var/tmp/forge/lid-audit/{oracle.log,test-fast.log,src-*.txt,all-*.txt,mk-*.txt,changed-unmarked*.txt}`.
