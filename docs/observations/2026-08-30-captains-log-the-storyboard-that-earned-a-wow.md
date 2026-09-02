# Captain's log, 2026-08-30 (evening watch) — the storyboard that earned a "Wow!!!"

*Written by mayor@skiff. The recovery LLD ratification decision went to Gene twice: once
as a correct, complete, receipted decision card — and once as four-panel ASCII
storyboards in plain STE100 language. The card got a considered question. The
storyboards got: **"Wow!!! Love it! Go!"** — ratification in five words, seconds after
reading. This log preserves the artifact and the lesson, because the lesson keeps
recurring: for Gene, the explanation IS the interface, and a drawn state-sequence beats
prose every time it has been tried (the label-bug storyboards, the three-option sort
tables, now this).*

## The feature, as it was successfully explained

The problem panel — what happens today on ~1 in 9 Surgeon calls (measured):

```
TODAY — the fumble

┌─ Panel 1: READ ────────────┐  ┌─ Panel 2: BUILD CALL ──────┐
│ agent: inspect route.clj    │  │ agent types from memory:    │
│                             │  │   file:  route.clj    ✓     │
│ server: here is the code    │  │   owner: route-event  ✓     │
│   (defn route-event ...     │  │   from:  ":done"      ✓     │
│    :done ... :done ...)     │  │   matches: 1          ✗     │
│                             │  │   (the code has TWO!)       │
└─────────────────────────────┘  └─────────────────────────────┘

┌─ Panel 3: REFUSED ─────────┐  ┌─ Panel 4: REPAIR LOOP ─────┐
│ server: NO.                 │  │ agent: re-read the file...  │
│  expect-count-mismatch      │  │ agent: rebuild the call...  │
│  (source unchanged)         │  │ agent: retry...             │
│                             │  │                             │
│                             │  │ COST: ~69 seconds,          │
│                             │  │ ~3,000 output tokens,       │
│                             │  │ 20 recovery actions         │
└─────────────────────────────┘  └─────────────────────────────┘
```

The solution panel — the server already read the code, so it fills the mechanical
fields itself; the agent types only the decision:

```
WITH THE FEATURE — fill in the holes

┌─ Panel 1: READ ────────────┐  ┌─ Panel 2: RESULT + FORM ───┐
│ agent: inspect route.clj    │  │ server: here is the code,   │
│                             │  │ AND a prepared form:        │
│                             │  │ ┌─────────────────────────┐ │
│                             │  │ │ file:  route.clj   [✓]  │ │
│                             │  │ │ owner: route-event [✓]  │ │
│                             │  │ │ from:  ":done"     [✓]  │ │
│                             │  │ │ matches: 2         [✓]  │ │
│                             │  │ │ to:    ______      HOLE │ │
│                             │  │ └─────────────────────────┘ │
└─────────────────────────────┘  └─────────────────────────────┘

┌─ Panel 3: DECIDE + FILL ───┐  ┌─ Panel 4: ONE CALL, DONE ──┐
│ agent decides to edit.      │  │ server: committed. verified.│
│ agent fills ONE hole:       │  │                             │
│   to: ":complete"           │  │ COST: ~52 seconds,          │
│                             │  │ ~2,100 output tokens,       │
│ (types 1 field, not 5)      │  │ 8 recovery actions          │
└─────────────────────────────┘  └─────────────────────────────┘
```

The safety rules that made "Go" safe to say, exactly as presented: the form is
non-executable text; it never nominates an edit — the agent decides first; every hole
must be filled before submission; normal validation gets no shortcut; ineligible
results pass through byte-unchanged with no hint.

The one-sentence version that carried it: **the server already knows the mechanical
facts because it just read the code — the feature stops making the agent retype them,
and typing is the expensive part.**

## What was ratified, precisely

Gene's "Wow!!! Love it! Go!" ratified SURGEON1's success-only Option A recovery LLD and
activated MCP-OP-PREP-REQ-001..009 (`docs/prepared-request-recovery-lld-20260830` @
050f9ea, three independent audits GO). Red tests and the first slice — one pure
projector, four narrow inspect-tool hooks — unlocked; install stays behind the full
gate (red→green, independent verification, live-route measurement, Gene's word). The
evidence base: recovery effect twice replicated (−25.3% wall, −30%/−47% output,
recovery actions 20→8), on a measured ~11% failure-path incidence — ~2 seconds per
Surgeon call in expectation. Routing remains a closed loss cited nowhere.

## The lesson, promoted

A decision card and a storyboard are not the same artifact at different polish levels;
they are different instruments. The card proves the decision is safe. The storyboard
makes the decision *legible* — Gene sees the state sequence, checks it against his own
model, and the correct answer becomes obvious to him in one read. The house pattern is
now: **for any feature that changes an interaction loop, draw the before-panel and the
after-panel over the same concrete example, with the costs in the corner of the last
frame.** The panels carried more ratification force than three audits — because the
audits proved it was right, but the panels let Gene SEE it was right.
