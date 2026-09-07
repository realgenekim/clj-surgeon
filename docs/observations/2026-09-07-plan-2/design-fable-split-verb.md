# Design proposal (Fable): nail the namespace-split use case — one lever, one receipt

## Why the current surface loses (from Cell A)
The model already owns the general method (parse → analyse → generate → rewrite). The tool's extraction verbs work one destination at a time with guarded transactions, and no verb returns the analysis the model needs before it can order the moves. So the tool arm pays the analysis natively AND pays N guarded round trips; the native arm pays the analysis once and executes once.

## Two additions, both whole-intent

### 1. `split_plan` (read; inspect_clojure mode or its own tool)
Input: workspace_root; source namespace file; a mapping `{destination-ns -> [form-names]}` covering every top-level form (or `:rest -> ns` for the remainder); optional architecture rules (allowed edges by layer, e.g. foundations/public) ; optional alias policy for callers.
Output (one receipt, ~1 s): the full dependency graph between destinations (edges with the referencing form and the referenced var); cycle verdict with the offending edges; forward references inside each destination (where the monolith's declares must survive); the promotion list (private forms referenced across destinations, with their referencing sites); the require/alias block each destination needs (external libs by actual usage, imports by class usage) and the require blocks every caller needs; the caller site table (file, line, owner form, old alias, new namespace) across admitted roots — resolved through analysis, not string match; unmapped forms; a content hash of every input file (the guard for step 2); rule violations if rules were given. No write.

### 2. `split_namespace` (write; one transaction)
Input: the `split_plan` receipt hash (or the same mapping; the verb recomputes and refuses on drift), decisions for each promotion (promote / keep-private-and-duplicate never / refuse), ordering policy (source order default), intra-destination reference style (`:refer` default = byte-identical bodies; or aliases), caller alias policy, delete-source true/false, verify profile.
Effect: creates every destination file (forms in source order, attached comments travel, generated ns forms), rewrites every caller (alias + require block regenerated), deletes the source, promotes exactly the decided privates, keeps the monolith's declares where a forward reference survives, and refuses BEFORE any write on: cycles, an unmapped form, a caller outside admitted roots, a promotion left undecided, or input drift. One receipt: files created, forms moved per file (count and names), sites rewritten per caller, promotions, declares kept/dropped, read-back hashes, undo receipt, and `verification` per profile (fast lane or the repo's own command) with honest `verification_complete` semantics (write proof vs semantic proof stated separately).

## What not to build
No per-destination loop verb (that is what lost). No caller-side ordering step (the plan orders). No new refusal that needs a second round trip when the plan already carried the answer (every refusal names the exact edge/form/site and composes the corrected mapping as next_call). No routing plate change until the rerun shows the win.

## Acceptance for "nailed"
Rerun Cell A on the rebuilt 7906 with the same plan supplied: tool arm passes all four oracles; wall to oracle under 90 s for two of two runs (prediction: ~5 s plan + ~5 s split + 22 s suite + caller overhead); native controls unchanged at 331/393 s. Then Cell B (Astra's rule, plan NOT supplied) to test whether `split_plan` also removes the discovery minutes.

## Batches for Astra
1. `split_plan` read with witnesses on a fixture and on the real views.clj plan (edges, cycles, promotions, caller table match the two native arms' computed graphs — those are the oracle for the analysis).
2. `split_namespace` write with refusal-before-write witnesses (cycle, unmapped, drift, undecided promotion) and a real-repo witness that reproduces Cell A's oracle on the views worktree base d9205abc.
3. Rebuild 7906, rerun T3/T4 (Opus) and Astra-caller arms; report.
