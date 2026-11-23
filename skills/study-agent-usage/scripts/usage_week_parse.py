"""Pure weekly usage relations; no transcript text is executed."""
import ast, itertools, json, re, shlex
SECRET = (r'ghp_[A-Za-z0-9]{20,}|github_pat_|sk-[A-Za-z0-9]{20,}|xox[abprs]-|-----BEGIN|'
          r'Bearer [A-Za-z0-9._-]{20,}|AKIA[0-9A-Z]{16}|["\x27](?:password|secret)["\x27]\s*[:=]\s*\S+')
FIELDS = ('calls', 'sessions', 'refusals', 'errors', 'unknown', 'fallbacks', 'undecodable')
SHELLS = ('exec_command', 'shell_command', 'Bash', 'shell')
def secret_line(line):
    hints = ('ghp_', 'github_pat_', 'sk-', 'xox', '-----begin', 'bearer ', 'akia', 'password', 'secret')
    lower = line.lower()
    if line.isascii() and not any(h in lower for h in hints): return False
    return bool(re.search(SECRET, line.replace('\\', ''), re.I))
def payload(record):
    r = record.get('payload') or record; a = r.get('arguments', r.get('input', {}))
    try: a = json.loads(a) if isinstance(a, str) else a
    except ValueError: pass
    return r, a
def text(a):
    if isinstance(a, dict): a = a.get('cmd', a.get('command', a.get('patch', a)))
    return a if isinstance(a, str) else json.dumps(a)
def command(a):
    value = a.get('cmd', a.get('command', '')) if isinstance(a, dict) else a
    return ' '.join(value) if isinstance(value, list) else text(value)
def shell_segments(cmd):
    lines, delimiters = [], []
    for line in cmd.splitlines():
        if delimiters:
            if line.strip() == delimiters[0]: delimiters.pop(0)
            continue
        lines.append(line)
        delimiters += re.findall(r'<<-?\s*["\x27]?([\w-]+)["\x27]?', line)
    lexer = shlex.shlex('\n'.join(lines), posix=True, punctuation_chars=';&|\n<>')
    lexer.whitespace_split = True; lexer.whitespace = ' \t\r'
    separators = lambda t: bool(re.fullmatch(r'[;&|\n]+', t))
    try: return [list(ts) for sep, ts in itertools.groupby(lexer, separators) if not sep]
    except ValueError: return [cmd.split()]
def actions(record):
    r, a = payload(record)
    if r.get('name', '').split('.')[-1] == 'exec' and isinstance(a, str):
        literal = r'"(?:\\.|[^"\\])*"|\x27(?:\\.|[^\x27\\])*\x27|`[^`]*`'
        pattern = (r'tools(?:\.([\w]+)|\[["\x27]([\w-]+)["\x27]\])\(\s*'
                   r'(\{(?:' + literal + r'|[^}"\x27`])*\}|' + literal + r'|[A-Za-z_$][\w$]*)')
        found = False
        for m in re.finditer(pattern, a):
            name, arg = m[1] or m[2], m[3]
            found = True
            try:
                if arg.startswith('{'):
                    prop = re.search(r'(?:^\{|,)\s*["\x27]?(?:cmd|command)["\x27]?\s*:\s*('
                                     + literal + ')', arg)
                    if name in SHELLS and not prop: raise ValueError('nonliteral command')
                    arg = prop[1] if prop else '{}'
                if re.fullmatch(r'[A-Za-z_$][\w$]*', arg):
                    if name != 'apply_patch': raise ValueError('nonliteral argument')
                    paths = re.findall(r'\*\*\* (?:Update|Add) File: ([^\n\r"\x27`\\]+)',
                                       a.replace('\\n', '\n'))
                    value = '\n'.join('*** Update File: ' + path.strip() for path in paths)
                else: value = arg[1:-1] if arg[0] == '`' else ast.literal_eval(arg)
            except (ValueError, SyntaxError):
                yield dict(record, undecodable=True)
                continue
            nested = dict(record, name=name, payload=None, arguments=value)
            yield from actions(nested)
        if not found: yield dict(record, undecodable=True)
    elif r.get('name', '').split('.')[-1] in SHELLS:
        cmd = command(a)
        try: shlex.split(cmd)
        except ValueError:
            yield dict(record, undecodable=True)
            return
        parts = [dict(record, name=r.get('name'), payload=None, output=r.get('output', ''),
                      arguments={'cmd': shlex.join(ts)})
                 for ts in shell_segments(cmd)] if 'clj-surgeon' in cmd else []
        yield from parts if any(detect_call(part) for part in parts) else [record]
    else: yield record
def detect_call(record):
    r, a = payload(record); name = r.get('name', '')
    op = name.split('__')[-1].split('.')[-1] if re.search(r'clj[-_]surgeon', name) else None
    if op: return dict(record, op=op, surface='mcp', output=r.get('output', ''))
    if name.split('.')[-1] not in SHELLS or 'clj-surgeon' not in command(a): return None
    for tokens in shell_segments(command(a)):
        while tokens and (re.match(r'^\w+=', tokens[0]) or tokens[0] in ('env', 'time', 'command', 'nohup')):
            tokens.pop(0)
        if tokens[:1] == ['timeout']:
            tokens = tokens[1:]
            while tokens and tokens[0].startswith('-'):
                tokens = tokens[2:] if tokens[0] in ('-k', '-s') else tokens[1:]
            tokens = tokens[1:]
        exe = tokens[0].rsplit('/', 1)[-1] if tokens else ''
        module = any(tokens[i:i+2] == ['-m', 'clj-surgeon.core'] for i in range(len(tokens)))
        if exe != 'clj-surgeon' and not (exe == 'bb' and module): continue
        op = tokens[tokens.index(':op')+1].lstrip(':') if ':op' in tokens[:-1] else ''
        if re.fullmatch(r'[a-zA-Z][\w!?-]*', op):
            return dict(record, op=op, surface='cli', output=r.get('output', ''))
def classify_outcome(output):
    s = (output if isinstance(output, str) else json.dumps(output, ensure_ascii=False)).replace('\\', '')
    value = r'["\s:]+(?!null\b|nil\b|false\b|true\b)'
    refusal = r'[:"](?:refusal_code|refusal-type)' + value + r'[\w-]+'
    typed = r'[:"]error[-_]type' + value + r'(?:stale|[a-z]+(?:-[a-z]+)+)(?=["\s},]|$)'
    status = r'"status"\s*:\s*"refused"|:refused\b|refused\s*·'
    if re.search(refusal + '|' + typed + '|' + status, s): return 'refusal'
    exit_code = r'exit(?:ed)?(?: with)?(?:[ _-]code)?["\s:]+'
    error = r'"is_?error"\s*:\s*true|^\s*(?:Error|Exception|Traceback)\b|"error"\s*:\s*"[^"\s]'
    if re.search(exit_code + r'-?[1-9]\d*|' + error + r'|:error\s+(?!nil\b|false\b)', s, re.I | re.M):
        return 'error'
    success = (exit_code + r'0\b|"success"\s*:\s*true\b|"status"\s*:\s*"success"'
               r'|read_complete["\s:]+true\b|:ok\s+true\b')
    if re.search(success, s, re.I): return 'success'
    return 'unknown'
def is_native_clj_edit(record):
    r, a = payload(record); name = r.get('name', '').split('.')[-1]; paths = []
    if name in ('Edit', 'Write', 'MultiEdit'): paths = [a.get('file_path', a.get('path', ''))]
    elif name == 'apply_patch': paths = re.findall(r'^\*\*\* (?:Update|Add) File: (.+)$', text(a), re.M)
    elif name in SHELLS and re.search(r'\b(?:sed|tee|cat|python[\d.]*)\b', command(a)):
        s = command(a)
        paths += re.findall(r'(?:open|Path)\(["\x27]([^"\x27]+)["\x27]\s*'
                            r'(?:,\s*["\x27][wa]["\x27]|\)\.write_(?:text|bytes)\()',
                            s if re.match(r'python[\d.]*\s', s) else '')
        for ts in shell_segments(s):
            if ts[:1] == ['sed'] and any(t.startswith('-i') for t in ts[1:]): paths += ts[3:]
            if ts[:1] == ['tee']: paths += [t for t in ts[1:] if not t.startswith('-')]
            if ts[:1] == ['cat']: paths += [ts[i+1] for i, t in enumerate(ts[:-1]) if t in ('>', '>>')]
    return any(re.search(r'\.(?:clj[cs]?|edn)$', path) and not
               re.search(r'\$|^/(?:private/)?tmp/|^/(?:private/)?var/folders/|'
                         r'^/var/tmp/forge/claude-|(?:^|/)scratchpad(?:/|$)', path)
               for path in paths)
def fold_turn(records):
    calls, edited = [], False
    for record in reversed(records):
        call = detect_call(record)
        if call:
            call['outcome'] = classify_outcome(call['output'])
            call['fallback'] = edited and call['outcome'] in ('refusal', 'error'); calls.append(call)
            if call['outcome'] == 'success': edited = False
        edited = edited or is_native_clj_edit(record)
    return list(reversed(calls))
def aggregate(calls):
    return {op: dict(calls=len(cs), sessions=len({c.get('session', '') for c in cs}),
                    fallbacks=sum(c['fallback'] for c in cs),
                    undecodable=sum(bool(c.get('undecodable')) for c in cs),
                    **{key: sum(c['outcome'] == outcome for c in cs)
                       for key, outcome in zip(FIELDS[2:5], ('refusal', 'error', 'unknown'))})
            for op in {c['op'] for c in calls} for cs in [[c for c in calls if c['op'] == op]]}
def render(aggregated, total_fields):
    rows = sorted(aggregated.items(), key=lambda item: (-item[1]['calls'], item[0]))
    lines = ['op=' + op + ' ' + ' '.join(f'{k}={v[k]}' for k in FIELDS) for op, v in rows]
    return '\n'.join(lines + [f"total={total_fields['calls']} " + ' '.join(f'{k}={total_fields[k]}'
                                                for k in (*FIELDS, 'files_scanned', 'seconds'))])
