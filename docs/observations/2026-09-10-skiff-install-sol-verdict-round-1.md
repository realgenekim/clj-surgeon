NO-GO

# Sol fence review: skiff install

- Reviewed sealed candidate: `3a3f2daa01615e01509aae1acccf23c4951115a4`
  (tip `3585c7e59880f64f53ce6d416ea4a6d3265fd84c`, base
  `3ea3803ea2e303a961a3edc1d76b8095ab5ff4f7`).
- Review completed: `2026-09-10T04:12:37Z`.
- Constraint observed: no `make test`. A mistaken direct `clojure -M` probe
  selected that alias's main and began a partial gate; its process group was
  terminated and none of its output is used as evidence below.

## Blocking findings

### SKIFF-INSTALL-FENCE-001 — darwin mount text can prove the wrong filesystem

`parse-darwin-mount-table` treats the last literal ` on ` as the device/mount
separator. A real mount point may contain those bytes. This executed table:

```text
/dev/root on / (apfs, local)
/dev/ram on /private/var/folders/evil on ram (tmpfs, local)
```

for target `/private/var/folders/evil on ram/T` returned `"apfs"`, not
`"tmpfs"` or `nil`: the malformed nested row was discarded and the root row
became positive proof of disk. Thus crafted mount text can make the ratchet
prove real disk, contrary to the required forged-to-refusal/unknown property.

There is a second authority break at the same boundary: `darwin-mount-fstype`
runs `mount` by bare name through `PATH`. Replacing that subprocess result with
`/dev/fake on / (apfs, local)` made `mount-fstype` return `"apfs"` and
`base-refusal` return `nil`. The claim that this is a non-redirectable system
source is false. The remedy needs an unforgeable system query and a parser that
cannot fall back past an ambiguous covering row.

### SKIFF-INSTALL-FENCE-002 — the derived suite lock has no repository key

`SUITE_LOCK` is
`$(SELF_TEST_TMP)/clj-surgeon-suite-$(id -u).lock`. Varying the checkout root
between `/var/tmp/checkout-a/` and `/var/tmp/checkout-b/` produced the same
path, `/var/tmp/forge/clj-surgeon-suite-1011.lock`. It is keyed only by scratch
root, the fixed product literal, and numeric UID. Distinct repositories owned
by the same user therefore collide, violating the requested rule that they
share only when repository identity says they are the same repository.

The repair belongs in `Makefile`; the ship contract explicitly makes a
reviewer patch touching `Makefile` a HOLD as `oracle-changed`, so this is not an
eligible `GO-WITH-FIX`.

### SKIFF-INSTALL-FENCE-003 — SWI-Prolog is not gate stage one

The install preflight does name missing `swipl` before writing anything and
prints both the Homebrew and Debian remedies. That part passes. The stronger
claim in the candidate does not: `make print-gate-stages` prints

```text
admit-transaction-recovery-battery
battery-fresh
alias-migration-test
mcp-test
test-bb
repository-hygiene
intent-audit
```

and `mcp-test-checks` (which invokes `mcp-operation-oracle`) is scheduled as a
phase-1 pool job after phase-0 MCP work. A caller who runs `make test` without
first running the installer can therefore pay earlier gate work before the
missing hard dependency refuses. Making the claimed ordering true also
requires gate/Makefile changes and is not eligible for reviewer self-repair.

## Requested checks

### (a) darwin mount parser

The shipped real-macOS fixture's focused test passes, including spaces and
longest-prefix selection. The adversarial delimiter and authority probes above
fail the required safety property; this is blocking finding 001.

### (b) pathname socket backend

The oracle passed all 13 rows under the default abstract backend and all 13
under forced `GATE_SLOT_BACKEND=path-socket`. With a live pathname holder,
deleting the root caused the original process to refuse with
`gate-refused: the gate root was replaced under a live holder`. A fresh,
independent coordinator then admitted against the recreated root. The same run
reported backend `path-socket` and receipt value `:path-socket`.

That is exactly the documented weaker guarantee: same-process replacement is
detected, while deletion plus an independent coordinator can split the
semaphore. The receipt does not overclaim the Linux guarantee.

### (c) `GATE_MEMAVAIL_MIB`

Both the Python admission process and Clojure coordinator were executed:

| value | result |
|---:|---|
| `0` | refuses as insufficient memory |
| `-1` | refuses (Python names negative; Clojure names insufficient memory) |
| `1000000000` | accepted as the explicit declaration; effective width clamps to `cpus/2` (8 on this seat) |

The huge declaration is not clamped in the receipt and is not checked against
physical memory. It does not exceed the CPU half-width cap, but the override is
trusted as operator authority rather than validated capacity.

### (d) derived lock path

Fails; see blocking finding 002.

### (e) Prolog refusal

With `swipl` removed from `PATH`, the install preflight printed
`MISSING -- make test cannot run (gate stage one)` plus
`brew install swi-prolog` and `apt install swi-prolog-nox`. A dry run of
`make install` showed the preflight command before every install recipe. The
preflight-before-write behavior passes; the claimed gate-stage ordering does
not, per finding 003.

### (f) Linux regression

The default Linux oracle passed 13/13. AST comparison with base `3ea3803e`
found `derived_width`, `hold_admission`, `try_acquire`, and `main` unchanged.
`capacity`, `slot_name`, and `claim` are not literally byte-for-byte because of
the portability dispatch and override. The Linux abstract name produced by old
and new code was byte-identical, and cross-version live holders excluded one
another in both directions. Thus the default abstract-socket admission
semantics are preserved, but a literal whole-path byte-identity claim would be
false.

## Positive install witness

A clean-prefix install under an empty temporary `HOME` completed with exit 0
in 9.18 seconds. `make check-agent-routing` then returned `:ok true` and
`:scope :installed`. The temporary prefix and all review-owned scratch roots
were removed.


> END RECEIPT (fence-run): worktree HEAD at review exit = 3a3f2daa01615e01509aae1acccf23c4951115a4 = fenced sha.
