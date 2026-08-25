# Captain's Log: the compiled cleanup hit 4.61x

**Date:** 2026-08-25  
**Status:** Replicated locally and on Anvil; exact-byte gated  
**Decision:** Ship compact structural transactions for fully supplied cleanup decisions; keep native editing as the default outside that boundary

## The result

A compiled `edit_clojure` transaction beat native read plus `apply_patch` by
**4.61x median end-to-end** on Anvil. This is the 2--5x complete-task win we
were looking for, on a capsule derived from an actual production extraction
rather than a repeated-site toy.

The frozen task reconstructs the source-side cleanup of
`sessionize-sched-killer` commit `557684689060ec87ee16b4faaa6558fe9081e7c6`:

- one 485-line namespace;
- 22 obsolete top-level owners, including attached comments;
- one namespace require rewrite;
- seven route rewrites; and
- 30 exact edit intents in total.

The function bodies are anonymized, but the owner count, change topology, and
441-line extraction scale come from the real commit. The decision is fully
supplied to both arms. Scoring requires exact final bytes.

## Three independent Anvil pairs

All callers used fresh `gpt-5.6-sol` / high turns at commit `8b4178c`. Each
seat owned an independent checkout. The benchmark started a private MCP server
at `-Xms64m -Xmx512m`; the live shared MCP was not part of the timed route and
remained untouched.

Anvil was `anvil-server`, one 16-core AMD EPYC-Genoa socket with 30 GiB RAM.

| Seat and CWD | Order | Compact | Native | Paired speedup |
|---|---|---:|---:|---:|
| `dev-a` — `/srv/fleet/dev-a/clj-surgeon-benchmark-8b4178c` | compact first | 34.078 s | 144.729 s | 4.25x |
| `dev-b` — `/srv/fleet/dev-b/clj-surgeon-benchmark-8b4178c` | native first | 31.658 s | 167.228 s | 5.28x |
| `dev-c` — `/srv/fleet/dev-c/clj-surgeon-benchmark-8b4178c` | compact first | 32.546 s | 150.138 s | 4.61x |

All six final files were byte-exact. Every compact mutation succeeded in one
call with no failed mutations. The median was 32.546 seconds compact versus
150.138 seconds native: a 117.592-second saving, **78.3% lower wall time**, and
**4.61x throughput**. Compact won all three pairs.

Durable evidence lives at:

```text
/srv/fleet/dev-a/clj-surgeon-study-results/20260825T074000Z-public-cfp-cleanup-sol-high-dev-a/results
/srv/fleet/dev-b/clj-surgeon-study-results/20260825T074000Z-public-cfp-cleanup-sol-high-dev-b/results
/srv/fleet/dev-c/clj-surgeon-study-results/20260825T074000Z-public-cfp-cleanup-sol-high-dev-c/results
```

## What actually got faster

This was not a faster parser beating a slow patch engine. The structural work
inside Surgeon is tiny relative to a Sol/high turn. The interface changed how
much source the model had to perceive and reproduce:

```text
Compiled route
  supplied decision
    -> one compact structural transaction
    -> resolve owners + hash-fence frozen bytes
    -> atomic commit + parse/readback
    -> 175-byte terminal receipt

Native route
  supplied decision
    -> inspect 485-line source
    -> keep large deletion and patch context in the model loop
    -> render a large textual patch
    -> apply_patch receipt
```

Every compact run used one tool round trip, zero source commands, and returned
175 bytes of tool output. Native used two to five tool round trips. Its source
commands returned 19,379 to 77,516 bytes. Native output-token counts were
6,372, 8,640, and 7,631; compact used 1,369, 1,134, and 1,093.

The useful complexity claim is therefore about the model boundary:

```text
compact representation ~= O(owner names + small literal rewrites)
native representation  ~= O(source read + changed source rendered as patch)
```

Both engines can batch a write. Surgeon wins this class because the LLM does
not have to manufacture diff context or emit hundreds of deleted lines.

## Why this survived skepticism

The first local version did not work. A real extraction cleanup revealed that
the compact address algebra could name Vars but not the namespace form. The
two obvious guesses refused before mutation. We added the explicit address
`within.namespace`, behavior tests, and a hot-reload proof. A second trial then
exposed an unfair formatting assumption in the prompt; the exact multiline
replacement was supplied symmetrically to both arms.

The smaller production-derived cleanup reached only 1.27x. A 17-owner
historical synthetic calibration reached 1.66x replicated. The 485-line
cleanup first reached 2.21x in one local canary, then 3.16x over three local
pairs, and finally 4.61x over three independent Anvil pairs. The result grew
with the amount of source native had to ingest and reproduce, as predicted.

We also audited an apparent zero-retry native row. One local native caller had
actually made a safe refused patch attempt that Codex recorded only in router
stderr, not the JSON file-change stream. The benchmark now counts that evidence
and its self-test preserves the fix. The Anvil results above had no stderr
failures, so the remote advantage does not depend on retry penalties.

## The honest claim boundary

This evidence does **not** say that Surgeon is universally faster than native
editing. It does not measure discovery, semantic design, moving bodies into a
new file, or writing new prose. It is one source namespace, and the caller is
given the complete desired decision. Native remains fearsome for one small
visible edit, prose, new files, and unsupported transformations.

It does prove a valuable routing rule:

> When a Clojure cleanup decision already names many owners and a few small
> rewrites, compile that decision directly. Do not make the LLM reread and
> retype the source merely to satisfy a textual patch format.

That is the church-organ interaction we wanted: visualize the change, play one
guarded chord, and receive terminal proof.

## A second Anvil stratum kept the claim honest

After the 4.61x result, the generic Anvil pair runner replayed the frozen
`decision-batch-edit` capsule at commit `34de9b5`. This is a real-task-derived,
two-file decision with six small, heterogeneous replacements. It is not a mass
owner deletion. Fresh Sol/high callers again used counterbalanced order and
exact-byte scoring.

| Seat and CWD | Order | Compact | Native | Compact advantage |
|---|---|---:|---:|---:|
| `dev-a` — `/srv/fleet/dev-a/clj-surgeon-benchmark-34de9b5` | compact first | 30.717 s | 32.760 s | 6.2% lower wall |
| `dev-b` — `/srv/fleet/dev-b/clj-surgeon-benchmark-34de9b5` | native first | 29.893 s | 31.378 s | 4.7% lower wall |
| `dev-c` — `/srv/fleet/dev-c/clj-surgeon-benchmark-34de9b5` | compact first | 28.047 s | 30.060 s | 6.7% lower wall |

All six trials were exact. Compact won every pair, with median wall 29.893
seconds versus 31.378 seconds for native: 1.485 seconds and 4.7% lower wall,
about 1.05x throughput. Compact used one MCP action, no source read, and a
174-byte receipt in every run. Native used one source read plus one patch and
returned 1,804--2,013 source bytes.

This falsifies “batching alone produces 2--5x.” Both routes already fit in one
model turn and six small replacements do not make native reproduce much text,
so the common model floor dominates. The 4.61x mechanism is narrower and more
valuable: owner-level intent avoids reading and rendering hundreds of lines
during extraction cleanup. The product should route both strata through
compact editing because it remained exact, first-attempt safe, and slightly
faster, but it should claim a decisive speedup only for source-volume-eliding
transactions.

Durable results:

```text
/srv/fleet/dev-a/clj-surgeon-study-results/20260825T082845Z-decision-batch-dev-a
/srv/fleet/dev-b/clj-surgeon-study-results/20260825T082845Z-decision-batch-dev-b
/srv/fleet/dev-c/clj-surgeon-study-results/20260825T082845Z-decision-batch-dev-c
```

## Kent Beck's contribution

Every cumbersome step paid rent:

- inability to address `ns` became `within.namespace`;
- repetitive owner deletion became `delete_owners`;
- four synchronized benchmark case tables became self-registering capsules;
- hidden native refusals became a durable stderr-backed metric; and
- manually drifting Codex, Claude, and root skill copies became one canonical
  package with `make sync-clj-surgeon-skill` and a mandatory mirror check.

Branch reconciliation found one more useful change stranded in the Anvil
deployment worktree. `inspect_clojure` now accepts `include_source=false` for
questions that need only form identity, line range, hash, count, or source
anchor. A live laptop probe against the 2,676-character
`validate-inspect-params` form returned its complete proof metadata and no
source body. This makes the common “I need the address, not the code” read
cheaper without weakening the hash-backed evidence. The default remains source
included so edit decisions do not pay a second read.

The next experiment should preserve this standard: choose another historical
counterfactual whose decision is fully reconstructable, prefer a genuinely
multi-file and noncontiguous cleanup, and add only the smallest missing
primitive that real friction justifies.

## Production rollout

The complete `make test` gate passed at commit `5e85987`:

- 605 Babashka tests and 5,197 assertions;
- 195 MCP tests and 1,615 assertions at `-Xmx512m`;
- four-tool stdio discovery and smoke;
- portfolio, schedule, harness, retention, and evidence self-tests; and
- zero failures or errors.

After rescuing metadata-only inspection from the deployment branch, the full
gate passed again at `716e7ac`: 605 Babashka tests with 5,197 assertions and
197 MCP tests with 1,626 assertions, plus the same heap, lifecycle, smoke,
benchmark, retention, and evidence gates.

A rollback-armed Anvil promotion then proved the exact new behavior twice. An
isolated port-17888 canary performed one namespace rewrite, one named-form
rewrite, and one owner deletion in a single `edit_clojure` request and matched
the expected bytes. Production port 7888 repeated the same acceptance before
the promotion was declared successful. The follow-up `e7f72e2` promotion also
proved `include_source=false` on both ports: proof metadata and source anchor
were present, while the selected form's source body was absent.

The live Anvil process is:

```text
PID:  739989
User: surgeon
CWD:  /srv/fleet/shared-tools/clj-surgeon-e7f72e2
Heap: -Xms64m -Xmx512m
```

The rollback receipt is
`/home/surgeon/.local/state/clj-surgeon/deployments/2026-08-25-e7f72e2/ROLLBACK.txt`.
The existing clojure-lsp process was not restarted: PID 2400995, CWD
`/home/surgeon/clj-surgeon`.

The matching CLI and advanced-only Codex/Claude skill package was installed for
dev-a, dev-b, and dev-c from `e7f72e2`. Their study workspaces advertise all
four tools on the shared production URL. The previous hand-managed Claude skill
directories were preserved, not deleted, at each seat's
`~/.claude/skills/clj-surgeon.pre-5e85987-20260825`. Fresh sessions see the new
package; already-running clients whose cached schema rejects `delete_owners` or
`within.namespace` need one new agent session, not a service restart.

The laptop installation also points CLI, Codex, and Claude at `e7f72e2`. Its
live MCP remained PID 75495, CWD `/Users/genekim/src.local/clj-surgeon`, and a
direct production `tools/list` confirmed compact top-level fields
`delete_owners`, `edits`, `programs`, and `workspace_root`, plus both
`within.form` and `within.namespace`.

## The capability became a fleet default

The benchmark win did not automatically change agent behavior. The installed
advanced skill deliberately excludes ordinary `inspect_clojure` and
`edit_clojure` calls because their tool schemas and always-loaded routing are
supposed to cover the common path. An acceptance audit found that this
assumption was true on the laptop but false on Anvil: dev-a, dev-b, and dev-c
had the tools and skill package, but no Codex global `AGENTS.md` rule that told
fresh callers when to use the compact route.

Commit `6ff11c9` made that contract installable. One canonical, versioned block
now teaches both Codex and Claude to:

- batch known structural reads and omit source only for metadata questions;
- use one `edit_clojure` transaction for a complete mechanical decision;
- use `within.form`, `within.namespace`, and `delete_owners` instead of prompt
  workarounds;
- trust frozen-snapshot guards and terminal mutation evidence; and
- keep native patching for small visible edits, prose, new files, and
  unsupported operations.

The installer preserves every unmanaged byte, preflights all targets before it
writes, refuses malformed or duplicate markers, writes atomically, and makes a
second run byte-identical. `make install` includes the block;
`make check-agent-routing` is its non-writing acceptance gate.

The rollout found one more gap that a receipt-only audit would have missed.
Anvil Codex was registered for the shared MCP, but `claude mcp list` reported no
servers on all three seats. We added `http://127.0.0.1:7888/mcp` at Claude user
scope and required a live health check. The resulting fleet state is:

| Surface | Exact routing block | Codex shared MCP | Claude shared MCP | CLI and skills |
|---|---|---|---|---|
| Skiff laptop | checked | registered | connected | `6ff11c9` |
| Anvil dev-a | checked | registered | connected | `6ff11c9` |
| Anvil dev-b | checked | registered | connected | `6ff11c9` |
| Anvil dev-c | checked | registered | connected | `6ff11c9` |

The Anvil service remained PID 739989, CWD
`/srv/fleet/shared-tools/clj-surgeon-e7f72e2`, at `-Xmx512m`. This rollout did
not restart it or the existing clojure-lsp PID 2400995, CWD
`/home/surgeon/clj-surgeon`.

The guarantee has one honest time boundary. Every **new** managed Codex and
Claude session now loads the routing block and can reach the tool. An existing
model context can retain old instructions or a cached MCP schema. If such a
session rejects `delete_owners` or `within.namespace`, start a new agent
session; do not restart the shared JVM.

The rollout's constituent gate passed 609 Babashka tests with 5,235 assertions
and 197 JVM MCP tests with 1,626 assertions at `-Xmx512m`. The unchanged stdio
smoke advertised all four tools and returned all three expected responses on
Anvil in 7.52 seconds and on the laptop in 55.53 seconds. Its first laptop run
had timed out with zero responses while system load was about 277. This was a
scheduler failure after both main suites had passed; the unchanged retry passed
as load fell. All post-smoke usage, benchmark, retention, and evidence gates
also passed.

The complete mechanism and routing boundary are summarized in
[Compiled editing: the route that can beat native patching](../compiled-editing-playbook.md).
