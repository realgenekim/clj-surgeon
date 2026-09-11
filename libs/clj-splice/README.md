# clj-splice

Three pure functions over validated Unicode Strings: `spans`, `splice`, `recount`.
Offsets are exclusive UTF-8 byte intervals over original text; parser columns are
UTF-16. The snapshot stores source once, physical/effective root IDs, N+1 gaps,
and nested syntax nodes. Only physical roots carry SHA-256. It classifies no owners.
Errors are ExceptionInfo with `:clj-splice/category`; no I/O or edit authorization.

`splice` accepts `(splice source [start end] replacement)`, including insertion.
`recount` is `spans` restricted to the structural projection: `:nodes`, `:roots`,
`:effective-roots`, `:effective-count`, and `:gaps`. Source and coordinate-index
tables are omitted. Consumers needing the full inventory call `spans` once.
Synthetic qualifier and conditional-prefix coordinates are explicitly annotated.
BOM is a token, not a preamble. There is no parser resource budget; consumers
requiring bounded parsing must retain their preflight.

From this directory: `clojure -J-Xmx1g -M:test` or `bb test`, with the caller's
approved temp directory set through JAVA_TOOL_OPTIONS and java.io.tmpdir.
JVM pins rewrite-clj 1.2.50. The tested bb releases are 1.12.209, embedding
1.2.50 ([release dependencies](https://raw.githubusercontent.com/babashka/babashka/v1.12.209/deps.edn)),
and 1.13.219, embedding 1.2.55
([release dependencies](https://raw.githubusercontent.com/babashka/babashka/v1.13.219/deps.edn)).
The suite enforces those release pins and emits a deterministic projection hash
for comparison across runtimes. The repository minimum 1.12.209 is tested explicitly.

For many edits against one snapshot, `(splice source [[[a b] replacement] ...])`
validates nonoverlap and assembles once. It adds an arity, not another function.

Batch order is `(start, end, argument-index)`. Equal points insert in argument
order. Strict overlap (`end > other.start` and `start < other.end`) refuses in
either argument order; points at interval boundaries are allowed.

Frozen corpus data lives in `fixtures/*.clj.txt`, outside both source roots.
The test locates it relative to its own resource, so JVM and bb use the same data.
