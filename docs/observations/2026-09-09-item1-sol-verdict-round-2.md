NO-GO

Sealed candidate reviewed: `79c94e6c425a084727d9e07d73ca0e62491aa640`
(`45beeb7eaff3b543a87468fcf5e2acc1b0b3089d` over
`3ea3803ea2e303a961a3edc1d76b8095ab5ff4f7`).

The four round-1 findings are repaired in their named corpus cases, and the installed
v3.7 bytes match the staged v3.7 bytes. The consumer nevertheless still has a false
green in its new Var-sidecar boundary, and its controlled `consume` path accepts
provenance that the builder explicitly says is incomplete. The frozen section 4/5
rule is that unknown authority-affecting evidence refuses; an incidental refusal on
today's self-reported production receipt does not make an incomplete observed-custody
receipt safe.

## Findings

### SOL-EC-005 — blocker: inline Var fields shadow the content-addressed sidecar

`resolve-vars` chooses the inline branch when either `:expected-vars` or
`:executed-vars` is present and never inspects a simultaneous `:vars-ref`. Starting
from the corpus's `controlled-complete.edn`, I added empty inline vectors beside each
suite execution's unchanged, valid `:vars-ref`. The installed consumer returned:

```text
DECISION consume discharged=R1,R2,R3,R4,R5,R6,R7,R8 out-of-band=R9,R10
```

Thus a receipt can retain a full content-addressed census as decorative bytes while
the matcher authorizes an empty inline census. The mutator itself warns that this
would “shadow the reference and quietly disable the check,” but the consumer does
not enforce mutual exclusion. This defeats SOL-EC-002/deviation 9 and is not among
the 72 corpus rows.

The four requested reference-integrity cases otherwise refuse: absent file, bad
digest and root escape pass their corpus rows; a separately derived bad `:bytes`
value returned `run-required reason=evidence-missing` with “byte length disagrees.”

### SOL-EC-006 — blocker: presence-only provenance still authorizes unknown inputs

Starting from the same controlled fixture, I retained every required key but replaced
the candidate `:inputs-manifest` and every execution's `:inputs-manifest` and
`:environment-manifest` with `{}`. The installed consumer again returned the same
`DECISION consume` line above. It checks presence, but never validates those values
against the independently derived inventory or a complete workspace/environment
boundary.

The controlled fixture also has no `:closure-basis` and no fixture/hook identities,
yet consumes. Those are the builder's still-unimplemented deviations 2, 3 and the
remaining part of 9. Today's genuine producer receipt is safely stopped earlier by
`custody-unverified`, but an observed-custody receipt can pass these unknowns
silently. That is not the per-obligation fail-closed direction required by section 4.

These are new findings, not bounded residue of SOL-EC-001..004. The implementation
that must change is the installed ship consumer outside this sealed repository patch,
so I did not apply a candidate fix or issue `GO-WITH-FIX`.

## Round-1 repair replay

The installed consumer corpus completed **72/72**. The relevant rows now say:

| Repair | Replayed result |
|---|---|
| SOL-EC-001 custody/provenance | Genuine producer `custody-unverified`; stripped provenance `provenance-missing`; incomplete proof `proof-pending` |
| SOL-EC-002 nested checks/Vars | Missing nested executions `nested-check-evidence-missing`; red child `obligation-red`; missing Var `scope-incomplete` |
| SOL-EC-003 rule bytes | R4 has 81 inputs and R5 has 50; weakening `test/mcp_operation_contract_oracle.pl` changed `policy-sha256` from `b3806b69…` to `27969109…` |
| SOL-EC-004 toolchain | An unobserved claimed tool gives `toolchain-unobserved`; `:sh` is `/usr/bin/dash` |

Installed/staged SHA-256 pairs were byte-identical for `gate-consume.clj`,
`gate-envelope.clj`, and `land`.

## Nine-deviation fail-closed audit

| # | Preserved? | Result |
|---|---|---|
| 1 custody remains self-reported | Yes | The real producer receipt refuses `custody-unverified`. |
| 2 partial input/environment boundary | **No** | SOL-EC-006: present empty manifests consume under observed custody. |
| 3 empty skips/focus without closure proof | **No** | SOL-EC-006: no `:closure-basis` is required; the controlled fixture consumes. |
| 4 R4 nested checks unavailable from producer | Yes | A real R4 execution without per-check evidence refuses `nested-check-evidence-missing`. |
| 5 historical per-obligation replay is counterfactual | Yes | Production candidate mismatch remains an earlier refusal. |
| 6 R9/R10 are slots only | Yes | Both remain explicitly out-of-band and never discharged by the gate receipt. |
| 7 no live consumption qualified | Yes today | Genuine receipts are self-reported and refuse; no saving is banked. |
| 8 nine Kaocha raw receipts/parser absent | Yes for this slice | Missing/unsupported evidence is not consumed. |
| 9 fixture/hook identity remains absent | **No** | Missing fixture/hook identity does not refuse, and SOL-EC-005 also bypasses the implemented Var census. |

The requested table is therefore not all-preserved.

## Other requested checks

- **One source of inventory:** the coordinator passes its live
  `gate-stage-manifest` and `suite-namespaces` to `gate-obligations/inventory`.
  R4 nested members are derived from the resolved `mcp-test-checks` recipe rather
  than a second literal list. The focused namespace ran 15 tests / 63 assertions,
  zero failures and errors.
- **Policy versus data:** the only `:data` input is
  `docs/observations/battery-ledger.edn`, because it is mutable evidence read by the
  freshness predicate. Its path and role remain policy; its current digest/length do
  not. Every other declared input is `:policy`.
- **Transport/toolchain:** the 23,831-byte v2 envelope parsed with both
  `clojure.edn/read-string` under Babashka and JVM Clojure. All nine tools have
  `:status :ok` and parsed versions; no launcher banner is a version.
- **Owner counters:** the `absent-failure-counter` row refused `lane-red`; absence
  was reported as `:absent`, not zero.
- **Makefile:** relative to the frozen base, exactly one target,
  `print-gate-obligations`, was added, plus its `.PHONY` registration and explanatory
  comment. No existing recipe changed.
- **Repair delta:** `1ed995c6..45beeb7e` changes only
  `battery_parallel_runner.clj`, `deftest_census.edn`, `gate_obligations.clj`,
  `gate_obligations_test.clj`, and `toolchain_identity.clj` (185 additions, 30
  deletions), all attributable to the four repairs and witnesses. `git diff --check`
  is clean.

`make test` was not run, as directed. The builder's retained seven-stage result was
not duplicated.
