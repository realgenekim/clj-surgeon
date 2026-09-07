# Friction ledger — dogfood2, marvin-voice-remote JSON write policy (2026-09-07)

Run: worktree /home/forge/src/mvr-dogfood2, branch fable/json-write-helper.
Outcome: task completed correctly, but **entirely natively**. The clj-surgeon MCP
server could not be used on this repository at all. 3 entries.

---

## F1 (BLOCKER) — the server refuses every `workspace_root` except its own install dir

**Exact tool text, refusal 1** (`apply_clojure_changes`, briefed alias-migration payload):

```
apply_clojure_changes
  refused · invalid-workspace-root at ["workspace_root"]

✓ source unchanged
→ pass_an_existing_absolute_workspace_root
```

**Exact tool text, refusal 2** (`inspect_clojure`, plain outline, same root):

```json
{"path":["workspace_root"],"operation":"workspace-route",
 "error_type":"invalid-workspace-root","reason":"invalid-workspace-root",
 "next_action":"pass_an_existing_absolute_workspace_root",
 "source_unchanged":true,"ok":false,
 "error":"workspace_root must be an existing directory",
 "workspace_root":"/home/forge/src/mvr-dogfood2"}
```

**What I expected.** `/home/forge/src/mvr-dogfood2` IS an existing absolute directory:

```
drwxr-xr-x 16 forge forge 4096 Sep  7 07:02 /home/forge/src/mvr-dogfood2
readlink -f  -> /home/forge/src/mvr-dogfood2      (canonical, not a symlink)
git rev-parse --show-toplevel -> /home/forge/src/mvr-dogfood2
```

so I expected the call to proceed, or to be told the *real* reason.

**What I did instead.** Probed with `workspace_root` OMITTED. That revealed the cause:

```json
{"error":"Source file does not exist","error_type":"source-file-not-found",
 "remedy":"Use an existing project-relative source path inside the configured project root.",
 "workspace_root":"/srv/fleet/shared-tools/clj-surgeon-e7f72e2"}
```

The server is pinned to its own install tree, `/srv/fleet/shared-tools/clj-surgeon-e7f72e2`,
and rejects any override. It is not that my directory does not exist — it is that the
server will not accept a root it was not configured with.

**This directly contradicts both schemas**, which document the parameter as:
> "Optional canonical absolute workspace root. **Omit to use the server default.** Preserve
> the returned workspace_root in follow-up calls."

That sentence promises the default is a fallback, not a fence.

**Seconds lost:** 94 s (apply-1-refused 07:04:12 -> apply-1 07:05:46), plus the
~59 s before it spent composing a call that could never have run. The whole tool
lane of this dogfood — its entire purpose — was lost.

**Proposed ratchets (in priority order):**
1. **Message fix (cheapest, do today).** `"workspace_root must be an existing directory"`
   is FALSE when the directory exists and is merely not permitted. Emit a distinct typed
   refusal: `workspace-root-not-permitted`, and **name the configured root in the error
   body** — the server already knows it and already returns it on the other path. Remedy
   text must become actionable, e.g. `configured_root=/srv/... ; this server serves only
   that root`. The current `→ pass_an_existing_absolute_workspace_root` sends the caller
   to re-check a fact that is already true, which is exactly what I did.
2. **Doc line.** Both schema descriptions must say plainly: *"A server started with a
   configured project root serves ONLY that root; passing any other workspace_root
   refuses."* Delete or qualify "Omit to use the server default."
3. **Default change / deployment.** A single fleet-shared server pinned to its own install
   directory can never edit a user repository — it can only edit clj-surgeon itself. Either
   the server must accept per-call roots (allowlisted), or the doctrine plate must stop
   showing `workspace_root` as a free per-call parameter.

---

## F2 — the doctrine plate names an `alias_migration` op that no verb exposes

**The misleading plate sentence** (global CLAUDE.md, "Alias migration — one whole-repository
call"), presented as a *witnessed contract* and stated to be
"a SCHEMA EXAMPLE INSTANTIATED against a fixture repository and executed there, published
byte-identical to the request that ran":

```json
{"op": "alias_migration", "workspace_root": "...",
 "from": {"lib": "maven.db", "var": "get-ds2"},
 "to": {"lib": "...", "var": "tokenize", "alias_policy": [...]},
 "scope": {"paths": ["src"]}, "expect": {"files": 2}}
```

**What I expected.** An `alias_migration` route callable on one of the four verbs — the
brief explicitly called it "the witnessed contract for exactly this."

**What I found.** I loaded all four schemas (`inspect_clojure`, `apply_clojure_changes`,
`edit_clojure`, `transform_clojure`). **None has an `op` property; none mentions
`alias_migration`, `alias_policy`, or `scope`.** All four are `additionalProperties: false`.
`apply_clojure_changes`'s `expect` requires `{changes, edits, files}` — a bare
`{"files": 7}` is not even a valid `expect`.

Note the failure mode: the server did **not** reject the unknown `op`/`from`/`to`/`scope`
keys. It accepted them silently and refused on `workspace_root` instead. Had F1 not
blocked first, I would have received a refusal about the wrong thing, or worse, a
partially-interpreted call.

**Seconds lost:** ~25 s loading two extra schemas to confirm the absence.

**Proposed ratchets:**
1. **Typed refusal:** unknown top-level keys must refuse by name —
   `unknown-parameter: op` — never be silently dropped. Silent acceptance of
   `additionalProperties:false` fields is the dangerous half of this entry.
2. **Doctrine correction:** either ship `alias_migration` or strike that block from the
   plate. A plate that certifies a request as "byte-identical to the request that ran"
   while no deployed verb can accept it is a false receipt, and it is installed in every
   seat's boot prompt.
3. Version the plate against the server build it was witnessed on (`clj-surgeon-e7f72e2`
   here), so a checker can flag a plate that outran its server.

---

## F3 — missing information the tool would have caught: the briefed site list was wrong

Not a tool defect; recorded because it is what the tool was supposed to prevent.

The brief (and its source grep) named **7 files / 19 calls**, and the migration payload
carried `"expect": {"files": 7}`. Native grep found **9 files / 21 calls** —
`app_route.clj` (1) and `tts.clj` (1) were omitted, both binding
`[clojure.data.json :as json]` and calling `json/write-str`.

Had the `alias_migration` route existed and run, `expect: {files: 7}` would have refused
against the true 9 — correct behavior, and the strongest argument FOR the route. As it
stands, the guard that would have caught the error is the one that does not exist.

Also observed: `groq.clj` and `capture_archive.clj` carry **pre-existing unused**
`[clojure.data.json :as json]` requires, left behind by the previous parse migration
(9f9cf61). Left untouched (outside my changed set), but it shows the prior alias
migration also did not clean up requires it orphaned. **Ratchet:** an alias-migration
route must report orphaned requires as a named, counted field in its receipt, not leave
them for a later lint pass.

**Seconds lost:** 0 (caught during orientation, before any edit).
