# Ethnographic Observation: Nested Surgery Earns Its Keep

**Date:** 2026-07-11
**Context:** First production use of `:find-subform` during the Captain editor
save-race repair in `social-media-writer`. The incident led to a single-flight
editor design: validate the visible browser snapshot, settle the outgoing node,
change identity, push the complete editor frame, and acknowledge as one
serialized command.

## Field Report

> The subform work feels excellent. The difference is not cosmetic: on a
> 4,000-line namespace, `:ls` gave me the six relevant forms immediately; then
> `:find-subform` proved there was exactly one nested `(state/save-session!)`
> inside `handle-save`, one unsafe `set-draft!` inside
> `handle-save-from-draft`, and one Save button at semantic path
> `editor-pane` → `editor-actions` → `button`.
>
> The replay address plus source hash makes it feel like AST-addressed surgery
> instead of “hope this regex still matches.” Compared with `rg + sed`, its
> advantage is strongest for repeated nested Hiccup/actions and huge handler
> forms; `rg` remains faster for broad discovery. This is already earning its
> keep.

## What Happened

The useful workflow had two levels of narrowing. Orientation began with:

```bash
clj-surgeon :op :ls :file src/writer/routes.clj
clj-surgeon :op :ls :file src/writer/state.clj
```

`state.clj` was exactly **4,036 lines with 322 top-level forms**. The outline
reduced the relevant reading target to `sync-draft-tx`, `editor-sync-key`,
`sync-draft!`, and the session-admission forms. Then structural search answered
three narrower questions:

```bash
clj-surgeon :op :find-subform \
  :file src/writer/views/draft_chat.clj \
  :inside editor-pane \
  :match '[:button.action-btn.secondary {:id "save-btn" :data-star-on:click "saveDraft()"} "Save Draft"]'

clj-surgeon :op :find-subform \
  :file src/writer/handlers/book_workshop.clj \
  :inside handle-save-from-draft \
  :match '(state/set-draft! (:draft body) :browser-sync true)'

clj-surgeon :op :find-subform \
  :file src/writer/routes.clj \
  :inside handle-save \
  :match '(state/save-session!)'
```

The searches established three facts needed for the refactor:

- `handle-save` contained exactly one nested `(state/save-session!)` call;
- `handle-save-from-draft` contained exactly one unsafe `set-draft!` call;
- the Save button had the semantic path `editor-pane` → `let` →
  `div#editor-pane.editor-pane` → `div.editor-actions` →
  `button.action-btn.secondary`.

Each search returned **exactly one** match with a semantic path, line range,
preorder replay address, source snippet, and whole-file SHA-256. The session
save path was `handle-save` → `let` → binding `ack` →
`state/save-session!`; the unsafe mutation path was `handle-save-from-draft` →
`let` → `when` → `state/set-draft!`. These were not merely text hits:
enclosing-form scope, structural ancestry, match count, replay address, and
source snapshot together made each result a guarded edit target.

## What This First Use Proved

It proved the value and ergonomics of structural **discovery**. It did not yet
prove the complete `:replace-subform` plan/apply workflow during a production
edit. The source study says explicitly:

> The forthcoming proof will add whether `:replace-subform` plans remain
> ergonomic during actual edits, not merely discovery.

That distinction matters. A replay address and source hash made the discovery
output feel AST-addressed, but the first session did not apply those recorded
addresses to production files.

A later read-only reproduction against the evolving working tree showed the
temporal behavior expected of structural queries:

- the Save Draft Hiccup vector still returned one match;
- the unsafe `set-draft!` call still returned one match;
- the old `(state/save-session!)` inside `handle-save` returned zero because
  `handle-save` had since been rewritten as the compound single-flight save
  command.

That third result is not a regression. It demonstrates why a selector and
snapshot hash are better than treating yesterday's line location as a durable
fact.

## Follow-Up: Production Replacement Proved

The missing proof arrived later in the same repair. `editor-panel` contained a
13-line inline-JavaScript “Edit in Draft” button. A structural pattern retained
the meaningful button text and keyboard hint while using `_` for the style,
click handler, and hint style:

```bash
clj-surgeon :op :find-subform \
  :file src/writer/views/book_workshop.clj \
  :inside editor-panel \
  :match '[:button {:style _ :onclick _} "Edit in Draft →" [:span.kbd-hint _ "^Enter"]]'
```

It returned exactly one AST subtree. The replacement plan collapsed the inline
fetch implementation to a call into the shared single-flight command:

```clojure
[:button
 {:style "padding:4px 12px; background:#ff9800; color:white; border:none; border-radius:4px; cursor:pointer; font-size:12px;"
  :onclick (str "editBookNodeInDraft("
                (pr-str id)
                ",this.closest("
                (pr-str ".bw-editor-panel")
                "))")}
 "Edit in Draft →"
 [:span.kbd-hint {:style "margin-left:4px; opacity:0.7;"} "^Enter"]]
```

The plan recorded result hash
`37ef0b96dedc9e00477b4f0872fa1bb45fb0519f0742cbb8806fe097e3219e64`.
Applying `/tmp/book-workshop-edit-plan.edn` returned `{:ok true}` with that
same hash. This completed the production sequence:

> unique structural discovery → guarded plan → human diff review → exact replay
> address → verified apply

### Quoting sharp edge

The first planning attempt used JavaScript-style `\x27` escapes inside the
replacement and failed during argument parsing. This is correct language
behavior: `\x27` is not a valid EDN/Clojure string escape. But it exposed a
documentation and CLI-error opportunity because the failure occurred before a
plan could be emitted.

The reliable recipe for nested Clojure-generated JavaScript is:

1. Put the complete `:with` form inside single shell quotes.
2. Use ordinary double-quoted Clojure strings inside it.
3. Use `pr-str` to generate quoted JavaScript string literals instead of
   manually embedding quote escapes.
4. Use valid Clojure escapes such as `\"`, `\\`, and `\u0027` when an escape is
   genuinely required; do not use JavaScript `\xNN` escapes.

The corrected plan used `(pr-str id)` and `(pr-str ".bw-editor-panel")`, which
removed the need to embed single-quote characters in the generated JavaScript.

## Why This Matters

The value is not that structural search replaces `rg`. The tools occupy
different parts of the workflow:

| Need | Better tool |
|---|---|
| Broad discovery across files | `rg` |
| Identify relevant top-level Clojure forms | `:ls` |
| Prove the number and location of nested syntax matches | `:find-subform` |
| Apply one reviewed nested edit to an unchanged source snapshot | `:replace-subform` / `:replace-subform!` |

The strongest cases are repeated nested Hiccup actions and very large handler
forms. Text search can reveal candidate lines, but it does not express the
enclosing form, structural ancestry, uniqueness requirement, or snapshot that
was reviewed.

## Design Lesson

The combination matters more than any single operation:

> `:ls` narrows the namespace → `:find-subform` proves the nested target and
> records its AST address and source hash → plan/apply performs that exact edit
> or refuses.

That is the boundary where clj-surgeon becomes more than a faster search tool.
It turns a hand-authored text patch into reviewable, fail-closed structural
bookkeeping while leaving discovery and architectural judgment to the human or
agent.

## Primary Source

The full incident and original field report live in the neighboring
`social-media-writer` checkout at
`docs/ethnographic-study-2026-07-11-live.md`, lines 196–236 at the time of this
observation.

The broader next-day analysis, including 38 direct production invocations
across UI, state, storage, and route work, is in
[Structural Lenses in the Wild](2026-07-12-lenses-in-the-wild.md).
