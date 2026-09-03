# bridge/receipt-ratchets ece8c1c — executed review: GO-WITH-FIX (2026-09-03T03:06Z)

Clean: `expect_matched.match` is the same structural matcher as inspect's `match` (no backtracking; output
capped at 20); `file` confined three times (contract, `resolve-source-path`, membership in the transaction's
original sources); `file_hash` cannot be omitted; the refusal fires before any write on every route (extraction
and editor routes refuse at contract validation; kernel clause above `authorize-effects`); no client breaks;
inventory witnesses cover the new property.

| # | sev | finding (proved) | fix |
|---|---|---|---|
| 1 | high | `spans-intersect?` is LINE-granular: two sites on one line, one edited → "all 2 matched sites addressed". Wrong failure direction. `:address {:preorder}`/`:end-preorder` exist and are unused. | intersect on preorder spans; witness: two sites one line, one edited → `unaddressed_match_count 1` |
| 2 | high | dispatch vocabulary has a count bound (40) but no CHARACTER bound: 60 long dispatches → 6,738 B of evidence, and a `;;` comment inside a dispatch vector appears verbatim; `str/join ", "` mangles multi-line entries | apply the sibling `available-owner-character-limit` pattern with a truncated flag; strip comments/newlines; witness: ≤ budget, no `;;`/`\n` |
| 3 | med | the new `invalid-require-policy` refusal is UNREACHABLE from `apply_clojure_changes` (contract refuses `invalid-enum`/`missing-fields` first; `:accepted` not in the normalize allowlist); FIELD-002 `[x]` holds only for inspect | either surface `field`+`accepted`+"never defaulted" on the apply route or re-scope the spec |
| 4 | med | `extract-dispatch` emits `#_skipped` and `^:meta :withmeta` spellings; the selector throws on both while scanning | skip `#_`; strip/handle metadata; witness both fixtures |
| 5 | med | published schema advertises `expect_matched` on the extraction/edits branches that refuse it; `edit_clojure`'s schema declares neither `changes` nor `expect_matched` but the handler accepts them | `:not` clauses for the non-direct branches; witness the schema rejects `{edits, expect_matched}` |
| 6 | low | `wildcard-pattern?` decides from raw text (`"a _ b"` inside a string → spurious note; `[a,_]` missed) | decide from the parsed node |
| 7 | low | no witness pins `minimal-request-examples` to the live validators; count-too-low and invalid-pattern routes lack end-to-end bytes-unchanged witnesses | add them |
| 8 | cosmetic | "1 defmethod arms"; `count 0` prints "all 0 matched sites addressed"; shape computed twice; a file-caused match failure labelled `expect-matched-invalid-pattern`; `resolve-expect-matched` reads a `:path-facts` no route produces | one-liners |
