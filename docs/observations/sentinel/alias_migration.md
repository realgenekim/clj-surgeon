# verb-sentinel — alias_migration

Weekly tool arm on the trunk build (MCP 7906) against the frozen row-2 request, on the
history-isolated seed repo. A SMOKE DETECTOR, not a speed certification.
Registered CONTEXT (agent-caller arms, NOT like for like with this job): native caller work
median 121 s (six N arms; exact median 119.8 s), tool caller work median 27 s (six D arms).
This job issues the call from the RUNNER, so its caller number is a call wall, not a caller's
work, and is structurally smaller. The like-for-like comparison is the PREVIOUS SENTINEL; the
drift band is the registered 2*s_N = 46.346 s.

## 2026-09-08T13:17:26Z  arm=sentinel-20260908T131651Z port=7906

- oracles: ACCEPTED=YES (PASS), call rc=0
- caller=4.310s ; primary=34.285s ; previous sentinel caller=none
- drift band vs the PREVIOUS SENTINEL, 2*s_N=46.346 s -> no-baseline (this is the first sentinel)
- context only, NOT like for like (those are agent-caller arms; this is a runner-issued call): registered tool caller 27 s, registered native caller 121 s
- CORRECTION 2026-09-08T13:2xZ: the first publication of this row compared 4.310 s against the 27 s AGENT-caller median and printed "IN". That comparison was not like for like. The band now runs against the previous sentinel and reads no-baseline on the first run; the program was corrected in the same edit (~/bin/verb-sentinel).
- evidence: `/var/tmp/forge/row2/sentinel-20260908T131651Z` (attestation.txt, candidate.patch, oracle-run.txt, wall.txt, stopwatch.log) ; job log `/var/tmp/forge/tighten/sentinel-alias-20260908T131651Z.log`

## 2026-09-08T13:21:36Z  arm=sentinel-20260908T132004Z port=7888

- oracles: ACCEPTED=NO (FAIL), call rc=3
- caller=0.049s ; primary=29.996s ; previous sentinel caller=4.310s
- drift band vs the PREVIOUS SENTINEL, 2*s_N=46.346 s -> IN
- context only, NOT like for like (those are agent-caller arms; this is a runner-issued call): registered tool caller 27 s, registered native caller 121 s
- evidence: `/var/tmp/forge/row2/sentinel-20260908T132004Z` (attestation.txt, candidate.patch, oracle-run.txt, wall.txt, stopwatch.log) ; job log `/var/tmp/forge/tighten/sentinel-alias-20260908T132004Z.log`

