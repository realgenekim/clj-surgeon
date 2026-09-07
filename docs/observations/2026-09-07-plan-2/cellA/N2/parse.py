import re,json,sys
src=open('/home/forge/src/cc-split-N2/src/cfp_scheduler_killer/views.clj').read()
lines=src.split('\n')
# find top-level form start lines: line starting with '(' at col 0
forms=[]
i=0
n=len(lines)
def scan(start):
    depth=0;instr=False;esc=False;incomment=False
    j=start
    while j<n:
        line=lines[j]
        k=0
        incomment=False
        while k<len(line):
            c=line[k]
            if instr:
                if esc: esc=False
                elif c=='\\': esc=True
                elif c=='"': instr=False
            elif incomment:
                pass
            else:
                if c=='"': instr=True
                elif c==';': incomment=True
                elif c=='\\':
                    k+=1  # char literal
                elif c in '([{': depth+=1
                elif c in ')]}':
                    depth-=1
                    if depth==0:
                        return j
            k+=1
        j+=1
    return n-1
while i<n:
    if lines[i].startswith('(') :
        end=scan(i)
        forms.append((i,end,lines[i]))
        i=end+1
    else:
        i+=1
out=[]
for s,e,first in forms:
    m=re.match(r'\((def\S*|comment|ns)\s+(\^:private\s+)?(\^\S+\s+)*([^\s\)\]]+)',first)
    kind=None;name=None;priv=False
    if m:
        kind=m.group(1);name=m.group(4);priv=bool(m.group(2)) or '^:private' in first
    out.append(dict(start=s+1,end=e+1,kind=kind,name=name,private=priv,first=first))
json.dump(out,open('/var/tmp/forge/plan2/N2/forms.json','w'),indent=1)
for f in out:
    print(f['start'],f['end'],f['kind'],f['name'],'PRIV' if f['private'] else '')
