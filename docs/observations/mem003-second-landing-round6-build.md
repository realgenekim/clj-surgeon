# MEM-003 second landing — round six build record

Branch `bridge/integration-2026-09-03-mem003`, built on `dc6ee93f` after the
round-five independent review returned **NO-GO** with two blocking findings and
three non-blocking ones. The review is committed alongside this file as
`mem003-second-landing-round5-review-opus.md`.

The round-five diagnosis, one sentence: *the type is opaque, the namespace's
public surface is probed, but the REACHABILITY of the laundering machinery is
neither* — and the requirement itself, `MCP-OP-TIME-006`, encoded the hole,
being scoped to "a public var" and to the `measured/` spelling.

## The requirement first

`MCP-OP-TIME-006` was amended before any witness was written, because a witness
that implements the old requirement faithfully still passes both attacks. It
now says: no expression under `src/` or `dev/experiments` shall obtain the
number a tagged reading holds by ANY route — a public var, a private var reached
by var-quote / `ns-resolve` / `resolve` / `find-var` / `intern` /
`requiring-resolve`, the protocol method as munged or unmunged interop, or a
field by reflection — except at a site the escape-hatch allow-list names; the
set of spellings shall be DERIVED rather than enumerated; and naming the
namespace in any form other than the sanctioned require is an offence, slash or
no slash.

## The two blockers

**Finding 1 — `(._launder r)`.** Every alternative of `escape-hatch-pattern`
was anchored on the literal text `measured/`, and a protocol method compiles to
a MUNGED Java interface method, so the sanctioned door was reachable as plain
interop with no namespace token at all. The pattern is now derived from three
reflective sources: the namespace's INTERNS (public and private, filtered by
the sentinel probe and by a zero-argument number test), the protocol's METHODS
from its `:sigs` map — emitted raw AND munged — and the opaque types' declared
FIELDS, bare, `.-f` and `.f`. Eleven spellings on the JVM.

A start-tick sentinel was added to the probe pool, because a probe that only
ever hands out `Reading`s cannot see `start-nanos` launder a `Tick`.

**Finding 2 — `((ns-resolve 'clj-surgeon.measured 'unwrap-readings) x)`.**
`measured-naming-offence` fired only on `clj-surgeon.measured/`, with a trailing
slash. It gains a `:reflective` clause: the namespace spelled as a QUOTED
SYMBOL, `(quote ...)`, a STRING or a KEYWORD, or any line naming the namespace
that also calls a var-resolution API. Nothing under the scanned roots resolves a
measured var at runtime, so it is an outright offence and green on the tree from
the moment it landed.

## The three non-blocking

**Finding 3 — the clock class list.** `clock-source-classes` was itself
hand-written, and a constructor is not a method. It is now a CLOSURE from ten
named roots — "the places a program can obtain a time from nothing" — over
`java.time.temporal.Temporal` and friends, which yields twenty classes
including `OffsetDateTime` and `LocalTime`, and deliberately excludes
`Duration`/`Period` so the three literal timeouts in `src/` stay out. Statics
returning a time value are factories and need no clock morpheme
(`Calendar/getInstance`, `OffsetDateTime/now`); zero-argument constructors of
time values are spelled `Date.` with a leading word boundary; every static is
also emitted as the dot special form `(. System nanoTime`. **159 spellings, up
from 46.**

The widened scan found three unclassified reads, all `Instant/parse` over
caller-supplied strings inside boolean predicates, all classified `:control`.

**Finding 4 — value equality.** Declared as the second accepted residual in
`measured.clj`, at the same tier as `setAccessible`, in the reviewer's own
words. The alternative the brief allowed — identity-only `=` — was checked and
rejected: the type's own witnesses depend on `(= (reading 1.5) (reading 1.5))`,
including the argument that justifies the constant `hashCode`.

**Finding 5 — Java collections.** `unpartitioned-measured-paths` now walks
`java.util.Map` and `java.util.Collection`, placed after the Clojure clauses
because Clojure collections implement both. A reading inside an `ArrayList` or
`HashMap` is now the same typed `:unpartitioned-measured-field` refusal every
other placement gets. The two REWRITING walkers still leave foreign collections
alone, deliberately and now documented: replacing a caller's `TreeMap` with a
Clojure map is a guess, and guessing is how a `ClassCastException` reached the
boundary in round three.

## The reviewer's plants, before and after

Each is `git archive` of the named commit with one line added at
`src/clj_surgeon/mcp_hot_verify.clj`, run under babashka — the runtime the
scanning gate runs in.

| plant | at `dc6ee93f` | at the round-six tip |
|---|---|---|
| `(._launder ...)` | 19 tests / 75 assertions / **0 failures** | **2 failures** — `untagged-clock verbs with no allow-list entry: (["src/clj_surgeon/mcp_hot_verify.clj" "verify!"])` |
| `((ns-resolve 'clj-surgeon.measured 'unwrap-readings) ...)` | 19 / 75 / **0 failures** | **1 failure** — `[... 115 :reflective ":verification_wall_ms ((ns-resolve 'clj-surgeon.measured 'unwrap-readings)"]` |
| the four clock spellings (`OffsetDateTime/now`, `Calendar/getInstance`, `(. System nanoTime)`, `(java.util.Date.)`) | 19 / 75 / **0 failures** | **2 failures** — `raw clock reads with no allow-list entry: (["src/clj_surgeon/mcp_hot_verify.clj" "verify!"])` |
| `@#'clj-surgeon.measured/unwrap-readings` | 2 failures (already caught) | **4 failures** (caught by four witnesses now) |

## Sabotage: every new witness proved RED

Each on a `git archive` export of the round-six tip, one defect reintroduced.

| sabotage | witnesses that went red | failures |
|---|---|---|
| `escape-hatch-pattern` back to the round-five literal | `...-carries-every-route-to-a-readings-number`, `...-scanner-catches-every-route-planted-in-a-receipt` | 5 |
| the escape-hatch derivation returns `[]` (floor only) | `...-is-derived-from-the-namespace-not-from-a-list` | 2 |
| the `:reflective` cond clause deleted | `the-require-witness-catches-a-planted-reflective-resolution` | 10 |
| constructors and the dot special form dropped from the clock derivation | `the-derived-clock-pattern-carries-every-jdk-time-shape` | 3 |
| the two Java-collection clauses deleted from the diagnostic | `a-reading-inside-a-java-collection-is-diagnosed`, `a-clock-reading-never-becomes-a-raw-number-in-any-placement` | 5 |

## Declared, not closed

- The `setAccessible`-with-a-computed-field-name residual (round four §3).
- Value equality as a bisection oracle (round five §4), now declared in
  `measured.clj`.
- The floor union in both derived patterns. Babashka is a GraalVM native image
  and carries reflection metadata only for registered classes:
  `(.getMethods (Class/forName "java.util.Calendar"))` answers 57 on the JVM and
  6 under babashka, and a `deftype`'s declared fields are not reflectable there
  at all. So two clock spellings and three field spellings are DERIVED on the
  JVM and RUN OFF THE FLOOR under babashka. The witnesses assert the derivation
  produces every floor entry on a runtime whose reflection is complete, and that
  predicate is MEASURED rather than sniffed from a property, so a future
  babashka that registers the class simply starts proving the same assertions.
- `bench/` remains a third unscanned root (round five §9). Nothing in it
  constructs an MCP result; not this branch's to close.
- The battery's `held-scales-with-n` FAILs and UNMEASURED reserved-peak lines
  are MEM-001's lane, pre-existing at the base.
