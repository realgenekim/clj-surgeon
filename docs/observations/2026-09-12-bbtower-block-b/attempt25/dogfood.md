# Source-edit dogfood ledger

The binding brief explicitly permits native apply_patch on exact Clojure forms.
The working-tree routing skill defaults this task to native. No Surgeon mutation
was attempted, so tool refusal and repair-text sufficiency are N/A throughout.

| File | Intent | Mechanism | Refusal / repair text |
|---|---|---|---|
| test/clj_surgeon/mcp_hot_verify_test.clj | Commit Sol's stale dev/experiments reproduction red; strengthen to actual reload, external count, exact roots; refuse outside/missing loaded dependency; give round-four fixtures an explicit image classpath | Native exact-form apply_patch; standard-clj formatter | N/A; test-authoring delimiter error repaired before red; later receipt scope error retained in focused-first.log |
| src/clj_surgeon/mcp_hot_verify.clj | Derive image roots, resolve require resources, classify jar/local/unresolved, carry discovery facts into receipts | Native exact-form apply_patch; standard-clj formatter | N/A; focused-second.log showed io/resource's default loader did not match require's bound loader; explicitly using RT/baseLoader repaired it |
| docs/intent/probe/refusals.edn | Register probe-dependency-unresolved and native false-green failure | Native exact-form apply_patch | N/A |
| test/clj_surgeon/mcp_alias_migration_test.clj | Pin the new reachable refusal, 163 → 164 kinds | Native exact-form apply_patch | N/A |
| test/clj_surgeon/lane_manifest_test.clj | Preserve existing sleep stimulus registration at its shifted line 497 | Native exact-form apply_patch | N/A; no new sleep or timing change |
| test/clj_surgeon/deftest_census.edn | Register exactly two new regression names, no removals | Declared CENSUS_REGENERATE=1 standalone test entrance | N/A; see census-registration.log |

Markdown intent/spec/tech-tree updates use native patches. Existing logs and the
pending battery row were preserved in ac001e20. No shared install was modified.
The regression probes, complete refusal registry witness, real image receipt
shape witness, and actual gates exercise the product under repair.
