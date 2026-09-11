# rename_alias fix round 1 — stopped on contract disagreement

Recorded: 2026-09-11T00:21:52.827495+00:00
Workspace: `/home/forge/src/clj-surgeon-rename`
HEAD: `a1989b12f1a183a31bad58052c303f78b5c4bcc2`

Status: STOPPED under the brief's explicit instruction: “if you believe it IS a contract change, stop and say so in the report.” No implementation or test changes were made. The working tree is clean. This is not a completed fix round or a Sol-ready handoff.

## Disagreement: F5(b)

I agree with the requested behavior as a useful correction to the product, but believe exempting every `(comment …)` body from the namespace-mutation tripwire changes the frozen written contract.

`docs/intent/rename-alias/contract.md:134–137` says:

> Refuse effective core in-ns/alias/ns-unalias/runtime require forms outside ns:
> `:unsupported-namespace-mutation`. Recognize unqualified/clojure.core heads outside
> ordinary quote; this is a conservative syntactic tripwire. Computed/macro-hidden
> namespace mutation and runtime alias manipulation are outside the guarantee.

Section 2, line 95, explicitly says:

> `(comment ...)` is code syntax, not a comment token.

The declared syntactic exception is ordinary quote, not comment-macro ancestry. For example, in a selected file with a valid target binding, `(comment (require '[y :as z]))` falls within the written tripwire. Exempting that form is sensible because the comment macro does not execute its body, but it is an additional exception to the stated syntactic rule. The contract's separate exclusion of comment-macro lookalikes from root-ns selection (line 112) does not explicitly extend to this tripwire.

The selected-ns qualifier at line 126 explicitly applies to duplicate library/alias checks. It supports F5(c) directly; it does not explicitly qualify the later runtime-mutation paragraph. Thus I cannot characterize all of F5 as an implementation-only correction. Opus likewise identified F5(b) as conforming to the letter and requiring a contract ruling.

The smallest concrete resolution would specify: namespace-mutation tripwires run only for selected namespaces and ignore forms under `comment`/`clojure.core/comment`; alias reference traversal continues inside those bodies. This preserves the separate reference-renaming promise. No such amendment was made here.

## Work and evidence

- Read the full Opus report and inspected the frozen contract, planner, shell, transaction call sites, and existing witness patterns.
- Commits: none. Pushes: none.
- RED logs: none created; no mutation probes or tests run.
- Boolean inventory and registered fault seams: not completed. Existing receipt fields were inspected, but no claim of exhaustive enumeration or seam coverage is made.
- Rename/insert witness gates, `make test-fast`, census delta and lint: not run. No green verification or `+N, 0 removed` census claim.
- No landing-gate-prewarm, server start, or port contact.

All requested fixes remain outstanding. Stop occurred during initial contract inspection, before F1 implementation.
