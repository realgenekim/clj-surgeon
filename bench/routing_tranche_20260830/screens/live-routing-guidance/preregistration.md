# Preregistration: installed routing-guidance transfer screen

## Question

Does the structural-first sentence that cleared a mock native-description
screen still change first mutation choice when delivered through Surgeon's
real `make install-agent-routing` surface?

This screen is a prerequisite for product design. It cannot authorize an
install or prove either of the other routing-package mechanisms.

## Arms

Both arms expose the same ordered fixture-scoped MCP registry, including the
same `native_patch` and `edit_clojure` names, descriptions, schemas, behavior,
and results. Both receive the same task prompt and frozen fixture.

- Control installs the repository's current
  `resources/clj-surgeon-agent-routing.md` into fresh per-run Codex and Claude
  homes by invoking `make install-agent-routing`.
- Treatment invokes the same target with
  `screens/live-routing-guidance/treatment-routing.md`. The only routing-source
  difference is this context-correct sentence at the start of the managed
  block: “For bounded edits to existing Clojure forms, call `edit_clojure`;
  use native patching for prose, new files, or unsupported changes.”

No run uses the minimal-schema treatment. No run changes the real user home,
installed routing block, shared MCP runtime, or product source.

## Frozen sequence and gates

The excluded control pilot runs `f01`, `f02`, `f03`, and `f04` once each. It
must be 4/4 environment-valid, semantically exact, and wrong-subject-free, with
at most 2/4 structural-first. A higher control is a ceiling stop.

Only after that pilot passes, the measured sequence is:

```text
f05 A B   f06 B A   f07 B A   f08 A B
```

This gives four starts per arm, equal first/second positions, and one matched
pair per fixture. Treatment clears the transfer screen only when:

- every start is environment-valid and semantically exact;
- wrong-subject and invalid calls are zero;
- the client-visible registry hash is identical across arms; and
- treatment structural-first exceeds control by at least one start, which is
  the smallest observable lift of 25 percentage points at this sample size.

Every attempt remains in the result. Failure, ceiling, or insufficient lift
stops the adoption package before HLD drafting.
