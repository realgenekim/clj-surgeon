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

## Addendum (2026-09-14 18:45Z) — the vs-native table landed: diff-impact selection vs the full fast lane

Preregistered (this directory: 2026-09-14-diff-impact-measure/), five frozen diffs, n=2 per arm, native first, load-gated.

| diff | NATIVE fast lane (16 lanes) | TOOL select + run selected | selected | catch preserved | ratio |
|---|---:|---:|---:|---|---:|
| D1 one src line, historical miss | 30.2 s / 30.1 s (red: splice-envelope-test) | 2,611 s / 2,599 s (red, same ns) | 92 | 1/1 | **86×** |
| D2 test-side runner change | 32.0 s / 27.3 s | 45.3 s / 44.1 s | 9 | vacuous | **1.5×** |
| D3 committed red race oracle | 31.2 s / 30.6 s (red: lane-manifest-test) | 1,985 s / 1,985 s (red, same + the battery-lane race tests native cannot see) | 101 | 1/1 | **64×** |
| D4 green src fix | 30.0 s / 28.8 s | 2,060 s / 2,064 s (a false red: fast-lane-isolation-test) | 93 | vacuous | **70×** |
| D5 docs-only | 32.6 s / 29.7 s | 25.7 s (after one lease refusal) / 25.2 s | 6 (no allowlist at that tip) | vacuous | 0.82× |

Bets: six wall bets BROKE (median D1–D4 67× against a bet of < 0.35); four catch-preservation bets HELD (no native failure missed on any diff; kill switch not tripped); first-attempt TOOL 4/5 (one external-lease refusal, named), NATIVE 5/5.

Learning: the selector is sound and the executor is the loss — it runs one cold JVM per selected namespace, serially, across all lanes, while the native fast lane spends 16 lanes in parallel on everything; on this box selection cannot beat a gate that has already spent the parallelism selection tries to save. Caveat: the arms differ in executor and lane scope, not only namespace set (the lane runner refuses subsets by census), so the ratios judge the tool's executor, not its selection; the selection and catch columns are the comparable part and the selector wins them. Two defects surfaced: the selector's child launcher omits -Duser.home inside the run root (a false red on D1/D3/D4 from the tree's own isolation ratchet), and no docs allowlist exists at older tips.

Window meter after this: 7. A preregistered vs-native table exists for the window (the meter's condition), and it is a loss recorded with its bets. Index unchanged at 4.

## Addendum 2 (2026-09-15 12:20Z) — the repaired replay (one replay, as ruled)

Subject trunk 5cc35402 (executor parity landed 09:31Z as stable/2026-09-15.1). Same five frozen diffs, n=2 per arm, native first, quiet box (all 30 cells first attempt).

| diff | NATIVE fast lane (8 admitted workers) | TOOL-FAST select + selected∩fast | selected fast/all | catch | ratio | noise gate | verdict |
|---|---:|---:|---:|---|---:|---:|---|
| D1 src line, historical miss | 29.4 / 30.7 s (red) | 33.3 / 33.1 s (same red) | 45 / 92 | 1/1 | **1.11** | 4.3 % | slower |
| D2 test-side runner change | 30.2 / 27.9 s | 12.7 / 12.7 s | 7 / 9 | vacuous | **0.44** | 8.0 % | **faster** |
| D3 committed red race oracle | 30.0 / 29.4 s (red) | 34.1 / 34.1 s (same red) | 51 / 101 | 1/1 | **1.15** | 1.9 % | slower |
| D4 green src fix | 30.7 / 27.2 s | 31.4 / 31.5 s | 46 / 93 | vacuous | 1.09 | 11.9 % | tie |
| D5 docs-only | 26.7 / 27.8 s | 7.0 / 7.1 s | 5 / 6 | vacuous | **0.26** | 4.0 % | **faster** |

TOOL-ALL (reported separately, never blended): 18.6 s (D5), 32 s (D2), 1,331 s (D1), 779 s (D3), 796 s (D4); it found the battery-lane race tests on D3 both runs, which the fast lane cannot see.

Bets: Fable median D1/D3/D4 0.8 → 1.105 BROKE; Astra 0.9 (plausible 0.7–1.2) → point broke, inside plausible. D2: Fable 0.45 / Astra 0.5 → 0.44 HELD both. D5: Fable 0.35 / Astra 0.4 → 0.26 (better than both). Catch preservation held everywhere (0 missed); first attempt 5/5 on all three arms; TOOL-ALL slower on D1/D3/D4 as both bet.

Learning (measured): selection wins only where it shrinks the fast projection to a handful; on broad source diffs the makespan floor is the slowest namespace (a 300 s-estimated lane member), so cutting 94 to 45 namespaces removes 0.6 s while cutting 95 to 7 removes 22 s. Decision rule applied: a TOOL-FAST win beyond noise with preserved catches exists on narrow diffs only; native routing stays the default; the selector earns a place only behind a size threshold on its fast projection (a routing decision, filed, not built).

Window meter: 8. A preregistered measurement, replayed once after a repair, with a bounded real win and zero missed catches. Index unchanged at 4 (E3 awaits the mayor's acceptance of the next real content-edge diff).
