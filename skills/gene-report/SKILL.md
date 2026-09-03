---
name: gene-report
description: "Trigger: 'Gene report' (also 'peek report', 'standard Gene report'). Generate the standard one-page report Gene reads when he peeks in on an automated hill-climb: headline + events to the contrary, wins vs native, losses vs native, exactly what the win is, surprises, learnings crystallized, best/worst news, board in Pacific time, decisions waiting on Gene with inbox ids, answers to his last questions. Count-first, receipts named, no invented terms."
user-invocable: true
---

# Gene report

Gene's words (2026-09-02): *"Create standard gene report for when I peek in on these automated
hill climbs — wins vs native, learnings, top wins vs losses, etc. surprises. Look at reports and
questions I've asked."* and *"Trigger: Gene report. Skill in surgeon repo."*

## When it fires

On "Gene report", "peek report", or "standard Gene report", or at the end of any automated
hill-climb (a cohort chain finishing, a builder branch landing) when Gene is likely to peek.

## What it produces

One markdown page at `docs/observations/<YYYY-MM-DD>-gene-peek-report.md` in the exact section
order of `docs/gene-peek-report.md` (the template; read it first, every time):

1. Headline, one sentence, biggest true number first; then *Events to the contrary:* none/these.
2. Wins vs native — table: task · native · tool · ratio (returns, wall) · correctness · n · receipt.
3. Losses vs native — same table. Never omit this section; an empty losses table is a claim.
4. Exactly what the win is — mechanism in one sentence, boundary in one sentence.
5. Surprises — up to five, one line each, with the number that surprised.
6. Learnings crystallized — up to five rules we now follow, each with the doctrine file + commit.
7. Best news / worst news — one each.
8. Board — running now, lands next, when, in Pacific time (Gene is Pacific; the box is UTC).
9. Decisions waiting on Gene — each with its inbox id and a one-line recommendation.
10. Answers to last peek's questions — read the transcript or the previous report's §10 and
    answer each of Gene's prior questions in one line.

## Sources, in order (facts come from receipts, never from memory)

- The scorer receipts on Anvil (`~/acid/receipts/<run>-score.md`), the captain's log
  (`docs/observations/<date>-captains-log-*.md`), the tech tree (`docs/tech-tree.md`), the
  watcher files (`docs/observations/<date>-tweezer-session-*-watch.md`), the maven inbox for
  open decisions (`maven-r inbox list`), the mayor's channel for the review queue.
- "vs native" means the same task, same prompt hygiene, same base, or the row says otherwise.

## Rules

- **The four things Gene reads for come first and are never blank (Gene, 2026-09-03: "Gene
  reports require perf improvements vs native, top wins and losses, learnings, and what's
  next"):** §2/§3 vs-native tables, §7 best/worst + a "top wins / top losses" pair, §6 learnings
  each with its ratchet, §8 board as ONE next action per lane. If no vs-native measurement was
  taken in the period, the §2 table's first row is `NONE MEASURED — <why>` and Surgeon-vs-
  Surgeon-before figures go in a separately titled table, never in the vs-native one.

- Count-first; headroom never consumption; every ratio names n and its instrument.
- A claim without a receipt path is not in the report.
- Quote Gene verbatim where his words are the source of a decision.
- Prose to a shell goes through a QUOTED heredoc (`<<'EOF'`); an unquoted one executes
  backticks (this skill's first instance lost a phrase that way).
- After writing: commit, push, and file the pointer to Gene's inbox
  (`maven-w inbox add task "Gene: peek report — <path>" --note "<the headline>"`), so there is
  a draft to prune, never a blank page.

## Example

`docs/observations/2026-09-02-gene-peek-report.md` — the first instance.
