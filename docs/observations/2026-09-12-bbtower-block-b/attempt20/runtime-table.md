# Six-run namespace controls

| Namespace | JVM n | JVM mean ms | JVM sd ms | bb n | bb mean ms | bb sd ms | Conservative ratio | Before | After |
|---|---:|---:|---:|---:|---:|---:|---:|---|---|
| clj-surgeon.battery-ledger-test | 6 | 38.166667 | 1.834848 | 6 | 6.333333 | 0.816497 | 0.230928290 | :bb | :bb |
| clj-surgeon.battery-parallel-test | 6 | 983.666667 | 24.426761 | 6 | 35.166667 | 1.940790 | 0.041771179 | :bb | :bb |
| clj-surgeon.fast-lane-isolation-test | 6 | 130.333333 | 3.265986 | 6 | 46.500000 | 1.224745 | 0.395387332 | :bb | :bb |
| clj-surgeon.helper-extraction-test | 6 | 812.000000 | 31.968735 | 6 | 818.000000 | 16.358484 | 1.137227081 | :bb | :bb |
| clj-surgeon.insert-forms-test | 6 | 1386.333333 | 52.106302 | 6 | 5175.333333 | 101.962084 | 4.195593579 | :jvm | :jvm |
| clj-surgeon.intent-transaction-test | 6 | 4964.666667 | 67.742650 | 6 | 5084.000000 | 47.099894 | 1.072272792 | :jvm | :jvm |
| clj-surgeon.lane-manifest-test | 6 | 1243.000000 | 57.955155 | 6 | 1306.000000 | 23.706539 | 1.200803353 | :bb | :bb |
| clj-surgeon.mcp-compact-edit-fields-test | 6 | 31.166667 | 2.639444 | 6 | 6.666667 | 0.816497 | 0.320601477 | :bb | :bb |
| clj-surgeon.mcp-contract-test | 6 | 73.833333 | 4.665476 | 6 | 13.000000 | 1.095445 | 0.235508983 | :bb | :bb |
| clj-surgeon.mcp-extraction-test | 6 | 137.333333 | 5.819507 | 6 | 48.833333 | 1.169045 | 0.407110077 | :bb | :bb |
| clj-surgeon.mcp-feature-thread-test | 6 | 42776.000000 | 174.068952 | 6 | 725903.833333 | 538.477267 | 17.134513783 | :jvm | :jvm |
| clj-surgeon.mcp-inspect-contract-test | 6 | 309.333333 | 11.465891 | 6 | 307.666667 | 12.420413 | 1.160983566 | :bb | :bb |
| clj-surgeon.mcp-intent-contract-test | 6 | 296.833333 | 14.851487 | 6 | 252.166667 | 9.217737 | 1.012996578 | :bb | :bb |
| clj-surgeon.mcp-paths-test | 6 | 15.500000 | 1.378405 | 6 | 2.000000 | 0.000000 | 0.156946570 | :bb | :bb |
| clj-surgeon.mcp-program-tool-test | 6 | 149.833333 | 14.688998 | 6 | 19.500000 | 1.516575 | 0.187066433 | :bb | :bb |
| clj-surgeon.mcp-read-request-normalization-test | 6 | 30.000000 | 2.756810 | 6 | 3.500000 | 0.836660 | 0.211273367 | :bb | :bb |
| clj-surgeon.mcp-recovery-test | 6 | 165.166667 | 5.492419 | 6 | 6.166667 | 0.408248 | 0.045291740 | :bb | :bb |
| clj-surgeon.mcp-schema-test | 6 | 33.000000 | 1.414214 | 6 | 9.666667 | 0.516398 | 0.354620631 | :bb | :bb |
| clj-surgeon.mcp-telemetry-test | 6 | 37.500000 | 1.224745 | 6 | 6.333333 | 0.516398 | 0.210157537 | :bb | :bb |
| clj-surgeon.mcp-workspace-test | 6 | 29.833333 | 1.940790 | 6 | 16.166667 | 1.602082 | 0.746417042 | :bb | :bb |
| clj-surgeon.mission-candidate-test | 6 | 17.833333 | 1.471960 | 6 | 3.000000 | 0.000000 | 0.201485444 | :bb | :bb |
| clj-surgeon.mission-forms-source-test | 6 | 361.500000 | 22.034065 | 6 | 29.000000 | 1.788854 | 0.102628979 | :bb | :bb |
| clj-surgeon.mission-forms-test | 6 | 38.500000 | 1.224745 | 6 | 6.000000 | 0.000000 | 0.166433151 | :bb | :bb |
| clj-surgeon.mission-git-test | 6 | 10.166667 | 1.940790 | 6 | 1.000000 | 0.000000 | 0.159106807 | :bb | :bb |
| clj-surgeon.mission-plain-forms-test | 6 | 88.333333 | 5.163978 | 6 | 44.166667 | 1.722401 | 0.610361374 | :bb | :bb |
| clj-surgeon.mission-typist-test | 6 | 53.000000 | 3.741657 | 6 | 21.333333 | 1.861899 | 0.550504294 | :bb | :bb |
| clj-surgeon.mission-usage-test | 6 | 54.833333 | 4.070217 | 6 | 14.000000 | 0.000000 | 0.299831457 | :bb | :bb |
| clj-surgeon.namespace-split-test | 6 | 1132.500000 | 50.646816 | 6 | 754.666667 | 3.829708 | 0.739256571 | :bb | :bb |
| clj-surgeon.outline-corpus-integration-test | 6 | 8037.333333 | 175.651549 | 6 | 20817.000000 | 125.067982 | 2.740964492 | :jvm | :jvm |
| clj-surgeon.outline-differential-test | 6 | 54.333333 | 4.589844 | 6 | 16.166667 | 0.408248 | 0.376119426 | :bb | :bb |
| clj-surgeon.quoted-var-refs-test | 6 | 88.833333 | 5.492419 | 6 | 10.500000 | 0.547723 | 0.148948866 | :bb | :bb |
| clj-surgeon.rename-alias-receipt-test | 6 | 496.500000 | 26.912822 | 6 | 1802.500000 | 29.971653 | 4.207253669 | :jvm | :jvm |
| clj-surgeon.rename-alias-test | 6 | 3203.000000 | 150.905268 | 6 | 6265.333333 | 74.995111 | 2.211273561 | :bb | :jvm |
| clj-surgeon.require-change-test | 6 | 155.833333 | 6.675827 | 6 | 43.500000 | 1.870829 | 0.331563031 | :bb | :bb |
| clj-surgeon.splice-envelope-test | 6 | 752.833333 | 12.812754 | 6 | 27577.166667 | 318.774790 | 38.798697248 | :jvm | :jvm |
| clj-surgeon.split-proof-gate-test | 6 | 28.833333 | 3.125167 | 6 | 6.833333 | 1.602082 | 0.444471385 | :bb | :bb |
| clj-surgeon.telemetry-events-test | 6 | 151.000000 | 12.016655 | 6 | 55.333333 | 2.422120 | 0.473963478 | :bb | :bb |
| clj-surgeon.workspace-onboarding-test | 6 | 167.500000 | 5.683309 | 6 | 112.666667 | 0.816497 | 0.732064202 | :bb | :bb |
