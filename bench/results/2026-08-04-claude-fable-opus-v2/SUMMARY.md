# Bounded Claude skill validation

All four clean-context children passed their independent correctness and
workflow gates. The matrix ran concurrently with a separate 90-second GNU
`timeout` boundary, raw stream, workspace, state receipt, and terminal receipt
for every child.

| Task | Requested | Resolved | Wall | Turns | Tool calls | Cost | Exact | One-shot |
|---|---|---|---:|---:|---:|---:|---|---|
| `ops-registry-xray` | Fable | `claude-fable-5` | 36.971s | 4 | 2 | $0.878034 | yes | yes: one Skill + one X-ray |
| `pair-view-edit` | Fable | `claude-fable-5` | 25.677s | 6 | 4 | $0.932874 | yes | yes: first plan applied separately |
| `ops-registry-xray` | Opus | `claude-opus-5` | 41.274s | 8 | 6 | $0.554476 | yes | no: five X-ray calls |
| `pair-view-edit` | Opus | `claude-opus-5` | 26.220s | 6 | 4 | $0.4650995 | yes | no: first plan was rejected, then repaired |

Total reported model cost was $2.8304835. A result can be exact without being a
one-shot; the receipts preserve that distinction instead of promoting repair
behavior into a stronger claim.

## Frozen artifacts

- Source commit: `477dca94120c74be378d84f836dbc7dc19a84783`
- Installed `SKILL.md` SHA-256:
  `2d9c5421480f047a66349d0af2cef399730ab9da5fb9d3d45b1ddea31d251775`
- Installed skill package source SHA-256:
  `23156b3dc2661bbf611c2a194dcfb35dc6e314bcdbcf23a999eae9411c25883f`
- Immutable CLI package source SHA-256:
  `3e5b6ca0fcdf5fa1e5b433cc5d54140916fe7b38e5e4c42b073534430fca88a4`
- Real `ops-registry` source SHA-256 before and after both reads:
  `c88bb6be6473730d7068b23b4fe2af27e564f0984cbfd7809cb8c5c0d6748562`
- `pair_view.clj` start SHA-256:
  `3d24b439a00b8b5410011cf4ae565c13fa9f2ea9ff7dc6be4050d020ab71cd6e`
- Exact expected and observed edit SHA-256:
  `05012ad4f42ebbbab074e01be60081caa9894c4b5f993c2e4ec86e91febb5c9a`
- Fable applied-plan SHA-256:
  `7a388fdad5aaadca26a6012dbe9602d87b6944ae18abf799de3f6a075c345a94`
- Opus applied-plan SHA-256:
  `e84c8012cd24624715a5af1c1588e50ceaf891deffe106a42b46d69ba1e329b8`

Both edit diffs contain one replacement, `:done` to `:complete`; the attached
comment and all unrelated bytes are identical. Both applied plans bind the same
start and result hashes. Prompts contain the user goal and output schema, not
the independently scored X-ray answer.

## Harness isolation proof

The fake-child self-test finished in about 1.02 seconds. Its fast child emitted
a success receipt, its failing child emitted a failure receipt, and both raw
outputs remained present while a third child stalled for ten seconds and was
terminated at its own one-second deadline. The stalled child's partial output
also survived. No model service is called by this self-test.

Every child directory retains the exact prompt, copied skill and receipt,
requested and resolved model, raw JSONL, extracted result, final answer, usage,
tool calls, start/final hashes, source diff, score, and atomic terminal receipt.
`MANIFEST.sha256` covers the durable evidence tree.
