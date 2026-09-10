# Oracle notes — T-02-study-row-line-range
### Several correct spellings

Any spelling that derives the expected row from the form in hand is correct:
`(str "reader-cond?@" (:line form) "-" (:end_line form))`,
`(format "reader-cond?@%d-%d" (:line form) (:end_line form))`, a `let`-bound
range, or a helper. **Probed:** the `format` spelling is accepted
(`curation/variants`, `alt-format-spelling`, exit 0). The assertion's message
string is free.

What is NOT correct, and is rejected: re-pinning the constant to today's lines
(`reader-cond?@48-50`) — the same defect one file-edit later. That is this
task's `wrong-output.clj`.

### Curator's construction

The historical episode also left a tautological `(is (= [a b] [a b]))` beside
the new assertion. `expected-output.clj` does not reproduce it: the task states
an intent, and the correct implementation of that intent does not include an
assertion that cannot fail. A caller who adds one anyway still passes — the
oracle constrains the deftest owner's behaviour, not its exact bytes.


## How this oracle decides

Two hashers that share no code:

1. **Semantics** — `clj-kondo --lint <file> --config '{:analysis true :output
   {:format :edn}}'`, read through `curation/kondo_edn.py`. Requires, aliases,
   defined vars and resolved var usages come from the analyzer's own name
   resolution, plus fixed-string assertions about the specific thing asked for.
   Any error-level finding other than `namespace-name-mismatch` (which only
   fires because the proof harness lints the file outside its repository) is a
   failure on its own.
2. **Preservation** — `curation/formhash.py`, a hand-written bracket-matching
   tokenizer. It hashes every top-level form's raw bytes, the raw bytes of the
   gap BEFORE each form, and each form's ordinal position. It uses no Clojure
   reader, so it cannot share a failure mode with rewrite-clj or edamame. A
   candidate that does not bracket-match is red, not unknown.

Only the owners named in `preserve_only` may differ; everything else must be
byte-identical, in the same order, with the same text between forms.

## What this oracle deliberately does NOT establish

It does not run the repository's tests. `meta.edn` names the repository's own
narrowest gate; the harness must run that separately, and must first record the
gate's verdict on the UNTOUCHED tree at `:sha` as a baseline (see
`:gate-note`). It does not check any file other than the one named — the
harness's own diff of the worktree owns that.

## Apparatus note

`/var/tmp/forge/cohort-fx/apparatus/oracle-lib.sh` and `apparatus/task-schema.md`
did not exist when these tasks were curated, so the oracle is the protocol's
self-contained fallback, with the shared helpers in
`/var/tmp/forge/cohort-fx/curation/oracle-lib.sh`. If the apparatus later
publishes an `oracle-lib.sh` with the same entry points, change the
`ORACLE_LIB` line at the top of `oracle.sh`; nothing else in the task depends
on it.
