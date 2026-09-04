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
#   makeunknown a Make target the attest-time map does not resolve (item 6)
#   rotate    a bound rollout whose file is REPLACED by a new inode mid-run (item 2)
#   setsidhang a descendant that leaves the driver's process group via setsid (item 5)
#   ceiling   a session id announced AFTER the 64 KiB banner scan ceiling (diagnostic)
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

  makeunknown)
    # A Make target the attest-time map does not resolve.  Sol, item 6: an unknown or
    # conditional target failed OPEN as a non-test action, so an unmetered test run
    # counted as one more non-test action -- the exact quantity E3's pass line uses.
    ret '"Running an unmapped target."'
    call '"shell"' '"{\"command\":[\"bash\",\"-lc\",\"make ghost\"]}"' '"c1"'
    out '"c1"' '"42 tests, 416 assertions, 0 failures."'
    ret '"Done.\n\nTOOLCALLS: 1"'
    printf 'TOOLCALLS: 1\n' > "$A/driver-report.md"
    ;;

  rotate)
    # The bound rollout is REPLACED by a different inode mid-run.  Sol, item 2: the
    # watcher kept reading the old (unlinked) inode while the retained copy was taken
    # BY PATH from the replacement, and the receipt asserted sources.agree=true over
    # two different files.
    SID=${FAKE_SESSION_ID:-22222222-3333-4444-5555-666666666666}
    : "${CODEX_HOME:?rotate fixture requires CODEX_HOME}"
    D="$CODEX_HOME/sessions/2026/09/03"
    mkdir -p "$D"
    echo "OpenAI Codex (research preview)"
    echo "session id: $SID"
    R="$D/rollout-2026-09-03T06-00-00-$SID.jsonl"
    : > "$R"
    ret '"Reading the tree."'
    call '"clj-surgeon__alias_migration"' '"{\"expect_files\":21}"' '"c1"'
    out '"c1"' '"{\"files_changed\":21}"'
    ret '"Done.\n\nTOOLCALLS: 1"'
    sleep 3                        # the watcher binds THIS inode and reads THESE bytes
    rm -f "$R"                     # rotation: the path now names a different inode
    : > "$R"
    ret '"a replacement file nobody metered"'
    call '"shell"' '"{\"command\":[\"bash\",\"-lc\",\"echo replaced\"]}"' '"r1"'
    out '"r1"' '"replaced"'
    sleep 3
    printf 'TOOLCALLS: 1\n' > "$A/driver-report.md"
    ;;

  setsidhang)
    # A descendant that leaves the driver's process group by calling setsid.  Sol,
    # item 5: the PGID reap could not see it, it survived the abort, and run.json
    # still reported zero orphans.
    call '"shell"' '"{\"command\":[\"bash\",\"-lc\",\"setsid sleep 60\"]}"' '"c1"'
    setsid bash -c "printf '%s\n' \$\$ > \"$A/fake-driver-setsid.pid\"; exec sleep 60" &
    printf '%s\n' "$$" > "$A/fake-driver.pid"
    sleep 60 &
    child=$!
    printf '%s\n' "$child" > "$A/fake-driver-child.pid"
    wait "$child"
    ;;

  ceiling)
    # The session id announced AFTER the 64 KiB banner scan ceiling.  Binding fails
    # closed, correctly -- but the diagnostic must say WHY.  "no ID announced" is a
    # false statement about a driver that announced one.
    SID=${FAKE_SESSION_ID:-33333333-4444-5555-6666-777777777777}
    : "${CODEX_HOME:?ceiling fixture requires CODEX_HOME}"
    D="$CODEX_HOME/sessions/2026/09/03"
    mkdir -p "$D"
    head -c 70000 /dev/zero | tr '\0' 'x'
    echo
    echo "session id: $SID"
    R="$D/rollout-2026-09-03T07-00-00-$SID.jsonl"
    : > "$R"
    ret '"Reading the tree."'
    ret '"Done.\n\nTOOLCALLS: 0"'
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
