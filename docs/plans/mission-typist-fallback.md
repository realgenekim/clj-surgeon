# Explicit transport fallback addendum

This extends `mission-typist-executor.md` without enabling fallback in any existing
mission or cohort. The parent freezes opt-in authority separately in its saved plan.

The trusted transport JSON may add `"fallback":{"provider":"groq","max_tokens":N}`.
Absent means the old behavior. This option requires route `openrouter-cerebras`,
`candidates=1`, positive integer N, and primary `max_tokens` + N <= 8192. Primary
output allocation is unchanged. Both requests reserve their whole allocation;
missing provider usage cannot enlarge the budget. The existing timeout_s is one
shared deadline including runtime key reads and both requests, not two deadlines.
Only HTTP 429 (`provider-rate-limited`) or 503 (`provider-unavailable`) may activate
one direct Groq attempt. No retries, delay, alternative providers or routes.

Timeouts, other HTTP failures, key failures, wrong model/upstream, malformed or
oversize responses, content refusal, empty/length output and acceptance failures
never activate fallback. These are protocol tests, not a claim of observed live
429/503 failures or provider availability. Exact fixed endpoints, runtime EDN key
files, model `openai/gpt-oss-120b`, Cerebras upstream pin and no redirects/proxies
remain unchanged. Successful Groq identity requires the actual model plus the
fixed direct endpoint (and matching upstream if present).

The selected candidate retains `attempts`: one or two redacted records with route,
requested model/upstream/token budget, observed identity when known, usage,
request wall, finish reason, content and typed error. No observed identity is
invented for HTTP refusals. `request_started` distinguishes a dispatched request
from a key-load refusal; an HTTP refusal also records its numeric status. Each candidate records actual fallback activation;
the batch records the opt-in policy separately. A failed second attempt stays
failed. Total requested output <=8192, attempt count <=2 and one deadline remain
true even when a key cannot be loaded. An unstarted fallback is explicitly skipped.
The executor owns child-process cancellation and downstream proof; this client
provides untrusted text only.

Verification matrix: absent opt-in compatibility; exact config/cap rejection;
429/503 success and second failure; every ineligible failure; primary success;
wrong identity on fallback; deadline exhausted before fallback; remaining timeout
and token allocation; lazy fixed-key loading and redaction; CLI exit and receipts.
All tests use fake keys, responses and clocks. No live network, paid calls or wall
performance claims. Run `python3 test/python/typist_transport_test.py`.

Verification: 24 mocked Python tests pass (13 existing, 11 new). The initial
new matrix refused under the old client; a separate over-deadline success
witness failed before the post-request deadline check. Receipts are retained at
`/var/tmp/forge/typist-fallback-{red,deadline-red,green}.txt`. AST parse and Git
whitespace checks pass. No live provider or integrated executor run was made.

## Frozen Clojure admission and execution

The optional public request is
`{:typist {:max-tokens 4096 :fallback {:provider :groq :max-tokens 4096}}}`.
Without either field, primary max-tokens remains 8192 and fallback is absent.
Explicit nil, nonintegers, nonpositive allocations, over-budget sums, a different
fallback provider, fallback on direct Groq/Spark, or extra fallback-map fields
refuse during pure route admission as `:condition :generation-budget` under the
existing typed route refusal. Unrelated existing typist fields keep their prior
validation behavior; this change does not invent a closed top-level field set.

The route freezes `:generation {:max-tokens P :timeout-s 30 :fallback ...}` with
only validated scalar values. The dossier hash covers that route. `request-one!`
uses only saved `authority :route :generation`, translates the optional map to
Python field spellings, and never reads generation policy from an apply-time
request. Old saved routes without :generation retain 8192/no-fallback defaults.
The direct Python child retains the fixed 30-second total request deadline and
35-second outer process bound. Neither fallback nor k silently enlarges a single
child's deadline or output-token reservation.

Race k is unchanged: k independent children each receive candidates=1; each may
make at most two serial route attempts when explicitly opted in. Thus the total
maximum reserved output across the race is k times the admitted per-child sum,
not one 8192-token reservation shared by all k. No retries for candidate quality,
parser/refusal, gate, or acceptance failures. Fallback policy remains outside
all already-frozen cohorts until explicitly preregistered.

Cost reporting from e805318f is preserved: OpenRouter usage is requested; known
provider-reported cost and token fields remain in each attempt. A 429/503 HTTP
response whose usage is unavailable stays unknown, not zero. No estimated Groq
price is introduced, and top-level selected cost must not substitute for summing
known per-attempt costs while separately reporting unknown attempts.

Dogfood record: existing Clojure owners were inspected through installed Surgeon
`:cat`. Parent explicitly authorized native insertions/literal edits after those
observed anchors because no registered persistent MCP was available and the
installed CLI documented no top-level insertion entrance. This is new-feature
work, ineligible for the mechanical typist executor; no unsupported tool syntax
or large whole-owner replacement was manufactured. No provider calls were made.

Integration verification: pure admission RED (29 failed assertions), followed by
GREEN; fake saved-policy forwarding RED (2 failed assertions: primary allocation
and absent fallback), followed by GREEN. Final JVM admission + executor suites:
14 tests / 243 assertions, no failures/errors. Python cost + fallback matrix:
29 tests pass. Formatter, AST and Git whitespace checks pass. Receipt directory:
`/var/tmp/forge/typist-fallback-integration/`. No live provider or timed experiment.
Lane enrollment remains parent-owned: admission namespace now 6 deftests; executor
namespace now 8. Full repository landing gates and independent review remain owed.

## Receipt-derived provider-fallback events

Emit `mission-provider-fallback`, explicitly distinct from a native-tool fallback,
only after an actual two-attempt receipt: OpenRouter/Cerebras request started and
returned typed HTTP429/503; the second Groq request actually started. Planned
fallback, key-load failure before dispatch, deadline-skipped fallback and absent
or malformed records do not prove a fallback call and emit no such event.
Native-tool fallback telemetry remains a separate pending requirement.

One event is emitted per completed fallback receipt at request-one!, including
losing race children that finish before cancellation. Capture only the mission
context binding across worker threads; never infer an id for direct calls. Do
not propagate a mission state into a provider event. If a child never returns a
receipt, this surface has no evidence and cannot claim a completed fallback.

Wall and usage come from the second attempt, not an observer stopwatch or the
planned route. Unknown measurements remain nil. Actual model/upstream are added
only when the receipt's model and upstream match the pins; the provider route is
known from the dispatched Groq endpoint. Drop source, prompts, raw provider errors,
paths and arbitrary fields. Logging must not change result or exception behavior.
Pure projection, actual-request mock wiring, context threading and logger failure
need tests. No live provider call is authorized by this telemetry change.

This event is a provider sub-event, not another top-level mission invocation.
Verification: the existing request-one! seam first failed three event assertions
with an unchanged successful transport result. Final combined observer/phase/
executor gate: 30 tests / 201 assertions PASS. Eight new battery tests cover
actual receipt routing, observed facts, exclusions, failed/unknown data, privacy,
logging failure, three worker contexts, and direct calls without invented ids.
A real temporary ledger write is checked and deleted in finally. Parent owns
lane enrollment: add mission-provider-fallback-events-test8 to :battery.

The first combined run lacked an explicit events path; any fixture events it
appended to shared telemetry are test contamination, never paid-provider usage.
It is retained as green-unisolated.txt. The final run explicitly set inherited
CLJ_SURGEON_EVENTS_FILE=/var/tmp/forge/provider-fallback-events/test-events.jsonl
and produced 12 isolated fixture phase events. Receipts are under
/var/tmp/forge/provider-fallback-events/. No live provider calls, timing claims,
production ledger backfill, or native-tool-fallback implementation occurred.
