# Selected-helper closure extraction (`helper_extraction`) — phase 1: intent and requirements

Status: PHASE 1 DRAFT for independent review (Astra directs this lane under Gene's
delegation of 2026-09-05). Nothing here is registered, implemented, or measured.
Registration of the draft requirements below happens in phase 2 together with their
RED witnesses, because the intent audit treats an unwitnessed active gap as a violation.

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

## The observable contract (draft)

```json
{"op": "helper_extraction",
 "workspace_root": "/abs/path",
 "from": {"file": "src/cfp_scheduler_killer/web/http.clj"},
 "helpers": ["html-response", "see-other", "text-response",
             "plain-not-found", "json-response", "with-etag"],
 "to": {"lib": "cfp-scheduler-killer.web.response",
        "alias_policy": ["response", "resp", "web-response"]},
 "scope": {"paths": ["src/**", "test/**"]},
 "verification": {"profile": "fan-proof"},
 "expect": {"caller_files": 28}}
```

Constant in N except `expect.caller_files`, one integer. No per-file, per-owner or
per-site table; no hashes the caller has to reproduce.

The server derives, in this order, and refuses at the first step it cannot close:

1. the selected owners in `from.file` (each helper must resolve to exactly one
   top-level `defn`/`defn-`/`def` there; a helper that is `declare`d and defined, or
   defined twice, is `helper-extraction-ambiguous-owner`);
2. the intra-source closure: helpers that call other selected helpers move together;
   a selected helper that calls a NON-selected private var of the source is
   `helper-extraction-private-dependency` (the decision is the caller's: select it
   too, or refuse);
3. every caller namespace under `scope.paths` that uses at least one selected helper
   under every spelling: alias, `:refer`, and FULLY QUALIFIED `ns/helper` whether or not
   the file requires the source namespace (a fully qualified use without a require is a
   site and gets the destination's fully qualified name);
3b. SOURCE-LOCAL uses: a retained function in `from.file` that calls a selected helper
   becomes a caller of the destination (the source namespace gains one require of
   `to.lib`); a selected helper that calls a retained PUBLIC var of the source makes the
   destination require the source, and if both directions occur the request is refused
   `helper-extraction-cyclic-dependency`; private dependencies follow step 2; an unsupported libspec grammar (prefix list,
   `:refer :all`, reader-conditional require, `:rename`) is
   `helper-extraction-unsupported-binding`;
4. the caller PARTITION: `moved-only` callers (every use of the old namespace is a
   selected helper: replace the require), `mixed` callers (retain the old require,
   add one new require), `untouched` (require the old namespace but use no selected
   helper: not in the footprint);
5. one alias per caller from `alias_policy`, first entry colliding with nothing bound
   in that file (`helper-extraction-alias-policy-exhausted` otherwise);
6. one guarded transaction through `execute-request!`: the existing typed
   `extraction` change (source retirement, destination creation with
   `require_policy` minimal) plus one exact whole-form `find`/`replace` per
   rewritten caller form and one per rewritten `ns` form, each with
   `expect {:matches 1}`;
7. the named verification profile, run inside the transaction, with its result in
   the receipt.

### Success receipt (O(1))

```json
{"ok": true, "operation": "helper_extraction", "committed": true,
 "helpers": 6, "source_retired": 6, "destination_created": true,
 "caller_files": 28, "partition": {"moved_only": 8, "mixed": 20, "untouched": 3},
 "sites": 258, "retained_sites": 172,
 "alias_histogram": {"response": 28},
 "verification": {"profile": "fan-proof", "status": "complete", "checks": 2, "ok": true},
 "closure": {"roots": ["src/**", "test/**"], "grammar": "supported-libspecs-only",
             "dynamic_references": "not-claimed"},
 "details_path": ".clj-surgeon/helper-extraction/<id>.edn",
 "undo_receipt": "...", "receipt_hash": "...", "elapsed_ms": 0}
```

The receipt never lists files. Per-caller detail goes to `details_path`.

### Plan responses (only where a real judgment remains)

A refusal is fail-closed (`source_unchanged`, `target_unchanged`, no bytes written),
names the ONE unresolved decision, and carries an executable `next_call` — the same
request with that decision made — so the caller never reconstructs hashes, counts,
selectors or facts the server already knows. Draft table:

| `error_type` | Raised when | `next_call` carries |
|---|---|---|
| `helper-extraction-ambiguous-owner` | a helper name resolves to two owners or to `declare`+`defn` | the same request with `helpers[i]` replaced by a positional owner reference |
| `helper-extraction-private-dependency` | a selected helper calls a non-selected private var | the same request with that var added to `helpers` |
| `helper-extraction-unsupported-binding` | a caller reaches the helper through grammar the tool cannot close | NONE. The refusal names the file and form; the caller must make that caller supportable (by hand) and resend the same request. Excluding the caller is not offered: retiring the definitions with an unrewritten caller left behind is the failure this verb exists to prevent |
| `helper-extraction-ambiguous-reference` | a bare symbol could resolve to two required namespaces | NONE. The refusal names the site; narrowing `scope.paths` is not offered, because a narrower scope silently leaves callers unrewritten |
| `helper-extraction-alias-policy-exhausted` | every alias collides in one file | the same request with one more `alias_policy` entry |
| `helper-extraction-expect-mismatch` | derived caller-file count differs from `expect.caller_files` | the same request with the derived count, presented as a PROPOSAL the caller confirms by resending; the receipt records `expect_revised: true` with both numbers. The server never proceeds on its own count |
| `helper-extraction-cyclic-dependency` | moving the selection would make source and destination require each other | NONE. The refusal names the two vars; the caller changes the selection |
| `helper-extraction-target-exists` | `to.lib` already defined or its path occupied | the same request with a different `to.lib` |
| `helper-extraction-verification-pending` | the named profile could not run (provider absent, budget) | NONE. Nothing is committed; the refusal names what the profile needed. A weaker profile is never suggested |

Every refusal is typed in the op itself (ratchet rung 5): the bad state is
unrepresentable, not detected afterwards.

## Outcome contract (what the receipt may and may not claim)

- Closure is exact over the named `scope.paths` roots and the SUPPORTED libspec
  grammar. It says so in the receipt (`closure.grammar`) and claims nothing about
  dynamic references, `resolve`, macros that generate calls, or strings.
- Protected bytes and file modes are preserved; the transaction kernel's snapshot,
  drift gate, atomicity, read-back proof and inverse receipt apply unchanged.
- Verification is either `complete` with its checks, or the request is refused as
  `verification-pending` with nothing committed. There is no "committed, verify later".
- The acceptance fixture RETIRES the original definitions in the source namespace,
  so a compile that still passes proves the callers were rewritten, not merely that
  the old definitions still exist. A receipt saying "atomic" and "parsed" is weaker
  than behavioral equivalence; the profile must run behavior.
- Unsupported binding defaults and parameter metadata refuse until a witness admits
  them. No universal claim that arbitrary macros were understood.

## Draft requirements (EARS; prefix `MCP-OP-HELPER`; registered in phase 2 with witnesses)

- HELPER-001 When the server starts in the full profile, it shall advertise
  `helper_extraction` as a public tool whose input schema is the closed field set above.
- HELPER-002 The request shall carry no per-file, per-owner or per-site table, so its
  size is constant in the number of callers.
- HELPER-003 When executing, the server shall itself resolve each helper to exactly one
  top-level owner in `from.file`, and shall refuse `ambiguous-owner` otherwise.
- HELPER-004 When a selected helper references a non-selected private var of the
  source, the server shall refuse `private-dependency` naming the var.
- HELPER-005 The server shall discover every caller under `scope.paths` that requires
  the source namespace, and every use of a selected helper under every spelling that
  file binds; discovery shall be byte-faithful to the filesystem it walks.
- HELPER-006 The server shall partition callers into moved-only, mixed and untouched,
  shall retain every non-selected use unchanged, and shall never replace a whole
  library require in a mixed caller.
- HELPER-007 When choosing a caller's alias, the server shall select the first
  `alias_policy` entry colliding with nothing bound in that file, and shall refuse
  `alias-policy-exhausted` otherwise.
- HELPER-008 The write shall be one transaction through `execute-request!`; on any
  refusal no byte of any file changes.
- HELPER-009 The receipt shall contain counts and histograms only, never a file list;
  per-caller detail shall be written to `details_path`.
- HELPER-010 Each refusal shall name exactly one unresolved decision and carry an
  executable `next_call`.
- HELPER-011 The named verification profile shall run inside the transaction; a
  profile that cannot run shall refuse `verification-pending` with nothing committed.
- HELPER-012 The receipt's `closure` field shall state the roots and the grammar over
  which closure is exact, and shall state that dynamic references are not claimed.
- HELPER-013 If the derived caller-file count differs from `expect.caller_files`, the
  server shall refuse `expect-mismatch` with the derived count in `next_call` as a
  proposal, and shall never proceed on its own count.
- HELPER-014 Fully qualified uses of a selected helper, with or without a require of the
  source namespace, shall be discovered and rewritten as sites.
- HELPER-015 Source-local uses of a selected helper by retained functions shall be
  rewritten to the destination with one added require; a dependency in both directions
  shall refuse `cyclic-dependency` with nothing written.
- HELPER-016 No refusal shall offer scope narrowing, caller exclusion, or a weaker
  verification profile as a continuation.

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

## Revision 2 (Astra's corrections, 04:40Z, all accepted)

1. No escape hatches: `unsupported-binding` and `ambiguous-reference` carry no scope-narrowing
   or exclusion continuation (HELPER-016).
2. No silent weakening: `expect-mismatch` proposes, never proceeds; `verification-pending`
   never suggests a weaker profile.
3. Fully qualified references without a require, source-local uses, and dependencies in
   both directions are accounted for or explicitly refused (HELPER-014/015, steps 3/3b).
4. The prior application's proof is labelled accurately: independent acceptance followed the
   write; it was not an in-transaction verification receipt.

## Phases (each artifact to Astra before the next starts)

1. This document.
2. Specs registered + RED witnesses (pure planner witnesses on fixtures; refusal
   witnesses one per `error_type`; a receipt-shape witness) — no runtime.
3. Pure planner green.
4. I/O boundary + kernel + registration green; `make test` (landing gate) green.
5. Hand-drive on the cfp candidate (Astra's fixture), with his oracle.
6. Measurement per the plan above.

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
   sensitive-body`) unless the existing extraction semantics already rewrite them faithfully and a
   behavior witness proves it; exact owner bytes never stand for semantic preservation.
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
   partition class `qualified-only` (rewritten to the destination's qualified symbol; no require added).
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
   namespace with stale Vars cannot manufacture a proof; the receipt claims only the coverage the
   profile actually ran (`verification.covered_callers`).
6. **LID phases, not custom phases.** The plan's "Phases" list is replaced by the repository's
   scoped LID phases: intent (HLD/LLD in the design doc) → EARS registry with `[ ]` markers →
   RED witnesses carrying the ids → GREEN implementation → landing gate. Registration happens in
   the same change as the RED witnesses so the audit never sees an unwitnessed active gap.

### RED witness inventory (Astra's minimum matrix, each one witness)
- typed terminal refusal has `next_call nil`; any generated continuation validates against the closed schema and preserves scope
- selected owner uses a retained public value in a non-head position → `retained-dependency`; retained private → `private-dependency`
- moved→moved peer reference rewritten to the destination's symbol
- `::ok` in a moved body → `namespace-sensitive-body` refusal (or proven-faithful rewrite with a behavior witness)
- direct source↔target back-edge and source→third-caller→target chain: both impossible by construction under rule 3, witnessed by a fixture that would create each
- source-local retained caller coexists with the extraction in one snapshot; footprint counts the source once
- fully qualified no-require caller rewritten, partition `qualified-only`
- supported selected reference outside `scope.paths` → `caller-outside-scope`; unsupported selected binding → `unsupported-binding`; unrelated bystander stays `untouched`
- synchronous verification failure and timeout restore every protected byte and mode and remove the destination (`restored: true`); rollback failure reports `source_unchanged: false`
- the proof runs in a fresh process; a warm stale Var cannot produce a false proof after retirement; `covered_callers` equals the declared caller set
- `expect.caller_files` absent accepted; supplied and wrong → `expect-mismatch` with both derived and expected counts
- request carrying any per-caller table → closed-field refusal; receipt carries no file list
