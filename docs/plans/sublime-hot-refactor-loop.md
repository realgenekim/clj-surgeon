# Sublime Hot Refactor Loop

**Status:** Complete
**Motivating issue:** `clj-surgeon-53o`

## Outcome

One `apply_clojure_changes` request turns one complete structural decision into
one joined refactor receipt:

```text
exact structural address
  -> compile and format candidate bytes
  -> failure-atomic commit and read-back
  -> reload the configured application nREPL
  -> run focused laws in that same JVM
  -> launch a bounded cold/e2e proof
```

The hot result returns as soon as the actual application JVM has accepted the
changed namespaces and the focused laws pass. A linked cold-verification job
continues outside the interactive critical path. The receipt never labels a
pending cold job complete.

## Bitter-Lesson Boundary

The caller decides architecture, exact owners, replacements, reload namespaces,
focused laws, and the repository-owned verification profile. clj-surgeon owns
addresses, snapshot hashes, formatting, write order, rollback, nREPL protocol,
bounded process supervision, and receipts.

The kernel does not infer which multimethod implementation should change,
invent test scope, accept arbitrary commands from an MCP request, or claim that
a focused law replaces the repository's full gate. Repository configuration is
the authority for formatter, application nREPL, and cold verification commands.

## Public Contract

### Dispatch-aware owners

Existing string entries in `changes[].forms` remain valid. A form entry may
also be a typed multimethod owner:

```json
{
  "kind": "defmethod",
  "name": "render",
  "dispatch": ":card"
}
```

The selector resolves exactly one top-level `defmethod` whose method name and
complete dispatch form are structurally equal. It refuses zero matches,
duplicate dispatch definitions, invalid dispatch syntax, or a same-named
ordinary definition. Lines and textual proximity are never part of the durable
address.

### Joined verification

The mutation request names one closed repository-owned profile:

```json
{"verify": "full"}
```

`.clj-surgeon.edn` supplies the executable policy. MCP input cannot contain a
shell command or arbitrary nREPL form. The configured formatter runs against a
staged candidate, before the first live-file write. The hot profile identifies
the actual application nREPL, namespaces to reload, and focused test Vars or
pure laws. The cold profile identifies a bounded subprocess gate.

A project composes that public name from closed data in `.clj-surgeon.edn`:

```clojure
{:formatter ["npx" "@chrisoakman/standard-clojure-style" "fix" "{files}"]
 :verification-profiles
 {"fast" {:commands [["clj-kondo" "--lint" "{files}"]]
          :hot {:port-file ".nrepl-port"
                :reload ["app.model" "app.routes"]
                :tests ["app.model-test/invariant"]
                :timeout-ms 5000}}
  "full" {:cold {:command ["make" "test"]
                  :timeout-ms 1200000}}}}
```

The formatter is mandatory for every managed transaction. Commands and the hot
proof run synchronously and retain rollback-on-failure. A cold profile launches
only after those gates pass. Its initial success is terminal for the interactive
edit but not for the cold gate:

```clojure
{:ok true
 :committed true
 :format {:status :complete :files 2}
 :hot-verification {:status :complete :jvm "app" :laws 7}
 :verification {:cold-verification
                {:status :running :verification_job "verify/..."}}
 :verification_complete false
 :next_call {:tool "inspect_clojure"
             :verification_job "verify/..."
             :view "verification"}}
```

When the cold job finishes, its durable receipt becomes `:complete` or
`:failed`. Cold failure does not silently roll back bytes already accepted by
the live application. It reports the inverse receipt as the executable remedy.
A profile that contains only `:commands` and `:hot` remains synchronous. A
profile that contains `:cold` is explicitly asynchronous. The built-in `fast`
profile is synchronous; built-in `full` launches the bounded `make test` job.

## Safety Invariants

- A refusal before commit changes no live source bytes.
- Formatting happens on staged candidate bytes, never as an untracked
  post-commit cleanup.
- The formatted candidate parses and becomes the hashed future state recorded
  by the receipt.
- Formatter failure or output outside the declared changed-file set refuses
  before commit.
- A dispatch selector resolves one method name plus one complete dispatch form.
- The hot verifier connects only to the configured application nREPL and proves
  its identity before reload.
- Reload order is explicit data; a newly introduced callee can load before its
  caller without consulting a stale language index.
- Hot-law failure triggers the existing guarded rollback and reports whether
  the rollback was accepted by the same application JVM.
- A cold job reserves bounded capacity atomically and has a deadline, PID, log
  bound, process-tree termination proof, and durable result. It cannot remain
  an invisible ghost process.
- Running and terminal cold status carry the original inverse receipt and
  receipt hash. A failed proof never sends the caller hunting for recovery data.
- `verification_complete=true` means every requested synchronous and cold gate
  is complete. A running cold job always reports false.
- Existing tests and verification gates are never weakened.

## Representative Acceptance Cases

| Case | Starting fact | Expected result |
|---|---|---|
| Dispatch-specific edit | 61 `defmethod render` forms; only `:card` is intended | One typed owner resolves and only its bytes change |
| Duplicate dispatch | Two `render :card` methods in reader branches | Refuse with both compact candidates; source unchanged |
| Formatter debt | Candidate is correct but not formatter-stable | Stage, format, hash, then commit once; no rollback archaeology |
| Formatter blast radius | Formatter changes an undeclared file | Refuse before commit and name the unexpected path |
| Live application proof | Browser JVM and test JVM differ | Reload and laws run in the configured browser JVM only |
| Load ordering | Transaction adds a callee and changes its caller | Reload callee before caller without semantic-index waiting |
| Focused-law failure | Reload succeeds but one law fails | Roll back source and reload original namespaces; concise failure |
| Cold success | Hot proof passes; full suite takes 30 seconds | Return hot receipt immediately; linked job later becomes complete |
| Cold failure | Hot proof passes; full suite fails | Preserve exact failure, logs, and undo command; never claim complete |
| Restart/drift | Application nREPL or source changes mid-transaction | Refuse or roll back with stable identity/drift data |

The dispatch fixture is synthetic but production-shaped. The formatter and
verification fixtures use real repository commands and a small real nREPL
application because mocks cannot prove process identity, reload, or formatter
filesystem behavior.

## Implementation Shape

1. Extend the pure owner model so `forms` accepts strings or typed owner maps.
2. Compile a `defmethod` address from method name plus parsed dispatch form.
3. Add a pure verification-program compiler. It returns ordered format, commit,
   hot-reload, focused-law, and cold-launch steps as data.
4. Add a staged formatter shell that materializes only candidate files under a
   confined temporary mirror and returns formatted strings to the compiler.
5. Add a small nREPL shell with explicit JVM identity, deadline, reload order,
   focused-law results, and rollback reload.
6. Add a supervised cold-job store with bounded output and a read-only status
   view through `inspect_clojure`; do not add another MCP tool.
7. Compose the existing transaction, receipt, diagnostic-delta, and inverse
   kernels. Do not duplicate them in the orchestration shell.

Build in dogfoodable slices: typed `defmethod` owner; staged formatting; live
nREPL proof; cold job; joined receipt. Each slice must improve the next slice's
own implementation loop.

## Test Plan

Pure tests exhaust method name/dispatch combinations, qualified names, metadata,
comments, reader branches, duplicate/missing owners, malformed dispatch forms,
permutation, formatting program compilation, reload order, receipt states, and
cold-job state transitions.

Boundary tests retain one real case for each irreducible fact: formatter success
and failure on staged files, no live-byte mutation before formatting succeeds,
actual nREPL identity/reload/law execution, process deadline/cancellation, cold
log bounds, atomic commit, read-back, rollback, and inverse receipt. Every new
field failure gets a named regression. No existing assertion is removed.

## Documentation and Release Checklist

- Update README, MCP tool description/schema, help, and all three skill copies.
- Document `.clj-surgeon.edn` formatter, application-nREPL, and cold-profile
  configuration with the smallest valid example.
- Record before/after tool actions, model recovery rounds, hot wall time, cold
  wall time, and test counts in a Captain's Log.
- Run `make mcp-reload` after contract changes and `make install` only after all
  focused and full gates pass.

## Verification Gates

1. New pure tests fail before each implementation slice.
2. Changed Clojure files are formatter-stable.
3. Changed namespaces have zero clj-kondo errors and warnings.
4. Focused pure and boundary tests pass in the persistent development nREPL.
5. `make mcp-test` and `make test` pass without weakened assertions.
6. One real dispatch-specific edit changes only the requested method.
7. One real staged-format transaction produces an executable inverse receipt.
8. One actual application JVM reload and focused-law receipt succeeds.
9. One cold success and one cold failure reach bounded terminal states.
10. A clean agent states a multi-owner refactor once and receives one joined
    receipt without manual formatting, reload, or test-command discovery.

## Definition of Done

The feature is complete when one typed MCP transaction can address a specific
multimethod implementation, format its complete candidate before commit,
atomically write and read it back, reload the configured live application JVM,
run focused laws there, launch a bounded cold gate, and return one truthful,
reversible receipt. Any ambiguity, drift, formatter failure, hot-proof failure,
or cold-process failure is explicit and requires no recovery archaeology.
