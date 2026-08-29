# Captain's Log: The Schema Shrank, but the Decision Did Not

Date: 2026-08-29

## Decision

Do not promote the extraction-only `apply_clojure_changes` surface from this
experiment. It preserved exact first-call behavior, but it did not produce a
repeatable speed improvement.

The treatment removed 63.7% of the client-visible target tool surface: 23,096
bytes became 8,390 bytes. All four Sol/high calls were exact, one-shot, and
free of shell, file-tool, discovery, refusal, and recovery actions. However,
the treatment won the first pair and lost the second. Its complete-wall
midpoint improved by only 5.2%, below the predeclared 10% gate, while its
pre-first-call midpoint was 5.2% slower.

## Frozen Cohort

Schedule: control, treatment, treatment, control. Each run used a fresh Codex
home, empty read-only workspace, no-effect MCP server, and the complete
production tool catalog. Only the description and input schema of
`apply_clojure_changes` varied.

| Run | Arm | Pre-first-call | Complete wall | Correct |
|---|---|---:|---:|---:|
| 01 | control | 12.897s | 18.253s | yes |
| 02 | treatment | 11.567s | 14.207s | yes |
| 03 | treatment | 19.911s | 23.299s | yes |
| 04 | control | 17.022s | 21.319s | yes |

| Aggregate | Control | Treatment | Treatment change | Gate |
|---|---:|---:|---:|---:|
| Pre-first-call midpoint | 14.960s | 15.739s | 5.2% slower | at least 20% faster |
| Complete-wall midpoint | 19.786s | 18.753s | 5.2% faster | at least 10% faster |

Pair one favored treatment by 10.3% before the call and 22.2% complete wall.
Pair two favored control: treatment was 17.0% slower before the call and 9.3%
slower complete wall. The treatment therefore did not win both pairs.

## What We Learned

The large visible-schema reduction was real, but schema bytes were not the
dominant cause of this decision's materialization interval. The model still
had to understand the same 15-form extraction and emit the same 481-byte
root-normalized request. At this sample size, service/model variance is larger
than any reliable benefit from hiding unrelated `apply_clojure_changes`
operations.

This is a useful stop result:

- Keep the complete public operation for now.
- Do not claim that a smaller schema makes this route faster.
- Do not rerun or tune the same hypothesis merely because pair one looked
  excellent.
- Favor changes that remove a decision, action, or narration boundary. Those
  mechanisms have produced the durable multi-x gains in the retained record.

## The Measuring Instrument Was a Win

Two token-free NO-GOs prevented an invalid cohort:

1. Candidate `d47aeb9` compared server/client catalogs positionally and
   serialized the logical request as pretty JSON, which guaranteed a false
   score.
2. Candidate `d90dc1c` did not bind projected tool order/cardinality and let
   symlink or regular-file output roots bypass the stale-artifact gate.

Each falsifier became a permanent witness. The final candidate refuses moved,
duplicate, omitted, or reordered client tools; wrong or dirty executable
identity; wrong workspace roots; nonempty, regular-file, or symlink output
roots; preambles; extra actions; noncanonical requests; and false mutation
language. The final truthful response is exactly `Captured.`

The instrument made an unexciting negative result trustworthy. That is more
valuable than a dramatic but self-consistent false win.

## Immutable Evidence

- Candidate: `05a3b049e5372b268de44868da8e8515ea623ac3`
- Candidate tree: `be9678753767d3d4d4f7ac1a48cc1dd8a124d67c`
- Independent GO receipt: `051bc29a0d13207973edde1236f69675403cfd1a`
- Cohort report SHA-256:
  `c7a0de8093a3d163d12ffb294a9c9aaabc1e147a3bad8951cd43b393f9d5530d`
- Runs table SHA-256:
  `a43dc75853b2e682e5b36b2bd88a9bb183526bfce1cb2b953bc86015e214cf1b`
- Archive:
  `/Users/genekim/src.local/clj-surgeon-bench-archive/2026-08-29/extraction-tool-surface-05a3b04-cttc.tar.gz`
- Archive SHA-256:
  `3c241f59577c04d02f042ed536ba6f5c7ea498e01f193cabc6f6faf38b99d9b5`

The archive excludes Codex authentication links. It contains all four attempts,
surface receipts, event clocks, exact arguments, scores, and the cohort report.
No model attempt was retried.
