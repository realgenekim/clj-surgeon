# Gene report — 2026-09-14 05:00Z (covers 2026-09-11 → 09-14)

## 1. vs-NATIVE (the only measured table in the window)

Probe verb vs a cold focused JVM run, preregistered, subject 759974c7, n=9 per arm per stratum, noise gate cleared ~48× (2026-09-13-probe-measure-report.md, amendment 2 settled the refusal column):

| stratum | NATIVE median ms | PROBE median ms | PROBE/NATIVE | first-attempt | refusals (probe / native, planted non-test) |
|---|---:|---:|---:|---|---|
| bb-portable (T1–T3) | 3,525.08 | 661.66 | **0.19** | 6/6 both arms | 2/2 typed / 0/2 (both `:test 0`, exit 0) |
| JVM-only (T4–T6) | 4,209.75 | 1,169.67 | **0.28** | 6/6 both arms | — |

Fable's bets held (0.50 / 0.30 / equal acceptance / 2-2-0); Astra's held on both arms. Setup 16.1 s repaid in six invocations. **No other vs-native timing was measured in the four days.** Everything else below is functional or apparatus work, and the window meter says so.

## 2. Sublime meters, last four days (three meters, never blended)

| day (UTC) | trunk | window | index (Fable) | index (Astra) | what moved it |
|---|---|---|---|---|---|
| 09-11 Thu | 9 | 8→9 | 3→4 | ~3 (low conf.) | ship v3.9–3.11, plan of record, sublime integration; first Codex routing table (M−L +10/+4) |
| 09-12 Fri | 9 | 7 | 4 | — | block B landed after 26 attempts / 9 ship rounds; the boofarama assessment; management rules changed |
| 09-13 Sat | 9 | 8 | 6 → restated 4–5 | ~4 | probe measure settled (window ↑); data-not-code landed; Astra: independence 0, daily unknown → I conceded |
| 09-14 Sun (to 05Z) | 9 | 5–6 | 4–5 | — | three landings; first independent receipt (a refusal); first destination task with telemetry on the Skiff; zero vs-native measurement launched today |

Reading, honest: trunk is at its self-issued ceiling and stays there. Window fell today because the hours went to landings and the pipeline's own bookkeeping, not to measurement. The index moved on two of its five meters for the first time — independence (0 → one receipt issued outside this seat, and it was a refusal on capacity) and fleet (one seat → a second seat running the tag with a real task and telemetry 4/4, self-issued) — and is unchanged on the ones that decide it (daily/second-encounter: the registered encounter recurred twice inside its own fix and is scored `untested` because the runner is unshipped).

## 3. Top wins and losses

**Wins**
- Three landings tonight, each Sol GO on its final read: the txn race fix (stable/2026-09-14.1), state-home / Gene's inb-3c65d7 (.2), the Skiff darwin blocker (.3).
- The Skiff runs the tag: CLI hash matches, typed probe refusal, server with 13 tools, a real edit accepted with telemetry 4/4 (mayor, inb-fd0b06). Gene: "macos works now."
- The first independent receipt exists (mayor, e4e4bed5): a refusal on capacity with numbers, and the mayor declined to invent headroom.
- Two class oracles that earned their keep before they were green: the 72-cell state-root matrix (first run red 43/48) and the 21-schedule race oracle (found the race one layer below where every battery said it was — in receipt-artifacts target resolution, i.e. in the previous day's landing).
- Sol named four classes on one review and was right on all four.

**Losses**
- The v3.13 install stopped this seat's line for seven minutes (ledger anchor sequence race); rolled back. Four rounds and four reviews later the encounter runner is verified and still unshipped.
- One fix, eight Astra rounds and six ship rounds; the registered encounter recurred twice INSIDE the fix meant to close its class.
- The pipeline lost to its own bookkeeping more than to code: receipt-branch push race ×3 (root cause: the autofix picks the first origin ref at the tip, which was the seat-created receipt branch), autofix without census ×2, tracked battery ledgers conflicting on every merge ×3.
- Mine: pushed a red tip once (exit masked by a filter), misread 71 passing matrix rows as failures once, overlapped two suites once and paid a 1.4 s fast-lane budget overrun that cost a landing, and my round-1 brief planted the hardcoded temp root that became recurrence 1.
- Zero vs-native measurement launched on 09-14.

## 4. Learnings → ratchets (filed, with owners)

- inb-f0202f (P0): ledger anchor race; install proof never exercises the production ledger; broken anchor must be a typed refusal.
- inb-db9e78: fix-branch selection excludes receipt branches, refuses ambiguity — in flight (ship bookkeeping packet).
- inb-61761d: autofix regenerates the deftest census — in flight (same packet) + `make census-regenerate` (product round in flight).
- inb-16b396: battery appends its tracked ledger only when minting a receipt; walls to the state root — in flight.
- inb-5fad8b: witness order-dependence; install proof blind to a red stage. inb-b10334: five registrations for a new test namespace, one entrance. inb-f85401: impact oracle lacks file-content edges. inb-9b79b1: formatter false green. inb-04761f: :receipt-out ignored. inb-6764cc: absent anchor degrades silently. inb-a307cb: maven `attempt --note` drops text (mayor filed the kiloclaw side).
- Closed: inb-c74f05 (race) by e2050d8f; inb-3c65d7 by f1433d82; inb-de7ca3 (Skiff rollout) by the mayor's record.
- Wiki: #73 "an injected fault that replaces a side effect tests half the class". Memory: witness exit never through a filter.

## 5. What's next

1. Land the ship bookkeeping packet and the battery-ledger gating (both running) — every landing paid ~20 min for these.
2. Ship the encounter runner (P0 first), then score E1/E2; that is the daily meter's first real number.
3. Registration entrances (test namespace, refusal kind, oracle edges): the three items that turned one-round fixes into three-round fixes.
4. On Gene's word: tower block C, the spent cohorts, the wiki promotion queue. Owed to the window meter: a vs-native measurement this week, not more apparatus.
