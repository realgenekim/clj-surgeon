import json,re,collections
src=open('/home/forge/src/cc-split-T2/src/cfp_scheduler_killer/views.clj').read().split('\n')
outline=json.load(open('outline.json'))
forms=outline['results'][0]['outline']['forms']
ns_of={}
priv=set()
for line in open('mapping.txt'):
    p=line.split(); ns_of[p[1]]=p[0]
allnames=set(ns_of)
for f in forms:
    if f['type']=='defn-': priv.add(f['name'])
    if f['type']=='def':
        txt=src[f['line']-1]
        if '^:private' in txt: priv.add(f['name'])
sym=re.compile(r"(?<![A-Za-z0-9\-\*\?!><=_.+/])([A-Za-z0-9\-\*\?!><=_.+]+)")
refby=collections.defaultdict(set)
for f in forms:
    if f['type']=='declare': continue
    name=f['name']; ns=ns_of[name]
    txt='\n'.join(src[f['line']-1:f['end_line']])
    # strip line comments
    txt='\n'.join(re.sub(r';.*$','',l) for l in txt.split('\n'))
    for m in sym.finditer(txt):
        t=m.group(1)
        if t in allnames and t!=name:
            refby[t].add(ns)
print("=== private forms referenced from another namespace (must become public) ===")
for n in sorted(priv):
    other={x for x in refby[n] if x!=ns_of[n]}
    if other: print(f"  {n} ({ns_of[n]}) <- {sorted(other)}")
print()
print("datastar-script refby:",sorted(refby['datastar-script']))
print("time-travel-bar refby:",sorted(refby['time-travel-bar']))
print("row-controls* refby:",sorted(refby['row-controls*']))
print()
print("=== all private forms ===")
print(sorted(priv))
