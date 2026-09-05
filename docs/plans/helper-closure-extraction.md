# Selected-helper closure extraction (`helper_extraction`) — consolidated contract (revision 3)

Status: CONSOLIDATED contract of record (revision 3, Astra's 05:07Z corrections applied in
place; earlier drafts live only in git history). Owning intent docs:
`docs/intent/helper-extraction/helper-extraction-design.md` (HLD→LLD) and
`helper-extraction-specs.md` (EARS registry, prefix `MCP-OP-HELPER`). Nothing is implemented
or measured; the registry lands together with its RED witnesses.

Origin: `docs/observations/2026-09-05-astra-next-api-advice.md` and the application
preflight in `/var/tmp/forge/astra-program/application-extraction-preflight/`.

## The cost this verb deletes

The real application (move six public response helpers out of
`cfp-scheduler-killer.web.http` into a new `web.response` namespace) is already
possible through `apply_clojure_changes` with an `extraction` request. The write is
cheap: one public call, 9.3 s, 30 files, 292 edits, committed; its behavioral acceptance
(24 helper cases, five real negatives) was an INDEPENDENT check that followed the write, not an
in-transaction verification receipt. The
PREPARATION is not: a 37,300-byte request encoding 85 caller changes across 28 caller
files, derived from 22 retained MCP reads (9.0 s of call wall inside an hour-long
envelope) plus a caller-side generator that reproduces hashes, counts, owners and a
closure argument the server then re-checks. Twenty callers mix moved and retained
helpers, so whole-library substitution is wrong; eight are response-only.

Everything in that payload is derivable from the source tree plus four decisions:
which helpers, where to, what the callers should call the new namespace, and how to
verify. The verb takes those four decisions and derives the rest. Its receipt is
O(1) in the number of callers, like `alias_migration`.

## The observable contract

```json
{"op": "helper_extraction",
 "workspace_root": "/abs/path",
 "from": {"file": "src/cfp_scheduler_killer/web/http.clj"},
 "helpers": ["html-response", "see-other", "text-response",
             "plain-not-found", "json-response", "with-etag"],
 "to": {"lib": "cfp-scheduler-killer.web.response",
        "alias_policy": ["response", "resp", "web-response"]},
 "scope": {"paths": ["src/**", "test/**"]},
 "verification": {"profile": "helper-proof"}}
```

Normal use omits `expect`; `expect.caller_files`, when supplied, is a strict guard. The request
is constant in the number of callers: no per-file, per-owner or per-site table, no hashes.

### Success receipt (O(1))

```json
{"ok": true, "operation": "helper_extraction", "committed": true, "kernel_status": "committed",
 "helpers": 6, "source_retired": 6, "destination_created": true,
 "caller_files": 28,
 "partition": {"moved_only": 8, "mixed": 20, "qualified_only": 0, "untouched": 3},
 "sites": 258, "retained_sites": 172, "alias_histogram": {"response": 28},
 "verification": {"profile": "helper-proof", "status": "checks-completed",
                  "structural_callers": 28, "helper_behaviors": 24, "compiled_callers": 0,
                  "ok": true},
 "closure": {"roots": ["src", "test"], "authorized_paths": ["src/**", "test/**"],
             "grammar": "supported-libspecs-only", "dynamic_references": "not-claimed"},
 "details_path": ".clj-surgeon/helper-extraction/<id>.edn",
 "undo_receipt": "...", "receipt_hash": "...", "elapsed_ms": 0}
```

Coverage is TYPED from the executed profile; there is no bare coverage count, and a check the
profile did not run is reported as 0, never implied.

### Refusals (v1: every one carries `next_call: null`)

| `error_type` | raised when | evidence carried |
|---|---|---|
| `helper-extraction-ambiguous-owner` | a helper name resolves to two owners, or to `declare` + definition | every owner found: line, kind |
| `helper-extraction-private-dependency` | a selected owner references a retained private var of the source | the var and the site |
| `helper-extraction-retained-dependency` | a selected owner references a retained PUBLIC var of the source, in any position | the var and the site (v1 refuses conservatively; no destination→source edge is ever created) |
| `helper-extraction-namespace-sensitive-body` | a moved body contains `::kw`, `::alias/kw`, syntax-quote or `*ns*` | the form and line (explicit v1 refusal) |
| `helper-extraction-unsupported-binding` | a caller reaches a selected owner through grammar the tool cannot close, or a qualified-only caller whose load path cannot be established | file, form |
| `helper-extraction-ambiguous-reference` | a bare symbol could resolve to two required namespaces | file, symbol, candidates |
| `helper-extraction-caller-outside-scope` | a supported reference exists under an admitted root outside `scope.paths` | file, site |
| `helper-extraction-alias-policy-exhausted` | every `alias_policy` entry collides in one file | file, collisions |
| `helper-extraction-expect-mismatch` | `expect.caller_files` supplied and ≠ derived | `expected_caller_files`, `derived_caller_files` |
| `helper-extraction-target-exists` | `to.lib` already defined or its path occupied | path |
| `helper-extraction-unknown-field` | the request carries a field outside the closed set | `unknown_fields` |
| `helper-extraction-verification-preflight-unavailable` | the profile is not synchronous, rollback-capable and runnable now | what it needed; nothing staged |

A `next_call` appears only when a schema-valid, scope-preserving, non-identical continuation is
mechanically known; v1 has none. No refusal offers scope narrowing, caller exclusion, an invented
alias or destination, or a weaker profile.

### Terminal states after staging

`committed` · `verification-failed` (restored: every protected byte and mode, destination removed)
· `verification-timeout` (restored) · `rollback-failed` (`source_unchanged: false`, files named,
kernel recovery-required evidence; never claims unchanged). Only synchronous, rollback-capable
profiles are admitted; capability is validated before any write; no committed success without
completed proof; the proof runs in a fresh process.

## What it reuses, what is new

Reused unchanged: `clj-surgeon.extract` / `mcp-extraction` (typed source retirement and
destination creation, `require_policy`), `clj-surgeon.alias-migration` (ns parsing,
file bindings, `choose-alias`, whole-form splice), the transaction kernel entrance.
New: the helper-owner resolver, the intra-source closure, the caller partition, the
`helper_extraction` planner that composes an `extraction` change with caller changes,
and the receipt. Estimated: one pure namespace (Babashka-safe), one I/O boundary
namespace, registration in `mcp-tool`.

## Measurement plan and falsifier (pre-registered, not run in phase 1)

1. Hand-drive first: the cfp candidate (frozen at `00e8f0fa`), the same six helpers,
   through the new verb, against the amended behavior oracle and the five real
   negatives already retained by Astra's review. A refusal is a failed hand-drive.
2. Then fresh caller pairs per model (Sol, Astra) on held-out helper sets, each with
   its contemporaneous native control given the SAME task facts and proof obligations;
   native may use a parser or a six-name batch script and may fuse write and tests.
3. Count preparation, schema discovery, refusals and cold startup separately and
   inclusively; report request-to-accepted-proof wall, never call wall.
4. Falsifier: if a capable native batch script plus tests remains faster after equal
   setup, the verb is kept as a quality option and is not sold as a speed route.

## Revision 3 (Astra's six corrections + static review of 9d4b54bb, 04:58–05:02Z) — the contract of record

Where this section disagrees with text above, THIS section wins. The owning intent documents are
`docs/intent/helper-extraction/helper-extraction-design.md` (HLD → LLD) and
`helper-extraction-specs.md` (EARS registry, prefix `MCP-OP-HELPER`).

1. **Continuation authority.** A refusal carries `next_call` ONLY when a schema-valid,
   scope-preserving, non-identical continuation is mechanically known (today: none in v1). Every
   other refusal carries bounded evidence plus the one unresolved decision and `next_call: null`.
   Positional-owner references are REMOVED from v1: `ambiguous-owner` refuses naming the owners
   found (line, kind). `expect_revised` and "both numbers" are REMOVED (a stateless request carries
   no old count): `expect-mismatch` reports `derived_caller_files` and `expected_caller_files`, nothing more.
2. **`expect.caller_files` is OPTIONAL** and absent in normal problem-to-done use; when supplied it
   is a strict guard.
3. **References, not only calls.** Discovery covers every supported REFERENCE to a selected owner:
   head-position calls, first-class uses (`(map http/f xs)`, `#'http/f`, `http/f` as a value),
   `:refer`ed bare symbols, fully qualified symbols with or without a require, and admitted `def`
   initializers. Selected-owner BODIES are scanned for their own dependencies: a moved→moved
   reference is rewritten to the destination's own symbol; a moved→retained-PUBLIC dependency is
   REFUSED in v1 (`helper-extraction-retained-dependency`, conservative: no back-edge from the
   destination into the source, so no cycle is possible through it, and no third-namespace cycle
   can be created either, because the destination requires nothing of the source); a
   moved→retained-PRIVATE dependency refuses `private-dependency`. Source-local uses (a retained
   source function referencing a moved helper) are lowered by the extraction machinery's own
   source rewrite (one require of the destination added to the source, symbols qualified), never
   by a second overlapping caller change; the source is both mutation subject and caller and is
   counted ONCE, against one immutable snapshot. Namespace-sensitive forms inside a moved body
   (`::kw`, `::alias/kw`, syntax-quote, `*ns*`) are REFUSED in v1 (`helper-extraction-namespace-
   sensitive-body`), explicitly; faithful rewriting is a future extension with its own witness,
   not a runtime admission rule. Exact owner bytes never stand for semantic preservation.
4. **One boundary for scope.** `scope.paths` is a WRITE-AUTHORIZATION subset of the project's
   admitted discovery roots (v1: the roots are explicit and config-bound: `src`, `test`, plus
   `.clj-surgeon.edn :source-roots` when present; no universal project discovery). Discovery runs
   over ALL admitted roots; a supported selected reference found OUTSIDE `scope.paths` refuses
   `helper-extraction-caller-outside-scope` (retiring shared definitions with a caller left behind
   is the failure the verb exists to prevent). `from.file` is always in the footprint regardless of
   roots. An UNSUPPORTED potential selected binding (prefix list, `:refer :all`, reader
   conditional, `:rename`, `ns-resolve`/`resolve` of the source) refuses `unsupported-binding`;
   an unrelated bystander that merely requires the source without referencing a selected owner
   gains no mutation authority and is `untouched`. No-require fully qualified callers are their own
   partition class `qualified-only`: the symbol is rewritten to the destination AND, for admitted
   static callers, one require of the destination is added so the rewritten symbol has a sound
   load path; where load semantics cannot be established the caller refuses `unsupported-binding`.
   Witness: a valid original that bootstrap-loads, then the caller loads after the plan.
5. **Terminal states are distinct**, and the promise matches the kernel: `verification-preflight-
   unavailable` (profile cannot run: refused before any write; nothing staged),
   `verification-failed` and `verification-timeout` (candidate bytes were staged, the kernel's hot
   rollback restored every protected byte and mode and removed the destination; receipt says
   `restored: true` with the restoration read-back), `rollback-failed` (restoration did not
   complete: receipt says `source_unchanged: false`, names the files, and carries the kernel's
   recovery-required evidence; it NEVER claims unchanged). Only synchronous, rollback-capable
   profiles are admitted in v1; capability is validated before writing. No committed success
   without completed proof. Proof = the acceptance-owned profile (Astra's, external guarded argv,
   candidate cwd: structural oracle, then helper behaviors) run in a FRESH process, so a warm
   namespace with stale Vars cannot manufacture a proof; the receipt reports the executed profile and its TYPED checks (structural
   callers, helper behaviors, compiled callers) and never an ambiguous coverage count.
6. **LID phases, not custom phases.** The plan's "Phases" list is replaced by the repository's
   scoped LID phases: intent (HLD/LLD in the design doc) → EARS registry with `[ ]` markers →
   RED witnesses carrying the ids → GREEN implementation → landing gate. Registration happens in
   the same change as the RED witnesses so the audit never sees an unwitnessed active gap.

### RED witness inventory (Astra's minimum matrix, each one witness)
- typed terminal refusal has `next_call nil`; any generated continuation validates against the closed schema and preserves scope
- selected owner uses a retained public value in a non-head position → `retained-dependency`; retained private → `private-dependency`
- moved→moved peer reference rewritten to the destination's symbol
- `::ok` in a moved body → `namespace-sensitive-body` refusal (explicit v1 refusal)
- direct source↔target back-edge and source→third-caller→target chain: both impossible by construction under rule 3, witnessed by a fixture that would create each
- source-local retained caller coexists with the extraction in one snapshot; footprint counts the source once
- fully qualified no-require caller rewritten, partition `qualified-only`
- supported selected reference outside `scope.paths` → `caller-outside-scope`; unsupported selected binding → `unsupported-binding`; unrelated bystander stays `untouched`
- synchronous verification failure and timeout restore every protected byte and mode and remove the destination (`restored: true`); rollback failure reports `source_unchanged: false`
- the proof runs in a fresh process; a warm stale Var cannot produce a false proof after retirement; the receipt's typed coverage names the executed profile and its checks, and a bare coverage count or an unexecuted compile claim fails the witness
- qualified-only caller: valid original bootstrap-loads, the rewritten caller carries the destination require and loads after the plan
- valid-original cycle witness: source→third→target→source would arise → `retained-dependency` refusal; no universal cycle-absence claim anywhere
- `expect.caller_files` absent accepted; supplied and wrong → `expect-mismatch` with both derived and expected counts
- request carrying any per-caller table → closed-field refusal; receipt carries no file list
