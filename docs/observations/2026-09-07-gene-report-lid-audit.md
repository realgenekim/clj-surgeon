# Gene report — LID audit block (2026-09-07T19:41Z)

Orders: "Go back to all the commits done on anvil and buster on these repos (and maybe even surgeon too), confirm that updates integrate LID, and opportunistically put in HLD too." "Small batches… if we break things, let's fix as we go. Confirm run-tests etc actually run prolog. Work should happen on astra."

## 1. vs-NATIVE performance — none measured in this block (tock work; Surgeon unused by design)
Ethnography: the audits, the ratchet fixes and every Astra/Sol pass made zero Surgeon calls; the telemetry collector moved only where earlier blocks ordered tool runs (apply 77→84, inspect 20→33 across the day, all from dogfood/pair runs).

## 2. Block ledger

| batch | seat | repo | outcome | proof |
|---|---|---|---|---|
| audits ×3 (read-only) | Opus | mvr, CC, clj-surgeon | ledgers in docs/observations/2026-09-07-lid-audit/ | mvr 8/11 integrated, 4 promises unregistered; CC 127/295 integrated, 93 spec ids unenforced, 25 prose intents; clj-surgeon trunk gates green but contract MCP-OP-only (165/721 rows blind), mission-ledger subsystem unlinked, PERF-SENT gate never run |
| Prolog check | — | mvr, CC | CONFIRMED | swipl 10; CC runtests-once → test-oracles 5 files pass; mvr runtests-once → director-control-contract 4/4 |
| mvr batches 1–4 | Astra | marvin-voice-remote | PRs #49 #50 #51 #52 (stacked on #48) | tapped-OVER row + dedup replay witness; :cmd/over C01 / :cmd/cancel C02 + 8-cell witness; frozen flagless artifact pinned to golden + micGate=¬barge; ARCHITECTURE.md §13 cross-linked — every batch red-first by plant, kaocha 579–582/0, Prolog ran |
| CC batches 1–5 | Astra | curtaincall-cfp | PRs #8 #9 #10 #11 #12 (stacked on #7) | contract enforces all 93 spec ids (46 debts, shrink-only); eight prose promises linked, prose INTENT refused under test/; API-key scope matrix + route census (rung-e STOPPED → decision for Gene); email delivery through a provider sink (dev-log :sent → decision for Gene); HLD sections + tenets + references — kaocha 1024–1036/0, oracles ran |
| clj-surgeon batches 1–2 | Astra + Sol review | clj-surgeon MCP/main | LANDED f61769da → 42c55d96; LANDED 94715053 → eb8a7686 | contract prefix-agnostic with a per-ID debt ledger that only shrinks (Sol r1 caught the prefix allowlist letting debt GROW; r2 GO); PERF-SENT audit required by the merge gate; WTL/OP-ALG covered; HLD flagship ops + "two missions" |
| my own defects caught by gates | Fable | clj-surgeon | fixed 32d49892 | skill-mirror drift and a 74/81-line skill entrance (70-line contract) left run_all red on trunk for ~4 h; two refused lands; lesson in memory |

## 3. Wins and losses
Wins: three trunk landings, eleven stacked PRs, every batch verified through the FULL entry with Prolog after the first pass had used kaocha only; four real reviewer catches (allowlist growth, debt ledger shape, weak witnesses passing under narrowing, a scan used as the sole witness); two behaviour hazards surfaced and put to Gene instead of changed silently.
Losses: I announced three Sol runs as Astra (one-shot default model); a stale working-tree diff made me claim a fence artefact was committed (corrected); my skill edit broke a trunk gate for hours; the CC gap count (168) is an upper bound.

## 4. Learnings → ratchets
- Traceability contracts must cover EVERY id vocabulary and a debt ledger must only shrink (landed as tests in clj-surgeon and CC).
- Prose intents in tests are invisible; refused now in CC.
- Any skills/ edit: sync mirrors + 70-line check before push (memory).
- sol-yolo runs are Sol unless CODEX_MODEL=gpt-6-astra with the newer codex (memory).
- Compare commits with `git diff <sha-a> <sha-b>`, never against a worktree another process modified (memory).

## 5. Decisions pending Gene
1. CC PR #10: historical API keys without a stored scope read as organizer — refuse, migrate, or keep?
2. CC PR #11: may the dev-log mail arm keep reporting :mode :sent?
3. Fan-out auto-route narrowing on the plate (inb-b39c7c) — skill already narrowed.
4. Merge order: mvr 48→52, CC 7→12 (stacked; each independently green).

## 6. Open debt (filed)
clj-surgeon gap 2: the mission-ledger subsystem (16 ns, ~4.1k LOC, zero linked intents) — next batch (inb filed). CC: 46 allowlisted spec ids and 17 INTENT-NOTE prose items to retire batch by batch. HLD omissions beyond today's sections are listed in each ledger.
