# Census branch (bridge/census-verb 7244141) red-team: GO-WITH-FIX (2026-09-03T02:28Z); items 1–4 block merge

Opus, executed. The four study-ops classes do not reach this branch: no subprocess on the request path
(`fs/glob`, never `find`); reading is `rewrite-clj/parse-string-all`, no reader eval; every discovered
path re-enters `mcp-paths/resolve-source-path` (symlink out of root → `path-outside-project`, no bytes
leak); `doors` reach only `contains?` lookups, the one `re-pattern` is `Pattern/quote`d and not
caller-settable. Fences untouched.

| # | sev | finding | fix |
|---|---|---|---|
| 1 | block | `pool_size`/`files` bounded only by the advertised JSON schema; `pool_size 0` → untyped `mcp-adapter-failure`, `4096` → 4096 platform threads (mcp_relation_census.clj:319, :282) | server-side validation via `contract/validate-tool-params` like every other tool; typed refusal + next_call |
| 2 | block | every scanned file slurped before `defines-arms?`; the `files:` arm bypasses `max-source-bytes`; OOM escapes the adapter (`catch Exception`) | filter on a streamed read before retaining; size cap on requested paths; catch Throwable at the adapter boundary for a typed refusal |
| 3 | block | `file-seq` follows symlinked dirs; first escaping path `reduced`→ whole census refuses (`dev/checkouts/foo -> ../../foo` makes the tool unusable); skip-dirs filter results, not the walk; the file cap applies after a full walk | `Files/walkFileTree` with `preVisitDirectory` pruning, no link following; skip escaping paths (count them) instead of aborting |
| 4 | block | CLI: `(long "8")` ClassCastException on the registry's own `:threads 8` example; `:doors conj` accepted → every conj is `:door`, `raw 0`; no size/count cap on the CLI glob | parse threads; share the MCP door validation (one kernel); cap |
| 5 | fix | pool-invariance witness runs one file through the pool (helpers_only defines no arms) | ≥2 arm-defining fixtures; a census_pool unit test |
| 6 | fix | CLI selects core `pmap` when threads>1 and echoes `:pool-size` it never used | `census_pool/pooled-map` on both entrances |
| 7 | fix | ≤4 KB not guaranteed: `by_file` untrimmable → 4142 B with long paths | trim `by_file` last; assert the invariant |
| 8 | fix | `phases_elapsed_ms` mislabelled: `:discover` includes slurp+filter; `:parse` is a SECOND serial parse for `declared`, run even without `doors` | skip the declared pass without `doors`; return `:declared` from the parallel plan |
| 9 | fix | a write behind an undeclared helper yields zero sites → `raw 0`, `next_action none` reads as clean | publish `unrecognised_calls` (or fold into `:unknown`) |
| 10 | fix | `declared` collected from arm files only → doors in a helpers ns refused (the branch's own fixture shape) | collect from all scanned files |
| 11 | docs | README "exactly four tools", COVERAGE-001 "four", CLAUDE.md/skill silent on relation_census; test label "carries no file text" is false (excerpts by design); `workspace_root` unrestricted (inherited) but census is the first tool that enumerates a tree | update prose; fix the label; note the enumeration posture for Gene |

Good, no defect found: the classifier (dominance with polarity, `:unknown` first-class with four reasons,
worker throws → typed per-file refusals, merge re-keyed by path, depth-bounded target resolution,
`with-shutdown!`). Fix round launched on `~/src/clj-surgeon-census`; re-review after.
