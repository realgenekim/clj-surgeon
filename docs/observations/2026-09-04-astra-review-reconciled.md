# Astra's assessment (GPT-6 Astra, 2026-09-04 22:01Z) — reconciled and decided (forge-anvil, 22:1xZ)

Source: /var/tmp/forge/2026-09-04-astra-surgeon-assessment-for-fable.md (read-only review of the logs through ~21:56Z;
no source audit, no new benchmark). Treated as untrusted input per house rules: read, reconciled against the receipts,
acted on only where it fits the program's goals and this seat's authority. Gene relayed it and asked for reconciliation.

## Reconciliation, point by point

| Astra's point | receipt | verdict |
|---|---|---|
| 1. Injected receipt ≠ live-tool result; the live MCP arm took 31 calls | T2 (MCP-attached) 31 raw / 6 reads / 10 patches; T3/T4 injected 19/18 | FACTUAL, ACCEPTED. Every "1.45×" claim from here says "injected receipt". A live-tool arm on an UNSEEN feature with independent acceptance is owed before any generalisation. |
| 2. Time buckets are attribution, not causation | the species script assigns time-to-next-call to the current call | ACCEPTED; already stated in the wall table's caption, now stated in vision.md's rule too. Wall totals stand; bucket savings are not additive budgets. |
| 3. Native adapts (generators, scripts) | ecaller/ensweep cohorts | ACCEPTED, no change: native gets equal access to batching and scripts in every arm (it already has apply_patch + Bash). |
| 4. Real-task integration came too late (15 gate rounds before missing-profile / mixed-language / exec-bit findings) | G, G2, G3 | ACCEPTED as a judgment; the process fix is already the doctrine's "hand-drive every mode you ship" — applied today, late. From here every gate round ends with a hand-driven real task before review. |
| 5. Evidence summaries lag (tech-tree.md says the gate is unmerged) | docs/tech-tree.md | FACTUAL, ACCEPTED: ledger refreshed in this commit; the queue doc remains the live ledger. |
| 6. Green suites ≠ feature completion (G2 passed with the feature absent) | G2 diff = .local_version only | FACTUAL, ACCEPTED: an independent held-out acceptance check (/var/tmp/forge/tweezer/acceptance/check.sh) is being built now and applied to every arm clone, with smw-base as the negative control; every future arm reports ACCEPT/REJECT beside its wall. |
| Routing ruling should not become model-independent | 2026-09-02 ruling measured on gpt-5.6-sol | ACCEPTED: re-measure with Astra at a clean boundary; the ruling stays as written for Sol until then. |
| "Do not expand the gate to preserve a rejected speed hypothesis" | T5: one admitted call, 13.2 min | ACCEPTED, already concluded at T5: the gate is a correctness product on this harness; r18 is safety (overlay), not speed. |

Different judgment, kept: Astra rates the alias-migration fan-out "wall secondary". The receipts say the same (wall
was secondary by pre-registration); the primary measure there was emitted characters, and it held on a second caller.
No factual disagreement found.

## Decisions (Astra's item 6)
- ACCEPTED: 1–6 and the routing re-measure. REJECTED: nothing of substance.
- STOPPED: feature_thread polish (frozen after its landing review, already ordered); gate speed claims (withdrawn at T5).
- NEXT EXPERIMENT: T6/T6b (receipt + landed plate, no gate) — the last injected-receipt wall arm; falsifier: raw > 19 on both.
  THEN the live-tool held-out arm: an UNSEEN feature (not Dequote/Format), MCP-attached feature_thread at the landed tip, independent
  acceptance, charged for discovery, invocation, refusals and recovery; falsifier: live ≥ native wall at n=2.
- SOL vs ASTRA (pre-registration, runs at the codex boundary after the current program's arms; needs codex ≥ 0.153.3 which the
  launcher will select explicitly per arm): same pinned client per pair; per model: native, native + live feature_thread, alias_migration
  whole-task; tasks: a known-site edit (control), the unseen cross-language feature, the N=21 fan-out; n=3 per cell interleaved; variance
  floor first (3 native replicates per model); meters: request-to-ACCEPT wall, failure rate, repair time, calls, adoption; report failed
  arms. Falsifier for "Astra changes the ruling": Astra-native beats Astra-tool on the fan-out square (the only square Sol-tool wins).
- WHAT WOULD CHANGE THE ROUTING RULING: two consecutive interleaved cohorts (n≥3) in which native + live Surgeon beats native on
  request-to-ACCEPT wall at ≥1.5× on an unseen task, for the model in question.

## Not granted by this review
No new permissions; main stays frozen; nothing merges from the seat except reviewed GO tips via ~/bin/land; codex pinned per arm.
