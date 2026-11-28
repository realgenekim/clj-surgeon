Read-only Cell C probe: partition, destination, static site and dependency edge report; no mutation or commit.
Owners: def, defn, defn-, defmulti, defmethod, defmacro, defonce, defrecord, defprotocol and deftype.
Report excluded top-level lists as non-owners; print owners, distinct names and one mapping row per name.
Count explicit/:all refers and aliases; resolve renamed locals after shadowing; retain repeated occurrences.
One scoped scanner supplies both sites and edges. Expand destructuring with Clojure's destructure, then
walk binding initializers before adding each local to scope: map :as first, keys in order, vectors positionally.
Defaults see outer bindings and earlier locals, never their own or later locals; recurse through nested patterns.
Fourth-read regressions: own-key default=1, :as shadow=0, later vector local=1, earlier vector local=0.
Cross explicit/:all refers and renames; cover :keys/:syms order, nested maps, arities, quotes and discards.
Assert the :as case has no f -> leaf edge; retain partition, alias, repeated-site and frozen-fixture checks.
Tests locate the script and fixtures through a classpath resource; namespace loading never exits.
Frozen Cell C: owners=141 distinct=141 mapped=141 unmapped=0 double=0 destinations=20 absent=20;
sites=62 callers=1 edges=226. Four src files only; missing-root=test. Receipt says 87 sites in five callers.
The inferred 25-site gap is unverified; freeze test callers from curtaincall-cfp d9205abc before reconciliation.
Scope: .clj and let/loop/fn/defn only; other binding macros and method scopes may over-count references.
Reader conditionals/.cljc and broader analysis are deferred. Script+test <=120 lines; test >= script; plan <=20.
Gates: self-test, suite-path require/run, fixture footer, kondo, line lengths/counts and diff; leave uncommitted.
