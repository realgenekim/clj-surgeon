import json,re,sys
sys.path.insert(0,'/var/tmp/forge/plan2/N2')
from map import MAPPING, FOUNDATION, PUBLIC
forms=json.load(open('/var/tmp/forge/plan2/N2/forms.json'))
lines=open('/home/forge/src/cc-split-N2/src/cfp_scheduler_killer/views.clj').read().split('\n')
name2ns={}
for ns,names in MAPPING.items():
    for n in names: name2ns[n]=ns
allnames=set(name2ns)
# sanity: forms present
present=set(f['name'] for f in forms if f['kind'] and f['kind'].startswith('def'))
print("in plan not in file:",sorted(allnames-present))
print("in file not in plan:",sorted(present-allnames))
# strip strings and comments per form body for reference scan
def clean(text):
    out=[];instr=False;esc=False
    i=0
    while i<len(text):
        c=text[i]
        if instr:
            if esc: esc=False
            elif c=='\\': esc=True
            elif c=='"': instr=False
            out.append(' ')
        else:
            if c=='"': instr=True; out.append(' ')
            elif c==';':
                while i<len(text) and text[i]!='\n': i+=1
                out.append('\n'); continue
            else: out.append(c)
        i+=1
    return ''.join(out)
edges={}
usage={}
for f in forms:
    if not (f['kind'] and f['kind'].startswith('def')): continue
    ns=name2ns.get(f['name'])
    if ns is None: continue
    body=clean('\n'.join(lines[f['start']-1:f['end']]))
    toks=set(re.findall(r"[A-Za-z0-9\*\+\!\-\_\?\<\>\=\']+[A-Za-z0-9\*\+\!\-\_\?\>\=]|[A-Za-z][A-Za-z0-9\*\+\!\-\_\?]*",body))
    toks=set(re.findall(r"(?<![\w\-\.\*\?\>])(?:->)?[A-Za-z][A-Za-z0-9\*\+\!\-\_\?\>\=]*",body))
    refs=set()
    for t in toks:
        if t in allnames and t!=f['name']:
            refs.add(t)
    usage[f['name']]=sorted(refs)
    for r in refs:
        tns=name2ns[r]
        if tns!=ns:
            edges.setdefault((ns,tns),set()).add((f['name'],r))
bad=[]
for (a,b),v in sorted(edges.items()):
    ok = b in FOUNDATION or b=='review'
    pub_bad = a in PUBLIC and b=='organizer-layout'
    flag='' if (ok and not pub_bad) else '  <<< VIOLATION'
    print(f"{a} -> {b}{flag}: {sorted(v)[:12]}")
json.dump({f"{a}|{b}":sorted(list(v)) for (a,b),v in edges.items()},open('/var/tmp/forge/plan2/N2/edges.json','w'),indent=1)
json.dump(usage,open('/var/tmp/forge/plan2/N2/usage.json','w'),indent=1)
