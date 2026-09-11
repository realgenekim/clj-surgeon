# rename_alias v1 — red-team review (Opus, forge@anvil)

**VERDICT: GO-WITH-FIX for sending to Sol.**

Subject: branch `fable/rename-alias-r1`, tip `a1989b12`, base trunk `a6e53564`, builder worktree
`/home/forge/src/clj-surgeon-rename` (read-only, untouched). Every probe ran in my own worktree
`/var/tmp/forge/rename-fx/wt-redteam` (branch `fable/rename-alias-redteam` @ `a1989b12`), temp under
`/var/tmp/forge/rename-fx/redteam`, every JVM `-J-Xmx1g`, `TMPDIR`/`java.io.tmpdir` pinned off `/tmp`,
no server started, no port contacted, no push, no edit to the builder's tree.

**I could not make it write wrong bytes.** Across ~40 boundary probes through the production path
(bb CLI `:rename-alias!` and `:op :rename-alias!` request-file, and the MCP handler Var called
directly) every observable behaviour matched the contract: E4 reproduces byte-exact
(`02332a74caf1…`), the 31→2 refusal carries the right actual count, lines and preorders, prefix
traps hold, literal keywords / strings / regex / char literals / semicolon comments / `#_` discards
are byte-identical, quoted and syntax-quoted symbols and auto-resolved keywords and `#::alias{}`
qualifiers all rename, two concurrent writers produce exactly one commit and one
`:source-hash-mismatch`, and a repository scope with one stale guard writes nothing. An
independently written bracket-matching hasher that never touches rewrite-clj agreed with the durable
receipt on **47/47 root-form digests** across three fixtures, and it independently confirms the
"22 route templates" and "29 route literals" counts on the E4 result.

What earns the WITH-FIX is not a wrong byte:

* **F1** — the guard that stands between a wrong splice and a silently corrupted commit **has no
  witness**. I deleted it and the builder's headline number (10 tests / 349 assertions) stayed green;
  with it deleted the verb commits `(def y eevx)` and reports `committed`, `write_verified true`,
  `other_forms_unchanged true`. This is *the same finding, of the same class, as insert_forms F1* —
  which was fixed for that verb (`insert-forms-candidate-test` now exists) and re-introduced here.
* **F2** — `other_forms_unchanged` is a **tautology**: `other` is defined as the rows that did not
  change, so the `every?` over it can never be false, and nothing refuses on it.
* **F3** — every site that begins at **column 1** is reported on the **wrong line** with a **nil
  preorder address**, including in the refusal evidence whose remedy tells the caller to go review
  those exact sites.

---

## Findings, by severity

### F1 — HIGH. The candidate role-recount guard is load-bearing and unwitnessed; deleting it commits corruption under a clean receipt

`src/clj_surgeon/rename_alias_plan.clj:247-249` is the last line of defence named by contract §4
("Reparse B, independently recompute binding/reference roles, require exactly the planned new
roles/counts, zero old effective references"):

```clojure
(when-not (and (empty? old-refs) (= expected-offsets (mapv (juxt :start :role) new-refs))
               (= 1 (count (filter #(and (= (:lib r) (:lib %)) (= (:new_alias r) (:alias %))) (:bindings new-ns)))))
  (p/refuse! :candidate-structure-mismatch [:candidate file] "Candidate alias roles differ from planned roles."))
```

**Reproduction 1 — delete the guard, the whole witness set stays green.**

```
cd /var/tmp/forge/rename-fx/wt-redteam
# replace lines 247-249 of src/clj_surgeon/rename_alias_plan.clj with (comment GUARD-DELETED)
/var/tmp/forge/rename-fx/redteam/runwit.sh clj-surgeon.rename-alias-test

Ran 10 tests containing 349 assertions.
0 failures, 0 errors.
```

That is the builder report's own focused figure reproduced with the safety net removed. The same is
true of the two other candidate guards:

| guard deleted | witnesses |
|---|---|
| `rename_alias_plan.clj:218-219` root/gap/discard preservation refusal | **10 tests / 349 assertions, 0 failures** |
| `rename_alias_plan.clj:247-249` candidate role recount | **10 tests / 349 assertions, 0 failures** |
| `rename_alias_plan.clj:255-256` inverse-identity (byte vs char offset) refusal | **10 tests / 349 assertions, 0 failures** |
| `rename_alias.clj:160-161` post-replacement disk read-back comparison | **10 tests / 349 assertions, 0 failures** |

**Reproduction 2 — the guard is load-bearing: without it a wrong offset commits corruption.**
Second defect in my worktree: the symbol role's site prefix `[0 "symbol"]` becomes `[1 "symbol"]`
(the splice lands one character inside the token). Fixture
`(ns a (:require [x.events :as events]))\n(def y events/x)\n`, ordinary request, total 1.

```
# guard PRESENT
bb -m clj-surgeon.core :rename-alias! :request-file /var/tmp/forge/rename-fx/redteam/e8.edn
   state = "refused"
   error-type = :candidate-structure-mismatch
   error = "Candidate alias roles differ from planned roles."
   FILE NOW: (ns a (:require [x.events :as events]))|(def y events/x)|      <-- untouched

# guard DELETED (the mutant all 10 witnesses pass)
   state = "committed"
   committed = true
   write_verified = true
   other_forms_unchanged = true
   forms_changed = 2
   references_changed = 1
   FILE NOW: (ns a (:require [x.events :as ev]))|(def y eevx)|              <-- source destroyed
```

`(def y events/x)` became `(def y eevx)` — a different Var, silently — and the receipt says
`committed`, `ok true`, `write_verified true`, `other_forms_unchanged true`. There is no tell at all
in the summary.

**Coverage gap that produced this.** Refusal types raised in production and asserted by **no**
witness:

```
:candidate-structure-mismatch    (§4)
:candidate-parse-error           (§4)
:io-error                        (§5; state is asserted, error-type is not)
:commit-outcome-unknown          (§5; state is asserted, error-type is not)
```

Contract §7: "Every refusal witness asserts exact error type AND all target bytes unchanged." The
first two have no witness of any kind. Check:

```
for t in candidate-parse-error candidate-structure-mismatch ...; do
  grep -c ":$t" test/clj_surgeon/rename_alias_test.clj test/clj_surgeon/rename_alias_parity_test.clj
done
```

**Fix (all three parts, one commit).**
1. A witness for `:candidate-structure-mismatch` through a seam that perturbs a planned site offset,
   asserting the exact error type **and** unchanged target bytes on disk.
2. A witness for `:candidate-parse-error` (a seam that makes the candidate unparseable).
3. Re-delete each of the four guards above and require the suite to go red. This is the
   `linked-intent-testing` obligation and this seat's standing rule
   (`marker-presence-audit-is-not-a-ratchet`): prove the ratchet goes red by reintroducing the
   defect. insert_forms already pays this; rename_alias must too.

### F2 — HIGH. `other_forms_unchanged` cannot be false, and the three "read-back" fields are plan-derived

`rename_alias_plan.clj:212,221`:

```clojure
other   (filterv #(= (:before_sha256 %) (:after_sha256 %)) rows)
...
:other_forms_unchanged (every? #(= (:before_sha256 %) (:after_sha256 %)) other)
```

`other` is *defined* as the rows whose before and after digests are equal, so the `every?` over it is
true by construction — including vacuously, when `other` is empty. It is `true` typed in, wearing a
computation. Nothing refuses on it: a root form that changed without authority is classified into
`changed` and silently inflates `forms_changed`. Reproduction 2 above shows exactly that:
`forms_changed 2`, `other_forms_checked` unchanged, `other_forms_unchanged true`, over a file whose
`(def y events/x)` form was destroyed.

Contrast `insert_forms_plan.clj:527,542`, where `other` is *positional* (all entries except the
edited owner's) and the same predicate is also the refusal condition. That verb's F1 fix did not
travel; this verb reintroduces the defect in a form that is harder to see.

Same class, three more fields:

* `rename_alias.clj:181` — `:read_back_hash (:result_hash x)`. The per-file "read-back hash" is a
  **copy of the planned candidate hash**, never a disk read.
* `rename_alias.clj:172` — `:read_back_hashes` is `(p/sha candidate)` over the in-memory candidate
  strings, not the files.
* `rename_alias.clj:179` — `:details_contains ["per_file" "sites" "preservation" "inverse_splices"]`
  is a hardcoded literal, not a description of what the artifact contains.

The facts behind them are genuinely verified elsewhere (`write-source!` compares the disk digest, and
`complete?` re-observes every path), but delete that comparison (E6) and the suite stays green and
every "read_back" field still reads correct. **Fix:** source each read-back field from
`journal/sha256-file`, compute `details_contains` from the detail map, and define `other` by position
(all root ordinals not in the authorized changed set) so the boolean and a refusal can both be
predicated on it.

### F3 — MEDIUM-HIGH. Any site beginning at column 1 gets the previous line's number and a nil preorder address

`rename_alias_plan.clj:126-128` computes the line with a strict `<`:

```clojure
line (inc (count (filter #(< % (:start x)) (rest starts))))
```

A start that falls exactly on a line boundary is attributed to the **previous** line; the wrong line
then makes `col` wrong, so `(get ad [line col tag])` misses and the address comes back nil.

```
source:
  1 (ns a (:require [x.events :as events]))
  2 events/col1
  3   events/col3
  4 #_x
  5 events/col1b

reported true sites:
  line=1  end_line=2  preorder=nil  start=40  form_index=2     <-- is on line 2
  line=3  end_line=3  preorder=10   start=54  form_index=3     <-- correct (column 3)
  line=4  end_line=5  preorder=nil  start=70  form_index=5     <-- is on line 5
```

`end_line` is right because its `:end` is exclusive, which is why E4 (whose two references are both
indented) looks clean and why no witness caught it. Contract §4 requires "original-source 1-based
line/end_line … `:address {:preorder N}`"; the E4 refusal's own remedy is "Review the two true
sites", so the caller is sent to the wrong line with no address. The same `:line` feeds the
`:unsupported-namespace-mutation`, `:unsupported-libspec` and `:ambiguous-alias-binding` diagnostics,
so a top-level `(require …)` at column 1 is reported one line early.

No write is affected — the splice uses `:start`/`:end`, which are correct. **Fix:** `<=` (or the
binary search in F4, which fixes both), plus a witness that places a reference at column 1 and
asserts its line and a non-nil preorder. `rename-alias-receipt-oracle` already has a column-1
reference in its fixture and asserts only counts.

### F4 — MEDIUM-HIGH. The planner is quadratic in file size; one 4,000-line file costs 63 s

Measured on my worktree, `rename-alias-plan/plan` on a synthetic file with one reference:

| lines | bytes | plan wall |
|---|---|---|
| 253 | 5,549 | 0.54 s |
| 503 | 11,049 | 1.51 s |
| 1,003 | 22,049 | 4.65 s |
| 2,003 | 44,049 | 17.08 s |
| 4,003 | 88,049 | **63.27 s** |

Doubling the input multiplies wall by ~3.7. Profiled:

```
n=1000  addresses= 50.3 ms   tree= 56.4 ms   annotate= 1988.9 ms
n=2000  addresses= 89.2 ms   tree= 60.9 ms   annotate= 7467.1 ms
```

`annotate` is the hot spot: it recomputes `(count (filter #(< % pos) (rest starts)))` for every node,
O(nodes × lines), and runs at least twice per selected file (source, then candidate). Real
consequences measured through the production CLI:

* E4, one 61 KB file: **2.95 s** for the count refusal, **18.9 s** for the committed rename.
* Repository scope over a copy of this repo's own `src/` (122 files, 3.5 MB): **149.9 s**, and it
  then refused (see F5) — 150 seconds to deliver nothing.

The contract makes no speed claim, and a rename is not a hot path — but its stated limits (1,000
files, 64 MiB) are not the operative bound; wall time is, by orders of magnitude.

**The fix is six lines and behaviour-preserving.** Replacing the linear scan with a binary search
over `starts` (and reusing it for `end_line`):

| lines | before | after |
|---|---|---|
| 1,003 | 4.65 s | 1.23 s |
| 2,003 | 17.08 s | 2.58 s |
| 4,003 | **63.27 s** | **5.04 s** (12.6×) |

with `clj-surgeon.rename-alias-test` still 10 tests / 349 assertions / 0 failures under the patch.
It also fixes F3 if the comparison is written `<=`.

### F5 — MEDIUM. Repository scope is refused by ordinary repository furniture, all-or-nothing

Three independent tripwires, each of which kills a whole-repository rename from a single file that
has nothing to do with the alias:

**(a) Any symlink anywhere under the root — even a non-Clojure one.** `rename_alias.clj:41` refuses
before the extension filter:

```
workspace: src/a.clj (the target), docs/real.txt, README-link.txt -> docs/real.txt
{:state "refused", :error-type :invalid-path, :at [:scope],
 :error "Repository scope contains a symlink.", :file "README-link.txt"}
```

Contract §1 says repository scope excludes "only `.git` directories" and includes ignored/untracked
files; the insertion path rules it reuses are about the *target* file. Refusing on any symlink in the
tree (a `latest ->` link, a linked `target/`, a checked-in doc symlink) is a deviation in effect.

**(b) A `(require …)`/`(alias …)`/`(in-ns …)`/`(ns-unalias …)` call anywhere in any inspected file,
including a skipped one and including inside `(comment …)`.** The 150 s repository run above ended:

```
{:state "refused", :error-type :unsupported-namespace-mutation,
 :at [:source "clj_surgeon/mission_forms_source.clj"], :line 379, ...}
```

— `(try (require 'cljfmt.core) true (catch Throwable _ false))`. **4 of 122 files (3.3%)** in this
repo's own `src/` carry such a form. And confirmed directly:

```
(ns a (:require [x.events :as events]))
(comment (require '[y :as z]))
events/x
=> :unsupported-namespace-mutation, file unchanged
```

A rich `(comment (require …))` block — ubiquitous in Clojure dev files — refuses the rename for the
whole repository. §2 does say `(comment …)` is code, so this is the contract's letter, but the
tripwire's stated rationale is that the form "can change namespace bindings", and a `require` with no
`:as`/`:as-alias`, or one inside `(comment …)`, cannot change the **alias** table. A narrower
tripwire (refuse `in-ns`/`alias`/`ns-unalias` always; refuse `require` only when its literal argument
carries `:as`/`:as-alias`) would remove nearly all of this tax without weakening the guarantee.
Recommend Sol rule on it before the first caller trial.

**(c) Duplicate library or alias bindings in a *skipped* file.** `binding!` runs the duplicate check
for every inspected file, before `selected?` is consulted:

```
src/a.clj  (ns a (:require [x.events :as events])) events/x     <- the target
src/b.clj  (ns b (:require [q.r :as u] [q.r :as v]))            <- unrelated
=> :ambiguous-alias-binding at [:source "src/b.clj" :ns], nothing written
```

Contract §3 scopes `:ambiguous-alias-binding` to the "selected ns". Conservative and fail-closed, but
a deviation, and another whole-repo blocker from an unrelated file.

### F6 — MEDIUM. A malformed candidate is reported as a *source* error

With a corrupting splice that damages the ns form, the candidate reparse refuses through
`namespace!`/`libspec!` rather than through the candidate guards:

```
{:state "refused", :error-type :unsupported-libspec,
 :at [:source "src/a.clj" :ns], :error "Unsupported require declaration."}
```

Contract §4: "Candidate failures are `:candidate-parse-error` or `:candidate-structure-mismatch`."
The caller is told their *source* ns is unsupported when their source is fine and the tool's own
candidate is broken. `candidate!` wraps only the `source!` call (line 239-240) in the
`:candidate-parse-error` translation; the `namespace!`/`references` calls on the candidate at lines
241-242 are unwrapped. **Fix:** wrap the whole candidate re-derivation and remap any refusal raised
inside it to `:candidate-structure-mismatch` with `:at [:candidate file]`.

### F7 — MEDIUM. The durable detail is ~16× the source, and is rewritten O(N) times per transaction

E4: a 61 KB source produces a **1,015,599-byte** durable detail, because contract §5 requires
before/after digests for "every gap/comment/discard span" — two 64-char hex strings per whitespace
node. `commit-plan!` calls `persist!` (a full `pr-str` + atomic write + read-back hash of the whole
detail) at lines 120, 131, 150, 155, 163 and 183 — roughly `2N+3` times for N selected files. For
repository scope that is O(N²) bytes of I/O on an artifact that is already O(total source). It is a
real part of the 2.95 s → 18.9 s gap between the E4 refusal and the E4 commit. **Fix:** write the
large sections once and let the progress persists carry only the receipt + transaction status.

### F8 — LOW. Refusal-envelope deviations from the §6 example

* `:remedy` is a single template for every refusal — `"Resolve the reported expect-count-mismatch
  failure at [:expect :references] before retrying."` — against the contract's reason-specific
  example (`"Review the two true sites; correct scope or count and resubmit with guards."`). Same
  shape as insert_forms F3, not yet fixed for either verb.
* `:write_refusal_evidence :subject` carries `{:change_index nil, :change_id nil, :selector_sha256 …}`;
  §4 specifies `{:selector_sha256 …}`.
* `:at` for a `.cljc` file is `[:file]` through `execute!` but `[:source "src/x.cljc"]` through the
  pure planner, for the identical condition and error type.
* §1 says paths are "normalize[d] to root-relative slash paths"; `paths!` refuses `src/./x.clj` and
  `src//x.clj` instead of normalising. Stricter, witnessed, harmless — but it is a deviation.

### F9 — LOW. `:context` is emitted on every site and asserted by nothing

`grep -c ":context"` is **0** in `rename_alias_test.clj`, `rename_alias_parity_test.clj` and
`rename_alias_oracle.clj`. Contract §2 pins four values and the unquote rule. I probed it directly
and **it is correct**, including "unquote returns to its enclosing non-syntax-quote context":

```
'events/quoted            -> quote
#'events/varquoted        -> quote
`(events/sq ...)          -> syntax-quote
~events/unq  ~@events/unqs-> ordinary
^events/meta  ^{:k ...}   -> metadata
`[^events/metainsq z]     -> metadata
(quote events/qq)         -> quote
(comment events/x)        -> ordinary
```

A missing ratchet, not a defect. One table-driven witness closes it.

### F10 — INFO. A rename inside `(comment …)` is invisible in the receipt

Sites inside a comment macro carry context `"ordinary"`, so nothing in the summary or the site record
tells a caller that the tool edited commented-out code. That is precisely least-sure choice 1, and
the caller trial that is supposed to judge it cannot see it from the receipt. Consider a distinct
context label (or a per-file `references_in_comment` count) before the trial rather than after.

---

## Conformance table (contract §-by-§)

| § | Subject | Verdict |
|---|---|---|
| 1 | One verb, MCP + CLI shorthand + `:op`, one planner/transaction/projector | conformant — both CLI spellings and the MCP handler produce identical receipts modulo `elapsed_ms`/artifact identity (probed directly; parity witness asserts it) |
| 1 | Closed EDN schema; unknown/duplicate keys, wrong types, non-integral counts refuse `:invalid-request` | conformant (`rename_alias_plan.clj:26-53`; `{} {}`, `{:version 1 :version 1}`, `#unsafe/tag`, `#=(…)`, `{`, `:preview true` all refuse) |
| 1 | Exactly one scope selector; `paths` explicit files, distinct; `expect_files` counts ALL inspected | conformant |
| 1 | Repository enumerates `.clj/.cljs/.cljc`, excludes only `.git`, no silent CLJC exclusion | conformant for extensions — **but see F5(a)**: any symlink in the tree refuses |
| 1 | Explicit scopes require the binding in EVERY file; repository skips non-matching; zero selected → `:old-alias-absent` | conformant (probed: skip with a same-spelled alias for another library left untouched; `:old-alias-absent`, `:alias-library-mismatch` both witnessed) |
| 1 | Guards cover exactly inspected paths; file-count mismatch precedes guard coverage; drift refuses | conformant — `paths!` runs before `guards!` and returns the complete path inventory; `:scope-changed-before-commit` witnessed |
| 1 | Alias/lib token validation, `old=new` refuses, `lib == old_alias` → `:ambiguous-alias-namespace` | conformant (11 malformed aliases witnessed, plus `nil/true/false/&/_`) |
| 1 | Limits: 1,000 files, 64 MiB aggregate, 8 MiB/file, depth 512, 100,000 references, `:limit-exceeded` never truncation | conformant in code and witnessed for depth/size; the 1,000-file and 64 MiB ceilings are unreachable in practice at the measured speed (F4) |
| 2 | Symbol role, exact namespace part, prefix trap, suffix retained | conformant — `ev` vs `event/x`, `:event/x`, `::event/x`, `'event/y`, `evx/z` all correct |
| 2 | Auto-resolved keyword `::alias/kw`; literal `:alias/kw` and `::kw` byte-identical | conformant |
| 2 | `#::alias{…}` prefix counted once; implicit keys and literal `#:alias{…}` preserved | conformant, including nested namespaced maps, `#::alias` + newline + `{`, and `#::aliasx{}` (no over-match) |
| 2 | ns libspec alias token is one separate binding edit per selected file | conformant (`:as` and `:as-alias`, option keyword preserved) |
| 2 | Tag names, library names, `:refer`/`:rename` declaration names are not reference roles | conformant (`#events/tag` payload traversed, tag preserved) |
| 2 | Strings/docstrings/regex/char/`;` comments opaque; `(comment …)` is code | conformant — 743 string literals byte-identical between A and B on E4, independently verified |
| 2 | `#_` and its entire subtree opaque, zero references, no refusal | conformant, including `#_#_`, discarded `ns` forms and discarded auto-keywords |
| 2 | Site context `quote`/`syntax-quote`/`metadata`/`ordinary`; unquote returns to the enclosing context | conformant in behaviour — **unwitnessed (F9)** |
| 3 | Exactly one root ns; `:ns-not-found` / `:multiple-ns-forms` / `:unsupported-ns-shape` | conformant |
| 3 | Supported libspec grammar; `:use`/`:load`/prefix lists/unknown options refuse | conformant |
| 3 | `:ambiguous-alias-binding`, `:alias-collision`, `:new-alias-capture`, `:old-alias-absent`, `:alias-library-mismatch` | conformant — **except `:ambiguous-alias-binding` also fires for skipped files (F5c)** |
| 3 | A referred/local name equal to `new_alias` is NOT a collision | conformant (`:refer [ev]` and `:rename {q ev}` both accepted) |
| 3 | Runtime namespace mutation refuses `:unsupported-namespace-mutation` | conformant to the letter — **see F5(b) for the cost** |
| 3 | `.cljs`/`.cljc`/`#?`/`#?@`/reader-eval refuse `:unsupported-source` before any write | conformant (the `:at` differs between entrances — F8) |
| 3 | Parse failure → `:source-parse-error` with file/line/column | conformant |
| 3 | Deterministic validation order; first failure returns full evidence; all files pass before replacement | conformant, and the stage-order repair the builder logged is real (cross-file source/binding stages now complete before collision) |
| 4 | Guard union `{:sha256}` XOR `{:read_receipt}`; `:invalid-guard`; stale → `:source-hash-mismatch` with expected/actual, `refresh-source` | conformant, witnessed, and re-probed |
| 4 | `:expect-count-mismatch` with expected/actual/per_file/`write_refusal_evidence`/`mismatched_files` | conformant field for field against the §6 example |
| 4 | Selector SHA over the canonical EDN shape, shared implementation, both transports | conformant (`mcp-write-refusal/generic-count-mismatch-evidence`; underscore projection applied to keys only) |
| 4 | Sites carry line/end_line, UTF-8 byte offset/length, `{:preorder N}`, `form_index`, role | **deviation — F3** (line and preorder wrong at column 1); offsets, lengths, `form_index` and roles are correct, including multibyte sources |
| 4 | Disjoint replacements, descending offset, no reprinting/formatting | conformant |
| 4 | Reparse B, recompute roles, require planned roles/counts, zero old references, exact bytes outside authorized intervals | implemented — **UNTESTED (F1)**; "exact bytes outside authorized intervals" holds only by construction of the splice, not by an independent check |
| 4 | Durable inverse evidence with original/result offsets, before/after bytes and hashes | conformant; the inverse-identity check is a genuine byte-vs-char-offset guard (it is what makes multibyte correct) — but also unwitnessed |
| 5 | Lock through capture/plan/stage/commit/read-back; stage all before any replacement; recheck before each | conformant — two concurrent writers: 1 commit (exit 0), 1 `:source-hash-mismatch` (exit 2), no lost update |
| 5 | All-or-nothing; stale before first write → `:source-changed-before-commit`/`:scope-changed-before-commit`, no-write | conformant — repository scope with one stale guard wrote **nothing** |
| 5 | State-first serialization; refused/failed/rolled-back/recovery-required shapes | conformant at all seven injected fault stages (`:stage`, `:before-recheck`, `:read-back`, `:second-replacement`, `:external-after-write`, `:restore`, `:receipt-finalize`) |
| 5 | `:concurrency "cooperative-lock+final-recheck"`, no CAS claim | conformant (verbatim in every success receipt) |
| 5 | `:transaction` with `partial_write`, replaced/restored/pending/unresolved, `file_states` with observed digests | conformant; `observed_sha256` is a genuine fresh disk hash |
| 5 | Durable detail mandatory; `receipt_details_path` + `receipt_hash`; complete inventories | conformant and complete (E4 detail: 35 form entries = 3 changed + 32 other, all gaps and discards, full sites, inverse splices) — **but see F7 on its size and rewrite count** |
| 5 | Summary ≤ 4 KiB, payload-free; inline caps with available/returned/omitted/truncated | conformant (E4 refusal 1,889 B; E4 success 1,328 B; the 30-site refusal caps at 10 with correct counts) |
| 5 | `write_verified=true`, `verification_complete=false`, `verification {parse+byte-preservation, not-run}`, `next_action "none"` | conformant |
| 5 | V1 ALWAYS omits `terminal_response` | conformant — the key appears in none of my receipts (CLI or MCP) |
| 6 | E4 request/response, result hash `02332a74…`, 2 references at lines 153/1112, preorders 537/4661 | **conformant, byte-exact** through the production bb CLI, in a fresh root I built myself from the committed blob |
| 6 | Refusal envelope field for field | conformant apart from **F8** (`:remedy`, `:subject` extra keys) |
| 6 | X is exactly one EDN map; tags/reader-eval/duplicate keys/trailing values refuse | conformant |
| 6 | Stdout one EDN receipt + newline, diagnostics on stderr | conformant — stderr empty on every run |
| 6 | Exit 0 committed only; 2 no-write refusal; 1 I/O/rollback/recovery | conformant — measured 0, 2, 2, 2, 1 across the probe set |
| 6 | MCP returns equivalent structuredContent and state-first summary; domain refusals are ordinary results | conformant (direct handler call: `error? false`, state-first summary, identical receipt) |
| 7 | Eleven literal `deftest` names, table-driven, enrolled in the census and lane manifest | conformant — all eleven present; `lane-manifest-test` and `mcp-operation-registry-test` green |
| 7 | "Every refusal witness asserts exact error type AND all target bytes unchanged" | **deviation — F1** (two refusal types with no witness); and most witnesses assert "unchanged" against the in-memory plan result, not bytes on disk |
| 7 | Independent oracle: separately authored CST walk, no production selector/splice/hash helpers | conformant in construction; it shares `rewrite-clj` **and** the insertion oracle's `inventory` — including the map-qualifier span repair this branch added, so that seam is not independently checked by it (I checked it independently in rung C instead) |

**Contract amendments: none.** `diff` of
`/home/forge/src/clj-surgeon-records/docs/observations/2026-09-10-rename-alias-contract-astra.md`
against `docs/intent/rename-alias/contract.md` on the branch is **empty**. No refusal was weakened
and the records copy was not edited.

**Branch identity note for Sol.** The builder's report names branch `fable/rename-alias`, base
`29f81c5f`, HEAD `def6c26c`; the tree under review is `fable/rename-alias-r1` @ `a1989b12` on base
`a6e53564` — the same seven commits rebased. `git diff --stat def6c26c a1989b12` is three files
(`battery-ledger.edn`, `battery-namespace-walls.edn`, `admit_patch_test.clj`), none of them in the
rename path, so the report's gate logs bind to the code as reviewed. They were not re-run at this
tip.

---

## Rung B — independent preservation oracle (no rewrite-clj)

The builder records the shared-parser limitation, and this branch *extends* the shared seam
(`insert_forms_plan/tree` and `insert_forms_oracle/inventory` both gained the same `:map-qualifier`
span repair), so production and the acceptance oracle are correlated exactly where the new code is.
I checked it from outside.

I reused and extended the bracket-matching top-level splitter from the insert_forms review
(`/var/tmp/forge/rename-fx/redteam/forms.py`) — no rewrite-clj, no Clojure reader, tracking string
literals, `\char` literals, `;` comments and reader prefixes; I added `^metadata` prefix handling so a
`^{…} (def …)` pair is one unit as rewrite-clj sees it. Then I compared its per-form SHA-256 against
the **durable receipt's** `changed_forms` + `other_forms` entries after a real committed rename.

```
python3 /var/tmp/forge/rename-fx/redteam/rungc-cmp.py /var/tmp/forge/rename-fx/redteam

f1: python units A=8  B=8  | receipt entries=8  | disagreements=0
f2: python units A=4  B=4  | receipt entries=4  | disagreements=0
f3: python units A=35 B=35 | receipt entries=35 | disagreements=0
```

| fixture | content | result |
|---|---|---|
| f1 | ns, standalone `;;` comment naming the alias, string containing `) ] } ;` and `\n`, regex with an unbalanced `(`, `\newline`, `#_(def …)`, `(comment (defn- …))`, `^{:doc …}` on a `def` holding `#::alias{…}`, syntax-quote/unquote, `::alias/kw` vs `:alias/lit` | 8 entries, **0 disagreements** |
| f2 | multibyte (`λ ✓ ééé`) in a string, a non-ASCII symbol name, `::alias/k`, `#::alias {…}` with a space, `#alias/tag` + payload | 4 entries, **0 disagreements** |
| f3 | the E4 blob, 35 root forms, committed rename | 35 entries, **0 disagreements** |

**And the "22 route templates", counted independently.** A regex + bracket walk over the E4 result,
touching neither rewrite-clj nor the oracle:

```
occurrences of 'events/' in A: 31        in B: 29        'ev/' in B: 2
string literals A=743 B=743   identical multiset = True
strings containing events/ in B: 29
(str …) forms mentioning events/ : 29    distinct: 22
```

31 → 29 with exactly 2 becoming `ev/`; all 743 string literals byte-identical; 29 route literals;
**22 distinct route templates**. The builder's corrected count is right and the correlated-parser
risk is not realised on these shapes.

---

## Rung E — witness reintroduction

Baseline `clj-surgeon.rename-alias-test`: **10 tests / 349 assertions / 0 failures** (4.8 s). Each
defect applied to my worktree only, reverted with `git checkout -- .`.

| # | Defect reintroduced | Witness | Result |
|---|---|---|---|
| E1 | symbol match drops the `/` boundary (`rename_alias_plan.clj:166`) | `rename-alias-prefix-trap` + 5 more | **RED, right reason** — prefix trap, E4, string-decoy, binding-matrix, reader-roles all fail |
| E2 | `walk` iterates `(:children x)` instead of `(p/effective x)` | discard-decoy | GREEN — **equivalent mutant**: the sibling `(= :uneval tag) []` clause still fires |
| E3 | `form-proof` root-count/gap/discard preservation refusal deleted (`:218-219`) | all | **GREEN — finding (F1)** |
| E4 | candidate role-recount refusal deleted (`:247-249`) | all | **GREEN — finding (F1)** |
| E5 | inverse-identity refusal deleted (`:255-256`) | all | **GREEN — finding (F1)** |
| E6 | post-replacement disk read-back comparison deleted (`rename_alias.clj:160-161`) | all | **GREEN — finding (F2)** |
| E7 | ns `:require`/`:import`/… declaration skip disabled (`:170`) | all | GREEN — equivalent mutant on every shape in the suite (a declaration token can never match `alias/`) |
| E8 | `splice` writes one character left (`:207`) | prefix-trap + 6 more | **RED, right reason** — 20+ failures; production refuses `:unsupported-libspec` (see F6) |
| E9 | both discard guards removed (`(:children x)` **and** the `:uneval` clause) | `rename-alias-discard-decoy`, `rename-alias-receipt-oracle` | **RED, right reason** — 15 failures, discarded forms counted and rewritten |
| E10 | symbol site offset shifted +1 (`:166`, `[1 "symbol"]`) | prefix-trap + others | **RED** by witness; and see F1 Reproduction 2 — the *guard* refuses, and with E4 also applied it **commits `(def y eevx)`** |

Four of ten are honest ratchets going red for the reason they name; two are equivalent mutants; the
remaining four are F1/F2.

---

## Rung F — cross-verb: insert_forms on this tip

The branch changes a shared reader seam (`insert_forms_plan/tree` gained `:map-qualifier` start/end
handling from the parent node, and `insert_forms_oracle/inventory` the same).

```
/var/tmp/forge/rename-fx/redteam/runwit.sh <all 22 insert-forms namespaces>

Ran 24 tests containing 548 assertions.
0 failures, 0 errors.
test-isolation: 0 violations across 22 namespace(s)
```

Also green on this tip: `rename-alias-parity-test` + `lane-manifest-test` +
`mcp-operation-registry-test` — 29 tests / 481 assertions / 0 failures / 0 preconditions failed.
Lint through `~/bin/clj-kondo` over all seven new/changed rename files: **0 errors / 0 warnings**.

I also probed the shared seam adversarially through the new verb: nested `#::alias{#::alias{…}}`,
`#::alias` under `^{…}` metadata, `#::alias` + newline + `{`, explicit `:alias/x` keys inside an
auto-resolved map, and the non-matching `#:alias{…}` / `#::aliasx{…}` lookalikes — all correct.

---

## What I could not test, and why

* **`make test-fast` / the full 77-namespace fast lane at this tip.** It runs a parallel coordinator
  (multiple JVMs) and a cohort is running on this box at concurrency 4; I held myself to one JVM. I
  ran 5 namespaces directly (rename-alias, rename-alias-parity, lane-manifest, mcp-operation-registry)
  plus all 22 insert-forms namespaces. The builder's `test-fast-verified.log` (77 ns / 766 tests /
  8,236 assertions) is the only evidence for the rest of the repo, and it was produced on the
  pre-rebase tree.
* **A live MCP server.** Ports were out of bounds and no server may be started, so I called
  `clj-surgeon.mcp-rename-alias/handle` directly with JSON-shaped string-keyed params. Transport
  framing, catalog registration in a running server and `structuredContent` serialisation are covered
  only by the branch's own tests.
* **The installed `~/bin/clj-surgeon` launcher.** Pinned to trunk's package, which has no
  `rename_alias`. I ran the production shape instead — `bb -m clj-surgeon.core` from the worktree,
  which is the invocation the launcher makes. Whether `make install-cli` produces a working package
  from this tip is untested here.
* **`make landing-gate` / prewarm.** Make wrappers that exercise out-of-bounds surfaces; not run. The
  tip has no complete green landing gate. Do not represent it as having one.
* **Crash recovery out-of-process.** The seven injected fault stages are covered by the branch's own
  witness and I re-ran them; I did not hard-kill the JVM mid-publication as I did for insert_forms, so
  the `recovery-status` reader's verdict on a real orphaned journal is untested here. There is still
  no documented or executable recovery procedure in `docs/intent/rename-alias/`.
* **Filesystems without `unix:nlink` or POSIX permissions.** ext4 under `/var/tmp/forge` only.
* **Performance versus native.** No matched-native trial was run and none is claimed. The F4 table is
  my own single-machine measurement of the planner on a loaded box (load1 3.7–8.5 throughout); treat
  the ratios, not the absolute numbers, as the finding.
* **A Sol review.** This is not one, and nothing here is an independent final-snapshot approval of the
  branch.

---

## Recommendation

Send to Sol with **F1**, **F3** and **F5(b)** named as the three questions to rule on.

Before merge, non-negotiable:

1. **F1 + F2** — witness `:candidate-structure-mismatch` and `:candidate-parse-error` (exact error
   type **and** unchanged bytes on disk); define `other` positionally so `other_forms_unchanged` can
   be false and can carry a refusal; source `read_back_hash`/`read_back_hashes` from the disk;
   compute `details_contains`. Then have the reviewer re-delete each of the four guards in the F1
   table and require the suite to go red. Register the linked intents — a fix that introduces a
   requirement registers an intent whose witness fails first.
2. **F3** — `<=` in `annotate`, plus a witness that puts a reference at column 1 and asserts its line
   and a non-nil preorder.

Cheap and high-value, same batch: **F4** (six lines, 12.6× at 4k lines, measured, witnesses green) —
it is the difference between repository scope being usable and being theatre; and **F6** (wrap the
candidate re-derivation so a broken candidate stops calling itself a source error).

**F5 is a contract question, not a code fix**, and should be settled before the first caller trial
rather than after: a repository-scope rename of a real Clojure repo is refused today by a symlinked
README, by a `(comment (require …))` block in an unrelated file, and by a duplicate `:require` entry
in a skipped file — measured at 4/122 files for the `require` tripwire alone in this repo's own
`src/`. The narrower alias-aware tripwire, and scoping `:ambiguous-alias-binding` to selected files
as §3 already says, would remove most of it without weakening a guarantee.

F7–F10 are cheap ratchets that can follow.
