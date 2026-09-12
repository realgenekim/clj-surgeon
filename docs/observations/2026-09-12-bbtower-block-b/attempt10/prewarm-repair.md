# One prewarm repair

Prewarm 1 caught outline-corpus-integration-test at 21,033 ms under bb,
over the existing 20,000 ms namespace ceiling. Alias and bb suites passed;
the mcp suite refused. Evidence: prewarm-1.log and prewarm-1/mcp/receipt.edn.

The isolated follow-up measurements, executed sequentially with the same
1 GiB cap and temp base, record 20,859 ms under bb and 8,306 ms under JVM.
Receipts: outline-bb.edn and outline-jvm.edn. The ratio is about 2.5113,
so TEST-ISO-016's existing measured-runtime rule assigns JVM. Cadence stays
integration; the per-namespace and lane budgets remain unchanged. This is
the only prewarm repair; prewarm 2 is the final permitted attempt.

The ceiling calibration run did not contain this integration namespace.
Replaying its facts with the shipped runtime map still gives 240,989 bb ms,
38,646 fast ms and a 374,149 ms ceiling. The replay asserts runtime agreement
for every measured member; it does not pretend unrelated map entries froze.
