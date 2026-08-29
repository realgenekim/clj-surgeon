# Captain's Log: Location Mistakes Became Compiler Input

Date: 2026-08-28

## Result so far

Eight retained attempts at the same 51-edit, nine-file cleanup contained four
distinct request bodies and three recurring location-shape mistakes. The old
strict route accepted seven of eight attempts after two bounded tolerance laws.
The implemented compact-only compiler accepts all eight and produces the exact
frozen future hashes while preserving 23 candidate owner rows and 27 declared
migration matches.

This is not fuzzy repair. The compiler only lowers a compact edit when the frozen
source proves one explicit owner:

1. An exact namespace name placed in `within.form` may become namespace scope only
   when no named owner competes and the file has one direct namespace with that
   exact name.
2. An omitted location around a complete namespace clause may become namespace
   scope only when the declared count equals the direct namespace-child count,
   namespace-descendant count, and whole-file lossless fingerprint count.
3. An omitted location around a complete named top-level form may become form
   scope only when before and after preserve owner kind and name and the frozen
   file contains exactly one direct owner with the complete old fingerprint.

The output is the same explicit generic transaction the strict kernel already
understands. Similarity grants no authority.

## The architectural move

```text
compact JSON request
        |
        v
source-blind schema preserves omission
        |
        v
one frozen workspace source map
        |
        v
pure injective location relation
   | exact proof                 | no exact proof
   v                             v
explicit namespace/form owner   typed pre-write refusal
        |
        v
unchanged generic transaction compiler
        |
        v
atomic commit + read-back + receipt
```

The normalizer is a private compact-edit preparation seam. Generic direct
changes, CLI selectors, retained-basis changes, extraction, and computed programs
do not enter it. This keeps tolerance at the interface boundary and mutation
authority in the existing compiler.

## Adversarial review changed the product

The first green implementation was not safe enough. Independent review found two
real defects before the candidate commit:

- A reader-conditional owner such as `#?(:clj (defn f ...))` could be mistaken
  for a direct top-level owner. The repaired proof requires the direct parsed
  node head to equal the outline owner type.
- Present but malformed `within` values such as `null` or `false` could be treated
  as omission or throw an untyped exception. The repaired adapter validates every
  present value as an object.

The review also found that the product replay proved future hashes but did not
publish the candidate's 23 owner rows and 27 declared matches. Those facts are now
carried and asserted, so payload reduction cannot silently discard intent.

## Immutable candidate

- Control: `24056d28fc42f071fb8948bc339a03e716eac4ef`
- Candidate: `7bf0d3d4063454496d26d610d1a2ff09781ff0e2`
- Candidate tree: `7cc68f3ed87db8fc42e092e371edf01ef9f25099`
- Binary diff SHA-256: `3d71c1696d4cc0056562173211d30f9cd2ad29e2b25875d883a0e66f577b0f17`
- Retained manifest SHA-256: `3ea6005ab4deb02f24ffedee52d79934f25f0b847a85606f046ca80135f88feb`

Local evidence:

- retained replay: 5 tests, 90 assertions, 8/8 exact;
- frozen result: 51 matches, nine files, exact future hashes;
- product intent: candidate 23 owner rows and 27 declared matches preserved;
- fast suite: 636 tests, 5,467 assertions;
- MCP suite: 274 tests, 2,361 assertions;
- Linked-Intent audit: coherent, no violations;
- independent adversarial verdict: safe to commit.

## First activation gate: correct mechanism, insufficient proof

SURGEON2 independently matched every candidate hash and passed the focused
mechanism tests, then stopped before Anvil. The stop was correct. Candidate
`7bf0d3d` proved that the implementation worked, but its activation evidence
could still hide several classes of regression:

- the durable manifest referred to `/tmp` raw captures and reconstructed the
  public requests from static data; deleting or replacing the raw corpus would
  not fail the witness;
- aggregate 23-owner/27-match checks could miss row permutation or substitution;
- compact-only non-invocation was directly proved for generic changes but not
  retained-basis, extraction, standalone programs, or CLI;
- client-visible schema parity was inferred from internal tests rather than an
  advertised MCP -> SDK -> Codex registry receipt for the exact candidate;
- `.cljc` platform ambiguity and a duplicate namespace-named top-level owner
  lacked exact permanent falsifiers.

The Anvil model launch was cancelled. These are evidence defects, not reasons to
weaken the gate. The next candidate must make every one mechanically falsifiable.

## The stop made the experiment stronger

Candidate `4904d4e` closes the six gaps without changing the normalization
mechanism:

- eight repository-local `captured-calls.json` files are byte- and hash-bound to
  the retained raw corpus;
- all 23 candidate owner/count rows are asserted in exact order, not only by
  aggregate count;
- throwing spies prove public generic-change and programs-only routes,
  retained-basis changes, extraction, and CLI never invoke compact
  normalization;
- separate exact `.cljc` reader-conditional owner and namespace-clause
  ambiguities, plus duplicate namespace-named owners, permanently refuse;
- a real MCP `tools/list` -> SDK -> fresh Codex registry receipt proves that the
  complete schema arrives unchanged while Codex exposes only `edit_clojure`;
- the JVM MCP test alias now declares the Babashka filesystem and process
  dependencies needed by its real CLI-route witness.

Focused evidence is green: retained replay 5 tests/142 assertions and compact
route/falsifier suite 9 tests/101 assertions. The first local cold MCP attempt
was intentionally not treated as a product verdict: it ran during an unrelated
photo-render convoy at load 179 and missed two 250 ms admission-timeout timing
assertions after all product tests had loaded. At load 13.7 the same gate passed
278 tests/2,385 assertions, including every ancillary analyzer-admission check.
The complete `make test` milestone then passed: fast 636/5,467, analyzer 4/20,
MCP 278/2,385, smoke, skill, harness, portfolio, retention, and evidence gates.

The strengthened witness also discovered a separate pre-existing public
contract mismatch: `edit_clojure` advertises programs-only input, but the handler
currently refuses after lowering it to an empty generic `changes` array. The
route-isolation witness preserves that exact behavior, and `clj-surgeon-e1g`
owns the decision to support it or route it explicitly to `transform_clojure`.

## Final activation gate: GO

- Candidate: `4904d4ea52c1e1330bc9f8a04c8a5e393af9a758`
- Candidate tree: `c47f1cce0b28696acebd29d0e66973621ea79e6e`
- Binary diff SHA-256:
  `790d9ae7b0f8623c5abf91b5dfea1ecdbf1937de06535186377a4c995bea1e5e`
- Retained manifest SHA-256:
  `536a2b882e5ab3cdfd1563d445d3c2919c62cb9e2679ded1899f38be3d5a74ca`

SURGEON2 independently reran the exact compact and replay gates at 512 MiB,
proved the candidate clean and dead afterward, and issued activation GO. The GO
authorizes only the frozen serial A/B, B/A model cohort. It does not authorize
installation, shared-runtime reload, or a broader experiment.

## Performance question

The causal question is now narrow: does accepting mechanically recoverable
location shapes eliminate enough refusal and reconstruction time to improve the
complete task, without making the successful request or receipt harder for the
model?

The frozen experiment uses the production `edit_clojure` surface, a fresh
Sol/high caller, the same 51-edit/nine-file fixture, and serial ABBA ordering.
Every attempt counts. The candidate must be exact, first-call successful,
atomic, and refusal-free before the cohort expands.

The retained same-fixture baseline is:

| Order | Compact `edit_clojure` | Native | Surgeon speedup |
|---|---:|---:|---:|
| compact-first | 55.763 s | 206.727 s | 3.71x |
| native-first | 61.354 s | 372.286 s | 6.07x |
| N=2 midpoint | **58.559 s** | **289.507 s** | **4.944x** |

Surgeon was 2/2 semantic and byte exact, with one verified MCP mutation and no
failed mutation in either run. Native was 2/2 meaning-preserving and 1/2 byte
exact; both routes retained one failed mutation path. The current experiment
does not optimize native. It asks whether eliminating compact-location refusals
can lower Surgeon's 58.559-second same-fixture midpoint while retaining its
one-shot geometry.

Results will be appended after the immutable Anvil receipt arrives.

## First model cohort stopped before the model

The first scheduled A/B, B/A cohort is immutable evidence, but it is not a
performance result. All four invocations stopped before the first model token
at the same client-registry gate:

```text
expected ["edit_clojure"]
actual   ["apply_clojure_changes" "edit_clojure"
          "inspect_clojure" "transform_clojure"]
```

No prompt, model, MCP operation, or source mutation occurred. Candidate
`4904d4e` was therefore not exercised or falsified, and no candidate/control or
native ratio can be computed from this cohort.

The stop exposed a benchmark wiring defect. `BENCH_MCP_TOOL_PROFILE=edit`
correctly selected the server's edit-only profile and changed the expected
registry. The canonical `write_mcp_config` branch did not pass
`--enabled-tools-edn`, so the generated Codex client configuration silently
used the canonical four-tool default. The earlier standalone surface proof
passed the filter explicitly and therefore could not falsify this canonical
harness branch.

Immutable stopped-cohort receipt:

- acceptance branch: `experiment/anvil-injective-location-cohort`;
- receipt commit: `7f155e981974b3b6fdf0eb814bddfc0d2b9facc4`;
- receipt tree: `611c62b722f651d1f48e5dd81fd3e31d72a13bc5`;
- archive:
  `/Users/genekim/src.local/clj-surgeon-bench-archive/2026-08-29/clj-surgeon-location-abba-pretoken-stop-20260829T052614Z.tar.gz`;
- archive SHA-256:
  `9d24bfd6b8d9ac885c304950eef9085c076110ace7e1f78b49bee9b001938b63`;
- embedded raw manifest SHA-256:
  `ca9c1a6ad94d129fd26a31b10d44259713802df5e484cf0413500b91e826e342`.

The earned ratchet is narrow: derive the generated client's enabled tools from
the same closed profile that configures the server, then prove the full/edit
boundary through the actual canonical harness before starting a model. A
repaired experiment must be a fresh whole cohort. Failed arms will not be
selectively retried or omitted.

## Second admission stop: product identity is not parent depth

The repaired full/edit preflight passed locally, but the next fresh cohort
stopped even earlier on Anvil, before the registry preflight or any model token.
The controller assumed the frozen product was always the derived branch's
immediate parent. The candidate projection had two harness commits above
`4904d4e`, while the control projection had one harness commit. Consequently,
`HEAD^` meant a harness commit in one arm and a product commit in the other.

This was again an experiment-controller defect, not a candidate result. The
independently computed product diffs were already identical, as were the
projected benchmark trees, but a positional ancestry assertion refused the
run. The next ratchet is to record and verify explicit immutable
`product_commit` and `harness_commit`/tree identities for every arm. Product
identity must never be inferred from the number of commits between it and the
derived experiment ref. A permanent token-free falsifier must cover unequal
harness depths before another cohort is authorized.
