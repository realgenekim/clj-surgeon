// Prototype: validated archived specimens -> documentation. No request execution.
const fs = require('fs'), crypto = require('crypto'), util = require('util');
const hash = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
const need = (ok, reason) => { if (!ok) throw Error(reason); };
function specimen(entry) {
  const raw = fs.readFileSync(entry.request), receipt = fs.readFileSync(entry.receipt);
  const q = JSON.parse(raw), record = JSON.parse(receipt);
  need(util.isDeepStrictEqual(q, record.request), 'request-receipt-mismatch');
  const envelope = record.response?.result, s = envelope?.structuredContent;
  need(envelope?.isError !== true && s?.ok === true, 'receipt-not-success');
  if (entry.kind === 'inspect') {
    need(Array.isArray(q.requests) && q.requests.every(x =>
      ['outline','match'].includes(x.operation) && typeof x.file === 'string'), 'unsupported-inspect-shape');
    const files = new Set(q.requests.map(x => x.file)).size;
    need(q.expect?.requests === q.requests.length && q.expect?.files === files, 'request-cardinality-mismatch');
    need(s.read_complete === true && s.request_count === q.requests.length && s.file_count === files, 'receipt-cardinality-mismatch');
  } else if (entry.kind === 'fanout') {
    need(Array.isArray(q.edits) && q.edits.every(x => typeof x.file === 'string' &&
      Number.isInteger(x.matches) && x.matches > 0), 'unsupported-fanout-shape');
    need(s.committed === true && s.files === new Set(q.edits.map(x => x.file)).size &&
      s.edits === q.edits.reduce((n,x) => n+x.matches,0), 'receipt-cardinality-mismatch');
  } else if (entry.kind === 'alias') {
    need(q.op === 'alias_migration' && Number.isInteger(q.expect?.files), 'unsupported-alias-shape');
    need(s.committed === true && s.files === q.expect.files, 'receipt-cardinality-mismatch');
  } else throw Error('unsupported-kind');
  return '## '+entry.kind+'\n\n```json\n'+raw.toString()+'```\n\n'+
    'Archived successful execution; request SHA256 '+hash(raw)+'; receipt SHA256 '+hash(receipt)+'.\n'+
    'This validates the retained specimen, not current source freshness, replay safety, or task semantics.\n';
}
try {
  const entries = JSON.parse(fs.readFileSync(process.argv[2]));
  // Validate all specimens before emitting any partial document.
  process.stdout.write(entries.map(specimen).join('\n'));
} catch (e) { process.stderr.write('REFUSED '+e.message+'\n'); process.exitCode=2; }
