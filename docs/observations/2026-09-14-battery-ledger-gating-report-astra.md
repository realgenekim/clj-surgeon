| Check | Result |
|---|---|
| Verdict | PASS with a diff-impact tool finding |
| Branch commit | `00566756472b627096774e41c349083ad340a66d` |
| Affected namespaces | PASS — 92 tests, 2,312 assertions |
| Census regeneration | PASS — +3/-0 |
| Lint / intent audit | PASS / PASS |
| Requested diff-impact | TOOL ERROR — zero selected, missing results file |
| One battery run | PASS — 1,203 tests, 19,637 assertions; 1,122 seconds |
| Tracked ledger / seed | Byte-identical; zero appended rows; clean checkout |
| State walls | 54 namespace walls + 14 Var walls; exact match to run measurements |
| Hand-carries / operational refusals | 2 / 1; zero tracked-evidence reverts |

Implemented exact receipt minting opt-in, state-backed runtime walls with a
read-only tracked seed, and the guarded census-regenerate Make entrance.
No new test namespaces, evidence commits, or pushes. All three new deftests
are registered in the derived census. Four linked BATTERY-LEDGER promises
have implementation and test witnesses; intent audit passes with the existing
repository witness-debt policy unchanged.

The requested diff-impact invocation selected no namespaces because it seeds
only changes under `src/`; this patch changes battery machinery under `test/`.
It then failed opening an uncreated results file. This is a tool failure, not
a green impact receipt. The explicitly run battery-ledger, battery-parallel,
and lane-manifest namespaces all pass; logs are retained alongside this report.

The initial witnesses failed before implementation. The original ledger CLI
was also replayed on a fixture and appended without opt-in; the new CLI printed
the candidate without adding a row and appended with opt-in. The first full
focused run caught seed data leaking into non-battery writes; the repair limits
seed inheritance to battery completion, after which all 92 tests passed.

Hand-carries are self-reported manual bridges: explicit affected-namespace
selection and CLI fixture wiring. The one operational refusal was command-policy
rejection of `rm -f` for an owned scratch file; exact-path `rm --` succeeded.
Intentional refusal cases inside tests are not counted as operational refusals.

The serial-width run means one worker lane. The existing coordinator and its
cold-launcher witnesses still create parent/child JVMs; all inherit the explicit
1,024 MB override. No speed comparison is claimed.

The single battery invocation passed with zero failures, errors, skips,
isolation violations, or temp leaks. Worker makespan was 1,105,735 ms and
serial-equivalent namespace time was 1,079,886 ms. Its complete Make receipt
reported 1,122 seconds, including preparation. The candidate line said
`battery-ledger: not appended (BATTERY_LEDGER_APPEND unset)`.

Both tracked files passed SHA-256 comparison against `tracked-before.sha256`.
`git-status-after.txt` is empty. The state file was absent before this run;
scheduling used the tracked seed, then completion wrote
`/home/forge/.local/state/clj-surgeon/battery/namespace-walls.edn`. Its 54
namespace walls exactly equal the run receipt's per-namespace measurements.
Snapshots are retained in `battery-receipt.edn` and `state-walls.edn`.

Owed outside this patch: repair diff-impact's source-only/empty-selection
handling. No successful impact receipt is claimed. The required explicit
affected-namespace checks, lint, intent audit, and one complete battery passed.
No reviewer disagreement was recorded; no independent reviewer was invoked.

## Round 2

Round-1 report above copied unchanged from the records worktree because it was
absent from this branch at 00566756. Full fence verdict read first from
`/var/tmp/forge/ledger-fx/sol-verdict-1.md`.

Class enumeration (line numbers at 00566756; exhaustive `rg state-root` plus
walls/image/ledger helper and publication searches across src/ and test/;
raw discovery retained in `/var/tmp/forge/ledger-fx/round2-writer-audit.txt`):

| Path derivation → writer | Admission before creation/publication | Class member |
|---|---|---|
| `test/clj_surgeon/battery_parallel_runner.clj:66–69` → `write-walls!:975`, mkdir:980, spit:996; completion:1152 | Neither root nor final path admitted; read fallback:72 and preparation:1050 also bypass | Yes, F1 |
| `src/clj_surgeon/probe_state.clj:143–150` → `warm!:154` mkdir → `mcp_http_server.clj:325` → `probe_state.clj:85–98` write-image!/publish-stage! | warm!:147 admits selected root; :150 admits final path before mkdir/server; write-image!:88 re-admits envelope before mkdir and atomic publication | No |
| `src/clj_surgeon/probe.clj:262–267` → local-image/read-image! | Read-only; no publication | No writer |
| `test/clj_surgeon/battery_ledger.clj:49,92–103` → append-entry! mkdir/spit; record-entry! gates append | Explicit tracked ledger path, never derived from state-root; STATE-HOME root admission does not apply | No |
| `test/clj_surgeon/install_test.clj:20–33` | Local variable named state-root is a temporary install fixture, not probe-state/state-root | No |
| `test/clj_surgeon/state_home_admission_test.clj:36–54`; `probe_state_test.clj:230–243`; battery_parallel_test.clj:30–60 | Admission/production writer witnesses, not additional production writers; fixture setup/cleanup is explicit test I/O | No additional writer |

No other member was found; ledger append needs no additional class matrix.
The new matrix uses real environment selection in fresh child processes and
calls production read-wall-record, prepare-suite!, write-walls!, finish-suite!.
No admission or I/O function is replaced. Rejected roots must remain absent;
redirect cases also compare complete selected-root snapshots including links.

RED (production unchanged): exit 1, **1 test / 62 assertions / 38 failures / 0 errors**.
The admitted positive control passes. Direct rejected roots return nil kind and
write/finish report `:unchanged? false`. Read/prepare return nil kind instead
of refusing. Descendant cells likewise lack the required typed refusal.

```text
FAIL in (walls-production-state-home-admission-matrix) (battery_state_admission_test.clj:102)
inside
[:inside :read {:read {:kind nil, :unchanged? true}, :prepare {:kind nil, :unchanged? true}, :write {:kind nil, :unchanged? false}, :finish {:kind nil, :unchanged? false}}]
expected: (= kind (get-in result [op :kind]))
  actual: (not (= :state-root-inside-workspace nil))
...
Ran 1 tests containing 62 assertions.
38 failures, 0 errors.
```

Full RED output is embedded in the RED commit message and retained at
`/var/tmp/forge/ledger-fx/round2-red.log`. Census: exit 0, `+1/-0`.
