**Astra consult — preparing MCP/main for public main**

Prepared 2026-09-10T15:53:35+00:00. This is an internal decision report, not public release copy. Repository inspection was read-only; the only file written is `/var/tmp/forge/plan2/cellC/astra-merge-main-report.md`. No commits, branch changes, fetches, pushes, experiments, test runs, service calls, or port activity were performed. The instruction “NO commits” governs the conflicting “Deliver, committed” phrase. Existing test and review results below are historical evidence, not tests executed by this consult.

**Recommendation: begin a private preparation lane this week; do not release MCP/main wholesale.** The platform and the two narrowly routed operations deserve publication preparation, but none of their present implementations has the documented months of dogfood Gene required. The candidate tree also fails the requested public-repository hygiene test. A stable tag and a sublime state score of 9 do not change either conclusion.

Gene’s September 10 request, “Come up with a plan to start merging your main,” authorizes this plan. It does not revoke the September 4 freeze. [CLAUDE.md](/home/forge/src/clj-surgeon-astra-consult/CLAUDE.md), under “MAIN IS FROZEN,” forbids code and documentation landings alike until Gene names a tested, months-dogfooded winner. It also says Gene eventually fast-forwards public main himself. The mayor should own candidate integration and the release checklist; Gene must either perform the final public update or explicitly delegate that single update to the mayor. The mayor has no standing exception to the freeze.

All repository-relative citations below refer to the inspected tree at `59d8bc0c`. Absolute paths into `clj-surgeon-records` and [/var/tmp/forge](/var/tmp/forge) identify separately retained evidence; their presence here does not make them distributable public evidence. No remote freshness or remote visibility was checked.

The requested Git commands returned:

```text
git rev-list --count origin/main..origin/MCP/main
2562

git diff --stat origin/main origin/MCP/main
1388 files changed, 287981 insertions(+), 1367 deletions(-)

origin/main     affdfe3fd59a63b3fbbb50da16b82509fafea907
origin/MCP/main 59d8bc0cad8886a028c24e8ffd52e4e5d82185c1
stable/2026-09-10^{commit} = origin/MCP/main
```

There are 2,387 non-merge commits and 175 merge commits in the range. `origin/main` is an ancestor of `origin/MCP/main`, so a fast-forward is technically possible. It would import the entire accumulated history. Public main’s last commit is dated September 4; side-branch commits newly reachable through MCP/main go back to September 2. The annotated September 10 tag names internal records, a seat identity and a session link; its annotation is itself publication material to review.

I examined the commit subjects by product, gate, census, installation, intent and records paths, alongside the full divergence statistics. The range is not one feature branch. The following areas overlap; only the explicitly stated path totals are additive within their own scope.

| Area | What diverged; useful commit subjects/anchors | Publication disposition |
|---|---|---|
| Routed operations and receipts | `6b5252c1` introduces `alias_migration`; `913020c8` compiles whole namespace partitions; `827a751a` moves receipt artifacts outside workspaces; `a9da4344` adds background proof; `1e57be64` repairs comment identity; `b2beec26` adds residual review facts. See [src/clj_surgeon/alias_migration.clj](/home/forge/src/clj-surgeon-astra-consult/src/clj_surgeon/alias_migration.clj), [src/clj_surgeon/namespace_split.clj](/home/forge/src/clj-surgeon-astra-consult/src/clj_surgeon/namespace_split.clj), [src/clj_surgeon/receipt_artifacts.clj](/home/forge/src/clj-surgeon-astra-consult/src/clj_surgeon/receipt_artifacts.clj), and the alias/helper-extraction intent leaves. All changed [src/clj_surgeon](/home/forge/src/clj-surgeon-astra-consult/src/clj_surgeon) paths together: 82 files, +40,357/-584. | Prepare alias and the exact cold split contract with their shared kernel and witnesses. Keep background-proof and other expanded contracts separately qualified. Fix artifact paths first. |
| Gate, coordinator, box-wide slots | `f2d93aba` is the distinct **product patch-admission gate**. `e8a0093c` introduces the battery coordinator; `ebaf7aec` generalizes it to landing; `3def037f` adds box admission; `2dfbc286` replaces the pathname semaphore with Linux abstract sockets. See [test/clj_surgeon/battery_parallel_runner.clj](/home/forge/src/clj-surgeon-astra-consult/test/clj_surgeon/battery_parallel_runner.clj), [test/gate_slot.py](/home/forge/src/clj-surgeon-astra-consult/test/gate_slot.py), [docs/plans/parallel-landing-gate.md](/home/forge/src/clj-surgeon-astra-consult/docs/plans/parallel-landing-gate.md). | Good platform candidate, after portable proof and receipt-policy repair. The repository gate is not the installed `ship`/`land` system. Do not transfer its speed result to `admit_clojure_patch`. |
| Censuses | The product `relation_census` begins at `7244141e`, with [census_discovery.clj](/home/forge/src/clj-surgeon-astra-consult/src/clj_surgeon/census_discovery.clj) and [census_pool.clj](/home/forge/src/clj-surgeon-astra-consult/src/clj_surgeon/census_pool.clj). Separately, `c0901e10`, `6a9fddca`, `da439651` and `e6ecd815` derive test and intent membership, name each deftest, prevent gate-driven regeneration and close fence findings. See [test/clj_surgeon/deftest_census.edn](/home/forge/src/clj-surgeon-astra-consult/test/clj_surgeon/deftest_census.edn), [lane_manifest.clj](/home/forge/src/clj-surgeon-astra-consult/test/clj_surgeon/lane_manifest.clj), [runner_membership.clj](/home/forge/src/clj-surgeon-astra-consult/test/clj_surgeon/runner_membership.clj), and [src/clj_surgeon/mcp_intent_contract.clj](/home/forge/src/clj-surgeon-astra-consult/src/clj_surgeon/mcp_intent_contract.clj). | Include membership enforcement with the platform. Relation census is a bounded review aid, not an idempotency proof or a demonstrated performance winner. |
| Install and Darwin | `d99ae2db`, `f17d7150`, `83cf6bc2`, `3585c7e5`, `064bc8bc`, `6067f938`: portable lock, path-socket backend, capacity fallback, prerequisite preflight, Darwin mount authority and two parser-fence repairs. [Makefile](/home/forge/src/clj-surgeon-astra-consult/Makefile) overall +547/-17; new [bin/install-preflight](/home/forge/src/clj-surgeon-astra-consult/bin/install-preflight), [bin/with-lock](/home/forge/src/clj-surgeon-astra-consult/bin/with-lock); [docs/install/skiff.md](/home/forge/src/clj-surgeon-astra-consult/docs/install/skiff.md). | Prepare with platform, but native macOS full-gate proof is absent. The clean-prefix Linux install passed; its full test did not. |
| Intent, specifications and tests | 40 changed [docs/intent](/home/forge/src/clj-surgeon-astra-consult/docs/intent) files, +7,276/-12; [docs/high-level-design.md](/home/forge/src/clj-surgeon-astra-consult/docs/high-level-design.md) +352; 21 changed plans. The 140 changed [test/clj_surgeon](/home/forge/src/clj-surgeon-astra-consult/test/clj_surgeon) files add 61,767 lines; 122 changed [test-fixtures](/home/forge/src/clj-surgeon-astra-consult/test-fixtures) files add 38,183. These span transactions, memory bounds, shell readers, helper extraction, feature threads and missions as well as the routed verbs. | Public contracts and direct witnesses travel with each included feature. Removing intent prose can break the executable intent audit. Separate policy approval from mechanical file deletion. |
| Records already on code ancestry | 832 changed [docs/observations](/home/forge/src/clj-surgeon-astra-consult/docs/observations) files, +109,821/-6, about 38% of added text. Includes captain’s logs, resume notes, verdicts, cohort requests/results, embedded source snapshots and [battery-ledger.edn](/home/forge/src/clj-surgeon-astra-consult/docs/observations/battery-ledger.edn). `1f86717a` records the September 9 ref split; four later battery-receipt commits still land on the code ref by design. | Keep raw internal records out of the proposed public delta; publish only separately approved, sanitized evidence. Ref splitting stopped routine future log traffic, not the inherited history or the ledger dependency. |
| Seat apparatus and remaining experiments | [bench/anvil-arms](/home/forge/src/clj-surgeon-astra-consult/bench/anvil-arms) alone adds 22 files and 7,576 text lines; [bin](/home/forge/src/clj-surgeon-astra-consult/bin) adds 34 files and 7,370 lines, including the typist provider runner. [resources/clj-surgeon-agent-routing.md](/home/forge/src/clj-surgeon-astra-consult/resources/clj-surgeon-agent-routing.md), global skill installers, [docs/two-vm-plan.md](/home/forge/src/clj-surgeon-astra-consult/docs/two-vm-plan.md), mission experiments and provider-cost tooling carry internal operating assumptions. A Python bytecode cache is tracked. | Exclude from the initial public scope. Makefile targets, runtime dependencies, fixtures and audits must be reconciled with the exclusion, not merely hidden from README. |

**What is publishable now.** No proposed change is authorized to land on public main now. As engineering material ready to enter preparation, the bounded gate/coordinator, tree-derived census enforcement, portable installation work, alias migration, cold exact-contract namespace splitting, transaction safeguards and truthful receipt contracts have useful implementation and review evidence. Their eligibility is conditional on the gates below. “Ready to prepare” is not “release-approved,” “months-proven,” or “safe to publish the current branch.”

The current whole tree is not publishable under Gene’s requested test. The obvious case-insensitive substring grep (`forge|anvil|7906|/var/tmp/forge|sol-yolo|records-push|ship`) matches **8,150 lines in 1,178 files**. Of those files, 815 changed in this divergence and 363 are unchanged from public main. The same scan on the public-main snapshot finds 2,439 lines in 395 files. This is partly inherited exposure; it is not all a new leak caused by MCP/main. The appendix lists every matching file and matching line ranges, including false positives such as `forget`, `forged`, `ownership`, and `mothership`. Counts are discovery results, not counts of secrets or defects.

The release blockers are concrete:

- **Executable seat roots remain.** [src/clj_surgeon/receipt_artifacts.clj:10](/home/forge/src/clj-surgeon-astra-consult/src/clj_surgeon/receipt_artifacts.clj:10) sets `*artifact-root*` to [/var/tmp/forge](/var/tmp/forge); namespace split calls that artifact layer, and the alias receipt path uses it too. This can refuse or write into the wrong user’s area on a normal machine. Use an explicit configurable, user-private durable root outside the workspace, with confinement, ownership, restart and undo-retention witnesses. Do not solve it by creating [/var/tmp/forge](/var/tmp/forge) on every public machine. [src/clj_surgeon/memory_battery_runner.clj:576](/home/forge/src/clj-surgeon-astra-consult/src/clj_surgeon/memory_battery_runner.clj:576) still defaults its direct runner to [/home/forge/tmp/membat](/home/forge/tmp/membat); the Makefile override does not make the direct entrance portable.
- **The removed lock path survives in user-facing recovery.** The Makefile’s executable lock now uses [bin/with-lock](/home/forge/src/clj-surgeon-astra-consult/bin/with-lock) and a derived repository key, but [test/clj_surgeon/battery_ledger.clj:115](/home/forge/src/clj-surgeon-astra-consult/test/clj_surgeon/battery_ledger.clj:115) still prescribes `flock /home/forge/tmp/suite.lock make test-battery`. [README.md:2340](/home/forge/src/clj-surgeon-astra-consult/README.md:2340) still describes a flock semaphore under `/var/tmp/forge/gate-slots/`, as does the opening of [docs/plans/parallel-landing-gate.md](/home/forge/src/clj-surgeon-astra-consult/docs/plans/parallel-landing-gate.md). These descriptions predate the abstract-socket repair. Correct the contract, not just the spelling of the path.
- **Seat tools and credential locations are embedded.** [bin/typist-run:367](/home/forge/src/clj-surgeon-astra-consult/bin/typist-run:367) and `:368` name provider files under [/home/forge/secrets](/home/forge/secrets); other defaults select an internal worktree and fixture roots. These are credential locations, not evidence that the credential contents are committed. The initial public scope should omit this experiment and its dependent publication/provider commands. [src/clj_surgeon/core.clj:2174](/home/forge/src/clj-surgeon-astra-consult/src/clj_surgeon/core.clj:2174), `:2202`, `:2212`, `:2287`, `:2298`, and [src/clj_surgeon/mission.clj:893](/home/forge/src/clj-surgeon-astra-consult/src/clj_surgeon/mission.clj:893) also publish seat-specific paths as runnable help.
- **Routing installation distributes internal instructions.** `make install` invokes `install-agent-routing` ([Makefile:205](/home/forge/src/clj-surgeon-astra-consult/Makefile:205)), modifying the user’s global Claude/Codex instruction files. [resources/clj-surgeon-agent-routing.md:32](/home/forge/src/clj-surgeon-astra-consult/resources/clj-surgeon-agent-routing.md:32), `:52`, `:97`, `:98`, `:100`, `:112` contains internal scratch and records pointers. The supported product names Codex and Claude can remain in public integration documentation if Gene approves; personal seats, internal hosts, private tools and private evidence addresses cannot. Remove the current contradictory native-versus-Surgeon directions in [AGENTS.md](/home/forge/src/clj-surgeon-astra-consult/AGENTS.md)/[CLAUDE.md](/home/forge/src/clj-surgeon-astra-consult/CLAUDE.md) as part of the public instruction contract. Announce affected Codex/Claude installations before a future routing installation; require installed parity afterward. This consult installs nothing.
- **README claims disagree with the current entrance.** [README.md:526](/home/forge/src/clj-surgeon-astra-consult/README.md:526) describes only two prerequisites and `:541` says installation only warns when they are missing; [bin/install-preflight](/home/forge/src/clj-surgeon-astra-consult/bin/install-preflight) requires bb, Java, Clojure, Git and Python, and swipl is required to test. The install section should explicitly disclose global instruction changes. [README.md:192](/home/forge/src/clj-surgeon-astra-consult/README.md:192) directs agents to the retired direct cclsp route. Its extraction headline and old speed figures are dated, bounded experiments, not universal routing guidance; the September 8 informed fan-out suspension must be visible. The MCP section explicitly calls MCP a development experiment. Preserve that qualification until Gene chooses a public support contract.
- **An installed split route is not established by installing the skill.** [src/clj_surgeon/workspace_onboarding.clj:298](/home/forge/src/clj-surgeon-astra-consult/src/clj_surgeon/workspace_onboarding.clj:298) emits an enabled-tools list without `namespace_split` or `require_change`. `make install` does not establish a live MCP connection. Before advertising a routed MCP split to public users, test its actual documented onboarding and tool visibility on a clean client; a schema in the server source is insufficient evidence of delivery.
- **Evidence links and artifacts need public curation.** The routing evidence document cites `docs/observations/2026-09-06-astra-alias-replication.md`, which is absent from this tree. Existing Markdown links in README/plate/skill resolve, but bare filenames and absolute scratch addresses do not thereby become available to a public reader. Keep only claims whose fixture, exact subject, method, limitations and sanitized receipt can be distributed and reproduced. Imported application snapshots in [test-fixtures/](/home/forge/src/clj-surgeon-astra-consult/test-fixtures) and raw transcripts need provenance and publication review before inclusion.
- **The current “repository hygiene” gate does not scan for these exposures.** [test/repository_hygiene_gate.sh](/home/forge/src/clj-surgeon-astra-consult/test/repository_hygiene_gate.sh) checks tracked [.cpcache](/home/forge/src/clj-surgeon-astra-consult/.cpcache) entries and ignore behavior. It does not check secrets, host paths, ports, routing instructions, transcripts or Python bytecode. The added [bench/anvil-arms/prompts/__pycache__/build-prompts.cpython-314.pyc](/home/forge/src/clj-surgeon-astra-consult/bench/anvil-arms/prompts/__pycache__/build-prompts.cpython-314.pyc) is one concrete omission. Do not interpret a green hygiene stage as a public-repository clearance.

A supplemental text scan finds [/home/](/home) in 536 files, `/Users/` in 284, the selected internal-port spellings in 455, and the seat-tool spellings in 254. Many are fixtures or historical records; some are real defaults. The appendix includes these locations as additional labels. A public port policy must distinguish configurable documented interface defaults from references to the internal running fleet. Under the strict request here, remove internal fleet references even from comments and examples. No listed port was contacted.

A bounded secret-signature scan found no private-key headers, GitHub/provider token signatures, AWS access IDs, credential-bearing HTTP URLs or quoted secret assignments in tracked UTF-8 text. The one long Bearer literal in [bench/measure_prefill_decode_ratio.sh:219](/home/forge/src/clj-surgeon-astra-consult/bench/measure_prefill_decode_ratio.sh:219) is the deliberately invalid upload-control credential documented beside it. A separate scan of non-merge textual patches in the requested divergence examined 310,050 added and 21,074 removed lines for key/token/credential-URL signatures and found none. **This is not a secret clearance:** compressed artifacts, binary blobs, full merge-only resolutions, all old reachable snapshots, commit messages, author metadata and tag annotations still require a dedicated publication scan. No actual credential files were opened and no credential was tested. Scans must report only finding locations and redacted evidence.

**Gene’s months-dogfooded criterion.** A defensible winner has a named capability and eligible contract, an attested implementation lineage, regular real use over the agreed months, successful independent behavioral acceptance, maintained safety review, and a complete failure/refusal/fallback ledger. A speed claim additionally needs its registered native controls, full request-to-verified wall and variance treatment. The age of a repository, elapsed days without use, mandatory fixture replays, test counts, a green suite, a stable tag and a subjective score cannot substitute for that evidence.

The repository started in March, so an older CLI capability could conceivably have months of experience. This audit did not establish that experience for its current implementation, and the older CLI already on main cannot confer its age on the new September features. The earliest newly reachable implementations below are days old.

| Capability on the inspected trunk | Evidence actually available | Meets “months-dogfooded winner” today? |
|---|---|---|
| Repository landing gate/coordinator and box slots | Coordinator introduced September 8, gate/slots September 9. [/var/tmp/forge/plan2/cellC/astra-gate-lanes-r2-report.md](/var/tmp/forge/plan2/cellC/astra-gate-lanes-r2-report.md) records three final gates around 166 s against one historical 510 s baseline. The records repo’s `docs/observations/2026-09-09-gate-lanes/sol-verdict-20260909T080543Z-2dfbc286fed9-GO.md` independently exercises the abstract-socket repair; [2026-09-09-gene-report-morning.md](/home/forge/src/clj-surgeon-records/docs/observations/2026-09-09-gene-report-morning.md) reports 175 s on landed trunk and consumed prewarm gates. Linux sharing is per network namespace, not across arbitrary container namespaces. | **No.** Strong recent operational evidence, measured bounds and fence repairs; about one day for the final mechanism. Separate the product gate from privately installed gate consumers, some of which had under-gating defects. |
| Product `admit_clojure_patch` gate | Introduced September 2; [docs/intent/mcp-operation-contract/admit-clojure-patch-specs.md](/home/forge/src/clj-surgeon-astra-consult/docs/intent/mcp-operation-contract/admit-clojure-patch-specs.md) and [docs/observations/admit-gate-round17-review-opus.md](/home/forge/src/clj-surgeon-astra-consult/docs/observations/admit-gate-round17-review-opus.md) record contract/fence work. [2026-09-06-strictly-better-evidence.md](/home/forge/src/clj-surgeon-astra-consult/docs/observations/2026-09-06-strictly-better-evidence.md) reports 135.1 s tool versus 130.6 s native for the whole-feature gate comparison. | **No.** Safety capability with extensive witnesses; that measurement is a wall loss, and it is not an automatic routed winner. |
| Alias migration | Introduced September 2, policy/receipt repairs through September 8. Strictly-better summary reports 31.1 s versus 42.9 s on the fixed no-collision fixture, with a missed 1.5× prediction and limited control design; its named primary replication file is missing here. [2026-09-08-row2-fresh-seat-pilot.md](/home/forge/src/clj-surgeon-astra-consult/docs/observations/2026-09-08-row2-fresh-seat-pilot.md) gives 3/4 unprompted routing, 4/4 accepted and 9 files/21 sites/3 collisions on the later fixture. [2026-09-08-row2-entrance.md](/home/forge/src/clj-surgeon-astra-consult/docs/observations/2026-09-08-row2-entrance.md) reaches 4/4 routing but misses the ≤60 s caller-work gate at 115.9 s. | **No.** Worth taking to sustained real dogfood after portable artifact storage and public evidence repair. Directed 3.98× caller-work and cold-pilot 1.36× with one native observation are different quantities, neither a months-long field estimate. |
| Exact-contract cold namespace split | Introduced September 7. [2026-09-07-plan-2-cellC-result.md](/home/forge/src/clj-surgeon-astra-consult/docs/observations/2026-09-07-plan-2-cellC-result.md) retains the 141-owner/20-destination/87-site/five-caller fixture and later CLI/MCP reruns; [2026-09-08-namespace-split-papercuts-round3.md](/home/forge/src/clj-surgeon-astra-consult/docs/observations/2026-09-08-namespace-split-papercuts-round3.md) retains real cold proof and zero counted paper cuts. [2026-09-08-row1-split-ledger.md](/home/forge/src/clj-surgeon-astra-consult/docs/observations/2026-09-08-row1-split-ledger.md) explicitly marks fresh-control waves, observer qualification, cold-start cohorts and the eligible month as absent at its freeze. | **No.** A narrow experimental candidate, not a general decomposition product. The large 5.6–9.7× historical ratios reuse older controls and are not fresh matched estimates for the final trunk. Retained-source splits and changed mappings are not covered by the automatic admission. |
| Background proof and rich receipts | September 8–9: detached closure and negative facts, then comment identity, `ns_edits`, footprint, lint and executed-load facts. [2026-09-08-row1-cohort/astra-row1-cohort-report.md](/home/forge/src/clj-surgeon-astra-consult/docs/observations/2026-09-08-row1-cohort/astra-row1-cohort-report.md) says **NO ADMISSION** for the registered warm/cold comparison. The records repo’s [2026-09-09-row5-adopt-4.md](/home/forge/src/clj-surgeon-records/docs/observations/2026-09-09-row5-adopt-4.md) reports 6/6 first-attempt accepted, zero refusals/fallbacks, but misses its primary 0.50 inspection-ratio gate at 0.675. Its dual-score follow-up finds one receipt-silent action and 13 trust-witness actions; it explicitly does not turn E4 into a retrospective pass. | **No.** Meaningful recent usability progress. Neither async return nor `committed` proves completed behavior. Completion requires current-snapshot successful named checks, no pending proof, and independent required review. |
| Product relation census; test/intent censuses | Relation census introduced September 2, heavily repaired through its fence rounds; see [docs/intent/relation-census/](/home/forge/src/clj-surgeon-astra-consult/docs/intent/relation-census) and [docs/observations/census-round25-review-opus.md](/home/forge/src/clj-surgeon-astra-consult/docs/observations/census-round25-review-opus.md). Test/intent census derivation landed September 9 with [deftest_census.edn](/home/forge/src/clj-surgeon-astra-consult/test/clj_surgeon/deftest_census.edn) and TEST-ISO-015. | **No.** The former locates review work and explicitly does not prove idempotency; the latter is a valuable enforcement mechanism with roughly a day of the new implementation, not a months-proven product claim. |
| Install/Darwin; remaining experiments | September 10 portability series. [docs/install/skiff.md](/home/forge/src/clj-surgeon-astra-consult/docs/install/skiff.md) explicitly says no Mac execution. The retained skiff-install report shows clean-prefix Linux install exit 0 in 25 s, a full-test stop at missing swipl, and 24 baseline CLI-dispatch assertion failures from analyzer resolution/home mismatch. Feature-thread, helper/mission and typist work have separate recent studies and unsupported shapes. | **No.** Actual macOS full install/test is open. The report’s “clean user” shorthand must be corrected to “clean prefix.” The rest needs independent scope and winner decisions; “stage 3” is not permission to ship everything. |

For the final portability fence, the records repo’s `docs/observations/2026-09-10-skiff-install-sol-verdict-round-3-GO.md` binds its GO to sealed candidate `6867f773`, with the same tree as tip `6067f938`. It expressly did not run `make test`. The tracked battery ledger’s latest pass is `6867f773`, 2026-09-10T04:39:26Z, 149 s, eight lanes, zero skipped, host `anvil-server`; it is not a Mac receipt or a new independent test of the complete sanitized public candidate.

**Proposed sequence.** These are preparation and qualification stages on unpublished branches until Gene explicitly opens the release gate. Each stage is reviewed against the cumulative candidate tree. A green gate for an original side-branch commit does not authorize its cherry-picked or squashed composition. Use a private integration checkout for source work; this read-only consult checkout remains untouched.

1. **Stage 0 — hygiene and public contract.** The mayor owns a local preparation branch pinned to `59d8bc0c` and a proposed file manifest: include product code, direct witnesses, portable build inputs and approved public intent; exclude raw seat records, private automation, caches, provider experiments and unsupported claim material. Correct the executable artifact roots, stale lock remedies, install documentation and public routing instructions. Preserve originals in the records store and create sanitized evidence summaries with an explicit provenance mapping; never rewrite old receipts to pretend they were produced on sanitized paths. Removing a notebook must not delete a fixture, oracle or named requirement consumed by the build. Audit dependencies before choosing files. A scope change in an existing linked-intent leaf follows its approved HLD → LLD → EARS → tests → code process; this report approves no implementation phase.

   Gate: Sol reviews the proposed public boundary and exercises the changed path/reader/subprocess contracts on the eventual candidate. All findings close against named bytes. Run the full public-repository scan over tracked text, binary/archive contents, generated install packages, selected ancestry, commit/tag metadata and any refs proposed for publication; classify each hit with no unexplained internal names or credential candidates. Resolve README/help/skill contradictions and verify every retained claim’s accessible evidence. After the receipt bootstrap below, require a genuine clean-user clone on a box other than Anvil to pass `make install && make test`, without existing seat scripts or copied caches. Failed or unrun means stage open. The historic clean-prefix install does not close it.

2. **Stage 1 — portable platform.** Build `public-candidate` from frozen public `main`, then import a reviewed squash of the qualified platform closure: coordinator, slots, temp/heap guards, test and intent census enforcement, portable installer, bounded memory/transaction prerequisites and their direct tests/specifications. Keep relation census only if its independent scope and fence are accepted; it can follow separately. Recommended history is one green, reviewable commit per coherent platform slice, not the 2,562-commit development stream. An internal manifest maps public commits to original SHAs and proof artifacts. Do not cherry-pick only the September 9–10 commits: those presume the earlier test partition, transaction machinery and inventories. Derive the dependency closure and either include it explicitly or delay the platform slice; never make the gate pass by dropping required namespaces.

   Gate: fresh Sol adversarial review of the composed candidate, including independent worker admission, failed/killed workers, stale or missing child receipts, namespace membership, untrusted readers/subprocess argv and workspace confinement. Public scan passes again. Fresh clean-user Linux and actual macOS clones execute the documented `make install && make test`; macOS must report its path-socket admission and actual capacity, and demonstrate the declared weaker root-deletion behavior. Preserve per-stage exits, unique run ID, candidate SHA/tree/source digest, toolchains, zero unexplained skips, namespace counters, leaks and refusal results. All analyzer lint uses the installed `~/bin/clj-kondo` serializer. Public CI must run from repository-owned commands without `sol-yolo`, `suite-run`, `ship`, `land-auto` or a private records checkout. Sol is the review role, not a runtime dependency for users.

3. **Stage 2 — eligible routed operations with intent.** Introduce alias migration first, then the exact witnessed cold split, as separately reviewable squashes including their necessary kernel, proof/undo and test changes. Publish a clean, runnable contract and sanitized receipt for each. Preserve the exact split limits: fully mapped known Cell C shape/manifest, named cold profile, explicit visibility/source-retirement policy, captured roots, required oracles and successful checks. Default native outside the admitted classes; preserve the informed fan-out suspension. Do not turn retained-source support, arbitrary Clojure macros, dynamic callers or warm-only proof into automatic routes by proximity. Ensure actual documented client registration exposes every advertised tool.

   Gate: Sol fence of the public CLI/MCP entrances, confinement and rollback/undo; public scan; clean-clone install/test on non-Anvil Linux and native macOS, plus each included verb’s direct behavioral acceptance and actual client delivery. Run the application’s required load/tests and paper-cut oracle as applicable. Finish detached proof before accepting any claim based on it. Carry the long-term evidence ledger with first attempts, refusals, repairs, fallback, unknown telemetry and complete verified wall. Gene’s named winner decision and the agreed months of real use are required before any of this stage lands on main. Performance studies belong to later separately authorized work, not this consult.

4. **Stage 3 — explicitly chosen remainder.** Review product patch admission, supporting reads, broader helper extraction, feature-thread and missions one capability at a time. Exclude typist/provider runners, deployment/auto-fix tools and raw cohort archives unless Gene deliberately wants them as separately documented public products. Generic “the rest” has no release authorization. Gate each accepted slice with the same Sol fence, public scan and clean-clone install/test, its own acceptance and maintenance evidence, and its own winner decision where required. No weaker gate for documents that affect installed agent routing or executable intent.

The battery receipt needs a deliberate bootstrap before those clean-clone gates. [test/clj_surgeon/battery_ledger.clj](/home/forge/src/clj-surgeon-astra-consult/test/clj_surgeon/battery_ledger.clj) stores evidence in [docs/observations/battery-ledger.edn](/home/forge/src/clj-surgeon-astra-consult/docs/observations/battery-ledger.edn), refuses entries older than 26 hours, rejects non-ancestor SHAs, and limits counted ancestry distance to 30 commits. Deleting all observations removes required input. Copying an old ledger into a squash based on main names SHAs outside the new ancestry. A frozen release tag also outlives a 26-hour receipt even if its code is unchanged. Consequently a permanently reproducible public `make test` cannot rely on inheriting today’s internal ledger.

Preserve the safety obligations while defining a public receipt policy in stage 0/1: on a fresh candidate clone, generate a fresh full battery receipt for that candidate through repository-owned commands, then run the documented install/test sequence. If the one-command public test contract is retained, its entrance must own that bootstrap rather than instruct every downstream user to commit a nightly ledger. Bind fresh evidence to the exact tested content and retain failures; do not disable `battery-fresh`, invent timestamps, substitute prewarm for landing, or mechanically relabel internal SHAs. Changing the policy is a specified and fenced implementation task, not a redaction. Keep [deftest_census.edn](/home/forge/src/clj-surgeon-astra-consult/test/clj_surgeon/deftest_census.edn) and intent membership distinct from this execution ledger; the former are checked membership contracts and cannot be regenerated silently by the gate.

**Gene’s decisions before implementation becomes publication.** Approve a public product scope and a precise interpretation of “months” (recommended minimum: 60 calendar days of regular eligible real use, with an agreed minimum use frequency and enough varied tasks to cover the claimed contract; the calendar alone never passes). Confirm whether gate infrastructure can receive an explicit, limited exception before the product winners qualify; absent that exception, its main landing also waits. Choose reviewed squashes versus development history. Decide whether sanitized intent/specifications are public; recommended yes, because the audit consumes them and they explain the guarantees, while raw operational notebooks remain separate. Decide whether global agent routing belongs in default install or an explicit optional target. Approve the receipt bootstrap/storage contract. Finally, specify who performs the public ref update and which immutable public release/RC tags exist. Keep `stable/2026-09-10` as its original internal checkpoint; do not silently move it or imply the score is release certification. Do not push all tags as part of a public release.

**Exposure and reversibility.** Publishing a branch to a public repository exposes that branch, even while main is frozen. Deleting sensitive files at the branch tip does not remove their parent blobs, commit subjects, internal author addresses, session links or tag annotations. The current local remote URL is the public repository, so an ordinary candidate push is a publication action. Some MCP history may already be remotely visible; this consult did not establish that. If a dedicated scan finds an actual credential in any already published ref, escalate that concrete finding for revocation/remediation; a new clean tip does not undo the exposure.

The routing plate is active software: `make install` writes it into global instructions. Internal Codex/Claude seat rules, evidence paths and unsupported automatic routes could influence unrelated projects. `SHIP-FIX-BLOCK` is described in [docs/observations/2026-09-08-tighten-binding-andon.md:533](/home/forge/src/clj-surgeon-astra-consult/docs/observations/2026-09-08-tighten-binding-andon.md:533) and the captain’s log: it carries sealed candidate identity, producer, patch manifest and bounded automatic repair, with an independent-review requirement for executable changes. Its installed implementation is outside this repository. Publishing that prose as a supported built-in workflow would promise machinery a clean clone does not contain and expose internal author/publication policy. Preserve the review principles in public contributor guidance only after removing the private command contract.

Battery ledgers disclose hosts and internal commit topology and participate in executable freshness checks. Raw cohorts can disclose source, prompts, request paths, service endpoints, session IDs and cost records. Curating these is a substantive publication decision. Keeping the old records privately also preserves negative findings and prevents sanitized summaries from becoming uncheckable success stories. Public claims need a distributable evidence subset; a private hash alone cannot support a public reproducibility promise.

The reversible alternative Gene suggested is useful for **internal dogfood**: cut a local/private `public-candidate` from the pinned MCP tag, add reviewed hygiene changes, install immutable packages on the skiff and use them for **N = 60 days proposed**, with failure and rollback receipts and a dated review checkpoint. A split replicated on the same fixture for sixty days is not varied real dogfood. Safety/semantic changes during the trial reopen the affected capability’s qualification; unrelated edits need not erase independent evidence. Keep the previous installed package and the original main/tag so the user can restore the known entrance. That branch can eventually fast-forward main because main is an ancestor, but only if Gene approves publication of its entire ancestry after the full scan. Cleanup commits alone cannot make that condition true.

My preferred public variant is the main-based, sanitized `public-candidate` from stages 0–2. Preserve the original March root/main ancestry, keep the full MCP development history privately, and qualify the **exact sanitized candidate** on the skiff. This avoids importing the additional internal ancestry and retains a fast-forward path from public main. It does not erase internal material already present in old public main; that inherited exposure needs an explicit disposition. If “no internal references” applies literally to every reachable historical blob, neither fast-forward alternative can satisfy it while retaining the existing public-main ancestry. Gene must decide whether the policy is a sanitized current tree plus no new historical exposure, or whether a separately authorized public-history remediation is required. This plan authorizes no rewrite of public history. Before public promotion, failure is handled by abandoning the candidate or restoring the prior installed package. After publication, a corrective/revert commit can reverse behavior but cannot retract downloaded history, so exposure review must happen first.

**The one first step for this week:** the mayor prepares one **unpublished README truth patch** on a local hygiene branch pinned to `59d8bc0c`. Limit it to the four immediately reviewable corrections: actual installation/test prerequisites, default global-routing installation, the current abstract/path-socket guarantees, and the distinction between dated narrow benchmark results and current automatic routes (including direct cclsp retirement). Review those sentences against [bin/install-preflight](/home/forge/src/clj-surgeon-astra-consult/bin/install-preflight), [Makefile](/home/forge/src/clj-surgeon-astra-consult/Makefile), [test/gate_slot.py](/home/forge/src/clj-surgeon-astra-consult/test/gate_slot.py) and [resources/clj-surgeon-agent-routing.md](/home/forge/src/clj-surgeon-astra-consult/resources/clj-surgeon-agent-routing.md). The output is one small concrete patch for Gene and Sol to review, with a source/evidence citation for each corrected claim. It starts the preparation lane without touching or publishing main and without pretending stage 0’s runtime path fixes are complete. No branch or patch was created by this consult.

The remaining stage gates are explicitly unrun. The meaningful next decision is whether Gene approves that preparation scope and the proposed public support/qualification policy, not whether to merge the current tag today.

**Scan appendix — exact file inventory.**

The inventory below is generated from tracked text at `origin/MCP/main`, without executing repository code. It uses case-insensitive substring matches, so lexical matches such as “membership,” “forged” and “forget” are retained and must not be classified as internal leaks automatically. Each value is a set of matching line numbers, with consecutive lines compressed to ranges; no source text or secret value is copied. `Δ` means the file differs from origin/main; `=` means unchanged in the endpoint diff. The seven requested spellings have these union-independent counts (one line may match several spellings):

- `f` = `forge`: 824 files; 5,196 matching lines.
- `a` = `anvil`: 356 files; 1,316 matching lines.
- `p` = `7906`: 82 files; 208 matching lines.
- `v` = [/var/tmp/forge](/var/tmp/forge): 454 files; 2,471 matching lines.
- `y` = `sol-yolo`: 51 files; 146 matching lines.
- `r` = `records-push`: 4 files; 11 matching lines.
- `s` = `ship`: 491 files; 1,758 matching lines.

Additional inventory labels: `H` = [/home/](/home); `U` = `/Users/`; `P` = numeric spellings `7888|7890|7894|7895|7906|8171|83[0-3][0-9]`; `T` = `sol-yolo|records-push|fence-run|land-auto|surgeon-call|suite-run|seat-receipt`. P is a text locator, not proof that each number denotes a port. The extra labels locate exposures missed by the seven terms. Archive/binary contents and arbitrary unlisted seat names need the dedicated scan described above. The tracked bytecode file was found by filename/binary inventory and is listed in the body, not treated as scanned UTF-8 text.

.beads

```text
.beads/interactions.jsonl [=] a:46,48,54,58; s:11-12,21; U:11-12; P:52
```

.claude

```text
.claude/skills/clj-surgeon/references/advanced-operations.md [Δ] s:16
```

(root files)

```text
.gitignore [Δ] a:41
CHANGELOG.md [Δ] s:14,331,365
CLAUDE.md [Δ] f:35; a:1,6,35,204,222; y:10; s:132,140,151,197,357,408; P:35,434,437,515; T:10
LQBM-REPORT.md [=] f:31,39,171,175
Makefile [Δ] f:46; a:73,94,111,151-154,849,851,854,856,859,861,864,866,870,872,874-876,878,888-889,1341; s:109,1063; H:46; P:24,84
README.md [Δ] f:785,2340; a:17,30,43,57,68; v:785,2340; s:231,376,1287,1514,1629,2350; P:685,1234-1235,1240
```

bench

```text
bench/README.md [=] a:143,154,159; P:20
bench/anvil-arms/README.md [Δ] f:110-111,118,125,129; a:1,11,13,29,114,116,121,123,263; p:145,192; H:110-111,118,125,129; P:145,191-192
bench/anvil-arms/prompts/build-prompts.py [Δ] a:186
bench/anvil-arms/run-arm.sh [Δ] f:25,81,245; p:33; y:237; s:183; H:25,81; P:33; T:237
bench/anvil-arms/score.py [Δ] a:45
bench/anvil-arms/self-test.sh [Δ] f:25,41,55,66,79,765,1409,1512,1785; a:9,108-109,174-175,767,773,1458,1859,1984,1991,2492; s:132,450,454,1183,2373; H:25,41,55,66,79,765,1409,1512,1785; P:308,1392
bench/anvil-arms/stop-server.sh [Δ] s:15
bench/anvil-arms/watch.py [Δ] a:810; y:4; T:4
bench/counterfactual-replay/README.md [=] a:34; P:29
bench/counterfactual-replay/cases/cclsp-optional/capsule.edn [=] P:8
bench/fanout/fan_check.clj [Δ] f:146,442
bench/fanout/gen-fanout.clj [Δ] f:4,335; H:4,335
bench/fanout/rescore-FAN.sh [Δ] f:9,28; H:9,28
bench/fanout/sabotage-FAN.sh [Δ] f:68,146,422,545,610,623,634,637,674-675,680,684,721-723,728,730,784,918,1022,1166,1239-1240; a:110,158-159,163-164,242-243,353-354,356-357,436-437,557-558,651-652,816-817,946-947,1037-1038,1245; v:1022,1166; H:68,146,422,545,637,784,918,1239-1240
bench/fixtures/edit_portfolio/dependency-move-edit/capsule.edn [=] s:2
bench/fixtures/edit_portfolio/sessionize-format-extraction/after/src/cfp_scheduler_killer/views.clj [=] f:655,1089,3311; s:790,800,820,1981,3830,3893,4056,4093,4095-4096
bench/fixtures/edit_portfolio/sessionize-format-extraction/before/src/cfp_scheduler_killer/views.clj [=] f:762,1196,3418; s:897,907,927,2088,3937,4000,4163,4200,4202-4203
bench/fixtures/edit_portfolio/submission-row-extraction-cleanup/capsule.edn [=] P:21,23
bench/measure_prefill_decode_ratio.sh [Δ] s:202
bench/memory_battery/generate_tree.clj [Δ] f:22,406; s:407; H:22,406
bench/parser_admission/red_witness.clj [Δ] f:107; a:41; H:107
bench/performance_regression_sentinel_io_test.clj [=] a:257,261
bench/queue_anvil_sol_wave2.sh [=] a:32
bench/raw-cohort-v2/run.py [Δ] f:20-21,23,244,304,365,463; v:20-21,23,244,304,365,463
bench/raw-cohort-v2/test_run.py [Δ] f:101,105,115,125,137,162,200,219; v:101,105,115,125,137,162,200,219
bench/relation_causal_artifacts.sha256 [=] P:9,19,36
bench/relation_causal_corpus.clj [=] P:34
bench/relation_causal_score_test.clj [=] f:459,464,815,821,831; H:748
bench/results/2026-08-04-claude-fable-opus-20260804T222330Z/MANIFEST.sha256 [=] P:24,52,81,109
bench/results/2026-08-04-claude-fable-opus-20260804T222330Z/fable-ops-registry-xray/final.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T222330Z/fable-ops-registry-xray/request.tsv [=] U:8
bench/results/2026-08-04-claude-fable-opus-20260804T222330Z/fable-ops-registry-xray/skill-install-receipt.edn [=] U:5-6
bench/results/2026-08-04-claude-fable-opus-20260804T222330Z/fable-ops-registry-xray/skill.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T222330Z/fable-ops-registry-xray/start.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T222330Z/fable-ops-registry-xray/terminal.tsv [=] U:8
bench/results/2026-08-04-claude-fable-opus-20260804T222330Z/fable-pair-view-edit/expected.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T222330Z/fable-pair-view-edit/final.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T222330Z/fable-pair-view-edit/request.tsv [=] U:8
bench/results/2026-08-04-claude-fable-opus-20260804T222330Z/fable-pair-view-edit/skill-install-receipt.edn [=] U:5-6
bench/results/2026-08-04-claude-fable-opus-20260804T222330Z/fable-pair-view-edit/skill.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T222330Z/fable-pair-view-edit/start.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T222330Z/fable-pair-view-edit/terminal.tsv [=] U:8
bench/results/2026-08-04-claude-fable-opus-20260804T222330Z/installed-claude-skill.receipt.edn [=] U:5-6
bench/results/2026-08-04-claude-fable-opus-20260804T222330Z/matrix.tsv [=] U:2,5
bench/results/2026-08-04-claude-fable-opus-20260804T222330Z/opus-ops-registry-xray/final.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T222330Z/opus-ops-registry-xray/request.tsv [=] U:8
bench/results/2026-08-04-claude-fable-opus-20260804T222330Z/opus-ops-registry-xray/skill-install-receipt.edn [=] U:5-6
bench/results/2026-08-04-claude-fable-opus-20260804T222330Z/opus-ops-registry-xray/skill.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T222330Z/opus-ops-registry-xray/start.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T222330Z/opus-ops-registry-xray/terminal.tsv [=] U:8
bench/results/2026-08-04-claude-fable-opus-20260804T222330Z/opus-pair-view-edit/expected.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T222330Z/opus-pair-view-edit/final.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T222330Z/opus-pair-view-edit/request.tsv [=] U:8
bench/results/2026-08-04-claude-fable-opus-20260804T222330Z/opus-pair-view-edit/skill-install-receipt.edn [=] U:5-6
bench/results/2026-08-04-claude-fable-opus-20260804T222330Z/opus-pair-view-edit/skill.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T222330Z/opus-pair-view-edit/start.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T222330Z/opus-pair-view-edit/terminal.tsv [=] U:8
bench/results/2026-08-04-claude-fable-opus-20260804T223329Z/MANIFEST.sha256 [=] P:4,29
bench/results/2026-08-04-claude-fable-opus-20260804T223329Z/installed-claude-skill.receipt.edn [=] U:5-6
bench/results/2026-08-04-claude-fable-opus-20260804T223329Z/matrix.tsv [=] U:2,5; P:3
bench/results/2026-08-04-claude-fable-opus-20260804T223329Z/opus-ops-registry-xray/final.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T223329Z/opus-ops-registry-xray/request.tsv [=] U:8; P:5
bench/results/2026-08-04-claude-fable-opus-20260804T223329Z/opus-ops-registry-xray/skill-install-receipt.edn [=] U:5-6
bench/results/2026-08-04-claude-fable-opus-20260804T223329Z/opus-ops-registry-xray/skill.sha256 [=] U:1; P:1
bench/results/2026-08-04-claude-fable-opus-20260804T223329Z/opus-ops-registry-xray/start.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T223329Z/opus-ops-registry-xray/terminal.tsv [=] U:8
bench/results/2026-08-04-claude-fable-opus-20260804T223329Z/opus-pair-view-edit/command-terminal.tsv [=] P:4
bench/results/2026-08-04-claude-fable-opus-20260804T223329Z/opus-pair-view-edit/expected.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T223329Z/opus-pair-view-edit/final.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T223329Z/opus-pair-view-edit/request.tsv [=] U:8; P:5
bench/results/2026-08-04-claude-fable-opus-20260804T223329Z/opus-pair-view-edit/skill-install-receipt.edn [=] U:5-6
bench/results/2026-08-04-claude-fable-opus-20260804T223329Z/opus-pair-view-edit/skill.sha256 [=] U:1; P:1
bench/results/2026-08-04-claude-fable-opus-20260804T223329Z/opus-pair-view-edit/start.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T223329Z/opus-pair-view-edit/terminal.tsv [=] U:8
bench/results/2026-08-04-claude-fable-opus-20260804T223709Z/MANIFEST.sha256 [=] P:3
bench/results/2026-08-04-claude-fable-opus-20260804T223709Z/installed-claude-skill.receipt.edn [=] U:5-6
bench/results/2026-08-04-claude-fable-opus-20260804T223709Z/matrix.tsv [=] U:2,5
bench/results/2026-08-04-claude-fable-opus-20260804T223709Z/opus-ops-registry-xray/command-terminal.tsv [=] P:4
bench/results/2026-08-04-claude-fable-opus-20260804T223709Z/opus-ops-registry-xray/final.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T223709Z/opus-ops-registry-xray/request.tsv [=] U:8
bench/results/2026-08-04-claude-fable-opus-20260804T223709Z/opus-ops-registry-xray/skill-install-receipt.edn [=] U:5-6
bench/results/2026-08-04-claude-fable-opus-20260804T223709Z/opus-ops-registry-xray/skill.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T223709Z/opus-ops-registry-xray/start.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T223709Z/opus-ops-registry-xray/state.tsv [=] P:4
bench/results/2026-08-04-claude-fable-opus-20260804T223709Z/opus-ops-registry-xray/terminal.tsv [=] U:8
bench/results/2026-08-04-claude-fable-opus-20260804T223956Z/MANIFEST.sha256 [=] P:1,20,26,58-59,83
bench/results/2026-08-04-claude-fable-opus-20260804T223956Z/fable-ops-registry-xray/command-terminal.tsv [=] P:4
bench/results/2026-08-04-claude-fable-opus-20260804T223956Z/fable-ops-registry-xray/final.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T223956Z/fable-ops-registry-xray/request.tsv [=] U:8; P:5
bench/results/2026-08-04-claude-fable-opus-20260804T223956Z/fable-ops-registry-xray/skill-install-receipt.edn [=] U:5-6
bench/results/2026-08-04-claude-fable-opus-20260804T223956Z/fable-ops-registry-xray/skill.sha256 [=] U:1; P:1
bench/results/2026-08-04-claude-fable-opus-20260804T223956Z/fable-ops-registry-xray/start.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T223956Z/fable-ops-registry-xray/state.tsv [=] P:4
bench/results/2026-08-04-claude-fable-opus-20260804T223956Z/fable-ops-registry-xray/terminal.tsv [=] U:8
bench/results/2026-08-04-claude-fable-opus-20260804T223956Z/fable-pair-view-edit/command-terminal.tsv [=] P:4
bench/results/2026-08-04-claude-fable-opus-20260804T223956Z/fable-pair-view-edit/expected.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T223956Z/fable-pair-view-edit/final.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T223956Z/fable-pair-view-edit/request.tsv [=] U:8; P:5
bench/results/2026-08-04-claude-fable-opus-20260804T223956Z/fable-pair-view-edit/skill-install-receipt.edn [=] U:5-6
bench/results/2026-08-04-claude-fable-opus-20260804T223956Z/fable-pair-view-edit/skill.sha256 [=] U:1; P:1
bench/results/2026-08-04-claude-fable-opus-20260804T223956Z/fable-pair-view-edit/start.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T223956Z/fable-pair-view-edit/state.tsv [=] P:4
bench/results/2026-08-04-claude-fable-opus-20260804T223956Z/fable-pair-view-edit/terminal.tsv [=] U:8
bench/results/2026-08-04-claude-fable-opus-20260804T223956Z/installed-claude-skill.receipt.edn [=] U:5-6
bench/results/2026-08-04-claude-fable-opus-20260804T223956Z/matrix.tsv [=] U:2,5; P:3
bench/results/2026-08-04-claude-fable-opus-20260804T223956Z/opus-ops-registry-xray/command-terminal.tsv [=] P:4
bench/results/2026-08-04-claude-fable-opus-20260804T223956Z/opus-ops-registry-xray/final.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T223956Z/opus-ops-registry-xray/request.tsv [=] U:8; P:5
bench/results/2026-08-04-claude-fable-opus-20260804T223956Z/opus-ops-registry-xray/skill-install-receipt.edn [=] U:5-6
bench/results/2026-08-04-claude-fable-opus-20260804T223956Z/opus-ops-registry-xray/skill.sha256 [=] U:1; P:1
bench/results/2026-08-04-claude-fable-opus-20260804T223956Z/opus-ops-registry-xray/start.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T223956Z/opus-ops-registry-xray/state.tsv [=] P:4
bench/results/2026-08-04-claude-fable-opus-20260804T223956Z/opus-ops-registry-xray/terminal.tsv [=] U:8
bench/results/2026-08-04-claude-fable-opus-20260804T223956Z/opus-pair-view-edit/command-terminal.tsv [=] P:4
bench/results/2026-08-04-claude-fable-opus-20260804T223956Z/opus-pair-view-edit/expected.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T223956Z/opus-pair-view-edit/final.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T223956Z/opus-pair-view-edit/request.tsv [=] U:8; P:5
bench/results/2026-08-04-claude-fable-opus-20260804T223956Z/opus-pair-view-edit/skill-install-receipt.edn [=] U:5-6
bench/results/2026-08-04-claude-fable-opus-20260804T223956Z/opus-pair-view-edit/skill.sha256 [=] U:1; P:1
bench/results/2026-08-04-claude-fable-opus-20260804T223956Z/opus-pair-view-edit/start.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T223956Z/opus-pair-view-edit/state.tsv [=] P:4
bench/results/2026-08-04-claude-fable-opus-20260804T223956Z/opus-pair-view-edit/terminal.tsv [=] U:8
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/MANIFEST.sha256 [=] P:12
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/fable-ops-registry-xray/final.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/fable-ops-registry-xray/request.tsv [=] U:8
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/fable-ops-registry-xray/skill-install-receipt.edn [=] U:5-6
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/fable-ops-registry-xray/skill.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/fable-ops-registry-xray/start.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/fable-ops-registry-xray/terminal.tsv [=] U:8
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/fable-pair-view-edit/expected.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/fable-pair-view-edit/final.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/fable-pair-view-edit/request.tsv [=] U:8
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/fable-pair-view-edit/skill-install-receipt.edn [=] U:5-6
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/fable-pair-view-edit/skill.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/fable-pair-view-edit/start.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/fable-pair-view-edit/terminal.tsv [=] U:8
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/fable-pair-view-expect-edit/expected.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/fable-pair-view-expect-edit/final.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/fable-pair-view-expect-edit/request.tsv [=] U:8
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/fable-pair-view-expect-edit/skill-install-receipt.edn [=] U:5-6
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/fable-pair-view-expect-edit/skill.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/fable-pair-view-expect-edit/start.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/fable-pair-view-expect-edit/terminal.tsv [=] U:8
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/installed-claude-skill.receipt.edn [=] U:5-6
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/matrix.tsv [=] U:2,5
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/opus-ops-registry-xray/final.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/opus-ops-registry-xray/request.tsv [=] U:8
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/opus-ops-registry-xray/skill-install-receipt.edn [=] U:5-6
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/opus-ops-registry-xray/skill.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/opus-ops-registry-xray/start.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/opus-ops-registry-xray/terminal.tsv [=] U:8
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/opus-pair-view-edit/expected.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/opus-pair-view-edit/final.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/opus-pair-view-edit/request.tsv [=] U:8
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/opus-pair-view-edit/skill-install-receipt.edn [=] U:5-6
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/opus-pair-view-edit/skill.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/opus-pair-view-edit/start.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/opus-pair-view-edit/terminal.tsv [=] U:8
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/opus-pair-view-expect-edit/expected.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/opus-pair-view-expect-edit/final.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/opus-pair-view-expect-edit/request.tsv [=] U:8
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/opus-pair-view-expect-edit/skill-install-receipt.edn [=] U:5-6
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/opus-pair-view-expect-edit/skill.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/opus-pair-view-expect-edit/start.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-20260804T231757Z/opus-pair-view-expect-edit/terminal.tsv [=] U:8
bench/results/2026-08-04-claude-fable-opus-v2/SUMMARY.md [=] P:15
bench/results/2026-08-04-claude-fable-opus-v2/fable-ops-registry-xray/final.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-v2/fable-ops-registry-xray/request.tsv [=] U:8
bench/results/2026-08-04-claude-fable-opus-v2/fable-ops-registry-xray/skill-install-receipt.edn [=] U:5-6
bench/results/2026-08-04-claude-fable-opus-v2/fable-ops-registry-xray/skill.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-v2/fable-ops-registry-xray/start.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-v2/fable-ops-registry-xray/terminal.tsv [=] U:8
bench/results/2026-08-04-claude-fable-opus-v2/fable-pair-view-edit/expected.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-v2/fable-pair-view-edit/final.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-v2/fable-pair-view-edit/request.tsv [=] U:8
bench/results/2026-08-04-claude-fable-opus-v2/fable-pair-view-edit/skill-install-receipt.edn [=] U:5-6
bench/results/2026-08-04-claude-fable-opus-v2/fable-pair-view-edit/skill.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-v2/fable-pair-view-edit/start.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-v2/fable-pair-view-edit/terminal.tsv [=] U:8
bench/results/2026-08-04-claude-fable-opus-v2/installed-claude-skill.receipt.edn [=] U:5-6
bench/results/2026-08-04-claude-fable-opus-v2/matrix.tsv [=] U:2,5
bench/results/2026-08-04-claude-fable-opus-v2/opus-ops-registry-xray/final.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-v2/opus-ops-registry-xray/request.tsv [=] U:8
bench/results/2026-08-04-claude-fable-opus-v2/opus-ops-registry-xray/skill-install-receipt.edn [=] U:5-6
bench/results/2026-08-04-claude-fable-opus-v2/opus-ops-registry-xray/skill.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-v2/opus-ops-registry-xray/start.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-v2/opus-ops-registry-xray/terminal.tsv [=] U:8
bench/results/2026-08-04-claude-fable-opus-v2/opus-pair-view-edit/expected.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-v2/opus-pair-view-edit/final.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-v2/opus-pair-view-edit/request.tsv [=] U:8
bench/results/2026-08-04-claude-fable-opus-v2/opus-pair-view-edit/skill-install-receipt.edn [=] U:5-6
bench/results/2026-08-04-claude-fable-opus-v2/opus-pair-view-edit/skill.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-v2/opus-pair-view-edit/start.sha256 [=] U:1
bench/results/2026-08-04-claude-fable-opus-v2/opus-pair-view-edit/terminal.tsv [=] U:8
bench/results/2026-08-04-codex-agent-skill-v3/MANIFEST.sha256 [=] P:4,12
bench/results/2026-08-04-v13-vs-2026-07-12-gpt-5.6-sol-medium/runs.tsv [=] P:20
bench/results/2026-08-24-edit-clojure-512m-smoke/01-r01-pair-view-expect-edit-mcp-hint-no-skill-mcp/target.diff [=] U:1
bench/results/2026-08-24-edit-clojure-cli-sol-high-3x/01-r01-pair-view-expect-edit-matched-skill-post/target.diff [=] U:1
bench/results/2026-08-24-edit-clojure-cli-sol-high-3x/02-r02-pair-view-expect-edit-matched-skill-post/target.diff [=] U:1
bench/results/2026-08-24-edit-clojure-cli-sol-high-3x/03-r03-pair-view-expect-edit-matched-skill-post/target.diff [=] U:1
bench/results/2026-08-24-edit-clojure-matched-skill-sol-high-10x/01-r01-pair-view-expect-edit-matched-skill-mcp/target.diff [=] U:1
bench/results/2026-08-24-edit-clojure-matched-skill-sol-high-10x/02-r02-pair-view-expect-edit-matched-skill-mcp/target.diff [=] U:1
bench/results/2026-08-24-edit-clojure-matched-skill-sol-high-10x/03-r03-pair-view-expect-edit-matched-skill-mcp/target.diff [=] U:1
bench/results/2026-08-24-edit-clojure-matched-skill-sol-high-10x/04-r04-pair-view-expect-edit-matched-skill-mcp/target.diff [=] U:1
bench/results/2026-08-24-edit-clojure-matched-skill-sol-high-10x/05-r05-pair-view-expect-edit-matched-skill-mcp/target.diff [=] U:1
bench/results/2026-08-24-edit-clojure-matched-skill-sol-high-10x/06-r06-pair-view-expect-edit-matched-skill-mcp/target.diff [=] U:1
bench/results/2026-08-24-edit-clojure-matched-skill-sol-high-10x/07-r07-pair-view-expect-edit-matched-skill-mcp/target.diff [=] U:1
bench/results/2026-08-24-edit-clojure-matched-skill-sol-high-10x/08-r08-pair-view-expect-edit-matched-skill-mcp/target.diff [=] U:1
bench/results/2026-08-24-edit-clojure-matched-skill-sol-high-10x/09-r09-pair-view-expect-edit-matched-skill-mcp/target.diff [=] U:1
bench/results/2026-08-24-edit-clojure-matched-skill-sol-high-10x/10-r10-pair-view-expect-edit-matched-skill-mcp/target.diff [=] U:1
bench/results/2026-08-24-edit-clojure-matched-skill-sol-high-10x/MANIFEST.sha256 [=] P:11,25,37,39,41,79,95
bench/results/2026-08-24-edit-clojure-matched-skill-sol-high-v2/01-r01-pair-view-expect-edit-matched-skill-mcp/target.diff [=] U:1
bench/results/2026-08-24-edit-clojure-matched-skill-sol-high-v2/02-r02-pair-view-expect-edit-matched-skill-mcp/target.diff [=] U:1
bench/results/2026-08-24-edit-clojure-matched-skill-sol-high-v2/03-r03-pair-view-expect-edit-matched-skill-mcp/target.diff [=] U:1
bench/results/2026-08-24-edit-clojure-matched-skill-sol-high-v2/MANIFEST.sha256 [=] P:9
bench/results/2026-08-24-edit-clojure-native-sol-high-3x/01-r01-pair-view-expect-edit-no-skill-native/target.diff [=] U:1
bench/results/2026-08-24-edit-clojure-native-sol-high-3x/02-r02-pair-view-expect-edit-no-skill-native/target.diff [=] U:1
bench/results/2026-08-24-edit-clojure-native-sol-high-3x/03-r03-pair-view-expect-edit-no-skill-native/target.diff [=] U:1
bench/results/2026-08-24-edit-clojure-sol-high-v3/01-r01-pair-view-expect-edit-mcp-hint-no-skill-mcp/target.diff [=] U:1
bench/results/2026-08-24-edit-clojure-sol-high-v3/02-r02-pair-view-expect-edit-mcp-hint-no-skill-mcp/target.diff [=] U:1
bench/results/2026-08-24-edit-clojure-sol-high-v3/03-r03-pair-view-expect-edit-mcp-hint-no-skill-mcp/target.diff [=] U:1
bench/results/2026-08-24-edit-clojure-sol-high-v3/MANIFEST.sha256 [=] P:25
bench/results/2026-08-24-edit-clojure-sol-high-v3/runs.tsv [=] P:2
bench/results/2026-08-26-selector-recovery-release/receipt.edn [=] U:25; P:38
bench/results/2026-08-29-prefill-decode-ratio/run-clean/SUMMARY.md [=] a:2; s:18
bench/results/2026-08-29-prefill-decode-ratio/run-clean/env_after.json [=] a:1
bench/results/2026-08-29-prefill-decode-ratio/run-clean/meta.json [=] a:1
bench/results/2026-08-29-prefill-decode-ratio/run-clean/score.json [=] a:13; s:109
bench/results/2026-08-29-prefill-decode-ratio/run-fleet-floor/SUMMARY.md [=] a:2; s:18
bench/results/2026-08-29-prefill-decode-ratio/run-fleet-floor/env_after.json [=] a:1
bench/results/2026-08-29-prefill-decode-ratio/run-fleet-floor/meta.json [=] a:1
bench/results/2026-08-29-prefill-decode-ratio/run-fleet-floor/score.json [=] a:13; s:109
bench/results/2026-08-29-prefill-decode-ratio/sweep-131000/SUMMARY.md [=] a:2; s:18
bench/results/2026-08-29-prefill-decode-ratio/sweep-131000/env_after.json [=] a:1
bench/results/2026-08-29-prefill-decode-ratio/sweep-131000/meta.json [=] a:1
bench/results/2026-08-29-prefill-decode-ratio/sweep-131000/score.json [=] a:13; s:109
bench/results/2026-08-29-prefill-decode-ratio/sweep-262000/SUMMARY.md [=] a:2; s:18
bench/results/2026-08-29-prefill-decode-ratio/sweep-262000/env_after.json [=] a:1
bench/results/2026-08-29-prefill-decode-ratio/sweep-262000/meta.json [=] a:1
bench/results/2026-08-29-prefill-decode-ratio/sweep-262000/score.json [=] a:13; s:109
bench/results/2026-08-29-prefill-decode-ratio/sweep-524000/SUMMARY.md [=] a:2; s:18
bench/results/2026-08-29-prefill-decode-ratio/sweep-524000/env_after.json [=] a:1
bench/results/2026-08-29-prefill-decode-ratio/sweep-524000/meta.json [=] a:1
bench/results/2026-08-29-prefill-decode-ratio/sweep-524000/score.json [=] a:13; s:109
bench/results/2026-08-31-prepared-confirm-preview-installed-route/cli-install-receipt.edn [=] U:5-7
bench/results/2026-08-31-prepared-confirm-preview-installed-route/codex-skill-install-receipt.edn [=] U:5-6; P:4,6
bench/results/2026-08-31-prepared-confirm-preview-installed-route/report.json [=] P:1
bench/results/2026-08-31-prepared-confirm-preview-installed-route/wire/05-commit.response.body [=] U:3
bench/run_anvil_calibration.sh [=] a:4-5
bench/run_anvil_compiled_edit_canary.sh [=] a:4-5
bench/run_anvil_format_extraction.sh [=] a:4-5,35,37,45-46
bench/run_anvil_portfolio_pair.sh [=] a:4-6,33,35,50-53,56
bench/run_anvil_public_cfp_cleanup.sh [=] a:6-7,9
bench/run_clean_codex.sh [Δ] a:983-984,986-988,990,992; s:151,153,778,1355,1358,1957,1971
bench/run_extraction_tool_surface_capture_screen.sh [Δ] P:16
bench/run_inspect_mcp_benchmark.sh [Δ] P:141
bench/run_relation_causal_cohort.sh [=] s:189
bench/run_selector_continuation_benchmark.clj [=] P:25
bench/score_prefill_decode_ratio.clj [=] s:217
```

bin

```text
bin/install-preflight [Δ] s:5
bin/typist-run [Δ] f:23-24,26,47,122,235,362,366-369,373,703,922,935,949,1045,1060,1065,1070,1111,2232,2255,2723,2731,2898-2899,3146-3147,3155; a:373,1305; v:26,47,122,235,362,366,369,922,935,949,1045,1060,1065,1070,1111,2232,2723,2731,2898-2899,3147; y:1062,1112,2722; H:23-24,367-368,703,2255,3146,3155; T:1062,1112,2722
bin/typist-run-test [Δ] f:26,30-31,56,66,121,135,186,225,250-251,327,382,384,395,405,422-423,436,439,528,667-668,680,682,695,698,722,727,741,745; v:26,30-31,56,66,121,327,528; H:135,186,225,250-251,382,384,395,405,422-423,436,439,667-668,680,682,695,698,722,727,741,745
bin/typist_transport.py [Δ] f:20-21,97; H:20-21
bin/with-lock [Δ] s:4
```

dev

```text
dev/audit_direct_cclsp_clients.py [=] P:27
dev/experiments/contention_witness.sh [Δ] f:14,17,20; v:14,17,20
dev/experiments/edit_field_alias_capture_manifest.edn [=] P:30
dev/experiments/extraction_tool_surface_screen.edn [=] a:77; P:44
dev/experiments/owner_aware_symbol_migration.clj [=] U:12
dev/experiments/verify_installed_prepared_actions.py [=] P:96
```

docs

```text
docs/architecture-stack.md [=] P:32,34
docs/clojure-edit-comparison.md [=] s:34
docs/closure-catalogue.md [=] s:148
docs/code-reader-explorer-frontier.md [=] s:118,174,176,248,263,350,468,480,487
docs/compiled-editing-playbook.md [=] a:111,135-137,139,145
docs/experiments/2026-08-31-threeway-acid-wall-battery-prereg.md [=] a:40; s:67,88,107,126,160; P:155,160
docs/experiments/2026-08-31-w1-product-cohort-prereg.md [=] P:20
docs/gene-peek-report.md [=] a:3,9
docs/high-level-design.md [Δ] f:1366; a:659,725; v:1366; s:6,61,194,236-237,310,576,815,903,1077,1115,1121,1126
docs/intent/2026-08-29-ratification/README.md [=] s:30,67
docs/intent/2026-08-30-prepared-request-ratification/prepared-request-specs.from-docs--prepared-request-recovery-lld-20260830.md [=] s:33
docs/intent/2026-08-30-prepared-request-ratification/prepared-request-specs.from-experiment--prepared-request-proxy-screen-20260830.md [=] s:35
docs/intent/2026-08-30-prepared-request-ratification/prepared-request-specs.md [=] s:34
docs/intent/2026-08-30-splice-reference-ratification/README.md [=] s:24,319
docs/intent/agent-routing/agent-routing-design.md [Δ] f:17,19; v:19; H:17
docs/intent/agent-routing/agent-routing-specs.md [Δ] f:31,38,47,51; v:31,38,47,51; H:38
docs/intent/alias-migration/alias-migration-design.md [Δ] f:49,379; v:49,379; s:68,391,411,443
docs/intent/alias-migration/alias-migration-specs.md [Δ] f:54,94-95,104; v:54,94-95; s:42,103-104,106,110,146
docs/intent/alias-migration/receipt-artifacts-specs.md [Δ] f:3; v:3
docs/intent/embedded-elaborator/embedded-elaborator-design.md [=] s:124,311,438,459
docs/intent/feature-thread/feature-thread-design.md [Δ] f:4,9; a:4,9
docs/intent/feature-thread/feature-thread-specs.md [Δ] f:4,9; a:4,9; s:160
docs/intent/helper-extraction/namespace-split-design.md [Δ] f:166,429; v:166; s:9,152,197,265,275,298
docs/intent/helper-extraction/namespace-split-papercuts.edn [Δ] s:126,128,137-138,140
docs/intent/helper-extraction/namespace-split-specs.md [Δ] s:52,74
docs/intent/hot-verification/hot-verification-design.md [Δ] s:48
docs/intent/insertion-boundary-and-gap/design.md [=] s:45,69,92,101,114
docs/intent/mcp-operation-contract/admit-clojure-patch-specs.md [Δ] f:1098,1133,1229,1313,1334,1385,1433; a:1653; s:26,374,891,906,1003,1702; H:1098,1133,1229,1313,1334,1385,1433; P:1675
docs/intent/mcp-operation-contract/mcp-operation-contract-design.md [Δ] f:46; a:882; s:756,849,1035,1392
docs/intent/mcp-operation-contract/mcp-operation-contract-specs.md [Δ] f:135,213,319; s:83,121,213,294
docs/intent/memory-boundedness/design.md [Δ] s:39
docs/intent/memory-boundedness/memory-boundedness-specs.md [Δ] f:104,111,127,155,159,218; H:218
docs/intent/memory/memory-transaction-design.md [Δ] a:17; s:43,65
docs/intent/memory/memory-transaction-specs.md [Δ] f:48,106; s:50,52,70,72
docs/intent/operation-algebra/operation-algebra-design.md [Δ] s:385,432
docs/intent/operation-algebra/operation-algebra-specs.md [Δ] f:12
docs/intent/performance-regression-sentinel/performance-regression-sentinel-design.md [=] a:565,654,668,684; P:598
docs/intent/performance-regression-sentinel/performance-regression-sentinel-specs.md [=] a:77,80-81; P:79
docs/intent/prepared-request-actions/prepared-request-actions-design.md [=] f:201
docs/intent/prepared-request-actions/prepared-request-actions-specs.md [=] f:21,64
docs/intent/prepared-request/prepared-request-specs.md [=] s:39
docs/intent/read-path-memory/read-path-memory-design.md [Δ] a:109
docs/intent/read-path-memory/read-path-memory-specs.md [Δ] s:27
docs/intent/read-request-normalization/read-request-normalization-design.md [=] s:38,76,132
docs/intent/relation-census/relation-census-design.md [Δ] s:15,66,78
docs/intent/relation-census/relation-census-specs.md [Δ] s:20,35,46,93-94
docs/intent/shell-argv-safety/shell-argv-safety-design.md [=] a:123
docs/intent/sibling-pair-edit/sibling-pair-edit-design.md [=] s:203; P:249
docs/intent/substantiation-telemetry/README.from-audit--substantiation-telemetry-second-successor-20260830.md [=] P:34
docs/intent/substantiation-telemetry/README.from-audit--substantiation-telemetry-w1-rebase-20260831.md [=] P:34
docs/intent/substantiation-telemetry/README.from-docs--substantiation-telemetry-ratification-20260830.md [=] P:34
docs/intent/substantiation-telemetry/README.from-experiment--substantiation-overhead-20260830.md [=] P:34
docs/intent/substantiation-telemetry/README.from-experiment--substantiation-overhead-w1-rebase-20260831.md [=] P:34
docs/intent/substantiation-telemetry/README.from-feature--substantiation-telemetry-w1-rebase-20260831.md [=] P:34
docs/intent/substantiation-telemetry/README.md [=] P:33
docs/intent/substantiation-telemetry/substantiation-telemetry-frozen-red.md [=] f:17
docs/intent/substantiation-telemetry/substantiation-telemetry-specs.from-audit--substantiation-telemetry-second-successor-20260830.md [=] f:35,69,73
docs/intent/substantiation-telemetry/substantiation-telemetry-specs.from-audit--substantiation-telemetry-w1-rebase-20260831.md [=] f:35,69,73
docs/intent/substantiation-telemetry/substantiation-telemetry-specs.from-docs--substantiation-telemetry-ratification-20260830.md [=] f:35,69,73
docs/intent/substantiation-telemetry/substantiation-telemetry-specs.from-experiment--substantiation-overhead-20260830.md [=] f:35,69,73
docs/intent/substantiation-telemetry/substantiation-telemetry-specs.from-experiment--substantiation-overhead-w1-rebase-20260831.md [=] f:35,69,73
docs/intent/substantiation-telemetry/substantiation-telemetry-specs.from-feature--substantiation-telemetry-w1-rebase-20260831.md [=] f:35,69,73
docs/intent/substantiation-telemetry/substantiation-telemetry-specs.md [=] f:34,68,72
docs/intent/temp-dir-hygiene/temp-dir-hygiene-design.md [Δ] f:19,22,41,138; a:11; v:19,22,41; T:24
docs/intent/temp-dir-hygiene/temp-dir-hygiene-specs.md [Δ] f:104,119; a:148; v:104,119
docs/intent/test-isolation/test-isolation-design.md [Δ] s:123
docs/intent/test-isolation/test-isolation-specs.md [Δ] f:71,419; a:485; v:419; s:288,430; H:71
docs/intent/worktree-lifecycle/worktree-lifecycle-consistency.md [=] s:30
docs/intent/worktree-lifecycle/worktree-lifecycle-design.md [=] s:74,77,188,272,315,657,672
docs/intent/worktree-lifecycle/worktree-lifecycle-edge-audit.md [=] s:70,96,124
docs/intent/worktree-lifecycle/worktree-lifecycle-specs.md [=] s:60-61,63
docs/intent/write-refusal-completeness/write-refusal-completeness-consistency-report.md [=] s:17
docs/intent/write-refusal-completeness/write-refusal-completeness-design.md [=] s:336
docs/mcp-publishing-dev.md [=] U:28; P:8,21,24,27,29,44,105
docs/memory-battery.md [Δ] f:31,38,45,98,122,257,259; a:257; H:31,38,45,257,259; T:259
docs/memory-usage.md [=] s:58,68,107,388,396,398; P:121,132,135,390
docs/mission-typist.md [Δ] f:9,22,132; v:9,22,132; s:77
docs/must-fix/006-benchmark-single-writer-isolation.md [=] s:17,43
docs/must-fix/README.md [=] s:4
docs/observations/2026-08-02-captains-log-the-file-became-a-structural-shell.md [=] s:265
docs/observations/2026-08-02-production-migration-in-the-wild.md [=] s:49
docs/observations/2026-08-03-captains-log-siblings-became-a-sequence.md [=] s:16
docs/observations/2026-08-03-captains-log-will-the-agent-choose-the-scalpel.md [=] s:182
docs/observations/2026-08-03-clean-codex-benchmark.md [=] s:140
docs/observations/2026-08-04-captains-log-a-clean-claude-caller.md [=] s:68,194
docs/observations/2026-08-04-captains-log-one-read-surface.md [=] s:121,867,1092
docs/observations/2026-08-04-captains-log-source-became-data.md [=] s:138,155
docs/observations/2026-08-05-captains-log-the-visible-skill-was-never-activated.md [=] s:56,111
docs/observations/2026-08-06-captains-log-a-representative-reroll-turned-the-race-into-a-tie.md [=] s:34
docs/observations/2026-08-06-captains-log-from-microscope-to-intent-transaction.md [=] T:48
docs/observations/2026-08-06-captains-log-the-same-trigger-split-the-callers.md [=] s:388,391,396,447
docs/observations/2026-08-06-captains-log-the-transaction-landed-but-reading-still-paid-per-question.md [=] s:337,364,472,479,511,759-760,802,882,900,947,1049
docs/observations/2026-08-07-captains-log-batched-inspect-missed-the-keep-gate.md [=] s:12
docs/observations/2026-08-07-captains-log-editing-from-inside-the-tool.md [=] s:56
docs/observations/2026-08-07-captains-log-end-to-end-structural-transaction-timings.md [=] P:426
docs/observations/2026-08-07-captains-log-rent-the-graph-keep-the-transaction.md [=] s:46,102,117,133,220
docs/observations/2026-08-07-captains-log-seven-out-of-ten-the-scalpel-was-fast-the-map-timed-out.md [=] s:5,12,24,98,116,132,158,166,168,215; U:24
docs/observations/2026-08-07-captains-log-the-decision-became-a-transaction.md [=] P:51,164,168
docs/observations/2026-08-07-captains-log-the-tool-contract-became-live.md [=] s:329,383,779; P:312
docs/observations/2026-08-07-clj-surgeon-dogfood-friction-report.md [=] s:382,386; P:214
docs/observations/2026-08-08-captains-log-last-nights-hill-climb.md [=] f:411; s:89,202
docs/observations/2026-08-08-captains-log-purgatory-was-deleted.md [=] s:3,17,37,49,94,105,148,192,198
docs/observations/2026-08-08-captains-log-seven-when-connected-three-when-not.md [=] s:3,208
docs/observations/2026-08-08-ethnographic-report-mothership-purgatory-retirement.md [=] s:1,5,17,26,30,33,47-48,55,68,86,250; U:5
docs/observations/2026-08-10-captains-log-nine-out-of-ten-the-monolith-became-a-map.md [=] s:19,116
docs/observations/2026-08-10-captains-log-the-scalpel-was-fast-the-refactor-was-not-compiled.md [=] s:483
docs/observations/2026-08-23-anvil-sol-high-interface-study-interim.md [=] a:1,7,15,28,36,42,46,238,263,290; U:254,279
docs/observations/2026-08-23-anvil-startup-memory-measurement-plan.md [=] a:1,10,314; s:154; P:76,80
docs/observations/2026-08-23-anvil-startup-memory-static-a.md [=] s:390
docs/observations/2026-08-23-anvil-startup-memory-storyboard.html [=] a:6,134,252
docs/observations/2026-08-23-clojure-tooling-memory-and-load-architecture-review.md [=] s:42,162,213,255-256,335,339,353,422,459; U:21-22,25-26,30-32,59-61,74,76,104-105,127-129,133,142,158-159,165,186,232,235,241-242,256,333-334,346,364,446-461
docs/observations/2026-08-24-anvil-round-two-and-startup-memory-synthesis.md [=] a:1,16,90,104,136,176-179
docs/observations/2026-08-24-captains-log-the-guarded-keystroke-beat-native.md [=] a:14,193,229,324; s:205,344
docs/observations/2026-08-24-compact-editor-versus-native-pilots.md [=] a:538,541,548; s:378
docs/observations/2026-08-24-ethnography-the-scalpel-must-earn-every-call.md [=] s:104,176
docs/observations/2026-08-24-mcp-startup-heap-breakthrough.md [=] U:33,47-50
docs/observations/2026-08-24-sci-programmable-edit-dogfood.md [=] f:62; a:4,176,178,207,222; s:129,227
docs/observations/2026-08-25-captains-log-every-operation-got-a-clock.md [=] s:43,45
docs/observations/2026-08-25-captains-log-the-bottleneck-was-decision-fragmentation.md [=] a:35
docs/observations/2026-08-25-captains-log-the-compiled-cleanup-hit-four-point-six-x.md [=] a:4,10,27,34,104,110,130,132,178,207,215,249,270,277-279,281,295; s:5; H:225,227,284; U:238; P:208,210,271
docs/observations/2026-08-25-owner-hallucinations-need-one-shot-evidence.md [=] U:128,130
docs/observations/2026-08-26-captains-log-codex-silence-has-shape.md [=] a:143,145,166,182
docs/observations/2026-08-26-captains-log-prompt-wins-plan-handle-stops.md [=] P:117
docs/observations/2026-08-26-captains-log-read-refusals-become-codebase-esp.md [=] a:69,77; U:166; P:202
docs/observations/2026-08-26-captains-log-selector-evidence-has-a-crossover.md [=] P:44
docs/observations/2026-08-26-captains-log-single-pass-extraction-survived-adversarial-acceptance.md [=] U:101
docs/observations/2026-08-26-captains-log-the-best-plan-was-no-plan.md [=] a:124; s:144,180
docs/observations/2026-08-26-captains-log-the-extraction-created-its-own-room.md [=] a:18,120,127; s:203,250
docs/observations/2026-08-26-captains-log-the-extraction-plan-became-a-next-call.md [=] P:52
docs/observations/2026-08-26-captains-log-three-days-of-option-value.md [=] a:12,50
docs/observations/2026-08-26-ethnography-complete-decisions-should-not-pay-for-planning.md [=] a:153,167,174
docs/observations/2026-08-26-hypothesis-surgeon-syntax-first-cclsp-escalation.md [=] s:28,35
docs/observations/2026-08-26-three-day-progress-assessment.md [=] a:137
docs/observations/2026-08-26-three-day-speed-option-portfolio.md [=] a:15; s:33,37-48,55
docs/observations/2026-08-27-agent-usage-release-window.md [=] s:161
docs/observations/2026-08-27-captains-log-direct-cclsp-clients-retired.md [=] s:39; U:61,63,68,70,74; P:88,102
docs/observations/2026-08-27-captains-log-one-algebra-two-entrances.md [=] f:15; a:129,131; U:137
docs/observations/2026-08-27-captains-log-sol-kept-its-chord-claude-found-it.md [=] a:59,148,167,206,216,229,242,252,260; U:229,242; P:249
docs/observations/2026-08-27-captains-log-terminal-proof-ended-the-second-plan.md [=] a:111,143,145,174,203,256; s:371; U:289; P:275,293,318
docs/observations/2026-08-27-captains-log-the-analyzer-took-the-lock-with-it.md [=] a:267,291,307,310; s:46,151,155-157; U:24,155-156,338
docs/observations/2026-08-27-captains-log-the-language-server-was-mostly-starting-itself.md [=] s:17,347; U:157-159,167-168,170,264,266,276,446-452,457,460,468,481
docs/observations/2026-08-27-captains-log-the-model-boundary-dwarfed-the-scalpel.md [=] U:218-219,222
docs/observations/2026-08-27-captains-log-tool-names-enter-the-arena.md [=] a:14,144,159,184,287,352; P:296,381
docs/observations/2026-08-27-captains-log-var-surface-was-a-small-index.md [=] s:22,169
docs/observations/2026-08-27-cli-mcp-causal-transport-parity-receipt.md [=] a:14,80,87,133; U:26
docs/observations/2026-08-27-clj-kondo-trigger-taxonomy-and-test-pyramid.md [=] a:151,196; U:75,80,87
docs/observations/2026-08-27-mutation-tool-naming-ethnography.md [=] s:108
docs/observations/2026-08-27-operation-algebra-no-model-subprocess-parity.md [=] U:108; P:103
docs/observations/2026-08-27-selector-continuation-counterfactual-receipt.md [=] P:27-28,125
docs/observations/2026-08-28-captains-log-catalog-screen-verdict-withdrawn.md [=] a:164; s:48; U:168; P:190,192
docs/observations/2026-08-28-captains-log-location-mistakes-became-compiler-input.md [=] a:96,111,189,217,236; U:221
docs/observations/2026-08-28-captains-log-tool-names-enter-the-real-arena.md [=] a:40,69,156,190,232,743; s:411; U:703
docs/observations/2026-08-28-owner-aware-call-construction-screen-design.md [=] a:3,164,169
docs/observations/2026-08-28-performance-vs-native-timeline.md [=] a:35,38,82-83,89,116,146; U:116
docs/observations/2026-08-28-performance-vs-native-timeline.tsv [=] a:20-21,25,29,36
docs/observations/2026-08-29-action-emission-clock-design-audit.md [=] P:36
docs/observations/2026-08-29-agent-usage-after-the-alias-breakthrough.md [=] a:66,119
docs/observations/2026-08-29-alias-replication-harness-adversarial-audit.md [=] P:27,70
docs/observations/2026-08-29-alias-replication-ledger-independent-reaudit.md [=] a:186; U:120; P:25,34,54
docs/observations/2026-08-29-captains-log-closed-aliases-removed-the-schema-cliff.md [=] a:26; U:148
docs/observations/2026-08-29-captains-log-closed-relations-earned-a-hold.md [=] a:169; P:265
docs/observations/2026-08-29-captains-log-location-tolerance-met-a-different-schema-cliff.md [=] a:9; U:132
docs/observations/2026-08-29-captains-log-positional-authority-cli-release.md [=] U:67,104; P:111
docs/observations/2026-08-29-captains-log-positional-mutation-authority-purged.md [=] s:35; P:23
docs/observations/2026-08-29-captains-log-relations-made-the-whole-decision-visible.md [=] s:20; H:170
docs/observations/2026-08-29-captains-log-request-shape-compression-screen.md [=] s:155; U:33; P:54
docs/observations/2026-08-29-captains-log-retained-relations-cleared-the-number-not-the-gate.md [=] a:9
docs/observations/2026-08-29-captains-log-six-designs-died-and-the-tool-was-barely-used.md [=] a:59; s:41,50,123,147,281,284
docs/observations/2026-08-29-captains-log-the-490-ms-tax-became-41-ms.md [=] a:13
docs/observations/2026-08-29-captains-log-the-ledger-had-two-sides.md [=] a:14; s:56,123
docs/observations/2026-08-29-captains-log-the-model-typed-less-it-did-not-think-less.md [=] a:12,129,136,202; s:27,56,190
docs/observations/2026-08-29-captains-log-the-predictions-that-failed.md [=] s:59,72
docs/observations/2026-08-29-captains-log-the-schema-shrank-but-the-decision-did-not.md [=] U:88
docs/observations/2026-08-29-captains-log-three-ways-to-name-the-wrong-thing.md [=] f:4; a:62,116,130,153; s:5,103,123-125
docs/observations/2026-08-29-captains-log-what-the-six-withdrawals-mean.md [=] s:42,49,120
docs/observations/2026-08-29-claude-inspect-result-price-tag-routing-experiment.md [=] P:177
docs/observations/2026-08-29-codex-catalog-floor-sweep-protocol.md [=] a:5,46,84
docs/observations/2026-08-29-compact-edit-field-alias-algebra.md [=] s:159; P:59
docs/observations/2026-08-29-complete-owner-vocabulary-causal-ab.md [=] a:50,145,244; H:204,210,226,369,375; P:216
docs/observations/2026-08-29-correct-flat-control-and-relation-causal-protocol.md [=] a:179,283; U:88
docs/observations/2026-08-29-emission-compression-composition-screen.md [=] f:208; a:208,266; U:257
docs/observations/2026-08-29-emission-compression-option-portfolio.md [=] a:225; U:213
docs/observations/2026-08-29-measurements-and-how-to-repeat-them.md [=] a:40,321; s:225,229,232,337,390
docs/observations/2026-08-29-native-prelanding-parse-gate-prize-audit.md [=] s:155
docs/observations/2026-08-29-owner-aware-call-construction-prerequisite-screen.md [=] s:100,105,112; U:79
docs/observations/2026-08-29-positional-subject-authority-audit.md [=] s:5,11
docs/observations/2026-08-29-retained-flat-relation-admission-audit.md [=] a:8; P:57
docs/observations/2026-08-29-symbol-migration-require-change-product-seam-audit.md [=] a:10; s:69,166,315,318
docs/observations/2026-08-29-three-arm-anvil-execution-envelope.md [=] a:1,8,64,86,98,103,110,124,160,168,238,249; U:227; P:44,118
docs/observations/2026-08-29-three-arm-request-shape-model-screen-protocol.md [=] a:273; P:63
docs/observations/2026-08-29-three-arm-request-shape-zero-model-receipt.md [=] a:9; s:100; U:163
docs/observations/2026-08-29-write-side-emission-and-read-side-encoding-study.md [=] s:170
docs/observations/2026-08-30-acejump-docs-postmortem.md [=] a:190; s:66,68,86,89,149,182,187,216,221-222,256,282,311
docs/observations/2026-08-30-acid-crossover-ladder-result.md [=] a:21,138,159
docs/observations/2026-08-30-acid-regression-crossover-preregistration.md [=] a:10,18,147
docs/observations/2026-08-30-acid-regression-gate-result.md [=] a:15,78
docs/observations/2026-08-30-captains-log-spark-and-the-inverse-surprise.md [=] s:69
docs/observations/2026-08-30-captains-log-the-day-the-routing-story-died-twice.md [=] a:96,123; s:79,93
docs/observations/2026-08-30-captains-log-the-night-the-arrow-walked-itself.md [=] a:43; H:102
docs/observations/2026-08-30-captains-log-the-night-the-verifiers-ran-the-ship.md [=] f:90; s:1,17,84,100
docs/observations/2026-08-30-differential-routing-interview-and-ablations.md [=] s:89
docs/observations/2026-08-30-dream-list-fulfillment-designs.md [=] f:496; s:93,288,875
docs/observations/2026-08-30-embedded-spark-elaborator-probe.md [=] s:70,73,85; U:123-124
docs/observations/2026-08-30-embedded-spark-elaborator-probe/app-server-transcript.jsonl [=] P:52-53,57
docs/observations/2026-08-30-embedded-spark-elaborator-probe/installed-cli-recon.json [=] s:12-13; U:121-122
docs/observations/2026-08-30-from-to-double-repetition-study.md [=] s:362
docs/observations/2026-08-30-gpt-5-3-spark-caller-screen.md [=] a:23,143; s:201
docs/observations/2026-08-30-label-addressed-write-designs.md [=] s:57,479,484,660,763,768
docs/observations/2026-08-30-ordinal-refusal-recovery-screen.md [=] P:115
docs/observations/2026-08-30-prepared-confirm-preview-live-route-token-measurement.md [=] U:124; P:111
docs/observations/2026-08-30-prepared-request-installed-route.md [=] s:77; P:28,36,86
docs/observations/2026-08-30-prepared-request-option-a-proxy-invalid-safety-stop.md [=] a:9; U:54
docs/observations/2026-08-30-prepared-request-option-a-proxy-matched-refusal-stop.md [=] a:9; U:54; P:5,54
docs/observations/2026-08-30-prepared-request-option-a-proxy-protocol.md [=] a:59
docs/observations/2026-08-30-prepared-request-option-a-proxy-safety-root-stop.md [=] a:9; U:58
docs/observations/2026-08-30-prepared-request-option-a-proxy-safety-shape-stop.md [=] a:9; U:55
docs/observations/2026-08-30-prepared-request-option-a-proxy-stdin-stop.md [=] a:9; U:35; P:27,33
docs/observations/2026-08-30-prepared-request-option-a-proxy-valid-verdict.md [=] a:9; U:88; P:90
docs/observations/2026-08-30-prepared-request-replication.md [=] a:5
docs/observations/2026-08-30-read-normalization-live-route-token-measurement.md [=] U:151; P:42,130
docs/observations/2026-08-30-read-request-normalization-install-receipt.md [=] U:72,78; P:61-62,102
docs/observations/2026-08-30-spark-isolation-screen/DESIGN.md [=] U:30
docs/observations/2026-08-30-spark-isolation-screen/RESULT.md [=] U:212
docs/observations/2026-08-30-spark-isolation-screen/lifecycle-kill-3-observer.jsonl [=] P:4
docs/observations/2026-08-30-spark-isolation-screen/lifecycle-oversize-1-observer.jsonl [=] P:181
docs/observations/2026-08-30-spark-isolation-screen/lifecycle-oversize-2-transcript.jsonl [=] P:4
docs/observations/2026-08-30-spark-isolation-screen/matrix-E-B-observer.jsonl [=] P:134,148,338,473,479,577,582,587
docs/observations/2026-08-30-spark-isolation-screen/matrix-E-B-transcript.jsonl [=] P:10
docs/observations/2026-08-30-spark-isolation-screen/matrix-E-S-observer.jsonl [=] P:142,339
docs/observations/2026-08-30-spark-isolation-screen/matrix-E-S-transcript.jsonl [=] P:10
docs/observations/2026-08-30-spark-isolation-screen/matrix-H-B-observer.jsonl [=] P:391
docs/observations/2026-08-30-spark-isolation-screen/matrix-H-B-transcript.jsonl [=] P:10,108,110-118,121
docs/observations/2026-08-30-spark-isolation-screen/matrix-H-S-transcript.jsonl [=] P:10,23-24,96-97,103,143-151
docs/observations/2026-08-30-spark-isolation-screen/screen-receipt.json [=] P:222,230,2642,3610,4724,4895,4903,7297,7566,7575,8243
docs/observations/2026-08-30-spark-isolation-screen/sha256-manifest.txt [=] P:26
docs/observations/2026-08-30-splice-reference-adversarial-replication-preregistration.md [=] U:156
docs/observations/2026-08-30-splice-reference-adversarial-replication-result.md [=] s:187; U:217
docs/observations/2026-08-30-splice-reference-screen-preregistration.md [=] a:7; U:165; P:20
docs/observations/2026-08-30-splice-reference-screen-result.md [=] a:171; U:195
docs/observations/2026-08-30-substantiation-telemetry-second-successor-independent-go.md [=] P:42
docs/observations/2026-08-30-the-dream-session-north-star.md [=] a:98
docs/observations/2026-08-30-transformation-redesign-exemplars.md [=] s:16,462-463
docs/observations/2026-08-30-write-refusal-001-installed-release.md [=] P:43,89
docs/observations/2026-08-30-write-refusal-001-live-route-token-measurement.md [=] U:112
docs/observations/2026-08-30-write-side-refusal-completeness-audit.md [=] s:102
docs/observations/2026-08-31-caller-transcript-emission-audit.md [=] U:21
docs/observations/2026-08-31-captains-log-the-nine-hour-watch.md [=] f:19; s:71
docs/observations/2026-08-31-embedded-elaborator-production-verification/RESULT.md [=] s:44,78
docs/observations/2026-08-31-embedded-elaborator-production-verification/d1-receipt.json [=] P:37,44
docs/observations/2026-08-31-fast-typist-utilization-designs.md [=] s:128,149,168,188,208,229,248,269,288,307,327,346,363,380,398,417
docs/observations/2026-08-31-prepared-confirm-affinity-live-route-token-measurement.md [=] U:118; P:96
docs/observations/2026-08-31-prepared-confirm-preview-installed-route-receipt.md [=] P:29,62
docs/observations/2026-08-31-prepared-confirmation-affinity-repair.md [=] P:64
docs/observations/2026-08-31-speed-claims-confirmation-audit.md [=] P:19,128,137
docs/observations/2026-08-31-substantiation-telemetry-w1-rebase-overhead-screen.md [=] P:20
docs/observations/2026-08-31-the-fleet-opines-on-the-magic.md [=] s:27,70
docs/observations/2026-08-31-threeway-acid-wall-battery.md [=] s:7,9,17,37,54,87,110
docs/observations/2026-09-01-worktree-registration-prune-implementation.md [=] s:39
docs/observations/2026-09-02-acid-rung-L/L-spec.md [=] f:3
docs/observations/2026-09-02-anvil-builder-seat-brief.md [Δ] f:3,12-13,58; a:1,3,5-7,9,12-13,19,22,29,43,46,56,59; P:4,22-23
docs/observations/2026-09-02-captains-log-bridge-wall-clock-ideal-program.md [=] f:133; a:262,323,325,327,332,342,360,363,390,399,510-511,588,606-607,618,633,671,709,758,760,801,816,823,828,861,889,908,1024,1377,1477,1484,1486,1769,1908; s:228,247,286,335,342,346,368,471,554,558,560,571,588,636,639,644,699,759-760,764-765,790,817,824,826-827,830,832,841,850,852,872,895,900,910,921,932,939,969,1029,1035,1041-1042,1058,1076,1082-1083,1085,1102,1154,1159,1161-1162,1170,1174,1191,1196-1198,1204,1208,1215,1222,1228,1230,1233,1246,1265,1281,1292,1302,1308,1329,1345,1383-1384,1387,1389,1396,1407,1421,1427,1432,1434,1453,1464,1473,1478,1495,1504,1509,1536,1545-1546,1560,1569,1577,1585,1589,1600,1607,1610,1614,1619,1684-1685,1689,1692,1699,1704,1730,1745-1746,1756,1758-1759,1770,1878; P:84,91,218,247,286,311,471,605-606,758-759,817,821,823,829,831,901,1164,1208,1675,1723
docs/observations/2026-09-02-captains-log-the-big-aha-and-reset.md [=] f:156,545,906,3040,3044,3111; a:54,94,157,193,507,547,552,717,722,724,845,864,898,1050,1098,1360,1375,1377,1381,1385,1515,1527,1600,1614,1917,2091,2212,2383,2418,2480,2685,2724,2907,3035,3037-3038,3040,3042,3124,3148,3170,3186,3191,3218,3235,3246,3250,3252,3255; s:19,129,252,258,303,313,316,362,396,410,426-427,435,528,565,573,596,634,707,733,745,883,902,1093,1140,1149,1223,1254,1275,1286,1301,1305,1329,1353,1468,1677,2191,2246,2528,2617,2719,2765,2779,2781,2783,3046,3227; P:507,547,552,834,963,1004,1051,1114,1127-1128,1362,1364,1413,1470,1526,1531,1689,1808,1835,1936,1941,1965,2009,2079,2156,2160,2203,2309,2419,2421,2428,2478,2480,2699,2905,3041,3045,3150; T:53
docs/observations/2026-09-02-gene-peek-report.md [=] f:108,124; a:1,103,108,124; s:5,18
docs/observations/2026-09-02-resume-here-bridge-program.md [=] f:1,221,729,742; a:1,8,15,21,29,50,52-53,66,77,90,93,98,100-101,105,107,115,118,121,126,133,151,168,181,187,200,208,212,217,221,229,241,252,265-266,288,295,306,308,332,373,386,541-543,555,597,608,635,655,715-716,729-730,742,746-747,761,766,770,781; y:26; s:15,17-18,21,50,92,102,156,172,230,235,493,596; P:17,21,52,65,82-83,90,109-110,210,212,222,229,238-240,246,251-252,265-266,268,288,306,308-309,311,327,333-335,374-375,381,386-387,399,402-403,413,416,427,433,448,486,495,510,512,531,543,564,619; T:26
docs/observations/2026-09-02-session-4-native-plan.md [=] f:3; H:3; P:8,244
docs/observations/2026-09-02-slope-spec-sl1.md [=] s:87,109,153
docs/observations/2026-09-02-stranded-lessons-harvest-index.md [=] f:130,319,859,929; s:201; P:233,363,486,779
docs/observations/2026-09-02-tweezer-session-3-watch.md [=] f:3,11,44; s:33,35,37,42,60-61; H:3,11
docs/observations/2026-09-02-tweezer-session-4-watch.md [=] f:3,5,14,56; s:116; H:3,5,14,56
docs/observations/2026-09-02-tweezer-session-5-watch.md [=] f:3,6-7,15; a:61,112; H:3,6-7,15; P:103
docs/observations/2026-09-02-wake-up-brief-surgeon-program.md [=] a:6,118,134; s:5,8,44,47,58,67,125,132,139; P:184
docs/observations/2026-09-03-andon-find-build-files-review-opus.md [=] f:9,34,48,58,419; a:3,9,13,494; s:396; H:9,419; T:419,423-424
docs/observations/2026-09-03-answer-to-mayor-ask.md [=] f:1; a:1,18,29,34; p:34; y:26; P:34; T:26
docs/observations/2026-09-03-anvil-arms-apparatus-round2-sol-NO-GO.md [=] f:5,24,34,40,49,69,71; a:1,11-22,49,69,71,75-84; H:5,24,34,40,49,69,71
docs/observations/2026-09-03-anvil-arms-apparatus-round3-sol-NO-GO.md [=] f:7,15-25,50,66,70,74,89,94; a:1,15-25,48,70,89,94,103-108; H:7,15-25,50,66,70,74,89,94
docs/observations/2026-09-03-anvil-arms-apparatus-round4-sol-review.md [=] f:9-12,28,30,35; a:1,9-14,22,24,26,28,35,42-47; H:9-12,28,30,35
docs/observations/2026-09-03-anvil-arms-apparatus-round5-sol-review.md [=] f:32; a:1,32,39,41,43,45,47,49,51; H:32
docs/observations/2026-09-03-anvil-arms-apparatus-round6-sol-review.md [=] f:12,19,21,23,25,27,29,31,33,43,49,67,80,82,84,86,88,106,108,110,112,114,116,118,120,136,138,140,142,144,146,148,150,152,154,156,158,160; a:1,12,19,21,23,25,27,29,31,33,43,49,67,80,82,84,86,88,106,108,110,112,114,116,118,120,136,138,140,142,144,146,148,150,152,154,156,158,160; H:12,19,21,23,25,27,29,31,33,43,49,67,80,82,84,86,88,106,108,110,112,114,116,118,120,136,138,140,142,144,146,148,150,152,154,156,158,160
docs/observations/2026-09-03-anvil-arms-apparatus-sol-review-NO-GO.md [=] f:7,33,35; a:1,7,11,13,15,17,19,21,23,25,27,29,31,33; H:7,33,35
docs/observations/2026-09-03-anvil-seat-wishlist-for-mayor.md [=] f:29,34,37-38; a:1,3,11,34,36; p:15,34,38; P:15,34-36,38
docs/observations/2026-09-03-brainfleet-hills.md [Δ] f:61,144,215,223,305,330,398,560,565,584,634,638,642,793,902,906-907,924,1021-1022,1139,1348,1485-1487,1490,1655-1656,1814,1898,1953; a:21,35,63,90,120,198,250,586,638,686,906,1139,1485,1491,1504,1953; p:187; y:99,247,670,675,874,898,1388,1945,2249,2313; s:131,149,340,434,790,834,990,1023,1174,1263,1473,1475,1701,2072-2074,2079,2113,2130,2209; H:61,72,144,215,223,305,330,398,560,565,584,634,638,642,793,902,907,924,1021-1022,1348,1486-1487,1490,1655-1656,1814,1898; P:187,216,585; T:99,247,670,675,874,898,1388,1945,2249,2313
docs/observations/2026-09-03-captains-log-anvil-seat.md [Δ] f:6,9,11,18,200,314,622,630,670,709,717,746,757,759,765,767,777,793,797,807,815,827,841,849,871,881,887,911,913,919,1003,1011,1017,1035,1045,1069,1098,1238,1246,1268,1294,1381,1405,1409,1429,1436,1467,1503,1661,1699,1715,1734,1835,1927,1938,1979,1985,2089,2141,2143,2147,2190,2222,2232,2243,2253,2286,2288,2315,2409,2429,2457,2481,2487,2579,2611,2651,2669,2677,2687,2795,2805,2817,2821,2831,2833,2841,2845,2855,2887,2909,2931,2935,2939,2945,2951,2953,2961,2971,2973,2999,3003,3007,3046,3048,3068,3083,3112,3140,3176,3217,3328,3332,3344,3348,3352,3384,3386,3398,3448,3482,3707,3776,3784,3830,3834,3957,3973,3988,4007,4009,4050,4164,4172,4178,4189,4197,4226,4236,4265,4267,4326,4347,4349,4351,4361,4363,4389,4391,4397,4403,4459,4465; a:1,6-7,9,18,20,90,133,174,198,200,204,293,318,322,359,361,367,369,451,465,477,487,686,711,723,734,742,753,761,769,853,855,879,909,959,1011,1013,1017,1077,1192,1284,1300,1720,1776,1835,2141,2147,2260,2285,2315,2429,2465,2651,2803,3083,3384,3398,3545,3547,3597,3613,3623,3671,3759,3768,3930,3947,3961,3999,4057,4139,4393,4403,4407,4413; p:196,200,208,660,951,1013,1061,1069,1904,1938,1977,1983,2021,2073,2075,2141,2143,2147,2151,2155,2253,2657,2867,3038,3112,3118,3120,3316,3350,3352,3354,3370,3374,3380,3382,3384,3386,3448,3492,3555,3581,3597,3631,3805,3914,4022,4293,4365,4397,4421,4435,4441; v:1238,1246,1268,1405,1409,1436,1661,1699,1835,1927,1938,1985,2089,2222,2232,2243,2253,2286,2288,2409,2429,2457,2481,2487,2611,2669,2677,2687,2795,2805,2817,2821,2831,2833,2841,2845,2855,2887,2909,2931,2935,2951,2953,2973,2999,3003,3007,3046,3048,3068,3112,3140,3176,3217,3328,3332,3344,3348,3398,3830,3834,3957,3973,3988,4009,4164,4172,4189,4267,4349,4351,4389,4391,4397,4459,4465; y:318,481,626,909,921,1023,1027,1035,1053,1073,1238,1822,2049,2163,3392,3394,3418,3420,3569,3571,3593,3602,3607,3707,3747,4385; r:4395,4403,4405,4419,4435,4480; s:208,268,270,497,600,617,622,630,632,643,670,699,705,717,740,797,839,841,851,853,863,887,889,1092,1238,1268,1461,1774,2147,2149,2151,2635,2817,2871,2887,2995,3081,3146,3209,3478,3571,3617,3639,3715,3727,3735,3751,4003,4147,4153,4155,4157,4168,4170,4172,4174,4189,4220,4222,4232,4238,4240,4263,4269,4279,4289,4293,4295,4310,4312,4314,4318,4320,4322,4326,4328,4330,4332,4349,4351,4365,4373,4387,4389,4391,4393,4395,4401,4403,4405,4409,4417,4419,4421,4433,4435,4439,4441,4480; H:767,777,871,913,919,1003,1011,1035,1045,1069,1098,1294,1381,1503,1715,1734,1979,2143,2147,2579,3352,3386,3448,3973,4050,4178,4197; P:10,174,196,200,204,207-208,210,220,481,483,626,660,676,853,861,863,869,877,951,953,955,995,1013,1051,1061,1069,1292,1294,1298,1835,1904,1938,1977,1983,2021,2073,2075,2077,2083,2085,2141,2143,2147,2151,2155,2217-2218,2236,2253,2657,2803,2867,2935,3038,3046,3112,3118,3120,3152,3156,3316,3350,3352,3354,3370,3374,3380,3382,3384,3386,3448,3492,3555,3581,3597,3631,3768,3805,3914,3983,4022,4293,4308,4365,4397,4421,4435,4441; T:318,477,481,485,533,626,909,921,955,1003,1023,1027,1035,1053,1073,1238,1246,1331,1822,1962,1966,2004,2009,2049,2069,2087,2099,2105,2119,2121,2163,2219,2611,2623,2793,2955,3352,3358,3392,3394,3418,3420,3569,3571,3593,3602,3607,3617,3625,3707,3747,3930,3934,3947,3949,3957,3969,3973,4045,4052,4147,4155,4172,4385,4389,4395,4397,4403,4405,4419,4433,4435,4480
docs/observations/2026-09-03-census-round10-rereview.md [=] f:11,13,15,17,19,21,23,25,27; H:11,13,15,17,19,21,23,25,27; T:27
docs/observations/2026-09-03-census-round11-rereview.md [=] f:9,11,13,15,17,19,21,23,25,27; H:9,11,13,15,17,19,21,23,25,27; T:27
docs/observations/2026-09-03-census-round12-rereview.md [=] f:9,11,13,15,17,19,21,23,25,27,29,31,33,35,37; H:9,11,13,15,17,19,21,23,25,27,29,31,33,35,37; T:37
docs/observations/2026-09-03-census-round13-rereview.md [=] f:11,13,15,17,19,21,23,25,27; H:11,13,15,17,19,21,23,25,27; P:3,29; T:27
docs/observations/2026-09-03-census-round14-rereview.md [=] f:11,13,15,17,19,21,23,25,27; H:11,13,15,17,19,21,23,25,27; T:29
docs/observations/2026-09-03-census-round15-rereview-opus.md [=] f:16,27,48,66,68,70,99,101,115-116,141,148,150,176,198,218,227,242,262,271,285,295,337,341,344,347,350,353,356,359; p:18; H:16,27,48,66,68,70,99,101,115-116,141,148,150,176,198,218,227,242,262,271,285,295,337,341,344,347,350,353,356,359; P:18; T:300
docs/observations/2026-09-03-census-round3-rereview-NO-GO.md [=] f:28; H:28; T:28
docs/observations/2026-09-03-census-round4-rereview.md [=] f:33; H:33; T:33
docs/observations/2026-09-03-census-round5-rereview.md [=] T:23
docs/observations/2026-09-03-census-round9-rereview.md [=] f:11,13,15,17,19,21,23; H:11,13,15,17,19,21,23; T:23
docs/observations/2026-09-03-delegation-techniques-the-how.md [=] f:10,112,115,117,128,167,190,219-220; a:10,113,117,156,178,251,264,303,330; p:168,185,204,304; y:150,162,199; s:135,137,416; H:112,115,117,128,167,190,219-220; P:114,168,185,204,304; T:150,162,199
docs/observations/2026-09-03-folddiff-round10-rereview-opus.md [=] f:9,80,235; s:25,73,189,214,224,277; H:9,235; T:235
docs/observations/2026-09-03-folddiff-round11-rereview-opus.md [=] f:9,291,338; s:66,160,234; H:9,291; T:291
docs/observations/2026-09-03-folddiff-round3-rereview.md [=] f:36; H:36; T:34
docs/observations/2026-09-03-folddiff-round4-rereview.md [=] T:7
docs/observations/2026-09-03-folddiff-round5-rereview.md [=] T:10
docs/observations/2026-09-03-folddiff-round6-rereview.md [=] T:15
docs/observations/2026-09-03-folddiff-round8-rereview.md [=] f:13,17,32,56,71,73,75; H:13,17,32,56,71,73,75
docs/observations/2026-09-03-folddiff-round9-rereview-opus.md [=] f:12,20,116-117,130,223,311; y:14-15; s:70,140,168; H:12,223; T:14-15,223
docs/observations/2026-09-03-gene-report-night.md [=] f:43,45,131,234,284,364; a:2,9,18,22,87,105-106,234,241,255,284,301,321,367,430; p:65,253,256,308; y:43,45,262; s:103,105,107,114,138,160,259,413; H:43,45,234; P:65,105,253,256,308,394,460,462; T:43,45,262,287,308
docs/observations/2026-09-03-integration-branch.md [Δ] f:3,11-12,326; a:3,126,269,272,313; s:74,144,159,222; H:11-12,326; T:104,107,114,304-305
docs/observations/2026-09-03-magic-moments-tweezer-watcher-anvil.md [=] f:11,13,200-201; a:1,3,6,13,62,64,66,69,98,183,200,204,209,269,280; p:245; s:178; P:200,243,245
docs/observations/2026-09-03-mem-003-round3-rereview-opus.md [=] f:10,15,26,33,37,197,273; H:10,197; T:197
docs/observations/2026-09-03-mem-003-round4-rereview-opus.md [=] f:10,136,363,365,371; s:79; H:10,363,365,371; T:363,365,371,385
docs/observations/2026-09-03-mem-003-round5-rereview-opus.md [=] f:10,13,40,381,383,389; s:307; H:10,13,40,381,383,389; T:381,383,389
docs/observations/2026-09-03-mem-003-round6-rereview-opus.md [=] f:10,13,311,314,319; s:100; H:10,13,311,314,319; T:311,314,319
docs/observations/2026-09-03-mem-003-round7-rereview-opus.md [=] f:9,307,310,314,322,325; p:13; s:21,43,109,175,200,256,280,284,342; H:9,307,310,314,322,325; P:13; T:307,310,314,322,325
docs/observations/2026-09-03-mem-003-round8-rereview-opus.md [=] f:11; p:17; s:36; H:11; P:5,16-17; T:90,94-97
docs/observations/2026-09-03-mem-003-sol-review.md [=] f:13,19; s:35; H:13
docs/observations/2026-09-03-mem-005-opus-review.md [=] f:8,201; a:9; s:180; H:8,201; T:194-197
docs/observations/2026-09-03-mem-005-parser-admission.md [Δ] f:140-141; a:5,286; s:63,313; H:140-141; T:325
docs/observations/2026-09-03-mem-005-round2-rereview-opus.md [=] f:9,392; a:10; s:400,420; H:9,392; T:354,380
docs/observations/2026-09-03-mem-005-round3-rereview-opus-GO.md [=] f:11-13,341; a:14; p:342; H:11-13,341; P:342; T:331
docs/observations/2026-09-03-mem-015-single-parse.md [Δ] f:5,25,29,124,202,228-232; a:3; H:5,25,29,124,202,228-232; T:202
docs/observations/2026-09-03-mem-015-sol-review.md [=] T:27
docs/observations/2026-09-03-memory-battery-baseline.md [Δ] f:6,153; a:3,145; H:6,153
docs/observations/2026-09-03-memory-battery-round2-sol-review.md [=] f:19,25,55; s:39; H:19,25; P:46
docs/observations/2026-09-03-memory-battery-sol-review.md [=] f:81,128; H:81,128; P:79,156
docs/observations/2026-09-03-memory-design-opus-answer.md [=] f:86; H:86
docs/observations/2026-09-03-memory-design-question-for-sol.md [=] a:6,45
docs/observations/2026-09-03-memory-design-reconciled.md [=] s:10
docs/observations/2026-09-03-memory-design-sol-answer-2.md [=] f:58,60,62,64,66,68,70; s:106,113,247; H:58,60,62,64,66,68,70
docs/observations/2026-09-03-memory-design-sol-answer.md [=] a:290; s:61,149
docs/observations/2026-09-03-merge-queue-for-mayor.md [Δ] f:7,9-10,23-24,28,31,35,37,39-40,60-61,83,91,133,135,137,139,141,143,145; a:17,23,27,36,43,91; p:54,63,91,137,147,155; v:7,24,31,61,83,133,135,137,139,141,143,145; y:15; s:7,9-10,24,27-29,34,131,157-158,160-161; H:9,23-24,28,37,39-40,60; P:54,58,61,63,91,137,147,155; T:15
docs/observations/2026-09-03-night-orders-anvil.md [=] f:1; a:1,4,28,35,40-41,51; p:25; y:47; P:25; T:27,47
docs/observations/2026-09-03-q5z-round10-rereview-opus.md [=] f:9,13; p:12; s:101; H:9,13; P:12; T:13,17-18,23
docs/observations/2026-09-03-q5z-round4-rereview.md [=] s:34; P:18; T:20
docs/observations/2026-09-03-q5z-round5-rereview.md [=] f:24; s:24
docs/observations/2026-09-03-q5z-round6-rereview.md [=] s:31; T:5
docs/observations/2026-09-03-q5z-round7-rereview-opus.md [=] f:8; s:233; H:8; P:27; T:17
docs/observations/2026-09-03-q5z-round8-rereview-opus.md [=] f:14; p:11,305; H:14; P:11-12,304-305; T:17,306
docs/observations/2026-09-03-q5z-round9-rereview-opus-GO.md [=] f:13-14,59,128,379; p:11,378; s:288,348,372; H:13-14,59,128,379; P:11,377-378; T:13,23,379
docs/observations/2026-09-03-ratchets-redteam-GO-WITH-FIX.md [=] s:4
docs/observations/2026-09-03-resume-here-anvil-seat.md [Δ] f:1,8-10,64,205,349,388-389,406,416,418,422,431,440,446,451,455,461,466,478,495,516,531,550,568,580-581,586,594,598,602,604,606,608,610,640,643,647,658,660,665,673,678,688,691,694,701; a:1,8,10,63,141,216-218,236-237,337,344,400,402,413,516,531,566,617-618,659,665,667,670; p:141,149,152,236,414,532,538,541,546,555,566,568,570,572,577,598,600,608,614,619,627,643,646,670,681,685,693; v:416,418,422,455,466,478,495,531,550,581,586,594,598,602,604,606,608,610,640,643,647,658,665,673,678,688,691,701; y:205,243,252,261,268,349,418; r:697; s:244,259,677-679,681-682,687-688,691,694-695; H:8,349,388-389,406,422,431,440,446,451,461,466,516,568,580,660,673,688,694; P:31,64,70,141,149-150,152,236,243,245,252,261,268,280,287,367,414,422,518,532,538,541,546,555,566,568,570,572,577,598,600,608,614,619,627,643,646,670,677,681,683,685,693; T:205,243,252,261,268,349,418,554-555,560,640,660-661,668,670,673,677,691,697
docs/observations/2026-09-03-rf2-q5z-redteam.md [=] f:35; H:35
docs/observations/2026-09-03-rf2-rereview.md [=] f:18
docs/observations/2026-09-03-rf2-round3-rereview-opus.md [=] f:7-8,11,221,247,250; a:234; H:7-8,11,221,247,250
docs/observations/2026-09-03-rf2-round4-rereview-opus-GO.md [=] f:10,12-13,392-393; a:369; H:10,12-13,392-393; T:13,373,394
docs/observations/2026-09-03-study-ops-o2-review-opus.md [=] f:9,112,218; a:9; s:147; H:9,112,218; T:25
docs/observations/2026-09-03-study-ops-redteam-NO-GO.md [=] a:3
docs/observations/2026-09-03-study-round3-rereview-opus.md [=] f:8,10; H:8,10
docs/observations/2026-09-03-study-round4-rereview-opus.md [=] f:8,10,12,260,263-264; p:264; s:73,131; H:8,10,12,260,263-264; P:264; T:12,41,264
docs/observations/2026-09-03-txn-journal-round2-rereview-opus.md [=] f:6-7,18,33,35,42,215,221; s:94,99,175; H:6-7,18,33,35,42; P:44; T:19-20,22
docs/observations/2026-09-03-txn-journal-round3-rereview-opus.md [=] f:15,17-18,21,38,53,55,62,133,137,283,286,292,411,442; p:19; s:73,76,323; H:15,17-18,21,38,53,55,62; P:19,54,64,66-67; T:20,39-40,42
docs/observations/2026-09-03-txn-journal-round4-rereview-opus.md [=] f:13,34,37,42,515; p:17; s:263; H:13,34,37,42; P:17; T:34,37,42
docs/observations/2026-09-03-txn-journal-round5-rereview-opus.md [=] f:13,35,38,43,166,189,196,318,320,378-379,456,481; p:17; s:127; H:13,35,38,43; P:17; T:35,38,43
docs/observations/2026-09-03-txn-journal-round6-rereview-opus.md [=] f:12,16,35,38,43,229,293,296,301,306,310,312,460; p:17; s:212; H:12,16,35,38,43; P:17,267,411; T:16,35,38,43
docs/observations/2026-09-03-txn-journal-round7-rereview-opus.md [=] f:3,12,16,23,34,37,42,100,104,109,111,124,276-277,279,282-287,290-291,314,441,444,518,545; p:17; H:12,16,34,37,42; P:17,175; T:16,34,37,42
docs/observations/2026-09-03-txn-journal-round8-rereview-opus.md [=] f:3,12,16,21-22,34,37,42,77,81-89,200,202-203,232,239,243,255,358,397,473,520,561,564,574,580; p:17; s:3,574; H:12,16,34,37,42; P:17; T:16,34,37,42
docs/observations/2026-09-03-txn-journal-round9-rereview-opus.md [=] f:12,17,34,181,244; p:18; H:12,17,34; P:18; T:17,34,37,41,44,47
docs/observations/2026-09-03-txn-journal-sol-review.md [=] f:7,13,15,21; s:1,3,75; H:7,13,15,21; P:14,22
docs/observations/2026-09-04-astra-plan-for-gene-2309z.md [Δ] f:17; v:17; s:17
docs/observations/2026-09-04-astra-review-reconciled.md [Δ] f:1,3,16; a:1; v:3,16; s:14
docs/observations/2026-09-04-e3-e6-prestaged.md [=] f:3,14-15,17,19,127,130,132,136,139,144,149,165,213,215,282,436,438,522,644,826,833,837,846-847,849,856,861,864,910,1041,1110,1117,1119,1131-1133,1137,1140,1168-1170,1237; a:3-4,14,132,792,1237,1241; p:111,116,439,1242; y:17,420,424,897,1146,1171; s:598,1021; H:15,17,19,127,136,139,149,165,213,215,282,438,522,644,833,837,846-847,849,856,861,864,910,1041,1117,1119,1131-1133,1137,1140,1169-1170; P:111,116,122,202,207,439,1242; T:17,420,424,897,1146,1171
docs/observations/2026-09-04-e3-p-cohort.md [=] f:3-5,203-204; a:3,162,208; y:26; H:4-5,203-204; T:26
docs/observations/2026-09-04-e6-lb-cohort.md [=] f:3,8,131,159; a:3,8,130; y:126; s:51; H:131,159; T:126
docs/observations/2026-09-04-e6c-routing-plate-cohort.md [=] f:3-5,7-8,181,190; a:3,150; p:191; y:80,149,160-161,164-165; H:4-5,7-8,181,190; P:191; T:80,149,160-161,164-165
docs/observations/2026-09-04-e6q-square3-cohort.md [=] f:3-4,7,117,120,127,164; a:3; p:119; y:98,101,103; H:4,7,117,120,127,164; P:119; T:98,101,103
docs/observations/2026-09-04-e6q2-bigfile-cohort.md [=] f:3-4,6,172; a:3; p:174; H:4,6,172; P:97,169,174
docs/observations/2026-09-04-eafford-sed-counterfactual-cohort.md [=] f:3,7,10,18,22,26,70,103,186,391,404,430-431,443; a:3,22,82; p:106,408; y:87; s:425; H:7,10,18,26,70,103,186,391,404,430-431,443; P:106,408; T:87
docs/observations/2026-09-04-ecaller-cohort.md [Δ] f:1,14,19; a:1; H:14,19
docs/observations/2026-09-04-eceiling80-cohort.md [=] f:3,6,9,47,56,301,313-314,359; a:3,359; p:296,355; y:251-252,265,352,360; H:6,9,47,56,301,313-314; P:295-296,355; T:251-252,265,352,360
docs/observations/2026-09-04-egater-replay-and-charsps.md [Δ] f:3,8,10,437,440-441; a:3; s:30,63,86,129,132,136,213,251,441; H:8,10,437,440-441
docs/observations/2026-09-04-eharness2-cohort.md [Δ] f:3,8; a:3; p:236; y:72,203,227,277,291; s:342; H:8; P:236; T:72,203,227,277,291
docs/observations/2026-09-04-ensweep-cohort.md [Δ] f:3,6,11,328,336,339,370; a:3,370; p:333; H:6,11,328,336,339; P:165,333
docs/observations/2026-09-04-eprewrite-cohort.md [=] f:3,7,14,248; a:3; p:215,253; y:188; H:7,14,248; P:215,252-253; T:188
docs/observations/2026-09-04-eprewrite-preregistration.md [=] f:5,32,332,352,421,427,430,432,438,448,450,452,454,464; a:5,351,448,450; p:418; y:349; s:495; H:32,332,352,421,427,430,432,438,448,450,452,454,464; P:418-419; T:349
docs/observations/2026-09-04-ereg-irregularity-cohort.md [=] f:3-4,7-9,126,146,150,172; a:3,124; p:149; H:4,7-9,126,146,150,172; P:148-149
docs/observations/2026-09-04-escalewall-preregistration.md [Δ] f:3-4,72,92; a:3,100; p:240; y:41-42,45-46,49-50,94,233,235; s:119,122; H:4,72,92; P:240,284; T:41-42,45-46,49-50,94,233,235
docs/observations/2026-09-04-ethread-cohort.md [Δ] f:3,76; a:3; y:74; H:76; T:74
docs/observations/2026-09-04-feature-thread-naive-reader-probes.md [Δ] f:3; v:3
docs/observations/2026-09-04-feature-thread-replay-result.md [Δ] f:372; a:3; v:372; P:221
docs/observations/2026-09-04-feature-thread-study.md [Δ] f:182-183; a:3,7,70,182,468; s:99,170,379,465,577; H:183
docs/observations/2026-09-04-feature-thread-verb-build.md [Δ] f:3,79,332; a:3; H:79,332; T:491-492,524-526
docs/observations/2026-09-04-gate-landing-composition.md [Δ] f:3,15,20,22,104,117,402; a:3,28,52,81,256,339,362,392; s:114; H:15,20,22,104,117,402; T:104,302,346,352-354
docs/observations/2026-09-04-gene-report-0055z.md [Δ] y:30; s:91; P:71; T:30
docs/observations/2026-09-04-gene-report-1612z.md [Δ] f:1; a:1
docs/observations/2026-09-04-gene-report-1905z.md [Δ] f:1; a:1
docs/observations/2026-09-04-gene-report-2015z.md [Δ] f:1; a:1
docs/observations/2026-09-04-gene-report-2139z.md [Δ] f:1; a:1
docs/observations/2026-09-04-smw-acceptance-check.md [Δ] f:11,18,21,72; v:18,21; H:11,72; P:24
docs/observations/2026-09-04-smw-five-searches-analysis-mayor.md [Δ] a:3; s:72
docs/observations/2026-09-04-suite-spike-round1-timing.edn [Δ] f:2,7,9-25,44,54,66,78,145,284,304-305,343; v:2,9-25,305; H:7,44,54,66,78,145,284,304,343
docs/observations/2026-09-04-suite-spike-round1-timing.md [Δ] a:4
docs/observations/2026-09-04-suite-spike-round1.md [Δ] f:1,96-98,121,181,278,280,282; a:1; v:121,278,280,282; H:96-98,181; T:189
docs/observations/2026-09-04-suite-spike-round2.md [Δ] f:1,16,92,101,320,325-326,330,355; a:1; v:101,355; s:54; H:16,92,320,325-326,330; T:17
docs/observations/2026-09-04-suite-spike-round3.md [Δ] f:1,15,129,135,165; a:1,91,140; s:24; H:15,129,135,165
docs/observations/2026-09-04-suite-spike-round4.md [Δ] f:1,120,130; a:1; v:120,130
docs/observations/2026-09-04-suite-spike-spec.md [Δ] f:14; a:31; v:14
docs/observations/2026-09-05-astra-fair-comparison-prereg.md [Δ] a:27
docs/observations/2026-09-05-astra-next-api-advice.md [Δ] f:7,9; v:7,9; s:11
docs/observations/2026-09-05-astra-perfect-ledger.md [Δ] f:3; v:3
docs/observations/2026-09-05-astra-portable-timeout.md [Δ] f:13; v:13
docs/observations/2026-09-05-astra-surgeon-usage-study.md [Δ] f:12,15,61,193; v:12,15,61,193; P:10,29,131,169
docs/observations/2026-09-05-captains-log-astra-four-hour-comparison.md [Δ] f:64,85,117,149,151,238,244; a:11; v:64,85,117,149,151,238,244; P:97
docs/observations/2026-09-05-fast-typist-bench-1788651489.edn [Δ] f:4,6,48,75,78,104,107,126,134,137,164,167,194,197,224,227,254,257,284,287,314,317,344,347,374,377,404; v:6,48,75,78,104,107,126,134,137,164,167,194,197,224,227,254,257,284,287,314,317,344,347,374,377,404; H:4
docs/observations/2026-09-05-fast-typist-bench-1788651548.edn [Δ] f:4,6,44,71,74,101,104,131,134,161,164,191,194,219,222,249,252,279,282,301,303,309,312,339,342,367,370,395; v:6,44,71,74,101,104,131,134,161,164,191,194,219,222,249,252,279,282,301,303,309,312,339,342,367,370,395; H:4
docs/observations/2026-09-05-fast-typist-cohort-1.md [Δ] f:3,54; a:1; v:3,54
docs/observations/2026-09-05-fast-typist-prereg.md [Δ] f:32; v:32
docs/observations/2026-09-05-fast-typist-provider-bench.md [Δ] f:104,119,123; a:104; v:119; H:123
docs/observations/2026-09-05-gene-report-0426z.md [Δ] f:1; a:1
docs/observations/2026-09-05-gene-report-1431z.md [Δ] f:1; a:1; T:27,33
docs/observations/2026-09-05-gene-report-1933z-aperture-window.md [Δ] T:28
docs/observations/2026-09-05-helper-extraction-fence-review-r1.md [Δ] f:21,25-27,30,179,238,304,310; v:21,25-27,30,179,238,304,310
docs/observations/2026-09-05-helper-extraction-fence-review-r2.md [Δ] f:31,36-38,41,258,359,364; v:31,36-38,41,258,359,364
docs/observations/2026-09-05-helper-extraction-fence-review-r3.md [Δ] f:76; v:76
docs/observations/2026-09-05-helper-extraction-fence-review-r4.md [Δ] f:47; v:47
docs/observations/2026-09-05-helper-extraction-fence-review-r5.md [Δ] f:40; v:40
docs/observations/2026-09-05-helper-extraction-fence-review-r6.md [Δ] f:37; v:37
docs/observations/2026-09-05-helper-extraction-fence-review-r7.md [Δ] f:32; v:32
docs/observations/2026-09-05-ideal-tool-riff-astra.md [Δ] f:349; s:32,136
docs/observations/2026-09-05-mission-ledger-sol-caller-probe-2.md [Δ] f:83,90,92,100,106,112,118,124,136,142,148,157; v:83,90,92,100,106,112,118,124,136,142,148,157; H:157
docs/observations/2026-09-05-mission-ledger-sol-caller-probe.md [Δ] f:22-23,45,58,80,89,102,108,111,116-117,125,131,141,149,159,167,173,179-182; v:22-23,45,58,80,89,102,108,111,116-117,125,131,141,149,159,167,173,179-182; H:131,141
docs/observations/2026-09-05-sol-fence-refexample-r1.md [Δ] f:3,11,26; v:26; H:3,11
docs/observations/2026-09-05-suite-spike-round5.md [Δ] f:3; a:3,257; s:14,16,45; H:190; T:230
docs/observations/2026-09-05-surgeon-ideal-shape-astra.md [Δ] s:15,47,83,153
docs/observations/2026-09-05-surgeon-ideal-shape-opus.md [Δ] f:104; s:112,245; H:104; P:111,142
docs/observations/2026-09-05-surgeon-ideal-shape-terra.md [Δ] s:139
docs/observations/2026-09-06-astra-agent-usage-result-authority.md [Δ] f:32; v:32
docs/observations/2026-09-06-astra-battery-archive-distance.md [Δ] f:24; v:24
docs/observations/2026-09-06-astra-cardinality-dogfood.md [Δ] f:29; v:29
docs/observations/2026-09-06-astra-checkpoint-1200z.md [Δ] f:21; v:21
docs/observations/2026-09-06-astra-class-capability-audit.md [Δ] f:2; v:2; s:10
docs/observations/2026-09-06-astra-codex-history-token-study.md [Δ] f:4,61-64,144,146,148; v:4,144,146,148; H:61-64
docs/observations/2026-09-06-astra-events-hardening-merge.md [Δ] f:30,33,52; v:30,33,52
docs/observations/2026-09-06-astra-existing-binding-reuse.md [Δ] f:7; H:7
docs/observations/2026-09-06-astra-fanout-final-and-existing-route.md [Δ] f:38,44; v:38,44
docs/observations/2026-09-06-astra-forms-cohort-prereg.md [Δ] f:23; v:23; s:47
docs/observations/2026-09-06-astra-forms-cohort-result.md [Δ] f:5,36; v:5,36
docs/observations/2026-09-06-astra-fresh-caller-review.md [Δ] f:15-17,61-64; v:15-17,61-64
docs/observations/2026-09-06-astra-groq-live-boundary.md [Δ] f:11; v:11
docs/observations/2026-09-06-astra-live-dogfood.md [Δ] f:62,70,78,84,90,100; v:62,70,78,84,90,100
docs/observations/2026-09-06-astra-maven-native-comparison.md [Δ] f:61; v:61; s:3,34
docs/observations/2026-09-06-astra-mission-run.md [Δ] T:27
docs/observations/2026-09-06-astra-native-control-usage.md [Δ] f:59; v:59
docs/observations/2026-09-06-astra-node-cache-gate-reproduction.md [Δ] f:5,34; v:5,34; H:34; T:34
docs/observations/2026-09-06-astra-node-cache-test-boundary.md [Δ] f:5,17,20; v:5,17; s:12; T:30,46
docs/observations/2026-09-06-astra-paper-cuts-ethnography.md [Δ] f:5; v:5
docs/observations/2026-09-06-astra-raw-cohort-control-failure.md [Δ] f:4,18; v:4,18
docs/observations/2026-09-06-astra-raw-cohort-prereg.md [Δ] s:38
docs/observations/2026-09-06-astra-raw-cohort-rereview.md [Δ] f:3; v:3
docs/observations/2026-09-06-astra-raw-cohort-v2-prereg.md [Δ] f:6,93,130,135; v:6,93,130,135; s:48,97
docs/observations/2026-09-06-astra-raw-cohort-v2-result.md [Δ] f:37,39; v:37,39
docs/observations/2026-09-06-astra-real-profile-utility.md [Δ] f:28; v:28
docs/observations/2026-09-06-astra-receipt-fixture-leak.md [Δ] f:9; v:9
docs/observations/2026-09-06-astra-recovery-pilot-result.md [Δ] f:43,76-77; v:43,76-77; s:30
docs/observations/2026-09-06-astra-single-jvm-cold-comparison.md [Δ] f:47,52; v:47,52
docs/observations/2026-09-06-astra-slot-stderr-finding.md [Δ] f:3; H:3; T:23
docs/observations/2026-09-06-astra-spark-utility.md [Δ] f:20; a:20; v:20
docs/observations/2026-09-06-astra-strict-3-review.md [Δ] s:5
docs/observations/2026-09-06-astra-typist-completion-audit.md [Δ] T:23
docs/observations/2026-09-06-astra-typist-progress.md [Δ] f:38; v:38
docs/observations/2026-09-06-astra-usage-after-mcp-resume.md [Δ] f:89,91; v:89,91
docs/observations/2026-09-06-astra-warm-transition-and-spark.md [Δ] f:23; v:23
docs/observations/2026-09-06-brainstorm-wall-breakthrough.md [Δ] f:59; v:59; s:34
docs/observations/2026-09-06-caller-help-repair-astra.md [Δ] f:5,29; v:5,29
docs/observations/2026-09-06-checkpoint-0600z.md [Δ] s:21
docs/observations/2026-09-06-checkpoint-1200z.md [Δ] f:53; p:14,59; v:53; P:14,59
docs/observations/2026-09-06-checkpoint-1500z.md [Δ] f:45; v:45; T:29
docs/observations/2026-09-06-compact-propose-astra.md [Δ] f:4,27; v:4,27
docs/observations/2026-09-06-ethnography-02-astra-0253-0336z.md [Δ] f:12; v:12; s:26; T:17
docs/observations/2026-09-06-ethnography-03-astra-0336-0540z.md [Δ] f:24; v:24
docs/observations/2026-09-06-ethnography-04-astra-post-resume.md [Δ] f:1,3,26; a:1,3; H:26
docs/observations/2026-09-06-events-ledger-and-the-blind-spot.md [Δ] f:8; v:8
docs/observations/2026-09-06-explicit-receipt-destination-astra.md [Δ] f:5,30,33-34; v:5,30,33-34
docs/observations/2026-09-06-extract-E-ethnography.md [Δ] f:1; v:1
docs/observations/2026-09-06-extract-E-preregistration.md [Δ] f:19; v:19
docs/observations/2026-09-06-extract-E/cohort.log [Δ] f:2-3,9; v:2-3,9
docs/observations/2026-09-06-extract-E/freeze.json [Δ] f:3,8-19; v:3,8-19; P:5
docs/observations/2026-09-06-extract-E/hand-drive-apply-resp.json [Δ] f:13,59; v:13; H:59
docs/observations/2026-09-06-extract-E/results.jsonl [Δ] f:1-4; v:1-4; s:1-4; H:1-4; P:1,4
docs/observations/2026-09-06-fanout-B-cohort-result.md [Δ] f:3; v:3
docs/observations/2026-09-06-fanout-B-cohort-results.jsonl [Δ] f:1-14; v:1-14; s:1-14; H:1-14; P:1,3,5-14
docs/observations/2026-09-06-fanout-B-cohort-summary.json [Δ] P:43,81,89
docs/observations/2026-09-06-fanout-B-final-admission-fable.log [Δ] f:8; v:8
docs/observations/2026-09-06-fanout-I-ethnography.md [Δ] f:1; v:1
docs/observations/2026-09-06-fanout-I-preregistration.md [Δ] p:5; P:5
docs/observations/2026-09-06-fanout-I-result.md [Δ] p:3; P:3
docs/observations/2026-09-06-fanout-I/cohort.log [Δ] f:2-3,12; v:2-3,12; P:10
docs/observations/2026-09-06-fanout-I/freeze-b.json [Δ] f:7-20; v:7-20; P:4
docs/observations/2026-09-06-fanout-I/results.jsonl [Δ] f:1-8; v:1-8; s:1-8; H:1-8; P:2-3,5-7
docs/observations/2026-09-06-fanout-I/summary.json [Δ] p:59; P:59,98
docs/observations/2026-09-06-fanout-J-ethnography.md [Δ] f:1; v:1
docs/observations/2026-09-06-fanout-J-preregistration.md [Δ] f:3; v:3; P:3
docs/observations/2026-09-06-fanout-J-result.md [Δ] p:3; P:3
docs/observations/2026-09-06-fanout-J/cohort.log [Δ] f:2-3,8; v:2-3,8; P:4-5
docs/observations/2026-09-06-fanout-J/freeze-b.json [Δ] f:7-20; v:7-20; P:4
docs/observations/2026-09-06-fanout-J/hand-probe-match-result.json [Δ] f:11; v:11; P:12,24,1431,1458,1483,1508,1533
docs/observations/2026-09-06-fanout-J/results.jsonl [Δ] f:1-4; v:1-4; s:1-4; H:1-4; P:1-4
docs/observations/2026-09-06-fast-typist-real-1-ab4-warm.log [Δ] f:4,15; v:4,15; H:4,15; P:31
docs/observations/2026-09-06-fast-typist-real-1-nw-controls-6.log [Δ] f:32; v:32
docs/observations/2026-09-06-fast-typist-real-1-prereg.md [Δ] f:234,236; v:236; H:234
docs/observations/2026-09-06-gene-concerns-and-responses.md [Δ] f:44; v:44; s:46
docs/observations/2026-09-06-gene-report-1620z-night.md [Δ] f:24,35; p:43,47,50; P:43,47,50
docs/observations/2026-09-06-gene-report-1950z-crank.md [Δ] f:3; v:3
docs/observations/2026-09-06-gpt-oss-cost-tally.md [Δ] a:18
docs/observations/2026-09-06-live-astra-typist-commentary.md [Δ] f:8-12,67,101,119,171,175,181,193,211,229,231,235,257,387,401,429,445,479,523; a:12,101; p:293,485,529; v:8-9,67,101,119,171,175,181,193,229,231,235,257,401,429,445,479; s:77,89,127,265,417,423,427,465; H:10-11,211; P:211,293,373,401,485,529; T:121,423
docs/observations/2026-09-06-mission-commit-cli-executed-review.md [Δ] f:4; v:4; T:43
docs/observations/2026-09-06-opus-mission-git-review.md [Δ] f:5,327-328,330,355; v:5,327-328,330,355; s:84; T:294
docs/observations/2026-09-06-opus-rereview-astra-git-seam-ebbf4389.md [Δ] f:3,5-6,14,18-20,50,78,84-85,92,112,146,343; a:3,84-85,92,109,343; v:6,50,78,146; H:5,14,18-20; T:51,253,276
docs/observations/2026-09-06-opus-review-astra-git-seam-b3dbd9e4.md [Δ] f:4-5,58,77-78,127,218; a:77-78,127,218; v:5,58; s:272; H:4; T:59,172,184
docs/observations/2026-09-06-proto-verify-in-call.md [Δ] f:1; v:1; P:1
docs/observations/2026-09-06-proto-warm-proof.md [Δ] f:1; v:1
docs/observations/2026-09-06-real-session-ethnography.md [Δ] f:1,8,47,49; v:1,47,49; H:8; T:89
docs/observations/2026-09-06-receipt-fixture-repair-astra.md [Δ] f:16,31; v:16,31; T:28
docs/observations/2026-09-06-routing-prompts-draft-r2.diff [Δ] f:284,349,722; a:144-145,349; v:284,722; s:433
docs/observations/2026-09-06-routing-prompts-draft-r3.diff [Δ] s:69
docs/observations/2026-09-06-routing-prompts-draft.diff [Δ] f:650,672,675; a:650,672,674; s:730
docs/observations/2026-09-06-skiff-install-stable.md [Δ] a:14
docs/observations/2026-09-06-sol-fence-astra-cardinality-r1.md [Δ] f:51,55-56; H:51,55-56; T:60
docs/observations/2026-09-06-sol-fence-astra-collector-r1.md [Δ] f:7,9; H:7,9
docs/observations/2026-09-06-sol-fence-astra-description-r1.md [Δ] f:11,34; H:11,34; T:49
docs/observations/2026-09-06-sol-fence-astra-freshness-r1.md [Δ] f:28,39,47,53; a:41; H:28,39,47,53
docs/observations/2026-09-06-sol-fence-astra-leak-tip-r1.md [Δ] f:27-28,35,53-55; H:27-28,35,53-55
docs/observations/2026-09-06-sol-fence-astra-typist-route-r1.md [Δ] f:3,40,42-45,148,188-189,192,243,248,258; v:148,243,248,258; H:3,40,42-45,188-189,192
docs/observations/2026-09-06-sol-fence-astra-typist-route-r2.md [Δ] f:35,37,67,102,104,106-108,111,117-118,156,224,259,284-285,301,358-360; a:102,104,117-118,313; v:67,106-108,111,156,224,259,284-285; s:247; H:35,37,301,358-360; T:66,84,110,155,200,223,234,258,270,283
docs/observations/2026-09-06-sol-fence-astra-typist-route-r3-delta.md [Δ] f:98,114,118,126,128,130; a:210,275; v:98,118,126,128,130; s:271,275; H:114; T:71,94,115,146,162,180,223,252
docs/observations/2026-09-06-sol-fence-fable-routing-prompts-r1.md [Δ] f:7-8,13,43,46,97-98,108,124,128,146-147,154-155,157,190; v:13,43,46,128,146-147,154-155,157; H:7-8,97-98,108,124,190; T:124
docs/observations/2026-09-06-sol-fence-fable-routing-prompts-r2.md [Δ] f:5,7,9,104-105,114,116-117,152,163; v:163; H:5,7,9,104-105,114,116-117,152; P:171,174; T:28
docs/observations/2026-09-06-sol-fence-fable-routing-prompts-r3.md [Δ] f:3,6,12,36,66-67,73,85,99,106,113; v:6,99,106,113; H:3,12,36,66-67,73,85,99; T:36,85
docs/observations/2026-09-06-sol-fence-fable-routing-prompts-r4.md [Δ] f:7,18,73-74,83,125; v:18; H:7,73-74,83,125; P:23; T:45,100
docs/observations/2026-09-06-sol-fence-fable-typist-real-repo-r1.md [Δ] f:9,20,23,33,49,59-61,85,89,92,95-97,100,103-104,151-153,186,193,206,209,226,237-238,245,247,262-264,283,287; v:9,59,61,85,96,100,103-104,151-153,186,193,206,209,237-238,245,247,262-264,283,287; H:20,33,49,89,92,95-97,151,226,245; T:261
docs/observations/2026-09-06-sol-fence-fable-typist-real-repo-r2.md [Δ] f:17,27,37,65,78-79,82-83,87,90,117-118,135,161,183,204; v:37,78-79,82-83,117-118,135,183,204; H:17,65,78-79,83,87,90,161; T:116
docs/observations/2026-09-06-sol-fence-fable-typist-real-repo-r3.md [Δ] f:27,42,50,58,61,71,94,156,197-198; v:27,42,61,71,197-198; H:58,61,71,94,156; T:196
docs/observations/2026-09-06-sol-fence-fable-typist-real-repo-r4.md [Δ] f:12,19,34-35,124,135,141,149,155,211,213,215,234; v:34-35,135,141,149,155,213,234; H:12,19,124,211,215
docs/observations/2026-09-06-sol-fence-fable-typist-real-repo-r5.md [Δ] f:12-13,16,18,24,27,42-43,63-64,72-73,78; v:64; H:12-13,16,18,24,27,42-43,63,72-73,78; T:85
docs/observations/2026-09-06-sol-fence-fable-typist-real-repo-r6.md [Δ] f:3,19,33,35-36,73-74,76-78,80-86,99,123-125,127-128,130,135,142,150,155-159,164,172-173,182,187,238; v:19,33,35-36,73-74,76-78,80-83,85,99,123-125,128,135,142,150,155-158,164,172-173,182,187,238; H:3,36,74,76,80-86,125,127,130,142,156,158-159,164
docs/observations/2026-09-06-sol-fence-match-owner-counts-r1.md [Δ] T:47
docs/observations/2026-09-06-sol-fence-match-owner-counts-r2.md [Δ] f:11,28-29; H:11,28-29; T:33
docs/observations/2026-09-06-sol-fence-public-result-ceiling-r1.md [Δ] T:22
docs/observations/2026-09-06-sol-fence-public-result-ceiling-r3.md [Δ] T:20
docs/observations/2026-09-06-sol-fence-refusal-text-r1.md [Δ] f:19,21,25; H:19,21,25
docs/observations/2026-09-06-sol-fence-refusal-text-r2.md [Δ] f:9
docs/observations/2026-09-06-sol-fence-refusal-text-r3.md [Δ] f:15,21,26,28,30,34; H:15,21,26,30,34
docs/observations/2026-09-06-sol-fence-refusal-text-r4.md [Δ] f:5; H:5; T:11
docs/observations/2026-09-06-sol-fence-refusal-text-r5.md [Δ] f:37,41,43,47,57,77; H:37,41,43,47,57; T:79
docs/observations/2026-09-06-sol-fence-refusal-text-r6.md [Δ] f:25,54,62; H:25,54,62; T:66
docs/observations/2026-09-06-sol-fence-refusal-text-r7.md [Δ] f:18,23-24,26-27,32; H:18,32; T:79
docs/observations/2026-09-06-sol-fence-refusal-text-r8.md [Δ] f:6-7; T:17
docs/observations/2026-09-06-strictly-better-evidence.md [Δ] f:79,131; v:79,131; P:75
docs/observations/2026-09-06-sublime-tool-for-astra.md [Δ] f:35; v:35; s:13,25,67
docs/observations/2026-09-06-two-hour-trial-closeout.md [Δ] f:23,47; v:23,47
docs/observations/2026-09-06-wiring-change.md [Δ] f:7,12; v:7,12; P:30
docs/observations/2026-09-07-aliasfix-r2-sol-review.md [Δ] f:5,17,19,21,34; H:5,17,19,21,34
docs/observations/2026-09-07-aliasfix-r3-sol-review.md [Δ] f:10,28; H:10,28
docs/observations/2026-09-07-aliasfix-r4-sol-review.md [Δ] f:33,41-42; H:33,41-42
docs/observations/2026-09-07-astra-lid-consult.md [Δ] s:9,21
docs/observations/2026-09-07-dogfood-2.md [Δ] p:13,24,45-46; P:1,13,15,24,45-46,48,50; T:13,24
docs/observations/2026-09-07-dogfood-2/cc-tool/request-1.json [Δ] f:1; H:1
docs/observations/2026-09-07-dogfood-2/cc-tool/request-2.json [Δ] f:1; H:1
docs/observations/2026-09-07-dogfood-2/cc-tool/response-1.json [Δ] f:1; H:1
docs/observations/2026-09-07-dogfood-2/cc-tool/response-2.json [Δ] f:1; H:1; P:1
docs/observations/2026-09-07-dogfood-2/cc/friction.md [Δ] f:1,10,12-13,19-24,76; H:1,10,12,19-24,76
docs/observations/2026-09-07-dogfood-2/cc/probes.json [Δ] f:2-3,5-9; H:2-3,5-9
docs/observations/2026-09-07-dogfood-2/cc/request-1.json [Δ] f:1; H:1
docs/observations/2026-09-07-dogfood-2/cc/request-2.json [Δ] f:1; H:1
docs/observations/2026-09-07-dogfood-2/cc/response-1.json [Δ] f:1; H:1
docs/observations/2026-09-07-dogfood-2/mvr-tool/friction.md [Δ] f:2; H:2
docs/observations/2026-09-07-dogfood-2/mvr-tool/request-1.json [Δ] f:1; H:1
docs/observations/2026-09-07-dogfood-2/mvr-tool/request-2.json [Δ] f:1; H:1
docs/observations/2026-09-07-dogfood-2/mvr-tool/response-1.json [Δ] f:1; H:1
docs/observations/2026-09-07-dogfood-2/mvr-tool/response-2.json [Δ] f:1; H:1
docs/observations/2026-09-07-dogfood-2/mvr/friction.md [Δ] f:3,29,32,35-37; s:116; H:3,29,32,35-37
docs/observations/2026-09-07-dogfood-2/mvr/request-1.json [Δ] f:2; H:2
docs/observations/2026-09-07-dogfood-2/mvr/response-2.json [Δ] f:1; H:1
docs/observations/2026-09-07-dogfood-mvr/apply-1-request.json [Δ] f:2; H:2
docs/observations/2026-09-07-dogfood-mvr/apply-1-response.json [Δ] f:16,37; H:16,37; P:29
docs/observations/2026-09-07-dogfood-mvr/inspect-1.json [Δ] f:11; H:11
docs/observations/2026-09-07-dogfood-mvr/stopwatch.log [Δ] P:8
docs/observations/2026-09-07-expectfix-r1-sol-review.md [Δ] f:6,16,27,30; H:6,16,27,30; T:53
docs/observations/2026-09-07-expectfix-r2-sol-review.md [Δ] f:7,14,16,18,26; H:7,14,16,18,26
docs/observations/2026-09-07-expectfix-r3-sol-review.md [Δ] f:23,25,34; H:23,25,34; T:55
docs/observations/2026-09-07-expectfix-r4-sol-review.md [Δ] f:13,15-16,22; H:13,15-16,22; T:30
docs/observations/2026-09-07-expectfix-r5-sol-review.md [Δ] T:21
docs/observations/2026-09-07-gene-report-dogfood-2.md [Δ] p:19,22,44; P:18-19,22,32,38,44; T:32,40
docs/observations/2026-09-07-gene-report-lid-audit.md [Δ] a:3; y:27; s:16; T:27
docs/observations/2026-09-07-gene-report-ten-decisions.md [Δ] a:7; p:26; P:26
docs/observations/2026-09-07-lid-audit/cc-batch-1-astra-report.md [Δ] f:185,196,207,266,268,275,277; a:275,277; v:185,196,207,268; y:269,298; H:185,266; T:269,298
docs/observations/2026-09-07-lid-audit/cc-batch-2-astra-report.md [Δ] f:3,267,353,479; a:479; v:267,353; y:500; s:26,35,164,270,485; H:3; T:500
docs/observations/2026-09-07-lid-audit/cc-batch-3-astra-report.md [Δ] f:3,319,407,425; a:425; v:319; y:419,444; s:13,18,400; H:3,407; T:419,444
docs/observations/2026-09-07-lid-audit/cc-batch-4-astra-report.md [Δ] f:3,128-129,163,179; a:179; v:128; y:169; H:3,129,163; T:169
docs/observations/2026-09-07-lid-audit/cc-batch-5-hld-astra-report.md [Δ] f:3,8,62-65,123; a:8; v:62-64; y:57; s:28,33; H:3,65,123; T:57
docs/observations/2026-09-07-lid-audit/cc.md [Δ] f:3,7,12,43-44,68,949-951,965,969,979-982; a:9,67; v:7,43-44,965,979-982; s:70,141,327,406,482,849,906; H:3,12,949-951,969; P:6
docs/observations/2026-09-07-lid-audit/mvr-batch-1-astra-report.md [Δ] f:4-5,98-99,153-154; a:4; v:5,98-99,153-154; s:66,70,186-187; H:4
docs/observations/2026-09-07-lid-audit/mvr-batch-2-astra-report.md [Δ] f:4,6,8,153-154,174-175,214,236-241; a:6,191-193,205-207; v:8,153-154,174-175,214,236-241; H:4
docs/observations/2026-09-07-lid-audit/mvr-batch-3-astra-report.md [Δ] f:3,6,9,169-170; a:6; v:9,169-170; s:46; H:3
docs/observations/2026-09-07-lid-audit/mvr-batch-4-hld-astra-report.md [Δ] f:3,6,9,181-182,185,198-201,222-223; a:6,227-229; v:9,181-182,185,198-201,222-223; s:108; H:3
docs/observations/2026-09-07-lid-audit/mvr.md [Δ] f:9,54-58,65,67-68; a:68; s:58,63,161,177; H:9
docs/observations/2026-09-07-lid-audit/surgeon-batch-1-astra-report.md [Δ] f:4,8,10,31,88,312,321-322,395-396,407,412; a:8; v:10,88,312,321-322,395-396,407,412; H:4,31
docs/observations/2026-09-07-lid-audit/surgeon-batch-1-round-2-astra-report.md [Δ] f:5,9,32,36,62-63,158,164,170,178,191,204,220,236,253,314,330,337-338,390,392-393,395-396,398-399,405-406,408,453-454; a:9,453-454; v:36,62-63,158,164,170,178,191,204,220,236,253,314,330,337-338,390,392-393,395-396,398-399,405-406,408; s:440; H:5,32,408; P:410,412; T:390,393,396,399
docs/observations/2026-09-07-lid-audit/surgeon-batch-1-sol-review-r1.md [Δ] f:23; v:23
docs/observations/2026-09-07-lid-audit/surgeon-batch-1-sol-review-r2.md [Δ] f:23; v:23; T:27
docs/observations/2026-09-07-lid-audit/surgeon-batch-2-astra-report.md [Δ] f:4,8,40,115-116,121,151,163,212-213,278-279,281,312; a:8; v:115-116,121,151,163,212-213,278-279,281,312; s:19,193,201; H:4,40,281; P:8,283,285,290
docs/observations/2026-09-07-lid-audit/surgeon-batch-2-sol-review-r1.md [Δ] f:3,12; s:12,16; H:3,12; T:31
docs/observations/2026-09-07-lid-audit/surgeon.md [Δ] f:3,8,11,13,27,180,316,363; a:11,13,180; v:27,363; s:249; H:3,8,316
docs/observations/2026-09-07-lid-cc-sol-report.md [Δ] f:129-132; v:129-132; y:31; s:6,23,47,101,144; T:31
docs/observations/2026-09-07-lid-mvr-sol-report.md [Δ] f:50,72; v:50,72
docs/observations/2026-09-07-namespace-split-papercuts.md [Δ] f:45,55,99; v:45,55,99
docs/observations/2026-09-07-pair-1-preregistration.md [Δ] p:7; P:7
docs/observations/2026-09-07-pair-1-result.md [Δ] f:38; v:38
docs/observations/2026-09-07-pair-1/A-T1/request-1.json [Δ] f:2; H:2
docs/observations/2026-09-07-pair-1/A-T1/request-2.json [Δ] f:2; H:2
docs/observations/2026-09-07-pair-1/A-T1/request-3.json [Δ] f:2; H:2
docs/observations/2026-09-07-pair-1/A-T1/request-4.json [Δ] f:2; H:2
docs/observations/2026-09-07-pair-1/A-T1/response-2.json [Δ] f:1; H:1; P:1
docs/observations/2026-09-07-pair-1/A-T1/response-3.json [Δ] f:1; H:1
docs/observations/2026-09-07-pair-1/A-T1/response-4.json [Δ] f:1; H:1
docs/observations/2026-09-07-pair-1/A-T2/request-1.json [Δ] f:1; H:1
docs/observations/2026-09-07-pair-1/A-T2/request-2.json [Δ] f:1; H:1
docs/observations/2026-09-07-pair-1/A-T2/response-1.json [Δ] f:1; H:1
docs/observations/2026-09-07-pair-1/A-T2/response-2.json [Δ] f:1; H:1; P:1
docs/observations/2026-09-07-pair-1/B-N1/stopwatch.log [Δ] P:5
docs/observations/2026-09-07-pair-1/B-T1/request-1.json [Δ] f:2; H:2
docs/observations/2026-09-07-pair-1/B-T1/request-2.json [Δ] f:2; H:2
docs/observations/2026-09-07-pair-1/B-T1/response-1.json [Δ] f:1; H:1; P:1
docs/observations/2026-09-07-pair-1/B-T1/response-2.json [Δ] f:1; H:1
docs/observations/2026-09-07-pair-1/B-T2/request-1.json [Δ] f:2; H:2
docs/observations/2026-09-07-pair-1/B-T2/request-2.json [Δ] f:2; H:2
docs/observations/2026-09-07-pair-1/B-T2/response-1.json [Δ] f:1; H:1; P:1
docs/observations/2026-09-07-pair-1/B-T2/response-2.json [Δ] f:1; H:1
docs/observations/2026-09-07-plan-2-cellC-result.md [Δ] p:57; P:57; T:26,76
docs/observations/2026-09-07-plan-2-preregistration.md [Δ] p:9; y:9; P:9; T:9
docs/observations/2026-09-07-plan-2/astra-clojure-strengths.md [Δ] f:7,19,21,64,72-77,139; v:7,19,21; s:74,77,119,155,159,163; H:19,64,72-77
docs/observations/2026-09-07-plan-2/astra-opine.md [Δ] f:16,31,33,45,47,49,87,89,91,105,109-145,149,153-187,191; v:16,31,33,45; s:87,98; H:47,49,87,89,91,105,109-145,149,153-187,191
docs/observations/2026-09-07-plan-2/astra-planning-riff.md [Δ] f:13,38,70,72,74,82,90,94,96,102,254,256; v:13,38,82,90,94,96,254; s:64,88,90,144,192,244; H:70,72,74,102,256
docs/observations/2026-09-07-plan-2/astra-riff.md [Δ] f:96; p:5,33,62,69,92,100; v:96; y:69; s:31,43,51,77,90; P:5,33,62,69,92,100; T:69
docs/observations/2026-09-07-plan-2/brief-N1.md [Δ] f:1,5,7,9,11; v:1,5,7,9; H:1,11; P:1
docs/observations/2026-09-07-plan-2/brief-R1.md [Δ] f:1,3,5,7,9,11; v:1,3,5,7,9; H:1,11; P:1
docs/observations/2026-09-07-plan-2/brief-T1.md [Δ] f:1,3,5,7,9,11; v:1,3,5,7,9; H:1,3,11; P:1
docs/observations/2026-09-07-plan-2/cc-split-request.edn [Δ] f:1; v:1
docs/observations/2026-09-07-plan-2/cellA/N1/analyze.py [Δ] f:2,4,6,73; v:2,6,73; H:4
docs/observations/2026-09-07-plan-2/cellA/N1/callers.py [Δ] f:2,9; v:2; H:9
docs/observations/2026-09-07-plan-2/cellA/N1/gen.py [Δ] f:2,34-35,37-38; v:2,35,37-38; H:34
docs/observations/2026-09-07-plan-2/cellA/N1/oracle3.py [Δ] f:2,4; v:2; H:4
docs/observations/2026-09-07-plan-2/cellA/N1/parse.py [Δ] f:2,42; v:42; H:2
docs/observations/2026-09-07-plan-2/cellA/N2/build.py [Δ] f:2,4,7-8,144; v:2,7-8,144; H:4
docs/observations/2026-09-07-plan-2/cellA/N2/callers.py [Δ] f:2,4; v:2; H:4
docs/observations/2026-09-07-plan-2/cellA/N2/deps.py [Δ] f:2,4-5,57-58; v:2,4,57-58; H:5
docs/observations/2026-09-07-plan-2/cellA/N2/fwd.py [Δ] f:2,4-5; v:2,4-5
docs/observations/2026-09-07-plan-2/cellA/N2/parse.py [Δ] f:2,50; v:50; H:2
docs/observations/2026-09-07-plan-2/cellA/R1/grep_oracle.py [Δ] f:3-4; v:4; H:3
docs/observations/2026-09-07-plan-2/cellA/R1/nrepl_eval.py [Δ] f:47-48; H:47-48
docs/observations/2026-09-07-plan-2/cellA/R2/nrepl_eval.py [Δ] f:45; H:45
docs/observations/2026-09-07-plan-2/cellA/R2/stopwatch.log [Δ] P:8
docs/observations/2026-09-07-plan-2/cellA/T1/analyze.py [Δ] f:2,4; v:2; H:4
docs/observations/2026-09-07-plan-2/cellA/T1/cands.py [Δ] f:2,5; v:2; H:5
docs/observations/2026-09-07-plan-2/cellA/T1/deps.py [Δ] f:2,4,32-33; v:2,32-33; H:4
docs/observations/2026-09-07-plan-2/cellA/T1/request-1.json [Δ] f:2; H:2
docs/observations/2026-09-07-plan-2/cellA/T1/request-10.json [Δ] f:3; H:3
docs/observations/2026-09-07-plan-2/cellA/T1/request-11.json [Δ] f:3; H:3
docs/observations/2026-09-07-plan-2/cellA/T1/request-12.json [Δ] f:3; H:3
docs/observations/2026-09-07-plan-2/cellA/T1/request-13.json [Δ] f:3; H:3
docs/observations/2026-09-07-plan-2/cellA/T1/request-14.json [Δ] f:3; H:3
docs/observations/2026-09-07-plan-2/cellA/T1/request-15.json [Δ] f:3; H:3
docs/observations/2026-09-07-plan-2/cellA/T1/request-16.json [Δ] f:3; H:3
docs/observations/2026-09-07-plan-2/cellA/T1/request-17.json [Δ] f:3; H:3
docs/observations/2026-09-07-plan-2/cellA/T1/request-18.json [Δ] f:3; H:3
docs/observations/2026-09-07-plan-2/cellA/T1/request-19.json [Δ] f:3; H:3
docs/observations/2026-09-07-plan-2/cellA/T1/request-2.json [Δ] f:2; H:2
docs/observations/2026-09-07-plan-2/cellA/T1/request-20.json [Δ] f:3; H:3
docs/observations/2026-09-07-plan-2/cellA/T1/request-21.json [Δ] f:3; H:3
docs/observations/2026-09-07-plan-2/cellA/T1/request-22.json [Δ] f:3; H:3
docs/observations/2026-09-07-plan-2/cellA/T1/request-23.json [Δ] f:3; H:3
docs/observations/2026-09-07-plan-2/cellA/T1/request-24.json [Δ] f:3; H:3
docs/observations/2026-09-07-plan-2/cellA/T1/request-25.json [Δ] f:3; H:3
docs/observations/2026-09-07-plan-2/cellA/T1/request-26.json [Δ] f:3; H:3
docs/observations/2026-09-07-plan-2/cellA/T1/request-27.json [Δ] f:3; H:3
docs/observations/2026-09-07-plan-2/cellA/T1/request-28.json [Δ] f:3; H:3
docs/observations/2026-09-07-plan-2/cellA/T1/request-29.json [Δ] f:3; H:3
docs/observations/2026-09-07-plan-2/cellA/T1/request-3.json [Δ] f:1; H:1
docs/observations/2026-09-07-plan-2/cellA/T1/request-30.json [Δ] f:3; H:3
docs/observations/2026-09-07-plan-2/cellA/T1/request-31.json [Δ] f:3; H:3
docs/observations/2026-09-07-plan-2/cellA/T1/request-32.json [Δ] f:3; H:3
docs/observations/2026-09-07-plan-2/cellA/T1/request-33.json [Δ] f:3; H:3
docs/observations/2026-09-07-plan-2/cellA/T1/request-34.json [Δ] f:3; H:3
docs/observations/2026-09-07-plan-2/cellA/T1/request-35.json [Δ] f:3; H:3
docs/observations/2026-09-07-plan-2/cellA/T1/request-36.json [Δ] f:3; H:3
docs/observations/2026-09-07-plan-2/cellA/T1/request-37.json [Δ] f:3; H:3
docs/observations/2026-09-07-plan-2/cellA/T1/request-4.json [Δ] f:2; H:2
docs/observations/2026-09-07-plan-2/cellA/T1/request-5.json [Δ] f:2; H:2
docs/observations/2026-09-07-plan-2/cellA/T1/request-6.json [Δ] f:3; H:3
docs/observations/2026-09-07-plan-2/cellA/T1/request-7.json [Δ] f:3; H:3
docs/observations/2026-09-07-plan-2/cellA/T1/request-8.json [Δ] f:3; H:3
docs/observations/2026-09-07-plan-2/cellA/T1/request-9.json [Δ] f:3; H:3
docs/observations/2026-09-07-plan-2/cellA/T1/response-5.json [Δ] f:2; H:2
docs/observations/2026-09-07-plan-2/cellA/T1/response-7.json [Δ] f:26; H:26
docs/observations/2026-09-07-plan-2/cellA/T2/analyze.py [Δ] f:2; H:2
docs/observations/2026-09-07-plan-2/cellA/T2/analyze2.py [Δ] f:2; H:2
docs/observations/2026-09-07-plan-2/cellA/T2/analyze3.py [Δ] f:2; H:2
docs/observations/2026-09-07-plan-2/cellA/T2/mkedits.py [Δ] f:10; H:10
docs/observations/2026-09-07-plan-2/cellA/T2/mkedits2.py [Δ] f:10; H:10
docs/observations/2026-09-07-plan-2/cellA/T2/mkoutline.py [Δ] f:2; H:2
docs/observations/2026-09-07-plan-2/cellA/T2/oracle3.py [Δ] f:2-3; v:2; H:3
docs/observations/2026-09-07-plan-2/cellA/T2/request-1.json [Δ] f:1; H:1
docs/observations/2026-09-07-plan-2/cellA/T2/stopwatch.log [Δ] P:1
docs/observations/2026-09-07-plan-2/cellB/C1/cli-1.txt [Δ] f:24-37,49; s:49; H:24-37
docs/observations/2026-09-07-plan-2/cellB/C1/cli-2.txt [Δ] f:23-36,48; s:48; H:23-36
docs/observations/2026-09-07-plan-2/cellB/C1/cli-3.txt [Δ] f:14,17-30; v:14; H:17-30
docs/observations/2026-09-07-plan-2/cellB/C1/extract-receipt.edn [Δ] f:1; s:1; H:1
docs/observations/2026-09-07-plan-2/cellB/C2/change-receipt.edn [Δ] f:1; H:1; P:1
docs/observations/2026-09-07-plan-2/cellB/C2/cli-1.txt [Δ] f:24-37,49; s:49; H:24-37
docs/observations/2026-09-07-plan-2/cellB/C2/cli-2.txt [Δ] f:14,17-30; v:14; H:17-30
docs/observations/2026-09-07-plan-2/cellB/C2/cli-4.txt [Δ] f:8,16,18,20,22,24,26,28,30,32; v:8; H:16,18,20,22,24,26,28,30,32; P:33
docs/observations/2026-09-07-plan-2/cellB/C2/extract-receipt.edn [Δ] f:1; s:1; H:1
docs/observations/2026-09-07-plan-2/cellB/M1/request-1.json [Δ] f:2; H:2
docs/observations/2026-09-07-plan-2/cellB/M1/request-2.json [Δ] f:2; H:2
docs/observations/2026-09-07-plan-2/cellB/M1/request-3.json [Δ] f:1; H:1
docs/observations/2026-09-07-plan-2/cellB/M1/request-4.json [Δ] f:2; H:2
docs/observations/2026-09-07-plan-2/cellB/M1/request-5.json [Δ] f:2; H:2
docs/observations/2026-09-07-plan-2/cellB/M1/response-3.json [Δ] f:1,8; H:1,8
docs/observations/2026-09-07-plan-2/cellB/M2/request-1.json [Δ] f:3; H:3
docs/observations/2026-09-07-plan-2/cellB/M2/request-2.json [Δ] f:3; H:3
docs/observations/2026-09-07-plan-2/cellB/M2/request-3.json [Δ] f:1; H:1
docs/observations/2026-09-07-plan-2/cellB/M2/request-4.json [Δ] f:1; H:1
docs/observations/2026-09-07-plan-2/cellB/M2/request-5.json [Δ] f:1; H:1
docs/observations/2026-09-07-plan-2/cellB/M2/request-6.json [Δ] f:1; H:1
docs/observations/2026-09-07-plan-2/cellB/M2/request-6b.json [Δ] f:1; H:1
docs/observations/2026-09-07-plan-2/cellB/M2/request-7.json [Δ] f:3; H:3
docs/observations/2026-09-07-plan-2/cellB/M2/response-2.json [Δ] f:1; H:1
docs/observations/2026-09-07-plan-2/cellB/M2/response-5.json [Δ] f:1; H:1
docs/observations/2026-09-07-plan-2/cellB/M2/response-6b.json [Δ] f:1; H:1
docs/observations/2026-09-07-plan-2/cellB/M2/response-7.json [Δ] f:1; H:1; P:1
docs/observations/2026-09-07-plan-2/cellB/N2/stopwatch.log [Δ] P:6
docs/observations/2026-09-07-plan-2/cellB/brief-C.md [Δ] f:6,8,22,35,59,63,65,78,83,101,115; v:8,35,59,63,65,78,83,101; H:6,22,115; P:10
docs/observations/2026-09-07-plan-2/cellB/brief-M.md [Δ] f:6,8,17,28,32,34,47,52,70,84; v:8,28,32,34,47,52,70; H:6,17,84; P:10
docs/observations/2026-09-07-plan-2/cellB/brief-N.md [Δ] f:6,8,23,25,38,43,61,75; v:8,23,25,38,43,61; H:6,75; P:10
docs/observations/2026-09-07-plan-2/cellB/mutations.md [Δ] f:4,24,82,99,104,106,111,291,308,313,315,320,396,433,495,512,517,519,524,704,721,726,728,733,774,798,842,859; p:320; v:99,104,106,111,308,313,315,320,396,433,512,517,519,524,721,726,728,733,798,842,859; s:912-913; H:4,24,82,291,495,704,774; P:320,327
docs/observations/2026-09-07-plan-2/cellB/oracle.sh [Δ] f:3,8,14; v:3,8,14
docs/observations/2026-09-07-plan-2/cellB/task.md [Δ] f:11,17; v:17; H:11
docs/observations/2026-09-07-plan-2/cellC/D1-request.edn [Δ] f:1; H:1
docs/observations/2026-09-07-plan-2/cellC/D1/receipt.edn [Δ] f:21,28-29,71,123,125; v:28,71,123,125; H:21,29
docs/observations/2026-09-07-plan-2/cellC/D2/receipt.edn [Δ] f:21,28-29,71,123,125; v:28,71,123,125; H:21,29
docs/observations/2026-09-07-plan-2/cellC/D3/receipt.edn [Δ] f:28,36-37,79,144,146; v:36,79,144,146; H:28,37
docs/observations/2026-09-07-plan-2/cellC/D4/receipt.edn [Δ] f:28,36-37,79,144,146; v:36,79,144,146; H:28,37; P:135
docs/observations/2026-09-07-plan-2/cellC/D5/receipt.edn [Δ] f:28,36-37,79,145,147; v:36,79,145,147; H:28,37
docs/observations/2026-09-07-plan-2/cellC/D6/receipt.edn [Δ] f:28,36-37,79,145,147; v:36,79,145,147; H:28,37
docs/observations/2026-09-07-plan-2/cellC/D7/papercuts.txt [Δ] f:1; H:1
docs/observations/2026-09-07-plan-2/cellC/D7/receipt.edn [Δ] f:28,36-37,79,145,147; v:36,79,145,147; H:28,37
docs/observations/2026-09-07-plan-2/cellC/D8/papercuts.txt [Δ] f:1; H:1
docs/observations/2026-09-07-plan-2/cellC/D8/receipt.edn [Δ] f:28,36-37,79,145,147; v:36,79,145,147; H:28,37
docs/observations/2026-09-07-plan-2/cellC/M3/mcp-response.json [Δ] f:1; v:1; H:1
docs/observations/2026-09-07-plan-2/cellC/M4/response.json [Δ] f:1; v:1; H:1
docs/observations/2026-09-07-plan-2/cellC/M5/mcp-response.json [Δ] f:1; v:1; H:1
docs/observations/2026-09-07-plan-2/cellC/M6/mcp-response.json [Δ] f:1; v:1; H:1
docs/observations/2026-09-07-plan-2/cellC/M7/mcp-response.json [Δ] f:1; v:1; H:1
docs/observations/2026-09-07-plan-2/cellC/M7/papercuts.txt [Δ] f:1; H:1
docs/observations/2026-09-07-plan-2/cellC/M8/mcp-response.json [Δ] f:1; v:1; H:1
docs/observations/2026-09-07-plan-2/cellC/M8/papercuts.txt [Δ] f:1; H:1
docs/observations/2026-09-07-plan-2/cellC/astra-B02-acceptance-2.md [Δ] f:5,9-11,13,31,46,48,74,86,96-105; v:5,9-11,86; s:29,58,84,100,105; H:13,46,48,74,96-105
docs/observations/2026-09-07-plan-2/cellC/astra-B02-acceptance.md [Δ] f:5,11-13,27,36,40,44,53,57,59,71,81,89-99; v:5,11-13; y:85,94; s:42,90,95; H:40,53,57,59,71,89-99; T:85,94
docs/observations/2026-09-07-plan-2/cellC/astra-B03b-report.md [Δ] f:6,16,99,105,107; a:16; H:6,99,105,107
docs/observations/2026-09-07-plan-2/cellC/astra-B03prep-report.md [Δ] f:4,16,36,53,55,57,76,87,100,109,111-112,173,211-212,214; a:16,99; v:53,57,173,211,214; H:4,36,55,76,87,100,109,111-112,212
docs/observations/2026-09-07-plan-2/cellC/astra-B07-report.md [Δ] f:8,17-18; a:8; v:17-18; H:18
docs/observations/2026-09-07-plan-2/cellC/astra-cadence.md [Δ] f:5,41,49,103,121,125,129; a:5; v:5,41,103,121,129; s:61,67,137,141,150; H:5,49
docs/observations/2026-09-07-plan-2/cellC/astra-coldstart.md [Δ] f:19,21,23,232,235; a:19; v:23,235; s:5,81,128,130,142,156; H:19,21,23,232
docs/observations/2026-09-07-plan-2/cellC/astra-daily-sublime.md [Δ] f:3,64; a:42,46,92; v:3; s:15,62,66; H:3,64
docs/observations/2026-09-07-plan-2/cellC/astra-papercuts-2-report.md [Δ] f:1,18; a:1; H:18
docs/observations/2026-09-07-plan-2/cellC/astra-papercuts-report.md [Δ] f:1,18; a:1; H:18
docs/observations/2026-09-07-plan-2/cellC/astra-plan-tree.md [Δ] f:89,137-142; a:137; v:89,140-142; s:3,23,89,96; H:137-139,142
docs/observations/2026-09-07-plan-2/cellC/astra-r10fix-report.md [Δ] f:3,11,33,77,107,116,121,134; a:116; v:11,77,121,134; s:21,30; H:3,33,107; T:35
docs/observations/2026-09-07-plan-2/cellC/astra-riff-where.md [Δ] f:85; a:85; v:85; s:3,5,38,59; H:85
docs/observations/2026-09-07-plan-2/cellC/astra-round3-report.md [Δ] f:1,19; a:1; H:19
docs/observations/2026-09-07-plan-2/cellC/astra-round4-report.md [Δ] f:4,10,44; a:10; v:44; s:4,100; H:4; P:7
docs/observations/2026-09-07-plan-2/cellC/astra-row2-papercut-report.md [Δ] f:4,16,44,53-55,70,73-75,89,115; a:115; v:16,44,53-55,70,73-75,89; H:4
docs/observations/2026-09-07-plan-2/cellC/astra-row3-report.md [Δ] f:64,134; a:134; v:64; s:40
docs/observations/2026-09-07-plan-2/cellC/astra-skill-review.md [Δ] f:13,29,36,83,85,87,89,91,93,95,145,159,198; a:13; s:9,26-27,51,85,147,150,153,157,169,196,198,200; H:13,29,36,83,85,87,89,91,93,95,145,159,198
docs/observations/2026-09-07-plan-2/cellC/astra-squares-revisit.md [Δ] f:5,17,30-32,88-96,102,104,110; s:40,96,113,115; H:5,17,30-32,88-96,102,104,110
docs/observations/2026-09-07-plan-2/cellC/astra-sublime-assessment.md [Δ] f:51,121; a:121; v:121; s:21,86; H:121
docs/observations/2026-09-07-plan-2/cellC/b02/astra-night [Δ] f:3,28,30-31,34,102,105,110; v:31,110; y:17,22,105; H:3,28,30,34,102,105; T:17,22,105
docs/observations/2026-09-07-plan-2/cellC/b02/astra-watch [Δ] f:21,24-25,67,97,101; a:23; v:24-25; H:21,67,97,101
docs/observations/2026-09-07-plan-2/cellC/b02/b02b-smoke-receipt.json [Δ] f:22-23,26,35-36,41,44-45,50,96,98-99,101,114,142,149,156,177,184; v:22-23,35-36,41,44-45,50,96,98-99,101,114,142,149,156,177,184; s:94; H:26; P:3
docs/observations/2026-09-07-plan-2/cellC/b02/b02smoke-receipt.json [Δ] f:15,18,21-22,25,49,51-52,54,66,91,96; v:15,18,21-22,25,49,51-52,54,66,91,96
docs/observations/2026-09-07-plan-2/cellC/b02/coldstart [Δ] f:17,40,53-54,109,111,113,121,149-150,214,281-282,486-487,502,540-541,586,607,614,638,650,654; a:281-282,486-487; v:17,40,53,650,654; y:541; s:154,383,457,546; H:54,109,111,113,121,149-150,214,502,540-541,586,607,614,638; T:541
docs/observations/2026-09-07-plan-2/cellC/b02/coldstart-grade [Δ] f:4,703,733,1681-1682,1764,1795,2367,2369; a:703-704,733-734; v:4,703,733,1764,2367,2369; y:155,920; s:1013,1017-1018,1107,1219,1343,1825,2066; H:1681-1682; T:155,920
docs/observations/2026-09-07-plan-2/cellC/b02/round [Δ] f:35,40,59,82-83,117,119-120,124,135,220-221,456,549,569,763,767,872,908,1092; a:549,569; v:35,40,59,119-120,124,135,1092; y:7,17,48,82,219,354; s:21,25,108-109,113,389,414,557,968,1886; H:82-83,117,220-221,456,763,767,872; T:7,17,48,82,219,354
docs/observations/2026-09-07-plan-2/cellC/b02/round-resume [Δ] f:38,43,55,60; p:44; v:38,55,60; y:5,193; H:43; P:44,194; T:5,193
docs/observations/2026-09-07-plan-2/cellC/b07-arms.md [Δ] f:25,42,57-58,130,136; v:25,42,57-58,136; H:130
docs/observations/2026-09-07-plan-2/cellC/b07-freeze.edn [Δ] f:131,133,136; v:131,133; H:136; P:21-22,27,42,69,83,102,121
docs/observations/2026-09-07-plan-2/cellC/brief-D1.md [Δ] f:1,3,5; p:3; v:1,3,5; H:1,5; P:1,3
docs/observations/2026-09-07-plan-2/cellC/brief-N5.md [Δ] f:1,5,7,11; v:1,5,7; H:1,11; P:1
docs/observations/2026-09-07-plan-2/cellC/brief-R3.md [Δ] f:1,3,5,7,11; v:1,3,5,7; H:1,3,11; P:1
docs/observations/2026-09-07-plan-2/cellC/brief-astra-night.md [Δ] f:8-9,11,14-15,25; a:11; v:8-9,14-15; H:8,11,25; P:14; T:14
docs/observations/2026-09-07-plan-2/cellC/cc-oracle.py [Δ] f:3; v:3
docs/observations/2026-09-07-plan-2/cellC/clojure-fast-feedback-SKILL-8004d35.md [Δ] s:61
docs/observations/2026-09-07-plan-2/cellC/coldstart/coldstart [Δ] f:6,22,32-33,79-81,94,106-107,122,156,159-161,164,233; a:106-107; v:6,22,32; y:160; s:85; H:33,79-81,94,122,156,159-161,164,233; T:160
docs/observations/2026-09-07-plan-2/cellC/coldstart/coldstart-grade [Δ] f:4; v:4; y:141; T:141
docs/observations/2026-09-07-plan-2/cellC/coldstart/counterexamples.md [Δ] f:7,16,86,168,306,418,467,478,622-623,798,823,872,1026,1049,1073; a:204,301; v:7,86,306,418,467,478,622-623,1049,1073; s:153,155,585,600,659,693,753,815; H:16,168,798,872,1026; P:466,477,489-492,563
docs/observations/2026-09-07-plan-2/cellC/coldstart/ledger.txt [Δ] s:39; P:110,114,117
docs/observations/2026-09-07-plan-2/cellC/coldstart/opus-B02-acceptance-4.md [Δ] f:15-16,49,185,189,191,194-196,199,212,235,276; v:15-16,189,212,276; H:185
docs/observations/2026-09-07-plan-2/cellC/opus-B02-acceptance-3.md [Δ] f:8,20-21,67,176,388,395,405,419,455,471,500,506,511,516,520,528,533,537,539,543,545,549,562,564,566,568,570,572,574,576; v:20-21,67,176,388,395,455,564,568,572,576; s:472,487,490,500,559; H:8,500,506,511,516,520,528,533,537,543,545,549,562,566,570,574; P:131,210
docs/observations/2026-09-07-plan-2/cellC/opus-B02-acceptance-4.md [Δ] f:15-16,49,185,189,191,194-196,199,212,235,276; v:15-16,189,212,276; H:185
docs/observations/2026-09-07-plan-2/cellC/out-D1.txt [Δ] f:1; H:1
docs/observations/2026-09-07-plan-2/cellC/out-D3.txt [Δ] f:1; H:1
docs/observations/2026-09-07-plan-2/cellC/out-N6.txt [Δ] f:1; H:1
docs/observations/2026-09-07-plan-2/cellC/out-round2-final.txt [Δ] f:1; v:1
docs/observations/2026-09-07-plan-2/cellC/r5adopt.log [Δ] f:1-2,5,10-11,14,18,23,28; v:1,5,10-11,14,23,28; H:1-2,5,18
docs/observations/2026-09-07-plan-2/cellC/sol-attacks-astra-squares.md [Δ] f:14-19; s:50,67; H:14-19
docs/observations/2026-09-07-plan-2/cellC/sol-squares-revisit.md [Δ] f:15,20,24-26,59,67-68,75; s:19,44; H:15,20,24-26,59,67-68,75
docs/observations/2026-09-07-plan-2/decisions.md [Δ] p:3; P:3
docs/observations/2026-09-07-plan-2/design-fable-split-verb.md [Δ] p:20,25; P:20,25
docs/observations/2026-09-07-plan-2/extraction-candidates.md [Δ] f:5-6,210,215,218-219,229; v:210,215,218-219,229; H:5-6
docs/observations/2026-09-07-plan-2/friction1-astra-report.md [Δ] f:6,17,19,39-40; a:6; v:17,39-40; H:19
docs/observations/2026-09-07-plan-2/friction1-sol-review-r1.md [Δ] f:12,17; v:17; H:12,17; T:34
docs/observations/2026-09-07-plan-2/friction1-sol-review-r2.md [Δ] f:5-7,20; s:8; H:6-7; T:22
docs/observations/2026-09-07-plan-2/manifest-validation.md [Δ] f:3-4,166; v:3-4,166
docs/observations/2026-09-07-plan-2/program-of-record.md [Δ] p:7; P:7
docs/observations/2026-09-07-plan-2/round.sh [Δ] f:33,65,67-68,72,99-100,260,337,355-356,433; a:337; v:33,67-68,72,433; y:7,22,98,164,356; s:325; H:65,99-100,260,355-356; T:7,22,98,164,356
docs/observations/2026-09-07-plan-2/run-bg.sh [Δ] f:6-7; v:6-7; y:3; T:3
docs/observations/2026-09-07-plan-2/split-build-astra-report.md [Δ] f:3-4,308,425,439,477,489,514,529,650-651; a:650-651; v:4,425,439,477,489,514,529; s:95,568; H:3,308; P:647
docs/observations/2026-09-07-plan-2/split-build-r1-verdict.md [Δ] f:37; v:37
docs/observations/2026-09-07-plan-2/split-build-r10-verdict.md [Δ] f:7-8,11; H:7-8,11; T:26
docs/observations/2026-09-07-plan-2/split-build-r11-verdict.md [Δ] f:6-9,11; v:11; H:6-9,11; T:24
docs/observations/2026-09-07-plan-2/split-build-r2-verdict.md [Δ] f:14; v:14; T:18
docs/observations/2026-09-07-plan-2/split-build-r3-verdict.md [Δ] f:17,33,37; v:37; H:17,33; T:39
docs/observations/2026-09-07-plan-2/split-build-r4-verdict.md [Δ] f:19,37; v:37; H:19,37; T:41
docs/observations/2026-09-07-plan-2/split-build-r5-verdict.md [Δ] s:12; T:26
docs/observations/2026-09-07-plan-2/split-build-r6-verdict.md [Δ] f:7-9; H:7-9; T:22
docs/observations/2026-09-07-plan-2/split-build-r7-verdict.md [Δ] f:14,16,18,22,34; H:14,16,18,22,34; T:47
docs/observations/2026-09-07-plan-2/split-build-r8-verdict.md [Δ] f:4,7; H:4,7; T:18
docs/observations/2026-09-07-plan-2/split-build-r9-verdict.md [Δ] f:3,5,13-15; v:3,5,13-15; T:19
docs/observations/2026-09-08-agent-grammar-adapters.md [Δ] f:3,19,42-43,55,77,120,126,131,148,309,311-312; a:3; v:19,42-43,55,309,311-312; s:6,12; P:42-43,55
docs/observations/2026-09-08-agent-grammar-consumers.md [Δ] f:3,26,93,97,106,112,118,153,248-249,302,306,320-321,372-373,389,401,409,416,427; a:3,302; v:26,93,97,106,112,118,153,248-249,306,320-321,372-373,389,401,409,416,427; s:19,314; P:92-93,112
docs/observations/2026-09-08-astra-B01-report.md [Δ] f:12-14,16,57,59,64,72,89,96; a:16; v:59,72,89; s:25,68; H:12-14,57,64,96
docs/observations/2026-09-08-batch5/b5-cc-opus-H-grade.txt [Δ] f:1,28,34,72,74; v:1,28,72,74; H:34; P:34
docs/observations/2026-09-08-batch5/b5-cc-opus-H-tip-grade.txt [Δ] f:1,17,28,34,56,72,74; v:1,17,28,56,72,74; H:34; P:17,34
docs/observations/2026-09-08-batch5/b5-cc-opus-W2-grade.txt [Δ] f:1,27,33,71,73; v:1,27,71,73; H:33
docs/observations/2026-09-08-batch5/b5-mvr-sol-H-grade.txt [Δ] f:1,28,34,72,74; v:1,28,72,74; H:34
docs/observations/2026-09-08-batch5/b5-mvr-sol-H-tip-grade.txt [Δ] f:1,17,28,34,56,72,74; v:1,17,28,56,72,74; H:34; P:17,34
docs/observations/2026-09-08-batch5/b5-mvr-sol-W-grade.txt [Δ] f:1,27,33,71; v:1,27,71; H:33
docs/observations/2026-09-08-batch5/evidence/cc/gate-green-receipt.edn [Δ] f:1; v:1; s:1; P:1
docs/observations/2026-09-08-batch5/evidence/cc/gate-green.log [Δ] f:1,9,11; v:1,9,11
docs/observations/2026-09-08-batch5/evidence/cc/gate-killed.log [Δ] f:1; v:1
docs/observations/2026-09-08-batch5/evidence/cc/kaocha-runs-listing-after-sigkill.txt [Δ] f:2-20; P:14
docs/observations/2026-09-08-batch5/evidence/cc/probe-green.txt [Δ] f:2; v:2
docs/observations/2026-09-08-batch5/evidence/cc/probe-red.txt [Δ] f:7; v:7
docs/observations/2026-09-08-batch5/evidence/mvr/gate-green-receipt.edn [Δ] f:1; s:1; P:1
docs/observations/2026-09-08-batch5/evidence/mvr/gate-green.log [Δ] f:1-2,12,14; a:6-8; v:1-2,12,14
docs/observations/2026-09-08-batch5/evidence/mvr/gate-killed.log [Δ] f:1; v:1
docs/observations/2026-09-08-batch5/evidence/mvr/kaocha-runs-listing-after-sigkill.txt [Δ] f:2-18; P:9
docs/observations/2026-09-08-batch5/evidence/mvr/probe-green.txt [Δ] f:2; v:2
docs/observations/2026-09-08-batch5/evidence/mvr/probe-red-receipt.edn [Δ] P:1
docs/observations/2026-09-08-batch5/evidence/mvr/probe-red.txt [Δ] f:7; v:7
docs/observations/2026-09-08-battery-parallel-verdict-parity.md [Δ] f:3,14; a:3; v:14; s:136; H:3
docs/observations/2026-09-08-blind-reader-witness.md [Δ] f:3,19,112,200,217,246,248-249; a:3; v:246,249; y:251; H:248; T:251
docs/observations/2026-09-08-gene-report-evening.md [Δ] f:23; a:1; s:25,29
docs/observations/2026-09-08-gene-report-night.md [Δ] s:18,26
docs/observations/2026-09-08-kaocha-plugins/astra-agent-reporter-build-report.md [Δ] f:1,3,24,26,43-44,54,59-60,72,84,87,106,108,112; v:59-60,72,84,108; H:1,3,24,26,54,87,108,112; P:102
docs/observations/2026-09-08-kaocha-plugins/astra-agent-reporter-riff.md [Δ] f:11,37,49,53,113; v:11; s:3,5; H:37,49
docs/observations/2026-09-08-kaocha-plugins/astra-kaocha-defects-2-report.md [Δ] f:38,42,59-60,66,68,74-75,81-82,88-89,95,97,103-104,110-111,117-118,124,130-131,137; p:66-68; v:38,42,59-60,66,68,74-75,81-82,88-89,95,97,103-104,110-111,117-118,124,130-131,137; P:66-68,130-131,137
docs/observations/2026-09-08-kaocha-plugins/astra-kaocha-defects-report.md [Δ] f:3,9-13; H:3,9-13
docs/observations/2026-09-08-kaocha-plugins/astra-kaocha-plugins-plan.md [Δ] f:4-5,84,90,98; a:5; v:84,90,98; H:4
docs/observations/2026-09-08-kaocha-plugins/astra-kaocha-plugins-report.md [Δ] f:6-8,30-32,66,86-88,90,95,98,102,107,111,128,157; a:8; v:6,95,102,128; H:7,30-32,66,86-88,90,98,107,111,157
docs/observations/2026-09-08-kaocha-plugins/astra-kaocha-status-report.md [Δ] f:3,14-17,20,24,59-60,75,77; v:14-17,20,24,59-60; H:3,75,77; P:15,59
docs/observations/2026-09-08-kaocha-plugins/astra-parent-outcome-report.md [Δ] f:3,8,10,12,16-19,26,29,32,35,40,43,48,51,56,59,64,67; v:12,16-19,26,29,32,35,40,43,48,51,56,59,64,67; H:3,8,10
docs/observations/2026-09-08-kaocha-watch/ccfp-A-coldstart.tsv [Δ] P:1-3
docs/observations/2026-09-08-kaocha-watch/ccfp-identity-trap.log [Δ] P:4-5,7-8
docs/observations/2026-09-08-kaocha-watch/ccfp-nrepl-stopwatch.tsv [Δ] P:12
docs/observations/2026-09-08-kaocha-watch/ccfp-watch-stopwatch.tsv [Δ] P:2-21
docs/observations/2026-09-08-kaocha-watch/mvr-A-coldstart.tsv [Δ] P:1-3
docs/observations/2026-09-08-kaocha-watch/mvr-nrepl-stopwatch.tsv [Δ] p:17; P:17
docs/observations/2026-09-08-kaocha-watch/mvr-smoke-watch-stopwatch.tsv [Δ] P:1
docs/observations/2026-09-08-kaocha-watch/mvr-smoke2-watch-stopwatch.tsv [Δ] P:1
docs/observations/2026-09-08-kaocha-watch/mvr-watch-stopwatch.tsv [Δ] P:2-21
docs/observations/2026-09-08-kaocha-watch/rename-probe.tsv [Δ] P:2-3,5-10
docs/observations/2026-09-08-kaocha-watch/rename-probe2.tsv [Δ] P:1-6
docs/observations/2026-09-08-kaocha-watch/tiny-watch.log [Δ] f:1-2; v:1-2; P:1-229
docs/observations/2026-09-08-namespace-split-papercuts-round2.md [Δ] f:53,64-65,233-235; a:235; v:53,64-65,233-234; H:65
docs/observations/2026-09-08-namespace-split-papercuts-round3.md [Δ] f:6,70,122,159,242,247; v:6,70,122,159,242,247; s:8
docs/observations/2026-09-08-night-heartbeat.md [Δ] f:6,60; p:32; v:6,60; H:6; P:32
docs/observations/2026-09-08-perfect-the-wins-tree.md [Δ] f:39,44; p:83; v:39,44; s:15; P:83
docs/observations/2026-09-08-recorder-v1.md [Δ] f:16,21,24,45,63,70,80,95,116,129,147-149,182,191,195,207,255,264-265,293,297,304,309,317,325-327,362-363,377,395,397,400,408,428,548,562-563,578,694-695,908,915,918,925,929-931,951,956,1125-1126,1132-1133,1177,1183,1186,1189,1197,1205-1206,1209,1212,1217,1225,1234,1237-1238,1240,1252,1261,1270,1278,1284,1286,1298,1300,1321; a:195,956,1189,1234,1252,1261,1270,1278,1300,1321; v:16,147-149,255,264,326-327,362-363,397,400,408,562-563,695,918,925,931,951,1177,1183,1209,1212,1217,1225,1237-1238,1240,1284,1286,1298; s:389; H:63,70,80,95,116,191,207,265,304,309,317,325,395,548,578,694-695,908,915,929-930,1186,1197,1205-1206; T:5,134,188,191,233,265,381,383,391,1186
docs/observations/2026-09-08-row1-cohort/astra-row1-cohort-prereg.md [Δ] f:5,7,17,19,35; v:5,7,19,35; s:23; H:17; P:7
docs/observations/2026-09-08-row1-cohort/astra-row1-cohort-report.md [Δ] f:22-25; v:22-25
docs/observations/2026-09-08-row1-cohort/freeze.edn [Δ] f:1; v:1; H:1; P:1
docs/observations/2026-09-08-row1-cohort/results.edn [Δ] f:1; p:1; v:1; H:1; P:1
docs/observations/2026-09-08-row1-split-ledger.md [Δ] f:4,94,118,295-303,311,317,324,338,365; v:4,118,295-303,317,338,365; s:64,69,133,270,331; H:94,311,324
docs/observations/2026-09-08-row2-alias-preregistration.md [Δ] f:39-42,89,211; v:211; H:39-42,89
docs/observations/2026-09-08-row2-entrance.md [Δ] f:1,17,177-178,180,186; a:1; p:122; v:17,177-178,180,186; y:28-29; s:108,174; P:122-123; T:28-29
docs/observations/2026-09-08-row2-fresh-seat-pilot-2.md [Δ] f:1,10,117,149,170,180,209-210,212-213,215; a:1; p:183; v:10,117,149,170,180,209-210,212,215; H:213; P:183
docs/observations/2026-09-08-row2-fresh-seat-pilot.md [Δ] f:1,9,105,125,130,141,147-148,150-151; a:1; p:124,134,140; v:9,105,125,130,141,147-148,150; y:17-18,28,92; H:151; P:124,134,140; T:5,17-18,28,92,96
docs/observations/2026-09-08-row2/d-receipts-stage3.txt [Δ] P:1
docs/observations/2026-09-08-row2/freeze-stage2.edn [Δ] f:2,6,9,13-14,23,26,33,35,47-48,81; p:32-33,35,38; v:2,6,9,13-14,23,26,35,47-48; H:33,81; P:32-33,35,38; T:38
docs/observations/2026-09-08-row2/freeze-stage3.edn [Δ] f:8,10,15,20,39,43,47,57,63,65,99,111; p:24,33; v:8,10,20,39,43,47,57,63,65,99; H:15,111; P:24,33; T:29,34
docs/observations/2026-09-08-row2/freeze.edn [Δ] f:2,8,26,39,68-69,81; v:2,8,26,39,68-69; H:81
docs/observations/2026-09-08-row2/results-stage2.edn [Δ] f:2-3; v:2-3
docs/observations/2026-09-08-row2/results-stage3.edn [Δ] f:3; v:3
docs/observations/2026-09-08-row2/revision-after-stage1.md [Δ] f:4,8; v:4,8
docs/observations/2026-09-08-row2/verdict-stage2.md [Δ] f:11; v:11
docs/observations/2026-09-08-row3-require-preregistration.md [Δ] f:9,64-66,95,260,539; v:260; H:9,64-66,95,539
docs/observations/2026-09-08-row3-schema-admission.md [Δ] f:60; v:60
docs/observations/2026-09-08-row5-adopt-2.md [Δ] f:3-4,129,136; a:3; v:4,129,136; s:68,111
docs/observations/2026-09-08-row5-adopt-3.md [Δ] f:3,7; a:3; v:7
docs/observations/2026-09-08-row5-adopt.md [Δ] f:3-4,137,177; a:3; v:4,137,177; s:23,100,113
docs/observations/2026-09-08-row5/freeze-stage1.edn [Δ] f:9,11,17-18,33,40,45,52,58-60,63-64,75-76,81,86,91-92; v:9,11,17,33,40,45,58-60,63-64,75-76,92; H:18,52,81,86,91; P:41
docs/observations/2026-09-08-row5/freeze-stage2.edn [Δ] f:5,29,31-33,35-38,40,43,47-48,65-66,95,110,131,134,150-151; v:5,29,31-33,35-38,40,47-48,65-66,95,131,134,150-151; s:8,54,79,82,93; H:43,110
docs/observations/2026-09-08-row5/freeze-stage3.edn [Δ] f:12,17,21-22,25,27,29-30,32,58-59,90,96,166,170; v:12,17,21-22,25,27,29-30,59,90,170; s:6,8,87-89,94,110,137,235; H:32,58,96,166; P:52
docs/observations/2026-09-08-row5/gate-verdict-stage2.txt [Δ] f:2,102; a:2; v:102; s:7,31
docs/observations/2026-09-08-row5/gate-verdict-stage3.txt [Δ] f:2,162; a:2; v:162; s:23,81,89,127
docs/observations/2026-09-08-row5/results-stage1.edn [Δ] f:2,15-16,23-24,31-32,39-40,47-48,55-56,139; v:2,15-16,23-24,31-32,39-40,47-48,55-56; H:139
docs/observations/2026-09-08-row5/results-stage2.edn [Δ] f:3,26,38,50,62,74,86; v:3,26,38,50,62,74,86; s:7,9,17-18,29-30,41-42,53-54,65-66,77-78,93,99,121; P:57
docs/observations/2026-09-08-row5/results-stage3.edn [Δ] f:4,27-28,45-46,59-60,72-73,88-89,100-101; v:4,27-28,45-46,59-60,72-73,88-89,100-101; s:11-13,19,32,40,50,64,77,93,139-140,158,162,167,176,214
docs/observations/2026-09-08-row5/stage3.json [Δ] s:127
docs/observations/2026-09-08-rows-sublime-3/astra-rows-sublime-3-report.md [Δ] f:5,7,9,15,26,34,40; a:5; v:9,34,40; s:40; H:7
docs/observations/2026-09-08-rows-sublime-3/background-closure.edn [Δ] f:1; v:1; H:1; P:1
docs/observations/2026-09-08-rows-sublime-3/sol-delta-fence-GO-WITH-FIX-2.md [Δ] f:14; v:14; P:5; T:53
docs/observations/2026-09-08-rows-sublime-3/sol-delta-fence-GO.md [Δ] T:45
docs/observations/2026-09-08-rows-sublime-3/sol-fence-GO-WITH-FIX.md [Δ] T:104
docs/observations/2026-09-08-rows-sublime-4-fence.md [Δ] f:18-21,25; v:18-21,25
docs/observations/2026-09-08-rows-sublime-4/astra-rows-sublime-4-report.md [Δ] f:4,15,25,180; a:15; v:25,180; s:184; H:4; P:49
docs/observations/2026-09-08-rows-sublime-4/make-test-receipt.json [Δ] f:6,16; v:16; H:6; P:13-14
docs/observations/2026-09-08-rows-sublime-4/receipt-sizes.edn [Δ] f:1; v:1
docs/observations/2026-09-08-rows-sublime-4/sol-fence-GO-WITH-FIX.md [Δ] f:26,147,188,192,380,501,544-546,550,554,742,863,905,910-912,916,920,1108,1229,1276,1280,1468,1589,1633,1637-1641,1654,1658,1846,1967,2012,2016-2020; v:26,147,188,192,380,501,545-546,550,554,742,863,910-912,916,920,1108,1229,1276,1280,1468,1589,1654,1658,1846,1967; H:544,905,1633,1637-1641,2012,2016-2020; P:149,503,865,1231,1591,1969
docs/observations/2026-09-08-rows-sublime/astra-rows-sublime-report.md [Δ] f:3,13,47,79,106,122-123,191; a:13; v:47,79,106,122-123,191; H:3,123
docs/observations/2026-09-08-rows-sublime/cold-receipt.edn [Δ] f:1; v:1; H:1
docs/observations/2026-09-08-rows-sublime/manual-cold-receipt.json [Δ] f:3,9-10,18-19; v:3,9-10,19; H:18
docs/observations/2026-09-08-rows-sublime/sol-fence-GO.md [Δ] f:6,65,78,86,93,107,109,115,129; v:6,86,107,109,115,129; s:140; H:78; T:149
docs/observations/2026-09-08-rows-sublime/warm-receipt.edn [Δ] f:1; v:1; H:1
docs/observations/2026-09-08-sublime-score-5-days.md [Δ] f:224,244; a:3-4; p:194; y:233; s:75,244,252,280; P:194,197,207; T:132,198,233
docs/observations/2026-09-08-tighten-binding-andon.md [Δ] f:1,4,21-25,30-33,62-63,71,123-124,127,145,149,152,163-164,178,198,242-243,249,269,377-378,411,417,486-487,508,559,562,564,582; a:1,127,249; v:4,33,123-124,149,163,178,242,269,377-378,411,417,486-487,508,559,562,564,582; r:558,560,582; s:387,393,408,411-412,417,420,434,438,457-458,480,485,498,502,504-505,507-508,517,519,524,533,559,562,564,569-570,572,581-582,586; H:21-25,30-32,62-63,145,149,152,164,198,243; T:76,85,89,208,217,222,244,254,260,262,266,271,294,307,310,312,377,393,395-398,418,479,514,521,558,560,563-564,572,581-582
docs/observations/2026-09-08-tighten-one-shots.md [Δ] f:1,7,23-25,28,35,39,45,133,155,187; a:1,35; p:38; v:7,23-25,28,39,155,187; H:23-25,45,155; P:38,68,70; T:14,23,35,45,63,68,78,81-82
docs/observations/2026-09-08-tighten/CHANGE-RULE.md [Δ] f:89,91; a:90; v:89; s:25,40,71; H:91
docs/observations/2026-09-08-tighten/SEAT-RECEIPT.md [Δ] f:33; a:61; s:60; H:33; T:3,16
docs/observations/2026-09-08-tighten/SKILL.md [Δ] f:15,136-139; a:127,137; v:136; s:81,92; H:15,137-139; T:17,24,54,101
docs/observations/2026-09-08-tighten/astra-autofix-riff.md [Δ] f:11,27; a:11,27; s:5,11,23,25,27,37,41,47,55,57,61; H:27
docs/observations/2026-09-08-tighten/astra-battery-floor-report.md [Δ] f:10,13,43,47-48,103; a:13; v:47-48,103; s:17; H:10,43
docs/observations/2026-09-08-tighten/astra-recorder-v1-design.md [Δ] f:5-6,34,38,45,50,58,66-68,103-104,118,136,138; v:5,67-68,103-104,138; s:130; H:6,45,50,58,66,136; T:6,122,124,132
docs/observations/2026-09-08-tighten/astra-sublime-axes.md [Δ] f:62,73,75,79,93,95,123,143; a:121; v:73,75,93,95,123; s:14,16,19,26,49,113; H:62,75,79,93,143; P:123
docs/observations/2026-09-08-tighten/astra-sublime-skill-review.md [Δ] f:5,8,40,45; v:5,8; s:19; H:5,8,40,45; T:7,41,45-46
docs/observations/2026-09-08-tighten/astra-t-plan-review.md [Δ] f:9; a:9; v:9; s:31,35,37,39,43,45,55,59,63,65,69,73; H:9; T:9,31,43,69
docs/observations/2026-09-08-tighten/astra-tighten-report.md [Δ] f:3,9-12; v:3,9-12; T:11,34-35
docs/observations/2026-09-08-tighten/battery-phases.md [Δ] f:3-5; a:4; v:3,5; s:179; H:4
docs/observations/2026-09-08-tighten/cells-verdict.md [Δ] f:3,5,7,12,14-15,30-32,39,45,52,70,73,75,79-80,84-86,89; a:3,7,31-32,73,86; v:5,14,30-32,45,70,80,86; y:21; H:12,15,30-32,39,52,75,79,84-86,89; T:4,21,30-32,58-59,86
docs/observations/2026-09-08-tighten/sol-battery-fresh-NO-GO.md [Δ] f:50; a:28; v:50; T:101
docs/observations/2026-09-08-tighten/sol-battery-parallel-GO-WITH-FIX.md [Δ] f:58,66; v:58; H:66; T:117
docs/observations/2026-09-08-tighten/sol-battery-parallel-fence2-NO-GO.md [Δ] f:16; v:16; T:65
docs/observations/2026-09-08-tighten/sol-t-plan-attack.md [Δ] s:35,41,43,45,47,58; T:41,45
docs/observations/2026-09-08-tighten/sol-tighten-review.md [Δ] f:13,17,45,62,64,66,78,110,158,162; a:17; v:158; s:123,166; H:13,64,110,158,162; T:7,17,64,94,134,140,146,150,158
docs/observations/2026-09-08-tighten/sublime-every-day-SKILL.astra.md [Δ] f:10-11,35-36,38,44,63-73,104-105; a:10,63,90; v:38,105; s:69,91,97; H:11,35-36,44,63-73,104-105; P:34; T:65,69,104
docs/observations/2026-09-08-tighten/sublime-every-day-SKILL.fable.md [Δ] f:39,43,49,51,53,55,57,59,61; v:43; s:78; H:39,49,51,53,55,57,59,61; T:41,49,57
docs/observations/2026-09-08-two-box-plan.md [Δ] a:1,3,5,34; s:20,32
docs/observations/2026-09-08-vitest-specimen.md [Δ] f:3,262; a:3,38; v:262; P:173
docs/observations/2026-09-09-gene-report-night.md [Δ] a:1; r:28; s:10,22,31; T:28
docs/observations/2026-09-09-night-reconciliation/gene-report-night-before-reconciliation.md [Δ] f:20; a:24,50; v:20
docs/observations/2026-09-09-night-reconciliation/watchdog-receipt.json [Δ] f:23; v:23
docs/observations/admit-gate-fix-round1-review-opus.md [Δ] f:6,46,143,172,255,272-273,277,424; a:5; s:14,50,117,140,167,246,274,375,460,476,478; H:6,46,143,172,255,272-273,277,424; T:322-323,415,420-421,424
docs/observations/admit-gate-round11-review-opus.md [Δ] f:9-10,34,45,99,115,157,247,364; v:10,34,45,99,115,247,364; H:9,157; P:11; T:34,99,115,160,171,247,363,384
docs/observations/admit-gate-round11-review-sol-partial.md [Δ] f:26,32,34-35,37-41; v:26,32,34-35,37-41; T:26
docs/observations/admit-gate-round14-review-opus.md [Δ] f:7,9,29,46,64,93,178,281,288,316,345,350; v:9,29,46,64,93,316; H:7,178,281,288,345,350; P:11; T:29,64,93,182,193,224,241,316
docs/observations/admit-gate-round17-review-opus.md [Δ] f:5,7,48,52-53,71-74,130,139,520,647; a:354; v:7,48,52-53,71,130,139,647; H:5,520; P:8,147-148
docs/observations/admit-gate-round3-review-sol.md [Δ] f:8,14,35,45,51,83,89,101,107,114,123,134,140,149,158,236; v:8,14,35,45,51,83,89,101,107,114,123,134,140,149,158,236; T:8,29,45,83,101,117,134,152,187,202,230
docs/observations/admit-gate-round4-review-opus.md [Δ] f:5,10,53,59,82,157,163,174,304,374-375,379; v:5,53,59,82,157,163,174,374-375; H:5,10,53,82,304,379; T:53,82,157,174,307,314,329
docs/observations/admit-gate-round5-review-opus.md [Δ] f:5-6,11,63,69,223,229,322,426,481,563-564,570; v:5,63,69,223,229,481,563-564; H:6,11,63,223,426,570; T:63,223,427,430,437,452,553
docs/observations/admit-gate-round6-review-opus.md [Δ] f:5-7,12,89,202,278,407; p:19; v:5,89,202,278; H:6-7,12,89,202,278,407; P:19; T:89,202,278,407,412,417,422
docs/observations/admit-gate-round7-review-sol.md [Δ] f:24,30,59,78,102,112-113,123,164,192,209,217,248,253-254; v:24,30,59,78,102,112-113,123,164,192,209,217,248,253-254; s:118; T:24,59,78,123,150,164,187,192,197,208
docs/observations/admit-gate-round9-review-sol.md [Δ] f:5,29,31,59,145,235-236,253-254,262,303; v:5,29,31,59,145,235-236,253-254,262,303; s:96,113; T:8,45,62,78,99,118,148,184,200,229,244
docs/observations/battery-ledger.edn [Δ] a:1-57
docs/observations/canary/2026-09-08.md [Δ] f:10,13,22,31,40; v:10,13,22,31,40; H:10
docs/observations/census-round16-rereview-opus.md [=] f:3,43,45,66,79-80,110,112,114,147,153,185,188,207,229,232,244,272,315,317,344,348,375,381,387,391,395,400,405-406; p:6; H:3,43,45,66,79-80,110,112,114,147,153,185,188,207,229,232,244,272,315,317,344,348,375,381,387,391,395,400,405-406; P:6; T:341,344,348
docs/observations/census-round17-rereview-opus.md [Δ] f:6,500,505; p:10; H:6,500,505; P:10,62; T:500,505
docs/observations/census-round18-rereview-sol.md [Δ] f:5; s:374; H:5; T:241,272,298-299,335,342
docs/observations/census-round19-review-opus.md [Δ] f:6,8,40,255,443,489; v:8,40,255,443,489; H:6; T:539,549,568-569,633,640
docs/observations/census-round21-review-opus.md [Δ] f:7,9,19-20,88-89,96,106,278; v:9,19-20,88-89,96,106; H:7,278; P:380; T:399,404
docs/observations/census-round23-review-opus.md [Δ] f:6,13,25,144,146,517,522,524-528; v:6,517,522,524-528; s:169,327,357; H:13,25,144,146; T:335,420,422,427-428
docs/observations/census-round25-review-opus.md [Δ] f:10,20,24,49,57,81,84,87; v:24,57,81,84,87; s:115,189,407,571; H:10,20,49; T:324-325,533
docs/observations/evidence/2026-08-30-from-to-double-repetition-receipt.json [=] P:513,521
docs/observations/evidence/2026-08-30-walls-of-text-exhibit-receipt.json [=] P:293,295
docs/observations/evidence/consumption-gap-20260830/phase1-coverage.json [=] P:29
docs/observations/evidence/owner-aware-prerequisite-bf98571.edn [=] U:64; P:60
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/bonus/01-Q/codex-config.toml [=] U:3
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/bonus/01-Q/mcp-telemetry/fe07de7c-6347-4a0d-ab4c-cc143f28f970.jsonl [=] P:1
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/bonus/02-R/codex-config.toml [=] U:3
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/bonus/02-R/events.jsonl [=] P:78,83
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/bonus/02-R/mcp-telemetry/96e6d987-686c-40ba-b90a-221d6a9b77a4.jsonl [=] P:18
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/bonus/02-R/proxy-receipts.jsonl [=] P:16-18,33,42,70
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/bonus/02-R/proxy-stream.jsonl [=] P:71,75
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/01-Q/codex-config.toml [=] U:3
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/01-Q/events.jsonl [=] P:10
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/01-Q/mcp-telemetry/0889b925-aea7-48ea-ac3d-e1024cfb7d20.jsonl [=] P:5
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/01-Q/proxy-receipts.jsonl [=] P:10
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/01-Q/proxy-stream.jsonl [=] P:13
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/02-R/codex-config.toml [=] U:3
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/02-R/events.jsonl [=] P:8
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/02-R/proxy-receipts.jsonl [=] P:6
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/02-R/proxy-stream.jsonl [=] P:9
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/03-R/codex-config.toml [=] U:3
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/03-R/events.jsonl [=] P:7
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/03-R/mcp-telemetry/8e99c196-2dd2-46c2-a131-0c0dfaaa87a2.jsonl [=] P:4
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/03-R/proxy-receipts.jsonl [=] P:6
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/03-R/proxy-stream.jsonl [=] P:9
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/04-Q/codex-config.toml [=] U:3
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/04-Q/episode.json [=] P:34
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/04-Q/score-rescored.json [=] P:47
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/04-Q/score.json [=] P:47
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/05-R/codex-config.toml [=] U:3
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/05-R/events.jsonl [=] P:7
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/05-R/proxy-receipts.jsonl [=] P:6
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/05-R/proxy-stream.jsonl [=] P:9
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/06-Q/codex-config.toml [=] U:3
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/06-Q/episode.json [=] P:34
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/06-Q/proxy-receipts.jsonl [=] P:1
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/06-Q/proxy-stream.jsonl [=] P:12
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/06-Q/score-rescored.json [=] P:47
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/06-Q/score.json [=] P:47
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/07-Q/codex-config.toml [=] U:3
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/07-Q/events.jsonl [=] P:5
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/07-Q/proxy-receipts.jsonl [=] P:7
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/07-Q/proxy-stream.jsonl [=] P:7
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/08-R/codex-config.toml [=] U:3
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/08-R/events.jsonl [=] P:9
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/08-R/proxy-receipts.jsonl [=] P:7-8
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/08-R/proxy-stream.jsonl [=] P:11
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/09-Q/codex-config.toml [=] U:3
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/09-Q/proxy-stream.jsonl [=] P:6,8,10
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/10-R/codex-config.toml [=] U:3
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/10-R/events.jsonl [=] P:8
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/10-R/proxy-receipts.jsonl [=] P:6
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/10-R/proxy-stream.jsonl [=] P:9
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/11-R/codex-config.toml [=] U:3
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/11-R/episode.json [=] P:34
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/11-R/events.jsonl [=] P:8,10
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/11-R/mcp-telemetry/2f8dd4a4-cd41-4cd3-8c70-ee383319d899.jsonl [=] P:1-5
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/11-R/proxy-receipts.jsonl [=] P:8,10
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/11-R/proxy-stream.jsonl [=] P:11,13
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/11-R/score-rescored.json [=] P:47
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/11-R/score.json [=] P:47
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/11-R/workspace/.receipts/310bffe7-b7e8-4399-8571-9afb8e97a613.edn [=] P:1
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/12-Q/codex-config.toml [=] U:3
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/13-R/codex-config.toml [=] U:3
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/13-R/events.jsonl [=] P:10
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/13-R/proxy-receipts.jsonl [=] P:10
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/13-R/proxy-stream.jsonl [=] P:6,13
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/14-Q/codex-config.toml [=] U:3
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/15-Q/codex-config.toml [=] U:3
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/15-Q/proxy-receipts.jsonl [=] P:11-12
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/16-R/codex-config.toml [=] U:3
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/16-R/episode.json [=] P:34
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/16-R/events.jsonl [=] P:9
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/16-R/proxy-receipts.jsonl [=] P:8-10
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/16-R/proxy-stream.jsonl [=] P:6,8,10-12
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/16-R/score-rescored.json [=] P:47
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/cohort/16-R/score.json [=] P:47
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/manifest-original.sha256 [=] P:7,11,35,49,62,149,158,197,218
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/manifest.sha256 [=] P:7,12,38,53,67,161,171,213,238
docs/observations/evidence/splice-reference-adversarial-replication-20260830/raw/run-config.json [=] U:133
docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/01-Q/codex-config.toml [=] U:3
docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/02-R/codex-config.toml [=] U:3
docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/03-R/codex-config.toml [=] U:3
docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/03-R/mcp-telemetry/b662352c-1f96-4587-9863-c4e493693ae1.jsonl [=] P:3
docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/04-Q/codex-config.toml [=] U:3
docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/04-Q/episode.json [=] P:34
docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/04-Q/score.json [=] P:42
docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/05-R/codex-config.toml [=] U:3
docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/06-Q/codex-config.toml [=] U:3
docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/07-Q/codex-config.toml [=] U:3
docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/07-Q/proxy-receipts.jsonl [=] P:5,7
docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/08-R/codex-config.toml [=] U:3
docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/08-R/proxy-receipts.jsonl [=] p:4; P:1,3-4,6
docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/09-Q/codex-config.toml [=] U:3
docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/09-Q/episode.json [=] P:30
docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/09-Q/mcp-telemetry/852cb1a4-8694-4301-9617-bb972e5d8939.jsonl [=] P:3
docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/10-R/codex-config.toml [=] U:3
docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/10-R/proxy-stream.jsonl [=] P:8
docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/11-R/codex-config.toml [=] U:3
docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/12-Q/codex-config.toml [=] U:3
docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/12-Q/proxy-receipts.jsonl [=] P:6
docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/13-R/codex-config.toml [=] U:3
docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/14-Q/codex-config.toml [=] U:3
docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/15-Q/codex-config.toml [=] U:3
docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/16-R/codex-config.toml [=] U:3
docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/16-R/events.jsonl [=] P:6
docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/16-R/mcp-telemetry/c4d22dc8-4ed0-453f-875b-1ba9a8cf2951.jsonl [=] P:3
docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/16-R/proxy-receipts.jsonl [=] P:6
docs/observations/evidence/splice-reference-screen-20260830/raw-v3/cohort/16-R/proxy-stream.jsonl [=] P:9
docs/observations/evidence/splice-reference-screen-20260830/raw-v3/manifest.sha256 [=] P:7,39,65,95,127,191,193
docs/observations/evidence/splice-reference-screen-20260830/raw-v3/pilot/01-Q/codex-config.toml [=] U:3
docs/observations/evidence/splice-reference-screen-20260830/raw-v3/pilot/01-Q/mcp-telemetry/b39cd118-6730-4df5-88c2-54e8011db4e8.jsonl [=] P:3
docs/observations/evidence/splice-reference-screen-20260830/raw-v3/pilot/02-R/codex-config.toml [=] U:3
docs/observations/evidence/splice-reference-screen-20260830/raw-v3/pilot/02-R/proxy-receipts.jsonl [=] P:2
docs/observations/evidence/splice-reference-screen-20260830/raw-v3/run-config.json [=] U:128
docs/observations/evidence/splice-reference-screen-20260830/raw/manifest.sha256 [=] P:1
docs/observations/evidence/splice-reference-screen-20260830/raw/pilot/02-R/episode.json [=] P:23
docs/observations/evidence/startup-memory/local-20260824-summary.tsv [=] U:5-8
docs/observations/evidence/startup-memory/xmx-1024m/class-histogram-ready.txt [=] p:7565,7909; P:9,17,128,1334-1335,1714-1715,3488-3493,6147,7561,7565,7576,7891,7893,7897-7898,7909
docs/observations/evidence/startup-memory/xmx-1024m/gc-safepoint.log [=] P:112,206,446,459,507,510,526,542,640,815,826
docs/observations/evidence/startup-memory/xmx-1024m/heap-ready.txt [=] P:2
docs/observations/evidence/startup-memory/xmx-1024m/heap-settled.txt [=] P:2
docs/observations/evidence/startup-memory/xmx-1024m/jvm-process.txt [=] H:1
docs/observations/evidence/startup-memory/xmx-1024m/rss-samples.tsv [=] P:9,14
docs/observations/evidence/startup-memory/xmx-2048m/class-histogram-ready.txt [=] p:7909,8142; P:1814-1815,2206-2207,3995-4000,6707,7891,7893,7897-7898,7909,8138,8142,8153,8174,8303-8342
docs/observations/evidence/startup-memory/xmx-2048m/gc-safepoint.log [=] P:50,206,459,789
docs/observations/evidence/startup-memory/xmx-2048m/jfr-runtime-context.txt [=] P:8
docs/observations/evidence/startup-memory/xmx-2048m/jfr-summary.txt [=] P:13
docs/observations/evidence/startup-memory/xmx-2048m/jvm-process.txt [=] H:1; P:1
docs/observations/evidence/startup-memory/xmx-2048m/probe-metadata.txt [=] P:4
docs/observations/evidence/startup-memory/xmx-2048m/ready.edn [=] P:1
docs/observations/evidence/startup-memory/xmx-2048m/rss-samples.tsv [=] P:32
docs/observations/evidence/startup-memory/xmx-2048m/stderr.log [=] P:1
docs/observations/evidence/startup-memory/xmx-2048m/telemetry/3b2aff5c-63d3-4285-9335-8778671c1976.jsonl [=] P:1
docs/observations/evidence/startup-memory/xmx-2048m/time.txt [=] P:2
docs/observations/evidence/three-arm-request-shape-6328db5.edn [=] U:83
docs/observations/evidence/three-arm-request-shape-a9a9d5d.edn [=] U:81
docs/observations/evidence/three-arm-request-shape-b298b8b.edn [=] U:84
docs/observations/evidence/verb-algebra-census-20260830/receipt.json [=] P:191,197,208
docs/observations/fanout-scorer-round3-review-sol.md [Δ] f:28,38,40,50-51,63,65,78-79,371,409; H:371,409
docs/observations/fanout-scorer-round4-review-opus.md [Δ] f:4,6,38-39,41,44,52,56,71-73,80,92,103,109,143,150,154,164,197-198,237,253-254,403,405,551-552,638,646-650; s:149; H:4,6,38-39,41,44,52,56,71-73,80,92,103,109,143,150,154,164,197-198,253-254,551-552,638,646-650
docs/observations/fanout-scorer-round5-review-sol.md [Δ] f:3,8,29-31,53-55,77-78,92,103-104,124-125,147,151,155,163-164,182-183,193,202-203,215,229-233,238-239,252,271,301,327,344,349,356,372-373,401-404,407; v:29-31,53-55,77-78,92,103-104,124-125,147,151,155,163-164,182-183,193,202-203,215,229-232,238-239,252,271,301,327,349,356,372-373,401-404; H:3,8,233,239,344,349,356,373,407
docs/observations/feature-thread-round1-review-opus.md [Δ] f:4-5,10,396,460-462,469,472; a:4; p:468; v:5,396,460-461,472; s:409; H:10,462,469; P:397,468; T:162-163,398
docs/observations/feature-thread-round12-review-sol.md [Δ] f:24,26-27,67,91,114,122-123,162-165,171-172,175-179,181-182,186,197-200,217-219,237-240,263,278,283-284,326-328,352-356,379-380,389-398,428,458,463-466,473; v:24,26-27,67,91,114,122-123,162-165,171-172,175-179,181-182,186,197-200,217-219,237-240,263,284,326-328,352-356,379-380,389-398,428,458,463-466,473; s:62,278; H:24,278,283-284,352; P:208-209; T:326-328,389-392,398
docs/observations/feature-thread-round3-review-opus.md [Δ] f:4-5,11,645-646,648,656,658; a:4; p:654; v:5,645-646,658; H:11,648,656; P:654; T:289-290,659
docs/observations/feature-thread-round5-review-sol.md [Δ] f:3,14,20,25,31-32,59,65,89,95,98,123-127,133-134,153,173-174,179,192,207-208,214,216,218,220,222,224,226,238,257,263,269,289-292,303,311,320,330,336,369,378,418,433,525-526,532,535-536; v:20,25,31-32,59,65,89,95,98,123-127,133-134,153,173-174,179,192,207-208,214,238,257,263,269,290-292,303,311,320,330,336,369,378,418,433,525-526,532,535-536; s:472; H:3,14,59,89,207-208,216,218,220,222,224,226,289-290,330; T:352,379,398-400
docs/observations/feature-thread-round7-review-sol.md [Δ] f:23,30,38,58,73,92,111-113,156,179,185,203-204,227,252-253,281-286,351-353,356-359,384,393-394,420,438-441,449,458,471,496-499; v:23,38,58,73,92,111-113,156,179,185,204,227,252-253,281-286,351-353,356-359,384,393-394,420,438-441,449,458,471,496-498; H:30,203-204,499; T:351-353,358,420,458
docs/observations/feature-thread-round9-review-sol.md [Δ] f:23,30,38,53,68,73,79,89,103,117,135,156,179,189,206,222,248,265,290-292,296; v:23,38,53,68,73,79,89,103,117,135,156,179,189,206,222,265,290-292,296; H:30,248; T:290-292
docs/observations/gate-namespace-walls.edn [Δ] s:1
docs/observations/mem003-second-landing-round1-review-sol.md [Δ] f:282,293,303-304; s:347; H:282,293,303-304; T:8,71,86,101,131,170,202,227,263,335,352-354,368,373,387,416,428,450,465
docs/observations/mem003-second-landing-round10-review-opus.md [Δ] f:8,18,78,104,459,514,562-563,595,613; v:18,78,104,459,613; H:8,514,562-563,595; P:17; T:562
docs/observations/mem003-second-landing-round2-review-sol.md [Δ] f:235,275,281-283,300,309; a:137; H:235,275,281-283,300,309; P:267,419; T:8,21,69,93,106,123-125,168,206,208-209,323,344,354,375,384,394
docs/observations/mem003-second-landing-round3-review-opus.md [Δ] f:5-6,44-45,77,225-226,344,359,467-468,480-481,486,553,555-556; H:5-6,44-45,77,225-226,344,359,467-468,480-481,486,553,555-556; P:447; T:60,109,143,181,225-227,323,438,457-458,557
docs/observations/mem003-second-landing-round4-review-opus.md [Δ] f:5,7,11,77,96,116,135,161,171,246,284,319,386,439,462-463,478,492-493; v:7,77,96,116,135; s:546; H:5,11,161,171,246,284,319,386,439,462-463,478,492-493; T:96,135,319,461,468-469
docs/observations/mem003-second-landing-round5-review-opus.md [Δ] f:6,9,12,23,25,76,86,96,116,134,187,198,229,233,263,276,299,309,332,415,435,505,544-545,579,622-623,646-648,656,679; v:9,23,25,76,86,96,116,134,187,198,229,233,299,309,505,544-545; s:343,366,376,736; H:6,12,76,263,276,332,415,435,579,622-623,646-648,656,679; T:76,96,134,198,233,276,309,415,505,621,630-631
docs/observations/mem003-second-landing-round6-review-opus.md [Δ] f:6,9,12,22,25-26,73,100,119,132,271,284,321,402,444,531,621-622,638,649,680,684-687,711,730,752; v:9,22,25-26,73,100,119,132,271,284,402,444,531,621-622,752; H:6,12,321,638,649,680,684-687,711,730; P:623; T:73,100,132,271,402,531,584,588-589
docs/observations/mem003-second-landing-round8-review-opus.md [Δ] f:9,18,61,64,106,127,130,194,366,411,480,515,573-576,579,594,615,652,655; p:654; v:18,61,64,106,127,130,194,366,411,515,574,652,655; s:689; H:9,480,515,573,575-576,579,594,615; P:653-654; T:130,521,523
docs/observations/namespace-split-r3/astra-r3-final-cold.txt [Δ] f:4-5; v:4-5
docs/observations/namespace-split-r3/astra-r3-final-warm.txt [Δ] f:2; v:2
docs/observations/q5z-round11-rereview-opus.md [=] f:5,8-9; H:5,8-9; P:17-18; T:20,30-31,64,84,198,239,256,294,359,388
docs/observations/q5z-round12-rereview-sol.md [Δ] f:364; H:364; T:27,53,80,99,120,131,137,148,217,244,301,311,317,390
docs/observations/q5z-round13-rereview-sol.md [Δ] f:198,224; H:198,224; P:198; T:8,31-32,55,66,112,127,147,164,174,203,233,253,304,309
docs/observations/q5z-round14-rereview-sol.md [Δ] f:8,23,48,68-70,99-100,126,144,165,191,202-203,219,230-231,279; H:8,23,48,68-70,99-100,126,144,165,191,202-203,219,230-231,279; T:8,23,48,68-70,99-100,126,202,230,279,314,319
docs/observations/q5z-round15-review-sol.md [Δ] f:8,14,27,33,39,49-51,57,78-80,85-86,92-93,107-108,115,200-201,209-210,225-226,236,242-244,250-251,275,282,290,294,303,309,391; v:8,14,27,33,39,49-51,57,78-80,85-86,93,107-108,115,200-201,209-210,226,236,242-244,251,275,282,290,294,303,309,391; H:50-51,92,108,225,236,250,294; P:377; T:8,27,49-51,107-108,236,303,329,336
docs/observations/q5z-round16-review-opus.md [Δ] f:8,13,43,67,79,98,117,161-162,166-167,186,242,348,352-353; p:364; v:13,43,67,79,98,117,161,166-167,186,242,352-353; s:175; H:8,162,348; P:364; T:43,67,79,98,117,150,186,242,295,300,327
docs/observations/q5z-round17-review-opus.md [Δ] f:5-6,9,38,71,112,189,262,342,389; v:6,38,71,112,189,262,342; s:22,295,347,429,471; H:5,9,389; T:38,71,112,189,262,282,304,413,416
docs/observations/q5z-round18-review-sol.md [Δ] f:12,18,69,169,175,182,194,203,211,223,229,248,254,284,320-321,327-328,367-368,374-375,402-403,409-410,428-429,435,447-448,454,469,475,484,489; v:12,18,69,169,175,182,194,203,211,223,229,248,254,284,320-321,327-328,367-368,374-375,402-403,409-410,428-429,435,447-448,454,469,475,489; s:102,243; H:203; P:203; T:12,69,107,121,223,248,321,368,403,429,448,469
docs/observations/seat-receipts/forge@anvil/2026-09-08.edn [Δ] f:21,24,34-35,38,40; a:21; v:38,40; s:5; H:24,34-35; T:1,5-6,10,18-19,24
docs/observations/sentinel/alias_migration.md [Δ] f:18,26; p:3,11; v:18,26; P:3,11,20,29,32; T:30
docs/observations/sentinel/namespace_split.md [Δ] f:14,22; v:14,22
docs/observations/study-ops-o2-round11-review-sol.md [Δ] f:52-53,59,63,78-79,85,89,112,118,135,137,142-144,150,155,159,161,163,203,227-228,270,300; v:52-53,59,63,78-79,85,89,112,118,142-144,150,155,159,161,163,203,227-228,270; s:135; T:52-53,78-79,112,142-144,203,227-228,247-249,251
docs/observations/study-ops-o2-round13-review-sol-partial.md [Δ] f:28,34,75-76,90,94,102; v:28,34,75-76,90,94,102; T:28,68
docs/observations/study-ops-o2-round2-review-sol.md [Δ] f:3,32,38,403,421,424; s:16,301; H:3,32,38,424; P:62; T:332,392,533,547,576,591
docs/observations/study-ops-o2-round3-review-sol.md [Δ] T:244,263,286,364,425,435,463,468,511,524
docs/observations/study-ops-o2-round4-review-opus.md [Δ] f:4-5,16,19,51,68-69,90,132,146,181,219,236,312,352,407,444,453,462,472,480,495; v:5,16,51,68-69,90,132,146,181,219,236,312,352,407,495; s:200; H:4,19,51,68,90,132,146,181,219,236,312,352,444,453,462,472,480; P:29,495,508; T:444,453,472,495
docs/observations/study-ops-o2-round5-review-sol.md [Δ] f:3,23,61,67,80,86,158,164,178-179,186,196,211,281,291,295,302,312,316,323,337,341,402-403; v:3,23,61,67,80,86,158,164,178-179,186,196,211,281,291,295,302,312,316,323,337,341,402-403; H:23; T:61,80,158,178-179,211,281,291,302,312,323,337,352,367,396
docs/observations/study-ops-o2-round6-review-sol.md [Δ] f:14,20,32,52-53,74,113,128,149,154,172,183,185,256,325,410; v:32,52-53,74,113,128,154,172,183,185,256,325,410; s:147; H:14,32; T:52-53,74,113,128,154,256,325,381,391,402
docs/observations/study-ops-o2-round7-review-sol.md [Δ] f:21,40,46,74,80,113,128,139-140,146,149,164,169,175-176,186,193,203,214,216,228,234,288,351,358,458-459; v:21,40,46,74,80,113,128,139-140,146,149,169,175,186,193,203,214,216,228,234,288,351,358,458-459; s:134,223; P:329,341; T:40,74,107,122,139-140,169,186,228,288,351,419,433,448
docs/observations/study-ops-o2-round9-review-sol.md [Δ] f:8,14,27,33,59,69-70,97,103-104,109,119-120,137,155,161,170,176,183,188,194,210,217,224,235,246,248,260,266,276,282,319,326,461-462,467,498; v:8,14,27,33,59,69-70,97,103-104,109,119-120,137,155,161,170,176,188,194,210,217,224,235,246,248,260,266,276,282,319,326,461-462,498; s:150,255; P:498; T:8,27,53,69-70,97,119-120,137,155,170,188,204,217,260,276,319,434-436,438
docs/observations/suite-spike-round2-review-sol.md [Δ] f:54,72-74,80-81,104,126,146,171,310,316,318,331,345,347-348,361,390,399,417,429,436,458,475,485,499,537,586,607,629,750; a:680,702; v:72-74,80,104,126,146,171,316,318,345,347-348,390,399,417,429,436,458,475,485,499,537,586,607,629; s:581; H:54,72-73,81,310,331,361,750; P:310,348; T:390
docs/observations/suite-spike-round3-review-sol.md [Δ] f:29,37,78,85,247,252; v:252; s:112,264; H:29,37,78,85,247; T:3,53,260
docs/observations/tmp-leak-round1-review-opus.md [Δ] f:4,6,9,15,55,59,61,73,132,134,137,167,171,178,221,248,251-253,322,328,336-339,346,350-351,359,390,400,448; a:4,298; v:15,55,59,73,132,134,137,167,171,178,221,248,251-252,322,328,336-339,346,350-351,359,390,400,448; H:6,9,61; P:339; T:88,189,196,257,408,413
docs/observations/tmp-leak-round2-review-opus.md [Δ] f:4,6,9,15-16,74,136,169,228,248,290-291,367,370; a:4; v:15-16,74,136,248,290-291,367,370; H:6,9; T:268,306,311
docs/observations/tmp-leak-round3-review-opus.md [Δ] f:5,7,19-20,28,33,49,138,179,198,200,204,209,290,403,410-411,413,428; a:5,152-154,156-157; v:19-20,138,179,198,200,204,209,403,410-411,413; H:7; P:383; T:245,265,276,357
docs/plans/2026-08-24-five-x-sci-editor-golf-review-brief.md [=] a:110
docs/plans/2026-08-26-five-times-native-next-12-hours.md [=] a:78,177,185,217,219; s:102
docs/plans/2026-08-26-next-hill-experiment-portfolio.md [=] s:30
docs/plans/2026-08-27-brain-fleet-next-hills.md [=] a:159
docs/plans/2026-08-27-mcp-mutation-tool-naming-options.md [=] s:453
docs/plans/2026-08-30-gpt-5-3-spark-caller-screen.md [=] a:17,115
docs/plans/2026-09-06-astra-patch-proof-usage.md [Δ] s:121
docs/plans/README.md [=] a:26,30; s:167
docs/plans/adaptive-clj-surgeon-interface-ethnography.md [=] a:3,15,29,36,43,74,185,225,264,277; H:33-34,81-83
docs/plans/alias-migration-binding-scope-repair.md [Δ] f:20; v:20; P:11,22
docs/plans/alias-migration-mixed-refer-repair.md [Δ] f:31; v:31; s:15,17
docs/plans/anvil-development-surface.md [=] a:1,7,17,25,28,39,55,57,77,80,86,96,107,145,152; s:49
docs/plans/atlas-paper-exercises.md [=] s:76-77,214,226
docs/plans/battery-launcher-matrix.md [Δ] f:17-18; v:17-18
docs/plans/bounded-cclsp-workspace-lifecycle.md [=] a:279-280; s:85,98,100,147; U:39,49,56,258,260,262,275,279,286,294
docs/plans/cli-public-operation-envelope-gap-analysis.md [=] a:242,264,297; P:257
docs/plans/clojure-native-edit-algebra.md [=] s:40,334,341
docs/plans/commit-counterfactual-replay.md [=] a:181,203,211,216-217
docs/plans/compact-exact-owner-deletion.md [=] s:5
docs/plans/compiled-edit-transaction-options.md [=] f:168; s:162
docs/plans/containing-line-edit-root.md [=] s:80
docs/plans/contract-and-runtime-coherence.md [=] s:62
docs/plans/cross-caller-mcp-extraction-benchmark.md [=] a:38,86; s:38; P:44
docs/plans/failure-atomic-extraction.md [=] s:28
docs/plans/friction-matcher-and-cardinality.md [Δ] f:18,20,26; v:18,26
docs/plans/global-compact-editor-routing.md [=] a:3,8,75,88,98,111,113; P:100
docs/plans/helper-closure-extraction.md [Δ] f:10; v:10
docs/plans/host-wide-clj-kondo-admission.md [=] a:108
docs/plans/hybrid-compiled-edit-transaction.md [=] s:119
docs/plans/intent-contract-all-prefixes.md [Δ] f:25,70; v:25,70; s:47,60
docs/plans/intent-transactions.md [=] s:99-100,151-152
docs/plans/live-contract-and-semantic-source-handshake.md [=] s:585
docs/plans/mcp-extraction-planning.md [=] P:154,184
docs/plans/mission-git-ledger.md [Δ] T:38
docs/plans/mission-git-receipt.md [Δ] f:162; a:55; v:162; T:164
docs/plans/mission-typist-executor.md [Δ] f:232; v:232
docs/plans/mission-typist-fallback.md [Δ] f:44,92,131,133; v:44,92,131,133
docs/plans/mission-usage-summary.md [Δ] f:44,52-55; v:44,52-54; H:55; T:55
docs/plans/mv-dependency-aware.md [=] s:7,23,211,330,338,394-395,408
docs/plans/namespace-split.md [Δ] f:29,145,231,293; v:29,293; s:56,108,118,289; H:145
docs/plans/one-compiler-two-entrances.md [=] s:167,193
docs/plans/one-shot-editor-gesture.md [=] a:138,169,186,206,208,277; U:145,159,229
docs/plans/one-shot-form-discovery.md [=] s:57,95,100
docs/plans/outline-corpus-lane.md [Δ] f:27,29; v:27,29
docs/plans/pair-view-evidence.md [=] s:8,25,155,166,258
docs/plans/parallel-landing-gate.md [Δ] f:6,38,91-92,121; a:92; v:6,38,91,121; s:24,29,76
docs/plans/proof-carrying-change-buffer.md [=] s:281; P:257
docs/plans/public-mutation-tool-naming-adversarial-review.md [=] a:299; s:12
docs/plans/recover-and-report-failure.md [=] s:86
docs/plans/representative-edit-portfolio.md [=] s:44
docs/plans/require-change.md [Δ] f:4; v:4
docs/plans/row2-receipt-artifacts.md [Δ] f:12,30,63,78; a:30; v:12,78; s:39; H:63
docs/plans/rows-sublime-4.md [Δ] f:16; v:16
docs/plans/selective-compiled-scalpel.md [=] a:82,240; s:190
docs/plans/sentinel-intent-gate-and-hld.md [Δ] f:40; v:40; s:1,14-15
docs/plans/show-form.md [=] s:44
docs/plans/structural-change-language.md [=] s:9,71,155-156,527,711
docs/plans/structural-lens-query.md [=] s:31,213,258
docs/plans/syntax-first-var-surface-spike.md [=] s:23
docs/plans/three-rounds-roadmap.md [=] s:69,136,160,211,254,259
docs/plans/typed-mcp-change-entrance.md [=] s:13
docs/plans/typed-mcp-inspect-entrance.md [=] s:45,136,139,288
docs/plans/uniform-mcp-elapsed-time.md [=] s:87,90-91
docs/plans/xray-maximality-audit.md [=] s:36,48,57
docs/plans/xray-maximality-handoff.md [=] s:82; U:63,66,314
docs/propogating-updates.md [=] P:8,10
docs/tech-tree.md [Δ] f:201,254,265,278,291,296,305,309,319,372,387-388,401,416,431,455,475,508,521-522,537,556-557,577-578,598-599,623-624,643-644,651,661-662; a:86,118-119,465; v:201,254,265,278,291,305,309,319,372,387-388,401,416,431,455,475,508,521-522,537,556-557,577-578,598-599,623-624,643-644,651,661-662; s:25,94,101,104-105,119,122,125-126,129,132,207,275,301,465,605,655,660; P:129
docs/testing-guidelines.md [=] s:161
docs/tweezer-loop.md [=] a:3,9,91
docs/two-vm-plan.md [Δ] f:48; a:1,3,5,34,48; s:20,32
docs/txn-journal.md [Δ] f:211,232,307,497; a:636; s:199-200,203,596
docs/vision.md [Δ] a:666-667,678; s:77,206,251,316,384,415,428,431,447,530,688,695
docs/why-reading-is-cheap-and-writing-is-expensive.md [=] a:121,292; s:173
```

resources

```text
resources/clj-surgeon-agent-routing.md [Δ] f:32,52,97-98,100,112; v:32,52,97,100,112; H:98
```

skills

```text
skills/clj-surgeon/references/advanced-operations.md [Δ] s:16
skills/fable-overseer/SKILL.md [Δ] f:24; a:3; v:24
skills/gene-report/SKILL.md [Δ] a:42
skills/safe-refactor/SKILL.md [Δ] s:16
skills/study-agent-usage/SKILL.md [Δ] f:118; v:118; T:143
```

src

```text
src/clj_surgeon/alias_migration.clj [Δ] f:903; s:412,1062,1071,1323-1324
src/clj_surgeon/core.clj [Δ] f:2174,2202,2212,2287,2298; v:2174,2202,2212,2287,2298; s:1566,1614,2233
src/clj_surgeon/form_identity.clj [Δ] s:411
src/clj_surgeon/mcp_admit_tool.clj [Δ] s:667,689,692,1575,1580,1590
src/clj_surgeon/mcp_alias_migration.clj [Δ] f:1480,1601; v:1480; s:1533,1545,1563,1594,1692,2006,2057
src/clj_surgeon/mcp_change_buffer.clj [Δ] f:54
src/clj_surgeon/mcp_combinable_transaction.clj [=] f:172
src/clj_surgeon/mcp_extraction.clj [Δ] s:354
src/clj_surgeon/mcp_feature_thread.clj [Δ] f:3116,3177,3347; s:470
src/clj_surgeon/mcp_http_server.clj [Δ] P:29
src/clj_surgeon/mcp_inspect_tool.clj [Δ] f:920; s:1263
src/clj_surgeon/mcp_intent_contract.clj [Δ] s:112,158
src/clj_surgeon/mcp_operation.clj [Δ] f:178-179
src/clj_surgeon/mcp_process.clj [Δ] s:124,135,142,166
src/clj_surgeon/mcp_relation_census.clj [Δ] s:120
src/clj_surgeon/mcp_semantic_client.clj [=] P:13
src/clj_surgeon/mcp_server.clj [Δ] f:226
src/clj_surgeon/mcp_tool.clj [Δ] s:1814
src/clj_surgeon/memory_battery.clj [Δ] f:429,453
src/clj_surgeon/memory_battery_runner.clj [Δ] f:576; H:576
src/clj_surgeon/mission.clj [Δ] f:893-894,922; v:893-894,922
src/clj_surgeon/mission_forms_source.clj [Δ] s:42
src/clj_surgeon/namespace_split_io.clj [Δ] s:434
src/clj_surgeon/parse_admission.clj [Δ] a:5,132,360,439
src/clj_surgeon/receipt_artifacts.clj [Δ] f:10; v:10
src/clj_surgeon/relation_census.clj [Δ] s:204,831,859,901,1830-1831,1960
src/clj_surgeon/spawn_ledger.clj [Δ] f:40
src/clj_surgeon/telemetry_events.clj [Δ] f:7; v:7; T:80
src/clj_surgeon/txn_journal.clj [Δ] f:785,987,995,1330,2603,2667,2736,2769-2770,2788,2794,2799,2807,2857,2861,2933; s:105,261,264,1194,1659,1702,1735,1745,1747,1754-1755,1757,1760,1762-1763,1766,1771,2058,2066,2074,2097,2100
src/clj_surgeon/workspace_onboarding.clj [Δ] P:15-16
```

test-fixtures

```text
test-fixtures/feature-thread/smw-dequote-after/Makefile [Δ] s:8-10
test-fixtures/feature-thread/smw-dequote-after/docs/intent/registry.edn [Δ] s:330
test-fixtures/feature-thread/smw-dequote/DEQUOTE-FORMAT.patch [Δ] U:2,136,212,219,226,237,294,383,389
test-fixtures/feature-thread/smw-dequote/MANIFEST.tsv [Δ] P:12,14
test-fixtures/feature-thread/smw-dequote/Makefile [Δ] s:8-10
test-fixtures/feature-thread/smw-dequote/docs/intent/registry.edn [Δ] s:330
test-fixtures/field-diffs/pre-image/src/marvin_voice_remote/channel.clj [Δ] f:708,728; s:1529
test-fixtures/field-diffs/pre-image/test/marvin_voice_remote/channel_test.clj [Δ] s:604,751,760; P:971,979
test-fixtures/field-diffs/z3-g2-N-2-frozen.diff [Δ] f:189,196
test-fixtures/field-diffs/z7-g2-Z-2-sewing.patch [Δ] H:2,171,366,376,393
test-fixtures/field-diffs/z7-pre-image/src/clj_surgeon/mcp_change_buffer.clj [Δ] f:38
test-fixtures/field-diffs/z8-commit-z8-g1-Z-0-67.patch [Δ] H:2
test-fixtures/mv/mothership_stranded_dep.clj [=] s:1
test-fixtures/relation-census/inventory_folds.clj [Δ] s:6,41,60,62
test-fixtures/require-change/golden/bridge3_new.clj [Δ] s:7,49
test-fixtures/require-change/seed/bridge3_new.clj [Δ] s:7,49
test-fixtures/state.clj [=] s:123,161
```

test

```text
test/clj_surgeon/admit_patch_test.clj [Δ] f:207,2256,4012; a:3370; v:2256; s:1869,2596,3854,3904,3990-3992,3994-3998,4000-4001,4003-4005,4012,7450; H:3628,3881,4012
test/clj_surgeon/agent_routing_test.clj [Δ] s:223
test/clj_surgeon/alias_migration_test.clj [Δ] f:650; s:312,320,801; H:650
test/clj_surgeon/analyzer_contract_test.clj [=] s:154-155
test/clj_surgeon/battery_ledger.clj [Δ] f:115; a:214; H:115
test/clj_surgeon/battery_ledger_test.clj [Δ] f:138; a:29; H:138
test/clj_surgeon/battery_parallel_runner.clj [Δ] a:82,232,258; s:183,200,968
test/clj_surgeon/battery_parallel_test.clj [Δ] s:28,223,546
test/clj_surgeon/cli_dispatch_test.clj [Δ] f:691,702-703,707; v:691; s:392,415,451
test/clj_surgeon/deftest_census.edn [Δ] f:484-485,493,512,818,833; s:212,353,431,604,620,983,1413
test/clj_surgeon/edit_test.clj [=] f:1059,1064,1066,1073
test/clj_surgeon/extract_header_test.clj [=] s:93,111-112
test/clj_surgeon/extract_test.clj [Δ] f:452; v:452
test/clj_surgeon/failure_report_test.clj [Δ] U:15,25,55; P:18
test/clj_surgeon/fast_lane_isolation_test.clj [Δ] f:82; H:82,106-107
test/clj_surgeon/insertion_gap_test.clj [=] s:35,55
test/clj_surgeon/lane_manifest.clj [Δ] f:81; s:207,219; H:81
test/clj_surgeon/lane_manifest_test.clj [Δ] a:330,892; s:19,221,257,269,355,474,486,609
test/clj_surgeon/mcp_alias_migration_test.clj [Δ] f:52,402,3450,3998,4002,7074,7088,7101,7160,7270; v:52,3450,7074,7088,7101,7160,7270; s:1548,2650,2660,3663,3724,3791,4561,5881,5891,5981,6624; H:402
test/clj_surgeon/mcp_change_buffer_test.clj [Δ] a:16
test/clj_surgeon/mcp_combinable_transaction_test.clj [Δ] f:244,275,338,452
test/clj_surgeon/mcp_compact_relations_test.clj [Δ] f:1341,1349,1362,1488,1525,1537,1561; H:1329
test/clj_surgeon/mcp_expect_guard_test.clj [Δ] f:116,274; v:116,274
test/clj_surgeon/mcp_extraction_plan_test.clj [Δ] s:138
test/clj_surgeon/mcp_extraction_test.clj [Δ] f:31-32,34; s:100; H:31-32,34
test/clj_surgeon/mcp_feature_thread_test.clj [Δ] f:391,718; v:391; s:856,1042
test/clj_surgeon/mcp_helper_extraction_test.clj [Δ] f:56,641,1298,1959; v:56,641,1298
test/clj_surgeon/mcp_inspect_tool_test.clj [Δ] f:1368,1376,1822,1999,2003-2007,2056,2058,2067,2069; v:1822; s:1674
test/clj_surgeon/mcp_intent_contract_test.clj [Δ] s:5,37-39,582
test/clj_surgeon/mcp_namespace_split_test.clj [Δ] f:232,376,384; v:232
test/clj_surgeon/mcp_operation_test.clj [Δ] f:156-157,170-171,181-182,194-195,199-200
test/clj_surgeon/mcp_prepared_confirmation_test.clj [Δ] f:162,171
test/clj_surgeon/mcp_process_test.clj [Δ] f:48; a:17; v:48; H:177,179-180
test/clj_surgeon/mcp_read_request_normalization_test.clj [Δ] s:28
test/clj_surgeon/mcp_relation_census_launcher_test.clj [Δ] s:6,61
test/clj_surgeon/mcp_relation_census_round20_test.clj [Δ] s:6
test/clj_surgeon/mcp_relation_census_test.clj [Δ] s:1212,1497,3165,3188,3285,3295,3304,3311,3321,3330,4136,6246,6260,7151
test/clj_surgeon/mcp_semantic_client_test.clj [Δ] P:10-12,19
test/clj_surgeon/mcp_tool_test.clj [Δ] f:2339,2371,2374
test/clj_surgeon/mcp_workspace_test.clj [Δ] f:121; v:121
test/clj_surgeon/memory/journal_child.clj [Δ] s:32,58
test/clj_surgeon/memory_battery_test.clj [Δ] f:543,545,570,587,590,593
test/clj_surgeon/mission_commit_cli_test.clj [Δ] f:54; v:54
test/clj_surgeon/mission_display_test.clj [Δ] f:131,137; v:131,137
test/clj_surgeon/mission_git_boundary_test.clj [Δ] f:14; v:14
test/clj_surgeon/mission_git_identity_test.clj [Δ] f:41; v:41
test/clj_surgeon/mission_git_ledger_test.clj [Δ] f:70; v:70
test/clj_surgeon/mission_git_process_test.clj [Δ] f:14; v:14
test/clj_surgeon/mission_git_test.clj [Δ] f:46
test/clj_surgeon/mission_provider_fallback_events_test.clj [Δ] f:27; v:27
test/clj_surgeon/mission_test.clj [Δ] f:30,71; v:30
test/clj_surgeon/mission_typist_executor_test.clj [Δ] f:55; v:55
test/clj_surgeon/move_dependency_test.clj [=] s:31-32,77-78,100-101,290,302
test/clj_surgeon/namespace_split_test.clj [Δ] f:984,992-993; s:508
test/clj_surgeon/ns_isolation.clj [Δ] a:383; s:681
test/clj_surgeon/ns_isolation_test.clj [Δ] s:145
test/clj_surgeon/outline_memory_test.clj [Δ] a:26
test/clj_surgeon/parser_admission_test.clj [Δ] a:130,208,348,375,412; s:12,128,341,385,424,434,518,523,534
test/clj_surgeon/reader_eval_fence_test.clj [Δ] s:302,463
test/clj_surgeon/receipt_artifacts_boundary_test.clj [Δ] f:37; v:37
test/clj_surgeon/runner_membership.clj [Δ] s:1,10,16,222,228,252
test/clj_surgeon/scope_stream_test.clj [Δ] f:23,30; a:81; H:23,30
test/clj_surgeon/split_proof_gate_test.clj [Δ] f:29,54
test/clj_surgeon/telemetry_events_test.clj [Δ] T:183,356
test/clj_surgeon/tmp_leak_support.clj [Δ] f:17,20,233,286,309; a:3; v:17,20,309; T:21
test/clj_surgeon/tmp_leak_support_test.clj [Δ] f:54,81-82,84,120,150,158,162,168,315; v:54,81-82,84,120,150,315; H:177,240; U:242
test/clj_surgeon/txn_journal_test.clj [Δ] f:25,97,903,936,940,945,1570,1883,2100,2104,2107,2771,2773,2776,2779,2834,2837,3005-3007,3010,3015-3016,3159,3169,3215,3298,3301,3306,3334,3351; s:807,809,823,830-831,848-849; H:25
test/clj_surgeon/worktree_lifecycle_io_test.clj [Δ] U:209
test/clj_surgeon/worktree_lifecycle_prune_test.clj [=] f:283,289,296,302,318,321,328,332,339,343
test/direct_cclsp_client_audit_test.sh [Δ] P:9,21
test/fixtures/show_form_migration.cljc [=] s:14
test/gate_slot.py [Δ] f:236; s:4,28,366
test/oracles/b07_split_adapter.clj [Δ] f:6; v:6
test/oracles/cell_b_oracle.sh [Δ] f:3,8,14; v:3,8,14
test/suite_concurrency_battery.sh [Δ] f:25,29; v:25,29
test/tmp_leak_ratchet_test.sh [Δ] f:134,138,141-144,146,148,157-158,162,457-460; v:457-460; s:7; T:299
```
