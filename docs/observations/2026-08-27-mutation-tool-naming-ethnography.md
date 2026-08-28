# Mutation Tool Naming Ethnography

**Status:** bounded naming evidence; no product-name recommendation yet  
**UTC window:** 2026-08-27 23:26:30 to 2026-08-28 05:08:02  
**Pacific window:** 2026-08-27 16:26:30 to 22:08:02 PDT  
**Counting authority:** `/tmp/clj-surgeon-agent-usage-20260827T232630Z-20260828T050802186471Z.json`  
**Receipt status:** `ok`

## Question

Do the public names `edit_clojure` and `apply_clojure_changes` help an agent
select the correct mutation interface?

The answer is no. Both names describe generic source mutation. They do not
state the important distinction:

```text
exact guarded replacement or deletion
                    versus
prepared structural operation with transaction-owned gates
```

The names are a real discoverability defect. They are not the main cause of
the observed refusals, native fallbacks, or payload errors.

## Sample and exclusions

The receipt contains 27 Codex sessions, of which 20 were Clojure-relevant. It
contains two Claude sessions, of which one was Clojure-relevant. Only one
Codex session used either mutation interface in this window. The Claude sample
made no Surgeon call and therefore provides no naming evidence.

The Codex receipt counted 14 `edit_clojure` calls and 18
`apply_clojure_changes` calls. A narrow transcript reconstruction found the
same 32 public invocations:

| Public interface | Calls | Committed | Safe refusal | Wrapper failure |
|---|---:|---:|---:|---:|
| `edit_clojure` | 14 | 12 | 2 | 0 |
| `apply_clojure_changes` | 18 | 12 | 5 | 1 |

The one wrapper failure occurred before the MCP server. This explains why the
service receipt contains 31 apply events while the agent receipt contains 32
public mutation calls.

This is a tool-development session. Several refusals were deliberate safety
probes. Counts from this window are not production failure rates. Native
Write or patching also remains the correct control for new files, prose, and
small visible literals.

I used the privacy-safe receipt first. I then used `jq` and `awk` to read only
the mutation-call sequence, result headers, and nearby user/assistant route
statements from the one receipt-named Codex evidence file. No project source,
payload bodies, URLs, or private domain details are reproduced here.

This is a focused naming study, not a complete provider study. It does not
advance the receipt's `agent-usage-window-end` marker.

## Proven findings

### 1. A human could not infer the boundary from the names

After seeing both tools used on adjacent changes, the user asked for the
high-level difference between them. That is direct evidence that the pair
`edit` versus `apply changes` does not communicate two distinct contracts.

Both phrases mean approximately “modify source.” Neither phrase names the
decision basis, authority boundary, or unique capability.

### 2. The route became coherent after the contract was explained

After the distinction was stated, the caller repeatedly used:

- `edit_clojure` for exact owner-scoped replacements and known owner deletion;
- `apply_clojure_changes` for insertion of complete forms and operations that
  needed the heavier transaction contract.

The caller completed 24 of 32 invocations. The split was symmetric: 12
commits through each interface. The interfaces are usable when their contract
is already in context. The defect is initial recognition, not an inability to
use the APIs.

### 3. One native fallback was caused by route persistence, not the names

The caller created a new file with native patching, where native patching was
appropriate. It then continued to patch existing owner-scoped Clojure forms.
The user noticed the missed structural route. The caller explicitly reported
that it had carried the new-file route into later existing-form edits.

Once corrected, the caller immediately used `edit_clojure` successfully for
an exact replacement and a known owner deletion.

This is a capability-visibility and action-mode persistence problem. A rename
alone does not prevent it. Routing guidance must reset the decision at each
new mutation shape.

### 4. Two immediate retries exposed count semantics, not naming failure

One `apply_clojure_changes` insertion took three attempts:

```text
attempt 1  owner match count confused with inserted-form count  -> refused
attempt 2  aggregate edit count declared as two                  -> refused
attempt 3  one intent / one edit                                 -> committed
```

Both refusals were pre-write and reported source unchanged. Safety worked.
The model misunderstood the relationship among owner matches, inserted forms,
intents, and aggregate edits. Better names for the two public tools will not
remove this bookkeeping. The request shape or diagnostics must make the count
algebra unnecessary or mechanically derivable.

### 5. Payload construction remained a separate source of friction

The window also contains malformed-form refusals, an ambiguous insertion-gap
refusal, and one orchestration-wrapper error. Some malformed-form refusals
were intentional product proofs. Others occurred while constructing large
forms.

These failures concern payload ergonomics and structural authorization. They
do not show that the caller chose the wrong public interface.

### 6. There is no Claude naming result in this receipt

Claude had one Clojure-relevant session, zero skill loads, and zero Surgeon
calls. Any claim that a new name helps Claude would be unsupported by this
window. Claude requires a fresh, controlled selection test.

## Inferred findings

The following explanations are plausible but not proven by this receipt:

- `apply_clojure_changes` may attract catch-all use because it sounds broader
  and more complete than `edit_clojure`.
- `edit_clojure` may sound less safe or less atomic, although both interfaces
  return atomic commit, read-back, and terminal verification evidence.
- The verb `apply` may suggest that a separate plan already exists, even when
  the request carries a direct structural operation.
- The noun `changes` may imply arbitrary edits, although the interface accepts
  a closed structural language and stronger transaction-owned gates.

These hypotheses need a clean-context selection cohort. They must not be
treated as reasons to rename production tools by intuition.

## Trace-derived naming laws

### Law 1: Name the decision basis

The lightweight name must say that the caller supplies an exact guarded edit.
The heavyweight name must say that Surgeon compiles or executes a prepared
structural transaction. Do not distinguish them with two generic mutation
verbs.

### Law 2: Make the pair contrast at a glance

The two names must form an obvious pair in a tool catalog. A caller must be
able to select one without reading both full schemas. Candidate pairs should
use parallel grammar and one contrasting term.

### Law 3: Do not encode false safety hierarchy

Both tools are atomic and verify written bytes. The names must not imply that
one is safe while the other is merely convenient. The real difference is the
kind of decision and the gates owned by the transaction.

### Law 4: Do not promise an all-purpose fallback

The heavyweight name must not sound like “apply any Clojure change.” It has a
closed operation language and should remain reserved for prepared semantic
work or transaction-owned gates.

### Law 5: Name and schema must agree

If the lightweight tool is named for exact edits, its top-level collection
should use `edits`. If the heavyweight tool is named for structural
transactions, its request and result should use transaction or operation
terms consistently. Mixed terms such as changes, intents, edits, matches, and
aggregate edits increase recovery turns.

### Law 6: Keep negative routing in the description

No name can encode the complete boundary. Each description must contain one
short positive rule and one short escalation rule. Example shape:

```text
Use this for exact guarded replacements and deletions.
Use the structural transaction tool when Surgeon must compile an operation or
run a gate inside rollback.
```

### Law 7: Reset routing by mutation shape

Instructions must tell the caller to classify each mutation independently.
Creating one new file with native patching does not authorize native patching
for the next existing Clojure owner.

### Law 8: A rename does not earn a bookkeeping claim

Evaluate names on first-choice accuracy and selection latency. Evaluate schema
changes separately on refusals, retries, and payload size. Do not credit a
rename for mechanically derived counts or easier form insertion.

### Law 9: Preserve the earned fast path

The current caller used both interfaces coherently after one explanation.
Any new pair must preserve or improve complete-task time for this learned
route. Familiarity loss is a real migration cost.

### Law 10: Test callers separately

Codex evidence does not transfer automatically to Claude. A naming gate needs
fresh Codex and Claude callers, the same task cards, the same visible tool
catalog, and no corrective prompt.

## Smallest falsifiable naming experiment

Create five neutral task cards:

1. one exact replacement inside a named owner;
2. several exact replacements in one frozen batch;
3. insertion of complete top-level forms;
4. a prepared semantic change with rollback-owned verification;
5. a new prose file that should use native writing.

For each candidate name pair, show fresh callers only the tool names and short
descriptions. Do not show the current routing answer. Score:

- correct first route;
- time to first mutation call;
- schema inspection or help calls;
- safe refusals before the first successful mutation;
- complete verified task time;
- native fallback when a Surgeon route is materially better.

Reject a candidate that improves verbal preference but reduces first-route
accuracy or slows the established Sol path. Run Claude as a separate stratum.

## Decision for the naming review

Proceed with candidate generation. Do not choose a production name from this
receipt alone.

The evidence requires a name pair that exposes **exact guarded edit** versus
**compiled structural transaction**. It also requires a separate schema
ratchet for count bookkeeping and a routing ratchet for action-mode
persistence. Combining those three problems into one rename would hide the
real causal work.
