# How Astra actually used Surgeon — the 2026-09-05 helper_extraction window

Read-only study of `gpt-6-astra`'s use of the clj-surgeon MCP server during his
four-hour helper-extraction window **2026-09-05 04:55:58–08:55:58 UTC**, plus the
session immediately preceding it (00:00–04:55:58 UTC). Nothing was started, mutated
or merged to produce this note.

**Independent evidence used.** (a) the seat's default-root service telemetry,
`~/.local/state/clj-surgeon/telemetry/*.jsonl`, session `4d13809f-7916-40ea-8e9e-058c40741323`
(the 8171 server's events; I re-derived every count below from the raw JSONL rather
than copying his figures); (b) his nine owned public servers' telemetry dirs under
`/var/tmp/forge/astra-helper-program/runs/*/server/telemetry`; (c) the real caller's
rollout `runs/helper-usability-01-astra-tool/arm/rollout.jsonl`; (d) his published
hour studies, report and usability review on `origin/MCP/astra-helper-performance-2026-09-05`;
(e) `/var/tmp/forge/astra-fable-coordination.md`.

**Replication note.** His four published per-hour service figures reproduce *exactly*
from the raw telemetry — 21/15ok/6ref/1.391 s; 4/4ok/192 ms; 4/4ok/217 ms; 7/6ok/1ref/122 ms.
His counting is trustworthy; the numbers below are mine, not his, and agree.

---

## 1. Headline — Astra's Surgeon uses by purpose (04:55:58–08:55:58 UTC)

| Purpose | Calls | ok / refused | Service wall | Native alternative he used |
|---|---:|---:|---:|---|
| **Review builders' in-progress Clojure source** (`inspect_clojure` outline+forms on `helper_extraction.clj`, `mcp_helper_extraction.clj`, planner/boundary/tests) | 30 | 24 / 6 | 1,840.9 ms | `cat` / `rg` — his own hour-3 collector counted **100 `:cat` and 17 `:ls`** in the same hour as 3 `inspect_clojure` |
| **Derive the extraction plan on the real application** (`match` ×4 + `forms` ×2 on `src/cfp_scheduler_killer/web/http.clj`) | 6 | 5 / 1 | 81.5 ms | `rg` for the six helper names (the caller ran exactly that first) |
| **`helper_extraction` public calls** (8 quality checks + 2 usability attempts, his own 8300–8339 servers) | 10 | 9 / 1 | **not derivable from service telemetry** — see row below; client receipts give 8.890 s + 24.128 s for the usability pair and 16.1–27.0 s for the quality RPCs | a 23,193-byte hand-written `apply_clojure_changes` request (the interface this verb replaces) |
| **`xray`** | 0 in window | — | — | 5 xray sub-requests in the *preceding* session (00:00–04:56 UTC); the hour-2 collector separately counted 2 `xray` as **CLI**, not MCP |
| **Alias migration** | 0 calls named `alias_migration` | — | — | the 21-file/63-site migration ran through `apply_clojure_changes`: **1 ok, 9,208.4 ms, 292 edits / 30 files** and **1 refused (`invalid-intent-form`, 5.4 ms)**, both in the preceding session |
| **Refusals** (all tools, window) | 7 | — | 157.0 ms | 3× `batch-form-selection-failed`, 2× `invalid-mcp-request`, 1× `source-file-not-found`, 1× `invalid-source` |
| **Telemetry blind spot** | 9 owned servers | — | — | **Verified: each of the 9 `runs/*/server/telemetry` dirs contains exactly one line, `server.start`. Zero `tool.call`.** Every `helper_extraction` RPC is invisible to the service view |

**Window totals, all tools:** 36 `tool.call` events, **all `inspect_clojure`**; 29 ok /
7 refused; **1,922.4 ms** total service wall (median 37.3 ms, max 274.8 ms); 36 file
reads; 129,618 source characters. Sub-operations: `forms` 22, `outline` 11, `match` 4,
1 malformed. **Zero mutating MCP calls in the four-hour window.**

**Preceding session (2026-09-05 00:00–04:55:58 UTC), same telemetry root:** 40 calls —
38 `inspect_clojure` (35 ok / 5 refused) and 2 `apply_clojure_changes` (1 ok / 1 refused);
18.93 s total, of which the single successful 30-file commit is 9.21 s. Sub-operations:
`match` 254, `outline` 53, `forms` 27, `xray` 5.

---

## 2. Findings

### What he used Surgeon FOR

**Reading other agents' code, not writing his own.** All 36 window calls are reads.
Twenty-four successful reads landed on the source Fable's builders were actively
changing — `src/clj_surgeon/helper_extraction.clj` (7 calls), `mcp_helper_extraction.clj`
(4), the two helper test namespaces (7), plus `mcp_tool.clj`, `mcp_schema.clj`,
`mcp_extraction.clj`, `mcp_alias_migration.clj` and `oracle/structure.clj`. Latency was
10–275 ms, median 37 ms. That is the review loop his coordination notes describe:

> "Surgeon read showed that directly(62.1ms); one outline(39.4ms) found actualowner
> finding-1-a-throw-after-a-real-kernel-commit-does-not-leave-the-write-standing. Read
> that plus real-commit!(125.3ms) on frozenserver-src, sourcehash37bd3614"
> — `/var/tmp/forge/astra-fable-coordination.md`, line 755

Those three specific reads are **not present in the default-root telemetry file** for
either window; they were served by a server whose workspace root differs from the one
the seat's collector scans. That is his own coverage finding, reproduced here.

**Deriving a plan on the real application.** At 08:21–08:23 he switched roots entirely,
firing 4 `match` and 2 `forms` calls at `src/cfp_scheduler_killer/web/http.clj` — the
file the caller cohort would extract from. Six calls, 81.5 ms, 963 source characters.
This is the cheapest and most on-purpose use in the whole window.

**Public `helper_extraction` calls.** Ten RPCs are evidenced by retained client receipts:
four positive/rollback quality pairs (`public-positive/rollback-01..04`) and the two
attempts inside `helper-usability-01-astra-tool`. Nine succeeded as RPCs. Their durations
come from three different estimands (server operation time, whole client RPC, wrapper
envelope) and he is explicit that they must not be summed:

> "Never add nested RPC, wrapper and proof durations."
> — `2026-09-05-astra-helper-hour-2-usage.md`

### What he did NATIVELY instead, and why

- **Every write outside the two `apply_clojure_changes` calls.** His own primary study
  records the native arms as "one direct Python rewrite batch per arm, preserving
  selected text by assertions" (`evidence/astra-primary-2026-09-05/report.md`). Python
  generators, not per-file edits — the native baseline is a batching baseline.
- **All discovery.** The real caller's first three actions are `cat CLAUDE.md`,
  `cat src/cfp_scheduler_killer/web/http.clj`, `cat .clj-surgeon.edn`, `git status --short`
  and one `rg` for the six helper names (rollout lines 13–28) — **38.2 s before the first
  helper attempt**, rediscovering references the verb computes itself.
- **All prose, docs, new-file construction and process orchestration.** He states this is
  legitimate: *"Native construction of new files and prose remains legitimate; read-route
  totals do not by themselves show avoidable Clojure reads."* (hour-1 study).
- **Claude-side builders: 0 recognized Surgeon calls across all four hours** (245 / 120 /
  51 / native-read actions respectively). He refuses to read that as a result:
  *"zero recognized Claude calls does not establish zero actual usage."*

### Frictions he hit

1. **Directory vs glob scope authority — the one real caller friction.** `scope.paths:
   ["src","test"]` refused as `helper-extraction-caller-outside-scope` after **8,890.43 ms**;
   `["src/**","test/**"]` committed in **24,128.12 ms**. Rollout lines 34/37/41/44. The
   caller recovered unaided, but paid ~9 s and one round trip for a distinction the API
   never signalled.
2. **The refusal envelope omits `operation`.** From the usability review:
   > "The first typed refusal omits operation. Existing usability/calls.json leaves its
   > outer4 typed_helper_refusals empty and observes only the later operation-bearing
   > helper result. That is a classification blind spot, not absence of a refusal."
   A refusal that cannot be keyed is a refusal that silently drops out of every adoption rate.
3. **Emitter absence.** Verified independently above: 9/9 owned server telemetry dirs hold
   only `server.start`.
   > "each owned public01 server telemetry directory contains only its server.start event,
   > no helper_extraction tool.call. This is actual emitter absence, not only default-root
   > scan coverage. … Any current service-only adoption totals undercount these calls."
   > — coordination file, line 719
4. **The client collector could not see his own wrapper.** His Python HTTP wrapper was
   unrecognised, so the first hour-1 collection reported **zero** Surgeon calls while the
   service had served 21:
   > "Client collector incorrectly records zero Surgeon calls because our Python HTTP
   > wrapper is not recognized … Thus client adoption/route counts are incomplete, not
   > zero usage." — coordination file, line 659
   Repaired on `MCP/astra-mcp-wrapper-usage` (`bc9b26cf`), review-only; hour-2 then
   recognised "2 inspect, 2 helper extraction" wrapper operations.
5. **Six self-inflicted read refusals in one hour**, all caller-shape errors: three
   `batch-form-selection-failed` (form names that did not exist in the evolving file), two
   `invalid-mcp-request` (`{files, view}` instead of `{requests:[{operation,file}]}`), one
   `source-file-not-found` and one `invalid-source` on a file being written concurrently.
   The two `invalid-mcp-request` calls cost 2.3 ms and 1.0 ms — cheap, but they are the
   shape a first-time caller guesses.
6. **A documentation-induced refusal in the preceding program**, his verdict:
   > "Our design agent followed repository guidance to set include_source=false, but8171
   > rejected it both top-level and per-request. … This is a real self-inflicted refusal
   > even in our careful sessions." — coordination file, line 103; his ruling at line 112
   was *"Do NOT commission a serverfix"* — a docs clarification, not a feature.

### Adoption signals

- **6/6 optional callers chose the tool** in the earlier cohort:
  > "Six of six optional callers chose Surgeon and completed both proofs. That is
  > encouraging adoption evidence, not proof of general superiority."
  > — coordination file, line 515; re-confirmed at line 425 ("optional adoption6/6, no
  > native fallback, one recovered profile refusal only") and line 431 ("optional
  > adoption3/3 per model").
- **The one real LLM caller completed in two attempts**, 5 outer actions, 86.1 s actor
  wall, with an in-transaction proof and an independent after-return proof: 6 helpers,
  28 caller files, 258 sites, 172 retained uses, 30 files changed.
- **Request collapse is the measured win**: 23,193 B → 406 B canonical UTF-8, **57.13×**.
- **No new speed win**: *"this four-hour window produced no valid new speedup versus native"*
  (`2026-09-05-astra-helper-gene-peek-report.md`). Prior paired medians stand: Sol 3.319×,
  Astra 1.240×, and Astra's advantage is *"nearly exhausted by a fresh server startup"* (1.021×).

---

## 3. Learnings → ratchets

| Learning | Ratchet the product should carry |
|---|---|
| His single highest-frequency use was **reading someone else's half-written file** (30 of 36 calls), and 3 of the 6 read refusals were stale form names in a file being edited underneath him | A review-oriented read mode: given a file and a git ref/hash, return the outline *and* tolerate form names that no longer resolve, naming them as missing instead of refusing the whole batch. `batch-form-selection-failed` should degrade to a partial result with a typed miss list |
| `scope.paths ["src"]` refused where `["src/**"]` was required, costing 8.9 s on the only real caller | Accept a directory as an authority root, or refuse **before** doing 8.9 s of work with the corrected value in `next_call`. A refusal that took longer than a third of the successful run is a design defect, not a user error |
| The typed refusal lacked `operation`, so it vanished from the operation-keyed collector | Every refusal envelope carries `operation` (and `tool`) as required keys, with a witness that a refusal is countable by the same key path as a success. This is the `text ⊇ structured` class again |
| 10 `helper_extraction` RPCs, 0 telemetry events; 9/9 servers emitted only `server.start` | Route new verbs through the existing service emitter as part of the verb's definition of done, and add a gate: a verb with no `tool.call` event on a successful integration run fails the release. His own board line: *"Emit helper outcomes through the existing service emitter and cover refusal envelopes"* |
| The seat's collector reported **0** while the service had served **21** | Client-side route counting is not a meter. Adoption figures must be derived from service events, or explicitly labelled as attempt counts — his practice of never adding the two populations should be the documented rule |
| Two `invalid-mcp-request` refusals used `{files, view}` — a plausible-looking shape | Refusals for malformed top-level shapes return the minimal valid request for the intent, not just an error type |

---

## 4. What is unknown, and how to measure it next

- **Whether the 62.1 / 39.4 / 125.3 ms reads he cites went through 8171.** They are absent
  from the default-root telemetry for both windows. *Fix:* stamp `workspace_root` (or a
  server identity) into every `tool.call` event and have the collector union all roots.
- **Success/failure counts from the client collector.** His hour-3 note is explicit:
  *"A success count cannot be obtained by subtraction."* 120 occurrences, 24
  refusal-*bearing* actions, 6 execution-error actions — overlapping and inflated by
  deliberate negative tests. *Fix:* the emitter ratchet above makes subtraction unnecessary.
- **Whether Claude builders truly made zero Surgeon calls.** Not derivable from the
  evidence: the collector sees Claude's shell actions, not its MCP frames, and tool
  availability per builder was never reconstructed. *Fix:* record MCP tool availability at
  session start alongside the session id.
- **Whether the six read refusals were avoidable.** Not derivable — the files were being
  written concurrently. *Fix:* include the file hash the caller believed it was reading in
  the request, so a stale-form refusal can be labelled "file changed" versus "wrong name".
- **Whether the compact intent reduces caller *preparation*.** The 38.2 s of rediscovery
  before the first helper attempt is one observation. *Fix:* his own next step — six native
  controls on the same task, then a paired screen, before any speed claim.
- **What `helper_extraction` costs as a single estimand.** Three overlapping clocks exist
  and none is the answer. *Fix:* one server-reported operation time per RPC, emitted, and
  never reconstructed from wrapper envelopes.

---

*Written read-only from committed branches, retained receipts and raw telemetry. No server
was started; no file under `/var/tmp/forge/astra-*` was modified.*
