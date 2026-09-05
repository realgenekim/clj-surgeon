# GO

Independent delta-only FENCE REVIEW round 7 of frozen `helper_extraction`
candidate `e1239a996b108a7d1e91c79b817513f9f19761f9`, completed at
`2026-09-05T10:12:45Z`.

**Finding 11 — CLOSED.** The throw path publishes the honest not-run face
`{status "unknown", profile "throwing-proof", ok false, fresh_process false,
reason ...}`. Draft 2020-12 validation accepts it only as
`verification-failed`; adding any of `structural_callers`, `helper_behaviors`,
or `compiled_callers` rejects it. An executed face relabelled `unknown` also
rejects. All 67 declared nested type mutations and all 10 nested constant
contradictions reject, including `closure.grammar`; all five declared
production-mapped faces validate against exactly their one named branch.

The stale mapper-copy defect is closed on two real `execute!` runs over
materialized fixtures. I added admitted root `dev` and one inward source
symlink, making the old result distinguishable. Both the real `/bin/true`
commit and injected `:run-proof!` throw published exactly:

```text
closure={roots ["src" "test" "dev"], authorized_paths ["src/**"],
         grammar "supported-libspecs-only",
         dynamic_references "not-claimed", pruned_symlinks 1}
```

The committed face validated only as `committed`; disk showed changed source
and a present destination. The throw face validated only as
`verification-failed`; disk showed the original source restored and no
destination.

Exactly two JVMs were used, both under `/var/tmp/forge/helper-fence-fx` with
`taskset -c 6-9 nice -n 10`; validation used Python-jsonschema 4.19.2's real
`Draft202012Validator`. No MCP server was started and no prohibited port was
contacted. The probe tree was deleted after filing.

**Overall boundary landing verdict: GO. Remaining item: none.**
