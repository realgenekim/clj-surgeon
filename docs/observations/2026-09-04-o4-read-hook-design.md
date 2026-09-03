# O4 — "Route, don't ask": the read-side hook, built and measured on the bench

*Built locally on Anvil as `forge`, no ssh, 2026-09-04. Branch `bridge/o4-read-hook`
off `origin/main` `ee90e249d3d0`; nothing merged, nothing pushed. Design source:
Opus's E6 result poll, `/home/forge/tmp/sol/e6poll-opus.md` §2 O4. Predecessors:
`2026-09-04-e6-lb-cohort.md` (free-choice adoption 0 of 3) and
`2026-09-04-e6c-routing-plate-cohort.md` (0 of 3 with the routing plate served,
connected and quoted back verbatim). Program record: **0 of 19.***

## The table first

| witness | command (verbatim) | result | route record |
|---|---|---|---|
| E6 arm command 1 | `rg -n -C 5 'System/currentTimeMillis\|\(ns marvin-voice-remote' src` | **diff EMPTY** (0 bytes) | `served_by surgeon`, 25 files, 227 ms |
| E6 arm command 2 | `rg -n -C 8 'System/currentTimeMillis\|\*now-ms\*\|\(ns ' src` | **diff EMPTY** | `served_by surgeon`, 25 files, 226 ms |
| E6 arm command 3 | `rg -n 'System/currentTimeMillis' src` | **diff EMPTY** | `served_by surgeon`, 25 files, 197 ms |
| `-l` form | `rg -l 'System/currentTimeMillis' src` | **diff EMPTY** | `served_by surgeon`, 25 files, 201 ms |
| long `-l` form | `rg --files-with-matches 'System/currentTimeMillis' src` | **diff EMPTY** | `served_by surgeon`, 25 files, 196 ms |
| `-e` form | `rg -n -e 'System/currentTimeMillis' src` | **diff EMPTY** | `served_by surgeon`, 25 files, 204 ms |
| fallback, non-Clojure path | `rg -n 'defn' .` | **diff EMPTY** | `fallback`, reason `non-clojure-candidate`, 17 ms |
| fallback, unsupported flag | `rg -n --max-depth 1 'System/…' src` | **diff EMPTY** | `fallback`, reason `unsupported-flag`, 16 ms |
| fallback, file argument | `rg -n 'System/…' src/…/channel.clj` | **diff EMPTY** | `fallback`, reason `path-argument-not-a-directory`, 11 ms |
| fallback, overlapping paths | `rg -n 'defn -main' src src/marvin_voice_remote` | **diff EMPTY** *(after the fix below)* | `fallback`, reason `overlapping-path-arguments`, 12 ms |
| exit status | match / no match / bad regex | **0 / 1 / 2**, ripgrep's own | — |

**Routed: 6 of 6 Clojure-scoped invocations. Fallbacks among them: 0.** Fixture:
a scratch clone of `marvin-voice-remote @ ab267f9` at `/tmp/o4-fx/mvr` (never the
working checkout). Read path: a clj-surgeon MCP server on **7941** built from
`bridge/study-ops-mcp 26e4810`, project-root pinned to the fixture. `ripgrep 15.1.0`.

**One line of learning:** routing the read side is *achievable and undetectable* —
100% of the commands the E6 arms actually typed were served through the Surgeon
read path with byte-identical output — and by that same construction this rung
saves **zero returns**, which is Opus's own prediction, now measured rather than
predicted.

**One caveat:** what the read path can own here is the *set of files searched*,
and nothing else. On a directory that is already all Clojure, the set the read
path returns and the set ripgrep would walk are the same set, so a transparent
rung cannot add value — it can only prove the seam exists. The value, if there
is any, is on the enriched rung, and that rung is detectable by construction and
needs its own cohort.

## What was built

`bin/rg-clj`, installed **as `rg`** first on an arm's `PATH`. Its argv is
ripgrep's argv; the agent types nothing new and Surgeon is never named. Decisions
live in `src/clj_surgeon/read_hook.clj` (pure); the shim does environment, HTTP,
subprocess and logging. Eight requirements, `MCP-OP-READ-HOOK-001..008`, in
`docs/intent/read-hook/`, with nine executable falsifiers in
`test/clj_surgeon/read_hook_test.clj` against a raw-socket stub of the
streamable-HTTP read path — hermetic, no server, no fixed port.

```
argv ─► parse-argv ─► servable? ─► ls-tree (SURGEON_URL) ─► set == ripgrep's own?
           │              │                                   yes ─► exec rg over the file list
           └── no ────────┴───────────────────────────────────  no ─► exec rg over the caller's argv
```

## Three measurements that decided the shape, before any code

1. **Ripgrep's directory output is not deterministic.** Twelve runs of E6's own
   first command over the 25-file fixture produced **twelve distinct SHA-256s**.
   "Byte-identical to ripgrep" therefore names nothing unless it is stated
   against `--sort path`, which ripgrep documents as single-threaded and stable
   (five runs, one hash). Every identity witness here is pinned there.
2. **Ripgrep's `--sort path` collation is not a byte sort of the same paths.**
   Substituting a `sort`-ordered file list moved `reducer/policy.clj`,
   `reducer/protocol.clj` and `reducer/shadow.clj` by 43 lines. The canonical
   order is therefore taken from ripgrep itself, via `rg --files --sort path`.
3. **The `ls-tree` receipt carries a namespace map, not matched lines.**
   `study/grep-tree` returns matching *paths* (`rg -li`); the formatters render
   forms and line spans. Nothing in the read path can reproduce a `-C 5` context
   block. So the division is forced: **the read path owns which files are
   searched; ripgrep owns what matches, how it prints, and the exit status.**

## The defect my own red team found, and the fix

The first green build **served** `rg -n 'defn -main' src src/marvin_voice_remote`
and produced a **non-empty diff** against ripgrep. Ripgrep prints a file once per
path argument that reaches it, **grouped by argument** — `core`, `server`,
`core`, `server` — while an explicit file list under `--sort path` groups by file
— `core`, `core`, `server`, `server`. The reconciliation compared *sets*, so the
duplicates collapsed and the multiplicity was invisible to it. Right lines, wrong
order, `served_by surgeon`, and every existing witness green.

The fix is two clauses and a refusal: the reconciliation compares counts as well
as sets, and an invocation whose ripgrep candidate list contains duplicates is
refused as `overlapping-path-arguments`. Both are witnessed
(`overlapping-path-arguments-are-refused`), the witness was RED before the fix on
exactly the `served_by` assertion, and the live receipt above is the same command
after it.

The general form is worth carrying: **a hook that reconciles by set equality is
blind to any property of a search that is not a property of its file set.**

## What this build does NOT claim

- **No wall claim, and the overhead is stated rather than hidden.** Per
  invocation on the fixture, ten runs each, at load 7.16 on 16 cores:
  native `rg` **8–13 ms** (median 9); hook **served** **237–328 ms** (median
  ~257), of which the `ls-tree` call is 197–292 ms; hook **fallback**
  **31–38 ms** (median 32 — babashka start plus the `--files` probe). Two rg
  calls in a 120-second arm is roughly **+0.5 s, about 0.4 % of wall**, two
  orders of magnitude inside E6's 172 s variance floor. It is a cost, not a
  saving.
- **No returns saved.** This rung returns exactly what ripgrep returns, so by
  construction it removes no round trip. `docs/vision.md`: *"A call must remove a
  return the agent would otherwise make."* This one does not, and says so.
- **No claim about `.`-rooted searches.** A whole-repository `rg` walks test,
  bench, dev and docs; the read path's source-path discovery does not. The sets
  disagree, `MCP-OP-READ-HOOK-007` fires, and the call falls back — measured
  above as `non-clojure-candidate` on this fixture. Routed percentage is
  therefore a property of *what the agent types*, and must be reported per
  command shape, never as one number.
- **No claim about a terminal.** The hook captures ripgrep's streams so it can
  report a byte count, which makes ripgrep see a pipe. When the hook detects an
  interactive console it inherits the streams instead and reports `bytes -1`;
  agent arms always pipe, so the cohort is unaffected.
- **No claim about `main`.** The witness server is `bridge/study-ops-mcp 26e4810`,
  which is **not merged** and does **not** contain the andon fix `a6df86ee`. Its
  `study/find-build-files` and `study/grep-tree` are argv-only in their own
  right, and the fixture is a tree this seat created; but no row here is a
  statement about `main`.

## The cohort that would measure it (not run here)

Pre-registration to be written before any arm, per the program's own rule.

- **Arms.** `N` = native, no hook, the frozen E6 N arms as the comparator.
  `H` = hooked: identical prompt, identical worktree, `PATH` prefixed with a
  private `bin` holding `rg → bin/rg-clj`, `SURGEON_URL` at the arm's own server,
  `SURGEON_ROUTE_LOG` at the arm's run directory. **The prompt is byte-identical
  between arms and never names clj-surgeon** — that is the whole point of the
  rung. Mirrored order, serial, `uptime` at each arm start.
- **Primary meter: routed percentage**, computed from the route log alone —
  `served_by == "surgeon"` over all invocations whose `reason` is not
  `path-argument-not-a-directory` or `non-clojure-candidate`; and separately, the
  raw share of all `rg` invocations that were routed. Both are deterministic from
  the log, not judged.
- **Pass line.** (a) every Clojure-scoped invocation routed, i.e. **zero**
  records with `served_by fallback` and a reason inside the servable class;
  (b) acceptance gate green in 3 of 3 (a gate, never a score); (c) wall inside
  the **172 s** floor and non-test actions inside the **6.1** floor; (d) **zero**
  runs in which the agent abandons the hooked path.
- **Reported, never claimed:** returns saved. The honest prior is **0**, and a
  measured 0 is the finding — it separates *"agents will not choose us"* from
  *"there was nothing to choose."*
- **Pre-flight, and it is not optional.** Assert `bb --version` and
  `rg --version` inside the arm's own environment before the arm starts, and
  assert that `command -v rg` resolves to the shim. The hook's interpreter is
  babashka; a shim whose interpreter is missing is not a degraded route, it is
  an outage on the agent's only discovery path, and it would look exactly like
  an agent that stopped using `rg`.
- **The apparatus ratchet that ships with it:** the route log makes the
  connection witness unnecessary for the read side — a record with
  `served_by surgeon` is proof, per invocation and bound to the arm's own run
  directory, that the read path answered. That is the first meter in this
  program that proves adoption without depending on the agent's cooperation.

## Deviations and refusals, every one

1. **The witness server is a branch tip, not `main` and not the E6 merge.**
   `bridge/study-ops-mcp 26e4810`, started with an explicit `:port 7941`,
   `:project-dir` pinned to the fixture, `:nrepl-port :none`. Recorded above.
2. **A start command was issued without an explicit port and would have targeted
   7888.** `clojure -M:clj-surgeon/mcp -m clj-surgeon.mcp-http-server` — whose
   default port is 7888, this box's production Surgeon. It died at
   `NullPointerException` in `clojure.main` before any socket was opened; the
   production listener's owning pid and uptime were checked afterwards and are
   unchanged. No harm done, and it is recorded because the near-miss is the
   finding: **the server is never started without `:port`.**
3. **The byte-identity witnesses on the real fixture are hand-driven, not in the
   suite.** They need a live Surgeon and the `marvin-voice-remote` clone; the
   suite's equivalents run against a hermetic stub on an ephemeral port. Both are
   recorded; neither substitutes for the other.
4. **Zero-path invocations (`rg PAT` with no path argument) are not served.**
   They fall back with `no-path-argument`. On a repository root the sets would
   disagree anyway; the refusal is cheaper and honest.
