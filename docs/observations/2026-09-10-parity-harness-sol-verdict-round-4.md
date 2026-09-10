NO-GO

# Sol fence review, round 4: byte-parity harness `b204521f`

Reviewed sealed candidate `ae0c16937c9ab4e96e8bd0ba9d2fca7eba2a9f44`
(base `13635f748c58767aa37d261ebb98b9e0a03465f5`, branch tip
`b204521fa09a0f0bc1689fc470eaf4e543697c21`) on 2026-09-10. The repairs
close PARITY-FENCE-005 and PARITY-FENCE-007, and most of PARITY-FENCE-006,
but one uncited retained capture can still carry provenance that contradicts the
authenticated evidence document while the comparator returns `PARITY`.

The required repair touches `bin/parity/compare.clj` and its witness under
`bin/parity/self-test`. The ship protocol classifies a reviewer patch touching
`bin/` as `HOLD reason=oracle-changed`, so no self-authorizing implementation
repair was applied.

## Finding

### PARITY-FENCE-006 — HIGH — provenance is checked only for captures cited by surviving rules

`validate-declaration` calls `verify-capture` only while validating a declaration
rule. The nine-rule declaration cites split and alias observations, but no rule
cites fanout. Consequently the retained
`evidence/OBS3-commit-bound/fanout/capture.edn` is never authenticated, contrary
to the repair contract that every capture name the same source commit, generator,
and stable build as the authenticated document.

Fresh control: I copied the complete OBS3 evidence tree, changed only the fanout
capture metadata to:

```clojure
{:source-commit "NOT-A-COMMIT"
 :generator-sha256 "FORGED"
 :stable-build "NOT-A-COMMIT"}
```

and compared the stored stable split pair with the shipped declaration. The
comparator returned:

```text
PARITY
rc=0
```

Evidence is retained at
`/var/tmp/forge/parity-fence-r4-uncited.XtUcVz`. This is not a hypothetical
extra file: fanout is one of the authenticated document's observed specimens,
its capture is checked in, and the generated observation rows name its capture
digest.

Required repair: enumerate every distinct capture named by the authenticated
observation document (or every retained capture under the declared evidence
root), require its file/digest integrity and its `:source-commit`,
`:generator-sha256`, and `:stable-build` equality before authorizing any
declaration, and add this exact uncited-capture control to `bin/parity/self-test`.

## Requested controls and verification

- `bin/parity/self-test` passed all 28 witnesses. This reran the round-3
  laundering, generator-rebound/NOT-A-COMMIT, and child-PATH controls.
- A planted `:candidate_hash` in both B receipt faces produced `DIVERGENCE 2`,
  naming `[:candidate_hash]` in each. `:map_hash`, `:snapshot_hash`, and
  `:candidate_hash` are absent from the volatile selectors; only
  `:receipt_hash` is declared volatile. Evidence:
  `/var/tmp/forge/parity-fence-r4-hash.7vzmi1`.
- A synthetic retained difference requiring two regex rules returned `PARITY`
  with both rules. Removing either rule returned `REFUSED 1` because the whole
  remaining declaration no longer removed `[:joint]`. That is the correct
  necessary-and-sufficient result: both rules are individually necessary and
  jointly sufficient. Evidence:
  `/var/tmp/forge/parity-fence-r4-joint2.JNg7BZ`.
- With a server I started on 7951 and both harness slots assigned to that port,
  both sides refused before launching a harness server and the run ended
  `alias REFUSED`, exit 2. Evidence:
  `/var/tmp/forge/parity-fence-r4-port/runs/occupied-port-control`.
- A live stable/stable alias run on 7971/7972 returned `PARITY`. While its Java
  child ran, `/proc/<pid>/cwd` was the isolated archived build, its actual
  `PATH` was `/var/tmp/forge/parity-fence-r4-live/childbin:/usr/bin:/bin`, no
  relevant inherited environment named clj-surgeon or an MCP endpoint, and the
  independent server on 7906 remained reachable. Both private ports were free
  after teardown. Evidence:
  `/var/tmp/forge/parity-fence-r4-live/runs/live-env-control`.
- Defaults remain 7951/7952. The guard rejects exactly 7888, 7890, 7894, 7895,
  and `83[0-3][0-9]` (8300–8339) before launch.
- The requested `0d58fda2..b204521f` delta contains 30 paths, all under
  `bin/parity-run` or `bin/parity/**`; no unrelated path changed. Shell syntax,
  Python parsing, and all three EDN declarations passed. `git diff --check`
  reports one non-semantic trailing space at `bin/parity-run:73`.

Per the brief, `make test` was not run.


> END RECEIPT (fence-run): worktree HEAD at review exit = ae0c16937c9ab4e96e8bd0ba9d2fca7eba2a9f44 = fenced sha.
