# Friction ledger — dogfood2, clj-surgeon MCP on /home/forge/src/cc-dogfood (2026-09-07)

## F1 — BLOCKER: every call refused `invalid-workspace-root` for a real, existing directory

Exact tool text (inspect_clojure, request-1.json):

    {"path":["workspace_root"],"operation":"workspace-route","error_type":"invalid-workspace-root",
     "reason":"invalid-workspace-root","next_action":"pass_an_existing_absolute_workspace_root",
     "source_unchanged":true,"ok":false,"error":"workspace_root must be an existing directory",
     "workspace_root":"/home/forge/src/cc-dogfood"}

Expected: a match batch over three files. `/home/forge/src/cc-dogfood` exists
(`drwxr-xr-x forge forge`, readlink -f is itself, contains deps.edn, src/, .git file).
What I did instead: probed to classify, then went native for the whole task.

Probe results (all read-only, no file opened):
| workspace_root | outcome |
|---|---|
| /home/forge/src/cc-dogfood | refused invalid-workspace-root |
| /home/forge/src/cc-dogfood/ (trailing slash) | refused invalid-workspace-root |
| /home/forge/src/cc-dogfood/src | refused invalid-workspace-root |
| /home/forge/src | refused invalid-workspace-root |
| /home/forge/src/curtaincall-cfp | refused invalid-workspace-root |
| /home/forge | ACCEPTED (advanced to path validation: `invalid-source-path`) |
| (omitted) | server default `/srv/fleet/shared-tools/clj-surgeon-e7f72e2` |

So a PARENT directory is accepted while its children are refused, which falsifies the
message's own claim ("must be an existing directory"). The real cause is a
configured-root/allowlist or sandbox-visibility rule that the message never names.
Seconds lost: ~74 s of the 40-minute budget (inspect-1 07:03:49 → native-helper 07:05:02),
plus the entire tool route for this task (0 tool-committed sites).

Proposed ratchet: split the refusal into two typed errors —
`workspace-root-not-found` (stat failed) vs `workspace-root-not-permitted`
(exists but outside the configured/allowed roots) — and have the second echo the
allowed roots (or the rule) in `remedy`, e.g. `"allowed roots: /srv/fleet/…, $HOME"`.
Add a witness that stats an existing-but-disallowed directory and asserts the
message does NOT say "must be an existing directory".

## F2 — the refusal names a verb I did not call, and the text block drops fields the JSON carried

Exact tool text (apply_clojure_changes, request-2.json):

    edit_clojure
      refused · invalid-workspace-root at ["workspace_root"]

    ✓ source unchanged
    → pass_an_existing_absolute_workspace_root

Expected: the header to say `apply_clojure_changes` (the verb I invoked), and the text
block to carry at least what the inspect refusal carried — the `error` sentence and the
offending `workspace_root` value. Both are missing here, so a caller reading only the text
block cannot see WHICH root was rejected or why.
What I did instead: recorded it verbatim and fell back to native.
Seconds lost: ~10 s (apply-1 07:05:19 → native fallback 07:05:23-ish); the cost is
diagnostic, not wall.

Proposed ratchet: text ⊇ structured for refusals too (this is the known
"text block must carry the structured receipt" class). Also assert the refusal header
equals the invoked tool name — an internal implementation verb (`edit_clojure`) leaking
into a caller-facing refusal is a false lead when the caller is debugging its own arguments.

## F3 — no way to learn the server's reachable roots without provoking an error

The only way I found the configured root (`/srv/fleet/shared-tools/clj-surgeon-e7f72e2`)
was to omit `workspace_root` and read it out of a *source-file-not-found* error.
Expected: a documented, non-error way to ask "what roots can you see?".
Seconds lost: folded into F1's ~74 s.
Proposed ratchet: include `allowed_roots` (or `configured_root`) in every
`invalid-workspace-root` refusal, and mention in the tool description that
`workspace_root` must be inside a configured root rather than merely absolute and existing.

## Non-friction note
The MCP server instructions and the routing plate both assume `workspace_root` is a free
parameter naming any repo ("Substitute the task inputs: workspace_root, …"). On this seat
that is false for every repo under /home/forge/src. If that is the intended sandbox, the
plate sentence is misleading and should say the root must be server-configured.
