---
name: lower-cost-of-change
description: >-
  Make a software-development feedback loop live, fast, and provable. Use when
  tool, server, plugin, MCP, schema, index, or agent-guidance work repeatedly
  requires restarts, reinstalls, reconnects, rediscovery, broad test runs, or
  manual verification before the next experiment can run.
---

# Lower the cost of change

Optimize the complete `edit -> load -> discover -> exercise -> verify` loop.
Do not optimize one command while another repeated boundary dominates wall time.

## Establish the loop

1. Read the repository's development entrance and nearest hot-reload guidance.
2. Write the current loop as five timed stages: edit, load, discover, exercise,
   verify. Count human or agent actions as well as wall time.
3. Classify each boundary:
   - **hot:** the next call observes the change;
   - **warm:** one explicit reload or focused command is required;
   - **cold:** a process, client session, install, or broad suite must restart.
4. Fix the most frequently paid cold boundary first. Preserve stable URLs,
   ports, process identities, and public entrances whenever possible.

Use the hottest capable entrance throughout the work. Do not invoke a
process-starting CLI when an already-connected persistent tool exposes the
same contract.

## Build the live path

- Route implementation changes through live indirection: reloadable Vars,
  modules, handlers, or registries instead of captured function values.
- Keep the previous working implementation active when a reload fails. Report
  the failed file and diagnostic without killing the serving process.
- Make data and indexes atomically replaceable and read them at request time.
- For MCP tool names, descriptions, annotations, or schemas, advertise
  `tools.listChanged`, update the live registry, and send
  `notifications/tools/list_changed`. Prove that the client re-lists tools.
- Reserve process restarts for dependency, runtime, or transport changes. A
  server restart behind a stable URL must not imply an agent-session restart.
- Put the complete entrance behind one repository command. Status output must
  name the live process, endpoint, source revision, reload state, and any cold
  action still required.

## Test before relying on it

Add the smallest permanent tests that prove the boundary:

- implementation change becomes visible on the next call;
- syntax or load failure preserves last-good behavior;
- schema replacement changes `tools/list` in one connected session;
- tool addition and removal produce the same live result;
- notification output does not corrupt the protocol transport;
- focused verification catches the changed contract;
- the documented one-shot command exercises the real service.

For a client whose dynamic refresh behavior is uncertain, run a live
acceptance test before claiming restarts are gone. Distinguish server support,
client support, and model-visible availability.

## Keep the fast path honest

- Run a focused deterministic gate after each change and the full release gate
  before completion. Do not weaken existing tests to make the loop faster.
- Record before/after median wall time and action count on the same task.
- Treat a faster incorrect or stale result as a failure.
- Update the repository instructions so the next agent uses the live path
  without rediscovering it.

The completion condition is concrete: make a representative implementation or
schema change, observe it through the existing client without an unnecessary
restart, run the focused gate, and produce one trustworthy status receipt.
