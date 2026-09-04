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
  `budget_bytes`, clj-surgeon shall default it to 24576, refuse a non-integer or
  non-positive value, and refuse any value above the hard cap of 32768 with the
  cap named in the refusal; the request shall never silently receive a clamped
  budget.

- [x] **MCP-OP-THREAD-003**: Conventions as data. When `feature_thread`
  resolves the leg roles for a repository, clj-surgeon shall read them from
  `.clj-surgeon/feature-thread.edn` under the resolved workspace root or from an
  inline `config` map, shall require AT LEAST five leg roles each declaring an id,
  file patterns and a kind, and shall refuse with the searched configuration path
  named when no convention set is available; it shall never infer leg roles from
  a built-in table of file names.

- [x] **MCP-OP-THREAD-004**: Every leg, always rendered. When
  `feature_thread` renders a receipt, clj-surgeon shall emit every leg the
  convention set declares, in the declared order, plus the automatic
  implementation leg, each either FOUND or CANDIDATE with a file, a line range,
  an evidence kind, a sha256 of the body bytes and the body, or ABSENT with the
  exact searches that were run, or N/A with the reason; it shall never omit a
  leg from the receipt.

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
  stated edit-aware order — sibling, governance template, secondary witnesses,
  next_call, menu, route, tests(js), tests, implementation, js-function,
  handler — stopping as soon as the receipt fits, and shall name every elided leg in an `elided:` row
  of both the text block and `structuredContent`; it shall never cut a body
  without naming it and shall never emit a receipt larger than the hard cap.

- [x] **MCP-OP-THREAD-012**: Text superset. When `feature_thread` returns,
  every leaf value of `structuredContent` shall appear in the text block; the
  text block shall never carry less than the structured result.

- [x] **MCP-OP-THREAD-013**: Honest status. When every COUNTED leg is FOUND,
  clj-surgeon shall report `COMPLETE (n of n)`; when any counted leg is ABSENT
  or CANDIDATE it shall report `INCOMPLETE (k of n)` and name each missing leg;
  the status shall be computed from the leg results and shall never read
  COMPLETE while a counted leg is not FOUND.

- [x] **MCP-OP-THREAD-014**: More than five leg roles. When a convention set
  declares MORE than the five leg roles, clj-surgeon shall accept it and resolve
  every declared leg in order; when it declares FEWER than five it shall refuse
  with `feature-thread-conventions-invalid`, naming the count it found.

- [x] **MCP-OP-THREAD-015**: Clojure definitions are definition-shaped. When a
  `:def` leg searches for a seed identifier, clj-surgeon shall recognise a
  Clojure `(defn`, `(defn-` or `(def` form as definition-shaped, in both the
  leg's search and the one-hop alias check; it shall never stamp
  `identifier(def)` on a hit that only the fallback identifier search matched.

- [x] **MCP-OP-THREAD-016**: The automatic implementation leg. When a seed names
  a DEFINITION in a source file, clj-surgeon shall report an `implementation`
  leg for it over the Clojure sources and the script globs the convention set
  declares, without the caller declaring that leg; it shall dedupe the leg by
  file and line range against the legs already resolved; and when no seed
  resolves to a definition it shall report the leg `N/A` with the reason and
  shall NOT count it toward the thread status.

- [x] **MCP-OP-THREAD-017**: Governance anchors. When `feature_thread` reports a
  governance row, clj-surgeon shall report the END line of the top-level EDN
  entry containing the hit and an insertion anchor after it, or `unparsed` when
  no entry resolves; and it shall report exactly one `template` row — the
  matched entry with the highest line — as a range and a refetch, never inlined.

- [x] **MCP-OP-THREAD-018**: Co-primary per language. When the tests leg's
  ranked hits span more than one language, clj-surgeon shall report the best hit
  of EACH language as a primary carrying its own boundary, sha256, anchor and
  body, shall anchor a script witness after its enclosing `test`/`it`/`describe`
  call, and shall render it as a leg row rather than a secondary `also` row.

- [x] **MCP-OP-THREAD-019**: The verify row. When a tests leg is located,
  clj-surgeon shall report the Makefile target(s) that run it as
  `{target, line, command, for, evidence}` with the recipe line verbatim,
  strongest evidence first — the recipe naming the file, else its directory,
  else the Clojure test alias labelled `alias`; when there is no Makefile it
  shall report an empty verify list with the reason named.

- [x] **MCP-OP-THREAD-020**: Edit-aware elision. When the receipt must shrink,
  clj-surgeon shall drop context before edit sites — the forms the seeds name
  and the handler last — in the fixed order `elision-order` declares.

- [x] **MCP-OP-THREAD-021**: The assert line is advisory. When `feature_thread`
  renders the rules row, its assert line shall tell the caller NOT to re-read
  the ranges the receipt already shipped, shall state that this read-only verb
  enforces nothing itself, and shall name `admit_clojure_patch` as the call that
  binds the pre-image.

- [x] **MCP-OP-THREAD-022**: The header names the governed number. When
  `feature_thread` renders its header, it shall show which figure the budget
  governs — the TEXT bytes — beside the structured bytes and their trunk cap and
  the total, and shall not present the leg status as a byte figure.

- [x] **MCP-OP-THREAD-023**: Regex versus division. When the script lexer meets
  a `/` preceded by a JavaScript keyword that ends in an identifier character
  (`return`, `typeof`, `case`, `in`, `of`, `new`, `delete`, `void`, `yield`,
  `do`, `else`, `instanceof`, `throw`, `await`), it shall treat it as a regex
  literal; it shall never label a truncated body `brace-window(lexed,closed)`.

- [x] **MCP-OP-THREAD-024**: CANDIDATE, not FOUND. When a leg's hit is a comment
  mention, or its boundary is not a parsed form or a closed brace window, or its
  only evidence is a fallback search (`identifier`, `route-assembled`,
  `route-tail`, `alias-only`), clj-surgeon shall report the leg `CANDIDATE` with
  the reason named, shall carry its range and body as usual, and shall NOT count
  it toward `COMPLETE`.

- [x] **MCP-OP-THREAD-025**: A binding, not an instruction. When
  `feature_thread` returns a located thread, it shall emit a `next_call` row
  naming `admit_clojure_patch` and carrying `expect_pre_sha256` as WHOLE-FILE
  digests of every located leg's file — the subject that verb actually binds.

- [x] **MCP-OP-THREAD-026**: The receipt is right about its own size. When
  `feature_thread` returns, `text_bytes` shall equal the UTF-8 size of the text
  block delivered, the operation clock shall ride inside the measured receipt at
  a fixed width, a budget refusal shall quote the budget the CALLER passed, and
  `receipt_bytes` shall mean text plus structured wherever it appears.

- [x] **MCP-OP-THREAD-027**: Seed ceilings at admission. When `subject` is
  longer than `max-subject-chars`, or `also` carries more than `max-also-seeds`
  seeds or a seed longer than `max-subject-chars`, clj-surgeon shall refuse
  before reading any file, naming the field and the ceiling.

- [x] **MCP-OP-THREAD-028**: The automatic leg is scanned over its own globs.
  When `feature_thread` bounds its workspace walk, it shall union the automatic
  `implementation` leg's globs into the candidate set BEFORE the walk, so that
  leg can find a definition in a file no declared leg selected.

- [x] **MCP-OP-THREAD-029**: An uncounted leg names its seed and its scope.
  When the automatic `implementation` leg is not counted, clj-surgeon shall
  report `N/A` only when a search really ran and found nothing new — naming the
  SEED whose definition is already a leg, never an unrelated leg's range — and
  shall report `UNSCANNED`, COUNTED toward the leg status, whenever the leg's
  globs were not part of the walk.

- [x] **MCP-OP-THREAD-030**: A printed search is a search that ran. When
  `feature_thread` renders a leg's `found by:`/`searched:` line, the command
  shall reproduce the candidate set the verb actually searched, including any
  `scope.paths` narrowing, so a caller pasting it into a shell cannot get an
  answer the receipt does not have.

- [x] **MCP-OP-THREAD-031**: A character class is part of the regex. When the
  script lexer is inside a regex literal and meets `[`, it shall enter character
  class state and treat every `/` until the matching unescaped `]` as regex
  content, so a valid literal such as `/[/}]/`, `/[^/}]/` or `p.split(/[/\\]/)`
  never ends early and never yields a truncated body labelled
  `brace-window(lexed,closed)`.

- [x] **MCP-OP-THREAD-032**: A comment is not code, however it is spelled. When
  `feature_thread` classifies a hit, it shall treat a line inside a Clojure
  `(comment …)` form, a form discarded by `#_`, or a script `/* … */` block
  comment as a comment mention exactly as it already treats `;` and `//`, so
  such a hit is CANDIDATE with a comment reason and can never make a leg FOUND
  or a thread COMPLETE.

- [x] **MCP-OP-THREAD-033**: The walk is confined and each file appears once.
  When `feature_thread` walks a workspace, containment shall be tested against
  the root plus a path separator, so a sibling directory whose name merely
  extends the root's is outside it, and the candidate list shall be
  de-duplicated so a symlink to a directory inside the tree cannot make one
  file be read and searched more than once.

- [x] **MCP-OP-THREAD-034**: Only a FOUND leg names where to write. When
  `feature_thread` reports a leg or co-primary whose status is `CANDIDATE`, it
  shall omit the insertion `anchor` from both the structured row and the text
  line, because an anchor is a claim about where a new sibling goes and the
  receipt has already said it does not vouch for that range.

- [x] **MCP-OP-THREAD-035**: A script leg states how the browser reaches it.
  When `feature_thread` locates a leg in a script file, it shall report
  `export`: the file, line and text of the `export`, `module.exports` or
  `window.X =` registration site for one of the identifier seeds, or the
  statement `none (classic script; functions are globals)` when the file has no
  module syntax at all, so a reader never searches for a registration site that
  does not exist.
