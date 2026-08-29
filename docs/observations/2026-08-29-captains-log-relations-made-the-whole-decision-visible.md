# Captain's Log: Relations May Make a Complete Decision Cheaper to State

**Date:** 2026-08-29  
**Bead:** `clj-surgeon-45j`  
**Experiment candidate:** `6328db51557bc39ef1a0d40ca171a1ac9873005a`  
**Retained archive:** `clj-surgeon-bench-archive/2026-08-29/6328db5-cohort-20260829T0851Z.tar.gz`  
**Archive SHA-256:** `1af9110d6bbdbe369cdcdf7feee0f70bac78b0f25717a24d937dfe603ecc9d2c`

## The result changed when we tested the scorer

The first analysis said the relation arm was 2/2 exact while flat and grouped
arms were 0/2. That was wrong. The models had supplied the complete decision;
the capture-only scorer had omitted a production normalization stage.

All four non-relation callers constructed all 33 edit rows and 14 owner
deletions. Three placed each exact namespace name in `within.form`; one placed a
typed namespace-owner object there. The historical scorer sent those requests
directly to the generic transaction compiler. Current production first runs
source-proved compact-location normalization, which safely lowers an exact
namespace name to namespace ownership when it is unique.

Replaying the immutable calls through the current product path corrected the
record:

| Arm | Historical score | Product-equivalent exact | Prompt-to-call midpoint | Capture wall midpoint | Payload |
|---|---:|---:|---:|---:|---:|
| F: flat rows | 0/2 | 2/2 | 65.841 s | 68.500 s | 6,470 B |
| A: file groups | 0/2 | 1/2 | 83.703 s | 87.000 s | 5,666/5,918 B |
| B: closed relations | 2/2 | 2/2 | 48.912 s | 51.500 s | 2,715 B |

Every successful product-equivalent replay compiled 51 matches across 9 files
to all nine frozen future hashes. A's other call remains a real public-schema
failure. No replay mutated source.

## The harness also taught the wrong interface

At the same candidate commit, production `edit_clojure` explicitly taught:

```text
within {form}
within {namespace:true} for the unique ns form
within {namespace:name} for an exact namespace
```

The experimental surface replaced that with the generic change-tool
description. Its first compact instruction said every edit contained
`within {form}`. Later prose mixed in a typed namespace-owner spelling from the
direct-change language. The nested schema was correct, but the high-salience
description was misleading. That explains the four callers' consistent choice
of the `form` property and makes the old correctness contrast doubly invalid.

The future harness must preserve the production description and prove the
client-visible registry surface before spending model tokens.

## What remains genuinely exciting

The corrected comparison is stronger for the materialization hypothesis. F and
B are both 2/2 product-equivalent exact, yet B reached the complete first call
16.929 seconds sooner at the midpoint—25.7 percent—and reduced capture wall by
17.0 seconds, or 24.8 percent. It represented the same canonical transaction in
2,715 bytes rather than 6,470.

```text
Normalized flat control                       Closed relation treatment

33 literal edit rows                          require_change
14 owner deletions                             symbol_migration
~6.0-6.5 KB                          versus   1 bespoke edit
                                                14 owner deletions
                                                2.7 KB
        |                                             |
        +-- compact-location normalization <----------+
        +-- same generic transaction compiler
        +-- same 51 matches / 9 files / future hashes
```

The relation did not make an incorrect decision correct. It may have made an
already-correct decision materially cheaper for the model to state. That is the
only causal hypothesis worth carrying forward.

## Claim boundary

This was still a capture-only `N=2` screen. The arms exposed different schemas,
the harness did not mutate files, and no exact verifier ran. Therefore:

- **GO** to LLD and EARS design after HLD approval, then to red tests,
  implementation, verification, and the real mutation screen only after each
  Linked-Intent gate passes;
- **NO-GO** to claim a product speedup or a new Surgeon-versus-native multiple;
- **NO-GO** to claim relations are needed for correctness;
- **NO-GO** to merge the experimental compiler wholesale; and
- **STOP** generic file grouping unless new evidence earns it.

## Cheapest decisive experiment

One immutable candidate exposes the production surface to every run.

```text
N = normalized flat request
R = closed relation -> ordinary edits -> same normalizer

Block 1: N R R N
Block 2, only if block 1 is exact and >=15% faster: R N N R
```

Before model launch, both oracles must pass the public schema and compile to the
same canonical transaction, 51 matches, 9 files, future hashes, exact verifier,
and terminal-response contract. Every run must perform one real
`edit_clojure` mutation and exact verification. Promotion at `N=8` requires 4/4
exact per arm, R faster in both blocks, and at least 20 percent lower complete
verified midpoint or median.

## Method lesson

The most valuable result was not the attractive 25.7 percent number. It was
catching our own false denominator before building a product around it.
SURGEON1 replayed the raw calls through production. Independent adversaries
identified both the skipped normalizer and the non-production description.
The relation architecture survived, but its reason for existence became much
narrower and more falsifiable.

That is the useful form of `(N * K * sigma) / t`: parallel review does not only
find more wins. It prevents us from manufacturing them.

## The measuring instrument was repaired

The reusable owner-aware screen now inherits `mcp-tool/edit-tool-description`
and the production editor schema for both arms. Its subject and oracle both run
through runtime admission, `tool-params->transaction`, source-proved
compact-location normalization, and the generic transaction compiler. The
capture-server test independently compares its control against the real public
tool registry, while the client-surface observer explicitly rejects regression
to the generic change-tool description.

The bounded zero-model suite is green:

```text
owner-aware prerequisite             3 tests / 15 assertions
call-construction scorer             4 tests / 30 assertions
capture-server projection            2 tests / 15 assertions
Codex registry surface observer      4 tests / 16 assertions
```

These repairs prevent the next cohort from reproducing either historical
confound. They do not retroactively change clock provenance: the retained
65.841-second and 48.912-second midpoints remain bound to the original
arm-specific surfaces and are descriptive only.
