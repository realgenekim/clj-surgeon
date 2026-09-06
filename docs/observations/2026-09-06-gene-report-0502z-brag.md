# Gene report — 05:0xZ, "anything to brag about?"

## 1. Versus native (the table first; every number has a receipt on MCP/main)

| what | native | tool | ratio | correct | receipt |
|---|---|---|---|---|---|
| helper-extraction whole task, attested, preregistered two-SD hurdle | Sol 105.6 s | Sol + Surgeon 45.4 s | **2.50x, clears the hurdle** | 6/6 vs 6/6 | Astra epoch 2, 23:24Z |
| real rename, complete CLI wall, first paired forms cohort | 22.6 s, 4/4 | 7.4 s, 3/4 (one typed refusal kept) | 3.0x with a reliability loss | | Astra, 02:18Z |
| real rename, warm native (6 controls, SD 1.04) vs whole-file typist k=5 | 21.4 s, 6/6 | 1.9 s, 4/4 | 11.4x | | Fable A/B 4 + controls |
| same rename, gate 0.06 / 0.75 / 7.1 s | 29.7 / ≈18.4 / 24.8 s | 1.9 / 2.3 / 8.6 s | 15.7 / 7.9 / 2.9x | | Fable A/B 3 |
| resident JVM gate (replay) | | 0.73 → 0.13 s per candidate | 7.8x → 10.4x | zero verdict drift | Fable A/B 5 |
| unified-diff typist on a real file | 29.7 s, 3/4 | none, 0/20 | loss | | Fable A/B 1 |

## 2. To brag about
- **A tool that made a change we kept.** Commit 981372ee on Astra's branch is a real rename in clj-surgeon produced by one live gpt-oss candidate through the executor: verified, receipted, undoable, fence GO. First dogfood of the night, and it was the tool's own code.
- **The blind spot closed by construction, and it showed effects in fifteen minutes.** Your ruling ("telemetry belongs inside the fns, one JSONL") became `~/.clj-surgeon/events.jsonl`; Astra's build was writing to it within twenty minutes of the commit, before it had passed its own review. It now holds 68 lines from 14 processes: missions, refusals, a fallback, and the first five MCP tool calls.
- **Slowification worked on the night's own defects.** Six Sol fence rounds on my branch, each catching a real thing (a key reachable by the test suite, a bypassable offline guard, a rounding that turned a provider cost into a fiction), then LANDED as 3dda2a61. The Git seam: an executed Opus review found two blockers (machine commits authored as Gene; undo leaving a published ref), Astra's own reviewer found a third (a hidden submodule gitlink), all three closed and re-reviewed GO within ninety minutes.
- **The comment gap, twice refused correctly, then fixed to his contract.** The single largest eligibility gain: comments preserved by exact node identity, a typed `:forms-comment-moved` naming the expressions, no opt-in that would have reintroduced his false acceptance.
- **Two agents reviewing each other by execution, not by reading.** Every HOLD tonight came with a counterexample; every fix came with a witness; nothing routed around a refusal.
- **Ethnography as a one-shot.** `ethno` reads a pane and the ledger: Surgeon reads have stuck (65 CLI calls, 28 forms-scoped), the apparatus share is the mandate, and it costs seconds.

## 3. Losses, plainly
- Nothing of Astra's is on trunk yet; his branch is in fence r2 now. Routing is HOLD on five code findings (policy GO). The typist's advantage is gate-bound and cold-vs-cold on a five-file fixture; the honest real-file number with a real gate is 2.9x to 7.9x. Dogfood on his own code: one keeper, two refusals, five ineligible. Four in five of his workers' actions are apparatus. The typist did no work for anyone between 02:53 and 03:36Z.

## 4. Next
Astra: usage and utility (your order, relayed): M-1 live through the executor, sub-minute loop, ledger line either way, ethno after each attempt. Me: land his branch on GO, fix and land routing, install the plate with receipts, 06:00Z ethnography and checkpoint.
