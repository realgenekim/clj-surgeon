#!/usr/bin/env bash
# measure_prefill_decode_ratio.sh — time how fast the model READS versus WRITES.
#
# WHY THIS EXISTS
#   docs/why-reading-is-cheap-and-writing-is-expensive.md rests the whole
#   "enrich input freely, shrink output hard" strategy on a prefill:decode ratio
#   that was REPEATED from published figures and never measured on our own
#   models and hardware. This probe measures it.
#
# THE THREE CONDITIONS (two conditions cannot separate the fixed per-turn floor)
#   A  large prompt, ~1 token output   -> wall ~= floor + prefill(large)
#   B  small prompt, large output      -> wall ~= floor + decode(large)
#   C  small prompt, ~1 token output   -> wall ~= floor
#
#   prefill_rate = marginal_input_tokens(A-C)  / (median A - median C)
#   decode_rate  = marginal_output_tokens(B-C) / (median B - median C)
#   ratio        = prefill_rate / decode_rate
#
# DOCTRINE: the probe emits FACTS, the fold emits VERDICTS.
#   This script writes verbatim codex JSONL, arrival timestamps, and environment
#   readings. It computes no rates and renders no judgement. Run the fold —
#   bench/score_prefill_decode_ratio.clj — over the output directory to get
#   medians, rates, spread, and the ratio. That split means a later question can
#   be re-asked of an old run without re-spending the calls.
#
# ROUTE: codex exec on the ChatGPT subscription (house rule: subscription before
#   metered API). No OPENAI_API_KEY is used or required.
#
# USAGE
#   bench/measure_prefill_decode_ratio.sh OUT_DIR
#
# KNOBS (env)
#   RATIO_MODEL        model id                              [gpt-5.6-sol]
#   RATIO_REASONING    reasoning effort                      [low]
#   RATIO_REPLICATES   replicates per condition              [9]
#   RATIO_CONDITIONS   subset to run, space separated        [A B C]
#   RATIO_BLOB_CHARS   condition-A filler size in chars      [1048000]
#   RATIO_COUNT_TO     condition-B "count to N" target       [600]
#   RATIO_PROFILE      clean | fleet                         [clean]
#                        clean = --ignore-user-config (no MCP servers, explicit
#                                model/reasoning) — isolates the model.
#                        fleet = the machine's real ~/.codex/config.toml,
#                                including MCP servers — measures the floor this
#                                project actually pays per turn.
#
# NOTES ON WHY THE PROMPTS LOOK LIKE THIS
#   * Condition A's filler is a FRESH random word sequence per replicate. A
#     repeated prompt would be served from the provider's prefix cache and would
#     measure cache lookup, not prefill.
#   * Condition A is sized near the observed hard ceiling (the API refuses input
#     over 1,048,576 characters). At smaller sizes the prefill cost is not
#     distinguishable from the floor at all — that is itself a finding, not a
#     failure, and the fold reports it as such.
#   * Condition B asks for an integer sequence because it is long, deterministic,
#     verifiable after the fact, and needs no tools.
#   * Replicates are INTERLEAVED (C, A, B, C, A, B, ...) so server-side load
#     drift lands on every condition equally instead of on whichever ran last.

set -euo pipefail

out_dir=${1:?usage: measure_prefill_decode_ratio.sh OUT_DIR}

model=${RATIO_MODEL:-gpt-5.6-sol}
reasoning=${RATIO_REASONING:-low}
replicates=${RATIO_REPLICATES:-9}
conditions=${RATIO_CONDITIONS:-A B C}
blob_chars=${RATIO_BLOB_CHARS:-1048000}
count_to=${RATIO_COUNT_TO:-600}
profile=${RATIO_PROFILE:-clean}

command -v codex >/dev/null || { echo "codex not on PATH" >&2; exit 2; }
command -v bb    >/dev/null || { echo "bb (babashka) not on PATH" >&2; exit 2; }

mkdir -p "$out_dir/trials"
work_dir=$(mktemp -d "${TMPDIR:-/var/tmp}/ratio-work.XXXXXX")
trap 'rm -rf "$work_dir"' EXIT

run_nonce=$(date +%s)-$$

# ---------------------------------------------------------------- environment
# Load 664 on a laptop invalidated a gate earlier in this project's history.
# Environment is evidence, not a footnote — it is recorded before and after.
# NOTE: loadavg is read with plain shell tools, not from a JVM. babashka's slurp
# raises "Invalid argument" on procfs files, which report size 0.
read_env () {
  local phase=$1
  printf '{"phase":"%s","wall_clock":"%s","loadavg":"%s","nproc":"%s","hostname":"%s","whoami":"%s","uname":"%s"}' \
    "$phase" \
    "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
    "$(cut -d' ' -f1-3 /proc/loadavg 2>/dev/null || uptime | sed 's/.*average[s]*: //' || echo unknown)" \
    "$( (nproc 2>/dev/null || sysctl -n hw.ncpu 2>/dev/null || echo unknown) )" \
    "$(hostname 2>/dev/null || echo unknown)" \
    "$(id -un 2>/dev/null || echo unknown)" \
    "$(uname -srm 2>/dev/null || echo unknown)"
}

# ------------------------------------------------------------ prompt payloads
# A fresh pseudo-random sequence of common English words. Common words are used
# rather than random characters so the token/character ratio resembles real
# prose and code rather than an adversarial tokenizer case.
make_blob () {
  local seed=$1 chars=$2 dest=$3
  bb -e "
(let [words (clojure.string/split
              \"the of and to in a is that for it as was with be by on not he this are or his from at which but have an they one had we all their there been if more when will would who so no said its about into them can only other new some could time these two may then do first any my now such like our over man me even most made after also did many before must through back years where much your way well down should because each just those people how too little state good very make world still own see men work long get here between both life being under never day same another know while last might us great old year off come since against go came right used take three\"
              #\"\\s+\")
      rng (java.util.Random. $seed)
      sb (StringBuilder.)]
  (while (< (.length sb) $chars)
    (.append sb (nth words (.nextInt rng (count words))))
    (.append sb \" \"))
  (spit \"$dest\" (subs (.toString sb) 0 $chars)))"
}

prompt_for () {
  local cond=$1 rep=$2 dest=$3
  case "$cond" in
    A)
      make_blob "$(( 1000 * rep + RANDOM ))" "$blob_chars" "$work_dir/blob.txt"
      { printf '%s' 'Filler text follows. Do not read it closely, do not analyze it, do not summarize it, do not use any tools. Reply with exactly one word: ok

'
        cat "$work_dir/blob.txt"; } > "$dest"
      ;;
    B)
      printf '%s' "Print the integers from 1 to ${count_to}, one per line, ascending, with no other text, no commentary, no code fences, and no tool use." > "$dest"
      ;;
    C)
      printf '%s' 'Do not use any tools. Reply with exactly one word: ok' > "$dest"
      ;;
    *) echo "unknown condition: $cond" >&2; exit 2 ;;
  esac
}

# --------------------------------------------------------------- codex invoke
codex_args=(exec --json --skip-git-repo-check --ephemeral
            -s read-only -C "$work_dir"
            -c model="$model" -c model_reasoning_effort="$reasoning")
if [ "$profile" = clean ]; then
  codex_args+=(--ignore-user-config)
fi

# --------------------------------------------------------------------- meta
{
  printf '{"schema":"clj-surgeon.prefill-decode-probe/v1"'
  printf ',"run_nonce":"%s"' "$run_nonce"
  printf ',"model":"%s","reasoning":"%s","profile":"%s"' "$model" "$reasoning" "$profile"
  printf ',"replicates":%s,"conditions":"%s"' "$replicates" "$conditions"
  printf ',"blob_chars":%s,"count_to":%s' "$blob_chars" "$count_to"
  printf ',"codex_version":"%s"' "$(codex --version 2>&1 | tr -d '"' | head -1)"
  printf ',"codex_args":"%s"' "${codex_args[*]}"
  printf ',"env_before":%s' "$(read_env before)"
  printf '}\n'
} > "$out_dir/meta.json"

echo "probe: model=$model reasoning=$reasoning profile=$profile replicates=$replicates conditions=[$conditions]" >&2
echo "probe: out=$out_dir" >&2

# ---------------------------------------------------------------- trial loop
# Interleaved rotation so drift is shared across conditions.
for rep in $(seq 1 "$replicates"); do
  for cond in $conditions; do
    tag=$(printf 'r%02d-%s' "$rep" "$cond")
    prompt_file="$work_dir/prompt.txt"
    prompt_for "$cond" "$rep" "$prompt_file"

    prompt_bytes=$(wc -c < "$prompt_file" | tr -d ' ')
    # Payload identity, so a replicate can be shown to have been unique (cache
    # defeat) without storing nine 1 MB blobs.
    prompt_sha=$( (sha256sum "$prompt_file" 2>/dev/null || shasum -a 256 "$prompt_file") | cut -c1-16 )

    start_ns=$(date +%s%N)
    # Each JSONL event is prefixed with its arrival time in nanoseconds. That
    # lets the fold separate local process startup from server-side turn time
    # without a second run.
    set +e
    codex "${codex_args[@]}" - < "$prompt_file" 2>"$out_dir/trials/$tag.stderr" \
      | while IFS= read -r line; do printf '%s\t%s\n' "$(date +%s%N)" "$line"; done \
      > "$out_dir/trials/$tag.jsonl"
    rc=${PIPESTATUS[0]}
    set -e
    end_ns=$(date +%s%N)

    {
      printf '{"tag":"%s","condition":"%s","replicate":%s' "$tag" "$cond" "$rep"
      printf ',"start_ns":%s,"end_ns":%s,"wall_ms":%s' \
        "$start_ns" "$end_ns" "$(( (end_ns - start_ns) / 1000000 ))"
      printf ',"exit_code":%s,"prompt_bytes":%s,"prompt_sha256_16":"%s"' \
        "$rc" "$prompt_bytes" "$prompt_sha"
      printf ',"loadavg":"%s"' "$(cut -d' ' -f1-3 /proc/loadavg 2>/dev/null || echo unknown)"
      printf '}\n'
    } > "$out_dir/trials/$tag.timing.json"

    printf 'trial %-8s rc=%s wall=%sms prompt=%sB\n' \
      "$tag" "$rc" "$(( (end_ns - start_ns) / 1000000 ))" "$prompt_bytes" >&2
  done
done

read_env after > "$out_dir/env_after.json"

# ------------------------------------------------------- network upload control
# Condition A ships ~1 MB and condition C ships ~1 KB, so (A - C) contains
# upload time as well as prefill. This control bounds the upload term: it POSTs
# the same payload size to the provider's edge with a deliberately invalid
# credential, so the body is uploaded and then rejected. No tokens are consumed
# and no model is invoked. Whatever this costs is time that (A - C) is charging
# to prefill but should not be — which is why the prefill rate is reported as a
# lower bound.
if [ "${RATIO_NETWORK_CONTROL:-1}" = 1 ] && command -v curl >/dev/null; then
  head -c "$blob_chars" /dev/zero | tr '\0' 'a' > "$work_dir/upload.bin"
  {
    printf '{"description":"POST of %s bytes to the provider edge, invalid credential, body uploaded then rejected","bytes":%s,"samples":[' \
      "$blob_chars" "$blob_chars"
    sep=""
    for _ in 1 2 3 4 5; do
      s=$(curl -s -o /dev/null -m 60 \
            -w '{"time_total":%{time_total},"time_starttransfer":%{time_starttransfer},"time_connect":%{time_connect},"speed_upload":%{speed_upload},"http_code":%{http_code}}' \
            -X POST https://api.openai.com/v1/chat/completions \
            -H 'Authorization: Bearer invalid-credential-upload-control' \
            -H 'Content-Type: application/json' \
            --data-binary "@$work_dir/upload.bin" 2>/dev/null || echo '{}')
      printf '%s%s' "$sep" "$s"; sep=","
    done
    printf ']}\n'
  } > "$out_dir/network_control.json"
  echo "network control written" >&2
fi

echo "probe: complete. fold with:" >&2
echo "  bb bench/score_prefill_decode_ratio.clj $out_dir" >&2
