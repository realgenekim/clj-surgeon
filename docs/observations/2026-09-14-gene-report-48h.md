# Gene report — 2026-09-14T13:26Z (covers the last 48 h: 2026-09-12 13:20Z → 2026-09-14 13:20Z; 06:20 Pacific Sunday)

## 1. vs-NATIVE (the only measured table in the window)

Probe verb vs a cold focused JVM run, preregistered, subject 759974c7, n=9 per arm per stratum, settled 2026-09-13 (amendment 2 closed the refusal column):

| stratum | NATIVE median ms | PROBE median ms | PROBE/NATIVE | first-attempt | planted non-test refusal (probe / native) |
|---|---:|---:|---:|---|---|
| bb-portable (T1–T3) | 3,525 | 662 | **0.19** | 6/6 both | 2/2 typed / 0/2 silent `:test 0` exit 0 |
| JVM-only (T4–T6) | 4,210 | 1,170 | **0.28** | 6/6 both | — |

All bets held. **No other vs-native measurement was launched in the 48 hours.** Every hour of 09-14 went to landings, the ship tooling, and its reviews.

## 2. Sublime meters (three, never blended)

| meter | 09-12 | 09-13 | 09-14 (to 13:20Z) | reading |
|---|---|---|---|---|
| trunk | 9 | 9 | 9 | self-issued ceiling; five landings today, each Sol GO on its final read |
| window | 7 | 8 | **5** | the window rewards measurement; today had none and lost three landings to my own process |
| index (Fable) | 4 | 6 → 4–5 | **4** | daily meter now has its first real number: **0 of 2** registered findings transferred — both recurred inside the fixes meant to close them |
| index (Astra) | — | ~4 | — | not re-asked today |

Honest reading of the index: the runner now exists and scores end to end on the production ledger, so the meter is real for the first time. What it measured is that the seat's fixes do not yet close their class on the first try (E1 recurred three times, E2 twice). The number is low because it is true, not because the meter is missing.

## 3. Top wins and losses

**Wins**
- Five trunk landings: txn-tombstone race (stable/2026-09-14.1), state-home identity (.2), Skiff darwin blocker (.3), battery ledger gating + walls admission (.4), diff-impact file-content/source-scan edges with fail-closed HOLD (.5). Every one a Sol GO after at least one NO-GO that named a real class.
- Skiff runs the tag with a real task and telemetry 4/4; the mayor issued the first receipt outside this seat (a refusal on capacity, numbers included).
- The encounter runner is installed and scored E1/E2. Its P0 turned out to be a different mechanism than I filed: not a lock race but an install swapping a module under live supervisors. Two independent reviews and an Astra design consult converged on the fix; the installer now warns about live old-vintage supervisors and probes the production ledger before declaring OK.
- The ship tooling gained: autofix fix-branch exclusion + census hook, receipt-chain-only ledger appends, and an install gate that refuses a candidate not descending from the installed source — the ratchet for a loss below.
- Two registration entrances: diff-impact edges landed; the one-entrance test-namespace registration is on its sixth Astra round with every Sol finding closed by a seam-free real-gate matrix (prewarm running now).

**Losses**
- I installed the encounter runner from a branch cut before the bookkeeping fixes and silently dropped them; the next landing reproduced the receipt-branch race and the receipt step refused. Rolled back — first to the wrong backup (a backup is what an install replaced), then to the right one. Cost: ~1 h and one landing.
- Two landings lost to the fast-lane budget because I launched ships while Astra/Opus builders ran JVMs — the same class as the 09-13 loss. Ships are now chained behind builder pids, and the tool-side refusal is being built (round 10).
- My P0 named a mechanism inferred from five numbers, and it was wrong; four review rounds had been spent on the anchor's locking.
- Two builder briefs turned "temp under X" into product paths (state root; registration lock) — recurrences 1 and one of the regns rounds.
- The regns branch needed six rounds: my brief's temp root, the fast-lane budget (matrix in the wrong lane), an agreeing-numbers merge with trunk, and a seam in its own matrix that hid the wrong-namespace first-contact message.

## 4. Learnings → ratchets (filed, with owners)

- Shipped: INSTALL.PROVEN carries source=<sha>; a non-descendant candidate refuses `install-drops-installed-fixes`; dirty and non-git sources refuse with recorded overrides; a missing witness is counted, never skipped (ship 440feb8, installed 12:57Z).
- In flight: ship/receipt-chain/packet refuse to launch while a JVM builder is alive (inb-0db961, Opus round 10).
- Memories written: P0 mechanism is evidence not inference; a brief's temp root leaks into product code; install proves new bytes not retained fixes (+ rollback corollary); witness exit never through a filter (reading-side twin).
- Filed: inb-c9e185 BATTERY_LEDGER_APPEND is ambient authority; inb-272a68 HOLD vs crash needs a parseable receipt + atomic write; inb-542224 empty evidence path misreports; inb-27e779 the P0 correction; inb-f46e4e bookkeeping residuals. Closed: inb-16b396, inb-f0202f, inb-f85401, inb-1b7f3c.
- Owed: anchor rotation plan (0.28 s/append at 15k rows); independent acceptance of the amended ENCOUNTER-RUN-020/022; a vs-native measurement this week.

## 5. What's next

1. Land the test-namespace entrance (regns 4a43f8db: prewarm → Sol round 3 → tag .6) and install ship round 10 (refuse-on-overlap). Then the ship lineage is one branch and the box holds its own line.
2. Register the day's recurrences as encounters through the runner (the ones above are exactly its subject) and score them on the next fix — the index moves only when a fix closes its class first time.
3. A vs-native measurement on a branch tip, ≥50% of the next block: candidates are the diff-impact selection (gate wall with vs without edges on a real diff) or the registration entrance (author time to register a namespace, tool vs native).
4. On Gene's word only: tower block C, the spent cohorts, the wiki promotion queue.
