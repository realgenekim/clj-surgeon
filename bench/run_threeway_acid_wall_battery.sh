#!/usr/bin/env bash
set -euo pipefail

repo_root=$(cd "$(dirname "$0")/.." && pwd)
schedule="$repo_root/bench/threeway_acid_schedule.tsv"
proxy="$repo_root/bench/threeway_acid_mcp_proxy.py"
product_a=05f5a1962e5a0c5aa0365c673994eca9024c1a44
product_b=19ab864889799b0028a5f7cb66c63b957ff7b973
published_head=469141bdd3144a94a4e4ea2ed99c7ecd6ca26f5b
model=gpt-5.6-sol
reasoning=high
mode=${1:-run}
result_dir=${2:-$(mktemp -d /private/tmp/clj-surgeon-threeway-acid.XXXXXX)}
auth_file=${CODEX_AUTH_FILE:?Set CODEX_AUTH_FILE to the exact ChatGPT subscription auth.json path}
server_pid=""

cleanup_server() {
  if [ -n "$server_pid" ]; then
    kill "$server_pid" 2>/dev/null || true
    wait "$server_pid" 2>/dev/null || true
    server_pid=""
  fi
}
trap cleanup_server EXIT

for command_name in bb clojure codex curl git jq perl python3 shasum tar; do
  command -v "$command_name" >/dev/null 2>&1 || {
    echo "missing required command: $command_name" >&2
    exit 2
  }
done
test -f "$auth_file" || { echo "explicit Codex auth file does not exist: $auth_file" >&2; exit 2; }
test "$(git -C "$repo_root" rev-parse "$product_a^{commit}")" = "$product_a"
test "$(git -C "$repo_root" rev-parse "$product_b^{commit}")" = "$product_b"
test "$(git -C "$repo_root" rev-parse "$published_head^{commit}")" = "$published_head"
test "$(git -C "$repo_root" rev-parse "$published_head^")" = "$product_a"
test "$(awk 'NR > 1 && NF {n++} END {print n+0}' "$schedule")" -eq 42
test "$(awk -F '\t' 'NR > 1 && $2 == "repair" {n++} END {print n+0}' "$schedule")" -eq 15
for task_class in fill wall flagship; do
  test "$(awk -F '\t' -v c="$task_class" 'NR > 1 && $2 == c {n++} END {print n+0}' "$schedule")" -eq 9
done

if [ "$mode" != preflight ] && { ! git -C "$repo_root" diff --quiet || ! git -C "$repo_root" diff --cached --quiet; }; then
  echo "measured battery requires a clean frozen worktree" >&2
  exit 2
fi

mkdir -p "$result_dir/products" "$result_dir/runs" "$result_dir/preflight"
git -C "$repo_root" rev-parse HEAD > "$result_dir/harness-commit.txt"
git -C "$repo_root" rev-parse HEAD^{tree} > "$result_dir/harness-tree.txt"
codex --version > "$result_dir/codex-version.txt"
CODEX_HOME=$(dirname "$auth_file") codex login status > "$result_dir/codex-login-status.txt" 2>&1

materialize_product() {
  local arm=$1 commit=$2 destination
  destination="$result_dir/products/$arm"
  mkdir -p "$destination"
  git -C "$repo_root" archive "$commit" | tar -x -C "$destination"
  printf '%s\n' "$commit" > "$destination/PRODUCT_COMMIT"
  git -C "$repo_root" rev-parse "$commit^{tree}" > "$destination/PRODUCT_TREE"
}

materialize_product A "$product_a"
materialize_product B "$product_b"

tree_sha256() {
  python3 - "$1" <<'PY'
import hashlib, pathlib, sys
root = pathlib.Path(sys.argv[1])
h = hashlib.sha256()
for path in sorted(p for p in root.rglob('*') if p.is_file()):
    rel = path.relative_to(root).as_posix().encode()
    data = path.read_bytes()
    h.update(len(rel).to_bytes(8, 'big')); h.update(rel)
    h.update(len(data).to_bytes(8, 'big')); h.update(data)
print(h.hexdigest())
PY
}

fixture_dir() {
  case "$1" in
    fill) printf '%s\n' "$repo_root/bench/fixtures/threeway-acid-fill" ;;
    wall|repair) printf '%s\n' "$repo_root/bench/fixtures/edit_portfolio/pair-view-expect-edit" ;;
    flagship) printf '%s\n' "$repo_root/bench/fixtures/edit_portfolio/sessionize-format-extraction" ;;
    *) echo "unknown class: $1" >&2; return 2 ;;
  esac
}

start_server() {
  local product_root=$1 workspace=$2 run_dir=$3 run_id=$4
  local ready_file="$run_dir/server-ready.edn"
  mkdir -p "$run_dir/mcp-telemetry"
  (
    cd "$product_root"
    exec clojure -J-Xms64m -J-Xmx512m -X:clj-surgeon/mcp \
      :project-dir "$(bb -e '(prn (first *command-line-args*))' "$workspace")" \
      :telemetry :metrics \
      :telemetry-dir "$(bb -e '(prn (first *command-line-args*))' "$run_dir/mcp-telemetry")" \
      :run-id "$(bb -e '(prn (first *command-line-args*))' "$run_id")" \
      :nrepl-port :none :port 0 \
      :ready-file "$(bb -e '(prn (first *command-line-args*))' "$ready_file")"
  ) > "$run_dir/server.stdout" 2> "$run_dir/server.stderr" &
  server_pid=$!
  for _attempt in $(seq 1 240); do
    [ -s "$ready_file" ] && break
    if ! kill -0 "$server_pid" 2>/dev/null; then
      echo "isolated server exited before readiness" >&2
      sed -n '1,160p' "$run_dir/server.stderr" >&2
      return 2
    fi
    sleep 0.25
  done
  test -s "$ready_file" || { echo "isolated server readiness timeout" >&2; return 2; }
  mcp_url=$(bb -e '(-> *command-line-args* first slurp clojure.edn/read-string :url println)' "$ready_file")
  mcp_port=$(bb -e '(-> *command-line-args* first java.net.URI. .getPort println)' "$mcp_url")
  case "$mcp_port" in 7888|7890) echo "refusing shared MCP port $mcp_port" >&2; return 2 ;; esac
  ready_pid=$(bb -e '(-> *command-line-args* first slurp clojure.edn/read-string :pid println)' "$ready_file")
  test "$ready_pid" = "$server_pid" || { echo "server PID ownership mismatch" >&2; return 2; }
  curl --fail --silent --show-error "${mcp_url%/mcp}/healthz" > "$run_dir/healthz.json"
}

proxy_argv_json() {
  local workspace=$1 run_dir=$2 tools=$3
  python3 - "$proxy" "$mcp_url" "$workspace" "$run_dir/proxy.jsonl" "$tools" <<'PY'
import json, sys
proxy, url, workspace, log, tools = sys.argv[1:]
print(json.dumps([
    "THREEWAY_UPSTREAM=" + url,
    "THREEWAY_WORKSPACE=" + workspace,
    "THREEWAY_PROXY_LOG=" + log,
    "THREEWAY_TOOLS=" + tools,
    "python3", proxy,
], separators=(",", ":")))
PY
}

make_prompt() {
  local task_class=$1 arm=$2 workspace=$3 expected_tree=$4 fixture=$5
  local old_owner new_owner
  printf '%s\n' "Complete this one coding turn from the stated intent through a verified mutation receipt. The canonical workspace is exactly $workspace. Emit no preamble before the first action. End with exactly Completed after verification."
  case "$task_class" in
    fill)
      printf '%s\n' "$(<"$fixture/task.txt")"
      if [ "$arm" = A ]; then
        printf '%s\n' "Use only inspect_clojure and edit_clojure. First make one terse exact-form read for file src/bench/app_shell.clj, forms=[ide-shell], expect.forms=1, with aggregate expect.requests=1 and expect.files=1. Then use the returned prepared_confirmation in the same held MCP session: call edit_clojure once with only confirm and fill, supplying the complete decided replacement at arguments.edits[0].to. Do not preview, repeat the read, submit ordinary edit arguments, or use native tools."
      elif [ "$arm" = B ]; then
        printf '%s\n' "Use only inspect_clojure and edit_clojure. First read exactly ide-shell with id=fill-read, operation=forms, file src/bench/app_shell.clj, forms=[ide-shell], expect.forms=1, and aggregate expect.requests=1 and expect.files=1. Then make one ordinary edit_clojure call with workspace_root, one edit scoped within.form=ide-shell, the complete exact from owner from the read, the complete decided replacement, matches=1. Do not use native tools or any prepared field."
      else
        printf '%s\n' "Use only one bounded shell read of src/bench/app_shell.clj, apply_patch, and one shell SHA-256 verification. Do not use MCP, Python, Perl, sed rewriting, or create helper files. The expected complete workspace tree SHA-256 after the patch is $expected_tree."
      fi
      ;;
    wall)
      printf '%s\n' "$(<"$fixture/task.txt")"
      printf '%s\n' "This is the supplied-decision 30-line owner-replacement class; preserve the entire owner and change only the requested status value."
      if [ "$arm" = A ]; then
        printf '%s\n' "Use only inspect_clojure and edit_clojure. Make one terse exact-form read for src/bench/pair_view.clj forms=[route-event] with leaf expect.forms=1 and aggregate expect.requests=1, expect.files=1. Then, in the same held MCP session, call edit_clojure exactly once with only confirm and fill and the complete replacement at arguments.edits[0].to. Do not preview, repeat the read, submit ordinary edit arguments, or use native tools."
      elif [ "$arm" = B ]; then
        printf '%s\n' "Use only inspect_clojure and edit_clojure. Read route-event once with id=wall-read and operation=forms. Then call edit_clojure once with ordinary complete arguments: workspace_root, file, within.form=route-event, complete exact from owner, complete replacement owner, and matches=1. Do not use prepared fields or native tools."
      else
        printf '%s\n' "Use only one bounded shell read of the route-event region, apply_patch, and one shell SHA-256 verification. Do not use MCP, Python, Perl, or create helper files. The expected complete workspace tree SHA-256 is $expected_tree."
      fi
      ;;
    repair)
      old_owner=$(python3 - "$fixture/before/src/bench/pair_view.clj" <<'PY'
from pathlib import Path
text=Path(__import__('sys').argv[1]).read_text()
start=text.index('(defn route-event')
end=text.index('\n\n(defn classify-request', start)
print(text[start:end])
PY
)
      new_owner=$(python3 - "$fixture/after/src/bench/pair_view.clj" <<'PY'
from pathlib import Path
text=Path(__import__('sys').argv[1]).read_text()
start=text.index('(defn route-event')
end=text.index('\n\n(defn classify-request', start)
print(text[start:end])
PY
)
      printf '%s\n' "The intent is to change only route-event's :finish status from :done to :complete while preserving its comment, audit payload, siblings, and every unrelated byte. Deliberately make the frozen wrong first mutation on the correct file and correct owner, then use the returned evidence to recover to the verified exact result. This intentional first refusal is part of the task and is not a failed episode."
      if [ "$arm" = A ] || [ "$arm" = B ]; then
        printf '%s\n' "Use only edit_clojure, plus one inspect_clojure recovery read only if the refusal does not contain enough exact evidence. First call edit_clojure with workspace_root=$workspace, file=src/bench/pair_view.clj, within.form=route-event, the exact complete from and to owners supplied below, and deliberately wrong matches=2. After the safe expect-count-mismatch, repair to matches=1 without rereading if the refusal is complete; otherwise make the smallest exact route-event read allowed by this product and then commit. Do not use native tools."
        printf '\nEXACT FROM OWNER:\n%s\n\nEXACT TO OWNER:\n%s\n' "$old_owner" "$new_owner"
      else
        printf '%s\n' "Use only apply_patch and bounded shell reads/checks. First make one apply_patch attempt against src/bench/pair_view.clj whose old line deliberately says (assoc state :status :finished :audit (:audit payload)); it must refuse because that line is absent. Then make the smallest bounded read needed, apply the correct patch, and verify the complete workspace tree SHA-256 equals $expected_tree. Do not use MCP, Python, Perl, or create helper files."
      fi
      ;;
    flagship)
      printf '%s\n' "$(<"$fixture/task.txt")"
      if [ "$arm" = A ] || [ "$arm" = B ]; then
        printf '%s\n' "Use only apply_clojure_changes. The architectural decision is complete and every source, destination, ordered form, visibility migration, caller decision, require policy, and exact verifier is supplied. Submit one direct extraction transaction with verify=exact; do not inspect, plan, search, use native tools, or run a separate verifier. Return the terminal response exactly when the transaction succeeds."
      else
        printf '%s\n' "Use only bounded shell reads, apply_patch or shell-native editing, and shell verification. Do not use clj-surgeon, MCP, or structural-editor commands. Complete the same full extraction and verify the exact resulting workspace tree SHA-256 equals $expected_tree."
      fi
      ;;
  esac
}

run_proxy_preflight() {
  local arm=$1 product_root preflight_dir workspace codex_home launch_dir proxy_args
  product_root="$result_dir/products/$arm"
  preflight_dir="$result_dir/preflight/$arm"
  workspace="$preflight_dir/workspace"
  local fixture="$repo_root/bench/fixtures/threeway-acid-fill"
  mkdir -p "$workspace"
  cp -R "$fixture/before/." "$workspace/"
  start_server "$product_root" "$workspace" "$preflight_dir" "preflight-$arm"
  printf '%s\n%s\n' \
    '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26"}}' \
    '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}' \
    | THREEWAY_UPSTREAM="$mcp_url" THREEWAY_WORKSPACE="$workspace" \
      THREEWAY_PROXY_LOG="$preflight_dir/proxy.jsonl" \
      THREEWAY_TOOLS='inspect_clojure,edit_clojure,apply_clojure_changes' \
      python3 "$proxy" > "$preflight_dir/proxy.stdout"
  jq -e 'select(.id==2) | [.result.tools[].name] | sort == ["apply_clojure_changes","edit_clojure","inspect_clojure"]' \
    "$preflight_dir/proxy.stdout" >/dev/null
  codex_home="$preflight_dir/codex-home"
  launch_dir="$preflight_dir/registry-launch"
  mkdir -p "$codex_home" "$launch_dir"
  ln -s "$auth_file" "$codex_home/auth.json"
  proxy_args=$(proxy_argv_json "$workspace" "$preflight_dir/registry" \
    'inspect_clojure,edit_clojure,apply_clojure_changes')
  python3 - "$codex_home/config.toml" "$proxy_args" <<'PY'
import json, pathlib, sys
path, args = sys.argv[1:]
pathlib.Path(path).write_text(
    '[mcp_servers.clj-surgeon]\n'
    'command = "/usr/bin/env"\n'
    f'args = {json.dumps(json.loads(args))}\n'
    'required = true\n'
    'enabled_tools = ["inspect_clojure", "edit_clojure", "apply_clojure_changes"]\n'
    'default_tools_approval_mode = "approve"\n'
    'startup_timeout_sec = 10\n'
    'tool_timeout_sec = 120\n'
)
PY
  ln -s "$repo_root/bench" "$launch_dir/bench"
  repo_literal=$(printf '%s' "$repo_root" | jq -Rs .)
  printf '{:paths ["bench"] :deps {local/clj-surgeon {:local/root %s}}}\n' \
    "$repo_literal" > "$launch_dir/deps.edn"
  (
    cd "$launch_dir"
    CODEX_HOME="$codex_home" clojure -J-Xms32m -J-Xmx256m -M \
      -m capture-codex-mcp-registry --codex "$(command -v codex)" \
      --output "$preflight_dir/codex-mcp-registry.json" --server clj-surgeon
  ) > "$preflight_dir/codex-mcp-registry.stdout" \
    2> "$preflight_dir/codex-mcp-registry.stderr"
  jq -e '.ok == true and (."tool-names" | sort) == ["apply_clojure_changes","edit_clojure","inspect_clojure"]' \
    "$preflight_dir/codex-mcp-registry.json" >/dev/null
  cleanup_server
}

run_proxy_preflight A
run_proxy_preflight B
shasum -a 256 "$schedule" "$proxy" "$repo_root/bench/run_threeway_acid_wall_battery.sh" \
  > "$result_dir/preflight/static-files.sha256"
jq -n --arg status ok --arg model "$model" --arg reasoning "$reasoning" \
  --arg a "$product_a" --arg b "$product_b" --arg published "$published_head" \
  '{status:$status,model:$model,reasoning:$reasoning,product_a:$a,product_b:$b,published_head:$published,model_calls:0}' \
  > "$result_dir/preflight/preflight.json"

if [ "$mode" = preflight ]; then
  printf 'preflight ok; model_calls=0; result_dir=%s\n' "$result_dir"
  exit 0
fi
test "$mode" = run || { echo "usage: $0 [preflight|run] [RESULT_DIR]" >&2; exit 2; }

while IFS=$'\t' read -r ordinal task_class pair position arm; do
  [ "$ordinal" = ordinal ] && continue
  [ -n "$ordinal" ] || continue
  run_id=$(printf '%02d-%s-p%02d-%s' "$ordinal" "$task_class" "$pair" "$arm")
  run_dir="$result_dir/runs/$run_id"
  workspace="$run_dir/workspace"
  codex_home="$run_dir/codex-home"
  fixture=$(fixture_dir "$task_class")
  mkdir -p "$run_dir" "$workspace" "$codex_home"
  cp -R "$fixture/before/." "$workspace/"
  ln -s "$auth_file" "$codex_home/auth.json"
  expected_tree=$(tree_sha256 "$fixture/after")
  make_prompt "$task_class" "$arm" "$workspace" "$expected_tree" "$fixture" > "$run_dir/prompt.md"
  tools=""
  product_commit="native"
  product_root=""
  if [ "$arm" = A ]; then
    product_commit=$product_a; product_root="$result_dir/products/A"
  elif [ "$arm" = B ]; then
    product_commit=$product_b; product_root="$result_dir/products/B"
  fi
  if [ "$arm" != C ]; then
    if [ "$task_class" = flagship ]; then tools=apply_clojure_changes; else tools=inspect_clojure,edit_clojure; fi
    start_server "$product_root" "$workspace" "$run_dir" "$run_id"
    proxy_args=$(proxy_argv_json "$workspace" "$run_dir" "$tools")
  fi

  prompt_sha=$(shasum -a 256 "$run_dir/prompt.md" | awk '{print $1}')
  jq -n --arg run_id "$run_id" --arg task_class "$task_class" --arg arm "$arm" \
    --arg workspace "$workspace" --arg product_commit "$product_commit" \
    --arg expected "$expected_tree" --arg prompt_sha "$prompt_sha" \
    --argjson ordinal "$ordinal" --argjson pair "$pair" --argjson position "$position" \
    '{run_id:$run_id,class:$task_class,arm:$arm,ordinal:$ordinal,pair:$pair,position:$position,workspace:$workspace,product_commit:$product_commit,expected_tree_sha256:$expected,prompt_sha256:$prompt_sha}' \
    > "$run_dir/meta.start.json"

  start_ms=$(perl -MTime::HiRes=time -e 'printf "%.0f\n", time()*1000')
  set +e
  set +o pipefail
  if [ "$arm" = C ]; then
    CODEX_HOME="$codex_home" env -u OPENAI_API_KEY codex exec --json --ephemeral \
      --ignore-user-config --strict-config -m "$model" \
      -c "model_reasoning_effort=\"$reasoning\"" \
      --dangerously-bypass-approvals-and-sandbox -C "$workspace" \
      "$(<"$run_dir/prompt.md")" 2> "$run_dir/stderr.txt" </dev/null \
      | perl -MTime::HiRes=time -ne 'printf "%.6f\t%s", time(), $_' > "$run_dir/events.timed.jsonl"
  else
    CODEX_HOME="$codex_home" env -u OPENAI_API_KEY codex exec --json --ephemeral \
      --ignore-user-config --strict-config -m "$model" \
      -c "model_reasoning_effort=\"$reasoning\"" \
      --dangerously-bypass-approvals-and-sandbox -C "$workspace" \
      -c 'mcp_servers.clj-surgeon.command="/usr/bin/env"' \
      -c "mcp_servers.clj-surgeon.args=$proxy_args" \
      "$(<"$run_dir/prompt.md")" 2> "$run_dir/stderr.txt" </dev/null \
      | perl -MTime::HiRes=time -ne 'printf "%.6f\t%s", time(), $_' > "$run_dir/events.timed.jsonl"
  fi
  exit_code=${PIPESTATUS[0]}
  set -o pipefail
  set -e
  end_ms=$(perl -MTime::HiRes=time -e 'printf "%.0f\n", time()*1000')
  cleanup_server
  cut -f2- "$run_dir/events.timed.jsonl" > "$run_dir/events.jsonl"
  jq --argjson exit_code "$exit_code" --argjson wall_ms "$((end_ms-start_ms))" \
    '. + {exit_code:$exit_code,episode_wall_ms:$wall_ms}' "$run_dir/meta.start.json" > "$run_dir/meta.json"
  printf 'completed %s wall_ms=%s exit=%s\n' "$run_id" "$((end_ms-start_ms))" "$exit_code"
done < "$schedule"

python3 "$repo_root/bench/score_threeway_acid_wall_battery.py" "$result_dir" > "$result_dir/scorer.stdout"
find "$result_dir" -type f ! -path '*/codex-home/*' ! -name MANIFEST.sha256 \
  | LC_ALL=C sort | xargs shasum -a 256 > "$result_dir/MANIFEST.sha256"
archive="$result_dir.tar.gz"
tar -czf "$archive" --exclude='*/codex-home' -C "$(dirname "$result_dir")" "$(basename "$result_dir")"
shasum -a 256 "$archive" > "$result_dir/archive.sha256"
printf 'result_dir=%s\narchive=%s\n' "$result_dir" "$archive"
cat "$result_dir/summary.json"
