#!/usr/bin/env bash
# test_run_opus_arm.sh — the harness's own witnesses.  NO model, NO JVM, NO server.
#
# A fake `claude` first on PATH writes a synthetic session transcript and touches a
# file in its cwd; a stub oracle stands in for rescore-FAN.sh.  Everything the real
# arm does around the caller -- refusing a reused identity, refusing a fixture whose
# sha is not the pinned one, refusing an out-of-band MCP URL, binding the session
# file, reading the model id and tool calls out of it, staging the diff, and invoking
# the acceptance oracle -- is exercised here at shell speed.
#
# Scratch lives ONLY under /var/tmp/forge/opuscaller-fx and /var/tmp/forge/opus-arms.
set -uo pipefail
HERE=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
FX=${OPUSCALLER_FX:-/var/tmp/forge/opuscaller-fx}
ARMS="$FX/arms"
PASS=0; FAIL=0
ok()   { echo "  PASS $*"; PASS=$((PASS+1)); }
bad()  { echo "  FAIL $*"; FAIL=$((FAIL+1)); }
check(){ if [ "$2" = "$3" ]; then ok "$1 ($2)"; else bad "$1: want '$3' got '$2'"; fi; }

rm -rf "$FX"; mkdir -p "$FX/bin" "$ARMS" "$FX/projects"

# ---- a minimal fixture repo with the shape the harness demands -------------------
BASE="$FX/base-repo"
mkdir -p "$BASE/src/acid/fanout" "$BASE/test" "$BASE/bin"
printf '(ns acid.fanout.ns-001 (:require [acid.fanout.store :as s]))\n(defn f [] (s/find-event 1))\n' > "$BASE/src/acid/fanout/ns_001.clj"
printf '(println "load")\n' > "$BASE/test/load_all.clj"
printf '#!/usr/bin/env bash\necho FAN-TEST tests=21 assertions=147 failures=0 errors=0\n' > "$BASE/bin/fan-test"
chmod +x "$BASE/bin/fan-test"
git -C "$BASE" init --quiet -b main
git -C "$BASE" add src test bin
GIT_AUTHOR_NAME=t GIT_AUTHOR_EMAIL=t@t GIT_COMMITTER_NAME=t GIT_COMMITTER_EMAIL=t@t \
  git -C "$BASE" commit --quiet -m fixture
BASE_SHA=$(git -C "$BASE" rev-parse HEAD)

# ---- frozen-prompt stand-ins (same three filenames the real screen uses) ----------
mkdir -p "$FX/prompts" "$FX/oracle/canonical-21"
for name in fanout-common fanout-native fanout-tool; do
  printf 'FROZEN PROMPT %s\n' "$name" > "$FX/prompts/$name.txt"
done
printf '{}\n' > "$FX/oracle/manifest-21.edn"

# ---- the stub oracle: records that it ran, against which tree, read-only ----------
cat > "$FX/bin/stub-oracle.sh" <<'OEOF'
#!/usr/bin/env bash
echo "stub-oracle: worktree=$1 n=$2 fixtures=$3 base=${FAN_BASE:-none}"
echo "$1" > "$OPUSCALLER_FX/oracle-called-with.txt"
echo "rescore-FAN: 6/6 checks passed"
exit 0
OEOF
chmod +x "$FX/bin/stub-oracle.sh"
ORACLE_SHA=$(sha256sum "$FX/bin/stub-oracle.sh" | cut -d' ' -f1)

# ---- the fake caller: a session transcript, a stream, and one real edit -----------
cat > "$FX/bin/claude" <<'CEOF'
#!/usr/bin/env bash
# fake `claude` — writes what the real CLI writes, and nothing else.
set -uo pipefail
[ "${1:-}" = "--version" ] && { echo "9.9.9 (Fake Claude Code)"; exit 0; }
SID=""; prev=""
for a in "$@"; do [ "$prev" = "--session-id" ] && { SID=$a; break; }; prev=$a; done
cat > /dev/null                       # consume the prompt on stdin
[ -n "$SID" ] || { echo "fake-claude: no --session-id" >&2; exit 64; }
ESC=$(printf '%s' "$PWD" | sed 's#[^A-Za-z0-9]#-#g')
DIR="${CLAUDE_PROJECTS_ROOT:?}/$ESC"; mkdir -p "$DIR"
MODEL=${FAKE_MODEL:-claude-opus-5}
MODEL2=$MODEL
[ -n "${FAKE_TWO_MODELS:-}" ] && MODEL2=claude-sonnet-5
{
  printf '{"type":"mode","mode":"normal","sessionId":"%s"}\n' "$SID"
  printf '{"type":"assistant","sessionId":"%s","timestamp":"2026-09-05T00:00:00Z","message":{"model":"%s","content":[{"type":"tool_use","id":"t1","name":"Edit","input":{"file_path":"src/acid/fanout/ns_001.clj"}}]}}\n' "$SID" "$MODEL"
  printf '{"type":"assistant","sessionId":"%s","timestamp":"2026-09-05T00:00:01Z","message":{"model":"%s","content":[{"type":"tool_use","id":"t2","name":"Bash","input":{"command":"bin/fan-test"}}]}}\n' "$SID" "$MODEL2"
} > "$DIR/$SID.jsonl"
printf '{"type":"system","subtype":"init","session_id":"%s","model":"%s","mcp_servers":[],"tools":["Edit","Bash"]}\n' "$SID" "$MODEL"
printf '{"type":"assistant","session_id":"%s","message":{"model":"%s","content":[{"type":"tool_use","id":"t1","name":"Edit","input":{"file_path":"src/acid/fanout/ns_001.clj"}}]}}\n' "$SID" "$MODEL"
printf '{"type":"assistant","session_id":"%s","message":{"model":"%s","content":[{"type":"tool_use","id":"t2","name":"Bash","input":{"command":"bin/fan-test"}}]}}\n' "$SID" "$MODEL"
# the write the arm is measured on
printf '(ns acid.fanout.ns-001 (:require [acid.fanout.store2 :as store2]))\n(defn f [] (store2/fetch-event 1))\n' > src/acid/fanout/ns_001.clj
printf 'scratch\n' > NEW-FILE.txt
exit 0
CEOF
chmod +x "$FX/bin/claude"

# an owned quiet window (the harness refuses without one) and a stub policy validator
# standing in for astra_policy.py, which needs a LIVE server to attest
printf 'owner=fable arm=test opened=now\n' > "$FX/quiet-window.md"
cat > "$FX/bin/stub-policy.py" <<'PEOF'
import json, os, sys
ready = json.load(open(sys.argv[sys.argv.index("--ready") + 1]))
if os.environ.get("STUB_POLICY_REFUSE"):
    print("ASTRA-POLICY REFUSED: stubbed refusal", file=sys.stderr); sys.exit(2)
print(json.dumps({"ready": ready, "policy_source": "STUB", "action": sys.argv[1],
                  "saw_ready_edn": "--ready-edn" in sys.argv,
                  "saw_spawned": "--spawned" in sys.argv}))
PEOF
printf '{"mcp_url":"x","port_pid":1,"server_cwd":"/x"}\n' > "$FX/ready.json"

cp "$FX/bin/claude" "$FX/bin/claude.orig"      # blocks 12/13 swap it and restore it

export PATH="$FX/bin:$PATH"
export OPUSCALLER_FX="$FX"
export CLAUDE_PROJECTS_ROOT="$FX/projects"
export RUNTIME_ALLOWED=1
export OPUS_FIXTURE_SRC="$BASE" OPUS_FIXTURE_SHA="$BASE_SHA"
export OPUS_PROMPT_DIR="$FX/prompts"
export OPUS_ORACLE="$FX/bin/stub-oracle.sh" OPUS_ORACLE_SHA256="$ORACLE_SHA"
export OPUS_ORACLE_FIX="$FX/oracle" OPUS_ARMS_ROOT="$ARMS"
export OPUS_MAX_WALL=60
# the stand-in prompts and oracle fixtures have their own hashes; the BINDING is what
# is under test, so the expected values are the stand-ins' real hashes
export OPUS_EXPECT_NATIVE_SHA=$(sha256sum "$FX/prompts/fanout-native.txt" | cut -d' ' -f1)
export OPUS_EXPECT_TOOL_SHA=$(sha256sum "$FX/prompts/fanout-tool.txt" | cut -d' ' -f1)
export OPUS_EXPECT_COMMON_SHA=$(sha256sum "$FX/prompts/fanout-common.txt" | cut -d' ' -f1)
export OPUS_EXPECT_MANIFEST_SHA=$(sha256sum "$FX/oracle/manifest-21.edn" | cut -d' ' -f1)
export OPUS_LOAD_SAMPLE_S=1
export OPUS_QUIET_WINDOW="$FX/quiet-window.md"
export OPUS_POLICY_BIN="$FX/bin/stub-policy.py"
export OPUS_READY="$FX/ready.json" OPUS_SERVER_SHA=0123456789012345678901234567890123456789
export OPUS_READY_EDN="$FX/ready.json" OPUS_SPAWNED="$FX/ready.json"

# N arms carry no server evidence; T/O arms do (his contract, enforced both ways)
run()  { env -u OPUS_READY -u OPUS_SERVER_SHA -u OPUS_MCP_URL -u OPUS_READY_EDN -u OPUS_SPAWNED \
             bash "$HERE/run-opus-arm.sh" "$@" > "$FX/last.out" 2>&1; echo $?; }
runt() { bash "$HERE/run-opus-arm.sh" "$@" > "$FX/last.out" 2>&1; echo $?; }

echo "== 1. a native arm end to end =="
rc=$(run N 1); A="$ARMS/opus-N-1"
check "exit 0" "$rc" 0
check "clone at the pinned base" "$(git -C "$A/wt" rev-parse "$(cat "$A/base.sha")" 2>/dev/null)" "$BASE_SHA"
[ -s "$A/diff.patch" ] && ok "diff.patch staged ($(wc -l < "$A/diff.patch") lines)" || bad "diff.patch empty"
grep -q 'ns_001' "$A/diff.patch" && ok "diff carries the caller's edit" || bad "diff missing the edit"
grep -q 'NEW-FILE.txt' "$A/status.porcelain" && ok "status.porcelain sees the new file" || bad "new file not recorded"
grep -q 'NEW-FILE.txt' "$A/diff.patch" && ok "the STAGED diff contains the new file (his diff-semantics finding)" || bad "new file absent from diff.patch"
[ "$(git -C "$A/wt" diff --cached --name-only | wc -l)" -gt 0 ] && ok "changes are actually staged in the clone" || bad "nothing staged"
check "session bound" "$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["session_bound"])' "$A/arm.json")" True
check "model read from the transcript" "$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["models_in_transcript"][0])' "$A/attribution.json")" claude-opus-5
check "tool calls extracted" "$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["count"])' "$A/calls.json")" 2
check "tool call names" "$(python3 -c 'import json,sys;print(",".join(sorted(json.load(open(sys.argv[1]))["by_name"])))' "$A/calls.json")" Bash,Edit
check "resolved model is an ID, not a prose pointer" "$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["resolved_model"])' "$A/adapter-result.json")" claude-opus-5
check "the receipt records the requested ALIAS separately" "$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["requested_model_alias"])' "$A/adapter-result.json")" claude-opus-5
check "a native arm made zero MCP tool calls" "$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["mcp_tool_calls"])' "$A/attribution.json")" 0
check "and native MCP absence was CHECKED, not assumed" "$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["mcp_expected_absent"])' "$A/attribution.json")" True
check "session file sha recorded" "$(python3 -c 'import json,sys;print(bool(json.load(open(sys.argv[1]))["session_sha256"]))' "$A/arm.json")" True
check "oracle ran against the clone" "$(cat "$FX/oracle-called-with.txt" 2>/dev/null)" "$A/wt"
check "oracle verdict recorded verbatim" "$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["oracle_verdict"])' "$A/arm.json")" "rescore-FAN: 6/6 checks passed"
check "correctness from the oracle" "$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["correctness"])' "$A/arm.json")" accepted
check "protected bytes unchanged" "$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["protected_bytes_match"])' "$A/arm.json")" True
check "the guard used HIS snapshot (bytes+mode, symlink-refusing)" "$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["helper"])' "$A/guard-before.json")" "astra adapter.snapshot (bytes+mode, symlink-refusing)"
check "immutable inputs are bound by sha in the receipt" "$(python3 -c 'import json,sys;d=json.load(open(sys.argv[1]))["immutable_inputs"];print(all(d[k] for k in ("frozen_prompt_sha256","composed_prompt_sha256","oracle_sha256","oracle_manifest_sha256","oracle_canonical_tree_sha256")))' "$A/arm.json")" True
grep -q 'task_wall_s=' "$A/task-wall.txt" && ok "monotonic wall recorded" || bad "no task_wall_s"
grep -q 'load_start=' "$A/task-wall.txt" && ok "boundary load recorded" || bad "no load"
[ -s "$A/load.jsonl" ] && ok "in-arm load samples recorded ($(wc -l < "$A/load.jsonl") samples)" || bad "no load.jsonl"
check "CLI version recorded" "$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["cli_version"])' "$A/arm.json")" "9.9.9 (Fake Claude Code)"

echo "== 2. an existing arm directory is refused, never overwritten =="
before=$(sha256sum "$A/arm.json" | cut -d' ' -f1)
rc=$(run N 1)
check "exit 2" "$rc" 2
grep -q 'already exists' "$FX/last.out" && ok "refusal names the reused identity" || bad "wrong refusal: $(head -1 "$FX/last.out")"
check "the existing arm is untouched" "$(sha256sum "$A/arm.json" | cut -d' ' -f1)" "$before"

echo "== 3. a fixture whose sha is not the pinned one is refused =="
rc=$(OPUS_FIXTURE_SHA=0000000000000000000000000000000000000000 run N 2)
check "exit 2" "$rc" 2
grep -q 'fixture sha mismatch' "$FX/last.out" && ok "refusal names the sha mismatch" || bad "wrong refusal: $(head -1 "$FX/last.out")"
[ -e "$ARMS/opus-N-2" ] && bad "an arm directory was created despite the refusal" || ok "nothing was created"

echo "== 4. an oracle whose sha is not the pinned one is refused =="
rc=$(OPUS_ORACLE_SHA256=$(printf 0%.0s $(seq 64)) run N 3)
check "exit 2" "$rc" 2
grep -q 'oracle sha mismatch' "$FX/last.out" && ok "refusal names the oracle" || bad "wrong refusal: $(head -1 "$FX/last.out")"

echo "== 5. a tool arm's MCP URL must be loopback in this cohort's own band =="
rc=$(OPUS_MCP_URL=http://127.0.0.1:8300/mcp runt T 1)
check "exit 2 for a forbidden-band port" "$rc" 2
grep -q '8340-8379' "$FX/last.out" && ok "refusal names the band" || bad "wrong refusal: $(grep REFUSED "$FX/last.out" | head -1)"
rc=$(OPUS_MCP_URL=http://127.0.0.1:8341/mcp runt T 2); B="$ARMS/opus-T-2"
check "a well-formed tool arm runs" "$rc" 0
check "the MCP binding is written per arm" "$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["mcpServers"]["clj-surgeon"]["url"])' "$B/mcp.json")" "http://127.0.0.1:8341/mcp"
grep -q 'mcp__clj-surgeon__alias_migration' "$B/prompt.txt" && ok "T prompt names the tool by its Claude name" || bad "T prompt lacks the tool name"
grep -q 'FROZEN PROMPT fanout-tool' "$B/prompt.txt" && ok "T prompt is built on the frozen tool text" || bad "T prompt is not the frozen text"
check "a native arm may not carry an MCP URL" "$(OPUS_MCP_URL=http://127.0.0.1:8341/mcp runt N 5)" 2
check "a tool arm without server evidence is refused" "$(OPUS_MCP_URL=http://127.0.0.1:8341/mcp env -u OPUS_READY bash "$HERE/run-opus-arm.sh" T 7 >"$FX/last.out" 2>&1; echo $?)" 2
grep -q 'requires OPUS_READY' "$FX/last.out" && ok "refusal names the missing ready evidence" || bad "wrong refusal: $(head -1 "$FX/last.out")"
check "rejected server evidence stops the arm" "$(STUB_POLICY_REFUSE=1 OPUS_MCP_URL=http://127.0.0.1:8341/mcp runt T 8)" 2
grep -q 'server attestation rejected' "$FX/last.out" && ok "refusal names the attestation" || bad "wrong refusal: $(head -1 "$FX/last.out")"

echo "== 6. the optional-adoption cell attaches the tool without a mandate =="
rc=$(OPUS_MCP_URL=http://127.0.0.1:8341/mcp runt O 1); C="$ARMS/opus-O-1"
check "exit 0" "$rc" 0
grep -q 'FROZEN PROMPT fanout-common' "$C/prompt.txt" && ok "O prompt is built on the frozen COMMON text" || bad "O prompt is not the common text"
grep -q 'not asked or expected to use it' "$C/prompt.txt" && ok "O prompt carries no mandate" || bad "O prompt lacks the neutral stanza"
grep -q 'Try that operation' "$C/prompt.txt" && bad "O prompt carries the T mandate" || ok "O prompt carries no T mandate"

echo "== 7. no run happens without an allocation =="
rc=$(RUNTIME_ALLOWED=0 run N 9)
check "exit 2" "$rc" 2
grep -q 'RUNTIME_ALLOWED' "$FX/last.out" && ok "refusal names the gate" || bad "wrong refusal: $(head -1 "$FX/last.out")"

echo "== 8. the same-cores doctrine and the owned quiet window =="
grep -q '^/usr/bin/taskset$' "$A/command.txt" && ok "the caller is launched under taskset" || bad "no taskset in command.txt"
grep -q '^12,13$' "$A/command.txt" && ok "pinned to cores 12,13" || bad "wrong cores: $(sed -n 2p "$A/command.txt")"
check "affinity recorded in the receipt" "$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["cpu_affinity"])' "$A/arm.json")" "taskset -c 12,13"
mv "$FX/quiet-window.md" "$FX/qw.bak"
check "no window at all is refused" "$(run N 6)" 2
grep -q 'no quiet window' "$FX/last.out" && ok "refusal names the missing window" || bad "wrong refusal: $(head -1 "$FX/last.out")"
printf 'owner=someone-else\n' > "$FX/quiet-window.md"
check "a peer's window is refused" "$(run N 7)" 2
grep -q 'held by another agent' "$FX/last.out" && ok "refusal names the peer" || bad "wrong refusal: $(head -1 "$FX/last.out")"
mv "$FX/qw.bak" "$FX/quiet-window.md"

echo "== 9. Astra's field names are carried, not paraphrased =="
for f in attest.json adapter-result.json; do
  [ -s "$A/$f" ] && ok "$f written" || bad "$f missing"
done
check "attest carries his correctness placeholder" "$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["correctness"])' "$A/attest.json")" pending-independent-acceptance
check "attest names the instruments it does NOT run" "$(python3 -c 'import json,sys;d=json.load(open(sys.argv[1]));print(all(k in d and d[k] is None for k in ("watch_sha256","score_sha256","make_targets_sha256")))' "$A/attest.json")" True
check "his timing block is present in full" "$(python3 -c 'import json,sys;t=json.load(open(sys.argv[1]))["timing"];print(all(k in t for k in ("adapter_start_monotonic_s","watch_start_monotonic_s","watch_end_monotonic_s","preparation_wall_s","watch_subprocess_wall_s","adapter_load_start","watch_load_start","watch_load_end","lock_wait_included","adapter_wall_s","adapter_load_end","adapter_wall_scope")))' "$A/adapter-result.json")" True
check "his wall scope is spelled out" "$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["timing"]["adapter_wall_scope"])' "$A/adapter-result.json")" "prepare-through-freeze-and-attestation; excludes the acceptance oracle"
check "the resolved-model SOURCE is stated" "$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["resolved_model_source"])' "$A/adapter-result.json")" "session transcript; the command alias is never the claim"

echo "== 10. the plan is the 21-arm roster the lead ruled =="
bash "$HERE/calibrate.sh" plan "$FX/plan.txt" >/dev/null
check "21 arms" "$(grep -c '^ ' "$FX/plan.txt")" 21
check "6 tool arms" "$(awk '$2=="T"' "$FX/plan.txt" | wc -l)" 6
check "12 native arms" "$(awk '$2=="N"' "$FX/plan.txt" | wc -l)" 12
check "3 adoption arms" "$(awk '$2=="O"' "$FX/plan.txt" | wc -l)" 3
check "the last three arms are the adoption cell" "$(grep '^ ' "$FX/plan.txt" | tail -3 | awk '{print $2}' | tr -d '\n')" OOO
check "the first six are calibration natives" "$(grep '^ ' "$FX/plan.txt" | head -6 | awk '{print $2}' | tr -d '\n')" NNNNNN
check "block B alternates which cell leads" "$(grep '^ ' "$FX/plan.txt" | sed -n '7,18p' | awk '{print $2}' | tr -d '\n')" NTTNNTTNNTTN
grep -q 'N-1, is the instrument preflight' "$FX/plan.txt" && ok "the plan names N-1 as the preflight" || bad "no preflight clause"
grep -q 'alias is NEVER the model claim' "$FX/plan.txt" && ok "the plan says the alias is not the model claim" || bad "no alias clause"
check "calibrate run still refuses without an allocation" "$(bash "$HERE/calibrate.sh" run >"$FX/last.out" 2>&1; echo $?)" 2

echo "== 11. BLOCKER 1 — ownership/preparation order (his probe: parent-precreated-tool-arm) =="
# the parent must NOT pre-create the arm; the ARM prepares, THEN the server starts
check "prepare then launch is the supported order" "$(OPUS_MCP_URL=http://127.0.0.1:8341/mcp bash "$HERE/run-opus-arm.sh" T 20 prepare >"$FX/last.out" 2>&1; echo $?)" 0
[ -d "$ARMS/opus-T-20/wt" ] && ok "the clone exists BEFORE any server would start" || bad "no clone after prepare"
check "launch then completes the same arm" "$(OPUS_MCP_URL=http://127.0.0.1:8341/mcp bash "$HERE/run-opus-arm.sh" T 20 launch >"$FX/last.out" 2>&1; echo $?)" 0
check "the arm reached its intended identity" "$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["id"])' "$ARMS/opus-T-20/arm.json")" opus-T-20
check "the server attestation saw the ready.edn and spawn record" "$(python3 -c 'import json,sys;d=json.load(open(sys.argv[1]));print(d["saw_ready_edn"] and d["saw_spawned"])' "$ARMS/opus-T-20/server-attest.json")" True
check "launch without prepare is refused" "$(OPUS_MCP_URL=http://127.0.0.1:8341/mcp bash "$HERE/run-opus-arm.sh" T 21 launch >"$FX/last.out" 2>&1; echo $?)" 2
grep -q 'has not been prepared' "$FX/last.out" && ok "refusal names the missing preparation" || bad "wrong refusal: $(head -1 "$FX/last.out")"
check "a second launch of the same arm is refused" "$(OPUS_MCP_URL=http://127.0.0.1:8341/mcp bash "$HERE/run-opus-arm.sh" T 20 launch >"$FX/last.out" 2>&1; echo $?)" 2
grep -q 'already been launched' "$FX/last.out" && ok "refusal names the reused identity" || bad "wrong refusal: $(head -1 "$FX/last.out")"

echo "== 12. BLOCKER 3 — native MCP absence is EXPLICIT, and checked =="
check "native passes --mcp-config" "$(grep -c '^--mcp-config$' "$A/command.txt")" 1
check "native passes --strict-mcp-config" "$(grep -c '^--strict-mcp-config$' "$A/command.txt")" 1
check "and the native map is explicitly EMPTY" "$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["mcpServers"])' "$A/mcp.json")" "{}"
check "the receipt states the mode" "$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["mcp_config_mode"])' "$A/arm.json")" explicit-empty
# a native arm that DID reach an MCP tool is :unverified, not a footnote
cat > "$FX/bin/claude" <<'MEOF'
#!/usr/bin/env bash
set -uo pipefail
[ "${1:-}" = "--version" ] && { echo "9.9.9 (Fake Claude Code)"; exit 0; }
SID=""; prev=""
for a in "$@"; do [ "$prev" = "--session-id" ] && { SID=$a; break; }; prev=$a; done
cat > /dev/null
ESC=$(printf '%s' "$PWD" | sed 's#[^A-Za-z0-9]#-#g')
DIR="${CLAUDE_PROJECTS_ROOT:?}/$ESC"; mkdir -p "$DIR"
M=${FAKE_MODEL:-claude-opus-5}
printf '{"type":"assistant","sessionId":"%s","message":{"model":"%s","content":[{"type":"tool_use","id":"t1","name":"mcp__clj-surgeon__alias_migration","input":{}}]}}\n' "$SID" "$M" > "$DIR/$SID.jsonl"
printf '{"type":"system","subtype":"init","session_id":"%s","model":"%s"}\n' "$SID" "$M"
printf '{"type":"assistant","session_id":"%s","message":{"model":"%s","content":[{"type":"tool_use","id":"t1","name":"mcp__clj-surgeon__alias_migration","input":{}}]}}\n' "$SID" "$M"
exit 0
MEOF
chmod +x "$FX/bin/claude"
check "a native arm that reaches MCP is :unverified (exit 3)" "$(run N 30)" 3
grep -q 'made 1 MCP tool call' "$ARMS/opus-N-30/attribution.log" && ok "the refusal names the MCP call" || bad "wrong attribution log: $(cat "$ARMS/opus-N-30/attribution.log")"
cp "$FX/bin/claude.orig" "$FX/bin/claude"; chmod +x "$FX/bin/claude"

echo "== 13. BLOCKER 4 — terminal outcomes propagate (his probes: wrong-model, oracle-failure) =="
check "a wrong-model transcript exits 3, not 0" "$(FAKE_MODEL=definitely-wrong-model run N 91)" 3
check "  ...and valid_measurement is false" "$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["valid_measurement"])' "$ARMS/opus-N-91/adapter-result.json")" False
cat > "$FX/bin/fail-oracle.sh" <<'FEOF'
#!/usr/bin/env bash
echo "rescore-FAN: FAILED"
exit 7
FEOF
chmod +x "$FX/bin/fail-oracle.sh"
check "an oracle that does not accept exits 4, not 0" "$(OPUS_ORACLE="$FX/bin/fail-oracle.sh" OPUS_ORACLE_SHA256=$(sha256sum "$FX/bin/fail-oracle.sh"|cut -d' ' -f1) run N 92)" 4
check "  ...and correctness says so" "$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["correctness"])' "$ARMS/opus-N-92/adapter-result.json")" not-accepted
check "two models in one transcript is :unverified" "$(FAKE_TWO_MODELS=1 run N 93)" 3

echo "== 14. FOLLOW-UP b/c — sampled load, and a wall that means its label =="
check "the sampler wrote samples, not two endpoints" "$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["load"]["samples"]>0)' "$A/arm.json")" True
check "contamination is classified per phase" "$(python3 -c 'import json,sys;d=json.load(open(sys.argv[1]))["load"];print("contaminated_driver" in d and "contaminated_acceptance" in d)' "$A/arm.json")" True
check "an unsampled phase is UNKNOWN, never clean" "$(python3 -c 'import json,sys;d=json.load(open(sys.argv[1]))["load"];print(d["contaminated_acceptance"] is None or isinstance(d["contaminated_acceptance"],bool))' "$A/arm.json")" True
check "the wall scope no longer over-claims" "$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["timing"]["adapter_wall_scope"])' "$A/adapter-result.json")" "prepare-through-freeze-and-attestation; excludes the acceptance oracle"
check "and the wall actually ENDS after attestation" "$(python3 -c 'import json,sys;t=json.load(open(sys.argv[1]))["timing"];print(t["adapter_wall_s"]>=t["watch_end_monotonic_s"]-t["adapter_start_monotonic_s"])' "$A/adapter-result.json")" True
check "acceptance has its own measured interval" "$(python3 -c 'import json,sys;t=json.load(open(sys.argv[1]))["timing"];print("acceptance_wall_s" in t and "verified_completion_wall_s" in t)' "$A/adapter-result.json")" True
check "the clock source is declared, not assumed comparable" "$(python3 -c 'import json,sys;print("/proc/uptime" in json.load(open(sys.argv[1]))["timing"]["monotonic_source"])' "$A/adapter-result.json")" True

echo
echo "test_run_opus_arm: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ] || exit 1
