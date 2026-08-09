# Captain's Log: Last Night's Hill Climb

**Date:** 2026-08-08

**Status:** Current synthesis of the five Captain's Logs written from 8:50 PM
through 12:53 AM Pacific. The source logs remain append-only experiment
records. This document states the latest interpretation.

## Bottom line

clj-surgeon crossed three boundaries last night:

1. The mutation kernel became a verified structural transaction compiler.
2. The persistent MCP route demonstrated complete-turn wall-clock wins against
   native controls.
3. The shared semantic and source services learned to preserve one code
   identity across workspaces, LSP sessions, exact bytes, and transactions.

The current product claim is bounded but material:

> For a hot service and a coherent Clojure change, clj-surgeon can preserve one
> model decision as one verified transaction. It can reduce complete agent wall
> time, tool actions, surfaced source, and recovery work. Native patching remains
> the control for one arbitrary text edit.

This is not a claim that every structural route wins. It is not a claim that
direct tool speed determines agent speed. The strongest evidence says the
opposite: a subsecond kernel can still lose when the caller guesses the schema,
reconstructs the request, or performs redundant discovery.

## What the five logs established

### 1. The experiential advantage is reduced mechanical state

One delete/edit/delete transaction changed three sites in three files in 6.2
seconds, including about four seconds of formatting and linting. The caller did
not retain line numbers, write order, partial completion, or rollback state.

Native Clojure patches remained fast for local edits. They required the caller
to retain delimiter state, forward-reference order, formatter cleanup, and the
aggregate relation among implementation, schema, receipts, tests, and help.

The useful distinction is not "structural versus textual" in the abstract:

| Change shape | Preferred route |
|---|---|
| One arbitrary text change | Native patch |
| One exact Clojure form | Choose the cheaper correct route |
| Related Clojure owners or files | One Surgeon transaction |
| JavaScript or prose | Native patch plus its normal verification |

### 2. Persistent MCP, not the CLI, is the product route

The CLI-plus-skill lane was 45.4% slower than native on the supplied six-edit
task. It paid for skill loading, help, process startup, and repeated translation.
That result is not evidence against the persistent MCP design.

The first MCP lane was close to native but sent the wrong request first. A
9.7-millisecond refusal caused 20.46 seconds of model recovery and payload
reconstruction. The transaction kernel used about 349 milliseconds.

After the tool contract made the first request unambiguous, the complete turn
crossed over immediately.

| Correctness-gated stratum | Surgeon median | Native median | Wall reduction |
|---|---:|---:|---:|
| One supplied nested edit | 21.595 s | 26.749 s | 19.3% |
| Six supplied edits in two files | 27.976 s | 68.932 s | 59.4% |
| Exploratory six-edit task | 62.876 s | 81.730 s | 23.1% |

The six-edit supplied route was 2.46 times faster than native. The exploratory
route also won end to end. In both routes, direct MCP read and write work was
subsecond. The gain came from fewer model-tool rounds, less source
reconstruction, and one terminal transaction.

### 3. The public surface should remain two tools

The durable MCP surface is:

```text
inspect_clojure          bounded perception and preparation
apply_clojure_changes    guarded action and verification
```

Whole-site delete, binding-aware local rename, comment-preserving map entry
insertion, sibling insertion, and other mechanics belong inside the transaction
language. They do not require another public tool.

The model owns meaning. cclsp owns resolved semantic relationships and the LSP
proof. clj-surgeon owns exact source, structural addresses, transactions, and
receipts. The formatter, linter, compiler, tests, and live REPL remain program
authorities.

### 4. Operational correctness is part of edit correctness

Last night's failures were often outside the rewrite kernel:

- hot reload advertised a new schema while an old dependency remained loaded;
- a healthy process served an uninitialized tool runtime;
- alternating workspace onboarding restarted cclsp because config order drifted;
- a wedged child remained marked ready;
- stale MCP session errors returned the wrong JSON-RPC ID;
- one nested project used the wrong classpath;
- several semantic calls hid queue state;
- a sibling Var caused a six-workspace semantic fan-out.

The fixes established a stronger rule:

```text
process alive != tool ready
functional health + one bounded real request = ready
```

The shared stack now uses one Surgeon process, one cclsp facade, lazy
workspace-specific clojure-lsp children, exact workspace identity, bounded
semantic scheduling, request telemetry, and exact-root recovery.

### 5. The semantic/source join became explicit

The proof handoff now binds each result to:

- one LSP session;
- one canonical workspace;
- one project-relative and absolute file identity;
- one exact source SHA-256;
- one range;
- one source owner or one explicit unresolved-owner state.

clj-surgeon independently reads and hashes the source before it retains a
basis. Missing evidence, mixed sessions, stale bytes, path disagreement, and
owner ambiguity refuse before mutation.

The latest sibling-workspace repair added provider selection. A server2 query
for a Var in a configured sibling changed from six LSP workspaces and 27.371
seconds to one workspace and 9.237 seconds cold. The warm replay took 0.210
seconds and returned the same definition SHA and 81 references.

## What is complete now

The following field defects from the logs are closed:

- tool/runtime readiness disagreement;
- binding-aware local rename with external-key preservation;
- comment-bearing nested map edits;
- bounded parallel semantic scheduling and progress;
- stale-session request correlation;
- bounded LSP timeout recovery;
- direct apply schema and validator disagreement;
- sibling-workspace fully qualified Var resolution.

The full gates at the end of the hill climb were:

| Repository | Result |
|---|---|
| clj-surgeon primary | 574 tests, 5,082 assertions |
| clj-surgeon MCP | 111 tests, 977 assertions |
| cclsp unit | 289 tests, 915 expectations |
| cclsp execution | 4 tests, 23 expectations |
| Static and delivery | formatter, Biome, typecheck, build, smoke, diff check, install |

## The three open P1 contracts

Do not add a broad new feature before these close.

### `clj-surgeon-0ck`: publish exact-source preparation

The documentation and handler support `mode=prepare-change` with an exact
project-relative `file` and top-level `form`. The live schema omits those
fields. A caller with exact source evidence therefore tried a private Var name,
paid 17.5 seconds, and received `semantic-var-not-found`.

Fix the single-source schema contract. Prove that one private or unindexed owner
returns `authority=exact-source` and a usable `next_call` without semantic
claims.

### `clj-surgeon-g08`: canonicalize workspace aliases

`/tmp` and `/private/tmp` can identify the same macOS directory. The blocked
delete/edit/delete benchmark refused because cclsp and clj-surgeon used
different spellings for one workspace.

Canonicalize identity before session creation, semantic requests, anchors, and
evidence comparison. Cover both the macOS alias and a repository symlink. Then
rerun the blocked three-site benchmark against a fresh native control.

### `clj-surgeon-5ss`: make concurrent onboarding lossless

Two simultaneous `clj-surgeon up` commands each reported success, but one
workspace update disappeared from the shared config.

Serialize or atomically merge the read-modify-write step. A successful command
must verify that its exact server block survived. The regression must run real
concurrent registrations and preserve unrelated server entries.

## Product vision

The product is becoming a structural exocortex for Clojure agents. That phrase
is useful only when it names observable behavior:

```text
exact perception
  -> resolved semantic relationships
  -> one explicit model decision
  -> one compiled structural transaction
  -> one verified receipt or one actionable refusal
```

The tool should make mechanical execution boring. It should not make
architecture automatic. The Bitter Lesson boundary remains intact:

- invest in general perception, addressing, cardinality, preservation, and
  replay;
- let stronger future models supply paths, interpretations, architecture, and
  replacements;
- do not encode one special refactoring opinion for every production incident;
- add an internal operator only when repeated field evidence identifies a
  general mechanical gap.

The target is not universal adoption. The target is a reliable comparative
advantage on the changes where structure matters.

## Specific recommendations

### Priority 1: close the control-plane triangle

Close `0ck`, `g08`, and `5ss` as small batches. Each fix should dogfood the
improved route in the next fix. These defects affect request truth, coordinate
truth, and onboarding truth. New editing syntax has lower leverage until all
three are reliable.

### Priority 2: finish the blocked benchmark stratum

After `g08`, rerun four correct Surgeon replicas and four matched native
controls for the delete/edit/delete task that starts from one Var. This is the
best current test of the full product loop:

```text
semantic discovery -> compact decisions -> mixed delete/replace -> verification
```

Do not infer its result from the 6.2-second direct transaction.

### Priority 3: make schemas executable specifications

Generate the live schema, descriptions, examples, runtime validator, and
`next_call` from one route contract where practical. Run every published
example through the production validator. A clean caller should never learn a
field through refusal.

The acceptance metrics are:

- first call accepted;
- zero help or skill-reference calls;
- zero recovery rounds;
- no source reread after terminal evidence;
- exact final bytes and verification receipt.

### Priority 4: optimize rounds, not milliseconds

Profile the kernel only when direct execution is no longer subsecond. The large
observed wins came from deleting complete rounds. Preserve phase timings for:

- evidence acquisition;
- decision latency after sufficient evidence;
- request construction;
- direct tool execution;
- refusal recovery;
- verification;
- final response.

Do not classify every quiet interval as model judgment.

### Priority 5: keep the default cognitive surface fixed

The default skill must remain within its 70-line budget. Put operational truth
in the typed schema and compact tool description. Keep CLI detail in an
on-demand reference. Continue to test the primary Codex and Claude entrances
for byte identity and recognition phrases.

### Priority 6: preserve the strongest control

Use native patching for one arbitrary local text edit, JavaScript, and prose.
Do not weaken the native lane to manufacture a Surgeon win. Add a Surgeon
operator only when structure removes enough discovery, ambiguity, repeated
payload construction, or unsafe replay to beat that control.

### Priority 7: treat formatting debt as an explicit migration

For a legacy target file, format the file before preparing the semantic basis.
Run the repository's real tests and keep the normalization separate from the
semantic change. Do not silently format thousands of unrelated lines inside a
structural transaction. Do not weaken the formatter gate to accommodate debt.

### Priority 8: retain one shared stack

Do not return to per-repository MCP servers. `clj-surgeon up` must own shared
registration, functional readiness, source roots, verification profiles, and
one compact receipt. clojure-lsp children remain lazy and workspace-specific.

### Priority 9: define the 9/10 field gate

A clean agent in a real repository should complete this route without coaching:

```text
clj-surgeon up
  -> one inspect or prepare call
  -> one model decision
  -> one apply call
  -> terminal answer
```

The route earns 9/10 when it has no shell fallback, schema repair, source
reread, service restart, or recovery archaeology. A refusal can still earn the
gate when it is bounded, names the violated contract, proves source unchanged,
and supplies one executable remedy.

## The runtime microscope found a false graph failure

The first exact-source anchor experiment appeared to move the bottleneck from
`textDocument/documentSymbol` to `textDocument/references`. The anchored path
correctly removed document-symbol discovery, but the shared service still
timed out after 55 to 60 seconds. A direct clojure-lsp CLI control returned the
same two references in 25.06 seconds.

The investigation initially produced a misleading positive result. A
Codex-owned stdio cclsp process returned the references in 6.1 seconds, but it
was using the clj-surgeon workspace rather than the target worktree. Its answer
was not authoritative for the target graph. The shared HTTP process and the
stdio process had different configuration, environment, children, caches, and
lifetimes, but no single tool exposed that difference.

`inspect_runtime` was added as a bounded read-only MCP control surface. Its
default response is three lines. Its structured response exposes the server
instance, config identity, workspace, LSP session, child PID, outstanding
JSON-RPC calls, semantic queue, recoveries, and initialization errors. The
first dogfood call showed the exact live state:

```text
cclsp 0.8.2 · cclsp-84038-...
1 workspace · 1 initialized
1 active semantic · 0 queued · 1 outstanding LSP
```

The named outstanding call was `textDocument/references`. The child PID then
led directly to its clojure-lsp log, which contained the actual defect:

```text
Cannot run program "clojure": No such file or directory
```

The shared launchd service started clojure-lsp by absolute path, so the native
binary ran. Its inherited PATH omitted Homebrew, so clojure-lsp could not run
the separate `clojure -A:test:dev -Spath` subprocess needed to build the
project graph. It loaded a 142 MB stale cache and completed the generic LSP
handshake, which made the service look initialized while authoritative
references could never complete.

The durable launcher now passes the invoking shell's complete PATH into the
managed cclsp service. A behavioral shell test proves the launchd command
retains it. After the service restarted with that PATH, the same cold anchored
query completed in 18.76 seconds with the same two references and one exact
LSP session.

| Route | Result | Wall time |
|---|---:|---:|
| Shared MCP before PATH repair | timeout | 55–60 s |
| clojure-lsp CLI control | correct | 25.06 s |
| Shared anchored MCP after repair, cold child | correct | 18.76 s |
| Shared anchored MCP after repair, warm child | correct | 5.56 s |

The repaired cold MCP route was 25% faster than the CLI control; the warm route
was 78% faster. Both returned the same two references and removed the
`documentSymbol` round entirely. More important, the diagnosis changed from
process archaeology to one typed state query. The runtime inspector is not a
new editing primitive; it is the missing observability surface for the one
shared stack.

The installed recovery path then exercised the complete contract against the
same worktree. In 18.58 seconds it proved the tool catalog, one definition and
two references from one LSP session, applied and verified a guarded mutation
to a disposable probe, removed the probe, and returned
`:terminal-state :recovered` with `:next-action :none`. Neither shared service
nor the agent session restarted.

## The complete graph was not the problem

A later workspace appeared to falsify the shared-stack design. Its
clojure-lsp cache was 216 MB, native references exceeded 116 seconds, and the
shared provider repeatedly exceeded its 45-second initialization budget. One
tempting remedy was to remove test or development source aliases from the
workspace graph.

That remedy was rejected. A caller surface that silently omits configured test
or development code defeats the product. The semantic proof must cover the
full configured graph or name its negative space explicitly.

Rebuilding the pathological cache reduced it from 216 MB to 28 MB. The same
full native reference query then completed in 13.01 seconds and returned the
same three sites. A fresh cclsp process, with the same source roots and parent
kondo configuration, returned the same surface in 11.67 seconds. The graph was
fast; the long-lived shared runtime was not.

Two runtime defects explained the remaining failures:

1. a stateful cclsp runtime could survive core TypeScript changes while the
   stable URL and newer tool surface still looked current; and
2. `make cclsp-start` defaulted to the repository's one-workspace fixture
   instead of the managed multi-workspace registry.

The first defect produced false-green health. The second made a controlled
restart silently forget six registered workspaces. Neither defect justified a
smaller semantic graph.

The repaired contract is explicit:

- `/healthz` returns 503 with `runtime_current=false` and
  `restart_required=true` when stateful source is newer than the live runtime;
- the managed launcher defaults to
  `~/.local/state/clj-surgeon/cclsp.json`;
- `restart_server` can initialize an exact configured workspace even when its
  previous child already exited; and
- readiness is proven by an exact Surgeon source anchor followed by resolved
  references, not by fuzzy workspace-symbol discovery.

The final production-shaped proof used the complete graph:

| Stage | Result | Wall time |
|---|---:|---:|
| Native reference query before cache rebuild | interrupted | >116 s |
| Native reference query after cache rebuild | 3 references | 13.01 s |
| Fresh cclsp, same full configuration | 3 references | 11.67 s |
| Root-scoped recovery from no running child | initialized | 4.68 s |
| Anchored semantic proof after recovery | 3 references | 3.01 s |

This is the intended comparative advantage: retain the whole semantic world,
repair only the broken workspace, and turn an exact structural witness into a
small resolved proof. Never make a faster claim by hiding callers.

## What not to do next

- Do not add another editing or navigation MCP tool without a measured route
  collapse. Keep `inspect_runtime` as the bounded observability exception for
  the shared stack.
- Do not build an autonomous refactoring oracle.
- Do not optimize cold per-task startup instead of preserving the shared hot
  service.
- Do not interpret direct tool time as complete task performance.
- Do not expose the full semantic surface when one compact proof and a small
  decision viewport are sufficient.
- Do not make Clojure source look like canonical printed data when exact source
  spelling and comments matter.
- Do not generalize the current result to JavaScript or prose.

## Success horizon

The 3x ambition is appropriate for coherent multi-site changes, not for every
edit. The supplied six-edit task already reached 2.46x. The next large gain
should come from compiling semantic discovery and mixed structural decisions
into one basis transaction, not from shaving another 100 milliseconds from the
kernel.

The global optimum is not "Surgeon everywhere." It is:

```text
the model spends time on meaning
the tools spend time on mechanics
the complete correct turn gets shorter
```

That is the standard by which the next hill climb should be judged.
