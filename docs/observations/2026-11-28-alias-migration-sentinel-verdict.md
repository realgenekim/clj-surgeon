# The alias-migration sentinel's SUSPEND verdicts: what survives says the output was correct

Written 2026-11-28 by the clj-surgeon seat on Anvil, under the mayor's night orders (OPEN-LOOPS item 1),
read independently once before push. Question put: the weekly verb sentinel said
`oracle=FAIL accepted=NO verdict=SUSPEND` for alias migration on 2026-10-18 and 2026-10-25, after PASS on
09-08, 09-13 and 09-20, and has refused every Sunday since 11-01. Is the verb wrong, is the oracle wrong,
or did the fixture move?

## Verdict

**Every surviving snapshot of the verb's output is byte-identical to the historical golden. No incorrect
migration is evident. The fixture did not move. The oracle step that said NO, and its cause, cannot be
recovered: the runs' own records under `/var/tmp` are gone.** On what survives, the two SUSPEND verdicts
were not correctness failures of the verb's output.

- In both FAIL runs the migrated result on all nine caller files equals the golden commit byte for byte,
  as it does in the 09-20 PASS run, and the three arms' tracked trees are the same tree.
- Today's planner on astra 7b312f85 reproduces the golden bytes on a frozen fixture, with and without the
  dangling symlink the tree carries: 9 files, 21 sites, alias histogram `{"mjson" 9}`, 3 collisions
  resolved, every file identical to golden, the two plans identical.
- This verdict does not re-admit automatic routing and establishes no performance win. Gene has since
  ruled the route suspended (64a) and the verb frozen off main (75); see the last section.

## Evidence

**What survived and where.** The sentinel built each weekly arm as a git worktree under `~/src`
(`mvr-row2-sentinel-<run>`), outside `/var/tmp`. Those directories exist for every run: 09-08 (two), 09-13,
09-20, 10-18, 10-25. Everything the runs wrote under `/var/tmp/forge/row2/<arm>/` (attestation.txt,
request.json, mcp-response.json, candidate.patch, oracle-run.txt, wall.txt) and under
`/var/tmp/forge/tighten/sentinel-alias-*.log` is absent. `/usr/lib/tmpfiles.d/tmp.conf` carries
`q /var/tmp 1777 root root 30d` and `systemd-tmpfiles-clean.timer` is active (daily after activation).
Directories remain, files do not: the two October arm directories hold only an empty `oracle/`; `oracle/`
and `oracle2/` are empty; `setup-arm.sh`, `grade-arm.sh` and `seed.sha` are absent; the seed repository
`/var/tmp/forge/row2/seed-repo/.git` has no HEAD, no config, no ref files and zero object files. The seed
repository's `objects/` and `refs/` directories were last modified 2026-10-12 at 19:25Z and `oracle2/` on
2026-10-20 at 19:25Z, inside the window between the last PASS and the FAILs. That the cleaner did the
deleting is an inference from the configuration and the empty directories, not a journal record; older
regular files do survive under `/var/tmp/forge` when something reads them. `/var/tmp` is not a durable
evidence store.

**The surviving candidates are correct.** For each of the nine caller files
(`app_route`, `bridge3_new`, `channel`, `codex_app_server`, `director_control`, `director_outbox`,
`reducer_session`, `server`, `tts`), the file in the 10-18 arm and in the 10-25 arm compares byte-equal
(`cmp`) with the file at marvin-voice-remote `5a4b4bc`, the historical golden; so does the 09-20 PASS arm
(27 comparisons). With a C locale, `comm` over `find src` of the 09-20 arm and the 10-18 arm lists no
entry present in one and absent from the other, and all 26 regular files under `src/` compare equal; the
reviewer extended this to all 352 tracked paths in each of the three arms against the golden commit.
Generated files, logs and git metadata differ between arms; the tracked trees do not.

**The fixture did not move.** The seed is the complete tree at `5a4b4bc^` (9f9cf614) with `json.clj` from
`5a4b4bc`, as preregistered in clj-surgeon-records `2026-09-08-row2-alias-preregistration.md`. The
`src/datastar_kit` symlink (target `../../datastar-helpers/src/datastar_kit`, absent on this machine) is
tracked in marvin-voice-remote since fb72b36 (2026-05-30) and is present in all six surviving arms,
September and October alike; the reproduction shows it changes nothing.

**What the environment witnesses say, and do not say.** The oracle's O1 step runs the repository's suite
in the arm; its log `00SERVER-LOGS.txt` has the same length and level counts in all three runs (1,844,
1,846 and 1,844 lines; 1,370, 1,372 and 1,370 info; 254 warnings; 43 errors each), and the same deliberate
fixture failures ("GCS is on fire" ten times in each). A similar log does not prove the same exit status.
The O3 step is a clj-kondo delta; `~/bin/clj-kondo` and `/usr/local/bin/clj-kondo` are the same
v2026.08.04 binary (byte-identical; file dates 09-03 and 08-13) and today report identical findings on the
clean seed and on the FAIL candidate (37 errors, 99 warnings, 23 infos, identical normalized multisets), so
the delta is zero today; that does not recover October's O3 result. The clj-kondo admission events for
07:0xZ on each Sunday show only the namespace-split arm's two events in every week, PASS and FAIL alike,
so that witness is neutral. What is left as the cause is the oracle runner (`grade-arm.sh` with
`oracle2/run_oracles_v2.sh` and its helpers, frozen by hash in `freeze-stage2.edn`) and whatever state it
read from `/var/tmp` on those two Sundays; a reaped input or helper is consistent with the directory
mtimes and cannot be proven.

## Reproduction on today's verb

`test-fixtures/alias-migration/mvr-5a4b4bc/` ships the seed's nine caller files at `5a4b4bc^` plus the
seeded `json.clj` and the tracked symlink, the golden nine files at `5a4b4bc`, the preregistered request,
a manifest of what ships and the manifest of the complete archive used on 2026-11-28. `repro.clj` beside
them loads an astra/namespace-split checkout and runs `validate-request -> plan! -> plan->spec ->
commit!` against a copy of the seed, then compares every file with the golden; its header gives the
command. The saved run of 2026-11-28 against the complete archive on 7b312f85 (Codex, this machine):
planned 9 files, 21 sites, `{"mjson" 9}`, 3 collisions; applied; all nine golden-byte-identical; the same
with the symlink removed; the two plans identical; the seed untouched; the checkout's focused planner tests
66 tests, 850 assertions, green, and `make alias-migration-test` 164 tests, 3,495 assertions, green
outside the sandbox. The packaged script was then made self-contained and re-run from the shipped fixture
before this pull request was pushed.

## The sentinel itself

It has been blind since 2026-11-01: it refuses with `mcp-endpoint-absent` because nothing listens on 7906
and it never starts a server (the four refusals in its log are undated lines after the 10-25 row). Its
row-2 setup and grading harness no longer exists on disk; `~/bin/verb-sentinel` itself does. Its records
clone (`~/src/clj-surgeon-records`, branch `records/MCP-main`) is 27 commits ahead of its remote and holds
the sentinel rows. Proposal, not done here: retire the cron entry with a re-review date. A replacement is
no longer wanted, since the verb is frozen off main; the fixture documents the verb under its archive tag.

## The rulings this record serves

The managed routing block in every seat's boot file still routes alias migration automatically and says a
correctness failure suspends the class. Gene ruled on 2026-11-28, verbatim: "64a; let's purge it from the
code -- but run brain fleet on how valuable this would be. I think it should be awesome; especially think
about how we fix code when we have high crapper or dryer code. Isn't this exactly the tool we need?" The
mayor reads "purge it from the code" as the automatic route leaving the routing text, not as deleting the
planner or its tests. Later the same day, on the second fleet's answer, Gene ruled (75) that alias
migration is frozen off main: not ported, not routed, reachable only under an archive tag, the branch
deleted with the others at cutover (decision 72). So the plate suspension is its own pull request on astra,
the verb stays under `archive/astra/namespace-split`, and this document is the record of why the sentinel
said what it said: not a gate for a port, which will not happen.
