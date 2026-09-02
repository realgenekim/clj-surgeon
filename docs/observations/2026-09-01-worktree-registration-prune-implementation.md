# Stale Worktree Registration Cleanup — Implementation Receipt

Status: implementation GO; first non-fixture prune remains Gene-gated.

## Authority and scope

Gene ratified the stale-registration LLD and EARS at commits `1644f668` and
`49c8f650`, then authorized the remaining LID phases. The implemented operation
is exactly one `:prune-missing-registration` target per reviewed plan. It does
not delete branches, remove files, run global `git worktree prune`, or treat a
failed experiment as disposable evidence.

The deferred rebase-landed seal remains design-only. Its field witness is
`c44ac759` with equivalent content landed at main `64eac2ee`; no patch-equivalence
authority was implemented in this slice.

## Phase chain

- Edge audit: `ba40d5d4`
- Frozen red: `b71e5d9f` — 48 tests, 225 assertions, 9 expected failures,
  0 errors
- Initial implementation: `82b289b0`
- Exact target, snapshot, and registration repairs: `31f54052`, `97377806`,
  `f3e950c6`
- Authority, compatibility, and recovery closure: `27d77dd5`, `e2a105e7`,
  `583e32c5`
- Final implementation candidate: `583e32c5883ae36a5a24cd8346cf438be8170a3e`
- Final implementation tree: `8f2087a8b9c8d69298c69387b4628daf2088babe`

All commits are local-only and authored as `surgeon2 <surgeon2@skiff>` with
Gene Kim's co-author trailer. Nothing was pushed.

## Verified behavior

The controller now:

1. Requires an exact absent registration row with a registered-HEAD-derived
   tree, closed nearest-parent identity, exact preservation proof, inactive
   ownership state, and a clean exact controller identity.
2. Compiles one outcome-free, privacy-safe, operation-bound plan and rejects
   rehashed drift in nested repository, controller, target, time, lease,
   preservation, and Supacode data.
3. Creates and recognizes only its exact runtime lease, runs the exact
   non-force `git worktree remove <target>` command, and proves that peer
   registrations remain unchanged.
4. Uses operation-specific, closed, privacy-safe journal transitions and exact
   typed terminal postconditions. Recovery preserves the original observed
   effect instead of changing receipt meaning.
5. Returns a meaning-free `:registration-pruned` receipt. It never claims that
   the branch was adopted, rejected, landed, or otherwise assigns experiment
   meaning.

## Final gates

- Focused lifecycle: 62 tests, 303 assertions, 0 failures, 0 errors
- Recovery: 10 tests, 40 assertions, 0 failures, 0 errors
- Fast suite: 685 tests, 5,783 assertions, 0 failures, 0 errors
- Targeted clj-kondo: 0 errors, 0 warnings
- Diff check: clean

The focused gate executes one real prune against a controller-owned temporary
Git fixture and recovers all eight journal crash positions exactly once. The
exact current Git compatibility matrix proves one missing-target success and
refusal for a recreated file, recreated directory, dangling symlink, and
locked registration; each refusal preserves the target path authority and the
peer registration.

The independent final audit returned GO for exact `583e32c5` / tree
`8f2087a8`. It replayed every accumulated falsifier and added an
operation-specific archive-result probe. No deterministic contradiction
remained under the no-real-apply limit.

## Operational boundary

No apply ran against a real repository registration. The first real
`:prune-missing-registration` target still requires a separate, explicit Gene
authorization, a reviewed dry-run plan, and the full journal and receipt
ceremony.
