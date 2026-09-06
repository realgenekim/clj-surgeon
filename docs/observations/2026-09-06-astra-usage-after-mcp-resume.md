# Astra: usage after the MCP resume

<!-- agent-usage-window-end: 2026-09-06T08:45:00Z -->

Recorded 2026-09-06T10:07:15.502127+00:00. Window: September 6,
2026, **07:52–08:45 UTC / 00:52–01:45 PDT**. This is one Astra session,
not a fleet comparison or an adoption experiment.

| Population | Observed catalog calls | Outcomes |
|---|---:|---|
| Astra / Codex, one sampled session | 3 inspect + 2 edit = 5 | 4 typed successes, 1 typed refusal |
| Claude | Not sampled | Unknown |
| Shared Surgeon service, same window | 4 inspect + 2 apply = 6 | 5 successes, 1 refusal |

The service's extra inspect was independently correlated to this session's
manual HTTP registration probe using the bounded invocation and matching
returned result. It is outside the collector's catalog-call taxonomy. Do not
inflate the catalog count to six or infer caller identity from service totals
alone. The public edit tool is recorded under the lower-level apply name in
service telemetry.

## Instrumentation repair

The first collection exposed two field failures. `edit_clojure` calls were
counted but absent from route phases. Also, a successful read's returned source
mentioned an error and was falsely classified as refused; the actual structured
refusal was missed. The accidental total of one refusal concealed both errors.

The repaired collector uses completed registered-server MCP result metadata,
never arbitrary returned source, as refusal authority. The actual refusal is
`invalid-intent-form`, counted once. Untyped outcomes remain unknown; absence of
a recognized error is not proof of success. Two faithful regressions failed
before the fix; the complete collector self-test and independent review passed.
The exact same bounds and sample were then collected once more. Only the
corrected receipt is the counting authority; the original is retained as
superseded and its counts are not pooled.

A separate token study sampled other sessions ending before these calls and
counted outer tool names. It cannot establish zero Surgeon use here. This
session's nested calls inside `exec` are actual operations, not shell-only
activity or prose mentions. This collector does not provide token accounting.

## What the session was doing

The route phases show structural inspection, a refused edit followed by its
corrected edit, native verification and Git work, then structural discovery for
a later multi-file task. Across the classified phases there are three Surgeon
read actions, two apply actions, sixteen native-read labels, four verification
labels and eight Git labels. An action can have multiple labels, so these are
not disjoint totals. One native patch is recorded. New reports, scripts and
experimental apparatus legitimately use native tools; this window supplies no
reason to force those through a Clojure editor.

The catalog call/result pairs consumed **1.744 seconds of outer-call wall**.
The six service operations consumed **1.089 seconds of server wall**, returned
1,998 source characters and read 23 files. The largest service read covered
20 files. These timers have different populations and boundaries and must not
be subtracted to claim transport overhead.

The recorded task interval is **unfinished and clipped to 50.58 minutes**,
not a completed edit latency. Completed clock items cover 59.24% of it;
25.53 minutes are labeled model reasoning, 3.28 minutes compaction, and
20.62 minutes are unattributed. Those are aggregate metadata labels, not
reasoning content. Unattributed time is not automatically model thinking.
The session includes coordination, proof preparation, experiments and reports;
the interval is not attributable to the five Surgeon operations.

## Interpretation and limits

Capability and actual use are demonstrated for one repair and structural
preparation. Controlled efficiency and voluntary adoption are not demonstrated
by this study. The repair's single refusal and the compact multi-file read are
useful product observations, not comparative success rates.

The next falsifiable improvement is fewer recovery decisions for malformed
multi-form edits: a refusal should explain the accepted structural unit clearly
enough that a fresh caller succeeds on its next attempt. Compare complete
verified work against native editing on the same task; do not use this mixed
research window as the control.

Sampling was limited to the named root rollout through a single-file symlink;
the filename is preserved but the collector's path-derived session key differs.
Claude history was deliberately excluded. Service roots are box-wide; the extra
probe attribution required the separate bounded correlation. Skill/catalog
visibility before the cutoff is not reconstructed from zero in-window visibility
counters. Repeated identical method calls within one `exec`, dynamic method
aliases and some direct-call formats remain coverage limits. Completed MCP item
counts and outer-call counts are different units. No commands, source bodies,
private workspace names or transcript prose are reproduced here.

Evidence: `/var/tmp/forge/astra-usage-window-0752-0845-fx/receipt-corrected.json`,
`summary-corrected.json`, and `supersession.json`; bounded transport correlation
and field-failure records under `/var/tmp/forge/astra-usage-collector-review-fx/`.
Collector fix: `b60b9103`. The paved `study-agent-usage-self-test` passed.
