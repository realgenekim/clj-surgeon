# Stable CLI dependency closure

The installed Babashka entrance must carry the local runtime dependencies used by
its lazy-loaded operations. prepare-cli-package stages Surgeon src and
libs/clj-splice/src atomically in the immutable versioned package. The launcher
names both copied roots. Babashka supplies Clojure and rewrite-clj; there are no
additional bb-only dependencies. The source hash includes the library source and
its dependency declaration so an existing package cannot hide a library change.
No operation grammar or MCP catalog changes.

The approved fix proceeds RED first through the installed subprocess boundary,
then packaging, focused verb groups, lint, fast suite, census review and the
requested prewarm gate. CLI-PACKAGE-001/002 in cli-package-specs.md own the promise.
