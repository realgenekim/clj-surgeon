#!/usr/bin/env python3
"""Offline, read-only repository inventory; generated artifacts stay beside this script."""
import collections, difflib, json, os, re, subprocess, sys, tempfile
from pathlib import Path
OUT = Path(__file__).resolve().parents[1]
ROOT = OUT.parents[2]
os.chdir(ROOT)
BASE = '08bf1d23859db426691e4018aa633edad91d6708'
TOOLS = Path.home() / '.local/state/pr-check/venv/bin'
def run(*args, **kw):
    return subprocess.run(args, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, **kw)
def git(*args):
    p = run('git', *args); p.check_returncode(); return p.stdout.strip()
def paths(ref, prefix=''):
    return git('ls-tree', '-r', '--name-only', ref, '--', *([prefix] if prefix else [])).splitlines()
def blob(ref, path):
    p = run('git', 'show', f'{ref}:{path}'); p.check_returncode(); return p.stdout
def write(name, text):
    (OUT / name).write_text(text + '\n')
def table(headers, rows):
    return '\n'.join('| ' + ' | '.join(map(str, row)) + ' |' for row in [headers, ['---'] * len(headers), *rows])
def clj(path):
    return bool(re.search(r'\.clj[^/]*$', path))
def lines(text):
    return len(text.splitlines())
def baseline():
    assert git('rev-parse', 'HEAD') == BASE, 'Run on the frozen baseline commit'
    assert not git('diff', BASE, '--', 'src', 'test', 'docs/intent'), 'Baseline inputs changed'
    versions, commands, controls = [], [], []
    with tempfile.TemporaryDirectory(prefix='.tools-', dir=OUT) as scratch:
        for tool in ('crapper', 'dryer'):
            meta = next((TOOLS.parent / 'lib').glob(f'python*/site-packages/{tool}-*.dist-info'))
            version = re.search(r'^Version: (.*)$', (meta / 'METADATA').read_text(), re.M)[1]
            versions.append(f'{tool} {version}: ' + (meta / 'direct_url.json').read_text())
            args = [str(TOOLS / tool), '--root', scratch, '--source-root', str(ROOT / 'src')]
            args += ['--use-existing-coverage'] if tool == 'crapper' else []
            commands.append(' '.join(args).replace(scratch, '"$scratch"'))
            reports = [run(*args, check=True) for _ in range(2)]
            write(tool + '.txt', reports[0].stdout.rstrip('\n').replace(str(ROOT), '<repo>'))  # no operator paths in the artifact
            write(tool + '.stderr.txt', reports[0].stderr.replace(scratch, '$scratch').rstrip('\n'))
            delta = ''.join(difflib.unified_diff(reports[0].stdout.splitlines(True), reports[1].stdout.splitlines(True)))
            (OUT / (tool + '.diff')).write_text(delta)
            assert not delta, f'{tool} same-commit stdout differs; see .diff'
            assert reports[0].stderr == reports[1].stderr, f'{tool} stderr differs'
            controls.append(f'{tool}: stdout diff empty (0 bytes); stderr byte-identical; no stripping in either control.')
    src = {p: (ROOT / p).read_text() for p in paths(BASE, 'src') if clj(p)}
    nsfiles = {m[1]: p for p, t in src.items() if (m := re.search(r'\(ns\s+([^\s()]+)', t))}
    scored = []
    for line in (OUT / 'crapper.txt').read_text().splitlines():
        m = re.match(r'^(.*?)\s+(\S+)\s+(\d+)\s+([\d.]+)%\s+([\d.]+)$', line)
        if m:
            name, ns, cc, cov, crap = m.groups(); scored.append((name, nsfiles.get(ns, ns), int(cc), crap))
    assert scored, 'Could not decode complexity report'
    pairs = re.findall(r'DUPLICATE score=(\S+)\n  (.*)\n  (.*)', (OUT / 'dryer.txt').read_text())
    pairs = [[s, a.replace(str(ROOT) + '/', ''), b.replace(str(ROOT) + '/', '')] for s, a, b in pairs]
    prefix = f'''# No-worse baseline: {BASE}

Rerun everything: `bash docs/quality/2026-11-22-baseline/bin/baseline.sh`. No network, coverage execution, source writes, commits, or checkouts. Python 3 stdlib drives the two pinned external tools.

Scope: `src/**/*.clj*`, {len(src)} tracked files, {sum(lines(t) for t in src.values())} physical lines (including blanks/comments). {len(scored)} functions scored; {len(pairs)} duplicate pairs. The entire `src` directory is passed explicitly; duplicate defaults: threshold 0.82, min-lines 4, min-nodes 20.

## Commands and versions

The script supplies an empty temporary `$scratch` under this artifact directory as metrics/coverage root, removes it on exit, and passes the absolute source path. Exact expanded commands (only scratch path parameterized):

```sh
{chr(10).join(commands)}
```

{chr(10).join(versions)}

`--use-existing-coverage` reads the empty scratch root: no coverage exists. Crapper assigns missing functions 0% and computes **CRAP = CC² + CC**. These are conservative assumed-zero scores, not measured test coverage or evidence that functions are untested. No test suite was run.

## Same-commit control

{chr(10).join(controls)} The `.diff` files retain the empty diffs; `.txt` files are raw stdout. Stderr is retained separately with only the temporary root replaced by `$scratch` for a stable artifact. Both invocations share one unchanged source snapshot and scratch root.

## Layout limits

The tools accept `.cljc`, but this snapshot has {sum(p.endswith('.cljc') for p in src)} such files under src, so reader-conditional correctness is untested. `bb.edn` is configuration, not a scored source file; scripts outside src and `test-fixtures/` are excluded by the requested scope. Crapper's default Clojure coverage launcher assumes `clj -M:cov --lcov`, which is not evidence of compatibility with this repository's Babashka tasks; it is bypassed. Dryer's top-level forms include non-functions. Structural fingerprints are candidates, not proof of equivalent behavior. No observed parse warnings are hidden; see stderr reports.

## 30 most complex functions

'''
    prefix = prefix.replace(str(ROOT), '<repo>').replace(str(Path.home()), '~')
    write('README.md', prefix + table(['Function', 'File', 'CC', 'CRAP (assumed 0% coverage)'], sorted(scored, key=lambda r: -r[2])[:30]) + '\n\n## 20 strongest duplicate pairs\n\n' + table(['Score', 'Left (inclusive lines)', 'Right (inclusive lines)'], pairs[:20]))
    intent_links()
    print(f'Baseline: {len(src)} source files; {sum(lines(t) for t in src.values())} lines; {len(scored)} functions; {len(pairs)} duplicate pairs')
def intent_links():
    idpat = r'\b[A-Z][A-Z0-9]*(?:-[A-Z0-9]+)+-\d{3}\b'
    tests = '\n'.join((ROOT / p).read_text(errors='replace') for p in paths(BASE, 'test'))
    linked = set(re.findall(idpat, tests)); rows, detail = [], []
    folders = sorted(p for p in (ROOT / 'docs/intent').iterdir() if p.is_dir())
    for folder in folders:
        ids = set()
        for p in sorted(folder.rglob('*')):
            if p.is_file():
                for line in p.read_text(errors='replace').splitlines():
                    if re.match(r'^\s*#', line) or re.match(r'^\s*-\s*\[[^]]*\]', line):
                        ids.update(re.findall(idpat, line.split('**:', 1)[0]))
                    elif (m := re.search(r'(?:@spec\s+|INTENT:\s*)((?:' + idpat + r'[ ,]*)+)', line)):
                        ids.update(re.findall(idpat, m[1]))
        missing = sorted(ids - linked)
        rows.append([folder.name, len(ids), len(ids & linked), ', '.join(missing) or '—'])
        detail.append(f'## {folder.name}\n\n' + (table(['Spec ID', 'Referenced in test/**'], [[i, 'yes' if i in linked else 'no'] for i in sorted(ids)]) if ids else 'No spec IDs defined by the scanned markers.'))
    write('intent-links.md', '# Intent link inventory\n\nBaseline ' + BASE + '. Read-only textual audit; a reference is not a passing behavioral witness. Definitions are IDs in headings, checkbox requirement definitions, @spec or INTENT: lines; duplicate IDs within each folder count once, including archived variants and deferred requirements. Test links include any whole bare ID in tracked test/** files (including fixtures); bench/** is outside this requested test scope. All IDs are listed individually below.\n\n' + table(['Folder', 'Specs', 'Linked', 'Unlinked IDs'], rows) + '\n\nAGENTS.md explicitly scopes the intent folders mcp-operation-contract and operation-algebra; the other 15 intent folders are outside that declared LID scope. Its source/test file globs and docs/high-level-design.md/Makefile are also scoped.\n\nCoherence checks exist: make runtests → mcp-test includes test/clj_surgeon/mcp_intent_contract_test.clj, exercising src/clj_surgeon/mcp_intent_contract.clj for unknown IDs and missing implementation/test witnesses. make performance-regression-sentinel-test runs test/performance_regression_sentinel_intent_test.sh, checking missing/unknown PERF-SENT IDs (also using bench tests). These are scoped checks, not a repository-wide check for all 17 folders. Search: rg -n -i "coherence|traceability|intent|LID" Makefile test resources bench; inspected the named gates, not executed them.\n\n' + '\n\n'.join(detail))
    print(f'Intent: {len(folders)} folders; {sum(r[1] for r in rows)} folder-local IDs; {sum(r[2] for r in rows)} linked; {sum(r[1]-r[2] for r in rows)} unlinked')
def branches():
    refs = [r.split()[0] for r in git('for-each-ref', '--format=%(refname:short) %(symref)', 'refs/remotes').splitlines() if len(r.split()) == 1]
    ahead = {r: int(git('rev-list', '--count', f'origin/main..{r}')) for r in refs}
    families = collections.defaultdict(list)
    for r in refs:
        name = r.removeprefix('origin/')
        if ahead[r] > 100 and r not in ('origin/astra/namespace-split', 'origin/MCP/main'):
            families[name.split('/')[0]].append(r)
    reps = {f: max(rs, key=lambda r: (ahead[r], r)) for f, rs in sorted(families.items())}
    selected = list(dict.fromkeys(['origin/main', 'origin/astra/namespace-split', 'origin/MCP/main', *reps.values()]))
    ops = 'ls cat xray alias_migration split-ns! apply_clojure_changes admit_clojure_patch edit_clojure helper_extraction inspect_clojure feature_thread require_change namespace_split insert-forms! rename-alias!'.split()
    rows, evidence, identities = [], [], []
    for ref in selected:
        files = paths(ref); source = {p: blob(ref, p) for p in files if p.startswith('src/') and clj(p)}
        tests = {p: blob(ref, p) for p in files if p.startswith('test/')}
        registry = {p: t for p, t in source.items() if re.search(r'/(core|.*registry|.*dispatch|mcp_.*)\.clj', p)}
        found = set()
        for p, t in registry.items():
            bindings = dict(re.findall(r'\(def\s+(?:\^:private\s+)?([\w-]+)\s+"([^"\n]+)"\)', t))
            for n, line in enumerate(t.splitlines(), 1):
                resolved = re.sub(r'(:name\s+)([\w-]+)\b', lambda m: m[1] + json.dumps(bindings.get(m[2], m[2])), line)
                for op in ops:
                    pattern = r':name\s+"' + re.escape(op) + r'"|:' + re.escape(op) + r'\s+\{|:aliases\s+\[[^]]*:' + re.escape(op) + r'(?=[\s\]])'
                    if re.search(pattern, resolved) and not line.lstrip().startswith(';'):
                        found.add(op); evidence.append(f'{ref}\t{op}\t{p}:{n}\t{line.strip()}')
        base = git('merge-base', 'origin/main', ref)
        rows.append([ref, sum(lines(t) for t in source.values()), sum(lines(t) for t in tests.values()), sum(len(re.findall(r'\(deftest\s', t)) for p, t in tests.items() if clj(p)), ', '.join(o for o in ops if o in found) or '—', base[:12] + ' ' + git('show', '-s', '--format=%cs', base), int(git('rev-list', '--count', f'origin/main..{ref}'))])
        identities.append(f'{ref}\t{git("rev-parse", ref)}')
    with tempfile.TemporaryDirectory(prefix='.merge-', dir=OUT) as scratch:
        objects = Path(scratch) / 'objects'; objects.mkdir()
        env = dict(os.environ, GIT_OBJECT_DIRECTORY=str(objects), GIT_ALTERNATE_OBJECT_DIRECTORIES=git('rev-parse', '--path-format=absolute', '--git-common-dir') + '/objects')
        merge = run('git', 'merge-tree', '--write-tree', 'origin/main', 'origin/astra/namespace-split', env=env)
        assert merge.returncode in (0, 1), merge.stderr
        write('merge-tree.txt', merge.stdout.rstrip() + '\n' + merge.stderr.rstrip())
    conflicts = sorted(set(re.findall(r'^\d+ [0-9a-f]+ [123]\t(.+)$', merge.stdout, re.M)))
    write('branch-operations.tsv', 'ref\toperation\tlocation\tregistry/dispatch text\n' + '\n'.join(evidence))
    write('branch-refs.tsv', 'ref\tsha\n' + '\n'.join(identities))
    text = '# Which branch is the tool?\n\nOffline local-ref snapshot; rerun `bash docs/quality/2026-11-22-baseline/bin/branches.sh`. No checkout/fetch/merge. Main means origin/main = ' + git('rev-parse', 'origin/main') + ' (the local main of this clone is ' + git('rev-parse', 'main') + ', the fork point of every large branch except the experiment family, which forks earlier; see the merge-base column). Full measured ref SHAs: [branch-refs.tsv](branch-refs.tsv).\n\nCounts use git ls-tree + git show: physical lines including blanks/comments in src/**/*.clj* and test/** (all tracked file types); deftest is textual `(deftest` count in test/**/*.clj*. Merge bases use git merge-base origin/main REF and git show -s --format=%cs; ahead uses git rev-list --count origin/main..REF. Operations are :name registrations (including same-file def string constants), CLI operation map keys or :aliases hits in core/registry/dispatch/MCP source; this is presence evidence, not a runtime or behavioral check. Full hit locations: [branch-operations.tsv](branch-operations.tsv). Only the 15 requested spellings are scanned; omitted spellings had no hits.\n\n' + table(['Ref', 'Src lines', 'Test lines', 'deftest', 'Operation spellings present', 'Merge base / date', 'Not on main'], rows)
    text += '\n\nRemote families with >100 commits not on main (prefix after origin/, before first /; slashless names are their own family; excludes the two explicitly named remote refs and symbolic HEAD). Ties use lexically greatest ref.\n\n' + table(['Family', 'Branches', 'Most-ahead representative', 'Commits'], [[f, len(families[f]), r, ahead[r]] for f, r in reps.items()])
    text += f'\n\n`git merge-tree --write-tree origin/main origin/astra/namespace-split`: exit {merge.returncode}; {len(conflicts)} conflicting files. ' + ('Clean merge is not possible.' if conflicts else 'No textual merge conflicts, because origin/main is only ' + git('rev-list', '--count', git('merge-base', 'origin/main', 'origin/astra/namespace-split') + '..origin/main') + ' commits (documentation) past the fork point; a merge would import the whole branch (' + run('git', 'diff', '--shortstat', 'origin/main', 'origin/astra/namespace-split').stdout.strip() + '), so taking the two winning verbs alone is a port, not a merge.') + ' Git object writes were redirected to temporary artifact-local storage; refs, index and worktree were untouched. This checks the entire branch, not a cherry-pick of just two features. Full output: [merge-tree.txt](merge-tree.txt). First 20 conflicting paths:\n\n' + ('\n'.join('- `' + p + '`' for p in conflicts[:20]) or 'None.')
    write('which-branch-is-the-tool.md', text)
    print(f'Branches: {len(refs)} remote refs; {len(reps)} families; {len(selected)} measured refs; {len(conflicts)} conflicting files')
if __name__ == '__main__':
    {'baseline': baseline, 'branches': branches}[sys.argv[1]]()
