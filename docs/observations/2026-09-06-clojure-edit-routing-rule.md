# Clojure edit routing rule — proposed replacement for the 2026-09-02 "prompts do not mention Surgeon" ruling (2026-09-06, 03:1xZ)

Gene: "what should the prompt read given our environment? We don't want to dissuade unnecessarily. What should the rule really be?"

The 2026-09-02 ruling was measured on Sol, the pre-13c12401 build, and the MCP per-form editing grammar; the agent paid 2x by layering per-form writes on its native loop. Tonight's route is the mission executor (owners + intended forms + proof profile → verified commit with receipt and undo, or a typed refusal before any write); its measured result on the same real file is 3x (Astra, complete CLI) to 11x (bench harness) on bounded edits, with refusals that name the reason. A rule written for the first must not govern the second.

## Agent-facing text (seat header + every builder brief)
**Clojure edits, routing rule (measured 2026-09-06 on trunk ≥ 13c12401 with the mission executor).**
- Reading: outline once, then read the form. `clj-surgeon :ls <file>` and `:cat :forms […]` are the default for any file over ~150 lines; raw `sed` only for a known line range.
- Bounded mechanical edits (rename, move helpers, thread a parameter, change every call site, add a require across namespaces): route through the executor first — `mission open` with the owners, the intended forms, and a proof profile; accept its verified commit or its typed refusal. On a refusal, do the edit natively and record one dogfood-ledger line: edit | route | refusal type | wall. Never route around a refusal.
- New code, new tests, prose, non-Clojure: native; say so in the ledger line as ineligible.
- Never: the per-form MCP write grammar for fan-out, `apply_clojure_changes` with a namespace owner, forms-scoped find+replace for insertion — the measured losers of 2026-09-02, still losers.
- Tie-break is the receipt: when both would work, use the executor if the change will be reviewed or landed (the receipt and undo are the value); native for throwaway scratch.
- The rule expires when the ledger says so: re-measured from ~/.clj-surgeon/events.jsonl (executor-first rate, refusal reasons, wall) at each Gene report. A rule that dissuades use of a tool that has since changed is a bug in the rule.

## Doctrine sentences (house-rules; Gene's or the mayor's to install)
Routing rules name the build and the caller they were measured on. When either changes, the rule is re-measured before it is applied, and the measurement comes from the tool's own ledger, not from a prompt experiment.

## Status
Installed on this seat (Fable's header and briefs) from 03:2xZ; house-rules text ready to paste. Astra already operates under the equivalent (executor-first for eligible edits, refusal → native, ledger line).
