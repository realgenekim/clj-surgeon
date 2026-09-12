# Attempt17 envelope census (in progress)

The first diagnostic invocation, not a counted prewarm, is retained verbatim
in `envelope-red.log`. It exited 2 in 1.9989320416934788 seconds before any
manifest stage. The exact denied path is `/dev/null`: three shell redirects
failed, including `require-swipl` at Makefile:260. SWI-Prolog is installed
(`/home/forge/.local/bin/swipl`, version 10.0.0). Its absence message is a
consequence of denied redirection, not a missing dependency.

The wrapper permits writes beneath the real worktree, a fresh packet-shaped
UUID TMPDIR, and `$HOME/.local/state/clj-surgeon`. It currently grants no
device exception. Whether `/dev/null` is part of the real packet's device
allowlist needs confirmation before this wrapper can claim parity.

No complete gate census or green envelope receipt exists yet. The gate's
stage loop refuses at a failed stage; its shell recipe also stops on its
first failed command. Unreached stages must be reported as unrun, never green.

## Focused red evidence

`cell-b-red.log`: three tests, twelve errors, exit 1. All deny creation of
`/var/tmp/b07-lint-*` at test/oracles/test_cell_b_oracle.py:15. Exact randomly
allocated paths are in that log (not reconstructed). A TMPDIR-derived
TemporaryDirectory is the intended repair; no assertion needs removal.

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

`test/kernel_warning_check.clj` has no matching root literals. The manifest is
admit-transaction-recovery-battery, alias-migration-test, mcp-test (including
mcp-test-checks), test-bb, test-bb-diagnostic, repository-hygiene, intent-audit.
Battery-fresh is intentionally absent from prewarm by the existing contract.
