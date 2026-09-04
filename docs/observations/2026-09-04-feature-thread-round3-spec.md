# feature_thread round three — spec (drafted from the naive-reader probes; starts when round two lands)

Goal unchanged: ONE call is the complete edit basis. Round two closes the implementation leg, governance anchors, co-primary tests, verify row, budget/elision, the assert wording and the header. Round three closes the three gaps both naive readers named:

1. **Co-menu-item siblings (peer commands).** For a `:use` leg whose boundary is a menu form, every other command bound in the same enclosing form (`menu-item … {:onclick "X()"}` → identifier X) is a PEER. Return `peers`: for each peer, its JS definition range (kind :def search, lexed brace window), sha256, anchor, and body under budget (elide first, after sibling). Evidence label `co-menu-item`. On the fixture: openTransformFromSelection (L332–…), expound, bulletize are peers of formatDraft; the real agent read them at four line ranges. Rule: peers whose definition is ABSENT are named as absent with the search that was run.

2. **request_contract row.** In `rules`: `:request_contract {:route "/api/transform/format" :handler_reads [keys the handler destructures or `get`s from the parsed body/params — from the handler form: `{:keys […]}`, `(:k body)`, `(get body "k")`] :js_posts [keys in the object literal passed to postJSON/fetch in the js-function body] :agree? bool}`. Cheap relational extraction; no JS parser — the lexed brace window already isolates the function body. On the fixture: handler reads `sync` (and after the edit `selection`); JS posts `{sync}`.

3. **Negative evidence.** `absent`: for the subject, every `also` seed, and every peer, the identifiers that resolved to NO definition and NO occurrence outside comments/strings, each with the search run. Plus an optional `probe` param: extra identifiers the caller wants ruled in or out (e.g. "dequote") reported the same way. A reader must never have to run `rg -i dequote` to learn there is nothing.

4. **Classic-script statement.** When the JS leg's file has no `export`/`module.exports`/`window.X =` for the subject, say `export: none (classic script; functions are globals)` so the reader does not search for a registration site.

Witnesses fail first; expected values are the fixture facts above; the naive-reader probe is re-run on the round-three receipt and the target is: ZERO calls before the patch other than the write itself.
