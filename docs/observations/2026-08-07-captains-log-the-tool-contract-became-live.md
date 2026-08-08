# Captain's Log: The Tool Contract Became Live

**Date:** 2026-08-07

## Question

Can we close the semantic/source proof gap without paying a server and coding-
agent restart for every schema experiment?

## The first join gap was in the development loop

The MCP protocol already had the missing mechanism. A server can advertise
`tools.listChanged=true`, replace its live registry, and send
`notifications/tools/list_changed`. A supporting client then requests
`tools/list` again.

`itrev-mcp` had already implemented and tested this path. clj-surgeon instead
contained this explicit capability:

```clojure
(.tools false)
```

The repeated Codex restarts were therefore not an inherent MCP cost. They were
a clj-surgeon product defect.

## The feedback loop became a feature contract

We created the repo-local Claude Code skill
`.claude/skills/lower-cost-of-change/SKILL.md`. It treats the complete loop as
the unit of optimization:

```text
edit -> load -> discover -> exercise -> verify
```

It classifies each boundary as hot, warm, or cold and fixes the most frequently
paid cold boundary first. It also adds one rule exposed by this session: use
the hottest capable entrance. Do not start a CLI when a connected persistent
tool provides the same contract.

That rule caught our own behavior. Despite a live `inspect_clojure` tool, the
agent repeatedly used `~/bin/clj-surgeon`. The strongest instruction at the top
of the working skill said to use the tool "from PATH"; detailed CLI recipes
then outweighed the smaller MCP nudge. Repository instructions now state the
entrance order explicitly:

```text
MCP -> cclsp graph query -> CLI fallback -> native fallback
```

The large CLI manual will move behind an on-demand reference. The default
skill will retain only MCP routing, the prepare/decide/apply workflow, safety
conditions, and terminal receipt rules. MCP does not eliminate guidance, but
it eliminates operation discovery, shell quoting, EDN flag encoding, and
stdout interpretation.

## Red then green

The first focused test failed because `server/tool-contracts` did not exist.
The implementation added a pure contract diff that excludes handler Vars, so a
handler-only reload does not churn the tool catalog. Schema, description,
annotation, addition, and removal changes produce explicit synchronized
operations.

A second boundary test caught a test-helper assumption: initialize returned
plain JSON while tool calls returned SSE-framed JSON. The helper now parses
both real wire forms instead of weakening the assertion.

The initial focused result was:

| Evidence | Result |
|---|---:|
| MCP tests | 54 |
| Assertions | 538 |
| Failures/errors | 0 / 0 |
| Live-session catalog path | replace, add, remove |
| Advertised capability | `tools.listChanged=true` |

One streamable-HTTP session observed a changed description, a third temporary
tool, its removal, and the restored two-tool catalog without reconnecting.

The live dogfood process required one bootstrap restart to load the new
registry machinery. The same Codex session then called `inspect_clojure`
successfully through the stable URL. Future Clojure and schema changes use
`make mcp-reload`: focused test first, nREPL reload second, live registry sync
third.

The end-to-end catalog probe preserved a negative result. The server replaced
both tool descriptions and reported them synchronized without reconnecting.
This Codex turn continued to expose the old descriptions in its model-visible
catalog. Calls stayed hot and used the new handlers, but Codex did not re-list
the changed contracts at that boundary. `tools/list_changed` removed the
server restart; it did not remove this client's catalog-refresh boundary.

## The next join is semantic identity to exact bytes

The proof-carrying basis still has one unacceptable inference. cclsp returns a
language-server location. clj-surgeon independently reads the file and assumes
that the location describes those bytes. A stale LSP index could point inside
a valid but wrong owner.

The closed handoff requires every semantic location to carry:

```json
{
  "lsp_session": "lsp-...",
  "file": "src/example.clj",
  "source_sha256": "...",
  "owner": "render-status",
  "range": {
    "start": {"line": 10, "character": 0},
    "end": {"line": 12, "character": 20}
  }
}
```

cclsp must bind all locations to one LSP process session and to bytes that were
synchronized to that process. clj-surgeon must independently hash its captured
bytes before it resolves structural addresses. A mismatch must return
`semantic-source-drift` and publish no basis.

That join is now focused-green on both sides. cclsp synchronizes changed open
files with `didChange`, gives every LSP process a session UUID, reruns the
semantic query after synchronizing all discovered files, and hashes the disk
again without resynchronizing before it publishes version-2 evidence.
Ownerless references and post-query byte drift refuse.

clj-surgeon now rejects legacy evidence, missing proof fields, mixed sessions,
conflicting file hashes, paths whose relative and absolute forms disagree, and
provider hashes that differ from its own source snapshot. The basis store
remains empty after refusal. The current focused gate is:

| Evidence | Result |
|---|---:|
| MCP tests | 58 |
| Assertions | 570 |
| Failures/errors | 0 / 0 |
| Single-subject proof basis | green |
| Multi-subject proof union | green |
| Mixed-session union retains a basis | no |

## The real customer widened the contract

A paused media refactor supplied the first demanding customer. Its Clojure
decision crosses five core namespaces and several related Vars. It must
centralize mechanics without erasing deliberate layout and loading profiles.
The worktree also contains unrelated edits, so broad replacement is forbidden.

One `subject` per basis would force the model to carry and reconcile several
partial inventories. That contradicts the product criterion. `prepare-change`
therefore now accepts either one `subject` or an ordered, unique `subjects`
array. It resolves each exact Var, requires one shared LSP session, unions and
deduplicates structural owners, preserves the caller's subject order, and
returns one decision surface and one next call.

The field customer exposed a deployment gap too. The connected clj-surgeon
MCP is confined to this repository. The paused agent works in a sibling
repository and therefore fell back to the installed CLI even though the MCP
process was hot. A repo-rooted MCP entrance is part of the acceptance contract,
not optional setup. The refactor stays paused until a clean agent can call the
new multi-subject route against its own workspace.

## Dogfooding drew blood

The structural read surface felt strong. Batched named-form reads removed line
bookkeeping, returned coherent hashes, and made repeated CLI startup feel
unnecessary. Exact structural replacement also felt calm when the complete
decision already existed.

The unsupported case was revealing. Adding new top-level forms required a
native patch. Delimiter repair kept the file parseable but changed its meaning
twice: one repair separated `(cond)` from its clauses; another changed a
two-argument `addressed-form-at` call into a zero-argument call followed by two
unrelated expressions. Focused tests caught both. Parseability was not enough.
This is direct evidence for the thesis that the transaction compiler must own
mechanical edit state.

Three smaller defects also remain recorded rather than normalized away:

- one success text reported zero edits while structured evidence reported one;
- the advertised `each_file` field was rejected by request validation;
- server-side contract reload succeeded while Codex retained stale descriptions.

## The goal was reactivated at the delivery boundary

The goal was reactivated after the proof kernel became focused-green. That is
the correct scope. The work is not complete merely because the producer and
consumer tests agree. The outcome must also be reachable from the repository
where the first broad refactor is paused.

Reactivation preserves five remaining gates:

1. One repo-owned command starts an isolated, repo-rooted cclsp and
   clj-surgeon stack.
2. The command installs a bounded project-local Codex configuration without
   replacing unrelated settings.
3. A real multi-subject `prepare-change` request proves one LSP session, exact
   owner/range evidence, and matching source hashes in that workspace.
4. The default Codex and Claude skills present the small MCP-first contract;
   the CLI manual remains an on-demand fallback.
5. Focused and full gates pass in both repositories before installation.

This is not scope creep. It is the difference between a tested mechanism and
a delivered capability. The acceptance test remains read-only: the paused
customer agent receives one verified basis, but does not resume its refactor
until the tool stack is installed and the evidence contract is proven.

## The real refactor became the acceptance test

The repo-rooted entrance is now one idempotent command:

```bash
make workspace-mcp-onboard WORKSPACE=/absolute/repository
```

It starts isolated cclsp and clj-surgeon services, writes one canonical cclsp
root, and manages a bounded block in the repository's `.codex/config.toml`.
The first field run found an existing canonical cclsp table. The naive append
would have produced duplicate TOML tables. The installer now migrates only the
related cclsp and clj-surgeon tables, preserves unrelated bytes, parses the
result with `tomllib`, and is idempotent. The second real run reported
`changed=false` and exactly two configured servers with six enabled tools.

Cold startup exposed another operational boundary. A fresh clj-surgeon JVM
could exceed the original 30-second readiness loop. The launcher now permits
60 seconds while retaining the same health and process checks.

The first four-Var field request refused before basis storage. clojure-lsp had
found a real `media-view` call inside a project-specific `>defn post-card`
form, but its document-symbol surface did not name that owner. Treating this as
a permanent provider failure would make custom macros invisible. Guessing an
owner in TypeScript would move source judgment into the wrong layer.

The join therefore became version 3:

```text
cclsp       proves session + file + range + synchronized SHA-256
             and reports owner_status = found | unresolved
clj-surgeon verifies a found owner against exact source
             or derives an unresolved owner from exact source
either       disagreement or no named structural owner -> no basis
```

Version 2 remains strict. New adversarial tests preserve its owner requirement
and prove version-3 project-macro recovery, wrong-owner refusal, ownerless-form
refusal, and an empty basis store after each failure.

The second field request crossed the semantic join and stopped at the output
budget: 14 sites, 24,924 visible characters, and 199,068 snapshot characters.
The site and snapshot budgets were healthy; the old 12,000-character visible
limit was too small for the actual coherent decision. The measured limit is
now 32 KiB, with a regression test fixing that bounded contract.

The third field request succeeded:

| Evidence | Result |
|---|---:|
| Requested Vars | 4 |
| Structural sites | 14 |
| Files | 8 |
| Visible source | 24,924 characters |
| Shared LSP sessions | 1 |
| Exact-source macro owners | 1 (`post-card`) |
| Other independently verified owners | 13 |
| `read_complete` / `source-unchanged` | true / true |
| Executable `next_call` | present |

No media source changed. The result is one retained, hash-fenced basis for the
paused refactor.

## Dogfood corrected the tool route again

During implementation, the agent still used several CLI `:ls` and `:cat`
calls even though the connected persistent MCP exposed the same reads. The
reason was operator error, not technical unavailability: the tools were
deferred from the initial catalog, and the agent skipped the repository's
explicit instruction to query that catalog before falling back. The CLI calls
therefore tested the wrong entrance and weakened the dogfood evidence.
After correction, one MCP batch found two parseable semantic corruptions from
delimiter repair: a shadowed owner-line variable and a prematurely closed
`let`.

One `apply_clojure_changes` transaction then replaced both complete forms and
verified two edits in one file. A second transaction changed the measured
budget and its contract test across two files. The experience matched the
design: state the complete decision once, receive one verified transaction.

Dogfood also made the remaining interface defects concrete:

- the generated union schema makes basis-only `verify` appear valid on the
  direct `changes` route;
- successful direct transactions still render “Applied 0” while structured
  evidence correctly reports the edit and file counts;
- top-level namespace changes cannot use a named-owner direct transaction;
- the default skill must instruct agents to query deferred MCP tools before
  starting the CLI.

These are completion work, not observations to normalize away.

The compact skill now starts with deferred-tool discovery and the ordered
entrance `inspect_clojure -> cclsp graph query -> CLI fallback -> native
fallback`. The complete CLI contract moved to an on-demand reference. Tests
require both surfaces: the default skill remains at most 70 lines, while the
fallback retains X-ray, navigation, transaction, plan, refusal, and receipt
guidance.

The response-summary defect then became the live-reload probe. Before reload,
the transaction engine returned 4 edits across 1 file, but the already-loaded
summary function rendered 0 and 0. `make mcp-reload` ran the 69-test focused
gate, reloaded the namespaces, and synchronized both tools. PID 8637 and port
7888 did not change. The same Codex session then applied a two-edit temporary
probe and rendered the correct summary:

```text
Applied 2 structural edit(s) across 1 file(s); verification complete.
```

The server contract is live. One connected HTTP session also observes schema
replacement, addition, and removal. The remaining boundary belongs to the
current Codex client: it continues to cache model-visible schema text after
`tools/list_changed`. New handlers are callable immediately; a newly added
argument can require a new Codex session even though the MCP server never
restarts.

The key product boundary remains unchanged:

```text
clojure-lsp  owns resolved semantic relationships
cclsp        owns the versioned proof handoff
clj-surgeon  owns exact source, addresses, transactions, and receipts
the model    owns the change decision
```

## The classpath is part of the address

The first real customer exposed one more join boundary. The repository has a
parent `deps.edn` and an independently runnable `server2/deps.edn`. A cclsp
process rooted at the parent could read paths under `server2`, but it could not
resolve the nested project's Vars. `resolve_var_surface` therefore failed even
though clj-surgeon could return the exact source form.

This was not a source-reader failure. It was a semantic-address failure:

```text
same pathname + wrong classpath = wrong program
```

Running both services from `server2/` closed the immediate gap. One live
`prepare-change` request then proved 14 definition and reference sites across
8 files, from one LSP session, with exact source owners and source hashes. The
media refactor can now start from a coherent decision basis instead of a pile
of separately remembered reads.

Dogfooding also found that verification belongs to the project. The default
transaction verifier invoked the nested cache and reported three false
unresolved Vars from the parent library. The command that represents this
project correctly is:

```sh
clj-kondo --config-dir ../.clj-kondo \
  --cache-dir ../.clj-kondo/.cache \
  --lint <changed-files>
```

The MCP server now reads a closed-data `.clj-surgeon.edn` verification profile
at startup. The customer workspace declares that command once; every later
transaction inherits it. Invalid profiles refuse before execution. Readiness
reports whether the profile came from the project or the built-in default.

The durable generalization is one cclsp facade over several explicit semantic
projects, not one accidental classpath for an entire directory tree:

```text
one cclsp MCP
  -> parent project  -> lazy clojure-lsp process
  -> server2 project -> lazy clojure-lsp process
  -> another app     -> lazy clojure-lsp process
```

File-specific queries route to the deepest owning project. Workspace queries
fan out and retain `semantic-project`, `lsp-session`, and source-hash
provenance. Ambiguous ownership refuses. Automatic discovery may propose
candidate `deps.edn` files, but configuration must decide which files describe
real semantic projects; examples and tooling directories must not silently
consume an LSP process.

This also sharpens the one-shot onboarding target. `clj-surgeon tools up`
should discover candidate projects, write the local MCP configuration, allocate
services, start each semantic worker lazily, verify one real query per project,
and return one compact readiness receipt. The user should choose projects, not
debug ports, caches, or classpaths.

## Previous delivery gate

The proof join, multi-subject decision surface, and repo-rooted entrance are
complete. The customer workspace reports both services ready. Its live
clj-surgeon contract changed from `dfebe2bf` to `c6d0f4e4` while PID `80521`
remained unchanged. The reload upserted `apply_clojure_changes` and reported
`server-restart-required=false`.

The final verification matrix was:

| Gate | Result |
|---|---:|
| clj-surgeon full suite | 552 tests, 4,902 assertions |
| clj-surgeon focused MCP suite | 69 tests, 635 assertions |
| cclsp full suite | 266 passed, 5 skipped, 825 assertions |
| Codex and Claude primary skill | 59 lines, byte-identical |
| Skill validation | 3 / 3 valid |
| Strict primary-skill text lint | 0 findings |
| Stable installation | CLI, Codex skill, and Claude skill with receipts |
| Repository diff check | clean |

The full clj-surgeon suite first failed with 155 documentation-contract
failures. The failures exposed missing CLI details after the primary skill was
compacted. We moved those details to an on-demand reference and retained every
old assertion. The later assertion total is smaller only because the malformed
help indentation no longer invents two fake operation names and their four
dynamic failure assertions; no test was removed or weakened.

The coding-agent restart boundary is now exact. Handler behavior and server
tool contracts reload without a JVM restart. Connected MCP clients that honor
`tools/list_changed` can relist without reconnecting. The current Codex client
can cache model-visible schemas for a turn, so a newly added argument can still
require one new Codex session. That client limitation is preserved rather than
misreported as a server limitation.

The paused field agent can now resume from its own repository with one
multi-subject `prepare-change` request. Its basis will exist only when cclsp's
single-session ranges and file hashes agree with clj-surgeon's exact snapshots.
The model decides the refactor once; the joined tools carry the addresses,
proof, drift checks, and transaction state.

## One shared server replaced one server per repository

The repo-rooted stack proved the semantic join, but it left the user owning
ports, launch jobs, state directories, and duplicate MCP registrations. That
was the wrong abstraction. Workspace identity belongs in the request, not in
the process topology.

The new entrance is:

```bash
clj-surgeon up [WORKSPACE]
```

It joins one canonical workspace to one shared clj-surgeon server and one
shared cclsp facade. `inspect_clojure` and `apply_clojure_changes` accept an
optional canonical `workspace_root`. The router creates isolated lazy
per-root contexts, loads that root's verification profile, strips the routing
field before inner validation, and refuses invalid, missing, or relative roots
as data. Omitting the field preserves the original single-root behavior.

Prepared changes now retain their workspace. Application independently
canonicalizes both roots and refuses a basis from another workspace before any
write. The compatibility test was valuable: temporary directories on macOS
can spell the same path as `/var/...` and `/private/var/...`. Comparing raw
strings rejected valid same-workspace bases. Comparing both real paths fixed
the address without weakening the guard.

The shared cclsp configuration keeps one explicit Clojure server entry per
canonical semantic root and preserves unrelated language servers. A changed
root registry restarts cclsp once; an unchanged registry reuses the live
process. The clj-surgeon JVM stays hot because its workspace contexts are lazy.

## Read and write receipts now have the same shape

The read surface established the desired visible contract:

```text
inspect_clojure
  2 requests · 2 files · 2 forms

✓ all requests resolved
✓ ordered snapshot
✓ hashes attached
✓ terminal evidence · read_complete=true · next action none
```

The write surface still exposed a large serialized receipt. That confused two
different audiences. The model needs the complete structured receipt; the
human needs the terminal result and the next action.

The live write surface now renders:

```text
apply_clojure_changes
  1 edits · 1 files

✓ failure-atomic commit
✓ read-back verification complete
✓ terminal evidence · verification_complete=true · next action none
```

The same response still carries the canonical workspace, changed-file count,
read-back hashes, receipt hash, undo receipt, and full verification as
`structuredContent`. Refusals remain compact but include the stable reason,
field path, unchanged-source statement, and executable remedy. Direct callback,
HTTP, and hot-reload tests pin both branches.

Dogfooding found one reload trap. Reloading a test namespace does not reload
the implementation namespace it requires. The first warm test therefore ran
the old workspace comparison and failed. Reloading the changed implementation
first made the unchanged test set pass. The durable inner loop is now:

```text
change implementation -> reload implementation -> reload tests -> run tests
```

## The first shared-workspace field proof

The first real onboarding targeted the paused media-refactor workspace.

| Run | Wall time | cclsp config | Codex config | Process action |
|---|---:|---|---|---|
| First `up` | 5.5 s | changed | changed | restart cclsp; reuse surgeon |
| Second `up` | 1.3 s | unchanged | unchanged | reuse both servers |

The second receipt reported `restart-required=false`. It did not rewrite the
project configuration or restart cclsp.

An HTTP call then addressed the customer workspace through the shared surgeon
process that was started from the clj-surgeon checkout. It selected
`resolve-post-media` from the customer source in **10.72 ms** and returned:

| Evidence | Result |
|---|---:|
| Requests / files / forms | 1 / 1 / 1 |
| Source characters | 547 |
| Canonical workspace | exact customer root |
| Ordered snapshot | yes |
| File and form SHA-256 | attached |
| `read_complete` / next action | true / none |

This closes the process-topology part of the acceptance test: one hot server
can serve exact, hash-bound structure from more than one repository without
confusing their verification profiles or source roots.

## Current bottom line

The MCP calls are now delivering almost exactly what the agent needs. Reads
return a bounded decision surface. Writes return a compact terminal receipt
while preserving complete machine evidence. A new workspace joins the shared
stack in one command, and the warm no-op path is cheap.

The focused gate after these changes is **79 tests, 714 assertions, zero
failures or errors**. A live temporary edit proved the new compact write
surface; its fixture was moved to Trash after verification.

The remaining work is delivery, not proof-kernel design: replace the legacy
per-workspace Make targets with compatibility aliases for `clj-surgeon up`,
make the installed entrance carry the runtime it needs, update both agent
skills and the README, dogfood one verified write in the customer workspace,
and run the full clj-surgeon and cclsp gates. The client-side schema cache still
requires a fresh coding-agent session when a tool gains a new argument; the
server itself no longer restarts for that change.

## The first shared write refused twice—and specified the next operator

The process migration completed after the first routing proof. Three surgeon
JVMs and three cclsp processes became one surgeon JVM and one cclsp process.
The shared cclsp registry now contains the clj-surgeon, media-customer, and
writing-application roots. The four superseded per-repository launch jobs were
removed only after all roots were present.

The first cross-workspace `prepare-change` then failed because the live reload
manifest omitted `mcp-semantic-client`. Exact-form reads had not exercised that
dependency. The manifest now includes paths, workspace routing, semantic
client, change buffer, inspect, writer, and server namespaces. A regression
test pins the set. The identical request then returned 7 sites across 6 files
from one LSP session in 3.5 seconds.

The requested customer edit was intentionally small: remove one obsolete Zoom
button from a large `post-card` owner. The client did not retype the owner. It
derived the candidate from the retained exact source and submitted one basis
decision. Both attempts refused safely:

| Attempt | Semantic gate | Formatting gate | Result |
|---|---:|---:|---|
| First derived owner replacement | failed | not reached | full rollback |
| Project-aware profile | passed | failed | full rollback |
| Corrected parent closers | passed | failed | full rollback |

The first failure exposed a stale per-workspace verification profile. Workspace
contexts now reload `.clj-surgeon.edn` for every basis application. The second
failure exposed that the legacy 2,400-line file is not globally Standard
Clojure Style clean before the edit; formatting the disposable candidate would
rewrite thousands of unrelated lines. A whole-file formatter check therefore
cannot distinguish new damage from inherited debt in that file.

The more important finding is cognitive. The client kept only the intent and
site ID in working context, but it still fetched 18,354 characters, spliced a
116-line owner, reasoned about parent closers, and sent the giant form back.
That is not yet compiling a decision. It is a client-side macro around the old
transaction.

The field case specifies the missing basis operator:

```json
{
  "site": "s3",
  "edit": {
    "inside": "post-card",
    "find": "(when (= media-type :video) ...)",
    "delete": true
  }
}
```

The server must own the retained source, exact match count, attached leading
comment, parent delimiters, changed-region formatting, address drift, rollback,
and receipt. The model should send the small structural intent, not an
owner-sized replacement. Several such decisions should be queueable against
one basis and compiled into one transaction.

The visible write copy was sharpened during this run too. “Failure-atomic
commit” was technically correct but visually read as a checked failure. The
live receipt now says:

```text
✓ atomic commit complete
✓ written bytes read back and verified
✓ terminal evidence · verification_complete=true · next action none
```

Keyword error types now render by name, and a failed verification claims
“source unchanged” only when the source never changed or rollback succeeded.
An incomplete rollback instead requires structured-receipt review. The gate is
now 79 focused MCP tests and 718 assertions, all green. No customer source
changed during the failed field attempts; both writes were read-back verified
after rollback, and the disposable formatting candidate was moved to Trash.

## The reader/writer became a structural exocortex

The compact operator crossed its first self-hosting gate. clj-surgeon changed
clj-surgeon through the live MCP:

~~~text
compact-delete ── rename definition ──▶ delete-subform-source
       │
       └────────── update caller ─────▶ delete-subform-source
~~~

One request proved two exact matches, compiled both edits against one source
snapshot, committed atomically, reparsed the result, read the bytes back, and
returned one terminal receipt. There was no plan file, shell escaping, manual
diff, or source reread.

Adversarial dogfood then found a real defect before the customer edit. Deleting
a form with a trailing same-line comment stranded the comment on the parent
line. A pure behavior matrix now covers first, middle, last, and inline
children; leading, trailing, detached, and next-form comments; missing and
ambiguous targets; complete-owner refusal; and the minimized real card-control
shape. The focused MCP gate is now **84 tests, 758 assertions, zero failures or
errors**.

The shared-workspace field probe found the next join defect. clj-surgeon could
select the exact (>defn post-card ...) owner, but cclsp returned zero workspace
symbols. The nested LSP root had not loaded the parent repository's Guardrails
and defcache clj-kondo hooks. clj-surgeon up now discovers the nearest
repository-bounded .clj-kondo/config.edn and supplies its directory to that
workspace's clojure-lsp process.

The same request then changed from zero symbols to 26 semantic sites. It
refused only because the full caller surface exceeded the closed visibility
budget: 26 sites and 127,920 source characters versus limits of 24 and 32,768.
That refusal exposed the next API boundary. A local implementation edit must
materialize one definition owner and report the caller graph as compact
evidence. It must not require 25 mechanical keep decisions.

The product has therefore crossed a qualitative boundary:

> This is far beyond a reader/writer. It is becoming the structural exocortex
> I would want for Clojure: exact perception, compiled intent, and mechanically
> boring execution. We are close to the first version that feels
> qualitatively different from ordinary editing.

The evidence behind that statement is operational, not aspirational. The tool
used itself to make atomic changes, caught source corruption with a pure
regression, repaired its shared semantic vocabulary, and converted an
unresolvable project macro into a proven 26-site graph. The remaining product
work is to keep broad discovery server-side, share exact workspace/Var/source
coordinates with cclsp, and materialize only the source required for the
current decision.

## Large refactors must pay formatting debt first

The first compact customer edit reached every semantic and structural gate,
then failed the repository's formatter check. The target was a legacy
2,400-line namespace. It was not Standard Clojure Style clean before the edit.

We first considered teaching the transaction verifier to distinguish inherited
formatting debt from new formatting damage. That would add a second formatting
policy inside clj-surgeon. It would also let a repository retain a permanently
red verification baseline.

The simpler contract is stronger:

```text
format one file
  -> run the complete repository tests
  -> commit the formatting-only change
  -> prepare a fresh structural basis
  -> apply the semantic transaction
  -> format and verify again
```

The unit is the file that the refactor will change. It is not the entire
repository. Formatting must occur before basis preparation because formatting
changes source hashes and structural addresses.

This migration has a visible cost. Formatting the untouched customer version
of `server2/src/server2/views.clj` changed 1,822 diff lines: 911 insertions and
911 deletions. Hiding those lines inside the media refactor would make the
semantic change difficult to review. A separate formatting commit makes the
cost explicit and pays it once.

The live worktree confirmed the immediate mechanical result. Formatting only
`views.clj` made the formatter check pass. clj-kondo reported zero errors and
20 warnings under the project's error-only policy. The complete test run then
stopped at one assertion in already-changing card-link behavior: 46 tests and
286 assertions ran before the failure. That result does not yet certify the
formatting migration. It establishes the exact test baseline that the active
customer branch must repair or reconcile before the formatting-only commit can
be declared green.

This is a caveat for the new workflow. One decision can become one verified
transaction only when the target file starts from a green verification
baseline. clj-surgeon must not silently format a legacy file as part of an
unrelated semantic edit, and it must not weaken the formatter gate. During a
large refactor, format each legacy target file first, validate it with the
repository's real tests, and keep that normalization separate in history.

The lesson is not that formatting is ceremony. Canonical source is part of the
transaction substrate. Once the debt is paid, later structural decisions can
remain small, hash-stable, and mechanically boring.

## The shared stack survived a real nested-workspace refactor

The next field run used one already-running clj-surgeon process against a
nested Clojure web application. A fresh live `tools/list` proved that both
tools published `workspace_root`. The current agent turn still displayed an
older cached schema, but the call itself accepted the field. One cross-workspace
read returned two exact owner forms in 84 ms. One later transaction changed ten
structural sites in one file, committed atomically, and verified the written
bytes in 0.2 seconds.

This corrected the earlier 26-site interpretation. The complete semantic
surface can contain 26 compact entries. With `scope=definition`, only the
definition owners are decision sites and carry source. The 24-site and 32 KiB
limits apply to the decision set, not to the compact proof surface. Broad
discovery no longer forces 25 mechanical keep decisions.

Two sources of friction deserved immediate repair:

1. The target application's development nREPL could reload production code but
   could not load tests. Its development command now adds a path-only
   `hot-test` alias. It does not enable the heavier test runtime policy. The
   edit, reload, and focused-test loop now runs in one JVM.
2. The stdio smoke test still parsed the human refusal summary as JSON. It now
   verifies stable refusal data in `structuredContent` and readable text in
   `content` as separate contracts.

The refactor preserved 21 preview-policy assertions, added 71 boundary,
profile, and production-shaped renderer assertions, and reduced lint warnings
inside the migrated owners. The live page returned HTTP 200 with shared media
markup. The complete application suite reached one failure owned by a
concurrent diagnostics change: the descriptor gained a candidate inventory,
but that change's exact-map assertion still expected the older shape. The
refactor did not weaken or edit that concurrent contract.

`clj-surgeon up` then onboarded the nested workspace idempotently. It found the
parent repository's clj-kondo configuration, reused both loopback services,
changed no agent configuration, and returned `restart-required=false`.

The product lesson is precise: friction is evidence. Cached schema text,
missing test classpaths, stale smoke assumptions, and misleading test
ownership were each cheaper to remove than to remember. Once removed, the
caller kept the semantic decision in working context and delegated workspace
routing, repeated replacements, byte verification, and receipts to the tool.

## Alternating workspaces became genuinely idempotent

The stronger alternating-workspace regression repaired the config-order bug.
Selecting an existing cclsp workspace now preserves its vector position and
serialized configuration. Three real workspaces then ran `clj-surgeon up`
through the same control plane. Each call returned
`cclsp-config-changed=false`, `codex-config-changed=false`, and
`restart-required=false`.

The live process proof was stronger than the helper test:

| Service | PID before | PID after |
|---|---:|---:|
| clj-surgeon MCP | 12712 | 12712 |
| shared cclsp | 68685 | 68685 |

One MCP client then read one exact form from the tool checkout in 13 ms and
one exact form from a nested application workspace in 19 ms. Each response
reported its own canonical root. A sibling module that previously required an
illegal `../` path was onboarded as a separate canonical root and returned a
12-form outline in 427 ms. No per-project server or path escape was required.

## The cclsp wedge became observable and recoverable

One long-lived workspace child stopped answering
`textDocument/documentSymbol`. The shared cclsp parent remained healthy. A
fresh isolated cclsp process with the same configuration resolved the same Var
in 487 ms. That separated a bad source or configuration from a wedged child.

The investigation found a false-ready defect. A timed-out language-server
initialization logged the failure but still marked the child initialized. Hot
reload also started every configured workspace child at once, which created an
avoidable initialization herd.

The provider now starts workspace children lazily. Initialization failure
never publishes a ready child. Each JSON-RPC request records workspace root,
LSP session, child PID, request ID, method, elapsed time, and outstanding
requests in `~/.local/state/clj-surgeon/cclsp/server.log`. A timed-out
`documentSymbol` request is cancelled. cclsp restarts only that canonical
workspace child and retries once under a new LSP session. The triggering result
reports `semantic_recovery`; `/healthz` reports the same recovery ledger to
other callers.

After the repair, a cold semantic request started only the selected child and
returned a proof basis. Other configured workspaces remained cold. Two later
semantic preparations through one shared Surgeon client returned separate,
root-bound bases for the tool checkout and the nested application in 6.0 and
3.5 seconds.

## Refusal archaeology disappeared

A real four-change transaction used incomplete source prefixes in its first
`find` field. The parser correctly refused before writing, but two adapters
collapsed the useful cause into `kernel-refusal`. The caller could not identify
the failed change without recovering the original request from telemetry.

The kernel now attaches the zero-based change index and stable change ID before
validation. The MCP adapter preserves the reason, phase, field, counts, owner,
file, and remedies. The visible result for the exact replay is now:

```text
apply_clojure_changes
  refused · invalid-intent-form
  change 0 · gallery-resolver · field :find

✓ source unchanged
→ Pass exactly one complete parseable Clojure form in :find for change 0 (gallery-resolver).
```

The live schema also closed an independent clean-caller report. It publishes
the exact fields in each direct change, the exclusive `forms`/`owner` choice,
one complete example, and the rule that `find` and `replace` each contain one
complete form. The focused gate ended at **101 tests, 878 assertions, zero
failures or errors**.

## The release gates closed

The final focused gate grew to **101 tests and 882 assertions** after the
bounded wrong-form candidate contract was added. The complete clj-surgeon run
passed **561 primary tests with 4,977 assertions** plus the focused MCP suite,
stdio smoke, benchmark self-tests, retention checks, and evidence-manifest
checks. The cclsp provider passed **273 tests and 860 expectations**, with five
intentional skips and no failures. Biome, TypeScript typecheck, the Bun build,
and both repositories' diff checks passed.

`make install` installed the stable CLI and identical Codex and Claude skills.
The final live audit alternated among the tool checkout, a nested application,
a sibling module, and a previously failing clean checkout. The shared Surgeon
and cclsp PIDs did not change. The result is one hot structural service, not a
collection of per-project daemons.

The bottom line is now operational: one command establishes the workspace,
one request selects its canonical root, one semantic proof binds the graph to
exact bytes, and one guarded transaction writes or refuses with enough data to
act. The caller no longer has to remember ports, child processes, stale
locations, partial edit progress, or refusal archaeology.

## Functional readiness replaced process readiness

A later server2 call exposed one remaining split-brain state. The HTTP process
and `/healthz` were green, but `inspect_clojure` returned
`server-not-initialized`. A live nREPL probe showed the exact contradiction:
the apply handler still had configuration, while the inspect handler and live
tool registry had lost theirs during namespace reload.

The defect was architectural. Process-lifetime configuration lived in private
atoms inside hot-reloaded handler namespaces. The repair moved tool config and
registry identity into one reload-stable runtime nucleus. Inspect, apply, tool
synchronization, and health now read the same state. `/healthz` returns 503
unless both runtime and registry are ready.

One transition restart loaded that nucleus. Afterward, `make mcp-reload`
preserved PID 98033, synchronized both tools, and left health functionally
green. A real server2 outline request then returned 14 forms in 133 ms without
restarting either service. The focused regression gate is now **106 tests and
937 assertions**, including a handler namespace reload that must preserve a
working inspect call.

The rule is sharper now:

```text
process alive != tool ready
functional health + one real bounded request = ready
```

## The skill budget stayed fixed while the tool grew

Adding exact-source preparation and guarded insertion initially expanded each
default agent skill to 77 lines. The complete suite refused because the repo
permanently caps that first-read surface at 70 lines and pins the recognition
phrases that cause correct routing.

The test was not weakened. Adjacent guidance was compressed, the exact MCP-first
route and owner-authority language were restored, and all three entrances ended
at 67 lines. The focused install contract passed 10 tests and 376 assertions.
This is the standard for one-shot growth: a new feature must earn space inside
the existing cognitive budget, not merely append another manual page.
