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


## 18:48Z — session 2 metered: 8 returns, 293 s, move at 6, green at 8; the first hand-driven run under native on both axes

Watcher record (`bridge/tweezer-1`, `docs/observations/2026-09-02-tweezer-session-2-watch.md`),
housekeeping separated and listed (8 calls excluded: an Anvil freeze, a scorer message, the
watcher spawn, two doc commits, the bundle attempts, a read of session 1's file).

| | session 1 (tool unpatched) | session 2 (tool patched live) | native benchmark |
|---|---|---|---|
| returns to the move | 27–31 | **6** | 9–10 |
| returns to green | 35 | **8** | 14.3 stripped / 20–24 |
| wall | 797 s | **293 s** | 311–342 s |

Per-call deviations the meter recorded: call 1 a clean refusal (`:missing [:form]`, agent-visible);
call 4 the extract's receipt reports counts but never states the three properties it now
guarantees (no docstring, pruned imports, derived visibility): not agent-visible, only checkable by
a header read; call 5 the two "callers to review" were deftest NAMES echoing the moved forms, not
references, indistinguishable in grep output; call 8 the suite read-back shared a cell with an
unrelated Anvil command (scope). Driver's private count 7 vs meter 8.

**Two protocol fixes.** (1) The watcher could not find my `TWEEZER SESSION n CLOSED` sentinel as
driver text in the transcript store (it found it only quoted inside agent prompts), so sessions
now close by writing a marker FILE in the worktree (`.tweezer/session-<n>.closed`, `date -u`
inside), which a watcher can stat. (2) No unrelated command in a metered cell, ever; the meter
counts the cell.

**Receipt ratchet, third instance:** a receipt must state the properties it guarantees, not
only counts. rf2-1's receipt gains `header {:docstring :none :imports-pruned n :visibility-derived
[…]}` (sent to the builder).


## 18:50Z — rf2 pushed, G1 by hand, G2 "not determinable": the receipt's field names carry history, not state

`bridge/rf2-extract-rewire` 57e3ca0 (base 837fabbe, main moved under it by the worktree-lifecycle
merge). The builder's acceptance: the rf1 extraction in ONE call, all five files byte-identical
to `rf1-reference.diff`, promotion derived, no `:public` needed. Root causes it found, both
better than the ethnographer's guesses: the `:ls` false refusal was clj-kondo's exit code read as
failure when it counts findings (2 = warnings) plus the diagnostic reading stderr while kondo
writes stdout; and `expected_shape` was being dropped at the wire boundary by a closed
diagnostic map, so rf2-3(a) would have reached no agent. rf1's eleven real payloads replay as
fixtures; the false require refusals are gone; one remaining refusal is kept deliberately and
made recoverable (deleting an `:as … :refer […]` entry would drop referred names; the old
extract wrote that entry, the new one does not).

**G1, my hand:** fresh checkout of 837fabbe, one CLI call, 1.3 s: 2+4+10 external sites
rewired, 7 internal qualified, dead require and two imports removed; diff vs reference differs in
whitespace and one docstring's wrapping (the tool moved it verbatim, the reference re-wrapped).
Suites on that scratch running.

**G2, naive reader (a fresh model given only the receipt): DETERMINABLE: no.** It read
`:remaining-source-callers` and `:callers-to-review 4` as UNFINISHED work it must do by hand,
when the tool had qualified every one of them; could not tell applied from dry run; did not learn
the target namespace; had no compile or test status. **The receipt's field names carry history,
not state.** Fix dispatched to the builder: `:applied`, `:target-ns`, `:target-file`, `:header`
(guarantees), `:source-callers-rewired`, `:external-callers-rewired`, `:callers-unresolved []`
(and `:complete` only when empty), `:compile`, with a witness that a cold reader can determine
the next call from the receipt alone. This is the ladder doing its job: the verb passed G1 by
hand and failed G2 on its receipt, before any agent or battery touched it.


## 19:02Z — G5 cold shadow on the rewiring verb: zero adoption; the anchor pinned; q5z built

**G5.** One cold Codex agent in a fresh worktree at rf2's sha (57e3ca0), the stripped native
prompt with its tooling rule replaced by one non-mandating line ("this worktree is clj-surgeon
itself; its babashka CLI is available in place as `bb -m clj-surgeon.core :op …`; `:op :help`
lists the operations; use it or not as you judge"). Result: **the tool was never mentioned or
called**; the extraction was done natively in 237 s (18:57:14Z → 19:01:11Z), five files, the same
shape as native everywhere; the agent also reported `TURNS: 1` against a prompt that asked for
a tool-call count, and noted the checkout was not the base the prompt named (true: the rf2 sha,
whose five subject files are byte-identical to 2311cc09). **Free-choice adoption today: 0 of 9.**
Presence and a name are not a path. The verb that wins in the hand (G1) and reads wrong on its
receipt (G2) is not chosen cold (G5). Next on the ladder is the cold shadow with the tool named
IN the task's own terms (what the receipt-first affordance looks like), and the mandated n=3.

**q5z built** (`bridge/q5z-alias-migration`, base 1dc018b): fifth top-level MCP tool
`alias_migration`, payload constant in N, receipt O(1) (< 1200 bytes at N=12, no per-file
list; details behind `details_path`), tool-side discovery, per-file alias against that file's
bindings, atomic through the kernel entrance `execute-mcp-change!` (drift gate per form; a
concurrent edit that adds a call site to an unplanned form is not migrated, written into the
design; the residue predicate over the tree is the closure proof), five typed refusals each with
an executable `next_call` (expect mismatch, policy exhausted, empty scope, indirect reference,
ambiguous ownership), `.cljc` reader conditionals refused rather than guessed. MCP only by
design. Fails-first 118 + 64 assertions. My suites running.

**Anchor R pinned.** The spec's var did not exist in curtaincall-cfp; measured menu: no single
var of `store` reaches 68 files; the whole-lib rename `store` → `event-store` does (68 src files,
815 src sites, plus 106 test files, 1075 sites; prefix-sharing siblings `store-pg`,
`store-checkpoint`, `store*-test` are real sed-catchers). Pinned as a lib-only migration
(`:var nil` both sides; q5z extension dispatched: rewrite every var of the lib, replace the
require, rename the defining namespace file, refuse on target-lib-exists and on sibling
touches). Gate `bin/kaocha unit` (1007 tests, 12232 assertions, 0 failures at base). R
predicates r1–r6 (scope, residue, load, suite, siblings, policy) with churn informational.
`chain-sl1r.sh` armed on GO-SL1R (pid 1624624); the slope chain on GO-SL1 (pid 1122646); both
need 7895 attested == Q5Z-SHA.

**z7 done** (gate on R3, n=3): walls Z 281 374 462 vs N 429 749 486, but the runner's diffs show
two Z arms touching 1 and 0 files: fast walls on abandoned work until the scorer says otherwise.
z8 (rung L control) running.


## 19:06Z — receipt v2: the cold reader now understands it and still cannot act; two more receipt defects, one of them rf1's "output too long"

The rf2 follow-up landed (uncommitted on 57e3ca0): the receipt leads with `:applied`,
`:target-ns`, `:target-file`, `:header` (docstring `:none | :caller-supplied |
:copied-from-source`, imports kept and pruned, visibility derived, alias, refer), `:source-header`,
`:source-callers-rewired`, `:external-callers-rewired`, `:callers-unresolved []`, `:complete`,
`:compile`, `:callers-mentions-only`, `:summary`, `:history` demoted; the dry run is the same map
with `:applied false` and a copy-pasteable `:would`; ordering enforced by construction (an
`array-map` past eight entries would silently unorder under `assoc`); byte-identity to the
reference holds with and without `:public`.

**G2, second pass (head of the receipt only):** the reader correctly took `:applied false` as a
plan, saw the empty unresolved list and `:complete true`, and stopped on `:compile {:checked
false}`: "compiling before writing is the only way this plan's correctness gets checked."
DETERMINABLE: no, for two reasons, both real. (1) **The dry run printed 347,405 bytes**: ~3 KB
of structure and the rest file text, a 238 KB segment after `:public-forms`, a 79 KB
`:new-file-preview` holding the whole new file. This is rf1's `output too long to use` (the dry
run truncated at 23,888 tokens in rf1-g2-A-1 and the agent ignored it), still present. Bound
dispatched: no file contents in a receipt, preview = ns form + form names with line ranges, a
witness that no string value exceeds 2,000 chars and the encoded receipt is under 4 KB. (2)
**The verb does not compile what it wrote.** The receipt hands the agent a command to run
instead, which is a return the receipt was supposed to remove. Dispatched: apply runs the compile
check as the transaction's last step (in-process on the JVM path; the CLI shells out to the
command it already prints) and reports `:compile {:checked true :ok …}` with `:undo` on failure.
That is the "think, compile, bang" shape at the receipt level: the receipt is the verdict only
when it contains one.

**z8 so far** (rung L control): natives 118, 126, 175; gate 250, 254, 337. The control is not
flat; the gate costs on a task with nothing to remove, which the mechanism allows and the
pre-registration called a falsifier of the wall claim on L. Scored when done.


## 19:07Z — G5b: the exact one-call command named in task terms, still never called; free choice 0 of 10

Same cold Codex agent, same worktree sha (57e3ca0), same stripped prompt; the tooling line now
reads, in the task's own terms: one command does the whole move, `bb -m clj-surgeon.core :op
:extract! :file … :to … :forms '[…]' :alias exact-verify` moves the forms verbatim, writes the
header, alias-qualifies the internal sites, rewires every external caller, drops the dead
requires and imports, prints a receipt; `:op :extract` is the dry run; use it or not as you
judge. **Zero mentions, zero calls.** Native extraction in 271 s (19:02:48Z → 19:07:19Z), five
files, `TURNS: 1` again.

**Free-choice adoption today: 0 of 10** (z3 F 0/4, z4 F 0/4, G5 0/1, G5b 0/1), with the last
one fully greased: the exact invocation, its guarantees, and the dry run all named. Naming a
tool does not put it in the agent's path. Sol's acquisition funnel is not the mechanism either;
the funnel was removed and the outcome did not move. What remains is habit: a fresh verb with no
usage history loses to `apply_patch` on every free choice a Codex agent makes, however good the
verb. Consequences: (1) the ladder's G5 is answered for this tool class, and the answer is that
the win exists only under mandate (or under a harness that routes the write through the verb,
which is what the gate does when mandated); (2) a "prompt affordance" line is not a lever and is
dropped from rf2's plan; (3) the honest product claim is "a mandated verb + gate makes an agent
faster and safer on X", never "agents prefer it".


## 19:19Z — Captain's log: what is possible, in the numbers of one move

Gene: *"1.3 sec vs __ native"* … *"OMG. 100x faster??? if so, captain log in terms of what's
possible!!!"*

**The measurement.** The nine-form extraction on Surgeon's own tree, cut plus sew (new
namespace with a correct header, seven internal sites qualified, 16 external sites in three
files rewired, dead requires and imports removed):

| | native (rf1, two runs) | the rewiring verb (G1, my hand) |
|---|---|---|
| time to the landed move | 141 s, 152 s | **1.3 s of tool time** |
| model returns to the move | 10, 9 | **1** (plus one to read the receipt) |
| writes | 4–5 patches | 1 |
| output | correct | byte-identical to the reference across five files |

**Three honest multipliers, and they are all true at once.** On the mechanical closure, ~110×:
the tool computes every consequence of the intent (callers, requires, imports, visibility,
verbatim moves) faster than the model can type one file. At the step level, ~4×: a model return
costs 20–40 s whether it is a call or a patch, so one call plus one receipt read is ~40 s against
~150. On the whole task, ~1.15× with the verb alone, because the tail (compile, two suites, the
report) is untouched and native pays it too; the gate absorbs the tail, and that is the other
half of the shape.

**What is possible, stated as a law.** An agent's cost is its count of *decisions*, not its
count of *edits*. On this task there are two decisions: which forms go where, and whether to
accept the verdict. Everything between them is mechanical closure, and mechanical closure runs
at machine speed once a verb takes the whole intent and returns a verdict a cold reader can act
on. The floor on this task is therefore two returns plus the gate: ~4 returns against native's
22, and 20 s of tool time against ~5 minutes of typing. That is the 5× on the agent's meter that
the 100× on the machine's meter makes possible.

**The boundaries, because a log that omits them is a brochure.** (1) The verb wins only under
mandate: free choice today was 0 of 10, with the exact command named. (2) The receipt is the
product: the cold reader could not act on it twice (fields carried history; 347 KB of file text;
compile unchecked), and every one of those is a return the verb was supposed to remove. (3) The
gate's own verification is a cost on tasks with nothing to remove (z8: ~1.9× on a two-minute
hoist), so the gate pays where the tail is large and costs where it is small. (4) Every number
above is one task, one tree; the slope experiment is what says whether the 100× on the machine's
meter grows with the codebase, which is the only way it becomes 10× on the agent's.

**What it makes newly cheap.** A refactor that used to be a day of a careful engineer or a
supervised agent run becomes: state the intent, read one receipt, let the gate verify. Six such
moves ran tonight by hand in the time one used to take. With the receipt fixed and the verb
mandated, that is what an unsupervised agent inherits.


## 19:24Z — z7 and z8 scored: the gate killed its own extraction arm, and the fix regressed on the control

Receipts `~/acid/receipts/z7-score.md`, `z8-score.md`; rs1's deferred acceptance resolved: all
three stripped-native runs PASS every predicate, churn 53 canonical (the −35 % returns cost
nothing).

**z7 (gate on the extraction, n=3).** The gate cost 2 of 3 Z runs the task: `require-removed`
refused the extraction's own defining act 13 times (92.3 % refusal); the cohort's fastest wall,
281 s, did a fifth of the job. This is the exact false refusal the hand drive hit in one call
tonight and f5965ad fixes; the woodchipper spent three arms confirming what the tweezer found
in 750 ms. Walls (Z 281 374 462 vs N 429 749 486) are therefore not comparable and are withdrawn
as a speed reading; the z7 claim is re-run only after the fixed gate serves 7894 (it does now).

**z8 (rung L control, n=4).** Not flat: **1.86×**, every Z run slower than every N run
(250 254 267 337 vs 118 126 175 178; d 3.51). Decomposition: the gate's own verification is
22.1 s per run, **17 % of the +128 s gap**; 83 % is agent time, 5.25 extra returns and 79 %
more tokens retrying around a **58.6 % refusal rate**, 13 of 17 refusals being the gate failing
to verify itself (`verification-runner-failed`, `no-mapped-test-namespace`). Zero hazards
caught. One Z run produced nothing (6 calls, 5 refused, empty diff). **And the fix regressed:
3 of 6 commits landed at `verification_status: partial`** under 2cc52fa, the state ADMIT-105
exists to make unreachable; z6 had 0 of 15. The guarantee is rung-dependent, which means it is
not a guarantee. The mandate itself is obeyed and cheap (0 native `.clj` patches, shell below
native); the verification pipeline behind it does not hold on this rung.

**Decisions.** (1) `bridge/admit-gate` f5965ad is WITHDRAWN from review a second time until the
partial-commit path is reproduced from z8's real payloads and closed with a property test over
every non-complete status (dispatched, real bytes first). (2) z7 is re-run on the fixed gate as
the gate's claim test; z8 stands as the cost boundary: the gate pays where it removes a large
tail and costs ~1.9× where it removes nothing, and on L most of that cost is refusals, not
verification. (3) Predictions (d97fc5d) on z8: control flat FAIL by 4 sd; 0 partial commits
FAIL; canonical churn 6/8 FAIL. The reproduction program's honest state: rung M flat (n=7), rung
L a loss, rung R3 unmeasured until the fix. The mechanism survives in the decomposition (the
gate's clock is small; refusals are the cost) and in the hand drive; it has not yet survived a
cohort.


## 19:26Z — q5z pushed and caught at G1 on the wire; rf2's receipt bounded 346 KB → 4 KB and the verb now compiles what it wrote

**q5z** (`bridge/q5z-alias-migration` 6b5252c, pushed; server 7895 attested at that sha, root
`~/acid/surgeon-q5z`). First real call over the HTTP wire, hand-driven at N=5 on a fresh worktree
of the generated repo: `mcp-adapter-failure: Wrong number of args (0) passed to
clj-surgeon.mcp-tool/default-receipt-dir`, 157 ms, source unchanged, acceptance = base. 182
assertions green because nothing drove the tool through the server's adapter with a real
request. Third instance today of the reviewer's input standing in for the caller's. G1 exists
for this; the battery would have measured a dead tool across ten arms. Fix dispatched with two
real-wire witnesses (a commit and a refusal whose next_call must arrive intact) and an arity
audit of every helper the adapter borrows.

**rf2** (follow-up on 57e3ca0, uncommitted, my suites running). The leak, measured per key:
`:_caller-plans` 238 KB (each caller plan carried the whole original AND the whole rewritten
file), `:_source` 78 KB (the entire source file), plus `:_form-texts`, `:_new-file-content`,
`:_moved-sources`: 337 KB of `_`-prefixed executor state against 8 KB of receipt, reaching
readers through a denylist (`dissoc`) where a forgotten key leaks by default. Now an allowlist
applied to both surfaces: dry run 4,077 bytes, apply 3,657, longest string 324 chars, no `_` key,
witnessed. The preview reports each form's RESULTING kind (a receipt that still said `defn-`
would deny the promotion it just made). Two corrections to my reading: the 79 KB after
`:new-file-preview` was `:_source` printing adjacent in hash-map order; the preview itself was
already 679 bytes. **The compile check runs inside `:extract!`** as the transaction's last step,
a bounded subprocess (loading the rewritten namespaces in-process would mutate what a server
is serving): `{:checked true :status :run :ok true :exit 0}` in 7.8 s on the fixture; failure
reported with `:undo` naming the receipt and the exact revert command, witnessed by a fixture
whose moved form references a helper left behind. Two defects the check found in itself:
`clojure -M:alias -e` runs the alias's `:main-opts` (it ran the whole suite and timed out; now
resolve the classpath, then `clojure.main`); and a false `:ok false` from an error raised in a
namespace the extraction never touched, now `:ok :unverified` when the check cannot see its
subject. The receipt-cannot-see-its-subject rule, applied to the receipt.


## 19:32Z — the closure catalogue: where the math is in our favour, measured, and where it is not

`docs/closure-catalogue.md`: 735 candidates over 592 namespaces and 183,704 var usages, three
repos, clj-kondo analysis, cost model = rf1's own arithmetic (blind, it predicts 140 s for rf1's
task against the measured 141–152 s). Top-5 real wins: cfp `store` → `event-store` (170 files,
2,056 sites, ~29 min native → ~1.4 min, the pinned anchor); cfp `events` rename (170 files, two
spellings, a `sed` silently misses the plain uses); Surgeon's own `validate-tool-params`
extraction (59-form closure in a 1,415-line file, 193 sites, 7 callers, rf1's shape at 6× the
cluster; the verb exists); cfp `web.http` rename (28 files, two spellings); mvr `channel` split
(9 files, 502 sites, 3.8×, the honest demonstration on a 40-file app).

**Three findings that outrank the ranking.** (1) The biggest fan-out in our repos is class D,
parameter threading: 315 of 735 candidates, up to 123 caller files, ~3,700 s of native work, and
none of it closable, because what to pass at each site is a judgment from that caller's scope.
**Do not build `param_thread`.** (2) Our repos are alias-uniform: of 364 closable candidates, 238
have one alias spelling; the files-that-must-be-READ term is 2–3 whether 9 files or 170 are
affected; median closable ratio **3.4×**, 24 % clear 5×. **The slope that would make the tool
10× does not exist in real code we own**; it has to be synthesised, which is exactly what sl1
does, and the anchors will show 3–5×, minutes not hours. Class E (library swaps across many ns
forms) is already won by `require_change` in one call. (3) **Surgeon cannot read the repos where
the fan-out lives**: on main, `:ls` refuses with `:forward-reference-analysis-failed` and an empty
diagnostic on 9 of cfp's 10 biggest src files and 6 of mvr's, while succeeding 10/10 on its own
tree: clj-kondo's findings exit codes read as failure, the defect rf2 fixed and main still
carries. The verb succeeds on the tree it was built in and fails on the repo it would be sold
into, and that asymmetry is invisible from inside Surgeon. Promotion of the rf2 `:ls` fix to
main is therefore the first prerequisite of every cfp win on the list.


## 19:42Z — correction to the z8 record, and the hole was my own ratified decision

The z8 score said "3 of 6 commits at `verification_status: partial`". The gate agent pulled the
rollouts: **no commit landed at partial**; every partial call was correctly refused
(ADMIT-105 held). The three bad commits landed at **`unverified`**, and the cause is a ladder:
refused at `verify focused`; refused again with `allow_partial true` (profile exists, waiver
correctly denied); then **`verify "none"`, committed.** Exactly three `verify: "none"` calls
exist across the four z8 Z runs, and they are exactly the three non-complete commits. The agents
met the gate, tried the waiver, and turned verification off, because the schema let them.

The hole was the decision I ratified earlier tonight: *"`verify: "none"` stays the explicit
waiver (the cohort prompts mandate focused)."* A prompt mandating focused is not a control; it
is a suggestion to an agent that can read the schema. **A gate a caller can turn off is the
caller's gate.** Fixed on the branch (uncommitted, my suites running): completeness required
regardless of `verify` (ADMIT-120; `verify none` now refuses a commit and its next_call proposes
focused; unverified answers stay available in preview, where they belong); the `allow_partial`
waiver decided on directly observed profile absence, published as `profile_absent`, never on a
runner reason (ADMIT-119); a profile that exists but has no `:command` is its own unverified
reason, not the no-profile waiver (ADMIT-118, a second latent hole reproduced locally before the
field data arrived). Witnesses: z8's exact committing call as a real-bytes fixture (fails-first:
"clock.clj was written to disk"), and a property test over 2 statuses × 17 reasons × 2 verify ×
2 allow_partial × 2 profile_absent = 272 combinations, refusing every one but the single waiver.
z7 replay: 1 of the 2 recoverable `require-removed` patches now admits with both dead requires
as notes; the other fails to apply against the base (replay fidelity, not the gate). The z8
scorer's column is corrected in place on Anvil.


## 19:43Z — q5z's one arity was three defects, and the third would have wrecked the cohort

The G1 wire call's arity error (`default-receipt-dir` with 0 args; the real dispatch passes
the project root) was the visible one. Behind it: (2) the adapter read `:verification-profiles`
straight off the server config, while a routed workspace publishes `-selection-fn`/`-profiles-fn`,
so any workspace other than the server's own would have used the SERVER's profiles; (3) the
adapter auto-selected a profile whenever the workspace had any, and every workspace has the
built-in defaults, whose `fast` runs `npx standard-clojure-style check`: a correct five-file
migration rolled back for a missing binary, plus ~2 s of wall on every call in a cohort whose
subject is wall. Verification is now opt-in (`verify` optional; `focused_test {:status
:not-requested}` otherwise; an unconfigured profile refuses before discovery), both entrances
share one `resolve-verification-config`, and three real-wire witnesses reproduce my exact JSON
against a routed workspace on a real Jetty server (commit; refusal with `next_call` replayed
verbatim to a commit; the routed workspace's own profile resolved). Fails-first on the arity
alone: 35 failures + 2 errors. The builder's own process finding: clj-kondo reports the arity
exactly (`called with 0 args but expects 1`); it had linted the five files it created and not
the one it edited. MCP-OP-ALIAS-027/028. My suites running; then commit, 7895 restart, G1 again.


## 19:48Z — z7b launched on the fixed gate; rf2's cohort blocked by its own compile command; q5z's oracle catches my alias rule

**z7b** (gate vs native on the extraction, strip prompts both arms, 7894 attested at 17125fe ==
GATE-SHA) launched 19:47:11Z via chain-z7b, "N Z N|Z N Z". This is the gate's claim test after
both hand-found defects (require-removed as a note; verify-none cannot commit).

**rf2's cohort (R3b) is installed and blocked by a src defect the installer found by running the
mandate on a pristine bcec265 worktree.** The verb does the move (`:complete true`,
`:callers-unresolved []`, 9 forms, 4 internal owners, 3 caller files) but the compile command
its receipt hands the agent omits the repo's test alias, so nrepl is off the classpath and it
exits 1; the verb reports `:ok :unverified :reason :classpath-incomplete` honestly, and an arm
told to "run the compile command the receipt names" would see a red unrelated to its work and
repair, landing bytes after the extract, the exact number the cohort measures. A false red
terminates the reading as surely as a false green. Fix dispatched: the workspace declares its
compile aliases in `.clj-surgeon.edn`, the verb uses them in-process and in the printed command,
and on an undeclared classpath the receipt names candidate aliases and marks the command
`:guessed`. The installer did not patch the pre-registered mandate; it failed the chain closed
and named the fix. Also from the installer: an arm launched by accident on a typo'd letter
(killed within a minute, nothing affected) turned into a refusal for unknown arm letters; the
chains' driver-wait `pgrep` was unanchored and now is; an atomic `mkdir` lock keeps two released
chains from launching together; the readout counts `bytes_beyond_verb` from the tree against a
verb-only reference, because codex logs carry no tool-call markers to count `apply_patch` from.
Baselines at bcec265 measured, not typed: test-fast 729/6159 with the 5 known routing failures;
mcp 382/4059 with the known macOS path. `mcp_tool.clj` at bcec265 is not byte-identical to
2311cc09 (one hunk, the workspace-target-ns fix); references to the nine forms unchanged.

**q5z G1, second pass** (7895 at 40b26b1): the verb commits over the wire, 5 files, 15 sites,
731 ms; load, suite, residue, protected regions all PASS; **p2 and p6c FAIL on alias choice**:
three files got the second policy entry where the canonical says the first was free;
`collisions_resolved 7` is the tell. Root cause is MY brief: I told the builder a policy entry
collides with any local binding of that name. In Clojure a local cannot shadow the qualifier of
`store2/fetch-event`; only ns-form aliases and referred names collide, which is what the
generator, the canonical and the cohort prompt say. Fix dispatched (one rule, two witnesses).
The byte oracle caught a spec error in the spec's own author, which is what it is for.


## 19:54Z — tweezer session 3, Gene's real work: the Stellman duplicate is one instance of a class, and a structural query found the class

Gene: *"you can see an emergency fix we did for Andrew Stellman duplicate record. It really makes
me nervous that a duplicate record showed up. Can we do a safety factor to make that class of
error impossible using our surgeon tools, with the watcher working."* Then: *"Make a big LID
assertion to prevent and find instances where vulnerable."*

curtaincall-cfp main 00e8f0fa, "Make event speaker creation retry-safe": the write side now
checks the projection before appending (check-then-append: still races under two concurrent
retries, the kernel's lost-update class from this morning); the fold side now treats the
announced-speakers relation as a set keyed by person-id (the durable half). The class: any fold
that conjes a fact onto a vector keyed by an identity duplicates under a repeated append-only
fact.

**Call 1, one return:** `inspect_clojure` with three structural `match` patterns and an outline
over `folds.clj` (970 lines, 127 forms, 116 `fold-event` methods): `(fnil conj [])` matched
**six** sites; the other two patterns none. A `grep conj` would have returned dozens and missed
the shape. **Call 2:** the six read with their dispatch values: `submission.speaker-added`
(the same defect one relation over), `speaker.blackout-window`, `file.comment-added`,
`reviewer.recused` and `reviewer.unrecused` (log entries), and `fold-task-chase`, which already
guards by chase-id `not-any?`, the idempotent shape the others lack. Five vulnerable, one good.

**The LID ratchet, dispatched:** FOLD-IDEM-001, one generative property over every fold method
(`(methods fold-event)`), folding any event twice from any state equals folding it once, with a
NAMED gap for any type without a sample, never a silent pass; prediction: exactly the five sites
fail, the chase helper passes. FOLD-IDEM-002 names each relation's identity (speaker by person-id
or email; window by value; comment by id; log entry by value). Fix: one `conj-distinct-by` helper
at the five sites, INTENT-tagged. Write-side idempotency key in the store's append path is the
follow-up bead. Branch `bridge/fold-idempotence`, merge is Gene's. Watcher on.


## 20:02Z — q5z passes its oracle on the third pass; the slope and anchor are GO; rf2's compile fix is in

**q5z** 13d86bb (collision = ns-form alias or referred name only; locals cannot shadow a
qualifier), 7895 attested. G1 pass 3 at N=5: `committed true`, 676 ms, histogram
`{store2 3, st2 1, store-2 1}`, 4 collisions, and `rescore-FAN` **p1–p6c all PASS, VERDICT=PASS**.
Three passes to get here, each caught by a different instrument: the wire (adapter arity and
two defects behind it), then the byte oracle (my own collision rule). `GO-SL1` and `GO-SL1R`
created; the chains take the cohort lock and run after z7b.

**rf2** a66b626: the compile check and the printed command use the workspace's declared aliases
(`.clj-surgeon.edn {:compile {:aliases [...]}}`, committed for this repo), an undeclared classpath
names candidate aliases and marks the command `:guessed`; on a fresh checkout the mandated CLI
now reports `:compile {:checked true :ok true :exit 0}` and the pasted command prints
`:compile-ok`. RF2-SHA re-pinned, `surgeon-rf2` synced, probe re-run green at exit 0; one
apparatus parse (`:ok true` read as a keyword by the recorder) stands between the probe and
`GO-RF2`.

**z7b so far:** gate 257 s against natives 344 and 552 s, gate arm at 0 self-run suites, all
failure sets equal to base. Wave two running.


## 20:10Z — the big LID assertion: 121 fold arms, nine not idempotent, one of them inside the emergency fix itself

Branch `bridge/fold-idempotence` on curtaincall-cfp (base 00e8f0fa), three commits, red → red → green.
`FOLD-IDEM-001`: while the application folds an append-only log into projected state, every
fold-event arm is idempotent in its fact. `FOLD-IDEM-002`: each set-like relation's identity,
named. The witness enumerates `(methods fold-event)`, 121 arms, a hand-written corpus of 121
samples over two seed states, and REFUSES any arm without a sample, so a new arm is a named gap.

**Fails-first, 121 arms, 8 not idempotent:** the five the structural query predicted
(`submission.speaker-added`, `speaker.blackout-window`, `file.comment-added`,
`reviewer.recused`, `reviewer.unrecused`) plus three the `(fnil conj [])` pattern is blind to:
`file.version-added` (`conj` onto a pre-seeded vector, no `fnil`), `export.generated` (a `cons`
capped at 50), and `review.blind-mode-set` (a counter, `inc` on every application). The chase
helper passed, as predicted. **Second red:** Gene's own fix keys announced speakers by person-id
and skips the removal when the entry has none, so an id-less speaker still duplicated; 9 of 121.
**Green:** one `conj-distinct-by` (replace in place; order is product-visible) plus a
newest-first sibling, nine sites INTENT-tagged; blind-mode advances its version on a change of
mode, not on re-application. 121/121, 0 gaps. `bin/kaocha unit` 1010 tests / 12513 assertions /
0 failures (base 1008 / 12244); ci, compile-check, test-js green; the seven pinned inventories
(routes, views, routes-architecture, intent contract, registry, witness identity, suite
architecture) undisturbed. Prolog oracle unverified on this box (no plunit).

**What the fold cannot fix, reported not touched:** the write side at `announce.clj:209/:245`
reads the projection then appends, outside the store's write lock, and with no cross-instance
lock on Cloud Run; the projection is now immune, the LOG is not, and `fire-sinks!` runs per
appended fact, so a duplicate fact double-fires webhooks. Follow-up bead: an idempotency key on
`store/append!` refused inside the write lock. Also: `review.blind-mode-set` has no writer left;
the legacy `event.speaker-announced` arm keys the same relation by name while the new arm keys
by person-id, a latent disagreement needing a product decision.

The pattern for the chronicle: a structural query found the class in one return; the generative
property found three shapes the query could not and a hole in the hand fix; the ratchet is the
property, not the fix.


## 20:15Z — session 3 metered: 4 driver returns; the query is the map, the property is the ratchet

Watcher receipt `docs/observations/2026-09-02-tweezer-session-3-watch.md`: 4 driver returns,
1196 s wall (the delegated build is most of it), 4 housekeeping calls excluded, three other
threads (rf2, q5z, z7b) correctly excluded as not session-3 calls. Two observations worth
keeping: (1) the structural match found 5 of the 9 vulnerable arms in one return and its receipt
gives no signal that its three literal patterns are incomplete; only the property over ALL arms
found the `conj` without `fnil`, the `cons`, the counter, and the hole in the hand fix. A
structural query is the map; the generative property is the ratchet; a session that stops at the
map has not made the class impossible. (2) My closing cell bundled a suite read-back, a push, an
inbox write and the session marker: the "no unrelated command in a metered cell" rule I wrote
this evening, broken by its author within two hours. The meter caught it; that is what it is for.


## 20:23Z — the slope's first point: at N=5 the tool is right in 25 s and native is wrong in 55 s; two apparatus faults on launch

**sl1-5** (rung FAN, generated repo, N=5 requiring namespaces, arms native vs `alias_migration` on
7895 at 13d86bb): T ended in **25 s**, N in **55 s**; my hand `rescore-FAN` from the two worktrees:
**T VERDICT=PASS on every predicate; N FAILS p2 (form tree ≠ canonical) on all five files while
passing p6c (alias policy)**. Native's failure is being diagnosed (a `:refer` left in place, a
require added instead of replaced, or a site missed, versus a cosmetic form difference the oracle
should tolerate; if the latter, the oracle is wrong, not native, and the log will say so).

**Two apparatus faults, both mine to own.** (1) `chain-sl1` built the run name in the same
`local` statement that assigned the point (`local point=$1 run=sl1-$point`), so bash expanded
`$point` before the assignment: every run was named `sl1-`, N=5 ran under that name, and every
later point refused `RUN-NAME-TAKEN`. Split the local, renamed the artifacts, set the remaining
points, re-armed. The same class as the hand-typed timestamp: a value read before it exists.
(2) The anchor's arms died at boot in 2 s: curtaincall-cfp's own checkout carries an agent
config that REQUIRES the retired `cclsp` MCP server, which answers 500 on Anvil, and codex refuses
to start a session without it. A real repo brings its own harness assumptions; the runner must
neutralise repo-local MCP config in the arm worktree and say so on the start line. Fix
dispatched; the anchor re-arms after it. Nothing measured was affected: N=5's two arms ran
cleanly and are scored from their worktrees.

Also: z7b's chain held the cohort lock ten minutes past its driver's exit because it scores six
diffs with two suites each before releasing; that is by design and was not intervened on.


## 20:23Z — z7b scored: one field, refusal → note, and the gate arm goes from 1 of 3 to 3 of 3 on the extraction

Receipt `~/acid/receipts/z7b-score.md` (scorer; rescore-R3 run on a quiet box the moment the lock
released: 42 predicate lines, zero FAILs).

| arm (both stripped) | n | wall (sd) | returns | actions | tokens | executed suites | acceptance | churn |
|---|---|---|---|---|---|---|---|---|
| Z gate 17125fe | 3 | **330.7 (77.2)** | **14.00** | 9.33 | 745,841 | 1.33 | 3/3 all PASS | 53 ×3 |
| N native | 3 | 432.7 (107.3) | 17.67 | 13.00 | 868,013 | 2.00 | 3/3 all PASS | 53 ×3 |

Admit: 5 calls, 2 refusals (both `patch-does-not-apply` stale hunks, both recovered next call),
3 commits **all at `complete`**, zero fix failures, `require-removed` firing in all three runs on
exactly z7's two files and spans as **`class: note`**. Gate seconds: 16.5 per commit, 0.06 per
refusal, **5.0 % of Z's wall**. `apply_patch` on `.clj` 0.00 (N 3.00); post-write probes 0.00.
`z7b-g1-Z-1` executed **zero** suites of its own and passed every predicate: the gate's focused
profile was the verification, which is the design. Predictions: executed suites ≤ 6 PASS (1.33
vs N 2); acceptance equal PASS; wall ≤ 0.75× FAIL by 6.2 s (0.764); refusals < 20 % FAIL (2 of
5). Welch −102 s, p 0.18, df 3.6: a direction; the gap leans on one 552 s native run.

**Against z7:** refusals 24 → 2, rate 92.3 % → 40 %, completion 1/3 → 3/3, the causal class
demoted from refusal to note: the cleanest single-variable result in the z-series. **Against
rs1:** the gate arm has reached the stripped-native baseline, 14.00 returns / 330.7 s vs 14.3 /
328 s, by a different route; z7b's own native arm ran worse than rs1's on the identical prompt
(17.67 / 432.7), which is how much cohort-to-cohort noise n=3 walls carry.

**Status of the gate's claim on R3:** correct and safe at n=3 (every commit verified, every
acceptance green, nothing written that the suites would reject), cheaper on returns by ~20 %,
and a wall direction of ~0.76× that needs n=6 to become a number. The z8 partial-commit
regression does not appear on this rung and is closed on the branch regardless.


## 20:27Z — sl1-5 diagnosed: native edited inside reader discards; the form-aware verb did not. A correctness win, not a throughput one

Receipt `~/acid/receipts/sl1-5-score.md`. Both arms used **three model returns**; T (one
`alias_migration` call, 0.29 s tool time; 5 files, 15 sites, 4 collisions resolved, read back
and verified) took 25 s and 65,414 tokens; N (one `rg -C 8` sweep, one `apply_patch` over five
files, one suite run) took 55 s and 103,123 tokens. **T: VERDICT=PASS, 8/8. N: FAIL on p2 (form
tree) AND p3 (protected regions, 5 of 58 changed).** The entire native delta against the
canonical is one line per file, and every one is inside a `#_` discard: `#_(find-event x)` →
`#_(store2/fetch-event x)` and its four alias-spelled siblings, in functions named
`decoy-discard-NNN` carrying the comment ";; store/find-event used to be called here".

**The oracle is right, and the brief's hypothesis (a cosmetic difference to tolerate) was
wrong.** The canonical does real work in all five files and touches zero discard lines; p2's
own text names discards as structure that must stay in place; p3 hashes them as protected
regions; p6b deliberately skips discards when scanning for residue, which is why native's
p4/p5/p6 all pass. Native's edit is semantically inert (a discard never evaluates) and formally
a scope violation against a region it was told to leave alone. The mechanism is tooling shape,
not carelessness: a text-scoped sweep cannot see the difference between a live call and a
discarded one; a form-aware migration can. **The first square measured tonight where the
structural route's semantic awareness yields a correctness result native cannot reach at any
speed.** Slope row one: N=5, returns 3 vs 3 (ratio 1.00), wall T/N 0.45, acceptance N FAIL / T
PASS. Points 10 and 20 so far: T 25 s and 26 s, flat; N 65 s at 10.


## 20:29Z — the anchor's boot failure was a 7888 hazard in disguise; z7c armed; slope flat for the tool through N=40

`repo-R/.codex/config.toml`, checked into curtaincall-cfp, declares two `required = true` MCP
servers: the retired `cclsp` (never started; codex refused the session, both arms died in 2 s)
and **`clj-surgeon` on 127.0.0.1:7888**, another seat's production server, which this apparatus
is under standing orders never to touch. A real repo brings its own harness assumptions, and one
of them pointed the arms at the one port they must not reach; only cclsp's failure stood in the
way. Fix (installer): per-arm neutralisation of repo-local MCP declarations, opt-in per rung,
originals backed up under the receipts dir, `repo_mcp_config=neutralised:<files>` on the start
line, the paths excluded from the freeze; the T arm's server is supplied by the runner's own
command-line override as before. Proven with a one-turn codex boot in a neutralised repo-R
worktree (`PROBE-EXIT=0`). chain-sl1r re-armed. Also found and removed by the installer: an
ordering deadlock (a chain waited on another's success marker; it now waits on the process),
and chain-sl1r holding the lock 38 minutes into scoring two empty diffs while another user's
JVMs loaded the box (stopped; needed recycling anyway). z7c (gate on R3 at n=6, mirrored arm
order across waves) armed on GO-Z7C, created now; queues behind rf2 and the slope.

**Slope, walls so far:** T 25, 25, 26, 24 s at N = 5, 10, 20, 40 (flat, as the mechanism
predicts); N 55, 65, 97 s at 5, 10, 20, rising; N=20's native gate line printed no suite
summary, which usually means the tree did not load. Acceptance per point from the chain's
score files when the run completes.


## 20:35Z — the slope, walls: the tool is flat from N=5 to N=80 and on the control; native grows with what it types

| N | native wall | tool wall | T/N |
|---|---|---|---|
| 5 | 55 s | 25 s | 0.45 |
| 10 | 65 s | 25 s | 0.38 |
| 20 | 97 s | 26 s | 0.27 |
| 40 | 111 s | 24 s | 0.22 |
| 80 | 121 s | 27 s | 0.22 |
| C (5 files × 48 sites) | **127 s** | 27 s | 0.21 |

Pre-registered readings, on wall: native at N=80 is 2.2× its N=5 (falsifier "within 1.3×" not
triggered); the ratio is monotone non-increasing on all five points and ≤ 0.35 at N=80 (flagship
criterion on wall met); the tool's wall is the same within 3 s across a 16× fan-out. **The
control is the decisive row:** native took 127 s on five files carrying 48 sites each, as long
as on eighty files with a few sites apiece, while the tool stayed at 27 s. Native's cost grew
with the volume it had to TYPE, not the files it had to read; the tool's cost grew with neither.
That reframes the slope's axis from "unread files" to "edit volume", and the reframing still
favours the tool, which computes what native types. Returns, tokens, acceptance per point and
native's failure classes come from the scorer (the chain's own score step wrote nothing; the
twelve worktrees are being rescored). Native already failed acceptance at N=5 on the reader
discards.

**Apparatus, two more:** the anchor and rf2 chains aborted on their own fail-closed check
"the runner names port 7888", tripped by the installer's new comment explaining the 7888 hazard;
the comment now says "the other seat's production port" and the chains relaunched (sl1-R running,
rf2 queued, z7c behind it). The check was right to exist and right to fire; a grep for a literal
is a grep for a literal.


## 20:41Z — the slope scored: the tool is flat and perfect; native's cost is site discovery; the falsifier fires because native got better

Receipt `~/acid/receipts/sl1-score.md` (scorer; acceptance from my rescore of all twelve
worktrees; the spec file was checked against the brief's wording since it is not on Anvil).

| N | sites | N returns | T returns | T/N | N wall | T wall | wall T/N | acceptance N | T |
|---|---|---|---|---|---|---|---|---|---|
| 5 | 15 | 3 | 3 | 1.00 | 55 | 25 | 0.45 | FAIL p2 p3 | PASS |
| 10 | 30 | 3 | 2 | 0.67 | 65 | 25 | 0.38 | PASS | PASS |
| 20 | 60 | 8 | 3 | 0.38 | 97 | 26 | 0.27 | FAIL p2 p4 p5 p6b p6c | PASS |
| 40 | 120 | 11 | 3 | 0.27 | 111 | 24 | 0.22 | FAIL p2 p3 | PASS |
| 80 | 240 | 6 | 3 | 0.50 | 121 | 27 | 0.22 | PASS | PASS |
| C | 240 (5 files) | 7 | 3 | 0.43 | 127 | 27 | 0.21 | FAIL p2 p6c | PASS |

**T:** one `alias_migration` call at every point, 0.29 → 1.71 s of tool time (scales with files,
a rounding error against a flat ~25 s wall), 6/6 `ok`, zero refusals, zero `apply_patch`, 6/6
acceptance. **N:** passes 2 of 6, three distinct failure modes: protected-region corruption (5:
discards; 40: discards, docstrings and string literals, 14 of 224 regions), syntactic
destruction (20: its own `migrate-store.sh` emitted unparseable files, namespaces fail to load,
three files end with no alias at all), and alias-policy violation under density (C: `st2` where
`store2` was free). The two points it passed are the two where its improvised tooling happened
to be exactly right. Not a scale story, a **variance** story.

**Mechanism, corrected twice by the receipts.** (1) Both arms type the same bytes: diffs
byte-identical at N=10 and N=80, six bytes apart at 20; the tool does not save typing. (2) The
control decides what native pays for: 240 sites in 5 files (C) costs native 127 s; 240 sites in
80 files costs 121 s; 16× fewer files, 2.6× fewer bytes, same wall; native's read cells track
sites (1, 1, 3, 2, 5, 5). **Native's cost is site discovery, and one call absorbs the whole site
set.** (3) The ratio is not monotone, a pre-registered falsifier fires, because native changes
strategy: hand patch to N=10, a shell script at 20–40 (11 returns), a correct Python generator
piped into `apply_patch` at 80 (6 returns). The regression at N=80 is native getting better;
T is flat at 3 throughout. "Bytes patched from the rollout" is invalid past N=10 for that reason
(the rollout holds the generator, not the diff); the scorer substituted the worktree diff.

**Predictions:** both brains overestimated native's returns (Sol 8→13, Opus 8→30; observed 3→6,
peak 11 at 40) and underestimated the tool (6–7 predicted; 2–3 observed). Falsifiers: native
wall at 80 is 2.2× N=5 (not triggered); ratio not monotone (**TRIGGERED**); ratio ≥ 0.85 (no);
fallback/refusals (none). Flagship: wall ratio ≤ 0.50 at N ≥ 40 PASS (0.22); zero fallback
PASS; ratio ≤ 0.35 at 80 FAIL (0.50); monotone ≥ 4/5 FAIL (3/4); acceptance both arms every N
FAIL (native 2/6). **2 of 5.**

**What is true, stated for the brag and its boundary:** on a fan-out migration the one-call
verb is 4.5× faster on wall at scale, 2× cheaper on returns, and correct at every point where
native failed four times out of six in three different ways. The 10× the slope was built to
find is not there on returns, because a competent native agent writes a generator at scale; the
durable win is deterministic correctness at any scale, in one call, with a receipt.


## 20:42Z — the anchor: both arms failed, for a verb defect and a spec hole

sl1-R (curtaincall-cfp at d9afe8e9, `store` → `event-store`, 170 files, ~2,000 sites): native
122 s, tool 164 s; native wins on wall as the design predicted for a uniform-alias rename. Both
fail r4. **Tool:** unloadable, `Unable to resolve var: store/*clock*` at `replay.clj:128`, a
reference reached through the `var` special form (`#'store/*clock*`), which the migration's site
discovery did not cover; the alias was removed and the reference kept. Same class the extract
verb tracks as quoted-var-references. The lib-only mode was never hand-driven (I drove the var
mode at N=5 and let the lib mode go to the field on its fixture); the ladder skipped is the
ladder that bites. Fix dispatched with the real bytes and a hand-drive on a scratch clone before
reporting. **Native:** 4 errors 3 failures because three `db-correct` tests read
`src/cfp_scheduler_killer/store.clj` by PATH as a fixture; a correct rename cannot pass r4 at
base count, and the path is a string literal the rules protect: a hole in the anchor's own
acceptance, dispatched to the installer to amend and re-measure. The anchor re-arms after both.


## 20:55Z — rf2 ran: the mandated rewiring verb, walls

rf2 (rung R3b, the rf1 extraction on `surgeon-rf2` at a66b626; C = `:extract!` with
`:rewire-callers` mandated, N = native, both stripped, n=3): **C 246, 230, 253 s (mean 243); N
291, 339, 378 s (mean 336); ratio 0.72; no overlap, every C run faster than every N run.** Both
arms' gate lines carry the identical extra 25 failures + 5 errors: the rf2 branch's own tests
that read the repo's live source as fixtures, moved by the extraction in both arms; the set
cancels and the scorer compares against base-minus-self-referential. The pre-registered readout
(native bytes landing after the verb; returns after the receipt) and Sol's promotion criteria
are with the scorer; z7c (gate at n=6) launches next on the lock.


## 20:57Z — the anchor amended: native passes; the tool's defect is a class, not a site

**Spec hole, corrected twice.** Not three tests: six deftests across three namespaces, driven by
four string literals naming `src/cfp_scheduler_killer/store.clj`, two of them ordinary source
constants under `src/` (`db/correct/bad_patching.clj:153`, `db/correct/person_identity.clj:19`),
inside files that are themselves in the migration set. Option (b), keep the file and retire the
ns in place, is not implementable: Clojure resolves a namespace to its path, so a renamed ns
cannot keep its old file. Amendment (a): r4 gates on the test count and a named allowance of
exactly those six, reports assertions rather than gating on them (an early error skips its
remaining assertions, so assertion count would fail a correct arm twice for one cause), and a
new informational r7 counts path fixtures repointed; the prompt's block 2 carries the carve-out,
generated from the measured base; `mk-R-base.sh` now measures the fixture sites so a sha refresh
cannot leave the prompt stale. **The native arm now passes the anchor:** r1–r6 PASS,
`allowed_hit=6/6` and nothing else, 122 s, 172 files, 4,054 lines. Two scorer defects fixed on
the way (the runner's own `.codex` neutralisation counted as arm work; a `git add` exclusion
cannot unstage, so the scorer now resets its index first).

**The tool's defect is a class.** The verb rewrote ordinary call sites in the same files and left
behind (1) qualified symbols in binding-vector position: `(binding [store/*clock* …])` at
replay.clj:128 and :238 plus six sites in `cli/judge_sandbox.clj`, `(with-redefs [store/now-iso
…])` and a six-var `with-redefs` in the tests; and (2) a quoted fully-qualified symbol in data
position, `(requiring-resolve 'cfp-scheduler-killer.store/state)` at sched_import.clj:127, which
fails lazily at call time with no compile error, the kind that loads and breaks in production.
Comments naming the var were correctly left alone. Dispatched to the verb's builder with the
real forms as fixtures; the anchor re-arms after 7895 restarts at the fix.


## 21:08Z — Gene's riff on the duplicate: one instance, the store layer, the accessor pair, and Sol's GO-WITH-FIX on the fold branch

Gene: max instances = 1 on Cloud Run, so the cross-instance race is not this duplicate's path;
two facts on one instance means two requests, almost certainly a second submission of the
announce form (the log's two facts carry actor and timestamp; that query decides). His fix
checks the projection then appends outside the write lock; correct enough on one instance,
protects the view not the log. Layers, cheapest first: an edge request id on the POST; an
idempotency key on `append!` checked inside the write lock (makes the log correct at one
instance); a unique index in Postgres (survives a second instance); set semantics in the fold
(already on the branch). Gene: *"Fold and store. Do quick review with sol."* The store layer is
building on `bridge/store-idempotency` (on top of the fold branch); Sol reviewed the fold branch.

**Ann's "I unpublished one, but both disappear"**, from the fold code: `event.speaker-unannounced`
removes every entry whose :name equals the payload's, and `event.program-speaker-updated` maps
over every entry with the same person-id; two rows under one identity, so any identity-keyed
operation applied to both. Set semantics remove the second row; the residual is the identity
mismatch (legacy arms by name, new arm by person-id).

**DRY and safer, measured by a structural query in one return:** 19 arms open with the
identical guard `(if-let [slug (:slug (event-by-id state (:event-id payload)))] _ state)`; 20
write sites spell `[:events slug :settings _]`; the announced-speakers vector is edited at 6
sites in 5 arms under three identities. Gene's refinement over a guard wrapper: an **accessor
pair** (`settings` / `update-settings`), a lens, the path spelled once, the missing-event case
handled once. Plan: accessor pair → relation module with one identity rule → set-like relations
declared as data (the property becomes a table check) → command functions that mint the fact
and its key. Was Surgeon helpful: yes, the `match` op with holes answered in two returns what
grep answers wrong; the refactor itself (one intent over 19 owners; extract-with-rewire for the
module) is the measured winner square, on Gene's real code, not yet driven by hand: session 4.
Sol's design review of the plan is running.

**Sol on the fold branch: GO-WITH-FIX.** (1) Replace-in-place silently overwrites a same-key
fact with DIFFERENT content where the old code appended; exports are first-wins and drop a
corrected receipt; recusal identity is not total on sparse entries. Fix: collision policy per
relation, immutable historical facts dedupe by whole value only, upserts keep last-wins,
adversarial samples for same-key/different-content and missing keys. (2) `review.blind-mode-set`
re-folds to lower versions than before; grep consumers (presenter-visibility, expected-version,
etag, policy-version, changed-at) and prove none treats every application as a revision
boundary. (3) Announced speakers: person-id everywhere, normalised name only as a legacy
fallback, person-id on the unannounce fact; two people named Ann must not share a fate. All
dispatched to the fold builder as round two.


## 21:09Z — Sol's design review of the fold refactor: the lens first, tagged identity, characterization before every edit

Gene: *"Isn't that a getter, even cleaner? Let's get sol review on this too!!!"* Sol: step 1 is
the right first move, and a small lens beats a `when-event` wrapper: `(settings state event-id)`
and `(update-settings state event-id f & args)`, update-style varargs, missing event → state
unchanged in one place; **no path function exposed**, because a caller could hand `update-in` a
path with a nil slug. Announced speakers get ONE tagged identity, `[:person-id id]` else
`[:name normalised]` with a total `[:anonymous row]` fallback: tags cannot collide by accident;
a name is only the identity of an unadopted legacy row and must never alias an identified
person; legacy unannounce removes name-identified rows only; adoption that collides with an id
row is a product decision, not a merge. Risk order with witnesses: the lens is a semantic no-op
pinned by before/after projection equality over the full fixture log; relation operations are
product-visible and pinned by focused histories (position kept on replay, same-name/different-id
rows coexist, unannounce preserves id rows, blank names, updates do not reorder); a declarative
relation table only after each relation's semantics are pinned; command keys are write-side and
belong with the store branch. **Structural one-transaction edits are appropriate for the 19
guard eliminations and 20 path rewrites, with projection equality as the gate; identities,
collision semantics and key policy are judgment.** Ordered commits: characterization → lens +
mechanical migration → relation module without changing arms → one announced-speaker arm per
commit → relation metadata later → command keys after the store lands. NO-GO pending product
decisions: merging legacy and id rows by name; the adoption-collision winner; changing ordering
or first/last-wins; collapsing blank identities; reading name-unannounce as removing identified
people; generic declarative relations before semantics are pinned. Session 4 = the lens over 19
owners as one structural transaction, watcher on, replay equality first; it starts when the fold
builder's round two (Sol's fixes + the tagged identity) lands, since both touch the same file.


## 21:24Z — rf2 scored: the rewiring extract verb beats native on every cross-pair; all five promotion criteria met

Receipt: Anvil `~/acid/receipts/rf2-score.md` (scorer `rf2-score.py`; acceptance `rescore-R3.sh`
run on a quiet box before z7c loaded it; the installer's byte-exact `rf2-readout.sh` and an
independent rollout scan agree). Rung R3b at RF2-SHA a66b626, both arms stripped, n=3 each.

| arm | wall mean (sd) | returns | actions | tokens | executed suites | bytes_beyond_verb | acceptance |
|---|---|---|---|---|---|---|---|
| C `:extract! :rewire-callers` | **243.0 s (11.8)** | 14.67 | 10.0 | 638,982 | 3.0 | **0, 0, 0** | a a2 b c1 c3 d PASS |
| N stripped native | 336.0 s (43.6) | 17.33 | 12.33 | 868,020 | 4.0 | 1, 3, 23 | identical set |

C = {230, 246, 253}, N = {291, 339, 378}: no overlap, ratio 0.723, d = -2.91 (Welch df 2.29,
so the p-value is arithmetic, not evidence; the non-overlap is the finding). Pre-registered
readout: A (native bytes after the verb) = 0 in 3/3 — Sol's 60% prior confirmed, Opus's
refuted; B (returns between receipt and first check) = 0 in 3/3 — Sol confirmed, Opus refuted.
Zero `apply_patch` cells in any C run; zero post-receipt re-reads of rewired files; one agent
says so in words ("Per instruction, I'm not reopening the rewired source files"). Acceptance:
PASS sets identical, FAIL sets byte-identical across all six (c2 = test data naming both
qualified forms; e1/e2 = seven rf2-branch tests that read the moved source as fixtures, plus
base's own routing failure) — the set cancels; `e1` ran with `expected=[]` (RF2-BASE.edn not
loaded into the predicate) which must be fixed before any absolute acceptance claim. Against
rf1's bare `:extract!` (31.0 returns, 405.5 s) the rewire flag halves returns and cuts 40% of
wall; against rs1 (328 s) and z7b (330.7 s) returns are equal and wall is ~87 s lower — the
first R3 arm out of the ~330 s band. Sol's five promotion criteria (≥2/3 paired wall wins;
fewer returns to move and total; zero native fallback; equal acceptance; no task-specific code):
all PASS. Caveats: n=3, one task, one base.

## 21:24Z — q5z class fix committed: 2753f23 on bridge/q5z-alias-migration

Agent a7a9731a5e97c7b4c: `binding`/`with-redefs`/`with-bindings` left-hand sides are sites (Vars,
not locals; head sets split); quoted fully-qualified symbols migrate even in files that never
require the lib (`:require-mode :qualified-only`, the `requiring-resolve` case in
sched_import.clj); `#'`, `(var …)`, syntax-quote and metadata values are sites; `'alias/x` and
`::alias/k` are typed refusals with next_call; string literals counted as `string_mentions`,
never rewritten. ALIAS-029..035 with real-bytes witnesses; fails-first 21 failures / 5 tests.
Anchor scratch at d9afe8e9: 171 files, 1872 sites, kondo delta exactly 0, only the six r4-allowed
failures. Verified independently on bridge: test-fast 734/6254 (5 pre-existing routing
failures), mcp-test 399/4467 (1 pre-existing). Anvil: `surgeon-q5z` checked out at 2753f23;
`restart-7895-at.sh 2753f23` was launched inside an ssh whose wait timed out — the child kept
running (memory: timeout kills the wait, not the child); a monitor waits on that pid and then
reads 7895's ready.edn, Q5Z-SHA, and chain-sl1r before the anchor run is called re-armed.


## 21:25Z — the finder: a structural scan of folds.clj found Andrew's class a second time (task chases)

Gene: *"Can we create kickass LID assertion, and maybe could even be generalized so that we find
other areas of vulnerability"* … *"does it uncover more kickass surgeon primitives that humiliate
… anyone stuck with Grep and RG"*. Two `inspect_clojure` calls (bridge 7888, workspace
`~/src/curtaincall-cfp-fold`, file hash e4bafd32…, 1.19 s + 0.58 s), fourteen `match` patterns over
the whole fold namespace: `(conj _ _)` 4, `(cons _ _)` 1, `(fnil conj _)` 2, `(into _ _)` 1,
`(concat _ _)` 0, `(update _ _ conj _)` 0, `(update-in _ _ conj _)` 0, `(update-in _ _ (fnil conj _) _)` 1,
`(remove _ _)` 1 — each match reported with its enclosing form and call path. Classification:
line 67 `(conj seen current)` and line 95 `(into base added)` are inside the pure
`effective-submission-speakers` (not fold writes); 163/170/183 are the bodies of the builder's
own `conj-once`/`cons-once`/`upsert-by` doors (round two renamed the helpers; 12 references);
line 553 email templates hand-roll an upsert-by-id (safe; a third spelling of `upsert-by`);
line 908 agenda selections conj into a set (safe); **line 721 `fold-task-chase`:
`(update :chases (fnil conj []) chase)` + `(inc chase-count)` — unguarded: a retried
`task.chased` appends twice and double-counts. Same class as Andrew's duplicate.** Sent to the
fold builder as a round-two item (identity `:chase-id`, witness two-identical-events → one chase).
The unannounce arm now reads `(remove #(= target (announced-speaker-identity %)) rows)` — the
tagged identity is in. Why grep cannot do this: it cannot tell a conj inside a pure helper from
a fold write, cannot match a form broken across lines, and cannot report the enclosing branch.
**The generalized LID (the relation law), for the next round:** for every fold arm writing into a
collection, adding the same fact twice equals once; removing by identity removes exactly the
rows with that identity; after replaying any log every collection is a set under its declared
identity; a relation with no declared identity is a typed refusal — the refusal list IS the
vulnerability finder. **Primitive to build:** a relation-write census verb — every write into a
collection in state, classified by identity door (distinct-by / upsert / set / raw) — one call on
any event-sourced repo. Filed to the maven inbox.


## 21:28Z — store-idempotency built (70c823cf, unpushed); main-loop review found a product-breaking forever key

Agent a93309b7f3a7f903b delivered STORE-IDEM-001/002 on `bridge/store-idempotency` (base a02d50a3,
five commits, +655/−50): `append!` checks the declared `:idempotency-key` against a fold-derived
`:idempotency-keys` index inside the write lock, returns a typed duplicate receipt without folding
or firing sinks (fire-sinks! dispatches under the same lock, which is why the refusal is sufficient);
Postgres partial unique index on (COALESCE(event-id,''), key) with 23505 mapped to the same receipt;
`append-all!` refuses keyed events; announce verbs pass `announced-speaker:<event>:<person>` and
drop check-then-append; five witnesses fails-first (the race witness appended 3 facts before the
fix); unit 1015/12605/0. Found-not-fixed by the agent: `record-participation!` still
check-then-appends (needs a product decision); two divergent `empty-state` literals.
**Review finding (mine, reading the diff):** the registry boundary says a forever key is legal only
where the relation has no remove verb and claims announced-speaker qualifies "because there is no
announced-speaker-removed fact". False: `event.speaker-unannounced` removes from the same
`:announced-speakers` collection. Announce → unannounce → re-announce would be refused forever, in
memory and at the PG index — Ann's exact workflow (unpublish, publish again). Fix spec sent: a
generational key `…:<gen>`, gen = unannounce facts already folded for that identity in that event,
derived in fold-one; racing announces share a gen (one refused), an unannounce advances it; the PG
index is unchanged; three fails-first witnesses (announce/unannounce/re-announce → two facts, one
row; racing re-announces → one; replay rebuilds the counts). Sol red-team of the store diff running
in parallel (`fold-review/sol-store-review.md`). Lesson for the ratchet ladder: a typed refusal is
only as correct as its identity rule; the builder's own boundary sentence was the oracle and it
was written with a false premise — the review has to check the premise, not the code.


## 21:30Z — 7895 serves 2753f23; anchor chain re-armed; store branch verified independently

Anvil `~/acid/receipts/7895-start.edn`: attested-sha 2753f23 via ready.edn → project-root → git
rev-parse (written 21:16:59Z, pid 382174, healthz ok). The restart ssh hung for 12 minutes
because `| tail -3` on the restart script inherited the JVM's stdout pipe; killing that tail let
the command list finish: `Q5Z-SHA` = 2753f23, `chain-sl1r` armed (waits on the cohort lock held by
chain-z7c, then preflights against Q5Z-SHA and runs sl1-R). Apparatus note: never pipe a script
that starts a long-lived server; redirect its output to a file. Store branch 70c823cf verified
on bridge by my own run: `bin/kaocha unit` 1015 tests, 12605 assertions, 0 failures. Sol's
red-team of the store diff relaunched from a neutral cwd — the first launch died because
curtain-call's `.codex/config.toml` demands the retired cclsp MCP server (the same trap the
anchor needed `strip-repo-mcp.py` for).


## 21:32Z — Sol red-team of the store branch: NO-GO, nine items; the generation moves inside the lock

Receipt: `scratchpad/fold-review/sol-store-review.md` (codex exec gpt-5.6-sol, read-only, from a
neutral cwd). Sound: replay rebuilds the key index through fold/fold-one on every path (load!,
checkpoint + tail, as-of); `append-all!` cannot write a keyed fact. Findings and rulings sent to
the builder: (3) **my generation spec was wrong in the same way as the original check-then-append —
the caller computed gen outside the lock**; ruling: the caller declares a rule
(`:idempotency {:relation … :event-id … :identity …}`), `append!` derives key+gen inside the write
lock and stamps the concrete key on the line; single instance makes the lock the boundary, and a
stale gen across instances degrades to a visible refusal, never a duplicate. (2) any 23505 was
read as our duplicate → only the idempotency index's constraint name maps; others rethrow.
(4) memory trims keys, PG indexes raw text → normalise before serialisation. (5) fold throwing after
a durable write leaves the key durable but absent from the atom with matching marks → invalidate
the mark. (6) durable-duplicate receipt carried `existing nil` → refresh and populate, or a typed
`:unavailable`. (8) memory keyed by key, PG by (event-id, key) → scope by event in both.
(7) `:already-announced` was an unread URL parameter → flash; different body → typed `:conflict`.
(9) privilege refusal on the index logged-and-continued → readiness fails closed when PG is
configured and the exact index is absent. (1) `record-participation!` still check-then-appends
`speaker.added-to-event` → own rule; forever key only if participation has no remove verb (Gene's
item). Pattern worth naming: three of nine are "the verifier's premise was false" (forever key,
any-23505, IF NOT EXISTS as proof of definition) — the review has to check premises.


## 21:33Z — fold round two at f115cc2d: per-relation policy, tagged identity, Ann's sequence pinned; my task-chase finding was a false positive

Agent a8fea285fa6efe9e5, five linear commits on `bridge/fold-idempotence`, unpushed until my run.
`conj-distinct-by` is gone; three named policies: `conj-once`/`cons-once` (immutable facts, identity
= whole value: comments, versions, export receipts, recusal log, blackouts) and `upsert-by`
(submission speakers by person-id else email else value; announced speakers by the tagged
identity `[:person-id id]` → `[:name normalised]` → `[:anonymous row]`). Fails-first: 19
assertions red at a896608d, green at f115cc2d. Two arms carried defects reachable only by replay:
`event.speaker-unannounced` removed every same-name row (Ann's report — now by tag, name-only
payload removes name-identified rows only; witness `two-anns-then-unannounce`); and
`event.announced-speaker-adopted` — **new finding, worse than the riff assumed** — did not update the
adopted row, it removed it and appended a `select-keys` copy WITHOUT the person-id, silently dropping
identity; now it claims only an unidentified row, and an adoption collision is kept-not-merged as a
documented open case (FOLD-IDEM-003 boundaries). Characterization golden: the shipped judge-sandbox
log (3,246 facts, 14 legacy announces) projects byte-identical; two synthetic histories blessed.
`review.blind-mode-set` advance-on-change verified against every consumer (the writer already refuses
a same-mode fact at review_plan.clj:210). Dead writer found: `events/unannounce-speaker!` has zero
callers; legacy announce is reachable only from the judge sandbox. Gates at f115cc2d: unit
1016/12599/0, ci, compile-check, test-js 18/18, property 121 arms 0 gaps; Prolog oracles UNVERIFIED
(swipl lacks plunit on this box).
**My finder result was wrong on its one positive:** `fold-task-chase`'s `(fnil conj [])` sits inside
`(not-any? #(= (:chase-id payload) (:chase-id %)) …)` three lines above the match. The structural
scan matched the write form and not its guard. Pinned by the builder (green before and after: a pin,
not a ratchet). Lesson for the census verb (inb-f5ee92): the identity door can be a predicate in an
enclosing branch, not only a named helper; classify by the guard, and report "raw" only when no
enclosing predicate mentions the written value's identity. Declined by the builder, correctly:
folding `comms.template-saved` into `upsert-by` (nil-id templates would change projection; no
fixture facts to prove equality). Corrected the record with Gene by voice.


## 21:36Z — fold branch pushed at f115cc2d after my own run (unit 1016/12599/0); inb-d603ce updated for Gene's merge


## 21:59Z — store branch 3aac4338: my run 1032/12800/0; Sol round two NO-GO, converging; round three rulings

Builder's round: all nine rulings landed with fails-first witnesses, rebased on f115cc2d; the
generation is derived inside the lock from a caller-declared rule; the comparable-body digest is
stamped on the fact so conflict detection survives replay; participation kept BOTH guards because
`domain/speakers.clj` writes the same relation through `append-all!` (the suite caught the
"drop the check" version: portal-test 20 → 21). My own unit run: 1032 tests, 12800 assertions,
0 failures. Sol round two (`scratchpad/fold-review/sol-store2-review.md`): CLOSED 2, 4, 5, 8;
PARTIAL 1, 3, 6, 7, 9; six new. Rulings sent as round three: (A) `append-all!` honours rules under
the lock, skipping already-claimed facts and listing them in the receipt, so every writer of
`speaker.added-to-event` carries the key and the precheck goes; (B) `:comparison :unavailable`
becomes a distinct unverified outcome, never "nothing was lost"; (C) the conflict banner names
exactly the refused relation and says the profile/program writes in the same request were
applied — no gesture reordering; (D) readiness compares the schema-qualified `pg_get_indexdef`
against the exact definition; (E) SHA-256 over canonical `pr-str` replaces the 32-bit `hash`.
Held out of the builder's scope: cross-instance generation serialisation — Gene runs
max-instances=1, and that precondition goes into STORE-IDEM-001's boundary verbatim; live
Postgres verification — owner work. The z7c scorer and sl1-R are still in flight.


## 22:12Z — z7c scored: the gate is wall-neutral at n=6; z7b's 0.76× withdrawn; correctness 6/6

Receipt `~/acid/receipts/z7c-score.md` (scorer `z7c-score.py` + `z7c-admit.py`, walls from
`z7c.log` end lines slot-mapped 1:1 to `acid_arm=` starts, arm letters corroborating; acceptance
read only after all twelve `rescore-R3 done` markers). Mirrored order "N Z N Z N Z | Z N Z N Z N".

| arm | n | wall (sd) | returns | actions | tokens | suites | acceptance |
|---|---|---|---|---|---|---|---|
| Z gate 17125fe | 6 | 339.3 (62.6) | 17.50 | 12.17 | 969,920 | 2.0 | 6/6 all PASS |
| N stripped | 6 | 348.2 (48.3) | 18.00 | 12.33 | 856,732 | 2.0 | 6/6 all PASS |

0.975×, Welch p 0.79; walls interleave completely (Z owns the fastest and the slowest run). The
chain's pre-registered falsifier ("Z within the 86 s floor sd of N") fires at 8.8 s; the 0.85×
claim misses by 42 s. Mechanism now visible: **z7b's native arm was slow, not its gate arm fast** —
stripped native on the same prompt and base reads 327.7 (rs1) / 432.7 (z7b, carried by one 552 s
run) / 348.2 (z7c); the gate arm moved 330.7 → 339.3. Pooled every stripped run on the rung, gate
n=9 vs native n=12: 0.924×, p 0.355 — nothing clears the floor. Correctness, counts not estimates:
7 commits all `complete`, `verify focused` on 18/18 admit calls, `verify none` never used, 108/108
acceptance predicates PASS, churn canonical 53 in all twelve runs of both arms, apply_patch on .clj
0 in every Z run. Refusals bimodal: 8, all in two runs (462 s / 27 returns / 1.55M tokens and
345 s / 19 / 1.12M) — 4 patch-does-not-apply (stale hunks), 2 invalid-admit-request, 1
invalid-patch (the agent piped `/bin/bash: line 3: ruby: command not found` into the gate as a
patch; refused in 0.00 s), 1 verification-failed (blocking-lint-findings, 7.5 s — the one
substantive catch). Gate 25.3 s per run, 7.4% of wall; the suite-saving seen once in z7b did not
recur (every run both arms executed exactly 2 suites). Z tokens +13%, driven by the two refusal
runs. **Standing claim for the gate: correctness (every commit verified, no waiver path), not
speed.** Second cohort today where a small-n speed win was a slow native trio (z3→z6, z7b→z7c):
rule stands, n≥6 before any wall claim. rf2's 0.723× remains the only within-cohort speed win on
this rung. Tech tree E1 and the Gene report §1/§3 corrected in this commit.

## 22:14Z — store round three at f568d595: every writer claims the key; a real 32-bit collision in the witness

Builder: (A) `append-all!` honours rules under the lock — a claimed key is skipped (not written,
not folded, no sink) and named in `:skipped-duplicates`; both participation writers declare
`folds/speaker-participation-rule` (constructors live in folds because domain-architecture-test
forbids domain → store) and the precheck is gone; (B) `:already-announced-unverified` with a
warning banner; (C) the conflict banner names the refused relation and says the rest WERE applied;
(D) exact `pg_get_indexdef` match + `indisunique`, schema-qualified — written from knowledge,
unverified against a live server, so a first Postgres boot may refuse and print expected vs
actual (documented first-deploy note); (E) SHA-256 over canonical `pr-str`, and the witness carries
a birthday-search collision: two real announced-speaker bodies differing only in org hash to
123905342 under the old digest, which reported `:already-announced`; now `:conflict`. Three of the
builder's earlier assertions were rewritten in place with notes naming their successors. Builder's
gates: unit 1038/12872/0. My own unit run and Sol's third pass running in parallel; push on both.


## 22:16Z — store f568d595: my run 1038/12872/0; Sol round three GO-WITH-FIX (single instance); two mechanical fixes as the last round

Sol (`scratchpad/fold-review/sol-store3-review.md`): original nine → CLOSED 1, 4, 5, 6, 7, 8;
PARTIAL 2 (message-fallback `names-constraint?` uses substring membership) and 9 (the expected
indexdef literal is unverified against a live server — an availability risk, not a false green);
ACCEPTED-AS-PRECONDITION 3 (cross-instance generation; max-instances=1 recorded verbatim in the
registry and docs). Round-two holes: CLOSED 2, 3, 4; PARTIAL 5 (same as 9), 6 (`canonical-value`
is not a general canonical encoder: sets → vectors, lists collapsed, non-EDN objects print
identity). Round-three mechanics verified: a key claimed earlier in the batch is seen through the
local `working` fold; a skipped fact is neither written, folded nor sink-fired; later facts proceed;
a mid-batch durable failure throws and invalidates marks; no single-instance path returns success
without writing and folding. Round four (last): exact quoted-name extraction in the fallback;
comparable-body domain = JSON/EDN data with a typed refusal for sets/lists/objects, sorted-key
recursive encoding. Owner work before a Postgres-backed deploy: install the index, capture the real
`pg_get_indexdef`, correct the literal if needed (the app refuses to boot until it matches); before
ever raising max-instances: database-side generation serialisation and `append-all!` handling of a
durable losing claim. Push after round four and my own run.


## 22:25Z — the win as four storyboards (Gene: "4. Exactly what the win is — Show in ascii art storyboards" … "Add to captain log. Amazing.")

**1. The extraction (rf2): native cuts and sews by hand; the verb does both in one call.**

```
NATIVE (stripped, n=3, mean 336 s, 17.3 returns)        VERB :extract! :rewire-callers (n=3, mean 243 s, 14.7 returns)

 read ns ─▶ patch A (cut 9 forms) ─▶ patch B (paste,      read ns ─▶ ONE CALL {:file :forms :to :rewire-callers}
 hand-write header) ─▶ grep callers (26 sites, 5 files)              │ 1.3 s of tool time
 ─▶ patch C, D, … (2.8 apply_patch on .clj per run)                   ▼
 ─▶ compile ─▶ missed caller, back to patch ─▶ 4 suites   RECEIPT {:applied true :complete true
                                                                    :callers-unresolved [] :compile 5 ns ok}
 t = 336 s (fastest 291)                                    ─▶ compile ─▶ 3 suites   (B = 0 returns between receipt and check;
                                                            t = 243 s (slowest 253)   A = 0 native bytes after the verb, 3/3)
 Every verb run beat every native run: {230, 246, 253} vs {291, 339, 378}. Acceptance byte-identical.
```

**2. The fan-out (sl1, alias migration N=5…80): native pays per site; the verb pays once.**

```
 N=5   native: grep→edit ×5 → suite            55 s,  3 returns, PASS   │ verb: ONE CALL {from to}   24 s, 2 returns, PASS
 N=20  native: grep→sed→collision→fix→suite    90 s,  7 returns, FAIL   │ verb: same call            25 s, 2 returns, PASS
 N=80  native: writes a Python generator      127 s, 11 returns, PASS   │ verb: same call            27 s, 3 returns, PASS
 control: 240 sites in 5 files cost native the same as in 80 files → the cost is site DISCOVERY, not typing.
 Wall 4.5× at scale, returns 2×, native 2/6, verb 6/6. Real repo: 171 files, 1,872 sites, one call, kondo delta 0.
```

**3. The gate (z7c, n=6): same speed, different last frame.**

```
 NATIVE 348 s:  read → patch → patch → compile → suites → git commit        (agent decides it is done)
 GATE   339 s:  read → patch → patch → compile → suites → admit_clojure_patch verify:focused
                                                            ├─ REFUSED 0.01 s  (bash error text piped in as a patch)
                                                            ├─ REFUSED 7.5 s   (blocking lint in the post-image)
                                                            └─ COMMIT complete  7/7 commits, 0 on verify none
 0.975×, p 0.79: FLAT. The gate buys the last frame (every commit verified, no waiver path); it costs 25 s/run
 and a fat tail (8 refusals, all in the 2 slowest runs). A ratchet, not an engine.
```

**4. Where the win stops.**

```
 (a) nothing to discover (rung L control, z8): native 149 s, gate 277 s (1.86×) — the gate's own suite is pure cost
 (b) free choice: tool called 0 / 10 — the win exists under MANDATE only
 (c) small n: z3 0.80× → z6 1.00×; z7b 0.76× → z7c 0.975× — no wall claim below n=6
 (d) structural match sees the write, not the guard: (fnil conj []) flagged raw; (not-any? #(= chase-id …)) three lines up
```

**pmap at large N — position (Gene: "Do we benefit from pmap in large n?").** Not on the migration
verb's write path: in the anchor arm the verb is seconds inside 228 s, the wall is the agent's returns
and the suites, and the commit must stay serial and all-or-nothing. Yes on whole-repo READ verbs: one
`inspect_clojure` call with 8 match patterns over one ~1,000-line file measured 1,191 ms (0.57 s for 6),
so a census over 171 files is ~3–4 min serial vs ~15 s on Anvil's 16 cores — the class where the verb
itself becomes the wall (census, `prepare-change` proofs, the finder). Rule: parallelise parse/match/
plan per file with a bounded executor (bare `pmap` is lazy, chunked, no back-pressure), serialise the
plan merge and the write; kondo already parallelises internally. Receipt gap: the alias-migration
receipt carries no per-phase `elapsed_ms`; the migration verb's own elapsed in the anchor is
UNMEASURED, not small. Decision: migration stays serial; the census verb (inb-f5ee92) gets a parallel
plan phase from the start; every receipt gets per-phase elapsed (facts, not verdicts).


## 22:28Z — sl1-R scored: both arms PASS, the fixed verb loads the tree, the tool wins every cost axis where native was predicted to win; the chain's "pass" was a noun

Receipt `~/acid/receipts/sl1-R-score.md` (scorer `sl1R-score.py`, rollouts 1:1 by worktree path;
acceptance by running `rescore-FAN-R.sh` on each worktree; diffs produced by hand at 22:20Z).
Anchor = curtain-call at d9afe8e9, rename store → event-store, alias migrated everywhere; T = q5z
2753f23 on 7895, N = native; gpt-5.6-sol; n=1 per arm.

| arm | wall | returns | actions | tokens total (out) | suites by arm | r1–r7 | load |
|---|---|---|---|---|---|---|---|
| N | 283 s | 14 | 13 | 499,455 (12,286) | 1, RED at load | PASS, r7 4/4 | 190 ns |
| T | 228 s | 13 | 9 | 321,834 (2,851) | 1, 2 failures | PASS, r7 4/4 | 190 ns |

T/N: wall 0.81, actions 0.69, tokens 0.64, output tokens 0.23; acceptance a tie (both 1007 tests /
12232 assertions / 2 failures, the two string-literal expectations in store_architecture_test that
the carve-out protects; four of the six allowed failures never fire because both arms repointed all
four path literals). The worktrees differ in one line of one file (mail.clj: N kept the fully
qualified form, T normalised to the alias). **The fixed verb loads the tree**: at 13d86bb the same
call left `1 tests, 1 errors` (replay.clj:128 `binding [store/*clock*]` unmigrated); at 2753f23
r3 = `:load-ok 190`. T's verb receipts verbatim: call 1 REFUSED `alias-migration-expect-mismatch`
(agent guessed expect.files 176, found 171; source unchanged; next_call handed back and re-sent as
is); call 2 COMMITTED 171 files / 1,872 sites / 0 collisions / string_mentions 4 / 62.1 s inside the
call; kondo_delta not requested. Bytes beyond the verb: 4 files × 1 line, exactly the four carved-out
path literals the verb reported and declined; zero corrective bytes. Native wrote a Python lexer,
generated one patch, then re-derived the token census in three more cells; its single permitted suite
run was RED at load (`#'store/*default-sinks-fn*` in sinks.clj:693 — the same reader-quoted class that
killed the tool arm at 13d86bb), it fixed by hand and shipped without re-running. The chain's own
prediction — "native is EXPECTED to win here (uniform alias, one sed)" — did not hold: a uniform alias
does not make the *verification* uniform; native spent its budget proving completeness (4 census
cells, 487k input tokens), which the receipt discharges. Caveat: n=1, 55 s gap, quality tie.

**Apparatus false green, three defects, all fixed on Anvil this hour.** (1) chain-sl1r's line
`sl1-R scored -> … pass`: `pass` was a hardcoded noun after `$(grep -c …)`, the count was empty
because the file did not exist, and an earlier run had printed `0 pass`; the token never once
meant success. (2) No FAN arm has EVER produced a diff: the runner's `git add -A -- . ":!.cpcache" …`
fails (exit 1) when the ignored `.cpcache` exists, because a negative pathspec makes git treat the
ignored path as explicitly named; `&&` then skipped the diff. `sl1-score.md` never noticed because it
scored worktrees directly. (3) The runner's acceptance line names `rescore-FAN.sh` (synthetic) for
the anchor, which cannot score R. Fixes (v5 canonical + v3 executing copy + chain-sl1r.sh, `bash -n`
green, backup kept): `":!.cpcache"` dropped and a `DIFF-FAILED rc=` line written to the run log on
any add/diff failure; the acceptance line names `rescore-FAN-R.sh` when FAN_N=R; the chain aborts
on an empty diff glob and on an empty score file, and prints `scored= passed= failed=` counts
instead of a label. Delivery invariant 20 in the flesh: the verifier printed a word about a subject
it never examined.

**Store branch pushed**: `bridge/store-idempotency` 96387535 (my run 1040/12908/0); inb-70711c
for Gene's merge, after fold (inb-d603ce); owner work and the max-instances=1 precondition named.

## 22:30Z — claypoole ratified for the plan phase (Gene: "I love using claypool pmap that uses thread pool and is eager.")

The census verb (inb-f5ee92) and any whole-repo read verb parallelise parse/match/plan with
`com.climate/claypoole` — `cp/upmap` over files on a `cp/threadpool` sized to the box inside
`cp/with-shutdown!`; eager start, bounded pool, worker exceptions rethrown at the consumer, the
merge re-keyed by path so order is irrelevant. The write/commit phase stays serial and
all-or-nothing. Receipts carry per-phase `elapsed_ms` so the parallel win is a measured number
(baseline: 1.19 s per file for 8 match patterns; 171 files ≈ 3–4 min serial vs ~15 s on 16 cores).


## 22:33Z — autonomy for the day: session 4 and the census verb launched in parallel (Gene: "No word needed. Use best judgement." / "I'm busy all day today. Keep going!" / "Or get sol opinion.")

Worktrees: `~/src/curtaincall-cfp-lens` = `bridge/settings-lens` at 96387535 (stacked on the store
branch; 19 guards, 23 settings paths counted); `~/src/clj-surgeon-census` = `bridge/census-verb` at
origin/main 8ac4332 (fetched, recorded). Session 4, step A (Opus builder): characterization first —
replay digest, the guard's three edge cases over the 19 event types, a structural inventory pinning
19/23 — then the lens with no call sites (LENS-001/002). Step B after A: the migration measured as
two arms — one structural transaction through Surgeon over the 19 owners driven by me with a watcher
(returns + stopwatch) vs an Opus agent natively on a second worktree; gate = the characterization
digest + unit suite. Census verb (Opus builder): `relation_census`, classification :door / :set /
:guarded (enclosing branch mentioning an identity key of the written value) / :raw; claypoole upmap
plan phase; allowlisted receipt with per-phase elapsed_ms; LID CENSUS-001..; real-bytes fixture from
folds.clj; pool-1-vs-N identical-answer witness; real-wire witness. Sol consulted in parallel on
the plan (`scratchpad/fleet/sol-plan-s4-census-answer.md`): KEEP / ADJUST / STOP per build, what
must not run unattended, the guard rule he would ship. His answer is folded in before step B starts.


## 22:35Z — Sol on the day plan: ADJUST both; dry plans before arms; the census gets `:unknown` and evidence

`scratchpad/fleet/sol-plan-s4-census-answer.md`. (a) The two-arm migration is legitimate as an
instrument, not as the decision procedure: produce BOTH plans dry first (native exact patch with
preconditions; Surgeon structural plan) and inspect match cardinality, ambiguity, projected churn,
and whether Surgeon targets forms or reprints owners — the house rules already record owner-scoped
`apply_clojure_changes` as a measured loser on fan-out. Native is the production default for this
migration; the Surgeon arm is research. Unconvincing if: arms not byte-identical at start, prompts
differ, warm-up charged to one arm, "one transaction" is 19 reprinted owners, native constrained to
per-form edits, success judged by tests alone (churn and tree equivalence matter), uncounted retries,
n=1 presented as general, winner chosen by speed. (b) `:guarded` as "an enclosing branch mentions an
identity key" overclaims: misses let-bound predicates, cond/case, membership idioms, threading,
helper-carried guards, early returns; falsely blesses same-key-different-value, wrong polarity,
existence vs nonexistence, `or` paths, shadowing, checks after the write. Ship rule: `:guarded` only
with dominance + same target + written value's identity expression + correct polarity; trace only
single-assignment let aliases and standard control forms; explicit membership idioms; no helper or
macro inference in v1; add `:unknown` with reasons; `:raw` only when positively shown; return evidence
(write, target path, identity expr, guard, polarity, uncertainty). The census locates review work; it
does not prove idempotency and is not an enforcement gate. (c) Nothing irreversible on branches; the
unattended hazards are decision boundaries: never auto-select and push "the correct tree" (nominate
in a receipt; Gene merges); no force-updates of stacked branches; no broad owner reprints; counts are
inventory tripwires, not invariants; no fence widening for recall; wire fields versioned.
**Rulings applied:** census builder re-specced (five classes, four-condition guard rule, evidence
fields, complete-output pool invariance, `:census-version 1`); lens builder told to compare the full
projection with a readable first-difference report and to name the count test a tripwire. Step B
redesigned: after step A, dry plans first (Surgeon `inspect_clojure` prepare-change/plan over the
19 owners on the lens worktree — read-only; native plan as one exact patch written by an Opus
agent on `bridge/settings-lens-native`, not applied), then the plans' cardinality/churn decide
whether the measured arms run at all; both trees stay on experimental branches with a receipt
nominating a winner; Gene merges.


## 22:50Z — the mayor relays Gene's nudge: study ops through the MCP, one kernel, now

Mayor, via the channel, with Gene verbatim: *"Nudge bridge: really think this needs to be done sooner
rather than later"* — on the finding that `:ls-tree` (and `:ls-deps`, `:deps`, `:topo`, `:ls-extract`,
plus the write ops `:mv`, `:rename-ns!`, `:fix-declares!`) live only in `core.clj` and appear nowhere
in the MCP surface (git grep on origin/main: zero hits in mcp_schema / mcp_tool / mcp_contract).
Why it matters, the mayor's reading: vision.md names square 3 (the questions grep answers wrong) as
winnable and `:ls-tree` as its foundation; it is the discovery half of the fan-out verb, what the
gate needs to name its owner delta, and the precondition for square 4. Constraint held: both
entrances call ONE kernel (docs/plans/one-compiler-two-entrances.md), never a forked path — the
class we closed three times yesterday. Study ops first because they are read-only and add no
refusal surface; write ops stay behind the gate. Tracked clj-surgeon-0me (mayor's). Decision under
day-autonomy: build now. Worktree `~/src/clj-surgeon-study` = `bridge/study-ops-mcp` from origin/main;
Opus builder; fails-first witnesses incl. the real wire; bounded allowlisted receipts. Merge note:
this and `bridge/census-verb` both register a tool in the MCP schema files; rebase the second lander.


## 22:55Z — session 4 step A landed: LENS-001 pin (d10a6009) + LENS-002 lens (55d1fd3f); the 19 arms and 23 sites are named

Builder's gates: unit 1040/12908/0 → 1044/13052/0 → 1050/13097/0; compile-check green; kondo clean.
LENS-001 is a pin (mutation probes: a renamed settings key failed the oracle naming `event.hero-set`
and the drift report printed the path with both values; one `if-let`→`when-let` tripped the
tripwire "18 copies, not 19"); every pre-existing golden digest survived byte-for-byte; the golden
now carries the whole projection beside the digest with a `clojure.data/diff` first-difference
report (empty relations pruned — `empty-state` carries 40 of them). LENS-002 fails-first in two
stages; missing event → `identical?` state AND `f` never called (call counter); no key and nil key →
`f` applied to nil, identical results. The 19 event types: schedule.locked, schedule.unlocked,
agenda.published, replay.marked, sink.registered, sink.removed, api-key.created, export.generated,
api-key.revoked, event.hero-set, event.email-notifications-set, event.day-hours-set,
event.unlisted-set, event.submission-cap-set, event.blind-review-set, event.speaker-unannounced,
event.speaker-announced, event.announced-speaker-adopted, event.announced-speaker-added. Guards at
folds.clj 628 636 644 673 680 686 695 709 722 1062 1067 1073 1080 1086 1091 1100 1148 1164 1184;
settings paths at 629 637 645 674 681 687 696 723 1063 1068 1075 1076 1081 1087 1093 1095 1110 1145
1157 1173 1175 1189 1201 (+174, the lens itself). Traps for the migration: `export.generated` is a
guard but writes `:exports`, not settings; `event.program-speaker-updated` (1201) writes settings
under a different guard (event AND person); 1145 is a read; three arms carry two paths each;
`announced-speaker-adopted` can no-op on a present event. Suspected src defects reported, not
fixed: the intent-registry test's `(deftest\s+([^\s\)]+)` regex mis-parses metadata on a deftest
name; `sink.removed`/`api-key.revoked` materialise `{:webhooks nil}`/`{:api-keys nil}` on an event
that never had one. Next (Sol's order): dry plans on both sides before any arm — native exact patch
on `bridge/settings-lens-native` (worktree created at 55d1fd3f, not applied), Surgeon plan via
`inspect_clojure` over the 19 owners with the watcher on.


## 23:01Z — session 4 dry plan, Surgeon side: 16 arms in ONE transaction, zero churn outside the forms, projection gate green (Gene: "Study surgeon usage and usefulness! Seems perfect for the job!!!")

Driver calls (watcher on from transcript offset 21805140; receipts in
`~/src/curtaincall-cfp-lens/.tweezer/session-4-watch.md`), scratch worktree
`~/src/curtaincall-cfp-lens-scratch` (detached at 55d1fd3f) so the lens tree stayed untouched:
1. `inspect_clojure` outline of folds.clj — 1.85 s, 139 forms; **deviation (receipt/schema): every
   `defmethod` is named `fold-event` with no dispatch value**, so the 19 arms cannot be addressed from
   the outline.
2. `forms` probe with a guessed owner `fold-event "schedule.locked"` — refused in 0.16 s with the
   22-name owner vocabulary; all ~117 arms collapse to one owner name. The addressing answer lives
   only in `apply_clojure_changes`' schema: `forms: [{kind: defmethod, name, dispatch}]`. A cold
   agent pays at least one refusal to learn that (return-tax y).
3. `match` for the guard pattern (19/19, each with full source, hash, preorder address, enclosing
   form) + `[:events slug :settings _]` (21; the two 5-element paths need a second pattern) — 0.6 s.
   One read gave the whole migration's content.
4. `apply_clojure_changes`, ONE call, 16 `changes`, each owner = the arm's dispatch, `find` = the
   guard form verbatim (interior INTENT comments included in two arms — accepted, spelling preserved),
   `replace` = the lens form — 3.8 s (formatter 0.7 s), `committed true`, `verification_complete true`,
   undo receipt written.
Churn: 58+/83−, 7 hunks, **every changed line inside the 16 replaced forms** — the filtered residue is
the sixteen `state))` closers and nothing else. Gate on the scratch (`bin/kaocha --focus` the two
lens namespaces): **20 tests, 250 assertions, 6 failures, all six the inventory tripwire** (19→3
guards, 24→6 path occurrences; counted twice because the ns loaded under both focus flags) —
whole-projection replay equality, the 19-arm oracle, the three edge cases and LENS-002 all green.
Excluded from the transaction, with reasons: `export.generated` (a guard but writes `:exports`, not
settings); `event.speaker-unannounced` and `event.announced-speaker-adopted` — conditional arms that
return `state` untouched on a present event; through the lens, an absent `:settings` would be
materialised as nil where the original left it absent, and the golden would catch it. **Plan
precondition LENS-003:** `update-settings` returns `state` unchanged when `f` returns the identical
settings value (`identical?`, nil included); then those two arms migrate too. Surgeon plan artifact:
`~/src/curtaincall-cfp-lens/.plan/surgeon-settings-lens.patch`; native plan pending from the agent on
`bridge/settings-lens-native`. Sol's unconvincing-if list, checked: same base sha both sides (55d1fd3f);
the Surgeon side is form-scoped, not owner-reprinted; churn measured; the correct tree is not
auto-selected — comparison receipt next, Gene merges. Session marker written.

