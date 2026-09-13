#!/usr/bin/env bash
set -u
export TMPDIR=/var/tmp/forge/datacode-fx
export JAVA_TOOL_OPTIONS='-Xmx1024m -Djava.io.tmpdir=/var/tmp/forge/datacode-fx'
round5=docs/observations/2026-09-12-data-not-code/round5
baseline=/var/tmp/forge/datacode-fx/round5-lint-base
files=(src/clj_surgeon/receipt_artifacts.clj test/clj_surgeon/lane_manifest.clj test/clj_surgeon/lane_manifest_test.clj test/clj_surgeon/receipt_artifacts_boundary_test.clj)
base_files=()
for file in "${files[@]}"; do
  mkdir -p "$baseline/$(dirname "$file")"
  git show "4658dc12:$file" > "$baseline/$file"
  base_files+=("$baseline/$file")
done
~/bin/clj-kondo --lint "${base_files[@]}" > "$round5/lint-baseline.log" 2>&1
printf '%s\n' "$?" > "$round5/lint-baseline.exit"
~/bin/clj-kondo --lint "${files[@]}" > "$round5/lint.log" 2>&1
printf '%s\n' "$?" > "$round5/lint.exit"
