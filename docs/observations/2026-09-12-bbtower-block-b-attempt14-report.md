# Attempt 14: PASS

The formatter launcher fix is committed on `bb-rewrite-tower-local` at
`e679393fe5b122b3f21d7b8418f7c57b58da70f8`, starting from
`c2ec3039a5b4382d2353e9c9f2b2f1b7e0db3dd1`. New evidence lives under attempt14.
The final observation commit follows verification; it is not itself a tested
source change. No push, tag, landing, shared installation or shared service
operation occurred.

## What changed and why

The product process launcher overrides inherited npm cache and log paths with
`<selected-temp-root>/npm-cache` and `<selected-temp-root>/npm-logs`. The policy
preserves nonblank TMPDIR except `/tmp` and `/dev/shm` trees, otherwise selecting
`/var/tmp`, as in attempts 12–13. This applies outside packet/test execution too.

For the default formatter, executable `standard-clj` on PATH wins, followed by
checkout `node_modules/.bin/standard-clj`, then the existing npx command. Custom
formatter argv is preserved. Each invocation retains expanded command and
resolution status in `:formatter`; the typist's `:format` receipt retains it.
The installed binary avoids npm altogether. The fallback remains capable of
using the network, but its npm writes are redirected away from HOME.

## Red and green evidence

The committed red witness invokes `format-candidates!` with the real default
command and an empty mode-0555 npm cache. At the original source tip, npx exits
1 with EACCES creating `_cacache` and reports that `_logs` could not be written
([red.log](red.log), [red.exit](red.exit)). This is the packet's native failure,
not a mocked process result. Red was committed before implementation.

After the fix, the same inherited read-only cache/log locations permit both
the resolved binary and real npx fallback to format a multiline source exactly
([green.log](green.log), combined [green.exit](green.exit) = 0). Only executable
discovery is made absent for the fallback; the command and launcher are real.
The receipts name the installed cli.mjs with `:resolved? true` and `/usr/bin/npx`
with `:resolved? false`. [npm-paths.txt](npm-paths.txt) records actual writable
cache/log directories under selected TMPDIR and the read-only inherited path.
No relative performance claim follows from these individual execution walls.

The initial BB witness invocation lacked the nrepl dependency; two script
forms initially had missing closers. A first green assertion expected inline
spacing that standard-clj intentionally preserves. The witness now uses a
multiline sample with observable indentation. All those setup/expectation
failures are retained separately; none is counted as the required native red.

Focused launcher/formatter tests pass 26 tests / 125 assertions, zero failures
or errors ([focused-green.log](focused-green.log)). The selection matrix covers
PATH priority, checkout fallback, npx fallback and custom commands; existing
success, nonzero, timeout and exception checks assert receipt identity. The
environment witness begins with home cache/log values and asserts their
replacement. One new test was added to the census; no test was removed.
Formatting and serialized `~/bin/clj-kondo` lint pass with zero warnings/errors
([format.log](format.log), [lint.log](lint.log)).

## Fresh HOME and required suites

The formerly red test is owned by `receipt-artifacts-boundary-test`; it invokes
`mission-typist-executor-test/real-proof-commit-and-undo`, explaining the stack
location in the brief. The entire owning namespace ran once under fresh empty
TMPDIR and scratch HOME: 24 tests / 202 assertions, zero failures/errors and
zero isolation violations, namespace wall 3,287 ms ([namespace.log](namespace.log)).
Both HOME endpoint listings contain only the root itself; [home-diff.txt](home-diff.txt)
is empty. TMPDIR was empty afterward and all owned roots were removed.
[focused.sh](focused.sh) retains the exact invocation and listing procedure.

`make test-fast` ran exactly once and passed: 1,243 tests / 11,778 assertions
plus clj-splice's 5 / 59; zero failures/errors. Fast cadence was 43,190 ms against
the unchanged 60,000 ms ceiling. The bb runtime sum was 14,778 ms against
343,102 ms, makespan 10,614 ms. Evidence: [test-fast.log](test-fast.log),
[test-fast.exit](test-fast.exit). No lane membership, runtime assignment or
budget changed.

First prewarm refused solely on four TEST-ISO-003 working-tree violations,
all naming this agent's concurrent edit of DOGFOOD.md. All assertions passed
(MCP 1,411 tests / 15,576 assertions) and all budgets passed. This was execution
contamination by the agent, not a formatter failure; it is not counted green.
[gate-first.md](gate-first.md) and [gate-first-mcp-receipt.edn](gate-first-mcp-receipt.edn)
retain the refusal. The one repair freezes repository writes and captures final
output outside the worktree until process exit; no product/oracle change.
The second and last prewarm passed: exit 0, `:state :passed`, `:problems []`,
`:landing? false`, complete wall 387,383 ms, run
`f9b818cd-f120-4bb6-8f33-dfc611e81e12`. All required stages exited zero.
[gate.md](gate.md) is byte-identical to the externally captured stdout/stderr;
[gate-receipt.edn](gate-receipt.edn) preserves the machine receipt.

| Final prewarm measurement | Result | Unchanged ceiling | Evidence |
| --- | ---: | ---: | --- |
| Alias battery cadence | 81,374 ms | 1,800,000 ms | gate.md:80 |
| MCP fast cadence | 43,167 ms | 60,000 ms | gate.md:480 |
| MCP integration cadence | 67,606 ms | 240,000 ms | gate.md:481 |
| bb runtime sum | 235,276 ms | 343,102 ms | gate.md:760 |
| bb runtime makespan | 86,493 ms | — | gate.md:760 |
| Serial bb diagnostic wall | 218,655 ms | — | gate.md:1098 |
| Complete prewarm wall | 387,383 ms | — | gate-receipt.edn |

Alias: 182 tests / 3,641 assertions. MCP: 1,411 / 15,576. bb: 899 / 7,984.
Serial diagnostic: 830 / 7,283. All passed. Exactly one fast suite, two
prewarms, and one procedural repair were used; no suites ran concurrently
outside the repository coordinator's own bounded lanes.

## Commits, dogfood and limits

| Commit | Purpose |
| --- | --- |
| 5197d751ac984d370e158db27332bb2af0375647 | Preserve prewarm-checkonly as prewarm-checkonly-run3 |
| 7a266c614f9b18dd3d281807ec43d18a03119a49 | Committed real-command EACCES red and plan |
| e679393fe5b122b3f21d7b8418f7c57b58da70f8 | Launcher fix, receipts, linked intent and regression coverage |

[DOGFOOD.md](DOGFOOD.md) records every source edit and its mechanism/refusal;
[commands.md](commands.md) records execution and setup corrections.
The read-only cache stand-in is not a Landlock rerun. Scratch HOME remained
empty at both endpoints; this cannot exclude transient create/delete activity.
JVM user.home stayed at the real account value, and product artifact writes
remain governed by their existing state-root policy. No general mount/symlink
confinement or offline guarantee for the npx fallback is claimed.

Least sure: behavior when a different installed formatter version is selected;
PATH preference intentionally follows the caller's installation. Disagreement
with the brief is only ownership of the red namespace; its exact named test
did run. Owed to Fable: independent review and a fresh packet-envelope recheck.
A prewarm does not certify landing or the omitted full battery.
