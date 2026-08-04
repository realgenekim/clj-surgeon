# Decide the X-Ray Evidence Contract

**Status:** Open decision
**Severity:** P1 public contract

## Evidence

`prepare-xray-options` accepts `:evidence :compact` and `:evidence :full`.
The X-ray operation registry documents only `:file` and `:expr`, so per-op help
does not reveal the accepted flag. README describes literal paths as the full
evidence route while compatibility text still mentions full evidence.

This leaves an LLM unable to know whether `:evidence` is public, compatibility
only, or accidental hidden surface.

## Required Outcome

Choose one contract:

1. document `:evidence` completely in operation help, README, skill, examples,
   and changelog; or
2. mark it as compatibility-only in code and tests, keep it out of the primary
   mental model, and state its stability policy; or
3. remove it through an explicit migration only after proving no supported
   caller depends on it.

Do not accept an undocumented public argument indefinitely.

## Tests and Verification

- Global/per-op help, CLI parsing, dispatch, README, skill, and changelog agree.
- Valid modes succeed; invalid modes return stable EDN and nonzero exit.
- Literal paths and computed compact evidence retain their current invariants.
- No existing full-evidence regression is weakened.

## Done When

A clean LLM can determine the complete supported evidence behavior from one
help response or the primary skill without guessing.
