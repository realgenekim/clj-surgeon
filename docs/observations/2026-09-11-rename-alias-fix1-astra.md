# rename_alias fix round 1 — completed, ready for Sol

Recorded: 2026-09-11T00:42:40.952830+00:00
Workspace: `/home/forge/src/clj-surgeon-rename`
Base: `a1989b12f1a183a31bad58052c303f78b5c4bcc2`
Final HEAD: `273a8bbf7fd496ad8628f0c1cefdc886f4e8f750`
Local branch label: `fable/rename-alias` (the supplied rebased HEAD was correct; no branch switch was needed).

All six requested items are implemented, committed, and verified. Seven small
named-file commits; no push, merge, AGENTS.md change, server start, port contact,
or landing-gate-prewarm. This is a builder handoff, not Sol approval.
The earlier stopped report is retained as `astra-fix1-report-before-ruling.md`.

## Commits

- `6d8b74cf` — Witness each rename candidate and read-back guard under injected faults
- `b9c3c490` — Derive rename receipt preservation and hashes from disk observations
- `7768cbe5` — Report column-one alias sites on their actual line and preorder
- `3f5566ce` — Ratchet every versioned write receipt boolean with a driven false witness
- `cd7bc3e4` — Bound rename planning with binary-search source coordinates
- `0a68ecda` — Scope alias eligibility to selected namespaces and exempt comment ancestry
- `273a8bbf` — Register fix-round intent and retain the sixteen added census witnesses

## RED then GREEN evidence

Every path below is under `/var/tmp/forge/rename-fx/`. RED means assertion failures
for the named defect, with zero test errors. The first four runs temporarily deleted
one production guard at a time outside the tests, ran the injected-fault witnesses,
and restored the original source before the next mutant. Tests never edit production.
The mutation driver is retained as `fix1-mutate.py`; the bounded JVM runner is
`fix1-run.sh`.

| Item | RED log | Observed RED | GREEN log |
|---|---|---|---|
| F1 candidate role recount | `fix1-red-1.log` | 2 failures; missing refusal and changed disk bytes | `fix1-green-1.log` |
| F1 form preservation | `fix1-red-2.log` | 2 failures; missing refusal and changed disk bytes | `fix1-green-1.log` |
| F1 inverse identity | `fix1-red-3.log` | 2 failures; missing refusal and changed disk bytes | `fix1-green-1.log` |
| F1 replacement disk comparison | `fix1-red-4.log` | 3 failures; committed instead of io-error/rollback, changed disk | `fix1-green-1.log` |
| F2 disk/artifact receipt evidence | `fix1-red-5.log` | 8 failures | `fix1-green-2.log` |
| F3 column-one coordinates | `fix1-red-6.log` | 4 failures | `fix1-green-3.log` |
| Class boolean ratchet | `fix1-red-7.log` | 2 failures in insert_forms: missing false preservation/write facts | `fix1-green-4.log` |
| F4 4,000-line planner bound | `fix1-red-8.log` | 8.025649921 seconds exceeds 5 seconds | `fix1-green-5.log` |
| F5 selected-scope eligibility | `fix1-red-9.log` | 10 failures across all four named witnesses | `fix1-green-6.log` |

F1's four seams are `*candidate-roles*`, `*preservation-tree*`,
`*inverse-evidence*`, and an injected one-shot `journal/sha256-file` observation
armed by the existing read-back hook. The first three assert
`:candidate-structure-mismatch` and unchanged original disk text. The fourth
asserts `:io-error`, `rolled-back`, and restored original disk text. A fifth
witness drives `*candidate-text*` with unparseable text and asserts
`:candidate-parse-error` plus unchanged original disk text. All five are green
with the guards restored (5 tests / 11 assertions).

F2's mutations wrap intent-transaction's injected `write-source!`: after a real
write returns, the seam corrupts a neighbor, trivia, or discard on disk. Recovery
preserves those external bytes and publishes false evidence. Read-back hashes are
computed from fresh observed disk text (validated UTF-8 preserves the exact bytes),
not candidate hashes. Root digests use raw CST text and declared edit ordinals,
independent of namespace resolution. Artifact-section claims are derived by reading
the actual published detail artifact; removing `sites` makes its membership false.
The hash fields are strings/maps, not booleans: witnesses require equality to an
independent SHA of the actual disk content and inequality to the planned result hash.

F3 witnesses line 1 / column 1 at the traversal seam (an executable file must begin
with ns), line 2 / column 1 in valid source, and line/preorder in count-refusal evidence.
F4 uses inclusive-start/exclusive-end binary search. The first GREEN run was
1.474204275 seconds; the final combined run was 1.085936655 seconds. Both used
`-J-Xmx1g`; the generated source has exactly 4,000 lines. These are local regression
measurements, not a replication of Opus's absolute 63-second wall or a matched-native
performance claim.

## Boolean inventory and seam registry

Rule, present in both receipt-projector docstrings and checked by the class test:
**EVERY BOOLEAN IN A RECEIPT MUST HAVE A WITNESS IN WHICH IT IS FALSE.**

Intent: `RECEIPT-BOOL-001` in
`docs/intent/receipt-booleans/receipt-booleans-specs.md` and
`docs/intent/receipt-booleans/registry.edn`; executable ratchet:
`test/clj_surgeon/receipt_booleans_test.clj`.
The existing repository intent audit discovers the new specifications and verifies
implementation/test links. The registry records EARS, misreadings, boundaries,
per-verb seams, and the motivating repeated defect.

The ratchet derives versioned committed-write verbs from the real public tool
catalog, independently of its scenario registry. It walks returned committed
receipts recursively and includes rename's durable per-file preservation. Every
boolean path must have a registered, supported scenario that produces literal
false at that same path. Missing is not false. A third verb or a new nested boolean
fails by name; the synthetic third-verb and unwitnessed-field cases exercise that
registration check.

| Boolean key | insert_forms false scenario | rename_alias false scenario |
|---|---|---|
| `committed` | `stage-failure` | `stage-failure` |
| `ok` | `stage-failure` | `stage-failure` |
| `mutation_attempted` | `stage-failure` | `stage-failure` |
| `source_unchanged` | `completed-write` | `completed-write` |
| `verification_complete` | `behavior-not-run` | `behavior-not-run` |
| `write_verified` | `neighbor-corruption` | `neighbor-corruption` |
| `other_forms_unchanged` | `neighbor-corruption` (nested preservation) | `neighbor-corruption` (summary AND durable per-file preservation) |
| `partial_write` | absent | `completed-write` (nested transaction) |
| `gaps_unchanged` | absent | `trivia-corruption` (durable per-file preservation) |
| `discards_unchanged` | absent | `discard-corruption` (durable per-file preservation) |

This is 7 distinct boolean keys for insertion and 10 for rename; rename's
`other_forms_unchanged` has two independently checked paths. Stage failure throws
before publication. Neighbor/trivia/discard scenarios mutate real post-write disk
content through the injected transaction writer. Already-false state fields are
witnessed by real completed-write / no-behavior-check scenarios; their values are
not forged with receipt overrides. No behavioral verification is claimed.

## Contract amendment and disagreements

Fable's ruling resolves the earlier F5(b) stop. The one paragraph replacing the
mutation-tripwire paragraph of §3 in `docs/intent/rename-alias/contract.md` says:

> Refuse effective core in-ns/alias/ns-unalias/runtime require forms outside ns
> only in SELECTED namespaces: `:unsupported-namespace-mutation`. Recognize
> unqualified/clojure.core heads outside ordinary quote, ignoring forms whose
> ancestry includes `comment` or `clojure.core/comment`; alias-reference traversal
> continues inside those bodies, so `(comment (events/x))` still counts and is
> rewritten as code syntax under §2. This is a conservative syntactic tripwire;
> computed/macro-hidden namespace mutation and runtime alias manipulation remain
> outside the guarantee. (Fable amendment, 2026-09-11.)

F5(a) non-Clojure symlinks and F5(c) skipped-file duplicates are implementation
corrections. Code-file symlink targets retain confined-path refusal. Selected
ordinary mutation forms and selected duplicate bindings still refuse; the original
rename suite exercises those boundaries. Comment ancestry does not suppress alias
reference traversal, including new-alias capture checks.

No further contract disagreement or stop. Other red-team observations F6–F10 were
outside the six requested items and were not claimed fixed here; Sol should retain
them as review context. In particular, this round does not redesign durable-detail
rewrite cost, expand refusal remedies, or add a comment-reference context label.

## Final verification

- `fix1-all-witnesses.log`: all 22 insert namespaces and all 7 rename namespaces,
  plus receipt-booleans, lane-manifest, and intent-contract: **100 tests / 1,945
  assertions / 0 failures / 0 errors**, zero isolation violations across 32 namespaces.
  This includes CLI/MCP parity and the performance battery witness.
- `fix1-test-fast.log`: **make test-fast exit 0**, **781 tests / 8,337 assertions /
  0 failures / 0 errors**, zero isolation violations across 82 namespaces, skipped 0.
  The repository coordinator used its own 512 MiB workers over eight lanes;
  direct witness JVMs used the brief's 1 GiB heap. Makespan: 32.921 seconds.
- `fix1-lint.log`: changed Clojure files linted only through `~/bin/clj-kondo`:
  **0 errors / 0 warnings** (one informational redundant-boolean-coercion note).
- Formatting: Standard Clojure Style on changed implementation/new test files;
  formatter logs retained as `fix1-format*.log`. `git diff --check` is clean.
- `fix1-census.log`, `fix1-census-diff.patch`: direct authorized census regeneration;
  diff read, **+16, 0 removed**, **1,700 → 1,716** total tests. New namespace membership
  is named in both the lane manifest and its adoption ledger. Final lane/census tests
  are green. The initial census pass correctly flagged missing adoption membership;
  that membership was added by name before the final gates.
- All owned temporary test fixtures were cleaned; logs and reports remain under
  `/var/tmp/forge/rename-fx/`. Final worktree is clean. No push or prewarm.

### The 16 census additions

- `clj-surgeon.receipt-booleans-test/every-receipt-boolean-has-a-driven-false-witness`
- `clj-surgeon.receipt-booleans-test/unregistered-verbs-and-fields-fail-by-name`
- `clj-surgeon.rename-alias-candidate-test/candidate-form-preservation-refuses`
- `clj-surgeon.rename-alias-candidate-test/candidate-inverse-identity-refuses`
- `clj-surgeon.rename-alias-candidate-test/candidate-parse-refuses`
- `clj-surgeon.rename-alias-candidate-test/candidate-role-recount-refuses`
- `clj-surgeon.rename-alias-candidate-test/replacement-read-back-refuses`
- `clj-surgeon.rename-alias-performance-test/four-thousand-line-planning-under-five-seconds`
- `clj-surgeon.rename-alias-positions-test/column-one-reference-addresses`
- `clj-surgeon.rename-alias-receipt-test/receipt-details-contains-observes-artifact`
- `clj-surgeon.rename-alias-receipt-test/receipt-observes-disk-neighbor-corruption`
- `clj-surgeon.rename-alias-receipt-test/receipt-observes-disk-trivia-and-discard-corruption`
- `clj-surgeon.rename-alias-scope-test/comment-ancestry-exempts-mutations-but-keeps-references`
- `clj-surgeon.rename-alias-scope-test/repository-ignores-non-clojure-symlinks`
- `clj-surgeon.rename-alias-scope-test/skipped-duplicate-bindings-do-not-refuse`
- `clj-surgeon.rename-alias-scope-test/skipped-namespace-mutations-do-not-refuse`
