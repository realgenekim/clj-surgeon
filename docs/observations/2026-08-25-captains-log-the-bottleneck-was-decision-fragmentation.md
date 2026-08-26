# Captain's Log: the Bottleneck Was Decision Fragmentation

The surprising number was not 4.61x. It was less than one percent.

Across 28 recent Codex turns that used clj-surgeon, the median turn lasted 9.08
minutes, used 11 actions, and crossed six route phases. Yet Surgeon itself
usually occupied less than one percent of complete wall time. Its median direct
action was 427 milliseconds, faster than the observed 628-millisecond native
`apply_patch` action.

We had been staring at the wrong clock.

Production agents used Surgeon for 230 structural-read phases but only 16
structural-apply phases. Nine Surgeon-using turns eventually patched natively.
The common shape was not a church-organ chord. It was a hesitant walk across
two editors:

```text
inspect -> inspect -> native read -> decide -> native patch -> verify
```

No parser optimization can rescue that workflow. The model pays to learn two
instruments, moves partial state between them, and often uses Surgeon only to
prepare a native patch. The direct tool is fast; the decision is fragmented.

The controlled counterfactuals show the alternative. Six small replacements
across two files produced only a 1.05x compact advantage because both routes
fit comfortably in one model turn. A production-derived 22-owner extraction
cleanup produced a 4.61x compact advantage because the model could name owners
instead of reading and reproducing hundreds of deleted lines.

Tonight we found the next serious test. It is derived from a real nine-file
submission-row extraction cleanup: 51 edits, 14 obsolete owners, namespace
migrations, dispersed source-and-test callers, and 429 removed lines. The first
fully correct Anvil pair landed at 59.879 seconds compact versus 269.173 seconds
native: 4.50x. Compact played one chord. Native used five actions and its first
patch was safely refused.

The experiment also failed in exactly the way a useful experiment should. A
second compact caller performed one successful atomic 51-edit transaction in
67.635 seconds, but missed the exact-byte oracle in nine namespace forms.
Initial suspicion fell on Surgeon formatting. The forensic evidence reversed
that conclusion: no formatter ran, every parsed form was equal, and the compact
editor preserved the literal bytes it had been given.

The benchmark prompt was wrong. Its “exact” Clojure fragments lived inside a
numbered Markdown list. Three presentation spaces plus three source spaces
appeared to the model as six literal spaces, while the accepted fixture
contained three. One caller faithfully applied six; another inferred the
Markdown convention and applied three. The tool was deterministic. The prompt
was not.

The retained three-seat cohort made the lesson sharper:

| Seat | Compact result | Native result | Compact wall | Native wall |
|---|---|---|---:|---:|
| dev-a | exact-byte false | exact | 67.635 s | 135.080 s |
| dev-b | exact-byte false | exact-byte false | 65.066 s | 439.382 s |
| dev-c | exact | exact | 59.879 s | 269.173 s |

Every compact caller made all 51 edits in one successful MCP transaction with
no refusal. The two red compact arms copied the ambiguous six-space prompt;
their parsed forms were correct. The red native arm retained 14 blank lines
after deleting owners. Both native failures that were observed began with a
safe failed patch attempt. Nothing was repaired, retried, excluded, or
reclassified.

That matters because the path to beating native is not permission to weaken
correctness. It is to make the compiled route easier to state correctly than a
textual patch:

```text
small visible change        native
unknown surface             discover, then choose once
complete mechanical change  one guarded edit_clojure transaction
```

The new architecture is a selective compiled scalpel. `edit_clojure` is not a
replacement for every editor and `inspect_clojure` is not a reason to avoid
`rg`. Surgeon earns the call when the decision is complete and its structural
address space lets the model avoid source reproduction. Everywhere else,
native tools retain home-field advantage.

The next gate is unusually crisp. Correct the ambiguous fixture without
overwriting its retained failures. Add a verifier that prevents indented
literal Clojure fences from entering another capsule. Commit a new benchmark
version. Then demand three out of three exact, one-shot compact successes and a
median paired advantage of at least 2x on the same historical case.

If that passes, we have not built a better text editor. We have found a better
instruction boundary: decide once, compile once, play the chord, and move on.

See [Selective Compiled Scalpel](../plans/selective-compiled-scalpel.md) for the
executable plan and falsification gates.
