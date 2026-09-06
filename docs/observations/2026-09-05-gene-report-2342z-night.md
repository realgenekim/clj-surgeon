# Gene report — the night of 2026-09-05 (Fable + Astra), written 23:4xZ

## 1. Versus native, measured tonight

**Astra's fair comparison, epoch 2** (his apparatus: attested subjects, cold server startup charged, preregistered two-SD hurdle, 36 runs, all correct; his figures verbatim; on astra/fair-profile-epoch2, not landed):

| arm | runs | median wall | p90 | correct | paired ratio | clears hurdle? |
|---|---|---|---|---|---|---|
| Sol native (paired) | 6 | 105.61 s | 134.22 s | 6/6 | | |
| Sol + Surgeon helper_extraction (one public call each, zero refusals) | 6 | 45.44 s | 49.50 s | 6/6 | **2.50x** | **yes** (saving 63.5 s vs hurdle 29.1 s) |
| Astra native (paired) | 6 | 45.18 s | 55.95 s | 6/6 | | |
| Astra + Surgeon | 6 | 35.02 s | 37.13 s | 6/6 | 1.30x | no (saving 10.1 s vs hurdle 11.7 s) |

Conditions in Astra's words: three known selected helpers, 21 homogeneous callers, 42 moved references, a preconfigured fresh-process behavioural profile, the public verb mandated, high-effort subjects. Establishes nothing about optional adoption or warm sessions. It is the first vs-native gain on this program's fair apparatus.

**Fast typist versus a cold careful author** (my apparatus: five-file fixture, millisecond gate, wall from request to verified result, startup charged, cold vs cold):

| task | gpt-oss-120b on Cerebras (pinned via OpenRouter) | gpt-oss-120b on Groq | Codex Spark (one process per call) | cold Sol |
|---|---|---|---|---|
| one-line edit, 1 candidate | 0.52 s (6/6) | 0.72–0.85 s | 7.47 s (4/6) | 16.27 s (6/6) |
| three-file fan-out, 1 candidate | 1.63 s (3/6) | 5.1–6.5 s (1–4/6) | 11.83 s (5/6) | 26.66 s (5/6) |
| three-file fan-out, 5 candidates | **1.95 s (6/6)** | 5.54 s (6/6) | not run | — |

## 2. Wins and losses

Wins
- **A preregistered, attested vs-native gain exists** (Sol 2.50x on the helper-extraction fixture), produced by Astra unprompted after he withdrew his own epoch-1 controls for clock and attestation defects.
- **The typist program went from idea to four cohorts and a three-provider benchmark in one night**, predictions written before each run. Findings that survive: the search mechanism (many candidates, one gate) is real only where a single candidate is unreliable; the upstream, not the model, sets the wall; the typist's natural edit format is context-anchored hunks; Spark is the most reliable single candidate and the slowest by wall.
- **One production change landed** (13c12401): every request-shape refusal now carries the field, the decision, and a runnable example, built from Sol's usability probe.

Losses
- **Spark's usage limit voided the first race**, and my first brief for it was wrong for the model. Ollama on CPU was a dead end (7.5 tok/s), measured and deleted on your order.
- **OpenRouter default routing is the slowest thing measured** (44.7 s on fan-out); an unpinned call is a measurement error.
- **Every typist ratio is cold-vs-cold on a toy fixture**; the warm-Sol comparison and any real-repo run are still owed, and arm T's whole-task negative for the typist stands.
- **The model flip on Astra's pane** (luna, then terra) has no receipt on any side; only the switch back to astra at 21:52Z is explained.

## 3. Learnings → ratchets
- Pin the upstream on every OpenRouter call, no fallback; record the upstream per candidate (done in the runner).
- A reasoning model's truncation (finish_reason length) is a typed refusal, never a wrong answer (done).
- Refusal text at the tail of stderr must be read whole; a truncated stream scored a quota refusal as a wrong candidate (fixed).
- Anchor refusals name the file block (done); failed candidates are retained, never dropped (done).
- Friction ledger: helper_extraction refused twice with the same reason → inb-a9b30e; filing needed a `clj` shim because this box lacks rlwrap (installed).
- Records only from the records worktree; the shared checkout incident from 15:04Z stayed contained.

## 4. What is next
- Morning: review and land Astra's epoch-2 branch through the fence and the landing gates (queued for the mayor; it carries bench code, not docs only).
- Typist: a warm-Sol arm with mirrored resident sessions (Astra's design), then one real-repo mission with a JVM gate, which is where the ratios will shrink and the honest number lives.
- Decide the fast-typist role in the mission ledger: k candidates behind the proof profile for fan-out missions; Spark or a single Cerebras call for known-site edits.

Headroom, count-first: Groq and OpenRouter spend tonight well under a dollar; Spark limit reset at 23:37Z and 12 calls used since; Astra's Codex meter not re-read since 14:35Z (10% weekly then).
