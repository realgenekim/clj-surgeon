# GO-WITH-FIX

**Finding 11 remains OPEN** on candidate
`ce162604644337055aa674bdf38edede7aee9e8f`.

- **CLOSED:** refusal now requires and pins `committed=false`.
- **CLOSED:** every terminal face requires and pins `source_file=1`.
- **CLOSED:** every terminal face requires exactly one of `details_path` and
  `details_unavailable`; both and neither are rejected.
- **PARTLY CLOSED:** `{}` is rejected for every declared `verification`,
  `closure`, `partition`, and `planned_partition`. Omitting the three typed
  check counts is correctly allowed: MCP-OP-HELPER-022 permits only checks the
  profile actually reported. But the schema requires
  `verification.{profile,ok,fresh_process}` on every terminal face while the
  production throw path calls `finish-failure!` with `proof=nil`; the production
  `terminal-receipt` then emits exactly `verification={status:"unknown"}`. That
  honest production-mapped `verification-failed` face is rejected.

The nested shapes also constrain key presence, not value types: Draft 2020-12
still accepts wrong-typed values for all four named objects. In particular,
`closure.grammar` is always emitted by production as
`"supported-libspecs-only"` but is not pinned (only
`closure.dynamic_references` is). The partition and planned-partition members
are production non-negative integer counts but the nested schemas do not say
so; likewise the four executed-verification members have no nested type
constraints.

Python-jsonschema 4.19.2's Draft 2020-12 validator accepted all five ordinary
production-derived faces against exactly one branch, rejected every named
round-5 counterexample, accepted removal of the optional typed check counts,
and reproduced both residuals: the no-proof production mapper face is invalid,
while the wrong-typed nested objects and contradictory closure grammar remain
valid.

Probe completed `2026-09-05T09:46:26Z` under
`/var/tmp/forge/helper-fence-fx`, pinned with `taskset -c 6-9 nice -n 10`.
Exactly two JVMs were attempted (one interrupted slow validator run and one
reviewer-script parse refusal); the completed validation used the same real
Draft 2020-12 Python validator as round 5. No MCP server or prohibited port was
used.

**Overall boundary landing verdict: GO-WITH-FIX.** Admit an honest unknown-proof
verification alternative (or make the mapper always emit real evidence without
fabrication), and type/pin the remaining production object invariants.
