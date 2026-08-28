# Captain's Log: Tool Names Enter the Arena

**Date:** 2026-08-27  
**Owner:** `clj-surgeon-x9d`  
**Experiment head:** `a953e418cf06c458199c9bda670af1c2b5d8157f`
**Status:** clean alternate-universe screen complete; keep catalog A

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

## Decision after the contaminated falsifier

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

## Clean alternate-universe screen

Commit `a953e41` closed the response-side leak. The candidate facade projects
only typed public identifier slots and exact server-owned routing phrases. It
preserves source, diffs, diagnostics, hashes, receipts, verifier evidence, and
terminal response bytes. Catalog A is characterized byte-for-byte against the
canonical handler. The focused cold gate passed 22 tests and 319 assertions.

The clean Anvil screen then ran all 11 supported catalogs. Each run used:

- a fresh Codex home with no installed Surgeon CLI or skill;
- `gpt-5.6-sol` at high reasoning;
- exact commit `a953e418cf06c458199c9bda670af1c2b5d8157f`;
- frozen input manifest
  `aa637f4e9bbc949645007d4859510523a04de4981b251043df2039e0083c4d12`;
- the same extraction task, fixture, prompt, scorer, and private MCP launch;
- one replica, serial execution within each lane, and zero source archaeology;
- no shared `:7888`, install, reload, or product-runtime change.

Correctness precedes speed in this table. `Semantic` says whether the resulting
source meaning survived. `Route` says whether the catalog led the caller through
the intended public one-shot extraction path. A semantically correct fallback
is still a catalog failure when it requires discovery, refusals, or raw MCP.

| Catalog | Extraction-facing name | First role | Observed route | Semantic | Route correct | Wall |
|---|---|---|---|---|---|---:|
| A | `apply_clojure_changes` | extract | one public extraction transaction | correct | **yes** | **45.600s** |
| L | `apply_clojure_plan` | edit | compact edit route | correct | no | 68.965s |
| C | `refactor_clojure` | inspect | inspect-led, three MCP failures | correct | no | 141.374s |
| M | `extract_clojure` | edit | compact edit route | correct | no | 117.048s |
| N | `extract_clojure` | edit | compact edit twice; one MCP failure | correct | no | 99.037s |
| O | `extract_clojure` | inspect | inspect twice; no destination | no mutation | no | 102.784s |
| P | `commit_clojure_extraction` | inspect | inspect twice, raw `tools/list`, then raw commit | correct | no | 162.240s |
| Q | `clojure.extract.commit` | inspect | refused unknown fields; required name unavailable | no mutation | no | 87.951s |
| R | `extract_clojure_commit` | inspect | refusal, MCP discovery, raw commit | correct | no | 169.374s |
| S | `extract_clojure_bang` | inspect | two refusals, MCP discovery, raw commit | correct | no | 165.448s |
| T | `move_clojure_owners` | inspect | three refusals, read-only plan, no mutation | no mutation | no | 143.744s |

Catalog A is the only benchmark-correct arm. It selected the extraction role
first, performed one atomic mutation, and completed without discovery or MCP
failure. Its byte-exact score was false, but the semantic scorer was correct;
byte equality is secondary for this benchmark and did not change meaning.

R and S are useful negative evidence. Their kernels could perform the task, and
their final source was semantically correct. Their public interfaces did not
make that capability usable: Sol first selected inspect, rejected the normal
route, rediscovered the private MCP protocol, and invoked the extraction tool
raw. They took 3.71x and 3.63x A's wall time. This is not a fair speed contest
between valid arms; it is the measured cost of a failed catalog route.

### Honest interpretation

This screen does not prove that the string `apply_clojure_changes` is the best
possible name in isolation. A catalog is a coupled public contract: name,
description, schema partition, order, title, effect annotations, refusal
language, and result projection. The experiment proves that the complete A
contract works and that none of the ten alternate contracts earned a release
cohort.

The surprising result is valuable. Names that looked clearer in a design
review—`extract_clojure`, `_commit`, `_bang`, and `move_clojure_owners`—did not
produce clearer behavior. Fresh Sol often interpreted the supplied extraction
payload as an edit or inspect request. When it recovered, it paid several model
and tool boundaries to do so. Conceptual elegance did not beat the established
route.

The screen is one replica per catalog. That is sufficient for the elimination
gate because every challenger failed route correctness. It is not sufficient
to estimate small performance differences among correct catalogs. No
challenger therefore advances to a replicated speed cohort.

Two environmental facts remain separate from the catalog verdict:

- the requested `~/bin/clj-kondo` was absent on the Anvil seats. Catalog A's
  atomic mutation verification still succeeded; several failed routes later
  found an equivalent linter. Future performance cohorts should provide one
  candidate-pinned verifier path;
- dev-b experienced a repaired composer refusal and had an old dormant Java
  process during part of the lane. This can confound M/N/O/P wall time, but it
  cannot turn their edit- or inspect-first selections into extraction-first
  routes.

### Decision

Keep catalog A. Do not publish any rename, `_bang`, `_commit`, dotted name, or
expanded extraction catalog from this screen. Do not spend a larger cohort on
a challenger that failed its first-action gate.

Keep the facade architecture. It earned its keep independently of the losing
names: we can now test names, descriptions, schema partitions, ordering, and
result presentation without modifying handlers or the shared runtime. The
next product ratchet is a behavior-neutral public-interface boundary with
catalog A byte parity and a guard that forbids public names below the boundary.
That change should make future interface experiments cheap; it should not
change the installed catalog by itself.

### Immutable evidence

| Lane | Catalogs | Result root | Artifact manifests |
|---|---|---|---|
| dev-a | A, L, C | `/srv/fleet/dev-a/clj-surgeon-catalog-results-clean-a953e418cf06-alc` | A `2b2670708045d22b3251a7e8ab76fae2f9ebebd672e602b27f5b2a83c3cbaefe`; L `c355b06e7e12e05968cb882241e1ae22d70b30b5c195846e22e1f42cbbce2834`; C `ead70bd7a4e57b0416e2c6ff6d6c799bf02ef18b3b3d827f5a0a516806b03830` |
| dev-b | M, N, O, P | `/srv/fleet/dev-b/clj-surgeon-catalog-results-clean-a953e418cf06-mnop` | M `9ebcb2a5dcf591a04de84d8d8a0c1d882fee5d78a69e76d984fbc9de81cc21c1`; N `991b770f06f87441148563ad82b5b922133e206c06e2ebbeb5f9ff1ea7b37b86`; O `12ccdcfb412284e06b5726bc7aaa68efdbb12ac2f105ed7c94e10557f5a13835`; P `0058b40a8c5fe742625866e3d44e27e2a436919a0db4c2eda494401f0f963738` |
| dev-c | Q, R, S, T | `/srv/fleet/dev-c/clj-surgeon-catalog-results-clean-a953e418cf06-qrst` | Q `11b28adfc1876e4220367c493959b6bc422b72bdc4eaa2ec66beb990ce518197`; R `3797646ed4d4eac1111907130bf54c7175a64e8337462e45676380e688291f99`; S `a20798fffa75b6860175f989fddc79f46310729106fc6f9144556f07c2961d21`; T `1d79fc60cb47299cc257398398e2d53e86eefb01448f4d891ea91a765ba2ea4e` |

Every arm retained exactly one terminal receipt, one zero-archaeology receipt,
and a 26-file artifact manifest. Every lane finished at the exact experiment
head with no tracked or staged diff and no lane-owned benchmark or candidate
MCP process. The repaired bridge refusal was
`bridge-refusal-d9f8c9280f29`; its retry was picked up as
`agentmsg-08e79e925117`.
