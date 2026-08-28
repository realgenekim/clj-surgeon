# Captain's Log: Tool Names Enter the Arena

**Date:** 2026-08-27  
**Owner:** `clj-surgeon-x9d`  
**Experiment head:** `6128b9babacdfc44cc9e59efa323c323a751e948`  
**Status:** first falsifiers complete; full Anvil catalog screen in flight

## Why test names at all?

The product objective is complete verified task time, not prettier vocabulary.
The mutation kernel often finishes in less than a second, while each additional
model/tool boundary can cost several seconds. A tool name or schema that causes
one wrong selection can therefore cost more than a large kernel optimization.

The plausible win is route deletion:

```text
ambiguous catalog
    -> interpret overlapping controls
    -> call a plausible wrong control
    -> refuse or fall back
    -> reinterpret the catalog
    -> mutate

orthogonal catalog
    -> map intent to one control
    -> mutate once
```

The skeptical case remains binding. `edit_clojure` already has strong field
evidence. More controls can increase choice cost and context size. `_bang` is
meaningful to a Clojurist but indirect to a general caller. A cosmetic rename
cannot repair overlapping schemas. Earlier experiments also showed that fewer
MCP schemas can be slower. A candidate must therefore beat the current route;
conceptual elegance is not a release gate.

## Reversible ratchet

Commits `5c2423a` and `6128b9b` created an isolated projected MCP catalog. Every
candidate reuses the same handlers and output schemas. The experiment changes
only public names, descriptions, schema partitions, titles, and conservative
effect annotations. It does not install, reload, or restart the shared MCP.

The no-model gate passed:

- 10 tests;
- 152 assertions;
- zero failures or errors;
- clj-kondo zero errors and warnings;
- benchmark self-test and diff check green;
- real isolated `tools/list` retained names, schemas, titles, and annotations.

The candidate set is:

| ID | Primary idea |
|---|---|
| A | Current names with stricter descriptions and schemas |
| L | `edit_clojure` plus `apply_clojure_plan` |
| C | `edit_clojure` plus `refactor_clojure` |
| M | Intent organ: edit, extract, plan, and transform |
| N | M plus `transform_clojure_with_clojure` |
| O | Edits-as-data versus program language |
| P | Effect-first `commit_*` verbs |
| Q | Dotted preview/commit names |
| R | Portable `_commit` names plus human bang titles |
| S | Portable `_bang` names plus human bang titles |
| T | Action verbs: write, move, preview, and apply |

## The benchmark taught us how to benchmark it

The first N invocation did not test N. The retained benchmark prompt commanded
the old `apply_clojure_changes` name. Sol correctly reported that the required
control was unavailable. The candidate catalog had exposed a benchmark coupling.

The smallest repair made candidate prompts name-neutral while preserving the
canonical prompt when no candidate catalog is selected. A candidate prompt now
supplies the semantic extraction goal and exact argument shape, then requires
the caller to select the extraction-only control from its schema. The self-test
proves that candidate prompts contain neither `apply_clojure_changes` nor
`edit_clojure`.

This is Kent Beck's cost-of-change rule in miniature: make the experiment honest
and cheap before drawing a product conclusion from it.

## First falsifiers

Both locally tested favorites lost the first-call gate.

| Catalog | First call | Complete observed route | Wall | Input tokens | Mutation meaning | Benchmark result |
|---|---|---|---:|---:|---|---|
| N | `inspect_clojure` | inspect refusal -> `edit_clojure` extraction -> exact lint shim | 70.670s | 158,734 | preserved | false |
| T | `inspect_clojure` | inspect x3 -> 13 shell investigations -> manually launched legacy stdio MCP -> `apply_clojure_changes` -> lint shim | 361.915s | 1,616,949 | preserved | false |

N never selected `extract_clojure`. It sent the supplied extraction payload to
`inspect_clojure`, then to `edit_clojure`. The shared handler still completed
the 15-owner extraction. The benchmark marked the run false because the exact
lint admission shim could not resolve its analyzer. That harness failure is
separate from the wrong public route.

T was the clearest action-verb challenger in first-person review. It still did
not select `move_clojure_owners`. After three inspection attempts, Sol searched
the benchmark and tool implementation, reconstructed the old stdio MCP entrance,
and called legacy `apply_clojure_changes` through a shell-launched client. The
extraction meaning survived, but the route was the opposite of a fast organ.

The initial result looked surprising, but Gene challenged the causal claim:

> Did this experiment actually remove every trace of the older catalog?

It did not. The session and installed skill were fresh, but the projected MCP
surface was not a self-consistent alternate universe:

- `inspect_clojure` retained its production description, including instructions
  to call `apply_clojure_changes`;
- the server-level instructions retained production tool names;
- several candidate-specific descriptions still named `edit_clojure` or
  `transform_clojure` even when the catalog renamed them;
- after the first refusal, T had shell access and searched the harness and source,
  where it rediscovered the legacy tool and stdio entrance;
- `inspect_clojure` remained first in catalog order.

Therefore N and T do **not** prove that model weights or a learned prior defeated
better names. They prove that changing catalog keys without changing every
caller-visible reference creates a contradictory interface. This is a harness
falsifier and a product lesson: names, descriptions, server instructions,
recovery remedies, annotations, and schemas must tell one consistent story.

## Parallel screen

Running eleven local Sol/MCP canaries would needlessly load Skiff. After the N
and T falsifiers, the remaining matrix moved to Anvil:

- dev-a: A, L, C;
- dev-b: M, O, P;
- dev-c: Q, R, S.

Every lane used Sol/high, exact commit `6128b9b`, the same fixture, scorer,
name-neutral prompt, one replica, serial execution within the lane, a private
checkout, and isolated result directories. The experimental commit was pushed
only to `experiment/mcp-catalog-screen-6128b9b`; `main` was not changed.

The parallel screen stopped after the already-running A, M, and Q canaries.
L, C, O, P, R, and S must not run against the contaminated surface. The A, M,
and Q results remain retained harness evidence, not candidate verdicts.

The stopped Anvil screen produced these contaminated controls:

| Catalog | First call | Complete route | Meaning | Wall | Disposition |
|---|---|---|---|---:|---|
| A | `apply_clojure_changes` | one legacy mutation route | preserved | 43.090s | Control only; harness said verified while the model reported verifier blockage. |
| M | `inspect_clojure` | inspect refusal -> edit success | preserved | 74.160s | Harness falsifier; one refusal and wrong public route. |
| Q | `inspect_clojure` | six MCP calls -> failed native mutation | not changed | 314.748s | Harness falsifier; no destination file and scorer false. |

A did not prove that a blurry name is intrinsically clearer. It proved that a
name supported consistently by the remaining production instructions beats a
new name contradicted by those instructions. This is still important product
evidence: a rename is an interface migration, not a catalog-key substitution.
It must change the complete caller-visible language together or not change at
all.

## Current decision

Do not publish N, T, `_bang`, `_commit`, or any other rename yet. Preserve the
two ugly local results. First make the alternate catalog internally complete:

1. project every caller-visible tool reference through one role lexicon;
2. replace server instructions with candidate-consistent text;
3. assert that `tools/list`, initialize instructions, annotations, and recovery
   text contain no unavailable public name;
4. prevent source archaeology from repairing a first-selection experiment;
5. rerun the stopped Anvil matrix only after the no-model leak gate passes.

Then compare:

1. first selected control;
2. schema validity of the first call;
3. recovery route;
4. semantic mutation correctness;
5. benchmark correctness and harness failures;
6. complete wall and token cost.

If no clean candidate selects extraction reliably, the next hill is not another
name. It is the catalog/routing shape: ordering, compatibility visibility, or an
extraction next-call supplied by task preparation. The kernel must not guess
global task intent, and the experiment must not coach the answer through a
hard-coded tool name.

## Architectural smell exposed by the experiment

Changing a public tool name should be one edge-level data change. It was not.
The production vocabulary appears in registry names and schemas, handler result
fields, human summaries, refusal remedies, and `next_call.tool`. A projected
catalog key therefore created a contradictory interface: the model entered
through the candidate name and recovered through the legacy name.

The desired boundary is a public interface facade:

```text
model
  -> public role/name/schema adapter
  -> canonical semantic operation
  -> typed semantic outcome
  -> public result/next-call renderer
  -> model
```

Below that facade, code should use semantic roles such as `:edit`, `:extract`,
`:plan`, `:transform-preview`, and `:transform-commit`. It should not contain
public tool identifiers. The facade alone maps roles to public names, titles,
schemas, summaries, and `next_call.tool`.

This does not authorize blind response rewriting. Results contain source,
diffs, diagnostics, verifier output, argv, hashes, receipts, and user text that
may legitimately include a tool-like string. The facade may project only typed
public identifier slots and exact server-owned routing templates. It must
preserve evidence and `terminal_response` byte-for-byte.

The isolated catalog server is the experimental form of this facade. Do not
refactor the production runtime before the naming comparison, because changing
both mechanism and vocabulary would confound the experiment. If a candidate
earns publication, promote the facade as a separate product ratchet and add a
permanent no-public-name-below-the-boundary test.

### Why Kent Beck would approve—and where he would object

The facade earns its keep by making the next real experiment cheaper. Today a
rename requires coordinated edits to the registry, schemas, summaries,
remedies, and next calls, followed by leak archaeology. With the facade, a
rename should be one role-to-name map change plus interface tests.

The safe sequence is:

1. characterize catalog A through the canonical handler;
2. insert the facade and require byte-identical A responses;
3. represent operation and continuation identity as semantic roles;
4. move one server-owned routing template at a time behind the renderer;
5. forbid public tool identifiers below the boundary;
6. change only the role-to-name map for each candidate experiment.

The counterfactual matters: the facade is a failure if it becomes a large
repair layer that recursively rewrites arbitrary handler output. That would
hide the coupling, not remove it. Catalog A byte equality, a typed projection
whitelist, and byte preservation for source, diagnostics, diffs, receipts,
hashes, verifier evidence, and terminal relay text are the stop gates.

The first broad response-projector draft was 459 changed lines. It was rejected
before commit. The replacement is a surgical catalog hook plus a separate pure
typed adapter and focused boundary tests. This is the Kent Beck move: lower the
cost and risk of the experiment before running the experiment.

The option-value payoff is larger than renaming. With one frozen kernel and
scorer, the facade lets us vary names, tool count, schema partitions, effect
signals, descriptions, ordering, refusal detail, continuation routing, and
terminal presentation independently. Losing interfaces leave no production
machinery behind. The causal discipline is to change one public factor at a
time while freezing the handler commit, fixture, caller stratum, and semantic
acceptance gate. This is `(N × K × sigma) / t` expressed as an executable
architecture.

## What became cheaper

We can now project and test a public catalog without changing product handlers
or the shared runtime. A naming hypothesis can fail in one clean caller before
it becomes an API migration. That option value is the main win so far.
