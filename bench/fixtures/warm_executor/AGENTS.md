# Prepared-edit executor fixture

For every requested edit, call the `clj-surgeon` MCP `edit_clojure` tool exactly
once with the arguments supplied by the user. Do not inspect files, run shell
commands, use another write tool, or change any other subject. After a verified
tool success, reply with exactly `EXACT_OK`.
