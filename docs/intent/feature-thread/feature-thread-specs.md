---
parent: feature-thread-design
prefix: MCP-OP-THREAD
status: "proposed (forge@anvil, 2026-09-04); built under Gene's build-and-measure instruction"
---

# Feature-Thread EARS Specifications

Status: **proposed (forge@anvil, 2026-09-04)**.
Every identifier below is `[x]` and is therefore witnessed BOTH by an `@spec`
annotation in `src/clj_surgeon/mcp_feature_thread.clj` and by a test in
`test/clj_surgeon/mcp_feature_thread_test.clj` that fails without the behavior.
The repository intent audit (`clj-surgeon.mcp-intent-contract/audit-current-repository`,
run as a test inside `make mcp-test`) enforces both halves.

## Requirements

- [x] **MCP-OP-THREAD-001**: Admission. When `feature_thread` receives a
  request, clj-surgeon shall require a non-blank `subject`, accept an optional
  `also` vector of additional identifier or route seeds, an optional `scope`
  carrying `workspace_root` and `paths`, an optional `config` naming a
  repository convention set or carrying it inline, an optional
  `budget_bytes` and an optional `include_bodies`; it shall refuse an unknown
  field, a blank subject, a non-vector `also`, or a malformed `scope` before
  reading any file.

- [x] **MCP-OP-THREAD-002**: Budget admission. When `feature_thread` receives
  `budget_bytes`, clj-surgeon shall default it to 12288, refuse a non-integer or
  non-positive value, and refuse any value above the hard cap of 32768 with the
  cap named in the refusal; the request shall never silently receive a clamped
  budget.

- [x] **MCP-OP-THREAD-003**: Conventions as data. When `feature_thread`
  resolves the leg roles for a repository, clj-surgeon shall read them from
  `.clj-surgeon/feature-thread.edn` under the resolved workspace root or from an
  inline `config` map, shall require exactly five leg roles each declaring an id,
  file patterns and a kind, and shall refuse with the searched configuration path
  named when no convention set is available; it shall never infer leg roles from
  a built-in table of file names.

- [x] **MCP-OP-THREAD-004**: Five legs, always rendered. When
  `feature_thread` renders a receipt, clj-surgeon shall emit exactly the five
  legs the convention set declares, in the declared order, each either FOUND
  with a file, a line range, an evidence kind, a sha256 of the body bytes and
  the body, or ABSENT with the exact searches that were run; it shall never omit
  a leg from the receipt.

- [x] **MCP-OP-THREAD-005**: Clojure legs are parsed. When a leg resolves to a
  Clojure file, clj-surgeon shall parse that file and report the range of the
  enclosing top-level form containing the hit, and shall use that exact range as
  the body; it shall never report a fixed line window for a Clojure leg.

- [x] **MCP-OP-THREAD-006**: JavaScript legs are brace-matched or labelled.
  When a leg resolves to a non-Clojure script file, clj-surgeon shall extract the
  body by matching braces from the definition line and label the evidence
  `brace-match`; when brace matching cannot close the body, it shall fall back to
  a bounded line window and label the evidence `window`; it shall never present a
  window as a matched body and shall never parse JavaScript.

- [x] **MCP-OP-THREAD-007**: One-hop alias, or alias-only. When the only
  definition-shaped occurrence of a seed identifier in a script file is an alias
  of the form `const X = Y;`, clj-surgeon shall follow that alias exactly one hop
  and, when the target definition is found, report the target's body with the
  alias site named in the evidence; when the target is not found, it shall report
  the leg ABSENT with evidence `alias-only` and both searches quoted, and shall
  never report the alias line as the implementation.

- [x] **MCP-OP-THREAD-008**: Content hash. When a leg is FOUND, clj-surgeon
  shall report a lowercase hex sha256 of the exact body bytes it read, in both
  the text block and `structuredContent`, so a later edit can assert its
  pre-image; the hash shall be omitted only when the leg is ABSENT.

- [x] **MCP-OP-THREAD-009**: Sibling. When the convention set or the request
  names a mirror rule, clj-surgeon shall resolve the neighbouring feature the
  subject should mirror and shall report its five legs with bodies elided to line
  ranges by default; when no sibling can be resolved it shall report the sibling
  row ABSENT with the rule that was applied, and the sibling row shall never
  count toward the five-leg status.

- [x] **MCP-OP-THREAD-010**: Rules. When the Clojure handler leg is FOUND,
  clj-surgeon shall report a rules row naming the wiring contract derived from
  the located forms: the editor or persistence path the handler calls, the
  required-argument precedent it enforces, and every `INTENT:` identifier
  appearing in comment lines immediately above any located form; an `INTENT:`
  identifier present above a located form shall never be dropped from the rules
  row.

- [x] **MCP-OP-THREAD-011**: Budget elision. When the rendered receipt exceeds
  the effective budget, clj-surgeon shall elide bodies to line ranges in the
  order tests, sibling, menu/caller, js-function, route, handler, stopping as
  soon as the receipt fits, and shall name every elided leg in an `elided:` row
  of both the text block and `structuredContent`; it shall never cut a body
  without naming it and shall never emit a receipt larger than the hard cap.

- [x] **MCP-OP-THREAD-012**: Text superset. When `feature_thread` returns,
  every leaf value of `structuredContent` shall appear in the text block; the
  text block shall never carry less than the structured result.

- [x] **MCP-OP-THREAD-013**: Honest status. When every declared leg is FOUND,
  clj-surgeon shall report `COMPLETE (5 of 5)`; when any leg is ABSENT it shall
  report `INCOMPLETE (k of 5)` and name each missing leg; the status shall be
  computed from the leg results and shall never read COMPLETE while a leg is
  ABSENT.
