# State Canonicalization Depth

**Status:** Open documentation precision
**Severity:** P1 data contract

## Evidence

`canonical-analysis-value` normalizes a selected value when that value itself
is a map literal or a known `(hash-map ...)` / `(array-map ...)` constructor.
It does not recursively rewrite nested constructor syntax. Unsupported calls
remain lists and odd known constructors refuse.

Some current prose can be read as promising a general recursive canonical
Clojure view.

## Required Outcome

Document the transformation as shallow, selected-value normalization unless a
separate recursive design is explicitly justified. Preserve the non-evaluation
boundary: constructor heads and arguments are never invoked.

## Tests and Verification

- Tests distinguish top-level selected constructors from nested constructors.
- Literal evidence remains byte-for-byte source-shaped.
- Known even constructors normalize; odd constructors refuse; unsupported
  calls remain lists.
- Duplicate keys and map ordering have explicitly asserted semantics where
  they affect returned EDN.

## Done When

An LLM can predict exactly which selected values change representation without
assuming evaluation or recursive normalization.
