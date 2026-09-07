import json,re,sys,collections
src=open('/home/forge/src/cc-split-T2/src/cfp_scheduler_killer/views.clj').read().split('\n')
outline=json.load(open('outline.json'))
forms=outline['results'][0]['outline']['forms']
ns_of={}
for line in open('mapping.txt'):
    p=line.split()
    ns_of[p[1]]=p[0]
# declares
decl={'time-travel-bar':'organizer-layout','row-controls*':'review','datastar-script':'shell'}
allnames=set(ns_of)
# build per-form body text (excluding the def line's own name token? keep simple)
bodies=[]
for f in forms:
    if f['type']=='declare': continue
    txt='\n'.join(src[f['line']-1:f['end_line']])
    bodies.append((f['name'],ns_of[f['name']],txt))
sym=re.compile(r"(?<![A-Za-z0-9\-\*\?!><=_.+/])([A-Za-z0-9\-\*\?!><=_.+]+)")
edges=collections.defaultdict(set)
detail=collections.defaultdict(set)
for name,ns,txt in bodies:
    used=set()
    for m in sym.finditer(txt):
        t=m.group(1)
        if t in allnames: used.add(t)
    used.discard(name)
    for u in used:
        tns=ns_of[u]
        if tns!=ns:
            edges[ns].add(tns)
            detail[(ns,tns)].add(u)
for a in sorted(edges):
    print(a,'->',sorted(edges[a]))
print()
FOUND={"avatar","form-controls","format","live-drafts","organizer-layout","shell"}
ALLOWED=FOUND|{"review"}
print("=== VIOLATIONS (target not foundation/review) ===")
for (a,b),v in sorted(detail.items()):
    if b not in ALLOWED:
        print(f"{a} -> {b}: {sorted(v)}")
print()
print("=== PUBLIC -> organizer-layout ===")
for (a,b),v in sorted(detail.items()):
    if a in {"auth","portal","public-cfp"} and b=="organizer-layout":
        print(f"{a} -> {b}: {sorted(v)}")

print()
print("=== detail for selected pairs ===")
for pair in [("organizer-layout","review"),("review","organizer-layout"),("auth","form-builder"),("form-builder","portal"),("integrations","review"),("dashboard","review"),("log","review"),("people","review"),("organizer-layout","shell")]:
    print(pair, sorted(detail.get(pair,[])))
