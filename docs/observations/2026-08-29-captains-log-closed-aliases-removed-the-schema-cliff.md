# Captain's log: closed aliases removed the schema cliff

Date: 2026-08-29

Status: frozen serial `A B B A` cohort complete; raw evidence retained; no
retry, install, reload, or shared-runtime action.

## Question

Can one closed edit-value vocabulary remove the expensive `old/new` and
`before/after` schema misses observed on the frozen 51-edit, nine-file
`submission-row-extraction-cleanup` task?

Arm A exposed only canonical `from/to`. Arm B exposed exactly three complete,
mutually exclusive pairs and identified `from/to` as canonical:

```text
from / to        canonical
old / new        exact alias
before / after   exact alias
```

The task, prompt, scorer, Sol/high model, edit-only client surface, fixture,
controller, seat, and serial schedule were frozen. The complete admission law
and artifact identities are recorded in
`2026-08-29-compact-field-alias-anvil-launch-prep.md`.

## Result

| Run | Arm | First pair | Calls | Server time | Wall | Exact | Meaning | First-call success |
|---|---|---|---:|---:|---:|---:|---:|---:|
| 01-A | canonical-only control | `old/new`, then `from/to` | 2 | 8.483 ms refused + 1,183.389 ms committed | 93.504 s | yes | yes | no |
| 02-B | closed aliases | `from/to` | 1 | 1,222.245 ms committed | 59.277 s | yes | yes | yes |
| 03-B | closed aliases | `from/to` | 1 | 1,126.683 ms committed | 63.595 s | yes | yes | yes |
| 04-A | canonical-only control | `old/new`, then `before/after` | 2 | 7.271 ms refused + 3.336 ms refused | 87.800 s | no | no | no |

Arm B was 2/2 exact, 2/2 meaning-preserving, and 2/2 one-shot. Each run used
one successful `edit_clojure` call, one atomic 51-edit transaction, zero shell
or source commands, zero discovery, zero refusal, and zero follow-up action.

Arm A was 1/2 exact and 0/2 one-shot. Run 01 recovered after one safe refusal.
Run 04 exhausted its permitted correction with another invalid pair and left
the complete source snapshot byte-identical. Every refusal was pre-write.

The Arm B wall midpoint was **61.436 seconds**. A valid same-cohort control
median does not exist because one of two control tasks did not complete.
Compared with the one successful control run, Arm B was 32.068 seconds faster,
34.3 percent lower wall time, or 1.52 times as fast. Compared with the prior
frozen two-exact-control midpoint of 95.558 seconds, Arm B was 34.122 seconds
faster, 35.7 percent lower, or 1.56 times as fast. The latter is a historical
matched baseline, not a simultaneous-arm estimate.

## Native comparison

The closest retained correct-native midpoint for this exact workload is
346.912 seconds. Against that denominator, Arm B's 61.436-second midpoint is
**5.65 times faster**.

An earlier, more conservative retained native midpoint is 202.127 seconds.
Against it, Arm B is still **3.29 times faster**.

Both denominators are reported because the structural result does not depend
on selecting the more favorable native cohort.

## What caused the win

The server did not become materially faster. Successful atomic transactions
took 1.127 and 1.222 seconds in Arm B, versus 1.183 seconds in the successful
control. The treatment removed a model-sized retry.

```text
Control 01

  materialize old/new payload       44.382 s
  safe server refusal                0.008 s
  interpret refusal                  3.947 s
  materialize from/to payload       32.898 s
  atomic transaction                 1.183 s

Candidate 02 / 03

  materialize canonical payload     42.214 / 40.025 s
  atomic transaction                 1.222 / 1.127 s
  done

Control 04

  materialize old/new payload       36.730 s
  safe server refusal                0.007 s
  interpret refusal                  9.776 s
  materialize before/after payload  29.787 s
  safe server refusal                0.003 s
  incomplete
```

The important causal boundary is call construction, not mutation execution.
A millisecond refusal costs tens of seconds because the model must materialize
another 51-edit request.

## A surprising mechanism detail

Neither candidate caller used an alias. Both selected canonical `from/to` on
the first call. Therefore this cohort proves the **closed alias contract as a
client-visible affordance** eliminated the live schema cliff; it does not by
itself prove that runtime alias lowering was the exercised success path.

This is still a useful result. The control schema required `from/to`, yet both
control callers ignored it. The candidate schema made the whole synonym set
explicit, made the three pairs mutually exclusive, and labeled `from/to` as
canonical. Fresh Sol/high callers then chose the canonical pair 2/2. The
runtime lowering remains permanent protection for a caller that chooses one
of the other two admitted pairs, but that fallback needs a separate direct
exercise if its field evidence is a release question.

The client-visible edit-item schema hashes were:

- Arm A: `048368f7ac8fb887da201d0c679690ea62d2e94c2ae1d7f0f01aca718c550c0c`;
- Arm B: `3888cfe8a5fc61d149e997099fd62f2d4e6cc020ec6661252641d86ab6e1ab88`.

## Immutable evidence

Product identities:

- Arm A product `4904d4ea52c1e1330bc9f8a04c8a5e393af9a758`, tree
  `c47f1cce0b28696acebd29d0e66973621ea79e6e`;
- Arm B product `ce05f6ee099ac029d96ecb6db6f5f225e4239b96`, tree
  `ecec5aebfa0f8adb6d76eeadaf3113ff8aeb7b3d`.

Prepared package:

- prep head `ac6da550dacf66ef8ccc8caf2cfe7f3eea880b83`;
- bundle SHA-256
  `88f6731c02172fd9e38109c8a12aed046a4e9f2bc19e6a15ecfbd0caede26db2`;
- controller SHA-256
  `eef5376c8b06e90b7480b59aa5bd690d6e0a3a3cf64252cb5b5c7f45c81a8829`;
- verifier SHA-256
  `513a4659f1134d91f93e06f942ad7cab7d569bed06b7c1566acc36782c5852c0`.

Raw archive:

`/Users/genekim/src.local/clj-surgeon-bench-archive/2026-08-29/clj-surgeon-field-alias-ce05-20260829T071022Z.tar.gz`

Archive SHA-256:

`e41da53fd2b973d3545f4608365416d40c20e24a8f865edccec161699563972f`

The local archive hash equals the remote archive hash. The first interrupted
local transfer remains beside it with suffix `.partial-9978840f` and is not
evidence authority.

Selected internal evidence hashes:

- admission TSV: `ec505221441932d80e45b1c5293a1bdf1b57aad72ec4ac4403246d0603a4797c`;
- attempts TSV: `01fe45ea667f5a49e480b6e6e9afa0eeb690d4a84a88da155c35dd75607e3702`;
- raw manifest file: `bc5741cb9f9adc54ed28b71b8e92c14ca0d55de94248d6927bd550a7609e537b`;
- controller log: `9e67ec2e25fd8a4b0f108415a29c899d07ad57c9755947c449be07e0ffc49aea`;
- exact-run scorer input: `a9eee14ff011d36fd6da201fb6517fc76aac9d9286950eb009666182009ceb22`.

The identical scorer-input hash for all three successful runs proves the
completed semantic target was the same despite different caller routes.

## Verdict

Promote the closed alias contract as the winner of this bounded hill. Its
strongest evidence is not a faster server; it is elimination of a repeated,
expensive request-construction failure. Keep the runtime injective and
fail-closed: one complete pair lowers, while partial, mixed, or multiple pairs
must still refuse before source read or mutation.

Do not overgeneralize from two candidate callers. The next useful evidence is
ordinary production dogfooding or a larger historical portfolio that measures
how often callers choose canonical fields, aliases, or invalid mixtures. Do
not spend another four-run cohort merely to force an alias if the permanent
direct tests already prove exact lowering.
