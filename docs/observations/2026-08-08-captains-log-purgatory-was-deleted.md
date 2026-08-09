# Captain's Log: Purgatory was deleted

Today Mothership deleted `resources/public/js/app-purgatory.js`.

Not renamed. Not emptied. Not hidden behind a compatibility loader. The file
and every generated-page loader are gone. Permanent tests now fail if the
bundle returns.

**Purgatory is dead. 🎉**

This was the kind of result clj-surgeon exists to enable: a broad retirement
that ended as a sequence of small, bounded, evidence-carrying decisions instead
of one heroic patch.

## The deletion

At Mothership's `source-reader-production-v1` checkpoint, its two legacy
JavaScript bundles contained:

- `app-purgatory.js`: 826 lines;
- `app-doomed.js`: 3,221 lines.

At the deletion checkpoint:

- Purgatory was **826 -> 0 lines** and the file no longer existed;
- Doomed was **3,221 -> 3,155 lines**;
- 894 gross lines of legacy JavaScript had disappeared;
- 682 lines of bounded production owners had been added; and
- the compared production JavaScript surface was 212 lines smaller.

The replacement was not another bundle with a nicer name. Browser-owned
capabilities acquired narrow owners such as `code-browser-nav.js`,
`command-center-actions.js`, `monitor-actions.js`, and `issue-actions.js`.
Rendering and application state moved to the server. Each legacy responsibility
was routed away before its old block was removed.

The final deletion landed in Mothership commit `ae0a87d`,
`refactor: delete app purgatory`.

## The all-time deletion record

The celebration prompted a repository-history audit. The correct comparison
was not raw repository deletions: one prior day removed a 26,595-line runtime
log, and another mechanically concatenated 29 JavaScript files into the three
legacy bundles. Neither event represented application-code retirement.

The bounded metric was gross lines deleted from `app-doomed.js` and
`app-purgatory.js`, with additions shown separately. By that measure,
2026-08-08 was Mothership's all-time high by a large margin:

```text
Gross lines deleted from app-doomed.js + app-purgatory.js
Scale: # ~= 30 lines

2026-04-05   596  ####################
2026-04-06   143  #####
2026-04-25    56  ##
2026-05-01    83  ###
2026-07-18   103  ###
2026-08-04    84  ###
2026-08-08  1469  #################################################
```

The day's balance was:

```text
legacy lines added        15
legacy lines deleted   1,469
net legacy shrink      1,454
```

That is 2.46 times the previous gross-deletion record and almost 19 times the
previous meaningful net-shrink record of 78 lines. Across all production code,
the formatter-normalized net reduction was 352 lines, the second-largest daily
production shrink in repository history behind 456 lines on 2026-03-18.

This distinction matters. The gross production diff for the day includes a
formatter commit that rewrote roughly 3,000 lines in each direction. The legacy
bundle result does not depend on that churn. It records a real loss of obsolete
implementation.

## The strangler worked

The operating rule was simple:

1. identify one executable responsibility;
2. build a bounded server-owned or browser-capability replacement;
3. load and verify the new owner;
4. prove and switch every consumer;
5. delete the whole legacy block; and
6. add an absence test so the old path cannot quietly return.

That sequence retired the App2 source fragment and shadow editor, extracted
code-browser navigation, removed command-center client ownership, bounded the
monitor actions, moved process-analysis rendering to Hiccup, and extracted the
remaining issue gestures. Only after those cuts did the file itself disappear.

The last deletion was boring. That was the victory.

## How instrumental was clj-surgeon?

**8/10 overall, and 8/10 for the extent to which this record was made possible
by Surgeon. Genuinely instrumental, but not the author of the architecture.**

Surgeon did not decide that Mothership should be server-rendered, choose the
Datastar strangler pattern, design the JavaScript adapters, or exercise the
browser. Those were judgment tasks.

Surgeon made the Clojure half of those judgments safe to execute:

- one batched structural read covered three files and four named forms without
  loading the surrounding large namespaces;
- each named form carried an exact source anchor into semantic resolution;
- cclsp resolved `app.views.ide-layout/ide-shell` and eight references under
  one language-server session;
- guarded changes compiled several exact owner edits into atomic transactions;
- declared match counts refused stale assumptions;
- comment-bearing insertion gaps refused instead of relocating source trivia;
- a verification failure restored the original bytes; and
- `read_complete=true` and `verification_complete=true` provided terminal
  evidence instead of inviting another anxious reread.

The counterfactual is not “the deletion was impossible.” It was possible with
grep, broad file reads, hand-counted callers, and native patches. The
counterfactual is that the agent would have carried more mechanical state and
the reviewer would have accepted more unproved assumptions.

Surgeon was most instrumental at the exact moment a strangler migration is
usually frightening: proving that the old loader no longer had a live caller.
It converted that proof from a narrative claim into a source-anchored reference
surface plus guarded edits.

The 8/10 “made possible” rating means the record was not literally impossible
without Surgeon. Native tools performed the JavaScript deletions, as Surgeon's
contract requires. The user and agent supplied the strangler architecture, and
the repository tests and browser checks supplied execution evidence.

Surgeon made the *safe scale* of the deletion possible. It kept large Clojure
owners bounded, proved caller surfaces before loader removal, compiled related
owner changes atomically, and preserved the starting bytes when a proposed
change lacked sufficient evidence. Without it, the same campaign would have
depended on more broad reads, hand-maintained caller lists, repeated grep, and
faith that no shell had been missed. The likely outcome would have been smaller
cuts, more hesitation, or both.

## Receipts

Mothership's final gates reported:

- 235 Clojure tests;
- 1,772 Clojure assertions;
- 75 JavaScript tests;
- zero failures; and
- successful live loads of the IDE, command center, monitor, and registered
  issue pages without `app-purgatory.js`.

Permanent source-retirement tests require the file and its loaders to remain
absent.

## Why this matters for Surgeon

This was not a synthetic rename or a benchmark fixture. The application is used
for daily production file reading. The migration crossed large Clojure views,
shared shells, browser assets, and live routes while the user was actively
depending on the reader.

Surgeon's value was not that it wrote more code. Its value was that it reduced
the amount of code and uncertainty the agent had to hold simultaneously. The
tool handled bookkeeping—form boundaries, exact bytes, reference locations,
cardinality, atomicity, rollback, and verification—while the agent retained
the architectural decision.

That is the bookkeeping-versus-judgment boundary from the vision, exercised on
a real deletion.

## The missing two points

The session also exposed concrete friction:

- top-level insertion still required the documented native-patch fallback;
- direct and prepared-basis changes exposed different verification contracts;
  and
- unrelated pre-existing clj-kondo warnings could reject a cosmetic owner
  rewrite during fast verification.

Those are product seams, not reasons to discount the result. The primary route
was present, attached to the correct workspace, and strong enough to make a
legacy-bundle retirement materially safer.

## Next watch

Mothership still contains 3,155 lines in `app-doomed.js`. The next campaign can
falsify this success: does the same inspect, resolve, decide, apply, and verify
route remain short as each remaining responsibility is strangled?

But today deserves the unqualified sentence:

> Mothership has no Purgatory.
