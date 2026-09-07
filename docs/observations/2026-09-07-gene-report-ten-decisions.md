# Gene report — the ten decisions, 21:58Z → 00:00Z (Gene: "Go on all ten. You choose order." … "Upon conclusion, spin everything down. And issue Gene report.")

## Block ledger
| item | what | result | receipt |
|---|---|---|---|
| 1 inb-7487a2 | pre-list builder doctrine | written into both role skills | skills/fable-overseer, skills/astra-frontier |
| 2 inb-fa0eb7 | drift policy: free reset first, then account | written into the resume note; not triggered (no third drift) | 2026-09-03-resume-here-anvil-seat.md |
| 3 inb-adcc9e | hot verify blocks for its whole timeout | LANDED 0ddf8ec5: 61,166 ms → 965 ms live; interrupted = failure; typed timeout/closed; ceiling rounds up; pin 1381 | fable/hot-verify-done 10f5131d |
| 4 inb-2b33ec + inb-186182 | receipt truth + default profile | LANDED 2b4080fe: verified/unverified texts differ and name checks; failure parity, one budget; lint the only built-in, unconfigured verify refused; pin 1393 | fable/receipt-truth 4f42168b |
| 5 inb-b60d6e | public result ceiling unenforced | LANDED 7030bb56: guard at the publication point, every envelope ≤ 32,768 B (32,767/32,768 pub, 32,769 refused → 684 B), oversized refusals bounded by size not name; Sol fence r3 LAND YES (80-case adversarial matrix); pin 1372 | fable/public-result-ceiling c10a90f6 |
| 6 inb-e02822 | compact counts mode (P2) | HOLD as approved | — |
| 7 inb-a36079 | hash policy | HOLD as approved | — |
| 8 inb-2da8ea | refusal-text last item | LANDED 57c48894: finalize-result changes only elapsed_ms, canonicalisation at the eight construction exits (RESULT-003), rendered next_call replays byte-exactly (escape-non-ascii, pointer fallback); Sol fence r8 LAND YES (273-case sweep); pin 1415 | fable/refusal-text-shape 2e6109cc |
| 9 inb-b3b6d1 | extraction friction | rode item 4's receipt work (next_call submittability remains open) | — |
| 10 inb-4a0f43 | mayor's docs batch | eight docs-only branches queued | 2026-09-03-merge-queue-for-mayor.md |

## Versus native wall
No new pair ran in this block (by design: all four are tool-perfect fixes). The only wall figure is the hot fix's live receipt: the same verified call 61,166 ms on trunk → 965 ms at the tip; it retires an actor's verification turn but is a warm focused test-vars call, not a suite.

## Wins and losses
- Wins: four source fixes landed in two hours with red-first witnesses, nine peer reviews by executed probe (every first tip held with a real defect, every round narrow), two Sol fences on the two gate boundaries; two builders and one reviewer withdrew their own unmeasured figures before they were repeated.
- Losses: three batteries pre-started on first tips and wasted (my choice, disclosed); the pinned corpus count trapped every merge (agreeing numbers auto-merged once, disagreeing twice) and cost a recount each time; a `Thread/sleep`-style pattern kill took my own shell once earlier; landing all four took to 00:00Z, ninety minutes past my 22:30Z estimate.
## Learnings → ratchets
- first tips are held: never pre-start a battery on a first tip; recompute pinned counts per namespace at every merge (memory: agreeing-numbers-merge-silently); the ceiling must be enforced on the envelope actually published, by size not by error name; a verification profile that lints is not a test profile; a renderer that promises sendable JSON must never prose-sanitise it.
## Spin-down
No runtime on either seat; ledger closed; usage watch last line 23:53Z (73 apply / 19 inspect / 9 admit since 04:44Z, experiment traffic); servers 7906/8171 on e8076379 (the four landings are server-side changes — rebuild is the next window's first announced act); tag stable/2026-09-06-strict unchanged (the skiff install note points at it; a new tag after the servers are rebuilt and one real task passes).
