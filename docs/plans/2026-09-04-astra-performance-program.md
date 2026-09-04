# Astra: four-hour comparative performance program

## Intent and scope

Gene authorizes parallel experiments, extensive Surgeon dogfooding with independent wall monitoring, and revisiting previously ceded squares in pursuit of 10×+ verified task gains from Clojure's structural properties. Astra leads; Fable owns existing lanes. Public main stays frozen. This branch owns only experiment apparatus and evidence, not production behavior.

The immediate apparatus seam is a model-selectable adapter around the existing watcher/scorer. The existing runner accepts a model argument but historically launches a hardcoded Sol driver; it also confines artifacts to another lane's root. We need isolated `/var/tmp/forge/astra-program` artifacts, a common pinned client, actual-session model validation, immutable acceptance inputs, and safe shared-host scheduling.

## Observable adapter contract

- Explicit model is exactly gpt-5.6-sol or gpt-6-astra; high reasoning, pinned client 0.153.3 for both. Capture binary, prompt, fixture, watcher and adapter hashes before launch. Validate the resolved model from session evidence afterward; do not promote a request string into an observation.
- Every arm receives a fresh clone and unique artifact directory. Refuse overwrite, unsafe identity, paths outside the program root, and inherited MCP configuration. Native has no MCP; tool arm has one required, identity-attested server on an assigned port.
- Reuse `bench/anvil-arms/watch.py` for session-bound telemetry and child cleanup. Preserve complete raw rollouts, watcher output, model output, and staged diff against the frozen base including new files.
- Process exit, complete event stream, actual model, external acceptance, and preserved fixture test infrastructure must all be checked. Missing evidence is unverified/refused, never success or zero calls.
- Test pure validation logic with literals and boundary behavior with a fake driver. No real model, JVM, or network is required for adapter tests. Before any cohort, run one real smoke per model and a tool hand-drive; smoke results are excluded from comparisons.

## Experiments and limits

1. Fresh common-client uniform fanout (21 owners, existing k1 fixture), native floors for both models, then interleaved native/tool comparisons. This establishes whether the model changes the relative tool advantage.
2. Irregular fanout (existing k6 and a new-seed larger case if time permits), testing symbol identity, alias collisions, preservation of comments/discards/reader conditionals, and generator-based native strategies. New seed is an unseen instance, not a new task family.
3. Live discovery transfer and SMW orientation, coordinated with Fable's external feature-acceptance work. Injected receipts remain oracle controls, not proof of live-tool benefit.
4. Own-work dogfooding: bounded structural reads, computed facts and any warranted edits. Observer records tool wall and complete task wall separately, plus source reads/retries removed or added.

Acceptance for migration reuses the pinned six-check FAN oracle plus source byte comparison and immutable test/script checks. Verify it rejects an unchanged baseline and accepts the canonical migration before scoring agents. Agent-written test results alone are insufficient.

Each actual cohort gets a separate frozen preregistration naming exact assets, ordering, floors, stopping rules, acceptance, and resource policy before measured arms. Six native floor runs per model precede a speed claim; smaller screens are exploratory. Shared-host timings cannot claim the prior exclusive-box E-SCALE-WALL design. Initial schedule uses one model arm and at most one bounded JVM; Fable's quiet-window/slot agreement applies. Record startup separately from warm task wall and retain cold-inclusive totals.

## Failure matrix and verification

Required adapter cases: supported/unsupported requested model; matching/mismatching/missing observed model; valid/escaping/existing output root; clean/inherited MCP configuration; native/tool with missing server evidence; dead server; mismatched workspace/server identity; successful/failed/incomplete fake driver; newly created file retained in diff; acceptance positive/negative controls. Tests need not duplicate the inherited watcher's existing matrix, but the real client's stream must be validated before measurement.

No production feature is declared shipped from this experiment. Findings go to the Astra captain's log with clock-derived timestamps, exact receipts, current claim status, counterfactual limits, and the next falsifier. Any subsequent production changes follow the owning intent workflow and independent review.
