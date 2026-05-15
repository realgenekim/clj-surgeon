# Field Extraction for `.clj-surgeon.edn`

**Status:** superseded by the fn-based design that actually shipped. See
the [README](../README.md#clj-surgeonedn--per-project-config) for the
current syntax, the stdlib extractor reference, and worked examples.

## What this doc was

An early design proposal for a declarative selector DSL — operators like
`[:nth N]`, `[:find-first :vector]`, `[:right-of :method]`. The idea was
to keep `.clj-surgeon.edn` pure data, no eval.

## Why we moved away from it

After implementing the DSL and trying it against real Metabase macros, a
few things were clear:

- **Vocabulary sprawl.** Every new macro shape tempted a new selector op.
  `:find-first-after`, `:when-type`, `:tuple`, `:join` — each addition
  felt necessary in context but the surface grew unmanageable.
- **Awkward two-form syntax.** A field-spec could be a bare vector (just
  the selector) or a map (selector + `:optional?` + `:emit?`). Default
  semantics differed between the two. Easy to forget which is which.
- **Less expressive than Clojure.** Real macros have shapes the DSL
  couldn't capture without escape hatches — multi-arity bodies, optional
  slots that depend on the *type* of an earlier slot, conditional
  extraction. Each forced another DSL primitive.
- **REPL-unfriendly.** To debug a selector, you had to run the whole tool.
  No way to paste a snippet into a REPL and see what it'd resolve to.

The replacement: `.clj-surgeon.edn` `:fields` values are real Clojure
functions evaluated in an SCI sandbox. clj-surgeon ships a stdlib of
named extractors (`->defn-name`, `->first-vector`, etc.). Users compose
those with normal Clojure and inline `(fn [z] ...)` when needed.

The fn-based design wins on:
- **No vocabulary to learn** beyond regular Clojure + the `rewrite-clj.zip`
  API.
- **REPL testable** — copy any extractor fn into a REPL, run it against a
  zloc, see the result.
- **Full expressiveness** — `let`, `cond`, `try/catch` all available.
- **One concept** — every field value is a function, not "vector means X,
  map means Y."

## See also

- [README — `.clj-surgeon.edn` section](../README.md#clj-surgeonedn--per-project-config)
- [skill.md — Generate `.clj-surgeon.edn` for a Repo](../skill.md#generate-clj-surgeonedn-for-a-repo) (workflow for Claude Code)
- `src/clj_surgeon/fields.clj` — stdlib extractor source
