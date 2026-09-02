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

## The winners, and where each one lives (added 12:11Z on Gene's request)

| winner | what it does | lives in | measured or reasoned |
|---|---|---|---|
| `:ls-tree` | table of contents for a whole source tree: every namespace, requires, public forms with arglists and line spans; grep-filterable across repos; seconds | **CLI only** (babashka, cold, no server) | the foundation of "questions grep answers wrong"; ran on voice-remote in under a second |
| `:ls-deps`, `:deps`, `:topo`, `:ls-extract` | dependency tree, intra-namespace call graph, topological order, minimal extractable unit | **CLI only** | the study step of a refactor, answered by the parser not by grep |
| `:extract!` | move forms to a new namespace, plan then execute | **both**: CLI op, and the MCP `apply_clojure_changes` extraction verb | no native equivalent |
| `:mv` + `:fix-declares!` | reorder a form relative to another, then drop the declares the reorder made unnecessary | **CLI only** | no native equivalent; dry-run first |
| `:rename-ns!` | structural namespace rename across the tree | **CLI only** | no native equivalent |
| `require_change` | add or change a require across many namespaces | **MCP only** (`apply_clojure_changes` verb) | nine namespaces, zero churn, measured (l1 Y-5) |
| `within` + `from`/`to` | surgical edit inside one known form | **MCP only** (`edit_clojure` / `apply_clojure_changes`) | zero churn, measured (l1 A-0, A-4, Y-0) |
| `inspect_clojure` outline, forms, owners, prepare-change | per-file perception | **MCP** | the read side of the gate and the fan-out verb |

**Confirmed, and a gap.** The MCP server holds the write winners (`require_change`,
`within`+`from`/`to`, extraction) and per-file perception; the study winners
(`:ls-tree`, the dependency views, `:mv`, `:rename-ns!`, `:fix-declares!`) live only in
the babashka CLI. The killer read, `:ls-tree`, is therefore invisible to an agent that has
the MCP and not the CLI. Filed: expose `:ls-tree` as an `inspect_clojure` workspace
operation (the same output, over the workspace root, with the grep filter), and the
refactor operations as `apply_clojure_changes` verbs, so the winners are reachable from
the route agents actually hold.

**The shape is right for development and testing.** The MCP server is a long-lived JVM with
a warm project image, telemetry on every call, typed refusals, a per-worktree dev instance
(`make mcp-dev-start`) and a hot reload; that is exactly the substrate the gate needs
(re-parse, kondo delta, focused suite, one receipt) and the only substrate on which
proof-before-write is possible. The CLI is the right shape for `:ls-tree`: cold, seconds,
no server, safe to run from a shell or a Makefile. Keep both; put the winners in both.

## The fast-typist shape (codex spark / gpt-oss typist), placed on the battlefield (12:12Z, Gene's question)

Measured: arm T (a cheap fast model typing from a strong model's spec) was negative on the
medium rung, 409 s against shipped 303 s and native 191 s, more actions, acceptance no
better. The mechanism is the same math as everything else: every handoff to the typist costs
the strong model a return to write the spec and a return to verify the result, so the typist
wins only when (spec length) is much smaller than (output length) AND the verification is
mechanical rather than another model return. On a 20-line edit the spec is the edit; there
is nothing to delegate. It is not on the withdrawn square by accident; it is the withdrawn
square with an extra hop.

Where it can win, and only there: the fan-out square, paired with the gate. The strong model
states one intent; the tool discovers the N owners and the per-site templates; the typist
fills N mechanical hunks (boilerplate, fixtures from a schema, a rule applied across many
files); the gate verifies structurally in one receipt instead of the strong model re-reading
N sites. Conditions that must hold: N large (the output-to-spec ratio is the whole margin),
the templates exact enough that the typist makes no judgment, and the verifier a predicate,
never a model. Filed as an experiment for after the gate exists: arm T2 = intent by the strong
model, hunks by the typist, verification by the gate, against native on rung L; prediction:
wins on tokens carried by the strong model, breaks even or loses on wall unless N exceeds
about twenty owners. Not part of the battleground on its own; a possible force multiplier on
one square once the gate and the fan-out verb exist.

## The hole between doctrine and prompt, and what I did about it (12:24Z; mayor's hand-off, Gene: "use its judgement")

The class: doctrine reaches a seat by git pull; the text an agent reads at boot is assembled
from managed blocks installed per box and per account out of band, and nothing ties the two.
The repeal of the Surgeon routing rule was pushed and every agent would still have read the
old forty lines. Same class as the night's other receipt-blindness finds.

Inventory of what I can reach: bridge (genek-forge on Buster) had NO block at all until
this morning, now carries the rewritten plate, hash c3c0d0f5 verified; Anvil tester has no
block and I am deliberately NOT installing one there, because the acid arms' global prompt
must stay empty to keep future cohorts comparable with the night's; Buster's marvin and
genek accounts are outside my reach by rule (no sudo, no-touch) and are reported to skiff,
not swept. Ratchets: an hourly cron on this seat, check-prompt-plate.sh, fetches clj-surgeon
main, checks the installed block against the current plate with the verifier that already
existed, logs the hash, and files an inbox item on drift (first run OK, main 57e3541);
a doctrine paragraph "Doctrine cannot silently disagree with the prompt" with a five-step
rule, on a claude-skills branch for skiff to merge; and a bead ask that the plate embed the
doctrine commit it was derived from so a checker never needs a human's memory. Judgement on
the sweep: no fleet-wide sweep from here, both because I cannot reach the accounts that
matter and because the rule says never change another seat's prompt without telling it;
each seat installs on its own pull, and the tripwire tells us who has not.

## Gate, build round one and red team round one (12:44Z)

Built on bridge/admit-gate from e5c4f46: patch_apply.clj, form_identity.clj,
mcp_admit_tool.clj, 31 witness tests (214 assertions) that failed before the code existed,
design doc with EARS MCP-OP-ADMIT-001..055 and two live receipts (clean patch admitted in
278 ms preview, 234 ms commit; duplicate definition refused in 20 ms, source unchanged).
My own run of the JVM suite: 408 tests, 4168 assertions, one failure, the pre-existing
macOS clj-kondo path assertion, untouched by the branch. Not committed.

Red team, nine probe families executed, scripts retained: GO on path confinement (../,
absolute, symlink-out, NUL, percent-encoded, backslash, all refused through the existing
mcp-paths helper), multi-file atomicity, and stale pre-image at commit. NO-GO on five:
a (declare foo) disables duplicate detection for foo, and definitions wrapped in reader
conditionals, do, metadata or discards evade it, and prefix-list require removal is missed;
verification_complete can be true with zero tests run because process exit was taken as
evidence, and commit precedes verification with no rollback; form-identity is quadratic
(55 s on a 16k-line file) and a 25 MB patch escapes the size cap as an uncaught exception;
preview-to-commit does not bind the pre-image; refusals echo the whole patch past the
public bound. All eight fixes are with the builder under the rule that every probe becomes
a witness test with its own EARS id, verify-before-commit first. The test doctrine's gate
held: nothing committed, nothing launched, arm Z stays armed behind a GO file.

## Ritual audit: half of what agents do, nothing asked for (13:03Z; 81 rollouts, receipt ~/acid/receipts/ritual-audit.md on Anvil)

Every native call in all 81 arm-runs (1,446 model returns, 3,008 shell sub-commands)
classified against the arm prompts, the Makefile, bin/kaocha and the acceptance text, with the
requiring line quoted where one exists.

| level | non-test calls | unmandated | share |
|---|---|---|---|
| model returns (cells) | 839 | 288 | 34 percent |
| shell sub-commands | 2,273 | 1,129 | 50 percent |

The unmandated work is confirmation and bookkeeping, not exploration (94 percent of it):
git diff 465 times in 80 of 81 runs, re-verifying an edit whose receipt the agent already
holds; git status 185 times in 78 of 81, which the prompt does not merely omit but
countermands ("never git commit, stash, or push"); bd commands 241 times in 69 of 81 runs,
including full create, claim, close arcs in 39 runs, on a throwaway worktree nobody reads,
traced to the repository's own AGENTS.md line "Use bd for ALL task tracking", an ambient
local gate; hand-run clojure -M -e probes 53 times against a prompt that says the pages gate
does the JS check. Native and Surgeon arms sit inside two sd of each other on the ritual
share (0.25 vs 0.40, spread 0.23), with or without the mandate arm: ritual is a property of
the local gate landscape, not the tool.

The finding that decides the next cohort: naming a substitute suppresses ritual, forbidding
it does not. The CLI arm, one named route per operation, was lowest at 28 percent; the
mandate arm, which explicitly forbids re-reads and git diff, still spent 13 locating, 7
confirming and 24 git or bd commands doing what it was told not to. That is also the gate's
exact mechanism: the receipt is the named substitute for git diff, the single largest ritual
of the night.

Cohort R, queued after E5 and E8, no code: two prompt variants against native, six-wide, one
wave each. H forbids and explains (this worktree has no beads and no reviewer; the
apply_patch result is your verification; git status and git diff add nothing here). J names
substitutes without forbidding (for each act the agent tends to perform, the thing that
already answers it: the apply_patch result is the diff; the pages gate is the JS check; the
runner records the task, no bd). Prediction from the audit: J removes more returns than H;
H removes little; if J clears the floor on actions with acceptance unmoved, that is a
zero-tool-cost win on every job in the fleet, and the beads line in every CLAUDE.md is a
decision for Gene.
