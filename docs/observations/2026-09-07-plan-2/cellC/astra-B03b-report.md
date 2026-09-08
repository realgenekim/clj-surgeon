# Astra B03b repair report

Status: **COMPLETE — all five findings repaired, required gates green, dry merge clean.**
Completed: 2026-09-08T05:42:55.427727+00:00
Written: 2026-09-08T05:26:59.447571+00:00
Worktree: `/home/forge/src/clj-surgeon-split`; branch: `astra/namespace-split`.
No push or installation. All changes are committed; working tree is clean.

## Commits

- `ae379414df560b885e8ab3e226e2562eca62baa4`: merge current working trunk into the branch; append-preserving ledger reconciliation. Second parent is `675bbd18c3c8fc9eea3a37a910b28d53a8f0d488`.
- `0b8a292116e039a043609ea0f9b800c1c5702955`: verifier, intent registry, proof rule, skill mirrors/cap, EOF repair and permanent witnesses.

- `a4868ca95c5ab5b03aa2983e49c7f2305259715b`: append the runner-written passing battery receipt for `0b8a2921` (694 tests / 13,332 assertions, 801 s).

All three commits have author and committer `forge-anvil <forge-anvil@anvil>` and
`Co-Authored-By: Gene Kim <genek@itrevolution.com>`.

## Finding 1: complete doctrine enforcement

`agent-routing-specs.md` now owns 33 complete required passages, grouped by the
existing ROUTING IDs. The verifier derives its needles from that registry; the
old independent 18-string code list is removed. Full passages include wrapped
qualifiers, all three call schemas, admission/exclusion boundaries, proof,
escape, retirement, evidence and terminal-response rules. The EDN parser fails
closed on missing/duplicate registries, missing/unknown intent coverage, malformed
EDN, trailing forms, empty vectors or blank/non-string requirements. Its path is
anchored to the verifier source; a check from outside the checkout passed.

Every passage has a deletion witness. Adding an intent and promise through the
registry alone makes the verifier reject the plate until that promise is present.
These are text-doctrine checks, not a parser or evaluator of runtime receipts.

| Independent Sol r7 mutation | Linked intent | Before implementation | After implementation |
|---|---|---|---|
| TWO automatic classes → THREE | ROUTING-PARITY-001 | Accepted; 2 failed assertions | Refused: missing-required-routing-section |
| Remove exact d9205abc/views.clj witness | ROUTING-SPLIT-001 | Accepted; 2 failed assertions | Refused: missing-required-routing-section |
| Remove changed mapping/policy/source shape requires new admission | ROUTING-SPLIT-001 | Accepted; 2 failed assertions | Refused: missing-required-routing-section |
| Replace alias_migration operation | ROUTING-PARITY-001 | Accepted; 2 failed assertions | Refused: missing-required-routing-section |

Each test first asserts the original plate is valid and the mutation changed it.
Warm JVM: `make nrepl`, port 46399, confirmed `user.dir` is this worktree; namespaces
reloaded for each cycle. Initial combined red: **18 tests / 195 assertions,
36 failures, 0 errors**. Final green: **18 tests / 298 assertions, 0 failures/errors**.
The original design EOF was separately replayed from `d31c3af7`; its witness
failed as intended. Logs: `B03b/red.log`, `B03b/artifacts-red.log`, `B03b/green.log`.
An initial test-authoring delimiter typo was repaired before the recorded red run;
it is not counted as a regression witness.

## Findings 2 and 3: proof and skill agreement

The plate requires every profile check, zero exit for those checks, no failed
checks, committed state, complete verification and empty pending proof. A nonzero
exit is excused ONLY with a `:baseline` map on `captured-reference-analysis` or
`candidate-lint-delta`. Any other nonzero exit refuses, even `:status "passed"`.
Registered papercuts remain mandatory. The corresponding text witness was red
before the change and green afterward.

`skills/clj-surgeon/SKILL.md` is canonical for generation; `skill.md` supersedes
installed copies for repository work. Canonical and both generated worktree
mirrors now say fan-out suspended, alias migration retained, and namespace split
admitted only for the frozen Cell C contract. They name the exact witness/counts,
policies, roots/profile, readmission boundary and baseline exception. The skill
uses **69 lines**, under the requested 70-line cap. The old skill self-check had
a stale 30-line cap: its reproduced refusal and corrected 70-line success are
`B03b/skill-budget-red.log` and `B03b/skill-budget-green.log`.

Executed `make sync-clj-surgeon-skill`, `make check-clj-surgeon-skill-mirrors`, and
`make clj-surgeon-skill-self-test`; all green. Full canonical diff: `B03b/skill.diff`.
No installed skill or global instruction file was written.

## Findings 4 and 5: ledger and whitespace

Initial `git merge-tree --write-tree HEAD origin/MCP/main` reproduced the single
ledger conflict. Merge commit `ae379414` preserves every row of both parents
byte-for-byte and in each parent's order, with the three reconciled rows in this order:

1. `9d132e7f9a8a5887970adbfbbf5297e48e2c4915`, 2026-09-08T02:39:36Z, fail, 785 s.
2. `8795741f07b0991642b28116730f82e25df84b26`, 2026-09-08T02:54:09Z, pass, 793 s.
3. `ec64f322499c397de972f0b1cbb8c29342d7f145`, 2026-09-08T04:27:55Z, pass, 791 s.

The runner subsequently appended the new passing `0b8a2921` receipt after these rows.
The permanent ROUTING-PARITY witness requires each original receipt exactly once and in this order.
The design now ends with exactly one newline; the witness refuses the original
extra EOF blank line. `git diff --check origin/MCP/main..HEAD` is clean. The merge
preserves trunk's archived receipt bytes, including their existing whitespace;
the task does not rewrite those archives.

## Verification

- `make check-agent-routing PLATE_ONLY=1`: **ok true**, 182 lines / 10,708 UTF-8 bytes,
  within 188 / 10,738. Doctrine commit `3c2eb6a9`; canonical SHA-256
  `67e457240c69a1c41bc335b7c3b68e2fbad35cd37d7dd8721031f1f094136393`.
- Standard Clojure Style formatted all three changed Clojure files. Serialized
  `~/bin/clj-kondo`: **0 errors, 0 warnings**.
- Direct intent audit: **ok true**, violations `[]`; **144 pre-existing pending
  witness violations** remain visible. All three ROUTING IDs retain code/test witnesses.
- Initial `make test`: **exit 0**; JVM **795 tests / 10,166 assertions**, Babashka **886 tests / 7,831 assertions**, zero failures/errors. Full output: `B03b/make-test.log`. This run exercised the final repair code. The only subsequent tree change was the appended battery receipt; the skill-budget self-test was also run separately.
- Final-head landing gate initially refused because the merge put the old receipt 43 counted commits behind HEAD (limit 30). That refusal is retained in `B03b/final-landing-gate.log`. Remedied with `flock /home/forge/tmp/suite.lock make test-battery`: **exit 0**, **694 tests / 13,332 assertions**, zero failures/errors, zero skipped/failed preconditions, zero isolation violations, **801 seconds**. Output: `B03b/battery.log`.
- Final commit `a4868ca9`: `make battery-fresh` **exit 0**, receipt **1 commit behind HEAD** (`B03b/final-freshness.log`). Final focused suite with the appended ledger row: **18 tests / 298 assertions**, zero failures/errors (`B03b/final-focused.log`). The full suite results above plus refreshed final-head freshness discharge the required gates; no code changed after the full-suite run.
- Final fresh-fetch dry merge against `d74604eb39129bcd0eead71372d1e1b5834fff5a`: **exit 0**, merged tree `15bb22b8537cc4a7df55118d2122c038e7ea0e50`. Receipt: `B03b/dry-merge-final.log`. Initial conflict and intermediate clean merge remain in `B03b/dry-merge-before.log` and `B03b/dry-merge-after.log`.
- Final `git diff --check origin/MCP/main..HEAD`: **exit 0**. Working tree clean; no push or installation.

The separate linked-intent-dev skill was unavailable after searching configured
skill locations and `/home/forge/opt`. Used the existing agent-routing HLD →
design → EARS → tests → code chain and the locally available
`/home/forge/opt/claude-skills/linked-intent-testing/SKILL.md`. The B03b user request
authorized completion of this leaf without phase approval pauses.
