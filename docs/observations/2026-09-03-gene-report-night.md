<!-- refreshed 10:13Z; final refresh ~13:00Z -->
# Gene peek report — Surgeon program, Anvil seat, the night of 2026-09-03 (04:35Z → 10:13Z, ~5h38m in of the 9h order)

Gene, going to sleep: *"I'm going to sleep; exhausted; you work through next 9 hours; you know the
goals; state them. mayor, please watch us every 10m; anvil, ask mayor what help you want from it;
mayor, confirm the help you're going to give."* The two goals, his priority order: (1) get every
ready branch to GO with an executed independent re-review, sitting in the mayor's queue; (2) land
the memory program (his choice B — optimistic transaction, disk-journaled, byte-budgeted heap) as
measurable pieces, "no `-Xmx` by judgement anywhere."

## 1. Headline

**The memory kernel's OOM was reproduced on command, TDD-style, then fixed and measured: at
`-Xmx256m`, 600 files that OOM under the old frozen-read pattern now commit and retain
14.19–14.40 MB (last re-measured 09:42Z: 14.11–14.24 MB), against a frozen `-Xmx2g` reference
that used 2,046.8–2,046.98 MB — a ~145× reduction.** All nine branches named in the night orders
reached GO overnight (kondo, routing-doc — both merged; ratchets; template-upsert; rf2; study-ops
(08:33Z, eighth GO); memory-battery; read-path-memory; q5z-alias-migration (09:37Z, ninth GO —
the branch whose OOM started the whole memory program)), every one sent back at least once by an
executed, independent red-team first. Only two are on main; the mayor has seven more waiting.

*Events to the contrary:* the kernel (B1) is GO-WITH-FIX but still not merge-ready (round 3
re-entered review at 09:42Z, not adopted by any verb yet). A Claude session-limit outage at
~09:55Z (HTTP 429, "resets 10am UTC") killed four in-flight agents mid-flight — the seat's own
Claude quota, not a box failure — all four relaunched after 10:00Z from committed state.

## 2. Wins vs native

No vs-native cohort ran tonight. E3 (fan-out verb) and E6 (study-ops free-choice adoption) were
pre-staged at 04:55Z (712a828, 1,234 lines) but remain blocked on the mayor's merge pass — q5z
and study-ops are both GO but neither has actually merged to main (checked 09:44Z: only kondo and
routing-doc are on origin/main) — not on further fix rounds. Named gap: no fresh wins-vs-native
numbers this cycle; the standing wins (rf2 243 s vs 336 s wall; the q5z anchor 228 s vs 283 s
wall) are unchanged from the 2026-09-02 peek report and were not re-measured tonight.

One number from tonight, explicitly **not** a vs-native comparison (same tool, old pattern vs
fixed pattern) so it belongs here only as a labeled aside: MEM-015 read-path single-parse
(`bridge/read-path-memory`, GO 07:04Z round 2) — 1,000 files, wall 11,784→6,621 ms (−43.8%),
allocated 25.28→14.95 GB (−40.9%), outlines byte-identical over a 160-file differential, 0
mismatches. Receipt: `docs/observations/2026-09-03-mem-015-sol-review.md`.

## 3. Losses vs native

None — no vs-native runs executed tonight, same gate as §2 (E3/E6 blocked on the merge pass).

## 4. Exactly what the win is

The B1 disk-journaled transaction kernel (MEM-006/007/012–014/020) replaced the old frozen-read
buffering pattern with a byte-budgeted heap plus disk journal, so the identical 600-file workload
that OOMed at `-Xmx256m` now commits and retains 14.11–14.24 MB against a 2,046.8–2,046.98 MB
`-Xmx2g` reference, parity byte-identical to the reviewer's own independent run. The boundary: the
kernel is GO-WITH-FIX at round 3 only, not yet adopted by any verb (~15 non-cooperating write
sites tabulated as an adoption obligation), and two of the memory program's five leaves
(parser-admission MEM-005, streaming-ls-tree MEM-003) are still in fix rounds with real,
unresolved findings — a crash on malformed input, an unauthenticated pagination cursor.

## 5. Surprises

- Opus reproduced the builder's exact counter-proof on q5z round 9 (09:37Z) and withdrew its own
  review item after 8 prior rounds — a reviewer reversing itself against live evidence.
- The hourly usage-watch collector held flat at 96 MCP calls / 49 ok / 47 refused for four
  straight hours (last checked 10:03Z: still 96/49/47) while the codex-provider counter moved
  128→457 — it was counting Sol reviewers probing their own branch servers on 7908–7910, not
  agent adoption of the tool (inb-46f90f).
- A Claude session-limit outage (~09:55Z) killed four in-flight agents simultaneously — the Opus
  re-checks of MEM-005 round 3 and the kernel round 3, the arms round-5 builder, and the MEM-003
  round-2 builder — all at once, from one quota edge.
- The branch that started the whole memory program (q5z-alias-migration) needed nine review
  rounds (Sol 1–6, Opus 7–9) before reaching GO.
- The B1 kernel's round-2 finding: rename is not CAS — a concurrent writer between revalidation
  and rename was silently overwritten while the receipt still said `committed=true`.

## 6. Learnings crystallized

- Codex `-c mcp_servers={}` overrides *merge* into a repo's declared MCP table rather than
  replacing it, so a repo-declared production server (7888) can still be dialed. Rule now
  followed: move the scratch clone's `.codex/config.toml` aside before every Sol launch, writing
  a one-server file only when an explicit url is passed. Artifact: `~/bin/sol-yolo`. Commit: not
  captured in tonight's log — named gap. Source: captain's log 06:58Z.
- Never `git add -A`/`.` on a shared checkout — add files by name only. Source: captain's log
  04:52Z (a `git add docs` swept two other agents' in-progress docs into a commit; caught before
  push). Doctrine file: not identified in tonight's log — named gap.
- A log path built from `git rev-parse HEAD` must be computed once into a variable, never
  re-evaluated inside a long-running command — a mid-run commit shifted HEAD and the first "zero
  failures" claim was read from an empty file. Source: captain's log 04:59Z. Doctrine file: not
  identified — named gap.
- Verify a process's cmdline BEFORE killing it, never after only printing it, on a shared box — a
  `pgrep` pattern matched the seat's own shell and nearly self-killed. Source: captain's log
  06:58Z. Doctrine file: not identified — named gap.
- Unit suites now run under `~/bin/suite-run` (one of three parallel lanes) rather than one
  shared `~/tmp/suite.lock`, which had 10 waiters pile up at load 3.6/16; the memory battery and
  memory-red keep the exclusive lock because their wall numbers are trend lines, not gates.
  Artifact: `~/bin/suite-run`. Commit: not captured — named gap. Source: captain's log 06:03Z.

## 7. Best news / worst news

**Best:** the delegated-review loop held under a full night of pressure — every substantive
branch that entered the queue was sent back by an executed, independent red-team at least once,
the worst finds were genuine RCE/TOCTOU-class holes (ripgrep `--pre`, `read-eval` on a scanned
`deps.edn`, a symlink race past a collision guard, an unauthenticated pagination cursor, an
admission ceiling bypassable by construct class), and twice tonight a reviewer's own disagreement
resolved correctly — once Sol conceding its tie-break rule (q5z round 4), once Opus reproducing
the builder's counter-proof and withdrawing (q5z round 9).

**Worst:** only two branches are actually on main (kondo, routing-doc); seven more sit at GO
waiting on the mayor, and the two branches E3/E6 are gated on — q5z and study-ops — are both GO
but *neither* has merged. The memory kernel (B1) is GO-WITH-FIX at round 3 but still not adopted
by any verb, and two of its five leaves (parser-admission MEM-005, streaming-ls-tree MEM-003) are
mid fix-round with real, unresolved findings. A Claude-side outage cost the seat four agents
around 10:00Z — recovered cleanly, but a reminder the seat itself is not the only thing that can
stop without warning.

## 8. Board (Pacific — Gene is PDT, UTC-7; the box is UTC)

- **Running now** (all relaunched after the 09:55Z outage, from committed state): MEM-005
  round-3 re-check (Opus), kernel (B1) round-3 re-check (Opus), arms-apparatus round 5 (Sonnet,
  clean at 895eed0), streaming-ls-tree (MEM-003) round 2 (Opus, from its RED commit 98775cb +
  one dirty file). Also building: census round 8 (Sol round 7's schema-invalid `next_call`
  finding), fold-diff round 7 (Sol round 5's live-store-read findings).
- **Lands next:** the mayor's independent verification + merge of the seven GO branches
  (receipt-ratchets, rf2-extract-rewire, template-upsert, memory-battery, study-ops-mcp,
  q5z-alias-migration, read-path-memory). E3/E6 cohort runs unblock once q5z and study-ops are
  actually on main, not merely GO.
- **When:** as of this refresh it is 03:13 PDT (10:13Z). The 9-hour order ends ~06:34 PDT
  (~13:34Z); the final refresh of this report lands ~06:00 PDT (~13:00Z).

## 9. Decisions waiting on Gene

- **Curtain-call merge order** — fold → store → settings-lens → template-upsert →
  lens-followups. `template-upsert` is GO (round 2, 06:27Z), `lens-followups` is ready,
  `fold-diff-tool`'s production read is GO at 347fe6d3 (08:29Z) with Sol's conditions; the order
  itself still awaits Gene's ruling. Recommend: fold after the mayor's fold-diff production run
  (post round-7 fix), store next, then the lens stack.
- **claude-skills PR #1** (sol-yolo bundled into the codex skill) — ready, the mayor was asked to
  merge or request changes; not yet actioned in tonight's log. Recommend: merge — it is the same
  fix already relied on for every Sol launch tonight (§6, first learning).
- **inb-3a9818** (production ops, fold-diff-checkpoint) — now GO with conditions (moved from
  HOLD to 347fe6d3 at 08:29Z). Sol's condition: retain the full receipt, since the VERDICT line
  alone carries no fallback caveat. Recommend: accept the condition; the production pin does not
  move again until the tip (round 7) earns its own GO.
- **inb-78e75c** (the "two public tools" invariant in the one-compiler plan, before census
  merges) — carried forward from the 2026-09-02 peek report, still open; tonight's log names it
  without adding detail. That report's recommendation stands: amend to "read tools compose
  through inspect; write tools stay gated."
- **inb-041b28** (the announce UI has no unannounce control) — carried forward from the
  2026-09-02 peek report, still open, still a product call; tonight's log names it without adding
  detail.
- Filed for awareness tonight, no decision required yet: inb-1165ce (night orders + both 7888
  boundary incidents relayed to the mayor), inb-46f90f (the usage-watch contamination, §5),
  inb-07c5e7 (rename.clj's own `z/of-string` constructor still ungated — left ungated on
  purpose, a fourth constructor MEM-005 doesn't cover).

## 10. Answers to last peek's questions (from the 2026-09-02 peek report, §10)

- *What model?* Still Fable 5.1 (Claude) for the seat; Opus and Sol (codex) do the reviews —
  five branches routed Opus-first tonight because OpenAI's content filter refuses our own
  symlink/path-confinement fixtures (rf2 ×2, study ×2, MEM-005, q5z, the kernel).
- *What host?* Still Anvil, user forge, 16 cores.
- *Crank up parallelism?* Changed again tonight: the single shared `~/tmp/suite.lock` that had 10
  waiters piled up at load 3.6/16 is now `~/bin/suite-run` (three parallel lanes); the memory
  battery and memory-red keep the exclusive lock on purpose (§6).
- *Friction ledger?* Still ratified practice — five more ratchets built tonight (§6), each with
  the exact text, a rule, and a trigger.
- *On deck / exploring / option value?* Still the tree-level requirers op (E3/E6), still gated on
  q5z — but the gate changed shape: q5z reached GO at 09:37Z, so the real remaining blocker is
  the mayor's merge to main, not further fix rounds.
- *Prosecution list in a trusted place?* Still the twelve inbox items from 2026-09-02, now joined
  by three more filed tonight (inb-1165ce, inb-46f90f, inb-07c5e7, listed in §9).
