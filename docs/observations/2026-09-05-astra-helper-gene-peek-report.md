**1. Headline.** **57.1× smaller request for a verified 30-file refactor**, and an actual Astra caller successfully used the new public operation—but this four-hour window produced **no valid new speedup versus native**.

Events to the contrary: our fixture initially contained contaminating client configuration; our oracle then rejected legitimate native namespace cleanup. The comparison stopped. Schema strictness and final feature gates remain release work. [Evidence ledger](/var/tmp/forge/astra-program/repo/docs/observations/evidence/astra-helper-2026-09-05/report.json).

**2. Wins vs native.**

| Task | Native | Tool | Ratio | Correctness / n | Receipt |
|---|---:|---:|---|---|---|
| **NONE MEASURED this window** | — | — | — | Zero accepted calibration controls; cohort stopped | [Ruling](/var/tmp/forge/astra-program/repo/docs/observations/evidence/astra-helper-2026-09-05/root-oracle-ruling.md) |
| Earlier 21-owner migration, Sol |117.85 s median|34.40 s median|3.319× median paired wall ratio|Six pairs; all 12 accepted|[Prior study](/var/tmp/forge/astra-program/repo/docs/observations/evidence/astra-primary-2026-09-05/report.md)|
| Earlier same migration, Astra |56.0 s median|45.1 s median|1.240× median paired wall ratio|Six pairs; all 12 accepted|[Prior study](/var/tmp/forge/astra-program/repo/docs/observations/evidence/astra-primary-2026-09-05/report.md)|

Earlier clocks exclude server startup and external acceptance. These are prior results, not gains newly established by the helper operation.

**Surgeon API before/after—not versus native:**

| Same helper-extraction intent | Before | After | What this establishes |
|---|---:|---:|---|
| Canonical UTF-8 JSON arguments |23,193 B|406 B|57.1256× smaller request; no wall/token speedup inferred|

The earlier 37,300 B number was a pretty-printed file, not the comparable encoding. [Size receipt](/var/tmp/forge/astra-helper-program/readiness/api-argument-size-comparison.json).

**3. Losses vs native.**

| Task | Native | Tool | Ratio | Correctness / n | Receipt |
|---|---:|---:|---|---|---|
| **NONE ESTABLISHED this window** | — | — | No valid ratio | Failed calibration and separate usability witness cannot supply a loss comparison | [Ruling](/var/tmp/forge/astra-program/repo/docs/observations/evidence/astra-helper-2026-09-05/root-oracle-ruling.md) |
| Earlier Astra migration pair2 |49.7 s|50.3 s|0.988×|One retained pair, both accepted; reversal within noise|[Prior study](/var/tmp/forge/astra-program/repo/docs/observations/evidence/astra-primary-2026-09-05/report.md)|
| Earlier Astra migration including startup |56.0 s median|54.94 s median|1.021× paired|Six pairs; effectively near a tie|[Prior study](/var/tmp/forge/astra-program/repo/docs/observations/evidence/astra-primary-2026-09-05/report.md)|

The new native actor took 128.1 s and passed all 28 caller checks, but failed its mandatory proof. Its sole structural failure was removal of an unused JSON require after moving its consumer. Two independent reviewers found the checker stricter than the task. Supplemental 24-case behavior passed on the unchanged result; this does not retroactively supply a successful actor proof. Each stopped epoch retains its failure and 14 unstarted rows.

**4. Exactly what the win is.** The LLM now names six helpers, destination, scope and proof; Surgeon derives the caller edits, retires the originals, writes the 30-file change and verifies it. This replaces the old 85-change request, but covers supported static references and 24 helper behavior cases—not full application compilation or arbitrary dynamic Clojure.

The actual caller used five outer actions and two helper attempts. A directory/glob mismatch caused an 8.89 s refusal; the corrected call committed and proved in 24.13 s, both server-reported operation times. The whole actor took 86.1 s. The 109.85 s parent envelope also includes startup, adapter overhead, shutdown, external proof and inventory checks. These are descriptive usability clocks, not controlled comparison evidence. [Usability review](/var/tmp/forge/astra-program/repo/docs/observations/evidence/astra-helper-2026-09-05/usability-review.md).

**5. Surprises.**

- **Three false-green rollback-witness episodes** required correction; observing a real write before testing restoration matters more than a green label.
- **One legitimate native cleanup** exposed an oracle that favored the tool’s output shape.
- **38.2 s before the first helper attempt:** the caller reread instructions/source and rediscovered references the operation already computes.
- **One scope refusal:** `src` looked reasonable where the API required `src/**`; the caller recovered without assistance.
- **Missing helper telemetry:** direct retained receipts showed calls that the default service view and operation-key collector missed.

[Captain’s log](/var/tmp/forge/astra-program/repo/docs/observations/2026-09-05-captains-log-astra-helper-program.md), [hour-three study](/var/tmp/forge/astra-program/repo/docs/observations/2026-09-05-astra-helper-hour-3-agent-usage.md), [usability review](/var/tmp/forge/astra-program/repo/docs/observations/evidence/astra-helper-2026-09-05/usability-review.md).

**6. Learnings crystallized.**

- Witness the changed state before claiming rollback: [testing guidelines](/var/tmp/forge/astra-program/repo/docs/testing-guidelines.md), commit 074a9a9e.
- Admit every real fixture before declaring the harness ready: same guide, d4c8c21c.
- Shared acceptance must permit task-valid alternatives; preserve failed evidence when correcting an oracle: same guide, 182aa54f.
- Give both routes bounded failure predicates and an evidence handle; “fail” alone prevented diagnosis here: same guide, 182aa54f.
- Keep startup, complete RPC clocks, wrapper attempts and missing telemetry distinct: [hour-three study](/var/tmp/forge/astra-program/repo/docs/observations/2026-09-05-astra-helper-hour-3-agent-usage.md), 5e1ed51a, with its receipt-path correction in 182aa54f.

**7. Best news / worst news.** Best: a real LLM completed the compact public operation with an in-transaction proof and independent after-return acceptance. Worst: I did not deliver the planned replicated performance comparison. **Top win:** verified intent replaces a large edit specification. **Top loss:** preparation and oracle mistakes reached the actual actor entrance too late. I should have exercised that entrance earlier while implementation proceeded.

**8. Board — September 5, 1:56 a.m. Pacific, at window close.**

| Lane | One next action | When |
|---|---|---|
| Fable product | Freeze v5 and finish its exact-source gates | After 2 a.m.; pending |
| Next experiment | Version the narrow namespace/proof-diagnostic correction, then establish native controls before optimizing the tool | Next authorized cohort; current comparison closed |
| Interface | Clarify directory versus glob authority and test whether complete requests can skip redundant discovery | Next design; no silent scope broadening |
| Telemetry | Emit helper outcomes through the existing service emitter and cover refusal envelopes | Follow-up; collector repair branch remains review-only |

Public `main` stays frozen. The actual LLM used 05df; its green full gate ran at source-equivalent docs commit a3caddec. Fable’s new eddbe6fd candidate includes stricter schema, a mapper correction and standard test-lane enrollment. Its 68 focused tests / 1,137 assertions, 581 fast tests / 7,299 assertions, and independently reviewed public positive/rollback paths pass. Sol r4 remains GO-WITH-FIX for schema permissiveness; v5 schema work, battery and full gate are pending. It is not shipped by this report.

**9. Decisions waiting on Gene.** None required to continue the already authorized branch work. No merge, deployment or broader benchmark approval is requested here.

**10. Answers to your questions.** **Project: 7/10. This window’s execution: 5/10.** The useful square is grounded in working code and prior repeated gains; sustained 10× whole-task performance remains unproved. The request collapse is real, but I missed the comparison milestone. In a further two hours, the concrete target would be the remaining product gates, a corrected oracle with negative witnesses, then six native controls and a paired screen if acceptance and time permit.

**Yes, I am Astra:** the root session and both actual callers record gpt-6-astra. Stronger native models materially change the contest; prior Sol 3.32× versus Astra 1.24× is why routing should depend on model, task, proof and server readiness.

**The historical lightning was real, but not 100× complete-task speed:** approximately 1.3 s hand-driven mechanics versus agents reaching the move in 141–152 s; “53” was churn, not a verified site count. SMW’s five-minute orientation was not measured here. [Historical audit](/var/tmp/forge/astra-program/repo/docs/observations/2026-09-04-captains-log-astra-four-hour-program.md).

**The next tool should accept a complete, bounded intent and return a verified result with truthful coverage and actionable failures.** Today we demonstrated that interface with an actual caller. To pursue 10×, target tasks dominated by discovery, binding analysis and proof coordination; multiplying files alone is insufficient. My advice to Fable is to finish the current correctness contract, improve scope/failure clarity, then test reduced caller preparation with fresh controls—not infer speed from the smaller request.
