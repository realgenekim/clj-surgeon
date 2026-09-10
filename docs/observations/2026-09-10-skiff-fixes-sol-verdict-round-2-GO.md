GO

# Sol fence review, round 2 — fable/skiff-fixes 98f7d23b

Reviewed sealed candidate `323e58ba6341bd81eec93cab09f40e6b6d3891d1`
(base `59d8bc0cad8886a028c24e8ffd52e4e5d82185c1`, branch tip
`98f7d23b8cc17bc78cbcb6510f95c577ce5351a1`). No required finding remains.

## Delta closure

- The test-only diff in `c196aab1` is byte-identical to the archived round-one
  fence patch after excluding `docs/observations/skiff-fixes-fence.md`; both
  patches have SHA-256 `93a1d84ff2c2525c5eee110125173de2b25abde0cb940deda93ecb0a03c45459`.
- `under-root?` has exactly one caller, the `workspace/receipt-dir` assertion
  that asks where an unwritten path would go. The eleven publication call
  sites still use `published-under-root?`; each consumes a committed result's
  receipt, details, retirement, or lock artifact. No publication assertion can
  reach the lexical predicate in the reviewed tree.
- The four affected namespaces passed: 90 tests, 1,474 assertions, zero
  failures/errors, and zero isolation violations. The boundary witness proves
  a real receipt returns true and a nonexistent artifact refuses with
  `:artifact-path-unresolvable`.

## Requested witnesses

- Forced-Darwin fake `vm_stat`/`sysctl`: gate reader and preflight both reported
  6718 MiB; missing tools produced the same typed `vm_stat` refusal.
- `(apply proc/process opts argv)` returned the expected subprocess output on
  bb 1.12.209 and bb 1.13.219.
- The longest bound leaf is `.pending-` plus 32 hex characters (41 bytes), and
  the root fit calculation uses that value. The 20-test socket oracle passed;
  an overlong declared root refused by name at 114 bytes against the 100-byte
  budget and 104-byte Darwin cap.
- A deliberately impossible `:min-bb-version "999.0.0"` warned, ran the body,
  and exited zero on bb 1.13.219. The declaration documents the measured floor;
  `jvm-error-test` supplies the actual compatibility enforcement.
- Linux-focused receipt, workspace, memory-reader, and socket checks passed.
  Per brief, `make test` was not run; the builder's full seven-stage gate on
  `98f7d23b` is the suite authority.


> END RECEIPT (fence-run): worktree HEAD at review exit = 323e58ba6341bd81eec93cab09f40e6b6d3891d1 = fenced sha.
