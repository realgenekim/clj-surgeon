# A Wrong In-Range Index Ended the Composition Hill

Date: 2026-08-29

Lane: SURGEON2, deterministic adversarial probe

Status: `file_index` permanently NO-GO for public mutation authority

## Question

A pure request-compression oracle found that `file_index` plus
`replacement_groups` reduced a frozen request by 23.67%. The safety falsifier
asked whether an in-range but wrong index could silently select another real
file.

## Fixture and result

Two files contained the same named owner and guarded source:

```text
src/intended.clj  (defn shared [] :old)
src/wrong.clj     (defn shared [] :old)
```

The file table was `["src/intended.clj" "src/wrong.clj"]`. Supplying index 1
instead of intended index 0 lowered to this canonical edit:

```clojure
{:file "src/wrong.clj"
 :within {:form "shared"}
 :from ":old"
 :to ":new"
 :matches 1}
```

The canonical kernel returned `:ok true`, `:committed true`, and
`:verification_complete true`. The intended file remained byte-identical and
the wrong file changed to `:new`.

The original complete probe is immutable in commit
`52ed39ccb5ed6166979bb9f29d5941946bb89f2d`.

## Why guards cannot repair the mapping

After the numeric index becomes a path, the request no longer contains the
intended path. The selected wrong file is real; its owner and source satisfy
all ordinary guards. Snapshot evidence proves what changed, not what the
caller meant to index.

Therefore a passing model cohort could never prove the index representation
safe. It would only prove that the model happened to emit correct indexes in
those runs. Positional input does not gain mutation authority; explicit file
and owner identity remains mandatory. Positional values may still be emitted
as diagnostics or derived evidence.
