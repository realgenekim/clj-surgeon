# Gene report — 2026-09-11 morning (window: 2026-09-10 19:40Z → 2026-09-11 08:10Z) — written 2026-09-11T08:05Z

## 1. Performance vs native

| what | native | Surgeon | verdict |
|---|---|---|---|
| E4 on the real file, complete clock intent → oracle (n=1, one builder) | Python as written 11.1 s and 29 broken route sites with gates green; corrected Python 13.8 s; hand-reviewed Edit 21.3 s | 8.5 s, byte-identical, two warranted refusals recovered in 1.2 s | Surgeon fastest and only route safe against the wrong request |
| Cohort, 12 real tasks + E4, fresh sessions, Sonnet + Opus, 120 runs | accepted 42–50% across arms; N (native policy) 50% | plate arms 42–46% | no difference; zero silent-wrong in any arm |
| Gate vs prompt, Claude, 156 runs | hooks fired 0–2/13, never on task source | H − H0 = −1 Surgeon-final both callers | Fable's thesis falsified as registered; Astra 11/12 |
| Gate vs prompt, Codex, 130 runs (Astra's design and bets) | bare 0/13 Surgeon-final both models | MCP available: astra 10/13, sol 4/13; plate: 4/5; acceptance unchanged | Astra's main bet held; routing moved, correctness did not |
| Test speed after clj-splice | — | focused verb gate 33.5 → 17.1 s; library 2.7 s JVM / 3.5 s bb; 1,149 → 517 assertions | measured, receipts in 2026-09-11-clj-splice/measure.md |

## 2. Wins and losses
**Wins.** Three trunks in one night, each one launch after the first: insert_forms (a6e53564), rename_alias (04648059), clj-splice with both verbs ported (eae1e432 = stable/2026-09-11.2). A census of 1,570 sessions that changed the meter in twelve minutes. A cohort apparatus with two independent readers that refuse to verdict when they disagree, calibration that caught its own scorer, and 119/120 byte-identical reconstructions. Two pre-registered experiments with two bettors, scored in public. A class ratchet for receipt booleans after the same defect appeared on two builds. The refusal policy rewritten under Gene's "hysterical" challenge and Astra's correction, with a native_failure field per refusal and a completeness check. The layer on rewrite-clj Gene asked for, three functions, fenced, isolated behind :local/root.
**Losses.** My gate-vs-prompt bets were wrong on every specific cell. Nine apparatus stops on its own bookkeeping in one night (manifest self-reference twice, deftest census, branch-name checkout, stale skill name, catalog pins ×4, orphaned reviewers ×3). The curator read the test entrance at HEAD not at each sha (rescored). The repo-level .codex/config.toml did not deliver MCP to codex exec here, contradicting the 09-06 memory (flagged, not overwritten). Sonnet never called Surgeon once in 84 fresh-session runs with a live server and a plate.

## 3. Learnings → ratchets
| learning | ratchet |
|---|---|
| A receipt boolean that cannot be false is a false green in the product | receipt_booleans_test fails by name for any verb without a registered false seam |
| Catalog pins live only in the gate lanes | memory: prewarm the exact tip before the first ship of a new tool/op; inb-f29131 one catalog registry |
| "Same edit natively" is undefined; refuse only on a broken promise | native_failure per refusal in refusals.edn + completeness assertion; memory refusal-must-name-the-native-failure |
| An agent cannot respond to a refusal it never receives | exposure reported first in every hook experiment; H0 control |
| A post-hoc gate cannot catch E4; only the declared count can | the four refusals live at request time |
| Fresh sessions do not carry the census habit | next measurement is long-context, not another fresh cohort |
| --ignore-user-config does not suppress ~/.codex/AGENTS.md | per-arm isolated CODEX_HOME in every Codex experiment |

## 4. What's next
1. Long-context replication on the Claude side (the only measurement that can speak to the census's 107 broken files).
2. Codex: arm N and the M/N cost ratio (Astra left them unmeasured); reproduce the repo-level .codex/config.toml delivery failure and settle the memory.
3. Ship v3.8 list (inb-7f191b, inb-e0f549, inb-6fd761, inb-d6a0c3, inb-5e5724, inb-78d458) and the catalog registry (inb-f29131) before any third verb.
4. File the rewrite-clj upstream issue (text drafted) on Gene's word; move libs/clj-splice to its own repo when Gene says.
5. Mayor: skiff installs and receipts (inb-ece087 supersedes), skiff seat-prompt exception (inb-a14fd6), skiff census (inb-9be58c). Gene: D1–D8 (inb-8c982e).
Sublime: state 9 (trunk eae1e432); window 9 for the night — three landings, two experiments scored honestly, the layer built; held from 10 by the apparatus stops and by my own falsified bets.
