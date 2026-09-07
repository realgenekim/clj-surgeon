Committed `9b205fd4` on `astra/namespace-split` as `forge-anvil`, with Gene’s trailer. Not pushed; worktree clean.

Fixed baseline-relative lint blocking, sorted and correctly indented requires, stale-prose advisories, verbatim docstrings, and named proof timings. Eight new witnesses cover intents `NS-SPLIT-016`–`021`. The fixture’s owner oracle now runs before kaocha; verification remains intact.

Fresh `d9205abc` fixture proof:

```text
231 tests, 2258 assertions, 0 failures.
views.clj gone: PASS
PASS: views.clj gone; all 141 owners occur exactly once in their 20 listed destination files.
1 tests, 7 assertions, 0 failures.
```

`make test` passed; touched-file lint is clean. Server require layout and all 20 docstrings passed independent checks.

In-call wall: **30.67/30.38 s before → 33.87 s after**, including **3.38 s** for the new candidate lint check.

[Full report and evidence](/home/forge/src/clj-surgeon-split/docs/observations/2026-09-07-namespace-split-papercuts.md).