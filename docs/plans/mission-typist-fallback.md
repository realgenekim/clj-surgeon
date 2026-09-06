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
