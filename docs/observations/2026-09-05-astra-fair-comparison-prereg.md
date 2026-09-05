# Astra fair comparison preregistration — 2026-09-05

Status: **started at 2026-09-05T15:04Z; no timed task arm has been accepted yet.**

This is the four-hour response to Gene’s directive: compare a bounded semantic intention against a native route while charging both routes for the complete task. The first task family is helper extraction: identify a bounded helper set, discover its callers, apply the coordinated change, prove the result, and leave an accepted tree.

## Arms and subject

Each paired run starts from the same fresh source fixture and prompt. The native arm may use `rg`, readers, scripts and `apply_patch`; the Surgeon arm may use the public `helper_extraction` tool and the same native tools. Neither prompt mandates a particular sequence. The tool server is a separate, attested process. Model cohorts are Sol and Astra, reported separately and never pooled.

The subject attestation records source commit, fixture hash, served server identity, model, prompt hash, runner hash and start clock before the first orientation action. Any mismatch, missing output, or incomplete call invalidates that arm and remains in the ledger.

## Sample and order

There are six accepted native controls per model before a comparison claim. Runs alternate in mirrored serial order (`N1 T1 T2 N2 N3 T3 ...`), with no cross-arm CPU contention. A control is accepted only when the independent behavior suite passes and the frozen diff is complete. A failed or refused arm is retained and is not silently replaced.

## Clock and accounting

The primary wall clock starts when the driver receives the task and ends after the accepted proof and final artifact freeze. It includes orientation, discovery, model returns, tool transport, edits, tests, proof, rollback/recovery work and artifact capture. Server startup is included in the headline; a separate startup-excluded column is descriptive only. The watcher is the counting authority for model returns, tool calls and native patch calls. Model returns and wall are reported separately.

The predicted direction is modest: Surgeon can win only when one semantic request removes enough locating, repeated hunk construction and post-write checking to exceed its own orientation, refusal and proof burden. A 10× result is not assumed. The falsifier is a paired median below native or a confidence interval crossing parity after all costs are included.

## Acceptance and review

The arm-independent suite decides correctness and is never averaged into speed. Both judges receive blind frozen artifacts and write a ruling for every disputed pattern. The report will contain paired raw rows, medians, variance floor, refusals by type, adoption, startup-inclusive and startup-excluded clocks, model returns, tool calls, native patch calls, and receipt paths. No ratio is written from a partial cohort.

The meter was validated before launch with `make anvil-arms-self-test`: **389 passed, 0 failed**. This proves the attestation, watcher, scorer, mirrored ordering, abort handling and cleanup apparatus; it is not a task result.

Next action: provision the fresh fixture and two model prompts, run the six native controls first, then interleave the tool arms under the same receipt contract. If the first accepted control cannot establish the subject or the tool call is not observable, stop and repair the apparatus rather than score the arm.
