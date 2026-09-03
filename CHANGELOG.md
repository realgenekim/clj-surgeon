# Changelog

All notable changes to clj-surgeon are documented here.

Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). No version
tags yet — everything accumulates under Unreleased until a baseline release
is cut.

## [Unreleased]

### Fixed

- `invalid-require-policy` now reaches `apply_clojure_changes`. The refusal
  that names the field, lists `minimal` and `copy-all`, echoes what it
  received, and states the field is never defaulted used to be unreachable
  from the write surface: the request contract refused `invalid-enum` or
  `missing-fields` first, and neither named the accepted values. Both an
  omitted and an unaccepted `require_policy` now produce the same named
  refusal on both routes, and the visible summary shows the field and its
  values.

- A published dispatch vocabulary is now bounded by characters as well as by
  arm count, and each entry is rendered as one comment-free line. Sixty long
  dispatch spellings used to produce kilobytes of refusal evidence, and a
  `;;` inside one commented out the rest of the joined summary line. The
  selector compares parsed dispatch values, so the rendered spelling is still
  one the selector accepts.

- `expect_matched` now decides "addressed" from preorder address spans
  rather than pre-image line numbers. Two matched sites on one line used to
  report "all matched sites addressed" when only one of them was edited —
  the wrong failure direction for a receipt whose job is naming what a
  transaction skipped. Sites nested inside an edited form still count as
  addressed.

- Restored the SCI computed-program capability boundary after the no-default
  `case` compatibility change exposed constructor shorthand and dot interop.
  Executable constructor, method, field, and explicit-dot forms now refuse
  before evaluation; quoted structural data and macro-expanded `case` remain
  supported. Permanent regressions capture the host-object and stderr-I/O
  exploit shapes and the pre-change causal control.

### Added

- Read refusals now name their own field. `inspect_clojure`'s `missing-fields`
  refusal reports the omitted names, the complete required set at that path, and
  the minimal valid object there, and its visible summary shows all three plus
  the next call. `invalid-require-policy` names the field, lists `minimal` and
  `copy-all`, echoes what it received, and states that the field is required and
  never defaulted — the published schema declares it required, so clj-surgeon
  does not substitute a default. A `match` result whose pattern uses `_` as a
  standalone wildcard and returns zero, or fewer than its declared expectation,
  now carries the note that each `_` matches exactly one subtree and a longer
  form needs a longer pattern; a pattern without a standalone `_` carries no
  note.


- `apply_clojure_changes` now reports matched-but-unaddressed sites. Copy the
  `file`, `file_hash`, pattern, and `match_count` from a prior `inspect_clojure`
  `match` receipt into the new optional `expect_matched` object and the
  transaction receipt returns `matched_count`, `addressed_matches`, and
  `unaddressed_matches` — every matched site the transaction did not touch,
  with its pre-image line and source hash. The basis is stateless: the file hash
  fences it against the transaction's own frozen pre-image, and a file, hash, or
  count disagreement refuses `expect-matched-stale` before any write. Field
  case: 19 guard sites matched, 16 addressed, and the exclusion rationale for
  the other 3 lived only in the driver's head.


- Multimethod owner addressing is now discoverable from reads. Every outline row
  for a `defmethod` carries `dispatch`, the exact source spelling of that arm's
  dispatch value, so a file whose owner vocabulary collapses many arms into one
  name still shows how to address one of them. The exact-owner selector refusal
  now recognizes a selector whose leading name owns `defmethod` arms and
  publishes the exact `{kind, name, dispatch}` owner form to send, the entrance
  that accepts it, and a bounded dispatch vocabulary (at most 40 values, with
  returned/omitted/truncated counts). Field case: 117 arms collapsed to one
  owner `fold-event`, and a cold agent paid a refusal to learn the shape from
  another tool's schema.


- Experimental `inspect_clojure` MCP read batches. One typed call can retrieve
  ordered named forms, a compact outline, exact structural matches, and
  capability-limited X-ray results from immutable once-read file snapshots.
  The failure-atomic contract rejects unknown fields, duplicate IDs, unsafe
  paths, cardinality mismatches, and bounded-output violations without partial
  evidence. The HTTP server now exposes exactly this read-only tool and the
  existing guarded `apply_clojure_changes` write tool, with standard read-only
  annotations and full `structuredContent` results. Path confinement is shared
  by both tools.
- A frozen four-task inspect portfolio and counterbalanced three-lane harness.
  The first four-run experiment kept 4/4 correctness, one MCP call, no shell
  calls, and no failed calls, but its 27.97-second median was only 13.8% below
  the CLI route's 32.44 seconds. The 2× hypothesis and 30% keep threshold were
  not met; the negative result is retained in the Captain's Log.
- Experimental persistent MCP entrance `apply_clojure_changes`. One typed call
  compiles exact owner-scoped replacements into the existing failure-atomic
  transaction kernel, rejects paths outside the configured project root,
  returns terminal read-back hashes, and publishes an inverse receipt. The
  loopback Streamable HTTP server includes health, readiness, full local
  telemetry, and an embedded development nREPL. Stdio remains a protocol smoke
  entrance. `make install-mcp-codex-dev` enables the branch-local experiment;
  stable `make install` does not.
- MCP-aware benchmark evidence for the frozen six-edit, two-file decision.
  Four assisted runs were exact at a 24.530-second median versus 43.190 seconds
  for native and 36.396 seconds for the current CLI-and-skill route. Four
  metadata-only runs produced zero MCP calls. A one-sentence project rule
  produced 4 / 4 adoption at a 27.432-second median.

- `:cat :forms '[a b c]'` reads several known top-level owners from one parsed
  file snapshot and returns their exact sources in requested order. The batch
  rejects invalid or duplicate names and returns no partial source when any
  owner is missing or ambiguous. Combined source over 65,536 characters also
  refuses without partial source. One optional `:platform` disambiguates the
  complete CLJC batch.
- `:cat :spec-file -` reads known owners across files as one guarded manifest.
  It preserves requested file and form order, reads each physical file once,
  and requires exact `:file-count` and `:form-count` values. Unknown keys,
  duplicate physical paths,
  count mismatches, selection failures, and excessive combined output refuse
  the complete transaction without partial source. Each successful file result
  includes its complete-snapshot hash.
- `:cat :spec-file - :format :semantic` emits a transcript-bounded canonical
  behavior view with one file hash and short header per form. It removes
  comments and layout and may expand reader shorthand; the default EDN format
  remains the exact lexical-source contract. Help and both agent skills now
  teach a noninteractive `printf | clj-surgeon` stdin route so callers do not
  invoke `:spec-file -` and wait for later input.
- `:change`, `:change!`, and `:undo-change!` for one heterogeneous exact intent
  transaction. A spec declares explicit file scopes, losslessly exact
  `:from` / `:to` forms, per-intent match counts, and aggregate intent, edit,
  and changed-file counts. `:change` compiles and previews the combined future
  state without writing. `:change!` rechecks snapshot hashes, commits all files
  with handled-failure rollback, verifies read-back hashes, and publishes one
  durable inverse receipt. `:undo-change!` refuses the complete inverse if any
  forward result hash is stale. Receipts use semantic child paths, preserve
  literal `#()` and comments, and refuse corrupt paths instead of falling back
  to unstable preorder coordinates.
- Scoped `:changes` compile explicit `:in` files, optional named `:forms`, exact
  structural `:find`, and `:do [:replace SOURCE]` into the same atomic
  transaction engine. Per-change `:matches`, `:each-form`, and `:each-file`
  guards prove both total cardinality and distribution. Aggregate `:changes`,
  `:edits`, and `:files` guards close the complete manifest. Named owners must
  resolve exactly once, and any mismatch refuses before writing. Legacy exact
  `:intents` remain accepted but cannot be mixed with scoped `:changes`.
- `:match-form`, the preferred name for structural pattern search. The existing
  `:find-subform` and `:grep-form` spellings remain compatibility aliases.
  Supplying the historically guessed `:pattern` argument now recommends
  `:match-form :match` for an EDN form and a bounded text-search command for a
  value that contains regular-expression alternation.
- Guarded literal `:edit :expect` no longer requires `:plan-out`. It applies
  the in-memory hash-fenced plan through the same atomic executor and returns
  the same whole-file parse and read-back-hash receipt. Supply `:plan-out` only
  to retain an audit artifact. Plan-only and computed edits still require the
  artifact and the separate review/apply boundary.
- Literal `replace` and `replace-span` forms written inline in `:edit :expr`
  now preserve their exact replacement source. Anonymous-function shorthand,
  comments, commas, metadata, reader syntax, and multiline layout survive the
  plan and verified apply. The planner carries this syntax as query metadata,
  verifies it against the evaluated replacement, and keeps saved EDN plans
  concrete and replayable. Computed replacements and the `:query` data surface
  retain canonical printing.
- `(line N)` and `[:line N]` structural roots for `:xray` and `:edit`. They
  select the one top-level form whose physical range or attached comment
  contains N, including otherwise unnamed custom macro forms. Blank gaps
  refuse with `:line-not-in-form`; overlapping reader-conditional owners
  refuse with `:ambiguous-form`. A following `match` can select one exact leaf,
  and literal replacement plus `:expect` remains a verified one-call edit.
  The same line contract now backs `:cat`, X-ray, planning, help, and both agent
  skill entrances. Shell-generated form commands quote names containing `>`,
  `?`, and other metacharacters.
- `:edit :expect FORM`, the optional one-call guarded edit. `:expect` is the
  caller's declared before-state: exactly one Clojure form, compared with the
  selected form losslessly. Whitespace does not change the verdict. Comments,
  metadata, reader macros, and token spelling must match. On equality `:edit`
  applies the in-memory plan through the existing `:replace-subform!` executor,
  returning its evidence merged with the verified apply receipt and `:mode
  :expect-guarded`. An optional `:plan-out` retains and verifies an audit
  artifact. Any difference refuses with
  `:error-type :expect-mismatch` and reports `:expected`, `:actual`, and
  `:actual-source` while source bytes and an existing plan artifact stay
  unchanged; a `:expect` that is not exactly one readable form refuses with
  `:error-type :invalid-expect` before the source read. Selection refusals keep
  their existing error types. `:expect` is optional and the default flow is
  unchanged: without it `:edit` remains plan-only, reviewed, then applied
  separately and requires `:plan-out`. `:expect` refuses computed `transform`
  replacements because their generated after-state still requires review.
  Retained plan paths must use an `.edn` suffix. Atomic writes preserve the
  source file's existing permissions.
- `:xray`, a read-only pure Clojure analysis over structurally selected values.
  A plain path such as `(form 'transition)` returns literal source;
  `(analyze path pure-function)` receives one stable ordered vector of ordinary
  Clojure data;
  `(expect-count path n)` optionally refines cardinality without changing that
  input type. `initializer` selects a `def` right-hand side without evaluating
  it. Computed analysis gives a selected map literal or top-level
  `hash-map` / `array-map` syntax one shallow, non-evaluating canonical view
  while nested constructor syntax and exact evidence remain source-shaped.
  `tree-seq` supports shape-independent traversal without a separate schema
  probe. Pure `key`, `val`, and `for` work in SCI, while direct loop and private
  macro-expansion internals remain refused. The sandbox is capability-limited,
  not termination-proof; callers remain responsible for bounded work.
  Computation returns `:value` with
  compact hash evidence. The canonical agent skill is a validated compact task
  router with advanced workflows loaded on demand.
  Former read spellings remain compatibility inputs but are not the primary
  surface. Named selection now sees `#?`
  and `#?@` branch-local forms and accepts an optional platform. X-ray never
  writes source or a plan and refuses truncated evidence, analyzer failure,
  non-EDN results, and oversized output.
- `:edit :expr`, a sandboxed SCI authoring surface that gives agents pure
  `clojure.core` collection functions and structural builders. The expression
  compiles to the existing lens query, saves the unchanged hash-bound review
  plan, and applies later through `:replace-subform!`. I/O, processes,
  namespaces, mutable references, and host interop remain unavailable.
- Native `(transform path pure-function)` edits. The function receives the
  exactly-one selected form as Clojure data. Planning saves its concrete
  replacement, not executable code, so the existing EDN executor remains the
  only apply path.
- `:lens` / `:q`, a jq-like EDN pipeline over the concrete Clojure syntax
  tree. A getter such as
  `[[:form transition] [:find :finish] :right]` reads a peer value across
  `case`, `cond`, map, and binding shapes with exact source, semantic path,
  address, ownership, source hash, and per-step cardinality. Ending the same
  path in `[:replace FORM]` emits one existing hash-bound replacement plan,
  never writes source, and applies later through `:replace-subform!`.
- `[:span 2]` promotes a selected node and its semantic peer into one located
  slice. A terminal `[:replace-span FORM FORM]` plans an equal-arity peer edit
  while preserving all internal comments and whitespace, including flattened
  sibling bodies inside `#(...)`; plans apply through `:replace-subform!`.
- `[:partition-all 2]` (general form: `[:partition-all N]`) partitions a
  selected node and its following semantic
  siblings into consecutive lossless spans. It makes full `case`, `cond`, map,
  binding, and alternating-argument inventories one structural read, retains
  a shorter final span without semantic inference, and reuses the guarded
  equal-arity `:replace-span` updater when exactly one partition is selected.
- `:outermost` filters the current query stream to maximal concrete-syntax
  subtrees while retaining disjoint roots. Use `[:find cond] :up :outermost`
  when repeated nested heads make the first outer guard unknown; placement is
  explicit (`:outermost :up` cannot remove contained owners), and a known first
  guard remains the shorter anchor.
- Machine-readable `clj-surgeon --version`, plus `:planned-operation` in apply
  receipts so the generic executor identifies node versus span plans.
- `:show-form`, a read-only one-shot that returns one complete top-level form
  by unqualified name, containing line, or case-sensitive literal `:contains`
  selector, with CLJC platform disambiguation,
  exact source, location, and a complete-file source hash. `:cat` is its strict
  structural-shell alias and never dumps the complete file.
- Executable `:show-form` remedies for the historically guessed `:get` command
  and line-only `:find-subform` invocation.
- `:grep-form`, a strict structural-shell alias for file-wide or optionally
  scoped `:find-subform` search.
- One-shot literal routing that replaces the former `rg -n` to
  `:show-form :line` bridge, structural
  sibling-pair boundaries, and a mandatory review boundary between replacement
  planning and application. CLI `:contains` values remain literal text even
  when they look like EDN, so `:contains :finish` works directly.
- Enclosing `:inside` ownership on every named file-wide structural match, plus
  read-back-verified `:replace-subform!` receipts with `:verified` hash, parse,
  and atomic-write evidence.
- `make install-cli` and the explicit `CLI_DEST=/path/to/clj-surgeon`
  override, including parent-directory creation and paths containing spaces.
- One vendor-neutral canonical agent skill, exposed as byte-identical native
  Claude and Codex packages with a drift-tested compact legacy entrance.
  Stable installation now uses content-addressed, read-only CLI and skill
  copies with commit/hash receipts; `make install-dev` is the explicitly
  branch-coupled development mode.
- Dependency-aware `:mv` planning plus `:mv-with-deps`, an opt-in alias for
  `:mv :with-deps true` that discloses and moves the minimum required
  transitive dependency closure.
- `:ls-tree` / `:tree` / `:map` — map every Clojure project under a directory:
  namespaces, form counts, line counts. Supports `:grep` filtering to find
  projects by content (e.g. `:grep "postgres|jdbc"`).
- Ops registry: single dispatch map driving dispatch, `--help`, and error
  messages. `--help` alone lists all ops by category; `:op <x> --help` shows
  per-op args and examples.
- `.clj-surgeon.edn` project-local config: declare source paths and project
  aliases once, then reference files by alias.
- CLJC structural operations: `:cljc-merge`, `:cljc-split`,
  `:cljc-add-require`, `:cljc-analyze` — merge CLJ/CLJS pairs into CLJC with
  reader conditionals, split back out, classify forms by platform.
- Reader-conditional awareness across all ops (outline, deps, extract).
- Centralized form classification in `forms.clj` — one source of truth for
  what counts as a defn, what is private, etc.

### Fixed

- Platform-qualified X-ray and lens form selection now derives ordinary-form
  platforms from `.clj`, `.cljs`, or `.cljc`. Cross-platform plain-file reads
  return zero evidence, while missing or unsupported file context refuses with
  structured EDN instead of matching a plausible wrong form.
- `:cljc-split` now writes both requested output files and returns their paths;
  the previous threaded side-effect form produced invalid `assoc` calls.
- `:mv` no longer reports success after introducing unresolved dependencies or
  stranding callers during a downward move. Refusals return stable EDN,
  recommend `:mv-with-deps` when applicable, and leave the file unchanged.
- `:mv` dependency refusals now recommend a non-mutating preview and expose a
  separate `:apply-command`; stranded dependency evidence distinguishes the
  original `:defined-at` line from its rejected-candidate `:would-be-at` line.
  Successful dry runs repeat `:apply-command` so each preview is self-contained.
- Global and `:mv` help no longer claim that only `!` operations write. The
  move help now gives an LLM-safe preview/refusal/review/apply workflow.
- Intra-namespace dependency analysis no longer treats common lexical bindings
  or quoted data as top-level var dependencies.
- ClassCastException on bare string ops: `clj-surgeon :op ls-tree` (value
  without leading colon) crashed instead of dispatching. Ops now accept both
  `:ls-tree` and `ls-tree`; unknown ops get a friendly error.
- CLJC reader-conditional requires now appear in outline output; grep no
  longer mishandles loose files outside a project.
- `>defn-` (Guardrails) now correctly detected as private.
- `:declares` returned empty — was reading from deps, which excludes declares.
- Namespace derivation for dialect-split source layouts (`.clj`/`.cljs`/`.cljc`).
