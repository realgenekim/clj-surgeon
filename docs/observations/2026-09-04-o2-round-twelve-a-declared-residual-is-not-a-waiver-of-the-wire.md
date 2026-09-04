# O2 round twelve — a declared residual is not a waiver of the wire

Written 2026-09-04 19:45 UTC by forge-anvil (lane O2, branch `bridge/study-ops-mcp`).
Answering Sol's O2 round-eleven review, `docs/observations/study-ops-o2-round11-review-sol.md` (NO-GO).

## What the round was

Two items. **Item 2 (BLOCKING)**: round eleven declared the `:a`/`"a"` and
`nil`/`""` pointer collision a safe RESIDUAL, on the ground that
`structuredContent` publishes both keys as the same JSON object key. **Item 11
(LANDING)**: absorb current `origin/MCP/main`. Item 6 — Gene's product
acceptance of the read-side text growth (MCP-OP-STUDY-051) — is not this lane's
and was not touched.

## Item 2 — the choice, and why

The reviewer named two acceptable answers: **(a)** refuse a colliding receipt
at the public boundary, or **(b)** give the second key a wire-surviving
disambiguated spelling in both faces. **(a) shipped.**

The argument for (b) is real: it keeps the pointer injective without refusing
anything. It pays for that with the one property this lane rests on — that
`structuredContent` IS the receipt and `content[0].text` is a rendering of
it. Under (b) the published object stops being the object the kernel holds, a
caller can no longer read a fact back out by the key it asked with, a literal
key spelled like the escape collides with it, and every consumer's key set
changes for a case none of them can hit. It makes the STRUCTURED face a
rendering, which is the defect class rounds six and seven were about.

(a) is also the honest answer rather than the cheap one. The object would carry
duplicate member names; RFC 8259 leaves that to the decoder and ordinary
decoders keep ONE, so the caller cannot address both keys however the text is
rendered. A `dropped:` line about a leaf no decoder will surface is a
declaration about something that does not exist on the wire.

**The invariant is stated about the WIRE, not the pointer**, and that is wider
than the reviewer's pair: `0` and `"0"` spell the DISTINCT pointers `[0]`
and `0` (MCP-OP-STUDY-052) and still publish as the one member `"0"`; and
`:a-b` and `:a_b` are made to collide by this namespace's own `json-key`
normalization. All three are one event — a lossy object — and the shared
pointer is one of its symptoms.

## What the witnesses caught that the fix did not anticipate

Three defects, two of them in the fix itself and one already shipped.

1. **`if-let` on a `nil` prior key.** The first colliding pair the walk had
   to find is `nil` against `""`, and a prior key of `nil` is FALSY, so the
   first fix reported no collision for exactly the pair the reviewer named:
   96 failures fell to 17, not to 0. `contains?` now.

2. **A bound on two of three caller-shaped fields is not a bound.** The keys
   were bounded and the colliding MEMBER NAME was not, so a 40,000-character
   key built a `full` refusal that could not fit the budget — and the
   fallback rung it fell to THREW, because `(stamp-envelope … {})` gives a
   substitute no clock and `format-elapsed-ms` refuses a nil elapsed time.
   **`public-budget-refusal` carries the same unclocked rung**, so its last
   rung could never have been taken either; both now carry the measured
   result's envelope.

3. **The gate's first act in the full suite was to refuse a receipt this tree
   ships.** `open-basis-sites!` assoc'd the internal `:workspace-root` onto
   the published `basis-view` result while the tool attaches the wire
   `:workspace_root`; `json-key` collapses the two, so every ordinary
   `basis-view` call published `workspace_root` twice and lost one of the
   values. **This falsifies a claim made in the GREEN commit** — that a
   colliding receipt is "unreachable in ordinary operation." It was reachable
   on every call, and no witness could see it because both spellings are
   excluded from the TEXT by the same normalized path. The correction is
   recorded in the commit that fixes it rather than left to be rediscovered.

## Item 11 — the merge, and the half that would have been dropped silently

Trunk `cc9544c4`. Two conflicts.

`core.clj`: the trunk GREW the 1,804-line region this branch DELETED when
round six moved the `:ls-tree` discovery kernel into `clj-surgeon.study`.
Of the 37 top-level forms in the trunk's block, 20 are that kernel and 17 are
the new relation census. The census is taken whole; the kernel is not
duplicated back, because the census references none of it — verified by
enumeration, not by reading.

**The part a take-the-census-and-move-on resolution would have lost:** all four
of `MCP-OP-SHELL-ARGV-004`, `-005`, `-006` and `-007` had their ONLY
`src` witnesses inside the deleted block. Taking the census alone would have
removed four `[x]` specs' implementations from the tree — a reader-eval fence,
a nesting ceiling and a `:paths` escape fence — and left the intent audit to
notice. They are ported into `clj-surgeon.study`, where the kernel lives,
exactly as round six ported the trunk's earlier `:ls-tree` controls. One of
them found a live divergence: this branch read `project.clj` with
`clojure.core/read-string` inside `(binding [*read-eval* false] …)`, which
MCP-OP-SHELL-ARGV-004 rules out in as many words — "as data" means a reader for
which `*read-eval*` is not CONSULTED, not one called with it bound false.

The trunk's own behavioural witnesses came with the merge and drive the ported
controls through the REAL launchers, which is the right way round: the port is
proved by the tests that were written against the code it replaced.

## Learnings, as ratchets

- **A residual is a claim, and a claim about one key at a time is not a claim
  about a map.** Round eleven's residual was true of `{:a 1}` and of
  `{"a" 1}` and false of `{:a 1, "a" 2}`. When a document says "these two
  are the same thing," ask what happens when both are present.
- **Name the invariant at the layer the loss happens.** Stating it about the
  POINTER would have closed two of the four colliding pairs and left `0`/`"0"`
  and `:a-b`/`:a_b` publishing lossy objects with perfectly distinct
  pointers.
- **A gate's first field catch is worth more than its witnesses.** The
  `basis-view` defect had shipped, had passed every suite, and was invisible
  to the carriage audit by construction. A convention held everywhere except at
  the one seam where an internal name meets a wire name.
- **When a lane MOVES code, the trunk's later fixes to that code are the
  merge's real content.** The conflict markers point at the census; the risk is
  in the twenty functions that merged without a marker at all.
