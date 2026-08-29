# Owner-aware call construction: prerequisite-corrected screen

Date: 2026-08-29

Durable owner: `clj-surgeon-45j`

Status: zero-model and client-surface gates passed; GO only for one fresh
Sol/high capture-only run per arm after explicit approval. No model run,
product integration, install, reload, shared-port call, or source mutation has
occurred.

## Why this is a new hypothesis

The first owner-aware experiment remains rejected evidence. Its control and
candidate both scored 0/4 because callers selected the namespace owner
incorrectly or omitted namespace scope. The candidate's 60.595-second median
was 11.72 percent below the control's 68.639 seconds, but neither arm compiled
the frozen future. This screen does not repair or rescore those runs.

Two independently earned product prerequisites have changed since then:

1. compact locations now normalize the retained namespace spellings
   injectively; and
2. the closed edit-field algebra accepts exactly one of `from`/`to`,
   `old`/`new`, or `before`/`after` and refuses partial or mixed pairs.

The bounded question is now legitimate: with the old shared failure removed,
does the smaller owner-aware request reduce fresh model call-construction time?

## Immutable identity

- Product base: `ce05f6ee099ac029d96ecb6db6f5f225e4239b96`
- Product tree: `ecec5aebfa0f8adb6d76eeadaf3113ff8aeb7b3d`
- Screen code commit: `bf985716b0d75c2741dc2bf1e7d3b848fdabc866`
- Screen code tree: `a9ee6497a1aefd373f29b0fd0e38360a5b8fe0d3`
- Product source/test/design diff SHA-256: `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`
- Harness SHA-256: `bf8b288fc28c4367c779229a747ff36677c0a64f45ca5e2c6343e3b112be05be`
- Scorer SHA-256: `15debe3e08a20cad7543afb19dea84b33861cc4c258c1960392b21d1881a2517`
- Observer SHA-256: `a3b8fc5b8259f14fb5ea0f306c24a013b78e9f8a1989adb1eee1a62c9160bc18`
- Frozen task SHA-256: `789809060a52d647197cf1fb5ade2cc0a76992209a0223991c7a51179f44d8e1`
- Frozen capsule SHA-256: `7d985f4d30acdf871f615b174e0f6c37338539253e6591cf898f96c26f39d4b9`

The only changes from the product base are under `bench/`, `dev/experiments/`,
and this observation. The screen's capture server records calls and elapsed
time. It cannot read or mutate the fixture workspace.

## Zero-model result

The scorer replayed all eight retained real calls through the existing
`symbol_migration` lowering, current field validator, current compact-location
normalizer, and real transaction compiler.

| Public field pair | Exact frozen futures |
|---|---:|
| `from` / `to` | 8/8 |
| `old` / `new` | 8/8 |
| `before` / `after` | 8/8 |

All 24 compiled futures contained exactly 51 matches in 9 files and reproduced
the nine expected final file hashes. Every candidate lowering preserved the
exact ordered 23 owner rows and 27 declared symbol matches. Partial pairs,
mixed pairs, a canonical-plus-alias pair, and two simultaneous alias pairs all
refused before write authority.

The complete local gate passed 13 logical tests and 67 assertions with zero
failures or errors. It made zero model calls and performed zero mutation
actions.

## Client-visible parity

A real Codex app-server registry preflight projected each capture-only MCP
surface. Both arms exposed exactly one callable tool named `edit_clojure`.
Both projections matched the advertised schema after the same two known SDK
normalizations. The preflight stopped before any prompt or model path.

- Preflight summary SHA-256: `a447892aa5d93b670cfeee76753ace62b20d67974a3f765593bc0e9f6b5ddc5d`
- Run-config SHA-256: `7a0442f15e1bad53dde643086c26cc4703242f9b1a1effbe05561c5a07976898`
- Prerequisite report SHA-256: `8116ac8f6820dae1c3b9c6a910d5d04046447ba1aa6eb568ce5bd477670735ce`
- Raw evidence archive: `/Users/genekim/src.local/clj-surgeon-bench-archive/2026-08-29/owner-aware-prereq-bf98571-20260829T075616Z.tar.gz`
- Archive SHA-256: `e14fcf6317ba8b49257ecfd57f035b5f9da020b892f26d994c7093fed77b8040`

The pilot declaration now records the actual order `[control, candidate]`.
Permanent self-test assertions retain both that order and the original
eight-run cohort order. This prevents a two-run pilot from claiming an
eight-run controller identity.

## Bytes and decision geometry

| Arm | Model-visible tool surface | Expected emitted arguments |
|---|---:|---:|
| Current flat control | 8,442 bytes | 6,353 bytes |
| Owner-aware candidate | 9,778 bytes | 4,347 bytes |
| Difference | +1,336 bytes | -2,006 bytes |

The candidate adds 1,336 bytes of up-front schema and description, then removes
2,006 bytes from the call. The model experiment is necessary because bytes
alone cannot tell whether the extra vocabulary reduces or increases decision
time.

## Relationship to the request-shape portfolio

The revived candidate is not a duplicate of Treatment A. It is a narrower,
more semantic notation.

| Shape | Scope | Payload | Relationship |
|---|---|---:|---|
| Treatment A: `file_groups` | Groups every generic edit by file | 5,189 bytes | More general and simpler; owner-aware saves another 842 bytes for this preserve-name migration. |
| Revived `symbol_migration` | One target alias plus exact file, owner, source symbol, and count rows | 4,347 bytes | Specializes the 23 symbol rewrites; retains all 9 literal namespace edits. |
| Treatment B: closed relations plus require delta | Compiles both symbol relations and namespace require changes | 3,600 bytes | Strictly broader on this fixture and 747 bytes smaller; owner-aware overlaps its symbol-relation domain but lacks its require compiler. |

Treatment A and the owner-aware candidate can be compared as alternative call
shapes. Treatment B subsumes much of the candidate's intended effect. Shipping
both relation syntaxes would create redundant authority paths unless separate
model evidence proves each has a distinct task-shaped crossover. Therefore
this pilot is evidence gathering, not a recommendation to add
`symbol_migration` to the product.

## Verdict and next gate

**GO** for the smallest requested screen only:

```text
run 1  current flat control       fresh Sol/high, capture only
run 2  owner-aware candidate     fresh Sol/high, capture only
```

Freeze commit, task, prompt, scorer, one-tool surface, and order. Score exact
first-call compilation, one-action geometry, emitted bytes, prompt-to-call
time, and capture-server time. A post-token harness failure consumes the pilot;
do not retry or expand it into a cohort.

This is **NO-GO** for product integration. A single pair can cheaply reveal
whether the prerequisite correction makes the old signal worth a proper
counterbalanced cohort. It cannot establish a general performance win.

Machine-readable evidence is in
`docs/observations/evidence/owner-aware-prerequisite-bf98571.edn`.
