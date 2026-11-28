import pathlib,re,json,os,sys
root=pathlib.Path(sys.argv[1] if len(sys.argv)>1 else os.getcwd()).resolve()
r=json.load(open('/var/tmp/forge/plan2/cellC/cc-split.json'))
assert not (root/r['source']['file']).exists(), 'original views.clj still exists'
actual={}
for file in (root/'src/cfp_scheduler_killer/views').glob('*.clj'):
 text=file.read_text()
 for name in re.findall(r'^\(def(?:n-|n|macro)?\s+(?:\^:private\s+)?([^\s\[()]+)',text,re.M):
  actual.setdefault(name,[]).append(str(file.relative_to(root)))
expected={n:d['file'] for d in r['destinations'] for n in d['forms']}
assert len(expected)==141,len(expected)
assert set(actual)==set(expected),(set(actual)-set(expected),set(expected)-set(actual))
for name,file in expected.items(): assert actual[name]==[file],(name,actual[name],file)
print('PASS: views.clj gone; all 141 owners occur exactly once in their 20 listed destination files.')
