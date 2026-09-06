# Sol caller report: mission ledger usability probe

Date: 2026-09-05 UTC

## Outcome and clock

- Timed first command began at 19:02:11.042277103 UTC.
- No receipt reached :verified. All four schema-valid apply attempts (M-8 through M-11) stopped before mutation with helper-extraction-verification-preflight-unavailable, configured_profiles [], and admitted_profiles [].
- The timestamp after the last apply refusal was 19:08:22.020944290: 370.978667187 s (6m10.979s) from the first command.
- PRE restoration and the independent recursive diff completed at 19:08:54.426378588: 403.384101485 s (6m43.384s) from the first command.
- Model returns: 24 through the last apply refusal; 27 through undo refusal, PRE restoration/diff, and checkout-status confirmation. There is no first-command-to-verified return count because verification was never reached.
- Final diff -ru expected-pre/src ws/src exited 0 with no output. git status --short in the checkout was also empty.

## Exact commands in order and wall times

The first setup attempt requested ws as its working directory before ws existed, so the process did not start (0.0 s). It contained mkdir, the requested bb command, marker apply_patch, find, and git status. I then ran:

1. Fixture setup (0.1 s tool wall):

~~~sh
set -e
mkdir -p /var/tmp/forge/sol-mission-fx/ws
bb -cp test:src -e "(require 'clj-surgeon.helper-extraction-fixture) (doseq [{:keys [file pre]} (clj-surgeon.helper-extraction-fixture/files :happy)] (let [f (java.io.File. \"/var/tmp/forge/sol-mission-fx/ws\" file)] (.mkdirs (.getParentFile f)) (spit f pre)))"
~~~

2. In ws, apply_patch added these exact files, followed by find . -maxdepth 5 -type f -print (0.1 s):

~~~edn
;; deps.edn
{:paths ["src" "test"]}
;; .clj-surgeon.edn
{:verification-profiles {"mission-proof" {:commands [["/bin/true"]]}}}
~~~

3. First timed mission call (6.26 s, exit 0):

~~~sh
date -u +%T.%N
/usr/bin/time -f 'WALL_SECONDS=%e EXIT=%x' ./bin/mission --help
~~~

4. First guessed plan (5.15 s, exit 1):

~~~sh
./bin/mission plan --state-home /var/tmp/forge/sol-mission-fx/state --workspace /var/tmp/forge/sol-mission-fx/ws --spec-file - <<'EDN'
{:verb "helper_extraction"
 :request {:source "src/acid/web/http.clj"
           :destination-ns "acid.web.response"
           :helpers ["html-response" "see-other" "text-response" "plain-not-found" "json-response" "with-etag"]
           :alias-policy ["response" "resp"]}
 :profiles {"mission-proof" {:commands [["/bin/true"]]}}}
EDN
~~~

5. Tried the global option before the verb (4.93 s, exit 0); it printed help:

~~~sh
./bin/mission --state-home /var/tmp/forge/sol-mission-fx/state plan --workspace /var/tmp/forge/sol-mission-fx/ws --spec-file - <<'EDN'
[same literal EDN as command 4]
EDN
~~~

6. Added request workspace_root, retaining guessed source, destination-ns, helpers, and alias-policy (4.81 s, exit 0): M-1 blocked.

7. Used only disclosed closed fields, initially op "move", from "acid.web.http", to "acid.web.response", scope "project", expect with alias policy, and verification "mission-proof" (4.87 s): M-2 blocked.

8. Changed from to {:file "src/acid/web/http.clj"} and to to {:namespace "acid.web.response"} (4.93 s): M-3 blocked.

9. Changed to to {:lib "acid.web.response" :alias_policy ["response" "resp"]} and expect to {} (4.90 s): M-4 blocked.

10. Changed scope to {:paths ["src/**/*.clj"]} (5.06 s): M-5 blocked.

11. Changed verification to {:profile "mission-proof"} (4.98 s): M-6 blocked.

12. Omitted expect (5.13 s): M-7 blocked because the target existed.

Commands 6–12 all used this exact shell envelope:

~~~sh
/usr/bin/time -f 'WALL_SECONDS=%e EXIT=%x' ./bin/mission plan --state-home /var/tmp/forge/sol-mission-fx/state --spec-file - <<'EDN'
[the exact EDN variant described above]
EDN
~~~

The final literal request used from command 12 onward was:

~~~edn
{:verb "helper_extraction"
 :request {:workspace_root "/var/tmp/forge/sol-mission-fx/ws"
           :op "move"
           :from {:file "src/acid/web/http.clj"}
           :to {:lib "acid.web.response" :alias_policy ["response" "resp"]}
           :helpers ["html-response" "see-other" "text-response" "plain-not-found" "json-response" "with-etag"]
           :scope {:paths ["src/**/*.clj"]}
           :verification {:profile "mission-proof"}}
 :profiles {"mission-proof" {:commands [["/bin/true"]]}}}
~~~

13. Show M-7 (0.04 s, exit 0):

~~~sh
./bin/mission show M-7 --workspace /var/tmp/forge/sol-mission-fx/ws --state-home /var/tmp/forge/sol-mission-fx/state
~~~

14. Resume M-7 (5.06 s, exit 1):

~~~sh
./bin/mission resume M-7 --workspace /var/tmp/forge/sol-mission-fx/ws --state-home /var/tmp/forge/sol-mission-fx/state
~~~

15. Check destination size (0.1 s): wc -c /var/tmp/forge/sol-mission-fx/ws/src/acid/web/response.clj. Result: zero bytes.

16. Preserve the zero-byte PRE placeholder without deleting it (0.00 s each):

~~~sh
mkdir -p /var/tmp/forge/sol-mission-fx/evidence
mv /var/tmp/forge/sol-mission-fx/ws/src/acid/web/response.clj /var/tmp/forge/sol-mission-fx/evidence/response.clj.empty-pre
~~~

17. Plan the final request (5.47 s, exit 0): M-8 ready.

18. Apply M-8, then date -u +%T.%N (4.75 s; CLI exit 0 despite failed receipt):

~~~sh
./bin/mission apply M-8 --workspace /var/tmp/forge/sol-mission-fx/ws --state-home /var/tmp/forge/sol-mission-fx/state
~~~

19. From workspace CWD, attempt recovery (4.90 s, exit 1):

~~~sh
/home/forge/src/clj-surgeon-fence2/bin/mission resume M-8 --workspace /var/tmp/forge/sol-mission-fx/ws --state-home /var/tmp/forge/sol-mission-fx/state
~~~

20. From workspace CWD, plan the final request via the absolute executable (5.15 s): M-9 ready.

21. From workspace CWD, apply M-9 and timestamp (4.83 s; CLI exit 0 despite failed receipt).

22. Force JVM user.dir while planning (0.61 s, exit 1 before mission load):

~~~sh
JAVA_TOOL_OPTIONS='-Djava.io.tmpdir=/var/tmp/forge -Duser.dir=/var/tmp/forge/sol-mission-fx/ws' /home/forge/src/clj-surgeon-fence2/bin/mission plan --state-home /var/tmp/forge/sol-mission-fx/state --spec-file - <<'EDN'
[final literal request]
EDN
~~~

23. Plan with conventional explicit config environment names (4.96 s): M-10 ready.

~~~sh
CLJ_SURGEON_CONFIG=/var/tmp/forge/sol-mission-fx/ws/.clj-surgeon.edn CLJ_SURGEON_CONFIG_FILE=/var/tmp/forge/sol-mission-fx/ws/.clj-surgeon.edn ./bin/mission plan --state-home /var/tmp/forge/sol-mission-fx/state --spec-file - <<'EDN'
[final literal request]
EDN
~~~

24. Apply M-10 with the same environment and timestamp (5.08 s; CLI exit 0 despite failed receipt).

25. Plan with explicit CLI config (4.96 s): M-11 ready.

~~~sh
./bin/mission plan --state-home /var/tmp/forge/sol-mission-fx/state --config /var/tmp/forge/sol-mission-fx/ws/.clj-surgeon.edn --spec-file - <<'EDN'
[final literal request]
EDN
~~~

26. Apply M-11 with explicit CLI config and timestamp (4.90 s; CLI exit 0 despite failed receipt):

~~~sh
./bin/mission apply M-11 --workspace /var/tmp/forge/sol-mission-fx/ws --state-home /var/tmp/forge/sol-mission-fx/state --config /var/tmp/forge/sol-mission-fx/ws/.clj-surgeon.edn
~~~

27. Explicit undo (4.72 s, exit 1):

~~~sh
./bin/mission undo M-11 --workspace /var/tmp/forge/sol-mission-fx/ws --state-home /var/tmp/forge/sol-mission-fx/state
~~~

28. Restore and independently compare PRE (0.00, 0.00, 0.02, 0.00 s):

~~~sh
cp /var/tmp/forge/sol-mission-fx/evidence/response.clj.empty-pre /var/tmp/forge/sol-mission-fx/ws/src/acid/web/response.clj
mkdir -p /var/tmp/forge/sol-mission-fx/expected-pre
bb -cp test:src -e "(require 'clj-surgeon.helper-extraction-fixture) (doseq [{:keys [file pre]} (clj-surgeon.helper-extraction-fixture/files :happy)] (let [f (java.io.File. \"/var/tmp/forge/sol-mission-fx/expected-pre\" file)] (.mkdirs (.getParentFile f)) (spit f pre)))"
diff -ru /var/tmp/forge/sol-mission-fx/expected-pre/src /var/tmp/forge/sol-mission-fx/ws/src
date -u +%T.%N
~~~

29. git status --short (0.01 s, exit 0, no output).

All mission calls were wrapped by /usr/bin/time -f 'WALL_SECONDS=%e EXIT=%x'; wrappers are omitted on repeated listings only for readability.

## What I wanted to type vs. what I had to type

I wanted one compact command mirroring the task—source, destination, six helper names, alias policy, scope, and proof profile—then apply and resume/undo. I had to reverse-engineer a nested closed EDN schema through seven plan refusals, discover that the exact fixture command creates a zero-byte occupied destination, preserve that placeholder manually, create four fresh missions because failures are terminal, and guess at config-discovery workarounds that were silently ignored.

The ready dossier itself felt good: it clearly quantified 31 caller files, 66 sites, 33 changed files, retained sites, caller partitions, snapshot hashes, and the exact next action. The ledger felt bad after refusal: it accumulated blocked/failed missions but offered no resolution transition, next_call, or way to repair a request/profile in place.

## Refusals and whether they named the decision

| Command / mission | Refusal | Named decision? |
|---|---|---|
| First guessed plan | Raw workspace_root exception | No |
| Global option before verb | Printed help, exit 0 | No refusal object |
| M-1 | helper-extraction-unknown-field | Yes: which closed field carries the information |
| M-2 | from must be {file} | No; question nil |
| M-3 | to must be {lib, alias_policy} | No; question nil |
| M-4 | scope.paths must be a glob array | No; question nil |
| M-5 | verification must be {profile} | No; question nil |
| M-6 | optional expect must be {caller_files} | No; question nil |
| M-7 plan | helper-extraction-target-exists | Yes: which namespace the helpers move to |
| M-7 resume | illegal blocked → applied, legal [], next_call nil | Yes: what to do with blocked mission |
| M-8 apply | verification-preflight-unavailable | Yes: which admitted profile proves the write |
| M-8 resume | illegal failed → applied | Yes: what to do with failed mission |
| JVM user.dir attempt | Clojure classpath exception | No |
| M-9/M-10/M-11 apply | same verification refusal, configured profiles [] | Yes: which admitted profile proves the write |
| M-11 undo | illegal failed → undone, legal [], next_call nil | Yes: what to do with failed mission |

All four failed applies returned process exit 0 even though their receipts said :ok false and :committed false and moved the mission to :failed.

## Reach-for verdict

No—I would reach for mission over rg+apply_patch on neither this 33-file extraction nor a one-site edit in its current form, because the excellent dossier never converted into an admitted verified write and failed missions were unrecoverable.
