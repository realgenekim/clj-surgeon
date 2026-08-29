# Captain's Log: Positional Mutation Authority Was Purged

Date: 2026-08-29

Issue: `clj-surgeon-qf9`

Lane: SURGEON2, isolated product candidate; no install, reload, or publication

## Outcome

A caller-supplied position can no longer select the owner of a direct CLI
mutation. An `:expect`-guarded `:edit` must start with `[:form NAME]` or
`[:form NAME PLATFORM]`.

Lines, navigation, spans, and partitions remain available for read-only
queries and plan-only edits. A later `:replace-subform!` still consumes only a
reviewed source/result-hash-fenced plan. Positional diagnostics and output
remain unchanged.

## Immutable chain

- relation-free base:
  `15b6a83d9428c41b52878335cac4e8619d77c36f`
- HLD and leaf design:
  `6e42984a831bb3b670e53c16c780803f4102035f`
- red duplicate-owner witness:
  `307d3431717d277bc8b694270bb35c4325fa32eb`
- green product candidate:
  `92b6289f1c713c197ce462f5117c17aebaf08103`
- green product tree:
  `caeffca3e635dfa7db27fb12f47878c40fef78b9`

The red witness produced eight failures. It changed `(defn wrong [] :old)` to
`:new` and replaced the pre-existing plan, while leaving the intended owner
unchanged. That is the exact shipped failure class.

## Caller-visible refusal

The real CLI exits 1 and returns:

```clojure
{:error-type :positional-mutation-authority-refused
 :source-state :unchanged
 :source-unchanged true
 :first-step [:line 14]
 :required-root [:form OWNER]
 :error
 "Direct :expect-guarded edits require a named top-level owner. Start with (form 'OWNER), or remove :expect and use :plan-out for review."
 :remedies
 {:named-owner
  {:instruction
   "Name the top-level owner with (form 'OWNER) before a direct :expect-guarded edit."}
  :plan-review
  {:instruction
   "Remove :expect, write the plan with :plan-out, review it, then apply it with :replace-subform!."}}}
```

The complete refusal capture SHA-256 was
`3946cb67739776453e14c10aa458d567c39846e9ffdde204d06da4e30c1b9f9c`.

## Closure evidence

The public registry inventory found one caller-visible positional argument:
`:show-form :line`, and that operation is read-only. The reviewed plan apply
entrance accepts only `:plan`; it does not accept a raw line, ordinal, index,
position, or address. The direct edit boundary also rejects valid unnamed
roots for a physical line, file-wide structural match, relative navigation,
and positional span before any source or plan I/O.

Verification:

- focused CLI edit: 37 tests, 450 assertions, green;
- complete fast suite: 639 tests, 5,492 assertions, green;
- Linked-Intent audit: `ok=true`, `violations=[]`;
- MCP suite: 284 tests, 2,899 assertions, with two failures in the unchanged
  250 ms cold analyzer admission test while host load rose from 5.59 to 12.25;
- exact failed analyzer test rerun in the isolated bounded nREPL: green;
- candidate diff against the base changes no analyzer source or test.

The full MCP result is therefore recorded as a pressure-confounded miss, not a
green suite. The exact affected product and intent gates are green.

## Safety boundary

This candidate changed no shared worktree, installed CLI, shared MCP server,
port, or existing process. Publication remains a separate gate owned by
SURGEON1 and Gene.
