# Astra: the stronger single-JVM cold comparator

Recorded 2026-09-06T09:58:04.633110+00:00

The same candidate gate and witness passed in one fresh JVM. Complete external
command wall was **2.611796 seconds**, versus the earlier warm source transition's
**0.865773 seconds**. This is a **3.02× component ratio in one unreplicated
observation**, with small differences in external watchdog overhead. The former
5.07× ratio remains specific to the weaker two-JVM cold path.

| Boundary | Observed wall |
|---|---:|
| One fresh JVM: startup and both proofs | 2.189234 s |
| Cold child wrapper through cleanup | 2.522742 s |
| Complete cold parent command | 2.611796 s |
| Earlier warm transition command | 0.865773 s |
| Warm startup plus transition | 4.294237 s |
| Warm startup, transition and shutdown | 4.761209 s |

The inner 2.189-second timer is not interchangeable with whole-command timing.
Cold wins for an isolated use. Batching helps either editing arm, so this
improvement cannot all be credited to Surgeon. Repeated genuine transitions,
voluntary adoption and complete user-task economics remain unmeasured here.

The original cold gate exits the JVM after success. The prepared comparator
pins that original text and reproduces its exact require/run-tests operation
without the success exit, then loads the original witness unchanged. Gate
counts are two tests / five assertions; witness counts are four / 24. Both ran
once and passed. A separate reviewer verified the retained output, eleven
workspace files and modes, seventy dependency/program hashes, removal of the
private HOME/tmp directory, and empty owned-process cleanup receipts.

Before execution, independent review caught final validation occurring before
cleanup. A successor moved validation after cleanup, refused unexpected
survivors, and retained a receipt on process-exit races: faithful RED then nine
offline checks passing. The external watchdog verifies its cleanup helper
before executing the same hashed bytes, reserves one attempt, and bounds work
to 32 seconds plus three seconds of cleanup allowance. No retry occurred.

This is a fresh-process comparison, not a cold filesystem-cache experiment.
The frozen trusted proof uses the existing socket-denial wrapper; it is not
an arbitrary-program containment claim. Original warm and two-JVM evidence
was preserved. Root personally invoked the one-JVM command after Fable's
explicit timing handoff, and independent review performed no timing rerun.

Evidence: `/var/tmp/forge/astra-cold-batch-v2-fx/parent-attempt/status.json`,
`run-01/result.json`, `run-01/stdout`, and `independent-outcome.md`.
