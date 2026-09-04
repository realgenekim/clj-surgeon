#!/usr/bin/env bash
# feature-thread.sh <repo> <seed> [seed...] — the five-slot cross-language feature-thread receipt.
#
# Answers the sixth question that five greps cannot: IS THIS THE WHOLE THREAD?
# Five named slots are ALWAYS rendered. Each is FOUND (with evidence kind and
# location) or ABSENT (with the exact search that was run and returned zero).
# status is COMPLETE only when all five are FOUND; a four-of-five thread is
# INCOMPLETE and names the missing slot. It never silently renders 4 as "the thread".
#
# bash + rg only. No parser, no server, no MCP. Slot roles are repo conventions
# and live in a per-repo config file (see --config), never hard-coded here.
#
# Origin: docs/observations/2026-09-04-feature-thread-study.md §4/§5 (the receipt
# shape and "the cheaper thing"). Instrument for cohort E-THREAD.
#
# Usage:
#   feature-thread.sh <repo-path> <seed> [more seeds...]
#   feature-thread.sh --config path/to/repo.conf <repo-path> <seed> [...]
#
# A seed beginning with "/" is treated as a ROUTE; anything else is an IDENTIFIER.
# Both kinds may be given. Default config: <script-dir>/<basename repo>.conf

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG=""

while [ $# -gt 0 ]; do
  case "$1" in
    --config) CONFIG="$2"; shift 2 ;;
    -h|--help) sed -n '2,25p' "${BASH_SOURCE[0]}"; exit 0 ;;
    *) break ;;
  esac
done

REPO="${1:-}"; shift || true
if [ -z "$REPO" ] || [ $# -eq 0 ]; then
  echo "usage: feature-thread.sh [--config FILE] <repo-path> <seed> [seed...]" >&2
  exit 2
fi
[ -d "$REPO" ] || { echo "feature-thread: no such repo: $REPO" >&2; exit 2; }
REPO="$(cd "$REPO" && pwd)"

if [ -z "$CONFIG" ]; then
  CONFIG="$SCRIPT_DIR/$(basename "$REPO").conf"
fi
if [ ! -f "$CONFIG" ]; then
  echo "feature-thread: no config for this repo: $CONFIG" >&2
  echo "  a repo's slot roles are its own conventions; write a <repo>.conf beside this script." >&2
  exit 2
fi

command -v rg >/dev/null || { echo "feature-thread: rg (ripgrep) is required" >&2; exit 2; }

# ---- config contract -------------------------------------------------------
# REPO_LABEL      human name
# EXCLUDE         rg -g exclusions applied to every search
# SLOT_IDS        five slot ids, space separated, in receipt order
# For each slot id S (with '-' mapped to '_' in variable names):
#   GLOBS_S       rg -g include globs for the files that carry that role
#   KIND_S        use | def | route | handler | test
REPO_LABEL=""; EXCLUDE=""; SLOT_IDS=""
# shellcheck disable=SC1090
. "$CONFIG"
[ -n "$SLOT_IDS" ] || { echo "feature-thread: config declares no SLOT_IDS: $CONFIG" >&2; exit 2; }

# ---- seeds -----------------------------------------------------------------
IDENTS=(); ROUTES=()
for s in "$@"; do
  case "$s" in
    /*) ROUTES+=("$s") ;;
    *)  IDENTS+=("$s") ;;
  esac
done

esc() { printf '%s' "$1" | sed -e 's/[][\.^$*+?(){}|\/]/\\&/g'; }

# alternation of escaped identifiers, empty string if none
ident_alt=""
for i in "${IDENTS[@]:-}"; do [ -n "$i" ] || continue; ident_alt="${ident_alt:+$ident_alt|}$(esc "$i")"; done
route_alt=""
for r in "${ROUTES[@]:-}"; do [ -n "$r" ] || continue; route_alt="${route_alt:+$route_alt|}$(esc "$r")"; done
# route tail: last two segments of each route, so a route assembled from a
# constant or a template still has a searchable tail. Named evidence, not a guess.
tail_alt=""
for r in "${ROUTES[@]:-}"; do
  [ -n "$r" ] || continue
  t="$(printf '%s' "$r" | awk -F/ '{n=NF; if(n>=2) printf "%s/%s", $(n-1), $n; else printf "%s", $n}')"
  tail_alt="${tail_alt:+$tail_alt|}$(esc "$t")"
done

# ---- search primitive ------------------------------------------------------
# run_rg <globs> <regex>   -> prints "file:line:text" hits.
# show_query <globs> <regex> -> prints the exact command, so a zero-hit slot can
# quote the search it actually ran. (These are two functions, not one setting a
# global: run_rg is always called in a command substitution, and a subshell
# cannot hand a variable back. That defect printed "searched:  — 0 hits" on the
# first hand-drive, which is precisely the silent-absence the receipt exists to
# prevent.)
run_rg() {
  local globs="$1" regex="$2"
  local -a args=(--no-heading -n --color never -e "$regex")
  local g
  # shellcheck disable=SC2086
  for g in $globs;   do args+=(-g "$g"); done
  for g in $EXCLUDE; do args+=(-g "$g"); done
  ( cd "$REPO" && rg "${args[@]}" . 2>/dev/null ) | sed 's#^\./##'
}
show_query() {
  local globs="$1" regex="$2" g out
  out="rg -n -e '$regex'"
  # shellcheck disable=SC2086
  for g in $globs; do out="$out -g '$g'"; done
  printf '%s' "$out"
}

# ---- slot patterns by kind -------------------------------------------------
# Each kind builds ONE regex from the seeds. Evidence kind is named in the
# receipt so a reader knows what the hit actually proves.
pattern_for() {
  local kind="$1"
  case "$kind" in
    use)
      # any occurrence of a seed identifier or the literal route
      printf '%s' "${ident_alt}${route_alt:+${ident_alt:+|}$route_alt}"
      ;;
    def)
      # definition-shaped occurrence in a script language, plus bare occurrence
      local p=""
      for i in "${IDENTS[@]:-}"; do
        [ -n "$i" ] || continue
        local e; e="$(esc "$i")"
        p="${p:+$p|}(async +)?function +$e\b|(const|let|var|window\.[A-Za-z_.]*) *$e *=|\b$e *[:=] *(async *)?(function|\()|defn-? +$e\b"
      done
      printf '%s' "$p"
      ;;
    route)
      # exact route literal, or its tail segments (catches a route assembled
      # from a constant or a template string). Tail hits are weaker evidence
      # and the receipt says so.
      printf '%s' "${route_alt}${tail_alt:+${route_alt:+|}$tail_alt}"
      ;;
    route-assembled)
      # LAST RESORT for a route the literal search missed: the route's segments
      # appearing as adjacent quoted strings, i.e. a path built by (str ...) or
      # a helper. Weak evidence, always labelled as such, never silently merged
      # with a literal hit.
      local p="" r
      for r in "${ROUTES[@]:-}"; do
        [ -n "$r" ] || continue
        local segs; segs="$(printf '%s' "$r" | tr '/' '\n' | grep -v '^$' | tail -2 | sed 's/^/"/; s/$/"/' | paste -sd' ' -)"
        [ -n "$segs" ] && p="${p:+$p|}$(esc "$segs")"
      done
      printf '%s' "$p"
      ;;
    handler)
      # filled in later from the route hit (the join); falls back to seeds
      printf '%s' "${ident_alt}${route_alt:+${ident_alt:+|}$route_alt}"
      ;;
    test)
      printf '%s' "${ident_alt}${route_alt:+${ident_alt:+|}$route_alt}${tail_alt:+|$tail_alt}"
      ;;
    *) printf '%s' "$ident_alt" ;;
  esac
}

# ---- pass 1: the route slot, so the handler slot can join on it -------------
DERIVED_HANDLER=""
DERIVED_FROM=""
for sid in $SLOT_IDS; do
  v="${sid//-/_}"
  eval "kind=\${KIND_$v:-use}"
  [ "$kind" = "route" ] || continue
  eval "globs=\${GLOBS_$v:-}"
  hits="$(run_rg "$globs" "$(pattern_for route)")"
  # a route the literal search cannot see must still be joinable to its handler,
  # or a templated route silently costs BOTH legs instead of one
  if [ -z "$hits" ]; then
    ar="$(pattern_for route-assembled)"
    [ -n "$ar" ] && hits="$(run_rg "$globs" "$ar")"
  fi
  if [ -n "$hits" ]; then
    # pull a var-quoted handler out of a reitit-style route line: #'ns/handle-x
    qvar="$(printf '%s\n' "$hits" | grep -oE "#'[A-Za-z0-9_.<>*+!?-]+/[A-Za-z0-9_.<>*+!?-]+" | head -1 | sed "s/^#'//")"
    DERIVED_HANDLER="${qvar##*/}"
    # the alias half of the var names the namespace; keep it so the handler slot
    # can prefer the file that namespace lives in when several namespaces define
    # a fn of the same name (handle-events exists in three, in marvin-voice-remote)
    DERIVED_NS="${qvar%/*}"
    [ -n "$DERIVED_HANDLER" ] && DERIVED_FROM="$(printf '%s\n' "$hits" | head -1 | cut -d: -f1,2)"
  fi
done

# ---- render ----------------------------------------------------------------
subject_line=""
[ ${#IDENTS[@]} -gt 0 ] && subject_line="subject=${IDENTS[0]}"
extra=("${IDENTS[@]:1}" "${ROUTES[@]:-}")
extra_s=""
for e in "${extra[@]:-}"; do [ -n "$e" ] || continue; extra_s="${extra_s:+$extra_s, }$e"; done
[ -z "$subject_line" ] && subject_line="subject=${ROUTES[0]}"

echo "feature-thread  repo=${REPO_LABEL:-$(basename "$REPO")}  $subject_line${extra_s:+  (also matched: $extra_s)}"

found_n=0; slot_n=0; missing=""
for sid in $SLOT_IDS; do
  slot_n=$((slot_n+1))
  v="${sid//-/_}"
  eval "kind=\${KIND_$v:-use}"
  eval "globs=\${GLOBS_$v:-}"

  regex="$(pattern_for "$kind")"
  evidence="identifier"
  case "$kind" in
    def)     evidence="identifier(def)" ;;
    route)   evidence="route-literal" ;;
    handler) evidence="form" ;;
    test)    evidence="identifier" ;;
  esac

  # The handler slot is the JOIN: search the name the route table actually names
  # FIRST and alone. Only if that misses do we fall back to the seed regex, whose
  # hits are every mention of the subject in src/ and would otherwise bury the
  # one form that answers the question.
  hits=""
  if [ "$kind" = "handler" ] && [ -n "$DERIVED_HANDLER" ]; then
    jregex="\(defn-? +$(esc "$DERIVED_HANDLER")\b"
    hits="$(run_rg "$globs" "$jregex")"
    if [ -n "$hits" ]; then
      regex="$jregex"
      evidence="form(joined from route $DERIVED_FROM -> ${DERIVED_NS:+$DERIVED_NS/}$DERIVED_HANDLER)"
      # several namespaces may define this fn name; the route named ONE. Prefer
      # the file whose path matches the route's namespace alias, and say when the
      # remaining hits are same-name forms in other namespaces, not extra legs.
      if [ -n "${DERIVED_NS:-}" ]; then
        nspat="$(printf '%s' "${DERIVED_NS##*.}" | tr '.-' '__')"
        pref="$(printf '%s\n' "$hits" | grep -E "/${nspat}\.clj:|/${nspat}s\.clj:" | head -1)"
        [ -n "$pref" ] && hits="$pref
$(printf '%s\n' "$hits" | grep -vF "$pref")"
      fi
    fi
  fi

  if [ -z "$regex" ]; then
    printf '  %-14s ABSENT  no seed of the kind this slot needs (searched: nothing)\n' "$sid"
    missing="${missing:+$missing, }$sid"
    continue
  fi

  [ -n "$hits" ] || hits="$(run_rg "$globs" "$regex")"
  q="$(show_query "$globs" "$regex")"
  n="$(printf '%s' "$hits" | grep -c . )"

  # --- one-hop alias follow -------------------------------------------------
  # `const formatDraft = runDraftFormatter;` is definition-SHAPED but it is not
  # the leg: the implementation is somewhere else, under another name. Reporting
  # the alias line as the JS function is a four-of-five thread rendered as five,
  # which is the exact failure this receipt exists to prevent. So: detect the
  # alias, follow it ONE hop, and label the evidence either way.
  if [ "$kind" = "def" ] && [ "$n" -gt 0 ]; then
    alias_line="$(printf '%s\n' "$hits" | grep -m1 -E "(const|let|var) +($ident_alt) *= *[A-Za-z_$][A-Za-z0-9_$]* *;")" || true
    real_def="$(printf '%s\n' "$hits" | grep -m1 -E "(async +)?function +($ident_alt)\b|($ident_alt) *[:=] *(async *)?(function|\()" )" || true
    if [ -n "$alias_line" ] && [ -z "$real_def" ]; then
      target="$(printf '%s' "$alias_line" | sed -E "s/.*(const|let|var) +($ident_alt) *= *([A-Za-z_$][A-Za-z0-9_$]*) *;.*/\3/")"
      aloc="$(printf '%s' "$alias_line" | cut -d: -f1,2)"
      tre="(async +)?function +$(esc "$target")\b|(const|let|var) +$(esc "$target") *= *(async *)?(function|\()"
      thits="$(run_rg "$globs" "$tre")"
      if [ -n "$thits" ]; then
        hits="$thits"
        n="$(printf '%s' "$hits" | grep -c . )"
        evidence="identifier(def, one hop: alias at $aloc -> $target)"
        q="$(show_query "$globs" "$tre")"
      else
        hits=""; n=0
        evidence="alias-only"
        q="$(show_query "$globs" "$tre") [after following the alias at $aloc -> $target]"
      fi
    fi
  fi

  # --- assembled-route fallback ---------------------------------------------
  # A route the literal search cannot see is a hidden leg, not an absent one, if
  # the segments appear as adjacent strings. Try that ONCE and label it weakly;
  # if it also misses, the slot stays ABSENT with both searches quoted.
  if [ "$kind" = "route" ] && [ "$n" -eq 0 ]; then
    aregex="$(pattern_for route-assembled)"
    if [ -n "$aregex" ]; then
      ahits="$(run_rg "$globs" "$aregex")"
      if [ -n "$ahits" ]; then
        hits="$ahits"
        n="$(printf '%s' "$hits" | grep -c . )"
        evidence="route-assembled(segments as adjacent strings; the literal route never appears)"
      else
        q="$q; and $(show_query "$globs" "$aregex")"
      fi
    fi
  fi

  if [ "$n" -gt 0 ]; then
    found_n=$((found_n+1))
    loc="$(printf '%s\n' "$hits" | head -1 | cut -d: -f1,2)"
    # a route slot answered only by a tail match is weaker evidence: say so
    if [ "$kind" = "route" ] && [ -n "$route_alt" ] && [ "$evidence" = "route-literal" ]; then
      if ! printf '%s\n' "$hits" | grep -qE "$route_alt"; then
        evidence="route-tail(literal route never appears; assembled or templated)"
      fi
    fi
    printf '  %-14s FOUND   %-46s evidence=%s%s\n' "$sid" "$loc" "$evidence" \
      "$([ "$n" -gt 1 ] && printf '  (+%d more)' "$((n-1))")"
    printf '%s\n' "$hits" | tail -n +2 | head -4 | while IFS= read -r h; do
      [ -n "$h" ] || continue
      printf '  %-14s         %s\n' "" "$(printf '%s' "$h" | cut -d: -f1,2)"
    done
  else
    printf '  %-14s ABSENT  searched: %s — 0 hits\n' "$sid" "$q"
    missing="${missing:+$missing, }$sid"
  fi
done

if [ "$found_n" -eq "$slot_n" ]; then
  printf '  %-14s COMPLETE (%d of %d)\n' "status" "$found_n" "$slot_n"
else
  printf '  %-14s INCOMPLETE (%d of %d)   missing: %s\n' "status" "$found_n" "$slot_n" "$missing"
  printf '  %-14s name the missing leg by hand, or confirm it does not exist. A thread\n' ""
  printf '  %-14s reported without it is NOT the whole thread.\n' ""
fi
exit 0
