NO-GO

# Sol fence review: `fable/battery-fresh-code-only` at `df3f9a18`

Reviewed against parent `05784068` on 2026-09-08. The single-commit attacks
fail closed, but the prefix exemption creates a two-commit laundering route
for regular files consumed by a gate. That route is present in the current
tree, contradicts the premise used to justify the widening, and can let a
battery-relevant change consume zero counted distance. Do not merge this tip.

## Blocking finding: a counted rename does not keep the destination counted

`archive-only-diff?` decides from only the current raw record's mode, status,
and path. A rename from `test/`, `Makefile`, or another live location into
`docs/observations/` is correctly counted because `--no-renames` exposes the
outside deletion. That protection lasts for only that commit. The rename costs
one of the allowed 30 commits and therefore does not require a new battery.
Every later `100644 -> 100644 M` at the destination is classified records-only.

This launders all three requested input shapes:

- `make -f docs/observations/Makefile` needs no executable mode;
- `bash docs/observations/landing_gate.sh` needs no executable mode;
- a test can read `docs/observations/gate.edn` as a fixture.

There is already a concrete counterexample to the claimed directory invariant:
`clj-surgeon.lane-manifest-test` includes
`docs/observations/2026-09-02-anvil-builder-seat-brief.md` in
`living-prose-files` and slurps it in the fast lane. A regular modification of
that input is accepted by `archive-only-diff?` as records-only. Whether that
particular input affects the battery lane is not the point: the implementation
has no mechanism that distinguishes an output from an input, and the current
tree disproves the asserted mechanism-level invariant that nothing under the
prefix is consumed by a lane.

Four intentionally red assertions were added to
`test/clj_surgeon/battery_ledger_test.clj`: a later modification of the
laundered Makefile, interpreter-invoked script, fixture, and the existing
living-prose input must count. On this tip all four are classified exempt.

Required correction: do not infer "observational output" from a directory
prefix plus mode. Restore a closed/manifested output set, or first make and
enforce the stronger repository invariant that no test, gate, runner,
interpreter invocation, or configuration consumer can read any exempt path.
The correction needs a lasting ratchet for future consumers, must move or
exclude the existing living brief, and must make the four red assertions pass.

## Requested attack matrix

Real Git histories under an owned `/var/tmp/forge` fixture and matching pure
assertions produced these results:

| Attack | Result | Evidence |
|---|---|---|
| Rename `Makefile` into the prefix | counted | `--no-renames` emitted outside `D` plus inside `A` |
| Rename executable gate script into the prefix | counted | modes remained `100755`; outside `D` also remained visible |
| Rename regular fixture into the prefix | counted | outside `D` plus inside `A` |
| Add symlink under the prefix pointing at `src` | counted | Git emitted mode `120000` |
| Mode-only `100644 -> 100755` | counted | equal object IDs did not bypass the mode-pair check |
| Empty commit/diff | counted | empty evidence is falsy, not vacuously exempt |
| Merge with docs-only first-parent lineage and code in the second | counted | both parents were inspected; the first-parent diff exposed `Makefile` |
| Modify the renamed regular destination later | **exempt: blocker** | path is now under the prefix and mode is `100644` |

The temporary probe fixtures were removed after execution.

## Age, ancestry, distance, and intent audit

The widening did not edit `freshness`, the 26-hour comparison, newest-entry
and newest-failure authority, the ancestry command, the 30-count comparison,
or the all-DAG `rev-list` range. Six focused semantic tests covering age,
failure authority, non-ancestry, the 30 boundary, raw audit fields, and records
churn passed with 31 assertions.

The raw-history fallback is also unchanged and was probed directly: at exactly
1000 raw commits the parent list was inspected (1000 diff calls in the stubbed
probe); at 1001 raw commits there were zero parent-list and diff calls, counted
distance stayed 1001, and ignored distance stayed zero.

The amended requirement honestly says that `A` outside the prefix counts, and
the classifier rejects such additions. It also retains the superseded rule.
The amendment is not honest as a complete safety argument, however: its design
premise says nothing under the prefix is loaded, compiled, or executed, while
the same design admits live operational briefs as a deliberate loss and the
current fast lane consumes one. Mode `100644` does not mean non-executable when
an interpreter is used.

## Commands and receipts

- Candidate baseline, affected namespace: 15 tests / 94 assertions, green.
- Candidate end-to-end records witness: 12 rows / 0 mismatches, green.
- Added single-commit attack assertions before the blocker: 15 tests / 101
  assertions, green.
- Final affected namespace with the laundering regressions: 15 tests / 105
  assertions, **4 failures**, exit 4, intentionally red.
- Focused age/ancestry/distance probes: 6 tests / 31 assertions, green.
- `git diff --check`: clean before the intentionally red assertions; no
  production implementation was changed by this review.



> END RECEIPT (fence-run): worktree HEAD at review exit = df3f9a18fa14b2cf7494d82eb06688b725dc0c80 = fenced sha.
