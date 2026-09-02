# Warm-executor screen: registered replacement invocation

Status: **FROZEN BEFORE THE REPLACEMENT MODEL TURN**

Parent preregistration: `d1ce1b2`

Recovery addendum: `2855471`

The recovered fixed schedule produced 4/5 exact Spark cold-prepared bangs and
5/5 exact Sol cold-prepared bangs. Spark replicate 2 was a clean transport
failure: the thread reported that no `edit_clojure` MCP tool was available,
made no tool call, changed no bytes, and had wrong-subject 0.

The original preregistration permits up to two retained replacements after the
fixed schedule. This invocation uses that rule without changing the route:

- run Spark replacement replicate 6 against the same bang-1 fixture and prompt;
- if and only if replicate 6 is transport-invalid, retain it and run replicate
  7, the second and final allowed replacement;
- stop after the first exact replacement;
- retain the invalid replicate 2 in the committed results;
- compute the matched Spark cold median from five exact observations only.

The summary fold is clarified to report both total prepared attempts and exact
prepared observations. No scorer, latency, prediction, reliability threshold,
amortization formula, drift rule, or winning gate changes.
