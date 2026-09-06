# Astra token study: surgeon MCP vs native edits — report

Generated: 2026-09-06. Script: `study.py` in this directory. Raw per-turn data: `turns.csv`.
Read-only analysis; nothing outside `/var/tmp/forge/astra-token-study/` was written.

## Headline finding

**The hypothesis could not be tested from these 4 rollout files.** Across all four
files combined, there are **zero** `function_call` or `custom_tool_call` events whose
tool name is a clj-surgeon MCP tool (`inspect_clojure`, `apply_clojure_changes`,
`edit_clojure`, `transform_clojure`, or any name containing `clj-surgeon`/`clj_surgeon`).
The comparison group "turns that used >=1 surgeon MCP tool AND made an edit" has **n = 0**
in every file. Mentions of "clj-surgeon" / "apply_clojure_changes" etc. do appear as
plain text (754+318+169+164 grep hits across the four files) but those are prose --
agents quoting doctrine, reporting other agents' results, or citing house-rules text --
not actual tool invocations.

## 1. Schema discovery (largest file, 9.6 MB, ...01a07467...astra...)

Top-level `type` values and counts:

| type | count |
|---|---|
| event_msg | 1751 |
| response_item | 1569 |
| token_usage_record | 515 |
| inter_agent_communication_metadata | 78 |
| turn_context | 24 |
| world_state | 4 |
| compacted | 3 |
| session_meta | 2 |

`event_msg` payload `type` counts: `item_completed` 1167, `token_count` 540,
`task_started` 21, `task_complete` 20, `thread_settings_applied` 3. **No `user_message`
payload type exists in this file** -- this harness variant marks turn boundaries with
`task_started`/`task_complete`, not chat-style `user_message` events (that shape only
appears in the two smaller worker files, see below).

`response_item` payload `type` counts: `reasoning` 439, `custom_tool_call` 360,
`custom_tool_call_output` 360, `function_call` 135, `function_call_output` 135,
`agent_message` 78, `message` 62.

Tool names actually invoked (by count):

| kind | name | count |
|---|---|---|
| custom_tool_call | exec | 360 |
| function_call | send_message | 97 |
| function_call | wait_agent | 35 |
| function_call | followup_task | 3 |

The token-accounting shape matches the hypothesis: `event_msg` payloads of type
`token_count` carry `info.total_token_usage` (cumulative) and `info.last_token_usage`
(per-response) with `input_tokens`, `cached_input_tokens`, `output_tokens`,
`reasoning_output_tokens`, `total_tokens` -- used exactly as described in the task brief.

## 2. Session identification

| file | session_id | cwd | model | first_ts | last_ts | role |
|---|---|---|---|---|---|---|
| ...T01-49-24...a07467... | 01a07467-f3ae-... | /home/forge/src/clj-surgeon | gpt-6-astra | 2026-09-06T01:49:24Z | 2026-09-06T07:20:45Z | Astra -- fleet coordinator |
| ...T00-32-05...a07421... | 01a07421-2b54-... | /home/forge/src/clj-surgeon | gpt-5.6-luna | 2026-09-06T00:32:05Z | 2026-09-06T07:15:11Z | Luna -- a different named coordinator agent, referenced alongside Astra |
| ...T18-37-52...df4a... | 01a072dc-df4a-... | /home/forge/src/clj-surgeon-spark2 | gpt-5.3-codex-spark | 2026-09-05T18:37:53Z | 2026-09-05T18:46:13Z | Spark worker |
| ...T18-37-52...df14... | 01a072dc-df14-... | /home/forge/src/clj-surgeon-spark1 | gpt-5.3-codex-spark | 2026-09-05T18:37:52Z | 2026-09-05T18:46:21Z | Spark worker |

**Only the first file is confirmed to be Astra** by its recorded `model` field
(`gpt-6-astra`). The string "astra" appears 14 times in file 1's own message text
(mostly relays it received about Astra/Fable work) and 21 times in the Luna file
(Luna discussing/coordinating with Astra) -- those are cross-references, not proof of
identity for the Luna file. Neither `astra/typist-route` nor bare `typist-route`
appears literally in any of the four files. The two Spark files are 8-minute,
single-task worker sessions in sibling worktrees (clj-surgeon-spark1/spark2), not
Astra sessions.

**Role split matters for the hypothesis.** The two long files (Astra, Luna) are
**fleet-coordinator loops**: their only tool calls are `exec` (shell) plus
`send_message`/`wait_agent`/`followup_task` (dispatching work to and waiting on other
agent sessions -- sessions not included in this file set). They never call `apply_patch`
and never call a clj-surgeon MCP tool directly. The two Spark files are workers that
did edit directly, but only via native tools: `exec_command` (100/95 calls) and
`apply_patch` (16/22 calls) -- also no MCP surgeon tool calls.

## 3. Turn segmentation

Turn boundary = event_msg payload type `user_message` OR `task_started` (the Astra/
Luna files only emit `task_started`; the Spark files emit one `user_message` each).
50 turns total: 21 (Astra) + 25 (Luna) + 2 (Spark2) + 2 (Spark1).

Token delta per turn = difference in `info.total_token_usage` (cumulative snapshot)
between the last `token_count` event before the turn and the last one inside it --
this is the metric used for the headline numbers below. `sum_last_input/output/...`
(sum of `last_token_usage` fields seen inside the turn) is also recorded per turn in
turns.csv as a cross-check; the two mostly track the cumulative-delta figures but
diverge on turns that included a context-compaction event (`compacted`/
`context_compacted`), where the cumulative counter can reset or jump -- visible in the
CSV as two negative input_tokens_delta rows for the Luna file.

## 4. Comparison: surgeon-tool turns vs native-only turns (edits only)

| group | n turns | median output tok | mean output tok | median input tok | mean input tok | median total tok | median tool calls/turn |
|---|---|---|---|---|---|---|---|
| surgeon (>=1 MCP surgeon tool, edit made) | 0 | -- | -- | -- | -- | -- | -- |
| native (shell/apply_patch only, edit made) | 2 | 55,925 | 55,925 | 6,743,639 | 6,743,639 | 6,799,564 | 116.5 |

The "native" group is both Spark files' single qualifying turn each (each file's whole
8-minute run collapsed into one turn because there is only one user_message/
task_started boundary per file). n=2 is too small to report meaningfully beyond the
raw numbers already shown; there is no surgeon-side data point to compare it against.

Across all 50 turns (edit and non-edit, all four files), zero turns contain a surgeon
MCP tool call. The comparison the task asked for cannot be produced from this input.

## 5. Limitations (read before trusting anything above)

- **No surgeon MCP tool calls exist in the supplied files, at all.** This is not a
  sampling artifact of turn segmentation -- it was confirmed independently via raw
  `grep -c` counts on tool-name JSON literals (`"apply_patch"`, clj_surgeon,
  inspect_clojure, apply_clojure_changes, edit_clojure, transform_clojure)
  across all four files, cross-checked against the parsed function_call/
  custom_tool_call name tallies. Every surgeon-related string found is inside
  message/reasoning text, never a tool name.
- **These four files are the wrong shape to test the hypothesis.** Two are
  orchestrator loops (Astra, Luna) that dispatch to other agents rather than editing
  directly; two are short single-task Spark workers that only used shell+apply_patch.
  If Astra's own direct MCP surgeon usage exists, it is in session files not included
  here (likely files with function_call names containing inspect_clojure/
  apply_clojure_changes/etc., findable by grepping ~/.codex/sessions/**/*.jsonl
  for those literal tool names before assuming any given file is relevant).
- **Turns are not matched tasks.** Even where edit-turns exist (the two Spark turns),
  each is a whole 8-minute single-task session collapsed to one "turn" by this
  harness's boundary markers, not a matched-difficulty unit -- task size, not tool
  choice, likely dominates any token difference.
- **History proves what was said/done, not outcome quality.** A tool-call count and a
  token delta say nothing about whether the resulting edit was correct, required
  fewer follow-up turns, or would generalize; this study cannot speak to net
  efficiency of a surgeon-using workflow, only counts.
- **Cumulative-counter deltas can go negative across a compaction event** (seen twice
  in the Luna file), a known hazard of computing turn deltas by subtracting
  total_token_usage snapshots rather than summing last_token_usage; both are kept
  in turns.csv so a reviewer can pick either metric.

## Files

- /var/tmp/forge/astra-token-study/study.py -- analysis script (schema discovery,
  session ID, turn segmentation, comparison).
- /var/tmp/forge/astra-token-study/turns.csv -- 50 per-turn rows (file, turn index,
  timestamps, tool-call counts by kind, token deltas). No message text.
- /var/tmp/forge/astra-token-study/_intermediate.json -- machine-readable summary
  used to build this report.
