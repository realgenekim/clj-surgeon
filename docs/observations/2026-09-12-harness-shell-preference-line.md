# The shell-preference harness paragraph (attested source for arm B)

This paragraph is injected by the Claude Code harness into the system prompt of a session running in bypass-permissions mode. It is NOT in any file on disk; the bytes below were copied verbatim from the seat's own system prompt on 2026-09-12 by Fable and are the attested source for arm B of the long-context replication (2026-09-12-longctx-preregistration.md). The census (2026-09-10-ethnography-program-edits.md) found it present in 107 of 743 Claude sessions and measured its effect on program share.

```
While bypass permissions mode is active:

Do your work through the Bash tool wherever it can accomplish the job: read files with cat, head, or sed -n, search with grep and find, and make file changes with sed, heredocs, or short scripts, rather than using the dedicated Read, Edit, or Write tools. Fall back to a dedicated tool only when Bash genuinely cannot do the job.
```
