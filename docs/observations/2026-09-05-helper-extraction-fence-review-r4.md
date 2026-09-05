# GO-WITH-FIX

1. **Finding 11 — OPEN.** Candidate
   `eddbe6fdc3a48832a8ab11ede48ecae90d4e14c6` closes every round-3
   counterexample, but the per-face matrix still omits production invariants
   and admits receipts the production mappers cannot emit.

Using `com.networknt.schema.Schema` 2.0.0 with Draft 2020-12 from the test
classpath:

- the production `terminal-receipt` / `refusal` examples for committed,
  verification-failed, verification-timeout, rollback-failed, and refusal each
  validate against exactly one correspondingly named branch;
- removing every matrix-required field and contradicting every matrix-pinned
  constant produced zero false greens;
- committed without `receipt_hash`, contradictory `kernel_status` on all four
  terminal faces, and refusal with `source_unchanged false` all validate against
  zero branches;
- rollback-failed with numeric `source_retired`, and rollback-failed without
  `planned_source_retired`, both validate against zero branches.

Remaining permissiveness, named exactly:

- every face accepts missing or contradictory `operation`, although both
  production mappers always emit `operation = "helper_extraction"`;
- rollback-failed accepts missing `verification`, although `terminal-receipt`
  always emits it;
- refusal accepts missing, or `true`, `mutation_attempted` and
  `write_authority`, although `refusal` always emits both as `false`;
- rollback-failed accepts any string for `source_retired_unknown`, although the
  mapper always emits one fixed sentence;
- rollback-failed accepts `recovery_required {}`, and both restored faces
  accept `restoration_read_back {}`. Production emits
  `recovery_required.{receipt,reason,recovery}` and
  `restoration_read_back.{files,aggregate_sha256,manifest_in}`; the latter's
  `manifest_in` is always `"details_path"`.

The matrix also leaves production plan projections optional: committed omits
`source_file`, `changed_files`, `retained_sites`, `alias_histogram`,
`partition`, `closure`, and `destination_lib`; each noncommitted terminal row
omits `helpers`, `source_file`, `closure`, `destination_lib`,
`planned_caller_files`, `planned_changed_files`, `planned_sites`,
`planned_retained_sites`, `planned_alias_histogram`, and `planned_partition`.
`source_file` is always `1` but is not pinned.

Probe completed `2026-09-05T08:53:25Z` in exactly two constrained JVMs under
`/var/tmp/forge/helper-fence-fx`. No MCP server or prohibited port was used.
