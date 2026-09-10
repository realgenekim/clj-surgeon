GO-WITH-FIX

# Sol fence review — fable/skiff-fixes 32678de6

Reviewed sealed candidate `3266d0f8106f4f63f6f1c8aa9f0ec0d7838698bb`
against base `59d8bc0cad8886a028c24e8ffd52e4e5d82185c1`.

## Required finding

`SKF-001` — the shared artifact-boundary witness used `getCanonicalFile`, so a
nonexistent artifact whose lexical name was under the expected receipt root was
accepted as published. The direct probe returned `true`; this fails open and did
not meet the brief's `toRealPath`/nonexistent-path check.

The uncommitted repair makes `published-under-root?` apply `toRealPath` to both
the artifact and declared root before comparing their canonical forms. Resolution
failure is now a typed `:artifact-path-unresolvable` refusal carrying the path and
cause. A focused regression pins the nonexistent-artifact case without weakening
the existing symlink and genuine-escape checks.

## Verification

- Darwin memory witness: fake `vm_stat`/`sysctl` produced 6718 MiB from both the
  gate reader and preflight; missing tools produced the same typed `vm_stat`
  refusal. The 3584 MiB floor and formula appeared in preflight and refusal.
- Babashka process call: `(apply proc/process opts argv)` executed successfully on
  exact bb 1.12.209 and installed bb 1.13.219. Exact bb 1.12.209 loaded all 98
  reachable namespaces with 0 unloadable, and its five JVM-error tests passed.
- Socket budget: the longest bound leaf is `.pending-` plus 32 hex characters =
  41 bytes. A synthetic 60-digit uid computed a 111-byte worst case; an oversized
  root override refused before bind with the path length, 100-byte budget,
  104-byte Darwin cap, and override remedy. The socket oracle passed 20 tests
  (2 platform skips).
- Canonicalization: every migrated assertion routes through the shared helper;
  the focused symlink, escape, and missing-path witness passed after the repair.
- Linux: `test/gate_memory_one_reader_test.sh`, the three focused memory tests,
  the focused artifact-boundary test, and the socket oracle passed. Clojure lint
  reported 0 errors and 0 warnings. Per brief, `make test` was not run.

## Advisory behavior observed

Babashka's `:min-bb-version` does not refuse an older runtime: a deliberately
future minimum emitted a warning, executed the body, and exited 0. The install
preflight separately names an older version as a gate blocker but also exits 0.
Thus 1.12.209 is an advertised/measured floor, not runtime enforcement.

<!-- SHIP-FIX-BLOCK
CANDIDATE: 3266d0f8106f4f63f6f1c8aa9f0ec0d7838698bb
FINDINGS: SKF-001
REPAIR: Require real artifact and receipt-root paths on both sides of the canonical boundary comparison and type unresolved paths.
PRODUCER: OpenAI Codex <codex@openai.com> model=gpt-5 session=skiff-fixes-fence-3266d0f8
PATCH-MANIFEST:
  docs/observations/skiff-fixes-fence.md +56 -0
  test/clj_surgeon/artifact_boundary_support.clj +18 -0
  test/clj_surgeon/receipt_artifacts_boundary_test.clj +10 -0
SHIP-FIX-BLOCK -->


> END RECEIPT (fence-run): worktree HEAD at review exit = 3266d0f8106f4f63f6f1c8aa9f0ec0d7838698bb = fenced sha.
