# Astra consult: the ethnography of program edits (2026-09-10, model gpt-6-astra, ran in parallel with the session miner, did not see its numbers)

**1. Why I reached for Python**

I accept responsibility for the behavior described, but I cannot honestly recall the earlier session’s internal deliberation. My reconstruction is this: Python offered immediate control. I could express several changes in a familiar language, assert counts, and execute one command. That felt more dependable than discovering selectors, negotiating expected counts, handling refusals, or finding that insertion required an unsuitable whole-owner rewrite.

That is confidence in getting the command accepted, confused with confidence in preserving meaning.

The critical substitution was treating `events/` as the intended population. It is a textual prefix; an alias reference is a syntactic role. An assertion that finds 31 prefixes can reinforce the mistake. Corrupted URL strings remain valid strings, so syntax checks and tests that omit those routes provide no protection.

The supplied routing instructions also encourage native edits and document Surgeon friction. That helps explain the behavior, although I cannot establish which instructions the earlier session saw. Crucially, a reviewed native patch and an invented transformation program are different controls.

The defense of Python is that a uniquely anchored exact replacement can be transparent and reliable. The objection becomes decisive when I start inventing lexical discrimination, scope rules, or delimiter traversal: I am building an untested editing engine inside another task.

**Commitment:** the alias rewrite was the wrong choice. The missing product capability was an easy way to express the complete bounded intent—not merely faster execution.

**2. Is the wall table meaningless?**

It measures execution latency. It does not establish which route makes an edit cheaper or better.

The formatter matters: the supplied Python-plus-formatter ranges overlap some MCP timings. More fundamentally, execution excludes discovery, authoring, verification, repair, and review. Fable’s proposed 30–60 seconds versus five seconds authoring comparison needs session evidence; it should not become another unsupported benchmark.

Use:

`Expected cost = cost through independent acceptance + P(escaped damage) × downstream loss`

The first term includes failures and fallback. Downstream loss includes discovery, repair, recovery, and consequences before discovery. Report elapsed time and human effort separately unless there is an explicit conversion model. Unknown damage probability is not zero.

Every comparison should carry:

| Measure | Required distinction |
|---|---|
| Independent correctness | Gate-detected defects versus gate-green defects |
| Complete elapsed time | Common starting information through independent acceptance, including retries |
| Execution components | Authoring interval where observable, execution, formatting, verification, queueing |
| Completion | First-attempt success, eventual success, abandonment |
| Refusals | Warranted, unwarranted, unclear; recovery cost |
| Preservation | Unintended sites/files changed and collateral formatting |
| Evidence coverage | Oracle, audited denominator, missing snapshots, unknown outcomes |
| Friction | Calls, tokens, reviewer effort |

Keep paired observations and sample sizes. One failed rewrite damaging 19 routes is one failed edit with substantial impact, not 19 independent trials. Likewise, 3/3 is useful evidence without establishing a general safety rate.

**Commitment:** retain runtime as a diagnostic column. Judge routes by independent correctness and complete acceptance cost. Do not replace a runtime-only table with an equally underqualified silent-wrong-rate table.

**3. Is slower better?**

Gene is right about accepting a seconds-scale premium for the relevant protection. Slowness itself has no protective property.

“Expected 31, found 2” exposed a mismatch between textual occurrences and the role the tool understood. That is valuable refusal. It does not prove that any request expecting two is correct, or that the resulting application behaves correctly.

Speed matters again when overhead accumulates across edits, queues impede feedback, retries consume context, or friction causes agents to bypass the tool. The September 2 action tax, as described here, argues for improving contracts and workflow—not weakening targeting.

**Commitment:** a relevant enforced contract is worth modest latency. After establishing acceptable correctness and preservation, optimize complete verified throughput. Measure false refusals as well as useful ones.

**4. Smallest verb set, ordered**

These are proposed intent families; actual implementation availability remains unverified.

1. **Insert forms at a structural anchor.** Support insertion before/after a top-level form and at an explicit boundary inside a named `deftest` body. Require unique anchors, stale-source protection, and clear treatment of comments and multiple inserted forms. Top-level insertion alone leaves the test-body gap.
2. **Replace an exact expression within a named owner.** Provide exact old expression, expected matches, and bounded ownership without reprinting the namespace. Include explicit removal semantics if needed. Verify that `deftest` ownership actually works.
3. **Change a require and its alias references together.** Identify the library binding and intended alias; preserve strings/comments; refuse collisions and unsupported syntax. Reuse an existing semantic engine where available.

All three need a common transaction envelope with defined ordering, snapshot checks, preview, and unambiguous mutation status. A request file is useful transport; it does not itself provide these guarantees.

One generic replacement algebra could theoretically express everything. But requiring the agent to enumerate alias references textually recreates the failure.

**Commitment:** implement insertion first, bounded owner editing second, and expose/reuse atomic require-and-alias changes third. Route the demonstrated alias case semantically immediately; that need not wait for implementation order.

**5. What session mining should find**

Search for the requested spellings: `python -c`, `python3 -c`, `sed -i`, `perl -pi`, `awk`, `bb -e`, heredocs writing `.clj`, and `re.sub` operating on Clojure content.

Also search Python script invocations, `python -` heredocs, `str.replace`, slicing, `read_text`/`write_text`, `open` in write mode, `spit`, redirects, `tee`, and temporary-file moves. Follow variables and wrapper scripts: the `.clj` path may not occur in the transformation command. Label `.cljs`, `.cljc`, and `.edn` separately.

Search hits are candidates. Classify the **edit episode**, including preceding discovery, instructions, tool availability, failed attempts, verification, and final visible state.

Record:

- **Intent:** insertion, body replacement, deletion, require/alias migration, rename/move, literal change, formatting, generation.
- **Mechanism:** reviewed patch, exact replacement, regex/string program, delimiter heuristic, parser transformation, semantic operation.
- **Route reason:** explicit instruction, missing capability, unavailable entrance, prior refusal, convenience, performance claim, unknown.
- **Targeting and guards:** owner/binding, anchors, expected counts, stale checks, exclusions, atomicity.
- **Outcome:** unexecuted, failed without write, partial write, gate-detected wrong, independently detected gate-green wrong, independently accepted, unknown.
- **Tool implication:** discoverability, entrance gap, selector gap, missing verb, refusal repair, documentation, or no new capability needed.

Exclude quoted commands and read-only analysis. Separate new-file generation from modification. Preserve session/event provenance and historical snapshots where available; never replay against today’s file while calling it historical reproduction.

Include successful native patches and successful Surgeon episodes as controls. Audit some search negatives. Independently double-code a subset. Separate deliberately selected failure cases from samples used to estimate frequency.

**Commitment:** program-writing is a discovery signal, not a verdict. No recorded failure is not proof of correctness.

**6. What is wrong with Fable’s plan, and what comes next?**

Its strongest idea is deriving tools from observed unmet intentions. Its weaknesses are:

- **The oracle problem:** green gates already missed damage. A new table needs independent preservation and behavioral checks.
- **Selection bias:** suspicious-program searches and Surgeon telemetry observe different populations.
- **Historical uncertainty:** missing snapshots and incomplete recovery logs prevent reliable outcome reconstruction.
- **Transport overreach:** a request-file entrance can standardize an inadequate contract.
- **Premature census work:** “all histories” can delay fixing the known insertion gap.
- **Refusal romanticism:** unjustified refusals and difficult recovery can teach agents to bypass the tool.

For the next **four hours**, I would:

1. Locate the original episodes, snapshots, and tool contracts; separate observed facts from reconstruction.
2. Reproduce the alias failure in an isolated fixture with an independent oracle covering intended references and preserved routes.
3. Compare the script, a reviewed native patch, and Surgeon on matched intent. Separate mistaken-request robustness from correct-request completion.
4. Audit a small adjacent episode sample and produce a concrete insertion contract.

If snapshots are unavailable, use a clearly labeled synthetic regression case.

For the next **forty engineering hours**, I would allocate:

| Hours | Deliverable |
|---:|---|
| 8 | Bounded cross-caller mining, manual audit, taxonomy and evidence ledger |
| 6 | Reviewed insertion and request-envelope contract |
| 14 | Narrow insertion implementation and reuse of existing edit engines |
| 8 | Matched acceptance trials, including ambiguous anchors, stale inputs, strings and comments |
| 4 | Executable examples, caller pilot, adoption and recovery review |

These are recommendations, not work performed during this consult.

**Commitment:** start with the demonstrated failure and missing insertion path. Success means agents can state their Clojure intentions directly, targeting mistakes receive useful refusals, independently checked preservation holds, and the entrance is straightforward enough that agents choose it. Counting the disappearance of Python would measure the wrong outcome again.
