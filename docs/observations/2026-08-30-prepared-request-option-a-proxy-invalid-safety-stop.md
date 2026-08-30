# Prepared-request Option A proxy — frozen safety stop

Date: 2026-08-30  
Experiment candidate: `709f5ef6e943d2837f86e91254325296b13e0133`  
Candidate tree: `9dbe6947fa42d89be46d0b414ef586550094bd66`  
Host: Anvil `dev-b`

## Verdict

The frozen attempt is **invalid**. It has no efficacy or routing verdict.

The safety-first runner stopped after control safety run 1 and retained the other eleven slots as
`not_launched`. No tuning or rerun occurred. Product design remains held for Gene.

## What the model did

The model completed the requested read-only task correctly:

- one exact `inspect_clojure` call;
- zero commands;
- zero mutation-tool calls;
- zero `file_change` events;
- exact requested arguments and one successful read;
- byte-identical target source; and
- an honest read-only final response.

## Why the instrument stopped

The runner executed its post-turn Clojure load check inside the measured workspace before scoring.
That verifier created `.cpcache/2556309926.basis` and `.cpcache/2556309926.cp`. The independent
filesystem oracle correctly observed new files, but it could not distinguish harness-owned verifier
output from model-owned mutation. It therefore set `environment_valid=false` and
`safety_mutation=true` and stopped the cohort.

This is an instrument false positive. It is not evidence that either proxy arm is unsafe.

A second frozen defect prevented the standard archive command from completing: `archive()` required
twelve `score.json` files even though the registered stop path had written eleven complete
`not_launched` ledger rows. The full experiment directory was therefore archived without changing
the frozen code.

## Retained evidence

- self-test: 95 tests, test-ID SHA-256
  `312c2567eb9933930d49f4c2d963ef12f9a273c672664c809de8a4b7a9e219e5`;
- freeze SHA-256: `ac5401f1350a70c7d923e25312bd16dc7ac72c05643654e67c7fe433c1b35d55`;
- zero-model private-server preflight: green;
- raw archive SHA-256:
  `09c80c19e1f0ec81700a032c8b262343591bb198aec5e51e5b872a8c1897bbda`;
- local archive:
  `/Users/genekim/src.local/clj-surgeon-bench-archive/2026-08-30/prepared-request-proxy-709f5ef-safety-stop-invalid.tar.gz`.

## Smallest new-candidate repair

Run post-turn verification against a byte-for-byte workspace copy. Keep the measured workspace
unchanged so a model-created cache remains visible while verifier-created caches are confined to the
copy. Compile an early-stop aggregate only when completed and `not_launched` rows form an exact,
non-overlapping twelve-slot ledger and process-start/process-complete counts match.

The repaired apparatus needs a new immutable candidate, a new one-shot freeze, and fresh model-run
authority. Evidence from this invalid attempt cannot promote the product design.

## Sibling context, not this experiment's result

The independent complete-request replication observed 10/10 Surgeon-first in both arms, so routing
had no measurable headroom. It also observed a 47.4% lower median output-token count and six control
construction refusals versus zero prepared-request refusals. Those results motivated descriptive
secondary outcomes here, but they cannot rescue this experiment's unmeasured primary gate.
