# insert_forms v1 — reviewed consult contract

Status: proposed builder contract; self-reviewed against trunk `bd124492bb557377afe94065ab58149110f21608`.
Recorded: 2026-09-10T21:19:37.067362+00:00. No implementation, test execution, independent reviewer approval,
or performance admission is claimed. MUST/REFUSE below are normative.

## 1. One operation, two entrances

Insert one nonempty payload at one structural boundary in one existing `.clj` file.
MCP tool `insert_forms` and CLI `clj-surgeon :insert-forms! :request-file X`
MUST call the same planner, transaction, and receipt projector. No server is
needed by the CLI. This is a new CLI shorthand, not today's `:op` requirement;
`:op :insert-forms! :request-file X` is also accepted and has identical behavior.
One request is one atomic splice; no batch, preview, replacement, or implicit retry.

Closed EDN schema (unknown keys, wrong types, duplicate map keys refuse):

```edn
{:version 1
 :workspace_root "/absolute/canonical/project"
 :file "src/example.clj"
 :guard {:sha256 "<64 lowercase hex digits>"}
 :anchor {:scope "top-level" :owner {:kind "defn" :name "a"}
          :expect 1 :position "after"}
 :payload {:text "(defn b [] 3)" :forms 1}}
```

All keys are keywords in EDN; enum values, names, and paths are strings.
MCP JSON maps keywords to identical string keys, without changing values or counts.
`version`, root, file, guard, anchor and payload are required. Positive integer
`payload.forms` is the sole payload cardinality assertion; `anchor.expect` MUST be 1.
Use strict UTF-8. Root must exist; file is a relative path resolving inside root.
Refuse symlink components, traversal, nonregular files, missing files and hardlinks
with link count >1. Preserve file permission bits. No file creation or Git commit.
Bound before parsing: request 2 MiB, source 8 MiB, payload 1 MiB, 1,000 inserted
forms, CST depth 512. Exceeding any bound is `:limit-exceeded`, never truncation.

Builder amendment (2026-09-10): source, payload and request parsing additionally
refuse more than 100,000 lexical units (tokens, reader prefixes, delimiters,
comments and whitespace runs), or reader/container nesting beyond 512, before
recursive parsing. The complete candidate is limited to 16 MiB of UTF-8; projected
indentation expansion is checked before materializing the adjusted payload, and
the complete candidate size is checked before materializing it. Staging and
read-back use this candidate ceiling, while initial capture retains the 8 MiB
source ceiling. These bounds refuse with `:limit-exceeded`; no truncation occurs.

LF and uniform CRLF sources are accepted; mixed newlines refuse. No BOM.
`.cljs`, `.cljc`, reader conditionals and reader-eval in source/payload refuse v1.
Parse syntax without evaluation, alias resolution, macroexpansion or data readers.
Other reader syntax (metadata, quote, syntax quote, regex, anonymous functions,
namespaced maps, tagged literals) is opaque syntax, preserved without execution.

## 2. Structural anchors and body grammar

Top-level owner kinds: `def`, `defn`, `defn-`, `deftest`, `ns`. Match the actual
list head and complete name symbol, case-sensitively; never prefixes or text hits.
Recognize `clojure.core/def[n][-]`, `clojure.core/ns`, `clojure.test/deftest` as
canonical equivalents of the corresponding unqualified heads; no alias inference.
Metadata around the list/name is included in its source span. `defmethod`,
`declare`, arbitrary defining macros and wrapped definitions are unsupported owners.
Only root children qualify. Never descend into `(comment ...)`, discarded forms,
quoted forms, strings, or other owners to discover an owner. A `(comment ...)`
root form still belongs to the preservation inventory.
`position` is exactly `before` or `after`; it selects the adjacent sibling gap.

A body anchor replaces the top-level anchor with:

```edn
{:scope "body" :owner {:kind "deftest" :name "roundtrip"}
 :expect 1 :testing_path [{:label "outer" :expect 1}
                        {:label "inner" :expect 1}]
 :boundary {:position "after-child" :child 2}}
```

`testing_path` is optional, default `[]`; each segment selects one DIRECT body
child whose head is `testing` or `clojure.test/testing` and whose first argument
is a string literal with the decoded value `label`. No substring matching.
Descend into that block's body, then resolve the next segment. Equal labels under
different parents are distinct; repeated equal labels in one parent refuse.
No recursive search, alias inference, computed labels, or traversal through `let`.
`boundary` is `{:position "first"}`, `{:position "last"}`, or
`{:position "after-child" :child N}`; N is 1-based and must be <= body child count.
First/last are legal for an empty body; after-child is not. `child` is forbidden
on first/last. "Top-level child" here means immediate child of the selected body,
not a recursive descendant. A nested `testing` expression counts as ONE child.
Comments, whitespace, commas and reader-discard nodes count as zero children.

`deftest` body starts after its name; `testing` body starts after its label.
For `defn`/`defn-`, exclude optional docstring, optional attribute map, parameter
vector, and optional pre/post map (the initial map when another body form follows).
Single-arity body starts after those headers. Multi-arity definitions require
`:arity N` on the body anchor, 1-based in source order; select that arity's body.
Omitted arity on multi-arity is `:anchor-ambiguous`; arity on single-arity or deftest
is `:invalid-request`; out-of-range is `:anchor-index-out-of-range`.
Exclude the multi-arity definition's trailing attribute map. Malformed recognized
headers refuse `:unsupported-owner-shape`. Body selection on `def`/`ns` refuses.

```edn
;; Body anchor examples, replacing only :anchor in the complete request above:
{:scope "body" :owner {:kind "defn" :name "compute"}
 :expect 1 :boundary {:position "first"}}
{:scope "body" :owner {:kind "defn" :name "compute"}
 :expect 1 :arity 2 :boundary {:position "last"}}
{:scope "body" :owner {:kind "deftest" :name "roundtrip"}
 :expect 1 :boundary {:position "after-child" :child 1}}
```

Zero owner/label matches: `:anchor-not-found`; >1 actual matches:
`:anchor-multiple-matches`. `:anchor-ambiguous` means missing disambiguating
structure, not an excuse to pick the first match. Counts are checked at EACH
selection step before resolving any boundary. Non-code lookalikes never count.

## 3. Payload, trivia, and the exact splice

Read ALL payload syntax to EOF. It must contain exactly `payload.forms` effective
root forms; whitespace and semicolon comments do not count. Unbalanced delimiters,
invalid tokens or trailing malformed syntax yield `:payload-parse-error`.
A different count, including zero, yields `:payload-form-count-mismatch`.
Reader-discard syntax in payload refuses `:unsupported-payload-syntax`: accepting
an invisible extra expression would undermine the count. Quoted and tagged forms
count as one each. Comments-only payload is a count mismatch, never a no-op.
Preserve leading, interior and trailing comments, all blank lines, internal spaces,
commas and literal spelling. Existing reader-discard syntax is preserved as trivia
for boundary purposes and as a separate span in the preservation inventory.

Plan one insertion offset p between source bytes. No original byte is deleted.
Find L, the preceding body child (or last header token for first/empty body), and
R, the following child (or container closing delimiter). Top-level before/after
uses the preceding/following root forms; virtual L at BOF has offset zero.
Start p at L's exclusive end, advancing past an attached trailing semicolon comment but leaving its newline in the gap. A top-level insertion reproduces the named anchor's existing inter-form gap on both sides of the payload (one blank line stays one blank line); a trailing comment stays attached to its original form and the gap rule applies after it. A body insertion uses the body's existing separator: newline plus sibling indentation, with blank lines only where the body already uses them. Advance p over the whitespace gap before the following sibling or closer, reusing its newline and indentation before the payload and reproducing the separator after the payload; no original byte is deleted. Never orphan a closer at column zero for last or empty bodies; indent a closer following a payload newline or trailing comment. At BOF insert before leading trivia, with no prefix; standalone comments remain before their original following form and payload comments belong to the payload.

Choose target column C (zero-based, Unicode codepoints; tabs in indentation refuse):
top-level = named anchor's opening column; body = first existing body's child
opening column; empty body = selected container's opening column + 2.
This is the anchor column returned in the receipt, not a whole-file style inference.
Find minimum leading-space indentation M of nonblank payload lines whose line start
is outside a multiline string/regex literal. On those lines only, replace M leading
spaces by C spaces. Blank lines and literal continuation lines stay byte-identical.
A tab in any indentation being measured or changed refuses `:unsupported-indentation`.
Preserve relative indentation; do not align arguments or rewrite tokens. Normalize
payload newline tokens outside literals to the source newline style (LF if none).
Never normalize a newline inside a literal. Reparse the resulting payload and assert
unchanged form count and unchanged nontrivia token spellings.

Added bytes S = the separator chosen above + reindented payload + only the missing right separator. Consumed gap bytes supply the left separator; at EOF add a final source-style newline unless the payload already ends with one. Source result B = A[0:p] + S + A[p:]; reparse B completely.
Verify inserted root/body siblings occupy exactly the requested boundary in B;
a swallowed form or altered token boundary refuses `:candidate-structure-mismatch`.
Source parse errors refuse `:source-parse-error`; candidate parse errors refuse
`:candidate-parse-error`. Never invoke a formatter, including a managed-write formatter.

## 4. Snapshot and write protocol

Exactly one guard alternative is required:

```edn
{:sha256 "<SHA-256 of complete original file bytes>"}
;; Portable receipt projection from a prior read, not an opaque server-session ID:
{:read_receipt {:version 1 :read_complete true
                :workspace_root "/absolute/canonical/project"
                :file "src/example.clj" :sha256 "<same 64 hex digest>"}}
```

The read-receipt schema above is a NEW portable projection required of supporting
read entrances; no claim that today's inspect receipt already supplies it.
It carries no stronger authority than the digest. Wrong path/root, false completion,
missing hash, opaque legacy receipt or both alternatives: `:invalid-guard`.
A native read plus SHA-256 suffices; no mandatory Surgeon discovery round trip.
Compare against the exact bytes read before parsing; mismatch is `:source-hash-mismatch`.
Do not silently substitute a newer digest into a mutation retry.

Hold the existing workspace write lock through capture, planning and commit. Stage
and validate future bytes, recheck target identity and full digest immediately before
atomic replacement, then read back and hash. A concurrent change detected before
replacement refuses `:source-changed-before-commit` and preserves that newer file.
Use the existing guarded transaction/journal; a hash recheck is NOT filesystem CAS
against arbitrary noncooperating writers. Do not claim protection from an external
write after the final check; report `:concurrency "cooperative-lock+final-recheck"`.
Post-replacement trouble is a failure, never a no-write refusal. Restore only if the
current file still equals this transaction's candidate; otherwise require recovery.

## 5. Receipt (mutation status first, no implied behavioral proof)

Serializers render `state`, `committed`, `mutation_attempted`, `source_unchanged`
first, in that order; map consumers MUST inspect values rather than key order.
`committed` means candidate is the verified final source, not a Git commit.
Successful payload-free summary example for this complete fixture:

```clojure
(defn a [] 1)
(defn ab [] 2)
```

Apply §1 with the real fixture hash below, payload `(defn b [] 3)`, and file/root
rebound to the owned fixture. Generated hashes and counts here are exact:

```edn
{:state "committed" :committed true :mutation_attempted true :source_unchanged false
 :ok true :operation "insert_forms" :version 1 :file "src/example.clj"
 :source_hash "81f14e0bae64ca76014a2c2368a7ca83dedb374ef244f8c5019b22ab61eb2b12"
 :result_hash "42941c271c531c0c058a984996826c4b65c2cec5d355b09965d8f03b409f0454"
 :read_back_hashes {"src/example.clj" "42941c271c531c0c058a984996826c4b65c2cec5d355b09965d8f03b409f0454"}
 :bytes_added 14 :forms_inserted 1 :anchor_column 0
 :splice {:offset 14 :length 14 :sha256 "b30dac5980dce85bdbf4130d40cdb14f8f4acc4640cfaacbe9eeb5b24dc6e5b4"}
 :line_range {:start 2 :end 2}
 :inserted_form_ranges [{:ordinal 1 :start_line 2 :end_line 2}]
 :preservation {:prefix_sha256 "9f7f8b28df3ae36aea970ba670a8939a2cd35620f77ce6cdb5723fb221c9a948"
                :suffix_sha256 "83f9249d855af8169bc768f86b07677f3ff636f1b477ed7399bd06511fbe7a7f"
                :other_forms_checked 2 :other_forms_unchanged true}
 :write_verified true :verification_complete false
 :verification {:tier "parse+byte-preservation" :behavior "not-run"}
 :concurrency "cooperative-lock+final-recheck"
 :next_action "none"}
```

Byte offsets are zero-based, lengths count UTF-8 bytes. Line numbers are 1-based;
`line_range` covers ALL inserted bytes in B, including separator newlines: the line
of B[p] through the line of B[p+length-1]. A newline belongs to its preceding line.
`inserted_form_ranges` excludes surrounding payload comments/trivia; ordinal is
payload order. Return these ranges in the detailed evidence if summary exceeds 4 KiB.
`bytes_added` includes separators and indentation; result length minus source length
MUST equal it. Proof prefix/suffix hashes are computed independently before/after.

Persist a receipt detail artifact through the existing receipt store, with path and
SHA-256 in `receipt_details_path` and `receipt_hash` on the summary (omitted only in
the illustrative map above). Detail includes full request, resolved owner/body path,
source/result byte counts, splice, and ordered per-form preservation entries:
`{:before_index 1 :after_index 1 :before_sha256 "..." :after_sha256 "..."}`.
Indexes are 1-based CST root expression ordinals including discarded expression nodes;
comments/gaps have their own spans. Include ALL other forms, even unnamed and duplicate
ones, plus the edited owner's before/after digest for body insertion. Never key by name.
The full inventory can exceed 4 KiB; the inline summary cannot. Before publication,
refuse `:limit-exceeded` at `[:receipt]` if required projected inline facts exceed
3,200 UTF-8 bytes, reserving the remainder for outcome diagnostics and timing. Include inverse splice
evidence in the durable receipt; no hidden source-file writes beyond the target.

`next_action="none"` discharges this verb's write, parse and preservation checks.
Caller-required compilation/tests/review remain caller work. V1 runs no verification
profile, leaves `verification_complete=false`, and ALWAYS OMITS `terminal_response`.
It MUST NOT manufacture an exact-success message from preservation hashes.
The seat's current `MCP-OP-RELAY-001..004` only admits
`Done — changes committed and exact verification completed.` after the normalized
project-owned exact-exit pass, verified read-back and inverse-receipt evidence.
Adding that gate/projector to this verb requires a separately reviewed evidence law;
profile/request fields are refused v1. If a later admitted version returns the field,
relay it EXACTLY only when all remaining user work is complete; otherwise continue.

## 6. Refusals and failures

```edn
{:state "refused" :committed false :mutation_attempted false :source_unchanged true
 :ok false :operation "insert_forms" :error-type :anchor-multiple-matches
 :error "Expected one top-level defn named a; found two."
 :at [:anchor :owner] :expected 1 :actual 2
 :candidates [{:kind "defn" :name "a" :line 1}
              {:kind "defn" :name "a" :line 8}]
 :next_action "revise-request"
 :remedy "Choose a uniquely named supported owner; duplicates cannot be selected by line."}
```

All validation refusals use that envelope; diagnostic keys vary by reason. `at` is
an EDN key/index path. Count refusals carry expected/actual; parse refusals line/column;
stale refusals expected/actual hashes; index refusals child count or arity count.
Cap candidates at 10 with total count and `candidates_truncated` boolean.
For stale reasons, `next_action="refresh-source"`; for malformed requests/payloads,
anchors, bounds, path/encoding/newline/syntax problems, `next_action="revise-request"`.
Path/encoding/newline and unsupported source-reader syntax use `:unsupported-source`
or `:invalid-path` as appropriate; all other schema errors use `:invalid-request`.
Receipt persistence/staging I/O failure before replacement: `state="failed"`,
`:error-type :io-error`, no mutation attempted, source unchanged, `next_action="retry-after-repair"`.
A restored post-write failure has `state="rolled-back"`, committed false,
mutation_attempted true, source_unchanged true, `next_action="retry-after-repair"`.
Unproved post-write state has `state="recovery-required"`, committed/source_unchanged
nil, mutation_attempted true, `next_action="recover"`, `:error-type :commit-outcome-unknown`,
and durable receipt location. Never blindly replay. No failure has terminal_response.
Do not fabricate an executable `next_call` requiring user intent; remedy names the
missing decision. This deliberately overrides the old universal-next_call convention.

## 7. Independent preservation oracle

Verifier takes independently captured A, B, request and receipt; verifies their whole
hashes first. With a separately authored CST walk (no production selector/splice/hash
helper), resolve the anchor, payload count, body grammar, boundary and p from A.
Check B=A[0:p]+S+A[p:] and SHA-256 of both preserved slices. Reparse B independently.
Inventory raw root spans, including metadata, by ordinal. Top-level insertion:
remove the N inserted spans from B's inventory and require every original form hash,
including the anchor's, identical and in order. Body insertion: every OTHER root form
must be byte-identical; removing S from the edited owner must recover its exact bytes.
Check all original trivia spans survive in order; a permitted split gap concatenates
to its original bytes. Independently verify each inserted form/token and boundary,
so a receipt cannot make a misplaced insertion pass by merely naming that position.
Check UTF-8 counts/ranges. An independently authored walk may share rewrite-clj as
parser; record that correlated-parser limitation. Run it red against deliberate
neighbor reformatting, wrong-body insertion, deleted comment and forged digest.
This is preservation/placement acceptance, not proof of application behavior.

## 8. Witnesses to write RED before implementation

Names below are literal required `deftest` names; use table-driven pure fixtures for
variants. Every refusal witness asserts exact error type AND unchanged target bytes.

- `insert-forms-exact-root-anchor` — a versus ab; before/after variants.
- `insert-forms-body-anchor` — outer/inner path, sibling repeated labels.
- `insert-forms-parse-stages` — extra close, missing close, malformed tail.
- `insert-forms-snapshot-guard` — one changed comment byte is sufficient.
- `insert-forms-exact-root-anchor` — line comment, string, comment macro,
  quote and #_ decoys; real anchor selected, decoys-only yields not-found.
- `insert-forms-exact-root-anchor` — absent, duplicated owner, repeated testing label.
- `insert-forms-body-anchor` — first/last/N, empty, N=0/out-of-range, nested counts.
- `insert-forms-body-anchor` — metadata/doc/attrs/prepost/multi-arity.
- `insert-forms-payload-count-and-order` — one/many/zero, comments-only, discard refusal.
- `insert-forms-layout-literal-preservation` — trailing/standalone/payload comments, BOF/EOF,
  no final newline, empty body, blank lines, CRLF, tabs, Unicode, multiline literals.
- `bounded-input-path-encoding` — no eval/data-reader execution; all bounds.
- `insert-forms-snapshot-guard` — valid projection, wrong root/path, false/missing.
- `insert-forms-other-top-level-hashes` — independent oracle plus deliberate corruptions.
- `insert-forms-publication-evidence` — injected staging/recheck/read-back failures,
  restored failure and intervening external write requiring recovery (filesystem test).
- `bounded-input-path-encoding` — traversal/symlink/hardlink/missing (filesystem test).
- `insert-forms-publication-evidence` — golden §5, detail inventory, bounded summary.
- `insert-forms-publication-evidence` — no v1 result emits exact-success text.
- `insert-forms-cli-mcp-parity` — actual request-file invocation, shorthand and :op,
  global/op help, EDN-only stdout, all exit classes, malformed/trailing EDN (boundary test).

First five are census-derived risk witnesses, not claims of five replayed incidents.
Freeze one minimized real defn and one nested-testing fixture with source commit/path
from the census episodes before builder acceptance; no invented provenance. Successful
candidates must independently parse and pass proportionate lint/load/focused tests;
lint through `~/bin/clj-kondo`. Do not format neighbors to satisfy a formatting gate.

## 9. CLI stdout and review disposition

X must contain exactly one EDN map plus optional comments/whitespace; no tagged EDN,
reader-eval, duplicate keys or trailing value. No shell-evaluated request expressions.
Exit 0 ONLY for committed/write_verified success. Exit 2 for no-write validation
refusal; exit 1 for I/O, rolled-back, internal or recovery-required failure.
Stdout is exactly one EDN receipt map plus newline for every handled outcome, no
banners/progress/stack traces; diagnostics go to stderr. Uncatchable termination may
produce no complete receipt: exit alone then never establishes no write. MCP returns
the equivalent map as structuredContent and a state-first summary; domain refusals
are ordinary tool results, not transport errors. `elapsed_ms` is measured, never fixed.

Self-review: resolved prefix selection, trivia ownership, body/header counting, portable
guards, line accounting and terminal eligibility against the documented trunk rules.
Three least-certain choices, nevertheless binding for v1: (1) gap trivia stays right,
including BOF header comments; (2) no verification profiles/terminal_response yet;
(3) refuse aliased testing/deftest heads rather than infer require bindings. Validate
these in the first caller trial; any revision updates contract and red witnesses first.

Evidence: records `docs/observations/2026-09-10-astra-ethnography.md` §4 and
`2026-09-10-ethnography-program-edits.md` (628 defn insert/edit and 1,920 test-body
calls are mixed intent classes, not insertion-only counts); `/var/tmp/forge/ethno/4h/
e4-matched-arms.md` demonstrates the need for independent preservation evidence.
Terminal law: trunk `docs/intent/mcp-operation-contract/mcp-operation-contract-design.md`
“Exact Terminal Response” and `src/clj_surgeon/mcp_tool.clj` `exact-terminal-response`.
No code or speed claim follows from this document; builder review/implementation gates remain.

### clj-splice port (2026-09-11)

Nested and literal intervals now come from clj-splice, with Surgeon retaining owner, body, header and layout policy. Mixed LF/CRLF is admitted: inserted separators use the first source newline (LF if absent), and every original byte is preserved. CRLF inside literals remains literal data. Bare CR, BOM and measured tab indentation admission remain deferred. The bounded lexical/shape preflight remains until rewrite-clj provides a parser-level resource budget; spans supplies literal discovery for indentation.
