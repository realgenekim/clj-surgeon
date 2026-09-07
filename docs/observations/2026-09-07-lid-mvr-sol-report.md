# Marvin JSON policy linked-intent report

Date: 2026-09-07  
Branch: `astra/json-policy-lid`  
Commit: `8d4975c` (`test: ratchet centralized JSON policy with linked intent`)  
Linked-intent rung: **b** (stable intent plus source-scanning and behavioral example witnesses)

Rung b is the highest rung that fits. This is a bounded source-policy and two concrete wrapper behaviors, not a state-space family (rung c), a relation across three or more entities (rung d), or a bad state that Clojure can make unrepresentable with a typed refusal (rung e).

## Files changed

- `docs/intent/registry.edn` — appended the active stable intent `JSON-POLICY-BOUNDARY-J01`.
- `src/marvin_voice_remote/json.clj` — linked both `parse` and `write` implementation sites to the intent.
- `test/marvin_voice_remote/json_policy_test.clj` — added the source-policy ratchet and independent behavioral examples, with test markers on both witnesses.
- `src/marvin_voice_remote/groq.clj` — removed the stale direct `clojure.data.json` require; added the missing `clojure.edn` require/alias exposed by lint.
- `src/marvin_voice_remote/capture_archive.clj` — removed the stale direct `clojure.data.json` require.

## Registry row

```clojure
{:id :JSON-POLICY-BOUNDARY-J01
 :status :active
 :ears "While code under src/ converts JSON, when it parses or serializes a value, the application shall route the operation through marvin-voice-remote.json."
 :misreadings
 ["Call clojure.data.json directly when its options happen to match the wrapper today — that restores multiple policy sites which can drift independently."
  "Let each parser caller choose its own key function — downstream code then receives string or keyword keys according to which boundary happened to parse the body."
  "Wrap serialization but discard caller options — callers that require literal slashes silently receive the library default instead."]
 :boundaries
 ["every .clj, .cljc, and .cljs file below src/ except marvin_voice_remote/json.clj, including newly added namespaces"
  "parse keywordizes keys at every nested object level"
  "write passes :escape-slash false through to the underlying serializer"]
 :tests [:all-application-json-uses-one-policy-boundary
         :json-policy-behavior-is-preserved]
 :rationale "The parse/write consolidation on 2026-09-07 migrated 9 reads and 21 writes but carried no linked intent or executable policy witness; two stale direct requires already remained. The source ratchet makes new bypasses red, while independent examples pin the wrapper semantics. Site: src/marvin_voice_remote/json.clj."}
```

## RED drill

The temporary plant in `groq.clj` deliberately split both the require and the call over lines. That proves the scanner does not depend on same-line formatting. The plant was removed immediately after this receipt.

Command:

```text
bin/kaocha --focus marvin-voice-remote.json-policy-test/all-application-json-uses-one-policy-boundary
```

Output (exit 1):

```text
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
GUARDRAILS IS ENABLED. RUNTIME PERFORMANCE WILL BE AFFECTED.
Mode: :runtime  config: {:throw? true}
Guardrails was enabled because the guardrails.enabled property is set to a (any) value.
[(F)]
Randomized with --seed 1243400535

FAIL in marvin-voice-remote.json-policy-test/all-application-json-uses-one-policy-boundary (json_policy_test.clj:51)
JSON policy bypasses found outside src/marvin_voice_remote/json.clj:
src/marvin_voice_remote/groq.clj:16: direct json/read-str or json/write-str call — (
src/marvin_voice_remote/groq.clj:5: direct clojure.data.json require — [clojure.data.json
expected: (empty? violations)
  actual: (not (empty? ["src/marvin_voice_remote/groq.clj:16: direct json/read-str or json/write-str call — (" "src/marvin_voice_remote/groq.clj:5: direct clojure.data.json require — [clojure.data.json"]))
1 tests, 1 assertions, 1 failures.
```

## GREEN witness

After removing the plant:

```text
$ bin/kaocha --focus marvin-voice-remote.json-policy-test
Picked up JAVA_TOOL_OPTIONS: -Djava.io.tmpdir=/var/tmp/forge
GUARDRAILS IS ENABLED. RUNTIME PERFORMANCE WILL BE AFFECTED.
Mode: :runtime  config: {:throw? true}
Guardrails was enabled because the guardrails.enabled property is set to a (any) value.
[(...)]
2 tests, 3 assertions, 0 failures.
```

The three assertions are: no source bypass, nested parse keys are keywords, and `write` preserves `:escape-slash false`.

## Repository gates (final snapshot)

Full suite tail:

```text
$ bin/kaocha
...
581 tests, 7845 assertions, 0 failures.
```

Changed-file lint tail:

```text
$ ~/bin/clj-kondo --lint docs/intent/registry.edn src/marvin_voice_remote/json.clj src/marvin_voice_remote/groq.clj src/marvin_voice_remote/capture_archive.clj test/marvin_voice_remote/json_policy_test.clj
linting took 52ms, errors: 0, warnings: 0
```

## Doubts and limits

- The ratchet intentionally recognizes Clojure source (`.clj`, `.cljc`, `.cljs`) and the named `clojure.data.json` boundary. It does not claim to ban another JSON library, reflective/dynamic Var resolution, or JSON implemented in non-Clojure source; those are outside this intent as written.
- The scanner requires call/require syntax, so prose mentions in comments or docstrings do not fail the gate. The red drill shows ordinary line-breaking cannot evade it.
- As LID itself cautions, traceability proves this promise is witnessed, not that the promise is intrinsically the right architecture. Here it agrees with the already-completed 9-read/21-write consolidation and Gene's explicit order.
