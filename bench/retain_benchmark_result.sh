#!/usr/bin/env bash
set -euo pipefail

repo_root=$(cd "$(dirname "$0")/.." && pwd -P)
archive_root=${BENCH_ARCHIVE_ROOT:-"$(dirname "$repo_root")/clj-surgeon-bench-archive"}

is_raw_bulk_path() {
  local path=$1
  local base=${path##*/}
  case "$path" in
    */workspace/*|*/workspace) return 0 ;;
  esac
  case "$base" in
    events.jsonl|event-clock.tsv|raw.jsonl|prompt.txt|final.txt|stderr.txt|commands.json|started-items.json|command.txt|result.json|source.start.clj|source.diff|expected.clj|jq-errors.txt|tool-calls.jsonl|SKILL.md)
      return 0
      ;;
    *) return 1 ;;
  esac
}

verify_tracked() {
  local failed=0 path
  while IFS= read -r path; do
    if is_raw_bulk_path "$path" && { [ -e "$repo_root/$path" ] || [ -L "$repo_root/$path" ]; }; then
      printf 'Tracked benchmark raw bulk violates retention policy: %s\n' "$path" >&2
      failed=1
    fi
  done < <(git -C "$repo_root" ls-files bench/results)
  if [ "$failed" -ne 0 ]; then
    return 1
  fi
  printf '%s\n' 'Benchmark retention verification passed: no raw bulk is tracked.'
}

retain_result() {
  local supplied=$1 parent base result_real prefix date_part archive_dir archive_path
  local effective_archive_root=${BENCH_ARCHIVE_ROOT:-$archive_root}
  parent=$(cd "$(dirname "$supplied")" && pwd -P)
  base=$(basename "$supplied")
  result_real="$parent/$base"
  prefix="$repo_root/bench/results/"

  case "$result_real" in
    "$prefix"*) ;;
    *)
      printf 'Refusing retention outside %s: %s\n' "$prefix" "$result_real" >&2
      return 2
      ;;
  esac
  if [ "$result_real" = "${prefix%/}" ] || [ ! -d "$result_real" ]; then
    printf 'Refusing invalid benchmark result directory: %s\n' "$result_real" >&2
    return 2
  fi
  if [ ! -f "$result_real/runs.tsv" ]; then
    printf 'Refusing incomplete result without runs.tsv: %s\n' "$result_real" >&2
    return 2
  fi
  if [ -e "$result_real/.result-owner" ]; then
    printf 'Refusing to retain a result with an active owner: %s\n' "$result_real" >&2
    return 2
  fi

  case "$base" in
    ????-??-??-*) date_part=${base:0:10} ;;
    *) date_part=$(date +%F) ;;
  esac
  archive_dir="$effective_archive_root/$date_part"
  archive_path="$archive_dir/$base.tar.gz"
  mkdir -p "$archive_dir"
  if [ -e "$archive_path" ]; then
    printf 'Refusing to replace existing benchmark archive: %s\n' "$archive_path" >&2
    return 2
  fi

  local archive_stage="$archive_path.tmp.$$"
  tar -czf "$archive_stage" -C "$parent" "$base"
  mv "$archive_stage" "$archive_path"
  local archive_sha
  archive_sha=$(shasum -a 256 "$archive_path" | awk '{print $1}')

  local path
  while IFS= read -r -d '' path; do
    if is_raw_bulk_path "$path"; then
      if [ -d "$path" ] && [ ! -L "$path" ]; then
        find "$path" -depth -delete
      else
        unlink "$path"
      fi
    fi
  done < <(find "$result_real" -depth \( -type f -o -type l -o -type d -name workspace \) -print0)

  local receipt_stage="$result_real/.archive-receipt.edn.tmp.$$"
  printf '%s\n' \
    '{:retention-version 1' \
    " :archive \"$date_part/$base.tar.gz\"" \
    " :archive-sha256 \"$archive_sha\"" \
    ' :git-content :structured-only}' \
    > "$receipt_stage"
  mv "$receipt_stage" "$result_real/archive-receipt.edn"

  local manifest_stage="$result_real/.MANIFEST.sha256.tmp.$$"
  (
    cd "$result_real"
    find . -type f ! -name 'MANIFEST.sha256*' ! -name '.MANIFEST.sha256*' -print \
      | LC_ALL=C sort \
      | while IFS= read -r file; do shasum -a 256 "$file"; done
  ) > "$manifest_stage"
  mv "$manifest_stage" "$result_real/MANIFEST.sha256"

  printf 'Archived complete benchmark evidence: %s\n' "$archive_path"
  printf 'Retained structured Git evidence: %s\n' "$result_real"
}

self_test() (
  local root result archive archive_file listing
  root=$(mktemp -d /tmp/clj-surgeon-retention-self-test.XXXXXX)
  result="$repo_root/bench/results/retention-self-test-$$"
  archive="$root/archive"
  archive_file="$archive/$(date +%F)/retention-self-test-$$.tar.gz"
  listing="$root/archive.list"
  trap 'find "$result" -depth -delete 2>/dev/null || true; find "$root" -depth -delete 2>/dev/null || true' EXIT
  mkdir -p "$result/run/workspace/src"
  printf '%s\n' 'run_id' > "$result/runs.tsv"
  printf '%s\n' 'correct=true' > "$result/run/score.tsv"
  printf '%s\n' 'secret transcript' > "$result/run/events.jsonl"
  printf '%s\n' $'1\t1000000\t1000\t18' > "$result/run/event-clock.tsv"
  printf '%s\n' '{:schema :clj-surgeon.benchmark-event-timing/v1}' \
    > "$result/run/phase-timing.edn"
  printf '%s\n' 'prompt' > "$result/run/prompt.txt"
  printf '%s\n' 'source' > "$result/run/workspace/src/input.clj"
  git -C "$repo_root" check-ignore -q "${result#"$repo_root/"}/run/events.jsonl"
  git -C "$repo_root" check-ignore -q "${result#"$repo_root/"}/run/workspace/src/input.clj"
  BENCH_ARCHIVE_ROOT="$archive" retain_result "$result"
  test -f "$archive_file"
  test -f "$result/runs.tsv"
  test -f "$result/run/score.tsv"
  test -f "$result/archive-receipt.edn"
  test -f "$result/MANIFEST.sha256"
  test ! -e "$result/run/events.jsonl"
  test ! -e "$result/run/event-clock.tsv"
  test -f "$result/run/phase-timing.edn"
  test ! -e "$result/run/prompt.txt"
  test ! -e "$result/run/workspace"
  tar -tzf "$archive_file" > "$listing"
  grep -q '/run/events.jsonl$' "$listing"
  grep -q '/run/event-clock.tsv$' "$listing"
  printf '%s\n' 'Benchmark retention self-test passed.'
)

case "${1:-}" in
  --verify-tracked) verify_tracked ;;
  --self-test) self_test ;;
  '')
    printf 'Usage: %s RESULT_DIR | --verify-tracked | --self-test\n' "$0" >&2
    exit 2
    ;;
  *) retain_result "$1" ;;
esac
