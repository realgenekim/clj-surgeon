# Captain's Log: Direct cclsp clients are retired

## Outcome

The workstation now has one public Clojure tooling boundary: clj-surgeon.
Agents do not register, discover, start, or call cclsp or clojure-lsp directly.
Surgeon may still use a private semantic provider when its structural evidence
cannot prove the answer, but that is an implementation detail behind the
Surgeon contract.

The cutover removed every active direct configuration found in a bounded
no-ignore scan, stopped every cclsp/clojure-lsp process outside the clj-surgeon
CWD, and added a permanent machine-wide audit.

## What clojure-lsp was doing

clojure-lsp is a semantic code-intelligence engine. In the retained Surgeon
window it was used for read-only definition/reference and call-hierarchy
questions. It can also perform code actions and refactors, so the program is
not intrinsically read-only. The retired direct MCP surface exposed more power
than agents needed. Surgeon's adapter confines present use to bounded semantic
queries and never delegates write authority.

Observed demand was small and initialization-heavy: three initialize requests
and one actual references request consumed 11.506 seconds of LSP wall, of which
9.603 seconds (83.46 percent) was initialization. This makes encapsulation,
syntax-first answers, selective escalation, and later snapshot-safe memoization
more attractive than one daemon per agent or repository.

## Configuration census

The no-ignore scan covered 309 repo-local MCP/config files and 566 repository
instruction files. It found:

- one direct Codex `[mcp_servers.cclsp]` registration in an active Sessionize
  diagnostic worktree;
- seven stale Claude `enabledMcpjsonServers` entries;
- one obsolete project Make target family in Reddit Scraper Fulcro;
- canonical direct-routing prose in Reddit Scraper Fulcro, Mothership,
  clojure-mcp-light, Sessionize, Curtain Call, and clj-surgeon;
- zero direct definitions in the global Codex config, global Claude settings,
  or current `.mcp.json` files.

All active configuration entries were removed. Canonical guidance now says to
use Surgeon semantic preparation and forbids direct semantic-provider write or
rename authority. Frozen benchmark baselines and implementation repositories
remain evidence, not active routing.

The requested `../**/Makefile` scan found 18 Makefiles with cclsp text:

- 15 belong to cclsp/clj-surgeon implementation or experiment worktrees and
  retain the private-provider lifecycle;
- three belong to the canonical Reddit Scraper Fulcro server and two frozen
  baselines. The canonical obsolete direct-install target was removed; the two
  baselines remain immutable counterfactual evidence.

## Process reap

Before termination, the direct legacy trees were:

- PID 7109, CWD `/Users/genekim/src.local/social-media-writer`, clojure-lsp,
  child of PID 7020 with the same CWD, direct cclsp wrapper;
- PID 7668, CWD `/Users/genekim/src.local/social-media-writer`, clojure-lsp,
  child of PID 7596 with the same CWD, direct cclsp wrapper.

The private shared provider was also outside the requested protected CWD:

- PID 48002, CWD `/Users/genekim/src.local/cclsp-structural-results`, provider
  supervisor;
- PID 3893, CWD `/Users/genekim/src.local/cclsp-structural-results`, provider
  broker.

All six processes were terminated. The two Claude parent sessions remained
alive. Surgeon MCP PID 65458, CWD `/Users/genekim/src.local/clj-surgeon`, was
preserved. It now returns provider-unavailable evidence for a semantic
escalation until an operator intentionally runs `make cclsp-start`; structural
reads and writes remain live.

## Permanent ratchet

`make cclsp-client-audit` emits
`clj-surgeon.direct-cclsp-client-audit.v1`. It scans repo-local Codex/Claude MCP
configuration and non-provider Makefiles. It reports only file, line, and a
bounded violation class; it never emits configuration values. It refuses:

- a Codex cclsp server block;
- a Claude cclsp enablement or JSON server key;
- a project Makefile cclsp target, install reference, or public `:7890` route.

Provider repositories, clj-surgeon implementation worktrees, and frozen
benchmark baselines are explicit exemptions. The real machine audit inspected
307 configuration files and 594 non-pruned Makefiles and returned zero
violations.

## Architectural boundary

```text
agent
  |
  | inspect_clojure / edit_clojure / apply_clojure_changes
  v
clj-surgeon  :7888
  |
  +-- structural proof from frozen source --------> terminal result
  |
  `-- named semantic proof gap
          |
          `-- private provider, started only when intentionally needed
```

The next hill is not to reproduce all of clojure-lsp. It is to measure the
small surface that callers actually need: exact Var definition, alias-aware
references, quoted Vars, and the few semantic cases structural proof cannot
close. The private provider remains an escalation oracle while Surgeon earns
those capabilities one exact relation at a time.
