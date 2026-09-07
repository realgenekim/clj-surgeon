import json,re
src=open('/home/forge/src/cc-split-T2/src/cfp_scheduler_killer/views.clj').read().split('\n')
starts=[]
for i,l in enumerate(src):
    m=re.match(r'\((def[a-z\-]*|declare)\s+(?:\^:private\s+)?([^\s\)\]]+)',l)
    if m: starts.append((i+1,m.group(1),m.group(2)))
forms=[]
for j,(ln,typ,nm) in enumerate(starts):
    end = starts[j+1][0]-1 if j+1<len(starts) else len(src)
    forms.append({'line':ln,'end_line':end,'type':'declare' if typ=='declare' else typ,'name':nm})
json.dump({'results':[{'outline':{'forms':forms}}]},open('outline.json','w'))
print(len(forms))
names=[f['name'] for f in forms]
import collections
print([k for k,v in collections.Counter(names).items() if v>1])
