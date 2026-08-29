# Owner-aware symbol-migration call-construction screen

**Status:** designed and zero-model scored; no model or Anvil run authorized yet

**Integration base:** `4f69761968af256d767ac97948f88bfb48cdcf1e`

## Decision question

Does one grouped, owner-aware symbol-migration table make a fresh Sol/high
caller emit the already-decided 51-change `edit_clojure` transaction materially
faster than the current literal-edit schema, without adding a refusal,
recovery action, or hidden semantic choice?

This is a call-construction screen, not a mutation benchmark. Both arms expose
one capture-only `edit_clojure` tool. The adapter records the first arguments
and returns success without reading or writing source. The offline scorer then
uses the real validator and transaction compiler against the frozen
`submission-row-extraction-cleanup` before sources.

## Frozen arms

Everything except the `edit_clojure` description and one input-schema property
is identical: task, source text, fixture, tool name, available tool set,
annotations, output schema, server instructions, Sol/high caller, fresh Codex
home, and counterbalanced position.

### Control

The exact `mcp-tool/tool-description` and
`mcp-schema/editor-tool-schema` at the integration base.

### Candidate

The exact control surface plus this property:

```json
{
  "symbol_migration": {
    "target_alias": "submission-row",
    "target_rule": "preserve-name",
    "columns": ["owner", "from", "matches"],
    "files": [
      ["src/sample/views/log.clj", [
        ["describe-rating", "review/fmt-stars", 3]
      ]]
    ]
  }
}
```

The exact complete candidate contains nine file groups, 23 owner rows, and 27
declared matches. It retains the nine namespace edits, the one bespoke owner
edit, and the 14-owner deletion group as ordinary current fields. The pure
lowerer expands every row to:

```json
{
  "file": "src/sample/views/log.clj",
  "within": {"form": "describe-rating"},
  "from": "review/fmt-stars",
  "to": "submission-row/fmt-stars",
  "matches": 3
}
```

`preserve-name` is the only candidate rule. The kernel does not discover or
choose files, owners, old symbols, target aliases, or counts.

The zero-model serialized sizes at this checkpoint are deliberately retained
as a design cost, not hidden as implementation detail:

| Surface | Control | Candidate | Delta |
|---|---:|---:|---:|
| Tool description | 4,189 chars | 4,517 chars | +328 |
| Input schema | 3,348 chars | 4,356 chars | +1,008 |
| Emitted call arguments | 6,353 bytes | 4,347 bytes | -2,006 |

The candidate therefore makes the output materially smaller while making the
pre-call tool surface 1,336 characters larger. The model experiment must decide
whether the more explicit relation reduces construction time enough to repay
that added instruction/schema cost.

## Causal route

```text
same complete task + one visible edit tool
                    |
          fresh Sol/high caller
                    |
             first MCP call only
                    |
         capture arguments; write nothing
                    |
     control ---------------- candidate
       |                           |
current request             expand symbol table
       |                           |
       +-------- same real --------+
        validate-tool-params
          -> tool-params->transaction
          -> compile-transaction
          -> nine future hashes
```

The scorer never accepts balanced JSON, a plausible tool call, or a hash of
the submitted payload as correctness. It requires the real compiler to produce
all 51 concrete matches, nine changed files, and every frozen capsule after
hash.

## Cohort and gates

Use eight fresh serial calls in two counterbalanced blocks:

```text
A B B A   B A A B

A = current control
B = owner-aware candidate
```

The candidate earns another product-design step only if:

1. control is 4/4 correct and candidate is 4/4 correct;
2. each first emitted call compiles through the real validator/compiler;
3. each run has one MCP action, zero refusals, zero recovery calls, zero shell
   calls, and zero file-change actions;
4. candidate call arguments are at most 4,500 serialized JSON bytes;
5. every run produces the identical frozen 51-change future hashes; and
6. candidate median turn-start to first-call emission is at least 15 percent
   lower than control.

Complete turn time is retained as secondary telemetry. This screen isolates
call construction; it does not claim the candidate improves server execution,
verification, or receipt interpretation.

## Ambiguity and falsification risks

| Risk | Boundary |
|---|---|
| Positional rows are compact but easier to transpose than objects. | `columns` must be exactly `[owner,from,matches]`; wrong order refuses before the current contract. |
| `prefixItems` support may differ between the MCP server, Codex ingestion, and later production validators. | Capture the exact client-visible schema. Any missing/rejected property invalidates the run. |
| Two rows may overlap or repeat one owner/from pair. | The unchanged transaction compiler remains authoritative; overlap refuses. No deduplication. |
| An unqualified `from` could be a local binding or a different Var. | Owner scope plus exact subtree count is the only authority. The table performs syntax replacement, not resolved-symbol migration. Product naming must not imply more. |
| `target_alias` may conflict with a local or namespace alias. | Alias choice remains caller judgment. The candidate never inserts or validates requires beyond separately supplied exact namespace edits. |
| `preserve-name` derives only the terminal symbol name. | No rename rule, fuzzy relation, or namespace inference is permitted in this candidate. |
| The long task itself may teach the literal control representation. | Both arms receive identical task bytes. The candidate must win despite that conservative bias; do not rewrite the task between arms. |
| Cached schema, skills, or repository instructions may leak old tool knowledge. | Use a fresh Codex home, one captured tool, no skills, a read-only empty workspace, and retain the independently observed tool registry. |

## Zero-model evidence

The pure scorer and capture adapter live under `dev/experiments`; production
schema, handlers, registry, installation, and shared MCP runtime remain
unchanged. The zero-model gate proves:

- the candidate adds exactly one top-level schema property;
- both captured call shapes compile to the same normalized transaction;
- both produce the frozen 51-match, nine-file future;
- the candidate remains within the 4.5 KB argument budget;
- a wrong owner remains invalid on the first call; and
- the N=8 aggregate gate rejects a cohort with fewer than four correct runs per
  arm or less than 15 percent emission improvement.

No model, Anvil, install, reload, shared port, or source mutation belongs to
this checkpoint.
