#!/usr/bin/env python3
"""E4, the MISTAKEN request, exactly as the spike records Astra's program:
rename the `events` alias to `ev` by replacing the textual prefix.

  intent: "change the require alias of cfp-scheduler-killer.events from
           events to ev, and rewrite its uses"
  program: s.replace('events/', 'ev/')  + the require line

Includes the guard style the census found on 54% of program edits: an assert
on the number of hits. The guard passes. That is the point.
"""
import re
import sys

path = sys.argv[1]
s = open(path).read()

n = s.count('events/')
assert n == 31, f"expected 31 alias uses, found {n}"   # the guard: a count of STRINGS

s = s.replace('[cfp-scheduler-killer.events :as events]',
              '[cfp-scheduler-killer.events :as ev]')
s = s.replace('events/', 'ev/')

open(path, 'w').write(s)
print(f"rewrote {n} occurrences + 1 require line")
