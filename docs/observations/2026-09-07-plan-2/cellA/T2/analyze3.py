import json,re,collections
src=open('/home/forge/src/cc-split-T2/src/cfp_scheduler_killer/views.clj').read().split('\n')
forms=json.load(open('outline.json'))['results'][0]['outline']['forms']
ns_of={}
for line in open('mapping.txt'):
    p=line.split(); ns_of[p[1]]=p[0]
allnames=set(ns_of)
sym=re.compile(r"(?<![A-Za-z0-9\-\*\?!><=_.+/])([A-Za-z0-9\-\*\?!><=_.+]+)")
detail=collections.defaultdict(set)
for f in forms:
    if f['type']=='declare': continue
    name=f['name']; ns=ns_of[name]
    txt='\n'.join(src[f['line']-1:f['end_line']])
    txt='\n'.join(re.sub(r';.*$','',l) for l in txt.split('\n'))
    txt=re.sub(r'"(?:[^"\\]|\\.)*"','""',txt)
    for m in sym.finditer(txt):
        t=m.group(1)
        if t in allnames and t!=name and ns_of[t]!=ns:
            detail[(ns,ns_of[t])].add((t,name))
edges=collections.defaultdict(set)
for (a,b),v in sorted(detail.items()):
    edges[a].add(b)
    print(f"{a} -> {b}: {sorted(set(x for x,_ in v))}")
print()
FOUND={"avatar","form-controls","format","live-drafts","organizer-layout","shell"}
ALLOWED=FOUND|{"review"}
for a in edges:
    for b in edges[a]:
        if b not in ALLOWED: print("VIOLATION",a,b,sorted(detail[(a,b)]))
for a in {"auth","portal","public-cfp"}:
    if "organizer-layout" in edges.get(a,()): print("PUBLIC-VIOLATION",a)
json.dump({k:sorted(v) for k,v in edges.items()},open('edges.json','w'),indent=1)
