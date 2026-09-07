import re,json,collections
ns_of={}
for line in open('mapping.txt'):
    p=line.split(); ns_of[p[1]]=p[0]
files=["src/cfp_scheduler_killer/server.clj",
       "test/cfp_scheduler_killer/polish_test.clj",
       "test/cfp_scheduler_killer/forms_test.clj",
       "test/cfp_scheduler_killer/comms_test.clj",
       "test/cfp_scheduler_killer/views_test.clj"]
root='/home/forge/src/cc-split-T2/'
def strip(txt):
    out=[];i=0;n=len(txt)
    while i<n:
        c=txt[i]
        if c=='"':
            j=i+1
            while j<n:
                if txt[j]=='\\': j+=2; continue
                if txt[j]=='"': break
                j+=1
            out.append(' '*(j-i+1)); i=j+1
        elif c==';':
            j=txt.find('\n',i)
            if j<0: j=n
            out.append(' '*(j-i)); i=j
        else:
            out.append(c); i+=1
    return ''.join(out)
pat_alias=re.compile(r"(?<![A-Za-z0-9._!?*<>=+-])views/([A-Za-z0-9!?*<>=+.-]+)")
pat_fq=re.compile(r"(?<![A-Za-z0-9._!?*<>=+-])cfp-scheduler-killer\.views/([A-Za-z0-9!?*<>=+.-]+)")
edits=[];per=collections.defaultdict(set);diff=[]
for f in files:
    raw=open(root+f).read(); txt=strip(raw)
    c0=collections.Counter(m.group(1) for m in pat_alias.finditer(raw))
    c1=collections.Counter(m.group(1) for m in pat_alias.finditer(txt))
    c2=collections.Counter(m.group(1) for m in pat_fq.finditer(txt))
    for k in c0:
        if c0[k]!=c1.get(k,0): diff.append((f,k,c0[k],c1.get(k,0)))
    for name,n in sorted(c1.items()):
        a='v-'+ns_of[name]; per[f].add(ns_of[name])
        edits.append({"file":f,"within":{"root":True},"from":"views/"+name,"to":a+"/"+name,"matches":n})
    for name,n in sorted(c2.items()):
        per[f].add(ns_of[name])
        edits.append({"file":f,"within":{"root":True},"from":"cfp-scheduler-killer.views/"+name,
                      "to":"cfp-scheduler-killer.views."+ns_of[name]+"/"+name,"matches":n})
json.dump(edits,open('caller-edits.json','w'),indent=1)
json.dump({k:sorted(v) for k,v in per.items()},open('caller-requires.json','w'),indent=1)
print("edits:",len(edits),"raw-vs-stripped diffs:",diff)
