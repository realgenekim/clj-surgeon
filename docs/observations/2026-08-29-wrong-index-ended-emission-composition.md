# A Wrong In-Range Index Ended the Composition Hill

Date: 2026-08-29

Lane: SURGEON2, deterministic adversarial probe

Candidate receipt: fbe65efe503444f517eb48e5c05919ca58f810de

Status: NO-GO; model cohort cancelled before launch

## Question

The pure oracle found that file_index + replacement_groups reduced the frozen
request by 23.67%. Forge required one safety falsifier before any model token:

> Can an in-range but wrong index silently select and mutate another real file?

A passing cohort cannot answer this. Four correct calls would only prove that
the model selected the right indices four times.

## Fixture

The disposable workspace contained two different files with the same named
owner and the same guarded source:

~~~text
src/intended.clj
(ns intended)
(defn shared [] :old)

src/wrong.clj
(ns wrong)
(defn shared [] :old)
~~~

The file table was:

~~~clojure
["src/intended.clj" "src/wrong.clj"]
~~~

The site used index 1. It was in range and therefore lowered deterministically
to src/wrong.clj. The intended decision was index 0.

The expanded canonical edit was:

~~~clojure
{:file "src/wrong.clj"
 :within {:form "shared"}
 :from ":old"
 :to ":new"
 :matches 1}
~~~

## Observed result

The current canonical kernel returned:

~~~clojure
{:ok true
 :committed true
 :verification_complete true
 :changes 1
 :edits 1
 :files 1}
~~~

Read-back proved:

~~~text
intended file unchanged: true
wrong file mutated:      true
wrong result:             (defn shared [] :new)
~~~

The probe used the bounded standalone nREPL at PID 55192 and a temporary
workspace. The workspace and receipt were deleted after read-back.

## Why every existing guard passed

The numeric index became a path before canonical validation. At that boundary,
the request no longer contained the intended path.

The selected wrong file was real. It contained exactly one owner named shared.
That owner contained exactly one :old token. The edit count, file count,
read-back, parse, and transaction guards therefore described a valid mutation
of the wrong subject.

There is no existing guard that can reconstruct caller intent after the
mapping is wrong. Snapshot guards prove what was changed, not what the caller
meant to index.

## Decision

file_index is NO-GO for this compression hill regardless of its 23.67% payload
reduction. The authorized F C C F / C F F C dev-a cohort is cancelled. No model
tokens were spent.

The other threshold-crossing pair remains ineligible:

- file_groups + replacement_groups: 23.64%, but file_groups already has a
  retained first-call correctness loss.

Therefore no composition in the current ranked inventory is both above the
20% byte gate and safety-qualified. The remaining hill is exhausted under the
current grammar and safety law.

A future design must introduce an independent invariant that binds each compact
site to its intended file. That would be a new mechanism, not a repair to this
cohort. It must return to the pure byte oracle and safety screen before any
model run. Do not relitigate the stopped file_index candidate.

## Permanent witness

The experiment test
in-range-wrong-index-can-silently-mutate-the-wrong-file pins the falsifier. It
requires all of these facts at once:

- lowering names src/wrong.clj;
- the canonical kernel returns ok, committed, and verification_complete;
- src/intended.clj remains byte-identical;
- src/wrong.clj changes from :old to :new.

No product code, install, reload, shared port, Anvil worker, or pre-existing
process was changed.
