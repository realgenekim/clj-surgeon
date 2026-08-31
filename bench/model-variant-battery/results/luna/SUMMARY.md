# gpt-5.6-luna

| Decode condition | Oracle valid | Median wall | MAD | Median decoded |
|---|---:|---:|---:|---:|
| B: 1..200 | 5/5 | 10688.0 ms | 332.0 ms | 437.0 |
| C: ok | 5/5 | 3247.0 ms | 87.0 ms | 5.0 |

E2E decode rate: 40.9 tok/s
Bootstrap-subtracted decode rate: 58.1 tok/s (resolved=true)

| Fill | Exact | One-shot | Wrong subject | Schema fumbles | Parse fumbles | Median model wall | Median bang wall |
|---|---:|---:|---:|---:|---:|---:|---:|
| result | 5/6 | 5/6 | 0 | 0 | 1 | 1486.6 ms | 2001.5 ms |

Per-bang timing and normalized oracle decisions are in `score.json`.
