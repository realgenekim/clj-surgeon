# Delivery probes — the Codex arms of the gate-vs-prompt experiment

Design: `clj-surgeon-records docs/observations/2026-09-11-gate-vs-prompt-codex-arm-astra.md`
(Astra's, frozen). Amendment 1 names the five Codex arms scored here:
**L, P, M, X0, X1** — Astra's sixth arm **N** (no CLI on PATH) is **not run**, and
its row in the bet table stays unmeasured. Everything below was measured
**before** any scored run, and every probe session is excluded from every scored
denominator.

Astra's rule, honoured here: *"A failed launch attestation remains a delivery
failure in the assigned-run ledger, not a zero-adoption result."* Two real
delivery failures were found and fixed before scoring, and one scoped delivery
**ceiling** was found and published rather than adapted around. The runs then
**falsified half of that ceiling**, and the correction is written into section 3
beside the original claim rather than replacing it.

---

## 0. Preflight on the shared apparatus

| check | result |
|---|---|
| Surgeon MCP endpoint `http://127.0.0.1:7906/mcp` | `initialize` OK, `serverInfo` = `clj-surgeon experimental` |
| `tools/list` | **13 tools**; `insert_forms` present, `rename_alias` present |
| CLI `~/bin/clj-surgeon` | stable launcher, receipt `:source-commit "04648059bd666fb3e40bfdaee7e0e327c273740d"`, `:source-hash "748ccd78…"` — the **same trunk** as the server |
| `codex-cli` | `0.153.3` at `/home/forge/.local/bin/codex` |
| auth | ChatGPT subscription, reused by symlinking the seat's `auth.json` into each arm's `CODEX_HOME`. No API key: `OPENAI_API_KEY` / `CODEX_API_KEY` are `env -u`'d off every launch. |
| shim refusal/allow matrix | `bin/shim-selftest.sh` — **20 / 20 PASS** (`bin/shim-selftest.log`) |

The shim matrix asserts *both* halves of every row — the decision **and** the
bytes: every blocked row leaves the target byte-identical, every allowed row
actually performs the edit and keeps the real executable's exit code. It covers
`sed -i`, `-i.bak`, `--in-place`, `-Ei`, `-i -e SCRIPT`; python `-c`, script
file and stdin; read-only shapes; a `.txt` counterexample; the `.cohort/requests/`
exemption; and the redirect counterexample Astra insists on (`: > t.clj`
truncates **before** any shim starts — recorded as prior damage, never as a
prevented write).

---

## 1. Two delivery failures found and fixed before scoring

**(a) The seat's own global instructions reached every arm.** The first probe's
rollout carried `world_state.agents_md.text` = the seat's `~/.codex/AGENTS.md`,
which is a 10,708-byte **clj-surgeon routing plate**. `--ignore-user-config` does
not suppress it (Astra predicted exactly this). Left in place it would have given
L, X0 and X1 the plate and destroyed the P−L contrast entirely. The same session
was also served `<skills_instructions>` and `<recommended_plugins>` from the
seat's `CODEX_HOME`.

*Fix:* a **per-arm isolated `CODEX_HOME`** under `/home/forge/.codex-cohort-fx/<ARM>`,
each containing only `config.toml`, `installation_id`, and a **symlink** to the
seat's `auth.json` (so token refresh stays in one file and no credential is
copied anywhere). No `AGENTS.md`, no `skills/`, no `plugins/`, no other MCP
registration. The seat's own `~/.codex` is not edited.

**(b) The repo-level `.codex/config.toml` did not deliver MCP.** Astra's design
(and this seat's memory) says arm M gets its Surgeon table from a gitignored
`.codex/config.toml` in the launch cwd. Measured: with the file planted and the
project marked trusted via `-c projects."<wt>".trust_level="trusted"`, arm M's
probe listed **no clj-surgeon tools** ("No clj-surgeon MCP tool was offered"). A
`-c mcp_servers.clj-surgeon.url=…` override also produced **NONE**. A
`--strict-config` run with a deliberately invalid key in that file did **not**
error, which says the project file was never loaded at all under
`--ignore-user-config`.

*Fix, and the deviation it entails:* the Surgeon MCP table is registered in **arm
M's own `CODEX_HOME/config.toml`** (`codex mcp add clj-surgeon --url
http://127.0.0.1:7906/mcp`, plus `startup_timeout_sec = 30`, `tool_timeout_sec =
120`), and `--ignore-user-config` is dropped (each arm's user config is now a
frozen file this runner wrote, not the seat's). The **treatment is unchanged** —
arm M has the MCP entrance and no other arm does — but the *carrier* differs from
the design's sentence. The worktree `.codex/config.toml` is still planted,
byte-identical in shape across arms, and hashed into every run's manifest; it is
recorded as **inert**. `codex mcp list` under arm L's home: *"No MCP servers
configured yet."* Under arm M's: `clj-surgeon  http://127.0.0.1:7906/mcp  enabled`.

---

## 2. The ten probes (one per arm per model)

One sacrificial Codex session per cell, on a fresh detached worktree of
`clj-surgeon` at `a8452157…`, with a throwaway `probe/fixture.clj`. Each was
asked to print its PATH resolution, the first line and length of its effective
AGENTS text, its non-shell tool inventory, one real MCP read if it had one, and
the exact result of `sed -i 's/AAA/BBB/' probe/fixture.clj`.

| probe | AGENTS chars (arm text) | `sed` / `python3` / `clj-surgeon` resolve to | Surgeon MCP | shim journal | fixture after |
|---|---|---|---|---|---|
| L / gpt-6-astra | 390 (L = 390 B) | all three `<wt>/.cohort/bin/*` | none offered | 1 would-block | **changed** |
| L / gpt-5.6-sol | 390 | all three `<wt>/.cohort/bin/*` | none offered | 1 would-block | changed |
| P / gpt-6-astra | 955 (P = 956 B) | all three `<wt>/.cohort/bin/*` | none offered | 1 would-block | changed |
| P / gpt-5.6-sol | 956 | all three `<wt>/.cohort/bin/*` | none offered | 1 would-block | changed |
| M / gpt-6-astra | 955 (M = 956 B) | all three `<wt>/.cohort/bin/*` | **13 tools listed** | 1 would-block | changed |
| M / gpt-5.6-sol | 956 | all three `<wt>/.cohort/bin/*` | **13 tools listed + one real call** | 1 would-block | changed |
| X0 / gpt-6-astra | 389 (X0 = 390 B) | all three `<wt>/.cohort/bin/*` | none offered | **1 blocked** | **unchanged** |
| X0 / gpt-5.6-sol | 390 | all three `<wt>/.cohort/bin/*` | none offered | **1 blocked** | **unchanged** |
| X1 / gpt-6-astra | 389 | all three `<wt>/.cohort/bin/*` | none offered | **1 blocked** | **unchanged** |
| X1 / gpt-5.6-sol | 390 | all three `<wt>/.cohort/bin/*` | none offered | **1 blocked** | **unchanged** |

(The 389/955 figures are the model's own character count of the same file; the
frozen bytes are 390 / 956.)

**PATH after the login shell.** `command -v` *inside* the session resolves
`sed`, `python3` **and** `clj-surgeon` to the worktree's `.cohort/bin`. This is
the check Astra insisted on and it mattered: Codex executes through
`/bin/bash -lc`, and this seat's `~/.profile` prepends `~/bin` and
`~/.local/bin`. An early probe under the non-isolated home did resolve
`clj-surgeon` to `/home/forge/bin/clj-surgeon` (same pinned implementation, but
it would have bypassed the journal). Under the final configuration the planted
launcher wins in all ten probes.

**X0 prints exactly `not run`.** X1 prints the frozen head
("Not run: this command has no form-preservation receipt…"), then the resolved
target, the owning form found from the file's own bytes, a filled
`:change!` request document, the exact invocation, and the insertion /
require-change entrances — then exits 2, with the fixture byte-identical.

**M's real MCP round-trip.** `mcp__clj_surgeon__inspect_clojure` was called with
the worktree's absolute `workspace_root` and returned a typed refusal
(`missing-fields: expect`) — a genuine server round-trip, not a configured URL.

**Residual, recorded, not fixed:** every session (all arms, identically) is also
offered OpenAI's own built-in `mcp__codex_apps__*` tools and the built-in skills
(`imagegen`, `openai-docs`, `plugin-creator`, `skill-creator`,
`skill-installer`) plus the **repository's own** `study-agent-usage` skill, which
ships in the task repo at every `:sha`. These are constant across arms and
therefore cannot produce an arm contrast, but they are not zero and they are not
something this runner can remove without editing the task repo.

---

## 3. The request-file witnesses Astra required — and one scoped ceiling

Astra: *"The builder must freeze exact successful request-file witnesses for all
advertised task families, including deftest ownership and E4's alias scope,
before scoring… If that entrance is absent, report a design delivery blocker
before spending runs, rather than add an unmeasured adapter mid-cohort."*

| family | entrance | witness | result |
|---|---|---|---|
| **I** — insert beside a named form | `clj-surgeon :insert-forms! :request-file X` | `mission_cli.clj` @ `71dd6eff`, `:anchor {:scope "top-level" :owner {:kind "defn" :name "show"} :position "after"}` | **committed**, `write_verified true`, `other_forms_unchanged true`, 1 form inserted — first attempt |
| **T** — deftest-body replacement | `clj-surgeon :op :change! :spec-file X :receipt-out Y` | `outline_memory_test.clj` @ `a8452157`, owner `thread-allocation-meter-is-available`, string literal replaced | **committed** after ONE argument repair (`:find`/`:do` take complete *forms*, so a string must be written as `"\"…\""`) — exactly the one-repair loop the plate instructs |
| **R (alias rename)** — E4's scope | `clj-surgeon :rename-alias! :request-file X` | `schedule.clj` @ `00e8f0fa`, `events` → `ev`, `:scope {:file … :expect_files 1}`, `:expect {:references {:per_file {… 2}}}` | **committed**; result hash `02332a74…` = the recorded correct bytes; **the task's own oracle returns PASS (12 checks)**, including all 29 route-string decoys |
| **R (add a require)** — no `:refer` in the ns | `clj-surgeon :op :require-change! :request-file X :plan-only true` | `namespace_split.clj` @ `a9da4344` (R-04), add `cheshire.core` | **plan OK**, alias `json` chosen `first-free`, counts `{:files 1 :adds 1 :removes 0}` |
| **R (add a require)** — ns contains `[clojure.test :refer […]]` | same | `core_discovery_test.clj` @ `077f5a64` (R-01) | **REFUSED** `unsupported-require`: *"Only single-line direct [lib] or [lib :as alias] entries are supported"* |

### The ceiling, stated plainly — and the correction the runs forced

**Before the runs** this section said: *three of the thirteen tasks (R-01, R-02,
R-03) have no Surgeon entrance for the step the plate tells the caller to
route.* Their namespaces all contain `[clojure.test :refer [deftest is …]]`, and
`require_change` refuses the whole file on that libspec shape — on **both**
entrances: the CLI refuses it, and the MCP `require_change` tool, called
directly against R-01 with a valid envelope, returns the identical
`unsupported-require`. No `ns` form can be reached as a `:change!` owner either
(`change-owner-mismatch … found 0` for both `ns` and the namespace symbol) and
`:op :edit`'s `(form 'ns)` selector matches 0 of 24 forms.

**The cohort falsified half of that.** In arm M, `gpt-6-astra` added the require
to all three files with **`edit_clojure` / `apply_clojure_changes` using
`within: {"namespace": "<ns>"}`** — a scope the builder had not tried (only
`within: {"form": "ns"}`, which does refuse). R-01 and R-03 were ACCEPTED that
way; R-02 passed the oracle. So the corrected statement is:

* **MCP: the entrance exists.** `edit_clojure` with a namespace-scoped `within`
  replaces the whole `(:require …)` block under a hash guard and reports
  `atomic commit complete / written bytes read back`.
* **CLI: no request-file entrance was found.** `require-change!` refuses the
  libspec shape, and no `:change!` / `:edit` owner reaches an `ns` form. The
  measurement agrees: across all 26 P-arm runs (CLI only, plate on), R-01, R-02
  and R-03 were never routed to Surgeon by either caller.

This asymmetry is itself a result, and it is part of what M − P is measuring for
`gpt-6-astra`: on this task family the MCP arm has a capability the CLI arm does
not. Publishing the pre-run claim and then correcting it from the runs is the
point — the original text is kept above so the correction is visible rather than
tidied away.

Consequences registered before scoring, and how they stand now:

* The pre-run "maximum Surgeon-final for P and M is 10/13" was **wrong for M**
  and **right for P**: M reached 10/13 with the require family included, P
  reached 4/13 and 5/13 without it.
* Astra's instruction was to report rather than adapt. **No adapter was added**;
  the plate was planted verbatim, refusals included.
* A warrant for any refusal is **UNKNOWN by construction** and stays UNKNOWN.

One more entrance-shape fact worth recording: Astra's plate spells the insertion
call `clj-surgeon :insert-forms! :request-file X` (no `:op`). That shorthand
**works** on this build — verified — so the plate points at a real entrance.

---

## 4. What each scored run carries

Planted, hashed into `runs/<id>/manifest.json` and into the worktree's own
`.cohort/manifest.json`:

```
AGENTS.md                  the arm text (L/X0/X1 = 390 B, P/M = 956 B)
.codex/config.toml         common settings; M's copy also carries the MCP table
                           (measured inert — see §1b — kept for fidelity)
.cohort/bin/sed            byte-identical shim, sha256 7e350df7…
.cohort/bin/python3        the same file
.cohort/bin/clj-surgeon    journaling launcher onto the pinned 04648059 CLI
.cohort/shim-policy.json   observe (L,P,M) | bare (X0) | repair (X1)
.cohort/requests/          empty
.cohort/repair-templates.json   X1 only
```

Suppressed, identically in every arm, and recorded per run in
`runs/<id>/suppressed-paths.txt`: the repository's own `AGENTS.md` and
`CLAUDE.md` (both exist at every `:sha`). Restored before the final diff is
taken, so no arm's text can be counted as the caller's edit; the planted files
are removed and excluded from the diff by `.git/info/exclude`.

`COHORT_JOURNAL` is an append-only file **outside** the worktree, one per run.
The observer overhead (both shims on PATH, both journaling) is present in the
controls too, exactly as the design requires.
