import datetime, hashlib, json, pathlib, re, subprocess
root=pathlib.Path('/home/forge/src/clj-surgeon-bbtower')
out=root/'docs/observations/2026-09-12-bbtower-block-b/attempt6'
manifest='da0217373e110e79ca729d7624afc7ddb767671d89b86dad1f6ae27488a059af'
launch=json.loads((out/'a-launch.json').read_text())
log=(out/'a-base.log').read_text()
err=(out/'a-base/lane-0.err').read_text()
refusal=next(x for x in err.splitlines() if x.startswith('tmp-refused:'))
makespan=int(re.search(r'battery-parallel: makespan (\d+) ms',log)[1])
subject=subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()
base=subprocess.check_output(['git','-C','target/base-checkout','rev-parse','HEAD'],text=True).strip()
admission={'subject':subject,'base':base,'manifest_id':manifest,'context_pack_sha256':hashlib.sha256(pathlib.Path('/var/tmp/forge/packets/35dc856c-986f-4921-b4d2-29bb3ff5afba/context-pack.md').read_bytes()).hexdigest(),'recorded_utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'base_tracked_diff':subprocess.check_output(['git','-C','target/base-checkout','diff','--stat'],text=True),'subject_tracked_diff':subprocess.check_output(['git','diff','--stat'],text=True),'worker_exit_97_count':len(re.findall(r'^========== lane .* exit 97,',log,re.M))}
(out/'admission-evidence.json').write_text(json.dumps(admission,indent=2)+'\n')
pre='Every JVM worker launched in this attempt ran at 1 GiB, not the Makefile’s 512 MiB, and under the packet temp directory, as imposed by the retained _JAVA_OPTIONS. Absolute sums under these settings are therefore not the landing gate’s numbers. The 60,000 ms and 240,000 ms verdicts inside this packet are provisional; the landing gate outside the packet (Fable’s ship path, seat environment) must re-establish them before any landing. Here no valid namespace sum or budget verdict was obtained: workers refused before testing.'
slow=f'''# Step 1: blocked by executed temp-isolation refusal

Subject `{subject}`; supplied base `{base}` verified before use.
Manifest `{manifest}`.

The base coordinator scheduled the required 61 namespaces over its automatic
8-lane width. All eight lanes exited 97 before emitting results. The coordinator
exited 1 and reported 61 missing namespaces, zero tests and zero assertions.
The zero serial-equivalent sum is an empty fold, **not a 0 ms performance result**.

```text
{refusal}
```

The private-root argv flag is overwritten by the sealed `_JAVA_OPTIONS` temp
property. `tmp_leak_support.clj:349` compares the private sentinel with the
actual property; line 372 refuses the mismatch. Matching this environment
across arms cannot make the baseline execute. The 1 GiB heap is accepted;
it is not the reason for stopping. Changing the frozen environment or weakening
the isolation oracle requires Fable/Gene authority, and neither was done.

| Arm | Namespace sum | Coordinator makespan | Complete launch wall |
|---|---|---|---|
| (a) base, 61 members | Unknown: none executed | {makespan} ms, failed startup only | {launch['elapsed_ms']} ms |
| (b) subject, bb children disabled | Not run | Not measured | Not measured |
| (c) subject as shipped | Not run | Not measured | Not measured |

Ten largest (a)/(c) namespace deltas: unavailable. No measured evidence identifies
bb contention, width, heap or startup charging as the original slowdown cause.
No step-2 repair can be selected from this run.

{pre}

The first launch accidentally used the tool's default login shell, which rewrote
TMPDIR and JAVA_TOOL_OPTIONS to /var/tmp/forge. It exited 1 after 6,509 ms
(coordinator makespan 4,254 ms); all lanes exited 97 on directory creation denied
by Landlock. `a-login-shell.json`, `a-login-shell.log` and `a-login-shell/` retain
that harness error. No namespace ran. The same frozen coordinator argv was then
executed with `login=false`, inheriting the sealed environment unchanged; this
is the authoritative refusal above. No clocks or argv were rewritten. Archived
first-run receipt paths name the original `a-base/` location; their logs now live
under `a-login-shell/`. No historical receipt is presented as fresh measurement.

Receipts: [launch](a-launch.json), [coordinator output](a-base.log),
[coordinator receipt](a-base/receipt.edn), [meter](meter.tsv).
'''
(out/'slowdown.md').write_text(slow)
(out/'gate.md').write_text('# Gate status\n\nZero landing-gate-prewarm attempts; zero test-battery attempts. No prewarm refusal-census or budget lines were emitted. Ordered execution is blocked at step 1.\n\nBase diagnostic coordinator output, verbatim (not a prewarm or budget pass):\n\n```text\n'+ '\n'.join(x for x in log.splitlines() if x.startswith(('Ran ', 'test-isolation:', 'battery-parallel: makespan ')))+'\n```\n')
owed=[
 {'what':'Step 1: valid a/b/c matched measurements, sums, makespans and ten deltas','owner':'Fable','unblock':'Reseal a launch environment that preserves each child private tempdir (1 GiB heap may remain), or authorize an equivalent measured protocol preserving the isolation oracle; then rerun all arms.'},
 {'what':'Step 2: measured-cause coordinator repair and make test-fast once','owner':'Astra','unblock':'Step 1 completes and identifies a cause; keep 60000 ms ceiling unchanged.'},
 {'what':'Step 3: NEW bb ceiling, derivation and boundary witnesses; Fable ratification','owner':'Fable','unblock':'A successful step-2 run supplies fast and bb serial sums; approve the derived new value in review.'},
 {'what':'Step 4: probe refusals, completeness, receipt boolean false seams and encounter score','owner':'Astra','unblock':'Earlier ordered steps complete; encounter entrance is installed but no score was attempted.'},
 {'what':'Step 5: feature-thread bb/JVM measurements, runtime witness and make test-integration once','owner':'Astra','unblock':'Earlier steps complete under executable environment; preserve 240000 ms ceiling.'},
 {'what':'Step 6: final-tip prewarm, at most one repair and final prewarm; verbatim gate lines','owner':'Astra','unblock':'Steps 1-5 complete; neither of the two allowed prewarm attempts was consumed.'},
 {'what':'Registered make test-battery obligation and outstanding zero-JVM acceptance checks','owner':'Astra','unblock':'Executable environment and completed implementation; ordered stop currently prevents gate completion.'},
 {'what':'Seat-environment landing budgets and independent acceptance','owner':'Fable','unblock':'External ship path re-establishes budgets and independent review; this packet cannot authorize landing.'}]
measurements=[{'name':'a-base failed coordinator makespan (not namespace performance)','before':None,'after':makespan,'receipt':str(out/'a-base/receipt.edn')},{'name':'a-base complete failed launch wall ms','before':None,'after':launch['elapsed_ms'],'receipt':str(out/'a-launch.json')},{'name':'a-base executed namespaces (61 scheduled)','before':61,'after':0,'receipt':str(out/'a-base.log')},{'name':'a-base namespace sum; b/c sums and makespans; top deltas','before':'Unmeasured','after':'Unmeasured; baseline refused','receipt':str(out/'slowdown.md')},{'name':'bb sum and new ceiling; feature-thread bb/JVM; integration sum','before':'Unmeasured','after':'Owed in frozen order','receipt':str(out/'slowdown.md')},{'name':'prewarm attempts and emitted budget/census lines','before':0,'after':0,'receipt':str(out/'gate.md')},{'name':'invalid login-shell launch wall ms (harness error)','before':None,'after':6509,'receipt':str(out/'a-login-shell.json')}]
report={'verdict':'blocked','manifest_id':manifest,'findings':[{'severity':'blocking','file':'test/clj_surgeon/tmp_leak_support.clj','line':372,'reproduction':'In the verified base checkout, execute the exact argv in a-launch.json with the sealed environment (login=false). All eight workers exit 97; no tests execute. Packet _JAVA_OPTIONS overwrites the private tmpdir.','exact_output':refusal},{'severity':'info','file':'docs/observations/2026-09-12-bbtower-block-b/attempt6/a-login-shell.json','line':1,'reproduction':'Initial exec used default login=true, causing shell initialization to replace TMPDIR/JAVA_TOOL_OPTIONS. Corrected to login=false with identical coordinator argv; both launch records retained.','exact_output':next(x for x in (out/'a-login-shell/lane-0.err').read_text().splitlines() if x.startswith('tmp-refused:'))}],'measurements':measurements,'commits':[],'least_sure':['Original slowdown cause remains unknown because no baseline namespace executed.','New bb margin and feature-thread runtime classification have no current measurement.','Later gate prerequisites remain untested.'],'disagreements':['The reseal authorizes 1 GiB/shared packet temp settings as-is, but execution proves the shared temp override defeats the existing private-root isolation guard. Heap and one-suite lease are accepted. Fable/Gene must resolve the executable protocol; changing the guard or injected options is outside this packet.'],'owed':owed}
(out/'report.json').write_text(json.dumps(report,indent=2)+'\n')
summary=f'''NO-GO

Build status: **blocked at step 1 by an executed temp-isolation refusal**.
All eight base lanes exited 97; none of the required 61 namespaces ran.
The sealed `_JAVA_OPTIONS` overwrote private worker temp-directory flags, so
`secure-tmpdir!` refused `sentinel-mismatch`. This is not a 1 GiB heap stop.

Subject remains detached at `{subject}`; base `{base}` verified clean.
Manifest: `{manifest}`. No implementation changes or commits.

{pre}

1. Step 1: base failed-startup makespan **{makespan} ms**, complete launch **{launch['elapsed_ms']} ms**.
   Namespace sums and a/c deltas remain unknown. Arms b/c were not run after
   the baseline refused. [slowdown.md](slowdown.md) records the exact refusal,
   clocks, argv and the separately retained login-shell harness error.
2. Step 2: no measured cause; coordinator repair and test-fast remain owed.
3. Step 3: no measured serial bb sum; no new ceiling was invented.
4. Step 4: probe registry, completeness, boolean seams and encounter score
   remain owed in the frozen order.
5. Step 5: feature-thread comparison, classification and integration remain owed.
6. Step 6: zero prewarm attempts; [gate.md](gate.md) records that no prewarm
   budget/census lines exist. Battery and independent acceptance remain owed.

Fable or Gene must reseal a launch environment that lets each worker retain
its private startup temp directory, or authorize an equivalent protocol that
preserves the existing isolation oracle. The 1 GiB cap can remain. Matching
an environment that prevents tests from running cannot produce slowdown deltas.
The shared launcher repair remains outside this packet, as instructed.

Only attempt6 observations were written. The initial login-shell error was
corrected locally without changing frozen coordinator argv or `_JAVA_OPTIONS`;
its complete logs and receipt remain alongside the sealed-environment refusal.
No tests, budgets, classification, instruction plates or shared install changed.
Two coordinator attempts ran sequentially, with explicit heap limits; no servers,
sub-agents or additional models were launched.

Least sure: slowdown mechanism, bb ceiling and later gate prerequisites.
[report.edn](report.edn) contains all required vector fields, named owners and
unblock conditions. Independent acceptance remains external.
'''
(out/'REPORT.md').write_text(summary)
# Preserve both raw launch clocks in the main meter; original small meter retained too.
old=(out/'login-shell-meter.tsv').read_text().splitlines()[1].replace('a-base\t','a-login-shell\t',1).replace('\ta-launch.json','\ta-login-shell.json')
rows=(out/'meter.tsv').read_text().splitlines()
(out/'meter.tsv').write_text(rows[0]+'\n'+old+'\n'+'\n'.join(rows[1:])+'\n')
print('Wrote report sources and retained evidence.')
