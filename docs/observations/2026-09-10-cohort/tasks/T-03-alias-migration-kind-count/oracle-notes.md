# Oracle notes — T-03-alias-migration-kind-count
### Several correct spellings

Where inside the frozen set the two new kinds are written, and the exact
wording of the comment beside them, are both free. The count must be 149 and
the two kinds must be inside the `frozen-refusal-kinds` form (checked by
locating that form's bytes, not by grepping the file).

### Scope

Two top-level forms change: the `def` that holds the frozen set and the
`deftest` that counts it. That is the smallest coherent intent — bumping the
count without adding the kinds leaves the file self-contradictory, and is this
task's `wrong-output.clj`.

The historical shell command also carried an unrelated `detail-` filename
filter change. It is deliberately out of scope, and the oracle requires it to
be ABSENT: preservation fails if any other form moves.

### Gate caveat

The count 149 is a claim about the entrance's source at `:sha`. Whether the
repository's own gate goes green depends on files this task does not touch.
Record the baseline first; see `:gate-note` in `meta.edn`.


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
