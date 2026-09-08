Parent outcome defect — completed 2026-09-08T20:59:06.492860+00:00

Repository: `/home/forge/src/kaocha-sublime`, `master`, baseline `c155a65d`.
Red-test commit: `338f5fe757644ccd056e5833d7b950d6f612e9d4`.
Fix SHA: `b1a838a282696a3f8e218de2bdcb368c48bcd5df`.
Push: `git push origin master` succeeded (exit 0). `git ls-remote origin refs/heads/master` confirmed `b1a838a282696a3f8e218de2bdcb368c48bcd5df`; working tree clean.

Intent: [AGENT-PARENT-OUTCOME-019](/home/forge/src/kaocha-sublime/docs/intent/registry.edn), linked in production code and three regression tests. Source: [blind-reader witness](/home/forge/src/clj-surgeon-records/docs/observations/2026-09-08-blind-reader-witness.md) at records commit `2035d62a`, case 4. The defect was that a reader could tail-read an automatic green despite its red parent.

Every newly emitted automatic follow-up DONE now carries `parent_outcome`, an object with `outcome`, `fail`, and `error`. Parent identity, digest and summary are taken from the same read of the parent receipt and persisted in the child. A missing/invalid parent produces `{"outcome":"unknown","fail":null,"error":null}`; replay uses the persisted summary. Primary DONE records omit the field. STATUS/FOLLOW selection and exit semantics are unchanged. The grammar block in [docs/agent-v1.md](/home/forge/src/kaocha-sublime/docs/agent-v1.md) explicitly keeps `v:1` for this additive field and documents strict-reader compatibility.

Red first: before implementation, the full suite ran 65 tests / 463 assertions and failed with 7 failures, 0 errors. Four failures were the actual emitted follow-up records lacking the fail/error/pass/unknown parent summaries; two were grammar requirements; one was the deliberately not-yet-implemented intent linkage. The primary-token tests passed on that baseline. Log: `/var/tmp/forge/parent-outcome-checks/red.log`. Command: `clojure -J-Djava.io.tmpdir=/var/tmp/forge/parent-outcome-checks -M:test -e ignored` (the extra arguments are ignored by the test runner).

Final checks:

- `clojure -J-Djava.io.tmpdir=/var/tmp/forge/parent-outcome-checks -M:test`: 65 tests, 463 assertions, 0 failures/errors; exit 0. Log: `/var/tmp/forge/parent-outcome-checks/green-final.log`.
- `~/bin/clj-kondo --lint src test`: 0 errors, 0 warnings; exit 0. Log: `/var/tmp/forge/parent-outcome-checks/lint.log`.
- `python3 scripts/agent_watch_witness.py`: exit 0, `watch-complete` passed. Includes the new parent-summary assertion, primary-token selection, reload error/recovery, deletion, and killed-run behavior. Log: `/var/tmp/forge/parent-outcome-checks/watch.log`.
- Independent Python JSON inspection matched each known emitted summary against its parent's actual DONE, checked null counts for unknown, and checked the record byte cap and child arithmetic. All fail/error/pass/unknown and natural-watch-pass cases passed. Captured lines and paths: `/var/tmp/forge/parent-outcome-checks/emitted-witnesses.json`.
- `git diff --check`: clean.

The records below are verbatim runner-owned `.events` lines, with no renaming, re-parenting, reformatting, or synthesized verdicts. The red/error/green/missing-parent integration cases run real Kaocha tests through `api/run`; the test harness explicitly supplies the automatic-follow-up tracker and selects a passing child after the parent. They prove the emitter contract, not that the normal watcher naturally schedules a green immediately after red. The separately identified natural watcher case exercises the real watcher scheduling path. No new blind-reader trial or reader-success-rate claim was made.

Red parent, green follow-up

Parent source: `/var/tmp/forge/parent-outcome-checks/kaocha-sublime-9414868553647549103/2026-09-08T20-57-12-364601335Z-0b670216-6013-4049-87d0-c6d3b60a45aa.events`.

```text
RUN-DONE {"receipt":"/var/tmp/forge/parent-outcome-checks/kaocha-sublime-9414868553647549103/2026-09-08T20-57-12-364601335Z-0b670216-6013-4049-87d0-c6d3b60a45aa.edn","omitted":0,"v":1,"parent":null,"candidate":"08e7c975577f72f7c3546431c6f47aa75e931dc9b0f0f4905b2cd45000d776f0","tests":1,"pending":0,"auto_followup":false,"wall_ms":36,"mode":"probe","coverage":"partial","outcome":"fail","shown":1,"skipped":4,"fail":1,"id":"2026-09-08T20-57-12-364601335Z-0b670216-6013-4049-87d0-c6d3b60a45aa","error":0,"sha256":"8e86221da62d4cdf2f800a42c5ae4dabb3102fc1dea8000589880df6edd7a1b7","pass":0,"assertions":1,"closure":{"selected":1,"total":2}}
```

Follow-up source: `/var/tmp/forge/parent-outcome-checks/kaocha-sublime-9414868553647549103/2026-09-08T20-57-12-413613574Z-02110262-4654-432e-ba5d-4129232de4c2.events`.

```text
RUN-DONE {"receipt":"/var/tmp/forge/parent-outcome-checks/kaocha-sublime-9414868553647549103/2026-09-08T20-57-12-413613574Z-02110262-4654-432e-ba5d-4129232de4c2.edn","omitted":0,"v":1,"parent":"2026-09-08T20-57-12-364601335Z-0b670216-6013-4049-87d0-c6d3b60a45aa","candidate":"08e7c975577f72f7c3546431c6f47aa75e931dc9b0f0f4905b2cd45000d776f0","tests":1,"pending":0,"auto_followup":true,"parent_outcome":{"outcome":"fail","fail":1,"error":0},"wall_ms":30,"mode":"watch","coverage":"complete","outcome":"pass","shown":0,"skipped":4,"fail":0,"id":"2026-09-08T20-57-12-413613574Z-02110262-4654-432e-ba5d-4129232de4c2","error":0,"sha256":"415fd3ba30af35cfec01a7b6239554b715df13a4fd714cc01a109794bddb7e65","pass":1,"assertions":1,"closure":{"selected":1,"total":2}}
```

Green parent, green follow-up

Follow-up source: `/var/tmp/forge/parent-outcome-checks/kaocha-sublime-4826203856100298943/2026-09-08T20-57-12-594547104Z-be9f88e9-2e5d-4306-9890-332ee1d26a3d.events`.

```text
RUN-DONE {"receipt":"/var/tmp/forge/parent-outcome-checks/kaocha-sublime-4826203856100298943/2026-09-08T20-57-12-594547104Z-be9f88e9-2e5d-4306-9890-332ee1d26a3d.edn","omitted":0,"v":1,"parent":"2026-09-08T20-57-12-549785537Z-6d61dc1e-a9d7-42df-ac2b-91aceaaabf60","candidate":"e8c9ec2e560dbe8aa49f4b26edbd317bb877bb39c58fc5089ac37a9cd8f5bebe","tests":1,"pending":0,"auto_followup":true,"parent_outcome":{"outcome":"pass","fail":0,"error":0},"wall_ms":29,"mode":"watch","coverage":"complete","outcome":"pass","shown":0,"skipped":4,"fail":0,"id":"2026-09-08T20-57-12-594547104Z-be9f88e9-2e5d-4306-9890-332ee1d26a3d","error":0,"sha256":"aa91ab43f55137187a3bc6c66861d155fd8af580a600ffad1896730efda54b8b","pass":1,"assertions":1,"closure":{"selected":1,"total":2}}
```

Parent receipt removed before follow-up completion

Follow-up source: `/var/tmp/forge/parent-outcome-checks/kaocha-sublime-4039466753613266372/2026-09-08T20-57-12-688958787Z-ac67ecb0-bc48-4949-957b-48b5efba7ab1.events`.

```text
RUN-DONE {"receipt":"/var/tmp/forge/parent-outcome-checks/kaocha-sublime-4039466753613266372/2026-09-08T20-57-12-688958787Z-ac67ecb0-bc48-4949-957b-48b5efba7ab1.edn","omitted":0,"v":1,"parent":null,"candidate":"4c2722697e2ab165103946ea571bcb7d64a8f032ff0869d0a796dbe34fe7c4b8","tests":1,"pending":0,"auto_followup":true,"parent_outcome":{"outcome":"unknown","fail":null,"error":null},"wall_ms":29,"mode":"watch","coverage":"complete","outcome":"pass","shown":0,"skipped":4,"fail":0,"id":"2026-09-08T20-57-12-688958787Z-ac67ecb0-bc48-4949-957b-48b5efba7ab1","error":0,"sha256":"22effbb75922f71c54049fab7070542d71fbe1873e4cf70ed05abe6f2ce4ec92","pass":1,"assertions":1,"closure":{"selected":1,"total":2}}
```

Parent with both a failure and an error

Follow-up source: `/var/tmp/forge/parent-outcome-checks/kaocha-sublime-18344750769826690634/2026-09-08T20-57-12-505353766Z-f695c4a8-3001-4fbc-9c41-380764eafe75.events`.

```text
RUN-DONE {"receipt":"/var/tmp/forge/parent-outcome-checks/kaocha-sublime-18344750769826690634/2026-09-08T20-57-12-505353766Z-f695c4a8-3001-4fbc-9c41-380764eafe75.edn","omitted":0,"v":1,"parent":"2026-09-08T20-57-12-460547171Z-81302b19-6723-46b4-99f7-805b60bf12a7","candidate":"e30d3622e4496461355cef259225a8c2acd57ac5c5f9196fcd3249fa5abd312d","tests":1,"pending":0,"auto_followup":true,"parent_outcome":{"outcome":"error","fail":1,"error":1},"wall_ms":29,"mode":"watch","coverage":"complete","outcome":"pass","shown":0,"skipped":4,"fail":0,"id":"2026-09-08T20-57-12-505353766Z-f695c4a8-3001-4fbc-9c41-380764eafe75","error":0,"sha256":"d08cea7baa8b1bab77f04d57c13484941ffc1af7307711ee5ed4f276c77a9b47","pass":1,"assertions":1,"closure":{"selected":1,"total":2}}
```

Naturally scheduled watcher follow-up (green parent)

Source: `/var/tmp/forge/kaocha-agent-v1/watch/target/kaocha-runs/2026-09-08T20-56-14-747571417Z-8509f993-14d8-41cc-8e73-1a0dde69023c.events`. The watcher witness independently asserted that FOLLOW still returned this record’s parent ID.

```text
RUN-DONE {"receipt":"/var/tmp/forge/kaocha-agent-v1/watch/target/kaocha-runs/2026-09-08T20-56-14-747571417Z-8509f993-14d8-41cc-8e73-1a0dde69023c.edn","omitted":0,"v":1,"parent":"2026-09-08T20-56-14-640622233Z-327fbb78-f492-4a7f-8163-c7f462c6dfbd","candidate":"24be613999042a78965a54e35b004e73e83ac98b2cbce95dc0a5727827c5e9ef","tests":1,"pending":0,"auto_followup":true,"parent_outcome":{"outcome":"pass","fail":0,"error":0},"wall_ms":59,"mode":"watch","coverage":"complete","outcome":"pass","shown":0,"skipped":0,"fail":0,"id":"2026-09-08T20-56-14-747571417Z-8509f993-14d8-41cc-8e73-1a0dde69023c","error":0,"sha256":"70c430e682517fc1034be0072a9b492b93f840e4e2de88dcab614599c05362ef","pass":1,"assertions":1,"closure":{"selected":1,"total":1}}
```

Timebox: started at 2026-09-08 20:51:57 UTC; completed within the requested 40 minutes. Two small commits; no unrelated repository changes retained.
