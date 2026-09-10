NO-GO

# Sol fence review: byte-parity harness `d1343a87`

Reviewed sealed candidate `cb2f7ad10c0d62b313873eb3ded0753053d075ed`
(base `59d8bc0cad8886a028c24e8ffd52e4e5d82185c1`, branch tip
`d1343a8714d556f7bf5d6834044bc657d7ea891b`). The candidate is not safe to ship:
two comparator controls produce false `PARITY`. Both repairs necessarily touch
`bin/`, which the ship protocol classifies as `HOLD reason=oracle-changed`; no
self-authorizing fix was applied.

## Findings

### PARITY-FENCE-001 — HIGH — the volatile declaration can be widened silently

`bin/parity/compare.clj` consumes every `:fields` rule without validating that
the rule has the required physical `:reason` and observed `:evidence`. There is
no executable witness for the stated anti-widening contract.

Fresh control: I added `{:key :unobserved_guard :match :key :to
"<UNOBSERVED>"}` to a temporary copy of the declaration, deliberately omitting
both `:reason` and `:evidence`, and compared receipts whose only difference was
that field. The comparator returned:

```text
PARITY
widen_rc=0
```

Evidence is retained at `/var/tmp/forge/parity-fence-JIL4EU`. This is the
brief's explicit required-fix condition. The repair needs a declaration
validator plus a red witness proving that an unobserved field cannot suppress a
difference. The same gate should cover tree exclusions; five of the six current
`:tree-excludes` have no `:evidence` member.

### PARITY-FENCE-002 — CRITICAL — a receipt file missing on one side is accepted

The on-disk receipt comparison is guarded by `(and exists-A exists-B)` at
`bin/parity/compare.clj:120`. If exactly one build fails to publish the receipt
file, comparison is skipped. Because `.clj-surgeon/**` is also excluded from
the tree manifest, no other layer necessarily observes the missing artifact.

Fresh control: from the stored green split pair, I moved only build B's
captured `receipt.edn` aside and replayed the comparator. It returned:

```text
PARITY
one-sided-receipt-rc=0
```

Evidence is retained at `/var/tmp/forge/parity-fence-presence-UGzWW3`. The
comparator must treat asymmetric receipt presence as divergence (and should
also refuse when a published receipt path names a file that was not captured).

## Requested checks

1. **Hash normalization:** `:receipt_hash` is declared volatile because it
   covers a receipt containing per-run identity and path data.
   `:map_hash`, `:snapshot_hash`, and `:candidate_hash` are absent from every
   normalization rule. A one-character plant in `:candidate_hash` returned
   `DIVERGENCE 1` and named `[:candidate_hash]` with both values
   (`/var/tmp/forge/parity-fence-JIL4EU`). PASS.
2. **Anti-widening witness:** a field with no observation data was accepted and
   hid a real difference. FAIL, `PARITY-FENCE-001`.
3. **Leaked-server guard:** I started an independent listener on 7951, assigned
   both harness slots to that port, and ran the alias specimen. Both slots
   logged `REFUSING ... port 7951 is ALREADY IN USE`, recorded
   `server-unavailable`, and did not call the listener. The run was non-green.
   Evidence: `/var/tmp/forge/parity-fence-port/runs/OCCUPIED-PORT-7951`. PASS.
4. **Build isolation:** a fresh same-build alias run started build-local MCP
   processes on 7953/7954 and returned parity. A strace-free `/proc` probe of
   the identical launch path showed cwd
   `/home/forge/src/clj-surgeon-fence`, Java from the system JDK, the requested
   private port in argv, and no argv reference to `~/bin/clj-surgeon`, its
   installed version store, or 7906. `clojure`, `bb`, and `python3` resolved to
   `/usr/local/bin/clojure`, `/usr/local/bin/bb`, and `/usr/bin/python3`.
   The unrelated 7906 listener remained PID 2930298. The child inherits a PATH
   containing `~/bin`, but the harness has no bare `clj-surgeon` execution and
   no URL targeting 7906. Evidence:
   `/var/tmp/forge/parity-fence-isolation/runs/CHILD-ENV`. PASS for the named
   reachability check.
5. **Port policy:** defaults are 7951/7952. The shell guard refuses
   7888, 7890, 7894, 7895, and the complete 8300--8339 range; boundary probes
   for 8300, 8319, and 8339 all selected `REFUSE`. PASS.

`bash -n bin/parity-run`, Python AST parsing of `bin/parity/mcp-call.py`, and
EDN parsing/counting of the 14 field rules and six tree exclusions also passed.
Per the brief, `make test` was not run.


> END RECEIPT (fence-run): worktree HEAD at review exit = cb2f7ad10c0d62b313873eb3ded0753053d075ed = fenced sha.
