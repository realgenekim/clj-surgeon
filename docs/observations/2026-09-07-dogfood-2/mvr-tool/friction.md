# Friction ledger — dogfood3, clj-surgeon `alias_migration` on marvin-voice-remote
Worktree /home/forge/src/mvr-dogfood3 · branch fable/json-write-helper-tool · base 9f9cf61
Task: migrate 18 `clojure.data.json/write-str` call sites in 9 namespaces to `marvin-voice-remote.json/write`.
Outcome: 0 sites migrated by the tool. 2 refusals, both on a request that was correct. Migrated natively.

## F1 — `alias-migration-alias-policy-exhausted` fires when the alias is already bound TO THE TARGET NAMESPACE

Exact tool text (response-1.json):

  alias_migration
    refused · alias-migration-alias-policy-exhausted · 101.56 ms

  ✓ source unchanged
  → Every alias_policy entry is already bound in src/marvin_voice_remote/bridge3_new.clj
  facts · alias_policy=["mjson"] · collided_bindings=["mjson"] · file="src/marvin_voice_remote/bridge3_new.clj" · mutation_attempted=false · next_action="correct_request" · scanned_files=25 · source_unchanged=true · write_authority=false
  remedy · to.alias_policy is exhausted for src/marvin_voice_remote/bridge3_new.clj: every one of its 1 entries — ["mjson"] — is already bound to another namespace in that file's ns form. No next_call is composed, because any alias this verb could propose would be outside the policy you sent and your own request forbids it. Add an alias that file does not bind to to.alias_policy, or exclude src/marvin_voice_remote/bridge3_new.clj through scope.exclude, and resend.
  next_call · none — this refusal has no mechanically composable correction; the remedy above names what only the caller can decide

The fact is FALSE. bridge3_new.clj:53 reads:

  [marvin-voice-remote.json :as mjson]

`mjson` is bound to `marvin-voice-remote.json` — the exact `to.lib` of this request. This is not a
collision; it is the happy path. Six of the nine target files (bridge3_new, channel,
codex_app_server, director_control, director_outbox, reducer_session) already bind `mjson` to the
target namespace, from the previous `parse` migration on this same branch. So the verb refuses
precisely the incremental case it exists to serve: a second Var migrated into a helper namespace the
repo has already started adopting.

EXPECTED: the verb sees the alias already points at `to.lib`, reuses it, adds no require, and
rewrites the call sites in those six files; it only needs to consult `alias_policy` for the three
files (app_route, server, tts) that do not yet bind the target.
DID: retried once with `alias_policy: ["mjson","mvrjson"]` (see F2), then migrated natively.
SECONDS LOST: ~10 s on the call itself; the whole tool arm (apply-1 07:15:26 → apply-2 07:15:48)
cost 22 s and produced zero sites.
RATCHET: before the exhaustion check, resolve each candidate alias's current binding; if it already
resolves to `to.lib`, mark it REUSABLE and continue. `collided_bindings` must exclude aliases bound
to the target namespace, and the refusal message should never say "bound to another namespace"
without having compared that namespace to `to.lib`. Witness: a two-file fixture where file A binds
the alias to the target lib and file B binds nothing — must migrate both, adding a require only to B.

## F2 — `alias-migration-indirect-reference` on an ordinary top-level function call

Exact tool text (response-2.json), after widening alias_policy to ["mjson","mvrjson"]:

  alias_migration
    refused · alias-migration-indirect-reference · 649.71 ms

  ✓ source unchanged
  → An indirect or macro-mediated reference in src/marvin_voice_remote/tts.clj cannot be closed mechanically
  facts · file="src/marvin_voice_remote/tts.clj" · form="(defn synthesize-speech\n \"Convert text to speech using ElevenLabs API.\n Returns {:audio-bytes <byte-array> :content-type \\\"audio/mpeg\\\"\n … · mutation_attempted=false · next_action="correct_request" · reason="unsupported-binding-scope" · scanned_files=25 · source_unchanged=true · write_authority=false
  remedy · This migration does not model this binding scope. Review and migrate its bindings and uses explicitly; no scope-changing next_call is provided.
  next_call · none — this refusal has no mechanically composable correction; the remedy above names what only the caller can decide

The named site is tts.clj:121, and there is nothing indirect or macro-mediated about it:

  (let [started (System/nanoTime)
        resp (hc/post (str api-url "/" voice-id)
                      {:headers {...}
                       :body (json/write-str
                               {:text text
                                :model_id "eleven_turbo_v2_5"
                                :voice_settings {...}})
                       :as :byte-array
                       :throw-exceptions false})

It is a literal `(json/write-str <map>)` in value position inside a map literal inside a `let` inside
a `defn`. `reason="unsupported-binding-scope"` names a binding scope, but no binding of `json` or
`write-str` occurs here; the only bindings in scope are `started` and `resp`, neither related. The
`form` field also truncates to the *docstring* of the enclosing defn rather than naming a line, so
the receipt does not actually point at the offending expression — I had to grep to find it.

EXPECTED: an ordinary qualified call in value position is the base case; it should have been
rewritten to `(mjson/write {...})`.
DID: second refusal reached the protocol limit → migrated all 9 files natively with sed + three ns
require insertions.
SECONDS LOST: 650 ms on the call, ~12 s reading and disproving it; combined with F1 the tool arm cost
22 s of the 108 s total and returned nothing.
RATCHET: (a) the refusal must carry `file:line` (or `:col`) for the site it could not model, not the
enclosing defn's docstring — a receipt that cannot name its subject is unactionable; (b) add a
witness for a qualified call in value position inside nested map/let/defn and prove it migrates; if
the analyzer genuinely cannot model it, the class name must say which construct, because
"indirect or macro-mediated" is falsifiable here and was false.

## F3 — refusal is whole-run, not per-file; one unmodellable site voids nine files

Both refusals aborted the ENTIRE migration (`scanned_files=25`, `mutation_attempted=false`). Eight
files were, as far as anything in either receipt shows, perfectly migratable; one file's verdict
killed all of them. Under `expect:{files:9}` the caller has already declared the blast radius, so a
partial result is checkable.
EXPECTED: either migrate the 8 and refuse the 1 with a named skip list, or offer a
`scope.exclude`-composed `next_call` that drops the offending file. The remedy in F1 literally tells
me to add the file to `scope.exclude` — but for F2 no such next_call was composed either, so the
caller must hand-edit the request each round.
SECONDS LOST: this is the reason the second attempt was my last one — each round is all-or-nothing,
so the expected number of rounds to green was unbounded.
RATCHET: compose the `scope.exclude`-augmented `next_call` for every per-file refusal class; and
report `files_migratable` / `files_refused` so the caller can see whether one bad file is holding
back a whole change.

## Wins — what the receipts got RIGHT and what that saved

- **`source_unchanged=true` + `mutation_attempted=false` on both refusals, and it was TRUE.**
  `git diff` after apply-2 was empty; nothing partial had to be unwound before going native. This is
  the single most valuable field in the receipt — it converted a failed tool call into a zero-cost
  event and let me start the native migration without an audit.
- **The refusals were fast and cheap** (101 ms and 650 ms, server-reported). Failing in under a
  second is what made two rounds affordable inside the budget.
- **F1's `collided_bindings` named the exact alias and the exact file.** The FACT was wrong, but the
  LOCATOR was right — it took one `grep -n mjson` to disprove it. Compare F2, whose locator was a
  truncated docstring and cost several times more to run down. Locator quality is the difference.
- **`next_call · none — this refusal has no mechanically composable correction`** is honest and I
  want to keep it: it told me not to wait for the tool to fix itself, which is why I fell back
  quickly rather than iterating on request shapes.
- **`scanned_files=25` in both** told me the scope resolved as intended (`src`, minus the exclude);
  I never had to doubt that the tool was looking at the right tree.
- **The `scope.exclude` argument was accepted and honored** — json.clj was never rewritten into a
  self-call. The DEFECT I was told to watch for did not occur (though nothing was written at all,
  so this is only weak evidence).
