# Prepared-request eligibility characterization audit

Date: 2026-08-31  
Auditor: SURGEON2  
Verdict: **NO-GO as release-train test cargo; product behavior remains green**

## Immutable subject

- Candidate: `6991639eabc45da1c260ed11cbdcf83bd06eed32`
- Tree: `5c0a0476bbe5c3b65117676ef0b7989fb7aa90bf`
- Branch: `test/eligibility-characterization-20260831`
- Test SHA-256:
  `724f964d1a2e1e506ff8f08ab97a6ed20c10a84afc427f830c7b1f930de479a5`
- Explainer SHA-256:
  `d2e84a7f9212032159b6f33d085e2b0f99d275afa65365873cb0263ace73504a`
- Characterization table SHA-256:
  `8f53bffe626fb4985b513024cb3cb1a818666a8d15cb011cef05f6789262ae9e`

## What is sound

The positive fixture is genuinely eligible under the current product
projector. Independent projection produced one inert prepared request, one
edit, one caller hole, a null replacement hole, exact file/owner/from/matches
guards, and byte-identical ordinary result after removing only
`prepared_request`:

```clojure
{:eligible true
 :ordinary-byte-equal true
 :edit-count 1
 :caller-holes ["arguments.edits[0].to"]
 :file "src/x.clj"
 :owner "target"
 :from "(def target 1)"
 :null-hole nil
 :matches 1}
```

The exact focused namespace is green:

```text
Ran 13 tests containing 13 assertions.
0 failures, 0 errors.
```

## Typed blockers

### 1. Several named guards have false-green fixtures

The committed duplicate-owner case changes the forms vector without updating
the declared form count or row/result character counts. Its own explainer
therefore reports four failures:

```clojure
[:count=form_count :chars=row :chars=result :owners-distinct]
```

If the product accidentally removed the owner-distinctness guard, the test
would still pass because three unrelated inconsistencies refuse the fixture.

The committed seven-form case likewise reports five failures:

```clojure
[:count-1-6 :count=form_count :chars=row :chars=result
 :every-form-evidence]
```

It therefore does not pin the six-owner eligibility boundary. The bad-file-SHA
case also conflates the direct SHA law with form-evidence and aggregate-hash
inconsistency.

Independent corrected fixtures prove the intended single-axis tests are
constructible without changing product code:

```clojure
{:dup-failing [:owners-distinct]
 :dup-attaches false
 :seven-failing [:count-1-6]
 :seven-attaches false}
```

The repair is to update all dependent counts and exact form evidence after
each mutation, then assert the explainer's exact singleton failure before
asserting descriptor omission.

### 2. The suite is not in a release runner

No Make target, dependency alias, `test/run_all.clj`, or
`mcp_test_runner.clj` entry names
`clj-surgeon.eligibility-characterization-test`. Cherry-picking the candidate
would therefore add a green test file that normal release gates never execute.

The smallest repair is one explicit runner registration plus a permanent
assertion or self-test that the namespace is loaded by the intended release
gate.

### 3. The recorded count does not reproduce

The commit message reports `14/14 green`. The exact immutable candidate runs
13 tests and 13 assertions. No fourteenth test or assertion is present in the
three-file diff. The evidence must be corrected to 13/13 or the missing
fourteenth witness must be added and named.

### 4. The explainer and EDN table are unbound evidence

The committed tests call the product projector directly but never compare
`dev/explain_eligibility.clj` with product admission and never validate the
banked EDN table. Both artifacts can drift while the test namespace stays
green. Either keep them out of release cargo or add a pure consistency test
that regenerates the table and proves explainer eligibility agrees with the
product projector for every row.

## Decision

NO-GO for release-train test cargo at `6991639e`. The product eligibility
function passed every executed case; this verdict is about witness authority,
not a product regression. The suite becomes admissible after the named guards
are isolated, the exact count is reconciled, and the namespace is mechanically
included in the release runner. The explainer/table must then be either bound
by tests or omitted from the cargo.
