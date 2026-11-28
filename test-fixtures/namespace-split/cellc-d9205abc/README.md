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
This owned wrapper requires exactly two arguments; otherwise it prints one usage line on stderr and
exits 2. It refuses optimized Python before reading inputs, then requires exactly one
manifest-path literal before substituting the supplied argument and executing the frozen oracle.
Missing or duplicate literals also exit 2. The profile clears `PYTHONOPTIMIZE` explicitly. The original
oracle bytes and digest are preserved; the wrapper and updated profile have their own manifest digests.
The oracle retains column-0 form matching.

## Replay usage and result

Run `bb test-fixtures/namespace-split/cellc-d9205abc/replay.clj <astra-checkout> [scratch]`; `CLJ_SURGEON_ASTRA`
replaces the checkout argument. Self-test: `bb test-fixtures/namespace-split/cellc-d9205abc/replay_test.clj`.
The scratch path must not exist and must be outside the fixture and donor checkout. Its parent must
exist (`scratch-parent-missing`, exit 2); missing manifest inputs report `replay-io-refused`, exit 2. Before copying
anything, the runner enumerates every regular file and symlink under `source/`, including hidden paths,
without following directory symlinks. Any path absent from the manifest's `source/` entries refuses
with `unlisted-source-file <path>` and exit 2; no scratch or control directory is created. It checks
every manifest digest in-process with `java.security.MessageDigest`, then copies only the verified
`source/` entries individually, preserving executable attributes, rebinds the request's workspace, and installs
`workspace.clj-surgeon.edn` as `<scratch>/.clj-surgeon.edn`. In the config, `{{scratch}}` becomes the
scratch root and `{{fixture}}` the fixture directory; the commands remain `bin/kaocha unit --fail-fast`
and `env -u PYTHONOPTIMIZE python3 <fixture>/cc-oracle-replay.py <scratch> <fixture>/cc-split.json`.
The donor receives an explicit environment containing only PATH, HOME, JAVA_HOME, LANG and TMPDIR when set.
No SMTP settings, tokens or Python optimization setting are inherited. Frozen `mail.clj` selects
dev-render when SMTP_HOST is absent and no workspace-local `secrets/smtp.edn` exists; the fixture has
no such file, and the inventory check refuses an added `source/secrets/smtp.edn`. It attempts commit first. Only a typed refusal with both
`mutation_attempted=false` and `committed=false` permits one diagnostic plan-only call. The output names
each path and exit and preserves invocation stderr. Missing split facts remain `DIFFERS`;
`created` counts only nonempty regular destination files, using `Files/isRegularFile` with
`NOFOLLOW_LINKS`, absent from the frozen source. A symlink (including a dangling link) or empty file
produces `destination-not-regular <path>` in the failing unexpected-path comparison.
A match reports `created same (20 destination files present on disk)`;
a mismatch reports `created DIFFERS <recorded> <on-disk>`. Receipt destination counts are not filesystem evidence.
After invocation the runner independently inventories every regular file and symlink in scratch,
including hidden paths, without following directory symlinks. It subtracts the frozen manifest's
`source/` entries (without that prefix), request destination files and installed `.clj-surgeon.edn`.
Paths under any `.cpcache/` directory are a separate `profile-artefacts` class created by the verification profile’s
Clojure classpath resolution, counted as `profile-artefacts: N (.cpcache/)` and excluded before the
recorded-artefact subset check.
The remainder must be a subset of the recorded artefacts (`00SERVER-LOGS.txt` and
`test/cfp_scheduler_killer/view_architecture_test.clj`; the installed profile is already subtracted).
The comparison line names total inventory and extra-path counts. An observed empty remainder reports
`expected-different`; nil verb facts cannot establish this boundary. Other paths are named in `DIFFERS`.
A failed verb inspection prints `workspace-inspection: failed <exit> <error>` and is never described as
clean. Acceptance uses the independent inventory. `details_path` is printed without opening it, so a
stale absolute detail path cannot crash comparison. Exit zero requires all invocations
to exit zero and every other fact to match. `request.edn`, `receipt-commit.edn`, and, if fallback runs,
`receipt-diagnostic.edn` stay in a
unique `replay-control-*` sibling directory of scratch, outside the inspected workspace. The runner
prints that directory as `Control:`. Keep it with scratch when retaining evidence; remove both afterward.
The diagnostic receipt never overwrites the commit-attempt receipt. A rollback or committed receipt is never retried.

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

Historical evidence (PR 89 replacement, retained unchanged):

`replay-receipt-2026-11-28-anvil-7b312f85.edn` records `state=committed`,
`verification_complete=true`, 87 caller sites, five callers and 20 destinations. It also records
`workspace_status.exit=128`, `clean_except_proven=false`, `unexpected_paths=nil` and
`error="Not a Git workspace"`. The old runner incorrectly treated that nil as an empty inventory.
The receipt is evidence of the earlier invocation, **not proof of the unexpected-path boundary**.
Its donor name and the previously reported ~9.3 s timing are historical operator claims; this review
round does not re-attest them. This seat ran the real donor replay on Anvil on 2026-11-28 and checked
in this receipt. Its bytes are now SHA-256 pinned in `source-manifest.txt`.

PR 94 round 3 verification uses the commands below. The fixture self-test is explicit opt-in and
requires `bb`, `env` and `python3`. It is no longer loaded by `test/run_all.clj`, so its subprocesses,
full-tree copies and classpath addition stay outside `make test-fast` / `make test-product`.
It checks nil verb facts with an extra inventory path, observed-empty acceptance, exact donor exit and
empty stderr for every stub mode, failed inspection reporting, and a missing details file. The real
runner CLI refuses both an added hidden file and a directory symlink on fixture copies by name with
exit 2 and no scratch/control creation. Python optimization and substitution guards remain covered.
The stub re-emits recorded receipt facts: its success case proves subprocess plumbing only, not
independent split acceptance. Literal-map tests separately witness dangling/empty destination rejection;
CLI probes cover missing inputs and scratch parents. These tests establish runner behavior.

```sh
bb test-fixtures/namespace-split/cellc-d9205abc/replay_test.clj
python3 test-fixtures/namespace-split/cellc-d9205abc/cc-oracle-replay.py; echo exit=$?
~/bin/clj-kondo --lint test-fixtures/namespace-split/cellc-d9205abc/replay.clj \
  test-fixtures/namespace-split/cellc-d9205abc/replay_test.clj
wc -l test-fixtures/namespace-split/cellc-d9205abc/replay{,_test}.clj
git diff --numstat
```

Round 3 local verification: self-test 9 tests / 112 assertions, zero failures/errors;
`~/bin/clj-kondo` zero errors/warnings; runner 67 lines plus tests 82 = 149, maximum 119 columns.
All 40 manifest digests match. The 38 pre-existing entries other than the owned Python wrapper retain
their original digests; the historical receipt bytes match commit `cf6eee91`, now explicitly pinned.

Round 3 `make test` stopped in `test-fast`: 764 tests / 6,320 assertions (no replay fixture tests),
2 failures and 4 errors. Diagnostics named unverified analyzer authority in forward-reference tests,
usage-log CLI exit/outcome mismatches, and unavailable recovery receipt paths under the home state
directory. Later product and benchmark targets did not run. This is not a green product-gate claim.

Deferred follow-ups: dependencies still require network/cache state; freezing a dependency closure
is separate work. The Python wrapper's execution globals are unchanged; the arity guard is fixed and
its owned digest updated. Expanding the compressed Clojure layout conflicts with the
150-line combined cap. Demo email addresses and historical receipt paths/UUIDs stay byte-identical under the freeze rule;
redaction requires a separate fixture policy. Empty remainders still pass the documented subset rule;
scratch/control directories stay available as evidence and require the documented manual cleanup.
Architecture-test provenance remains unresolved as documented above. Previous product-gate/benchmark
claims are withdrawn here; current gate results must come from the actual command output.
