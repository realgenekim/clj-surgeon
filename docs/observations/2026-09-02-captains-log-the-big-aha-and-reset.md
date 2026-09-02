# Captain's log, 2026-09-02: the big aha, and the reset

*bridge seat, written 2026-09-02T12:02:42Z. Receipts for every number: `2026-09-02-captains-log-bridge-wall-clock-ideal-program.md` in this directory, each with its commit timestamp.*

## What was asked

Gene, at the start of the night: measure whether clj-surgeon makes an agent faster or
better than native edits; "you have liberty to modify surgeon to try to achieve the
theoretical wall clock ideal"; "dogfooding is key"; "do real jobs as arms"; "the acid test
is the ultimate test"; "poll the brain fleet with each result, explore many, never pick one."
And, in the morning, after reading the receipts: "Close all surgeon paths that are losers.
Change skill and house rules." "Pull 'Surgeon is available and expected' from every Clojure
agent prompt today." "Last mission: build whatever you think would help LLMs build better
code; make this a tool we can brag about, and exploit the unique advantages of homoiconic
Clojure."

## The aha

**The tax is the route, and the agent knows it.** On the verified shipped build, in six
paired draws, the Surgeon arm cost 1.8x native wall and 2.1x the actions, 3.6 and 3.8
standard deviations above a floor of nine identical runs. Not because the tool is slow: tool
execution is 3 to 4 percent of wall and 87 percent is the model's own time between calls.
Not because of refusals: wall against refusal count within the Surgeon arm had a rank
correlation of 0.03. Not because of the interface: the CLI matched MCP on wall and was worse
on everything else. Not because of discipline: every prompt lever we pulled moved the
agent's compliance and none moved the cost. The taxonomy of every primitive call in twelve
rollouts said it plainly: the Surgeon arm kept the whole native loop, 102 native calls to
native's 91, and added 51 Surgeon calls that displaced almost nothing. Then s1 said the rest:
with the tool merely optional, all three agents made zero Surgeon calls and ran at native
speed; ordered to substitute, they routed their reads through the tool, kept writing
natively wherever they could, drew four refusal classes nobody had seen, and paid 2.2x.
**Given the choice, the agent declines the tool, and it is right to.**

**The write contract made it inevitable.** Surgeon's write verbs demand exact literals and
owners the tool does not discover, so they can only sit on top of the native read loop,
while the one native step they replace, apply_patch, was already cheap, atomic, and batched:
on the 21-owner refactor native wrote the whole change in one patch cell, and the Surgeon
arms added eight to ten per-form writes on top of a still-present native patch. The tool's
advertised home ground, fan-out, was its worst rung. Its cheapest affordance, insertion at a
form's top anchor, chose the wrong site in a dozen diffs and became a correctness defect.
Two thirds of every refusal the agents drew was the intent grammar itself. The summer's
north star, five-times-native extraction, lived on the one square that cannot pay: a better
per-form editor for edits the agent already had in hand.

**The bisect that settled a quality question with a ledger.** The wave build's clarity
deficit could not be resolved by judge scores at any n we could afford. Typing every refusal
did it in one pass: the insertion-gap fix introduced a refusal that fired in three of three
runs on that build and in none elsewhere; the agents' escapes from it were where the clarity
went. surgeon1 fixed it the same night; b2 showed no regression in the same apparatus.

**The apparatus was lying in the same way the tool was.** Every Surgeon row before 05:30Z
had called another seat's production server while the runner printed a sha it never read.
The suite-run count was a grep over mentions. My own receipt timestamps drifted seven hours.
"main" on Anvil was a hand-copied bundle 69 commits stale. The acceptance suite passed a
broken button because it asserted markers. Each was the same class: a receipt blind to its
subject. Each was withdrawn with its mapping and earned a mechanism: identity read from the
server and attested per arm, timestamps from the clock, staged diffs with a completeness
gate, acceptance as a gate and never a score.

## The reset

**Doctrine.** "Surgeon is not the default route for Clojure edits" is in house rules with
Gene's ruling verbatim; the clj-surgeon skill is reduced to the measured winners with a
closed-as-losers list; the routing plate that installed the old instruction into every
seat's global prompt is rewritten (the mayor found that half: doctrine never reached the
place the instruction took effect); the self-hosting mandate is withdrawn; the safe-refactor
skill gains the one section where Surgeon belongs, because refactoring is the job where the
work is the structure. The laptop disables the MCP; the bridge keeps it, for testing.

**The battlefield** (vision.md, and CLAUDE.md with the math). Four squares: verification
after the agent's own patch; one intent across N owners with tool-side discovery and splice;
questions grep answers wrong, with `:ls-tree` as the foundation; proof before write in the
warm JVM. One square withdrawn: the single known-site edit. The bar on every square: a call
must remove a return the agent would otherwise make, on the route the agent already takes,
and the free-choice arm must use it at all.

**The build.** Two designers, three plans each, independent, chose the same first shape: a
gate on the agent's own native patch (`admit_clojure_patch`). One call takes the diff the
agent was about to apply, re-parses pre and post images as forms, reports the delta by owner
and by protected node class, refuses form-level hazards with an executable next_call,
commits atomically, runs the kondo delta and the focused suite, and returns one receipt in
place of the three post-write returns. Its value does not depend on the agent choosing us.
It is being built LID-style on bridge/admit-gate and will be measured as arm Z against
native under the test doctrine before a claim is written.

**The test doctrine** (CLAUDE.md), so this does not take months again: native control in
every cohort, variance floor first, free-choice adoption as the acceptance test, attested
subject, typed ledger and call-site taxonomy on every run, acceptance as a gate, frozen
complete artefacts, clock-derived timestamps, two blind judges with rubric rulings, and
every fix a ratchet in the apparatus.

## What became cheaper

A cohort on Anvil is now six attested arms, frozen and scored, in twenty minutes, with the
floor known. The next question about any Clojure agent tool costs an evening, not a summer.
