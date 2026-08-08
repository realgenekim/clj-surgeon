# Repository Agent Instructions

Read and follow [CLAUDE.md](CLAUDE.md) before making changes. It is the
canonical repository instruction file for all coding agents, regardless of
vendor or runtime.

Its Clojure routing and deferred-cclsp discovery rules apply before native
Read, Edit, grep, sed, or cat touches existing source. Load the working-tree
skill before acting.

Use the hottest capable entrance: prefer the persistent `inspect_clojure` and
`apply_clojure_changes` MCP tools. Use `~/bin/clj-surgeon` only when MCP is
unavailable, the operation is not exposed there, or the CLI itself is under
test.

For non-trivial feature work, `CLAUDE.md` requires the design, planning,
testing, documentation, and verification standards that must be satisfied
before the work is complete. Do not treat those linked documents as optional
background reading.
