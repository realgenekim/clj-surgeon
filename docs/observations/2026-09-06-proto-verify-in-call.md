# Prototype hand-drive: verify inside the call (2026-09-06 21:0xZ, the-gene-maven seed, server 8171; measurement only, nothing committed; receipts /var/tmp/forge/proto-verify-fx/)

| mode | edit ms | verify ms | total | what the receipt tells the actor |
|---|---|---|---|---|
| A: no verify, actor runs the gate natively | 339 | 1,801 (separate turn) | ~2,140 + a turn | committed, bytes read back; nothing about tests |
| B: verify "fast", breaking edit | ~334 | 1,953 | 2,354 | refused verification-failed, source_unchanged=true, rolled_back=true, compiler error verbatim |
| C1: verify "fast", passing | ~295 | 2,205 | 2,569 | commit + verification.ok=true, per-check exit/elapsed |
| C2: consecutive verified call | ~232 | 2,018 | 2,311 | identical — cold every time |
| D: broken test assertion | ~318 | 2,002 | ~2,320 | expected/actual + "Ran 58 tests … 1 failures" verbatim in structuredContent |
Native gate for reference: 2.003 s cold, 1.80 s repeat. Server-side verify costs the same cold ~2 s; the :commands path forks per call; only a :hot (nREPL) profile would be warm and the seed has none.

## Root cause of tonight's alias_migration rollback under verify:"fast"
No .clj-surgeon.edn in the seed → the server's default profiles: fast = clj-kondo + standard-clojure-style on {files} (a LINT/FORMAT gate, not the repo's tests); full = make test (the seed has no make test). Both fail on PRE-EXISTING debt in untouched source (kondo exit 2 unused binding; formatter exit 1). The migration was correct; the profile was wrong. A ws-only .clj-surgeon.edn binding fast/full to `python3 proof/run.py gate` fixed it.

## Verdict
"Verify inside the call" costs ~2 s of server wall per edit and retires one actor turn plus the rollback decision; on failure the receipt carries the gate's own bytes (expected/actual, tally). It cannot retire: (1) cost — cold per call, N edits pay N×2 s where a warm actor-side REPL pays once; (2) the success TEXT block is identical for verified and unverified calls ("written bytes read back and verified" means bytes, not tests) — an actor reading text learns nothing and will re-verify; (3) the failure TEXT truncates mid-expression and drops actual/tally that structuredContent carries — the text ⊇ structured class again. Wall saving real; adoption saving not until (2)/(3) are fixed and the default profile is the repo's real tests.
