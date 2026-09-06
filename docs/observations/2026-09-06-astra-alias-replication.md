# Astra: whole-repository alias migration, fresh caller replication

2026-09-06, final independent decision19:40Z. **The tool was1.38x faster on median complete verified task wall; the preregistered1.5x prediction was missed.** All six controls and all six paired actors were correct. Three tool arms each made one successful whole-intent migration, with no refusal, retry or native source fallback.

| Arm | Runs | Full-wall median | Descriptive p90* | Correct |
|---|---:|---:|---:|---:|
| Native controls |6|44.276s|50.074s|6/6|
| Paired native |3|42.870s|43.273s|3/3|
| Paired Surgeon |3|31.074s|36.875s|3/3|

*Nearest-rank p90 equals the maximum at these small sample sizes; this is not a tail-latency estimate. Full wall is parent dispatch through actor completion, capture/cleanup, independent six-check proof and final automated scope audit. Later independent human-style agent review is outside that arm clock and inside preparation-to-decision. This task required no Git commit or publication, matching the original experiment.

Pairs N/T, seconds:42.870/36.875,43.273/30.649,37.854/31.074. Pairwise ratios1.163x,1.412x,1.218x; ratio of medians1.3796x. Actor-only medians42.390/30.575s. Fresh controls had sampleSD3.430s; the11.796s median full-wall gap exceeds the preregistered2SD threshold6.859s. Controls were serial and pairs concurrent, so this is an operational threshold, not a formal significance claim with identical scheduling variance. All three pairs favored the tool, but none reached1.5x.

## What was tested

Reused E-CALLER's synthetic `fanout-k1/repo-21` at `65fe39a9071083f478ed091ab64ebdf05c02abbd`, canonical source and frozen six-check proof. Each fresh actor received the same114-file repository:100 source namespaces,21 targets and63 qualified uses. The task moved references from `acid.fanout.store/find-event` to `acid.fanout.store2/fetch-event`, preserving unrelated local names, comments, strings, metadata, reader discards and other namespace references. Alias policy was supplied, but **all21 targets could use store2 and zero collisions occurred**. This does not test actual collision resolution or a new real-repository adoption case.

All actors used pinned Codex0.153.3 and individually recorded `gpt-6-astra/medium`. Native had unrestricted ordinary scripting/read/patch choices. Tool was directed to use the existing `alias_migration` intent with native fallback permitted and charged. It received the valid current nested field names, not a reference solution. Both arms owed the same existing load and behavioral proof; Surgeon did not claim that proof was included in its migration receipt.

The shared8171 service was operator-attested rebuilt at `e8076379`; its worktree HEAD, healthy response and actual preflight were retained. Healthz itself does not attest a loaded commit, so this is not cryptographic runtime identity. Each actor ran through `SLOT_OWNER=astra ~/bin/slot -t`; paired launches alternated N/T first. Fable's extract pair was explicitly permitted concurrently. Slot timing is separately retained; no exclusive quiet marker was used or left behind. Do not pool these walls with historical serial experiments.

## Correctness and useful mechanisms

Every output passed the original six checks: exact21-target file set, canonical form equality,106 protected regions intact,100 namespace loads,21tests/147assertions with zero failures/errors, and no retired references or alias-shadowing errors. Direct canonical source comparison additionally returned zero. Independent saved-trace review covered every paired actor and confirmed actual tool commits and no native fallback. Protected non-source bytes and source modes were preserved; tool extras were the returned detail EDN and its manifest. Inherited inventory excludes `.git` and is not a census of arbitrary special filesystem objects.

Native actors used Python batch scripts, not21literal patches. All paired native and tool actors used three outer actions. Tool callers discovered the skill/catalog, invoked one migration, then ran the required load/tests. Actual migration service times were423,474 and438ms. They did not mistake atomic source commit for Git publication or assume it supplied unrequested tests.

The tool replaced owner/source reading and script construction; it did not remove all actor turns or proof. This is the useful square: a known namespace/Var intent lets the kernel enumerate and rewrite resolved sites while the actor states the policy. It is not evidence that homoiconicity alone makes general feature work faster. Our earlier complete-feature losses remain losses.

## Preparation, corrections and accounting

The historical E-CALLER2.22/3.66x headline measured emitted characters, not wall. Its shell `diff | head` wrapper could hide a nonzero diff status. We reused the underlying six-check scorer but directly checked both proof and diff exit codes. The old task's contradictory blanket “no old var name” clause was corrected to preserve protected decoys. This is a new prospective replication, not a regrade of old results.

One preflight call using obsolete flat fields refused without mutation; the refusal supplied the current nested schema. Corrected preflight migrated21files/63sites in0.595s and passed all proof plus canonical bytes. The unedited baseline failed. These setup calls are not actor attempts or speedups. No paid actor was rerun or discarded.

Preparation from19:23Z through final independent decision took1,053.423s (17.56min), including six controls and review. This misses the desired ten-minute learning-loop target. Actor token totals: controls670,978; paired native298,155; paired tool270,353; total1,239,486. Cached input and reasoning are subsets; dollar cost is unknown. The observer/builder token budget is additional, not included in these actor totals.

Retained authority: `/var/tmp/forge/astra-alias-replication-fx/`: preregistration, manifest SHA `275e52fa8f7ffeac82670f5be0039515d205553b1c6a11a863bc9a1faf3be8d3`, per-arm result/proof/trace receipts, pair launch/slot records, `native-floor.json`, final `summary.json`, independent reviews and read-only usage study. The ledger block is closed with both walls and the missed prediction explicitly stated. All runtime is terminal. No further run is needed to report this result.
