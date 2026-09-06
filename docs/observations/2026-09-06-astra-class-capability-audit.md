# Astra — owner_forms class/capability audit (read-only)
Subject: current /var/tmp/forge/astra-typist-route-fx; no calls, tests, or source edits.
Conclusion: the five classes are admission/rate labels, not five transformation implementations.
The backend is replacement of existing named definition slots. General move/extract/create/delete is not represented.
A valid-shaped request can become ready while its prose asks for an impossible move; ready means frozen authority, not proven intent realizability.
This is a real user-facing scope/admission gap if “supported mechanical class” is understood as a supported operation, but no unsafe write or class-specific compiler failure has been demonstrated.

## What the code actually does
src/clj_surgeon/mission_typist.clj:8 defines the five keywords; :43–49 binds rate identity to class/provider.
Its failed-condition :67–111 checks membership, dossier, policy, proof, budget and provider/rate; it does not interpret intent or dispatch on class.
Its dossier :137–153 prompts replacement definitions, preserving original names unless planner supplies :new-owner.
No class-specific transformation branch appears in the executor/forms/compiler paths inspected.
src/clj_surgeon/mission_typist_executor.clj:37–49 confined-file requires an existing regular file.
owner-span :69–83 requires one existing named owner, validates definition syntax, and derives its exact existing span.
plan :109–153 freezes selected owner/proof files, resolves spans, assembles a :basis, and calls typist/dossier.
There is no derive-basis function in current src/test; the equivalent basis derivation is inline in plan.
Proof files join the frozen proof closure; they do not become editable owner slots merely by being listed.
src/clj_surgeon/mission_cli.clj:79–89 turns successful owner_forms plan into :ready without a generated candidate.

src/clj_surgeon/mission_forms.clj:25–55 definition accepts exactly one top-level def/defn/defn-, rejecting protected syntax.
compile-forms :123–160 requires each replacement's original [file,owner] to identify exactly one frozen owner.
The resulting definition must keep head/visibility and docstring, and its name must equal original or planner :new-owner.
The compiled change replaces that original span in that original file. No destination-file/name field exists beyond new-owner.
src/clj_surgeon/mission_candidate.clj:52–68 locate/splice and :70 onward compile-candidate preserve bytes outside authorized spans.
No top-level empty deletion, sibling definition insertion, new file, removed file, or namespace rewrite is available through this owner_forms entrance.
Nested body edits can introduce local helpers/expressions, but that is not moving a top-level Var to another namespace.

Important precision: JSON owner-forms does NOT enforce exact coverage of every selected owner.
compile-forms rejects unknown/duplicate replacement keys, but does not compare the entire expected key set.
A subset can compile; independent proof must catch incomplete requested work. The omitted slots remain unchanged.
Plain :clojure-forms DOES enforce complete coverage, one file only: mission_plain_forms.clj:95–117.
Thus “fixed owner slots” is accurate for both formats; “exact owner coverage” is only accurate for plain forms.

## Classes versus representable work
:rename: existing definition/local-body edits; top-level new name must be supplied by planner; callers need their own slots.
:thread-parameter: existing signature/body/caller replacements fit, subject to head/docstring/comment and proof constraints.
:fanout: multiple existing definition replacements fit. New forwarding helper or require insertion needs separate preparation/route.
:witness: can modify an existing def/defn-shaped witness; cannot create a test file or directly select deftest (unsupported definition head).
:move-helpers: only re-expressing behavior within existing slots or tightly constrained swaps/reassignments can fit.
Even swaps must preserve each slot's head/docstring and declared resulting name; this is not general helper movement.

## Pure-data counterexample, not an executed test or fabricated live prior
Starting with the existing synthetic `eligible` fixture in test/clj_surgeon/mission_typist_test.clj:8–24:
```clojure
(-> eligible
    (assoc :mission-class :move-helpers
           :intent "Move run from src/a.clj into a new src/b.clj; remove the old definition.")
    (assoc-in [:rate :mission-class] :move-helpers))
```
Static route inspection finds no failing condition: files/owners/policy/proof stay identical and class/rate match.
This only demonstrates pure admission shape; fixture evidence is synthetic and must never authorize a live provider.
For an otherwise established public plan, this same prose does not affect the successful-plan→ready projection.
But adding b.clj as a target would fail existing-file admission; leaving it out gives no destination slot.
No replacement can both remove run's top-level definition and create b.clj under this basis. Strong acceptance must refuse all candidates.

## Existing evidence and recommendation
mission_forms_test.clj:16–35 witnesses rename preservation and refusal of unknown owners, multiple forms, visibility changes.
mission_candidate_test.clj:14–34 witnesses preservation outside spans and original-snapshot authority.
mission_typist_test.clj:26–59 tests rate boundaries and unknown class; these are not move/extraction capability witnesses.
docs/mission-typist.md:24–27 explicitly says proposal is authority preview, not a candidate diff; :83 lists classes without replacement-only limits.
Document labels as workload/rate categories constrained to existing def/defn slots; explicitly exclude top-level insertion/deletion/movement/new files.
Do not claim planning proves prose feasibility or classify all move-helpers requests as invalid from the name alone. No capability extension or admission relaxation proposed here.
