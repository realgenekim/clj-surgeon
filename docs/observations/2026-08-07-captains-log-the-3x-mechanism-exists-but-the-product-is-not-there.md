# Captain's Log: The 3x mechanism exists, but the product is not there

**Date:** 2026-08-07

**Question:** How close is clj-surgeon to a repeatable threefold improvement
over native Clojure work, and what is the shortest credible route to that
result?

## Bottom line

We have crossed 3x in mechanism isolates and one favorable editing task. We
have not crossed 3x in a replicated end-to-end product lane.

The strongest replicated write result is 1.76x faster than native. The best
mutually correct semantic-reference result is 1.93x faster than native, from
one replication. The first correct proof-carrying change-buffer task is 1.75x
faster than native, from one paired probe. The generic read batch is only 1.16x
faster than the current CLI route.

These are substantial gains, but they are not nirvana.

The remaining gap is plausible. The replicated write benchmark took 24.530
seconds. A threefold result against the 43.190-second native median requires
14.397 seconds. We must remove about 10.1 seconds, or 41% of the current MCP
task wall. The guarded transaction itself took 89.875 milliseconds. Parser,
transport, and write optimization cannot supply the missing ten seconds.

The next product must remove model rounds and representation translation. It
must not add another command.

## Scoreboard: where 3x is real and where it is not

Correctness gates all timing claims.

| Surface | Strongest relevant control | Current result | Multiplier | Evidence quality |
|---|---:|---:|---:|---|
| Typed guarded write | Native 43.190 s | MCP 24.530 s | **1.76x** | Four counterbalanced correct runs |
| Project-rule write adoption | Native 43.190 s | MCP 27.432 s | **1.57x** | Four correct runs. Voluntary adoption: 4/4 |
| Proof-carrying return-contract edit | Native 54.13 s | Basis route 31.00 s | **1.75x** | One correct paired probe; exact two-call route |
| Generic structural read | CLI 32.442 s | MCP 27.969 s | **1.16x** | Four counterbalanced correct runs |
| Reference-owner task | Native 46.686 s | cclsp 24.252 s | **1.93x** | One mutually correct replication |
| Outgoing-call task | Native 60.777 s, incorrect | cclsp 20.049 s, correct | **3.03x wall** | Not a valid efficiency comparison because native failed correctness |
| Owner-enrichment mechanism | Bare positions 46.708 s | Owner-enriched result 12.908 s | **3.62x** | Causal payload isolate, not a complete transport comparison |
| Favorable two-file edit | Native 134.26 s | Surgeon 38.60 s | **3.48x** | One run. Native first attempted a context-free patch and recovered |

The 3x results are still valuable. They prove that deleting recovery and
location-to-owner bookkeeping can create the desired magnitude. They do not
prove that the product produces that magnitude across ordinary tasks.

## What the measurements now say

### 1. The kernel is no longer the bottleneck

Direct tool execution is 4-11% of complete task wall. The write transaction
completed in about 90 milliseconds. The live two-file `inspect_clojure`
self-hosting probe completed in 178 milliseconds. A native image, faster
parser, or smaller JSON serializer can improve polish. None can remove the
missing ten seconds from the replicated write lane.

### 2. One typed action is necessary, but not sufficient

`apply_clojure_changes` removed shell quoting, skill loading, source reads,
diff inspection, and recovery. That produced an 18.660-second replicated win.
It did not reach 3x because the caller still spends substantial time parsing
the task, constructing the full edit payload, and composing the answer.

### 3. Batching calls does not guarantee batching thought

`inspect_clojure` reduced ten CLI calls to one MCP call. Complete wall improved
by only 13.8%. The result still carried about 24 KB and 12,426 exact source
characters. The caller had fewer tool boundaries, but it still had to digest a
large mixed evidence packet.

The next read improvement must make the result answer-shaped, not merely
batch-shaped.

### 4. Rich return values can cross 3x

Adding enclosing owners to cclsp reference locations changed the same semantic
evidence from a 46.708-second recovery route to 12.908 seconds with no follow-up
tools. The semantic engine was already fast. The result shape deleted the
caller work.

This is the best clue in the current evidence base: return the next decision's
inputs, not low-level coordinates.

The first proof-carrying change-buffer replay confirmed the direction on a
complete edit. Returning call expressions made the treatment take 95.75
seconds and forced source reconstruction. Returning complete named owners made
the same route finish in 31.00 seconds with no source read and no fallback. The
result did not cross 3x, but it removed 64.75 seconds from the failed product
shape.

## The route to nirvana

For an unknown change, two tool calls are the honest minimum:

```text
inspect and prepare
  -> model decides once
  -> apply and verify
```

For a fully supplied decision, the first call disappears:

```text
apply and verify
```

Do not build an autonomous refactoring oracle to remove the remaining model
decision. That would encode judgment in the tool and violate the Bitter-Lesson
boundary. Instead, make it almost effortless for the model to supply that
decision.

### The proposed two-call surface

```text
inspect_clojure
  workspace-scoped semantic and structural questions
  -> compact evidence
  -> hash-bound decision register

apply_clojure_changes
  decision register + model-supplied replacements
  -> atomic write
  -> configured verification profile
  -> terminal receipt
```

The model supplies questions, scope, and desired replacements. The tools own
file discovery, resolved references, structural addresses, match counts,
snapshot hashes, write order, rollback, and verification plumbing.

### 1. Add workspace-scoped inspection

The current inspect contract requires the caller to know every file. Ordinary
development often starts with a symbol, behavior, or structural pattern. A
prior `rg` or file-enumeration round defeats the one-call read surface.

Add bounded workspace operations:

- resolved definitions, references, callers, and callees from hot cclsp;
- structural match across project Clojure files;
- explicit budgets for files, matches, source characters, and result bytes;
- overflow refusal with per-file counts and one executable narrowing remedy.

Rent semantic facts from cclsp. Do not build another kondo index. Re-anchor
every semantic result to a hash-bound owner before returning it.

### 2. Return a decision register, not a second manifest-writing task

When inspect finds candidate sites, store their file, owner, exact before form,
counts, and hashes under a short-lived server-side register. Return compact
evidence plus the register identifier and the unresolved decision fields.

The apply call should accept the register and only the model's new information:

```json
{
  "basis": "decision-7f3a",
  "decisions": [
    {"id": "body-class", "replace": ":body.page"}
  ]
}
```

The server reconstructs the complete guarded transaction. It refuses if any
snapshot changed. The full semantic manifest remains available as a recovery
artifact, so the register never becomes the only durable address.

This is manifest symmetry made concrete. The caller does not translate a read
result into files, forms, patterns, and counts that the server already knows.

### 3. Put verification behind a closed profile

The final `make test` call creates another model/tool boundary. Allow the apply
request to name a repository-configured verification profile such as `fast` or
`mcp`. Do not accept an arbitrary shell command in the MCP payload.

The profile owns the command, timeout, output budget, and pass criteria. The
receipt reports the transaction proof and verification proof separately.
External test side effects are not transaction-atomic and must never be
described as rolled back.

### 4. Make every result answer-shaped and bounded

Do not return exact source twice. Return only evidence required for the next
decision, plus hashes and negative space. Keep large exact results in an
optional server-side result register. Fetching a member by handle is a recovery
route, not the normal route.

Result registers are useful when output truncation is measured. They are not
the next standalone feature: the current inspect portfolio stayed below the
transcript boundary and still missed its keep gate.

### 5. Hide low-comparative-advantage operations

Native patching remains the preferred control for one unique text or prose
change. cclsp remains the preferred semantic sensor. clj-surgeon should become
irresistible only where it owns a real advantage:

- related edits across files or owners;
- structural broadcast changes;
- dependency-aware moves and renames;
- edits that need count, distribution, drift, and rollback guarantees;
- a read result that should compile directly into a guarded change.

The product wins by routing well, not by replacing every native tool.

## Revised build order

The earlier roadmap ranked result registers before manifest symmetry. Current
evidence changes that order.

1. Freeze a goal-level development portfolio from real prompts and commits.
   Do not expose the expected mechanical patch to the caller.
2. Measure the current integrated two-tool route against native and current
   CLI controls.
3. Add workspace-scoped inspection with cclsp-backed semantic operations.
4. Add the hash-bound decision register and basis-based apply request.
5. Add closed, repository-configured verification profiles.
6. Add result-register drill-down only for a task that actually crosses the
   visible-output budget.
7. Add new edit operators only when a portfolio task cannot express its
   decision through exact replacement.

Build each slice small enough to dogfood in the next slice.

## The next falsifiable experiment

Create three frozen goal-level tasks:

1. Trace a function's callers, change its return contract, and update all real
   callers and tests.
2. Find a repeated structural shape across files and change only the intended
   owners.
3. Move a form whose dependencies and other users constrain the safe move.

Each task starts from a commit and ends at acceptance tests. The prompt states
the goal, not the patch. Run four counterbalanced replicates for:

- native tools only;
- today's cclsp plus `inspect_clojure` plus `apply_clojure_changes`;
- the decision-register treatment.

Keep the treatment only when all runs are correct and its aggregate median is
at least 3x faster than the best correct control. Also require:

- no more than two source-bearing tool actions before verification;
- no repeated file or owner discovery;
- no caller-authored mechanical manifest fields already proven by inspect;
- no post-success reread or diff;
- bounded evidence with no transcript truncation;
- one terminal receipt that names both source and verification authorities.

## Nirvana, stated plainly

The model should feel that it described the change once and the repository
compiled that decision into reality.

```text
goal
  -> one coherent evidence packet
  -> one model decision
  -> one verified repository transition
```

That experience can be 3x faster because it removes six or seven mechanical
rounds. It cannot be 3x faster because a 90-millisecond transaction becomes a
30-millisecond transaction.

The frontier is no longer faster syntax surgery. It is preserving one model
decision as one transaction from perception through proof.
