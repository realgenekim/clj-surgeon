# Invalid attempt 1

This attempt is retained and is not scored. Every semantic gate passed except
the no-cue comparison because the first harness revision normalized the arm and
workspace path prefixes but did not exclude the per-run `receipt_hash` and
receipt UUID. The two normalized payloads therefore had different hashes:

- control: `280c8eff3070ea9463bcd857ae28472663d137c8b1d82d6ce287fa0157484dde`
- candidate: `f26b87267fd1dd9ae15900285b57e31091971b660db8f32ab7b74b38f9efdf9f`

The exact diff contained only `receipt_hash` and the UUID component of
`undo_receipt`. Commit `222df610` made those two dynamic fields explicit in the
protocol and normalizer. The complete experiment was then rerun into
`valid-run`; no value from this directory contributes to the reported result.
