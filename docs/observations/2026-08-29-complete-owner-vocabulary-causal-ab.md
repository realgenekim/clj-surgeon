# Complete owner vocabulary causal A/B

Date: 2026-08-29 PT  
Lane: SWEEP lane 1  
Status: **repair cohort complete; frozen causal screen passes**

## Question and source prior

Does a refusal that returns the complete owner vocabulary cause a fresh Sol
caller to skip a same-file recovery read before its next mutation attempt?

The retained observational source is commit `0ddfd1d` and
`docs/observations/2026-08-29-first-call-read-success-screen.md`. It counted
same-file rereads after 12 of 25 complete-list refusals (48.0%) and 134 of 166
truncated-list refusals (80.723%), an unadjusted 32.723 percentage-point split.
The source explicitly did not claim causality.

## Predictions frozen before launch

1. Arm C will reread in about 5 of 10 valid episodes; Arm T will reread in
   about 8 of 10. The predicted effect is a 30--35 percentage-point absolute
   reduction and a risk ratio near 0.60.
2. Arm C will require fewer recovery tool turns from the controlled refusal to
   the successful mutation. The median difference prediction is at least one
   tool turn.
3. Both arms will finish semantically correct in all valid episodes.
4. The wrong-subject rate will be exactly zero in both arms.

The primary mechanism is killed unless, among episodes valid on all three
declared dimensions, Arm C's reread rate is at least 30 percentage points lower
than Arm T's, Arm C's median recovery turns are no worse, and the wrong-subject
count is zero. Any wrong-subject mutation is reported loudly and independently
even if the primary metric passes. A passing N=10/arm result is a causal screen,
not a population-resolution claim.

## Resolution bound

The minimum cohort is 10 fully valid episodes per arm. Under independent
binomials with the observational priors `pC=0.48` and `pT=0.80723`, an exact
two-sided Fisher test at alpha 0.05 has only about 16.1% power. The corresponding
normal 95% half-width for the risk difference is about 39.5 percentage points.
This cohort cannot resolve a 30-point effect statistically. Failure to reject
zero will never be reported as equivalence; the report will show the effect
estimate, an unconditional exact or score interval, and this bound.

## Frozen experiment

### Caller and schedule

- Host/seat: Anvil `dev-a`, authenticated ChatGPT subscription route.
- Caller: fresh `codex exec --json --ephemeral` process per episode.
- Model: `gpt-5.6-sol`; reasoning effort `high`.
- Fixed interleaving: `C T T C` repeated five times. Invalid episodes remain in
  the chart; replacements continue in the same arm until each arm has 10
  episodes valid on all three dimensions.
- Every episode receives a fresh `CODEX_HOME`, workspace, fixture, and state.
  No install, shared MCP runtime, shared port, or live repository source is used.

### Real multi-owner fixture

Each workspace contains a parseable Clojure namespace at
`src/sample/views.clj` with 24 top-level `defn` owners. The frozen owner order is:

```text
render-home render-account render-profile render-settings render-help
render-login render-logout render-search render-results render-detail
render-cart render-checkout render-receipt render-history render-alerts
render-metrics render-admin render-dashboard render-activity render-feed
render-export render-import render-health render-version
```

Every owner returns a distinct literal map. Only `render-dashboard` contains
the target entry `:status :pending`; the required result is `:status :ready`.
The experiment controller records and fences the exact fixture, prompt, helper,
model, reasoning, schedule, and expected-output SHA-256 values before launch and
again before every episode.

### Controlled refusal and arms

The prompt requires the model to make its first mutation attempt, without a
source read, through the episode-local `./owner-edit dashboard` helper. That
declaration is deliberately invalid. The helper returns the same refusal in
both arms except for one field:

- **Arm C:** `available owners` contains all 24 names in source order.
- **Arm T:** `available owners` contains only the first 10 names and says that
  the list is truncated; this reproduces the historical shape and omits
  `render-dashboard`.

After refusal, the model may recover however it chooses, but `./owner-edit
OWNER` remains the only authorized mutation route. Supplying
`render-dashboard` performs the exact guarded replacement. Any other owner
refuses without writing. The helper cannot be modified.

The prompt, source, helper behavior, output ordering, and all result text are
identical across arms except the complete-versus-truncated owner vocabulary.

## Frozen scoring law

The controlled refusal is the completed first `./owner-edit dashboard` command.
The next mutation attempt is the next `./owner-edit` command or any direct file
mutation, whichever occurs first.

### Primary

`same_file_recovery_read=true` when, after the controlled refusal and before
the next mutation attempt, a completed tool action reads or searches
`src/sample/views.clj`. Reading the helper, listing directories, or reading
another file does not count. A command that both reads and mutates the fixture
counts as a read and a route violation.

### Secondary

`recovery_turns_to_success` is the number of completed model/tool boundaries
after the controlled refusal through the first successful fixture mutation,
inclusive. Report every value and arm medians; do not discard retries.

`wrong_subject=true` when any owner other than `render-dashboard` changes at
any point or when the final source differs from the exact expected bytes in any
other location. This safety outcome is never folded into the primary metric.

### Three independent validity dimensions

- `environment_valid`: exact model/reasoning and fenced inputs; recognized
  event stream; exit zero; exactly one controlled refusal; no timeout,
  transport failure, helper drift, or unknown actionable event type.
- `semantic_correct`: final fixture exactly equals the frozen expected bytes
  and the target mutation succeeded.
- `route_adherent`: no source read precedes the controlled refusal; all source
  mutations use the unchanged helper; first attempt is exactly `dashboard`;
  no direct patch or alternate writer is used.

All three booleans are shown separately for every episode. The primary
per-protocol estimate uses their conjunction. A second intention-to-treat chart
keeps every scheduled episode and every loss. Replacements never erase the
episode that caused them.

## Stop and retention law

After launch, no prompt, scorer, fixture, schedule, classification, or gate is
tuned. A failed or malformed position is retained and scored; only the minimum
valid-arm count may extend the fixed schedule. The whole cohort stops before
the next token if any fenced input or executable identity drifts.

The Anvil controller retains each raw JSONL event stream, stderr, prompt,
fixture before/after, helper log, per-episode score, manifest, and archive hash.
The final report records the remote path and copied-back archive hashes. It
does not advance the agent-usage window marker because this is a targeted
causal experiment rather than a history sweep.

## Execution result: typed refusal

Execution began from the preregistered commit
`21263688cea01eb4a1295fb56b0798a4babe1b1d` and tree
`d216eef5ba063914f63e230475338e7a48a3e609`. The frozen controller, scorer,
helper, prompt, 24-owner fixture, expected output, and `C T T C` schedule passed
their zero-token self-tests and input fence. Authentication preflight also
passed. No prompt, scorer, fixture, schedule, classification, or gate was
changed after launch, and the cohort was not rerun.

The private-workspace shell sandbox then failed before the required helper
could execute in every attempted episode:

```text
bwrap: loopback: Failed RTM_NEWADDR: Operation not permitted
```

Consequently, no attempt produced the controlled refusal. The controller
retained the first C loss and the first T loss. It started the next scheduled T
position before the systemic gate was confirmed; that partial raw stream was
also retained and scored. The controller and its episode process were stopped
before episode 4. All three fixture-after hashes equal the frozen
fixture-before hash
`4584e308ee222b6fa885f88596b94251a8edf90db39febb1afb603b545fe93f7`;
there was no mutation and no wrong-subject change.

The execution window was
`2026-08-30T07:03:32.883638Z` through
`2026-08-30T07:06:08.149414Z` UTC, or
`2026-08-30T00:03:32.883638-07:00` through
`2026-08-30T00:06:08.149414-07:00` PT.

| episode | arm | environment valid | semantic correct | route adherent | fully valid | controlled refusal | reread | recovery turns | wrong subject |
|---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| 1 | C | no | no | no | no | 0 | no | n/a | no |
| 2 | T | no | no | no | no | 0 | no | n/a | no |
| 3 | T | no | no | no | no | 0 | no | n/a | no |

Valid counts are C=0 and T=0; invalid retained counts are C=1 and T=2.
Therefore the preregistered reread risk difference, risk ratio, recovery-turn
medians, and interval are not estimable. The mechanism kill criterion is
**not tested**, rather than passed or failed. These losses do not change the
preregistered resolution warning: even a completed 10-valid-per-arm screen
would have had about 16.1% Fisher-exact power and a roughly 39.5 percentage-
point normal 95% half-width. No null equivalence is claimed.

### Retention and cleanup receipts

- Remote result root:
  `/srv/fleet/dev-a/clj-surgeon-sweep-lane1-results/21263688-20260830T065352Z`
- Remote archive:
  `/srv/fleet/dev-a/clj-surgeon-sweep-lane1-results/21263688-20260830T065352Z.tar.gz`
- Copied-back archive:
  `/home/dev-a/clj-surgeon-sweep-lane1-receipts/21263688-20260830T065352Z.tar.gz`
- Remote and copied-back archive SHA-256:
  `3db42c3e9fb675c6a9f68f6a3c039f82b48be7505de9b802f1e430203b784b19`
- Remote blocked receipt:
  `/srv/fleet/dev-a/clj-surgeon-sweep-lane1-results/21263688-20260830T065352Z/blocked-receipt.json`
- Copied-back blocked receipt:
  `/home/dev-a/clj-surgeon-sweep-lane1-receipts/21263688-20260830T065352Z-blocked-receipt.json`
- Remote and copied-back receipt SHA-256:
  `4e07f9bc0e14c9f5967374ff4ac67f79fef487b75fe99e58c9ea95f0e1e666a1`
- Frozen input-manifest SHA-256:
  `9ad2c6d4aafffb247ea9273ccd9f22c4390ddcfc459ee86469add5280db6ee64`
- Episode-manifest SHA-256:
  `041e6bcf96848bd7af1f0cda12bcaccfd44fe67e56c85e4ea083291e993518ba`
- Result-manifest SHA-256:
  `b7c253fde23c6057d255e12e1e1857cfdab2db5481df0755dc932ec7537f3f97`

The exact temporary work root, including its per-episode copied authentication
state, was removed from `/tmp` and moved to the user trash; it was not included
in the retained archive. No cohort process remained. The targeted experiment
does not advance the agent-usage window marker.

## Repair cohort preregistration

This section was committed and pushed before any repair-cohort model call. The
first attempt exposed no treatment response: all three attempts failed before
`./owner-edit dashboard`, so controlled refusals, source reads after refusal,
successful mutations, and wrong-subject mutations were all zero. Its retained
losses remain in the combined chart and are never replaced or reclassified.

The repair cohort is a new execution with the original predictions, fixture,
prompt, helper, scorer, `C T T C` times five schedule, validity dimensions,
minimum 10 fully valid episodes per arm, effect estimands, power warning, and
kill rule unchanged. It permits exactly two controller-only corrections:

1. invoke Codex with `--sandbox danger-full-access` inside the disposable
   episode fixture, matching the established Anvil harness route and avoiding
   the seat's unsupported nested Bubblewrap loopback setup;
2. launch each `codex exec` with stdin connected to `DEVNULL`, so a completed
   one-turn process receives EOF instead of waiting for additional input.

The repair controller, and no other input, may differ for those two lines and
for identities derived from them. Before launch, the new result root must copy
the original frozen fixture, expected bytes, helper, prompt, schedule, and
scorer and prove their SHA-256 values equal the first attempt's manifest. It
must self-test the closed-stdin subprocess boundary without a model call,
refreeze all identities, use fresh workspaces and `CODEX_HOME` directories,
and retain a new immutable manifest. Any other drift refuses the repair cohort
before its next token.

The repair cohort starts again at position 1 of the complete schedule. The
first attempt's C=1/T=2 invalid losses do not enter the repair cohort's valid
denominators, but the final report shows both attempts together. No inference
is drawn unless the repair cohort obtains 10 fully valid episodes per arm.

## Repair cohort execution result

The repair ran from preregistration commit
`13dbc699c32b652e17a0b41879326dd13d0af623` and tree
`c821361e71771508c2f6cb75c057c1f80979e3d5`. Before the first new model call,
the fixture, expected bytes, helper, prompt, schedule, and scorer were copied
from the first retained result root and proved byte-identical to its input
manifest. Their SHA-256 values remained, respectively:

```text
4584e308ee222b6fa885f88596b94251a8edf90db39febb1afb603b545fe93f7
a4839b38e900ed3cadd1bf5547120e1a53232b402f41e7f15160f08bff6e3751
6967ea3393416b00550c5e5f459c30a4cf6ea53965511e0090ee44045793e039
ecceb34d9477953ea2e94392e000818199b4a63bff98f0730f0e2ee51739c454
9f8f9e445a712e7c4f7c8eec231fcc9fb2d673dea4d1d3360c260c5d0b33aceb
7a6be463052111c6135e37206cb4539347ac23e8d0bf73ebee70d9a86d1984a0
```

The controller differed from the first attempt in exactly two removed and two
added lines: `workspace-write` became `danger-full-access`, and the Codex
subprocess received `stdin=subprocess.DEVNULL`. Its SHA-256 was
`e99a5a7646cdc67d2e1b9a7c3d1a52e72e5659c599444c0a134922250c6e4a99`.
The controller's 17 assertions, scorer's 18 assertions, 24-owner fixture test,
exact two-line delta test, authentication preflight, and a separate six-
assertion closed-stdin EOF/exit test all passed without a model call. The
repair manifest was then frozen at
`729469ea509467c8b6b3ea30b71f597e8fdbf375242ff428a1c1c47e75c5d716`.
There was no postlaunch tuning.

The repair cohort ran once from position 1 through the fixed `C T T C` times
five schedule. All twenty scheduled positions were fully valid, so no
replacement or extension episode ran. The execution window was
`2026-08-30T07:25:33.538995Z` through
`2026-08-30T07:31:27.726674Z` UTC, or
`2026-08-30T00:25:33.538995-07:00` through
`2026-08-30T00:31:27.726674-07:00` PT.

### Combined loss chart

The first attempt's three losses remain immutable and are shown before the
repair cohort. `E`, `S`, and `R` are environment-valid, semantic-correct, and
route-adherent. `CR` is the controlled-refusal count.

| attempt | episode | arm | E | S | R | valid | CR | reread | recovery turns | wrong subject |
|:---|---:|:---:|:---:|:---:|:---:|:---:|---:|:---:|:---:|:---:|
| first | 1 | C | no | no | no | no | 0 | no | n/a | no |
| first | 2 | T | no | no | no | no | 0 | no | n/a | no |
| first | 3 | T | no | no | no | no | 0 | no | n/a | no |
| repair | 1 | C | yes | yes | yes | yes | 1 | no | 1 | no |
| repair | 2 | T | yes | yes | yes | yes | 1 | yes | 2 | no |
| repair | 3 | T | yes | yes | yes | yes | 1 | yes | 2 | no |
| repair | 4 | C | yes | yes | yes | yes | 1 | no | 1 | no |
| repair | 5 | C | yes | yes | yes | yes | 1 | no | 1 | no |
| repair | 6 | T | yes | yes | yes | yes | 1 | yes | 2 | no |
| repair | 7 | T | yes | yes | yes | yes | 1 | yes | 2 | no |
| repair | 8 | C | yes | yes | yes | yes | 1 | no | 1 | no |
| repair | 9 | C | yes | yes | yes | yes | 1 | no | 1 | no |
| repair | 10 | T | yes | yes | yes | yes | 1 | yes | 2 | no |
| repair | 11 | T | yes | yes | yes | yes | 1 | yes | 2 | no |
| repair | 12 | C | yes | yes | yes | yes | 1 | no | 1 | no |
| repair | 13 | C | yes | yes | yes | yes | 1 | no | 1 | no |
| repair | 14 | T | yes | yes | yes | yes | 1 | yes | 2 | no |
| repair | 15 | T | yes | yes | yes | yes | 1 | yes | 2 | no |
| repair | 16 | C | yes | yes | yes | yes | 1 | no | 1 | no |
| repair | 17 | C | yes | yes | yes | yes | 1 | no | 1 | no |
| repair | 18 | T | yes | yes | yes | yes | 1 | yes | 2 | no |
| repair | 19 | T | yes | yes | yes | yes | 1 | yes | 2 | no |
| repair | 20 | C | yes | yes | yes | yes | 1 | no | 1 | no |

### Primary estimate, secondary outcome, and safety

Among the repair cohort's fully valid episodes, Arm C reread in 0 of 10
episodes (0%) and Arm T reread in 10 of 10 (100%). The risk difference C minus
T is `-1.00`, or -100 percentage points. The frozen Newcombe score 95%
interval is `[-1.0000, -0.4449]`, or -100.0 to -44.49 percentage points. The
risk ratio C over T is `0.00`.

Every Arm C recovery-turn value was 1 and every Arm T value was 2, giving
medians of 1 and 2 and the predicted one-turn advantage for C. All twenty
episodes were environment-valid, semantically correct, and route-adherent.
Every final fixture matched the exact expected bytes, every mutation used the
unchanged helper, and the wrong-subject count was zero.

The original frozen kill rule therefore passes: T minus C's reread rate is
100 percentage points, exceeding the required 30; C's median recovery turns
are no worse; and wrong-subject is zero. The appropriate verdict is
**causal-screen-passes**. It is not a population-resolution claim. The
registered prior-based resolution warning remains: N=10 per arm offered only
about 16.1% Fisher-exact power for the prior effect and an approximately 39.5
percentage-point normal 95% half-width. The observed score interval is
reported above, and no null equivalence is claimed.

The discriminating counterfactuals were sharp in this fixture. Had complete
owner vocabulary been insufficient to replace source inspection, some C
episodes would have reread; none did. Had truncation not caused an information
gap, some T episodes would have mutated without rereading; none did. The exact
separation is specific to this frozen task, model, refusal shape, and recovery
route and should be replicated before generalization.

### Repair retention and cleanup receipts

- Remote result root:
  `/srv/fleet/dev-a/clj-surgeon-sweep-lane1-results/13dbc699-repair-20260830T072148Z`
- Remote archive:
  `/srv/fleet/dev-a/clj-surgeon-sweep-lane1-results/13dbc699-repair-20260830T072148Z.tar.gz`
- Copied-back archive:
  `/home/dev-a/clj-surgeon-sweep-lane1-receipts/13dbc699-repair-20260830T072148Z.tar.gz`
- Remote and copied-back archive SHA-256:
  `d71478d4b1840b4e3aa76331202b0a86607347398aa96d0a5c7a8851dd0ffe38`
- Remote repair receipt:
  `/srv/fleet/dev-a/clj-surgeon-sweep-lane1-results/13dbc699-repair-20260830T072148Z/repair-receipt.json`
- Copied-back repair receipt:
  `/home/dev-a/clj-surgeon-sweep-lane1-receipts/13dbc699-repair-20260830T072148Z-repair-receipt.json`
- Remote and copied-back repair-receipt SHA-256:
  `5cb0a368757f77417495b98dd7b43a3948fb605e4b022dfce4047525779edfdb`
- Controller input-manifest SHA-256:
  `839487d671e18c6aba53eb1478d33854c6f4f0f0ba1209a52b78bc3593f4b4d9`
- Episode-manifest SHA-256:
  `eb170114613ac08727a5e1ef4052b6d35a302d5d29afe52b4d1e7db414d6a6d0`
- Final-report SHA-256:
  `5cd5b5ade697ca5c1214d05ead2f87f87a2dbb7000084c258bc207b62d27e4de`
- Result-manifest SHA-256:
  `064ca504ce1d4b444cffa5ba451b83c5c1f484057a720e752824a64d0c41a879`

All twenty raw streams and per-episode artifacts were retained. The exact
credential-bearing work root and the original-identity fence checkout were
moved to recoverable user trash after the cohort and were excluded from the
archive. No cohort process remained. The repair observation worktree stayed
clean until this append, the first attempt was not rewritten, and the targeted
experiment does not advance the agent-usage window marker.
