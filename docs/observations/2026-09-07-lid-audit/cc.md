# LID integration audit — curtaincall-cfp @ origin/main

**Scope.** Read-only audit of `/home/forge/src/curtaincall-cfp`, `origin/main`
(`92a7ca14fd904e4d962df38614b9af3a7ef41a11`, 2026-09-03), commits since **2026-08-15**.
No edits, no commits, no checkouts. Nothing under `~/acid`, no `chain-*.sh`, no
`.cohort-lock`, no `GO-*`, no fleet/runtime directory, no ports 7888/7890/7894/7895/83xx.
Temp files: `/var/tmp/forge/lid-audit/`.

**Order audited against.** Gene: *"Go back to all the commits done on anvil and buster on
these repos, confirm that updates integrate LID, and opportunistically put in HLD too."*

**Definitions used.** LID = `/home/forge/opt/claude-skills/linked-intent-testing/SKILL.md`
(registry with forever-stable ids · `INTENT:` tag at the implementation · `INTENT-TEST:`
tag at the witness · a bidirectional contract test in the suite everyone already runs).
HLD = `docs/high-level-design.md`.

---

## 0. Headline

The **enforced half of LID in this repo is healthy and green**. The **unenforced half is
where the promises live.**

- `docs/intent/registry.edn` — 51 ids, contract-tested in both directions by
  `test/cfp_scheduler_killer/intent_contract_test.clj`. **Green** (verified this session).
- `docs/intent/<topic>/*-specs.md` — **93 further ids** in six topic spec files, marked with
  `@spec`. **Nothing in the test suite enforces them.** 38 of the 93 (41%) have no `@spec`
  marker on any test; 22 have no marker in `src/` at all; 13 have neither. Every
  `AGENDA-WRITER-00{1,2,3}` row is ticked `[x]` in its spec file and appears **nowhere** in
  `src/` or `test/`.
- **25 load-bearing promises are already written down as prose `;; INTENT:` comments inside
  test files** — no id, no registry row, invisible to the contract test in both directions
  (it harvests `INTENT:` from `src/` only and `INTENT-TEST:` from `test/` only). These are
  the cheapest ratchets available in the repo: the behaviour and the test already exist; only
  the id and two tag edits are missing.
- `src/` is disciplined: **all 40 `INTENT:` tags in `src/` carry a real registry id; zero
  prose-only tags.** The rot is entirely on the test side and in the specs files.

---

## 1. Counts (mechanical pass)

Script: `/var/tmp/forge/lid-audit/mech.py`. Raw per-commit output:
`/var/tmp/forge/lid-audit/mech.json`; ranked gap rows: `/var/tmp/forge/lid-audit/groups.txt`.

| bucket | commits | meaning |
|---|---:|---|
| **commits since 2026-08-15 on origin/main** | 987 | all |
| **commits touching `src/`** | **295** | the audit population |
| `integrated-linked` | **38** | commit added an `INTENT:`/`@spec` id in `src/` **and** a witness in `test/` named the same id, within the commit or the next 5 |
| `integrated-witness` | **9** | no new src id, but the window's test edits carry LID markers |
| `integrated-doc` | **80** | window touched `docs/intent/` (registry or a specs file) |
| `gap-plain-test-only` | **159** | `src/` changed, tests changed, **no LID marker anywhere in the window** |
| `gap-no-test` | **6** | `src/` changed, no test and no intent doc in the window |
| `gap-unwitnessed-id` | **3** | commit touched a src `INTENT:` id with **no** matching witness id in the window |
| **INTEGRATED total** | **127 (43%)** | |
| **GAP total** | **168 (57%)** | |
| `pure-move-candidate` | 0 | no rename-only / zero-churn src commits in the population |

**Read the 57% honestly.** LID is *supposed* to be selective — the skill forbids it as
ceremony for copy changes and leaf functions. A large share of the 159 `gap-plain-test-only`
commits are presentation edits that correctly need no intent (`homepage/closing: center the
swyx thank-you`, `Row diet, part one: histogram bar heights are a class`). The judgment pass
below separates those from the load-bearing ones. **The number that matters is not 168; it is
the 15 groups in §3 and the 25 prose intents in §2.2.**

Author distribution of the 987 (context for "anvil and buster seats"): Gene Kim 524, cfp4 144,
cfp3 98, Curtain Call Merger 59, code-director 49, sdeputy 31, forge-skiff 30, cfp2 14,
cfp-mayor 9, mayor 8, cfp1 6, dev-a 4, others 1–2 each. Per the seat-identity rule, "Gene Kim"
before 2026-08-29 is agent seats, so authorship is not a usable signal for this audit and was
not used as one.

---

## 2. The structural findings (these outrank any single commit)

### 2.1 Two LID systems, one gate

`intent_contract_test.clj` reads exactly one file:

```clojure
(def ^:private registry-path "docs/intent/registry.edn")
```

It enforces bidirectional traceability over the 51 registry ids and nothing else. The 93
spec-file ids under `docs/intent/*/**-specs.md` (`AGENDA-*`, `CORR-*`, `ROUGH-*`, `SPK-EDIT-*`,
`SPK-MULTI-*`, `LETTER-*`) are checkbox lists in Markdown with `@spec` tags scattered through
`src/` and `test/`, **checked by nothing**. Measured this session:

| | count |
|---|---:|
| spec ids declared in `docs/intent/*/**-specs.md` | 93 |
| … with a marker somewhere in `src/` | 71 |
| … with a marker somewhere in `test/` | **55** |
| … with **no test marker** | **38** |
| … with **no src marker** | **22** |
| … with **neither** | **13** |

Ticked-`[x]`-but-untraceable rows include the whole Rough Blocking writer family and most of
the append-only correction family:

- `AGENDA-WRITER-001/002/003` — *creates and places one null-title talk for a known person* /
  *moves an existing talk instead of creating a speaker-name block* / *refuses when there is no
  known person and no explicit ghost request*. Ticked done; **zero occurrences in `src/` or
  `test/`**. This is an identity + refusal promise on the scheduler writer.
- `AGENDA-LEGACY-001/002`, `AGENDA-SUBMISSION-001/002` — legacy-suppression and
  snapshot-preservation, ticked, no marker either side.
- `CORR-APPLY-001/002`, `CORR-BP-001..003`, `CORR-CHARLOTTE-001..003`, `CORR-PLAN-001..003`,
  `CORR-RECEIPT-001`, `CORR-VIEW-001/002` — ticked, **no test marker**. (`db_correct_test.clj`
  does carry `@spec CORR-APPLY-003, CORR-APPLY-004, CORR-RECEIPT-002` — so the convention is
  live, just unevenly applied and unenforced.) These are the correction/replay boundary the HLD
  itself calls the "Correction boundary."
- `SPK-EDIT-001..006, 008` — no `src/` marker.

**The ratchet.** Extend the existing contract test to harvest the specs files exactly as it
harvests `registry.edn`: every `[x]` spec id must have a `@spec`/`INTENT:` tag in `src/` **and**
a `@spec`/`INTENT-TEST:` tag in `test/`; every `@spec` tag must name a declared id. This is one
test-file change against a mechanism that already exists, and it turns 93 documentation rows
into 93 ratchets. It will go red on the 38 above — which is the point, and each red is a real
missing witness, not a false alarm.

### 2.2 Twenty-five promises written as prose `INTENT:` in tests — id-less, gate-less

`;; INTENT: <free prose>` appears **25 times in `test/`** and **zero times in `src/`** (where
every one of the 40 tags carries a registry id). The contract test cannot see these: it looks
for `INTENT:` under `src/` and `INTENT-TEST:` under `test/`, so a prose `INTENT:` in a test file
is invisible in both directions. Each of these is an articulated, already-tested, load-bearing
promise missing only a registry row and two tag edits:

| file:line | the prose promise | class |
|---|---|---|
| `test/cfp_scheduler_killer/review_scores_csv_test.clj:86` | "an export must not turn our data into someone else's code" | security / refusal (CSV formula injection) |
| `test/cfp_scheduler_killer/exports_test.clj:1463` | "an export must leave visible evidence it happened" | receipt / delivery |
| `test/cfp_scheduler_killer/exports_test.clj:1502` | "a speaker who adds the CFP deadline to their calendar must be able…" | delivery (ICS) |
| `test/cfp_scheduler_killer/content_management_test.clj:704` | "refusing an organizer's task-creation attempt must not also destroy…" | refusal preserves work |
| `test/cfp_scheduler_killer/content_management_test.clj:636` | "the editorial approval gate must never take a program off the public…" | policy gate |
| `test/cfp_scheduler_killer/blob_durability_test.clj:2` | "A DURABILITY DOWNGRADE MUST NOT HAPPEN SILENTLY" | storage / loud failure |
| `test/cfp_scheduler_killer/telemetry_test.clj:262` | Gene 2026-08-18 — "all writes to bq should be done in `(future …)`" | async guarantee |
| `test/cfp_scheduler_killer/telemetry_test.clj:177` | "the sink mapping is PURE and privacy-bounded" | privacy boundary |
| `test/cfp_scheduler_killer/board_test.clj:1188` | "bulk export is a chair act, and the reviewer board must not offer it" | authorization |
| `test/cfp_scheduler_killer/board_test.clj:1227` | "nothing on a blinded reviewer's page may offer speaker stewardship" | authorization / blind review |
| `test/cfp_scheduler_killer/reviewer_queue_views_test.clj:80` | "a queue row must say whether THIS reviewer has weighed in" | identity in a queue |
| `test/cfp_scheduler_killer/reviews_test.clj:726` | "the Manifesto and the board cannot drift apart" | policy/doc coherence |
| `test/cfp_scheduler_killer/schedule_test.clj:966` | "a block occupies its room" | scheduler constraint |
| `test/cfp_scheduler_killer/schedule_test.clj:578` | "the schedule opens on ROUGH BLOCKING (Gene, 2026-08-20)" | default surface |
| `test/cfp_scheduler_killer/public_widgets_test.clj:864` | "archiving is a fact, not a deletion — so the permalink must not rot" | publication boundary |
| `test/cfp_scheduler_killer/public_widgets_test.clj:904` | card.png → card.jpg, "but a social…" (cache) | cache identity |
| `test/cfp_scheduler_killer/public_widgets_test.clj:936` | "`/events/:slug/exports/agenda-embedded.html` is a CONTRACT with…" | external contract |
| `test/cfp_scheduler_killer/public_widgets_test.clj:1155/1194/1249` | org bolding · deep-link only when identified · one-name rule under two billings | public projection |
| `test/cfp_scheduler_killer/routes_contract_test.clj:389` | "llms.txt is a PROMISE TO AN AGENT, and an unpinned promise is the…" | agent contract |
| `test/cfp_scheduler_killer/agent_test.clj:457` | "every title, abstract, bio and form answer we hand an agent was…" | prompt-injection boundary |
| `test/cfp_scheduler_killer/forms_test.clj:586` | "a show-when rule keys on the option's TEXT" | form policy |
| (3 further occurrences are the contract test's own docstring/messages, not promises) | | |

**The ratchet.** Each becomes a registry row + `;; INTENT: <ID>` at the src site + rename the
test's `;; INTENT:` to `;; INTENT-TEST: <ID>`. Then add the disjointness guard the skill asks
for: **a `;; INTENT:` tag appearing under `test/` should itself fail the contract test**, so the
class cannot come back.

### 2.3 What is green (verified this session, not asserted)

```
make test-oracles      → 5 plunit oracles, all pass
                         admin_event_slack_contract, review_board_contract,
                         rough_drag_projection, speaker_profile_edit_contract,
                         talk_agenda_contract
bin/kaocha unit --focus cfp-scheduler-killer.intent-contract-test \
                --focus cfp-scheduler-killer.intent-registry-architecture-test \
                --focus cfp-scheduler-killer.intent-witness-identity-architecture-test
                       → 9 tests, 676 assertions, 0 failures
```

The oracles are wired into `runtests-once` (`test-oracles test-js new-mission-worktree-test`),
which is the skill's own wiring requirement — this repo does that correctly and the Makefile
comment even says why ("an oracle only run by hand is a diary entry, not a gate").

---

## 3. Top gap groups by blast radius — EARS row + witness shape drafts

Ranked by blast radius (load-bearing class × churn × how silently a refactor could narrow the
promise). Each row is one atomic EARS outcome; where a promise needed an "and" it was split, per
the skill. "HLD section" names where the promise should be recorded in
`docs/high-level-design.md`, or NONE.

### G1 — API-key scope authority (authorization) · blast radius: HIGHEST

Commits `7ec2d6236` "Add scoped API key authorization", `52d2f7989` "Add secure scoped review
API writes", `4052d3b05` "authz: declared /api policies now enter the surface gate (closes
staging-only anonymous …)". Implementation: `src/cfp_scheduler_killer/exports.clj`
(`api-key-scopes` L1716, `api-key-scope` L1720, `stored-key-scope` L1826, `api-key-context`
L1837, `valid-token?` L1867), `src/cfp_scheduler_killer/handlers/public_api.clj` (`api-context`
L125, `needs-scope` L150), `src/cfp_scheduler_killer/api_reviews.clj` (`record-review!` L130,
`:insufficient-scope` L144), `src/cfp_scheduler_killer/folds.clj:505` (`api-key.created`
persists `:scope`).

**Marker state.** `AUTHZ-001/003/004` are tagged in `event_surface_authorization.clj` — but
**zero markers anywhere on the scope machinery itself**, and the `4052d3b05` gate-widening
carries prose only. Registry has no API-key/scope row (`API-002`, `API-004` are `:proposed` and
about payload shape, not authority).

**Tests today.** `api_key_security_test.clj`, `api_v1_review_test.clj`, `authz_event_scope_test.clj`
(`api-key-create-and-revoke-round-trip-test`) — none carry markers.

**EARS row (draft).**
> `:id :API-SCOPE-001` — *When a request presents a bearer API key to an `/api/*` route that
> declares a policy, the application shall answer with the authority of that key's stored scope
> only.*
> Split rows: `:API-SCOPE-002` — *When a request presents no bearer key to a declared `/api/*`
> route, the application shall answer 401.* `:API-SCOPE-003` — *When an `/api/*` route template is
> declared in `declared-event-surface-policies`, the application shall route it through the
> declared-authorization gate.*
> `:misreadings` — **"An absent `:scope` means organizer."** (`stored-key-scope` L1826 really does
> default to `:organizer`; a fold normalization or a "clean up nil scopes" migration silently
> promotes every affected key to full organizer authority, and every green test uses either an
> explicitly-scoped key or the deliberately-legacy one). **"Deleting an `/api/*` row from the
> policy map removes a restriction."** It removes the route from the gate entirely — back to
> ungated.
> `:boundaries` — legacy key with no stored scope · a policy row deleted · a new `/api/*` route
> added without a policy row · a key revoked mid-request.

**Witness shape.** Rung **e** is reachable and should be taken: make the absent-scope state
unrepresentable — `stored-key-scope` returns a typed refusal (or the narrowest scope, `:read`)
rather than `:organizer`, and `create-api-key!` refuses to persist a key without an explicit
scope. Behind that, a rung-**c** state-space test enumerating the full cross-product
{no key, read, reviewer, review-bot, organizer, key-with-`:scope`-dissoc'd} × {every declared
`/api/*` template} asserting the exact status code from a hand-written literal matrix (never
derived from `needs-scope`), plus a rung-**a/b** guard asserting every `/api/*` route in the
route table appears in `declared-event-surface-policies` — so adding a route without a policy is
red, not open.

**HLD section.** **MISSING** — the HLD has no authorization or tenancy section at all. Add
`## Authorization model` under System Design: personas (anonymous / speaker / reviewer / chair /
organizer / API key), the two gates (`wrap-declared-event-authorization` and per-scope checks),
and the fail-closed rule.

---

### G2 — Email/notification delivery: identity, idempotence, and the false-green · blast radius: HIGHEST

Commits `9f4ece607` "Carry notification recipients by identity", `59f741548` "Pin batch
notification recipients by ID", `43eeeab83` "Confirm single email delivery", `59cb2db51` "Refuse
queued emails with unresolved fields", `739005d78` "mail: deliver accepted-decision .ics invites
via SES instead of a false-green dev-log", `7b3bf6004` "Deliver magic-link emails synchronously",
`6f412d03c` "Derive mail outbox from in-memory projection", `233578a6c` "Add queued email send tab",
`97a328d98` "Render letter previews for real recipients".
Implementation: `src/cfp_scheduler_killer/mail.clj` (`dispatch!` L71, `send!` L164, `outbox` L191,
`unresolved-tokens` L215, `approve!` L224, `send-now!` L239), `src/cfp_scheduler_killer/ses.clj`,
`src/cfp_scheduler_killer/inform.clj` (`inform!`, `:notified-at` idempotence guard),
`src/cfp_scheduler_killer/handlers/communications.clj` (`notification-recipient-ids` L141-155).

**Marker state.** **Zero `INTENT:` markers in `mail.clj` or `ses.clj`.** The only tag in the path
is `LETTER-001` at `inform.clj:137`, which governs where letter *templates* come from — not
recipients, delivery, or idempotence. Every load-bearing rule (simulation firewall, "never
throws", ics-not-dev-log) lives in docstrings.

**Tests today.** `mail_projection_test.clj`, `comms_test.clj`, `acceptance_notify_test.clj`
(`re-accepting-an-accepted-talk-does-not-re-email-test`), `call_for_papers_eval_test.clj`,
`io_email_test.clj` — none carry markers except `LETTER-001/002` on a page-rendering test.

**EARS rows (draft) — four, because this is four promises:**
> `:MAIL-IDENT-001` — *When an organizer informs a decision, the application shall address each
> queued letter by the recipient's stable person id.*
> `:MAIL-IDEMP-001` — *While a submission carries a `:notified-at` fact, when an organizer informs
> that submission again, the application shall queue no further letter.*
> `:MAIL-REFUSE-001` — *When a queued letter still contains an unresolved merge field, the
> application shall refuse to send it and report `{:mode :blocked}`.*
> `:MAIL-TRUTH-001` — *When no mail provider is configured, the application shall record the
> dispatch as unsent rather than appending a successful `email.sent` fact.*
> `:misreadings` for `MAIL-TRUTH-001` — **"dev-log counts as sent."** `dispatch!`'s SES arm is
> guarded by `(and (nil? cfg) (not suppress?) (ses/enabled?))` and the very next arm, `(nil? cfg)`,
> appends `email.sent` with `:via "dev-log"` and returns `{:mode :sent}`. **The whole suite runs
> with no SMTP config, so nearly every mail test exercises the dev-log branch and asserts
> `:mode :sent`** — one reordered arm turns "we emailed 90 speakers" into a fact about nothing.
> Only `ics-bearing-letter-takes-the-real-provider-path-not-dev-log-test` pins the distinction, and
> only for `.ics`-bearing letters.
> `:misreadings` for `MAIL-IDEMP-001` — "idempotence belongs to the status transition." It rests
> solely on re-reading `:notified-at`; a new "resend decision" verb, or a batch path that reads
> submissions once before the loop, re-emails the cohort with every test green.

**Witness shape.** Rung **e** for `MAIL-TRUTH-001`: make the dev-log arm append a distinct
`email.simulated` fact type so an `email.sent` fact **cannot** exist without a provider receipt —
then the outbox projection and the organizer's receipt count only real sends by construction.
Rung **c** for idempotence: a state-space test over {first inform, repeat inform, resend verb,
batch inform, concurrent double-submit} × {accepted, declined, waitlisted} asserting the count of
`email.queued` facts against a hand-written literal table. Rung **b** for identity: assert the
receipt's `notification-recipient-ids` equals the literal set of person ids, never a count —
`inform-all-results!`'s own comment says "a count cannot safely identify a recipient."

**HLD section.** **MISSING** — add `## Delivery and notification` under System Design: the
outbox-as-projection model, the simulation firewall, the approve→send gate, and the rule that a
send fact requires a provider receipt.

---

### G3 — Export receipts + CSV formula injection · blast radius: HIGH

Commits `b9a3c0bfe` "Export receipts survive projection, and the ledger keeps its promise" (+700/-670),
`621b784fa` "exports: a generated export leaves a receipt (ABS-13)", `c54c2e98a` "R3: a CSV export
must not turn our data into someone else's code", `6f98d09ce` "Add review results exports".
Implementation: `src/cfp_scheduler_killer/exports.clj` (`record-export!` L50, `exports-for` L61,
`export-receipt-sentence` L73), `src/cfp_scheduler_killer/folds.clj:510` (receipts at
`[:events slug :exports]`, deliberately **beside** `:settings`), `src/cfp_scheduler_killer/review_scores_csv.clj`
(`formula-leaders` L16, `neutralize-formula` L22, `csv-cell` L41).

**Marker state.** exports.clj's tags are all unrelated (`PRED-EXPORT-001`, `AGENDA-PUBLIC-007`,
`CANONICAL-LINK-001`, `DEMO-001`). `record-export!`, the `export.generated` fold, and **all of
`review_scores_csv.clj` carry zero markers**. The commit cites **ABS-13**, which is *not* a
registry id — an external judging label mistaken for one. Both promises exist as ID-less prose
`;; INTENT:` comments in the tests (§2.2).

**EARS rows (draft) — two:**
> `:EXPORT-RECEIPT-001` — *When an organizer downloads an export, the application shall append one
> `export.generated` receipt, with row count, byte count and SHA-256, that remains visible after any
> later event-settings edit.*
> `:EXPORT-CSV-SAFE-001` — *When any CSV surface emits a cell whose text begins with `=`, `+`, `-`,
> `@`, tab or carriage return, the application shall emit that cell so a spreadsheet displays it as
> text.*
> `:misreadings` — **"The receipt is defined by where it is stored."** Moving the fold to
> `[:events slug :settings :exports]` "for symmetry with `:api-keys`" (which genuinely does live in
> `:settings`) reinstates the exact bug: `event.updated`'s shallow settings merge wipes them.
> Compounding it, tolerant readers downstream (`(or (:exports event) [])`) convert "receipts
> vanished" into "no exports taken yet" — `exports-for` is deliberately written to throw instead.
> **"Neutralization is a property of this one writer."** `neutralize-formula` is called only from
> `review_scores_csv/csv-cell`, and its test reaches the private fn directly — so it stays green
> even if `render` stops calling it, and any *new* CSV writer (`speaker_csv.clj`, a bundle export,
> a future exports.clj CSV) ships injection with a fully green suite.

**Witness shape.** For the receipt: rung **b** already exists
(`export-receipts-survive-an-unrelated-settings-edit-test`) — give it an id, then add a rung-**a**
architecture guard that fails if `:exports` ever appears under a settings path in the fold. For
CSV: rung **e** — route every CSV emission through one `csv/row` primitive and add a
source-scanning architecture test (this repo already has that pattern:
`deliverables-handler-has-no-bare-text-plain-refusals`) that fails on any `str/join ","` over
user-derived data outside that primitive; witness it **through the public `render`**, not the
private `csv-cell`, so the call cannot be removed silently.

**HLD section.** **MISSING** — add `## Exports and receipts` (or a Key Design Decision *"A
receipt is a fact beside settings, never inside them"*). The existing Tenet "One receipt from
promise through fold" covers only Rough Blocking.

---

### G4 — Refusal preserves work (forms) and refuses silent durability downgrade (blobs) · blast radius: HIGH

Commits `1df07eda0` "Refusing a task never throws the organizer's work away" (+331/-279),
`2a650de01` "refuse a silent durability downgrade; four headshots repaired in production".
Implementation: `src/cfp_scheduler_killer/handlers/speaker_tasks.clj` (`refuse-task-creation` L194,
re-renders the whole page with `:task-draft` + 422; a six-branch refusal `cond` at L259-276; a
hand-rolled 422 at L308-317), `src/cfp_scheduler_killer/io/blob.clj` (`shared-store?` L26,
`default-put!` L34 throwing `:blob/durability-downgrade-refused`, dynamic `*put-fn*` L67).

**Marker state.** **No markers in src for either.** The blob promise exists only as an ID-less
prose docstring in `blob_durability_test.clj:2`. `OPS-TOOL-DURABILITY-001` sounds adjacent but is
about `bin/` tools being git-tracked — do not conflate.

**EARS rows (draft) — two:**
> `:REFUSE-KEEP-001` — *When an organizer submits an invalid deliverable-task or reminder-schedule
> form, the application shall re-render that page with every value the organizer typed still in its
> field.*
> `:BLOB-DURABLE-001` — *While the store backend is the shared Postgres store and no upload bucket
> is configured, when an upload is attempted, the application shall refuse the write.*
> `:misreadings` — **"422 plus a message is the contract; the draft values are decoration."** The
> existing architecture guard only greps for bare `text/plain`; a lighter fragment response or a
> new hand-rolled 422 (the pattern is already copyable at L308-317) passes it while dropping the
> typed values. **Only one of six refusal branches has a work-preservation witness.**
> **"The refusal belongs to `default-put!`."** It is bypassed by any caller that rebinds `*put-fn*`
> — as the tests themselves do — and `shared-store?` reads `STORE_BACKEND=postgres` from the live
> environment, so any change to how the backend is named or selected makes the throw silently
> disappear while all three blob tests still pass.

**Witness shape.** Rung **c** for the form half: enumerate all six refusal branches × the full
field set, asserting each typed value round-trips into the re-rendered HTML (hand-written literal
expectations). Rung **e** for the blob half: move the refusal from `default-put!` into `put!` so
no rebinding can bypass it, and make the backend a typed value rather than a string read from the
environment at call time; add a witness that drives the **production** path (per the constellation
rule *witness through the production path* — a fix witnessed only through a tamper seam regresses
on real inputs).

**HLD section.** **MISSING** — the HLD Tenets say nothing about refusal. Add a tenet
*"A refusal preserves the operator's work and is loud"* plus a `## Storage durability` note under
System Design.

---

### G5 — Reviewer track scope (the positive half is unregistered) · blast radius: HIGH

Commits `d35b51b8c` "Add reviewer track scope controls", `12b79bf96` "Add per-member review track
scope", `edbd5bcca` "Apply reviewer scopes to board coverage".
Implementation: `src/cfp_scheduler_killer/committees.clj` (`member-scope` L100, `scope-tracks` L207,
`tracks-for-person-on-event`), `src/cfp_scheduler_killer/reviews.clj` (`filter-board` L370),
`src/cfp_scheduler_killer/handlers/board.clj` (`board-state` L59, `my-tracks`/`show-all?` L101-105,
`:coverage`/`:needs-coverage`/`:rankable` from `scope-rows` L115-118),
`src/cfp_scheduler_killer/review_work.clj` (`progress-for-event` L58),
`src/cfp_scheduler_killer/handlers/dashboard.clj` (`handle-member-scope` L331, chair-only 403).

**Marker state.** The **negative** half is registered (`AUTHZ-002`; and the REV-BOARD-001 row's
boundary text). The **positive** half — scope narrows the default view and the coverage
arithmetic — has **no id and no marker** on any of `scope-tracks`, `filter-board`,
`progress-for-event`, or `handle-member-scope`.

**Tests today.** `track_scope_test.clj` (9 deftests including `rating-is-never-gated-by-track-test`,
`open-scope-is-unchanged-test`), `committee_scope_contract_test.clj` — **no markers on any of them.**

**EARS rows (draft) — two:**
> `:REV-SCOPE-001` — *When a reviewer whose committee membership is scoped to a track set opens the
> event Review Board, the application shall open it pre-filtered to those tracks.*
> `:REV-SCOPE-002` — *While a reviewer's board is track-filtered, the application shall offer a
> control that reveals every track.*
> `:misreadings` — **"Scope is authorization."** A maintainer hardening review could move the
> `tracks` filter from `filter-board` into `enriched-for-event`/`project-submissions`, or delete the
> `show-all?` branch as dead code, converting a default filter into a hard fence.
> `rating-is-never-gated-by-track-test` exercises the *rating verb* directly, not the projection, so
> a projection-level filter leaves the write path green while making out-of-track work permanently
> invisible. **"An unrecognized scope shape means open."** `scope-tracks` returns `nil` for any shape
> it does not recognize; changing the stored shape (`:field :track` → `:tracks`) silently un-scopes
> every reviewer while every test that builds scopes through `set-member-scope!` stays green.

**Witness shape.** Rung **c**: enumerate {open scope, one track, two tracks, unrecognized shape} ×
{default view, `?all=1`, explicit `&track=`} × {board rows, coverage count, rankable count},
comparing against a hand-written literal table — and include an explicit assertion that an
out-of-track submission is **reachable** under `?all=1` (that is the assertion a hard-fence
refactor would break). Add a rung-**a** guard that an unrecognized scope shape is a typed refusal
rather than `nil`.

**HLD section.** **MISSING** — belongs in the new `## Authorization model` section, drawing the
line the code draws and the tests do not name: **scope narrows a view; it never grants or denies a
write.**

---

### G6 — Bletchley applicant lane: sheet fence + idle-poll idempotence · blast radius: HIGH (largest unlinked subsystem)

Commits `144a5fb9d` "bp lane: read the Bletchley applications sheet into an isolated event log"
(+917), `3997b3608` "the sheet is the record, and the surface is /events/:slug/applicants"
(+762/-904), `16364c3c1` "live sync: an idle poll must append nothing, and a change is a change to
a ROW", `21b0c4e01` "the applicants surface belongs to ONE conference; scope it that way",
`6b9851049` "bp lane: claim AH..AK on the real sheet — and move the fence, because AC has data".
Implementation: `src/cfp_scheduler_killer/bp_apps.clj` (`refresh!` L851, `cells->applicant` L824,
fence constants L172-238, `assert-machine-lane!` L587, `assert-our-column!` L680, `write-by-key!`
L736, `claim-machine-lane!` L1030), `src/cfp_scheduler_killer/handlers/bp.clj` (`poll-once!` L665,
`push-to-watchers!` L652, `start-poller!` L676), `src/cli/bp_setup.clj`.

**Marker state.** **Zero `INTENT:` in any of the three files** — and by churn this is the single
largest unlinked subsystem in the repo (`bp_apps.clj` 2,087 · `views/bp_rating.clj` 1,496 ·
`handlers/bp.clj` 1,380 lines of churn since 2026-08-15, **all with zero markers**). The design
rule that no `bp.*` event ever enters a Curtain Call store is stated as unlabelled prose at
`folds.clj:953-970`.

**Tests today.** `bp_apps_test.clj` — 800 lines, no markers; the two fences are well covered
(`a-column-we-did-not-create-is-untouchable-at-any-address-test`,
`the-machine-lane-refuses-every-human-column-before-the-network-test`,
`one-bad-cell-refuses-the-whole-batch-test`). **The idle-poll promise has zero tests** — nothing
exercises `refresh!`, `poll-once!`, or asserts `:changed`.

**EARS rows (draft) — three:**
> `:BP-FENCE-001` — *When the applicants lane writes back to the spreadsheet, the write shall
> touch only columns Curtain Call created, identified by header name at any address.*
> `:BP-POLL-001` — *When a live-sync poll re-reads a sheet no human has edited, the system shall
> report zero changed rows and push no repaint to any watcher.*
> `:BP-SCOPE-001` — *When any `/events/:slug/applicants*` route is requested with a slug other
> than the Bletchley Park event, the system shall answer as if the surface does not exist.*
> `:misreadings` — **"The read fence still means A:AG."** `read-range-a1` (L156) is **dead code at
> HEAD** and its docstring is now false by design ("Deliberately not A:AK: reading our own lane
> back would fold our verdicts in as though a human had written them") — `refresh!` reads the whole
> tab on purpose since "the sheet is the record". A maintainer trusting that docstring re-narrows
> the read and silently erases every verdict, tag and score from the cache; nothing tests
> `read-range-a1`, `refresh!`, or the round-trip. **"A change is a change to the fetch."**
> `:changed` compares only keys present in the new map, so a row *deleted* from the sheet is never
> "changed"; and re-adding `:fetched-at` to the compared map reinstates "every poll changed
> everything" with nothing to catch it.

**Witness shape.** Rung **b** first and cheaply: a fixture-backed `refresh!` (inject the sheet
reader) asserting `:changed` is empty on an identical second read, non-empty for exactly the one
edited row, and that `push-to-watchers!` was not called — this is the promise with **zero**
coverage today. Rung **e** for the read fence: delete `read-range-a1` or make it a typed refusal
so the stale docstring cannot mislead. Rung **c** for the fence: enumerate {our header at its
address, our header moved, a human header at our address, an unknown header} × {read, write}
against a literal expectation table.

**HLD section.** **MISSING** — add `## External lanes` under System Design: the applicant lane's
isolation rule (no `bp.*` fact ever reaches a Curtain Call store), the two-fence column ownership
model, and the one-conference clamp.

---

### G7 — Frozen rating order (queue identity) · blast radius: HIGH

Commits `4d6be0afd` "/bp/rate: the hidden rating screen, keyboard-first, writing back to the lane"
(+751), `cb1f5b510` "bp: an order frozen on an empty cache re-takes itself once applicants exist",
`a795108dd` "the board, the paned rate view, tags, and Gene's own columns" (+1867/-421).
Implementation: `src/cfp_scheduler_killer/handlers/bp.clj` — `lens-order` L88, `session!` L110
(the empty-cache re-take), `relens!` L128 (the only place an order is re-taken), `ctx` L147
("the order is fixed; the contents are live"), `sessions` atom L78.

**Marker state.** None. The promise is prose in the ns docstring (L17-23).

**Tests today.** `bp_apps_test.clj` tests the **pure comparator** thoroughly
(`every-sort-is-stable-so-re-sorting-never-shuffles-the-screen-test`, etc.) and **nothing anywhere
touches `sessions`, `session!`, `relens!`, or `ctx`.** The frozen-order identity promise and the
empty-cache re-take have no witness at all.

**EARS row (draft).**
> `:BP-ORDER-001` — *While a rating session holds a frozen order, when a rater records a verdict,
> the applicant's position in the queue shall not change.*
> Split: `:BP-ORDER-002` — *When a rating session's order was frozen while the applicant cache was
> empty, the next load shall re-take the order.*
> `:misreadings` — **"A newly-arrived applicant should just appear."** There is a real visible gap
> at HEAD: a poller-delivered applicant never enters an already-frozen order, and the obvious fix
> — relaxing `session!`'s `(empty? (:order s))` guard to a count comparison, or calling
> `lens-order` inside `ctx` — **un-freezes the queue mid-pass, sliding the row out from under the
> hand that just rated it**, which is the exact failure the freeze exists to prevent. Every test
> stays green because the whole session layer is untested. Also unwitnessed: `relens!`'s
> focus-preservation rule (keep focus on the same person, fall back to index 0), which would
> silently degrade to "always jump to top".

**Witness shape.** This one is a textbook rung **c**: enumerate the interleaving
{rate, rate, poller-inserts, rate, sort, filter, sync} and assert the rendered order equals a
hand-written literal sequence at each step — the failure is one point in a family (interleavings),
which is exactly the skill's trigger for a state-space test. Add a rung-**b** direct witness for
the empty-cache re-take and for focus preservation across `relens!`.

**HLD section.** **MISSING** — a Tenet: *"A queue's order is frozen for the hand that is working
it; contents may change, positions may not."*

---

### G8 — Presenter visibility: one authority, unenforced · blast radius: HIGH (privacy)

Commits `000f8e0d3` "Presenter visibility has one authority, and every renderer says so",
`17ecffdbd` "A blinded reviewer is not offered the speaker's management page", `645d59377` "Move
blind review notice into sidebar control".
Authority: `src/cfp_scheduler_killer/review_plan.clj` (`visibility-policy-event` L30). Rule:
`src/cfp_scheduler_killer/voting_policy.clj` (`presenter-visible?` L81, `hide-presenter-info?` L37),
`src/cfp_scheduler_killer/domain/review_plan.clj` (L138/153). Renderers:
`views/organizer_layout.clj` L66-111, `views/review.clj` L548/554/932-939,
`views/submission_row.clj` L226/234, `handlers/board.clj` (six sites).

**Marker state.** `REV-VIS-001/002` mark the **rule**; **nothing marks
`review-plan/visibility-policy-event` as the sole authority.** The registry rows name
`:rev-vis-001-redaction-holds-before-vote` and
`:legacy-hide-identity-redacts-reviewer-and-anonymous-api-surfaces` — **neither is the name of the
deftest literally called `presenter-visibility-has-one-authority-and-never-blinds-the-chair-test`**
(`board_test.clj:477`), which carries no marker.

**EARS row (draft).**
> `:REV-VIS-004` — *When any surface renders or describes presenter identity to a reviewer, it
> shall derive visibility from the durable review-plan policy.*
> Paired refusal row: `:REV-VIS-005` — *When application source reads
> `[:settings :hide-presenter-info]` outside the visibility authority, the architecture guard shall
> fail.*
> `:misreadings` — **"Copying the adjacent idiom is fine."** Direct legacy reads are still live and
> copyable at `views/integrations.clj:280`, `views/review.clj:554/932/938`,
> `views/submission_row.clj:234`, `reviews.clj:270`. An event created with Blind review carries the
> durable policy but **not** the legacy flag — so a new reviewer-facing surface (print sheet, CSV,
> htmx fragment, API projection) that copies `(voting-policy/hide-presenter-info? event)` renders
> full presenter identity to a blinded reviewer. Every existing test passes: each fetches one known
> URL, so a surface they do not name is unwitnessed by construction.

**Witness shape.** Rung **e** plus a source sweep — the strongest available and the repo already
owns both patterns (`io_architecture_test.clj` for GCP edges,
`no-call-site-rewrites-the-vocabulary-by-hand-test` for the decision vocabulary). Make
`hide-presenter-info?` private to the authority ns, expose one `presenter-visible?` entry point,
and add an architecture test that fails on any `[:settings :hide-presenter-info]` read outside it.
Then a rung-**c** matrix over {every reviewer-facing route} × {open, reveal-after-vote, blind} ×
{chair, reviewer, anonymous} — enumerated from the route table, so a NEW route is covered the day
it is added rather than the day someone remembers.

**HLD section.** **MISSING** — belongs in `## Authorization model` alongside G1/G5, as a named
sub-rule: *presenter visibility has exactly one authority and every renderer derives from it.*

---

### G9 — Telemetry: the fail-quiet writer and the missing boot probe · blast radius: MEDIUM-HIGH

Commits `b53129cde` "Telemetry writes to BigQuery, and says so out loud when it cannot",
`57660014d` "Telemetry retargets to our own table, and the async guarantee gets a pin",
`a6068368a` "Fix forward: BigQuery gets an adapter, telemetry keeps only meaning".
Implementation: `src/cfp_scheduler_killer/telemetry.clj` (`bq-write-batch!` L250, `flush-once!`
L295, `probe-sink!` L340, `start!` L362), `src/cfp_scheduler_killer/io/bq.clj` (`insert-payload`
with `skipInvalidRows`/`ignoreUnknownValues` both `false`; `insert-all!` throws on non-2xx **and**
on 200-with-`insertErrors`).

**Marker state.** None in src. Two ID-less prose `INTENT:` comments in `telemetry_test.clj`
(L177 purity/privacy, L262 Gene's async guarantee) — §2.2.

**Tests today.** `telemetry_test.clj` (4 deftests), `io_architecture_test.clj`. No markers.
**`probe-sink!` has zero test coverage** — it appears only at its defn and its single call site.

**EARS rows (draft) — two:**
> `:TELEM-TRUTH-001` — *When the telemetry sink rejects a batch or accepts it with per-row
> `insertErrors`, the flusher shall count a write failure and retain the batch for retry.*
> `:TELEM-PROBE-001` — *When telemetry is enabled at boot and the sink refuses a canary row,
> startup shall log `:telemetry-sink-unreachable` at ERROR.*
> `:misreadings` — **"The noisy retries are the bug."** Flipping `skipInvalidRows` /
> `ignoreUnknownValues` to `true`, or dropping the `insertErrors` throw inside `insert-all!`,
> reinstates the original fail-quiet writer — and `bq-payload-refuses-partial-writes-test` asserts
> those flags only on the **payload map** built by `insert-payload`, so a change inside
> `insert-all!` keeps it green. **"The boot probe is startup noise."** Deleting or short-circuiting
> the `probe-sink!` call in `start!` is invisible: nothing tests it.

**Witness shape.** Rung **b** against a stubbed HTTP edge: a 200-with-`insertErrors` response must
produce a counted failure and a retained batch (assert the counter and the queue, not the log
string); a refusing sink at boot must produce the ERROR. Rung **a** guard: assert the two flags at
the **`insert-all!`** boundary (the production path), not on the payload map — per *witness through
the production path*.

**HLD section.** **MISSING** — one line in the new `## Delivery and notification` section, or its
own `## Telemetry`: *a write that was not accepted is never recorded as accepted.*

---

### G10 — Archived event publication boundary · blast radius: MEDIUM-HIGH (was a prod bug)

Commit `b78462e61` "An archived event keeps its link and stops publishing its program" (+411/-367).
Implementation: `src/cfp_scheduler_killer/handlers/public_widgets.clj` — **`visible-event` L77-90**
is the enforcement point (`(when-not (:archived-at event) event)`), used by ~20 public child
handlers; `listed-event` L63-75 deliberately does **not** check archived; `handle-program` L105-140
serves the notice at 200. Also `exports.clj:1145-1150` (removed from catalog/llms.txt).

**Marker state.** None for archiving — **and one actively misleading one**: `;; INTENT: EMB-005`
sits at `views/public_widgets.clj:1183`, immediately above `archived-program-page`, but EMB-005's
text ("the one canonical Program surface every public entrance renders") describes `program-page`,
where the same marker is repeated at L1211. **A maintainer grepping the archived page's intent gets
a row that says the opposite of what that function does.** Existing witness:
`archived-event-keeps-its-permalink-but-not-its-program-test`
(`public_widgets_test.clj:870`), preceded by an ID-less prose `INTENT:` (§2.2).

**EARS row (draft).**
> `:ARCHIVE-PUB-001` — *When an archived event's public permalink is requested, the application
> shall answer 200 with the archived notice and no session or speaker rows.*
> Split: `:ARCHIVE-PUB-002` — *When any public child surface of an archived event is requested, the
> application shall answer 404.*
> `:misreadings` — **"`listed-event` and `visible-event` are interchangeable helpers."** The
> boundary is enforced by *which private helper a handler happens to call*, with no manifest and no
> route-level assertion. Any new public child surface reaching for `listed-event` — or calling
> `events/event-by-slug` directly — republishes an archived event's full program to anyone with the
> slug: the original production bug verbatim, with the suite green because the test names only four
> hard-coded paths. Second: `listed-event` consults `auth/member-of-event?`, so a signed-in chair
> takes a different branch and nothing asserts they still get the notice.

**Witness shape.** Rung **c** driven from the route table, not a hard-coded list: enumerate every
declared public route for an archived event and assert the literal expected status
(permalink→200+notice, everything else→404) — so a new public route is covered on the day it is
added. Rung **a**: fix the misplaced `EMB-005` marker in the same change (per the skill: *if an
oracle existed and missed the bug, correcting it is part of the fix*).

**HLD section.** **MISSING** — a Key Design Decision: *"Archiving is a fact, not a deletion: the
permalink survives, the program does not."* The existing "Legacy adoption is additive" decision is
adjacent but does not cover publication.

---

### G11 — A persisted URL must never name the receiving machine · blast radius: MEDIUM-HIGH

Commit `430c373da` "a stored URL must not name the machine that received the request" (+367/-340).
Implementation: `src/cfp_scheduler_killer/announce.clj` (`merge-headshot-upload` stamps
`"/headshots/<id>"`), plus `handlers/files.clj`, `handlers/portal.clj`, `handlers/speakers.clj`,
`handlers/board.clj`, `domain/speaker_tasks.clj` (`valid-headshot-url?`), `submissions.clj`.

**Verdict against the existing `CANONICAL-LINK-001`: NOT COVERED — this is a distinct promise.**
CANONICAL-LINK-001 governs *emission* ("while the application emits an absolute IDENTITY URL …
shall use the configured canonical origin") and is resolved per request; all nine of its `INTENT:`
tags sit on emission sites and all its witnesses are response-body sweeps over a freshly-created
event. This promise is about a value **written into the shared event store and read back from a
different environment months later**, and its correct shape is *host-relative, no origin at all* —
which CANONICAL-LINK-001's EARS neither requires nor permits. A response sweep cannot see a
poisoned stored fact written by another host.

**Marker state.** No marker at any stored-URL site. `stored_urls_test.clj`'s ns docstring carries
the ID-less prose *"INTENT: A URL WE PERSIST MUST NOT NAME THE MACHINE THAT RECEIVED THE REQUEST."*

**EARS row (draft).**
> `:STORED-URL-001` — *When the application persists a resource URL into the event store, it shall
> store a host-relative path.*
> `:misreadings` — **"The class guard covers the class."** The guard is a single brittle regex,
> `#"request-host[^)]*\)\s*\"/headshots/"`. A stored URL for any *other* asset kind
> (`/attachments/`, `/slides/`, `/logos/`), or the same headshot URL built via a helper, `str/join`,
> or a threaded `->` so the literal no longer directly follows the call, reintroduces
> multi-environment poisoning with both tests green. This is the constellation's own
> *scanner-brief-names-vs-spellings* failure mode: a source-scanning control derived from one
> spelling cannot see the same call written differently.

**Witness shape.** Rung **e**: give persistence a typed value (`->StoredPath`) that cannot be
constructed from a request host, so the bad state is unrepresentable — then the regex guard becomes
a backstop rather than the control. Failing that, a behavioural witness at the boundary: append a
fact through each production write path with a hostile `Host:` header and assert the **stored**
value (read back from the store, not the response) is host-relative — for every asset kind, driven
from a list of persisted-URL fields rather than one literal.

**HLD section.** **MISSING** — a Tenet: *"A persisted value must be readable from any host; only
responses may name an origin."* Pairs with, and is distinct from, the canonical-origin rule.

---

### G12 — Recusal history must survive a replay · blast radius: MEDIUM-HIGH (recovery)

Commit `f3a46d991` "recusal: post-click state is visible and its history survives a restore (ABS-12)".
Implementation: `src/cfp_scheduler_killer/folds.clj` — `reviewer.recused` L694 / `reviewer.unrecused`
L704, both appending to `[:review-recusal-log key]` while `:review-recusals` holds only current
state; `src/cfp_scheduler_killer/domain/review_work.clj` L15-42.

**Marker state.** None at any site.

**Tests today.** `reviews_test.clj:381`
`recusal-post-click-state-is-visible-and-history-survives-restore-test` and
`domain/review_work_test.clj:36` `recusal-is-algebraic-idempotent-and-reversible` — no markers, and
**critically, "survives a restore" is tested only against the live snapshot.** `replay_test.clj`
contains no recusal case; **no test refolds the event log from empty and compares the log.**

**EARS row (draft).**
> `:RECUSE-LOG-001` — *When the event log is replayed from empty state, the recusal log shall be
> identical to the log held before the replay.*
> Split: `:RECUSE-LOG-002` — *When a reviewer restores after recusing, the projection shall clear
> the current recusal while the log retains one ordered entry per event.*
> `:misreadings` — **"The log is a derivable cache."** Rebuilding it from `:review-recusals` on
> load, or `dissoc`-ing the log key in the unrecused arm for symmetry with the arm above it, passes
> every existing test — none of them discards in-memory state and refolds. History evaporates on
> the next restart: precisely the recovery promise the commit was written for.

**Witness shape.** Rung **b**, and it is nearly free because the harness exists: drive
recuse→restore→recuse, snapshot the log, refold the event log from empty, assert byte-equality of
the ordered log. **The word "restore" in the test name currently means "un-recuse", not "restart" —
the test does not test the promise its name claims.** That renaming is itself part of the fix.

**HLD section.** Covered in spirit by `### Fact and view flow` ("Historical fact readers remain
permanent") — but add the explicit invariant *every projection must be reproducible by replay from
empty*, and name recusal as an instance.

---

### G13 — Scheduler: a block occupies its room, blackout windows, auto-place counts · blast radius: MEDIUM

Commits `9784946a2` "Speaker blackout windows as scheduling constraint", `ca545c786` "One-action
auto-place with placed/unplaceable report (AIA-08)", `5b8ec9577` "Blocks occupy their rooms when the
scheduler reports, not only when it proposes".
Implementation: `src/cfp_scheduler_killer/schedule.clj` (`blackout-conflicts` L278, `conflicts` L302
with `occupants` L320-328 and the room arm L350-357), `src/cfp_scheduler_killer/handlers/schedule.clj`
(`handle-schedule-suggest` L818-831, `unplaceable` at L827).

**Marker state.** None for these three. **A trap:** `schedule.clj`'s only id is
`;; INTENT: SCHED-002` at L367 — a *different* promise (withholding conflicted sessions from public
surfaces) sitting ~10 lines below the block-occupancy code, so a grep for "the intent covering
conflicts" lands on the wrong row. The occupancy rationale (L312-319) is prose citing "AIA-05, two
judges, bd 7llg.3" with no id. The `block_room_conflict` test is preceded by ID-less prose (§2.2).

**EARS rows (draft) — three:**
> `:SCHED-BLACKOUT-001` — *When a session is placed inside a speaker's blackout window, the
> scheduler shall report a high-severity blackout conflict naming that speaker.*
> `:SCHED-OCCUPY-001` — *When two occupants of the same room overlap in time — sessions, blocks, or
> one of each — the scheduler shall report a room double-booking.*
> `:SCHED-AUTOPLACE-001` — *When an organizer triggers auto-place, the system shall report the
> placed count and the unplaceable count from the placement result.*
> `:misreadings` — **"One of the two implementations is the real one."** Room occupancy is
> implemented **twice, independently**: `schedule/conflicts` (report path) and
> `schedule-suggestions/block-conflict?` (propose path), with nothing asserting they agree.
> Refactoring either — skipping an unroomed or zero-duration block, changing `overlap?` boundary
> semantics on one side — re-opens the AIA-05 bug (proposes over a block, or reports zero conflicts)
> with `block-room-conflict-test` green, because that test drives only the report path.
> **"`unplaceable` is a report."** It is `(- unscheduled placed)`, a subtraction; if `apply!` ever
> places something outside the tray or moves an already-placed session, the banner shows a wrong or
> negative number and both pinned cases (3/0, 0/3) still pass.

**Witness shape.** Rung **e**: one shared occupancy predicate consumed by both the report and
propose paths, so disagreement is unrepresentable; plus a rung-**c** differential test that
enumerates {session, roomed block, unroomed block, zero-duration block} × {overlap, touch, disjoint}
and asserts **both paths return the same verdict** — the agreement itself is the assertion. For
auto-place, have `apply!` return `{:placed [...] :unplaceable [...]}` and assert the banner from
those collections, never from arithmetic.

**HLD section.** **PARTIAL** — `## Approach` and the Tenets mention Rough Blocking, and the
`Blocks are non-talk agenda items` decision exists, but nothing states *a block occupies its room*
or the conflict-report contract. Extend that decision.

---

### G14 — Per-person submission cap and the withdrawn-frees-a-slot rule · blast radius: MEDIUM

Commits `f353da71e` "event details: an organizer can set a per-person submission cap",
`3f5056d3c` "submissions: no default per-person cap; withdrawn talks free a slot (rxom)".
Implementation: `src/cfp_scheduler_killer/submissions.clj` (`submission-cap` L373,
`submission-count-for-email` L379, `cap-reached?` L389, enforcement in `create-submission!` ~L739
throwing `{:type :cap-reached}`, `capture!` L874 deliberately exempt),
`src/cfp_scheduler_killer/events.clj` `set-submission-cap!` L877, `folds.clj:870`.

**Marker state.** None in any of the five files. Tests exist and are decent
(`submission_cap_control_test.clj`, `submissions_test.clj:261`) — no markers.

**EARS rows (draft) — two:**
> `:SUB-CAP-001` — *When a speaker submits to an event whose organizer set a per-person cap, the
> system shall refuse the submission once their count of non-withdrawn talks reaches that cap.*
> `:SUB-CAP-002` — *While no organizer has set a cap, the system shall impose no per-person limit.*
> `:misreadings` — **"Withdrawn is a string."** The slot is freed by testing
> `(not= "Withdrawn" (:status s))` — one exact-case literal on the projected submission. A second
> withdrawal representation (lowercase, `"Retracted"`, a `:withdrawn-at` timestamp, a soft-delete
> flag, or normalization in the review fold) silently makes withdrawn talks consume the cap forever;
> the speaker is locked out and the error says only "Submission limit reached". The test drives
> withdrawal through `reviews/set-status!` with the exact literal, so it passes.
> **"The demo override is a default."** `submission-cap` is `(or (demo-submission-cap event)
> settings-cap)` — the env override **beats an organizer who explicitly cleared the cap**, and no
> test pins that precedence in either direction.

**Witness shape.** Rung **e**: make withdrawal a predicate over a typed status value rather than a
string comparison at the call site (the repo already has the pattern —
`domain/decision_status.clj` plus its `no-call-site-rewrites-the-vocabulary-by-hand-test` source
sweep); extend that sweep to cover cap counting. Rung **b**: pin the demo-override precedence
explicitly in both directions.

**HLD section.** **MISSING** — one line under `### Draft validity differs from publication
validity`, or a new decision *"Capacity counts non-withdrawn work only."*

---

### G15 — Decision-status vocabulary and the unrated sort · blast radius: MEDIUM

Commits `497012a45` "decision status: one vocabulary for 'no longer open work' (zl91)",
`ea97074c9` "review progress: a decided submission is complete-by-decision (q6gx, ABS-08 part 1)",
`bd25e9f99` "board sort: an unrated talk sinks in BOTH directions (ABS-10, Stars sort)",
`a8fe913f1` "board: name the unrated, and say which queue you are looking at".
Implementation: `src/cfp_scheduler_killer/domain/decision_status.clj`
(`terminal-decision-statuses`, `review-obligation-closed-statuses`),
`src/cfp_scheduler_killer/reviews.clj` (`rank-by-mean` L239, `sort-board` L294),
`src/cfp_scheduler_killer/domain/review_work.clj` (`progress-for-reviewer` L56-88).

**Marker state.** None. (`reviews.clj:110` carries `REV-BOARD-INDEX-001` — the *performance* index
promise, an unrelated concern in the same file: another wrong-row-on-grep trap.) This group already
has the repo's **strongest** un-linked guard,
`no-call-site-rewrites-the-vocabulary-by-hand-test` — it deserves an id more than most.

**EARS rows (draft) — two:**
> `:BOARD-SORT-001` — *When the board is sorted by mean stars in either direction, a submission
> with no rating shall sort after every rated submission.*
> `:REV-OBLIG-001` — *When a submission's status is Accepted, Waitlisted, Declined, or Withdrawn,
> it shall leave every reviewer's remaining-work count.*
> `:misreadings` — **"`rank-by-mean`'s tuple is an implementation detail."** `sort-board`'s
> `"ready-to-decide"` arm destructures it **positionally** (`first`/`second`) and re-appends its own
> tie-break, while the `"avg"` arm uses the whole tuple. Collapsing the tuple because a `nil` middle
> element "looks like a bug", or reordering it, silently changes ready-to-decide's meaning while
> `unrated-talks-sort-last-in-both-directions-test` — which only checks *which end* the unrated
> lands on, with distinct means — stays green. **"Terminal means communicated."** `"Withdrawn"` is
> `conj`ed onto `terminal-decision-statuses`; adding a new terminal status (e.g. `"Cancelled"`) to
> the base vector automatically enrols it in the **communicated** set, so a letter is sent for a
> state nobody decided. The source sweep only forbids hand-rolled sets; the vocabulary test asserts
> members, not the closure rule.

**Witness shape.** Rung **c** with ties: enumerate {rated high, rated low, tied means, unrated} ×
{asc, desc, ready-to-decide, contested} against a hand-written literal ordering **including the
tie-break**, so a positional destructure change is caught. Rung **a** for the vocabulary: assert the
*relationship* — every terminal status is review-obligation-closed, and the communicated set is
named explicitly rather than derived by `conj` — so adding a status forces an explicit decision.

**HLD section.** **MISSING** — add to `## Success Metrics` or a decision: the review-obligation
vocabulary and its closure rule. (Note the HLD's Success Metrics are entirely Charlotte/Bletchley
migration counts — see §5.)

---

### Runners-up (confirmed load-bearing, ranked 16-18, same treatment warranted)

- **Event clone / cross-event leak** — `d4a89d153`, `2ee46f783`. `event_clone.clj` has no marker and
  `clone-mints-fresh-structure-without-copying-history` asserts only submission count plus field/room
  /committee shape — never ratings, comments, recusals, review assignments, schedule slots, or the
  clone's raw log. A "make the clone usable immediately" change that copies the review plan or seeds
  slots via `store/append!` passes every assertion, and `submission-in-event!` does not help: it
  guards the *mutation* boundary, which a clone-time append bypasses. EARS:
  *When an organizer clones an event, the clone shall receive zero facts from the source event log.*
  Witness: refold the clone's log and assert its fact-type set is a subset of the structural types.
- **Agenda-fragment external contract** — `8803976f6`/`5193fae6d` "fail closed behind content
  negotiation", `161c9d9bb` "publish speaker IDENTITY, never make a host page guess it". Prose
  `INTENT:` at `public_widgets_test.clj:936` calls it "a CONTRACT with…" — an external consumer
  contract with no id.
- **llms.txt / agent-facing promises** — prose `INTENT:` at `routes_contract_test.clj:389` ("an
  unpinned promise is the…") and `agent_test.clj:457` (speaker text is data, not instructions —
  a prompt-injection boundary). `AGENT-URL-001/002/003` exist and are witnessed; these two adjacent
  promises are not.

---

## 4. HLD omissions

`docs/high-level-design.md` (16 KB) is an excellent design document **for one domain** — the
canonical agenda and the append-only correction boundary. Its sections: Problem · Approach ·
Target Users · Goals · Non-Goals · Tenets · System Design (Domain identities · Fact and view flow ·
Correction boundary) · Key Design Decisions (9) · Success Metrics · FAQ · References.

**It is not a system HLD, and three things make that costly rather than merely incomplete:**

1. **It never mentions the intent registry or LID at all** (`rg -i 'intent|LID|EARS|registry'`
   returns only incidental prose). The repo's own strongest engineering practice is invisible in its
   own architecture document, so a new seat reads the HLD and does not learn that load-bearing code
   requires an intent row.
2. **Its References list one of six intent topics** (`intent/speaker-profile-edit/…-design.md`).
   `canonical-agenda`, `append-only-correction`, `communications`, and both rough-blocking designs
   are unreferenced, despite `canonical-agenda` being the HLD's own subject.
3. **Its Success Metrics are a migration checklist, not system metrics** — "Charlotte reports
   exactly 16 null-title talks…", "Bletchley Park receives exactly 36 geometry rows…". Those were
   true on a date. Nothing states what "healthy" means for the running product.

**Sections the HLD should have and does not** (each maps to gap groups above):

| missing HLD section | why | gap groups |
|---|---|---|
| `## Authorization model` | No section describes personas, the declared-policy gate, API-key scopes, committee scope, or the fail-closed rule — the largest cluster of unlinked load-bearing code | G1, G5, G8 |
| `## Delivery and notification` | Mail is an outbox projection with an approve→send gate, a simulation firewall, and a dev-log branch that can record a send that never happened; none of it is in the HLD | G2, G9 |
| `## Exports and receipts` | The "receipt beside settings, never inside" rule is a real architectural constraint discovered by a production bug and recorded nowhere | G3 |
| `## External lanes` | The applicant lane keeps a whole event log isolated from the Curtain Call store, with a two-fence column ownership model — the largest unlinked subsystem, undocumented | G6, G7 |
| `## Public surface boundary` | Which helper enforces archived/unlisted, what a public child surface may call, the agenda-fragment external contract, and llms.txt as an agent promise | G10, runners-up |
| `## Storage durability` | Blob backends, the shared-store refusal, the host-relative persisted-URL rule | G4, G11 |
| **Tenet: "A refusal preserves the operator's work and is loud"** | The Tenets cover history, partial truth, identity, fresh proof, auto-flow, receipts — **not refusal**, which is one of this repo's most-exercised design moves | G4, G6, G13 |
| **Tenet: "A persisted value must be readable from any host"** | Distinct from the canonical-origin emission rule and confused with it today | G11 |
| **Invariant: "every projection is reproducible by replay from empty"** | `### Fact and view flow` implies it; nothing states it, and the recusal log currently has no replay witness | G12 |
| **`## Engineering practice` naming LID** | The registry, the two marker conventions, the contract test, the oracle wiring, and the rule that a load-bearing change carries an intent | §2.1, §2.2 |

**Opportunistic HLD wins, cheapest first:** (a) add the four missing intent-topic links to
References — one line each, zero risk; (b) add the `## Engineering practice` section pointing at
`docs/intent/registry.edn` and `intent_contract_test.clj`; (c) split Success Metrics into
"Migration acceptance (Charlotte/Bletchley, closed)" and "System health"; (d) then the six missing
System Design sections, in gap-group order.

---

## 5. Recommended order of work (highest ratchet per hour first)

1. **Extend `intent_contract_test.clj` to the specs files** (§2.1). One test change; turns 93
   documentation rows into ratchets; goes red on 38 real missing witnesses.
2. **Ban `;; INTENT:` under `test/`** in the same test, and convert the 25 prose intents (§2.2) into
   registry rows with `INTENT:`/`INTENT-TEST:` tags. Each is a promise that is *already tested* —
   this is pure linkage, no new behaviour, and it retires the largest silent class in the repo.
3. **G1, G2, G8** — authorization, delivery truth, and presenter-visibility authority. All three have
   a reachable rung **e** (unrepresentable bad state) and all three currently have their strongest
   assertion in a test whose name does not appear in any registry `:tests` vector.
4. **G6/G7** — the applicant lane, because it is the largest zero-marker subsystem and its
   idle-poll and frozen-order promises have **no tests at all**, not merely no markers.
5. **Correct the three wrong-row-on-grep traps** found this session, each a one-line fix that
   prevents a maintainer from reading the wrong intent: `EMB-005` above `archived-program-page`
   (`views/public_widgets.clj:1183`); `SCHED-002` ten lines below the block-occupancy code
   (`schedule.clj:367`); `REV-BOARD-INDEX-001` in the middle of the sort/vocabulary code
   (`reviews.clj:110`). Per the skill: *if an oracle existed and missed the bug, correcting it is
   part of the fix.*
6. **HLD**: References + `## Engineering practice` + Success Metrics split (an hour), then the six
   System Design sections as their gap groups are closed.

---

## 6. Raw commands

```bash
# orientation
git -C /home/forge/src/curtaincall-cfp log -1 --format='%H %ad %an' origin/main
git -C /home/forge/src/curtaincall-cfp log origin/main --since=2026-08-15 --oneline | wc -l
git -C /home/forge/src/curtaincall-cfp log origin/main --since=2026-08-15 --format='%an' | sort | uniq -c | sort -rn

# marker census
rg -n 'INTENT:'      src        # 40 tags, all id-bearing
rg -n 'INTENT-TEST:' test
rg -n '@spec'        src test
rg -n ';;\s*INTENT:\s*' test | rg -v ';;\s*INTENT:\s*[A-Z][A-Z0-9]*-'   # 25 prose intents, no id
rg -n ';;\s*INTENT:\s*' src  | rg -v ';;\s*INTENT:\s*[A-Z][A-Z0-9]*-'   # 0

# registry + specs inventory  (inline python, see cc.md §2.1)
rg -o ':id\s+:[A-Za-z0-9?!*<>=+._-]+' docs/intent/registry.edn | wc -l    # 51
rg -c '^\s*-\s*\[[ x]\]\s*\*\*[A-Z]' docs/intent/*/*-specs.md            # 93 total

# mechanical pass (295 src-touching commits, 5-commit window)
python3 /var/tmp/forge/lid-audit/mech.py            # -> mech.json, counts table
python3 - < grouping snippets                        # -> groups.txt (168 gap rows, ranked)

# per-file churn vs marker presence
git -C /home/forge/src/curtaincall-cfp log origin/main --since=2026-08-15 --numstat --format='' -- src \
  | awk 'NF==3 && $1!="-" {a[$3]+=$1+$2} END{for(f in a) print a[f], f}' | sort -rn | head -30

# the repo's own LID gates (both green this session)
make test-oracles
bin/kaocha unit --focus cfp-scheduler-killer.intent-contract-test \
                --focus cfp-scheduler-killer.intent-registry-architecture-test \
                --focus cfp-scheduler-killer.intent-witness-identity-architecture-test
```

**Artifacts.** `/var/tmp/forge/lid-audit/mech.py` (script) ·
`/var/tmp/forge/lid-audit/mech.json` (295 classified commits) ·
`/var/tmp/forge/lid-audit/groups.txt` (168 gap rows, ranked by churn) ·
`/var/tmp/forge/lid-audit/cc.md` (this ledger).

**Honest limits of this audit.** (a) The mechanical classifier is a *window* heuristic — a commit
whose witness landed 6+ commits later reads as a gap; the judgment pass corrected this for the top
groups only, so the 168 figure is an upper bound on real gaps and the 15 groups are the load-bearing
findings. (b) Per the skill's own honest limit: **traceability proves an intent is witnessed, never
that it was right.** Every EARS row and misreading drafted here is a proposal for a human to ratify,
not a finding about correctness. (c) Nothing was executed against production, no fleet directory or
runtime port was touched, and no file in the repository was modified.
