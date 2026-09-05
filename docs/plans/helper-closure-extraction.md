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
cheap: one public call, 9.3 s, 30 files, 292 edits, committed and verified. The
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
3. every caller namespace under `scope.paths` that requires `from.file`'s namespace
   and uses at least one selected helper, under every spelling the file binds
   (alias, `:refer`, fully qualified); an unsupported libspec grammar (prefix list,
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
| `helper-extraction-unsupported-binding` | a caller reaches the helper through grammar the tool cannot close | the same request with that file in `scope.exclude`, and the file named |
| `helper-extraction-ambiguous-reference` | a bare symbol could resolve to two required namespaces | the same request with `scope.paths` narrowed |
| `helper-extraction-alias-policy-exhausted` | every alias collides in one file | the same request with one more `alias_policy` entry |
| `helper-extraction-expect-mismatch` | derived caller-file count differs from `expect.caller_files` | the same request with the derived count |
| `helper-extraction-target-exists` | `to.lib` already defined or its path occupied | the same request with a different `to.lib` |
| `helper-extraction-verification-pending` | the profile could not run (provider absent, budget) | the same request with `verification.profile` set to a runnable one; the write is NOT committed |

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
  server shall refuse `expect-mismatch` with the derived count in `next_call`.

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

## Phases (each artifact to Astra before the next starts)

1. This document.
2. Specs registered + RED witnesses (pure planner witnesses on fixtures; refusal
   witnesses one per `error_type`; a receipt-shape witness) — no runtime.
3. Pure planner green.
4. I/O boundary + kernel + registration green; `make test` (landing gate) green.
5. Hand-drive on the cfp candidate (Astra's fixture), with his oracle.
6. Measurement per the plan above.
