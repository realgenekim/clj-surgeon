NO-GO

Sealed candidate reviewed: `2d8f86a082129881a1cdcca6d77c711722e39505`
(`9dc25924fc3851af3fbf8767df0a1bed6cffe304` merged over
`3ea3803ea2e303a961a3edc1d76b8095ab5ff4f7`).

The named SOL-EC-005/006 absence and empty-value reproductions are repaired.
The installed consumer nevertheless still consumes authority values that are
present but false, and the generated ratchet does not enumerate those fields.
The frozen section 4 rule is value and identity binding, not field presence:
unknown or unverified authority evidence must require execution.

## Findings

### SOL-EC-007 — blocker: environment policy and manifest authority are not bound

The inventory places `:environment-policy` beside the obligations, but computes
`:policy-sha256` from `(volatile-free obs)` only. I redefined the environment
policy in memory from the six installed keys to `NEW_LOAD_BEARING_KEY` with a
different basis and regenerated the inventory. The result was:

```edn
{:same-policy-hash? true
 :old-environment-policy
 {:keys ["JAVA_TOOL_OPTIONS" "CLJ_SURGEON_GATE_RUN_ID" "MAKELEVEL"
         "PATH" "SHELL" "LANG"]
  :basis "policy-selected keys, values digested"}
 :new-environment-policy
 {:keys ["NEW_LOAD_BEARING_KEY"] :basis "changed-rule"}}
```

Thus a candidate can shrink or otherwise change the environment rule without
moving the policy identity that is compared to the receipt. This is a rule, not
volatile data, so it must be covered by the policy hash.

The consumer also treats membership in `:environment-manifest/:selected` as
proof that a key is digested; it never validates the value. Starting from the
controlled complete fixture, I replaced each execution's first selected digest
with `nil`. The result remained:

```text
DECISION consume discharged=R1,R2,R3,R4,R5,R6,R7,R8 out-of-band=R9,R10
```

Two related content-address claims are likewise only shape-checked. Replacing
either `:candidate/:inputs-manifest/:sha256` or
`:obligations/:manifest-sha256` with 64 `f` characters also consumed. The
consumer neither reads and verifies the candidate reference nor compares the
obligation-manifest digest with the independently loaded inventory. These are
validly shaped invented digests, not byte binding.

The required-input repair itself works: replacing a required input's digest
with a different valid SHA-256 refused `inputs-mismatch`. The defect is the
remaining unvalidated environment and manifest authority around that repair.

### SOL-EC-008 — blocker: the field generator does not cover the consumer's authority surface

The generator invokes the same consumer's `--print-inert`, but then adds its own
global skip set and walks only selected nesting depths. It is therefore not
solely derived from the declaration the consumer enforces.

A concrete load-bearing field declared inert by that generator logic is
`:environment-manifest/:basis`: `skip-keys` hard-codes `:basis`, while
`environment-gap` requires it to equal the inventory's basis. Removing that
field directly refused `environment-incomplete`, but none of the reported 240
field paths attacks it.

More importantly, the walk stops at
`:environment-manifest/:selected`; it does not enumerate the selected keys or
their digest values. That is why the `nil` digest in SOL-EC-007 is absent from
the 845 mutations. The sweep's result is internally correct for the paths it
generated, but “every authority field” and the resulting schema-ratchet claim
are false.

### SOL-EC-009 — blocker: any nonempty fixture identity discharges the requirement

For R3/R4/R5 the inventory now names `:fixture-identities` as required, but it
does not derive the expected fixture/hook identities. The consumer checks only
`(seq (:fixture-identities e))`. Replacing every populated fixture census with
this unrelated singleton still consumed:

```edn
["not/a/real-fixture"]
```

```text
DECISION consume discharged=R1,R2,R3,R4,R5,R6,R7,R8 out-of-band=R9,R10
```

This preserves presence, not the section 4 requirement that test identities
include and match fixture/hook context. It leaves deviation 9 fail-open and is
also outside the generator's empty/absent mutation vocabulary.

These are new authority-binding findings, not bounded residue of
SOL-EC-005/006. The required repair is in the installed ship consumer and its
fixture generator, outside this sealed repository delta, so I did not leave a
candidate fix.

## Round-1 and round-2 replay

The installed/staged SHA-256 values were byte-identical for
`gate-consume.clj`, `gate-envelope.clj`, and `land`. The bounded v3.7 core
corpus completed 73 rows with zero mismatches, followed by the advertised
`845 generated from 240 fields, 845 refused, 0 CONSUMED` sweep.

Direct reproductions against the controlled fixture gave:

| Reproduction | Decision |
|---|---|
| Inline Var vectors beside `:vars-ref` | `vars-ambiguous` |
| Empty inline Var census for R3's two selected namespaces | `scope-incomplete` |
| Referenced expected count 159, sidecar count 158 | `evidence-missing` |
| Wrong valid digest for a required input | `inputs-mismatch` |
| One inventory-selected environment key unaccounted | `environment-incomplete` |
| Missing required closure basis | `legacy-scope-unknown` |
| Missing required fixture identities | `provenance-missing` |
| Missing failure counter | `provenance-missing`; absence was not read as zero |

The earlier malformed, custody, nested-check, Var-census, rule-input,
toolchain, stage-set, missing-counter, and proof-pending rows also remained
green in the core replay. `make test` was not run, as directed.

## Nine-deviation fail-closed audit

| # | Preserved? | Result |
|---|---|---|
| 1 custody remains self-reported | Yes | Real producer bytes refuse `custody-unverified`; observed custody requires a distinct named observer. |
| 2 partial input/environment boundary | **No** | SOL-EC-007: the environment rule is outside `policy-sha256`, selected values need not be digests, and untracked/ignored/external influence still has no completeness marker. An earlier custody refusal does not make this obligation fail-closed. |
| 3 empty skips/focus without closure proof | Yes for the named suite slice | R3/R4/R5 now name an exact closure basis; absence or mismatch refuses. Real producer bytes do not silently satisfy it. |
| 4 R4 nested checks unavailable from producer | Yes | Missing per-member executions refuses `nested-check-evidence-missing`; a red member refuses `obligation-red`. |
| 5 historical replay is counterfactual | Yes | Production matching still checks the current candidate/tree before discharge. |
| 6 R9/R10 are slots only | Yes | Both remain explicitly out-of-band and are never discharged by a gate receipt. |
| 7 no live consumption qualified | Yes today | Real self-reported receipts refuse and run the gate. No saving is banked. |
| 8 nine Kaocha raw receipts/parser absent | Yes for this slice | Unsupported or missing evidence is not consumed. |
| 9 fixture/hook identity remains absent | **No** | SOL-EC-009: any nonempty string vector satisfies the consumer, including a demonstrably unrelated identity. |

The requested table is therefore not all-preserved.

## Other requested checks

- **One inventory source:** confirmed. The coordinator passes its live
  `gate-stage-manifest` and `suite-namespaces` to `gate-obligations/inventory`;
  `runner-policy` resolves those same Vars for `print-gate-obligations`.
  The current target prints ten obligations and the expected seven ordered
  stages. R4 nested members remain derived from the transitive recipe.
- **Policy versus data:** the only `:data` input is
  `docs/observations/battery-ledger.edn`, because it is mutable evidence read by
  R2's freshness predicate. Its path and role remain in the policy projection;
  changing only its digest/length leaves the policy hash stable. Every other
  declared required input is `:policy`. SOL-EC-007 is the remaining rule/data
  separation failure: `environment-policy` is a rule but is omitted from the
  hash.
- **Transport/toolchain:** the v2 envelope parsed with both Babashka
  `clojure.edn/read-string` and JVM Clojure. It carries nine parsed versions,
  including Java `openjdk version "21.0.12" 2026-07-21` and shell
  `/usr/bin/dash`, with `:unknown []`; the JVM launcher banner was not used as
  the Java version.
- **Owner counters:** the missing-failure-counter reproduction refused
  `provenance-missing`. The owner-emitted suite executions carry explicit
  `:failures`, `:errors`, `:isolation-violations`, `:leaks`, and
  `:skipped-preconditions` values.
- **Makefile:** relative to the frozen base, exactly one target,
  `print-gate-obligations`, was added, plus its `.PHONY` registration and
  explanatory comment. No existing recipe changed.
- **Repair delta:** `45beeb7e..9dc25924` changes only
  `test/clj_surgeon/battery_parallel_runner.clj` and
  `test/clj_surgeon/gate_obligations.clj` (49 additions, 13 deletions).
  The additional R2 discharge policy, environment policy, suite leak predicate,
  R7 recipe-derived predicate basis, and suite evidence requirements are all
  part of the advertised repair/ratchet work. `git diff --check` is clean.


> END RECEIPT (fence-run): worktree HEAD at review exit = 2d8f86a082129881a1cdcca6d77c711722e39505 = fenced sha.
