# (superseded 2026-09-03: re-review at 212b045 lifts this to GO-WITH-FIX — see 2026-09-03-study-ops-rereview.md) NO-GO: bridge/study-ops-mcp b3c17bb must not merge until the security round lands (2026-09-03T02:12Z)

Opus red-team (standing in for Sol; codex is unauthenticated on the Anvil seat), executed against the
branch bytes, no server started. Verdict **NO-GO**; items 1–4 blocking.

| # | sev | site | finding | fix | witness |
|---|---|---|---|---|---|
| 1 | critical | study.clj:194-206 `grep-tree`; pass-through mcp_inspect_tool.clj:472 | `grep` reaches ripgrep as a bare positional with no `--`; `grep: "--pre=/bin/sh"` executes every scanned file (executed: `$S/PWNED` created, receipt said `no-clojure-files`). The `(str dir)` positional becomes the pattern, so rg searches CWD — also escapes the confined dir. | `"--"` (and `-e` for the grep fallback) before the pattern; reject `^-` patterns in `execute-ls-tree` | `grep-cannot-become-an-rg-flag`: `grep "--pre=/bin/sh"` → `invalid-grep-pattern`; argv unit asserts `"--"` directly before the pattern |
| 2 | critical | study.clj:119 `extract-source-paths` | `clojure.core/read-string` with `*read-eval*` true on every deps.edn/bb.edn/project.clj in the tree; `#=(clojure.core/spit …)` executed (proved). CLI-only before; MCP-reachable now, wrapped in a silent catch. | `clojure.edn/read-string`; project.clj with `*read-eval*` false or rewrite-clj | fixture deps.edn with `#=(spit …)`; assert the file is never written |
| 3 | high | study.clj:122-134, :151, :350 | `find-clj-files` matches by name and follows symlinks: `src/leak.clj -> /etc/passwd` is outlined (executed); `:paths ["../../.."]` in a scanned deps.edn moves the scan outside the root (`fs/path` unnormalised) | after `find`, drop files whose realpath is outside root; normalise `(fs/path root p)` and drop escapes | `ls-tree-does-not-read-through-a-symlink-out-of-the-root` (the MCP-OP-STUDY-006 falsifier the suite promises but lacks) |
| 4 | high | study.clj:241-259 + :419; mcp_inspect_tool.clj:447 | the whole tree is outlined (`pmap`, not claypoole) BEFORE any bound: 1072 files → 618 MB heap, 2.86 s, to return 3 files; ~0.55 MB/file linear → a 10k-file tree ≈ 5.7 GB; no file cap, no timeout, no `-prune` in `find-clj-files` | cap discovery (`study-tree-too-large` refusal), outline lazily to the byte budget, claypoole `upmap` bounded, prune skip-dirs | `ls-tree-refuses-an-oversized-tree-before-parsing-it` (3000 files; elapsed bound; <50 outlined) |
| 5 | med-high | mcp_inspect.clj:709-717 `study-oversized` | self-returning `next_call` at the ceiling (`(min 16384 (max required limit))`) — the loop MCP-OP-STUDY-007 forbids | mirror `study-truncation`: no call at the ceiling, `narrow_scope` | extend `a-study-receipt-is-bounded-and-says-so` with `study-max-limit` redef |
| 6 | med | mcp_inspect_tool.clj:430-444 `ls-tree-refusal` | unconditional `next_call {:dir "."}` equals the failing call for `no-clojure-files` at `"."` | omit when identical; `narrow_scope` | `grep "zzzz"` at `"."` → no `next_call` |
| 7 | med | mcp_inspect.clj:668/:554, :740-750, :722-728 | `source_character_count 0` hardcoded; `topo` bounds only `:sorted` (`:cycles` unbounded, `form_count` 0 on an all-cycle file); `deps` with `:form` unbounded | compute the count; budget `:cycles` and ls-extract's other keys; oversized check on single-form deps | equality with `(count (slurp file))`; all-cycles fixture within limit |
| 8 | med | study.clj:303, :309 | the text payload's `── total: N files` counts KEPT projects; receipt says 1072/3 while the body says total 3 | pass true totals or suppress under truncation | `str/includes? (:tree r) (str "total: " (:file_count r))` |
| 9 | low-med | mcp_inspect.clj:642-656 `bound-rows` | off-by-one: array brackets/separators not charged → payload can be limit+1 | seed `used` at 1 | loop n in 1..400 asserting `≤ n` |
| 10 | low | mcp_inspect_tool.clj:452 region; :1053 | ls-tree branch skips `validate-inspect-params`; `format` unvalidated (echoes raw) | validate `#{"text" "edn"}`, reject unknown keys | `format "EDN"` → `invalid-format` |
| 11 | low | core.clj:148 | `run-ls-tree` still destructures `format` (harmless now) | rename to `output-format`; add to the golden with an error path | golden |
| 12 | low | study.clj:186, :191 | `rg --version` spawned twice per grep scan | hoist into a let | covered by 1's argv test |

Also noted: `forms/find-config-file` walks up to `/` with no root bound and SCI-evaluates `.clj-surgeon.edn`
`:fields` — pre-existing, now fanned out per outlined file; the SCI fence itself (`sci-opts`, no `:classes`)
is intact and cannot reach `babashka.process`. Parity witness compares projections at one limit on
non-truncating inputs; bounding is MCP-only and unwitnessed against the CLI. Contract: the `mode` enum
widening breaks no existing caller.

**Disposition (bridge):** items 1–4 blocking; 5–12 in the same batch. The security round runs on the
same worktree after the names-only builder lands (it was told to put `--` before the pattern if it
touches `grep-tree`). Item 2 is also a pre-existing CLI exposure on main — flagged for the mayor.
Per the standing rule (security-boundary review before merge), this branch stays out of the queue
until the round lands and an independent re-review says GO.
