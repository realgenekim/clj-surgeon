#!/usr/bin/env bash
# TEST-ISO-009b end-to-end witness — `make battery-fresh` over a REAL git history.
#
# The fast-lane witness (clj-surgeon.battery-ledger-test) drives the classifier as a pure
# function: it proves the DECISION. It cannot prove the plumbing that feeds it — that
# `git diff --raw -z --no-renames` really emits `:000000 100644 <z> <sha> A\0path\0` for a new
# captain's log, that the anchored header regex really matches it, and that `git rev-list
# --parents` really lines up one line per commit. A classifier that is right about a string it
# never receives is worth nothing, so this builds the history with git and asks the real CLI.
#
# The two numbers are the ones the exemption exists for:
#   40 records-only commits since the receipt  -> battery-fresh: OK      (they do not count)
#   31 code commits since the receipt          -> battery-fresh: REFUSED (too-far-behind)
# and the third is the one that keeps it honest:
#   40 records + 31 code                       -> REFUSED. Paperwork never buys code slack.
#
# THE +1 IN EVERY COUNT IS REAL, NOT AN ARTEFACT. `make test-battery` records the sha it TESTED
# and the seat commits the ledger afterwards, so the receipt's own commit is always one commit
# ahead of the sha it names — and the ledger is deliberately never exempt, so it counts. Every
# expected number below therefore reads "1 + the code commits", exactly as it does in the repo.
#
# No JVM, no network, no clone of this repo: an owned scratch repo under /var/tmp/forge.
set -uo pipefail
LEDGER_CLJ=$(cd "$(dirname "$0")" && pwd)/clj_surgeon/battery_ledger.clj
ROOT=${TMPDIR_ROOT:-/var/tmp/forge}
FX=$(mktemp -d "$ROOT/battery-fresh-records-lane.XXXXXX") || exit 2
trap 'rm -rf "$FX"' EXIT
ROWS=0; MIS=0

git init -q "$FX/repo" || exit 2
cd "$FX/repo" || exit 2
git config user.email "witness@example.invalid"; git config user.name "witness"
git config commit.gpgsign false
mkdir -p docs/observations src

# The receipt's tree: one code commit, then the receipt naming it.
echo '(ns x)' > src/x.clj
git add src/x.clj && git commit -q -m "code: the tree the battery ran on"
BASE=$(git rev-parse HEAD)
NOW=$(date -u +%Y-%m-%dT%H:%M:%SZ)
printf '{:sha "%s" :started "%s" :wall_s 727 :verdict :pass :host "witness"}\n' \
  "$BASE" "$NOW" > docs/observations/battery-ledger.edn
git add docs/observations/battery-ledger.edn
git commit -q -m "battery: receipt for the base tree"
RECEIPT=$(git rev-parse HEAD)

check() { bb "$LEDGER_CLJ" check 2>&1; }

row() {  # row <name> <want OK|REFUSED> <extra-substring>
  local name=$1 want=$2 sub=${3:-}; local out rc bad=""
  out=$(check); rc=$?
  ROWS=$((ROWS+1))
  case "$want" in
    OK)      { [ "$rc" = 0 ] && grep -q 'battery-fresh: OK' <<<"$out"; } || bad="rc=$rc" ;;
    REFUSED) { [ "$rc" != 0 ] && grep -q 'battery-fresh: REFUSED' <<<"$out"; } || bad="rc=$rc" ;;
  esac
  [ -z "$sub" ] || grep -qF "$sub" <<<"$out" || bad="$bad want-substring:$sub"
  if [ -n "$bad" ]; then MIS=$((MIS+1)); echo "  MISMATCH $name: $bad"; printf '    %s\n' "$out" | head -6
  else echo "  ok $name  -> $(grep -E 'battery-fresh:' <<<"$out" | head -1 | cut -c1-120)"; fi
}

echo "== battery-fresh records-lane witness (real git history)"

# The receipt commit itself changed ONLY the ledger, and the ledger is never exempt: it counts.
row receipt-alone-is-fresh OK "1 commit(s) behind HEAD"

# 40 records-only commits: 20 NEW files (the records lane's normal act) and 20 modifications.
for i in $(seq 1 20); do
  printf 'a new log, number %s\n' "$i" > "docs/observations/2026-09-08-log-$i.md"
  git add "docs/observations/2026-09-08-log-$i.md"
  git commit -q -m "records: new log $i"
done
for i in $(seq 1 20); do
  printf 'appended line %s\n' "$i" >> docs/observations/2026-09-08-log-1.md
  git add docs/observations/2026-09-08-log-1.md
  git commit -q -m "records: append $i"
done
ROWS=$((ROWS+1))
n=$(git rev-list --count "$RECEIPT..HEAD")
if [ "$n" = 40 ]; then echo "  ok forty-records-commits-built"; else MIS=$((MIS+1)); echo "  MISMATCH forty-records-commits-built: $n"; fi
row forty-records-commits-still-fresh OK "1 commit(s) behind HEAD"

# ...and the audit line must still report the RAW distance, never only the flattering one.
ROWS=$((ROWS+1)); AUDIT=$(check)   # capture first: under `pipefail`, `check | grep` inherits bb's
                                   # nonzero exit on a REFUSED row and reports a false mismatch
if grep -q ':raw-commits-behind 41' <<<"$AUDIT"; then echo "  ok raw-distance-still-reported (raw 41, ignored 40, counted 1)"
else MIS=$((MIS+1)); echo "  MISMATCH raw-distance-still-reported: $(head -2 <<<"$AUDIT")"; fi

# 31 CODE commits on top of the 40 records commits: the budget is untouched, so this refuses --
# and it refuses on 32, not 72. The 40 records commits are still exempt; they simply cannot buy
# the code commits any slack.
for i in $(seq 1 31); do
  printf '(ns x) ;; %s\n' "$i" > src/x.clj
  git add src/x.clj && git commit -q -m "code: change $i"
done
row forty-records-plus-thirty-one-code-refuse REFUSED "32 commits behind"
ROWS=$((ROWS+1)); AUDIT=$(check)
if grep -q ':raw-commits-behind 72' <<<"$AUDIT"; then echo "  ok records-commits-not-charged-to-code (raw 72, counted 32)"
else MIS=$((MIS+1)); echo "  MISMATCH records-commits-not-charged-to-code: $(head -2 <<<"$AUDIT")"; fi

# The 30 budget, at its boundary, with no records churn at all.
git reset -q --hard "$RECEIPT"
for i in $(seq 1 29); do printf '(ns x) ;; %s\n' "$i" > src/x.clj; git add src/x.clj; git commit -q -m "code: $i"; done
row twenty-nine-code-plus-receipt-is-thirty-and-passes OK "30 commit(s) behind HEAD"
printf '(ns x) ;; 30\n' > src/x.clj; git add src/x.clj; git commit -q -m "code: 30"
row one-more-code-commit-refuses-at-thirty-one REFUSED "31 commits behind"

# An executable file under docs/observations/ is NOT records-lane content.
git reset -q --hard "$RECEIPT"
printf '#!/bin/sh\necho hi\n' > docs/observations/run-me.sh; chmod +x docs/observations/run-me.sh
git add docs/observations/run-me.sh && git commit -q -m "records: an executable fixture"
row executable-under-observations-counts OK "2 commit(s) behind HEAD"

# A MIXED commit (a doc and a source file together) is a code commit.
git reset -q --hard "$RECEIPT"
printf 'note\n' > docs/observations/mixed.md; printf '(ns x) ;; mixed\n' > src/x.clj
git add docs/observations/mixed.md src/x.clj && git commit -q -m "mixed: doc and code"
row mixed-commit-counts OK "2 commit(s) behind HEAD"

# A deletion inside the lane is records-lane content.
git reset -q --hard "$RECEIPT"
printf 'to be deleted\n' > docs/observations/doomed.md
git add docs/observations/doomed.md && git commit -q -m "records: add"
git rm -q docs/observations/doomed.md && git commit -q -m "records: delete"
row records-delete-is-exempt OK "1 commit(s) behind HEAD"

# A commit touching docs/intent/ is NOT records-lane content: intent is a proof input.
git reset -q --hard "$RECEIPT"
mkdir -p docs/intent; printf 'spec\n' > docs/intent/spec.md
git add docs/intent/spec.md && git commit -q -m "intent: a spec change"
row docs-intent-counts OK "2 commit(s) behind HEAD"

echo "battery-fresh records-lane witness: $ROWS rows, mismatches: $MIS"
[ "$MIS" = 0 ] || exit 1
