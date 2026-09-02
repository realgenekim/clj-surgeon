# The tweezer loop — discover tool shapes by hand, at the meter, before any battery

Gene, 2026-09-02: *"to discover the novel forms of tools we need, running on anvil (multi-armed)
seems ridiculous. That is wood chipper and chainsaw work. we need tweezer work, nearby, fastest
feedback, in REPL."* And, ratifying the meter: *"the job of the live writer observer is to provide
at the meter measurements, to ensure that 'feels good' is true, but also that 'was actually
faster' -- ask the runner, but also look at stopwatch."*

Anvil verifies discoveries; it does not manufacture them. rf1 spent six arms, a scorer and an
ethnographer to learn what ten minutes of hand-driving `:extract!` would have shown. The
ethnography was tweezer work done post hoc at hours of latency. But "feels good" is what promoted
the winners list that lost 1.4–1.75× when measured: a human at a REPL absorbs the returns that
cost an agent its wall. So the loop has a meter, and a cold agent, before it has a battery.

## Roles

**Driver.** The chief of staff (or Sol) at the REPL/CLI/MCP on a real repo, doing a real task
(first: the rf1 extraction, then the caller rewiring). Before every tool call the driver states,
in the transcript, the expectation: what the call should do, what the receipt should say, how
many returns remain. The driver may not repair the tool's output silently: every hand repair is
an event.

**Watcher (the meter).** A Sonnet-class agent reading the driver's session transcript (every tool
call with its timestamp; the stopwatch is the transcript). It writes one record per call and a
running commentary. Fields per call:

```clojure
{:n 7 :t-start "…Z" :elapsed-ms 4120 :cum-wall-s 212 :returns-so-far 7
 :tool "bb :op :extract!" :intent "move nine forms" :expected "new ns, callers named"
 :actual "new ns; source docstring copied; callers named, not rewired"
 :deviation #{:semantic :scope :receipt}   ; or #{} ; classes: schema semantic scope receipt refusal cleanup
 :hand-repair? true :agent-visible? false   ; could a cold agent have known this from the tool's own text?
 :native-equivalent "one apply_patch Add File"  :returns-vs-native +1}
```

The watcher must NOT: suggest the next call, repair arguments, interpret results privately for
the driver, edit files, or declare that an agent would adopt the tool. Otherwise it is an
unmetered copilot. Its running totals are the stopwatch: returns and wall so far against the
native benchmark for the same task (rf1 native: 9–10 returns to the move, 20–24 total, 311–342 s).

**Shape spec.** The watcher's records compile into: trigger conditions in repository-visible
terms; one canonical invocation; preconditions and atomic postconditions; exact changed/unchanged
scope; a bounded refusal taxonomy, each refusal with its complete remedy; receipt fields
sufficient to avoid re-reading; one recovery path; a maximum return budget. **A shape is not ready
while the watcher's explanation contains knowledge absent from the tool's own description or
response.**

## What the driver cannot see, and the instrument that restores it

The hand driver is the most context-rich operator possible and begins with the winning verb.
Field agents must notice the tool, guess its schema, learn from a refusal in one fact, trust the
receipt, and not abandon it. Free-choice adoption of a working faster gate was 0 of 8 today;
distrust after an `ok` receipt fired in 4 of 4. No hand session shows that.

**Cold-agent shadow (5 minutes, 3–6 returns).** One agent gets only the normal task, the repo,
and the production tool descriptions. It must narrate its next intended action before each call.
Stop after tool choice plus the first successful mutation, or at five minutes. It tests the
acquisition funnel; the driver tests the mechanics. Pair them.

## Promotion ladder (merged from Sol and Opus, 2026-09-02; any deterministic contract failure returns to G1)

| gate | test | pass | minutes |
|---|---|---|---|
| G0 arithmetic | returns budget on paper vs native's for the same task | tool returns ≤ native − 3, no O(N) agent-computed payload | 5 |
| G1 hand-drive | the real task with the watcher on; one recorded invocation per verb pasted into the pre-registration | does what its docstring says; nothing silently ignored; returns and wall at or below native by the stopwatch | 15–20 |
| G2 naive-reader | after each call, a fresh cheap model gets ONLY the tool's output bytes and is asked "what is your next call?" | ≥ 80 % determinable (a refusal with an empty diagnostic scores zero) | 2 |
| G3 shape spec | the watcher's close, written once by Opus | reviewable; every refusal carries next_call; receipt makes a re-read unnecessary | 10 |
| G4 replay arm n=1 | one agent runs the recorded sequence | zero fallback to `apply_patch` on functional bytes | 15 |
| G5 cold shadow / free choice n=1 | tool present, not mandated; the agent narrates intent before each call; stop after tool choice + first mutation | chooses the tool within 3 returns, valid first call within 5 | 5 |
| G6 battery | n ≥ 6 paired, observables pre-registered | the claim | 15 + ~90 scoring |

Pre-battery total ≈ 52 min against a battery's ≈ 105 min plus a misdirected day. G5 is
non-negotiable: a verb nobody picks cannot win in the field however fast it is.

**Watcher amendments (Opus):** event-driven per call, never clocked (a clocked narrator
manufactures relay hops); six fields only per call (`#`, intent, expected vs actual, deviation
class, **return-tax**: would an agent pay a model return here, **context-privilege**: did the
driver use knowledge the tool did not supply); hard 60-minute cap plus an idle stop, both
self-firing (the commentary skill's four runaway scars); Opus once, at the close, for the shape
spec only. **Correction to the chief of staff's critique (Opus):** the rf1 ethnography was NOT
tweezer work done late; it read agent rollouts, and its findings are agent behaviour a hand
session never emits. Hand-drive replaces the missing smoke test, not the ethnography.

## Standing decisions (2026-09-02)

- rf2 (`:extract!` with `:rewire-callers`): finish the build, G0–G5 by hand (≈ 50 min), then its
  n=3 kill-or-promote cohort with the pre-registered readout. q5z (`alias_migration`): finish,
  hand-drive at N=5 only (G1–G2, 10 min), then the slope runs as designed: its readout IS the
  battery (n=1 per N) and its acceptance already carries the receipt rf1 lacked.
- The gate cohorts already queued (rs1, z6, z7, z8) run: they need no attention and measure the
  one shape that has already won.
- Every tweezer session's records and commentary are committed under `docs/observations/` as a
  dated receipt; the captain's log carries the headline.
