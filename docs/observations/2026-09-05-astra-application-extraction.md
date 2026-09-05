# Astra: application-derived response extraction, bounded quality GO

2026-09-05. One existing public `apply_clojure_changes` transaction moved six
response helpers into a new namespace and rewrote 258 selected uses in 28 caller
files. The reviewed candidate passes the focused oracle: exactly 30 changed
files, all caller linkage and preservation checks, exact moved/retained owner
multiplicity and bodies, and 24 baseline-identical JVM helper cases. The 172
unrelated uses remain unchanged. Independent final audit returned bounded GO.

This was an isolated clone experiment. The original application and immutable
baseline remain unchanged. No deployment, original-application edit, or Git
commit resulted from this lane. This report contains no application source,
private request data, or model transcript prose.

## Authority and public result

The application baseline was committed HEAD
`00e8f0fae2c19258f0eab008e6b02caa8545591d`. The exact 37,300-byte payload
was mechanically generated from retained structural census evidence and frozen
as SHA256 `ba9a205276e45d7c1cbe0b702d124d982a281535bb50099e4e2ea49ada8e3636`.
Immediately before writing, regeneration checked selected-source and all caller
hashes, reproduced those payload bytes, and checked full baseline/candidate
inventory equality. The actual current public schema accepted the request.

The server was independently attested at clean trunk
`b8249bc57e2bddc84bc8ec81dc320305d4fce596`, listener PID2164553,
birth201458286, port8171, cores6–9, 512MB heap. The retained process/checkout/
health evidence is historical provenance; health itself does not embed a build
hash. Attestation receipt SHA256:
`1bdc6e46219825fabb34855b6f36af40b79b2003280a0cc3c8c468daadb72a66`.

The single public response reports committed bytes, 30 files, 292 edits,
verification_complete=true and next_action=none. Its caller proof explicitly
says structural-candidates-only. The independent oracle supplies the narrower
application-derived preservation and helper-behavior evidence; neither result
proves all possible dynamic callers.

## Original oracle failure remains a failure

The first candidate oracle failed `target-header-no-extra-clauses`, despite
passing all caller checks and all 24 behavior cases. Public inspection showed
the target copied the original namespace docstring, already present in the
prepared plan. The oracle incorrectly assumed its rewrite-clj tag would be
`:string`/`:multi-line`; a faithful literal probe observed `:token`.

The independently reviewed amendment compares non-require namespace extras
exactly against the original, preserving order and source spelling. Named
witnesses accept the copied docstring and reject changed, missing or duplicate
docstrings and an added clause. The amended synthetic gate passes 3 positive
and 15 negative witnesses, plus 2 Python checks. Prior code, freeze, baseline
and failed result remain preserved. A fresh baseline was executed under the
amended freeze, then the SAME candidate passed oracle2. There was no public
reapply, product repair, or retroactive relabeling of oracle1.

Five actual positive-derived negative copies were then checked: duplicate
retained definition, duplicate moved definition, one selected caller left old,
one unrelated retained call moved, and one moved helper body changed. All five
exited1 with their intended failed predicate while still satisfying the exact
30-file footprint. Independent audit verified the exact single perturbation,
hash, and failure for each. Bulk copies were cleaned; exact perturbed source
and raw receipts remain. Positive and baseline bytes were unchanged.

## Timing and remaining prize

The earlier preparation ledger contains 22 MCP reads totaling 9.039 seconds of
call wall, plus 0.344 seconds initialization. Their start timestamps span
00:53:56Z–01:49:18Z: an interrupted preparation envelope, not active task wall.
It excludes reasoning, shell work, cloning, oracle construction and review.

The public write took 9.286 seconds HTTP call wall. The amended candidate oracle
took 6.563 seconds separately; its fresh baseline took 2.035 seconds. Original
failed verification and the oracle correction remain additional real work.
These are separate scopes, not a complete-task speed measurement. No native
competitor ran here and no speed ratio is claimed. A credible native competitor
would batch a six-name mapping script, not manually edit 186 owners.

The next API target is the 85 prepared caller changes: 28 namespace decisions
and 57 grouped symbol changes. The request already expresses the six-helper
goal, but its caller closure was supplied by the client. Mechanically deriving
that closure, preserving the 20 mixed callers and 8 response-only callers, is
the remaining bookkeeping opportunity. This result does not establish the
speed or generality of an unimplemented multi-helper closure API.

All changed files parse and the pinned caller preservation checks pass. The
24 cases load selected helper namespaces, not all 28 caller namespaces or the
full application. Broader exports-test discovery found mail-disabled/private
JSONL fixtures, but cached test-classpath binding and require-time side effects
were not fully established; those tests were not run. No full-application
compile, service, deployment or generic-extraction guarantee is claimed.
The isolated `/var/tmp` path also avoids a known ancestor `/src/` namespace
inference defect; it does not prove that production-path bug fixed.

## Retained local receipts

Root: `/var/tmp/forge/astra-program/application-extraction-preflight/`.

- `direct-payload-v2/`: exact payload, generator authority, preparation ledger.
- `runtime-server-attestation-1.json`, `public-extraction-1.json`: actual endpoint and one write.
- `candidate-oracle-1/`, `oracle/docstring-amendment.json`: original failure and explicit correction.
- `baseline-behavior-2/`, `candidate-oracle-2/`: amended-epoch baseline and passing same candidate.
- `real-negatives-1/`: five executed negative witnesses and preserved perturbations.
- `independent-review/final-review.md`: bounded independent GO.
- `final-state-1.json`, `quality-timings-1.json`, `FINAL-EVIDENCE-HASHES.json`: immutability, separate clocks and receipt hashes.

Public receipt SHA256: `b1c96c4e6e4e608e7db196dc487fe7154db51918fafb43d6a9249b7cdd51ded4`.
Amended oracle result SHA256: `16b215d9170206b25bcca5b2c02f7dc013f150f7aa23473b95d57a07439d692b`.
Independent final review SHA256: `6385f2e76e0dd3c37a152ada51522769b13379708f3166df029a4b10f156e6ab`.
