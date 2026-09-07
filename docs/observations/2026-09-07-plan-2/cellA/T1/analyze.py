import re, sys, json
sys.path.insert(0,'/var/tmp/forge/plan2/T1')
from map import MAP
src=open('/home/forge/src/cc-split-T1/src/cfp_scheduler_killer/views.clj').read()
lines=src.split('\n')
starts=[i for i,l in enumerate(lines) if l.startswith('(def')]
forms=[]
for idx,s in enumerate(starts):
    e = starts[idx+1] if idx+1<len(starts) else len(lines)
    m=re.match(r'\(def[a-z-]*\s+(?:\^:private\s+|\^\{[^}]*\}\s+)?([^\s\(\[\{]+)', lines[s])
    name=m.group(1)
    forms.append({'name':name,'start':s,'end':e,'src':'\n'.join(lines[s:e])})
names=[f['name'] for f in forms]
print("total defs:", len(forms))
dup=[n for n in set(names) if names.count(n)>1]
print("dups:",dup)
n2ns={}
for ns,ns_names in MAP.items():
    for n in ns_names:
        if n in n2ns: print("DOUBLE-MAPPED", n)
        n2ns[n]=ns
mapped=set(n2ns)
allnames=set(names)
print("in plan not in file:", sorted(mapped-allnames))
print("in file not in plan:", sorted(allnames-mapped))
print("plan count:", len(mapped))
