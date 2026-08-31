# gpt-5.6-sol

| Decode condition | Oracle valid | Median wall | MAD | Median decoded |
|---|---:|---:|---:|---:|
| B: 1..200 | 5/5 | 10739.0 ms | 270.0 ms | 403.0 |
| C: ok | 5/5 | 3884.0 ms | 474.0 ms | 5.0 |

E2E decode rate: 37.5 tok/s
Bootstrap-subtracted decode rate: 58.1 tok/s (resolved=true)

| Fill | Exact | One-shot | Wrong subject | Schema fumbles | Parse fumbles | Median model wall | Median bang wall |
|---|---:|---:|---:|---:|---:|---:|---:|
| result | 6/6 | 6/6 | 0 | 0 | 0 | 3051.0 ms | 3741.9 ms |

Per-bang timing and normalized oracle decisions are in `score.json`.
