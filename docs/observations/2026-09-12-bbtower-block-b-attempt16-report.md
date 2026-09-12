# Block B attempt 16: PASS

The 120-byte sun_path witness now selects the first writable root whose UTF-8
byte length plus separator plus LONGEST_LEAF fits SUN_PATH_BUDGET. Candidates
are, in order: CLJ_SURGEON_GATE_ROOT when nonempty; the production csg-<uid>
spelling beneath the disk-policy TMPDIR; that spelling beneath /var/tmp.
A temporary-file creation checks actual write authority, including Landlock.
Newly created candidate directories are removed by unittest cleanup; existing
roots remain intact. Every socket uses a fresh UUID namespace.

If no candidate qualifies, unittest.SkipTest explicitly names the envelope
fact and every candidate's root bytes, full sun_path bytes, and writability.
This conditional skip is explicitly authorized by the attempt16 brief, which
supersedes the original block's blanket stop on skipping tests. No production
code, budget, membership, or runtime assignment changed.

## Red and green evidence

The [wrapper](restricted-oracle.py) reuses attempt15/restricted-oracle.py's exact
Landlock restriction function, allowing writes only beneath the caller's
TMPDIR. The adaptation retains TMPDIR instead of replacing it with a short
random directory. Neither wrapper changes shared permissions.

| Run | Result / selected branch | Evidence |
|---|---|---|
| Original source, restricted UUID-shaped TMPDIR | Exact line-445 AssertionError: 117 not less than or equal to 100; 21 tests, one failure, two existing platform skips | [red](oracle-red-117.log) |
| Original source, four-byte-longer TMPDIR | Same failure class: 121 > 100 | [initial red](oracle-red.log) |
| Repaired source, identical restricted UUID-shaped TMPDIR | Named envelope skip: TMPDIR candidate 72 root bytes / 114 sun_path bytes, writable; fallback 17 / 59, unwritable. 21 tests, three skips, zero errors/failures | [green restricted](oracle-green-restricted.log) |
| Repaired source, unrestricted packet-shaped TMPDIR | Real bind via policy fallback /var/tmp/csg-1011, 17 / 59 bytes; 21 tests, two platform skips | [long](oracle-long.log) |
| Repaired source, unrestricted short TMPDIR | Real bind via policy TMPDIR /var/tmp/forge/bbtower-fx/a16/csg-1011, 38 / 80 bytes; 21 tests, two platform skips | [short](oracle-short.log) |
| Repaired source, restricted short TMPDIR | Real bind via same policy TMPDIR; 21 tests, two platform skips | [short restricted](oracle-short-restricted.log) |
| Repaired source, operator override | Real bind via CLJ_SURGEON_GATE_ROOT=/var/tmp/forge/bbtower-fx/a16/operator, 38 / 80 bytes; 21 tests, two platform skips | [operator](oracle-operator.log) |

The requested long TMPDIR was created and used exactly:
`/var/tmp/forge/bbtower-fx/packets/0123456789ab-0123-0123-0123-0123456789ab/tmp`.
The exact-117 paired Landlock runs use
`/var/tmp/forge/packets/01234567-0123-0123-0123-0123456789ab/tmp`.
All oracle commands use `python3 -B -m unittest discover -v -s test/oracles
-p test_gate_slot.py`, either directly or through the retained wrapper.
Each passing real-bind branch still asserts occupied-slot refusal, the byte
budget, and directory mode. The skip does not claim kernel-bind coverage.

## Tooling finding for Fable

A packet temp root of at most 40 bytes would let this witness bind for real
inside the envelope on this host: 40 + 1 + len(csg-1011) + 1 + LONGEST_LEAF(41)
is 91 bytes, within the 100-byte cap. The restricted short-TMPDIR run above
also demonstrates the real bind. This is a packet tooling ship item for Fable
to file, outside this block; this change does not alter the packet apparatus.
Filing is owed to Fable; this session has not sent an external message or
claimed that a ticket exists.

## Verification

One `make test-fast`: exit 0, 94 namespaces, zero isolation violations,
45,087 ms serial-equivalent fast sum / 60,000 ms ceiling, 24,689 ms makespan
([test-fast.log](test-fast.log)). Both make commands run sequentially with
TMPDIR=/var/tmp/forge/bbtower-fx and
JAVA_TOOL_OPTIONS='-Xmx1g -Djava.io.tmpdir=/var/tmp/forge/bbtower-fx', with
_JAVA_OPTIONS removed.

One `make landing-gate-prewarm`, exit 0; no repair or retry needed. The
[receipt](gate-receipt.edn) records :state :passed, :problems [], :prewarm? true,
:landing? false; all seven stages and shell checks exited zero. Complete wall:
387,393 ms, from 2026-09-12T13:17:50.473970273Z to
2026-09-12T13:24:17.867287948Z (machine-recorded receipt timestamps).
The MCP fast sum is 45,234 / 60,000 ms; integration 67,153 / 240,000 ms;
bb runtime 235,793 / 343,102 ms. Alias, MCP and bb suite makespans are
90,253, 90,104 and 86,081 ms respectively.

[gate.md](gate.md) is the verbatim combined stdout/stderr capture, including
every emitted budget and refusal line. No separate refusal-census summary
was emitted. [prewarm-run/](prewarm-run/) retains the complete worker output
and receipts. Its lane-0.out and lane-0.err show the oracle performed a real
bind via policy TMPDIR with 34 root bytes / 76 sun_path bytes: 21 tests,
zero failures/errors and only the two existing platform skips.

## DOGFOOD

| Source edit | Intent | Mechanism | Refusal type | Repair text sufficient? |
|---|---|---|---|---|
| test/oracles/test_gate_slot.py | Ordered writable byte-budgeted selection, explicit envelope skip, selected/root-bind output | Native apply_patch; real oracle, Landlock, test-fast and prewarm | None | N/A |
| attempt16/restricted-oracle.py | Reuse attempt15 Landlock with supplied long or short TMPDIR | New Python file via native shell write; executed red and green | None | N/A |

The working-tree routing skill selects native for this ordinary Python edit;
no automatic Surgeon class applies. No Surgeon calls, refusals, or fallbacks
occurred. This is executed boundary dogfood, not a Surgeon performance claim.
The source diff passed git diff --check before its commit.

## Commits and limits

- Starting branch tip: f19a6a44; `git checkout bb-rewrite-tower-local` confirmed
  branch ownership. No push, merge, tag, or shared installation change.
- a1f9aa72 preserves the untracked fifth packet report under
  [prewarm-checkonly-run5/](prewarm-checkonly-run5/), committed first.
- 758e8604 contains the witness repair tested by all green runs and gates.
- The commit containing this report adds attempt16 evidence last.
- Least sure: native macOS was not tested; Landlock verifies the focused oracle,
  while prewarm runs outside the packet as required by the binding brief.
- The original brief's attempt1 REPORT.md and probe-contract.md paths are absent
  in this checkout. Attempt15 and the retained packet report supply the current
  premises; prior block deliverables are not re-claimed here.
- Owed: Fable's independent review, the short packet-root ship item, and any
  full landing/battery-fresh authority. Prewarm alone grants no landing authority.
