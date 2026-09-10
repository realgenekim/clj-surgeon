NO-GO

# Sol fence review, round 14: preservation brief

- reviewed: sealed candidate `e8b25243e0c3aef7f8d08019ecce8a067a16eca8`
- branch tip accepted by the brief: `04110f80ba8feb2bd9f21a31210526687823a5db`
- base: `3ea3803ea2e303a961a3edc1d76b8095ab5ff4f7`
- prior verdict: round 13 `NO-GO` on PB-FENCE-020
- excluded as directed: `make test`; verification used `bin/` only

## Blocking finding

### PB-FENCE-021 — R8 checks only the first `file-refusal` definition

The candidate's live `file-refusal` is table-driven and currently has no independent
branch: it is a single `some` over the ordered `refusal-kinds` table. The new R8
source-shape check does not establish that this remains the sole dispatch authority,
however. `file-refusal-violations` finds only the first textual occurrence of
`(defn file-refusal`, truncates at the next top-level `def`, and never checks for a
second definition or another refusal collection elsewhere in the brief.

I exercised the additional drift shape requested in the review brief in an isolated
copy at `/var/tmp/forge/item3/r14-drift.ejQvFB`. Immediately after the candidate's
table-driven function I added:

```clojure
(def alternate-refusal-kinds
  [{:kind "declaration-present"
    :predicate (fn [f] (boolean (seq (:declares f))))
    :detail (fn [_] "a declaration exists")}])

(defn file-refusal [f]
  (or (some ... alternate-refusal-kinds)
      (some ... refusal-kinds)))
```

The real `bin/preservation-tables-test` still exited 0 through R1-R8 and printed its
PASS. The metadata-wrapped valid declaration fixture then exercised the unenumerated
path: both trees were refused as `declaration-present`, while the dumped refusal table
still contained only the candidate's seven triggerable rows and one defensive row.
Thus a second `some` over a different collection creates a real refusal kind that R8
does not see, exactly the independent-authority drift PB-FENCE-020 was intended to
make impossible.

Repair must make the test reject multiple `file-refusal` definitions and any refusal
dispatch outside the one table, or derive/check the complete return surface
structurally rather than inspecting a 700-character slice of the first textual match.
That repair touches `bin/`, which the ship-fix contract classifies as
`HOLD reason=oracle-changed`, so I left no `GO-WITH-FIX` patch.

## Candidate checks completed

- The unmodified candidate's `bin/preservation-tables-test` exits 0. R1-R8 pass;
  all seven triggerable kinds reach their expected refusal, and the one defensive row
  remains documented and excluded from generated valid-input specimens.
- A bounded replay run re-established clean Cell C at 141 moved, 99 preserved moved
  bodies, 32 in-place bodies needing review, and zero reconciliation/refusal,
  require-order, undefined-alias, or forward-reference failures. The wrong-binding
  probe reduced moved-body preservation from 99 to 98. Promotion, dropped-comment,
  omitted-owner, require-sort, and local-shadow rows also retained their expected
  distinguishing signals before the run was stopped once the independent R8 drift
  blocker was reproduced.
- `77ee1e41..04110f80` changes only `bin/preservation-brief` (+67/-55) and
  `bin/preservation-tables-test` (+37/-2); `git diff --check` is clean. The history is
  seventeen commits over `3ea3803e`.
- The implementation files import no `clj-surgeon.*` namespace. Receipt consumption
  remains in the receipt-comparison derivation and Section C; A/B inventories are
  independently derived. Replay still starts from `git archive` of the real
  Curtaincall CFP trees `d9205abc` and `65ad613b`.
- Section B's hard stops remain prominent. Section 9 still preregisters 21 specimens,
  63 observations, two observations per protected class per arm, no catch-rate
  estimate at that denominator, and strict dominance as the safety co-primary.

## Decision

The candidate implementation repairs the current duplicate authority, but its new
ratchet still permits the requested independent drift shape and stays green while a
valid file reaches an unenumerated refusal kind. The reviewer would again have to
inspect all refusal dispatch manually. The proof-burden boundary therefore remains
open and the candidate must not ship.


> END RECEIPT (fence-run): worktree HEAD at review exit = e8b25243e0c3aef7f8d08019ecce8a067a16eca8 = fenced sha.
