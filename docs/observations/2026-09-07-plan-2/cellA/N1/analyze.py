import sys, json, re
sys.path.insert(0,'/var/tmp/forge/plan2/N1')
from map import OWNER, PLAN, NS, ALIAS, FILE
src=open('/home/forge/src/cc-split-N1/src/cfp_scheduler_killer/views.clj').read()
lines=src.split('\n')
forms=json.load(open('/var/tmp/forge/plan2/N1/forms.json'))

SYM=set("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789*+!_'?<>=/.&%-")
def tokens(text):
    """yield (start,end,tok) for bare symbol tokens (not keywords, not in strings/comments/chars)"""
    i=0; n=len(text); out=[]
    while i<n:
        c=text[i]
        if c=='"':
            i+=1
            while i<n:
                if text[i]=='\\': i+=2; continue
                if text[i]=='"': i+=1; break
                i+=1
            continue
        if c==';':
            while i<n and text[i]!='\n': i+=1
            continue
        if c=='\\':
            i+=2; continue
        if c==':':
            # keyword: consume ':' then token chars
            i+=1
            while i<n and text[i]==':': i+=1
            while i<n and text[i] in SYM: i+=1
            continue
        if c in SYM:
            s=i
            while i<n and text[i] in SYM: i+=1
            out.append((s,i,text[s:i]))
            continue
        i+=1
    return out

def form_text(f, idx):
    return '\n'.join(lines[f['start_line']-1:f['end_line']])

refs={}
for f in forms:
    if not f['head'].startswith('def'): continue
    t=form_text(f,0)
    toks=set(tok for _,_,tok in tokens(t))
    refs[f['name']]=sorted(x for x in toks if x in OWNER and x!=f['name'])

edges=set()
for name, rs in refs.items():
    a=OWNER[name]
    for r in rs:
        b=OWNER[r]
        if a!=b: edges.add((a,b))
allowed={'avatar','form-controls','format','live-drafts','organizer-layout','shell','review'}
print("EDGES:")
for a,b in sorted(edges):
    flag='' if b in allowed else '   <<< DISALLOWED TARGET'
    print(f"  {a} -> {b}{flag}")
# cycle check
import itertools
rem=set([x for e in edges for x in e]); E=set(edges)
while rem:
    targets={b for a,b in E}
    roots=rem-targets
    if not roots:
        print("CYCLE among", rem); break
    rem-=roots
    E={(a,b) for a,b in E if a not in roots}
else:
    print("ACYCLIC ok")
json.dump({'refs':refs,'edges':sorted(edges)},open('/var/tmp/forge/plan2/N1/refs.json','w'),indent=1)
