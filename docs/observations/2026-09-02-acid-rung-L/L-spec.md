# SPEC — rung L: ONE SERVER-OWNED WALL CLOCK (`marvin-voice-remote.clock`)

Benchmark rung **L** ("large"), staged 2026-09-02 by forge@bridge.
Base commit: **`ab267f9`** ("Enable safe production friction defaults: F4 on, F5 observe,
F2 stays off") in `marvin-voice-remote`.

**Task class:** mechanical cross-file refactor — hoist a repeated primitive into one
namespace and thread it through every call site, plus a required-alias change in ten `ns`
forms. **21 top-level `defn`/`def` owners across 11 namespaces.**

**Arms:** native edits vs. the `clj-surgeon` structural editor over MCP. The change is
deliberately *mechanical*: fully specified below, so two competent agents produce the same
behavior and differ only in how they got there.

---

## 0. Why this change (the standing defect it closes)

At `ab267f9` there are **22 textual occurrences of `System/currentTimeMillis` in `src/`**
(21 real forms plus one mention inside a docstring). Three namespaces have each independently
grown a private `^:dynamic *now-ms*` var so that *that namespace alone* can be frozen for a
test. The rest read the machine clock directly and cannot be frozen at all.

Consequences visible today:

- `/bridge4` renders **two** cache-bust `?v=<epoch-ms>` tokens from **two separate clock
  reads**; they routinely differ by 1 ms. A page whose bytes depend on scheduling jitter is a
  page no golden can pin without a normalizer.
- `auth/make-session-cookie` mints an issued-at stamp against one clock and
  `auth/verify-session-cookie` checks the age against another. No test can drive expiry.
- Three "the whole server is frozen" freezes exist, and none of them freeze the whole server.

**Server-owned vs client-owned — the line this change must not cross.** Everything in scope is
the **SERVER's** clock: stamps the server mints, deadlines the server enforces, and cache-bust
tokens the server renders into a page. The **CLIENT's** clock is the browser's `Date.now()`,
which appears 23 times inside the JS string literals in `channel.clj`. Those are a different
quantity, owned by a different machine, and are **out of scope** (clause 6). Nothing in this
change may be observable to a browser.

---

## 1. CLAUSE 1 — the new namespace

Create **`src/marvin_voice_remote/clock.clj`**, namespace **`marvin-voice-remote.clock`**,
with **exactly three** top-level owners and **no `:require` of any `marvin-voice-remote.*`
namespace** (it must be requirable from `blob.clj`, which sits at the bottom of the graph;
requiring anything project-local risks a cycle).

```clojure
(def ^:dynamic *now-ms-fn*
  "<docstring>"
  (fn [] (System/currentTimeMillis)))

(defn now-ms
  "<docstring>"
  ^long []
  (long (*now-ms-fn*)))

(defn fixed
  "<docstring>"
  [ms]
  (fn [] (long ms)))
```

Binding contract, exactly:

- `*now-ms-fn*` is **`^:dynamic`**, public, and holds a **0-arg fn** returning epoch
  milliseconds. Production never rebinds it.
- `now-ms` is public, 0-arity, returns a **`long`**, and reads **through the var** (so a
  binding takes effect).
- `fixed` is public, 1-arity, returns a 0-arg fn yielding `(long ms)`.
- `clock.clj` is the **only** file under `src/` allowed to name `System/currentTimeMillis`,
  and it may name it **exactly once** — so write its docstrings without that literal text.

Docstrings are yours to write; names, arities, metadata, and the single machine-clock read
are not.

---

## 2. CLAUSE 2 — every other clock read routes through it

Replace **every** `(System/currentTimeMillis)` form in `src/` (21 forms, enumerated in §8)
with **`(clock/now-ms)`**, preserving the surrounding expression exactly — same arithmetic,
same `let` bindings, same string concatenation, same position.

Each of the **ten** adopter files gains `[marvin-voice-remote.clock :as clock]` in its `ns`
`:require`. The alias is **`clock`**, not `c`, not `mvr.clock`. Insert it in the position that
preserves that file's existing require ordering.

The ten adopter files:

```
src/marvin_voice_remote/auth.clj
src/marvin_voice_remote/blob.clj
src/marvin_voice_remote/bridge3_new.clj
src/marvin_voice_remote/channel.clj
src/marvin_voice_remote/codex_app_server.clj
src/marvin_voice_remote/director_control.clj
src/marvin_voice_remote/reducer_lab.clj
src/marvin_voice_remote/reducer_session.clj
src/marvin_voice_remote/reducer/shadow.clj
src/marvin_voice_remote/sse_registry.clj
```

---

## 3. CLAUSE 3 — the three existing `*now-ms*` vars DELEGATE; they do not disappear

`sse-registry/*now-ms*`, `reducer-session/*now-ms*`, and `bridge3-new/*now-ms*` **keep their
names, their `^:dynamic` metadata, their public visibility, and their docstrings** (you may
amend the prose; you may not delete the var). Only the **default value** changes:

| var | at `ab267f9` | after |
|---|---|---|
| `sse-registry/*now-ms*` | `(fn [] (System/currentTimeMillis))` | `(fn [] (clock/now-ms))` |
| `reducer-session/*now-ms*` | `#(System/currentTimeMillis)` | `#(clock/now-ms)` |
| `bridge3-new/*now-ms*` | `#(System/currentTimeMillis)` | `#(clock/now-ms)` |

**Both directions must hold, and this is the subtle part:**

- **Delegation.** With none of the three bound, freezing `clock/*now-ms-fn*` freezes all three.
- **Local override still wins.** With one of the three bound, *its* binding governs and the
  shared clock is ignored. This is not optional: `sse_registry_test.clj`,
  `reducer_session_test.clj`, and `bridge3_new_test.clj` all bind these vars today and must
  keep passing untouched.

Reading through the var, rather than calling `clock/now-ms` at the call sites in those
namespaces, is what makes both true at once.

`reducer_session.clj` line 225 mentions `System/currentTimeMillis` **in prose inside the
`*now-ms*` docstring**. Update that sentence to say `clock/now-ms` — clause 9's sweep is
textual and does not exempt comments.

---

## 4. CLAUSE 4 — the named behavioral consequences

These are the six surfaces the acceptance suite exercises directly. Each must observe the
shared clock when it is frozen:

- **4a `auth/make-session-cookie`** — the issued-at field of `"ok|<issued-ms>|<hmac>"` is
  `(clock/now-ms)`. *Server-owned:* no client supplies or influences this stamp.
- **4b `auth/verify-session-cookie`** — the age comparison against `session-max-age-secs`
  (7 days) reads `(clock/now-ms)`. A cookie minted at `T` verifies at `T + 3 days` and fails
  at `T + 8 days`, with both times supplied only by rebinding the shared clock.
- **4c `channel/bridge4-page-html`** — both `?v=<epoch-ms>` cache-bust tokens come from the
  shared clock, so with it frozen **both are the same number**. *Server-owned:* the token is
  minted server-side and rendered into the page; the browser never computes it.
- **4d `director-control/now-ms`** — stays a **private** `defn-` with that name; its body
  becomes `(clock/now-ms)`.
- **4e `reducer.shadow/record!` and `reducer.shadow/snapshot`** — `:shadow/at` on a recorded
  row and `:shadow/exported` on a snapshot are shared-clock reads.
- **4f `reducer-lab/reducer-lab-page`** — its `"/style.css?v=<epoch-ms>"` token is a
  shared-clock read. (`reducer-lab/shadow-page` and `reducer-lab/update-session!` change the
  same way; only `reducer-lab-page` is asserted.)

---

## 5. CLAUSE 5 — behavior with nothing bound is IDENTICAL

No arithmetic, no ordering, no rounding, no unit changes. With no binding in effect, every
one of the 21 sites must produce the same value it produces today. This is a plumbing change:
the only new capability is that a test can now freeze the whole server at once.

---

## 6. CLAUSE 6 — do NOT overreach

Explicitly **out of scope**; touching any of these is a defect, not initiative:

- `java.time.Instant/now` and `System/nanoTime` (21 occurrences across `src/`). They are a
  different type with different precision; converting them would change rendered strings.
- **JS `Date.now()`** inside the page string literals (23 in `channel.clj`). That is the
  **CLIENT's** clock, running in the browser. It is not the server's and must not be routed
  through a server-side var.
- Anything under `test/`, `dev/`, `scripts/`, `mobile/`, `resources/`.
- Dependencies: add none.

---

## 7. CLAUSE 7 — the non-negotiable invariants

1. **The full suite stays green.** `bin/kaocha` at `ab267f9` is `577 tests, 7784 assertions,
   0 failures` in ~19 s. It must still be exactly that. **Do not edit, add, delete, or weaken
   any file under `test/`** — the acceptance suite is external and supplies the new coverage.
2. **Every golden is byte-identical. No golden changes.** Do not run `make golden-update`.
   `scripts/check_pages.clj` normalizes `?v=\d+`, so replacing the *source* of that number
   changes no golden byte. Verified: `make check-pages` passes unchanged.
3. No new dependencies, no `deps.edn` change, no `Makefile` change.

---

## 8. CLAUSE 8 — the owners (file:line at `ab267f9`)

**21 owners across 11 namespaces.** "Sites" are the `(System/currentTimeMillis)` forms inside
each owner.

| # | namespace | owner | owner at file:line | sites (line) |
|---|---|---|---|---|
| 1 | `marvin-voice-remote.clock` | `*now-ms-fn*` | **NEW** `src/marvin_voice_remote/clock.clj` | — |
| 2 | `marvin-voice-remote.clock` | `now-ms` | **NEW** same file | — |
| 3 | `marvin-voice-remote.clock` | `fixed` | **NEW** same file | — |
| 4 | `marvin-voice-remote.auth` | `make-session-cookie` | `src/marvin_voice_remote/auth.clj:52` | 56 |
| 5 | `marvin-voice-remote.auth` | `verify-session-cookie` | `src/marvin_voice_remote/auth.clj:60` | 69 |
| 6 | `marvin-voice-remote.blob` | `fetch-token` *(private)* | `src/marvin_voice_remote/blob.clj:46` | 60 |
| 7 | `marvin-voice-remote.blob` | `access-token` *(private)* | `src/marvin_voice_remote/blob.clj:62` | 64 |
| 8 | `marvin-voice-remote.bridge3-new` | `*now-ms*` | `src/marvin_voice_remote/bridge3_new.clj:66` | 73 |
| 9 | `marvin-voice-remote.channel` | `handle-bridge3-dictate` | `src/marvin_voice_remote/channel.clj:1395` | 1430 |
| 10 | `marvin-voice-remote.channel` | `bridge3-page-html` *(private)* | `src/marvin_voice_remote/channel.clj:1844` | 1875, 1951 |
| 11 | `marvin-voice-remote.channel` | `bridge4-page-html` *(private)* | `src/marvin_voice_remote/channel.clj:3103` | 3136, 3238 |
| 12 | `marvin-voice-remote.channel` | `code-director-page-html` *(private)* | `src/marvin_voice_remote/channel.clj:3408` | 3468 |
| 13 | `marvin-voice-remote.codex-app-server` | `await-event!` | `src/marvin_voice_remote/codex_app_server.clj:196` | 200, 209 |
| 14 | `marvin-voice-remote.director-control` | `now-ms` *(private)* | `src/marvin_voice_remote/director_control.clj:18` | 18 |
| 15 | `marvin-voice-remote.reducer-lab` | `update-session!` *(private)* | `src/marvin_voice_remote/reducer_lab.clj:253` | 260 |
| 16 | `marvin-voice-remote.reducer-lab` | `reducer-lab-page` | `src/marvin_voice_remote/reducer_lab.clj:387` | 406 |
| 17 | `marvin-voice-remote.reducer-lab` | `shadow-page` | `src/marvin_voice_remote/reducer_lab.clj:679` | 698 |
| 18 | `marvin-voice-remote.reducer-session` | `*now-ms*` | `src/marvin_voice_remote/reducer_session.clj:222` | 231 *(+ docstring prose at 225)* |
| 19 | `marvin-voice-remote.reducer.shadow` | `record!` | `src/marvin_voice_remote/reducer/shadow.clj:536` | 577 |
| 20 | `marvin-voice-remote.reducer.shadow` | `snapshot` | `src/marvin_voice_remote/reducer/shadow.clj:714` | 728 |
| 21 | `marvin-voice-remote.sse-registry` | `*now-ms*` | `src/marvin_voice_remote/sse_registry.clj:83` | 86 |

Plus **ten `ns`-form edits** (one `:require` line each) in the files of owners 4–21. Those are
not counted as owners.

**Namespaces touched (11):** `clock` *(new)*, `auth`, `blob`, `bridge3-new`, `channel`,
`codex-app-server`, `director-control`, `reducer-lab`, `reducer-session`, `reducer.shadow`,
`sse-registry`.

---

## 9. CLAUSE 9 — the terminal sweep

After the change, from the repo root:

```
grep -rn "System/currentTimeMillis" src/
```

must print **exactly one line**: the single machine-clock read inside
`src/marvin_voice_remote/clock.clj`'s `*now-ms-fn*`. No other file under `src/` — and no
comment or docstring anywhere under `src/`, `clock.clj` included — may contain that text.

---

## 10. Verify

**What the implementing agent runs (one focused namespace, not the full suite):**

```bash
bin/kaocha --focus marvin-voice-remote.bridge3-new-test
```

`--focus` still compiles every namespace under `src/` and `test/` (`tests.edn` puts `src` on
the test paths), so a compile error anywhere fails it. It runs `42 tests, 416 assertions` in
~9 s, and it is the one namespace that binds **two** of the three `*now-ms*` vars and diffs a
golden — clause 3's override contract and clause 7's golden invariant in a single run.

**What the grader runs (arm-independent):**

```bash
# 1. acceptance suite
cp acid_L_acceptance_test.clj <repo>/test/marvin_voice_remote/acid_l_acceptance_test.clj
cd <repo> && bin/kaocha --focus marvin-voice-remote.acid-l-acceptance-test
#    expect: 12 tests, 82 assertions, 0 failures

# 2. full suite, unchanged
cd <repo> && rm test/marvin_voice_remote/acid_l_acceptance_test.clj && bin/kaocha
#    expect: 577 tests, 7784 assertions, 0 failures

# 3. goldens, unchanged
cd <repo> && rm -rf target/check-pages-data && \
  clojure -J-Dmvr.data.dir=target/check-pages-data -M -e "(load-file \"scripts/check_pages.clj\")"
#    expect: ✓ check-pages: all pages parse + match golden

# 4. the sweep
cd <repo> && grep -rn "System/currentTimeMillis" src/
#    expect: exactly one hit, in src/marvin_voice_remote/clock.clj
```

**Baseline receipts, measured on a clean `ab267f9` checkout (2026-09-02, Buster):**

| check | `ab267f9` | after a correct change |
|---|---|---|
| `bin/kaocha` | 577 tests, 7784 assertions, 0 failures (~19 s) | identical |
| `bin/kaocha --focus …bridge3-new-test` | 42 tests, 416 assertions, 0 failures (~9 s) | identical |
| `check_pages.clj` | all pages match golden | identical |
| acceptance suite | 12 tests, 65 assertions, **39 failures** (10 of 12 deftests red) | 12 tests, 82 assertions, 0 failures |
| `grep -c System/currentTimeMillis src/ -r` | 22 lines / 10 files | 1 line / 1 file |

The acceptance suite's `acid-l-11` and `acid-l-12` are **guards**: they pass at `ab267f9` by
design, and go red only if the implementation overreaches (clause 6) or moves a golden byte
(clause 7).

---

## 11. Clause → assertion map

| clause | acceptance deftest |
|---|---|
| 1 | `acid-l-1-clock-namespace-has-the-pinned-shape` |
| 2, 9 | `acid-l-2-only-the-clock-file-names-system-currenttimemillis` |
| 2 | `acid-l-3-all-ten-adopters-require-and-call-the-shared-clock` |
| 3 | `acid-l-4-existing-now-ms-vars-delegate-but-a-local-binding-still-wins` |
| 4a | `acid-l-5-make-session-cookie-stamps-the-shared-clock` |
| 4b | `acid-l-6-verify-session-cookie-reads-the-shared-clock` |
| 4c | `acid-l-7-bridge4-cache-busts-come-from-the-shared-clock` |
| 4d | `acid-l-8-director-control-now-ms-delegates` |
| 4e | `acid-l-9-shadow-record-and-snapshot-stamp-the-shared-clock` |
| 4f | `acid-l-10-reducer-lab-page-cache-bust-comes-from-the-shared-clock` |
| 6 | `acid-l-11-other-time-sources-are-untouched` *(guard)* |
| 5, 7 | `acid-l-12-unbound-behavior-and-the-bridge4-golden-are-unchanged` *(guard)* |

---

## 12. Sizing note

A reference implementation was built and measured on 2026-09-02: 1 new file (33 lines),
21 form replacements, 10 `ns` edits, 1 docstring sentence. Full suite green, goldens
unchanged, acceptance 12/12. **Estimated 15–30 minutes of native editing for a strong agent**
— it is bounded by the number of edits, not by any judgment call, which is exactly what makes
it a structural-editor benchmark.
