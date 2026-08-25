# Compiled editing: the route that can beat native patching

## Outcome

`edit_clojure` is now the preferred route when a Clojure change is already a
complete mechanical decision. The model supplies owner names and small exact
rewrites. Surgeon resolves them against one frozen snapshot, commits the batch
atomically, and returns terminal verification.

This route delivered a **4.61x median end-to-end speedup** over native read plus
`apply_patch` for a production-derived, 485-line extraction cleanup. It remained
exact but reached only **1.05x** for a smaller two-file, six-replacement task.
The difference defines the useful boundary: Surgeon wins decisively when a
compact structural decision avoids reading or reproducing a large amount of
source. Batching alone is not enough.

## The five breakthroughs

### 1. Make the public gesture smaller than the implementation

The original heavyweight transaction API exposed preparation, expectations,
verification modes, and rollback concepts at once. It was powerful but easy to
mis-shape. `edit_clojure` keeps that machinery behind one small entrance:

```text
workspace + complete edits/programs/deletions
  -> compile against one frozen snapshot
  -> compare-and-swap guarded commit
  -> parse and read-back verification
  -> terminal receipt
```

The model does not need to understand the executor to use it safely.

### 2. Address intent instead of rendering source context

Native patching makes the model read enough source to construct diff context,
then emit much of the changed source again. A compiled transaction can name the
same decision with:

- `within.form` for a named top-level owner;
- `within.namespace` for the `ns` form;
- exact `from` and `to` forms for small nested replacements; and
- `delete_owners` for several exact top-level deletions.

The decisive benchmark deleted 22 owners, rewrote one namespace require, and
changed seven routes with 30 compact intents. The compact arm returned about
175 bytes of tool output. Native source commands returned 19,379 to 77,516
bytes and required the model to manufacture a large patch.

```text
Compiled route                         Native route

owner names + small rewrites           read 485-line file
              |                                |
              v                                v
one guarded transaction                hold source in model context
              |                                |
              v                                v
175-byte terminal receipt              render and apply large patch
```

Both engines can batch writes. The asymptotic advantage is at the model
boundary: compact intent scales with the decision; textual patching also scales
with source context and reproduced source.

### 3. Turn every real annoyance into address algebra

The first production cleanup could address Vars but not the namespace form.
The right response was not a prompt workaround. It was `within.namespace`.
Repetitive owner deletion similarly became `delete_owners`.

This is the Kent Beck loop for agent tools:

1. notice a cumbersome or failure-prone gesture;
2. add the smallest named primitive that removes it;
3. preserve the refusal and regression as a test; and
4. use the cheaper gesture on the next real change.

The result should feel like an expert editor chord: decide once, play once,
then move to validation.

### 4. Make one shot safe enough to omit defensive round trips

A one-call route is useful only if source cannot change underneath it. Surgeon
resolves all owners and exact forms against one frozen snapshot. Exact match
counts and source hashes fence the write. A stale or malformed decision refuses
before mutation. Successful writes parse and read back before returning
`verification_complete=true`.

Therefore, an already-decided edit does not need a preflight source read, and a
successful terminal receipt does not need a ritual reread or diff. The agent
still runs the same proportional formatter, linter, and affected tests that a
native edit requires. Tool verification replaces redundant edit verification;
it does not replace product verification.

### 5. Route narrowly enough to keep native as an ally

Native `apply_patch` remains a fearsome competitor. Use it for:

- one small visible literal change;
- prose or a new file;
- a change whose patch is already obvious and compact; or
- an operation outside Surgeon's address language.

Use one `edit_clojure` call when the complete decision names several owners,
small exact rewrites, computed programs, or owner deletions. Use the heavyweight
`apply_clojure_changes` only when prepared semantic evidence, a unique
operation, or transaction-coupled gates justify its extra surface.

This boundary explains both Anvil strata:

| Production-derived decision | Compact median | Native median | Result |
|---|---:|---:|---:|
| 485-line cleanup, 30 intents | 32.546 s | 150.138 s | **4.61x** |
| two files, six small rewrites | 29.893 s | 31.378 s | **1.05x** |

All 12 remote trials were exact. Compact won every pair, but only the
source-volume-eliding cleanup produced the target 2--5x gain.

## Fleet-wide default

The canonical always-loaded rule is
[`resources/clj-surgeon-agent-routing.md`](../resources/clj-surgeon-agent-routing.md).
`make install` installs it into both global instruction files without changing
unmanaged bytes. `make check-agent-routing` verifies the current block without
writing. The installer preflights every target, refuses malformed or duplicate
markers, writes each changed file atomically, and is byte-idempotent.

Commit `6ff11c9` was accepted on 2026-08-25:

| Surface | Codex | Claude | Compact routing | CLI and skill receipt |
|---|---|---|---|---|
| Skiff laptop | MCP registered | MCP connected | installed and checked | `6ff11c9` |
| Anvil `dev-a` | MCP registered | MCP connected | installed and checked | `6ff11c9` |
| Anvil `dev-b` | MCP registered | MCP connected | installed and checked | `6ff11c9` |
| Anvil `dev-c` | MCP registered | MCP connected | installed and checked | `6ff11c9` |

The Anvil MCP serving these agents is PID 739989, CWD
`/srv/fleet/shared-tools/clj-surgeon-e7f72e2`, with `-Xmx512m`. The product
handler did not need a restart for the instruction rollout.

Verification covered 609 Babashka tests with 5,235 assertions and 197 JVM MCP
tests with 1,626 assertions at `-Xmx512m`. The unchanged stdio smoke advertised
all four tools and returned all three expected responses on Anvil in 7.52
seconds and on the laptop retry in 55.53 seconds. The laptop's first smoke
attempt timed out while system load was about 277; all earlier suites had
passed, and the unchanged retry passed as scheduler pressure fell.

Global instructions are loaded when an agent session starts. Thus every new
managed Codex or Claude session receives the route and has access to the shared
tool. An already-running session can retain old instructions or a cached MCP
schema. Start a new agent session if it rejects `delete_owners`,
`within.namespace`, or another field that production currently advertises.

## What to optimize next

Do not add generality because it is possible. Mine historical commits for
counterfactuals where the desired decision is reconstructable, then compare
fresh, same-model compact and native callers with exact outcome gates. The most
promising work has one or more of these properties:

- many exact owner operations;
- large source bodies that the final decision does not need to reproduce;
- several noncontiguous edits that share one frozen snapshot; or
- a repeated real-world refusal that one small address primitive can remove.

Keep the common model floor visible. If both routes fit in one short turn and
the native patch is small, a 2--5x claim is not credible. The goal is not to
replace the model's editor. It is to remove representation work the model
truly does not value.

## Evidence

- [Captain's Log: the compiled cleanup hit 4.61x](observations/2026-08-25-captains-log-the-compiled-cleanup-hit-four-point-six-x.md)
- [Compact editor versus native pilots](observations/2026-08-24-compact-editor-versus-native-pilots.md)
- [One-shot editor gesture plan](plans/one-shot-editor-gesture.md)
- [Global compact-editor routing plan](plans/global-compact-editor-routing.md)
