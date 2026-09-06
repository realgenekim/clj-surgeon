#!/usr/bin/env python3
"""Bounded prototype provider transport: one trusted JSON request on stdin.

Input keys: route, prompt, candidates (1..5), max_tokens (1..8192),
timeout_s (1..120); all required, unknown fields refuse. The executor supplies
this trusted config and must close stdin / bound the complete process lifetime.
Keys never come from stdin/environment. Output candidates are untrusted text,
not applied edits. Batch is sequential and waits for all k attempts; no fallback.
"""
import json,math,os,re,signal,socket,stat,sys,time,urllib.error,urllib.request
from pathlib import Path

MODEL='openai/gpt-oss-120b'
MAX_REQUEST=262144
MAX_RESPONSE=1048576
ROUTES={
 'openrouter-cerebras':('https://openrouter.ai/api/v1/chat/completions','/home/forge/secrets/openrouter.edn','Cerebras'),
 'groq':('https://api.groq.com/openai/v1/chat/completions','/home/forge/secrets/groq.edn','Groq')}
class Refusal(Exception):pass

def validate(value):
 if not isinstance(value,dict) or set(value)!={'route','prompt','candidates','max_tokens','timeout_s'}:raise Refusal('invalid-config')
 if value['route'] not in ROUTES:raise Refusal('unsupported-route')
 for key,maximum in [('candidates',5),('max_tokens',8192),('timeout_s',120)]:
  if type(value[key]) is not int or not 1<=value[key]<=maximum:raise Refusal('invalid-budget')
 if not isinstance(value['prompt'],str) or not value['prompt'].strip():raise Refusal('empty-prompt')
 if len(json.dumps(payload(value),ensure_ascii=False).encode())>MAX_REQUEST:raise Refusal('request-too-large')
 return dict(value)

def payload(c):
 p={'model':MODEL,'messages':[{'role':'user','content':c['prompt']}],'max_tokens':c['max_tokens'],'stream':False}
 if c['route']=='openrouter-cerebras':
  p['provider']={'order':['Cerebras'],'allow_fallbacks':False}
  p['usage']={'include':True}
 return p

def parse_key(raw):
 # Deliberately only one EDN keyword/string pair; no general reader/evaluation.
 if len(raw)>16384:raise Refusal('invalid-key-file')
 try:
  m=re.fullmatch(r'\s*\{\s*:(?:openrouter-api-key|key)\s+("(?:[^"\\]|\\.)*")\s*\}\s*',raw.decode('utf-8'))
  key=json.loads(m[1]) if m else None
 except (ValueError,UnicodeError):raise Refusal('invalid-key-file') from None
 if not isinstance(key,str) or not key or any(ord(c)<33 or ord(c)>126 for c in key):raise Refusal('invalid-key-file')
 return key

def load_key(route):
 try:
  fd=os.open(ROUTES[route][1],os.O_RDONLY|os.O_NONBLOCK|os.O_NOFOLLOW)
  with os.fdopen(fd,'rb') as f:
   if not stat.S_ISREG(os.fstat(f.fileno()).st_mode):raise Refusal('invalid-key-file')
   return parse_key(f.read(16385))
 except OSError:raise Refusal('key-unavailable') from None

def failure(kind):return {'usable':False,'error_type':kind,'content':None,'finish_reason':None,'reasoning_tokens':None,'cost_usd':None,'cost_source':None}

def candidate(d,route):
 if not isinstance(d,dict):return failure('invalid-response')
 if d.get('error'):return failure('provider-error')
 if d.get('model')!=MODEL:return failure('model-mismatch')
 upstream=ROUTES[route][2]
 if (route=='openrouter-cerebras' and d.get('provider')!=upstream) or (route=='groq' and d.get('provider') not in [None,upstream]):return failure('upstream-mismatch')
 choices=d.get('choices')
 if not isinstance(choices,list) or len(choices)!=1 or not isinstance(choices[0],dict):return failure('invalid-choices')
 choice=choices[0];msg=choice.get('message');finish=choice.get('finish_reason')
 if not isinstance(msg,dict):return failure('invalid-message')
 usage=d.get('usage') if isinstance(d.get('usage'),dict) else {}
 details=usage.get('completion_tokens_details') if isinstance(usage.get('completion_tokens_details'),dict) else {}
 count=lambda v:v if type(v) is int and v>=0 else None
 cost=usage.get('cost')
 cost=cost if ((type(cost) is int and cost>=0) or (type(cost) is float and math.isfinite(cost) and cost>=0)) else None
 r={'usable':False,'error_type':None,'model':MODEL,'upstream':upstream,'upstream_evidence':'response-provider' if d.get('provider') else 'fixed-direct-endpoint','content':None,'finish_reason':finish if finish in ['stop','length','content_filter','tool_calls','function_call'] else None,'reasoning_tokens':count(details.get('reasoning_tokens')),'completion_tokens':count(usage.get('completion_tokens')),'prompt_tokens':count(usage.get('prompt_tokens'))}
 r.update(cost_usd=cost,cost_source='provider-reported' if cost is not None else None)
 if isinstance(msg.get('content'),str):r['content']=msg['content']
 if msg.get('refusal'):r['error_type']='provider-refusal'
 elif finish!='stop':r['error_type']='output-length' if finish=='length' else 'nonterminal-output'
 elif not isinstance(msg.get('content'),str) or not msg['content'].strip():r['error_type']='empty-content'
 else:r.update(usable=True,content=msg['content'])
 return r

class NoRedirect(urllib.request.HTTPRedirectHandler):
 def redirect_request(self,*args,**kwargs):raise Refusal('redirect-refused')

def request(route,key,body,timeout):
 # No environment proxy routing and no redirect that might forward credentials.
 opener=urllib.request.build_opener(urllib.request.ProxyHandler({}),NoRedirect())
 req=urllib.request.Request(ROUTES[route][0],body,headers={'Authorization':'Bearer '+key,'Content-Type':'application/json','User-Agent':'curl/8.5.0 forge-typist-transport'})
 with opener.open(req,timeout=timeout) as response:
  if response.geturl()!=ROUTES[route][0]:raise Refusal('redirect-refused')
  return response.read(MAX_RESPONSE+1)

def attempt(c,key,send=request):
 started=time.monotonic()
 try:
  raw=send(c['route'],key,json.dumps(payload(c),ensure_ascii=False).encode(),c['timeout_s'])
  if len(raw)>MAX_RESPONSE:raise Refusal('response-too-large')
  if key.encode() in raw:raise Refusal('secret-in-response')
  r=candidate(json.loads(raw),c['route'])
  if isinstance(r.get('content'),str) and key in r['content']:raise Refusal('secret-in-response')
 except (TimeoutError,socket.timeout):r=failure('timeout')
 except urllib.error.HTTPError:r=failure('http-error')
 except Refusal as e:r=failure(str(e) if str(e) in {'response-too-large','secret-in-response','redirect-refused'} else 'transport-refused')
 except Exception:r=failure('transport-or-response-error')
 r['request_wall_s']=time.monotonic()-started
 return r

def alarm(_signum,_frame):raise TimeoutError()

def main():
 try:
  raw=sys.stdin.buffer.read(MAX_REQUEST+1)
  if len(raw)>MAX_REQUEST:raise Refusal('request-too-large')
  c=validate(json.loads(raw));key=load_key(c['route']);results=[]
  # CLI is single-threaded. Wall deadline covers connection and body read,
  # unlike a socket timeout alone. Each attempt is bounded independently.
  previous=signal.signal(signal.SIGALRM,alarm)
  try:
   for i in range(c['candidates']):
    signal.setitimer(signal.ITIMER_REAL,c['timeout_s'])
    try:r=attempt(c,key)
    finally:signal.setitimer(signal.ITIMER_REAL,0)
    r['index']=i;results.append(r)
  finally:signal.signal(signal.SIGALRM,previous)
  print(json.dumps({'route':c['route'],'model':MODEL,'batch_mode':'sequential-waits-all','fallback':False,'candidates':results},ensure_ascii=False))
  return 0 if any(r['usable'] for r in results) else 2
 except Refusal as e:print(json.dumps(failure(str(e))));return 2
 except Exception:print(json.dumps(failure('invalid-input-or-runtime')));return 2
if __name__=='__main__':sys.exit(main())
