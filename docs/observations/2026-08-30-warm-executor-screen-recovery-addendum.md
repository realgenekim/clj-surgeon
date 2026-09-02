# Warm-executor screen: frozen recovery addendum

Status: **FROZEN BEFORE ANY RECOVERY MODEL TURN**

Parent preregistration: `d1ce1b2`

Attempt 1 reached two deterministic harness failures:

1. all ten cold-trivial turns completed validly (five per model);
2. all ten cold-prepared transport attempts reached the exact requested MCP
   call, then were refused before mutation because the config used approval
   mode `writes` while the non-interactive thread used approval policy `never`;
3. the runner then failed before the first warm model turn because it had not
   created the warm app-server log directory.

Every attempt-1 receipt is retained. The prepared attempts are invalid
transport observations: the model chose the correct tool and exact arguments,
the tool did not execute, all fixtures stayed unchanged, and wrong-subject was
zero. They do not enter the prepared-latency or reliability estimands.

This addendum freezes exactly three mechanical recovery changes:

- create each app-server log directory before opening stderr;
- set the sole enabled MCP tool's documented approval mode to `approve`, which
  preapproves that tool while retaining thread approval policy `never`;
- reuse the ten valid cold-trivial observations, rerun the invalid
  cold-prepared cell in full, and run the previously unstarted warm cells.

The recovery also copies the non-secret Codex config beside each protocol log
and removes runtime `CODEX_HOME` directories after use so no auth symlink or
session store enters Git.

No model, order, prompt, fixture, guard, scorer, prediction, exclusion rule,
magnitude threshold, sample size, amortization formula, drift rule, or winning
gate changes. No recovery model turn may run until this addendum and its
mechanical runner change are committed and pushed.
