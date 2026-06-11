# Changelog

All notable changes to clj-surgeon are documented here.

Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). No version
tags yet — everything accumulates under Unreleased until a baseline release
is cut.

## [Unreleased]

### Added

- `:ls-tree` / `:tree` / `:map` — map every Clojure project under a directory:
  namespaces, form counts, line counts. Supports `:grep` filtering to find
  projects by content (e.g. `:grep "postgres|jdbc"`).
- Ops registry: single dispatch map driving dispatch, `--help`, and error
  messages. `--help` alone lists all ops by category; `:op <x> --help` shows
  per-op args and examples.
- `.clj-surgeon.edn` project-local config: declare source paths and project
  aliases once, then reference files by alias.
- CLJC structural operations: `:cljc-merge`, `:cljc-split`,
  `:cljc-add-require`, `:cljc-analyze` — merge CLJ/CLJS pairs into CLJC with
  reader conditionals, split back out, classify forms by platform.
- Reader-conditional awareness across all ops (outline, deps, extract).
- Centralized form classification in `forms.clj` — one source of truth for
  what counts as a defn, what is private, etc.

### Fixed

- ClassCastException on bare string ops: `clj-surgeon :op ls-tree` (value
  without leading colon) crashed instead of dispatching. Ops now accept both
  `:ls-tree` and `ls-tree`; unknown ops get a friendly error.
- CLJC reader-conditional requires now appear in outline output; grep no
  longer mishandles loose files outside a project.
- `>defn-` (Guardrails) now correctly detected as private.
- `:declares` returned empty — was reading from deps, which excludes declares.
- Namespace derivation for dialect-split source layouts (`.clj`/`.cljs`/`.cljc`).
