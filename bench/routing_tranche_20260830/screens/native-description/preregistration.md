# Frozen screen: explicit routing rule in the native description

Hypothesis: guidance attached to the native tool Codex already intends to use
is more salient than guidance attached only to the structural tool.

The sole arm difference is the exact sentence appended to `native_patch` in B:
“For bounded edits to existing Clojure forms, call `edit_clojure` instead; use
this tool for prose, new files, or unsupported changes.” The structural tool,
schemas, behavior, position, prompt, fixture, and scorer are unchanged.

Prediction: A 25% and B 50% structural-first, a +25pp lift. Kill if lift is
under 15pp or reverses on more than one-third of the 12 fixtures (more than 4).
The frozen 2-run A pilot must be valid and below 2/2 structural before the
48-start cohort. Wrong-subject must be 0.
