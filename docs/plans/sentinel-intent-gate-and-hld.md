# Sentinel traceability gate and flagship HLD coverage

The sentinel's 50 PERF-SENT rows have a bidirectional shell witness audit,
but the ordinary suites never execute it. The HLD also lacks architectural
accounts of alias migration, feature thread and relation census. This batch
closes audit gap 3 and appends the missing HLD coverage; mission-ledger
implementation and its linked intents belong to the next batch.

The existing HLD promise to gate durable intent flows through the operation
contract design to MCP-OP-TRACE-006. Extract the shell audit into its own Make
target and require it from `make mcp-test`, the ordinary merge gate. Keep the
full sentinel behavioral target separate: it launches cold runtimes. The JVM
fast lane forbids child processes, so its witness checks the Make dependency
and executable recipe as data using the existing runner-membership parser.
No JVM lane membership changes, source-policy broadening or debt exemptions
are needed. WTL and OP-ALG already use the widened, exact per-ID debt gate;
report their rows, existing missing pairs and mutation failures explicitly.

Red evidence: the wiring witness fails on the original Makefile; a scratch
copy with one sentinel test marker removed fails the shell audit. After the
change, drive the actual merge-gate prerequisite with the same sabotage and
retain its failure, restore it, and retain the ordinary lane's success line.
Also inject an unknown marker to exercise the reverse direction. These are
traceability checks, not execution of the performance sentinel or a battery.

The scratch probe found the old audit decoded only 49 of the 50 rows: its
single-topic regex omitted PERF-SENT-LEDGER-ROOT-001 on both sides and accepted
its deletion. Extend both patterns to multi-part topics and retain a shell
self-test for missing simple/multi-part and unknown multi-part IDs on the same
Make target. The self-test itself fails against the original audit script.

Append three 4–8 sentence operation sections with existing enforcing IDs and
caller receipt fields, disambiguate read mission from MISSION LEDGER with
pointers, and add references for the audit's 15 omitted or thin leaves.
Do not rewrite existing HLD prose or ratify unimplemented leaves by citation.

Acceptance: focused intent-contract namespace, operation Prolog oracle,
bare fast alias, `make mcp-test`, changed-Clojure formatting and paved kondo.
Recompute manifest counts from source. Retain commands, tails, exit codes,
diff stat and doubts under `/var/tmp/forge/lid-batch/`, then commit on the
current branch with the requested environment author and coauthor trailers.
Gene's bounded brief authorizes the design → requirements → red tests →
implementation → documentation → verification cascade through that commit.
