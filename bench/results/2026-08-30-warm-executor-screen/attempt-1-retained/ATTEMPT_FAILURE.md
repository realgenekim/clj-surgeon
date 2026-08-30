# Attempt 1 retained failure

Frozen runner SHA: `d1ce1b2` / runner hash recorded in `meta.json`.

Completed before failure:

- cold trivial: 5 valid turns per model;
- cold prepared transport attempts: 5 per model, all deterministically refused
  before mutation because the generated client config requested write approval
  while the non-interactive thread correctly used approval policy `never`;
- no wrong-subject mutation occurred;
- no warm model turn started.

The process then failed while constructing the first warm app-server because
the runner had not created its log directory:

```text
FileNotFoundError: [Errno 2] No such file or directory:
.../warm/spark/app/app-server.stderr
```

The recovery addendum preserves every attempt, reuses the valid cold-trivial
cell, reruns the invalid prepared transport cell after preapproving the sole
enabled MCP tool, and runs the previously unstarted warm cells. No hypothesis,
prompt, fixture, metric, magnitude threshold, or scorer changes.
