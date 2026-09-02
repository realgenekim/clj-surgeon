---
parent: high-level-design
prefix: MCP-OP
---

 #Extraction Rewiring and Non-Fatal Outlines

# #Context

Cohort rf1 ran the same real extraction six times against clj-surgeon's own
source: move a nine-form exact-verification cluster out of
`clj_surgeon/mcp_change_buffer.clj` into a new namespace and repoint every
caller. Receipts: `docs/observations/2026-09-02-captains-log-the-big-aha-and-reset.md`
and the ethnography `rf1-ethno.md`.

`:extract!` cut the cluster correctly in one call in every structural run. Every
structural run then spent four to fifteen further returns repairing what the cut
left behind, and all four finished with a native patch. The repair was always
the same five things, and none of them is a judgment call:

1. the source namespace docstring was copied verbatim into the target;
2. imports were copied wholesale while requires were pruned;
3. the source require was emitted as `[target :as alias :refer [...]]`;
4. private forms a remaining owner must call stayed `defn-`;
5. the source namespace kept requires and imports whose only use had just left,
   and every located caller site was reported and left untouched.

The tool already proves everything the repair needs. `:remaining-source-callers`
names each remaining owner and the moved Vars it calls. `:callers-to-review`
names every other file that references a moved name.
`:omitted-target-requires` proves which requires the target does not need. The
gap is not knowledge; it is that the operation declines to apply what it has
proved. `:extract!` cuts and does not sew.

# #Design

Extraction becomes one complete operation over a proved file set, not a cut
plus a report. The planner already computes the caller inventory purely; the
rewiring plan is compiled in the same pure pass, previewed by `:extract`, and
applied by `:extract!` under the existing failure-atomic write and read-back
contract, extended from two files to the complete proved set.

Two ordering rules make the result predictable and reviewable. The target emits
its forms in the caller's declared `:forms` order rather than an internal
topological sort, because the caller states the intended reading order and a
re-sorted file is a diff the caller did not ask for. The target header carries
no docstring unless the caller supplies one, because a copied docstring
describes the namespace it came from.

Header minimization becomes symmetric: imports are pruned to the classes the
moved forms reference exactly as requires already are, and the source loses each
require and import whose last reference left with the moved forms. Both
directions fail closed on a shape they cannot prove.

Alias policy is explicit. The source and every caller receive
`[<target-ns> :as <alias>]` and never `:refer`, because a refer list is a second
place to maintain and the rewiring already qualifies every site.

# #Unknown Arguments

Two of two rf1 runs passed `:public-forms` to the CLI, which is not an argument
of `:extract!`. It was accepted, ignored, and reported as success; the forms
stayed private and the agent discovered it only from a later `git diff`. A
success receipt for work that did not happen is worse than a refusal, because it
terminates investigation. Unknown arguments become a typed refusal that lists
the accepted keys, and the capability those calls wanted ships as `:public`.

# #Non-Fatal Forward-Reference Analysis

`:ls` runs clj-kondo to report forward references and treated any non-zero
clj-kondo exit as an analysis failure. clj-kondo exits non-zero when it has
FINDINGS, not when it has failed; and its findings go to stdout while the
diagnostic was read from stderr. A file with one lint warning therefore refused
its whole outline with an empty diagnostic.

Two corrections apply. The analysis stage judges the analysis by whether it
produced a parseable payload, not by the linter's finding-count exit code. And
an outline that parses is never failed by a stage that only decorates it: a
failure of forward-reference analysis returns the outline with
`:forward-refs :unavailable` and one note. Structure the caller can already see
must not be withheld because an optional decoration was unavailable.

# #The Workspace Declares Its Compile Classpath

The post-apply compile check resolves the project classpath with
`clojure -Spath`. On a project whose namespaces need a test-only dependency
that is bare classpath is not the project's classpath, and the check fails for
a reason the extraction did not cause.

That failure is typed honestly as `:ok :unverified`, so the verb does not tell
a reader to revert correct work. But the receipt also prints the command it
ran, and a printed command that is red on a correct move is worse than no
command: an agent told to run it will try to repair, and land bytes after an
extraction that was already right.

A workspace knows which alias assembles its classpath and the tool cannot.
So the workspace says so, in the `.clj-surgeon.edn` it already owns:

```clojure
{:compile {:aliases ["clj-surgeon/mcp-test"]}}
```

Both the check and the printed command then use
`clojure -Spath -A:<aliases joined by colons>`. The alias reader ignores
top-level keys it does not recognise, so this section is additive and cannot
affect form classification.

When nothing is declared the check still runs with a bare `-Spath`. If that
comes back `:classpath-incomplete`, the receipt does not stop at "unverified":
it publishes `candidate-aliases`, the `deps.edn` aliases that add a `test`
path, sorted, and reprints its command with the first of them applied and
marked `guessed`. The point is not that the guess is right — it is that the
agent's next call is determinate rather than something it has to invent, and
the receipt names declaring the alias as the durable fix.

This repository ships its own `.clj-surgeon.edn` declaring
`clj-surgeon/mcp-test`. It is the repository's configuration, not test
apparatus: any agent extracting inside this tree needs it.
