# Alias-replication ledger independent re-audit

Date: 2026-08-29

Decision: **NO-GO for adding model-run mode or launching the cohort from
candidate `c8477adf4ded65c12d9a442a749f08693b39a61c`.**

The repair closes every false green reported at `37b479c`. Three new executable
false greens remain at the boundary between the attempt ledger and its raw
evidence. The scorer also rejects the safe-refusal control outcome that its
documents say can contribute a lower bound.

This is an experiment-harness defect. The product compiler, alias mechanism,
and token-free one-tool projection remain accepted.

## Frozen subject

- Supplied worktree:
  `/private/tmp/clj-surgeon-alias-replication-harness.YkQW7i`
- Independent detached audit worktree:
  `/tmp/clj-surgeon-alias-reaudit.WMvbHe/worktree`
- Candidate: `c8477adf4ded65c12d9a442a749f08693b39a61c`
- Candidate tree: `41c44a132ff91c7899ed71952a9c9c9c8550d583`
- Red witness: `d540c9811a282a2b3de8cb725dd1a3cb8ccbbc8d`
- Green implementation: `24a13758314e2b05f5c9c53a9e2c140f3d1021ae`
- Producer receipt: `0db172009fa5efa47ab91a7767569c02cf99b08e`
- Runner SHA-256:
  `042cc94dcd133c8e654f3db33c952f64a80cd5e977fca89deae8280e10524993`
- Scorer SHA-256:
  `b4cdb7a84d7c476d56acbfc188b3a9c6c630682c194a86968560ef1ffbb69966`
- Protocol SHA-256:
  `76fa345a88194cd1dc958c4cdf9f3d9ce54020effe890eeb9935b8abba439e1d`
- Capture-server SHA-256:
  `3444390ca0691db476d433685edbd4a342f7676aacc2d09478904fb818d87c3d`
- Independent adversarial probe SHA-256:
  `3de7a9a43f4eb861b1993c4145cca00ce9d82eff203c11fa053e90de381459e0`

The supplied worktree was at receipt head `0db1720` with additional uncommitted
scorer and test changes. The audit did not rewind or edit it. It created a new
detached worktree at the exact requested candidate.

The protocol contains `:model-run-authorized false`. The runner has only
`--self-test` and `--preflight`; model mode is absent.

## Verification that passed

- Exact self-test: 7 tests, 56 assertions, zero failures and zero errors.
- Shell syntax and candidate diff checks passed through the self-test.
- Exact token-free preflight passed for both arms.
- Both Codex projections exposed one ordered and unique `edit_clojure` tool.
- Control client-visible surface SHA-256:
  `6cd8c6cc5d79b1e49afe702670c7ea21234e8e7717520d5743e3ef0c1582562a`
- Treatment client-visible surface SHA-256:
  `81b5ae8311653f860502df9419c23bab234461677c854b4325be5be4223718bf`

The repaired pure compiler now refuses all previously reported cases:

- control 2/2 first-call parity;
- extra, missing, reordered, or duplicate attempt identities;
- reuse of each named isolation field;
- a treatment retry;
- caller-supplied `correct`, `one-shot`, `semantic-decision-exact`, or
  `adherent` flags around invalid call data;
- captured-call and event-call count mismatch;
- negative, zero, NaN, infinite, or over-timeout wall clocks;
- a positional treatment loss or less than 20 percent midpoint improvement.

Nil or empty capture data, empty events, and a non-string workspace identity
also refuse. The exact causal rate law is present: treatment must be 2/2
correct one-shot and control correct-one-shot count must be less than two.

## New executable false greens

### 1. Textual workspace uniqueness is not isolation

The scorer accepts four different path strings that all canonicalize to the
same directory:

```text
/private/tmp/.../worktree
/private/tmp/.../worktree/.
/private/tmp/.../worktree/../worktree
/tmp/.../worktree
```

All four canonicalized to
`/private/tmp/clj-surgeon-alias-reaudit.WMvbHe/worktree`; `compile-ledger`
returned `:ok true` and `:isolated true`.

The accepted baseline has a second form of the same defect. Every captured
request uses one shared `workspace_root`, while the ledger carries four
unrelated strings named `workspace-1` through `workspace-4`. The scorer never
binds a captured request root to its claimed isolated workspace.

### 2. Event arguments are not bound to captured arguments

An invalid partial-pair request was placed in the treatment MCP start event
while the capture row retained a valid request. Event-call count still equaled
capture-call count. `compile-ledger` returned `:ok true`.

The current route compiler checks only event tool identity and count. The
current semantic compiler checks only capture parameters. Those two evidence
streams can describe different calls.

### 3. Duplicate event call identity is accepted

Both event calls in one control attempt were given the same item ID.
`compile-ledger` returned `:ok true`. The route compiler does not require
unique call IDs or matched start/completion pairs.

These defects are independent of caller-supplied summary booleans. Those
booleans are correctly ignored. The remaining caller-controlled raw fields are
capture, events, clock, and isolation; no runner exists yet to establish their
provenance or join them into one observation.

## Safe incomplete control is currently impossible

The accepted historical archive is:

- `/Users/genekim/src.local/clj-surgeon-bench-archive/2026-08-29/clj-surgeon-field-alias-ce05-20260829T071022Z.tar.gz`
- SHA-256:
  `e41da53fd2b973d3545f4608365416d40c20e24a8f865edccec161699563972f`

Historical control `04-A` made two semantically exact alias calls: first
`old`/`new`, then `before`/`after`. Both were publicly refused. Both receipts
reported source unchanged; no mutation occurred. The process completed with
exit 0 after 87,800 ms and reported the failed task honestly.

Selected retained evidence hashes:

- `phase-timing.edn`:
  `2299fa51099ce67325fe2aaab58780fb762edfefc20e010602f5e4e7908606e4`
- `started-items.json`:
  `a73b2309b33136ade834ddff3b46e72a431bd5a3cf97391867998a565f3ee0e8`
- `final.txt`:
  `d0ec77dd04554707ee771eb21c2040f0bf55eec590d904ac319edfdf8bea10ed`
- `terminal.tsv`:
  `ee97658d61cdcecd8f1d2a38fb9f76a8877bfff0503ecb1f8619b7b74069d772`

The new scorer rejects this outcome. Every row must be `:adherent`, and
adherence requires the final captured call to be publicly admitted plus the
agent's exact final text `Captured.`. Therefore the documented statement that
an incomplete safe control can contribute a lower bound is not implemented.

Do not weaken treatment authority to fix this. Define two control outcome
classes:

1. `successful-admitted`: the current admitted, semantically exact route with
   exact `Captured.` final evidence;
2. `safe-refusal-lower-bound`: control only, with every request semantically
   exact but publicly refused, exact tool completion evidence showing
   `source_unchanged=true`, `mutation_attempted=false`, `captured=false`, and
   `error_type=public-arm-schema-denied`, no shell/file/fallback action, and an
   honest non-success terminal message.

Treatment must remain 2/2 `successful-admitted`, correct, and one-shot. A safe
control refusal counts as not correct and not one-shot. Its observed process
wall is a lower bound on complete-task time, not a verified complete wall and
never a multiplier denominator.

## Smallest repair

1. Canonicalize every path-like isolation field. Require four distinct
   canonical workspaces, Codex homes, capture paths, and run directories.
2. Bind each captured request's canonical `workspace_root` to that attempt's
   canonical isolated workspace. Bind the workspace's frozen initial source
   hashes before the run; do not compare all four calls against one unrelated
   root string.
3. Give every raw call one unique identity. Require an exact start/completion
   pair and exact normalized argument equality between the Codex event and the
   capture server's ordered row. Require the captured public-admission result
   and event completion result to agree.
4. Add the separate safe-control outcome above. Keep exact `Captured.` as the
   success-only final. Use a distinct lower-bound timing field and claim scope.
5. When the future model runner is designed, make it the only constructor of
   the append-only ledger. Fence prompt, task, arm surface, client projection,
   source snapshot, process identity, output root, raw event stream, capture
   file, clocks, and every admitted launch before scoring.

After these repairs, rerun this exact adversarial matrix before adding a model
mode. The current candidate remains valuable pure experimental work, but it is
not yet authority for a token-bearing cohort.

## Scope

No model, Anvil arm, product file, install, reload, shared port, or existing
runtime was changed. The exact preflight created only isolated temporary
servers and stopped them normally.
