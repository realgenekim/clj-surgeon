# Archive Benchmark Evidence

**Status:** Open
**Severity:** P0 release blocker

## Evidence

The Captain's Log contains durable summaries, but the valid prompts,
transcripts, commands, timings, usage, scorers, diffs, and hashes exist only
under `/private/tmp`. The audit plan explicitly requires raw evidence to be
retained. Temporary directories do not satisfy that contract.

Two roots must never enter a comparison:

- `/private/tmp/clj-surgeon-xray-tree-seq-pilot-20260804` was corrupted by
  concurrent resume writers.
- `/private/tmp/clj-surgeon-claude-fable-opus-20260804-v1` never launched a
  valid model trial.

## Required Outcome

Archive every valid result root named in the handoff under a durable,
versioned `bench/results/` location, or produce an intentionally redacted but
independently rescorable archive. Add a manifest containing source/result
hashes, candidate tags, model settings, exclusions, and the redaction policy.
Keep all material SFW.

## Tests and Verification

- A script or pure verifier checks manifest paths and SHA-256 hashes.
- Every summarized result can be traced to its prompt, final answer, commands,
  score, and timing row.
- Excluded roots are named and cannot be included by the summarizer.
- A repository and commit-history scan contains none of the prohibited
  project-context terms.

## Done When

A fresh checkout can reproduce every table in the Captain's Log from durable
artifacts without access to `/private/tmp`.
