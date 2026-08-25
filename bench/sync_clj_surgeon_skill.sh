#!/usr/bin/env bash
set -euo pipefail

repo_root=$(cd "$(dirname "$0")/.." && pwd)
mode=${1:---check}
canonical="$repo_root/skills/clj-surgeon"
claude="$repo_root/.claude/skills/clj-surgeon"
root_skill="$repo_root/skill.md"
references=(mcp-advanced.md cli-fallback.md advanced-operations.md)

case "$mode" in
  --check|--write) ;;
  *) echo "usage: $0 [--check|--write]" >&2; exit 2 ;;
esac

render_root_skill() {
  sed 's|(references/|(skills/clj-surgeon/references/|g' \
    "$canonical/SKILL.md"
}

if [ "$mode" = --write ]; then
  mkdir -p "$claude/references"
  cp "$canonical/SKILL.md" "$claude/SKILL.md"
  for reference in "${references[@]}"; do
    cp "$canonical/references/$reference" "$claude/references/$reference"
  done
  temporary="$root_skill.tmp.$$"
  trap 'rm -f -- "$temporary"' EXIT HUP INT TERM
  render_root_skill > "$temporary"
  mv "$temporary" "$root_skill"
  trap - EXIT HUP INT TERM
fi

cmp "$canonical/SKILL.md" "$claude/SKILL.md"
for reference in "${references[@]}"; do
  cmp "$canonical/references/$reference" "$claude/references/$reference"
done
rendered=$(mktemp "${TMPDIR:-/tmp}/clj-surgeon-root-skill.XXXXXX")
trap 'rm -f -- "$rendered"' EXIT HUP INT TERM
render_root_skill > "$rendered"
cmp "$rendered" "$root_skill"

printf 'clj-surgeon skill mirrors synchronized (%s)\n' "$mode"
