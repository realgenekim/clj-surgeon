#!/usr/bin/env python3
"""Read-only, single-process session miner. Transcript bytes are never executed."""
import argparse
import collections as C
import datetime as D
import hashlib
import json
import math
import pathlib
import re
import shlex
import statistics
import subprocess
import time

HOME = pathlib.Path.home()
SECRET = re.compile(r'ghp_[A-Za-z0-9]{20,}|github_pat_|sk-[A-Za-z0-9]{20,}|xox[abprs]-|-----BEGIN|Bearer [A-Za-z0-9._-]{20,}|AKIA[0-9A-Z]{16}|["\x27](?:password|secret)["\x27]\s*[:=]\s*\S+', re.I)
CLOJURE = re.compile(r'[^\s"\x27<>`,;()\[\]{}]+\.(?:clj|cljc|cljs|edn)\b')
SURGEON = re.compile(r'clj[-_]surgeon|namespace_split|require_change', re.I)
CALL_FIELDS = ['date','store','session','cwd','repo','surface','op','outcome','refusal_code','next_action','fallback','turn_id','actions_to_turn_end','input_shape','output_head']


def compact(x):
    return json.dumps(x, ensure_ascii=False, separators=(',', ':'))


def safe(s):
    s = str(s).replace('/home/forge', '~')
    # Do not expose other home directories, including platform-specific homes.
    return re.sub(r'/(?:home|Users)/[^/\s"\x27]+', '<home>', s)


def one(s, n=300):
    return safe(s).replace('\t', ' ').replace('\r', ' ').replace('\n', ' ')[:n]


def clip(x):
    if isinstance(x, str): return safe(x)[:60]
    if isinstance(x, dict): return {safe(k): clip(v) for k,v in x.items()}
    if isinstance(x, list): return [clip(v) for v in x]
    return x


def loads(x):
    if isinstance(x, str):
        try: return json.loads(x)
        except (ValueError, TypeError): pass
    return x


def text_content(x):
    if isinstance(x, str): return x
    if isinstance(x, list):
        return '\n'.join(text_content(i.get('text', i.get('content',''))) if isinstance(i,dict) else str(i) for i in x)
    return compact(x) if x is not None else ''


# A deliberately non-evaluating JavaScript literal lexer. Dynamic expressions stay opaque.
JS = re.compile(r'\s+|//[^\n]*|/\*[\s\S]*?\*/|"(?:\\.|[^"\\])*"|\x27(?:\\.|[^\x27\\])*\x27|`(?:\\.|[^`\\])*`|[A-Za-z_$][\w$]*|-?\d+(?:\.\d+)?|.', re.S)


def js_string(t):
    if t.startswith('"'):
        try: return json.loads(t)
        except ValueError: return t[1:-1]
    t=t[1:-1]
    return re.sub(r'\\(u[0-9a-fA-F]{4}|x[0-9a-fA-F]{2}|.)', lambda m: chr(int(m[1][1:],16)) if m[1][0] in 'ux' and len(m[1])>1 else {'n':'\n','r':'\r','t':'\t','b':'\b','f':'\f'}.get(m[1],m[1]),t)


def js_literal(ts, i):
    if i>=len(ts): return '<unresolved>', i
    t=ts[i]
    if t[0:1] in ('"',"'",'`'):
        if t.startswith('`') and '${' in t: return '<unresolved-template>',i+1
        return js_string(t),i+1
    if t in ('{','['):
        obj={} if t=='{' else []; end='}' if t=='{' else ']'; i+=1
        while i<len(ts) and ts[i]!=end:
            if ts[i]==',': i+=1; continue
            if t=='{':
                k=js_string(ts[i]) if ts[i][0:1] in ('"',"'") else ts[i]; i+=1
                if i>=len(ts) or ts[i]!=':': return '<unresolved-object>',i
                v,i=js_literal(ts,i+1);obj[k]=v
            else:
                v,i=js_literal(ts,i);obj.append(v)
            if i<len(ts) and ts[i] not in (',',end): return '<unresolved-expression>',i
        return obj,i+1
    if t in ('true','false','null'): return {'true':True,'false':False,'null':None}[t],i+1
    if re.fullmatch(r'-?\d+(\.\d+)?',t): return loads(t),i+1
    return '<unresolved:'+t+'>',i+1


def unwrap(name, arg, stats):
    if name.split('.')[-1] != 'exec' or not isinstance(arg,str): return [(name,loads(arg))]
    ts=[m[0] for m in JS.finditer(arg) if not m[0].isspace() and not m[0].startswith(('//','/*'))]
    found=[]
    for i in range(len(ts)-4):
        if ts[i]=='tools' and ts[i+1]=='.' and re.fullmatch(r'[\w$]+',ts[i+2]) and ts[i+3]=='(':
            v,_=js_literal(ts,i+4); found.append((ts[i+2],v))
            if '<unresolved' in compact(v): stats['unresolved_nested_arguments']+=1
    if found:
        stats['decoded_exec_wrappers']+=1
        if len(found)>1: stats['multi_call_wrappers']+=1
        return found
    stats['opaque_exec_wrappers']+=1
    return [(name,{'opaque_exec':True})]


def shell_command(name,arg):
    if name.split('.')[-1].lower() in ('bash','shell','exec_command','run_shell_command','shell_command','monitor'):
        if isinstance(arg,dict): return arg.get('cmd',arg.get('command',''))
        if isinstance(arg,list): return ' '.join(map(str,arg))
        if isinstance(arg,str): return arg
    return ''


def shell_segments(cmd):
    # Heredoc bodies are data. Keep the command line, discard the body through delimiter.
    lines=[]; delimiters=[]
    for line in cmd.splitlines():
        if delimiters:
            if line.strip()==delimiters[0]: delimiters.pop(0)
            continue
        lines.append(line)
        delimiters += re.findall(r'<<-?\s*["\x27]?([\w-]+)["\x27]?',line)
    try:
        lexer=shlex.shlex('\n'.join(lines),posix=True,punctuation_chars=';&|()\n')
        lexer.whitespace=' \t\r';lexer.whitespace_split=True;lexer.commenters='#'
        tokens=list(lexer)
    except ValueError: return []
    segs=[];seg=[]
    for t in tokens:
        if re.fullmatch(r'[;&|()\n]+',t):
            if seg:segs.append(seg);seg=[]
        else:seg.append(t)
    if seg:segs.append(seg)
    return segs


def cli_ops(cmd):
    ops=[]
    for seg in shell_segments(cmd):
        i=0
        while i<len(seg) and (re.match(r'^\w+=',seg[i]) or seg[i] in ('env','command','exec','time','timeout','nice','sudo') or re.fullmatch(r'\d+(?:s|m)?',seg[i]) or seg[i].startswith('-')): i+=1
        if i>=len(seg):continue
        exe=seg[i]
        if not re.search(r'(?:^|/)clj-surgeon$',exe):continue
        rest=seg[i+1:]
        if ':op' in rest:
            j=rest.index(':op');ops.append(rest[j+1].lstrip(':') if j+1<len(rest) else 'missing-op')
        elif ':ls' in rest:ops.append('ls')
        elif ':ls-tree' in rest:ops.append('ls-tree')
        elif 'bin/clj-surgeon' in exe:
            ops.append(rest[0].lstrip(':') if rest else 'unspecified')
    return ops


def category(name,arg,cmd):
    base=name.split('.')[-1].lower()
    if base in ('apply_patch','edit','multiedit','write','notebookedit'):return 'native-edit'
    if cmd:
        if re.search(r'\bsed\s+-[^\s]*i|\bperl\s+-[^\s]*i|\bpython[23]?\s+-\s*<<|\bcat\s*>|\btee\b|(?:^|[;&|\n])\s*ed\b|<<[^\n]*>',cmd):return 'native-edit'
        if re.search(r'\b(?:make|clojure|clj|bb|lein|kaocha|pytest|test-focus|test)\b|\bgit\s+(?:diff|status)\b',cmd):return 'verify'
        if re.search(r'(?:^|[;&|\n])\s*(?:rg|grep|cat|head|tail|ls|find)\b|\bsed\s+-n',cmd):return 'native-read'
    if base in ('read','grep','glob'):return 'native-read'
    return 'other'


def changes(name,arg,cmd):
    """Extract explicit edited Clojure paths and before/after evidence, never read source."""
    base=name.split('.')[-1].lower(); before=[];after=[]; files=set()
    if isinstance(arg,dict):
        p=arg.get('file_path',arg.get('path',''))
        if p and CLOJURE.search(p):files.add(p)
        if base=='edit':before.append(arg.get('old_string',''));after.append(arg.get('new_string',''))
        if base=='multiedit':
            for e in arg.get('edits',[]):before.append(e.get('old_string',''));after.append(e.get('new_string',''))
        if base=='write':after.append(arg.get('content',''))
    patch=arg if isinstance(arg,str) else (arg.get('patch',arg.get('input','')) if isinstance(arg,dict) else '')
    if base=='apply_patch' or '*** Begin Patch' in patch or '*** Begin Patch' in cmd:
        patch=patch or cmd
        for line in patch.splitlines():
            m=re.match(r'\*\*\* (?:Update|Add|Delete|Move to) (?:File: )?(.+)',line)
            if m and CLOJURE.search(m[1]):files.add(m[1].strip())
            if line.startswith('-') and not line.startswith('---'):before.append(line[1:])
            if line.startswith('+') and not line.startswith('+++'):after.append(line[1:])
    # Shell inference needs an edit command, not incidental paths in a read.
    if cmd and category(name,arg,cmd)=='native-edit':files.update(CLOJURE.findall(cmd))
    return files,'\n'.join(before),'\n'.join(after)


def edit_units(name,arg,cmd,files,before,after):
    patch=arg if isinstance(arg,str) else (arg.get('patch',arg.get('input','')) if isinstance(arg,dict) else '')
    patch=patch or cmd
    units=[];current=None
    if '*** Begin Patch' in patch:
        for line in patch.splitlines():
            match=re.match(r'\*\*\* (?:Update|Add|Delete) File: (.+)',line)
            if match:
                current={'files':{match[1].strip()},'before':'','after':''} if CLOJURE.search(match[1]) else None
                if current:units.append(current)
            elif current and line.startswith('-') and not line.startswith('---'):current['before']+=line[1:]+'\n'
            elif current and line.startswith('+') and not line.startswith('+++'):current['after']+=line[1:]+'\n'
        return units
    return [{'files':files,'before':before,'after':after}] if files and (before or after) else []


def secret_line(line):
    # JSONL embeds quoted credentials inside escaped string values.
    return bool(SECRET.search(line) or SECRET.search(line.replace('\\"', '"').replace("\\'", "'")))


def receipt_fields(out):
    """Extract only top-level fields; returned source and examples are inert data."""
    body=re.split(r'\bOutput:\s*\n',out,maxsplit=1)[-1].strip()
    try:
        obj,end=json.JSONDecoder().raw_decode(body)
        if isinstance(obj,dict):
            if isinstance(obj.get('structuredContent'),dict):return obj['structuredContent'],body
            if 'exit_code' in obj:
                nested,_=receipt_fields(str(obj.get('output','')))
                return dict(nested,_exit=obj.get('exit_code')),body
            if isinstance(obj.get('content'),list):
                for item in obj['content']:
                    if isinstance(item,dict) and item.get('type')=='text':
                        nested,summary=receipt_fields(item.get('text',''))
                        if nested:return nested,summary
            return obj,body
    except (ValueError,RecursionError):pass
    # EDN is tokenized, never read/evaluated. Nested values are skipped intact.
    if body.startswith('{'):
        tokens=re.findall(r'"(?:\\.|[^"\\])*"|[{}\[\]()]|[^\s,{}\[\]()]+',body)
        fields={};i=1
        while i<len(tokens) and tokens[i]!='}':
            key=tokens[i].lstrip(':');i+=1
            if i>=len(tokens):break
            value=tokens[i];i+=1
            if value in ('{','[','('):
                depth=1
                while i<len(tokens) and depth:
                    if tokens[i] in ('{','[','('):depth+=1
                    elif tokens[i] in ('}',']',')'):depth-=1
                    i+=1
                fields[key]='<nested>'
            else:
                if value.startswith('"'):
                    try:value=json.loads(value)
                    except ValueError:value=value[1:-1]
                elif value=='true':value=True
                elif value=='false':value=False
                elif value in ('nil','null'):value=None
                else:value=value.lstrip(':')
                fields[key]=value
        return fields,body
    return {},body


def typed_code(out):
    fields,body=receipt_fields(out)
    for k in ('refusal_code','refusal-code','refusal_type','refusal-type','error_type','error-type','code','kind'):
        v=fields.get(k)
        if isinstance(v,str) and re.fullmatch(r'[\w./-]+',v) and v not in ('error','refusal','refused','<nested>'):return v
    m=re.search(r'^(?:[\w_-]+ )?refused\s*[·:]\s*([\w./-]+)',body,re.I)
    if m:return m[1]
    return ''


def outcome(out,is_error=False):
    if not out:return 'unknown',''
    fields,body=receipt_fields(out)
    code=typed_code(out)
    status=str(fields.get('status',fields.get('state',fields.get('outcome','')))).lower()
    err=fields.get('error')
    summary=body.splitlines()[0] if body else ''
    # Only receipt fields / initial terminal summary are classification evidence.
    if 'timeout' in code or re.search(r'\b(?:timed out|time limit exceeded|TimeoutError)\b',summary,re.I):return 'timeout',code
    refused=any(fields.get(k) not in (None,False,'false','nil','null','') for k in ('refusal','refusal_code','refusal-code','refusal_type','refusal-type'))
    if status in ('refused','refusal') or refused or re.search(r'^(?:[\w_-]+ )?refused\b',summary,re.I) or (code and (fields.get('ok') is False or err)):return 'refusal',code
    if is_error or fields.get('isError') is True or fields.get('is_error') is True or status in ('error','failed') or fields.get('_exit') not in (None,0) or re.search(r'Process exited with code [1-9]|"exit_code"\s*:\s*[1-9]|^(?:Syntax error|Execution error|Traceback|Exception in thread)',summary):return 'error',code
    if err or fields.get('ok') is False:return 'error',code
    if fields.get('ok') is True or fields.get('success') is True or fields.get('read_complete') is True or status in ('ok','success','committed','passed','complete','applied','read-complete') or fields.get('_exit')==0 or fields.get('exit_code')==0:return 'success',''
    if fields.get('operation') in ('show-form','list-forms','ls','cat','xray') or ('ns' in fields and 'forms' in fields):return 'success',''
    if re.search(r'\b(?:ok|success|read_complete)=true\b|atomic commit complete|written bytes read back and verified',body[:2000],re.I):return 'success',''
    if re.search(r'Process exited with code 0',summary):return 'success',''
    return 'unknown',code


def percentile(xs,p):
    return sorted(xs)[max(0,math.ceil(len(xs)*p)-1)] if xs else ''


def med(xs):return statistics.median(xs) if xs else ''


def bucket(n):return '1' if n==1 else '2-3' if n<=3 else '4-9' if n<=9 else '10+'


class Miner:
    def __init__(self,out):
        self.out=out; self.stats=C.Counter();self.rows=[];self.native=[];self.controls=[];self.examples={};self.names=C.Counter();self.output_examples={};self.turn_counts=C.Counter();self.turn_no=0

    def records(self,path,store,sample=False):
        try:
            with path.open(encoding='utf-8',errors='replace') as f:
                for line in f:
                    if secret_line(line):
                        if not sample:self.stats['dropped_secret_lines']+=1
                        continue
                    try:r=json.loads(line)
                    except ValueError:
                        if not sample:self.stats['malformed_lines']+=1
                        continue
                    if not isinstance(r,dict):continue
                    yield r
        except OSError:
            if not sample:self.stats['unreadable_sessions']+=1

    def sampling(self,files):
        # This pass precedes the counting pass. Retain one skeleton for every observed shape.
        for store,paths in files.items():
            for path in paths[::max(1,len(paths)//20)][:21]:
                for n,r in enumerate(self.records(path,store,True)):
                    if n>=2500:break
                    p=r.get('payload',{})
                    subtype=p.get('type') if isinstance(p,dict) else None
                    k=store+'/'+str(r.get('type'))+('/'+str(subtype) if subtype else '')
                    def skeleton(v,depth=0):
                        if isinstance(v,dict):return {k:skeleton(x,depth+1) for k,x in v.items() if k not in ('base_instructions','encrypted_content','tools','summary','message','last_agent_message','diagnostics','usage','input_transformations')}
                        if isinstance(v,list):return [skeleton(x,depth+1) for x in v[:1]]
                        return '<redacted>' if isinstance(v,str) else v
                    if store=='claude' and isinstance(r.get('message'),dict):
                        c=r['message'].get('content',[])
                        if isinstance(c,list):
                            for b in c:
                                if isinstance(b,dict):self.examples.setdefault('claude/block/'+str(b.get('type')),skeleton(b))
                    self.examples.setdefault(k,skeleton(r))

    def scan(self,path,store):
        self.stats['sessions_scanned_'+store]+=1
        cwd='';actions=[];pending={};physical_seen=set();started=False;ended=False;previous_user=False;self.continuations={}
        session=path.name
        session_key=hashlib.sha256(str(path).encode()).hexdigest()[:12]
        self.turn_no=0
        def finish():
            nonlocal actions,pending,started,ended,previous_user
            if actions:self.finish_turn(actions,store,session,session_key)
            actions=[];pending={};started=False;ended=False;previous_user=False
        for r in self.records(path,store):
            p=r.get('payload',{});p=p if isinstance(p,dict) else {}
            typ=r.get('type');ts=r.get('timestamp','')
            if store=='codex':
                if typ=='session_meta':cwd=p.get('cwd',cwd)
                if typ=='turn_context':cwd=p.get('cwd',cwd)
                boundary=(typ=='event_msg' and p.get('type')=='task_started')
                user=(typ=='response_item' and p.get('type')=='message' and p.get('role')=='user')
                if boundary:
                    if actions:finish()
                    started=True;previous_user=False;self.turn_no+=1
                elif user:
                    # task_started + its initial user record describe one turn.
                    if actions and (ended or not started or previous_user):finish();self.turn_no+=1
                    elif not started:self.turn_no+=1
                    previous_user=True
                if typ=='event_msg' and p.get('type') in ('task_complete','turn_aborted'):ended=True
                if typ=='response_item' and p.get('type') in ('function_call','custom_tool_call'):
                    cid=p.get('call_id',p.get('id'));name=p.get('name','');arg=p.get('arguments',p.get('input',{}))
                    key=(cid,name)
                    if cid and key in physical_seen:self.stats['duplicate_call_records']+=1;continue
                    physical_seen.add(key);self.stats['outer_tool_calls_'+store]+=1
                    self.names[(store,name)]+=1
                    made=self.add_call(actions,store,cwd,ts,name,loads(arg),cid)
                    pending[cid]=made
                if typ=='response_item' and p.get('type') in ('function_call_output','custom_tool_call_output'):
                    self.attach(pending.get(p.get('call_id'),[]),p.get('output',''),False)
            else:
                cwd=r.get('cwd',cwd)
                m=r.get('message',{});m=m if isinstance(m,dict) else {};content=m.get('content',[])
                blocks=content if isinstance(content,list) else []
                # Mixed user records containing any prose/image block are real user turns.
                if typ=='user' and (isinstance(content,str) or any(isinstance(b,dict) and b.get('type')!='tool_result' for b in blocks)):
                    finish();self.turn_no+=1
                for b in blocks:
                    if not isinstance(b,dict):continue
                    if typ=='assistant' and b.get('type')=='tool_use':
                        cid=b.get('id');name=b.get('name','');key=(cid,name)
                        if cid and key in physical_seen:self.stats['duplicate_call_records']+=1;continue
                        physical_seen.add(key);self.stats['outer_tool_calls_'+store]+=1;self.names[(store,name)]+=1
                        pending[cid]=self.add_call(actions,store,cwd,ts,name,b.get('input',{}),cid)
                    elif typ=='user' and b.get('type')=='tool_result':self.attach(pending.get(b.get('tool_use_id'),[]),b.get('content',''),b.get('is_error',False))
        finish()

    def add_call(self,actions,store,cwd,ts,name,arg,cid):
        made=[]
        for name,arg in unwrap(name,arg,self.stats):
            cmd=shell_command(name,arg);cmd=cmd if isinstance(cmd,str) else ''
            ops=cli_ops(cmd)
            surface='cli' if ops else ('mcp' if SURGEON.search(name) else '')
            if surface=='mcp':ops=[name.split('__')[-1].split('.')[-1]]
            cat=category(name,arg,cmd)
            files,before,after=changes(name,arg,cmd) if cat=='native-edit' else (set(),'','')
            action={'units':edit_units(name,arg,cmd,files,before,after),'name':name,'arg':arg,'cmd':cmd,'cwd':cwd,'date':str(ts)[:10],'surface':surface,'ops':ops,'cat':cat,'files':files,'before':before,'after':after,'out':'','error':False,'cid':cid}
            if isinstance(arg,dict):
                base=name.split('.')[-1]
                key=('cell',str(arg.get('cell_id'))) if base=='wait' else (('session',str(arg.get('session_id'))) if base=='write_stdin' else None)
                if key and key in getattr(self,'continuations',{}):action['_parent']=self.continuations[key]
            actions.append(action);made.append(action);self.stats['tool_calls_scanned_'+store]+=1
        return made

    def attach(self,actions,out,is_error):
        s=text_content(out)
        if not actions:return
        if len(actions)==1:
            a=actions[0];a['out']=s;a['error']=is_error
            target=a.get('_parent',a)
            if target is not a:
                target['out']=s;target['error']=is_error;self.stats['continuation_outputs_joined']+=1
            if not hasattr(self,'continuations'):self.continuations={}
            match=re.search(r'Script running with cell ID ([\w-]+)',s)
            if match:self.continuations[('cell',match[1])]=target
            try:
                obj=json.loads(s)
                if isinstance(obj,dict) and obj.get('session_id') is not None:self.continuations[('session',str(obj['session_id']))]=target
            except ValueError:pass
            return
        # Wrapper output is not guaranteed to preserve item order or even print every result.
        self.stats['nested_actions_with_unattributable_output']+=len(actions)
        for a in actions:a['ambiguous_output']=True

    def finish_turn(self,aa,store,session,session_key):
        turn=f'{store}:{session_key}:{self.turn_no}'
        self.turn_counts[store]+=1
        ss=[(i,a,op) for i,a in enumerate(aa) for op in a['ops']]
        edits=[a for a in aa if a['cat']=='native-edit'];files=set().union(*(a['files'] for a in edits)) if edits else set()
        # Resolve relative paths lexically against cwd; do not inspect transcript-specified paths.
        normfiles=set()
        for a in edits:
            for p in a['files']:
                normfiles.add(str(pathlib.PurePosixPath(a['cwd'])/p) if not p.startswith('/') else p)
        n=len(normfiles);repo=self.repo(aa[0]['cwd'])
        if not ss and n:
            native_actions=sum(a['cat']=='native-edit' for a in aa)
            rec={'turn_id':turn,'store':store,'session':session,'repo':repo,'bucket':bucket(n),'files':n,'tool_calls':len(aa),'native_actions':native_actions,'class':self.candidate(edits,n)}
            self.controls.append(rec)
            if native_actions>=2 and (n>=2 or rec['class']=='extract-function'):self.native.append(rec)
        for i,a,op in ss:
            nexta=aa[i+1] if i+1<len(aa) else None
            if nexta and nexta['surface']:nxt='surgeon-same' if op in nexta['ops'] else 'surgeon-other'
            else:nxt=nexta['cat'] if nexta else 'turn-end'
            fallback=0
            for later in aa[i+1:]:
                if later['surface']:break
                if later['cat']=='native-edit':fallback=1;break
            out,code=outcome(a['out'],a['error'])
            retried=bool(nexta and nxt=='surgeon-same')
            repaired=bool(retried and outcome(nexta['out'],nexta['error'])[0]=='success')
            row=dict(zip(CALL_FIELDS,[a['date'],store,session,safe(a['cwd']),self.repo(a['cwd']),a['surface'],op,out,code,nxt,fallback,turn,len(aa)-i,one(compact(clip(a['arg']))),one(a['out'])]))
            row.update(_retry=int(retried),_repaired=int(repaired),_next=one(compact({'name':nexta['name'],'input':nexta['arg']})) if nexta else 'turn-end',_bucket=bucket(n) if n else '0',_files=n)
            self.rows.append(row)

    @staticmethod
    def repo(cwd):
        parts=pathlib.PurePosixPath(cwd).parts
        for marker in ('worktrees','.worktrees'):
            if marker in parts:
                j=parts.index(marker)
                if j+1<len(parts):return parts[j+1]
        return parts[-1] if parts else 'unknown'

    @staticmethod
    def candidate(edits,n):
        edits=[u for a in edits for u in a.get('units',[])]
        before='\n'.join(a['before'] for a in edits);after='\n'.join(a['after'] for a in edits)
        ns=lambda t:set(re.findall(r'\(ns\s+([\w.-]+)',t))
        defs=lambda t:set(re.findall(r'\(defn-?\s+([^\s()[\]]+)',t))
        oldns,newns=ns(before),ns(after);olddefs,newdefs=defs(before),defs(after)
        if n>=2 and oldns and newns and oldns!=newns and any(re.search(re.escape(x)+r'[/\s]',before) for x in oldns):return 'rename-namespace'
        reqfiles=set()
        for a in edits:
            if re.search(r':require|\[[\w.-]+\s+:as\s+',a['after']) and a['after']!=a['before']:reqfiles.update(a['files'])
        if len(reqfiles)>=3:return 'add-require-across-files'
        for x in olddefs & newdefs:
            removed={p for a in edits if x in defs(a['before']) and x not in defs(a['after']) for p in a['files']}
            added={p for a in edits if x in defs(a['after']) and x not in defs(a['before']) for p in a['files']}
            if removed and added and removed.isdisjoint(added):return 'move-var'
        byfile=C.defaultdict(lambda: ['', ''])
        for a in edits:
            for p in a['files']:
                byfile[p][0]+=a['before']+'\n';byfile[p][1]+=a['after']+'\n'
        for old,new in byfile.values():
            for x in defs(new)-defs(old):
                if old.strip() and re.search(r'\('+re.escape(x)+r'\s',new):return 'extract-function'
        sig=lambda t:dict(re.findall(r'\(defn-?\s+([^\s()[\]]+)\s*(\[[^\]]*\])',t))
        b,a=sig(before),sig(after)
        if n>=2 and any(x in a and b[x]!=a[x] for x in b):return 'arity-change'
        old=set(re.findall(r'\b([\w.-]+)/[\w!?*+.-]+',before));new=set(re.findall(r'\b([\w.-]+)/[\w!?*+.-]+',after))
        if n>=2 and old-new and new-old:return 'alias-migration'
        return 'other-multi-file' if n>=2 else 'unclassified-single-file'


def tsv(path,fields,rows):
    with path.open('w',encoding='utf-8') as f:
        f.write('\t'.join(fields)+'\n')
        for r in rows:f.write('\t'.join(one(r.get(k,''),100000) for k in fields)+'\n')


def md_table(fields,rows):
    lines=['| '+' | '.join(fields)+' |','| '+' | '.join('---' for _ in fields)+' |']
    for r in rows:lines.append('| '+' | '.join(one(r.get(k,''),1000).replace('|','\\|') for k in fields)+' |')
    return '\n'.join(lines)+'\n'


def coverage_sources(stats):
    p=HOME/'src/clj-surgeon';r=subprocess.run(['git','-C',str(p),'show','b2beec26:docs/intent/helper-extraction/namespace-split-papercuts.edn'],capture_output=True,text=True)
    if r.returncode:stats['papercut_source_unavailable']=1;pap=''
    else:pap='\n'.join(x for x in r.stdout.splitlines() if not secret_line(x))
    entries=[]
    for m in re.finditer(r':id\s+:(NS-SPLIT-\d+)',pap):
        end=pap.find(':id',m.end());chunk=pap[m.end():end if end>=0 else len(pap)]
        entries.append((m[1],chunk))
    bp=p/'.beads/issues.jsonl'
    if bp.exists():
        for line in bp.open(errors='replace'):
            if secret_line(line):continue
            try:b=json.loads(line)
            except ValueError:continue
            entries.append((str(b.get('id','unknown')),str(b.get('title',''))+' '+str(b.get('description',''))))
    else:stats['beads_source_absent']=1
    return entries


def covers(rows,entries):
    # Match distinctive diagnostic phrases, not generic words such as file, call or error.
    needles=['baseline','indentation','docstring','continuation','retained','retention','promotion','private','catalog','warm','cold','rollback','stale','alias collision','unrequired','comment','profile','verification','require','namespace','arity','ambiguous']
    evidence=' '.join(r['op']+' '+r['refusal_code']+' '+r['output_head'] for r in rows).lower()
    hits=[]
    for key,txt in entries:
        matches=[w for w in needles if re.search(r'\b'+re.escape(w)+r'\b',evidence) and re.search(r'\b'+re.escape(w)+r'\b',txt.lower())]
        # At least two keywords or one high-specificity keyword; report keywords for audit.
        if len(matches)>=2 or any(w in matches for w in ('docstring','indentation','continuation','catalog','arity','alias collision','unrequired')):
            hits.append(key+' ('+', '.join(matches)+')')
    return '; '.join(hits[:6]) or 'none found'


def write_outputs(m,entries,start,paths):
    out=m.out;rows=m.rows
    fields=CALL_FIELDS;tsv(out/'calls.tsv',fields,rows)
    weekly=C.defaultdict(list);codes=C.defaultdict(list);groups=C.defaultdict(list);acts=C.defaultdict(list);patterns=C.defaultdict(list)
    for r in rows:
        try:d=D.date.fromisoformat(r['date']);week=(d-D.timedelta(days=d.weekday())).isoformat()
        except ValueError:week='unknown'
        weekly[(week,r['store'],r['surface'])].append(r)
        if r['refusal_code'] and r['outcome']=='refusal':codes[r['refusal_code']].append(r)
        groups[(r['op'],r['surface'])].append(r);acts[r['op']].append(r)
        if r['outcome'] in ('refusal','error') or r['fallback']:patterns[(r['op'],r['outcome'],r['next_action'])].append(r)
    wr=[dict(week=k[0],store=k[1],surface=k[2],calls=len(v),sessions=len({r['session'] for r in v})) for k,v in sorted(weekly.items())]
    cr=[{'code':k,'count':len(v),'example_op':v[0]['op'],'first_date':min(r['date'] for r in v),'last_date':max(r['date'] for r in v)} for k,v in sorted(codes.items(),key=lambda kv:(-len(kv[1]),kv[0]))]
    fr=[dict(op=k[0],surface=k[1],calls=len(v),refusals=sum(r['outcome']=='refusal' for r in v),errors=sum(r['outcome']=='error' for r in v),fallbacks=sum(r['fallback'] for r in v),fallback_rate=round(sum(r['fallback'] for r in v)/len(v),6),retries=sum(r['_retry'] for r in v),repaired=sum(r['_repaired'] for r in v)) for k,v in sorted(groups.items())]
    for stem,data,f in [('calls_per_week',wr,['week','store','surface','calls','sessions']),('refusal_codes',cr,['code','count','example_op','first_date','last_date']),('fallback_by_op',fr,['op','surface','calls','refusals','errors','fallbacks','fallback_rate','retries','repaired'])]:
        tsv(out/(stem+'.tsv'),f,data);(out/(stem+'.md')).write_text(md_table(f,data))
    ar=[dict(block='surgeon',op=k,repo='all',bucket='all',calls=len(v),median_actions_to_turn_end=med([r['actions_to_turn_end'] for r in v]),p90=percentile([r['actions_to_turn_end'] for r in v],.9)) for k,v in sorted(acts.items())]
    ng=C.defaultdict(list)
    for r in m.controls:ng[(r['repo'],r['bucket'])].append(r)
    for (repo,b),v in sorted(ng.items()):ar.append(dict(block='native',op='native',repo=repo,bucket=b,calls=len(v),median_actions_to_turn_end=med([r['tool_calls'] for r in v]),p90=percentile([r['tool_calls'] for r in v],.9)))
    # Explicitly matched surgeon strata alongside the requested global per-op block.
    sg=C.defaultdict(list)
    for r in rows:sg[(r['op'],r['repo'],r['_bucket'])].append(r)
    for (op,repo,b),v in sorted(sg.items()):ar.append(dict(block='surgeon-matched',op=op,repo=repo,bucket=b,calls=len(v),median_actions_to_turn_end=med([r['actions_to_turn_end'] for r in v]),p90=percentile([r['actions_to_turn_end'] for r in v],.9)))
    af=['block','op','repo','bucket','calls','median_actions_to_turn_end','p90'];tsv(out/'actions_to_end.tsv',af,ar);(out/'actions_to_end.md').write_text('For native rows, calls means turns and the median/p90 measure total tool calls per turn.\n\n'+md_table(af,ar))
    evidence_fields=['turn_id','store','session','repo','bucket','files','tool_calls','native_actions','class'];tsv(out/'native_turns.tsv',evidence_fields,m.controls)
    cf=['op','surface','calls','refusals','errors','fallbacks','fallback_rate','retries','repaired'];tsv(out/'retry_evidence.tsv',['turn_id','op','surface','retry','repaired','files','bucket'],[dict(turn_id=r['turn_id'],op=r['op'],surface=r['surface'],retry=r['_retry'],repaired=r['_repaired'],files=r['_files'],bucket=r['_bucket']) for r in rows])
    friction=['# Frequent friction patterns','', 'Ranked by call count; timeout-only patterns enter only when they also have fallback. Examples are literal retained excerpts with paths redacted; coverage is a keyword lead, not proof that an entry fixes the case.','']
    ranked=sorted(patterns.items(),key=lambda kv:(-len(kv[1]),kv[0]))[:20]
    for rank,(k,v) in enumerate(ranked,1):
        friction += [f'## {rank}. `{k[0]}` / {k[1]} / {k[2]}',f'\nCount: {len(v)}\n',f'covers: {covers(v,entries)}','']
        for j,r in enumerate(v[:2],1):
            friction += [f'Example {j}: {r["store"]}, `{r["session"]}`, `{r["turn_id"]}`, {r["date"]}', '', '```text',r['input_shape'],r['output_head'] if r['outcome']=='refusal' else r['_next'],'```','']
        if len(v)<2:friction+=['Only one retained example exists for this pattern.','']
    (out/'friction_top20.md').write_text('\n'.join(friction))
    classes=['rename-namespace','add-require-across-files','move-var','extract-function','arity-change','alias-migration','other-multi-file'];cand=[]
    for k in classes:
        v=[r for r in m.native if r['class']==k];ma=med([r['native_actions'] for r in v]);cand.append(dict(classification=k,occurrences=len(v),median_native_actions=ma,median_files_touched=med([r['files'] for r in v]),clears_bar='yes' if ma!='' and ma>=5 else 'no'))
    cfields=['classification','occurrences','median_native_actions','median_files_touched','clears_bar'];tsv(out/'candidates.tsv',cfields,cand)
    rules='''Population: turns with no detected Surgeon call, at least two native edit actions and at least two distinct explicitly touched Clojure/EDN paths, plus single-file extract-function candidates meeting the same action threshold. Counts are occurrences of heuristic evidence, not confirmed semantic refactorings. Exactly one class is assigned, in the order below.

1. rename-namespace: changed `(ns NAME)` tokens plus an old namespace/reference token in removed text, with at least two files.
2. add-require-across-files: added `:require` or `[lib :as alias]` text in edit actions covering at least three paths.
3. move-var: the same defn name removed without replacement in one edit and added in a disjoint file edit.
4. extract-function: combined edits to one file have nonempty removed text, a new defn name, and a call to that name in added text.
5. arity-change: a direct defn signature vector changes and at least two paths are edited.
6. alias-migration: at least one qualified-symbol prefix disappears and another appears, with at least two paths.
7. other-multi-file: remaining eligible turns.

Weaknesses: these are lexical, not AST checks. Patch hunks retain their file ownership; macro expansion, implicit references and shell transformations remain unresolved. Comments/strings, docstrings, multi-arity definitions, macros and file creations can cause false positives or negatives. Signature changes may rename arguments without changing arity. Alias changes need not preserve the referenced Var. Shell paths are inferred only for recognized edit commands; dynamic filenames and opaque wrappers are missed. Paths in shell source strings can overcount files. Multi-file patches are one action. Repeated patches to one file are multiple actions. Single-file extraction is included; other single-file edits are not assigned a candidate class. The >=5 bar applies to median native edit actions, not all tools or wall time. A class clearing it is only a candidate for review.
'''
    (out/'candidates.md').write_text('# Native change candidates\n\n'+md_table(cfields,cand)+'\n'+rules)
    m.stats['surgeon_calls_found']=len(rows)
    for store in ('codex','claude'):
        for surf in ('cli','mcp'):m.stats[f'surgeon_calls_{store}_{surf}']=sum(r['store']==store and r['surface']==surf for r in rows)
    for k in ('success','refusal','error','timeout','unknown'):m.stats['outcome_'+k]=sum(r['outcome']==k for r in rows)
    m.stats['unclassified_outcomes']=m.stats['outcome_unknown'];m.stats['native_comparison_turns']=len(m.controls);m.stats['candidate_turns']=len(m.native)
    m.stats['runtime_seconds']=round(time.monotonic()-start,3)
    for k in ('dropped_secret_lines','malformed_lines','unreadable_sessions'):m.stats.setdefault(k,0)
    counts=[dict(metric=k,value=v) for k,v in sorted(m.stats.items())];tsv(out/'counts.tsv',['metric','value'],counts)
    tsv(out/'tool_names.tsv',['store','name','outer_calls'],[dict(store=s,name=n,outer_calls=c) for (s,n),c in sorted(m.names.items())])
    tsv(out/'turn_counts.tsv',['store','tool_bearing_turns'],[dict(store=s,tool_bearing_turns=c) for s,c in sorted(m.turn_counts.items())])
    method='''# Method

Single-process Python 3 standard-library miner. No transcript command is evaluated or executed. A deterministic empirical sampling pass precedes the full streaming scan (up to 21 evenly spaced files/store, at most 2,500 retained records/file). Files are streamed one line at a time; actions are retained for the current turn, and only condensed evidence survives thereafter. Counts below refer to the full scan, not the sample. JSONL files are physical sessions, including Claude subagent files; duplicated tool IDs within a file are skipped, not deduplicated across sessions. Active session files are not frozen: this is the snapshot reached during traversal, not a simultaneous corpus snapshot.

## Counts

'''+md_table(['metric','value'],counts)+'''
## Observed record shapes

Codex stores `session_meta.payload.cwd`; `turn_context.cwd` can update it. Calls and outputs are `response_item` records using `call_id`. `arguments` can be a JSON string, while custom calls use raw `input`. Most sampled calls use an `exec` JavaScript wrapper. Both direct call types and nested calls were observed; the static lexer extracts literal `tools.NAME(...)` calls, including shell and MCP calls, without executing JavaScript. Indirect calls, computed arguments and templates with interpolation remain unresolved. Such wrapper invocations are evidence of submitted calls, not proof that a conditional branch ran. Opaque wrappers count once as other; decoded literal nested calls count individually. Multi-call wrapper outputs cannot safely be attributed to individual calls and yield unknown outcomes. Hidden/not-printed results also remain unknown. `write_stdin` and `wait` count as actions and do not inherit Surgeon identity. When a literal cell_id/session_id can be joined to its initial returned continuation identifier, the final output replaces the initial pending output for outcome and output_head; joins are counted. No final result is inferred when the join is missing.

Codex `task_started` and its initial user message are coalesced. A subsequent user message after actions begins a turn when the prior task ended, lacks a task start, or already contained a user message. `task_complete`/`turn_aborted` mark the task ended, preserving late tool output association until the next boundary. Claude assistant `message.content` lists contain `tool_use`; user lists contain matching `tool_result`. User strings or lists with non-result blocks start turns. Non-message metadata, repeated API-request snapshots and event mirror messages do not create additional calls. Dropped secret lines can remove boundaries or outputs and therefore weaken sequence evidence.

Examples below preserve observed field/block shapes, with string values replaced by `<redacted>` (including content). Only one example per distinct sampled shape is retained.

'''
    for k,v in sorted(m.examples.items()):method+=f'### {k}\n\n```json\n{one(compact(v),5000)}\n```\n\n'
    method+='''## Detection and classifiers

Only executed-tool records enter the action stream. Surgeon MCP recognition tests tool names for clj-surgeon, clj_surgeon, namespace_split or require_change. CLI recognition lexes shell command segments, strips heredoc bodies and comments, and requires an executable token named clj-surgeon (possibly a path); ordinary prose and a path argument to cat/rg do not count. Operation is the token after `:op`, or ls for `:ls`; a bin/clj-surgeon invocation without these retains its first argument. Multiple explicit operations in one shell action yield multiple calls with the same action position. Invocation via arbitrary shell functions, dynamically built commands, bb -m launchers and substitutions inside quoted strings is outside this declared recognizer. Shell control structures/wrapper options can cause misses. Static JS branches may overcount; this is recorded as a limit, not silently called confirmed execution.

Native edits are apply_patch/Edit/MultiEdit/Write/NotebookEdit, or the SPEC-A shell edit patterns (including python heredocs, which may be read-only). Native reads use the named read tools or shell read commands. Verification recognizes SPEC-A command names and git diff/status. Edit takes precedence over verify, then read, then other for mixed shell actions. Next action is the immediately following decoded action; same/other is by operation. Fallback means any later native edit before the next Surgeon action in the same turn, even with intervening reads or tests. Retrying means immediate surgeon-same next action; repaired means that retry has positively classified success. A fallback is a sequence observation, not a causal judgment.

Output classification uses top-level JSON/EDN receipt fields and initial terminal summaries, never nested returned source. Precedence: explicit timeout; typed refusal or refused terminal summary; machine error/is_error/nonzero exit or explicit exception; explicit success/commit/read-complete or exit zero; unknown. Structured MCP content is unwrapped when available. Valid show-form/list-forms receipts are successful reads. A transport exit zero alone proves tool transport completion, not semantic correctness. Verification-incomplete receipts can therefore be success here but do not prove the user task completed. Embedded source strings and nested fixtures are excluded from status evidence. Mixed-command output can still hide the relevant receipt; this is an attribution limit. Codes are extracted from top-level refusal_code/refusal-type, code/kind/error-type fields and the observed `TOOL refused · CODE` summary; booleans/generic status words are rejected. Missing codes remain blank. No output or unattributable wrapper output is unknown. calls.tsv retains the first 300 characters, so classifications can rely on evidence beyond that prefix. No successful outcome is inferred solely from a missing error.

All original transcript lines are checked for the specified token patterns and quoted password/secret keys followed by a value BEFORE parsing, sampling, excerpting or writing. Entire matching lines are discarded. dropped_secret_lines counts the full scan only. Retained excerpts replace /home/forge with ~ and other home prefixes with <home>; tabs/newlines are flattened. input_shape recursively truncates every string value at 60 characters, compact-serializes, then caps at 300. These clipped strings can cut JSON syntax at the final cap as requested. Friction excerpts use retained literal text, except path redaction, flattening and truncation. Patterns with one occurrence cannot supply two independent examples and are marked accordingly.

## Controls, matching and limits

Native controls are all tool-bearing turns without detected Surgeon calls that explicitly edit at least one .clj/.cljc/.cljs/.edn file. File paths are extracted from Edit/Write arguments, apply_patch headers, and recognized shell edit commands; source files are never opened. Relative edited paths normalize against cwd lexically. Repository is cwd's last component, or the repository component after worktrees/.worktrees. This heuristic can split nested directories of one repo or conflate equal basenames. File buckets are 1, 2-3, 4-9 and 10+. Surgeon turns with no observed native Clojure edits are bucket 0: Surgeon-target paths are not assumed to be writes and cannot be matched to native controls by touched files.

The native block of actions_to_end.tsv groups by repo and file bucket. surgeon-matched rows use identical repo/bucket keys for observed native edits in Surgeon turns; global surgeon rows remain per op. This is stratification, not randomized matching or a causal estimate. Native measures whole-turn tools; Surgeon measures suffix tools from the call inclusive. Task intent, model, difficulty, date and success are unmatched. Missing/dynamic edit paths and unrecognized Surgeon calls can contaminate controls. Therefore medians cannot establish speedup, correctness, first-attempt performance or a wall-time win. p90 uses nearest rank. Calls counts Surgeon calls in Surgeon blocks and native turns in native blocks.

candidates.md explains mutually exclusive ordered lexical heuristics and weaknesses. native_turns.tsv contains every control turn, its class and action/file counts; candidates count rows with >=2 native edit actions and either >=2 files or a single-file extract-function classification. retry_evidence.tsv retains per-call retry/repair/file-bucket data in calls.tsv order. counts.tsv, tool_names.tsv and turn_counts.tsv make full-scan counts auditable. Weekly session counts are distinct basenames within a week/store/surface and should not be summed to obtain unique sessions. Papercut coverage reads the pinned git object (b2beec26) without modifying that repository, plus the specified beads file when available. Keyword coverage is advisory, uses diagnostic output/op/code only, and does not assert semantic equivalence. Missing beads or papercuts are recorded in counts.

Instrumentation repair: an initial audit caught returned Clojure source being mistaken for outcome evidence. The classifier was repaired to use top-level receipts/terminal summaries, regression-checked, and the complete corpus rerun; every table here comes from the replacement run alone. Secret scanning also checks escaped quoted keys.

No files are committed. The miner writes only its output directory. Subprocess use is restricted to one read-only git show after the scan; no parallelism. Runtime covers sampling, full scan and table generation through count finalization. It excludes the final METHOD write and console output.
'''
    (out/'METHOD.md').write_text(safe(method))
    return counts


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--codex',type=pathlib.Path,default=HOME/'.codex/sessions')
    parser.add_argument('--claude',type=pathlib.Path,default=HOME/'.claude/projects')
    parser.add_argument('--out',type=pathlib.Path,default=pathlib.Path('docs/study-2026-11-22'))
    args=parser.parse_args();start=time.monotonic();args.out.mkdir(parents=True,exist_ok=True)
    files={'codex':sorted(args.codex.expanduser().glob('**/rollout-*.jsonl')),'claude':sorted(args.claude.expanduser().glob('**/*.jsonl'))}
    m=Miner(args.out);m.sampling(files)
    for store,paths in files.items():
        for path in paths:m.scan(path,store)
    entries=coverage_sources(m.stats);counts=write_outputs(m,entries,start,files)
    print('METHOD counts')
    for r in counts:print(f'{r["metric"]}: {r["value"]}')
    print('Files written:')
    for p in sorted(args.out.rglob('*')):
        if p.is_file():print(safe(p))


if __name__=='__main__':main()
