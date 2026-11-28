# Read-only machine footprint: parser and census slice

Piece 1 covers registrations and cron only. `bin/surgeon-footprint` reads six fixed MCP config paths
and the current user's crontab. System units and other users' crontabs are out of scope.
The config paths are `~/.codex/config.toml`, `~/.claude.json`, `~/.claude/settings.json`,
`~/.claude/mcp.json`, `./.mcp.json` and `./.codex/config.toml`.
TOML census parsing strips comments outside quotes, accepts quoted dotted server keys and joins args arrays;
this is a config-subset reader, not a validating TOML implementation (multiline strings are outside this slice).
JSON root and project registrations retain project paths; a per-file parse error prints and the census continues.
Cron retains active lines naming the tools and includes disabled or active lines referencing readable scripts
naming clj-surgeon, cclsp or clojure-lsp or containing 7906, 7888, 7890, surgeon-call or /mcp.
Each matching script path appears alongside the original line.
Script paths may be literal quoted or unquoted tokens (absolute, relative or ~/); no shell expansion or execution.
Relative cron script paths resolve against $HOME; project config paths resolve against the census cwd.
No writes, signals or MCP service connections; absent, unreadable and command-failure inputs remain distinct.
Tests cover quoted keys, comments/escapes, multiline args, nested project registrations, parse recovery,
all eight cron markers, disabled sentinel discovery, relative cron paths,
missing and unreadable inputs, multiple scripts and mocked census wiring.
The cron fixture minimizes the independent review's disabled sentinel and the readable ~/bin/verb-sentinel header.
Piece 2 adds launch units (loaded AND installed user systemd unit files, plus launchd), listeners,
processes and the routing-block hash. Process server/mention classification, server-only counts,
malformed-block detection and hostname/time metadata remain deferred.
The `command=` lines may echo tokens embedded in MCP args or URLs; review output before pasting it.
Acceptance: self-test with a positive test-count gate, serialized kondo, Make target and line counts.
Assertions stay outside output capture; the footer reports registration and cron totals.
README and this plan are Content exemptions as authorized; script plus Makefile changes must total <=150 lines.
