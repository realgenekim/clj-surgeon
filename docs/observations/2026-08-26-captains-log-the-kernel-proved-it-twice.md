# Captain's Log: The Kernel Proved It Twice

## Outcome

The first internal extraction compiler worked exactly as designed at the route
boundary: a fresh Sol/high caller completed the historical 15-form Sessionize
extraction with one `apply_clojure_changes` call and one exact `clj-kondo`
command. It made no inspection call, source read, native edit, failed mutation,
or retry.

It was also slower than it should have been. The first transaction spent
22.079 seconds inside the MCP kernel and completed in 53.254 seconds. The route
was right; the implementation repeated its most expensive proof.

## What happened

The extraction compiler needs two facts at once:

1. which selected private forms are still called by the remaining source; and
2. the future source with exactly those forms made public.

The first implementation called the pure `extract/compile-plan` once to learn
the required visibility and again to produce the promoted future source. A
warm nREPL probe on the exact 4,594-line fixture measured the duplicate work:

```text
discovery compile       10.836 s
visibility recompile    12.764 s
                        --------
duplicate pair          23.600 s
```

This was not model latency, MCP transport, JSON, the formatter, or a 512 MiB
heap problem. It was our code proving the same frozen extraction twice.

## The change

The pure planner now has one internal option,
`derive-required-public-forms`. During its original pass it computes required
visibility, treats exactly that proven set as the requested public set, and
constructs the promoted target source. Explicit `public_forms` remains a
separate authoritative path: an explicit empty vector still refuses when a
private moved form must become public.

`mcp-extraction/compile-extraction` now invokes the planner exactly once. A
permanent witness counts calls and fails if omission ever reintroduces a second
compile.

The safety boundary did not move:

- the same frozen workspace snapshot supplies every fact;
- only mechanically required visibility is derived;
- discovered external callers still require an explicit change or ignore
  decision;
- genuine unknowns still refuse before any formatter, verifier, receipt, file,
  or directory effect;
- supplied expectations and visibility remain authoritative guards.

## Measured result

The exact-fixture warm nREPL time fell to 8.069 seconds. Three fresh local
Sol/high callers then exercised the installed-shaped no-skill route.

| Route | Correct | Median complete wall | Median actions | Median kernel |
|---|---:|---:|---:|---:|
| Fully supplied direct extraction | 2/2 | 37.871 s | 2 | not retained here |
| Public plan then apply | 2/2 | 49.941 s | 3 | split across two calls |
| Internal compiler, single pass | 3/3 | 47.240 s | 2 | 10.515 s |
| Native control, closed line | 2/2 | 122.278 s | 5.5 | n/a |

The three internal-compiler complete walls were 46.482, 47.240, and 49.582
seconds. Every run used exactly one apply and one lint, with zero discovery,
refusals, native edits, or post-mutation reads.

Relative to the first double-compile trial, the representative tool kernel
fell from 22.079 to 10.515 seconds: 52.4% lower. Complete wall fell from 53.254
to 46.482 seconds in the paired first-after comparison. Relative to the prior
public plan/apply median, the three-run internal median is 2.701 seconds, or
5.4%, lower and removes one entire model/tool phase.

This first local cohort established the route but did not isolate a stable
residual: it trailed the earlier fully supplied median by 9.369 seconds across
different runs. Because both routes now have two actions and one structural
compile, an independent matched acceptance run was required before treating
that difference as product cost.

## Independent adversarial acceptance

SURGEON2 tested the exact released commit `65e72b7` from a detached, clean
worktree. No SURGEON2 code was integrated or published.

| Acceptance arm | Correct | Complete wall | Kernel | Route |
|---|---:|---:|---:|---|
| Fully supplied decision | yes | 39.150 s | 11.121 s | one apply + one lint |
| Omitted mechanical facts | yes | 37.500 s | 8.297 s | one apply + one lint |

Both success arms had zero discovery, refusals, failures, or native edits. In
the matched environment, omission was 1.650 seconds faster, not slower, than
the supplied route. The earlier 9.369-second cross-cohort gap was therefore not
evidence of inherent internal-compiler overhead.

The two refusal boundaries also passed:

- an external caller with a destination-alias collision returned
  `extraction-decisions-required` in 110.219 ms, with a completed plan, exact
  hashed genuine unknown, no write authority, unchanged source, and no target,
  receipt, formatter, or verifier effect;
- a stale `source_hash` returned `source-hash-mismatch` in 3.804 ms, before all
  effects, with byte-identical source and no target or receipt.

The stale refusal does not yet publish explicit `mutation_attempted=false` and
`write_authority=false`, although its effects prove both. Bead
`clj-surgeon-6l9` owns common evidence normalization. This evidence-only defect
does not weaken the refusal and does not block the release.

Immutable success receipt:
`/tmp/clj-surgeon-surgeon2-65e72b7-success-arms-r2`; `runs.tsv` SHA-256
`afa2abcecc7579bf8794620fb2349adcf8af3aa9a7ba98b143b69c7a3e6bd1a8`;
summary SHA-256
`f5372b7ed71c1668e209c1713f7d101ccee648ea1e0fc239506ce7a44a9eeb19`.

## Release receipt

Stable release commit: `65e72b7010b380facbad1bc2fa30a17eb552804e`.

- core: 614 tests / 5,279 assertions, zero failures;
- MCP: 235 tests / 1,951 assertions, zero failures;
- formatter and clj-kondo: clean on changed Clojure files;
- 512 MiB heap, cclsp, stdio, benchmark, portfolio, retention, and evidence
  gates: green;
- stable CLI, Codex skill, and Claude skill installed from the exact commit;
- global Codex and Claude routing synchronized;
- shared MCP hot-reloaded in place with no server restart.

## Next hill

Instrument the winning two-action route as four server/model intervals:

```text
prompt delivered
    -> first apply begins
        -> planning complete
            -> formatting/commit complete
                -> final lint begins and ends
```

The matched acceptance closes HILL-3: no residual product regression remains
to explain before accepting the mechanism. Interval telemetry is still the
right next optimization instrument, but it should seek an absolute two-action
speedup rather than defend omission parity. Do not restore a public plan,
broaden correctness scoring, or optimize native. The product goal remains one
coherent decision, one guarded mutation, one proportional verifier.
