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
pat_alias=re.compile(r"(?<![A-Za-z0-9._!?*<>=+-])views/([A-Za-z0-9!?*<>=+.-]+)")
pat_fq=re.compile(r"(?<![A-Za-z0-9._!?*<>=+-])cfp-scheduler-killer\.views/([A-Za-z0-9!?*<>=+.-]+)")
edits=[]
per_file_ns=collections.defaultdict(set)
unknown=collections.defaultdict(set)
for f in files:
    txt=open(root+f).read()
    c1=collections.Counter(m.group(1) for m in pat_alias.finditer(txt))
    c2=collections.Counter(m.group(1) for m in pat_fq.finditer(txt))
    for name,n in sorted(c1.items()):
        if name not in ns_of:
            unknown[f].add(name); continue
        a='v-'+ns_of[name]
        per_file_ns[f].add(ns_of[name])
        edits.append({"file":f,"within":{"root":True},"from":"views/"+name,"to":a+"/"+name,"matches":n})
    for name,n in sorted(c2.items()):
        if name not in ns_of:
            unknown[f].add('FQ:'+name); continue
        per_file_ns[f].add(ns_of[name])
        edits.append({"file":f,"within":{"root":True},
                      "from":"cfp-scheduler-killer.views/"+name,
                      "to":"cfp-scheduler-killer.views."+ns_of[name]+"/"+name,"matches":n})
json.dump(edits,open('caller-edits.json','w'),indent=1)
json.dump({k:sorted(v) for k,v in per_file_ns.items()},open('caller-requires.json','w'),indent=1)
print("edits:",len(edits))
print("unknown (left alone):",{k:sorted(v) for k,v in unknown.items()})
for k,v in per_file_ns.items(): print(k,len(v),sorted(v))
