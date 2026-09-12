NO-GO

F1 — BLOCKER. CLASS: bounded, malformed EDN request shapes whose parser throws a `Throwable` outside `Exception`. The required class oracle is a depth/prefix fuzz over every EDN container shape at the 8,192-character servlet boundary, asserting that no throwable escapes and exactly one bounded typed EDN refusal is written.

The real servlet boundary fails this contract:

- `clojure -J-Xss512k -M:clj-surgeon/test-deps -e '<POST 8,192 nested "[" characters through probe-servlet>'`
- Output: `{:escaped java.lang.StackOverflowError, :caught-by-servlet-exception-boundary false, :wire-bytes 0}`

The class is broader than vectors:

- `clojure -M:clj-surgeon/test-deps -e '<drive vector/list/map/set nesting through probe/read-bounded>'`
- Output: all four shapes produced `java.lang.StackOverflowError`; all reported `Exception? false`.

`~/bin/clj-surgeon :op :cat :file src/clj_surgeon/mcp_http_server.clj :form probe-servlet` confirms the servlet catches only `Exception`. Consequently, malformed input can escape without the typed refusal required by BB-PROBE-002 and without any encoded/bounded receipt at the HTTP crossing.

Required repair: introduce a pre-parse EDN nesting bound or an equivalent safe boundary that converts every member of this class into a complete typed refusal, then add the class-enumerating servlet oracle. This is a new finding, so the delta rule prohibits treating it as another autofix attempt; I left no patch.

Other requested pressure points cleared:

- CLJC: focused JVM runs reported `25 tests / 61 assertions` and the downstream CLJC/platform suite reported `83 tests / 1,745 assertions`, both with zero failures. Empty sides and per-platform ordering are covered.
- Six-run replay: isolated `bb attempt20/fold.clj --table-only` reported `{:namespaces 38, :samples 456, :failed {}}`; all three generated artifact hashes matched the committed files byte-for-byte.
- Ceiling: `bb test/clj_surgeon/bb_ceiling.clj …/ceiling-run/receipt.edn` returned `240989`, `42143`, and `343102`.
- Runtime/cadence accounting: the structural read shows separate `:runtime-sums-ms` and `:lane-sums-ms` groupings. The live census found 64 namespaces overlapping `:bb` runtime and `:fast` cadence, each with one cadence owner—no duplication within either axis.
- Landing coverage: the manifest probe returned both `install-test` and `parser-admission-test` as `{:lane :battery, :cadence :landing-and-nightly}`. `bb test/clj_surgeon/battery_ledger.clj check` returned `OK` for `8c542ede`, two commits behind the sealed merge, and `git diff --exit-code 8c542ede..HEAD -- src test Makefile deps.edn bb.edn bin scripts` exited 0.

> END RECEIPT (fence-run): worktree HEAD at review exit = ea5e5ed0761b894c8640ac71563f6cd439ab65ef = fenced sha.
