---
parent: mcp-operation-contract-design
prefix: MCP-OP-THREAD
status: "proposed (forge@anvil, 2026-09-04); built on Gene's instruction to build it and see if any agent uses it"
---

# Cross-language feature thread (`feature_thread`)

Status: **proposed (forge@anvil, 2026-09-04)**. Design authority for the
`feature_thread` MCP read verb. It grants no write, install, reload, or
benchmark authority; the verb it describes never mutates a file.

## Context

Gene, dictated 2026-09-04 (relayed by the mayor):

> "I had asked for a change that required five searches, both in JavaScript and
> closure … I think surgeon should allow us to potentially search files outside
> of closure files … let's go to where the problem is versus what the tool is
> currently capable of."

The origin transcript is `social-media-writer`, codex session
`01a0678b-d807-7e42-ac73-db3bd41ca674`, 2026-09-03. Asked whether
`:ls-tree` helped, the seat answered in its own words:

> "The tool I'd want here is a cross-language 'feature thread' query: given
> `formatDraft` or `/api/transform/format`, return the menu caller, JS
> function, route, handler, and tests in one compact receipt."

Cohort **E-THREAD** (30 arms, `docs/observations/2026-09-04-ethread-cohort.md`)
measured the *search* half of that and closed it: native `rg` found 5/5 legs on
all five threads with zero false completes, so a receipt that only *locates*
the legs is not a capability. What it did measure, and keep, is the cost:

> on social-media-writer the receipt halved the search work (7.4 → 3.3 tool
> calls; 66 → 33 s) at identical quality.

Gene's reframe, verbatim, is the design input for this leaf:

> "I suggest we build it, and see if any agents use it … for clj and maybe js,
> bring back the forms? … reading is fast, but don't want to swamp context
> window … remember the goal: 2x reading is good, but if we can save tool
> calls, we rack up gains."

**The unit of cost is the tool call.** The remaining ~3.3 calls after the
locating receipt are *reads of the forms the receipt already located*. So this
verb carries the bodies, and two rows a search cannot produce.

## What the verb is

One read call that answers, for one subject (an identifier, a route, or both):

1. **five named legs**, each `FOUND` with an exact location, a content hash and
   the body, or `ABSENT` with the exact searches that were run;
2. **the sibling** — the neighbouring feature the subject should mirror, with
   its own five legs, bodies elided to ranges by default. "Make one like Format
   Draft" is a request native search cannot know to ask for;
3. **the rules** — how the thread is wired: the editor/persistence path the
   Clojure handler calls, argument precedent, and the `INTENT:` ids found in
   comments immediately above the located forms.

## What the verb is not

- **Not a JavaScript parser.** clj-surgeon owes Clojure-grade correctness for
  Clojure and nothing more. A JS leg's body is produced by brace matching from
  the definition line, and when brace matching cannot close the body the verb
  falls back to a bounded line window and *says so in the evidence label*. It
  never claims to have parsed JavaScript.
- **Not a search that hides its misses.** A leg the searches did not reach is
  `ABSENT` with the searches quoted, never omitted, and the status is then
  `INCOMPLETE (k of 5)`. E-THREAD's own scar: a frozen oracle that admits one
  right answer per slot measures itself. This verb reports witnesses, not
  verdicts about which witness is canonical.
- **Not a repo-independent guess about file roles.** A thread's five legs are
  *that repo's conventions*. They are data — `.clj-surgeon/feature-thread.edn`
  in the workspace, or passed inline — never hard-coded in this namespace.

## Evidence ladder (strongest first)

| kind | meaning | body source |
|---|---|---|
| `form` | a Clojure top-level form located by parse | exact form range |
| `form(joined from route …)` | the handler named by the route table entry | exact form range |
| `route-literal` | the route string appears verbatim | enclosing form |
| `route-assembled` | the route's segments appear as adjacent strings | enclosing form |
| `identifier(def)` | a definition-shaped occurrence in a script file | brace match |
| `identifier(def, one hop: alias …)` | followed `const X = Y;` one hop to `Y` | brace match |
| `identifier` | a bare occurrence (a caller, a test reference) | brace match or window |
| `brace-match` / `window` | body-extraction method, always labelled | — |
| `alias-only` | the only definition-shaped hit was an alias whose target was not found — **not** a leg | none |

A leg is never promoted above the evidence that produced it, and the label
travels into both the text block and `structuredContent`.

## Clojure legs are parsed, never windowed

The Clojure legs (route, handler, and any Clojure test or state-contract leg)
are located by search and then **parsed to the enclosing top-level form** with
the repository's existing form machinery, so the range is exact and the body is
a complete form. This is the only place in the receipt where clj-surgeon's
parser earns the call, and it is the reason the verb belongs in this server
rather than staying a shell script.

## Budget

The receipt is measured **after rendering**, on both faces an agent receives: the
text block and the JSON encoding of `structuredContent`. Both counts are
reported (`text_bytes`, `structured_bytes`, `receipt_bytes` for their sum). The
budget is applied to the **text**, because MCP-OP-THREAD-012 makes the text a
superset of the structure — the structured half is a machine-readable copy of
bytes already counted, and budgeting the sum would halve the real payload to buy
nothing. The structured half is guarded separately by the trunk's
`public-byte-budget` (32,640).

Default budget 16,384 bytes; hard cap on `budget_bytes` 32,768, refused rather
than clamped. Over budget, bodies are elided to line ranges in a fixed, stated
order, cheapest evidence first:

```
secondary tests -> tests -> sibling -> menu/caller -> js-function -> route -> handler
```

The handler is elided last because it is the form the caller is about to edit.
Every elision is named in an `elided` row carrying the leg, the bytes, the range,
the sha256 and an exact `refetch` command, so an elision always says how to undo
itself. A silent cut is a defect of the same class as a four-of-five thread
rendered as five, and a mid-form cut is worse than a range. If the whole receipt
with every body elided still exceeds the budget, the verb refuses rather than
truncating.

## Anchors and the pre-image assert

Each FOUND leg carries an `anchor` — `after:L454`, or
`after:L2148 in-form:L2083-L2376` — saying where a NEW sibling goes, which is
the one fact neither a search nor a body carries. The receipt closes with an
`assert` line: before any edit, re-hash each leg's range and compare it to the
`sha256` the receipt carried; a mismatch is a REFUSAL for a stale pre-image,
never a retry. That is what makes this a write instrument rather than a read.

## Status

`COMPLETE (5 of 5)` only when every leg is `FOUND` or explicitly declared empty
by the caller's config. Any other case is `INCOMPLETE (k of 5)` and names the
missing legs. The status line is computed from the leg vector, never written as
a literal.

## Adoption

E-THREAD measured 0/19 free-choice adoption for Surgeon verbs on this seat's
fleet, and the 2026-09-02 ruling stands: this verb is *built to be measured*, on
Gene's instruction, and if it is to be used it must be routed (a skill line or a
mandate), never merely described. The pre-registered adoption experiment lives
in `docs/observations/2026-09-04-feature-thread-study.md` §9.
