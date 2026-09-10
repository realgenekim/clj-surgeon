# Oracle notes — I-03-heap-sample-retention
### Several correct spellings

The settle duration, the docstring wording and the argument-free shape's
internals are free. **Probed:** a variant with `(Thread/sleep 40)` and a
different docstring is accepted (`curation/variants`,
`alt-sleep-and-docstring`, exit 0). Pinned: a public `sample-retention!`, a
private `retention-peak`, a forced collection, the running maximum, and the
placement — directly after `to-mb` and directly before `measure`.

### Gate

`test/clj_surgeon/memory/heap.clj` has no test namespace of its own; it is
support code for the memory battery. `:gate` is therefore the narrowest honest
check available, that the namespace loads. The full memory battery
(`clojure -J-Xmx1g -M:clj-surgeon/memory-test`) is the broader gate and is
heavy; if the harness runs it, run it identically in every arm.

`wrong-output.clj` is the half-done version: the state without the meter.


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
