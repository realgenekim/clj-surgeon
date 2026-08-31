# In-flow receipt steering A/B preregistration

Frozen before any model call. Date: 2026-08-31.

Gene's question, verbatim: "whether these steer you towards 100ms path vs 1m path."

## Question and causal contrast

Does a data-only price signal on the successful receipt for one ordinary
`edit_clojure` call change the same caller's path choice on its next edit?
The only arm-visible difference is one structured field on edit 1's successful
tool result:

```json
{"prepared_alternative":{"digest":"<sha256>","typed_bytes":3551,"confirm_fill_bytes":1773,"saved_tokens_estimate":444}}
```

Arm S receives the populated field. Arm C receives the byte-for-byte ordinary
product receipt. The proxy adds no coaching prose and never changes product
source. It is experiment-only.

## Frozen platform and isolation

- Product base: published prepared-confirm head
  `469141bdd3144a94a4e4ea2ed99c7ecd6ca26f5b`.
- Experiment branch: `experiment/steering-ab-20260831`.
- One local loopback MCP process is launched from that worktree. Each episode
  gets a fresh workspace, fresh Codex home, fresh Codex app-server process,
  fresh ephemeral thread, and fresh MCP session through an episode-local proxy.
- Subscription authentication only. API-key environment variables are removed.
- Primary caller: exact model `gpt-5.6-sol`, reasoning `high`, provider fallback
  disabled. Two descriptive bonus cells use exact model
  `gpt-5.3-codex-spark`, reasoning `low`, fallback disabled.
- Codex app-server holds one thread across edit 1 and edit 2. This is required:
  the prepared confirmation is session-bound, so a fresh `codex exec` process
  would make confirm+fill unreachable.
- Only `inspect_clojure` and `edit_clojure` are exposed. Shell and native file
  mutation are prohibited identically in both arms.

## Frozen fixture and episode

The fixture is one namespace with two complete top-level form replacements of
the same class. Edit 1 is a small whole-form replacement. Its prompt explicitly
requires the ordinary hand-typed `from`/`to` route, seeding that habit. After
the successful edit 1 call, the proxy performs an invisible, read-only
`inspect_clojure` forms read for `wall-policy` on the same upstream MCP session.
That creates the prepared confirmation used to calculate the signal.

Edit 2 is another supplied whole-form replacement. Its frozen `from` form is
1,575 bytes excluding its final newline, making the ordinary full `from`/`to` call materially
larger than confirm+fill. Both complete forms are printed in the edit 2 user
turn. The caller is told only to choose the best advertised clj-surgeon route
from evidence already in the session, make one exact mutation, avoid shell or
native editing, and finish with `DONE`. The prompt does not name, explain, or
ask about the signal.

`typed_bytes` is the UTF-8 byte count of canonical compact JSON for the frozen
ordinary edit 2 arguments. `confirm_fill_bytes` is the corresponding byte count
for `{confirm,fill}`. `saved_tokens_estimate` is
`floor((typed_bytes - confirm_fill_bytes) / 4)`. In accord with the 2026-08-31
`conn` tweezer result, route choice should be payload-dependent: hand typing is
optimal for tiny edits and a signal there would nag. The experiment proxy
therefore exposes `prepared_alternative` only when
`saved_tokens_estimate >= 100`. This exact threshold is frozen.

The fixture identities are frozen in `freeze.json`. At this checkout's
fixed-width episode path, ordinary edit 2 arguments are 3,551 bytes,
confirm+fill arguments are 1,773 bytes, and the estimate is 444 saved tokens.

## Frozen schedule and stopping rules

The mandatory sub-ceiling pilot is two fresh C episodes, `P-C1 P-C2`, excluded
from all confirmatory estimates. If both route through confirm+fill or request
the descriptor route, the fixture is broken for this question and the main
cohort does not run.

If the pilot passes, run 12 fresh Sol episodes in ABBA blocks:

```text
01 S   02 C   03 C   04 S
05 C   06 S   07 S   08 C
09 S   10 C   11 C   12 S
```

This is `n=6` per arm. Do not reroll valid cells. Infrastructure failures that
occur before a model can make the edit 2 path choice may be replaced once with
the same arm and a retained failure receipt. After the main cohort, run two
non-gating descriptive Spark cells in order `B-C1 B-S1`.

The product-consideration kill rule is a steering lift below 25 percentage
points at `n=6` per arm. No product mutation is authorized by this experiment.

## Frozen outcomes and gates

Primary per cell: whether the edit 2 caller chooses confirm+fill or requests
the prepared descriptor route. Operationally this is true when, before its
successful edit 2 mutation, the caller either:

1. calls `edit_clojure` with `confirm` and `fill`; or
2. calls `inspect_clojure` for exactly `wall-policy` to obtain the descriptor.

The arm rate is qualifying cells divided by six. Steering lift is S minus C in
percentage points.

Secondary outcomes:

- edit 2 caller emission bytes: sum of canonical compact UTF-8 JSON bytes of
  every model-authored MCP `arguments` object from edit 2 turn start through
  its successful mutation;
- edit 2 wall: app-server monotonic time from `turn/start` send to
  `turn/completed` receive;
- exactness: final fixture SHA-256 equals the frozen expected file SHA-256;
- wrong-subject count: any edit 2 mutation touching a file or owner other than
  `src/bench/steering.clj` / `wall-policy`, or any final changed path outside
  that file.

Exactness must be equal between arms and complete in both arms (6/6 versus
6/6), and wrong-subject must be zero, before reporting a steering verdict.
Otherwise report the safety-gate failure and treat the primary estimate as
non-actionable. Wall and bytes are descriptive medians; no significance test
is preregistered at this sample size.

Retain all sanitized app-server events, proxy wire events, model/tool calls,
monotonic timings, source hashes, model identity/reroute evidence, failures,
derived per-cell scores, aggregate report, and a SHA-256 manifest. If the caller
spontaneously mentions the signal, retain its exact statement as qualitative
gold; it is never a gate or a scored outcome.

## Claim boundary

This answers only whether this structured in-flow receipt signal steers this
Codex caller on this wall-sized supplied-edit class. It does not establish a
general latency win, a tiny-edit policy, a population effect, or permission to
ship the proxy field.
