# Block B attempt 23 — GO under the corrected rule

The corrected portability rule and the three requested repairs are implemented.
The single fast run, counted battery and real prewarm all passed. No prewarm
repair or retry was needed. This is a branch-only handoff; nothing was pushed.
The complete-test census folds to **99 portable, 10 bb-ineligible, 0 refused**,
with **50 pre-existing initial-load exclusions** separately accounted. The
predicted 100/9 split was not reproduced: after its fixture repair, prune exposed
the same SCI lock-release limitation as recovery. No passing JVM control is
inferred from a passing bb control. No budget, cadence or test membership changed.

## Corrected rule and complete census

TEST-ISO-016 now treats the JVM as the reference and bb as an eligible
accelerator. Both complete controls passing yields portable; a passing JVM plus
a registered bb capability failure yields ineligible and an explicit JVM
assignment; JVM failure or an unregistered bb failure refuses by namespace.
The closed vocabulary covers SCI host interop, native-image reflection, missing
nrepl.core on the bb classpath, and bb-hosted JVM launchers. Missing/wrong-identity
controls cannot earn portability. The manifest prints every ineligible name and
its precise reason, and retains named initial-load exclusions separately.

- [Every namespace, assignment and classification](portability-census.md)
- [Machine-readable fold and all control paths](census.edn)
- [Individual account of attempt22's twelve failing controls](nonportable-findings.md)
- [JVM rule witness](runtime-rule-jvm.log) and [bb rule witness](runtime-rule-bb.log):
  2 tests / 1,028 passing assertions each, zero failures or errors.

The fold uses frozen attempt22 controls plus six fresh controls for merge,
split and prune. Initial-load exclusions have no complete JVM control in that
frozen census; they are not reported as portable or as passing complete tests.
Historical six-sample cost measurements are preserved. Capability admission
overrides their historical assignment where required; no speed claim follows
from these correctness controls or from the single suite observations.

## Repairs and newly exposed limits

The strict CLJC split emitter now emits one ordinary reader conditional per
original body form, preserving each platform's order and source text. The JVM
reader witnesses cover both platforms and either empty side. The shared oracle
normalizes generated anonymous-function symbols, including inside preserved
reader conditionals, while retaining distinct arguments and ordinary symbols.
Prune's two fixture bases now use java.io.tmpdir and keep their finally cleanup.

The original JVM reproduction is [red-jvm.log](red-jvm.log): 49 tests, one
failure and seven errors. The first edited reload had a missing test delimiter;
its diagnostic and repair are recorded in [dogfood.md](dogfood.md).

| Final complete control | JVM | bb | Evidence |
|---|---|---|---|
| cljc.merge-test | 14 tests / 26 assertions, pass | Same, pass | controls/clj-surgeon.cljc.merge-test-{jvm,bb}-test.control.edn |
| cljc.split-test | 11 tests / 35 assertions, pass | Same, pass | controls/clj-surgeon.cljc.split-test-{jvm,bb}-test.control.edn |
| worktree-lifecycle-prune-test | 24 tests / 82 assertions, pass | 24 tests / 82 assertions, 20 failures | controls/clj-surgeon.worktree-lifecycle-prune-test-{jvm,bb}-test.control.edn |

The TMPDIR fix exposed an exact Git-version gate: only Apple Git 2.50.1 was
registered. This host runs [Git 2.53.0](git-version.log). Both fresh pre-admission
controls passed the real compatibility matrix (absent-target removal, refusal
for reappeared file/directory/dangling symlink/locked target, and peer
preservation), but had 19 apply/recovery assertion failures on
unsupported-git-version. Those controls are frozen under
[prune-before-git-admission](prune-before-git-admission/).

The existing WTL-PRUNE-006 admission rule permits a version once that matrix is
witnessed. The product registry now includes the exact observed version; neither
the version check nor any test was bypassed. The fresh JVM control then passed.
The bb control now reaches SCI's refusal of FileLockImpl.release and its retained
lock causes replay failures. [The actual helper probe](recovery-lock-probe.log)
establishes that mechanism for both recovery and prune. Thus all nine original
bb failures are ineligible, and prune is a tenth. This is the evidenced reason
for 99/10/0 instead of the predicted 100/9/0.

## Requested gates

| Entrance, once each | Result | Evidence |
|---|---|---|
| make test-fast | PASS: 1,244 tests / 12,560 assertions; 47,382 ms against 60,000; zero isolation violations | checks/fast.log, fast-total.edn, fast-run/ |
| make test-battery | PASS: 1,166 tests / 18,472 assertions; zero failures/errors/isolation violations/skips; 1,090,847 ms serial-equivalent against 1,800,000 | checks/battery.log, battery-total.edn, battery-run/ |
| make landing-gate-prewarm | PASS, all stages exit zero, problems empty; 398,680 ms coordinator / 401 s wrapper | gate.md, prewarm.edn, prewarm-run/ |

The battery's counted ledger row names af4e0aa2, takes 166 seconds, and was
committed with generated namespace packing estimates in 61719983. The wrapper
observed 168 seconds; coordinator makespan was 163,586 ms. These distinct
measurements are retained rather than substituted for one another.

Prewarm names source tip 61719983 and reports fast 44,879 ms against 60,000,
integration 69,205 ms against 240,000, and the bb suite's actual bb work
241,400 ms against 343,102. The post-pool bb diagnostic also passed and took
225,230 ms; it is a required stage of this one gate, not a second prewarm.
[gate.md](gate.md) is byte-identical to checks/prewarm.log, including every
refusal-census and budget line, with its digest in [gate.sha256](gate.sha256).
The one-repair/final-retry allowance was unused. The repaired source is unchanged
between the tested tip and the final evidence commit.

## Commits, dogfood and remaining review

- a269922c — corrected rule, capability registry, assignments and witness.
- b8a9f60b — JVM-readable emitter, oracle and TMPDIR fixture repairs.
- 70b5db11 — witnessed Git 2.53.0 admission and narrower generated-symbol oracle.
- af4e0aa2 — prune's newly exposed ineligibility and historical cost distinction.
- 61719983 — passing counted battery ledger and generated namespace walls.
- The commit containing this report is the final attempt23 evidence commit.

[Dogfood ledger](dogfood.md) accounts for every source edit and the one syntax
repair. [Commands](commands.md), [source patch](source.patch), formatter logs and
[zero-warning/error lint](lint-final.log) retain the implementation evidence.
All source edits used the binding brief's native exact-form patch route; no
Surgeon performance or adoption claim is made. No push, tag, merge, shared-server
restart or shared install modification was performed.

Fable's independent review remains external, particularly the additional
Git-version admission and the changed census split. The 50 initial-load
exclusions remain bounded by their configured-classpath evidence. This attempt
does not claim a fresh full 159-namespace census, a speed improvement, or merge
authorization. The reference and bb fresh controls and the final gates carry
their actual source subjects in their receipts.
