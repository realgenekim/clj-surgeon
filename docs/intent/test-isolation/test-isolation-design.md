# JVM Test-Suite Isolation -- design note

## The cost this exists to remove

Every builder brief in this fleet carries "run mcp-test twice"; every
heartbeat caps the number of concurrent JVM suites on the box. That cap is
the cost, and Gene's filing is blunt about it: *"Unacceptable that we have to
gate parallelism because of tests!!!"*

## What round one measured, and what it changes

`clojure -M:clj-surgeon/mcp-test` was 716.7 s over 49 namespaces (865 tests,
13 023 assertions). **Eleven namespaces that launch cold JVM/bb/CLI children
are 674.0 s of that -- 94%** -- and one of them, `reader-eval-fence-test`, is
65% by itself. The other 36 finish 865 tests' worth of work in 20.9 s.

Two consequences follow, and they point in opposite directions from the
obvious reading:

1. **The suite is not slow.** A handful of subprocess batteries are. Making
   them fast is not available -- the whole point of the reader-eval fence
   witness is that a REAL, separately-launched launcher process does not
   evaluate a build file it discovers, so those JVM spawns are necessary and
   cannot be batched into one warm JVM. The spike's withdrawal clause
   partly applies.
2. **But the lane cap was never paid for by the tests as a body.** It was
   paid for by eleven namespaces plus two load-fragile ones. Round two's win
   is not making the batteries fast; it is letting the 20.9 s of real tests
   stop queueing behind them.

## The partition

| lane | rule | wall |
|---|---|---|
| `:fast` | No child process, no bind, no network, no read of the real `$HOME` or outside the run's own tmpdir subtree, no write into the working tree. | ~30 s cold |
| `:integration` | Binds an ephemeral port or drives a server in-process, or writes a per-test workspace into the repository root. Per-test unique resources; still no cold child JVM and still no network. | ~11 s |
| `:battery` | Launches a JVM, `bb`, a CLI, `clj-kondo`, `git` or `strace`; or measures the machine; or reaches the network. | ~674 s |

`make mcp-test` -- the merge gate -- is fast + integration. `make test` runs
the battery after.

**Network is a battery property, explicitly.** Round one's runtime sampler
caught `mcp-prepared-wire-test` spawning `clojure -X:clj-surgeon/mcp`, which
spawns `git remote-https origin https://github.com/bhauman/clojure-mcp`
through `~/.gitlibs`. No source scan of that namespace names a URL. A merge
gate whose wall depends on a remote host is not a merge gate.

## Why the manifest is the authority and the metadata is a cross-check

Three descriptions of the partition exist -- the manifest, each namespace's
own ns metadata, and the files on disk -- and the witness asserts set
equality among them in both directions. A manifest alone drifts silently: a
namespace deleted from it simply stops running and the suite goes GREEN with
less in it. This repository has already paid for that failure mode without
noticing: `mcp-formatter-test` was required by no runner and no Make target
-- three green tests over live production formatter paths that nothing ran.

Round two made it a DECLARED exclusion carrying that sentence as its reason,
which is the difference between a known gap and an accident. The round-two
review's answer was that a known gap is still a gap: declaring an omission
makes it visible, it does not make it non-loss. So round three does both
halves. The INSTANCE: the namespace is adopted into `:fast`, which it
qualifies for on the lane's own rules -- it injects its process runner, binds
nothing, and stages through `java.io.tmpdir`. The CLASS: an exclusion is now
a REDIRECTION, and `every-exclusion-names-a-runner-that-actually-exists`
requires each reason to name a `make <target>` or a `:clj-surgeon/<alias>`
that exists in the tree. A namespace can be sent to another runner or
deleted; it can no longer be declared into orphanhood.

## Why isolation is unrepresentability, not detection, where it can be

`java.io.tmpdir` and `user.home` are set as startup `-D` flags on a re-exec'd
child. The measured reason is recorded in `clj-surgeon.tmp-leak-support`'s
docstring: a runtime `System/setProperty` is NOT honoured for real file
creation on either bb or a real JVM, and the first cut of the temp ratchet
ran green for two whole suites while leaking thousands of directories into a
directory nobody was watching. A witness that watches the wrong place is
worse than no witness, because a false green terminates investigation.

## Why the race fixes are a class, not two instances

Round one named two load-fragile assertions. Running the two namespaces
two-wide behind a 12-way CPU burner found **six sites across four commits**,
and the last two inverted the lesson of the first two:

- A generous CEILING fixes "the other party was slow."
- It cannot fix "the other party had already left" -- an owner that held a
  lock for one second while the observer waited patiently on a 15 s ceiling.
  A rendezvous has to be WINNABLE, so the owner's HOLD is widened and the
  owner is killed once the observation is done.
- It cannot fix "one environment variable doing two opposite jobs" -- the
  same 100 ms admission timeout given to both the lock owner and the waiter,
  where it is the waiter's contract and the owner's race.

The general form: **a test that fails under load is not usually asserting
something too strict; it is usually asserting something about the machine
while believing it is asserting something about the code.**
