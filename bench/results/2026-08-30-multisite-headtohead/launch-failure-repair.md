# Config-repair cohort: killed before model execution

The separately preregistered repair cohort at registration commit
`4c3b564178bb0f635dfe3a94106827c163fb90fe` is killed.

All 12 scheduled `codex exec` processes exited with code 1 during strict config
loading. Each process reported the same normalized error:

```text
unknown configuration field `tools.view_image`
```

The temporary `CODEX_HOME` path differs across raw stderr files. There were no
session events, tool-hook files, last-message files, provider-usage records, or
fixture mutations. Therefore, no executor-model call occurred and no outcome
information was observed.

`codex debug models` was not an adequate exec-path preflight. It loaded both
configs but did not validate the `tools` table that `codex exec --strict-config`
later rejected. The complete failed observations remain in `runs-repair/`; no
episode is replaced or included in an arm comparison.
