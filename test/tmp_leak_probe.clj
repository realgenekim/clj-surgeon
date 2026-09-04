;; RATCHET witness apparatus (2026-09-04, inb-9483a4, round two): the bb
;; entry point for clj-surgeon.tmp-leak-probe. `bb` re-execs by SCRIPT PATH,
;; so the bb arm of the witness needs a script, not a -main.
(require '[clj-surgeon.tmp-leak-probe :as probe])
(apply probe/-main *command-line-args*)
