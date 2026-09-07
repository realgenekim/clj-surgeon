# CurtainCall LID batch 3 — G1 API-key scope authority

Worktree: `/home/forge/src/cc-lid4`; branch `astra/lid-batch-3`.
Base commit: `450dc8faaa42eb035ec67a4553ff447c5d04e602`.

## Result and behavior decision

Recovered and linked G1 only. Added ten atomic registry/EARS rows, current HLD and
authorization design, five witnesses (including a literal scope × route-class
matrix), and a complete 167-row API method/template census. Final production
source changes are comments only: all five source files compare identically with
the base commit after removing full-line comments. Every planted behavior change is restored;
`server.clj` has no final diff. No API behavior migration was shipped.

**The rung-e historical-read change is STOPPED.** A named key without a `:scope`
member currently resolves to organizer, including a hashed read key whose scope
was removed. Changing this to a typed refusal or read-only default changes existing
stored-key authority. This batch therefore ships the requested rung b/c compatibility
witnesses and documented misreadings. Explicit nil/invalid scope still refuses.
The new-key five-argument boundary already has a typed `:invalid-api-key-scope`
refusal before append (rung e); it is now witnessed. The three-argument compatibility
arity still stores organizer explicitly. No production key inventory was read.

**Behavior question for the owner:** Should historical absent-scope keys be
inventoried and rotated before requiring an explicit stored scope, or should their
organizer compatibility remain? Until that decision, removing a scope member can
promote a narrow key. These tests pin the compatibility contract, not a claim that
promotion by data loss is safe.

The audit draft conflated bearer and session authority. All valid scopes can read
private API data; only reviewer/review-bot can create reviews; only organizer can
publish/unpublish speakers. Organizer is not a superset of reviewer. The clone
route uses session authority (full-app bearer-only request: 302, declared middleware
alone: 403), and review-policy PUT needs a chair session (bearer-only: 401).
There is no truthful universal “declared API route without bearer returns 401” row.

## Rows

- [x] **API-SCOPE-001**: When a caller requests an event's private API data, the handler shall admit exactly a usable read, reviewer, review-bot, or organizer key belonging to that event.
- [x] **API-SCOPE-002**: When a caller posts a valid new API review, the handler shall admit only reviewer or review-bot bearer scope.
- [x] **API-SCOPE-003**: When a caller requests speaker publication or unpublication through the API, the handler shall admit only organizer bearer scope.
- [x] **API-SCOPE-004**: When a bearer-only caller requests a session-authorized API operation, the application shall grant no session authority from the API key.
- [x] **API-SCOPE-005**: When a matching stored named key has no scope member, the key resolver shall preserve organizer authority.
- [x] **API-SCOPE-006**: When a matching stored named key has a present invalid scope, the key resolver shall refuse its credential.
- [x] **API-SCOPE-007**: When an API request presents a revoked named key or a key belonging to another event, the key resolver shall grant no authority for the requested event.
- [x] **API-SCOPE-008**: When explicit API-key creation receives an invalid scope, the command shall refuse before append with invalid-api-key-scope.
- [x] **API-SCOPE-009**: When the API route inventory is checked, every registered API method-template pair shall have its required policy row in the independent authority census.
- [x] **API-SCOPE-010**: When an API route declares a session audience, the declared authorization middleware shall enforce that audience.

Each row has `:misreadings`, `:boundaries`, named `:tests`, and source/test markers.
Registry witnesses are:

| Rows | Named test |
|---|---|
| 001–007 | `api-scope-by-route-class-literal-matrix-test` |
| 005–006 | `historical-scope-presence-is-not-truthiness-test` |
| 008 | `explicit-scope-creation-refuses-invalid-before-append-test` |
| 009 | `api-route-census-requires-an-explicit-policy-row-test` |
| 010 | `declared-api-policy-enters-session-gate-test` |

The matrix is hand-written: 11 credential states × 20 request variants (220 HTTP
outcomes), including every v1 route, private query variants, speaker concealment,
and the session-only clone route. Credential states are no key, read, reviewer,
review-bot, organizer, absent stored scope, legacy event token, nil scope, invalid
scope, revoked, and wrong-event. Denied operations additionally assert no appended
facts. The pure stored-row table covers plaintext compatibility and keyword/string/
symbol parsing; the HTTP table uses real folded hashed rows whose scope is removed
or corrupted. Async fixture sinks are drained before assertions.

## Route census boundary

`api-route-policies.edn` fixes the entire `/api/` route universe independently of
runtime declarations. Every actual route needs an explicit row naming its current
authority owner. The census was seeded from source route syntax and reviewed by
owner; the executable witness compares these fixed rows against `server/make-routes`,
including additive routes. Expected HTTP statuses are never generated from runtime
scope helpers or this census.

Only clone currently has an API entry in `declared-event-surface-policies`. The
witness explicitly requires that runtime organizer row and an independent literal
for the entire declared API subset. Deleting the row cannot shrink the test's route
universe. The other API routes deliberately retain their existing session or
handler checks. This is a normal-suite coverage guard, not a new runtime policy
engine. Deleting clone's row removes its SECOND gate; it does not prove anonymous
full-app access, since the outer session gate remains.

## Red-first by plant, then restoration

Fixture discovery initially failed on inappropriate speaker publication data,
JSONL string scopes, and an async fixture append. Those fixture mistakes were
corrected before collecting the deliberate-plant evidence; they are not claimed
as product defects. The pre-plant focused scope and linkage run passed 19 tests /
1,169 assertions. Each plant below then modified the actual SUT, ran the indicated
witness, produced named assertion failures, and was restored in `finally`. Final
full gates run on the restored source, not on test expectations changed to fit a plant.

Commands inherit `cc-b3-env.sh`. For the first six plants:

```sh
bin/kaocha unit --focus cfp-scheduler-killer.api-scope-contract-test
```

For the constructor plant:

```sh
bin/kaocha unit --focus cfp-scheduler-killer.api-scope-contract-test/explicit-scope-creation-refuses-invalid-before-append-test
```

| Plant | Exit | Exact test summary |
|---|---:|---|
| `absent-scope-read-default` | 3 | 5 tests, 488 assertions, 3 failures. |
| `nil-scope-organizer` | 12 | 5 tests, 488 assertions, 12 failures. |
| `read-key-can-publish` | 4 | 5 tests, 488 assertions, 4 failures. |
| `organizer-can-review` | 6 | 5 tests, 488 assertions, 6 failures. |
| `deleted-clone-policy` | 3 | 5 tests, 488 assertions, 3 failures. |
| `new-route-no-policy` | 2 | 5 tests, 489 assertions, 2 failures. |
| `invalid-constructor` | 10 | 1 tests, 15 assertions, 10 failures. |

### absent-scope-read-default

```diff
--- src/cfp_scheduler_killer/exports.clj
+++ src/cfp_scheduler_killer/exports.clj
@@ -1830,7 +1830,7 @@
 ;; INTENT: API-SCOPE-006 — present invalid scope refuses; nil is not absence.
 (defn- stored-key-scope [key-row]
   (if-not (contains? key-row :scope)
-    :organizer
+    :read
     (or (api-key-scope (:scope key-row))
         (do
           (log/error :api-key-refused
```

Exact named red (ANSI color removed):

```text
FAIL in cfp-scheduler-killer.api-scope-contract-test/api-scope-by-route-class-literal-matrix-test (api_scope_contract_test.clj:160)
:absent-scope :post /api/v1/events/api-review/speakers/d5f7c115-ef3b-445d-b3e7-3fcb3414ed49/publish
Expected:
  204
Actual:
  -204 +403
```

### nil-scope-organizer

```diff
--- src/cfp_scheduler_killer/exports.clj
+++ src/cfp_scheduler_killer/exports.clj
@@ -1829,7 +1829,7 @@
 ;; INTENT: API-SCOPE-005 — absent member preserves historical organizer authority.
 ;; INTENT: API-SCOPE-006 — present invalid scope refuses; nil is not absence.
 (defn- stored-key-scope [key-row]
-  (if-not (contains? key-row :scope)
+  (if-not (:scope key-row)
     :organizer
     (or (api-key-scope (:scope key-row))
         (do
```

Exact named red (ANSI color removed):

```text
FAIL in cfp-scheduler-killer.api-scope-contract-test/historical-scope-presence-is-not-truthiness-test (api_scope_contract_test.clj:185)
{:scope nil}
Expected:
  nil
Actual:
  -nil +:organizer
```

### read-key-can-publish

```diff
--- src/cfp_scheduler_killer/handlers/public_api.clj
+++ src/cfp_scheduler_killer/handlers/public_api.clj
@@ -211,7 +211,7 @@
           (not (api-authed? req event))
           (needs-token "a token is required to change speaker publication")
 
-          (not= :organizer (:scope (api-context req event)))
+          (not (#{:read :organizer} (:scope (api-context req event))))
           (needs-scope :organizer)
 
           (nil? person-id)
```

Exact named red (ANSI color removed):

```text
FAIL in cfp-scheduler-killer.api-scope-contract-test/api-scope-by-route-class-literal-matrix-test (api_scope_contract_test.clj:160)
:read :post /api/v1/events/api-review/speakers/86370144-c32d-4b1e-8d9e-f17701d5a7f9/publish
Expected:
  403
Actual:
  -403 +204
```

### organizer-can-review

```diff
--- src/cfp_scheduler_killer/handlers/public_api.clj
+++ src/cfp_scheduler_killer/handlers/public_api.clj
@@ -180,7 +180,7 @@
           (nil? context)
           (needs-token "a scoped API key is required to record a review")
 
-          (not (#{:reviewer :review-bot} (:scope context)))
+          (not (#{:reviewer :review-bot :organizer} (:scope context)))
           (http/json-response 403
                               {"error" "this endpoint requires reviewer or review-bot scope"})
 
--- src/cfp_scheduler_killer/api_reviews.clj
+++ src/cfp_scheduler_killer/api_reviews.clj
@@ -140,7 +140,7 @@
       (if existing
         (prior-result existing request)
         (case (:scope context)
-          :review-bot (record-bot! event-id submission-id context request actor)
+          (:review-bot :organizer) (record-bot! event-id submission-id context request actor)
           :reviewer (record-human! event-id submission-id context request actor)
           (refuse! :insufficient-scope
                    "This endpoint requires reviewer or review-bot scope."
```

Exact named red (ANSI color removed):

```text
FAIL in cfp-scheduler-killer.api-scope-contract-test/api-scope-by-route-class-literal-matrix-test (api_scope_contract_test.clj:160)
:absent-scope :post /api/v1/events/api-review/submissions/8c33df79-f504-4758-98f3-dfd94d2532e2/reviews
Expected:
  403
Actual:
  -403 +201
```

### deleted-clone-policy

```diff
--- src/cfp_scheduler_killer/event_surface_authorization.clj
+++ src/cfp_scheduler_killer/event_surface_authorization.clj
@@ -29,7 +29,6 @@
 
    [:get "/events/:slug"]                                   :speaker
    [:get "/events/:slug/details"]                           :speaker
-   [:post "/api/events/:slug/clone"]                        :organizer
 
    [:get "/events/:slug/fragment"]                          :reviewer
    [:get "/events/:slug/committee"]                         :reviewer
```

Exact named red (ANSI color removed):

```text
FAIL in cfp-scheduler-killer.api-scope-contract-test/api-route-census-requires-an-explicit-policy-row-test (api_scope_contract_test.clj:217)
deleting a declared API policy must not erase the test's route universe
Expected:
  {[:post "/api/events/:slug/clone"] :organizer}
Actual:
  {-[:post "/api/events/:slug/clone"] :organizer}
```

### new-route-no-policy

```diff
--- src/cfp_scheduler_killer/server.clj
+++ src/cfp_scheduler_killer/server.clj
@@ -160,6 +160,7 @@
      ["/events/:slug/mcp" {:post {:handler #'agent-handlers/handle-mcp}}]
      ;; /api/v1 is public by construction; handlers decide what a token widens.
      ;; Both index spellings are routed because the auth pattern includes the slash.
+     ["/api/unclassified" {:get {:handler #'public-api-handlers/handle-api-index}}]
      ["/api/v1/" {:get {:handler #'public-api-handlers/handle-api-index}}]
      ["/api/v1" {:get {:handler #'public-api-handlers/handle-api-index}}]
      ["/api/v1/events/:slug" {:get {:handler #'public-api-handlers/handle-api-event}}]
```

Exact named red (ANSI color removed):

```text
FAIL in cfp-scheduler-killer.api-scope-contract-test/api-route-census-requires-an-explicit-policy-row-test (api_scope_contract_test.clj:221)
[:get "/api/unclassified"] needs an explicit authority owner
expected: (#{:public-handler :declared-organizer :session-policy-write :publication-write :private-read :review-write :session-gate :public-read} policy)
  actual: nil
5 tests, 489 assertions, 2 failures.
```

### invalid-constructor

```diff
--- src/cfp_scheduler_killer/exports.clj
+++ src/cfp_scheduler_killer/exports.clj
@@ -1775,7 +1775,7 @@
   ([event label scope person-id actor]
    (let [requested-scope scope
          scope (api-key-scope requested-scope)]
-     (when-not scope
+     (when false
        (throw (ex-info "Choose a valid API key scope."
                        {:type :invalid-api-key-scope
                         :scope requested-scope
```

Exact named red (ANSI color removed):

```text
FAIL in cfp-scheduler-killer.api-scope-contract-test/explicit-scope-creation-refuses-invalid-before-append-test (api_scope_contract_test.clj:195)
nil
Expected:
  :invalid-api-key-scope
Actual:
  -:invalid-api-key-scope +nil
```

## Green verification and lint

All commands inherit `TMPDIR`, `TMP`, `TEMP`, `-Djava.io.tmpdir`, and the blob
upload root under `/var/tmp/forge/lid-batch/`. `PATH` puts `~/bin` first, so even
suite-owned kondo subprocesses use the serialized paved entrance. No live server
was started. The Makefile worktree test makes disposable local Git fixtures under
the permitted temp root; its local fixture pushes are not project pushes.

### `make runtests-once`

Exit 0.

```text
oracle: test/oracles/admin_event_slack_contract.pl
oracle: test/oracles/review_board_contract.pl
oracle: test/oracles/rough_drag_projection.pl
oracle: test/oracles/speaker_profile_edit_contract.pl
oracle: test/oracles/talk_agenda_contract.pl
# tests 18
# pass 18
# fail 0
PASS: fetched origin/main beats stale local main and refusals hold
1032 tests, 13103 assertions, 0 failures.
```

### `bin/kaocha unit`

Exit 0.

```text
1032 tests, 13103 assertions, 0 failures.
```

### Kondo baseline comparison

```sh
~/bin/clj-kondo --lint src test --config '{:output {:format :json}}'
```

Baseline tracked files: Counter({'warning': 102, 'info': 29, 'error': 3})
Final: Counter({'warning': 102, 'info': 29, 'error': 3})
Added diagnostics: {}
Removed diagnostics: {}

The original pre-edit EDN baseline (`cc-b3-kondo-before.edn`) scanned 384 files:
3 errors, 102 warnings, 29 info. A second JSON baseline scan overlapped creation of
the new untracked test; its incomplete-test diagnostics are excluded from baseline
comparison, recovering exactly those original counts. Final comparison includes
ALL files, including the finished test. Diagnostics compare filename/type/level/
message with multiplicity, ignoring line shifts caused by added comments. No new
or removed diagnostics. Raw lint exit is 3 both before and after; lint is baseline-
unchanged, not globally clean.

Existing errors: `handlers/bp.clj:661` invalid arity of `push-everything!`;
`reviews.clj:328` static `Instant/EPOCH` called as a function;
`rubric_predicates.clj:70` unresolved `=>`. They are outside G1 and unchanged.

`git diff --check` passed. Runtime-source comparison ignoring full-line comments
proved the production behavior unchanged, and the temporary `server.clj` route
plant is fully restored.

## Files

- `docs/high-level-design.md` — current authorization model and topic links.
- `docs/intent/registry.edn` — ten appended active rows.
- `docs/intent/authorization/authorization-design.md` — observable matrix,
  compatibility decision, rejected alternatives, edge audit, census limits.
- `docs/intent/authorization/authorization-specs.md` — ten linked EARS rows.
- `docs/intent/authorization/api-route-policies.edn` — 167 fixed authority rows.
- `src/cfp_scheduler_killer/exports.clj` — scope parsing/grant markers.
- `src/cfp_scheduler_killer/folds.clj` — scope-presence markers and corrected
  historical key-storage comment.
- `src/cfp_scheduler_killer/handlers/public_api.clj` — scope/session gate markers.
- `src/cfp_scheduler_killer/api_reviews.clj` — new-review authority marker.
- `src/cfp_scheduler_killer/event_surface_authorization.clj` — census/gate markers.
- `test/cfp_scheduler_killer/api_scope_contract_test.clj` — five witnesses.

## Doubts and limitations

- Historical scope loss still promotes authority; the behavior-change decision
  above is intentionally unresolved. No production credential inventory was taken.
- Revocation is checked before a request; this batch does not establish atomic
  mid-request revocation. Some handlers resolve context more than once.
- Scope admission is witnessed for valid entities and request bodies. Existing
  review suites still own recusal, membership, idempotency, and fact-shape rules.
- Public payload widening/redaction and every session/handler policy outside G1
  are not exhaustively audited by this census. A census proves coverage, not that
  every named owner enforces the right behavior.
- No new Prolog model is retained: the bounded scope × route-class matrix is
  exhaustive in Clojure and no distinct relational counterexample justified rung d.
- The project-mentioned `linked-intent-dev` skill is absent. The available
  `/home/forge/.claude/skills/linked-intent-testing/SKILL.md` was read and used,
  following the same explicitly documented fallback as batches 1 and 2 and the
  user's continuation instruction. An early clarification was resolved from that
  prior-batch evidence. HLD → design → EARS/edge audit → witnesses → source linkage
  documents the current contract; no behavior migration was inferred.
- `bd prime` ran; `bd create` failed because the worktree database lacks
  `issue_prefix`, as in batch 2. No task was created/claimed or DB reinitialized.
- No subagents, project push, deployment, live server, forbidden-port command,
  acid, chain scripts, cohort locks, GO files, or fleet/runtime directories were
  used. Source worktree plus permitted temp artifacts only.
- `.codex/config.toml` initially showed deleted and was restored immediately with
  `git checkout -- .codex/config.toml`. It is not staged or committed. The unrelated
  untracked `.codex/config.toml.disabled-by-sol-yolo` remains untouched.

## Commit and final state

```text
Commit: ae6ea2655d607d632bb69fe20c979239f8c77f80
Author: forge-anvil <forge-anvil@anvil>

Link API scope authority to compatibility and route census witnesses

Register the recovered scope contract with ten EARS rows and an independent
167-route authority census. Pin historical absent-scope organizer grants,
invalid-scope refusal, scope-specific writes, and the separate session gate.
Preserve runtime behavior pending an explicit historical-key migration decision.

Verify witness sensitivity with seven restored SUT plants. Run the full
Makefile gate and independent unit entry; kondo matches the existing baseline.

Co-Authored-By: Gene Kim <genek@itrevolution.com>
Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>
```

Final status:

```text
?? .codex/config.toml.disabled-by-sol-yolo
```

All tracked files are clean. Only the pre-existing untracked disabled-config backup
remains. The commit contains the 11 listed files and excludes `.codex/config.toml`.
No push was performed. Both full entries exited 0; all seven plants were killed;
kondo introduced zero diagnostics relative to baseline.
