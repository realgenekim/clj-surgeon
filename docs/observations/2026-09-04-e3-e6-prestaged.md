# E3 and E6, pre-staged — everything but the word "go"

*Written 2026-09-03T04:51:09Z by the Anvil seat (forge@anvil) under the night orders
(`docs/observations/2026-09-03-night-orders-anvil.md`, hill 3: "Pre-stage the morning's
measurement. E3 (fan-out verb vs native on the 21-owner rung) and E6 (study-ops free-choice
adoption) become runnable the moment q5z and study merge. Arm prompts, predicates, and the
pre-registered pass lines get written tonight so the morning starts with 'go'.")*

**Nothing in this file has been executed.** No server was started, no arm was run, no branch was
merged. This is a pre-registration: hypotheses, pass lines, arm prompts, attestation, refusal
ledger, scoring predicates, prerequisites, and the literal command sequence. It is written for a
reader who was not in the session that produced it.

**Where it runs (Gene, 2026-09-03):** both cohorts run **locally on this Anvil box, as `forge`** —
no ssh, no other seat. Runner roots `/home/forge/tmp/arms/e3/` and `/home/forge/tmp/arms/e6/`;
arm servers on ports **7907–7910**; one JVM suite at a time under
`flock /home/forge/tmp/suite.lock`; the arm driver is `~/bin/sol-yolo` (`codex exec`, gpt-5.6-sol)
or `claude -p`, named and fixed per arm (A.9); a watcher meters every tool call and every model
return and writes one `receipt.json` per arm (A.10). **`/home/forge/acid` is another seat's
territory: its `GO-*` files, `.cohort-lock` and `chain-*.sh` are never touched, and it is read
only for the two FAN fixtures.**

Authorities, all on `main` unless noted:
`docs/vision.md` (the battlefield, the four squares, the constraints, "the law of decisions",
"what winner is allowed to mean"), `docs/tech-tree.md` rows **E3** and **E6**,
`docs/observations/2026-09-02-captains-log-bridge-wall-clock-ideal-program.md` (the apparatus:
v1 variance floor 08:05Z, n1 12:45Z, call-site taxonomy 14:15Z, attestation ratchet 09:06Z,
"shape two" 11:38Z), `docs/observations/2026-09-02-captains-log-the-big-aha-and-reset.md`
(rs1 18:28Z, tweezer session 1 18:29Z/18:37Z, ritual audit),
`docs/observations/2026-09-02-slope-spec-sl1.md` (the prompt shape these arm prompts inherit),
`docs/observations/2026-09-02-acid-rung-L/` (`L-spec.md`, `L-prompt-main.md`,
`acid_L_acceptance_test.clj`), `docs/tweezer-loop.md` (the G0–G6 ladder),
`docs/closure-catalogue.md` (class D), and the two branches
`bridge/q5z-alias-migration` (`docs/intent/alias-migration/*`) and
`bridge/study-ops-mcp` (`docs/intent/study-ops/*`).

---

## 0. Three interpretive rulings a reader must see before anything else

These are judgment calls made while pre-staging. They are stated here rather than buried so that
the morning can overrule any of them in one sentence.

**Ruling 1 — "rung L and a purpose-built 21-owner parameter-threading rung" is read as a matched
PAIR at N = 21, and the variable held between them is whether the fan-out is tool-closable.**
Rung L (`docs/observations/2026-09-02-acid-rung-L/L-spec.md`) is already a 21-owner,
11-namespace parameter-threading rung: hoist `System/currentTimeMillis` into
`marvin-voice-remote.clock` and thread `clock/now-ms` through every call site, plus ten `ns`
`:require` edits. So E3 runs two rungs with the **owner count held constant at 21**:

| rung | fan-out shape | can `alias_migration` close it? |
|---|---|---|
| **E3-L** — the existing acid rung L, `marvin-voice-remote` @ `ab267f9` | 21 owners / 11 namespaces / 10 `ns` edits; the fan-out predicate is *textual* (`System/currentTimeMillis`) | **No.** `alias_migration` takes `from.lib` + `from.var`, a Clojure namespace and var. `System/currentTimeMillis` is host interop with no lib. The only applicable verb on this rung is `require_change` for the ten `ns` forms. |
| **E3-P** — purpose-built, generated at N=21 by the sl1 generator | 21 owners / 21 files that must be read / 3 sites each = 63 sites; the fan-out predicate is *structural* (a required lib + var) | **Yes**, in one call, payload constant in N. |

This is not a workaround; it is the experiment. `docs/vision.md` claims the fan-out square pays
only when *the tool discovers the owners*. E3-L is the boundary control that says what the verb
costs when it cannot discover them (the layering tax, measured at 2.12× non-test actions in n1);
E3-P is the positive rung. Running only E3-P would measure a synthetic best case with no
statement about the shape agents actually meet.

**Ruling 2 — a literal class-D parameter-threading rung is NOT built.** `docs/closure-catalogue.md`
line 109 records class D (parameter threading, ≥8 callers, ≥4 files) as **"none possible"**, and
C1's finding is explicit: *"class D (parameter threading) is the largest fan-out and NOT closable,
do not build param_thread."* A rung purpose-built to be class-D-pure would be a guaranteed-refusal
arm measuring a decision already taken. E3-L is the closest **real** class-D-shaped rung and
serves that role at zero build cost.

**Ruling 3 — no arm prompt mentions that Surgeon is "available and expected".** House rule,
measured: that phrasing is a standing 1.8–2.1× tax (`docs/tech-tree.md`, routing table, n1 12:45Z
receipt), and Gene pulled it from every Clojure agent prompt on 2026-09-02. The tool arms here use
the **mandated-shape** wording from `docs/vision.md` "the law of decisions" — *a harness that
routes the write through the verb* — which is a different instrument from an availability
advertisement. The free-choice arm (E6-F) uses the neutral wording only.

---

# PART A — shared apparatus (both experiments)

## A.1 The variance floor — the numbers no claim may ignore

Nine identical runs, same server, prompt, task, cores (`v1`, receipt `3e26e1c`; bridge log
Receipt 08:05Z). **Reproduced verbatim; do not re-derive it, do not re-measure it, cite it.**

| metric | mean | sd | min to max | 2 sd |
|---|---|---|---|---|
| wall s | 634 | 86 | 516 to 781 | **172** |
| total actions | 27.1 | 2.9 | 21 to 31 | **5.9** |
| non-test actions | 18.6 | 3.0 | 13 to 23 | **6.1** |
| MCP calls | 7.8 | 2.9 | 3 to 12 | 5.9 |
| input tokens | 1.84 M | 0.25 M | 1.29 M to 2.12 M | 0.51 M |
| acceptance failed assertions | 2.11 | 1.05 | **0 to 4** | 2.1 |

Consequences, binding on every table either experiment produces:

1. The wall spread is **(781 − 516) / 634 = 42 %** on identical inputs. **A wall difference
   smaller than 172 s is not a finding at n = 3.** Report it; never claim it.
2. **A non-test-action difference smaller than 6.1 is not a finding at n = 3.**
3. **The acceptance suite spans 0 to 4 failed assertions on identical inputs. It is a GATE, never
   a score** (`docs/vision.md`, "Cautions from the summer"; rule receipt `3e26e1c`). An arm either
   clears its gate or it does not; the failure count is never compared between arms.
4. **Wall and returns are two meters and are reported separately, always** (rs1, 18:28Z: the
   ritual strip removed 35 % of the returns and moved wall by +0.4 %, because on suite-bound rungs
   the wall IS the suite runtime). Never write "wall = returns".
5. Counts that are **deterministic** — how many calls to a named verb, whether a native
   `apply_patch` touched a `.clj` file, whether the acceptance gate is green — have no variance
   floor and may be claimed at n = 1. The primary observables below are chosen to be of this kind.

## A.2 Ports, hosts, and the hard boundaries

The seat's ports are **7906–7910 and nothing else**. 7906 is bound to the live session's
`.mcp.json` and is not free for an arm.

| use | port |
|---|---|
| seat's own live Surgeon (do not reuse) | 7906 |
| E3-P tool arm | **7907** |
| E3-L tool arm | **7908** |
| E6 free-choice arm | **7909** |
| spare / re-run | **7910** |

**Never contact 7888 (another seat's production Surgeon, user `surgeon`), 7894, or 7895 (cohort
servers).** Never touch `~/acid/GO-*`, `~/acid/.cohort-lock`, `chain-*.sh`, or any curtain-call
fleet directory. Native arms get **no MCP server configured at all** — that is what makes them the
positive control, and a native arm that can see a Surgeon port is void.

One full JVM suite per repo at a time under **`flock /home/forge/tmp/suite.lock`**. Check `uptime` before
launching; do not start a wave above load 8 on 16 cores.

## A.3 Run directory layout — everything runs LOCALLY on this box, as `forge`

**These cohorts run on Anvil, here, as the `forge` user. No ssh, no other seat, no fleet
directory.** The runner root is:

```
/home/forge/tmp/arms/<experiment>/          # experiment = e3 | e6
```

**It is never `/home/forge/acid`.** `~/acid` belongs to another seat: its `GO-*` files, its
`.cohort-lock`, and its `chain-*.sh` scripts drive a running battery, and a stray write there
wrecks it. This apparatus borrows `~/acid`'s *design* (armed cohorts, mirrored order, per-arm
attestation, flat receipt namespace) and none of its *files*. The one exception is read-only
fixture reuse (`~/acid/fanout/gen-fanout.clj`, `rescore-FAN.sh`) — and if those are not readable
by `forge`, see B.5.

Every arm-run owns one directory. Nothing is shared between arms except the read-only base repo.

```
/home/forge/tmp/arms/<exp>/<exp>-<rung>-<arm>-<slot>/     # e.g. .../e3/e3-P-T-1
    attest.edn        written BEFORE the driver starts (A.4); the run refuses on mismatch
    prompt.md         byte copy of the prompt actually served
    prompt.sha256     sha256 of prompt.md
    worktree/         git worktree or scratch clone, cut fresh from the pinned base
    server/           ready.edn + server.log + telemetry, for MCP arms only
    driver.log        the driver's own stdout/stderr
    rollout.jsonl     the driver rollout (codex) or transcript JSONL (claude -p)
    watch.jsonl       the WATCHER's record: one line per tool call and per model return (A.10)
    diff.patch        git diff vs base, frozen BEFORE any acceptance file is copied in
    receipt.json      the scored receipt (A.10) — the only artifact a reader must trust
    receipt.md        the human-readable row
```

The naming convention `<run>-g<g>-<arm>-<slot>` is the canonical one from runner v5; `g` (group)
is dropped here because these cohorts have one group. Receipts collect flat in
`/home/forge/tmp/arms/<exp>/receipts/`, mirroring `~/acid/receipts/<cohort>-score.md`.

## A.4 The attestation checklist — per arm, written before the agent starts

This is the ratchet installed on 2026-09-02 (bridge log Receipt 09:06Z, runner v5). It exists
because a receipt that does not name its subject is `:unverified`, never success. **Any value that
cannot be obtained is the literal string `"unverified"` — never empty, never omitted.** The arm
**REFUSES** (writes `ATTEST-MISMATCH` on the receipt line and never launches the agent) when the
port shows no pid, or when the server's project HEAD does not begin with the sha the runner
expected.

`attest.edn`, one map, every key mandatory:

| key | how it is obtained | applies to |
|---|---|---|
| `:exp` `:rung` `:arm` `:slot` `:group` | the runner's own arguments | all |
| `:start-utc` | `date -u +%Y-%m-%dT%H:%M:%SZ` **inside the write command**, never hand-typed | all |
| `:worktree` | absolute path | all |
| `:worktree-head` | `git -C <worktree> rev-parse HEAD` | all |
| `:base` | the pinned base sha the worktree was cut from | all |
| `:prompt-path` `:prompt-sha256` | `sha256sum prompt.md` | all |
| `:model` | the exact model id passed to the agent runner | all |
| `:runner-sha256` | `sha256sum` of the runner script itself | all |
| `:mcp-url` `:mcp-port` | as configured for this arm | T, F |
| `:expected-server-sha` | the branch head the runner was told to expect | T, F |
| `:healthz` | **`curl -fsS http://127.0.0.1:<port>/healthz`** — the JSON the server itself returns (`:ok :server :transport :host :port :url :pid :project-root`) | T, F |
| `:port-pid` | **`ss -ltnp 'sport = :<port>'`** — the pid actually owning the port | T, F |
| `:ready-pid` `:ready-project-root` | parsed out of that arm's `server/ready.edn` | T, F |
| `:server-project-head` | `git -C <ready-project-root> rev-parse HEAD` | T, F |
| `:mcp-absent-proof` | for N: `ss -ltn` shows no listener the arm's env can reach, and the agent config names no MCP server | N |

**Refusal conditions, all fail-closed:**
`:port-pid` is `"unverified"` → refuse. `:server-project-head` does not start with
`:expected-server-sha` → refuse. `:ready-project-root` ≠ the worktree the arm was told to serve →
refuse. `:worktree-head` ≠ `:base` → refuse (a dirty or wrong worktree). Arm **N** with any
reachable Surgeon port → refuse.

The live example this ratchets against: **port 7888 has a listener with no visible pid**, which
both the PORT-NOT-MINE check and the attestation refuse.

## A.5 Starting a branch server for a tool arm

`make mcp-serve` ignores `MCP_PORT` until `inb-d8a635` lands (it always binds 7888, which is
another seat's production server and is forbidden), so **never use `make mcp-serve` here**. Start
the server directly, from a checkout of clj-surgeon at the branch head, bound to the arm's own
worktree:

```bash
A=/home/forge/tmp/arms/e3/e3-P-T-1
mkdir -p "$A/server"
cd /home/forge/tmp/arms/e3/server-src            # clj-surgeon checkout at the branch head
nohup clojure -X:clj-surgeon/mcp \
  :project-dir "\"$A/worktree\"" \
  :port 7907 \
  :telemetry :full \
  :telemetry-dir "\"$A/server/telemetry\"" \
  :run-id "\"e3-P-T-1\"" \
  :ready-file "\"$A/server/ready.edn\"" \
  :nrepl-port :none \
  > "$A/server/server.log" 2>&1 &
```

Before the driver starts, both must succeed and both go into `attest.edn`:

```bash
curl -fsS http://127.0.0.1:7907/healthz            # 200 + the readiness JSON
test -f "$A/server/ready.edn"
```

`:project-dir` is the **arm's worktree**, not the clj-surgeon checkout. The tweezer-1 finding
(call 1) is that the server can derive a target namespace from *its own* root and ignore
`workspace_root`, so binding the project dir per arm is not optional.

One server per arm-run; kill it at the end of the arm (`kill $(sed -n 's/.*:pid \([0-9]*\).*/\1/p'
"$A/server/ready.edn")`) and **never signal a process this session did not start**.

## A.6 The typed refusal ledger

One row per refusal the tool emits, across all arms of both experiments. It settled what judge
scores could not (`3ed0f84`, `3ccc563`), and it is the primary metric whenever a wall difference
sits inside the floor.

| column | meaning |
|---|---|
| `exp` | `e3` or `e6` |
| `rung` | `L`, `P`, or `Lb` (rung L with the owner table removed — E6) |
| `arm` | `N` / `T` / `F` |
| `slot` | run index within the arm |
| `n` | the refusal's ordinal within that run |
| `t_offset_s` | seconds from `:start-utc` |
| `verb` | the MCP tool called (`alias_migration`, `require_change`, `inspect_clojure`, …) |
| `error_type` | the tool's own typed reason, verbatim (`alias-migration-expect-mismatch`, `study-form-not-found`, `invalid-compact-relation`, …) |
| `class` | `schema` / `semantic` / `scope` / `receipt` / `refusal` / `cleanup` (the tweezer-loop deviation classes) |
| `next_call_present` | did the refusal carry an executable `next_call`? (boolean) |
| `next_call_sent_verbatim` | did the agent send it unchanged? (boolean) |
| `returns_to_recover` | model returns between the refusal and the next successful write |
| `agent_visible` | could a **cold** agent have known the fix from the tool's own text alone? (boolean) |
| `abandoned_route` | did the agent stop using the verb after this refusal? (boolean) |
| `outcome` | `recovered` / `abandoned` / `fatal` |

Two derived figures are pre-registered as reportable: **refusal rate** = refusals ÷ tool calls, and
**recovery cost** = mean `returns_to_recover`. Known prior classes to expect and count separately:
`invalid-compact-relation` (every rung-L Surgeon run, `clj-surgeon-az8`, OPEN),
`invalid-intent-form` (2/3 of rung-M refusals, `clj-surgeon-xio`, OPEN),
`require-change-unprovable` (tweezer-1 call 12: a false refusal on an entry the call never
touched). A refusal with an empty diagnostic scores `agent_visible=false` and, by the G2 rule,
counts zero.

## A.7 Scoring predicates

**Pre-flight 0 (mandatory, once, before any arm runs).** The rollout field names below are the
codex JSONL shape as of 2026-09-02. Run this on ONE throwaway rollout and confirm each predicate
returns a plausible number before trusting any of them; a scorer that silently returns 0 is the
`verdict-label-was-a-noun` failure and it has happened in this program. Every predicate below
that globs must `set -o pipefail` and **abort on an empty glob** rather than print a zero.

```bash
A=/home/forge/tmp/arms/<exp>/<exp>-<rung>-<arm>-<slot>
R=$A/rollout.jsonl

# 1. MODEL RETURNS — the cost unit. One assistant message = one return.
jq -r 'select(.payload.type=="message" and .payload.role=="assistant")|1' "$R" | wc -l

# 2. TOTAL ACTIONS — every tool call the agent issued (shell + MCP).
jq -r 'select(.payload.type=="function_call")|.payload.name' "$R" | wc -l

# 3. NON-TEST ACTIONS — total actions minus any whose command invokes a test runner.
#    (n1 native mean = 10.0; v1 floor sd = 3.0, so 2 sd = 6.1)
jq -r 'select(.payload.type=="function_call")|(.payload.name+" "+(.payload.arguments//""))' "$R" \
  | grep -vcE 'kaocha|fan-test|bin/test|clojure -M:test'

# 4. TOOL CALLS BY VERB — deterministic; no variance floor; claimable at n=1.
jq -r 'select(.payload.type=="function_call")|.payload.name' "$R" | sort | uniq -c | sort -rn

# 5. WRITE CALLS THROUGH THE VERB (E3 primary observable).
jq -r 'select(.payload.type=="function_call" and (.payload.name|test("alias_migration|require_change|edit_clojure|apply_clojure_changes")))|.payload.name' "$R" | wc -l

# 6. NATIVE apply_patch LANDING FUNCTIONAL .clj BYTES (the fallback predicate; must be 0 in T).
jq -r 'select(.payload.type=="function_call")|(.payload.arguments//"")' "$R" \
  | grep -cE 'apply_patch' | tee /dev/stderr
grep -cE '^\+\+\+ .*\.clj$|^\*\*\* Update File: .*\.clj$' $A/diff.patch   # cross-check

# 7. ls-tree ADOPTION (E6 primary observable): did it happen, and how early?
jq -r 'select(.payload.type=="function_call" and .payload.name=="inspect_clojure")|.payload.arguments' "$R" \
  | grep -c '"mode"[[:space:]]*:[[:space:]]*"ls-tree"'
#    "how early": ordinal of the first such call among assistant returns
jq -r 'select(.payload.type=="message" and .payload.role=="assistant")|"RET"' "$R" \
  | cat -n | head -1   # combine with the ordinal from a single jq pass; see the scorer note below

# 8. DISTINCT SOURCE FILES OPENED BEFORE THE FIRST WRITE (E6 secondary).
jq -r 'select(.payload.type=="function_call")|(.payload.arguments//"")' "$R" \
  | awk '/apply_patch|alias_migration|edit_clojure/{exit} {print}' \
  | grep -oE '[A-Za-z0-9_/.-]+\.cljc?' | sort -u | wc -l

# 9. WALL — from the attestation start to the runner's own completion stamp. Never hand-typed.
python3 - <<'PY'
import json,datetime,sys,pathlib
a=pathlib.Path(sys.argv[1]); print(a)  # read :start-utc from attest.edn and :end-utc from run.log
PY

# 10. CHURN — insertions/deletions against the pinned base, taken BEFORE any acceptance
#     file is copied into the worktree.
git -C $A/worktree diff --shortstat <BASE>
```

**Churn pass line, E3-L:** the canonical rung-L change is **+59 / −34**; "within 20 percent" is
therefore **insertions in [47, 71] and deletions in [27, 41]** (`docs/tech-tree.md` E3;
bridge log 11:38Z "shape two"). **E3-P:** compute the canonical from `canonical-21/` at pre-flight
and apply the same ±20 %; do not hardcode a number this file cannot verify.

**Two definitions, restated exactly as the program measured them, because a table that means
something else is worthless:**

- **Model return ("cell") = one model turn.** One return may batch several shell sub-commands.
  The ritual audit measured both levels and they differ by ~2×: *model returns (cells) 839
  non-test / 288 unmandated (34 %)* versus *shell sub-commands 2,273 / 1,129 (50 %)*. **Report
  returns, and say which level you counted.** Wall is the sum of returns.
- **Non-test actions = total tool calls minus the calls that invoke the test runner, matched AT
  COMMAND POSITION**, with harness polls subtracted at 9 s each — the `edit_wall.py` definition
  (bridge log 12:0xZ / Receipt "edit wall"). Report `in-run test s` as its own column so the
  excluded cost is visible rather than hidden.

**The instrument that was WITHDRAWN, so nobody rebuilds it.** `grep -c` over the *run log* is not
a counter. It counted the prompt echo (10 hits before any act), `-Spath`, rejected commands, and a
`ps | rg` watchdog; it produced "s3-B ran kaocha 8 times" when the true number was 4, and the
suite-invocation counts 15/14/14/18/14/14 that it produced were withdrawn on the counter's own
authority. **Count from the rollout, keyed by tool call, never from the log's text.**

**The wall arithmetic to sanity-check every row against:**
`complete verified wall ≈ 9 s × model actions + ~50 s × test-suite runs`, with tool execution at
3–4 % of wall whatever the tool. A row that disagrees badly with that identity has a counting bug,
not a finding.

**One shell trap this apparatus has already hit.** `git add -A -- . ":!.cpcache"` **fails with
exit 1 when the ignored `.cpcache` exists**, because a negative pathspec makes git treat the
ignored path as explicitly named — and no FAN arm ever produced a diff because of it. Freeze
`diff.patch` with a plain `git -C "$A/worktree" diff <BASE>` and **write a `DIFF-FAILED rc=<n>`
line into the arm's log on non-zero exit** rather than letting an empty diff pass for a clean one.

**Churn, the mechanical formula.** For rung L the pass line is the raw `+59 / −34` band (B.2). For
any rung reusing the R3-family scorer, churn is
`total_clj_changed − new_ns_lines − removed_lines_matched_verbatim` (canonical native move = 53),
per `rescore-R3.sh` clause d.

**A scorer note that has cost this program a night before:** predicate 7 needs the ordinal of the
first `ls-tree` call *among assistant returns*, which is one ordered pass over the rollout, not two
independent greps. Write it as a five-line python pass, print the computed count, and **abort with
a non-zero exit if the rollout has zero assistant messages** — a hardcoded verdict word over a
missing number is exactly the `verdict-label-was-a-noun` defect.

## A.8 The ritual-strip block (goes into EVERY prompt of both experiments, identical bytes)

`rs1` (big-aha log, Receipt 18:28Z) measured this: returns 22.0 → 14.3 (**−35 %**), tool actions
17.5 → 11.0 (−37 %), tokens −27 %, **wall +0.4 % (flat)**. Cohort R and `rt2` add the mechanism:
**prohibiting a named artifact removes exactly the returns it names; telling an agent that
something is already known does nothing** (forbid −88 % of unmandated sub-commands; the
"there is no skill installed" clause was obeyed **not at all**, 3 of 3).

The block below is the sl1 rendering (`docs/observations/2026-09-02-slope-spec-sl1.md`, §4 of the
shared body), which is the committed, byte-stable form of the rs1 lines. **It is identical in
every arm of both experiments** — that is what makes the arms comparable — and it is quoted here
verbatim so a cold reader can paste it:

```
4. RITUAL

This worktree is throwaway and has no reviewer and no beads workflow: do not run bd, do not
run git status or git diff, do not re-read a file you just patched, do not hand-run
clojure -M -e syntax probes; the apply_patch result is your verification of the edit and
bin/fan-test performs the load check. Every extra command costs a full model turn.

Four specific things this environment will tempt you into, all of which are waste here:
  (i)   run the suite ONCE, with a single blocking wait; do not poll it,
  (ii)  target/ and .cpcache are generated; never clean them, never inspect them,
  (iii) there is no skill or playbook installed for this task; do not search the filesystem
        for one, and never run a find or rg rooted above this worktree,
  (iv)  report your total tool-call count on the last line as  TOOLCALLS: <n>.
```

Two substitutions when this block is used on rung L or rung Lb, and no others:
`bin/fan-test` → `bin/kaocha --focus marvin-voice-remote.bridge3-new-test`.
Clause (iii) is known to be inert (obeyed 0 of 3 in rs1) and is kept anyway **only** so the
prompts stay byte-comparable with the sl1 cohort; it is not expected to do anything.

`TOOLCALLS:` — not `TURNS:`. The old `TURNS:` line was a null instrument: it returned `1` from
every run, 6 of 6 (ritual audit, big-aha log). `TOOLCALLS:` was obeyed accurately (14/13/15 vs
measured 15/13/15). **The self-reported number is never the scored number** — the driver's
self-count undercounted by more than half on its first outing (18:37Z, 35 returns / 797 s against
a self-reported "15"). Predicates 1–3 in A.7 are the meter.

---

## A.9 The arm driver — two named options, one picked per arm and recorded

**Option S — `codex exec` as Sol (the default).** Live on this seat via the `genekkanban` pool.
The wrapper `~/bin/sol-yolo` already does exactly what an arm needs and already carries the
fences:

```bash
~/bin/sol-yolo <worktree> <prompt-file> [mcp-url] [report-path]
# -> codex exec --color never --skip-git-repo-check -C <worktree>
#      --dangerously-bypass-approvals-and-sandbox -m gpt-5.6-sol
#      -c model_reasoning_effort="high"
#      [-c mcp_servers.clj-surgeon.url="<mcp-url>"] -o <report> - < <prompt-file>
```

Three properties that make it the right arm driver, and one that must be respected:

- **The MCP server is bound per invocation** (`-c mcp_servers.clj-surgeon.url=…`). Omit the third
  argument and the arm has **no** Surgeon server configured — which is precisely what a native
  positive control requires, and it is a property of the command line rather than of a config file
  someone might forget to revert.
- **The worktree is the only fence** (`-C <worktree>`, YOLO). Each arm therefore gets its **own
  scratch clone**, never a shared checkout. The wrapper already refuses `/home/forge/src/clj-surgeon`
  because that checkout serves the seat's own MCP on 7906.
- **It runs on the ChatGPT subscription**, so twelve or eighteen arm-runs cost approximately
  nothing, which is the whole reason a battery is affordable at all.
- **Respect: always redirect stdin.** Every `codex exec` invocation in this apparatus ends
  `</dev/null` at the call site (the wrapper reads the prompt from `- < "$PF"`, so the *runner's*
  own shell must not leave a terminal on stdin) — the stdin-hang bug is a known way to lose a run.

**Option C — `claude -p` (the second caller).** Use it when the arm is deliberately varying the
caller. Tech-tree **E2** is open on exactly this question and it has *never been varied*: *"if
Claude also declines or layers, the finding is about the tool; if it substitutes, it was about
Sol."*

```bash
claude -p --model <model-id> \
  --output-format stream-json --verbose \
  --mcp-config "$A/mcp.json" \            # OMIT ENTIRELY for a native arm
  --add-dir "$A/worktree" \
  < "$A/prompt.md" > "$A/rollout.jsonl" 2> "$A/driver.log"
```

**The rule: one driver per ARM, the same driver across both arms of a comparison, and the exact
model id recorded in `attest.edn` `:model`.** A cohort whose native arm ran on one driver and whose
tool arm ran on another measures the drivers. For E3 and E6 as pre-registered here, **both arms of
every rung use Option S (Sol)**, because Sol is the caller every prior figure in this program was
measured on and the pass lines quote those figures. If Gene wants the E2 question answered in the
same morning, add a **third** arm (`T-C` / `N-C` on `claude -p`) as its own pair — never by swapping
a driver inside an existing pair.

## A.10 The watcher — the meter, and the one receipt a reader must trust

**The driver's self-count is never the reported figure.** On its first outing the driver reported
"15 returns" for a session the meter closed at **35 returns / 797 s**; both numbers were true under
their own definitions and only the meter's counts, *because a cold agent has overhead too and does
not get to call it something else*. Likewise `TOOLCALLS:` in the prompt is a **prompt-adherence
probe**, not a measurement: it is recorded and compared against the meter, and where they disagree
the meter wins and the disagreement is reported.

**The watcher is a shell wrapper around the driver, not a second agent.** It is event-driven per
call, never clocked (a clocked narrator manufactures relay hops). It writes `watch.jsonl`, one JSON
object per event, appended as the events happen:

```json
{"t":"2026-09-04T14:03:11Z","ms_since_start":41230,"kind":"return","n":7}
{"t":"2026-09-04T14:03:12Z","ms_since_start":42010,"kind":"call","n":7,"seq":11,
 "tool":"alias_migration","test_call":false,"args_sha256":"…","elapsed_ms":731,
 "outcome":"ok","error_type":null}
{"t":"2026-09-04T14:03:59Z","ms_since_start":89400,"kind":"call","n":8,"seq":12,
 "tool":"shell","cmd_head":"bin/kaocha --focus …","test_call":true,"elapsed_ms":9120,
 "outcome":"ok","error_type":null}
```

Fields, and why each exists:

| field | why |
|---|---|
| `kind` = `return` \| `call` | the two meters (A.7). Returns are model turns; calls nest inside them. |
| `n` | the model-return ordinal — this is what makes "was `ls-tree` called within the first 3 returns" answerable (E6's primary observable) |
| `seq` | the tool call's ordinal within the run |
| `tool`, `cmd_head` | which verb, or the first token of the shell command |
| `test_call` | **matched at command position**, per the `edit_wall.py` definition; non-test actions = `count(call) − count(test_call)` |
| `elapsed_ms` | tool execution, which should total 3–4 % of wall; if it does not, something is wrong with the run, not with the theory |
| `outcome`, `error_type` | feeds the typed refusal ledger (A.6) without a second pass |
| `args_sha256` | lets a later reader prove two arms sent the same payload without storing payloads |

Implementation: tail the driver's own JSONL (codex writes rollout events; `claude -p
--output-format stream-json` writes one JSON object per event) with a small `python3` reader that
stamps `date -u` **inside the write** — never a hand-typed time — and appends to `watch.jsonl`. It
must **abort with a non-zero exit if the run produced zero `return` events**, because a silent zero
is the `verdict-label-was-a-noun` defect and it has taken this program down before.

**Hard limits on the watcher, from the tweezer-loop protocol:** it may not suggest the next call,
repair arguments, interpret results for the driver, or edit files — otherwise it is an unmetered
copilot. It carries a hard 60-minute cap and an idle stop, both self-firing.

**`receipt.json` — one per arm, and the only artifact anyone downstream is allowed to cite.**
Written after the arm's gate has run and `diff.patch` is frozen:

```json
{
  "exp": "e3", "rung": "P", "arm": "T", "slot": 1,
  "driver": "codex-exec-sol", "model": "gpt-5.6-sol",
  "attest": {
    "start_utc": "2026-09-04T13:58:02Z", "end_utc": "2026-09-04T14:05:44Z",
    "worktree": "/home/forge/tmp/arms/e3/e3-P-T-1/worktree",
    "worktree_head": "…", "base": "…",
    "prompt_path": "…/prompt.md", "prompt_sha256": "…",
    "runner_sha256": "…",
    "mcp_url": "http://127.0.0.1:7907/mcp", "mcp_port": 7907,
    "expected_server_sha": "…",
    "server_sha": "…",              // READ FROM THE SERVER: ready.edn project-root -> git rev-parse HEAD
    "healthz": { "ok": true, "pid": 123456, "project_root": "…", "port": 7907 },
    "port_pid": 123456,             // from `ss -ltnp 'sport = :7907'`
    "attest_ok": true               // false => ATTEST-MISMATCH, the arm never ran
  },
  "meter": {
    "wall_s": 462,
    "returns": 9,
    "total_actions": 14,
    "test_actions": 1,
    "non_test_actions": 13,
    "in_run_test_s": 9,
    "tool_exec_s": 14.2,
    "self_reported_toolcalls": 14   // the prompt's TOOLCALLS: line, for adherence only
  },
  "verbs": { "alias_migration": 1, "shell": 13 },
  "writes": { "via_verb": 1, "native_apply_patch_clj": 0 },
  "churn": { "insertions": 63, "deletions": 21, "band": [47, 71, 27, 41], "within_band": true },
  "refusals": [
    { "n": 1, "t_offset_s": 88, "verb": "alias_migration",
      "error_type": "alias-migration-expect-mismatch", "class": "semantic",
      "next_call_present": true, "next_call_sent_verbatim": true,
      "returns_to_recover": 1, "agent_visible": true,
      "abandoned_route": false, "outcome": "recovered" }
  ],
  "gate": { "name": "rescore-FAN.sh 21", "green": true, "detail": "6/6 checks" },
  "notes": []
}
```

**Any receipt whose `attest.server_sha`, `attest.port_pid`, or `attest.prompt_sha256` is
`"unverified"` is `:unverified` and is not scored.** A false green is worse than an error, because
it terminates investigation.

# PART B — E3: the fan-out intent verb vs native

## B.1 Pre-registered hypothesis

> **A verb that takes a whole intent and discovers its own owners removes the reads native cannot
> avoid, and therefore removes model returns — but only where the owners are structurally
> discoverable. Where they are not, the same verb is pure layering cost.**

Derived from `docs/vision.md` square 2 ("Fan-out: one intent across N owners, the tool discovering
the owners… This is the only square where wall can go positive, and only here") and from the law
of decisions (an agent's cost is its count of decisions, not its count of edits).

**Directional predictions, written before any run:**

| | E3-P (structural fan-out, verb applies) | E3-L (textual fan-out, verb does not apply) |
|---|---|---|
| write calls through a verb | **1** | 1 (`require_change`, the ten `ns` forms only) |
| non-test actions, T vs N | T **below** N | T **at or below** N; layering if above |
| native `apply_patch` on `.clj` in T | **0** | > 0 and expected — there is no verb for the 21 sites |
| wall | T below N, but claimable only if the gap exceeds 172 s | parity; no claim either way |
| typed refusals in T | ≤ 20 % of tool calls | `invalid-compact-relation` is a live prior on this rung |

## B.2 The pass line, as numbers

Straight from `docs/tech-tree.md` row E3 ("one write call; non-test actions at or below 10.5;
churn within 20 percent; wall positive only on high fan-out") and bridge log 11:38Z ("Tests: rung L
non-test actions at or below native's 10.5; churn within 20 percent of the canonical +59/−34").

**E3 PASSES iff all six hold:**

1. **One write call.** In arm T on rung P, the count from predicate 5 is **exactly 1**
   `alias_migration` call that commits. (Deterministic; n=1 sufficient; a refusal followed by its
   own `next_call` counts as **two** calls and fails this line — say so in the receipt, do not
   round it away.)
2. **Non-test actions ≤ 10.5** in arm T, per run mean, on both rungs. 10.5 is native's own n1
   figure (native N mean 10.0, taxonomy 14:15Z), not an aspiration. **Failure threshold:** a T mean
   above `10.5 + 6.1 = 16.6` is layering, and is the same signature as the shipped per-form editor
   (n1: 21.2 non-test actions, 2.12× native).
3. **Churn within 20 %.** E3-L: insertions ∈ [47, 71], deletions ∈ [27, 41] against the canonical
   +59/−34. E3-P: within ±20 % of the churn computed from `canonical-21/` at pre-flight.
4. **Zero native fallback in T on rung P.** Predicate 6 returns 0 `apply_patch` hunks touching
   `.clj` in every arm-T run on rung P. One fallback on functional bytes is the sl1 falsifier
   verbatim: *"an incomplete closure hands N back as repair and is rf1 again."*
5. **Acceptance green in BOTH arms, as a gate.** E3-L: `acid_L_acceptance_test.clj` at
   **12 tests, 82 assertions, 0 failures**, the full suite at **577 tests, 7784 assertions, 0
   failures**, goldens byte-identical, and `grep -rn "System/currentTimeMillis" src/` printing
   **exactly one line**. E3-P: `rescore-FAN.sh <worktree> 21` green on all six of its checks. An
   arm that fails its gate is void and is re-run, never scored.
6. **Wall reported, claimed only above the floor.** A T/N wall difference below **172 s** at n=3 is
   written into the table and explicitly labelled *inside the floor, not a finding*.

**Falsifiers (any one closes the fan-out square and sends the effort to the gate):**
`alias_migration` refuses more than 20 % of its calls on rung P; or T falls back to `apply_patch`
on functional bytes on rung P; or T's non-test actions exceed native's on **both** rungs (the verb
is layering even where it applies).

**Positive control.** Native is an arm in every cohort. *"A benchmark that never includes 'do it
without the tool' cannot lose and so cannot learn."*

## B.3 n per arm, and why

Tech-tree budget for E3 is **12 arm-runs**. Allocation:

| rung | arm | n | runs |
|---|---|---|---|
| P (purpose-built, N=21) | N native | 3 | 3 |
| P | T tool, write mandated through the verb | 3 | 3 |
| L (acid rung L @ `ab267f9`) | N native | 3 | 3 |
| L | T tool | 3 | 3 |
| | | | **12** |

**Order is mirrored** (the z7c correction): run `P-N-1, P-T-1, P-T-2, P-N-2, P-N-3, P-T-3`, then the
same mirrored order on L. z7b's 0.76× was withdrawn precisely because its native arm ran slow —
native reads 327.7 / 432.7 / 348.2 across `rs1` / `z7b` / `z7c` on the identical prompt and base.
Unmirrored order buys a false winner.

Slots run **serially**, one at a time, so walls are comparable; contended walls are not (bridge log
07:25Z, e3 group 1: *"walls here are contended and not comparable to sequential runs"*).

## B.4 Arm prompts, verbatim

Both arms are **byte-identical outside §5**. The runner hashes both and records the hashes in each
attestation (A.4). Install as `/home/forge/tmp/arms/e3/prompts/{E3-P-N,E3-P-T,E3-L-N,E3-L-T}.md`.

### B.4.1 `E3-P-N.md` and `E3-P-T.md` — shared body (§1–§4)

```
You are working in a throwaway git worktree of a Clojure project. Do the task below and stop.
Do not commit, do not push, do not create branches or worktrees.

1. THE TASK

The namespace acid.fanout.store is being retired. Its var find-event has moved to
acid.fanout.store2 and been renamed fetch-event. Nothing else in the tree changes.

In EVERY namespace under src/ that requires acid.fanout.store:
  - replace that require with acid.fanout.store2,
  - rewrite every qualified use of the old var to the new namespace and the new var name,
  - choose the alias for the new require by this policy, applied to THAT FILE ALONE:
        prefer  store2
        if store2 is already bound in that file, use  st2
        if st2 is already bound in that file, use     es
        if es is already bound in that file, use      store-2
    Never introduce an alias that shadows an alias or a referred name already bound in
    that file. Different files will legitimately end up with different aliases.

There are exactly 21 such namespaces. src/ contains 100 namespaces in total; the other
79 must not change.

2. WHAT MUST NOT CHANGE

A local binding named find-event, the string literal "find-event", the token find-event
inside a docstring or a comment, and any use of find-event that resolves to a DIFFERENT
required namespace are all unrelated to this change and must be left byte-for-byte alone.
Comments, metadata, reader-conditional branches and #_ discards stay where they are.

3. DONE MEANS

  - every namespace under src/ loads,
  - bin/fan-test is green at its base count with an empty failure set,
  - no occurrence of acid.fanout.store or of the old var name remains in src/.

4. RITUAL

This worktree is throwaway and has no reviewer and no beads workflow: do not run bd, do not
run git status or git diff, do not re-read a file you just patched, do not hand-run
clojure -M -e syntax probes; the apply_patch result is your verification of the edit and
bin/fan-test performs the load check. Every extra command costs a full model turn.

Four specific things this environment will tempt you into, all of which are waste here:
  (i)   run the suite ONCE, with a single blocking wait; do not poll it,
  (ii)  target/ and .cpcache are generated; never clean them, never inspect them,
  (iii) there is no skill or playbook installed for this task; do not search the filesystem
        for one, and never run a find or rg rooted above this worktree,
  (iv)  report your total tool-call count on the last line as  TOOLCALLS: <n>.
```

### B.4.2 `E3-P-N.md` §5 — native arm (positive control)

```
5. TOOLING

You have your ordinary native tools only: shell, rg, sed, and apply_patch. There is no
structural editing server available. Use whatever route you judge fastest and safe,
including a scripted edit if you believe it is correct for this tree.
```

### B.4.3 `E3-P-T.md` §5 — tool arm, write routed through the verb

```
5. TOOLING

A clj-surgeon MCP server is configured. It exposes alias_migration, one call that takes the
whole intent — the old lib and var, the new lib and var, the alias policy in order, and the
number of files you expect to be affected — discovers every requiring namespace and every
qualified site itself, chooses each file's alias against that file's own bindings, and
returns one receipt: files changed, sites rewritten, the alias histogram, collisions
resolved, the kondo delta and the focused-test result.

Route the write through that call. Its receipt is your verification of the rewrite; do not
re-read the files it reports as changed. If it refuses, it returns an executable next_call —
send that. You still have your native tools; use them if the tool cannot complete the task.
```

*(Note for the reader: §5 of the tool arm is the sl1 tool block with one word changed — "Make
that one call" → "Route the write through that call" — which is the mandated shape named in
`docs/vision.md` "the law of decisions". It contains no availability advertisement, per Ruling 3.
The last sentence is deliberate and must not be removed: without an escape hatch the arm measures
compliance, not cost, and the s1 substitution mandate showed agents escape on writes anyway
(obeyed on reads, escaped on writes, +210 s).)*

### B.4.4 `E3-L-N.md` and `E3-L-T.md`

The shared body is **`docs/observations/2026-09-02-acid-rung-L/L-prompt-main.md` verbatim**, with
exactly three edits:

1. Delete its `## Reporting` item 5 (`TURNS: <n>`) and replace it with
   `report your total tool-call count on the last line as  TOOLCALLS: <n>.` — `TURNS:` is the
   withdrawn null instrument.
2. Insert the §4 RITUAL block from A.8 immediately before `## Verify`, with `bin/fan-test`
   replaced by `bin/kaocha --focus marvin-voice-remote.bridge3-new-test`.
3. Append §5 TOOLING as below.

`E3-L-N.md` §5:

```
5. TOOLING

You have your ordinary native tools only: shell, rg, sed, and apply_patch. There is no
structural editing server available. Use whatever route you judge fastest and safe,
including a scripted edit if you believe it is correct for this tree.
```

`E3-L-T.md` §5:

```
5. TOOLING

A clj-surgeon MCP server is configured. It exposes require_change, one call that adds or
changes a :require entry across many namespaces at once: you name the entry and the files,
and it splices each ns form without re-printing the rest of the file, returning one receipt.

Route the ten :require additions of CLAUSE 2 through that single call. Its receipt is your
verification of those ten edits; do not re-read the ns forms it reports as changed. If it
refuses, it returns an executable next_call — send that.

The 21 call-site replacements of CLAUSE 2 have no corresponding verb, because
System/currentTimeMillis is host interop and not a var in a required namespace. Make those
edits with your native tools.

You still have your native tools throughout; use them if the tool cannot complete a step.
```

*(This §5 is honest about the boundary on purpose. A tool arm told to route an inapplicable
intent through a verb produces a refusal cascade and measures the prompt, not the tool. Naming
what the verb does **not** cover is what makes E3-L a boundary control rather than a rigged loss.)*

## B.5 Prerequisites for E3

**Must be on `main` before the first arm runs:**

| # | branch | head as of 2026-09-03T04:34Z | why E3 needs it |
|---|---|---|---|
| 1 | `bridge/kondo-path-test` | `f8a9ef9` | **GO**; kills the exact-profile baseline failure. Merge first — without it the baseline suite is not clean and no acceptance gate is trustworthy. |
| 2 | `bridge/routing-doc-test` | `a9d8701` | **GO**; restores the MCP-OP-RELAY-004 paragraph. Merge second. |
| 3 | **`bridge/q5z-alias-migration`** | round 3, re-review `acb7e66e0b6224894` was running | **The experiment does not exist without this.** `alias_migration` is the verb under test. |
| 4 | `bridge/rf2-extract-rewire` | round 3 (`5839b52`), re-review running | Not strictly required by E3, but it carries the composition fixes (`:extract!` / `:ls` / `require_change`) that `docs/vision.md` "what winner is allowed to mean" demotes until merged. `require_change` is the E3-L tool arm's verb; run E3-L only after this is on main, or record in the receipt that the E3-L tool arm ran against the demoted `require_change`. |
| 5 | the memory battery (`MEM-001`/`MEM-011`) green for `alias_migration` | `bridge/txn-journal` + `bridge/clj-surgeon-membat` | **Hard gate for rung P.** `alias_migration` OOM'd on 450 files × 1.9 MB. Rung P is 100 namespaces, so it is probably inside the ceiling — but *probably* is not a receipt. Run `make memory-battery` (100/1k/10k files, explicit `-Xmx`, numeric pass lines) and quote its numbers in the E3 receipt. A verb that dies on the codebase it targets cannot be measured. |

**What the mayor must have merged:** items 1–4 above, in that order, each after an independent
executed re-review that finds no NO-GO item. Nothing merges from the Anvil seat. The current state
of every lane is `docs/observations/2026-09-03-merge-queue-for-mayor.md`.

**Fixtures that must exist on the box (verify, do not assume — the seat that wrote this file
cannot read `~/acid`):**

- `~/acid/fanout/gen-fanout.clj` (the sl1 generator; pure, deterministic, `--n N --seed 7`).
- `~/acid/fanout/rescore-FAN.sh` (the six-check mechanical acceptance for the FAN family).
- `marvin-voice-remote` reachable at `ab267f9`, and
  `docs/observations/2026-09-02-acid-rung-L/acid_L_acceptance_test.clj` on `main` (it is).

If `gen-fanout.clj` is absent or not runnable by the seat, **E3-P does not run** and the morning's
first act is to say so, not to substitute a different rung. E3-L can still run alone; the receipt
then carries only the boundary control, and says so.

**Pre-flight, in order, before the first arm (≈ 25 min):**

- **PF-1** — merge state confirmed: `git -C ~/src/clj-surgeon log --oneline -1 origin/main` shows
  q5z merged. Record the sha; it is `:expected-server-sha` in every T attestation.
- **PF-2** — generate the rung: `bb ~/acid/fanout/gen-fanout.clj --n 21 --seed 7`, producing
  `repo-21/`, `canonical-21/`, `manifest-21.edn`, tagged `fanout-21`. **The runner reads the tag's
  sha into each attestation — no hand-typed base.**
- **PF-3** — compute rung P's canonical churn: `git diff --shortstat` between `repo-21/` and
  `canonical-21/`. Write the ±20 % band into the pre-registration file before any arm runs.
- **PF-4** — G1 hand-drive of `alias_migration` at N=21, per `docs/tweezer-loop.md`: one recorded
  invocation pasted into the pre-registration, watcher on, **15–20 minutes**. It must do what its
  docstring says with nothing silently ignored. *This is non-negotiable and is the cheapest thing
  in this document.* `E1` cost the program a night because three red teams fed the gate unified
  diffs and the field's first real payload was `apply_patch` V4A; the free-choice arm would have
  found it in one run. Do not skip PF-4 to save twenty minutes.
- **PF-5** — scorer smoke: run every predicate in A.7 over the PF-4 rollout and confirm each
  returns a plausible non-zero number. Abort the morning if any silently returns 0.
- **PF-6** — `uptime`. Do not launch a wave above load 8.

## B.6 The "go" sequence for E3 — run here, as `forge`, on this box

Nothing below runs until PF-1..PF-6 are green. **No ssh. No other seat. No `~/acid` writes.**

```bash
#!/usr/bin/env bash
set -euo pipefail
export ROOT=/home/forge/tmp/arms/e3
mkdir -p "$ROOT"/{prompts,receipts,server-src}

# ---- 0. one JVM suite at a time, for the whole cohort
exec 9>/home/forge/tmp/suite.lock          # every `flock -x 9 <suite cmd>` below shares it

# ---- 1. install the four prompts of B.4, then hash them (the hash goes in every attestation)
for pr in E3-P-N E3-P-T E3-L-N E3-L-T; do
  test -s "$ROOT/prompts/$pr.md" || { echo "missing prompt $pr" >&2; exit 2; }
  sha256sum "$ROOT/prompts/$pr.md" | cut -d' ' -f1 > "$ROOT/prompts/$pr.sha256"
done

# ---- 2. the server source: clj-surgeon at the MERGED main (never the seat's own checkout)
git -C /home/forge/src/clj-surgeon fetch origin
MAIN_SHA=$(git -C /home/forge/src/clj-surgeon rev-parse origin/main)
rm -rf "$ROOT/server-src"
git clone --no-hardlinks /home/forge/src/clj-surgeon "$ROOT/server-src"
git -C "$ROOT/server-src" checkout --detach "$MAIN_SHA"
test "$(git -C "$ROOT/server-src" rev-parse HEAD)" = "$MAIN_SHA"      # prove the base

# ---- 3. rung P worktrees: one SCRATCH CLONE per arm-run, never shared (the YOLO fence)
for slot in 1 2 3; do for arm in N T; do
  A="$ROOT/e3-P-$arm-$slot"; mkdir -p "$A/server"
  git clone --no-hardlinks /home/forge/acid/fanout/repo-21 "$A/worktree"   # READ-ONLY source
  git -C "$A/worktree" rev-parse HEAD > "$A/base.sha"
done; done

# ---- 4. rung L worktrees: fetch the remote ref FIRST, then cut at the pinned base
git -C /home/forge/src/marvin-voice-remote fetch origin
for slot in 1 2 3; do for arm in N T; do
  A="$ROOT/e3-L-$arm-$slot"; mkdir -p "$A/server"
  git clone --no-hardlinks /home/forge/src/marvin-voice-remote "$A/worktree"
  git -C "$A/worktree" checkout --detach ab267f9
  git -C "$A/worktree" rev-parse HEAD > "$A/base.sha"
done; done

# ---- 5. run one arm. MIRRORED order, SERIAL, one at a time:
#            P:  N-1  T-1  T-2  N-2  N-3  T-3
#            L:  N-1  T-1  T-2  N-2  N-3  T-3
#        (the z7c form, literally "N Z N Z N Z | Z N Z N Z N" reversed between waves)
run_arm () {                      # run_arm <rung> <arm> <slot> <port|->
  local rung=$1 arm=$2 slot=$3 port=$4
  local A="$ROOT/e3-$rung-$arm-$slot"
  local PROMPT="$ROOT/prompts/E3-$rung-$arm.md"

  cp "$PROMPT" "$A/prompt.md"; sha256sum "$A/prompt.md" | cut -d' ' -f1 > "$A/prompt.sha256"

  # 5a. tool arms only: start the branch server bound to THIS worktree (A.5)
  local URL=""
  if [ "$arm" = T ]; then
    ( cd "$ROOT/server-src" && nohup clojure -X:clj-surgeon/mcp \
        :project-dir "\"$A/worktree\"" :port "$port" :telemetry :full \
        :telemetry-dir "\"$A/server/telemetry\"" :run-id "\"e3-$rung-$arm-$slot\"" \
        :ready-file "\"$A/server/ready.edn\"" :nrepl-port :none \
        > "$A/server/server.log" 2>&1 & )
    for _ in $(seq 1 60); do curl -fsS "http://127.0.0.1:$port/healthz" >/dev/null 2>&1 && break; sleep 1; done
    URL="http://127.0.0.1:$port/mcp"
  fi

  # 5b. ATTEST BEFORE THE DRIVER STARTS — refuses on mismatch, never runs the driver (A.4)
  bash "$ROOT/attest.sh" "$A" "$arm" "$port" "$MAIN_SHA" || { echo "ATTEST-MISMATCH $A" >&2; return 1; }

  # 5c. drive + meter. The watcher wraps the driver; the driver never reports its own numbers.
  python3 "$ROOT/watch.py" --arm "$A" -- \
    ~/bin/sol-yolo "$A/worktree" "$A/prompt.md" "$URL" "$A/driver-report.md" \
    < /dev/null > "$A/driver.log" 2>&1 || true          # a failed arm is a scored arm, not a crash

  # 5d. freeze the diff BEFORE any acceptance file touches the worktree
  git -C "$A/worktree" diff "$(cat "$A/base.sha")" > "$A/diff.patch" \
    || echo "DIFF-FAILED rc=$?" >> "$A/driver.log"

  # 5e. stop this arm's server (only a process THIS script started)
  [ "$arm" = T ] && kill "$(sed -n 's/.*:pid \([0-9][0-9]*\).*/\1/p' "$A/server/ready.edn")" || true
}

# ---- 6. the gates. Arm-independent, run AFTER diff.patch is frozen, under the suite lock.
#  rung P:
#    flock -x 9 bash /home/forge/acid/fanout/rescore-FAN.sh "$A/worktree" 21     # 6/6 checks
#  rung L (all four, in this order):
#    cp docs/observations/2026-09-02-acid-rung-L/acid_L_acceptance_test.clj \
#       "$A/worktree/test/marvin_voice_remote/acid_l_acceptance_test.clj"
#    flock -x 9 bin/kaocha --focus marvin-voice-remote.acid-l-acceptance-test    # 12 / 82 / 0
#    rm "$A/worktree/test/marvin_voice_remote/acid_l_acceptance_test.clj"
#    flock -x 9 bin/kaocha                                                       # 577 / 7784 / 0
#    flock -x 9 clojure -J-Dmvr.data.dir=target/check-pages-data \
#       -M -e '(load-file "scripts/check_pages.clj")'                            # goldens identical
#    grep -rn "System/currentTimeMillis" "$A/worktree/src/"                      # exactly 1 line

# ---- 7. score: A.7 predicates over rollout.jsonl + watch.jsonl -> receipt.json (A.10) per arm,
#         then the cohort table into $ROOT/receipts/e3-score.md, refusal ledger appended (A.6).
```

Three helper scripts this sequence assumes, all to be written at pre-flight and all under
`$ROOT/`: **`attest.sh`** (A.4 — writes `attest.edn`, exits non-zero on any refusal condition),
**`watch.py`** (A.10 — wraps the driver, writes `watch.jsonl`, aborts on zero returns), and
**`score.py`** (A.7 — emits `receipt.json`, aborts on an empty glob or a zero-return rollout).
None of them exists yet; **writing them is the first hour of the morning**, and PF-5 is the check
that they work. They are ~150 lines total and they are the difference between a cohort and a
rumour.

**Report shape (Gene's standing rule: the table first, then one line of learning, then one
caveat).** The E3 receipt opens with:

| rung | arm | n | wall s | model returns | non-test actions | write calls via verb | native .clj patches | churn +/− | refusals | acceptance gate |
|---|---|---|---|---|---|---|---|---|---|---|

then one line naming what was learned, then one caveat. **Every wall gap below 172 s and every
non-test-action gap below 6.1 is labelled inside the floor in the table itself, not in a footnote.**

# PART C — E6: study ops, free-choice adoption

## C.1 Pre-registered hypothesis

> **Exposing `:ls-tree` through the MCP read entrance makes a table of contents cheap enough that
> an agent facing an unfamiliar tree calls it once at the start and then reads fewer files. If it
> does not call it, the exposure failed — and that is the finding.**

`docs/tech-tree.md` E6 verbatim: *"agents call it once at the start and read fewer files; if they
do not call it, the exposure failed."* And its status line, which is the honest starting point:
*"BUILT 2026-09-02, adoption UNMEASURED … the free-choice cohort through the MCP has not run, so
no claim."*

**The prior is brutal and must be quoted in the receipt whatever happens.** Free-choice adoption
across this program on 2026-09-02 was **0 of 10** — `s1` optional ("fastest safe completion")
declined 3 of 3 at native speed; rf2's G5 cold shadow 0/1 and G5b **with the exact one-call command
named in the task's own terms** 0/1; rf1 4/4 CLI but 0/4 through the MCP extraction verb.
`docs/vision.md`: *"A tool's presence and name are not a path."* If E6 comes back 0 of n, that is
the eleventh consecutive negative and it closes the question, cheaply.

## C.2 The rung: rung Lb — rung L with the map torn out

E6 cannot use rung L as written, because `L-prompt-main.md` **CLAUSE 8 enumerates all 21 owners
with file:line**. That is precisely the discovery `:ls-tree` exists to remove; leaving it in
guarantees zero adoption for a reason that has nothing to do with the tool.

**Rung Lb** = `L-prompt-main.md` with these deletions and nothing else:

- delete **CLAUSE 8** entirely (the 21-owner table and the "line numbers are a starting map" line);
- delete the ten-file list in **CLAUSE 2** (keep the instruction, drop the enumeration);
- in the "Why" section, keep `22 textual occurrences of System/currentTimeMillis under src/`
  (the count is the task's own definition of done via CLAUSE 9; removing it would make the task
  unbounded rather than discovery-heavy);
- delete the CLAUSE 4 bullet list's `file:line` references if any survive.

Everything else — clauses 1, 3, 4, 5, 6, 7, 9, the Verify section — stays byte-identical. The
**acceptance suite is unchanged and arm-independent**: `acid_L_acceptance_test.clj`, 12 tests, 82
assertions, 0 failures. That is the whole point of reusing rung L: a discovery-heavy rung with a
mechanical, already-built, arm-independent grader.

Add the A.8 RITUAL block (with the `bin/kaocha --focus` substitution) and the `TOOLCALLS:` line,
exactly as in B.4.4.

## C.3 The pass line, as numbers

**PRIMARY — adoption. A count, deterministic, no variance floor, claimable at n = 1.**

> **PASS: ≥ 2 of 3 free-choice runs (arm F) issue at least one `inspect_clojure` call with
> `mode="ls-tree"`, and in at least one of those runs the first such call occurs within the
> agent's first 3 model returns.**

Rationale for "within 3 returns": the tech-tree prediction is *"agents call it once at the start"*.
An `ls-tree` call issued at return 14, after the agent has already read every file, is adoption
that bought nothing and is scored as `late` in the receipt, not as a pass.

**Pre-registered group-sequential extension, declared here so it is not p-hacking:** if arm F
adoption comes back **exactly 1 or 2 of 3**, run **three more F slots** (budget 9 arm-runs instead
of 6) and the pass line becomes **≥ 4 of 6 with at least two early**. If adoption is **0 of 3**, do
**not** extend — the prior is 0/10 and a fourth, fifth and sixth null run buys nothing.

**SECONDARY — reads, reported with min–max, never a bare mean.** Distinct `.clj`/`.cljc` files
opened by read tools before the first write (predicate 8). Pre-registered direction: **down** in F
versus N. **No measured variance floor exists for this observable**, so it is claimable only if the
two arms' ranges do not overlap at n = 3. Report the ranges; if they overlap, write *ranges
overlap, no claim*.

**Also reported, no pass line attached:** model returns; non-test actions (floor 6.1); wall (floor
172 s); tokens; every `inspect_clojure` operation used (`ls-tree`, `deps`, `topo`, `ls-deps`,
`ls-extract`, `outline`, `forms`, `owners`, `prepare-change`) with its count; whether any receipt
came back `truncated=true` / `read_complete=false` and what the agent did next.

**GATE, not a score:** the acceptance suite, both arms, exactly as in B.2 item 5. Acceptance spans
0 to 4 failures on identical inputs; it can never resolve arm quality here.

**FALSIFIER, pre-registered:** **0 of 3 adoption ⇒ the exposure failed.** The tech-tree E6 row moves
to Findings as a measured negative, `ls-tree`-through-MCP is filed alongside the other free-choice
nulls (0/11 now), and the remaining question becomes whether a *mandate* recovers it — which is a
different, later, and much less interesting experiment, because `docs/vision.md` already rules that
*"Free-choice adoption is the acceptance test. A feature the agent declines when the tool is
optional has not shipped, whatever the benchmark says under a mandate."*

## C.4 n per arm

Tech-tree budget for E6 is **6 arm-runs**.

| rung | arm | n | runs |
|---|---|---|---|
| Lb | **N** native (positive control, no MCP) | 3 | 3 |
| Lb | **F** free choice (MCP on 7909, not named as preferred) | 3 | 3 |
| | | | **6** (+3 under the C.3 extension rule) |

Mirrored order, serial: `Lb-N-1, Lb-F-1, Lb-F-2, Lb-N-2, Lb-N-3, Lb-F-3`.

**There is no mandated arm in E6, deliberately.** E6's question is adoption; a mandate answers a
different question and would eat the budget. If F adopts, the mandated cohort is the natural
follow-up and gets its own pre-registration.

## C.5 Arm prompts, verbatim

Byte-identical outside §5. Install as `/home/forge/tmp/arms/e6/prompts/{E6-Lb-N,E6-Lb-F}.md`.

### C.5.1 `E6-Lb-N.md` §5 — native positive control

```
5. TOOLING

You have your ordinary native tools only: shell, rg, sed, and apply_patch. There is no
structural editing server available. Use whatever route you judge fastest and safe.
```

### C.5.2 `E6-Lb-F.md` §5 — free choice

```
5. TOOLING

A clj-surgeon MCP server is configured for this workspace alongside your ordinary native
tools. Its read tool, inspect_clojure, answers structural questions about Clojure source
without you opening the files: mode "ls-tree" returns a table of contents for a directory
(namespace, requires, and every top-level form with its arglist and line span, filterable
by a content pattern or a namespace pattern); request operations "deps", "topo", "ls-deps"
and "ls-extract" answer dependency questions about one file; and "outline", "forms" and
"owners" address forms inside one file.

Use whatever route you judge fastest and safe. Nothing here is required.
```

*(Three things about this §5 are deliberate and must not be edited without re-registering the
experiment. First: it says what the tool **does**, not that it is preferred, expected, faster, or
recommended — that is the difference between an exposure and a mandate, and "available and
expected" is a measured 1.8× loser. Second: the closing line is the `s1` optional wording
("Use whatever route you judge fastest and safe"), which is the arm that declined 3 of 3, so E6's
result is directly comparable to that prior. Third: it names the capability in enough detail that
a decline is a **choice** and not a discovery failure — otherwise E6 measures the tool description,
not adoption, and we would learn nothing we could act on.)*

## C.6 Prerequisites for E6

| # | branch | state as of 2026-09-03T04:34Z | why |
|---|---|---|---|
| 1 | `bridge/kondo-path-test` `f8a9ef9` | **GO**, merge first | clean baseline |
| 2 | `bridge/routing-doc-test` `a9d8701` | **GO**, merge second | clean baseline |
| 3 | **`bridge/study-ops-mcp`** | round three `acffa7722710273de` was running against `docs/observations/2026-09-03-study-ops-rereview.md` (GO-WITH-FIX, items 1–11, blockers 1–4) | **E6 does not exist without it.** The four blocking items were `rg` flag injection via `grep`, read-eval on `deps.edn`, symlink escape, and unbounded pre-parse. E6 must not run against an unfixed build: an arm-run is an agent pointing a tool at a tree. |

**Hard gate: E6 runs only after `bridge/study-ops-mcp` is merged to `main` with the round-three
fixes AND an independent executed re-review that finds no NO-GO item.** The NO-GO doc
(`docs/observations/2026-09-03-study-ops-redteam-NO-GO.md`) stands until a re-review says
otherwise. This is not conservatism: `MCP-OP-STUDY-013/014/015` are the intents that make `ls-tree`
safe to point at an arbitrary directory, and E6 is exactly that act, six times.

**Pre-flight, in order (≈ 20 min):**

- **PF-1** — `origin/main` carries study ops; record the sha as `:expected-server-sha`.
- **PF-2** — build rung Lb from `L-prompt-main.md` per C.2, diff it against the original, and
  confirm the diff contains **only** deletions. A rung Lb that accidentally adds a hint is a
  different experiment.
- **PF-3** — confirm the acceptance suite still grades rung Lb: run
  `acid_L_acceptance_test.clj` against a clean `ab267f9` checkout and confirm the documented red
  baseline, **12 tests, 65 assertions, 39 failures (10 of 12 deftests red)**. If that baseline has
  moved, the grader is not the grader this file describes and E6 stops.
- **PF-4** — hand-drive `inspect_clojure mode=ls-tree` once against a rung-Lb worktree on 7909
  (G1, 10 min). Confirm the receipt is bounded, that `read_complete` is honest, and — the specific
  thing to look for — that a **truncated** receipt does not serve back the call just made
  (`MCP-OP-STUDY-007` says it must emit `next_action="narrow_scope"` at the maximum limit). A loop
  here would burn an arm-run's whole budget.
- **PF-5** — scorer smoke over the PF-4 rollout, especially predicate 7 (the ordinal pass). Confirm
  it aborts rather than returning 0 on an empty rollout.
- **PF-6** — `uptime`.

## C.7 The "go" sequence for E6 — run here, as `forge`, on this box

Identical machinery to B.6; only the rung, the prompts, the port and the arm letters change.

```bash
#!/usr/bin/env bash
set -euo pipefail
export ROOT=/home/forge/tmp/arms/e6
mkdir -p "$ROOT"/{prompts,receipts,server-src}
exec 9>/home/forge/tmp/suite.lock

# 1. build rung Lb per C.2 from L-prompt-main.md; diff it and confirm DELETIONS ONLY
diff -u docs/observations/2026-09-02-acid-rung-L/L-prompt-main.md "$ROOT/prompts/Lb-body.md" \
  | grep -E '^\+[^+]' && { echo "rung Lb ADDS text; that is a different experiment" >&2; exit 2; }

# 2. install the two prompts of C.5 and hash them
for pr in E6-Lb-N E6-Lb-F; do
  sha256sum "$ROOT/prompts/$pr.md" | cut -d' ' -f1 > "$ROOT/prompts/$pr.sha256"
done

# 3. server source at the MERGED main (study ops present)
git -C /home/forge/src/clj-surgeon fetch origin
MAIN_SHA=$(git -C /home/forge/src/clj-surgeon rev-parse origin/main)
git clone --no-hardlinks /home/forge/src/clj-surgeon "$ROOT/server-src"
git -C "$ROOT/server-src" checkout --detach "$MAIN_SHA"

# 4. six scratch clones at ab267f9, one per arm-run, never shared
git -C /home/forge/src/marvin-voice-remote fetch origin
for slot in 1 2 3; do for arm in N F; do
  A="$ROOT/e6-Lb-$arm-$slot"; mkdir -p "$A/server"
  git clone --no-hardlinks /home/forge/src/marvin-voice-remote "$A/worktree"
  git -C "$A/worktree" checkout --detach ab267f9
  git -C "$A/worktree" rev-parse HEAD > "$A/base.sha"
done; done

# 5. MIRRORED, SERIAL:  N-1  F-1  F-2  N-2  N-3  F-3
#    F arms: server on 7909, :project-dir = that arm's worktree, URL passed to sol-yolo.
#    N arms: NO url argument at all -> codex has no Surgeon server configured. That is the control.
#    Same run_arm body as B.6 step 5, with port 7909 and arm letters N/F.

# 6. gate: the rung-L acceptance sequence from B.6 step 6 (12/82/0, 577/7784/0, goldens, sweep)

# 7. score with A.7 predicates 1,2,3,4,7,8,9,10 + the refusal ledger -> receipt.json per arm,
#    cohort table into $ROOT/receipts/e6-score.md
```

**Report shape:**

| arm | slot | ls-tree called? | first call at return # | other inspect ops used | files read before first write | model returns | non-test actions | wall s | acceptance gate |
|---|---|---|---|---|---|---|---|---|---|

then the adoption count as a fraction (`k of 3`), then one line of learning, then one caveat.
**Lead with the adoption fraction; it is the number a reader would panic about.**

# PART D — the single "go", and what it costs

## D.1 The decision tree the morning executes

Everything below happens **on this box, as `forge`, with no ssh and no other seat involved.**
Runner roots `/home/forge/tmp/arms/e3/` and `/home/forge/tmp/arms/e6/`; arm servers on 7907–7910;
one JVM suite at a time under `flock /home/forge/tmp/suite.lock`; the arm driver is
`~/bin/sol-yolo` (`codex exec`, gpt-5.6-sol, high reasoning) unless an arm is deliberately varying
the caller, in which case it is `claude -p` and the pair is a separate cohort (A.9).

```
Are attest.sh, watch.py and score.py written and smoke-tested (PF-5)?
  no  -> write them FIRST (~1 h, ~150 lines). A cohort without a meter is a rumour.
  yes -> continue

Is q5z merged to main with an executed re-review finding no NO-GO?
  no  -> E3-P does not run. Say so. E3-L may still run (it needs only require_change).
  yes -> is `make memory-battery` green with numeric pass lines?
           no  -> E3-P does not run; a verb that OOMs on its target repo cannot be measured.
           yes -> PF-1..PF-6 -> GO E3 (12 arm-runs, ~2.5 h serial at ~10 min/run + scoring)

Is study-ops merged to main with an executed re-review finding no NO-GO?
  no  -> E6 does not run. Say so.
  yes -> PF-1..PF-6 -> GO E6 (6 arm-runs, ~1.5 h serial + scoring)
```

E3 and E6 use disjoint ports (7907/7908 vs 7909) and disjoint runner roots, but **they share the
one suite lock and the box's 16 cores**. Run them serially, E3 first (it is the one with a wall
claim in it, and contended walls are not comparable — bridge log 07:25Z: *"walls here are contended
and not comparable to sequential runs"*). If both must run in one morning, E6's arms may be
interleaved *only* into E3's scoring gaps, never into its arm-runs.

**Check `uptime` before each wave.** Do not launch above load 8 on 16 cores; a tree walk or a
second suite turns a measured wall into a measurement of the box.

## D.2 What a reader should refuse to accept from these experiments

Written now, so it cannot be negotiated later:

- **Any wall claim under 172 s at n = 3.** Every wall claim under 170 s of the summer was inside a
  floor nobody had measured.
- **Any acceptance-suite comparison between arms.** It is a gate. It spans 0–4 on identical input.
- **Any receipt whose attestation carries `"unverified"` in `:port-pid`, `:server-project-head`, or
  `:prompt-sha256`.** *"If the evidence source can transform, collapse, truncate, or omit the
  subject, the strongest honest receipt is `:unverified`."*
- **Any "pass" printed as a word rather than computed from a number.** Scoring steps print computed
  counts and abort on empty globs.
- **Any self-reported turn or tool count as the scored figure.** The meter is A.7; the driver's own
  count undercounted by more than half on its first outing.
- **Any figure that did not come out of `receipt.json`.** The watcher is the meter; the driver's
  own closing line is prompt-adherence data, nothing more.
- **Any E3-P result presented as a product claim.** Rung P is synthetic. sl1's standing caveat
  applies unchanged: *"a synthetic-only win is a finding about irregular fan-out, not a product
  claim."* The real-repo point is the sl1 anchor (cfp), not this.

## D.3 Open variables this pre-registration deliberately does not close

- **The caller has never been varied** (tech-tree E2). Every arm here runs one model. If E3-P's
  tool arm loses, "the finding is about the tool"; if it wins, it is a finding about the tool
  *with this caller*. Record `:model` in every attestation so a second caller can be added later
  without re-running the baseline.
- **Rung P's owner count is held at 21 to match rung L**, which means E3 measures a *level*, not
  the *slope*. The slope is sl1's job (N = 5/10/20/40/80). If E3-P passes, the honest next question
  is sl1's, not a wider E3.
- **The driver is fixed at Sol for both arms of every pair**, so the E2 question ("is this about
  the tool or about Sol?") is *not* answered by these cohorts. Adding `claude -p` as a third pair
  is one line of `run_arm` and 6 more arm-runs; do it as its own cohort, never by swapping a
  driver inside an existing pair.
- **Whether `require_change` is a "winner" is currently demoted** by `docs/vision.md` "what winner
  is allowed to mean" until the rf2 composition fixes merge. If E3-L runs before rf2 lands, its
  receipt must say so in the caveat line.


> **Prerequisite amendment (2026-09-03 09:4xZ, forge-anvil):** the E3 go-tree originally required `make memory-battery` green. The battery is RED on main *by design* on rows other lanes own (read-all, rename full-match); the condition E3 actually needs is that `alias_migration` ADMITS the rung's scope instead of OOMing — which bridge/q5z-alias-migration f51ceae provides (aggregate-bytes ceiling, typed refusal before any read; GO after nine executed rounds). Read the prerequisite as: q5z merged + `alias_migration` plan-only completes on the rung's worktree at -Xmx512m. The battery's own lines are the memory program's gate, not E3's.
