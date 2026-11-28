# Cell C namespace-split fixture, frozen 2026-11-28

Provenance. The exact witnessed contract behind the automatically routed exact-contract namespace-split class (the routing plate's own words, "exact-contract
namespace split": curtaincall-cfp commit `d9205abcc9b9f31d93bd4a76537b74fccd583ca6`,
`src/cfp_scheduler_killer/views.clj` (141 named owners into 20 absent destinations, 87 static sites in
the caller files). Until today the request and its receipts lived only under `/var/tmp` on Anvil, where
systemd's thirty-day cleaner reaped the alias-migration sentinel's evidence; this directory is the copy
that cannot be lost (fleet plan step 2; mayor's MORE-TONIGHT, 2026-11-28).

Files.

- `source/src/cfp_scheduler_killer/views.clj`, `exports.clj`, `server.clj`, `sinks.clj`: the bytes at
  `d9205abc` (`git show d9205abc:<path>` in `/home/forge/src/curtaincall-cfp`); `source-manifest.txt`
  carries their SHA-256 digests and those of the request, manifest and oracle, so byte-exactness can be
  checked without that clone (`server.clj` ends without a trailing newline, as at the commit). The twenty
  `views/*.clj` destination files the receipts name do not exist at that commit; the split creates them.
  The frozen sources carry real `itrevolution.net` addresses from the application's demo roles and
  placeholders; they are the commit's bytes, not credentials.
- `D1-request.edn`: the frozen Cell C request as written 2026-09-07 (`/var/tmp/forge/plan2/cellC/`). The
  weekly sentinel sent a byte-identical request apart from `:workspace_root`; `:workspace_root` must be
  rebound to a fresh worktree of the source commit before use.
- `cc-split.json`: the same manifest in the MCP `namespace_split` shape; `cc-oracle.py`: the Cell C
  oracle used by the round runner.
- `sentinel-20261025-receipt.json`, `sentinel-20261025-split-receipt.edn`: the round receipt and the
  split receipt of the last sentinel run that completed (2026-10-25, trunk 454a2863). Read them together:
  the split receipt records committed state, complete verification (six checks) and 87 rewritten
  sites across five callers (`server.clj` 62 and four test files 16, 4, 2 and 3), but also
  `:load_status :not-run` with nothing loaded, `dynamic_references "not claimed"`,
  `:clean_except_proven false` with three `:unexpected_paths` (one a test file,
  `view_architecture_test.clj`) and three `:unrequired_qualified_refs` (for example
  `cfp-scheduler-killer.store/now-inst` in `views/dashboard.clj`, which `views.clj` never required);
  and the ENCLOSING round recorded `verdict: RED`, `exit_code: 1` and a witness-runner exit of 97 with
  unknown test counts. Read the oracle result as what it is: four oracles passed on a split whose load was
  not run and whose tree was not clean. The four test callers are now frozen here. `exports.clj` and
  `sinks.clj` are frozen only because they hold pre-existing namesakes (`date-fmt`, `fmt-stars`) that the
  receipt records; neither request names them and the receipt rewrote neither.

Not here: the split's output tree. The receipts carry its hashes. The replay below builds a scratch
workspace from these inputs and installs the recovered `split-unit` profile, which `d9205abc` itself
does not carry. It still needs the archived verb, its analyzer admission gate, and the application's
Java/Clojure dependencies. The profile passes the fixture's `cc-split.json` to `cc-oracle-replay.py`.
This wrapper refuses optimized Python with exit 2 before reading inputs, then requires exactly one
manifest-path literal before substituting the supplied argument and executing the frozen oracle.
Missing or duplicate literals also exit 2. The profile clears `PYTHONOPTIMIZE` explicitly. The original
oracle bytes and digest are preserved; the wrapper and updated profile have their own manifest digests.
The oracle retains column-0 form matching.

## Replay usage and result

Run `bb test-fixtures/namespace-split/cellc-d9205abc/replay.clj <astra-checkout> [scratch]`; `CLJ_SURGEON_ASTRA`
replaces the checkout argument. Self-test: `bb test-fixtures/namespace-split/cellc-d9205abc/replay_test.clj`.
The scratch path must not exist and must be outside the fixture and donor checkout. The runner checks
the SHA-256 manifest in-process with `java.security.MessageDigest`, copies `source/` with file attributes, rebinds the request's workspace, and installs
`workspace.clj-surgeon.edn` as `<scratch>/.clj-surgeon.edn`. In the config, `{{scratch}}` becomes the
scratch root and `{{fixture}}` the fixture directory; the commands remain `bin/kaocha unit --fail-fast`
and `env -u PYTHONOPTIMIZE python3 <fixture>/cc-oracle-replay.py <scratch> <fixture>/cc-split.json`.
The donor receives an explicit environment containing only PATH, HOME, JAVA_HOME and LANG when set.
No SMTP settings, tokens or Python optimization setting are inherited. Frozen `mail.clj` selects
dev-render when SMTP_HOST is absent and no workspace-local `secrets/smtp.edn` exists; the fixture has
no such file. It attempts commit first. Only a typed refusal with both
`mutation_attempted=false` and `committed=false` permits one diagnostic plan-only call. The output names
each path and exit and preserves invocation stderr. Missing split facts remain `DIFFERS`;
`created` compares the recorded count with request destination files present on disk in scratch
and absent from the frozen source. A match reports `created same (20 destination files present on disk)`;
a mismatch reports `created DIFFERS <recorded> <on-disk>`. Receipt destination counts are not filesystem evidence.
Replay `unexpected_paths` must be a subset of `.clj-surgeon.edn`, `00SERVER-LOGS.txt`, and
`test/cfp_scheduler_killer/view_architecture_test.clj`; nil means no unexpected paths. Accepted subsets
report `expected-different`; any other path is named in `DIFFERS`. Exit zero requires all invocations
to exit zero and every other fact to match. `request.edn` and the last `replay-receipt.edn` stay in a
unique `replay-control-*` sibling directory of scratch, outside the inspected workspace. The runner
prints that directory as `Control:`. Keep it with scratch when retaining evidence; remove both afterward.
A rollback or committed receipt is never retried.

Round 2 additions, all obtained with `git show d9205abc:<path>` from the read-only rescue checkout
`/home/forge/src/surgeon-fixture-rescue/plan2/build/round-sentinel-ns-20261025T070002Z-fx`:

| Added under `source/` | Why needed |
|---|---|
| `test/cfp_scheduler_killer/{polish_test,forms_test,comms_test,views_test}.clj` | The four recorded test callers supply the 25 sites missing from the original fixture. |
| `bin/kaocha` (executable), `tests.edn`, `deps.edn` | Exact launcher, unit-suite selection, dependencies and `:run-tests` alias. |
| `test/cfp_scheduler_killer/test_helpers.clj` | The callers require the temporary-store fixture. |
| `src/cfp_scheduler_killer/{auth,auth_google,committees,events,forms,inform,mail,notices,people,portal,replay,reviews,schedule,secrets,seed,sessionize_import,sse,store,submissions}.clj` | Transitive local namespace dependencies of the frozen sources and callers; the launch-only probe failed first on missing `auth`. |
| `resources/letters/{accepted,declined,waitlisted}.txt` | The comms tests render decision letters and the template list; without these, `configured-send-test` handed the transport a nil recipient. |

No other application tests, resources, build scripts, or cached dependencies were added. These inputs
exercise the requested caller subset, not the complete historical application suite. `source-manifest.txt`
covers every frozen input and the new profile template. The request, split manifest, and all frozen source files are unchanged.

The fifth requested file, `test/cfp_scheduler_killer/view_architecture_test.clj`, exists in the rescue
working tree but **does not exist in `d9205abc`** (`git show` exits 128). It is not frozen or silently
substituted from disk. This is consistent with its being an unexpected path in the old receipt, but does
not establish its provenance. That gap remains open.

Verification on 2026-11-28 (PR 89 review round 3):

The earlier claim of a fresh committed replay on donor `7b312f85` is withdrawn: its receipt was
only an unshared local path, so this checkout does not substantiate that result. Real donor replay
remains for the operator. A receipt without `facts.footprint.created` uses the filesystem comparison above.

Self-test requires the runner namespace and loads the frozen sentinel receipt and profile:
6 tests / 73 assertions, no failures/errors; kondo reports zero errors/warnings. All 39 SHA-256 entries
pass. Runner plus tests total 150 lines, each under 120 columns. The checked-in subprocess tests run
the real runner CLI against a stub donor for matching exit 0, nonzero-exit rejection and typed-refusal
fallback. The stub checks the complete environment allowlist, sibling request placement, executable
copying and profile installation. Python subprocesses cover optimization refusal, missing/duplicate
manifest literals, successful substitution and active assertions. The new regressions failed against
the old code before the fixes. An additional default-scratch probe passed and verified that both
control files stay outside scratch; the reported canonicalization failure did not reproduce here.
These probes establish runner behavior, not a real split's behavior.
The round 3 `make test` gate stopped in `test-fast`: 756 tests / 6,196 assertions, 2 failures / 4 errors
in analyzer authority, usage-log CLI outcomes, and recovery receipt writes under `~/.local/state`,
matching the previously recorded gate failures. Later gate stages did not run.

Deferred follow-ups: extra unlisted files under `source/` are not detected by the digest manifest;
adding source inventory validation and its witnesses is outside this 150-line slice. Real demo email
addresses remain because the sources are frozen content; changing them would require a separate
fixture policy. Architecture-test provenance remains unresolved as documented above.
