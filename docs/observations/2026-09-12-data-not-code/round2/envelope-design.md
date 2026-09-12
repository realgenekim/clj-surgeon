# Destination envelope: phase-one design for review

Status: proposed implementation design; launcher product contract supplied by
Gene is accepted. Recorded 2026-09-12T20:32:30Z. Starting subject:
`81328496d25916b096e29764128af81276360a34`, `fable/data-not-code-local`.

This is the item-1 HLD supplement. Its parents are the
[operation architecture](../../../high-level-design.md) and
[operation algebra](../../../intent/operation-algebra/operation-algebra-design.md).
The existing [artifact contract](../../../intent/alias-migration/receipt-artifacts-specs.md)
continues to own artifact-root validation and workspace isolation. The binding
build brief is `/var/tmp/forge/datacode-fx/brief-astra-datacode.md`; Gene's round-2
message supplies the previously missing launcher contract.

## Authority and lifetime

The trusted operation context gains this value:

```clojure
:destination-envelope
{:id <sha256-of-roots-vector>
 :roots [<absolute-path> ...]
 :source :launcher} ; or :policy-default
```

The launcher constructs and retains the envelope once at process start. Its
declared roots are, in order: the disk-policy temp root (TMPDIR when set and
outside /tmp and /dev/shm, otherwise /var/tmp); the passwd home's
`.local/state/clj-surgeon`, replaced by the artifact-root override when set;
and the invocation workspace. A launcher supplying no envelope receives the
same bounded roots with `:source :policy-default`. Missing launcher authority
never denotes unrestricted publication.

The implementation will give the roots vector a deterministic encoding before
hashing (UTF-8 EDN vector, SHA-256 rendered as lowercase hex). Source selection
does not change the ID when the roots vector is identical. The root vector and
its ID are immutable invocation evidence; admission resolves paths without
rewriting that evidence.

`operation-algebra/context-keys` recognizes the trusted field and
`derive-capabilities` carries its validated value alongside effect authority.
The pure algebra does not read environment variables, passwd data, or disk.
`cli-change-context` and `mcp-change-context` carry the launcher value. CLI and
both configured MCP startup entrances must establish it before publication.
An internal caller's narrower envelope is preserved, never merged with the
policy default.

Public request schemas do not gain this field. A request carrying it is refused
as an unknown request field at its own entrance. Selecting an artifact root,
receipt path, or request workspace cannot grant additional destination roots.

## Admission before effects

The shared artifact boundary admits the concrete requested destination against
resolved envelope roots before creating any directory or publishing bytes.
Resolution follows symlinks through existing ancestors while retaining absent
suffix components. Containment compares path components, not string prefixes.
This covers missing parents and symlinks beneath an otherwise admitted root.

The artifact boundary retains its existing ownership, disk classification,
workspace isolation, and descendant checks. Admission is additional authority;
it does not turn an admitted root into permission to bypass those checks.

Outside-envelope refusal has the complete data:

```clojure
{:error-type :write-outside-envelope
 :effect <publication-family>
 :path <requested-destination>
 :resolved-path <resolved-destination>
 :envelope-id <id>
 :roots <declared-roots>}
```

The ordinary receipt publication family uses `:receipt-publish`. Each other
covered family retains an explicit effect identity. An admission refusal
creates nothing and leaves pre-existing outside bytes unchanged. Successful
receipts expose `:envelope-id`, binding the observed write to its authority.

`append-telemetry!` currently validates the artifact root, calls `mkdirs`, then
checks the ledger symlink. Its new ordering admits both the final directory
and ledger destination and performs the symlink check before `mkdirs`. Ledger
failure remains separate from the migration's actual commit state; an already
committed mutation must not be reported as a pre-write refusal.

The initial native inventory found artifact-boundary consumers for workspace
locks, alias migration, helper extraction, namespace split, require change,
rename alias, insertion, extraction, edit/change receipts, patch admission,
change buffers, and typist bookkeeping. Implementation must follow each
consumer to its concrete publication target; checking only the base root or
workspace-derived directory is insufficient when a final filename is a symlink.

Coverage is the shared artifact boundary and its publication families.
Landlock remains unchanged. This does not claim to confine every writer,
arbitrary subprocess effects, or adversarial filesystem races between checking
and publication.

## Acceptance and next phase

The next phase will link atomic EARS promises and refusal registration to
independent witnesses. Expected roots and refusal fields will be literals,
not answers computed by the implementation being tested. Required witnesses:

- Request-carried authority is refused as an unknown request field.
- A trusted envelope narrower than the policy default denies a real owned,
  disk-backed destination without creating its directory or changing bytes.
- Missing launcher authority supplies exactly the bounded policy default.
- Missing parents and a symlink escape are refused before creation.
- Authorized real publication succeeds and reports the matching envelope ID.
- Ledger directory and final-file escapes are rejected before `mkdirs`.
- The publication-family inventory prevents a directory-only check from
  silently omitting final target admission.

After the required phase review, red witnesses are committed before production
changes; their actual failures and subsequent green runs become receipts under
this round2 directory. Items 2–6 remain in the brief's order. No probe
measurement is authorized to run: item 5 is preregistration only.
