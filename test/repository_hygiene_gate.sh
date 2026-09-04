#!/bin/sh
# @spec MCP-OP-ALIAS-036
# @spec MCP-OP-ALIAS-053
#
# The machine-local build cache is tracked nowhere in this repository, at ANY
# depth, and a gitignore rule covers it at any depth. Fails closed: a working
# tree where git cannot answer is a working tree whose hygiene is unobservable,
# which is a failure and never a pass.
set -u

repository="${1:-.}"
cd "$repository" || { echo "ERROR: cannot enter $repository"; exit 1; }

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "ERROR: git is unusable in $repository, so repository hygiene cannot be"
  echo "       observed; this gate fails closed rather than passing on an"
  echo "       empty view."
  exit 1
fi

# git's exit status is checked on its OWN, never through a pipeline: the status
# of `git ls-files | grep` is grep's, so a git that cannot produce the
# inventory is indistinguishable from an inventory with no cache in it — the
# gate would report green precisely when it can see nothing.
if ! inventory=$(git ls-files 2>/dev/null); then
  echo "ERROR: git cannot produce the file inventory for $repository, so"
  echo "       repository hygiene cannot be observed; this gate fails closed"
  echo "       rather than passing on an empty view."
  exit 1
fi

tracked=$(printf '%s\n' "$inventory" | grep -E '(^|/)\.cpcache/') || tracked=""
if [ -n "$tracked" ]; then
  echo "ERROR: machine-local .cpcache files are tracked:"
  echo "$tracked"
  exit 1
fi

for probe in .cpcache/probe.marker sub/nested/.cpcache/probe.marker; do
  if ! git check-ignore -q "$probe"; then
    echo "ERROR: $probe is not covered by any gitignore rule"
    exit 1
  fi
done

echo "repository hygiene: no machine-local build cache is tracked at any depth"
