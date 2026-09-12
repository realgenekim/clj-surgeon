# Gate external-write audit

Read-only audit at green source tip 7e63d53d. Scope: the gate stage manifest in
`test/clj_surgeon/battery_parallel_runner.clj`, its source writers and test
fixtures. This is a source/path audit, not a syscall trace or a claim that every
conditional writer executes in every run. No audited site was changed.

`~` below means Java `user.home` for Clojure and shell HOME for Python/scripts.
An isolated home can relocate these defaults; TMPDIR alone does not. Explicit
receipt/profile/output paths can also name external locations.

| Default external path | Writer / source | XDG_STATE_HOME honoured? |
| --- | --- | --- |
| `~/.local/state/clj-surgeon/clj-kondo.lock` | `mcp_process.clj:53`, analyzer admission wrapper opens/updates it | No; dynamic binding or `CLJ_SURGEON_CLJ_KONDO_LOCK` |
| `~/.local/state/clj-surgeon/clj-kondo-priority.lock` | `mcp_process.clj:61`, same wrapper | No; dynamic binding or `CLJ_SURGEON_CLJ_KONDO_PRIORITY_LOCK` |
| `~/.local/state/clj-surgeon/clj-kondo-events.jsonl` | `mcp_process.clj:184`, `resources/clj-kondo-admission.py:120` appends events; installed shim uses same protocol | No; `CLJ_SURGEON_CLJ_KONDO_EVENTS` / dynamic binding / wrapper argument |
| `~/.local/state/clj-surgeon/telemetry/*.jsonl` | `mcp_telemetry.clj:17,65,140`: session creation, append and retention deletion | No; explicit `:directory` or telemetry off |
| `~/.local/state/clj-surgeon/mcp-server.log` | `mcp_server.clj:22`, MCP logging initialization | No; configured log path |
| `~/.clj-surgeon/events.jsonl` | `telemetry_events.clj:70` and append path | No; `CLJ_SURGEON_EVENTS_FILE` |
| `~/.local/state/clj-surgeon/workspaces/<workspace-id>/...` | `mcp_workspace.clj:75` state, explicit-state-home transactions; `mission.clj:169,191,970` missions and INDEX | No; `state-home` is an alternate HOME prefix, not XDG_STATE_HOME |
| `~/.local/state/clj-surgeon/artifacts/<verb>-receipts/<workspace-id>/...` | `receipt_artifacts.clj:12,149`: common root and directory; writer families listed below | **Yes**; `CLJ_SURGEON_ARTIFACT_ROOT` takes priority, then XDG_STATE_HOME, then home default. Root validation still applies. |
| `~/.local/state/clj-surgeon/artifacts/alias-migration-receipts/ledger.edn` | `mcp_alias_migration.clj:2821`: cross-workspace telemetry ledger | **Yes**, through the common artifact root |
| `/var/tmp/forge/helper-fx/...` | `mcp_helper_extraction_test.clj:56`, helper fixtures including alias boundary delegation | No; `CLJ_SURGEON_HELPER_TMP` |
| `/var/tmp/forge/mission-fx/...` | `mission_test.clj:29`, mission fixtures | No; `CLJ_SURGEON_MISSION_TMP` |
| `/var/tmp/forge/<generated git fixture>` | `mission_git_boundary_test.clj:14`, explicit Files.createTempDirectory base | No |
| `/var/tmp/forge/gate-mode-<uuid>` | `test/oracles/test_gate_slot.py:473`, permission witness creates/chmods/removes its fixture | No; fixed fixture parent |
| `/var/tmp/b07-lint-*` | `test/oracles/test_cell_b_oracle.py:15`, Python TemporaryDirectory with explicit parent | No; fixed fixture parent |
| `~/.m2`, `~/.gitlibs` (conditional cache writes) | Clojure CLI/dependency preparation and cold test children | Not selected by repository XDG state handling; dependency/tool-specific caches |
| `/var/tmp/clj-surgeon-gate-<uid>/...` (Darwin fallback only) | `test/gate_slot.py:272`, pathname socket admission if TMPDIR is absent/too long | No; Linux gate slots are abstract sockets, with **no filesystem write** |

Common artifact writer families: `alias-migration` (mcp_alias_migration),
`apply-clojure-changes` (mcp_change_buffer), `require-change` (require_change_io),
`edit-clojure` and `extract` (mcp_tool, mcp_workspace, extract), `transaction`
(mcp_workspace and intent transaction journal/object publication),
`helper-extraction` (mcp_helper_extraction), `namespace-split`
(namespace_split_io), `typist` (mission_cli / mission_typist_executor), `change`
(core), `workspace-lock` (workspace_lock), and `admit-clojure-patch`
(mcp_admit_tool focused report). Receipts, inverse data, detail documents,
transaction preimages and lock files all share the XDG-aware artifact root.

Related paths that must not be mistaken for observed gate writes:
`pressure-status.json` is **read** by mcp_process/admission (the pressure monitor
owns its writes); Makefile MCP service state under `mcp/`, `dev-<port>/`, and
`cclsp/` belongs to service-start targets, not this gate's stage manifest.
`memory-battery` state belongs to its separate target. No shared service was
started. Explicit profile commands can have their own outputs beyond this
repository's defaults; cache writes depend on installed tool/cache state.

## Packet 28bbdab4 attribution

Evidence root:
`target/gate-prewarm/3f55e7ca-01d5-47ab-99d7-47a9addd8c30/`.

* `alias/lane-1.out:5-8`: `detail-pruning-never-deletes-a-document-this-writer-does-not-own`
  cannot write `artifacts/alias-migration-receipts/<id>/peer-0.edn`.
  Line 2041 similarly names `detail-caller-0.edn`. These are external artifact
  writes, not bb's default temp selection.
* The same lane at lines 282, 360 and later returns `invalid-receipt-path` or
  `alias-migration-receipt-dir-escapes`, with `telemetry.recorded=false` and
  the external `alias-migration-receipts/ledger.edn` path. The write envelope
  blocks the root/parent creation; changing bb tmpdir cannot repair that.
* `alias/lane-0.out:65,141,205,323,508,666,778,966` records failures publishing
  transaction, split, prepared-apply, lock, extraction, cold-proof, require-change
  and compact-edit artifacts. Line 1070 explicitly shows the admit focused
  report failing under `artifacts/admit-clojure-patch-receipts/`. This lane is
  the external-publication boundary witness, not another occurrence of only
  the alias receipt subdirectory.
* `alias/lane-0.out:840-843` separately fails under `/var/tmp/forge/helper-fx`;
  that hard-coded fixture root is also outside the packet TMPDIR.
* `bb/lane-0.out` File.createTempFile failures, `bb/lane-1.out:8`'s nil Reader,
  and bb lane 10's temp failure are the motivating bb-default-temp defect.
  A File.createTempFile stack alone does not distinguish these from explicit
  artifact-parent temp writes in JVM alias lanes; the path and lane matter.

Fable owns the separate write-envelope/state-root follow-up. This block changes
only the coordinator's bb argv and its regression witness.
