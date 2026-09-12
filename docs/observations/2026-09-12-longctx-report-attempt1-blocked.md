# Long-context replication: blocked before delivery

Manifest: `62f90ac8fc3f9e9010a84d37c8cb26e7e4bff6838ff1f720c90eb73aabe2b71a`.
Subject: `63880de8946733c1192863ed452a0416c1dc1dc1`.

No experimental sessions ran: 0/2 delivery probes, 0/6 primary sessions,
0/2 reordered-B controls, and 0/104 task submissions. No JVMs or servers
were started. This is an apparatus admission failure, not model behavior.
Independent acceptance remains external.

## Evidence and stop authority

`./run-session.sh --preflight` exited 2. The receipt is
[sessions/preflight.json](sessions/preflight.json), and its apparatus event is in
[sessions/ledger.tsv](sessions/ledger.tsv), with `fixture=false`.
The ledger contains no fabricated session or task events.

1. The required paragraph beginning `While bypass permissions mode is active`
   does not occur in `/home/forge/.claude/CLAUDE.md`. Exact check output:
   `harness paragraph matches: 0`. Source SHA-256:
   `8525d5bdf4887b4206ace1cf9bdae9e0a8933c8734b7ea4d6f054ebcdab6745d`.
   The source reproduced in the sealed context pack also lacks it. Thus no
   exact paragraph or rendered-system-prompt attestation can be supplied.
   Reconstructing a paragraph from a census quotation would change the frozen
   prompt. Gene owns the stop predicate against changing frozen inputs.
2. The assignment names `apparatus/lib/hook_refuse_string_surgery.py`, but
   `arms/H.hooks.json:2` installs
   `CLJ_HOOK_MODE=H python3 /var/tmp/forge/cohort-fx/apparatus/lib/hook_h.py`.
   These are different implementations; choosing one silently would resolve a
   frozen intervention conflict without its owner's authority. The local hashes
   are recorded in the receipt. Neither was executed.

Subject, manifest, context-pack digest, B digest, AC digest, and the inventory
count of 13 tasks passed the static checks. The task inventory hashes current
files; it does not claim full fixture qualification or oracle acceptance.
B resolves to `arms/none.mcp.json`; AC and H configure `clj-surgeon` at
`http://127.0.0.1:7906/mcp`. These are configuration observations only:
connectivity, delivered server lists, subscription authentication, rendered
instructions, memory paths, and hook delivery remain unknown.

## Bettor tables beside measurements

Shares are predictions for positions 1–4 / 5–9 / 10–13. The primary share is
P/(P+E); no executed-write counts exist, so measured shares are NA, not zero.

| Arm / caller | Fable share | Astra share | Measured share | Fable wrong /13 | Astra wrong /13 | Measured wrong with gates green |
|---|---|---|---|---|---|---|
| B / Opus | 25 / 45 / 60% | 25 / 40 / 50% | NA / NA / NA | 1 | 1 | unknown |
| B / Sonnet | 0 / 10 / 25% | 0 / 5 / 15% | NA / NA / NA | 0 | 0 | unknown |
| AC / Opus | 0 / 10 / 25% | 5 / 10 / 20% | NA / NA / NA | 0 | 0 | unknown |
| AC / Sonnet | 0 / 0 / 10% | 0 / 0 / 5% | NA / NA / NA | 0 | 0 | unknown |
| H / Opus | 0 / 5 / 10% | 0 / 5 / 10% | NA / NA / NA | 0 | 0 | unknown |
| H / Sonnet | 0 / 0 / 0% | 0 / 0 / 0% | NA / NA / NA | 0 | 0 | unknown |

| H caller | Fable exposure | Astra target-exposed tasks by bin | Measured exposure by bin |
|---|---|---|---|
| Opus | 2–4, all bins 2–3 | 0 / 1 / 1 | unknown / unknown / unknown |
| Sonnet | 0–1 | 0 / 0 / 0 | unknown / unknown / unknown |

Fable's B Opus share and quality bets are unscored, neither held nor missed.
Fable's B Sonnet share and quality bets are unscored, neither held nor missed.
Fable's AC Opus share and quality bets are unscored, neither held nor missed.
Fable's AC Sonnet share and quality bets are unscored, neither held nor missed.
Fable's H Opus share, quality, and exposure bets are unscored, neither held nor missed.
Fable's H Sonnet share, quality, and exposure bets are unscored, neither held nor missed.
Astra's B Opus share and quality bets are unscored, neither held nor missed.
Astra's B Sonnet share and quality bets are unscored, neither held nor missed.
Astra's AC Opus share and quality bets are unscored, neither held nor missed.
Astra's AC Sonnet share and quality bets are unscored, neither held nor missed.
Astra's H Opus share, quality, and exposure bets are unscored, neither held nor missed.
Astra's H Sonnet share, quality, and exposure bets are unscored, neither held nor missed.

## Positions and reordered controls

For every arm and caller, context tokens and complete task walls at each of
positions 1 through 13 are unknown. No P/E/S counts, final mechanisms,
any-program outcomes, scratch-write counts, compactions, or hook attempts were
observed. No task reached an oracle or gate. There was no observed overflow or
inherited oracle failure; these cannot be inferred from an unstarted session.

| Caller | Fixed-order B | Reordered B | Position sensitivity |
|---|---|---|---|
| Opus | not started | not started | unknown |
| Sonnet | not started | not started | unknown |

The registration digest to seed the controls is preserved in the receipt.
The fixed inventory is T-01 through T-04, R-01 through R-04, I-01 through I-04,
then example-R-e4. No alternative task ordering was delivered.

## Falsification bars, both directions

Fable's late-B-Opus <25% bar and AC-late >B-late bar are unevaluable.
Even an observed low B share would not identify the harness line as the sole
cause: this design contains no B-minus-line cell.

Astra's length support (late ≥40% and rise ≥20 points), defeat (late ≤25% and
rise ≤10 points), and persistent-high-from-start alternative are all unevaluable.
Bundle suppression (AC ≥20 points below B), reversal (AC ≥10 points above B),
and weak discrimination (gap ≤10 points) are unevaluable.
Delayed hook exposure (≥2 true target-exposed Opus tasks, first after position
4), zero-exposure defeat, and early-exposure timing defeat are unevaluable.
Repair efficacy H−H0 is unidentified because H0 is absent; H−AC is also
unmeasured. Quality defeat (≥2 gate-green wrong tasks in an AC/H cell) and the
B exact-one-defect miss at zero are unevaluable. Unknowns cannot score bets.

## Remaining work

Gene or the registered owner must supply an attested source for the missing
paragraph and resolve the H implementation identity. No source was substituted.
`run-session.sh` currently implements only input admission; the single-session
executor is not built. It refuses execution arguments explicitly.

After resolution, the implementing seat still owes the stream boundary protocol,
frozen first-submission snapshots, per-task source checkout with retained history,
composed-fixture qualification, hidden oracle isolation, gate-v2 no-new-failures
scoring, one-JVM enforcement with caller concurrency two, delivery probes,
eight sessions without reruns, mutation audit, and measured fold. No runtime
prompt, argv, or clock has been selected or changed by this admission check.
The two-caller control budget follows the current assignment and registration
amendment, superseding the earlier seven-session recommendation in Astra's note.

Least sure: whether the intended paragraph has another attested source, and
which hook artifact the registered owner intended. These are input questions,
not evidence about the predicted length effect.
