# O2 / PF-5 — one `ls-tree` call, sufficient in the TEXT block

*Anvil seat, `bridge/study-ops-mcp` worktree `/home/forge/src/clj-surgeon-study`, base
`4480e3d`. Zero arm-runs; bench work only, hand-driven in process. Sources read:
`docs/observations/2026-09-04-e6-lb-cohort.md` (the PF-4 finding), `/home/forge/tmp/sol/e6poll-opus.md`
(O2's pass line), `/home/forge/tmp/arms/e6/pf4/c2.json` (the 6-of-10 receipt), and
`docs/intent/study-ops/study-ops-specs.md`.*

Written 2026-09-03 23:18 UTC.

## What was asked

Make one `inspect_clojure mode="ls-tree"` call sufficient for a small tree, in the TEXT
content. Two defects, named by the poll; one apparatus ratchet.

## What was measured, before anything was changed

Reproduced the field receipt exactly against `/home/forge/tmp/arms/e6/pf3`:

| call | `read_complete` | files | `tree` chars | TEXT chars |
|---|---|---|---|---|
| real `src`, grep, limit 4096 (the old default) | false | 2 of 10 | 1,914 | **146** |
| real `src`, grep, limit 16384 (the ceiling) | false | 6 of 10 | 14,956 | **146** |
| toy 10-file / 30-form tree, grep, default | true | 10 of 10 | 2,064 | **113** |

The complete text rendering of those ten grep-matched files is **24,940 characters** — above
the 16,384 ceiling by half again. So no raise of the limit alone can make that particular tree
complete in one call; it is a tree a caller must narrow. The toy tree was already complete at
the receipt level and still returned **zero rows** to a text-only client.

## Defect 1 — the rows never reached the text block

`ls-tree-summary` built a header, a status line, and an arrow, and discarded the payload. It
now renders the bounded payload between them — the tree verbatim for `format=text`, the
compact JSON rows for `names` and `edn` — and, when the receipt is truncated, spells the
continuation in the text (`next call: inspect_clojure mode=ls-tree dir=src
grep=System/currentTimeMillis limit=16384`) or, at the ceiling where no continuation can
advance, the remedy. The rows are exactly the bounded payload, so the text cannot disagree
with the receipt and is bounded by the same `limit`.

| receipt | TEXT before | TEXT after |
|---|---|---|
| toy 10-file / 30-form tree, grep, default limit | 113 | **2,178** |
| toy tree, `format=names` (the no-grep default) | 113 | **880** |
| real E6 `src`, grep, default limit | 146 | **4,545** |

## Defect 2 — the default bound admitted almost nothing in the format grep selects

4096 was calibrated on the atomic study operations. `format=text` is what `ls-tree` selects
once `grep` is present, and at 4096 the real ten-file `src` returned **2 of 10**.
`ls-tree-default-limit` is now **8192**: a twenty-five-file toy tree that returned 19 of 25
returns 25 of 25, the real `src` returns 3 of 10 rather than 2, and a ten-file / thirty-form
tree is complete in one call inside 8 KB of text.

The ceiling stays 16,384 and stays a typed boundary, witnessed at the edge rather than
asserted about the constant: **77 toy files render in 16,370 characters and fit; the
seventy-eighth truncates to 76** with `narrow_scope` and the remedy in the text.

## The ratchet — proving the connection per arm

Confirmed the poll's caveat rather than assuming it: no per-arm-identifiable session event
existed. A server emits `server.start` once per PROCESS carrying the SERVER's run id, and
`tool.call` per call carrying the same one. When one server serves several arms — the shape
every free-choice cohort runs — the record cannot tell which client called, so a silent
connection failure and a deliberate decline are the same bytes.

`telemetry/record-session-start!` now emits `session.start` the first time a server serves
each distinct workspace root, and never again for that root, so its count is a count of
sessions rather than of calls. It carries a content-free `workspace_key` (the first sixteen
hex characters of the root's SHA-256, which a cohort recomputes from the worktree path it
launched the arm in) in every mode, the root itself only in `:full`, and a `client_run_id`
when a caller supplies one. Both public entrances announce their workspace.

## Honest scope — what this does NOT establish

- **O2's pass line as literally written against the E6 worktree is not met, and cannot be by
  a bound change.** Its `>= 22 hit line numbers across >= 10 files, read_complete=true,
  <= 8 KB` was scored against `marvin-voice-remote/src`, whose complete `format=text`
  rendering is 24,940 characters. The pass line is met on the ten-file / thirty-form toy tree
  this brief re-scoped it to. What the real tree gets instead is a text block that carries
  three real files of rows and names its own continuation, where it carried a header.
- **`format=names` is the sufficient shape for that real tree**, and now says so in text: all
  25 files of `pf3/src` complete at the default in 3,065 characters of TEXT.
- **This cannot move adoption by itself.** A fresh session cannot know the payload changed.
  Scoring a re-run of E6-Lb against it would read as a fourteenth null.
- **Nothing was run against a live server.** All witnesses are in process; 7910 was ceded to
  a routing cohort mid-task and no port was bound.

## Gates

- `suite-run bb test/run_all.clj` — 731 tests, 6,023 assertions, 0 failures, 0 errors
- `suite-run clojure -M:clj-surgeon/mcp-test` — 437 tests, 5,634 assertions, 0 failures, 0 errors
- `make mcp-operation-oracle` — pass
- study-ops CLI goldens (`cli-ls-tree-bytes-match-the-frozen-golden` and its edn, refusal and
  prune-target siblings, inside `bb test/run_all.clj`) — green, and all four golden files are
  byte-identical: the CLI renders through `study/format-ls-tree-text`, which was not touched.
- `mcp-intent-contract/audit-current-repository` — ok, 0 violations. It caught two untagged
  intents mid-task and is the reason `MCP-OP-STUDY-038` and `-039` carry implementation
  witnesses.

Specs: `MCP-OP-STUDY-007` amended (the default is per-mode now); `-036`, `-037`, `-038`,
`-039` added with falsifier rows.
