#!/usr/bin/env python3
"""Independent row-3 O2/O4/O5 grader. Reads seed/candidate/golden bytes only.

Provenance: Sol row-3 preregistration, Marvin 9f9cf614, parent d170f3d5.
Does not import Surgeon, consume its plans, or normalize inherited source.
"""
import argparse
import json
import re
from pathlib import Path

LIB = b'marvin-voice-remote.json'
ALIAS = b'mjson'
FILES = [f'src/marvin_voice_remote/{name}.clj' for name in (
    'bridge3_new', 'capture_archive', 'channel', 'codex_app_server',
    'director_control', 'director_outbox', 'groq', 'reducer_session')]


def forms(data):
    """Small independent lexical tree: offsets refer to original UTF-8 bytes."""
    def read(i):
        start = i
        c = data[i:i+1]
        if c in (b'(', b'[', b'{'):
            close = {b'(': b')', b'[': b']', b'{': b'}'}[c]
            children = []
            i += 1
            while i < len(data):
                if data[i:i+1] == close:
                    return (start, i+1, c, children), i+1
                if data[i:i+1] in (b')', b']', b'}'):
                    raise ValueError('mismatched delimiter')
                node, i = read(i)
                if node is not None:
                    children.append(node)
            raise ValueError('unclosed delimiter')
        if c in b' \t\r\n,':
            return None, i+1
        if c == b';':
            end = data.find(b'\n', i)
            return None, len(data) if end < 0 else end+1
        if c == b'"':
            i += 1
            while i < len(data):
                if data[i:i+1] == b'\\':
                    i += 2
                elif data[i:i+1] == b'"':
                    return (start, i+1, b'string', []), i+1
                else:
                    i += 1
            raise ValueError('unclosed string')
        if c == b'\\':
            # Character literal; consume a delimiter character as its value.
            i += 2
        else:
            i += 1
        while i < len(data) and data[i:i+1] not in b' \t\r\n,()[]{};"':
            i += 1
        return (start, i, b'token', []), i
    result, i = [], 0
    while i < len(data):
        node, i = read(i)
        if node:
            result.append(node)
            # Row-3 only needs the first (ns ...) form. Body bytes stay opaque.
            if node[2] == b'(' and node[3] and data[node[3][0][0]:node[3][0][1]] == b'ns':
                break
    return result


def entries(data):
    top = forms(data)
    namespaces = [n for n in top if n[2] == b'(' and n[3] and data[n[3][0][0]:n[3][0][1]] == b'ns']
    if len(namespaces) != 1:
        raise ValueError('namespace is not unique')
    clauses = [n for n in namespaces[0][3] if n[2] == b'(' and n[3] and data[n[3][0][0]:n[3][0][1]] == b':require']
    if len(clauses) != 1:
        raise ValueError('direct require is not unique')
    result = []
    for node in clauses[0][3][1:]:
        if node[2] != b'[':
            raise ValueError('unsupported direct libspec')
        text = data[node[0]:node[1]]
        match = re.fullmatch(rb'\[([^\s\[\]]+)(?:\s+:as\s+([^\s\[\]]+))?\]', text)
        if not match:
            raise ValueError('unsupported direct libspec options')
        result.append({'start': node[0], 'end': node[1], 'lib': match[1], 'alias': match[2]})
    return result


def line_start(data, pos):
    return data.rfind(b'\n', 0, pos) + 1


def line_end(data, pos):
    i = data.find(b'\n', pos)
    return len(data) if i < 0 else i+1


def attached_start(data, pos):
    start = line_start(data, pos)
    while start:
        previous = line_start(data, start-1)
        if not data[previous:start].lstrip(b' \t').startswith(b';'):
            break
        start = previous
    return start


def without_target(data, allow_hanging=False):
    targets = [e for e in entries(data) if e['lib'] == LIB]
    if len(targets) != 1 or targets[0]['alias'] != ALIAS:
        raise ValueError('expected exactly one target under mjson')
    target = targets[0]
    start, end = line_start(data, target['start']), line_end(data, target['end'])
    prefix, suffix = data[start:target['start']], data[target['end']:end]
    if prefix.strip():
        if not allow_hanging or prefix.strip() != b'(:require' or suffix not in (b'\n', b'\r\n'):
            raise ValueError('target insertion is not a standalone line')
        # Explicitly surfaced historical capture_archive opener exception.
        return data[:target['start']].rstrip(b' \t') + data[target['end']:]
    if suffix not in (b'\n', b'\r\n'):
        raise ValueError('target line owns other bytes')
    return data[:start] + data[end:]


# @spec REQUIRE-CHANGE-014
def grade_bytes(seed, candidate, golden):
    findings = []
    try:
        inherited = entries(seed)
        if any(e['lib'] == LIB for e in inherited):
            findings.append('seed already contains target')
        if without_target(candidate) != seed:
            findings.append('O4 protected bytes differ after deleting target line')
        if without_target(golden, allow_hanging=True) != seed:
            findings.append('O2 historical non-target projection differs')
        target = next(e for e in entries(candidate) if e['lib'] == LIB)
        before = next((e for e in inherited if e['lib'] > LIB), None)
        nearest = before or inherited[-1]
        anchor = attached_start(seed, before['start']) if before else line_end(seed, nearest['end'])
        prefix = seed[line_start(seed, nearest['start']):nearest['start']]
        indent = prefix if not prefix.strip() else b' ' * len(prefix)
        eol = b'\r\n' if seed[nearest['end']:line_end(seed, nearest['end'])].endswith(b'\r\n') else b'\n'
        expected = seed[:anchor] + indent + b'[' + LIB + b' :as ' + ALIAS + b']' + eol + seed[anchor:]
        if candidate != expected:
            findings.append('O5 stable insertion / comment attachment / indent / ending mismatch')
        if not any(e['lib'] == b'clojure.data.json' and e['alias'] == b'json' for e in entries(candidate)):
            findings.append('O2 occupied json binding was not preserved')
        if target['alias'] != ALIAS:
            findings.append('O2 alias policy mismatch')
    except (ValueError, StopIteration, IndexError) as error:
        findings.append(str(error) or 'missing target')
    return findings


# @spec REQUIRE-CHANGE-014
def grade_tree(seed_root, candidate_root, golden_root):
    findings = []
    changed = []
    seed_paths = [p for p in seed_root.rglob('*') if p.is_file() and '.git' not in p.relative_to(seed_root).parts]
    for path in seed_paths:
        file = path.relative_to(seed_root).as_posix()
        other = candidate_root / file
        if not other.is_file():
            findings.append({'file': file, 'finding': 'missing inherited path'})
            continue
        seed, candidate = path.read_bytes(), other.read_bytes()
        if seed != candidate:
            changed.append(file)
        if file not in FILES and seed != candidate:
            findings.append({'file': file, 'finding': 'outside declared scope'})
        if file in FILES:
            findings.extend({'file': file, 'finding': finding}
                            for finding in grade_bytes(seed, candidate, (golden_root/file).read_bytes()))
    if set(changed) != set(FILES):
        findings.append({'finding': 'changed path set differs', 'actual': sorted(changed)})
    return {'ok': not findings, 'files': len(changed), 'papercuts': len(findings), 'findings': findings}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--seed', type=Path, required=True)
    ap.add_argument('--candidate', type=Path, required=True)
    ap.add_argument('--golden', type=Path, required=True)
    args = ap.parse_args()
    result = grade_tree(args.seed, args.candidate, args.golden)
    print(json.dumps(result, sort_keys=True))
    print(f"O2 GOLDEN: {'PASS' if result['ok'] else 'FAIL'}")
    print(f"O4 ZERO-CHURN: {'PASS' if result['ok'] else 'FAIL'}")
    print(f"O5 REQUIRE-BLOCK PAPERCUTS: {result['papercuts']}")
    return 0 if result['ok'] else 1


if __name__ == '__main__':
    raise SystemExit(main())
