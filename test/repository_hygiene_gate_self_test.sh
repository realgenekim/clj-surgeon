#!/bin/sh
# @spec MCP-OP-ALIAS-053
#
# The gate itself, driven over throwaway repositories: a cache forced in BELOW
# the root must fail it, and a directory where git cannot answer must fail it
# too. The root-anchored gate this replaces passed both.
set -u

gate="$(cd "$(dirname "$0")" && pwd)/repository_hygiene_gate.sh"
failures=0

check() {
  expected="$1"; description="$2"; directory="$3"
  sh "$gate" "$directory" >/dev/null 2>&1
  actual=$?
  if [ "$actual" != "$expected" ]; then
    echo "FAIL: $description (expected exit $expected, got $actual)"
    failures=$((failures + 1))
  else
    echo "ok: $description (exit $actual)"
  fi
}

make_repository() {
  directory=$(mktemp -d)
  git -C "$directory" init -q
  printf '.cpcache/\n' > "$directory/.gitignore"
  printf '(ns demo)\n' > "$directory/demo.clj"
  git -C "$directory" add .gitignore demo.clj
  echo "$directory"
}

clean=$(make_repository)
check 0 "a clean repository passes" "$clean"

deep=$(make_repository)
mkdir -p "$deep/sub/.cpcache"
printf 'machine-local\n' > "$deep/sub/.cpcache/x"
git -C "$deep" add -f sub/.cpcache/x
check 1 "a cache forced in below the root fails the gate" "$deep"

rooted=$(make_repository)
mkdir -p "$rooted/.cpcache"
printf 'machine-local\n' > "$rooted/.cpcache/x"
git -C "$rooted" add -f .cpcache/x
check 1 "a cache forced in at the root fails the gate" "$rooted"

unignored=$(make_repository)
: > "$unignored/.gitignore"
git -C "$unignored" add .gitignore
check 1 "a repository with no ignore rule fails the gate" "$unignored"

outside=$(mktemp -d)
check 1 "a directory git cannot answer for fails the gate" "$outside"

rm -rf "$clean" "$deep" "$rooted" "$unignored" "$outside"

if [ "$failures" != "0" ]; then
  echo "repository hygiene gate self-test: $failures failure(s)"
  exit 1
fi
echo "repository hygiene gate self-test: all cases pass"
