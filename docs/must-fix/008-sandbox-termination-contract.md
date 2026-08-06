# State and Test the Sandbox Termination Boundary

**Status:** Resolved 2026-08-06
**Severity:** P1 safety and availability

## Evidence

The branch refuses authored `loop`, `recur`, `lazy-seq`, and private chunk
machinery while allowing the same internals for `for` macro expansion. This
prevents the field failure caused by direct `(loop [] (recur))`.

It does not prove termination. Allowed pure functions can still express
unbounded work, for example reducing an unbounded `(range)`. The user has
explicitly rejected solving the halting problem; the documentation must remain
equally explicit.

## Required Outcome

State that SCI is capability-limited, not termination-proof. Decide whether
process-level time/memory limits are warranted by the CLI boundary. Do not add
a complex evaluator budget without measured field evidence, but do not claim a
general nontermination guard from a symbol denylist.

## Tests and Verification

- Permanent tests retain direct `loop`/`recur` refusal and successful bounded
  `for` comprehension.
- Help and docs use precise capability language.
- If a runtime limit is added, a subprocess test proves bounded refusal and
  unchanged source without hanging the test process.
- Normal real-program X-ray performance remains competitive.

## Done When

The public safety claim exactly matches the mechanism, and any promised
resource bound has a deterministic boundary test.

## Resolution

Help, README, changelog, vision, and both installed agent skills now state that
X-ray is capability-limited, not termination-proof, and that analyzers must
perform bounded work. Permanent tests retain direct `loop`/`recur` refusal,
bounded `for` success, quoted structural-symbol search, no-source-I/O refusal,
and the exact help/skill contract. No process-level runtime budget was added:
field evidence supports the capability boundary, but does not yet justify a
new evaluator mechanism.
