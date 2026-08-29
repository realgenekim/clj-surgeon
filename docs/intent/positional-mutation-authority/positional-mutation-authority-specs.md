# Positional Mutation Authority Requirements

## Direct mutation authority

- [ ] **POS-AUTH-001**: When CLI `:edit` receives `:expect`, the compiled query
  shall start with exactly one caller-visible named top-level owner step
  `[:form NAME]` or `[:form NAME PLATFORM]` before source or plan I/O can
  occur.
- [ ] **POS-AUTH-002**: When a direct CLI edit starts with a line, file-wide
  match, navigation, ordinal, index, or any root other than `[:form NAME]`, the
  CLI shall refuse before mutation with error type
  `:positional-mutation-authority-refused`, source state `:unchanged`, the
  supplied first step, and instructions to name the owner or use the reviewed
  plan route.
- [ ] **POS-AUTH-003**: When two owners contain the same expected subtree and a
  wrong-but-in-range line selects the second owner, the direct edit shall leave
  the complete file byte-identical and shall not create or change a plan
  artifact.
- [ ] **POS-AUTH-004**: When a direct edit starts with an unambiguous named
  owner and satisfies all existing selection and expectation guards, the
  operation shall preserve the existing atomic write and verification
  behavior.

## Non-mutation positional evidence

- [ ] **POS-AUTH-005**: While a CLI lens request is read-only or plan-only, it
  shall retain existing line, navigation, span, partition, range, and address
  behavior because those requests do not directly mutate source.
- [ ] **POS-AUTH-006**: When `:replace-subform!` applies a reviewed plan, it
  shall continue to require the plan's exact source and result hashes; callers
  shall not gain a public raw line, ordinal, index, or address input.
- [ ] **POS-AUTH-007**: Positional values emitted as diagnostics, receipts,
  ranges, relation `file_index`, or relation `row_index` shall remain evidence
  and shall not become mutation input authority.

## Registry closure

- [ ] **POS-AUTH-008**: The public-operation authority inventory shall fail if
  any direct mutation entrance accepts a caller-supplied line, ordinal, index,
  or positional coordinate as the subject identity.
- [ ] **POS-AUTH-009**: CLI help for direct `:edit` shall teach named-owner
  authority and shall not advertise a line-rooted one-call mutation example.
- [ ] **POS-AUTH-010**: The requirements registry shall link the executable
  duplicate-owner and wrong-file falsifiers that demonstrate why content,
  count, parse, and read-back guards cannot recover caller intent after a
  positional subject is lowered.

## Evidence

- [Positional subject authority audit](../../observations/2026-08-29-positional-subject-authority-audit.md)
- [Wrong in-range index falsifier](../../observations/2026-08-29-wrong-index-ended-emission-composition.md)
