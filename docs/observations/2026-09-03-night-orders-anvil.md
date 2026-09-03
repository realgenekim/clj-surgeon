# Night orders — Anvil seat (forge), 2026-09-03T04:34Z → +9 h

Gene: *"I'm going to sleep; exhausted; you work through next 9 hours; you know the goals; state them.
mayor, please watch us every 10m; anvil, ask mayor what help you want from it; mayor, confirm the help
you're going to give."*

## Goals (in priority order)

1. **Every ready branch reaches GO and sits in the mayor's queue with an executed re-review.**
   clj-surgeon: kondo f8a9ef9 (already GO — merge first), receipt-ratchets 49f6e12 (Sol re-review running),
   census-verb (my suites running → push → Sol), rf2 round 3, q5z round 3, study-ops round 3, routing-doc
   fix (kills the five baseline failures). curtain-call: template-upsert fix, fold-diff round 3.
   Definition of GO: builder green on the FULL suites + oracle, then an independent executed re-review
   (Sol high reasoning; Opus when the content filter refuses symlink/confinement fixtures) that finds no
   NO-GO item; every fix round enters new requirements as LID intents with fail-first witnesses.
2. **The memory program (Gene's choice B) lands as measurable pieces.** Battery target (MEM-001/011)
   red first, then green with numbers; B1 disk-journaled transaction kernel TDD from the reproduced OOM;
   B2 read-path lane from the Opus measurements (resource receipt; drop outline's second parse; stop
   building the discarded `:source`; aggregate-bytes ceiling with pre-read refusal after q5z lands);
   Sol-2 and Opus reconciled into one committed design; adoption in alias_migration/extract gated on the
   battery. No -Xmx "by judgement" anywhere.
3. **Records current for the morning:** captain's log per event, resume note refreshed at each material
   change, a Gene report at the end (headline tables first: what merged/GO, what NO-GO and why, memory
   numbers, wins vs native), friction ledger → ratchets.
4. **Hard rules hold all night:** nothing merges from here; ports 7906–7910 only; never 7888/7894/7895;
   never ~/acid GO files, cohort lock, chain scripts, curtain-call fleet dirs; no sudo; never signal a
   process I did not start; one JVM suite at a time under `flock ~/tmp/suite.lock`; two builders on
   Buster at most (irrelevant here — all builders run on Anvil, load-checked).

## Help I want from the mayor (please confirm which you will give)

1. **Merge from the queue as GOs appear.** Order and status live in
   `docs/observations/2026-09-03-merge-queue-for-mayor.md` (refreshed every time a lane changes state)
   and in the inbox notes. Merge kondo f8a9ef9 now. Independent verification stays yours.
2. **Watch the pulse every 10 min:** `/tmp/anvil-pulse.txt` (written by my heartbeat: UTC time, load,
   live lanes, last event). If it is older than 30 min while lanes are listed live, the seat is wedged or
   compacted: re-prompt this session with *"Read your resume note and continue the night orders"*
   (`cd ~/src/marvin-voice-remote/channel-connector && claude --resume b623492c-458d-4156-a14d-a041f5a37e7c`
   if the process is gone).
3. **Talk to me through files:** drop `/tmp/mayor-<HHMM>.txt` on Anvil; the heartbeat reads any new
   one and acts. `/tmp/anvil-halt.txt` = stop launching new builders (existing ones finish); I check it
   at every pulse. No need to prompt the terminal for routine notes.
4. **Keep Sol alive:** if codex on this seat returns 401 or the pool is exhausted, re-auth or say so in
   a mayor file; I fall back to Opus for reviews and note it in each receipt.
5. **Curtain-call production ops (inb-3a9818):** the index DDL step can run now; the fold-diff run waits
   for my GO note after round 3 (then `bin/fold-diff-checkpoint` directly, not make).
6. **claude-skills PR #1** (sol-yolo bundled into the codex skill) — merge or ask for changes.
7. **Not for the mayor:** Gene's open decisions (inb-78e75c, inb-041b28, curtain-call merge order beyond
   what is already ruled) wait for the morning.

Pulse file: `/tmp/anvil-pulse.txt`. Resume note: `docs/observations/2026-09-03-resume-here-anvil-seat.md`.

## The big goal (Gene: "give me the big goal you're driving towards; reread vision.md")

vision.md's claim: an agent's cost is its count of decisions, not its count of edits; a verb that
takes a whole intent and a gate that verifies the result turn a 9-return, 150 s task into one call
and 1.3 s of tool time (~110× on the closure). The win exists only under mandate, and the receipt is
the product. Tonight's hills, in order, and why each is worth the tokens:

1. **Get the two-call shape onto main safely.** extract-with-rewire (rf2), alias_migration (q5z),
   census, study ops, receipt ratchets, admit-gate follow-ups. Every one was NO-GO or GO-WITH-FIX on
   an executed red-team (real RCE-class holes: rg --pre, read-eval, symlink escapes, unbounded
   parse). They are the substrate for every experiment in the tech tree; nothing can be measured
   on the agent's route until they merge. Tokens go to executed re-reviews and fail-first fixes.
2. **Make the verbs survive the repo they are for.** alias_migration OOM'd on 450 files × 1.9 MB.
   A verb that dies on the codebase it targets cannot be measured. The reconciled memory design
   (aggregate ceiling before parse, single-parse outline, disk-journaled pre-image, battery as the
   merge gate) is the shortest path; the battery's numbers are the receipt.
3. **Pre-stage the morning's measurement.** E3 (fan-out verb vs native on the 21-owner rung) and E6
   (study-ops free-choice adoption) become runnable the moment q5z and study merge. Arm prompts,
   predicates, and the pre-registered pass lines get written tonight so the morning starts with "go".
4. **Leave a reader the whole picture:** log, resume note, merge queue, and a Gene report whose
   first table is what merged, what was refused and why, and the memory numbers.

Not spent on: new verbs, re-litigated decisions, unmandated benchmarks, or any lane whose result
is not a number, a green witness, or a filed verdict.
