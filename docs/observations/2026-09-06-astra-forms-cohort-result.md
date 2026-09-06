# Astra: four-pair owner-forms CLI comparison

Measured 2026-09-06T02:20:24.392300+00:00 under the announced quiet window.
Engine0a49f012; preregistration in the adjacent forms-cohort-prereg document.
Raw frozen inputs, per-run receipts/diffs and costs: `/var/tmp/forge/astra-forms-cohort-fx`.

The all-correct prediction **failed**. Native verified4/4; owner forms verified3/4.
The tool refused its fourth candidate before writing. No retries were added.

| Arm | Runs verified | Median complete command | Nearest-rank p90 | Sample SD |
|---|---:|---:|---:|---:|
| Context-warm/process-cold native Sol |4/4|22.588s|36.442s|7.793s|
| Cold one-process owner_forms, k1 |3/4|7.405s|7.981s|0.450s|

The3.05x ratio is **latency only, not an equal-reliability completion speedup**.
Both medians retain all four terminal walls, including refusal. The15.183s median
latency gap clears the preregistered six-control2SD floor2.080s; the new native
four-run SD widened substantially (2SD15.585s), so a larger same-wave sample is
needed. The36.442s native trial stays in the table. Small n is not precision.

| Pair | Native complete wall | Tool complete wall | Tool outcome |
|---|---:|---:|---|
|1|21.461s|6.881s|verified|
|2|18.905s|7.981s|verified|
|3|36.442s|7.380s|verified|
|4|23.715s|7.431s|forms-owner-mismatch; no write|

## Surprise: we reintroduced quoting into the model interface

T4 named the correct original owner in its JSON envelope, but its form string
contained literal backslash-n sequences after JSON decoding. Exact replay in the JVM shows all five names and definition heads matched;
two intended docstrings were not parsed as docstrings. The combined definition
property guard returned forms-owner-mismatch. The earlier name-mismatch
interpretation was wrong; this paragraph corrects it. The source
was not repaired, unescaped heuristically, or applied. This is a representation
failure, not evidence that owner discovery missed a caller. JVM evidence: `/var/tmp/forge/plain-forms-T4-JVM-differences.txt`. It also shows why
whole-file success rates cannot silently calibrate JSON owner-form reliability.

Next bounded option: for one-file missions, accept raw complete Clojure definitions
and have the kernel map their names to frozen planned owners. No JSON-encoded code
strings, no old whitespace, no model-supplied offsets. Preserve the existing
compiler's authority and syntax guards. This is an unmeasured alternative; the
current failed trial remains failed. Multi-file ambiguity must refuse until the
format has an explicit file authority. A separate pure decoder is being prototyped.

## Cost and boundaries

Four paid Cerebras calls cost **$0.00953775 total**, directly provider-reported.
Prompt tokens6120; completion9861, including reasoning6443. Reasoning is a subset
of completion, not an extra amount to add. All four candidates, including the
rejected one, contribute to cost. Native dollar usage is unknown here.

Native orientation10.618s is retained separately and unamortized. Human intent and
owner selection, initial fixture materialization, and external review are excluded
on both sides. Tool planning/startup/formatting/proof/commit are included; native
self-checks are included. This is the admitted prepared-change square, not
problem-to-done discovery or an optional-adoption study. Both arms used the same
frozen real-1 source/test and byte-identical independent witness.
