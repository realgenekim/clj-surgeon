# Held-out acceptance check — `Edit → Dequote/Format`

`check.sh <clone-dir>` — an INDEPENDENT behavioural gate for the replay arms of the
clj-surgeon tweezer program. It never runs a test the arm wrote, never reads the arm's
report, and never trusts suite-green. It boots the arm's app and drives the feature over
HTTP, reading every verdict back out of the page the app serves.

Written after arm **G2 passed the repo's own suites with the feature ABSENT**.

```
./check.sh /home/forge/tmp/replay/smw-T4
ACCEPT smw-T4 ok=4/4                       # exit 0
REJECT smw-base failed=<names>             # exit 1
```

## How it runs the app

- The clone is **copied** to `/var/tmp/forge/accept-fx/<name>-run` (`cp -a`) and deleted
  after; the clone itself is never written to, never `git`-touched.
- Server: `ENV=dev clojure -J-Xmx512m -J-Dwriter.test=1 -M:dev -m writer.core --port <P>`,
  `TMPDIR=/var/tmp/forge`. `-Dwriter.test=1` turns the ROOT of `writer.state/*io-enabled*`
  OFF, so the app performs **no file I/O at all** — required here because a loaded session
  can hold a FILE document whose workspace lives outside the copy.
- Port: first free port from 8200 upward by `ss -ltn`, never 7888/7890/7894/7895/8171/8173/8174.
- Ready = `GET /` returns 200 (up to 240 s). Everything runs in the foreground; the server
  is killed and the copy removed on every exit path (`trap`).
- Receipts: `logs/<clone>.log` (server) and `logs/<clone>.probe.txt` (probe output:
  discovered route, payload spelling, refusal status, failures).

## Discovery, not implementation knowledge

The check is written against the **contract in the task prompt**, not against the reference
clone's code. Both the route and the request shape are DISCOVERED:

- **Routes**: every `"/api/…"` string in the clone's own `src/writer/routes.clj` whose name
  mentions `dequote|unquote|format`, plus a built-in candidate list (and
  `/api/transform/format` last, so an arm that merely widened the existing whole-document
  Format is still *reached* — and then fails `outside-preserved`).
- **Payload spellings** tried in order until one returns 2xx: `selection-start/-end` (kebab),
  `selection:{start,end}` (nested), `selection:{selection-start,selection-end}`,
  `selectionStart/End` (camel), `selection_start/_end`, bare `start/end`, and the offsets
  inside `sync`. Measured in the field: kebab (most arms), nested (N3, P, T1b), camel (T1, T6).
- The `sync` snapshot is built by the check itself; when the server answers 409 with a
  `server-editor-key`, the check adopts that key and retries — the app tells it its own
  identity fence, no implementation detail is assumed.

## The fixture and the four assertions

Document installed via `POST /api/sync-draft`, then read back from the served page
(`<textarea id="draft-editor">`, HTML-unescaped) — the readback is the authority:

```
KEEP-BEFORE
> outside quoted line        <- OUTSIDE the selection, and quoted on purpose
> alpha beta                 <- selection start
> gamma delta                <- selection end
KEEP-AFTER
```

| id | name | assertion |
|----|------|-----------|
| A1 | `selection-transformed` | 2xx, and the selection region comes back **dequoted AND formatted**: no line still begins with `>`, the words survive in order, and it is not merely `alpha beta\ngamma delta` (dequoted but not run through the repo's mechanical formatter). |
| A2 | `outside-preserved` | The document after the command **starts with the exact prefix bytes and ends with the exact suffix bytes** — including the quoted line OUTSIDE the selection. A whole-document implementation dequotes that line and fails here. |
| A3 | `empty-selection-refused` | Fixture re-established, then the **same route and payload shape** posted with `start == end`: the response must be non-2xx (or 2xx carrying an explicit refusal) **and** the document read back must be byte-identical. |
| A4 | `menu-command` | `GET /` markup carries a `dequote` command **inside the `id="app-menu-edit"` region** (not merely somewhere on the page). |

A legitimate alternative design is accepted: if the document is unchanged server-side but
the 2xx body carries the dequoted+formatted selection text (client splices it), A1/A2 pass
and the receipt records `client-splice contract`. Setup failure (cannot install the fixture)
exits 2 and reports `could-not-drive` — it is never silently counted as a pass.

## Results — 2026-09-04, every clone under /home/forge/tmp/replay

| clone | verdict | route / payload discovered | notes |
|---|---|---|---|
| smw-G   | ACCEPT 4/4 | dequote-format / kebab  | |
| smw-G2  | **REJECT** | (none) fell through to `/api/transform/format` | A2 outside-preserved, A3 empty-selection-refused (200), A4 menu-command — **feature absent** |
| smw-G3  | ACCEPT 4/4 | dequote-format / kebab  | |
| smw-GN  | ACCEPT 4/4 | dequote-format / kebab  | |
| smw-GN2 | ACCEPT 4/4 | dequote-format / kebab  | |
| smw-GN3 | ACCEPT 4/4 | dequote-format / kebab  | |
| smw-N   | ACCEPT 4/4 | dequote-format / kebab  | |
| smw-N2  | ACCEPT 4/4 | dequote-format / kebab  | |
| smw-N3  | ACCEPT 4/4 | dequote-format / nested | empty selection refused 422 |
| smw-N5  | ACCEPT 4/4 | dequote-format / kebab  | |
| smw-NC  | ACCEPT 4/4 | dequote-format / kebab  | |
| smw-NS  | ACCEPT 4/4 | dequote-format / kebab  | |
| smw-P   | ACCEPT 4/4 | dequote-format / nested | |
| smw-T1  | ACCEPT 4/4 | dequote-format / camel  | |
| smw-T1b | ACCEPT 4/4 | dequote-format / nested | |
| smw-T1c | ACCEPT 4/4 | dequote-format / kebab  | |
| smw-T1C | ACCEPT 4/4 | dequote-format / kebab  | |
| smw-T2  | ACCEPT 4/4 | dequote-format / kebab  | |
| smw-T3  | ACCEPT 4/4 | dequote-format / kebab  | |
| smw-T3b | ACCEPT 4/4 | dequote-format / kebab  | |
| smw-T4  | ACCEPT 4/4 | dequote-format / kebab  | reference clone |
| smw-T4b | ACCEPT 4/4 | dequote-format / kebab  | |
| smw-T5  | ACCEPT 4/4 | dequote-format / kebab  | |
| smw-T6  | ACCEPT 4/4 | dequote-format / camel  | first run hit a transient tools.deps classpath race; passed on retry |
| smw-T6b | ACCEPT 4/4 | dequote-format / kebab  | |
| smw-X   | ACCEPT 4/4 | dequote-format / kebab  | |
| smw-base     | **REJECT** (negative control) | `/api/transform/format` | feature absent, as expected |
| smw-base2    | **REJECT** (negative control) | `/api/transform/format` | feature absent |
| smw-contract | **REJECT** (negative control) | `/api/transform/format` | feature absent |

All 25 feature arms ACCEPT 4/4. The three baselines and G2 REJECT with the identical
failure triple, which is the check's own calibration: **it cannot tell G2 apart from an
untouched baseline, because there is nothing to tell apart.**

Every clone's `git status --porcelain` count was identical before and after the run.
