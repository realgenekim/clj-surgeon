# Block B attempt18: PASS

Attempt17's remaining steps are complete. The whole restricted gate passed,
`make test-fast` passed once, and the second and final counted prewarm passed.
The first counted prewarm failed because I copied evidence into the working
tree while it was running. That failure is retained, not waived or omitted.

Source repairs are committed as `30808abc354493f876bc7af3857ae4e5fe5cfe0a`,
starting from `172a068553ba38048ae602bc99ed85b7cacb6b48` on
`bb-rewrite-tower-local`. This attempt's evidence directory is committed last.
Both green gate receipts cover the same source digest:
`b57f4e1d55ed702f29bf8d8ae33aa3e114b1e88bc5a21208dff643431af61544`.
The final evidence commit adds observations only; it is not the receipt's Git HEAD.

## Findings and repairs

`restricted-gate.py` preserves attempt17's three roots, packet-shaped UUID
TMPDIR, JAVA_TOOL_OPTIONS with explicit heap and tmpdir, removal of
_JAVA_OPTIONS, TRUNCATE mediation and ABI >= 3. Its sole permission addition
is WRITE_FILE on `/dev/null`, matching `/home/forge/bin/lib/packet.py:78–82`.
The full gate now exercises the envelope rather than failing its first redirect.

The diagnostic plus the unreached-command census exposed five fixture writers:

1. Cell B's unit fixture created `/var/tmp/b07-lint-*` outside TMPDIR.
2. The papercut baseline mirror used `/var/tmp/split-oracle-*`.
3. Cell B's shell default used `/var/tmp/forge/plan2/cellB/run-$$`.
4. The alias suite's receipt publication witness indirectly loaded the helper
   fixture default `/var/tmp/forge/helper-fx`. It now inherits java.io.tmpdir.
5. The clj-kondo hygiene witness put its checkout under TMPDIR but registered
   it in the source's external common Git directory. A disposable shared clone
   now owns its Git metadata under TMPDIR, checks out the exact HEAD, and keeps
   the same before/after ignored-file comparison.

The first three were fixed first. Explicit fixture overrides and all test
assertions remain. [envelope-census.md](envelope-census.md) retains every red
node and exact observed path, identifies inferred parent paths honestly, and
records keep/change decisions for the direct-node and expanded Clojure scans.
The original console excerpt truncates Cell B stderr; `diagnostic-run/lane-0.err`
retains all twelve errors. The gate's early exits were covered by a sequential
`make -k` over every unreached command; its only additional red was the Git
registration. The later complete wrapper run passed all seven stages.

## Verification

| Invocation | Result | Evidence |
|---|---|---|
| Whole-gate diagnostic, not counted prewarm | Exit 2; 126.6481672860682 s | envelope-red.log; diagnostic-run/ |
| Static writer probes, old source | 2 probes: 1 failure, 1 error | static-red.log |
| Unreached commands | Exit 2; 316.195281105116 s; only kondo hygiene red | unreached-red.log |
| Repaired static writers, Cell B and kondo hygiene | Exit 0; 6.398247377946973 s | focused-green.log |
| Helper publication boundary | 24 tests / 202 assertions; exit 0; 10.946637454908341 s | helper-green.log |
| Format and paved lint | Changed form retained; lint 0 errors / 0 warnings | format.log; lint.log |
| Whole restricted gate | All 7 stages exit 0; receipt 390645 ms; wrapper wall 392.7211568816565 s | envelope-green.log; envelope-green-receipt.edn; envelope-green-run/ |
| `make test-fast`, exactly once | 1243 tests / 11778 assertions; 0 failures/errors; fast sum 44734 ms < 60000; makespan 24134 ms | test-fast.log; test-fast-run/ |
| Counted prewarm 1 | Exit 2; 176 isolation violations from 44 observer-created files seen by 4 namespaces | prewarm-1.log; prewarm-1-run/; prewarm-repair.md |
| Counted prewarm 2, final | All 7 stages exit 0; 386004 ms; every suite has 0 isolation failures | prewarm-final.log; prewarm-final-receipt.edn; prewarm-final-run/ |

Final counted prewarm: alias 182 tests / 3641 assertions, MCP 1411 / 15576,
bb 899 / 7984, and standalone bb diagnostic 830 / 7283; all have zero
failures/errors. Fast sum 45629 ms < 60000; integration 67783 ms < 240000;
bb runtime 235333 ms < 343102. The currently registered bb ceiling is unchanged;
this does not claim it existed at the original base. All values come from
`prewarm-final.log`. No performance comparison is claimed.

The restricted gate's slot oracle preserves three existing skips: two
path-socket-only cases on Linux and the named sun_path byte-budget case.
The ordinary final prewarm preserves two path-socket-only skips and executes
the byte-budget witness with its shorter root. No skip was introduced here.

`gate.md` is the byte-for-byte concatenation of both counted prewarm outputs,
including every refusal-census, budget and isolation line. The separate logs
are exact slices. The final receipt records `:state :passed`, `:prewarm? true`,
`:landing? false`, `:problems []`, run `155a0aec-7e51-42a0-9ba5-64d325031378`,
completed `2026-09-12T14:02:35.379045064Z`.

## The one counted repair

I archived `test-fast-run/` after starting counted prewarm 1. The existing
TEST-ISO-003 oracle correctly refused those new files. Every one of its
176 violations is accounted for in `prewarm-repair.md`; none is a denied
filesystem path or a product test assertion failure. The repair was to finish
archiving before starting the final prewarm and leave the working tree alone
until it exited. No code or oracle was changed to obtain the final pass.
Exactly two counted prewarms ran; there was no third attempt and no second
test-fast run.

## DOGFOOD

| Source edit | Intent | Mechanism | Refusal type | Repair text sufficed? |
|---|---|---|---|---|
| attempt18/restricted-gate.py | Packet device parity, retaining other restrictions | Copy of attempt17 + native apply_patch; real whole-gate runs | No edit refusal; original /dev/null EACCES resolved by supplied parity fact | Yes |
| test/oracles/test_cell_b_oracle.py | Owned fixture scratch | Native apply_patch; original red and full/focused green | Runtime EACCES at twelve fixture roots | Yes |
| test/oracles/namespace_split_papercut_oracle.py | Owned baseline mirror and accurate documentation | Native apply_patch; actual mirror creation in static-probes.py | Runtime EACCES | Yes |
| test/oracles/cell_b_oracle.sh | TMPDIR-derived default, retained override | Native apply_patch; execute actual setup fragment | Runtime mkdir permission denial | Yes |
| test/clj_surgeon/mcp_helper_extraction_test.clj | Inherit runner's private JVM temp root | Exact-form native apply_patch; formatter; ~/bin/clj-kondo; actual publication witness | Runtime FileNotFoundException after denied parent creation | Yes |
| test/clj_kondo_admission_path_test.sh | Own Git metadata as well as checkout | Native apply_patch; real shell self-test under wrapper and both gates | Git exit 128, external common-directory EACCES | Yes |
| attempt18/static-probes.py | Exercise the two writers the gate unit tests do not reach | Native file creation; red then green under wrapper | Expected old-source filesystem failures | Yes |
| docs/tech-tree.md | Record the witnessed envelope finding | Native apply_patch | None | N/A |

No Surgeon mutation was called: these ordinary edits use the working-tree
skill's native route. The linked-intent-testing skill was found at
`/home/forge/src/claude-skills-nrepl/linked-intent-testing/SKILL.md`; existing
temp-hygiene intent and the user's authorization govern the fixture repair.
The full-file formatter exposed unrelated legacy drift; it was discarded and
only the changed form retained. The first cleanup batch was rejected for its
`rm -f` style command before execution; exact-path unlink and receipt-validated
owned-root cleanup completed safely instead.

## Limits, disagreements and owed

The wrapper proves this filesystem envelope on this host; it is not a real
packet attestation or landing authority. The prewarm correctly says
`:landing? false`. Fable's independent review, any outstanding original-block
ratifications, and a future actual landing/battery decision remain external.
No required attempt18 execution remains owed. There is no policy disagreement;
the first counted failure was my evidence-copy error.

Six UUID packet directories created by this attempt were removed after evidence
retention; `cleanup.log` names each. Earlier attempts and shared installs were
left alone. No budgets, membership, runtime assignments, shared filesystem permissions,
servers, pushes, tags, merges, AGENTS.md or CLAUDE.md changed.

Recorded from the clock: 2026-09-12T14:03:26.345284+00:00.
