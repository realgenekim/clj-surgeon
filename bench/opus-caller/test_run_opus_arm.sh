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
{
  printf '{"type":"mode","mode":"normal","sessionId":"%s"}\n' "$SID"
  printf '{"type":"assistant","sessionId":"%s","timestamp":"2026-09-05T00:00:00Z","message":{"model":"%s","content":[{"type":"tool_use","id":"t1","name":"Edit","input":{"file_path":"src/acid/fanout/ns_001.clj"}}]}}\n' "$SID" "$MODEL"
  printf '{"type":"assistant","sessionId":"%s","timestamp":"2026-09-05T00:00:01Z","message":{"model":"%s","content":[{"type":"tool_use","id":"t2","name":"Bash","input":{"command":"bin/fan-test"}}]}}\n' "$SID" "$MODEL"
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

export PATH="$FX/bin:$PATH"
export OPUSCALLER_FX="$FX"
export CLAUDE_PROJECTS_ROOT="$FX/projects"
export RUNTIME_ALLOWED=1
export OPUS_FIXTURE_SRC="$BASE" OPUS_FIXTURE_SHA="$BASE_SHA"
export OPUS_PROMPT_DIR="$FX/prompts"
export OPUS_ORACLE="$FX/bin/stub-oracle.sh" OPUS_ORACLE_SHA256="$ORACLE_SHA"
export OPUS_ORACLE_FIX="$FX/oracle" OPUS_ARMS_ROOT="$ARMS"
export OPUS_MAX_WALL=60

run() { bash "$HERE/run-opus-arm.sh" "$@" > "$FX/last.out" 2>&1; echo $?; }

echo "== 1. a native arm end to end =="
rc=$(run N 1); A="$ARMS/opus-N-1"
check "exit 0" "$rc" 0
check "clone at the pinned base" "$(git -C "$A/wt" rev-parse "$(cat "$A/base.sha")" 2>/dev/null)" "$BASE_SHA"
[ -s "$A/diff.patch" ] && ok "diff.patch staged ($(wc -l < "$A/diff.patch") lines)" || bad "diff.patch empty"
grep -q 'ns_001' "$A/diff.patch" && ok "diff carries the caller's edit" || bad "diff missing the edit"
grep -q 'NEW-FILE.txt' "$A/status.porcelain" && ok "status.porcelain sees the untracked file" || bad "untracked file not recorded"
check "session bound" "$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["session_bound"])' "$A/arm.json")" True
check "model read from the transcript" "$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["models_in_transcript"][0])' "$A/attribution.json")" claude-opus-5
check "tool calls extracted" "$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["count"])' "$A/calls.json")" 2
check "tool call names" "$(python3 -c 'import json,sys;print(",".join(sorted(json.load(open(sys.argv[1]))["by_name"])))' "$A/calls.json")" Bash,Edit
check "session file sha recorded" "$(python3 -c 'import json,sys;print(bool(json.load(open(sys.argv[1]))["session_sha256"]))' "$A/arm.json")" True
check "oracle ran against the clone" "$(cat "$FX/oracle-called-with.txt" 2>/dev/null)" "$A/wt"
check "oracle verdict recorded verbatim" "$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["oracle_verdict"])' "$A/arm.json")" "rescore-FAN: 6/6 checks passed"
check "correctness from the oracle" "$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["correctness"])' "$A/arm.json")" accepted
check "protected bytes unchanged" "$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["protected_bytes_match"])' "$A/arm.json")" True
grep -q 'task_wall_s=' "$A/task-wall.txt" && ok "monotonic wall recorded" || bad "no task_wall_s"
grep -q 'load_start=' "$A/task-wall.txt" && ok "load at start/end recorded" || bad "no load"
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
rc=$(OPUS_MCP_URL=http://127.0.0.1:8300/mcp run T 1)
check "exit 2 for a forbidden-band port" "$rc" 2
grep -q '8340-8379' "$FX/last.out" && ok "refusal names the band" || bad "wrong refusal: $(head -1 "$FX/last.out")"
rc=$(OPUS_MCP_URL=http://127.0.0.1:8341/mcp run T 2); B="$ARMS/opus-T-2"
check "a well-formed tool arm runs" "$rc" 0
check "the MCP binding is written per arm" "$(python3 -c 'import json,sys;print(json.load(open(sys.argv[1]))["mcpServers"]["clj-surgeon"]["url"])' "$B/mcp.json")" "http://127.0.0.1:8341/mcp"
grep -q 'mcp__clj-surgeon__alias_migration' "$B/prompt.txt" && ok "T prompt names the tool by its Claude name" || bad "T prompt lacks the tool name"
grep -q 'FROZEN PROMPT fanout-tool' "$B/prompt.txt" && ok "T prompt is built on the frozen tool text" || bad "T prompt is not the frozen text"
rc=$(run N 4)
check "a native arm may not carry an MCP URL" "$(OPUS_MCP_URL=http://127.0.0.1:8341/mcp run N 5)" 2

echo "== 6. the optional-adoption cell attaches the tool without a mandate =="
rc=$(OPUS_MCP_URL=http://127.0.0.1:8341/mcp run O 1); C="$ARMS/opus-O-1"
check "exit 0" "$rc" 0
grep -q 'FROZEN PROMPT fanout-common' "$C/prompt.txt" && ok "O prompt is built on the frozen COMMON text" || bad "O prompt is not the common text"
grep -q 'not asked or expected to use it' "$C/prompt.txt" && ok "O prompt carries no mandate" || bad "O prompt lacks the neutral stanza"
grep -q 'Try that operation' "$C/prompt.txt" && bad "O prompt carries the T mandate" || ok "O prompt carries no T mandate"

echo "== 7. no run happens without an allocation =="
rc=$(RUNTIME_ALLOWED=0 run N 9)
check "exit 2" "$rc" 2
grep -q 'RUNTIME_ALLOWED' "$FX/last.out" && ok "refusal names the gate" || bad "wrong refusal: $(head -1 "$FX/last.out")"

echo
echo "test_run_opus_arm: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ] || exit 1
