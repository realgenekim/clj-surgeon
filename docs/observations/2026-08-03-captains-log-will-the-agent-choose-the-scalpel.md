# Captain's Log: will the agent choose the scalpel?

The next question is not whether clj-surgeon *can* perform an exact edit. It
can. The more interesting product question is whether a clean coding agent,
given a fair choice, prefers it over the text tools it already knows.

That distinction matters. A benchmark that says “use clj-surgeon” measures
instruction following. A benchmark that hides the executable on `PATH`
measures accidental discovery. Neither tells us whether the interface has
become the agent's preferred instrument.

The clean experiment has three separate moments:

1. **Discovery:** give only the desired outcome. Does the agent find an
   unadvertised structural CLI?
2. **Choice:** add only “The clj-surgeon CLI is installed and available.” Does
   the agent voluntarily choose it over shell readers and line patches?
3. **Onboarding:** install the normal skill. Does the complete product teach a
   correct one-shot route without task-specific prompting?

We almost mixed those moments into one 48-cell factorial. An independent
critique caught the confounds. A version-matched skill changes policy and
initial context, not just capability. Naming `[:partition-all 2]` against a
pre-feature binary advertises an impossible operation. Asking the agent to
“choose the fastest route” telegraphs the desired answer. Those runs might be
interesting diagnostics, but they cannot establish voluntary preference.

The corrected primary study uses only outcome-only and neutral-awareness
prompts, four repetitions each, in isolated neutral repositories. The exact
same real-program-derived fixture is overlaid into pre and post environments.
The version-specific checkout contributes only its executable. Skills and
operation hints are separate follow-on studies and are labeled accordingly.

The write control is especially revealing. A familiar line edit can look
cheap because the command is short:

```text
search → print context → patch → print/diff/hash again
```

But the apparent single edit hides several proof obligations. Did the search
identify syntax rather than a comment or string? Was the intended peer chosen?
Did the patch touch the duplicate elsewhere? Did source change between read
and write? Is the resulting file parseable? Did verification read the same
bytes that were written?

The structural route makes those obligations one artifact:

```text
select + transform → reviewable hash-fenced plan → explicit apply → verified receipt
```

The plan is still non-writing. Application remains a later command because
human/model review is a real consent boundary, not latency to optimize away.
“One-shot editing” therefore means one expression states the complete
selection and transformation. It does not mean planning and mutation are
silently fused.

This exposes a product-language opportunity. `:q` is already both getter and
planned updater, but its name advertises query more strongly than edit.
`:replace-subform` advertises mechanics rather than intent. In the Unix-shaped
surface—`:ls`, `:cat`, `:grep-form`, `:q`—the obvious missing word is `:edit`.

The lean hypothesis is deliberately small:

> If `:edit` names the existing terminal lens-updater contract, a clean agent
> will discover the safe structural write path with less help and no new
> mutation semantics.

An MVP must not add arbitrary evaluation, fuzzy selection, multiple edits, or
an auto-apply shortcut. It should reuse the exact query grammar, plan schema,
hashes, diff, replay address, and verified executor already in production. A
read-only pipeline under `:edit` should refuse with a concise remedy to use
`:q`; zero and multiple targets should preserve their current structured
refusals. The apply command must stay visually and temporally separate.

The prototype earns permanence only through behavior:

- blank agents choose it after neutral awareness;
- correct edit plans take fewer source-bearing calls than the observed text
  route;
- agents do not need exact query syntax pasted into the prompt;
- plan review and apply remain separate;
- no benchmark gain comes from skipping verification;
- the name reduces help and recovery detours rather than merely adding another
  alias to memorize.

This is lean startup for an agent tool. The MVP is not a miniature product
roadmap. It is the cheapest truthful interface change that can falsify the
adoption hypothesis. The clean Codex transcript is the customer interview;
exact file bytes and receipts are the retention metric.

## Prototype checkpoint

The implementation remained as small as the hypothesis. `:edit` is a primary
write-category command with three required arguments: `:file`, the existing
lens `:query`, and `:plan-out`. It adds no query step and no plan field.
Successful node and span requests return the same plans as `:q`.

The façade adds four guards that a query-oriented entrypoint did not promise:

- a getter-only pipeline refuses and points reads back to `:q`;
- unknown consent-shaped flags such as `:force`, `:yes`, or `:apply` refuse
  instead of being ignored;
- the plan path may not canonically resolve to the source path;
- plan artifacts use the same fail-closed atomic writer as source application,
  so a failed plan cannot truncate the previous review artifact.

The focused prototype suite passes 6 tests and 51 assertions. The existing help
and lens suites also pass: 70 tests and 657 assertions. clj-kondo reports no
errors in the changed production and test namespaces. These facts establish
the safety contract, not product value. The name earns permanence only if the
neutral-awareness Codex study changes voluntary behavior.

## First voluntary-choice result: the noun did not win

The first valid study ran four independent repetitions in each stable/prototype
× hidden/aware cell. The only prompt difference in the aware condition was:
“The clj-surgeon CLI is installed and available.” No skill or operation name
was supplied.

| Context | Stable exact | Prototype exact | Stable median calls | Prototype median calls |
|---|---:|---:|---:|---:|
| Outcome only | 1/4 | 0/4 | 5 shell + 1 file change | 3 shell + 1 file change |
| Neutral awareness | 4/4 | 4/4 | 6 shell | 7 shell |

Awareness, not the new command, produced the large correctness gain. Seven of
eight hidden-tool agents used text reads plus a file-change patch and changed
an unrelated byte: the final blank line disappeared. All eight aware agents
used guarded structural plans and preserved every byte.

The prototype did not meet its adoption gate:

- `:edit` appeared in only 1 of 4 aware prototype runs;
- it was the first source-bearing operation in 0 of 4;
- that agent first used `rg`, then `:q`, then `:edit`;
- the other agents chose `:replace-subform` twice and `:q` once;
- prototype medians rose from 6 to 7 shell calls, 4.6 KB to 8.3 KB of output
  from source-bearing commands, 39.2 s to 46.0 s wall time, and 109.2k to
  111.6k cumulative input.

This is a clean negative result for “add the obvious noun.” The stable global
help already exposed two credible editing routes. Adding a third fragmented
choice and made the help scan larger. The lone `:edit` user proved that the
facade works safely, but not that it is preferred.

One fair iteration remains before removal. The global quick start taught `:q`
but did not show `:edit`, so agents who scanned only the orientation page could
not copy the new one-shot spelling. The v2 experiment adds exactly one global
quick-start example and one general rule: when a named form plus exact sibling
anchor and replacement are already supplied, the edit plan may be the first
source-bearing call. It does not alter the task prompt, skill, query grammar, or
planner. Four new neutral-awareness sessions will test whether this small
onboarding correction changes voluntary choice. If it does not, remove the
facade rather than adding more persuasion.

## v2: help taught adoption, but one action too late

Four new neutral-awareness sessions compared the first prototype with only the
global-help correction. Exactness remained 4/4 in both versions. The help
example moved eventual `:edit` use from 1/4 to 4/4, proving that a single copied
route was sufficient syntax teaching.

It did not make the route one-shot:

- first-source `:edit` remained 0/4;
- every v2 agent ran `rg` in the same first command as `clj-surgeon --help`;
- every agent then opened `edit --help` before planning;
- median source-bearing commands did not fall;
- median output from source-bearing commands increased from 4.7 KB to 13.2 KB;
- one agent spent 12 shell calls, 255k cumulative input tokens, and 106 seconds
  after trying to reconstruct Git-based verification in a neutral workspace.

The apparent paradox is temporal. Global help cannot teach the agent until the
agent chooses to call it. Clean agents hedged that latency by launching their
familiar `rg` inspection concurrently. A better global example cannot travel
backward in the event stream.

The next legitimate coaxing level is the installed skill. It is visible before
the first action and is the repository's normal onboarding surface. The v3
skill makes one distinction: `:q` reads; `:edit` plans when the supplied
relationship and replacement are already exact. If that does not make the
plan the first source-bearing command and remove help/text detours, the facade
has no remaining evidence-based iteration.

## v3: the skill made `:edit` real

The version-matched skill changed the result. Across four repetitions of three
ambiguous edit shapes:

| Shape | Old skill median calls | Edit-first median calls | Old median input | Edit-first median input | First-source `:edit` |
|---|---:|---:|---:|---:|---:|
| `case` peer result | 3 | 3 | 64.2k | 64.3k | 4/4 |
| outer `cond` result | 6 | 5 | 110.7k | 102.3k | 3/4 |
| `let` initializer | 4 | 3 | 73.6k | 65.1k | 4/4 |

Both versions were exact in 12 of 12 runs. The new skill selected `:edit` in
12 of 12. It made `:edit` the first source-bearing command in 11 of 12 and
removed one median command in the two harder shapes. The case task was already
at the floor, so its execution cost stayed flat.

This quantifies coaxing:

| Onboarding | Eventual `:edit` | First-source `:edit` |
|---|---:|---:|
| Executable hidden | 0/4 | 0/4 |
| CLI named, v1 help | 1/4 | 0/4 |
| CLI named, global edit example | 4/4 | 0/4 |
| Installed edit-first skill, three shapes | 12/12 | 11/12 |

The skill is part of the product. CLI help can teach syntax after discovery;
only pre-action repository onboarding consistently changes the first action.

One run remains a real failure despite its exact final bytes. The agent created
a correct `:edit` plan, then used `apply_patch` to replay the diff, deleted the
plan, and performed Ruby/hash/query verification. The skill said to apply the
plan separately, but did not state the prohibition beside the first edit
example. That field failure now requires an immediate rule: after an exact
plan, the only source-changing command is `:replace-subform!`; never reproduce
the plan with a text patch.

The deeper opportunity is now clearer. `:edit` is a successful discoverability
facade when the skill uploads it, but it still embeds an EDN vector inside shell
syntax and exposes plan-file ceremony. The next hill is the Clojure-native
expression in `docs/plans/clojure-native-edit-algebra.md`: ordinary pure
Clojure combinators that compile to the same guarded planner. The kernel stays;
the authoring surface competes against the measured 3/5-command baseline.

## Native algebra: the photograph becomes executable

The Stage A prototype makes the proposed expression ordinary Clojure. `form`,
`match`, `where`, navigation functions, spans, and replacement functions build
the byte-for-byte existing query vector. They do not parse, match, evaluate,
read, write, plan, or apply anything.

The pure contract passes 8 tests and 192 assertions, including exhaustive
public-builder checks on valid, invalid, and terminal paths. This is an
important but narrow win: the pleasant expression is real and inherits the
existing planner, but a shell caller still needs classpath and namespace setup.
The next probe must price that invocation ceremony honestly. The native syntax
wins only if it reduces agent translation or an end-to-end action, not because
the example looks beautiful in a document.

## SCI: a bridge, and a useful hallucination

Gene noticed that Babashka already runs on SCI. That changes the shell-side
economics. clj-surgeon can interpret one Clojure-shaped query through an
explicit capability allowlist, without asking the caller to set a classpath or
load a namespace. The prototype permits only thread-first composition,
quoting, and the ten pure builders. It has no access to host I/O, namespaces,
evaluation, concurrency, Java classes, interop, or unrestricted SCI mode.

The first blind authoring pair was exact for both surfaces. The native `case`
expression was 88 characters. The equivalent EDN vector was 86. Native syntax
did not earn a compactness win.

The second pair was more revealing. The EDN agent authored the outer `cond`
query exactly. The native agent invented named path arguments plus
`parent-head?` and `outermost-parent?`. Those names are coherent Clojure, but
they are not clj-surgeon. The same latent fluency that makes the syntax feel
natural also creates a larger space of plausible programs.

This is not a reason to abandon the path. It is a reason to make the path
smaller. SCI provides an explicit execution allowlist. Structured errors return
the capability groups, allowed symbols, builder signatures, and a one-sentence
remedy. The next clean-session benchmark must test whether that compact contract
produces first-pass use or merely makes second-pass repair pleasant.

## Give it Clojure

Gene cut through an overdesigned turn in the experiment: “Let it use all of
Clojure. Or rather pure functions. Give it Clojure.” That changed the native
surface from ten renamed query constructors into an actual algebra.

The SCI context now includes broad pure `clojure.core` collection, control, and
higher-order operations. An expression can use `let`, destructuring, `fn`,
`assoc`, `update`, `mapv`, `filterv`, `reduce`, `comp`, `juxt`, predicates,
sequence operations, and the structural builders. It still cannot access I/O,
processes, namespaces, mutable references, classes, or host interop.

This is the important product hypothesis. The value is not parentheses instead
of brackets. The value is that an agent can calculate a selector or replacement
with the same data language already active while editing Clojure.

The real CLI boundary passes. `:edit :expr` and `:edit :query` emit identical
saved plan data for the motivating `case` edit. Planning preserves the source,
and the old `:replace-subform!` executor applies the native plan with its normal
read-back hash and whole-file parse receipt. Unsafe expressions refuse before
the tool reads source or changes an existing plan. The next hill is voluntary
clean-agent use, not more syntax design.

## The first A/B says “not yet”

Twenty-four clean Codex edit runs compared the pre-native checkpoint with the
pure-Clojure CLI. Four replicates each covered a `case` result, an outer `cond`
result, and a `let` initializer. Every run was byte-exact. Clean agents chose
`:expr` in only one of the 12 post runs.

The median `case` and binding runs stayed at three shell calls. The post
`cond` runs regressed from five to six calls, from 101,630 to 125,395 input
tokens, and from 46.96 to 61.67 seconds. The single native attempt took eight
calls. This surface failed its voluntary-use keep gate for literal edits.

That failure identified the missing capability. The native expression could
construct a query from constants, but it could not receive the selected form.
It was Clojure-shaped query syntax, not yet Clojure transforming Clojure. For a
known literal replacement, the EDN vector remained shorter and clearer.

The next MVP adds a terminal `transform`. Its pure function receives the
exactly-one selected form as Clojure data. Planning evaluates it once, then
saves only the concrete replacement, diff, and hashes. Zero or many matches do
not invoke the function. The durable plan remains inert EDN, and the existing
executor remains the only write path. This creates a real one-shot hypothesis:
derive a replacement from source without a preliminary read.

## The algebra closes the loop

The source-derived A/B passed. Eight clean Codex runs compared the query-only
native checkpoint with `transform`, using four replicates of an edit whose new
vector the agent had to calculate from unknown source values. Every run was
byte-exact.

Post agents chose `transform` in four of four runs. Their first source-bearing
call was the edit plan in four of four runs, versus zero of four pre runs.
Median shell calls fell from five to three. Median cumulative input fell from
102,837 to 65,829 tokens, output from 1,201 to 718 tokens, source output from
2,150 to 1,527 bytes, and wall time from 42.36 to 27.67 seconds.

This is the distinction the literal benchmark exposed. Native query
construction was aesthetic equivalence. Source-derived transformation removes
an information-gathering command. The agent can state the rule directly in the
language already active in context, while the kernel still owns selection,
cardinality, planning, hashing, replay, and verification.

Three of four post agents used the ideal three-command sequence: read skill,
plan, apply. One added a plan-path existence probe. The next small hill-climb is
to state that a task-specific plan path needs no preflight: successful planning
atomically replaces it, while any refusal preserves the existing artifact.

The four-run guidance replication revealed a better explanation. Every agent
used the correct first-source transform, but all four made two skill reads. The
skill had grown to 241 lines. The standard first command read lines 1–240, then
the agent fetched the final line. The product fix is not more edit guidance. It
is a 240-line ceiling, now enforced by a permanent test so the complete skill
fits in one standard read.

## Best ever: four perfect three-call edits

The final clean-context replication achieved the target in all four runs. Each
agent used exactly this sequence:

1. read the complete clj-surgeon skill once;
2. plan the source-derived edit with `:edit :expr` and `transform`;
3. apply the reviewed plan with `:replace-subform!`.

All four results were byte-exact. All four agents chose `:expr` voluntarily.
All four made the edit plan their first source-bearing call. All four used
separate plan and apply commands with a verified receipt. None used `:q`,
`:cat`, `rg`, `sed`, a help call, a text patch, or a post-apply reread.

Median shell calls were 3, cumulative input was 65,806 tokens, uncached input
was 14,075 tokens, output was 731 tokens, source output was 1,529 bytes, and
wall time was 28.22 seconds. Against the query-only pre checkpoint, median calls
fell from 5 to 3, cumulative input from 102,837 to 65,806, source output from
2,150 to 1,529 bytes, and wall time from 42.36 to 28.22 seconds.

These are the best measured clj-surgeon results for a source-derived structural
edit. The qualification matters. Literal paths and replacements still favor
shorter EDN. `transform` wins when the new form genuinely depends on the current
form, because it removes the preliminary information-gathering command without
weakening the review boundary.

The final one-call improvement came from treating documentation size as runtime
performance. At 241 lines, the skill required two standard reads. At 240 lines,
it fit in one. The repository now tests that ceiling. The one-shot standard is
therefore executable: the safe route, the skill that teaches it, and the token
boundary that keeps it one-shot all fail together if they drift.
