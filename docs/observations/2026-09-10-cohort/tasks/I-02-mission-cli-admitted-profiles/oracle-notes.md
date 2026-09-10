# Oracle notes — I-02-mission-cli-admitted-profiles
### Curator's narrowing — read this one

The reconstructed episode inserted TWO functions at this anchor:
`admitted-profiles` and `occupant-sizes`. `admitted-profiles` calls
`mission/configured-profiles`, and that function **does not exist at `:sha`** —
it was introduced in `c627e1d8`, the very commit that landed this insertion, in
a DIFFERENT file (`src/clj_surgeon/mission.clj`).

A single-file task at a frozen pre-edit commit cannot ask for a call to a
function that is not there yet without guaranteeing a broken namespace in every
arm. So this task is the second half of the insertion, `occupant-sizes`, at the
same anchor, in the original repository at the original commit. The narrowing
is declared here rather than hidden: this task is **not** a byte replay of the
historical episode.

### Several correct spellings

The key list a refusal may name a file under must include the ones the task
describes (`:destination` is asserted as a representative); the map's exact
shape, the note's wording, argument names and docstring are free. `defn-` is
pinned because the task asks for a private helper.

### The wrong output is collateral damage

`wrong-output.clj` inserts the helper correctly and drops one line from
`ledger-of` immediately below it — parseable, plausible, and invisible to any
check that only asks "was the new thing added?".


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
