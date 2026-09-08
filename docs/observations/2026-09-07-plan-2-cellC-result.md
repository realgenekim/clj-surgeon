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

## Wave 2 + 3 (23:44Z): warm-JVM REPL arms, and the verb on Astra's paper-cut build (9b205fd4) via CLI and MCP

Conditions: R5/R6 on split/base2 (4f6283aa = d9205abc + `make nrepl` composing :run-tests:test:nrepl + guard tracked) with the nREPL ALREADY WARM and eval-probed before orient-start; D3/D4 (CLI launcher) and M3/M4 (MCP `namespace_split` on the branch server 7907 via ~/bin/surgeon-call) on d9205abc with the same request. SIX arms concurrent for wave 3 (Cell A/C wave 1 had four) — walls may read slightly high.

| arm | connect | plan | moves/split | callers | warm green | cold suite | total | correct |
|---|---|---|---|---|---|---|---|---|
| D3 verb CLI, fixed build | — | 12 s | 59 s (in-call 37.3 s, kaocha 23.5 s + lint-delta 5.0 s) | in call | — | in call | **116 s** (oracle→done 32 s of caller reporting) | 4/4 |
| D4 verb CLI, fixed build | — | | | | | | **87 s** | pending report |
| M3 verb MCP, fixed build | — | | | | | | **151 s** | pending report |
| M4 verb MCP, fixed build | — | | | | | | **80 s** | pending report |
| R5 REPL, warm JVM | 60 s (client written, JVM was up) | 254 s | 353 s | 445 s | 445 s | 471 s | **513 s** | pending report |
| R6 REPL, warm JVM | 21 s | 279 s | 341 s | 478 s | 482 s | 515 s | **554 s** | pending report |
| controls N5/N6 native + manifest | | 242 / 90 s | | | | | 408 / 486 s | 4/4 |

Verb medians: wave 1 78 s → wave 3 102 s (six-concurrent, +5 s lint-delta check, caller reporting inside the clock on D3). Still ÷4.4 native. REPL with a warm JVM: 513/554 vs native 408/486 — the JVM start was NOT what held the REPL arms back; planning (254/279 s) and emission are. Paper cuts confirmed fixed by D3: requires merged in sorted position with the host's 3-space indent, 3 prose-mention advisory rows emitted, lint-delta baseline-relative (introduced_errors 0, +4 warnings), kondo capture row labelled baseline. New wart (D3): all 20 destinations carry a verbatim copy of the monolith's docstring ("this file"), correct-by-construction but misleading on format/avatar/shell.

Review: Sol GO for 913020c8 (r1 verdict, 2 JUDGMENT rows: unknown_count scope honest-narrow; dry merge shows two additive conflicts vs origin/MCP/main 8a64a092). Delta review r2 of 9b205fd4 launched 23:44Z (pid 3948537).
