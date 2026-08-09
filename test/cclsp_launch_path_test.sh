#!/bin/sh
set -eu

test_root=$(mktemp -d -t clj-surgeon-cclsp-path.XXXXXX)
trap 'rm -rf "$test_root"' EXIT HUP INT TERM

test_home="$test_root/home"
fake_bin="$test_root/bin"
state_dir="$test_root/state"
cclsp_home="$test_root/cclsp"
config_file="$test_home/.local/state/clj-surgeon/cclsp.json"
launch_calls="$test_root/launch-calls"
launched="$test_root/launched"

mkdir -p "$fake_bin" "$state_dir" "$cclsp_home/node_modules/.bin" \
  "$(dirname "$config_file")"
printf '{"mcpServers": []}\n' >"$config_file"
: >"$launch_calls"

cat >"$fake_bin/curl" <<'EOF'
#!/bin/sh
set -eu
if [ -f "$TEST_LAUNCHED" ]; then
  printf '{"ok":true,"config_path":"%s"}\n' "$TEST_CONFIG_FILE"
  exit 0
fi
exit 28
EOF

cat >"$fake_bin/launchctl" <<'EOF'
#!/bin/sh
set -eu
printf '%s\n' "$*" >>"$TEST_LAUNCH_CALLS"
case "${1:-}" in
  print) test -f "$TEST_LAUNCHED" ;;
  remove) exit 0 ;;
  submit) : >"$TEST_LAUNCHED" ;;
esac
EOF

cat >"$cclsp_home/node_modules/.bin/bun" <<'EOF'
#!/bin/sh
exit 99
EOF

chmod +x "$fake_bin/curl" "$fake_bin/launchctl" \
  "$cclsp_home/node_modules/.bin/bun"

inherited_path="$fake_bin:/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin"
HOME="$test_home" \
PATH="$inherited_path" \
TEST_LAUNCHED="$launched" \
TEST_CONFIG_FILE="$config_file" \
TEST_LAUNCH_CALLS="$launch_calls" \
make --no-print-directory -C "$(pwd)" cclsp-start \
  CCLSP_STATE_DIR="$state_dir" \
  CCLSP_HOME="$cclsp_home" \
  CCLSP_HEALTH_ATTEMPTS=1 \
  CCLSP_HEALTH_INTERVAL=0

grep -Fq "export PATH=\"\$3\"" "$launch_calls"
grep -Fq "$inherited_path" "$launch_calls"
grep -Fq "$config_file" "$launch_calls"
grep -Fq ':server-restarted false' "$state_dir/last-start.edn"

echo "cclsp launch PATH regression passed"
