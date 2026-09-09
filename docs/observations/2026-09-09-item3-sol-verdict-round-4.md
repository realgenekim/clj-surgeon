NO-GO

# Sol fence review, round 4: preservation brief

- reviewed: sealed candidate `50bcd107919fbf1b3d5810f234f005df51a3bc7c`
- branch tip accepted by the brief: `77126c8d81a7ad9f1d0e6cf09fafd9bc0f35c819`
- base: `3ea3803ea2e303a961a3edc1d76b8095ab5ff4f7`
- prior verdict: round 3 `NO-GO` on PB-FENCE-006/007/008
- verification: table ratchet; full 13-row real-tree replay; the three permanent round-3 pair rows; independently planted `for`, multi-arity `fn`, `catch`, and `when-not` scope witnesses; an independently planted referred-name collision pair; independence, section-B prominence, real-tree provenance, preregistration, and exact candidate delta
- excluded as directed: `make test`

## Blocking finding

### PB-FENCE-009 — a referred binding macro whose name is in a modelled table still lets A certify a changed binding

The round-4 repair makes alias resolution precede the uppercase-class heuristic, but bare table names still
precede all namespace resolution. `head-status` returns `:modelled` for any spelling in `modelled-heads` at
`bin/preservation-brief:365`; only afterward, in the unreachable `:else` branch, does it derive the file's
`:refer` target at lines 369–375. The table calls bare `testing` a non-binding `clojure.test` macro at lines
290–299, but the source file is free to refer a different Var named `testing`.

I planted a pair on copies of the real Cell C trees. Both trees add this binding macro to the already-required
`cfp-scheduler-killer.committees` namespace and refer it as `testing`:

```clojure
(defmacro testing [bindings & body]
  `(let ~bindings ~@body))
```

The base `committee-page` contains:

```clojure
(testing [header :probe] header)
```

The candidate contains:

```clojure
(testing [header :probe] organizer-layout/header)
```

Those expressions resolve differently: the base names the macro's local binding and the candidate names the
relocated Var. The brief nevertheless leaves `committee-page` at
`:identical-modulo-requalification`, keeps **103 of 141** relocated bodies mechanically preserved, and does
not name `testing` among the unmodelled heads. B3 lists the bare `header`, but that does not repair section
A's false preservation claim. This fires the explicit section 9.8(6) kill switch.

Witness trees and retained output:

- base: `af479e73eaae1891f154d33a441ed8be6dbd21bc`
- candidate: `af46602f80106cc30133366da43b863d540aac3a`
- brief: `/var/tmp/forge/item3/sol-round4-replay/out/referred-testing-valid.md`
- machine result: `/var/tmp/forge/item3/sol-round4-replay/out/referred-testing-valid.json`

Required repair: resolve a bare head through the file's namespace before applying any spelling-based core or
test table. Apply a table's semantics only when the resolved Var is the intended `clojure.core` or
`clojure.test` Var; a referred external macro must be modelled from its resolved definition or frozen as
unmodelled. Add this pair as a permanent replay row and make the ratchet exercise resolution collisions, not
only isolated source strings.

The repair changes `bin/`, which the ship-fix contract marks `HOLD reason=oracle-changed`; I therefore did
not patch it and cannot answer `GO-WITH-FIX`.

## Requested probes and controls

- **Round-3 probes:** the permanent `are-binding`, `uppercase-alias`, and `kind-collision` pair rows all move
  preserved bodies 103→102 and put `committee-page` in `:changed`. The earlier external-macro row also moves
  103→102. PB-FENCE-006/007/008 are repaired for their exact specimens.
- **Table ratchet:** `bin/preservation-tables-test` passes R1–R4: 203 function entries, 48 non-frozen heads,
  41 claims carrying 45 executed witnesses, and six named historic replay rows.
- **Reviewer-planted scope witnesses:** nested `for :let` map destructuring, a multi-arity named `fn`, and a
  `catch` binder each return `BOUND`; `(when-not (nil? x) (identity probe))` returns `FREE`. These are distinct
  from the builder's literal witness strings.
- **Full replay:** all 13 rows complete with exit 0. Wrong binding, silent promotion, dropped comment, omitted
  owner, both load-order defects, local shadow, symlink escape, external macro, and the three round-3 pairs are
  all surfaced. The clean Cell C row remains 103/141 relocated and 32/64 caller bodies preserved.
- **Section-B-only defects:** promotion and require-order specimens exit 3 and are hoisted above A beneath
  `HARD STOP`, `Do not consume`, and “A signal that lives in section B is not a weaker signal.” That is loud
  enough; these design obligations should not be relabelled as body-identity failures.
- **Independence:** none of the three implementation files requires `clj-surgeon.*`. The brief loads only the
  independent bin scanner. A/B facts are derived before `read-receipt` is called at line 1007; receipt values
  feed the C comparison and its hard-stop summary, not the A/B derivation.
- **Real-tree provenance:** `bin/preservation-replay:14-16` pins Curtaincall CFP `d9205abc` and `65ad613b`, and
  lines 26–36 build the scratch repository with `git archive` of those trees. It is not a synthetic seed.
- **Preregistration:** section 9 explicitly says nothing has run; 21 specimens × 3 arms = 63 observations and
  9 reviewers × 7 = 63. It states two observations per protected class per arm, refuses to estimate a catch
  rate at that denominator, and preregisters strict dominance before any arm. This is honest.
- **Exact delta:** `1cb98b5e..77126c8d` changes only `bin/preservation-brief` (+116/-52),
  `bin/preservation-replay` (+69/-3), and adds `bin/preservation-tables-test` (+228/-0).
  `git diff --check` is clean. No other candidate path changed.

## Decision

Do not ship this candidate as a proof-burden reducer. The three round-3 failures are repaired, and the new
table ratchet passes its own witnesses, but it proves semantics for bare spellings rather than for the Vars
those spellings resolve to in each file. A referred binding macro that collides with a table name reproduces
the prohibited false section-A certification.


> END RECEIPT (fence-run): worktree HEAD at review exit = 50bcd107919fbf1b3d5810f234f005df51a3bc7c = fenced sha.
