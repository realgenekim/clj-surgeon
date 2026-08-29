# Captain's Log: Retained Relations Cleared the Number, Not the Gate

Date: 2026-08-29

Bead: `clj-surgeon-45j`

Audit head: `e0596c9`

Scope: retained evidence only. No model, product, Anvil, install, reload, or
shared runtime was used.

## Question

The repaired relation HLD requires the relation arm to lower both request
emission time (`T_emit`) and complete verified wall (`T_verified`) by at least
20 percent, with the relation arm winning both counterbalanced blocks. Can the
retained evidence decide `REL-GO` versus `REL-HOLD` before LLD approval?

## Evidence census

Two immutable archives appeared relevant.

### Closed edit-field aliases

Archive:
`clj-surgeon-field-alias-ce05-20260829T071022Z.tar.gz`

SHA-256:
`e41da53fd2b973d3545f4608365416d40c20e24a8f865edccec161699563972f`

This is valid evidence for the closed alias vocabulary, but it is not a
relation arm. Both successful candidate callers emitted the same complete
6.3--6.5-kilobyte canonical `from/to` request. Neither used relation lowering
or an alias spelling. The archive therefore cannot test whether
`symbol_migration + require_change` lowers `T_emit`.

### Flat, grouped, and closed-relation capture cohort

Archive: `6328db5-cohort-20260829T0851Z.tar.gz`

SHA-256:
`1af9110d6bbdbe369cdcdf7feee0f70bac78b0f25717a24d937dfe603ecc9d2c`

Candidate: `6328db51557bc39ef1a0d40ca171a1ac9873005a`

The retained order was `F A B B A F`. Projecting only normalized-flat `F` and
closed-relation `B` produces the symmetric order `F B B F`. The observer clock
records the complete turn-start-to-tool-call interval used by the current
`T_emit` definition.

| Positional pair | Flat `T_emit` | Relation `T_emit` | Reduction |
|---|---:|---:|---:|
| first | 60.775 s | 51.518 s | 15.2% |
| second | 70.907 s | 46.307 s | 34.7% |
| midpoint | 65.841 s | 48.912 s | **25.7%** |

The capture-only complete walls show the same direction:

| Positional pair | Flat wall | Relation wall | Reduction |
|---|---:|---:|---:|
| first | 63.000 s | 54.000 s | 14.3% |
| second | 74.000 s | 49.000 s | 33.8% |
| midpoint | 68.500 s | 51.500 s | **24.8%** |

The retained numbers therefore clear the new 20-percent pooled `T_emit`
threshold and favor the relation in both positional pairs. The mechanism is
numerically plausible.

## Why the cohort does not pass today's gate

The archive predates the causal laws that now govern promotion:

1. It is capture-only. No source mutation, exact verifier, rollback boundary,
   receipt, or `T_verified` clock exists.
2. Each arm advertised a different experimental schema and description. The
   current design requires both arms to see one identical enlarged production
   surface, so schema-ingestion cost is charged equally.
3. The experimental description replaced the production compact-editor text
   with generic change-tool prose that taught the wrong location spelling.
4. The historical scorer bypassed production compact-location normalization
   and falsely marked both flat calls incorrect. A later product-equivalent
   replay established that they were exact, but that repair is not evidence
   emitted by the original timed process boundary.
5. The candidate used an experimental pure expansion path, not the proposed
   product facade followed by the one existing transaction engine.
6. The retained event clocks show an `agent_message` before the MCP call. The
   current protocol requires `edit_clojure` to be the first emitted item with
   no preamble or earlier action.
7. The six-arm schedule and run identities were not frozen under the later
   real-mutation `N R R N / R N N R` protocol.

Silently retaining only the attractive timing rows would convert historical
reconnaissance into authority it never had.

## Decision

The retained evidence does not decide `REL-GO` versus `REL-HOLD`, because no
run is admissible for product promotion under the current gate. It does change
the risk assessment:

- the proposed `T_emit` effect is not invented after the fact;
- both positional comparisons favor the relation;
- the pooled 25.7-percent signal exceeds the newly declared 20-percent gate;
  and
- the still-unanswered question is whether the effect survives one identical
  production surface and real verified mutation.

Therefore retain `REL-GO` as the recommendation for **LLD only**. Do not claim
that the gate passed, do not authorize EARS/tests/code/model work from this
archive, and do not weaken the new gate. The LLD must preserve the exact
same-surface control and one-transaction laws so the later experiment can
answer the remaining question honestly.

If LLD cost grows beyond one pure facade plus existing adapters, stop and
choose `REL-HOLD`; the retained evidence does not justify a second engine.
