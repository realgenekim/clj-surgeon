# rename_alias v1 build report

Recorded 2026-09-10T23:50:06.745240+00:00.
Branch `fable/rename-alias`, base `29f81c5f`, final HEAD `def6c26cba10804969257082d0d801e85f16f285`.
Implementation and required verification are complete. Worktree is clean; commits
name their files explicitly. Nothing was pushed. No main changes, servers,
forbidden ports, landing-gate-prewarm, or edits to AGENTS.md.

## Ordered build and commits

The first artifact was `seams.md`, exactly 20 lines. The authoritative contract
was read whole and copied without normative changes to
`docs/intent/rename-alias/contract.md`. The mandate's actual skill was
`/home/forge/opt/claude-skills/linked-intent-testing/SKILL.md`.

The initial RED commit contains all eleven literal §7 witness names, the typed
`:not-implemented` stub, the separately authored oracle, and the exact Git blob
from curtaincall-cfp `00e8f0fa`. `red-final.log` records 11 tests / 209 assertions,
122 failures and one error from the absent CLI entrance. The source fixture SHA-256
is `5f086f7789fdb556ba6e819b26d6eba8ec0345e22b7aaa152b2e1a03648234a1`.

```text
e7614428 test: freeze rename_alias v1 witnesses RED against typed stub
7b76d436 fix: retain source spans for namespaced map qualifiers
ef9f636d feat: plan fixed alias reader roles and preservation evidence
c197279d feat: publish rename_alias through guarded request-file and MCP entrances
ed4948e7 test: strengthen alias preservation and expose cross-file refusal order RED
ea67df79 fix: honor global validation stages and distinguish aliases from declaration symbols
def6c26c docs: record rename_alias witnesses and final E4 verification
```

The census was regenerated through its direct CENSUS_REGENERATE entrance and
its diff READ before the initial RED commit: 1,689 → 1,700 deftests, eleven
additions and zero removals. The added names are:

```text
clj-surgeon.rename-alias-parity-test/rename-alias-cli-mcp-parity
clj-surgeon.rename-alias-test/rename-alias-binding-matrix
clj-surgeon.rename-alias-test/rename-alias-discard-decoy
clj-surgeon.rename-alias-test/rename-alias-e4-verbatim
clj-surgeon.rename-alias-test/rename-alias-prefix-trap
clj-surgeon.rename-alias-test/rename-alias-reader-roles
clj-surgeon.rename-alias-test/rename-alias-receipt-oracle
clj-surgeon.rename-alias-test/rename-alias-scope-guards-counts
clj-surgeon.rename-alias-test/rename-alias-source-boundaries
clj-surgeon.rename-alias-test/rename-alias-string-decoy
clj-surgeon.rename-alias-test/rename-alias-transaction-faults
```

Final regeneration retains exactly the same ledger; its diff was also read and
is empty. Evidence: `census-red.log`, `census-final.log`, and the committed ledger.

## Implementation and preservation

Both MCP `rename_alias` and the shorthand/`:op` CLI request-file entrances call
one planner and transaction adapter. Shared insertion code owns bounded one-value
EDN reading, strict UTF-8, guard validation, confined paths, state-first printing,
and atomic publication primitives; the existing commit engine owns multi-file
preflight/read-back and guarded rollback. Rename adds a complete inspected scope,
reader-role prefixes, complete true-site count evidence, staged per-file inverse
records, and durable progress/outcome receipts. Receipt summaries remain bounded;
a 30-site refusal proves that capped inline items retain complete durable evidence.

Per-file detail inventories every changed and other root form by ordinal, including
duplicates and discarded forms, with actual before/after hashes; skipped files also
have complete preservation inventories. Candidate reparsing, role recount and
inverse identity precede publication. Runtime namespace mutation and alias capture
refuse. Strings, comments, regex, character literals and entire discards stay intact.
Quoted symbols, auto-resolved keywords/maps, metadata and syntax quote/unquote count.

The independent test oracle reuses the insertion oracle's inventory/hash utilities,
not production selectors/splices/hashes. Its own role walk, byte-span checks and
inverse reconstruction reject corrupted neighbors/comments/discards, missed sites,
forged result digests, missing form inventories and forged reference counts. Both
production and this oracle share rewrite-clj; parser independence is not claimed.
The retained E4 shell oracle adds independent clj-kondo usage-set analysis.

## Contract amendments and corrected witnesses

No normative contract amendment was needed, and no required refusal was weakened.
The namespaced-map qualifier span repair extends a shared reader seam because
rewrite-clj supplies no coordinates for that synthetic child. Alias restrictions
remain strict, while ordinary library/referred declaration symbols retain the
broader grammar promised by the contract. Cross-file validation now completes
source-safety and binding stages before collision checking. These are implementation
corrections to the existing contract, not changes to its admission boundary.

The copied parity fault driver initially contained an accidental unquoted require
vector; correcting that test driver left success/refusal/recovery expectations
unchanged. The added E4 template check initially confused distinct literal fragments
(two) with distinct enclosing route-construction templates (22); it now independently
counts the latter. Neither the frozen blob nor expected result hash changed.

`stage-order-red.log` records two failures that demonstrated a collision in an earlier
file masking a later binding/source refusal. `stage-order-green.log` records the
repair. `declaration-symbol-red.log` records six failed assertions for legal `_`
libraries and apostrophized referred names; `declaration-symbol-green.log` records
the repair. Reserved aliases still refuse. The earlier failures remain retained.

## Final gates

- `focused-verified.log`: **11 tests, 507 assertions, 0 failures/errors**. Includes
  real CLI shorthand and `:op`, direct MCP callbacks, malformed EDN, stdout/exits,
  guard/count refusals and injected rollback/recovery.
- `test-fast-verified.log`: **77 namespaces, 766 tests, 8,236 assertions,
  0 failures/errors, 0 skipped/failed preconditions, 0 isolation violations**;
  every lane exits 0. Coordinator makespan 25.118 s.
- `lint-verified.log`: all changed production/test Clojure files through
  `~/bin/clj-kondo`, **0 errors / 0 warnings**. One pre-existing informational
  redundant-str finding remains in core.clj.
- `git diff --check 29f81c5f`: passes after removing a trailing blank documentation
  line. No source changes followed the final verification; the last commit records
  documentation only.

The first test-fast failure was the missing independent registry-catalog entry for
rename_alias; the entry now points to real parity/fault witnesses. A later gate
had zero assertion failures but correctly reported isolation violations because
census regeneration rewrote the ledger concurrently. The final gate ran without
concurrent repository writes. Earlier gate logs are retained rather than regraded.

Operational deviations: the first make run placed generated logs under its hardcoded
`target/gate-parallel`; they were moved intact under `rename-fx/test-fast-output`,
with the ignored worktree path now pointing there. Two direct Babashka boundary
probes created short-lived helper fixtures under `/tmp`: Babashka did not inherit
java.io.tmpdir from JAVA_TOOL_OPTIONS. The helpers removed them in `finally`; the
subsequent probe set System/java.io.tmpdir explicitly before fixture creation.
All final JVM probes use `-J-Xmx1g` and the named tmpdir; the required make target
uses its existing smaller 512 MiB heaps. No outside fixture was retained.

## E4 final arm

Original bytes came from the committed verbatim fixture. An isolated shared-object
clone under `rename-fx/e4` supplies the Git baseline for the retained oracle without
changing the source repository. `e4-oracle.sh` differs from the retained oracle only
in its mktemp destination, relocated under rename-fx. The correct request is
`e4-request.edn`; the actual branch CLI command was:

```text
bb -m clj-surgeon.core :rename-alias! :request-file /var/tmp/forge/rename-fx/e4-request.edn
```

Started `2026-09-10T23:47:14.971534+00:00`, finished `2026-09-10T23:47:34.774963+00:00`.
Request construction + CLI: **19.224074 s**.
Independent oracle + final hash: **0.579352 s**.
Complete measured request-to-oracle wall: **19.803426 s**.
Fixture setup is excluded and declared in `e4-timing.json`; first attempt succeeds,
CLI exit 0, oracle exit 0, all eleven oracle checks PASS.

Result SHA-256:
`02332a74caf1ead4d70c555b530230b8b03aebc306bd72f5d129f111c876da0c`.
There are exactly two day-hours uses through ev, in agenda-days and schedule-page;
35 root forms remain, only ns/agenda-days/schedule-page change, all gaps survive,
and all 29 route literals plus the whole string multiset are unchanged. The focused
witness additionally counts the 22 enclosing route templates. E4's secondary lint
retains its baseline findings; these are separate from the build's 0/0 lint gate.

Artifacts: `e4-receipt.edn`, `e4-oracle.log`, `e4-timing.json`, `run-e4.py`, and the
external durable detail located by the receipt. The earlier 19.819 s replay is
retained under `e4-before-final-review/`. This is a correctness replay with no fresh
native control, not a speed/admission claim. The final replay overlapped a focused
witness process; that wall is not an isolated performance measurement. Curtaincall
behavior tests were not run; verification_complete remains false as required.

## Three least-sure choices

1. **Quoted symbols count; discards do not.** The syntactic contract deliberately
   changes quoted data spelling, while re-enabled discarded code may retain the
   old alias. Literal/context witnesses protect this boundary; caller trials remain
   the way to judge its usefulness.
2. **Every inspected file is guarded, including skips.** This gives a closed snapshot
   and explicit membership-drift refusal, but repository-wide guard size and the
   requirement that all inspected files have supported namespaces may be costly.
   Cooperative locking plus final rechecks is not arbitrary-writer filesystem CAS.
3. **A separate verb owns alias-and-reference edits.** require_change and
   alias_migration keep their existing meanings. Shared reader/transaction seams
   reduce duplication, but future caller evidence may favor a unified public
   envelope. This build does not decide that future product question.
