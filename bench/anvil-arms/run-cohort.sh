#!/usr/bin/env bash
# run-cohort.sh — one cohort: n slots per arm, SERIAL, in MIRRORED order.
#
# Mirrored order is the z7c correction, and it is not decoration: z7b's 0.76x was
# withdrawn precisely because its native arm happened to run slow.  For arms A and B
# at n=3 the order is
#
#     A-1  B-1  B-2  A-2  A-3  B-3
#
# i.e. odd slots lead with arm 1, even slots lead with arm 2.  Slots run one at a
# time, because contended walls are not comparable to sequential ones.
#
#   run-cohort.sh --root DIR --exp e3 --rung P --arms N,T --n 3 \
#                 --prompt-dir DIR --prompt-prefix E3-P \
#                 --ports N=-,T=7907 --expected-server-sha SHA [run-arm options...]
#
#   --dry-run   print the mirrored order and each arm's plan; run nothing
#
# Every unrecognised argument is passed through to run-arm.sh unchanged.
set -uo pipefail

HERE=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)

ROOT=""; EXP=""; RUNG=""; ARMS=""; N=3; PROMPT_DIR=""; PROMPT_PREFIX=""
PORTS=""; DRY=0; PASSTHROUGH=()

while [ $# -gt 0 ]; do
  case "$1" in
    --root) ROOT=$2; shift 2;;
    --exp) EXP=$2; shift 2;;
    --rung) RUNG=$2; shift 2;;
    --arms) ARMS=$2; shift 2;;
    --n) N=$2; shift 2;;
    --prompt-dir) PROMPT_DIR=$2; shift 2;;
    --prompt-prefix) PROMPT_PREFIX=$2; shift 2;;
    --ports) PORTS=$2; shift 2;;
    --dry-run) DRY=1; shift;;
    *) PASSTHROUGH+=("$1"); shift;;
  esac
done

for required in ROOT EXP RUNG ARMS PROMPT_DIR PROMPT_PREFIX; do
  [ -n "${!required}" ] || { echo "run-cohort: --${required,,} is required" >&2; exit 64; }
done

# A cohort of zero arms is not a cohort.  Sol, item 10: n=0 exited successfully with
# an empty run directory -- a green result over zero evidence.
case "$N" in
  ''|*[!0-9]*) echo "run-cohort: --n must be a positive integer (got '$N')" >&2; exit 64;;
esac
[ "$N" -ge 1 ] || { echo "run-cohort: --n must be >= 1 (got $N); a cohort of zero arms is not a cohort" >&2; exit 64; }

IFS=',' read -r ARM1 ARM2 <<<"$ARMS"
[ -n "${ARM2:-}" ] || { echo "run-cohort: --arms needs exactly two arms, e.g. N,T" >&2; exit 64; }

port_for () {
  local arm=$1 entry
  IFS=',' read -ra entries <<<"$PORTS"
  for entry in "${entries[@]}"; do
    case "$entry" in "$arm="*) printf '%s' "${entry#*=}"; return 0;; esac
  done
  printf '%s' "-"
}

# --- the mirrored order -----------------------------------------------------------
ORDER=()
for slot in $(seq 1 "$N"); do
  if [ $((slot % 2)) -eq 1 ]; then
    ORDER+=("$ARM1-$slot" "$ARM2-$slot")
  else
    ORDER+=("$ARM2-$slot" "$ARM1-$slot")
  fi
done

echo "run-cohort: $EXP rung $RUNG arms $ARM1/$ARM2 n=$N mirrored order: ${ORDER[*]}"
if [ $DRY -eq 1 ]; then
  printf 'ORDER %s\n' "${ORDER[*]}"
fi

rc_total=0
ABORT_STEP=""; ABORT_REASON=""; RAN=0
for step in "${ORDER[@]}"; do
  arm=${step%-*}
  slot=${step#*-}
  prompt="$PROMPT_DIR/$PROMPT_PREFIX-$arm.md"
  if [ ! -s "$prompt" ]; then
    echo "run-cohort: missing prompt $prompt" >&2
    exit 2
  fi
  args=(--root "$ROOT" --exp "$EXP" --rung "$RUNG" --arm "$arm" --slot "$slot"
        --prompt "$prompt" --port "$(port_for "$arm")")
  [ $DRY -eq 1 ] && args+=(--dry-run)
  bash "$HERE/run-arm.sh" "${args[@]}" "${PASSTHROUGH[@]}"
  rc=$?
  RAN=$((RAN+1))
  if [ $rc -ne 0 ]; then
    # STOP.  The instrument has refused once; every later arm in this cohort would be
    # spent producing evidence the cohort cannot cite.
    ABORT_REASON=arm-failed
    [ -s "$ROOT/$EXP-$RUNG-$arm-$slot/ATTEST-MISMATCH" ] && ABORT_REASON=attest-mismatch
    ABORT_STEP=$step
    rc_total=$rc
    echo "COHORT-ABORT $ABORT_REASON step=$step rc=$rc ran=$RAN of ${#ORDER[@]} — no further arm is launched" >&2
    break
  fi
done

# --- collect the receipts flat, mirroring ~/acid/receipts/<cohort>-score.md --------
if [ $DRY -eq 0 ]; then
  mkdir -p "$ROOT/receipts"
  out="$ROOT/receipts/$EXP-$RUNG-score.md"
  {
    printf '# %s rung %s — %s vs %s, n=%s (mirrored, serial)\n\n' \
      "$EXP" "$RUNG" "$ARM1" "$ARM2" "$N"
    if [ -n "$ABORT_STEP" ]; then
      printf '**COHORT-ABORT %s at %s** — %s of %s arm-runs were launched; the rows below\n' \
        "$ABORT_REASON" "$ABORT_STEP" "$RAN" "${#ORDER[@]}"
      printf 'marked NO-RECEIPT never ran. This cohort is :unverified, not a result.\n\n'
    fi
    printf '| exp | rung | arm | slot | wall s | returns | non-test actions | write calls via verb | native .clj patches | churn +/- | refusals | gate |\n'
    printf '|---|---|---|---|---|---|---|---|---|---|---|---|\n'
    for step in "${ORDER[@]}"; do
      arm=${step%-*}; slot=${step#*-}
      md="$ROOT/$EXP-$RUNG-$arm-$slot/receipt.md"
      if [ -s "$md" ]; then
        sed -n '3p' "$md"
      else
        printf '| %s | %s | %s | %s | NO-RECEIPT | | | | | | | |\n' "$EXP" "$RUNG" "$arm" "$slot"
      fi
    done
  } > "$out"
  echo "run-cohort: wrote $out"
fi

exit $rc_total
