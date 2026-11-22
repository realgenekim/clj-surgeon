# Part B method

Run: `python3 docs/study-2026-11-22/bin/ledger.py --surgeon ~/src/clj-surgeon --records ~/src/clj-surgeon-records --out docs/study-2026-11-22`

Python standard library and sequential Git read commands only; no concurrent subprocesses. No source repository writes, checkout, reset, fetch, or commit. Writes are the seven Part B deliverables; other output files are hashed before and after. Python CSV consumers should set csv.field_size_limit(100000000): exhaustive members fields can exceed the default 128 KiB. Documents are data, never executed. Home paths and recognized credentials are redacted before output.

## Snapshot and counts

- surgeon HEAD `1f962abc1447c594c9fc76636273f7d81fc59e72`: 1094 observation files at HEAD; 10395 distinct all-history observation paths; 487 Markdown files at HEAD.
- records HEAD `3ddd00196f6537aaf711cabc3df86c777b8a2061`: 2305 observation files at HEAD; 10395 distinct all-history observation paths; 1119 Markdown files at HEAD.

- reachable_commits: 5994
- branches: 554
- refs_including_symbolic: 555
- symbolic_refs_excluded: 1
- named_families: 12
- raw_prefixes: 28
- unassigned_to_named_family: 20
- siblings: 38
- sibling_heads_outside_commit_universe: 0
- ledger_rows: 1205
- non_main_commits_on_multiple_branches: 4319
- excess_branch_commit_memberships: 878041
- cross_verdict_ambiguous_commits: 2788
- surgeon_observation_files_HEAD: 1094
- surgeon_observation_files_all_history: 10395
- surgeon_observation_markdown_HEAD: 487
- records_observation_files_HEAD: 2305
- records_observation_files_all_history: 10395
- records_observation_markdown_HEAD: 1119
- rows_won and routed: 2
- rows_lost and suspended: 8
- rows_inconclusive: 772
- rows_never measured: 423
- commits_won and routed: 0
- commits_lost and suspended: 0
- commits_inconclusive: 0
- commits_never measured: 1796
- commits_on main: 1396
- commits_unassigned: 2802
- Runtime: 66.941 seconds (script through table generation; final method write/hash check excluded).

## Commands and family rules

Every Git command uses `git --no-optional-locks -C REPO`. Read commands: `rev-parse --verify DEFAULT`/`rev-parse HEAD`; `log --all --format=%H%x09%ad%x09%P --date=iso-strict`; `for-each-ref --format=... refs/heads refs/remotes`; `symbolic-ref --quiet --short HEAD`; `rev-list --count main..HEAD`; `status --porcelain=v1 -z --untracked-files=all`; `show -s --format=%cI HEAD`; `ls-tree -r --name-only HEAD docs/observations`; `log --all --date-order --no-renames --root --format=@@%H %ad --date=short --raw --no-abbrev --diff-filter=AM -- SCOPES`; `cat-file --batch`. SCOPES: docs/observations, docs/experiments, docs/intent, docs/vision.md, docs/tech-tree.md, AGENTS.md, CLAUDE.md, skill.md, logs, records. Logs/records history metadata is inventoried; bodies are not used to classify verdicts.

Symbolic remote HEAD aliases excluded; otherwise retain both local and remote refs. Strip refs/heads or refs/remotes then origin/; raw prefix is before first slash or hyphen. Prefixes with fewer than three branches become misc; raw_prefix retained. Unassigned-to-named-family includes misc. Prefix before slash takes precedence over splitting a slash-free name on hyphen. First commit date is minimum committer timestamp in whole reachable ancestry, not branch creation; last date is tip committer timestamp. Ancestry closures reproduce rev-list main..branch and merge-base --is-ancestor; default main, else origin/main, master, origin/master. Siblings are actual Git directory matches clj-surgeon-*, including records. Dirty count includes individual untracked files; rename pairs count once.

## Observations and merge rules

Every observation path ever added/modified is retained, including fixtures and historical deletions. Representative text is the first AM blob in all-ref date-order traversal, with exact commit and blob identifiers; alternate variants are not combined. Filename date (else dated parent) takes precedence. Undated paths use earliest reachable addition date from the batched history (equivalent to per-path git log --all --diff-filter=A --format=%ad --date=short). The first leading # line is headline, even for scripts. Numeric extraction takes first line with numeric x/×/s/ms/seconds/minutes/hours/% or a number near wall/ratio. It does not establish that a numeric mention is an actual measurement. Verdict words are lexical indicators only. Series is the dated stem's topic, else dated parent directory topic, else basename.

Ledger merge: exact normalized topic (trailing result/report/preregistration/hypothesis removed) plus same calendar date merges across repositories. The captain's log supplies two separately labeled facets (clock composition and per-form comparison), since these are distinct requested anchors. Seven explicit anchor programs additionally combine clearly named cross-date receipts: namespace-split/plan-2-cellC, alias-migration/astra-alias-replication, fanout/fan-out/pair-1, per-form-writes, direct-cclsp-clients, strictly-better-evidence, bridge-wall-clock-ideal-program. Individual subcohorts remain in members/receipts. Differently named/different-date series otherwise remain distinct. Sibling copies remain separate experiments unless their full branch topic or directory name identifies an explicit anchor program; a common seat prefix is insufficient to merge a sibling into an aggregate family. Misc raw prefixes remain separate experiments. Family evidence linkage requires at least two topic tokens (except explicit anchors); broad bridge/cohort/arm families remain unattributed. Evidence-only links do not consume the original series row. Branch/sibling memberships can overlap; commit reconciliation below exposes this ambiguity. members and merge_rule expose every merge.

Hypotheses prefer topic-matched preregistrations, then earliest matching observation; take verbatim hypothesis/prediction/expectation line, or next paragraph if heading only. Otherwise label the first observation statement as nonexplicit hypothesis. No source means Unknown. Results keep first numeric candidate per observation, at most two receipts per row, untruncated; exact repo:path@commit provenance retained. This can include planned or incidental numbers, explicitly marked as a limitation. Source text is not treated as authoritative instructions.

## Verdict gates and known anchors

No numeric candidate -> never measured; otherwise inconclusive. These describe matched evidence availability, not proof of actual experimental fate. A direct Verdict/Decision/Disposition line saying lost/suspended/retired/falsified can yield lost and suspended. Winner promotion is conservative: only reviewed anchor receipt plus routing/doctrine witness yields won and routed. Doctrine sources are representative reachable AGENTS.md, CLAUDE.md, skill.md, docs/vision.md, docs/tech-tree.md, routing intent files, cited at exact commits. This does not prove installed-seat parity. cclsp retirement is a surface/security decision, not a performance loss: scan counts are labeled accordingly. Captain's log performance loss is distinct from its model/tool time composition.

- alias-migration: receipt and doctrine verified
- bridge-wall-clock-ideal-program: receipt and doctrine verified
- direct-cclsp-clients: receipt and doctrine verified
- fan-out: receipt and doctrine verified
- namespace-split: receipt and doctrine verified
- per-form-writes: receipt and doctrine verified
- strictly-better-evidence: receipt verified

## Commit reconciliation and limits

Universe: every commit reachable by --all in surgeon, counted once. Main ancestry has precedence. For each remaining commit, collect verdict classes of containing family/sibling rows. One class -> that class; multiple classes or uncovered -> unassigned. unique_commits_exclusive sums to the universe. Branch sums and family_sibling_union_nonexclusive deliberately overlap and are nonadditive. The number of distinct non-main commits on more than one branch and extra branch memberships are reported separately. Sibling HEAD outside the universe has unknown attribution and is not fabricated into the total. Ledger commits sum main..branch per ref for family rows, use the sibling union for sibling/anchor rows, and are zero for unattached observation series. Family commits sum main..branch per ref; sibling histories are not added to raw family sums. Branch containment cannot show which experiment authored a shared ancestor.

Brief counts are reference expectations, not forced outputs: actual trees include nested fixture files, and all-history counts can be larger. Git has no durable branch creation timestamps. Broad topic attribution, overlapping experiments, nonnumeric receipts, alternate historical variants, and installed routing state can remain unknown. Numeric candidates may be plans, baselines, or incidental counts: inspect source before using a row as a benchmark claim. No semantic full-text model validates every receipt. This ledger is an auditable index, not independent acceptance of historical claims.

Git invocation counts: cat-file=2, for-each-ref=1, log=3, ls-tree=2, rev-list=38, rev-parse=118, show=38, status=38, symbolic-ref=38.
Files written: branches.tsv, siblings.tsv, observations.tsv, ledger.tsv, ledger.md, commits_by_verdict.tsv, METHOD-B.md. Script: bin/ledger.py.
