# verb-sentinel — alias_migration

Weekly tool arm on the trunk build (MCP 7906) against the frozen row-2 request, on the
history-isolated seed repo. A SMOKE DETECTOR, not a speed certification.
Registered: native caller work median 121 s (six N arms; exact median 119.8 s), tool caller work
median 27 s (six D arms), 2*s_N = 46.346 s.

## 2026-09-08T13:17:26Z  arm=sentinel-20260908T131651Z port=7906

- oracles: ACCEPTED=YES (PASS), call rc=0
- caller=4.310s ; primary=34.285s ; previous sentinel caller=none
- registered tool caller 27 s, band 2*s_N=46.346 s -> IN ; registered native caller 121 s
- evidence: `/var/tmp/forge/row2/sentinel-20260908T131651Z` (attestation.txt, candidate.patch, oracle-run.txt, wall.txt, stopwatch.log) ; job log `/var/tmp/forge/tighten/sentinel-alias-20260908T131651Z.log`

