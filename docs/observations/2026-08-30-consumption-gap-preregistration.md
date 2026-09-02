# Consumption-gap preregistration

Date: 2026-08-30 PT  
Branch: `experiment/consumption-gap-20260830`  
Status: **frozen before production-outcome inspection**

## Typed contradiction

The complete-vocabulary causal screen at `c1e89d5d` produced zero same-file
recovery reads in ten complete-list episodes. Production telemetry reported
that all 137 retained owner-selection refusals carried complete owner-name
lists, yet 119 were followed by a reread. This study distinguishes:

- **H-LOC:** the names were complete but the positional answer was not. The
  refusal's `show_form` projection capped location rows at ten, so callers read
  to recover an omitted location or to disambiguate duplicate owner names.
- **H-HABIT:** callers reread information already present and directly usable
  in the refusal because the payload was not trusted, noticed, or consumed.

Phase 1 is zero-new-model telemetry. Phase 2 is forbidden unless the frozen
Phase 1 gate reports `both-hypotheses-alive`.

## Population and boundary

The population is exactly the 119 retained production episodes already scored
as a reread among the 137 complete-owner-list refusals. An episode begins at
the completed typed owner-selection refusal and ends immediately before the
next mutation attempt. The recovery read is the first completed read action in
that interval; a batched action is one read and all of its returned semantic
kinds are classified together. Later reads are secondary and cannot replace
the first-read classification.

The source receipt, time bounds, collector commit, and SHA-256 are frozen in
the result report before extraction. A population other than 119 is a typed
`population-mismatch`, not a silently revised denominator. Every episode is
retained. Missing evidence is `unclassifiable`, never guessed from command
names or route phases.

The history collector is the only broad history reader. Transcript inspection
is allowed only for a receipt-named session, the registered refusal-to-next-
mutation action interval, and fields that the privacy-safe receipt cannot
answer. The derived ledger contains hashed episode identity, caller model,
anonymous owner tokens and locators, semantic-kind labels, classification,
and evidence hashes—never prose, source bodies, workspace paths, commands,
URLs, account names, or hidden reasoning.

## Registered canonical facts

`bench/consumption_gap/classify.py` consumes one JSON array. Each deidentified
episode records:

- caller model and a hashed episode ID;
- the refusal's complete owner-name set, displayed anonymous location rows,
  cap flag, required next selector, answer token, and uniqueness;
- the first recovery read's owner-name set, anonymous location rows,
  duplicate groups, resolved-duplicate flag, semantic kinds, and an explicit
  evidence-complete boolean.

The adapter may anonymize owner strings and locators with stable per-study
tokens, but equality and multiplicity must be preserved. It may not infer a
field from the final successful mutation. Two independent hashes fence the
raw bounded evidence and the canonical projected episode.

## Mutually exclusive classification law

Classification uses this precedence:

1. **duplicate-name-disambiguation** — the read returns at least two distinct
   location/typed identities for one owner name already in the complete list,
   or the bounded evidence explicitly shows that the read resolved such a
   duplicate. This supports H-LOC.
2. **location-beyond-cap** — the refusal says location rows were capped and
   the read returns an owner-to-location mapping for a complete-list owner that
   had no displayed refusal row. This supports H-LOC.
3. **same-owner-names-only** — the read's only semantic kind is `owner-name`,
   it returns at least one name, and every returned name was already in the
   refusal's complete set. This supports H-HABIT.
4. **other** — the read adds source/body text, dependencies, hashes, a new
   owner name, or any other semantic fact without satisfying rules 1 or 2.
5. **unclassifiable** — completeness, the refusal, or first-read result is not
   observable well enough to apply the preceding rules.

`verbatim_consumable=yes` only when the exact unique selector required by the
next mutation call appears as one contiguous refusal value and can be copied
unchanged. Namespace qualification, ordinal/location choice, duplicate
resolution, spelling transformation, or missing uniqueness makes it `no`.
Missing selector evidence is `indeterminate`. H-HABIT's high-confidence count
is `same-owner-names-only AND verbatim_consumable=yes`.

Counts are reported overall, by exact caller model (unknown is retained), and
by all three consumability states. No model stratum with fewer than five
episodes receives a rate interpretation.

## Frozen Phase 1 decision gate

Classifiable coverage below 90% requires an instrumentation repair and replay
of the identical bounds before any live screen. Among classifiable episodes:

- H-LOC is dominant at at least 80% location/duplicate support and at most 10%
  high-confidence H-HABIT support.
- H-HABIT is dominant at at least 80% high-confidence verbatim/name-only
  support and at most 10% H-LOC support.
- `other` at 50% or more resolves the contradiction as a third information
  requirement that the proposed duplicate-location screen does not test.
- Both hypotheses remain materially alive only when H-LOC has at least eight
  episodes and high-confidence H-HABIT has at least eight, without a preceding
  dominance verdict. Only this verdict opens Phase 2.
- Otherwise the only category with at least eight episodes is the only
  materially supported registered hypothesis; if neither reaches eight, the
  registered pair is inadequate.

These thresholds and the classifier self-tests are committed and pushed
before the production outcomes are projected.

## Conditional Phase 2 protocol

If and only if Phase 1 opens the gate, use fresh subscription-authenticated
`gpt-5.6-sol` processes with no API-key route. The fixture contains duplicate
owner names at distinct anonymous locations and makes the correct next call
require the location-qualified identity.

- **Arm L:** complete owner names and complete location rows.
- **Arm C:** the current complete-name shape with location rows capped at ten.
- Fixed schedule: `L C C L` repeated four times, giving eight episodes per arm.
- The first `L C C L` block is the retained sub-ceiling pilot. Continue only
  if all four environments are valid, the helper and input fences hold, no
  episode times out, and total pilot wall is below six minutes. Pilot episodes
  remain in the final denominator; no prompt or scorer may change.
- Primary: any recovery read between the controlled refusal and next mutation.
- Secondary: recovery tool turns to success and exact semantic completion.
- Safety: wrong-subject must be exactly zero in both arms.
- Screen pass: Arm L's reread rate is at least 30 percentage points below Arm
  C, its median recovery turns are no worse, and wrong-subject is zero. This is
  a causal screen, not a population estimate.

Every raw stream, prompt, fixture before/after, helper log, score, manifest,
model/reasoning identity, and archive hash is retained. Losses remain visible;
replacements can extend an arm to eight fully valid episodes but never erase a
scheduled loss.

## Transfer claim to test explicitly

Sweep-1 made the refusal answer unusually easy to consume: owner names were
unique, the helper accepted the owner token alone, the complete arm contained
the target token verbatim, no location/ordinal/dispatch identity was required,
the target source body was irrelevant, and the route was forced through one
helper. Production transfers only when the refusal exposes every decision
variable required by the next call, the correct answer is unique and verbatim
consumable, and the task does not still require source or positional context.
Complete vocabulary alone is not the transfer condition.
