# MEM-003 second landing — round five build record

Branch `bridge/integration-2026-09-03-mem003`, built on `694f538d` after the
round-four independent review returned **NO-GO** with two blocking findings
(`docs/observations/mem003-second-landing-round4-review-opus.md`, filed with
this round). Worktree `/home/forge/src/clj-surgeon-integ2`, proven clean at the
tip before the first edit. Fixtures under `/var/tmp/forge/mem003r5-fx`, removed
at the end. No server started, on any port.

Every item is a RED commit whose witness fails at the tip for the stated reason,
then a GREEN commit.

## §1 — the type was opaque; the NAMESPACE was not

The reviewer reproduced round three's §1a verbatim in effect with
`measured/unwrap-readings`: a public verb turning a tagged reading into a bare
number at any depth, calling `value` internally so no CALLER of it matched the
escape-hatch pattern, which enumerates laundering verbs **by name**.
`measured/field` was the same class from an already-published block, and the
branch's own §4 recovery fix called `unwrap-readings` twice in `src/` with no
allow-list entry.

The repair is at the rung above name-scanning, in two halves.

**CORRECTED IN ROUND SIX (round-five review §8): this sentence was false.** `^:private` in Clojure is a RESOLUTION CONVENTION, not a boundary: `((ns-resolve 'clj-surgeon.measured 'unwrap-readings) x)` reached it, and `(._launder r)` reached the protocol method beneath it, both with every witness green. The honest statement is *inconvenient from ordinary code, and an offence when named* -- which is what round six made true, by deriving the escape-hatch pattern from the namespace's interns, the protocol's methods and the type's fields, and by making a quoted-symbol, string or keyword spelling of the namespace a `:reflective` offence. The original claim follows, uncorrected, as the record of what was believed.

~~**Impossible from ordinary code.**~~ `unwrap-readings` is private — reachable only
from `measured`, `attach` and `partition-measured`, the three verbs that BUILD
or PARTITION a block. `field` is deleted; nothing needed a convenience verb for
reaching into a published block. The three call sites this forced:
`recovery/recover!`'s two receipts now build their block with `measured/attach`,
and `mcp-operation/measured-field` reads one with `get-in` on the well-known key.

**A complement that knows the namespace, not its names.** Three witnesses:

1. a REFLECTIVE probe over `(ns-publics 'clj-surgeon.measured)` — every callable
   public var (`ifn?`, not `fn?`, so a protocol method is not skipped for its
   runtime representation), at every declared arity, called with a tagged
   reading carrying a sentinel. The sentinel returning outside a `:measured`
   block is an offence unless the var is in `sanctioned-laundering-vars` AND
   matched by `escape-hatch-pattern` — so sanctioning also makes every `src/`
   call site cost an allow-list line;
2. the same probe over an already-published block, sanctioning only
   `measured-key` itself, so a convenience verb for reaching into a block is an
   offence;
3. a SOURCE scan asserting every `measured/<verb>` reference under the scanned
   roots names a var that is public **today**.

Witness (3) is what makes the reviewer's plant red: a private verb named in
`src/` is an offence with no list to consult.

**Declared residual.** Once a number is inside a measured block it IS a bare
number — MEM-005 requires a plain JSON number on the wire — so `get-in` reaches
it and no type can prevent that. What the witness holds is that the namespace
offers no verb making it cheap.

**Accepted residual (review §3).** `setAccessible` with a computed field name
reads the number and no textual scan can see it; a JVM without a security
manager cannot prevent reflection. Recorded at the type with the reviewer's
ruling verbatim: *"Textual scanning cannot close that, and a deliberate attacker
is not the threat model here — record it, do not chase it."*

## §2 — the clock ratchet is derived from the JDK

`(.lastModified report)` was published as `:report_written_at` at
`mcp_admit_tool.clj:737`, inside the hashed parity subject, two lines above the
read the same scan had just caught and routed. `clock-pattern` was four
hand-written spellings.

It is now DERIVED: eleven JDK time classes, every public method whose return
type is an epoch/duration number or a `java.time`/`FileTime` value and whose
name carries a time morpheme — 46 spellings including `.lastModified`,
`.lastModifiedTime`, `.toMillis`, `.toEpochMilli`, `Files/getLastModifiedTime`,
`FileTime/fromMillis`, `Instant/ofEpochMilli`, `Clock/systemUTC`. Two witnesses
guard the derivation itself: the representative spelling of every JDK time shape
must still be produced, and each is PLANTED in a receipt-publishing function and
the scan must name the form it sits in.

The blocking site is routed through `measured/file-modified-ms`, a tagged
file-mtime reading that lives in the one file allowed to read a clock raw (its
absent case is nil, because `lastModified` returns 0 for a missing file and 0
reads as the epoch rather than as "no answer"). The other twelve sites the
widened scan surfaced are lease, identity, prune and stat control, each with its
own `:control` reason — six new forms and three raised counts. No `:receipt`
entry exists; the witness still refuses one.

## §3 — a reading cannot leak through a hash

`hashCode` returned `(hash launderable)`, so `:some_field (hash r)` put a
clock-varying integer into the parity subject through no verb any scan matches.
It is now `(hash ::reading)` / `(hash ::tick)` — a keyword hash, deterministic
across runs where an identity hash would not be. Equality still consults the
number; the contract only requires equal objects to hash equally.

## Intents

`MCP-OP-TIME-006` (the public surface) and `MCP-OP-TIME-007` (the derived clock
class) are new rows in
`docs/intent/mcp-operation-contract/mcp-operation-contract-specs.md`; the audit
reads 356 specs, 0 violations (354 at `694f538d`).
