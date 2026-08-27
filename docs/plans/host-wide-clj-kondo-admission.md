# Plan: Host-Wide clj-kondo Admission

## Incident and outcome

On 2026-08-27, five whole-repository clj-kondo launches between 18:11:12 and
18:12:00 preceded a Skiff load increase from 20.7 to 118.5. One observed
analyzer used 98% CPU and 443 MB RSS while the host already held about 34.9 GB
of swap. Spotlight and paging prolonged the incident after the analyzer exited.

The immediate outcome is one host-wide admission gate for every Surgeon-owned
clj-kondo launch. At most one admitted analyzer may execute on the laptop.
Concurrent callers wait only within their existing deadline; an expired wait
is unavailable analyzer authority, never a semantic finding.

## Admission law

```text
Surgeon command
  -> basename is not clj-kondo -> execute unchanged
  -> basename is clj-kondo
       -> acquire one OS-owned host lock within the command deadline
       -> record owner PID, canonical command CWD, and start time
       -> inherit the lock descriptor and exec the exact analyzer
       -> analyzer owns the lock through its complete lifetime
       -> release on success, failure, timeout, cancellation, or process death
```

The lock is an operating-system advisory record lock, not a PID file. A wrapper
acquires it and then execs the analyzer with the descriptor inherited. A lock
held only by a parent JVM is insufficient: killing that parent can release its
FileChannel lock while an analyzer child survives, permitting overlap. Stale
owner text is diagnostic only and cannot keep the lock held after its process
dies. Lock waiting and execution share one deadline so serialization cannot
double a request's maximum wall time.

The gate covers forward-reference analysis, binding analysis, MCP diagnostic
verification, MCP exact verification, and the candidate-bounded Var fallback.
A repository-owned direct-shell wrapper will use the same lock for agent lint
commands. An arbitrary invocation of the Homebrew binary by absolute path is an
explicit bypass; routing and installation can eliminate ordinary bypasses but
cannot make an advisory user-space lock mandatory for unrelated programs.

## Evidence and refusal

An admitted call records its PID and canonical command CWD in the lock file.
An admission timeout reports the requested wait, observed owner text when
available, and no analyzer child launch. MCP verification classifies admission
loss as unverified and preserves the existing rollback law. It must not turn a
resource refusal into an ordinary lint failure.

Under a fresh red or critical flight-recorder sample, or current normalized
one-minute load at the red threshold, new analyzer admission defers before
waiting, rechecks while waiting, and rechecks after lock acquisition. It
launches no analyzer and records a typed pressure receipt. Yellow still permits
the one admitted analyzer. The gate uses no `ps`, `top`, `vm_stat`, JVM probe,
or rich diagnostic.

Serialization alone does not stop a fast convoy from reusing one stale green
sample. The next independent ratchet is one-sample/one-launch debt for raw
shell work plus a closed mission lease for paved workflows. The first lease is
owned only by `analyzer-contract-test` and names its owner PID, canonical CWD,
scope hash, five-launch count, and five-minute budget. Every child releases the
physical lock. Every subsequent child requires a fresh pressure observation
taken after the prior child exits. An interactive waiter runs before the next
mission child, and pressure never yields to the lease.

## Permanent witnesses

1. Two threads in one JVM launching independent wrappers never overlap.
2. Two independent JVMs sharing one lock never overlap.
3. Killing the exec'd admitted analyzer releases the OS lock without deleting a stale
   lock file.
4. A bounded waiter expires without launching its analyzer and reports owner
   PID/CWD evidence.
5. The command deadline includes both lock wait and child execution.
6. Non-clj-kondo commands do not acquire the gate.
7. Exact-verifier admission loss remains unverified and rolls back staged
   source.
8. The installed direct-shell shim and every Surgeon process entrance use the
   same analyzer-lifetime wrapper and lock on macOS.
9. Red pressure before, during, or immediately after lock acquisition launches
   no analyzer and returns typed unavailable authority.
10. A source operation never reacquires analyzer authority after its first
    write.

## Test integrity without analyzer amplification

The initial static census found 29 real clj-kondo processes across 17 test
vars. The first zero-launch fast-runner proof found four additional CLI
integration/help entrances, correcting the complete-suite baseline to 33. The
target pyramid preserves provider integrity with five sequential real contract
launches while moving the behavior matrix to normalized fixtures:

1. one forward-reference/fix-plan end-to-end contract;
2. one binding-analysis schema contract;
3. one batched lint of all move candidates;
4. one real diagnostic baseline;
5. one real future-snapshot diagnostic verification.

Admission and cleanup tests continue to use fake analyzers. Pure plan,
diagnostic-delta, and rollback matrices consume frozen provider evidence. The
everyday runner therefore launches no real analyzer; the milestone contract
target remains mandatory and sequential.

## Verification order

- Pure and in-JVM admission tests first.
- Cross-process fake-analyzer tests on Anvil or under a low-load local gate.
- Focused MCP rollback tests.
- Changed-file formatter and clj-kondo only through the serialized entrance.
- Cold full suite only when normalized host load and memory admission permit.
