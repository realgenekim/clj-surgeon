# Positional Subject Authority Audit

Date: 2026-08-29

Lane: SURGEON2, shipped-surface audit and deterministic local probe

Product authority: SURGEON1

Issue: `clj-surgeon-qf9` (P0)

## Result

Yes. A currently shipped mutation route accepts an unchecked positional
selector that can silently lower to the wrong owner.

CLI `:op :edit` accepts `(line N)` as the root of an `:expect`-guarded direct
write. The content guard proves that the selected subtree equals `:expect`. It
does not prove that the line selected the owner the caller intended. Duplicate
content makes the distinction observable.

## Executable proof

The disposable source was:

~~~clojure
(ns demo.duplicate)

(defn intended [] :old)

(defn wrong [] :old)
~~~

The direct write was equivalent to:

~~~text
clj-surgeon :op :edit \
  :file duplicate.clj \
  :expr "(-> (line 5) (match :old) (replace :new))" \
  :expect :old
~~~

Observed receipt evidence:

~~~clojure
{:ok true
 :mode :expect-guarded
 :operation :replace-subform!
 :match-count 1
 :selector {:query [[:line 5] [:find :old] [:replace :new]]}
 :applied-edit {:path [{:form wrong}]
                :line 5
                :before ":old"
                :after ":new"}
 :verified {:whole-file-parsed true
            :atomic-write true
            :read-back-hash <result-hash>}}
~~~

The final bytes retained `(defn intended [] :old)` and changed only
`(defn wrong [] :new)`. This is not a stale-source or parsing failure. Every
existing guard truthfully verified a valid edit of the wrong subject.

The experiment-only witness
`shipped-line-selector-can-silently-mutate-the-wrong-owner` runs this complete
filesystem boundary and requires the wrong-owner receipt path and exact final
bytes. The isolated worktree nREPL at port 59168, PID 41830, CWD
`/private/tmp/clj-surgeon-emission-compression.x8sTjT/worktree`, and `-Xmx512m`
passed five tests and 70 assertions.

## Shipped entrance inventory

| Entrance | Caller-visible positional or opaque value | Mutation authority | Audit result |
|---|---|---:|---|
| CLI `:edit` with `(line N)` and `:expect` | One-based physical line | Direct | **Unsafe:** a wrong in-range line can select another owner with duplicate expected content and commit successfully. |
| CLI `:edit` with `right`, `left`, `up`, or `down` and `:expect` | Relative zipper position | Direct | Positional selection can choose a different duplicate subtree. A named `(form NAME)` root preserves owner identity, but the nested semantic subject remains caller-selected by position. |
| CLI `:edit` with `span` or `partition-all` | Sibling count and partition ordinal | Plan first | Multi-form spans cannot satisfy the one-form direct `:expect` comparison. They remain reviewable, hash-fenced plans before `:replace-subform!`. |
| CLI `:replace-subform!` | Saved plan contains internal preorder addresses | Direct only from reviewed plan | Checkable reference. The caller cannot submit a raw preorder as write authority; the plan binds complete source/result hashes and refuses stale source. |
| CLI `:change!` | Input list order; compiler emits internal addresses | Direct | Paths and optional owner names are explicit. Internal indexes and preorder addresses are derived from one frozen snapshot, not supplied as subject aliases. |
| CLI extraction, move, rename, declare repair, and undo routes | Operation order or receipt IDs | Direct | File paths, form names, namespace prefixes, or hash-fenced receipts remain explicit authority. No caller file/owner ordinal was found. |
| MCP `inspect_clojure` line and xray selectors | Line or relative position | Read only | No write authority. |
| MCP `edit_clojure` and explicit `apply_clojure_changes` | Array row indexes exist only for diagnostics | Direct | Public mutations retain explicit file paths and named owner/root scopes. No public file, owner, span, line, or ordinal selector was found. |
| MCP compact relations | `file_index` and `row_index` in refusal diagnostics | No input authority | These are zero-based error locations in output, not subject selectors. Relation inputs retain exact files, owners, source symbols, and counts. |
| MCP retained `basis` and decision `site` IDs | Opaque server-issued IDs | Direct after validation | Checkable references. Unknown/expired basis, unknown site, workspace mismatch, incomplete coverage, and stale source refuse before write. The in-memory compiled basis remains source authority. |
| MCP extraction source hash and undo receipt | Opaque hash/receipt | Direct after validation | Checkable references bound to explicit files and frozen bytes; tampering or stale source refuses. |

The public MCP schema traversal inspected every advertised tool. It found no
numeric or positional mutation selector. The only opaque mutation inputs were
the retained `basis` and decision `site` IDs above. The `file_index` and
`row_index` fields found in MCP code are relation diagnostic output only.

## Why plan review changes the safety class

An opaque or positional value is not automatically unsafe. It is unsafe when a
wrong but valid value can become mutation authority without an independent
binding check.

~~~text
unchecked line/index
    -> resolve another real subject
    -> discard intended identity
    -> content/hash guards validate wrong subject
    -> silent wrong mutation

server-issued plan/basis
    -> bind exact workspace + source snapshot + subjects
    -> wrong/unknown/stale reference refuses
    -> no write
~~~

This gives the compression law:

> Compress repetition. Never replace identity with an unchecked reference.

## Re-screened emission portfolio

| Shape | Class | Exact retained effect | Decision |
|---|---|---:|---|
| Omit `matches=1` | Repetition removal | 5.62% alone | Safe but contained by stronger shapes. |
| `file_groups` | Repetition removal | 18.16% alone | Explicit file paths remain, but retained model correctness loss makes it ineligible. |
| `replacement_groups` | Repetition removal | 18.55% alone | Best new safe pure option. Each site still names file and owner; below the 20% model gate. |
| Closed relations | Repetition removal | 22.80% in the older pure screen; larger measured product win | Explicit files, owners, source symbols, and counts remain. Already the champion production lane, not a new hill. |
| Closed relations plus require delta | Repetition removal | 42.96% in the older pure screen | Explicit named identities remain. Supersedes replacement groups on this fixture. |
| `file_index` | Identity replacement | 13.04% alone; 23.67% with replacement groups | Permanently NO-GO. Deterministic wrong-file and shipped wrong-owner analogues both commit silently. |
| Positional tuples | Field-identity replacement | 21.47% alone | NO-GO without an independent self-describing binding. A wrong in-range slot can remain schema-valid while changing meaning. |
| Retained basis or plan handle | Checkable reference, not same-turn representation compression | Not comparable | Safe only because the server binds it to workspace, snapshot, and subject evidence. It adds a preparation/review boundary and is not a substitute for this emission hill. |

No unimplemented repetition-only composition both clears 20% and retains prior
correctness evidence:

- `replacement_groups + omit matches=1` remains 18.55% because the stronger
  shape already omits the defaults;
- `replacement_groups + file_groups` reaches 23.64%, but `file_groups` retains
  a model correctness loss;
- adding replacement groups to either closed relation shape saves zero
  additional bytes because relations already consume those repeated rows.

The honest new-option ceiling is therefore 18.55%. The already-earned closed
relation facade remains the repetition-only champion.

## Smallest safe product ratchet

The narrowest repair is to remove direct write authority from line-rooted
`:expect` edits. A line-rooted edit remains useful as a plan, but must return to
the existing review plus hash-fenced `:replace-subform!` route. Named-owner
roots can retain one-call guarded mutation.

That ratchet closes the proven wrong-owner case without inventing heuristic
intent recovery. A later design may restore a one-call line route only with a
caller-visible, self-describing owner binding that a wrong line cannot satisfy.
For an unnamed macro owner, that likely means an exact owner source anchor or
hash, not another ordinal.

Do not fix this by checking whether the wrong owner is real, whether the leaf is
unique inside that owner, or whether read-back matches. The falsifier already
passes every one of those checks.

## Scope and non-actions

This audit changed no product namespace, installed tool, shared server, port,
or existing process. It does not authorize a fix. `clj-surgeon-qf9` owns the
Linked-Intent repair and permanent product witnesses.

An earlier exploratory probe used the shared standalone analysis nREPL at
port 53157 while SURGEON1 had a global `with-redefs` capture active. SURGEON1's
test correctly observed the unrelated temporary workspace. That mixed-process
gate is discarded as coordination-confounded. The 59168 gate above was rerun
in a separate JVM and is the verification authority for this receipt.
