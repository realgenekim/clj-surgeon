# Program of record — nail the namespace-split use case (Gene, 2026-09-07 21:3xZ)

Gene, verbatim: "Yes, propose design to nail this use case — riff with Astra, and then have Astra build." "And repeat timings; repeat until paper cuts and it's perfect, and you have decisive wins vs native. And make sure there's a cli version of it. (Use MCP or cli for dev work; whichever has fastest feedback loop); maybe nREPL, too."

The loop (each turn is one ledger block):
1. Design reconciled (Fable's design-fable-split-verb.md × Astra's astra-opine.md) → one contract for `split_plan` (read) and `split_namespace` (write), MCP and CLI parity from the first batch, nREPL-driven development where it is the fastest feedback (the tool's own repo has `make nrepl`).
2. Astra builds in small batches (analysis read → split write → CLI → rebuild 7906), each with red-first witnesses, Sol fence review, ~/bin/land.
3. Rerun the cell: same base d9205abc, same plan supplied, fresh Opus callers N and T (n=2 each, then Astra callers), stamps by the process, four oracles. Decisive = Astra's threshold: every treated run accepted, matched median wall reduction ≥ 30% AND > 2σ of the native controls, no regression — on this task and on a plan-not-supplied task (Cell B).
4. Every refusal, detour or receipt defect the callers hit goes to the friction ledger with exact text; fix; repeat from 2.
5. Stop only when the threshold holds on two consecutive reruns and the ledger has no open paper cut for the verb.

Native controls so far (Cell A, Opus, 4 concurrent): 331 s and 393 s to all four oracles. Tool arms (current surface): moves complete at ~660 s and ~750 s, callers/oracle pending.
