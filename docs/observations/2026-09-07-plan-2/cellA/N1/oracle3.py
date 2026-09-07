import sys, re, os
sys.path.insert(0,'/var/tmp/forge/plan2/N1')
from map import OWNER, FILE
D='/home/forge/src/cc-split-N1/src/cfp_scheduler_killer/views/'
files={f: open(D+f).read() for f in sorted(os.listdir(D))}
mismatch=[]
for name, dest in sorted(OWNER.items()):
    pat=re.compile(r'^\(def[a-z-]*\s+(?:\^:private\s+)?'+re.escape(name)+r'(?=[\s)])', re.M)
    hits={f: len(pat.findall(t)) for f,t in files.items()}
    total=sum(hits.values())
    want=FILE[dest]+'.clj'
    if total!=1 or hits[want]!=1:
        mismatch.append((name, want, {f:c for f,c in hits.items() if c}))
print("ORACLE3: %d plan form names checked (129 named + 12 ^:private slots resolved)" % len(OWNER))
print("ORACLE3 mismatches: %d" % len(mismatch))
for m in mismatch: print("  MISMATCH", m)
