import re, sys, json
sys.path.insert(0,'/var/tmp/forge/plan2/T1')
from map import MAP
src=open('/home/forge/src/cc-split-T1/src/cfp_scheduler_killer/views.clj').read()
lines=src.split('\n')
starts=[i for i,l in enumerate(lines) if l.startswith('(def')]
forms=[]
for idx,s in enumerate(starts):
    e = starts[idx+1] if idx+1<len(starts) else len(lines)
    m=re.match(r'\(def[a-z-]*\s+(?:\^:private\s+)?([^\s\(\[\{]+)', lines[s])
    forms.append({'name':m.group(1),'start':s,'end':e,'src':'\n'.join(lines[s:e])})
n2ns={n:ns for ns,ns_names in MAP.items() for n in ns_names}
names=sorted(n2ns, key=len, reverse=True)
# token regex: clojure symbol chars
tok=re.compile(r"[A-Za-z0-9_*+!?<>=\-'.$/]+")
edges={}  # ns -> set(ns)
formdeps={}
for f in forms:
    body=f['src']
    # strip the def line's own name occurrence: just remove first line's name
    toks=set(tok.findall(body))
    # strip string literals crudely? keep simple; also strip comments
    body2='\n'.join(re.sub(r';.*$','',l) for l in body.split('\n'))
    body2=re.sub(r'"(\\.|[^"\\])*"','""',body2, flags=re.S)
    toks=set(tok.findall(body2))
    deps={t for t in toks if t in n2ns and t!=f['name']}
    formdeps[f['name']]=sorted(deps)
    a=n2ns[f['name']]
    for d in deps:
        b=n2ns[d]
        if a!=b: edges.setdefault(a,set()).add(b)
json.dump({k:sorted(v) for k,v in edges.items()}, open('/var/tmp/forge/plan2/T1/ns-edges.json','w'), indent=1)
json.dump(formdeps, open('/var/tmp/forge/plan2/T1/form-deps.json','w'), indent=1)
# topo sort
allns=set(MAP)
order=[]; done=set()
while len(order)<len(allns):
    prog=False
    for n in sorted(allns-done):
        if edges.get(n,set())-done-{n} == set():
            order.append(n); done.add(n); prog=True
    if not prog:
        print("CYCLE among:", sorted(allns-done))
        for n in sorted(allns-done):
            print("  ",n,"->",sorted(edges.get(n,set())-done-{n}))
        break
print("ORDER:", order)
for n in order: print(n, "->", sorted(edges.get(n,set())-{n}))
