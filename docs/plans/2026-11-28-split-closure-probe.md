Read-only Cell C probe: implement the user's partition, destination, site and edge report using main's analyzer.
Owners are def, defn, defn-, defmulti, defmethod, defmacro, defonce, defrecord, defprotocol and deftype only.
Report excluded top-level list forms (including ns, declare, comment and bare calls) as non-owners=N.
Test literal mappings, lexical shadowing, repeated sites and frozen counts; ordinary test namespace loads without exit.
Gate: self-test (positive test count, zero fail/error), suite-path require/run, fixture, lint, line budget; no commit.
Round 2: count explicit :refer and :refer :all occurrences, excluding lexical let/loop/fn/defn bindings.
Keep repeated references; test initializers, nested scopes, destructuring, and single/multiple arities.
Print definition count as owners=N, unique names as distinct=N, and one mapping row per unique name.
Skip reader-discard forms. Discovery remains .clj only; reader conditionals and .cljc are outside this
120-line slice. Run --self-test from the repository root (bb.edn supplies the src:test classpath).

Finding to close before any port: the frozen Cell C tree holds only four src files. The 25 recorded test-caller
sites (polish_test 16, forms_test 4, comms_test 2, views_test 3) cannot be reproduced until those test/ callers
are frozen from curtaincall-cfp commit d9205abc. The probe prints missing-root=test for this reason; current
fixture evidence covers 62 sites in server.clj, not the receipt's 87 sites across five callers.
