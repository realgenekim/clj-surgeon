# Captain's log: the formatter was fast; `npx` was not

Date: 2026-08-29

## Outcome

The formatter phase contains a removable process-launch tax. On the frozen
Sessionize 15-owner extraction, a direct `standard-clj` process cut integrated
transaction wall by 878.747 ms, or 46.4%, while producing byte-identical source
and destination files.

This is a real kernel improvement, but it is not another complete-task
multiplier. Applied to the retained 19.216-second product route, the measured
kernel delta projects complete wall to 18.337 seconds: 4.6% lower and 6.67x the
122.278-second native baseline instead of 6.36x. That projection still requires
a clean-context product cohort before publication as measured complete wall.

## Paired integrated transaction

The serial order was `npx`, direct, direct, `npx`. Every run used a fresh copy of
the same fixture, one real `mcp-tool/execute-request!` extraction transaction,
the same 15 owners, one staged exact-exit verifier, and a 512 MiB JVM.

| Position | Formatter | Transaction wall | Correct source | Correct destination | Terminal verification |
|---:|---|---:|---|---|---|
| 1 | `npx ... fix` | 1,944.949 ms | yes | yes | yes |
| 2 | direct `standard-clj fix` | 1,045.438 ms | yes | yes | yes |
| 3 | direct `standard-clj fix` | 983.596 ms | yes | yes | yes |
| 4 | `npx ... fix` | 1,841.580 ms | yes | yes | yes |
| midpoint | `npx` | 1,893.264 ms | | | |
| midpoint | direct | 1,014.517 ms | | | |

Exact post-transaction hashes were identical in all four runs:

- source: `6ed498052c8a30531047b1d1c9bd23c609bc32355403e8412b7cfda178a5f822`
- destination: `bdaf9cdc5b748b22563c575d8a8278c3634ef8b44d2b187f4e23374ca9e9c0f1`

Raw retained evidence is under
`/private/tmp/clj-surgeon-formatter-abba.m1I8yr`. The reusable no-model driver is
`dev/experiments/formatter_process_canary.clj`.

## Safety evidence from the laptop gate

The first paired attempt used the real project clj-kondo exact verifier. The
machine-wide admission gate observed critical normalized pressure, launched no
analyzer, classified verification as unverified, and rolled both transactions
back to the exact original source. Those runs are safety evidence only; they are
not formatter timing samples. The gate was not bypassed.

The scored cohort therefore used `/bin/test -s <staged-target>` under the same
closed exact-exit transaction contract. Earlier retained product runs already
prove the real exact clj-kondo route. A later complete-wall publication cohort
must run the project verifier when pressure is green.

## Independent review

SURGEON2 independently reproduced the direct integrated extraction and audited
the packaging boundary. Its superseding immutable receipt is commit
`bb4bb3eb47eb8e3d8d6903390cc64970b446ad19`, with observation SHA-256
`ac71bca9e7e136e096ae22531005cdb6a671b121c5b4ea8221b81a4d0a8836fc`.

The review agrees with the performance result and rejects the tempting unsafe
implementation. `/opt/homebrew/bin/standard-clj` is a machine-global v0.24.0
installation, not a repository-owned dependency. Hard-coding it would trade
about one second for an undeclared, non-portable runtime contract.

## Decision

GO:

- retain the repository-owned exact-version direct formatter seam;
- bind package version, lockfile, Node, executable, and argv before transaction
  execution;
- batch all staged files through one direct process; and
- retain the current formatter rollback and verification laws.

NO-GO:

- no Homebrew path in the product default;
- no unversioned executable selected from `PATH`;
- no formatter daemon; process startup is already near 100 ms; and
- no 6.67x product claim until measured end to end.

The product-owned default crosses Linked Intent because it changes startup,
installation, and transaction evidence. Its HLD is now recorded; LLD, EARS,
tests, and product code wait for design approval. This preserves the measured
win without letting a benchmark shortcut become architecture.
