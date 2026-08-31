# `eligible-descriptor` legibility refactor: independent audit

## Verdict

GO for staged product-code cargo at exact candidate
`47d48390a673b68736593ed0b9e76395b08baf79`.

This receipt does not authorize installation, reload, or shared-runtime action.

## Identity and scope

- Branch: `refactor/eligibility-legible-checks-20260831`
- Candidate: `47d48390a673b68736593ed0b9e76395b08baf79`
- Tree: `8d6509028f78a719d45fd46ef09e40b9341233fe`
- Parent: `5573f3a803c0b0061e0532dd2944ed797b7bf8c2`
- Changed path: `src/clj_surgeon/mcp_prepared_request.clj`
- Candidate path SHA-256:
  `00dd385150db3584230ca9f7087bef595e6560794c25f407250c0f6ac9e3c409`
- Parent path SHA-256:
  `67f3449d6bdb98bf1839d654a61f7d542e3d5dfb163a8d9cfe771c539de7cfdc`
- Clean detached worktree:
  `/private/tmp/clj-surgeon-eligibility-legibility-audit.HbGhtQ/worktree`

The diff changes one production form, `eligible-descriptor`. The original
25-condition `and` chain becomes an ordered vector of named thunks plus a lazy
`some` traversal. Bindings, predicate order, descriptor construction, and the
4,096-byte admission boundary remain unchanged.

## Verification

### Existing boundary nets

A fresh 512 MiB JVM ran the characterization and product prepared-request
namespaces together:

```text
Ran 27 tests containing 195 assertions.
0 failures, 0 errors.
{:test 27, :pass 195, :fail 0, :error 0}
```

This is the exact sum of the independently audited characterization suite
(`14/26`) and prepared-request product suite (`13/169`). It covers the
same-name and metadata identity falsifiers, LF/CRLF and comment/trailing-space
variants, exact 4,096/4,097 descriptor budget, higher pre-finalization budget,
snapshot guards, ordinary edit guards, and stale replay.

`git diff --check` and Standard Clojure Style check of the changed path were
green.

### Malformed-input differential

The private production decision was invoked independently at the parent and
candidate with the same deterministic corpus:

- complete eligible base;
- `nil` and empty values;
- early `ok=false` with a later non-countable `forms` value;
- valid early fields with non-vector `forms`;
- inconsistent form counts before form-evidence evaluation;
- stale snapshot guards;
- wrong file-hash map;
- invalid row collection that throws in the unchanged eager binding;
- a nil row.

Every case returned the same value or the same exception class and message in
both versions. In particular, malformed `forms=42` returned `nil` rather than
throwing: later count predicates were not evaluated. Conversely,
`results=42` threw the same `IllegalArgumentException` in both versions because
the unchanged eager `row` binding precedes either predicate representation.

### New call-order falsifier

The audit wrapped the five production helper seams and recorded their exact
call order for valid input and five refusal boundaries. Parent and candidate
traces were identical:

```text
valid          [:path :sha :form-evidence :sha :root :absent :absent]
early ok=false []
bad hash       [:path :sha]
bad count      [:path :sha]
bad root       [:path :sha :form-evidence :sha :root]
stale snapshot [:path :sha :form-evidence :sha :root]
```

This falsifies both eager later-predicate evaluation and duplicate predicate
execution. It also proves the named vector preserves the original first-failure
order rather than merely preserving final truth values.

## Conclusion

The thunks are load-bearing and correctly implemented. `some` consumes the
ordered vector lazily; every label is truthy; each reached predicate executes
exactly once; and the first false predicate stops evaluation. The refactor adds
legible internal names without adding a public diagnostic surface or changing
eligibility, refusal identity, descriptor bytes, exceptions, or write
authority.
