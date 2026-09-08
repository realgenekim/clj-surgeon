GO-WITH-FIX

# Sol fence: `astra/namespace-split` `a9da4344`

Target: `a9da43441234f0f815ab9c9393ae03dda7fa82f4`, based on
`1802e2ad`, with builder commits `e03b37b4` and `a9da4344`. The submitted tip
must not land by itself. It may land on `MCP/main` only with this fence patch;
public `main` remains frozen.

## Findings, in requested order

1. **Background proof: fixed before landing.** The submitted status reducer
   accepted a closure that omitted or changed the launched worker argv, and a
   live PID was not bound to its process start. The fail-first
   `background-status-state-space` produced two false `complete` results.
   Closures now bind and status validates receipt id, original receipt hash,
   candidate hash, PID, process start identity and worker argv. A second worker
   records its actual PID/start on its failed closure, so it cannot impersonate
   the sealed worker; PID reuse fails the start-identity check. Editing the
   original receipt changes the hash and fails closure identity. The worker
   temp root now refuses before publication unless the real existing path is an
   absolute descendant of `/var/tmp`; `/tmp`, `/var/tmp` itself, relative,
   missing and escaping symlink paths fail. `proof-status` CLI probes for failed,
   stale and dead-worker receipts all exited nonzero with the typed errors
   `proof-gate-failed`, `proof-snapshot-stale` and `proof-worker-exited`.

2. **Destructive test setup: required fix applied.** The old
   `oversized-review-facts-refuse-before-publication` constructed `src` and
   `test` paths directly from `root`. The new helper checks the caller supplied
   a non-nil absolute root, canonicalizes it, and requires it to be a strict
   descendant of the JVM's test temp root before `file-seq` or deletion. The
   fail-first nil-root witness raised the wrong unbound-helper failure; after
   repair it returns `:unsafe-test-workspace` and confirms the repository's
   `src/clj_surgeon/core.clj` remains present and byte-sized unchanged.

3. **Receipt facts: guarded.** `receipt-size` already takes the maximum of UTF-8
   EDN and escaped-JSON sizes. The added adversarial receipt is below 65,536
   bytes as EDN and above it as escaped JSON; publication refuses with
   `:receipt-size-bound`. The existing hostile filename witness covers newline,
   U+2028 and U+202E and produces encoded text without those controls.

4. **Committed facts-only: guarded.** The focused boundary witness commits with
   source retirement, then resolves facts from the committed transaction while
   `analyze!` is forced to throw. A changed request returns
   `committed-facts-request-mismatch`; a moved candidate returns
   `committed-facts-stale`, both naming the closure receipt. No deleted-source
   read escaped as an exception.

5. **Printed manifest: fixed before landing.** The submitted manifest included
   verification but omitted the captured `snapshot_hash`; the fail-first test
   found nil. It now includes the computed snapshot hash. Byte-identical replay
   succeeds in plan-only, while changing one hash byte returns `split-refused`
   with a `:snapshot-drift` blocker.

6. **Intent/fail-first audit.** NS-SPLIT-050..054 were present, but the submitted
   witnesses did not name closure argv/PID-reuse identity, unsafe temp admission,
   cross-format byte bounds, one-byte manifest tampering, nil-root destructive
   setup, or failed/dead-worker CLI exits. The product gaps are now registered
   as NS-SPLIT-055..058 with implementation and test links; the destructive
   fixture witness is linked to existing TEST-ISO-003. Repository intent audit:
   `{:ok true, :violations []}`. Fail-first witnesses are
   `background-status-state-space`,
   `background-temp-root-refuses-tmpfs-and-relative-paths`,
   `destructive-fixture-setup-refuses-an-unsafe-root`, and
   `printed-manifest-roundtrips`. The EDN/JSON, committed-facts and CLI-status
   probes passed on the submitted guards and therefore were not red tests.

7. **Frozen Cell C contract hashes: no.** Neither commit changes the checked-in
   frozen manifest; base and tip both hash to
   `1982c78be180554afdcae7f35c4c3905aaf9fed83cdd86f7b1edd1cfac132f93`.
   The external Cell C oracle hashes to
   `3f37ed778ed6658f60254706edac2db8ee8d82dc7e3fcfbb4e2c4774383a3607`.
   The rows-3 background receipt and the prior frozen cold receipt agree on map
   hash `b3ac9adc5a060e6c080db9e4b8cb1d21fde589f9dbcc06036c97b3eea588b818`,
   snapshot hash
   `43b15c857ebcc69b0a4bb8c051d13a319b6eed823a6330a382191e1bb762f938`,
   and counts 141 owners / 20 destinations / 87 sites / five caller files.
   Receipt shape and proof identity are strengthened; the frozen Cell C
   source/mapping/oracle contract is unchanged.

## Focused verification

- Fail-first: five assertions failed for missing/changed argv acceptance,
  absent temp-root admission, absent nil-root cleanup refusal, and absent
  manifest snapshot binding.
- `clj-surgeon.split-proof-gate-test`: 4 tests, 28 assertions, green.
- Targeted facts/manifest/deletion probes: 7 tests, 36 assertions, green.
- `clj-surgeon.split-proof-gate-boundary-test`: 3 tests, 69 assertions, green,
  including real detached workers and three CLI exit processes.
- Intent audit: green, no violations.
- `~/bin/clj-kondo` on the five affected Clojure files: zero errors/warnings.
- Standard Clojure Style and `git diff --check`: green.
- The builder's retained evidence is consistent with its report: original
  `make test` exit 2, resumed MCP stage exit 2, final BB/hygiene resume exit 0,
  with hashes bound by `acceptance-summary.json`. Per instruction, this fence
  did not run `make test`.

One exploratory whole-namespace invocation used the incomplete `-M:test`
classpath and hit missing `nrepl/core` in an unrelated lazy-loaded namespace;
it is not counted as product evidence. All requested affected probes above were
rerun with their correct focused classpaths and are green.


> END RECEIPT (fence-run): worktree HEAD at review exit = a9da43441234f0f815ab9c9393ae03dda7fa82f4 = fenced sha.
