# Refusals: what they are, why they exist, and the native-parity rule

Written 2026-09-11 after Gene: "At times seems totally overboard and hysterical and alien to me, because I never see the risk while doing native." and "If our goal is to do no worse than native, why are we worrying about all these hundreds (thousands) of cases. Are we building general case clj parser???"

## 1. What a refusal is, in plain language

Native editing (sed, python, the Edit tool) treats a Clojure file as text. Surgeon reads it as forms (code shapes), through rewrite-clj, an existing library. We are not building a parser. A refusal is Surgeon declining to write because what it read as forms does not match what the caller said: the count is wrong, the file moved, the result would not be a form, the target is not unique.

## 2. The storyboard

```
PANEL 1 — native, from the caller's seat

   "rename the alias events -> ev"
        |
   sed: s/events\//ev\//g
        |
   tests GREEN   lint CLEAN   diff plausible
        |
   ship it.                       (95+ times in 100 this is the end)

PANEL 2 — the one time it goes wrong, and why nobody sees it

   31 places said "events/":
       2 were the alias       events/day-hours      <- the code
      29 were URL strings     "/events/123/edit"    <- routes
   sed changed all 31.
   tests GREEN (no test hits those routes)
   lint CLEAN  (a broken URL is a valid string)
   diff 32 lines, plausible
   -> 404s appear later with no trail back to this edit.
   (E4, real file, 2026-09-10: 2026-09-10-e4-matched-arms.md)

PANEL 3 — the four refusals that stop Panel 2

   1. "expected 31, found 2"        COUNT MISMATCH   -> fix one number
   2. "source hash mismatch"        STALE FILE       -> re-read, retry
   3. "candidate structure mismatch" BROKEN FORM     -> nothing written
   4. "anchor ambiguous / absent"    WRONG TARGET     -> say which one

PANEL 4 — the price, measured on the real file

   sed route:             0.02 s  + 29 broken routes, gates green
   Surgeon, wrong count:  refused; 1.2 s to fix one field; landed
   Surgeon, right count:  8.5 s start to verified; byte-identical

PANEL 5 — where Gene is right

   a) In fresh short tasks the risk almost never appears: the 120-run
      cohort (2026-09-11-cohort-report-v2.md) had ZERO silent breakages
      in any arm. The seatbelt did nothing because nobody crashed.
   b) A refusal that is wrong, or that the caller cannot act on,
      teaches the caller to go around the tool. The one recorded
      rationale in 6,124 program edits is exactly that (Fable, 09-02).
```

## 3. Where it became overboard

Native promises nothing, so it needs no witnesses. A tool that promises "I did not touch the other forms" needs a witness that can fail. That is legitimate, and small: E4 needs the four refusals in Panel 3.

What the two verbs refuse today, beyond those four: mixed line endings; a byte-order mark; a hard link with two names; a symlink; a `.cljc` file or a reader conditional; a request over 2 MiB; more than 100,000 lexical units; a NUL in a path; a payload that is only a `#_` discard; comment-only payloads; and a body of "structured error sentence" ceremony. Native edits every one of those and gets them right almost every time. On those inputs Surgeon is worse than native by construction: it declines work native does. The `.cljc` refusal excludes a whole file class.

How it happened: the review ladder. Each Opus red-team round found an edge; each edge became a refusal plus a witness; nobody asked whether native would have gotten it wrong. The builders' own "least sure" lists say the same in politer words (2026-09-10-insert-forms-build-astra.md, 2026-09-11-rename-alias-fix1-astra.md).

## 4. The rule

**A refusal is justified only if the same edit done natively would have produced a wrong result or a silent hazard. Everything else does what native does: write the file, and at most note it in the receipt.**

Corollary for reviews: for each proposed refusal, name the native failure it prevents, or it is not a refusal. This question goes into every red-team brief and every contract template.

## 5. The sort, applied to insert_forms and rename_alias as of trunk 04648059

| keep (native gets it wrong silently) | demote to native behaviour (write; note in receipt) |
|---|---|
| count of forms does not match the declared count | mixed CRLF/LF; BOM |
| source hash mismatch (file changed since read) | symlink components; hard-link count > 1 |
| candidate structure mismatch (the write would break a form) | `.cljc` / reader conditionals (edit them; the reader handles `#?`) |
| anchor ambiguous or absent | request/source/payload size and depth ceilings, except as a memory guard with a large bound |
| candidate/source parse error (cannot read the file as forms at all) | discard-only or comment-only payload (write it) |
| | NUL in path (an OS error, not a verb decision) |
| | the structured-error-sentence ceremony beyond "state first, error type, remedy" |

Keeping the first column keeps the four promises and the witnesses that prove them (including the receipt-boolean seams). Demoting the second column removes most of the witness surface the two builds accumulated.

## 6. What "no worse than native" means, stated once

Never write wrong bytes silently (the one thing native does silently); refuse only where native would have been wrong; on every other input behave as native does. Not: correct on every input a reviewer can imagine.

## 7. Next step, when Gene says go

An audit round on both verbs with Astra: demote the second column, delete or convert their witnesses, keep the four promises and the boolean seams, re-run the E4 arm and the fast suite, ship. Smaller than anything built on 2026-09-10. This document should also reach the code repository's docs/intent as the refusal policy for any third verb, via the normal ship lane (trunk is code only; this copy is the record).
