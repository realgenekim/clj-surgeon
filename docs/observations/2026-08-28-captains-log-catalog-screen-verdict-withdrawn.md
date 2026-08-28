# Captain's Log: The Catalog Verdict Did Not Survive Contact With Its Traces

**Date:** 2026-08-28  
**Owner:** `clj-surgeon-x9d`  
**Experiment head:** `a953e418cf06c458199c9bda670af1c2b5d8157f`  
**Status:** the strong 1-of-11 vocabulary verdict is withdrawn

<!-- agent-usage-window-end: 2026-08-28T14:52:11.111450Z -->

## Corrected verdict

The screen did not prove that the current name `apply_clojure_changes` beat ten
alternate vocabularies. It uncovered useful product and benchmark defects, but
those defects invalidate the claimed vocabulary comparison.

The decisive falsifier is operational, not philosophical:

```text
advertised catalog L or M
    edit_clojure schema: no extraction field
                 |
                 v
model calls edit_clojure with an extraction payload anyway
                 |
                 v
shared unscoped runtime handler accepts extraction
                 |
                 v
15-form extraction commits successfully
                 |
                 v
facade relabels the result as edit_clojure
                 |
                 v
scorer calls the completed task a wrong-role failure
```

Catalogs L, M, and N therefore did not demonstrate that the model could not use
their extraction vocabulary. They demonstrated that the public tool boundary
did not enforce its own schemas and that the scorer preferred a nominal role
over the completed user outcome.

The honest conclusion is narrower:

- catalog A produced one short route in one run;
- several alternate catalogs produced semantically correct source;
- the experiment cannot attribute the route differences to vocabulary;
- no public name should be kept, rejected, or shipped from this cohort;
- the facade remains a valuable architectural ratchet because it made this
  contradiction observable and makes a corrected experiment cheaper.

## Experiment-breaking findings

| Finding | Exact evidence | Consequence |
|---|---|---|
| Cross-tool authority leak | Both `edit-clojure-tool` and `clj-change-tool` use `#'handle-clj-change`. That callback calls `handle-operation`, which derives the operation from request fields rather than the invoked public entrance. | A request can exercise capability absent from the invoked tool's advertised schema. Public names and schemas were not real authority boundaries. |
| Runtime did not enforce the advertised schema | `editor-tool-schema` contains only `edits`, `programs`, or `delete_owners`; nevertheless archived L and M calls sent `extraction` to `edit_clojure` and committed all 15 forms. | “Wrong tool” did not mean failed capability. The model crossed the nominal boundary successfully. |
| Response projection hid the crossover | The facade wraps the shared handler by invoked role and rewrites operation identifiers. L's structured result says `operation: edit_clojure` even though the executed request was semantic extraction. | The receipt describes the entrance, not the actual operation. The scorer cannot reconstruct what executed from the projected result alone. |
| Primary score optimized adoption, not the user outcome | L, M, and N have `source-fidelity.edn` with `parseable=true`, `meaning-preserved=true`, and `correct=true`, but `runs.tsv.correct=false` because their first resolved role was `edit`. | The 1-of-11 headline inverted the repository's north star: complete verified task time, not Surgeon route compliance. |
| Required verifier was absent | Every prompt required `~/bin/clj-kondo`. A and L got exit 127 and explicitly reported that lint cleanliness was not confirmed. | A was called benchmark-correct although the requested complete verification did not succeed. It cannot be the verified control. |
| Client-visible catalog was not retained | Each run retained the server-side role receipt and candidate name, but not the exact initialize response and `tools/list` bytes/hash observed by Codex before its first action. | We know what the server intended to advertise. We cannot prove what the model actually received. This is especially material because most callers selected `inspect_clojure` despite explicit extraction instructions. |
| Native callable-tool exposure followed old names | C/L/M/N retained canonical `edit_clojure`, and every one eventually called it through the native MCP router. O/P/Q/R/S/T renamed every mutation tool, and every one initially called only canonical `inspect_clojure`. P/R/S used their new extraction names only after manually fetching `tools/list` and issuing raw HTTP calls. | This is strong evidence of a client exposure, refresh, or cached-tool confound. It is not proof that the model rejected the candidate names. |
| Some candidate surfaces still mixed vocabularies | The later raw R/S `tools/list` captures include title strings such as `edit_clojure_commit!`, `edit_clojure_bang!`, and `transform_clojure_with_clojure_with_clojure`. These strings are not represented by the simple role receipt. | The proposed “one internally complete lexicon” premise was false for at least part of the caller-visible surface. |
| The 11 arms were not a vocabulary-only factorial | A/L/C expose 4 tools; M/N/O expose 5; P/Q/R/S/T expose 6. The arms also change schema partition, descriptions, annotations, order, and preview/commit splitting. | The combined table compares complete interface universes, not eleven names. Only A/L/C are close to a name-only contrast. |
| One replica was treated as an elimination gate | Each catalog ran once, serially within one of three hosts. Catalog, host, order, and random model latency are entangled. | A first-action miss in one stochastic call cannot eliminate a vocabulary. |
| Fresh home did not erase the model's learned prior | The harness removed installed Surgeon skills and CLI copies, but it cannot remove pretrained or product-level familiarity with `inspect_clojure`, `edit_clojure`, or `apply_clojure_changes`. | The screen may measure migration cost from an existing vocabulary. It does not isolate intrinsic name clarity. |
| Model provenance was asserted but not archived | The result artifacts do not contain the resolved Codex argv, model, or reasoning effort. | The launch charter says Sol/high, but the raw archive cannot independently prove that stratum. |
| Archaeology proof was bounded too narrowly | The zero count only rejects commands mentioning a short list of files such as `CLAUDE.md`, `SKILL.md`, and `mcp_candidate_catalog.clj`. | Zero is useful evidence, but not proof that every possible legacy-name source was absent. Later raw `tools/list` discovery in P/R/S was visible in traces and was part of recovery, not first-call archaeology. |

The corrected arm classification is therefore:

| Catalog | Semantic source | Verifier outcome | Boundary or route defect | Valid vocabulary comparison? |
|---|---|---|---|---|
| A | correct | requested path failed, exit 127 | one sample; client surface absent | no |
| L | correct | requested path failed, exit 127 | extraction crossed through edit entrance | no |
| C | correct | alternate binary passed | inspect-first recovery; one sample | no |
| M | correct | alternate binary passed | extraction crossed through edit entrance | no |
| N | correct | alternate binary passed | same crossover; duplicate-form retry | no |
| O | no mutation | not run | inspect-first stop | no |
| P | correct | requested path failed, exit 127 | raw protocol recovery | no |
| Q | no mutation | not run | inspect refusal and stop | no |
| R | correct | requested path failed, exit 127 | raw protocol recovery | no |
| S | correct | alternate binary passed | raw protocol recovery | no |
| T | no mutation | not run | inspect and plan only | no |

This does not say every alternative is equally good. It says the retained
screen supplies no clean arm-to-arm vocabulary comparison. Across all arms,
the defensible counts are 8/11 semantically correct outputs, 1/11 correct first
roles, 0/11 successful executions of the exact requested verifier, and 0/11
fully valid benchmark successes.

## The timing result was not a name result

Catalog A and L performed essentially identical server work:

| Interval | A | L | Difference |
|---|---:|---:|---:|
| Initial model materialization | 24.669s | 50.075s | +25.405s |
| MCP operation observed wall | 2.248s | 2.250s | +0.002s |
| Server-authoritative operation | 2.213s | 2.214s | +0.001s |
| Complete process wall | 45.600s | 68.965s | +23.365s |

The kernel did the same 15-form extraction in both arms. Nearly the entire wall
difference occurred before the first call in one sample per name. That is model
or service variance until a counterbalanced cohort says otherwise.

Catalog M sharpens the same point. Its successful extraction kernel took
1.925s, but its initial model materialization took 78.487s. Catalog N first
duplicated `->instant`, received a safe refusal in 8.98ms, corrected the payload,
and then committed in 1.681s. The duplicate was caller stochasticity, not a
vocabulary property.

The fixed execution order was A→L→C on dev-a, M→N→O→P on dev-b, and
Q→R→S→T on dev-c, with the three lanes overlapping. No host load receipt was
retained. Catalog family, host, position, and wall time are therefore
confounded.

## What the raw traces do prove

The experiment is not worthless. It proves several useful facts:

1. All eleven prompts were byte-identical. Their SHA-256 is
   `d7b9808b9e1e33e62bb6d8e3f1b313ad3712efd39c8d7d0738eefc17856d33e0`.
2. Every private server recorded the intended catalog identifier and completed
   startup.
3. Every retained bounded archaeology counter was zero.
4. A, L, M, and corrected N all completed the supplied extraction through one
   shared runtime family. Their semantic scorer passed.
5. The first-call tool and exact arguments are retained, including the L/M/N
   `edit_clojure` crossover.
6. P, R, and S show expensive recovery routes through raw protocol discovery.
   Those traces remain useful UX evidence, but not clean vocabulary arms.
7. O, Q, and T show that a model can fail to discover an advertised capability.
   Without the client-observed catalog receipt and replication, the cause is
   unresolved.
8. The isolated public-interface facade is still worth keeping. Its value is
   cheap, reversible interface experiments and typed projection—not evidence
   that catalog A won.

## Corrected scoring law

The old `runs.tsv.correct` field is a route-gated composite. It overwrites
semantic success when route adherence fails. Its `verified` field means only
that the mutation receipt reports commit, read-back verification, and no next
mutation action. It does not mean that the task's clj-kondo command succeeded.

A corrected harness must preserve independent facts:

| Field | Meaning |
|---|---|
| `semantic_correct` | The scorer reports parseable, meaning-preserved source with no semantic errors. |
| `mutation_receipt_verified` | The atomic mutation committed, read back, and reached its mutation-scoped terminal state. |
| `task_verifier_status` | Exact task command is `pass`, `fail`, `missing`, or `not-run`; an approved equivalent needs its own binary/version/hash evidence. |
| `route_adherent` | The expected role was selected first and the declared route constraints held. |
| `client_surface_verified` | The exact client-observed instructions and tool catalog were archived before first action. |
| `environment_valid` | Fixture, source, caller argv, model, effort, tool surface, and verifier provenance are complete. |
| `benchmark_correct` | Environment valid, semantic correct, mutation receipt verified, task verifier passed, and the benchmark's declared route criterion passed. |

Do not overwrite one field because another failed. That separation is what
made the 8/11 semantic rate visible beneath the old 1/11 headline.

## Complete trace availability

All 300 retained files from the three Anvil lanes are copied locally without
summarization:

```text
/Users/genekim/src.local/clj-surgeon-bench-archive/2026-08-28/
  catalog-screen-a953e41-adversarial-audit/
```

The directory contains the prompt, complete Codex JSONL event stream, started
items, commands, final response, phase clocks, candidate catalog, server-side
role receipt, MCP server stdout/stderr, MCP telemetry, source-fidelity score,
route score, hashes, diffs, and per-arm artifact manifest for A, L, C, M, N, O,
P, Q, R, S, and T.

Integrity evidence:

| Artifact | Size/count | SHA-256 |
|---|---:|---|
| `raw-tree.sha256` | 300 entries | `547813df03328d673cbc2f58402797076fe6ebcaef20fe59210a534b45ad585c` |
| `catalog-screen-a953e41-adversarial-audit.tar.gz` | 216 KiB | `83bd0b95b43371e48bd17ae3cf2b60699422a03ce9959ae2f12d749cf4087e6f` |

The three lanes retained identical frozen-input manifests:

| Input | SHA-256 |
|---|---|
| `bench/run_clean_codex.sh` | `453e451c439ad75c4e969a70233967ce95d2ef1c235e95a306eb58d04a57316f` |
| extraction task | `f1e9e3353a4ce89c57bbd143c46e8a66db5501868e74287f7890f5e658772b09` |
| semantic scorer | `2ef2880b422526b7ec6f31d85bf2f29fb54855ee6531526270dacf3a4cd63fa4` |
| candidate catalog | `6ac281ad91a4451ede84c74fdcd70a14699c474e61f99fe6f5af2305e0cf8334` |
| response projection | `9b3a863db72242127f988abfd49e65fc9ec52189131886e4fae2614cce000a89` |

The historical archive lacks one artifact that a corrected harness must add:
the exact client-observed initialize and `tools/list` exchange before the first
model action.

## Corrected experimental law

A future vocabulary result is valid only when all of these hold:

1. **Real entrance authority.** Each public tool validates its own closed
   request schema at runtime. A compact edit entrance must refuse extraction;
   an extraction entrance must refuse compact-edit fields.
2. **Two operation identities.** Receipts retain both `invoked_tool` and
   `executed_operation`. Projection may translate presentation, but it must not
   erase a crossover.
3. **Client-observed surface receipt.** Capture and hash the exact initialize
   instructions, ordered tool names, descriptions, schemas, annotations, and
   output schemas delivered on the model's connection before its first action.
4. **Task correctness is primary.** A run passes only if source meaning is
   preserved and the requested verifier succeeds. First-role choice and route
   geometry are diagnostic outcomes, not vetoes over a completed task.
5. **Pinned verifier.** Supply one candidate-owned executable path or fuse the
   exact repository verifier into the transaction. Exit 127 is an invalid arm,
   not a verified success.
6. **Pure contrast first.** Start with A/L/C, holding tool count, schema,
   descriptions, annotations, ordering, fixture, and prompt constant except for
   the one name token being tested. Test split catalogs separately.
7. **Counterbalance stochastic callers.** Run at least four fresh serial
   replicas per retained arm with randomized or ABBA order on the same host.
   Do not eliminate an arm from one first-call sample.
8. **Two prompt strata.** Keep the current forced first-selection probe as a
   classification test, and add a natural task prompt as the end-to-end UX
   test. Do not generalize from one to the other.
9. **Report semantic success separately.** Publish task correctness, verifier
   result, invoked tool, executed operation, first-call schema validity, route
   actions, and wall time as separate columns.
10. **Archive caller provenance.** Retain the resolved Codex executable and
    argv, version, model, reasoning effort, and environment receipt per arm.

## Decision

Withdraw the statement that catalog A defeated all ten alternatives. Preserve
the raw cohort as a falsifier and benchmark-harness regression corpus.

Before another naming cohort, fix the entrance-authority and evidence gaps in
the experimental facade and harness. Do not rename production tools, and do
not use this invalid screen as evidence for keeping the current names either.
The names are an open question again.

The most valuable result is architectural: a public interface is not a name or
a JSON schema. It is an enforced capability boundary plus evidence of what the
caller actually saw and what the runtime actually executed.
