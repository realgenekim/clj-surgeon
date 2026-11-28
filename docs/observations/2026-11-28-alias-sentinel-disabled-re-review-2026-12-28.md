# The weekly alias sentinel on Anvil is disabled; re-review due 2026-12-28

Written 2026-11-28T08:36:02Z by the surgeon seat on forge@anvil. Standing policy: a disabled tripwire carries a re-review date.

What was disabled: the forge crontab line that ran `~/bin/verb-sentinel` every Sunday at 07:00 UTC. It called the clj-surgeon MCP endpoint on port 7906 (dead since the MCP retirement, Gene's ruling 71 b) and ran the alias-migration row-2 oracle; its last two verdicts (2026-10-18 and 10-25) were `oracle=FAIL verdict=SUSPEND`, and the finding on main (`docs/observations/2026-11-28-alias-migration-sentinel-verdict.md`, pull request 77) records that every surviving output of those runs matches the golden and that the failing step cannot be recovered because the runs' records are gone. The verb is frozen off main (ruling 75); a weekly FAIL on a frozen verb teaches seats to ignore alarms (second fleet, piece 3).

The crontab line as it stands (read with `crontab -l` on Anvil as forge):

```text
# DISABLED 2026-11-28, RE-REVIEW 2026-12-28 (Gene ruling 71b: MCP retired everywhere; alias migration frozen off main, ruling 75). The weekly verb sentinel called the dead MCP endpoint on 7906 and has refused every Sunday since 11-01; its 10-18 and 10-25 alias SUSPEND verdicts came from the oracle apparatus, whose failing step could not be named because its records were reaped from /var/tmp (docs/observations/2026-11-28-alias-migration-sentinel-verdict.md); script and logs kept: 0 7 * * 0 /home/forge/bin/verb-sentinel >> /var/tmp/forge/tighten/cron-verb-sentinel.log 2>&1
```

Backup of the crontab before the change: `~/crontab.bak-2026-11-28-074707` on Anvil (untracked; this file is the tracked record).

Re-review on 2026-12-28, owner: the surgeon seat, or whoever then holds the clj-surgeon seat on Anvil. Question to answer then: is there any sentinel worth running for the frozen verb (none, if the archive tag is the only home), or should the line be deleted outright. Until then the script and its logs are kept unchanged.
