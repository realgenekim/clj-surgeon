# Xray refusal repair

Pre-existing failure: red-tip-jvm.log and red-trunk-jvm.log each show
26 tests / 456 assertions / 3 failures / 0 errors. Their bb controls each
show 26 / 456 / 0 / 0. The throwaway trunk is a detached shared clone of
a15531ee under target/attempt22-trunk. No trunk source was edited.

The JVM SCI evaluator adds an ex-info with `:type :sci/error` above a
host exception. The host `:invalid-xray-path` or `:invalid-xray-analyzer`
is in the cause, not the outer ex-data. The evaluator's catch previously
examined only the outer exception. JVM SCI also reports forbidden `spit`
as `Could not resolve symbol: spit`; the existing text classifier knew
only `Unable to resolve symbol` and `is not allowed`.

4492f9df walks the cause chain, preserves the original typed expression
exception, translates the existing path/analyzer types to their established
xray reasons, and recognizes both SCI forbidden-symbol message spellings.
Untyped evaluation failures retain their original cause and expose its message.
The SCI allowlist is unchanged.

The added witness checks typed exception identity and data through direct and
wrapped evaluator failures, for both edit and xray expression types, and
checks an ordinary evaluation failure's cause/message. witness-red-jvm.log
records its pre-fix failures. green-xray-jvm.log and green-xray-bb.log each
pass 27 tests / 467 assertions / 0 failures / 0 errors.

An initial green attempt exposed the existing 1,024-character CLI receipt
bound: adding cause text to already-typed forbidden-symbol refusals produced
1,034 characters on both runtimes. That retained red is in green-first-*.
Cause text is now added only for `:evaluation-failed`, as requested; the bound
is unchanged. Serialized clj-kondo reports zero warnings and errors.

43a36e6b extends the witness through an actual SCI call to a host builder,
and checks a third typed refusal (`:invalid-xray-cardinality`). That added
matrix failed six assertions on each runtime before the correction, as recorded
in `real-evaluator-red-{jvm,bb}.log`. The evaluator now rethrows typed domain
exceptions generally, retaining the established path/analyzer reason adaptation;
only genuinely untyped evaluation errors receive `:evaluation-failed`.

Final controls in `final-evaluator-{jvm,bb}.log` each pass xray's 27 tests /
477 assertions and edit-DSL's 28 tests / 419 assertions. Those source bytes are
unchanged in the runtime-rule commit 14ae555f. The complete census was frozen
at 4492f9df; these later focused controls cover the evaluator follow-up, and the
later Make battery exercises the resulting xray namespace again on the JVM.
