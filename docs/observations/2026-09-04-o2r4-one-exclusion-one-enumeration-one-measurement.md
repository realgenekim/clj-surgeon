# O2 round 4 — closing Sol's round-three NO-GO on `bridge/study-ops-mcp`

Written 2026-09-04T04:41Z by forge@anvil, worktree `/home/forge/src/clj-surgeon-study`,
from `e258519`. Nothing merged; nothing pushed to `main`. Fixtures under
`/tmp/o2r4-fx` only. One server was started, on explicit port 8060, and stopped.

Round three closed six findings and shipped a NO-GO. Every one of Sol's four
blockers reproduced VERBATIM in this checkout at `e258519` before any fix, and
each is closed RED then GREEN.

## The four blockers, reproduced and closed

| § | Sol's receipt, reproduced here at `e258519` | the rule that closed it |
|---|---|---|
| 2 | `empty_map spelling="{}" present=false label_present=false` · `fresh_missing= [[[:special :empty_map] {}] [[:special :nil_value] nil] [[:special :empty_vector] []] [[:results] []]]` · `product_predicate_missing= []` | ONE exclusion mechanism. An empty collection is a leaf; a value-less leaf is carried as `pointer=spelling` or not at all. |
| 3, 9 | `scanned_count= 22` · `helper_reason= duplicate-form included_in_scan= false refused= true` | The enumeration is the RUNTIME, enforced at construction. A source scan complements; it never ratchets. |
| 4 | `uncarried_count= 6` · `hypothetical_one_copy_public_bytes= 31869 hypothetical_fits= true` | Allowances are DERIVED from what the budget leaves. An elision names each dropped leaf. |
| 5 | `payload= 420 … huge_published= 32860 huge_bounded= false` (and 32,841 / 32,912 / 32,996) | The fit measures the FINAL published envelope. No reserve. |

## The same probes, re-run on the fixed tree

```text
§2  fresh_missing= []          product_predicate_missing= []     (was 4 misses)
§3  helper_reason= duplicate-form  refused= true  leaf_misses= []
    runtime-derived reason set = 23; literal scan = 22; diff = duplicate-form
§4  uncarried_count= 1 uncarried_paths= [[:error]]  public_bytes= 32026
    hypothetical_one_copy_public_bytes= 42048 hypothetical_fits= false
§5  payload= 400 fit_measure= 31571 normal_published= 31571 huge_bounded= true
    payload= 420 fit_measure= 32531 normal_published= 32531 huge_bounded= true
    payload= 430 fit_measure= 32512 normal_published= 32512 huge_bounded= true
    payload= 440 fit_measure= 32763 normal_published= 32763 huge_bounded= true
    payload= 460 fit_measure= 32667 normal_published= 32667 huge_bounded= true
```

Three of those lines carry the whole argument.

**§4's single remaining uncarried leaf is the one the budget genuinely cannot
carry.** Sol's fixture is a refusal whose `path` and `error` are each about
10,000 characters. Round three dropped six leaves with 10,921 bytes unspent;
round four drops exactly one, and the same probe now measures the hypothetical
complete rendering at 42,048 bytes against a 32,768-byte budget —
`hypothetical_fits= false`. The elision is forced, and the text names `error`
as the leaf it dropped.

**§5's `fit_measure` and `normal_published` are now the same number in every
row.** They differed by 17 bytes before, which is what the 64-byte reserve
existed to absorb. Nothing is added after the fit, so there is nothing to
reserve, and the fit's target is the declared budget itself.

**Sol's `label_present` column reads `false` for the value-less shapes and that
is correct.** His probe looks for `special.<key>:`; the label is rendered
`special.<key>=null`, with `=`, because `pointer=spelling` is the form that
distinguishes a rendered empty value from an absent one. His own independent
walker and predicate — `fresh_missing` — is the criterion, and it is empty.

## Sabotage, promoted from a review exercise to a test

Sol's rung D — rewriting `(refuse! :expected-object …)` as
`(refuse! (identity :expected-object) …)` — dropped the round-three scan from
22 to 21 with the whole suite green. It cannot do that now, and neither can
the helper route that was already present in ordinary source:

```clojure
(is (thrown-with-msg? IllegalArgumentException #"not enumerated"
      (#'inspect/unique-strings! ["a" "a"] ["requests" 0 "forms"] :brand-new-reason)))
```

`refuse!` throws a plain `IllegalArgumentException` rather than an `ex-info`,
precisely so the evaluator's `catch clojure.lang.ExceptionInfo` cannot turn a
defect in the namespace into a refusal a caller would read as its own fault.
A new reason therefore needs an edit to `refusal-reasons` AND a fixture that
reaches it; whichever is missing is a failing test.

## Small-read wire cost — reported, not fixed (Sol §11)

Sol's cost fixture was reaped from `/tmp` before this round, so the table below
is measured on a rebuilt equivalent (`/tmp/o2r4-fx/cost-project`: one
three-defn namespace with a require). The three columns are the SAME fixture at
three revisions, so the deltas are comparable even though the absolute numbers
differ from Sol's.

| mode | `a0b0520` public bytes | `e258519` (r3) | `760bb151` (r4) | r3→r4 | a0b0520→r4 |
|---|---|---|---|---|---|
| forms | 1,645 | 2,109 | 2,109 | +0 | +28% |
| outline | 1,402 | 1,740 | 1,778 | +38 | +27% |
| match | 2,136 | 2,713 | 2,737 | +24 | +28% |
| xray | 1,581 | 2,283 | 2,283 | +0 | +44% |
| deps | 1,258 | 1,522 | 1,560 | +38 | +24% |
| topo | 1,119 | 1,390 | 1,417 | +27 | +27% |
| ls-deps | 1,133 | 1,395 | 1,397 | +2 | +23% |
| ls-extract | 1,136 | 1,400 | 1,402 | +2 | +23% |
| ls-tree | 888 | 977 | 1,006 | +29 | +13% |

Round four adds 0–38 bytes per small read on top of round three's ~25%. Those
bytes are the value-less leaves that used to vanish — `results=[]`,
`truncated=false`-shaped facts, empty `platforms` lists — now printed with
their pointers. The largest absolute result here is 2,737 bytes against a
32,768-byte budget, so this is a cost to report, not a bound to defend.

Token cost remains unmeasured. Bytes are not tokens, and no arm has been run
that would turn this table into a claim about what a caller pays.

## What this round does NOT establish

- **No adoption or preference claim.** Nothing here says an agent chooses
  `inspect_clojure`, or reads the text block rather than `structuredContent`.
- **No token measurement.** See above.
- **The MEM-003 recomposition is prepared, not performed.** `invoke!` publishes
  exactly what its `:fit` returned, and a witness drives that with the envelope
  nested under `measured` — so the fit will measure the new wire shape when it
  lands. The recomposition onto `dd9d8b9`'s successor is still a separate job.
- **The exact-boundary HTTP probe from Sol's §6 was not re-run as written.** Its
  `candidate-at` helper searches on PRE-FITTED bytes, a quantity this round
  retires, so its target search no longer lands. The boundary is held in-suite
  instead: `the-published-pair-is-bounded-under-every-accepted-clock` drives the
  real callback entrance at four near-boundary receipts, and the HTTP witness in
  `mcp_http_server_test` asserts the leaf criterion across the wire. The HTTP
  probe re-run on port 8060 published `bounded= true` on both targets.

## Gates at the tip

```text
~/bin/suite-run bb test/run_all.clj
  Ran 731 tests containing 6023 assertions. 0 failures, 0 errors.
~/bin/suite-run clojure -M:clj-surgeon/mcp-test
  Ran 485 tests containing 6319 assertions. 0 failures, 0 errors.
make mcp-operation-oracle
  mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]
intent audit
  {:ok true, :violations []}
git diff --exit-code 4480e3d..HEAD -- test-fixtures/
  (empty)
```

One earlier run showed `direct-shell-shim-uses-the-same-host-admission` failing;
it is the admission-wait flake Sol also recorded at `6587c9d`, green on re-run,
and unrelated to anything in this round.
