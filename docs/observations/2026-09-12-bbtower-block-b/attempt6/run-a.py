import datetime, json, os, pathlib, subprocess, time
root=pathlib.Path('/home/forge/src/clj-surgeon-bbtower')
out=root/'docs/observations/2026-09-12-bbtower-block-b/attempt6'
argv=['clojure','-J-Xms64m','-J-Xmx512m','-M:clj-surgeon/test-battery-parallel','--suite','fast','--work-dir',str(out/'a-base')]
record={'name':'a-base','argv':argv,'cwd':str(root/'target/base-checkout'),'started_utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'environment':{k:os.environ.get(k) for k in ['JAVA_TOOL_OPTIONS','_JAVA_OPTIONS','JDK_JAVA_OPTIONS','TMPDIR']}}
(out/'a-launch.json').write_text(json.dumps(record,indent=2)+'\n')
t0=time.monotonic_ns()
with (out/'a-base.log').open('w') as log:
 result=subprocess.run(argv,cwd=record['cwd'],stdout=log,stderr=subprocess.STDOUT)
record.update(exit=result.returncode,elapsed_ms=(time.monotonic_ns()-t0)//1000000,completed_utc=datetime.datetime.now(datetime.timezone.utc).isoformat())
(out/'a-launch.json').write_text(json.dumps(record,indent=2)+'\n')
(out/'meter.tsv').write_text('name\tstarted_utc\tcompleted_utc\telapsed_ms\texit\treceipt\n'+'\t'.join(str(record[k]) for k in ['name','started_utc','completed_utc','elapsed_ms','exit'])+'\ta-launch.json\n')
print(json.dumps(record,indent=2))
