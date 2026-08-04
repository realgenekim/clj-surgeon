# Changelog

All notable changes to clj-surgeon are documented here.

Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). No version
tags yet — everything accumulates under Unreleased until a baseline release
is cut.

## [Unreleased]

### Added

- `:xray`, a read-only pure Clojure analysis over structurally selected values.
  A plain path such as `(form 'transition)` returns literal source;
  `(inspect path :one pure-function)` refuses zero or many and receives one
  value; `(inspect path :all pure-function)` receives a selection vector.
  Computation returns `:value` with compact hash evidence.
  Former read spellings remain compatibility inputs while this unified surface
  is measured. Named selection now sees `#?`
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
