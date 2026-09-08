Committed **`5b78bed2`** on `astra/namespace-split` as `forge-anvil`, with Gene’s trailer. Clean worktree; not pushed.

Implemented **NS-SPLIT-022–027**: destination-owned docs, import pruning, aligned continuations, require layout/grouping, and advisory qualified references.

- In-call wall: **35.393 s → 33.391 s**.
- `make test`: **781 JVM + 873 Babashka tests**, all green.
- Focused witnesses: **28 tests / 275 assertions**. Touched-file lint clean.

```text
231 tests, 2258 assertions, 0 failures.
views.clj gone: PASS
PASS: views.clj gone; all 141 owners occur exactly once in their 20 listed destination files.
1 tests, 7 assertions, 0 failures.
```

Whole-snapshot lint delta is **0 errors / 0 warnings**. View-only lint has **0 errors / 0 new warning identities**; raw warnings are **14 → 15** because the pre-existing `store` warning repeats in two destinations.

[Full report, failing witnesses, server diffs, and namespace headers](/home/forge/src/clj-surgeon-split/docs/observations/2026-09-08-namespace-split-papercuts-round2.md).