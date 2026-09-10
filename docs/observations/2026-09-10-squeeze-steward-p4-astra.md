**P4 ruling: DO-NOT-INSTALL `/var/tmp/forge/ship-v3.8-delivery/`.**

The selected installed-epoch composition is correct, the destination override is closed, and the helper has useful new refusal/retry witnesses. The delivery still violates the prior publication contract: `verb-sentinel` swallows publication failure, the adapters do not share checkout exclusion with their publisher, and root/remote/subject validation remains incomplete. P7 and P8 are explicitly unfinished prerequisites from the previous ruling. A steward review cannot convert those pending checks into permission to install. This is not INSTALL-WITH-FIX: a two-line sentinel correction alone would not qualify the composition.

Written 2026-09-10T07:54:48.108214+00:00. Review began 2026-09-10 07:46:44 UTC; deadline 08:16:44 UTC. Steward: Astra, this Codex session, separate from the builder but sharing its account and artifact access. This is independent scrutiny with shared-custody limits, not independent v1 observation or fleet qualification. Authority and acceptance remain those in the [prior ruling](/var/tmp/forge/plan2/cellC/astra-squeeze-steward-report.md), [CHANGE-RULE.md](/home/forge/src/claude-skills-nrepl/tighten-the-loop/CHANGE-RULE.md), and [SEAT-RECEIPT.md](/home/forge/src/claude-skills-nrepl/tighten-the-loop/SEAT-RECEIPT.md). I read the entire Delivery round, including its pending obligations and terminal receipt.

**The principal blocker is executable terminal logic, not missing paperwork.**

[verb-sentinel](/var/tmp/forge/ship-v3.8-delivery/verb-sentinel:159) assigns `PUBRC=$?` in `run_split`; line 214 does likewise in `run_alias`. Neither function reads it afterward. Lines 169–170 and 233–234 are identical:

```sh
[ "$VERDICT" = OK ] || return 1
return 0
```

Consequently an otherwise green run with `publish()` returning 3 still returns 0, and the dispatcher at lines 237–244 exits 0. The terminal can contain `verdict=OK published=committed+PUBLISH-FAILED(...)`. The builder report, bundle manifest, and commit message say publication failure produces a nonzero terminal outcome; these bytes contradict that claim.

I executed these exact terminal fragments, read into memory from the hashed delivery file, inside functions with controlled scalar variables. No adapter, publisher, worker, Git mutation or external message was executed:

| Fragment | Inputs | Actual exit | Required |
|---|---|---:|---|
| `run_split` terminal | `VERDICT=OK PUBRC=3` | **0** | nonzero |
| `run_alias` terminal | `VERDICT=OK PUBRC=3` | **0** | nonzero |
| Both terminals, positive control | `VERDICT=OK PUBRC=0` | 0 | zero |
| Both terminals, failed-oracle control | `VERDICT=SUSPEND PUBRC=0` | 1 | nonzero |

These are bounded control-flow witnesses, not substitutes for the real-entrance P7 cases. Repair both terminal paths, retain the worker verdict independently of publication, and test the dispatcher as well as `publish()`. Its line 106 also returns 0 solely from an `OK` first line, ignoring the captured helper exit; all adapters should require both successful exit and valid terminal evidence.

The canary's ordinary publication-failure path is improved: a PASS grade plus `PUBLISH-FAILED` now exits 3. But [canary-cell](/var/tmp/forge/ship-v3.8-delivery/canary-cell:125) can set `PUBRC=4` on add failure, then replace `PUBLISHED=ADD-FAILED` with `PUBLISHED=committed` if the subsequent commit succeeds. Publication is skipped because `PUBRC` remains 4, while the terminal only checks `PUBLISHED`, allowing exit 0. Also, lines 162–164 do not check child `RC`: a PASS-looking child receipt plus `RC=17` and successful publication exits 0. Executing those terminal fragments with the stated inputs confirmed both exits. These are conditional source-level counterexamples, not claims that such live jobs ran here. The latter behavior is inherited, but the already-required exit-17 qualification must catch it rather than bless it by omission. Sentinel's split arm also does not retain the `round` exit at line 142 before deriving its oracle verdict.

**Publication is not yet a bound transaction.**

1. **First mutation precedes branch validation.** The lock is correctly acquired first, but [records-push line 76](/var/tmp/forge/ship-v3.8-delivery/records-push:76) runs `git pull --rebase origin "$DEST"` before the first `validate`. A wrong local branch can therefore be rebased or fast-forwarded before exit 6, or return exit 3 on a conflict instead. Row 06 checks the remote code refs, not the wrong local branch tip or worktree. Check branch/root/remote under the lock before pull/rebase, and retain post-rebase validation. A known rebase conflict may remain for recovery; accurately distinguish that state from a pre-mutation refusal.
2. **Root spelling and marker existence do not bind repository/origin identity.** Lines 48–50 accept the canonical path string or *any* root containing a regular `.records-fixture-origin` file. No marker content, recorder manifest, effective Git root or fetch/push URL is validated. In the successful retained PAIR9-new repository, that marker is even tracked in the pushed tree, because the recorder created it before `git add -A`. It is a fixture convention, not evidence of recorder ownership. Nothing checks an alternate `remote.origin.pushurl`, multiple push URLs, or an origin pointing at another repository. Bind effective production root and fetch/push repository identity before mutation; fixture authorization must name the recorder's actual root/origin without creating a general production escape. Current production configuration was correct at my read, which does not make the guard enforce it.
3. **The artifact check is a finite end-tree denylist.** Lines 36 and 68 cover `src test bin dev deps.edn bb.edn Makefile`, but do not bind the intended record paths/blobs or pending commit set. Unrelated paths outside that list remain eligible; an intermediate code change reverted in a later commit has no final diff yet its commit history can ride the push. This does not meet the prior promise to prevent unrelated candidate commits being carried. Keep the legitimate pending-publication interpretation, rather than comparing historical records ancestry to current code trunk, and validate the declared records subject against its resolved publication base.
4. **Adapter writes remain outside the helper's lock.** Seat-receipt writes its EDN at line 164, fetches/counts at 224–225, and stages/commits at 228–236 before calling the helper. Canary appends/stages/commits at 115–133; sentinel appends in each arm and stages/commits in `publish()` before the helper call. None acquires or hands off the helper's checkout lock. Another adapter can append or commit during a publisher's rebase/validation. An immutable `TIP` prevents a later HEAD change from changing that push argument; it does not prove that this adapter's intended blob was included. Bind the named record blob and use one cooperating transaction owner or a tested lock handoff covering append/stage/commit/refresh/rebase/push/readback. The prior ruling explicitly required this; repository-wide P5 expansion is still outside scope.
5. **Complete failure evidence is not retained through every path.** The helper stores only the first 160 flattened characters of a rejection, not its raw output. If a later `validate` exits 6/8/12, it does not call `tail_attempts`, losing earlier rejection history from the terminal. Canary and sentinel retain only the helper's first line in their published summary and do not persist full `RPOUT`; seat-receipt does log it. Retain the complete attempt evidence outside the compact summary. Seat-receipt's fresh-base fetch exit at 224 is also ignored, so its `CARRIED` count can still use stale evidence.

**Claim-by-claim findings over the retained bytes and receipts.**

| Builder claim | P4 finding |
|---|---|
| 15/15 carried files equal installed, only helper differs | Correct as the starting-copy description. The **final** 16-target set has **11 unchanged targets and five changed targets**: records-push, seat-receipt, canary-cell, verb-sentinel, tighten. All 16 equal retained scratch `deliv-dest`. The three Clojure helpers and the parked envelope/consumer fixture set match the installed epoch. Relative to `min-src`, the shared fixture changes are only the records matrix and run-ship-v3; four adapters are newly included. |
| Override removed; APPROVED constant | Confirmed: executable assignment is literal `APPROVED=records/MCP-main`; the environment variable no longer supplies it. Remaining mentions are historical comments/tests. |
| Three previous probes now exit 7 | **Executed on the actual delivery helper**, with `RECORDS_REPO=/dev/null`: `DEST=MCP/main`; `DEST=MCP/main APPROVED_OVERRIDE=MCP/main`; `DEST=main APPROVED_OVERRIDE=main` all exit 7 with `forbidden-destination approved=records/MCP-main`. Here `DEST` and `APPROVED_OVERRIDE` abbreviate the actual variables `RECORDS_DEST` and `RECORDS_APPROVED_DEST`. No checkout or lock was reached. |
| Row 12: six spellings, no lock | Confirmed in source, lines 224–235: six environment combinations, literal exit 7, unchanged remote code refs, absent helper lock. The unrelated-ref case does not combine both overrides, unlike the earlier requested matrix; the constant comparison nevertheless rejects it. |
| Root guard, exit 11 | Actual `/dev/null` probe with default destination exits 11; row 13 asserts exit 11 and no publication. The marker/origin limitations above prevent calling this complete root/remote binding. |
| Branch/subject checked under lock and after rebase | Confirmed **after** each successful rebase; missing pre-rebase check remains a blocker. |
| Failed fetch/diff exit 12 | `validate`'s fetch/diff failures explicitly exit 12. An initial `pull` fetch failure is instead classified `rebase-failed`, exit 3. No matrix case injects the exit-12 paths. Fail-closed improvement accepted; blanket exit-12 coverage not established. |
| Non-src artifact guard | Listed seven paths are present. Row 17 exercises four: Makefile, deps.edn, test/x_test.clj, bin/thing. Row 08 covers src. dev and bb.edn are source-covered, not individually exercised there; intended-subject limitations remain. |
| Validated object ID pushed, never HEAD/force | Confirmed line 83 pushes `$TIP:refs/heads/$DEST`, without force; TIP is resolved after validation and again after retry validation. The `+` in fetch updates a local tracking ref, not a forced remote push. |
| Row 14 genuinely retries | Confirmed mechanism and assertions: pre-receive creates an interloper commit, moves the records ref, declines first push; row requires exit 0, `attempts=2`, a retained rejection, record presence and interloper ancestry. Retained graph is `7ecad6f records: r14` over `814d584 interloper` over base. **Interloper has the same tree as its parent**, so this is not the claimed two-distinct-record-blob retry witness. Row 05 has two files but moves the remote before the helper starts. Preserve both witnesses and add the missing combined case. |
| Rows 15/16 and literal refusal exits | Source asserts exit 5 with three rejection summaries and absent publication; row 16 asserts exit 9 on a held lock. Rows 06–10 now explicitly assert 6/7/8/3/10. Accepted as implemented assertions, with the 19-row green result supported by the retained scratch-install summary; I did not rerun mutating fixtures. |
| P1 adapters route through helper; unchanged no longer skips | Calls and unchanged fall-through are present in all three. Full P1 closure is **rejected**, for terminal/transaction defects above and pending P7 behavior. `--no-push` is explicitly labelled unpublished; complete no-push/dry-run behavior remains part of P7. |
| Row 18 source scan, red first, minimum eight scripts | I ran the exact read-only scan body on delivery plus current bundle: **21 scripts scanned, no hits**. I applied its same filter to the three parent-commit adapter blobs: all three old direct code-ref pushes were found at 237/134/94. This independently corroborates the static old-red/current-green distinction; historical red-first ordering is supported by the builder report, not a separately sealed observation. The minimum of eight is present but aggregate: one missing scan root can still leave eight. It cannot prove required member coverage or runtime failure handling. |
| Bundle 731723d pushed to fork | All five changed bundle files and their committed blobs at `731723d7755546dba615a5465718f8bfbc422a32` equal delivery. Local branch and fork tracking ref name that commit; tracking reflog records `update by push` at 07:23:19Z. This supports the reported push without a new network operation; it is not a live remote-ref readback. Manifest rows match bytes, while its claim about nonzero sentinel outcomes is false. |
| INSTALL OK v3.9, 235 rows, scratch DEST | Confirmed [acceptance9.log](/var/tmp/forge/squeeze/out/acceptance9.log): 12 set summaries sum to **235**, all `mismatches: 0`, installer stamp 07:37:05Z, `ACCEPTANCE9 EXIT=0` at 07:43:58Z. All retained installed targets and copied delivery fixtures match delivery now. This is real scratch composition evidence, **not adapter behavioral qualification**. |

The installer adds the four adapter targets and correctly defaults SRC to the delivery tree. Its regression loop contains no adapter behavior suite; adapters themselves hardcode `/home/forge/bin/records-push`, so a scratch target directory is not by itself proof of their final dependency resolution. Four required regression scripts are inherited from the canonical fixture directory rather than shipped in this tree: run-land-auto, run-land-publication-truth, run-ship-v2 and run-ship-v3.1. They exist and match the retained scratch copies. The installer skips an absent suite and grades the final output string rather than enforcing each suite's exit status (lines 146–152). Therefore retain the explicit suite inventory, per-suite exits and counts at the operator gate; an `INSTALL OK` banner alone is insufficient.

**Timing: useful corroboration, still not final qualification.**

The raw old record reports 660.340 s; the successful treatment record reports 0.150 s, hence **660.190 s removed from this fixture publication**. Treatment helper terminal is 0.110 s, independent readback tail 0.040 s; lease alive at readback is `yes`. Both successful records have exit 0 and confirmed visibility. I checked the retained bare origins: code refs equal the recorded IDs, and the named record blob on each records ref is `75935190bb2d239adde940d4b00d79a8972984a5`. The final helper equals its v3.9 frozen copy.

The recorder now reads `/proc/uptime`, stamps its final endpoint after the code-ref comparison, and no longer kills the holder. Its numerical formatting does not confer millisecond clock resolution. More significantly, [harness9.sh line 118](/var/tmp/forge/squeeze/harness9.sh:118) backgrounds a subshell `wait` for a PID belonging to the parent shell; it does not produce an eventual release receipt. It writes `holder_not_killed_by_recorder=true`, not observed full lifetime. Retain actual holder completion and lifetime in the final recorder.

[The pair log](/var/tmp/forge/squeeze/out/pair9.log) ends with the **first failed treatment**, exit 11 after 130.310 s of unsuccessful readback. The later successful treatment is in `PAIR9-new.record.txt` at 07:36:50Z. Thus one successful rerun exists, with one failed treatment attempt retained in the aggregate log; the pair log alone is not the successful pair. The same label reused the fixture directory and overwrote the per-attempt helper/record files. Preserve unique attempt identities, raw artifacts and recorder hashes in the next qualification. The old and repaired-recorder executions also do not have separately bound harness hashes in their arm records.

All three initial refs still share one base object in each fixture; divergent histories and concurrent code-ref advance remain unwitnessed. Six fresh old controls, three counterbalanced pairs and a registered idle pair over the final frozen composition remain **P8 pending**, as the builder correctly states. Do not silently substitute this one successful rerun or the old epoch's controls. Field transfer remains untested; ship acceleration remains zero claimed and zero measured. No new build-hour payoff is certified by this incomplete delivery.

**What must happen before an operator may install.**

The builder should repair the bounded publication/adapter defects above in an isolated successor of this composition, preserve this candidate and all failed evidence, and refresh the bundle, manifest, binding text and applicable fixtures together. Complete P7 at the actual three entrances with recorder-owned private roots/remotes and controlled children: positive named-blob visibility under a live code lease; forbidden destination/override, wrong root/branch/remote, unrelated artifact, dirty/conflicting state, rejection, timeout and missing readback; exit 17, assertion-red, stale candidate, pending proof and missing observer; unchanged retry, no-push/dry-run, and concurrent adapter writers. Preserve old outcomes rather than retroactively marking them green. Complete the already-required final P8 protocol, then obtain a new steward ruling naming those exact bytes. Relabelling these obligations “P7/P8” does not move them after installation.

At that later approved handoff, the operator must check all of the following:

- **Fresh qualification and identity:** final approved source/installer/targets/fixtures/bundle hashes, effective dependency resolutions and intended modes. Recheck every file that can be replaced against its frozen installed baseline, including all canonical fixture dependencies; the seven original hashes alone are insufficient. Do not copy an unrelated staged v3.7/v3.8 consumer epoch or unqualified bundle-ahead bytes.
- **Real quiet window:** actual `/var/tmp/forge/ship/.lease` free, no affected ship/land/publisher/adapter processes, and owner-controlled exclusion of new affected jobs through replacement and validation. My read at **2026-09-10T07:50:11Z** found an empty real lease directory; that is not an install reservation. The installer's one-time process/lease checks and per-file rename do not create an atomic multi-file rollout.
- **Publication binding and worktree state:** canonical records worktree, `records/MCP-main` source branch, approved effective fetch and every push destination, approved pending record subjects, no unresolved rebase/conflict or unowned dirty/staged work, and compatible cooperating checkout ownership. At my read the real root/branch and single effective fetch/push URL matched `/home/forge/src/clj-surgeon-records`, `records/MCP-main`, and `https://github.com/realgenekim/clj-surgeon.git`; recheck at handoff. No public-main mutation or code-trunk records publication.
- **Explicit installer inputs:** the later qualified SRC, `DEST=/home/forge/bin`, `FIXTURES=/var/tmp/forge/tighten/fixtures`, and `SHIP_LEASE=/var/tmp/forge/ship/.lease`. This report supplies no GO command for today's tree. Keep complete backups and a rollback manifest for targets and canonical fixtures, including executable modes (`sol-yolo` 0700).
- **Proof against actual installed bytes:** verify all required suites were present and completed with literal successful exits, expected counts and no mismatches; run the qualified adapter/dependency/binding checks through real command resolution and confirm installed/bundle parity. Keep affected jobs closed if replacement partly succeeds or post-install proof fails; `INSTALL NOT PROVEN` can occur after writes. Restoration must be owner-controlled and verified.
- **Scope after install:** discharge the stop only through the authorized owner and qualification receipt. P3 is one later already-useful publication during an independently occurring real ship, not a manufactured ship. P5 remains bounded to the declared cooperating checkout; P6 remains parked. Configuration-only/self-issued v0 evidence does not become v1 observed participation or fleet uptake.

P1 is **partially implemented, not closed**; P2's packaging/push claim is supported locally, but its contents are not install-qualified; this P4 is a refusal with concrete findings; P7/P8 remain prerequisites; P9 remains bundle-ahead-of-installed. No repair in this report reopens the parked consumer authority.

**Audit boundary and hash binding.**

I executed four pre-checkout helper refusal probes, Bash syntax checks (23 shell files, all exit 0), the read-only row-18 source scan and old-blob filter, and scalar-only terminal fragments. I read local Git objects/configuration with `GIT_OPTIONAL_LOCKS=0`, compared hashes, inspected retained fixtures/receipts and checked the real lease directory without modifying it. I did not run a full fixture suite, adapter job, installer, publisher reaching a checkout, ship, analyzer, JVM, network operation, commit, push, checkout, message/Andon tool, or lock/lease mutation. The only authored file is this requested report. The scoped refusal is communicated to the requesting operator here; no external alarm delivery or acknowledgment is claimed.

The following manifest binds the bytes reviewed. It is an audit snapshot, not an authorization to install them. Target names are relative to `/var/tmp/forge/ship-v3.8-delivery/`; fixture entries are part of the installer source set.

```text
e95ea5e60d130c561ed9db43924685a1bc5ca938a9d711377f073390984d9f35  ship
647a734413567a86b4821a2595947624da67159b38342ed4e42346796c41ad9e  land
cc09e618af202f4b33d662373fc6b714fbb854417fe061f43ebf6d1d1f267e98  fence-run
fedf998c2bb635cdd59fbc55b7d0350e2f5f8a01e27dd600776784df93e11e3d  receipt-chain
edcac9bdc9725ba9b31132032504eb78641c93e0f80c02c2e40221e6bf9d7b9e  land-auto
72c8e4f13d6e8ee67e78a48281bb5f90c48434b9f657faba6790dd5cafd54e9a  records-push
3c13238191dbcbc47c396c22718d892bb2595c6e68065e6fdde9177d1d311093  seat-receipt
e98bfaf473a83a4fe7bab358837af9d782907840b3945ee8cbd84f39f205151b  canary-cell
12220e9029056a2af729e2b672961ff500417cdb30092619189dccfcfaa6fd32  verb-sentinel
5099e429e970c4ce8a1e34287e5fdc05fbfd0a79ea2f1dd5f0b23dba4efddbfe  tighten
864632b6bee6ebbbd6e52d85d86797e40398ff6043ca872c8859dd5fc67a720a  ship-fix-block-spec.md
45c3d1dd5ed5f12037948d1e316ad993a62122b6da6ac44f9b0e6447297a802d  run-bg
44401a573e08d4f3540d5760d3ff92857532d2f6bad1caa6fab84d2ad719ef48  sol-yolo
034b5afed6023b29da55ee8354b2c9448ca8d507dba5de17c057ba9ffb2765c3  gate-envelope.clj
6448222daab675d95e75eaa1cee24d73c9805ad342fbfdfc2d16ba624ccddfbe  gate-consume.clj
cb28ac1a416bdee456f8ab3e56cbd98d4fa299f6031624af1719150db262e915  mutate.clj
e76bc8e86f978ee6ee2d659ff1d33c3362ad35c96a900e6bdaee26850f845b18  install.sh
d1cf6f3cd5aae71c0086da3de23c03d471f2b3efac4eea05b99c3d16525a61e7  fixtures/envelope-green.edn
8b8e0fa6600ba447a77a9de28b870f79549fd4836fc25077c870c0541205109a  fixtures/gate-vars-alias-migration-test.edn
97478a3fe892290519a106e908b4b320ffd3331d65fc8c8b7dc52f99b8645db7  fixtures/gate-vars-mcp-test.edn
535d274fa4d6984a67a4ae00871c0f8a46f160d4b9dc63ae4bf7fe9c7c3bc936  fixtures/gate-vars-test-bb.edn
044c446daa9ba2168c00ef8055e2292321e08260e963fe8e7f2bd04b494b8fd2  fixtures/gen-field-mutants.clj
afafd75d53dd898710230b160e9cda584c1b595dc02df77254b3c4e3eb5721cd  fixtures/inventory-3ea3803e.edn
55e25d68b3300df8e0931b99d7dd7d992ee122c5df1c825f91d81075933d4907  fixtures/records-push-v3.7-frozen.sh
be07c23c5cf5d055f7d94878fe70e76cb719e0c7b316e14758f5d5f4ef3dcebb  fixtures/run-records-push-v3.8.sh
b6fdc98f093e74190d8d992917aa4b323d91a1b1906fd395e8adc7f6ea3ac010  fixtures/run-ship-v3.2.sh
b0c428af48d9c6b2ce4986dd29a3b2753db287455cfa46d217f2d8966620c1df  fixtures/run-ship-v3.3.sh
3b85d733c08d0ed87946ba45baa8c542c14bd8c9578a96f99879e280ae2672e1  fixtures/run-ship-v3.4.sh
d771ac7a89048a248c95e4ee19f970d15f287c506ebb942c012be79a689fe66c  fixtures/run-ship-v3.5.sh
3d2b972a7ac754c61d1a6f1145907aa4681a030cfa7706526f54c1962481c383  fixtures/run-ship-v3.6.sh
dc58d565d86cd930ec8a33f436c9a08c668680667678bb50a36fe26289245fc3  fixtures/run-ship-v3.7.sh
35535beb314ee0877019277a5db641a658987c59b0f1906cea8a80af020d9594  fixtures/run-ship-v3.sh
bface800a969df10b13c407888d07d37402860dbbc23832efb7e63fb116ca469  fixtures/stub-gate.clj
78986f6c59f002308928623f9feb43f908294df6e41f92394fe953f2ba95ccbf  fixtures/toolchain-observed.edn
```

For the eleven unchanged targets, the hashes above are also the installed baseline. The five changed targets still had these old installed hashes at this audit:

```text
55e25d68b3300df8e0931b99d7dd7d992ee122c5df1c825f91d81075933d4907  /home/forge/bin/records-push
2a9694eec4fd6dbc056744d40d3e26605f9b1cf94a5fd1a93f222da664076128  /home/forge/bin/seat-receipt
b7bc9c6bddf3589e903d2010e10415808fb028bf2a8985260c7d478924675728  /home/forge/bin/canary-cell
7955d1fdc1f3c49d7e81e496a81b317cc69ebb6989cbcb7395377cd6c83cdae6  /home/forge/bin/verb-sentinel
ceac37f31552eff0929454a8939f1dca6b5e6dbd295d7bd0c4ef2b1b49625321  /home/forge/bin/tighten
```

Inherited required fixture dependencies (canonical and scratch copies matched):

```text
b37b18bafe0afc4268db20b11f49eb39f671a6f5374d332aa3efbb21953ec1da  /var/tmp/forge/tighten/fixtures/run-land-auto.sh
fe7fc0cede857c373b0d69c563e234a61742b9cbd97766471103d2c8d307338c  /var/tmp/forge/tighten/fixtures/run-land-publication-truth.sh
4669be68690576eaac985420cde1db9c734ccae9fb0e4e8a2987c0b5b6ab8435  /var/tmp/forge/tighten/fixtures/run-ship-v2.sh
1eb9bd803e84f11e8736d3eec8a6ebf43462defc0aa2f4ae98b98c9b1c2018a9  /var/tmp/forge/tighten/fixtures/run-ship-v3.1.sh
```

Retained evidence hashes:

```text
8898ca21bd054f155d80e16e54e40a38b83676896fd158b9f5328f3b17f431e0  /var/tmp/forge/plan2/cellC/opus-squeeze-report.md
7c1538a679fd660aac4746775b8ca5b6343d051cf18915ac5cd7664174206c67  /var/tmp/forge/plan2/cellC/astra-squeeze-steward-report.md
4f8021267a8f35ae7a4ecf356467225bf1461e77b0144cdad28d11b2d7fbe1ac  /var/tmp/forge/squeeze/harness9.sh
9e6a2824573423f2368860b2fcc9896497332cc170d8057ede16566bc5649f3f  /var/tmp/forge/squeeze/out/acceptance9.log
fd33b3343bafcab0079e4bfa34be20577e95acc0654fc12fa6db6109d604f799  /var/tmp/forge/squeeze/out/pair9.log
837379489834bf9b018edd38443f396347480f02c6b30d70b68bb0d0bd808c20  /var/tmp/forge/squeeze/out/PAIR9-old.record.txt
f7eb8d7df992a7cb704d233d2731cd868969d5b355d9221604f0e6d5970f7702  /var/tmp/forge/squeeze/out/PAIR9-new.record.txt
b37f48686726d598996e850f356dc34fc70ec3c49b434fa30b71c4f19fedce3b  /var/tmp/forge/squeeze/out/PAIR9-new.helper.txt
f1faf9815116096f3dadaf05eef08613cdbd966bc09aa96cb73015fa58de01ec  /home/forge/src/claude-skills-nrepl/tighten-the-loop/bin/MANIFEST.txt
```
