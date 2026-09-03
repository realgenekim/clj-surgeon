---
parent: read-hook-design
prefix: MCP-OP-READ-HOOK
status: "red-first; O4 'Route, don't ask' — free-choice adoption 0 of 19, 2026-09-04"
---

# Read-Side Routing Hook Specifications

These IDs are stable and must not be reused if a requirement is deleted.
Status marks follow the repository contract: `[ ]` active gap (test witness
required), `[x]` implemented (implementation and test witnesses required),
`[D]` deferred.

- [x] **MCP-OP-READ-HOOK-001**: While every path argument of a ripgrep invocation is a directory whose complete ripgrep candidate set contains only Clojure source files, when the read hook runs that invocation, the read hook shall print output byte-identical to the output ripgrep itself would print for the caller's original argument vector.

- [x] **MCP-OP-READ-HOOK-002**: When the read hook cannot serve an invocation exactly, the read hook shall execute the real ripgrep with the caller's original argument vector unchanged.

- [x] **MCP-OP-READ-HOOK-003**: When the read hook finishes an invocation and a route log is configured, the read hook shall append exactly one route record naming the path arguments, the flags, whether the answer was served through the clj-surgeon read path or fell back to ripgrep, the elapsed milliseconds, and the byte count of the answer.

- [x] **MCP-OP-READ-HOOK-004**: When the read hook finishes an invocation, the read hook shall exit with the exit status ripgrep produced for that invocation.

- [x] **MCP-OP-READ-HOOK-005**: When the read hook resolves the real ripgrep executable, the read hook shall never resolve to the hook itself.

- [x] **MCP-OP-READ-HOOK-006**: While the caller's original invocation would have printed a filename prefix on every matching line, when the read hook serves that invocation from an explicit file list, the read hook shall request the filename prefix from ripgrep.

- [x] **MCP-OP-READ-HOOK-007**: While the read hook serves an invocation, the file set the read hook searches shall be the file set the clj-surgeon read path returned, and the read hook shall refuse to serve when that set differs from ripgrep's own candidate set for the same invocation.

- [x] **MCP-OP-READ-HOOK-008**: When the read hook falls back to ripgrep, the read hook shall not have written any byte of its own to standard output or standard error.

## Misreadings these requirements exist to forbid

- "Byte-identical means the same lines." It does not. It means the same bytes,
  in the same order, with the same context separators and the same filename
  prefixes. Ripgrep's own directory walk is **not** deterministic between runs
  (measured 2026-09-04: twelve runs of one command over a 25-file tree produced
  twelve distinct SHA-256s), so 001 is stated against a deterministic baseline
  and the witness pins it there. A witness that compares against an unsorted
  ripgrep run is comparing against a coin flip.
- "If the hook cannot serve it, print what it can and note the rest." A partial
  answer on the agent's only discovery path is an outage that looks like a
  result. 002 and 008 make the fallback total and silent.
- "Falling back means printing ripgrep's error." It means *becoming* ripgrep:
  the same argument vector, the same streams, the same exit status. A message
  the real ripgrep would not have printed is a detectable difference and a
  contaminated cohort.
- "The read path is a cache, so a stale or truncated receipt is still usable."
  It is not. A truncated `ls-tree` receipt names fewer files than the tree
  holds; serving from it silently drops matches. 007 requires refusal.
- "The hook is faster, so the file set can be trusted without checking." The
  hook makes **no wall claim at all**. 007 keeps ripgrep's own candidate set as
  the falsifier for the read path's answer, so a discovery defect surfaces as a
  logged fallback rather than as a missing match.
- "Equal sets means the same search." It does not. Path arguments can overlap,
  and ripgrep then prints a file once per argument that reaches it, interleaved
  by argument rather than grouped by file. Set equality is blind to that
  multiplicity, and an explicit file list cannot reproduce the interleaving at
  all. MCP-OP-READ-HOOK-007 therefore compares counts as well as sets, and
  MCP-OP-READ-HOOK-002 refuses overlapping path arguments outright.
- "Exit 0 when we served successfully." Ripgrep exits 1 when nothing matched.
  004 forbids inventing a status.
- "Resolving `rg` from `PATH` is enough." The hook *is* `rg` on that `PATH`.
  005 exists because the obvious implementation is an infinite exec loop.

## Falsifiers

The falsifier matrix, with the required result and the named witness for each
requirement, is in `read-hook-design.md`. Each falsifier is executable and
lives in `test/clj_surgeon/read_hook_test.clj`.

## Rationale

Free-choice adoption of `inspect_clojure` is **0 of 19** across the program
(`docs/observations/2026-09-04-e6-lb-cohort.md`, 0 of 3 on a neutral exposure;
`docs/observations/2026-09-04-e6c-routing-plate-cohort.md`, 0 of 3 with the
routing sentence in the tool description and quoted back verbatim by the
model). `docs/vision.md` already rules the mechanism: *"A tool's presence and
name are not a path"* and *"Sit on the agent's route. Do not ask it to change
route; it will not, and it is right."* Every one of the nine E6 arms opened
with the same reflex, `rg -n … 'System/currentTimeMillis' src`.

This leaf is the read side of that ruling: the route is `rg`, so the hook sits
on `rg`. It is deliberately a **transparent** rung — it changes who decides
which files are searched, and nothing else — because the alternative, enriching
the answer, is by construction detectable and belongs in its own cohort with
correctness, not routing, as the primary.
