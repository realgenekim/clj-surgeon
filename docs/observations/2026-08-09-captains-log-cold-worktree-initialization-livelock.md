# Captain's Log: The cold worktree never got warmer

**Date:** 2026-08-09

**Question:** Why did one exact `prepare-change` call and the sanctioned
recovery command consume roughly 97 seconds without producing a semantic
surface, even though structural inspection remained fast?

## Bottom line

The semantic graph was not expensive. The language-server process was alive,
but its launch environment had no locale and it never answered `initialize`.

A fresh Git worktree had no `.lsp` cache and an empty `.clj-kondo` directory.
cclsp launched clojure-lsp three times. Each child spent 45 seconds in the LSP
`initialize` request, hit the cold-initialization deadline, and was discarded.
The next caller then started from zero. Recovery amplified the failure by
launching another equally cold child.

Meanwhile, one `inspect_clojure` request read two exact forms in two files in
167 milliseconds. The structural service was healthy; only the semantic graph
was unavailable.

The lifecycle defects multiplied and obscured that failure, so the lease and
recovery repairs were still necessary. The final fix is an explicit UTF-8
locale at every service boundary, one lazy initialization lease per canonical
workspace, a complete LSP client that answers server requests, and a stable
supervisor that replaces the hot-reloaded HTTP child without overlapping
generations.

## Field evidence

The failed route was:

| Stage | Child | Result |
|---|---:|---|
| First semantic preparation | 6149 | `initialize` timed out after about 45 s |
| Subsequent semantic request | 10003 | a new `initialize` timed out after about 45 s |
| Sanctioned recovery | 17026 | a third `initialize` timed out after about 45 s |
| Exact structural snapshot | — | two files and two forms in 167 ms |

The fresh worktree contained no `.lsp` directory and a zero-byte
`.clj-kondo` directory. A warmed sibling worktree contained about 38 MB of
`.lsp` state and 132 MB of `.clj-kondo` state; its clojure-lsp child initialized
in 6.7 seconds.

This comparison initially implicated cold graph construction. That diagnosis
was incomplete. Later controls proved the same cache-empty fixture initializes
quickly when the process has a locale. Restart-on-timeout was one real product
defect; the missing service environment invariant was the reason the first
child never became useful.

## Why the current recovery contract was confusing

`clj-surgeon recover` correctly refused to claim success. Its terminal receipt
was `:fallback-safe`, preserved source, and named the semantic witness as the
failed phase. Two caller-facing defects remained:

1. The receipt pointed to a global `last-failure.edn`, so the agent inspected
   the file before and after recovery and briefly believed it belonged to
   another workspace.
2. The receipt promised a fallback but omitted an executable fallback field.
   The agent inferred the structural CLI route from prose.

The agent then made a reasonable but unsupported leap: because structural MCP
reads worked, it proposed continuing with verified MCP transactions. A failure
receipt should not require that interpretation. It should distinguish the
available capabilities explicitly.

## The corrective contract

The lifecycle should become:

```text
workspace registered
  -> one initialization lease starts or already exists
  -> every semantic caller joins that lease
  -> structural reads and exact-source preparation remain available
  -> initialization completes once or returns one typed terminal failure
  -> no caller timeout starts a replacement child
```

The machine result should separate service capabilities:

```clojure
{:structural-read :ready
 :structural-write :ready
 :semantic-surface :warming
 :source-anchor :retained
 :caller-proof :unavailable
 :safe-route :exact-source
 :fallback-command ["clj-surgeon" ":op" ":cat" "..."]}
```

The exact fallback command will depend on the retained evidence. The important
contract is that it is executable, project-confined, and derived mechanically.
No agent should inspect a recovery directory or reconstruct a command from
documentation.

## What to build

1. One initialization lease per canonical workspace in cclsp.
2. Separate interactive semantic deadlines from a longer bounded cold-start
   budget.
3. Recovery attaches to `:warming`; it does not restart.
4. Workspace onboarding publishes the new worktree immediately; the first
   semantic request begins bounded warming outside the config-watcher callback.
5. A semantic refusal retains exact-source evidence and names the capabilities
   that remain safe.
6. Recovery receipts use workspace/fingerprint-scoped paths and contain one
   executable report and fallback action.
7. One trace identifier joins Surgeon preparation, cclsp admission, the
   initialization lease, child PID, LSP session, and terminal receipt.

## Acceptance test

Create a real temporary Git worktree with no `.lsp` or `.clj-kondo` cache.
Launch two semantic requests and one recovery request against the same
canonical root.

The test passes only when:

- exactly one clojure-lsp child initializes;
- every caller observes the same initialization identity;
- caller timeout does not kill the child;
- recovery starts no replacement;
- success publishes one exact semantic surface, or failure publishes one typed
  terminal receipt;
- every failure leaves source unchanged;
- the receipt contains the available-capability matrix and executable next
  action; and
- a successful warm follow-up completes in under five seconds.

## Bitter-Lesson boundary

Do not copy or share clojure-lsp caches merely to make the benchmark green.
Cross-worktree cache reuse risks stale semantic evidence and requires its own
content-identity and invalidation proof. The first fix is more general: let the
authoritative semantic engine finish exactly once, retain its lifecycle state,
and make every caller observe that truth.

## Bottom line

Cold startup is allowed to be expensive. Repeating cold startup is not.

The perfect-tool experience is one bounded wait with visible progress, or an
immediate exact-source route whose limits are explicit. It is never two hidden
45-second waits followed by recovery archaeology.

## Implementation and real acceptance result

The repair changed both sides of the join.

cclsp now owns one initialization lease per configured workspace. Every LSP
operation applies its own short caller deadline without cancelling that lease.
`restart_server` attaches to a warming workspace instead of replacing its
child. Runtime status publishes `cold`, `warming`, `ready`, or `failed`, plus
the session, child PID, elapsed initialization time, and lease deadline. The
first implementation prewarmed newly added workspaces during config reload.
The causal investigation later moved that spawn to the first semantic request
so a file-watcher callback never owns a long-lived language-server transition.

clj-surgeon now generates a 120-second initialization lease while preserving
the 10-second interactive deadline. A semantic refusal retains the exact
source anchor and returns a capability matrix plus an executable structural
CLI command. Recovery receipts are stored under the deterministic workspace
receipt directory rather than one global file.

The first real rerun exposed another join loss: the Java MCP client discarded
`structuredContent` whenever cclsp also set `isError=true`. Surgeon therefore
reduced a precise warming result to an opaque `TextContent[...]` string. The
normalizer now retains the typed status, session, PID, source anchor, next
action, and executable next call on error results. A repeated SMW preparation
then returned those fields intact in 10.4 seconds instead of requiring receipt
archaeology.

The cache-empty acceptance workspace produced this timeline:

| Event | Result |
|---|---|
| `clj-surgeon up` | hot-added the workspace and, in that first implementation, began prewarming |
| Initial runtime state | `warming`, session `lsp-f7b…`, PID 33661 |
| Caller one | typed `semantic-provider-warming` after 10 s |
| Caller two | same typed result, same session, same PID |
| `clj-surgeon recover` | 14.3 s, no restart, `recovery_count=0` |
| Recovery receipt | workspace-scoped with exact `:cat :file … :form …` fallback |
| Lease terminal state | one typed 120-second initialization failure |
| Replacement children | zero |

The language server did not finish indexing this deliberately cache-empty
fixture before the bounded 120-second lease expired. That is a valid terminal
failure, not yet a speed win. The lifecycle criterion did pass: no caller or
recovery action discarded work, multiplied JVMs, or hid the safe structural
route.

Verification at this point:

- cclsp focused suite: 95 tests passed;
- cclsp full suite: 303 tests passed, 964 assertions;
- cclsp TypeScript lint and typecheck: clean;
- clj-surgeon focused recovery/onboarding suite: 23 tests, 122 assertions;
- clj-surgeon pure suite: 601 tests, 5,229 assertions;
- clj-surgeon MCP suite: 123 tests, 1,041 assertions;
- clj-surgeon stdio, heap, readiness, benchmark-harness, retention, and
  evidence-manifest gates: clean;
- clj-surgeon formatter and install: clean.

The first full-suite run also found a test-isolation defect. An embedded nREPL
client stopped waiting after five seconds, but its handler-redefinition eval
continued. Cleanup restored the process-global Vars before that late eval
finished, so the eval overwrote the restored values and contaminated later
tests. The test now waits for the terminal reply under suite load and stops the
executor before restoring the Vars. No behavioral assertion was removed. The
complete suite then passed from a fresh process.

## The final causal experiment

The remaining question was tested at four increasingly faithful boundaries:

| Route | Result |
|---|---:|
| `clojure-lsp dump` on the cache-empty fixture | 0.25 s |
| Transparent JSON-RPC initialize from a shell | about 1.3 s cold |
| cclsp `ServerManager` from a shell | 0.79 s |
| The same manager under a launchd-shaped environment with no locale | did not answer |
| Same launchd-shaped environment with only `LANG=en_US.UTF-8` added | 0.71 s |

That is a one-variable explanation. The graph was small. The protocol envelope
was valid. cclsp's manager was capable. The shared service environment was not.

The durable implementation now:

- supplies `LANG=C.UTF-8` and `LC_ALL=C.UTF-8` in the managed launch command;
- preserves those values in the stable Node development supervisor;
- repairs and publishes the effective locale in the HTTP runtime and
  `/healthz`;
- publishes hot-added workspace configurations without spawning an LSP child
  from the watcher callback;
- starts the one lease on the first real semantic request;
- answers standard server-initiated LSP requests and returns JSON-RPC
  `-32601` for unknown requests instead of leaving the server waiting; and
- logs every server request before dispatch so a future wedge has a causal
  trace.

## Real acceptance

The actual shared MCP, not a test double, resolved the cache-empty fixture after
hot reload:

| Call | Wall time | Result |
|---|---:|---|
| First semantic request | 3.43 s | exact definition and two references |
| Warm follow-up | 1.29 s | same session and same two references |
| Cold request after the final supervisor restart | 4.69 s | exact definition and two references |

The first request created one clojure-lsp child. The workspace moved from
`cold` to `ready`; `/healthz` published `C.UTF-8`; and the follow-up reused the
same LSP session. The former 120-second non-result became a correct cold answer
in under four seconds.

After restarting the supervisor itself, not only its hot-reloaded HTTP child,
the shared MCP reconnected without an agent restart and repeated the cold proof
in 4.69 seconds. The stable process tree was one Node supervisor and one Bun
HTTP child, both under the explicit UTF-8 locale.

Final verification:

- cclsp full suite: 305 tests passed, 970 assertions, 5 intentional skips;
- cclsp focused locale, supervisor, lease, and protocol suite: 73 tests passed;
- cclsp lint and TypeScript typecheck: clean;
- clj-surgeon pure suite: 601 tests, 5,229 assertions;
- clj-surgeon MCP suite: 123 tests, 1,041 assertions;
- clj-surgeon stdio, readiness, benchmark harness, retention, and evidence
  manifest gates: clean; and
- stable CLI plus both agent skills: installed.

Cold startup is allowed to cost something. An absent process invariant is not
startup cost. Perfection here means the service supplies the invariant, proves
it in health data, and still verifies the next real semantic query.
