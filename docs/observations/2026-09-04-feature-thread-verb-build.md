# Building the `feature_thread` MCP verb — schema, receipts, and what the fleet changed

*forge@anvil, 2026-09-04T06:05:26Z. Branch `bridge/feature-thread-verb` off `origin/MCP/main`
(`a8a8079`). Gene's instruction, verbatim: "I suggest we build it, and see if any
agents use it … for clj and maybe js, bring back the forms? … reading is fast,
but don't want to swamp context window … remember the goal: 2x reading is good,
but if we can save tool calls, we rack up gains." The unit of cost is the tool
call.*

Design authority: `docs/intent/feature-thread/feature-thread-design.md`.
Requirements: `docs/intent/feature-thread/feature-thread-specs.md`
(MCP-OP-THREAD-001..013, every one `[x]` and therefore audited for BOTH an
implementation and a test witness).
Implementation: `src/clj_surgeon/mcp_feature_thread.clj` (1,527 lines).
Witnesses: `test/clj_surgeon/mcp_feature_thread_test.clj` (17 deftests,
413 assertions).
Fixture: `test-fixtures/feature-thread/smw-dequote/` and `…-after/`.

---

## 1. The schema, as built

```
feature_thread {
  subject        : string, required. An identifier, or a route when it starts with "/".
  also           : [string], further seeds for the same thread.
  scope          : { workspace_root : string, paths : [string] }
  config         : the convention set, inline. Omitted -> .clj-surgeon/feature-thread.edn
  budget_bytes   : integer, default 16384, hard cap 32768. Never silently clamped.
  include_bodies : boolean.  mode : "edit-basis" | "locations"   (aliases of each other)
  mirror         : string, the seed of the feature this one should mirror
  axis           : { name, precedents }, the one axis the new feature differs on
}
```

The convention set is DATA, never a built-in table of file names:

```clojure
{:repo-label "social-media-writer"
 :exclude-dirs ["OLD" "drafts" "compiles" "vendor"]
 :legs [{:id "menu-caller" :kind :use     :globs [...]}
        {:id "js-function" :kind :def     :globs [...]}
        {:id "route"       :kind :route   :globs [...]}
        {:id "handler"     :kind :handler :globs [...]}
        {:id "tests"       :kind :test    :globs [...]}]
 :sibling    {:rule :adjacent-route-entry}
 :governance {:globs ["docs/intent/registry.edn" "test/**/intent_contract_test.clj" "Makefile"]}}
```

Exactly five leg roles; four or six is a typed refusal naming the field. No
JavaScript parser exists anywhere in the namespace.

Per leg, when FOUND: `file`, `from`, `to`, `evidence`, `boundary`, `sha256` of
the exact body bytes, `bytes`, `body`, `anchor`, `refetch`, and up to four
secondary witnesses as ranges. When ABSENT: every search that was run, quoted as
the `rg` command that would reproduce it, plus any file it could not read with a
typed reason. Then `sibling`, `rules` (durable path, refusal statuses, INTENT
ids resolved to their registry rows, governance tail, axis, and the `assert`
line), `elided`, and the status.

---

## 2. The T1 receipt, verbatim

Hand-driven through this seat's own Surgeon server on an explicit port 8116
(`clojure -X:clj-surgeon/mcp :port 8116`), over Streamable HTTP, `tools/list`
first:

```
['inspect_clojure', 'apply_clojure_changes', 'edit_clojure', 'transform_clojure',
 'alias_migration', 'admit_clojure_patch', 'feature_thread']
```

Then `tools/call feature_thread` with only `subject`, `also` and
`scope.workspace_root` — the convention set came from the workspace's own
`.clj-surgeon/feature-thread.edn`:

```text
receipt feature-thread/v2  subject=formatDraft also=/api/transform/format,mechanical-format  root=/home/forge/src/clj-surgeon-thread/test-fixtures/feature-thread/smw-dequote  repo=social-media-writer  budget=16384B  used=32406B  text=15400B  structured=17006B  status=COMPLETE (5 of 5)
leg menu-caller  src/writer/views/components.clj L102-L113 sha256:679b2b275f0b9ad929ff59a23252c4c38bb3a83fa4432eefce621392a7583b3f evid=identifier-or-route boundary=form(parsed, member of L92-L165 top-tabs) bytes=707 anchor=after:L113 in-form:L92-L165 form=top-tabs
  found by: identifier-or-route: rg -n -e '\QformatDraft\E|\Qmechanical-format\E|\Q/api/transform/format\E' -g 'src/**/views/*.clj' -g 'src/**/views/**/*.clj' -g 'src/**/components.clj'
  BODY<<
     (menu "app-menu-edit" "Edit"
           (menu-item "app-menu-edit" "Transform selection…" "⌥T"
                      {:onclick "openTransformFromSelection()"})
           (menu-item "app-menu-edit" "Send selection to AI" "⌥E"
                      {:onclick "expound()"})
           (menu-item "app-menu-edit" "List selection" "⌥L"
                      {:onclick "bulletize()"})
           [:div.app-menu-separator {:role "separator"}]
           (menu-item "app-menu-edit" "Format draft" nil
                      {:onclick "formatDraft()"})
           (menu-item "app-menu-edit" "Undo last server edit" nil
                      {:data-star-on:click (ds/post-action* "/api/draft/undo" {})}))
  >>
leg js-function  resources/public/js/editor-commands.js L389-L454 sha256:ab3318a09d0910c410fb0369519f72f6d54b43193378f6db9cc1bb9c9c805612 evid=identifier(def) boundary=brace-window(lexed,closed) bytes=2973 anchor=after:L454
  found by: definition-shaped: rg -n -e '(?:async +)?function +(?:\QformatDraft\E|\Qmechanical-format\E)\b|(?:const|let|var) +(?:\QformatDraft\E|\Qmechanical-format\E)\s*=|(?:window|globalThis)\.(?:\QformatDraft\E|\Qmechanical-format\E)\s*=|\b(?:\QformatDraft\E|\Qmechanical-format\E)\s*[:=]\s*(?:async\s*)?(?:function|\()' -g 'resources/public/js/*.js'
  BODY<<
async function formatDraft() {
  if (!beginEditorCommand('format draft')) return;
  const draftEditor = document.getElementById('draft-editor');
  // Restore-what-you-saved: remember the readOnly value this command is about
  // to overwrite, so cleanup can hand back exactly that and never unlatch a
  // journal-dead editor.
  // INTENT: EDITOR-JDEAD-009
  // @spec EDITOR-JDEAD-009
  const readOnlyBeforeCommand = draftEditor ? draftEditor.readOnly === true : false;
  if (draftEditor) draftEditor.readOnly = true;
  try {
    const sync = (typeof collectDraftSync === 'function') ? collectDraftSync() : null;
    // INTENT: EDITOR-SNAP-011
    // @spec EDITOR-SNAP-011
    if (!sync) {
      showNotification('Format blocked: the visible editor snapshot is unavailable.', true, 9000);
      return;
    }
    const response = await postJSON('/api/transform/format', {sync});
    // A Format 409 is the OPPOSITE of a failed transform: the server refused
    // because the visible snapshot is stale, and that is resolvable.
    // INTENT: EDITOR-CONF-005
    // @spec EDITOR-CONF-005
    if (response.status === 409) {
      const raised = await raiseConflictFromResponse(response);
      if (raised === 'shown') {
        showNotification('FORMAT BLOCKED—visible text is preserved. Choose an action in the red banner.', true, 12000);
      } else if (raised === 'unusable') {
        showNotification('FORMAT REFUSED—the server rejected it and its conflict could not be displayed. Your visible text is unchanged.', true, 20000);
      }
      return;
    }
    if (response.status !== 200) {
      showNotification('Format was not committed; visible text remains intact.', true, 10000);
      return;
    }
    const frame = await response.json();
    if (!applyAuthoritativeEditorFrame(frame, 'accepted-operation')) {
      showNotification('Format committed, but its editor frame was invalid. Reload before editing.', true, 12000);
      return;
    }
    if (typeof acknowledgeDurableDraftJournal === 'function') {
      acknowledgeDurableDraftJournal(response);
    }
    // INTENT: EDITOR-DURA-007
    // @spec EDITOR-DURA-007
    if (typeof editorCommandWasDurablySaved === 'function' &&
        !editorCommandWasDurablySaved(response, sync)) {
      showNotification('Format committed, but the response carried no durable receipt for your exact text. Keep this page open and try again.', true, 15000);
      return;
    }
    showNotification('Formatted and saved.');
  } catch (error) {
    console.error('[FormatDraft]', error);
    showNotification('Format failed; visible text remains intact.', true, 9000);
  } finally {
    const currentDraftEditor = document.getElementById('draft-editor');
    // INTENT: EDITOR-JDEAD-009
    // @spec EDITOR-JDEAD-009
    if (currentDraftEditor) {
      currentDraftEditor.readOnly = readOnlyBeforeCommand ||
        (typeof draftJournalDead === 'function' && draftJournalDead());
    }
    endEditorCommand();
  }
}
  >>
leg route  src/writer/routes.clj L2148-L2148 sha256:9bd0ce1e388ff364ba8eacdfbf5c13c9befbe51e50ab29db59f39b193bc9fe4f evid=route-literal boundary=form(parsed, member of L2083-L2376 make-routes) bytes=73 anchor=after:L2148 in-form:L2083-L2376 form=make-routes
  found by: route-literal: rg -n -e '\Q/api/transform/format\E' -g 'src/**/routes.clj' -g 'src/**/routes/**/*.clj' -g 'src/**/api_paths.clj'
  BODY<<
   ["/api/transform/format" {:post {:handler #'transform/handle-format}}]
  >>
leg handler  src/writer/handlers/transform.clj L606-L680 sha256:db6e58198a23fa5d1fc4fca7857a85330870d4fbd68c1b0e47be9adf23f0d9dd evid=handler-join boundary=form(parsed) bytes=4049 anchor=after:L680 form=handle-format
  found by: handler-join: rg -n -e '\(defn-? +\Qhandle-format\E\b' -g 'src/**/handlers/*.clj' -g 'src/**/handlers.clj'
  BODY<<
(defn handle-format
  "POST /api/transform/format — one fenced, durable editor command.
   The visible browser snapshot and the mechanical rewrite commit together;
   the HTTP response carries the authoritative frame, so SSE delivery is not
   the acknowledgement."
  [request]
  (let [{:keys [sync]}
        (try (http/parse-json-body request) (catch Exception _ {}))]
    (if-not (map? sync)
      {:status 400 :body "Visible editor snapshot required"}
      (try
        (let [journal-headers (editor-journal/journal-receipt sync)
              before-draft (:draft sync)
              res (editor-dispatch/fold-editor-snapshot-and-tx!
                   sync
                   (fn [st]
                     (let [formatted (mechanical-format (:draft st))]
                       (state/sync-draft-tx
                        st {:draft formatted :allow-blank-draft? true}))))
              frame (state/editor-frame-data (:state res))
              session-ack (when (editor-dispatch/book-ack-ok? (:book res))
                            (state/save-session!))]
          (cond
            (not (editor-dispatch/book-ack-ok? (:book res)))
            (if (get-in res [:book :ack :timeout])
              {:status 202 :body "Format save outcome unknown"}
              {:status 500 :body "Format was not confirmed durable"})

            (and session-ack (not (:ok session-ack)))
            (if (:timeout session-ack)
              {:status 202 :body "Format session save outcome unknown"}
              {:status 500 :body "Format session was not confirmed durable"})

            :else
            (do
              (state/log-event! {:type "transform.format"
                                 :before-length (count (or before-draft ""))
                                 :after-length (count (:draft frame))
                                 :before-sha256 (sha256-hex (or before-draft ""))
                                 :after-sha256 (sha256-hex (:draft frame))
                                 :browser-version (:state-version sync)
                                 :server-version (:state-version frame)
                                 :editor-sync-key (:editor-sync-key frame)})
              ;; Secondary projections may refresh over SSE, but the browser
              ;; applies the editor frame from this response.
              (sse/push-book-trees!)
              {:status 200
               :headers (merge {"Content-Type" "application/json"
                                "X-State-Version" (str (:state-version frame))
                                "X-Editor-Sync-Key" (:editor-sync-key frame)}
                               journal-headers)
               :body (json/write-str frame)})))
        (catch clojure.lang.ExceptionInfo e
          (let [{:keys [reason] :as conflict} (ex-data e)]
            (if (#{"stale-version" "stale-editor" "conflict-changed"
                   "invalid-editor-state" "unsettled-editor-projection"
                   "invalid-journal-receipt"} reason)
              (do
                (state/log-event! {:type "transform.format.rejected"
                                   :reason reason
                                   :browser-version (:browser-version conflict)
                                   :server-version (:server-version conflict)})
                ;; The browser can only PRESENT a conflict it can read: the
                ;; view-model is what supplies outcome, conflict-id, actions and
                ;; cross-document. Without it a Format 409 is undisplayable and
                ;; degrades to a bare refusal notification.
                ;; INTENT: EDITOR-CONF-005
                ;; @spec EDITOR-CONF-005
                {:status 409
                 :headers {"Content-Type" "application/json"}
                 :body (json/write-str
                        (assoc (editor-conflict/view-model "transform.format" conflict)
                               :error "Format editor conflict"
                               :saved false))})
              (throw e))))))))
  >>
leg tests  test/writer/handlers/transform_apply_test.clj L349-L384 sha256:3f09f5fff4e4c944dd5424b2b4cffc31f5bdcc4800a2cc103356784ddad70058 evid=form(deftest,CALLS-handle-format) boundary=form(parsed) bytes=1997 anchor=after:L384 form=format-folds-visible-book-snapshot-and-returns-authoritative-frame
  found by: identifier-route-or-tail: rg -n -e '\QformatDraft\E|\Qmechanical-format\E|\Q/api/transform/format\E|\Qtransform/format\E' -g 'test/**/*.clj' -g 'test/*.clj' -g 'test/**/*.js' -g 'test/*.js' -g 'test/**/*.mjs'
  BODY<<
(deftest format-folds-visible-book-snapshot-and-returns-authoritative-frame
  (let [project-idx (get-in @state/app-state [:book-workshop :active-project-idx])
        project-id "format-project"
        node-id "format-node"
        server-text "server older text"
        visible-text "Browser latest line one\nline two.  "
        node (state/make-book-node {:id node-id :title "Format me" :level 0
                                    :draft server-text})]
    (swap! state/app-state
           (fn [st]
             (-> st
                 (assoc :draft server-text :context "ctx" :leftovers "left"
                        :state-version 40)
                 (assoc-in [:book-workshop :projects project-idx :id] project-id)
                 (assoc-in [:book-workshop :projects project-idx :nodes] [node])
                 (assoc-in [:book-workshop :editing-node]
                           {:project-id project-id :node-id node-id
                            :project-idx project-idx :node-idx 0}))))
    (let [tree-pushes (atom 0)]
      (with-redefs [sse/push-book-trees! #(swap! tree-pushes inc)
                    sse/push-draft-sync-conflict! (constantly nil)
                    state/log-event! (constantly nil)]
        (let [sync {:draft visible-text :context "ctx" :leftovers "left"
                    :cursor-pos 0 :state-version 40
                    :editor-sync-key "book-node:format-node"}
              response (transform/handle-format (json-request {:sync sync}))
              frame (json/read-str (:body response) :key-fn keyword)
              expected (transform/mechanical-format visible-text)]
          (is (= 200 (:status response)))
          (is (= 41 (:state-version frame)))
          (is (= expected (:draft frame)))
          (is (= expected (:draft @state/app-state)))
          (is (= expected
                 (get-in @state/app-state
                         [:book-workshop :projects project-idx :nodes 0 :draft])))
          (is (= 1 @tree-pushes)))))))
  >>
also tests test/js/browser_runtime_classic_script_test.js:L90-L92 evid=identifier refetch=nl -ba test/js/browser_runtime_classic_script_test.js | sed -n '90,92p'  — BODIES ELIDED reason=rank(secondary witness)
sibling /api/transform/set-model rule=adjacent-route-entry at=src/writer/routes.clj:2147  legs: menu-caller ABSENT · js-function ABSENT · route FOUND src/writer/routes.clj:L2147-L2147 sha256:6e3c499c4db3464e506841e004b11bb345b004a69cd01c410f13a23efda2b104 refetch=nl -ba src/writer/routes.clj | sed -n '2147,2147p' · handler FOUND src/writer/handlers/transform.clj:L702-L710 sha256:2ec41269da8583c47c99dada1cffda328fef17d3e7ba860ca0835edfff791c0d refetch=nl -ba src/writer/handlers/transform.clj | sed -n '702,710p' · tests ABSENT  — BODIES ELIDED to ranges
rules durable_path=["http/parse-json-body" "editor-journal/journal-receipt" "editor-dispatch/fold-editor-snapshot-and-tx!" "state/sync-draft-tx" "state/editor-frame-data" "editor-dispatch/book-ack-ok?" "state/save-session!" "state/log-event!" "sse/push-book-trees!" "json/write-str" "editor-conflict/view-model"] refusal_statuses=["200" "202" "400" "409" "500"] intents=["EDITOR-JDEAD-009" "EDITOR-SNAP-011" "EDITOR-CONF-005" "EDITOR-DURA-007"]
  governance docs/intent/registry.edn:240  {:id :EDITOR-SNAP-011  refetch=nl -ba docs/intent/registry.edn | sed -n '236,260p'
  governance docs/intent/registry.edn:268  :rationale "2026-08-21 probe finding 8: saveDraft, saveBackToBoo  refetch=nl -ba docs/intent/registry.edn | sed -n '264,288p'
  governance docs/intent/registry.edn:270  {:id :EDITOR-JDEAD-009  refetch=nl -ba docs/intent/registry.edn | sed -n '266,290p'
  governance docs/intent/registry.edn:299  :rationale "2026-08-21 probe findings 5 and 6: the journal's onE  refetch=nl -ba docs/intent/registry.edn | sed -n '295,319p'
  governance docs/intent/registry.edn:311  different and lossier change, and EDITOR-JDEAD-009 has already m  refetch=nl -ba docs/intent/registry.edn | sed -n '307,331p'
  governance docs/intent/registry.edn:382  {:id :EDITOR-CONF-005  refetch=nl -ba docs/intent/registry.edn | sed -n '378,402p'
assert before any edit, re-hash each leg's line range and compare to its sha256; a mismatch is a REFUSAL (stale pre-image), never a retry
 structured-only · elapsed_ms=247.453042 · conventions_source=".clj-surgeon/feature-thread.edn" · operation="feature_thread" · mode="edit-basis" · legs.0.hit_line=111 · legs.0.refetch="nl -ba src/writer/views/components.clj | sed -n '102,113p'" · legs.1.refetch="nl -ba resources/public/js/editor-commands.js | sed -n '389,454p'" · legs.2.refetch="nl -ba src/writer/routes.clj | sed -n '2148,2148p'" · legs.3.refetch="nl -ba src/writer/handlers/transform.clj | sed -n '606,680p'" · legs.4.also.0.bytes=109 · legs.4.refetch="nl -ba test/writer/handlers/transform_apply_test.clj | sed -n '349,384p'" · sibling.legs.2.anchor="after:L2147 in-form:L2083-L2376" · sibling.legs.3.bytes=396 · sibling.legs.3.anchor="after:L710"
elapsed 247.45 ms
=== isError: False
=== structured bytes: 17446

```

`isError: false`. Server stopped after the drive.

---

## 3. Receipt sizes, T1–T5 and the Dequote/Format pair

Budget applies to the TEXT block. `structured` is reported beside it and guarded
separately by the trunk's `public-byte-budget` (32,640). The reason is this
verb's own MCP-OP-THREAD-012 guarantee: the text is a SUPERSET of the structured
content, so the structured half is a machine-readable copy of bytes already
counted. Budgeting the sum would halve the real payload to buy nothing. Gene's
brief said "measure the FINAL rendered receipt (text and structured)": both are
measured and both are reported; the deviation is which one the budget gates, and
it is stated here rather than buried.

| thread | tree | status | text B | structured B | total B | elisions |
|---|---|---|---|---|---|---|
| **T1** `formatDraft` | smw-dequote (fixture) | COMPLETE 5/5 | **15,400** | 17,006 | 32,406 | none |
| **T2** aliased JS | smw-t2 | COMPLETE 5/5 | 15,909 | 17,539 | 33,448 | none |
| **T3** assembled route | smw-t3 | COMPLETE 5/5 | 15,882 | 17,481 | 33,363 | none |
| **T4** `ackReply` | marvin-voice-remote | COMPLETE 5/5 | 11,176 | 12,707 | 23,883 | none |
| **T5** `streamAction` | marvin-voice-remote | **INCOMPLETE 3/5** | 6,248 | 7,636 | 13,884 | none |
| **Dequote/Format, after the break** | smw-dequote-after | COMPLETE 5/5 | 14,621 | 16,188 | 30,809 | none |

Every leg was checked against the frozen E-THREAD truth
(`/home/forge/tmp/arms/ethread/frozen/truth.tsv`), and the committed fixture
reproduces the real clone byte for byte: all five T1 leg hashes are identical
between `test-fixtures/feature-thread/smw-dequote` and a fresh clone of
`realgenekim/social-media-writer` at `2df99c98`.

**T2** — the alias is followed one hop:
`evid=identifier(def, one hop: alias at resources/public/js/editor-commands.js:389 -> runDraftFormatter)`,
body from `resources/public/js/editor-format.js:3-68`. The frozen truth's hidden
leg. When the target does NOT exist the leg is `ABSENT evid=alias-only` with
both searches quoted, and the status drops to INCOMPLETE — the alias line is
never presented as the implementation.

**T3** — the literal route appears nowhere in the tree, and the leg is found at
`src/writer/routes.clj:2149` with `evid=route-assembled`, the weaker evidence
labelled as weaker.

**T5 is the honest one.** Its two hidden legs are genuinely unreachable from the
seeds, and the receipt says `INCOMPLETE (3 of 5)` and names them. It does not
claim them. But the `rules` row carries
`intents=["MVR-DIRECTOR-CONTROL-LEASE-001"]`, read from the `// INTENT:` comment
two lines above `function streamAction` — the E-THREAD T5 finding, now
mechanical. A search on the identifier or the route reaches neither hidden leg;
the comment does.

---

## 4. The named test case: SMW `Edit → Dequote/Format`, and the five assertions

Ground truth, seeds `formatDraft` / `/api/transform/format` / `mechanical-format`:

| result | assertion |
|---|---|
| **PASS** | **(1) RECALL** — all five owners from one seed set in ONE call, each at the leg the truth assigns it: `components.clj` (menu), `editor-commands.js` (JS bridge), `routes.clj` (route), `handlers/transform.clj` (handler), `transform_apply_test.clj` (handler tests). The sixth owner, `test/js/browser_runtime_classic_script_test.js`, is carried as the ranked secondary test witness. |
| **PASS** | **(2) RANGES, not files** — the JS leg is `L389-L454`, the body is byte-identical to that slice, `boundary=brace-window(lexed,closed)`; the route leg is the ONE line `L2148`, not a window; the handler is `form(parsed)` `L606-L680`. The transcript read `editor-commands.js` at FOUR guessed ranges to get this. |
| **PASS** | **(3) NO FALSE MEMBERS** — every file the receipt names anywhere (legs, secondaries, sibling, governance) is inside the frozen allowed set; the test prints any extra by name. |
| **PASS** | **(4) TYPED REFUSAL** — with `editor-commands.js` chmod'd unreadable on a scratch copy, the JS leg is `ABSENT`, `unreadable: editor-commands.js (unreadable)`, status `INCOMPLETE (4 of 5)`, `legs_missing ["js-function"]`, and the text block does not contain the string `COMPLETE (5 of 5)`. A second witness deletes the directory entirely and gets the same shape. |
| **PASS** | **(5) WARM-UP METER** — below. |

**The warm-up meter, reported honestly.** The human baseline in the transcript is
**six batched read rounds**, with one file read at four guessed line ranges. The
verb's number on this fixture is **ONE round**: one call returns all five bodies,
and there is nothing the caller must read afterwards — `bodies=5`,
`missing-after-receipt=0`, so the count is 1 and not one-plus-what-you-already-knew.
That is a measurement of ROUNDS TO A COMPLETE THREAD on this fixture. It is **not**
a claim that a real Dequote/Format edit costs one tool call: the write, the test
run and the governance edits are still ahead, and the adoption cohort that would
measure calls-to-a-gate-green-edit has not been run. Predicted 10 → 4 with the
verb routed; unmeasured.

**The governance tail: RETURNED, deliberately.** Gene's amendment asked for a
decision. The verb returns it, in the `rules` row: `docs/intent/registry.edn`
rows for `EDITOR-SNAP-011`, `EDITOR-JDEAD-009`, `EDITOR-CONF-005`,
`EDITOR-DURA-007`, plus `test/writer/intent_contract_test.clj` and the `Makefile`
test target when they mention the subject or one of those ids — as ranges with a
`refetch` command, never inlined whole. The reason: the transcript shows the
agent discovering the registry, the contract test and the Makefile target
AFTER it had the five owners (lines 213–236), a second thread nothing in the
five-leg receipt would have pointed at.

**The second fixture — the moment it broke.** `smw-dequote-after` is the same
tree with the transcript's own patch applied (its six additive runs, verbatim;
the patch is committed beside the fixture as `DEQUOTE-FORMAT.patch`, and the
`handle-format` rewrite it also contains is NOT applied — stated in the fixture
README, because no leg depends on it). On that tree the thread for
`dequoteFormatSelection` is COMPLETE 5/5, the JS leg is
`async function dequoteFormatSelection() {`, the menu leg contains
`Dequote/Format`, and `EDITOR-DEQUOTE-016` appears in `rules.intents` AND is
resolved to `docs/intent/registry.edn:402`.

---

## 5. What the fleet poll changed

Taken:

1. **Name `feature_thread`** (both seats; the caller's own coinage).
2. **Edit basis, not a richer locator** — bodies, not just locations.
3. **`sha256` per leg over the exact byte range, plus an `assert` line** in the
   receipt: re-hash before any edit, a mismatch is a refusal for a stale
   pre-image, never a retry. This is what makes the receipt a write instrument.
4. **`anchor=` per leg** — `after:L454`, `after:L2148 in-form:L2083-L2376`. The
   insertion point is the one fact neither a search nor a body carries.
5. **JS by LEXER, never a parser** — states for single and double quotes,
   template literals with `${}` interpolation, line and block comments, and
   regex literals; `evid=... boundary=brace-window(lexed,closed)`; ceiling 400
   lines; automatic downgrade to `line-window(+/-40, unclosed at L<n>)`. Six unit
   witnesses, one per lexical hazard, plus an unclosed-body witness. The
   regex-versus-division ambiguity fails INTO the downgrade, never into a wrong
   answer.
6. **The test leg is RANKED by evidence kind** — `form(deftest,CALLS-handle-format)`
   outranks `form(deftest,string-assert)`; only the top witness carries a body,
   secondaries are range + kind + `refetch`. This is E-THREAD truth-correction #3
   made mechanical: the frozen oracle named the lint test, three agents named the
   test that actually calls the handler, and the agents were right.
7. **Elision order** `secondary-tests → tests → sibling → menu → js-function →
   route → handler`, each cut printed as
   `elided <leg> <bytes>B reason= range= sha256= refetch=<command>`; never a
   mid-form cut; when every body is gone and it still does not fit, the verb
   REFUSES.
8. **The `rules` row as a second receipt mode** — the durable path the SMW
   handler routes through (`editor-dispatch/fold-editor-snapshot-and-tx!`,
   `editor-journal/journal-receipt`, `state/sync-draft-tx`), the statuses it
   refuses with (`400 409 …`), the INTENT ids resolved to registry rows.
9. **`mode: "edit-basis" | "locations"`** as an alias of `include_bodies`.

Taken with a stated modification:

10. **The budget.** Three different numbers arrived (Gene: 12,288 default /
    32,768 cap; coordinator round 1: 16,384 default / 32,768 cap; coordinator
    round 2 quoting Opus: 10,240 soft / 16,384 hard). Built as: **default 16,384,
    hard cap 32,768, applied to the TEXT block**, with `text_bytes`,
    `structured_bytes` and their sum all reported. 10,240 is a good explicit
    `budget_bytes` for a caller that wants bodies only — it is witnessed, and it
    elides. The measured floor of a fully-elided receipt on this fixture is
    6,411 bytes; below that the verb refuses rather than cut further.

Not taken, with the reason:

11. **`BODY-PARTIAL(head-only)` — a body degraded to signature + docstring at a
    form boundary.** Not built. It is a third representation of a body whose only
    consumer is a budget squeeze that the elision ladder already handles by
    dropping to a range with a hash and a refetch command. A partial body is the
    one thing in this design that could be mistaken for a whole one, which is the
    failure class the whole receipt exists to prevent. Filed as the obvious next
    increment if the adoption cohort shows callers refetching elided handlers.
12. **A per-leg budget table** (Sol's `per_leg:{menu:512,js:5120,…}`). The single
    total plus a stated elision order achieves the same outcome with one number
    to reason about; per-leg caps would introduce a second way for a body to be
    cut and a second thing to explain.

---

## 6. Two defects found by hand-driving, and their fixes

**The route entry was a 25-line window.** `narrow-to-member` originally required
the enclosing bracket to open BEFORE the hit line's first character. A route-table
entry opens in the middle of its own line, so no span ever qualified and the leg
fell through to the oversized-form window. Fixed to LINE containment; the route
leg is now the exact one-line entry (73 bytes instead of 1,874). The docstring
carries the scar.

**A convention set that crossed the JSON boundary was refused as malformed.**
`:kind :use` written in EDN arrives as `"use"` after the MCP round-trip. The
admission rejected every inline config. Fixed by coercing both spellings, which
is why the file-based and inline paths now have separate witnesses.

---

## 7. Gates

| gate | result |
|---|---|
| `make mcp-operation-oracle` | `mcp-operation oracle: pass; legacy counterexamples=[verification_failed,verification_pending]` |
| `~/bin/suite-run clojure -M:clj-surgeon/mcp-test` | `Ran 728 tests containing 8856 assertions.` / `0 failures, 0 errors.` (rc=0) |
| `~/bin/suite-run bb test/run_all.clj` | `Ran 814 tests containing 6724 assertions.` / `0 failures, 0 errors.` (rc=0) |
| intent audit (`audit-current-repository`) | `:ok true :specs 363 :violations 0` (350 + the 13 new) |
| `make mcp-smoke` | `{:ok true, :operation :mcp-stdio-smoke, :server "clj-surgeon", :tools [… "feature_thread"], :response-count 3}` |
| `make repository-hygiene` | `repository hygiene: no machine-local build cache is tracked at any depth` |
| `clj-surgeon.mcp-feature-thread-test` alone | `{:test 17, :pass 413, :fail 0, :error 0}` |

The catalog gained a seventh tool, so thirteen witnesses that asserted six were
updated honestly rather than relaxed: `tools-for-profile`,
`outcome-classes-by-tool`, `mcp_server_test` (including the deftest NAME
`exposes-exactly-six-typed-tools` → `…-seven-…`), `mcp_http_server_test` (four
places, including the add/remove `:tool-count` arithmetic),
`mcp_stdio_smoke` (assertion and message text), `admit_patch_test`,
`mcp_operation_contract_oracle.pl` (two `required_outcome` rows plus
`forbids_job_clock(receipt)`), `mcp_operation_registry_test`, and the
`enabled_tools` TOML line in `workspace_onboarding` and its byte-identical test
mirror — which was also missing `admit_clojure_patch` and now names both.
