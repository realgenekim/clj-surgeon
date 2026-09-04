# `feature_thread` fixture — social-media-writer, Edit → Dequote/Format

The named test case of the `feature_thread` verb. Provenance: Gene's own
2026-09-03 codex session in `social-media-writer`
(`01a0678b-d807-7e42-ac73-db3bd41ca674`), relayed to this seat 2026-09-04 as
`/tmp/smw-dequote-format-transcript.md` (597 events) with the mayor's reading in
`/tmp/smw-five-searches-analysis.md`. The transcript's own patch is kept here
verbatim as `DEQUOTE-FORMAT.patch`.

## Two trees

* **`smw-dequote/`** — the repository as the session found it: upstream
  `realgenekim/social-media-writer` at
  `2df99c989e2dc1963161c13f7a341847c16b4deb` (`origin/main` on 2026-09-04, and
  the tree the session worked on; the session's patch was never pushed, so
  `mechanical-format-selection` is absent here — the stated difference).
* **`smw-dequote-after/`** — the same tree at the moment the feature BROKE
  ("DEQUOTE/FORMAT BLOCKED", Alt-T not firing, save failing; transcript line
  949). Built by applying the transcript patch's SIX ADDITIVE runs verbatim: the
  new `dequoteFormatSelection` JS command, the new Edit-menu item, the new
  `mechanical-format-selection` defn, the two new handler tests, the two new
  node tests, and the new `:EDITOR-DEQUOTE-016` registry row. It does NOT apply
  the patch's rewrite of `handle-format`, which no leg of the thread depends on.
  Stated here rather than hidden.

## Bytes

`MANIFEST.tsv` records, per file, whether it is EXACT upstream bytes or a
LINE-PRESERVING REDACTION (every line outside the kept ranges replaced by an
empty line, so every original line number is preserved), with the sha256 of the
fixture file and of the kept ranges joined. Three files are redacted because
they are large and only their thread-bearing regions matter:
`src/writer/routes.clj`, `docs/intent/registry.edn`, `Makefile`.

`src/writer/routes.clj` keeps one range that is not part of the Dequote/Format
thread: **L392-L445**, `(defn handle-save "POST /api/save — …" …)`. It is here
because the round-five review found the verb reporting that DOCSTRING as the
`route` leg of `saveDraft` and the thread `COMPLETE (5 of 5)`, 1,729 lines from
the real entry at L2121 — and the redaction had blanked the decoy, so the
fixture could not reproduce the live defect. A fixture that cannot hold the bug
is not a fixture for it (MCP-OP-THREAD-044).

## Ground truth (the five owners, two languages)

| leg | file | language |
|---|---|---|
| menu-caller | `src/writer/views/components.clj` | Clojure |
| js-function | `resources/public/js/editor-commands.js` | JavaScript |
| route | `src/writer/routes.clj` | Clojure |
| handler | `src/writer/handlers/transform.clj` | Clojure |
| tests | `test/writer/handlers/transform_apply_test.clj` and `test/js/browser_runtime_classic_script_test.js` | Clojure + JavaScript |

Seeds: `formatDraft`, `/api/transform/format`, `mechanical-format`.

Governance tail (not code, and the transcript shows the agent needed all of it
after it had the five owners): `docs/intent/registry.edn`,
`test/writer/intent_contract_test.clj`, the `Makefile` test target. The verb
RETURNS this, in the `rules` row, rather than refusing it.

## Warm-up meter

The human baseline in the transcript is SIX batched read rounds, with
`resources/public/js/editor-commands.js` read at FOUR guessed line ranges
(220-390, 440-500, 360-455, 1-130) before the JavaScript half of the thread was
in hand. That is the number the verb is measured against.
