# Spike: the JVM test suite — speed, purity, isolation (filed 2026-09-04, Gene)

Gene, verbatim: *"I think a spike to clean up JVM test suite and speed it up and ensure pure and at least tests that
don't interfere with each other is definitely warranted. Unacceptable that we have to gate parallelism because of tests!!!"*

(bd has no database on this seat; this file on MCP/main is the durable record and its commit sha the id.)

## Evidence from 2026-09-04
- The census lane's reader-eval fence test: 466 s of a 742 s `mcp-test` lane (63%), six cold JVM launcher drives.
- One JVM launcher drive: 65 s at load 7, ~4 min at load 12 (the census builder's own measurement).
- Two "unreproduced failures under load" disclosures (feature_thread 3, study-ops 6); reviewers re-ran 0/10 and 0/2.
- Landing gates on the census merge: 17 min wall for mcp-test + bb + oracle + hygiene + battery.
- The admit gate's battery receipt lives in `target/`, shared by every lane that runs from the same checkout.
- /var/tmp/forge leaked thousands of suite temp dirs before the tmp-leak ratchet landed; /tmp filled twice (inodes).
- Every builder brief now carries "run mcp-test twice"; every heartbeat caps concurrent JVM suites. That cap is the cost.

## Round 1 — measure and classify (read-only; deliverable is a table + partition proposal)
1. Per-namespace wall for `clojure -M:clj-surgeon/mcp-test` (a timing reporter around `clojure.test/test-ns`), quiet box.
2. Classify every test namespace: pure / temp-filesystem / spawns a JVM (ProcessBuilder, sh, `make`) / binds a port (fixed
   vs 0) / touches `target/`, `~/.local/state`, `$HOME`, `.cpcache` / mutates a global atom or `alter-var-root` / sleeps.
3. Interference hunt: run two copies of the suite concurrently from two clones of the same commit; diff their failures
   against a solo run. Then four. Name every colliding pair and its shared resource.
4. Proposal: fast lane (pure, N-wide safe, target < 60 s quiet), integration lane (fs/ports, per-test unique resources),
   battery lane (JVM spawns, resource proofs). Which namespaces move where; what each move costs.

## Round 2 — implement with a ratchet
- The partition as Makefile targets and deps.edn aliases; `make test` = fast then integration then batteries.
- The ratchet: a test in the fast lane that FAILS if any fast-lane namespace spawns a JVM, binds a fixed port, writes
  outside its own `java.io.tmpdir` subdir, or reads `target/` — a source-scanning witness plus a runtime witness
  (a temp-root diff and a port ledger), the same class as the tmp-leak ratchet.
- Proof: four fast-lane suites concurrently on Anvil, 0 failures, three times.

## Withdrawal
If round 1 shows the wall is dominated by genuinely necessary JVM spawns that cannot be batched into one warm JVM, the
spike reports that number and stops; the lane cap stays and is documented as a cost, not a gate.

## The LID family for test isolation — TEST-ISO-0xx (Gene, 2026-09-04: "We need LID rules on our tests to make sure they don't interfere!!!!")

Every rule = a linked intent (stable id, fail-first witness, INTENT:/INTENT-TEST: links per the linked-intent-testing
skill); where a mechanism can make the bad state unrepresentable, that rung is taken over detection.

| id | rule | witness / mechanism | rung |
|---|---|---|---|
| TEST-ISO-001 | every test ns declares `:lane :fast/:integration/:battery` in ns metadata | runner refuses an undeclared ns; registry audit = set equality both directions (the refusal-kind pattern) | unrepresentable |
| TEST-ISO-002 | a fast-lane test spawns no process | `ProcessHandle.current().descendants()` before/after each ns; source scan (ProcessBuilder, sh, make) corroborates | runtime witness |
| TEST-ISO-003 | a fast-lane test writes only inside its own tmpdir subdir | per-ns tmpdir fixture; diff of tmp root, `target/`, `git status --porcelain` before/after; fails naming the path | runtime witness |
| TEST-ISO-004 | no fixed ports | source scan for port literals; listener diff before/after each ns; every server start through the port-0 allocator with a ledger | witness + allocator |
| TEST-ISO-005 | no global mutation leaks | snapshot every project var root + known global atoms before/after each ns; a var left altered fails naming it (catches the leaked with-redefs) | runtime witness |
| TEST-ISO-006 | no home / state / telemetry reads in the fast lane | fast-lane JVM started with `user.home` and `java.io.tmpdir` on a throwaway dir (seat guard already refuses tmpfs) | unrepresentable |
| TEST-ISO-007 | time budget per ns and per lane (fast lane < 60 s quiet) | the timing reporter fails a ns over budget with its wall | runtime witness |
| TEST-ISO-008 | order independence | shuffled ns order with a printed seed, two seeds per gate; one-seed failure = interference, seed reproduces it | runner |
| TEST-ISO-009 | concurrency proof | battery: N clones run the fast lane concurrently, 0 failures, three times — the spike's merge gate and the nightly | battery |
| TEST-ISO-010 | no thread/executor leaks | live non-daemon thread count before/after each ns | runtime witness |
| TEST-ISO-011 | no sleeps/polls in the fast lane | source scan for Thread/sleep outside the one sanctioned wait helper | source witness |
| TEST-ISO-012 | no two fast-lane nss share a mutable resource | declared resources per ns; relational oracle — KEPT ONLY if it finds a counterexample the native witnesses missed | oracle |

One fixture hosts 002/003/004/005/006/010 (a before/after snapshot per namespace) — one mechanism, six witnesses.
001 + 007 make the lane manifest an intent registry: moving a ns needs a reason at the pin.

Cherry-pick: ROUND TWO builds 001–007 and 009. FILED: 008 (runner must own ordering), 010, 011, 012 (only if it finds one).
