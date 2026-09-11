# Receipt boolean class ratchet

Parent: ../rename-alias/design.md; shared by insert_forms and subsequent versioned
request-file verbs. Registry: [registry.edn](registry.edn).

- [x] **RECEIPT-BOOL-001**: For every boolean-valued receipt key, the class ratchet
  shall enumerate committed evidence recursively, require a registered seam by
  verb and field name, drive that seam, and observe false at each occurrence.
  Missing verbs and missing fields shall fail by name. Already-false outcome
  fields have explicit completed-write / behavior-not-run scenarios; no boolean
  is exempt because its success value is false. Faulted writes never claim success.

  Witnesses: `every-receipt-boolean-has-a-driven-false-witness`,
  `unregistered-verbs-and-fields-fail-by-name`. Public versioned write tools are
  discovered from the real tool catalog, independently of this registry; a third
  versioned write verb must register an executable scenario and all boolean keys.
  Durable per-file rename preservation is included, alongside the returned receipt.
