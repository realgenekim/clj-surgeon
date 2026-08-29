# Agent usage after the alias breakthrough

<!-- agent-usage-window-end: 2026-08-29T07:32:24.734205Z -->

Date: 2026-08-29

Receipt:
`/tmp/clj-surgeon-agent-usage-20260828T145211111450Z-20260829T073224734205Z.json`

## Window and method

The bounded window is 2026-08-28 14:52:11Z through 2026-08-29 07:32:24Z,
or 2026-08-28 07:52:11 through 2026-08-29 00:32:24 Pacific time. The standard
privacy-safe collector used the preceding observation marker as its lower
bound. It emitted hashed session and target identities, action kinds, payload
sizes, direct wall, complete-turn wall, and completed-item clocks. It did not
emit transcript prose, source bodies, workspace paths, or raw service events.

The receipt covers 17 Clojure-relevant Codex sessions and one
Clojure-relevant Claude session. This is an activity window, not a balanced
provider benchmark. The Claude sample contained no Surgeon-using task turn,
so no Codex-versus-Claude performance conclusion is available.

## Scoreboard

| Measure | Codex | Claude |
|---|---:|---:|
| Sessions in window | 19 | 5 |
| Clojure-relevant sessions | 17 | 1 |
| Task turns | 78 | 0 |
| Surgeon-using turns | 21 | 0 |
| Recorded Surgeon calls | 222 | 0 |
| Recorded native reads | 781 | 2 |
| Recorded native patches | 144 | 0 |

Codex used 126 `inspect_clojure`, 22 `edit_clojure`, ten
`apply_clojure_changes`, 30 CLI `:cat`, 17 CLI `:show-form`, 15 CLI `:ls`, and
two CLI `:get` operations. CLI and MCP counts describe observed transport,
not whether a fallback was avoidable. A transport choice is only avoidable
when the caller had the equivalent surface and was not testing that transport.

## The agent boundary remains the dominant cost

The MCP service recorded 159 calls with a 249 ms median direct wall and 1.882
second p90. The Codex event clock found 185 completed Surgeon-to-next-action
boundaries with a 9.089 second median and 29.959 second p90. The completed
reasoning items inside those boundaries had a 2.930 second median. The rest is
unattributed and may include inference, scheduling, serialization, transport,
logging, or UI delay. It is not valid to label all of it hidden thinking.

```text
typical observed boundary

  Surgeon completes       next agent action arrives
          |-----------------------|
          0.249 s tool median      9.089 s boundary median
```

Sixty-eight boundaries ended in another Surgeon read and 42 ended in a native
read. Nineteen ended in a Surgeon mutation and 14 in a native patch. This
again points to compiled decisions and read missions, not micro-optimizing a
subsecond kernel, as the larger prize.

## The frozen 51-edit call exposes a larger special case

The two exact alias-aware Anvil calls provide a higher-resolution clock than
the broad usage receipt:

| Run | Argument bytes | Initial output | Call materialization | Server | Receipt interpretation | Complete wall |
|---|---:|---:|---:|---:|---:|---:|
| 02-B | 6,477 | 6.216 s | 42.214 s | 1.222 s | 2.928 s | 59.277 s |
| 03-B | 6,353 | 18.237 s including one unclassified message gap | 40.025 s | 1.127 s | 3.372 s | 63.595 s |

The request contains 33 exact edit rows. Actual `from` and `to` source is
2,595 bytes. Repeated file paths contribute 935 bytes and owner names 345
bytes. Roughly 2.5 KB remains in repeated keys, location structure,
punctuation, and transaction framing. A source-blind representation screen
found these zero-model sizes with the same semantic decision:

| Shape | Bytes | Reduction |
|---|---:|---:|
| Current row objects | 6,477 | — |
| File-grouped readable objects | 5,172 | 20.1% |
| Ordered tuples | 4,339 | 33.0% |
| Grouped mappings with readable sites | 4,490 | 30.7% |
| Ordered file/mapping dictionaries plus rows | 3,565 | 45.0% |

The smallest JSON is not automatically the best model interface. Numeric
indirection can save bytes while increasing transposition and reference
errors. The next screen must hold exact first-call correctness above payload
size or wall time.

## Failures and coverage limits

The service recorded 124 successful and 35 refused MCP calls. Twenty-eight
refusals were `batch-form-selection-failed`, four were
`invalid-intent-form`, two were `invalid-mcp-request`, and one was
`expect-count-mismatch`. The broad counts do not establish that every refusal
was avoidable. The retained alias cohort does establish one exact causal case:
a millisecond vocabulary refusal induced another 29–33 seconds of large-call
construction.

No cclsp or clojure-lsp event appeared in this window. This is a true zero for
the instrumented service window, not proof that no older, uninstrumented, or
external semantic route existed.

## Next falsifiable improvement

Screen two request representations against the now-correct alias-aware
control:

1. a readable grouped-replacement form near 4.5 KB; and
2. an ordered dictionary-row form near 3.6 KB.

Both must compile injectively to the identical 33 canonical edit rows, 51
matches, nine future hashes, and one owner-deletion group. A treatment loses
on any incorrect first call, refusal, recovery action, shell or source read,
or changed semantic hash. Only after a capture-only screen passes should a
real mutation cohort spend Anvil capacity.

