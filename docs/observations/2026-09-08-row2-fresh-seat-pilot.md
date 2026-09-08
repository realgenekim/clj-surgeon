# Row 2 cold-start pilot — does a FRESH agent take the routed alias path? (forge@anvil, 2026-09-08T20:58:36Z)

Row 2's named blocker in the perfect-the-wins tree was *"cold-start pilot of the alias route
on a fresh seat."* Stages 1-3 measured a **directed** route: the D brief named
`~/bin/surgeon-call alias_migration <request> <port>` and forbade every other mutation. This
pilot removes the instruction and keeps everything else frozen.

Preregistration (frozen before any cell, with its own Amendment 0):
`/var/tmp/forge/row2-pilot/design/prereg.md`, sha256 `5bd55e1f46dc096a…` at freeze.

## Headline

| cell | runner / model | route (independent witness) | acceptance class | caller work | primary wall | actions | apparatus |
|---|---|---|---|---|---|---|---|
| PA1 | Claude Agent tool / opus | **ROUTED** — receipt bound to the worktree | **accepted** (O1-O5 all PASS) | 95.8 s | 295.3 s | 18 | 4 (22.2%) |
| PA2 | Claude Agent tool / opus | native — no bound call | accepted-with-minor | 121.2 s | 252.5 s | 22 | 4 (18.2%) |
| PS1 | `sol-yolo` / gpt-5.6-sol | **ROUTED** — receipt bound | **accepted** † | 89.2 s | 362.0 s | 12 | 4 (33.3%) |
| PS2 | `sol-yolo` / gpt-5.6-sol | **ROUTED** — receipt bound | **accepted** | 89.3 s | 267.0 s | 11 | 4 (36.4%) |

**Registered gate: PASS.** Routed unprompted **3/4** (≥ 3/4 required). Accepted **4/4**
(≥ 3/4 required). Every routed cell produced the frozen answer: 9 files, 21 live sites,
`alias_histogram {"mjson": 9}`, 3 collisions resolved, protected `cmd-attrs` prose intact,
kondo delta 0, suite 579 tests / 7,833 assertions / 0 failures.

By runner: Sol **2/2 routed**, Opus **1/2 routed**.

† PS1's first grading returned `rejected` on one O5 MINOR whose entire content was the string
`.codex/` — a directory **`sol-yolo` itself installs** to declare the MCP server. Both numbers
are published: raw (`oracle-run.raw.txt`, O5 FAIL) and corrected (`oracle-run.txt`, O5 PASS).
See "Apparatus correction 1" below; the correction was declared and applied symmetrically
before PS2 ran.

## Comparison with the registered stage-3 numbers — the number that changes

| quantity | stage 3 (directed) | this pilot (cold start) |
|---|---|---|
| routed caller work | 27.0 s median (n=6) | **89.3 s median (n=3)** |
| native caller work | 119.9 s median (n=6) | 121.2 s (n=1) |
| routed / native | 0.251 → **3.98x less caller work** | 0.737 → **1.36x less** |

The native cell lands almost exactly on the registered native median (121.2 vs 119.9 s), which
is the strongest evidence the fixture and clock still measure what they measured this morning.
The **routed** cell is 3.3x slower than its directed twin, and the whole difference is
orientation: a directed D arm was handed a byte-frozen request and spent 9-16 s before
`mutation-start`; a fresh cell must read the task, establish the 9/21 shape, and compose the
request itself. **The registered 3.98x is a property of the directed harness, not of a fresh
caller.** The route still wins cold, by roughly 1.36x on caller work — with n=1 native, that
ratio is a signal, not an estimate.

## One learning

**The plate is delivered and read; the failure mode is non-consideration, not rejection.**
Both Claude cells received the routing plate — `grep -c "Alias migration -- one
whole-repository call"` returns 1 in each transcript's system prompt, and both carry
"Strictly better, or native". PA1 loaded the schema (`ToolSearch
"select:mcp__clj-surgeon__alias_migration"`) and made one call. **PA2 never issued a single
non-Bash action**: 22 Bash calls, zero ToolSearch, zero MCP, and not one sentence in its own
narration weighing a route. Asked how it worked, it described `grep` + a scripted `sed` pass.
There is no quotable reason for going native because **no decision was made** — the deferred
MCP tools were listed but their schemas were never fetched, and the agent moved straight from
"I'll start with the clock stamp" to "Now I have the full plan."

That matters because it names the lever. A cell that weighed the route and declined it would
be an argument about evidence. A cell that never looked is an argument about **surfacing** —
and it is consistent with the seat's standing finding that free-choice *exposure* routing is
dead (adoption 0/19 in the September 3 cohort). What is new here is the other half: with the
plate present **and the verb's schema one deferred-tool fetch away**, 3 of 4 fresh cells did
route. The plate is doing work; the last gap is that a deferred tool is invisible until
someone spends an action to look at it.

**And the native cell paid the exact tax the verb exists to remove.** PA2 volunteered it,
verbatim: *"deleting the require on line 52 shifted the docstring up one line, so my
line-excluded `sed` guard landed one line off and rewrote that prose. I caught it in the
immediate verification grep and restored the exact original text… Line-number guards computed
before a same-file deletion are the hazard."* The protected `cmd-attrs` docstring is the
fixture's named canary and O4's kill condition. PA2 caught it and O4 passed — but the native
route reached into the protected prose and the routed route cannot: `string_mentions=0` is a
field on the receipt.

**Secondary — does the receipt suffice?** Split by runner, and the split is clean. Both Sol
cells treated the receipt as terminal for the rewrite and checked only metadata; PS1 said so
in its own words: *"I'm checking only change metadata and whitespace integrity now — without
reopening or reapplying the already verified rewrite."* PA1 did **not**: it enumerated the 21
sites natively **before** the call to confirm the shape, and read the complete `git diff`
after. That extra pass is most of PA1's 6.6 s caller-work penalty over the Sol cells and is
exactly the "fresh caller wants a gate, not a second editor" behaviour already on this seat's
record.

## One caveat

**Two cells per runner cannot separate model from runner from availability.** The Sol cells
were launched through `sol-yolo`, which writes `<worktree>/.codex/config.toml` with
`[mcp_servers.clj-surgeon] required = true`; the Claude cells inherited a server that was
merely *present* in the client config, with its schema deferred behind ToolSearch. Both are
availability, not instruction — neither brief mentions a route, clj-surgeon, MCP,
`surgeon-call`, or a request file (verified by a spelling-enumerated grep in every arm
attestation) — **but they are not the same availability**, and the 2/2 vs 1/2 split lines up
with the asymmetry exactly as it would if the asymmetry, and not the model, were the cause.
This pilot cannot tell those apart. The cheap next cell is a Claude arm with the
`alias_migration` schema pre-loaded rather than deferred; if that closes the gap, the finding
is a surfacing defect and not a model difference.

## Apparatus correction 1 — the runner's own `.codex/` was scored against the agent

Full note: `/var/tmp/forge/row2-pilot/apparatus/APPARATUS-CORRECTION-1.md`.

Two oracles saw a directory the *runner* installed:

1. `check-untracked.sh` listed `.codex/config.toml` as a tenth path → FAIL. Root cause: the
   exclude had been appended to the per-worktree `$GIT_DIR/info/exclude`, **which git does not
   read** — git reads `$GIT_COMMON_DIR/info/exclude`. Fixed with `--git-common-dir`; PS1
   re-checked → PASS. A wrong path, not a weakened rule.
2. `o5_papercut.sh` then counted `.codex/` as a stray ignored path → 1 MINOR → O5 FAIL →
   `rejected`.

Treatment, declared before PS2 ran and applied to both Sol cells: the runner-installed
`.codex/` is removed before grading, so a Sol cell is graded on the same workspace shape as a
Claude cell. **The oracle was not modified and its allowlist was not widened.** Raw and
corrected gradings are both retained. This is the coldstart harness's CX-1 rule — a defect of
delivery is never a behaviour FAIL — applied to grading rather than to launch, and it is worth
writing down because the failure looked exactly like an agent leaving scratch files behind.

Contamination check, since the pilot directory holds this design: PA1's arm directory briefly
contained an `attestation.txt` naming `mcp_port=7906`. Its transcript shows **zero** reads of
that file and zero reads under `/var/tmp/forge/row2`, so no cell learned the route from the
apparatus. Later cells write their attestation into a `chmod 000` directory instead.

## Fixture and integrity

Unchanged from stages 1-3: seed repo `/var/tmp/forge/row2/seed-repo` at
`6dcdbc9db67a91179b5940a16c16b760583f2603` (history-isolated, no remote), seeded `json.clj`
`5e2b4151…`, task `ec4c2a9e24fad8fbc4f0733454f773df2d1b20c61129f2de0ff402d9b9c331a2`,
golden bytes `chmod 000` for the whole of every cell. Every arm attested head, cleanliness,
absence of a remote, both repo boot-file hashes, brief neutrality, and 7906 reachable; the
repo's own `CLAUDE.md`/`AGENTS.md` carry **no** route instruction (the single incidental
string "clj-surgeon" is a docs path in the Memento pointer, recorded verbatim in each
attestation). Cells ran **sequentially**, one arm JVM at a time, `uptime` before each, and
grading was held once while another seat's work pushed the box to load 17.

MCP server: this seat's clj-surgeon on **7906** (stage 3's 7907 is gone). Route witness is
the server's own receipt bundles under `/var/tmp/forge/alias-migration-receipts/`, which carry
absolute paths and therefore bind a call to a worktree; transcript and witness agreed on all
four cells.

## Paths

* preregistration + brief template: `/var/tmp/forge/row2-pilot/design/` (sealed during cells)
* apparatus, per-arm attestations, correction note: `/var/tmp/forge/row2-pilot/apparatus/`
* per-cell evidence (brief, stopwatch, agent report/stdout, route witness, oracle outputs,
  candidate patch, wall): `/var/tmp/forge/row2-pilot/{PA1,PA2,PS1,PS2}/`
* worktrees, preserved: `/home/forge/src/mvr-row2-{PA1,PA2,PS1,PS2}`
* stage 1-3 preregistration and results: `docs/observations/2026-09-08-row2-alias-preregistration.md`,
  `docs/observations/2026-09-08-row2/`
* the weekly alias verb sentinel: `docs/observations/2026-09-08-tighten-one-shots.md`

Nothing was committed or pushed in any specimen or in clj-surgeon; this file is the only
commit this pilot makes.
