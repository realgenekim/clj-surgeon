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

Finding to close before any port: the frozen Cell C tree holds only four src files. The probe reports
62 sites in server.clj and missing-root=test; the receipt records 87 sites across five callers.
The inferred 25-site gap and prior per-file breakdown (polish_test 16, forms_test 4, comms_test 2,
views_test 3) are unverified: the missing callers and equivalent counting rules are not asserted.
Freeze the test callers from curtaincall-cfp d9205abc before reconciling those totals.

Round 3: scan :or defaults in enclosing let/loop/fn/defn scope, including nested parameter destructuring;
resolve :rename local names to source owners, with shadowing applied before resolution. The regression
matrix crosses explicit/:all refers, renamed/original names, nested defaults, arities and shadowing.
Tests anchor script and fixtures to their classpath resource, independent of CWD and *file* spelling.
The 62-site count remains a bounded syntactic observation: for/doseq/letfn/if-let/when-let/binding/catch
and defmethod/deftype/defrecord method scopes are unsupported and may over-count shadowed references.
Broader scope analysis and a larger layout budget are deferred beyond this final 120-line slice.
