# plan-2 Cell C result — the one-call namespace_split verb vs native vs REPL-driven, all with the machine-ready manifest (2026-09-07, written 23:24Z)

Fixture: curtaincall-cfp views.clj (4594 lines, 141 defs) → 20 destination namespaces, 87 caller sites in 5 files; base split/base d9205abc; 4 oracles (kaocha unit green, views.clj gone, cc-oracle.py 141/141 owners exactly once, historical architecture guard 7/7). Fresh Opus callers, 4-concurrent on 16 cores, stamps process-written. Verb build: branch astra/namespace-split 913020c8 via bb launcher (CLI `:op :split-ns!`); MCP entrance not exercised in this cell.

| arm | JVM ready | plan | moves/split | callers | warm green | cold suite | total wall | correct |
|---|---|---|---|---|---|---|---|---|
| D1 verb, one CLI call | — | 12 s | 50 s (in-call 30.7 s, 22 s kaocha) | in call | — | in call | **70 s** | 4/4 |
| D2 verb, one CLI call | — | 15 s | 53 s (in-call 30.4 s) | in call | — | in call | **86 s** | 4/4 |
| N5 native + manifest | — | 242 s | 251 s | 322 s | — | 349 s | **408 s** | 4/4 |
| N6 native + manifest | — | 90 s | 248 s | 382 s | — | 408 s | **486 s** | 4/4 |
| R3 REPL-driven + manifest | 95 s | 234 s | 287 s | 390 s | 447 s | 477 s | **535 s** | 4/4 |
| R4 REPL-driven + manifest | 30 s | 296 s | 379 s | 458 s | 469 s | 519 s | **564 s** | 4/4 |

Medians: D 78 s · N 447 s · R 550 s. Native ÷ 5.7 for the verb (Cell A plan-free native median 362 s → ÷ 4.6). Astra's killer threshold (≥ 30% matched median reduction, > 2σ of native controls, every treated run accepted, no regression) — cleared at n=2; pre-registration requires two consecutive reruns after the paper-cut round (pending).

Learning: the manifest did not rescue native or REPL (planning stayed 90–296 s; all four hand arms re-derived the reference graph themselves via clj-kondo analysis). The win is the verb's interface — one request, decisions not files — and its static reference authority (~8 s). The REPL arms had real red→green loops this time (R3: empty :require, eaten ns parens, wrong alias → relaunch; R4: classpath .addURL hack, 12 unused requires) caught warm in seconds, and still lost by 50–80 s to native: JVM start 30–95 s, planning unchanged, file emission the same typing, cold proof still paid.

Manifest defect (all four hand arms): `:declares` marks every entry forward-reference-within-destination false; two are true (dev-strip→time-travel-bar in organizer-layout; row-controls→row-controls* in review). The verb derives declares itself and was unaffected. Filed with the paper cuts (inb-525d27).

Verb paper cuts (D1/D2 callers): kondo capture exit 3 unlabelled inside a green receipt; requires appended unsorted / 4-space indent / whitespace-only line left; stale prose mentions of the retired ns unflagged; ns docstrings re-encoded with literal \\n. Handed to Astra (gpt-6-astra) on the branch 23:24Z.

Caveats: n=2 per arm, one fixture, 4-concurrent; D callers skipped plan-only; R arms both needed a classpath repair the :nrepl alias lacks (test deps) — a fixture defect that cost R3 ~40 s and R4 ~60 s, not enough to change the ordering. Raw stamps and receipts: 2026-09-07-plan-2/cellC/<arm>/.
