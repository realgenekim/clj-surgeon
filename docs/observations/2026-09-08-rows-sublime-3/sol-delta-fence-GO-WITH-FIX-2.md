GO-WITH-FIX

# Sol delta fence: `astra/namespace-split` `0956951b`

Target: `0956951bd4de2bea391d988373b83359c698e42d`, exactly one commit
above reviewed base `a9da43441234f0f815ab9c9393ae03dda7fa82f4`.
It may land on `MCP/main` after the one required caller-contract repair below;
public `main` remains frozen.

## Delta identity

CONFIRMED. Excluding only
`test/clj_surgeon/mcp_intent_contract_test.clj`, the target delta is
byte-identical to `/var/tmp/forge/rows3-fix-fx/sol-fence.patch`: both are 491
lines / 31,047 bytes, SHA-256
`6909d1633f896c81659d61752f03e31e10a5716a50edbd787537fd286de5da04`,
and `cmp` exits 0. The excluded file contains only the adjacent explanatory
comment and the required non-MCP intent-ledger count change 233 to 237. No
other byte is present.

## Five risk rulings

(a) **REQUIRED before landing:** retain strict `/var/tmp` as this repository's resource-safety convention rather than an OS-dependent “non-tmpfs” test; `:split-ns!` help must state `TMPDIR=/var/tmp/<owned-dir>` for Babashka and `-Djava.io.tmpdir=/var/tmp/<owned-dir>` for JVM/MCP, and `background-gate-unsafe-tmpdir` must preserve an actionable `next_call` before mutation.

(b) **ACCEPT:** pre-fence receipts cannot acquire trustworthy historical `:worker_started`; `invalid-proof-original` is the correct fail-closed one-way migration for this not-yet-landed experimental receipt shape, with no synthesized recovery or weakened identity.

(c) **ACCEPT / wording honest:** this is the exact argv supplied by the launcher and sealed through the original receipt/job, while PID plus start instant binds the closure to that launched process; the verdict did not claim ProcessHandle-measured argv.

(d) **ACCEPT for landing:** launch already refuses an unavailable start instant, and a later missing observation is a conservative false negative rather than a false completion; distinguishing `proof-worker-identity-unavailable` from observed exit is a non-blocking precision follow-up.

(e) **CONFIRMED:** finding 3 was witness-only because `receipt-size` already selected the maximum of UTF-8 EDN and escaped-JSON bytes; the patch added the adversarial witness and intent links, not production behavior.

## Red witnesses left for the next builder

Only ruling (a) requires product changes. Three focused assertions now witness
it:

- `background-temp-root-refuses-tmpfs-and-relative-paths` requires the gate's
  `background-gate-unsafe-tmpdir` ex-data to carry
  `{:action "set-java-tmpdir-and-retry",
  :java_tmpdir "/var/tmp/<owned-temp-directory>"}` as `:next_call`.
- `unsafe-background-temp-root-refusal-is-actionable` requires the public
  namespace-split boundary to preserve that exact `next_call`, with
  `mutation_attempted=false`.
- `background-proof-help-names-temp-root-requirement` requires the operation help to name
  both caller setup forms.

The focused RED run failed exactly four assertions: missing gate `next_call`,
missing public-boundary `next_call`, and the two absent help strings. No product
code or intent document was changed. Per instruction, `make test` was not run.


> END RECEIPT (fence-run): worktree HEAD at review exit = e04ab8b362982e03520758b50c76afd52998502f = fenced sha.
