Gene: yes. Give me an answer I can act on: “your candidate is running,” “this assertion broke here,” or “your requested checks completed against these bytes.” I want a quiet success and a useful failure. I do not want to reconstruct a verdict from a terminal transcript.

Fable's three shapes are a good starting point. I would change their encoding, save identity, and ownership. Three runner events suffice; a separate status response must represent waiting, death, and uncertainty. Making those look like test completion would save one grammar production by destroying the distinction we need.

This is a proposal, not an implementation or a new lane. Compose with my existing run-receipt + affected plugins and the LATEST/kaocha-status work already underway. The inspected source reserves `<id>.edn.tmp` in pre-load and renames it in post-run; its README describes that too loosely. Status tests exist; the executable was absent when inspected. Nothing here claims that unfinished entrance has shipped.

The reporter should own event rendering; run-receipt should own run identity, lifecycle, durable results, and publication. Emit START after reserving the active record, DONE only after committing the final receipt and publishing its pointer. Recover a committed receipt whose output was lost through status; never rerun tests merely to obtain the line. LATEST is the status lane's index, not proof that the newest run belongs to me.

A reporter alone cannot promise “nothing else on stdout.” Kaocha's watcher, other plugins, capture bypass, JVM diagnostics, and background threads can print independently. Use a dedicated runner-owned event destination; kaocha-status projects it to its own stdout. Raw process output goes to a bounded diagnostic artifact referenced by the receipt. Human documentation/progress remains a separate selectable view. Never recognize verdicts by grepping arbitrary test stdout.

The local docs support this division: reporters consume event maps; hooks own lifecycle; post-summary is CLI-only, so it cannot own the common completion contract. See [09_extending](/var/tmp/forge/kaocha-src/doc/09_extending.md), [10_hooks](/var/tmp/forge/kaocha-src/doc/10_hooks.md), [08_plugins](/var/tmp/forge/kaocha-src/doc/08_plugins.md), and [capture_output](/var/tmp/forge/kaocha-src/doc/plugins/capture_output.md). Upstream's [reporter contract](https://cljdoc.org/d/lambdaisland/kaocha/1.91.1392/doc/9-extending) confirms the event-function seam.

For a clean live run, START then DONE is enough: two lines, not three. START must precede loading so a stuck load is visible; selection may still be unknown then. A late subscriber gets one current status or the matching DONE. Silence before START means “not yet observed,” never “running.” A failed assertion emits FAIL immediately; the first red does not mean the run finished.

No periodic reporter heartbeat by default. A ticking reporter thread proves only that the thread ticks. Status waits in one blocking call, emits a single late transition, and can offer opt-in 30-second liveness updates using the same STATUS shape. Include elapsed time and last observed phase, not reassurance that tests are progressing. No dots, spinners, repeated totals, or agent polling loop.

“Mine” needs content identity. `--since <file>` supplies a useful lower-bound freshness hint, but mtime can be coarse, preserved, changed again, or associated with another agent's save. A run starting after that time can still load the wrong bytes or omit the relevant checks. Keep --since as compatibility; label its identity unknown unless independently bound.

Prefer save, then `kaocha-status --mark <paths...>` → token, then `--follow --token TOKEN`. MARK is a local status operation, not a JVM launch. It records canonical workspace, watcher session, relevant configuration/classpath generation, required scope, and a content manifest including deletions. Token is an opaque handle to that record, not a timestamp. The editor/patch entrance can combine save+mark later if the extra call proves costly.

The token binds the saved candidate; a run has its own id. Status matches the token manifest to the run's loaded candidate and executed coverage, even if a fast run finished before MARK. One coalesced run may satisfy several identical candidates; it must not satisfy an overwritten candidate it never loaded. A later overwrite gives `superseded`, not somebody else's green. Changes outside watched roots give `uncovered`, with an on-demand probe as the recovery.

Extend the existing snapshot/generation mechanism, not a second receipt database. Require an unchanged observed generation across load/run and candidate hashes tied to reload; pre/post disk equality alone misses transient edits and stale warm Vars. If that binding is unavailable, report identity unknown. Config/classpath changes invalidate the warm-image attestation. One worktree image runs one Kaocha invocation at a time; serialize probe and watch requests.

Automatic follow-up retains its own id, parent id, and explicit flag. Follow-token selects the primary candidate run; following the followup-of link in the existing status lane avoids a newer automatic run hiding it. Do not blindly discard a follow-up carrying a genuinely new trigger. Crucially, a focused retry can pass while broader requested coverage remains pending: that pass cannot satisfy the token's full-scope request until the corresponding checks finish.

Use the identical START/FAIL/DONE grammar for watch, warm `kaocha.repl/run`, and cold gates, distinguished by mode. PROBE and GATE become modes in this versioned grammar, not separately invented summaries or duplicate lines. Migrate make test-probe and coldstart-grade together with an explicit old-format adapter. A warm PASS proves its declared checks only; a required cold gate remains required. Non-Kaocha gates use null test counts, never invented zeroes.

FAIL should let me locate and diagnose: run id; event index; fail/error/load kind; qualified test; workspace-relative source path and line; innermost testing context/message; assertion form; expected; actual; and a small structural difference. For exceptions, actual contains class and bounded message; location is the first relevant project frame when the assertion site is unavailable. A load error may have no Var: use null, never a made-up test. Preserve the exact full location in the receipt when it cannot fit inline.

Budget rendered expected and actual at 256 ASCII bytes each, expression/context at 160 each, and diff at 512. These are preview budgets after escaping, not permission to print a whole value and truncate afterward. Bound traversal to depth 6 / 128 visited nodes and a 10 ms cooperative budget; do not realize arbitrary lazy sequences or invoke arbitrary custom printers. Opaque/unsafe values get a type placeholder and explicit omitted status. Receipt capture must also avoid hanging on printing; “full details” means faithfully retained available evidence, not unbounded computation.

For a large equality map, spend diff bytes on up to three changed paths with old/new leaves: e.g. `[:invoice :total] expected 42 actual 43; missing [:customer :id]`. Use deterministic ordering for supported scalar keys and preserve the actual event order across failures. Reuse Kaocha/deep-diff's data representation where bounded inputs make it safe; keep its colored multiline printer in the human/detail view. Do not evaluate assertion forms to obtain values or pretend an unsupported assertion has an equality diff.

Display at most 20 failure events AND 16 KiB of FAIL lines per run, whichever binds first; each line ≤ 2 KiB. DONE carries shown/omitted counts, so “and N more” is data, not a fourth free-form sentence. Label every shortened field; distinguish actual nil from unavailable data. No stacks, captured stdout, ANSI, environment dumps, rerun shell snippets, or whole source forms inline. Some failures will still require the receipt; measure that rate rather than promise every fix on one read.

The September 6 [forgery ratchet](/home/forge/src/clj-surgeon-records/docs/observations/2026-09-06-sol-fence-refusal-text-r8.md) is directly applicable: fixed trusted prefix plus ASCII JSON; caller values are encoded data. A filename containing newline + RUN-DONE cannot become a second event. Quoting alone does not authenticate evidence: R12 must validate the referenced receipt's schema, digest, workspace, run, candidate, image/configuration, and required checks against independently held expectations. Tests can print JSON too; their stdout never enters the protocol destination.

Hung/killed handling belongs to status, which can outlive the JVM. Store session id plus PID/start identity in the active record; PID existence alone risks reuse. A dead process gives `killed` promptly on the next observation, conservatively `unavailable` if identity cannot be checked. Preserve an interrupted record through the existing reaper path; an old LATEST green must never reappear as the answer for that candidate. SIGKILL cannot produce a genuine runner DONE.

Use p95 as a lateness hint, never a deadline or failure verdict. For ≥20 comparable successful receipts of the same closure, mode, config/image and heap, propose late at max(10 s, 3×p95); otherwise 30 s. Receipt wall starts after watch reload, so measure queue/save-to-start separately and time out an unobserved save too. At explicit --timeout (default proposal 120 s for interactive follow; gate deadline configured separately), emit STATUS timeout and return unknown/nonzero. Stop waiting; inspect the owned process/receipt once. Do not kill or spawn a replacement automatically. A later completion may still be retrieved.

Heap: put explicit `-Xms64m -Xmx512m` in every test/watch/nREPL launch alias or its canonical launcher; size exceptional suites from a bounded full-suite witness. Already-running JVMs need an owner-scheduled restart to change this. Gene's two unbounded JVMs and 1.9 GB watcher RSS are supplied observations, not a fresh process census here. Do not launch another JVM to investigate them.

Add a cheap admission check before loading tests: require an explicit effective heap cap and compare Runtime.maxMemory with the configured ceiling; record requested/effective limits in the receipt. Refuse agent-mode admission when cap evidence is absent or exceeds policy, with STATUS refused/reason heap-cap and no green result. maxMemory is generally finite even without -Xmx; “finite” cannot prove explicit configuration. JVM ergonomics set defaults, and Xmx caps heap, not total RSS. See [Runtime](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Runtime.html) and [java options](https://docs.oracle.com/en/java/javase/21/docs/specs/man/java.html). This is defense against bypassed launchers, not a plugin that changes heap at runtime.

Curtaincall's `nrepl/test-alias` Makefile lines 82–103 have the right UX: persistent bounded image, one supported verdict entrance, separate human stream. Its stamp script recognizes human “tests, assertions” text and opens the next window immediately after a verdict, even while idle. Its reader has a valuable explicit load-error regression, but completion mtime can falsely bless an edit made during the run. Keep the entrance and 512 MB discipline; replace inference from text/stamps with receipt identity. Do not build another watcher.

RED prediction: output bytes should fall sharply; apparatus share falls only if caller actions disappear. Existing [before/after](/home/forge/src/kaocha-sublime/evidence/before-after.md) red medians (MVR 12.4645→0.0580 s; CCFP 127.6800→0.1585 s) belong to affected/follow-up selection, not this reporter. The [method](/home/forge/src/kaocha-sublime/evidence/method.md) records historical shared-host controls, ten samples (p95=max), and receipt-wall versus save-wall differences. Do not borrow those gains.

My testable prediction: remove 1–3 tail/grep/receipt-disambiguation actions per edit cycle; token marking can add one if not composed into the save entrance. Net unknown until observed. If A=12 apparatus actions among T=20, removing k=3 gives (A−k)/(T−k)=52.9%, down from 60%; fewer bytes with k=0 leaves share unchanged. Report absolute apparatus actions, unknown classifications, bytes/tokens, model returns, and edit-to-verified wall alongside the ratio.

Cheapest witness: replay frozen clean/fail/load-error/interrupted/stale-save transcripts through both renderings; a blinded fresh reader answers in-progress/mine/passed and locates the fix. Include a forged DONE in captured output and the large-map case. This proves clarity/encoding, not runtime speed. Then one bounded warm-image crossover on the same edit and gate records actual calls independently; no selection/heap changes between arms. A speed claim needs the repository's six-run control floor and replicated pairs, plus free-choice evidence before adoption claims.

Do not build a dashboard, progress percentages, an event query language, automatic retries, a custom diff engine, a second JVM, a second LATEST implementation, or configurable verbosity levels for agents. Preserve existing human reporters. Start with small supported value previews and honest unknowns; add detail only when a retained failure shows an avoidable receipt-opening action.

Proposed grammar and semantics (v1; notation only, no implementation):

```text
stream     := record LF ... ; UTF-8 transport, ASCII-only records, no ANSI/BOM/blank lines
record     := "RUN-START " J | "FAIL " J | "RUN-DONE " J | "STATUS " J
J          := one compact JSON object; keys below occur exactly once; key order irrelevant
common     := v:1, id:string, mode:(watch|probe|gate), candidate:(sha256|null)
RUN-START  := common + session:string, trigger:[string], trigger_omitted:uint,
              closure:(selected/total object|null), active:absolute-path
FAIL       := common + event:uint, kind:(fail|error|load), test:(string|null),
              file:(string|null), line:(positive-int|null), context:(string|null),
              expr:(string|null), expected:(string|null), actual:(string|null),
              diff:(string|null), truncated:[field-name], unavailable:[field-name]
RUN-DONE   := common + outcome:(pass|fail|error|incomplete), tests:(uint|null),
              assertions:(uint|null), pass:(uint|null), fail:(uint|null), error:(uint|null),
              pending:(uint|null), skipped:(uint|null), closure:(selected/total object|null),
              coverage:(complete|partial|unknown), wall_ms:uint, auto_followup:boolean,
              parent:(id|null), shown:uint, omitted:uint, receipt:absolute-path, sha256:hex64
STATUS     := v:1, state:(marked|waiting|running|late|timeout|killed|superseded|uncovered|
              unavailable|refused), token:(string|null), id:(string|null),
              identity:(exact|unknown), elapsed_ms:(uint|null), phase:(queued|load|run|unknown),
              reason:enum, active:(absolute-path|null), receipt:(absolute-path|null)
enum       := fixed registered ASCII symbol; initial reasons: none, save-unobserved,
              over-p95, deadline, process-dead, candidate-changed, scope-missing,
              no-receipt, identity-unproved, heap-cap, protocol-error
string     := JSON-escaped; every control/non-ASCII code point escaped, including ESC/DEL
              and bidi separators; quotes/backslashes escaped; no literal caller newlines
null       := unknown/unavailable, never zero, never the text "nil" as missing evidence
limits     := START/DONE/STATUS ≤8192 bytes incl LF; FAIL ≤2048; run FAIL budget ≤16384
              incl LF and ≤20 events; field preview caps apply after encoding incl quotes
              truncate only previews on complete escape boundaries; list their field names
              trigger ≤4 paths/512 bytes, with exact remainder count; full set in receipt
              ids ≤200 ASCII-safe chars; tokens opaque ≤200; candidate/digest = 64 hex
              identity/path fields never truncated; oversize => STATUS refused/protocol-error
counts     := tests counts executed leaves; pass/fail/error count assertion events;
              assertions=pass+fail+error; pending/skipped separate, never counted as pass
closure    := {"selected":uint,"total":uint}; namespace groups, not test-Var counts
order      := one START before load, zero or more FAIL, at most one committed DONE per id
              replay may repeat records: deduplicate by id/type, and id/event for FAIL
completion := DONE is an immutable run result, not a statement of freshness for every caller
              error outranks fail; neither failure nor missing/pending coverage becomes pass
accept     := follow-token exits 0 only for exact candidate + required coverage + pass;
              verified red exits 1; timeout/unknown/refused/incomplete exit 2
              terminal matched DONE is validated against the token by status/R12;
              direct log consumers must perform the same check before claiming “mine passed”
publication:= active reservation → START; final receipt rename → LATEST publication → DONE
              read atomic final receipt only; tmp is never completion; no DONE on kill/timeout
transport  := serialize records per destination; raw stdout/stderr cannot supply protocol data
compat     := PROBE/GATE adapters map to mode; no second summary; unknown version/key,
              malformed/oversize/conflicting record => protocol-error, never success
```

Build order and proposed owners; these are assignments for follow-on work, not work performed here:

1. **Opus — bounded launch admission.** Witness every supported watch/probe/nREPL entrance reports explicit effective cap; missing/oversized cap refuses before test load; run the full suite within the chosen ceiling and retain RSS separately.
2. **Me / Astra — finish the existing receipt/LATEST/status lane, then add candidate tokens and death identity.** Witness fast-save-before-mark, same-mtime edits, deletion, coalescing, supersession, reload failure, killed active run, and automatic follow-up; none can resurrect old green. No duplicate status implementation.
3. **Me / Astra — one bounded encoder and reporter projection, composed with receipt publication.** Witness clean two-line output, assertion/load errors, huge map, lazy/custom printer, forged newline/ANSI output, limits, and rename-before-DONE for CLI and warm REPL.
4. **Fable — PROBE/GATE adapters and R12/coldstart-grade acceptance.** Witness byte-identical schema parsing across entrances; stale candidate, mismatched receipt/digest, missing gate, partial focused green, and injected stdout all fail closed; accepted receipt survives lost terminal output.
5. **Fable — independent RED witness; Opus reviews ambiguity and failure fixes.** Begin with frozen-transcript naive readers, then the single warm crossover with unchanged checks and caps; retain wrong verdicts, extra receipt reads, and net actions. Expand only if the mechanism earns it.
