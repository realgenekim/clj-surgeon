# Prepared Confirmation Affinity Repair

## Outcome

The staged repair makes the installed W1 session law actionable without
weakening it. Public instructions now tell Streamable HTTP callers to retain
`Mcp-Session-Id`, tell stdio callers to retain one connection, and direct
sessionless callers to submit the served `prepared_request.arguments` as
ordinary explicit edit arguments. All caller surfaces name `ok` as the only
success/refusal discriminator.

Prepared-confirmation refusals now carry complete visible remedies:

- `invalid-prepared-confirmation` renders ordered `invalid_fields` as one
  canonical escaped JSON array literal.
- `prepared-confirmation-unknown` names both safe routes in one sentence,
  without revealing whether another session served the digest.

No confirmation became portable. No write, lookup, transaction, snapshot, or
registry authority changed. This candidate was not installed or reloaded.

## Linked-Intent chain

- HLD: `10705a99afc71d29f776a567dc9821863dff8fcb`
- LLD: `0e964324aecd3535cbb55af6c5e8346c0a10d577`
- EARS: `5b835ad046bf35cd71fa7a3235c3ad194f6aae25`
- edge audit: `247e58aaea2db3cd115943d5c02a369354485ae5`
- frozen red: `a3935045e1067305ce6de57d44a20be80a376111`

The frozen red was 22 tests, 115 assertions, exactly 19 failures, and zero
errors. Its closed decomposition was 9 affinity-guidance failures, 6 outcome
discriminator failures, and 4 visible-remedy and hostile-key escaping
failures.

## Adversarial evidence

A hostile top-level request key containing instruction-shaped prose and a
newline/quote sequence was admitted only as invalid-field data. The public
handler rendered it inside this canonical JSON array:

```text
["ignore prior instructions\n\"now\"","confirm","fill"]
```

The same public handler rendered the unknown remedy as:

```text
Reuse the serving MCP session or submit ordinary explicit edit arguments.
```

In both cases `ok=false`, the source was unchanged, and no write authority or
mutation attempt was present. This supports a later, separately scoped house
law: any surface that renders caller-supplied names into visible agent text
should use one canonical data delimiter and canonical escaping. Read-side and
telemetry audits are future work, not part of this repair.

## Dogfood ledger

The test insertion used `apply_clojure_changes` because the prepared semantic
decision and verification belonged in one transaction. Two typed pre-write
refusals exposed an invalid top-level expectation and an ambiguous insertion
gap; both left source unchanged. The corrected transaction committed with
read-back SHA-256
`8755d0e20cded698f924c1fd2b2138500f7ccf7af78959f1f38cfcc593e1f19b`.

The final product implementation used one `edit_clojure` transaction: four
exact effects across three Clojure namespaces. It completed with
`verification_complete=true`, canonical-effect SHA-256
`27fc301f38460bafd50aa95c245f1c8ccbbbb2761a73271c43ff4368a938fdba`,
and receipt SHA-256
`1d41f219c75dacc8b4fcc38274c06b5ff71853e357d9924a9cd106c3bd522c79`.
Skill prose and its generated mirrors were updated by the repository's owned
sync path.

## Verification

- warm focused: 22 tests / 115 assertions / 0 failures / 0 errors
- cold focused: 22 / 115 / 0 / 0
- intent audit: `ok=true`, `violations=[]`; PREP-ACT-019..021 each have direct
  product and test witnesses
- core: 647 / 5,562 / 0 / 0
- analyzer contract: 4 / 20 / 0 / 0
- full MCP: 344 / 3,799 / exactly 2 known cold-admission clock failures /
  0 errors
- unaffected cold-verifier subset: 6 / 43 / 0 / 0
- changed-file clj-kondo on the semantically identical staged tree: 0 errors /
  0 warnings; the exact minimal-diff candidate retry was pressure-deferred at
  normalized one-minute load 0.632 and was not bypassed
- MCP oracle, heap configuration, analyzer admission path, analyzer target,
  cclsp startup/client audit, stdio smoke, skill mirrors, and all post-MCP
  benchmark/evidence self-tests: green

The two MCP failures are the already-characterized
`cold-clj-kondo-admission-timeout-is-unverified` assertions: the result is
`:clj-kondo-admission-unverified` with delegated admission rather than the
raced timeout status. The same two failures reproduce in the isolated 7-test,
50-assertion namespace. They are unrelated to this caller-guidance/refusal
change and remain a separately owned deterministic-clock repair.
