# Prepared-request eligibility successor re-audit

Date: 2026-08-31  
Auditor: SURGEON2  
Verdict: **NO-GO as release-train test cargo; two prior blockers closed, two remain**

## Immutable subject

- Candidate: `5a6c970a4b0dce915ae0e817960d3017018bc0d1`
- Tree: `1f5f70fcf739e30a332bde0456a11930a6452866`
- Parent rejected candidate: `6991639eabc45da1c260ed11cbdcf83bd06eed32`
- Repaired test SHA-256:
  `1b3f0182abdf47bcac0da70e9f7a3a43afda9397318cc592e229b36e1c16b462`
- Repaired runner SHA-256:
  `8844e1a11c110733f8a8ee5ff668625d37c2008e8587b00384a4f94e9f86fba8`

## Prior blockers closed

### Single-axis witnesses

The repaired fixtures are internally consistent. Independent evaluation with
the candidate's explainer and the real product projector returned:

```clojure
{:dup-failing [:owners-distinct]
 :dup-attaches false
 :seven-failing [:count-1-6]
 :seven-attaches false}
```

Removing either named product guard would now turn its corresponding fixture
eligible and fail the direct projector assertion. The earlier multi-cause
false greens are closed.

### Release-runner registration

`clj-surgeon.eligibility-characterization-test` appears once in the runner
namespace require vector and once in `mcp_test_runner/-main`'s ordered
`run-tests` arguments. The cargo is no longer silently omitted.

## Remaining typed blockers

### 1. Agreement test reads an unrelated absolute checkout

`dev-explainer-agrees-with-product` executes:

```clojure
(load-file "/tmp/cathedral/dev/explain_eligibility.clj")
```

The candidate is in a clean detached worktree elsewhere. The focused test
passed only because `/tmp/cathedral` happened to exist and its explainer
currently had the same SHA-256 as the candidate copy:

```text
d2e84a7f...  /tmp/cathedral/dev/explain_eligibility.clj
d2e84a7f...  <candidate>/dev/explain_eligibility.clj
```

This is not candidate-bound evidence. A missing `/tmp/cathedral`, a stale
checkout, or another process changing it makes the release result depend on
external machine state. Conversely, a candidate explainer regression can be
hidden by a still-good cathedral copy.

The repair is to load the candidate-owned explainer through the test
classpath, or relocate it into a required test-support namespace. Do not use
an absolute shared-worktree path.

The current agreement assertion also checks only that the positive base is
eligible. It does not bind the explainer's negative classifications to product
truth. If the stated cargo claim is general agreement, assert agreement for
the complete characterization table, including exact singleton failures for
duplicate owners and the seven-form boundary.

### 2. The corrected count still does not reproduce

The successor was reported as `15/15 reproducible`. Exact clean-detached
execution produced:

```text
Ran 14 tests containing 14 assertions.
0 failures, 0 errors.
```

The namespace contains one positive test, twelve refusal tests, and one
agreement test: fourteen total. The evidence must say 14/14 or add and name the
missing fifteenth witness.

## Decision

NO-GO for release-train cargo at `5a6c970a`. Product eligibility remains green,
the two safety-sensitive single-axis guards are now genuinely pinned, and
runner wiring is correct. Publication is blocked only on making the explainer
agreement candidate-local and reconciling the exact test count. A general
explainer-agreement claim additionally requires checking all characterized
rows, not only the positive base.
