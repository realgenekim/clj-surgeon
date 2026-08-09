# clj-surgeon

clj-surgeon is a Babashka CLI for structural reads and refactors in Clojure,
ClojureScript, and CLJC files. It parses source with
[rewrite-clj](https://github.com/clj-commons/rewrite-clj) and returns EDN.
Claude Code and Codex skills teach coding agents the shortest safe commands.

Use clj-surgeon to:

- inspect a namespace without reading the complete file
- select one top-level form by name, line, or distinctive text
- search and compute over nested Clojure forms
- materialize one guarded multi-file edit plan as one reversible transaction
- create a reviewed, hash-bound replacement plan and apply it separately
- move forms with explicit dependency handling
- reorder definitions to remove unnecessary `(declare ...)` forms
- rename namespaces or extract forms to new files
- inspect or transform reader-conditional code

The project began after a coding agent spent 45 minutes refactoring a
5,000-line `views.clj` file. The agent repeatedly read source to find, move, and
order forms. The initial four-hour prototype had 13 operations and about 1,500
lines. The current implementation is larger and requires Babashka and
clj-kondo. Those counts describe the prototype, not the current product.

## A structural editor for agents

Clojure source already contains named forms, nested expressions, dependency
edges, and reader conditionals. A text-only workflow discards that structure.
It searches for text, recovers a line number, prints a range, patches syntax,
and rereads the file.

clj-surgeon gives coding agents a composable structural interface. `:ls`
inventories a namespace. `:cat` returns exact selected forms from one or more
files. `:xray` reads or computes over a Clojure path.

`:match-form` finds syntax instead of textual lookalikes. Dependency operations
expose the graph. `:change!` compiles a complete exact edit plan into one
verified transaction. Computed single edits retain separate planning and
application.

```text
ls / cat / xray / match-form / deps  →  intent  →  change!  →  receipt
                                      computed edit  →  plan  →  apply
```

For resolved definitions, references, implementations, and call hierarchy,
search the deferred MCP catalog for `mcp__cclsp__*` before falling back to source. Use
clj-surgeon for concrete syntax, structural computation, guarded writes, and
receipts. cclsp supplies semantic evidence. It does not have write authority
in this workflow. [The Clojure Agent Tool Stack](docs/architecture-stack.md)
documents the clj-kondo, clojure-lsp, cclsp, and clj-surgeon runtimes and
authority boundaries.

Run `clj-surgeon :op :help` for the complete caller surface. Unknown operations
list the preferred names, not compatibility spellings. When an invalid call
still identifies one safe intent, its EDN includes executable `:command-args`
with the preferred operation and canonical argument names. A value that looks
like a text regular expression instead returns a bounded text-search remedy.
Execute the returned remedy instead of guessing another spelling.

When all exact before-forms, after-forms, scopes, and counts are known,
`:change!` applies the complete plan in one call. It commits all files as one
failure-atomic unit and saves a hash-fenced inverse receipt. Use `:change` to
review the combined plan without writing. Use `:undo-change!` only while every
forward result hash still matches.

A computed single write first produces a hash-bound plan and exact diff. A
later command applies only that reviewed artifact. A literal single write with
`:expect` applies in one call because the caller declared the exact
before-state. All write routes return read-back-hash and whole-file-parse
evidence.

The design target is one command for each judgment boundary:

```text
one complete declared intent  →  one verified transaction
```

Each command owns its mechanical work: parsing, selection, ordering, ambiguity
detection, hashing, atomic writing, and verification. The agent decides intent
and scope, reviews the plan, and consents to the change. If the tool cannot
select safely, it refuses with bounded evidence and executable remedies. It
does not silently widen scope, choose an ambiguous match, or move additional
code without consent.

When one target relationship and replacement are explicit, an `:edit`
operation may be the first source-bearing call. Use `:expect` to apply a known
literal replacement, or emit a plan for review. When several exact edits are
already known, put them in one `:change!` spec. When intent depends on source
discovery, run `:xray` first.

This is the project's Bitter Lesson boundary. clj-surgeon adds general
structural primitives and trustworthy feedback. It does not add special cases
that try to infer the caller's intent.

The product standard is behavioral. Give a clean agent a real task and inspect
every command. A transcript exposes a product defect when the agent:

- uses `rg` only to obtain a line number
- rereads evidence that an apply receipt already proves
- guesses a documented invocation
- calls help for a route that the skill should make clear

After a defect, update the CLI, help, skill, or contract. Then rerun the task in
a fresh context.

One clean-context edit replication reduced the workflow from 10 shell calls and
157,481 cumulative input tokens to four calls and 77,421 tokens. The four calls
read the skill, selected with `:cat :contains`, planned, and applied. The edit
was exact, and the apply returned a verified receipt.

An adjacent single-pair replication compared the previous paved road with the
structural query pipeline. Both routes made the exact edit. Both planned and
applied separately, and both avoided textual fallback.

The previous route used four shell calls,
77,320 cumulative input tokens, and 1,602 source-output bytes. The structural query used
three calls, 63,514 input tokens, and 1,380 source-output bytes. Uncached input
fell from 17,160 to 15,386. Wall time increased from 33.3 to 42.3 seconds, so
this result supports fewer calls and less context, not a speed claim.

The target state is simple: agents should rarely need line-number choreography
or text surgery for structural Clojure work.

The experiments, wrong turns, and clean-agent transcripts behind this vision
are preserved in the
[captain's log](docs/observations/2026-08-02-captains-log-the-file-became-a-structural-shell.md).

## Measured performance

The original planning observation compared clj-surgeon outlines with Explore
agents that read five files containing about 5,000 lines:

- clj-surgeon used about 1,000 tokens. The Explore agents used about 150,000
  tokens for similar information.
- clj-surgeon returned in milliseconds. The Explore agents took about
  100 seconds.

The files were `command_center.clj` (821 lines), `routes.clj` (2,894 lines),
`state.clj` (614 lines), `processes.clj` (508 lines), and
`process_handlers.clj` (198 lines). This dated observation was not a controlled
comparison against current models.

A later clean-context benchmark compared commit `cc0f306`, commit `477dca9`,
and a native control that used ordinary source tools without clj-surgeon. Each
version ran each task four times. Efficiency values are medians of runs that
passed the task's correctness gate.

| Task | Version | Correct | Wall | Calls | Input tokens | Source output |
|---|---|---:|---:|---:|---:|---:|
| Named form | `cc0f306` | 4/4 | 28.792 s | 4 | 72,000 | 6,720 B |
|  | `477dca9` | 4/4 | 22.709 s | 2 | 43,971 | 1,461 B |
|  | Native control | 4/4 | 23.949 s | 2 | 41,379 | 2,322 B |
| Semantic form | `cc0f306` | 4/4 | 39.444 s | 5 | 82,806 | 7,883 B |
|  | `477dca9` | 4/4 | 28.384 s | 2 | 44,016 | 1,531 B |
|  | Native control | 4/4 | 27.347 s | 3 | 56,778 | 4,244 B |
| Structural find | `cc0f306` | 4/4 | 35.959 s | 4 | 76,213 | 663 B |
|  | `477dca9` | 4/4 | 24.222 s | 2 | 43,672 | 596 B |
|  | Native control | 4/4 | 40.627 s | 3 | 56,285 | 844 B |
| Case edit | `cc0f306` | 4/4 exact bytes | 53.502 s | 8 | 176,294 | 37,443 B |
|  | `477dca9` | 4/4 exact bytes | 52.832 s | 8 | 116,114 | 10,542 B |
|  | Native control | 0/4 exact bytes | — | — | — | — |

All native case-edit runs changed the requested token but deleted an unrelated
trailing blank byte. Because no native run passed the exact-byte correctness
gate, the table does not report native efficiency medians for that task.

The first public transaction dogfood was not a controlled agent benchmark. It
measured the mechanism before the clean-context comparison. One EDN spec
contained three heterogeneous intents and produced four exact edits across two
copied Clojure files.

Forward apply, durable receipt publication, and exact undo completed in
approximately 0.5 seconds in one Babashka process. The receipt was 3,466 bytes,
and both final file hashes equaled their starting hashes. This proved bounded
mechanism cost and reversibility.

A later controlled benchmark measured the complete agent task on the frozen
six-edit, two-file decision. Each lane ran four correct replicates. Efficiency
medians include only runs whose final bytes matched the accepted files.

| Route | Correct | MCP adopted | Median wall | Median input | Shell calls |
|---|---:|---:|---:|---:|---:|
| Assisted `apply_clojure_changes` | 4 / 4 | 4 / 4 | **24.530 s** | **44,020** | **0** |
| MCP + one project rule | 4 / 4 | 4 / 4 | **27.432 s** | 44,215 | **0** |
| Current CLI + skill | 4 / 4 | — | 36.396 s | 64,842 | 3 |
| MCP available, no routing rule | 4 / 4 | 0 / 4 | 40.618 s | 74,865 | 2 |
| Native control | 4 / 4 | — | 43.190 s | 74,872 | 4 |

The assisted MCP route was 18.660 seconds faster than native, a 43.2%
reduction. It used one verified mutation call, zero source reads, and zero
failed mutations in every run. Tool metadata alone did not cause adoption. A
one-sentence project `AGENTS.md` rule changed adoption from 0 / 4 to 4 / 4
without loading the skill. See the
[complete Captain's Log](docs/observations/2026-08-07-captains-log-one-call-crossed-the-double-digit-gate.md).

A later rerun removed one ambiguous `owner` recovery round from that same
six-edit task. Every one-shot MCP run was correct; four of five native controls
were valid and correct.

| Route | Correct efficiency runs | Median wall | Tool actions | Source bytes surfaced |
|---|---:|---:|---:|---:|
| One-shot Surgeon MCP | 5 / 5 | **27.976 s** | **1.0** | **0** |
| Fresh native control | 4 / 5 | 68.932 s | 3.5 | 3,577 |

In this complete supplied-decision stratum, native took 2.46 times as long;
one-shot MCP reduced median wall time by 59.4%. The later exploratory lane also
crossed over: 62.876 seconds for Surgeon versus 81.730 seconds for native, a
23.1% reduction. One supplied nested edit showed a smaller 19.3% reduction.
The claims apply to a shared hot service and these tested Clojure change
strata. See the
[end-to-end timing log](docs/observations/2026-08-07-captains-log-end-to-end-structural-transaction-timings.md)
and the current
[last-night synthesis](docs/observations/2026-08-08-captains-log-last-nights-hill-climb.md).

A later paired read benchmark asked fresh Codex callers 45 exact behavior
questions about 45 named forms in three frozen files. Both answers passed all
45 checks. Exact-source EDN made Surgeon 43.46 seconds slower because the
result exceeded the visible transcript boundary and caused recovery reads.
The explicit `:format :semantic` view reduced the transaction from 68,339 to
47,814 characters. On the identical rerace, Surgeon finished in 102.48 seconds
versus 108.39 seconds for native generated extractors: **5.91 seconds (5.5%)
faster**, with 27.8% less command output. This is one paired roll, not a stable
median. The complete negative and positive stages are preserved in the
[transaction/read Captain's Log](docs/observations/2026-08-06-captains-log-the-transaction-landed-but-reading-still-paid-per-question.md).

A later single-replication experiment tested a local cclsp fork as the
cross-file semantic sensor. These are causal pilot results, not stable
medians:

| Relationship task | Before usable contract | Enriched cclsp | Native | Outcome |
|---|---:|---:|---:|---|
| Resolve three reference owners | 60.762 s | 24.252 s | 46.686 s | Enriched cclsp was correct and 48% faster than the correct native control |
| List direct outgoing calls | 84.799 s, incorrect | 20.049 s, correct | 60.777 s, incorrect | Enriched cclsp was the only correct route and saved at least 40.728 s |

The gains came from removing recovery rounds. Read-only MCP annotations made
the calls usable without approval pauses. Agent-native argument aliases
removed guessed-field failures. Owner-enriched references removed
line-to-form lookup. Each result is one replication, not a stable median. See the
[semantic-sensor Captain's Log](docs/observations/2026-08-07-captains-log-rent-the-graph-keep-the-transaction.md).

The first clean proof-carrying change-buffer task started with only a Var and a
return-contract goal. After one negative design stage, `inspect_clojure`
returned the complete definition and complete named owner of every semantic
reference. The caller then used the returned basis in one
`apply_clojure_changes` call.

| Route | Correct | Wall | Source reads after skill | Mutation calls |
|---|---:|---:|---:|---:|
| Native source tools | yes | 54.13 s | 4 | 1 |
| Proof-carrying basis | yes | **31.00 s** | **0** | **1** |

The basis route was 23.13 seconds faster, a 42.7% wall reduction and 1.75x
speedup. It used exactly two MCP calls: prepare, then apply and verify. This is
one paired probe, not a replicated median or a 3x claim. See the
[transaction Captain's Log](docs/observations/2026-08-07-captains-log-the-decision-became-a-transaction.md).

## Production examples

clj-surgeon renamed this project from `ns-surgeon` to `clj-surgeon` in less
than one second. It changed 10 files, moved 10 files, and updated namespace
declarations and `:require` entries structurally. It preserved aliases and
string literals.

**One-command declare cleanup:** A 2768-line production namespace with 7 forward declares. One command:

```
$ clj-surgeon :op :fix-declares! :file src/writer/state.clj

{:summary {:moves 7, :declares-deleted 5, :skipped 2}}

$ make runtests-once
337 tests, 1056 assertions, 0 failures.
```

The command removed five declarations. Three required direct moves, and two
required their leaf dependencies. It skipped two moves with non-leaf dependency
chains. The [first-use observation](docs/observations/2026-03-28-first-real-use.md)
records the failure pattern that led to `:fix-declares`.

## Install

```bash
git clone https://github.com/realgenekim/clj-surgeon.git
cd clj-surgeon
make install    # → CLI plus Codex and Claude skills
```

`make install` creates content-addressed, read-only copies of the CLI runtime
and the canonical `skills/clj-surgeon` package under
`${INSTALL_ROOT:-$HOME/.local/share/clj-surgeon}`. The CLI launcher and the
Codex and Claude skill entrances point to those stable copies, never to the
active checkout. Switching this repository's branch therefore cannot silently
change an installed version.

The default entrances are `~/bin/clj-surgeon`,
`${CODEX_HOME:-$HOME/.codex}/skills/clj-surgeon`, and
`${CLAUDE_HOME:-$HOME/.claude}/skills/clj-surgeon`. Each gets a neighboring
`.receipt.edn` file that records the source commit, source hash, destination,
and immutable package path. Run `make install-cli`,
`make install-codex-skill`, or `make install-claude-skill` to install one
surface. Installation refuses to replace unrelated files or skill directories.

To put the CLI somewhere else, pass its complete file path as `CLI_DEST`. The
installer creates the parent directory and handles paths containing spaces:

```bash
make install-cli CLI_DEST="$HOME/.local/bin/clj-surgeon"
```

`CLI_DEST` affects only the CLI. Set `CODEX_HOME`, `CLAUDE_HOME`, and
`INSTALL_ROOT` independently when the skill homes or stable package root need
non-default locations:

```bash
make install \
  CLI_DEST="$HOME/.local/bin/clj-surgeon" \
  CODEX_HOME="$HOME/.config/codex" \
  CLAUDE_HOME="$HOME/.config/claude" \
  INSTALL_ROOT="$HOME/.local/share/clj-surgeon"
```

For tool development only, `make install-dev` deliberately creates a
branch-coupled CLI launcher and direct skill links to the current checkout.
Its output and receipts use `:mode :development-link`. Run stable
`make install` again before benchmarks or release validation.

### Requirements

Two binaries must be on `PATH`:

- **[babashka](https://babashka.org/)** (`bb`) — the runtime. rewrite-clj and cheshire are built in.
- **[clj-kondo](https://github.com/clj-kondo/clj-kondo)** — used by `:ls`, `:fix-declares!`, and any op that needs forward-reference detection. Without it those ops fail.

No-sudo install (e.g. inside the Claude Code sandbox, where `sudo` is blocked):

```bash
mkdir -p ~/bin   # ~/.profile already prepends ~/bin to PATH on Ubuntu/Debian
bash <(curl -s https://raw.githubusercontent.com/babashka/babashka/master/install) --dir ~/bin
bash <(curl -s https://raw.githubusercontent.com/clj-kondo/clj-kondo/master/script/install-clj-kondo) --dir ~/bin
```

`make install` will warn (but not fail) if either binary is missing.

What changed since you installed? See [CHANGELOG.md](CHANGELOG.md).

### Experimental Codex MCP entrance

Keep this branch-local while the MCP contract is under evaluation. The
development installer points the CLI and both skills at the current checkout,
starts the hot clj-surgeon and cclsp services on loopback, and registers exactly
two clj-surgeon tools with Codex:

- `inspect_clojure` for read-only structural batches and proof-carrying change
  preparation;
- `apply_clojure_changes` for guarded direct or basis-backed mutation.

```bash
make install-mcp-codex-dev

# Join any repository to the same hot clj-surgeon and cclsp processes.
clj-surgeon up /absolute/repository
```

`clj-surgeon up [WORKSPACE]` defaults to the current directory. It discovers
the effective clj-kondo configuration, registers the canonical workspace with
the shared cclsp provider, starts the shared loopback services when needed, and
installs the two MCP entries in the workspace's `.codex/config.toml`. It
preserves unrelated TOML, migrates older per-repository tables, validates the
result, and is idempotent. `make workspace-mcp-onboard WORKSPACE=/repo` remains
a compatibility alias.

The managed cclsp service inherits the invoking shell's complete PATH. This is
part of semantic correctness: clojure-lsp is a native executable, but it still
invokes the separate `clojure` command to discover a project's classpath.

If an already-onboarded agent reports `invalid-mcp-session`, missing tools, or
a green health check followed by a failed structural call, use the bounded
reset button once:

```bash
clj-surgeon recover /absolute/repository
```

Success means more than healthy processes. The command opens fresh MCP
sessions, checks the actual tool catalog, resolves one hash-bound Clojure Var
through cclsp, performs one temporary failure-atomic write with read-back
verification, removes its fixture, and returns `:terminal-state :recovered`
with `:next-action :none`. It does not restart healthy shared services.

Failure returns one terminal state, a redacted receipt path, and one executable
report command. Run that command once on this development machine:

```bash
clj-surgeon report-failure --receipt ~/.local/state/clj-surgeon/recovery/last-failure.edn
```

The reporter creates or updates one local Bead by stable failure fingerprint.
It never uploads or retains source, prompts, URLs, or workspace paths. When a
local `.beads` database is unavailable, it returns the same safe issue draft as
data and performs no write. Do not loop on `up` or `recover`.

Start a new Codex session in the target repository only when onboarding changed
its `.codex/config.toml`. Do not start another MCP server for the repository.
For a workspace other than the server default, send its canonical absolute
`workspace_root` on `inspect_clojure` and `apply_clojure_changes`. A prepared
`next_call` already carries the same root; preserve it unchanged.

A qualifying edit can then be one native tool call. Send all known Clojure
replacements, scopes, and cardinality guards to `apply_clojure_changes`. A
successful `verification_complete=true` receipt is terminal evidence, so do
not reread or diff the files afterward. One server remains hot across projects
and Codex sessions. Each request is confined to its selected canonical
`workspace_root`; a relative path cannot escape that root. On macOS, a local
`launchd` job keeps the service alive across terminal and Codex restarts. It
listens only on `127.0.0.1:7888` and records full local telemetry under
`~/.local/state/clj-surgeon/mcp`. The job is session-persistent, not a permanent
login item; rerun the installer after logging out or rebooting.

The direct route uses one closed object per change:

```json
{
  "workspace_root": "/absolute/workspace",
  "changes": [{
    "id": "status",
    "files": ["src/app.clj"],
    "forms": ["render"],
    "find": ":old",
    "replace": ":new",
    "expect": {"matches": 1, "each_form": 1}
  }],
  "expect": {"changes": 1, "edits": 1, "files": 1}
}
```

Use exactly one of `forms` or `owner` and exactly one mutation action in each
change. The actions are `replace`, `insert_before`, `insert_after`,
`rename_binding`, and `assoc_entry`. `find`, a replacement, and every inserted
member must be one complete parseable Clojure form. Insertion preserves the selected sibling's existing whitespace
separator. It refuses when that gap contains comments or detached source;
replace a larger exact span when comment placement is part of the decision.

For example, add two members after one exact set or vector member without
replacing the whole owner:

```json
{
  "workspace_root": "/absolute/workspace",
  "changes": [{
    "id": "add-metrics",
    "files": ["src/metrics.clj"],
    "forms": ["numeric-fields"],
    "find": ":wall-ms",
    "insert_after": [":cached-input-tokens", ":uncached-input-tokens"],
    "expect": {"matches": 1, "each_form": 1, "each_file": 1}
  }],
  "expect": {"changes": 1, "edits": 1, "files": 1}
}
```

Use `rename_binding` when a local name must change but its external `:keys`
keyword must not change. Name every owner. `matches` counts the binding and its
resolved local usages. `each_form: 1` requires one selected binding per owner.

```json
{
  "workspace_root": "/absolute/workspace",
  "changes": [{
    "id": "rename-sort-binding",
    "files": ["src/app.clj"],
    "forms": ["feed-page", "table-page"],
    "rename_binding": {
      "from": "sort-by",
      "to": "sort-field",
      "preserve_external_key": true
    },
    "expect": {"matches": 12, "each_form": 1}
  }],
  "expect": {"changes": 1, "edits": 12, "files": 1}
}
```

Use `assoc_entry` to add one entry to maps that have the same Clojure value but
different comments or layout. Existing source trivia remains in place. Add
`inside` when one complete ancestor form must distinguish equivalent maps.

```json
{
  "workspace_root": "/absolute/workspace",
  "changes": [{
    "id": "add-contract-field",
    "files": ["test/app_test.clj"],
    "forms": ["contract-test"],
    "find": "{:a 1 :b 2}",
    "inside": "(is (= {:a 1 :b 2} actual))",
    "assoc_entry": {"key": ":status", "value": ":ready"},
    "expect": {"matches": 1}
  }],
  "expect": {"changes": 1, "edits": 1, "files": 1}
}
```

When one Var or one related Var set names the subject but the exact edit sites
are unknown, compile the decision in two calls:

```text
inspect_clojure prepare-change -> fill keep/replace/delete holes -> apply_clojure_changes
```

```json
{
  "mode": "prepare-change",
  "subjects": [
    "clj-surgeon.mcp-contract/normalize-success-receipt",
    "clj-surgeon.mcp-tool/success-summary"
  ],
  "intent": "Add context to the terminal receipt without changing callers"
}
```

If the owner is known but cclsp does not index the file, use the exact-source
route instead:

```json
{
  "mode": "prepare-change",
  "file": "bench/summarize_clean_codex.clj",
  "form": "numeric-fields",
  "intent": "Add one benchmark metric",
  "label": "metrics"
}
```

This route proves the exact file hash, unique named owner, and structural
address. It intentionally reports zero references because it does not claim
language-server coverage.

The response contains a compact, complete `surface` vector for every definition
and reference. With `scope=definition`, only the definition decision carries
source; open selected retained site IDs when more owner source is required.
With `scope=surface`, every site is a decision and carries its complete named
owner. The response also contains a complete `next_call`. Preserve its
`workspace_root`, basis, site IDs, and verification profile. Replace every
`null` with `{"keep":true}`, `{"replace":"ONE FORM"}`,
`{"delete":true}`, or one compact nested edit. A whole-site delete removes the
prepared owner and its contiguous leading comment block; use it for obsolete
definitions, call sites, tests, and `declare` forms. Submit that basis request
to `apply_clojure_changes` once.

Do not reconstruct a direct `changes` request.
Apply uses the retained source hashes and zipper paths. It does not repeat
semantic resolution or selection. Omit `verify` from prepare unless the user
explicitly requests the full repository suite; the default is `fast`.

The basis is process-local and expires after one hour. The server retains at
most 32 bases. Prepare refuses before publishing a basis when the decision set
exceeds 24 sites, 32 KiB of visible decision source, or 4 MiB of retained
source. `scope=definition` can still report a larger compact proof surface
because references do not become decisions or carry visible source.
The `fast` profile runs clj-kondo and Standard Clojure Style on changed files.
The `full` profile runs `make test`. Verification failure restores the original
files before it returns.

cclsp binds every definition and reference to one LSP session, a canonical
project-relative file, an exact range, and the SHA-256 of the synchronized
source bytes. clj-surgeon independently reads and hashes those files before it
stores a basis. A language-server owner must match the exact-source owner. For
a version-3 ownerless reference inside a project macro, clj-surgeon must derive
one exact-source owner. Missing evidence, mixed sessions, stale bytes, owner
ambiguity, and owner disagreement refuse before basis storage.

An unanchored fully qualified Var may live in another workspace already joined
to the shared stack. Pass the caller's canonical `workspace_root`; do not use
`../` paths or guess the owning project. `clj-surgeon up` publishes each
workspace's confined Clojure source roots. cclsp converts the namespace to its
canonical file suffix, queries only workspaces that contain that file, and
returns both `requested_workspace_root` and `authoritative_workspace_root`.
`workspace_search.selection` records the shortlist and every queried outcome.
If any workspace lacks source-root metadata, cclsp falls back to all configured
Clojure workspaces and preserves typed zero, ambiguous, error, and timeout
results instead of claiming uniqueness.

Every named form returned by `inspect_clojure` includes a `source_anchor` with
the exact file, SHA-256, owner, and zero-based range. Copy that object into
cclsp `resolve_var_surface`; do not restart discovery with an unanchored
workspace-symbol query. For one architectural question involving up to four
known Vars, call `resolve_var_surfaces` once with the ordered anchored subjects.
It returns every complete surface or typed refusal in the same order.

An inspect call declares all knowable reads in one `requests` vector. The four
operation variants are `forms`, `outline`, `match`, and `xray`; every variant
has a closed JSON schema and paths are confined to project-relative `.clj`,
`.cljs`, and `.cljc` files. Distinct canonical files are read once, results
retain request order and SHA-256 snapshot hashes, and any validation, parse,
cardinality, or output-limit failure refuses the complete batch. Full evidence
is in `structuredContent`; the bounded text result is an ordinary transcript
summary. The read tool is annotated read-only, non-destructive, idempotent, and
closed-world. This experiment adds no resources, prompts, shell access,
unrestricted evaluation, or custom MCP UI.

```json
{
  "requests": [
    {
      "id": "summary-fields",
      "operation": "forms",
      "file": "bench/summarize_clean_codex.clj",
      "forms": ["numeric-fields", "boolean-fields"],
      "expect": {"forms": 2}
    }
  ],
  "expect": {"requests": 1, "files": 1}
}
```

The first counterbalanced read portfolio produced 4/4 correct one-call MCP
runs with zero shell and failed calls. Its 27.97-second median was 13.8% lower
than the CLI structural route's 32.44 seconds, not the hypothesized 2× and not
the 30% keep threshold. Treat the tool as experimental and preserve that
negative result when evaluating the next iteration.

The first self-hosted proof-carrying change resolved one definition and two
call sites in 0.45 seconds. Its original `fast` profile accidentally ran the
complete MCP suite and took 45.65 seconds. Changed-file verification reduced
that gate to 2.69 seconds: 0.19 seconds for clj-kondo and 2.50 seconds for the
style check. This is component evidence, not yet a 3x clean-agent result.

For an isolated evaluation, start the server with
`BENCH_MCP_PORT=7889 make benchmark-inspect-mcp`; the harness creates a fresh
temporary Codex home per run and writes only this registration there:

```toml
[mcp_servers.clj-surgeon]
url = "http://127.0.0.1:7889/mcp"
required = true
enabled_tools = ["inspect_clojure"]
default_tools_approval_mode = "writes"
```

The harness stops its own server on exit. Removing its external result
directory removes the temporary Codex homes; it never replaces the user's
global registration.

Check or remove the experimental entrance with:

```bash
make mcp-status
make uninstall-mcp-codex-dev
```

`make mcp-status` checks both loopback services. cclsp listens on port 7890 and
clj-surgeon listens on port 7888. TypeScript changes reload under the stable
cclsp URL. Run `make mcp-reload` for Clojure handler or tool-contract changes.
The command preserves the MCP PID and URL, synchronizes the live registry, and
reports before/after contract hashes.

`http://127.0.0.1:7888/healthz` is a functional readiness check. It returns
success only when both the shared tool runtime and live tool registry are
ready. A real bounded `inspect_clojure` request remains the authoritative
post-reload probe.

A client that honors
`tools/list_changed` re-lists without reconnecting. The current Codex turn can
cache its model-visible schema text. Start a new session only when that cached
schema prevents a required call. See `CLAUDE.md` for the exact hot/warm/cold
boundary.

cclsp starts one lazy `clojure-lsp` child for each workspace that receives a
semantic request. Shared onboarding configures a 10-second interactive request
timeout and a separate 45-second cold-initialization timeout. Exact Surgeon
source anchors include the owner-token position, so the primary semantic route
calls `textDocument/references` without a redundant `documentSymbol` round.

Use cclsp's read-only `inspect_runtime` MCP tool before reading logs or
inspecting processes. It returns a compact server summary and bounded
structured state for one workspace or semantic request: runtime/config
identity, LSP session, child PID, initialization state, outstanding calls,
queue, recoveries, and the last initialization error. A timed-out semantic
request is a typed refusal, not a reported success. The durable JSONL flight
recorder is
`~/.local/state/clj-surgeon/cclsp/server.log`. Other workspace children and the
shared cclsp parent remain running during a root-scoped recovery.

cclsp health also binds readiness to the stateful runtime generation. If core
LSP source changed after that runtime was constructed, `/healthz` returns 503
with `runtime_current=false` and `restart_required=true`. Tool-only hot reloads
keep the stable URL. `restart_server` can start an exact configured workspace
whose previous LSP child already exited; it reports `previous_exit=not-running`
and waits for replacement initialization before returning success.

`make install` remains the stable copied CLI-and-skills installation. It does
not enable the experimental MCP server.

## Teach coding agents

This repository ships a native project skill at
`.claude/skills/clj-surgeon/SKILL.md`. `make install` copies the same canonical
package to `~/.claude/skills/clj-surgeon` for use outside this checkout. The
root `skill.md` remains a compact legacy entrance for configurations that
explicitly read a Markdown file. A test checks it against the canonical skill,
with only its repository-relative reference links allowed to differ.

The canonical package keeps uncommon move, extraction, dependency, and CLJC
guidance in `references/advanced-operations.md`. It keeps the complete
process-starting CLI manual in `references/cli-fallback.md`. The default skill
is a compact MCP-first contract. It tells agents to inspect the deferred tool
catalog before they start a shell process.

The skill activates before an agent uses native Read, Edit, grep, sed, or cat
on an existing Clojure file. It routes coherent reads to `inspect_clojure`,
semantic graph questions to cclsp, and structural writes to
`apply_clojure_changes`. It retains native Write for new files and native
editing for unsupported transformations. The CLI reference is loaded only
when MCP is unavailable, the operation is not exposed, or the CLI is under
test.

`make install` also installs the same canonical package for Codex at
`${CODEX_HOME:-$HOME/.codex}/skills/clj-surgeon`. Do not maintain separate
vendor copies. Tests require the installed Claude and Codex skill content to
match the canonical package.

## Operations

### Read-only operations

#### `:ls` — See the skeleton of a namespace

```bash
clj-surgeon :op :ls :file src/writer/state.clj
```

Every top-level form with exact line boundaries, types, names, arglists, and forward reference detection. 236 forms in a 2768-line file, returned in ~200ms.

#### `:cat` — Read exact top-level forms from one or more files

Use a name:

```bash
clj-surgeon :op :cat :file src/writer/state.clj :form transition!
```

When several owner names in one file are known, read them from one parsed
snapshot and preserve the requested order:

```bash
clj-surgeon :op :cat :file src/writer/routes.clj \
  :forms '[handle-sync-draft draft-conflict-response]'
```

`:forms` accepts a nonempty EDN vector of up to 50 unique unqualified names.
The batch is all-or-nothing. If any name is invalid, missing, duplicated, or
ambiguous, the command returns compact per-name evidence and no partial source.
Combined source over 65,536 characters also refuses without partial source. Add
one `:platform` to disambiguate the complete CLJC batch.

When known owners span files, send one manifest through stdin instead of one
shell command per file:

```bash
printf '%s\n' \
  '{:reads [{:file "src/app/routes.clj" :forms [route-request conflict-response]} {:file "src/app/events.clj" :forms [publish-conflict! clear-conflict!]}] :expect {:file-count 2 :form-count 4} :limits {:source-chars 65536}}' |
  clj-surgeon :op :cat :spec-file - :format :semantic
```

The manifest preserves file and form order. It reads each distinct physical
file once and returns one complete-file hash per file. `:expect` must declare
the exact file and form counts. The command rejects unknown keys, duplicate
physical paths, count mismatches, failed form selection, and output above the
declared or hard limit. Every refusal returns no partial source. Use
`:spec-file PATH` for a saved manifest. Inline `:spec` remains available for a
small manifest, but stdin avoids shell escaping.

Always attach stdin in the same shell action. Do not invoke `:spec-file -` and
wait to send the manifest later. `:format :semantic` is the compact behavior
and architecture view. It prints canonical Clojure data with file hashes and a
hard output cap. It omits comments and layout, and reader shorthand such as
`#()` may expand. Omit `:format` when comments, layout, exact token spelling, or
other lexical evidence matters; the default EDN result preserves exact source.

Or use a line contained by the form:

```bash
clj-surgeon :op :cat :file src/writer/state.clj :line 1134
```

Quote form names that contain shell metacharacters. The quotes belong to the
shell command, not to the Clojure name:

```bash
clj-surgeon :op :cat :file src/convert.clj :form 'source->target'
clj-surgeon :op :cat :file src/convert.clj :form 'ready?'
```

Without the quotes, a shell can interpret `>` as redirection or `?` as a glob
before clj-surgeon receives the argument.

Or select the one form containing distinctive literal text:

```bash
clj-surgeon :op :cat :file src/writer/state.clj :contains :finish
```

Supply exactly one of `:form`, `:forms`, `:line`, or `:contains`. `:contains`
searches for a case-sensitive literal substring, not a regular expression. It
searches form source, strings, docstrings, and attached comments.

Multiple occurrences in one form succeed. Occurrences in multiple forms refuse
with bounded candidate evidence. CLI values remain literal text, so a search
such as `:finish` does not need an EDN-string workaround. For ambiguous
reader-conditional definitions, add `:platform :clj` or `:platform :cljs`.

Single-form success returns the exact parsed form source, type, optional name,
platforms, line range, attached-comment start, and complete-file source hash.
Same-file batch success returns the records in an ordered `:forms` vector under
one complete-file source hash. Cross-file success returns the ordered file
results under `:files`. A missing or ambiguous selector returns structured EDN
and exits nonzero. The command never chooses the first match.

`:cat` never dumps the complete file.

When a top-level name, containing line, or distinctive text is known, use
`:cat` as the first source inspection instead of reconstructing a `sed` range.
Do not run `:ls`
solely as a preflight. Continue to use `rg` for broad textual discovery,
`:match-form` for nested structural syntax, and bounded text reads for context
that genuinely spans forms or files. Inside one Clojure file, use
`:cat :contains` instead of `rg -n` followed by `:cat :line`. Do not
print a large outline merely to discover the line.

#### `:xray` — Read and compute with one Clojure path

Use `:xray :expr` for literal structural reads and pure computation. A path
without a terminal returns exact source evidence:

```bash
clj-surgeon :op :xray :file src/state.clj \
  :expr "(-> (form 'transition) (match :finish) right)"
```

Use `(line N)` when the enclosing top-level form has no configured name, but a
physical source line identifies it. The line can be the form's opening line,
an interior line, its closing line, or a contiguous comment immediately above
it. Then select the exact nested syntax normally:

```bash
clj-surgeon :op :xray :file src/cache.clj \
  :expr "(-> (line 412) (match '(old-reader account-id)))"
```

`(line N)` selects the owner, not the nested leaf. A blank gap refuses with
`:line-not-in-form`. A physical line shared by reader-conditional owners
refuses with `:ambiguous-form`. Prefer `(form 'NAME)` when the semantic name is
known. Use `(line N)` as a precise physical locator for otherwise unnamed
top-level syntax.

End a literal path with `expect-count` to return exact source only when the
cardinality matches:

```bash
clj-surgeon :op :xray :file src/policy.clj \
  :expr "(-> (form 'audit-report) initializer (expect-count 1))"
```

End the same path with `analyze` to derive an answer from the ordered selection
vector. Put `expect-count` before it when cardinality must be exact:

```bash
clj-surgeon :op :xray :file src/policy.clj \
  :expr "(-> (form 'audit-report) initializer (expect-count 1) (analyze (fn [[report]] (frequencies (map :category (:events report))))))"
```

`analyze` always passes a vector of zero, one, or many selected values in query
order. `expect-count` refuses before calling the function and does not change
that input type. Therefore one selected vector is `[[...]]`, which remains
distinct from multiple selected scalar forms.

Use `initializer` after a named `def` to select its right-hand side directly.
It returns zero matches for an unbound `def` or a non-`def` form and never
evaluates constructor syntax.

The value contains parsed source syntax, not evaluated program state. For computed
X-ray, a selected value that is itself a map literal or `hash-map` /
`array-map` constructor receives one shallow canonical map view. Nested
constructor syntax remains syntax.

The tool pairs already-parsed key/value
forms. It never invokes a constructor or argument. Unsupported calls remain
lists, malformed map constructors refuse, and literal X-ray plus evidence
retain exact source. Return concrete EDN from computation. Realize lazy results
with `vec` or another collection.

`analyze` always receives one vector of ordinary Clojure data. Write one
terminating pure function over that contract instead of a separate
shape-discovery query. When a predicate identifies the desired descendants, use
`(filter predicate (tree-seq coll? seq value))` inside that function.

Literal paths return full source evidence. Computed paths keep `:value`,
addresses, ranges, trace, per-match hashes, a selection hash, and the
complete-file hash without repeating selected source. Add `:evidence :full`
when a computed read must also return exact selected source. Compact evidence
is the default. New read workflows should use the Clojure X-ray surface above.

Named paths are file-aware. `(form 'load-starred-post :cljs)` and the EDN step
`[:form load-starred-post :cljs]` select one reader-conditional branch in a
`.cljc` file. The extension also constrains ordinary forms: a plain `.clj` form
cannot match `:cljs`, and a plain `.cljs` form cannot match `:clj`. A platform
selector without a `.clj`, `.cljs`, or `.cljc` context refuses. Without a
platform, duplicate branch-local definitions remain honest multiple matches.

`:xray` uses the same sandboxed pure Clojure functions and structural builders
as `:edit :expr`. It never writes source or creates a plan. It refuses truncated
input, analyzer failure, lazy or non-EDN output, and output over 65,536
characters. SCI does not expose I/O, processes, namespaces, mutable references,
classes, or host interop. X-ray is capability-limited, not termination-proof:
analyzers remain responsible for bounded work.

The same `right` relationship moves from a `case` key, `cond` guard, map key,
or binding name to its paired value. Navigation skips whitespace and comments
while returned addresses still refer to the lossless concrete-syntax tree.

In the initial unprimed four-run checksum experiment, `:xray` was exact in four
of four runs. The preceding workflow was exact in three of four. Median wall
time fell 21%, shell calls 33%, input tokens 45%, and output tokens 43%.
Routine literal reads remain cheaper than computation. The
[maximality audit](docs/plans/xray-maximality-audit.md) then compared compact
X-ray with an external Babashka pipeline and direct execution. See the
[experiment record](docs/observations/2026-08-04-captains-log-source-became-data.md).

Make the same path an updater by ending it with `[:replace FORM]`:

```bash
clj-surgeon :op :edit :file src/state.clj \
  :query '[[:form transition] [:find :finish] :right [:replace (assoc state :status :complete)]]' \
  :plan-out plan.edn

clj-surgeon :op :replace-subform! :plan plan.edn
```

The first command never writes source. It refuses unless the path selects
exactly one node, reparses the candidate file, and emits the existing versioned,
hash-bound single-edit plan with selector, trace, diff, source hash, and result
hash. Review it, then run the separate apply command. Never chain plan and
apply. Invalid queries return the compact supported-step grammar in the error,
so an agent does not need a second `--help` call or a wall of generic help.

By default, the `:edit` front door authors the same plan with sandboxed pure
Clojure:

```bash
clj-surgeon :op :edit :file src/state.clj \
  :expr "(-> (form 'transition) (match :finish) right (replace '(assoc state :status :complete)))" \
  :plan-out plan.edn

clj-surgeon :op :replace-subform! :plan plan.edn
```

This is Clojure manipulating Clojure data. `:expr` provides pure
`clojure.core` collection functions—including `let`, destructuring, `assoc`,
`update`, `mapv`, `filter`, `reduce`, `comp`, and `juxt`—plus structural query
builders. SCI compiles the result to the existing query vector. The unchanged
planner still enforces exact-one selection, one terminal edit, complete-file
parse, diff, and hashes.

The important native operation is `transform`. It receives the selected form
as Clojure data, so an agent can derive a replacement without first reading and
copying the current value:

```bash
clj-surgeon :op :edit :file src/policy.clj \
  :expr "(-> (form 'retry-policy) (match :delays) right (transform #(mapv (partial + 100) %)))" \
  :plan-out plan.edn
```

The function runs only after the selector finds exactly one form. The review
plan contains the concrete replacement as `[:replace ...]`, never the function. The
existing EDN executor can therefore read and replay the plan without SCI. Use
`replace` when the new form is already known. Use `transform` when the new form
depends on the selected code or data.

A literal `replace` or `replace-span` written inline in `:expr` preserves the
exact replacement source. This includes anonymous-function shorthand,
comments, commas, metadata, reader syntax, and multiline layout:

```bash
clj-surgeon :op :edit :file src/page.clj \
  :expr "(-> (form 'page) (match '{:dev-mode? dev-mode?}) (replace '{:dev-mode? dev-mode? :head {:asset-url #(views/static %)}}))" \
  :expect '{:dev-mode? dev-mode?}'
```

The planner retains that literal source beside the evaluated query and verifies
that both describe the same replacement. A replacement computed through a
local binding or `transform` has no literal source to preserve. The `:query`
surface also contains data rather than source spelling. Those routes use
canonical printing and still require plan review.

The returned `:selector :query` is semantic data, so it can display `#()` as an
equivalent `fn*` form. This is not the planned source spelling. Read the edit's
`:after` and `:diff` fields for the exact source that clj-surgeon will write.

Without `:expect`, `:plan-out` is required because the command is plan-only.
Use an `.edn` suffix. Do not preflight whether that path exists. A successful
plan atomically replaces that artifact. A refusal leaves an existing plan
unchanged.

`:expect` is the optional one-call guarded form. It is the caller's declared
before-state for a literal replacement. The comparison ignores whitespace.
It does not ignore comments, metadata, reader macros, or token spelling. Those
elements can affect behavior or preserve design intent.

On equality, `:edit` applies the plan in memory and returns its evidence merged
with the verified apply receipt and `:mode :expect-guarded`. No plan filename is
needed. Add `:plan-out plan.edn` only when an audit artifact must be retained.
On any difference the command exits nonzero with `:error-type
:expect-mismatch`, reports `:expected`, `:actual`, and `:actual-source`, and
leaves both the source bytes and an existing plan artifact untouched:

```bash
clj-surgeon :op :edit :file src/state.clj \
  :expr "(-> (form 'transition) (match :finish) right (replace '(assoc state :status :complete)))" \
  :expect '(assoc state :status :done)'
```

The same guarded route works inside an unnamed top-level owner. This is one
CLI call because the caller declares the exact old leaf:

```bash
clj-surgeon :op :edit :file src/cache.clj \
  :expr "(-> (line 412) (match '(old-reader account-id)) (replace '(new-reader account-id)))" \
  :expect '(old-reader account-id)'
```

The line narrows ownership. `match` selects the leaf. `:expect` verifies the
old leaf before the tool writes atomically, reparses the complete file, and
returns the read-back hash. Repeated copies in other top-level forms remain
outside the selection.

Use shell single quotes around `:expect` source when possible. They prevent the
shell from expanding `$`, backticks, and other source characters.

If the selected subtree contains a comment or metadata that is absent from
`:expect`, the command refuses with `:expect-mismatch`. It returns the lossless
`:actual-source`. Prefer a narrower replacement that leaves the surrounding
syntax in place:

```bash
clj-surgeon :op :edit :file src/state.clj \
  :expr "(-> (form 'transition) (match :done) (replace :complete))" \
  :expect :done
```

This command replaces only `:done`. A comment elsewhere in the enclosing form
stays in place. Do not rely on automatic comment migration. The tool refuses an
undeclared comment because it cannot infer the correct destination after a
larger rewrite.

`:expect` refuses a terminal `transform`. A transform computes its after-state,
so the before-state alone cannot replace review of the generated diff. Remove
`:expect`, review the saved plan, and apply it separately. Use a literal
`replace` when both the before-state and after-state are known.

Without `:expect` nothing changes: `:edit` stays plan-only and the documented
default remains plan first, review, then apply separately with
`:replace-subform!`. `:expect` mechanizes that review gate rather than removing
it—the saved plan is still the audit artifact—so use it when the before-state is
already known exactly, and the two-command flow when it is not. The guarded
receipt verifies the source write and the saved plan artifact. Atomic source
writes preserve the existing file permissions, including the executable bit.

The SCI environment does not expose filesystem or process operations,
namespace loading, mutable references, Java classes, or host interop. Supply
exactly one of `:query` and `:expr`. Use the literal EDN query when it is
shorter. Use pure Clojure when computation makes the selector or replacement
clearer. Both surfaces save the same plan and use the same separate verified
executor. A refusal includes the allowed capabilities and signatures, so a
caller can repair the expression without a second help command.

When adjacent forms are themselves the meaningful object, promote the current
node and its next semantic peer into a lossless slice with `[:span 2]`:

```bash
clj-surgeon :op :xray :file src/state.clj \
  :expr "(-> (form 'transition) (match :finish) (span 2))"

clj-surgeon :op :edit :file src/state.clj \
  :query '[[:form transition] [:find :finish] [:span 2] [:replace-span :finish (assoc state :status :complete)]]' \
  :plan-out plan.edn

clj-surgeon :op :replace-subform! :plan plan.edn
```

`:span N` contains the current semantic node and its next `N-1` siblings and
never crosses their parent. It counts through comments and whitespace without
owning the trivia before or after the slice. `:replace-span` requires exactly
`N` replacement forms and replaces corresponding nodes, preserving every
intervening comment and whitespace byte. The same primitive makes a flattened
anonymous-function body addressable—for example,
`[[:find select-keys] [:where {:parent-tag :fn}] [:span 3]]`—without pretending
that rewrite-clj contains a wrapper list that is not actually there.

When the task asks for every pair, use `[:partition-all 2]` instead of reading
the owner, counting children, or issuing one query per key:

```bash
clj-surgeon :op :xray :file src/state.clj \
  :expr "(-> (form 'transition) (match 'case) up down right right (partition-all 2))"
```

`[:partition-all N]` starts at the current node and partitions it with all
following semantic siblings. Each result is an existing lossless span with
`:forms`, exact `:source`, addresses, gaps, and
`:partition {:size N :index I :complete? BOOLEAN}`. The operation never crosses
the parent and never drops a remainder. For a `case`, a final one-form span can
be the default expression. The tool reports only that the span is incomplete;
the caller decides what it means. A nested branch result remains one subtree.

If repeated nested heads make the first outer sibling unknown, promote every
head to its owner and retain the maximal owners before partitioning:

```bash
clj-surgeon :op :xray :file src/policy.clj \
  :expr "(-> (form 'classify-request) (match 'cond) up outermost down right (partition-all 2))"
```

Placement matters. The symbol nodes selected by `[:find cond]` do not contain
one another, but their owner lists do. Thus use `:up :outermost`, not
`:outermost :up`. If the first outer guard is already known, anchor on it
directly. That route is shorter.

Use `:right` for one known value, `[:span 2]` for one known pair, and
`[:partition-all 2]` for all pairs in a sibling suffix. A terminal
`[:replace-span FORM ...]` can update one unambiguous partition with equal
arity. Multiple partitions refuse mutation, so enumeration never becomes a
silent bulk edit.

This algebra removes the `cat owner → reconstruct peer match → plan` bridge.
The existing commands remain useful standard-library spellings: `:cat` is the
fastest exact top-level read, and `:match-form` / `:replace-subform` are concise
when an independent subtree pattern already identifies the target.

#### `:ls-tree` / `:tree` / `:map` — Map an entire directory of repos

```bash
# Map a single project
clj-surgeon :op :ls-tree :dir .

# Search across ALL your repos in seconds
clj-surgeon :op :ls-tree :dir ~/src.local/ :grep "postgres|jdbc"
```

The operation discovers projects through `deps.edn` or `project.clj`, reads
their `:paths`, and outlines each `.clj` file. With `:grep`, it uses ripgrep to
select matching files before parsing. In one observed search across 20
repositories, this reduced a 90-second full scan to 3 seconds and about 3,000
tokens. An Explore agent took more than 80 seconds and 55,000 tokens for the
same question.

See [docs/ls-tree.md](docs/ls-tree.md) for full documentation.

#### `:ls-deps` — Transitive dependency tree

```bash
clj-surgeon :op :ls-deps :file state.clj :form transition!
```

The result shows the dependency chain as a nested tree. It identifies leaves,
transitive dependencies, and cycles. Use it to assess a move or extraction.

```
transition! (line 2655)
├── log-event! (line 2600)
│   └── events-file ◆ leaf
├── log-wal! (line 2627)
│   └── wal-file ◆ leaf
├── transition-tx (line 2642)
│   └── settle-editing-state ◆ leaf
└── wal-snapshot ◆ leaf
```

#### `:ls-extract` — Minimal extractable unit

```bash
clj-surgeon :op :ls-extract :file state.clj :form rebuild-ai-paragraphs!
```

The result contains the target form and each private helper used only by that
form. This is the minimum extraction set that preserves those dependencies.

#### `:declares` — Audit which declares are needed

```bash
clj-surgeon :op :declares :file src/writer/state.clj
```

#### `:deps` — Intra-namespace call graph

```bash
clj-surgeon :op :deps :file state.clj :form sync-draft!
```

#### `:topo` — Topological sort (optimal form ordering)

```bash
clj-surgeon :op :topo :file state.clj
```

### CLJ, CLJS, and CLJC operations

Reader conditionals require structural handling. A form inside `#?(:clj ...)`
is not an ordinary list at the top level. Free-form edits can create malformed
splices, inconsistent aliases, or missing `:refer` lists.

These operations are deterministic and refuse to emit malformed reader
conditionals. The read-only operations `:ls`, `:deps`, `:topo`, `:ls-deps`,
`:ls-extract`, and `:declares` are reader-conditional-aware. For example, a
`(defn foo …)` inside `#?(:clj …)` has `:platforms [:clj]` in the outline and
participates in dependency analysis.

#### `:cljc-merge` — Combine parallel CLJ + CLJS files into one CLJC

```bash
clj-surgeon :op :cljc-merge :clj src/foo.clj :cljs src/foo.cljs :out src/foo.cljc
```

The operation handles shared and one-sided requires, including
`#?@(:clj […])` and `#?@(:cljs […])`. It handles divergent aliases, divergent
`:refer` lists, npm string requires for `:cljs`, and per-form body collisions as
`#?(:clj … :cljs …)`. It refuses namespace docstrings, attribute maps,
unsupported subforms, name mismatches, and body-count mismatches.

#### `:cljc-split` — Inverse of merge

```bash
clj-surgeon :op :cljc-split :file src/foo.cljc :clj-out src/foo.clj :cljs-out src/foo.cljs
```

Round-trip-tested: `(merge → split → merge)` is a fixed point.

#### `:cljc-add-require` — Platform-aware require addition

```bash
clj-surgeon :op :cljc-add-require :file src/foo.cljc \
  :platform :cljs :ns goog.string :as gstr :out src/foo.cljc
```

`:platform` is `:clj`, `:cljs`, or `:cljc`. The operation refuses to bind one
alias to two namespaces on the same platform. It preserves npm string literals,
so `:ns "react"` remains a string.

#### `:cljc-analyze` — Structured classification for LLM consumption

```bash
clj-surgeon :op :cljc-analyze :clj src/foo.clj :cljs src/foo.cljs    # pair
clj-surgeon :op :cljc-analyze :file src/foo.cljc                      # single CLJC
```

The result contains EDN buckets for shared, one-sided, and divergent requires.
It also contains per-platform top-level form summaries.

### Write operations

#### `:change` / `:change!` / `:undo-change!` — Compile one complete plan

Use one transaction when the complete plan is already known. A scoped change
states the files, optional unique owner forms, exact structural target, literal
replacement, and exact consent counts. The aggregate expectation guards the
complete plan.

```bash
clj-surgeon :op :change! :spec-file - \
  :receipt-out /tmp/ui-change.edn <<'EDN'
{:changes
 [{:id :body-class
   :in ["src/ui.clj"]
   :forms [shell reader]
   :find ":body"
   :do [:replace ":body.page"]
   :expect {:matches 2 :each-form 1}}]
 :expect {:changes 1 :edits 2 :files 1}}
EDN
```

This follows the `kubectl apply -f -` convention: command-line arguments select
the operation, stdin carries the large structured document, and stdout returns
the compact receipt. Use `:spec-file PATH` for a saved document. Inline `:spec`
remains compatible for small plans, but it is not the primary agent route.

All changes compile against the same original snapshots. `:find` matches exact
structural source inside the explicit files and optional owner forms.
Whitespace can differ. Comments, metadata, reader macros, token spelling, and
collection type must match. `:each-form` proves the match distribution across
owners. `:each-file` proves it across files. A named owner must resolve exactly
once in each scoped file.

The first scoped-change slice supports `[:replace SOURCE]`. It does not yet
support relational `:path`, captures, insertion, deletion, computed
transformations, regular expressions, or fuzzy matching. Use `:edit` for one
relational or computed change. Use the specialized move, dependency, require,
rename, and CLJC operations for their stronger contracts.

Any count mismatch, overlap, invalid future file, or stale hash refuses the
complete transaction. A handled write or receipt-publication failure restores
transaction-owned bytes. Recovery never overwrites unknown concurrent bytes.

Use the non-mutating command when the combined diff needs review:

```bash
clj-surgeon :op :change :spec-file plan.edn
```

Successful `:change!` output is compact. The durable receipt contains concrete
inverse edits and original/result hashes. Undo refuses before writing when any
current file differs from its recorded forward result:

```bash
clj-surgeon :op :undo-change! :receipt /tmp/api-change.edn
```

Do not open the receipt file. The `:change!` stdout contains the counts, hashes,
verification, receipt path, and inverse summary needed for the decision. Pass
the saved path directly to `:undo-change!`.

Do not split one known multi-edit plan into repeated `:edit` or
`:replace-subform!` calls. Use those operations when the replacement must be
computed from selected source or reviewed as one independently meaningful
edit.

Legacy exact `:intents` with `:files`, `:from`, `:to`, `:expect-count`, and the
original aggregate expectation keys remain accepted. Do not mix `:intents`
and `:changes` in one transaction.

#### `:fix-declares` / `:fix-declares!` — Eliminate unnecessary declares

```bash
clj-surgeon :op :fix-declares :file state.clj     # plan (dry run)
clj-surgeon :op :fix-declares! :file state.clj    # execute
```

The compound operation:
1. Finds removable declares via topological sort
2. Checks each form's dependencies at the destination
3. Pulls leaf deps along with their dependents (no Whac-A-Mole)
4. Moves safe defns above their first callers
5. Deletes stale declare lines
6. Skips truly unsafe moves (non-leaf dep chains) with warnings

#### `:mv` — Reorder a form within a file

`mv` writes its input file directly unless `:dry-run true` is present. An LLM
should use this branching workflow, not jump straight to `:mv-with-deps`:

```bash
# 1. Orient and preview the exact requested move. Neither command writes.
clj-surgeon :op :ls :file state.clj
clj-surgeon :op :mv :file state.clj :form foo :before bar :dry-run true

# 2a. Only when the preview returns :ok true, review :plan/:diff and execute.
clj-surgeon :op :mv :file state.clj :form foo :before bar
```

Moves a named form (including its preceding comment header) before another
form. Before writing, `:mv` validates the complete candidate. If the move would
strand a dependency or caller, it refuses with structured EDN, changes no
bytes, and exits nonzero.

A dependency refusal is a decision point for an agent:

```clojure
{:error-type :would-strand-dependencies
 :form "foo"
 :before "bar"
 :stranded [{:name "config"
             :defined-at 80       ; original source line
             :would-be-at 94      ; line in the rejected candidate
             :required-before 40}]
 :recommended-action :preview-dependency-closure
 :recommended-command
 "clj-surgeon :op :mv-with-deps :file state.clj :form foo :before bar :dry-run true"
 :apply-command
 "clj-surgeon :op :mv-with-deps :file state.clj :form foo :before bar"}
```

Run the safe `:recommended-command`, then inspect all of
`:plan/:added-forms`, `:move-order`, and `:diff`. Execute `:apply-command` only
after consenting to every added form. The successful preview repeats
`:apply-command`, so it is self-contained. `:mv-with-deps` always forces
`:with-deps true`, even if a caller also supplies `:with-deps false`.

```bash
# 2b. Dependency refusal only: preview the widened move, then apply after review.
clj-surgeon :op :mv-with-deps :file state.clj :form foo :before bar :dry-run true
clj-surgeon :op :mv-with-deps :file state.clj :form foo :before bar
```

`:mv-with-deps` is the opt-in alias for `:mv :with-deps true`. It moves only
the minimum transitive dependency closure required at the selected destination,
lists every added form in the plan, and never silently adds declarations or
moves callers. For `:would-strand-users`, cycles, ambiguity, unsupported source
layouts, or any other refusal, stop and choose a different destination or
refactor. Do not force the alias.

A dry run is an informational preview, not a saved hash-bound application
artifact. Preview again after any source change. After writing, rerun `:ls`,
audit any now-redundant declaration with `:declares` or `:fix-declares`, and run
the target repository's formatter, linter, compiler, and tests. Shell-quote
names containing special characters, for example `:form '*state*'`.

#### `:rename-ns` / `:rename-ns!` — Rename a namespace prefix

```bash
clj-surgeon :op :rename-ns :from old-prefix :to new-prefix :root .   # plan
clj-surgeon :op :rename-ns! :from old-prefix :to new-prefix :root .  # execute
```

Walks every `.clj` file's AST. Renames ns declarations and `:require` entries structurally (not text replace). Computes file moves. Flags non-Clojure files for manual review. **This is how clj-surgeon renamed itself.**

#### `:extract` / `:extract!` — Move forms to a new namespace

```bash
clj-surgeon :op :extract :file src/writer/state.clj \
  :forms '[rebuild-ai-paragraphs! enter-distillery!]' \
  :to src/writer/state/distillery.clj    # plan (dry run)

clj-surgeon :op :extract! :file src/writer/state.clj \
  :forms '[rebuild-ai-paragraphs! enter-distillery!]' \
  :to src/writer/state/distillery.clj    # execute
```

The compound extraction operation:

1. Finds named forms and their exclusive private helpers (`:ls-extract` closure)
2. Copies the source namespace form as a template and initially retains extra requires
3. Writes forms to the new file in topological order
4. Removes extracted forms from the source file
5. Adds a require for the new namespace to the source file
6. Reports callers that may need updating

Planning is pure. Only `:extract!` writes files. After extraction, use the
compiler to find unresolved bare references and correct them.

#### `:match-form` / `:replace-subform` — Nested structural edits

Use `:match-form` for file-wide structural search. It is not a text regular
expression. Matching ignores formatting, and `_` matches exactly one subtree:

```bash
clj-surgeon :op :match-form :file src/views.clj \
  :match '(post! "/api/items" _)'
```

Add `:inside` only when the containing top-level form is already known or when
you need to narrow multiple matches:

```bash
clj-surgeon :op :match-form :file src/views.clj :inside render \
  :match '(post! "/api/items" _)'
```

Plan exactly one replacement, review the EDN and diff, then apply that exact
hash-guarded plan:

```bash
clj-surgeon :op :replace-subform :file src/views.clj :inside render \
  :match '(post! "/api/items" _)' \
  :with '(items/actions surface)' \
  :plan-out plan.edn

clj-surgeon :op :replace-subform! :plan plan.edn
```

Search reports every match. Replacement refuses zero or multiple matches.
Each search match includes its enclosing top-level name in `:inside` when one
is mechanically available. Reuse that value directly to narrow a replacement.
Do not run `:cat :line` merely to recover the owner.

`:match` and `:with` must each contain exactly one complete Clojure form—trailing
syntax is an error. The plan is a versioned EDN artifact with stable
`:operation`, `:selector`, hash, edit, diff, and provenance fields. Its `:diff`
is a standard unified diff, suitable for human or agent review.

Application validates the plan version, source snapshot, recorded address,
exact before text, complete rewritten-file parse, and result hash. It then
atomically replaces the target file and reads it back.

A successful EDN receipt includes `:applied-edit`, `:verified` whole-file parse,
`:read-back-hash`, and atomic-write evidence. Do not repeat those checks with
`rg`, `:cat`, `git diff`, or `shasum`. The reviewed plan is the edit-level
diff. Continue with the relevant formatter, linter, compiler, and tests.

Review an aggregate Git diff only if the task already establishes a worktree or
explicitly requests the diff. Do not probe `.git` only to repeat edit-level
evidence. If atomic replacement is unavailable, the command fails. It does not
fall back to a weaker write. Every error is concise EDN and exits nonzero.

Apply the reviewed plan directly with `:replace-subform!`. Do not edit the plan
with `apply_patch` or another text tool. If the intended edit changes, generate
a new plan.

A `case` clause, `cond` branch, map entry, or binding pair is adjacent sibling
syntax, not a synthetic wrapper list. When one peer identifies another, use
`:xray` navigation rather than reconstructing a match from the whole owner. Use
`:replace-subform` when an independently readable value or expression already
identifies the target exactly. See
[issue #21](https://github.com/realgenekim/clj-surgeon/issues/21).

Run plan generation as a standalone shell command. Observe and review its
result before a separate apply command. Never chain planning and application.

In its [first production use](docs/observations/2026-07-11-subform-first-real-use.md),
`:ls` reduced a 4,036-line, 322-form namespace to the relevant synchronization
and admission forms. Three `:find-subform` queries then uniquely located the
Save Draft Hiccup vector, an unsafe browser-sync mutation, and a session-save
call by semantic path. The practical division was clear. `rg` remained faster
for broad discovery, while structural search was substantially safer for
repeated Hiccup actions and large handler forms. This first report validated
discovery. The follow-up below validated the replacement workflow separately.

A follow-up production edit supplied that proof: `:find-subform` uniquely
selected a 13-line inline-JavaScript “Edit in Draft” button, and the reviewed
plan replaced it with a shared single-flight command call. `:replace-subform!`
returned the exact result hash recorded by the plan.

When a replacement contains Clojure that generates JavaScript, quote the whole
`:with` form with single shell quotes and use `pr-str` for generated JavaScript
string literals:

```bash
clj-surgeon :op :replace-subform \
  :file src/writer/views/book_workshop.clj \
  :inside editor-panel \
  :match '[:button {:style _ :onclick _} "Edit in Draft →" [:span.kbd-hint _ "^Enter"]]' \
  :with '[:button {:onclick (str "editBookNodeInDraft(" (pr-str id) ",this.closest(" (pr-str ".bw-editor-panel") "))")} "Edit in Draft →"]' \
  :plan-out plan.edn
```

JavaScript `\xNN` escapes are not valid EDN/Clojure string escapes. Prefer
`pr-str`, or valid Clojure escapes such as `\"`, `\\`, and `\u0027`.

A broader [session-history ethnography](docs/observations/2026-07-12-lenses-in-the-wild.md)
found 38 direct production structural-query invocations across UI, state, storage, and route
work. Beyond replacement, agents used zero/one match results as executable
hypotheses about code shape and used sequential hash-bound plans to refactor a
fast-moving dirty worktree safely.

## Custom defining forms

Clojure codebases can define forms with macros such as
[Guardrails](https://github.com/fulcrologic/guardrails) `>defn` and `>defn-`,
[Malli](https://github.com/metosin/malli) `mu/defn`, and
[Schema](https://github.com/plumatic/schema) `s/defn`. Projects can also define
macros such as `defendpoint`, `defenterprise`, and `defsetting`.

The `forms.clj` classification module defines what counts as a definition, what
has arglists, and what is private. The operations `:ls`, `:deps`, `:topo`,
`:ls-deps`, `:ls-extract`, `:extract`, and `:fix-declares` use this module.

Classification has three rules:

1. Match core forms such as `defn`, `defn-`, `def`, `defonce`, and `defmacro`
   exactly.
2. For a qualified form such as `mu/defn`, `s/defn`, or `malli.util/defn-`,
   remove the qualifier and match the local name. Thus `mu/defn`, `m/defn`, and
   `malli/defn` all classify as `:defn`.
3. List non-standard names such as `>defn` and `>defn-` in `forms.clj` as
   explicit aliases.

Before you add support for a macro, check whether qualification already handles
it:

```clojure
;; Namespace-qualified forms just work — no config needed
(mu/defn foo ...)     ; ✓ auto-detected: local name "defn" → :defn
(s/defn bar ...)      ; ✓ auto-detected: local name "defn" → :defn
(my.lib/defn- baz ..) ; ✓ auto-detected: local name "defn-" → :defn- (private)
```

If the macro has a non-standard local name, add one entry to `explicit-aliases`
in `src/clj_surgeon/forms.clj`:

```clojure
(def explicit-aliases
  {">defn"          :defn
   ">defn-"         :defn-
   "defendpoint"    :defn      ;; your custom macro here
   "defenterprise"  :defn})
```

## Agent workflow

The Claude Code and Codex entrances load one canonical skill package. That
skill teaches these common routes:

- Before native source tools touch an existing Clojure file, load the skill;
  retain native Write for new files and unsupported prose- or comment-heavy
  changes.
- For an unknown large file, use `:ls`, then read only the required forms.
- For a known form, line, or distinctive text, start with `:cat`.
- For nested syntax in an unnamed top-level form, start an `:xray` or `:edit`
  expression with `(line N)`, then narrow to the exact subtree.
- For a known exact multi-edit plan, use one `:change!` transaction. Use
  `:change` when the combined diff needs review first.
- For a computed single edit, generate and review one plan before applying it.
- For a declaration, run `:fix-declares` before deciding whether to apply
  `:fix-declares!`.
- For an extraction, inspect `:ls-deps` and `:ls-extract` before planning the
  write.
- For a namespace rename, review `:rename-ns` before running `:rename-ns!`.

The tool performs parsing, selection, movement, and require rewriting. The
agent decides scope and intent.

### Repeated agent-usage studies

The repo-local `study-agent-usage` skill turns the recurring Codex-versus-Claude
ethnography into one bounded command:

```bash
make study-agent-usage
```

The canonical package lives under `skills/study-agent-usage`. The repository
exposes it to Codex through `.agents/skills/` and to Claude Code through
`.claude/skills/`, both as links to that one package.

It scans both providers from the newest completed study marker, prints a
compact aggregate, and writes the complete versioned JSON receipt to the
reported temporary path. Pass `--receipt-out PATH` through
`AGENT_USAGE_ARGS` to retain it elsewhere. Neither surface contains transcript
prose or workspace paths. The receipt distinguishes skill visibility, skill
loading, real clj-surgeon invocations, native Clojure actions, direct tool
wall, and complete Codex turn wall. A completed study records the emitted
`next_marker`, which becomes the next automatic "since last time" boundary.
Run `make study-agent-usage-self-test` to verify both history parsers and the
privacy contract.

## Development method

Features begin with observed agent behavior on real tasks:

1. Give a clean agent a bounded task.
2. Record unnecessary reads, guesses, help calls, unsafe plans, and repeated
   work.
3. Separate mechanical bookkeeping from decisions that require judgment.
4. Add the smallest general structural primitive that removes the mechanical
   work.
5. Add a named regression, exhaustive pure tests, CLI contract tests, and a
   real-program-derived fixture.
6. Repeat the clean-context task and preserve the transcript.

The [observation records](docs/observations/) contain the resulting experiments
and failure reports.

## Why structural operations help

[rewrite-clj](https://github.com/clj-commons/rewrite-clj) provides a
position-aware concrete-syntax tree. clj-surgeon can therefore walk and replace
syntax nodes while preserving comments and formatting.

| Operation | Structural leverage | Typical alternative |
|-----------|---|---|
| Namespace rename | Symbol-node replacement | IDE or language-server refactor |
| Dependency graph | Walk resolved form references | Full language server |
| Topological sort | Reuse the dependency graph | Separate semantic analysis |
| Dep tree visualization | Project the same graph | Separate indexing layer |
| Forward ref detection | 1 clj-kondo call | Custom parser + resolver |
| Form boundaries | Built into rewrite-clj | Tree-sitter + custom queries |
| Require inference | Namespace/form data | Module resolution engine |
| Leaf dep-pulling | Reuse dep graph + form mover | Compound IDE refactor |

Babashka bundles the parser and printer. Analysis composes collection
operations, and transforms use zipper replacements. This provides reusable
structural leverage without requiring a complete language-server refactor.

The long-open clojure-lsp
[`move-form` issue #566](https://github.com/clojure-lsp/clojure-lsp/issues/566)
shows the broader problem. clj-surgeon handles bounded bookkeeping and leaves
scope decisions to the caller.

## Failures that shaped the tool

Real use on a 2,768-line production file exposed these defects. Each fix added
a regression test:

1. **`:mv` matches declare, not defn** — `find-form` hit the first name occurrence. Fixed: skip `declare` forms.
2. **`z/next` infinite walk** — traversed entire file, not just one form. Fixed: scope zipper to form subtree.
3. **Topo sort reversed** — Kahn's algorithm on wrong edge direction. Fixed: process zero-dependency forms first.
4. **Metadata blindness** — `(def ^:private events-file ...)` returned `"^:private"` as the name. Fixed: detect `:meta` node tag, walk to rightmost child.
5. **Declares in dep graph** — `(declare foo)` returned empty deps, masking real `(defn foo)` deps. Fixed: exclude declares from analysis.

See [docs/observations/](docs/observations/) for the complete build-use-fix
records.

## Testing

```bash
make test
```

Pure analysis functions take strings, zippers, or data and return data. Tests
exercise those functions with literals. Filesystem and subprocess tests cover
only contracts that require those boundaries. Write tests verify that refusal
does not change bytes and that a successful candidate parses and passes the
appropriate lint or compile check. See
[docs/testing-guidelines.md](docs/testing-guidelines.md).

## Architecture

```
src/clj_surgeon/
  forms.clj          # single source of truth for defining-form classification
  core.clj           # CLI entry point, :op dispatch
  outline.clj        # rewrite-clj form boundary parser (CLJC-aware)
  forward_refs.clj   # clj-kondo forward-ref detection
  move.clj           # form reordering within a file
  analyze.clj        # dep graph, topo sort, dep tree, closure (CLJC-aware)
  rename.clj         # namespace prefix rename (AST surgery)
  fix_declares.clj   # compound op: eliminate removable declares + pull leaf deps
  extract.clj        # compound op: move forms to a new namespace file
  cljc/
    walk.clj         # reader-conditional-aware top-level form walker
    merge.clj        # CLJ + CLJS → CLJC (divergent aliases, npm, body collisions)
    split.clj        # CLJC → CLJ + CLJS (inverse of merge)
    require_ops.clj  # platform-aware add-require (with alias-collision detection)
    analyze.clj      # structured CLJC classification for LLM consumption
```

The installed CLI requires Babashka and clj-kondo. Babashka supplies
rewrite-clj and Cheshire in its runtime. clj-kondo supplies the static
forward-reference boundary.

## Prior art and acknowledgments

- [rewrite-clj](https://github.com/clj-commons/rewrite-clj), created by Yannick
  Scherer ([@xsc](https://github.com/xsc)) and maintained by Lee Read
  ([@lread](https://github.com/lread)), provides the zipper and concrete-syntax
  tree.
- [Babashka](https://github.com/babashka/babashka), created by Michiel Borkent
  ([@borkdude](https://github.com/borkdude)), supplies the runtime, rewrite-clj,
  and Cheshire.
- [clj-kondo](https://github.com/clj-kondo/clj-kondo), also created by Michiel
  Borkent, supplies forward-reference analysis.
- Eric Dallo's [ECA](https://github.com/editor-code-assistant/eca) work and the
  long-open [clojure-lsp#566](https://github.com/clojure-lsp/clojure-lsp/issues/566)
  helped define the boundary between bookkeeping and judgment.
- Dan Peddle's [cclsp](https://github.com/dazld/cclsp) showed how many structural
  operations an agent-facing Clojure tool can expose.

## Design principle: bookkeeping, not judgment

clj-surgeon performs bounded mechanical work. The caller decides what to move,
where to move it, and whether to apply a reviewed plan. The compiler and tests
then provide external feedback.

This boundary keeps the tool general. It also prevents the tool from silently
guessing intent or expanding the requested scope.

## Compatibility aliases

Existing callers can continue to use `:outline`, `:show-form`, `:find-subform`,
`:grep-form`, `:lens`, and `:q`. New callers should use `:ls`, `:cat`,
`:match-form`, `:xray`, and `:edit`.
