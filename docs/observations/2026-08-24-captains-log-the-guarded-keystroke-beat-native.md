# Captain's Log: The Guarded Keystroke Beat Native Where It Counts

**Date:** 2026-08-24

<!-- agent-usage-window-end: 2026-08-24T14:57:46.740415Z -->

**Question:** Did the new `edit_clojure` entrance merely become usable, or did
it become a better product route than both the Surgeon CLI and native editing
for the supplied exact nested Clojure change?

## Bottom line

The first result was decisive against the observed production native route.
Two subsequent symmetric Anvil controls narrowed the claim—and made it more
credible.

When callers had semantic owner/old/new information but no physical line
context, direct `edit_clojure` finished 6/6 exactly and native `apply_patch`
finished 0/6: every native caller safely guessed the wrong old line. When
native callers were allowed one bounded read before patching, both routes were
6/6 exact and native was about 2.22 seconds faster in the median paired run.
Surgeon still used one action instead of two.

The task named one owner, one exact semantic replacement, one preservation
trap, and every unrelated byte as part of the result. Fresh Sol/high callers
using the installed skill and hot MCP completed it 10/10 exactly. The clean
native arm completed it exactly only 2/3 times. Its failed presentation made
the requested semantic change but silently removed an unrelated trailing blank
line—the precise class of collateral change the guarded editor was built to
prevent.

Against the realistic installed-skill condition, MCP was:

- 30.244 seconds faster;
- 36.7% lower wall time;
- approximately four times fewer tool actions;
- equally exact in the CLI comparison samples.

Against the clean native arm, MCP was not merely more convenient. It was more
reliable: 10/10 exact versus 2/3 exact. Among native's two correct runs, MCP
still reduced median wall by 24.067 seconds and used about one-third as many
tool actions.

This is a product-route win, not proof that one in-memory MCP function is
intrinsically faster than one perfectly composed patch. The model-facing
boundary is the product. Native's patch primitive is quick; the repeated
reading, patching, diffing, EOF repair, and proof burden are not.

## The scoreboard

Correctness gates every efficiency median. The native wall and action medians
therefore include only its two exact runs.

| Route | Exact | Median wall | Median actions | Failed mutations |
|---|---:|---:|---:|---:|
| Directly routed `edit_clojure` | 3/3 | **23.528 s** | **1** | 0 |
| Installed skill + `edit_clojure` | **10/10** | **52.275 s** | **3** | 0 |
| Installed skill, CLI only | 3/3 | 82.519 s | 12 | 0 |
| Native tools, no skill | **2/3** | 76.342 s | 10 | 0 |

The zero in native's failed-mutation column needs care. Native tools have no
transactional refusal channel. The incorrect caller successfully performed a
mutation and reported no tool failure; byte-exact scoring later caught the
unrelated deletion. Mechanical success was not semantic correctness.

## What the routes actually felt like

The directly routed MCP sequence was church-organ simple:

```text
edit_clojure
  -> exact compare-and-swap guard
  -> atomic write + parse + read-back hash + inverse receipt
  -> answer
```

The installed-skill MCP route retained some avoidable ceremony:

```text
load skill
  -> sometimes inspect the owner
  -> edit_clojure once
  -> answer
```

All ten callers selected `edit_clojure` as their first mutation. Each completed
exactly one successful guarded mutation, with zero refused mutations and zero
MCP failures. The median of three actions means the remaining opportunity is
routing and redundant reading, not edit recovery.

The CLI-only route exposed the shell boundary:

```text
load skill
  -> attempt the preferred hot entrance
  -> load fallback instructions
  -> ask for CLI help
  -> compose Clojure/EDN through shell quoting
  -> guarded CLI edit
  -> structural read or diff
  -> answer
```

The CLI mutation itself was not slow. Once composed, the successful guarded
write commands took 0.643 to 2.025 seconds. Complete turns took 79.297 to
87.711 seconds. Almost all of the cost was interface ceremony.

The native route was familiar and deceptively easy:

```text
rg a narrow context
  -> native file change
  -> inspect diff
  -> notice or miss unrelated byte drift
  -> sometimes repair and re-diff
  -> answer
```

The two correct native callers used repeated file changes and repeated diffs to
arrive at the exact hash. The third changed `:done` to `:complete` correctly but
removed the fixture's terminal blank line. Its semantic edit was right and its
artifact was wrong.

## Why this is more than a benchmark trick

The interface succeeded because it preserves the size of the model's decision.
The caller supplies only:

```text
file + named owner + exact old subtree + exact new subtree + match count
```

Surgeon owns everything mechanical:

- canonical workspace confinement;
- owner resolution;
- exact match cardinality;
- stale-source refusal;
- failure-atomic writes;
- parse and read-back verification;
- result hashes;
- inverse receipts and safe undo.

Native editing asks the model to keep those invariants in working memory and
then rediscover evidence through diffs. `edit_clojure` turns them into the
instrument. That is the difference between holding a scalpel and holding a
church-organ chord: one gesture produces the complete intended state.

## The adversarial counterfactual

The strongest claim is bounded.

The direct MCP arm received an explicit routing hint. The native arm received
no native-specific hint and the fixture retained guarded-tool wording. This is
not a pure microbenchmark of one precomposed patch versus one precomposed MCP
payload. Historical neutral controls put that narrower race much closer:
21.595 seconds for one-shot MCP versus 26.749 seconds for native, both 4/4
exact.

That does not erase today's result. It identifies what won: the complete
product route. In ordinary agent operation, judgment, interface selection,
command construction, preservation proof, and recovery all count. The installed
MCP route was both faster and more reliable than the observed native route.

The CLI comparison is similarly a product-fallback comparison, not the minimum
possible CLI command count. The current skill prefers hot MCP, so CLI-only
callers paid an onboarding and fallback tax. A future CLI adapter that accepts
the same compact edit object over stdin could remove most quoting ceremony
while retaining zero idle memory.

### What the current evidence does not prove

The skeptical reading is stricter than the scoreboard:

- the MCP and native routing instructions were not symmetric;
- the MCP service was already hot, so its startup cost was outside task wall;
- the cohorts ran sequentially rather than as paired, randomized trials;
- the native sample was only three runs, and its failed run encountered a
  deliberate byte-preservation trap;
- the specialized MCP received file, owner, old form, new form, and count in a
  contract designed for this task, while `apply_patch` remained a generic text
  primitive;
- there were no native-positive controls such as prose editing or creating a
  new file.

Therefore the result proves that the observed production MCP route beat the
observed native route. It does not yet prove that `edit_clojure` beats an
equally coached, perfectly composed `apply_patch` across Clojure edits. The
historical neutral result—4/4 exact at 21.595 seconds for MCP versus 4/4 at
26.749 seconds for native—is the cleanest existing evidence for that narrower
claim, and it is promising rather than decisive.

### The Anvil experiment that can settle it

Freeze one commit and pre-register four arms before launching callers:

1. **Native routed:** only native read and patch tools are visible, with one
   neutral native routing sentence.
2. **MCP routed:** only `edit_clojure` is visible, with an equivalent-length
   neutral MCP routing sentence.
3. **Production choice:** both routes and the normal installed skill are
   visible. This measures adoption and total product behavior.
4. **Guarded text CAS, if available:** a non-structural compare-and-swap tool
   receives the same old/new/count information. This separates the value of
   stale-source guards from the value of Clojure ownership.

Pair callers by model, reasoning, account, fixture, and time block; randomize
arm order across dev-a, dev-b, and dev-c. Score hidden target bytes, never agent
claims. Infrastructure failures may be rerun; model and tool failures count.
Measure exact first mutation, exact final bytes, complete-turn wall among exact
runs, action count, reads, repairs, tokens, refusals, and CPU/RSS.

Run both hot steady-state and cold/amortized views. A hot shared service is the
real production architecture, but its startup cost must also be reported and
amortized over one, two, and several edits. Start with 12 replicas per arm,
then confirm a passing result with at least 30 per arm.

Use a balanced portfolio: exact nested replacement, duplicate lookalikes,
comment/EOF preservation, stale concurrent modification, six edits across two
files, semantic multi-owner change, prose editing, and new-file creation. The
last two are native-positive controls.

Predeclare the win gate: MCP must be at least 95% exact-final and 90% exact on
the first mutation, no less exact than native, and at least 20% or five seconds
faster in paired median wall with a bootstrap interval excluding zero. Its
advantage must survive amortized startup by a realistic second or third edit,
without a material regression on native-positive controls.

### First symmetric Anvil calibration

Two twelve-call calibrations at `gpt-5.6-sol` high now answer the narrow exact
nested-edit question.

| Treatment | MCP | Native | Interpretation |
|---|---:|---:|---|
| Strict no-read mutation | 6/6 exact, 18.870–21.683 s per-seat medians | 0/6 exact | Structural addressing can act from owner plus semantic subtree where line patching lacks physical context. |
| Natural best route | 6/6 exact, 20.665 s pooled median, 1 action | 6/6 exact, **18.510 s**, 2 actions | Native remains faster for one tiny edit when one bounded read is allowed; MCP is smaller and proof-carrying. |

Native won five of six natural-route pairs; its median paired advantage was
approximately 2.22 seconds. One MCP call took 57.815 seconds, but the pooled
median direction does not depend on that outlier. The fresh MCP bootstrap
median was 6.415 seconds and is excluded from task wall because the production
architecture is persistent. It is still decisive evidence against moving back
to a cold per-edit MCP process.

This calibration completes only the first two arms on one task. Production
choice, guarded text CAS, stale-source, multi-file, semantic, prose, and
new-file strata remain open.

## Direct tool wall versus complete-turn wall

The structured `edit_clojure` cohort retains complete-turn wall but not a
benchmark-specific direct-tool duration. The broader telemetry window reports
182 ms median for `inspect_clojure` and 1.540 seconds median for
`apply_clojure_changes`, across mixed tasks rather than this cohort. Historical
exact-edit kernel measurements were about 115 ms.

The CLI cohort does retain individual command durations: 0.643 to 2.025
seconds for the successful guarded mutation versus an 82.519-second complete
turn median. The gap is model ceremony, not disk-write speed. Native file-change
durations were not retained separately, so no direct primitive-speed claim is
made for that arm.

## Sampling and evidence

The controlled cohorts used `gpt-5.6-sol` at high reasoning on the same
`pair-view-expect-edit` fixture and exact target hash. The direct MCP cohort had
three callers, the installed-skill MCP cohort ten, the CLI-only cohort three,
and the native cohort three. Cohorts were sequential and correctness was scored
from complete target bytes rather than agent prose.

Retained structured evidence:

- `bench/results/2026-08-24-edit-clojure-sol-high-v3`;
- `bench/results/2026-08-24-edit-clojure-matched-skill-sol-high-10x`;
- `bench/results/2026-08-24-edit-clojure-cli-sol-high-3x`;
- `bench/results/2026-08-24-edit-clojure-native-sol-high-3x`.

Each directory contains an immutable external-archive receipt. Raw bulk remains
outside Git.

The surrounding privacy-safe ethnographic receipt covers
2026-08-23T13:22:51.926321Z through 2026-08-24T14:57:46.740415Z, or
2026-08-23 06:22:51 through 2026-08-24 07:57:46 Pacific. It counted seven Codex
sessions, five Clojure-relevant Codex sessions, 38 Codex task turns, 17
Surgeon-using turns, and 377 classified Surgeon calls. Claude contributed four
sessions and one Clojure-relevant session but no Surgeon calls in this window.
The controlled benchmark receipts—not this ambient aggregate—are authoritative
for the four-row scoreboard.

## Breakthrough ladder

| Gate | Result |
|---|---|
| Capability implemented | `edit_clojure` exposes the compact guarded edit |
| Mechanism verified | exact bytes, refusal, atomicity, receipt, and undo tests pass |
| Self-hosted | the tool edited and restored its own implementation |
| Fresh caller succeeds | directly routed callers finished 3/3 in one action |
| Realistic activation succeeds | installed-skill callers finished 10/10 exact |
| Controlled efficiency gate | faster and fewer-action than CLI; more exact than the observed native cohort; symmetric Anvil confirmation pending |

The feature has crossed from “promising primitive” to “admitted product route.”

## Smallest next falsifiable improvement

Run the production-choice arm with both routes and the normal installed skill.
Then add a duplicate-lookalike and stale-concurrent-modification pair. The
claim survives only if callers naturally select the right route, MCP keeps its
one-action proof advantage, stale source refuses, and native-positive controls
remain native.

## Captain's verdict

Native editing remained a fearsome control. Once it received the physical
context it naturally needs, it became both exact and slightly faster on the
smallest edit. That correction is part of the breakthrough, not a retreat from
it.

`edit_clojure` made the zero-read semantic decision possible, reduced the
natural route from two actions to one, and carried its stale-source/ownership
proof in the mutation itself. Native won tiny-edit wall time. Surgeon won
structural addressability and proof density. The next portfolio will tell us
where those advantages outweigh two seconds.

Bang. One guarded chord. Exact source—and now an honest metronome beside it.
