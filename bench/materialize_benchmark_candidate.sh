#!/usr/bin/env bash
set -euo pipefail

if [ "${1:-}" = "--self-test" ]; then
  self_repo=${2:-$(cd "$(dirname "$0")/.." && pwd -P)}
  self_root=$(mktemp -d /tmp/clj-surgeon-candidate-materializer.XXXXXX)
  trap 'rm -rf "$self_root"' EXIT HUP INT TERM
  "$0" "$self_repo" HEAD "$self_root/candidate" >/dev/null
  test -x "$self_root/candidate/bin/clj-surgeon"
  test -f "$self_root/candidate/candidate-receipt.edn"
  test -f "$self_root/candidate/candidate.tar"
  expected_commit=$(git -C "$self_repo" rev-parse HEAD)
  actual_commit=$(bb -e '(-> *command-line-args* first slurp clojure.edn/read-string :source-commit println)' \
    "$self_root/candidate/candidate-receipt.edn")
  test "$actual_commit" = "$expected_commit"
  PATH="$self_root/candidate/bin:$(dirname "$(command -v bb)"):/usr/bin:/bin:/usr/sbin:/sbin" \
    clj-surgeon --help >/dev/null
  printf '%s\n' "candidate materializer self-test passed: $expected_commit"
  exit 0
fi

if [ "$#" -ne 3 ]; then
  echo "usage: $0 REPOSITORY COMMIT DESTINATION" >&2
  exit 64
fi

repository=$(cd "$1" && pwd -P)
source_ref=$2
destination=$3

for command_name in bb git shasum tar; do
  command -v "$command_name" >/dev/null 2>&1 || {
    echo "Missing required command: $command_name" >&2
    exit 69
  }
done

source_commit=$(git -C "$repository" rev-parse --verify "$source_ref^{commit}")
source_tree=$(git -C "$repository" rev-parse --verify "$source_commit^{tree}")

if [ -e "$destination" ]; then
  echo "Refusing to replace candidate destination: $destination" >&2
  exit 73
fi

mkdir -p "$destination/source" "$destination/bin"
archive="$destination/candidate.tar"
git -C "$repository" archive --format=tar --output="$archive" "$source_commit"
tar -xf "$archive" -C "$destination/source"

wrapper="$destination/bin/clj-surgeon"
apply_stage="$wrapper.tmp.${BASHPID:-$$}"
trap 'rm -f "$apply_stage"' EXIT HUP INT TERM
printf '%s\n' \
  '#!/bin/sh' \
  'set -eu' \
  'candidate_root=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd -P)' \
  'exec bb -cp "$candidate_root/source/src" -m clj-surgeon.core "$@"' \
  > "$apply_stage"
chmod +x "$apply_stage"
mv "$apply_stage" "$wrapper"
trap - EXIT HUP INT TERM

archive_sha=$(shasum -a 256 "$archive" | awk '{print $1}')
wrapper_sha=$(shasum -a 256 "$wrapper" | awk '{print $1}')
receipt="$destination/candidate-receipt.edn"
bb -e '
  (let [[output repository source-ref source-commit source-tree archive-sha wrapper-sha]
        *command-line-args*]
    (spit output
          (str (pr-str {:artifact :benchmark-candidate
                        :schema :clj-surgeon.benchmark-candidate/v1
                        :mode :git-archive
                        :source-repository repository
                        :source-ref source-ref
                        :source-commit source-commit
                        :source-tree source-tree
                        :archive "candidate.tar"
                        :archive-sha256 archive-sha
                        :source-root "source"
                        :cli-wrapper "bin/clj-surgeon"
                        :cli-wrapper-sha256 wrapper-sha})
               "\n")))' \
  "$receipt" "$repository" "$source_ref" "$source_commit" "$source_tree" \
  "$archive_sha" "$wrapper_sha"

receipt_sha=$(shasum -a 256 "$receipt" | awk '{print $1}')
printf '%s  %s\n' "$receipt_sha" "candidate-receipt.edn" \
  > "$destination/candidate-receipt.sha256"

printf '%s\n' "$receipt"
