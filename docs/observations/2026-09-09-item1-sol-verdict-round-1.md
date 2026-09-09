NO-GO

Sealed candidate reviewed: `801b00cfabf0f9e5c198388e1408ca2e3180b0e8`
(`1ed995c63dacacffd95156e6f0c27914206119b6` over
`3ea3803ea2e303a961a3edc1d76b8095ab5ff4f7`).

The inventory/receipt work closes useful defects, but the installed v3.7 consumer
can still return `consume` while normative authority and execution evidence are
unknown. That is the opposite of the frozen section 4/5 fail-closed contract and
blocks landing.

## Findings

### SOL-EC-001 — blocker: self-reported custody and absent provenance consume

The controlled green envelope says `[:producer :custody] :self-reported`.
Nevertheless, `gate-consume.clj` returned:

```text
DECISION consume discharged=R1,R2,R3,R4,R5,R6,R7,R8 out-of-band=R9,R10
```

I then derived a fixture from those bytes that removed both
`:candidate/:inputs-manifest` and `:candidate/:workspace-snapshot-sha256`, and
removed every execution's `:inputs-manifest`, `:environment-manifest`,
`:declared-skips`, `:focus-omissions`, and `:unexecuted-tests`. It returned the
same `consume` decision.

This is not merely a less detailed receipt. Section 4 says only qualified
custody/provenance can discharge an obligation and unknown influence requires
the complete check. Section 5 says missing scope/basis/focus fields are unknown,
not empty. The matcher checks non-empty omission vectors with `seq`, but does
not require the fields or validate custody, input manifests, environment
manifests, workspace snapshot, or `:proof :complete?`. The envelope writer
explicitly emits `:custody :self-reported`; its problem list does not include
that condition. The builder's deviation 7 (no live consumption qualified) is
also no longer a safe boundary now that v3.7 is installed and this receipt can
skip the landing battery.

### SOL-EC-002 — blocker: nested checks and Var coverage can pass silently

R4's fourteen `mcp-test-checks` members are a second hand-written list in
`gate_obligations.clj`, not identities derived from the recipe the coordinator
executes. The receipt records one R4 exit and namespace identities. The consumer
does not require per-check executions, loaded/executed Var identities, fixture or
hook identity, or evidence references. Therefore a check skipped inside a
passing recipe, or a required Var silently not executed while its namespace is
reported, can still discharge R4/R8. These are exactly deviations 4 and 9 from
the builder report; they do not preserve the required fail-closed direction.

The top-level seven-stage inventory and suite namespace selections do share the
coordinator's `gate-stage-manifest` and `suite-namespaces`, which is good, but it
does not cure the second nested-check list or the absent per-check/per-Var
evidence.

### SOL-EC-003 — blocker: the policy hash omits rule bytes

The ledger-digest repair itself is correct. The only declared `:data` input is
`docs/observations/battery-ledger.edn`: it is mutable evidence read by R2's
freshness rule, so its path and role belong in the policy projection while its
current bytes do not. Every other declared input is correctly labelled
`:policy`.

But the declared set is incomplete, so `:policy-sha256` still does not cover the
rules. For example, R4 includes only the Makefile recipe text, lane manifest, and
`deps.edn`; it omits the bytes of the Prolog oracle, four Python oracle files,
and the eight shell/self-test implementations invoked by `mcp-test-checks`.
Selected test implementation/Var contracts are likewise represented by names,
not rule digests. The consumer also does not compare the receipt's source digest
or input reference. A candidate can therefore weaken a rule, produce a matching
current-candidate inventory/receipt, and have the weakened evidence consumed.
There is no frozen-base-policy/approved-intent comparison required by section 4.

The broader missing worktree/index, relevant untracked/ignored inputs, external
inputs, and structured environment from deviation 2 is also accepted rather
than converted to `run-required` (SOL-EC-001).

### SOL-EC-004 — blocker: independent toolchain observation is incomplete

Java's launcher banner is no longer promoted to a version, and banner-only
producer output is `:unknown`. However, the producer receipt has nine tool keys,
including `:sh`, while land's observed toolchain has only eight and omits `:sh`.
`check-toolchain` compares a tool only when the observed map already contains
that key, so the absent independent observation passes. The producer also
records shell `:version "sh"` from the first line and discards the resolved
executable path on the second line. Thus the claimed complete independent
toolchain comparison is not fail-closed.

## Nine reported deviations: fail-closed audit

| # | Preserved? | Finding |
|---|---|---|
| 1 custody remains self-reported | No | SOL-EC-001: it still consumes. |
| 2 partial input/environment boundary | No | SOL-EC-001/003: missing provenance still consumes. |
| 3 skips/focus/closure emitted empty | No | SOL-EC-001: absence is treated like empty; closure basis is not required. |
| 4 R4 nested checks lack executions | No | SOL-EC-002: one passing R4 exit can discharge them. |
| 5 historical per-obligation replay is counterfactual | Yes | Production rejects candidate mismatch before matching. |
| 6 R9/R10 are slots only | Yes | They remain explicitly `NOT discharged` and are not claimed by the gate receipt. |
| 7 no live consumption qualified | No | v3.7 is installed and consumes despite item 1 custody. |
| 8 nine Kaocha raw receipts/parser missing | Yes for this slice | Unsupported/missing evidence is not consumed; the feature remains unimplemented. |
| 9 no per-Var obligation matching | No | SOL-EC-002: namespace-only matching can pass silently. |

## Requested checks

- Inventory origin: the seven stages and suite namespaces use the coordinator's
  live Vars; R4 nested members do not, so the “no second list” check fails.
- Policy/data split: the ledger is correctly the sole declared data input and
  all declared rule inputs are policy, but omitted rule bytes make the overall
  policy coverage fail.
- EDN/toolchains: the printed inventory and green v2 envelope parsed with both
  Babashka `clojure.edn/read-string` and JVM Clojure
  `clojure.edn/read-string`. Java versions are not banners. Independent
  toolchain completeness still fails under SOL-EC-004.
- Missing owner counter: removing R4 `:failures` produced
  `DECISION run-required reason=lane-red obligation=R4 -- :R4 failures=:absent`.
  This requested behavior passes.
- Makefile: exactly one recipe target, `print-gate-obligations`, was added.
  The other Makefile bytes are its `.PHONY` registration and explanatory
  comment. No other target recipe changed.

## Verification

- `clj-surgeon.gate-obligations-test`: 12 tests / 40 assertions, zero failures
  and errors.
- `git diff --check 3ea3803e..HEAD`: clean.
- Retained v3.7 corpus log: 48/48 expected decisions. Its positive row is also
  the false green in SOL-EC-001 because the corpus expects self-reported custody
  to consume and contains no missing-provenance/scope-presence negative.
- `make test` was not run, as directed.

No candidate implementation fix was applied. Correcting the blockers requires
changing and requalifying the installed consumer/envelope protocol (and the
inventory contract), including behavior outside this sealed repository patch;
that is not a bounded small repair appropriate for automatic ship closure.


> END RECEIPT (fence-run): worktree HEAD at review exit = 801b00cfabf0f9e5c198388e1408ca2e3180b0e8 = fenced sha.
