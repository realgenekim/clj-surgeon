# Probe contract: not green

Subject: `978a0c485ea4d23504da4105f7572614268b4335`.

Read-only inspection cannot supply the requested green witness list:

- `git ls-files '*refusals.edn'` lists only
  `docs/intent/insert-forms/refusals.edn` and
  `docs/intent/rename-alias/refusals.edn`. No probe refusal registry or its
  native-failure completeness witness is present at this subject.
- `docs/intent/receipt-booleans/registry.edn` registers only `insert_forms`
  and `rename_alias`. `test/clj_surgeon/receipt_booleans_test.clj:23` selects
  versioned committed-write tools; its executable scenarios at line 67 have
  the same two verbs. It does not establish the requested probe coverage.
  The read-only [replay](replay.log) prints the missing probe registration.
- `test/clj_surgeon/help_test.clj:22` contains pure probe identity/verdict
  assertions. These are existing witnesses, not evidence that the CLI's warm
  example executed in this attempt. The example execution remains owed under
  the packet's no-server constraint.

No probe behavior, registry, test, or help text was changed. A successful
refusal-kind census is distinct from native-failure completeness and receipt
boolean coverage. The Block A census repair cannot discharge these checks.

Owner: Fable. Unblock: identify the intended existing probe registry/witness
subject or authorize a bounded scope amendment, and assign execution of the
warm example to an eligible executor. Independent acceptance stays external.
