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

## Overnight orders

The hill climb will not confuse pretty bytes with correct programs. Parse and
intended structural behavior are mandatory. So is lossless preservation of
meaning-bearing material outside the declared change: comments, docstrings,
metadata, reader-discard forms, lint directives, strings, regexes, and unrelated
source. Whitespace-only drift can be recorded as presentation-red without
discarding an otherwise correct semantic result.

Comments deserve conservatism. They are invisible to parsed-form equality and
may contain more than prose: build directives, disabled examples, safety
warnings, provenance, or the reason a strange implementation must remain.
Deleting a named owner with its attached comments is an explicit operation.
Losing or casually rewriting any other comment is a correctness failure unless
the task itself owns that comment and supplies the intended replacement.

The overnight loop follows Kent Beck's economic rule. When the next experiment
is awkward, first make that change cheap: capture the failure, add the smallest
guard or pure seam, commit it green, and immediately rerun the motivating case.
The objective is not a grander editor. It is a lower cost of learning until the
one-shot route is both faster and harder to get wrong than native patching.

See [Selective Compiled Scalpel](../plans/selective-compiled-scalpel.md) for the
executable plan and falsification gates.

## Captain's log: the EDN chord played on the first try

The benchmark correctness-policy migration exposed a tooling tax in miniature:
twelve `capsule.edn` files needed the same guarded root-level replacement, with
one file retaining an additional `:lint true` key. Native patching could batch
the work, but the caller still had to reproduce twelve file hunks. This was the
right Kent Beck side quest because the transaction kernel already knew how to
apply exact replacements across files; only the compact public contract denied
access to that capability.

The new route accepts either one `file` with a named Clojure owner or explicit
`files` with `within.root=true`. Root scope is the only EDN scope. The declared
match count is enforced in every file; duplicate paths, mixed `file`/`files`,
named EDN owners, and stale per-file counts refuse before writing. The existing
transaction supplies frozen-snapshot atomicity, exact source preservation,
parse/read-back verification, hashes, and undo.

The motivating migration then succeeded on its first live use:

| Evidence | Result |
|---|---:|
| Public calls | 1 |
| Declared source shapes | 2 |
| Exact edits | 12 |
| Files | 12 |
| Refusals or retries | 0 |
| Server execution | 110.55 ms |
| Complete local tool round trip | 240 ms |
| Verification | atomic commit + read-back complete |

The economic verdict needs two clocks. The editing interaction paid back
immediately: one guarded chord replaced twelve repeated hunks in under a
quarter second. The implementation took roughly twelve minutes, so this single
migration alone did not repay the entire feature investment. The primitive is
small and reusable, however; a few comparable EDN/config migrations amortize
it, and every future use reduces both mechanics and partial-write risk. That is
the low-cost-of-change ratchet we wanted, not evidence that every repeated text
edit needs a new API.

## Captain's log: one redundant name cost an entire model turn

The corrected dev-b replay isolated a different kind of waste. Its compact arm
guessed the natural shape `within.namespace=true`. The schema required the
namespace's string name, so Surgeon safely refused in 7.33 ms. Without reading
source, the caller repeated nine namespace names and the second call committed
all 51 edits exactly in 1.049 seconds. Complete turn time was 111.061 seconds.

The native arm took 400.498 seconds and ended incorrect. To manufacture one
large patch, the caller built a Clojure transformation program. Top-level `def`
return values from that helper escaped onto stdout and were prepended to every
target file as `#'user/...` forms. The patch engine applied what it was given;
the model-managed compiler pipeline corrupted the result.

This is evidence for two narrow conclusions, not a blanket victory claim:

1. Requiring a caller to restate a namespace name already uniquely present in
   one file is redundant ceremony. `within.namespace=true` now resolves exactly
   one `ns` owner and refuses zero or multiple owners before writing.
2. Surgeon's advantage on this case is hiding a dangerous compiler pipeline,
   not making replacement intrinsically faster. A stronger native cohort must
   remain free to find a safer batched patch route; the observed failure stays
   in the denominator.

## Captain's log: the redundant namespace name disappeared

The next frozen cohort tested the smallest ergonomic repair suggested by the
dev-b refusal. The compact prompt named the complete 51-edit decision but used
`within.namespace=true`; the caller no longer had to repeat nine namespace
names already present uniquely in their files.

The first terminal pair, on dev-c with fresh Sol/high callers, was decisive:

| Route | Correct | Exact presentation | Complete wall | Tool actions | Failed mutations |
|---|---:|---:|---:|---:|---:|
| compact `edit_clojure` | yes | yes | 60.243 s | 1 | 0 |
| native control | yes | yes | 220.772 s | 23 | 0 |

Compact was 3.67x faster. Its one MCP call atomically committed all 51 edits
across nine files. The retained telemetry proves all nine namespace-scoped
edits used `within.namespace=true`; the transaction completed without a
refusal, retry, shell command, source reread, or post-decision inspection.
Native remained a real competitor: it also reached the exact answer, but used
22 shell reads followed by one file mutation.

This is stronger evidence than a fast server timer. The product change removed
one piece of redundant caller ceremony, and a fresh model then played the
complete church-organ chord on the first attempt. The complete-turn advantage
came from collapsing mechanics and source reproduction, not from weakening the
oracle: exact bytes, parse, and meaning-preservation all remained gates.

The other frozen seats remain evidence even if they are ugly. dev-a's compact
arm was exact in 69.155 seconds, while its native arm was rejected by the route
guard after 306.824 seconds because it invoked Clojure 14 times. dev-b was still
running when this checkpoint was written. Neither arm was repaired, rerun,
excluded, or silently promoted into the efficiency denominator.
