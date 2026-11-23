# Design note: what the weekly usage count may claim (after three failed reviews of PRs 43 and 44)

Rule applied (CLAUDE.md change rules, round 3 failed): stop pushing and write the design on one page for a fleet read.

## What was asked

Phase 1 step E: one line per week per operation, "calls, sessions, refusals, corrected fall-backs". The study's miner
counted any native Clojure edit after a call as a fall-back and over-counted (split-ns! 63% became 3% when read by
hand). The command was to count honestly, fast (under two minutes a week), from the Codex and Claude session stores.

## What three review rounds established

Every defect the two reviewers confirmed is one problem in different clothes: **attribution across undecodable text.**
Codex sessions record agent actions as JavaScript wrapper source (`tools.exec_command({...})`, `tools.apply_patch(p)`
with the patch in a variable, comments, string literals, several calls per wrapper) with one combined output; shell
commands join calls with `&&`, `;`, `||`, heredocs and comments. A line counter must decide, per record, which call ran,
which output belongs to it, and which file an edit wrote. Each round fixed the shapes the reviewers named and each round
the next review found another: payload order, substring "success", list-shaped commands, variable-held patches, comment
and string literals, control operators, heredoc bodies, shell comments, wrapper receipts attributed to the wrong call.
The parser grew from 70 to 158 lines and the shapes are not finite. Continuing round by round is the hill-climb the
testing doctrine forbids: a quantity ("fall-backs") whose definition is what the last fix made it.

## The decision

Report two numbers, never one, and let the second carry the uncertainty:

1. **Attributed**: calls, outcomes and edits where the record is a single decodable action. One tool call per record (a
   Claude Bash record with one command and no control operators; a Codex wrapper holding exactly one `tools.*` call
   with a literal argument and its own output). Outcome from that record's own output. A fall-back is counted only when
   the refusal or error AND the later native edit are both attributed records in the same turn.
2. **Unattributed**: every record the decoder cannot classify as above: multi-call wrappers, variable-held patches,
   control-operator chains, heredocs, comments, undecodable JSON. Counted per week as `unattributed_records`, with the
   subset that mentions clj-surgeon as `unattributed_surgeon_mentions`. Nothing in this bucket is guessed.

The weekly line then reads `op=ls attributed_calls=.. refusals=.. errors=.. fallbacks=..` plus one
`unattributed records=.. surgeon_mentions=..` line. Fall-backs are a lower bound by construction; the gap is visible.

## What this gives up, and why that is right

The September week would show fewer attributed calls than the study's 459 (perhaps a third), because the builders drove
the tool through Codex wrappers. That is the truth of the data: those weeks are not cleanly countable, and the
hand-audit already supplied their corrected rates. The command's job is the weeks ahead, where the trigger text and the
CLI route produce single-action records, and where a count nobody can dispute matters more than a large one.

## Size and shape

About 90 source lines for the parser (the decodable-record predicate, outcome, edit path, fold, aggregate, render) and
about 60 for the command (mtime lower bound only, secret filter before json, `-B`, child statuses all-or-nothing),
tests above the bar on literals, in two pull requests under the recorded exception. The existing branches are kept as
evidence of the rounds; the rewrite starts from main with the reviewers' fixtures as its first tests.

## Asked of the fleet read

Agree or amend: (a) two numbers, attributed and unattributed, no inference; (b) a fall-back needs both ends attributed;
(c) September is reported as mostly unattributed and the hand audit stands as its figure. Then one more build, one
independent read, one push.
