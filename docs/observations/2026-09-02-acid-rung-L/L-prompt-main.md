# RUNG L — implement "one server-owned wall clock" in `marvin-voice-remote`

You are implementing a **fully specified mechanical refactor** in a Clojure repo. Everything
you need is below. There is no design decision to make: names, arities, metadata, call sites,
and the verify command are all pinned. Two agents doing this correctly produce the same
behavior.

**Working copy:** the repo checkout you were given, at commit **`ab267f9`**. Work in place.
**Do not commit, do not push, do not create branches or worktrees.** When you are done, stop
and report.

---

## Ground rules

- **Run tests exactly once, and only this focused namespace:**
  ```bash
  bin/kaocha --focus marvin-voice-remote.bridge3-new-test
  ```
  **Do not run the full `bin/kaocha`.** The focused run still compiles every namespace under
  `src/` and `test/`, so it catches any compile error; it takes ~9 s and must end at
  `42 tests, 416 assertions, 0 failures`.
- **Do not edit, add, or delete anything under `test/`.** Coverage for this change is supplied
  by an external acceptance suite you will not see. Weakening or "fixing" an existing test is
  a failure, not a repair.
- **Do not run `make golden-update`.** No golden byte may change.
- No new dependencies. No `deps.edn` or `Makefile` changes.
- If you find a defect outside this scope, **report it, do not fix it.**
- End your final message with a line of the exact form `TURNS: <n>` — see "Reporting" below.

---

## Why (the standing defect)

At `ab267f9` there are **22 textual occurrences of `System/currentTimeMillis` under `src/`**
(21 real forms, plus one mention inside a docstring). Three namespaces have each independently
grown a private `^:dynamic *now-ms*` var so that *that namespace alone* can be frozen for a
test; everything else reads the machine clock directly and cannot be frozen at all.

What that costs today:

- `/bridge4` renders **two** cache-bust `?v=<epoch-ms>` tokens from **two separate clock
  reads**; they routinely differ by 1 ms.
- `auth/make-session-cookie` mints an issued-at stamp against one clock and
  `auth/verify-session-cookie` checks the age against another; no test can drive expiry.
- Three "freeze the server" mechanisms exist and none of them freeze the server.

**SERVER-OWNED vs CLIENT-OWNED — the line you must not cross.** Everything in scope is the
**SERVER's** clock: stamps the server mints, deadlines the server enforces, cache-bust tokens
the server renders into a page. The **CLIENT's** clock is the browser's `Date.now()`, which
appears 23 times inside JS string literals in `channel.clj`. That is a different quantity on a
different machine. **Leave every `Date.now()` alone.** Nothing you do here may be observable to
a browser.

---

## CLAUSE 1 — the new namespace

Create **`src/marvin_voice_remote/clock.clj`**, namespace **`marvin-voice-remote.clock`**, with
**exactly three** top-level owners and **no `:require` of any `marvin-voice-remote.*`
namespace** (it must be requirable from `blob.clj`, which sits at the bottom of the dependency
graph; a project-local require risks a cycle).

```clojure
(def ^:dynamic *now-ms-fn*
  "<your docstring>"
  (fn [] (System/currentTimeMillis)))

(defn now-ms
  "<your docstring>"
  ^long []
  (long (*now-ms-fn*)))

(defn fixed
  "<your docstring>"
  [ms]
  (fn [] (long ms)))
```

Pinned: `*now-ms-fn*` is `^:dynamic`, public, holds a 0-arg fn. `now-ms` is public, 0-arity,
returns a `long`, and reads **through the var** so a binding takes effect. `fixed` is public,
1-arity, returns a 0-arg fn yielding `(long ms)`.

`clock.clj` is the **only** file under `src/` allowed to name `System/currentTimeMillis`, and
it may name it **exactly once** — so write your docstrings without that literal text.

---

## CLAUSE 2 — route every other clock read through it

Replace **every** `(System/currentTimeMillis)` form under `src/` (21 forms, enumerated below)
with **`(clock/now-ms)`**, preserving the surrounding expression exactly: same arithmetic,
same `let` binding, same string concatenation, same position.

Add `[marvin-voice-remote.clock :as clock]` to the `ns` `:require` of each of these ten files,
in the position that preserves that file's existing require ordering. The alias is **`clock`**.

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

## CLAUSE 3 — the three existing `*now-ms*` vars DELEGATE; they do not disappear

`sse-registry/*now-ms*`, `reducer-session/*now-ms*`, and `bridge3-new/*now-ms*` **keep their
names, their `^:dynamic` metadata, their public visibility, and their docstrings** (amend the
prose if you like; do not delete the var). Only the **default value** changes:

| var | at `ab267f9` | after |
|---|---|---|
| `sse-registry/*now-ms*` | `(fn [] (System/currentTimeMillis))` | `(fn [] (clock/now-ms))` |
| `reducer-session/*now-ms*` | `#(System/currentTimeMillis)` | `#(clock/now-ms)` |
| `bridge3-new/*now-ms*` | `#(System/currentTimeMillis)` | `#(clock/now-ms)` |

**Both directions must hold, and this is the one subtle part of the task:**

- **Delegation** — with none of the three bound, freezing `clock/*now-ms-fn*` freezes all three.
- **Local override still wins** — with one of the three bound, *its* binding governs and the
  shared clock is ignored. Not optional: `sse_registry_test.clj`, `reducer_session_test.clj`,
  and `bridge3_new_test.clj` all bind these vars today and must keep passing untouched.

Keeping the call sites in those namespaces reading **through the var** is what makes both true
at once. Do not replace in-namespace `(*now-ms*)` calls with `(clock/now-ms)`.

`reducer_session.clj` line 225 mentions `System/currentTimeMillis` **in prose inside the
`*now-ms*` docstring**. Update that sentence to say `clock/now-ms`; the clause-9 sweep is
textual and does not exempt comments.

---

## CLAUSE 4 — the behavioral consequences that will be asserted

Each of these must observe the shared clock when it is frozen:

- **4a `auth/make-session-cookie`** — the issued-at field of `"ok|<issued-ms>|<hmac>"`.
  *Server-owned:* no client supplies or influences this stamp.
- **4b `auth/verify-session-cookie`** — the age comparison against `session-max-age-secs`
  (7 days). A cookie minted at `T` verifies at `T + 3 days`, fails at `T + 8 days`, with both
  times supplied only by rebinding the shared clock.
- **4c `channel/bridge4-page-html`** — both `?v=<epoch-ms>` cache-bust tokens, so with the
  clock frozen **both are the same number**. *Server-owned:* the token is minted server-side
  and rendered into the page; the browser never computes it.
- **4d `director-control/now-ms`** — stays a **private** `defn-` with that name; its body
  becomes `(clock/now-ms)`.
- **4e `reducer.shadow/record!` and `reducer.shadow/snapshot`** — `:shadow/at` on a recorded
  row, `:shadow/exported` on a snapshot.
- **4f `reducer-lab/reducer-lab-page`** — its `"/style.css?v=<epoch-ms>"` token.
  (`reducer-lab/shadow-page` and `reducer-lab/update-session!` change the same way.)

## CLAUSE 5 — behavior with nothing bound is IDENTICAL

No arithmetic, ordering, rounding, or unit changes. With no binding in effect every one of the
21 sites produces the same value it produces today. This is plumbing; the only new capability
is that a test can now freeze the whole server at once.

## CLAUSE 6 — do NOT overreach

Out of scope; touching any of these is a defect, not initiative:

- `java.time.Instant/now` and `System/nanoTime` (21 occurrences across `src/`) — different
  type, different precision; converting them would change rendered strings.
- **JS `Date.now()`** inside page string literals (23 in `channel.clj`) — the CLIENT's clock.
- Anything under `test/`, `dev/`, `scripts/`, `mobile/`, `resources/`.

## CLAUSE 7 — non-negotiable invariants

1. The full suite stays green (the grader runs it; you do not). **No file under `test/`
   changes.**
2. **Every golden is byte-identical.** Do not run `make golden-update`.
   `scripts/check_pages.clj` normalizes `?v=\d+`, so replacing the *source* of that number
   changes no golden byte.
3. No new dependencies, no `deps.edn` change, no `Makefile` change.

---

## CLAUSE 8 — the 21 owners (file:line at `ab267f9`)

"Sites" are the `(System/currentTimeMillis)` forms inside each owner.

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

Line numbers are from `ab267f9` and are a starting map, not an oracle — re-derive them if your
edits shift the file.

---

## CLAUSE 9 — the terminal sweep you must run before reporting

```bash
grep -rn "System/currentTimeMillis" src/
```

must print **exactly one line**: the single machine-clock read inside `clock.clj`'s
`*now-ms-fn*`. No other file under `src/`, and no comment or docstring anywhere under `src/`,
may contain that text.

---

## Verify

Run these two commands, in this order, once each:

```bash
grep -rn "System/currentTimeMillis" src/          # expect: 1 line, clock.clj
bin/kaocha --focus marvin-voice-remote.bridge3-new-test   # expect: 42 tests, 416 assertions, 0 failures
```

That focused namespace is the right single meter: it compiles the whole tree, it binds **two**
of the three `*now-ms*` vars (clause 3's override contract), and it diffs a golden (clause 7).

---

## Reporting

Finish with a short report:

1. the sweep output (should be one line);
2. the focused-test result line, verbatim;
3. `git diff --stat` (do **not** commit);
4. any out-of-scope defect you found — reported, not fixed;
5. a final line of exactly this form, with no other text on it:

```
TURNS: <n>
```

where `<n>` is the number of assistant turns you took in this task, counting this final one.
