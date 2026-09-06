# Fast typist — provider benchmark (2026-09-05, ordered by Gene 22:2xZ)

Gene, verbatim: "let's create code where fast typer is ideally codex spark, and then either gpt oss on groq or open router. Let's benchmark which faster." and "what matters is wall clock time; codex cli may have startup costs, which is a fact of life."

Runner: clj-surgeon bridge/mission-ledger `bin/typist-run --bench --providers groq,openrouter,spark --dossier <d> --rounds 6` — k=1, rounds interleaved round-robin across providers, same apply/gate/acceptance, wall = process spawn (spark) or request send (APIs) to first verified result, startup charged. Predictions were logged before any run (captain's log 22:2xZ): API providers win on wall for bounded dossiers; Spark ~5–10 s (process start per call); falsifier = Spark median under 3 s on onesite.

## Rows so far (Groq only; the other two blocked at run time)

| dossier | provider | model | rounds verified | first-verified wall, sorted (s) | median | max | median response wall | median tok/s (provider-reported) | refusals |
|---|---|---|---|---|---|---|---|---|---|
| onesite | groq | openai/gpt-oss-120b | 6/6 | 0.48 0.62 0.69 0.75 0.95 1.39 | 0.72 | 1.39 | 0.68 | 481 | 0 |
| fanout | groq | openai/gpt-oss-120b | 1/6 | 5.98 | 5.98 | 5.98 | 4.77 | 477 | 0 |
| onesite | openrouter | openai/gpt-oss-120b | — | no key on this seat at run time (refused pre-network, exit 4) | | | | | |
| onesite | spark | gpt-5.3-codex-spark | — | usage limit ("try again at 11:37 PM" = 23:37Z); recorded as refusal, call wall 2.8 s to the refusal | | | | | |

Groq throughput is flat across dossiers (481 vs 477 tok/s); the fan-out dossier is slower only because it emits ~7x more diff. Single-candidate hit rate on fan-out was 1/6 here and 2/6 in the earlier k=1 rounds (pooled 3/12); onesite is at the ceiling at k=1.

Codex CLI startup floor on this seat, measured separately: ~3.5 s for a trivial low-effort Sol call, and the lean flags (`--ignore-user-config --ephemeral -c project_doc_max_bytes=0`) change nothing because this seat's Codex config declares no MCP servers. Spark's row will carry that floor; that is the fact of life Gene named.

Pending: OpenRouter row (key asked of the mayor), Spark row (after 23:37Z), then the three-provider fan-out table.

## OpenRouter rows (22:44–22:52Z) — routing is the whole story

The OpenRouter key arrived (shape `{:openrouter-api-key …}`; runner accepts it). First bench with OpenRouter's DEFAULT routing, then pinned to fast upstreams (`provider.order = [Cerebras, Groq]`, no fallbacks). Groq rerun alongside in every bench (interleaved), so its rows are fresh controls.

| dossier | provider (upstream) | rounds verified | first-verified wall, sorted (s) | median | max | median tok/s |
|---|---|---|---|---|---|---|
| onesite | groq | 5/6 | 0.62 0.75 0.85 0.86 0.95 | 0.85 | 0.95 | 478 |
| onesite | openrouter, default routing (CoreWeave ×2, DeepInfra ×3, SiliconFlow ×1) | 5/6 | 2.24 2.71 3.24 4.06 9.59 | 3.24 | 9.59 | n/a |
| fanout | groq | 4/6 | 3.69 4.44 5.79 6.54 | 5.12 | 6.54 | 452 |
| fanout | openrouter, default routing (DigitalOcean ×5, DeepInfra ×1) | 2/6 | 37.28 52.03 | 44.66 | 52.03 | n/a |
| onesite | groq (control rerun) | 6/6 | 0.50 0.68 0.69 0.76 0.95 1.27 | 0.72 | 1.27 | 479 |
| onesite | **openrouter → Cerebras** (6/6 routed there) | 6/6 | 0.42 0.43 0.48 0.55 0.57 0.72 | **0.52** | 0.72 | n/a (OpenRouter reports no timing) |
| fanout | groq (control rerun) | 3/6 | 5.38 6.49 7.67 | 6.49 | 7.67 | 447 |
| fanout | **openrouter → Cerebras** (6/6 routed there) | 3/6 | 1.49 1.63 1.69 | **1.63** | 1.69 | n/a |

Reading: the model is identical; the upstream decides the wall. OpenRouter's default routing lost by 4x (onesite) and 9x (fanout) because it chose slow hosts; pinned to Cerebras it beats Groq by 1.4x on the small output and 4x on the large one (the fan-out diff is ~2.5k completion tokens; 1.6 s wall implies well over 1.5k tok/s end to end, versus Groq's ~450). Hit rates are the same across providers (same model, same temperature): fanout single-candidate pooled tonight 13/36 on Groq, 5/12 on OpenRouter. So the fastest typist tonight is gpt-oss-120b on Cerebras via OpenRouter, pinned; unpinned OpenRouter is the slowest thing measured.

Standing order for the harness: OpenRouter calls always carry `provider.order` with no fallback; a routed-elsewhere response is a protocol violation, not a datum. Spark row still pending (23:37Z).

## Cerebras k=5 on the fan-out dossier (22:49–22:51Z) — the headline row tonight

Raw log: 2026-09-05-fast-typist-fanout-cerebras-k5.log; all 30 candidates routed to Cerebras (receipts).

| arm | rounds verified | first-verified wall, sorted (s) | median | max | candidates verified |
|---|---|---|---|---|---|
| F k=5, gpt-oss-120b on Cerebras (via OpenRouter, pinned) | **6/6** | 1.86 1.86 1.88 2.02 2.58 2.81 | **1.95** | 2.81 | 13/30 (43%) |
| F k=5, gpt-oss-120b on Groq (earlier tonight) | 6/6 | 4.98 5.29 5.44 5.63 5.66 6.50 | 5.54 | 6.50 | 15/30 (50%) |
| N, cold Sol (gpt-5.6-sol), one process per round | 5/6 | 20.96 22.66 26.66 27.77 42.63 | 26.66 | 42.63 | 5/6 |

Cold vs cold, startup charged both sides, same dossier bytes, same gate, same acceptance: five fast candidates on Cerebras reach a verified three-file fan-out change in a median 1.95 s, every round; one cold Sol author in 26.66 s, five rounds of six. 13.7x on wall. The caveats from the cohort doc all still apply (five-file fixture, bb gate, no warm-Sol comparison, no real-repo claim).

## Spark rows (23:38–23:40Z, after the usage-limit reset) — the three-provider table

Bench receipts: 2026-09-05-fast-typist-bench-<epoch>.edn (two files, onesite and fanout; Groq rerun alongside as the interleaved control).

| dossier | provider | rounds verified | first-verified wall, sorted (s) | median | max | median response wall |
|---|---|---|---|---|---|---|
| onesite | spark (gpt-5.3-codex-spark, effort low, one `codex exec` per call) | 4/6 | 5.75 7.41 7.53 12.86 | **7.47** | 12.86 | 6.71 |
| onesite | groq (control) | 6/6 | 0.48 0.70 0.78 0.83 0.89 0.98 | 0.80 | 0.98 | 0.77 |
| fanout | spark | **5/6** | 4.66 7.11 11.83 13.47 17.48 | **11.83** | 17.48 | 11.92 |
| fanout | groq (control) | 1/6 | 5.25 | 5.25 | 5.25 | 4.31 |

Prediction on record (22:2xZ): Spark 5–10 s on bounded dossiers, startup-dominated; falsifier = Spark median under 3 s on onesite. Observed 7.47 s: prediction held, falsifier not met. The fastest-sampling model is the slowest typist by wall on this harness because every call is a fresh Codex process (~3.5 s floor measured separately) plus its own reasoning at effort low.

The finding that was NOT predicted: Spark is the most RELIABLE single candidate on the fan-out dossier — 5/6 rounds against gpt-oss-120b's 1/6 in the same minutes (pooled gpt-oss single-candidate rate on fanout tonight 14/42 ≈ 33%). Spark's mistakes are fewer; its wall is ~7x Groq's and ~7x Cerebras's k=5. So the axes separate: gpt-oss on Cerebras/Groq is the fast, unreliable typist that needs k>1 and a gate; Spark is the slower, more reliable typist that needs neither on this dossier. Which wins a real task depends on gate cost: at a millisecond gate, five Cerebras candidates (1.95 s, 6/6) beat one Spark (11.8 s, 5/6); at a gate costing tens of seconds per candidate, Spark's reliability would win back the difference.

## Three-provider summary, wall to verified (medians, k=1 unless noted)

| dossier | Cerebras via OpenRouter (pinned) | Groq | Spark | OpenRouter default routing | cold Sol (reference) |
|---|---|---|---|---|---|
| onesite | 0.52 s (6/6) | 0.72–0.85 s (5–6/6) | 7.47 s (4/6) | 3.24 s (5/6) | 16.27 s (6/6) |
| fanout, k=1 | 1.63 s (3/6) | 5.1–6.5 s (1–4/6) | 11.83 s (5/6) | 44.66 s (2/6) | 26.66 s (5/6) |
| fanout, k=5 | 1.95 s (6/6) | 5.54 s (6/6) | not run | not run | — |

Gene's question answered: fastest typist by wall = gpt-oss-120b on Cerebras, pinned through OpenRouter; then Groq; Spark last, by an order of magnitude, because of the per-call process. Most reliable single candidate = Spark.

## Offline contract (from fix round 5, 03:4xZ) — chokepoint, not sandbox

**`TYPIST_OFFLINE=1` is a CHOKEPOINT, NOT A SANDBOX.** Receipts written under
it carry `:offline_contract "chokepoint-not-sandbox"`, and the same words are
in `bin/typist-run`'s module docstring, its header comment, `--print-key-paths`
output, and the `bin/typist-run-test` banner.

What it does:

- It refuses **the runner's OWN** spawn and connect paths at one chokepoint —
  every `os.system`/`os.popen`/`os.exec*`/`os.spawn*`/`os.posix_spawn*`,
  `subprocess.run|call|check_call|check_output|Popen`, `urlopen`,
  `http.client.HTTP(S)Connection` and `socket.create_connection` this process
  reaches, plus a `codex`/`claude` word scanned inside any shell string.
  Typed refusal, exit 6, before the call.

What it does **not** do:

- It does not confine children, and it is not a sandbox. Sol fence r4
  demonstrated the escapes and they are real: `bash -c /abs/path/codex`, a
  `PATH` reset, `env -i`, a re-exec from a spawned `python3`, a socket opened
  by a child. **Python monkeypatching plus a mutable `PATH` cannot make that
  impossible.** Only an OS-enforced process/network boundary or a strictly
  confined broker can.

**No such boundary is available on this box to this user.** Measured
2026-09-06 on Anvil as `forge`:

```text
$ unshare -rn true
unshare: write failed /proc/self/uid_map: Operation not permitted
$ command -v bwrap firejail
(no output; exit 1)
```

There is no `sudo` on this seat either. So the guarantee was made **witnessed**
rather than **enforced**, in two parts, both asserted at the end of
`bin/typist-run-test`:

1. **No real key is reachable.** Every runner subprocess the suite starts goes
   through one helper that always passes `--keys-dir` pointing at a fenced
   dummy-key directory under `/var/tmp/forge` (syntactically valid values, not
   real keys); the suite never reads `~/secrets`. The proof is the new
   `--print-key-paths` dry run, which prints resolved key **paths only** and
   never opens the files: both provider paths must resolve under the fence and
   none may touch `/home/forge/secrets`.
2. **Nothing was spent.** The `~/.codex/sessions` **path set** is compared
   before and after (a set, not a newest-mtime: a rewritten file is not a new
   session and a deleted one is not the absence of a new one), plus an egress
   canary — `~/.clj-surgeon/events.jsonl` must not have grown, and no `.edn`
   under the suite's own fx tree may carry `:cost_source "provider"` or a real
   `:upstream`.

Stated the way the banner states it: *a general-purpose child could still
spawn or connect; what this suite proves is that it did not (session set
unchanged) and could not have spent (no real key reachable).*

**The landing gates run the suite under this same contract** — the same
`bin/typist-run-test`, the same `--keys-dir` fence, the same two witnesses. A
gate that ran under a different, stronger claim would be asserting a boundary
