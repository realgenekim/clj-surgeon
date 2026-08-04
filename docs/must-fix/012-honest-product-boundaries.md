# State Honest Boundaries Versus Native Tools

**Status:** Open product contract
**Severity:** P2 scope and positioning

## Evidence

The experiments show strong advantage when a task needs structural discovery,
semantic sibling navigation, computed syntax data, or hash-bound replay. They
do not show that clj-surgeon should replace every native editing tool.

Current honest division:

```text
supplied unique literal replacement       apply_patch
cross-file textual discovery              rg
small arbitrary multi-form context        bounded text reader
runtime/semantic state                     nREPL, compiler, linter
computed singular structural data         :xray
source-derived singular edit              :edit plus transform
ambiguous peer-shaped edit                 structural lens
```

The algebra still lacks general insertion, deletion, arity-changing splice,
nested move, explicitly consented multi-edit, and project-wide structural
search. Those may be valuable hills, but they are not silently supplied by
X-ray.

## Required Outcome

Make README, skill, and help state when clj-surgeon is the preferred lens and
when a native tool remains faster or more expressive. For each missing algebra
operation, either create a separately scoped plan backed by a real field
failure or state it as an explicit non-goal. Do not enlarge the X-ray release
merely to satisfy a “perfect editor” slogan.

## Tests and Verification

- Clean choice benchmarks include at least one trivial task where native
  patching should win and one structural task where clj-surgeon should win.
- Agent guidance does not coerce clj-surgeon for unsupported work.
- Any new mutation primitive gets the full pure matrix, unchanged-byte
  refusals, real-program fixture, separate plan/apply, and atomic verification.
- Cross-file structural search, if pursued, preserves string/comment decoy
  semantics and bounded output.

## Done When

A clean LLM selects clj-surgeon for its measured structural strengths without
being trained away from faster native tools, and every unsupported operation is
either explicitly deferred or independently designed and tested.
