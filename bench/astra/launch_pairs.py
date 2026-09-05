#!/usr/bin/env python3
"""Explicit inclusive schedule slice. Existing artifacts refuse; no resume/skip."""
import argparse,json,os,re,subprocess,sys
from datetime import datetime,timezone
from pathlib import Path
import server_lifecycle as lifecycle
ROOT=Path('/var/tmp/forge/astra-program')
SCHEDULE=ROOT/'primary-pair-schedule.json'
SCHEDULE_SHA='0e9b2fffc96f46b91ea06df3f63b8234aa17e31784c3c0ce5370c21c089fcfea'
SERVER_SHA='da7ba418cbe3e1de22efdd1471a0c295c0422d80'
QUIET=Path('/var/tmp/forge/quiet-window.md')
FIXTURES=Path('/home/forge/tmp/arms/ereg/fanout-k1')
write=lifecycle.write
sha=lifecycle.adapter.file_digest

def quiet(text,now):
 pairs=re.findall(r'(?:^|\s)(owner|until)=([^\s]+)',text)
 fields={}
 for key,val in pairs:
  if key in fields:raise ValueError('duplicate quiet field: '+key)
  fields[key]=val
 if fields.get('owner','').lower()!='astra':raise ValueError('quiet window is not Astra-owned')
 try:until=datetime.fromisoformat(fields['until'].replace('Z','+00:00'))
 except (KeyError,ValueError):raise ValueError('quiet window needs until=ISO-with-offset')
 if until.tzinfo is None or until<=now:raise ValueError('quiet window expired or has no timezone')
 return {'owner':fields['owner'],'until':until.isoformat(),'checked_utc':now.isoformat()}

def check_quiet():return quiet(QUIET.read_text(),datetime.now(timezone.utc))
def paths(row):return {key:ROOT/directory/row['name'] for key,directory in [('arm','arms'),('receipt','receipts'),('server','servers'),('fixture','fixtures')]}
def unused(row):
 ps=paths(row)
 for key in ['arm','receipt','server']:
  if ps[key].exists() or ps[key].is_symlink():raise ValueError('existing '+key+'; refusing '+str(ps[key]))
 if not ps['fixture'].is_dir() or ps['fixture'].is_symlink():raise ValueError('missing/noncanonical fixture')
 return ps

def commands(row):
 ps=paths(row)
 orches=[sys.executable,str(ROOT/'orchestrate.py'),'--receipt',str(ps['receipt']),
         '--oracle-fixtures',str(FIXTURES),'--arm',str(ps['arm']),'--worktree',str(ps['fixture']),
         '--prompt',str(ROOT/'prompts'/('fanout-'+row['route']+'.txt')),'--model',row['model'],
         '--cpus','12,13','--max-wall','900','--canonical',str(ROOT/'canonical')]
 start=None;stop=None
 if row['route']=='tool':
  start=[sys.executable,str(ROOT/'server_lifecycle.py'),'start','--run-dir',str(ps['server']),'--fixture',str(ps['fixture']),'--sha',SERVER_SHA]
  stop=[sys.executable,str(ROOT/'server_lifecycle.py'),'stop','--run-dir',str(ps['server'])]
  orches+=['--mcp-url','http://127.0.0.1:8301/mcp','--server-sha',SERVER_SHA,'--ready',str(ps['server']/'ready.json')]
 return {'start':start,'orchestrate':orches,'stop':stop}

def refusal_flags(value):
 """Inspect structured result values only; never evaluate/string-parse JS."""
 flags=[]
 if isinstance(value,dict):
  for key,val in value.items():
   if key in ('error_type','error-type','errorType') and val:flags.append(str(val))
   if key in ('isError','is_error') and val is True:flags.append(key)
   if key=='ok' and val is False:flags.append('ok=false')
   if key=='Err' and val is not None:flags.append('Err')
   if isinstance(val,(dict,list)):flags+=refusal_flags(val)
 elif isinstance(value,list):
  for val in value:flags+=refusal_flags(val)
 return sorted(set(flags))

def observed_refusals(rows):
 found=[];mcp=0
 for line,row in enumerate(rows,1):
  p=row.get('payload',{});item=p.get('item',{})
  if p.get('type')=='mcp_tool_call_end':event=p;mcp+=1
  elif p.get('type')=='item_completed' and item.get('type')=='McpToolCall':event=item;mcp+=1
  else:continue
  flags=refusal_flags(event.get('result',{}))
  if event.get('status') in ('failed','error') or event.get('error'):flags+=['runtime-failed']
  if flags:found.append({'rollout_line':line,'flags':sorted(set(flags)),'tool':event.get('tool',event.get('invocation',{}).get('tool'))})
 return {'mcp_completed_events':mcp,'typed_refusals':found,
         'coverage':'structured MCP completed-event results plus frozen scorer refusal lists; no assertion about arbitrary JS strings or unrecorded calls'}

def outcome(row):
 ps=paths(row)
 def load(p):return json.loads(p.read_text())
 result=load(ps['arm']/'adapter-result.json');accept=load(ps['receipt']/'acceptance-result.json')
 receipt=load(ps['arm']/'receipt.json');orches=load(ps['receipt']/'orchestration-result.json')
 observation=observed_refusals([json.loads(l) for l in (ps['arm']/'rollout.jsonl').read_text().splitlines()])
 failures=[]
 if result.get('valid_measurement') is not True:failures.append('invalid-measurement')
 if accept.get('independently_accepted') is not True:failures.append('independent-acceptance-failed')
 if orches.get('slot_returncode')!=0:failures.append('orchestrator-nonzero')
 if observation['typed_refusals'] or receipt.get('refusals') or receipt.get('refusals_mcp'):failures.append('detected-tool-refusal')
 if row['route']=='tool' and observation['mcp_completed_events']==0:failures.append('tool-runtime-evidence-missing')
 return {'failures':failures,'refusal_observation':observation,'contamination':orches.get('contamination'),
         'automatic_exclusion':False,'experimental_claim':False}

def invoke(argv,log):
 with log.open('x') as out:
  try:return subprocess.run(argv,env=dict(os.environ,SLOT_OWNER='astra',PYTHONDONTWRITEBYTECODE='1'),stdout=out,stderr=subprocess.STDOUT).returncode
  except OSError as error:out.write('SPAWN-FAILED: '+str(error)+'\n');return 127

def run_row(index,row,batch):
 record={'index':index,'row':row,'commands':commands(row),'quiet_before':check_quiet()}
 ps=unused(row);entry=batch/f'{index:02d}-{row["name"]}';entry.mkdir()
 write(entry/'plan.json',record);started=False;failure=None;data=None
 try:
  if record['commands']['start']:
   started=True
   if invoke(record['commands']['start'],entry/'server-start.log')!=0:raise ValueError('server-start-failed')
  record['quiet_before_orchestrator']=check_quiet()
  if invoke(record['commands']['orchestrate'],entry/'orchestrator.log')!=0:raise ValueError('orchestrator-nonzero')
  data=outcome(row)
  if data['failures']:raise ValueError(','.join(data['failures']))
 except (ValueError,OSError) as error:failure=str(error)
 finally:
  if started and ((ps['server']/'owner.json').exists() or (ps['server']/'stopped.json').exists()):
   rc=invoke(record['commands']['stop'],entry/'server-stop.log')
   if rc:failure=(failure+'; ' if failure else '')+'server-stop-failed'
 write(entry/'result.json',{'failure':failure,'outcome':data,'quiet_before_orchestrator':record.get('quiet_before_orchestrator'),'retained':True})
 if failure:raise ValueError('row '+str(index)+' stopped: '+failure)
 return {'index':index,'name':row['name'],'retained':True}

def selection(first,last):
 if sha(SCHEDULE)!=SCHEDULE_SHA:raise ValueError('frozen schedule changed')
 rows=json.loads(SCHEDULE.read_text())
 if not 1<=first<=last<=len(rows):raise ValueError('indices must be1..24 inclusive')
 return list(enumerate(rows[first-1:last],first))

def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--first',type=int,required=True);p.add_argument('--last',type=int,required=True);p.add_argument('--dry-run',action='store_true');args=p.parse_args()
 selected=selection(args.first,args.last)
 for _,row in selected:unused(row)
 batch=ROOT/'batches'/f'{args.first:02d}-{args.last:02d}'
 if batch.exists():raise ValueError('batch index exists; no implicit resume')
 plan={'first':args.first,'last':args.last,'schedule_sha256':SCHEDULE_SHA,'server_sha':SERVER_SHA,
       'rows':[{'index':i,'row':r,'commands':commands(r)} for i,r in selected],
       'helper_hashes':{str(ROOT/n):sha(ROOT/n) for n in ['launch_pairs.py','server_lifecycle.py','orchestrate.py']}}
 if args.dry_run:print(json.dumps(plan,indent=2));return 0
 plan['quiet']=check_quiet();batch.mkdir(parents=True);write(batch/'plan.json',plan)
 completed=[]
 try:
  for i,row in selected:
   if any(sha(Path(path))!=expected for path,expected in plan['helper_hashes'].items()):raise ValueError('launcher/helper changed during batch')
   completed.append(run_row(i,row,batch))
 except (ValueError,OSError) as error:
  write(batch/'result.json',{'completed':completed,'terminal':'stopped','reason':str(error),'automatic_exclusion':False});print(str(error),file=sys.stderr);return 2
 write(batch/'result.json',{'completed':completed,'terminal':'completed-slice','experimental_claim':False});return 0

if __name__=='__main__':
 try:sys.exit(main())
 except (ValueError,OSError) as error:print('PAIR LAUNCH REFUSED: '+str(error),file=sys.stderr);sys.exit(2)
