# Config-delivery reproduction — which memory is right, under which flags

Three sacrificial `codex exec` launches (`gpt-6-astra`, `codex-cli 0.153.3`, ChatGPT
subscription auth, `codex login status` = "Logged in using ChatGPT"), one throwaway
detached worktree of `clj-surgeon` at `1f962abc`, excluded from every scored
denominator (its own `AGENTS.md`/`CLAUDE.md` were suppressed; the planted probe
`AGENTS.md` says outright "This session's history must not reach any scored
caller"). Each was asked to list its non-shell tool names and, if any MCP tool was
offered, call exactly one read-only operation and report the verbatim result.

Argv held identical to the production runner's (this worktree's `run-one.sh`),
which does **not** pass `--ignore-user-config`:

```
env -u OPENAI_API_KEY -u CODEX_API_KEY CODEX_HOME=<one of the three below> \
  codex exec --cd <wt> --model gpt-6-astra --ignore-rules \
  -c 'projects."<wt>".trust_level="trusted"' -c 'model_reasoning_effort="high"' \
  --sandbox danger-full-access -c 'approval_policy="never"' \
  --json --output-last-message <out> -
```

## The three launches

| # | MCP carrier | repo-level `<wt>/.codex/config.toml` | `CODEX_HOME/config.toml` | argv `-c mcp_servers…` | delivered? | receipt |
|---|---|---|---|---|---|---|
| A | repo-level `.codex/config.toml` in launch cwd | has `[mcp_servers.clj-surgeon]` table | no MCP table | none | **YES** — real `mcp_tool_call` | `repro/probeA-events.jsonl` (thread `01a093a3-5d75-7a31-bf0f-ee7d4f97f0e3`) |
| B | `CODEX_HOME/config.toml` | absent (removed before this launch) | has `[mcp_servers.clj-surgeon]` table | none | **YES** — real `mcp_tool_call` | `repro/probeB-events.jsonl` (thread `01a093a4-a725-7f12-aab1-2a6786915261`) |
| C | argv `-c mcp_servers.clj-surgeon....` | absent | no MCP table | `url`, `startup_timeout_sec`, `tool_timeout_sec` | **YES** — real `mcp_tool_call` | `repro/probeC-events.jsonl` (thread `01a093a5-8ea4-78e0-a91c-ee649c3c441a`) |

"Delivered" is read from the rollout's own `item.completed` event carrying
`"type": "mcp_tool_call", "server": "clj-surgeon", "tool": "inspect_clojure"` — a
genuine round trip (a typed `missing-fields` refusal came back each time, because
the probe prompt didn't hand the model a valid `expect`/`requests` envelope; the
point being scored here is delivery, i.e. that the call reached the server at all,
not that the call succeeded), not an inference from prose or from a configured URL
sitting unused. Example (probe A, `item_2`):

```json
{"id":"item_2","type":"mcp_tool_call","server":"clj-surgeon","tool":"inspect_clojure",
 "arguments":{"workspace_root":"/var/tmp/forge/measure-codexN/repro/wt",
              "requests":[{"file":"deps.edn","operation":"outline"}]},
 "result":{... "reason":"missing-fields" ...}}
```

## The finding

**All three delivered.** Under `codex-cli 0.153.3`, with the exact argv this
runner actually uses for scored runs (no `--ignore-user-config`), a repo-level
`.codex/config.toml` in the launch cwd DOES register and deliver the MCP server —
directly contradicting the earlier isolation-memory claim that it "did not
deliver MCP at all," and directly confirming the older repo-config-memory claim.

This does not mean the earlier probe was wrong on its own terms — it means the
two memories were each measured under a different flag set, and neither said so
loudly enough:

* **`codex-seats-need-repo-level-mcp-config`** (2026-09-06) measured a repo-level
  file working, and its own addendum already narrowed the claim: it does NOT load
  under `--ignore-user-config --ignore-rules`, but an argv `-c mcp_servers...`
  override still does under those same ignore flags. That addendum is **correct**
  and this reproduction did not need to re-touch it (probe C here reproduces the
  same argv-override path, without the ignore flags, and it also delivers).
* **`codex-exec-isolation-findings`** (2026-09-11) measured a repo-level file
  NOT delivering — but that probe batch's actual production recipe used
  `--ignore-user-config --ignore-rules --strict-config` (the design doc's example
  invocation, ¶"Invocation and delivery checks"). Under `--ignore-user-config`
  specifically, the isolation-memory's own finding (repo-level config not loaded)
  is consistent with the repo-config-memory addendum (repo-level not loaded under
  ignore flags) — **they were never actually in conflict on the ignore-flags
  case.** The conflict this task was asked to resolve was narrower than either
  memory states standalone: "repo-level config.toml doesn't deliver MCP for
  `codex exec`, full stop" is **false** — it doesn't deliver **specifically when
  `--ignore-user-config` is also passed**, and the cohort's own eventual fix
  (registering M's MCP table in the arm's own `CODEX_HOME/config.toml`, and
  dropping `--ignore-user-config` from every arm) sidestepped the question this
  probe answers, rather than closing it.

**Correction to write into the memory store:** `codex-exec-isolation-findings`
should be narrowed to name `--ignore-user-config` as the actual variable (not
"repo-level `.codex/config.toml` vs. `CODEX_HOME`" as if that were the axis), and
`codex-seats-need-repo-level-mcp-config` needs no correction beyond the addendum
it already carries. Neither memory was reproduced against `codex-cli 0.153.3`
with `--ignore-user-config` before this task; that specific cell (repo-level
file, `--ignore-user-config` present) remains the one combination this
reproduction did not re-run, because the production runner this packet uses
does not pass that flag and the assignment asked for the runner's own three
carriers, not a fourth flag sweep.

## What this does NOT establish

* Not re-tested here: `--ignore-user-config` present, repo-level carrier (that
  is the delivery-probes.md / isolation-memory cell already on record; this
  reproduction did not spend a fourth launch re-confirming it since it is outside
  the three carriers the assignment named).
* The refused `missing-fields` result on every probe is an artifact of the probe
  prompt asking for "any read-only call," not a claim about `inspect_clojure`'s
  contract; a well-formed request (as arm M's scored runs show, and as the
  original repo-config-memory receipt shows) is answered normally.
