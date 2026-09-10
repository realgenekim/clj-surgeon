01. Baseline is bd124492bb557377afe94065ab58149110f21608 on fable/insert-forms; initial tree clean.
02. Read the entire external insert_forms contract before repository source discovery.
03. core.clj owns the CLI; there are no src/clj_surgeon/cli*.clj files at this baseline.
04. core/ops-registry holds handlers, argument descriptions, help and examples.
05. Existing :split-ns! delegates via requiring-resolve to namespace-split-io/cli!.
06. namespace-split-io/cli! reads :request-file with edn/read-string over slurp.
07. That reader is not sufficient for insert's bounded, exactly-one-value EDN contract.
08. core/parse-args parses key/value arguments and rejects repeated CLI keys.
09. Insert shorthand must be normalized before ordinary pair parsing.
10. core/run-op prints results; split has a dedicated receipt-text branch.
11. Insert needs its own state-first single-map rendering, without pprint line expansion.
12. core/-main currently exits 1 for returned errors; insert needs refusal exit 2.
13. Global and operation help derive from ops-registry and need both invocation examples.
14. mcp-tool/tools-for-profile supplies the catalog; all-tools chooses the runtime profile.
15. mcp-namespace-split/tool demonstrates standalone schema, handler Var and outcome classes.
16. mcp-schema.clj also owns shared operation schemas; insert needs one closed schema authority.
17. mcp-server/public-tool-registry requires outcome classes for every advertised tool.
18. mcp-server/sync-tools! compares contracts and updates live registration; do not call servers here.
19. mcp-operation/invoke! measures domain elapsed time and publishes summary plus structured data.
20. invoke! derives callback error from :ok; insert domain refusals need an ordinary-result adapter.
21. namespace-split-io publishes through mcp-extraction, with receipt-artifacts storage.
22. intent-transaction/build-receipt records guarded inverse edits and a receipt hash.
23. intent-transaction/commit-compiled! accepts injected read/write/create functions for fault tests.
24. commit-compiled! preflights whole-file hashes, writes, verifies hashes, and guards recovery.
25. file-ops/with-publish-lock* supplies a per-path JVM mutex plus advisory filesystem lock.
26. txn-journal/with-cooperating-writes is the documented cooperative-writer entrance; integration needs further inspection.
27. receipt-artifacts/default-artifact-root permits CLJ_SURGEON_ARTIFACT_ROOT; all build artifacts must stay under insert-fx.
28. lane-manifest/manifest, namespace :lane metadata and on-disk namespaces must agree.
29. lane-manifest-test/the-corpus-only-ever-grows-and-the-arithmetic-is-shown prints CENSUS_REGENERATE=1; read its resulting ledger diff.
30. Mandatory linked-intent-dev skill is missing; no tests or implementation have been written pending that prerequisite.
