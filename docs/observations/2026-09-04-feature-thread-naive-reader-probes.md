# feature_thread — naive-reader probes on the r3 receipt (Dequote/Format, six legs, 32 KB) — 06:38Z

The gate on a receipt (vision.md): a fresh model, ONLY the receipt, 'what is your next call?'. Two readers, no repo access. Prompt: /var/tmp/forge/tweezer/probe-prompt.md (Gene's request text + the rendered receipt).

## Convergence table

| call the reader still wanted | Opus | Sol | overlay on the REAL edit | disposition |
|---|---|---|---|---|
| re-hash every leg range before editing (6 calls) | yes | yes (6 sha256sum) | the real agent never did this | SELF-INFLICTED by the receipt's assert line → r2 addendum: the gate checks shas at write time, do NOT re-read |
| selection precedent in JS (openTransformFromSelection/bulletize/expound) | yes | yes | the real agent read editor-commands.js at four ranges for exactly this | ROUND 3: co-menu-item siblings (peer commands of the same menu form → their JS definitions) |
| server-side selection contract / request keys | yes | yes | the real edit posted {sync, selection{start,end}} to the SAME route | ROUND 3: request_contract row (keys the handler reads from the body; keys the JS posts) |
| test runner / verify command | yes | yes | three calls in the transcript | ROUND 2: verify row from the Makefile |
| registry entry anchor | yes | — | inserted at L401 after the last matched entry | ROUND 2: governance form_end + anchor |
| negative evidence ('dequote' exists?) | yes | — | mechanical-format already dequotes; both readers saw it in the body | ROUND 3: absent list for probed identifiers |
| JS export/registration site | yes | — | none exists: functions are globals in a classic script | ROUND 3: say so (evidence: classic script, no export block) |
| header 'budget=32768 used=40641 … COMPLETE' reads as an overrun | yes | — | rendering | r2 addendum: name the governed number |

Both readers answered Gene's parenthetical from the bodies alone: Format Draft is whole-document, and mechanical-format already dequotes, so the new thing is SELECTION scope. Sol's diagnosis of the seed: 'also= should have included openTransformFromSelection, bulletize' — i.e. the verb should find the peer commands itself.

## Sol's answer, verbatim

The receipt proves the current command formats the whole draft. It does not contain enough evidence to safely implement selection-only behavior. `mechanical-format` already performs the requested order: dequote first, then format.

1. Calls still needed

First, the receipt explicitly requires stale-preimage checks before editing:

```bash
sed -n '102,113p' src/writer/views/components.clj | sha256sum
```

Expected: `679b2b275f0b9ad929ff59a23252c4c38bb3a83fa4432eefce621392a7583b3f`

```bash
sed -n '389,454p' resources/public/js/editor-commands.js | sha256sum
```

Expected: `ab3318a09d0910c410fb0369519f72f6d54b43193378f6db9cc1bb9c9c805612`

```bash
sed -n '2148,2148p' src/writer/routes.clj | sha256sum
```

Expected: `9bd0ce1e388ff364ba8eacdfbf5c13c9befbe51e50ab29db59f39b193bc9fe4f`

```bash
sed -n '606,680p' src/writer/handlers/transform.clj | sha256sum
```

Expected: `db6e58198a23fa5d1fc4fca7857a85330870d4fbd68c1b0e47be9adf23f0d9dd`

```bash
sed -n '349,384p' test/writer/handlers/transform_apply_test.clj | sha256sum
```

Expected: `3f09f5fff4e4c944dd5424b2b4cffc31f5bdcc4800a2cc103356784ddad70058`

```bash
sed -n '81,132p' src/writer/handlers/transform.clj | sha256sum
```

Expected: `007315c59652811d7c1c058bcdb2e2458ad01e0fb65cb4d74926dbfb19ca98a3`

Any mismatch is a refusal, per the receipt.

Then I need the missing selection contract and neighboring implementation:

```bash
rg -n -C 35 'openTransformFromSelection|expound|bulletize|selectionStart|selectionEnd|setSelectionRange|setRangeText' resources/public/js src test
```

This determines how this repository captures a selection, represents offsets, handles an empty selection, restores focus/selection, and exposes editor commands globally.

I also need the existing server-side selection-edit pattern:

```bash
rg -n -C 35 'selection-start|selection-end|selectionStart|selectionEnd|selected-text|cursor-pos|fold-editor-snapshot-and-tx!|sync-draft-tx' src/writer test/writer
```

This prevents inventing an incompatible request shape or incorrectly splicing JavaScript selection offsets on the server.

Finally, because the receipt gives no executable test command, I need to discover the repository’s test aliases:

```bash
rg -n -C 8 '(:test|kaocha|test-runner|node --test|browser_runtime_classic_script_test)' deps.edn bb.edn package.json Makefile README.md
```

I would then run the discovered relevant Clojure and JavaScript commands as a baseline before editing. Their exact commands cannot truthfully be stated from this receipt alone.

2. Patch outline

This is provisional until the selection-pattern searches establish the repository contract.

- `src/writer/views/components.clj`, within the supplied `L102-L113` range, replace the existing form anchored by:

  ```clojure
  (menu-item "app-menu-edit" "Format draft" nil
             {:onclick "formatDraft()"})
  ```

  with approximately:

  ```clojure
  (menu-item "app-menu-edit" "Dequote/Format" nil
             {:onclick "dequoteFormatSelection()"})
  ```

- `resources/public/js/editor-commands.js`, insert after the supplied `after:L454` anchor:

  ```javascript
  async function dequoteFormatSelection() {
    // begin fenced editor command
    // obtain draft-editor selectionStart/selectionEnd
    // reject/no-op with a notification when selection is empty
    // collectDraftSync()
    // POST sync plus the repository-standard selection coordinates
    // handle 409 and non-200 exactly like formatDraft
    // apply authoritative editor frame
    // verify the durable journal receipt
    // restore the prior readOnly/journal-dead state in finally
  }
  ```

  The actual selection extraction and restoration must follow the neighboring commands found by the required search.

- `src/writer/routes.clj`, insert after the supplied `after:L2148` anchor:

  ```clojure
  ["/api/transform/dequote-format"
   {:post {:handler #'transform/handle-dequote-format-selection}}]
  ```

  If the repository already has a generic selection-transform endpoint, use that instead of adding this route.

- `src/writer/handlers/transform.clj`, insert after the supplied `after:L680` anchor:

  ```clojure
  (defn handle-dequote-format-selection [request]
    ;; parse sync and validated selection coordinates
    ;; enter fold-editor-snapshot-and-tx!
    ;; split (:draft st) into prefix / selected / suffix
    ;; transformed = (mechanical-format selected)
    ;; sync prefix + transformed + suffix durably
    ;; return authoritative frame and journal headers
    ;; preserve the existing 202/409/500 and conflict behavior
  )
  ```

  Only the selected substring is passed to `mechanical-format`; prefix and suffix remain byte-for-byte unchanged. No change is needed to `mechanical-format`, because its `unquoted` binding precedes paragraph formatting.

- `test/writer/handlers/transform_apply_test.clj`, insert after the supplied `after:L384` anchor:

  Add a test with quoted, wrapped text surrounded by deliberately unformatted text. Assert that:

  ```clojure
  resulting-draft
  = untouched-prefix
    + (transform/mechanical-format selected-text)
    + untouched-suffix
  ```

  Also assert status `200`, authoritative version advancement, book-node persistence, and one tree push. Add rejection tests for malformed/out-of-range coordinates if the established selection contract requires them.

- JavaScript tests: the receipt only exposes `test/js/browser_runtime_classic_script_test.js:L90-L92`, not enough context for a safe insertion anchor. After inspecting it, add coverage for the command’s global availability and request payload, plus empty-selection behavior if supported by that harness.

3. What is missing or wrong

The receipt followed `subject=formatDraft`, which is a whole-document feature thread, although the new requirement is selection-only. It contains no selection command, coordinate contract, empty-selection behavior, or selection-edit test.

The field that should have removed the selection search is the header’s `also=` field. It should have included selection-path identifiers such as:

```text
also=/api/transform/format,mechanical-format,openTransformFromSelection,bulletize
```

There is a second independent omission: no test invocation/verification-command field is present. Consequently, the exact Clojure test command cannot be inferred safely.

4. Verification

The receipt supplies no test-running command. Its only test-related command is a read:

```bash
nl -ba test/writer/handlers/transform_apply_test.clj | sed -n '349,384p'
```

That does not execute tests.

After discovering the project’s test runner with the command in section 1, I would run:

- The focused Clojure namespace containing `transform_apply_test.clj`.
- The JavaScript test containing `browser_runtime_classic_script_test.js`.
- The repository’s full Clojure and JavaScript suites if their documented commands are available.

I would not invent exact runner syntax such as `clojure -M:test` from this receipt; it does not establish that alias or runner.
## Opus's answer, digest (full text in the seat's session transcript)

Seven calls: (1) rg for the selection commands' JS definitions; (2) rg the route table + handler defs for the selection contract; (3) rg -i dequote for negative evidence; (4) rg window./globalThis. export for the subject; (5) sed Makefile/deps.edn for the runner; (6) registry refetch near EDITOR-CONF-005; (7) six refetch+sha256sum from the assert line. Patch outline used the receipt's anchors verbatim (after:L680, after:L2148, after:L454, after:L113 in-form L92-L165, after:L384) and proposed extracting mechanical-dequote from mechanical-format L81-132. Flagged the sibling rule (adjacent-route-entry picked a positional neighbour, not a semantic one) and the budget header.
