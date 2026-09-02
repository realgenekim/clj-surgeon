# The Gene peek report — standard shape for an automated hill-climb

Purpose: when Gene peeks in on an automated hill-climb (Anvil cohorts, tweezer sessions,
builder branches), one page answers the questions he actually asks, in the order he asks them,
with the number first and the caveat in the same breath. Derived from his questions on
2026-09-02: "surprises and learnings and wins?", "best and worst news?", "what allowed us to
beat native?", "exactly what was the nature of the win?", "1.3 sec vs __ native", "any good
news?", "no events to the contrary yet, right?", "we are tweezering?", "why are we running on
Anvil?", "learnings and wins", "is X in the MCP kernel now?".

Rules: count-first; headroom never consumption; every ratio names its n and its instrument;
"vs native" means the same task, same prompt hygiene, same base; a claim without a receipt path
is not in the report; Pacific time for Gene; no invented terms of art.

## Sections, in order

1. **Headline** — one sentence: the biggest true number and what it is a number of.
   Then one line: *Events to the contrary:* none / these.
2. **Wins vs native** — table: task · native · tool · ratio (returns, wall) · correctness · n · receipt.
3. **Losses vs native** — same table.
4. **Exactly what the win is** — two sentences: the mechanism, and the boundary where it stops.
5. **Surprises** — up to five, one line each, with the number that surprised.
6. **Learnings crystallized** — up to five, each stated as a rule we now follow, and what changed
   in doctrine (file + commit).
7. **Best news / worst news** — one each.
8. **Board** — what is running now, what lands next, when (Pacific).
9. **Decisions waiting on Gene** — each with its inbox id and a one-line recommendation.
10. **Answers to last peek's questions** — his questions from the previous peek, one line each,
    so continuity is visible.

Generate an instance under `docs/observations/<date>-gene-peek-report.md`, commit it, and file
its pointer to Gene's inbox with `maven-w inbox add` (the writer-surface rule: a draft to prune,
never a blank page).
