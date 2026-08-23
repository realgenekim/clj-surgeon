# Captain's Log: Surgeon earned its keep; the map was not deploy-grade

<!-- agent-usage-window-end: 2026-08-12T09:35:34.079971Z -->

**Window:** 2026-08-10 17:16:01–2026-08-12 09:35:34 UTC;
2026-08-10 10:16:01–2026-08-12 02:35:34 PDT

## Question

After another forty hours of real agent work, how useful were clj-surgeon and
cclsp? Did agents merely see the routing instructions, or did they use the
tools successfully enough to trust them during large Clojure changes?

## Method and exclusions

`make study-agent-usage` produced one privacy-safe version 3 receipt from the
previous observation marker through the collection time. The complete receipt
is the counting authority. It joins Codex and Claude histories with
clj-surgeon MCP, cclsp, and clojure-lsp telemetry without emitting transcript
prose, workspace paths, source bodies, or raw service events.

This is a naturalistic mixed-task window, not a matched benchmark. It includes
tool development, self-hosting, repository work, and unrelated verification.
Native writes remain correct for new files and prose-heavy edits. Claude's
history format supplied session and action counts but no task-turn boundaries,
result payloads, or direct action wall, so the Claude row measures adoption,
not end-to-end efficiency. No transcript prose was needed and the collector
required no instrumentation repair.

## Scoreboard

| Measure | Codex | Claude |
|---|---:|---:|
| Sessions in window | 27 | 36 |
| Clojure-relevant sessions | 24 | 21 |
| Trigger visible | 24 | 17 |
| Skill loaded | 23 | 0 |
| Sessions that actually called Surgeon | 23 | 1 |
| Surgeon calls | 3,266 | 41 |
| MCP inspect calls | 1,243 | 15 |
| MCP apply calls | 438 | 26 |
| Unbounded Clojure reads | 0 | 15 |
| Native patch/edit actions | 825 | 228 |

Codex crossed every adoption rung: the trigger was visible in all 24 relevant
sessions, 23 loaded the skill, and those same 23 invoked Surgeon. Surgeon was
used in 89 of 240 recorded Codex task turns. Claude demonstrated that the MCP
contract itself was usable—41 calls in one session—but visibility did not
produce routine adoption: no Claude session loaded the skill, and 20 of 21
relevant sessions made no Surgeon call.

## What agents actually did

Codex split almost evenly between the hot MCP entrance and the CLI. Its 1,681
MCP calls comprised 1,243 inspections and 438 applications. The remaining
1,585 calls used structural CLI operations, led by 904 named-form reads and
477 outlines. Actual CLI mutation included 10 change applications, 12
extractions, 17 subform replacements, and reversible undo operations. Claude
used 15 MCP inspections and 26 MCP applications.

The aggregate Codex route recorded 2,034 Surgeon-read actions, 54 plans, and
491 applies. This was not token routing theater: agents read bounded syntax,
planned guarded changes, applied transactions, and exercised undo paths.
Surgeon returned 11.8 million source/result characters while Codex recorded
zero unbounded Clojure reads.

But adoption did not yet compress the whole workflow. Surgeon-using Codex
turns still contained 725 native patch actions. The dominant collapsed phases
were:

| Phase | Occurrences |
|---|---:|
| Native patch | 644 |
| Surgeon read plus native read | 551 |
| Native read | 484 |
| Surgeon read | 403 |
| Surgeon apply plus native read | 306 |
| Native read plus verification | 263 |

These phases served complete Clojure task turns, not isolated microbenchmarks.
The common boundary remained `inspect -> inspect/read -> patch or apply ->
verify`, rather than the target `prepare once -> decide once -> apply once ->
verify`. Some native actions were appropriate; the receipt cannot label each
one as necessary or avoidable. The prevalence of mixed Surgeon/native phases
is nevertheless strong evidence that safe structural mechanics and complete
interaction compression are separate achievements.

## Direct tool wall versus task-turn wall

The clj-surgeon MCP served 1,744 calls across both providers. Inspection was
fast: 136 ms median and 2.854 s p90. Application was heavier but bounded in the
ordinary case: 2.013 s median and 23.462 s p90. The service spent 112.6 minutes
of aggregate direct wall and returned about 4.1 million source characters.

Across the 89 Surgeon-using Codex task turns, direct Surgeon action wall was
68.7 minutes. Complete task-turn wall was 69.4 hours: 24.1 minutes median and
62.3 minutes p90, versus 22.8 seconds median and 66.1 seconds p90 of direct
Surgeon action wall. Direct Surgeon work was only 1.65% of aggregate turn wall.
Task-turn duration can contain thinking, other tools, verification, waiting,
and inactivity, and service work can overlap, so this ratio is diagnostic
rather than a speedup claim. It says the structural kernel was usually not the
long pole.

The interaction count is less flattering. Surgeon-using turns had a median of
20 Surgeon calls and a p90 of 69, with a median of two native patches. The
existing supplied-intent gate is at most three Surgeon calls. This naturalistic
cohort is harder and more tool-development-heavy than that matched portfolio,
so the values are not a controlled regression, but the distance to the product
shape is unmistakable.

## Failure-closed, but refusal recovery still costs turns

The MCP service completed 1,378 calls and refused 366: a 21.0% refusal rate.
The largest categories were 105 batch-form selection failures, 63 verification
failures, 35 invalid MCP requests, 28 count mismatches, 23 unsupported
insertion parents, and 20 invalid intent forms. Batch-form selection alone was
6.0% of all MCP calls, still above the existing below-5% field gate.

These totals include adversarial tool development and regression exercise, so
they do not estimate a production user's error rate. They do prove that the
guards were exercised at scale. The receipt reports no evidence of a refusal
silently mutating source. The product win is safety; the remaining UX problem
is converting mechanically recoverable refusals into one bounded next call.

## cclsp: useful after admission, unreliable at the entrance

cclsp admitted 75 MCP calls: 73 `resolve_var_surface` requests and two direct
reference queries. Sixty-eight completed and seven refused. Median MCP wall was
2.041 seconds and p90 was 10.277 seconds. Once a reference request reached
clojure-lsp, seven of eight completed; reference wall was 2.854 seconds median
and 10.034 seconds p90.

Startup dominated the failure and latency budget. clojure-lsp recorded 34
requests across 30 sessions and 24 workspaces. Eleven timed out. Ten of 25
`initialize` requests timed out, a 40% initialization-timeout rate. Initialize
consumed 24.3 of the LSP layer's 24.9 aggregate minutes, with a 39.5-second
median and roughly 120-second p90. No workspace recovery was recorded in the
window.

That makes the practical division sharp:

```text
exact owner or nested syntax
  -> Surgeon: routinely fast, guarded, reversible

cross-namespace caller proof
  -> cclsp after warm admission: valuable and usually completes
  -> cclsp initialization: too failure-prone to make deploy-critical proof
     depend on it without an exact-source fallback
```

This supports the field judgment that cclsp is promising but not yet the map
to bet a deployment on. The weakness is not primarily reference resolution;
it is acquiring and retaining a ready semantic session.

## Progress against the product goals

The capability and mechanism rungs are solid. Structural reads and guarded
writes were exercised thousands of times, multi-edit applications reached a
67-edit maximum, multi-file transactions reached four files, and both Codex
and Claude completed MCP applications. Self-hosting is routine for Codex.

Fresh-caller success is uneven. Codex loaded and used the skill in 23 of 24
relevant sessions, while Claude used Surgeon in only one relevant session and
never loaded the skill. The controlled efficiency gate is not answered by this
window. Prior matched results remain the authority for complete-turn speedups;
this mixed cohort supplies adoption and route-friction evidence only.

Three explicit gates remain open:

- The supplied-intent route targets a median of at most three Surgeon calls;
  this naturalistic window measured 20.
- Batch-form selection refusals target below 5% of MCP calls; this service
  window measured 6.0%.
- The semantic trace work targets a hot structural p99 below two seconds;
  the current receipt still cannot join one agent action through Surgeon,
  cclsp, and every LSP phase, while initialization p90 remains about 120
  seconds.

## Counterfactual limits

The receipt does not contain matched tasks, repository labels, semantic
correctness judgments, or a native-only replay. High call counts may reflect
development of Surgeon itself. Native patches may be proper for prose, shell,
or unsupported edits. Service wall can overlap task-turn wall. Claude lacks
turn boundaries. The data therefore cannot claim that Surgeon made the whole
window faster, that every refusal was caller error, or that cclsp returned a
semantically complete graph on every successful response.

It can support narrower claims: Surgeon was genuinely and repeatedly used;
exact structural operations were usually fast; callers still mixed structural
and native mechanics; and cclsp initialization timed out often enough to make
semantic proof operationally fragile.

## Smallest falsifiable improvement

Make one canonical workspace initialization lease survive callers and expose a
single readiness promise: after `clj-surgeon up` reports ready, 20 consecutive
anchored `resolve_var_surface` calls across the already-configured workspace
portfolio must start without a new LSP initialization, complete or return a
typed semantic miss within 10 seconds, and record zero initialize timeouts.

Then repeat this receipt. If initialization timeouts remain nonzero or callers
still pay repeated warming/refusal phases, cclsp is not yet deploy-grade. If
the gate passes, rerun a matched cross-namespace refactor to measure whether
the warm semantic map removes native discovery and shortens complete task-turn
wall.

## Bottom line

clj-surgeon is useful now. It has crossed from capability demonstration into
routine, failure-closed agent work, and its direct latency is rarely the main
cost. Its next problem is interaction compression, not proving that structural
editing works.

cclsp is useful when warm. Its reference path is the right semantic complement
to Surgeon's syntax authority, but initialization reliability still makes it a
best-effort proof source rather than a deploy-critical dependency.
