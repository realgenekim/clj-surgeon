# Three-way acid battery preregistration: caller wall or it did not happen

Frozen on 2026-08-31 before any battery model episode.

## Question and headline law

Does the current installed clj-surgeon surface reduce the complete time a real
caller experiences, compared with the exact pre-install-night product and a
matched native route?

There is one headline metric: **complete caller-turn wall from prompt dispatch
(the intent is stated) through process completion after a verified mutation
receipt**. Direct server time is never a headline or a substitute. Every row
also decomposes that same wall into:

1. caller emission: wall outside recorded action intervals, including model
   inference, request emission, receipt interpretation, and final response;
2. transport or local-tool wall: client-observed action intervals less measured
   product-server time; native local tool execution lives here; and
3. product server: sum of `tool.call.timings_ms.total_ms`, zero for native.

The three components must sum to complete wall with zero residual after
rounding. This definition deliberately prevents a millisecond server slice
from being presented as the outcome of a roughly minute-long caller turn.

## Exact arms

| Arm | Exact surface | Caller route |
|---|---|---|
| A — latest | product `05f5a1962e5a0c5aa0365c673994eca9024c1a44`, tree `7cb0f58bdc4d8469d1f7757b0f0ee65e61f4fdc1`; canonical published receipt head `469141bdd3144a94a4e4ea2ed99c7ecd6ca26f5b`, tree `5048e545ab63a74d922aab0b3297618232df1073` | terse exact reads and same-session `confirm+fill` without preview when eligible; direct prepared extraction otherwise |
| B — two days ago | product `19ab864889799b0028a5f7cb66c63b957ff7b973`, tree `72e1ef5fc09587013e7b8d60f2ed027385280973` | old explicit read and ordinary complete mutation contract |
| C — native | no clj-surgeon product | bounded shell read, `apply_patch` or shell-native edit, and exact shell verification |

Origin was read after fetch: `release/closed-relations-published` resolves to
`05f5a196`, and `release/prepared-confirm-preview-published-20260831` resolves
to `469141bd`. `9af88fba` is an ancestor of A. B diverges after merge base
`543798a` and is not approximated by a local revert.

Caller is exactly Codex `gpt-5.6-sol`, reasoning `high`, subscription auth,
with `OPENAI_API_KEY` removed. The cohort runs serially on Anvil `dev-a`.
Every episode receives a fresh Codex home, workspace, private 512 MiB product
server where applicable, OS-assigned port, and one stdio-to-HTTP proxy that
captures the initialization `Mcp-Session-Id` and reuses it for every upstream
call. This makes A eligible for W1. Per-call-session callers are explicitly
ineligible because audit `c8b07168` proves that their confirmation lookup is
`prepared-confirmation-unknown` even after rereading.

## Frozen task classes

1. **THE FILL.** Real-task-derived nine-line `ide-shell` owner. The decision is
   two literal changes; the whole replacement is about ten lines. A reads the
   owner tersely and emits only the replacement hole under `confirm+fill`. B
   emits both complete old and new owners. C reads, patches, and hashes.
2. **THE WALL.** The benchmark-field-failure `route-event` owner is 30 lines.
   The semantic decision changes one status value, while the required guarded
   replacement is the complete owner. This is the registered
   `|replacement| >> |decision|` class. A uses same-session `confirm+fill`
   without preview; B repeats the complete old and new forms; C patches the
   single line after one bounded read.
3. **THE REPAIR EPISODE.** Every arm intentionally makes one wrong first
   mutation on the correct file and correct owner. A and B submit the same
   exact old/new owners with deliberately wrong `matches=2`, then recover from
   `expect-count-mismatch`; A has installed refusal family 001, while B omits
   its resolved source-free rows and per-form evidence. C first patches an
   absent `:finished` line, then reads and repairs. A near-miss owner is not
   used because that would not isolate the installed refusal treatment.
4. **THE FLAGSHIP RERUN.** The historical 15-form Sessionize extraction: two
   files, a 4,594-line source, moved comments/dependencies, visibility change,
   and 24 remaining caller owners in this frozen capsule. A and B get one
   direct exact verified extraction transaction; C gets only native tools.
   Fresh C is measured at n=3. The README's historic 207.898 s matched native
   pair and the promoted 122.278 s native median are retained as external
   anchors, never substituted for these new rows or used in their confidence
   intervals.

The same external exact-tree scorer admits all arms. Wrong-subject means any
explicit source file outside the capsule's declared file set or any final
unexpected byte. A refusal or wrong count on the correct subject is not
wrong-subject. Required wrong-subject total is **0**.

## Fixed schedule and denominator

The frozen schedule has 42 starts:

- FILL: 3 matched three-arm pairs, 9 starts;
- WALL: 3 matched three-arm pairs, 9 starts;
- REPAIR: 5 matched three-arm pairs, 15 starts; and
- FLAGSHIP: 3 matched three-arm pairs, 9 starts.

Within each pair the three arms rotate across first, second, and third
position. Classes are interleaved in pair waves. The exact 42 rows are in
`bench/threeway_acid_schedule.tsv`; SHA-256
`c6d6d54843ed372673ad290811324eb1863b3b1c305b63d614aa27b61d1f2086`.
There is no adaptive stopping, attractive-row retry, dropped timeout, or
replacement episode. A prelaunch refusal before Codex starts may be repaired;
after a process starts, its row remains in the denominator.

## Registered predictions

These are magnitude predictions, not desired findings:

| Class | Registered caller-wall prediction | Registered mechanism |
|---|---|---|
| FILL | A/B median paired speedup 1.10–1.50×; C may tie or beat A | avoiding repetition of the old owner helps, but the fixed model floor dominates a nine-line form |
| WALL | A/B 1.30–2.00×; A/C 0.8–1.3× | A avoids re-emitting the 30-line old owner; native can still express a tiny line patch |
| REPAIR | A/B at least 1.50× and A/C at least 1.50× | A's complete refusal should delete a model-sized rediscovery/retry episode |
| FLAGSHIP | A is no more than 20% slower than B and is at least 4× faster than fresh C | both Surgeon surfaces already compile the supplied extraction; native pays reconstruction and verification |

Secondary predictions: A emits at least 20% fewer MCP argument bytes than B
in FILL and at least 30% fewer in WALL; A REPAIR uses no recovery read in at
least 4/5 while B needs a read in at least 4/5; all 42 outcomes are exact and
wrong-subject is 0/42.

## Kill, invalidation, and interpretation criteria

- Any product SHA/tree mismatch, model substitution, non-high effort, API-key
  auth, shared port, missing held-session proof for A, dirty frozen harness, or
  scorer/hash drift stops the cohort before the next model.
- Any wrong-subject event kills the safety claim and the affected class's
  performance claim. Every row remains reported.
- A class with fewer than all preregistered rows or fewer than 100% exact
  outcomes receives no speed headline. It is reported as an invalid or failed
  class, including all walls.
- FILL kills the claim that confirm+fill improves caller wall if A/B is below
  1.10×. WALL kills that claim below 1.20×. REPAIR kills the dramatic causal
  claim if either A/B or A/C is below 1.30×. FLAGSHIP kills the no-regression
  claim if A is over 20% slower than B and kills the large-class claim below
  4× versus fresh C.
- Server time may explain a result but can never rescue a caller-wall kill.
- Preview is omitted by design because retained n=8 evidence found that its
  extra caller boundary outweighed its emission saving on this edit shape.
  This battery therefore estimates confirm+fill, not preview value.

## Small-n statistics and headline form

For each class and arm, report n, exact count, complete-wall median, raw range,
all raw walls, median split, argument bytes, output tokens, actions, and
refusals. Speedup is reported both as the median within-pair ratio and the
ratio of arm medians. The 95% interval is a paired Student-t interval on log
wall ratios, exponentiated back to speedup. At n=3 or n=5 this interval is
necessarily wide and assumption-dependent; raw ratios remain beside it. No
`p<0.05`, equivalence, or population-resolution claim will be made.

The quoteable report table has one row per class and columns:

```text
class | A caller wall [split] | B caller wall [split] | C caller wall [split]
      | B/A speedup (95% CI) | C/A speedup (95% CI) | exact / wrong subject
```

## Frozen artifacts and token-free gate

| Artifact | SHA-256 |
|---|---|
| runner | `23bea58770c209cda22c92b940a739eee022ded42d832098a365507ffa845ca0` |
| scorer | `845e9a15b1f702c6fa93bed5b28c116ad8d6f89beae1f82c7e287458e0993ba4` |
| held-session proxy | `f9f79435dc0ed1c4bb795208910314b061db080c7ae94c8b784ec6be6185bf00` |
| FILL before / after | `0216d80132683a30afe056aa29598c45443683be22e644f48a9f6375499a60c7` / `2584dd6c4c17e8e46ad3f3d80a9d63e9577d26d840d262a8560516b78c0f7876` |
| WALL/REPAIR before / after | `56a156f76bad4330ab79d6671462893374749277d1c56422e87ef19748fe81ca` / `ac1d08366599cce00e7c6fe2440e43e83aeb8af647018bfca31f89231faf5d32` |
| FLAGSHIP capsule / task | `593ba31ef93a6acff04907583cbbbe8ff286108193f75b16b26304339cf38e5c` / `f1e9e3353a4ce89c57bbd143c46e8a66db5501868e74287f7890f5e658772b09` |

The local token-free preflight started fresh A and B servers on private ports,
initialized the proxy, captured and reused a nonempty session ID, and observed
exactly `inspect_clojure`, `edit_clojure`, and `apply_clojure_changes` through
both the proxy and the real Codex app-server registry. The A and B registry
receipt SHA-256 values are `17540407199dcc9c5105bc14c80e8067d20cc4a3d49e00dd77c908348d4991da`
and `66acbe89c88735a5f63f64a214440726223069a90ed8da8e312358a39355241f`.
It reported `model_calls=0`. Preflight receipt SHA-256:
`a1bac526ee8fd1a490f4920e7d2c21c00e2b85479d44c8b4298cca969496aaf0`.
The exact frozen commit must repeat this gate on dev-a before launch.

Delivery uses branch `bench/threeway-acid-20260831`, author
`sol <sol@skiff>`, and trailer
`Co-Authored-By: Gene Kim <genek@itrevolution.com>`.
