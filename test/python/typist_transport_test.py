import importlib.util,io,json,pathlib,socket,unittest,urllib.error
from unittest.mock import patch
P=pathlib.Path(__file__).resolve().parents[2]/'bin/typist_transport.py'
s=importlib.util.spec_from_file_location('transport',P);t=importlib.util.module_from_spec(s);s.loader.exec_module(t)
class TransportTests(unittest.TestCase):
 def cfg(self,**kw):return dict(route='openrouter-cerebras',prompt='edit it',candidates=1,max_tokens=20,timeout_s=2,**kw)
 def response(self,**kw):
  r={'model':t.MODEL,'provider':'Cerebras','choices':[{'message':{'content':'candidate'},'finish_reason':'stop'}],'usage':{'completion_tokens_details':{'reasoning_tokens':3}}};r.update(kw);return r
 def test_pin(self):
  c=t.validate(self.cfg());p=t.payload(c);self.assertEqual(p['provider'],{'order':['Cerebras'],'allow_fallbacks':False});self.assertEqual(p['model'],t.MODEL)
 def test_success_usage(self):
  r=t.candidate(self.response(),'openrouter-cerebras');self.assertTrue(r['usable']);self.assertEqual(r['reasoning_tokens'],3)
 def test_identity(self):
  for kw in [{'model':'other'},{'provider':'Groq'},{'provider':None}]:self.assertFalse(t.candidate(self.response(**kw),'openrouter-cerebras')['usable'])
 def test_groq(self):
  r=self.response();r.pop('provider');self.assertTrue(t.candidate(r,'groq')['usable']);self.assertFalse(t.candidate(self.response(),'groq')['usable'])
 def test_terminal_refusals(self):
  for finish in ['length','content_filter',None]:
   r=self.response();r['choices'][0]['finish_reason']=finish;self.assertFalse(t.candidate(r,'openrouter-cerebras')['usable'])
  r=self.response();r['choices'][0]['message']['refusal']='private text';self.assertNotIn('private text',json.dumps(t.candidate(r,'openrouter-cerebras')))
 def test_empty(self):
  r=self.response();r['choices'][0]['message']['content']=' ';self.assertFalse(t.candidate(r,'openrouter-cerebras')['usable'])
 def test_caps(self):
  for key,val in [('candidates',6),('max_tokens',8193),('timeout_s',121),('prompt','x'*t.MAX_REQUEST),('model','other')]:
   c=self.cfg();c[key]=val
   with self.assertRaises(t.Refusal):t.validate(c)
 def test_key_subset(self):
  self.assertEqual(t.parse_key(b'{:key "secret"}'),'secret');self.assertEqual(t.parse_key(b'{:openrouter-api-key "secret"}'),'secret')
  for x in [b'#=(evil)',b'{:key "a" :key "b"}',b'{:key 3}',b'{:key "a\\n"}']:
   with self.assertRaises(t.Refusal):t.parse_key(x)
 def test_transport_redaction(self):
  def fail(*a):raise RuntimeError('secret-key-body')
  r=t.attempt(t.validate(self.cfg()),'secret-key-body',fail);self.assertNotIn('secret-key-body',json.dumps(r));self.assertFalse(r['usable'])
 def test_typed_error_and_escaped_key_redaction(self):
  def fail(*a):raise t.Refusal('secret-key-body')
  self.assertNotIn('secret-key-body',json.dumps(t.attempt(t.validate(self.cfg()),'secret-key-body',fail)))
  raw=json.dumps(self.response()).replace('candidate',r'\u0073ecret').encode()
  self.assertEqual(t.attempt(t.validate(self.cfg()),'secret',lambda *a:raw)['error_type'],'secret-in-response')
 def test_timeout(self):
  def fail(*a):raise socket.timeout('sensitive')
  self.assertEqual(t.attempt(t.validate(self.cfg()),'secret',fail)['error_type'],'timeout')
 def test_response_cap(self):
  r=t.attempt(t.validate(self.cfg()),'secret',lambda *a:b'x'*(t.MAX_RESPONSE+1));self.assertEqual(r['error_type'],'response-too-large')
 def test_key_echo(self):
  r=self.response();r['choices'][0]['message']['content']='leakedsecret';out=t.attempt(t.validate(self.cfg()),'leakedsecret',lambda *a:json.dumps(r).encode());self.assertNotIn('leakedsecret',json.dumps(out));self.assertFalse(out['usable'])
class CostTests(unittest.TestCase):
 cfg=TransportTests.cfg
 response=TransportTests.response
 def test_usage_requested_only_openrouter(self):
  c=t.validate(self.cfg());self.assertEqual(t.payload(c)['usage'],{'include':True})
  c['route']='groq';self.assertNotIn('usage',t.payload(c))
 def test_provider_cost_values(self):
  for value in [0,0.0023,1]:
   r=t.candidate(self.response(usage={'cost':value}),'openrouter-cerebras')
   self.assertEqual(r['cost_usd'],value);self.assertEqual(r['cost_source'],'provider-reported')
  for value in [None,-1,True,False,'0.002',float('inf'),float('-inf'),float('nan')]:
   r=t.candidate(self.response(usage={'cost':value}),'openrouter-cerebras')
   self.assertIsNone(r['cost_usd']);self.assertIsNone(r['cost_source'])
  self.assertIsNone(t.candidate(self.response(),'openrouter-cerebras')['cost_usd'])
 def test_length_and_refusal_retain_cost(self):
  for finish,refusal in [('length',None),('stop','private refusal')]:
   raw=self.response(usage={'cost':0.004,'completion_tokens':7})
   raw['choices'][0]['finish_reason']=finish;raw['choices'][0]['message']['refusal']=refusal
   r=t.candidate(raw,'openrouter-cerebras');self.assertFalse(r['usable'])
   self.assertEqual(r['cost_usd'],0.004);self.assertEqual(r['completion_tokens'],7)
 def test_groq_and_transport_failure_unknown(self):
  self.assertIsNone(t.candidate(self.response(provider='Groq'),'groq')['cost_usd'])
  self.assertIsNone(t.failure('timeout')['cost_usd'])
class FallbackTests(unittest.TestCase):
 cfg=TransportTests.cfg
 response=TransportTests.response
 def configured(self):
  return t.validate(self.cfg(fallback={'provider':'groq','max_tokens':10}))
 def exercise(self,c,outcomes,clock=lambda:0):
  calls=[];keys=[]
  def send(route,key,body,timeout):
   calls.append((route,json.loads(body),timeout))
   value=outcomes[len(calls)-1]
   if isinstance(value,Exception):raise value
   return json.dumps(value).encode()
  def load(route):keys.append(route);return 'private-'+route
  return t.run_candidate(c,send,load,clock),calls,keys
 def http(self,status):return urllib.error.HTTPError('private-url',status,'private-body',{},None)
 def test_opt_in_validation(self):
  self.configured()
  for update in [{'fallback':True},{'fallback':{'provider':'other','max_tokens':1}},
                 {'fallback':{'provider':'groq','max_tokens':False}},
                 {'fallback':{'provider':'groq','max_tokens':0}},
                 {'fallback':{'provider':'groq','max_tokens':8190}},
                 {'fallback':{'provider':'groq','max_tokens':1,'extra':1}},
                 {'route':'groq'},{'candidates':2}]:
   c=self.cfg(fallback={'provider':'groq','max_tokens':10});c.update(update)
   with self.assertRaises(t.Refusal):t.validate(c)
 def test_explicit_http_fallback(self):
  for status,kind in [(429,'provider-rate-limited'),(503,'provider-unavailable')]:
   r,calls,keys=self.exercise(self.configured(),[self.http(status),self.response(provider='Groq')])
   self.assertTrue(r['usable']);self.assertTrue(r['fallback']);self.assertEqual(len(r['attempts']),2)
   self.assertEqual(r['attempts'][0]['error_type'],kind);self.assertEqual(r['attempts'][0]['http_status'],status)
   self.assertNotIn('model',r['attempts'][0]);self.assertEqual(r['attempts'][1]['model'],t.MODEL)
   self.assertEqual(keys,['openrouter-cerebras','groq'])
   self.assertEqual([x[1]['max_tokens'] for x in calls],[20,10])
   self.assertNotIn('provider',calls[1][1]);self.assertNotIn('private-',json.dumps(r))
 def test_no_opt_in_and_primary_success(self):
  for c,first in [(t.validate(self.cfg()),self.http(429)),(self.configured(),self.response())]:
   r,calls,keys=self.exercise(c,[first]);self.assertEqual(len(calls),1);self.assertFalse(r['fallback'])
 def test_ineligible_failures_do_not_fallback(self):
  length=self.response();length['choices'][0]['finish_reason']='length'
  refusal=self.response();refusal['choices'][0]['message']['refusal']='no'
  empty=self.response();empty['choices'][0]['message']['content']=''
  for first in [self.http(400),self.http(401),self.http(403),self.http(500),self.http(502),
                socket.timeout('secret'),RuntimeError('secret'),self.response(model='other'),
                self.response(provider='other'),{},length,refusal,empty,{'error':{'code':429}}]:
   r,calls,keys=self.exercise(self.configured(),[first]);self.assertFalse(r['usable']);self.assertEqual(len(calls),1)
 def test_second_attempt_never_retries(self):
  for second in [self.http(429),self.response(model='wrong'),self.response(provider='other')]:
   r,calls,_=self.exercise(self.configured(),[self.http(503),second]);self.assertFalse(r['usable']);self.assertEqual(len(calls),2)
 def test_shared_deadline(self):
  times=iter([0,0,0,0,3])
  r,calls,_=self.exercise(self.configured(),[self.http(429)],lambda:next(times,3))
  self.assertEqual(len(calls),1);self.assertEqual(r['fallback_skipped'],'deadline-exhausted')
 def test_remaining_deadline_and_key_failure(self):
  now=[0];calls=[]
  def send(route,key,body,timeout):
   calls.append(timeout)
   if len(calls)==1:now[0]=1;raise self.http(429)
   return json.dumps(self.response(provider='Groq')).encode()
  r=t.run_candidate(self.configured(),send,lambda _: 'fake-key',lambda:now[0])
  self.assertTrue(r['usable']);self.assertEqual(calls,[2,1])
  def badkey(route):
   if route=='groq':raise t.Refusal('key-unavailable')
   return 'fake-key'
  calls.clear();now[0]=0
  r=t.run_candidate(self.configured(),send,badkey,lambda:now[0])
  self.assertFalse(r['usable']);self.assertEqual(r['error_type'],'key-unavailable');self.assertEqual(len(calls),1);self.assertFalse(r['attempts'][1]['request_started'])
 def test_cross_attempt_key_redaction(self):
  r,_,_=self.exercise(self.configured(),[self.http(429),self.response(provider='Groq',choices=[{'message':{'content':'private-openrouter-cerebras'},'finish_reason':'stop'}])])
  self.assertEqual(r['error_type'],'secret-in-response');self.assertNotIn('private-openrouter',json.dumps(r))
 def test_completed_after_deadline_is_not_usable(self):
  now=[0]
  def send(*_):now[0]=3;return json.dumps(self.response()).encode()
  r=t.run_candidate(self.configured(),send,lambda _: 'fake-key',lambda:now[0])
  self.assertFalse(r['usable']);self.assertEqual(r['error_type'],'timeout');self.assertEqual(len(r['attempts']),1)
 def test_cli_receipt_and_single_alarm(self):
  c=self.configured();out=io.StringIO();calls=[]
  def send(*_):
   calls.append(1)
   if len(calls)==1:raise self.http(429)
   return json.dumps(self.response(provider='Groq')).encode()
  run=t.run_candidate
  with patch.object(t.sys,'stdin',io.TextIOWrapper(io.BytesIO(json.dumps(c).encode()))),patch.object(t.sys,'stdout',out),patch.object(t.signal,'signal'),patch.object(t.signal,'setitimer') as timer,patch.object(t,'run_candidate',lambda c:run(c,send,lambda _: 'fake-key',lambda:0)):
   self.assertEqual(t.main(),0)
  r=json.loads(out.getvalue());self.assertEqual(len(r['candidates'][0]['attempts']),2)
  self.assertTrue(r['fallback']);self.assertEqual(r['fallback_policy'],c['fallback'])
  self.assertEqual([call.args[1] for call in timer.call_args_list],[2,0])
 def test_cli_refusal_is_nonzero_without_dispatch(self):
  c=self.cfg(fallback={'provider':'groq','max_tokens':8192});out=io.StringIO()
  with patch.object(t.sys,'stdin',io.TextIOWrapper(io.BytesIO(json.dumps(c).encode()))),patch.object(t.sys,'stdout',out),patch.object(t,'run_candidate') as run:
   self.assertEqual(t.main(),2);run.assert_not_called()
  self.assertEqual(json.loads(out.getvalue())['error_type'],'invalid-fallback')
if __name__=='__main__':unittest.main()
