---
parent: high-level-design
prefix: MCP-OP
---

 #Admit Clojure Patch

# #Context

Every measured cohort this month shows the same route. An agent reads the
source it needs, composes a unified diff in its own head, and hands that diff
to its native `apply_patch`. It then pays three separate returns to find out
whether the write was right: it re-reads the file, it runs `git diff`, and it
runs a focused test command. Structural tooling offered *before* that write has
to be paid for with a second design pass, and the cohorts decline to pay it.

`admit_clojure_patch` does not ask for the route to change. It accepts the
diff the agent already wrote, at the moment the agent would otherwise spend
three returns, and replaces all three with one receipt. Everything it can say
beyond those three returns is a property that only a reader of forms can state:
which top-level owners actually changed, which bytes moved without any
structural reason, which protected nodes were destroyed, and which hazards a
line-oriented patcher is structurally unable to see.

This leaf owns the gate's public contract, its structural report, its hazard
vocabulary, and its verification honesty. It does not own patch authorship,
selector semantics, the SCI evaluation fence, or path confinement; those remain
where they are.

# #Public Contract

## #Request

```json
{
  "workspace_root": "/abs/path",
  "patch": "--- a/src/app/core.clj\n+++ b/src/app/core.clj\n@@ -10,3 +10,3 @@\n ...",
  "mode": "preview",
  "verify": "focused"
}
```

| Field | Required | Default | Meaning |
|---|---|---|---|
| `workspace_root` | no | configured root | Canonical absolute workspace, resolved by the shared router |
| `patch` | yes | — | The exact payload the caller would give `apply_patch`, in either the V4A or the unified-diff grammar |
| `mode` | no | `preview` | `preview` never writes; `commit` writes when no refusal-class hazard exists |
| `verify` | no | `focused` | `focused` runs the lint delta and the mapped focused tests; `none` runs neither |
| `expect_pre_sha256` | no | — | Per-file pre-image digests, copied from a preview's `next_call`; binds this commit to the bytes that preview inspected |

There is nothing else. No owners, no selectors, no counts, no expectations.
Every fact the gate needs beyond the patch it reads from the workspace itself;
the one optional field it accepts, `expect_pre_sha256`, is a value the gate
itself produced in the previous call. That is the whole adoption argument, and it is deliberately the whole
request schema.

## #Receipt

```clojure
{:ok true
 :operation :admit-patch-preview          ; or :admit-patch!
 :mode "preview"                          ; echoed request mode
 :committed false
 :files [{:file "src/app/core.clj"
          :kind "clojure"                 ; clojure | data | passthrough
          :hunks 2
          :hunk_line_spans {:pre [[10 12] [40 41]] :post [[10 13] [41 42]]}
          :owners {:added [] :removed [] :changed ["handle-tick"]}
          :protected_node_drift {}
          :byte_drift_outside_hunks 0
          :pre_sha256 "…" :post_sha256 "…"}]
 :owners {:added [] :removed [] :changed ["src/app/core.clj::handle-tick"]}
 :protected_node_drift {}
 :byte_drift_outside_hunks 0
 :hazards []
 :lint_delta {:ran true :ok true :introduced-count 0 :removed-count 0
              :blocking-introduced []}
 :tests {:ran true :passed 12 :failed 0 :skipped 0
         :namespaces ["app.core-test"]}
 :hashes {"src/app/core.clj" {:pre "…" :post "…"}}
 :pre_image_binding "bound"        ; "unbound", or "created" when nothing existed
 :lock_scope :cross-process        ; or :process on commit, :none on preview
 :lock_path "/abs/path/.clj-surgeon/write.lock"
 :verification_status :complete    ; or :partial, :unverified
 :verification_reasons []
 :elapsed_ms 812.44
 :verification_complete true
 :source-unchanged true
 :next_call {:tool "admit_clojure_patch"
             :arguments {:workspace_root "/abs/path" :mode "commit"
                         :verify "focused"
                         :expect_pre_sha256 {"src/app/core.clj" "…"}}
             :patch_field "patch"
             :patch_sha256 "…"
             :note "resend the same patch text in the patch field; …"}}
```

A refusal carries the same closed key set plus `:error-type`, `:error`, and
`:ok false`. A refusal payload is never empty and never omits the receipt; a
caller must be able to act on the refusal without asking a second question.

`next_call` is always executable, and it never carries the patch. Echoing the
payload would let a refusal grow without bound with the very input that caused
it, and the caller already holds the text; the follow-up therefore names
`patch_field` and `patch_sha256`, which bind it to the same patch without
restating it. In preview with no refusal-class hazard the follow-up is the
identical request with `"mode": "commit"` plus `expect_pre_sha256`. In any
refusal it is the identical request with `"mode": "preview"` plus a
`blocked_by` field naming what stopped it. After a successful commit it is
`nil`.

Every published receipt is fitted to `mcp-write-refusal/public-byte-budget`,
the same 32640-byte budget `bound-public-refusal` enforces for write refusals.
The budget lives in one place and is shared, not restated; when a receipt
cannot fit, its `hazards` and `files` collections are trimmed longest-first and
`payload_truncated` plus a per-key omitted count record exactly what was cut,
so a trimmed list can never be mistaken for a complete one.

# #Design

## #Component boundary

```text
mcp_admit_tool.clj      effects: routing, snapshot read, commit, verify, telemetry
   |
   +-- patch_apply.clj       pure: unified diff -> post images + hunk spans
   +-- form_identity.clj     pure: pre/post images + spans -> delta + hazards
   +-- intent_transaction/commit-compiled!    existing atomic CAS + read-back
   +-- diagnostic_delta/diagnostic-delta      existing location-independent lint delta
   +-- mcp_paths/resolve-source-path          existing confinement, unchanged
```

`patch_apply.clj` and `form_identity.clj` are pure: they take strings and
return data. They are testable entirely from source literals, with no temporary
files, which is the repository's stated standard for pure logic. All filesystem
and process effects live in `mcp_admit_tool.clj`.

## #Patch application

Two grammars are accepted, selected by the first non-blank line of the
payload: `*** Begin Patch` opens the `apply_patch` V4A grammar, and
`diff --git`, `--- `, or `Index: ` opens unified diff. Both are described
below, and everything downstream of application -- hunk spans, drift,
protected nodes, the pre-image binding -- is computed identically for either.

This is not generosity; it is the difference between a gate that is on the
caller's route and one that is beside it. Measured in the field: a prompt that
told six agents to "write the change as a unified diff, the same format you
would give apply_patch" produced 85 admissions of which 59 refused, 32 with
the identical message *patch contains no unified diff file headers* -- because
`apply_patch` does not take a unified diff. It takes V4A, and V4A is what the
agents wrote. All six fought the parser and then fell back to their native
tool; 93% of the extra wall was model returns spent on that argument, and the
gate caught no hazard at all because it never saw a patch. A contract the
caller cannot express is not a contract, however well it is verified.

**Unified diff**: optional `diff --git` lines, `---` and `+++` file headers,
`@@ -l,s +l,s @@` hunk headers, and body lines prefixed by a space, `-`, or
`+`. A zero-length body line is read as an empty context
line, because many producers strip trailing whitespace. `\ No newline at end of
file` is honoured on both sides.

**apply_patch (V4A)**: `*** Begin Patch` opens the payload and `*** End Patch`
closes it. `*** Update File: path` opens a file section, optionally followed by
`*** Move to: path`; `*** Add File: path` and `*** Delete File: path` name
whole-file operations. Hunks inside an update open with `@@`, whose trailing
text is a *context anchor* rather than a line number, and carry the same
space/`-`/`+` body lines.

The grammars differ in exactly one thing, and it is the whole of the work: how
a hunk is located.

Application is strict either way. For a unified hunk at pre-image line `l`,
the concatenation of its context and removed lines must equal the snapshot's
lines `[l, l+s)` exactly. There is no offset search, no fuzz factor, and no
whitespace tolerance.

A V4A hunk carries no line numbers, so it is located by searching the file
from the current cursor for its exact context-and-removed block. When the `@@`
line carries text, that text anchors the search: the gate finds the anchor
first and looks for the block after it, which is what lets one patch edit the
second of two identical forms. The anchor is a hint and not a requirement --
if it does not match, the search falls back to the whole remaining file. An
author who writes an approximate anchor has still described the change
unambiguously, and refusing them would reproduce the exact failure this
grammar support exists to end.

A patch that does not apply is a typed refusal that names the file, the hunk
index, and the first mismatched or unlocatable line — never a best-effort
merge.

**The body delimits a hunk; the declared counts are advisory.** This is the
one place the design changed twice, and the second change was the field's.

A header that *undercounts* its body used to leave the surplus to be ignored,
applying the truncated hunk the counts described and dropping the rest — how a
patch that merely miscounted became an `:unreadable-post-image`, with a
three-line owner deleted by a header admitting one line leaving `[s]` and
`(inc s))` behind, orphaned and unbalanced, reported as though the author had
written it. The first fix refused that. Then the replay showed the mirror
image: 19 of 77 refused payloads *overcounted*, ran into the next `@@`, and
were refused as unreadable — and a further 10 undercounted and were refused by
the new check. In every one of those 29, the body said exactly what the author
meant.

So the counts are no longer load-bearing. A hunk runs to the next header, and
what it carries is what it means. A header whose counts disagree with its body
is recorded on the hunk and otherwise ignored.

**That is only safe because every line in between is understood.** The first
version of this rule was not: a body line the reader could not classify
quietly ended the hunk, and the remaining lines fell through the top-level
loop, which ignored whatever it did not recognise. The two together dropped
work in silence. A context line that lost its leading space committed a no-op
and reported success. A removed line whose own text began `-- ` rendered as
`--- `, was read as a file header, and deleted one line where the author had
written three. A payload with one unreadable line in the middle applied the
first of two edits and returned `ok: true`. The design document claimed
"nothing is dropped, so nothing is silently truncated" while all three were
live.

There is now no lenient reading of a line whose marker is missing. An
unclassifiable body line, and any patch-body line belonging to no hunk, is a
`:hunk-truncated` refusal naming the line and its number in the payload.
A `---` line is a file header only when a `+++` line follows it, which is the
disambiguation the format itself provides. And a bare `@@`, the sibling
grammar's marker, opens a content-located hunk — field payloads mix the two
inside one file section, and reading a real marker as garbage because the
header was the other grammar's would be the same mistake in reverse.

**A patch that changes nothing is refused.** If the post image equals the pre
image for every file named, the receipt is `:no-op-patch`, in preview as well
as commit. A no-op can never be a success receipt: it is not a small correct
edit, it is a request the reader failed to understand, and it was the shape
every one of the truncation defects came back wearing.

**A workspace-absolute header path names the file it means.** Ten payloads —
the first admit call of ten of the twelve runs — wrote the agent's own
worktree path into the header and were refused as invalid relative paths. The
path was the right file, inside the root, unambiguous. The gate now rewrites
an absolute path that lies under the resolved root to its relative remainder
before resolution. This is normalisation and not confinement: anything not
under that root is passed through unchanged and refused by the same guard as
before.

### #Whole-file operations

Creation, deletion and renaming — `*** Add File`, `*** Delete File`,
`*** Move to`, and `/dev/null` on either side of a unified header — are
admitted operations, not named refusals.

The v1 boundary said a creation has no pre-image to diff against, so its
central number is undefined. That was true of the *implementation* and false
of the *idea*: the pre-image of a file that does not exist is empty, and once
that is written down every part of the gate works on it unchanged. A created
file's owners are all added, its byte drift is zero because there is no
earlier text to have moved, and its hazards — a duplicate definition, an image
that does not read — are exactly the ones computable from the post image
alone. A deletion is the mirror: post-image empty, owners all removed. A move
is a creation of the destination carrying the patch's edits and a deletion of
the source, in one transaction, so a half-finished rename cannot survive a
failure.

The field decided this. On the rung-L task, which begins by creating a file,
`unsupported-patch-operation` fired on the first call of all six gate runs.
The one run that took the refusal's advice, created the file natively, and
then admitted the rest in a single verified commit was **the only gate run in
either cohort that never fell back to `apply_patch` on a `.clj` file**, and it
beat three of six natives. A boundary that every real task trips on the first
call is not a boundary; it is the reason the tool is not used.

Two guards replace the two the boundary was standing in for. A creation is
fenced by the **absence of its target**: `resolve-new-source-path` refuses a
path that already exists, which is also the staleness check a creation needs,
since a file that appeared between preview and commit fails it. A deletion is
fenced by the **workspace's own requires**: deleting a file is the one edit
whose damage is entirely outside the file, so nothing in the deleted image can
show it. The gate reads the workspace's namespace forms — the same structural
read it uses everywhere else — and refuses with `:namespace-form-removed`
naming every caller that would stop loading. A namespace nothing requires
deletes cleanly. The gate's structural report is a delta between two images of the same
file; a creation has no pre image to compare and a deletion has no post image,
so admitting them would mean publishing a receipt whose central number is
undefined. This is a boundary, not an oversight, and it is named in the refusal.

Each hunk contributes two spans: its pre-image line range and its post-image
line range. The narrow spans, covering only the removed and added lines rather
than the surrounding context, are what the string-literal hazard consults.

## #Form-identity delta

For each touched Clojure source, both images are parsed into an ordered vector
of top-level units. A unit is either an **owner** — a top-level form whose head
satisfies `forms/defining-form?`, named by its second element — or a **gap**,
the run of whitespace and comments between two owners. Using
`forms/defining-form?` rather than a private copy of the rule keeps the gate
from drifting away from the kernel's own classification, which
`CLAUDE.md` names as the single source of truth for what a defining form is.

Owners are aligned by name. Gaps are aligned by the pair of owner names that
bracket them, and a gap is compared only when both brackets exist in both
images, so an added or removed owner does not manufacture false gap drift.

Two alignment limits are known and stated rather than hidden. A top-level form
that is not a defining form and not an `ns` — a bare `(comment …)` or a
top-level side effect — has no name, so it is aligned by its ordinal among
unnamed forms; a patch that inserts one ahead of the others will report the
following unnamed forms as changed. And a name that occurs more than once in
either image is excluded from alignment entirely and reported only as the
duplicate hazard, because aligning it would require guessing which occurrence
the patch meant.

For each aligned owner pair the gate computes two predicates:

- `source-equal?` — the two source strings are identical.
- `code-equal?` — the two node trees are equal after removing every whitespace,
  comment, metadata, and reader-discard node.

These give three outcomes.

| `source-equal?` | `code-equal?` | Classification |
|---|---|---|
| true | true | untouched |
| false | true | **drift**: bytes moved with no structural change |
| false | false | **changed**: a real edit, reported as a changed owner |

### #byte_drift_outside_hunks

The mission text names this "bytes that differ outside the patch's own hunk
ranges." Read literally against a strictly applied patch that quantity is zero
by construction — every byte a strict patch changes is, definitionally, inside
one of its own hunks. The useful and computable reading of the same intent is
**bytes the patch changed that lie outside its structural change**, and that is
what the gate publishes under the contracted key:

```text
byte_drift_outside_hunks
  = Σ over drifting owners (differing-region size of pre vs post source)
  + Σ over drifting inter-owner gaps (differing-region size)
```

The differing-region size of two strings is `max(len) − common-prefix −
common-suffix`: the width of the window inside which they disagree. It is a
size, not an edit distance, and it is cheap and stable.

A clean patch, which changes code, produces zero. A patch that reformats a
comment it did not need to touch, or reprints an untouched form with different
whitespace, produces a positive number and names the owner. That is the exact
class of change the repository's Kent Beck section already calls out —
presentation drift scored separately from semantic correctness, with comments,
metadata, discards, and lint directives protected unless a declared change owns
them — and this is its machine-checkable form.

The literal hunk ranges are not discarded; they are published per file as
`hunk_line_spans` so a caller can still see exactly which lines the patch
declared.

### #protected_node_drift

For every touched owner, the gate counts four protected classes in both
images: comments, metadata, reader conditionals, and `#_` reader discards. It
publishes a per-owner, per-class delta under two conditions:

1. the class's nodes differ in count or in text and the owner's code did not
   change, or
2. the count **decreased**, for any reason.

The second condition is the important one. An edit that legitimately changes
code while silently deleting the comment above it is precisely the failure this
repository protects against, and it must be visible even though the owner is a
genuinely changed owner. An increase alongside a real code change is ordinary
authorship and is not reported.

## #Hazards

Each hazard carries `type`, `file`, `owner` when one exists, `span`, `class`,
and a one-line `message`. `class` is `:refusal` or `:informational`. Only
`:refusal` blocks a commit.

| Type | Class | Predicate |
|---|---|---|
| `:unreadable-post-image` | refusal | The post image does not parse as balanced Clojure |
| `:duplicate-definition` | refusal | One file's post image defines the same top-level symbol more than once |
| `:require-removed` | refusal | The post image's `ns` form no longer requires a library, or no longer refers a symbol, the pre image did |
| `:namespace-form-removed` | refusal | The post image has no `ns` form where the pre image had one |
| `:opaque-string-edit` | informational | A hunk changed the interior of a code-shaped string literal over 200 characters whose opening delimiter is outside every hunk |

`:unreadable-post-image` is the hazard that most justifies the gate. A textual
patcher's correctness criterion ends at line matching; it will happily produce
a file with one unbalanced paren and report success, and the agent discovers it
one compile later. The gate cannot produce that outcome, because reading the
post image is how it computes everything else.

`:duplicate-definition` is the shadowed-declaration class, and the detector
walks for it rather than pattern-matching the top level. A definition counts
wherever a reader will still evaluate it: inside `when`, `let`, `binding`,
`try`, `if`, `do`, metadata, a reader conditional, `(eval '(defn ...))`, or an
`(intern ns 'sym ...)` call, at any depth, with the wrapper path recorded on
the hazard so a receipt can say where it was found. It does not count what is
read and discarded — `#_`, `(comment ...)` — nor a bare quoted form, which is
data; `eval` is the exception that proves that rule, because its quoted
argument is executed.

Reader conditionals get one further distinction, and it is a judgement about
the whole file rather than about one form. Each conditional definition is
tagged with its platform, and a symbol's binding count is its unconditional
definitions plus the largest count any single platform carries. So
`#?(:clj (defn parse ...))` beside `#?(:cljs (defn parse ...))` is one
definition however far apart they are written, which is ordinary `.cljc` and
must never be refused; `#?(:clj (do (defn parse ...) (defn parse ...)))` is
two, because that branch really does bind it twice; two `:clj` branches in two
separate conditionals are two; and an unconditional definition beside a `:clj`
one is two, because a JVM reader evaluates both. Collapsing per form got the
first of those wrong and collapsing per name got the rest wrong.

The mission text describes the class twice — "duplicate top-level definition of the same symbol in one
file" and "a `def`/`defn` whose symbol is redefined later in the same file" —
but those two sentences denote one predicate over one file's post image, so the
gate implements one detector rather than inventing a distinction it cannot
defend. The hazard answers both readings: it names every defining span in
source order, so "which one wins" is on the receipt, and it carries
`introduced-by-patch` so a pre-existing duplicate is not attributed to the
caller. Defining forms whose names legitimately repeat, notably `defmethod`,
are excluded from the predicate.

`:require-removed` compares the libraries named by `:require` and
`:require-macros` clauses in the pre and post `ns` forms, read structurally
from the node tree rather than through `sexpr`, which fails outright on an
`ns` form carrying a reader conditional. A prefix list names one library per
member, so dropping one member of `[clojure [string :as str] [set :as set]]`
is a removal. A libspec moved *into* a reader conditional is still required
for that branch's platform and is not a removal. The comparison also carries
each library's referred symbols, so dropping `difference` from
`[clojure.set :refer [union difference]]` is a removal that names the symbol.
A removal is a refusal because it is the cheapest way to produce a file that
reads and lints but does not load.

`:namespace-form-removed` is separate because deleting the `ns` form removes
every require, alias, import and the namespace's own identity in one edit.
Reporting that as a single removed owner understates it to the point of being
misleading.

`:opaque-string-edit` is informational on purpose. A long code-shaped string —
a JavaScript body, an SQL statement, an embedded template — is opaque to a
Clojure reader, so the gate cannot verify a change inside it. Refusing would
punish a change that may be exactly right. Reporting says plainly: this hunk
edited content the structural gate could not check. "Code-shaped" is a
deliberately loose heuristic (a brace plus one of a small set of code tokens),
because a false positive costs one informational line and a false negative
costs a silent blind spot.

## #Verification

`verify: "focused"` runs two independent checks, and their honesty rules are
stricter than their coverage.

**Lint delta.** clj-kondo runs twice over the touched files: once over the
pre images and once over the post images, both materialized in a scratch
directory outside the workspace so preview and commit compute the same thing.
Findings are compared by `diagnostic-delta/diagnostic-delta`, which is
location-independent by design, so an unrelated edit that merely moves an
existing finding does not read as a regression. Introduced findings at
`:warning` or `:error` are blocking.

**Focused tests.** The touched source namespaces are mapped to `<ns>-test`.
The mapped namespace is run only when its file exists. The command is
repository-owned: the workspace configuration supplies a bounded
`:focused-test` profile whose `{namespaces}` and `{snapshot}` placeholders are
expanded with the derived namespaces and with a temporary directory holding
the post images. A profile whose command does not name `{snapshot}` would test
the bytes on disk — in preview the unpatched ones, after a commit no longer a
proof of anything the gate decided — so such a command is reported as **not
run** rather than credited. When no profile is configured, the gate publishes
the derived namespaces with `ran: false` and a stated reason. Guessing a test
runner would be the exact "generic verify=fast is not equivalent verification"
mistake this repository already stopped once.

### #Verification runs before the write, not after it

The order is fixed: apply to the snapshot, compute hazards, run every
requested check **against that snapshot**, and only then commit. A commit that
wrote first and verified afterwards can publish `ok: true, committed: true`
beside a failing test, which is a receipt that reports the opposite of what
happened; it also leaves the repair to a second act that may never come.

Two outcomes are distinguished, and the distinction carries the whole design:

- A check that **failed** — blocking analyzer findings, or focused tests with
  a non-zero failure count — is a refusal. Nothing is written, the receipt is
  `ok: false` with `error-type: verification-failed`, and `next_call` names
  which check blocked it.
- A check that **could not run** — no analyzer on the box, no declared test
  profile, no namespace to attribute a result to — does not block the commit,
  because a repository that has not declared a focused-test command would
  otherwise be unable to commit at all. It does keep `verification_complete`
  false, with the reason on the receipt.

### #What counts as a test result

`verification_complete` is true only when the analyzer ran clean **and** the
focused runner produced evidence that can be attributed to the namespaces the
gate asked about. A process that exits zero has proved that a process exited
zero; counting that as a test run is precisely how a gate comes to report a
verification it never performed. Evidence is therefore one of:

The evidence is a **report file the runner wrote**. The gate expands a
`{report}` placeholder to a path inside the snapshot directory it just
created, so the file cannot pre-exist and its presence proves this command
produced it. The report is read as EDN, JSON, or JUnit XML — whatever the
repository's own runner already emits — and must name every mapped namespace
with a positive test count and no failures or errors.

Nothing the command *prints* is evidence. A stdout summary is text the command
chose to emit: `printf 'Ran 7 tests containing 21 assertions.\n0 failures'`
passed the earlier check completely, and so did `/bin/true`. Worse, the
earlier namespace check compared the runner's reported namespaces against the
list the gate had just handed it, which any command passes by echoing its own
input. Naming `{snapshot}` in argv was equally cosmetic: it proved a word
appeared on a command line.

The threat model here is worth stating precisely, because it changes what the
fix has to achieve. **The focused-test command is repository-declared
configuration, not agent input** — it comes from the server's start map or from
a file in the tree, and a caller cannot supply one. So the adversary is not a
hostile command; it is an *ordinary* command believed by accident: a runner
misconfigured to point at the wrong directory, a wrapper that swallows a
non-zero exit, a suite that silently matched no namespaces. Every one of those
prints something plausible and writes no report. The report file closes the
gap not by defeating a liar but by requiring an artifact that only real work
produces.

A non-zero exit is decisive on its own. A command that writes a spotless
report and then exits three did not finish the way it meant to, and its report
describes whatever happened before it gave up rather than a clean suite; the
gate publishes `:runner-exit-nonzero` with the code and caps the status at
`:partial`. It still does not block the commit, because an unfinished check is
an unproven one rather than a failed one — a report that actually *names*
failures is what blocks.

Anything else publishes `verification_complete: false` with a stated reason:
`:no-test-evidence` when no report was written, `:unreadable-test-report` when
one was written but could not be parsed, `:report-namespaces-do-not-match` when
it covers different namespaces, `:test-command-not-report-bound` or
`:test-command-not-snapshot-bound` when the declared command cannot produce
one, and `:no-mapped-test-namespace` when there was nothing to attribute a
result to. The gate never reports a check it did not run, and never converts
an unavailable check into a pass.

### #Where the focused-test profile comes from

Precedence is explicit, because the two sources answer different questions.
The server's start configuration — the `-X` args map, key `:focused-test` with
`{:command [...] :timeout-ms n}` — is what *this server* was launched to do.
Failing that, `.clj-surgeon/focused-test.edn` at the workspace root, in the
same shape, is what *this tree* says about itself and travels with it. The
receipt names which source supplied the profile. Without a loader on either
path the gate could only ever report `verification_complete: false`, which is
how the first implementation shipped a verification story that no real commit
could reach.

### #What the receipt says about verification as a whole

`verification_complete` answers one question — did everything pass? — and its
`false` cannot distinguish a clean analyzer run with no test profile from a
run where nothing at all could be checked. Those deserve different reactions,
so every receipt also carries `verification_status`:

| Status | Meaning |
|---|---|
| `:complete` | every requested check ran and passed |
| `:partial` | at least one requested check produced a usable result, at least one did not |
| `:unverified` | no requested check produced a usable result |

with `verification_reasons` naming each shortfall. And when verification was
requested in commit mode and the status is `:unverified`, the receipt reports
`ok: false` while still reporting `committed: true`. The caller asked for
verification and did not get any; the bytes landed, and `ok` tells the truth
about the proof rather than about the write.

## #Commit

Commit reuses the existing transaction commit path rather than writing bytes
itself. The gate builds a compiled transaction value carrying the original
sources, the future sources, and one file plan per changed file with its
pre-image and post-image SHA-256, then calls
`intent-transaction/commit-compiled!`. That path already performs the
all-files hash preflight, the per-file recheck immediately before each
replacement, the atomic replace, the read-back hash proof, and the rollback
protocol. Reimplementing any of it would create a second, weaker write path.

A file whose bytes changed between snapshot and commit fails the preflight and
refuses as `:source-hash-mismatch` with nothing written.

### #Exclusive write authority, not just a hash guard

The kernel's compare-and-swap reads the file, compares its hash, and writes.
That is check-then-act with nothing in between, so two writers that read the
same bytes both pass the check and the second overwrites the first. The gate
made the window wider still by holding its snapshot across hazard analysis and
a full verification run.

Measured, before the fix: eight concurrent one-line commits to disjoint lines
of one file, six trials. Four trials lost an edit that its own receipt reported
as `committed: true`, and three ended in the kernel's
`:transaction-recovery-required` state with `source-unchanged: false` — the
worst receipt in the vocabulary, because it says neither "your change landed"
nor "nothing happened".

A commit therefore holds exclusive write authority over the canonical
workspace root, from the snapshot through the write. Within one server a
monitor keyed by that root serialises threads; where the workspace carries a
`.clj-surgeon` directory, an advisory file lock in it serialises separate
server processes on the same tree. The lock file is never created in a tree
that has no state directory: a gate asked to write one source file has no
business scattering directories through a repository. Immediately before the
write, still holding the lock, the gate re-reads every frozen file and refuses
if anything moved, so the interval between the proof and the write is empty.

Preview takes no lock. It writes nothing, and a preview that ran the analyzer
and a focused suite while holding the workspace's write lock would block every
commit on that tree for the length of a test run, buying no safety at all.

Because the cross-process half is conditional, every commit receipt states
what it actually got: `lock_scope` is `:cross-process` with the `lock_path`
when an advisory lock was available, and `:process` when serialisation reached
only the threads of one server. A conditional guarantee that lives in a design
document is a guarantee the reader of a receipt does not have. A lock that
cannot be taken at all — a read-only state directory, a `write.lock` that is
already a directory — is a typed `:workspace-lock-unavailable` refusal naming
the path, not an unexplained tool failure.

After the fix the same probe commits all eight edits in every trial, with no
losses and no recovery states. **The same lost update exists in the kernel
without this gate** — measured on `edit_clojure` at 8-way concurrency, two of
three trials lost an edit and one reached manual recovery. That is a kernel
finding, recorded here and owned elsewhere; this leaf does not change the
kernel's commit path beyond taking the lock around it.

### #Binding a commit to the preview that authorized it

The transaction's own preflight protects the window between *this* call's
snapshot and *this* call's write. It cannot protect the window between a
preview and a later commit, because those are two independent requests against
two independent snapshots: an unbound commit re-reads the file, re-applies the
patch to whatever is there now, and writes a post image the preview never saw.

A preview therefore publishes `expect_pre_sha256` in its `next_call` — the
pre-image digest of every touched file. A commit that carries those digests is
checked against the freshly frozen snapshot before anything else happens, and
refuses as `:source-hash-mismatch` if any file moved. A commit that omits them
is still legal, because a one-shot commit is a legitimate call, but every
receipt now states which it was: `pre_image_binding` is `"bound"` or
`"unbound"`. An unbound commit is a choice a reader can see, not a silent one.

# #Behaviour Matrix

| Case | mode | Refusal hazard | Result | Bytes written | `verification_complete` | `next_call` |
|---|---|---|---|---|---|---|
| Clean single-file patch | preview | no | ok, zeros | none | false | same call, `commit` |
| Clean multi-file patch | preview | no | ok, zeros | none | false | same call, `commit` |
| Clean patch | commit | no | ok, committed | all changed files, atomically | true when checks ran and passed | none |
| Comment reformat outside the edit | preview | no | ok, protected drift and byte drift positive | none | false | same call, `commit` |
| Whitespace-only reprint of an untouched form | preview | no | ok, byte drift positive, protected drift empty | none | false | same call, `commit` |
| Long code-shaped string edited without its delimiter | preview | no | ok, informational hazard | none | false | same call, `commit` |
| Duplicate top-level definition | preview | yes | ok false, hazard listed | none | false | same call, `preview`, `blocked_by` |
| Duplicate top-level definition | commit | yes | ok false, hazard listed, committed false | none | false | same call, `preview`, `blocked_by` |
| Post image does not read | either | yes | ok false, hazard listed | none | false | same call, `preview`, `blocked_by` |
| `ns` loses a require | either | yes | ok false, hazard listed | none | false | same call, `preview`, `blocked_by` |
| Hunk context does not match | either | n/a | typed refusal `:patch-does-not-apply` | none | false | same call, `preview` |
| Malformed or blank patch | either | n/a | typed refusal `:invalid-patch` | none | false | same call, `preview` |
| Add File, or `--- /dev/null` | either | no | ok, owners all added, `pre_image_binding: "created"` | the new file | per check | same call, `commit` |
| Add File whose target exists | commit | n/a | typed refusal `:target-already-exists` | none | false | same call, `preview` |
| Created file with a duplicate definition or an image that does not read | either | yes | typed refusal, nothing created | none | false | same call, `preview` |
| Delete File, or `+++ /dev/null` | either | no | ok, owners all removed | the file is removed | per check | same call, `commit` |
| Delete File whose namespace is still required | either | yes | typed refusal `:namespace-form-removed` naming the dependents | none | false | same call, `preview` |
| Move to | either | no | ok, destination created with the edits, source deleted, one transaction | both | per check | same call, `commit` |
| Payload in neither grammar | either | n/a | typed refusal `:invalid-patch` naming both grammars and quoting the first line | none | false | same call, `preview`, with `expected_headers` |
| Unified hunk body overruns its header | either | n/a | typed refusal `:hunk-body-overruns-header` | none | false | same call, `preview` |
| V4A hunk whose `@@` anchor does not match | either | n/a | applies, if the block itself is found | as usual | as usual | as usual |
| Non-source path in patch | either | n/a | typed refusal `:unsupported-patch-target` | none | false | same call, `preview` |
| Same file in two file headers | either | n/a | typed refusal `:duplicate-patch-target` | none | false | same call, `preview` |
| Patch over the admission limit | either | n/a | typed refusal `:patch-too-large`, before decoding | none | false | split the patch |
| Source changed after snapshot | commit | no | typed refusal `:source-hash-mismatch` | none | false | same call, `preview` |
| Source moved since the preview, digests supplied | commit | no | typed refusal `:source-hash-mismatch` | none | false | same call, `preview` |
| Blocking analyzer findings | commit | no | typed refusal `:verification-failed` | none | false | same call, `preview` |
| Focused tests failed | commit | no | typed refusal `:verification-failed` | none | false | same call, `preview` |
| `verify: "none"` | commit | no | ok, committed | all changed files | false | none |
| Analyzer unavailable | commit | no | ok, committed, lint status unverified | all changed files | false | none |
| Focused runner not snapshot- or report-bound | commit | no | ok, committed, tests reported not run, status `:partial` | all changed files | false | none |
| Runner printed a summary and wrote no report | commit | no | ok, committed, `:no-test-evidence`, status `:partial` | all changed files | false | none |
| Report names other namespaces | commit | no | ok, committed, `:report-namespaces-do-not-match` | all changed files | false | none |
| Report shows failures | commit | yes | typed refusal `:verification-failed` | none | false | same call, `preview` |
| Clean report, runner exited non-zero | commit | no | ok, committed, status `:partial`, `:runner-exit-nonzero` with the code | all changed files | false | none |
| Workspace lock cannot be taken | commit | n/a | typed refusal `:workspace-lock-unavailable` | none | false | same call, `preview` |
| No requested check produced a result | commit | no | **`ok: false`**, `committed: true`, `:verification-unverified` | all changed files | false | same call, `preview` |
| Two commits race on one file | commit | no | serialised; each commits or refuses | all changed files of the winner | per check | none |
| Patch over the byte limit | either | n/a | typed refusal `:patch-too-large` | none | false | split the patch |

Three rows carry the load. A refusal in commit mode writes nothing and still
returns the complete receipt; a caller never has to choose between knowing what
happened and knowing that nothing happened. A check that *failed* refuses
before the write, so no receipt can read `ok: true, committed: true` beside a
failing test. And a check that *could not run* is still an honest `ok` with
`verification_complete: false` — the write succeeded, the proof did not, and
the receipt says which and why.

# #Out of Scope

**Caller-supplied `workspace_root` (red-team finding 9).** A caller may name any
canonical absolute directory as `workspace_root`, and the shared router honours
it; the admission gate then confines every path to *that* root rather than to
the root the server was configured with. This is the router's existing
behaviour for every workspace-routed tool — `edit_clojure` and
`apply_clojure_changes` accept the same field — and it predates this leaf. It is
recorded here because a reader of the confinement argument above will otherwise
believe the configured project root is the boundary; the boundary is the
*requested* root. Changing that is a routing decision for the whole server, not
for one new verb, and this branch deliberately does not make it.
`MCP-OP-ADMIT-071` characterizes the behaviour so that a later change to it is
a visible, deliberate one rather than an accident.

**The same lost update exists in the kernel (red-team round two, finding 1).**
The write lock this leaf adds sits *around* the kernel's commit path and does
not change it. Measured without the gate, `edit_clojure` at eight-way
concurrency on one file lost an edit in two of three trials and reached
`:transaction-recovery-required` in one. Every entrance that calls
`intent-transaction/commit-compiled!` shares that exposure. Fixing it belongs
in the transaction kernel, where the guard is owned, not in one verb that has
learned to hold a lock; it is recorded here so the next reader knows the gate's
safety is local and the kernel's is not.

# #Non-Goals

- Composing, repairing, widening, or reformatting a patch.
- Fuzzy, offset, or whitespace-tolerant hunk matching.
- Whole-file creation and deletion in v1.
- Inventing a test runner when the repository has not declared one.
- Becoming a second editing language: the gate exposes no selectors, owners,
  counts, or expectations.
- Making a fan-out edit cheaper to author. The gate makes native edits
  verified; it does not make them shorter.

# #Requirements

Registration and request admission.

- [x] **MCP-OP-ADMIT-001**: When the MCP server publishes its full public tool catalog, clj-surgeon shall register exactly one tool named `admit_clojure_patch` whose declared outcome classes are preview, committed, and typed refusal.
- [x] **MCP-OP-ADMIT-002**: When an admit request is received, clj-surgeon shall resolve its workspace root through the shared workspace router and confine every path named by the patch to that resolved root.
- [x] **MCP-OP-ADMIT-003**: If an admit request omits its patch, supplies a blank patch, or supplies text that is not a parseable unified diff, then clj-surgeon shall publish a typed refusal and leave every file unchanged.
- [x] **MCP-OP-ADMIT-004**: When an admit request omits mode or verify, clj-surgeon shall use preview and focused respectively, and shall publish a typed refusal for any other value of either field.
- [x] **MCP-OP-ADMIT-005**: If a patch names a path that is not a project-relative Clojure or EDN source path inside the workspace root, then clj-surgeon shall publish a typed unsupported-target refusal naming that path in both preview and commit mode.

Patch application.

- [x] **MCP-OP-ADMIT-010**: When a patch is admitted, clj-surgeon shall apply every hunk to one frozen in-memory snapshot of current file bytes, matching context and removed lines exactly, without offset search or fuzz.
- [x] **MCP-OP-ADMIT-011**: If a hunk's context or removed lines do not equal the frozen snapshot at the hunk's declared position, then clj-surgeon shall publish a typed refusal naming the file and hunk and leave every file unchanged.
- [x] **MCP-OP-ADMIT-012**: When an admit request runs in preview mode, clj-surgeon shall write no file.
- [x] **MCP-OP-ADMIT-013**: When a patch is applied, clj-surgeon shall record each hunk's pre-image and post-image line span and publish those spans in the receipt.
- [D] **MCP-OP-ADMIT-014**: *Withdrawn.* If a patch creates or deletes a whole file, then clj-surgeon shall publish a typed unsupported-operation refusal and leave every file unchanged. Superseded by MCP-OP-ADMIT-095, 096 and 097 after the rung-L field result: the refusal fired on the first call of all six gate runs, and the operations are now admitted with defined empty images. The id is retained and never reused.

Form-identity delta.

- [x] **MCP-OP-ADMIT-020**: When a touched file is Clojure source, clj-surgeon shall parse its pre-image and post-image and publish the top-level owners added, removed, and changed, keyed by defining-form name.
- [x] **MCP-OP-ADMIT-021**: When an owner's pre-image and post-image differ in source but are equal after removing whitespace, comment, metadata, and reader-discard nodes, clj-surgeon shall count that owner's differing bytes as drift outside the patch's structural change.
- [x] **MCP-OP-ADMIT-022**: When a run of source between two owners present in both images differs, clj-surgeon shall count its differing bytes as drift outside the patch's structural change.
- [x] **MCP-OP-ADMIT-023**: When a touched owner's comment, metadata, reader-conditional, or reader-discard nodes differ in count or in text without a code change, or decrease in count for any reason, clj-surgeon shall publish that per-class delta as protected-node drift for that owner.
- [x] **MCP-OP-ADMIT-024**: When no owner and no inter-owner run drifts, clj-surgeon shall publish a byte drift of zero.

Hazards.

- [x] **MCP-OP-ADMIT-030**: When clj-surgeon publishes a hazard, that hazard shall carry its type, its file, its owner when one exists, its source span, and its refusal or informational class.
- [x] **MCP-OP-ADMIT-031**: If a post image cannot be read as balanced Clojure, then clj-surgeon shall publish a refusal-class unreadable-post-image hazard.
- [x] **MCP-OP-ADMIT-032**: If a post image defines the same top-level symbol more than once in one file, then clj-surgeon shall publish a refusal-class duplicate-definition hazard naming every defining span in source order and whether the patch introduced the duplication.
- [x] **MCP-OP-ADMIT-033**: If a post image's namespace form no longer requires a library its pre image required, then clj-surgeon shall publish a refusal-class require-removed hazard naming the missing libraries.
- [x] **MCP-OP-ADMIT-034**: When a patch changes the interior of a code-shaped string literal longer than two hundred characters whose opening delimiter lies outside every hunk, clj-surgeon shall publish an informational opaque-string-edit hazard and shall not refuse for it.

Verification.

- [x] **MCP-OP-ADMIT-040**: When focused verification is requested, clj-surgeon shall compare analyzer findings for the touched files between the pre-image and post-image as a location-independent multiset and publish that delta.
- [x] **MCP-OP-ADMIT-041**: When focused verification is requested, clj-surgeon shall derive focused test namespaces from the touched source namespaces, publish the derived namespaces, and run only those that exist.
- [x] **MCP-OP-ADMIT-042**: When verification is requested as none, clj-surgeon shall run no analyzer and no test and shall publish verification completeness as false.
- [x] **MCP-OP-ADMIT-043**: If a requested verification is unavailable, deferred, or failing, then clj-surgeon shall publish its typed status and shall publish verification completeness as false.
- [x] **MCP-OP-ADMIT-044**: When the analyzer ran clean and the focused runner produced attributable test evidence, being either a per-namespace receipt covering every mapped namespace or a parsed summary that ran a positive number of tests and names those namespaces, clj-surgeon shall publish verification completeness as true; otherwise it shall publish false with a stated reason.

Receipt, commit, and refusal.

- [x] **MCP-OP-ADMIT-050**: When an admit request terminates, clj-surgeon shall publish one receipt carrying outcome, mode, commit state, files, owners, protected-node drift, byte drift, hazards, lint delta, tests, hashes, elapsed time, verification completeness, and next call.
- [x] **MCP-OP-ADMIT-051**: If a refusal-class hazard is present in commit mode, then clj-surgeon shall write no file and shall publish the complete non-empty receipt with a next call naming the same request in preview mode and the hazard that blocked it.
- [x] **MCP-OP-ADMIT-052**: When no refusal-class hazard is present in commit mode, clj-surgeon shall commit every changed file through one atomic compare-and-swap transaction with read-back proof.
- [x] **MCP-OP-ADMIT-053**: If a touched file's bytes changed after the snapshot was frozen, then clj-surgeon shall refuse the commit as a stale-source refusal and leave every file unchanged.
- [x] **MCP-OP-ADMIT-054**: When an admit request terminates, clj-surgeon shall emit one telemetry call event naming the tool, the request shape, and the outcome shape without publishing source.
- [x] **MCP-OP-ADMIT-055**: When clj-surgeon publishes an admit refusal, that refusal shall be non-empty and shall carry a stable error type.

Adversarial review. Each of the following was falsified by a red-team probe
before it was written; the probe is named beside the id and its witness lives
in `test/clj_surgeon/admit_patch_test.clj`.

- [x] **MCP-OP-ADMIT-060**: If a patch names a path through parent traversal, an absolute path, a percent-encoded or backslash-escaped separator, an embedded NUL, a symlink resolving outside the workspace, or twice in two file headers, then clj-surgeon shall publish a typed refusal and write nothing. *(p1)*
- [x] **MCP-OP-ADMIT-061**: If any file in a multi-file patch cannot be applied or written, then clj-surgeon shall leave every other file in the patch at its original bytes. *(p2a, p2b)*
- [x] **MCP-OP-ADMIT-062**: If a touched file's bytes change between the frozen snapshot and the write, then clj-surgeon shall refuse the commit and preserve the competing content. *(p3a, p3b)*
- [x] **MCP-OP-ADMIT-063**: When clj-surgeon publishes a preview, its next call shall carry the pre-image digest of every touched file; when a commit supplies those digests, clj-surgeon shall refuse with a stale-source refusal if any file no longer matches; and every receipt shall state whether the commit was bound to a preview. *(p3c, p7b)*
- [x] **MCP-OP-ADMIT-064**: When a definition is wrapped in a reader conditional, a `do` form, or metadata, clj-surgeon shall still count it as a definition of its symbol; when it is inside `#_` or `(comment …)` it shall not; and a `declare` shall neither be counted as a definition nor prevent another definition of the same symbol from being counted. *(p4, p5 P4-live)*
- [x] **MCP-OP-ADMIT-065**: When an `ns` form requires libraries through a prefix list, or carries a reader conditional, clj-surgeon shall read every required library structurally, so that dropping one prefix-list member is a removed require and renaming only an alias is not. *(p4)*
- [x] **MCP-OP-ADMIT-066**: If a request's patch exceeds the admission limit, then clj-surgeon shall publish a typed too-large refusal before decoding the request, and no oversized payload shall escape the handler as an exception. *(p5a, p5b)*
- [x] **MCP-OP-ADMIT-067**: When clj-surgeon resolves a character offset to a line, it shall do so in logarithmic time, and a form-identity delta over a sixteen-thousand-line file shall complete within two seconds. *(p5c, p8)*
- [x] **MCP-OP-ADMIT-068**: When verification is requested, clj-surgeon shall run it against the snapshot before any file is written; if the analyzer reports blocking findings or the focused tests fail, it shall write nothing and publish a typed verification refusal; and a focused test command that cannot be pointed at the snapshot shall be reported as not run rather than credited. *(p6b, p6c)*
- [x] **MCP-OP-ADMIT-069**: When clj-surgeon publishes a refusal, its next call shall carry the patch's digest and field name rather than the patch text, and the published payload shall fit the shared public byte budget. *(p7a)*
- [x] **MCP-OP-ADMIT-070**: When a patch names an unsupported target, clj-surgeon shall refuse in preview as well as in commit, so no preview advertises a commit that is certain to refuse. *(p1g)*
- [x] **MCP-OP-ADMIT-071**: When a caller supplies a workspace root, clj-surgeon shall route the request to that canonical root, and the admission gate shall neither widen nor narrow the shared router's confinement contract. *(p9)*

Adversarial review, round two. Confinement, atomicity, the preview binding,
the admission cap and the linear line index held. Five classes did not.

- [x] **MCP-OP-ADMIT-080**: When focused verification runs, clj-surgeon shall accept as test evidence only a machine-readable report the runner wrote to a gate-named path inside the snapshot directory, naming every mapped namespace with a positive test count and no failures; text the command printed shall never be evidence. *(r1)*
- [x] **MCP-OP-ADMIT-081**: When clj-surgeon resolves the focused-test profile, it shall take it from the server start configuration, and failing that from `.clj-surgeon/focused-test.edn` at the workspace root, reporting which source supplied it. *(r6)*
- [x] **MCP-OP-ADMIT-082**: When an admit request terminates, clj-surgeon shall publish a verification status of complete, partial, or unverified together with the reasons; and if verification was requested in commit mode and the status is unverified, then the receipt shall report `ok` false while still reporting the commit that happened. *(r1e)*
- [x] **MCP-OP-ADMIT-083**: When a definition is introduced under any wrapper that still evaluates, at any depth, clj-surgeon shall count it and record the wrapper path; two definitions inside one reader-conditional branch shall count twice while one definition per branch counts once; a libspec inside a reader conditional shall count as required; a dropped `:refer` symbol shall be a removed require naming that symbol; and deleting the `ns` form shall be a refusal-class hazard. *(r3)*
- [x] **MCP-OP-ADMIT-084**: When clj-surgeon commits, it shall hold exclusive write authority over the canonical workspace root from the snapshot through the write, serialising threads within a server and, where the workspace carries a `.clj-surgeon` directory, server processes on the same tree; no request shall report a commit whose bytes are absent from the file. *(r5)*
- [x] **MCP-OP-ADMIT-085**: When a published payload is trimmed to fit the shared budget, clj-surgeon shall report the cumulative rows and bytes omitted across every trimming step. *(r7 follow-up)*
- [x] **MCP-OP-ADMIT-086**: When clj-surgeon enforces the admission limit, it shall count the patch in UTF-8 bytes rather than characters. *(r5a follow-up)*

Adversarial review, round three. The gate passed; three items remained.

- [x] **MCP-OP-ADMIT-087**: When clj-surgeon commits, the receipt shall state the scope of the lock it held as process or cross-process, naming the lock file when the lock reached across processes. *(x4)*
- [x] **MCP-OP-ADMIT-088**: If the workspace write lock cannot be taken, then clj-surgeon shall publish a typed lock-unavailable refusal naming the lock path, and write nothing. *(x4)*
- [x] **MCP-OP-ADMIT-089**: If the focused test command exits non-zero, then clj-surgeon shall publish verification as at best partial with the exit code and a runner-exit reason, whatever its report says. *(x1 A4)*
- [x] **MCP-OP-ADMIT-090**: When a symbol is defined once per reader-conditional platform, however many reader conditionals carry those branches, clj-surgeon shall count one definition; when one platform carries two definitions, or an unconditional definition accompanies a conditional one, it shall count two. *(x3 C1, C1b, C1c)*

Field result, arm Z. The gate lost in the field for a reason no adversarial
review could see, because every review wrote its own fixtures in the grammar
the gate already accepted.

- [x] **MCP-OP-ADMIT-091**: When a patch is submitted, clj-surgeon shall accept both the `apply_patch` V4A grammar and unified diff, selecting between them by the first non-blank line, and shall locate a V4A hunk by matching its content, treating the text on its `@@` line as a disambiguating hint rather than a requirement. *(z1)*
- [x] **MCP-OP-ADMIT-092**: When clj-surgeon reads a unified hunk, the body shall delimit it and the declared line counts shall be advisory, so that a header which miscounts in either direction neither truncates the applied hunk nor refuses a readable patch. *(z1, the unreadable-post-image cause; corrected by the field replay)*
- [x] **MCP-OP-ADMIT-093**: If a payload is in neither accepted grammar, then clj-surgeon shall name the grammars it tried, quote the first offending line, and publish the expected first line of each grammar in its next call. *(z1)*
- [x] **MCP-OP-ADMIT-094**: When clj-surgeon commits, it shall leave no control file, snapshot, or report of its own visible to the workspace's version control. *(z1)*

Field result, arm Z2 (rung L). The task begins by creating a file, so the v1
boundary fired on the first call of all six gate runs.

- [x] **MCP-OP-ADMIT-095**: When a patch creates a file, clj-surgeon shall admit it against an empty pre-image, report every owner of the new file as added, compute its hazards over the post image alone, publish a pre-image binding of created, and fence the write on the absence of the target. *(z2)*
- [x] **MCP-OP-ADMIT-096**: When a patch deletes a file, clj-surgeon shall admit it as owners removed; and if the deleted namespace is still required elsewhere in the workspace, it shall publish a refusal-class namespace-form-removed hazard naming the dependents. *(z2)*
- [x] **MCP-OP-ADMIT-097**: When a patch moves a file, clj-surgeon shall admit it as one transaction that creates the destination carrying the patch's edits and deletes the source. *(z2)*

Field replay. The 109 payloads six gate runs actually sent, extracted from the
z1 and z2 rollouts and replayed through the parser.

- [x] **MCP-OP-ADMIT-098**: When a patch header names an absolute path that lies inside the resolved workspace root, clj-surgeon shall read it as the project-relative path it denotes; a path outside that root shall be refused exactly as before. *(field replay)*

Adversarial review, round four. The reader's leniency was itself a
silent-truncation engine.

- [x] **MCP-OP-ADMIT-099**: When clj-surgeon splits patch text, it shall remove exactly one terminating newline and no more, so that a payload ending in a blank line keeps it and a payload ending in a newline gains no phantom line.
- [x] **MCP-OP-ADMIT-100**: If a line inside a hunk body carries no space, minus or plus marker, or a patch-body line belongs to no hunk, then clj-surgeon shall publish a typed truncation refusal naming the line and its number and apply nothing; a `---` line shall be read as a file header only when a `+++` line follows it; and a bare hunk marker shall open a content-located hunk rather than being read as an unclassifiable line.
- [x] **MCP-OP-ADMIT-101**: When a V4A hunk body carries a single-space context line, clj-surgeon shall read it as a context line for a blank source line rather than as whitespace to skip.
- [x] **MCP-OP-ADMIT-102**: If a patch produces a post-image identical to the pre-image for every file it names, then clj-surgeon shall refuse it as a no-op rather than publish a success receipt.

The field, rungs L and M (`z3`, `z4`, `z5`). Four commits on rung L and the z5
replay commit were written to disk on a verification that never ran, and two
real `git diff` payloads could not be read past their second file section.

- [x] **MCP-OP-ADMIT-103**: When a unified payload carries git's extended file headers — `index`, `old mode`, `new mode`, `deleted file mode`, `new file mode`, `similarity index`, `dissimilarity index`, `rename from`, `rename to`, `copy from`, `copy to` — between a `diff --git` line and that section's `---`/`+++` pair or first hunk, clj-surgeon shall accept and ignore them; outside that region an unclassifiable line shall still be refused as before. *(z5)*
- [x] **MCP-OP-ADMIT-104**: If a file section declares binary content (`Binary files … differ`, or `GIT binary patch`), then clj-surgeon shall publish a typed `binary-patch-unsupported` refusal naming the file, rather than skipping the section and reporting success for the rest of the patch. *(z5)*
- [x] **MCP-OP-ADMIT-105**: If mode is commit and verification was requested and `verification_status` is anything other than complete, then clj-surgeon shall refuse with a typed `verification-incomplete` error, write nothing, publish `committed false`, `mutation_attempted false` and `source-unchanged true`, and name preview and the blocking reason in `next_call`; every receipt shall carry `mutation_attempted`. *(z4, z5)* A runner that wrote a report and then exited non-zero (`runner-exit-nonzero`) remains `partial` rather than unverified — a report exists, it is merely untrustworthy — and is refused under this requirement all the same, because `partial` is no longer permission to write.
- [x] **MCP-OP-ADMIT-106**: When the caller passes `allow_partial true` and the workspace declares no focused-test profile at all, clj-surgeon shall permit the commit that MCP-OP-ADMIT-105 would otherwise refuse; when a profile exists and did not deliver evidence, `allow_partial` shall not waive the refusal. *(z4, z5)*
- [x] **MCP-OP-ADMIT-107**: When the focused runner was invoked and no report file appeared, clj-surgeon shall publish a typed reason distinguishing a non-zero or unfinished run (`verification-runner-failed`) from a clean run that produced nothing (`report-file-absent`), carry the runner's exit code, the resolved report path, the expanded command argv, its working directory, and the last 40 lines of the runner's merged output, and shall report `verification_status` as unverified rather than partial in both cases. *(z4, z5)*
- [x] **MCP-OP-ADMIT-109**: When the workspace ships `.clj-surgeon/focused-test.edn` carrying a `:namespaces` mapping, clj-surgeon shall select each touched file's focused test namespaces from that mapping — keyed by the source path or by the source namespace, valued as one namespace or a collection of them — and shall fall back to the `<ns>-test` path convention only for files the mapping does not cover. *(z4)*
- [x] **MCP-OP-ADMIT-110**: clj-surgeon shall resolve the focused-test profile from the repository file first and the server start configuration second, merged one key at a time so a tree that declares only `:namespaces` still runs the server's `:command`; and the receipt shall name which source supplied the command (`profile_source`) and which supplied the coverage mapping (`profile_source_namespaces`, or `path-convention` when neither did). *(z4)*
- [x] **MCP-OP-ADMIT-111**: If a test namespace the tree asserted — through the `:namespaces` mapping, or by the touched file being a suite itself — resolves to no file on the snapshot classpath, then clj-surgeon shall publish the typed reason `focused-namespace-missing` naming the source file, the namespace sought and every path tried, shall not invoke the runner, and shall report `verification_status` as unverified so that a commit refuses under MCP-OP-ADMIT-105. A source file with no sibling suite under the path convention remains the pre-existing `no-mapped-test-namespace` case and is not an error, because the convention discovers coverage where the mapping asserts it. *(z4)*
- [x] **MCP-OP-ADMIT-112**: When a patch touches a file that is itself a test namespace, clj-surgeon shall run that namespace as its own focused coverage rather than deriving a `<ns>-test-test` that cannot exist. *(z4)*
- [x] **MCP-OP-ADMIT-113**: Every focused verification receipt shall list `focused_namespaces`, the test namespaces actually selected for each touched file, so the caller can see what was run rather than infer it. *(z4)*
- [x] **MCP-OP-ADMIT-114**: When a patch removes a `:require` and the patched image references the removed library nowhere — not by its fully qualified name, not through any alias the pre-image ns form bound for it, and not through a `:refer`red symbol still used unqualified — clj-surgeon shall report the `require-removed` hazard as class `note` saying it was admitted as a dead-require removal, and shall carry it in the receipt's `hazards` rather than suppressing it. The reference test shall be structural over the post-image nodes, descending into reader-conditional branches and never into strings, comments or `#_` discards; a library the pre-image referred `:all` cannot be shown dead and stays a refusal. *(sewing, hand-driven)*
- [x] **MCP-OP-ADMIT-115**: If the patched image still references a library the ns form dropped, then the `require-removed` hazard shall remain class `refusal` and shall name every remaining reference site with its file, line, symbol and the route by which it still reaches the library. *(sewing, hand-driven)*
- [x] **MCP-OP-ADMIT-116**: Every hazard refusal's `next_call` shall carry `lifted_by`, naming what would lift the refusal and the sites to repair; where nothing can lift it, `lifted_by` shall say so explicitly rather than offering a follow-up that refuses identically. *(sewing, hand-driven)*
- [x] **MCP-OP-ADMIT-117**: When a patch drops a symbol from a `:refer` vector while the library stays required, clj-surgeon shall apply the same structural evidence test: the hazard is class `note` when the patched image uses that symbol nowhere unqualified, and stays class `refusal` naming every remaining bare use with its file and line when it does. A use that survives only as a qualified call is not evidence, because the library is still required and the qualified call still resolves. *(sewing, hand-driven)*
- [x] **MCP-OP-ADMIT-118**: clj-surgeon shall distinguish a workspace that declares no focused-test profile (`no-focused-test-profile`) from one that declares a profile naming no runnable command (`focused-test-profile-has-no-command`); the second shall read as unverified, and shall never inherit a waiver written for the first. *(z8)*
- [x] **MCP-OP-ADMIT-119**: The `allow_partial` waiver shall be decided on the directly observed absence of a focused-test profile, published on the receipt as `profile_absent`, and never on a runner reason that happens to name that state. *(z8)*
- [x] **MCP-OP-ADMIT-120**: A commit shall require `verification_status` complete regardless of the `verify` argument; `verify: "none"` shall refuse a commit rather than waive one, and the refusal's `next_call` shall propose `verify: "focused"` as the call that could lift it. Verification may still be declined in `preview`, which is where an unverified answer belongs. *(z8)*
- [x] **MCP-OP-ADMIT-108**: If `expect_pre_sha256` does not name exactly the files the patch touches, then clj-surgeon's refusal shall list the files the patch touches, the files that were named, and the difference in both directions, so the caller can repair the call without a second preview. *(z4)*

Field replay E-GATE-R, 2026-09-04. Fourteen real 21-file patches from the
frozen native corpus were replayed through the shipped verb. On all fourteen
`lint_delta` came back `{ran false, error-type clj-kondo-unavailable}` — not
because clj-kondo was absent, but because its EDN output (11,999 to 21,883
bytes) was cut at the 12,000-byte visible-bytes limit the receipt budget
imposes on every process read, so `edn/read-string` failed. Whether a k=1
patch was verified at all was decided by the digit count of a temp-directory
suffix; at k=2 and above it failed every time. The receipt said `unverified`
rather than clean, so nothing was falsely green — but the gate's substantive
detector had never run on a real input.

- [x] **MCP-OP-ADMIT-121**: If the analyzer's output exceeds the byte ceiling the gate reads it under, then clj-surgeon shall publish the typed failure `analyzer-output-truncated` naming the ceiling, the observed output size, the detector, and a remedy naming both routes out — raise the ceiling or narrow the patch — and shall reserve `clj-kondo-unavailable` for an analyzer that did not answer at all.
- [x] **MCP-OP-ADMIT-122**: clj-surgeon shall read the analyzer's findings under a ceiling that exists only to bound this process's memory, distinct from and far above the byte budget that bounds what a receipt publishes; a fan-out patch whose findings exceed the receipt budget shall still be verified, and the receipt shall not grow because the read did.
- [x] **MCP-OP-ADMIT-123**: When a requested detector produces no reading, clj-surgeon shall publish `detectors_not_run`, naming each such detector and the typed reason it produced nothing, and the receipt's text block shall carry the verification status as a word, every detector and reason that structure names, and a statement that the receipt is not a clean bill of health; no receipt shall present an empty hazard list beside a silent detector without saying so.
- [x] **MCP-OP-ADMIT-124**: If the analyzer could not run at all, then `verification_status` shall be `unverified` rather than `partial`, on the same terms MCP-OP-ADMIT-107 already applies to a focused runner that could not run; a check with no reading is not half a verification whichever of the two it is.
- [x] **MCP-OP-ADMIT-125**: `detectors_not_run` shall name a detector when that detector produced no reading, not when its process failed to exit; both halves shall be decided by one predicate computed once, so `verification_status` and `detectors_not_run` cannot disagree about what ran. A focused runner that exited and wrote an unusable report — namespaces nobody asked for, zero tests, a clean report from a non-zero exit, or nothing at all — produced no reading and shall be named. A suite that ran and failed, and an analyzer that ran and introduced a blocking finding, each produced the reading the gate asked for, are already blocking on their own terms, and shall not also be reported silent. A receipt on which no detector was ever consulted shall not publish an empty list, because `[]` is the affirmative claim that every requested detector answered.
- [x] **MCP-OP-ADMIT-126**: The `allow_partial` waiver shall apply only where a half-verification actually happened: the analyzer produced a reading, the focused suite is absent by observed profile rather than by failure, `verification_status` is `partial`, and `verify` is `focused`. A dead analyzer beside an absent profile is zero detectors, not one of two, and shall not be waivable; `verify: "none"` shall never be waivable in commit mode, because MCP-OP-ADMIT-120 closed that rung and a waiver that ignores the status word re-opens it one rung over on any repository without `.clj-surgeon/focused-test.edn`.
- [x] **MCP-OP-ADMIT-127**: When the analyzer's bounded run reports a typed admission failure — the admission wrapper unavailable, the clj-kondo executable unavailable, the admission lock timed out, the run deferred under host pressure, or the process interrupted — clj-surgeon shall publish that type, the gate path where the ex-data names one, and a remedy, rather than collapsing it into `clj-kondo-unavailable`, which is reserved for an analyzer that answered something unreadable. Every type it can publish shall read as unverifiable, so no admission failure can score as `partial`.
- [x] **MCP-OP-ADMIT-128**: clj-surgeon shall resolve the analyzer admission wrapper independently of the JVM's working directory — from an explicit override, the installed wrapper, or the build's own classpath — so that a workspace-routed server started outside a clj-surgeon checkout does not report a missing analyzer for every admit call on every workspace it routes.
- [x] **MCP-OP-ADMIT-129**: If a `Throwable` that is not an `Exception` reaches the admit handler's edge — an `OutOfMemoryError` raised below the analyzer read ceiling above all — then clj-surgeon shall publish a typed refusal naming the maximum heap and a remedy, rather than escaping the handler with no receipt at all; and because the gate cannot know how far such a failure got, that refusal shall not claim `source unchanged` in either the structured receipt or its text block.
- [x] **MCP-OP-ADMIT-130**: The admit path's memory bound shall be a receipt, not an argument: a bounded self-test shall drive `default-lint-runner` over 100, 1,000 and 10,000-file synthetic analyzer answers with distinct findings, in a JVM with an explicit `-Xmx`, and emit a numeric PASS or FAIL line per arm naming the findings count, the analyzer bytes, the observed heap and the budget. It shall be reachable by a named Make target and shall never be wired into `test`, `test-fast` or `mcp-test`, so it cannot become a slow gate by accident.

Landing review, round three (inb-cbca17). `admit_clojure_patch` is the
catalog's only write tool, and its refusal text sat outside the trunk's
text-is-a-superset-of-structuredContent ratchet the alias-migration verb
already carries (MCP-OP-ALIAS-059): `remedy` and `next_call` were absent from
every refused receipt's text, including the ceiling that names the number
that would lift an `analyzer-memory-exhausted` refusal and the follow-up call
`verification-incomplete` itself proposes; the tool description tells a
caller to copy `expect_pre_sha256` from a preview's `next_call`, and the text
never showed a `next_call` at all.

- [x] **MCP-OP-ADMIT-131**: Every refusal kind clj-surgeon's admit gate constructs — enumerated from the source, not from a maintained list, so a kind added later without a text witness fails this gate the day it is written — shall render every leaf of its structured receipt in `content[0].text`, however deep, bounded per leaf and in total count rather than dropped: a leaf past the per-fact character ceiling is elided with a stated cut, and a refusal carrying more leaves than the text's fact budget states how many more live in structuredContent rather than silently stopping. `remedy`, when present, shall render as its own line.
Adversarial review, round four (Sol, on the round-three landing). The
text-is-a-superset ratchet MCP-OP-ADMIT-131 and MCP-OP-ADMIT-132 installed
was, in each of four places, a universal claim standing on a partial witness.

- [x] **MCP-OP-ADMIT-133**: The set of `:error-type` values clj-surgeon's admit gate may publish shall be DECLARED, not derived by scanning source text, and shall be enforced at the one point every published receipt passes — outside every `catch` on the entrance's path — so that a refusal whose kind is unenumerated throws a plain `IllegalArgumentException` rather than reaching a caller. It shall be a plain `IllegalArgumentException` and not an `ex-info` carrying an `:error-type`, because that is the shape the gate's own catch clauses turn back into a receipt, and a violation that launders itself into the surface the guard protects is not guarded. The enumeration shall be proved complete by EXECUTION and in both directions: the suite shall record every kind the entrance actually publishes and assert set equality with the declaration, so a kind that reaches the surface unenumerated fails, and a kind enumerated that no fixture drives fails too. The source scan shall survive only as a complement — every kind constructed in the files the gate calls shall be either enumerated or named in a justified not-reachable list with its reason, and an excuse the scan no longer finds shall be deleted rather than carried. *(s1)*

- [x] **MCP-OP-ADMIT-134**: The text block shall name every leaf its structured receipt spells, with NO exclusion by key and none by shape: `:files` and `:hashes` are leaves like any other, and an empty map, an empty vector, a `nil` and a blank string shall render with their label and the characters JSON spells for them — `key={}`, `key=[]`, `key=null`, `key=""`. The renderer's exclusion set shall be empty, so there is no policy for a witness to copy; if a key ever leaves the walk it shall be named here with its reason before it leaves. A leaf past the per-fact character ceiling shall be cut with the cut stated in characters, never dropped; a receipt whose facts exceed what is left of the one public byte budget shall NAME the leaves it did not print and state their exact count, and that section — the elision note included — shall fit that remainder (superseded in its budget arithmetic by MCP-OP-ADMIT-136, which deleted the fixed half-share this clause originally named). The witness shall walk the receipt AS JSON, through a second implementation sharing no function and no constant with the renderer. *(s2)*

- [x] **MCP-OP-ADMIT-135**: Every receipt's `next_call` shall render in `content[0].text` as sendable JSON VERBATIM at any size, or as an explicit statement that no follow-up call exists; there shall be no pointer, no truncation, and no second budget of its own. It shall be rendered last, after the fact walk, so that under budget pressure other leaves elide first — each leaf's value cut at the per-fact ceiling with the cut stated, then whole leaves dropped from the tail of the path-sorted order with the omitted count stated — and the `next_call` last of all, which is to say never. If the `next_call` alone exceeds the one public payload budget, clj-surgeon shall publish a typed refusal naming its exact character count, the budget, and a remedy, rather than a pointer: the reader this text block exists for is the reader who cannot read `structuredContent`. The witness shall parse the JSON back out of the rendered text and assert it equals the receipt's own call, digest for digest, so that the claim under test is that the text is SENDABLE and not merely that it is long. *(s4)*

Adversarial review, round five (Opus, on the round-four landing). The
text-is-a-superset ratchet was still false on an ORDINARY receipt: the fact
section obeyed a SECOND, invented budget of half the public one, so a
twenty-file preview whose structured face was 15,086 bytes -- under half the
32,640-byte budget, untruncated -- published a text missing 68 of the leaves
structuredContent spelled, and the builder's own witness, unmodified, failed
68 assertions on it.

- [x] **MCP-OP-ADMIT-136**: There shall be ONE public byte budget and the fact section shall be charged what is actually left of it after the header, the error sentence, the source-unchanged line, the detector note, the remedy line and the verbatim `next_call` are counted -- never a fixed share of it. The text shall name every leaf its structured receipt spells for every receipt whose structured face fits that budget; when both faces cannot fit, the STRUCTURED face shall give ground first, trimmed by the receipt's own bounded-payload machinery until the text that spells it fits, so that supersetness is preserved by shrinking the structure rather than by silently shortening the text. A caller's load-bearing fields -- `ok`, `operation`, `source-unchanged`, `mutation_attempted`, `pre_image_binding`, `lock_scope`, `error`, `remedy` and `next_call` -- shall render in a fixed head that elision never reaches, so that no field is lost because its name sorts late. If leaves must still be elided, the text shall name them and state their exact count; it shall never be a silent subset. The witnesses shall drive twenty-file and forty-file previews through the entrance and run the superset witness on the receipts the gate actually publishes.

- [x] **MCP-OP-ADMIT-137**: The predicate that decides a receipt is a refusal shall be the SAME on both of its faces: whatever `summary` renders as a refusal, `checked-refusal-kind!` shall check the kind of. A guard that fires on `(false? :ok)` while the renderer branches on truthiness leaves `:ok nil` rendered to the caller as a refusal under a kind nothing enumerated, and `refusal` merges its caller's data map last, so that override is one keyword away. The witness shall assert the RELATION between the two predicates over a range of `:ok` values, copying neither side's test.

- [x] **MCP-OP-ADMIT-138**: A refusal kind whose only fixture is a TIMING bound shall be proved by a battery target and not by the fast merge gate. The `transaction-recovery-required` fixture widens its window by racing a busy-spinning watcher thread against a 64-file write; it is a resource race, and worse, it was load-bearing for the `:once` set-equality assertion, so a flake would have reported "the enumeration claims kinds no fixture drives" for an unrelated reason. It shall move to a named Make target, never wired into `test`, `test-fast` or `mcp-test`, and the `:once` witness shall accept a DECLARED battery-only kind — declared with the target that proves it, so the exemption names its own evidence rather than being an absence.

- [ ] **MCP-OP-ADMIT-139**: No receipt clj-surgeon's admit gate publishes shall exceed the number its own refusal text calls the public payload budget. The oversize-`next_call` decision shall be made on the RECEIPT that would carry the call, envelope included and after the payload has been trimmed, and not on the call's characters alone: at a `next_call` of exactly 32,640 characters the published receipt was 32,911 bytes, 271 over, because the keys, quotes and braces that carry the call were charged to nobody. The refusal shall name the call's size, the receipt's size and the budget, and it shall fire only when the `next_call` is the reason the receipt cannot fit — a receipt too large for other reasons states its elision and publishes, as it always has. The witness shall drive both sides of the bound through the receipt-publishing path.

- [x] **MCP-OP-ADMIT-132**: Every receipt's `next_call` — refused or not — shall render in `content[0].text` as sendable JSON when it fits the text's own budget, as a bounded pointer naming its length in structuredContent when it does not, or as an explicit statement that no follow-up call exists; a preview's `next_call.arguments.expect_pre_sha256`, which the tool description tells a caller to copy for the commit that authorizes it, shall therefore be readable from the text alone.

# #Witness Failure Baseline

The witness tests in `test/clj_surgeon/admit_patch_test.clj` were written and
run before any implementation existed. The recorded failure output is:

```text
$ java -cp "$(clojure -Spath -A:clj-surgeon/mcp-test)" clojure.main \
    -e "(require 'clj-surgeon.admit-patch-test)"

Syntax error macroexpanding at (clj_surgeon/admit_patch_test.clj:1:1).
#:clojure.error{:phase :execution, :line 1, :column 1,
                :source "clj_surgeon/admit_patch_test.clj"}
CAUSE: Could not locate clj_surgeon/form_identity__init.class,
clj_surgeon/form_identity.clj or clj_surgeon/form_identity.cljc on classpath.
```

Every witness in that file names a namespace that did not exist, so the suite
could not even load. That is the intended shape of a pre-implementation
failure: the tests were written against the contract in this document, not
against code that already worked.

# #Recorded Evidence

The gate was exercised end to end through a live dev MCP server on port 7897
(`make mcp-dev-start MCP_DEV_PORT=7897`), driven over the streamable-HTTP MCP
transport, against a scratch copy of the `marvin-voice-remote` repository. The
original repository was never opened for writing.

`tools/list` on that session returned:

```json
["inspect_clojure", "apply_clojure_changes", "edit_clojure",
 "transform_clojure", "admit_clojure_patch"]
```

## #Receipt one: a clean patch, previewed then committed

The patch changes one expression inside one owner:

```diff
--- a/src/marvin_voice_remote/store.clj
+++ b/src/marvin_voice_remote/store.clj
@@ -47,7 +47,7 @@
 (defn get-recording [id] (get @recordings id))

 (defn list-recordings []
-  (->> @recordings vals (sort-by :created-at) reverse vec))
+  (->> @recordings vals (sort-by :updated-at) reverse vec))

 (defn set-status!
   "Move a recording to a new status. Records the event and bumps the
```

Preview first, which wrote nothing. Note that its `next_call` carries the
pre-image digest and the patch's digest, and not the patch:

```text
----- TEXT -----
admit_clojure_patch
  admit-patch-preview · 1 file(s) · owners +0 ~1 -0 · drift 0 bytes · hazards 0 · 400.70 ms
verification_complete=false
----- STRUCTURED -----
{
  "source-unchanged": true,
  "committed": false,
  "protected_node_drift": {},
  "verification_reasons": [
    "no-focused-test-profile"
  ],
  "verification_complete": false,
  "owners": {
    "added": [],
    "removed": [],
    "changed": [
      "src/marvin_voice_remote/store.clj::list-recordings"
    ]
  },
  "verification_status": "partial",
  "next_call": {
    "tool": "admit_clojure_patch",
    "arguments": {
      "mode": "commit",
      "verify": "focused",
      "workspace_root": "/home/genek-forge/tmp/claude-1002/-home-genek-forge-src-marvin-voice-remote-channel-connector/b623492c-458d-4156-a14d-a041f5a37e7c/scratchpad/mvr-gate-scratch",
      "expect_pre_sha256": {
        "src/marvin_voice_remote/store.clj": "368c39e1914489da56f778bc14ae76d39231137a4749a18905f3ba36c8f6d2d0"
      }
    },
    "patch_field": "patch",
    "patch_sha256": "7cf52e3ed246ca1008990fce21a5b8fd0a18b25bab9f6157e3b1f9708481275c",
    "note": "resend the same patch text in the patch field; it is deliberately not echoed here"
  },
  "mode": "preview",
  "hazards": [],
  "lint_delta": {
    "baseline-count": 0,
    "blocking-introduced": [],
    "blocking-introduced-count": 0,
    "introduced-count": 0,
    "introduced": [],
    "future-count": 0,
    "unchanged-count": 0,
    "removed-count": 0,
    "ok": true,
    "ran": true,
    "removed": []
  },
  "pre_image_binding": "unbound",
  "tests": {
    "ran": false,
    "passed": 0,
    "failed": 0,
    "skipped": 0,
    "tests-run": 0,
    "namespaces": [],
    "reason": "no-focused-test-profile"
  },
  "elapsed_ms": 400.695658,
  "workspace-root": "/home/genek-forge/tmp/claude-1002/-home-genek-forge-src-marvin-voice-remote-channel-connector/b623492c-458d-4156-a14d-a041f5a37e7c/scratchpad/mvr-gate-scratch",
  "hashes": {
    "src/marvin_voice_remote/store.clj": {
      "pre": "368c39e1914489da56f778bc14ae76d39231137a4749a18905f3ba36c8f6d2d0",
      "post": "aa8541e0c00a10ddef41e2bdbc07ce03e2e43da8970e5a7ab4d156765aece8ff"
    }
  },
  "byte_drift_outside_hunks": 0,
  "files": [
    {
      "owners": {
        "added": [],
        "removed": [],
        "changed": [
          "list-recordings"
        ]
      },
      "hunk_line_spans": {
        "pre": [
          [
            50,
            50
          ]
        ],
        "post": [
          [
            50,
            50
          ]
        ]
      },
      "protected_node_drift": {},
      "file": "src/marvin_voice_remote/store.clj",
      "byte_drift_outside_hunks": 0,
      "pre_sha256": "368c39e1914489da56f778bc14ae76d39231137a4749a18905f3ba36c8f6d2d0",
      "kind": "clojure",
      "hunks": 1,
      "post_sha256": "aa8541e0c00a10ddef41e2bdbc07ce03e2e43da8970e5a7ab4d156765aece8ff"
    }
  ],
  "ok": true,
  "operation": "admit-patch-preview"
}
```

Then the follow-up exactly as the preview handed it back, with the binding:

```text
----- TEXT -----
admit_clojure_patch
  admit-patch! · 1 file(s) · owners +0 ~1 -0 · drift 0 bytes · hazards 0 · 228.30 ms
verification_complete=false
----- STRUCTURED -----
{
  "source-unchanged": false,
  "committed": true,
  "protected_node_drift": {},
  "verification_reasons": [
    "no-focused-test-profile"
  ],
  "verification_complete": false,
  "owners": {
    "added": [],
    "removed": [],
    "changed": [
      "src/marvin_voice_remote/store.clj::list-recordings"
    ]
  },
  "verification_status": "partial",
  "next_call": null,
  "mode": "commit",
  "hazards": [],
  "lint_delta": {
    "baseline-count": 0,
    "blocking-introduced": [],
    "blocking-introduced-count": 0,
    "introduced-count": 0,
    "introduced": [],
    "future-count": 0,
    "unchanged-count": 0,
    "removed-count": 0,
    "ok": true,
    "ran": true,
    "removed": []
  },
  "pre_image_binding": "bound",
  "tests": {
    "ran": false,
    "passed": 0,
    "failed": 0,
    "skipped": 0,
    "tests-run": 0,
    "namespaces": [],
    "reason": "no-focused-test-profile"
  },
  "elapsed_ms": 228.297233,
  "workspace-root": "/home/genek-forge/tmp/claude-1002/-home-genek-forge-src-marvin-voice-remote-channel-connector/b623492c-458d-4156-a14d-a041f5a37e7c/scratchpad/mvr-gate-scratch",
  "hashes": {
    "src/marvin_voice_remote/store.clj": {
      "pre": "368c39e1914489da56f778bc14ae76d39231137a4749a18905f3ba36c8f6d2d0",
      "post": "aa8541e0c00a10ddef41e2bdbc07ce03e2e43da8970e5a7ab4d156765aece8ff"
    }
  },
  "byte_drift_outside_hunks": 0,
  "files": [
    {
      "owners": {
        "added": [],
        "removed": [],
        "changed": [
          "list-recordings"
        ]
      },
      "hunk_line_spans": {
        "pre": [
          [
            50,
            50
          ]
        ],
        "post": [
          [
            50,
            50
          ]
        ]
      },
      "protected_node_drift": {},
      "file": "src/marvin_voice_remote/store.clj",
      "byte_drift_outside_hunks": 0,
      "pre_sha256": "368c39e1914489da56f778bc14ae76d39231137a4749a18905f3ba36c8f6d2d0",
      "kind": "clojure",
      "hunks": 1,
      "post_sha256": "aa8541e0c00a10ddef41e2bdbc07ce03e2e43da8970e5a7ab4d156765aece8ff"
    }
  ],
  "ok": true,
  "operation": "admit-patch!"
}
```

The file on disk afterwards carried `:updated-at`, and its SHA-256 equalled the
receipt's `post` hash. `verification_complete` is false on a successful commit
because the scratch workspace declares no focused-test profile: the analyzer
delta ran and passed, no test evidence exists, and the receipt says which.

Replaying that same bound commit, now that the workspace has moved, refuses
rather than writing a second time over content the preview never saw:

```text
----- TEXT -----
admit_clojure_patch refused · source-hash-mismatch · 4.65 ms
The workspace moved since the preview that authorized this commit: src/marvin_voice_remote/store.clj
source unchanged
----- STRUCTURED -----
{
  "source-unchanged": true,
  "committed": false,
  "protected_node_drift": {},
  "drifted": [
    {
      "file": "src/marvin_voice_remote/store.clj",
      "expected-hash": "368c39e1914489da56f778bc14ae76d39231137a4749a18905f3ba36c8f6d2d0",
      "actual-hash": "aa8541e0c00a10ddef41e2bdbc07ce03e2e43da8970e5a7ab4d156765aece8ff"
    }
  ],
  "verification_reasons": [],
  "verification_complete": false,
  "owners": {
    "added": [],
    "removed": [],
    "changed": []
  },
  "error": "The workspace moved since the preview that authorized this commit: src/marvin_voice_remote/store.clj",
  "verification_status": "unverified",
  "next_call": {
    "tool": "admit_clojure_patch",
    "arguments": {
      "mode": "preview",
      "verify": "focused",
      "workspace_root": "/home/genek-forge/tmp/claude-1002/-home-genek-forge-src-marvin-voice-remote-channel-connector/b623492c-458d-4156-a14d-a041f5a37e7c/scratchpad/mvr-gate-scratch"
    },
    "patch_field": "patch",
    "patch_sha256": "7cf52e3ed246ca1008990fce21a5b8fd0a18b25bab9f6157e3b1f9708481275c",
    "note": "resend the same patch text in the patch field; it is deliberately not echoed here",
    "blocked_by": "source-hash-mismatch"
  },
  "mode": "commit",
  "hazards": [],
  "lint_delta": {
    "ran": false
  },
  "pre_image_binding": "unbound",
  "tests": {
    "ran": false,
    "passed": 0,
    "failed": 0,
    "skipped": 0,
    "namespaces": []
  },
  "elapsed_ms": 4.651209,
  "workspace-root": "/home/genek-forge/tmp/claude-1002/-home-genek-forge-src-marvin-voice-remote-channel-connector/b623492c-458d-4156-a14d-a041f5a37e7c/scratchpad/mvr-gate-scratch",
  "hashes": {},
  "byte_drift_outside_hunks": 0,
  "files": [],
  "error-type": "source-hash-mismatch",
  "ok": false,
  "operation": "admit-patch-refused"
}
```

## #Receipt two: a duplicate definition, refused

The patch appends a second `get-recording` to the same file, which is the
shadowed-declaration class, and asks to commit:

```diff
--- a/src/marvin_voice_remote/store.clj
+++ b/src/marvin_voice_remote/store.clj
@@ -76,3 +76,6 @@
   (swap! recordings update id merge m)
   (swap! recordings update id assoc :updated-at (now-str))
   (get @recordings id))
+
+(defn get-recording [id]
+  (get @recordings id))
```

```text
----- TEXT -----
admit_clojure_patch refused · duplicate-definition · 23.10 ms
Top-level symbol get-recording is defined 2 times in one file
source unchanged
----- STRUCTURED -----
{
  "source-unchanged": true,
  "committed": false,
  "protected_node_drift": {},
  "verification_reasons": [],
  "verification_complete": false,
  "owners": {
    "added": [],
    "removed": [],
    "changed": []
  },
  "error": "Top-level symbol get-recording is defined 2 times in one file",
  "verification_status": "unverified",
  "next_call": {
    "tool": "admit_clojure_patch",
    "arguments": {
      "mode": "preview",
      "verify": "focused",
      "workspace_root": "/home/genek-forge/tmp/claude-1002/-home-genek-forge-src-marvin-voice-remote-channel-connector/b623492c-458d-4156-a14d-a041f5a37e7c/scratchpad/mvr-gate-scratch",
      "expect_pre_sha256": {
        "src/marvin_voice_remote/store.clj": "aa8541e0c00a10ddef41e2bdbc07ce03e2e43da8970e5a7ab4d156765aece8ff"
      }
    },
    "patch_field": "patch",
    "patch_sha256": "4c5d77b86e38c74bb1ea853c30e90da87c033421417e445f7118120869be2718",
    "note": "resend the same patch text in the patch field; it is deliberately not echoed here",
    "blocked_by": "duplicate-definition"
  },
  "mode": "commit",
  "hazards": [
    {
      "spans": [
        [
          47,
          47
        ],
        [
          80,
          81
        ]
      ],
      "file": "src/marvin_voice_remote/store.clj",
      "type": "duplicate-definition",
      "introduced-by-patch": true,
      "class": "refusal",
      "kind": "defn",
      "owner": "get-recording",
      "message": "Top-level symbol get-recording is defined 2 times in one file",
      "span": [
        47,
        47
      ]
    }
  ],
  "lint_delta": {
    "ran": false
  },
  "pre_image_binding": "unbound",
  "tests": {
    "ran": false,
    "passed": 0,
    "failed": 0,
    "skipped": 0,
    "namespaces": []
  },
  "elapsed_ms": 23.103899,
  "workspace-root": "/home/genek-forge/tmp/claude-1002/-home-genek-forge-src-marvin-voice-remote-channel-connector/b623492c-458d-4156-a14d-a041f5a37e7c/scratchpad/mvr-gate-scratch",
  "hashes": {
    "src/marvin_voice_remote/store.clj": {
      "pre": "aa8541e0c00a10ddef41e2bdbc07ce03e2e43da8970e5a7ab4d156765aece8ff",
      "post": "d01a255f89607ecb4ad4fd3c26d1b4788db3c36e775cc1e4e18e35dc6af5f07f"
    }
  },
  "byte_drift_outside_hunks": 0,
  "files": [
    {
      "owners": {
        "added": [],
        "removed": [],
        "changed": []
      },
      "hunk_line_spans": {
        "pre": [
          [
            76,
            75
          ]
        ],
        "post": [
          [
            79,
            81
          ]
        ]
      },
      "protected_node_drift": {},
      "file": "src/marvin_voice_remote/store.clj",
      "byte_drift_outside_hunks": 0,
      "pre_sha256": "aa8541e0c00a10ddef41e2bdbc07ce03e2e43da8970e5a7ab4d156765aece8ff",
      "kind": "clojure",
      "hunks": 1,
      "post_sha256": "d01a255f89607ecb4ad4fd3c26d1b4788db3c36e775cc1e4e18e35dc6af5f07f"
    }
  ],
  "error-type": "duplicate-definition",
  "ok": false,
  "operation": "admit-patch-refused"
}
```

Nothing was written. The file's SHA-256 after the refusal was still
`aa8541e0c00a10ddef41e2bdbc07ce03e2e43da8970e5a7ab4d156765aece8ff`, the post
hash of the previous successful commit. The hazard names both defining spans in
source order, so the receipt answers both "there is a duplicate" and "which one
wins"; `introduced-by-patch` being true attributes the duplication to this
patch rather than to the file it landed on.

# #Adversarial Review

Nine red-team probes were run against the first implementation of this gate;
the scripts are retained at `scratchpad/redteam-admit/p1..p9.clj`. Confinement,
atomicity, and the commit-time compare-and-swap held. Eight classes did not,
and every one of them is now a named requirement with a witness test.

| Probe | What it attacked | Before | After |
|---|---|---|---|
| p1a-f | Traversal, absolute paths, percent and backslash escapes, NUL, symlink escape | refused, nothing written | unchanged; now witnessed by ADMIT-060 |
| p1g | Preview of a non-source target | `ok: true`, advertising a commit certain to refuse | `ok: false`, `:unsupported-patch-target` in both modes (ADMIT-070) |
| p1h | One file named by two file headers | `:transaction-write-failed` after a write and rollback | `:duplicate-patch-target`, refused before the transaction (ADMIT-060) |
| p2a, p2b | A later file that cannot apply or cannot be written | earlier files restored | unchanged; now witnessed by ADMIT-061 |
| p3a, p3b | A file mutated between snapshot and write | `:source-hash-mismatch`, newer bytes kept | unchanged; now witnessed by ADMIT-062 |
| p3c, p7b | Preview, external edit, then commit | committed over content the preview never saw | preview hands back `expect_pre_sha256`; a bound commit refuses, and every receipt states `pre_image_binding` (ADMIT-063) |
| p4 | Duplicate hidden in `#?`, `do`, `^{}`, or shielded by `declare` | all four evaded detection | all four detected; `#_` and `(comment …)` correctly are not definitions (ADMIT-064) |
| p4 | Require dropped from a prefix list, or from an `ns` carrying a reader conditional | not detected | detected; an alias-only rename still is not a removal (ADMIT-065) |
| p5a | 1 MB, 25 MB, 50 MB patches | 1 MB refused; 25 MB and 50 MB threw `StreamConstraintsException` out of the handler | all three return `:patch-too-large` in 0-6 ms, before any decode (ADMIT-066) |
| p5c, p8 | Delta cost against file size | quadratic: 1 531 ms at 2 002 lines, 54 859 ms at 16 002 | linear: 193 ms at 2 002, 367 ms at 16 002, 902 ms at 32 002 (ADMIT-067) |
| p6b | A focused-test command that exits 0 and runs nothing | credited as a passing test run | `:test-command-not-snapshot-bound`; a command that cannot be pointed at the snapshot is never credited (ADMIT-068) |
| p6c | Commit with blocking lint and failing tests | file written, then verification reported as failed | verification runs against the snapshot first; `:verification-failed`, nothing written (ADMIT-068) |
| p7a | Refusal payload size | receipt echoed the whole patch back in `next_call` | digest and field name only; receipt fits `public-byte-budget` (ADMIT-069) |
| p9 | Caller-supplied `workspace_root` outside the configured root | honoured by the shared router | unchanged and deliberate; recorded under **Out of Scope** and characterized by ADMIT-071 |

## #Round two

The fixed gate was reviewed again. Confinement, atomicity, the preview
binding, the admission cap and the linear line index held on re-test. Four
classes did not, and the scripts are retained at
`scratchpad/redteam-admit2/r1..r6.clj`.

| Probe | What it attacked | Before | After |
|---|---|---|---|
| r5 | 8 concurrent commits, disjoint lines, one file | 4 of 6 trials **lost an edit its receipt called committed**; 3 trials reached `:transaction-recovery-required` with `source-unchanged: false` | 6 of 6 trials commit all 8; nothing lost, no recovery states (ADMIT-084) |
| r1a | `printf 'Ran 7 tests...'`, `{snapshot}` named and ignored | `verification_complete: true` | `:no-test-evidence`; stdout is never evidence (ADMIT-080) |
| r1b | `cd` into the snapshot, run nothing, print a summary | `verification_complete: true` | `:no-test-evidence` (ADMIT-080) |
| r1c, r1d | zero tests, exit 0 | `verification_complete: false` for the wrong reason | `:test-command-not-report-bound` (ADMIT-080) |
| r1e | analyzer deliberately unable to run, then commit | `ok: true`, `committed: true`, `error-type: nil` | `verification_status: :unverified`, **`ok: false`**, `committed: true`, `:verification-unverified` naming both reasons (ADMIT-082) |
| r1f | no sibling test file | committed with a bare `false` | committed, `verification_status: :partial` (ADMIT-082) |
| r6 | `:focused-test` has no loader in `src/` | every real commit could only be `verification_complete: false` | loaded from the `-X` start map, else `.clj-surgeon/focused-test.edn`; the receipt names the source (ADMIT-081) |
| r3 | definitions hidden in `when`, `let`, `binding`, `try`, `if`, `eval`, `intern` | all seven walked past the detector | all seven counted, with the wrapper path on the hazard (ADMIT-083) |
| r3 | two definitions inside **one** reader-conditional branch | collapsed to one | counted twice; one per branch still counts once (ADMIT-083) |
| r3 | libspec moved into `#?(:clj [lib :as x])` | falsely reported `:require-removed` | recognised as present for that branch (ADMIT-083) |
| r3 | `:refer` symbol dropped, library kept | not detected | `:require-removed` naming the symbol (ADMIT-083) |
| r3 | whole `ns` form deleted | reported only as an owner removal | refusal-class `:namespace-form-removed` (ADMIT-083) |
| — | `payload_omitted` after several trimming steps | reported the last step only | cumulative rows and `payload_omitted_bytes` (ADMIT-085) |
| — | admission cap on multibyte source | counted characters | counts UTF-8 bytes (ADMIT-086) |

The concurrency numbers are the ones to keep. Eight writers, one file,
disjoint lines -- the friendliest possible race -- and the receipts were wrong
four times in six. A receipt that says `committed: true` about bytes that are
not in the file is worse than an error, because it terminates the caller's
investigation; that is the failure mode this repository's own doctrine names
first, and a hash guard cannot prevent it because it answers a different
question than the one a writer needs answered. The same probe run against
`edit_clojure` with no gate at all lost an edit in two of three trials, which
is why that finding is recorded under **Out of Scope** rather than closed here.

## #Round three

The gate passed on re-test: confinement, atomicity, the lock, the preview
binding, the report-file evidence, the deeper detector and the byte cap all
held. Three items were open, and the probes are at
`scratchpad/redteam-admit3/x1..x5.clj`.

| Probe | What it attacked | Before | After |
|---|---|---|---|
| x1 A4 | clean report, runner exits 3 | credited `:complete`, `verification_complete: true` | `:partial`, `verification_complete: false`, `:runner-exit-nonzero` with the code; the commit still stands (ADMIT-089) |
| x1 A1, A3, A3b | report written by a runner that ran nothing; foreign namespaces; own failures | already correct | unchanged; a report naming other namespaces is not evidence, and one naming failures blocks |
| x3 C1b | two adjacent `#?` forms, disjoint branches, one symbol | **refused as a duplicate — a false positive on ordinary `.cljc`** | not a duplicate; deduplicated by platform across every conditional in the file (ADMIT-090) |
| x3 C1c | two definitions inside one `:clj` branch | refused | still refused, and so are two `:clj` branches in separate forms (ADMIT-090) |
| x4 E2 | receipt says nothing about lock scope | no key mentioned the lock | `lock_scope` `:process` or `:cross-process`, with `lock_path` when it reaches across processes (ADMIT-087) |
| x4 E3, E4 | read-only state directory; `write.lock` is a directory | `:admit-tool-failure` | `:workspace-lock-unavailable` naming the path, nothing written (ADMIT-088) |
| x5 | `payload_omitted_bytes` accounting | measured against a payload whose annotations were not excluded on both sides | content delta with annotation keys excluded, and stable when re-bounded (ADMIT-085) |

The `.cljc` false positive is the one worth dwelling on. Every earlier round
made the detector see *more*, and each time the risk was the same: a detector
that refuses correct code is not a safer detector, it is a broken one that
teaches its caller to route around it. One definition per platform is the
single most ordinary shape in cross-platform Clojure, and the gate was
refusing it. The fix is not a special case but a better rule — count what a
single reader would evaluate — and that rule happens to make every earlier
duplicate case come out the same way it did before.

## #The field, arm Z

Three adversarial rounds passed the gate and one field run failed it, for a
reason none of the rounds could reach: every review wrote its fixtures in the
grammar the gate already accepted.

| Symptom | Measured | Cause | Fix |
|---|---|---|---|
| Refusal rate | 59 of 85 admissions refused (69%) | — | — |
| The single largest class | 32 `:invalid-patch`, the **identical** first refusal in all six runs: *patch contains no unified diff file headers* | the prompt said "unified diff, the same format you would give `apply_patch`", but `apply_patch` takes V4A | both grammars accepted, detected by the first line (ADMIT-091) |
| Second class | 16 `:patch-does-not-apply` | V4A hunks carry no line numbers to apply at | V4A hunks located by content, `@@` text as a hint (ADMIT-091) |
| Two `:unreadable-post-image` | self-inflicted | a header that undercounted its body left the surplus removals to be silently dropped, applying a truncated hunk that cut an owner in half | `:hunk-body-overruns-header`, refused before application (ADMIT-092) |
| All six runs | fell back to `apply_patch` on the `.clj` files; 93% of the extra wall was model returns spent arguing with the parser; the gate caught no hazard and post-write calls did not go to zero | the gate was never on the route | — |

The lesson is not about diffs. Three rounds of adversarial review made the
gate progressively harder to fool and did not once ask whether the caller
could reach it, because each round wrote its own inputs. A red team that
supplies its own fixtures is testing the implementation against the
specification; only the field tests the specification against the world. The
cheapest thing this gate could have done, at any point in three rounds, was
read one real payload.

Two smaller things the same run surfaced. The gate left `.clj-surgeon/`
untracked in every workspace it committed to, so `git status` reported a
change nobody made -- nothing in the commit path stages anything, the control
file simply sat there; the state directory now carries a self-ignoring
`.gitignore` covering the lock and any report artefact, while leaving the
repository's own `focused-test.edn` tracked. And the snapshot venue was
already outside the workspace, which is now witnessed rather than assumed.

## #Replaying the field

The acceptance test the three adversarial rounds lacked: not fixtures the
reviewers wrote, but the 109 payloads six gate runs actually sent, extracted
from the z1 and z2 rollouts and replayed through the parser.

| Measure | Before | After |
|---|---|---|
| Payloads that parse | 32 of 109 | **109 of 109** |
| Of the 77 field refusals, parse | 0 | **77 of 77** |
| Of the 77, refused for a cause the gate no longer has | — | **51 of 77** |
| First admit call of each run, against the correct pristine pre-image | 0 of 10 applied | **8 of 10 apply** |

The 51 breaks down as the causes that no longer exist: 39 `invalid-patch`
(wrong grammar, miscounted headers, absolute paths), 6
`unsupported-patch-operation` (whole-file creation), 3 `admit-tool-failure`,
2 `unreadable-post-image` (the gate's own truncation), 1
`source-file-not-found`. The remaining 26 were refused for reasons that still
exist and are still right: 17 `patch-does-not-apply` (the context genuinely
did not match) and 9 `verification-failed`.

**The apply column cannot be measured honestly from the rollouts, and saying
so is part of the result.** Three reconstructions were tried — each payload
independently against the base tree, sequential per run, and sequential with
the agents' 19 interleaved native `apply_patch` calls replayed too — and the
fidelity check fails in all three: of the 32 payloads that *succeeded in the
field*, only 0 to 4 re-apply, depending on the strategy. The reason is
structural. A payload that now succeeds where it once refused changes the tree
for everything after it, and much of what follows in the rollout is the
agent's *retry of the same edit*, which then cannot apply. Reconstructing the
field pre-image would need every shell command replayed as well.

So the parse number is the honest headline, because it is state-independent
and it is exactly what the grammar fix moves; the first-call number is the
honest apply evidence, because those ten payloads ran against a pristine tree
and need no reconstruction at all. Both are in the table. The sequential
replays are retained in `scratchpad/` as negative results.

Two further defects came out of the replay that no adversarial round had
reached, and both were in code the rounds had already hardened: the count
handling, whose first fix was itself wrong in the mirror direction, and the
phantom trailing line a terminating newline leaves behind, which was invisible
while counts terminated a hunk and annexed a fourth line onto every three-line
hunk the moment the body did. Neither was reachable by a reviewer writing
fixtures, because a reviewer writes headers that count correctly.

The measured complexity change is worth keeping as a number rather than a
claim. The old line lookup counted newlines from the start of the file on
every call, and the call count grows with the file, so the delta was quadratic
in file size; the same fixture that took 54.9 seconds now takes 0.37 seconds,
and the bound in ADMIT-067 is stated at two seconds so a regression is caught
long before it is felt.

## #The rungs, and why the verification never ran

Three cohort runs on Anvil put the gate in front of an agent that had not been
told to like it. `z3` (rung M, a four-file feature) obtained
`verification_status: complete` on every admit call. `z4` (rung L, an
eleven-file refactor) and the `z5` replay obtained it on none, and the shape of
the failure was identical in both: `tests.ran true`, `tests-run 0`, `exit 1`,
`report_written false`, `reason no-test-evidence`, `verification_status
partial`. **Four commits landed on rung L that way, and so did the z5 replay
commit.** Each receipt honestly reported `verification_complete: false`, and
each one had already written the files.

Two separate things had to be true for that, and the fix separates them.

**The gate defect: `partial` was permission.** `partial` says one of the two
requested checks produced a usable result. A clean analyzer delta is one. So a
focused runner that was launched, exited non-zero, and wrote nothing scored one
out of two, and one out of two wrote to disk. The commit path then noticed the
shortfall *after* the transaction and downgraded `ok` on a receipt whose
`committed` was already `true` — a field about a write that had happened,
which is not a gate. MCP-OP-ADMIT-105 moves the check in front of the write and
makes the shortfall a typed refusal; MCP-OP-ADMIT-107 stops a runner that could
not run from counting as half a verification at all.

**The apparatus defect, on z5, named exactly.** The gate server on 7894 was
launched with a `:focused-test` profile whose command is
`["clojure" "-Sdeps" "…" "-M:gate" "bin/gate-report.clj" "{snapshot}" "{report}" "{namespaces}"]`,
run with the workspace root as its working directory. The cohort runner installs
`bin/gate-report.clj` into an arm's worktree **only when the arm is `Z` or `F`**,
and the file exists nowhere in the repository under test — `git ls-tree` at the
base commit has `bin/kaocha` and no `gate-report.clj`. The z5 replay ran against
`N`-arm worktrees, which never received it. So the command's own script was
absent, `clojure -M:gate` exited 1, and no report could appear. **The missing
file is `bin/gate-report.clj` in `~/acid/wt/z5-replay-N1` and
`~/acid/wt/z5-replay-N2`; its source is `~/acid/receipts/gate-report.clj`; the
installer that skips it is the arm test in the cohort runner.**

**On z4 the cause is not recoverable, and that is the finding.** Both z4 `Z`
worktrees carry `bin/gate-report.clj` and `.clj-surgeon/focused-test.edn`,
byte-identical to z3's, installed by the same line of the same runner. The
runner was invoked and exited 1; everything it said about why was thrown away,
because the receipt kept the exit code and discarded the output. Diagnosing z5
took a shell script and a `git ls-tree`; diagnosing z4 is no longer possible
from the artifacts at all. MCP-OP-ADMIT-107 is written so this class of question
is answered in the payload: the resolved report path, the expanded argv, the
working directory, and the last forty lines the runner printed.

One adjacent observation, recorded and not acted on: `resolve-focused-test`
gives the server's start configuration precedence over the workspace's
`.clj-surgeon/focused-test.edn`, so the `:namespaces` mapping those worktrees
shipped — the repository's own statement of which suite covers which source —
was never read. The gate derived `<ns>-test` by path convention instead. On
rung M the derived namespaces existed and passed. On rung L the patch touched
eleven sources whose derived suites span the whole reducer stack. That is a
plausible mechanism for z4's exit 1 and it is not evidence, which is the point.

## #The z4 hunk that did not apply

The other z4 refusal was `patch-does-not-apply: Hunk 0 of
src/marvin_voice_remote/reducer_session.clj does not match the file; its first
line is "            [clojure.data.json :as json]"`. It is worth recording that
this one was right. The rollout's bytes carry a bare `@@` hunk whose first
context line is that require vector alone on a line, indented thirteen spaces.
In the pre-image the form is inline after `  (:require `, and every genuine
continuation is indented twelve. The context line does not occur in the file at
any indent, so `git apply` would refuse it too; the matcher is not stricter than
git here, and the agent's own retry — which corrected exactly that context line
— succeeded. The parser repair in MCP-OP-ADMIT-103 must not loosen this, so it
is pinned by a witness of its own.
