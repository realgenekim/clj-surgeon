# gpt-5.3-codex-spark

| Decode condition | Oracle valid | Median wall | MAD | Median decoded |
|---|---:|---:|---:|---:|
| B: 1..200 | 5/5 | 3486.0 ms | 293.0 ms | 533.0 |
| C: ok | 5/5 | 3404.0 ms | 253.0 ms | 103.0 |

E2E decode rate: 152.9 tok/s
Bootstrap-subtracted decode rate: 5243.9 tok/s (resolved=false)

| Fill | Exact | One-shot | Wrong subject | Schema fumbles | Parse fumbles | Median model wall | Median bang wall |
|---|---:|---:|---:|---:|---:|---:|---:|
| result | 6/6 | 6/6 | 0 | 0 | 0 | 921.9 ms | 1460.1 ms |

Per-bang timing and normalized oracle decisions are in `score.json`.
