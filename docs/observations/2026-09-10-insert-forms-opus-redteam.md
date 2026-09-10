# insert_forms v1 — red-team review (Opus, forge@anvil)

**VERDICT: GO-WITH-FIX for sending to Sol.**

Subject: branch `fable/insert-forms`, tip `33d75cfc`, base `bd124492`, worktree
`/home/forge/src/clj-surgeon-insert` (read-only). All probes ran in my own worktree
`/var/tmp/forge/insert-fx/wt-redteam` (branch `fable/insert-forms-redteam` @ `33d75cfc`),
temp under `/var/tmp/forge/insert-fx/redteam`, every JVM `-J-Xmx1g`, no server started,
no push, no edit to the builder's tree.

I could not break the shipped behaviour. Across 30 boundary probes through the production
path (bb CLI request-file, JVM `-M -m clj-surgeon.core`, and the MCP handler Var) I found
**no case where insert_forms wrote wrong bytes, lost a byte, or lost an update**. The §5
golden reproduces byte-exact through the production bb entrance. Two concurrent writers with
the same guard produced exactly one commit and one `:source-hash-mismatch`. An independently
written bracket-matching hasher that never touches rewrite-clj agreed with the builder's
oracle on 10/10 root-form digests across three adversarial fixtures.

What earns the WITH-FIX is not a wrong byte. It is (F1) that the one guard standing between
a bad splice and a silently corrupted commit **has no witness at all** — I deleted it and the
builder's own headline number, 18 tests / 457 assertions, stayed green — and (F2) that the
verb's ordinary output is formatting a caller will immediately want to undo.

---

## Findings, by severity

### F1 — HIGH. The candidate preservation/structure guard is unwitnessed, and `other_forms_unchanged` is a hardcoded noun, not a computed fact

`src/clj_surgeon/insert_forms_plan.clj:442-446` is the last line of defence named by contract
§3 ("a swallowed form or altered token boundary refuses `:candidate-structure-mismatch`").
The receipt beside it claims preservation with a **literal**:

    src/clj_surgeon/insert_forms_plan.clj:456
    :preservation {... :other_forms_checked (count other) :other_forms_unchanged true}

`true` is typed in. Its only justification is the `refuse!` five lines above.

**Reproduction 1 — delete the guard, the whole witness set stays green.**

    cd /var/tmp/forge/insert-fx/wt-redteam
    # replace lines 442-446 of src/clj_surgeon/insert_forms_plan.clj with a comment
    /var/tmp/forge/insert-fx/redteam/runwit.sh <all 18 insert-forms namespaces>

    Ran 18 tests containing 457 assertions.
    0 failures, 0 errors.
    SUMMARY {:test 18, :pass 457, :fail 0, :error 0, :type :summary}

That is the builder report's exact headline ("Final focused run: 18 tests, 457 assertions,
zero failures/errors") reproduced with the safety net removed.

**Reproduction 2 — the guard is load-bearing: without it a wrong offset commits corruption.**
Same worktree, second defect: `offset` returns `(max 0 (dec p))` (splice lands one byte inside
the preceding form). Fixture `(defn a [] 1)\n(defn ab [] 2)\n`, the §5 golden request.

    # guard PRESENT
    bb --classpath src -m clj-surgeon.core :insert-forms! :request-file req.edn
    {:state "refused", :committed false, :mutation_attempted false, :source_unchanged true,
     :at [:candidate], :error "Inserted siblings or preserved root forms differ.",
     :error-type :candidate-structure-mismatch, :next_action "revise-request", ...}
    EXIT=2      file unchanged

    # guard DELETED (the mutant all 18 witnesses pass)
    {:state "committed", :committed true, :mutation_attempted true, :source_unchanged false,
     :bytes_added 15, :inserted_form_ranges [], ...
     :preservation {... :other_forms_checked 2, :other_forms_unchanged true}, ...}
    EXIT=0
    file now:
    (defn a [] 1
    (defn b [] 3)
    )
    (defn ab [] 2)

The receipt says `committed`, `ok true`, `write_verified true` and **`other_forms_unchanged
true`** over a file whose anchor form was demonstrably rewritten. The only tell is
`:inserted_form_ranges []`, and nothing tells a consumer to read it.

**Coverage gap that produced this.** Three contract-named refusal types are raised in
production and asserted by **no witness**:

    :candidate-structure-mismatch   (§3)
    :candidate-parse-error          (§3)
    :source-parse-error             (§3)

Contract §8: "Every refusal witness asserts exact error type AND unchanged target bytes."
These three have none. Cross-check:
`grep -oh ':[a-z-]*' src/clj_surgeon/insert_forms_plan.clj src/clj_surgeon/insert_forms.clj`
vs the same over `test/clj_surgeon/insert_forms_*_test.clj`.

**Fix (both halves, same commit).**
1. Compute the boolean: `:other_forms_unchanged (every? #(= (:before_sha256 %) (:after_sha256 %)) other)`
   so it cannot read `true` while the check is absent. Same for the count.
2. Add the three missing refusal witnesses. The cheapest RED that stays honest is the one
   above — a seam that perturbs `p`, asserting `:candidate-structure-mismatch` **and** unchanged
   target bytes; plus a truncated-source fixture for `:source-parse-error`.
3. Highest rung available here: have the reviewer re-delete the guard and require the suite to
   go red (memory: `marker-presence-audit-is-not-a-ratchet` — prove the ratchet goes red by
   reintroducing the defect, which is exactly what this section did).

### F2 — MEDIUM-HIGH. Every insertion emits a spurious blank line, and the amount of it depends on whether the anchor happens to carry a trailing comment

This is contract-specified (§3: p stays at L's exclusive end, "All remaining gap bytes stay on
the right", prefix newline + payload + suffix newline) and the §5 golden hash encodes it —
so it is a **contract** defect, not an implementation deviation. It is also the verb's most
visible caller-facing behaviour, and it is disclosed nowhere in the builder's report.

Observed, production bb entrance, real files:

    source:  (defn a [] 1)\n(defn ab [] 2)\n           request: after defn a
    result:  (defn a [] 1)
             (defn b [] 3)
                                <-- blank line the caller did not ask for
             (defn ab [] 2)

    source with a blank line between forms:
             (defn a [] 1)
             (defn inserted-one [] :x)     <-- jammed flush against the anchor
                                           <-- and the original blank line is now doubled
             (comment (defn- a [] 9))

    body insertion into a deftest:
             (testing "outer"
               (is 1)
               (is :new)
                                <-- blank line inside the body
               (is 2))

    empty body, boundary {:position "first"}, source "(defn a [])":
             (defn a []
               1
             )                  <-- closing paren orphaned at column 0

    same anchor but WITH a trailing comment — different formatting, no blank line:
             (defn a [] 1) ; tail
             (defn b [] 3)
             (defn ab [] 2)

`{:position "last"}` on a non-empty body has the same orphaned-paren shape
(`"(deftest t\n  1\n  2\n  3\n)"` is a frozen expectation in
`test/clj_surgeon/insert_forms_body_boundaries_test.clj:13`).

Why it matters for a verb whose entire pitch is "never invoke a formatter": the caller must
run a formatter anyway, or hand-fix, on every single insertion — which reintroduces exactly the
neighbour churn the contract forbids the tool from causing. **Ask Sol to rule on the p rule
before the first caller trial**; the candidate revision is "when the gap immediately after L
begins with a newline, advance p past that newline and emit no prefix newline", which makes the
no-trailing-comment and trailing-comment cases agree and removes the orphaned paren for `last`.
Contract §9 already flags this as least-certain choice (1) and says "Validate these in the first
caller trial; any revision updates contract and red witnesses first."

### F3 — MEDIUM. §6 refusal envelope deviates from the contract's EDN example, field for field

Contract §6 example:  `:candidates [{:kind "defn" :name "a" :line 1} {:kind "defn" :name "a" :line 8}]`
Production (`insert_forms_plan.clj:268`): `(mapv #(select-keys % [:line :start :end]) (take 10 candidates))`

Observed:

    :candidates [{:line 3, :start 42, :end 72} {:line 5, :start 77, :end 107}]

`:kind` and `:name` are absent, `:start`/`:end` are extra. Cap-at-10, `:actual` as the total, and
`candidates_truncated` are all conformant. No witness asserts the candidate map shape.

Second deviation in the same envelope: `:remedy` is one hardcoded sentence for **every** refusal
(`insert_forms_plan.clj:45`), "Revise the field named by at using the reported contract."
Contract §6: "remedy names the missing decision", and its example remedy is reason-specific. The
generic string is actively wrong on `:source-hash-mismatch`, where `next_action` is
`refresh-source` and no field needs revising:

    {:state "refused", ..., :error-type :source-hash-mismatch, :next_action "refresh-source",
     :remedy "Revise the field named by at using the reported contract."}

### F4 — MEDIUM-LOW. BOF insertion lands above a file-header comment

    source:  ;; file header\n(defn a [] 1)\n     request: before defn a
    result:  (defn b [] 3)
             ;; file header
             (defn a [] 1)

Contract §3 says this deliberately ("At BOF, insert before all leading trivia... deliberately
does not guess whether a file-header comment documents the whole file") and §9 names it as
least-certain choice (1). Confirming it in the field: the common real shape is
`;; Copyright\n(ns foo)\n...`, and "insert before ns" puts the payload above the copyright.
Bundle the ruling with F2.

### F5 — LOW. Crash window: recoverable, but there is no recovery procedure, tool, or witness

Arranged via the injected `:external-after-write` hook, hard-killing with
`(.halt (Runtime/getRuntime) 9)` immediately after publication.

    target file:  committed, sha c3f186594e61b7be...  (the intended candidate)
    durable receipt: :state "planned", :next_action "await-publication",
                     :result_hash "c3f186594e61b7be..."   <-- matches the file
    leftover:     <receipt>.edn.candidate  (the staging seed, normally removed in `finally`)

Good news: recovery **is** decidable — the planned receipt carries `result_hash`,
`inverse_splice` and `source_hash`, so hashing the target distinguishes committed / not-committed
/ third-party-modified without guessing. What is missing is (a) any documented or executable
recovery procedure in `docs/intent/insert-forms`, (b) a witness that a recovery reader reaches
the right verdict from a planned receipt, and (c) cleanup of the orphaned `.edn.candidate`,
which accumulates one file per crash. The builder names this as open risk 3 and least-sure
choice 2; I confirm it and add that the receipt is sufficient, so the fix is documentation plus
one witness, not a redesign.

### F6 — LOW. A shared write primitive changed for Babashka portability, with no new witness

`src/clj_surgeon/file_ops.clj:97-103` — `with-publish-lock*` no longer calls `(.release lock)`;
it relies on `with-open` closing the channel. Every verb in the repo uses this. I read it as
behaviour-equivalent (the `with-open` close releases the lock on both the throw and the normal
path, still inside the same `try`/`finally (.unlock monitor)`), and my two-process concurrency
probe passed cleanly. But the function carries a long comment about a lock that once escaped
`commit!` and stranded a workspace, and this branch adds **no** file-ops witness that the lock
is released after `f` throws. Cheap ratchet, high blast radius.

### F7 — LOW. `read_receipts` is emitted unconditionally on every completed inspect result

`src/clj_surgeon/mcp_inspect_tool.clj:889-896` attaches, for every file in `file_hashes`, a
five-key map that repeats the digest and adds four constants. It is required by contract §4
("a NEW portable projection required of supporting read entrances"), but it lands on every
`inspect_clojure` caller whether or not they intend to insert, roughly doubling the per-file
cost of the `file_hashes` block, and the projection is fully derivable by the caller from
`file_hashes` + `workspace_root` + `read_complete`. Worth confirming with Sol that a large
multi-file outline still fits its response envelope.

### F8 — INFO. `execute!`'s outer catch can report `recovery-required` for a clean commit

`insert_forms.clj:199-203`: the catch inspects `@completed`, which by then holds the **successful**
receipt if the throw happened after `commit-plan!` returned — e.g. releasing the publish lock.
It then returns `recovery-required` / `:commit-outcome-unknown` for a transaction that
committed and verified. Fail-safe in the right direction (contract §6 reserves
`recovery-required` for unproved post-write state), so INFO not a defect, but it will produce
alarming false positives whose only cure is reading the durable receipt.

---

## Conformance table (contract §-by-§)

| § | Subject | Verdict |
|---|---|---|
| 1 | One op, two entrances, same planner/transaction/projector | conformant — CLI and MCP receipts identical modulo `elapsed_ms`/artifact path (verified by direct comparison) |
| 1 | Closed EDN schema; unknown keys, wrong types, dup keys refuse | conformant (`insert_forms_plan.clj:143-199`; `{:a 1 :a 2}`, `{} {}`, extra key all refuse `:invalid-request`) |
| 1 | Bounds 2 MiB / 8 MiB / 1 MiB / 1000 forms / depth 512, `:limit-exceeded` never truncation | conformant, and **amended** (see amendments below) |
| 1 | Path rules: relative, inside root, no traversal/symlink/nonregular/missing/nlink>1; permission bits preserved | conformant — symlink target refused `:invalid-path`, file left a symlink; `chmod 600` survived a commit (600 → 600) |
| 1 | LF and uniform CRLF accepted; mixed refuses; no BOM | conformant — uniform CRLF committed with `\r\n` separators; one LF line among CRLF refused `:unsupported-source` |
| 1 | `.cljs`/`.cljc`/reader conditionals/reader-eval refuse | conformant (`#?`, `#=`, `.cljs` all refused) |
| 2 | Exact list head + complete name symbol, case-sensitive, never prefixes | conformant — `a` vs `ab` correct; witness goes red when I make it a prefix match (D1) |
| 2 | `clojure.core/*`, `clojure.test/deftest` canonical equivalents | conformant (witnessed in `insert_forms_defn_headers_test`) |
| 2 | Metadata included in the source span; name unwrapped | conformant — `(defn ^:private a ...)` and `^{:doc "x"}\n(defn a ...)` both selected; `"^:private a"` as a name refused |
| 2 | Never descend into `(comment ...)`, discards, quotes, strings | conformant — decoys ignored; decoys-only file yields `:anchor-not-found`; witness goes red when I make it descend (D2) |
| 2 | `defmethod`/`declare`/wrapped defs unsupported | conformant (`:anchor-not-found`) |
| 2 | `testing_path` direct children, string literal labels, no substring, repeats refuse | conformant — repeated inner labels → `:anchor-multiple-matches`, `:at [:anchor :testing_path 1]`, expected 1 / actual 2 |
| 2 | boundary first/last/after-child N, N 1-based ≤ child count, `child` forbidden on first/last, after-child illegal on empty | conformant (N=0 → `:invalid-request`, N=5 → `:anchor-index-out-of-range`, `{:position "first" :child 1}` → `:invalid-request`) |
| 2 | defn header exclusion, multi-arity `:arity`, trailing attr map excluded, malformed → `:unsupported-owner-shape`, body on def/ns refuses | conformant (all six sub-cases probed) |
| 3 | Payload read to EOF, exact `payload.forms`, discard refuses `:unsupported-payload-syntax`, comments-only is a count mismatch | conformant |
| 3 | Unbalanced/invalid/malformed tail → `:payload-parse-error` | conformant |
| 3 | Splice offset p rule; standalone comment never moved across R; BOF before leading trivia | conformant to the letter — **see F2/F4 for the consequences** |
| 3 | Column C, minimum-indent M, literal continuation lines byte-identical, tabs refuse | conformant — multiline string continuation lines preserved byte-for-byte; tab in payload and in measured source indentation both refuse `:unsupported-indentation`; witness goes red when I drop the literal exclusion (D4) |
| 3 | Newline normalization outside literals only | conformant (CRLF payload/source case) |
| 3 | Reparse, assert unchanged form count and token spellings; `:candidate-structure-mismatch` / `:candidate-parse-error` / `:source-parse-error` | implemented, **UNTESTED — F1** |
| 4 | `{:sha256}` XOR `{:read_receipt}`; wrong path/root/false completion/both → `:invalid-guard`; mismatch → `:source-hash-mismatch` | conformant (witnessed and re-probed; a one-byte change in a trailing comment refuses with expected/actual digests) |
| 4 | Lock through capture/plan/commit, recheck identity + digest before replacement, read back and hash | conformant — two concurrent writers: 1 commit (exit 0), 1 `:source-hash-mismatch` (exit 2), no lost update |
| 4 | `:concurrency "cooperative-lock+final-recheck"`, no CAS claim | conformant (verbatim in every success receipt) |
| 5 | Golden receipt, field for field | **conformant, byte-exact.** Reproduced through the production bb CLI: `source_hash 81f14e0b…`, `result_hash e51722e4…`, `bytes_added 15`, `splice {13,15,6e460fd4…}`, `line_range {1,2}`, `inserted_form_ranges [{1,2,2}]`, `preservation {1d1f4497…, c35754ff…, 2, true}`, `anchor_column 0` — all identical to the contract |
| 5 | state/committed/mutation_attempted/source_unchanged rendered first | conformant (`insert_forms.clj:210-213`) |
| 5 | Durable detail with `receipt_details_path` + `receipt_hash`; request, resolved anchor, byte counts, splice, ordered per-form entries incl. discards, edited owner before/after, inverse splice | conformant — inspected the artifact directly |
| 5 | Inline summary ≤ 4 KiB, ranges to detail if it exceeds | conformant, **amended** to a 3,200-byte pre-publication refusal (see below) |
| 5 | `verification_complete=false`, `verification {parse+byte-preservation, not-run}`, never `terminal_response` | conformant (no `terminal_response` key in any of ~30 receipts, success or failure) |
| 6 | Refusal envelope | **deviation — F3** (`:candidates` shape, generic `:remedy`) |
| 6 | `next_action` per class; stale → refresh-source | conformant |
| 6 | failed / rolled-back / recovery-required shapes | conformant (witnessed at four injected stages; I confirmed the crash case out-of-process) |
| 7 | Independently authored oracle, run red against reformatting / wrong-body / deleted comment / forged digest | conformant, and **independently corroborated** — see rung B below. Correlated-parser limitation is recorded in the oracle's own docstring |
| 7 | Verifier takes independently captured A and B | partial — `insert_forms_support/accepted` runs the oracle over `insert/plan`'s in-memory candidate, never over the file read back from disk. The written bytes are separately asserted against a literal in `insert_forms_parity_test` |
| 8 | 18 literal `deftest` names, table-driven | conformant, all 18 present and enrolled in the census |
| 8 | "Every refusal witness asserts exact error type AND unchanged target bytes" | **deviation — F1**, three refusal types have no witness |
| 9 | X is exactly one EDN map, no tags/dup keys/trailing value; no shell evaluation | conformant (trailing value, `#inst`, `#unsafe/tag`, `#=`, `{` all refuse `:invalid-request`) |
| 9 | Exit 0 committed only; 2 refusal; 1 I/O / rolled-back / recovery | conformant — measured 0 / 2 / 2 / 2 / 2 / 1 / 2 / 0 across eight invocations |
| 9 | Stdout exactly one EDN map + newline, diagnostics to stderr | conformant — stderr empty on every run |
| 9 | `elapsed_ms` measured, never fixed | conformant — varied 0.28–99.1 ms; the literal `0.0` in `mcp_insert_forms.clj:13` is overwritten downstream by `operation/invoke!` (probed: `:limit-exceeded` came back `elapsed_ms 0.671187`) |

### Amendments to the contract, and whether the builder listed them

`diff /home/forge/src/clj-surgeon-records/docs/observations/2026-09-10-insert-forms-contract-astra.md
      /home/forge/src/clj-surgeon-insert/docs/intent/insert-forms/contract.md`
yields exactly two hunks:

1. §1 +10 lines — 100,000 lexical units, reader/container depth 512 before recursive parsing,
   16 MiB candidate ceiling checked before materialization.
2. §5 +2 lines — required projected inline facts above 3,200 UTF-8 bytes refuse
   `:limit-exceeded` at `[:receipt]` before publication.

Both are listed in the builder's report, items 1 and 9, with matching reasons. **No
undisclosed amendment.** Neither weakens a refusal. The records copy was not edited.

---

## Rung B — preservation oracle independence

The builder's open risk 1 is that production and oracle share rewrite-clj. I wrote a 60-line
Python top-level-form splitter that uses **bracket matching only** — no rewrite-clj, no Clojure
reader — tracking string literals, `\char` literals, `;` comments and reader prefixes
(`' \` ~ ~@ @ ^ # #_`), and compared its per-form SHA-256 against the durable receipt's
`preservation_entries` after a real committed insertion.

    /var/tmp/forge/insert-fx/redteam/forms.py     (independent hasher)
    /var/tmp/forge/insert-fx/redteam/cmp.py       (comparison)
    python3 /var/tmp/forge/insert-fx/redteam/cmp.py

Three fixtures, chosen to punish a naive parser:

| fixture | content | result |
|---|---|---|
| f1 | `ns`, `defn`, `(comment (defn- a ...))`, `#_(defn a ...)`, `defn` — top-level after | 5 entries, **0 disagreements** |
| f2 | string containing `) ] } ;` and a newline, `\newline` char literal, `#"regex with ( bracket"` — top-level before, payload with a leading comment | 3 entries, **0 disagreements** |
| f3 | `deftest` + `testing`, body insertion after-child 1, plus a sibling `defn` | 2 entries, **0 disagreements** (including the edited owner's before/after pair) |

Both index accounting (1-based CST root ordinals *including* the `:uneval` node) and every
digest agreed. The correlated-parser risk is real in principle but **is not realised on these
shapes**; I found no disagreement to report.

---

## Rung D — witness reintroduction

Baseline over the six chosen namespaces: 6 tests / 90 assertions / 0 failures.
Each defect applied to my worktree only, then reverted with `git checkout --`.

| # | Defect reintroduced | Witness | Result |
|---|---|---|---|
| D1 | `owner?` matches by prefix instead of exact name (`insert_forms_plan.clj:259`) | `insert-forms-after-prefix-named-defn` | **RED, right reason** — `a` now also matches `ab`, 4 failures, all `:anchor-multiple-matches` where a commit was expected |
| D2 | `resolve-anchor` walks the whole tree instead of root children (`:313`) | `insert-forms-comment-string-anchor-decoys` | **RED, right reason** — 15 failures; decoy inside `#_` selected, decoys-only file no longer `:anchor-not-found`, and a decoy file was mutated |
| D3 | `offset` no longer advances past a trailing comment on L's line (`:342-346`) | `insert-forms-trivia-and-indentation` | **RED, right reason** — candidate placed the payload before ` ; trailing`, and the independent oracle also refused |
| D4 | `indent` stops excluding multiline-literal lines (`:386`) | `insert-forms-trivia-and-indentation` | **RED, right reason** — `:candidate-structure-mismatch` at `[:payload]`, "Reindentation changed tokens" |
| D5 | `one!` accepts the first of several matches (`:264`) | `insert-forms-anchor-cardinality` | **RED, right reason** — 8 failures; duplicate owner silently selected and a repeated `testing "x"` label was written into |
| D6 | `guard!` stops comparing the digest (`:221`) | `insert-forms-stale-hash-refuses` | **RED, right reason** — 6/6 assertions fail, stale source would commit |
| **D7** | **`facts` preservation/structure guard deleted (`:442-446`)** | **all 18 namespaces** | **GREEN — 18 tests / 457 assertions / 0 failures. This is F1.** |

Six of seven witnesses are honest ratchets that go red for the reason they name. The seventh
is the finding.

---

## Rung E — the seat-header irony check

Clean, one line: no test in this branch writes a `.clj` fixture through `sed`, a heredoc, or
string surgery. Every fixture is a Clojure string literal handed to `spit`, and the only
`str/replace` calls (`insert_forms_preservation_test.clj:21-23`) deliberately corrupt a
*candidate* so the oracle can refuse it — which is the contract §7 red-run, not a caller edit.
The contract does not forbid callers from editing text, so there is no rule to be ironic about.

---

## What I could not test, and why

- **Physical `~/bin/clj-surgeon`.** That launcher is pinned to `bd124492`'s installed package,
  not this branch. I ran the production shape instead: `bb --classpath <worktree>/src -m
  clj-surgeon.core`, which is byte-for-byte the same invocation the installed launcher makes.
  Whether `make install-cli` produces a working package from this tip is untested here.
- **A live MCP server.** Ports were out of bounds and no server may be started, so I called
  `clj-surgeon.mcp-insert-forms/handle` directly with JSON-shaped string-keyed params. Transport
  framing, tool-catalog registration in a running server, and `structuredContent` serialisation
  are covered only by the branch's own tests, which I did not independently re-run against a
  server.
- **`make landing-gate` / prewarm at the final tip.** The builder states plainly that prewarm
  was run once at `c5dec4ca` and **not** rerun after the receipt-capacity change at `390a2eb3`;
  that is still true, and I did not rerun it (it is a Make wrapper that exercises out-of-bounds
  surfaces). The final tip has no complete green prewarm. Do not represent it as having one.
- **`make test-fast` at the final tip.** I ran the 18 insert-forms namespaces (457 assertions,
  green) but not the 750-test fast lane; the builder's `test-fast-last.log` is the only evidence
  for the rest of the repo, and it predates nothing material in the insert path.
- **Filesystems without `unix:nlink` or POSIX permissions.** The hardlink and permission checks
  would fall to `:io-error` there; I tested only ext4 under `/var/tmp/forge`.
- **Performance versus native.** No matched-native trial was run and none is claimed. The
  builder's 0.435 s / 3.799 s planner figures are single local runs on his snapshot; I did not
  reproduce or contest them.
- **The Sol reviewer's final report.** It does not exist (the builder's service errored twice).
  This review does not substitute for it, and nothing here should be read as an independent
  final-snapshot approval of the whole branch.

---

## Recommendation

Send to Sol with F1 and F2 named as the two questions to rule on.

Before merge, non-negotiable: **F1** — compute `other_forms_unchanged`, add the three missing
refusal witnesses, and have the reviewer re-delete the guard and require red. That is a
`linked-intent-testing` obligation under this seat's standing rule that every fix which
introduces a requirement registers an intent whose witness fails first.

**F2** is a contract revision, not a code fix; it should be settled before the first caller
trial rather than after, because every witness expectation in the branch bakes the current
spacing in and will have to be re-blessed together.

F3 is a ten-line fix with a witness. F4 rides with F2. F5–F7 are cheap ratchets that can
follow.
