# Astra rows sublime — 2026-09-08

Branch-only work in `/home/forge/src/clj-surgeon-split`, `astra/namespace-split`,
starting at `5acda6d4549d1e8354a5bd38e4aac3cce91146bf` (contains `96819291`).
Operator authorization supplies the spec and permits the linked-intent-testing
substitute through completion. No push or main merge. Work began 13:11:17 UTC.

## Commits

- `11317b6f8381ba661e932fd65a3e9c6743f15b61` — fix(split): externalize profiles and report incomplete proof honestly
- `77ac921c96a8cb382d5ee9034919d4bbe45eb9d7` — feat(alias): record refusal wall and explicit route telemetry unknowns

Both authored and committed as `forge-anvil <forge-anvil@anvil>`, with
`Co-Authored-By: Gene Kim <genek@itrevolution.com>`. Working tree clean; no push.

## Row 5: external configuration and honest proof

NS-SPLIT-047 adds request `:verification {:profile "split-unit" :profile-file
"/abs/operator/profiles.edn"}` and CLI `:profile-file`. The file contains
`{:verification-profiles {"split-unit" {:commands [...]}}}`. Explicit external
configuration takes precedence over injected and workspace configuration; it
never writes `.clj-surgeon.edn`. Relative, inside-workspace, missing, malformed,
nonregular and over-1-MiB files refuse before mutation; resolved symlinks into the
workspace refuse too. Existing profiles without this field continue to work.

NS-SPLIT-048 makes the actual executed command evidence govern completion.
A cold `/bin/true` profile publishes the exact D2–D6 public shape below, with
its executed argv, exit and duration in `:checks`:

```clojure
{:state "committed" :committed true :ok true :mutation_attempted true
 :verification_complete false :proof_pending ["cold-suite"]}
;; command check, apart from measured duration_ms:
{:name "true" :profile "b07-cell-b" :command ["/bin/true"]
 :exit 0 :status "passed"}
```

NS-SPLIT-049 chooses typed refusal for `{:commands []}`:
`verification-empty-profile`, no mutation, incomplete proof, `cold-suite` pending.
The shared helper boundary advertises its corresponding typed refusal as well.
Other configured programs remain trusted operator gates: this change does not
infer what arbitrary scripts prove. CLI help explains how to find the target's
cold gate, verify the current snapshot against the receipt, and record separate
proof closure without replaying a committed split.

Red → green witnesses (all log paths below are under
`/var/tmp/forge/rows-sublime/`):

| Intent / defect | Red evidence | Green evidence |
|---|---|---|
| NS-SPLIT-047 external file and CLI field | `row5-red.log:4` rejects the new field; `:32` loses CLI path | `row5-green2.log`, `row5-cli.log`, `lane-repair-green.log` |
| NS-SPLIT-048 true-only honesty | `row5-red.log:36` actual complete=true/pending=[] | Real-process `row5-trivial-profile-receipt-is-honest`; `lane-repair-green.log` |
| NS-SPLIT-049 empty profile | `row5-red.log:40` generic unavailability; `:48` generic split refusal | `row5-boundaries.log`, `lane-repair-green.log` |
| Closed helper refusal catalog | `row5-catalog-red.log:4` missing empty-profile kind | `final-gates-warm.log`, final cold gate |
| Pending-proof caller guidance | `help-review-red.log` four absent help phrases; fresh caller NO-GO | `help-review-green.log`; final fresh caller GO |

The first red log also contains an erroneous expected fixture filename set;
that test expectation was corrected. Its aggregate failure count is not offered
as thirteen independent implementation defects. The substantive missing-field,
false-completion and generic-refusal lines above establish the intended reds.

The in-image Cell C loop repeatedly ran warm tests and the papercut oracle.
Final runtime iteration: 52 tests / 530 assertions, zero failures/errors,
PAPERCUTS=0 (`final-warm.log`, `final-warm-oracle.log`). The two real-process
publication witnesses were subsequently moved intact into the existing battery
namespace to satisfy cold process isolation; the final affected warm set is
59 tests / 412 assertions, zero failures/errors (`lane-repair-green.log`).
The external-profile witness proves the exact final file set and unchanged
`deps.edn`; neither configuration nor receipt artifacts are added to the tree.

## Row 2: observable refusal cost, honest unknowns, external ledger

ALIAS-MIGRATION-004/005 add `refusal_price` in milliseconds for refusals (nil
for commits), `fallback "unknown"`, and an explicit `unknown` vector. First
attempt and complete verified wall remain unknown because they require caller
observation. Missing counts are nil and named unknown, never fabricated zeros.

The outermost direct/MCP entrance records exactly one EDN telemetry line under
`/var/tmp/forge/alias-migration-receipts/ledger.edn`, including routing refusals:
id, wall_ms, outcome, files, sites, collisions, refusal_price, fallback, unknown.
Wall runs from entrance to operation result, before ledger I/O; it excludes
transport and later caller verification. Thread and file locks serialize writes.
Ledger failure preserves the mutation receipt, marks recording false and ledger
unknown. Unexpected operation throws retain transport semantics and record an
unknown outcome. Nested entrances suppress duplicate ledger lines.

Red: `row2-red.log:4` missing refusal_price, `:8` absent fallback,
`:36` absent refusal wall, `:40` missing ledger. Green: all six added witnesses,
including concurrent whole-line publication, nested calls, routing refusal,
unexpected throws and ledger failure; full alias boundary is 143 tests /
3,331 assertions, zero failures/errors (`row2-full-green4.log`).

Frozen Row 2 request replay on a fresh `6dcdbc9db67a91179b5940a16c16b760583f2603`
fixture commits nine files / 21 sites / three resolved collisions. Telemetry ID
`af5219d2-1609-4e4e-95d6-2ea37f4ff823`, wall 3835.068212 ms, recorded=true.
O1 load/suite, O2 golden structure, O3 serialized kondo, O4 prose/collision,
and O5 papercuts all PASS, MINOR=0, ACCEPTED=YES (`alias-oracles.log`).
The original request, receipt, fixture patch/status/base and independent oracle
outputs are retained beside the logs. No row-2 timing comparison is claimed.

## Row 1: one hand-driven warm/cold observation

Two fresh worktrees use `split/base2` `4f6283aa`: frozen `d9205abc` source plus
the authorized Makefile test alias and architecture witness. Each nREPL was
started with `make nrepl`, verified against its workspace, and preloaded outside
timing. The frozen `/var/tmp/forge/plan2/cellC/D1-request.edn` was rebound only
for workspace and external profile configuration. Warm and cold were each run
once, outside any matched performance experiment. Both commit 20 destinations,
141 owners, 87 static sites, five caller files, 26 changed paths.

| Observation | Request-to-return / hand-gate wall | Receipt |
|---|---:|---|
| `:proof :warm` | **19735.998518 ms (19.736 s)** | `warm-receipt.edn`: committed-probe-only, verification_complete=false |
| `:proof :cold` | **43305.313086 ms (43.305 s)** | `cold-receipt.edn`: committed, verification_complete=true, proof_pending=[] |
| Hand-run pending cold gates on warm candidate | **23914.787754 ms (23.915 s)** | `manual-cold-receipt.json`: all three exits 0 |

The warm receipt's exact pending proof is:

```clojure
:verification_complete false
:proof_pending
["python3 /var/tmp/forge/plan2/cellC/cc-oracle.py /var/tmp/forge/rows-sublime/cellC-warm"
 "python3 /home/forge/src/clj-surgeon-split/test/oracles/namespace_split_papercut_oracle.py /var/tmp/forge/rows-sublime/cellC-warm"
 "bin/kaocha unit --fail-fast"]
```

Its warm probe passes 168 tests / 1,767 assertions. The separate hand-driven
cold gate runs those exact three commands in order, all exit 0, with 231 tests /
2,258 assertions in the cold suite. The original warm receipt stays immutable
and incomplete; the independent gate receipt closes the pending work. Cold
receipt checks also all pass; baseline analysis nonzero exits carry the allowed
baseline maps. See `proof-runs.clj`, both external profile/request files and
`manual-cold-gate.py` for the exact procedure.

Both original receipts honestly report `00SERVER-LOGS.txt` as an unexpected
workspace path from the target test/probe commands. Before-cleanup patches are
retained. After preserving that evidence, only this owned fixture log was
restored to HEAD. Final fixture statuses contain exactly the 26 intended paths,
no `.clj-surgeon.edn`. This cleanup is separate from, and does not rewrite, the
original receipt's workspace exception evidence.

Independent post-gate snapshot checks cover all 75 source/test files against
receipt read-back hashes for changed paths and HEAD bytes for unchanged paths,
plus inventory for new/deleted paths: no mismatch, missing or unexpected file.
Warm receipt hash: `98433b6f90b4cc1a1ee394f002b4b89bceaee00b28985a2afb3ede1598def4bf`.
Cold receipt hash: `c0f26ac96787ae35c4da23f5e5d18b9590412b38bbce4f8b1f542e6bbd438c91`.
Evidence: `warm-post-gate-snapshot.json`, `cold-post-gate-snapshot.json`.

These are three walls from a single hand-driven observation. They do not supply
native controls, variance estimates, a matched cohort, or new routing admission.

## Final verification and review

Final `make test` **PASS**, exit 0, **499.658 s**, finished
2026-09-08T14:01:49.489554+00:00. Affected battery: **164 tests / 3,495 assertions**;
JVM fast/integration: **819 / 10,414**; Babashka: **888 / 7,863**. Zero failures,
errors or isolation violations. Hygiene and the required shell/oracle checks
also pass. `make-test-accepted.log` and `make-test-accepted-receipt.json` retain
the complete final gate. Its exact tested working-tree bytes match the two
commits; no implementation changed after this gate.

The planned single final cold run required two repair attempts; all are retained:

- `make-test-first.log` / `make-test-first-receipt.json`: exit 2, 202.533 s.
  The affected battery passed; JVM failures exposed three stale census pins and
  two process-spawn isolation violations from the new real-process witnesses.
- `make-test-final.log` / `make-test-final-receipt.json` (second attempt, despite
  that historical filename): exit 2, 202.088 s. All test assertions passed,
  but isolation still rejected the two process witnesses and detected my edit
  to `cli_dispatch_test.clj` during the run. That concurrent edit was a workflow
  error, not a repository defect. I moved the two witnesses to the battery lane
  and held the repository fixed throughout the final accepted gate.

Serialized `~/bin/clj-kondo` changed-files checks have zero errors and warnings
(`lint-final.log`, `lint-census.log`, `lint-lane-final.log`); the only reported
info diagnostics predate this change. `git diff --check` passes.

The default repository intent audit is green: 758 specs, no violations, with
144 preexisting explicitly deferred missing-witness entries unchanged. The
unrestricted audit still reports that existing debt; it is retained rather than
misrepresented as wholly linked intent. See `intent-audit.edn`,
`intent-audit-final.log`, and `intent-audit-unrestricted.edn`.

A fresh caller agent, given only the rendered split help, first identified
pending-proof and artifact/recovery gaps. Those were repaired and witnessed.
It then requested explicit recomputation of actual current workspace hashes;
that wording was repaired. Final verdict GO for the bounded help surface,
with actionable preview, apply, pending gate closure and undo instructions.
This was a caller-surface review, not independent review of implementation.

All evidence lives in `/var/tmp/forge/rows-sublime/`. `changes.patch` and
`source-manifest.json` preserve the completed repository delta and SHA-256s.

Completed 2026-09-08T14:03:28.927469+00:00; within the 90-minute operator timebox.
