# Captain's log: single-pass extraction survived adversarial acceptance

Date: 2026-08-26

SURGEON2 tested SURGEON1 candidate
`65e72b7010b380facbad1bc2fa30a17eb552804e` in a clean detached worktree.
No candidate source was changed. No release or shared-runtime action was taken.

## Result

Accept the single-pass extraction mechanism. Keep one response-evidence defect
separate as `clj-surgeon-6l9`.

| Case | Outcome | Complete wall or kernel time | Route or safety evidence |
|---|---|---:|---|
| Supplied complete decision | Correct | 39.150 s wall; 11.121 s kernel | One apply, one exact lint, zero discovery or failure |
| Mechanically derivable omissions | Correct | 37.500 s wall; 8.297 s kernel | One apply, one exact lint, zero discovery or failure |
| External caller with target-alias collision | Safe refusal | 110.219 ms warm kernel | Completed plan, one exact hashed unknown, no authority or effects |
| Stale source hash | Safe refusal | 3.804 ms warm kernel | Source equal, target absent, receipt absent, formatter and verifier did not run |

The mechanically derived route was 1.650 seconds faster than the supplied route
in this pair. Its kernel was 2.824 seconds faster. One pair is not a population
estimate. The mechanism claim rests on route geometry and the permanent
one-compile witness: the model no longer pays a public planning phase when the
kernel can prove every omitted fact from one frozen snapshot.

```text
BEFORE: derivable omission

inspect plan -------- model boundary -------- apply -------- exact lint

AFTER: same authority

apply
  |
  +-- freeze workspace once
  +-- compile plan once
  +-- derive required visibility and counts
  +-- no genuine unknown? commit atomically
  `-- genuine unknown? refuse before write with completed plan
                                                        |
                                                        v
                                                   exact lint
```

## Case details

### 1. Supplied decision

The fresh Sol/high session made exactly two actions:

1. `apply_clojure_changes`
2. `clj-kondo --lint src/cfp_scheduler_killer/views.clj
   src/cfp_scheduler_killer/views/format.clj --fail-level error`

The apply returned `ok=true`, `verification_complete=true`, one change, and two
files. The scorer accepted the result as semantically faithful.

### 2. Mechanically derivable omissions

The fresh Sol/high session omitted `public_forms`, caller decision arrays, and
aggregate expectations. It made the same two actions as case 1. The apply
returned `ok=true`, `verification_complete=true`, one change, and two files.
There was no `inspect_clojure`, native source read, refusal, failed mutation, or
repair.

### 3. Genuine caller decision

The source fixture moved `not-blank` and `fmt-date`. An external caller already
used the destination alias for `clojure.string`. The executor returned
`extraction-decisions-required` before any extraction side effect.

Fourteen assertions proved:

- `mutation_attempted=false`;
- `write_authority=false`;
- the completed plan names required visibility `not-blank`;
- the genuine unknown names the exact caller and a 64-hex source hash;
- source and caller bytes are unchanged;
- no target or receipt exists.

This is the correct authority boundary. The compiler reports the decision. It
does not guess through the alias collision.

### 4. Stale hash

A public extraction request supplied a deliberately wrong 64-hex source hash.
The executor returned `source-hash-mismatch` in 3.804 ms. The source remained
byte-identical. No target or receipt directory was created. Formatter and
verifier callbacks were booby-trapped to throw and did not run.

The response did not include `mutation_attempted=false` or
`write_authority=false`. This is an evidence defect, not a mutation defect. Bug
`clj-surgeon-6l9` owns the normalization: every typed pre-write extraction
refusal should state the two false fields explicitly so one response is
terminal evidence for an agent.

## Evidence

- Candidate worktree:
  `/Users/genekim/src.local/clj-surgeon-surgeon2-65e72b7-acceptance`
- Candidate head:
  `65e72b7010b380facbad1bc2fa30a17eb552804e`
- Success runs:
  `/tmp/clj-surgeon-surgeon2-65e72b7-success-arms-r2`
- `runs.tsv` SHA-256:
  `afa2abcecc7579bf8794620fb2349adcf8af3aa9a7ba98b143b69c7a3e6bd1a8`
- `summary.md` SHA-256:
  `f5372b7ed71c1668e209c1713f7d101ccee648ea1e0fc239506ce7a44a9eeb19`
- Boundary probe SHA-256:
  `4b27e6ae9eefe27428f6521d68a0f9144d3ab0c1f0bf2354d5ec8e013c3abf9f`

The first harness launch failed before server readiness because Bash with
`set -u` treated an empty `mcp_java_opts` array as unbound. Supplying the
documented `BENCH_MCP_JAVA_OPTS=-J-Xmx2g` input produced the valid run. That
launch miss did not exercise the model or candidate and is excluded from the
performance receipt.
