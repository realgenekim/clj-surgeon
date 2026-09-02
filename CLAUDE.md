# clj-surgeon

Babashka CLI tool for structural operations on Clojure namespaces.

## Required reading

Before non-trivial feature or refactoring work, read:

- [AGENTS.md](AGENTS.md) for the scoped Linked-Intent Development contract.
- [docs/vision.md](docs/vision.md) for the bookkeeping-versus-judgment boundary,
  plan contracts, and fail-closed design principles.
- [docs/testing-guidelines.md](docs/testing-guidelines.md) for the one-shot
  feature standard and required test layers.
- The applicable plan in [docs/plans/](docs/plans/) when one exists.

When working in this repository, the working-tree [skill.md](skill.md)
supersedes any installed clj-surgeon skill; installed copies are stable
snapshots that may lag the branch.

If a non-trivial feature has no plan, write one in `docs/plans/` before
implementation. A useful plan fixes the observable contract, non-goals,
failure data, exhaustive behavior matrix, real-program evidence, documentation
updates, and verification gates. It is not a chronological coding diary.

## Testing

- Run: `make test`
- For the inner Clojure development loop, first run
  `clj-nrepl-eval --discover-ports` and reuse the project-local nREPL. Reload
  the changed namespaces and run focused test vars or pure semantic probes in
  that warm JVM. Reserve fresh `clojure -M` or `make` test processes for
  milestone gates, because cold runs verify classpath, startup, and isolation
  behavior that a warm JVM can hide.
- If discovery finds no nREPL for the current worktree, start `make nrepl`,
  retain its printed port, and verify `(System/getProperty "user.dir")` before
  loading code. Treat this process as a standalone analysis JVM, not as the
  shared MCP runtime. Never launch the nREPL alias directly: the Make target
  applies the repository's bounded 64 MB initial and 512 MB maximum heap. Use
  `require` with `:reload` after source changes and
  prefer several small pure evaluations over one large probe.
- **Read [docs/testing-guidelines.md](docs/testing-guidelines.md)** before writing tests.
- Core rule: pure functions take data and return data. Test them with literals, not temp files.
- If you need a temp file to test a function, the function needs refactoring, not the test.
- Every field failure gets a named regression test and a faithful fixture or
  source literal that records its provenance.
- Exhaust the pure behavior matrix; use filesystem and subprocess tests only
  for contracts that genuinely require those boundaries.
- A CLI feature is incomplete until help, parsing, dispatch, EDN output,
  nonzero error exits, and the documented invocation are tested.
- A write feature is incomplete until refusal leaves bytes unchanged and a
  successful candidate is reparsed, linted or compiled as appropriate, and
  exercised on a real-program-derived fixture.

## Kent Beck: lower the cost of change first

- Optimize for rapid, safe experimentation. When an intended product or
  benchmark change is cumbersome, fragile, or expensive to repeat, pause and
  make that class of change cheaper before making the change itself.
- Prefer a small reversible ratchet: extract a pure decision, add the missing
  characterization test, shorten the feedback command, improve the fixture or
  receipt, or remove one accidental interface choice. Commit that ratchet
  independently, then run the motivating change through it.
- Do not turn "make it easier" into a speculative framework. The improvement
  must immediately reduce the complete verified time or risk of the real next
  experiment.
- Keep meaning-preservation non-negotiable while hill climbing. Whitespace-only
  presentation drift may be scored separately from semantic correctness.
  Comments, metadata, reader-discard forms, lint directives, and unrelated
  source remain protected unless the declared change explicitly owns them.

### Increase option value while lowering the cost of change

- When uncertainty is high, split a proposed capability into independently
  testable seams with explicit acceptance gates. Prefer pure relations,
  transport adapters, scorers, fixtures, and projections that can advance or
  be rejected without coupling the other options.
- Make experimental commits small, green, and cherry-pickable. Put one
  mechanism or decision in each checkpoint so another branch can reuse the
  earned seam without inheriting the complete experiment.
- Freeze representative field evidence before optimizing. Run competing
  options against the same corpus and retain negative results as receipts, but
  keep losing machinery out of the production path.
- Judge modularity by the option value it creates: parallel experiments,
  cheaper reversal, independent verification, or reuse in the next real task.
  Do not add indirection that merely renames the same coupled decision.
- Prefer the smallest option that removes complete-task actions or risk. If a
  general model can reason from bounded facts, expose those facts and keep
  authority mechanical instead of growing a heuristic policy engine.

## The battlefield and the test doctrine (Gene, 2026-09-02; supersedes the five-times-native extraction mission)

Read [docs/vision.md](docs/vision.md) "The battlefield" first. The 2026-09-02 program
(receipts: `docs/observations/2026-09-02-captains-log-bridge-wall-clock-ideal-program.md`)
measured the shipped per-form editor at 2x native wall with no quality win, declined by the
agent when optional, and closed that square. We compete on four squares only: the gate on the
agent's own patch (build first: `admit_clojure_patch`), one intent across N owners with
tool-side discovery and splice, questions grep answers wrong (`:ls-tree` is the foundation),
and proof before write in the warm JVM. The five-times-native extraction north star and its
binding stops are history, kept in git; the measured winners (`require_change`, `within` +
`from`/`to`, `:extract!`, `:rename-ns!`, `:fix-declares!`) stay.

**Test doctrine, so no feature spends months on a dead square.** A feature is not shipped,
and no speed or quality claim about it may be written anywhere, until ALL of the following
hold; each is a receipt in the captain's log with its commit timestamp:

1. **Native positive control in every cohort.** Every measurement includes "do it without the
   tool" on the same task, same wave, same cores. A benchmark that cannot lose cannot learn.
2. **Variance floor first.** At least six identical runs of the control arm before any
   comparison; a wall claim must clear two sd of that floor, and any quality score inside the
   judges' own floor on identical inputs is reported as noise, not as a result.
3. **Free-choice adoption cohort.** Three or more runs with the tool available but optional
   and the prompt asking for the fastest safe completion. If the agent declines the feature,
   the feature has not shipped, whatever a mandated arm scores.
4. **Attested subject.** Every arm writes a machine-generated attestation before work starts:
   server identity read from the server (healthz, port pid, ready.edn project root and its
   HEAD), prompt hash, worktree commit, model, runner hash; any mismatch refuses the arm. A
   receipt whose evidence source can transform or omit its subject is `:unverified`.
5. **Typed refusal ledger and call-site taxonomy on every run.** Refusals by typed reason;
   every native call classified pre-edit, post-edit, patch, test, other. The mechanism is
   named from these, never from wall alone.
6. **Acceptance is a gate, not a score.** The arm-independent suite decides pass or fail;
   it is never averaged across arms. Add one behavioural assertion per feature; a
   marker-only suite passed a broken button.
7. **Frozen, complete artefacts.** Diffs staged (`git add -A`) and captured against the base
   commit at arm end, with an ended-gate and a completeness gate; worktrees kept to cohort
   end; per-slot and per-group names so repeated arms never overwrite each other.
8. **Timestamps from the clock, never typed.** Headers and receipts take their time from
   `date -u` inside the write; git commit timestamps are the record.
9. **Both judges, blind, with a written rubric ruling for every disputed pattern**, and the
   judges' disagreement recorded as a finding, never averaged away.
10. **Every fix earns a ratchet in the apparatus** (the runner refuses the class next time),
    and every withdrawn claim is corrected at the mechanism and in every place it was relayed.

Venue is part of t: anything needing a JVM or more than one arm runs on Anvil, pinned,
attested, with receipts frozen by the runner. Buster and the skiff keep the connector and light
work. Record the method as well as the result in the captain's log: option, ratchet, evidence,
counterfactual, surprise, falsifier, decision, and what becomes cheaper next.

## Architecture

- `forms.clj` — single source of truth for form classification (what is a defn, what is private, etc.)
- `outline.clj` — parse a file into structured form data (line boundaries, names, arglists, requires)
- `analyze.clj` — dependency analysis, topo sort, extraction closures (all pure, takes zippers)
- `core.clj` — CLI dispatch, ls-tree pipeline, formatting

## Benchmark harness architecture

- Treat `bench/run_clean_codex.sh` and `bench/run_clean_claude.sh` as thin
  caller adapters, not independent applications. Before adding logic to either
  script, ask whether a pure data transformation or shared policy can move to
  a tested Babashka namespace and be called by both harnesses.
- Use the strangler pattern. Characterize current behavior, extract one seam,
  route both harnesses through it, and keep the old shell path until parity is
  proved. Do not rewrite a working harness wholesale.
- Keep shell for irreducible process wiring such as environment isolation,
  redirection, signals, and launching the caller. Put task catalogs, fixture
  validation, schedules, scoring, retention policy, summaries, and receipt
  construction in shared Babashka components whenever practical.
- Do not copy a Codex harness feature into the Claude harness or vice versa.
  Extract the common contract first; keep only caller-specific invocation and
  telemetry parsing in the adapters.
- Every extracted seam needs pure self-tests plus the existing boundary
  self-tests on both harnesses. Extraction must preserve or strengthen the
  evidence and retention contracts.

## Key conventions

- Use the hottest capable entrance. Prefer `inspect_clojure` for bounded
  structural reads. For writes, prefer compact `edit_clojure`, then native
  `apply_patch` when it is smaller or clearer. Use heavyweight
  `apply_clojure_changes` only for a prepared semantic basis, an operation
  absent from the compact editor, or gates that must participate in rollback.
  Use the process-starting CLI only when MCP is unavailable, the operation is
  not exposed there, or the CLI itself is under test.
- Do not discover, register, or call the semantic backend directly. For
  cross-file definitions, references, implementations, incoming calls, and
  outgoing calls, use Surgeon `inspect_clojure` preparation. Surgeon owns any
  required semantic escalation and returns the exact source anchors. Do not
  reopen source only to recover an enclosing form that the result already
  names.
- For several known structural questions, prefer the read-only
  `inspect_clojure` MCP tool. One `read_complete=true` result is terminal
  evidence; do not split or repeat the batch.
- When only names, ranges, counts, hashes, or source anchors are needed, set
  `include_source=false`. Omit it when source is needed for judgment or an edit;
  a metadata-only read must not cause a second call for source.
- **Latency discipline during this editor study:** an `inspect_clojure` result's
  compact human summary can omit requested source even though the same call's
  `structuredContent.results` already contains it. Preserve and consume that
  structured result from the first call. When invoking MCP through an
  orchestration wrapper, emit the needed `structuredContent` in that same tool
  turn; never repeat an equivalent inspection merely to make hidden source
  visible. The server read may take only milliseconds while each unnecessary
  model/tool boundary costs many seconds of complete wall time.
- **Compact editing discipline during this editor study:** do not use the
  heavyweight `apply_clojure_changes` API to implement its compact successor
  merely for dogfooding theater. Use `edit_clojure` when one exact guarded
  subtree batch fits its current contract. Otherwise use native `apply_patch`,
  then format and test the changed Clojure files. Dogfood the new interface at
  its public end-to-end boundary; do not pay legacy schema ceremony while
  constructing that boundary. Once the successor is live, prefer it for real
  eligible edits and record any fallback as interface evidence; this
  construction exception must not dissuade use of the new API being built.
- A named-form result includes the exact `source_anchor` needed by Surgeon's
  semantic preparation. Preserve it. For up to four related known Vars, submit
  one ordered `subjects` request. Do not discard exact evidence and restart
  with an unanchored workspace-symbol query.
- For an unanchored fully qualified Var, keep the caller's canonical
  `workspace_root` even when the Var may live in a configured sibling project.
  Do not guess a sibling path. Onboarding publishes source roots; Surgeon
  returns the authoritative workspace and exact fallback evidence.
- If one Var or one related Var set names the change but exact sites are
  unknown, call `inspect_clojure` with `mode=prepare-change`, one concise
  `intent`, and either `subject=namespace/name` or an ordered `subjects` array.
  Copy its `next_call`. Replace every decision `null` with exactly one
  `keep=true` or one replacement form. Call
  `apply_clojure_changes` once. Do not repeat semantic resolution, source reads,
  selectors, counts, hashes, basis IDs, or site IDs.
- When the files, named top-level forms, and desired changes are already known,
  call `edit_clojure` directly with `workspace_root`, required literal `edits`,
  optional computed `programs`, and optional `delete_owners` groups. Address a
  literal inside a named Var with `within.form`; address the `ns` form with
  `within.namespace`. Each delete group names one file and the exact top-level
  owners to remove. Per-edit old subtrees, resolved owner names, and exact
  program expectations are stale-source guards; all items compile against one
  frozen snapshot and commit atomically. Do not preflight-read or add `verify`
  or aggregate counts. Apply the same proportional formatter/linter/test policy
  as the native route; never add a tool call merely because Surgeon performed
  the edit. Escalate to `apply_clojure_changes` only when required gates must
  roll back the transaction or its unique semantic operations are required.
- If semantic preparation does not index a known owner, prepare it with
  project-relative `file` plus exact top-level `form`. This exact-source route
  does not claim a reference surface.
- No private semantic provider has write authority in this repository. Use
  clj-surgeon for structural writes, guarded transactions, and receipts.
- Public pure functions for testable logic: `source-paths-from-config`, `filter-projects-by-hits`, `format-file-text`, `format-ls-tree-text`, `extract-ns-requires`
- Private I/O wrappers delegate immediately to pure functions; for example,
  `extract-source-paths` delegates to `source-paths-from-config`.
- All ops return EDN data, not side effects (except `!`-suffixed ops which write files)
- Errors use a human-readable `:error` string plus a stable keyword
  `:error-type` and structured diagnostic fields.
- The following are CLI fallback conventions. Before native Read, Edit, grep,
  sed, or cat touches an existing Clojure,
  ClojureScript, or CLJC file, load the working-tree skill and use
  clj-surgeon. Native Write remains appropriate for new files; use native
  editing for unsupported prose- or comment-heavy changes. For a file over
  500 lines, use `:ls` first when the relevant form is unknown. When a
  top-level name or containing line is already known, use `:cat` as the first
  source inspection; do not run `:ls` solely as a preflight or reconstruct a
  `sed` range.
- When distinctive text is known but its containing form is not, use
  `:cat :contains` to select that form in one command; do not manufacture
  a line with `rg -n` or print a large outline. Use `rg` for broad cross-file
  discovery. Use `:match-form` for file-wide structural patterns; each named
  match reports reusable `:inside` ownership. Add `:inside` only when the
  parent is known or ambiguity needs narrowing. `:match` accepts one EDN form
  pattern, not a regular expression.
- When sibling syntax identifies a target—a `case` key, `cond` guard, map key,
  or binding name—read it with `:xray :expr "(-> (form 'transition) (match
  :finish) right)"`. For a known literal replacement, use the same path with
  `:edit`, terminal `replace`, and `:expect` to apply and verify in one call.
  Do not grep a repeated expression and then read its owner merely to recover
  sibling context.
- When a physical line identifies one otherwise unnamed top-level owner, start
  an X-ray or edit path with `(line N)`, then use `match` or navigation to
  select the exact nested syntax. The line can be inside the form or in its
  attached comment. Blank gaps and overlapping owners must refuse. Prefer
  `(form 'NAME)` when semantic identity is known.
- When the adjacent forms are themselves the intended object, use
  `[[:form transition] [:find :finish] [:span 2]]`. A terminal
  `[:replace-span :finish (assoc state :status :complete)]` requires equal
  replacement arity and preserves every comment and whitespace byte between
  the peers; apply its plan separately with `:replace-subform!`.
- When a task asks for every pair in a `case`, `cond`, map, binding vector, or
  other flat sibling run, start at the first member and use
  `[:partition-all 2]`. Do not read the owner and manually count children or
  issue one query per key. The final shorter span is explicit and must be
  interpreted by the caller. clj-surgeon does not infer a default or error.
- When repeated nested heads make that first outer member unknown, promote the
  heads to their owner nodes and filter contained owners with
  `[:find cond] :up :outermost` before navigating to the children. Placement is
  significant: use `:up :outermost`, not `:outermost :up`. When the first outer
  guard is already known, anchor there directly because that route is shorter.
- Use `:xray :expr` as the primary structural read surface. Write one pure Clojure
  path, such as `(-> (form 'transition) (match :finish) right)`, to
  return literal source evidence. End it with `analyze`; its function always
  receives a vector in match order. Add `expect-count` before `analyze` when
  cardinality must be exact; refusal occurs before the function runs and does
  not change its input type. The computed `:value` has compact hash evidence;
  literal paths retain full source. X-ray never writes source or a plan. For
  a selected `def`, use `initializer` to select its right-hand side without
  evaluating it. Computed X-ray shallowly normalizes a selected map literal or
  top-level `hash-map`/`array-map` syntax to one map view; nested constructor
  syntax and exact evidence remain source-shaped. The SCI sandbox limits
  capabilities but does not prove termination, so analyzers must perform
  bounded work. For CLJC, pass `:clj` or `:cljs` to `form`.
- When a replacement is computed or its before-state is not declared, generate
  a replacement plan in a standalone shell command, or use
  `transform_clojure` to avoid shell quoting. The MCP route accepts one SCI path
  ending in `transform`, an exact match count, and an explicit changed-character
  budget. It lowers every selected leaf to a separate frozen structural address;
  preview is the default. Use `commit=true` only for an already-decided bounded
  relation over narrow selections. One-shot commit refuses a selected subtree
  that contains a comment. For the CLI route, observe and review the plan before
  running a separate apply command; never chain planning and application. When
  the intended relationship, literal replacement, and exact
  before-state are known, one `:edit` call with `:expect` may be the first
  source-bearing action. Omit `:plan-out` unless an audit artifact must be
  retained. Read first only when the choice requires a separate judgment. A
  successful verified apply receipt proves exact replay,
  read-back hash, atomic write, and whole-file parse; do not reread the edited
  or neighboring forms solely to reproduce that evidence. Review an aggregate
  Git diff only when task context already establishes a worktree or explicitly
  requests it; do not probe `.git` merely to repeat edit-level evidence.
- A literal `replace` or `replace-span` written inline in `:expr` preserves its
  exact source spelling, including `#()`, comments, commas, metadata, and
  multiline layout. Keep the literal at the terminal builder when spelling
  matters. A computed replacement and the `:query` surface contain only data
  and use canonical printing.
- Format changed Clojure files before linting or testing. Use the repository's
  formatter when configured; otherwise run
  `npx @chrisoakman/standard-clojure-style fix <changed-files>`.
- Do not declare a feature complete from a green legacy suite alone. Show the
  new tests that fail before the implementation, pass afterward, and cover the
  real invocation that motivated the work.

## Live MCP development

`make mcp-start` starts the shared local stack:

```text
clojure-lsp <-> cclsp http://127.0.0.1:7890/mcp
                         |
                         v
              clj-surgeon http://127.0.0.1:7888/mcp
```

- Join a workspace with `clj-surgeon up [WORKSPACE]`; do not create a new
  server pair for each repository. The command is idempotent and keeps older
  Make onboarding targets only as compatibility aliases.
- For a stale MCP session, missing tools after onboarding, or false-green
  health, run `clj-surgeon recover [WORKSPACE]` once. Continue only after its
  real semantic and guarded-write proof returns `:terminal-state :recovered`.
  On failure, execute the returned redacted `report-command` once and use the
  named fallback. Never loop on recovery or restart healthy shared services.
- For a non-default workspace, pass its canonical absolute `workspace_root`
  to both MCP tools. Preserve `workspace_root` from a prepared `next_call`.
  Workspace routing is request data, not MCP server identity.
- A direct `changes` item uses `id`, `files`, exactly one of `forms` or
  `owner`, `expect`, and exactly one action: `replace`, `delete`, `insert_before`,
  `insert_after`, `rename_binding`, or `assoc_entry`. Replacement, insertion,
  and `assoc_entry` also require one complete `find` form. Sibling insertion preserves the existing
  whitespace gap and refuses when that gap contains comments or detached
  source. A binding pair or map entry is sibling syntax; target its complete
  value form instead of submitting the pair as a form prefix.
- To address one multimethod implementation, put a typed owner in `forms`:
  `{:kind "defmethod" :name "render" :dispatch ":card"}`. Never recover it
  from a line number or replace every same-named `defmethod`.
- Managed writes format staged future bytes before commit. A configured hot
  profile reloads the declared namespaces and runs exact law Vars in the
  repository's application nREPL; hot failure rolls back source and reloads the
  originals.
- A configured cold profile returns `verification_complete=false` with one
  `inspect_clojure` `next_call`. Continue useful work, then copy that call once.
  Its status carries the original `undo_receipt` and `receipt_hash`. Do not
  poll, rerun the mutation, or treat the cold suite as a late rollback.
- To delete two or more known named owners, use one direct change with `forms`
  and `delete=true`; do not create marker forms, wait for cclsp, or use native
  cleanup.
- Use `rename_binding` to preserve an external `:keys` keyword while renaming
  one resolved local binding per named owner. Use `assoc_entry` to preserve
  comments in logically equal maps. Add `inside` to select one semantic
  ancestor when equal maps occur in the same owner.

- cclsp uses the pinned Bun under `../cclsp-structural-results/node_modules`.
  Its launchd job runs `bun --watch`. TypeScript changes load under the same
  URL. The managed launcher must pass its complete shell PATH because
  clojure-lsp invokes the separate `clojure` executable for classpath discovery.
  Use cclsp `inspect_runtime` before logs or process inspection; scope it to one
  workspace or request. Run `make cclsp-status` only for provider health.
- Interactive semantic requests have a 10-second timeout; cold initialization
  has a separate 45-second timeout. Exact source anchors call references at the
  proven owner token and must make zero document-symbol requests. Runtime state
  reports the LSP session, child PID, outstanding calls, queue, recoveries, and
  initialization error. `/healthz` refuses stale stateful runtime generations;
  a root-scoped `restart_server` can initialize a configured workspace whose
  old child already exited. Use the JSONL flight recorder only when that bounded
  state is insufficient. Do not restart the shared parent or unrelated children.
- clj-surgeon runs an embedded nREPL. For interactive probes, prefer the
  persistent Clojure MCP `clojure_eval` tool with that port. It avoids shell
  quoting and returns one structured result while retaining the same live JVM
  and definitions. Use `clj-nrepl-eval` for Make automation or when the MCP
  tool is unavailable. Confirm that the port belongs to the live MCP JVM before
  loading code:

  ```bash
  PORT=$(cat ~/.local/state/clj-surgeon/mcp/nrepl-port)
  lsof -nP -iTCP:$PORT -sTCP:LISTEN
  clj-nrepl-eval --port "$PORT" \
    "(require 'clj-surgeon.mcp-inspect-tool :reload)"
  ```

  In an agent session, the equivalent preferred probe is:

  ```text
  clojure_eval(port=PORT,
    code="(require 'clj-surgeon.mcp-inspect-tool :reload)")
  ```

- Handler Vars are dereferenced for each request. Run `make mcp-reload` after a
  handler, schema, description, annotation, or catalog change. The command runs
  the focused gate, reloads the namespaces, synchronizes the live registry, and
  reports contract hashes. It does not restart port 7888.
- The server advertises `tools.listChanged=true` and emits
  `notifications/tools/list_changed`. A supporting client re-lists the tool
  catalog on the same connection. The current Codex client can keep its
  model-visible schema text for the life of a turn. Start a new Codex session
  only when that cached schema prevents the next call. Do not restart the MCP
  server for a Clojure or tool-contract change.
- `/healthz` is a functional readiness check: it returns success only when the
  shared tool runtime and live tool registry are both ready. After reload work,
  verify one real `inspect_clojure` request as the final authority.
- `make mcp-status` verifies both loopback services, the launchd job, and the
  Codex registration.
- Run `make mcp-test` after each live patch. Run `make test` before completion.
