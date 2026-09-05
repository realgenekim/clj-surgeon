---
parent: mcp-operation-contract-design
prefix: MCP-OP-ALIAS
---

# Alias Migration (`alias_migration`) — low-level design

## Why this leaf exists

`symbol_migration` + `require_change` are a *closed authority* pair: their own
contract says "Owners, old symbols, and positive counts are authority, not
discovery." The caller must already know every file, every owner, and every
per-site match count. That payload is `O(N)` in the number of affected
namespaces and it *adds* a counting obligation instead of removing a read.

For a fan-out var migration the whole cost the tool must delete is the
**reads**: the model has to open every namespace to learn its alias, its
bindings, and its call sites. A verb that removes those reads must do the
discovery itself and must return a receipt whose length does not grow with `N`.

`alias_migration` is that verb. One call carries the intent; the server
discovers, decides, splices, and commits; the receipt is `O(1)`.

## The observable contract

```json
{"op": "alias_migration",
 "workspace_root": "/abs/path",
 "from": {"lib": "acid.fanout.store", "var": "find-event"},
 "to":   {"lib": "acid.fanout.store2", "var": "fetch-event",
          "alias_policy": ["store2", "st2", "es", "store-2"]},
 "scope": {"paths": ["src/**"]},
 "expect": {"files": 80}}
```

Every field is constant in `N` except `expect.files`, which is one integer.
There is no per-file, per-owner, or per-site table anywhere in the request.

### Success receipt (`O(1)` in `N`)

```json
{"ok": true, "operation": "alias_migration", "committed": true,
 "files": 12, "sites": 36,
 "alias_histogram": {"store2": 10, "st2": 2},
 "collisions_resolved": 2, "refer_sites": 3,
 "lib_renamed": null,
 "kondo_delta": {"status": "...", "introduced": 0, "removed": 0, "blocking_introduced": 0},
 "focused_test": {"status": "...", "ok": true},
 "details_path": ".clj-surgeon/alias-migration/<id>.edn",
 "undo_receipt": "...", "receipt_hash": "...",
 "elapsed_ms": 12.34}
```

The receipt **never** contains a per-file list. Per-file detail (the file set,
each file's chosen alias, its collided policy entries, its site count and its
require mode) is written to `details_path` inside the workspace's
`.clj-surgeon` directory and is read only when a human wants it.

### Typed refusals

Each refusal is fail-closed (`source_unchanged: true`, no bytes written) and
carries an **executable `next_call`** — a complete `alias_migration` request the
caller can send verbatim after making the one decision the tool cannot make.

| `error_type` | Raised when | `next_call` |
|---|---|---|
| `alias-migration-expect-mismatch` | discovery finds a different number of requiring namespaces than `expect.files` | same request with `expect.files` set to the found count |
| `alias-migration-indirect-reference` | the old lib or var is reachable only through a construct the tool cannot mechanically close: a prefix-list libspec, `:use`, a runtime `require`/`alias`, a quoted or syntax-quoted occurrence, or a site inside a non-selected reader-conditional branch of a `.cljc` file | same request with `scope.paths` narrowed to exclude the named file |
| `alias-migration-ambiguous-ownership` | a bare (referred) occurrence of the var could resolve to two required namespaces | same request with `scope.paths` narrowed to exclude the named file |
| `alias-migration-alias-policy-exhausted` | every entry of `alias_policy` collides with something already bound in one file | same request with one additional policy entry appended |
| `alias-migration-empty-scope` | no namespace under `scope.paths` requires `from.lib` | same request with a widened `scope.paths` |
| `alias-migration-mixed-var-spec` | exactly one of `from.var` / `to.var` is null | same request with both vars null |
| `alias-migration-target-lib-exists` | `to.lib` is already defined while `from.lib` still is, or its path is occupied | same request with a different `to.lib` |

## Decomposition

```
clj-surgeon.alias-migration          pure, Babashka-safe
  parse-ns-requires                  ns form -> libspecs
  file-bindings                      every name bound in a file
  choose-alias                       first non-colliding alias_policy entry
  plan                               sources map -> plan | typed refusal

clj-surgeon.mcp-alias-migration      I/O boundary, no transaction knowledge
  expand-scope                       glob -> confined relative source paths
  plan!                              read + confine + call the pure planner
  plan->changes                      plan -> one `changes` transaction spec
  receipt                            transaction result + plan -> O(1) receipt

clj-surgeon.mcp-tool                 registration + kernel routing only
  handle-alias-migration             plan! -> execute-request! -> receipt
```

`mcp-alias-migration` deliberately knows nothing about the transaction kernel,
and `alias-migration` knows nothing about the filesystem. The write goes
through **`execute-request!` with a `changes` array**, the same kernel entrance
`apply_clojure_changes` uses, so path confinement, snapshot freezing, the
stale-source drift gate, failure atomicity, read-back proof, and the durable
inverse receipt all apply unchanged. Nothing in `edit_dsl`, the SCI allowlist,
the evaluation fence, path confinement, or `commit-compiled!` is touched.

## Why whole-form replacement

Each generated change is one exact `find` / `replace` pair whose `find` is the
**complete original source of one top-level form** and whose `replace` is the
spliced source of that same form, with `expect {:matches 1}`.

* The tool, not the DSL, decides which occurrences are sites. A token-level
  `find` of `store/find-event` would also match occurrences inside `#_`
  discards, metadata, quoted data, and non-selected reader-conditional
  branches, all of which the contract requires to stay byte-identical.
* rewrite-clj splices only the site tokens and the one require entry, so every
  other byte of the form — comments, commas, indentation, metadata, discards —
  is preserved exactly.
* The exact original bytes in `find` are the drift gate: if a *planned form*
  changed between discovery and commit its fingerprint no longer matches, the
  count guard fails, and the whole transaction refuses before any write.

  **What that guard does and does not prove**, measured while building the
  atomicity witness: it is a *per-form* guard, not a per-file one, and it is the
  same guarantee every other write tool in this repository gives. A concurrent
  edit that leaves every planned form byte-identical — a new comment at the top
  of the file, an unrelated new function — commits successfully, and correctly
  so. What it cannot see is a concurrent edit that *adds a new call site* to an
  unplanned form: that site is simply not migrated, and the receipt's site count
  is the count as of the frozen read. Callers who need whole-file freshness must
  re-run the verb; the residue predicate over the tree is what proves closure,
  not the transaction guard. This was found by an atomicity test whose first
  drift injection (prepending one comment line to the eleventh file) committed
  cleanly; the witness now drifts the file's `ns` form, which is always a
  planned form.
* Unnamed top-level forms are addressable this way; `forms`-scoped changes are
  not.

The receipt cost of this choice is zero: the changes array is server-internal
and never crosses the model boundary.

## Analysis rules

**Requiring namespaces.** A file is in scope if its single `ns` form has a
direct `(:require …)` clause naming `from.lib` as a plain symbol or as a vector
libspec. Any other route to the lib is a typed refusal, not a silent miss.

**Spellings.** Within such a file the var may be written as
`<alias>/<var>` for every `:as` / `:as-alias` alias, as
`<from.lib>/<var>`, and — only when the file has `:refer [<var>]` or
`:refer :all` — as a bare `<var>`.

**Sites.** A site is a `:token` node equal to one of those spellings that is
not inside an `:uneval` (`#_`) node and not inside a reader-conditional branch
other than the file's own platform branch or `:default`. A bare spelling is a
site only where it is not lexically shadowed; shadowing is tracked through
`let`-family binding vectors and `fn`/`defn` parameter vectors. Strings,
docstrings and comments are different node types and can never match.

**Every position a qualified symbol can occupy.** A qualified symbol is not
always a bare token in call position, and missing one of these leaves the alias
retired while a reference to it survives — which is a *load* failure, not a
subtle one. The anchor found this the hard way:

| position | treatment | why |
|---|---|---|
| `alias/x`, `alias/*earmuffed*` | site | ordinary |
| `#'alias/x`, `(var alias/x)` | site | a `:var` node wraps the token; the reference is real |
| **`(binding [alias/*x* v] …)`**, `with-redefs`, `with-bindings` | **left-hand side is a site** | these rebind **Vars**: the LHS is a reference through the alias map, not a binding form |
| `(let [x v] …)` and family | left-hand side is *not* a site | these bind locals |
| `` `(alias/x) `` | site | the **reader resolves the alias** inside a syntax quote |
| `'alias/x` | typed refusal | an ALIAS-qualified literal that nothing resolves; whether it is a reference is a judgment |
| `'from.lib/x` (fully qualified, quoted) | site, in lib mode | names exactly one namespace; a runtime `(requiring-resolve 'old.lib/v)` breaks *lazily at call time*, with no compile error |
| `^{:validator alias/f} x` | site | metadata values are evaluated code |
| `::alias/k` | typed refusal | see below |
| `:alias/k` | never a site | a plain keyword is not alias-resolved |
| `#?(:clj …)` non-selected branch | not a site | already covered |
| `#_(alias/x)` | not a site | already covered |

**A file that never requires the lib can still name it.** `sched_import.clj`
deliberately avoids the compile-time dependency and reaches the store through
`@(var-get (requiring-resolve 'cfp-scheduler-killer.store/state))`. Discovery
scoped to *requiring* namespaces never sees it, the tree loads clean, and the
import path throws the first time anybody runs it. So in lib mode every file in
scope is scanned for fully-qualified spellings, and a file that has only those
is migrated with no require change (`require-mode :qualified-only`).

**String literals naming the old lib are neither rewritten nor refused.** The
anchor's architecture tests assert things *about* the codebase —
`#{"cfp-scheduler-killer.store" …}` — and other strings are data paths like
`"data/store/events.jsonl"`. Rewriting them would silently edit test
expectations; refusing for them would block a migration over something that is
not a code reference. The receipt carries `string_mentions`, a count, and
`string_mention_sites`, a bounded list of `file:line`, so the bucket is visibly
non-zero and an operator can go straight to it rather than search for it. The
needle follows what the migration retires — the lib in lib mode, the qualified
`lib/var` in var mode, because a var migration leaves the lib and its other
vars standing and a string naming the survivor is not stale work.

**The `binding` case is the one that broke the real anchor.** An earlier draft
classified `binding` with `let`, so `(binding [store/*clock* sim-birth] …)` at
`replay.clj:128` had its left-hand side skipped as if it were a local. The
migration rewrote every ordinary site and the require, then the load failed with
`Unable to resolve var: store/*clock* in this context` — the alias was gone and
the reference was not. `with-redefs` has the same shape and appears in dozens of
the anchor's tests.

**Auto-resolved keywords refuse, and this is a deliberate scope decision.**
`::store/k` reads as `:cfp-scheduler-killer.store/k`: the alias is resolved at
read time exactly as it is for a symbol, so leaving it breaks the read once the
alias is retired. But rewriting it *changes the keyword's value*, and a
keyword's namespace is part of its identity — it may be persisted in a database,
dispatched on by a multimethod, or compared against a literal written elsewhere
in a form this verb never sees. Neither outcome is bookkeeping, so the verb
refuses with reason `auto-resolved-keyword`, names the file and the exact form,
and hands back a `next_call` that excludes that file. The alternative — a silent
change to a data value — is precisely the failure class this repository treats as
worse than an error, because it terminates investigation. (The anchor repo
contains zero `::store/` occurrences, so this costs nothing there.)

**Alias choice — and why the collision set is exactly the `ns` form.** The
chosen alias is the first `alias_policy` entry that collides with nothing in
that file's `ns` form. The collision set is precisely:

> {aliases introduced by `:as` and `:as-alias`} ∪ {names introduced by `:refer`}

and nothing else. A local binding, a `fn`/`defn` parameter, a destructured
name, and a top-level `def`/`defn` name are **not** collisions.

The reason is a fact about Clojure, not a policy preference. In
`store2/fetch-event` the symbol is *qualified*, and the namespace part of a
qualified symbol is resolved through the namespace's **alias map** at read and
analysis time. Lexical scope plays no part in it, so
`(let [store2 1] (store2/fetch-event id))` is unambiguous and correct: `store2`
alone reads the local, `store2/fetch-event` reads through the alias. The same
argument covers top-level definitions — a var named `store2` and an alias named
`store2` coexist, because `store2` reads the var and `store2/x` reads the alias.
The file's own namespace name is likewise not a collision.

A first draft of this verb fed every local binding in the file into the
collision set. It was not merely over-cautious: it produced a *different alias
than the canonical* in every file that happened to contain a local named for a
policy entry, and the byte oracle caught exactly that — `ns_000.cljc`,
`ns_033.clj` and `ns_085.clj` were assigned a later policy entry when `store2`
was in fact free, and the receipt over-reported `collisions_resolved`. The
local-scope analysis is still carried by the walker, where it belongs and is
correct: a local named `find-event` *does* shadow a **referred** `find-event`,
because that spelling is unqualified.

**Require rewrite.** When every use of `from.lib` in the file is a site being
migrated, the old libspec is *replaced* by `[to.lib :as <alias>]`. When any
other use survives — another var of the old lib, or an occurrence the tool
deliberately left alone in a non-selected reader-conditional branch — the new
libspec is *added* alongside and the old require is left in place. In selected-Var mode,
unrelated names declared in the old explicit `:refer` vector also require keeping
the old libspec, even if their uses are not selected sites. The selected name is
removed from that old vector once its uses have migrated, unless a deliberately
retained reader branch still needs it. Other refer names, options, and trivia
remain at the old library. An empty resulting refer vector is valid and preserves
its surrounding trivia. A selected-Var migration retains `:refer :all` at the old
library because it cannot enumerate unrelated imports from the client alone.
This partition works when the selected Var is subsequently removed from the old
library. Both refer policy spellings retain their existing selected-Var behavior:
selected bare calls become qualified calls using the new alias; the policy table
below governs whole-library migrations.

## Verification and the receipt's two summary fields

**Verification is opt-in.** `kondo_delta` and `focused_test` come from the
workspace's own configured verification profile, routed through the existing
transaction verification path, so a lint regression or a failing focused test
rolls the whole transaction back — but only when the request names a profile in
`verify`. Otherwise both fields report `{"status": "not-requested"}`.

This matches the contract the other public write tools state ("omit `verify`
unless the user or repository explicitly requests a configured transaction
profile"), and the reason is not merely consistency. The built-in default `fast`
profile is `clj-kondo --lint` **plus** `npx @chrisoakman/standard-clojure-style
check`. An earlier draft auto-selected that profile whenever the workspace had
one configured — which every workspace does, since those are the defaults. The
result, found on the first real call over the HTTP wire: a completely correct
migration was rolled back because `npx` could not run, the refusal blamed
"verification failed", and every call paid two seconds of wall time in a cohort
whose entire subject is wall time. Naming a profile the workspace does not
configure refuses before any write rather than silently skipping verification.

## The server adapter must borrow the dispatch's derivations, not just its config

The verb's MCP handler routes `workspace_root` through the same
`mcp-workspace` router the other tools use, and it must then repeat two
derivations the direct dispatch performs, because a routed workspace context is
*not* a complete config:

* **The receipt directory.** A workspace context carries `:receipt-dir` only
  when the server was started with one; for any other workspace it is nil and
  the directory must be derived from the routed project root
  (`default-receipt-dir project-root`). Calling that helper with the wrong
  arity is the entire failure mode of the first live call.
* **The verification profiles.** A context published by the HTTP server carries
  `:verification-profile-selection-fn` / `:verification-profiles-fn` rather than
  `:verification-profiles`. An entrance that reads `:verification-profiles`
  directly gets the *server's* profiles, not the requested workspace's. That
  resolution now lives in one shared function,
  `mcp-tool/resolve-verification-config`, called by both entrances so they
  cannot drift apart again.

Both are witnessed by tests that start the real HTTP server on an ephemeral
port **without** a `:receipt-dir` and address a `workspace_root` that is not the
server's project directory — the exact shape of the live call. A unit test that
drives the handler directly with an explicit receipt directory cannot see either
defect.

## Lib-only migration (`var: null` on both sides)

A null `var` on both sides means *the whole library moves*. This is the shape the
real-repo anchor needs: `curtaincall-cfp` at `d9afe8e9` has no single var used by
all 68 requiring namespaces, so the only instantiation that reaches N=68 is
`cfp-scheduler-killer.store` → `cfp-scheduler-killer.event-store` with no var
rename (815 src sites over 68 files, plus 1075 test sites over 106 files).

Exactly one of the two `var` fields being null is a typed refusal
(`alias-migration-mixed-var-spec`): the two shapes have different closures and
guessing which one the caller meant is not the tool's decision.

**What moves.** Every qualified use of *every* var of the old lib, under every
spelling that file makes legal, becomes `<alias>/<same var name>`. The var names
themselves are unchanged; only the qualifier moves.

**Referred names — `to.refer_policy`, and why the default is what it is.**

| policy | the require becomes | bare referred uses |
|---|---|---|
| **`preserve-refer`** (default) | `[to.lib :refer [same names]]`, plus `:as <alias>` only if the file also has alias-qualified sites | untouched; re-pointed by the require alone |
| `alias-qualify` | `[to.lib :as <alias>]`, the `:refer` dropped | rewritten to `<alias>/<name>` |

`preserve-refer` is the default because it is the *smaller* closure and the one
that cannot be wrong. Re-pointing one libspec migrates every referred name at
once, with no per-name shadow analysis and no risk of rewriting a local that
happens to share a referred name. `alias-qualify` is offered because a caller may
want the qualifier visible at every site, and it is exact — the walk carries the
set of referred names still unshadowed at each point in the tree — but it does
strictly more work for the same result. `:refer :all` is refused either way: the
referred set is not mechanically knowable from the requiring file.

**The defining namespace moves too.** `to.lib` does not exist yet, so the
transaction renames the namespace that defines `from.lib`: `store.clj` becomes
`event_store.clj` with its `ns` symbol rewritten. This is **not** composed from
`clj-surgeon.rename` — that module does its own filesystem walk and its own file
moves outside any transaction, and its rewrite is prefix-based over the whole ns
tree (it would also rewrite `from.lib.impl`, which this verb must not). Instead
the new file is created **inside the kernel transaction** via `:create-files`, so
the new namespace and every rewritten caller land or refuse together, and the
kernel's undo receipt removes the created file.

The superseded file is then **retired, not deleted**: it is moved to
`.clj-surgeon/alias-migration/retired/<its path>` and the receipt names that
location. Deleting it is not something the kernel's inverse receipt can undo, so
the honest design keeps the bytes and says where they are. If retiring fails, or
if verification fails afterwards, the file is moved back before the transaction is
rolled back — the tree is never left with two definitions of the same code or
none.

**Prefix-sharing siblings.** The anchor repo supplies better sed-catchers than
anything synthetic: `store-pg`, `store-checkpoint`, and four `store*-test`
namespaces are all corrupted by a naive
`s/cfp-scheduler-killer.store/cfp-scheduler-killer.event-store/`. Every match in
this verb is by **whole symbol identity** — libspec equality, qualifier set
membership, and defining-namespace equality are all `=` on the complete symbol,
never a substring or prefix test. The fixture carries `acid.fanout.store-pg`,
`acid.fanout.store-checkpoint`, a `store-pg/write!` call site, and a
`acid.fanout.store-test` namespace under `test/`, and a witness asserts every one
of them is byte-identical after the migration.

## Non-goals

Renaming a namespace, moving a var's definition, editing callers in another
workspace, choosing between two candidate owners, and any change whose
per-caller decision is a judgment rather than a closure.

## CLI

MCP only. The CLI is not an entrance for this verb: the experiment measures the
model's call count, and `core.clj` dispatch would add a second layer with no
measured return (`CLAUDE.md`, "the CLI as an MCP substitute" is a closed loser).

### Selected refer repair: supported identity and explicit boundary

Refer ownership uses symbol identity, including a metadata-bearing symbol.
Unrelated metadata-bearing entries survive as their original nodes; retiring the
selected import removes its entire metadata-bearing entry. The selected Var's
`:rename` local binding is not modeled by this migration's scope walker. When
that entry is present, the operation refuses before planning writes with
`alias-migration-indirect-reference`, reason `unsupported-selected-renamed-refer`,
the file and an explicit missing-capability remedy, and no `next_call`. A caller
must review and perform a migration that accounts for the renamed binding; the
refusal never prescribes widening scope or dropping the affected file. Unrelated
`:rename` entries remain at the old library. Full renamed-binding migration is a
separate capability, and this refusal counts as a failed benchmark task.

Metadata wrappers on a refer vector or rename map do not change its import meaning. The planner unwraps metadata only to inspect the underlying node and preserves wrappers on retained imports. Reader-discarded entries are protected literal source, not live refer names or rename keys. Import identity and selected-rename detection operate on nodes and tokens without applying `n/sexpr` to arbitrary supplied forms.


## Binding scope repair boundary (2026-09-05 candidate)

Supported let-family initializers see incoming scope; each local shadows only after its initializer, in later initializers and the body. Reader discards occupy no runtime child position and introduce no names. Binding metadata survives while identity comes from the wrapped value. Vector/map destructuring follows declarations rather than all descendant tokens, including :keys/:syms/:strs/:as and excluding :or expressions. letfn shares function names, not all parameter names.

When the form contains a potential selected reference, the bounded walker refuses :or destructuring defaults and metadata-wrapped parameter/binding vectors for qualified and referred migrations alike. Reader discards do not establish potential-reference authority. An unrelated default-only bystander remains outside the plan. For live referred-name migration it also refuses if-let/if-some, as->, for/doseq, multiple function arities, a selected named fn (including metadata-wrapped names), or letfn parameter shadowing of selected names. The result is alias-migration-indirect-reference, reason unsupported-binding-scope, file/form evidence, source_unchanged=true, mutation_attempted=false, no planned files, no executable next_call, and a remedy that does not change migration scope. These are explicit unsupported capabilities, not successful migrations. General macro expansion and a complete binding compiler remain outside scope. Existing qualified and ordinary refer controls remain required. See docs/plans/alias-migration-binding-scope-repair.md for evidence and pending gates.
