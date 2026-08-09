#!/bin/sh
set -eu

test_root=$(mktemp -d -t clj-surgeon-cclsp-start.XXXXXX)
trap 'rm -rf "$test_root"' EXIT HUP INT TERM

fake_bin="$test_root/bin"
state_dir="$test_root/state"
cclsp_home="$test_root/cclsp"
config_file=$(python3 -c 'import pathlib,sys; print(pathlib.Path(sys.argv[1]).resolve())' \
  "$test_root/cclsp.json")
curl_count="$test_root/curl-count"
launch_calls="$test_root/launch-calls"

mkdir -p "$fake_bin" "$state_dir" "$cclsp_home/node_modules/.bin"
printf '{"mcpServers": []}\n' >"$config_file"
printf '0\n' >"$curl_count"
: >"$launch_calls"

cat >"$fake_bin/curl" <<'EOF'
#!/bin/sh
set -eu
count=$(cat "$TEST_CURL_COUNT")
count=$((count + 1))
printf '%s\n' "$count" >"$TEST_CURL_COUNT"
if [ "$count" -lt 3 ]; then
  exit 28
fi
printf '{"ok":true,"config_path":"%s"}\n' "$TEST_CONFIG_FILE"
EOF

cat >"$fake_bin/launchctl" <<'EOF'
#!/bin/sh
set -eu
printf '%s\n' "$*" >>"$TEST_LAUNCH_CALLS"
if [ "${1:-}" = "print" ]; then
  exit 0
fi
exit 88
EOF

cat >"$cclsp_home/node_modules/.bin/bun" <<'EOF'
#!/bin/sh
exit 99
EOF

chmod +x "$fake_bin/curl" "$fake_bin/launchctl" \
  "$cclsp_home/node_modules/.bin/bun"

PATH="$fake_bin:$PATH" \
TEST_CURL_COUNT="$curl_count" \
TEST_CONFIG_FILE="$config_file" \
TEST_LAUNCH_CALLS="$launch_calls" \
make --no-print-directory -C "$(pwd)" cclsp-start \
  CCLSP_CONFIG="$config_file" \
  CCLSP_STATE_DIR="$state_dir" \
  CCLSP_HOME="$cclsp_home" \
  CCLSP_HEALTH_ATTEMPTS=3 \
  CCLSP_HEALTH_INTERVAL=0

test "$(cat "$curl_count")" = 3
test "$(grep -c '^print ' "$launch_calls")" = 2
if grep -Eq '^(remove|submit) ' "$launch_calls"; then
  echo "cclsp-start replaced a managed service after a transient health miss" >&2
  exit 1
fi
grep -Fq ':server-restarted false' "$state_dir/last-start.edn"

echo "cclsp-start transient-health regression passed"
