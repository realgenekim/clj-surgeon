NO-GO

# Sol fence review, round 2: byte-parity harness `4b594926`

Reviewed sealed candidate `a094bb6817fee6dc03b9ba9de40034455605d57f`
(base `59d8bc0cad8886a028c24e8ffd52e4e5d82185c1`, branch tip
`4b594926b1d17e21d55c74e7c1e107d6a696906c`) at
`2026-09-10T17:15:26Z`. The round-1 controls now pass, but the assurance layer
is still not safe to ship: two fresh controls produce false `PARITY`.
Both repairs necessarily touch `bin/`, which the ship protocol classifies as
`HOLD reason=oracle-changed`; no self-authorizing repair was applied.

## Findings

### PARITY-FENCE-003 — CRITICAL — volatility provenance is not bound to a run

`bin/parity/compare.clj:42-44` proves only that a rule's `{:run :specimen
:path}` triple occurs in the checked-in `observed-volatility.edn`. The evidence
file carries neither a digest/attestation of the source capture nor a binding to
the compared stable-build commit and run artifacts. The comparator never
invokes or authenticates the generator. A hand edit can therefore invent both
the observation and the rule that cites it.

Fresh control: in a temporary copy I added an observation for
`RUN-THAT-NEVER-HAPPENED` at `[:candidate_hash]`, cited it from a new
`:candidate_hash` normalization, and planted a different candidate hash on B.
The comparator returned:

```text
PARITY
forged_rc=0
```

With the shipped declaration, the identical plant returned `DIVERGENCE 1`,
named `[:candidate_hash]`, and exited 1. The widening alone suppressed a real
content-hash difference. Evidence is retained at
`/var/tmp/forge/parity-fence-r2-PtgXYj`.

Required repair: bind every admitted observation to immutable source evidence
from an actual stable-vs-stable run (including the executor commit and hashes of
the captured sides), and validate that binding before accepting a rule. A
checked-in row asserting its own provenance is not sufficient.

### PARITY-FENCE-004 — HIGH — `:expects-receipt false` is one-way and can lie

`bin/parity/compare.clj:163-180` consults `:expects-receipt` only when
`receipt.edn` is absent on both sides. When the declaration says `false` but
both sides contain receipts, the comparator silently compares them and may
return parity. Thus the declaration does not enforce the claimed predicted
artifact state.

Fresh control: I compared the stored stable split pair, which contains
`receipt.edn` on both sides, while selecting the shipped `:fanout` specimen,
which declares `:expects-receipt false`. The comparator returned:

```text
PARITY
false_decl_rc=0
```

Required repair: presence on either side must diverge or refuse when the
selected specimen declares `:expects-receipt false`, with a self-test for the
two-sided-present case. Also retain a published `receipt_path` independently
of target-file existence: `bin/parity-run:120-122` currently writes
`receipt-path.txt` only when the named file exists, so its published-path
witness is not end-to-end.

## Round-1 controls and bounded verification

`bin/parity/self-test` passed 10/10. This re-ran both round-1 controls: an
undocumented/unobserved normalization refused with exit 2, and asymmetric
receipt presence diverged naming the missing side. The suite also covered all
new declared cases, but it has no forged-provenance control and treats
two-sided receipt presence under `:expects-receipt false` as untested.

The sealed delta `d1343a87..4b594926` changes exactly seven parity-harness
files: `bin/parity-run`, `bin/parity/compare.clj`,
`bin/parity/observe-volatility.clj`, `bin/parity/observed-volatility.edn`,
`bin/parity/self-test`, `bin/parity/specimens.edn`, and
`bin/parity/volatile-fields.edn`. No product source, build manifest, or other
repository file changed.

`git diff --check d1343a87..4b594926`, `bash -n` for the two shell entrances,
Python compilation of `mcp-call.py`, and EDN reads of all three declarations
passed. Per the brief, `make test` was not run.


> END RECEIPT (fence-run): worktree HEAD at review exit = a094bb6817fee6dc03b9ba9de40034455605d57f = fenced sha.
