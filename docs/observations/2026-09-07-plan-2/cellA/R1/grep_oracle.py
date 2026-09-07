#!/usr/bin/env python3
import re, os, sys, glob
ROOT="/home/forge/src/cc-split-R1"
plan=open("/var/tmp/forge/plan2/split/plan.md").read()
secs=re.split(r"(?m)^## ", plan)[1:]
expected={}  # name -> file
for s in secs:
    hdr=s.splitlines()[0]
    m=re.match(r"cfp-scheduler-killer\.views\.(\S+)\s+\(src/(\S+)\)", hdr)
    if not m: continue
    f=m.group(2)
    for line in s.splitlines()[1:]:
        mm=re.match(r"^- (.+)$", line)
        if mm and mm.group(1)!="^:private":
            expected[mm.group(1)]=f
files=sorted(glob.glob(ROOT+"/src/cfp_scheduler_killer/views/*.clj"))
defs={}  # name -> [relpaths]
for p in files:
    rel=os.path.relpath(p, ROOT+"/src")
    for line in open(p):
        m=re.match(r"^\(def\S*\s+(?:\^:private\s+)?([a-zA-Z0-9!?*<>=+/.$_-]+)", line)
        if m: defs.setdefault(m.group(1), []).append(rel)
mismatch=[]
for name,f in sorted(expected.items()):
    locs=defs.get(name, [])
    if len(locs)!=1 or locs[0]!=f:
        mismatch.append((name, f, locs))
print("plan names checked:", len(expected))
print("mismatches:", len(mismatch))
for m in mismatch: print("  MISMATCH", m)
print("views.clj exists:", os.path.exists(ROOT+"/src/cfp_scheduler_killer/views.clj"))
extra=sorted(n for n in defs if n not in expected)
print("defs under views/ not named in plan (%d): %s" % (len(extra), extra))
print("duplicate-defined names:", [ (n,l) for n,l in defs.items() if len(l)>1 ])
