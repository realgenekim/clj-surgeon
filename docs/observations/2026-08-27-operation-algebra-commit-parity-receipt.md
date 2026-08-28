# Operation-Algebra Commit Parity Receipt

Date: 2026-08-27

## Decision

Accept the commit-route compatibility seam at candidate
`b05b3a03afb7e40020192444777a5a1c20b91a69` for integration review.

The no-model witness found no public-result, canonical-domain, receipt, or
pure-undo compatibility difference from pre-cutover commit `91b2190` in the
bounded matrix. The witness does not install, reload, launch an analyzer, or
touch a shared port.

## Safety Shape

The comparison does not run the old and new commit engines against live source
bytes.

1. The pre-cutover CLI route, candidate CLI route, and candidate MCP route run
   against identical source and request data with fake commit and receipt
   boundaries.
2. The fake comparison records zero authoritative source commits and zero live
   receipts.
3. The candidate CLI route then performs one authoritative commit in an
   isolated temporary workspace and publishes one receipt.
4. The candidate live result and receipt are compared with the pre-cutover fake
   baseline.
5. Each version validates and compiles the other version's receipt into an
   inverse without applying that inverse.

The receipt-publication failure case calls the fake commit boundary twice: once
for the forward result and once for rollback. Both calls are deliberately fake;
the case performs zero authoritative commits.

## Matrix

All five cases passed for both candidate entrances.

| Case | Pre vs candidate CLI | Pre vs candidate MCP | CLI vs MCP domain | Live effect |
|---|---:|---:|---:|---:|
| Success | exact | exact | exact | one candidate CLI commit |
| Compile refusal | exact | exact | exact | none |
| Stale source | exact | exact | exact | none |
| Restored write failure | exact | exact | exact | none |
| Receipt-publication failure | exact | exact | exact | none |

For every fake case, source bytes remained unchanged and no receipt file was
published. Candidate CLI and MCP produced the same canonical outcome. Their
legacy result maps remained exactly equal to the pre-cutover result map.

## Exact Evidence From the Retained Run

- Harness report hash:
  `cf226f4010499b6bc7d898449219a06521bd9f4b22df0dccfb0c09f8781e5299`
- Exact receipt-source SHA-256, identical in pre-cutover fake, candidate CLI
  fake, candidate MCP fake, and candidate live routes:
  `746423b8bba9a636e23c5ca880ca9324fd20fa64a3de385c4d1690dc530cdeb2`
- Receipt data hash:
  `e9ded8223456f24c775b3bdba1a101d885ce9137d20a21dbd8649c3180f42180`
- Cross-version inverse facts hash, identical in both directions:
  `ec356c93cc66c92ce0713766e4cdf815453efcd9d80b9597164c716edbf8b6e7`
- Live candidate legacy-result hash, equal to the pre-cutover success result:
  `93755843390827819d040a8796171305d64e6786e6ac4f7204b00c561369d363`

Scenario result and domain hashes:

| Case | Legacy result | Candidate domain |
|---|---|---|
| Success | `93755843390827819d040a8796171305d64e6786e6ac4f7204b00c561369d363` | `c53f6285247bda454b34aea01c4c3ebfd990fc4151cac231b92a6e1a2e5c4213` |
| Compile refusal | `006bf3887f968f196061d9adb165c54b21a9a142cbf17b5e6805bb392849e501` | `a6d93ba81eea56dc552b04f327746cfa0514ecc93a11c598a5169885af008036` |
| Stale source | `f88bfa7812a95e5552ea2b6398df00ac3e2b88d132d18923f720a463b796ed35` | `65c6f571bc4ed8a91dfc7b5408007ff997be72f571b91c8b618134c0131dfd0a` |
| Restored write failure | `94cb7e2017ce0037d4eda1ac182766f4aba859eaf027b1fede42c2c90fad2067` | `ea406ff99d8d5996774c8737551a85d5e1d3adf0fe41341d5d734aba5d60ac79` |
| Receipt-publication failure | `901fa840acb35310555b6d6b2b6cef0aa2cd446c88736b0ce4f0c1ccf05c8a6f` | `276354077ca09767e25a9c3eb317d1d7590a787039a5f023613405f321ff18b2` |

## Verification

The retained test command is:

```text
bb -cp src:test:dev/experiments \
  dev/experiments/operation_algebra_commit_parity_test.clj
```

Result: 2 tests, 21 assertions, 0 failures, 0 errors.

The standalone report command is:

```text
bb -cp src:test dev/experiments/operation_algebra_commit_parity.clj
```

It materializes both exact Git commits with `git archive`, uses the same
external worker source in each archive, and deletes the temporary comparison
tree after completion.

## Limits

- This is a commit and receipt compatibility witness, not a performance result.
- It does not prove formatter, verifier, extraction, or program behavior.
- It proves candidate CLI and MCP projections through the shared transaction
  route but performs the single authoritative live commit through CLI only.
- The isolated receipt hash contains the temporary canonical file path. A later
  run will therefore produce a different receipt hash while preserving the
  within-run exact-equality gate.
- Shared MCP publication and live callback behavior remain separate gates.

## Recommendation

Cherry-pick the harness, its test, and this receipt. Use it as the
`OP-ALG-PARITY-002`, `OP-ALG-RECEIPT-001`, `OP-ALG-RECEIPT-002`, and
`OP-ALG-SHADOW-001` compatibility witness. Do not treat it as evidence that the
full MCP cutover or release publication is complete.
