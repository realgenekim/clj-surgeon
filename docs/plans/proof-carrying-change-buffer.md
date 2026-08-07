# Proof-Carrying Change Buffer

**Status:** Implemented experiment on `local/mcp-perfect-tool-experiment`

## Outcome

One semantic decision remains one edit transaction.

```text
understand the goal
  -> inspect_clojure prepare-change
  -> fill explicit decision holes
  -> apply_clojure_changes
  -> receive verified receipt
```

The model chooses meaning once. The server owns symbol resolution, exact source
addresses, hashes, counts, write order, rollback, verification, and receipts.

This feature adds no MCP tool. It extends the existing two-tool contract:

- `inspect_clojure` gains `mode: "prepare-change"`.
- `apply_clojure_changes` accepts the returned basis and decisions.

The direct `apply_clojure_changes` request remains the preferred route when the
caller already knows every exact replacement. Native patching remains the
control for one small arbitrary text edit.

## Why this shape

The previous route made the caller retain mechanical state:

```text
read -> select -> edit -> inspect -> repeat -> reconstruct -> test
```

The change buffer moves that state into a short-lived server basis. The basis
retains the complete source snapshots and structural addresses. The response
shows only exact source that can change the model's decision.

The design follows the repository's bitter-lesson boundary:

- cclsp and clojure-lsp resolve the named Var and its references.
- clj-surgeon anchors those locations to lossless Clojure forms.
- The model supplies `keep` or one exact replacement form.
- The transaction compiler applies retained addresses. It does not rerun a
  selector or infer a replacement.

The tool does not interpret natural language, recommend architecture, invent
source, or silently treat unresolved evidence as absent.

## Exact public contract

### Prepare

```json
{
  "mode": "prepare-change",
  "subject": "clj-surgeon.mcp-contract/normalize-success-receipt",
  "intent": "Add context to the terminal receipt without changing callers"
}
```

`subject` must contain one fully qualified Var as `namespace/name`. `intent`
must contain one concise model decision. `verify` is optional and defaults to
`fast`. Unknown fields refuse through the closed MCP schema.

The successful `structuredContent` has this shape:

```json
{
  "ok": true,
  "operation": "inspect_clojure",
  "mode": "prepare-change",
  "basis": "cb-7f3a...",
  "subject": "clj-surgeon.mcp-contract/normalize-success-receipt",
  "intent": "Add context to the terminal receipt without changing callers",
  "site-count": 3,
  "file-count": 2,
  "visible-character-count": 3412,
  "sites": [
    {
      "id": "s1",
      "file": "src/clj_surgeon/mcp_contract.clj",
      "role": "definition",
      "owner": "normalize-success-receipt",
      "line": 260,
      "end-line": 291,
      "source": "(defn normalize-success-receipt ...)"
    },
    {
      "id": "s2",
      "file": "src/clj_surgeon/mcp_contract.clj",
      "role": "reference",
      "owner": "classify-kernel-result",
      "line": 299,
      "end-line": 299,
      "source": "(normalize-success-receipt project-root result)"
    }
  ],
  "next_call": {
    "basis": "cb-7f3a...",
    "decisions": [
      {"site": "s1", "replace": null},
      {"site": "s2", "replace": null}
    ],
    "verify": "fast"
  },
  "read_complete": true,
  "source-unchanged": true
}
```

The text result contains counts and the basis only. Source appears once in
`structuredContent`.

The definition site is the complete top-level definition. Each reference site
is the complete named owner reported by cclsp. This gives the model the code
that consumes a return value, not only the call expression that produced it.
Identical owner paths deduplicate to one decision site. Comments inside an
owner remain exact source. Textual lookalikes do not become semantic sites.

### Decide

Copy `next_call`. Replace every `null` with exactly one action.

Keep a site:

```json
{"site": "s2", "keep": true}
```

Replace a site:

```json
{"site": "s1", "replace": "(defn normalize-success-receipt ...)"}
```

The caller must answer every site exactly once. A decision cannot contain both
`keep` and `replace`. A replacement must contain one complete Clojure form and
must differ from the prepared source. The caller must not change the basis or
site IDs.

### Apply

```json
{
  "basis": "cb-7f3a...",
  "decisions": [
    {"site": "s1", "replace": "(defn normalize-success-receipt ...)"},
    {"site": "s2", "keep": true}
  ],
  "verify": "fast"
}
```

Apply compiles retained zipper paths against retained source hashes. It does
not resolve the Var again. It preflights every source hash before the first
write, commits all changed files, reads them back, and runs the named closed
verification profile.

If verification fails, apply compiles the inverse from the same transaction
and restores every changed file. A successful apply publishes a durable inverse
receipt and returns compact terminal evidence:

```json
{
  "ok": true,
  "operation": "apply-basis",
  "basis": "cb-7f3a...",
  "match-count": 2,
  "changed-file-count": 2,
  "verification_complete": true,
  "receipt-file": "...edn",
  "receipt-hash": "...",
  "read_back_hashes": {"...": "..."},
  "next_action": "none"
}
```

## Authorities and retained state

cclsp owns cross-file semantic resolution. Its `resolve_var_surface` operation
uses clojure-lsp to return one exact definition and owner-enriched references.
It refuses zero or multiple workspace definitions.

clj-surgeon owns source and mutation evidence. Each basis retains:

- the normalized subject, intent, and verification profile;
- one source snapshot per canonical file;
- SHA-256 source hashes;
- lossless zipper paths and preorder addresses;
- exact before-source and source ranges for every site.

A basis is process-local, opaque, and short-lived. The store keeps at most 32
bases for one hour. It does not make a durable address promise.

## Closed budgets

Prepare publishes no basis when any default limit is exceeded:

| Limit | Default |
|---|---:|
| Deduplicated sites | 24 |
| Agent-visible source characters | 12,000 |
| Retained source snapshot characters | 4,194,304 |

The refusal reports the observed counts and the limits. The remedy directs the
caller to narrow the subject or use typed inspection for a manual decision.

## Verification profiles

Requests select a profile name. They cannot submit shell commands.

| Profile | Closed behavior |
|---|---|
| `fast` | Run clj-kondo and Standard Clojure Style on changed files only |
| `full` | Run `make test` |

Whole-file parse and read-back hashing happen before either profile. The fast
profile is an editing-loop gate. The full repository suite remains the release
gate.

## Failure contract

Every expected failure is data and leaves source unchanged, or reports a
verified rollback:

| Failure | Stable error |
|---|---|
| Subject is not `namespace/name` | `invalid-change-subject` |
| Semantic provider is unavailable | `semantic-provider-unavailable` |
| Zero or multiple definitions | `semantic-provider-refusal` |
| Provider path escapes the project | `semantic-path-outside-project` |
| No containing structural forms | `semantic-sites-not-addressable` |
| Site or character budget exceeded | `change-buffer-budget-exceeded` |
| Basis is absent or expired | `unknown-or-expired-basis` |
| Missing, duplicate, or extra site | `basis-coverage-mismatch` |
| Both or neither decision actions | `invalid-basis-decision` |
| Replacement equals before-source | `unchanged-basis-decision` |
| Source changed after prepare | `source-hash-mismatch` |
| Retained address is stale | `stale-path` or `stale-subform` |
| Edits overlap | `overlapping-intents` |
| Verification fails | `verification-failed` with rollback evidence |

## Hot development contract

`make mcp-start` starts both loopback services:

```text
clojure-lsp <-> cclsp :7890 <-> clj-surgeon :7888 <-> coding agent
```

cclsp uses a pinned repo-local Bun and reloads TypeScript on save. The
clj-surgeon process exposes nREPL. Reload changed handler namespaces without
restarting the MCP listener. A tool schema or catalog change still requires a
server restart because the SDK publishes that contract at initialization.

## Required evidence

The experiment is incomplete unless all of these remain green:

- pure address, decision, budget, stale-source, overlap, and rollback tests;
- a real HTTP MCP prepare -> decide -> apply test in one session;
- cclsp unit, HTTP transport, typecheck, and lint suites;
- clj-surgeon focused and full suites;
- formatting, clj-kondo, and `git diff --check`;
- one self-hosted transaction with a durable receipt;
- a clean-agent benchmark against current direct MCP and native controls.

The implementation can ship as an experiment before it wins the clean-agent
benchmark. It cannot claim the 3x product goal until matched correct runs show
that result.

## First clean-agent evidence

One paired return-contract task completed correctly through the exact intended
route:

| Route | Wall | Tool route |
|---|---:|---|
| Native control | 54.13 s | Search, bounded reads, native patch, checks |
| Proof-carrying basis | 31.00 s | Prepare, then basis apply and verify |

The treatment used two MCP calls, no source reads after the skill, and no
fallback. It saved 23.13 seconds, reduced wall time by 42.7%, and was 1.75x
faster. This satisfies the clean-caller usability requirement for the first
probe. Replication remains required before a stable performance claim. The 3x
product gate remains open.
