# Moving the consumers to kaocha-sublime b1a838a

*forge-anvil, 2026-09-08. Astra's item 4b. Two-and-a-half-hour timebox; the work below
took about 90 minutes of wall from first read to pushed branches.*

Item 4 built the two specimen entrances (`make test-probe`, `make test-gate`) and taught
`coldstart-grade` to accept their records
(`docs/observations/2026-09-08-agent-grammar-adapters.md`). It also reported three
defects it had to work around. Astra fixed six defects in `kaocha-sublime`
(`c155a65d`) plus the parent-outcome defect (`b1a838a`); this item moves the consumers
onto that pin, deletes the workarounds, and proves each fix at the entrance rather than
in the library's own tests.

**The one-line result:** curtaincall-cfp's declared cold gate can now report
`outcome:"pass"`. Before the fix it could not, ever, on any tree — and item 4's grader
carries two narrowings written specifically to stop that impossibility from convicting a
compliant agent.

## What shipped

| Repo | Branch | Commit | On |
|---|---|---|---|
| marvin-voice-remote | `nrepl/test-alias` | `56a4984` | `76e878f` |
| curtaincall-cfp | `nrepl/test-alias` | `f821c3bb` | `4b40b8a9` |
| `~/bin/coldstart-grade` | not a repo | 3870 → 4000 lines, sha256 `430723cf…` (was `9fbdcb6b…`) | — |
| `/var/tmp/forge/coldstart/fixtures/agentv1-run-done.sh` | not a repo | 34 → 45 rows | — |

Both specimens pin `io.github.marvin-openclaw777/kaocha-sublime` to
`b1a838a282696a3f8e218de2bdcb368c48bcd5df` — both aliases in marvin-voice-remote
(`:run-tests` and `:nrepl`), the one shared `:run-tests` pin in curtaincall-cfp, whose
`make nrepl` composes `:run-tests:test:nrepl`.

## The probe adapter: the image is no longer driven by hand

`bin/test-probe` used to reload the named namespaces with `require :reload`, reset
`kaocha-sublime.image/active` to `nil`, and run through `kaocha.repl/run`. That whole
sequence is replaced by

```clojure
(kaocha-sublime.probe/run targets {:suite :unit})
```

with `targets` a vector of namespace or namespace/Var symbols, source first. `:events`
is relayed verbatim, `:path` names the completed receipt, `:error` is the diagnostic,
and `:exit` is the run's own verdict. `kaocha.plugin.run-receipt` is required BEFORE the
first agent-mode run, because the plugin installs the guard that owns the lease's
`finally` and refuses if it is first loaded from inside an already-entered
`kaocha.api/run`.

Two defects die with the hand-rolled sequence, and both were witnessed at the entrance.

**A deleted Var kept running.** `require :reload` re-evaluates a file but never unmaps
what the file no longer defines, so a deleted `deftest` stayed interned and kept
executing — a probe reporting a green for a test that no longer exists. Add a Var, probe,
delete it, probe:

| | marvin-voice-remote | curtaincall-cfp |
|---|---|---|
| with the doomed Var | `"tests":2` | `"tests":2` |
| after deleting it | `"tests":1` | `"tests":1` |

**A load error wedged the image.** A mistyped namespace ended the run through Kaocha's
`:kaocha/early-exit` path, which never reached `finish!`, so the lease stayed held and
the NEXT probe in that JVM died with `OverlappingFileLockException` until the image was
restarted. The script's "restart the image (make nrepl)" advice was a bandage over that
leak. Now it is an ordinary typed refusal, and the next probe on the same image passes —
which is the whole point, so both halves are quoted below.

## The gate adapter: one address for every outcome

The heap refusal used to publish under an id of the plugin's own choosing
(`refused-<uuid>`), so a launcher that had minted its own `KAOCHA_RUN_ID` could only find
it by listing the refusal streams that had not existed before the invocation — a
directory diff, which is exactly the reasoning the receipt discipline forbids everywhere
else. `b1a838a` honours `KAOCHA_RUN_ID` for the refusal stream, so
`target/kaocha-runs/<launcher id>.events` addresses a completed run and a refusal alike.
The fallback is deleted.

The same edit adds the rule the report asks for, on both entrances: **a refusal, or a
missing DONE, is never success merely because the upstream process returned zero.** Each
entrance now reads its own owned stream before letting an exit 0 stand.

## The records, verbatim

Every line below is the runner's own bytes off the specimen's `make` entrance stdout,
unedited.

The probe, red then green (marvin-voice-remote, `make test-probe`; entrance exit 1 then
0, receipt digest and `:totals` checked against each record):

```text
FAIL {"v":1,"candidate":"86b49a09833881e10db4130e5bd2f00dd70d4417c70744d0bf8d527338eb2c89","file":"test/marvin_voice_remote/item4b_witness_test.clj","mode":"probe","unavailable":[],"event":1,"line":7,"expr":"(= {:invoice {:total 42}} {:invoice {:total 43}})","id":"2026-09-08T21-10-37-570140246Z-22f6d4e9-570c-4b6e-9d48-2cb5c5df1298","kind":"fail","expected":"{:invoice {:total 42}}","truncated":[],"context":"invoice total","actual":"{:invoice {:total 43}}","test":"marvin-voice-remote.item4b-witness-test/item4b-red-green-witness","diff":"[{:path [:invoice :total], :expected 42, :actual 43}]"}
RUN-DONE {"receipt":"/var/tmp/forge/item4b-fx/mvr/target/kaocha-runs/2026-09-08T21-10-37-570140246Z-22f6d4e9-570c-4b6e-9d48-2cb5c5df1298.edn","omitted":0,"v":1,"parent":null,"candidate":"86b49a09833881e10db4130e5bd2f00dd70d4417c70744d0bf8d527338eb2c89","tests":1,"pending":0,"auto_followup":false,"wall_ms":98,"mode":"probe","coverage":"complete","outcome":"fail","shown":1,"skipped":579,"fail":1,"id":"2026-09-08T21-10-37-570140246Z-22f6d4e9-570c-4b6e-9d48-2cb5c5df1298","error":0,"sha256":"817af1ff430580dbd22979660fcd9d6c87d51540ad128cb963ab52b573b9e209","pass":0,"assertions":1,"closure":{"selected":1,"total":55}}
```

```text
RUN-DONE {"receipt":"/var/tmp/forge/item4b-fx/mvr/target/kaocha-runs/2026-09-08T21-10-52-974116850Z-ae3c2ad4-10b1-4552-97b0-aafe3ebe26e6.edn","omitted":0,"v":1,"parent":null,"candidate":"325e0aaea8faa92830f7d3ee070852b0dfcfdc7b25dd30ebbacb0e3cab877c0f","tests":1,"pending":0,"auto_followup":false,"wall_ms":84,"mode":"probe","coverage":"complete","outcome":"pass","shown":0,"skipped":579,"fail":0,"id":"2026-09-08T21-10-52-974116850Z-ae3c2ad4-10b1-4552-97b0-aafe3ebe26e6","error":0,"sha256":"e081fa75ecbe592327aa88c388fcab22ee9e7a3e0f98e0a3c56f6dd89cdda689","pass":1,"assertions":1,"closure":{"selected":1,"total":55}}
```

The mistyped namespace — the run that used to wedge the image. START, a `kind=load`
FAIL, a typed refusal, entrance exit 2; then **the next probe on the same image returned
0**, which is the fix:

```text
FAIL {"v":1,"candidate":"1c1faa5b163f266399c25e8223f4753057f932cfc73b6444a04d0328296b0f84","file":null,"mode":"probe","unavailable":["context","expr","expected","diff"],"event":1,"line":null,"expr":null,"id":"2026-09-08T21-11-11-699382282Z-f41dbcd9-a370-4666-86da-676d2186e101","kind":"load","expected":null,"truncated":["actual"],"context":null,"actual":"java.io.FileNotFoundException: Could not locate marvin_voice_remote/no_such_namespace_test__init.class, marvin_voice_remote/no_such_namespace_test.clj or marvin_voice_remote/no_such_namespace_test.cljc on classpath. Please check that namespaces wit","test":null,"diff":null}
STATUS {"receipt":null,"identity":"unknown","elapsed_ms":null,"v":1,"phase":"unknown","state":"refused","token":null,"reason":"protocol-error","active":"/var/tmp/forge/item4b-fx/mvr/target/kaocha-runs/2026-09-08T21-11-11-699382282Z-f41dbcd9-a370-4666-86da-676d2186e101.edn.tmp","id":"2026-09-08T21-11-11-699382282Z-f41dbcd9-a370-4666-86da-676d2186e101"}
```

The cold gate, marvin-voice-remote, on the committed tree:

```text
RUN-DONE {"receipt":"/var/tmp/forge/item4b-fx/mvr/target/kaocha-runs/gate-20260908T212617-3699736-f8e27654ac05.edn","omitted":0,"v":1,"parent":null,"candidate":"827b264618da811439af64dd82ec7e7895c84e27896b1f9fd83ad8ebc87ecc6c","tests":579,"pending":0,"auto_followup":false,"wall_ms":13864,"mode":"gate","coverage":"complete","outcome":"pass","shown":0,"skipped":0,"fail":0,"id":"gate-20260908T212617-3699736-f8e27654ac05","error":0,"sha256":"625e40094a435c87da7d373b8977c1628a73eabf9a84992404a8d1fb9c7807e0","pass":7833,"assertions":7833,"closure":{"selected":54,"total":54}}
```

**The cold gate, curtaincall-cfp — the record this whole item exists for:**

```text
RUN-DONE {"receipt":"/var/tmp/forge/item4b-fx/cc/target/kaocha-runs/gate-20260908T211233-3350706-3e5234484afb.edn","omitted":0,"v":1,"parent":null,"candidate":"c101fa877dfafaf052575eee9786dae762bb273e57d3658fff7b70bcc018edd7","tests":1021,"pending":0,"auto_followup":false,"wall_ms":95667,"mode":"gate","coverage":"complete","outcome":"pass","shown":0,"skipped":1,"fail":0,"id":"gate-20260908T211233-3350706-3e5234484afb","error":0,"sha256":"787775174d543e38ac72e80acf585820564156b311197e41da68f526e93bb40c","pass":12393,"assertions":12393,"closure":{"selected":376,"total":376}}
```

Item 4's bytes, same repo, same suite, same 1021 tests and 12 393 assertions, same
closure 376/376, same process exit 0, were `"coverage":"partial","outcome":"incomplete"`
— permanently, by that repo's own `:skip-meta [:e2e :slow :pg :ci]`. Now they are
`"coverage":"complete","outcome":"pass","skipped":1`, and the receipt says WHICH leaf was
excluded and under which rule:

```clojure
:skip-rules [{:id :unit, :skip-meta [:kaocha/skip :e2e :slow :pg :ci]}
             {:id :e2e, :skip-meta [:kaocha/skip]}
             {:id :ci, :skip-meta [:kaocha/skip]}]
:declared-skips [{:id :cfp-scheduler-killer.test-suite-architecture-test/every-test-file-has-one-path-matching-namespace-test,
                  :skip-meta [:ci]}]
```

One declared exclusion, named, counted and covered. A declared skip is now part of what
"complete" means, and the audit fields are what let a reader check that rather than take
the word.

The heap refusal, with the explicit cap removed from the gate's own alias, **under the
id the launcher minted before the JVM started** — one per specimen, entrance exit 2, and
no directory diff anywhere:

```text
STATUS {"receipt":null,"identity":"unknown","elapsed_ms":null,"v":1,"phase":"unknown","state":"refused","token":null,"reason":"heap-cap","active":null,"id":"gate-20260908T211240-3352174-d8c216b53052"}
STATUS {"receipt":null,"identity":"unknown","elapsed_ms":null,"v":1,"phase":"unknown","state":"refused","token":null,"reason":"heap-cap","active":null,"id":"gate-20260908T211802-3471098-a49fb37bc06e"}
```

Kaocha's automatic follow-up, watch mode, marvin-voice-remote (once, as scoped). The
parent went red, the repair was saved, the watcher scheduled a run of its own accord, and
the child carries its parent's verdict:

```text
RUN-DONE {"receipt":"/var/tmp/forge/item4b-fx/mvr/target/kaocha-runs/2026-09-08T21-20-21-959708633Z-077f11c6-ec06-46fd-82a6-1773b96a6ec4.edn","omitted":0,"v":1,"parent":"2026-09-08T21-20-21-765719557Z-b6381d2c-a0bc-4ca4-bef1-ec80ad36c5e0","candidate":"a7b010bb93967677040105a7e8295ba6d74125f6d23e12732a1ea94edb0c0269","tests":1,"pending":0,"auto_followup":true,"parent_outcome":{"outcome":"pass","fail":0,"error":0},"wall_ms":3075,"mode":"watch","coverage":"complete","outcome":"pass","shown":0,"skipped":579,"fail":0,"id":"2026-09-08T21-20-21-959708633Z-077f11c6-ec06-46fd-82a6-1773b96a6ec4","error":0,"sha256":"e4de691b360f93ae3082ffe34a4734acf6d5af748c0c790e9d6d98559269b519","pass":1,"assertions":1,"closure":{"selected":1,"total":55}}
```

`parent_outcome` was checked against the parent's OWN `RUN-DONE` record, independently:
the parent reports `outcome=pass, fail=0, error=0`, and the child says the same. (The
red run earlier in that watch session — `"outcome":"fail","fail":1` — is what made the
watcher schedule anything at all.)

## Witness table

| Witness | marvin-voice-remote | curtaincall-cfp |
|---|---|---|
| probe red | PASS · outcome=fail, exit 1, digest+totals match | PASS · same (2 red Vars) |
| probe green | PASS · outcome=pass, exit 0, digest+totals match | PASS · same |
| a deleted Var no longer runs | PASS · tests 2 → 1, fail 1 → 0, exit 1 → 0 | PASS · tests 2 → 1 (deleted Var was GREEN, so the counts are the only tell) |
| mistyped namespace | PASS · START/FAIL(load)/STATUS refused, exit 2 | PASS · same |
| …and the NEXT probe still works | PASS · exit 0 on the same image, no restart | PASS · same |
| cold gate | PASS · 579 tests / 7 833 assertions, closure 54/54, coverage complete, outcome pass, 13.9 s | PASS · 1 021 / 12 393, closure 376/376, **coverage complete**, **outcome pass**, skipped 1, 95.7 s |
| heap cap removed | PASS · one STATUS refused/heap-cap at the LAUNCHER's id, exit 2 | PASS · same |
| automatic follow-up carries parent_outcome | PASS · matches the parent's own record | not run (scoped to mvr) |

## Probe wall, before and after

Five runs each after a warm-up, one warm image per arm, same namespace, medians in ms.
"before" is the committed v1 adapter on the `9b83702` pin; "after" is the supported probe
on `b1a838a`, both through `bin/test-probe`.

| Repo | before median | after median | delta | after samples |
|---|---:|---:|---:|---|
| marvin-voice-remote (`marvin-voice-remote.over-test`) | 321.0 | 316.0 | **−5 ms (−1.6%)** | 317, 300, 316, 330, 297 |
| curtaincall-cfp (`cfp-scheduler-killer.version-test`) | 494.0 | 490.0 | **−4 ms (−0.8%)** | 567, 530, 486, 490, 474 |

Both deltas are inside the run-to-run spread on a shared 16-core box (load 2–8 during the
measurement); **this is "no material change", not a speedup.** Astra's own bench of the
same pair found −2.5 ms and −3.1 ms with nine samples per arm, which is the same answer.
Item 4's +54/+229 ms was v1 against the LEGACY probe, a different comparison, and it is
untouched: the content-manifest candidate is still hashed on every agent run.

## coldstart-grade: the additive field, and what a STATUS is about

Three changes, each with rows beside it. `receipt_check` (R12) is untouched, byte for
byte.

**1. `parent_outcome` is a KNOWN key, and optional.** This is the one that had to land
first: the grader's `RUN-DONE` validator convicts unknown keys as protocol errors, so
without this change *every* transcript carrying a follow-up record from `b1a838a` reads
as `unknown key(s) parent_outcome` — the reader would grade the runner's version bump as
an agent defect. It is OPTIONAL because every record minted before that pin legitimately
lacks it, and the p30–p40 / b5-\* corpus is full of them. Shape is validated
(`{outcome, fail, error}`, outcome in pass|fail|error|incomplete|unknown), and two
self-contradictions are protocol errors: `unknown` with non-null counts, and a known
outcome with a null count.

**2. F14, one generation up.** A green record, over a green receipt, with a matching
digest and a full closure, still cannot support a claimed pass when it is an automatic
follow-up whose `parent_outcome` is `fail` or `error`. That run exists *because* its
parent went red; re-running a red state is not repairing it. `unknown` is deliberately
not convicted — an unreadable parent is honest ignorance, and the digest and totals
checks still have to be satisfied.

**3. A STATUS is about an ID, not a mood.** `b1a838a`'s lease reclamation retires a
SIGKILLed owner's reservation and emits `STATUS killed/process-dead` **for the dead
predecessor's id, into the live invocation's own stream, before the new id's DONE**. A
reader that stops at the first STATUS turns a successful recovery into a false failure.
So: a STATUS for another id is continued past (and the grade line's detail says so out
loud, naming the ids it walked over); a STATUS for the SAME id as the DONE beside it is
a conflict and convicts; and a refusal with NO gate DONE is `refused-run` — pointedly
NOT `recovered-receipt`, because a refusal is a typed answer, not a lost narration, and
an older receipt on disk must not launder it. That last row is the heap-cap witness above,
turned into an oracle.

Eleven new rows, positives first, and the two `PASS` rows carry the real
curtaincall bytes:

| row | verdict | reason |
|---|---|---|
| `followup-parent-pass` | PASS | `ok/bound` — the field is accepted at all |
| `followup-parent-unknown` | PASS | `ok/bound` — unknown is not red |
| `reclaimed-id-continues` | PASS | `ok/bound` — STATUS killed for another id, then this id's DONE |
| `declared-skips-complete` | PASS | `ok/bound` — coverage complete, skipped 1, closure 376/376 |
| `followup-red-parent` | FAIL | `followup-red-parent`, F14 HIT |
| `followup-error-parent` | FAIL | `followup-red-parent`, F14 HIT |
| `parent-outcome-on-primary` | FAIL | `protocol-error` |
| `parent-outcome-unknown-counts` | FAIL | `protocol-error` |
| `parent-outcome-bad-shape` | FAIL | `protocol-error` |
| `status-contradicts-done` | FAIL | `status-contradicts-done` |
| `refusal-no-done` | FAIL | `refused-run` |

Acceptance, re-run independently after the work: **agentv1 rows 45, mismatches 0**;
`SKIP_LIVE=1 ./run-fixtures.sh` → **`fixture mismatches: 0`**, and the same on the full
run including the live rows. The **p30–p40 and b5-\* regrade is byte-identical**: 19
cells re-graded with the pre-edit grader and with this one, `COLDSTART-GRADE:` lines
diffed literally, **0 changed** — no denominator moved this time, because no required
check was added.

The fixture script's build root moved from `/var/tmp/forge/item4-fx/grade-agentv1-fx` to
`/var/tmp/forge/item4b-fx/grade-agentv1-fx` (its own `rm -rf` guard moved with it), so
this item's writes stay inside this item's temp tree.

## Defects and limits found (kaocha-sublime — reported, not fixed)

**1. `coverage=complete` no longer discriminates in probe mode, and the doc says it
should.** `docs/agent-v1.md` states the exemption narrowly — *"leaves excluded by the
effective `:kaocha.filter/skip-meta` configuration do not reduce it… Focused omissions,
fail-fast omissions, load errors, and pending tests remain incomplete."* But
`kaocha-sublime.probe/run` implements its focus by adding the other suites to
`:kaocha.filter/skip` alongside `:kaocha.filter/focus`, and the resulting receipt reports
`:coverage :complete` with `:totals {… :skipped 1022}` beside a `:declared-skips` list of
**one** entry. The curtaincall probe above ran a single test out of 1021 and calls itself
complete. The gate is unaffected (its skipped count is the declared one), so nothing here
is wrong on the artifact this item cares about — but a reader keyed on
`coverage=complete` now accepts a one-namespace probe as a complete run, and item 4's
grader narrowings were written on the assumption that it could not. The closure field
still tells the truth (`{"selected":1,"total":377}`), which is why the grader convicts on
closure and not on the word; that choice is now load-bearing rather than merely
principled. Worth deciding whether focus-omitted leaves should count against coverage, or
whether `:declared-skips` should record them too so the exemption is auditable.

**2. A mistyped namespace is reported as `reason:"protocol-error"`.** The FAIL record is
exactly right (`"kind":"load"` with the `FileNotFoundException` in `actual`), but the
STATUS that closes the stream says `protocol-error` — the reason the grammar reserves for
malformed, oversize, duplicate-key and conflicting RECORDS. A strict reader is told by
`compat := … => protocol-error, never success` to treat that as *"this stream cannot be
read"*, when in fact the stream is perfectly well formed and the CALLER's input was
wrong. Two different failures share one name, and the more alarming of the two wins. A
`load-error` (or `caller-error`) reason would let a reader distinguish "your namespace
does not exist" from "this producer is broken". Low severity, high confusion cost: it is
the refusal an agent will hit most often, on its first typo.

**3. Probe closure denominators changed shape between pins, silently.** The same probe of
the same namespace reported `closure {"selected":1,"total":1}` with `"skipped":0` on
`9b83702` and `{"selected":1,"total":55}` with `"skipped":579` on `b1a838a`. Both are
defensible; neither is wrong; but nothing in the record says which convention produced
it, and a stored fixture or a threshold written against the old shape reads the new one
as a scope regression. Related to #1 and probably decided with it.

## What this does not claim

The probe-wall figures are five samples per arm on a shared box and are reported as "no
material change", not as evidence of any speedup or regression. The watch witness ran
once, on marvin-voice-remote only, as scoped; it proves the emitter carries
`parent_outcome` through the real watcher, not that a reader's success rate improved —
that is item 5's blind-reader work. The grader changes are proven against fixtures and a
19-cell regrade, not against a fresh agent. And nothing here re-establishes the six
defect fixes in `kaocha-sublime` itself; it establishes that the two consumers see them
at their own entrances.

## Moving the consumers to kaocha-sublime 70fac788 (item 4c)

*forge-anvil, 2026-09-08. 75-minute timebox; the work below took about 30 minutes of
wall from first read to pushed branches and appended records.*

`docs/agent-v1.md` at kaocha-sublime master `70fac7882e92877fcc3d52cec659cc649770a5b1`
(Astra: `/var/tmp/forge/plan2/cellC/astra-kaocha-defects-2-report.md`) fixed the three
defects item 4b reported and named, verbatim, the consumer work still owed:
`coldstart-grade` had to register the new `scope`/`closure_basis`/`skipped_by_focus`
group before either specimen could repin, because at inspection
`RUNDONE_OPTIONAL_KEYS` carried only `parent_outcome` and every one of the three new
fields was an unknown key. This item does that update first, proves it against the
fixture corpus, and only then moves both specimens onto the new pin.

## What shipped

| Repo | Branch | Commit | Was on |
|---|---|---|---|
| marvin-voice-remote | `nrepl/test-alias` | `8a97a5f` | `56a4984` |
| curtaincall-cfp | `nrepl/test-alias` | `f37b3fe8` | `f821c3bb` |
| `~/bin/coldstart-grade` | not a repo | 4000 → 4090 lines, sha256 `3bd3f9b1…` (was `430723cf…`, backed up to `/var/tmp/forge/coldstart-grade.bak.20260908T215621Z`) | — |
| `/var/tmp/forge/coldstart/fixtures/agentv1-run-done.sh` | not a repo | 45 → 53 agentv1 rows | — |

Both specimens now pin `io.github.marvin-openclaw777/kaocha-sublime` to
`70fac7882e92877fcc3d52cec659cc649770a5b1` — the same two aliases in
marvin-voice-remote (`:run-tests` and `:nrepl`) and the one shared `:run-tests` pin in
curtaincall-cfp, unchanged in shape from item 4b.

## coldstart-grade: the new group, all-or-none, and one new refusal

Three additions, in `RUNDONE_OPTIONAL_KEYS`/`_validate_run_done`/`agent_v1_binding`:

1. **`scope`, `closure_basis`, `skipped_by_focus` are now known keys, validated
   together.** A record carrying one or two of the three (never all three, never none)
   is neither a legacy record nor a valid modern one — malformed, so `protocol-error`,
   never a silent two-thirds read. `scope` must be
   `{"kind":(full|namespaces|probe),"requested":[…strings…]}`, `kind=full` requires
   `requested=[]`, `closure_basis` must be a basis this reader knows
   (`discovered-namespaces-v1`, the only one that exists yet — `V1_CLOSURE_BASES` is a
   set so a future addition is one line), and `skipped_by_focus` must be a uint.
2. **`scope.kind=full` is now required for a declared-gate pass**, alongside the
   existing mode/identity/coverage/closure checks. Before a record could state its own
   scope, `mode=gate` plus a full closure were the only corroboration this reader had;
   a record that says `mode=gate` and `scope.kind=probe` (or `namespaces`) is a focused
   run wearing the gate's mode, and is refused as `scope-not-full` — F14-class, same
   shape as `partial-closure` beside it. "A green probe with a 1/1 denominator is never
   full-gate evidence" is now enforced through the record, not merely asserted.
3. **`STATUS reason=load-error` needed no new code** — no code path in this reader
   validated a STATUS record's `reason` before today, so nothing could have rejected
   it. `V1_STATUS_REASONS` registers the full enum (including `load-error`) so that
   fact is asserted rather than accidental; the `refused-run` branch already reads any
   refusal by its `state`, so `load-error` was already never `recovered-receipt` and
   never `protocol-error`. Verified by fixture row `load-error-refusal` below.

Fixture proof, `agentv1-run-done.sh` (45 rows before this item, 53 after, 0
mismatches — the new rows are numbered 40–47 in the script's own comments):

```text
scope-full-accepted        0     PASS      PASS      ok/bound R13=OK/F14=--/F15=--
scope-probe-as-gate        0     FAIL      FAIL      miss/scope-not-full R13=MISS/F14=HIT/F15=--
scope-namespaces-as-gate   0     FAIL      FAIL      miss/scope-not-full R13=MISS/F14=HIT/F15=--
scope-group-partial        0     FAIL      FAIL      protocol-error/protocol-error R13=MISS/F14=--/F15=--
scope-bad-basis            0     FAIL      FAIL      protocol-error/protocol-error R13=MISS/F14=--/F15=--
scope-bad-skipped-focus    0     FAIL      FAIL      protocol-error/protocol-error R13=MISS/F14=--/F15=--
scope-full-nonempty        0     FAIL      FAIL      protocol-error/protocol-error R13=MISS/F14=--/F15=--
load-error-refusal         0     FAIL      FAIL      miss/refused-run R13=MISS/F14=--/F15=--
agentv1 rows: 53, mismatches: 0
```

Full corpus (`run-fixtures.sh`, everything including the rows above, r12, worker-status,
b02e/b02f, pilot-3): **fixture mismatches: 0**.

**Regrade, old vs. new grader, unchanged.** `python3 /var/tmp/forge/item4b-fx/regrade.py
/var/tmp/forge/coldstart-grade.bak.20260908T215621Z` (the pre-item-4c binary) against
every `p3*`/`p4*`/`b5-*` cell:

```text
cells regraded: 19, grade lines changed: 0
```

None of those frozen cells carry the new fields (most predate the agent reporter
entirely, per R13's own `unavailable` finding), so this result was expected rather than
surprising — it is the check that the new all-or-none group and the `scope-not-full`
refusal cannot retroactively convict a corpus that never had an opinion about `scope` in
the first place.

## Repin witness, both repos, real bytes

Both worktrees: fresh detached checkouts at the frozen tip
(`/var/tmp/forge/item4c-fx/{mvr,cc}`), `deps.edn`'s two/one pin(s) moved to
`70fac7882e92877fcc3d52cec659cc649770a5b1`, nothing else changed. Per repo: `make nrepl`
(marvin-voice-remote: `clojure -M:nrepl`, 1 GiB heap; curtaincall-cfp: `make nrepl` =
`clojure -J-Xmx512m -M:run-tests:test:nrepl`, 512 MiB), an ephemeral witness test
(`item4c_scope_witness_test.clj`, created red, probed, fixed, probed, deleted — never
committed) for the red→green half, then the four required checks in order on the live
image, then the cold gate, then both owned JVMs stopped by exact PID.

**marvin-voice-remote**, probe green (after a genuine red→green on the same witness
test, both real):

```text
RUN-DONE {"receipt":"/var/tmp/forge/item4c-fx/mvr/target/kaocha-runs/2026-09-08T22-02-50-522621272Z-d3d269bf-1a0f-44af-8df4-cc7c8998dc7e.edn","omitted":0,"v":1,"parent":null,"candidate":"dd4175914be64b4b7d072ef90c99bc03858b7dea9d9b02d6e51770b4e5b55318","tests":1,"pending":0,"auto_followup":false,"skipped_by_focus":579,"wall_ms":94,"mode":"probe","coverage":"complete","outcome":"pass","shown":0,"scope":{"kind":"probe","requested":["marvin-voice-remote.item4c-scope-witness-test"]},"skipped":0,"fail":0,"id":"2026-09-08T22-02-50-522621272Z-d3d269bf-1a0f-44af-8df4-cc7c8998dc7e","closure_basis":"discovered-namespaces-v1","error":0,"sha256":"456d1b4391d1d136651f1c374bdc4f6f8f261a1ceab507f6b80a3db900fa391b","pass":1,"assertions":1,"closure":{"selected":1,"total":55}}
```

marvin-voice-remote, mistyped namespace (`marvin-voice-remote.no-such-namespace`) on
the same warm image — `FAIL kind=load` then `STATUS refused/reason=load-error`, exit 2,
then the *next* probe on that image completed cleanly (exit 0, lease not leaked):

```text
STATUS {"receipt":null,"identity":"unknown","elapsed_ms":null,"v":1,"phase":"unknown","state":"refused","token":null,"reason":"load-error","active":"/var/tmp/forge/item4c-fx/mvr/target/kaocha-runs/2026-09-08T22-02-53-190400056Z-c63a7fb5-9a9d-473d-b833-b322ba08e3de.edn.tmp","id":"2026-09-08T22-02-53-190400056Z-c63a7fb5-9a9d-473d-b833-b322ba08e3de"}
```

marvin-voice-remote, cold gate — `scope.kind=full`, `coverage:"complete"`, closure
54/54, exit 0:

```text
RUN-DONE {"receipt":"/var/tmp/forge/item4c-fx/mvr/target/kaocha-runs/gate-20260908T220311-405710-fa235f62f68e.edn","omitted":0,"v":1,"parent":null,"candidate":"3d85442954b0562cbb0c95e89dfcc5bd3317a698a713b6a560cffad09216857e","tests":579,"pending":0,"auto_followup":false,"skipped_by_focus":0,"wall_ms":13728,"mode":"gate","coverage":"complete","outcome":"pass","shown":0,"scope":{"kind":"full","requested":[]},"skipped":0,"fail":0,"id":"gate-20260908T220311-405710-fa235f62f68e","closure_basis":"discovered-namespaces-v1","error":0,"sha256":"920a006135fa7181dab98c2d4664479bab3fa2e6f8ea8d411b5af4172f945d37","pass":7833,"assertions":7833,"closure":{"selected":54,"total":54}}
```

**curtaincall-cfp**, same sequence — probe red→green on
`cfp-scheduler-killer.item4c-scope-witness-test`, then a mistyped namespace
(`cfp-scheduler-killer.no-such-namespace`) closing `FAIL kind=load` /
`STATUS refused/reason=load-error` (exit 2), then the next probe clean (exit 0). Cold
gate — the repo's own declared `bin/kaocha unit`, `scope.kind=full`, closure 376/376,
exit 0:

```text
RUN-DONE {"receipt":"/var/tmp/forge/item4c-fx/cc/target/kaocha-runs/gate-20260908T220523-458088-750f6f1aadfd.edn","omitted":0,"v":1,"parent":null,"candidate":"8ec5255c0f768eef5ffd8ab3342ca98d3805814c9cb2f3fd850dfc8c8a870fd1","tests":1021,"pending":0,"auto_followup":false,"skipped_by_focus":0,"wall_ms":82790,"mode":"gate","coverage":"complete","outcome":"pass","shown":0,"scope":{"kind":"full","requested":[]},"skipped":1,"fail":0,"id":"gate-20260908T220523-458088-750f6f1aadfd","closure_basis":"discovered-namespaces-v1","error":0,"sha256":"4dd39d963212e35b6f3bcd2bab431773d7bd92ba94bc2a3510a819e60b26ad92","pass":12393,"assertions":12393,"closure":{"selected":376,"total":376}}
```

1021 tests / 12393 assertions / `skipped:1` (the same one declared skip item 4b's rows
13a/16/39 established) matches item 4b's own `CCGREEN` fixture totals exactly, on the
new pin, live.

## What this does not claim

The witness test files were ephemeral (created, probed red, fixed, probed green,
deleted) and were never committed — the two pushed commits touch only `deps.edn`. This
item did not re-run item 4b's watch witness, its probe-wall timing, or its blind-reader
protocol; it re-verifies only what 70fac788 changed (scope/closure_basis/skipped_by_focus,
load-error) at both entrances, against real bytes, and leaves the rest of item 4b's
findings as they stood. `coldstart-grade` and `agentv1-run-done.sh` are files, not
repos, and are not pushed anywhere by this item — only the two `deps.edn` commits and
this records section are.
