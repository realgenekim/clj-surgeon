# Row 2 — putting the decision in the ENTRANCE (`make alias-migrate`) (forge@anvil, 2026-09-08)

The two fresh-seat pilots (`2026-09-08-row2-fresh-seat-pilot.md`,
`2026-09-08-row2-fresh-seat-pilot-2.md`) routed 3/4 and 3/4 and named the failure mode:
**non-consideration** — the native cells issued zero ToolSearch and zero MCP calls and
never weighed a route at all. They also measured a second cost: cold routed callers spent
**89–115 s** of caller work where directed callers, handed a byte-frozen request, spent
**27 s**. The seat's standing finding is that free-choice *exposure* routing is dead;
route without asking.

So this round moved the decision into the **entrance**: both specimens now own
`make alias-migrate FROM=<lib>/<var> TO=<lib>/<var>`, and one line in each repo's boot
block names it. Then four fresh cells were run on the frozen row-2 task with **only that**
added.

Preregistration frozen before any cell:
`/var/tmp/forge/row2-entrance/design/prereg-entrance.md`, sha256
`d9ca023b59d16de0926f9a9a84dacbbbde52e9264e2f2b2d99347a3a8a6a55d8`. The brief handed to
each cell is **byte-identical to the pilots'** (template sha256 `0394a72f2834a23a…`), still
naming no route, no clj-surgeon, no MCP — and not naming `make alias-migrate` either.

## Headline

| cell | runner / model | route (independent witness) | entrance used | acceptance | caller work | orient→mut | mutation | mut→cand | actions |
|---|---|---|---|---|---|---|---|---|---|
| E1 | Claude Agent tool / opus | ROUTED-CALL-BOUND | **yes** | **accepted** (O1–O5 PASS) | 122.8 s | 82.9 s | 16.2 s | 23.7 s | 22 |
| E2 | Claude Agent tool / opus | ROUTED-CALL-BOUND | **yes** | **accepted** | 84.0 s | 56.7 s | 9.2 s | 18.1 s | 20 |
| E3 | `sol-yolo` / gpt-5.6-sol | ROUTED-CALL-BOUND | **yes** | **accepted** | 116.2 s | 54.8 s | 16.1 s | 45.3 s | 14 |
| E4 | `sol-yolo` / gpt-5.6-sol | ROUTED-CALL-BOUND | **yes** | **accepted** | 115.6 s | 53.5 s | 15.3 s | 46.8 s | 15 |

**Registered gate: SPLIT — the route clause PASSES, the caller-work clause FAILS.**

* **≥ 3/4 use the entrance unprompted — PASS, 4/4.** Every cell named `make alias-migrate`
  in its own report, and every cell has exactly one `alias_migration` receipt bound to its
  worktree by absolute path.
* **caller-work median ≤ 60 s — FAIL, 115.9 s** (mean 109.6 s).

And the outcome is identical everywhere: all four candidate patches are **byte-identical**
to each other, to the local witness W1, and to the pilots' routed golden —
sha256 `0649dd0bad24821981c73355fb6299a8b959922830dd016d332bf78faedfec1b`. 9 files,
21 sites, `alias_histogram {"mjson": 9}`, 3 collisions resolved, `string_mentions 0`,
suite 579 tests / 7,833 assertions / 0 failures in every cell.

## The number that moved, and the number that did not

| quantity | directed (stage 3) | cold pilots | **entrance cohort** |
|---|---|---|---|
| route adoption, unprompted | n/a (directed) | 6/8 (3/4 and 3/4) | **4/4** |
| caller work, median | 27.0 s (n=6) | 89.3 s (pilot 1, n=3) · 101.2 s (pilot 2 pooled, n=6) · 115.0 s (Claude routed, n=4) | **115.9 s (n=4)** |
| mutation window, median | — | 19.4 s (routed, n=4) · 50.2 s (native, n=3) | **15.7 s** |
| native caller work | — | 119.9 s registered · 133.9 s (pilot 2, n=2) | — |

Against native the entrance still wins (115.9 / 133.9 = **1.16x less caller work**), and
the mutation window alone is **3.2x** shorter than a native cell's. Against the *routed*
pilots it wins nothing: 115.9 vs 101.2 pooled, 115.9 vs 115.0 Claude-only. With n=4 these
are signals, not estimates.

## One learning

**The entrance fixed the decision, not the clock — because the clock was never spent on
the decision.** Composing the request is 9–16 s of a 84–123 s cell. Break the stamps and
the shape is unmissable:

* **orientation (orient-start → mutation-start): 53.5–82.9 s**, median 55.8 s — reading the
  task, establishing the 21/9 shape, checking alias occupancy in nine namespaces, and (in
  three of four cells) *reading `bin/alias-migrate` itself* before trusting it;
* **the entrance call: 9.2–16.2 s** — one `make` line, one receipt;
* **self-verification after the receipt: 18.1–46.8 s** — every cell re-read the diff to
  confirm the protected prose survived, even though `string_mentions=0` is a field on the
  receipt it had just printed.

Orientation plus post-hoc verification is **86–89%** of caller work in every cell. A gate
of ≤ 60 s was therefore unreachable by *any* change to the request-composition step, and
the 27 s directed figure is confirmed as a property of the directed harness — an arm handed
frozen bytes and told to make no other mutation skips both of the expensive phases.

What the entrance *did* buy is the thing the pilots said was broken. Adoption went from
6/8 under free choice with the plate present, to **4/4** with a repo-owned verb — and it
did so through two different discovery paths: **E4 read `AGENTS.md` and `CLAUDE.md`**
(1 read each) and then the target; **E3 never opened either boot file** and found the
target in the `Makefile` (23 hits, 0 boot-file reads). E4 called it "the *prescribed*
entrance" — a `make` target reads as house policy in a way a plate section does not.

The second thing it buys is a floor under the failure mode. In the pilots, the native cell
PA2 rewrote a protected docstring with a line-offset `sed` and only caught it by grep;
here no cell could reach the prose at all, because none of them edited bytes. Four cells,
four byte-identical patches, zero prose contact.

**Corollary — the next lever is the receipt, not the verb.** Every cell paid 18–47 s
re-deriving what the receipt already asserted. `fresh-caller-wants-a-gate-not-a-second-editor`
is the standing note; this cohort prices it at roughly a third of caller work.

## One caveat

**The discovery probe — the part of the entrance meant to remove orientation — was never
exercised by a cell.** All four passed `EXPECT_FILES=9`, because the task file states
"exactly 21 live sites in exactly 9 caller files"; E1 read `bin/alias-migrate`, noticed the
probe path, and deliberately asserted the count instead. So the field evidence covers the
asserted branch only. The probe is witnessed locally (W1 on the mvr fixture: discovered
`expect.files = 9` from a fail-closed `expect.files=0` probe, then committed 9/21/3 in
4.9 s wall; and the cc witness, which discovered 1) — but a task whose file count is *not*
given is untested, and that is precisely the task shape where the probe would matter.

Secondary caveats: n=2 per runner cannot separate model from runner from availability; and
the `≤ 60 s` threshold was registered against a directed comparator that, as this cohort
shows, measures a different set of phases.

## The entrance, as shipped

`bin/alias-migrate` + one `Makefile` target + one line in `AGENTS.md` and `CLAUDE.md`,
identical in both specimens:

```
make alias-migrate FROM=old.lib/old-var TO=new.lib/new-var \
     [ALIASES=a,b,c] [SCOPE=src,test] [EXPECT_FILES=n] [EXCLUDE=path,...]
```

* Composes the exact request the routing plate specifies — `from` lib+var, `to`
  lib+var+`alias_policy`, `scope.paths`, `expect.files`, `workspace_root` = the repo root —
  and sends it to the installed clj-surgeon. The CLI at `ffccb016`/`d3d6e8a5` has **no**
  alias-migration op (`clj-surgeon :op :alias-migration` → `{:error "Unknown op"}`), so the
  entrance speaks MCP directly over `http://127.0.0.1:$SURGEON_MCP_PORT/mcp` (default 7906;
  it refuses 7888/7890/7894/7895 by name).
* `scope.exclude` defaults to the file that defines the target library — a library is never
  one of its own callers.
* `alias_policy` defaults to the target library's last segment.
* With no `EXPECT_FILES`, it runs a **fail-closed discovery probe** (`expect.files=0`;
  receipt carries `mutation_attempted=false`, `source_unchanged=true`) and reads the true
  count out of the server's own refusal. An **asserted** `EXPECT_FILES` is never
  auto-repaired: that assertion is the caller's guard, and a mismatch refuses.
* Prints the receipt's terminal lines — `state`, `files`, `sites`, `collisions`,
  `alias_histogram`, `string_mentions`, `next_action`, `receipt`, `undo_receipt`, and on a
  refusal `refusal_price` and the executable `next_call` — then one summary line:
  `ALIAS-MIGRATE <accepted|refused> files=<n> sites=<n> collisions=<n> receipt=<path>`.
* Typed, fail-closed refusals: `missing-argument`, `malformed-argument`, `empty-scope`,
  `bad-workspace`, `forbidden-port`, `capability-unavailable` (exit 4, reported as
  UNVERIFIED, never a route choice), `server-refusal`. **There is no hand-edit fallback**,
  and every refusal says so.

The boot line, in a new `## Entrances` section directly under the title of **both**
`AGENTS.md` and `CLAUDE.md` in each repo (the harness preserves these files; the inlined
COLDSTART bytes come from the skill and were not touched):

> For an alias/require migration with a known from→to: `make alias-migrate FROM=… TO=…`
> is the entrance; it emits the receipt.

## Witnesses (run before the cohort)

| witness | repo | result |
|---|---|---|
| W1 — the frozen row-2 task via `make alias-migrate`, no `EXPECT_FILES` | mvr | probe discovered 9; **committed 9 files / 21 sites / 3 collisions**, `{"mjson": 9}`, `string_mentions 0`, 4.9 s wall; receipt path exists; unchanged stage-3 oracle **O1–O5 PASS, ACCEPTED: YES**; patch byte-identical to the golden |
| W2a–d — missing `TO`, missing `FROM`, empty `SCOPE`, malformed `TO` | mvr | four typed refusals; `git status --porcelain --untracked-files=all` md5 identical before and after → **no mutation** |
| cc-1 — missing `TO` | curtaincall-cfp | typed refusal, no mutation |
| cc-2 — a real migration (`cheshire.core/generate-string` → `clojure.data.json/write-str`, `SCOPE=src`) on a throwaway worktree | curtaincall-cfp | probe discovered 1; **committed 1 file / 1 site / 1 collision**, `{"djson": 1}`; worktree discarded |

## Declared apparatus treatment — grading-time de-treatment

The stage-3 oracle hardcodes `SEED_SHA=6dcdbc9…` and requires the changed-path set to be
*exactly* the nine caller files, so the four treatment files would have been scored against
the agent. Declared in the preregistration before any cell and applied identically to all
four: before grading, `git reset --mixed 6dcdbc9…`, `git checkout -- Makefile AGENTS.md
CLAUDE.md`, `rm -f bin/alias-migrate`, and (for the Sol cells) `rm -rf .codex/` — the parent
pilot's own CX-1 rule applied to a treatment rather than to a runner artifact. **The oracle
was not modified and its allowlist was not widened.** Verified on W1 before the cohort:
after de-treatment `check-untracked.sh` lists exactly the nine caller files. All four cells:
`UNTRACKED-PATH CHECK: PASS`.

PRIMARY wall (`orient-start → oracle-complete`: E1 837.2 s, E2 616.1 s, E3 445.4 s,
E4 256.2 s) includes operator latency before grading and grading concurrency, and is **not
comparable across cells**; caller work is the clean figure, as in both pilots.

## Paths

* Entrance, shipped: `marvin-voice-remote` `nrepl/test-alias` **9d4a09d** (was `8a97a5f`);
  `curtaincall-cfp` `nrepl/test-alias` **16419006** (was `f37b3fe8`) — `bin/alias-migrate`,
  `Makefile`, `AGENTS.md`, `CLAUDE.md`.
* Preregistration: `/var/tmp/forge/row2-entrance/design/prereg-entrance.md`
* Apparatus: `/var/tmp/forge/row2-entrance/apparatus/{setup-cell.sh,grade-cell.sh,route-witness.sh}`,
  attestations `attest-E{1..4}.txt`
* Cells: `/var/tmp/forge/row2-entrance/E{1,2,3,4}/` — `brief.md`, `stopwatch.log`,
  `route-witness.txt`, `de-treatment.txt`, `grade.log`, `oracle/`, `candidate-vs-entrance-seed.patch`,
  agent report/transcript
* Fixture treatment commit: seed repo `entrance-seed`
  `31ff5b9d8608d5c0a141b9de70bfac409ee716e6` on top of the pilot seed
  `6dcdbc9db67a91179b5940a16c16b760583f2603`
* Witnesses: `/var/tmp/forge/row2/W1/` (oracle, candidate patch)
