# Three-Arm Anvil Execution Envelope

Date: 2026-08-29

Durable owner: `clj-surgeon-45j`

Status: **NO-GO for model launch.** The existing capture boundary is proven on
Anvil, but the final `F A B B A F` harness is not yet immutable and the seat's
current quota has not been verified.

This receipt changes no product code, installation, shared runtime, or shared
port. It launches no benchmark model arm.

## Decision

Use `dev-a` as the conditional serial seat after the remaining admission gates
pass. Do not launch the three-arm cohort yet.

The existing Faraday harness successfully exercised the important boundary on
that seat:

- an exact checkout was transported by a hash-verified Git bundle;
- the zero-model suite passed;
- two fresh private capture MCP servers launched with `-J-Xms64m -J-Xmx512m`;
- the real Codex registry projected exactly one `edit_clojure` tool for both
  existing surfaces;
- the preflight reported `model_calls=0` and `mutation_actions=0`;
- both servers exited, and no process from the checkout remained.

That result proves the current two-arm envelope. It does not authorize the
future F/A/B client surfaces. The final combined harness must repeat the same
preflight for all three exact surfaces before it can spend a token.

## Immutable inputs

| Item | Identity |
|---|---|
| Three-arm protocol commit | `068fef5ca918f899225cd139e59ae84164f2b56b` |
| Protocol tree | `38ca8cb6b1317140dd9b95d8ddf4af8009fd2ee4` |
| Protocol SHA-256 | `9c94571ccb23f48b722518e76fc4ef0d111bbf7aca6349275418fceea35fd641` |
| Product basis | `ce05f6ee099ac029d96ecb6db6f5f225e4239b96` |
| Product-basis tree | `ecec5aebfa0f8adb6d76eeadaf3113ff8aeb7b3d` |
| Accepted Faraday prerequisite | `d77c65360d0784fe16dceb1c188a5c062e07ab78` |
| Faraday tree | `6f80d3e4ed1150ae4f7e73cdabc330ec5e831489` |
| Transport bundle SHA-256 | `9ebc1103a7086e31e50a5c7ebc1a13be7d9806dbd74fd753b021bc837c2c1dce` |

The exact accepted Faraday file hashes matched the protocol:

| Path | SHA-256 |
|---|---|
| `bench/run_owner_aware_call_construction_screen.sh` | `816dc5ac73f868d1d65bc50aa31660299c885e7230647f2b7079d2b6b15af2be` |
| `dev/experiments/owner_aware_call_capture_server.clj` | `688aa1bb088e4b62b0399b01cd1c10ebc5ab9b5f2271ec4e585f1b02c5eaba24` |
| `dev/experiments/owner_aware_call_capture_server_test.clj` | `4f8093500d2213cc099b89c6b43083c6b0d69466e8add63d38e807407aae5179` |
| `dev/experiments/owner_aware_call_construction_prereq.clj` | `15debe3e08a20cad7543afb19dea84b33861cc4c258c1960392b21d1881a2517` |
| `dev/experiments/owner_aware_call_construction_prereq_test.clj` | `0cd3c13d6c8593449bbab557c45461f3c5df9e101d4dd6cba7f72fd433cd9ff8` |
| `dev/experiments/owner_aware_mcp_surface_observer.clj` | `a3b8fc5b8259f14fb5ea0f306c24a013b78e9f8a1989adb1eee1a62c9160bc18` |

## Seat and toolchain

The live catalog identified this conditional seat:

```text
logical seat  dev-a
session       anvil-tmux-p32
pane          %32
composer      empty
footer        gpt-5.6-sol high
```

Read-only remote checks reported:

```text
codex login status  Logged in using ChatGPT
Codex CLI           0.147.0
Java                21.0.12
Clojure CLI         1.12.5.1664
Babashka            1.13.219
```

The seat must be cataloged again immediately before launch. A different pane,
nonempty composer, non-Sol model, non-high reasoning footer, or active turn is
a pre-token refusal.

### Quota is not yet proven

`codex login status` proves authentication, not quota. The repo-owned Anvil
runbook says the seat's TUI `/status` display is the authoritative quota
source.

An attempted `agent-bridge send` of `/status` did not execute the TUI slash
command. The bridge correctly treated the text as an ordinary agent message,
and an unintended operational Sol turn started. It was not a benchmark prompt
or experiment arm, and it produced no quota evidence. Do not repeat this
method.

The repository currently exposes no verified non-model slash-command control:

- `agent-bridge` and `anvil-session-helper` expose catalog, read, message
  delivery, recovery, ensure, and restart operations, but no raw `/status`
  action;
- the account-rotation runbook obtains `/status` by asking an agent to run it,
  which consumes an agent turn;
- the Anvil runbook prohibits raw `tmux send-keys` for prompt text. Raw tmux is
  only a bounded recovery path for control keys.

Therefore the launch owner must obtain current quota either from a human-run
TUI `/status` or from a separately approved operational agent turn. A launch
must not infer quota from login state, old history, or the model footer.

## Anvil repository and 512 MiB proof

Before this work, no `dev-a` checkout contained the exact product basis or
Faraday prerequisite. The envelope now has a dedicated detached checkout:

```text
/srv/fleet/dev-a/clj-surgeon-three-arm-envelope-d77c653
HEAD  d77c65360d0784fe16dceb1c188a5c062e07ab78
tree  6f80d3e4ed1150ae4f7e73cdabc330ec5e831489
```

The checkout also contains commit objects for the product basis and protocol.
It had a clean worktree after checkout.

The token-free suite passed on Anvil:

```text
owner-aware-call-construction-prereq-test  3 tests / 15 assertions
owner-aware-call-construction-screen-test  4 tests / 26 assertions
owner-aware-call-capture-server-test       2 tests / 11 assertions
owner-aware-mcp-surface-observer-test      4 tests / 15 assertions
total                                     13 tests / 67 assertions
failures                                  0
errors                                    0
```

The exact command then ran `--pilot --preflight-only`. It launched one fresh
private server for each existing arm using:

```text
clojure -J-Xms64m -J-Xmx512m ... :nrepl-port :none :port 0
```

Both real Codex registry projections returned:

```json
{"ok":true,"source":"codex-mcp-registry","server":"clj-surgeon","tool":"edit_clojure","normalizations":["annotations-null-empty-object","drop-top-level-any-of"]}
```

The terminal preflight receipt reported:

```text
ok                true
model_calls       0
mutation_actions  0
arms              control, candidate
git_head          d77c65360d0784fe16dceb1c188a5c062e07ab78
prerequisite SHA  8116ac8f6820dae1c3b9c6a910d5d04046447ba1aa6eb568ce5bd477670735ce
```

This is a real Anvil launch proof for the accepted two-arm capture boundary.
The final F/A/B harness must repeat it after its own immutable head exists.

## Fresh-run isolation

The accepted harness already enforces the required process and state geometry:

```text
one serial Anvil seat
  -> one run directory
     -> fresh CODEX_HOME
        -> auth.json symlink only
        -> arm-specific config.toml
     -> fresh read-only empty workspace
     -> fresh private capture server, port 0, nREPL disabled
     -> real Codex registry observation
     -> client-surface comparison
     -> model process only after every admission check passes
     -> server termination and wait
```

The future controller must use the exact palindrome `F A B B A F`. It must not
reuse a Codex home, workspace, server, or port between positions.

## Stop law

Before the first token and again before each arm, prove all of these:

1. exact product, protocol, harness, scorer, task, fixture, and compiler hashes;
2. exact model `gpt-5.6-sol` and reasoning `high`;
3. exact server provenance, exactly one enabled tool, and tool name
   `edit_clojure`;
4. exact advertised-to-SDK-to-Codex description, nested input schema, output
   schema, annotations, and total surface bytes, except only the protocol's
   preapproved arm delta and two already-proved Codex normalizations;
5. a fresh Codex home, fresh private 512 MiB server, and fresh read-only empty
   workspace;
6. current authenticated account and adequate quota.

If any identity or client-surface check fails, stop the whole cohort before
that model. Write a typed refusal and retain the evidence. Do not repair the
cohort in place, retry only the ugly position, or continue with later arms.

After a model begins, any timeout, transport failure, refusal, or malformed
capture consumes that declared position. Every attempt remains in the
denominator and archive. Never downgrade the model or reasoning level to keep
the screen moving.

## Artifact retention and copy-back

The proven preflight is retained remotely at:

```text
/srv/fleet/dev-a/clj-surgeon-three-arm-envelope-results/d77-preflight-20260829T0810Z
```

The 190-file manifest and archive were copied back without deleting the
remote evidence:

| Artifact | SHA-256 |
|---|---|
| `d77-preflight-20260829T0810Z.raw.sha256` | `e9d7265c4ce6f54928a3c5353585f77a022f047ba7255b98cf7e3b9e27e3d9d5` |
| `d77-preflight-20260829T0810Z.tar.gz` | `65572826c6e81bf7abc5c87ee2cfc6b438c6e7cb6d6dc0fae955158e57fc838d` |

Local archive directory:

```text
/Users/genekim/src.local/clj-surgeon-bench-archive/2026-08-29
```

The archive contains auth symlinks, not auth bytes. Local and remote archive
hashes are equal.

The final cohort uses the same retention law:

1. retain each arm directory, including failed or invalid attempts;
2. write a sorted SHA-256 manifest after the controller stops;
3. archive the run directory and manifest without deleting the source;
4. hash the archive on Anvil;
5. copy both files to the dated local benchmark archive;
6. verify local hashes equal the remote hashes before reporting retention;
7. do not clean the remote run until a separate retention policy authorizes it.

## Remaining admission gates

The launch remains NO-GO until all four gates pass:

1. the combined F/A/B experiment harness and pure expanders have an accepted
   immutable commit and tree;
2. that exact head is transported into the dedicated Anvil checkout and every
   controller input hash matches;
3. a fresh token-free F/A/B client-surface preflight passes with three private
   512 MiB launches and zero model calls;
4. the live seat's current quota is proven by authoritative `/status` evidence.

After those gates, the launch owner may run one whole serial six-position
screen. No smaller subset, selective retry, or mid-cohort repair is authorized.
