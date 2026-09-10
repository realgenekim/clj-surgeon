#!/usr/bin/env python3
"""E4, the CORRECT request: rename the `events` alias to `ev` and rewrite ONLY
the alias uses, leaving the 29 URL string literals alone.

The discriminating anchor is a lexical one invented for this task: an alias
reference is preceded by a form/whitespace boundary, never by `/` or a quote
and never inside a string. The spike's careful-python E2 arm used:

    re.subn(r'(?<![\\w/.\\-"])events/', 'ev/', s)

That lookbehind is the entire difference between this file and the broken one.
It is a hand-built lexer for Clojure symbol boundaries, written inside a task
that was not about writing a lexer.
"""
import re
import sys

path = sys.argv[1]
s = open(path).read()

# 1. the require binding — a unique exact anchor, no ambiguity
s, n_req = re.subn(re.escape('[cfp-scheduler-killer.events :as events]'),
                   '[cfp-scheduler-killer.events :as ev]', s)
assert n_req == 1, f"expected 1 require, found {n_req}"

# 2. the alias uses — the anchor that discriminates a symbol from a URL string
s, n_use = re.subn(r'(?<![\w/.\-"])events/', 'ev/', s)
assert n_use == 2, f"expected 2 alias uses, found {n_use}"

open(path, 'w').write(s)
print(f"rewrote {n_req} require + {n_use} alias uses")
