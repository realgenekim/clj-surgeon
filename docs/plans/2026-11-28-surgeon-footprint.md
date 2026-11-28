# Read-only machine footprint: parser and census slice

`bin/surgeon-footprint` reads six fixed MCP config paths, current-user systemd units and the current user's cron.
TOML census parsing strips comments outside quotes, accepts quoted dotted server keys and joins args arrays;
this is a config-subset reader, not a validating TOML implementation (multiline strings are outside this slice).
JSON root and project registrations retain project paths; a per-file parse error prints and the census continues.
Cron retains active lines naming the tools and includes disabled or active lines referencing readable scripts
containing 7906, 7888, 7890, surgeon-call or /mcp, with each matching script path alongside the original line.
Script paths may be literal quoted or unquoted tokens (absolute, relative or ~/); no shell expansion or execution.
No writes, signals or service connections; absent inputs and command failures remain visible.
Tests cover quoted keys, comments/escapes, multiline args, nested project registrations, parse recovery,
all five cron markers, disabled sentinel discovery, missing inputs, multiple scripts and mocked census wiring.
The cron fixture minimizes the independent review's disabled sentinel and the readable ~/bin/verb-sentinel header.
Deferred for the 150-line budget: listeners, process server/mention classification and server-only counts,
routing hashes and malformed-block detection, macOS launch agents, and hostname/time metadata.
Acceptance: self-test, live census including the disabled sentinel, serialized kondo, Make target and line counts.
README and this plan are Content exemptions as authorized; the script plus existing Make target must total <=150.
Live cron verification remains blocked if the environment denies access to the current user's crontab spool.
