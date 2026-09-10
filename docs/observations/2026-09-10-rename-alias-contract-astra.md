# rename_alias v1 — reviewed consult contract

Status: proposed builder contract; self-reviewed against trunk
`bd124492bb557377afe94065ab58149110f21608`. Recorded: 2026-09-10T21:40:22.631067+00:00.
Consult only: no implementation, executed tests, independent approval or performance
admission claimed. MUST/REFUSE are normative. Gene's order is insertion first, then
alias rename, both through the request-file entrance.

## 1. One verb, inherited envelope, closed request

MCP `rename_alias` and CLI `clj-surgeon :rename-alias! :request-file X` MUST call
one pure planner, guarded transaction and receipt projector. Also accept
`clj-surgeon :op :rename-alias! :request-file X`. CLI needs no server. These are
new entrances, not claims about trunk. No preview, multiple-intent batch, implicit
retry, alias policy, verification profile or formatter in v1.
Reuse `2026-09-10-insert-forms-contract-astra.md` §§1,4–6,9: versioned envelope,
portable guards, state-first receipts, durable detail, refusal/recovery states,
stdout/exits and terminal eligibility. Specializations below are normative.

```edn
{:version 1 :workspace_root "/absolute/canonical/project"
 :scope {:file "src/example.clj" :expect_files 1}
 :lib "curtaincall.events" :old_alias "events" :new_alias "ev"
 :expect {:references {:per_file {"src/example.clj" 2}}}
 :guards {"src/example.clj" {:sha256 "<64 lowercase hex digits>"}}}
;; Other complete :scope values:
{:paths ["src/a.clj" "test/a_test.clj"] :expect_files 2}
{:repository true :expect_files 124}
;; Alternative complete :expect value:
{:references {:total 7}}
```

All top-level fields shown are required. Closed schema: unknown/duplicate keys,
wrong types and nonintegral counts refuse `:invalid-request`. EDN keys are keywords;
names, paths and enums are strings. MCP JSON has identical string keys/values.
The insertion envelope's `:error-type` keyword remains hyphenated (JSON key
`"error-type"`); within/from-to diagnostic field names remain underscore names.
Exactly one scope selector: `file`, nonempty `paths` of explicit FILES (no directory
or glob), or `repository true`. expect_files is positive and counts ALL inspected
files, not changed files. Reject duplicate paths; normalize to root-relative slash
paths and sort lexically. Repository recursively enumerates `.clj`, `.cljs`, `.cljc`,
excluding only `.git` directories; includes ignored/untracked files, no deps/Git
inference. Unsupported discovered extensions refuse; no silent CLJC exclusion.

Explicit scopes require the lib+old_alias binding in EVERY file. Repository scope
selects exact matching bindings; other valid files are skipped unchanged with zero
references. Zero selected files refuses `:old-alias-absent`. A same-spelled alias
for another library is never renamed. All inspected files must parse and have a
supported ns, including skips. This conservative repository scope is intentional.
Exactly one reference assertion: nonnegative integer total OR per_file covering
EVERY inspected file, zeros for skips. Binding tokens do NOT count as references;
zero references permits renaming an unused alias. Guards cover exactly inspected
paths. File-count mismatch precedes guard coverage validation and returns complete
path inventory plus expected/actual. Repeat enumeration before commit; drift refuses.

lib must be a single namespace symbol; aliases must be distinct simple unqualified
symbols valid as alias prefixes. Reject whitespace, slash, colon, reader punctuation,
reserved nil/true/false/&/_, and trailing tokens. Safe token parsing must validate
and round-trip old/x, ::old/x, new/x and ::new/x. old=new is :invalid-request.
If a simple lib equals old_alias, refuse `:ambiguous-alias-namespace`: fully
qualified namespace spellings and alias spellings cannot then be distinguished.

Reuse insertion byte/path rules: canonical existing root, confined regular files,
no traversal, symlink components, hardlinks >1, BOM, invalid UTF-8 or mixed newlines.
Preserve permissions, LF/uniform CRLF, tabs and trivia. No source creation/deletion
or Git commit. Limits: request 2 MiB, source 8 MiB/file, CST depth 512; additionally
1,000 inspected files, 64 MiB aggregate source, 100,000 references. Bound before
allocation/walk where possible; `:limit-exceeded`, never truncated planning.

## 2. Alias references: syntactic roles, not substrings

Use a lossless reader/CST and static alias table from ns. Recognize reader roles
without evaluation, namespace loading, macroexpansion or data-reader execution.
Host *ns* must not influence keywords or syntax quote. This is syntactic resolution,
not executing Clojure read/eval or consulting runtime alias state.
An effective, non-discarded occurrence counts once when it is:

- A SYMBOL with namespace part EXACTLY old_alias: events/day-hours, #'events/x,
  symbols in calls, values, binding positions, metadata, quote, syntax quote/unquote,
  and `(comment ...)` bodies. Change only the namespace part, retaining slash/name.
  Prefix trap: old ev does not match event/x. No per-Var enumeration is required.
- An auto-resolved KEYWORD ::events/kw, including metadata/destructuring forms such
  as ::events/keys. Preserve :: and local name. Literal :events/kw is NOT alias
  resolution and stays byte-identical; ::kw also stays unchanged.
- The alias PREFIX of #::events{...}, counted ONCE regardless of implicit keys.
  Explicit references in its contents count independently. Preserve implicit key
  spellings and literal #:events{...} prefixes.

The ns libspec alias token is one separate BINDING edit per selected file. Library
names, import declarations, :refer/:rename declaration names and tagged-literal TAG
names are not reference roles. Preserve #events/tag's tag name; traverse its payload
normally without running a data reader. Other aliases and fully qualified library
symbols stay unchanged. No bare events, runtime string or computed symbol is renamed.
Strings/docstrings, semicolon comments, regex and character literals are opaque,
byte-identical, zero references. `(comment ...)` is code syntax, not a comment token.

#_ and its ENTIRE discarded subtree (including nested discards/metadata) are opaque,
byte-identical, zero references. Finding events/x or ::events/x there does NOT refuse.
Re-enabling discarded code may need a later rename. Malformed discarded syntax still
refuses parsing. Global v1 reader-eval/reader-conditional restrictions apply even in
discards, but never to lookalike text inside strings/comments/regex.
Quoted symbols intentionally count even though literal data spelling changes: this
contract is syntactic alias renaming, not general semantic equivalence. Sites expose
context "quote", "syntax-quote", "metadata" or "ordinary"; nearest enclosing context
wins. Unquote returns to its enclosing non-syntax-quote context for this label.

## 3. Eligibility, collisions and typed refusals

Exactly one effective root ns/clojure.core/ns form, allowing metadata, must be the
first effective root expression. Comments/discards do not count. Missing ns =>
`:ns-not-found`; multiple root ns => `:multiple-ns-forms`; misplaced/malformed ns =>
`:unsupported-ns-shape`. Quoted/discarded/comment-macro lookalikes never select ns.
Support standard ns name/doc/attr header and flat :require symbol/vector libspecs.
Target has exactly one :as old OR :as-alias old; preserve the option keyword.
Accept unchanged :refer :all or simple-symbol vectors, :rename maps of simple
symbols, and :exclude simple-symbol vectors. Duplicate options, qualified declaration
names, malformed values, :rename keys outside explicit :refer => `:unsupported-libspec`.
For :refer :all, :rename keys may be any simple symbols; no library loading to check.
Accepted declarations and all bare referred uses stay byte-identical. A referred/local
name equal to new_alias is NOT an alias-table collision.

Prefix-list requires, :use, unknown libspec options and shapes preventing a complete
alias table refuse :unsupported-ns-shape. Standard :import/:refer-clojure/:gen-class
clauses remain unchanged. :load refuses :unsupported-ns-shape because it can change
namespace bindings. Do not infer alias state from executable namespace setup.
Duplicate libraries/aliases in selected ns => `:ambiguous-alias-binding`, even when
bound to the same library. new_alias already bound by :as OR :as-alias =>
`:alias-collision`, even for the target library. Return aliases/libs and binding sites.
Effective new_alias/x, ::new_alias/x or #::new_alias{} without a binding =>
`:new-alias-capture`; introducing an alias could retarget existing syntax. Literal
keywords, tag names and discards do not trigger capture. Explicit-scope missing old
=> :old-alias-absent; old bound to another lib => `:alias-library-mismatch`.

Refuse effective core in-ns/alias/ns-unalias/runtime require forms outside ns:
`:unsupported-namespace-mutation`. Recognize unqualified/clojure.core heads outside
ordinary quote; this is a conservative syntactic tripwire. Computed/macro-hidden
namespace mutation and runtime alias manipulation are outside the guarantee.
.cljs/.cljc, #?/#?@ and reader-eval refuse `:unsupported-source` before any write;
no platform branch selection in v1. Parse failure => `:source-parse-error` with
file/line/column. Path/encoding/limit/schema errors reuse insertion types/remedies.

Deterministic validation order: schema/path/limits; scope cardinality; guard coverage/
hashes; source safety/parse/ns shape; binding selection; collision/capture; reference
counts; candidate validation; final stale checks; commit. Sort by file/preorder within
stages. First failure returns full relevant evidence; all files pass before replacement.
All refusals include insertion's :at EDN key/index path and remedy. Scope cardinality
uses expected/actual, stale errors expected/actual hashes, parse errors line/column.
Reference cardinality uses the existing within/from-to count fields below.

## 4. Guards, counts, addresses and splices

Each guards value uses EXACTLY insertion's union, no extra write authority:

```edn
{:sha256 "<complete original bytes SHA-256>"}
{:read_receipt {:version 1 :read_complete true
                :workspace_root "/absolute/canonical/project"
                :file "src/example.clj" :sha256 "<same digest>"}}
```

Portable receipt is insertion's NEW projection, not today's opaque inspect receipt.
Native hashes suffice. Wrong identity, missing/false completion or both alternatives
=> `:invalid-guard`; stale hash => `:source-hash-mismatch` with expected/actual hashes,
next_action="refresh-source". Returned fresh hashes never authorize automatic retry.
Reference count mismatch => `:expect-count-mismatch`, expected_count, actual_count,
per_file_counts and write_refusal_evidence with snapshot_guards, selector_sha256 and
true items, as within+from/to does today. For per_file assertions expected_count and
actual_count are maps; also supply mismatched_files. For total they are integers.
Items enumerate ALL true references, never binding/literal decoys; detail is complete.
For scope cardinality use `:scope-file-count-mismatch` with expected/actual and paths.

Selector SHA reuses mcp-write-refusal canonical EDN hashing of
`{:files sorted-paths :scope {:kind :root}
  :matcher {:operation "rename_alias" :version 1 :lib L :old_alias O :new_alias N}
  :expectation reference-expectation}`. Both transports use the same implementation.
Sites are original-source 1-based line/end_line, UTF-8 zero-based offset/byte length,
`:address {:preorder N}` and :form_index. Preorder is zero-based trunk
intent-transaction z/of-string/z/next walk, excluding trivia as that walk does, NOT
a reference-only ordinal; traverse/address discards even though selection skips them.
Keyword/map prefix sites address their CST node; byte offset distinguishes spans.
form_index is insertion's 1-based raw root expression ordinal (including discards,
metadata belongs to its expression). Role is binding/symbol/auto-keyword/auto-map.

Plan disjoint binding-token/alias-prefix replacements; apply descending byte offset.
No reprinting/formatting. Reparse B, independently recompute binding/reference roles,
require exactly the planned new roles/counts, zero old effective references, unchanged
suffixes and exact bytes outside authorized intervals. Candidate failures are
`:candidate-parse-error` or `:candidate-structure-mismatch`. Durable inverse evidence
records original/result offsets, before/after bytes and hashes of each splice.

## 5. Atomicity and mutation-first receipt

Hold workspace lock through capture, planning, staging, commit/read-back. Reuse
insertion's guarded journal. Stage ALL candidates and durable inverse records before
ANY replacement. Recheck every inspected identity/digest and repository membership;
then recheck each target immediately before its atomic replacement. Read back every
replacement; verify the complete inspected final snapshot. Commit only if all selected
files equal candidates and all skips equal originals. Journal progress durably.
All-or-nothing SUCCESS with rollback is not multi-path atomic visibility: other readers
may see intermediate writes. Report concurrency="cooperative-lock+final-recheck";
no filesystem CAS protection against arbitrary external writers.
Before first replacement, staleness => `:source-changed-before-commit` or
`:scope-changed-before-commit`, no-write refusal. After any replacement ATTEMPT,
failure is never a no-write refusal. Restore only files still equal to this transaction's
candidates; never overwrite an intervening writer. Verify every restoration.

Serialize state, committed, mutation_attempted, source_unchanged FIRST, in that order.
Insertion states/values are binding: refused=false/false/true; pre-write I/O failed=
false/false/true; verified rolled-back=false/true/true; recovery-required=nil/true/nil,
error-type=:commit-outcome-unknown. No-write source_unchanged means unchanged BY THIS
REQUEST; rolled-back true additionally proves restored originals. Never report a
partially applied plan committed. next_action is revise-request, refresh-source,
retry-after-repair or recover as insertion specifies; include a remedy, never invent
an executable next_call requiring missing intent. No failure has terminal_response.

After staging starts, include :transaction with partial_write (boolean, nil if unknown),
replaced_files, restored_files, pending_files, unresolved_files and file_states. Lists
are paths; file_states maps every inspected path to {:state S :observed_sha256 H},
where S is original/candidate/external/unknown, H omitted if unreadable. Lists record
verified replacements/restorations, never guesses about a throwing system call.
partial_write=true means a known proper subset of selected files remains candidate;
all committed/all restored => false. False does not rule out recovery-required.
All no-write refusals may omit transaction; mutation_attempted becomes true on the
first replacement attempt. Uncatchable process death requires journal recovery.

Durable receipt detail via existing store is mandatory; summary has
receipt_details_path and receipt_hash. Before replacement, persistence/staging failure
is :io-error, failed/no-write, retry-after-repair. After replacement, follow rollback/
recovery law; durable journal must locate recovery if final receipt persistence fails.
Detail includes normalized request, inspected/selected/skipped paths, complete sites,
source/result/read-back hashes/sizes, inverse_splices and this per-file shape:

```edn
{:file "src/example.clj" :references_changed 2 :bindings_changed 1 :forms_changed 3
 :source_hash "<A hash>" :result_hash "<B hash>" :read_back_hash "<B hash>"
 :changed_forms [{:before_index 1 :after_index 1
                  :before_sha256 "<original ns hash>" :after_sha256 "<new ns hash>"}]
 :preservation {:other_forms_checked 32 :other_forms_unchanged true
                :other_forms [{:before_index 2 :after_index 2
                               :before_sha256 "<H>" :after_sha256 "<H>"}]
                :gaps_unchanged true :discards_unchanged true}}
```

Lists above illustrate ENTRY shapes, not complete E4 inventories: include EVERY
changed and OTHER form, named/unnamed/duplicate, by ordinal; never key by name.
Include equal before/after digests for every gap/comment/discard span. Reversing only
authorized splices must recover each edited form exactly. Include both binding and
reference sites with role/context/form_index/line/address/byte span in detail.
Summary <=4 KiB, payload-free; full evidence is durable. Inline site/candidate prefixes
cap at 10 AND byte budget, with available_count/returned_count/omitted_count/truncated;
candidate lists also have candidates_truncated. Do not truncate true actual counts.
Large per-file maps move to detail together; details_contains names omitted sections.
Always retain state, next_action, aggregate counts and detail path/hash inline.
Refusal evidence has authority=false and write_authority=false.

Success: write_verified=true, verification_complete=false,
verification={:tier "parse+byte-preservation" :behavior "not-run"}, next_action="none".
V1 ALWAYS OMITS terminal_response, as insertion does, and rejects profile fields.
Caller tests/review remain. A later admitted version needs the project-owned exact-exit
pass/read-back/inverse evidence gate; then relay exact text only if ALL user work is done.

## 6. E4 request and response examples

Freeze VERBATIM the `00e8f0fa` blob from `/home/forge/src/curtaincall-cfp`, path
`src/cfp_scheduler_killer/views/schedule.clj`; do not minimize. The actual library is
cfp-scheduler-killer.events, not the brief's curtaincall.events shorthand. Original
hash below was independently checked against that Git blob during this consult.
Bind root to an owned fixture containing that exact path and source.

```edn
{:version 1 :workspace_root "/var/tmp/forge/insert-fx/alias-e4"
 :scope {:file "src/cfp_scheduler_killer/views/schedule.clj" :expect_files 1}
 :lib "cfp-scheduler-killer.events" :old_alias "events" :new_alias "ev"
 :expect {:references {:total 2}}
 :guards {"src/cfp_scheduler_killer/views/schedule.clj"
          {:sha256 "5f086f7789fdb556ba6e819b26d6eba8ec0345e22b7aaa152b2e1a03648234a1"}}}
```

These response maps are SCHEMA examples, not executions of the new verb. Angle-bracket
values are computed by the builder. Mandatory detail/transaction fields are defined
above. E4 result hash and reference addresses come from retained oracle/sm2 evidence.

```edn
{:state "committed" :committed true :mutation_attempted true :source_unchanged false
 :ok true :version 1 :operation "rename_alias"
 :files_inspected 1 :files_changed 1 :bindings_changed 1 :references_changed 2
 :forms_changed 3 :other_forms_checked 32 :other_forms_unchanged true
 :read_back_hashes {"src/cfp_scheduler_killer/views/schedule.clj"
                    "02332a74caf1ead4d70c555b530230b8b03aebc306bd72f5d129f111c876da0c"}
 :transaction {:partial_write false
               :replaced_files ["src/cfp_scheduler_killer/views/schedule.clj"]
               :restored_files [] :pending_files [] :unresolved_files []
               :file_states {"src/cfp_scheduler_killer/views/schedule.clj"
                             {:state "candidate" :observed_sha256 "<result hash>"}}}
 :receipt_details_path "<durable absolute path>" :receipt_hash "<detail SHA-256>"
 :details_contains ["per_file" "sites" "preservation" "inverse_splices"]
 :write_verified true :verification_complete false
 :verification {:tier "parse+byte-preservation" :behavior "not-run"}
 :concurrency "cooperative-lock+final-recheck" :next_action "none"}
;; Same request with total 31: two true references, no mutation.
{:state "refused" :committed false :mutation_attempted false :source_unchanged true
 :ok false :version 1 :operation "rename_alias" :error-type :expect-count-mismatch
 :error "Expected 31 alias references; found 2." :at [:expect :references]
 :expected_count 31 :actual_count 2
 :per_file_counts {"src/cfp_scheduler_killer/views/schedule.clj" 2}
 :write_refusal_evidence
 {:version 1 :operation "rename_alias" :family "generic-count-mismatch"
  :failed_stage "intent-compilation" :authority false :write_authority false
  :subject {:selector_sha256 "<computed selector hash>"}
  :expected_count 31 :actual_count 2
  :per_file_counts {"src/cfp_scheduler_killer/views/schedule.clj" 2}
  :snapshot_guards {"src/cfp_scheduler_killer/views/schedule.clj"
                    "5f086f7789fdb556ba6e819b26d6eba8ec0345e22b7aaa152b2e1a03648234a1"}
  :items [{:file "src/cfp_scheduler_killer/views/schedule.clj" :scope_kind "root"
           :line 153 :end_line 153 :address {:preorder 537}}
          {:file "src/cfp_scheduler_killer/views/schedule.clj" :scope_kind "root"
           :line 1112 :end_line 1112 :address {:preorder 4661}}]
  :available_count 2 :returned_count 2 :omitted_count 0 :truncated false}
 :receipt_details_path "<durable absolute path>" :receipt_hash "<detail SHA-256>"
 :next_action "revise-request"
 :remedy "Review the two true sites; correct scope or count and resubmit with guards."}
```

X is exactly one EDN map plus optional whitespace/comments; reject tags, reader-eval,
duplicate keys/trailing values. Stdout exactly one EDN receipt plus newline; diagnostics
stderr. Exit 0 only committed/write_verified; 2 no-write validation refusal; 1 I/O,
internal, rollback or recovery failure. Uncatchable termination may yield no receipt;
exit alone proves no mutation status. MCP returns equivalent structuredContent and
state-first summary; domain refusals are ordinary results, not transport errors.
elapsed_ms is measured; parity excludes elapsed and generated artifact identities.

## 7. Relationship and RED-first witnesses

Keep require_change's add/remove/alias-policy contract WITHOUT symbol edits. E4's
plan_only refusal “Target library cannot also be removed” is a warranted boundary.
Keep alias_migration's from-lib/Var → to-lib/Var migration with per-file alias policy,
including its existing lib-only capability. Do not redirect/weaken admitted contracts.
rename_alias keeps the SAME library/local Var names and renames a fixed alias's
entire syntactic reference set. A require_change mode would reduce public tool count
and reuse ns discovery, but make require-only/body-editing promises and count/guard
semantics mode-dependent. COMMIT to Gene's separate verb; share pure parser/splice/
transaction/projector seams where compatible, never implement as remove+add emulation.

Write RED first; literal test names, mainly pure table-driven source fixtures.
Every refusal witness asserts exact error type AND all target bytes unchanged:
- `rename-alias-e4-verbatim`: exact blob, 35 forms, 3 changed lines/forms (ns line 4,
  agenda-days line 153, schedule-page line 1112), 2 references, 29 unchanged route
  literals/22 templates, exact result hash; 31→2 refusal then corrected success.
- `rename-alias-prefix-trap`: old ev, event/x untouched; arbitrary local Var names.
- `rename-alias-reader-roles`: keyword alias/literal keyword, syntax quote/unquote,
  ordinary/Var quote, metadata, destructuring, namespaced maps, tagged payload/tag.
- `rename-alias-string-decoy`: strings/docstrings/escaped/multiline, regex, character,
  semicolon comments untouched; comment-macro references renamed.
- `rename-alias-discard-decoy`: nested #_, discarded ns/symbol/keyword preserved,
  zero count; malformed discard and global unsupported syntax refuse.
- `rename-alias-binding-matrix`: collision/duplicate/capture/absent/wrong lib, :as-alias,
  :refer/:rename interactions, same-named locals, unused alias, no-op refusal.
- `rename-alias-scope-guards-counts`: all scopes, skips, counts, membership drift,
  complete true sites, hash/read-receipt guards and no automatic stale retry.
- `rename-alias-source-boundaries`: absent/multiple/misplaced ns, CLJC/conditionals,
  dynamic ns, unknown libspecs, UTF-8/CRLF/BOM/limits and path confinement.
- `rename-alias-transaction-faults`: stage/second-file replacement/read-back/restore/
  receipt failures, external writes and crash recovery; truthful per-file/partial status.
- `rename-alias-receipt-oracle`: form/gap hashes, addresses, bounded/full detail and
  terminal omission; corrupt routes/comments/neighbors, miss a site, forge a digest.
- `rename-alias-cli-mcp-parity`: actual request-file shorthand/:op, global/op help,
  parsing, stdout, exits, success/refusal/recovery against identical snapshots.

Independent oracle captures/hashes A and B itself, uses a separately authored CST
walk (no production selector/splice/hash helpers), verifies exact authorized intervals,
roles/counts, other forms/gaps/discards and reverse-splice identity. E4 additionally
checks whole string-literal multiset and exact two-use set. Record shared-parser
limitations. Run proportionate lint/load/tests during implementation (lint via
~/bin/clj-kondo) and required repository completion gates. Green lint/tests alone miss
E4's route corruption. Census 1,876 require/alias program edits and 29 confirmed broken
files are a MIXED class, not 1,876 pure alias renames. No speed/admission claim.

## Least sure

1. **Quoted symbols count; discards do not.** Literal data changes and re-enabling
   discards merit caller trials; this remains the binding v1 syntactic contract.
2. **Repository guards cover every inspected file; unsupported files refuse.** Strong
   closed-snapshot evidence costs request size/adoption. Changing this requires new
   scope/guard/race witnesses, not silent affected-only filtering.
3. **Separate verb rather than require_change mode.** Clear ownership/count semantics
   win here; caller evidence may later justify a unified public intent envelope.

Self-review checked insertion envelope/guards/terminal law, E4 raw receipt/oracle,
trunk require_change/alias_migration and intent_transaction address conventions.
Resolved binding/reference counts, literal/auto keywords, repository absence, transient
partial visibility and recovery truthfulness. Primary records under
`/home/forge/src/clj-surgeon-records/docs/observations`:
`2026-09-10-insert-forms-contract-astra.md`, `2026-09-10-e4-matched-arms.md`,
`2026-09-10-ethnography-program-edits.md`; raw sm2.out under /var/tmp/forge/ethno/4h.
Builder implementation/acceptance gates remain future work.
