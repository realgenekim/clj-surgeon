# Captain's Log: the file became a structural shell

The motivating failure was almost comically small. An agent working in a large
Clojure namespace knew the function it needed, but clj-surgeon could only show
the namespace outline or search inside an already named form. To read one
complete top-level definition, the agent guessed two commands in sequence and
still had to reconstruct a textual line range. The structural tool could locate
the object but could not hand the object back.

That gap became `:show-form`:

```bash
clj-surgeon :op :show-form :file state.clj :form transition!
clj-surgeon :op :show-form :file state.clj :line 1134
```

The operation returns the exact parsed form, its location and platforms, and a
hash of the complete file snapshot. It accepts exactly one selector. Missing,
invalid, absent, or ambiguous selectors fail closed as structured EDN. It does
not guess what the caller meant, choose the first duplicate, infer an edit, or
silently widen scope. This is the Bitter Lesson boundary in miniature: give the
agent better mechanical perception and leave judgment with the agent.

The satisfying part was using clj-surgeon to build clj-surgeon. `:ls` first
mapped the 800-line CLI namespace. `:find-subform` located small implementation
targets. Hash-bound `:replace-subform` plans were reviewed and applied directly.
Two proposed plans were rejected and regenerated when review showed that they
would damage comments. The new `:show-form` then read `format-op-help` by name
and by an interior line; both paths returned the same complete source and file
hash.

The clean-context experiments were more valuable than the happy-path tests.
Before the feature, a fresh Codex session explored global help, the outline,
dependency help, and nested-search help without finding a one-shot form reader.
After the first implementation, fresh agents found `:show-form` and avoided
`sed`, but both ran a redundant `:ls` first. The culprit was not the command. It
was our own skill text: “Run `:ls` before reading a large Clojure file”
contradicted the new one-shot route.

The skill, README, CLI help, legacy skill, and repository agent instructions
now state the sharper rule: when the form name or containing line is unknown,
start with `:ls`; when either is already known, make `:show-form` the first
source inspection and do not run `:ls` solely as a preflight. Permanent
anti-drift assertions protect that wording across the agent-facing surfaces.

Fresh ephemeral Codex sessions then performed each named-form and line-form
task with exactly one source command. Neither session used `:ls`, `sed`, `awk`,
`head`, `tail`, or a reconstructed range on the Clojure file. A third session
started with the historically guessed `:get`; the nonzero structured error
supplied an executable `:show-form` remedy, which succeeded as the second CLI
command without consulting help. Another clean-context review confirmed that
an already reviewed replacement plan leads directly to one
`:replace-subform! :plan` command, not an edited plan or `apply_patch`.

The feature tests are deliberately disproportionate to the implementation.
They cover pure selection by symbol, string, form boundary, interior line,
ending line, and attached comment; namespace and comment forms; every invalid
selector; absent and duplicate forms; CLJC reader conditionals and platform
disambiguation; project-defined form aliases; invalid source; unreadable paths;
shell-significant names; executable remedies; CLI exits; the historical wrong
commands; a sanitized real-program-derived migration fixture; and agreement
among help, README, changelog, and both skills. The focused help, show-form, and
structural-lens suite currently has 84 tests and 662 assertions. The full suite
passes with 330 tests and 1,620 assertions.

The final adversarial review was a useful warning against declaring victory
from those counts alone. It found that global integer coercion could break
unrelated numeric strings, missing-file refusals lost canonical context,
invalid guessed selectors still received doomed remedies, ambiguity evidence
was not actually bounded, the advertised pure core consulted mutable alias
state, `/` was rejected as a legal unqualified name, and one boundary test had
no distinct interior line. Every critique became a fix and a permanent
regression. Line coercion is now local to the form reader; alias data crosses
the I/O boundary as immutable input; remedies must be executable; candidate
evidence caps at ten while retaining total count; and the CLI refusal matrix,
`.cljs` platform case, and true start/interior/end cases are explicit. No test
was weakened.

The API now wants to become a small structural shell:

```text
ls -> cat -> find/deps/users -> plan -> apply -> verify
```

`ls` inventories the namespace. `cat` is the memorable proposed alias for the
canonical `show-form`: return one explicitly selected structural object, never
dump the whole file. `find-subform` addresses inside it. `deps` and a future
resolved `users` operation expose the graph. Every mutation should converge on
one reviewable, hash-bound plan and one guarded application protocol.

The experiment changed the standard for “one-shot.” It is not enough for the
command to exist, for the tests to be green, or for the README to contain an
example. A capability is one-shot only when a clean agent chooses the narrow
route without detours. When it does not, the transcript becomes a regression
fixture and the instructions are repaired. The world-class part is not that we
predicted every source of confusion. It is that confusion now leaves evidence
and permanently improves the paved road.

The next experiment began immediately: `:cat` was added and tested as a strict
alias while `:show-form` remained the canonical machine operation. Selected
`:cat` calls return the same source and hash; bare `:cat` refuses rather than
introducing whole-file dump behavior. A fresh agent asked to treat clj-surgeon
as a structural shell selected `:cat :form` as its only Clojure-source command,
skipped `:ls`, and recognized `:show-form` as the canonical operation. The Unix
vocabulary survived contact with a clean context without weakening the machine
contract.

## Log entry: the clean agent found the next sharp edges

The error-message experiment settled a useful question. Dumping global help on
every malformed call would be hostile to an agent's context window. Bare
`:cat` and the historically guessed `:get` both recovered in exactly two CLI
calls from concise local EDN, without calling `--help`. The right refusal is not
the biggest manual; it is the smallest sufficient contract plus an executable
remedy when the correction is high confidence.

A semantic lookup then used `:ls` to turn one distinctive phrase into a form
name. It worked, but printed a 40-form inventory. The route is now explicit:
`rg -n` for the one lexical coordinate, then `:show-form :line` for the exact
structural object. Text search is the index; clj-surgeon is the lens.

The next clean agent asked for a structural pattern without knowing its parent.
It opened help because our prose implied `:inside` was mandatory, then
discovered that file-wide `:find-subform` had existed all along. We had made the
agent independently guess the same capability twice: once to wonder whether
file-wide search existed and again to infer that omitting a documented-looking
argument might work. `:grep-form` is now the strict shell alias, with file-wide
search as the first example and `:inside` documented as optional narrowing.

Finally, a small `case` edit exposed the boundary of subtree operations. The
agent searched for `(:finish (assoc ...))`, but a `case` clause is two sibling
forms, not a list. It recovered by selecting the contained `assoc`, generated a
correct plan, and applied it—but chained plan, apply, and verification in one
shell command. That defeats review even if the plan is perfect. The help and
skills now state that plan generation is a standalone command whose result must
be observed before a separate apply. The general missing primitive is a
sibling-span lens for `case`, `cond`, bindings, and map entries, recorded as
[issue #21](https://github.com/realgenekim/clj-surgeon/issues/21). It is a
general structural operation, not a growing catalog of hand-coded special
forms.

The file-wide retest then found a deeper version of the same truth. A plain
list in `move.clj` was found in one `:grep-form` call, but an apparent call
inside `#(...)` was not: rewrite-clj represents that anonymous-function body as
sibling nodes rather than a standalone list node. Issue #21 now includes this
self-hosting acceptance case. The tool should expose the representation it
actually has, then give the agent a general span lens over it.

## Log entry: semantic text became an address, then the edit loop closed

The next benchmark asked the sharper question: if an agent reaches for `rg`,
what structural fact is still missing? A distinctive phrase inside one file was
being used only to manufacture a line number for `:show-form`. Literal
`:contains` selection removed that bridge. In a fresh matched-skill session,
the old route was skill → `rg -n` → `:show-form :line`; the new route was skill
→ `:cat :contains`. Both returned exact source. Calls fell from three to two,
cumulative input from 62,032 to 43,554 tokens, and wall time from 28.1 to 24.1
seconds.

The first clean edit run was more useful because it was not impressive. The
agent used `:grep-form` to find two identical expressions, then needed `:cat`
to recover the `:finish` sibling context. After applying the plan, it attempted
`git diff`, probed `.git`, and reread the unrelated form. The new receipt had
removed one old verification call, but the complete route still took eight
calls and more tokens than the adjacent old-tool run. Green unit tests had not
made the workflow one-shot.

That transcript produced three product changes:

- CLI `:contains` values now remain literal text, so keyword-shaped clues such
  as `:finish` work directly instead of being coerced to EDN keywords.
- The skill and help route sibling labels, guards, keys, and binding names to
  `:cat :contains`; this returns the owner and surrounding form in one read.
- A reviewed one-edit plan plus the verified apply receipt is explicitly the
  stopping condition for exact-edit verification. Agents do not reread source,
  repeat hashes, or probe for a Git worktree merely to reproduce that evidence.

The next blank session fell to five calls; the only remaining detour was a
failed `git diff`. After the stopping condition was made explicit, two fresh
post sessions independently chose the four-call ideal: read the installed
skill, `:cat :contains :finish`, generate and review one scoped plan, then apply
that unchanged plan. Neither used `rg`, `sed`, `:ls`, help, Git probing, or a
post-apply reread.

The final adjacent replication remained byte-exact and kept plan and apply in
separate commands. Old versus new was 10 → 4 shell calls, 157,481 → 77,421
cumulative input tokens, 24,617 → 14,189 uncached input tokens, 9,782 → 1,596
source-output bytes, and 67.4 → 49.3 seconds. The apply emitted the complete
586-byte receipt with matching result/read-back hashes, one applied edit,
atomic-write evidence, and whole-file parse success.

One prior four-call run made the exact edit but Codex's JSON event recorded an
empty apply output. The strict benchmark scored `:verified false`; it did not
infer a receipt from exit status, final bytes, or the agent's prose. A direct
relative-plan reproduction emitted the full receipt, and the next clean run
captured it normally. This appears to be a transient event-capture anomaly, but
preserving the failed score matters more than polishing the result.

The mental model survived contact with the evidence, with one qualification.
clj-surgeon is becoming the `ed`/REPL structural microkernel for Clojure: a
perfect lens for naming and returning parsed objects, plus deterministic,
guarded transformations. “Every task in one command” would erase review and
consent. “One command per honest judgment boundary” is the stronger ideal:
one structural discovery, one inspectable plan, one verified apply. Clean-agent
transcripts are now acceptance tests for whether those boundaries are obvious
and minimal.

Raw result directories remain on the benchmark host at
`/tmp/clj-surgeon-one-shot-20260803-v1` through `v4`. The final replication's
`runs.tsv` SHA-256 is
`8e8ce927c057fd96065cc4c1f8135e052cb6916617e64af30c951b84b0b30159`.

## Log entry: the path became both getter and updater

The sibling-span question led to a more general answer than a span command.
jq's durable idea is not its punctuation; it is that a filter is both a getter
and an updater. Clojure gives us an even better substrate for an agent-facing
version: an EDN pipeline over the lossless concrete syntax tree. The first
algebra is deliberately small—locate a named form, find a subtree, filter by
node or parent tag, then navigate left, right, up, or down. A navigation-only
pipeline reads. The same pipeline ending in `[:replace FORM]` emits the existing
single-edit, hash-bound plan. It never writes source.

The self-hosting probe was the delightful proof. One command located the exact
set inside clj-surgeon's own `parse-args` that keeps selected CLI values raw:

```bash
clj-surgeon :op :q :file src/clj_surgeon/core.clj \
  :query '[[:form parse-args] [:find #{:match :with :contains :query}]]'
```

The result included the exact source, complete semantic path through nested
`let`, `cond->`, `map`, `fn`, binding, and `if` forms, stable address, line,
owner, source hash, and a two-stage cardinality trace. clj-surgeon had become
capable of explaining its own CLI parser without a text-to-line-number bridge.

The first blank-context comparison gave us the next hill rather than a victory
lap. Before the skill taught the algebra, a clean agent needed seven calls to
find that same raw-argument set: outline, wrong form, help, wrong scoped search,
owner read, then final search. A second clean agent made a peer edit perfectly
with the prior paved road, but still needed `cat → plan → apply`. The new updater
can make the exact task `query-plan → apply`: two structural calls, with the
review boundary fully intact because the first call is non-mutating and contains
the selected source, trace, diff, and hashes.

This corrects our slogan. “Discovery, plan, apply” is right when discovery
changes the decision. When the user's target and replacement are already exact,
forcing a separate discovery call is theater. The stronger standard remains
one command per honest judgment boundary: read when judgment needs evidence,
always review a non-mutating plan, then apply separately. The clean-context
replication after the skill update will decide whether the jq-shaped model is
obvious enough to become the paved road.

## Log entry: jq slices resolved the missing peer object

The first algebra could move from a `case` key to its right-hand value and edit
that node safely, but issue #21 asked a stricter question: can the pair itself
be addressed, including the flattened body of `#(...)` where no call-list node
exists? jq supplied the next clue again. Paths are not limited to scalar nodes;
arrays have slices.

The concrete-syntax analogue is `[:span N]`: promote each selected semantic
node into that node plus its next `N-1` siblings without crossing the parent.
The updater is `[:replace-span FORM ...]`. V2 deliberately requires equal
arity, which gives comments a wonderfully boring ownership rule: leading and
trailing trivia remain outside the slice, and every gap between corresponding
forms survives byte-for-byte. No insertion, deletion, fuzzy window, inferred
branch, or bulk write entered the kernel.

The clean-context result reached the physical optimum. Given a `:finish` key,
an internal comment, its result expression, and an unrelated identical result,
the agent independently chose exactly two structural calls: one non-writing
span plan and one separate verified apply. It used no preliminary read, `rg`,
`sed`, outline, help, Git probe, or reread. The plan selected the pair once; the
comment and unrelated peer survived. Its sole critique was that the generic
`:replace-subform!` receipt did not repeat `:replace-span`; that observation
became the `:planned-operation` receipt field.

This is the strongest version of the editor metaphor so far. `cat` is a named
structural read, `q` is the composable path language, a span is a slice, a plan
is the immutable edit buffer, and apply is explicit consent fenced by hashes.
It feels Unix-native because the commands are small and pipe-shaped, jq-native
because paths are also updaters, and Clojure-native because no syntax needs to
pretend it is text or a wrapper form that does not exist.
