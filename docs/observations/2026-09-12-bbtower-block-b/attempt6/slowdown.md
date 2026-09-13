# Step 1: blocked by executed temp-isolation refusal

Subject `efdced9bd47dfb1265fbf9b48515ea8f89a42dc9`; supplied base `eae1e43280635be9b4e317e176d29476fd358702` verified before use.
Manifest `da0217373e110e79ca729d7624afc7ddb767671d89b86dad1f6ae27488a059af`.

The base coordinator scheduled the required 61 namespaces over its automatic
8-lane width. All eight lanes exited 97 before emitting results. The coordinator
exited 1 and reported 61 missing namespaces, zero tests and zero assertions.
The zero serial-equivalent sum is an empty fold, **not a 0 ms performance result**.

```text
tmp-refused: java.io.tmpdir base=/var/tmp/forge/packets/35dc856c-986f-4921-b4d2-29bb3ff5afba/tmp was handed a re-exec sentinel it does not own: it names root="/var/tmp/forge/packets/35dc856c-986f-4921-b4d2-29bb3ff5afba/tmp/clj-surgeon-suite-3008526-844d44c3" but this process's java.io.tmpdir is "/var/tmp/forge/packets/35dc856c-986f-4921-b4d2-29bb3ff5afba/tmp". Refusing to treat a shared base as a private run root. Launch with -Djava.io.tmpdir=/var/tmp/clj-surgeon, or export TMPDIR=/var/tmp/clj-surgeon before invoking bb (bb does not read JAVA_TOOL_OPTIONS). On macOS the per-user $TMPDIR the shell already sets is real disk and needs no override -- an EMPTY TMPDIR is the usual cause of this refusal, not a wrong one.
```

The private-root argv flag is overwritten by the sealed `_JAVA_OPTIONS` temp
property. `tmp_leak_support.clj:349` compares the private sentinel with the
actual property; line 372 refuses the mismatch. Matching this environment
across arms cannot make the baseline execute. The 1 GiB heap is accepted;
it is not the reason for stopping. Changing the frozen environment or weakening
the isolation oracle requires Fable/Gene authority, and neither was done.

| Arm | Namespace sum | Coordinator makespan | Complete launch wall |
|---|---|---|---|
| (a) base, 61 members | Unknown: none executed | 6127 ms, failed startup only | 7440 ms |
| (b) subject, bb children disabled | Not run | Not measured | Not measured |
| (c) subject as shipped | Not run | Not measured | Not measured |

Ten largest (a)/(c) namespace deltas: unavailable. No measured evidence identifies
bb contention, width, heap or startup charging as the original slowdown cause.
No step-2 repair can be selected from this run.

Every JVM worker launched in this attempt ran at 1 GiB, not the Makefile’s 512 MiB, and under the packet temp directory, as imposed by the retained _JAVA_OPTIONS. Absolute sums under these settings are therefore not the landing gate’s numbers. The 60,000 ms and 240,000 ms verdicts inside this packet are provisional; the landing gate outside the packet (Fable’s ship path, seat environment) must re-establish them before any landing. Here no valid namespace sum or budget verdict was obtained: workers refused before testing.

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
