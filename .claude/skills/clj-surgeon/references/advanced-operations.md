# Advanced clj-surgeon operations

Read only the section required by the current task.

## Dependencies and extraction

Inspect before extracting:

```bash
clj-surgeon :op :deps :file state.clj :form sync-draft!
clj-surgeon :op :ls-deps :file state.clj :form transition!
clj-surgeon :op :ls-extract :file state.clj :form rebuild!
clj-surgeon :op :declares :file state.clj
```

Use the dependency output as evidence; decide architecture and ownership
yourself. Preview extraction before executing it:

```bash
clj-surgeon :op :extract :file src/state.clj \
  :forms '[distill refine helper]' :to src/state/distillery.clj
clj-surgeon :op :extract! :file src/state.clj \
  :forms '[distill refine helper]' :to src/state/distillery.clj
```

After execution, compile and test. Resolve bare references by qualification or
parameters rather than introducing circular dependencies.

When a namespace contains or gains `(declare ...)`, inspect first, then apply:

```bash
clj-surgeon :op :fix-declares :file src/state.clj
clj-surgeon :op :fix-declares! :file src/state.clj
```

## Move forms safely

Always begin with the narrow non-mutating preview:

```bash
clj-surgeon :op :mv :file src/my/ns.clj \
  :form foo :before bar :dry-run true
```

- On `:ok true`, review `:plan/:diff`, then run its `:apply-command`.
- Only on `:would-strand-dependencies`, run the returned
  `:recommended-command`. It previews `:mv-with-deps`, exactly
  `:mv :with-deps true`.
- Review `:requested-forms`, `:added-forms`, `:move-order`, and `:diff`; obtain
  explicit consent for every added form before running `:apply-command`.
- On `:would-strand-users`, cycles, ambiguity, unsupported layout, or any other
  refusal, stop. The alias never moves callers or adds declarations.

Move previews are not saved hash-bound plans. Preview again after any source
change. After writing, rerun `:ls`, audit `:declares`, format, lint, compile,
and test.

## Namespace rename

Plan before execution:

```bash
clj-surgeon :op :rename-ns :from old-prefix :to new-prefix :root .
clj-surgeon :op :rename-ns! :from old-prefix :to new-prefix :root .
```

## CLJC operations

Use deterministic operations instead of manually splicing reader conditionals:

```bash
clj-surgeon :op :cljc-analyze :clj src/foo.clj :cljs src/foo.cljs
clj-surgeon :op :cljc-merge :clj src/foo.clj :cljs src/foo.cljs :out src/foo.cljc
clj-surgeon :op :cljc-split :file src/foo.cljc :clj-out src/foo.clj :cljs-out src/foo.cljs
clj-surgeon :op :cljc-add-require :file src/foo.cljc \
  :platform :cljs :ns goog.string :as gstr :out src/foo.cljc
```

Inspect with `:cljc-analyze` before reconciling divergent forms or requires.
Let the tool preserve reader conditionals and reject alias collisions.
