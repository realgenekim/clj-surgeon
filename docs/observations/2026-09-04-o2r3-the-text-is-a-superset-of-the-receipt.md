# O2 round 3 — closing Sol's round-two NO-GO on `bridge/study-ops-mcp`

Written 2026-09-04T03:01Z by forge@anvil, worktree `/home/forge/src/clj-surgeon-study`,
from `a0b0520`. Nothing merged; nothing pushed to `main`. Fixtures under
`/tmp/o2r3-fx` only. No Surgeon server was started.

Round two shipped twelve commits and a NO-GO. This round closes all six
findings, RED then GREEN per finding, with each RED quoting the figure Sol's
own probe produced in THIS checkout before the fix.

## What each finding cost, and what closed it

| § | Sol's receipt, reproduced here | closed by |
|---|---|---|
| 2 | `plus_one_structured_bytes= 32558 plus_one_fit_ok= false` — a receipt 210 bytes UNDER the budget refused | bisection of the text allowance to zero, then two further rungs, then a refusal |
| 2 | `payload=100 structured=31549 fit_ok=false` — the 512-character per-result floor made a 32-result batch unrenderable | an imposed limit lowers the floor; whole result blocks drop from the tail |
| 2 | `single_row_source_in_text= false abridged_notice= false terminal_claim= true` | a row whose body does not fit renders as its row line and COUNTS the dropped body |
| 2 | `! text abridged · 97 of 200 rows` beside `✓ complete tree · read_complete=true` | one line, one claim |
| 3 | 103 uncarried `structuredContent` leaves over ten calls | `receipt-fact-lines`: every leaf the structural rendering misses prints as a bounded `path: value` line |
| 4 | `next call: inspect_clojure mode=ls-tree dir=. format=text limit=16384` | the verbatim compact JSON argument object, identical to the typed path |
| 5 | 18 of 22 `refuse!` reasons unreached; `cause_unbounded= true text_chars= 10612` | the enumeration is scanned out of the source; the cause is bounded with a marker |
| 6 | a refusal fact named in `refusal-structural-keys` shipped text-blind with 5,998 assertions green | the same leaf criterion applied to refusals |
| 8 | the retired "source-free companion" contract still stated in three documents | retired in place, dated, with a witness |

## The finding-6 escape, and why closing it produces no red test

Sol's escape was "add a refusal fact, name it in the exclusion set, supply no
renderer." Rerun on the fixed tree, that sabotage no longer HAS a red state to
find, because the defect it exploited cannot occur. Three sabotage runs, on
the committed GREEN tree, say where the ratchet now lives:

| sabotage | what it does | result |
|---|---|---|
| A — Sol's exact escape | `:sabotage` added to `refusal-structural-keys` and emitted on every `missing-fields` refusal | **suite green, and the fact IS in the text**: `value_reaches_the_text= true`, rendered as `  sabotage: HIDDEN-REFUSAL-FACT`. Before the fix the same probe on the same tree returned `value_reaches_the_text= false` with the suite green. The escape is now unrepresentable, not merely detected. |
| B — hide it properly | `:sabotage` added to `text-excluded-leaf-keys`, the real exclusion set, and emitted | **RED**, 1 failure: `the-excluded-leaf-set-is-frozen-and-every-member-carries-its-reason`. Growing the exclusion set is a failing test. |
| C — break the renderer | `receipt-fact-lines` silently skips leaves named `:hash` | **RED**, 2 failures: `forms: 1 receipt leaves the text does not carry`, `match: 1 …`. |

So the class is closed at three rungs: the primitive makes the original defect
impossible, the exclusion set is frozen by an equality assertion, and the
renderer is held by the coverage witnesses.

## Per-mode leaf coverage, before and after

Measured by the round-three witness over one fixture (uncarried leaves,
excluding the enumerated `:workspace_root`):

| mode | RED `ba6e29b` | GREEN | refusal kind | RED `08e4490` | GREEN |
|---|---|---|---|---|---|
| xray | 25 | 0 | form-not-found | 59 | 0 |
| forms | 18 | 0 | study-form-not-found | 5 | 0 |
| outline | 12 | 0 | invalid-study-limit | 5 | 0 |
| match | 10 | 0 | invalid-format | 5 | 0 |
| ls-deps | 9 | 0 | dir-not-found | 5 | 0 |
| ls-extract | 8 | 0 | missing-fields | 4 | 0 |
| topo | 8 | 0 | unknown-fields | 4 | 0 |
| deps | 7 | 0 | expectation-mismatch | 4 | 0 |
| ls-tree | 3 | 0 | unsupported-operation | 4 | 0 |
| ls-tree/names | 3 | 0 | invalid-xray | 4 | 0 |
| | | | file-not-found | 4 | 0 |
| | | | match-expectation | 4 | 0 |
| | | | unknown-parameter | 4 | 0 |
| **total** | **103** | **0** | **total** | **111** | **0** |

Sol's own probe, which knows nothing of the exclusion set, agrees: every typed
mode's `all_missing` fell to 1 (that one leaf being `:workspace_root`) and
every `row_missing` to 0.

## What this round does NOT establish

- **The text block roughly doubles for small reads.** A `forms` call on one
  form went from 1,907 to 2,421 public bytes. That is the price of the
  criterion, and it is bounded, but nobody has measured what it costs an agent
  in tokens across a real session. That is a measurement, not an argument.
- **The budget-abridged path is honest, not complete.** `ls-tree dir=src
  limit=16384` on this repository still cannot carry its own tree in the text.
  It says so, names `structuredContent`, and never reads as terminal. A caller
  that reads only text still does not have the answer.
- **`publish-reserve` is a constant, not a proof.** 64 bytes covers the
  difference between the rendering measured with the clock at zero and the
  rendering published with the real clock. It is comfortably larger than any
  elapsed rendering this tool can produce; it is not derived.
- **Two `mcp_process_test` cases flaked once** under load
  (`stale-owner-text-does-not-own-the-operating-system-lock`,
  `explicit-admission-skips-a-path-shadowing-shell-shim`) and passed on an
  immediate rerun with identical code. Unrelated to this branch; worth a look
  by whoever owns them.
