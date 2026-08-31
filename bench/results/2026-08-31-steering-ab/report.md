# Receipt steering A/B result

## Verdict

Primary: S 1/6; C 0/6; lift 16.667 percentage points.
Safety gates: passed. Exactness S 6/6, C 6/6; wrong-subject 0.
Preregistered <25pp kill: triggered.

The data-only receipt signal did not clear the frozen steering bar. It changed the prepared-route choice in one of six signal cells and zero of six controls, but that 16.7-point lift is below 25 points. No product promotion is supported.

## Secondary outcomes

Median edit-2 caller emission: S 2015.5 bytes; C 2154.5 bytes; delta -139 bytes (-6.5%).
Median edit-2 wall: S 82.428 s; C 74.121 s; delta 8.307 s (11.2%).
The sole prepared adopter (04) emitted 3527 bytes over 2 edit calls and took 138.048 s. Its first fill key was ["to"] and refused; it recovered with ["arguments.edits[0].to"].

## Route detail

Sol routes S: 01=ordinary-full, 04=confirm-fill, 06=ordinary-anchor, 07=ordinary-full, 09=ordinary-anchor, 12=ordinary-anchor.
Sol routes C: 02=ordinary-full, 03=ordinary-anchor, 05=ordinary-anchor, 08=ordinary-full, 10=ordinary-anchor, 11=ordinary-full.

Six of the eleven non-prepared main cells compressed the supplied whole-form change to a tiny owner-scoped anchor edit. This is a post-frozen descriptive split, not a change to the primary score. It matters to interpretation: the 444-token signal priced confirm+fill against the canonical 3,551-byte full-form call, while many callers independently found an even smaller ordinary path.

Spark bonus: B-C1=ordinary-full, B-S1=ordinary-anchor.

Qualitative signal mentions: 0.

## Identity and claim boundary

Published product base: 469141bdd3144a94a4e4ea2ed99c7ecd6ca26f5b. Preregistration: b36999d4e4029088a2d3ea00a835fc6dd21dfb4e.
This experiment answers only whether the four-field in-flow receipt signal steered the next path choice on this supplied wall-sized edit. It does not establish a general latency win or authorize a product field.
