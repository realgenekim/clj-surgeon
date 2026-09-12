# Block B attempt19: PASS-WITH-RETAINED-REDS

Trunk `a15531ee` is merged into `bb-rewrite-tower-local`. The repaired merged
source passed one real prewarm, all seven stages, in 394503 ms. The once-only
test-fast and restricted diagnostic were red before the census repair; neither
was repeated or relabeled green. The earlier packet's auto-added battery remains
red. No push or landing is claimed.

## Commits and merge resolution

- `6b73b389`: preserved the first check-only prewarm that passed inside the
  envelope, at the user's named prior tip `e7cefb82`, under
  `prewarm-checkonly-run7/`; retained its red battery log and ledger row.
- `db318270`: no-fast-forward merge of trunk `a15531ee`, naming trunk's CLI
  splice bundle and the branch's tmpdir/gate lanes in the message.
- `84a0833c`: registered the two merged trunk witnesses in the derived census.
- This directory's remaining evidence is committed last. Its containing commit
  adds observations only; the prewarm attests source tip `84a0833c`, not that
  later evidence commit.

The stable launcher retains the branch's TMPDIR policy and explicit
`-Djava.io.tmpdir`, and names both copied `src` and `libs/clj-splice/src` on its
classpath. Package preparation copies the library, and package identity includes
its source and dependency declaration. The branch's development launcher,
bb-child tmpdir policy, diagnostic stage, and fast/bb entrances remain present.
The installed-launcher witness and launcher-check exercise the combined result.
`merge.patch` records the relevant merge diff.

Both tech-tree entries are retained, with trunk's September 11 entry before the
September 12 entry. Committing the packet battery evidence before the merge
introduced an additional append conflict in battery-ledger.edn; both dated rows
are retained in chronological order.

## Verification, in execution order

| Invocation | Result | Evidence |
|---|---|---|
| Initial focused bb invocation | Load failure: JVM-only McpAsyncServerExchange unavailable; no test assertions ran | focused.log, focused.exit |
| Focused JVM witnesses | 34 tests / 584 passing assertions; zero failures/errors | focused-jvm.log, focused-jvm.exit |
| Branch attempt13/launcher-check.py, unchanged | Both generated launchers pass eight TMPDIR/argv cases each and real CLI version calls | launcher-check.log, launcher-check.exit |
| make test-fast, exactly once | Exit 2; 1243 tests / 11779 assertions; one census failure, zero errors; fast sum 46443 / 60000 ms, makespan 24286 ms, zero isolation violations | test-fast.log, test-fast-run/fast/receipt.edn |
| Whole gate under attempt18/restricted-gate.py, exactly once | Exit 2; 169.6600949820131 s; shell, alias and bb passed; MCP failed on the same census mismatch | restricted-gate.log, restricted-gate-run/ |
| Census regeneration and direct witness | Exactly two additions, no removals; 1 test / 7 assertions, zero failures/errors | census-repair.log, census-repair.patch |
| Real make landing-gate-prewarm, exactly once | Exit 0; all seven stages passed; 394503 ms | prewarm.log, prewarm-receipt.edn, prewarm-run/ |

The focused JVM run covers trunk's installed splice and package identity tests,
the branch's runtime-rule witness, all ns-isolation ceiling witnesses,
splice-envelope refusal completeness, and receipt booleans including probe.
The first bb invocation was an executor selection error; switching to the
repository's test-deps JVM entrance required no code or runtime-policy change.

The fast and diagnostic failures named precisely
`clj-surgeon.install-test/cli-package-identity-covers-bundled-splice-source` and
`clj-surgeon.install-test/installed-cli-loads-splice-verbs-outside-the-checkout`.
The existing census oracle caught the integration omission. Its documented
`CENSUS_REGENERATE=1` entrance added those names; the diff was reviewed before
commit. No tests were deleted, skipped, or reclassified, and no budget changed.
The diagnostic stopped before the standalone diagnostic and final stages and
therefore produced no final gate receipt; its suite receipts and pool/stage
files are retained. An attempted copy reported that absence rather than
silently retaining a stale receipt.

Final prewarm: alias 182 tests / 3641 assertions; MCP 1411 / 15577; bb 901 / 7994;
standalone bb diagnostic 832 / 7293; all zero failures/errors. MCP fast sum
45007 / 60000 ms; integration 66261 / 240000 ms; bb suite runtime sum
239714 / 343102 ms. Every suite receipt has zero leak failures. These values
come from prewarm.log and prewarm-receipt.edn. The existing bb ceiling is
unchanged; this does not describe it as pre-existing at the original base.

The final receipt says `:state :passed`, `:prewarm? true`, `:landing? false`,
`:problems []`, run `da0708ea-e20b-4f46-b7e5-47e913ed59b6`, completed
`2026-09-12T14:35:42.599586661Z`. Source digest:
`6463611f69fb3372f043849044e9de285f64f1f4c7ce2a4bafdd9eb25212cd74`.
There was one preparatory census repair and no real-prewarm repair or retry.

`gate.md` is the byte-for-byte concatenation of restricted-gate.log followed by
prewarm.log, with no inserted headings: all emitted refusal, census, budget and
isolation lines are retained verbatim. Evidence was archived only after each
run exited. The owned diagnostic UUID root was removed after archival; see
cleanup.log. Other attempts and shared installs were left alone.

## DOGFOOD

| Edited file(s) | Intent | Mechanism | Refusal type | Repair text sufficed? |
|---|---|---|---|---|
| Makefile | Preserve bundled dependency closure and explicit tmpdir together | Git merge plus bounded native Python conflict resolution | Content conflict | Both parents plus user contract supplied the complete combination |
| test/clj_surgeon/install_test.clj | Retain trunk's executable installed-splice/package witnesses and branch tests | Git automatic three-way merge; real focused and gate execution | None | N/A |
| test/clj_surgeon/deftest_census.edn | Account for both trunk tests | Repository CENSUS_REGENERATE entrance; reviewed two-line addition | Named census mismatch, suite failed | Yes, named additions and exact regeneration command |
| docs/tech-tree.md | Keep both histories in order and record merge finding | Bounded native Python conflict resolution, then native apply_patch | Content conflict | Yes |
| docs/observations/battery-ledger.edn | Preserve both battery observations | Exact-form native apply_patch | Append conflict | Yes |
| docs/high-level-design.md; docs/intent/cli-package/design.md; docs/intent/cli-package/cli-package-specs.md; docs/observations/battery-namespace-walls.edn | Retain trunk's intent and measurements | Git automatic merge | None | N/A |

No Surgeon mutation was called: this merge uses the working-tree skill's native
route. Dogfooding exercised the real installed CLI and existing public gates.
The linked-intent-testing skill was consulted at
`/home/forge/src/claude-skills-nrepl/linked-intent-testing/SKILL.md`; existing
CLI-PACKAGE and TEST-ISO promises govern the merge and census repair. No new
behavioral contract or budget was introduced.

## Limits, disagreements, and owed

No required attempt19 execution remains owed. Final prewarm is green; the
once-only standalone fast and restricted diagnostic invocations remain red
historical observations. This is no fresh green restricted-envelope proof of
the repaired source, no battery certification, and no performance comparison.
The archived run7 receipt hash matches its original packet report, preserving
the first successful check-only prewarm without erasing the packet's failure.

Fable's independent review and any further battery/envelope or landing decision
remain external. The prior packet's battery-red follow-up remains with Fable
as recorded in prewarm-checkonly-run7/report.edn. Least sure: the repaired
census has not been rerun under the restricted wrapper; final prewarm runs
outside that wrapper. There is no policy disagreement. No shared install,
server, push, tag, public-main merge, AGENTS.md, or CLAUDE.md changed.

Clock-derived report timestamp is in recorded-at.txt.
