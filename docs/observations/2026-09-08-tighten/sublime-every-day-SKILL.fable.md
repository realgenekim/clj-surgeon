---
name: sublime-every-day
description: "The sublime dev experience as a daily practice: an inner loop measured in seconds (clojure-fast-feedback), a fix routine measured in minutes (round), a daily unit that turns friction into delivered ratchets (tighten), and a seat/fleet layer that proves it every day. Say 'sublime' to open the day; say 'tighten' for the daily unit."
user-invocable: true
---

# Sublime every day — the practice, in three nested loops

> **STATUS 2026-09-08: DRAFT, written by the Fable seat, awaiting Astra's review.** Superset of
> [clojure-fast-feedback](../clojure-fast-feedback/SKILL.md) (contained by reference) and
> [tighten-the-loop](../tighten-the-loop/SKILL.md) (the daily unit; Sol NO-GO conditions apply until met).
> Commands marked *contract-only* do not exist on this seat yet.

## What "sublime" means, measured

Gene's words: fast, frequent, scrutinized feedback; "genuine 10x". Our meters, in the order they bind:

| loop | unit of work | sublime is | falls short when |
|---|---|---|---|
| inner | one red→green edit | warm probe ≤ 10 s; cold proof once, at the end | any JVM start inside the loop; a second JVM; a test run on the wrong tree |
| routine | one fix round (builder → grade → witnesses) | ≤ 4 min, ONE command, receipt with pids | a hand-written brief; a pid from a handle; a round over 10 min |
| daily | one seat, one day | friction → witness → delivered → next encounter accepted | a paper cut found by a cohort; review per iteration; a fix without a ratchet |
| fleet | every seat, every day | every seat writes a receipt; a fresh seat passes 11/11, 0 forbidden | a seat with no receipt; zero Andon pulls in a week; apparatus share ≥ 50 % |

The number Gene reads first is **apparatus share**: the fraction of a fresh agent's actions spent on the
loop's machinery rather than the task. Under 50 % is the alarm line; the canary measures it daily.

## Loop 1 — the inner loop (seconds): `clojure-fast-feedback`

Delivered as INLINE BYTES (`COLDSTART.md`, 12 lines) into the repo's `AGENTS.md`/`CLAUDE.md`, never as
a pointer: a pointer is not delivery, and Codex cannot load a Claude skill. One warm nREPL per repo
carrying the test alias (`clj -M:test:nrepl`, port in `.nrepl-port`), reload + affected tests as the
probe via `clj-nrepl-eval`, one cold suite as the proof, an oracle for output quality. Rules that
travel with it: run the gate BEFORE editing; never start a second JVM; temp under `/var/tmp/<seat>`;
the receipt line is literal, never paraphrased. Full text: the parent skill.

## Loop 2 — the routine (minutes): `round`

`/home/forge/bin/round <worktree> <findings.md> <name>` owns ordering, timing, errors and the receipt:
a long-lived builder (orientation paid once), the paper-cut oracle (a predicate, not a cohort), the
witnesses, a verdict line, pids in the receipt. Reviews (`fence-run`) and landings (`land`) are BATCH
costs, once per batch of rounds, never per round. The second hand-written brief for any routine is
the stop signal: build the one-shot, then continue. Corpus: `/var/tmp/forge/round/fixtures/run-fixtures.sh`.

## Loop 3 — the day: `tighten`

Say **tighten**. The daily unit, in order (absolute commands; details in tighten-the-loop):

1. `/home/forge/bin/seat-receipt` — am I on the loop? (routing plate fresh, corpora clean, JVM client
   resolvable, disk and inode headroom as REMAINING, friction count). Committed and pushed to records.
2. `/home/forge/bin/canary-cell` — one known task through the whole loop on a fresh specimen; grades
   11 required / 12 forbidden; prints apparatus share; over 50 % files a tripwire item.
3. `/home/forge/bin/ledger-to-findings <scope>` — projects the PROSPECTIVELY approved friction items
   into a findings file and prints the round command. It renders a selection; it never makes one.
4. `/home/forge/bin/round …` — one earned batch; the builder is delegated (Fable specs and verifies;
   an Opus mayor routes selection to Fable and Sol, implementation to Codex Sol).
5. `/home/forge/bin/fence-run` once per batch → `/home/forge/bin/land <sha> "<title>"` on GO and
   battery-fresh. Landing is not delivery: the consumer seat's uptake is verified before "delivered".
6. `/home/forge/bin/verb-sentinel` weekly — every admitted verb still passes its oracle; drift is
   measured against the previous sentinel, `no-baseline` on a first run, never against a cohort number.
7. `/home/forge/bin/tighten status` — the loop on one screen, no JVM, no network.
   `tighten binding` — *contract-only*: the immutable seat binding Sol's review requires.

## The change rule (the fixed point)

Improvement may change machinery but never its own authority or evidence. If product bytes are
unchanged while a changed oracle, classifier, observer, task set, acceptance rule or clock makes a prior
failure pass or improves a metric, qualification is REFUSED under the current intent. Every meter change
freezes the current epoch first and replays the triggering case on old and new instruments. Full text:
tighten-the-loop/CHANGE-RULE.md.

## The rails (the mistakes that cost the hours, encoded)

- A pid comes from a receipt line the run printed; never `$!`, a wrapper, a helper, or a cwd scan.
- The reviewer's probe and the builder's witness are ONE test file in the repo; never prose between them.
- The meter runs on the oracle-clean tip immediately; review gates landing, never measurement.
- Every fix registers a linked intent (linked-intent-testing); witnesses fail first and assert at the ceiling.
- Test with the caller's real bytes; hand-drive every mode you ship; a receipt names its subject, its
  evidence source and the causal binding, else it is `:unverified`.
- Report at phase changes only, one line each. Announce a batch before it starts, with its expected wall.

## Every seat proves it (the fleet layer)

A seat receipt per seat per day; a missing receipt is a finding, not silence. Canary daily, sentinel
weekly, both on cron with a freshness tripwire. The Andon cord is one command with a delivery receipt
(*contract-only* today: use the maven inbox + a direct session message; ack ≤ 5 min or escalate). Zero
pulls in a week is an alarm. New seats inherit everything from the inline bytes plus the bundled `bin/`
(tighten-the-loop/bin, MANIFEST with hashes); the cold-start harness (`coldstart`, `coldstart-grade`)
proves a fresh Claude and a fresh Codex execute it before it is called delivered.

## What Gene keeps by hand

Direction changes, irreversible scope, device receipts, the round's commit-contract ruling
(inb-9c04cc), naming the first epoch's steward / recorder / duty supervisor / rung 2, and pruning
this text. Everything else runs without him and reports headroom, never consumption.

## Scars (why each rule exists; one line each, receipts in clj-surgeon-records)

Cohort-found paper cuts (81 → 0 only after the oracle) · a 45-min round for 3 min of machine work ·
pid misbinding ×5 · pilot 1/4 → 8/8 after inlining the bytes · a fixed-point attack that passed the
draft (Sol, 2026-09-08) · a fabricated inode headroom of 0 · a self-built receipt presented as v1.
