import importlib.util,json,pathlib,socket,unittest
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
if __name__=='__main__':unittest.main()
