# gpt-5.6-terra

| Decode condition | Oracle valid | Median wall | MAD | Median decoded |
|---|---:|---:|---:|---:|
| B: 1..200 | 0/5 | n/a ms | n/a ms | n/a |
| C: ok | 5/5 | 3466.0 ms | 414.0 ms | 5.0 |

E2E decode rate: n/a tok/s
Bootstrap-subtracted decode rate: n/a tok/s (resolved=false)

| Fill | Exact | One-shot | Wrong subject | Schema fumbles | Parse fumbles | Median model wall | Median bang wall |
|---|---:|---:|---:|---:|---:|---:|---:|
| result | 6/6 | 6/6 | 0 | 0 | 0 | 1557.8 ms | 2258.1 ms |

Per-bang timing and normalized oracle decisions are in `score.json`.
