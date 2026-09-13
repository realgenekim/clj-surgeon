# Attempt18 envelope census

The whole-gate diagnostic is `envelope-red.log`, exit 2 in
126.6481672860682 seconds. It is not a counted prewarm. Its full pool output
is retained in `diagnostic-run/`; the coordinator's console excerpt truncates
the shell stderr, so the full lane file is authoritative for all twelve paths.

## Observed red writers

| Node | Evidence | Policy repair |
|---|---|---|
| mcp-test-checks / Cell B (3 tests, 12 errors) | diagnostic-run/lane-0.err | TMPDIR-derived TemporaryDirectory |
| alias lane 0 / receipt-artifacts-boundary-test, helper publication witness (1 failure, 1 error) | diagnostic-run/alias/lane-0.out | helper fixture inherits java.io.tmpdir |
| papercut baseline mirror, focused real filesystem probe | static-red.log | TMPDIR-derived TemporaryDirectory |
| Cell B shell default, extracted actual setup | static-red.log | TMPDIR-derived default; explicit ORACLE_TMP retained |
| clj-kondo-admission-path-self-test, unreached-command census | unreached-red.log | disposable clone owns Git metadata under TMPDIR; exact HEAD and hygiene comparison retained |

Exact observed paths, quoted from original logs (including the helper's
FileNotFoundException after its unchecked parent mkdir failed):

```
diagnostic-run/lane-0.err: PermissionError: [Errno 13] Permission denied: '/var/tmp/b07-lint-o8_4uo2u'
diagnostic-run/lane-0.err: PermissionError: [Errno 13] Permission denied: '/var/tmp/b07-lint-neluyvpg'
diagnostic-run/lane-0.err: PermissionError: [Errno 13] Permission denied: '/var/tmp/b07-lint-bmoqaun0'
diagnostic-run/lane-0.err: PermissionError: [Errno 13] Permission denied: '/var/tmp/b07-lint-37are371'
diagnostic-run/lane-0.err: PermissionError: [Errno 13] Permission denied: '/var/tmp/b07-lint-n1wxvaja'
diagnostic-run/lane-0.err: PermissionError: [Errno 13] Permission denied: '/var/tmp/b07-lint-y9j9ba8u'
diagnostic-run/lane-0.err: PermissionError: [Errno 13] Permission denied: '/var/tmp/b07-lint-rdfh7izk'
diagnostic-run/lane-0.err: PermissionError: [Errno 13] Permission denied: '/var/tmp/b07-lint-6707lszy'
diagnostic-run/lane-0.err: PermissionError: [Errno 13] Permission denied: '/var/tmp/b07-lint-8rcbgg13'
diagnostic-run/lane-0.err: PermissionError: [Errno 13] Permission denied: '/var/tmp/b07-lint-qr7w84yq'
diagnostic-run/lane-0.err: PermissionError: [Errno 13] Permission denied: '/var/tmp/b07-lint-xv8t0222'
diagnostic-run/lane-0.err: PermissionError: [Errno 13] Permission denied: '/var/tmp/b07-lint-_ah3h0k8'
diagnostic-run/alias/lane-0.out:   actual: java.io.FileNotFoundException: /var/tmp/forge/helper-fx/details-path-2658516382189079/src/acid/app/x16.clj (No such file or directory)
static-red.log: PermissionError: [Errno 13] Permission denied: '/var/tmp/split-oracle-yci8ur94'
static-red.log: AssertionError: False is not true : /var/tmp/forge/plan2/cellB/run-3413730
unreached-red.log: fatal: could not create directory of '/home/forge/src/clj-surgeon/.git/worktrees/clj-surgeon-kondo-hygiene.XG55He': Permission denied
```

The helper trace reports the failed leaf write. The unwritable mkdir target
is its ancestor `/var/tmp/forge/helper-fx/details-path-2658516382189079`;
that exact parent is inferred from the materializer and leaf, not a kernel
EACCES string. The shell probe's mkdir error omits the path; its printed
TMP value supplies the exact intended root. No other device is writable.

## Coverage

The diagnostic passed recovery, MCP and bb runtime suites, and the shell
checks before Cell B. Pool refusal prevented later manifest stages. The
sequential `make -k` census in `unreached-red.log` explicitly executes every
remaining mcp-test-checks target and the three after/audit stages. Expected
negative ratchet messages are not failing nodes. This census exited 2 solely for clj-kondo-admission-path-self-test;
test-bb-diagnostic passed 830 tests / 7283 assertions, repository hygiene
passed and intent audit returned :ok true. The subsequent focused-green.log
passed both static probes, all three Cell B tests, and the repaired kondo
hygiene witness. helper-green.log passed 24 tests / 202 assertions.
The final whole-wrapper receipt remains the end-to-end authority.

## Static keep/change decisions

The complete direct-node scans are retained as `literal-scan.txt` and
`stage-literal-scan.txt`. These are static findings, not observed gate reds.

| File / matches | Decision and reason |
|---|---|
| test/oracles/test_cell_b_oracle.py:15 | CHANGE: force tempfile root from TMPDIR, with disk fallback only when unset. |
| test/oracles/namespace_split_papercut_oracle.py:478 and doc line 29 | CHANGE: baseline mirror must inherit TMPDIR; describe actual scratch policy. Its gate unit test currently tests only attribution, not baseline filesystem creation. |
| test/oracles/cell_b_oracle.sh:14 and doc lines 3,8 | CHANGE: default scratch derives from TMPDIR; keep explicit ORACLE_TMP override. Gate's Cell B test extracts A8 and supplies TMP directly, so the full-script default is a static finding. |
| Both oracles' ~/bin/clj-kondo / $HOME/bin/clj-kondo | KEEP: mandated executable entrance, not a home write. |
| test/oracles/test_gate_slot.py:72–75 | KEEP: existing disk-root policy; provided packet TMPDIR is retained. |
| test/oracles/test_gate_slot.py:387–429 | KEEP: pure expected path spellings, no filesystem writes. |
| test/oracles/test_gate_slot.py:483,547 | KEEP: documentation of bind boundary and sticky-directory policy. |
| test/gate_slot.py:269,290 and surrounding comments | KEEP: Darwin short socket fallback and unset-TMPDIR fallback. Current Linux gate uses kernel abstract sockets; the long-root witness separately applies its named byte-budget skip. |
| test/tmp_leak_ratchet_test.sh /tmp and /var/tmp literals | KEEP: named RAM-root refusal inputs, pure Make policy queries, or literal-write detection regex/comments. Real fixture root at line 20 already derives from TMPDIR. |
| test/performance_regression_sentinel_intent_test.sh:17; test/direct_cclsp_client_audit_test.sh:5; test/clj_kondo_admission_path_test.sh:4,103; test/cclsp_start_test.sh:4 | KEEP: mktemp templates derive from TMPDIR, /var/tmp only fallback. |
| test/repository_hygiene_gate_self_test.sh:25,53,62 | KEEP: mktemp inherits TMPDIR. |
| test/mcp_heap_config_test.sh:24 | KEEP: fake lifecycle state derives from TMPDIR. JAVA_HOME and /opt/test-java are executable-selection assertions, not writes. |
| test/clj_kondo_admission_path_test.sh HOME; test/cclsp_start_test.sh CCLSP_HOME | KEEP: overrides point inside private fixture roots. |
| test/admit_transaction_recovery_battery.clj:42–46 | KEEP: Files/createTempDirectory inherits explicit java.io.tmpdir. |
| test/clj_surgeon/tmp_leak_support.clj | KEEP: temp/home isolation derives from supplied root; /tmp documentation explains rejected inputs. Tildes in macro forms are Clojure unquotes, not home paths. |
| test/clj_surgeon/battery_parallel_runner.clj:453–454 | KEEP: rejects RAM TMPDIR and uses disk fallback; valid packet TMPDIR passes through. |
| Makefile:43,283 | KEEP: disk-policy selection and refusal-only missing-TMPDIR check. |
| Makefile state defaults:23,76,86,87 | KEEP: state beneath allowed ~/.local/state/clj-surgeon. |
| Other Makefile HOME/path matches | KEEP: installation, server, benchmark, help, and routing targets outside this gate; fixture self-tests override installation destinations. No shared installation is authorized. |


## Expanded Clojure scan

`clojure-literal-scan.txt` scans all test Clojure namespaces, beyond direct
manifest entry points. KEEP pure path data, parser/reader refusal payloads,
missing-root inputs, command-argv assertions, existing state-root writes,
and supplied-temp fallbacks. In particular:

- receipt_artifacts_boundary_test and battery_parallel_test: subprocesses
  print the selected property; they do not write those literal paths.
- mcp_alias_migration_test: artifact validation and refusal literals;
  actual artifacts are in the state root. Whole alias lane must pass.
- mcp_process_test: cwd literals and injected process responses; fixture
  artifacts use its private roots. No rewrite of assertions is authorized.
- memory/oom_reproduction_test and memory/journal_green_test: disk fallbacks
  in separate memory batteries, not prewarm members.
- mission_test's legacy mission-fx default and mission_display_test's guarded
  live read belong to battery-only namespaces, outside this gate. Kept and
  explicitly not certified by this gate.
- mcp_helper_extraction_test: CHANGE the fixture root reached indirectly by
  receipt_artifacts_boundary_test, despite its own battery classification.

The earlier direct-node scan missed Git common-directory writes even though
mktemp itself was correct. The clj-kondo hygiene row above supersedes the
attempt17 KEEP decision for that self-test's worktree mechanism. All other
keep decisions still apply. No new skip, budget or membership change.

## End-to-end result

The second whole-wrapper invocation passed all seven manifest stages at
30808abc354493f876bc7af3857ae4e5fe5cfe0a. `envelope-green-receipt.edn` records
`:state :passed`, `:prewarm? true`, `:landing? false`, `:problems []`,
`:wall-ms 390645`, and source digest
`b57f4e1d55ed702f29bf8d8ae33aa3e114b1e88bc5a21208dff643431af61544`.
The wrapper's complete request wall is 392.7211568816565 seconds in
`envelope-green.log`. Full raw stage/lane evidence is `envelope-green-run/`.
No stage is left unrun and no new skip was added. Existing kernel/byte-budget
and fixture preconditions are preserved verbatim in the logs.
