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

## Close-losers branch, red team round one (13:05Z): NO-GO, the signature class again

The typed refusals that close the loser shapes at the server were built (mcp_close_losers.clj,
splice_drift.clj, design doc with EARS MCP-OP-CLOSE-*, seven new tests; my own runs match
main's failure sets exactly). The red team's executed probes found the headline refusal dead
in production: the server keywordizes request params before validation and the refusal read
string keys, so it returned nil for every real request while every unit test passed with
string keys. The drift oracle scored zero for junk adjacent to a span and for insertions at
a zero-length span, because it located gaps by searching from a cursor rather than at the
offsets the pre-image spans predict; only whole-file reformats were caught. And one
restaging action (assoc_entry) was missing from the refused list. Sound and kept: the
whole-file drift gate, the splice path (comments, metadata, reader conditionals and
multi-line strings preserved byte for byte), the winners unrefused with drift 0, executable
next_calls, nine EARS ids each with a witness that fails when its check is removed. Fixes
routed with the rule that the witness for the key mismatch must drive the production entry
path, not the validator. Fourth "verifier blind to its subject" instance of the day, caught
before a commit by the doctrine's own gate.

## E5: the stale-onset defect, pre-registered predicate, one build, one prompt (13:24Z)

Nine shipped (7893, attested) and nine native arms on rung M, three alternating waves, scored
by the deterministic predicate (guard placed before the speechStartAt reset without resetting
it), validated 24 of 24 against the judges before the cohort ran; no judge in the loop.

| arm | DEFECTIVE | SAFE | walls s |
|---|---|---|---|
| shipped A | 6 of 9 (2, 3, 1 per wave) | 3 | 506, 550, 599, 437, 546, 615, 549, 571, 590 (mean 552) |
| native N | 0 of 9 | 9 | 298, 315, 366, 308, 313, 317, 305, 386, 415 (mean 336) |

Fisher's exact test on 6 of 9 against 0 of 9 gives p about 0.009. The skew the pooled tally
suggested and the fleet distrusted is real on this build and this task: the Surgeon arm
places the mic-gate guard at the cheap top anchor, before the reset the branch depends on,
in two thirds of its runs, and the native arm never did in eighteen. This is the claim that
upgrades "slower" to "harmful", now stated with the rigour the closing round asked for
(single build, single prompt, mechanical predicate, no pooling). Wall on the same cohort:
1.64x, consistent with n1. All eighteen gates green.

Why it matters beyond the number: the acceptance suite passed all six defective diffs, so the
defect class is invisible to markers and visible only to a parsed pre-and-post comparison,
which is the gate's job; the arm Z cohort measures whether the gate catches it in situ.

## Close-losers round two, and a correction to the churn mechanism (13:26Z)

All three red-team findings fixed with witnesses MCP-OP-CLOSE-001..016 that drive the
production entry path: keys normalised once at the refusal's entry (the strongest proof was an
existing test that had been committing the exact closed loser through the real path and
passing; it is now the refusal's witness); the drift oracle is positional (every untouched gap
must sit at its reference offset, a changed span length is :unlocatable and fails closed);
all four restaging actions enumerated; the refusal's next_call replays to success (CLOSE-016).
Suite: 391 tests, 4016 assertions, one pre-existing failure.

**Correction to the 241e1bb receipt ("Surgeon's printer re-emits the whole file").** The
builder's direct probe shows the compiler splices and preserved every byte outside the span
for both loser shapes. The whole-file reformat came from mcp-tool's prepare-compiled! running
`standard-clojure-style fix` on the entire staged file, a formatter Surgeon itself invokes,
installed only when the request is not an editor gesture. That reproduces the measured 3 of 3
against 0 of 4 separation exactly, Y-5's zero churn included. The earlier receipt ruled a
formatter out by counting the agent's tool calls, and was blind to Surgeon's own subprocess:
the receipt-blind-to-subject class, in my own analysis. Bead 46o's fix is therefore smaller
than "splice, never re-print": stop whole-file formatting on the changes route, or format
only the edited span. Preview mode remains unproven (both change contexts hardcode commit) and
the design doc says so rather than claiming it.

Two decisions above the builder's level, for Gene and surgeon1: the drift gate now refuses any
changes-route commit on a file the formatter would reformat, which closes the churn but the
real fix may be to stop restaging; and a namespace-owner insertion has no complete redirect
because from/to take one form and require_change is bound to symbol_migration, so those
refusals name the missing field rather than hand back a runnable call.

## E5 scored: the throughput gap replicates a fourth time; the defect tracks presence, not usage (13:27Z)

| metric (n=9 each) | shipped A | native N | delta in floor sd |
|---|---|---|---|
| wall s | 551 | 336 | 2.5 |
| total actions | 32.2 | 19.2 | 4.5 |
| input tokens | 2.09 M | 1.21 M | 3.5 |
| native patches | 2.0 | 4.2 | |
| typed refusals | 3.4 (31 total, 18 invalid-intent-form) | 0 | |
| diff lines added | 85.7 | 94.4 | |

All three throughput metrics clear two sd of the floor, consistent with n1, s1 and l1. Native
is not defect-free by doing less: it shipped larger diffs on the same four files. Fisher's
exact one-sided on 6 of 9 against 0 of 9: p 0.0045.

Inside the shipped arm nothing separates defective from safe runs at two sd, and every
directional signal runs against a "more tool use causes it" story: defective runs made fewer
MCP calls (6.3 vs 11.0), fewer writes, fewer refusals, and more native patches. The defect
tracks the tool's presence in the loop, not how hard the agent leans on it; the one
ambiguous-insertion-gap refusal of the cohort landed on a SAFE run. Hypothesis under test
now, from the rollouts: with Surgeon present the agent has the form's boundaries from the
outline but never reads the body of the JavaScript string (the outline cannot see inside a
string literal), so it inserts the guard at the function's top without having seen the
reset line the branch depends on, while native agents grep the body and see it.

## Gate, red team round two (13:30Z): NO-GO on four points, one of them in the kernel

Held: pre-image binding (replay, wrong file, creation disguised as edit all refused), the
byte cap (25 and 50 MB refused in under 4 ms), payload bounding, path confinement, multi-file
rollback, linear identity (128k lines in 2.9 s), the detector on defmulti, defprotocol,
defrecord, deftype, defmacro, defonce, deftest, and the correct non-duplicate for the same
symbol in :clj and :cljs branches. Failed: eight concurrent commits on one file lost
claimed-committed edits in four of six trials, because the kernel's commit-compiled! is
check-then-write with no lock and the gate widens the window across lint and tests, so this
is a kernel finding for surgeon1 as well as a gate fix; the {snapshot} placeholder in the
test command is cosmetic, a command that lists it and prints "Ran 7 tests" is credited, and
:focused-test has no production loader, so every real commit today reads verification
incomplete; lint deliberately unverified plus commit returns ok true; the duplicate detector
does not walk into when, let, binding, try or if, collapses two defs in one reader-conditional
branch, and falsely refuses a libspec wrapped in a reader conditional. Round three routed:
a per-workspace write lock from snapshot to commit with the hash re-checked under it; test
evidence from a report file the runner writes inside the snapshot; a loader from the start
config or a repo file; verification_status on every receipt; detector depth. Two red-team
rounds have now found, in a verification gate, both halves of the class the night was about:
a receipt that can say verified without evidence, and a write that can say committed and be
lost. The doctrine's gate held both times.

## The read-less hypothesis is falsified; the mechanism is insertion strategy (13:30Z)

All 18 of 18 E5 runs had the `if(playing){speechStartAt=0` line in context before their
first onsetReady write; include_string_symbols was used zero times; no run learned the reset
from an inspect result; every agent in both arms read the JavaScript string with rg or sed.
My hypothesis is dead. What distinguishes the runs is the shape of the insertion:

| strategy | runs | defective |
|---|---|---|
| in-block: guard inside the existing if(playing) branch, after the reset | 10 | 0 |
| top-guard: at the head of onsetReady (or a wrapper), condition re-derived as micGate and playing | 8 | 6 |

Fisher's exact one-sided on the strategies: p 0.0015. The two safe top-guards differ from
the six defective ones by exactly one statement, speechStartAt=0 carried into the guard body.
Shipped is defective more often because it chooses the fragile shape more often, 7 of 9
against native's 1 of 9 (p 0.0076), not because it read less: every defective guard names
"playing" in its own condition, which is derivable only from the line the hypothesis said it
never saw. And among the six defective runs the first write was a Surgeon write in three and
a native apply_patch in three. So the tool's presence shifts which strategy the agent
chooses, the boundary insert over the in-branch edit, even when the agent then types the
guard natively. That is the cheap-affordance story restated correctly: not the tool's hand,
the tool's framing. For the gate this means the hazard to detect is generic and structural:
a new early return placed before an existing state reset in the same function, which a
parsed pre-and-post comparison can see and a text diff cannot.

## Close-losers, red team round two (13:37Z): mechanism confirmed by instrumentation; the basis route is the open door

Instrumenting format-candidates! settles the churn question: it fires on the changes, basis
and extraction routes and never on edits; one logical edit gives 89 bytes of drift on
changes with the formatter, zero without, zero on edits with the formatter configured. The
churn is Surgeon running standard-clojure-style fix over the whole staged file on every
non-editor-gesture route. Bead 46o is a formatter-scope fix. Every round-one defect and every
round-two fix holds on the changes and edits routes (five key shapes refused; drift caught
for same-length byte changes, CRLF, BOM, trailing newline, tabs, multibyte; unlocatable
refuses at commit; a second file's drift refuses the whole transaction; winners commit at
drift 0; fence and paths untouched). NO-GO because the basis route, prepare-change, the route
the server's own instructions recommend, runs the formatter with no drift gate and skips
parameter validation entirely, so neither the loser refusal nor the gate can reach it, and
probe q4 committed whole-file churn there with ok true; extraction is ungated the same way;
and the drift gate fails open when a compile path omits its splice guard. Round three: span-
scoped formatting on every committing route (or whole-file formatting off on basis and
extraction as on edits), the gate as backstop everywhere, basis through validation, and a
refusal when the guard is missing. A design decision for surgeon1 surfaced by the probes:
require_change is bound to symbol_migration, so a namespace-owner require insertion has no
one-call redirect, and the only legal completion rewrites call sites nobody asked for.

## E8: b2 widened to n=6; a b2 finding withdrawn; a new refusal class on main (13:43Z)

| metric, b2 plus e8, n=6 per arm | B main 2311cc09 | A shipped | delta in floor sd |
|---|---|---|---|
| wall s | 575 | 637 | -0.7 |
| total actions | 32.7 | 32.8 | -0.1 |
| MCP calls | 10.3 | 11.8 | -0.5 |
| typed refusals | 4.0 | 4.7 | -0.5 |
| input tokens | 2.37 M | 2.28 M | +0.3 |
| acceptance failed | 1.67 | 2.50 | |

Nothing clears two sd at n=6; every B-favouring number from b2 shrank when the sample
doubled, and the two waves disagree on direction. Refusals by reason over twelve runs:
overlapping-intents 0 on both; ambiguous-insertion-gap 1 on B against 3 on A, rarer, not
eliminated; batch-form-selection-failed 1 on B against 5 on A, so b2's "B strictly removes
the class" was an n=3 artefact and is WITHDRAWN. New: missing-fields appears in 4 of 6 B runs
and in 0 of the 40 shipped-arm runs with typed ledgers tonight (its only other sighting was
b1's G build). It fails closed and is correctly typed, so it is not a safety regression, but
it is a real agent-visible behaviour change in 2311cc09 that nobody has characterised
(which call, which field, whether the agent recovers). Promotion verdict, both gates: under
"demonstrated improvement", 2311cc09 does not pass; under "no detected regression", it passes
only once missing-fields is characterised and shown recoverable, so the honest state is
"held pending one characterisation", not "safe to promote" as I said after b2. That earlier
line is corrected here.

## missing-fields on main, characterised (13:46Z): a wave-lineage requirement, one extra return, recovered every time

All four B-arm missing-fields refusals are inspect_clojure reporting missing "expect": a
forms request without its own per-request expect (2 of 4) or a call with no top-level expect
(2 of 4); outline requests without per-request expect are accepted, so the requirement
attaches to forms. Recovery 4 of 4 in one round trip by re-sending with expect; no verb
switch, no fallback, no abandonment. The byte-identical refusal comes out of the G build
(ec63d0ff) before the merge, and shipped's agents never wrote either shape in 21 inspect
calls, so this is a wave-lineage schema requirement shipped never exercised, not a
tightening from 2311cc09. Promotion, corrected once more and now stable: from the ledger,
2311cc09 shows no detected regression at n=6 and one agent-visible cost, an extra return on
forms requests that omit expect, in 4 of 6 runs; that cost belongs to the intent-grammar bead
(xio), where "expect required per request" is exactly the kind of field an agent cannot
guess, and the refusal does carry correct_request as its next action. "Demonstrated
improvement" is not shown at n=6 and nothing in the ledger suggests it would be at n=12.

## Andon pull: lost updates in the shipped kernel's commit path (13:58Z; puller bridge, incident record here)

Gate round three finished (56 witness tests, 453 assertions; a per-workspace lock from
snapshot to commit makes eight concurrent commits succeed six trials of six; test evidence
now only from a report the runner writes to a gate-named path inside a fresh per-call
snapshot directory, stdout never; :focused-test loaded from the start map or a repo file;
verification_status on every receipt with ok false when nothing requested produced a result;
detector walks when, let, binding, try, if, eval and intern). While proving the lock, the
builder ran the same eight-way shape against edit_clojure ALONE: two of three trials lost an
edit whose receipt said committed (trial 0 claimed 4 and 5, present 4; trial 2 claimed 4 and
5, present 5), one reached transaction-recovery-required. Cause: intent-transaction/
commit-compiled! is check-then-write with no mutual exclusion, shared by every write
entrance. Pulled to skiff as credible and reproduced, scoped: only concurrent writers on one
file through one instance are exposed, which single-agent-per-worktree use is not, so no
fleet freeze was asked for; the ask is that no deployment share an instance across agents
writing one tree until the kernel path is serialised, and that surgeon1 own the fix. Repro:
scratchpad/redteam-admit2/r5.clj against edit_clojure. The gate's lock wraps the kernel path
and does not fix it; recorded in the design doc under out of scope.

## Cohort R: prohibition beat explanation, and my prediction was wrong in direction (14:02Z)

| arm (rung M, six-wide) | unmandated sub-commands per run | total actions | wall s | tokens | acceptance failed |
|---|---|---|---|---|---|
| H forbid and explain (n=3) | 1.67 | 17.3 | 284 | 1.06 M | 2.33 |
| J name substitutes (n=3) | 12.0 | 20.0 | 324 | 1.25 M | 1.33 |
| N native (n=6) | 14.3 | 21.2 | 358 | 1.46 M | 1.83 |

Forbidding removed 88 percent of unmandated sub-commands (minus 2.6 sd of this cohort's own
native spread), deleted the bd, git, environment and probe classes outright, and cut the
largest class, post-patch confirmation, by 75 percent. Naming substitutes did nothing that
clears noise (minus 16 percent) and confirmation calls rose 10 percent under it: telling an
agent an answer is already known does not stop it checking; naming the command and saying do
not run it does. The audit's inference that forbidding fails came from the mandate arm,
whose prompt forbade AND ordered substitution through a tool that was fighting it; that was
the confound, and this cohort removes it.

The cost side is the honest half: ritual is cheap per return. Deleting 12.7 sub-commands per
run removed 3.8 model returns (minus 1.3 sd of the floor) and about 75 s of wall (minus 0.9
sd), because most ritual rides inside cells that also do real work. Acceptance did not move.
So the paragraph is a free and safe win on tokens and sub-commands, and a suggestive but
unproven one on returns and wall at n=3; rt2 (forbid against native, six each) launched to
settle the returns question. Doctrine implication for Gene either way: on throwaway
worktrees, a one-paragraph prohibition of bd, git status, git diff and post-patch re-reads
costs nothing and removes most of what the audit found.

## Close-losers, red team round three (14:03Z): the vacuous span is a real false green; 46o is a top-level-form fix

Round two's fixes hold under every prior probe: basis smuggling refused in three key shapes,
a basis commit under a reformatting formatter lands at drift 0 with untouched runs preserved,
a missing guard refuses. Three new NO-GO points, all routed: a whole-form span plus a
formatter commits with drift 0 while every byte on disk was rewritten, because the oracle
compares gaps and there are none (fix: measure against the expected post-image, pre-image
with the request's own replacement spliced in, inside spans too); a file in future-sources
absent from the guard commits unmeasured and a nil reference throws; the extraction route
publishes no drift field while CLOSE-008 is checked. The finding that matters most sits
outside the branch: standard-clojure-style fix on a complete top-level form in isolation
produced bytes identical to formatting it inside the full file, differing only for sub-form
fragments by the lost starting column. So the builder's "whole-file context is required" was
wrong, and bead 46o's fix is "format the enclosing top-level form, never the file": small,
and it removes the churn class at the source instead of refusing it at the gate.

## Gate, red team round three (14:15Z): GO with three small items

The lock holds under two server processes on one tree (five of five trials, eight of eight
commits present) and in-process (six of six); previews and reads do not block while a
commit holds it; an exception mid-commit releases it. The evidence path is fresh per call
and refuses foreign-namespace and failing reports; the twelve-way verification matrix is
honest in every row; the kernel commit path, the SCI fence and path confinement are
sha-identical to main. Three items before commit: a non-zero runner exit after a clean
report is credited complete; two adjacent top-level reader conditionals defining one symbol
on disjoint platforms are a false duplicate; the receipt does not disclose lock scope. All
three routed as round four; then the branch is committed and pushed as a branch for
surgeon1 and the mayor to review, and 7894 stands up on Anvil for arm Z.

## rt2, pooled with rt1: the forbid paragraph is the cheapest win of the summer (14:21Z)

| metric (rung M) | forbid H (n=9) | native N (n=12) | delta | Welch p |
|---|---|---|---|---|
| total actions | 15.2 | 20.1 | -24 percent | 0.0065 |
| non-test actions | 6.8 | 11.2 | -39 percent | under 0.0001 |
| wall s | 259 | 353 | -27 percent | under 0.0001 (d = -2.0) |
| input tokens | 0.88 M | 1.30 M | -32 percent | 0.0012 |
| unmandated sub-commands | 1.44 | 15.25 | -91 percent | under 0.0001 (d = -2.8) |
| acceptance failed | 1.78 | 1.83 | flat | 0.91 |

All nine forbid runs sit between 1 and 2 unmandated sub-commands; native's range is 9 to 30.
Paired by wave, the forbid arm beat its own wave's native on all four throughput measures
in all three shared waves. Classes: bd, git, repl-probe and js-check erased; post-patch
confirmation cut from 8.2 to 1.3 per run. Name-the-substitutes (n=3) stays at 12.0, having
moved only the classes it said do not exist. Under the conservative per-run two-sd screen
only the sub-command cut clears; on a means test at n=9 against 12 every throughput metric
separates; acceptance is the tightest null of the night. Killing ritual costs no correctness.

Doctrine verdict: ship the forbid-and-explain paragraph in delegation prompts for throwaway
worktrees, scoped to where its three premises hold (no reviewer, no beads workflow, a gate
that performs the JS check), sold as "kills ritual, cheaper as a side effect, costs no
correctness". For scale: this is a larger wall reduction on native than anything Surgeon
ever measured in either direction, from one paragraph. The paragraph, verbatim as run:

> This worktree is throwaway and has no reviewer and no beads workflow: do not run bd, do
> not run git status or git diff, do not re-read a file you just patched, do not hand-run
> clojure -M -e syntax probes; the apply_patch result is your verification of the edit and
> the pages gate performs the JS check. Every extra command costs a full model turn.

## Both branches committed and pushed for review (14:28Z)

bridge/admit-gate f2d93ab: the gate, 61 witness tests, three red-team rounds, final suite 438
tests with only main's pre-existing failure. bridge/close-losers 205e13a: the loser shapes
refused at the server on every committing route, drift against the expected post-image,
fail-closed guard, 22 EARS ids, four red-team rounds, final suite 399 tests likewise. Both
authored forge-bridge with Gene as co-author, pushed as branches only; merge belongs to skiff
and surgeon1. Neither touches the SCI fence, path confinement or the kernel commit path; the
Andon on the kernel's check-then-write commit stays open. Next: 7894 on Anvil from f2d93ab
with report-file test evidence, then E1, the gate against native on both rungs.

## Arm Z live: E1 launched (14:44Z; z1 started 14:42:54Z)

The gate serves on Anvil port 7894 from the pushed branch f2d93ab (pid 550992), with the
focused-test command as a report-writing kaocha wrapper that overlays the gate's snapshot
before running (the gate refuses any command without a {snapshot} token, and a negative
control showed a break injected only in the snapshot produced four failures, so the overlay
is real). End-to-end proof: preview with tests from the report file, bound commit under the
cross-process lock, duplicate definition refused in 32 ms before lint or tests. z1 is rung M,
Z (native prompt plus one admit call per Clojure change, receipt terminal) against native,
six a side over two waves, every arm attested with the server sha read from the server; z2
on rung L follows. Caveat: the gate's JVM shares cores 10 and 11 with one native slot per
wave, so walls carry that; actions, refusals and receipts do not. Predictions on record:
post-write shell calls to zero in the Z arm; wall within one sd of native; the stale-onset
and shadowed-declaration classes caught at the gate in situ.

## z1 walls: the gate arm is slower than native, and slower than the shipped editor was (15:18Z)

| arm (rung M, six-wide, paired by wave) | walls s | mean |
|---|---|---|
| Z native plus admit gate | 439, 674, 932, 475, 895, 1035 | 742 |
| N native | 354, 384, 421, 240, 258, 322 | 330 |

All twelve gates green. The wall prediction (within one sd of native) FAILS on its face:
2.2x, wider than the shipped editor's 1.8x. Before reading it as the design's death, the
receipts have to say where the time went, because the gate's own verification spawns a
fresh JVM and a focused kaocha run per admit call (about five to fifteen seconds of tool
time each, the killer risk Sol named), which is tool time and not returns, and the receipts
also decide the other two predictions: post-write calls to zero, and hazards caught in situ.
Scoring now; z2 on rung L runs in the same chain regardless, because the large rung is
where the gate's per-call cost amortises differently.

## E1 scored: the gate lost in the field, and the reason is mine (15:23Z)

| metric (rung M, n=6 each) | Z native plus gate | N native | delta in floor sd |
|---|---|---|---|
| wall s | 742 | 330 | +4.8 |
| total actions | 33.5 | 19.8 | +4.7 |
| tokens | 2.47 M | 1.27 M | +4.8 |
| post-write native calls after a verified receipt | 1.0 per run | n/a | |
| admit calls | 85, all commit mode | | |
| admit refusals | 59 (69 percent): invalid-patch 32, patch-does-not-apply 16, verification-failed 9, unreadable-post-image 2 | | |
| verified receipts | 17 complete, 12 partial, 56 unverified | | |
| admit tool time | 2.7 s per call, 5 percent of Z's wall | | |
| fell back to apply_patch on .clj | 6 of 6 runs | | |

All three predictions failed. Post-write calls did not go to zero. Wall did not stay within
a sd of native; it went to 2.2x, worse than the shipped editor. The gate caught no
substantive hazard in situ (two self-inflicted read-back failures). Ninety-three percent of
the extra wall was model returns, and the returns went to one thing: the first refusal in
all six runs is identical, "patch contains no unified diff file headers". The prompt told the
agent to write "a unified diff, the same format you would give apply_patch", and those are
not the same format: Codex's apply_patch grammar is "*** Begin Patch / *** Update File:" with
context-matched hunks, and that is what every agent emitted. The gate's contract was on the
agent's route in design and off it at the byte level, and the agent fought the parser and
then abandoned the mandate in every run. Three red-team rounds could not see this because
they fed the gate unified diffs. The free-choice arm would have found it in one run, and the
doctrine I wrote this morning says to run that first. I did not.

Also found: the gate stages its own control files (the lock and the profile under
.clj-surgeon) into the index, which polluted the frozen diffs until the freeze learned to
reset the index and exclude them; one Z run (Z-1, ten calls, ten refusals, zero verified
receipts) delivered nothing to disk, so its diff is empty and its acceptance is a task
failure; acceptance for Z is being rescored on the corrected diffs. Fix routed: the gate
accepts the apply_patch grammar as well as unified diff, detected by the header; refusals
name the grammar tried and the offending line; the commit path never stages control files;
then z3 reruns with the free-choice arm alongside the mandated one. z2 on rung L is running
with the same defect and will show the same shape; kept as confirmation.

## z2 walls: the same shape on the large rung (15:36Z)

| arm (rung L, six-wide) | walls s | mean |
|---|---|---|
| Z native plus gate (grammar defect present) | 305, 369, 435, 254, 258, 543 | 361 |
| N native | 173, 213, 213, 169, 213, 316 | 216 |

All twelve full gates green at the base counts. The gate arm is 1.7x native here against
2.2x on the medium rung; whether any Z arm completed the 21-owner hoist, and how much of the
gap is the same grammar fight, comes from the scorer and the rung-L acceptance suite.

## z1 gate-arm acceptance on the corrected diffs (15:37Z)

Failed assertions of 39: Z-0 1 (acid-9), Z-2 3, Z-4 1 (acid-9), Z-1 20 with an empty diff (the
run whose ten admit calls were all refused delivered nothing; a task failure), Z-3 5, Z-5 3;
native 2.0 mean. Excluding the failed run the gate arm sits at 2.6, inside the suite's
floor; including it, the gate arm is the only arm today with a run that shipped nothing.
Nothing in E1 moved in the gate's favour; the grammar defect explains the shape but the
receipt stands as measured.

## z2 scored: the same grammar fight, one structural limit, and one run that shows the shape working (15:40Z)

| metric (rung L, n=6 each) | Z native plus gate | N native | delta in floor sd |
|---|---|---|---|
| wall s | 361 | 216 | +1.7 |
| total actions | 15.2 | 10.7 | +1.6 |
| tokens | 0.70 M | 0.43 M | +1.1 |
| admit calls | 24 (4 preview, 20 commit), 18 refused (75 percent), 0 hazards | | |
| refusals by type | invalid-patch 7, unsupported-patch-operation 6, admit-tool-failure 3, patch-does-not-apply 1, source-file-not-found 1 | | |
| acceptance (rung L suite) | 0 failures, 6 of 6 | 0 failures, 6 of 6 | |

Every arm in both columns completed the hoist (11 files, one currentTimeMillis left, 82
assertions clean). The grammar refusal repeats in 5 of 6 gate runs. New on this rung: the
gate refuses whole-file creation and deletion ("apply them natively and admit the edits
separately"), and the task begins by creating clock.clj, so all six gate runs were told by
the gate to go around it. The penalty no longer clears the floor here; 95 percent of it is
still model returns in the re-submission loop, admit tool time 3 s per call. One run,
z2-g2-Z-1, used the shape as designed: two admit calls, took the creation advice, then
admitted the rest in one verified commit, never touched apply_patch on a .clj, and finished
in 254 s, ahead of three of six natives. That is the first field evidence that when the
grammar matches, the gate is at parity or better, and it is one run. Round five gains
creation and deletion (Add File and Delete File in the apply_patch grammar, /dev/null in
unified diff) with post-image hazards and a namespace-removal check.

## Gate round five: both grammars, and the self-inflicted hazard explained (15:41Z)

Commit 5be1ce9 on bridge/admit-gate (not pushed): the parser accepts Codex's apply_patch
V4A grammar and unified diff, selected by the first non-blank line; hunks in V4A are located
by context match with the @@ text as anchor; the same edit in both grammars yields byte-
identical post-images; a payload in neither grammar returns invalid-patch with the grammars
tried, the offending line, and the expected headers in next_call. The unreadable-post-image
hazards of z1 were the gate's own doing: the old parser trusted the @@ counts over the hunk
body and silently discarded surplus lines, producing an unbalanced file it then reported as
the author's defect; now :hunk-body-overruns-header, refused before application. The lock
directory gets a self-ignoring .gitignore so the workspace status shows only the patched
files. 68 witness tests, 570 assertions; mcp-test 445 with only main's failure. The
builder's own closing line, kept verbatim in the design doc: "Three adversarial rounds
hardened this gate against inputs the reviewers wrote, and the field killed it with the
first input a caller wrote. The cheapest available test at any point in those rounds was
one real payload." Next: creation and deletion admitted as operations, then the acceptance
test the rounds lacked, replaying all 109 field payloads through the new parser.

## The field replay: 109 real payloads through the fixed gate (16:12Z; commit b171338 on the branch, unpushed)

| measure | before | after |
|---|---|---|
| payloads that parse | 32 of 109 | 109 of 109 |
| the 77 field refusals that parse | 0 | 77 |
| of those 77, refused for a cause the gate no longer has | | 51 (39 invalid-patch, 6 unsupported operation, 3 tool failure, 2 unreadable image, 1 file not found) |
| first admit call of each run against the pristine pre-image | 0 of 10 apply | 8 of 10 apply |

The remaining 26 are still-correct refusals: 17 context genuinely did not match, 9
verification failed. The apply column beyond the first call could not be measured honestly
(a payload that now succeeds changes the tree, and much of what followed in the rollouts
was the agent retrying that same edit), so the parse and first-call numbers lead and the
failed reconstructions are kept as negative results.

Whole-file operations are admitted now: creation with an empty pre-image and post-image
hazards, deletion fenced by the workspace's own requires (deleting a namespace something
still requires refuses naming the dependents), move as create plus delete in one
transaction. The replay found two more defects three review rounds had hardened: hunk
counts were wrong in both directions (nineteen field payloads overcounted, ten
undercounted, all with bodies that said exactly what the author meant; the body now
delimits a hunk and the counts are advisory, with strict content matching still refusing
anything that does not belong), and a terminating newline annexed a phantom line once the
body took over, caught by an existing witness on the first run. Ten of twelve runs wrote the
worktree's absolute path into the header; those normalise to the relative remainder, and
anything outside the root is refused as before. 76 witness tests, 636 assertions.

Builder's flag, kept: "the count fix I shipped in round five was wrong, and only the field
showed it; both times the mistake had the same shape: I trusted a declared number over the
bytes in front of me."

## Curtain-call head (16:12Z): Gene, "assume yes from mayor" — d9afe8e9 is the true head; the refactor draft (inb-868bb7) stands as built.

## Self-inflicted, minor, logged: the gate branch was pushed before its round-four verdict
While writing the resume note I used an unquoted heredoc and the note's prose contained
backticked commands; bash executed them. Result: bridge/admit-gate pushed to b171338 before
the fourth red team returned, and a harmless failed checkout of a path that exists only on
Anvil. Blast radius: none (a branch, never main; any NO-GO lands as further commits). Ratchet:
prose fed to bash goes through quoted heredocs, and commands never sit in backticks inside
notes. Same family as the hand-typed timestamps: text and instruction confused at the
boundary where they take effect.

## Cohort rf1, pre-registered: refactoring Surgeon with Surgeon, on Anvil (16:20Z)

Gene, verbatim: "A big refactor on surgeon??? In the surgeon sweet spot?!? Do on anvil and see
what we learn!!! Time is worth it! Advancing multiple objectives!!!! Learn as much as you can
on actually important refactoring!!! How utterly meta! Captain log!!! What do you hypothesize
going in???"

The task: a real extraction from clj-surgeon's own hot files (candidates by git churn and
ls-extract exclusive-dependency size: mcp_tool.clj, intent_transaction.clj, mcp_change_buffer),
per the safe-refactor playbook: study with ls-tree and ls-deps, extract with :extract then
:extract!, compile-check, focused tests, on a worktree of main 2311cc09. Arms: N native, A
shipped (7893), B main (7889); three per wave at four cores because the Surgeon suites are
heavy; two waves; scored on actions, typed refusals, tokens, churn against the canonical
move, and an arm-independent acceptance (forms moved verbatim, new namespace loads, callers
updated, test-fast and the focused mcp namespaces at the base's failure set). Runs after z3
and z4 in the chain.

Hypotheses, written before the first arm starts:
H1. This is the first square where Surgeon beats native on returns: extraction has no native
    equivalent, so A and B complete the move in fewer non-test actions than N (predict 0.6x
    or less), because :extract! does in one call what native does in six to ten patches.
H2. Wall follows returns for once: A and B at or below N (predict within the floor, direction
    A faster), the first cohort of the summer where the tool is not slower.
H3. Churn: :extract! moves forms verbatim, so A and B ship less line churn than native,
    UNLESS the whole-file formatter fires on the changes route (the 46o defect), in which case
    B shows the l1 churn signature and the drift gate on close-losers would have refused it.
    Prediction: at least one Surgeon run shows formatter churn; native shows none.
H4. Refusals: extraction-decisions-required and invalid-intent-form appear on A and B;
    native draws zero; the mandate-free prompt (Surgeon expected but not ordered) keeps the
    fallback rate below the s1 mandate's.
H5. Correctness: all arms pass the focused suites; native is more likely to leave a stale
    alias or a missed caller that the compiler, not the tests, catches; Surgeon's extraction
    receipt names the callers it rewired.
H6. The meta finding: agents refactoring Surgeon with Surgeon will hit refusals on Surgeon's
    own source shapes (large forms, reader conditionals in the MCP layer, the Prolog oracle
    files) that the medium and large rungs never exercised; every such refusal is a bead,
    and that is the dogfood ledger the mayor kept.
If H1 and H2 hold, the winners list is confirmed on the tool's own code, and the brag is
real: Surgeon refactored itself faster than a native agent could. If H1 fails, the refactor
square is not a square either, and the skill section shrinks to the study ops.

## Gate, red team round four (16:24Z): NO-GO on one root cause, a silent no-op success

Whole-file operations, confinement, the round one-to-three regressions, the 109-of-109 parse
and the applied-diff fidelity of field payloads all held. One root cause fails: a hunk body
line the reader does not recognise truncates the hunk and the rest is silently discarded,
yet the gate commits with ok true. A context line missing its leading space, a routine
producer error, commits a no-op whose pre and post hashes are equal and whose receipt says
success; a removed line beginning with two dashes deletes one line instead of three; two
requested edits apply as one. That falsifies the design doc's own sentence "nothing is
dropped, so nothing is silently truncated", and it is the false-green class in its purest
form, in a tool whose only purpose is to not issue false greens. Field exposure measured at
zero of 109 payloads, which is why z1 did not show it and why it must be fixed before z3
anyway. Round six: an unrecognised body line refuses (:hunk-truncated, naming the line), a
post-image equal to the pre-image refuses (:no-op-patch), one terminating newline is
stripped so seven of 109 field payloads stop tripping spurious context mismatches, a
single-space V4A context line counts as blank, and a stale v1 paragraph leaves the doc.

## rf1 ethnography protocol, pre-registered (16:36Z)

Gene: "an opportunity to do some ethnographic research in terms of what works, what
doesn't, specifically where does behavior not match expected behavior. And since this is
the sweet spot, we spend a little extra time trying to optimize."

Expected behaviour per arm, written before the first rollout is read, from the
safe-refactor playbook and the skill's own workflow:
E1. Study first: one ls-tree (or outline) of the source file, one ls-deps or ls-extract on
    the target unit, before any write. Deviation: writes before study; more than two study
    calls; study via grep instead of the structural op when the op was available.
E2. Plan then execute: :extract (plan) is read, then :extract! (or the MCP extraction verb)
    is called once with the reviewed plan. Deviation: skipping the plan; re-planning after
    a refusal without reading the refusal's fields; falling back to manual moves.
E3. One structural move, verbatim: the forms land in the new namespace byte-identical, the
    ns form of the source gains one require, callers are alias-qualified. Deviation: forms
    retyped; formatter churn; callers missed; declare left behind.
E4. Verify once, proportionally: compile-check, then the focused namespaces, then test-fast
    once at the end. Deviation: repeated full suites; test runs before the move is complete;
    no compile-check.
E5. The receipt is trusted: after an extraction receipt, no re-read of the moved forms, no
    git diff. Deviation: confirmation calls after a receipt (the taxonomy's CONFIRM class).
E6. Refusals are recovered from their fields: a refusal with a next_call or a named missing
    field is answered in one call. Deviation: verb switch, probing, native fallback, or
    abandonment; the refusal text quoted verbatim in every case.
For each run the ethnographer records, turn by turn: the step the agent was on, the
expected act, the actual act, the deviation class (none, prompt ambiguity, missing
affordance, refusal not actionable, output too long to use, habit or ritual, task
misunderstanding), the verbatim evidence, and the cost in returns. Native arms are read
against the same E1 to E6 with native equivalents, so "deviation" is not synonymous with
"used the tool". Instruments: the rollouts pinned by worktree; the servers' own telemetry
via the repo's study-agent-usage, study-agent-timeline and study-agent-read-chains targets
on the 7893 and 7889 telemetry dirs for the cohort's window; the acceptance script; the
curtain-call refactor ledger from the live branch as a second, human-scale source.
Output: a friction table (deviation class by arm with counts and one verbatim example each),
the three largest deviations by returns lost, and for each a proposed fix typed as prompt,
tool affordance, refusal text, or doctrine. Then rf2: the same task with the top fixes
applied, same arms, to measure whether the deviations move. That is the optimisation loop
Gene asked for, on the square where the tool is supposed to win.

## Gate round six, and z3 launched (16:44Z)

Commit 1ca44b4, pushed on the branch: an unrecognised line inside a hunk body, or any body
line belonging to no hunk, refuses as :hunk-truncated naming the line, never a partial
apply; a post-image equal to the pre-image refuses as :no-op-patch in preview and commit, the
ratchet that makes any future truncation self-reporting; a --- line is a file header only
when +++ follows; one terminating newline stripped; a single-space V4A context line counts as
blank. Tightening the reader first dropped the field corpus to 107 of 109; the builder
checked rather than accepted it and found both payloads mix the grammars inside one file
section (a bare @@ after a counted one), which is a real marker, now honoured, corpus back to
109. 82 witness tests, 686 assertions; my own mcp-test 459 tests, 4640 assertions, only
main's failure. The builder's flag, kept: two rounds running it loosened a check and
asserted the consequence instead of testing it.

7894 restarted on 1ca44b4 (pid 2941763, ready.edn agrees). z3 launched 16:43:33Z on rung M:
Z (gate mandated, prompt now names the apply_patch grammar), F (gate OPTIONAL, the
acceptance test), N native, two waves of six, all attested with the server sha read from
the server; z4 on rung L follows, then rf1 (Surgeon refactoring Surgeon) is armed behind z4.
Predictions for z3, on record: the refusal rate on Z falls from 69 percent to under 20; the
optional arm F uses the gate at all in at least two of six runs (if zero, the gate has not
earned its call even when it works); post-write calls on Z fall below 0.5 per run; wall on
Z within one sd of native; hazards caught in situ at least once across the twelve.

## 16:49Z — rf1 installed: the extraction is chosen, the acceptance script is proven both ways

The Surgeon-on-Surgeon rung (R3) is installed on Anvil and armed behind z4. Nothing launched.

**The extraction.** `src/clj_surgeon/mcp_change_buffer.clj` (1,694 lines, 26 commits in 30 days,
third-hottest src file) loses nine forms to a new sibling namespace
`clj-surgeon.mcp-exact-verify` (next to the existing cold-verify and hot-verify): the exact
verification profile compiler, the bounded subprocess runner, the outcome classifier, and their
private helpers (`expand-command`, `bytes->hex`, `sha256-text`, `run-process!`,
`admission-unverified?`, `compile-exact-profile`, `classify-exact-process-outcome`,
`run-exact-verification!`, plus the one-line `exact-verification-visible-bytes` def). Forms 2–9
are one contiguous 156-line block with zero references back into the rest of the file, so the
move is acyclic by construction. It has real cross-file callers: `mcp_formatter.clj` uses only
the two moved fns from the change buffer, so its require must be *replaced* not kept (a stale
require is caught mechanically); `mcp_tool.clj` has four sites; the change-buffer test has ten;
seven internal sites in four staying functions become alias-qualified, which forces one
deliberate visibility change (`admission-unverified?` from `defn-` to `defn`). `ls-extract`
sizes it as one unit; the whole move is one `:extract!` call with an explicit `:forms` vector,
the cleanest possible test of H1.

**Correction to my brief.** I had told the installer to expect five failures in
`agent_routing_test` on Anvil. Measured on a pristine worktree of 2311cc09: test-fast is
`664 tests, 5691 assertions, 0 failures`; the JVM MCP suite is `377 tests, 3945 assertions,
1 failure`, the known macOS clj-kondo path assertion, which lives *inside the namespace being
refactored*. Both prompts quote it and forbid touching it; "fixing" it is a detectable spec
violation. There is no focus mechanism for that suite (the alias's `:main-opts` win), so the
bounded gate is the whole suite at 57 s.

**Acceptance (`~/acid/receipts/rescore-R3.sh`, arm-independent).** a: new ns defines exactly the
nine; b: source defines none; c1: no unqualified moved refs left; c2: no `change-buffer/`
qualified refs to the nine anywhere; c3: the five touched namespaces compile; d: churn outside
the move, mechanical (`total_clj_changed − new_ns_lines − removed_lines_matched_verbatim`),
canonical native move = 53; e1/e2: both failure *sets* equal the base's. Proven both ways: the
reference solution (native, `~/acid/receipts/rf1-reference.diff`) passes a–e; the pristine base
fails a, b, c1, c2, c3 and passes e1/e2, as it must. Cost 1 m 55 s per subject.

**Runner.** Rung R3 in v5 (and already in v3, since the z3 launch copied v5 after the patch):
repo `~/acid/surgeon-task`, a fresh clone at 2311cc09 that is *not* `surgeon-main` (7889's
checkout); gate prints both suites' failure sets on the gate line; `.cpcache` excluded from the
freeze (18 files of noise otherwise); suite counter learned the R3 commands. Arms: N native,
A shipped 64eac2e (7893), B main 2311cc09 (7889); `CORES_PER_ARM=4`, three per wave,
`"N A B|N A B"`. Chain preflight fails closed on: v3 lacks R3, prompts or scorer missing,
surgeon-task wrong tree, rf1.log present, 7893 or 7889 down (one pid-guarded self-heal of 7889,
then abort). 7889 was found down at install time and is up now.

**Design note kept on record.** The forbid-ritual paragraph (−27 % wall) is *not* in the R3
prompts. rf1 runs the pre-registered design; the paragraph is an rf2 lever, applied to all three
arms identically, so its effect is separable from the tool's.

**Prompts.** `R3-main.md` (sha256 9f5ffe4e…) names the four MCP tools, the CLI in place
(`bb -m clj-surgeon.core`), the STUDY / PLAN / EXECUTE / COMPILE-CHECK arc with `:ls-extract`,
`:extract` dry run and `:extract!`, and the two facts the agent cannot know (the CLI runs the
tree it is editing; the MCP server is a different checkout). `R3-native.md` (ccea2c6b…) names
the same arc with native tools only. Both end with the six report items and a `TURNS:` line.

Receipts on Anvil: `rf1-base-testfast.out`, `rf1-base-mcp.out`, `rf1-reference.diff`,
`acid-cohort-v5.sh.pre-R3`; worktrees `~/acid/rf1/{ref,base}`.


## 16:54Z — curtain-call dogfood: three safe extractions shipped to a branch, one stopped at the plan, one Surgeon defect found

Gene's live refactor (Kent Beck safe refactor, hot commit spots of curtaincall-cfp) ran as a
delegated Opus build in a worktree off origin/main d9afe8e9. Branch `bridge/safe-refactor-1`,
five commits authored forge-bridge with Gene as co-author, pushed, NOT merged (merge is Gene's).
My own re-run of `bin/kaocha unit` on the branch head: `1009 tests, 12833 assertions, 0 failures`
(base: 1007 / 12232 / 0; the delta is the Phase 0 pins).

| commit | what | gate after |
|---|---|---|
| 6bc2cdde | Phase 0, test-only (+133): pins writer authorization (157 writers, 16 gate-public, the other 141 answer anonymously with 302 to /), and re-runs the finite-GET sweep on a second event | unit 1009/12821/0 |
| ffa4172f | presenter-visibility rail → `views/nav_policy.clj` (3 forms, 65 lines) | voting-policy, routes-contract, ci |
| f36c08b7 | board status chips → `views/board_filters.clj` (2 forms, 17 lines) | board, routes-contract, ci |
| c1150a20 | board URL-state codec → `views/board_query.clj` (7 forms, 134 lines; callers in replay.clj, review_updates.clj) | board, polish, routes-contract, ci |
| 7b220edf | choose the three namespaces in the view-architecture guard | full unit 1009/12833/0 |

`views/review.clj` 1033→884, `views/organizer_layout.clj` 754→690, `server.clj` untouched.
The route topology golden was never re-blessed: no handler var moved.

**The gate finding (the one that matters for the ethnography).** The builder's per-extraction
gate was insufficient: the trailing full suite came back red on
`view-namespace-architecture-test`, a clj-kondo guard that pins the exact SET of view
namespaces and their allowed edges. It would have failed after the first extraction. The repo
pins TWO inventories (route topology and view-namespace topology) in different files and the
builder had read one. The correction is the gate, not the guard: the three namespaces were
chosen deliberately, as the guard's own comment says they must be; `acyclic?` never failed.
Deviation class for the protocol: **E3, verification gate narrower than the repo's invariants**.

**X3 stopped at the plan.** `:extract` (dry run) listed seven authorization files in
`:callers-to-review`, because `auth/event-manager?` and `organizer-layout/event-manager?` are
different functions sharing a name (same for `may-create-events?`). That is an authorization
review, not a mechanical move; per `security-boundary-review-before-merge` it was not executed.
The dry run did exactly what the safe-refactor arc promises: the plan surfaced the hazard before
a byte moved. The draft's own unit for X3 was also mis-specified (`person-event-path` is not
exclusive).

**X1+X4 merged on evidence.** `:extract` showed X1 alone needs `effective-blind?` promoted
public; the three together need zero promotions. The cut that needs no edit is the one that
keeps the resolver private.

**Surgeon in the winner square, tallied.** 3 `:extract!` applied, 6 `:expect`-guarded
visibility edits applied, 2 plans rejected on review, 2 refusals, 1 native fallback. The
fallback is a tool defect: `:extract!` copies the SOURCE namespace's docstring onto the new
namespace; `(form 'ns)` selects nothing; a line-rooted plan proposed replacing the entire ns
form with a bare string, which the agent discarded and hand-edited. Filed inb-4a51e5 (bead
wanted; bridge has no beads db for clj-surgeon). Every extraction pays that return until fixed.

**Pre-existing, not fixed, recorded.** `make test-all` dies at step 1 on this box
(`library(plunit)` absent from the user-local SWI-Prolog; the Prolog oracle is `unverified`,
never claimed green); `bin/test-new-mission-worktree` does `git init --bare` without
`--initial-branch=main`; `bin/kaocha unit` is order-randomized and flaked once in three
(root cause in repo code: `await-sinks!` discards the return of `await-for`, so a straggler
appends into the next test's store, exactly as the repo's own comment predicts). A suite-wide
nondeterminism fix inside a refactor branch would destroy attribution, so it is left and named.

Ledger with inverse receipts: bridge scratchpad `curtaincall-refactor-ledger.md`. Draft for
Gene: inb-868bb7.


## 17:09Z — z3 scored: the corrected gate is the cheapest arm when mandated, and nobody chooses it when free

Rung M, base ab267f9, four runs per arm, server 7894 at 1ca44b4 (dual grammar). Receipt:
`~/acid/receipts/z3-score.md` (scorer; `z3-score.py`; no runner, prompt or scorer logic edited).
Acceptance: my background call `rescore.sh z3` was wrong (it takes repo, base, outfile, diffs) and
died on `dirname: missing operand`; the scorer re-ran it unmodified with the documented arguments.

| arm | n | wall mean (sd) | model returns | tool actions | tokens | extra post-write probes | acceptance failing (of 39) | stale-onset defective |
|---|---|---|---|---|---|---|---|---|
| Z mandated gate | 4 | **285.8 (35.8)** | **13.2** | **11.8** | 763,593 | **0** | 2.50 | **0/4** |
| F optional gate | 4 | 322.0 (60.5) | 18.0 | 14.8 | 1,128,238 | 1.25 | 1.00 | 0/4 |
| N native | 4 | 356.2 (61.9) | 21.2 | 16.8 | 1,301,344 | 2.50 | **0.50** | 2/4 |

**Learning.** The dual-grammar fix works completely: 5 admit calls, 0 refusals, 5/5
`verification_status: complete`, 0 previews needed, 0 `apply_patch` fallback on a `.clj` in any
mandated run (z1: 69 % refused, 5 of 6 fell back). The mandated arm is the fastest and leanest arm
of the night, 59 % of native's tokens, 8 fewer returns per run; its entire non-gate post-write
traffic across four runs is one `git diff`. **But 0 of 4 optional runs ever called the tool**, not
late, not after a native failure, never; one enumerated it by name with a discovery filter and went
to `apply_patch` anyway. And 0 hazards: nothing was refused, so nothing could be caught.

**Caveat.** n=4. Welch on wall Z vs N: −70.5 s, t −1.97, df 4.8, p ≈ 0.049, d −1.39; a hair inside
0.05, and 0.82× the v1 single-run floor sd, so a direction to confirm, not a demonstration. The two
quality instruments disagree in opposite directions: stale-onset says Z clean and N 2/4 defective;
the acid suite says Z fails 2.50 assertions (acid-7 in all four runs, acid-9 in two) against
N's 0.50, +1.90 sd of the floor. Neither survives n=4, and which assertions acid-7 and acid-9 are
must be read before either reading is believed.

**Predictions on record (ae8517c).** 1 Z refusals < 20 %: 0/5, PASS. 2 F adopts in ≥ 2 runs:
0/4, FAIL. 3 post-write shell < 0.5 per run on Z: 0.25 non-gate, PASS (4.00 if the mandated
final gate is counted). 4 Z wall within 1 sd of N: gap 1.14× N's own sd, 0.82× the floor sd,
split, and in Z's favour. 5 ≥ 1 hazard caught: 0, FAIL. Two PASS, two FAIL, one split; the two
clean failures are adoption and hazard-catching, the two things that would justify the gate
existing rather than merely being survivable.

**Position taken.** The adoption result measures habit, not value: `apply_patch` is a trained
reflex and a tool-listing entry does not compete with a reflex. A gate is policy, not a
convenience, so the doctrine line "free-choice adoption is the acceptance test" gets a carve-out:
for a GATE the acceptance test is mandated wall at or below native plus hazards caught on real
caller bytes; free choice remains the test for conveniences. z3 passes the first half and has
not tested the second.

**Next, pre-registered.** z5-replay: feed the two stale-onset-defective native diffs of this
cohort (`z3-g1-N-1`, `z3-g2-N-2`) through `admit_clojure_patch` on 7894 as patches against the
base, preview mode. Prediction: both refused, reason naming the onset defect or the focused
suite; if both are admitted, the gate's verification profile is blind to the very defect the
predicate was built for, and that is a finding against the gate, not the predicate. Cost: two
calls. Then z4 (rung L) scored the same way; rf1 launched 17:06:48Z with a green preflight.


## 17:12Z — 46o red team: NO-GO, and the stronger check was one probe away

Executed-probe review of branch `bridge/format-form-scope` (13 probes, scratch
`46o-redteam/`, worktree byte-identical after). Verdict NO-GO; not handed to the mayor.

| finding | severity | evidence |
|---|---|---|
| F1 token BAG admits semantics-changing formatter output | NO-GO | swapped `if` branches, `(- debit credit)`, and a `:refer` symbol moved between two requires all committed with `byte_drift_from_expected 0`; the pre-46o guard refused the same bytes with drift 80 |
| F2 bound applies to an unpinned `npx @chrisoakman/standard-clojure-style`; no test executes it | NO-GO | no version, no lockfile; every test injects the command or compares the vector |
| F3 the scope "proof" (FMT-004) cannot fail for any output of `splice-forms` | fix wording | seven formatter outputs incl. `))))garbage((((` all read `byte-drift-outside-forms 0, :exact`; gaps are unreachable by construction, so the property is true, but it is not a measurement of the formatter |
| F4 `file-regions` never checks the staged candidate equals the guard's `:reference`; the post-format guard launders any earlier staging churn; unparseable candidate is a silent no-op that still overwrites the guard | fix before merge | old guard refuses 48 bytes, new guard passes with 0; latent today (no pre-formatter hook exists), fail-open for the next one |
| F6 formatter returning nil or throwing yields an UNTYPED refusal with `source_unchanged false` while the file is unchanged | fix before merge | contradicts FMT-009 |
| F5 exempt guard entries rewritten when another file has forms; F7 one temp file per form, a single 1735-fragment call exits 249 | notes | follow-up bead |
| fails-first understated: 23 assertions across 8 tests with base wiring, not 13 | record | correct the doc |

**The measured fix (p7).** An order-sensitive token stream that sorts the sibling clauses of
`:require`/`:import` as whole subtrees refuses all four corruptions, admits sorted requires and
pure whitespace, and over all 1735 top-level forms of the tree with the real standard-clj 0.29.0
produces **0 false refusals** (the real formatter changes bytes in 16 forms, the bag in 0, the
stream in 8, all eight `ns` forms). The design doc's "stated limit" (a bag cannot tell `(- a b)`
from `(- b a)`) was not a limit that had to be accepted; the alternative cost nothing and was
never measured. Same class as the gate grammar miss this morning: the reviewer's model of the
input stood in for the input.

**Andon reading.** A broken verifier that has broken nothing is the pull-worthy class, but this
one is an unmerged branch failing inside its own gate: no pull; the candidate is stopped and its
owner told. The builder is back in with the five fixes and the exact witness tests named (F1
stream + end-to-end `a-bag-preserving-semantic-rewrite-does-not-commit`; F2 pin 0.29.0 + one
gated test that runs the real binary over a committed fixture; F4 two typed refusals; F6 typed
`:formatter-failed`; F3 "guaranteed by construction", and the token check named as the ONLY
bound on the formatter).

What survived: scoping is real (l1 230 bytes vs 569), gap preservation is structural, editor
gestures stay exempt (`require_change` 0 formatter calls, drift 0), tests fail first, both suites
on exactly the base failure sets.


## 17:23Z — z5-replay and z4: the gate commits without evidence; admit-gate withdrawn from review

Receipts on Anvil: `z5-replay.md`, `z4-score.md`, `z3-score.md` § acid-7/acid-9 (scorer; new
scripts `z5-mcp-call.py`, `z5-show.py`, `z5-acid-detail.sh`, `z4-score.py`; port 7888 never
contacted; fresh worktrees `z5-replay-N1`/`-N2` at ab267f9, no live arm touched).

**z5-replay, prediction FAILED both ways.** Prediction on record (a595796): both
stale-onset-defective native diffs refused. Round 1, the frozen diffs as-is: refused
`:hunk-truncated` at the SECOND file section on `index 9c3f3ec..5eef8df 100644`; the
dual-grammar parser survives the first `index` line and dies on the next. Not a finding about the
change; a second parser hole on real git bytes. Round 2, four `index` lines stripped: both
ADMITTED, `ok true`, `hazards 0`, `verification_status partial`, `verification_reasons
["no-test-evidence"]`, `tests.ran true, tests-run 0, exit 1, report_written false`. Round 3,
commit mode: `committed true` on the same payload. **The gate wrote a defective change to disk
with zero test evidence and reported ok.** And independently: the focused profile is blind to
this defect; run by hand in the committed worktree, `68 tests, 530 assertions, 0 failures`.
Fixing the collector would not have made this a catch.

**z4, rung L, the 21-owner hoist, n=4 per arm.**

| arm | wall (sd) | returns | actions | tokens | admit calls / refusals | verification | apply_patch fallback | acceptance | churn |
|---|---|---|---|---|---|---|---|---|---|
| Z mandated | 170.2 (85.7) | 9.25 | 7.25 | 416k | 9 / 3 (33 %) | partial ×6, unverified ×3, **complete ×0** | 0/4 | 4/4 | +59/−34 |
| F optional | 156.2 (24.7) | 9.25 | 7.50 | 378k | 0 | – | 1.75 | 4/4 | +59/−34 |
| N native | 148.8 (29.0) | 9.25 | 7.50 | 369k | – | – | 1.75 | 4/4 | +59/−34 |

Total convergence: identical returns, canonical churn in all twelve diffs, 12/12 acceptance,
Welch p 0.64 and 0.69. On a tightly specified mechanical refactor the tooling stops mattering.
Every one of Z's four commits landed at `partial`/`no-test-evidence`: four writes to disk with
no passing test behind them. Refusals: `patch-does-not-apply` ×2 (a `reducer_session.clj` hunk),
`invalid-admit-request` ×1 (`expect_pre_sha256` must name exactly the touched files). Free-choice
adoption 0/4, and none of the four even ran the discovery probe; **0 of 8 across z3 and z4.**
Predictions: 1 PASS (wall within 1 sd), 4 FAIL (refusals 33 %, adoption 0, post-write 0.50 not
below 0.50, hazards 0). stale-onset: all twelve UNDETERMINED, correctly; rung L touches no
`onsetReady` code.

**acid-7 / acid-9 named.** acid-7: the server must render the heartbeat interval as a literal
(`?hbms=3100` → `3100`; default `2500`); every Z run rendered neither (4/4; F 2/4; N 1/4).
acid-9: both buttons' `.disabled` derived from `recording`; two Z runs gated on `enabled` or
wired only CANCEL. stale_onset asks "is what you wrote correct" (control-flow ordering inside
`onsetReady`); acid-7/9 ask "did you write all of it, on the server side". The Z arms wrote
correct code and less of the spec surface. One quality number covering both does not exist in
this data; the two meters are not near each other.

**Decisions.** (1) `bridge/admit-gate` 1ca44b4 WITHDRAWN from the mayor's review queue: a
verifier that writes on `partial` can falsely authorize; unshipped (my branch, my port), so no
cord. Fix dispatched on the branch: commit is a typed refusal `:verification-incomplete` on
anything but complete; "runner invoked, exit non-zero, no report" is `:verification-runner-failed`
carrying the runner tail, never partial; git extended headers accepted (binary a typed refusal);
the two frozen native diffs become real-bytes fixtures; the `expect_pre_sha256` refusal lists both
file sets; root cause of the missing evidence on z4/z5 to be established from the Anvil
worktrees (candidate: `bin/gate-report.clj` is installed per arm by the runner and absent from a
fresh worktree; the payload must name the missing thing). (2) Hazard-catching cannot come from a
test profile: the defect the predicate names passes the focused suite. The stale-onset predicate
belongs inside the gate as a structural hazard detector over the patched forms, a typed refusal,
the rung-5 ratchet, and the homoiconic square. Filed for the next gate round, after the fail-open
fix. (3) z3's mandated-arm win stands as stated (its five commits were `complete`), with the new
caveat that the same server admits on `partial` elsewhere; the win is on cost, not yet on safety.


## 17:36Z — the reproduction program, pre-registered (Gene: "You have the alchemy formula that should work -- let's goooo!")

Gene's question was whether z3 was lightning in a bottle. Half right: one rung, n=4, p 0.049,
and flat on the next rung. But the mechanism is known, *the gate wins by deleting native's
post-write ritual*, so it predicts where it reproduces and where it must not. Three tests, all on
the FIXED gate (commit refuses on anything but complete verification; git extended headers
accepted; real-bytes fixtures), server 7894 restarted at the fix sha before any arm starts. rf1
ethnography and rf2 (structural route) proceed separately; these three are about the gate.

| run | rung | arms | prediction (pre-registered) | falsifier |
|---|---|---|---|---|
| z6 | M (controls/mic-gate), 2 cores | Z gate mandated vs N, n=8 each | Z wall ≤ 0.85 × N; Z refusals < 10 % of admit calls; ≥ 90 % of Z commits at `complete`; Z extra post-write probes 0; stale-onset Z 0/8 | Z wall within the floor sd of N, or any Z commit at `partial` (the fix makes that a refusal, so a `partial` commit is a fix failure) |
| z7 | R3 (the rf1 extraction, Surgeon's own tree), 4 cores | Z gate mandated vs N, n=3 each | the ritual-heaviest task of the night (native ran the suites 14–15 times): Z suite invocations ≤ 6 (N 14–15); Z wall ≤ 0.75 × N; refusals < 20 % despite five-file patches; acceptance a–e equal | Z suite invocations ≥ 10, or Z wall ≥ N: the gate does not remove a ritual the agent runs by habit rather than by need |
| z8 | L (21-owner hoist), 2 cores | Z vs N, n=4 each | CONTROL: flat; Z within 1 sd of N; 0 commits at `partial`; canonical churn 8/8 | a Z win here would mean the mechanism is wrong (there is no ritual to remove on L) |

If z6 and z7 win and z8 stays flat, it is a mechanism. If only z6 wins, Leyden was right.
Apparatus for z7 (new): `R3-gate.md` prompt (native tools plus the mandated gate, the apply_patch
grammar named, the R3 spec and the two forbidden edits verbatim), a focused-test profile for the
Surgeon tree so the gate has evidence to collect (the report-file wrapper and namespaces named in
the runner's R3 rung), and the fix-sha attestation on 7894. Chain: z6 → z7 → z8, armed on GO-Z6,
which is created only after the fix's suites are green and 7894 is restarted at the fix.


## 17:45Z — rf1 scored: Surgeon refactoring Surgeon loses on every cost axis, ties exactly on quality

Receipt `~/acid/receipts/rf1-score.md` (scorer; `rf1-score.py`; acceptance `rf1-rescore.out`,
"rescore-R3 done"; no `.clj-surgeon` telemetry dir existed in any rf1 worktree, so the call
ledger is reconstructed from the rollouts).

| arm | n | wall mean | returns | actions | tokens | suites | MCP / CLI calls | native patches | acceptance a–e | churn vs 53 |
|---|---|---|---|---|---|---|---|---|---|---|
| N native | 2 | **326.5** | **22.0** | **17.5** | **914,848** | 14.5 | 0 / 0 | 4.5 | all PASS 2/2 | **53** |
| A shipped 7893 | 2 | 405.5 | 31.0 | 23.0 | 1,946,458 | 16.0 | 5.5 / 6.0 | 1.0 | all PASS 2/2 | **53** |
| B main 7889 | 2 | 460.0 | 38.5 | 32.0 | 2,419,042 | 14.0 | 2.5 / 15.5 | 1.5 | all PASS 2/2 | **53** |

**Learning.** The tool worked and was used: 4 of 4 Surgeon runs called `:extract!` (through the
babashka CLI every time; the MCP extraction verb never), the move landed verbatim, and
`churn_outside_move` is exactly canonical in all six diffs; the l1 formatter pathology did not
recur. It lost anyway, on every cost axis, and the rollouts show why: native landed the entire
new namespace with ONE `apply_patch` `*** Add File`, one return after its first read; the
structural route needed 2–5 returns (study → dry-run plan → execute) to reach the same point.
Then the shipped build refused **8 of 8** MCP require rewirings (`invalid-compact-relation` ×6,
`require-change-unprovable` ×2) and both A runs fell back to a native patch for that step; main
got 1 of 3 through (`invalid-intent-form` ×2). One B run spent 13 `:mv` calls reordering forms
after the extract. Ordering is unanimous run by run: 311, 342 < 382, 429 < 455, 465.

**Caveat.** n=2 per arm; Welch df 1.2–1.7; the p-values support direction only. The g2-A-1 gate
scare (1 error in `prepared-confirm-preview-commit-and-replay-cross-the-real-http-wire`, 3931 of
3945 assertions) is environmental: the quiet-worktree rescore passes e2 with 0 errors; the diff
touches no wire code; the sibling A run passes with the identical change.

**Hypotheses (aee6e8e).** H1 FAIL and inverted (returns A/N 1.41×, B/N 1.75×; study→move native 1
return, Surgeon 2–5). H2 FAIL (A +24 %, B +41 % wall). H3 FAIL both halves (no Surgeon churn
advantage; no formatter churn either). H4 PARTIAL (refusals A 8, B 2, N 0; named types half
right: `extraction-decisions-required` never fired, `invalid-intent-form` did). H5 half (all arms
pass; native left no stale alias). H6 PASS (two refusal classes no rung M or L cohort produced,
both while rewiring requires in Surgeon's own MCP layer: beads).

**What this does to the doctrine.** The winners list (`:extract!`, `:rename-ns!`,
`require_change`, `within`) was promoted on churn and refusal receipts, never on a wall or
returns receipt against native on a real extraction. This is that receipt, and it is a loss. Not
flipping house rules on n=2, but the list carries a measured caveat from now: **the refactor arc
is itself the cost.** Study, plan, execute are three returns; native skips all three because the
model already holds the target in its head. The win math says a structural verb wins only if the
whole refactor (move + caller rewiring across files + ns requires + compile check) collapses into
ONE return whose response is the verdict, i.e. the extract verb fused with the gate, the
"think compile bang" shape. rf2's top-3 fixes come from the ethnography (running); the two new
refusal classes are the beads H6 asked for (inbox to the mayor for creation).


## 17:47Z — 46o verification round: GO-WITH-FIX; the census certified the repo's style, not the check

Red team re-executed its own probes against the fixed branch (15 probes): F1, F2, F3, F4, F6 all
CLOSED at the layer they belong to (pure predicate with witnesses; typed laundering refusals that
install no guard; typed `formatter-failed` on nil and throw; one pinned version constant derived
at both call sites; the real binary executes in CI; `scope-drift`'s docstring now reads "a
self-test, not a proof… nothing about the formatter is bounded by it"). The e2e ns-form deviation
judged acceptable: the predicate is witnessed on real ns forms and at `format-scoped-candidates!`;
only the wire layer uses a clause list in a defn, forced by the closed-loser constraint. String
probes 9/9 correct: the real formatter never touches bytes inside a string, regex, or docstring.

Two NEW findings from the real pinned 0.29.0 on ordinary source, both GO-WITH-FIX, both with a
zero-cost fix measured 12/12 and 0 false refusals over 1738 forms (probe p10):
- **N1** the formatter rewrites `;;foo` → `;; foo` (and `;;;foo`, end-of-line `;;t`); the stream
  check refuses it as `format-altered-form`, killing the entire transaction on the real wire route
  with a message accusing the formatter of changing code. The 1738-form census read 0 because this
  repo never writes that comment style: **the census measured the repo's style, not the check.**
  Fix: normalise whitespace after a comment's semicolons in `sig`; fixture gains the shapes the
  census cannot supply (`;;no-space`, a comment inside `:require`, a multi-line string).
- **N2** the clause sort treats comments as independent siblings, so a comment inside
  `(:require …)` can be reattached to a different clause and commit; the real formatter moves a
  comment WITH its clause. Fix: sort clause groups (leading comments + the clause).

Builder dispatched for the last round; then commit, push, hand to the mayor with both red-team
receipts. F5/F7 deferred to a follow-up bead by agreement.


## 17:52Z — rf1 ethnography: the tool cuts beautifully and cannot sew

Receipt `~/acid/receipts/rf1-ethno.md` (533 lines; six rollouts turn by turn; server telemetry
on 7893/7889 as second source; three deviation classes added to the protocol's seven: `refusal
false`, `silent accept`, `harness artifact`).

**Returns from first read to the move landed:** N 10, 9 · A 16, 15 · B 29, 16. Native's cut is
4–5 patches; the structural cut is ONE `:extract!` with a hash-fenced receipt, and it genuinely
does in one call what native does in two. Everything downstream of the cut is where native wins:
the ns header, the imports, the alias, the visibility flip and the twenty-three caller sites are
one native patch and zero structural calls that succeed. Server telemetry: 7893 logged 8
`apply_clojure_changes` attempts across two runs and committed 0; 7889 logged 3 and committed 1.
Eleven structural mutation attempts, one commit, 9 %. Four of four structural runs finished their
rewiring with `apply_patch`.

**Ranked deviations (returns lost).** 3.1 `:ls` refused the file `:extract!` had just written,
`{:error-type :forward-reference-analysis-failed :exit 2 :diagnostic ""}`; the agent "fixed" a
defect that was not there with eight `:mv` pairs, 15 returns, 38 % of B-g1; `inspect_clojure`
outlines the same bytes (same sha256) cleanly; three other runs shipped that order unchanged and
green. 3.2 the `edit_clojure` refusal ladder, one fact per refusal, four returns per A run, and the
fourth is FALSE: `require-change-unprovable` on the `:as … :refer […]` require `:extract!` itself
wrote, and on `[clojure.test :refer …]` the call never touched; it ended the structural route in
2 of 2 A runs. 3.3 `:extract!`'s header is the entire content of every native fallback: source
docstring copied, unrelated imports copied, `:refer` style the spec forbids, `:public-forms`
silently accepted and ignored (2 of 2), dead requires left in the source, and `:callers-to-review`
named correctly and rewired never. 3.4–3.8 arm-independent: suite-poll ping-pong (2–8 returns),
`.cpcache` cleanup (2–4), an absent skill hunted with an unbounded `$HOME` walk, the sandbox
rejecting `rm -rf <abs>`, and `TURNS: 1` reported by all six (a null instrument).

**Instrument withdrawn.** The suite-invocation counts 15/14/14/18/14/14 were `grep -c` over the
run log, which contains the prompt (10 hits before any act) plus `-Spath`, rejected commands and
a `ps | rg` watchdog. Counted from the rollouts: **every run executed each suite exactly once, at
the end, as specified.** The E4 "repeated suites" deviation did not occur; the structural route
removed no verification return and could not have (e1/e2 pin both suites). Counter fix dispatched
to the runner. Also: `rf1-rescore.out` covers all six runs (54 lines, six ids); the ethnographer's
"2 of 6" note was a stale read; the scorer's 6/6 acceptance stands.

**Four self-inflicted refusals, H6 SUPPORTED, all beads:** the `:ls` false refusal; `require_change`
refusing Surgeon's own emitted require; `:extract!` silently ignoring an unknown argument;
`:op :mv :form bytes->hex` unquoted eaten by the shell as a redirection.

**rf2, dispatched as a build on `bridge/rf2-extract-rewire` off main:** rf2-1 `:extract!` with
`:rewire-callers` default true (caller's form order, no docstring copy, pruned target imports,
`[ns :as last-segment]` never `:refer`, dead source requires/imports removed, every caller
rewired incl. replace-when-only-moved-vars, `:public`, typed refusal on unknown args; acceptance =
byte-identical to `rf1-reference.diff`); rf2-2 `:ls` never fails an outline that parses;
rf2-3 `invalid-compact-relation` carries `expected_shape`, provability scoped to the entries
named; rf1's exact payloads as fixtures. Prompt edits identical across arms: single `wait`,
`.cpcache` is generated, ignore the absent skill, count tool calls. Ethnographer's traced
prediction for the shipped path with the verb: **7 returns to move vs native 9 (0.78×)**.

**The claim under it, sent to the fleet (Sol + Opus, independent, to attack):** every tool call
that is not the whole intent adds a return, because the model already holds the target and reads
faster than it calls; tools win only when they take a complete intent, compute ALL its mechanical
consequences, and return one verdict; homoiconicity is what makes the consequences computable;
the winnable square is "intents with large mechanical consequence fan-out, one call, one
verdict", not "refactoring". Gene: "we are pretty darned sure this is a winnable test… activate
brainfleet to confirm… is this where we grind it out until we figure out how to win?"


## 17:55Z — fleet round one on rf1 (Sol and Opus, independent): winnable at ~0.85×, my claim corrected, grind-once not campaign

Both brains got the same five questions with `rf1-score.md` and `rf1-ethno.md` attached
(bridge scratchpad `fleet/`; Sol via `codex exec -m gpt-5.6-sol`, Opus via a subagent).

| question | Sol | Opus |
|---|---|---|
| winnable? ceiling | yes; 6–7 returns to move, 17–19 total, wall 0.82–0.90× | yes, narrowly; 6–7 / 17–19 / 0.82–0.88×; native's ~12.5-return tail is untouchable by any editing verb |
| my "one intent, all consequences, one verdict" claim | directionally right, overstated; the GATE (a fragment of the intent) already won; homoiconicity makes syntax tractable, not consequences computable; amended: *tools win when they compress the intent-to-trusted-verdict path by more returns than discovery + invocation + repair + distrust cost* | wrong in four places: fan-out must be UNREAD fan-out (apply_patch collapses read fan-out for free); already falsified by the gate; homoiconicity is not the mechanism, static analysis is (no Clojure moat); "one verdict" fuses the winner (verdicts) with the loser (mutations) |
| rf2 prediction | move in 7, wall ≈ 0.88×, **P(beats native, n=3) 65 %**; loss mode: post-success distrust | move in 9 (8–12), wall ≈ 1.00× (0.90–1.15), **P 30 %**; the verb asserts 23 rewrites the agent never read, so the audit grows with the saving |
| grind or trap | **run rf2 once as kill-or-promote**, pre-registered criteria (≥2/3 paired wall wins, fewer returns, zero fallback, equal acceptance, no task-specific code); if it clears, go straight to unseen extractions | **trap as a grind**; ship the verb (fixes four real defects), run rf2 once, then move to uncapped shapes |
| decisive next experiments | (1) paired rf2, 6 runs; (2) three UNSEEN extractions of small/medium/awkward fan-out, 6 runs, *highest value*; (3) receipt-authority ablation, 6 runs | (1) **ritual strip on native alone**, 3 runs: N 22 → 15–16 returns, 326 → 255–275 s, which would erase the 0.78× target before rf2 runs; (2) **unread fan-out at scale** (require/alias change across ≥20 namespaces, ≥100 sites), 8 runs, *most decisive*: tool 0.60–0.75× and improving with N, or the program ends; (3) rf2 once, 6 runs |

**Agreements I accept.** Ceiling ~0.85×, not H1's 0.6×; n ≥ 6 per arm to see it (d ≈ 1.8 on the
measured sd); distrust after `ok` is the top loss mode (E5 fired 4/4); rf2 once, never a
campaign; the three corrections to my claim (the gate contradicts "only complete intent";
UNREAD fan-out is the axis; static analysis, not homoiconicity, is the mechanism).
**The disagreement, which is the signal:** 65 % vs 30 % on rf2, and Opus's observation that the
largest return sink in rf1 (ritual, 13–15 returns per run) is unowned and larger than the whole
A−N gap. Round two dispatched: each attacks the other's number.

**Position taken.** Ship the verb regardless. Ritual strip on native FIRST (3 runs). Then rf2
once at n=6 with identical stripped prompts, Sol's promotion criteria. Then unread fan-out at
scale, which is bead q5z, the fan-out intent verb shelved as a dead end this morning; if a key
resurrects a dead end, it is that one. The gate program (z6/z7/z8) runs in parallel as the one
measured win.


## 17:56Z — fleet round two: the disagreement reduced to two observables, both pre-registered

Each brain attacked the other's answer (bridge scratchpad `fleet/opus-answer.md` § Round two,
`fleet/sol-round2.md`).

| item | Sol | Opus |
|---|---|---|
| P(rf2 beats native, n=6, ritual-stripped prompts both arms) | **60 %** | **35 %** |
| the one assumption behind the gap | atomic closure succeeds routinely and its receipt substitutes for most independent checking | a first-shipped `:rewire-callers` mints refusals or fallback (base rate: 10 refusals in 4 runs on a mature path; dead-require pruning not decidable) and the audit grows with the unread rewrites |
| **settling observable A** (rf2 rollouts) | native `apply_patch` calls landing functional bytes after the extract: **0 in 3 of 3 → Sol** | **≥ 1 in 2 of 3 → Opus** |
| **settling observable B** (rf2 rollouts) | returns between the `extract!` ok receipt and the first compile/test: **≤ 1 → Sol** | **≥ 3 → Opus** |
| ritual strip on native, prediction | 15–17 returns, **285–305 s** (0.87–0.94×); a single `wait` removes polling calls, not suite runtime | 15–16 returns, **255–275 s** (0.82×) |
| same claim? | overlap, not identical: Sol's is an acceptance rule (cost identity, terms measurable only after the run); Opus's carries a sign (mutations lose, verdicts win), "not established by four distrustful runs" | concedes Sol's axis: reads that do not CONVERGE (indirect, macro-mediated, generated, ambiguous ownership), not reads that are long; concedes "conservative mechanical closure, not ALL consequences" |
| discriminating experiment | native vs atomic mutator with a prompt-authorized covering receipt on an unseen high-unread-fan-out transformation | three arms on one task: native / tool-mutates-with-receipt / native-mutates + tool-verifies-only; Opus predicts verify-only ≥ native > mutate; Sol predicts mutate+covering-verdict wins |
| decisive next test | **three unseen extractions** (small / medium / awkward fan-out): does the closure and receipt generalise, or is rf2 compiled for R3; the 20-ns require cohort "repeats an already-measured sweet spot" | **scale slope** (one shape, N = 20+ namespaces, 100+ sites): "does a square exist at all" is prior to "does this verb generalise"; a monotone prediction is identifiable at n=1 per point |

**Decisions.** (1) Observables A and B are the pre-registered readout of rf2; wall is reported
but does not adjudicate the mechanism. (2) rs1 (ritual strip, native only, 3 runs) runs before
z6; both predictions above are on record. (3) Order after rf2: the scale slope first (bead q5z,
the fan-out intent verb, resurrected), then Sol's three unseen extractions as the overfitting
guard. (4) The three-arm discriminator is already assembled from parts: z7 (gate on R3) is the
verify-only arm, rf2 is the mutate-with-receipt arm, N is native; read them together.


## 18:10Z — big game only: the scale slope (sl1) is pre-registered; the 15 % square is closed after one run

Gene: *"Where do we find the 5-10x gains? If this isn't one of them, let's not waste our time on
15% gains. Hunt big game, not rodents that aren't even nutritious -- juice not worth the squeeze.
So where do we get maximum payoff?"*

**Position taken.** Wall is returns; native does a known move in ~10 returns plus a ~12-return
tail; no per-call editing verb can beat that by more than a few returns, so per-call verbs are
rodents by construction. rf2 gets its one kill-or-promote run because the verb is built, and no
more. Five-to-ten-times lives only where the ratio is unbounded: (1) reads that grow with the
codebase and the tool's calls do not (unread fan-out); (2) agents running unsupervised behind a
mechanical verifier, the multiplier measured in Gene's hours (twelve arms in eight minutes; the
curtain-call extractions shipped during a flight); (3) defects that never ship, each deleting a
later debugging loop. Order: the gate chain (cheap, and it is the verifier (2) needs), then the
slope, then hazards.

**The slope, designed (Opus; spec committed as
`docs/observations/2026-09-02-slope-spec-sl1.md`).** Two findings changed it: the existing fan-out
verbs are disqualified by their own schema (`symbol_migration` takes an O(N) agent-computed
per-site list with match counts: "authority, not discovery"; it removes no read and adds a
counting duty; at N=80 the tool arm loses by construction), so the minimal q5z, `alias_migration`,
one call, payload constant in N, receipt O(1) in N, must be built first; and the best real fan-out
we own (curtaincall-cfp `store/` across 74 files) aliases identically in 68 of 68 with zero
`:refer`, so its answer is one `sed` and it yields one point: kept as the adversarial anchor
native should win, not as the slope. The slope runs on a generated repo with decoys (locals named
like the alias policy, strings and docstrings containing the old name, `#_`, `#?` branches,
colliding aliases) and a manifest of protected-region hashes so a `sed` answer fails predicate 3.
Transformation (a): retire a fan-out namespace and rename its var, alias chosen per file against
that file's own bindings, so files-that-must-be-read grows with N while sites-per-file is held.
Budget 14 arm-runs: N = 5/10/20/40/80 × native/tool at n=1 (the readout is the slope), control C
(5 files × 48 sites: separates reads-grow from patch-size-grows), anchor R.

**Pre-registered predictions (returns to done), both brains:** Sol: native 8→13 across N=5→80,
tool flat 6–7, ratio 0.75→0.54 ("never big game"); Opus: native 8→30, superlinear past N≈20,
ratio 0.75→**0.23**. **Ends the structural-editing program:** native wall at N=80 within 1.3× of
N=5, or the ratio not monotone decreasing, or ≥ 0.85 everywhere, or any q5z fallback to
`apply_patch` on functional bytes, or refusals > 20 %. **Flagship if:** ratio ≤ 0.35 at N=80,
monotone on ≥ 4 of 5 points, wall ratio ≤ 0.50 at N ≥ 40, zero fallback, acceptance green both
arms at every N. Honest prior: ~1.5×. Run once; a flat slope closes the square.

**Dispatched.** q5z build on `bridge/q5z-alias-migration` off main (MCP verb `alias_migration`,
tool-side discovery, per-file alias against the file's bindings, atomic through the kernel, O(1)
receipt, typed refusals with next_call, fixture of 12 namespaces with every decoy, atomicity
witness); Anvil apparatus (generator, canonical + manifest per N, `rescore-FAN.sh` proven PASS on
canonical and FAIL on base and on a `sed` answer, prompts per N and arm with the strip lines,
rung FAN, chain-sl1 on GO-SL1 requiring 7895 attested == Q5Z-SHA).

**Apparatus receipt.** `restart-7894-at.sh` aborted at 2cc52fa with `Unreadable arg: "{:command"`:
its launch line built the focused-test argument through an unquoted command substitution, so the
shell split the EDN map into words; the original start script quotes it. Patched to a bash
array (`.pre-quotefix` kept); restart re-run; chain-z6 stayed fail-closed throughout. rs1 (ritual
strip, native × 3) launched 18:08:05Z with the per-arm session leader and the executed-suite
counter in place; formatter branch `bridge/format-form-scope` pushed at 62981ee and handed to the
mayor.


## 18:16Z — tweezers before the woodchipper: Gene's critique, two brains, one protocol, and GO

Gene, verbatim: *"to discover the novel forms of tools we need, running on anvil (multi-armed)
seems ridiculous. That is wood chipper and chainsaw work. we need tweezer work, nearby, fastest
feedback, in REPL. We do work on our side of the anvil interface, highly interactive. critique. I
recommend doing surgeon refactor in REPL, maybe with watcher (like
/live-writing-session-commentary generating commentary and metacognition, on opus or sonnet?)
And when we discover pattern that feels good, where wins are demonstrated, then we put it into
the anvil test multi-arm battery?"* Ratifying the meter: *"the job of the live writer observer is
to provide at the meter measurements, to ensure that 'feels good' is true, but also that 'was
actually faster' -- ask the runner, but also look at stopwatch."* Then: *"Captain log. Go!"*

**Verdict (mine, then both brains): adopt with changes.** Anvil verifies discoveries; it does not
manufacture them. rf1 pointed six arms at a verb nobody had executed once: a missing ten-minute
smoke test, not a missing methodology (Opus). "Feels good" is what promoted the winners list that
lost 1.4–1.75× when measured, because a human at a REPL absorbs the returns that cost an agent
its wall (Sol: *"the REPL can optimize the wrong product: a tool that feels excellent when
operated by its author"*). The driver's blind spots are choice and epistemic state: noticing the
tool, guessing the schema, learning from a refusal in one fact, trusting the receipt, abandoning
it. Two cheap instruments restore them inside the loop, neither a battery: the naive-reader probe
(a fresh model gets only the tool's output bytes: "what is your next call?"; ≥ 80 % determinable)
and the cold-agent shadow / free-choice arm at n=1 (five minutes; tool present, not mandated).

**Correction accepted.** I told Gene the rf1 ethnography "was tweezer work done post hoc". Opus:
false; it read six agent rollouts, and 8/8 refusals, 9 % commit rate, 4/4 re-reads after `ok`,
13–15 ritual returns are agent behaviour a hand session never emits. Hand-drive replaces the
smoke test, not the ethnography. Also accepted: arithmetic (G0) before both REPL and apparatus,
the day's highest-value finding was paper; and a watcher with no exit criterion inherits the
commentary skill's four runaway scars, so it carries a 60-minute cap and an idle stop.

**Protocol**: `docs/tweezer-loop.md` (52ca6b1, amended). Watcher = Sonnet, event-driven per call,
six fields (intent, expected vs actual, deviation class, return-tax, context-privilege), the
transcript's timestamps are the stopwatch, running totals against native's benchmark for the same
task (rf1 native: 9–10 returns to the move, 20–24 total, 311–342 s); Opus once at the close for
the shape spec. Ladder G0–G6, ≈ 52 min pre-battery. rf2: G0–G5 by hand, then its n=3 cohort. q5z:
hand-drive at N=5, then the slope as designed (its readout is the battery). Gate cohorts already
queued run untouched.

**GO.** Tweezer session 1 starts now: the rf1 extraction by hand on `~/src/clj-surgeon-tweezer`
(branch `bridge/tweezer-1` at 2311cc09), driver = bridge, tools = the bridge's own Surgeon MCP
(7888 local) and the tree's CLI, expectation stated before every call, watcher on the session
transcript. Receipt to follow under `docs/observations/`.


## 18:28Z — rs1: the ritual strip removed a third of the returns and none of the wall

Receipt `~/acid/receipts/rs1-score.md` (scorer; `rs1-score.py`; acceptance rescore deferred while
z6 runs, the gate lines are identical to base on both suites for all three runs).

| | rf1 native (n=2) | rs1 stripped native (n=3) | delta |
|---|---|---|---|
| wall | 326.5 s | **327.7 s** | +0.4 % |
| model returns | 22.0 | **14.3** | −35 % |
| tool actions | 17.5 | 11.0 | −37 % |
| tokens | 914,848 | 668,887 | −27 % |
| suites executed | 2 | 2 | 0 |
| `.cpcache` cleanup returns | 3.0 | **0.0** | −3 |
| poll returns | 5.0 | 4.0 | −1 |
| skill-file cells | 1.5 | 1.67 | +0.17 |

**Predictions (a40fc3e): all four FAIL on wall**, Opus 255–275 s (+19 % off), Sol 285–305 s
(+7 % off); returns 4 of 6 individual observations inside the bands, both means just below.
Sol's mechanism is confirmed on the number that matters: polls and housekeeping are cheap
returns; the wall is suite runtime, and both cohorts executed exactly two suites.

**Which lines were obeyed.** `.cpcache` fully (the whole −3); `TOOL CALLS` fully and accurately
(14/13/15 vs counts 15/13/15; the old `TURNS:` line produced `1` from every run); the single wait
partially (still a `write_stdin` + `wait` pair per suite; one rf1 run already did it right
without the line); the absent-skill line NOT AT ALL (3 of 3 still read skill files; the
`$HOME`-walk clause was inert, the behaviour was already absent). Same asymmetry as cohort R:
**prohibiting a named artifact removes exactly the returns it names; telling an agent that
something is already known does nothing.**

**Consequences.** (1) On R3 wall is suite-bound: returns and wall are two meters and are reported
separately from here on; "wall = returns" holds where the tail is small (rung M) and not where
two JVM suites dominate. (2) rf2's native benchmark re-bases to **14.3 returns / 328 s** with the
strip prompt on both arms. (3) The suite-count instrument correction stands: rf1's 14–18 were
withdrawn on the counter's own authority; every run executed each suite once.


## 18:29Z — tweezer session 1, driver's receipt: three one-line fixes at the REPL, one confirmed false refusal, move at call 14

Branch `bridge/tweezer-1` at 92dc72c (pushed): the rf1 extraction driven by hand at the nREPL of
the tree (port 40179), Surgeon's own functions called directly, expectation stated before each
call, the watcher metering the transcript. Native benchmarks: 9–10 returns to the move and 20–24
total unstripped (rf1), 14.3 total stripped (rs1); 311–342 s.

| call | act | result |
|---|---|---|
| 1 | MCP plan-extraction on the bridge server, workspace_root = worktree | one return: nine forms, required public form, four internal owners, four caller files (one a false candidate in `dev/`), a hash-bound next_call; **target-ns derived from the path relative to the server's root, not the workspace** (`clj-surgeon-tweezer.src.clj-surgeon.mcp-exact-verify`): tool defect, unsubmittable as served |
| 2 | rg the sites + server root | 16 external sites in 3 files; the `dev/` candidate has none |
| 3 | CLI `:extract!` | cut landed, 170 lines; header defects exactly as rf1: docstring copied, 4 unrelated imports, `:refer` on three forms, promoted form left `defn-`, dead source requires kept |
| 4 | REPL: source of `compile-target-header` | `:minimal` proves and prunes REQUIRES, then installs them into a rename of the whole source ns form: docstring and every import ride along |
| 5–7 | REPL: live patch (strip docstring, prune imports to classes the forms mention), one wrong-position guess, one probe, re-run `execute!` | header = the reference's: no docstring, two imports, 167 lines |
| 8–9 | REPL: start the MCP server in-process | refused to load: `admission-unverified? is not public`, the extract's own `:refer` to a form it left private |
| 10 | REPL: `plan` source | `compile-plan` supports `:public-forms` and `:derive-required-public-forms` and computes `missing-required-public-forms`; **`plan` drops both keys**, so the capability is unreachable from CLI and REPL alike; the tool knew the promotion was mandatory and shipped a private form |
| 11 | REPL: patch `plan` to forward the keys, re-run with derivation on | promoted form is `defn`; buffer loads |
| 12 | MCP `edit_clojure` on 7888: symbol_migration 23 sites with owners + require_change 4 files (add alias; replace in formatter; remove the `:refer` require in the source) | **refused `require-change-unprovable` at files[2]**, the test file, for the `[clojure.test :refer …]` entry the call never touched: rf1's ladder confirmed with a perfect payload, 93 ms |
| 13–14 | one mechanical sewing pass (23 sites, 4 ns forms, dead requires and imports), retry after my own count check | all five namespaces load; diff vs `rf1-reference.diff` differs in 12 whitespace lines |
| 15 | both suites once | test-fast 664 tests 0 failures; mcp 377 tests, the 1 known macOS assertion |

**Shape findings, all agent-invisible until the tool's text says them:** (1) `plan` drops the
visibility keys (one-line fix; the CLI never exposed them, hence rf1's "silently ignored"); (2)
the header compiler prunes requires but not imports or the docstring (one function); (3) the
MCP server derives the target ns from its own root, ignoring `workspace_root` (defect; bead);
(4) `require_change` provability must be scoped to the entries named (rf2-3, confirmed); (5) the
extract's `:refer` to internal callers is what forces the ladder: rewire the seven internal
sites instead (rf2-1). With (1), (2) and a rewiring extract, the hand path is plan → extract →
compile → suites: **four returns**. Watcher's count for the session window: 27 returns, 503 s,
including housekeeping calls; its per-call records and close are the meter's receipt and follow.


## 18:37Z — session 1, the meter's receipt: 35 returns and 797 s, against the driver's "15"

The watcher (Sonnet, on the transcript) closed session 1 at **35 model returns / 797 s wall**
(31 / 697 s to the committed move) against native's 20–24 / 311–342 s, and against the driver's
own closing line "15 returns". Both numbers are true under their definitions: the driver counted
only calls it considered the session's and excluded the mayor reply, log commits, the watcher
spawn, a monitor stop, a tool-schema load, and an rs1 log write interleaved mid-session; the
meter excluded nothing. **The meter's number is the one that counts: a cold agent has overhead
too and does not get to call it something else.** Gene's brief for the watcher was "ask the
runner, but also look at stopwatch"; the runner undercounted by more than half on its first
outing. Ratchet: the driver's self-count is never the reported figure; the session-2 watcher
separates housekeeping calls explicitly so the count is auditable, not argued.

The watcher's shape observations, nine deviations, the ones that matter for the tool: three
SILENT failures that no receipt surfaced (a live patch that did not fire while the receipt's
summary counts stayed identical; an in-process server start that produced no output, no error,
no port; a supplied option, `derive-required-public-forms`, ignored with an identical receipt), so
**a receipt must name every option it honoured and refuse the ones it does not know** (rf2-1
vii, now with three receipts behind it); the one agent-visible refusal was the MCP
`require-change-unprovable`, which named a remedy; the target-ns derivation from the server root
(inb-a0cb65). Watch file: `bridge/tweezer-1` `docs/observations/2026-09-02-tweezer-session-1-watch.md`.

Session 2 (the explicit-change cluster, five forms, with the session-1 patches in the tool):
plan and extract clean, header correct, entry point promoted automatically, two false caller
candidates from a substring match on a deftest NAME, sewing three lines, compile and kondo
clean at call 4, suites at call 5; the meter's count follows from its own watcher.


## 18:38Z — sl1 apparatus installed and armed (nothing launched)

On Anvil: `~/acid/fan/gen-fan.py` (deterministic, seed 7; 100 namespaces at every N, targets
nested 5 ⊂ 10 ⊂ 20 ⊂ 40 ⊂ 80; mixed `:as` / `:refer` / plain spellings, docstrings, comments
inside `(:require …)`, `.cljc` with `#?`, top-level `#_`; canonical post-image DERIVED at
generation, never hand-written; manifest with sha256 of every protected decoy region located by
defn name, never by line); `fanlib.py` and `fancheck.clj` (rewrite-clj form equality keeping
comments, metadata, `#?` and `#_` as structure; residue scan skipping strings/comments/`#_`;
alias-shadow check); `rescore-FAN.sh <worktree-or-diff> <N>` with predicates p1–p6c, **proven
at N=5 and N=80: canonical PASS all, base FAIL p1 p2 p6, naive `sed` FAIL everything** (at N=80
it stomps 244 of 412 protected regions and breaks the load while touching four of eighty targets
correctly); `mkprompt-FAN.sh` (14 prompts, byte-identical outside §5, the four ritual lines in
this task's terms); rung FAN in v5 (base read from the repo's commit, never typed; arm T on
7895 with attestation); `chain-sl1.sh` armed on GO-SL1 (pid 1122646; requires 7895 attested ==
Q5Z-SHA; runs sl1-5 … sl1-80 then sl1-C, 12 of 14 arm-runs). Anchor R blocked: Anvil has no
GitHub credential to clone curtaincall-cfp; one bundle push away. Two apparatus defects found
and fixed during the proofs (manifest recorded a pool alias for `:refer` files; an empty TSV
field shifted columns under bash `read`). Remaining: the q5z verb (building) → checkout at
`~/acid/surgeon-q5z`, server on 7895 writing ready.edn, `Q5Z-SHA`, **hand-drive at N=5 first
(G1–G2, per the tweezer protocol)**, then GO-SL1.


## 18:42Z — z6 scored: the fix is real, the wall win was a slow baseline; falsifier triggered on rung M

Receipt `~/acid/receipts/z6-score.md` (scorer; acceptance run, rs1's rescore-R3 still deferred
while z7 works in the same repo).

| | z3 Z (n=4) | z3 N (n=4) | z6 Z (n=7) | z6 N (n=7) |
|---|---|---|---|---|
| wall | 285.8 | 356.2 | **293.1 (53.6)** | **296.7 (54.3)** |
| returns | 13.2 | 21.2 | 18.1 | 21.7 |
| tokens Z as % of N | 59 % | | 86 % | |
| admit refusals | 0 / 5 | | **5 / 21 (23.8 %)** | |
| commits at complete | 5 / 5 | | **15 / 15; partial never appears** | |
| extra post-write probes | 0 | 2.5 | **0** | 0.57 |
| `apply_patch` on `.clj` | 0/4 | | **0/7** | 4.57 per run |
| stale-onset defective | 0/4 | 2/4 | **3/7** | 1/7 |
| acid acceptance failing (of 39) | 2.50 | 0.50 | 2.14 | 1.71 |

Welch Z vs N at df 12: −3.6 s, t −0.12, p 0.90, d −0.07. **Predictions (d97fc5d): 2 PASS (100 %
complete commits; 0 post-write probes), 3 FAIL (wall ≤ 0.85×: 0.988; refusals < 10 %: 23.8 %;
stale-onset 0/7: 3/7); falsifier TRIGGERED (gap 0.04 floor sd).** Between cohorts Z moved +7.3 s
and N moved −59.5 s: z3's native arm carried two 400 s+ runs in four; z6's one in seven. The z3
headline was a slow baseline, not a fast gate. Gene's Leyden question is answered for rung M.

**What reproduced and what is new.** The fix does what it says: 15 of 15 commits at `complete`,
`partial` unreachable across 21 calls. The gate caught two genuine hazards in situ for the first
time in the series: an unbalanced post-image (`Unmatched delimiter: )` at `channel_test.clj:1037`)
and blocking lint findings, both refused with "nothing was written". Two behavioural results hold
across cohorts: zero extra post-write probes and zero native `.clj` patching. Refusals are a real
cost after all: 23.8 %, two mechanical, one self-limit (`verification-incomplete`:
no-clojure-files, no-mapped-test-namespace), two genuine. `verification-runner-failed` and
`focused-namespace-missing` never fired; two of three new classes remain unexercised.

**Reading against the mechanism.** The gate wins by deleting native's post-write ritual; on this
cohort native's ritual was 0.57 extra probes per run (z3: 2.5), so there was little to remove,
and the result is flat. Consistent with the mechanism, fatal to the rung M claim. z7 is the real
test: native runs two JVM suites on R3, and z7's first gate arm executed zero suites itself and
ended at 281 s against rs1's 328.


## 18:46Z — gate session by hand: the sewing through admit_clojure_patch, one false refusal fixed live, then one verified commit in 21 s

The gate server started in-process from an nREPL on `bridge/admit-gate` (2cc52fa), rooted at the
tweezer worktree on 7899 (`:project-root` attested in ready.edn), with the R3 focused profile and
wrapper installed in the tree. Session 2's sewing (alias require, one site, one dead require)
reverted and resubmitted as git's own unified diff, mode commit, verify focused.

| call | result |
|---|---|
| 1 | REFUSED in 750 ms, hazard `require-removed`: "The ns form no longer requires clj-surgeon.mcp-compact-location", class refusal, next_call offering only preview, no override. The require is dead (clj-kondo unused-namespace); every extraction's sewing removes one. **False refusal in the winner square.** |
| live patch | `form-identity/require-hazards` wrapped: a `:require-removed` hazard stays a refusal only if the patched image still references the lib or its alias; otherwise class `:note`, message says so. |
| 2 | ADMITTED: `committed true`, `verification_status complete`, 20.9 s, 38 tests in `clj-surgeon.mcp-tool-test` via the R3 wrapper, lint delta 0 introduced, drift 0 bytes, owners −5 ~2, the dead-require note carried in `hazards`. The write equals session 2's sewing exactly. |

**Shape.** Extract in one call (session 2, with the header fixes) + gate in one call = a verified
structural move in two returns plus study. rf2's rewiring verb makes the extract self-sewing, so
the cold path is: plan → extract-with-rewire → gate-commit → done. The next ladder step is G5,
whether a cold agent takes that path unprompted.

**Two gate findings for the branch:** (1) dead-require removal must be admissible when no
reference remains (fix dispatched with a witness); (2) `next_call` on a hazard refusal must name
the override or the evidence that would lift it, else the refusal is not actionable.

