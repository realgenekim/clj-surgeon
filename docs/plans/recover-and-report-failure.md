# Recover and Report Failure

**Issue:** `clj-surgeon-9gj`

## Observable contract

`clj-surgeon recover [WORKSPACE]` performs one bounded repair attempt against
the shared local MCP stack. Success requires a real structural probe after
workspace registration. Health endpoints alone do not prove recovery.

```clojure
{:ok true
 :operation :clj-surgeon-recover
 :terminal-state :recovered
 :workspace "/canonical/root"
 :proof {:catalog {:inspect-clojure true :apply-clojure-changes true}
         :semantic {:subject "sample.core/target"
                    :lsp-session "lsp-..."}
         :mutation {:verification-complete true
                    :source-unchanged-after-cleanup true}}
 :agent-session-restart-required false
 :next-action :none}
```

The command stops after one repair attempt. It never asks the caller to repeat
`up`. An unknown Streamable HTTP session returns protocol status 404 so a
conforming client can reconnect. If the current client still cannot refresh,
the receipt returns `:terminal-state :restart-required` and names that boundary
once.

`clj-surgeon report-failure --receipt PATH` accepts one EDN recovery receipt.
It redacts private context, computes a stable failure fingerprint, and creates
or updates one local Bead in the clj-surgeon repository. It never creates a
public issue. When the local source repository or Beads database is
unavailable, it returns the redacted issue draft as data and does not write.

## First production defect

The installed Babashka CLI reproduced this failure during `up`:

```clojure
{:type :sci/error
 :error "Method close on class sun.nio.ch.FileLockImpl not allowed!"
 :error-type :invalid-arguments}
```

The OS lock is valid. The defect is lifecycle syntax: `with-open` invokes
`FileLock.close`, which SCI does not expose. Closing the owning file channel
releases the lock on both Babashka and the JVM. The implementation must not
invoke methods on the concrete `sun.nio.ch.FileLockImpl` value.

## Second production defect

The Social Media Writer worktree reproduced a timeout inversion during the
semantic witness. cclsp detected a 10-second `textDocument/documentSymbol`
timeout, replaced only that workspace's clojure-lsp child, and completed the
requested `resolve_var_surface` in 21.683 seconds. The recovery client's generic
10-second HTTP deadline expired first, so `clj-surgeon recover` returned
`:fallback-safe` even though the bounded inner recovery succeeded.

The recovery probe must keep ordinary MCP requests on the short deadline. Its
cclsp semantic witness must use a 60-second outer deadline so cclsp can spend up
to 55 seconds on one bounded request and return its typed result. This does not
authorize retries or an unbounded wait. A semantic witness that exceeds the
60-second deadline still returns one terminal failure receipt.

## Third production defect

The first installed SMW verification reached onboarding while the shared cclsp
health endpoint and managed launchd job were already live. One one-second
health probe missed during concurrent provider work. `cclsp-start` treated that
single miss as process death, removed the healthy shared service, and then
failed to replace it before the readiness deadline. Recovery returned
`:mcp-lifecycle-failed` before it could exercise the repaired semantic witness.

The lifecycle entrance must retry a transient health miss while the managed
launchd job remains present. A later healthy response with the expected config
must return success without `launchctl remove` or `launchctl submit`. A healthy
response with a different config keeps the existing managed/unmanaged ownership
rules. Bounded retries that never obtain health may still replace the managed
service once.

## Anchored semantic fast path

An exact Surgeon source anchor currently proves the owner and whole-form range,
but not the owner-name token position. cclsp therefore calls
`textDocument/documentSymbol` to rediscover that position before it can call
`textDocument/references`. The SMW recovery witness timed out twice on that
discovery call before the reference query ran.

Named-form anchors must add a zero-based, end-exclusive `selection_range` for
the exact owner-name token. cclsp must independently verify the file hash,
range containment, and token bytes. A valid anchored request then calls
`textDocument/references` directly. It must not call
`textDocument/documentSymbol`. Unanchored discovery retains the existing
fallback. Returned reference locations remain LSP authority; Surgeon maps them
to exact source owners while it prepares the change basis.

## Behavior matrix

| Case | Result | Side effects |
|---|---|---|
| Healthy registered workspace | `:recovered`, zero destructive restarts | Real probe only |
| Missing workspace registration | Register once, then probe | One verified config update |
| Invalid MCP/cclsp session | Recover only that workspace, then probe | Other workspaces unchanged |
| cclsp performs one bounded child recovery | Wait up to 60 seconds for its typed result | No parent or unrelated child restart |
| One cclsp health probe misses while the managed job remains live | Retry health, then retain the same service | No remove or submit |
| Exact source anchor includes a valid owner selection range | Query references directly | Zero `documentSymbol` requests |
| Anchor selection is missing, outside the owner, stale, or names another token | Refuse or use the documented compatibility route | No false semantic proof |
| Structural probe fails after repair | `:fallback-safe` | Redacted receipt available |
| Client catalog cannot refresh | `:restart-required` | No server restart loop |
| First report for fingerprint | Create one local Bead | Redacted data only |
| Repeated report for fingerprint | Append evidence to same Bead | No duplicate issue |
| Off Gene's laptop | Return issue draft as data | No Bead or GitHub write |
| Invalid or private receipt fields | Refuse or redact | No issue write |

## Safety and privacy

- Canonicalize every workspace path.
- Never include prompts, source text, commands, URLs, account names, or raw
  workspace paths in an issue.
- Hash the canonical workspace and failure signature.
- Use `bd --directory` with the discovered clj-surgeon root. Never depend on the
  consumer's current repository.
- A failed report must not hide the original recovery receipt.
- Shared-service repair must not restart healthy unrelated workspace children.

## Non-goals

- Do not make `recover` an unbounded supervisor.
- Do not infer source changes or apply user-code mutations. A uniquely named,
  tool-owned probe form may be created, transactionally changed, verified, and
  removed inside the workspace.
- Do not create GitHub issues.
- Do not promise that every MCP client honors `tools/list_changed`; report the
  exact client boundary when it does not.
- Do not encode project-specific semantic probe Vars.

## Test layers

1. Pure lifecycle and receipt classification with literal data.
2. Pure redaction, fingerprinting, deduplication choice, and issue-draft tests.
3. The faithful Babashka `FileLock.close` regression through installed-style
   CLI execution.
4. CLI help, parsing, EDN stdout, success exit, refusal exit, and documented
   invocations.
5. A fake-runner recovery boundary that proves one attempt and no unrelated
   restart.
6. A real local recovery call that proves tool catalog, structural source,
   cclsp semantics, and a guarded write through fresh MCP sessions.

## Completion gates

- Format all changed Clojure files.
- Focused pure and CLI tests pass.
- `make mcp-test` and `make test` pass without weaker assertions.
- `make install` publishes the commands.
- The installed `up` command no longer produces the SCI refusal.
- A deliberately failed receipt creates or updates one deduplicated local test
  issue only inside an isolated Beads fixture.
- One real `recover` invocation returns terminal EDN and a real structural
  probe succeeds afterward.
