# Astra — completed native controls C1–C4: usage observations

Read-only snapshot of completed controls only; no D/N comparison, no active-arm inspection, no advice sent to actors, no prompt or cohort file changes. Sources: runner-b/cohort/C{1,2,3,4}/result.json and each receipt-bound rollout. All four rollout hashes matched their saved receipt; task_complete=true. Rollout basenames below identify exact source; line references count JSONL lines.

| Control | Outer exec actions | Copy-through-actor wall s | Total with external proof s |
|---|---:|---:|---:|
| C1 | 5 | 73.826 | 78.483 |
| C2 | 6 | 121.281 | 125.781 |
| C3 | 8 | 110.657 | 115.517 |
| C4 | 7 | 98.436 | 102.986 |

Copy-through-actor wall is result.json.wall_s (seed copy through actor completion); actor_wall_s is the actor subprocess alone. Total includes parent proof and scope audit; the difference is4.500–4.861s, not an isolated JVM startup or pure test-time measurement. All four have correct=true; external gate58tests/190assertions and witness19tests/496assertions passed. No native-versus-tool efficiency inference follows from this subset.

## Dossier uptake and reading

The identical supplied dossier explicitly says discovery is supplied, names 20files/54owners/59sites and includes selected owner source. It forbids general suite/DB/network use but does not forbid focused checks. Every actor begins with a targeted rg over the supplied file list, then checks namespace requires and helper insertion context. These are bounded confirmation reads, not a five-minute open-ended repository orientation. Require aliases and insertion context are real editing obligations beyond a call-site source list. All four also build workspace-wide byte/text/hash snapshots for untouched-file verification. C2/C3/C4 retain full encoded/text snapshots in functions store; C1 switches from full source payloads to hashes after output parsing fails. No inference that every reread is redundant; distinguish supplied owner facts, namespace context and whole-workspace preservation evidence.

## Editing shape: scripts construct one batch; apply_patch performs the write

C1–C3 use Python regex/string substitution plus difflib to generate a native patch; C4 constructs its patch in JavaScript from a saved source snapshot. All adapt existing aliases, add the one missing require, insert the local forwarding helper, and assert the supplied site count. Each successful FileChange covers the batch; none uses one hand-authored patch per owner. C4 emits whole-file replacement hunks, yet its subsequent byte comparison and external scope proof pass. Large patch representation is not actual source churn.

C1 source: rollout-2026-09-06T10-13-07-01a07635-1d6c-73c0-bd67-ca7f57a367ae.jsonl. Lines13/18 read;25 generates full-source JSON plus patch;28 fails JSON.parse on “Warning: t…” output. Line32 retries with hashes/smaller diff;35 records successful FileChange. Line42 reads proof infrastructure and checks exact final hashes. It does not run an internal JVM proof; external proof establishes correctness.

C2 source: rollout-2026-09-06T10-14-26-01a07636-4fd3-7833-baeb-140f6463ddbc.jsonl. Lines13/20 inspect/snapshot;28 generates/applies batch;31 FileChange succeeds. Lines39/47/55 run three self-authored forwarding checks. Outputs43/51 fail first on an unclosed form, then on the check's empty-argument sequence expectation; 59 passes after recording args as vectors. No intervening product edit occurs.

C3 source: rollout-2026-09-06T10-16-31-01a07638-3b3d-7da3-be66-dc04c79eeb5d.jsonl. Lines13/18/25 read/snapshot;33 applies batch;36 FileChange succeeds. Lines43/50 inspect proof infrastructure then run a custom forwarding/reader check. Outputs54 and61 fail for the same malformed-check/argument-sequence issues;65/68 finally pass. Exact-byte checking already passed alongside the first failed check.

C4 source: rollout-2026-09-06T10-18-27-01a07639-fe86-7cb0-81f7-ae7e762c0a98.jsonl. Lines13/21 inspect/snapshot;26 builds patch;32/34 fails missing End Patch marker; 36/38 corrects framing and commits the batch. Lines45/49 fail the custom check's empty-args comparison while exact-byte comparison passes;53/56 pass after vec.

## Utility interpretation, not an interim comparative result

The native method is already batch-oriented. Observed friction is chiefly payload/patch framing and voluntarily rebuilt proof logic, not59individual edits. Three actors spend additional decision cycles correcting their own witnesses after the source change is complete. The saved external proof still runs for all four; inner checks do not replace it. Command runtimes alone cannot assign all intervening elapsed wall to reasoning or tool execution. These observations identify hypotheses to test after cohort completion; they do not authorize changing the frozen prompts, suppressing proof, or claiming tool-route savings.

## Context demand: one large supplied packet, repeated inference context

Each actor's bound rollout line9 contains the dossier exactly once:92,634UTF-8
bytes (92,630characters). Initial developer/user text totals102,995bytes;
this is visible transcript text, not an exact complete model-input byte count.
actor-b.py:318 opens the dossier as stdin; no second dossier path-read was seen.
All four begin with the same first cumulative input snapshot:39,638tokens.
C1 then reports81,937→126,287→171,221→216,728→265,956 cumulative input:
repeated inference over growing context, not265,956 unique prompt tokens.

| Control | Cumulative input | Cached input subset | Input minus cached | Visible custom-result text bytes |
|---|---:|---:|---:|---:|
| C1 | 265956 | 226944 | 39012 | 24700 |
| C2 | 326714 | 285568 | 41146 | 27733 |
| C3 | 422022 | 380672 | 41350 | 28013 |
| C4 | 402868 | 359040 | 43828 | 36908 |

Counts come from bound cumulative snapshots, not sums of repeated last-usage
records. Cached tokens are still input and are not assumed free in cost or wall.
Tool-generated workspace snapshots are processed inside exec; they are not all
printed back as source to the model. The last column measures only returned
custom-output text, including formatting/escaping, not hidden intermediate data.
Context demand is substantial despite5–8outeractions. It plausibly contributes
to elapsed wall, but this observational sample cannot causally separate context
processing, generation, tool latency and scheduling. See companion
`/var/tmp/forge/astra-dn-control-context.json` for exact cumulative sequences.
