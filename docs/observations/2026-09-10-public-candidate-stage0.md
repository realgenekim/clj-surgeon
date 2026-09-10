# Public candidate, stage 0 — the preparation lane

Written 2026-09-10T16:27:32Z. Branch `fable/public-candidate` at `9405a6eb` in
`/home/forge/src/clj-surgeon-pubcand`, created with `~/bin/worktree-add` from
`stable/2026-09-10` and proved at creation:
HEAD = `59d8bc0cad8886a028c24e8ffd52e4e5d82185c1` = `stable/2026-09-10^{commit}`.

**Nothing has been pushed anywhere.** The only remote configured on this repository is
`origin` = `https://github.com/realgenekim/clj-surgeon.git` — the PUBLIC repository. There
is no `fork` remote. Pushing this branch to `origin` would itself be a publication while
main is frozen, so no push is possible today without first adding a private remote, and I
have added none.

**Astra's preferred variant, recorded here so it is not lost.** Astra would build
`public-candidate` FROM frozen public `main` and import reviewed squashes of platform
slices, rather than cutting from the MCP tag. This work does not compete with that: what
it produces is the **file manifest** (what is in, what is out, and why), the **scanner**
that gates any candidate however it is built, the **README claims table**, and strip
commits that define what such a squash would have to contain.

---

## A. The red inventory on the tag

`bin/public-surface-scan` (babashka, ~35 s over 3632 files) was committed FIRST and red.
It reports one line per hit as `path:line: [category/rule] excerpt`, a count per category,
and exits 1 on any hit that `public-surface-allowlist.edn` does not justify with a written
reason. Full inventory retained at `/var/tmp/forge/plan2/cellC/pubcand/red-tag.txt`.

| category | hits | files | what it looks for |
|---|---:|---:|---|
| seat-path | 5118 | 1028 | `/home/forge`, `/home/tester`, `/home/genek`, `/Users/genekim`, `/var/tmp/forge`, `~/tmp/…` |
| seat-box | 3188 | 520 | anvil, buster, skiff, bridge |
| internal-port | 661 | 210 | 7888, 7890, 7894, 7895, 7906, 8171, 8300–8339 |
| seat-tool | 1116 | 246 | sol-yolo, records-push, ship, fence-run, receipt-chain, land-auto, typist-run, suite-run |
| seat-name | 2730 | 408 | forge-anvil, sol-anvil, astra, codex-fenced, mayor, forge-bridge |
| credential | 61 | 16 | `~/secrets`, private-key headers, provider tokens, AWS ids, bearer literals, quoted credential assignments |
| records-lane | 1821 | 1819 | by PATH: `docs/observations/`, ledgers, receipts, `bench/anvil-arms/`, `__pycache__`/`.pyc` |
| **TOTAL** | **14695** | **2255** | of 3632 tracked files — 62% of the tree named this fleet |

The credential figure is **locations, not committed secrets**. It matches Astra's separate
finding: a bounded signature scan over tracked UTF-8 text found no private keys, provider
tokens, AWS ids or credential URLs. This scan is not a secret clearance either — it reads
tracked text only, never blobs, history, commit messages or tag annotations.

## B. The strip, as reviewable commits, with the count falling

| # | sha | category | tracked files | scan total |
|---|---|---|---:|---:|
| 0 | `59d8bc0c` | the tag, untouched | 3632 | 14695 |
| 1 | `c4b6994a` | the scanner + allowlist, **red first** | 3632 | 14695 |
| 2 | `3e83d3a2` | remove `docs/observations/` (1794 files) + `docs/README.md` pointer | 1841 | — |
| 3 | `c948c214` | remove bench seat apparatus (814 files), Makefile reconciled | 1027 | 1752 |
| 4 | `17109146` | bin/ prune — **later reverted, see below** | 995 | — |
| 5 | `6f74db0e` | seat paths → derived per-user defaults | 995 | seat-path 319 → 28 |
| 6 | `39699689` | ports → documented overridable defaults | 995 | internal-port 56 → 6 |
| 7 | `9405a6eb` | remove the routing plate, seat skills, seat docs | 983 | 1198 |
| 8 | `e6b9c7e9` | repair three regressions the gate caught | 981 | — |
| 9 | `f2469376` | **revert** the bin/ prune: the typist runner is a product dependency | 1013 | — |
| 10 | `d7e9b32e` | remove the agent-routing feature **whole** | 1010 | — |
| 11 | `57740672` | the allowlist, a written reason per entry | 1010 | **447** outside the allowlist |

Eleven commits, each reviewable on its own. Two of them are reversals the gate forced,
and they are in the history on purpose: a candidate whose strip commits hide their own
retractions teaches the next person nothing.

### 2 — the records lane
1794 files removed. Three machine-read artefacts stay because the suite consumes them, not
a human: `battery-ledger.edn`, `battery-namespace-walls.edn`, `gate-namespace-walls.edn`.
Before removing anything I verified that **no code reads an observation file**: every
`docs/observations/…` reference in `src/`, `test/` and `dev/` is a comment or a string
compared as data. `docs/README.md` now says the records lane is private and tells a reader
how to read a surviving citation — as a provenance label, never as a file they can open.

### 3 — the bench apparatus
`bench/anvil-arms/` (22 files, including the tracked `__pycache__/*.pyc` the current
hygiene gate cannot see), `bench/results/` (784 receipt records), `bench/raw-cohort-v2/`,
five box-named runners and a wave-queue script. Six Makefile targets, their `.PHONY`
entries, their `make help` lines and one inter-target call were removed with them — the
exclusion is reconciled with the build, not hidden from the README. `bench/fixtures/` and
`bench/memory_battery/` stay: five test namespaces slurp them.

### 4 — bin/, and the rule that looked right and was not

Commit `17109146` applied a mechanical rule — a script stays if `make install` or
`make test` invokes it — and dropped the fast-typist provider runner and its dossiers.
**The gate disproved it and `f2469376` reverts it.**
`src/clj_surgeon/mission_typist_executor.clj` reaches `bin/typist-run` at RUNTIME,
through a subprocess whose path no Makefile and no test source spells out. Measured:

| tree | `mission-typist-executor-test` |
|---|---|
| at the tag `59d8bc0c` | 11 tests, 73 assertions, 0 fail, 0 error |
| `bin/` removed | 38 fail, 2 error |
| runner removed, dossiers kept | 38 fail, 2 error |
| `bin/` restored | 11 tests, 73 assertions, 0 fail, 0 error |

**The rule was right; my method for answering it was wrong.** "Does the build call it"
cannot be answered by grepping for the path — only by running the gate. And the finding
matters beyond bin/: what the release review classified as a private provider
**experiment** is wired into a product code path. Either it is product and must be
prepared for publication, or the executor's dependency on it is the thing to remove.

**I did not touch its credential handling.** `bin/typist-run` carries an explicit
fence — *"FIXED paths. Never environment-selected: a caller-controlled path is a
caller-controlled key"* — over key files under a seat home. Those paths must change
before publication, but making them environment-derived would **widen** that fence, and
`bin/typist-run-test` witnesses it. The right public form keeps "fixed, not
caller-selected" while computing the location per user, with the fence's witnesses
updated in the same change and a security review before it merges. That is scoped work,
not something to improvise at the end of a timebox.

### 5, 6 — seat paths and ports
Covered above; unchanged by the later repairs.

### 7 — the routing feature comes out WHOLE

`9405a6eb` removed the private plate and its 14 witnesses but kept
`agent_routing.clj`, on the theory that the mechanism is product and only the doctrine
it carries is private. **The gate disproved that twice, precisely.**

First, `make test` refused on exactly one lane — the intent-contract audit — with
exactly four new violations: `MCP-OP-RELAY-004`, `ROUTING-FANOUT-001`,
`ROUTING-SPLIT-001`, `ROUTING-PARITY-001`, each left without a test witness.

Then I tried the smaller repair: keep the ten witnesses that drive synthetic blocks
(marker handling, byte preservation outside the markers, preflight-before-write,
install/check end to end, refusal shapes) and drop only the eight that quote the plate's
sentences. **That fails at load:**

```
Execution error (ExceptionInfo) at clj-surgeon.agent-routing/registry-sections
Each declared intent requires nonempty plate passages
```

The module's own registry is keyed on the doctrine intents and demands passages from the
shipped plate. Read the intents and it is obvious why — they are assertions *about the
plate's content*: "the plate shall require the September 8 fan-out suspension", "the
plate shall require namespace_split admission only for the exact frozen Cell C shape",
"agent routing shall state that `next_action=none` never proves the request is
finished". **Those are one operator's doctrine written as requirements. They are not
this tool's behaviour, and no public build can satisfy them.**

So `d7e9b32e` removes the feature whole: the module, the plate,
`docs/intent/agent-routing/`, both Makefile targets with their `.PHONY`, help and
`install-dev` references, `AGENT_ROUTING_SOURCE`, the `@spec` marker in bench/,
`MCP-OP-RELAY-004`, and the run_all entry. `make intent-audit` is `:ok true` again —
zero new violations against the recorded pending set, the same state as the tag, which I
checked as a control before assuming the breakage was mine.

**The cost, plainly: a public clj-surgeon has no agent-routing installer.** A public one
needs a plate whose requirements are about the TOOL rather than about one fleet's routing
policy, plus its own witnesses. Related and now fixed regardless: `make install` no
longer modifies the user's global Claude/Codex instruction files, which was Astra's
release blocker.

---

## C. Proof

### The landing gate: GREEN

`make test` on `57740672`, all seven stages exit 0, total **355.3 s**:

| stage | exit | wall |
|---|---:|---:|
| admit-transaction-recovery-battery | 0 | 14.4 s |
| battery-fresh | 0 | 11.4 s |
| alias-migration-test | 0 | 169.5 s |
| mcp-test | 0 | 226.4 s |
| test-bb | 0 | 167.8 s |
| repository-hygiene | 0 | 1.8 s |
| intent-audit | 0 | 1.9 s |

Zero `FAIL in` / `ERROR in` / `gate-refused` in the log
(`/var/tmp/forge/plan2/cellC/pubcand/gate4.log`). It took **four runs** to get there,
and the three refusals are the most valuable thing in this report:

**Run 1 — three regressions, all mine.**
1. *Backticks in a Makefile recipe are command substitution.* The line I added to
   `install` contained `\`make install\`` as prose about a command, so `make install`
   invoked itself forever; `install-test` drives the real installer, so the 30-minute
   gate I sat through **was that recursion**, not a slow box.
2. *Eleven witnesses asserted the SPELLING of the artifact root, not its contract.* When
   `*artifact-root*` became derived, every
   `(str/starts-with? path "/var/tmp/…/<verb>-receipts/")` went red — and my earlier text
   pass had "fixed" them by rewriting the literal, quietly moving them from asserting one
   wrong machine's path to asserting another. The invariant was never the root: it is
   that the artifact is published **outside the workspace**, under an absolute per-user
   root, in a verb-named directory. All eleven now assert that, and `mcp_workspace_test`
   additionally asserts the receipt directory is *not inside* the workspace — the thing
   that actually mattered, which the literal never checked.
3. *`CLAUDE.md` is a TESTED product surface.* `xray_test` slurps README, both skill
   packages, `docs/vision.md`, CHANGELOG **and CLAUDE.md** and requires each to teach the
   computed read boundary. **A file a test reads is an interface.** Section restored.

**Run 2 — bin/.** See section 4. **Run 3 — the routing feature.** See section 7.

### `make install`
`make -n install` no longer recurses, and `clj-surgeon.install-test` (40 s of real
installer drives) is inside the green gate. A standalone timed `make install` was not run
separately.

### anvil2 — NOT RUN
**The host does not resolve from this seat**: `ssh anvil2` →
`Temporary failure in name resolution`, and there is no entry in `~/.ssh/config`,
`~/.ssh/known_hosts` or `/etc/hosts`. I did not invent a number for it. The closest
substitute I could run here is a clean clone with a clean HOME — that is a clean-*prefix*
test, not a clean-*user* test on a second box, and Astra's correction of the earlier
skiff report's wording applies to mine too.

### `make check-agent-routing`
The target no longer exists: the feature was removed whole (section 7). Before that
removal I had made it refuse cleanly on a missing plate
(`{:ok true … :status :no-routing-plate …}`, exit 0) — that behaviour went with the
module, and is the shape a future public plate should keep.

### The battery ledger on a fresh clone — measured, not reasoned

Coordinator's question, answered by running it. A clean `git clone` of the candidate into
`/var/tmp/forge/pubcand-cleanuser/repo`, three states:

| state of `docs/observations/battery-ledger.edn` | `make battery-fresh` |
|---|---|
| as shipped (inherited receipt, sha `6867f773`) | **OK** — 11.6 h old, 9 commits behind HEAD, exit 0 |
| present but empty | **REFUSED (no-entries)**, exit 1 |
| file absent entirely | **REFUSED (no-entries)**, exit 1 |

It refuses **by name**, which is the right behaviour:

```
battery-fresh: REFUSED (no-entries) -- no battery receipt in
docs/observations/battery-ledger.edn -- the battery lane has never recorded a run here,
so nothing on this tree distinguishes `it passed` from `it was never run`
REMEDY: run the battery and commit its receipt --
  make test-battery
  git add docs/observations/battery-ledger.edn && git commit
```

(That remedy previously read `flock /home/forge/tmp/suite.lock make test-battery`. The
Makefile lock has been derived for some time; the sentence had not caught up.)

`make print-gate-stages` confirms **`battery-fresh` is a gate stage**, so this refusal
takes `make test` down. Three consequences a public candidate has to face:

1. **A candidate ships either a receipt or a red gate.** Today's inherited receipt passes
   only because it is 11.6 h old and 9 commits behind. At 26 h it goes `:stale`; past 30
   commits `:too-far-behind`.
2. **A candidate built Astra's way — from public `main` with imported squashes — inherits
   a receipt whose sha is not an ancestor of its HEAD**, and refuses `:not-an-ancestor`.
   That is the tripwire working correctly: the receipt genuinely says nothing about that
   tree.
3. **The shipped ledger is itself a publication item.** Every line carries
   `:host "anvil-server"`.

**Proposed public receipt bootstrap (a proposal, not a policy change, not implemented).**
One repository-owned target — say `make public-receipt-bootstrap` — that (a) refuses
unless the working tree is clean and HEAD is the exact candidate commit, (b) runs the full
battery, (c) appends one receipt naming that commit, (d) prints the `git add`/`git commit`
line. The documented public sequence becomes
`make install && make public-receipt-bootstrap && make test`, and the candidate ships an
**empty** ledger rather than this fleet's. That removes the last seat receipt from the tree,
keeps the tripwire's meaning intact — the receipt is about *your* tree, produced on *your*
machine — and turns a confusing red into a documented first step. It changes no threshold
and no refusal reason; only who produces the first entry.

### The README claims table

Every claim checked against this candidate tree.

| # | README claim | line | status | evidence |
|---|---|---|---|---|
| 1 | "Two binaries must be on `PATH`" (bb, clj-kondo) | 526 | **UNBACKED** | `bin/install-preflight` marks bb, java, clojure, git, python3 `required`; clj-kondo is a separate analyzer requirement; swipl is required to test |
| 2 | "`make install` will warn (but not fail) if either binary is missing" | 541 | **UNBACKED** | preflight accumulates `missing_required` and refuses |
| 3 | install does not disclose that it edits global agent instructions | 520–545 | **FIXED HERE** | `make install` no longer touches them at all; the routing installer is gone |
| 4 | "search the deferred MCP catalog for `mcp__cclsp__*`" | 192 | **CONTRADICTED** | direct cclsp/clojure-lsp MCP clients are retired in current doctrine; 27 cclsp mentions remain |
| 5 | extraction headline + speed table at commit `573e240` | 10–70 | **BACKED-BUT-DATED** | a dated, bounded, matched experiment, presented as a headline rather than a bounded result |
| 6 | the September 8 informed fan-out suspension | — | **ABSENT** | the 0.68×/0.59× suspension appears nowhere; a reader is left with the favourable figures only |
| 7 | MCP called "an explicit development experiment" | 545, 1298, 2461 | **BACKED** | preserve the qualification until Gene chooses a public support contract |
| 8 | "box-wide flock slot under `…/gate-slots/`" | 2340 | **UNBACKED** | `test/gate_slot.py` uses an ABSTRACT unix socket on Linux, a pathname socket on Darwin. The mechanism changed; the sentence did not |
| 9 | alias ledger path | 785 | **UNBACKED — introduced by this strip** | the sentence names `/var/tmp/…`; the code writes under `~/.local/state/clj-surgeon/artifacts/` |
| 10 | 18 markdown links into `docs/observations/…` | 89–2321 | **BROKEN — introduced by this strip** | each points at a file this candidate does not ship |
| 11 | an installed split route / enabled-tools list | — | **UNBACKED** | `workspace_onboarding.clj` emits a tools list without `namespace_split` or `require_change`, and `make install` establishes no live MCP connection |

**Items 9 and 10 are damage this strip caused and did not repair.** A strip that removes a
document owes a repair to every claim that cited it. They are the top of the next batch.

---

## What remains for Gene

**1. The scan is red on the authored tree, and I did not make it green by allowlisting
prose.** 14,695 hits in 2,255 files became **447 in 112**, with 860 hits in 28 files
carried by six allowlist entries — two of them marked OPEN DECISION, not justification.
The residual:

| where | hits | what it is |
|---|---:|---|
| `docs/` | 200 | narrative in plans and design docs: "measured on anvil", reviewer and integrator names |
| `bin/` | 144 | the restored typist runner: seat home key paths, scratch roots, its own name |
| `test/`, `bench/` | 93 | comments and test data |
| `.beads/`, README, CHANGELOG, dev | 10 | see below |

By rule: 149 `anvil`, 51 `astra`, 51 seat-`bridge`, 47 `typist-run`, 33 `/home/forge`,
29 `/var/tmp/forge`, 27 `skiff`, 27 `~/secrets`, 9 `suite-run`, 8 `mayor`, 6 gate-slot
ports, 3 `sol-yolo`. **One more careful batch — but not the way I first tried it.**

**2. A blanket seat-name substitution corrupts identifiers. Measured, not feared.**
I ran one in a throwaway clone first. It produced `(ns an independent reviewer-typist-real1`,
`bash bench/run_the reference host_portfolio_pair.sh`,
`test_remote_receipt_cannot_claim_that host_authority()`, and rewritten filenames inside
`docs/observations/…` citations. **The worktree never saw it.** Seat names live in
namespaces, filenames, env-var prefixes, branch names and git identities as well as prose,
and no word-boundary rule separates them. Enumerate the identifier occurrences first; edit
the prose per file.

**3. `test-fixtures/field-diffs/` is a snapshot of a DIFFERENT private application.**
56 files of voice-remote source and tests. `admit_patch_test.clj` consumes it as its
real-world diff corpus, so deleting it removes a real gate; keeping it publishes another of
Gene's codebases; sanitising it destroys what makes it evidence. **A decision, not a
strip.** Same question, smaller, for `test-fixtures/require-change/*bridge3*`.

**4. The typist runner is inside a product code path.** Section 4. Decide whether it is
product (then its credential fence needs the public redesign described there, with a
security review) or whether the mission executor's dependency on it is what should go.

**5. `.beads/interactions.jsonl`** is the issue tracker's export and carries seat paths and
actor names. I deliberately reverted my scrub of it — it is a passive export of a Dolt DB
and hand-editing it is the wrong repair. Decide whether `.beads/` ships at all.

**6. Agent routing.** Removed whole, with proof that plate and mechanism are inseparable
as built (section 7). A public build has no routing installer until someone writes a plate
whose requirements are about the tool.

**7. History vs squash.** Untouched, deliberately: this candidate is a **tree**. A
fast-forward would import 2,562 commits whose messages, author metadata and tag
annotations **this scan never read** — it reads tracked text only. If Gene wants history,
extend the scan to commit messages and annotations before that decision is safe. Astra's
variant (build from public `main`, import reviewed squashes) sidesteps this, and these
strip commits are exactly the manifest such squashes must satisfy.

**8. Intent specs.** `docs/intent/` stays: `@spec` ids bind code and witnesses to it and
`make intent-audit` gates the binding, so it is product. Its design documents carry cohort
narrative naming seats and boxes — part of the 200. `docs/plans/` (90 files) is mixed and
**cannot simply be dropped**: `src/` and `test/` cite several of its files as "contract of
record".

**9. The two claim classes this strip broke** — README items 9 and 10 above.

**10. The public receipt bootstrap**, proposed above.

---

## The three lessons, if only three survive

1. **A scan that cries wolf gets switched off.** Two of my own rules were wrong in the same
   way — the port patterns matched digits inside SHA-256 digests, and `bridge` matched an
   ordinary English noun. Both were fixed by narrowing, and both would have trained the
   first reader to ignore the tool.
2. **A witness pinned to a constant is not a witness.** Eleven assertions "passed" for
   years by naming one machine's path; when the path became derived they went red without
   any behaviour changing, and my first repair made them pin a *different* wrong constant.
   Assert the contract.
3. **Only the gate knows what the build calls.** Grep answered "does anything use this?"
   confidently and wrongly twice — the typist runner (a runtime subprocess) and the routing
   plate (a load-time registry). Both times the gate's refusal was the finding.

---

## Files and receipts

| what | where |
|---|---|
| candidate worktree | `/home/forge/src/clj-surgeon-pubcand`, branch `fable/public-candidate`, tip `57740672` |
| the scanner | `bin/public-surface-scan` (bb), `public-surface-allowlist.edn` |
| red inventory on the tag | `/var/tmp/forge/plan2/cellC/pubcand/red-tag.txt` |
| per-stage hit dumps | `…/red-after-c4.txt`, `…/red-after-c6.txt`, `…/red-after-c7.txt`, `…/final-hits.txt` |
| final residual (allowlist applied) | `…/residual-final.txt` |
| gate logs: 3 refusals then green | `…/gate1.log`, `…/gate2.log`, `…/gate3.log`, `…/gate4.log` |
| clean-clone fixture | `/var/tmp/forge/pubcand-cleanuser/repo` |
| unmodified-tag control | `/var/tmp/forge/pubcand-tagbase` |
| interim at 90 minutes | `/var/tmp/forge/plan2/cellC/opus-public-candidate-interim.md` |
