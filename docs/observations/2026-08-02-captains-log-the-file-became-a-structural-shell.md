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
