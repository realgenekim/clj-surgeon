# Captain's Log: Nine out of ten — the monolith became a map

**Date:** 2026-08-10  
**Field site:** `sessionize-sched-killer`  
**Question:** Can clj-surgeon turn a 4,590-line Clojure view namespace into an
evidence-backed decomposition plan without making the agent read or move the
whole file?

## Bottom line

Yes. The field rating was **9/10**.

The namespace contained 144 top-level forms: 141 definitions and three
`declare` forms. It mixed document shells, organizer chrome, form controls,
review-board fragments, twenty product/foundation surfaces, and three public
entry points. The agent used `:ls`, `:ls-deps`, `:ls-extract`, and non-mutating
`:extract` previews. No Clojure form moved during the reconnaissance.

Clj-surgeon made the namespace tractable because it returned ownership-sized
evidence instead of 4,590 lines of source. It found the five real forward
references, showed the dependency closure of proposed surfaces, and proved two
representative extractions against the current source:

| Proposed namespace | Forms | Source lines |
|---|---:|---:|
| `views.format` | 15 | 109 |
| `views.dashboard` | 8 | 276 |

The dashboard preview included a newly added `event-dashboard-region`, the
stable fragment boundary used by one-shot Datastar time travel. That form did
not exist when the first decomposition brief was written. A fresh structural
inventory caught the drift immediately and put it beside
`event-dashboard-page`, where it belongs.

## Before and after

```text
BEFORE

cfp_scheduler_killer/
├── server.clj
└── views.clj                         4,590 lines / 144 forms
    ├── document shells + sidebar + DEV strip
    ├── formatting + form controls + avatars
    ├── event setup + dashboard + committee
    ├── review board + fragments + capture
    ├── log + integrations + schedule
    ├── communications + replay + form builder
    ├── portal + public CFP + drafts
    └── landing + login + people
```

```text
AFTER

cfp_scheduler_killer/
├── server.clj
└── views/
    ├── format.clj
    ├── form_controls.clj
    ├── avatar.clj
    ├── shell.clj
    ├── organizer_layout.clj
    ├── live_drafts.clj
    ├── event_setup.clj
    ├── dashboard.clj
    ├── committee.clj
    ├── review.clj
    ├── log.clj
    ├── integrations.clj
    ├── schedule.clj
    ├── communications.clj
    ├── replay.clj
    ├── form_builder.clj
    ├── people.clj
    ├── portal.clj
    ├── public_cfp.clj
    └── auth.clj
```

The tree is the concrete flat target under `cfp-scheduler-killer.views.*`.
Conceptual foundation/organizer/public groups are enforced by resolved
dependency rules rather than extra package nesting.

## What structural evidence changed

Three boundaries would have been easy to get wrong with line ranges.

First, `time-travel-bar` appears near the review board, but its architectural
owner is organizer chrome: `dev-strip` calls it. Moving it with review would
create the wrong dependency direction and preserve a 1,187-line forward gap.

Second, `answer-input` serves the form builder, public CFP, and speaker portal.
It belongs in shared controls rather than whichever surface happens to sit
nearest in the file.

Third, board rows use pooled avatars while the people page consumes review
formatters. A leaf avatar namespace breaks the potential `review <-> people`
cycle.

An after-sale review against the safe-refactor gates found a fourth boundary
improvement. The first plan's `layout` namespace still mixed generic
document/runtime mechanism, organizer navigation policy, and shared live
fragment values. The revised foundation graph separates them:

```text
shell:             versioned, favicon, page-shell, datastar-script
organizer-layout:  sidebar, DEV strip, time travel, organizer shell, header
live-drafts:       CFP note/status and portal status fragments
```

That separation changes a naming preference into an enforceable architectural
promise: public views can require `shell` but cannot reach organizer chrome.
Likewise, `not-blank` moved conceptually from `controls` to `format`; its 41
resolved call sites across 15 owning forms show presentation normalization, not
form-widget ownership. The remaining four widgets become the more truthful
`form-controls` namespace.

The refreshed dashboard closure added a fourth useful result:

```text
event-dashboard-page
├── layout: organizer-shell, header
├── format: fmt-date-range
└── event-dashboard-region
    ├── review presentation: inform-banner, submissions-sparkline
    ├── avatar: pool-face
    ├── controls: not-blank
    └── format: fmt-instant, cfp-public-url
```

This supports extracting review and the leaf foundations before dashboard. It
also preserves the scrubber architecture: the DEV strip stays outside
`#dashboard-region`, so a fragment patch cannot replace the slider mid-drag.

## The real caller surface was smaller than it looked

A naive `views/foo` search was polluted by files that alias
`cfp-scheduler-killer.reviews` as `views`, and even by the `views` suffix inside
the token `reviews/foo`. Restricting the search to namespaces that actually
require `cfp-scheduler-killer.views` found only four direct consumers:

```text
src/cfp_scheduler_killer/server.clj
test/cfp_scheduler_killer/forms_test.clj
test/cfp_scheduler_killer/polish_test.clj
test/cfp_scheduler_killer/views_test.clj
```

That is excellent news for incremental migration. It is also evidence that
textual caller discovery is not a semantic reference surface.

## Why the score was not ten

The non-mutating extraction previews were safe and useful, but two parts of the
complete migration still required clj-kondo analysis and careful judgment.

### 1. Same-namespace users are migration work

Extracting a foundation does not only update `server.clj` and tests. Every form
left in the old monolith that calls the extracted Vars must be qualified in the
same transaction. The extraction preview did not summarize those internal
users.

Resolved var-usage analysis measured the actual blast radius. Examples:

| Extracted Var | Resolved internal call sites | Distinct owning forms |
|---|---:|---:|
| `fmt-instant` | 9 | 6 |
| `fmt-when` | 8 | 6 |
| `not-blank` | 41 | 15 |
| `answer-input` | 6 | 4 |
| `organizer-shell` | 18 | 18 |
| `header` | 17 | 17 |
| `page-shell` | 7 | 7 |
| `pool-face` | 5 | 5 |

A raw text count had reported 81 occurrences of `header` because it included
CSS classes, keywords, and prose. Resolved usage reduced that to the seventeen
real function calls. This is exactly the mechanical bookkeeping the structural
tool should eventually own.

### 2. Generated namespace requires are scaffolding

Both extraction previews copied the monolith's complete require list into the
new namespace. The format slice needs Java time plus `events/valid-timezone?`,
not committees, exports, portal, schedule, Datastar, JSON, and every other
monolith dependency. Each extraction must currently prune requires immediately
and run clj-kondo before it can be considered a coherent result.

## Product implications for clj-surgeon

Two improvements would turn this 9/10 route into a credible 10/10 route:

1. An extraction preview should report internal users of every moved Var,
   grouped by enclosing top-level form, and include them in the migration
   receipt or recommended follow-up transaction.
2. Caller evidence should distinguish namespaces that semantically require the
   source namespace from textual token matches and alias collisions.

A third improvement would remove cleanup work: synthesize the minimal target
require/import set from resolved usages, or at least report which copied
requires are unused.

The bookkeeping-versus-judgment boundary remains right. Clj-surgeon should not
decide that time travel belongs to layout or that avatar is the correct cycle
breaker. It should expose the graph completely enough that the agent makes
that judgment once, then compile the move and caller rewrites without another
search expedition.

## Verdict

Clj-surgeon did not merely save tokens. It changed the quality of the design.
The resulting plan partitions every definition, eliminates accidental forward
references, preserves Datastar fragment identity, and has a safe extraction
sequence. The remaining work is no longer “understand a 4,590-line file.” It is
twenty bounded namespace transactions with explicit gates.

That is structural exocortex territory.
