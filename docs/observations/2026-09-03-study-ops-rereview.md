# study-ops re-review at 212b045 — GO-WITH-FIX (NO-GO lifted); items 1–4 before the queue (2026-09-03T04:02Z)

Headline (measured, before 8a52931 / after 212b045): ls-tree over 1100 real files at 512 MB heap 3346 ms →
333 ms (10×, flat in tree size); 3000-file tree refused in 55 ms with 0 parsed; `grep "--pre=/bin/sh"`
→ typed refusal, no subprocess; `#=(spit …)` in all three build-file kinds writes nothing. Items 1–5, 7,
9–12 CLOSED; 6 and 8 PARTIAL. `mcp_paths` additions purely additive (existing resolvers byte-identical).
`forms/find-config-file` reach from an MCP ls-tree is ZERO (aliases atom is CLI-populated only; the prior
"fanned out per outlined file" was inaccurate); the walk remains reachable only from prepare-change.

| # | sev | hole (witnessed) | fix |
|---|---|---|---|
| 1 | block | REGRESSION: `source_character_count` was redefined for outline/study ops as source READ, but `enforce-output-budget` charges it as source RETURNED (65,536) → `outline` refuses 7 files in this repo (e.g. intent_transaction.clj 126,596 chars) with an unactionable `request_less_evidence` | exempt no-source results from the returned-source budget, or a separate read-count field |
| 2 | block | `max_files` bounds the COUNT after `discover-projects` materialises; build files with `:paths [".."]` make discovery quadratic: 500 files + 501 deps.edn → file_count 250,500, 8.4 s; overlapping projects double-count (`total-file-count` no dedup; a 2-file tree reports 5 and prints files 2–3×) — falsifies STUDY-015 | dedup across projects; apply the cap during accumulation |
| 3 | block | `ns_grep` (branch-introduced): `re-pattern` unguarded → `"["` or `5` → `mcp-adapter-failure` with a raw Java message, no typed fields; `grep:5` passes the `^-` check and breaks the output schema (`isError`, no error_type) | compile once under a guard → `invalid-ns-grep-pattern`; type-check grep/ns_grep server-side |
| 4 | block | item 6 defeated by spelling the default: `limit 4096` brings the self-returning next_call back (`ls-tree-next-call` never carries `:limit`; `ls-tree-request-arguments` keeps it) | carry `:limit` in the continuation or drop it in the request args |
| 5 | fix | per-project headers still print shown counts; a project dropped by truncation vanishes with `project_count 2` | true counts; name dropped projects |
| 6 | fix | `invalid-grep-pattern`'s continuation echoes the rejected pattern (invalid-format got the drop treatment) | explicit continuation dropping it |
| 7 | fix | `no-clojure-files` and `study-tree-too-large` embed the canonical root (STUDY-006) | scan-relative dir in messages |
| 8 | fix | a known-futile `next_call` (required 28,168 > ceiling, limit 4096 → raise to 16,384) | `raisable?` also requires required ≤ ceiling |
| 9 | fix | nothing pins the parallel strategy (swap `bounded-map` → `map` and every test passes) | strategy witness + partial-n equality |
| 10 | fix | `core.clj:734` rendered rg remedy lacks `--` | add it |
| 11 | fix | `text` empty receipt at `limit 1` is 40 chars, floor undocumented; `-prune` now hides `src/app/target/` unwitnessed | document floor; golden with a target dir |

Round three launched on `~/src/clj-surgeon-study`; a short re-check after.
