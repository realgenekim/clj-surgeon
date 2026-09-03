#!/usr/bin/env bash
# fake-driver.sh — the PF-5 smoke driver.  It is NOT a model: it emits a synthetic
# codex-shaped rollout.jsonl, line by line, so the whole attest -> watch -> score
# chain can be proved end to end in seconds with no network, no MCP server, and no
# arm-run budget.  A cohort without a proved meter is a rumour (A.10).
#
#   fake-driver.sh <ARMDIR> <fixture>
#
# fixtures:
#   pf5   3 model returns, 2 tool calls, one of them a test call  (the PF-5 shape)
#   rich  adds an MCP verb call, a typed refusal carrying next_call, a native
#         apply_patch landing .clj bytes, and an inspect_clojure ls-tree at return 2
#   codexsession a codex-shaped session under a private CODEX_HOME, plus a DECOY
#           rollout from another session with a newer mtime
#   partial   a call whose result never arrives before the driver exits
#   makeverify a kaocha run reached through `make verify` (a non-test-named target)
#   zero  a rollout with tool calls and NO assistant return (the abort path)
#   hang  no returns, then sleeps, so the zero-return WINDOW fires rather than EOF
set -euo pipefail

A=${1:?usage: fake-driver.sh <ARMDIR> <fixture>}
FIXTURE=${2:-pf5}
R="$A/rollout.jsonl"
: > "$R"

now () { date -u +%Y-%m-%dT%H:%M:%SZ; }
line () { printf '%s\n' "$1" >> "$R"; sleep 0.05; }

ret () { line "{\"timestamp\":\"$(now)\",\"type\":\"response_item\",\"payload\":{\"type\":\"message\",\"role\":\"assistant\",\"content\":[{\"type\":\"output_text\",\"text\":$1}]}}"; }
call () { line "{\"timestamp\":\"$(now)\",\"type\":\"response_item\",\"payload\":{\"type\":\"function_call\",\"name\":$1,\"arguments\":$2,\"call_id\":$3}}"; }
out () { line "{\"timestamp\":\"$(now)\",\"type\":\"response_item\",\"payload\":{\"type\":\"function_call_output\",\"call_id\":$1,\"output\":$2}}"; }

case "$FIXTURE" in
  pf5)
    ret '"Reading the tree."'
    call '"shell"' '"{\"command\":[\"bash\",\"-lc\",\"rg -n \\\"System/currentTimeMillis\\\" src/\"]}"' '"c1"'
    out '"c1"' '"src/a.clj:12"'
    ret '"Running the focused suite."'
    call '"shell"' '"{\"command\":[\"bash\",\"-lc\",\"bin/kaocha --focus marvin-voice-remote.bridge3-new-test\"]}"' '"c2"'
    out '"c2"' '"42 tests, 416 assertions, 0 failures."'
    ret '"Done.\n\nTOOLCALLS: 2"'
    printf 'TOOLCALLS: 2\n' > "$A/driver-report.md"
    ;;

  rich)
    ret '"Getting a table of contents first."'
    call '"clj-surgeon__inspect_clojure"' '"{\"mode\":\"ls-tree\",\"path\":\"src\"}"' '"c1"'
    out '"c1"' '"{\"namespaces\":21,\"read_complete\":true}"'
    ret '"Routing the write through the verb."'
    call '"clj-surgeon__alias_migration"' '"{\"from\":{\"lib\":\"acid.fanout.store\",\"var\":\"find-event\"},\"expect_files\":21}"' '"c2"'
    out '"c2"' '"{\"error\":{\"type\":\"alias-migration-expect-mismatch\"},\"next_call\":{\"tool\":\"alias_migration\",\"arguments\":{\"expect_files\":20}}}"'
    ret '"Resending the next_call it handed back."'
    call '"clj-surgeon__alias_migration"' '"{\"from\":{\"lib\":\"acid.fanout.store\",\"var\":\"find-event\"},\"expect_files\":20}"' '"c3"'
    out '"c3"' '"{\"files_changed\":20,\"sites\":60}"'
    ret '"One site the verb does not own."'
    call '"shell"' '"{\"command\":[\"bash\",\"-lc\",\"apply_patch <<PATCH\\n*** Begin Patch\\n*** Update File: src/fake/sample.clj\\n@@\\n+;; touched natively\\n*** End Patch\\nPATCH\"]}"' '"c4"'
    out '"c4"' '"Done"'
    printf ';; native line 1\n;; native line 2\n;; native line 3\n' >> "$A/worktree/src/fake/sample.clj"
    ret '"Running the focused suite."'
    call '"shell"' '"{\"command\":[\"bash\",\"-lc\",\"bin/kaocha --focus marvin-voice-remote.bridge3-new-test\"]}"' '"c5"'
    out '"c5"' '"42 tests, 416 assertions, 0 failures."'
    ret '"Done.\n\nTOOLCALLS: 5"'
    printf 'TOOLCALLS: 5\n' > "$A/driver-report.md"
    ;;

  partial)
    # A return, a completed call, then a call whose RESULT NEVER ARRIVES before the
    # driver exits.  Sol, item 3: this flushed as `no-output` with rc 0 and stayed a
    # citeable receipt instead of an incomplete-run refusal.
    ret '"Reading the tree."'
    call '"shell"' '"{\"command\":[\"bash\",\"-lc\",\"ls src/\"]}"' '"c1"'
    out '"c1"' '"fake"'
    ret '"Landing the edit."'
    call '"shell"' '"{\"command\":[\"bash\",\"-lc\",\"apply_patch < p.diff\"]}"' '"c2"'
    printf 'TOOLCALLS: 2\n' > "$A/driver-report.md"
    ;;

  makeverify)
    # A test runner reached through a Make target whose NAME does not say "test".
    # Sol, item 4: `make verify` metered as test_call=false, so a whole kaocha run
    # counted as a non-test action.
    ret '"Running the project verify target."'
    call '"shell"' '"{\"command\":[\"bash\",\"-lc\",\"make verify\"]}"' '"c1"'
    out '"c1"' '"42 tests, 416 assertions, 0 failures."'
    ret '"Done.\n\nTOOLCALLS: 1"'
    printf 'TOOLCALLS: 1\n' > "$A/driver-report.md"
    ;;

  codexsession)
    # A codex-shaped session: it announces its own session id, writes ITS rollout under
    # its private CODEX_HOME, and a DECOY rollout from another session lands in the same
    # home afterwards, so the decoy always has the newer mtime.  Sol, item 8 (blocker):
    # newest-mtime discovery selected the decoy and then latched it permanently.
    SID=${FAKE_SESSION_ID:-11111111-2222-3333-4444-555555555555}
    DECOY_SID=99999999-8888-7777-6666-555555555555
    : "${CODEX_HOME:?codexsession fixture requires CODEX_HOME}"
    D="$CODEX_HOME/sessions/2026/09/03"
    mkdir -p "$D"
    echo "OpenAI Codex (research preview)"
    echo "session id: $SID"
    echo "workdir: $A/worktree"
    R="$D/rollout-2026-09-03T05-00-00-$SID.jsonl"
    : > "$R"
    ret '"Reading the tree."'
    call '"shell"' '"{\"command\":[\"bash\",\"-lc\",\"rg -n pattern src/\"]}"' '"c1"'
    out '"c1"' '"src/a.clj:12"'
    ret '"Running the focused suite."'
    call '"shell"' '"{\"command\":[\"bash\",\"-lc\",\"bin/kaocha --focus x\"]}"' '"c2"'
    out '"c2"' '"42 tests, 416 assertions, 0 failures."'
    ret '"Done.\n\nTOOLCALLS: 2"'
    # the decoy, written LAST so its mtime is newest
    DEC="$D/rollout-2026-09-03T05-00-01-$DECOY_SID.jsonl"
    R="$DEC"; : > "$R"
    for _ in 1 2 3 4 5 6 7 8 9; do ret '"a concurrent session nobody asked this arm to meter"'; done
    printf 'TOOLCALLS: 2\n' > "$A/driver-report.md"
    ;;

  zero)
    call '"shell"' '"{\"command\":[\"bash\",\"-lc\",\"ls\"]}"' '"c1"'
    out '"c1"' '"src"'
    ;;

  hang)
    # No returns, then a long child.  The child's pid is RECORDED so the self-test can
    # prove by pid -- not by name -- that aborting the watcher reaped it.  Sol, item 5:
    # the watcher signalled only the driver's own pid, and this `sleep` survived under
    # PPID 1 until it was cleaned up by hand.
    call '"shell"' '"{\"command\":[\"bash\",\"-lc\",\"sleep 60\"]}"' '"c1"'
    sleep 60 &
    child=$!
    printf '%s\n' "$$" > "$A/fake-driver.pid"
    printf '%s\n' "$child" > "$A/fake-driver-child.pid"
    wait "$child"
    ;;

  *)
    echo "fake-driver: unknown fixture $FIXTURE" >&2
    exit 2
    ;;
esac
exit 0
