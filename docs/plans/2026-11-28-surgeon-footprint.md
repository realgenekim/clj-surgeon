# Read-only machine footprint: parser and census slice

Piece 1 covers registrations and cron only. `bin/surgeon-footprint` reads six fixed MCP config paths
and the current user's crontab. System units and other users' crontabs are out of scope.
The config paths are `~/.codex/config.toml`, `~/.claude.json`, `~/.claude/settings.json`,
`~/.claude/mcp.json`, `./.mcp.json` and `./.codex/config.toml`.
TOML census parsing strips comments outside quotes, accepts quoted dotted server keys and joins args arrays;
this is a config-subset reader, not a validating TOML implementation. Inline tables produce unsupported entries
with physical line numbers. Triple-quote markers refuse the file with an unsupported error, even in unrelated
content; duplicate tables remain separate observations. Full multiline-string and inline-table parsing is deferred.
JSON root and project registrations retain project paths; a per-file parse error prints and the census continues.
Cron retains active lines naming the tools and includes disabled or active lines referencing readable scripts
naming clj-surgeon, cclsp or clojure-lsp or containing 7906, 7888, 7890, surgeon-call or /mcp.
Each matching script path appears alongside the original line. Only regular files are read, including symlinks
whose targets are regular files; FIFOs, devices and directories are skipped. Reads stop at 65,536 bytes.
Unreadable regular files are reported alongside the cron line; later content is outside the census.
Script paths may be literal quoted or unquoted tokens (absolute, relative or ~/); no shell expansion or execution.
Relative cron script paths resolve against $HOME; project config paths resolve against the census cwd.
No writes, signals or MCP service connections in census mode. Missing config files print absent; read failures
print unreadable. Crontab command failures (including no installed crontab) currently print unreadable as well.
Separating absent crontabs from permission/command failures is deferred under the final-round line budget.
Tests cover quoted keys, comments/escapes, multiline args, nested project registrations, parse recovery,
all eight cron markers, disabled sentinel discovery, relative cron paths,
missing and unreadable inputs, multiple scripts and mocked census wiring. Round 3 adds inline-table diagnostics,
duplicate tables, triple-quoted strings, unreadable scripts, pure type-probe classification and mocked special-file
reads that must never open a stream. One temporary-file test verifies both real read paths and the 64 KiB cap.
The cron fixture minimizes the independent review's disabled sentinel and the readable ~/bin/verb-sentinel header.
Piece 2 extends the same script: pure listener, process, merged unit-name and routing-block parsers.
Read-only commands fall back ss -ltnp → ss -ltn → netstat -ltn; failures print unavailable reasons.
On macOS the netstat fallback is `netstat -an -p tcp`; dotted address.port listeners include `tcp4`.
The subprocess allowlist is exactly: `crontab -l`, `ss`, `lsof` (piece 3), `netstat`, `ps`, `systemctl`, `launchctl`.
macOS uses `ps -eo pid,etime,args`; pure elapsed-time parsing converts [[dd-]hh:]mm:ss to seconds.
Other hosts use `ps -eo pid,etimes,args`. Classification starts after leading env assignments and env wrappers:
only cclsp/clojure-lsp executables, runtime main server namespaces/classes, or clj-surgeon with its own
mcp-serve token count as servers; argument paths remain mentions. bb may launch the clj-surgeon script.
Launchd labels and installed filenames are deduplicated after stripping .plist; denied/failed LaunchAgents
listings print `~/Library/LaunchAgents: unavailable: <reason>` and the census continues.
Routing files use the cron reader's regular-file guard and 64 KiB cap; rejected paths print not-regular.
Tests cover IPv4/IPv6, exact ports, command owners, executable/main versus argument mentions,
loaded/installed deduplication, launchd names, absent/malformed blocks and the repository SHA function.
The footer adds Java's local-host name (unknown-host on exception), ISO instant and listener/server/unit counts;
mentions never count as servers. No hostname subprocess is used.
MCP args, URLs and matching cron lines (including inline environment assignments) may expose secrets.
Review output before pasting it; generic redaction is deferred to keep its policy and tests outside this slice.
Acceptance: self-test with a positive test-count gate, serialized kondo, Make target and line counts.
Assertions stay outside output capture; the footer reports registration and cron totals.
README and this plan are Content exemptions as authorized; script plus Makefile changes must total <=150 lines.

## Piece 3: Darwin listener evidence and Claude profiles

The field case is the mayor's MacBook-Air-67.local census: entries=0, cron=0, listeners=0,
servers=0, units=0. The mayor confirmed no listener on 7888 by hand with lsof. That confirms
the listener observation, not the completeness of entries=0: kanban and builder profiles were omitted.
The old code tried ss twice, then Darwin's netstat -an -p tcp. A successful command with no
matching rows printed listen: none; all commands failing printed listen: absent. Thus the reported
none is consistent with netstat succeeding without matches, not evidence that ss was the only check.
Without the original command diagnostics, the exact fallback execution cannot be established.

Piece 3 keeps the census read-only with no service connections. Tool order is ss -ltnp,
ss -ltn, lsof +c 0 -nP -iTCP -sTCP:LISTEN, then netstat (-an -p tcp on Darwin, -ltn elsewhere).
Output identifies listeners-source: ss|lsof|netstat|none. A successful empty check prints listen: none;
exhausting all tools prints listen: unavailable: no ss, lsof or netstat. lsof exit 0 succeeds despite warnings;
exit 1 succeeds with nonblank stdout or no stderr. Empty exit 1 with stderr and other failures fall through.
Owners retain command and pid. Listener totals use the printed section's count, with unavailable counted as zero.

Default paths remain. Extra Claude directories come from nonblank CLAUDE_CONFIG_DIR, immediate
~/.claude-* directories (no symlink following), and repeatable --config-dir <dir> options.
Directory paths expand a leading ~/ against HOME, then resolve real paths when they exist
(absolute normalized paths otherwise) and are deduplicated;
~/.claude is the default, not an extra profile. Discovery failure prints profiles: unavailable: <reason>;
CLAUDE_CONFIG_DIR and explicit directories still contribute, and all host sections and the footer run.
Explicitly selecting ~/.claude also checks its .claude.json. All default and selected candidate files
are normalized and deduplicated together before reading; each file is read once, even when HOME is selected
or a file symlink aliases another candidate. Observations retain candidate order and the first spelling as label.
Registrations print disabled=<value> when the field is present, including disabled=null for JSON null.
Each extra directory contributes .claude.json and settings.json, labelled by full path;
missing files print absent, including misspelt explicit directories. Unreadable files retain diagnostics. Empty directories
count in profiles: <n> extra (<names>), whose names are sorted directory paths.

| Contract | Witness |
|---|---|
| lsof IPv4, wildcard IPv6, loopback IPv6, exact ports and owner | Mayor's literal rows and 78880 decoy |
| ss, lsof, netstat selection, empty success, all unavailable | Mocked command matrix, both host modes |
| Default absent versus extra existing/absent, three extra profiles, candidate order | Literal path-to-JSON map |
| Repeated option and invalid/missing argument | Pure argument matrix and CLI exit probes |
| Environment/glob/explicit/tilde paths, deduplication, symlinks | Isolated temporary HOME boundary probe |
| Missing/unreadable HOME preserves selected profiles | Missing-HOME CLI boundary and injected denied listing |
| lsof warnings, exit 0/1, nil/empty/nonempty stdout | Twelve-case matrix; nil ss stdout stays nil |
| HOME selected by option or environment, one registration | PR 90 round-three regression: entries=1, file once |
| File aliases and repeated candidates read once | Read-count fixture and real-path boundary |

Acceptance remains at most 150 changed script lines, test additions at least source additions,
lines under 120 columns, self-test, ~/bin/clj-kondo and a live Linux census identifying ss.
README and this plan are prose exemptions. No commit or real-home fixture writes.

## Replacement after PR 90

PR 90 closed after its third review round. Codex confirmed that selecting HOME through either
`--config-dir` or `CLAUDE_CONFIG_DIR` read the default `.claude.json` twice, producing entries=2 for
one registration. The previous guard only deduplicated the default settings.json and ran after reads.

The replacement collects every default and selected file candidate, normalizes file paths, and groups
by normalized identity before any config read. Each group is read once and retains its first display label.
Normalization uses the real path when available, otherwise the absolute normalized path; file symlinks
therefore share one read with their targets. Discovery still excludes globbed directory symlinks, while
explicitly selected symlinks resolve normally. JSON null now prints disabled=null.

The PR 90 regression first failed six assertions: each of the two invocations counted and printed the
registration twice and rendered disabled= without null. The final boundary fixture covers absolute,
relative and tilde HOME selections through both entrances, plus a settings.json symlink to .claude.json.
The profile-file fixture separately counts reads, checks default/settings overlap and repeated paths,
and retains absent, empty and unreadable-file diagnostics. Existing profile discovery, CLI options,
listener fallback and warning matrices remain covered. Temporary fixtures are removed in finally;
child censuses use an empty PATH and an absolute bb path to avoid real host commands.

Live ss on Linux is verified on Anvil in the seat run reported in the task handoff. Live lsof on macOS
is not yet verified on a host; the mocked matrix stands in until the mayor's next laptop run.
This replacement's sandbox run used lsof: both ss probes reported
`Cannot open netlink socket: Operation not permitted`. That run is not an additional live ss receipt.

Round-two verification: `bb bin/surgeon-footprint --self-test` passes 24 tests / 415 assertions;
`~/bin/clj-kondo --lint bin/surgeon-footprint` reports zero errors and warnings.
The canonical temporary HOME fixes the macOS /var versus /private/var expectation mismatch.
With java.io.tmpdir set to a symlink and HOME containing a decoy Claude profile, the round-one script
at 2f52605b fails nine assertions; the revised script passes all 415. Before/after logs are retained in
`/var/tmp/forge/pr93-round2-x7r7uuv_/{before,after}.log`. This Linux reproduction is not a laptop receipt.
Mock census tests stub profile discovery and path resolution to literal /project paths; the read-once
fixture pins three extra profiles and exact read order. The denied-discovery fixture also uses literal paths.
Real CLI probes run from the temporary HOME with absolute bb/script/classpath paths and an empty PATH;
absolute, relative and tilde selections through both entrances still produce entries=1 and disabled=null.
The live Linux run at 2026-11-28T10:21:02Z reports listeners-source: lsof, profiles: 0 extra (),
and entries=0 cron=0 listeners=0 servers=0 units=0. Both ss probes are denied by the sandbox's netlink policy.
The script has 131 additions + 19 deletions = 150 changed lines against origin/main at caed7eb1.
Added lines comprise 87 test lines and 44 source/dispatch lines; the longest script line is 119 columns.
`make surgeon-footprint-self-test` also passes; `git diff --check` is clean.
README and this plan remain prose exemptions. No commit or push.
