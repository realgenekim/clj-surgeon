# Read-Side Routing Hook — design and falsifier matrix

*Leaf prefix `MCP-OP-READ-HOOK`. Specifications: `read-hook-specs.md`.
Observation and cohort design: `docs/observations/2026-09-04-o4-read-hook-design.md`.*

## The shape

`bin/rg-clj` is a PATH shim. It is installed **as `rg`**, first on an arm's
`PATH`, so its argument vector is ripgrep's argument vector and the agent never
types a new command. Every decision it makes is a pure function in
`clj-surgeon.read-hook`; the shim itself does environment, HTTP, subprocess and
logging, and nothing else.

```
argv ──► parse-argv ──► servable? ──► ls-tree over the path args (SURGEON_URL)
                            │                     │
                            │                     ├── set == ripgrep's candidate set?
                            │                     │        yes ──► substitute-argv ──► exec real rg
                            │                     │        no  ──────────────────────┐
                            └── no ───────────────┴──────────────────────────────────┴──► exec real rg
                                                                                          (original argv)
```

**What the read path owns:** the set of files searched. **What ripgrep owns:**
which lines match, how they are printed, in what order, and the exit status.
That division is forced, not chosen — see "Why the hook does not print its own
matches" below.

## Why the hook does not print its own matches

Two measurements, both on `marvin-voice-remote @ ab267f9`, 2026-09-04:

1. **Ripgrep's directory output is not deterministic.** Twelve runs of
   `rg -n -C 5 'System/currentTimeMillis|\(ns marvin-voice-remote' src`
   produced twelve distinct SHA-256s. There is therefore no single byte string
   that "ripgrep's answer" names, and byte-identity can only be asserted
   against `--sort path`, which ripgrep documents as single-threaded and
   stable.
2. **Ripgrep's `--sort path` collation is ripgrep's own** and does not equal a
   plain byte sort of the same paths (`reducer/policy.clj` versus
   `reducer_session.clj` order out by 43 lines on this tree). The canonical
   order is therefore taken from ripgrep itself
   (`rg --files --sort path <original path args>`), which is the same call that
   supplies the falsifier set for MCP-OP-READ-HOOK-007.

3. **Overlapping path arguments interleave, and a set cannot see it.** Measured
   on the fixture, 2026-09-04: `rg -n 'defn -main' src src/marvin_voice_remote`
   prints each match twice, **grouped by argument** (`core`, `server`, `core`,
   `server`), while an explicit file list ordered by `--sort path` groups by
   file (`core`, `core`, `server`, `server`). An earlier build of this hook
   served that call and produced a NON-EMPTY diff — the right lines in the wrong
   order — because the reconciliation compared sets and the duplicates
   collapsed. The hook now refuses `overlapping-path-arguments`, and the
   reconciliation compares counts as well as sets.

The `ls-tree` receipt carries a namespace map, not matched lines: `study/grep-tree`
returns matching *paths* (`rg -li`), and `format-ls-tree-*` renders forms and
line spans. Nothing in the read path can reproduce a `-C 5` context block. So
this rung routes discovery and leaves matching where it is.

## Falsifier matrix

| id | falsifier (what must fail if the promise is broken) | required result | witness |
|---|---|---|---|
| 001 | Serve the three commands the E6 arms actually ran, plus `-l` and `-e` forms, and diff against ripgrep's own deterministic answer | empty diff, every form | `served-answer-is-byte-identical-to-ripgrep` |
| 002 | A non-Clojure candidate file under the path argument; an unsupported flag; a file path argument; an unreachable read path; a truncated receipt | ripgrep's own answer, unchanged, in every case | `unservable-invocations-fall-back-to-real-ripgrep`, `unsupported-flags-fall-back`, `unreachable-read-path-falls-back` |
| 003 | Run one served and one fallen-back invocation with a route log configured | exactly two records, carrying paths, flags, `served_by`, `ms`, `bytes` | `every-invocation-appends-exactly-one-route-record` |
| 004 | A pattern that matches, one that does not, and an argument vector ripgrep rejects | 0, 1, 2 — the same as ripgrep | `exit-status-is-ripgreps-own` |
| 005 | Put the shim first on `PATH` under the name `rg` and invoke it | terminates, with ripgrep's answer, never an exec loop | `real-ripgrep-is-never-the-hook-itself` |
| 006 | Serve an invocation whose single path argument is a directory | every line carries its filename prefix | `filename-prefix-survives-the-substitution` |
| 007 | Hand the hook a read-path file set with one file removed, and one with a file added | refuses to serve; falls back; the route record says so | `a-read-path-set-that-disagrees-with-ripgrep-is-refused` |
| 001, 002 | Two path arguments that overlap (`src` and `src/app`), so ripgrep prints each file once per argument | refuses to serve; ripgrep's own answer | `overlapping-path-arguments-are-refused` |
| 008 | Every fallback case in 002 | the hook's own streams are empty; only ripgrep's bytes appear | asserted inside the 002 witnesses |

## Environment

| variable | meaning | absent |
|---|---|---|
| `SURGEON_URL` | streamable-HTTP MCP endpoint of a clj-surgeon server scoped to the workspace | every invocation falls back |
| `SURGEON_ROUTE_LOG` | file the route records are appended to | no records are written |
| `SURGEON_RG_REAL` | absolute path of the real ripgrep | resolved from `PATH`, skipping the hook itself |
| `SURGEON_WORKSPACE_ROOT` | workspace root the read path is scoped to | walked up from the working directory |
| `SURGEON_HOOK_TIMEOUT_MS` | budget for the read-path call | 3000 |

## What this leaf does not promise

- **No wall claim.** The read-path call measured 204–245 ms warm against a
  25-file tree, on top of a ripgrep invocation that costs milliseconds. The
  hook is a routing instrument, not a speedup, and `docs/vision.md`'s own
  constraint is to count returns rather than milliseconds.
- **No returns saved.** This rung returns exactly what ripgrep returns, so by
  construction it removes no round trip. A rung that appends structural rows
  would be detectable by construction and needs correctness, not routing, as
  its primary.
- **No claim about `.`-rooted searches.** A whole-repository `rg` sees test,
  bench and dev trees the read path's source-path discovery does not; the sets
  disagree, MCP-OP-READ-HOOK-007 fires, and the invocation falls back. That is
  the intended behaviour and a measured quantity, not a defect.
