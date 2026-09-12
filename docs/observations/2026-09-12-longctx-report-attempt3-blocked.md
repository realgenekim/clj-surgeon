# Long-context attempt 3: blocked before scored sessions

Manifest: `74d00bce1fc4929c5af4fd0bbf41c600b163819b1c0974effa1b89f5b1793206`. Subject: `db10be6d27decdad05dbb562666be2aa621013a9`.

The two remaining delivery probes completed in this packet's foreground supervisor,
with concurrency two and exit 0 for both callers. **Four total probes are spent;
zero of six primary sessions, zero of two controls, and zero of 104 task submissions
ran.** No behavior bet is scored. Independent acceptance remains external.

Amendment 2 is implemented: B uses bypass mode with the attested `arms/B.md`
unchanged as CLAUDE.md; AC/H use acceptEdits with a PreToolUse permission allowance
for Bash, Edit and Write. H retains the exact registered refusal command before
the added allowance. Both real fixture refusals survived that allowance.
Prior receipts remain untouched under `sessions/`; current receipts are under
`sessions/attempt3/`, and both attempts are distinguished by paths in the append-only
[packet ledger](sessions/ledger.tsv). Its rows all carry fixture=false; the two
probe fixtures are nevertheless excluded from scored exposure.

## Delivery evidence

| Caller | Mode | Paragraph count attested | H fixture refusals | Fixture unchanged | Memory files | Exit |
|---|---|---:|---:|---|---:|---:|
| claude-sonnet-5 | acceptEdits | 0 | 1 | True | 0 | 0 |
| claude-opus-5 | acceptEdits | 0 | 1 | True | 0 | 0 |

Both init events report clj-surgeon connected, acceptEdits, and apiKeySource=none.
The subscription check reports claude.ai / max; Claude Code is 2.1.267.
Both callers say `PARAGRAPH_COUNT=0`. These are caller attestations, not independent
captures of rendered system bytes. H was exercised directly; AC shares its text,
MCP configuration and allowance but was not separately probed without the refusal hook.
Both memory directories exist and have empty listings, satisfying amendment 2.
The SHA-256 of each canonical JSON listing (`[]`) is
`4f53cda18c2baa0c0354bb5f9a3ecbe5ed12ab4d8e11ba873c2f11161202b945`.
Sonnet additionally read settings and ran a memory-directory listing despite the
probe's no-other-commands instruction; those read-only actions are retained in
its transcript. Opus performed only the requested Bash attempt.

Receipts: [Sonnet](sessions/attempt3/probe-claude-sonnet-5/result.json),
[Opus](sessions/attempt3/probe-claude-opus-5/result.json),
[admission and memory listing hashes](sessions/attempt3/delivery-admission.json),
[auth/version](sessions/attempt3/client-attestation.json).

**Corrected B is unadmitted.** Neither remaining probe ran corrected B. The previous
Opus bypass probe reported two copies with the explicit CLAUDE.md paragraph still
present. Removing that copy suggests one, but does not supply the required
exactly-one rendered-instruction attestation. B's empty MCP configuration is
statically verified only. Fable owns the four-probe cap; Gene owns frozen changes.
No extra probe or scored session was used to bypass this admission gap.

## Composed fixture qualification

The current runner starts at the first task's repository SHA and replaces only
each next target from its own SHA, preserving earlier results in Git history.
E4 is from curtaincall-cfp, while both sequence bases are clj-surgeon. Neither
base contains E4's required `bin/kaocha` entrance, and no task target adds it.
This is apparatus failure before any caller edit, not inherited model breakage.
The read-only Git-object check started no JVM and ran no gate.

- fixed_order, E4 position 13: `fatal: path 'bin/kaocha' does not exist in 'a8452157be7073a8f6b165a17dce6b316a14a9c2'`
- control_order, E4 position 7: `fatal: path 'bin/kaocha' does not exist in 'a9da43441234f0f815ab9c9393ae03dda7fa82f4'`

[Exact receipts](sessions/attempt3/composed-gate-preflight.json).
The prior 13 correct/13 wrong oracle qualification receipts remain historical
evidence; they do not qualify composed gates. No fixture, task, oracle, or gate
baseline was changed to conceal this problem. A composed-fixture contract is
owed to Gene/the registered fixture owner. Other scored-runner implementation
obligations are explicit in [runner admission](sessions/attempt3/runner-admission.json)
and [report.edn](report.edn); this runner is not represented as qualified.

## Frozen intervention bytes

B CLAUDE.md SHA-256: `2f5435d3d5f712b41a9a12b6571bfe74182dbafd3843a2c34ca448d7caac88b9`.
AC/H CLAUDE.md SHA-256: `1c3236c45dfab7df8f3a0a7a1669a0464cf0debe9cff6046eb52daeafc7f5137`.
The following attested paragraph is expected from the bypass harness, and is
**not copied into B CLAUDE.md**:

```
While bypass permissions mode is active:

Do your work through the Bash tool wherever it can accomplish the job: read files with cat, head, or sed -n, search with grep and find, and make file changes with sed, heredocs, or short scripts, rather than using the dedicated Read, Edit, or Write tools. Fall back to a dedicated tool only when Bash genuinely cannot do the job.
```

Paragraph SHA-256: `c22f56ffa34a2efdd714bdb882bd178363e0b9ae07527b67e7c5adb3c8df423e`.
H command: `CLJ_HOOK_MODE=H python3 /var/tmp/forge/cohort-fx/apparatus/lib/hook_h.py`.
H script SHA-256: `c1076e585bf22af7b83d9ba9d8ecbdb3c613e002dbe5389998b70de55bb61850`.
[Input checks](sessions/attempt3/preflight.json) record these identities.

## Registered bets beside measurements

Shares pool executed target calls P/(P+E), with bins 1–4 / 5–9 / 10–13.
No scored calls exist: P/E/S, P/(P+E+S), any-program task use, submission mechanism,
scratch/request preparation counts and ambiguous-write audits are NA, not zero.

| Arm / caller | Fable share % | Astra share % | Measured share % | Fable wrong /13 | Astra wrong /13 | Measured wrong, gates green |
|---|---|---|---|---:|---:|---|
| B / Opus | 25 / 45 / 60 | 25 / 40 / 50 | NA / NA / NA | 1 | 1 | unknown |
| B / Sonnet | 0 / 10 / 25 | 0 / 5 / 15 | NA / NA / NA | 0 | 0 | unknown |
| AC / Opus | 0 / 10 / 25 | 5 / 10 / 20 | NA / NA / NA | 0 | 0 | unknown |
| AC / Sonnet | 0 / 0 / 10 | 0 / 0 / 5 | NA / NA / NA | 0 | 0 | unknown |
| H / Opus | 0 / 5 / 10 | 0 / 5 / 10 | NA / NA / NA | 0 | 0 | unknown |
| H / Sonnet | 0 / 0 / 0 | 0 / 0 / 0 | NA / NA / NA | 0 | 0 | unknown |

| H caller | Fable target exposure | Astra target exposure by bin | Measured by bin |
|---|---|---|---|
| Opus | 2–4, bins 2–3 | 0 / 1 / 1 | unknown / unknown / unknown |
| Sonnet | 0–1 | 0 / 0 / 0 | unknown / unknown / unknown |

Fable's B Opus share and quality bets are unscored: neither held nor missed.

Fable's B Sonnet share and quality bets are unscored: neither held nor missed.

Fable's AC Opus share and quality bets are unscored: neither held nor missed.

Fable's AC Sonnet share and quality bets are unscored: neither held nor missed.

Fable's H Opus share and quality bets and exposure bet are unscored: neither held nor missed.

Fable's H Sonnet share and quality bets and exposure bet are unscored: neither held nor missed.

Astra's B Opus share and quality bets are unscored: neither held nor missed.

Astra's B Sonnet share and quality bets are unscored: neither held nor missed.

Astra's AC Opus share and quality bets are unscored: neither held nor missed.

Astra's AC Sonnet share and quality bets are unscored: neither held nor missed.

Astra's H Opus share and quality bets and exposure bet are unscored: neither held nor missed.

Astra's H Sonnet share and quality bets and exposure bet are unscored: neither held nor missed.

## Position, tokens, walls and reordered B

All 13 per-task context measurements and complete walls in each of the eight
planned sessions are unknown. Probe tokens are not task context. No compaction,
overflow or oracle carry-over was observed in a scored session because none
started. Unstarted tasks are not invented noncompletion events. Overflow must
be recorded as noncompletion from its position, and unreachable carry-over
oracles as unknown, when scored execution becomes admitted.

| Caller | B fixed | B reordered | Order sensitivity |
|---|---|---|---|
| Opus | unstarted | unstarted | unknown |
| Sonnet | unstarted | unstarted | unknown |

Fixed order: T-01..T-04, R-01..R-04, I-01..I-04, example-R-e4.
Control seed (registration SHA-256): `ba7ed8454b13ee478dd49a4cbf399a02d2c9e81a1d74c18ea2afff29b862a37e`.

Control order: R-04-namespace-split-require-cheshire, R-02-recovery-require-measured, I-01-mission-cli-stale, I-04-mission-display-decision-view, R-03-workspace-string-alias, R-01-core-discovery-require-edn, example-R-e4, I-03-heap-sample-retention, T-02-study-row-line-range, T-03-alias-migration-kind-count, T-04-lane-manifest-total, T-01-outline-memory-spec-marker, I-02-mission-cli-admitted-profiles.

[Execution freeze](sessions/attempt3/execution-freeze.json) holds the task hashes,
orders, prompt, per-arm argv, and monotonic clock definition. No scored delivery
used this order. Amendment 2 changes the registration digest, so this attempt's
seed differs from attempt 2's undelivered control order.

## Falsification bars, both directions

Fable's B Opus late <25% and AC late >B late bars are unevaluable. Low B late share
would not establish that the paragraph alone explains the census; B-minus-line
is absent and no extra cell is authorized here.

Astra's length support (late ≥40%, rise ≥20 points), defeat (late ≤25%, rise ≤10
points), and persistent-high-from-start alternative are unevaluable. Bundle
suppression (AC ≥20 points below B), reversal (AC ≥10 points above B), and weak
discrimination (gap ≤10 points) are unevaluable. The contrast changes a bundle.

Delayed target exposure (≥2 H Opus tasks, first after position 4), zero exposure
as a missed forecast, and early exposure contradicting timing are unevaluable.
Fixture refusals cannot score these bars. H−AC is unmeasured; H−H0 repair efficacy
is unidentified without H0. The hypothetical H0 bar (≥2 extra accepted Surgeon
completions, losing ≤1 accepted completion) cannot be evaluated here.

Quality defeat (≥2 gate-green wrong tasks in an AC/H cell) and an exact-one B
point bet missed at zero are unevaluable. Unknowns cannot score a bet.

## Verification and closure

[Five zero-model transport/amendment tests](sessions/attempt3/transport-check-final.log)
passed, including one process across two task turns, timeout cleanup, preserved
H hook plus allowance, and rejection of stale admission receipts. Input hashes
are checked against the frozen snapshot. The two probe caller processes exited
before their foreground supervisor returned. [Closure](sessions/attempt3/closure.json)
records process inspection and unchanged inputs. No scored directory exists.
No production ledger was written and no server was started. The runner remains
blocked; these checks do not qualify the outstanding scored path.
