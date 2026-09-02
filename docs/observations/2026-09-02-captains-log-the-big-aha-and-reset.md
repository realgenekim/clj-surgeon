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
