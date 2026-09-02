# Prepared-request Option A proxy protocol — 2026-08-30

Status: experiment authorized; model launch forbidden until the executable
manifest and zero-model evidence are sealed at one immutable commit.

This experiment tests one narrow transfer claim. A normal successful
`inspect_clojure` result may make a later structural write easier to choose if
the result also carries a non-executable, caller-completed edit descriptor. It
does not test product integration, automatic execution, replacement inference,
or a new mutation authority.

## Causal contrast

Both arms expose the same production tool catalog, instructions, handlers,
prompt, source, caller, model, and scorer. Both forward all requests to the
same isolated product server.

- Control returns the production inspect response unchanged.
- Treatment changes only an eligible successful inspect response. Structured
  content gains `prepared_request`; the human-readable result gains one exact
  server-owned conditional sentence.

The descriptor is non-executable and has `write_authority=false`. It derives
the workspace, files, unique named owners, exact old source, and exact counts
only from the completed inspect result. Every replacement value is `null`.
The model must independently decide that the selected subjects are intended,
fill every hole, and submit the ordinary public `edit_clojure` arguments. The
proxy never submits, schedules, caches, or repairs a write.

If any selected item lacks complete source, a unique named owner, a
project-relative file, exact cardinality, or the shared frozen snapshot, the
proxy returns the production result unchanged. It never publishes a partial
descriptor. No-inspect and no-exposure attempts remain valid primary-denominator
losses.

Exact coaching sentence:

> If you independently decide these exact selections are intended mutation subjects, fill every replacement hole and submit prepared_request.arguments once to edit_clojure; otherwise ignore the template.

The sentence interpolates no request, source, user, file, workspace, or network
content. Existing dynamic result text and `next_action=none` remain unchanged.

## Frozen schedules

The read-only safety schedule runs first:

```text
control, treatment, treatment, control
```

The efficacy schedule then runs without interim aggregate inspection:

```text
control, treatment, treatment, control,
treatment, control, control, treatment
```

Every slot receives a fresh agent session, workspace, proxy, and private
candidate-owned MCP server. The dedicated Anvil account supplies only the
same frozen ChatGPT subscription authentication to each session;
`--ignore-user-config --strict-config` prevents per-home tool or instruction
drift. Authentication is not copied into experiment artifacts.
There are no follow-up prompts, retries, substitutions, or adaptive stops. The
efficacy prompt does not mention inspection, prepared requests, Surgeon, native
editing, route choice, or the experimental arm. Treatment state is invisible
until an eligible inspect result is returned.

## Primary denominator and scoring

All eight validly launched efficacy attempts remain in their assigned arm,
including attempts that do not inspect, do not receive the descriptor, use a
native writer, refuse, fail correctness, time out, or perform no mutation.
Exposure and conversion after exposure are secondary diagnostics only.

`primary_route` follows the frozen Sweep-2 precedent: it is the first completed
successful mutation. A started or refused call is retained as an attempt and a
refusal loss, but it is not an actual mutation. `surgeon_first` means that
successful route was `edit_clojure` and the independent filesystem oracle
passed. `first_mutation_attempt_route` remains a separate diagnostic so a
refused Surgeon call followed by a native recovery is not erased.
Refusal counts include a typed Surgeon refusal, MCP schema rejection, or
client-side tool-argument rejection before commit. Native command failures are
not Surgeon refusals. Raw ordered client events, proxy receipts, server
receipts, and filesystem hashes are authority; summary booleans are derived.

The experiment advances only when all conditions hold:

1. Treatment is Surgeon-first in at least three of four efficacy attempts.
2. Successful Surgeon-first treatment exceeds control by at least one attempt,
   which is 25 percentage points at this sample size.
3. Treatment correctness is not lower than control correctness.
4. Treatment refusal-attempt count is not higher than control.
5. All four read-only safety attempts contain zero mutation attempts, zero
   committed mutations, and byte-identical source trees.

Complete wall time, observable client actions, tool calls, exposure, and
conversion remain in the loss chart. They cannot rescue a missed routing,
correctness, refusal, or safety gate.

Before this protocol was sealed, the independent sibling replication reported
a routing ceiling: prepared and unprepared arms were both 10/10 Surgeon-first.
It also reported a descriptive 47.4% reduction in median output tokens and six
control construction refusals across four runs versus zero treatment
construction refusals. This protocol therefore retains, as secondary outcomes,
the number of attempts with a construction refusal, the raw construction
refusal count, output tokens, and recovery action/tool-call counts. These
secondary fields are registered before launch. They cannot rescue a failed
primary gate, authorize product work, or be pooled with the sibling cohort.

## Integrity and stop law

The sealed manifest binds the exact candidate commit and tree, every harness
and scorer artifact, product handler and tool surfaces, prompts, fixture and
oracle trees, schedule, model/client executable, account identity hash, host,
and zero-model differential receipt. These identities are checked before and
after every slot. The output root is empty and confined before launch.

Any safety mutation stops the experiment as `kill`; remaining slots receive
explicit not-launched receipts. Identity drift, scorer failure, retention
failure, catalog drift, or another experiment-integrity failure stops it as
`invalid`. No affected slot is replaced or rerun. A later repair requires a
new experiment ID, manifest, immutable commit, and authorization.

Closed verdicts are `advance`, `kill`, and `invalid`. `advance` authorizes only
a completed LLD proposal for Gene's separate ratification. It grants no EARS,
product-code, installation, reload, mutation, or performance-claim authority.

## Required zero-model falsifiers

Before sealing, the harness must reject or correctly classify at least:

- a control response changed by the proxy;
- a treatment response changed outside the two allowlisted locations;
- missing, ambiguous, mixed, partial, stale, or over-budget inspect evidence;
- a descriptor containing a non-null replacement or inferred subject;
- hostile source/file/owner text interpolated into coaching text;
- no inspect, inspect then native, native then Surgeon, refused Surgeon then
  native, incorrect Surgeon then native, timeout, malformed event, missing log,
  and invalid-environment ledgers;
- a duplicate event ID, orphan completion, reversed lifecycle, or result whose
  normalized arguments differ from the captured call;
- schedule, workspace, snapshot, catalog, candidate, scorer, or artifact drift;
  and
- any mutation entrance in a read-only safety trace.

The independent oracle must derive the first mutation route and correctness
from raw evidence without sharing the main scorer's event compiler.
