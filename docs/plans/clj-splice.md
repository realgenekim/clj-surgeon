# clj-splice frozen scope

Approved intent: September 11 clj-splice proposal as corrected by Astra review §§1–6; refusal review §2. Base 04648059 on fable/clj-splice. Three public functions only: spans, splice, recount. Validated Unicode String inputs; UTF-8 byte offsets over original bytes, exclusive ends. rewrite-clj columns are UTF-16 code units. Reject unpaired surrogates and non-boundary edits. LF, CRLF, bare CR and CRFF mapping follows parser normalization; original bytes remain authoritative.

Physical roots exclude trivia but include discard nodes. Effective roots additionally exclude discards: #_#_1 2 3 has one effective root, 3. For N physical roots, reassembly is g0 f0 … fN-1 gN, including the final gap. Hash only disjoint physical root byte intervals. An enclosing interval changes after insertion; only disjoint unaffected intervals translate with unchanged hashes.

Nested nodes have stable per-snapshot IDs, parent/ordered child IDs, syntax tags, byte spans, original row/column and explicit synthetic provenance for map qualifiers and conditional prefix children. No authoritative owner kind/name. Leading BOM remains a token in the library; both verbs continue refusing BOM. Tab admission and conditional editing remain deferred.

Surgeon projects nested nodes into its existing owner/body rules and trivia-skipping zipper preorder addresses. Receipt keys, one-based physical form ordinals, one-based line/end_line, zero-based :address {:preorder N}, byte offset/length, path-bound stale guards, changed/other form and inverse-splice evidence remain compatible. E4 references remain lines 153/1112 and preorders 537/4661; E4 output SHA-256 remains 02332a74caf1ead4d70c555b530230b8b03aebc306bd72f5d129f111c876da0c.

Retain candidate role, structure and inverse mutants, insertion parse-valid wrong-offset mutant, column-one and disk-evidence witnesses. Keep lexical!/shape-limit! resource preflight until parser budgets exist; move literal discovery only where spans supplies equivalent data. Mixed LF/CRLF insertion uses the first source newline as inserted-newline policy, preserving every original byte. Exactly-one-EDN request reading remains separate.

Stages commit in order: freeze; independently green library on JVM and bb; rename; insertion; consolidation; measurements/final gate; report. No pushes, other worktree writes, servers or AGENTS.md edits. Temp only under /var/tmp/forge/splice-fx. Contract questions are recorded without blocking independent work.

Frozen Opus before/after corpora (copied byte-for-byte; includes E4):

| File | SHA-256 | Provenance |
|---|---|---|
| `test-fixtures/clj-splice/rename-f1-A.clj` | `3ff2479f45a68abccd7c2553ce89ae9049ee97ad8bfdcd4d9fab6845ccbeaa80` | `/var/tmp/forge/rename-fx/redteam/rungc/f1-before.clj` |
| `test-fixtures/clj-splice/rename-f1-B.clj` | `30dcf725fa27bc4c3457ab2dac254c1e99ca3d7b7780497ca648c15d2e3f3f64` | `/var/tmp/forge/rename-fx/redteam/rungc/f1/src/a.clj` |
| `test-fixtures/clj-splice/rename-f2-A.clj` | `52f494cdad0c3d37cc0af0e3842c175ae0e88c42013db9103219e1203da0ed15` | `/var/tmp/forge/rename-fx/redteam/rungc/f2-before.clj` |
| `test-fixtures/clj-splice/rename-f2-B.clj` | `7053949f16d80908d705f261e81a0a44e01aa9752c6cd6b8be45a0df262a2446` | `/var/tmp/forge/rename-fx/redteam/rungc/f2/src/a.clj` |
| `test-fixtures/clj-splice/rename-f3-A.clj` | `5f086f7789fdb556ba6e819b26d6eba8ec0345e22b7aaa152b2e1a03648234a1` | `/var/tmp/forge/rename-fx/redteam/rungc/f3-before.clj` |
| `test-fixtures/clj-splice/rename-f3-B.clj` | `02332a74caf1ead4d70c555b530230b8b03aebc306bd72f5d129f111c876da0c` | `/var/tmp/forge/rename-fx/redteam/rungc/f3/src/cfp_scheduler_killer/views/schedule.clj` |
| `test-fixtures/clj-splice/insert-f1-A.clj` | `cb1aba5a9ad6f0db93e706a5d1bc3b3baf1f237718a8d873873474a10cd3c485` | `/var/tmp/forge/insert-fx/redteam/bfix/f1/A.clj` |
| `test-fixtures/clj-splice/insert-f1-B.clj` | `0e0d7294d56540ed9501109a99b263d19b31b6a9bd40c23d34f870d0285d8202` | `/var/tmp/forge/insert-fx/redteam/bfix/f1/B.clj` |
| `test-fixtures/clj-splice/insert-f2-A.clj` | `a081f3a2b7c645522d1f634655708754f5339612a81bc4c88ebfdaba4b64176d` | `/var/tmp/forge/insert-fx/redteam/bfix/f2/A.clj` |
| `test-fixtures/clj-splice/insert-f2-B.clj` | `3102fc3f1184b9c156cf6f2d3715d2ee245113ac8f6bbf00ce923355089d6944` | `/var/tmp/forge/insert-fx/redteam/bfix/f2/B.clj` |
| `test-fixtures/clj-splice/insert-f3-A.clj` | `11b6f6a38a9028a188689f138e5287c5b41629c16c18914ac785b6014c573556` | `/var/tmp/forge/insert-fx/redteam/bfix/f3/A.clj` |
| `test-fixtures/clj-splice/insert-f3-B.clj` | `6b2ce64aa334d655de87bf224265821b1bed33096900c24a80a16d9e4b070bda` | `/var/tmp/forge/insert-fx/redteam/bfix/f3/B.clj` |

## Fix round 1 after Opus

Contract (Opus M1–M3): refusal registries equal the remedy case vocabulary of each verb, including transport failures. Missing rows must fail the shared envelope group. Batch edits sort by start/end/argument index; strict pair overlap refuses, boundary points are legal, equal points preserve argument order. Column-one reference addresses pin line/column/preorder to 1/1/0 and 2/1/9. End-line adjustment is unreachable for consumed alias-reference tokens (single-line); remove it. Low fixes split malformed UTF-8 from capability-only unsupported source, restrict recount to structural keys, and move frozen data outside source roots. Witnesses stay in existing groups. RED logs precede each fix; final JVM/both bb library, 20 verb groups, test-fast, one prewarm, lint and census review required. No push.
