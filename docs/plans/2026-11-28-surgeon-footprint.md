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
The subprocess allowlist is exactly: `crontab -l`, `ss`, `netstat`, `ps`, `systemctl`, `launchctl`.
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
