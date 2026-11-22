# Native change candidates

| classification | occurrences | median_native_actions | median_files_touched | clears_bar |
| --- | --- | --- | --- | --- |
| rename-namespace | 5 | 20 | 18 | yes |
| add-require-across-files | 43 | 29 | 17 | yes |
| move-var | 0 |  |  | no |
| extract-function | 89 | 16 | 7 | yes |
| arity-change | 1 | 55 | 33 | yes |
| alias-migration | 6 | 14.5 | 18.0 | yes |
| other-multi-file | 1035 | 7 | 6 | yes |

Population: turns with no detected Surgeon call, at least two native edit actions and at least two distinct explicitly touched Clojure/EDN paths, plus single-file extract-function candidates meeting the same action threshold. Counts are occurrences of heuristic evidence, not confirmed semantic refactorings. Exactly one class is assigned, in the order below.

1. rename-namespace: changed `(ns NAME)` tokens plus an old namespace/reference token in removed text, with at least two files.
2. add-require-across-files: added `:require` or `[lib :as alias]` text in edit actions covering at least three paths.
3. move-var: the same defn name removed without replacement in one edit and added in a disjoint file edit.
4. extract-function: combined edits to one file have nonempty removed text, a new defn name, and a call to that name in added text.
5. arity-change: a direct defn signature vector changes and at least two paths are edited.
6. alias-migration: at least one qualified-symbol prefix disappears and another appears, with at least two paths.
7. other-multi-file: remaining eligible turns.

Weaknesses: these are lexical, not AST checks. Patch hunks retain their file ownership; macro expansion, implicit references and shell transformations remain unresolved. Comments/strings, docstrings, multi-arity definitions, macros and file creations can cause false positives or negatives. Signature changes may rename arguments without changing arity. Alias changes need not preserve the referenced Var. Shell paths are inferred only for recognized edit commands; dynamic filenames and opaque wrappers are missed. Paths in shell source strings can overcount files. Multi-file patches are one action. Repeated patches to one file are multiple actions. Single-file extraction is included; other single-file edits are not assigned a candidate class. The >=5 bar applies to median native edit actions, not all tools or wall time. A class clearing it is only a candidate for review.
