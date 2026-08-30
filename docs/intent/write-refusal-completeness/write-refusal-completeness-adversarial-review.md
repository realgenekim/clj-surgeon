---
parent: write-refusal-completeness-design
status: pre-ratification adversarial review
---

# Write-Side Refusal Completeness Adversarial Review

## Method

A separate agent received a REFUTE brief. It read the exact packet, the full
audit at `e418c851`, the permanent read-normalization leaf, the prepared-request
authority packet at `0228fbe`, and the relevant active HLD and MCP requirements.
It tested 13-site coverage, ranking, EARS context independence, contradiction,
payload bounds, oversize behavior, write and retry authority, and phase stops.
The agent made no file edits.

## First exact pass

Verdict: **REFUTED**.

Reviewed SHA-256 values:

- design: `b9905593d863934a1fae2bdd5ee1875b2f7134ca0a8d6f3833f1c2a3a565b37a`;
- specs: `7d8ddd78644c4e10905f5ac8e0301341537a156ddea062d6cb799aa6ebed8fdd`;
- consistency report: `cfafadfc46b948036f83830d3f1b65998f71978f1dc7c8a5b72b13607be4776f`.

The reviewer confirmed the 13-site mapping, eight stable IDs, audit ranking,
001 priority, `[D]` state, and no-write direction. It found these ratification
blockers or major defects:

| Finding | Correction applied |
|---|---|
| The draft assumed a write-result bound that active intent does not define. | Proposed an exact 32,768-byte full-JSON write result envelope, the active read-side byte measurement, a 128-row limit, and a compact overflow fallback. |
| The continuation did not uniquely identify its omitted candidate universe. | Closed the descriptor over entrance, refusal, family, subject, query SHA-256, ordering version, guards, offset, row bound, and remaining count. Kept the consumer behind a separate read leaf. |
| Seven EARS statements omitted the common overflow law. | Added the exact bound and overflow branch to every requirement plus one closed registry-wide descriptor and fallback contract. |
| 001 assumed form counts and exact owners for every generic scope. | Made per-form counts form-scope-only and defined closed form, namespace, and root row identity. |
| 003 implied that one relation failed and did not close failure identity. | Defined three ordered relations, applicability, and a closed failed-predicate vocabulary. |
| Family projections were suggestions rather than a public schema. | Added common envelope fields, closed family names, subjects, mandatory and conditional row fields, exact ordering, guard scope, and profile violation IDs. |
| Ratification and test activation language disagreed. | Stated that design/EARS ratification leaves every ID `[D]` and that red-test activation requires separate authority. |
| Snapshot guards appeared mandatory even for pre-source profile admission. | Made guards conditional on source capture and defined an empty continuation guard map for pre-source families. |

## Second exact pass

Verdict: **REFUTED**.

Reviewed SHA-256 values:

- design: `7d19f2636b64ed94c2ed0cf55aad8646fd3c7c3d1ffa984e2e29747d1018ea8a`;
- specs: `d8e85e0e3aae2ab8d18a8aab2453dac3cf539cb9fbf91cf343978581828289f8`;
- consistency report: `b89a881738dcb7922cfa510086def37d5db45d19b31f399936d7b6f551d9f770`.

The reviewer confirmed the first-pass repairs to priority, stable IDs, overflow
branches, 001 scope, family projections, phase stops, and no-write authority.
It found these remaining defects:

| Finding | Correction applied |
|---|---|
| The zero-row fallback retained unbounded count maps, names, paths, and guards. | Added a constant-size fail-empty projection that retains stable refusal identity, bounded safety booleans, numeric counts, fixed limits, and no unbounded dynamic value. |
| The query hash did not include the selector or configuration that generated candidates. | Added a mandatory family-specific `selector_sha256`; 008 also binds a source-free configuration-universe SHA-256. |
| 008 disagreed about omitted versus empty top-level guards. | Top-level guards are omitted; only the inert descriptor contains an empty guard map. |
| 008 included `profile-not-project-owned`, which belongs to a distinct unaudited refusal, and did not define violation short-circuiting. | Removed that violation and defined absent, non-map, and map applicability for the remaining nine IDs. |
| 003 included `namespace-name-in-form`, which cannot emit `compact-location-unresolved`. | Restricted 003 to `namespace-clause` and `complete-named-owner`; existing owner resolution remains under 002. |
| The context-free report overstated what one EARS sentence carries alone. | Made the complete packet the context-free review unit and named the registry-wide normative paragraph required by each EARS statement. |

## Later exact passes

The reviewer continued to receive a REFUTE brief after every material
correction. No reviewed technical file changed during any pass.

| Pass | Design / specs / consistency SHA-256 | Verdict | Finding and disposition |
|---:|---|---|---|
| 3 | `6b7cbb203a4ba0e6062ccff28c59b9b1fa8b682dd2c9179dabdcb21250967fb2` / `c05ebf199bd9ed3d7dc4dde546664f634b89f9ee6871872cab6a0fb09ab65046` / `cc6e56628cc8c6052a256a79c4f1cda518e740410ffe948c199916d73ba9902b` | REFUTED | Presentation inherited caller order, but the candidate-query identity did not bind every caller-ordered collection as an exact ordered vector. The fixed fail-empty object also omitted finalizer-owned `elapsed_ms` and relied on family stages that were not closed for all constructors. Every caller-ordered vector was bound in the canonical selector digest. The fixed object became a domain projection finalized with `elapsed_ms` before full-result measurement, and one closed stage value was registered per family. |
| 4 | `602dd02b2068b4239b319bea7f05791bbe9b468eb6c06d824b8a981a81be8799` / `3b69c66b1e9bd36f7b924d81bbd0d259316886780dddb4afe597c31605311ad8` / `323da9e4240448bef699770e6f0cb929928ece76df74990debc3c8ebfc3de254` | REFUTED | Exact prefix sizing still required a finalized result containing `elapsed_ms` before the unchanged finalizer could add that field. The stage language also failed to distinguish preserving an existing stage from publishing a newly registered stage when none existed. A 128-byte timing reserve and pre-finalization sizing budget were added. Post-finalization measurement became an invariant check. Existing stages are preserved, while stage-less constructors publish the registered family stage. |
| 5 | `36318ca26adbd9a06c4061cef9d3c3967c84cb0f0b36331a60ff95feabd0880b` / `c4438254a248ea11e28ffd5401b64da050a4677f3843b42dece4bafc2801e6e1` / `513fc1fc8e60942f115d1aeb2ed627b0fe78a31e9510b3c8921383f0115b8ae2` | REFUTED | A 128-byte timing reserve was sound, but the zero-row fallback still triggered at 32,768 pre-finalization bytes. The trigger moved to 32,640. |
| 6 | `ce379d32eb013ac2d2329b04c81ca9b1ced5bcd9ed9c3610d023826ed1ff11b0` / `f9b3d289986023ca338e6261d3eacc3f46adc8c4f82c57dee8f36518c179a7f5` / `b6b6883cd03d11499b6ff5decf1480098e5159f039bf12e14c17a94cc762a60e` | **SURVIVES** | No ratification-blocking defect remained. |

The surviving pass confirmed:

- exact 13-site coverage and audit priority;
- eight stable `[D]` requirements;
- the 32,640-byte domain budget, 128-byte timing reserve, 32,768-byte finalized bound, and fixed fail-empty path;
- ordered selector and configuration-universe binding;
- the two-relation 003 scope and nine-ID 008 scope;
- packet-level context independence and phase stops; and
- no executable retry, selected candidate, prepared request, or inherited
  write authority.

Final verdict: **SURVIVES. Ready for Gene's design and EARS ratification
decision. No test or implementation is authorized.**
