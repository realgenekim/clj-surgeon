# Alias-replication harness adversarial audit

Date: 2026-08-29

Decision: **NO-GO for adding model-run mode or launching the alias cohort.**

The one-kernel arm compiler and the one-tool client surface are fit for the
experiment. The cohort reporter is not yet causal authority. It can certify a
retry-deletion claim when both controls are already correct on their first
call, it can ignore an admitted extra attempt, and it does not bind all
isolation or clock facts needed by the claim.

## Frozen subject

- Supplied worktree:
  `/private/tmp/clj-surgeon-alias-replication-harness.YkQW7i`
- Independent detached audit worktree:
  `/tmp/clj-surgeon-alias-replication-audit.0WUagO/worktree`
- Base: `44d564ecbed90f72d3b25f945a52b7d7ebccacd7`
- Candidate: `938af1353695ae0e1c24af186d3bc54855f5c5ea`
- Candidate tree: `6262c05e869390caaade0bd1dbc83cdab92fe661`
- Runner SHA-256:
  `0e8014c9d84ea6b900e377b4cb6482dfc2165efc16d15189ef8f9cb9ec8dafc1`
- Scorer SHA-256:
  `75ed701d0067df7b9eac7e1a9ea419a067718ef688ad9155cf52ab780d2eb4d8`
- Capture-server SHA-256:
  `3444390ca0691db476d433685edbd4a342f7676aacc2d09478904fb818d87c3d`

The supplied worktree had advanced after the request. The audit did not
rewind or edit it. It used the exact candidate in a new detached worktree.

## What passed

### One current product compiler

The treatment uses the current product `edit_clojure` description and schema.
The control changes only the public field vocabulary from the three accepted
closed pairs to canonical `from` and `to`. Both arms then use the same path:

```text
public arm admission
  -> canonical workspace routing
  -> product request validation
  -> editor-gesture lowering
  -> compact-location normalization
  -> canonical transaction compiler
```

The retained zero-model test proves that canonical `from`/`to`, `old`/`new`,
and `before`/`after` all compile to the same 51-edit, 9-file future with the
same hashes. The control aliases remain semantically exact but not publicly
authorized. The scorer preserves that distinction.

No bounded request in the adversarial admission matrix was accepted by the
scorer and refused by the corresponding production path. Partial pairs,
mixed pairs, and unknown fields all refused. Wrong or absent benchmark roots
also refused at the scorer's frozen-workspace boundary. Product-supported
fields outside this experiment's public arm, such as `verify` and `expect`,
were rejected by the experimental arm before compilation; this is an
intentional closed-fixture restriction, not evidence of wider product parity.

### One-tool surface and no-effect capture

The exact token-free preflight passed for both arms:

- one ordered and unique tool: `edit_clojure`
- control client-visible surface SHA-256:
  `6cd8c6cc5d79b1e49afe702670c7ea21234e8e7717520d5743e3ef0c1582562a`
- treatment client-visible surface SHA-256:
  `81b5ae8311653f860502df9419c23bab234461677c854b4325be5be4223718bf`
- control advertised surface SHA-256:
  `b8552a4f57bb4c632af49d06c959a88aed8cc1ae99c80e1830f8410b0420b501`
- treatment advertised surface SHA-256:
  `37727eb92385f4012a8c58d485f7b4b06930852d71ff6944673a7272b5d54112`
- final capture text: exactly `Captured.`

The one-tool projection faithfully reproduces the accepted alias mechanism.
No catalog repair is required. The capture handler is deliberately no-effect;
capture success is not mutation correctness and must remain subordinate to
the offline compiler/future-hash evidence.

### Existing gates

The following gates passed at the exact candidate:

- harness self-test: 7 tests, 40 assertions, 0 failures, 0 errors
- shell syntax
- candidate diff check
- exact token-free control and treatment preflight

Identity, tree, runner, scorer, clean-checkout, and clean-output checks occur
before the current preflight starts its client projection. The exact tree plus
clean-checkout fence transitively binds the supplied capture-server hash.

## Executable false-green findings

The adversarial probe called `cohort-report` directly with otherwise valid
four-row cohorts and changed one claimed fact at a time.

| Falsifier | Actual result | Required result |
|---|---:|---:|
| Both controls also correct and one-shot; treatment is only faster | **pass** | refuse |
| One control correct and one-shot; treatment 2/2 one-shot | pass | pass under the strict-rate law below |
| Treatment contains a retry | refuse | refuse |
| Wrong order | refuse | refuse |
| Missing summarized run | refuse | refuse |
| Duplicate summarized run | refuse | refuse |
| Five admitted attempts summarized as four rows | **pass** | refuse |
| Four workspaces but reused hidden client/server isolation | **pass** | refuse |
| Treatment loses either positional pair | refuse | refuse |
| Midpoint improvement below 20% | refuse | refuse |
| Missing complete-wall clock | refuse | refuse |
| Negative complete-wall clock | **pass** | refuse |
| Absurd but finite complete-wall clock | **pass** | refuse |

The first false green confirms the suspected causal defect. The current
report checks that treatment is 2/2 one-shot and that control decisions are
semantically exact. It does not require treatment to improve first-call
success over control.

The dropped-attempt probe also shows that four retained summary rows are not
an attempt ledger. A model runner cannot prove attempt retention by supplying
four rows after a fifth admitted launch occurred.

## Exact causal law

Do not require historical control replication to be exactly 0/2. That would
discard a valid 1/2 control result. Require a strict aggregate first-call-rate
improvement:

```text
treatment correct one-shot count = 2 of 2
control correct one-shot count < treatment correct one-shot count
```

Thus control 0/2 or 1/2 can support the retry-deletion claim. Control 2/2
cannot. Record the exact control count; never turn an incomplete control into
a speed multiplier.

No `T_emit` win is required for this cohort. The accepted mechanism deletes a
retry; historical first-call construction was approximately equal between
arms. Retain `T_emit` as descriptive telemetry, but gate promotion on the
strict first-call-rate improvement, exact semantic future, and the declared
complete-wall law.

## Smallest repair before model mode

1. Derive the cohort rows from an append-only attempt ledger. Bind exact
   attempt count, exact `C T T C` order, stable attempt IDs, and every admitted
   post-authorization launch. Refuse if any attempt is missing or duplicated.
2. Add the strict first-call-rate law above. Keep treatment 2/2 correct and
   one-shot. Refuse control parity even when treatment wall is lower.
3. Expand isolation evidence beyond workspace. Require unique per-run Codex
   home, session/thread, capture path, output/run directory, and capture-server
   identity or port. Bind these fields into each ledger row.
4. Derive `correct`, `one-shot`, `semantic-decision-exact`, and route adherence
   from captured calls plus the frozen public-admission and compiler evidence.
   Do not accept caller-supplied booleans as authority.
5. Require complete-wall clocks to be numeric, finite, positive, and no larger
   than the model runner's frozen numeric timeout. Bind the runner and scorer
   to the same timeout.
6. Preserve the current pre-authorization order when model mode is added:
   exact head/tree/file identities and clean git/output, token-free client
   projection, then authorization, then model launch. Freeze the prompt, task,
   arm surface, and expected future hashes in the launch manifest.

These repairs belong to the experiment harness and scorer only. They do not
require a product change, second compiler, catalog expansion, install, reload,
or shared-runtime action.

## Decision boundary

After the repairs, rerun the zero-model adversarial matrix. A green result can
authorize only the frozen four-call `C T T C` capture cohort. It cannot
authorize product promotion. Model launch remains **NO-GO** at candidate
`938af1353695ae0e1c24af186d3bc54855f5c5ea`.
