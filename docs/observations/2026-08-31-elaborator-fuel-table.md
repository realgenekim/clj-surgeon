# Elaborator fuel table: measured completion

## Frozen completion preregistration

Frozen at 2026-08-30 21:29 PDT, before this completion experiment's first
model call. This completion extends
`experiment/elaborator-fallback-battery-20260831` at `e3df8ee` and retains
that battery's original frozen rules for its existing arms. Before publication
the completion branch was fast-forwarded to the parent's immediate-arm results
commit, `873ebfc`, so those calls and artifacts are inherited rather than
duplicated.

The missing-cell fills are run serially. LUNA and TERRA use the supplied
`/private/tmp/bang-rig`; before changing models, the current daemon is stopped
and restarted with the exact `BANG_MODEL` pin. No two model daemons or fill
trials overlap. LUNA receives at least four full guarded-bang fills, TERRA is
extended to at least four guarded-bang fills total, and `openai/gpt-oss-120b`
receives at least eight fills through the supplied `/tmp/or-bang.sh` pattern.
There are no correctness retries. Transport or quota failures still consume a
scheduled trial and are reported in the denominator.

Scoring is frozen as follows:

- **Exact**: the isolated proposed replacement parses as exactly one Clojure
  form, its top-level owner is the requested owner, and its normalized reader
  value equals the preregistered expected form. A committed guarded edit is
  necessary but not sufficient.
- **One-shot**: the first and only elaborator response is submitted once to the
  guarded edit and commits without repair, reprompt, or a second mutation.
- **Wrong-subject**: the returned top-level owner differs from the requested
  owner, or the mutation targets a file other than the fixture. Every
  wrong-subject result scores zero exact even if some inner expression is
  useful.
- **Wall splits**: report read/inspection, model elaboration, guarded apply, and
  end-to-end wall when exposed by the adapter. The bang rig's retained
  `spark_ms` field is reported as model elaboration; total rig wall is reported
  as end to end. Missing splits are `n/a`, never inferred from total wall.
- **Aggregation**: every cell states scheduled `n`. Counts use every scheduled
  trial as denominator. Wall medians use completed trials with the named timing
  field and state their timing `n`; failed trials remain visible separately.
  Even-`n` medians are the arithmetic mean of the two middle values.

For the metered OpenRouter arm, the fixed ceiling is 12 scheduled calls or
US$0.10 of provider-reported cost, whichever arrives first; the target sample
is eight. After each response, record prompt, completion, and total tokens plus
the response's provider-reported cost. Stop immediately after a response makes
the cumulative cost meet or exceed the ceiling. If the endpoint omits cost,
retain raw usage and label dollar spend unavailable rather than inventing it.
The API key is read only from the user-supplied exact path
`~/src.local/secrets/openrouter.edn`; no home-directory scan is permitted and
no secret is copied into an artifact.

## Unified fuel table

Every denominator below is a scheduled fill. Every timing cell states its own
measured timing `n`; no failed fill was removed from a wall median.

| Elaborator arm | Scheduled fills | Exact | One-shot | Wrong subject | Schema / parse fumbles | Median read | Median model | Median apply | Median end to end |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| `gpt-5.6-terra` | n=6 | 6/6 | 6/6 | 0/6 | 0/6 / 0/6 | 173.4 ms (n=6) | 1,557.8 ms (n=6) | 181.8 ms (n=6) | 2,258.1 ms (n=6) |
| `gpt-5.6-luna` | n=6 | 5/6 | 5/6 | 0/6 | 0/6 / 1/6 | 172.0 ms (n=6) | 1,486.6 ms (n=6) | 168.7 ms (n=6) | 2,001.5 ms (n=6) |
| `gpt-5.6-sol` | n=6 | 6/6 | 6/6 | 0/6 | 0/6 / 0/6 | 175.6 ms (n=6) | 3,051.0 ms (n=6) | 198.4 ms (n=6) | 3,741.9 ms (n=6) |
| `openai/gpt-oss-120b` on Cerebras | n=8 | 8/8 | 8/8 | 0/8 | 0/8 / 0/8 | 85.0 ms (n=8) | 393.0 ms (n=8) | 100.5 ms (n=8) | 650.0 ms (n=8) |

The guarded-fill result is cleanly separated from the deterministic decode
probe. TERRA, Sol, and oss-120b were exact on every measured fill. LUNA was
exact on five of six and emitted one unterminated `fill-branch-call` form. The
guard rejected it; under the frozen no-retry rule it remains a parse fumble,
scores zero exact and zero one-shot, and is not a wrong-subject event.

On these samples, oss-120b had the lowest median fill wall: 3.47x lower end to
end than TERRA and 5.76x lower than Sol. TERRA retained perfect fill
reliability while using 39.6% less median end-to-end wall than Sol. LUNA's
median was lower than TERRA's. Its 5/6 exact, zero-wrong-subject,
zero-schema-fumble result meets the parent battery's literal `reliable enough`
threshold, but the malformed sixth result did not commit. TERRA therefore
ranks before LUNA under the frozen reliability-first ordering; oss-120b is the
measured external-provider speed and reliability leader, not a subscription
arm.

## OpenRouter meter

The oss arm stopped at its frozen target of eight scheduled calls. All eight
responses reported `openai/gpt-oss-120b` and provider `Cerebras`; none used a
provider fallback.

| Meter | Measured value |
|---|---:|
| Scheduled / completed calls | 8/8 |
| Prompt tokens | 970 (n=8) |
| Completion tokens, including reasoning | 805 (n=8) |
| Total tokens | 1,775 (n=8) |
| Provider-reported spend | **US$0.00094325** across n=8 |
| Spend in cents | **0.094325 cents** across n=8 |
| Frozen ceiling | US$0.10 or 12 calls |

This was 0.94325% of the dollar ceiling. The first call was a 4.449-second
model cold outlier; it remains in the n=8 median and was not retried.

## Decode context from the parent battery

The parent battery's separate 1-through-200 decode endpoint is retained rather
than conflated with guarded-fill wall.

| Model | B sequence oracle | C `ok` oracle | E2E decode rate | Bootstrap-subtracted rate |
|---|---:|---:|---:|---:|
| `gpt-5.6-terra` | 0/5 scheduled | 5/5 scheduled | not estimable (valid B n=0/5) | not estimable (valid B n=0/5) |
| `gpt-5.6-luna` | 5/5 scheduled | 5/5 scheduled | 40.9 tok/s (valid B n=5) | 58.1 tok/s (valid B n=5, C n=5; resolved) |
| `gpt-5.6-sol` | 5/5 scheduled | 5/5 scheduled | 37.5 tok/s (valid B n=5) | 58.1 tok/s (valid B n=5, C n=5; resolved) |

TERRA's five B responses did not reach the exact 1-through-200 oracle, so its
rate is deliberately not promoted from partial output. This does not conflict
with its 6/6 guarded fills: the endpoints ask different questions.

## Per-fill receipts

The scorer reads exactly one Clojure form, checks its top-level owner, compares
its normalized data value with the frozen expected form, and checks structured
mutation paths. The tables below are projections of the committed JSON
receipts; the score files retain replacement hashes and complete usage facts.

### TERRA, n=6

| Trial | Exact | One-shot | Wrong subject | Read ms | Model ms | Apply ms | Total ms |
|---|---:|---:|---:|---:|---:|---:|---:|
| 01 branch-call | 1 | 1 | 0 | 152.8 | 1,591.9 | 283.4 | 2,249.2 |
| 02 literal | 1 | 1 | 0 | 297.2 | 1,523.7 | 180.3 | 2,300.1 |
| 03 map-value | 1 | 1 | 0 | 161.0 | 1,745.0 | 183.3 | 2,267.1 |
| 04 qualified-call | 1 | 1 | 0 | 165.8 | 2,218.3 | 190.8 | 2,709.3 |
| 05 selected-arity | 1 | 1 | 0 | 181.0 | 1,493.9 | 174.1 | 1,984.3 |
| 06 thread-tail | 1 | 1 | 0 | 225.0 | 1,500.3 | 142.2 | 2,011.0 |

### LUNA, n=6

| Trial | Exact | One-shot | Wrong subject | Read ms | Model ms | Apply ms | Total ms |
|---|---:|---:|---:|---:|---:|---:|---:|
| 01 branch-call | 0 | 0 | 0 | 159.0 | 4,408.9 | 163.9 | 4,874.4 |
| 02 literal | 1 | 1 | 0 | 562.5 | 1,888.1 | 303.6 | 3,161.7 |
| 03 map-value | 1 | 1 | 0 | 174.2 | 1,341.4 | 169.9 | 1,820.8 |
| 04 qualified-call | 1 | 1 | 0 | 189.7 | 1,436.5 | 167.5 | 2,007.7 |
| 05 selected-arity | 1 | 1 | 0 | 169.8 | 1,432.0 | 220.0 | 1,965.4 |
| 06 thread-tail | 1 | 1 | 0 | 160.6 | 1,536.7 | 167.4 | 1,995.3 |

### Sol, n=6

| Trial | Exact | One-shot | Wrong subject | Read ms | Model ms | Apply ms | Total ms |
|---|---:|---:|---:|---:|---:|---:|---:|
| 01 branch-call | 1 | 1 | 0 | 169.7 | 3,850.7 | 181.9 | 4,360.7 |
| 02 literal | 1 | 1 | 0 | 287.4 | 2,957.5 | 193.7 | 3,729.2 |
| 03 map-value | 1 | 1 | 0 | 181.5 | 2,796.4 | 148.9 | 3,289.5 |
| 04 qualified-call | 1 | 1 | 0 | 158.9 | 3,929.3 | 261.6 | 4,502.4 |
| 05 selected-arity | 1 | 1 | 0 | 153.6 | 3,144.4 | 275.6 | 3,754.6 |
| 06 thread-tail | 1 | 1 | 0 | 211.6 | 1,886.9 | 203.1 | 2,461.9 |

### oss-120b, n=8

The six frozen cases ran once, then the fixture was restored and the literal
and branch-call endpoints were repeated as scheduled fills 7 and 8. They are
replicates, not correctness retries.

| Trial | Exact | One-shot | Wrong subject | Read ms | Model ms | Apply ms | Total ms |
|---|---:|---:|---:|---:|---:|---:|---:|
| 01 literal | 1 | 1 | 0 | 193.0 | 4,449.0 | 125.0 | 4,831.0 |
| 02 qualified-call | 1 | 1 | 0 | 92.0 | 1,848.0 | 100.0 | 2,107.0 |
| 03 map-value | 1 | 1 | 0 | 71.0 | 711.0 | 134.0 | 976.0 |
| 04 selected-arity | 1 | 1 | 0 | 160.0 | 267.0 | 75.0 | 573.0 |
| 05 thread-tail | 1 | 1 | 0 | 70.0 | 323.0 | 99.0 | 554.0 |
| 06 branch-call | 1 | 1 | 0 | 78.0 | 366.0 | 131.0 | 644.0 |
| 07 literal repeat | 1 | 1 | 0 | 106.0 | 367.0 | 85.0 | 623.0 |
| 08 branch-call repeat | 1 | 1 | 0 | 75.0 | 419.0 | 101.0 | 656.0 |

## Seriality and identities

The rig snapshot records TERRA warm at `04:27:03.782Z`, shutdown at
`04:27:26.901Z`; LUNA spawn at `04:29:23.696Z`, warm at `04:29:30.219Z`, and
shutdown at `04:29:46.756Z`; and Sol spawn only afterward at
`04:31:46.545Z`. Thus the ChatGPT-account bang arms did not overlap. The oss
arm used a separate MCP server rooted in the completion worktree and ran its
eight provider calls synchronously.

| Artifact | SHA-256 |
|---|---|
| Frozen parent preregistration commit | `e3df8ee7e6c3a47cee0de3874305b3477ebaf4c2` |
| Parent immediate-arm results commit | `873ebfc9cb42f0d83fdc1286ad7e74e5995cb085` |
| Rig target commit | `4ec9394c59805addef05076cf2c78c463b8ea6e6` |
| Rig server | `209aa9b799b5819bbd60332084698c36b59df0f996c30a6a07a32b8438ddbad8` |
| Rig client | `898cb2d8c700b0f225c0f1907ec4056315815ae4fc2839c446666d3a58f57491` |
| Frozen fill cases | `3f311542257b4b83aa79eea87c9d9ee11bc640a26bd86b5e7379992d3ec845ca` |
| Parent preregistration | `bd180724eb9a5e406e0d57371ebd0db1b2d6cd1bddb397e312b57d404cc2bdcb` |
| Metered oss adapter | `57be28a1071d6121226b382234b683f8786929ea5ed2550904a895dd542dfe88` |
| Clojure scorer | `d487a6a58443539f8c2059456fdc8cef6003657ab52fe2e82fa32ad3a3539e2c` |
| Rig seriality log snapshot | `90f198cce5a2361a3b110da653b2dc7facec8e44b3e6e6350aef772233eef516` |
| TERRA result tree, 46 files | `b6cc42da2b910e88f4bc37cfcdc1eeac18a92cf1f2cd02371f059fdf9ff6dc1c` |
| LUNA result tree, 46 files | `52e2352e8ba5035c242e9e07dd7d45767b54fb4d29db71e8a67ed0e441c9d830` |
| Sol result tree, 46 files | `44d1b0cebd98e4ea56d2ecac25fe21990d9868c8978d5d9cb86f91f63ee3ca2b` |
| oss-120b result tree, 9 files | `00471ea12278ae8fc8a7ae325b397bbb55a3c2660f6ab5607e9153b6be69f82d` |

The result-tree identity is SHA-256 over sorted relative path, NUL, and each
file's SHA-256 digest. The copied rig log is a point-in-time seriality receipt;
no API key or credential is present in any committed artifact.
