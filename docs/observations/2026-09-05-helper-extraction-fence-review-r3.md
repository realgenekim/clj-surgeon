# GO-WITH-FIX

1. **Finding 11 — OPEN** — all five real/public-shaped receipts validate against
   exactly one discriminated branch, but the registered schema still does not
   enforce all authority and constants: removing committed `receipt_hash`
   remains valid; every terminal branch accepts a contradictory
   `kernel_status`; and refusal accepts `source_unchanged false`.
2. **Post-commit-throw witness — CLOSED** — `real-commit!` resolves
   `clj-surgeon.mcp-extraction/commit!`, throws loudly on nil, and the observing
   wrapper proves kernel success, destination existence, and a changed source
   hash before throwing. My independent PROBE 1 then proved complete byte
   restoration and destination removal.
3. **Terminal-state accounting — CLOSED** — a real failing proof reports
   `source_retired 0`; every `planned_*` value equals an actual plan fact; and
   the rollback-failed mapper face carries `source_retired_unknown` rather than
   inventing a retirement count.

Independent FENCE REVIEW round 3 of frozen `helper_extraction` candidate
`05df8b6db1935b57a57b59e463b429dc4599073d`, completed at
`2026-09-05T07:44:45Z`. Delta only: the two round-2 items and the requested
terminal-state change. Findings 1–10 were not re-reviewed.

## Evidence

Four faces were executed through the registered public `:tool-fn`: configured
`/bin/true`, `/bin/false`, `/bin/sleep 1` with a 20 ms timeout, and an
uninitialized refusal. Rollback-failed was constructed through
`terminal-receipt`, as explicitly permitted, with the public finalizer's
`elapsed_ms` added. Draft 2020-12 validation reported:

```text
committed             valid=true  matches=[committed]
verification-failed   valid=true  matches=[verification-failed]
verification-timeout  valid=true  matches=[verification-timeout]
rollback-failed       valid=true  matches=[rollback-failed]
refusal               valid=true  matches=[refusal]
```

The positive examples therefore pass, and the branches are disjoint. The
negative schema probe is what keeps finding 11 open:

```text
remove committed receipt_hash                 => still valid
set terminal kernel_status to "contradiction" => still valid (all four states)
set refusal source_unchanged to false         => still valid
```

Required fix: require `receipt_hash` in the committed branch; pin each terminal
`kernel_status` to its status; pin refusal `source_unchanged` to true. Retain
the already-correct status/state constants and per-face authority requirements.

The corrected supplied verification-step and post-real-commit throw witnesses
ran together: `2 tests, 102 assertions, 0 failures, 0 errors`. Independent
PROBE 1 observed:

```text
resolved clj-surgeon.mcp-extraction/commit! = true
kernel-ok=true destination-exists=true source-changed=true
source hash e35743d6... -> 85f85108...
terminal=verification-failed restored=true source_retired=0
final-tree-restored=true final-destination-exists=false
```

The real `/bin/false` public path restored the tree and returned plan-derived
facts exactly equal to the fixture oracle:

```text
source_retired=0
planned_source_retired=6 planned_caller_files=31 planned_changed_files=33
planned_sites=66 planned_retained_sites=23
planned_alias_histogram={response 29, resp 1}
planned_partition={moved_only 9, mixed 21, qualified_only 1, untouched 3}
```

All fixtures and probe artifacts were under
`/var/tmp/forge/helper-fence-fx`. Exactly two constrained JVM launches were
made with `taskset -c 6-9 nice -n 10`; the first exited before loading because
the probe path was absent, and the second completed every probe in one run. No
MCP server was started and no port was contacted.
