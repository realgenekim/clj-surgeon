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
exhausting all tools prints listen: unavailable: no ss, lsof or netstat. lsof's empty exit 1 with
no stderr is an empty selection; other failures fall through. Owners retain command and pid.

Default paths remain. Extra Claude directories come from nonblank CLAUDE_CONFIG_DIR, immediate
~/.claude-* directories (no symlink following), and repeatable --config-dir <dir> options.
Directory paths are normalized and deduplicated; ~/.claude is the default, not an extra profile.
Explicitly selecting ~/.claude also checks its .claude.json; its settings.json is counted only once.
Registrations print disabled=<value> when the field is present.
Each extra directory contributes existing .claude.json and settings.json, labelled by full path;
missing extra files are omitted. Unreadable files retain diagnostics. Empty extra directories still
count in profiles: <n> extra (<names>), whose names are sorted directory paths.

| Contract | Witness |
|---|---|
| lsof IPv4, wildcard IPv6, loopback IPv6, exact ports and owner | Mayor's literal rows and 78880 decoy |
| ss, lsof, netstat selection, empty success, all unavailable | Mocked command matrix, both host modes |
| Default absent versus extra existing/absent, two profiles | Literal path-to-JSON map, no temp directory |
| Repeated option and invalid/missing argument | Pure argument matrix and CLI exit probes |
| Environment/glob/explicit paths, deduplication, symlinks | Isolated temporary HOME boundary probe |

Acceptance remains at most 150 changed script lines, test additions at least source additions,
lines under 120 columns, self-test, ~/bin/clj-kondo and a live Linux census identifying ss.
README and this plan are prose exemptions. No commit or real-home fixture writes.

Verification in review round 2: the listener subprocess matrix requires lsof +c 0 and supplies
a literal clojure-lsp listener on port 9000; the existing exact invocation failed 12 assertions before
the fix. Linux and Darwin netstat winners now receive their respective row shapes.
The default-profile census fixture contains a real settings registration and asserts entries=2,
so removing its duplicate-read guard fails. The temporary HOME boundary rejects a matching symlink
and regular file, tests environment/explicit-path deduplication through profile-dirs, then invokes
the CLI with one explicit external directory and an empty CLAUDE_CONFIG_DIR. It reports three extra
profiles and checks disabled=true output. It does not exercise an empty profile, repeated options
or default-profile selection through the CLI; repeated options have a pure parsing witness.
Fixtures are removed in finally. Removing the default settings guard in a temporary script copy
produced two failures (duplicate path and entries=3); the working script passes 23 tests / 352 assertions.
Serialized ~/bin/clj-kondo reports zero errors/warnings. Relative to the PR base 2535894d, the script
has 135 added + 15 deleted = 150 changed lines, with a longest line of 118 columns.
The live run reports listeners-source: lsof and listen: none; both ss probes report
Cannot open netlink socket: Operation not permitted.

The requested live listeners-source: ss receipt remains unavailable in this sandbox.
The subprocess matrix verifies successful ss selection, but is not live-host evidence or an approved
substitute for that acceptance requirement. A live macOS receipt is also outstanding.
Deferred under the 150-line script budget: accepting lsof stdout alongside benign stderr, nonexistent
HOME handling, and tilde expansion of quoted explicit paths. Each needs behavior and boundary tests;
this round retains existing coverage and prioritizes the confirmed defects.
