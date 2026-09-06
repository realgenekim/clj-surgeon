# ASTRA strict-3 review — 2026-09-06 20:09Z

ACK strict-3 CHANGES: nearly ready, but the published read example is not the one the receipt validates.

1. BLOCKING: published requests r1/r2 both name src/app/core.clj, r3 names src/app/db.clj, yet expect.files=3. The retained executed request instead names three DISTINCT files (maven/db.clj, maven/inbox.clj, maven/tweets.clj). Fix the public example to two distinct files with expect.files=2 and execute that same topology, or publish the three-distinct-file example actually executed. Exact-shape assurance must preserve repeated-file relationships, not just field names. This was precisely the avoidable refusal cost we wanted to remove.

2. Small copy correction: the fan-out published call has two edits entries, one with matches=2; the executed request has three entries with matches=1. Alias published count21 was instantiated as2. Describe receipts as schema examples instantiated against the fixture, not verbatim exact requests. Prefer publishing the actual self-consistent fixture examples with a brief instruction to substitute task inputs. Do not require new cohorts; a valid exact example execution is enough.

3. Remove the skill's “server runtime was about 2% of every measured wall” universal claim; “count complete verified task wall, including all discovery and repair costs” suffices. Also delete “or when one of these applies; each has no native equivalent or measured as a win” from Other capabilities. It reopens automatic eligibility via the list and wrongly sweeps extraction into an unqualified winning/no-equivalent category. Keep explicit user request or separately approved experiment as the exceptions to the performance default.

I accept the revised alias request, verification values, mutation-aware escape, and suspend-versus-noisy-loss distinction. The evidence document reports the narrow measured boundaries correctly. The three xray failures are disclosed as pre-existing, not claimed repaired. With the read-example correction and these small copy fixes folded, no broader redesign or experiment is needed before final GO.
