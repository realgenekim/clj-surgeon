# The system from the driver's seat — Fable's answer (2026-09-11)

Gene: "think deeply about how to make this entire system as agent-intuitive, agent-ergonomic, and agent-accretive as you can possibly imagine. Put yourself in the driver's seat… a synthetic SYSTEM that is maximally coherent, cohesive, modular, and interconnected, forming a tower of linked abstractions that are maximally legible to you as an agent."

This is written by the agent that drove the system for the last thirty-six hours: three trunks, two pre-registered experiments, a census, a library, nine self-inflicted stops. Astra's independent answer is beside it (2026-09-11-astra-vision.md); the reconciliation follows both.

## 1. What it was like to drive

The product is good at one thing the apparatus is bad at. A Surgeon verb takes a request as data, refuses with a typed reason that carries its own repair, and returns a receipt that IS the diff. I could drive that blind. The apparatus around it — ship, land, fence-run, receipt-chain, records-push, the cohort runner, the inbox — is a set of shell one-shots that emit prose logs, hold state in pid files and run directories, and stop on conditions I discover by grepping. Every one of the nine stops last night cost the same forty minutes: find the log, find the line, find the file, guess the fix, relaunch by hand, re-arm a watcher by hand.

Concretely, from the seat:
- I re-derived the state of the world after every compaction from a resume note I maintain by hand and a captain's log that is now hundreds of entries. Nothing could answer "what is running, what landed, what is parked, what is owed to whom, what is blocked on what" without me assembling it.
- Every launch was a bespoke bash line: env exports I must remember, a brief I wrote from scratch, run-bg, then a separate Monitor bound to a pid I copied out of a log line. Every wait was a pid loop. Every result came back in a different prose shape and I extracted numbers with grep.
- Rules lived in my memory files and in briefs, not in the tools: prewarm before the first ship; never a branch that is checked out elsewhere; one JVM while a cohort runs; never edit a staged set; quoted heredocs. I forgot two of them last night and paid a round each. A rule I have to remember is a rule the system does not have.
- Three agents (Astra, Opus, Sol) each re-read the same eight documents to build the same picture, per brief, per round.
- The hand-offs builder → red-team → Sol → ship → land → rebuild → tag → inbox → log were mine to sequence, every time, by hand, with the same ten commands.

The tool taught me the fix for the apparatus: every operation is a verb with a request, a typed refusal with repair, and a receipt. The apparatus should be built the way the verbs are.

## 2. The tower

```
 L5  THE STORY       Gene report, captain's log, resume note        <- PROJECTIONS of L4, prose on top; never hand-maintained state
 L4  THE BOARD       one query: running / landed / parked / owed / blocked; bets vs measured; refusal tallies   <- a FOLD over L3
 L3  THE LEDGER      one append-only event stream every verb writes: {run-id, verb, request-hash, state, evidence paths, owner, t}
 L2  APPARATUS VERBS build · review · ship · land · measure · publish · ask   <- request-file in, typed refusal w/ repair, receipt out
 L1  SURGEON VERBS   insert_forms · rename_alias · edit · require_change …    <- the same contract, on code
 L0  BYTES & FORMS   clj-splice: spans · splice · recount                    <- pure, no policy, fenced at three
```

Each layer only talks to the one below through the same three things: a request (data, closed schema, guard hash), a refusal (typed, with the repair, naming the native failure or `none`), a receipt (state first, evidence paths, hashes, next_action). Policy lives in L1/L2 refusals. It never lives in L0 (pure), never in L5 (prose), and never in an agent's memory file: a rule that matters becomes a refusal in the verb that would break it.

**What each layer promises.** L0: bytes in, bytes out, reassembly law, no I/O. L1: the four E4 promises (count, stale, structure, anchor) plus parse errors; everything else behaves like native. L2: a run has one id, one owner, one state machine (requested → running → refused | red | green → landed | parked), evidence at known paths, and cannot be launched into a state the verb knows will fail (branch checked out elsewhere; no prewarm receipt on a catalog change; a builder holding the JVM lease; a staged set that changed after install). L3: append-only, one file, every verb writes it, nothing else is the source of truth. L4: pure fold, regenerable from L3 at any time; disagreement between the board and a log is a bug in a verb's receipt, not in the board. L5: generated, then edited by hand only for narrative.

## 3. Legibility: the one query

`board` reads L3 and prints, in this order: what is RUNNING (verb, run-id, owner, elapsed, the one line the verb last emitted), what LANDED since the last board (sha, tag, verbs added), what is PARKED with its definition of done, what is OWED and by whom (inbox items with age; bets unscored), what is BLOCKED on what. The resume note becomes `board --resume` (the same fold plus the pointer list). The captain's log entry for a landing is `board --entry <run-id>` with the receipt's facts filled in, and I write the paragraph of meaning on top. Heartbeats stop being prose in my context; they become a row in L3 that the board reads.

The test of legibility is compaction: after losing everything, one command restores the picture, and the picture is right because it was never maintained by hand.

## 4. Ergonomics

- **Briefs as data.** A brief is EDN with slots: `owner` (worktree, branch, base), `rules-profile` (a named set of hard rules the verb enforces, not prose the agent must obey), `inputs` (records paths by id), `order` (numbered steps with their deliverable), `stop-conditions`, `deliverable` (report path + required fields), `budget` (JVMs, minutes). The one-shot `build` renders it into the model's prompt AND enforces the enforceable parts. The same brief re-runs on resume.
- **Reports as data.** Every builder/reviewer writes `report.edn` (verdict, findings [severity, file:line, reproduction, exact output], measurements [name, before, after, receipt path], commits, least-sure, disagreements) plus the prose. The board scores bets against `measurements`; the log cites `receipt path`.
- **Launch as one verb with a handle.** `run build <brief.edn>` returns `{run-id, pid, log, ledger-row}`; `wait <run-id>` blocks or subscribes; no pid copying, no env exports (the verb owns them), no bespoke Monitor scripts.
- **Rules as refusals, in this order:** ship refuses without a prewarm receipt for the exact tip when the catalog/registry changed; receipt-chain checks out the sealed sha detached and refuses a branch checked out elsewhere; a JVM lease that `build` and `measure` take and `ship` reads; a staged install set is content-addressed and install refuses drift; records-push holds one lease and queues rather than failing; fence-run never leaves an orphan (a stop kills its reviewer by exact pid and archives what exists). Five of last night's nine stops disappear.
- **Context by reference.** Briefs cite records ids; the verb assembles the context pack once and hands the same pack to Astra, Opus and Sol. Three re-readings become one.

## 5. Accretion

- A red-team finding becomes a witness in the layer that owns it (library or verb), never a memory line.
- A refusal cannot be added without its `native_failure` row; the completeness check enforces it; the tally of warranted/unwarranted refusals is a board column.
- Every experiment is a `measure` run with a pre-registration id; bets are rows; scoring is a fold; nobody "remembers" who was right.
- The census re-runs monthly as a `measure` verb over the session roots; program share per intent per model is a board column with its trend.
- Memory files shrink: what is enforced by a verb is deleted from memory; what remains is about Gene, not about the tools.

## 6. Resources

Where the two days went, honestly: roughly a third of builder and reviewer tokens re-read the same documents per round; roughly a third of my own turns were stops, relaunches and hand-offs; the experiments themselves were cheap. The three changes that halve it: context packs by reference (one read instead of three); rules as refusals (five stops of nine); the board (every compaction and every status question answered by one fold instead of a re-derivation).

## 7. Build first, in this order

1. **The ledger + board** (L3 + L4): every existing one-shot appends a row at start, state change and end; `board` folds. One block. Makes the hand-maintained resume note and status re-derivations unnecessary.
2. **Rules as refusals in ship/receipt-chain/fence-run/install** (the five above): the ship v3.8 list, restated as refusals with repair. One block. Makes five stop classes unrepresentable.
3. **`run` + briefs and reports as data**: one launch verb with a handle, brief.edn slots, report.edn fields, context packs by reference. One block. Makes bespoke launches, pid copying and per-round re-reading unnecessary.
Then the product work continues on top: long-context replication and the Codex arm N as `measure` runs; the third verb only after these three.

Least sure: whether one ledger file survives contention from six concurrent verbs without its own lease (it needs one; records-push already taught this), and whether Gene wants the board in the terminal or on a page.
