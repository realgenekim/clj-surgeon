# Correct Performance and README Claims

**Status:** Open
**Severity:** P2 release truth

## Evidence

The direct compact comparison measured v12 at 23.764 seconds and v13 at 24.462
seconds. v13 is the selected smaller candidate, not the speed winner.

README still contains historical headline claims such as “13 ops,” “~1500
lines,” and “zero dependencies beyond babashka.” The current operation registry
and source tree have grown substantially. Historical origin-story facts may
remain when labeled as historical; present-tense product facts must be derived
or updated.

## Required Outcome

- Describe v13 as smaller and selected under the close-result rule; describe
  v12 as the fastest measured compact candidate.
- Separate adjacent benchmark comparisons from global or timeless claims.
- Recompute current operation and line counts, or replace volatile counts with
  durable qualitative language.
- Qualify dependency claims precisely: embedded libraries are still software
  dependencies even when Babashka supplies them.

## Tests and Verification

- Drift tests derive any retained current counts from the operation registry
  and source tree.
- Captain's Log, plan, README, changelog, and handoff use consistent timing
  language.
- No headline uses incorrect runs, corrupted pilots, or invalid Claude trials.
- Historical claims are explicitly dated or scoped to the origin story.

## Done When

Every quantitative product claim is reproducible from durable evidence or
clearly labeled historical context.
