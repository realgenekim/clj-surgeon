# Process spans (prototype)

node trace.js begin /absolute/task.json task-id
node trace.js run /absolute/task.json read - -- /usr/bin/git status --short
node trace.js mark-start /absolute/task.json edit read
# Perform external native apply_patch here. All intervening time is included.
node trace.js mark-end /absolute/task.json edit
node trace.js run /absolute/task.json proof edit -- /usr/bin/node verify.js
node trace.js end /absolute/task.json
node trace.js report /absolute/task.json

Use unique span IDs; dependencies are comma-separated IDs or -. Begin/end include
manual/model gaps. Commands run in caller cwd with caller environment; receipts
store argv SHA256 only (not raw argv/env/output). Command output remains inherited
in the terminal. Exit status propagates (signals/refusals nonzero); failed commands
still produce completed spans. mark spans are EXTERNAL BRACKET, not pure edit wall.
No rerun/automatic retry. Fixed60s process deadline. Report requires all spans closed.
Do not remove the .lock directory while any command is appending; a stale lock
causes a bounded refusal. This prototype expects trusted local callers and regular
local filesystem files; no network filesystem or hostile process isolation claim.

The report's declared longest-duration DAG path is descriptive, not causal savings.
Use separate fresh traces for repeated attempts; never overwrite or repair a ledger.

Archive limitation of this frozen research version: report checks the current
host/boot clock identity too, so re-reporting on another host or after reboot refuses.
The preserved keeper-report.json and overhead reports can be read as ordinary JSON
anywhere. Do not change recorded ledger clock identities to bypass the check.
