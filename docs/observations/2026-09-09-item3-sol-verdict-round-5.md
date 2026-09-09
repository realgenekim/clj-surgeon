NO-GO

# Sol fence review, round 5: preservation brief

- reviewed: sealed candidate `45fad0cd0f8b8d202e5919b64a1e11ae0e33737f`
- branch tip accepted by the brief: `83c138143738557b317827a65982c65040bdc032`
- base: `3ea3803ea2e303a961a3edc1d76b8095ab5ff4f7`
- prior verdict: round 4 `NO-GO` on PB-FENCE-009
- verification: table ratchet; full 14-row real-tree replay; the four permanent collision pairs; independently planted own-namespace/core-shadow, `:refer :all`, and absent-alias resolution pairs; independence, section-B prominence, real-tree provenance, preregistration, and exact candidate delta
- excluded as directed: `make test`

## Blocking finding

### PB-FENCE-010 — `:refer :all` is not resolved, so a colliding binding macro still receives table semantics

Round 5 correctly keys its semantic tables by fully qualified Var, but the namespace scanner does not
produce the fully qualified Var for `:refer :all`. `refer-map` at
`bin/preservation_scan.clj:267-273` records only `:refer` values whose spelling starts with `[`. For
`[some.ns :refer :all]` it records no referred symbols. `head-info` therefore reaches its bare-name fallback
at `bin/preservation-brief:391-397` and treats `testing` as the modelled
`clojure.test/testing`, even when Clojure resolves it to a different Var imported by `:refer :all`.

I planted a pair on copies of the real Cell C trees. Both trees add this binding macro to the already-used
`cfp-scheduler-killer.committees` namespace and require that namespace with `:refer :all`:

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

Both heads resolve to `cfp-scheduler-killer.committees/testing`. The base expression returns the macro's
local `header`; the candidate ignores that binding and names the relocated Var. They are semantically
different, but the brief reports `committee-page` as `:identical-modulo-requalification`, leaves the
preserved count at **102**, and does not name `testing` as unmodelled. This is the same-token-stream-after-
canonicalisation falsifier and fires the section 9.8(6) kill switch.

Witness trees and retained output:

- base: `d54ac1e4b16a89c72766b6b173a4102bd491a922`
- candidate: `0bd45d5813ada2507cb5deacfabacb361060e6f8`
- brief: `/var/tmp/forge/item3/sol-round5-replay/out/refer-all-shadow-full.md`
- machine result: `/var/tmp/forge/item3/sol-round5-replay/out/refer-all-shadow-full.json`

Required repair: model `:refer :all` from definitions in the addressed tree, or conservatively make bare
heads unmodelled whenever a `:refer :all` can supply them. Add a generated collision control and a permanent
real-tree pair row for this syntax. The current generated R5 witnesses exercise only an already-expanded
explicit refer map (`bin/preservation-tables-test:203-233`), so all 276 can pass without testing namespace
parsing.

The repair necessarily changes `bin/`, which the ship-fix contract marks `HOLD reason=oracle-changed`; I did
not patch the oracle and cannot answer `GO-WITH-FIX`.

## Requested probes and controls

- **Four repaired collision rows:** `are-binding`, `uppercase-alias`, `kind-collision`, and
  `referred-testing` all move `committee-page` out of the preserved set. Each pair reports 101 preserved
  bodies versus the clean row's 102 and changes the relevant tier from
  `:identical-modulo-requalification` to `:changed`.
- **Own namespace shadows core:** an independently planted, valid own-namespace binding macro named `doto`
  (with `:refer-clojure :exclude [doto]`) resolves to the definition in each tree, not
  `clojure.core/doto`; `committee-page` is `:changed`. Base `0be833cb9b46620c88ff433180192779a9e7df8a`,
  candidate `77ba79086fcbb3c5cb583339897ed336ecfe0d4a`, output
  `/var/tmp/forge/item3/sol-round5-replay/out/own-doto-shadow.{md,json}`.
- **`:refer :all`:** fails as PB-FENCE-010 above.
- **Alias to an absent namespace:** `[absent.ns :as ghost]` plus `ghost/testing` is conservatively
  unmodelled; `committee-page` is `:changed` and the preserved count is 101. Base
  `fda477b6b2f010319087c062facacc899e2a5035`, candidate
  `977f00b299fa14d4444b00b9cfe4883b0b17c9d5`, output
  `/var/tmp/forge/item3/sol-round5-replay/out/absent-alias.{md,json}`.

## Other required checks

- **Table ratchet:** `bin/preservation-tables-test` exits 0: 202 function Vars, 74 modelled/frozen Vars,
  37 witnessed claims, 45 executed scope witnesses, 276 generated shadow rows plus controls, and seven
  required historic replay rows.
- **Full replay:** all 14 rows complete. Clean Cell C is 141/141 relocated, 102 relocated bodies preserved,
  and 32/64 caller bodies preserved. All 13 planted defects are surfaced, including the four permanent
  collision pairs.
- **Section-B-only defects:** promotion and require-order specimens exit 3 and are hoisted above A beneath
  `HARD STOP`, `Do not consume`, and “A signal that lives in section B is not a weaker signal.” This is loud
  enough; they should remain decision failures rather than be relabelled as body-identity failures.
- **Independence:** none of the three implementation files requires `clj-surgeon.*`. The only occurrence is
  a scanner comment. A/B facts are computed before `read-receipt` is invoked at
  `bin/preservation-brief:1058`; receipt data feeds section C disagreement, not A/B derivation.
- **Real-tree provenance:** `bin/preservation-replay` pins Curtaincall CFP `d9205abc` and `65ad613b` and
  builds its scratch repository with `git archive` of those trees. It is not a synthetic seed.
- **Preregistration:** section 9 says no arm has run, closes 21 specimens × 3 arms = 63 observations against
  9 reviewers × 7, guarantees two specimens per protected class per arm, expressly refuses to estimate a
  catch rate at n=2, and preregisters strict dominance instead. This is honest.
- **Exact round-5 delta:** `77126c8d..83c13814` changes only `bin/preservation-brief` (+199/-148),
  `bin/preservation-replay` (+13/-4), and `bin/preservation-tables-test` (+93/-72). `git diff --check` is
  clean. No other candidate path changed.

## Decision

Do not ship this candidate as a proof-burden reducer. The explicit-refer failure from round 4 is repaired,
and the other requested resolution controls behave conservatively, but `:refer :all` bypasses that repair
and reproduces the prohibited false section-A certification.


> END RECEIPT (fence-run): worktree HEAD at review exit = 45fad0cd0f8b8d202e5919b64a1e11ae0e33737f = fenced sha.
