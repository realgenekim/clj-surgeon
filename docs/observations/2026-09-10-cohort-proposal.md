# Proposal: programs-to-contracts cohort — measure the improvement (2026-09-10)

Gene: "Propose something and have Astra review. Then go. Measure improvement."

## Claim under test
The program-writing habit (census: 6,124 program edits / 2,132 editor / 220 Surgeon; Fable 100%, Opus 90%, Sonnet 17%, Sol 3%) is driven by (1) a harness line preferring shell edits, (2) refusals without a repair path, (3) no route to a one-call verb for the three dominant intents, (4) verification without a diff. Interventions on (1) and (3) — the cheap ones — should move program share and silent-wrong rate measurably within a day.

## Design
- Intents (the census's top three = ~80% of program edits): T (edit a deftest body), R (change a require alias and its uses), I (insert a defn after a named defn). Four tasks each = 12 tasks, drawn from curtaincall-cfp @ 00e8f0fa real files, each with an INDEPENDENT oracle (the E4 oracle pattern: kondo analysis for bindings/usages; per-form hashes vs git for preservation; string-literal multiset for decoys). Oracle must be red on a constructed wrong output before use.
- Arms (free choice of method; the agent is told the task, never the tool):
  B  baseline: the seat's prompt as of 2026-09-10 morning (with the "prefer sed/heredocs" harness line).
  A  the Clojure exception line added ("Clojure files are forms, never strings").
  AC A + a routing plate naming insert_forms / rename_alias (or alias_migration until it lands) / within+from/to with request-file examples, routed without asking.
- Callers: claude-sonnet-5 and claude-opus-5 (17% / 90% in the census); 12 tasks x 3 arms x 2 callers = 72 runs, each a fresh worktree, each `claude -p` on the subscription (no metered keys), Anvil-local, ≤4 concurrent, every JVM -Xmx bounded.
- Meter per run: mechanism used (program / editor / surgeon — from the transcript's tool calls, the census classifier), oracle verdict, gate verdict (repo tests) so gate-green defects are counted separately, complete wall from prompt to oracle, refusals (warranted/unwarranted), diff read (y/n), forms changed beyond intent.
- Improvement = program share and oracle-fail rate, B → A → AC, per caller, with Wilson intervals; n=12 per cell. The census reading is the outside anchor, not a control.

## Threats to validity (for Astra to attack)
- Tasks are drawn by the same seat that built the tool; selection bias toward tool-shaped tasks.
- 12 per cell is small; report intervals, no p-values.
- The AC arm needs insert_forms on trunk (in fix round now) — if it slips, AC runs with the two existing verbs and says so.
- Free choice on a fresh session ≠ the census's long sessions (context length is a driver we do not model).
- The oracle and the tool share rewrite-clj (Opus red-team: independent bracket hasher agreed 10/10; keep it as the second oracle).

## Cost
Apparatus (tasks + oracles + runner): one Opus builder, ~2 h. Run: ~72 x 3 min = ~1 h wall at 4 concurrent. Report: 30 min.
