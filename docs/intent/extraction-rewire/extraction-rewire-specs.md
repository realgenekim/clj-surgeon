---
parent: extraction-rewire-design
prefix: MCP-OP
---

 #Extraction Rewiring and Non-Fatal Outline Specifications

This file is the stable intent registry for the extraction-rewiring leaf.
IDs are never reused. The status marker records whether the current code and
tests witness the requirement.

# #Target Emission Order and Header

- [x] **MCP-OP-EXTRACT-001**: When extraction compiles a target namespace, clj-surgeon shall emit the moved forms in the caller's declared `forms` order and shall not reorder them by an internal topological sort.
- [x] **MCP-OP-EXTRACT-013**: If the caller's declared `forms` order would place a moved form before a moved form it references, then clj-surgeon shall refuse before writing, name each offending pair, and publish a dependency order that satisfies the constraint ; it shall never silently reorder the caller's forms and shall never emit a target that would require a `declare`.
- [x] **MCP-OP-EXTRACT-002**: When extraction compiles a target namespace header under the minimal require policy, clj-surgeon shall omit any docstring unless the caller supplies `doc`, and when the caller supplies `doc` it shall emit exactly that text as the target docstring ; the copy-all policy shall continue to preserve the complete source header exactly, docstring included.
- [x] **MCP-OP-EXTRACT-003**: When extraction compiles a target namespace header under the minimal require policy, clj-surgeon shall retain exactly the import entries whose classes the moved forms reference, preserving source order and dropping an emptied package group and an emptied import clause ; an import clause shape it cannot prove shall refuse the extraction without changing any file.

# #Source Header Narrowing and Alias Policy

- [x] **MCP-OP-EXTRACT-004**: When extraction rewrites the source namespace, clj-surgeon shall add `[<target-ns> :as <alias>]` to its requires and shall never emit a `:refer` list for the target ; the alias shall default to the target namespace's last dot-separated segment and shall be exactly the caller's `alias` when one is supplied.
- [x] **MCP-OP-EXTRACT-005**: When extraction rewrites the source namespace, clj-surgeon shall remove each require and each import entry that the moved forms referenced and the remaining source no longer references, and shall retain every entry the remaining source still references and every entry the moved forms never referenced.

# #Caller Rewiring

- [x] **MCP-OP-EXTRACT-006**: When `rewire_callers` is enabled, which shall be the default, clj-surgeon shall alias-qualify every remaining source call site of a moved Var through the target alias, and shall leave no unqualified reference to a moved Var in the source.
- [x] **MCP-OP-EXTRACT-007**: When `rewire_callers` is enabled, clj-surgeon shall rewrite each `<source-alias>/<moved-var>` reference in every proved caller file, including test namespaces, to `<target-alias>/<moved-var>`, and shall add `[<target-ns> :as <alias>]` to that file's requires ; where a caller file's only references to the source namespace were moved Vars, it shall replace that file's source-namespace require with the target require rather than keep both.
- [x] **MCP-OP-EXTRACT-008**: When `rewire_callers` is enabled, clj-surgeon shall write the complete proved file set as one failure-atomic transaction with parse and read-back verification, and shall restore every touched file when any write or verification fails ; when `rewire_callers` is disabled, it shall change only the source and target and report the caller inventory unchanged.
- [x] **MCP-OP-EXTRACT-009**: When extraction is planned as a dry run, clj-surgeon shall preview the complete per-file effect of the same compiled plan, including the target header, the narrowed source header, and every caller rewrite, and shall write no file.

# #Argument Admission

- [x] **MCP-OP-EXTRACT-010**: If a CLI operation receives an argument its registry entry does not declare, then clj-surgeon shall refuse before dispatch with an `unsupported-arguments` refusal naming the unknown arguments and the accepted keys, and shall not execute the operation ; an operation whose registry entry declares open arguments, because it recognises historical call shapes and returns a repaired command, shall keep its own admission.
- [x] **MCP-OP-EXTRACT-012**: When extraction cannot prove the source namespace header it is narrowing, it shall leave that header exactly as the caller wrote it, report that narrowing was unavailable, and complete the extraction ; header narrowing only removes entries the extraction itself made dead, so failing to narrow shall never fail an otherwise correct extraction, while target-header minimization shall continue to refuse.
- [x] **MCP-OP-EXTRACT-011**: When extraction receives `public`, clj-surgeon shall promote exactly those selected private forms from `defn-` to `defn` in the target, and shall refuse when a named form is not a selected private form or cannot be promoted losslessly.

# #Non-Fatal Forward-Reference Analysis

- [x] **MCP-OP-LS-001**: When forward-reference analysis runs its analyzer, clj-surgeon shall judge success by whether the analyzer produced a parseable analysis payload and shall not treat the analyzer's finding-count exit status as an analysis failure.
- [x] **MCP-OP-LS-002**: If forward-reference analysis fails or is unavailable for a file whose outline parses, then the `ls` operation shall return that complete outline with `forward_refs` set to `unavailable` and one explanatory note, and shall exit zero.
- [x] **MCP-OP-LS-003**: If forward-reference analysis fails, then its refusal shall carry a non-empty diagnostic drawn from the analyzer's own output streams rather than an empty string.

# #Workspace-Scoped Namespace Derivation

- [x] **MCP-OP-EXTRACT-014**: When clj-surgeon derives a target namespace name for a workspace, it shall read the source paths from that workspace root's own `deps.edn` and express the target path relative to that root ; it shall never read the serving process's working directory or a hard-coded source-path list, so a server answering for a different checkout cannot derive a namespace from its own layout.
- [x] **MCP-OP-EXTRACT-023**: When the `plan-extraction` MCP route or the `apply_clojure_changes` extraction route resolves an explicit `workspace_root` that differs from the serving process's own configured project root, clj-surgeon shall derive `target-ns` and the target namespace form from that resolved `workspace_root` alone, including when the MCP tool is reached over its real HTTP transport, and shall derive it correctly even when a directory named `src` also exists as an ancestor above the workspace root ; it shall never copy the source namespace's docstring onto the target namespace form. (clj-surgeon-23j)

# #Receipts a Cold Reader Can Act On

- [x] **MCP-OP-EXTRACT-015**: When extraction returns a receipt, it shall publish a `header` map stating the properties its header rewrite guarantees -- `docstring` as `none`, `caller-supplied`, or `copied-from-source`; the exact `imports-kept` and `imports-pruned`; the `visibility-derived` forms promoted from private; the `alias`; and `refer` as `none` or the exact referred names -- together with a `source-header` map naming the requires and imports removed from the source ; counts alone shall never stand in for these properties, and the dry run shall publish the same map.
- [x] **MCP-OP-EXTRACT-016**: When extraction returns a receipt, every caller field shall name a STATE rather than a queue of history: `source-callers-rewired` and `external-callers-rewired` shall list what this extraction repointed, `callers-unresolved` shall list only files that require the source namespace and still need work or whose header could not be read, `complete` shall be true exactly when `callers-unresolved` is empty, and files that provably do not require the source namespace shall be reported separately as `callers-mentions-only` ; the receipt shall lead with `applied`, `target-ns` and `target-file`, shall publish `compile` with its status and the exact command when it has not compiled anything, shall name `quoted-var-references-unrewired` rather than an unexplained count, and shall keep any historical field under `history`. A dry run shall publish `applied false` and the exact command that would apply it.

# #Bounded Receipts and a Checked Apply

- [x] **MCP-OP-EXTRACT-017**: When extraction publishes a receipt, it shall carry only its ordered head and an explicit allowlist of compiled-plan fields, shall never publish a whole source file or any executor working state, and both the dry run and the apply shall obey that same allowlist ; no string value in a receipt shall exceed 2,000 characters and the encoded receipt for the reference extraction shall be under 4,096 bytes.
- [x] **MCP-OP-EXTRACT-018**: When extraction previews the new file, it shall publish the target namespace form together with each moved form's name, resulting kind and line range, and never the file's text ; the complete text shall be reachable only through an explicitly requested `preview-path`.
- [x] **MCP-OP-EXTRACT-019**: When extraction applies, it shall compile the touched namespaces as the last step of the transaction, in one bounded subprocess running the same command the receipt publishes, and report `compile` with `checked`, `ok`, the namespaces and an output tail ; a failure shall be reported honestly without auto-reverting, and shall name the receipt and command that revert it. A dry run shall report `checked false` and `will-check true`.
- [x] **MCP-OP-EXTRACT-020**: If a post-apply compile fails because the project classpath could not be resolved, or the error is raised inside a file this extraction did not change, then clj-surgeon shall report `ok :unverified` with the reason rather than `ok false`, because a receipt whose evidence source cannot see its own subject must never tell a reader to revert correct work.

# #Workspace-Declared Compile Classpath

- [x] **MCP-OP-EXTRACT-021**: When a workspace declares compile aliases in its own `.clj-surgeon.edn` under `{:compile {:aliases [...]}}`, clj-surgeon shall resolve the post-apply compile classpath with `clojure -Spath -A:<aliases joined by colons>`, shall print that same command in the receipt on both the dry run and the apply, and shall publish the aliases it used ; an explicit `compile-alias` argument shall override the declaration, and an unrecognised top-level configuration key shall never affect form classification.
- [x] **MCP-OP-EXTRACT-022**: If a post-apply compile reports `classpath-incomplete` and the workspace declared no compile aliases, then the receipt's `compile` shall publish `candidate-aliases` -- the `deps.edn` aliases that add a `test` path, sorted -- and shall reprint its command with the first candidate applied and marked `guessed`, so the reader's next call is determinate ; the receipt shall also name declaring the alias as the durable fix.
