# Captain's Log: Three Days of Option Value

The most important result of the last three days is not merely that Surgeon
beat native editing. It is that we found a repeatable way to discover when and
why it should.

The stable landmark is now tagged
`speed/extraction-3.23x-2026-08-26`: a matched-correctness 15-owner extraction
completed in 37.871 seconds with Surgeon versus 122.278 seconds with native
tools. That is 3.23x, with one guarded extraction transaction and one exact
lint. A different owner-cleanup stratum reached 5.80x locally and 4.61x on
Anvil. A six-small-replacement task reached only 1.05x. Those three facts are
more valuable together than the best number alone: the mechanism is coherent
decision compression and source-volume elision, not structural-tool adoption
or the number of edit sites.

## The method that emerged

We began with a seductive but weak question: can a structural editor beat
`apply_patch`? The better question became: can the model express one complete
decision once, let a mechanical kernel compile and guard it, and avoid paying
for repeated perception, serialization, mutation, and recovery?

That reframing produced a disciplined loop:

1. Freeze a real historical counterfactual and its task-level meaning.
2. Make the scorer honest before making the tool look fast.
3. Observe the route as the model's keystrokes, not as isolated tool latency.
4. Split the next hypothesis into the smallest reversible seam.
5. Run a tiny adversarial cohort designed to kill it.
6. Commit an earned mechanism separately from its evidence.
7. Retain the loss, confound, and stop decision.
8. Combine only independently measured wins.

This is Kent Beck's low cost of change applied to tool research. When changing
the benchmark was painful, we improved fixtures, scorers, retention, or warm
nREPL feedback first. When a production change was coupled, we extracted the
pure decision and installed a permanent guard. The enabling refactor was not
speculative architecture; it had to make the very next experiment faster or
safer. Small green commits made backtracking cheap enough that ambitious
experiments stopped being frightening.

## `(N * K * sigma) / t`

The option-value heuristic explains why the work accelerated:

- **N — independent capabilities.** We separated structural compilation,
  transport projection, scoring, fixtures, routing guidance, refusal evidence,
  and benchmark retention. A failed ranker no longer threatened the editor.
- **K — parallel alternatives.** Local dogfood, fresh Anvil callers, Fable,
  Codex Sol, and two Surgeon lanes could test different hypotheses against the
  same frozen evidence.
- **sigma — uncertainty worth buying down.** We spent runs on genuine seams:
  whether native was actually wrong, whether omission incurred a tax, whether
  complete recovery evidence helped easy and hard cases equally, and where
  complete wall disappeared.
- **t — verified decision-cycle time.** Warm nREPL, isolated worktrees,
  counterbalanced small batches, exact receipts, hot reload, and reversible
  commits reduced the time from idea to trustworthy stop/continue decision.

The formula is not a score to maximize blindly. More modules without
independent decisions are indirection. More parallel runs before the scorer is
honest multiply confusion. High uncertainty is valuable only when a cheap
experiment can change the decision. Lower cycle time must include verification,
not merely typing.

## The magical part: SURGEON1 and SURGEON2

The two-lane structure became a small research organization.

Its genesis was intensely observational. Gene watched Codex work, copied and
pasted responses that looked strange, and interrupted recurring failure
patterns with deceptively simple questions: Why did that safe edit need three
attempts? Why did the model read the same owner twice? Why did a tool report
200 milliseconds while Codex appeared silent for more than 30 seconds? What,
exactly, was happening in that blank interval?

Those moments supplied the most valuable hypotheses. They exposed the gap
between service latency and complete task time, the difference between a
correct refusal and an ergonomic tool, and the model's tendency to fragment a
decision when the interface did not offer an obvious chord. The benchmark
portfolio is, in large part, a formalized collection of things Gene noticed
while sitting beside the model.

Gene often entered SURGEON2's lane in “tweezer mode”: watching a narrow
experiment unfold, asking what the model was actually trying to accomplish,
and co-creating the next probe without taking over the production line.
SURGEON2 turned those observations into isolated fixtures, adversarial cohorts,
and immutable receipts. SURGEON1 then decided which causal mechanism belonged
in the product. The result was neither top-down architecture nor unattended
automation; it was toolmaker and tool user studying one another at very high
resolution.

SURGEON1 owned the production line: current truth, compatible architecture,
integration, publication, and the obligation to say no. SURGEON2 owned option
creation: read-path recovery, parity audits, adversarial fixtures, model
cohorts, and immutable receipts. Its work had value when it reduced uncertainty
or exposed a reusable seam, not when it produced a branch.

Several interactions show why this worked:

- SURGEON2's broad read-recovery research discovered that complete owner
  vocabulary helps hard rank-7 misses but can slow easy rank-1 misses. The
  product did not absorb a large policy engine. A 162-byte authority
  instruction earned the useful part; the compact plan handle stopped.
- An experimental extraction-manifest compiler converged with SURGEON1's
  existing transport-neutral planner. We rejected duplicate implementation
  while keeping the safety tests and authority boundary it revealed.
- SURGEON2 adversarially tested SURGEON1's internal extraction compiler on
  supplied decisions, mechanically derivable omissions, a genuine caller
  ambiguity, and stale source. The mechanism survived; an output-evidence
  defect became a separate issue rather than blocking the speed win.
- The first shared hot reload claimed success but a live refusal remained
  sparse. The lanes stopped, found missing reload dependencies, installed a
  permanent regression, and retried once with an announced window. The failure
  became infrastructure.

The asymmetry matters: SURGEON2 does not merge “into” SURGEON1 merely because
it completed work. It presents option value. SURGEON1 decides whether the
mechanism fits the one production algebra and cherry-picks only the earned
part. This preserved both speed and coherence.

## Wins that changed the product

- A fickle nested edit became a one-shot guarded compact edit.
- One transaction could apply 51 edits across nine files atomically.
- Exact selector failures now return complete snapshot evidence and bounded,
  explicitly non-authoritative hypotheses through both MCP and CLI.
- Mechanically complete extractions no longer require a public planning/model
  phase; genuine architectural choices still refuse before write with a
  completed frozen plan.
- Proportional verification removed editor-specific ceremony.
- Startup and analysis JVMs gained bounded heaps, improving fleet capacity
  without pretending memory work was the task-speed breakthrough.
- Installed routing made the earned behavior available to fresh coding agents,
  not only the session that invented it.

## Losses that sharpened the thesis

- Native was not generally incorrect. Forensic task-level scoring corrected an
  initially flattering comparison and established 122.278 seconds as the
  honest matched baseline.
- Fewer MCP schemas removed 90.7% of catalog text and still ran 15.6% slower.
- Compact `plan_id` reduced payload but missed both the byte and decision-time
  gate.
- Generic `verify=fast` was not equivalent to exact lint and correctly rolled
  back good work.
- Formatter startup, narration suppression, and edit-site count were not the
  missing 13 seconds.
- A broad fuzzy ranker was complexity without authority. Similarity belongs in
  hypotheses; exact relations alone may authorize execution.
- SCI is a useful bounded program language, not a universal editor. Its win
  comes when it compresses heterogeneous intent, not merely when it touches 60
  sites.

The discipline was to celebrate these stops. Every losing option prevented a
larger wrong architecture.

## What the telemetry says now

The three-day privacy-safe usage receipt counted 963 Codex Surgeon operations,
including 48 structural applies and 686 structural reads. The service itself
was usually fast: median MCP wall was 204ms, while complete turns remained
dominated by route fragmentation and model boundaries. Median inspection batch
width was still one. Codex also used 423 native patches and 1,480 native reads;
Claude made only one Surgeon inspection. This is not evidence to force tool
adoption. It identifies the remaining product problem: make a complete mission
the cheapest, most legible action for a capable model.

The frozen extraction makes the immediate gap concrete. One independent run
was 37.500 seconds: 8.297 seconds inside the extraction kernel and 29.203
seconds across exact lint, model, scheduling, transport, serialization, and
narration boundaries. The retained value `1407` was lint output characters,
not 1.407 seconds of lint wall—an instructive telemetry-reading correction.
Five-times-native requires at most 24.456 seconds, another 13.415-second
reduction from the stable result. No honest single hypothesis currently owns
all of it.

## Course for the next twelve hours

First, make the invisible residual observable without perturbing it. Then test
three independent candidates: hot or incremental complete proof, the exact
repository verifier inside the transaction, and a cheaper post-decision
materializer. Combine only cells that win alone. The final gate is not a
projection: at least two correct clean-context Surgeon replicas must finish at
or below 24.456 seconds.

If the clocks show no residual interval of at least three seconds, stop shaving
the frozen task and spend the remaining option budget on historical compiled
decision chords and read-mission compression. The larger goal is not one heroic
benchmark. It is a tool whose best path feels like an expert organist's chord:
perceive the decision, strike it once, verify, and move on.

The deepest achievement is cultural. We learned to be ambitious without being
credulous: to chase 5x, invite the strongest counterfactual, preserve every
loss, and make the next change cheaper before making it. That is why 5x now
looks like an engineering program rather than a wish.
