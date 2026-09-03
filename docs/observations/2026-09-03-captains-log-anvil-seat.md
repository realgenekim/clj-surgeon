# Captain's log — 2026-09-03, the seat on Anvil

## 2026-09-03T01:42Z — moved; oriented; cleared to build

The mayor moved this seat overnight (transcript 24 MB + 60 memory files; project keys rewritten) to
forge@anvil per Gene's order ("pick up session over there and have it just be like talking with you";
checklist inb-20c591). First action: the program resume note, then the Anvil seat note written and
pushed (f93cbbf), Memento pointers in both CLAUDE.md files (fcbb4a0, d170f3d), a seat-move memory.
Facts verified on the box: identity forge-anvil, gh as marvin-openclaw777 (`gh auth setup-git` was
needed once for https pushes), 16 cores, load ~1.1, ports 7888–7895 all held by other seats, `~/acid`
not readable by forge (good: the boundary is enforced by the filesystem, not by discipline). Missing:
the channel connector (no reply tool), maven creds, clj-nrepl-eval, `~/bin`, `/opt/claude-skills`, crons.
Reported as instructed; Gene: "Super!!! Let's go!!"

**First build here:** inb-a0f37e (names-only `ls-tree` rendering + `ns_grep`) and inb-d8a635 (`make
mcp-serve` honours MCP_PORT), re-run from origin `bridge/study-ops-mcp` b3c17bb in
`~/src/clj-surgeon-study` because the Buster builder's work never reached origin. Builder identity
forge-anvil; free port ≥ 7900 for the wire witness; one suite at a time under `~/tmp/suite.lock`.
