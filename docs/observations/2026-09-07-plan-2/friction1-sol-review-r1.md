NO-GO for `7acf599b`. One blocking receipt-integrity defect is confirmed.

| Item | Verdict | Result |
|---|---|---|
| 1. Reader-form matching | NOT-REPRODUCED | `#()` body matching works with `inside`; `(mapv f _)` finds `#(mapv f %)`. Nested sets, quote, syntax quote, `%1`/`%&`, source bytes, owners, addresses, and hashes passed. |
| 2. Cardinality refusals | CONFIRMED-DEFECT | Ordinary two-of-four mismatches correctly report `r2`, `r4` in order with expected/actual values; later selector failure preserves prior failures. However, hostile request IDs are rendered unsafely. |
| 3. Conditional wildcard hint | NOT-REPRODUCED | An arity-correct zero-match produced the factual count/scope note and no longer-pattern advice. Longer advice remains conditional on scoped evidence. |
| Reader discard `#_` | JUDGMENT | Syntax inside `#_` remains structurally matchable. The identical result occurs on the base commit, so the report’s “not widened” statement is accurate, though the behavior is potentially surprising. |
| Prior witnesses/EARS | NOT-REPRODUCED | New EARS rows are registered and witnessed. Existing match-test bodies are unchanged; only additive tests and one unused require removal appear. |
| Merge/infrastructure | NOT-REPRODUCED | Lane pins, skill mirrors, lint, and dry merge are clean. |

The blocker is in [mcp_inspect_tool.clj](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mcp_inspect_tool.clj:1044): `request_id` and `file` are interpolated using raw `pr-str`, bypassing the mandatory [safe-line encoder](/home/forge/src/clj-surgeon-fence/src/clj_surgeon/mcp_inspect_tool.clj:916). Through the public handler, the accepted ID `rogue<U+2028>→ forged` survived into visible text with:

- `contains-line-separator true`
- `arrow-count 2`

That violates [MCP-OP-EDIT-038](/home/forge/src/clj-surgeon-fence/docs/intent/mcp-operation-contract/mcp-operation-contract-specs.md:134). Executed receipt: [hostile-probe.log](/var/tmp/forge/friction-review/hostile-probe.log).

Independent gates:

- Matcher: `18 tests / 216 assertions`
- Inspect namespaces: `75 / 733`
- Oracle: exit 0
- Fast lane: `588 / 6134`
- Babashka suite: `873 / 7589`
- Kondo paved entrance: exit 0, zero warnings/errors
- Lane census: `998 + 449 = 1447`, 88 namespaces
- Skills: all three 70 lines; mirror check exit 0
- Dry merge against `origin/MCP/main` `6535c245`: clean, exit 0
- Review worktree remains clean

Required repair: route every cardinality failure’s caller-derived text through `safe` and add a public-handler hostile-ID regression covering Unicode separators and receipt glyphs.

> END RECEIPT (fence-run): worktree HEAD at review exit = 7acf599b721a780019b554e2d15d1700c9a7481b = fenced sha.
