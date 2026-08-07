This is a read-only Clojure structural inspection task. Do not modify any file.

Return the following exact facts in the required JSON schema:

1. For these ordered forms in `bench/summarize_clean_codex.clj`, return each
   name, starting line, and ending line, plus their combined exact-source
   character count: `numeric-fields`, `boolean-fields`, `summarize-group`,
   `markdown`, `self-test`.
2. For these ordered forms in `bench/rescore_clean_codex.clj`, return each name,
   starting line, and ending line, plus their combined exact-source character
   count: `rescore-row`, `emit-table`.
3. Outline the large `src/clj_surgeon/show_form.clj` namespace and return its
   physical line count, top-level definition count excluding the `ns`
   declaration, first named definition, and last named definition.
4. Structurally match `(send! _)` in
   `bench/fixtures/read_portfolio/match_decoys.clj`. Return the exact match
   count, ordered enclosing owner names, and ordered exact matching sources.
   Comments and strings contain textual decoys and must not count.
5. Use a computed structural X-ray over the `numeric-fields` initializer in
   `bench/summarize_clean_codex.clj` and return the number of fields.

Correctness is a gate. Preserve request order. Do not guess from filenames.
