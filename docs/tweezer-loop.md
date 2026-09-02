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

## Promotion ladder (any deterministic contract failure returns to step 1)

| step | what | gate | minutes |
|---|---|---|---|
| 1 contract smoke | hand-drive the smallest representative case | zero silent ignores, invalid output, false refusals, unexplained residue | 5–10 |
| 2 full hand-drive | the real task incl. callers and cleanup, watcher on | returns and wall at or below native's for the same task, by the stopwatch | 10–20 |
| 3 cold-agent shadow | normal prompt, no prescribed sequence | selects the tool within 3 returns, valid first call within 5 | 5 |
| 4 single cold completion | n=1 agent completes unassisted | accepted diff, no fallback, returns below native's | 10–15 |
| 5 paired pilot | 2 tool arms + 2 native arms on Anvil | no acceptance regression, credible return/wall advantage | 20–30 |
| 6 battery | the claim, at n ≥ 6 per arm, predictions pre-registered | the pre-registered predicates | 15 + scoring |

Cancel a build's battery, not necessarily the build, if it fails twice on the same deterministic
contract or cannot beat native in the single cold completion.

## Standing decisions (2026-09-02)

- rf2 (`:extract!` with `:rewire-callers`) and q5z (`alias_migration`): finish the builds,
  hand-drive both at steps 1–2, cold shadow at 3, before any pilot or battery. No battery as
  planned.
- The gate cohorts already queued (rs1, z6, z7, z8) run: they need no attention and measure the
  one shape that has already won.
- Every tweezer session's records and commentary are committed under `docs/observations/` as a
  dated receipt; the captain's log carries the headline.
