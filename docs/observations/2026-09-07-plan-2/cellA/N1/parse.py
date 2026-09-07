import re, json, sys
p='/home/forge/src/cc-split-N1/src/cfp_scheduler_killer/views.clj'
src=open(p).read()
lines=src.split('\n')
# find top-level form start lines: line begins with '('
# use paren depth scanner respecting strings, char literals, ; comments
depth=0
i=0
n=len(src)
in_str=False
forms=[]  # (start_idx, end_idx)
start=None
while i<n:
    c=src[i]
    if in_str:
        if c=='\\': i+=2; continue
        if c=='"': in_str=False
        i+=1; continue
    if c==';':
        while i<n and src[i]!='\n': i+=1
        continue
    if c=='\\':
        i+=2; continue
    if c=='"':
        in_str=True; i+=1; continue
    if c in '([{':
        if depth==0: start=i
        depth+=1
    elif c in ')]}':
        depth-=1
        if depth==0:
            forms.append((start,i+1))
    i+=1
def linenum(idx): return src.count('\n',0,idx)+1
out=[]
for (s,e) in forms:
    text=src[s:e]
    m=re.match(r'\(([a-zA-Z0-9!?*<>=_./-]+)\s+(?:\^:private\s+|\^\{[^}]*\}\s+)*([a-zA-Z0-9!?*<>=+_./>-]+)', text)
    head=m.group(1) if m else '?'
    name=m.group(2) if m else '?'
    out.append({'start_line':linenum(s),'end_line':linenum(e-1),'head':head,'name':name,'private':'^:private' in text[:120] or head.endswith('-') and head in ('defn-','defmacro-')})
json.dump(out,open('/var/tmp/forge/plan2/N1/forms.json','w'),indent=1)
print(len(out))
for f in out: print(f['start_line'],f['end_line'],f['head'],f['name'])
