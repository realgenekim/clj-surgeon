import pathlib
import sys

if len(sys.argv) != 3:
    print('Usage: cc-oracle-replay.py <scratch> <manifest>', file=sys.stderr)
    sys.exit(2)

if sys.flags.optimize:
    print('Optimized Python disables oracle assertions; refusing replay.', file=sys.stderr)
    sys.exit(2)

oracle = pathlib.Path(__file__).with_name('cc-oracle.py')
source = oracle.read_text()
manifest_literal = "'/var/tmp/forge/plan2/cellC/cc-split.json'"
if source.count(manifest_literal) != 1:
    print('Expected exactly one frozen oracle manifest literal; refusing replay.', file=sys.stderr)
    sys.exit(2)
source = source.replace(manifest_literal, repr(sys.argv[2]))
# The frozen oracle resolves the tree it checks from sys.argv[1] (its original invocation passed the
# build directory there). Forward the scratch path explicitly as that first argument so the oracle
# checks the replayed workspace and nothing else; the manifest path travels through the literal above.
sys.argv = [str(oracle), sys.argv[1], sys.argv[2]]
exec(compile(source, str(oracle), 'exec'))
