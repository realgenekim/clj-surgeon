# Block B attempt 26 — the class Sol named: a Throwable escapes the probe servlet's EDN parse

Verdict: **GO**. The class is closed at the boundary, the oracle that finds it
runs in the merge gate, and one real `make landing-gate-prewarm` at the final
tip returned `:state :passed`, `:problems []`.

Final commit: `839b7c53457531ee749aab6182891d960403185c`
Branch: `bb-rewrite-tower-land-local` (from `df86ecda`, the receipt commit). Not pushed.

## What the oracle found at the tip, before the fix

Seven reader-recursive EDN shapes, POSTed through the REAL `probe-servlet`
entrance on a 512 KiB stack. **Ten shapes escaped as `StackOverflowError` with
`:wire-bytes 0`** — no refusal, no receipt, no bytes; the client reads nothing
and the thread of the *shared* warm image dies:

| depth | shapes that escaped | shapes that refused |
|---|---|---|
| 1,024 | discard `#_`, list `(`, map `{`, meta `^`, set `#{`, tagged `#a/b` | vector `[` |
| 2,048 | discard, list, set, vector | map, meta, tagged (over the 8,192-character bound first) |

Three more escaped through the Exception-only boundary directly
(`StackOverflowError`, `AssertionError`, bare `Error`): 13 escapes in all.
Depths 1–256 were already fine, and everything at 4,096+ was caught by the
existing character bound — so the hole is exactly the band where a request is
*bounded in characters and unbounded in depth*.

Two things the finding teaches that the old spec did not say:

1. **A character bound is not a depth bound.** 8,192 nested `[` fits the
   8,192-character request bound.
2. **Counting `[` does not enumerate the class.** `clojure.edn` recurses per
   *prefix form* too — `#tag`, `^meta`, `#_` — and those overflowed at depth
   1,024 while a vector at the same depth did not.

## The fix, at the boundary, in two halves

(a) **Pre-parse nesting bound.** `probe/read-bounded-request` counts
reader-recursive openers over the request CHARACTERS before `clojure.edn`
reads anything — containers (including `#{`) plus `#tag`, `^`, `#_`, skipping
openers inside a string, a character literal or a comment. Above
`probe/request-depth-bound` (**64**, against a closed request's own depth of
**two**) it refuses `:probe-request-too-deep` carrying `:bound`, `:depth`,
`:bytes`. The bound is the shape's ceiling with room — it is deliberately not
a guess at the reader's stack limit, so the reader's limit is never the thing
under test.

(b) **Throwable boundary.** `probe-servlet` catches `Throwable`, not
`Exception`; `probe/request-refusal` converts anything that still arrives into
`:probe-request-unreadable` carrying `:throwable-class` and **no stack**. The
bound keeps the parse shallow; the Throwable boundary holds when the bound is
wrong.

Both kinds are registered in `docs/intent/probe/refusals.edn` with the native
failure each prevents — *a request that kills the shared warm image's thread*,
and *a client that reads zero bytes as a verdict* — so the shared completeness
witness covers them, and `BB-PROBE-002` now states the bound, the counted
spellings, the Throwable boundary and the two misreadings that produced the
defect.

## Measurements

| run | result | log |
|---|---|---|
| tip reproduction, `-Xss512k` | 10 escapes / 10 zero-byte responses over 49 cases | `tip-escape-probe.log` |
| oracle RED at `df86ecda` | 13 throwables escaped, 10 with wire-bytes 0 | `oracle-red.log` |
| oracle GREEN at the fix | `{:test 3, :pass 589, :fail 0, :error 0}` | `oracle-green.log` |
| splice-envelope-test (refusal completeness) | `{:test 2, :pass 104, :fail 0, :error 0}` | `refusal-completeness.log` |
| mcp-http-server-test | `{:test 24, :pass 841, :fail 0, :error 0}` | `mcp-http-server-test.log` |
| mcp-hot-verify-test | `{:test 20, :pass 158, :fail 0, :error 0}` | `mcp-hot-verify-test.log` |
| receipt-booleans-test | `{:test 2, :pass 46, :fail 0, :error 0}` | `receipt-booleans-test.log` |
| `make test-fast`, once | exit 0; 1244 tests / 12560 assertions, 0 failures; fast 43,116 ms of 60,000 ms | `test-fast.log` |
| whole gate under `attempt18/restricted-gate.py`, once (diagnostic) | exit 2 — the frozen refusal-kind pin, 164 → 166 | `restricted-gate.log` |
| `make landing-gate-prewarm`, one real run at the final tip | exit 0; `:state :passed`, `:problems []`, `:wall-ms 394212`, git-head `839b7c53` | `prewarm-1.log` |

Gate budgets at the final tip: `cadence :fast` 45,673 ms of 60,000;
`cadence :integration` 66,976 ms of 240,000; `cadence :battery` 240,434 ms of
1,800,000; `bb-runtime` 238,142 ms of 343,102. `test-isolation: 0 violations
across 101 namespace(s)`. Every line verbatim in `gate.md`.

The oracle namespace is in the merge gate, not battery-only: `mcp-test`
lane 19, `clj-surgeon.mcp-http-server-test` exit 0, 16,328 ms lane /
3,397 ms namespace wall.

## DOGFOOD — every source edit

| file | intent | mechanism | refusal | did the repair text suffice? |
|---|---|---|---|---|
| `test/clj_surgeon/mcp_http_server_test.clj` | the class oracle: 7 shapes x 9 depths on a 512 KiB stack, plus an openers-only control and a Throwable-boundary witness | native `Edit` on the exact form | none | n/a |
| `test/clj_surgeon/deftest_census.edn` | register the 3 new witnesses | repository `CENSUS_REGENERATE=1` entrance; diff read before commit (+3, -0, 2586 total) | none | n/a |
| `src/clj_surgeon/probe.clj` | `read-bounded-text` split, `request-recursion-depth`, `read-bounded-request`, `request-refusal`, `request-depth-bound` | native `Edit` on the exact form | none | n/a |
| `src/clj_surgeon/mcp_http_server.clj` | widen `probe-servlet`'s boundary to `Throwable` | native `Edit` on the exact form | none | n/a |
| `docs/intent/probe/refusals.edn` | register both kinds with their native failures | native `Edit` on the exact form | none | n/a |
| `docs/intent/hot-verification/bb-probe-specs.md` | BB-PROBE-002 states the bound, the spellings and the Throwable boundary | native `Edit` | none | n/a |
| `test/clj_surgeon/mcp_alias_migration_test.clj` | pin the two new kinds, 164 -> 166 | native `Edit` | **`the-refusal-enumeration-is-pinned-in-count-and-in-membership`** refused the change | **yes** — the failure named both kinds literally and both directions of the set difference; no investigation was needed |

## Commits

| sha | what |
|---|---|
| `f7bcbce778c8ef581dc3963c4d8ca87f481aa1c0` | test: RED class oracle (red log path in the message) |
| `091114e1f3be9d09e9030890cd136eaf41c32a1e` | probe: pre-parse nesting bound + Throwable boundary + both intents |
| `839b7c53457531ee749aab6182891d960403185c` | test: pin the two new kinds in the frozen entrance enumeration |

`docs/observations/2026-09-12-bbtower-block-b/attempt26/` is deliberately
UNCOMMITTED.

## Least sure

1. **The depth measure is deliberately conservative and could over-refuse.**
   It is `peak container depth + CUMULATIVE prefix count`, and prefixes are
   never discharged — so a flat request carrying 65 unrelated `#_` forms would
   refuse at depth 65 though the reader would never recurse that deep.
   Over-refusing is a bounded typed refusal; under-refusing is a
   StackOverflowError. A closed probe request measures 2, so the margin is 62,
   and `probe-request-depth-bound-reads-only-real-openers` witnesses a
   63-deep request being accepted into the request contract.
2. **The oracle pins the small stack to a 512 KiB THREAD, not the test JVM.**
   Same reproduction as Sol's `-Xss512k` and deterministic inside the gate's
   shared JVM, but it does not prove behaviour at the gate JVM's default stack
   — which is larger, i.e. weaker pressure, so this is the harder case, not a
   weaker one.
3. **The bound value (64) is a judgement, not a measurement.** It was chosen
   from the request shape's own depth, not from where the reader overflows
   (which varied by shape between 256 and 2,048 at 512 KiB, and would move
   with the stack size — which is exactly why the bound must not be derived
   from it).

## Disagreements

None with Sol's round-8 verdict. His reproduction is confirmed verbatim at the
tip, and the class is broader than he showed: `^` metadata and `#_` discard
overflow too, and they are not containers, so a fence that counted only
container openers would have passed his reproduction and still shipped the
defect.

## Owed

1. **The CLIENT side of the same class is untouched.** `probe/cli!` still
   catches `Exception` only and `read-bounded` still has no nesting bound, so
   a broken or hostile SERVER can overflow the babashka client's stack with a
   deeply nested response. Out of the named class (request shapes at the
   servlet), not fixed and not witnessed. This is the next finding, not a
   regression.
2. `docs/intent/receipt-booleans/registry.edn` was not touched: the two new
   kinds carry no receipt booleans.
3. The oracle adds roughly 3.4 s to `mcp-http-server-test`'s namespace wall.
   Integration is at 66,976 ms of 240,000 ms, so nothing is at risk, but 63
   servlet drives on a fresh 512 KiB thread each is the expensive part if the
   integration budget ever tightens.
