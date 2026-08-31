# Prepared-request eligibility characterization: fourth independent audit

## Verdict

GO for test cargo at exact candidate
`5573f3a803c0b0061e0532dd2944ed797b7bf8c2` (tree recorded below).

This audit is verification-only. It does not authorize product changes,
installation, reload, or shared-runtime mutation.

## Immutable identity

- Branch under audit: `test/eligibility-characterization-20260831`
- Candidate: `5573f3a803c0b0061e0532dd2944ed797b7bf8c2`
- Candidate tree: `0bed869d13ccd6cbd54798dc2e202272b6a1d627`
- Clean detached worktree:
  `/private/tmp/clj-surgeon-eligibility-fourth-audit.Z0OHMf/worktree`
- Test source SHA-256:
  `77d3489e83f7964a574aff3af8d110e70e9191e4cb826e97402912a5f7882ddc`
- Runner source SHA-256:
  `8844e1a11c110733f8a8ee5ff668625d37c2008e8587b00384a4f94e9f86fba8`

## Independent evidence

### Fresh-JVM focused gate

The candidate namespace was required from its declared test classpath in a
new 512 MiB JVM and run directly:

```text
Ran 14 tests containing 26 assertions.
0 failures, 0 errors.
{:test 14, :pass 26, :fail 0, :error 0}
```

This reproduces the candidate's corrected count without REPL residue.

### Prior seven-form false green is closed

The general agreement witness now contains the base plus all twelve refusal
mutations, including the internally consistent seven-form boundary. An
adversarial redefinition changed only the explainer's seven-form result to
`:eligible? true` while leaving product behavior unchanged. The exact witness
failed:

```text
{:corruption :seven-form-reported-eligible,
 :result {:test 1, :pass 12, :fail 1, :error 0}}
```

The previous false green therefore cannot recur.

### Hermetic loading and runner registration

- `clj-surgeon.eligibility-explainer` is loaded through the candidate's test
  classpath; the fresh-JVM gate proves there is no `/tmp` dependency.
- The repaired test namespace uses an ns-form require; no runtime `require`,
  alias collision, or `resolve` dance remains.
- Structural matching found exactly two runner registrations for
  `clj-surgeon.eligibility-characterization-test`: one in the namespace
  require and one in the `run-tests` call.

## Scope conclusion

All four blockers from the first three audits are closed:

1. single-axis refusal fixtures are pinned;
2. the test is registered in the real runner;
3. exact fresh-JVM counts are truthful;
4. the candidate-local explainer agrees with product truth across every
   characterized row, and the formerly omitted seven-form row now has an
   executable falsifier.

The suite is suitable to ride the next release train as test-only cargo.
