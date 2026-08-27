# Hypothesis: Surgeon First, cclsp on Semantic Escalation

## Claim to test

Surgeon's snapshot-bound structural evidence may now answer a large fraction of
the questions for which coding agents currently initialize cclsp. `:ls-tree`
alone is not a replacement for clojure-lsp. The potential replacement is a
bundle of deterministic evidence already owned by Surgeon:

```text
known or discovered file/owner
  -> exact top-level definition
  -> namespace require/alias/refer evidence
  -> local dependency closure
  -> workspace caller candidates
  -> exact quoted-Var evidence
  -> frozen source hashes
  -> guarded plan or transaction
```

This evidence is fast, reproducible, and bound to the same source snapshot as
the eventual mutation. It may be sufficient for ordinary extraction, movement,
namespace surgery, and caller-accounting work without paying semantic-provider
startup or warmup.

## What cclsp still uniquely offers

cclsp remains the escalation path when syntax cannot prove the relationship:

- definition and reference resolution when the file or owner is unknown;
- locals, destructuring, shadowing, aliases, refers, and ambiguous bare symbols;
- protocol and interface implementations;
- incoming and outgoing call hierarchy;
- symbols supplied by dependencies or the classpath;
- some macro-generated, Java, and reader-conditional relationships;
- authority that a reference site resolves to one exact Var rather than merely
  looking structurally plausible.

Reimplementing these broadly could recreate clojure-lsp or clj-kondo analysis.
That possibility is not inherently disqualifying: capable coding agents can
rebuild large systems, and owning the small semantic surface that Surgeon
actually needs could make startup, memory use, tuning, observability, and the
snapshot contract materially simpler. The caution is empirical scope. We do
not yet know whether the valuable subset is small or whether ordinary-looking
queries conceal most of a language server.

The initial architecture should therefore be an escalation ladder and a
measurement instrument, not an up-front replacement project:

```text
rg       broad textual discovery
Surgeon  exact syntax, topology, complete snapshot proof
cclsp    semantic authority only when syntax cannot decide
```

## Meta-experiment: make Surgeon a memoizing cclsp front door

Before recreating semantic operations in Clojure, expose selected cclsp
responses through the Surgeon MCP envelope and memoize them under explicit
validity keys. This is useful even if cclsp remains the implementation:

```text
Surgeon request
  -> exact workspace + source hashes + semantic session identity
  -> cache lookup
  -> cclsp on miss
  -> normalized semantic evidence + provenance + timing
  -> guarded Surgeon plan
```

The cache must never outlive its authority. Keys need the semantic operation,
normalized subject/anchor, relevant source snapshot, provider/session identity,
and configuration/classpath identity. Warming, timeout, partial coverage,
mixed-session evidence, unknown hashes, or provider refusal are not cacheable
authority. A cached answer that cannot prove current snapshot validity is a
hint at most.

The wonderfully meta payoff is that the proxy traffic becomes the
specification for replacement. It can show:

- which cclsp operations coding agents truly request;
- which response fields affect a later decision;
- hit rate and safe invalidation frequency;
- cold startup, provider, serialization, and route-boundary costs;
- which requests Surgeon syntax already answers;
- the smallest unresolved semantic relations worth implementing natively.

Only after this measurement should we choose among continued proxying,
persistent shared cclsp, a small native semantic kernel, or a deeper subsumption
of clojure-lsp behavior.

## Why this is plausible now

The privacy-safe three-day receipt from 2026-08-24 through
2026-08-27T04:45:04Z observed eight cclsp MCP admissions backed by six
underlying LSP requests. Five of those six requests were `initialize`; only one
was a substantive `textDocument/prepareCallHierarchy` request. The sample is
small and older admission telemetry has coverage limits, so this is not proof
that cclsp is wasteful. It is evidence that initialization ceremony may exceed
semantic work in the routes we currently exercise.

## Smallest falsifiable experiment

Replay every retained cclsp request in the bounded three-day receipt. For each
request, hide the original answer and classify it before execution:

1. `surgeon-sufficient`: exact syntax and complete workspace enumeration can
   prove the needed decision from the frozen snapshot;
2. `semantic-escalation-required`: syntax yields zero, many, shadowed,
   generated, protocol, classpath, or otherwise non-authoritative candidates;
3. `insufficient-evidence`: the receipt cannot reconstruct the original
   question without private source or hindsight.

For the first two classes, compare:

- complete route actions and wall;
- provider initialization count;
- bytes returned;
- exact selected owner/reference correctness;
- stale-source and ambiguity behavior;
- whether the result can directly feed a guarded mutation without rediscovery.

Run the same corpus once through the memoizing front door. Report cold misses,
valid warm hits, invalidations, provider starts, response shapes, retained
fields, and the projected native implementation surface. A high cache hit rate
is a useful operational win; a low and narrow miss vocabulary is evidence that
native reimplementation may be tractable.

### Continue gate

Continue toward a syntax-first routing rule only if at least 60% of faithfully
reconstructable requests are Surgeon-sufficient, every such answer is exact,
and complete wall falls by at least 30% without a later native or cclsp
fallback.

### Stop gate

Stop if fewer than half of the requests are reconstructable, any syntax answer
silently chooses among semantic ambiguity, or cclsp remains necessary later in
the same route. Do not grow a heuristic symbol resolver to rescue the result.

## Product boundary

Even a positive replay earns only routing: "start with Surgeon when its
complete syntax proof is authoritative; escalate to cclsp on named semantic
gaps." It does not earn deletion of cclsp, automatic fuzzy selection, or a new
semantic engine inside Surgeon.
