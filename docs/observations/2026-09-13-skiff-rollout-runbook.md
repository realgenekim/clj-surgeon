# Skiff rollout runbook — clj-surgeon stable/2026-09-13.2 for real work + dogfood telemetry

**Addressed to:** mayor@skiff. **Owner of execution on the Skiff:** the mayor. **Owner of this runbook:** forge@anvil (Fable).
**Order:** Gene, verbatim, 2026-09-13: "instruct fable model to make it so, and ahve it put full instructions for mayor @ skiff in maven inbox" (relayed by Astra; brief at /var/tmp/forge/fable-to-astra.md, section "ASTRA → FABLE — GENE ORDER").
**Companion custody item:** inb-811f37 (the mayor's account issues an INDEPENDENT receipt for trunk 53b9f158). That is a separate deliverable; installing this runbook does not discharge it, and delivering this runbook does not complete it.

## 0. What this is, in four lines

- WHERE: the Skiff (Gene's Apple Silicon laptop), checkout `/Users/genekim/src.local/clj-surgeon/`, GitHub `realgenekim/clj-surgeon`. The shared clj-surgeon MCP JVM there runs under launchd on port 7888 from that checkout (docs/mcp-publishing-dev.md).
- STATE: the Skiff's last recorded install is **stable/2026-09-10 = 59d8bc0c** (receipt inb-e09349: `make install` PASSED on Darwin arm64 25.5.0, bash 5.3.9, bb 1.12.209, java 26.0.2, clojure 1.12.4.1602, swipl 10.0.2; `make test` then FAILED on four Darwin defects). All four were fixed and landed (fable/skiff-fixes → a15531ee stable/2026-09-11.3), then the probe verb and TEST-ISO-016 (759974c7 stable/2026-09-13.1), then destination-envelope admission (53b9f158 stable/2026-09-13.2). If the Skiff moved since 09-10, step 1 reads the real receipt.
- TARGET: **stable/2026-09-13.2 = commit 53b9f158979eb1c71cd8afa1d5397f7254c6d55a** (annotated tag object 84eadae0a8507886200b8d489c16c1e695adf5d8, on GitHub). Trunk MCP/main is at the same commit. Public `main` stays frozen; nothing here touches it.
- WHY NOW: Gene ordered real-work use on the Skiff with dogfood telemetry. The first destination task and its retrievable telemetry are the done condition; install alone proves availability, not adoption.

## 1. What is shipped, and what is NOT

**Shipped by `make install` from the tag (everything writes under `$HOME`, no sudo):**

| artifact | where on the Skiff | expected receipt (from the Anvil install of the SAME commit) |
|---|---|---|
| CLI launcher | `~/bin/clj-surgeon` + `~/bin/clj-surgeon.receipt.edn` | `:source-commit "53b9f158…"`, `:source-hash "33190a06a9c31ba35c3ceb02a2513ba42a7791e0ce60d6b02739d23193999f4e"` |
| versioned package | `~/.local/share/clj-surgeon/versions/53b9f158…/` | contains `cli-33190a06…` and `skill-21489506…` |
| Claude skill | `~/.claude/skills/clj-surgeon` + `.receipt.edn` | `:source-hash "21489506479b62ee3b7d53e26bab37d5c3290423ff1407041090ffcde89f96f7"` |
| Codex skill | `~/.codex/skills/clj-surgeon` + `.receipt.edn` | same hash |
| routing plate | managed block inside `~/.claude/CLAUDE.md` and `~/.codex/AGENTS.md` | `:block-hash "67e457240c69a1c41bc335b7c3b68e2fbad35cd37d7dd8721031f1f094136393"` |

A different source-hash for the same commit is a real finding (platform-dependent packaging), not a typo: paste it back.

**NOT shipped, deliberately (Anvil-only assumptions):** the ship/land/packet machinery (`~/bin/ship`, `run build`, Landlock write roots, `run-bg`, `/proc`-based identity), `tighten`/`measure`/the encounter register and its runner, `seat-receipt`, `usage-watch`. They assume Linux, Landlock, and this seat's paths (`/var/tmp/forge`, `/home/forge`). They stay on Anvil. The Skiff gets the product (CLI, MCP server, skills, plate) and its own telemetry. Do not copy `~/bin/lib` from Anvil.

## 2. Prerequisites (from docs/install/skiff.md at the tag — the preflight refuses without them)

`bb`, `java`, `clojure`, `git`, `python3` (refused if missing). To USE: `clj-kondo` (`brew install clj-kondo/brew/clj-kondo`). For `make test` only: `swipl` (`brew install swi-prolog`). Optional: `brew install coreutils`.
The gate refuses a RAM-backed temp base and needs ≥ 3584 MiB available (`GATE_MEMAVAIL_MIB=<MiB>` declares what the box may lend). Socket slots live under `/tmp/csg-$UID` (darwin's 104-byte AF_UNIX cap); never delete it while a gate runs.

## 3. Ordered commands — copy exactly, read each expected output before the next step

Every timestamp below comes from `date -u`; write nothing by hand.

### Step 1 — record the destination identity and the CURRENT state (before changing anything)

```sh
date -u +%FT%TZ; sw_vers; uname -m; sysctl -n hw.ncpu; sysctl -n hw.memsize; id -un; hostname
cd /Users/genekim/src.local/clj-surgeon
git rev-parse HEAD; git describe --tags --exact-match 2>/dev/null || echo "not on a tag"
cat ~/bin/clj-surgeon.receipt.edn 2>/dev/null || echo "no CLI receipt"
cat ~/.claude/skills/clj-surgeon.receipt.edn 2>/dev/null | grep -E 'source-(commit|hash)'
make check-agent-routing 2>&1 | tail -3
launchctl print "gui/$(id -u)/com.realgenekim.clj-surgeon-mcp" >/dev/null 2>&1 && echo "MCP launchd: present" || echo "MCP launchd: absent"
curl -fsS --max-time 2 http://127.0.0.1:7888/healthz && echo " (7888 healthy)" || echo "7888 not answering"
```

Expected: a `:source-commit` line (59d8bc0c… if nothing moved since 09-10). Keep this block verbatim; it is the rollback target and the "before" half of the receipt.

### Step 2 — fetch and verify the tag and commit (no checkout of anything unverified)

```sh
git fetch origin --tags
git rev-parse 'stable/2026-09-13.2^{commit}'      # MUST print 53b9f158979eb1c71cd8afa1d5397f7254c6d55a
git rev-parse 'stable/2026-09-13.2'               # tag object: 84eadae0a8507886200b8d489c16c1e695adf5d8
git cat-file -p 'stable/2026-09-13.2' | head -5    # tagger forge-anvil; message names ship round 3, Sol GO, battery receipt 47c5706a
git status --porcelain | wc -l                     # MUST be 0; if not, stop and paste `git status`
git checkout --detach 53b9f158979eb1c71cd8afa1d5397f7254c6d55a
git rev-parse HEAD                                 # 53b9f158…
```

If either sha differs, STOP and paste both lines back; do not install.

### Step 3 — announce the routing-plate change to the Skiff seats, then install

The plate rewrites the managed block in `~/.claude/CLAUDE.md` and `~/.codex/AGENTS.md` for every seat that shares this `$HOME` (surgeon1, surgeon2, cfp3, the mayor). Doctrine: never change another seat's prompt without telling that seat first. Post one line to each live seat: "clj-surgeon plate updates to 53b9f158 (block 67e45724) at <date -u>; running sessions keep the old block until they restart."

```sh
date -u +%FT%TZ
make install 2>&1 | tee /tmp/clj-surgeon-install-53b9f158.log | sed -n '/preflight/,/^OK to install\|^REFUSED/p'
tail -12 /tmp/clj-surgeon-install-53b9f158.log
```

Expected in order: the preflight block with `platform Darwin arm64`, `mode :path-socket`, a MEASURED `memory … MiB available`, `temp base … ("apfs" -- accepted)`; then `OK to install. Every earned win is available on this box.`; then `Installed stable CLI /Users/genekim/bin/clj-surgeon from commit 53b9f158…, source hash 33190a06…`, the two skill lines with hash `21489506…`, and the routing line `{:ok true, :operation :install-agent-routing, :block-hash "67e45724…", …}`. A `REFUSED` line names its own remedy; paste it verbatim and stop.

### Step 4 — prove the install by receipts, not by the log

```sh
grep -E 'source-(commit|hash)' ~/bin/clj-surgeon.receipt.edn ~/.claude/skills/clj-surgeon.receipt.edn ~/.codex/skills/clj-surgeon.receipt.edn
ls ~/.local/share/clj-surgeon/versions/53b9f158979eb1c71cd8afa1d5397f7254c6d55a/
make check-agent-routing 2>&1 | tail -2           # must report parity, block 67e45724…
~/bin/clj-surgeon :help >/dev/null && echo "CLI runs"
~/bin/clj-surgeon :probe :ns clj-surgeon.forms; echo "rc=$?"
```

The last line is a deliberate harmless refusal: expect a typed refusal (`probe-refused` / `probe-target-not-a-test-namespace` or `probe-connection-failed` when no warm image is registered), rc 1, and NO stack trace. That is the verb refusing correctly; a stack trace is a finding.

### Step 5 — landing gate on the Skiff (recommended; ~10 min; the darwin receipt Gene never had)

```sh
date -u +%FT%TZ; make test 2>&1 | tee /tmp/clj-surgeon-test-53b9f158.log | tail -1; date -u +%FT%TZ
```

Read the LAST line, one EDN map: `:state :passed`, `:landing? true`, `:capacity {:lanes N :admission :path-socket …}`, `:wall-ms`. Anything else: paste the map and the first `gate-refused:` line. This is optional for real-work use (the CLI and server do not need it) and mandatory for the receipt Gene asked for on 09-10.

### Step 6 — the shared MCP server on 7888 (launchd), telemetry FULL

```sh
make mcp-stop                                   # idempotent; removes the launchd job, pid/ready files
make mcp-start                                  # starts cclsp (7890) then clj-surgeon (7888) from THIS checkout, :telemetry :full
cat ~/.local/state/clj-surgeon/mcp/ready.edn     # :pid, :port 7888
curl -fsS --max-time 2 http://127.0.0.1:7888/healthz; echo
```

Tool catalog through a real MCP session (expect 13 tools; the list below is the one this commit serves):

```sh
H=$(curl -s -D - -o /dev/null -X POST http://127.0.0.1:7888/mcp -H 'Content-Type: application/json' -H 'Accept: application/json, text/event-stream' --data '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"skiff-check","version":"0"}}}' | grep -i '^mcp-session-id' | tr -d '\r' | cut -d' ' -f2)
curl -s -X POST http://127.0.0.1:7888/mcp -H 'Content-Type: application/json' -H 'Accept: application/json, text/event-stream' -H "Mcp-Session-Id: $H" --data '{"jsonrpc":"2.0","method":"notifications/initialized"}' >/dev/null
curl -s -X POST http://127.0.0.1:7888/mcp -H 'Content-Type: application/json' -H 'Accept: application/json, text/event-stream' -H "Mcp-Session-Id: $H" --data '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}' | grep -o '"name":"[a-z_]*"' | sort -u | tr '\n' ' '; echo
```

Expected 13: admit_clojure_patch alias_migration apply_clojure_changes edit_clojure feature_thread helper_extraction insert_forms inspect_clojure namespace_split relation_census rename_alias require_change transform_clojure.
Logs: `~/.local/state/clj-surgeon/mcp/server.log`. Telemetry dir: `~/.local/state/clj-surgeon/mcp/telemetry/` (run-id `dogfood`, 30-day retention).

### Step 7 — register the server for sessions (per working repository)

Claude Code: in the repository you will work in, `.mcp.json` (create or merge; keep other servers):

```json
{"mcpServers": {"clj-surgeon": {"type": "http", "url": "http://127.0.0.1:7888/mcp"}}}
```

Codex: `clj-surgeon up /absolute/repo --force` (development-only guard; edits that repo's `.codex/config.toml`, idempotent; start a NEW Codex session afterwards). Do not start a second server per repository. Pass the repo's absolute `workspace_root` on `inspect_clojure` / `apply_clojure_changes` when it is not the server's default project.

### Step 8 — telemetry: what exists, where, and how to pull it

Two ledgers, both appended by the calls themselves (TELEMETRY-EVENTS-001; no watcher to point):

1. `~/.clj-surgeon/events.jsonl` — box-wide, one JSON line per completed public MCP call. Fields that EXIST: `ts` (call end, UTC), `seat`, `pid`, `kind` ("mcp-call"), `tool`, `ok` (bool), `error_type` (typed refusal kind or null), `wall_ms` (SERVER-side total for the call), `mission_id`, `dropped` (append failures counted), and cost pass-throughs (`prompt_tokens`, `completion_tokens`, `reasoning_tokens`, `cost_usd`, `provider`, `upstream`) which are null unless the caller supplies them.
2. `~/.local/state/clj-surgeon/mcp/telemetry/` — the server's `:full` event stream (`server.start`, `tool.call` with `timings_ms`, outcomes), same run-id `dogfood`.

What is NOT there, so record it by hand and label it: client-side wait (the agent's request-to-response wall) — unknown to the server; whole-request accepted completion (did the user's task finish and get accepted) — a human or external acceptor fact; native fallbacks (the agent did it by hand instead) — invisible unless the session transcript says so; setup/recovery/manual interventions — only if you write a timestamped line. For the first task (step 9) write those lines yourself with `date -u`.

Collection (one bounded window, replace the two timestamps with your step-9 start/end):

```sh
START=2026-09-13T00:00:00Z; END=2026-09-14T00:00:00Z
awk -v s="$START" -v e="$END" -F'"ts":"' 'NF>1{split($2,a,"\""); if(a[1]>=s && a[1]<=e) print}' ~/.clj-surgeon/events.jsonl > /tmp/dogfood-events.jsonl
wc -l /tmp/dogfood-events.jsonl; python3 -c "import json,sys; rows=[json.loads(l) for l in open('/tmp/dogfood-events.jsonl')]; print({'calls':len(rows),'ok':sum(r.get('ok') is True for r in rows),'refused':sum(r.get('ok') is False for r in rows),'tools':sorted({r.get('tool') for r in rows}),'error_types':sorted({r.get('error_type') for r in rows if r.get('error_type')}),'dropped_total':sum(r.get('dropped') or 0 for r in rows),'wall_ms_max':max([r.get('wall_ms') or 0 for r in rows] or [0])})"
ls -la ~/.local/state/clj-surgeon/mcp/telemetry/ | tail -5
tail -20 ~/.local/state/clj-surgeon/mcp/server.log
```

### Step 9 — destination acceptance: ONE real bounded task in a FRESH session

In a fresh Claude Code (or Codex) session in a real repository of yours with the step-7 registration:

1. Confirm the effective version and prompt: `cat ~/bin/clj-surgeon.receipt.edn` (53b9f158), and in the session ask it to list its MCP tools (13 above) and to quote the first line of the managed routing block from its instructions (must mention alias migration and namespace split as the two routed classes).
2. Write `date -u` as TASK-START. Do one bounded real task the routed classes cover, e.g. an alias migration: "migrate alias X to Y across src" via `alias_migration` with `expect.files` set; or, if no such task exists today, one surgical `within`+`from`/`to` edit through `edit_clojure`/`require_change` on a real file, then run the repository's own tests. Write `date -u` as TASK-END.
3. Check the proof independently of the tool: `git diff --stat`, run the repository's tests yourself, read the receipt's `verification_complete` and named checks (a `true` field alone is not proof; a pending/false field is a refusal to accept).
4. Exercise a harmless refusal inside the session: ask it to `inspect_clojure` a path OUTSIDE `workspace_root` (expect a typed refusal, no write, no crash) — and note whether the session repaired once or fell back native.
5. Pull the telemetry for [TASK-START, TASK-END] with step 8; confirm the rows match the calls the session made (tool names, ok/refused, error_type of the refusal).
6. Record: destination identity (step 1 block), install receipt (step 4), session kind and model, TASK-START/END, the tool calls and their `wall_ms`, the refusal's `error_type`, the result (accepted / not), and every manual intervention with its timestamp.

Local parity and availability are NOT adoption. Everything you record here is **self-issued** (your seat built and accepted it). Where the mayor's separate account owns custody (inb-811f37), that receipt is the independent one; do not merge the two.

### Step 10 — rollback (exact, tested shape: reinstall the previous tag)

```sh
make mcp-stop
cd /Users/genekim/src.local/clj-surgeon
git checkout --detach 59d8bc0c            # or the :source-commit your step-1 receipt showed
make install 2>&1 | tail -6                 # receipts return to that commit; the routing block is regenerated from that tree
grep source-commit ~/bin/clj-surgeon.receipt.edn
make check-agent-routing 2>&1 | tail -1
make mcp-start
```

`~/.local/share/clj-surgeon/versions/` keeps both packages; nothing is deleted. Telemetry files are append-only and are not rolled back.

## 4. Known limitations, stated plainly

- Index and fleet claims: this install moves neither the sublime index nor fleet qualification; the first destination task with retrievable telemetry is the initial done condition, and it is self-issued.
- Darwin semaphore: the gate's split-semaphore guarantee is DETECTED on macOS, not unrepresentable (docs/install/skiff.md).
- The probe verb (`clj-surgeon :probe :ns`) needs a warm image registered under the seat state root; the shared 7888 server does not serve the probe route. Expect the typed refusal in step 4; do not treat it as a defect.
- Telemetry lacks client wait and accepted-completion; record those by hand (step 8).
- The encounter runner, ship v3.13 packet tooling, and independent-acceptance machinery are Anvil-only and not shipped.
- Landed but not yet in a stable tag for the Skiff: state-home identity fix (inb-3c65d7), in progress on Anvil; it changes where the warm image writes its descriptor and does not affect this install.

## 5. Durable artifacts and receipts

- This runbook: `clj-surgeon-records` (records/MCP-main) `docs/observations/2026-09-13-skiff-rollout-runbook.md`; the commit sha and the file's sha256 are in the inbox item that carries it.
- Anvil's install of the same commit (the expected receipts above): `/var/tmp/forge/install-53b9f158.log` on Anvil, receipts under `/home/forge/bin/` and `/home/forge/.claude/skills/`, `/home/forge/.codex/skills/`.
- The landing: ship run `/var/tmp/forge/ship/20260913T053427Z-ce08cb015850/` on Anvil; Sol GO verdict `2026-09-13-data-not-code-sol-fence-verdict-2-GO.md`; battery receipt commit 47c5706a; tag stable/2026-09-13.2.
- Prior Skiff receipt: inb-e09349 (2026-09-10).

## 6. The exact response requested from mayor@skiff (reply on the inbox item)

1. Step-1 block verbatim (identity + before-state).
2. Step-2 two shas; step-3 preflight block + OK line + the three receipt lines; step-4 receipt greps + routing parity line + the probe refusal line.
3. Step-5 last EDN map (or the refusal), with the two `date -u` stamps.
4. Step-6 ready.edn, healthz, and the 13-tool line.
5. Step-9 record (identity, receipt, session kind/model, TASK-START/END, calls with wall_ms, the refusal's error_type, result, manual interventions), and the step-8 summary dict for that window.
6. Any line that said REFUSED, verbatim; any deviation from an expected output, verbatim.
7. Whether inb-811f37 (independent receipt from the mayor's account) is done, in progress, or blocked, and on what.
