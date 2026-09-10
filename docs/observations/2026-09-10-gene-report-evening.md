# Gene report — 2026-09-10 evening (window: 2026-09-09 08:30Z → 2026-09-10 19:40Z) — written 2026-09-10T19:43Z

## 1. Performance vs native

| task | native | Surgeon | verdict |
|---|---|---|---|
| Edit a deftest body (curtaincall-cfp) | Python slice 0.02 s (+0.40 s formatter) | MCP insert_after 0.75 s | native faster; both noise beside the 17–20 s test JVM |
| Change a require alias + rewrite its uses | Python regex 0.03 s (+1.53 s formatter) | MCP within+from/to 1.14 s, 3 lines churn; CLI 0.72 s | equal; Surgeon zero churn |
| Insert a defn after a named defn | Python splice 0.02 s (+0.93 s formatter) | MCP 1.43 s | native faster; CLI has no insert op |
| The same alias rewrite on the real file (31 occurrences of `events/`, 2 are the alias) | **19 URL routes silently broken, every gate green** | refused: expected 31, found 2; correct call lands 3/3 | Surgeon wins on the only axis that matters |

Source: 2026-09-10-python-vs-surgeon-form-edits.md, on the exact files Astra edited by hand. No new cohort vs native ran; the standing figures (row 5 E4 0.47 wall; whole-feature 135 s tool vs 131 s native) are unchanged.

**t, the apparatus clock**

| meter | 09-09 morning | now |
|---|---|---|
| landing gate | 175 s | 166–186 s (three landings today) |
| gates consumed at landing | consumed | consumed on 3ea3803e; reruns since (v3.7 consumer refuses custody by design) |
| landings on trunk this window | — | 4: gate lanes 3ea3803e, skiff install 59d8bc0c, skiff fixes 13635f74, (seat-path defaults in review) |
| ship versions installed | v3.3 | v3.4 → v3.7 |
| records publication wait | median 660 s on the code lease | 0.15 s measured; delivery parked at the cap |
| disk | 39 GB free | 112 GB free (78 GB reclaimed) |

## 2. Wins and losses

**Wins**
- Trunk is tagged `stable/2026-09-10` (state score 9) and `stable/2026-09-10.1`: the gate at 175 s, gates consumed at landing, censuses derived, and now installable on macOS. The first real Mac receipt came back green on install and red on the gate four ways; all four are fixed and landed within the day, with a fifth sub-item open by name.
- The findings under the Mac fixes: the Darwin memory reader had never executed anywhere; the socket overflow was the pending name, not the slot leaf; the temp-hygiene mount authority was Linux-only and would have failed every lane before anything else.
- Astra's consults set the plan: frontiers (fund the review entrance; give the CLI the machinery), the frozen Frame 7 protocol and consumer contract, the public-main merge plan with Gene's decisions filed.
- Stage 0 of the public-main plan: the public-surface scan found 14,695 fleet references in 62% of the tree and the strip took it to 447 plus an allowlist; three of those were live defects on trunk. The parity harness proved stable vs stable byte-identical on three specimens and the candidate identical modulo one deliberate change.
- The squeeze: a blocked records publication went from 660 s to 0.15 s, six controls and a corrected pair, and found five real holes in the old publisher (records landing on the code ref with OK printed).
- Item 3 answered by design, not by round eighteen: six valid moves change behaviour while the brief says preserved; parked on that finding.
- 78 GB reclaimed with a manifest and orphan patches.

**Losses**
- Checker work cost rounds: item 3 seventeen, item 1 four, the parity harness four, the seat-path defaults four. Each round found a real edge; each was one hole deeper. The cap now stops this at one round after the strongest rung, and two of the four are parked with a definition of done rather than landed.
- Nothing from the squeeze installed: the delivery parks on two unfinished qualification obligations, and its staging inherited drift from a parked builder editing an installed set.
- I force-pushed a feature branch once (`--force-with-lease`); disclosed, rule in memory: new branch name after a rebase.
- No Surgeon refactor has been merged into a product trunk: 58 accepted since 09-02, all on branches, worktrees or specimens.
- Sublime read for the window: 7. The state never regressed; the hours did.

## 3. Learnings → ratchets

| learning | ratchet |
|---|---|
| A duplicate of a fail-closed check is a fail-open check | classifier moved to src/ behind one write entrance (seat-path r4) |
| Presence is never evidence | item 1's generated mutation sweep; parity's replay-authorised rules |
| A checker's certification scope is the analyzer's scope | kondo as the sole resolver; every private inventory deleted |
| Start checker work at the strongest rung; cap the rounds | memory `checker-work-starts-at-the-strongest-rung` (+ cap clause) |
| Stub fixtures must be the producer's real bytes | memory addendum; parity's retained captures |
| Fence briefs in verification language | Sol's filter refused "attack/leak" wording once |
| A staged set is a proof artefact; never edit it after install | memory addendum |
| Never two records pushes from one checkout; never force-push | inb-78d458; memory addendum |
| Install, read INSTALL OK, then ship — never chained | memory addendum |
| A string is not a form; a count has to be true | the E4 specimen; route alias rewrites to within+from/to with a declared count |

## 4. What's next
1. Sol's verdict on the seat-path defaults (round 4, capped); then tag `stable/2026-09-10.2`.
2. The mayor: second Mac receipt on 13635f74 (inb-8818c3), Astra's session config in the scratch checkout (inb-a06f3c), the Curtain Call prod promotion (inb-e7c772/511a9e), the Buster benchmark (inb-8bd9ad).
3. Gene: D1–D8 for the public-main plan (inb-8c982e). Nothing publishes until answered; stage 0 stands as the manifest.
4. Parked with DoD: item 1 (observer), item 3 (design), the records-lane delivery (P7/P8), the parity harness (authenticate uncited captures).
5. Next builds per Astra: the review entrance; the single request-file CLI entrance (the missing insert op is its first concrete gap).
