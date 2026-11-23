# q5z re-review at 23ea871 — GO-WITH-FIX; items 1–3 before the queue (2026-09-03T03:58Z)

All seven prior items CLOSED or PARTIAL on their probes (cycle 14 ms; escape never entered; 3000 → typed
refusal; 3.6 MB → typed, bytes not chars, before slurp; symlinked defining file refused before any write,
0 receipts; stubbed non-commit → `:ok false`, a partial write can never publish `:ok true`; retire-failure
receipt deleted; `retired_to` relative; retention 20). The over-declare idiom is restored on both entrances
and the scan is bounded before the mismatch is computed. Oracle pass.

| # | sev | hole (witnessed) | fix |
|---|---|---|---|
| 1 | block | the two ceilings do not bound their product: 450 files × 1.9 MB (both under limit) → OutOfMemoryError at 512 MB, untyped throw (no `try` in the handler) | aggregate `max-scope-bytes` accumulator |
| 2 | block | depth bound 64 TRUNCATES silently (65-segment file vanishes; the restored over-declare idiom then launders it: the caller re-sends `found_files` and commits with the deep ns's require left on the retired lib) | typed refusal naming the depth |
| 3 | block | `visitFileFailed → CONTINUE`: `chmod 000 src/locked` removes files from scope silently | count and refuse (or name in the receipt) |
| 4 | fast | the raw walk is unbounded; the ceiling bounds the filtered set (60k non-source files walked and materialised) | bound the walk |
| 5 | fast | ceiling and scope-path refusals carry `next_call nil`; `excluding-call` already builds the executable shape; one symlinked file out of root refuses the whole scope with no remedy | reuse `excluding-call` |
| 6 | fast | retention prunes PEERS' published `details_path` (40 concurrent → 20 deleted) | protect peers or document best-effort |
| 7 | fast | hygiene gate root-anchored (`git add -f sub/.cpcache/x` passes) and the test degrades to `(is true)` outside a repo | any depth; fail when git unusable |
| 8 | main | pre-existing: `mcp_change_buffer_test.clj:686` asserts `/opt/homebrew/bin/clj-kondo` — the one baseline failure on every Linux run | fix on main (resolve the binary, not the path) |
