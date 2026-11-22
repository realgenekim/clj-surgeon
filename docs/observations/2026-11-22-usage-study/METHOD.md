# Method

Single-process Python 3 standard-library miner. No transcript command is evaluated or executed. A deterministic empirical sampling pass precedes the full streaming scan (up to 21 evenly spaced files/store, at most 2,500 retained records/file). Files are streamed one line at a time; actions are retained for the current turn, and only condensed evidence survives thereafter. Counts below refer to the full scan, not the sample. JSONL files are physical sessions, including Claude subagent files; duplicated tool IDs within a file are skipped, not deduplicated across sessions. Active session files are not frozen: this is the snapshot reached during traversal, not a simultaneous corpus snapshot.

## Counts

| metric | value |
| --- | --- |
| beads_source_absent | 1 |
| candidate_turns | 1179 |
| continuation_outputs_joined | 2114 |
| decoded_exec_wrappers | 32655 |
| dropped_secret_lines | 1108 |
| malformed_lines | 0 |
| multi_call_wrappers | 6393 |
| native_comparison_turns | 1786 |
| nested_actions_with_unattributable_output | 14711 |
| opaque_exec_wrappers | 183 |
| outcome_error | 86 |
| outcome_refusal | 220 |
| outcome_success | 1172 |
| outcome_timeout | 3 |
| outcome_unknown | 568 |
| outer_tool_calls_claude | 64786 |
| outer_tool_calls_codex | 37877 |
| runtime_seconds | 201.388 |
| sessions_scanned_claude | 680 |
| sessions_scanned_codex | 1123 |
| surgeon_calls_claude_cli | 122 |
| surgeon_calls_claude_mcp | 145 |
| surgeon_calls_codex_cli | 1627 |
| surgeon_calls_codex_mcp | 155 |
| surgeon_calls_found | 2049 |
| tool_calls_scanned_claude | 64786 |
| tool_calls_scanned_codex | 46097 |
| unclassified_outcomes | 568 |
| unreadable_sessions | 0 |
| unresolved_nested_arguments | 2309 |

## Observed record shapes

Codex stores `session_meta.payload.cwd`; `turn_context.cwd` can update it. Calls and outputs are `response_item` records using `call_id`. `arguments` can be a JSON string, while custom calls use raw `input`. Most sampled calls use an `exec` JavaScript wrapper. Both direct call types and nested calls were observed; the static lexer extracts literal `tools.NAME(...)` calls, including shell and MCP calls, without executing JavaScript. Indirect calls, computed arguments and templates with interpolation remain unresolved. Such wrapper invocations are evidence of submitted calls, not proof that a conditional branch ran. Opaque wrappers count once as other; decoded literal nested calls count individually. Multi-call wrapper outputs cannot safely be attributed to individual calls and yield unknown outcomes. Hidden/not-printed results also remain unknown. `write_stdin` and `wait` count as actions and do not inherit Surgeon identity. When a literal cell_id/session_id can be joined to its initial returned continuation identifier, the final output replaces the initial pending output for outcome and output_head; joins are counted. No final result is inferred when the join is missing.

Codex `task_started` and its initial user message are coalesced. A subsequent user message after actions begins a turn when the prior task ended, lacks a task start, or already contained a user message. `task_complete`/`turn_aborted` mark the task ended, preserving late tool output association until the next boundary. Claude assistant `message.content` lists contain `tool_use`; user lists contain matching `tool_result`. User strings or lists with non-result blocks start turns. Non-message metadata, repeated API-request snapshots and event mirror messages do not create additional calls. Dropped secret lines can remove boundaries or outputs and therefore weaken sequence evidence.

Examples below preserve observed field/block shapes, with string values replaced by `<redacted>` (including content). Only one example per distinct sampled shape is retained.

### claude/api-request

```json
{"type":"<redacted>","sessionId":"<redacted>","id":"<redacted>","timestamp":"<redacted>","version":"<redacted>","querySource":"<redacted>","shapeHash":"<redacted>","params":{"model":"<redacted>","betas":["<redacted>"],"max_tokens":128000,"thinking":{"type":"<redacted>","display":"<redacted>"},"context_management":{"edits":[{"type":"<redacted>","keep":"<redacted>"}]},"output_config":{"effort":"<redacted>"}},"keep":0,"tail":["<redacted>"],"cacheControl":[{"path":["<redacted>"],"value":{"type":"<redacted>","ttl":"<redacted>","scope":"<redacted>"}}],"requestDigest":"<redacted>"}
```

### claude/api-request-blob

```json
{"type":"<redacted>","sessionId":"<redacted>","hash":"<redacted>"}
```

### claude/api-request-shape

```json
{"type":"<redacted>","sessionId":"<redacted>","shapeHash":"<redacted>","timestamp":"<redacted>","version":"<redacted>","shape":{"system":[{"type":"<redacted>","text":"<redacted>"}]}}
```

### claude/assistant

```json
{"parentUuid":"<redacted>","isSidechain":false,"wireToolInputs":{"toolu_0195DD8LtgXnM4o8rUMrZJNH":{"file_path":"<redacted>"}},"apiBlockIndex":0,"requestId":"<redacted>","requestRef":"<redacted>","requestedModel":"<redacted>","type":"<redacted>","uuid":"<redacted>","timestamp":"<redacted>","advisorModel":"<redacted>","effort":"<redacted>","perTurnEffort":"<redacted>","userType":"<redacted>","entrypoint":"<redacted>","cwd":"<redacted>","sessionId":"<redacted>","version":"<redacted>","gitBranch":"<redacted>"}
```

### claude/atis-latch

```json
{"type":"<redacted>","atis":"<redacted>","sessionId":"<redacted>"}
```

### claude/attachment

```json
{"parentUuid":"<redacted>","isSidechain":false,"attachment":{"type":"<redacted>","snapshot":{"workingDirectory":"<redacted>","isWorktree":false,"isGitRepo":false,"additionalWorkingDirectories":[],"platform":"<redacted>","shell":"<redacted>","osVersion":"<redacted>"}},"type":"<redacted>","uuid":"<redacted>","timestamp":"<redacted>","rendered":[{"content":"<redacted>"}],"renderedRole":"<redacted>","userType":"<redacted>","entrypoint":"<redacted>","cwd":"<redacted>","sessionId":"<redacted>","version":"<redacted>","gitBranch":"<redacted>"}
```

### claude/block/text

```json
{"type":"<redacted>","text":"<redacted>"}
```

### claude/block/thinking

```json
{"type":"<redacted>","thinking":"<redacted>","signature":"<redacted>"}
```

### claude/block/tool_addition

```json
{"type":"<redacted>","tool":{"type":"<redacted>","definition":{"name":"<redacted>","description":"<redacted>","input_schema":{"type":"<redacted>","properties":{"batch":{"type":"<redacted>"},"container":{"type":"<redacted>","properties":{"kind":{"type":"<redacted>"},"id":{"type":"<redacted>"},"create":{"type":"<redacted>"}},"required":["<redacted>"]},"verbose":{"type":"<redacted>"},"opId":{"type":"<redacted>"}}},"eager_input_streaming":true}}}
```

### claude/block/tool_result

```json
{"tool_use_id":"<redacted>","type":"<redacted>","content":"<redacted>"}
```

### claude/block/tool_use

```json
{"type":"<redacted>","id":"<redacted>","name":"<redacted>","input":{"file_path":"<redacted>"},"caller":{"type":"<redacted>"}}
```

### claude/cost-state

```json
{"type":"<redacted>","sessionId":"<redacted>","totalCostUSD":1.8336172000000002,"totalAPIDuration":204509,"totalAPIDurationWithoutRetries":204484,"totalToolDuration":645,"totalLinesAdded":95,"totalLinesRemoved":25,"totalDuration":206265,"startTime":1795062686502,"modelUsage":{"claude-opus-5-5":{"inputTokens":31,"outputTokens":19836,"thinkingTokens":8956,"cacheReadInputTokens":1756826,"cacheCreationInputTokens":135676,"webSearchRequests":0,"costUSD":1.8336172000000002}},"hasUnknownModelCost":false}
```

### claude/last-prompt

```json
{"type":"<redacted>","lastPrompt":"<redacted>","leafUuid":"<redacted>","sessionId":"<redacted>"}
```

### claude/queue-operation

```json
{"type":"<redacted>","operation":"<redacted>","timestamp":"<redacted>","sessionId":"<redacted>","content":"<redacted>"}
```

### claude/user

```json
{"parentUuid":null,"isSidechain":false,"promptId":"<redacted>","type":"<redacted>","uuid":"<redacted>","timestamp":"<redacted>","permissionMode":"<redacted>","promptSource":"<redacted>","turnOrigin":"<redacted>","turnPosition":{"promptIndex":0,"turnIndex":1},"userType":"<redacted>","entrypoint":"<redacted>","cwd":"<redacted>","sessionId":"<redacted>","version":"<redacted>","gitBranch":"<redacted>"}
```

### codex/event_msg/agent_message

```json
{"timestamp":"<redacted>","type":"<redacted>","payload":{"type":"<redacted>","phase":"<redacted>","memory_citation":null}}
```

### codex/event_msg/item_completed

```json
{"timestamp":"<redacted>","ordinal":9,"type":"<redacted>","payload":{"type":"<redacted>","thread_id":"<redacted>","turn_id":"<redacted>","item":{"type":"<redacted>","id":"<redacted>","content":[{"type":"<redacted>","text":"<redacted>","text_elements":[]}]},"started_at_ms":1788562512815,"completed_at_ms":1788562512815}}
```

### codex/event_msg/patch_apply_end

```json
{"timestamp":"<redacted>","type":"<redacted>","payload":{"type":"<redacted>","call_id":"<redacted>","turn_id":"<redacted>","stdout":"<redacted>","stderr":"<redacted>","success":true,"changes":{"~/tmp/sol/gate7-sol-review.md":{"type":"<redacted>","content":"<redacted>"}},"status":"<redacted>"}}
```

### codex/event_msg/sub_agent_activity

```json
{"timestamp":"<redacted>","type":"<redacted>","payload":{"type":"<redacted>","event_id":"<redacted>","occurred_at_ms":1788472806432,"agent_thread_id":"<redacted>","agent_path":"<redacted>","kind":"<redacted>"}}
```

### codex/event_msg/task_complete

```json
{"timestamp":"<redacted>","type":"<redacted>","payload":{"type":"<redacted>","turn_id":"<redacted>","error":{"codex_error_info":"<redacted>"},"started_at":1788400828,"completed_at":1788400848,"duration_ms":19608}}
```

### codex/event_msg/task_started

```json
{"timestamp":"<redacted>","type":"<redacted>","payload":{"type":"<redacted>","turn_id":"<redacted>","started_at":1788400828,"model_context_window":258400,"collaboration_mode_kind":"<redacted>"}}
```

### codex/event_msg/thread_settings_applied

```json
{"timestamp":"<redacted>","type":"<redacted>","payload":{"type":"<redacted>","thread_settings":{"model":"<redacted>","model_provider_id":"<redacted>","approval_policy":"<redacted>","approvals_reviewer":"<redacted>","permission_profile":{"type":"<redacted>"},"cwd":"<redacted>","reasoning_effort":"<redacted>","reasoning_summary":"<redacted>","personality":"<redacted>","collaboration_mode":{"mode":"<redacted>","settings":{"model":"<redacted>","reasoning_effort":"<redacted>","developer_instructions":null}}}}}
```

### codex/event_msg/token_count

```json
{"timestamp":"<redacted>","type":"<redacted>","payload":{"type":"<redacted>","info":{"total_token_usage":{"input_tokens":16037,"cached_input_tokens":11264,"cache_write_input_tokens":0,"output_tokens":437,"reasoning_output_tokens":155,"total_tokens":16474},"last_token_usage":{"input_tokens":16037,"cached_input_tokens":11264,"cache_write_input_tokens":0,"output_tokens":437,"reasoning_output_tokens":155,"total_tokens":16474},"model_context_window":258400},"rate_limits":{"limit_id":"<redacted>","limit_name":null,"primary":{"used_percent":22.0,"window_minutes":10080,"resets_at":1788786616},"secondary":null,"credits":{"has_credits":false,"unlimited":false,"balance":"<redacted>"},"individual_limit":null,"spend_control_reached":null,"plan_type":"<redacted>","rate_limit_reached_type":null}}}
```

### codex/event_msg/user_message

```json
{"timestamp":"<redacted>","type":"<redacted>","payload":{"type":"<redacted>","images":[],"local_images":[],"audio":[],"local_audio":[],"text_elements":[]}}
```

### codex/inter_agent_communication_metadata

```json
{"timestamp":"<redacted>","type":"<redacted>","payload":{"trigger_turn":true}}
```

### codex/response_item/additional_tools

```json
{"timestamp":"<redacted>","ordinal":2,"type":"<redacted>","payload":{"type":"<redacted>","id":"<redacted>","role":"<redacted>"},"metadata":{"client_authored":false,"mcp_attribution":{"status":"<redacted>"}}}
```

### codex/response_item/agent_message

```json
{"timestamp":"<redacted>","type":"<redacted>","payload":{"type":"<redacted>","id":"<redacted>","author":"<redacted>","recipient":"<redacted>","content":[{"type":"<redacted>","text":"<redacted>"}],"internal_chat_message_metadata_passthrough":{"turn_id":"<redacted>"}}}
```

### codex/response_item/custom_tool_call

```json
{"timestamp":"<redacted>","type":"<redacted>","payload":{"type":"<redacted>","id":"<redacted>","status":"<redacted>","call_id":"<redacted>","name":"<redacted>","input":"<redacted>","internal_chat_message_metadata_passthrough":{"turn_id":"<redacted>"}}}
```

### codex/response_item/custom_tool_call_output

```json
{"timestamp":"<redacted>","type":"<redacted>","payload":{"type":"<redacted>","id":"<redacted>","call_id":"<redacted>","output":[{"type":"<redacted>","text":"<redacted>"}],"internal_chat_message_metadata_passthrough":{"turn_id":"<redacted>"}}}
```

### codex/response_item/function_call

```json
{"timestamp":"<redacted>","type":"<redacted>","payload":{"type":"<redacted>","id":"<redacted>","name":"<redacted>","namespace":"<redacted>","arguments":"<redacted>","call_id":"<redacted>","internal_chat_message_metadata_passthrough":{"turn_id":"<redacted>"}}}
```

### codex/response_item/function_call_output

```json
{"timestamp":"<redacted>","type":"<redacted>","payload":{"type":"<redacted>","id":"<redacted>","call_id":"<redacted>","output":"<redacted>","internal_chat_message_metadata_passthrough":{"turn_id":"<redacted>"}}}
```

### codex/response_item/message

```json
{"timestamp":"<redacted>","type":"<redacted>","payload":{"type":"<redacted>","id":"<redacted>","role":"<redacted>","content":[{"type":"<redacted>","text":"<redacted>"}],"internal_chat_message_metadata_passthrough":{"turn_id":"<redacted>"}}}
```

### codex/response_item/reasoning

```json
{"timestamp":"<redacted>","type":"<redacted>","payload":{"type":"<redacted>","id":"<redacted>","internal_chat_message_metadata_passthrough":{"turn_id":"<redacted>"}}}
```

### codex/session_meta

```json
{"timestamp":"<redacted>","type":"<redacted>","payload":{"session_id":"<redacted>","id":"<redacted>","timestamp":"<redacted>","cwd":"<redacted>","originator":"<redacted>","cli_version":"<redacted>","source":"<redacted>","thread_source":"<redacted>","model_provider":"<redacted>","history_mode":"<redacted>","context_window":{"window_id":"<redacted>"},"git":{"commit_hash":"<redacted>","branch":"<redacted>","repository_url":"<redacted>"}}}
```

### codex/token_usage_record

```json
{"timestamp":"<redacted>","ordinal":13,"type":"<redacted>","payload":{"thread_id":"<redacted>","turn_id":"<redacted>","session_id":"<redacted>","root_turn_id":"<redacted>","response_id":"<redacted>","turn_token_usage":{"input_tokens":15598,"cached_input_tokens":12160,"cache_write_input_tokens":0,"output_tokens":104,"reasoning_output_tokens":0,"total_tokens":15702},"thread_token_usage":{"input_tokens":15598,"cached_input_tokens":12160,"cache_write_input_tokens":0,"output_tokens":104,"reasoning_output_tokens":0,"total_tokens":15702}}}
```

### codex/turn_context

```json
{"timestamp":"<redacted>","type":"<redacted>","payload":{"turn_id":"<redacted>","cwd":"<redacted>","workspace_roots":["<redacted>"],"current_date":"<redacted>","timezone":"<redacted>","approval_policy":"<redacted>","approvals_reviewer":"<redacted>","sandbox_policy":{"type":"<redacted>"},"permission_profile":{"type":"<redacted>","file_system":{"type":"<redacted>","entries":[{"path":{"type":"<redacted>","value":{"kind":"<redacted>"}},"access":"<redacted>"}]},"network":"<redacted>"},"model":"<redacted>","comp_hash":"<redacted>","personality":"<redacted>","collaboration_mode":{"mode":"<redacted>","settings":{"model":"<redacted>","reasoning_effort":null,"developer_instructions":null}},"multi_agent_version":"<redacted>","realtime_active":false}}
```

### codex/world_state

```json
{"timestamp":"<redacted>","type":"<redacted>","payload":{"full":true,"state":{"agents_md":{"directory":"<redacted>","text":"<redacted>"},"apps_instructions":false,"environments":{"environments":{"local":{"cwd":"<redacted>","status":"<redacted>","shell":"<redacted>"}},"current_date":"<redacted>","timezone":"<redacted>","filesystem":"<redacted>"},"environments_instructions":false,"git_attribution":false,"host_skills":{"body":"<redacted>","includeInstructions":true},"model":"<redacted>","multi_agent_mode":{"mode":"<redacted>","usage_hint_hash":"<redacted>"},"multi_agent_usage_hint":"<redacted>","orchestrator_skills":{"includeInstructions":true,"enabled":false},"permissions":{"instructions":"<redacted>","approved_command_prefixes":[]},"personality":{"model":"<redacted>","personality":"<redacted>"},"plugins_instructions":false,"realtime":{"active":false},"skills":{"includeInstructions":true}}}}
```

## Detection and classifiers

Only executed-tool records enter the action stream. Surgeon MCP recognition tests tool names for clj-surgeon, clj_surgeon, namespace_split or require_change. CLI recognition lexes shell command segments, strips heredoc bodies and comments, and requires an executable token named clj-surgeon (possibly a path); ordinary prose and a path argument to cat/rg do not count. Operation is the token after `:op`, or ls for `:ls`; a bin/clj-surgeon invocation without these retains its first argument. Multiple explicit operations in one shell action yield multiple calls with the same action position. Invocation via arbitrary shell functions, dynamically built commands, bb -m launchers and substitutions inside quoted strings is outside this declared recognizer. Shell control structures/wrapper options can cause misses. Static JS branches may overcount; this is recorded as a limit, not silently called confirmed execution.

Native edits are apply_patch/Edit/MultiEdit/Write/NotebookEdit, or the SPEC-A shell edit patterns (including python heredocs, which may be read-only). Native reads use the named read tools or shell read commands. Verification recognizes SPEC-A command names and git diff/status. Edit takes precedence over verify, then read, then other for mixed shell actions. Next action is the immediately following decoded action; same/other is by operation. Fallback means any later native edit before the next Surgeon action in the same turn, even with intervening reads or tests. Retrying means immediate surgeon-same next action; repaired means that retry has positively classified success. A fallback is a sequence observation, not a causal judgment.

Output classification uses top-level JSON/EDN receipt fields and initial terminal summaries, never nested returned source. Precedence: explicit timeout; typed refusal or refused terminal summary; machine error/is_error/nonzero exit or explicit exception; explicit success/commit/read-complete or exit zero; unknown. Structured MCP content is unwrapped when available. Valid show-form/list-forms receipts are successful reads. A transport exit zero alone proves tool transport completion, not semantic correctness. Verification-incomplete receipts can therefore be success here but do not prove the user task completed. Embedded source strings and nested fixtures are excluded from status evidence. Mixed-command output can still hide the relevant receipt; this is an attribution limit. Codes are extracted from top-level refusal_code/refusal-type, code/kind/error-type fields and the observed `TOOL refused · CODE` summary; booleans/generic status words are rejected. Missing codes remain blank. No output or unattributable wrapper output is unknown. calls.tsv retains the first 300 characters, so classifications can rely on evidence beyond that prefix. No successful outcome is inferred solely from a missing error.

All original transcript lines are checked for the specified token patterns and quoted password/secret keys followed by a value BEFORE parsing, sampling, excerpting or writing. Entire matching lines are discarded. dropped_secret_lines counts the full scan only. Retained excerpts replace ~ with ~ and other home prefixes with <home>; tabs/newlines are flattened. input_shape recursively truncates every string value at 60 characters, compact-serializes, then caps at 300. These clipped strings can cut JSON syntax at the final cap as requested. Friction excerpts use retained literal text, except path redaction, flattening and truncation. Patterns with one occurrence cannot supply two independent examples and are marked accordingly.

## Controls, matching and limits

Native controls are all tool-bearing turns without detected Surgeon calls that explicitly edit at least one .clj/.cljc/.cljs/.edn file. File paths are extracted from Edit/Write arguments, apply_patch headers, and recognized shell edit commands; source files are never opened. Relative edited paths normalize against cwd lexically. Repository is cwd's last component, or the repository component after worktrees/.worktrees. This heuristic can split nested directories of one repo or conflate equal basenames. File buckets are 1, 2-3, 4-9 and 10+. Surgeon turns with no observed native Clojure edits are bucket 0: Surgeon-target paths are not assumed to be writes and cannot be matched to native controls by touched files.

The native block of actions_to_end.tsv groups by repo and file bucket. surgeon-matched rows use identical repo/bucket keys for observed native edits in Surgeon turns; global surgeon rows remain per op. This is stratification, not randomized matching or a causal estimate. Native measures whole-turn tools; Surgeon measures suffix tools from the call inclusive. Task intent, model, difficulty, date and success are unmatched. Missing/dynamic edit paths and unrecognized Surgeon calls can contaminate controls. Therefore medians cannot establish speedup, correctness, first-attempt performance or a wall-time win. p90 uses nearest rank. Calls counts Surgeon calls in Surgeon blocks and native turns in native blocks.

candidates.md explains mutually exclusive ordered lexical heuristics and weaknesses. native_turns.tsv contains every control turn, its class and action/file counts; candidates count rows with >=2 native edit actions and either >=2 files or a single-file extract-function classification. retry_evidence.tsv retains per-call retry/repair/file-bucket data in calls.tsv order. counts.tsv, tool_names.tsv and turn_counts.tsv make full-scan counts auditable. Weekly session counts are distinct basenames within a week/store/surface and should not be summed to obtain unique sessions. Papercut coverage reads the pinned git object (b2beec26) without modifying that repository, plus the specified beads file when available. Keyword coverage is advisory, uses diagnostic output/op/code only, and does not assert semantic equivalence. Missing beads or papercuts are recorded in counts.

Instrumentation repair: an initial audit caught returned Clojure source being mistaken for outcome evidence. The classifier was repaired to use top-level receipts/terminal summaries, regression-checked, and the complete corpus rerun; every table here comes from the replacement run alone. Secret scanning also checks escaped quoted keys.

No files are committed. The miner writes only its output directory. Subprocess use is restricted to one read-only git show after the scan; no parallelism. Runtime covers sampling, full scan and table generation through count finalization. It excludes the final METHOD write and console output.
