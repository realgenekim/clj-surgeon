#!/bin/sh
set -eu

printf 'user\t%s\n' "$(id -un)"
printf 'cwd\t%s\n' "$(pwd -P)"
resolved=$(command -v clj-surgeon 2>/dev/null || true)
printf 'resolved_cli\t%s\n' "$resolved"

if [ -z "$resolved" ]; then
  printf 'receipt_state\tabsent-cli\n'
  exit 0
fi

receipt="${resolved}.receipt.edn"
if [ ! -f "$receipt" ]; then
  printf 'receipt_state\tmissing\n'
  printf 'receipt_path\t%s\n' "$receipt"
  exit 0
fi

printf 'receipt_state\tpresent\n'
printf 'receipt_path\t%s\n' "$receipt"
sha256sum "$resolved" "$receipt"
sed -n '1,80p' "$receipt"
