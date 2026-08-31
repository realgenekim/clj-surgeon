# Prepared-request eligibility third audit

Date: 2026-08-31  
Auditor: SURGEON2  
Verdict: **NO-GO as release-train test cargo; one agreement row remains missing**

## Immutable subject

- Candidate: `691549470e9260bcbaee7d9ba6dd2bea3faf7fd6`
- Tree: `901abd9b85f2997970c6e371bfad36cd2eaa7454`
- Prior successor: `5a6c970a4b0dce915ae0e817960d3017018bc0d1`
- Characterization test SHA-256:
  `40659de6cf7c3bd97cae006503c7497acc6bc98091139fb90de80fa29932e974`
- Candidate-local explainer SHA-256:
  `813d5f53598c591f3b20d544a17fd9cf6b1852b6140d0e7b8a930320d2d375a8`
- Runner SHA-256:
  `8844e1a11c110733f8a8ee5ff668625d37c2008e8587b00384a4f94e9f86fba8`

## Prior blockers closed

The explainer now lives at
`test/clj_surgeon/eligibility_explainer.clj`, is required through the candidate
test classpath, and no longer depends on `/tmp/cathedral` or any other shared
checkout.

Exact clean-detached fresh-JVM execution reproduces the corrected count:

```text
Ran 14 tests containing 25 assertions.
0 failures, 0 errors.
```

The duplicate-owner and seven-form product witnesses remain single-axis and
runner require/execution registration remains present.

## Remaining false green

The test named `explainer-agrees-with-product-on-all-rows` checks the positive
base plus eleven mutations. The suite contains twelve refusal mutations. Its
`muts` vector omits the internally consistent seven-form boundary used by
`seven-forms-refused`.

That omission is executable, not editorial. The audit replaced only the
explainer's answer for seven-form inputs with an incorrect eligible result,
then ran the exact agreement test. It still passed all assertions:

```clojure
{:corruption :seven-form-reported-eligible
 :agreement-test {:test 1 :pass 12 :fail 0 :error 0}}
```

Therefore an explainer regression in `:count-1-6` is not bound to product
truth despite the test name and general-agreement claim. The direct product
seven-form guard remains pinned; this blocker concerns only the promised
explainer/product agreement authority.

## Smallest repair

Add the same internally consistent seven-form result used by
`seven-forms-refused` to the agreement test's mutation collection. Preserve
its synchronized `forms`, `form_count`, row character count, and result
character count so it exercises only `:count-1-6`. Then rerun from a fresh JVM
and update the exact assertion count.

## Decision

NO-GO at `69154947`. Candidate-local provenance and the reported 14/25 count
are now correct; no product regression was found. One missing agreement row is
the sole remaining release-cargo blocker.
