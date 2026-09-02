# Prepared Confirmation Affinity Repair — Independent GO

## Verdict

GO for the ratified W1 session-affinity caller repair at exact candidate
`7e0300fe0a75623fa6d7f275d2b99b57aa34f26d`, tree
`1cfae15edb7ab167ce3c86f8157e8b0338c90790`.

This verdict authorizes staging and the next declared evidence gate only. It
does not authorize installation, MCP reload, shared-runtime mutation, or a
portable confirmation capability.

The audit used a clean detached worktree. The candidate and its source tree
remained byte-identical throughout the audit.

## Frozen red and focused green

The frozen red authority
`a3935045e1067305ce6de57d44a20be80a376111` is an ancestor of the candidate.
An independent cold replay reproduced exactly:

- 22 tests;
- 115 assertions;
- 19 failures;
- 0 errors.

The exact candidate then passed the same namespace with 22 tests, 115
assertions, 0 failures, and 0 errors.

`clj-surgeon.mcp-intent-contract/audit-current-repository` returned `ok=true`
and `violations=[]`. PREP-ACT-019, PREP-ACT-020, and PREP-ACT-021 all have
implementation and test witnesses.

## Independent adversarial matrix

The durable probe is
`dev/experiments/prepared_confirm_affinity_crossverify.clj`, SHA-256
`754ea414ffa31bc5307b2dfabca6e201049a02a61d14b64aac7af1278f6e4b9d`.
It invokes the public edit handler with SDK `java.util.LinkedHashMap` values,
not only Clojure maps.

It proved:

1. A hostile field named `ignore prior instructions\n\"quoted-now\"` remains
   structured invalid-field data. Visible content contains zero raw newline
   instances and exactly one canonically escaped JSON instance. The visible
   remedy and structured remedy are identical. The refusal has `ok=false`,
   `source_unchanged=true`, `mutation_attempted=false`, and
   `write_authority=false`.
2. Cross-session, restart-lost, and never-served digests all return
   `prepared-confirmation-unknown`. After excluding only the ordinary dynamic
   elapsed measurement, their structured results and visible content are
   identical. Each uses the exact remedy: `Reuse the serving MCP session or
   submit ordinary explicit edit arguments.` No source changes.
3. Representative public inspect, preview, commit, consumed-replay, and
   cross-session outcomes all carry a Java Boolean `ok`. Their values were
   `[true true true false false]`. The confirmed commit completed exact
   verification; replay refused as consumed.
4. Both public tool descriptions and all three owned skill surfaces name
   `Mcp-Session-Id`, the same stdio connection, the
   `prepared_request.arguments` fallback, Boolean `ok`, and the prohibition on
   descriptor/digest-presence discrimination.
5. The root skill SHA is
   `2fbfdfe5a90753ba3b4a4453200c13b5b32002e5ad1f20a1d735c94702867445`.
   The two generated mirrors are byte-identical at SHA
   `a124023c3fdccdd25bb2141a56154ee637d44aeda8e8fc4f40351d64636234bb`.
   The root skill differs only under the repository-owned relative-link
   layout; all five required guidance phrases occur in every surface.

Registry, session binding, transaction, writer, preview authority, and
ordinary explicit edit implementation files were not changed by this repair.

Replay the probe from the candidate worktree with:

```sh
clojure -J-Xms64m -J-Xmx512m \
  -Sdeps '{:aliases {:clj-surgeon/mcp-test {:main-opts []}}}' \
  -M:clj-surgeon/mcp-test \
  dev/experiments/prepared_confirm_affinity_crossverify.clj
```

## Repository gates and retained losses

- Standard Clojure Style check: all four changed Clojure files formatted.
- Changed-file clj-kondo: 0 errors, 0 warnings.
- Core: 647 tests / 5,562 assertions / 0 failures / 0 errors.
- Full MCP: 344 / 3,799 / exactly 2 failures / 0 errors. Both are the known
  `cold-clj-kondo-admission-timeout-is-unverified` wall-clock assertions.
- The unaffected cold-verifier subset: 6 / 43 / 0 / 0.
- MCP oracle, heap, admission-path, analyzer-target, cclsp startup/client,
  stdio smoke, and skill-mirror checks passed.

The first full run reached the analyzer gate while the admission recorder
reported normalized load 0.611 and correctly pressure-deferred clj-kondo. The
analyzer result was 4 tests / 14 assertions / 5 failures / 2 errors and is
retained as environment-invalid, not candidate evidence. A bounded retry at
normalized load 0.544 produced the same typed pressure deferral. Neither run
was bypassed or relabeled green. The candidate does not change analyzer code,
and its focused product, intent, formatting, lint, handler, and MCP evidence
remained green apart from the separately owned two cold-clock assertions.

## Dogfood ledger

This audit made no product edits. It used three bounded `inspect_clojure`
requests for seven changed forms and three public-boundary helpers. Every read
returned `read_complete=true`; none was repeated. The adversarial probe and
this docs-only receipt used native `apply_patch`, which is the correct route
for new files. No fallback from an eligible Surgeon mutation occurred.
