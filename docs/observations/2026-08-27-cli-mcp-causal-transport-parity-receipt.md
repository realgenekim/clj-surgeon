# Captain's Log: The Transport Was Different; the Intent Was Not

Date: 2026-08-27

Issue: `clj-surgeon-9xi`

## Outcome

The first three CLI-versus-MCP strata have semantic parity on exact commit
`a2a928db68ea896536133c017ac439634fa070cd`.

This receipt makes no CLI-versus-MCP speed claim. The existing benchmark
harness resolves an unpinned global CLI, and the visible CLI on all three
Anvil development seats is commit `6ff11c9c5c127499ff121c5f4a262fe87f14f351`,
not the candidate. A timing ratio from those arms would have different code in
the numerator and denominator.

The smallest earned next step is a benchmark-only candidate wrapper that is
created from the same checkout as the isolated MCP server. The harness must
verify both identities before it starts either model arm.

## Frozen boundary

| Item | Value |
|---|---|
| Worktree | `/Users/genekim/src.local/clj-surgeon-cli-mcp-causal` |
| Branch | `experiment/cli-mcp-causal-parity` |
| Candidate | `a2a928db68ea896536133c017ac439634fa070cd` |
| Model calls | 0 |
| Analyzer launches | 0 |
| Product files changed | 0 |
| Installs, reloads, shared ports, or process mutation | none |

The differential captures the intended request, rendered CLI argv and stdin
bytes, shell program bytes, parsed CLI request, intended and transmitted MCP
JSON bytes, normalized MCP request, shared-kernel outcome, and canonical
hashes.

## Measured parity

The focused run completed 6 tests and 37 assertions with zero failures in
0.40 seconds. The complete canonical report hash is
`38c338b7da8bc77fee1b6cc8417190fca9825b18fa58942868ae827565a8dc45`.

| Stratum | CLI boundary | MCP boundary | Shared fact | Result |
|---|---|---|---|---|
| Batched exact read | `:cat :spec-file -` EDN | `inspect_clojure` JSON | names, files, ranges, and source | equal; fact hash `1676a1b436ff40740acaccad832cfb16619aab1535bcaf3611ca21bad3f75145` |
| Generic intent change | `:change :spec-file -` EDN | typed `changes` JSON | transaction EDN, compiled intent, and future source hashes | all equal; compiled-intent hash `51710492cc43bd1ed06ade2c9ceb43458d0344115627e2c6a4f14b6c76f29ec3` |
| Exact selector refusal and retry | missing CLI form plus corrected stdin request | missing MCP form plus corrected JSON request | complete owner vocabulary, ranked hypotheses, authority, and retry source | equal; rank 1 `editor-gesture-schema`, `authority=false`; retry fact hash `4e99a450d827804d53b524786fae666e6baddd4270e92b93a590c0773b24faed` |

This is transport-neutral semantic parity, not byte equality between EDN and
JSON presentation formats.

## The escaping falsifier

The change fixture contains all of these in one structural payload:

- an apostrophe in `O'Reilly`;
- escaped double quotes;
- backslashes;
- a newline escape;
- nested EDN and Clojure forms.

The historical shell shape

```sh
printf '%s\n' '<EDN>'
```

failed before clj-surgeon could parse the request because the apostrophe ended
the shell's single-quoted word. The direct stdin route preserved the exact EDN.
A quoted literal heredoc also preserved it. Therefore the observed failure is
in shell materialization, not in `parse-spec-document`, the intent compiler,
or the transaction kernel.

This localizes the CLI ergonomics problem: do not ask the model to encode
structural EDN inside a shell-quoted argument. Render a request document once
and provide it on stdin or by file.

## Why the Anvil timing pilot stopped

`bench/run_inspect_mcp_benchmark.sh` has SHA-256
`61fd32f6d095bdefaa733a2653457ed0e06932f6c01a7823b96d97ceab5a8020`.
It sets a macOS-only base PATH and prepends `$HOME/bin` for the CLI arm. It does
not build a candidate wrapper or assert the CLI receipt's source commit.

A bounded read-only probe found this on every Anvil development seat:

| Seat | Visible CLI source commit | Candidate match |
|---|---|---|
| dev-a | `6ff11c9c5c127499ff121c5f4a262fe87f14f351` | no |
| dev-b | `6ff11c9c5c127499ff121c5f4a262fe87f14f351` | no |
| dev-c | `6ff11c9c5c127499ff121c5f4a262fe87f14f351` | no |

The probe only read command resolution, receipt bytes, and hashes. It did not
execute clj-surgeon, install code, mutate a repository, or start a model or
analyzer.

Retained historical evidence reported MCP at 27.969 seconds and CLI at 32.442
seconds for a typed read, a 13.8% MCP advantage. That result was below its own
keep threshold and is not a same-candidate causal result for this experiment.
It remains context, not a denominator.

## Smallest earned harness seam

Add a benchmark-only candidate entrance with these laws:

1. Build one CLI wrapper from the exact checkout that starts the isolated MCP
   server. Do not install it.
2. Give the CLI arm a private bin directory. Do not prepend `$HOME/bin`.
3. Before either arm, assert the checkout commit, wrapper SHA-256, receipt
   SHA-256, and receipt source commit.
4. Feed CLI requests through `:spec-file -`. Preserve intended EDN, exact stdin
   bytes, argv, and shell program bytes.
5. Preserve intended JSON, transmitted bytes, normalized request, and shared
   semantic hashes for MCP.
6. Use the existing read fixture, prompt, output schema, and exact scorer for
   the first serial AB pilot.
7. Publish a speed denominator only when both arms are correct and their
   semantic fact hashes match. A route recommendation still requires at least
   a 20% complete-wall win or a material correctness/recovery improvement.

The existing clean-Codex harness already demonstrates how to create a private
candidate wrapper. The earned change is to reuse that isolation mechanism in
the inspect harness, not to build another compiler or scorer.

## Artifact hashes

| Artifact | SHA-256 |
|---|---|
| `dev/experiments/cli_mcp_transport_differential.clj` | `c46e9c0e3b3a728a5ec0c97ac4ce0651d75644e808805f6a92b4bdf88e679d2b` |
| `dev/experiments/cli_mcp_transport_differential_test.clj` | `85751cbdea222bdc948736f4f4917a59d8895dfe40f01cca520cfd1cf643f370` |
| `dev/experiments/anvil_cli_commit_probe.sh` | `fe1dcbb7ab8e9c22e3de29b345be2bbd733174fecc092b8d7b3170ae851cb05f` |
| Local installed candidate wrapper | `2e0f2e422265ccb861897406aaae152318cfdb1e93abd633e8b572cbe93431da` |
| Local installed candidate receipt | `449446f23f905779e0171378a9956106013a469e1777ee30b51286278fd82ebb` |

## Preserved failed attempts

One exploratory expression guessed a nonexistent `file-ops/sha256` helper.
The final differential uses its own bounded SHA-256 function. One structural
read guessed a test owner that did not exist; the selector refusal exposed the
actual owner and the next batched read succeeded. Neither attempt changed
source. They reinforce the product lesson: exact owner vocabulary and direct
request bytes are evidence; model guesses are not.

## Recommendation

Keep the differential and receipt as the acceptance oracle for
`clj-surgeon-9xi`. Cherry-pick no product behavior from this branch. Next,
change only the benchmark's candidate-isolation seam, run its self-test, and
then run one serial same-commit read AB pilot. Do not start the broader
change/refusal timing matrix until the first pair proves identical task,
schema, scorer, code, and semantic facts.
