1. The classification is directionally reasonable, but the framing is favorable to T1.

I would reclassify or clarify:

- `CLAUDE.md` is an instruction/discovery read. It may be reported separately as mandated overhead, but it remains a tool call.
- `bd prime` is discovery if it supplies task or repository context; `create/claim/close` and `.beads` writes are ceremony.
- Source reads embedded in shell/`bd` calls should be counted as read operations, not additional top-level tool calls. The analysis currently mixes these units.
- A final status check is verification; redundant status checks are overhead.
- Suite runs and every real-file patch count toward reaching a green edit.
- The synthetic receipt costs one call in any deployable comparison, even though this replay injected it into the prompt.
- Wait/poll calls belong in harness-raw totals but can be excluded from a separately named “agent decisions” metric.

With those conventions:

- Discovery reads: `10/2 = 5×` survives as an operation count.
- Core/substantive work: `21/10 = 2.1×` including receipt, or `21/9 = 2.33×` excluding it. Call this “task-core,” not “whole task.”
- Raw: `32/25 = 1.28×`, so 1.3× survives.
- “Calls before first patch: 2.2×” does not survive an all-in count: it is `13/7 = 1.86×` once the receipt is charged.

Because the independent gate rerun is pending, these are ratios to an agent-claimed green edit, not yet a measured green edit. And because this is one hand-classified pair, the call ledger and classification rules need publishing before “honest” can be independently established.

2. Tenfold cannot occur on this harness through a read-side receipt alone.

Even an ideal receipt cannot eliminate the mandatory ceremony, write, or four suite invocations. Using the stated minimum—eight ceremony calls, four suites, one receipt, and one write—the raw floor is about 14 calls, so the observed 32-call native run can improve by at most about `2.3×`. Using T1’s actual 12 ceremony calls and two patches makes the ceiling smaller.

The mechanism capable of approaching 10× is not a better document; it is a change in transaction granularity:

`receipt containing complete edit contract → one atomic apply/verify/commit call`

That call must validate anchors/SHA, apply the patch, run all gates server-side, and return structured diagnostics. Ceremony must be removed or moved inside that transaction. Any 10× comparison must also state whether the native arm receives the same atomic gate; otherwise it confounds receipt quality with privileged execution.

3. I would run these next, with paired randomized replicas under matched load:

| Arm | Prediction | Pre-registered withdrawal line |
|---|---|---|
| **R: five paired N/T1 replicas** | Median source-read reduction `3–6×`; task-core reduction `1.7–2.7×`; raw reduction `1.1–1.5×`. | Withdraw a reproducible receipt benefit if median paired source-read reduction is `<2×`, task-core ratio is `<1.3×`, or independently verified green rate is worse by more than one run out of five. |
| **T2: MCP receipt, mandatory first call** | Similar counts to T1, not dramatically better: 1 receipt, 1–3 source reads, roughly 23–27 raw calls. Its value is testing the real delivery path. | Withdraw first-call delivery as reliable if any run fails to call it first, receipt retrieval fails, or median raw count is more than two calls above T1. |
| **T3: round-three receipt with selection precedent, peer commands, and request contract** | Zero pre-patch source reads; about 8 task-core calls including receipt and roughly 22–23 raw, assuming patch/test behavior is unchanged. | Withdraw “complete edit basis” if any successful run needs an unplanned source read before writing, or if zero-read runs produce a semantic or placement defect. |
| **G: T3 plus atomic `admit_clojure_patch` apply/verify/commit** | One receipt plus one admit operation in the ideal path; with current ceremony exposed, approximately 17–20 raw calls, still nowhere near 10×. | Withdraw the atomic-gate mechanism if the median run needs more than one post-receipt edit/verification call, diagnostics cannot support recovery, or verified-green rate falls below the ordinary path. |
| **C0: no-ceremony fixture, crossed with N/T3 and ordinary/G execution** | T3 with ordinary execution remains around `2–3×`; T3+G may reach 2–3 calls. A fair N+G comparison will likely remain below 10× because native discovery remains. | Withdraw the 10× end-to-end target if the paired median against an execution-matched native arm is `<8×`, or its confidence interval includes `<5×`. |

Record both top-level tool calls and logical operations, but never substitute one for the other after seeing results. Also preregister correctness beyond the existing suites: exact six-site coverage, intended handler placement, selection semantics, and absence of unrelated changes.

4. The written `≥8 calls` withdrawal line has already fired. The preregistered “edit basis” claim must therefore be withdrawn for this experiment; the narrower post-hoc claim—“it is a strong discovery accelerator”—remains plausible.

For the next experiment, “edit basis” would be falsified if:

- T3 still requires source inspection to determine the first valid patch.
- Zero-read edits miss sites, choose incorrect semantics, or pass suites while violating the request.
- Independent reruns do not reproduce green results.
- Fresh anchors/SHA do not protect against stale or mismatched receipts.
- Removing the receipt’s bodies/ranges/contracts does not worsen reads or correctness, suggesting mere prompting rather than supplied evidence caused the effect.
- Native and receipt arms have materially different verified-green rates.

5. **Gene: The receipt looks like a 5× discovery accelerator, not yet a 10× edit basis—it missed its registered cutoff and delivered only 1.28× all-in, while 10× requires an atomic apply/verify gate, removal of fixed ceremony, and paired replication.**