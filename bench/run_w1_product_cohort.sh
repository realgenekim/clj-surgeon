#!/usr/bin/env bash
set -euo pipefail

repo_root=$(cd "$(dirname "$0")/.." && pwd)
base_commit=05f5a1962e5a0c5aa0365c673994eca9024c1a44
model=gpt-5.6-sol
reasoning=high
result_dir=${1:-$(mktemp -d /private/tmp/clj-surgeon-w1-product-cohort.XXXXXX)}
auth_file=${CODEX_AUTH_FILE:-$HOME/.codex/auth.json}
schedule="$repo_root/bench/w1_product_cohort_schedule.tsv"
fixture="$repo_root/bench/fixtures/edit_portfolio/pair-view-expect-edit"
ready_file="$result_dir/server-ready.edn"
server_pid=""

cleanup() {
  if [ -n "$server_pid" ]; then
    kill "$server_pid" 2>/dev/null || true
    wait "$server_pid" 2>/dev/null || true
  fi
}
trap cleanup EXIT

for command_name in bb clojure codex curl git jq perl python3 shasum tar; do
  command -v "$command_name" >/dev/null 2>&1 || {
    echo "missing required command: $command_name" >&2
    exit 2
  }
done

test -f "$auth_file" || { echo "Codex auth file not found at the explicit path: $auth_file" >&2; exit 2; }
test "$(git -C "$repo_root" rev-parse "$base_commit^{commit}")" = "$base_commit"
test -z "$(git -C "$repo_root" diff "$base_commit" -- src resources deps.edn bb.edn)" || {
  echo "product source differs from the frozen published commit" >&2
  exit 2
}
test "$(awk 'NR > 1 && NF {count++} END {print count+0}' "$schedule")" -eq 16

mkdir -p "$result_dir/workspaces" "$result_dir/mcp-telemetry"
git -C "$repo_root" rev-parse HEAD > "$result_dir/harness-commit.txt"
git -C "$repo_root" rev-parse "$base_commit^{tree}" > "$result_dir/product-tree.txt"
git -C "$repo_root" diff "$base_commit" -- src resources deps.edn bb.edn > "$result_dir/product-source.diff"
shasum -a 256 "$result_dir/product-source.diff" > "$result_dir/product-source.diff.sha256"
codex --version > "$result_dir/codex-version.txt"

python3 -m venv "$result_dir/tokenizer-venv"
"$result_dir/tokenizer-venv/bin/python" -m pip install --disable-pip-version-check \
  -r "$repo_root/bench/w1_product_cohort_requirements.txt" \
  > "$result_dir/tokenizer-install.txt" 2>&1
"$result_dir/tokenizer-venv/bin/python" -m pip freeze > "$result_dir/tokenizer-freeze.txt"

server_started_ms=$(perl -MTime::HiRes=time -e 'printf "%.0f\n", time()*1000')
(
  cd "$repo_root"
  exec clojure -J-Xms64m -J-Xmx512m -X:clj-surgeon/mcp \
    :project-dir "$(bb -e '(prn (first *command-line-args*))' "$result_dir/workspaces")" \
    :telemetry :metrics \
    :telemetry-dir "$(bb -e '(prn (first *command-line-args*))' "$result_dir/mcp-telemetry")" \
    :run-id '"w1-product-cohort-20260831"' \
    :nrepl-port :none \
    :port 0 \
    :ready-file "$(bb -e '(prn (first *command-line-args*))' "$ready_file")"
) > "$result_dir/server.stdout" 2> "$result_dir/server.stderr" &
server_pid=$!

for _attempt in $(seq 1 240); do
  [ -s "$ready_file" ] && break
  if ! kill -0 "$server_pid" 2>/dev/null; then
    echo "isolated MCP server exited before readiness" >&2
    sed -n '1,160p' "$result_dir/server.stderr" >&2
    exit 2
  fi
  sleep 0.25
done
test -s "$ready_file" || { echo "isolated MCP server did not become ready" >&2; exit 2; }
mcp_url=$(bb -e '(-> *command-line-args* first slurp clojure.edn/read-string :url println)' "$ready_file")
mcp_port=$(bb -e '(-> *command-line-args* first java.net.URI. .getPort println)' "$mcp_url")
case "$mcp_port" in
  7888|7890) echo "refusing shared MCP port: $mcp_port" >&2; exit 2 ;;
esac
ready_pid=$(bb -e '(-> *command-line-args* first slurp clojure.edn/read-string :pid println)' "$ready_file")
test "$ready_pid" = "$server_pid" || { echo "server PID ownership mismatch" >&2; exit 2; }
curl --fail --silent --show-error "${mcp_url%/mcp}/healthz" > "$result_dir/healthz.json"
server_ready_ms=$(perl -MTime::HiRes=time -e 'printf "%.0f\n", time()*1000')
printf '%s\n' "$((server_ready_ms - server_started_ms))" > "$result_dir/server-bootstrap-ms.txt"

expected_tree_sha256=$("$result_dir/tokenizer-venv/bin/python" - "$fixture/after" <<'PY'
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
)
printf '%s\n' "$expected_tree_sha256" > "$result_dir/expected-tree.sha256"

make_prompt() {
  local arm=$1 workspace=$2
  printf '%s\n' "TASK: In src/bench/pair_view.clj, change only the :finish result inside route-event so its :status value is :complete instead of :done. Preserve its attached comment, the audit payload, and every unrelated byte."
  printf '%s\n' "Use only the advertised clj-surgeon MCP tools. Do not use shell commands, native file readers, file-change tools, or any other MCP tool. Emit no preamble before the first tool call."
  printf '%s\n' "The canonical workspace_root is exactly $workspace. First call inspect_clojure exactly once with this semantic object: workspace_root=$workspace; one request for file src/bench/pair_view.clj and forms=[route-event] with expect.forms=1; aggregate expect.requests=1 and expect.files=1."
  if [ "$arm" = C ]; then
    printf '%s\n' "After the read, independently decide the exact complete replacement. Use the served prepared_confirmation: call edit_clojure with only confirm, fill, and preview=true. Review the complete inert preview. If it represents the requested exact change, call edit_clojure exactly once more with the identical confirm and fill and no preview field. Do not submit prepared_request.arguments or an ordinary full edit."
  else
    printf '%s\n' "After the read, independently decide the exact complete replacement. Ignore prepared_confirmation and prepared_request as execution entrances. Compose the complete ordinary edit_clojure arguments from the read evidence: workspace_root plus one edit naming file src/bench/pair_view.clj, within.form route-event, the complete exact from form, the complete replacement to form, and expect.matches=1. Commit exactly once. Do not use confirm, fill, or preview."
  fi
  printf '%s\n' "Treat a successful verification_complete=true commit receipt as terminal evidence. Then reply exactly Completed."
}

while IFS=$'\t' read -r ordinal pair arm; do
  [ "$ordinal" = ordinal ] && continue
  run_id=$(printf '%02d-p%02d-%s' "$ordinal" "$pair" "$arm")
  run_dir="$result_dir/$run_id"
  workspace="$run_dir/workspace"
  codex_home="$run_dir/codex-home"
  mkdir -p "$run_dir" "$workspace" "$codex_home"
  cp -R "$fixture/before/." "$workspace/"
  ln -s "$auth_file" "$codex_home/auth.json"
  {
    printf '%s\n' '[mcp_servers.clj-surgeon]'
    printf 'url = "%s"\n' "$mcp_url"
    printf '%s\n' 'required = true'
    printf '%s\n' 'enabled_tools = ["inspect_clojure", "edit_clojure"]'
    printf '%s\n' 'default_tools_approval_mode = "writes"'
    printf '%s\n' 'startup_timeout_sec = 5'
    printf '%s\n' 'tool_timeout_sec = 60'
  } > "$codex_home/config.toml"
  make_prompt "$arm" "$workspace" > "$run_dir/prompt.md"

  start_ms=$(perl -MTime::HiRes=time -e 'printf "%.0f\n", time()*1000')
  set +e
  set +o pipefail
  CODEX_HOME="$codex_home" PATH="/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin" \
    codex exec --json --ephemeral --ignore-rules --skip-git-repo-check \
      --sandbox read-only --color never -m "$model" \
      -c "model_reasoning_effort=\"$reasoning\"" \
      -C "$workspace" "$(cat "$run_dir/prompt.md")" \
      2> "$run_dir/stderr.txt" </dev/null \
    | perl -MTime::HiRes=time -ne 'printf "%.6f\t%s", time(), $_' \
      > "$run_dir/events.timed.jsonl"
  exit_code=${PIPESTATUS[0]}
  set -o pipefail
  set -e
  end_ms=$(perl -MTime::HiRes=time -e 'printf "%.0f\n", time()*1000')
  episode_wall_ms=$((end_ms - start_ms))

  cut -f2- "$run_dir/events.timed.jsonl" > "$run_dir/events.jsonl"
  jq -n \
    --arg run_id "$run_id" --arg arm "$arm" --arg workspace "$workspace" \
    --arg expected "$expected_tree_sha256" --argjson ordinal "$ordinal" \
    --argjson pair "$pair" --argjson exit_code "$exit_code" \
    --argjson episode_wall_ms "$episode_wall_ms" \
    '{run_id:$run_id,ordinal:$ordinal,pair:$pair,arm:$arm,workspace:$workspace,expected_tree_sha256:$expected,exit_code:$exit_code,episode_wall_ms:$episode_wall_ms}' \
    > "$run_dir/meta.json"
  printf 'completed %s wall_ms=%s exit=%s\n' "$run_id" "$episode_wall_ms" "$exit_code"
done < "$schedule"

"$result_dir/tokenizer-venv/bin/python" "$repo_root/bench/score_w1_product_cohort.py" "$result_dir" \
  > "$result_dir/scorer.stdout"

find "$result_dir" -type f ! -path '*/tokenizer-venv/*' ! -path '*/codex-home/*' ! -name MANIFEST.sha256 \
  | LC_ALL=C sort | xargs shasum -a 256 > "$result_dir/MANIFEST.sha256"
archive="$result_dir.tar.gz"
tar -czf "$archive" --exclude='*/tokenizer-venv' --exclude='*/codex-home' \
  -C "$(dirname "$result_dir")" "$(basename "$result_dir")"
shasum -a 256 "$archive" > "$result_dir/raw-archive.sha256"
printf 'result_dir=%s\narchive=%s\n' "$result_dir" "$archive"
cat "$result_dir/summary.json"
