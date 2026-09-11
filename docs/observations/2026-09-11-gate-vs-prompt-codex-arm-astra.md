# Codex arm: Astra owns these bets

Design only, gpt-6-astra, evidence cutoff 2026-09-11T03:03:27Z; no callers launched by this consult.
Run the same frozen 12 tasks plus example-R-e4 on **gpt-6-astra and gpt-5.6-sol**, in fresh sessions. This review session is not a scored subject and its history must not reach them.
My thesis: a concise plate plus a delivered MCP entrance will cause more Surgeon completions than a PATH refusal; a refused shell writer often chooses `apply_patch`. Astra's census 71% and Sol's 3% are call shares from mixed historical work, not probabilities for these tasks.
Scope correction: these seats have no configured experimental hook layer. Installed `codex-cli 0.153.3` exposes hook-related flags; “Codex cannot have hooks” is not established. Freeze all native hooks off here; test the requested PATH mechanism independently.

## Arms and exact worktree planting contract

Use six arms, 13 tasks × 2 models = **156 scored runs**, paired by task/model, plus six identical N/T-03 calibrations per model = **168 scheduled runs**. Freeze a seeded block order before launch, balance order across models/arms, and cap concurrency at four including other cohort work.
Common to every worktree: `AGENTS.md`, `.codex/config.toml`, `.cohort/bin/sed`, `.cohort/bin/python3`, `.cohort/shim-policy.json`, `.cohort/manifest.json`; initially empty `.cohort/requests/`. No project skill or Claude hook files are added.
The two common executable shims are byte-identical across arms: bounded recognizer, append-only external journal, then exec the pre-resolved real executable when allowed. The policy selects observe/bare/repair; observe never blocks. Observer overhead is therefore present in controls too.
Freeze original repo instruction/config files externally; suppress all other effective AGENTS/override files, routing skills, plugins, CLAUDE imports and MCP registrations in the isolated launch view, identically across arms. Record each suppressed path. Preserve source/test bytes and normal build configuration.
Do this in a clean worker runtime with subscription authentication at its normal location; do not edit the live seat's global instructions/config. `--ignore-user-config` alone does not isolate user AGENTS or skills. Refuse preflight if the effective instruction/tool inventory differs from this manifest.

| Arm | AGENTS content | Additional files and common-policy values | Contrast |
|---|---|---|---|
| N | neutral text below | observe; no Surgeon binary, no MCP table | competent native availability control |
| L | neutral + availability line | observe; `.cohort/bin/clj-surgeon` | L−N: request-file CLI availability |
| P | L text + plate below | identical to L | P−L: plate with CLI held fixed |
| M | identical to P | P files; add Surgeon MCP table to `.codex/config.toml` | M−P: MCP availability with plate fixed |
| X0 | identical to L | L files; policy bare | X0−L: bounded obstruction |
| X1 | identical to L | L files; policy repair; `.cohort/repair-templates.json` | X1−X0: repair bundle on same obstruction |

Neutral AGENTS text, verbatim: “Complete the supplied task using the fastest safe available method. Preserve unrelated source. Run the repository's required checks. A successful write is not proof of behavior. Use /var/tmp for scratch files and ~/bin/clj-kondo for lint. Do not delegate, commit, or push.”
L availability line, verbatim: “The optional clj-surgeon CLI is on PATH; its help documents request-file operations. Native tools remain available.”
Plate appended only in P/M, verbatim: “For this experiment, use clj-surgeon for insertion beside a named form or inside a deftest, require-alias/use changes, and deftest-body replacements. Insertion: `clj-surgeon :insert-forms! :request-file X`. Other edits: use the documented bounded `within` + `from`/`to` contract with expected matches. Prefer MCP when offered, otherwise the CLI request-file entrance. Write request files under `.cohort/requests/`. Read mutation status before recovery; repair one clear argument refusal once, then use an exact native patch if needed. Finish required verification.”
This is a routing treatment, not unprompted voluntary adoption. L−N changes availability plus its minimal announcement; M−P includes MCP schema/context cost and is not a pure wire-latency comparison.

`.cohort/bin/clj-surgeon` is an executable launcher pinned to the same immutable implementation for L/P/M/X0/X1; it passes argv unchanged, journals request/receipt hashes externally, and never silently translates unsupported operations. Resolve and hash its full executable/dependency chain, not only a moving symlink. N's PATH has no second Surgeon entrance.
The builder must freeze exact successful request-file witnesses for all advertised task families, including deftest ownership and E4's alias scope, before scoring. Do not assume the insertion CLI implies a general edit request-file command exists. If that entrance is absent, report a design delivery blocker before spending runs, rather than add an unmeasured adapter mid-cohort.
All `.codex/config.toml` files contain the same settings: `approval_policy = "never"`, `sandbox_mode = "danger-full-access"`, `model_reasoning_effort = "high"`, `web_search = "disabled"`; `[shell_environment_policy]` with `inherit = "all"` and `set = { PATH = "<absolute frozen run PATH>" }`, expanded mechanically per worktree.
Only M adds this table (7906 is the apparatus endpoint, not permission to restart it):

```toml
[mcp_servers.clj-surgeon]
url = "http://127.0.0.1:7906/mcp"
startup_timeout_sec = 30
tool_timeout_sec = 120
```

## Invocation and delivery checks

Read-only local checks here established CLI 0.153.3, the flags below, and `codex login status` = “Logged in using ChatGPT.” Model authorization in a new isolated worker remains a preflight check; never substitute another model or silently use the API.
In the clean worker, preserve saved subscription auth and unset API-key overrides. Keep the worker's normal HOME/CODEX_HOME; capture auth mode without copying credentials into artifacts. Project config is loaded only for trusted projects; set trust for the actual resolved checkout/project root, including linked-worktree root resolution. [Configuration precedence](https://learn.chatgpt.com/docs/config-file/config-basic).
For each cell the runner constructs argv directly; `TRUST_ARG` is TOML `projects."<resolved project root>".trust_level="trusted"`, with the path serialized, never evaluated as shell code. `PINNED_PATH` begins with the worktree's `.cohort/bin` and contains only the frozen required executables.
Run from the worktree itself and also pass `--cd`; use a new process, never resume/fork an existing session:

```bash
env -u OPENAI_API_KEY -u CODEX_API_KEY PATH="$PINNED_PATH" \
  "$CODEX_BIN" exec --cd "$WT" --model "$MODEL" \
  --ignore-user-config --ignore-rules --strict-config \
  -c "$TRUST_ARG" -c model_reasoning_effort='"high"' \
  --sandbox danger-full-access -c approval_policy='"never"' \
  --json --output-last-message "$OUT/final.txt" - \
  < "$TASK_PROMPT" > "$OUT/events.jsonl" 2> "$OUT/stderr.log"
```

`MODEL` is exactly `gpt-6-astra` or `gpt-5.6-sol`; `CODEX_BIN` is the attested absolute executable (currently `/home/forge/.local/bin/codex`). The runner's subprocess cwd is WT. No `--ephemeral`: retain the raw rollout and map its thread ID to OUT. Saved auth reuse and JSON event output are documented in [noninteractive mode](https://learn.chatgpt.com/docs/non-interactive-mode).
Separate excluded smoke sessions must prove resolved model/effort, effective AGENTS text, clean skill/plugin inventory, actual shell PATH after shell startup, and absence/presence of MCP tools. `command -v` outside Codex is insufficient. M must list the required tool schemas and complete one real read on a sacrificial fixture using WT's absolute workspace root. Merely configuring a URL is insufficient. [MCP configuration](https://learn.chatgpt.com/docs/extend/mcp?surface=cli).
Archive server identity/commit/catalog hashes and CLI identity; freeze both throughout. Restart each Codex caller after config changes; do not restart a shared server. A failed launch attestation remains a delivery failure in the assigned-run ledger, not a zero-adoption result; no hidden replacements.

## The PATH refusal is deliberately bounded

Freeze the recognizer to `sed -i` with resolved literal protected-file operands, and Python `-c`, stdin or script-file programs whose static subset resolves a protected path and an explicit write sink (`open` write modes / pathlib write calls). Recognize `.clj/.cljc/.cljs/.edn` source; exempt request-only files under `.cohort/requests/`. Preserve read-only behavior and exit codes.
X0 and X1 make identical block decisions, exit 2 before invoking the interpreter, and journal argv/stdin/script hash, resolved target, pre-hash and reason. X0 prints only `not run`. X1 prints: “Not run: this command has no form-preservation receipt. Source unchanged by this interpreter. Use the following bounded request-file entrance; inspect mutation status and finish the repository checks.” Then emit the frozen filled request example and exact invocation when its target/owner are resolvable; otherwise explicitly say target unresolved and give documented discovery.
The request renderer may use only the attempted command and common source/task information; never the held-out answers. Its templates and selection rules are frozen before scoring, including working CLI examples. `.cohort/repair-templates.json` is an additional X1 artifact; if a caller reads it before refusal, tag anticipatory exposure and do not credit a later shift solely to the boundary message.
This shim is not a filesystem security boundary. Absolute `/usr/bin/python3`, other interpreters, `tee`, generated scripts outside the recognized subset and native `apply_patch` remain escapes. Shell redirection can truncate a target BEFORE a shim starts; record that as prior damage, never a prevented write. Do not promise zero program writes.
Preflight refusal/allow matrices and unchanged-byte witnesses cover supported shapes, request-file writing, read-only scripts and redirect counterexamples. Harness/oracle Python uses an absolute real interpreter and never passes through the treatment shim. L's observe journal estimates exposure opportunity without forcing it.

## Outcomes and rollout classification

Use the corrected common 900-second deadline through required verification and independent acceptance, including queueing, repair and startup. Kill only the process group the runner created. Archive first final submission before private oracle feedback; no answer-key repairs. Gate-v2 commands/baselines and oracles are identical across cells; retain all worktrees through cohort end.
Parse actual rollout records (`response_item` function/custom calls and their outputs), plus `--json` completed command/file-change/MCP events; decode nested `functions.exec` calls. Join by IDs to the external shim/CLI/server journals. Do not reuse the Claude-only classifier unchanged or infer use from prose mentioning Surgeon.
Classify each target-mutating episode: program string/regex/script; native patch/editor; Surgeon CLI mutation; Surgeon MCP mutation; mixed; unknown. Reads, tests, request-file creation, failed/no-op attempts and `admit_clojure_patch` verification are separate. Keep census-compatible `git apply` program counts alongside the more precise patch taxonomy.
Record first attempted route, successful mutation sequence, refusal exposure, next attempted route, verified recovery, bypass and noncompletion. Identify the mechanisms responsible for surviving intended deltas; multiple surviving producers are mixed, not “last tool wins.” Formatting is an annotation, not new intent authorship. Missing provenance is unknown; inspect raw artifacts instead of guessing.
For the table below, Surgeon final means all surviving intended edits were authored by Surgeon (CLI or MCP), and program-final is defined analogously. Report CLI/MCP separately plus mixed/no-write/unknown; task-level any-program and census call share have separate denominators. All assigned runs remain in rate denominators. Two blinded graders audit exposed runs and every ambiguous final producer; retain disagreements.

## My numerical bets, before seeing these callers run

Each entry is **Astra / Sol**, task counts out of 13. These are point forecasts, not equivalence margins or promises about the broader census.

| Arm | Surgeon final | Program final | Independently accepted |
|---|---:|---:|---:|
| N | 0 / 0 | 4 / 0 | 8 / 9 |
| L | 1 / 0 | 4 / 0 | 8 / 9 |
| P | 4 / 2 | 2 / 0 | 8 / 9 |
| M | 6 / 4 | 1 / 0 | 8 / 9 |
| X0 | 1 / 0 | 2 / 0 | 8 / 9 |
| X1 | 3 / 0 | 1 / 0 | 8 / 9 |

I predict X0/X1 exposure in 3/13 Astra tasks and 0/13 Sol tasks. No exposure means no evidence for the conditional repair mechanism. Of exposed Astra tasks I predict next-route Surgeon attempts in 1/3 X0 and 2/3 X1; remaining next routes are native patches. I predict two extra accepted Surgeon completions in X1−X0 for Astra, zero for Sol; Surgeon-final and accepted-Surgeon forecasts need not coincide.
My main bet wins only if M exceeds L by at least 3 Surgeon-final tasks for Astra and 2 for Sol, without losing more than one accepted completion per model. The specific plate bet P−L is at least +2 Astra and +1 Sol. The incremental MCP bet M−P is at least +1 for each. Report paired discordances and marginal intervals; these are pilot thresholds, not significance tests. Publish every miss, including a successful native-only outcome.
Cost bet: accepted-run median M/N complete-wall ratio 1.30 Astra, 1.25 Sol; X1/X0 1.15 Astra, 1.00 Sol. Also publish per-task paired wall and failed-run time; differing successful subsets cannot establish a speed advantage. I predict routing movement without a demonstrated acceptance gain.
Budget: assuming 3 minutes/run, 168 runs consume 8.4 run-hours, ideal four-way floor 2.1 hours; 900-second caps allow 42 run-hours. Add an estimated 2–4 engineering hours for isolation, classifier and shim witnesses, plus explicitly metered delivery probes and review. Record input/cached/output tokens, turns, queue time and quota interruptions. Subscription runs have no per-run API invoice here; quota and paid-seat opportunity cost are not zero. No unsupported dollar/token price is substituted.
Ownership: these forecasts are mine. Freeze this document, manifests and runner hashes before the first scored run; implementation/delivery failures and forecast failures get separate rows. This consult authorizes no rollout result claim and executes no experiment.
