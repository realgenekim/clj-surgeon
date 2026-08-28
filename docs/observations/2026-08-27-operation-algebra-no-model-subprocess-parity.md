# Captain's Log: The Algebra Did Not Slow the CLI

Date: 2026-08-27

Requirement: `OP-ALG-PERF-002`

## Outcome

The operation-algebra cutover preserved no-model CLI subprocess performance on
the frozen apostrophe-bearing generic-change preview.

Across all valid corrected cohorts, 128 counterbalanced runs per arm produced:

| Arm | p50 | p95 |
|---|---:|---:|
| Pre-cutover `91b2190` | 158.510 ms | 250.591 ms |
| Candidate `b05b3a0` | 159.765 ms | 256.706 ms |
| Candidate regression | **+0.79%** | **+2.44%** |
| Maximum allowed | +5.00% | +5.00% |

Every parsed preview result was equal within its cohort. The isolated PATH
recorded zero analyzer, formatter, or verifier invocation. No model, MCP
server, shared port, source mutation, install, reload, formatter, verifier, or
analyzer participated in the measurement.

This is subprocess parity, not a CLI-versus-MCP speed claim and not a claim
that the operation algebra makes the preview faster.

## Frozen boundary

| Item | Value |
|---|---|
| Pre-cutover commit | `91b21901b438e82d21967ddd1d08c3b83b723d15` |
| Candidate commit | `b05b3a03afb7e40020192444777a5a1c20b91a69` |
| Harness head | `89e193f74b5f45eb365f0aa3aaaaed73f0233fa3` |
| Fixture source SHA-256 | `de194bd26668b566404ef106dd7beacbba315dd0c8ed9353fb7065d1b35a641f` |
| Request SHA-256 | `0171a14002759013791c4d41127239abc4f23b6b7148dc18865e6c421022032f` |
| Candidate materializer SHA-256 | `77dc77dca06fb56393d96da43c07daa725ff11da58995ef575540dd2c828756c` |
| CLI wrapper SHA-256, both arms | `d2bb6e634ebc2f4bbf89e4b913ee7ac10fa4cf8cb0c13dcb2f387149f06fa00d` |

The harness used `git archive` materialization for both arms and retained each
source tree, archive hash, wrapper hash, provenance-receipt hash, exact stdin,
stdout, stderr, exit code, elapsed nanoseconds, parsed-output hash, and run
order. The operation was `:change` preview through `:spec-file -`, so all runs
used one immutable source workspace.

The fixture includes `O'Reilly`, escaped quotes, backslashes, and a newline
escape. Stdin carries the EDN directly. No shell-quoted payload participates.

## Protocol

Each cohort performed one declared warm-up per arm. Measured runs used an ABBA
counterbalance:

```text
replicate 1: PRE  POST
replicate 2: POST PRE
replicate 3: PRE  POST
...
```

The measured interval starts immediately before launching the candidate-owned
wrapper and ends when the Babashka CLI subprocess exits. Candidate
materialization, fixture construction, and receipt summarization are outside
the timed interval.

The subprocess PATH begins with a private directory containing the exact
Babashka executable and refusal shims for `clj-kondo`, `clojure-lsp`,
`standard-clj-format`, `npx`, `node`, and `bun`. The launch-event file remained
empty with SHA-256
`e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`.

## The ugly runs stayed visible

The experiment did not pass cleanly on the first try.

1. The first eight-run harness incorrectly counted an empty event file as one
   launch. Its event-file hash proved that the file was empty. The harness bug
   was corrected at `1208256`; that receipt is not included in the valid
   aggregate.
2. The corrected eight-run cohort passed p50 at `+0.78%` but failed p95 at
   `+39.43%`. With eight samples, nearest-rank p95 is the single maximum.
3. A 20-run cohort passed p50 at `+2.58%` but failed p95 at `+22.53%`. The
   candidate arm contained 228 ms and 407 ms spikes.
4. During the failed tail cohorts, retained host evidence showed load averages
   near 11, AddressBookSourceSync using about 64% CPU, Spotlight using about
   64% CPU, WindowServer about 45%, and Supacode about 34%. Counterbalancing
   reduced drift but could not make an eight-sample maximum a stable p95.
5. The 100-run cohort passed independently: candidate p50 `-0.18%`, p95
   `-3.47%`. Pooling every valid corrected run, including both earlier failed
   cohorts, produced the 128-run result at the top of this receipt.

This sequence matters. The 100-run result does not erase the tail observations.
The pooled result prevents selecting only the favorable cohort.

## Immutable evidence

| Artifact | SHA-256 |
|---|---|
| Corrected 8-run receipt | `951b50dbfba47864e1550790a816e363375505d99243f28e86eebb0130cb2186` |
| 20-run receipt | `d71be267031d006ea9022981f5dfd01bc6accb9e5e2ce27a6eb7b94e64b5933e` |
| 100-run receipt | `9bdc483ceccb7f97ba15dfcae9905f7aad34805083fe6927e526bd9d6de08f19` |
| Pooled corrected receipt | `c20cbede731873a140926c5591d107d28d2b93738d2f0cc83245792bb3e24dd2` |
| Complete raw archive | `dab21633877cc5890ead35996c93e451d7631f8a8a590d39d7d215396df7ff80` |

Raw archive:

`/Users/genekim/src.local/clj-surgeon-bench-archive/2026-08-27/operation-algebra-perf-002-89e193f-with-pooled.tar.gz`

The archive also retains the initial harness-counter failure, candidate source
archives, candidate receipts, every raw stdout/stderr file, and every per-run
EDN clock.

## Decision

Accept `OP-ALG-PERF-002` for candidate `b05b3a0`. The no-model preview remains
within the five-percent p50 and p95 bounds, and the cutover adds no observed
heavy-tool launch.

Cherry-pick the benchmark harness and this receipt. Do not change product code
from this branch. For future sub-250 ms subprocess comparisons on an active
laptop, default to at least 20 runs per arm and preserve a larger cohort when
tail noise changes the decision.
