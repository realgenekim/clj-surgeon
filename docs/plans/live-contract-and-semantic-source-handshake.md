# Live MCP Contract and Semantic/Source Handshake

**Status:** Complete — implementation, live multi-workspace proof, full release
tests, and stable installation passed on 2026-08-07
**Motivating incidents:** A clj-surgeon schema change required a Codex restart
because the server advertised `tools.listChanged=false`. Separately, a
proof-carrying change basis trusted cclsp locations without proof that
clojure-lsp resolved those locations against the exact bytes clj-surgeon read.

## Outcome

Lower the cost of changing the tool while closing the remaining correctness
gap between semantic resolution and lossless source mutation.

1. A live clj-surgeon MCP server publishes tool additions, removals, and
   contract replacements through `notifications/tools/list_changed`.
2. Every successful cclsp `resolve_var_surface` location carries one LSP
   session identity, an exact file range, owner status, and the SHA-256 of the
   synchronized source bytes. Version 3 permits `owner_status=unresolved` for
   references inside project macros that the language server cannot classify.
3. clj-surgeon compares every provider hash with its own canonical file
   snapshot before it publishes a change basis. Any mismatch refuses as
   `semantic-source-drift`.

No caller performs an extra hash, refresh, or validation step.

## Field extension: project-owned verification profiles

The first real customer transaction proved that semantic readiness and
mutation verification have different project inputs. The `server2` language
server resolved the complete change surface, but the default clj-kondo command
used the nearest incomplete cache and rejected three valid Vars from a local
dependency.

An optional `.clj-surgeon.edn` at the MCP project root can now define the
closed `:verification-profiles` map. The server reads this file at startup.
Explicit process options retain precedence, and repositories without the file
retain the built-in profiles. Profile commands remain vectors of strings; an
agent cannot supply or modify them in a mutation request.

The field fixture must prove all of these results:

| Case | Result |
|---|---|
| No project config | Built-in profiles remain unchanged |
| Valid closed profiles | Project profiles load exactly |
| Explicit process profiles | Explicit profiles override the file |
| Invalid EDN or command shape | Server startup refuses with stable data |
| `server2` local dependency | Fast verification reports zero warnings |

This extension does not widen source confinement or change the semantic
workspace. Codex continues to launch from `server2/`; project-owned verification
only teaches the transaction gate how that project already validates its code.

## Field extension: one idempotent entrance

The server2 acceptance run proved two remaining setup defects:

1. A transaction cannot name the top-level `ns` form as its owner.
2. Each customer checkout currently requires a separate clj-surgeon/cclsp port
   pair and project-local Codex tool tables.

The bounded remedy is `clj-surgeon up [WORKSPACE]`. It defaults to the current
directory, canonicalizes the root, adds that root to cclsp's existing
multi-project configuration, starts or refreshes one stable loopback tool
stack, writes one clj-surgeon Codex table, and finishes with a readiness probe.
Repeating the command must be a no-op apart from the probe.

The two existing MCP tools gain one optional `workspace_root` string. Its
canonical path is the workspace identity. There are no aliases, workspace
names, registry UI, or collision rules. The default remains the server's
startup root for compatibility. Each result and mutation receipt records the
canonical workspace root.

Direct changes may use either the existing named `forms` scope or exactly one
closed owner object:

```json
{"owner":{"kind":"namespace","name":"server2.media.interface"}}
```

Only `kind=namespace` is added now. Anonymous top-level indexes and whole-file
owners remain out of scope until a field case requires them.

The public MCP surface remains exactly `inspect_clojure` and
`apply_clojure_changes`. cclsp and clojure-lsp remain internal semantic
providers. Existing workspace Make targets become compatibility wrappers only
after the new entrance passes the real server2 continuation.

## Field extension: shared Clojure coordinates and definition vocabulary

The first shared-workspace dogfood request exposed two distinct join defects.
The request named `server2.views/post-card`. clj-surgeon selected the exact
`(>defn post-card ...)` owner, but cclsp returned zero workspace symbols.

The tools used different coordinates:

```text
clj-surgeon   canonical workspace + namespace/name + exact source owner
cclsp         LSP workspace + fuzzy symbol query
clojure-lsp   document URI + line/character
```

All three layers must use one versioned coordinate envelope:

```clojure
{:workspace-root "/canonical/project/root"
 :var {:namespace "server2.views"
       :name "post-card"}
 :source {:file "src/server2/views.clj"
          :sha256 "..."}
 :range {:start {:line 586 :character 7}
         :end {:line 702 :character 3}}}
```

`workspace-root` selects one project context. `namespace` and `name` identify
one logical Clojure Var. `file`, `range`, and `sha256` identify one physical
source incarnation. cclsp adds `lsp-session` to identify the semantic snapshot.
No layer can replace these fields with a fuzzy name lookup.

The preparation order changes:

1. An unanchored compatibility query can nominate one candidate definition
   file. Its ranges and owners are discovery data only. They cannot become a
   basis.
2. clj-surgeon reads that candidate, verifies the exact namespace and named
   owner, and constructs a source anchor from its own bytes.
3. clj-surgeon sends the source anchor to cclsp in a second request.
4. cclsp verifies the anchor hash, synchronizes that exact file, and only then
   queries clojure-lsp at the anchored owner.
5. cclsp returns session-bound definition and reference coordinates.
6. clj-surgeon independently verifies every returned hash and owner before it
   stores a basis.

The anchored request extends `resolve_var_surface` without breaking the
unanchored compatibility route:

```json
{
  "var": "server2.views/post-card",
  "workspace_root": "/canonical/project/root",
  "source_anchor": {
    "file": "src/server2/views.clj",
    "source_sha256": "...",
    "owner": "post-card",
    "range": {
      "start": {"line": 587, "character": 0},
      "end": {"line": 704, "character": 44}
    }
  }
}
```

Anchor ranges use zero-based LSP coordinates and enclose one complete named
top-level form. `workspace_root` is required when `source_anchor` is present.
The candidate file must be project-relative and confined to that root. The
anchor owner must equal the Var name. The candidate file's `ns` form must equal
the Var namespace. The SHA-256 must equal the bytes that cclsp reads before it
sends `didOpen` or `didChange`.

The anchored route does not call `workspace/symbol` to recover the definition
range. After synchronization, it calls `textDocument/documentSymbol` for the
exact file, requires one matching symbol inside the supplied owner range, and
uses that current position for references. This ordering prevents a stale
workspace-symbol range from surviving a whole-file format.

The first unanchored query is not semantic authority. A wrong candidate file,
namespace mismatch, missing owner, ambiguous owner, stale anchor hash, owner
outside the supplied range, or post-query byte drift refuses before basis
storage. Re-running only the unanchored route or restarting clojure-lsp is not
an accepted remedy for coordinate drift.

The source anchor does not grant cclsp write authority. It also does not let
cclsp claim semantic resolution from syntax alone. If clojure-lsp cannot
resolve the anchored position, cclsp must refuse.

Definition recognition is a separate contract. clj-surgeon needs a syntactic
owner specification. clj-kondo and clojure-lsp need semantic macro analysis.
These configurations are related, but they are not interchangeable:

| Form | clj-surgeon requirement | clj-kondo requirement |
|---|---|---|
| `deftest` | Built-in named-owner classification | Built-in analysis |
| `>defn` and `>defn-` | Explicit owner aliases | Guardrails analysis hook |
| `defcache` | Project owner specification | Project `:analyze-call` hook |
| `mu/defn` | Qualified local-name classification | Malli configuration |

`clj-surgeon up` must discover the effective configuration for both systems.
For a nested workspace, it walks toward the repository root and selects the
nearest `.clj-surgeon.edn` and `.clj-kondo/config.edn`. It configures that
workspace's clojure-lsp process with the effective `:kondo-config-dir`.
It does not copy alias strings into cclsp or translate an arbitrary clj-kondo
hook into a weaker `:lint-as` rule.

The cclsp proof records the effective analysis configuration:

```clojure
{:analysis {:kondo-config-dir "/canonical/repository/.clj-kondo"
            :config-sha256 "..."}}
```

The readiness probe must resolve one custom defining form when the workspace
contains one. For the motivating field case, it must resolve
`server2.views/post-card` and return the same source hash that clj-surgeon
captured. A configuration change invalidates the affected LSP session and its
retained bases.

## Field extension: structural buffers and decision-sized output

The first `scope=definition` field call resolved 26 semantic locations but
overflowed the caller while presenting the result. This is a contract failure.
The semantic provider can produce a large proof, but the model must not carry
that proof as repeated source text.

One prepared basis has three distinct representations:

1. The retained proof contains the complete cclsp result, synchronized source,
   hashes, structural addresses, and rollback inputs. It is private to
   clj-surgeon.
2. The public surface is an ordered vector of compact site maps. It contains
   every proven definition and reference exactly once.
3. The decision viewport contains exact source only for sites that require the
   current model decision.

For example:

```clojure
{:basis "cb-..."
 :operation :rename
 :surface
 [{:id :rename/s01
   :role :definition
   :file "src/example.clj"
   :form "old-name"
   :line 18}
  {:id :rename/s02
   :role :reference
   :file "src/consumer.clj"
   :form "render"
   :line 42}]
 :decision-site-ids [:rename/s01]
 :decision-sites
 [{:id :rename/s01
   :context :form
   :source "(defn old-name ...)"}]}
```

The operation label is optional and cosmetic. If the caller does not supply a
label, clj-surgeon assigns a neutral label such as `:op-226`. A site ID is a
Clojure namespaced keyword scoped by its opaque basis. The complete identity is
`[basis-id site-id]`; two bases can both contain `:rename/s01` without a
collision. Site order is deterministic: definitions first, then references by
canonical file and source range. Site IDs never survive a changed source
snapshot.

The public vocabulary describes what the caller sees:

| Term | Meaning |
|---|---|
| `site` | One definition or reference found in the program |
| `surface` | The complete ordered vector of found sites |
| `form` | The enclosing named `defn`, `deftest`, `defcache`, `>defn`, or similar form |
| `selected code` | The exact expression that can change |
| `containing expression` | The expression immediately around the selected code |
| `decision site` | A site for which the model must choose keep, change, or delete |
| `basis` | The frozen source snapshot and proof retained by clj-surgeon |

Terms such as `owner`, `target`, and `parent` remain implementation vocabulary.
They do not appear in the caller workflow. `owner-authority` can remain in the
proof ledger because it names a precise semantic/source agreement.

`scope=definition` still proves the complete semantic surface. It returns all
locations in `:surface`, but only definition IDs in `:decision-site-ids` and
only definition source in `:decision-sites`. `scope=surface` makes every
surface ID a decision site. Omitted sites are never interpreted as `keep`.

The public result obeys these limits:

```clojure
{:max-retained-proof-bytes 4194304
 :max-decision-source-characters 24000
 :max-public-result-bytes 32768}
```

The last limit is measured after the final MCP result is constructed. Source
must occur in exactly one public representation; display text and structured
content cannot duplicate it. A successful result is never truncated. When a
complete decision packet cannot fit, preparation refuses before publishing a
basis with `:error-type :decision-output-budget-exceeded`, exact required and
limit maps, and one executable narrowing remedy.

### Virtual structural buffers

Every site is a lazy immutable view over the retained basis, not a copied file:

```text
:rename/s01  selected code -> containing expression -> named form
:rename/s02  selected code -> containing expression -> named form
...
:rename/s26
```

The existing `inspect_clojure` tool retrieves one or several site views by
basis and site ID. Context expands structurally, never by an unstable line
window. Supported context levels are `selection`, `containing-expression`, and
`form`. A read uses the retained snapshot and performs no disk reread. Old
views remain frozen after a commit and are never rebound silently.

An MCP resource URI can represent the same address for clients that support
resources, for example `clj-surgeon://cb-.../rename/s01`. The namespaced keyword
is the agent-facing coordinate. The URI is transport plumbing and does not add
a public tool.

### Revisioned decision buffer

For a heterogeneous surface, the model can move partial decisions out of its
working context without changing source. `apply_clojure_changes` accepts
batched staging commands against a basis and returns an immutable draft
revision:

```clojure
{:basis "cb-..."
 :commands
 [{:sites [:rename/s01] :decision {:replace "..."}}
  {:sites [:rename/s02 :rename/s03] :decision :keep}]
 :expect {:new-decisions 3 :revision 0}}
```

The stored expansion is a map from every decided site ID to exactly one
decision. Missing keys remain undecided. A staging response reports changed,
kept, and undecided counts plus the next bounded window of undecided IDs. It
does not mutate project source.

Repeated sites can be selected as one group only when clj-surgeon proves one
structural shape and reports its fingerprint, representative site, exact count,
and zero variants. The compiler retains both the compact commands and their
complete site-by-site expansion.

Final commit requires the expected draft revision and zero undecided sites.
It compiles the complete decision map into the existing hash-fenced,
failure-atomic transaction. A one-site iterator is available through the next
undecided window, but it is a fallback. Batch and proven-group decisions remain
the preferred path because 26 request/response rounds would recreate the
latency this feature removes.

The MCP server never prompts the model autonomously. Each response gives a
bounded next action. The client decides whether to open more structural
buffers, stage another batch, or commit.

This extension has two independent keep gates:

- Definition vocabulary: `>defn`, `defcache`, and other configured macros are
  visible to semantic queries from a nested workspace.
- Shared coordinates: an exact `post-card` anchor cannot resolve or mutate
  `render-post-cards`.

Keep gate:

- one `up` command from an unconfigured checkout;
- no manual ports or launch labels;
- no Codex restart when selecting another already-onboarded workspace;
- one structural inspection and one verified mutation for the remaining
  server2 caller migration;
- canonical-root isolation tests prove that bases, verification configuration,
  paths, and receipts cannot cross workspaces;
- no new public MCP tool names.

## Bitter-Lesson Boundary

This feature moves mechanical consistency checks into the tools. It does not
infer whether a reference should change, interpret the user's intent, or grant
cclsp write authority. clojure-lsp remains the semantic authority; cclsp owns
the versioned bridge result; clj-surgeon remains the exact-source and mutation
authority.

## Public Contract

### Live tool contracts

The MCP initialize result advertises `capabilities.tools.listChanged=true`.
After a successful reload, the server diffs the complete current tool
contracts by name, description, input schema, output schema, and annotations.
It removes missing tools and adds or replaces new or changed tools on the live
SDK server. The SDK emits `notifications/tools/list_changed`; a supporting
client reissues `tools/list` without reconnecting.

Implementation-only Var redefinitions remain visible on the next call and do
not churn the catalog. A failed reload keeps the last-good handlers and tool
contracts registered.

One repository command reloads the MCP namespaces, synchronizes the catalog,
runs the focused MCP gate, and reports the before/after contract hashes and
whether an agent-session restart remains necessary.

### Semantic result

`resolve_var_surface` keeps its tool name and arguments. Version 2 remains
strict: every location must contain a named language-server owner. Version 3
adds explicit owner provenance without weakening the source proof:

```json
{
  "version": 3,
  "operation": "resolve_var_surface",
  "status": "ok",
  "authority": "language-server",
  "lsp_session": "lsp-...",
  "definition": {
    "lsp_session": "lsp-...",
    "file": "src/example.clj",
    "source_sha256": "...",
    "owner_status": "found",
    "owner": "render-status",
    "range": {
      "start": {"line": 10, "character": 0},
      "end": {"line": 12, "character": 20}
    }
  },
  "references": []
}
```

Every definition/reference location uses the same `lsp_session`. `file` is
project-relative and canonical. `range` is the exact LSP range for that
semantic location, not the broader owner range. `source_sha256` hashes the
exact bytes synchronized to the language server for that file. A definition
always has `owner_status=found`. A reference has either a smallest named
document-symbol owner or `owner_status=unresolved` with a null owner. The
latter is not permission to mutate. It delegates only owner classification to
clj-surgeon's hash-matched structural parser.

cclsp refuses rather than returning unproven locations when a location is not
a file, escapes the configured root, cannot be read, or changes while the
semantic request is assembled. If the LSP process changes during the request,
it refuses with a stable session-drift error. Missing document-symbol coverage
is data in version 3, not a guessed owner.

### Basis publication

Before address resolution, clj-surgeon validates:

- one non-empty `lsp_session` exists and all sites repeat it;
- every site contains file, owner status, range, and a 64-hex
  `source_sha256`;
- every file is confined to the configured project;
- cclsp's hash equals clj-surgeon's SHA-256 of the exact captured bytes;
- a language-server owner equals the named structural owner containing the
  exact range; or
- an unresolved version-3 reference resolves to one named structural owner.

Any missing structural owner or disagreement between the provider owner and
the exact-source owner refuses before basis storage. Successful sites report
`owner-authority` as `language-server+exact-source` or `exact-source`.

Mismatch returns:

```clojure
{:ok false
 :error-type :semantic-source-drift
 :file "src/example.clj"
 :lsp-session "lsp-..."
 :provider-hash "..."
 :actual-hash "..."
 :remedy "Retry inspect_clojure prepare-change after the language server catches up."}
```

No basis is stored on any refusal.

## Safety Invariants

- A tool-registry failure never replaces the recorded last-good contract.
- Live synchronization never prints outside the MCP transport or diagnostic
  logger.
- A basis is never published from legacy or incomplete semantic evidence.
- Version-2 owner requirements remain unchanged.
- A version-3 unresolved owner is never accepted without exact-source owner
  resolution.
- All provider paths remain project-confined before any read.
- Hash validation precedes structural address capture and basis-store writes.
- Apply retains its existing full source-hash preflight, atomic write,
  rollback, parse, and verification guarantees.
- One MCP request selects one canonical workspace root. Project-relative paths,
  retained bases, receipts, and verification configuration cannot cross it.
- A language-server timeout can restart only the selected workspace child and
  can retry semantic resolution once. It cannot restart the shared parent or
  unrelated workspace children.
- Interactive semantic requests use a 10-second bound. Cold child
  initialization uses a separate 30-second bound. Restart waits for the old
  child to exit before starting a replacement and reports the complete
  lifecycle receipt.
- A failed or timed-out language-server initialization never publishes a ready
  child. Configured workspaces remain lazy until their first semantic request.

## Test Plan

### Pure matrix

| Dimension | Cases |
|---|---|
| Tool diff | unchanged, add, remove, schema change, description change, annotation change |
| Registry failure | remove fails, replace fails, partial attempt retains accurate registered state |
| Semantic evidence | v2 complete, v2 missing owner, v3 found owner, v3 unresolved owner, missing session, mixed sessions, missing hash, invalid hash, missing range |
| File identity | equal hash, provider stale, disk changes during capture, duplicate locations in one file |
| Range/owner | contained, project macro fallback, boundary positions, wrong provider owner, no structural owner, out-of-root path |

### Boundary and field regressions

- One streamable-HTTP session observes the initial two tools, a schema
  replacement, a temporary third tool, and its removal without reconnecting.
- Initialize advertises `tools.listChanged=true` and the server emits the
  notification for every catalog change.
- A failed namespace reload preserves the previous callable handler and schema.
- A real clojure-lsp/cclsp request returns version-2 proof for a definition and
  multiple references; version 3 additionally proves the project-macro
  fallback on a real caller.
- Mutate a referenced file between cclsp proof and clj-surgeon capture; prepare
  refuses `semantic-source-drift`, leaves the basis store unchanged, and a
  clean retry succeeds.
- The existing real HTTP prepare/decide/apply program remains green.

## Documentation and Release Checklist

- Update README, `CLAUDE.md`, the repo-local clj-surgeon skill, and MCP help.
- Install and use `.claude/skills/lower-cost-of-change/SKILL.md` as the reusable
  development-loop discipline.
- Document the exact hot/warm/cold boundary and one reload/status command.
- Record the closed join and measured feedback-loop improvement in the
  Captain's Log.

## Verification Gates

- Validate the new skill.
- Run focused cclsp tests, full Bun tests, formatting, lint, and typecheck.
- Format changed Clojure files before clj-kondo or tests.
- Run focused MCP tests, HTTP smoke, full `make test`, and `git diff --check`.
- Prove a live schema change in the connected Codex session if the client
  honors `tools/list_changed`; otherwise preserve the exact client limitation.
- Run one real `prepare-change` call and verify all proof fields and the
  clj-surgeon hash agreement.

## Definition of Done

The feature is complete when one connected HTTP client observes a representative
MCP schema change without reconnecting and the same Codex session observes a
reloaded handler without a server or agent restart. If Codex keeps the
model-visible schema text cached, the completion evidence must preserve that
client limitation and name the one new-session boundary. A real
`prepare-change` request publishes a basis only when cclsp's session-bound
per-file SHA-256 evidence equals clj-surgeon's independently captured bytes.
The field request must include a reference inside a project-specific defining
macro and report an exact-source owner. Every adversarial mismatch must refuse
with stable data and no retained basis.

The shared-workspace extension is done only when repeated and alternating
`clj-surgeon up` calls preserve both shared service PIDs and return no config
change for already-onboarded roots. One live client must read and prepare
changes in two canonical roots, and a sibling module must work as its own root
without `../`. A failed multi-change request must name its exact change index,
change ID, field, reason, and remedy while proving source unchanged.

Cross-workspace Var resolution is complete when onboarding publishes
deterministic, confined `sourceRoots` for every configured Clojure workspace.
Given a caller root and one fully qualified Var, cclsp must map the namespace to
candidate `.clj`, `.cljc`, and `.cljs` files, query only candidate workspaces,
and retain the caller root separately from the authoritative owner root. If the
metadata is incomplete, it must query all configured Clojure roots. Zero,
multiple, error, and timeout outcomes are typed refusals; a timeout may not
erase candidates returned by completed workspaces.

## Completion Evidence

| Gate | Evidence |
|---|---|
| Full clj-surgeon suite | 574 primary tests with 5,077 assertions; 111 MCP tests with 977 assertions; zero failures or errors |
| cclsp suite | 289 passing tests with 915 expectations plus 4 execution tests with 23 expectations; 5 intentional unit skips and 2 intentional execution skips; zero failures |
| Static gates | Biome, TypeScript typecheck, Bun build, stdio smoke, benchmark self-tests, retention checks, and `git diff --check` passed |
| Stable installation | `make install` installed the CLI and matching Codex and Claude skills |
| Idempotent shared services | Repeated onboarding preserved clj-surgeon PID 12712 and cclsp PID 68685 |
| Multi-root read | One client read exact forms from the tool checkout and a nested application workspace in 13 ms and 19 ms |
| Multi-root semantic preparation | One client produced separate root-bound bases in 6.0 s and 3.5 s |
| Sibling module | A formerly rejected sibling path worked as its own canonical root and returned a 12-form outline in 427 ms |
| Former readiness failure | `clj-surgeon up` onboarded the previously failing clean checkout without a config change, restart, or PID change |
| Actionable refusal | The production-derived four-change replay named change 0, `gallery-resolver`, field `:find`, reason, remedy, and `source_unchanged=true` |
| Bounded discovery | A wrong named form returned 10 available names and the nearest candidate from the already-read snapshot without returning partial source |
| Mothership semantic recovery | Four anchored surfaces resolved in 2.539 s. A forced child wedge recovered in 12.457 s after the timeout split, down from 32.452 s. |
| Batched related-Var audit | `resolve_var_surfaces` resolves up to four ordered anchored Vars without independent workspace-symbol discovery and retains each complete result or typed refusal. |
| Cross-workspace Var routing | A server2 request resolved sibling `reddit.mongodb.mongodb/start`, the identical SHA, and 81 references by querying one of six configured workspaces: 9.237 s first run and 0.210 s warm versus 27.371 s for all-root fan-out. |

The live health surface showed six configured roots with source-root metadata,
no outstanding requests, and no initialization errors. The
public MCP surface remained exactly `inspect_clojure` and
`apply_clojure_changes`.
