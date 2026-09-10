Written 2026-09-10T03:31:19.137243+00:00.

**Take (e): finish the ref split at the records publication entrance.** Make a records-only publication to `records/MCP-main` independent of the code ship lease, and route tighten's record publishers through that entrance. This is the best evidenced seconds-per-build-hour purchase in the four-hour block. Expected saving: **600–660 seconds per publication currently blocked near the observed median**, central **630 seconds**, for at most four builder-hours: **150–165 seconds saved per eligible publication per build-hour**. These are forecasts from measured waits, not an executed treatment result.

**Number to beat: 660 seconds of reported lease wait. Acceptance target: zero code-lease wait, ≤30 seconds from publication request to independently confirmed records visibility on the registered local fixture, and ≥600 seconds improvement in the matched 660-second-lease comparison.** Actual network publication receives its own measured wall. Expected reduction in the historical 594-second code ship: **0 seconds**. Do not call this a 630-second ship acceleration.

That distinction is why I would take this small operational gain while retaining the frontiers ruling: keep the complete gate, fund a better review entrance as the next substantive product experiment, and stop warm-proof variants. The user explicitly included records publication among the candidates. It has a directly removable wait backed by an already decided separation of refs; the review alternatives still need an experiment to establish how much work disappears. This recommendation buys faster delivery of the findings and receipts that close the tighten loop. It does not claim that a background publisher's wait was all active operator time, or that it delayed every build.

This is a read-only consult, not implementation, qualification, installation or publication authority. Checkout: `/home/forge/src/clj-surgeon-astra-consult`, clean at `3ea3803ea2e303a961a3edc1d76b8095ab5ff4f7`, identical to the local `origin/MCP/main`; no fetch or checkout was performed. Records were read on `records/MCP-main`, observed at `e0dcb106de3c1d84399fd07478846cfb171157ed`; another worker's untracked report was left alone. No tests, analyzers, JVMs, reviewers, ships, daily jobs or pushes were launched. The only output written is this file. The 60-minute consult timebox began at the clock reading `2026-09-10 03:22:49 UTC`.

**What the retained clocks actually establish**

I extracted terminal `SHIP status=` rows from `/var/tmp/forge/run-bg/ship-*.log`, and `RECORDS-PUSH` rows from `/var/tmp/forge/run-bg/records-push-*.log`. The latter population has 21 files with one terminal row each. All report OK; this glob is a retained-log population, not an independently complete census of all publication requests.

| Retained population / specimen | Observation | What it licenses |
|---|---:|---|
| Records-push, 21 terminal rows | Wait median **660 s**, range **0–1,500 s**; 20 positive waits | There is repeated recorded blocking in the publication entrance. |
| Those 21 rows | Sum of reported waits **14,340 s** | Aggregate waiter-seconds only. Concurrent waiters may overlap; this is not four hours of recoverable calendar time. |
| Item 3, 17 ship attempts | Review sum **14,707 s**, median **813 s**, range **453–1,519 s**; all HOLD / no-go | Review controls these unsuccessful attempts. There is no accepted item-3 ship in this population. |
| Item 3 prewarm | Median **315 s**, range **315–345 s** | Faster prewarm alone does not shorten these review-bound terminal attempts. |
| Item 1, three ship attempts | Review **543 / 528 / 648 s**, all HOLD | Same controlling lane, but different tasks; not controls for brief shape. |
| Successful gate-lanes ship, `20260909T080543Z-2dfbc286fed9` | Ship **594 s**, review **573 s**, battery **165 s**, observed prewarm **316 s**, historical `gates=consumed` | Useful historical timeline, not the current v3.7 consumption baseline. |
| Clean trunk full gate on `3ea3803e` | **175 s** | An isolated full gate observation, not a matched isolated ship-prewarm arm. |

The records waits, in filename order, are: `item1=450`, `item3r2=660`, `item3r5=960`, `items=600`, `parallelism=0`, `park=510`, `r10=930`, `r11=840`, `r12=930`, `r13=1050`, `r14=570`, `r15=510`, `r16=450`, `r17=1500`, `r3=450`, `r6=780`, `r7=720`, `r8=1020`, `r9=660`, `v36=180`, `verdicts=570`, all seconds. In particular, the supplied 450–1,020-second range omits retained 1,050- and 1,500-second waits. The helper increments `waited` after each nominal 30-second sleep; it is not an independently measured complete elapsed time. The new experiment must measure the latter.

The successful ship's [event journal](/var/tmp/forge/ship/20260909T080543Z-2dfbc286fed9/events.log) resolves the dependency order: launch 08:05:43, review and battery start 08:05:46, prewarm start 08:05:48, prewarm observed green 08:11:04, review exit 08:15:19, phase B complete 08:15:24, remote publication confirmed 08:15:34, fence cleanup complete 08:15:36. The [terminal log](/var/tmp/forge/run-bg/ship-gate-lanes-6.log:42) reports 594 seconds; journal stamps have one-second precision. The review controlled the join.

The [raw prewarm log](/var/tmp/forge/run-bg/ship-20260909T080543Z-2dfbc286fed9-fastlane-1.log) adds a useful correction: coordinator wall **297.702 s**, 08:05:53.544–08:10:51.249, with classpath preparation **117.839 s**. Ship reports **316 s at observation**, not the exact child termination duration: its default poll is 15 seconds and `fl_record` timestamps observation. Preparation runs through worker admission, so this stage can include waiting as well as the `clojure -Spath` command. The records do not separately identify those components. Do not attribute all of the 316−175 difference to CPU contention or claim all 118 preparation seconds are eliminable.

The [gate round-3 report](/home/forge/src/clj-surgeon-records/docs/observations/2026-09-09-gate-lanes/astra-round-3.md) measured two simultaneous complete entrances at **221.447 / 269.672 s**, with aggregate worker occupancy bounded at eight. That is evidence of contention under an honest bound, not evidence for a particular faster schedule. The [frontiers consult](/var/tmp/forge/plan2/cellC/astra-frontiers-report.md) also explains why the historical consumed landing must not substitute for current custody-qualified reuse: v3.7's safe answer is run-required when observation is unqualified.

**Comparison of the six candidates**

All build-hour and saving estimates below are prospective. The seconds have different endpoints deliberately named; they are not additive.

| Candidate | Measured constraint and expected useful saving | Four-hour decision |
|---|---|---|
| **(a) Battery/prewarm scheduling** | On the historical successful ship, `max(review 573, prewarm 316, battery 165)` is review. Simple battery-first arithmetic gives roughly `165+175=340 s`, still below review, but potentially later than the existing 316-second prewarm observation. Those component numbers are not matched arms. Width splitting may reduce queueing or accelerate review's local probes, but no measured review effect is retained. **0 s demonstrated off that ship; central 0 forecast.** | Defer. Keeping the bound while changing orchestration is possible without editing capacity code, but a width competition is an experiment, not a known gain. It must measure first-red as well as accepted completion; delaying early detection can lose even if the clean join is unchanged. |
| **(b) Fence brief shape** | Item-3 review median **813 s**; 17 reviews cost **14,707 s**. A bounded current-contract brief could plausibly save **60–150 s per eligible review**, with a real possibility of zero. No recorded matched brief-only treatment proves that estimate. At four hours, roughly **15–37.5 s per review per build-hour**, before qualification costs. | Second choice for a ship-only objective; retain the review entrance as the next product investment. A rigorous review experiment is less likely to finish qualification in this one block than the records interlock. |
| **(c) Kondo cache keyed per tree** | No duplicate identical full-tree kondo analysis was found in the automated clean ship entrance on this trunk. Ship's explicit lint is in the auto-fix branch on changed files; admission deliberately analyzes before and after images. The unlanded preservation implementation runs once per tree; its retained example records **5.915 s for both trees**, inside **36.167 s** checker wall. | **0 s established on the current clean ship path.** Repeated base trees in a future replay may benefit, but that is another population. A valid cache key also needs analyzer/config/features/roots/environment inputs; a Git tree SHA alone is insufficient. Keep lint through `~/bin/clj-kondo`. |
| **(d) Battery floor** | [Floor report](/var/tmp/forge/plan2/cellC/astra-battery-floor-report.md): longest indivisible remaining Var **136.888 s**, measured longest lane **169.502 s**, complete gate **172 s** in that run; later ship battery **165 s**. Without splitting that Var, `165−136.888≈28 s` is an optimistic packing headroom, excluding overhead. | **0 s off the review-bound reference**; another split could lower the floor, but no current measured complete-ship gain supports it. Avoid the parallel builder's capacity surface. |
| **(e) Records publication independent of code lease** | **660 s median reported wait**, mechanically caused by a loop that remains after the refs separated. Forecast **600–660 s saved per currently blocked median publication**, **0 s claimed off ship**. | **Take this one.** Four hours can contain a faithful red, narrow repair, real Git boundary matrix, fresh controls, independent review and a later publication witness. |
| **(f) Remaining gate shell chain / other consult ideas** | [Round 2](/home/forge/src/clj-surgeon-records/docs/observations/2026-09-09-gate-lanes/astra-round-2.md:187) has fast wave **25.903 s**, shell sequence **120.725 s**, authority work **14.357 s**: **160.985 s** before other overhead. The alias namespace is **72.486 s**. Perfectly reducing the shell chain to that floor suggests roughly **48 s gate headroom**, not a proved safe parallelization. Warm review checkout was only a **0–30 s** forecast; carried review authority needs qualification. | None beats the directly observed records wait within four hours. No gate omission, warmer proof variant, new preservation checker, or review-authority reuse in this block. |

The battery floor split is real work left to do, not an assertion that the battery is optimal. The scheduling row likewise does not deny contention. They lose this allocation because the controlling endpoint and a witnessed removable term matter more than the largest-looking component duration.

**What is wrong with the review brief, without pretending to know its cost**

Archived item-3 briefs are **1,450–1,770 words**, not a steadily growing wall of prose: round 1 is 1,450, round 6 is 1,770, round 16 is 1,652 and round 17 is 1,624. But the requested historical work grows: round 2 says “re-run your two probes”; round 17 says “re-run your sixteen probes and five controls.” Each inspected sealed brief also contains the GO-WITH-FIX instruction block **twice**; the last copy alone is about **520 words**. Round 17 still carries the original 141/141 and 64/64 preservation claims while its current paragraph says 99/141 and 32/64. These are concrete entrance defects.

The [round-17 verdict](/var/tmp/forge/ship/20260910T014707Z-10b77576307c/verdict-1.md) confirms that the historical probes actually ran. It also records a useful new blocking finding: blank required explanation cells escaped the oracle. The preceding [round-16 ship](/var/tmp/forge/run-bg/ship-item3-16.log:20) took **453 s** of review; round 17 took **1,519 s** despite a slightly shorter brief and a delta of **+61/−8** in one test script. Brief word count cannot explain this variation. The retained summary clocks do not partition reconstruction, fixture execution, new attack development, model reasoning and queueing. **The fraction attributable to historical reproduction is unknown.** It would be false to label 1,066 seconds, or most of 813 seconds, “brief overhead.”

The rule I would test later is: one current sealed candidate/base and current acceptance contract; one exact delta; one named executable regression command covering the accumulated positives and negatives on the final candidate; one section of still-open decisions; one verdict/fix protocol. Historical narratives become linked evidence, not repeated work orders. A reviewer still runs the complete relevant regression corpus, inspects changed oracle/implementation seams and performs independent adversarial scrutiny; a changed or missing test/receipt invalidates any reuse. Deduplicating prose is harmless housekeeping, but saved review execution would have to come from eliminating reconstruction or duplicate runs. It cannot come from declaring prior GO sufficient. No such prior-authority consumer is part of the chosen block.

**The one change, and its necessary delivery boundary**

The current [records-push](/home/forge/bin/records-push) is only 1,487 bytes. It defaults `DEST=records/MCP-main`, pulls/rebases that ref, then waits while any file in the ship lease directory names a live PID, and finally pushes `HEAD:$DEST`. Its own comment explains the old reason: records advancing the *same* ref during code landing rejected a green landing. [Gene's 01:34Z decision 1a](/home/forge/src/clj-surgeon-records/docs/observations/2026-09-03-captains-log-anvil-seat.md:4480) moved records to a separate ref. Waiting on the code lease is now broader than that protection requires.

Implement **one ref-scoped records publication contract**, not a general scheduler:

- Bind the approved records repository/remote, exact destination `refs/heads/records/MCP-main`, intended record commit/artifact set and source checkout identity. Refuse `MCP/main`, public `main`, another ref, or an unrelated pending candidate commit before mutation. The current `RECORDS_DEST` override must not silently bypass this boundary. Validate only the pending publication against the approved artifact set; do not reinterpret the historical records ancestry as a new patch or discard existing records.
- Serialize publishers that share the **records checkout/ref** across refresh, rebase, tip selection, push and readback. Hold no code ship lease and leave all of ship's ownership/gate/publication checks intact. Independent records writers still need their own concurrency discipline; merely deleting `sleep` is not a complete publisher.
- Publish the pinned, validated tip with an explicit refspec. Preserve rebase conflicts and rejected/ambiguous pushes as failed or unverified attempts; no force push and no success on a missing readback. Confirm the intended record blob/hash is reachable from the remote records ref, including if another valid records writer advanced it. Preserve failed attempts in the receipt.
- Route tighten's publication adapters to this same entrance and correct the corresponding binding/delivery copies. Keep the behavior of their corpora, canary, sentinel and grades unchanged.

The last point is required by source inspection, not speculative scope expansion. Installed [seat-receipt](/home/forge/bin/seat-receipt:235), [canary-cell](/home/forge/bin/canary-cell:133) and [verb-sentinel](/home/forge/bin/verb-sentinel:93) still pull and push `MCP/main` directly. Installed [tighten's binding text](/home/forge/bin/tighten:150) also advertises the old destination. Therefore deleting the wait in `records-push` alone would accelerate manual records-push calls while leaving the named tighten workflow outside that repair. Correct these adapters as consumers of the same publication contract; do not expand into unrelated instrument repair. Their existing wrong-destination path is a negative qualification case, **not a valid slower performance control**. The timed positive control below is the existing records-push helper on its already correct records destination.

This finding also means **do not launch the uncorrected default daily publication path as the opening measurement**. Preserve the finding for the instrument owner and test isolated copies. This consult has invoked none of those writes, and has sent no messages or alarms to other seats.

The [bundle manifest](/home/forge/src/claude-skills-nrepl/tighten-the-loop/bin/MANIFEST.txt) says `/home/forge/bin` is authoritative on this seat and package files are delivery copies. `records-push` is not currently in that bundle; bind and deliver the helper explicitly if the corrected callers depend on it. Do not count editing a package copy as installed uptake. No edits are needed in `test/gate_slot.py`, battery capacity code, or the Makefile lock lines. The code gate and Opus's skiff work are outside the candidate's write set.

**Failing witness, then exact before/after measurement**

Proposed finding ID: `SQUEEZE-RECORDS-REF-LEASE-001` (a report-local proposal, not an invented inbox entry). Intent anchor: Gene's decision 1a plus SEAT-RECEIPT's approved-ref/consumer-visibility contract.

The independent recorder freezes a private local bare origin containing distinct `main`, `MCP/main` and `records/MCP-main` refs, a records checkout with one intended pending record commit, an independent readback checkout, and a private lease directory. Use a retained real report as the publication payload, with its bytes/hash sealed before builder work. A real live helper process owns a `pid=<PID>` lease record in the exact shape the installed script reads; it represents a code ship and touches no production lease. Predeclare the invariant: **the record becomes visible on the records ref while that other-ref lease is still live, and the two code refs do not move**.

The first red executes the frozen installed helper with its existing argv/environment contract:

```sh
env RECORDS_REPO="$fixture_records" \
    RECORDS_DEST=records/MCP-main \
    RECORDS_LEASE_DIR="$fixture_code_lease" \
    RECORDS_PUSH_POLL=1 RECORDS_PUSH_WAIT=3 \
    /home/forge/bin/records-push
```

All variables above are future recorder-bound fixture paths, not current task commands. Keep the lease holder alive for more than the three-second wait cap. Expected **old failure**: exit 4, `HELD reason=ship-lease-live waited=3s`, intended blob absent remotely. Expected repaired behavior: valid publication and independent readback while the holder remains alive. Capture actual outcomes before modifying code; this report has not executed the red. The small cap makes the behavioral red cheap; it is **not** the 660-second performance measurement.

At the production entrance, exercise old and candidate against the same frozen matrix: ordinary records success; live code lease; dead lease; two concurrent records publishers; a code ref advancing concurrently; wrong repository/root; forbidden destination; unrelated pending code commit; dirty/conflicting records checkout; rejected push; push/readback disagreement; failed child with retained exit 17. Include stale final candidate, intended-assertion red, pending-proof and missing-observer cases required by CHANGE-RULE wherever a wrapper claims grade/qualification. The publisher must not upgrade any of those grades simply because bytes were transported. Verify both positive delivery and unchanged code refs, with no evidence loss on failures. Record each changed verdict and its intent anchor. A fake Git executable is insufficient for the decisive ref/race/readback cases: use real Git with the private bare remote.

The recorder then uses this exact timing protocol:

1. **Freeze inputs and population.** Original helper hash below; final candidate package hash; Git version; same local host/filesystem; private fixture snapshots and record payload; exact refspec; all adapter argv/configuration; code lease lifetime **660 s**, default old poll **30 s**, wait cap **1,500 s**. Lease starts at the publication request and remains live for the registered duration even if the candidate finishes early. Record its actual start/release times. Never wait for a favorable live ship duration to choose a sample.
2. **Clock the complete publication task.** `t0`: recorder releases the publication request, before helper process startup, lock acquisition, refresh or rebase. `t1`: independent consumer obtains the intended artifact from the remote records ref and confirms its hash plus unchanged code refs. Use monotonic elapsed clocks with UTC event anchors. Record helper terminal time separately. Neither `waited=0`, local commit, exit 0 nor a Git push message is `t1`.
3. **Six identical old controls first**, using fresh copies of the same fixture, sequentially. These should expose roughly 660–690 s plus ordinary Git/readback work; poll alignment is observed, not normalized away. Retain all six and compute sample SD `s0`. They cost roughly **66–69 minutes**. Builder work may proceed beside these lightweight, isolated controls; it must not change their frozen executable or fixtures.
4. **Three fresh matched old/new pairs**, order **old/new, new/old, old/new**, after the candidate is frozen. Same fixture/payload and independently owned 660-second leases in every arm. Treatment holders may remain alive in their private directories after accepted publication; their remaining idle lifetime need not block the next independent fixture. Charge actual startup, own-ref contention, rebase, failures, repairs and consumer checks. Do not rescale the three-second red into a production performance number. Approximately another **33–35 minutes** of control wall, plus treatment runs, buys a measured result within the block.
5. **Decision arithmetic.** Publish each `Δi=T_old,i−T_new,i`, medians and range, all accepted/assigned counts, first-attempt success and every failed/censored attempt. Require full positive/negative matrix acceptance, all three eligible treatment publications completed within **30 s**, no code-lease waiting, and median `Δ≥max(600 s, 2*s0)`. Also run an idle-lease control/treatment pair to expose newly added overhead. Any correctness regression rejects the candidate regardless of time; an effect inside noise is inconclusive, not a win.
6. **Second encounter.** Schedule one already useful records publication through the final installed entrance while a real code ship is active, with an independent consumer recording request-to-visibility and actual code-ref evolution. Run only after normal owner qualification. The code ship may legitimately advance its own ref; show that the records publisher did not cause that advance. Register the intended record before release; do not manufacture a new ship for a favorable clock. If no eligible ship/publication occurs by the deadline, close with measured fixture results and `transfer=untested`, not a fabricated field gain.

The independent readback fixture measures observable functionality; a different process under the same account does **not** establish inaccessible observer custody. Disclose that limitation. Do not upgrade the v0 seat receipt to v1 `observed`, or resume evidence consumption, on this experiment's strength. Fixture performance is explicitly scoped to local Git transport; the later actual remote run is the network/delivery measurement. Its benefit is reported separately from any complete useful-work-to-accepted-task clock, where waits can overlap other work.

**Four-hour allocation and terminal receipt**

| Block time | Deliverable |
|---|---|
| 00:00–00:20 | Bind owners/scope, preserve installed bytes and triggering logs, seal cases and acceptance, execute faithful red. Record opening block and existing findings selection through supported entrances. |
| 00:20–01:30 | Recorder runs the six old controls while builder repairs only the publication entrance/adapters and their witnesses in an isolated instrument workspace. No gate load or production ref writes. |
| 01:30–02:15 | Freeze final candidate; run real Git matrix and three matched pairs. Any further repair invalidates candidate timing and must fit the remaining budget. |
| 02:15–03:15 | Independent review/qualification, production caller checks against the isolated origin, destination and artifact binding checks, correct delivery copies and fresh epoch proposal. Preserve every red/unknown. |
| 03:15–04:00 | Owner-controlled delivery if qualified, actual consumer uptake and the registered later publication if available; immutable result/manifest, block close and readback. Otherwise park the concrete candidate with actual measurements and pending obligations. |

One attempt at repairing the measurement/contract seam, then park if it cannot be qualified within the block; do not turn this into another seventeen-round checker. Performance arithmetic needs about 100 minutes of control wall, leaving substantial time for the small implementation and review. Four hours at a 630-second median saving repays builder wall after roughly **23 similarly blocked publications**, excluding maintenance; this is publication-latency accounting, not a claim of 23 avoided human interruptions.

Apply [SKILL.md](/home/forge/src/claude-skills-nrepl/tighten-the-loop/SKILL.md), [CHANGE-RULE.md](/home/forge/src/claude-skills-nrepl/tighten-the-loop/CHANGE-RULE.md) and [SEAT-RECEIPT.md](/home/forge/src/claude-skills-nrepl/tighten-the-loop/SEAT-RECEIPT.md): the builder cannot issue its own authoritative qualification, changed instrument verdicts need explicit intent anchors, failed and pending work survives, and delivery is proved at the consumer. September 9's ruling names Astra steward and an Opus one-shot recorder/acceptor, but a builder acting as Astra cannot also independently qualify this candidate; bind a separate verifier through the owner before promotion. Do not build a new observer service in this block.

The skill's installed `round` profile is explicitly **namespace-split Cell C only**. Do not pass this Bash/publication task to it and count a Cell C grade as validation. Use the existing owner-controlled instrument builder/reviewer entrance, recording the same tighten finding → red → candidate → matrix → timing → delivery chain. A generic publication round profile is not installed and is not another feature to build here. `tighten day` is only the installed prepare-day subset, not the full claimed twelve-step unit. These are concrete delivery limits, not reasons to run unrelated daily mutations during this read-only consult.

The final block result should state actual values in this shape (placeholders below are not measurements):

```text
finding=SQUEEZE-RECORDS-REF-LEASE-001
reference=<hash> candidate=<hash> scope=records-publication
red=<retained literal exit/output> matrix=<old/new evidence + independent verdict>
control_n=6 control_median_s=<measured> control_sd_s=<measured>
pairs_n=3 old_median_s=<measured> new_median_s=<measured> delta_median_s=<measured>
accepted=<n>/<assigned> failures=<n> censored=<n> code_ref_violations=<n>
lease_wait_old_s=<measured> lease_wait_new_s=<measured>
field_request_to_visibility_s=<measured or unknown-with-reason>
ship_seconds_saved=0-claimed transfer=<observed-for-named-task or untested>
build_wall_s=<measured> block_wall_s=<measured> outcome=<done|parked|killed>
pending=<explicit obligations> evidence=<immutable manifest and consumer readback>
```

For block-ledger, preserve its documented meaning of `--attempts` as refused/failed attempts separately from the all-attempt population. `none` means unmeasured wall, not zero. Report a local fixture win as such if real transfer or independent authority remains pending. The four-hour block must end with measured numbers and a bounded verdict; the clock cannot authorize a weaker gate or an unobserved success.

**Frozen source identities read for this recommendation**

| Installed input | SHA-256 |
|---|---|
| `/home/forge/bin/records-push` | `55e25d68b3300df8e0931b99d7dd7d992ee122c5df1c825f91d81075933d4907` |
| `/home/forge/bin/ship` | `e95ea5e60d130c561ed9db43924685a1bc5ca938a9d711377f073390984d9f35` |
| `/home/forge/bin/fence-run` | `cc09e618af202f4b33d662373fc6b714fbb854417fe061f43ebf6d1d1f267e98` |
| `/home/forge/bin/seat-receipt` | `2a9694eec4fd6dbc056744d40d3e26605f9b1cf94a5fd1a93f222da664076128` |
| `/home/forge/bin/canary-cell` | `b7bc9c6bddf3589e903d2010e10415808fb028bf2a8985260c7d478924675728` |
| `/home/forge/bin/verb-sentinel` | `7955d1fdc1f3c49d7e81e496a81b317cc69ebb6989cbcb7395377cd6c83cdae6` |
| `/home/forge/bin/tighten` | `ceac37f31552eff0929454a8939f1dca6b5e6dbd295d7bd0c4ef2b1b49625321` |

These bind inspection, not replay or installed qualification. The builder must recheck drift before using them as the experiment's old epoch. All candidate effects remain unmeasured at the end of this consult.
