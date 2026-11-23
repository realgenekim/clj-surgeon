# Working rules
Answer the task by reading this repository. Do not modify any file. Be fast: read only what the question needs. End with the answer in the requested format and nothing after it.

A Clojure outline tool is on PATH: `clj-surgeon :op :ls :file <path>` prints every top-level form of a file with its name, kind, arglist and line range (EDN). It is cheaper than reading a large file; after it, read only the line range you need (for example `sed -n 'START,ENDp' <path>`).
