# Astra fix round 1 — insert_forms v1

Ready for Sol. Branch `fable/insert-forms`, base `33d75cfc`, final tip `29f81c5f`.
Owned only `/home/forge/src/clj-surgeon-insert`; named-file commits, no push.
No server, formatter, forbidden-port operation, or landing-gate-prewarm was invoked.
Scratch and logs are under `/var/tmp/forge/insert-fx`; the repository's test-fast
runner also writes its standard ignored `target/gate-parallel/` evidence.
JVM invocations use `-J-Xmx1g` or the runner's smaller heap cap, with
`JAVA_TOOL_OPTIONS=-Xmx1g -Djava.io.tmpdir=/var/tmp/forge/insert-fx` and matching TMPDIR.
Lint used `~/bin/clj-kondo` exclusively.

## Commits

a75baf0a fix(insert-forms): witness candidate guards and compute preservation
6769b0b5 fix(insert-forms): reuse existing sibling separators
5e7c180f fix(insert-forms): explain refusals and identify candidates
908c266b fix(insert-forms): document and witness planned-receipt recovery
9f96547e test(insert-forms): remove redundant oracle binding
73c43729 test(insert-forms): enroll all six fix-round regressions
cd5e22f9 fix(insert-forms): preserve comment tails and inline body indentation
29f81c5f docs(insert-forms): record fix-round intent and verification contract

## F1 — computed preservation and guard witnesses

`other_forms_unchanged` now evaluates the per-form before/after hash comparisons;
it is never a literal in production. The existing structure guard remains mandatory.
Two identity-by-default dynamic seams permit offset and candidate-text fault
injection. Tests never edit production source. The structure witness perturbs the
splice one byte into the anchor and asserts `:candidate-structure-mismatch`, refused
state, and unchanged disk bytes. Source and candidate parse witnesses similarly
assert exact `:source-parse-error` / `:candidate-parse-error` and unchanged disk bytes.

Required RED: `fix1-red.log` contains the run with the structure guard temporarily
removed, after the new witnesses and computed preservation were installed:
2 tests / 9 assertions / **3 failures, 0 errors**. All three failures are the
structure witness: the mutant committed, had no refusal type, and changed the
file to the corrupt nested-defn result. Source/candidate parse witnesses still pass.
The guard was restored before subsequent work. `fix1-guard-green.log` records
2 tests / 9 assertions / 0 failures / 0 errors. Final complete suite also passes
these same witnesses after the spacing change.

During fixture development, splice locations inside a vector, map, and regex
prefix still parsed under rewrite-clj; they were not valid candidate-parse
witnesses. The retained candidate-text seam supplies an actually unbalanced
candidate to the real parser. No parser stub or production-code mutation is used
by the permanent witness. The temporary guard deletion was the requested manual
mutation experiment, restored from the exact saved current file.

## F2 — spacing and every changed old expectation

The single §3 p-rule paragraph now states Fable's gap rule. Its later S formula
was mechanically updated to remove the contradictory unconditional-newline rule.
Whitespace already in the source supplies the left separator; the inserted bytes
supply the right separator. Trailing anchor comments remain attached; standalone
comments remain before their original following form. Body last/empty closers stay
beside the last payload form, or are indented after a payload newline/comment.
No formatter. The oracle independently reconstructs the revised p and separators.

Every changed pre-existing expectation is listed below. Escapes denote literal
bytes. `fix1-spacing-expectations.diff` contains the exact old/new source diff.

| Witness/file suffix (`insert_forms_*_test.clj`) | Changed expectation |
|---|---|
| `after_prefix_named_defn`, census fixture | `:ok)\n\n(def untouched` → `:ok)\n(def untouched` |
| `after_prefix_named_defn`, golden after-a | `3)\n\n(defn ab` → `3)\n(defn ab` |
| `anchor_decoys`, line-comment variant | Same removal of one newline between b and ab; decoy prefix unchanged |
| `anchor_decoys`, string variant | Same removal between b and ab; string prefix unchanged |
| `anchor_decoys`, comment-macro variant | Same removal between b and ab; comment form unchanged |
| `anchor_decoys`, quote variant | Same removal between b and ab; quoted form unchanged |
| `anchor_decoys`, reader-discard variant | Same removal between b and ab; discarded form unchanged |
| `body_boundaries`, first | `(deftest t\n  3\n\n  1\n  2)` → `(deftest t\n  3\n  1\n  2)` |
| `body_boundaries`, last | `(deftest t\n  1\n  2\n  3\n)` → `(deftest t\n  1\n  2\n  3)` |
| `body_boundaries`, after-child 1 | `(deftest t\n  1\n  3\n\n  2)` → `(deftest t\n  1\n  3\n  2)` |
| `body_boundaries`, empty first | `(deftest t\n  3\n)` → `(deftest t\n  3)` |
| `body_boundaries`, empty last | Same empty-body closer change |
| `defn_headers`, metadata/doc/attrs/prepost | Appended `\n  42\n)` → `\n  42)` |
| `defn_headers`, qualified single arity | Same appended closer change |
| `defn_headers`, selected multi-arity | `x\n  42\n) {:attr true})` → `x\n  42) {:attr true})` |
| `deftest_nested_testing`, census fixture | `(is (= 1 1))\n)))` → `(is (= 1 1)))))` |
| `deftest_nested_testing`, nested-label fixture | `(is (= 1 1))\n))\n  (testing` → `(is (= 1 1))))\n  (testing` |
| `parity`, CLI shorthand | Disk golden removes one newline between b and ab |
| `parity`, CLI `:op` spelling | Same disk golden change |
| `payload_count`, quoted/tagged multiform | `'x #foo/bar 2\n\n(defn ab` → `'x #foo/bar 2\n(defn ab` |
| `portable_receipt`, accepted read receipt | Golden removes one newline between b and ab |
| `trivia`, CRLF fixture | Final `2)\r\n\r\n` → `2)\r\n` |
| `receipt`, result_hash | `e51722e43edda0df0cc6689090281ba4ab7e946806711b266950d370d865d247` → `42941c271c531c0c058a984996826c4b65c2cec5d355b09965d8f03b409f0454` |
| `receipt`, bytes_added | 15 → 14 |
| `receipt`, splice | offset 13 → 14; length 15 → 14; hash `6e460fd413f380e275e9c769cb90498cf7804956fd893f56816ef521a9c323ef` → `b30dac5980dce85bdbf4130d40cdb14f8f4acc4640cfaacbe9eeb5b24dc6e5b4` |
| `receipt`, line_range | `{:start 1 :end 2}` → `{:start 2 :end 2}` |
| `receipt`, preservation prefix | `1d1f4497724ad81799012397fd4781c16ca5cc6fcbaec3a42d75dfd80f2d160e` → `9f7f8b28df3ae36aea970ba670a8939a2cd35620f77ce6cdb5723fb221c9a948` |
| `receipt`, preservation suffix | `c35754ffc009bb12060d89d0f63814c80d264267a52f6528392ac3e02a3bafe4` → `83f9249d855af8169bc768f86b07677f3ff636f1b477ed7399bd06511fbe7a7f` |

The §5 example repeats those revised facts, including read_back_hashes. Source hash,
inserted-form line ranges, form count, and preservation count/boolean are unchanged.
Golden hashes were independently calculated with Python hashlib from literal bytes.
Existing before/BOF expectations and trailing/standalone-comment expectations remain
unchanged. No refusal expectation was relaxed. Deliberately invalid oracle examples
remain invalid examples, not new success expectations.

The new spacing witness covers blank-line top-level gaps with and without attached
comments, blank-separated body after-child/last, empty defn, an existing closing gap,
explicit trailing payload newline, trailing payload comment, inline body siblings,
and newline-terminated payload before an existing sibling. `fix1-spacing-red.log`
records the initial spacing RED; `fix1-spacing-edge-red.log` records 3 failures
for the trailing-comment and inline-sibling cases before their fixes.

## F3 — refusal envelope

Candidates now contain kind/name/line (plus spans); ambiguous multi-arity owners
report capped, indexed arity candidates. The envelope witness checks exact candidate
identity for duplicate owners and missing arity, plus exact refusal and unchanged
target bytes. A table checks remedies for all 19 validation refusal types, including
fresh-read guard refresh, candidate selection, malformed input, unsupported syntax,
limits, parse/placement defects, and form counts. Remedies identify the decision;
no generic one-sentence remedy substitutes for every type. `fix1-envelope-red.log`
and `fix1-envelope-green.log` record RED then 2 tests / 46 assertions green.

## Crash-window recovery

`clj-surgeon.insert-forms/recovery-status` documents the procedure on the recovery
entrance: establish the publisher has stopped; read the durable detail; freshly hash
the target; compare result_hash before source_hash. Verdicts are :published (never
replay), :not-published (reconsider against a fresh read), or :target-changed (manual
reconciliation, preserve external bytes). Keep the receipt; remove only that
transaction's `.candidate` seed after the publisher is stopped and reconciliation
is complete. No automatic replay, cleanup, or transaction redesign.

The one recovery witness suppresses outcome finalization after real publication,
reads the still-planned durable receipt and actual target bytes, and checks all
three digest verdicts. `fix1-recovery-red.log` proves the absent reader failed first.
The test models the lost-finalization window; it does not kill a JVM.

## Final verification and census

- `fix1-focused-final.log`: **22 namespaces, 24 tests, 548 assertions, 0 failures/errors**.
  Includes both production bb CLI spellings and direct MCP-handler parity.
- `fix1-test-fast.log`: **76 namespaces, 756 tests, 7,877 assertions, 0 failures/errors**;
  zero isolation violations, zero failed preconditions, all eight lanes exit 0.
- `fix1-census.log`: direct CENSUS_REGENERATE=1 entrance, **1,689 deftests**, green.
- `fix1-lint.log`: changed Clojure files through `~/bin/clj-kondo`, **0 errors / 0 warnings**.
- `git diff --check 33d75cfc`: green. Worktree clean after named-file commits.
- No landing-gate-prewarm; no final Sol verdict claimed.

The first census run produced no additions because I had not yet enrolled the four
new namespaces in the explicit manifest. The first test-fast failed three manifest
checks for exactly those omissions. Adding manifest membership and adoption reasons,
then rerunning direct census regeneration, resolved them; no count was re-pinned.
An intermediate lint found one redundant let in the oracle, then passed after it
was flattened. Final logs above are the completed passing runs.

I READ the ledger diff: **six additions, zero removals**, all listed here:

1. `clj-surgeon.insert-forms-candidate-test/insert-forms-candidate-structure-refuses`
2. `clj-surgeon.insert-forms-candidate-test/insert-forms-parse-errors-refuse`
3. `clj-surgeon.insert-forms-envelope-test/insert-forms-candidate-envelope`
4. `clj-surgeon.insert-forms-envelope-test/insert-forms-refusal-remedies`
5. `clj-surgeon.insert-forms-recovery-test/insert-forms-planned-receipt-recovery`
6. `clj-surgeon.insert-forms-spacing-test/insert-forms-existing-separators`

Stable requirements INSERT-FORMS-019..024 and their INTENT / INTENT-TEST / @spec
links document these promises. The local linked-intent-testing skill path in the
build addendum is absent; I read its named canonical copy at
`/home/forge/opt/claude-skills/linked-intent-testing/SKILL.md`. AGENTS.md was not edited.

## Disagreements and remaining review boundaries

No disagreement with F1–F3 or Fable's spacing ruling. The original report suggested
asking Sol to decide spacing; Fable's explicit ruling in this brief superseded that
suggestion. The §3 obsolete S formula necessarily changed with the one-paragraph
p-rule amendment so the contract cannot demand both spacing policies.

BOF header placement (F4) remains the deliberately documented contract behavior;
this brief did not request a header-ownership change. F6's shared file-lock witness,
F7's inspect response cost, and F8's conservative recovery-required reporting were
not redesigned in this scoped round. The independent oracle still shares
rewrite-clj with production; no new parser-independence or performance claim is made.
Recovery classifies observed bytes and documents manual reconciliation; it is not
an automatic crash-cleanup daemon. Sol review and ship-owned prewarm remain pending.
