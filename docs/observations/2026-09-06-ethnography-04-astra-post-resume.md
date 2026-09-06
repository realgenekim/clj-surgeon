# Ethnography 04 — Astra after the MCP resume (08:22Z, read-only, ~/bin/ethno on pane forge-anvil:1.0, last 3000 lines)

== ethno 2026-09-06T08:22:05Z pane=forge-anvil:1.0 last 3000 lines
== action verbs
     85 Ran
     30 Explored
     25 Interacted
      1 Edited
== commands run (first token)
     41 python3
     10 cat
      4 slot
      4 git
      4 clj-surgeon
      3 tmux
      3 tail
      3 bb
      2 rg
      2 mkdir
      2 date
      1 red-tmp
== surgeon reads/edits
   clj-surgeon CLI: 94   :cat/:ls: 3   bin/mission: 1   raw sed/rg/grep: 2   native edits (Edited): 1
== apparatus share
   77 of 85 Ran actions are apparatus (python/git/ps/bb/ls/cat/tail/mkdir/stat) = 90%
== ledger /home/forge/.clj-surgeon/events.jsonl
   kinds: {'mission-plan': 74, 'mission-apply': 56, 'mission-undo': 39, 'mission-verify': 55, 'mission-commit': 49, 'mission-fallback': 1, 'mcp-call': 22}
   mission rows with a verb: 202  ok 174  refused 28  reasons {'typist-incomplete-file-set': 3, 'mission-refused': 9, 'mission-verification-profile-not-admitted': 4, 'forms-protected-syntax': 2, 'mission-snapshot-stale': 2, 'mission-exception': 4, 'typist-receipt-dir-required': 4}
   rows without a verb (witness/test runs): 94

## Reading
- His hour is APPARATUS work: 85 Ran actions, 90% python/git/bb/cat — running the maven epoch, the replays, the report. One native edit. The full surface was available for 25 minutes of this window; he used it for one real repair (edit_clojure: 1 refusal, then 2 edits in 696 ms) and a handful of inspects. Not a rejection — a period with almost no edits to make.
- The refusal he hit (two forms in a require replacement → invalid-intent-form) is the kind the tool must answer with the fix in the refusal text (inb-e68905).
- Ledger totals are mostly missions from his cells (74 plan / 56 apply / 55 verify / 49 commit, 28 refusals — typist-incomplete-file-set 3, mission-refused 9, verification-profile-not-admitted…); the MCP verbs are 22 rows including this seat's three smokes.
- Rev 2 consequence: the next cut should come from the refused mission reasons above (they are HIS last refused calls), not from a backlog. verification-profile-not-admitted and typist-incomplete-file-set are the two to open with him.
